<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEventList } from '@/api/event'
import type { Event } from '@/api/event'
import { getRegistrationListByEvent, updateRegistration, deleteRegistration } from '@/api/registration'
import type { RegistrationVO } from '@/api/registration'

const route = useRoute()
const router = useRouter()
const meetingId = Number(route.params.meetingId)
const eventId = Number(route.params.eventId)

const eventInfo = ref<Event | null>(null)
const registrations = ref<RegistrationVO[]>([])
const regFilterStatus = ref<number | undefined>(undefined)

const regStatusMap: Record<number, string> = { 0: '已报名', 1: '已晋级', 2: '已取消' }
const regStatusType: Record<number, string> = { 0: 'primary', 1: 'success', 2: 'info' }

async function fetchEvent() {
  try {
    const res: any = await getEventList({ sportsMeetingId: meetingId })
    const list: Event[] = res.data || res || []
    eventInfo.value = list.find(e => e.id === eventId) || null
  } catch { /* ignore */ }
}

async function fetchRegistrations() {
  try {
    const res: any = await getRegistrationListByEvent(eventId)
    registrations.value = res.data || res || []
  } catch { /* ignore */ }
  filterRegistrations()
}

const filteredRegistrations = ref<RegistrationVO[]>([])

function filterRegistrations() {
  filteredRegistrations.value = registrations.value.filter(r => {
    if (regFilterStatus.value !== undefined && regFilterStatus.value !== null && r.status !== regFilterStatus.value) return false
    return true
  })
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

function goBack() {
  router.back()
}

onMounted(() => {
  fetchEvent()
  fetchRegistrations()
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
        <span>组别：{{ eventInfo.groupType }}</span>
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
      </div>
      <el-table v-if="filteredRegistrations.length > 0" :data="filteredRegistrations" stripe border size="small">
        <el-table-column prop="participantName" label="参赛者" width="120" />
        <el-table-column prop="eventName" label="项目" />
        <el-table-column prop="scheduleName" label="赛次" width="90" />
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
