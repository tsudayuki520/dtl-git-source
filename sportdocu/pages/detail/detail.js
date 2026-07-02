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
      this.loadDetail(options.id)
      this.loadEvents(options.id)
      this.loadNotices(options.id)
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

  loadDetail(id) {
    wx.request({
      url: auth.BASE_URL + '/api/sports-meeting/' + id,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const meeting = res.data.data
          const regOpen = this.isRegistrationOpen(meeting)
          this.setData({ meeting, regOpen })
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
          const categoryOrder = ['田赛', '径赛', '团队赛']
          const map = {}
          events.forEach(e => {
            const cat = e.category || '径赛'
            if (!map[cat]) map[cat] = []
            map[cat].push({ id: e.id, name: e.name, gender: e.gender, groupTypeName: e.groupTypeName, allowRegister: e.allowRegister, registerLimit: e.registerLimit })
          })
          const eventCategories = categoryOrder
            .filter(cat => map[cat])
            .map(cat => ({ category: cat, events: map[cat] }))
          this.setData({ eventCategories }, () => {
            this.loadRegisterCount(id)
          })
        }
      },
      fail: (err) => {
        console.error('获取项目列表失败', err)
      }
    })
  },

  /** 拉取各项目已报名人数，回填到 eventCategories */
  loadRegisterCount(sportsMeetingId) {
    wx.request({
      url: auth.BASE_URL + '/api/register/count?sportsMeetingId=' + sportsMeetingId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const countMap = res.data.data || {}
          const eventCategories = this.data.eventCategories.map(group => ({
            category: group.category,
            events: group.events.map(evt => ({
              ...evt,
              registeredCount: countMap[evt.id] || 0
            }))
          }))
          this.setData({ eventCategories })
        }
      },
      fail: (err) => {
        console.error('获取报名人数失败', err)
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

  goToPlayerList() {
    wx.showToast({ title: '选手名单功能开发中', icon: 'none' })
  },

  /** 点击项目卡片：查看该项目的报名人员（只读） */
  goToEventRegistrations(e) {
    const { id, name } = e.currentTarget.dataset
    wx.navigateTo({
      url: '/pages/event-registrations/event-registrations?eventId=' + id + '&eventName=' + encodeURIComponent(name)
    })
  },

  goToResult() {
    wx.showToast({ title: '成绩公告功能开发中', icon: 'none' })
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
