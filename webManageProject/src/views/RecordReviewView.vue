<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMeetingList } from '@/api/meeting'
import type { SportsMeeting } from '@/api/meeting'
import { getEventList } from '@/api/event'
import type { Event } from '@/api/event'
import { getResultList } from '@/api/result'
import type { ResultVO } from '@/api/result'
import { reviewRecord } from '@/api/record'

const meetings = ref<SportsMeeting[]>([])
const events = ref<Event[]>([])
const allCandidates = ref<ResultVO[]>([])

const meetingId = ref<number | undefined>(undefined)
const categoryFilter = ref<'all' | '田赛' | '径赛' | '团队赛'>('all')
const eventFilter = ref<number | 'all'>('all')
const statusFilter = ref<'all' | 0 | 1 | 2>('all')

// 序号守卫：防止快速切换时旧请求覆盖新结果
let meetingsReqId = 0
let eventsReqId = 0
let candidatesReqId = 0

async function fetchMeetings() {
  const reqId = ++meetingsReqId
  try {
    const res: any = await getMeetingList()
    if (reqId !== meetingsReqId) return
    meetings.value = res.data || res || []
    if (meetings.value.length > 0) {
      meetingId.value = meetings.value[0].id
      fetchEvents()
      fetchAllCandidates()
    }
  } catch {
    if (reqId !== meetingsReqId) return
    ElMessage.error('加载运动会失败')
  }
}

async function fetchEvents() {
  if (!meetingId.value) return
  const reqId = ++eventsReqId
  try {
    const res: any = await getEventList({ sportsMeetingId: meetingId.value })
    if (reqId !== eventsReqId) return
    events.value = res.data || res || []
  } catch {
    if (reqId !== eventsReqId) return
    events.value = []
  }
}

// 拿运动会下全部成绩，按 (项目,赛次) 分组，每组按成绩排序取前 3 作为候选
async function fetchAllCandidates() {
  if (!meetingId.value) return
  const reqId = ++candidatesReqId
  try {
    const res: any = await getResultList(meetingId.value)
    if (reqId !== candidatesReqId) return
    const all: ResultVO[] = res.data || res || []
    const groups = new Map<string, ResultVO[]>()
    for (const r of all) {
      if (r.scoreValue == null) continue
      const key = `${r.eventId}_${r.scheduleId ?? r.eventScheduleId ?? 0}`
      if (!groups.has(key)) groups.set(key, [])
      groups.get(key)!.push(r)
    }
    const candidates: ResultVO[] = []
    for (const group of groups.values()) {
      const cat = group[0]?.category || ''
      group.sort((a, b) => {
        const av = a.scoreValue ?? 0
        const bv = b.scoreValue ?? 0
        if (cat === '田赛') return bv - av  // 距离远优先
        return av - bv  // 径赛/团队赛：时间短优先
      })
      candidates.push(...group.slice(0, 3))
    }
    allCandidates.value = candidates
  } catch {
    if (reqId !== candidatesReqId) return
    allCandidates.value = []
    ElMessage.warning('加载候选失败')
  }
}

const filteredResults = computed(() =>
  allCandidates.value.filter(r => {
    if (categoryFilter.value !== 'all' && r.category !== categoryFilter.value) return false
    if (eventFilter.value !== 'all' && r.eventId !== eventFilter.value) return false
    if (statusFilter.value !== 'all' && (r.recordStatus ?? 0) !== statusFilter.value) return false
    return true
  })
)

function formatScore(r?: ResultVO): string {
  if (!r || r.scoreValue == null) return '-'
  if (r.category === '田赛') return `${(r.scoreValue / 100).toFixed(2)}米`
  const totalSeconds = Math.floor(r.scoreValue / 1000)
  const ms = r.scoreValue % 1000
  return `${totalSeconds}.${String(ms).padStart(3, '0')}秒`
}

const statusMap: Record<number, string> = { 0: '待审', 1: '已通过', 2: '已拒绝' }
const statusType: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info' }

async function handleReview(id: number, action: 'approve' | 'reject') {
  try {
    await reviewRecord(id, action)
    ElMessage.success(action === 'approve' ? '已通过并入册' : '已拒绝')
    await fetchAllCandidates()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '操作失败')
  }
}

function onMeetingChange() {
  eventFilter.value = 'all'
  fetchEvents()
  fetchAllCandidates()
}

onMounted(fetchMeetings)
</script>

<template>
  <div class="record-review-page">
    <div class="content-card">
      <div class="tab-toolbar">
        <div class="tab-toolbar-left">
          <el-select v-model="meetingId" placeholder="运动会" style="width:180px" @change="onMeetingChange">
            <el-option v-for="m in meetings" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
          <el-select v-model="categoryFilter" style="width:120px">
            <el-option label="全部分类" value="all" />
            <el-option label="田赛" value="田赛" />
            <el-option label="径赛" value="径赛" />
            <el-option label="团队赛" value="团队赛" />
          </el-select>
          <el-select v-model="eventFilter" placeholder="项目" style="width:160px">
            <el-option label="全部项目" value="all" />
            <el-option v-for="e in events" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
          <el-select v-model="statusFilter" style="width:110px">
            <el-option label="全部状态" value="all" />
            <el-option label="待审" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
        </div>
        <span class="toolbar-hint">展示各赛次前 3 名候选；通过即入册校运会纪录档案</span>
      </div>

      <el-table :data="filteredResults" stripe border size="small">
        <el-table-column label="" width="40" align="center">
          <template #default="{ row }">
            <el-tooltip
              :content="row.recordStatus === 1 ? '已通过入册' : row.recordStatus === 2 ? '已拒绝' : '破纪录候选（赛次前3）'">
              <span :class="['record-flag',
                row.recordStatus === 1 ? 'record-flag-approved' :
                row.recordStatus === 2 ? 'record-flag-rejected' : 'record-flag-pending']">▲</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="participantName" label="参赛者" width="120" />
        <el-table-column prop="eventName" label="项目" />
        <el-table-column prop="category" label="分类" width="80" />
        <el-table-column prop="scheduleName" label="赛次" width="90" />
        <el-table-column label="成绩" width="120">
          <template #default="{ row }">{{ formatScore(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType[row.recordStatus ?? 0] as any" size="small">
              {{ statusMap[row.recordStatus ?? 0] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="(row.recordStatus ?? 0) === 0">
              <el-button link type="success" size="small" @click="handleReview(row.id, 'approve')">通过</el-button>
              <el-button link type="danger" size="small" @click="handleReview(row.id, 'reject')">拒绝</el-button>
            </template>
            <span v-else style="color:#999;font-size:12px">已审核</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.record-review-page { padding: 20px; }
.content-card { background:#fff; border-radius:8px; padding:16px; box-shadow:0 1px 3px rgba(0,0,0,0.08); }
.tab-toolbar { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; }
.tab-toolbar-left { display:flex; gap:8px; }
.toolbar-hint { color:#999; font-size:12px; }
.record-flag { font-size:14px; font-weight:600; }
.record-flag-pending { color: #e6a23c; }
.record-flag-approved { color: #67c23a; }
.record-flag-rejected { color: #c0c4cc; }
</style>
