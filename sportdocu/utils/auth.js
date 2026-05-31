const BASE_URL = 'http://localhost:8080'

/**
 * 获取本地存储的 token
 */
function getToken() {
  return wx.getStorageSync('token') || ''
}

/**
 * 保存 token 到本地
 */
function setToken(token) {
  wx.setStorageSync('token', token)
}

/**
 * 获取用户信息缓存
 */
function getUserInfo() {
  return wx.getStorageSync('userInfo') || null
}

/**
 * 保存用户信息到本地
 */
function setUserInfo(info) {
  wx.setStorageSync('userInfo', info)
}

/**
 * 静默登录：wx.login() → 后端换取 token
 * 返回 Promise，resolve(token)
 */
function silentLogin() {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (res) => {
        if (!res.code) {
          reject(new Error('wx.login 失败'))
          return
        }
        wx.request({
          url: BASE_URL + '/api/auth/login',
          method: 'POST',
          data: { code: res.code },
          success: (resp) => {
            if (resp.data && resp.data.code === 200) {
              const data = resp.data.data
              setToken(data.token)
              setUserInfo({
                userId: data.userId,
                nickname: data.nickname,
                avatarUrl: data.avatarUrl,
                phone: data.phone,
              })
              resolve(data.token)
            } else {
              reject(new Error(resp.data?.message || '登录失败'))
            }
          },
          fail: (err) => reject(err),
        })
      },
      fail: (err) => reject(err),
    })
  })
}

/**
 * 确保已登录：有 token 直接返回，没有则静默登录
 * 返回 Promise，resolve(token)
 */
function ensureLogin() {
  const token = getToken()
  if (token) {
    return Promise.resolve(token)
  }
  return silentLogin()
}

/**
 * 封装带 token 的请求，401 时自动重新登录
 * 用法: auth.request({ url: '/api/xxx', method: 'GET', data: {} })
 */
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
      },
      success: (res) => {
        if (res.statusCode === 401) {
          // token 过期，重新登录后重试
          silentLogin().then(() => {
            request(options).then(resolve).catch(reject)
          }).catch(reject)
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

/**
 * 绑定手机号（需要企业认证的小程序才能用）
 * 在 <button open-type="getPhoneNumber"> 的回调中调用
 */
function bindPhone(code) {
  return request({
    url: '/api/auth/phone',
    method: 'POST',
    data: { code: code },
  })
}

/**
 * 退出登录
 */
function logout() {
  wx.removeStorageSync('token')
  wx.removeStorageSync('userInfo')
}

module.exports = {
  BASE_URL,
  getToken,
  setToken,
  getUserInfo,
  setUserInfo,
  silentLogin,
  ensureLogin,
  request,
  bindPhone,
  logout,
}
