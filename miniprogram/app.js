// LingoPlay 小程序入口
App({
  onLaunch() {
    const token = wx.getStorageSync('token')
    if (!token) {
      wx.reLaunch({ url: '/pages/login/login' })
    }
  },
  globalData: {
    user: null
  }
})