<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMeetingList } from '@/api/meeting'
import type { SportsMeeting } from '@/api/meeting'
import { getEventList } from '@/api/event'
import type { Event } from '@/api/event'
import { getRegistrationListByEvent } from '@/api/registration'
import { getResultsByEventAndSchedule } from '@/api/result'
import type { ResultVO } from '@/api/result'
import { reviewRecord } from '@/api/record'

const meetings = ref<SportsMeeting[]>([])
const events = ref<Event[]>([])
const schedules = ref<{ id: number; name: string }[]>([])
const results = ref<ResultVO[]>([])

const meetingId = ref<number | undefined>(undefined)
const eventId = ref<number | undefined>(undefined)
const scheduleId = ref<number | undefined>(undefined)
const statusFilter = ref<'all' | 0 | 1 | 2>('all')

// 序号守卫：防止快速切换 select 时旧请求覆盖新结果
let meetingsReqId = 0
let eventsReqId = 0
let schedulesReqId = 0
let resultsReqId = 0

async function fetchMeetings() {
  const reqId = ++meetingsReqId
  try {
    const res: any = await getMeetingList()
    if (reqId !== meetingsReqId) return
    meetings.value = res.data || res || []
    if (meetings.value.length > 0) {
      meetingId.value = meetings.value[0].id
      fetchEvents()
    }
  } catch {
    if (reqId !== meetingsReqId) return
    ElMessage.error('加载运动会失败')
  }
}

async function fetchEvents() {
  if (!meetingId.value) return
  const reqId = ++eventsReqId
  eventId.value = undefined
  scheduleId.value = undefined
  schedules.value = []
  results.value = []
  try {
    const res: any = await getEventList({ sportsMeetingId: meetingId.value })
    if (reqId !== eventsReqId) return
    events.value = res.data || res || []
  } catch {
    if (reqId !== eventsReqId) return
    events.value = []
  }
}

async function fetchSchedules() {
  if (!eventId.value) return
  const reqId = ++schedulesReqId
  scheduleId.value = undefined
  results.value = []
  try {
    const res: any = await getRegistrationListByEvent(eventId.value)
    if (reqId !== schedulesReqId) return
    const regs = res.data || res || []
    const map = new Map<number, string>()
    regs.forEach((r: any) => {
      if (r.scheduleId && !map.has(r.scheduleId)) map.set(r.scheduleId, r.scheduleName || `赛次${r.scheduleId}`)
    })
    schedules.value = [...map.entries()].map(([id, name]) => ({ id, name }))
  } catch {
    if (reqId !== schedulesReqId) return
    schedules.value = []
  }
}

async function fetchResults() {
  if (!eventId.value || !scheduleId.value) return
  const reqId = ++resultsReqId
  try {
    const res: any = await getResultsByEventAndSchedule(eventId.value, scheduleId.value)
    if (reqId !== resultsReqId) return
    results.value = res.data || res || []
  } catch {
    if (reqId !== resultsReqId) return
    results.value = []
    ElMessage.warning('加载成绩失败')
  }
}

const category = computed(() => {
  const ev = events.value.find(e => e.id === eventId.value)
  return ev?.category || ''
})

// 前 3 名 id 集合（results 已按成绩排序，取前 3 有成绩的）
const top3Ids = computed(() =>
  results.value.filter(r => r.scoreValue != null).slice(0, 3).map(r => r.id)
)

const filteredResults = computed(() => {
  if (statusFilter.value === 'all') return results.value
  return results.value.filter(r => (r.recordStatus ?? 0) === statusFilter.value)
})

function isTop3(id: number) {
  return top3Ids.value.includes(id)
}

function formatScore(r?: ResultVO): string {
  if (!r || r.scoreValue == null) return '-'
  if (category.value === '田赛') return `${(r.scoreValue / 100).toFixed(2)}米`
  const totalMs = r.scoreValue
  const totalSeconds = Math.floor(totalMs / 1000)
  const ms = totalMs % 1000
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  if (minutes > 0) return `${minutes}:${String(seconds).padStart(2, '0')}.${String(ms).padStart(3, '0')}`
  return `${seconds}.${String(ms).padStart(3, '0')}秒`
}

const statusMap: Record<number, string> = { 0: '待审', 1: '已通过', 2: '已拒绝' }
const statusType: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info' }

async function handleReview(id: number, action: 'approve' | 'reject') {
  try {
    await reviewRecord(id, action)
    ElMessage.success(action === 'approve' ? '已通过并入册' : '已拒绝')
    await fetchResults()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || '操作失败')
  }
}

onMounted(fetchMeetings)
</script>

<template>
  <div class="record-review-page">
    <div class="content-card">
      <div class="tab-toolbar">
        <div class="tab-toolbar-left">
          <el-select v-model="meetingId" placeholder="运动会" style="width:180px" @change="fetchEvents">
            <el-option v-for="m in meetings" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
          <el-select v-model="eventId" placeholder="项目" style="width:160px" @change="fetchSchedules">
            <el-option v-for="e in events" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
          <el-select v-model="scheduleId" placeholder="赛次" style="width:120px" @change="fetchResults">
            <el-option v-for="s in schedules" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <el-select v-model="statusFilter" style="width:110px">
            <el-option label="全部状态" value="all" />
            <el-option label="待审" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
        </div>
        <span class="toolbar-hint">前 3 名自动标三角形候选；通过即入册校运会纪录档案</span>
      </div>

      <el-table v-if="eventId && scheduleId" :data="filteredResults" stripe border size="small">
        <el-table-column label="" width="40" align="center">
          <template #default="{ row }">
            <el-tooltip
              v-if="isTop3(row.id)"
              :content="row.recordStatus === 1 ? '已通过入册' : row.recordStatus === 2 ? '已拒绝' : '破纪录候选（赛次前3）'">
              <span :class="['record-flag',
                row.recordStatus === 1 ? 'record-flag-approved' :
                row.recordStatus === 2 ? 'record-flag-rejected' : 'record-flag-pending']">▲</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="participantName" label="参赛者" width="120" />
        <el-table-column prop="eventName" label="项目" />
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
      <el-empty v-else description="请选择项目和赛次查看候选" />
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
