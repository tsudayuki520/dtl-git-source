// profile.js
const auth = require('../../utils/auth')

Page({
  data: {
    userInfo: null,
    defaultAvatar: '/images/default-avatar.png',
  },

  onShow() {
    this.loadUserInfo()
  },

  loadUserInfo() {
    auth.request({
      url: '/api/auth/info',
      method: 'GET',
    }).then(data => {
      this.setData({ userInfo: data })
    }).catch(err => {
      console.error('获取用户信息失败', err)
    })
  },

  // 选择头像
  onChooseAvatar(e) {
    const tempUrl = e.detail.avatarUrl
    if (!tempUrl) return

    wx.showLoading({ title: '上传中...' })

    // 上传到后端
    wx.uploadFile({
      url: auth.BASE_URL + '/api/auth/avatar',
      filePath: tempUrl,
      name: 'file',
      header: {
        'Authorization': 'Bearer ' + auth.getToken()
      },
      success: (res) => {
        wx.hideLoading()
        const data = JSON.parse(res.data)
        if (data.code === 200) {
          this.setData({ 'userInfo.avatarUrl': data.data })
          wx.showToast({ title: '头像已更新', icon: 'success' })
        } else {
          wx.showToast({ title: '上传失败', icon: 'none' })
        }
      },
      fail: (err) => {
        wx.hideLoading()
        console.error('上传头像失败', err)
        wx.showToast({ title: '上传失败', icon: 'none' })
      }
    })
  },

  // 保存昵称（失去焦点时触发）
  onNicknameConfirm(e) {
    const nickname = e.detail.value.trim()
    if (!nickname || nickname === this.data.userInfo.nickname) return

    auth.request({
      url: '/api/auth/nickname',
      method: 'PUT',
      data: { nickname },
    }).then(() => {
      this.setData({ 'userInfo.nickname': nickname })
      wx.showToast({ title: '昵称已更新', icon: 'success' })
    }).catch(err => {
      console.error('更新昵称失败', err)
    })
  },

  // 退出登录
  onLogout() {
    wx.showModal({
      title: '提示',
      content: '确认退出登录？',
      success: (res) => {
        if (res.confirm) {
          auth.logout()
          this.setData({ userInfo: null })
          wx.showToast({ title: '已退出', icon: 'success' })
        }
      }
    })
  },
})
