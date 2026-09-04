// LingoPlay 小程序入口
App({
  onLaunch() {
    // 游客模式：不再强制跳转登录页，游客可直接进入主页浏览每日单词与课程；
    // 触发需要登录的操作（学习、闯关提交、社区发言等）时再引导登录
    // （见 utils/request.js 的 401 统一处理）
  },
  globalData: {
    user: null
  }
})
