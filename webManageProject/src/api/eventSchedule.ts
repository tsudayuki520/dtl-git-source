import request from '@/utils/request'

export interface EventSchedule {
  id: number
  eventId: number
  scheduleId: number
}

export function getEventSchedules(eventId: number) {
  return request.get('/admin/event-schedule/list', { params: { eventId } })
}

export function getEventSchedulesBySchedule(scheduleId: number) {
  return request.get('/admin/event-schedule/listBySchedule', { params: { scheduleId } })
}

export function getEventSchedulesBySportsMeeting(sportsMeetingId: number) {
  return request.get('/admin/event-schedule/listBySportsMeeting', { params: { sportsMeetingId } })
}

export function saveEventSchedules(eventId: number, scheduleIds: number[]) {
  return request.post('/admin/event-schedule/save', { eventId, scheduleIds })
}

export function deleteEventSchedule(id: number) {
  return request.delete(`/admin/event-schedule/${id}`)
}
