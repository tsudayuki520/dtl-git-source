// register.js
const auth = require('../../utils/auth')

Page({
  data: {
    categoryGroups: [],
  },

  onLoad(options) {
    if (options.sportsMeetingId) {
      this.setData({ sportsMeetingId: options.sportsMeetingId })
      this.loadEvents(options.sportsMeetingId)
    }
  },

  loadEvents(id) {
    wx.request({
      url: auth.BASE_URL + '/api/event/list?sportsMeetingId=' + id,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const events = (res.data.data || []).filter(e => e.allowRegister)
          const categoryOrder = ['田赛', '径赛', '趣味赛']
          const map = {}
          events.forEach(e => {
            const cat = e.category || '径赛'
            if (!map[cat]) map[cat] = []
            map[cat].push({
              id: e.id,
              name: e.name,
              gender: e.gender,
              groupTypeName: e.groupTypeName,
              registerLimit: e.registerLimit || 0,
              registeredCount: 0,
            })
          })
          const categoryGroups = categoryOrder
            .filter(cat => map[cat] && map[cat].length > 0)
            .map(cat => ({ category: cat, events: map[cat] }))
          this.setData({ categoryGroups })
          this.loadRegisterCount(id)
        }
      },
      fail: (err) => {
        console.error('获取项目失败', err)
      }
    })
  },

  loadRegisterCount(sportsMeetingId) {
    wx.request({
      url: auth.BASE_URL + '/api/register/count?sportsMeetingId=' + sportsMeetingId,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const countMap = res.data.data || {}
          const categoryGroups = this.data.categoryGroups
          categoryGroups.forEach(group => {
            group.events.forEach(evt => {
              evt.registeredCount = countMap[evt.id] || 0
            })
          })
          this.setData({ categoryGroups })
        }
      },
      fail: (err) => {
        console.error('获取报名人数失败', err)
      }
    })
  },

  goToRegister(e) {
    const { id, name, gender, groupType } = e.currentTarget.dataset
    wx.navigateTo({
      url: '/pages/register-form/register-form?eventId=' + id +
           '&eventName=' + encodeURIComponent(name) +
           '&eventGender=' + encodeURIComponent(gender) +
           '&eventGroupType=' + encodeURIComponent(groupType) +
           '&sportsMeetingId=' + this.data.sportsMeetingId
    })
  },
})
