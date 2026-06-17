// event.js - 比赛项目列表页（带组别筛选）
const auth = require('../../utils/auth')

Page({
  data: {
    events: [],
    groupTypes: [{ id: 0, name: '全部' }],
    currentGroup: 0,
    scheduleName: '',
    statusMap: { 0: '进行中', 1: '已结束' },
    statusColor: { 0: '#2196F3', 1: '#999999' },
  },

  onLoad(options) {
    if (options.scheduleId) {
      const scheduleName = decodeURIComponent(options.scheduleName || '')
      this.setData({ scheduleId: options.scheduleId, scheduleName, sportsMeetingId: options.sportsMeetingId })
      this.loadGroupTypes()
      this.loadEvents()
    }
  },

  loadGroupTypes() {
    const sportsMeetingId = this.data.sportsMeetingId
    if (!sportsMeetingId) return
    wx.request({
      url: auth.BASE_URL + '/api/group-type/list?sportsMeetingId=' + sportsMeetingId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const list = res.data.data || []
          const groupTypes = [{ id: 0, name: '全部' }].concat(list.map(g => ({ id: g.id, name: g.name })))
          this.setData({ groupTypes })
        }
      },
      fail: (err) => {
        console.error('获取组别列表失败', err)
      }
    })
  },

  loadEvents() {
    const id = this.data.scheduleId
    const groupTypeId = this.data.currentGroup
    let url = auth.BASE_URL + '/api/event/list?scheduleId=' + id
    if (groupTypeId !== 0) {
      url += '&groupTypeId=' + groupTypeId
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
    const groupTypeId = e.currentTarget.dataset.groupTypeId
    this.setData({ currentGroup: groupTypeId })
    this.loadEvents()
  },
})
