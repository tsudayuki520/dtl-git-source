const auth = require('../../utils/auth')

Page({
  data: { userCode: '', password: '', loading: false, redirect: '' },
  onLoad(opts) { if (opts.redirect) this.setData({ redirect: decodeURIComponent(opts.redirect) }) },
  onInputCode(e) { this.setData({ userCode: e.detail.value }) },
  onInputPwd(e) { this.setData({ password: e.detail.value }) },
  async handleLogin() {
    const { userCode, password } = this.data
    if (!userCode || !password) { wx.showToast({ title: '请输入账号密码', icon: 'none' }); return }
    this.setData({ loading: true })
    try {
      await auth.login(userCode, password)
      wx.showToast({ title: '登录成功', icon: 'success' })
      if (this.data.redirect) wx.redirectTo({ url: encodeURI(this.data.redirect) })
      else wx.switchTab({ url: '/pages/profile/profile' })
    } catch (e) {
      wx.showToast({ title: e.message || '登录失败', icon: 'none' })
    } finally { this.setData({ loading: false }) }
  }
})
