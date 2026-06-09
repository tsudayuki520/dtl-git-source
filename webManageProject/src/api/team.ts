import request from '@/utils/request'

export interface Team {
  id: number
  sportsMeetingId: number
  groupTypeId: number | null
  name: string
  leader: string
  coach: string
  totalScore: number
}

export function getTeamList(params: { sportsMeetingId?: number; groupTypeId?: number }) {
  return request.get('/admin/team/list', { params })
}

export function addTeam(data: Partial<Team>) {
  return request.post('/admin/team/add', data)
}

export function updateTeam(data: Partial<Team>) {
  return request.put('/admin/team/update', data)
}

export function deleteTeam(id: number) {
  return request.delete(`/admin/team/${id}`)
}
