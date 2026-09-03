const { get, post } = require('../../utils/request')
const { t } = require('../../utils/i18n')

Page({
  data: {
    i18n: {},
    status: null,
    channel: '',
    paying: false,
    orders: []
  },

  onLoad() {
    this.load()
  },

  onShow() {
    this.applyI18n()
    this.load()
  },

  applyI18n() {
    this.setData({
      i18n: {
        title: t('vip.title'),
        subtitle: t('vip.subtitle'),
        never: t('vip.never'),
        active: t('vip.active'),
        expireAt: t('vip.expireAt'),
        planName: t('vip.planName'),
        perks: t('vip.perks'),
        perk1: t('vip.perk1'),
        perk2: t('vip.perk2'),
        perk3: t('vip.perk3'),
        payTitle: t('vip.payTitle'),
        payNow: t('vip.payNow'),
        mockNote: t('vip.mockNote'),
        unavailable: t('vip.unavailable'),
        orders: t('vip.orders'),
        noOrders: t('vip.noOrders')
      }
    })
    wx.setNavigationBarTitle({ title: t('vip.title') })
  },

  load() {
    Promise.all([get('/vip/status'), get('/vip/orders')]).then(([status, orders]) => {
      const channels = (status.channels || []).map((c) => c)
      const first = channels.find((c) => c.enabled)
      const STATUS_MAP = {
        PENDING: { label: t('vip.statusPending'), cls: 'pending' },
        PAID: { label: t('vip.statusPaid'), cls: 'paid' },
        FAILED: { label: t('vip.statusFailed'), cls: 'failed' },
        REFUNDED: { label: t('vip.statusRefunded'), cls: 'failed' }
      }
      const orderItems = (orders || []).slice(0, 10).map((o) => {
        const st = STATUS_MAP[o.status] || { label: o.status, cls: 'pending' }
        return {
          orderNo: o.orderNo,
          amount: o.currency === 'USD' ? '$' + o.amount : '¥' + o.amount,
          statusLabel: st.label,
          statusCls: st.cls
        }
      })
      this.setData({
        status: Object.assign({}, status, { channels }),
        orders: orderItems,
        channel: this.data.channel || (first ? first.code : '')
      })
    }).catch(() => {})
  },

  pickChannel(e) {
    const code = e.currentTarget.dataset.code
    const c = this.data.status.channels.find((x) => x.code === code)
    if (!c || !c.enabled) return
    this.setData({ channel: code })
  },

  startPay() {
    const { channel, paying, status } = this.data
    if (!channel || paying || !status || status.vip) return
    this.setData({ paying: true })
    post('/vip/orders', { channel })
      .then((pay) => {
        if (pay.mode === 'mock') {
          // 演示支付：走与真实回调一致的幂等入账
          return post('/vip/mock-pay/' + pay.orderNo).then(() => {
            wx.showToast({ title: t('vip.paySuccess'), icon: 'success' })
            this.afterPaid()
          })
        }
        if (pay.mode === 'jsapi') {
          // 小程序内微信支付（需商户凭证配置后生效）
          const p = pay.jsapi
          return new Promise((resolve) => {
            wx.requestPayment({
              timeStamp: p.timeStamp,
              nonceStr: p.nonceStr,
              package: p.package,
              signType: p.signType || 'MD5',
              paySign: p.paySign,
              success: () => {
                wx.showToast({ title: t('vip.paySuccess'), icon: 'success' })
                this.afterPaid()
              },
              fail: () => {},
              complete: resolve
            })
          })
        }
        if (pay.mode === 'qrcode' || pay.mode === 'redirect') {
          // 支付宝 / PayPal / 扫码：小程序内无法直接跳转外部收银台，复制链接引导到浏览器
          const url = pay.redirectUrl || pay.codeUrl
          wx.setClipboardData({
            data: url,
            success: () => {
              wx.showModal({
                title: t('vip.payTitle'),
                content: t('vip.redirectTip'),
                showCancel: false,
                confirmText: t('common.confirm')
              })
            }
          })
          return Promise.resolve()
        }
        return Promise.resolve()
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ paying: false })
      })
  },

  afterPaid() {
    get('/users/me').then((user) => {
      wx.setStorageSync('user', user)
      getApp().globalData.user = user
    }).catch(() => {})
    this.load()
  },

  goBack() {
    wx.navigateBack({ fail: () => wx.reLaunch({ url: '/pages/index/index' }) })
  }
})
