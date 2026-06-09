import request from '@/utils/request'

export interface Record {
  id: number
  groupType: string
  eventName: string
  unit: string
  name: string
  score: number | null
  recordTime: string
  venue: string
  createTime: string
  updateTime: string
}

export function getRecordList() {
  return request.get('/admin/record/list')
}

export function addRecord(data: Partial<Record>) {
  return request.post('/admin/record/add', data)
}

export function updateRecord(data: Partial<Record>) {
  return request.put('/admin/record/update', data)
}

export function deleteRecord(id: number) {
  return request.delete(`/admin/record/${id}`)
}
