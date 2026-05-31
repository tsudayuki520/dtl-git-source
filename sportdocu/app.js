// app.js
const auth = require('./utils/auth')

App({
  onLaunch() {
    // 静默登录：有 token 直接用，没有则 wx.login 换取
    auth.ensureLogin().then(() => {
      console.log('登录成功')
    }).catch(err => {
      console.error('自动登录失败', err)
    })
  },

  globalData: {
    userInfo: null,
  },
})
