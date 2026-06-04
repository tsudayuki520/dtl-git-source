import request from '@/utils/request'

export interface RegistrationVO {
  id: number
  participantId: number
  eventId: number
  status: number
  createTime: string
  participantName: string
  eventName: string
}

export function getRegistrationList(sportsMeetingId: number) {
  return request.get('/admin/registration/list', { params: { sportsMeetingId } })
}

export function updateRegistration(id: number, status: number) {
  return request.put('/admin/registration/update', { id, status })
}

export function deleteRegistration(id: number) {
  return request.delete(`/admin/registration/${id}`)
}
