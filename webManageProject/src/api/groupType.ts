import request from '@/utils/request'

export interface GroupType {
  id: number
  sportsMeetingId: number
  name: string
  perTeamLimit?: number
  limitEventIds?: string | null
  perPersonLimit?: number
  personLimitEventIds?: string | null
}

export function getGroupTypeList(sportsMeetingId: number) {
  return request.get('/admin/group-type/list', { params: { sportsMeetingId } })
}

export function addGroupType(data: Partial<GroupType>) {
  return request.post('/admin/group-type/add', data)
}

export function updateGroupType(data: Partial<GroupType>) {
  return request.put('/admin/group-type/update', data)
}

export function deleteGroupType(id: number) {
  return request.delete(`/admin/group-type/${id}`)
}

export function getLimitConfig(groupTypeId: number) {
  return request.get('/admin/group-type/limitConfig', { params: { groupTypeId } })
}

export function saveLimitConfig(data: {
  groupTypeId: number
  perTeamLimit: number
  eventIds: number[]
  perPersonLimit: number
  personEventIds: number[]
}) {
  return request.post('/admin/group-type/saveLimitConfig', data)
}
