<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMeetingDetail } from '@/api/meeting'
import type { SportsMeeting } from '@/api/meeting'
import { getScheduleList, updateSchedule } from '@/api/schedule'
import type { Schedule } from '@/api/schedule'
import { getEventList, addEvent, updateEvent, deleteEvent } from '@/api/event'
import type { Event } from '@/api/event'
import { getGroupTypeList } from '@/api/groupType'
import type { GroupType } from '@/api/groupType'
import { getEventSchedulesBySchedule, getEventSchedules, saveEventSchedules } from '@/api/eventSchedule'
import { getRegistrationList } from '@/api/registration'

const route = useRoute()
const router = useRouter()
const scheduleId = Number(route.params.scheduleId)
const meetingId = Number(route.params.meetingId)

const meeting = ref<SportsMeeting | null>(null)
const schedule = ref<Schedule | null>(null)
const allSchedules = ref<Schedule[]>([])
const events = ref<Event[]>([])
const groupTypes = ref<GroupType[]>([])
const regCountMap = ref<Record<string, number>>({})
const eventDialogVisible = ref(false)
const eventForm = ref<Partial<Event>>({})
const eventFormScheduleIds = ref<number[]>([])

const scheduleStatusMap: Record<number, string> = { 0: '进行中', 1: '已结束' }

async function fetchMeeting() {
  try {
    const res: any = await getMeetingDetail(meetingId)
    meeting.value = res.data || res
  } catch { /* ignore */ }
}

async function fetchSchedule() {
  try {
    const res: any = await getScheduleList(meetingId)
    const list: Schedule[] = res.data || res || []
    allSchedules.value = list
    schedule.value = list.find((s: Schedule) => s.id === scheduleId) || null
  } catch { /* ignore */ }
}

async function fetchEvents() {
  try {
    const assocRes: any = await getEventSchedulesBySchedule(scheduleId)
    const associations: any[] = assocRes.data || assocRes || []
    const eventIds = new Set(associations.map((a: any) => a.eventId))
    if (eventIds.size === 0) {
      events.value = []
      return
    }
    const res: any = await getEventList({ sportsMeetingId: meetingId })
    events.value = (res.data || res || []).filter((e: Event) => eventIds.has(e.id))
  } catch { /* ignore */ }
}

async function fetchRegCounts() {
  try {
    const res: any = await getRegistrationList(meetingId)
    const list: any[] = res.data || res || []
    const map: Record<string, number> = {}
    list.forEach((r: any) => {
      if ((r.status === 0 || r.status === 1) && r.scheduleId) {
        const key = `${r.eventId}-${r.scheduleId}`
        map[key] = (map[key] || 0) + 1
      }
    })
    regCountMap.value = map
  } catch { /* ignore */ }
}

async function fetchGroupTypes() {
  try {
    const res: any = await getGroupTypeList(meetingId)
    groupTypes.value = res.data || res || []
  } catch { /* ignore */ }
}

