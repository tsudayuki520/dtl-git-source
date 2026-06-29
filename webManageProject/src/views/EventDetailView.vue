<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEventList } from '@/api/event'
import type { Event } from '@/api/event'
import { getRegistrationListByEvent, addRegistration, updateRegistration, deleteRegistration, promoteTopN } from '@/api/registration'
import type { RegistrationVO } from '@/api/registration'
import { getParticipantList } from '@/api/participant'
import type { Participant } from '@/api/participant'
import { getResultsByEventAndSchedule, addResult, updateResult } from '@/api/result'
import type { ResultVO } from '@/api/result'

const route = useRoute()
const router = useRouter()
const meetingId = Number(route.params.meetingId)
const eventId = Number(route.params.eventId)
const scheduleId = Number(route.params.scheduleId)

const eventInfo = ref<Event | null>(null)
const registrations = ref<RegistrationVO[]>([])
const regFilterStatus = ref<number | undefined>(undefined)

const regStatusMap: Record<number, string> = { 0: '已报名', 1: '已晋级', 2: '已取消' }
const regStatusType: Record<number, string> = { 0: 'primary', 1: 'success', 2: 'info' }

// ============ 成绩（行内编辑） ============
// resultMap 必须用 computed 依赖响应式 results，
// 否则成绩保存后（只更新 results）模板的成绩列不会重渲染。
const results = ref<ResultVO[]>([])
const resultMap = computed(() => new Map<number, ResultVO>(results.value.map(r => [r.participantId, r])))
const editingId = ref<number | null>(null)
const editingScore = ref('')

// 成绩值（径赛=毫秒，田赛=厘米）与录入值（径赛=秒，田赛=米）互转，并格式化显示
function scoreValueToInput(scoreValue: number | null | undefined): string {
  if (scoreValue == null) return ''
  const cat = eventInfo.value?.category
  if (cat === '径赛') return (scoreValue / 1000).toString()
  if (cat === '田赛') return (scoreValue / 100).toString()
  return String(scoreValue)
}
function inputToScoreValue(val: string): number {
  const n = Number(val)
  if (!Number.isFinite(n)) return 0
  const cat = eventInfo.value?.category
  if (cat === '径赛') return Math.round(n * 1000)
  if (cat === '田赛') return Math.round(n * 100)
  return Math.round(n)
}
function formatScore(r?: ResultVO): string {
  if (!r) return '录入'
  const cat = eventInfo.value?.category || r.category
  if (cat === '趣味赛') return r.points != null ? `${r.points}分` : '录入'
  if (r.scoreValue == null) return '录入'
  if (cat === '径赛') {
    const totalMs = r.scoreValue
    const totalSeconds = Math.floor(totalMs / 1000)
    const ms = totalMs % 1000
    const minutes = Math.floor(totalSeconds / 60)
    const seconds = totalSeconds % 60
    if (minutes > 0) return `${minutes}:${String(seconds).padStart(2, '0')}.${String(ms).padStart(3, '0')}`
    return `${seconds}.${String(ms).padStart(3, '0')}秒`
  }
  if (cat === '田赛') return `${(r.scoreValue / 100).toFixed(2)}米`
  return String(r.scoreValue)
}

// 自动聚焦指令：el-input 渲染时聚焦内部 input
const vFocus = {
  mounted: (el: HTMLElement) => {
    el.querySelector('input')?.focus()
  }
}

async function fetchResults() {
  try {
    const res: any = await getResultsByEventAndSchedule(eventId, scheduleId)
    results.value = res.data || res || []
  } catch { /* ignore */ }
}

function startEdit(row: RegistrationVO) {
  editingId.value = row.participantId
  const existing = resultMap.value.get(row.participantId)
  if (eventInfo.value?.category === '趣味赛') {
    editingScore.value = existing?.points != null ? String(existing.points) : ''
  } else {
    editingScore.value = scoreValueToInput(existing?.scoreValue)
  }
}

function cancelEdit() {
  editingId.value = null
}

async function saveScore(row: RegistrationVO) {
  // 防止回车后 blur 重复触发
  if (editingId.value !== row.participantId) return
  const val = editingScore.value.trim()
  if (val === '') {
    editingId.value = null
    return
  }
  if (!Number.isFinite(Number(val))) {
    ElMessage.warning('请输入有效成绩')
    return
  }
  const existing = resultMap.value.get(row.participantId)
  const cat = eventInfo.value?.category
  try {
    if (cat === '趣味赛') {
      // 趣味赛录入积分（points），不存成绩值
      const points = Math.round(Number(val))
      if (existing) {
        await updateResult({ id: existing.id, eventScheduleId: existing.eventScheduleId ?? undefined, points })
      } else {
        await addResult({ sportsMeetingId: meetingId, eventId, participantId: row.participantId, scheduleId, scoreValue: null, points })
      }
    } else {
      // 径赛/田赛录入成绩值
      if (existing) {
        await updateResult({ id: existing.id, eventScheduleId: existing.eventScheduleId ?? undefined, scoreValue: inputToScoreValue(val) })
      } else {
        await addResult({ sportsMeetingId: meetingId, eventId, participantId: row.participantId, scheduleId, scoreValue: inputToScoreValue(val) })
      }
    }
    ElMessage.success('保存成功')
    editingId.value = null
    fetchResults()
  } catch {
    ElMessage.error('保存失败')
  }
}

