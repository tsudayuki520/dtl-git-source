import request from '@/utils/request'

export interface Notice {
  id: number
  sportsMeetingId: number | null
  title: string
  content: string
  fileUrl: string | null
  fileName: string | null
  createTime: string
  updateTime: string
}

export function getNoticeList(sportsMeetingId: number) {
  return request.get('/notice/sports-meeting', { params: { sportsMeetingId } })
}

export function addNotice(data: Partial<Notice>) {
  return request.post('/admin/notice/add', data)
}

export function uploadNoticeFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/notice/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function updateNotice(data: Partial<Notice>) {
  return request.put('/admin/notice/update', data)
}

export function deleteNotice(id: number) {
  return request.delete(`/admin/notice/${id}`)
}
