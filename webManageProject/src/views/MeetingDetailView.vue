<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMeetingDetail } from '@/api/meeting'
import type { SportsMeeting } from '@/api/meeting'
import { getEventList } from '@/api/event'
import type { Event } from '@/api/event'
import { getEventSchedulesBySportsMeeting } from '@/api/eventSchedule'
import type { EventSchedule } from '@/api/eventSchedule'
import { getParticipantList, getParticipantListByTeam, addParticipant, updateParticipant, deleteParticipant } from '@/api/participant'
import type { Participant } from '@/api/participant'
import { getScheduleList, addSchedule, updateSchedule, deleteSchedule } from '@/api/schedule'
import type { Schedule } from '@/api/schedule'
import { getNoticeList, addNotice, updateNotice, deleteNotice, uploadNoticeFile } from '@/api/notice'
import type { Notice } from '@/api/notice'
import { getResultList, getResultListByEvent, addResult, updateResult, deleteResult } from '@/api/result'
import type { ResultVO, ResultItem } from '@/api/result'
import { getTeamList, addTeam, updateTeam } from '@/api/team'
import type { Team } from '@/api/team'
import { getGroupTypeList, addGroupType, updateGroupType, deleteGroupType, getLimitConfig, saveLimitConfig } from '@/api/groupType'
import type { GroupType } from '@/api/groupType'

const route = useRoute()
const router = useRouter()
const meetingId = Number(route.params.id)
const meeting = ref<SportsMeeting | null>(null)
const activeTab = ref('schedule')

// ============ 运动会信息 ============
async function fetchMeeting() {
  try {
    const res: any = await getMeetingDetail(meetingId)
    meeting.value = res.data || res
  } catch {
    ElMessage.error('获取运动会信息失败')
  }
}

const meetingStatusMap: Record<number, { label: string; color: string }> = {
  0: { label: '筹备中', color: '#909399' },
  1: { label: '报名中', color: '#1890ff' },
  2: { label: '进行中', color: '#52c41a' },
  3: { label: '已结束', color: '#ff4d4f' },
}

function formatDate(d: string) {
  return d ? d.substring(0, 10) : ''
}

// ============ 赛程轮次 (含比赛项目) ============
const schedules = ref<Schedule[]>([])
const events = ref<Event[]>([])
const eventScheduleList = ref<EventSchedule[]>([])
const scheduleDialogVisible = ref(false)
const scheduleForm = ref<Partial<Schedule>>({})
const scheduleStatusMap: Record<number, string> = { 0: '进行中', 1: '已结束' }

async function fetchSchedules() {
  try {
    const res: any = await getScheduleList(meetingId)
    schedules.value = res.data || res || []
  } catch { /* ignore */ }
}

async function fetchEvents() {
  try {
    const res: any = await getEventList({ sportsMeetingId: meetingId })
    events.value = res.data || res || []
  } catch { /* ignore */ }
}

async function fetchEventSchedules() {
  try {
    const res: any = await getEventSchedulesBySportsMeeting(meetingId)
    eventScheduleList.value = res.data || res || []
  } catch { /* ignore */ }
}

function getEventIdsBySchedule(scheduleId: number): Set<number> {
  return new Set(eventScheduleList.value.filter(es => es.scheduleId === scheduleId).map(es => es.eventId))
}

function getEventsBySchedule(scheduleId: number) {
  const ids = getEventIdsBySchedule(scheduleId)
  return events.value.filter(e => ids.has(e.id))
}

function goScheduleDetail(scheduleId: number) {
  router.push(`/meeting/${meetingId}/schedule/${scheduleId}`)
}

function openScheduleAdd() {
  scheduleForm.value = { sportsMeetingId: meetingId, name: '', sort: schedules.value.length + 1, status: 0 }
  scheduleDialogVisible.value = true
}
function openScheduleEdit(row: Schedule) {
  scheduleForm.value = { ...row }
  scheduleDialogVisible.value = true
}
async function handleScheduleSubmit() {
  try {
    if (scheduleForm.value.id) {
      await updateSchedule(scheduleForm.value)
    } else {
      await addSchedule(scheduleForm.value)
    }
    ElMessage.success('操作成功')
    scheduleDialogVisible.value = false
    fetchSchedules()
  } catch { ElMessage.error('操作失败') }
}
async function handleScheduleDelete(id: number) {
  try {
    const count = getEventsBySchedule(id).length
    const msg = count > 0
      ? `该轮次下有 ${count} 个项目，确定删除？`
      : '确定删除该轮次？'
    await ElMessageBox.confirm(msg, '提示', { type: 'warning' })
    await deleteSchedule(id)
    ElMessage.success('删除成功')
    fetchSchedules()
    fetchEvents()
  } catch { /* cancel */ }
}

// ============ 组别管理 (含代表队) ============
const groupTypes = ref<GroupType[]>([])
const teams = ref<Team[]>([])
const gtDialogVisible = ref(false)
const gtForm = ref<Partial<GroupType>>({})
const teamDialogVisible = ref(false)
const teamForm = ref<Partial<Team>>({})
const expandedGroupTypes = ref<number[]>([])

async function fetchGroupTypes() {
  try {
    const res: any = await getGroupTypeList(meetingId)
    groupTypes.value = res.data || res || []
    if (groupTypes.value.length > 0 && expandedGroupTypes.value.length === 0) {
      expandedGroupTypes.value = [groupTypes.value[0].id]
    }
  } catch { /* ignore */ }
}

