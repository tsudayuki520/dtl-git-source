<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getParticipantList } from '@/api/participant'
import type { Participant } from '@/api/participant'
import { getRegistrationListByParticipant } from '@/api/registration'
import type { RegistrationVO } from '@/api/registration'

const route = useRoute()
const router = useRouter()
const meetingId = Number(route.params.meetingId)
const participantId = Number(route.params.participantId)

const participant = ref<Participant | null>(null)
const registrations = ref<RegistrationVO[]>([])

const statusMap: Record<number, { label: string; type: 'info' | 'success' | 'danger' }> = {
  0: { label: '已报名', type: 'info' },
  1: { label: '已晋级', type: 'success' },
  2: { label: '已取消', type: 'danger' },
}

function statusLabel(s: number) {
  return statusMap[s]?.label ?? '未知'
}
function statusType(s: number) {
  return statusMap[s]?.type ?? 'info'
}

function formatDate(t?: string) {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 16)
}

// 按项目分组（同一项目可能有预赛+决赛多条记录）
const groupedByEvent = computed(() => {
  const map = new Map<number, { eventId: number; eventName: string; items: RegistrationVO[] }>()
  for (const r of registrations.value) {
    const g = map.get(r.eventId)
    if (g) {
      g.items.push(r)
    } else {
      map.set(r.eventId, { eventId: r.eventId, eventName: r.eventName ?? '未知项目', items: [r] })
    }
  }
  return Array.from(map.values())
})

async function fetchParticipant() {
  try {
    const res: any = await getParticipantList(meetingId)
    const list: Participant[] = res.data || res || []
    participant.value = list.find(p => p.id === participantId) || null
  } catch { /* ignore */ }
}

async function fetchRegistrations() {
  try {
    const res: any = await getRegistrationListByParticipant(participantId)
    registrations.value = res.data || res || []
  } catch { /* ignore */ }
}

function goBack() {
  router.push(`/meeting/${meetingId}`)
}

onMounted(() => {
  fetchParticipant()
  fetchRegistrations()
})
</script>

<template>
  <div class="participant-detail-page">
    <!-- 顶部信息栏 -->
    <div class="info-bar" v-if="participant">
      <div class="info-bar-top">
        <div class="info-bar-left">
          <el-button link @click="goBack" style="margin-right:12px;font-size:14px">← 返回</el-button>
          <h2 class="info-title">{{ participant.name }}</h2>
          <el-tag v-if="participant.teamName" size="small">{{ participant.teamName }}</el-tag>
        </div>
      </div>
      <div class="info-meta">
        <span>学号/工号：{{ participant.userCode }}</span>
        <span>性别：{{ participant.gender }}</span>
        <span v-if="participant.phone">电话：{{ participant.phone }}</span>
        <span v-if="participant.college">学院：{{ participant.college }}</span>
        <span v-if="participant.major">专业：{{ participant.major }}</span>
        <span>已报项目：{{ groupedByEvent.length }} 个 / 共 {{ registrations.length }} 条记录</span>
      </div>
    </div>

    <!-- 项目卡片 -->
    <div class="content-card">
      <div class="tab-toolbar">
        <span class="toolbar-hint">该参赛人员报名的所有项目</span>
      </div>

      <div v-if="groupedByEvent.length > 0" class="event-grid">
        <div v-for="group in groupedByEvent" :key="group.eventId" class="event-card">
          <div class="event-card-header">
            <span class="event-name">{{ group.eventName }}</span>
            <el-tag size="small" type="warning">{{ group.items.length }} 个赛次</el-tag>
          </div>
          <div class="event-card-body">
            <div v-for="item in group.items" :key="item.id" class="schedule-row">
              <span class="schedule-name">{{ item.scheduleName ?? '未分配赛次' }}</span>
              <el-tag size="small" :type="statusType(item.status)">{{ statusLabel(item.status) }}</el-tag>
              <span class="schedule-time">{{ formatDate(item.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-else description="该参赛人员暂无报名记录" />
    </div>
  </div>
</template>

<style scoped>
.participant-detail-page { padding: 20px; }
.info-bar {
  background: #fff; border-radius: 8px; padding: 20px;
  margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.info-bar-top {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px;
}
.info-bar-left { display: flex; align-items: center; gap: 8px; }
.info-title { font-size: 18px; margin: 0; }
.info-meta { display: flex; gap: 24px; font-size: 13px; color: #888; flex-wrap: wrap; }
.content-card {
  background: #fff; border-radius: 8px; padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.tab-toolbar {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
}
.toolbar-hint { color: #999; font-size: 13px; }

.event-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.event-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.2s;
}
.event-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
.event-card-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}
.event-name { font-size: 15px; font-weight: 600; color: #303133; }
.event-card-body { padding: 8px 16px; }
.schedule-row {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}
.schedule-row:last-child { border-bottom: none; }
.schedule-name { flex: 1; font-size: 13px; color: #606266; }
.schedule-time { font-size: 12px; color: #c0c4cc; }
</style>
