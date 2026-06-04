// index.js
const auth = require('../../utils/auth')

Page({
  data: {
    banners: [],
    notices: [],
    meetings: [],
    statusMap: { 0: '筹备中', 1: '报名中', 2: '进行中', 3: '已结束' },
    statusColor: { 0: '#909399', 1: '#4CAF50', 2: '#2196F3', 3: '#999999' },
  },

  onLoad() {
    this.loadBanners()
    this.loadNotices()
    this.loadMeetings()
  },

  onShow() {
    this.loadMeetings()
  },

  loadBanners() {
    // 轮播图是公开接口，不需要 token
    wx.request({
      url: auth.BASE_URL + '/api/banner/list',
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          this.setData({ banners: res.data.data })
        }
      },
      fail: (err) => {
        console.error('获取轮播图失败', err)
      }
    })
  },

  loadNotices() {
    // 全局公告，公开接口不需要 token
    wx.request({
      url: auth.BASE_URL + '/api/notice/global',
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          this.setData({ notices: res.data.data })
        }
      },
      fail: (err) => {
        console.error('获取公告失败', err)
      }
    })
  },

  loadMeetings() {
    wx.request({
      url: auth.BASE_URL + '/api/sports-meeting/list',
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          this.setData({ meetings: res.data.data })
        }
      },
      fail: (err) => {
        console.error('获取运动会列表失败', err)
      }
    })
  },

  onSearch(e) {
    const keyword = e.detail.value.trim()
    if (!keyword) return
    wx.navigateTo({
      url: '/pages/search/search?keyword=' + keyword
    })
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/detail/detail?id=' + id
    })
  },

  goToNoticeList() {
    wx.navigateTo({
      url: '/pages/notice/notice'
    })
  },
})
