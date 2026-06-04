<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMeetingDetail } from '@/api/meeting'
import type { SportsMeeting } from '@/api/meeting'
import { getEventList, addEvent, updateEvent, deleteEvent } from '@/api/event'
import type { Event } from '@/api/event'
import { getParticipantList, addParticipant, updateParticipant, deleteParticipant } from '@/api/participant'
import type { Participant } from '@/api/participant'
import { getScheduleList, addSchedule, updateSchedule, deleteSchedule } from '@/api/schedule'
import type { Schedule } from '@/api/schedule'
import { getNoticeList, addNotice, updateNotice, deleteNotice } from '@/api/notice'
import type { Notice } from '@/api/notice'
import { getRegistrationList, updateRegistration, deleteRegistration } from '@/api/registration'
import type { RegistrationVO } from '@/api/registration'

const route = useRoute()
const router = useRouter()
const meetingId = Number(route.params.id)
const meeting = ref<SportsMeeting | null>(null)
const activeTab = ref('event')

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
  0: { label: '报名中', color: '#1890ff' },
  1: { label: '进行中', color: '#52c41a' },
  2: { label: '已结束', color: '#ff4d4f' },
}

function formatDate(d: string) {
  return d ? d.substring(0, 10) : ''
}

// ============ 比赛项目 ============
const events = ref<Event[]>([])
const eventFilterCategory = ref('')
const eventFilterGender = ref('')
const eventDialogVisible = ref(false)
const eventForm = ref<Partial<Event>>({})

async function fetchEvents() {
  try {
    const res: any = await getEventList({ sportsMeetingId: meetingId })
    events.value = res.data || res || []
  } catch { /* ignore */ }
}

const filteredEvents = ref<Event[]>([])
watch([events, eventFilterCategory, eventFilterGender], () => {
  filteredEvents.value = events.value.filter((e) => {
    if (eventFilterCategory.value && e.category !== eventFilterCategory.value) return false
    if (eventFilterGender.value && e.gender !== eventFilterGender.value) return false
    return true
  })
}, { immediate: true })

function openEventAdd() {
  eventForm.value = { sportsMeetingId: meetingId, scheduleId: 0, name: '', category: '径赛', gender: '不限', groupType: '学生组', allowRegister: 1, registerLimit: 0, status: 0 }
  eventDialogVisible.value = true
}
function openEventEdit(row: Event) {
  eventForm.value = { ...row }
  eventDialogVisible.value = true
}
async function handleEventSubmit() {
  try {
    if (eventForm.value.id) {
      await updateEvent(eventForm.value)
    } else {
      await addEvent(eventForm.value)
    }
    ElMessage.success('操作成功')
    eventDialogVisible.value = false
    fetchEvents()
  } catch { ElMessage.error('操作失败') }
}
async function handleEventDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该项目？', '提示', { type: 'warning' })
    await deleteEvent(id)
    ElMessage.success('删除成功')
    fetchEvents()
  } catch { /* cancel */ }
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

// ============ 赛程轮次 ============
const schedules = ref<Schedule[]>([])
const scheduleDialogVisible = ref(false)
const scheduleForm = ref<Partial<Schedule>>({})

async function fetchSchedules() {
  try {
    const res: any = await getScheduleList(meetingId)
    schedules.value = res.data || res || []
  } catch { /* ignore */ }
}

function openScheduleAdd() {
  scheduleForm.value = { sportsMeetingId: meetingId, name: '', status: 0 }
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
    await ElMessageBox.confirm('确定删除该轮次？', '提示', { type: 'warning' })
    await deleteSchedule(id)
    ElMessage.success('删除成功')
    fetchSchedules()
  } catch { /* cancel */ }
}

const scheduleStatusMap: Record<number, string> = { 0: '进行中', 1: '已结束' }

// ============ 公告通知 ============
const notices = ref<Notice[]>([])
const noticeDialogVisible = ref(false)
const noticeForm = ref<Partial<Notice>>({})

async function fetchNotices() {
  try {
    const res: any = await getNoticeList(meetingId)
    notices.value = res.data || res || []
  } catch { /* ignore */ }
}

