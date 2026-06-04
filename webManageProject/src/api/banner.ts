import request from '@/utils/request'

export interface Banner {
  id: number
  imageUrl: string
  title: string
  sortOrder: number
  status: number
  createTime: string
  updateTime: string
}

export function getBannerList() {
  return request.get('/admin/banner/list')
}

export function uploadBanner(formData: FormData) {
  return request.post('/admin/banner/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function updateBanner(data: Partial<Banner>) {
  return request.put('/admin/banner/update', data)
}

export function deleteBanner(id: number) {
  return request.delete(`/admin/banner/${id}`)
}
