import request from '@/utils/request'

export interface ResultVO {
  id: number
  sportsMeetingId: number
  eventId: number
  eventScheduleId: number | null
  scheduleId: number | null
  participantId: number
  // 成绩值：径赛=毫秒数，田赛=厘米数（按 category 区分）
  scoreValue: number | null
  // 积分（用于代表队总分计算）
  points: number
  createTime: string
  updateTime: string
  participantName: string
  eventName: string
  scheduleName: string
  // 项目分类：田赛/径赛/团队赛
  category: string
}

export interface ResultItem {
  id?: number
  sportsMeetingId: number
  eventId: number
  scheduleId?: number
  eventScheduleId?: number
  participantId: number
  scoreValue: number | null
  points?: number
}

export function getResultList(sportsMeetingId: number) {
  return request.get('/admin/result/list', { params: { sportsMeetingId } })
}

export function getResultListByEvent(eventId: number) {
  return request.get('/admin/result/listByEvent', { params: { eventId } })
}

export function getResultsByEventAndSchedule(eventId: number, scheduleId: number) {
  return request.get('/admin/result/listByEventAndSchedule', { params: { eventId, scheduleId } })
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