async function fetchTeams() {
  try {
    const res: any = await getTeamList({ sportsMeetingId: meetingId })
    teams.value = res.data || res || []
    teams.value.forEach(t => fetchTeamParticipants(t.id))
  } catch { /* ignore */ }
}

function getTeamsByGroupType(groupTypeId: number) {
  return teams.value.filter(t => t.groupTypeId === groupTypeId)
}

function openGtAdd() {
  gtForm.value = { sportsMeetingId: meetingId, name: '' }
  gtDialogVisible.value = true
}
function openGtEdit(row: GroupType) {
  gtForm.value = { ...row }
  gtDialogVisible.value = true
}
async function handleGtSubmit() {
  try {
    if (gtForm.value.id) {
      await updateGroupType(gtForm.value)
    } else {
      await addGroupType(gtForm.value)
    }
    ElMessage.success('操作成功')
    gtDialogVisible.value = false
    fetchGroupTypes()
  } catch { ElMessage.error('操作失败') }
}
async function handleGtDelete(id: number) {
  try {
    const count = getTeamsByGroupType(id).length
    const msg = count > 0
      ? `该组别下有 ${count} 个代表队，确定删除？`
      : '确定删除该组别？'
    await ElMessageBox.confirm(msg, '提示', { type: 'warning' })
    await deleteGroupType(id)
    ElMessage.success('删除成功')
    fetchGroupTypes()
    fetchTeams()
  } catch { /* cancel */ }
}

function openTeamAdd(groupTypeId: number) {
  teamForm.value = { sportsMeetingId: meetingId, groupTypeId, name: '', leader: '', coach: '', totalScore: 0 }
  teamDialogVisible.value = true
}

// ============ 限报配置 ============
const limitDialogVisible = ref(false)
const limitForm = ref({
  groupTypeId: 0,
  perTeamLimit: 0,
  eventIds: [] as number[],
  perPersonLimit: 0,
  personEventIds: [] as number[]
})
const allEvents = ref<Event[]>([])
const eventsByCategory = computed(() => {
  const m: Record<string, Event[]> = {}
  for (const e of allEvents.value) {
    const cat = e.category || '其他'
    ;(m[cat] = m[cat] || []).push(e)
  }
  return m
})

function isCategoryAllChecked(cat: string, field: 'eventIds' | 'personEventIds') {
  const ids = (eventsByCategory.value[cat] || []).map(e => e.id)
  return ids.length > 0 && ids.every(id => limitForm.value[field].includes(id))
}

function toggleCategoryAll(cat: string, checked: any, field: 'eventIds' | 'personEventIds') {
  const ids = (eventsByCategory.value[cat] || []).map(e => e.id)
  if (checked) {
    limitForm.value[field] = Array.from(new Set([...limitForm.value[field], ...ids]))
  } else {
    limitForm.value[field] = limitForm.value[field].filter(id => !ids.includes(id))
  }
}

async function openLimitConfig(gt: GroupType) {
  limitForm.value.groupTypeId = gt.id
  if (allEvents.value.length === 0) {
    const res: any = await getEventList({ sportsMeetingId: meetingId })
    allEvents.value = res.data || res || []
  }
  const res: any = await getLimitConfig(gt.id)
  const cfg = res.data || res
  limitForm.value.perTeamLimit = cfg?.perTeamLimit || 0
  limitForm.value.perPersonLimit = cfg?.perPersonLimit || 0
  try {
    limitForm.value.eventIds = cfg?.limitEventIds ? JSON.parse(cfg.limitEventIds) : []
    limitForm.value.personEventIds = cfg?.personLimitEventIds ? JSON.parse(cfg.personLimitEventIds) : []
  } catch {
    limitForm.value.eventIds = []
    limitForm.value.personEventIds = []
  }
  limitDialogVisible.value = true
}

async function saveLimit() {
  await saveLimitConfig({
    groupTypeId: limitForm.value.groupTypeId,
    perTeamLimit: limitForm.value.perTeamLimit,
    eventIds: limitForm.value.eventIds,
    perPersonLimit: limitForm.value.perPersonLimit,
    personEventIds: limitForm.value.personEventIds
  })
  ElMessage.success('限报配置已保存')
  limitDialogVisible.value = false
}
async function handleTeamSubmit() {
  try {
    if (teamForm.value.id) {
      await updateTeam(teamForm.value)
    } else {
      await addTeam(teamForm.value)
    }
    ElMessage.success('操作成功')
    teamDialogVisible.value = false
    fetchTeams()
  } catch { ElMessage.error('操作失败') }
}

// ============ 代表队 - 参赛人员分配 ============
const teamParticipants = ref<Record<number, Participant[]>>({})
const assignDialogVisible = ref(false)
const assignTeamId = ref<number>(0)
const assignTeamName = ref('')
const assignSelectedIds = ref<number[]>([])

async function fetchTeamParticipants(teamId: number) {
  try {
    const res: any = await getParticipantListByTeam(teamId)
    teamParticipants.value[teamId] = res.data || res || []
  } catch { /* ignore */ }
}

function getTeamParticipants(teamId: number): Participant[] {
  return teamParticipants.value[teamId] || []
}