// 名次映射：按成绩排序后的名次（仅有成绩的排名，无成绩显示'-'）
const rankMap = computed(() => {
  const map = new Map<number, number>()
  results.value.forEach((r, i) => {
    const hasScore = r.category === '趣味赛' ? r.points != null : r.scoreValue != null
    if (hasScore) map.set(r.participantId, i + 1)
  })
  return map
})

// 一键晋级前N名
const promoteDialogVisible = ref(false)
const promoteTopNValue = ref(8)
function openPromoteDialog() {
  promoteTopNValue.value = 8
  promoteDialogVisible.value = true
}
async function handlePromoteTopN() {
  try {
    const res: any = await promoteTopN(eventId, scheduleId, promoteTopNValue.value)
    const count = res?.data?.promoted ?? res?.promoted ?? 0
    ElMessage.success(`成功晋级 ${count} 人`)
    promoteDialogVisible.value = false
    fetchRegistrations()
  } catch {
    ElMessage.error('晋级失败')
  }
}

// ============ 手动添加报名 ============
const addDialogVisible = ref(false)
const participants = ref<Participant[]>([])
const addSelectedIds = ref<number[]>([])
const addSearch = ref('')

async function fetchEvent() {
  try {
    const res: any = await getEventList({ sportsMeetingId: meetingId })
    const list: Event[] = res.data || res || []
    eventInfo.value = list.find(e => e.id === eventId) || null
  } catch { /* ignore */ }
  // 类别加载后需重新排序（团队赛按代表队排序）
  filterRegistrations()
}

async function fetchRegistrations() {
  try {
    const res: any = await getRegistrationListByEvent(eventId)
    const all: RegistrationVO[] = res.data || res || []
    registrations.value = all.filter(r => r.scheduleId === scheduleId)
  } catch { /* ignore */ }
  filterRegistrations()
}

const filteredRegistrations = ref<RegistrationVO[]>([])

function filterRegistrations() {
  let list = registrations.value.filter(r => {
    if (regFilterStatus.value !== undefined && regFilterStatus.value !== null && r.status !== regFilterStatus.value) return false
    return true
  })
  // 团队赛：按代表队名称排序，便于按队查看
  if (eventInfo.value?.category === '团队赛') {
    list = [...list].sort((a, b) => (a.teamName || '').localeCompare(b.teamName || ''))
  }
  filteredRegistrations.value = list
}

function formatDate(d: string) {
  if (!d) return ''
  return d.substring(0, 16).replace('T', ' ')
}

async function handleRegStatusChange(row: RegistrationVO, status: number) {
  try {
    await updateRegistration(row.id, status)
    ElMessage.success('状态更新成功')
    fetchRegistrations()
  } catch { ElMessage.error('操作失败') }
}

async function handleRegDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该报名记录？', '提示', { type: 'warning' })
    await deleteRegistration(id)
    ElMessage.success('删除成功')
    fetchRegistrations()
  } catch { /* cancel */ }
}

// ============ 添加参赛人员 ============
async function openAddDialog() {
  addSelectedIds.value = []
  addSearch.value = ''
  try {
    const pRes: any = await getParticipantList(meetingId)
    participants.value = pRes.data || pRes || []
  } catch { /* ignore */ }
  updateAvailableParticipants()
  addDialogVisible.value = true
}

// 已报名该项目的选手ID
function getRegisteredParticipantIds(): Set<number> {
  return new Set(registrations.value.filter(r => r.status !== 2).map(r => r.participantId))
}

// 可选人员：未报名该项目且未被取消的
const availableParticipants = ref<Participant[]>([])
function updateAvailableParticipants() {
  const registeredIds = getRegisteredParticipantIds()
  const kw = addSearch.value.toLowerCase()
  availableParticipants.value = participants.value.filter(p => {
    if (registeredIds.has(p.id)) return false
    if (kw && !p.name.toLowerCase().includes(kw) && !p.userCode.toLowerCase().includes(kw)) return false
    return true
  })
}

async function handleAddSubmit() {
  if (addSelectedIds.value.length === 0) {
    ElMessage.warning('请选择参赛人员')
    return
  }
  try {
    for (const pid of addSelectedIds.value) {
      await addRegistration({ participantId: pid, eventId, scheduleId })
    }
    ElMessage.success(`成功添加 ${addSelectedIds.value.length} 名参赛人员`)
    addDialogVisible.value = false
    fetchRegistrations()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '添加失败')
    fetchRegistrations()
  }
}

function goBack() {
  router.back()
}

onMounted(() => {
  fetchEvent()
  fetchRegistrations()
  fetchResults()
})
</script>

