// profile.js
const auth = require('../../utils/auth')

Page({
  data: {
    userInfo: null,
  },

  onShow() {
    if (!auth.isLoggedIn()) {
      this.setData({ userInfo: null })
      return
    }
    // 登录时 auth.login 存的 userInfo = {userId, userCode, name}
    this.setData({ userInfo: wx.getStorageSync('userInfo') || null })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  },

  goMyRegistrations() {
    if (!auth.requireLogin('/pages/my-registrations/my-registrations')) return
    wx.navigateTo({ url: '/pages/my-registrations/my-registrations' })
  },

  changePassword() {
    if (!auth.requireLogin('/pages/change-password/change-password')) return
    wx.navigateTo({ url: '/pages/change-password/change-password' })
  },

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
      },
    })
  },
})
