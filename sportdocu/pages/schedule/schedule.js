// schedule.js - 赛程轮次列表页
const auth = require('../../utils/auth')

Page({
  data: {
    schedules: [],
    statusMap: { 0: '进行中', 1: '已结束' },
    statusColor: { 0: '#2196F3', 1: '#999999' },
  },

  onLoad(options) {
    if (options.sportsMeetingId) {
      this.setData({ sportsMeetingId: options.sportsMeetingId })
      this.loadSchedules()
    }
  },

  onShow() {
    if (this.data.sportsMeetingId) {
      this.loadSchedules()
    }
  },

  loadSchedules() {
    wx.request({
      url: auth.BASE_URL + '/api/schedule/list?sportsMeetingId=' + this.data.sportsMeetingId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          this.setData({ schedules: res.data.data || [] })
        }
      },
      fail: (err) => {
        console.error('获取赛程列表失败', err)
      }
    })
  },

  goToEvents(e) {
    const scheduleId = e.currentTarget.dataset.id
    const scheduleName = e.currentTarget.dataset.name
    wx.navigateTo({
      url: '/pages/event/event?scheduleId=' + scheduleId + '&scheduleName=' + encodeURIComponent(scheduleName)
    })
  },
})
