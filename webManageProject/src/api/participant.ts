import request from '@/utils/request'

export interface Participant {
  id: number
  sportsMeetingId: number
  userCode: string
  name: string
  phone: string
  gender: string
  college: string
  major: string
}

export function getParticipantList(sportsMeetingId: number) {
  return request.get('/admin/participant/list', { params: { sportsMeetingId } })
}

export function addParticipant(data: Partial<Participant>) {
  return request.post('/admin/participant/add', data)
}

export function updateParticipant(data: Partial<Participant>) {
  return request.put('/admin/participant/update', data)
}

export function deleteParticipant(id: number) {
  return request.delete(`/admin/participant/${id}`)
}
