<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMeetingList, addMeeting, updateMeeting, deleteMeeting } from '@/api/meeting'
import type { SportsMeeting } from '@/api/meeting'

const router = useRouter()
const meetings = ref<SportsMeeting[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增运动会')
const statusFilter = ref<number | undefined>(undefined)

const statusMap: Record<number, { label: string; color: string; borderColor: string }> = {
  0: { label: '报名中', color: '#e6f7ff', borderColor: '#1890ff' },
  1: { label: '进行中', color: '#f6ffed', borderColor: '#52c41a' },
  2: { label: '已结束', color: '#fff0f0', borderColor: '#ff4d4f' },
}

const form = ref<Partial<SportsMeeting>>({
  name: '',
  status: 0,
  organizer: '',
  contactPhone: '',
  venue: '',
  registrationStart: '',
  registrationEnd: '',
  competitionDate: '',
})

async function fetchMeetings() {
  loading.value = true
  try {
    const res: any = await getMeetingList({ status: statusFilter.value })
    meetings.value = res.data || res || []
  } catch {
    ElMessage.error('获取运动会列表失败')
  } finally {
    loading.value = false
  }
}

function openAdd() {
  dialogTitle.value = '新增运动会'
  form.value = {
    name: '',
    status: 0,
    organizer: '',
    contactPhone: '',
    venue: '',
    registrationStart: '',
    registrationEnd: '',
    competitionDate: '',
  }
  dialogVisible.value = true
}

function openEdit(row: SportsMeeting) {
  dialogTitle.value = '编辑运动会'
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    if (form.value.id) {
      await updateMeeting(form.value)
      ElMessage.success('更新成功')
    } else {
      await addMeeting(form.value)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchMeetings()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该运动会吗？删除后不可恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteMeeting(id)
    ElMessage.success('删除成功')
    fetchMeetings()
  } catch {
    // 用户取消
  }
}

function goDetail(id: number) {
  router.push(`/meeting/${id}`)
}

function formatDate(d: string) {
  if (!d) return ''
  return d.substring(0, 10)
}

onMounted(fetchMeetings)
</script>

<template>
  <div class="home-page">
    <!-- 操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 130px" @change="fetchMeetings">
          <el-option label="报名中" :value="0" />
          <el-option label="进行中" :value="1" />
          <el-option label="已结束" :value="2" />
        </el-select>
      </div>
      <el-button type="primary" @click="openAdd">+ 新增运动会</el-button>
    </div>

    <!-- 卡片网格 -->
    <div v-loading="loading" class="card-grid">
      <div
        v-for="item in meetings"
        :key="item.id"
        class="meeting-card"
        :style="{ borderLeftColor: statusMap[item.status]?.borderColor }"
      >
        <div class="card-header">
          <span class="card-title">{{ item.name }}</span>
          <el-tag
            :color="statusMap[item.status]?.color"
            :style="{ color: statusMap[item.status]?.borderColor, border: 'none' }"
            size="small"
          >
            {{ statusMap[item.status]?.label }}
          </el-tag>
        </div>
        <div class="card-info">
          <div>地点：{{ item.venue }}</div>
          <div>赛事日期：{{ formatDate(item.competitionDate) }}</div>
          <div>主办方：{{ item.organizer }}</div>
          <div>联系电话：{{ item.contactPhone }}</div>
        </div>
        <div class="card-actions">
          <el-button type="primary" link @click="goDetail(item.id)">进入管理 →</el-button>
          <el-button link @click="openEdit(item)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(item.id)">删除</el-button>
        </div>
      </div>

      <el-empty v-if="!loading && meetings.length === 0" description="暂无运动会数据" />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="运动会名称" required>
          <el-input v-model="form.name" placeholder="请输入运动会名称" />
        </el-form-item>
        <el-form-item label="主办方" required>
          <el-input v-model="form.organizer" placeholder="请输入主办方" />
        </el-form-item>
        <el-form-item label="联系电话" required>
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="比赛地点" required>
          <el-input v-model="form.venue" placeholder="请输入比赛地点" />
        </el-form-item>
        <el-form-item label="赛事日期" required>
          <el-input v-model="form.competitionDate" type="date" />
        </el-form-item>
        <el-form-item label="报名开始" required>
          <el-input v-model="form.registrationStart" type="datetime-local" />
        </el-form-item>
        <el-form-item label="报名截止" required>
          <el-input v-model="form.registrationEnd" type="datetime-local" />
        </el-form-item>
        <el-form-item label="状态" v-if="form.id">
          <el-select v-model="form.status">
            <el-option label="报名中" :value="0" />
            <el-option label="进行中" :value="1" />
            <el-option label="已结束" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.home-page {
  padding: 20px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}
.meeting-card {
  background: #fff;
  border-radius: 8px;
  padding: 18px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border-left: 4px solid #409eff;
  transition: box-shadow 0.2s;
}
.meeting-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.card-title {
  font-size: 16px;
  font-weight: bold;
}
.card-info {
  font-size: 13px;
  color: #888;
  line-height: 1.8;
}
.card-actions {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  gap: 8px;
}
</style>
