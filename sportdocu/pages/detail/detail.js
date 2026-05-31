// detail.js
const auth = require('../../utils/auth')

Page({
  data: {
    meeting: {},
    statusMap: { 0: '报名中', 1: '进行中', 2: '已结束' },
    statusColor: { 0: '#4CAF50', 1: '#2196F3', 2: '#999999' },
  },

  onLoad(options) {
    if (options.id) {
      this.loadDetail(options.id)
    }
  },

  loadDetail(id) {
    wx.request({
      url: auth.BASE_URL + '/api/sports-meeting/' + id,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          this.setData({ meeting: res.data.data })
        }
      },
      fail: (err) => {
        console.error('获取运动会详情失败', err)
      }
    })
  },
})
