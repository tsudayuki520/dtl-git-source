import request from '@/utils/request'

export interface Participant {
  id: number
  sportsMeetingId: number
  teamId: number | null
  teamName: string | null
  userCode: string
  name: string
  phone: string
  gender: string
  college: string
  major: string
  perPersonLimit?: number
}

export function getParticipantList(sportsMeetingId: number) {
  return request.get('/admin/participant/list', { params: { sportsMeetingId } })
}

export function getParticipantListByTeam(teamId: number) {
  return request.get('/admin/participant/listByTeam', { params: { teamId } })
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

export function removeFromTeam(participantId: number) {
  return request.put('/admin/participant/clearTeam', null, { params: { participantId } })
}
