// event.js - 比赛项目列表页（带组别筛选）
const auth = require('../../utils/auth')

Page({
  data: {
    events: [],
    groupTypes: ['全部', '学生组', '教工组'],
    currentGroup: 0,
    scheduleName: '',
    statusMap: { 0: '进行中', 1: '已结束' },
    statusColor: { 0: '#2196F3', 1: '#999999' },
  },

  onLoad(options) {
    if (options.scheduleId) {
      const scheduleName = decodeURIComponent(options.scheduleName || '')
      this.setData({ scheduleId: options.scheduleId, scheduleName })
      this.loadEvents()
    }
  },

  loadEvents() {
    const id = this.data.scheduleId
    const idx = this.data.currentGroup
    let url = auth.BASE_URL + '/api/event/list?scheduleId=' + id
    if (idx !== 0) {
      url += '&groupType=' + encodeURIComponent(this.data.groupTypes[idx])
    }

    wx.request({
      url: url,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          this.setData({ events: res.data.data || [] })
        }
      },
      fail: (err) => {
        console.error('获取项目列表失败', err)
      }
    })
  },

  switchGroup(e) {
    const idx = e.currentTarget.dataset.index
    this.setData({ currentGroup: idx })
    this.loadEvents()
  },
})
