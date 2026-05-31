// notice.js
const auth = require('../../utils/auth')

Page({
  data: {
    notices: [],
    onlyUnread: false,
  },

  onLoad() {
    this.loadNotices()
  },

  onShow() {
    this.loadNotices()
  },

  loadNotices() {
    const url = this.data.onlyUnread
      ? '/api/notice/list?onlyUnread=true'
      : '/api/notice/list'

    auth.request({
      url: url,
      method: 'GET',
    }).then(data => {
      this.setData({ notices: data })
    }).catch(err => {
      console.error('获取通知失败', err)
    })
  },

  toggleUnread() {
    this.setData({ onlyUnread: !this.data.onlyUnread })
    this.loadNotices()
  },

  goToNoticeDetail(e) {
    const id = e.currentTarget.dataset.id
    // 标记已读
    auth.request({
      url: '/api/notice/' + id + '/read',
      method: 'POST',
    }).catch(err => {
      console.error('标记已读失败', err)
    })
    wx.navigateTo({
      url: '/pages/notice-detail/notice-detail?id=' + id
    })
  },
})
