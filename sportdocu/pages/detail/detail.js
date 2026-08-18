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
    Promise.all([
      auth.request({ url: '/api/event/list?sportsMeetingId=' + id }),
      auth.request({ url: '/api/schedule/list?sportsMeetingId=' + id }).catch(() => []),
      auth.request({ url: '/api/admin/event-schedule/listBySportsMeeting?sportsMeetingId=' + id }).catch(() => []),
    ]).then(([events, schedules, esList]) => {
      // 轮次 id -> 名称（预选赛/复赛/决赛）
      const scheduleNameById = {}
      ;(schedules || []).forEach(s => { scheduleNameById[s.id] = s.name })
      // event 行 -> 轮次名称：同项目不同赛次是多行 event，借此区分预/复/决
      const eventRoundByName = {}
      ;(esList || []).forEach(es => {
        if (es.isDeleted === 0 && scheduleNameById[es.scheduleId]) {
          eventRoundByName[es.eventId] = scheduleNameById[es.scheduleId]
        }
      })
      // 按分类分组，不去重，同项目不同组别都展示
      const categoryOrder = ['田赛', '径赛', '团队赛']
      const map = {}
      ;(events || []).forEach(e => {
        const cat = e.category || '径赛'
        if (!map[cat]) map[cat] = []
        map[cat].push({
          id: e.id,
          name: e.name,
          gender: e.gender,
          groupTypeName: e.groupTypeName,
          allowRegister: e.allowRegister,
          registerLimit: e.registerLimit,
          roundName: eventRoundByName[e.id] || '',
          roundTag: this.roundTag(eventRoundByName[e.id]),
        })
      })
      const eventCategories = categoryOrder
        .filter(cat => map[cat])
        .map(cat => ({ category: cat, events: map[cat] }))
      this.setData({ eventCategories }, () => {
        this.loadRegisterCount(id)
      })
    }).catch((err) => {
      console.error('获取项目列表失败', err)
    })
  },

  roundTag(name) {
    if (!name) return 'other'
    if (name.indexOf('预') !== -1) return 'pre'
    if (name.indexOf('复') !== -1 || name.indexOf('半') !== -1) return 'semi'
    if (name.indexOf('决') !== -1) return 'final'
    return 'other'
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
