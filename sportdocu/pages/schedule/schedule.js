// schedule.js - 赛程轮次列表页
const auth = require('../../utils/auth')
const cache = require('../../utils/cache')

const SCHEDULES_TTL = 5 * 60 * 1000
const schedulesCacheKey = id => 'schedules:' + id


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
    const cacheKey = schedulesCacheKey(this.data.sportsMeetingId)
    // 先用缓存渲染，随后仍然请求网络并覆盖（保证后台新增赛次后能立刻看到）
    const cached = cache.get(cacheKey)
    if (cached) this.setData({ schedules: cached })
    wx.request({
      url: auth.BASE_URL + '/api/schedule/list?sportsMeetingId=' + this.data.sportsMeetingId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const schedules = res.data.data || []
          cache.set(cacheKey, schedules, SCHEDULES_TTL)
          this.setData({ schedules })
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
      url: '/pages/event/event?scheduleId=' + scheduleId + '&scheduleName=' + encodeURIComponent(scheduleName) + '&sportsMeetingId=' + this.data.sportsMeetingId
    })
  },
})
