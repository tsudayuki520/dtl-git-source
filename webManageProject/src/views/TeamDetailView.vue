<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMeetingDetail } from '@/api/meeting'
import type { SportsMeeting } from '@/api/meeting'
import { getTeamList, updateTeam, deleteTeam } from '@/api/team'
import type { Team } from '@/api/team'
import { getParticipantListByTeam, getParticipantList, updateParticipant } from '@/api/participant'
import type { Participant } from '@/api/participant'
import { getGroupTypeList } from '@/api/groupType'

const route = useRoute()
const router = useRouter()
const meetingId = Number(route.params.meetingId)
const teamId = Number(route.params.teamId)

const meeting = ref<SportsMeeting | null>(null)
const team = ref<Team | null>(null)
const groupTypeName = ref('')
const participants = ref<Participant[]>([])

// 分配人员弹窗
const assignDialogVisible = ref(false)
const allParticipants = ref<Participant[]>([])
const assignSelectedIds = ref<number[]>([])

async function fetchMeeting() {
  try {
    const res: any = await getMeetingDetail(meetingId)
    meeting.value = res.data || res
  } catch { /* ignore */ }
}

async function fetchTeam() {
  try {
    const res: any = await getTeamList({ sportsMeetingId: meetingId })
    const list: Team[] = res.data || res || []
    team.value = list.find((t: Team) => t.id === teamId) || null
    if (team.value?.groupTypeId) {
      const gtRes: any = await getGroupTypeList(meetingId)
      const gtList: any[] = gtRes.data || gtRes || []
      const gt = gtList.find((g: any) => g.id === team.value!.groupTypeId)
      groupTypeName.value = gt?.name || ''
    }
  } catch { /* ignore */ }
}

async function fetchParticipants() {
  try {
    const res: any = await getParticipantListByTeam(teamId)
    participants.value = res.data || res || []
  } catch { /* ignore */ }
}

function openEditDialog() {
  teamEditVisible.value = true
  teamEditForm.value = { ...team.value! }
}

const teamEditVisible = ref(false)
const teamEditForm = ref<Partial<Team>>({})

async function handleEditSubmit() {
  try {
    await updateTeam(teamEditForm.value)
    ElMessage.success('编辑成功')
    teamEditVisible.value = false
    fetchTeam()
  } catch { ElMessage.error('操作失败') }
}

async function handleTeamDelete() {
  try {
    await ElMessageBox.confirm('确定删除该代表队？', '提示', { type: 'warning' })
    await deleteTeam(teamId)
    ElMessage.success('删除成功')
    router.push(`/meeting/${meetingId}`)
  } catch { /* cancel */ }
}

async function handleRemoveFromTeam(participantId: number) {
  try {
    await ElMessageBox.confirm('确定将该人员移出代表队？', '提示', { type: 'warning' })
    await updateParticipant({ id: participantId, teamId: null })
    ElMessage.success('移出成功')
    fetchParticipants()
  } catch { /* cancel */ }
}

// 分配人员
async function openAssignDialog() {
  assignSelectedIds.value = []
  try {
    const res: any = await getParticipantList(meetingId)
    const all: Participant[] = res.data || res || []
    const currentIds = new Set(participants.value.map(p => p.id))
    const assignedIds = new Set<number>()
    // 过滤掉已分配其他代表队的
    all.forEach(p => {
      if (p.teamId && !currentIds.has(p.id)) assignedIds.add(p.id)
    })
    allParticipants.value = all.filter(p => !assignedIds.has(p.id) && !currentIds.has(p.id))
  } catch { /* ignore */ }
  assignDialogVisible.value = true
}

async function handleAssignSubmit() {
  if (assignSelectedIds.value.length === 0) {
    ElMessage.warning('请选择人员')
    return
  }
  try {
    for (const pid of assignSelectedIds.value) {
      await updateParticipant({ id: pid, teamId })
    }
    ElMessage.success(`成功分配 ${assignSelectedIds.value.length} 名人员`)
    assignDialogVisible.value = false
    fetchParticipants()
  } catch { ElMessage.error('分配失败') }
}