<template>
  <div class="event-detail-page" v-if="eventInfo">
    <!-- 顶部信息栏 -->
    <div class="info-bar">
      <div class="info-bar-top">
        <div class="info-bar-left">
          <el-button link @click="goBack" style="margin-right:12px;font-size:14px">← 返回</el-button>
          <h2 class="info-title">{{ eventInfo.name }}</h2>
        </div>
      </div>
      <div class="info-meta">
        <span>类别：{{ eventInfo.category }}</span>
        <span>性别：{{ eventInfo.gender }}</span>
        <span>组别：{{ eventInfo.groupTypeName }}</span>
        <span>报名人数：{{ registrations.length }}</span>
      </div>
    </div>

    <!-- 报名记录 -->
    <div class="content-card">
      <div class="tab-toolbar">
        <div class="tab-toolbar-left">
          <el-select v-model="regFilterStatus" placeholder="全部状态" clearable style="width:120px" @change="filterRegistrations">
            <el-option label="已报名" :value="0" />
            <el-option label="已晋级" :value="1" />
            <el-option label="已取消" :value="2" />
          </el-select>
        </div>
        <el-button type="warning" size="small" @click="openPromoteDialog">一键晋级前N名</el-button>
        <el-button type="primary" size="small" @click="openAddDialog">+ 添加参赛人员</el-button>
      </div>
      <el-table v-if="filteredRegistrations.length > 0" :data="filteredRegistrations" stripe border size="small">
        <el-table-column label="名次" width="70">
          <template #default="{ row }">{{ rankMap.get(row.participantId) ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="participantName" label="参赛者" width="120" />
        <el-table-column v-if="eventInfo?.category === '团队赛'" prop="teamName" label="代表队" width="120" />
        <el-table-column prop="eventName" label="项目" />
        <el-table-column prop="scheduleName" label="赛次" width="90" />
        <el-table-column :label="eventInfo?.category === '趣味赛' ? '积分' : '成绩'" width="110">
          <template #default="{ row }">
            <el-input
              v-if="editingId === row.participantId"
              v-model="editingScore"
              v-focus
              size="small"
              style="width:130px"
              :placeholder="eventInfo?.category === '趣味赛' ? '积分，如8' : (eventInfo?.category === '田赛' ? '米，如5.32' : '秒，如12.350')"
              @keyup.enter="saveScore(row)"
              @keyup.esc="cancelEdit"
              @blur="saveScore(row)"
            />
            <el-button v-else link type="primary" size="small" @click="startEdit(row)">
              {{ formatScore(resultMap.get(row.participantId)) }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="regStatusType[row.status]" size="small">{{ regStatusMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报名时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 1" link type="primary" size="small" @click="handleRegStatusChange(row, 1)">晋级</el-button>
            <el-button v-if="row.status !== 0" link type="success" size="small" @click="handleRegStatusChange(row, 0)">恢复</el-button>
            <el-button link type="danger" size="small" @click="handleRegDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无报名记录" />
    </div>

    <!-- 弹窗：添加参赛人员 -->
    <el-dialog v-model="addDialogVisible" title="添加参赛人员" width="600px" destroy-on-close @open="updateAvailableParticipants">
      <div style="margin-bottom:12px">
        <el-input v-model="addSearch" placeholder="搜索姓名/学号" clearable style="width:200px" @input="updateAvailableParticipants" />
      </div>
      <el-table :data="availableParticipants" stripe border size="small" max-height="400"
        @selection-change="(rows: any[]) => addSelectedIds = rows.map(r => r.id)">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="userCode" label="学号/工号" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column prop="teamName" label="代表队" width="120" />
        <el-table-column prop="college" label="学院" />
      </el-table>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="addSelectedIds.length === 0" @click="handleAddSubmit">
          确认添加 ({{ addSelectedIds.length }} 人)
        </el-button>
      </template>
    </el-dialog>

    <!-- 弹窗：一键晋级前N名 -->
    <el-dialog v-model="promoteDialogVisible" title="一键晋级前N名" width="380px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="晋级前">
          <el-input-number v-model="promoteTopNValue" :min="1" :max="999" controls-position="right" style="width:140px" />
          <span style="margin-left:8px">名</span>
        </el-form-item>
        <div style="color:#999;font-size:12px;margin:0 0 0 90px;line-height:1.6">
          按当前赛次成绩排序（径赛时间短/田赛距离远/趣味赛积分高），前N名晋级到下一赛次。<br/>
          已晋级或无成绩的自动跳过；已是最后一轮则不晋级。
        </div>
      </el-form>
      <template #footer>
        <el-button @click="promoteDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePromoteTopN">确定晋级</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.event-detail-page { padding: 20px; }
.info-bar {
  background: #fff; border-radius: 8px; padding: 20px;
  margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.info-bar-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.info-bar-left { display: flex; align-items: center; }
.info-title { font-size: 18px; margin: 0; }
.info-meta { display: flex; gap: 24px; font-size: 13px; color: #888; flex-wrap: wrap; }
.content-card {
  background: #fff; border-radius: 8px; padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.tab-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.tab-toolbar-left { display: flex; gap: 8px; }
</style>
