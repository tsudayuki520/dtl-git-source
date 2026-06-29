import request from '@/utils/request'

export interface TeamScoreAdjustment {
  id: number
  teamId: number
  deltaAmount: number
  note: string
  createTime: string
}

export function getAdjustmentList(teamId: number) {
  return request.get('/admin/teamScoreAdjustment/list', { params: { teamId } })
}

export function addAdjustment(data: { teamId: number; deltaAmount: number; note: string }) {
  return request.post('/admin/teamScoreAdjustment/add', data)
}

export function deleteAdjustment(id: number) {
  return request.delete(`/admin/teamScoreAdjustment/${id}`)
}