async function handleAssignSubmit() {
  try {
    for (const pid of assignSelectedIds.value) {
      await updateParticipant({ id: pid, teamId: assignTeamId.value })
    }
    ElMessage.success('分配成功')
    assignDialogVisible.value = false
    fetchTeamParticipants(assignTeamId.value)
    fetchParticipants()
  } catch { ElMessage.error('分配失败') }
}

// ============ 参赛人员 ============
const participants = ref<Participant[]>([])
const participantSearch = ref('')
const participantDialogVisible = ref(false)
const participantForm = ref<Partial<Participant>>({})

async function fetchParticipants() {
  try {
    const res: any = await getParticipantList(meetingId)
    participants.value = res.data || res || []
  } catch { /* ignore */ }
}

// 可分配的人员：该运动会下所有未分配代表队的人员
const assignableParticipants = ref<Participant[]>([])
watch([participants, assignDialogVisible], () => {
  if (assignDialogVisible.value) {
    const assignedIds = new Set<number>()
    Object.values(teamParticipants.value).forEach(list => {
      list.forEach(p => { if (p.teamId) assignedIds.add(p.id) })
    })
    assignableParticipants.value = participants.value.filter(p => !assignedIds.has(p.id))
  }
})

const filteredParticipants = ref<Participant[]>([])
watch([participants, participantSearch], () => {
  const kw = participantSearch.value.toLowerCase()
  filteredParticipants.value = participants.value.filter((p) => {
    if (!kw) return true
    return p.name.toLowerCase().includes(kw) || p.userCode.toLowerCase().includes(kw) || (p.college || '').toLowerCase().includes(kw)
  })
}, { immediate: true })

function openParticipantAdd() {
  participantForm.value = { sportsMeetingId: meetingId, userCode: '', name: '', phone: '', gender: '男', college: '', major: '' }
  participantDialogVisible.value = true
}
function openParticipantEdit(row: Participant) {
  participantForm.value = { ...row }
  participantDialogVisible.value = true
}
async function handleParticipantSubmit() {
  try {
    if (participantForm.value.id) {
      await updateParticipant(participantForm.value)
    } else {
      await addParticipant(participantForm.value)
    }
    ElMessage.success('操作成功')
    participantDialogVisible.value = false
    fetchParticipants()
  } catch { ElMessage.error('操作失败') }
}
async function handleParticipantDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该参赛人员？', '提示', { type: 'warning' })
    await deleteParticipant(id)
    ElMessage.success('删除成功')
    fetchParticipants()
  } catch { /* cancel */ }
}

// ============ 公告通知 ============
const notices = ref<Notice[]>([])
const noticeDialogVisible = ref(false)
const noticeForm = ref<Partial<Notice>>({})
const noticeUploading = ref(false)

async function fetchNotices() {
  try {
    const res: any = await getNoticeList(meetingId)
    notices.value = res.data || res || []
  } catch { /* ignore */ }
}

function openNoticeAdd() {
  noticeForm.value = { sportsMeetingId: meetingId, title: '', content: '', fileUrl: null, fileName: null }
  noticeDialogVisible.value = true
}
function openNoticeEdit(row: Notice) {
  noticeForm.value = { ...row }
  noticeDialogVisible.value = true
}
async function handleNoticeFileUpload(e: globalThis.Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  noticeUploading.value = true
  try {
    const res: any = await uploadNoticeFile(file)
    if (res.data) {
      const parts = (res.data as string).split(',')
      noticeForm.value.fileUrl = parts[0]
      noticeForm.value.fileName = parts[1] || file.name
    }
    ElMessage.success('文件上传成功')
  } catch { ElMessage.error('文件上传失败') }
  finally { noticeUploading.value = false }
}
function handleNoticeFileRemove() {
  noticeForm.value.fileUrl = null
  noticeForm.value.fileName = null
}
async function handleNoticeSubmit() {
  try {
    if (noticeForm.value.id) {
      await updateNotice(noticeForm.value)
    } else {
      await addNotice(noticeForm.value)
    }
    ElMessage.success('操作成功')
    noticeDialogVisible.value = false
    fetchNotices()
  } catch { ElMessage.error('操作失败') }
}
async function handleNoticeDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该公告？', '提示', { type: 'warning' })
    await deleteNotice(id)
    ElMessage.success('删除成功')
    fetchNotices()
  } catch { /* cancel */ }
}

// ============ 成绩管理 ============
const results = ref<ResultVO[]>([])
const resultDialogVisible = ref(false)
const resultForm = ref<Partial<ResultItem>>({})
const resultSearch = ref('')
const resultFilterSchedule = ref<number | undefined>(undefined)
// 成绩录入临时输入：径赛分/秒/毫秒，田赛米/厘米
const resultInput = ref({ minutes: 0, seconds: 0, millis: 0, meters: 0, centimeters: 0 })

// 当前选中项目的分类（决定录入单位）
const selectedEventCategory = computed(() => {
  const ev = events.value.find(e => e.id === resultForm.value.eventId)
  return ev?.category || ''
})

// 成绩值按分类格式化显示
function formatScore(row: ResultVO): string {
  if (row.scoreValue == null) return '-'
  if (row.category === '径赛') {
    const totalMs = row.scoreValue
    const totalSeconds = Math.floor(totalMs / 1000)
    const ms = totalMs % 1000
    const minutes = Math.floor(totalSeconds / 60)
    const seconds = totalSeconds % 60
    if (minutes > 0) {
      return `${minutes}:${String(seconds).padStart(2, '0')}.${String(ms).padStart(3, '0')}`
    }
    return `${seconds}.${String(ms).padStart(3, '0')}秒`
  }
  if (row.category === '田赛') {
    return `${(row.scoreValue / 100).toFixed(2)}米`
  }
  return String(row.scoreValue)
}

