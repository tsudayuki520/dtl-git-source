<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBannerList, uploadBanner, updateBanner, deleteBanner } from '@/api/banner'
import type { Banner } from '@/api/banner'

const banners = ref<Banner[]>([])
const selectedIds = ref<number[]>([])
const loading = ref(false)
const editDialogVisible = ref(false)
const addDialogVisible = ref(false)
const uploading = ref(false)

const editForm = ref<Partial<Banner>>({})
const addForm = ref({ title: '', sortOrder: 0 })
const addFile = ref<File | null>(null)

async function fetchBanners() {
  loading.value = true
  try {
    const res: any = await getBannerList()
    banners.value = res.data || res || []
  } catch {
    ElMessage.error('获取轮播图列表失败')
  } finally {
    loading.value = false
  }
}

function handleFileChange(file: File) {
  addFile.value = file
}

async function handleAdd() {
  if (!addFile.value) {
    ElMessage.warning('请选择图片')
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', addFile.value)
    formData.append('title', addForm.value.title)
    formData.append('sortOrder', String(addForm.value.sortOrder))
    await uploadBanner(formData)
    ElMessage.success('上传成功')
    addDialogVisible.value = false
    addForm.value = { title: '', sortOrder: 0 }
    addFile.value = null
    fetchBanners()
  } catch {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

function openEdit(row: Banner) {
  editForm.value = { ...row }
  editDialogVisible.value = true
}

async function handleEditSubmit() {
  try {
    await updateBanner(editForm.value)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    fetchBanners()
  } catch { ElMessage.error('操作失败') }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('此删除操作不可逆，是否继续？', '提示', { type: 'warning' })
    await deleteBanner(id)
    ElMessage.success('删除成功')
    fetchBanners()
  } catch { /* cancel */ }
}

function onSelectionChange(rows: Banner[]) {
  selectedIds.value = rows.map(r => r.id)
}

async function handleBatchDelete() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 张轮播图？此操作不可逆。`, '批量删除', { type: 'warning' })
    const results = await Promise.allSettled(selectedIds.value.map(id => deleteBanner(id)))
    const ok = results.filter(r => r.status === 'fulfilled').length
    const fail = results.length - ok
    if (fail === 0) ElMessage.success(`成功删除 ${ok} 张轮播图`)
    else ElMessage.warning(`删除完成：成功 ${ok} 张，失败 ${fail} 张`)
    selectedIds.value = []
    fetchBanners()
  } catch { /* cancel */ }
}

async function toggleStatus(row: Banner) {
  try {
    await updateBanner({ id: row.id, status: row.status === 1 ? 0 : 1 })
    ElMessage.success('状态更新成功')
    fetchBanners()
  } catch { ElMessage.error('操作失败') }
}

onMounted(fetchBanners)
</script>

<template>
  <div class="banner-page">
    <div class="toolbar">
      <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
        批量删除<span v-if="selectedIds.length > 0">（{{ selectedIds.length }}）</span>
      </el-button>
      <el-button type="primary" @click="addDialogVisible = true">+ 上传轮播图</el-button>
    </div>

    <el-table v-loading="loading" :data="banners" stripe border size="small" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="45" />
      <el-table-column label="图片" width="140">
        <template #default="{ row }">
          <el-image v-if="row.imageUrl" :src="row.imageUrl" style="width:100px;height:56px" fit="cover" :preview-src-list="[row.imageUrl]" preview-teleported />
          <span v-else style="color:#999">无图片</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="toggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 上传弹窗 -->
    <el-dialog v-model="addDialogVisible" title="上传轮播图" width="480px" destroy-on-close>
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="图片" required>
          <input type="file" accept="image/*" @change="(e: Event) => handleFileChange((e.target as HTMLInputElement).files![0])" style="width:100%" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="addForm.title" placeholder="轮播图标题" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="addForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleAdd">上传</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑轮播图" width="480px" destroy-on-close>
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="图片预览">
          <el-image v-if="editForm.imageUrl" :src="editForm.imageUrl" style="width:200px;height:112px" fit="cover" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="editForm.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.banner-page { padding: 20px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>
