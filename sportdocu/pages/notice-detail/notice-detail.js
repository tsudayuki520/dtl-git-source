// notice-detail.js
const auth = require('../../utils/auth')

Page({
  data: {
    notice: {},
  },

  onLoad(options) {
    if (options.id) {
      this.loadDetail(options.id)
    }
  },

  loadDetail(id) {
    auth.request({
      url: '/api/notice/' + id,
      method: 'GET',
    }).then(data => {
      this.setData({ notice: data })
    }).catch(err => {
      console.error('获取通知详情失败', err)
    })
  },
})