function openNoticeAdd() {
  noticeForm.value = { sportsMeetingId: meetingId, title: '', content: '' }
  noticeDialogVisible.value = true
}
function openNoticeEdit(row: Notice) {
  noticeForm.value = { ...row }
  noticeDialogVisible.value = true
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

// ============ 报名记录 ============
const registrations = ref<RegistrationVO[]>([])
const regFilterEvent = ref('')
const regFilterStatus = ref<number | undefined>(undefined)

async function fetchRegistrations() {
  try {
    const res: any = await getRegistrationList(meetingId)
    registrations.value = res.data || res || []
  } catch { /* ignore */ }
}

const filteredRegistrations = ref<RegistrationVO[]>([])
watch([registrations, regFilterEvent, regFilterStatus], () => {
  filteredRegistrations.value = registrations.value.filter((r) => {
    if (regFilterEvent.value && r.eventName !== regFilterEvent.value) return false
    if (regFilterStatus.value !== undefined && regFilterStatus.value !== '' && r.status !== regFilterStatus.value) return false
    return true
  })
}, { immediate: true })

const regStatusMap: Record<number, string> = { 0: '已报名', 1: '已晋级', 2: '已取消' }
const regStatusColor: Record<number, string> = { 0: '#409eff', 1: '#67c23a', 2: '#909399' }

async function handleRegStatusChange(id: number, status: number) {
  try {
    await updateRegistration(id, status)
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

// ============ Tab 切换加载数据 ============
const loadedTabs = new Set<string>()

function onTabChange(tab: string) {
  if (loadedTabs.has(tab)) return
  loadedTabs.add(tab)
  if (tab === 'event') fetchEvents()
  else if (tab === 'participant') fetchParticipants()
  else if (tab === 'schedule') fetchSchedules()
  else if (tab === 'notice') fetchNotices()
  else if (tab === 'registration') fetchRegistrations()
}

onMounted(() => {
  fetchMeeting()
  loadedTabs.add('event')
  fetchEvents()
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

      <!-- ======== 比赛项目 ======== -->
      <el-tab-pane label="比赛项目" name="event">
        <div class="tab-toolbar">
          <div class="tab-toolbar-left">
            <el-select v-model="eventFilterCategory" placeholder="类别" clearable style="width:100px">
              <el-option label="径赛" value="径赛" />
              <el-option label="田赛" value="田赛" />
              <el-option label="趣味赛" value="趣味赛" />
            </el-select>
            <el-select v-model="eventFilterGender" placeholder="性别" clearable style="width:100px">
              <el-option label="男" value="男" />
              <el-option label="女" value="女" />
              <el-option label="不限" value="不限" />
            </el-select>
          </div>
          <el-button type="primary" size="small" @click="openEventAdd">+ 新增项目</el-button>
        </div>
        <el-table :data="filteredEvents" stripe border size="small">
          <el-table-column prop="name" label="项目名称" />
          <el-table-column prop="category" label="类别" width="80" />
          <el-table-column prop="gender" label="性别" width="70" />
          <el-table-column prop="groupType" label="组别" width="80" />
          <el-table-column label="开放报名" width="80">
            <template #default="{ row }">{{ row.allowRegister === 1 ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="人数上限" width="80">
            <template #default="{ row }">{{ row.registerLimit === 0 ? '不限' : row.registerLimit }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openEventEdit(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleEventDelete(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ======== 参赛人员 ======== -->
      <el-tab-pane label="参赛人员" name="participant">
        <div class="tab-toolbar">
          <el-input v-model="participantSearch" placeholder="搜索姓名/学号/学院" clearable style="width:220px" />
          <el-button type="primary" size="small" @click="openParticipantAdd">+ 新增人员</el-button>
        </div>
        <el-table :data="filteredParticipants" stripe border size="small">
          <el-table-column prop="userCode" label="学号/工号" width="120" />
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="gender" label="性别" width="60" />
          <el-table-column prop="phone" label="电话" width="130" />
          <el-table-column prop="college" label="学院" />
          <el-table-column prop="major" label="专业" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openParticipantEdit(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleParticipantDelete(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ======== 赛程轮次 ======== -->
      <el-tab-pane label="赛程轮次" name="schedule">
        <div class="tab-toolbar">
          <div></div>
          <el-button type="primary" size="small" @click="openScheduleAdd">+ 新增轮次</el-button>
        </div>
        <el-table :data="schedules" stripe border size="small">
          <el-table-column prop="name" label="轮次名称" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'primary' : 'info'" size="small">{{ scheduleStatusMap[row.status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openScheduleEdit(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleScheduleDelete(row.id)">删除</el-button>
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

      <!-- ======== 报名记录 ======== -->
      <el-tab-pane label="报名记录" name="registration">
        <div class="tab-toolbar">
          <div class="tab-toolbar-left">
            <el-select v-model="regFilterStatus" placeholder="全部状态" clearable style="width:120px">
              <el-option label="已报名" :value="0" />
              <el-option label="已晋级" :value="1" />
              <el-option label="已取消" :value="2" />
            </el-select>
          </div>
        </div>
        <el-table :data="filteredRegistrations" stripe border size="small">
          <el-table-column prop="participantName" label="参赛者" width="100" />
          <el-table-column prop="eventName" label="项目" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :color="regStatusColor[row.status]" style="border:none;color:#fff" size="small">{{ regStatusMap[row.status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="报名时间" width="170">
            <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-select
                :model-value="row.status"
                size="small"
                style="width:90px"
                @change="(val: number) => handleRegStatusChange(row.id, val)"
              >
                <el-option label="已报名" :value="0" />
                <el-option label="已晋级" :value="1" />
                <el-option label="已取消" :value="2" />
              </el-select>
              <el-button link type="danger" size="small" @click="handleRegDelete(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- ======== 弹窗：比赛项目 ======== -->
    <el-dialog v-model="eventDialogVisible" :title="eventForm.id ? '编辑项目' : '新增项目'" width="480px" destroy-on-close>
      <el-form :model="eventForm" label-width="90px">
        <el-form-item label="项目名称" required>
          <el-input v-model="eventForm.name" />
        </el-form-item>
        <el-form-item label="所属轮次">
          <el-select v-model="eventForm.scheduleId" placeholder="选择轮次" style="width:100%">
            <el-option v-for="s in schedules" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="eventForm.category" style="width:100%">
            <el-option label="径赛" value="径赛" />
            <el-option label="田赛" value="田赛" />
            <el-option label="趣味赛" value="趣味赛" />
          </el-select>
        </el-form-item>
        <el-form-item label="性别限制">
          <el-select v-model="eventForm.gender" style="width:100%">
            <el-option label="不限" value="不限" />
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="组别">
          <el-select v-model="eventForm.groupType" style="width:100%">
            <el-option label="学生组" value="学生组" />
            <el-option label="教工组" value="教工组" />
          </el-select>
        </el-form-item>
        <el-form-item label="开放报名">
          <el-switch v-model="eventForm.allowRegister" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="人数上限">
          <el-input-number v-model="eventForm.registerLimit" :min="0" />
          <span style="margin-left:8px;color:#999;font-size:12px">0 表示不限</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="eventDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEventSubmit">确定</el-button>
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

    <!-- ======== 弹窗：赛程轮次 ======== -->
    <el-dialog v-model="scheduleDialogVisible" :title="scheduleForm.id ? '编辑轮次' : '新增轮次'" width="400px" destroy-on-close>
      <el-form :model="scheduleForm" label-width="80px">
        <el-form-item label="轮次名称" required>
          <el-input v-model="scheduleForm.name" placeholder="如：预赛、半决赛、决赛" />
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

    <!-- ======== 弹窗：公告通知 ======== -->
    <el-dialog v-model="noticeDialogVisible" :title="noticeForm.id ? '编辑公告' : '新增公告'" width="560px" destroy-on-close>
      <el-form :model="noticeForm" label-width="70px">
        <el-form-item label="标题" required>
          <el-input v-model="noticeForm.title" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="noticeForm.content" type="textarea" :rows="8" />
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
</style>
