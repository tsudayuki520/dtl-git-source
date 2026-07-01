<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRecordList, addRecord, updateRecord, deleteRecord } from '@/api/record'
import type { Record } from '@/api/record'

const records = ref<Record[]>([])
const dialogVisible = ref(false)
const form = ref<Partial<Record>>({})

async function fetchRecords() {
  try {
    const res: any = await getRecordList()
    records.value = res.data || res || []
  } catch { ElMessage.error('获取记录失败') }
}

function formatDate(d: string) {
  if (!d) return ''
  return d.substring(0, 16).replace('T', ' ')
}

function formatScore(score: number | null | undefined, category: string | null | undefined): string {
  if (score == null) return '-'
  if (category === '田赛') return `${score}米`
  if (category === '径赛' || category === '团队赛') return `${score}秒`
  return `${score}`
}

function scoreUnit(category: string | null | undefined): string {
  if (category === '田赛') return '米'
  if (category === '径赛' || category === '团队赛') return '秒'
  return ''
}

function openAdd() {
  form.value = { groupType: '', eventName: '', category: '', unit: '', name: '', score: null, scoreValue: null, recordTime: '' }
  dialogVisible.value = true
}
function openEdit(row: Record) {
  form.value = { ...row }
  dialogVisible.value = true
}
async function handleSubmit() {
  try {
    if (form.value.id) {
      await updateRecord(form.value)
    } else {
      await addRecord(form.value)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    fetchRecords()
  } catch { ElMessage.error('操作失败') }
}
async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该记录？', '提示', { type: 'warning' })
    await deleteRecord(id)
    ElMessage.success('删除成功')
    fetchRecords()
  } catch { /* cancel */ }
}

onMounted(fetchRecords)
</script>

<template>
  <div class="record-page">
    <div class="toolbar">
      <div></div>
      <el-button type="primary" @click="openAdd">+ 新增记录</el-button>
    </div>

    <el-table :data="records" stripe border size="small">
      <el-table-column prop="groupType" label="组别" width="100" />
      <el-table-column prop="eventName" label="项目" width="120" />
      <el-table-column prop="category" label="类别" width="90" />
      <el-table-column prop="unit" label="单位" width="140" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column label="成绩" width="110">
        <template #default="{ row }">{{ formatScore(row.score, row.category) }}</template>
      </el-table-column>
      <el-table-column label="时间" width="160">
        <template #default="{ row }">{{ formatDate(row.recordTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑记录' : '新增记录'" width="520px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="组别">
          <el-input v-model="form.groupType" placeholder="如：学生组、教工组" />
        </el-form-item>
        <el-form-item label="项目">
          <el-input v-model="form.eventName" placeholder="如：100米、跳远" />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="form.category" placeholder="选择类别" style="width:100%">
            <el-option label="径赛" value="径赛" />
            <el-option label="田赛" value="田赛" />
            <el-option label="团队赛" value="团队赛" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="form.unit" placeholder="如：计算机学院" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="成绩">
          <el-input-number v-model="form.score" :precision="2" :min="0" style="width:100%" :placeholder="scoreUnit(form.category) ? `单位：${scoreUnit(form.category)}` : ''" />
          <span v-if="scoreUnit(form.category)" style="margin-left:8px;color:#999;font-size:12px">{{ scoreUnit(form.category) }}</span>
        </el-form-item>
        <el-form-item label="时间">
          <el-input v-model="form.recordTime" type="datetime-local" />
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
.record-page { padding: 20px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>
