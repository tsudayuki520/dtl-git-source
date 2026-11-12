const auth = require('../../utils/auth')
const { groupEvents, filterEvents, buildResults } = require('../../utils/match')

Page({
  data: {
    meetingStatus: 2,
    meetings: [], meetingOptions: [], activeMeetingId: null, activeMeeting: null,
    meetingIndex: 0, events: [], eventCount: 0, scheduleCount: 0,
    keyword: '', category: '', categories: ['全部', '径赛', '田赛', '团队赛'],
    groupOptions: [{ key: null, name: '全部组别' }], groupIndex: 0,
    expandedEventId: null, loading: false, eventsLoading: false,
    pageError: '', eventsError: '', scheduleError: '', updatedAt: '',
  },

  onLoad() {
    this._groups = []
    this._schedules = []
    this._meetingVersion = 0
    this._selectionVersion = 0
  },

  onShow() {
    this._disposed = false
    return this.loadMeetings()
  },

  onUnload() {
    this._disposed = true
    this._meetingVersion++
    this._selectionVersion++
  },

  onPullDownRefresh() {
    return this.loadMeetings().finally(() => wx.stopPullDownRefresh())
  },

  async loadMeetings() {
    const version = ++this._meetingVersion
    this.setData({ loading: true, pageError: '' })
    try {
      const data = await auth.request({ url: '/api/sports-meeting/list' })
      if (this._disposed || version !== this._meetingVersion) return
      const meetings = (data || []).filter(meeting => [2, 3].includes(meeting.status))
      this.setData({ meetings })
      await this.showMeetingOptions()
    } catch (error) {
      if (!this._disposed && version === this._meetingVersion) {
        this.setData({ pageError: '运动会加载失败，请检查网络后重试' })
      }
    } finally {
      if (!this._disposed && version === this._meetingVersion) this.setData({ loading: false })
    }
  },

  showMeetingOptions() {
    const meetingOptions = this.data.meetings.filter(meeting => meeting.status === this.data.meetingStatus)
    const selected = meetingOptions.find(meeting => meeting.id === this.data.activeMeetingId) || meetingOptions[0]
    this.setData({ meetingOptions })
    if (selected) return this.selectMeeting(selected.id)
    this._selectionVersion++
    this._groups = []
    this._schedules = []
    this.setData({ activeMeetingId: null, activeMeeting: null, events: [], eventCount: 0,
      scheduleCount: 0, expandedEventId: null, eventsLoading: false, eventsError: '',
      scheduleError: '', updatedAt: '' })
    return Promise.resolve()
  },

  onStatusTap(event) {
    const status = Number(event.currentTarget.dataset.status)
    if (status === this.data.meetingStatus) return
    this.setData({ meetingStatus: status })
    return this.showMeetingOptions()
  },

  onMeetingChange(event) {
    const meeting = this.data.meetingOptions[Number(event.detail.value)]
    if (meeting && meeting.id !== this.data.activeMeetingId) return this.selectMeeting(meeting.id)
  },

  async selectMeeting(id) {
    const version = ++this._selectionVersion
    const changed = id !== this.data.activeMeetingId
    const activeMeeting = this.data.meetingOptions.find(meeting => meeting.id === id)
    if (!activeMeeting) return
    if (changed) {
      this._groups = []
      this._schedules = []
      this.setData({ keyword: '', category: '', groupIndex: 0,
        groupOptions: [{ key: null, name: '全部组别' }], events: [], eventCount: 0,
        scheduleCount: 0, expandedEventId: null, updatedAt: '' })
    }
    this.setData({ activeMeetingId: id, activeMeeting,
      meetingIndex: this.data.meetingOptions.findIndex(meeting => meeting.id === id),
      eventsLoading: true, eventsError: '', scheduleError: '' })
    // 等轮次与项目均就绪再拉取成绩，避免轮次接口慢时排序错误。
    const [events, schedules] = await Promise.all([
      auth.request({ url: '/api/event/list?sportsMeetingId=' + id })
        .then(data => ({ data: data || [] })).catch(() => ({ error: true })),
      auth.request({ url: '/api/schedule/list?sportsMeetingId=' + id })
        .then(data => ({ data: data || [] })).catch(() => ({ error: true })),
    ])
    if (!this.isCurrent(version)) return
    if (!schedules.error) this._schedules = schedules.data
    this.setData({ scheduleCount: this._schedules.length,
      scheduleError: schedules.error ? '轮次信息未更新，成绩暂按已有轮次展示' : '' })
    if (events.error) {
      this._groups.forEach(group => {
        if (group.resultState === 'loading') group.resultState = 'idle'
      })
      this.publishEvents()
      this.setData({ eventsLoading: false, eventsError: '项目更新失败，请重试' })
      return
    }
    this._groups = groupEvents(events.data)
    const options = [{ key: null, name: '全部组别' }]
    this._groups.forEach(group => {
      if (!options.some(option => option.key === group.groupKey)) {
        options.push({ key: group.groupKey, name: group.groupTypeName })
      }
    })
    const previous = this.data.groupOptions[this.data.groupIndex]
    const groupIndex = Math.max(0, options.findIndex(option => previous && option.key === previous.key))
    const expandedEventId = this._groups.some(group => group.key === this.data.expandedEventId)
      ? this.data.expandedEventId : null
    const now = new Date()
    this.setData({ groupOptions: options, groupIndex, expandedEventId,
      eventCount: this._groups.length, eventsLoading: false,
      updatedAt: [now.getHours(), now.getMinutes()].map(value => String(value).padStart(2, '0')).join(':') })
    this.publishEvents()
    if (expandedEventId) await this.loadResults(expandedEventId)
  },

  isCurrent(version) {
    return !this._disposed && version === this._selectionVersion
  },

  publishEvents() {
    const group = this.data.groupOptions[this.data.groupIndex]
    this.setData({ events: filterEvents(this._groups, this.data.keyword,
      this.data.category, group ? group.key : null) })
  },

  onSearchInput(event) {
    this.setData({ keyword: event.detail.value })
    this.publishEvents()
  },

  onCategoryTap(event) {
    const category = event.currentTarget.dataset.category
    this.setData({ category: category === '全部' ? '' : category })
    this.publishEvents()
  },

  onGroupChange(event) {
    this.setData({ groupIndex: Number(event.detail.value) })
    this.publishEvents()
  },

  clearFilters() {
    this.setData({ keyword: '', category: '', groupIndex: 0 })
    this.publishEvents()
  },

  onEventTap(event) {
    const key = event.currentTarget.dataset.key
    const expandedEventId = this.data.expandedEventId === key ? null : key
    this.setData({ expandedEventId })
    if (expandedEventId && !this.data.eventsLoading) return this.loadResults(key)
  },

  retryEvents() {
    return this.selectMeeting(this.data.activeMeetingId)
  },

  retryResults(event) {
    if (!this.data.eventsLoading) return this.loadResults(event.currentTarget.dataset.key)
  },

  async loadResults(key) {
    const group = this._groups.find(item => item.key === key)
    if (!group || group.resultState === 'loading' || group.resultState === 'ready') return
    const version = this._selectionVersion
    group.resultState = 'loading'
    group.resultError = ''
    this.publishEvents()
    try {
      const arrays = await Promise.all(group.rounds.map(round =>
        auth.request({ url: '/api/result/listByEvent?eventId=' + round.id })))
      if (!this.isCurrent(version)) return
      group.results = buildResults([].concat(...arrays.map(rows => rows || [])), this._schedules, group.category)
      group.resultState = 'ready'
    } catch (error) {
      if (!this.isCurrent(version)) return
      group.resultState = 'error'
      group.resultError = '成绩加载失败，请重试'
    }
    this.publishEvents()
  },

  goToRanking() {
    const meeting = this.data.activeMeeting
    if (meeting) wx.navigateTo({ url: '/pages/result/result?sportsMeetingId=' + meeting.id
      + '&name=' + encodeURIComponent(meeting.name) })
  },
})
