const BASE_URL = 'http://localhost:8080'

function getToken() { return wx.getStorageSync('token') || '' }
function isLoggedIn() { return !!getToken() }

function login(userCode, password) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + '/api/auth/login',
      method: 'POST',
      data: { userCode, password },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          wx.setStorageSync('token', res.data.data.token)
          wx.setStorageSync('userInfo', {
            userId: res.data.data.userId,
            userCode: res.data.data.userCode,
            name: res.data.data.name,
          })
          resolve(res.data.data)
        } else { reject(new Error(res.data?.message || '账号密码错误')) }
      },
      fail: () => reject(new Error('网络错误'))
    })
  })
}

function logout() {
  wx.removeStorageSync('token')
  wx.removeStorageSync('userInfo')
}

function request(options) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Authorization': token ? ('Bearer ' + token) : '',
        'Content-Type': 'application/json',
        ...(options.header || {}),
      },
      success: (res) => {
        if (res.statusCode === 401) {
          logout()
          // 跳登录页，带 redirect 回跳当前页
          const pages = getCurrentPages()
          const cur = pages[pages.length - 1]
          const redirect = cur ? '/' + cur.route : ''
          wx.redirectTo({ url: '/pages/login/login?redirect=' + encodeURIComponent(redirect) })
          reject(new Error('未登录'))
          return
        }
        if (res.data && res.data.code === 200) {
          resolve(res.data.data)
        } else {
          reject(res.data)
        }
      },
      fail: (err) => reject(err),
    })
  })
}

function requireLogin(redirect) {
  if (!isLoggedIn()) {
    wx.navigateTo({
      url: '/pages/login/login?redirect=' + encodeURIComponent(redirect || '')
    })
    return false
  }
  return true
}

module.exports = { BASE_URL, getToken, isLoggedIn, login, logout, request, requireLogin }
