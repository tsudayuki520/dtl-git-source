import request from '@/utils/request'

export interface Event {
  id: number
  sportsMeetingId: number
  scheduleId: number
  name: string
  category: string
  gender: string
  groupType: string
  allowRegister: number
  registerLimit: number
  status: number
}

export function getEventList(params: { sportsMeetingId: number }) {
  return request.get('/event/list', { params })
}

export function addEvent(data: Partial<Event>) {
  return request.post('/admin/event/add', data)
}

export function updateEvent(data: Partial<Event>) {
  return request.put('/admin/event/update', data)
}

export function deleteEvent(id: number) {
  return request.delete(`/admin/event/${id}`)
}
