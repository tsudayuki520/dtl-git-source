// my-teams.js - 我的队伍：查看当前用户参加的代表队
const auth = require('../../utils/auth')

Page({
  data: {
    teams: [],
    loading: true,
  },

  onLoad() {
    this.loadTeams()
  },

  async loadTeams() {
    this.setData({ loading: true })
    try {
      const data = await auth.request({ url: '/api/my/teams', method: 'GET' })
      this.setData({ teams: data || [] })
    } catch (e) {
      wx.showToast({ title: e?.message || '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },
})
