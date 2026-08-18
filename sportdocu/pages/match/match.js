// match.js
const auth = require('../../utils/auth')

Page({
  data: {
    meetings: [],
    activeMeetingId: null,
    schedules: [],
    events: [],
    expandedEventId: null,
    loading: false,
    eventsLoading: false,
    resultsLoading: false,
  },

  onShow() {
    this.loadOngoing()
  },

  loadOngoing() {
    this.setData({ loading: true, meetings: [], schedules: [], events: [], activeMeetingId: null, expandedEventId: null })
    auth.request({
      url: '/api/sports-meeting/list?status=2',
    }).then((data) => {
      const meetings = data || []
      this.setData({ meetings, loading: false })
      if (meetings.length > 0) {
        this.selectMeeting(meetings[0].id)
      }
    }).catch((err) => {
      console.error('获取赛况失败', err)
      this.setData({ loading: false })
    })
  },

  onTabTap(e) {
    const id = Number(e.currentTarget.dataset.id)
    if (id === this.data.activeMeetingId) return
    this.selectMeeting(id)
  },

  selectMeeting(id) {
    this.setData({ activeMeetingId: id, schedules: [], events: [], expandedEventId: null })
    this.loadEvents(id)
    this.loadSchedules(id)
  },

  loadSchedules(sportsMeetingId) {
    auth.request({
      url: '/api/schedule/list?sportsMeetingId=' + sportsMeetingId,
    }).then((data) => {
      this.setData({ schedules: data || [] })
    }).catch((err) => {
      console.error('获取轮次列表失败', err)
    })
  },

  loadEvents(sportsMeetingId) {
    this.setData({ eventsLoading: true })
    auth.request({
      url: '/api/event/list?sportsMeetingId=' + sportsMeetingId,
    }).then((data) => {
      const raw = data || []
      // 同一项目（名称/性别/分类/组别相同）可能按赛次存了多行 event，归并为一个卡片
      const map = {}
      const groups = []
      raw.forEach(e => {
        const key = e.name + '|' + e.gender + '|' + e.category + '|' + e.groupTypeName
        if (!map[key]) {
          map[key] = {
            key,
            name: e.name,
            gender: e.gender,
            category: e.category,
            groupTypeName: e.groupTypeName,
            rounds: [],
          }
          groups.push(map[key])
        }
        map[key].rounds.push(e)
      })
      // 展示全部项目（不再按是否进行中筛选）
      this.setData({ events: groups, eventsLoading: false })
    }).catch((err) => {
      console.error('获取项目失败', err)
      this.setData({ eventsLoading: false })
    })
  },

  onEventTap(e) {
    const key = e.currentTarget.dataset.key
    const expandedEventId = this.data.expandedEventId === key ? null : key
    this.setData({ expandedEventId })
    if (expandedEventId !== null) {
      this.loadResults(key)
    }
  },

  loadResults(groupKey) {
    const groups = this.data.events
    const group = groups.find(g => g.key === groupKey)
    if (!group || group.results) return // 已加载过则不重复请求
    this.setData({ resultsLoading: true })
    // 该项目每个赛次是一行 event，分别拉取其成绩后合并
    const reqs = group.rounds.map(r =>
      auth.request({ url: '/api/admin/result/listByEvent?eventId=' + r.id })
        .then(d => d || [])
        .catch(() => [])
    )
    Promise.all(reqs).then((arrays) => {
      const rows = [].concat.apply([], arrays)
      // 轮次排序依据：schedule.sort 越小越靠前（预→复→决）
      const sortMap = {}
      ;(this.data.schedules || []).forEach(s => { sortMap[s.id] = s.sort == null ? 0 : s.sort })

      // 按轮次（scheduleId）分组
      const groupMap = {}
      const groupOrder = []
      rows.forEach(r => {
        const sid = r.scheduleId
        const key = sid == null ? '__none' : sid
        if (!groupMap[key]) {
          groupMap[key] = {
            scheduleId: sid,
            scheduleName: r.scheduleName || (sid == null ? '成绩' : '第' + sid + '轮'),
            rows: [],
          }
          groupOrder.push(key)
        }
        groupMap[key].rows.push(r)
      })
      groupOrder.sort((a, b) =>
        (sortMap[groupMap[a].scheduleId] ?? 999) - (sortMap[groupMap[b].scheduleId] ?? 999))

      // 每组内按返回顺序编号名次（后端已按田赛大/径赛小排序，轮内相对顺序正确）
      const results = groupOrder.map((key, gi) => {
        const g = groupMap[key]
        return {
          key: 's' + gi,
          scheduleId: g.scheduleId,
          scheduleName: g.scheduleName,
          tag: this.roundTag(g.scheduleName),
          rows: g.rows.map((r, i) => ({
            key: key + '_' + i,
            rank: r.scoreValue == null ? '' : i + 1,
            name: r.participantName || r.userCode || '未知选手',
            unit: r.college || '',
            score: this.formatScore(r.scoreValue, r.category),
            hasScore: r.scoreValue != null,
            points: r.points || 0,
          })),
        }
      })
      const current = this.data.events
      const idx = current.findIndex(g => g.key === groupKey)
      if (idx !== -1) {
        this.setData({ ['events[' + idx + '].results']: results })
      }
      this.setData({ resultsLoading: false })
    }).catch((err) => {
      console.error('获取成绩失败', err)
      this.setData({ resultsLoading: false })
    })
  },

  roundTag(name) {
    if (!name) return 'other'
    if (name.indexOf('预') !== -1) return 'pre'
    if (name.indexOf('复') !== -1 || name.indexOf('半') !== -1) return 'semi'
    if (name.indexOf('决') !== -1) return 'final'
    return 'other'
  },

  formatScore(scoreValue, category) {
    if (scoreValue == null) return '未录入'
    if (category === '田赛') return (scoreValue / 100).toFixed(2) + '米'
    const totalSeconds = Math.floor(scoreValue / 1000)
    const ms = scoreValue % 1000
    return totalSeconds + '.' + String(ms).padStart(3, '0') + '秒'
  },
})
