import request from '@/utils/request'

export interface ResultVO {
  id: number
  sportsMeetingId: number
  eventId: number
  participantId: number
  score: number | null
  createTime: string
  updateTime: string
  participantName: string
  eventName: string
}

export interface ResultItem {
  id?: number
  sportsMeetingId: number
  eventId: number
  participantId: number
  score: number | null
}

export function getResultList(sportsMeetingId: number) {
  return request.get('/admin/result/list', { params: { sportsMeetingId } })
}

export function getResultListByEvent(eventId: number) {
  return request.get('/admin/result/listByEvent', { params: { eventId } })
}

export function addResult(data: Partial<ResultItem>) {
  return request.post('/admin/result/add', data)
}

export function updateResult(data: Partial<ResultItem>) {
  return request.put('/admin/result/update', data)
}

export function deleteResult(id: number) {
  return request.delete(`/admin/result/${id}`)
}
