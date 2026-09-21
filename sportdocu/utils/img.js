// img.js - 华为云 OBS 图片处理（缩放）
//
// 依赖 OBS 桶已开通「图片处理」且对象可匿名读。未开通时该 URL 会 404，
// 调用方需自行兜底（见 pages/index 的 onBannerError）。

/** OBS 直链域名特征，非 OBS 链接（本地图等）原样返回 */
function isObsUrl(url) {
  return url.indexOf('myhuaweicloud.com') !== -1 || url.indexOf('.obs.') !== -1
}

/**
 * 把图片 URL 缩放为指定宽度（等比）。
 * @param {string} url 原图 URL
 * @param {number} w 目标宽度（像素）
 */
function resize(url, w) {
  if (!url || !w) return url
  if (!isObsUrl(url)) return url
  if (url.indexOf('x-image-process') !== -1) return url
  const sep = url.indexOf('?') === -1 ? '?' : '&'
  return url + sep + 'x-image-process=image/resize,w_' + w
}

module.exports = { resize }