function goBack() {
  router.push(`/meeting/${meetingId}`)
}

onMounted(() => {
  fetchMeeting()
  fetchTeam()
  fetchParticipants()
})
</script>

<template>
  <div class="team-detail-page" v-if="team">
    <!-- 顶部信息栏 -->
    <div class="info-bar">
      <div class="info-bar-top">
        <div class="info-bar-left">
          <el-button link @click="goBack" style="margin-right:12px;font-size:14px">← 返回</el-button>
          <h2 class="info-title">{{ team.name }}</h2>
          <el-tag v-if="groupTypeName" size="small">{{ groupTypeName }}</el-tag>
        </div>
        <div style="display:flex;gap:8px">
          <el-button size="small" @click="openEditDialog">编辑</el-button>
          <el-button type="danger" size="small" @click="handleTeamDelete">删除</el-button>
        </div>
      </div>
      <div class="info-meta">
        <span v-if="team.leader">领队：{{ team.leader }}</span>
        <span v-if="team.coach">教练：{{ team.coach }}</span>
        <span>总分：{{ team.totalScore ?? 0 }}</span>
        <span>{{ participants.length }} 名队员</span>
      </div>
    </div>

    <!-- 队员列表 -->
    <div class="content-card">
      <div class="tab-toolbar">
        <span class="toolbar-hint">管理该代表队下的参赛人员</span>
        <el-button type="primary" size="small" @click="openAssignDialog">+ 分配人员</el-button>
      </div>
      <el-table v-if="participants.length > 0" :data="participants" stripe border size="small">
        <el-table-column prop="userCode" label="学号/工号" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="college" label="学院" />
        <el-table-column prop="major" label="专业" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" size="small" @click="handleRemoveFromTeam(row.id)">移出</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无队员，点击上方按钮分配人员" />
    </div>

    <!-- 弹窗：编辑代表队 -->
    <el-dialog v-model="teamEditVisible" title="编辑代表队" width="480px" destroy-on-close>
      <el-form :model="teamEditForm" label-width="80px">
        <el-form-item label="队名" required>
          <el-input v-model="teamEditForm.name" />
        </el-form-item>
        <el-form-item label="领队人">
          <el-input v-model="teamEditForm.leader" />
        </el-form-item>
        <el-form-item label="教练员">
          <el-input v-model="teamEditForm.coach" />
        </el-form-item>
        <el-form-item label="总分">
          <el-input-number v-model="teamEditForm.totalScore" :precision="2" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="teamEditVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗：分配人员 -->
    <el-dialog v-model="assignDialogVisible" title="分配人员" width="600px" destroy-on-close>
      <p style="color:#999;font-size:13px;margin-bottom:12px">仅显示该运动会下未分配代表队的人员</p>
      <el-table :data="allParticipants" stripe border size="small" max-height="400"
        @selection-change="(rows: any[]) => assignSelectedIds = rows.map(r => r.id)">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="userCode" label="学号/工号" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column prop="college" label="学院" />
      </el-table>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="assignSelectedIds.length === 0" @click="handleAssignSubmit">
          确认分配 ({{ assignSelectedIds.length }} 人)
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.team-detail-page { padding: 20px; }
.info-bar {
  background: #fff; border-radius: 8px; padding: 20px;
  margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.info-bar-top {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px;
}
.info-bar-left { display: flex; align-items: center; }
.info-title { font-size: 18px; margin: 0; }
.info-meta { display: flex; gap: 24px; font-size: 13px; color: #888; flex-wrap: wrap; }
.content-card {
  background: #fff; border-radius: 8px; padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.tab-toolbar {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;
}
.toolbar-hint { color: #999; font-size: 13px; line-height: 32px; }
</style>
