// register.js
const auth = require('../../utils/auth')

Page({
  data: {
    categoryGroups: [],
    emptyTip: '暂无可报名项目',
  },

  onLoad(options) {
    if (options.sportsMeetingId) {
      this.setData({ sportsMeetingId: options.sportsMeetingId })
    }
  },

  // 每次进入/返回本页都重新拉取，后台新增项目后可见
  onShow() {
    const id = this.data.sportsMeetingId
    if (id) {
      this.loadEvents(id)
    }
  },

  // 有代表队就只列出「面向本人组别」的项目，与服务端 RegistrationServiceImpl 的组别校验保持一致；
  // 没有代表队/组别时不带 groupTypeId，列出全部项目 —— 与服务端一致：这类人只受项目自身规则约束。
  async loadEvents(id) {
    if (!auth.isLoggedIn()) {
      this.setData({ categoryGroups: [], emptyTip: '请先登录后再报名' })
      return
    }

    let groupTypeId = null
    try {
      const teams = await auth.request({ url: '/api/my/teams', method: 'GET' })
      const team = (teams || []).find(t => String(t.sportsMeetingId) === String(id))
      groupTypeId = team ? team.groupTypeId : null
    } catch (err) {
      console.error('获取我的代表队失败', err)
    }

    let url = auth.BASE_URL + '/api/event/list?sportsMeetingId=' + id
    if (groupTypeId) url += '&groupTypeId=' + groupTypeId

    wx.request({
      url: url,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const events = (res.data.data || []).filter(e => e.allowRegister && e.currentOpenScheduleId)
          const categoryOrder = ['田赛', '径赛', '团队赛']
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
          this.setData({ categoryGroups, emptyTip: '暂无可报名项目' })
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
