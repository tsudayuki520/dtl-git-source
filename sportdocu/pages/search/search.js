// search.js
const auth = require('../../utils/auth')

Page({
  data: {
    keyword: '',
    results: [],
    statusMap: { 0: '筹备中', 1: '报名中', 2: '进行中', 3: '已结束' },
    statusColor: { 0: '#909399', 1: '#4CAF50', 2: '#2196F3', 3: '#999999' },
  },

  onLoad(options) {
    const keyword = options.keyword || ''
    this.setData({ keyword })
    if (keyword) {
      this.search(keyword)
    }
  },

  onSearch(e) {
    const keyword = e.detail.value.trim()
    if (!keyword) return
    this.setData({ keyword })
    this.search(keyword)
  },

  search(keyword) {
    wx.request({
      url: auth.BASE_URL + '/api/sports-meeting/search?keyword=' + keyword,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          this.setData({ results: res.data.data || [] })
        }
      },
      fail: (err) => {
        console.error('搜索失败', err)
      }
    })
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/detail/detail?id=' + id
    })
  },
})
