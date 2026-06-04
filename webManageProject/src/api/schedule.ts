import request from '@/utils/request'

export interface Schedule {
  id: number
  sportsMeetingId: number
  name: string
  status: number
}

export function getScheduleList(sportsMeetingId: number) {
  return request.get('/schedule/list', { params: { sportsMeetingId } })
}

export function addSchedule(data: Partial<Schedule>) {
  return request.post('/admin/schedule/add', data)
}

export function updateSchedule(data: Partial<Schedule>) {
  return request.put('/admin/schedule/update', data)
}

export function deleteSchedule(id: number) {
  return request.delete(`/admin/schedule/${id}`)
}
