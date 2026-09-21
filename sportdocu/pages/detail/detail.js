// detail.js
const auth = require('../../utils/auth')

Page({
  data: {
    meeting: {},
    eventCategories: [],  // [{category: '田赛', events: ['跳远','铅球']}, ...]
    notices: [],          // 赛事通知
    statusMap: { 0: '筹备中', 1: '报名中', 2: '进行中', 3: '已结束' },
    statusColor: { 0: '#909399', 1: '#4CAF50', 2: '#2196F3', 3: '#999999' },
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ meetingId: options.id })
    }
  },

  // 每次进入/返回本页都重新拉取，避免停留在旧快照（后台新增项目后小程序看不到）
  onShow() {
    const id = this.data.meetingId
    if (id) {
      this.loadDetail(id)
    }
  },

  /** 判断当前是否在报名时间内 */
  isRegistrationOpen(meeting) {
    if (!meeting) return false
    if (meeting.status !== 1) return false
    const now = new Date().getTime()
    const start = meeting.registrationStart ? new Date(meeting.registrationStart.replace('T', ' ')).getTime() : 0
    const end = meeting.registrationEnd ? new Date(meeting.registrationEnd.replace('T', ' ')).getTime() : Infinity
    return now >= start && now <= end
  },

  /** 聚合接口：meeting + 项目(含轮次名/已报名人数) + 赛事通知，一次拿全 */
  loadDetail(id) {
    auth.request({ url: '/api/sports-meeting/' + id + '/detail' }).then((data) => {
      const meeting = data.meeting || {}
      // 按分类分组，不去重，同项目不同组别都展示
      const categoryOrder = ['田赛', '径赛', '团队赛']
      const map = {}
      ;(data.events || []).forEach(e => {
        const cat = e.category || '径赛'
        if (!map[cat]) map[cat] = []
        map[cat].push({
          id: e.id,
          name: e.name,
          gender: e.gender,
          groupTypeName: e.groupTypeName,
          allowRegister: e.allowRegister,
          registerLimit: e.registerLimit,
          roundName: e.roundName || '',
          roundTag: this.roundTag(e.roundName),
          registeredCount: e.registeredCount || 0,
        })
      })
      const eventCategories = categoryOrder
        .filter(cat => map[cat])
        .map(cat => ({ category: cat, events: map[cat] }))
      this.setData({
        meeting,
        eventCategories,
        notices: data.notices || [],
        regOpen: this.isRegistrationOpen(meeting),
      })
    }).catch((err) => {
      console.error('获取运动会详情失败', err)
    })
  },

  roundTag(name) {
    if (!name) return 'other'
    if (name.indexOf('预') !== -1) return 'pre'
    if (name.indexOf('复') !== -1 || name.indexOf('半') !== -1) return 'semi'
    if (name.indexOf('决') !== -1) return 'final'
    return 'other'
  },

  goToSchedule() {
    wx.navigateTo({
      url: '/pages/schedule/schedule?sportsMeetingId=' + this.data.meetingId
    })
  },

  /** 点击项目卡片：查看该项目的报名人员（只读） */
  goToEventRegistrations(e) {
    const { id, name } = e.currentTarget.dataset
    wx.navigateTo({
      url: '/pages/event-registrations/event-registrations?eventId=' + id + '&eventName=' + encodeURIComponent(name)
    })
  },

  goToResult() {
    wx.navigateTo({
      url: '/pages/result/result?sportsMeetingId=' + this.data.meetingId + '&name=' + encodeURIComponent(this.data.meeting.name || '')
    })
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
