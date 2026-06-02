// match.js
const auth = require('../../utils/auth')

Page({
  data: {
    meetings: [],
    expandedId: null,
    statusMap: { 0: '报名中', 1: '进行中', 2: '已结束' },
    statusColor: { 0: '#4CAF50', 1: '#2196F3', 2: '#999999' },
  },

  onShow() {
    this.loadOngoing()
  },

  loadOngoing() {
    this.setData({ expandedId: null })
    wx.request({
      url: auth.BASE_URL + '/api/sports-meeting/list?status=1',
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          this.setData({ meetings: res.data.data })
        }
      },
      fail: (err) => {
        console.error('获取赛况失败', err)
      }
    })
  },

  toggleExpand(e) {
    const id = e.currentTarget.dataset.id
    const expandedId = this.data.expandedId === id ? null : id
    this.setData({ expandedId })

    // 展开时加载该运动会的项目
    if (expandedId !== null) {
      this.loadEvents(expandedId)
    }
  },

  loadEvents(sportsMeetingId) {
    wx.request({
      url: auth.BASE_URL + '/api/event/list?sportsMeetingId=' + sportsMeetingId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const events = (res.data.data || []).filter(e => e.status === 0)
          const meetings = this.data.meetings
          const idx = meetings.findIndex(m => m.id === sportsMeetingId)
          if (idx !== -1) {
            this.setData({ ['meetings[' + idx + '].events']: events })
          }
        }
      },
      fail: (err) => {
        console.error('获取项目失败', err)
      }
    })
  },
})
