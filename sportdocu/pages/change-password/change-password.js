// change-password.js
const auth = require('../../utils/auth')

Page({
  data: {
    oldPassword: '',
    newPassword: '',
    confirm: '',
    loading: false,
  },

  onInputOld(e) { this.setData({ oldPassword: e.detail.value }) },
  onInputNew(e) { this.setData({ newPassword: e.detail.value }) },
  onInputConfirm(e) { this.setData({ confirm: e.detail.value }) },

  async handleSubmit() {
    const { oldPassword, newPassword, confirm } = this.data
    if (!oldPassword || !newPassword) {
      wx.showToast({ title: '请填写完整', icon: 'none' })
      return
    }
    if (newPassword !== confirm) {
      wx.showToast({ title: '两次新密码不一致', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    try {
      await auth.request({
        url: '/api/auth/change-password',
        method: 'POST',
        data: { oldPassword, newPassword },
      })
      wx.showToast({ title: '修改成功，请重新登录', icon: 'success' })
      // 密码改了，token 仍有效但语义上应重新登录
      setTimeout(() => {
        auth.logout()
        wx.redirectTo({ url: '/pages/login/login' })
      }, 1500)
    } catch (e) {
      wx.showToast({ title: e?.message || '修改失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },
})
