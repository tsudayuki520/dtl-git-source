// my-registrations.js
const auth = require('../../utils/auth')

Page({
  data: {
    groups: [],
    totalCount: 0,
    loading: true,
  },

  onLoad() {
    this.loadRegistrations()
  },

  async loadRegistrations() {
    this.setData({ loading: true })
    try {
      const data = await auth.request({ url: '/api/my/registrations', method: 'GET' })
      const groups = data || []
      const totalCount = groups.reduce((sum, g) => sum + (g.items ? g.items.length : 0), 0)
      this.setData({ groups, totalCount })
    } catch (e) {
      wx.showToast({ title: e?.message || '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },
})
