import request from '@/utils/request'

export interface SportsMeeting {
  id: number
  name: string
  status: number
  organizer: string
  contactPhone: string
  venue: string
  registrationStart: string
  registrationEnd: string
  competitionDate: string
  createTime: string
  updateTime: string
}

export function getMeetingList(params?: { status?: number }) {
  return request.get('/admin/sports-meeting/list', { params })
}

export function getMeetingDetail(id: number) {
  return request.get(`/sports-meeting/${id}`)
}

export function addMeeting(data: Partial<SportsMeeting>) {
  return request.post('/admin/sports-meeting/add', data)
}

export function updateMeeting(data: Partial<SportsMeeting>) {
  return request.put('/admin/sports-meeting/update', data)
}

export function deleteMeeting(id: number) {
  return request.delete(`/admin/sports-meeting/${id}`)
}
