<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addNotice, updateNotice, deleteNotice } from '@/api/notice'
import request from '@/utils/request'

interface Notice {
  id: number
  sportsMeetingId: number | null
  title: string
  content: string
  createTime: string
  updateTime: string
}

const notices = ref<Notice[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增全局通知')

const form = ref<Partial<Notice>>({ title: '', content: '' })

async function fetchNotices() {
  loading.value = true
  try {
    const res: any = await request.get('/notice/global')
    notices.value = res.data || res || []
  } catch {
    ElMessage.error('获取全局通知失败')
  } finally {
    loading.value = false
  }
}

function openAdd() {
  dialogTitle.value = '新增全局通知'
  form.value = { title: '', content: '' }
  dialogVisible.value = true
}

function openEdit(row: Notice) {
  dialogTitle.value = '编辑全局通知'
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    if (form.value.id) {
      await updateNotice(form.value)
    } else {
      await addNotice({ ...form.value, sportsMeetingId: null })
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    fetchNotices()
  } catch { ElMessage.error('操作失败') }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该通知？', '提示', { type: 'warning' })
    await deleteNotice(id)
    ElMessage.success('删除成功')
    fetchNotices()
  } catch { /* cancel */ }
}

function formatDate(d: string) {
  return d ? d.substring(0, 10) : ''
}

onMounted(fetchNotices)
</script>

<template>
  <div class="notice-page">
    <div class="toolbar">
      <div></div>
      <el-button type="primary" @click="openAdd">+ 新增通知</el-button>
    </div>

    <el-table v-loading="loading" :data="notices" stripe border size="small">
      <el-table-column prop="title" label="标题" />
      <el-table-column label="内容" show-overflow-tooltip>
        <template #default="{ row }">{{ row.content }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form :model="form" label-width="70px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="8" />
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
.notice-page { padding: 20px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>