function openEventAdd() {
  eventForm.value = { sportsMeetingId: meetingId, name: '', category: '径赛', gender: '不限', groupTypeId: groupTypes.value[0]?.id ?? null, allowRegister: 1, registerLimit: 0, status: 0 }
  eventFormScheduleIds.value = [scheduleId]
  eventDialogVisible.value = true
}
async function openEventEdit(row: Event) {
  eventForm.value = { ...row }
  try {
    const res: any = await getEventSchedules(row.id)
    const list: any[] = res.data || res || []
    eventFormScheduleIds.value = list.map((a: any) => a.scheduleId)
  } catch { eventFormScheduleIds.value = [] }
  eventDialogVisible.value = true
}
async function handleEventSubmit() {
  try {
    if (eventForm.value.id) {
      await updateEvent(eventForm.value)
      await saveEventSchedules(eventForm.value.id, eventFormScheduleIds.value)
    } else {
      const res: any = await addEvent(eventForm.value)
      const newId = res.data
      if (newId) {
        await saveEventSchedules(newId, eventFormScheduleIds.value)
      }
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

async function toggleScheduleStatus() {
  if (!schedule.value) return
  try {
    const newStatus = schedule.value.status === 0 ? 1 : 0
    await updateSchedule({ ...schedule.value, status: newStatus })
    ElMessage.success('状态更新成功')
    fetchSchedule()
  } catch { ElMessage.error('操作失败') }
}

function goBack() {
  router.push(`/meeting/${meetingId}`)
}

onMounted(() => {
  fetchMeeting()
  fetchSchedule()
  fetchEvents()
  fetchRegCounts()
  fetchGroupTypes()
})
</script>

<template>
  <div class="schedule-detail-page" v-if="schedule">
    <!-- 顶部信息栏 -->
    <div class="info-bar">
      <div class="info-bar-top">
        <div class="info-bar-left">
          <el-button link @click="goBack" style="margin-right:12px;font-size:14px">← 返回</el-button>
          <h2 class="info-title">{{ schedule.name }}</h2>
          <el-tag :type="schedule.status === 0 ? 'primary' : 'info'" size="small">{{ scheduleStatusMap[schedule.status] }}</el-tag>
        </div>
        <el-button size="small" @click="toggleScheduleStatus">
          {{ schedule.status === 0 ? '标记为已结束' : '标记为进行中' }}
        </el-button>
      </div>
      <div class="info-meta" v-if="meeting">
        <span>{{ meeting.name }}</span>
        <span>{{ events.length }} 个项目</span>
      </div>
    </div>

    <!-- 项目列表 -->
    <div class="content-card">
      <div class="tab-toolbar">
        <span class="toolbar-hint">管理该轮次下的比赛项目</span>
        <el-button type="primary" size="small" @click="openEventAdd">+ 新增项目</el-button>
      </div>
      <el-table v-if="events.length > 0" :data="events" stripe border size="small">
        <el-table-column prop="name" label="项目名称" />
        <el-table-column prop="category" label="类别" width="80" />
        <el-table-column prop="gender" label="性别" width="70" />
        <el-table-column prop="groupTypeName" label="组别" width="80" />
        <el-table-column label="开放报名" width="80">
          <template #default="{ row }">{{ row.allowRegister === 1 ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="报名/上限" width="100">
          <template #default="{ row }">{{ regCountMap[`${row.id}-${scheduleId}`] || 0 }} / {{ row.registerLimit === 0 ? '不限' : row.registerLimit }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" size="small" @click="router.push(`/meeting/${meetingId}/schedule/${scheduleId}/event/${row.id}`)">查看报名</el-button>
            <el-button link type="primary" size="small" @click="openEventEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleEventDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无项目，点击上方按钮添加" />
    </div>

    <!-- 弹窗：比赛项目 -->
    <el-dialog v-model="eventDialogVisible" :title="eventForm.id ? '编辑项目' : '新增项目'" width="480px" destroy-on-close>
      <el-form :model="eventForm" label-width="90px">
        <el-form-item label="项目名称" required>
          <el-input v-model="eventForm.name" />
        </el-form-item>
        <el-form-item label="参赛轮次" required>
          <el-select v-model="eventFormScheduleIds" multiple placeholder="请选择轮次" style="width:100%">
            <el-option v-for="sch in allSchedules" :key="sch.id" :label="sch.name" :value="sch.id" />
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
          <el-select v-model="eventForm.groupTypeId" style="width:100%" placeholder="请选择组别">
            <el-option v-for="gt in groupTypes" :key="gt.id" :label="gt.name" :value="gt.id" />
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
  </div>
</template>

<style scoped>
.schedule-detail-page { padding: 20px; }
.info-bar {
  background: #fff; border-radius: 8px; padding: 20px;
  margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.info-bar-top {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px;
}
.info-bar-left { display: flex; align-items: center; }
.info-title { font-size: 18px; margin: 0; }
.info-meta { display: flex; gap: 24px; font-size: 13px; color: #888; }
.content-card {
  background: #fff; border-radius: 8px; padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.tab-toolbar {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;
}
.toolbar-hint { color: #999; font-size: 13px; line-height: 32px; }
</style>
