// cache.js - 带 TTL 的本地缓存
//
// 用法只限于「首屏秒出」：先 cache.get 渲染，网络请求照常发，回来再覆盖。
// 不要写成「命中就跳过请求」——后台改了数据小程序必须能马上看到。

const PREFIX = 'cache:'

/** 取缓存；不存在或已过期返回 null */
function get(key) {
  const full = PREFIX + key
  const v = wx.getStorageSync(full)
  if (!v || !v.expire) return null
  if (Date.now() > v.expire) {
    wx.removeStorageSync(full)
    return null
  }
  return v.data
}

/** 写缓存，ttlMs 毫秒后过期 */
function set(key, data, ttlMs) {
  try {
    wx.setStorageSync(PREFIX + key, { data, expire: Date.now() + ttlMs })
  } catch (e) {
    // 存储写满等异常不影响正常流程
    console.warn('写缓存失败', key, e)
  }
}

function remove(key) {
  wx.removeStorageSync(PREFIX + key)
}

module.exports = { get, set, remove }