// 录入控件值合并为 scoreValue
function buildScoreValue(): number | null {
  const cat = selectedEventCategory.value
  const i = resultInput.value
  if (cat === '径赛') {
    return i.minutes * 60000 + i.seconds * 1000 + i.millis
  }
  if (cat === '田赛') {
    return i.meters * 100 + i.centimeters
  }
  return null
}

// 编辑时把 scoreValue 拆成录入控件值
function splitScoreValue(scoreValue: number | null, category: string) {
  const i = { minutes: 0, seconds: 0, millis: 0, meters: 0, centimeters: 0 }
  if (scoreValue == null) return i
  if (category === '径赛') {
    i.minutes = Math.floor(scoreValue / 60000)
    const rest = scoreValue % 60000
    i.seconds = Math.floor(rest / 1000)
    i.millis = rest % 1000
  } else if (category === '田赛') {
    i.meters = Math.floor(scoreValue / 100)
    i.centimeters = scoreValue % 100
  }
  return i
}

async function fetchResults() {
  try {
    const res: any = await getResultList(meetingId)
    results.value = res.data || res || []
  } catch { /* ignore */ }
}

function getScheduleIdsByEventId(eventId: number | undefined): number[] {
  if (eventId == null) return []
  return eventScheduleList.value.filter(es => es.eventId === eventId).map(es => es.scheduleId)
}

function getScheduleNameById(scheduleId: number | null): string {
  if (scheduleId == null) return '未分类'
  const sch = schedules.value.find(s => s.id === scheduleId)
  return sch ? sch.name : '未分类'
}

const filteredResults = ref<ResultVO[]>([])
watch([results, resultSearch, resultFilterSchedule], () => {
  const kw = resultSearch.value.toLowerCase()
  filteredResults.value = results.value.filter((r) => {
    if (resultFilterSchedule.value !== undefined && resultFilterSchedule.value !== null) {
      if (r.scheduleId !== resultFilterSchedule.value) return false
    }
    if (!kw) return true
    return (r.participantName || '').toLowerCase().includes(kw) || (r.eventName || '').toLowerCase().includes(kw)
  })
}, { immediate: true })

function getResultsGroupedBySchedule(): { scheduleId: number | null, scheduleName: string, items: ResultVO[] }[] {
  const groups: Record<string, ResultVO[]> = {}
  for (const r of filteredResults.value) {
    const schId = r.scheduleId ?? null
    const key = String(schId)
    if (!groups[key]) groups[key] = []
    groups[key].push(r)
  }
  return Object.entries(groups).map(([key, items]) => ({
    scheduleId: key === 'null' ? null : Number(key),
    scheduleName: items[0].scheduleName || getScheduleNameById(items[0].scheduleId),
    items,
  }))
}

function openResultAdd() {
  resultForm.value = { sportsMeetingId: meetingId, eventId: undefined as any, scheduleId: undefined, participantId: undefined as any, scoreValue: null, points: 0 }
  resultInput.value = { minutes: 0, seconds: 0, millis: 0, meters: 0, centimeters: 0 }
  resultDialogVisible.value = true
}
function openResultEdit(row: ResultVO) {
  resultForm.value = { id: row.id, sportsMeetingId: row.sportsMeetingId, eventId: row.eventId, scheduleId: row.scheduleId ?? undefined, participantId: row.participantId, scoreValue: row.scoreValue, points: row.points }
  resultInput.value = splitScoreValue(row.scoreValue, row.category)
  resultDialogVisible.value = true
}
async function handleResultSubmit() {
  if (!resultForm.value.scheduleId) {
    ElMessage.warning('请选择赛次')
    return
  }
  resultForm.value.scoreValue = buildScoreValue()
  try {
    if (resultForm.value.id) {
      await updateResult(resultForm.value)
    } else {
      await addResult(resultForm.value)
    }
    ElMessage.success('操作成功')
    resultDialogVisible.value = false
    fetchResults()
  } catch { ElMessage.error('操作失败') }
}
async function handleResultDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该成绩记录？', '提示', { type: 'warning' })
    await deleteResult(id)
    ElMessage.success('删除成功')
    fetchResults()
  } catch { /* cancel */ }
}

// ============ 成绩历史记录 ============
const historyDialogVisible = ref(false)
const historyFilterEvent = ref<number | undefined>(undefined)
const historyResults = ref<ResultVO[]>([])

async function openHistoryDialog() {
  historyFilterEvent.value = undefined
  historyDialogVisible.value = true
  await fetchHistoryResults()
}

async function fetchHistoryResults() {
  try {
    if (historyFilterEvent.value) {
      const res: any = await getResultListByEvent(historyFilterEvent.value)
      historyResults.value = res.data || res || []
    } else {
      const res: any = await getResultList(meetingId)
      historyResults.value = res.data || res || []
    }
  } catch { /* ignore */ }
}

// ============ Tab 切换加载数据 ============
const loadedTabs = new Set<string>()

