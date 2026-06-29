import request from '@/utils/request'

export interface RegistrationVO {
  id: number
  participantId: number
  eventId: number
  scheduleId: number
  status: number
  createTime: string
  participantName: string
  teamName: string | null
  eventName: string
  scheduleName: string
}

export function getRegistrationList(sportsMeetingId: number) {
  return request.get('/admin/registration/list', { params: { sportsMeetingId } })
}

export function getRegistrationListByEvent(eventId: number) {
  return request.get('/admin/registration/listByEvent', { params: { eventId } })
}

export function getRegistrationListByParticipant(participantId: number) {
  return request.get('/admin/registration/listByParticipant', { params: { participantId } })
}

export function addRegistration(data: { participantId: number; eventId: number; scheduleId?: number }) {
  return request.post('/admin/registration/add', data)
}

export function updateRegistration(id: number, status: number) {
  return request.put('/admin/registration/update', { id, status })
}

export function deleteRegistration(id: number) {
  return request.delete(`/admin/registration/${id}`)
}

export function promoteTopN(eventId: number, scheduleId: number, topN: number) {
  return request.post('/admin/registration/promoteTopN', { eventId, scheduleId, topN })
}
