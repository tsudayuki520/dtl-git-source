// result.js - 成绩公告：团体总分排行
const auth = require('../../utils/auth')

Page({
  data: {
    meetingName: '',
    teams: [],
    loading: false,
  },

  onLoad(options) {
    if (options.sportsMeetingId) {
      const meetingName = options.name ? decodeURIComponent(options.name) : ''
      this.setData({ sportsMeetingId: options.sportsMeetingId, meetingName })
      this.loadRanking(options.sportsMeetingId)
    }
  },

  onShow() {
    if (this.data.sportsMeetingId) {
      this.loadRanking(this.data.sportsMeetingId)
    }
  },

  loadRanking(sportsMeetingId) {
    this.setData({ loading: true })
    auth.request({
      url: '/api/admin/team/list?sportsMeetingId=' + sportsMeetingId,
    }).then((data) => {
      const teams = (data || [])
        .slice()
        .sort((a, b) => (b.totalScore || 0) - (a.totalScore || 0))
        .map((t, i) => ({ ...t, rank: i + 1 }))
      this.setData({ teams, loading: false })
    }).catch((err) => {
      console.error('获取团体总分失败', err)
      this.setData({ loading: false })
    })
  },
})
