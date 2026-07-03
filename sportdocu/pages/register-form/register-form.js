const auth = require('../../utils/auth')

Page({
  data: {
    eventId: '',
    eventName: '',
    eventGender: '',
    eventGroupType: '',
    sportsMeetingId: '',
    submitting: false,
  },

  onLoad(options) {
    // 触发登录：未登录跳登录页，登录后回跳本页继续报名
    const redirect = '/pages/register-form/register-form?' + Object.entries(options).map(([k,v]) => `${k}=${v}`).join('&')
    if (!auth.requireLogin(redirect)) return

    this.setData({
      eventId: options.eventId,
      eventName: decodeURIComponent(options.eventName || ''),
      eventGender: decodeURIComponent(options.eventGender || ''),
      eventGroupType: decodeURIComponent(options.eventGroupType || ''),
      sportsMeetingId: options.sportsMeetingId,
    })
  },

  async submit() {
    if (this.data.submitting) return
    this.setData({ submitting: true })
    try {
      await auth.request({
        url: '/api/register/submit',
        method: 'POST',
        data: {
          sportsMeetingId: this.data.sportsMeetingId,
          eventId: this.data.eventId,
        },
      })
      wx.showToast({ title: '报名成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 1500)
    } catch (e) {
      wx.showToast({ title: e?.message || '报名失败', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },
})
