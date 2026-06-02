// detail.js
const auth = require('../../utils/auth')

Page({
  data: {
    meeting: {},
    eventCategories: [],  // [{category: '田赛', events: ['跳远','铅球']}, ...]
    notices: [],          // 赛事通知
    statusMap: { 0: '报名中', 1: '进行中', 2: '已结束' },
    statusColor: { 0: '#4CAF50', 1: '#2196F3', 2: '#999999' },
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ meetingId: options.id })
      this.loadDetail(options.id)
      this.loadEvents(options.id)
      this.loadNotices(options.id)
    }
  },

  loadDetail(id) {
    wx.request({
      url: auth.BASE_URL + '/api/sports-meeting/' + id,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          this.setData({ meeting: res.data.data })
        }
      },
      fail: (err) => {
        console.error('获取运动会详情失败', err)
      }
    })
  },

  loadEvents(id) {
    wx.request({
      url: auth.BASE_URL + '/api/event/list?sportsMeetingId=' + id,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const events = res.data.data || []
          // 按分类分组，不去重，同项目不同组别都展示
          const categoryOrder = ['田赛', '径赛', '趣味赛']
          const map = {}
          events.forEach(e => {
            const cat = e.category || '径赛'
            if (!map[cat]) map[cat] = []
            map[cat].push({ id: e.id, name: e.name, gender: e.gender, groupType: e.groupType, allowRegister: e.allowRegister })
          })
          const eventCategories = categoryOrder
            .filter(cat => map[cat])
            .map(cat => ({ category: cat, events: map[cat] }))
          this.setData({ eventCategories })
        }
      },
      fail: (err) => {
        console.error('获取项目列表失败', err)
      }
    })
  },

  loadNotices(id) {
    wx.request({
      url: auth.BASE_URL + '/api/notice/sports-meeting?sportsMeetingId=' + id,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          this.setData({ notices: res.data.data })
        }
      },
      fail: (err) => {
        console.error('获取赛事通知失败', err)
      }
    })
  },

  goToSchedule() {
    wx.navigateTo({
      url: '/pages/schedule/schedule?sportsMeetingId=' + this.data.meetingId
    })
  },

  goToSignSheet() {
    wx.showToast({ title: '签名表功能开发中', icon: 'none' })
  },

  goToPlayerList() {
    wx.showToast({ title: '选手名单功能开发中', icon: 'none' })
  },

  goToResult() {
    wx.showToast({ title: '成绩公告功能开发中', icon: 'none' })
  },

  goToAppeal() {
    wx.showToast({ title: '申述功能开发中', icon: 'none' })
  },

  goToNoticeDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/notice-detail/notice-detail?id=' + id
    })
  },

  goToRegister() {
    wx.navigateTo({
      url: '/pages/register/register?sportsMeetingId=' + this.data.meetingId
    })
  },
})
