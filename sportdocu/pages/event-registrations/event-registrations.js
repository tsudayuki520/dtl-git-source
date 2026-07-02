// event-registrations.js
const auth = require('../../utils/auth')

Page({
  data: {
    eventId: null,
    eventName: '',
    registrations: [],
    loaded: false
  },

  onLoad(options) {
    const eventName = options.eventName ? decodeURIComponent(options.eventName) : ''
    this.setData({ eventName })
    wx.setNavigationBarTitle({ title: eventName || '报名人员' })
    if (options.eventId) {
      this.setData({ eventId: options.eventId })
      this.loadRegistrations(options.eventId)
    }
  },

  loadRegistrations(eventId) {
    wx.showLoading({ title: '加载中' })
    wx.request({
      url: auth.BASE_URL + '/api/admin/registration/listByEvent?eventId=' + eventId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          this.setData({ registrations: res.data.data || [] })
        } else {
          wx.showToast({ title: '加载失败', icon: 'none' })
        }
      },
      fail: () => {
        wx.showToast({ title: '网络错误', icon: 'none' })
      },
      complete: () => {
        this.setData({ loaded: true })
        wx.hideLoading()
      }
    })
  }
})
