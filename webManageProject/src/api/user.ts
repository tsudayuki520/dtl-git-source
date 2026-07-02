import request from '@/utils/request'

export function resetPassword(userId: number) {
  return request.post('/admin/user/reset-password', { userId })
}