function onTabChange(tab: string) {
  if (loadedTabs.has(tab)) return
  loadedTabs.add(tab)
  if (tab === 'schedule') { fetchSchedules(); fetchEvents(); fetchEventSchedules() }
  else if (tab === 'groupType') { fetchGroupTypes(); fetchTeams(); fetchParticipants() }
  else if (tab === 'participant') fetchParticipants()
  else if (tab === 'notice') fetchNotices()
  else if (tab === 'result') { fetchResults(); fetchSchedules(); fetchEvents(); fetchEventSchedules(); fetchParticipants() }
}

onMounted(() => {
  fetchMeeting()
  loadedTabs.add('schedule')
  fetchSchedules()
  fetchEvents()
  fetchEventSchedules()
})
</script>

<template>
  <div class="detail-page" v-if="meeting">
    <!-- 运动会信息栏 -->
    <div class="info-bar">
      <div class="info-bar-top">
        <h2 class="info-title">{{ meeting.name }}</h2>
        <el-tag
          :color="meetingStatusMap[meeting.status]?.color"
          style="border: none; color: #fff;"
          size="large"
        >
          {{ meetingStatusMap[meeting.status]?.label }}
        </el-tag>
      </div>
      <div class="info-meta">
        <span>地点：{{ meeting.venue }}</span>
        <span>赛事日期：{{ formatDate(meeting.competitionDate) }}</span>
        <span>主办方：{{ meeting.organizer }}</span>
        <span>联系电话：{{ meeting.contactPhone }}</span>
      </div>
    </div>

    <!-- Tab 管理区 -->
    <el-tabs v-model="activeTab" @tab-change="onTabChange" class="main-tabs">

      <!-- ======== 赛程轮次 ======== -->
      <el-tab-pane label="赛程轮次" name="schedule">
        <div class="tab-toolbar">
          <span class="toolbar-hint">点击轮次进入管理其下的比赛项目</span>
          <el-button type="primary" size="small" @click="openScheduleAdd">+ 新增轮次</el-button>
        </div>
        <el-empty v-if="schedules.length === 0" description="暂无赛程轮次，请先创建" />
        <div v-else class="schedule-card-list">
          <div
            v-for="sch in schedules" :key="sch.id" class="schedule-card"
            @click="goScheduleDetail(sch.id)"
          >
            <div class="schedule-card-header">
              <span class="schedule-card-name">
                <el-tag size="small" type="info" effect="plain" style="margin-right:6px">第{{ sch.sort || '?' }}轮</el-tag>{{ sch.name }}
              </span>
              <el-tag :type="sch.status === 0 ? 'primary' : 'info'" size="small">{{ scheduleStatusMap[sch.status] }}</el-tag>
            </div>
            <div class="schedule-card-info">
              <span>{{ getEventsBySchedule(sch.id).length }} 个项目</span>
            </div>
            <div class="schedule-card-actions" @click.stop>
              <el-button link type="primary" size="small" @click="openScheduleEdit(sch)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleScheduleDelete(sch.id)">删除</el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- ======== 组别管理 (含代表队) ======== -->
      <el-tab-pane label="组别管理" name="groupType">
        <div class="tab-toolbar">
          <span class="toolbar-hint">展开组别查看并管理其下的代表队</span>
          <el-button type="primary" size="small" @click="openGtAdd">+ 新增组别</el-button>
        </div>
        <el-empty v-if="groupTypes.length === 0" description="暂无组别，请先创建" />
        <el-collapse v-else v-model="expandedGroupTypes" class="nested-collapse">
          <el-collapse-item v-for="gt in groupTypes" :key="gt.id" :name="gt.id">
            <template #title>
              <div class="collapse-title">
                <span class="collapse-title-name">{{ gt.name }}</span>
                <span class="collapse-count">{{ getTeamsByGroupType(gt.id).length }} 个代表队</span>
                <el-button size="small" link type="primary" @click.stop="openLimitConfig(gt)">限报配置</el-button>
                <div class="collapse-title-actions" @click.stop>
                  <el-button link type="primary" size="small" @click="openGtEdit(gt)">编辑</el-button>
                  <el-button link type="danger" size="small" @click="handleGtDelete(gt.id)">删除</el-button>
                </div>
              </div>
            </template>
            <div class="nested-section">
              <div class="tab-toolbar">
                <div></div>
                <el-button type="primary" size="small" @click="openTeamAdd(gt.id)">+ 新增代表队</el-button>
              </div>
              <el-empty v-if="getTeamsByGroupType(gt.id).length === 0" description="暂无代表队，点击上方按钮添加" :image-size="60" />
              <div v-else class="team-card-list">
                <div v-for="team in getTeamsByGroupType(gt.id)" :key="team.id" class="team-card" @click="router.push(`/meeting/${meetingId}/team/${team.id}`)">
                  <div class="team-card-header">
                    <span class="team-card-name">{{ team.name }}</span>
                    <el-tag type="info" size="small">{{ getTeamParticipants(team.id).length }} 人</el-tag>
                  </div>
                  <div class="team-card-info">
                    <span v-if="team.leader">领队：{{ team.leader }}</span>
                    <span v-if="team.coach">教练：{{ team.coach }}</span>
                    <span v-if="!team.leader && !team.coach" style="color:#c0c4cc">暂无领队/教练信息</span>
                  </div>
                </div>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-tab-pane>

      <!-- ======== 参赛人员 ======== -->
      <el-tab-pane label="参赛人员" name="participant">
        <div class="tab-toolbar">
          <el-input v-model="participantSearch" placeholder="搜索姓名/学号/学院" clearable style="width:220px" />
          <el-button type="primary" size="small" @click="openParticipantAdd">+ 新增人员</el-button>
        </div>
        <el-table :data="filteredParticipants" stripe border size="small">
          <el-table-column prop="userCode" label="学号/工号" width="120" />
          <el-table-column label="姓名" width="100">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="router.push(`/meeting/${meetingId}/participant/${row.id}`)">{{ row.name }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="gender" label="性别" width="60" />
          <el-table-column prop="phone" label="电话" width="130" />
          <el-table-column prop="teamName" label="代表队" width="120" />
          <el-table-column prop="college" label="学院" />
          <el-table-column prop="major" label="专业" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="router.push(`/meeting/${meetingId}/participant/${row.id}`)">详情</el-button>
              <el-button link type="primary" size="small" @click="openParticipantEdit(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleParticipantDelete(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ======== 公告通知 ======== -->
      <el-tab-pane label="公告通知" name="notice">
        <div class="tab-toolbar">
          <div></div>
          <el-button type="primary" size="small" @click="openNoticeAdd">+ 新增公告</el-button>
        </div>
        <el-table :data="notices" stripe border size="small">
          <el-table-column prop="title" label="标题" />
          <el-table-column label="内容" show-overflow-tooltip>
            <template #default="{ row }">{{ row.content }}</template>
          </el-table-column>
          <el-table-column label="附件" width="120">
            <template #default="{ row }">
              <a v-if="row.fileName" :href="row.fileUrl" target="_blank" style="color:#409eff;font-size:12px">{{ row.fileName }}</a>
              <span v-else style="color:#ccc">-</span>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openNoticeEdit(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleNoticeDelete(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ======== 成绩管理 ======== -->
      <el-tab-pane label="成绩管理" name="result">
        <div class="tab-toolbar">
          <div class="tab-toolbar-left">
            <el-input v-model="resultSearch" placeholder="搜索参赛者/项目" clearable style="width:200px" />
            <el-select v-model="resultFilterSchedule" placeholder="全部赛次" clearable style="width:140px">
              <el-option v-for="sch in schedules" :key="sch.id" :label="sch.name" :value="sch.id" />
            </el-select>
          </div>
          <div class="tab-toolbar-left">
            <el-button size="small" @click="openHistoryDialog">查看历史记录</el-button>
            <el-button type="primary" size="small" @click="openResultAdd">+ 录入成绩</el-button>
          </div>
        </div>
        <div v-for="group in getResultsGroupedBySchedule()" :key="group.scheduleId ?? 'none'" class="result-group">
          <div class="result-group-title">{{ group.scheduleName }}（{{ group.items.length }} 条）</div>
          <el-table :data="group.items" stripe border size="small">
            <el-table-column prop="participantName" label="参赛者" width="100" />
            <el-table-column prop="eventName" label="项目" />
            <el-table-column label="成绩" width="130">
              <template #default="{ row }">{{ formatScore(row) }}</template>
            </el-table-column>
            <el-table-column label="积分" prop="points" width="80" />
            <el-table-column label="创建时间" width="170">
              <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openResultEdit(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="handleResultDelete(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-if="filteredResults.length === 0" description="暂无成绩数据" />
      </el-tab-pane>
    </el-tabs>

    <!-- ======== 弹窗：赛程轮次 ======== -->
    <el-dialog v-model="scheduleDialogVisible" :title="scheduleForm.id ? '编辑轮次' : '新增轮次'" width="400px" destroy-on-close>
      <el-form :model="scheduleForm" label-width="80px">
        <el-form-item label="轮次名称" required>
          <el-input v-model="scheduleForm.name" placeholder="如：预赛、半决赛、决赛" />
        </el-form-item>
        <el-form-item label="轮次序号">
          <el-input-number v-model="scheduleForm.sort" :min="0" />
          <span style="margin-left:8px;color:#999;font-size:12px">越小越靠前（如预赛1、复赛2、决赛3）</span>
        </el-form-item>
        <el-form-item label="状态" v-if="scheduleForm.id">
          <el-select v-model="scheduleForm.status" style="width:100%">
            <el-option label="进行中" :value="0" />
            <el-option label="已结束" :value="1" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleScheduleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- ======== 弹窗：组别 ======== -->
    <el-dialog v-model="gtDialogVisible" :title="gtForm.id ? '编辑组别' : '新增组别'" width="400px" destroy-on-close>
      <el-form :model="gtForm" label-width="80px">
        <el-form-item label="组别名称" required>
          <el-input v-model="gtForm.name" placeholder="如：学生组、教职工组" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="gtDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGtSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- ======== 弹窗：限报配置 ======== -->
    <el-dialog v-model="limitDialogVisible" title="限报配置" width="640px" destroy-on-close>
      <!-- 规则A：每代表队限报 -->
      <div style="font-weight:600;margin-bottom:4px">每代表队限报</div>
      <div style="margin-bottom:8px;color:#999">
        勾选受限项目（按类别分组），设置每代表队在选定项目中最多可报人数。
      </div>
      <div v-for="(evs, cat) in eventsByCategory" :key="'a-'+cat" style="margin-bottom:12px">
        <div style="margin-bottom:6px">
          <el-checkbox :model-value="isCategoryAllChecked(cat,'eventIds')" @change="(v:any)=>toggleCategoryAll(cat,v,'eventIds')">{{ cat }}（全选）</el-checkbox>
        </div>
        <el-checkbox-group v-model="limitForm.eventIds" style="margin-left:24px">
          <el-checkbox v-for="ev in evs" :key="ev.id" :value="ev.id">{{ ev.name }}</el-checkbox>
        </el-checkbox-group>
      </div>
      <el-form-item label="每代表队限报人数" style="margin-top:8px">
        <el-input-number v-model="limitForm.perTeamLimit" :min="0" />
        <span style="margin-left:8px;color:#999">0 = 不限</span>
      </el-form-item>

      <el-divider />

      <!-- 规则B：每人限报 -->
      <div style="font-weight:600;margin-bottom:4px">每人限报</div>
      <div style="margin-bottom:8px;color:#999">
        勾选受限项目（按类别分组），设置每人在选定项目中最多可报项目数。
      </div>
      <div v-for="(evs, cat) in eventsByCategory" :key="'b-'+cat" style="margin-bottom:12px">
        <div style="margin-bottom:6px">
          <el-checkbox :model-value="isCategoryAllChecked(cat,'personEventIds')" @change="(v:any)=>toggleCategoryAll(cat,v,'personEventIds')">{{ cat }}（全选）</el-checkbox>
        </div>
        <el-checkbox-group v-model="limitForm.personEventIds" style="margin-left:24px">
          <el-checkbox v-for="ev in evs" :key="ev.id" :value="ev.id">{{ ev.name }}</el-checkbox>
        </el-checkbox-group>
      </div>
      <el-form-item label="每人限报项目数" style="margin-top:8px">
        <el-input-number v-model="limitForm.perPersonLimit" :min="0" />
        <span style="margin-left:8px;color:#999">0 = 不限</span>
      </el-form-item>
      <template #footer>
        <el-button @click="limitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveLimit">保存</el-button>
      </template>
    </el-dialog>

    <!-- ======== 弹窗：代表队 ======== -->
    <el-dialog v-model="teamDialogVisible" :title="teamForm.id ? '编辑代表队' : '新增代表队'" width="480px" destroy-on-close>
      <el-form :model="teamForm" label-width="80px">
        <el-form-item label="队名" required>
          <el-input v-model="teamForm.name" placeholder="如：计算机学院代表队" />
        </el-form-item>
        <el-form-item label="领队人">
          <el-input v-model="teamForm.leader" />
        </el-form-item>
        <el-form-item label="教练员">
          <el-input v-model="teamForm.coach" />
        </el-form-item>
        <el-form-item label="总分">
          <el-input-number v-model="teamForm.totalScore" :precision="2" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="teamDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTeamSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- ======== 弹窗：参赛人员 ======== -->

    <el-dialog v-model="participantDialogVisible" :title="participantForm.id ? '编辑人员' : '新增人员'" width="480px" destroy-on-close>
      <el-form :model="participantForm" label-width="90px">
        <el-form-item label="学号/工号" required>
          <el-input v-model="participantForm.userCode" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="participantForm.name" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="participantForm.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="电话" required>
          <el-input v-model="participantForm.phone" />
        </el-form-item>
        <el-form-item label="学院">
          <el-input v-model="participantForm.college" />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="participantForm.major" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="participantDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleParticipantSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- ======== 弹窗：分配人员到代表队 ======== -->
    <el-dialog v-model="assignDialogVisible" :title="`为「${assignTeamName}」分配人员`" width="560px" destroy-on-close>
      <p style="color:#999;font-size:13px;margin-bottom:12px">仅显示该运动会下未分配代表队的人员</p>
      <el-table :data="assignableParticipants" stripe border size="small" max-height="400"
        @selection-change="(rows: any[]) => assignSelectedIds = rows.map(r => r.id)">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="userCode" label="学号/工号" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column prop="teamName" label="代表队" width="120" />
        <el-table-column prop="college" label="学院" />
        <el-table-column prop="major" label="专业" />
      </el-table>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="assignSelectedIds.length === 0" @click="handleAssignSubmit">
          确认分配 ({{ assignSelectedIds.length }} 人)
        </el-button>
      </template>
    </el-dialog>

    <!-- ======== 弹窗：成绩录入 ======== -->
    <el-dialog v-model="resultDialogVisible" :title="resultForm.id ? '编辑成绩' : '录入成绩'" width="480px" destroy-on-close>
      <el-form :model="resultForm" label-width="90px">
        <el-form-item label="比赛项目" required>
          <el-select v-model="resultForm.eventId" placeholder="请选择项目" style="width:100%" :disabled="!!resultForm.id">
            <el-option v-for="e in events" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="赛次" required>
          <el-select v-model="resultForm.scheduleId" placeholder="请选择赛次" style="width:100%" :disabled="!!resultForm.id">
            <el-option
              v-for="schId in getScheduleIdsByEventId(resultForm.eventId)"
              :key="schId"
              :label="getScheduleNameById(schId)"
              :value="schId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="参赛人员" required>
          <el-select v-model="resultForm.participantId" placeholder="请选择参赛人员" filterable style="width:100%" :disabled="!!resultForm.id">
            <el-option v-for="p in participants" :key="p.id" :label="`${p.name}（${p.userCode}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="成绩" v-if="selectedEventCategory === '径赛'">
          <div style="display:flex;align-items:center;gap:4px;flex-wrap:wrap">
            <el-input-number v-model="resultInput.minutes" :min="0" :controls="false" style="width:64px" />
            <span style="margin-right:8px">分</span>
            <el-input-number v-model="resultInput.seconds" :min="0" :max="59" :controls="false" style="width:64px" />
            <span style="margin-right:8px">秒</span>
            <el-input-number v-model="resultInput.millis" :min="0" :max="999" :controls="false" style="width:84px" />
            <span>毫秒</span>
          </div>
        </el-form-item>
        <el-form-item label="成绩" v-else-if="selectedEventCategory === '田赛'">
          <div style="display:flex;align-items:center;gap:4px;flex-wrap:wrap">
            <el-input-number v-model="resultInput.meters" :min="0" :controls="false" style="width:96px" />
            <span style="margin-right:8px">米</span>
            <el-input-number v-model="resultInput.centimeters" :min="0" :max="99" :controls="false" style="width:72px" />
            <span>厘米</span>
          </div>
        </el-form-item>
        <el-form-item label="成绩" v-else>
          <span style="color:#999">该项目（{{ selectedEventCategory || '未选择项目' }}）暂不支持直接录入成绩，可通过积分记录</span>
        </el-form-item>
        <el-form-item label="积分">
          <el-input-number v-model="resultForm.points" :min="0" controls-position="right" style="width:100%" placeholder="该成绩对应积分（用于代表队总分）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resultDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResultSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- ======== 弹窗：成绩历史记录 ======== -->
    <el-dialog v-model="historyDialogVisible" title="成绩历史记录" width="700px" destroy-on-close>
      <div style="margin-bottom:12px">
        <el-select v-model="historyFilterEvent" placeholder="按项目筛选" clearable style="width:240px" @change="fetchHistoryResults">
          <el-option v-for="e in events" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
      </div>
      <el-table :data="historyResults" stripe border size="small" max-height="400">
        <el-table-column prop="participantName" label="参赛者" width="100" />
        <el-table-column prop="eventName" label="项目" />
        <el-table-column label="成绩" width="130">
          <template #default="{ row }">{{ formatScore(row) }}</template>
        </el-table-column>
        <el-table-column label="积分" prop="points" width="80" />
        <el-table-column label="录入时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- ======== 弹窗：公告通知 ======== -->
    <el-dialog v-model="noticeDialogVisible" :title="noticeForm.id ? '编辑公告' : '新增公告'" width="560px" destroy-on-close>
      <el-form :model="noticeForm" label-width="70px">
        <el-form-item label="标题" required>
          <el-input v-model="noticeForm.title" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="noticeForm.content" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="附件">
          <div v-if="noticeForm.fileName" class="notice-file-info">
            <a :href="noticeForm.fileUrl || undefined" target="_blank" style="color:#409eff">{{ noticeForm.fileName }}</a>
            <el-button link type="danger" size="small" @click="handleNoticeFileRemove" style="margin-left:8px">移除</el-button>
          </div>
          <div v-else>
            <input type="file" @change="handleNoticeFileUpload" :disabled="noticeUploading" style="width:100%" />
            <span v-if="noticeUploading" style="color:#999;font-size:12px">上传中...</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleNoticeSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.detail-page {
  padding: 20px;
}
.info-bar {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}
.info-bar-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.info-title {
  font-size: 20px;
  margin: 0;
}
.info-meta {
  display: flex;
  gap: 24px;
  font-size: 13px;
  color: #888;
  flex-wrap: wrap;
}
.main-tabs {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}
.tab-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.tab-toolbar-left {
  display: flex;
  gap: 8px;
}
.toolbar-hint {
  color: #999;
  font-size: 13px;
  line-height: 32px;
}

/* 嵌套折叠面板样式 */
.nested-collapse {
  border: none;
}
.nested-collapse :deep(.el-collapse-item__header) {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 0 16px;
  margin-bottom: 8px;
  border: 1px solid #ebeef5;
  height: 44px;
  line-height: 44px;
}
.nested-collapse :deep(.el-collapse-item__wrap) {
  border: none;
  margin-bottom: 12px;
}
.nested-collapse :deep(.el-collapse-item__content) {
  padding: 0;
}
.collapse-title {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}
.collapse-title-name {
  font-weight: 600;
  font-size: 14px;
}
.collapse-count {
  font-size: 12px;
  color: #909399;
}
.collapse-title-actions {
  margin-left: auto;
  display: flex;
  gap: 4px;
}
.nested-section {
  padding: 0 8px 8px;
}

/* 代表队卡片网格 */
.team-card-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
}
.team-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px;
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
}
.team-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}
.team-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.team-card-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.team-card-info {
  font-size: 12px;
  color: #909399;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* 赛程卡片列表 */
.schedule-card-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}
.schedule-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
}
.schedule-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}
.schedule-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.schedule-card-name {
  font-size: 15px;
  font-weight: 600;
}
.schedule-card-info {
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}
.schedule-card-actions {
  display: flex;
  gap: 4px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

/* 成绩分组 */
.result-group { margin-bottom: 20px; }
.result-group-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
  line-height: 20px;
}

.notice-file-info {
  display: flex;
  align-items: center;
  font-size: 13px;
}
</style>
