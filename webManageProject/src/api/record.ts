import request from '@/utils/request'

export interface Record {
  id: number
  sportsMeetingId: number | null
  groupType: string
  eventName: string
  category: string
  unit: string
  name: string
  score: number | null
  scoreValue: number | null
  resultId: number | null
  recordTime: string
  createTime: string
  updateTime: string
}

export function getRecordList(params?: { sportsMeetingId?: number; eventName?: string; category?: string }) {
  return request.get('/admin/record/list', { params })
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

export function reviewRecord(resultId: number, action: 'approve' | 'reject') {
  return request.post('/admin/record/review', { resultId, action })
}
