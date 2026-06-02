// register-form.js
const auth = require('../../utils/auth')

Page({
  data: {
    eventId: '',
    eventName: '',
    eventGender: '',
    eventGroupType: '',
    sportsMeetingId: '',
    form: {
      userCode: '',
      name: '',
      phone: '',
      gender: '',
      college: '',
      major: '',
    },
  },

  onLoad(options) {
    this.setData({
      eventId: options.eventId,
      eventName: decodeURIComponent(options.eventName || ''),
      eventGender: decodeURIComponent(options.eventGender || ''),
      eventGroupType: decodeURIComponent(options.eventGroupType || ''),
      sportsMeetingId: options.sportsMeetingId,
    })
  },

  onInputCode(e) { this.setData({ 'form.userCode': e.detail.value }) },
  onInputName(e) { this.setData({ 'form.name': e.detail.value }) },
  onInputPhone(e) { this.setData({ 'form.phone': e.detail.value }) },
  onInputCollege(e) { this.setData({ 'form.college': e.detail.value }) },
  onInputMajor(e) { this.setData({ 'form.major': e.detail.value }) },

  onGenderChange(e) {
    this.setData({ 'form.gender': e.detail.value })
  },

  submit() {
    const { userCode, name, phone, gender, college, major } = this.data.form
    if (!userCode.trim()) { wx.showToast({ title: '请输入学号/工号', icon: 'none' }); return }
    if (!name.trim()) { wx.showToast({ title: '请输入姓名', icon: 'none' }); return }
    if (!phone.trim()) { wx.showToast({ title: '请输入电话号码', icon: 'none' }); return }
    if (!gender) { wx.showToast({ title: '请选择性别', icon: 'none' }); return }

    wx.request({
      url: auth.BASE_URL + '/api/register/submit',
      method: 'POST',
      header: { 'Content-Type': 'application/json' },
      data: {
        sportsMeetingId: this.data.sportsMeetingId,
        eventId: this.data.eventId,
        userCode: userCode.trim(),
        name: name.trim(),
        phone: phone.trim(),
        gender: gender,
        college: college.trim(),
        major: major.trim(),
      },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          wx.showToast({ title: '报名成功', icon: 'success' })
          setTimeout(() => wx.navigateBack(), 1500)
        } else {
          wx.showToast({ title: res.data?.message || '报名失败', icon: 'none' })
        }
      },
      fail: (err) => {
        console.error('报名请求失败', err)
        wx.showToast({ title: '网络错误', icon: 'none' })
      },
    })
  },
})
