import request from '@/utils/request'

export interface GroupType {
  id: number
  sportsMeetingId: number
  name: string
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
