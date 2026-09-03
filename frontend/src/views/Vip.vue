<template>
  <div class="page-container vip-page">
    <PageBack to="/" :label="t('common.back')" />
    <h1 class="page-title">{{ t('vip.title') }}</h1>
    <p class="page-subtitle">{{ t('vip.subtitle') }}</p>

    <!-- 会员状态卡 -->
    <div class="vip-hero" :class="{ active: status.vip }">
      <div class="vip-hero-badge">👑</div>
      <div class="vip-hero-main">
        <div class="vip-hero-title">
          <span class="vip-tag">{{ t('vip.badge') }}</span>
          <span v-if="status.vip">{{ t('vip.active') }}</span>
          <span v-else-if="status.expired">{{ t('vip.expired') }}</span>
          <span v-else>{{ t('vip.never') }}</span>
        </div>
        <div class="vip-hero-meta" v-if="status.vipExpireAt">
          {{ t('vip.expireAt') }}：{{ formatDate(status.vipExpireAt) }}
          <template v-if="status.vipMonths">
            · {{ t('vip.totalMonths') }} {{ status.vipMonths }} {{ t('vip.months') }}
          </template>
        </div>
      </div>
    </div>

    <!-- 套餐与权益 -->
    <div class="plan-card">
      <div class="plan-price-row">
        <div class="plan-price">
          <span class="price-symbol">¥</span><span class="price-num">5</span>
          <span class="price-unit">{{ t('vip.perMonth') }}</span>
        </div>
        <div class="plan-name">{{ t('vip.planName') }}</div>
      </div>
      <div class="plan-perks">
        <div class="perk-title">{{ t('vip.perks') }}</div>
        <div class="perk-item"><el-icon><Star /></el-icon>{{ t('vip.perk1') }}</div>
        <div class="perk-item"><el-icon><Star /></el-icon>{{ t('vip.perk2') }}</div>
        <div class="perk-item"><el-icon><Star /></el-icon>{{ t('vip.perk3') }}</div>
      </div>
    </div>

    <!-- 支付方式 -->
    <div class="pay-section" v-if="!status.vip">
      <div class="pay-title">{{ t('vip.payTitle') }}</div>
      <div class="channel-grid">
        <button
          v-for="c in status.channels"
          :key="c.code"
          class="channel-card"
          :class="{ selected: channel === c.code, disabled: !c.enabled }"
          :disabled="!c.enabled"
          @click="channel = c.code"
        >
          <span class="channel-icon">{{ c.icon }}</span>
          <span class="channel-name">{{ c.name }}</span>
          <span v-if="c.code === 'PAYPAL'" class="channel-note">{{ t('vip.paypalNote') }}</span>
          <span v-else-if="c.code === 'MOCK'" class="channel-note">{{ t('vip.mockNote') }}</span>
          <span v-else-if="!c.enabled" class="channel-note">{{ t('vip.unavailable') }}</span>
        </button>
      </div>

      <el-button
        type="primary"
        size="large"
        round
        class="pay-btn"
        :loading="paying"
        :disabled="!channel"
        @click="startPay"
      >
        {{ t('vip.payNow') }} · ¥5{{ t('vip.perMonth') }}
      </el-button>

      <div class="security-note">
        <el-icon><Lock /></el-icon>{{ t('vip.security') }}
      </div>
    </div>

    <!-- 我的订单 -->
    <div class="orders" v-if="orders.length">
      <div class="orders-title">{{ t('vip.orders') }}</div>
      <div v-for="o in orders" :key="o.orderNo" class="order-row">
        <span class="order-no">{{ o.orderNo }}</span>
        <span class="order-amount">¥{{ o.amount }}</span>
        <el-tag size="small" :type="statusType(o.status)">{{ statusText(o.status) }}</el-tag>
      </div>
    </div>

    <!-- 微信扫码弹窗 -->
    <el-dialog v-model="qrVisible" :title="t('vip.qrcodeTitle')" width="360px" align-center>
      <div class="qr-box">
        <img v-if="qrDataUrl" :src="qrDataUrl" alt="WeChat Pay QR" class="qr-img" />
        <div class="qr-tip">{{ t('vip.qrcodeTip') }}</div>
        <div class="qr-waiting" v-if="polling">
          <el-icon class="is-loading spin"><Loading /></el-icon>
          {{ t('vip.waiting') }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import { vipApi } from '../api'
import { useUserStore } from '../store/user'
import PageBack from '../components/PageBack.vue'

const { t } = useI18n()
const route = useRoute()
const store = useUserStore()

const status = ref({})
const channel = ref('')
const paying = ref(false)
const orders = ref([])
const qrVisible = ref(false)
const qrDataUrl = ref('')
const polling = ref(false)
let pollTimer = null
let activeOrderNo = ''

onMounted(async () => {
  await load()
  // PayPal/支付宝回跳结果提示（?pay=success|fail|cancel）
  const pay = route.query.pay
  if (pay === 'success') {
    ElMessage.success(t('vip.paySuccess'))
    await store.fetchMe()
    await load()
  } else if (pay === 'fail') {
    ElMessage.error(t('vip.payFail'))
  } else if (pay === 'cancel') {
    ElMessage.info(t('vip.payCancel'))
  }
})

onBeforeUnmount(() => stopPoll())

async function load() {
  try {
    status.value = await vipApi.status()
    orders.value = (await vipApi.myOrders()).slice(0, 10)
    // 默认选中第一个可用渠道
    if (!channel.value) {
      const first = (status.value.channels || []).find((c) => c.enabled)
      if (first) channel.value = first.code
    }
  } catch (e) { /* 未登录时由路由守卫处理 */ }
}

async function startPay() {
  if (!channel.value || paying.value) return
  paying.value = true
  try {
    const pay = await vipApi.createOrder(channel.value)
    activeOrderNo = pay.orderNo
    if (pay.mode === 'mock') {
      await vipApi.mockPay(pay.orderNo)
      ElMessage.success(t('vip.paySuccess'))
      await afterPaid()
    } else if (pay.mode === 'qrcode') {
      qrDataUrl.value = await QRCode.toDataURL(pay.codeUrl, { width: 260, margin: 1 })
      qrVisible.value = true
      startPoll()
    } else if (pay.mode === 'redirect') {
      ElMessage.info(t('vip.redirecting'))
      window.location.href = pay.redirectUrl
    } else if (pay.mode === 'jsapi') {
      // Web 端不会出现；App 端由 Capacitor 微信插件处理（预留）
      ElMessage.warning(t('vip.unavailable'))
    }
  } catch (e) { /* request 层已提示 */ } finally {
    paying.value = false
  }
}

function startPoll() {
  stopPoll()
  polling.value = true
  pollTimer = setInterval(async () => {
    try {
      const o = await vipApi.orderStatus(activeOrderNo)
      if (o && o.status === 'PAID') {
        stopPoll()
        qrVisible.value = false
        ElMessage.success(t('vip.paySuccess'))
        await afterPaid()
      }
    } catch (e) { /* 单次轮询失败忽略 */ }
  }, 2000)
}

function stopPoll() {
  polling.value = false
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function afterPaid() {
  await store.fetchMe()
  await load()
}

function formatDate(s) {
  if (!s) return ''
  return String(s).replace('T', ' ').slice(0, 16)
}

function statusText(s) {
  const map = {
    PENDING: t('vip.statusPending'),
    PAID: t('vip.statusPaid'),
    FAILED: t('vip.statusFailed'),
    REFUNDED: t('vip.statusRefunded')
  }
  return map[s] || s
}

function statusType(s) {
  const map = { PENDING: 'warning', PAID: 'success', FAILED: 'info', REFUNDED: 'danger' }
  return map[s] || 'info'
}
</script>

<style scoped>
.vip-page {
  max-width: 760px;
  margin: 0 auto;
}

/* 状态卡 */
.vip-hero {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 22px;
  border-radius: 16px;
  border: 1px solid rgba(120, 150, 255, 0.2);
  background: rgba(255, 255, 255, 0.04);
  margin: 16px 0 14px;
}

.vip-hero.active {
  background: linear-gradient(120deg, rgba(255, 200, 90, 0.14), rgba(255, 160, 60, 0.05));
  border-color: rgba(255, 200, 90, 0.4);
}

.vip-hero-badge {
  font-size: 40px;
  filter: drop-shadow(0 0 12px rgba(255, 200, 90, 0.5));
}

.vip-hero-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 700;
  color: var(--ll-text, #e8ecf8);
}

.vip-tag {
  background: linear-gradient(90deg, #ffb84f, #ff9a3c);
  color: #2b1a00;
  font-size: 12px;
  font-weight: 800;
  padding: 2px 10px;
  border-radius: 12px;
  letter-spacing: 1px;
}

.vip-hero-meta {
  margin-top: 6px;
  font-size: 13px;
  color: var(--ll-text-muted, #8a97b8);
}

/* 套餐 */
.plan-card {
  border-radius: 16px;
  border: 1px solid rgba(255, 200, 90, 0.35);
  background:
    radial-gradient(120% 160% at 0% 0%, rgba(255, 184, 79, 0.16), transparent 55%),
    rgba(255, 255, 255, 0.04);
  padding: 20px 22px;
  margin-bottom: 16px;
}

.plan-price-row {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin-bottom: 14px;
}

.plan-price {
  color: #ffb84f;
  font-weight: 800;
}

.price-symbol {
  font-size: 22px;
}

.price-num {
  font-size: 44px;
  line-height: 1;
}

.price-unit {
  font-size: 14px;
  color: var(--ll-text-muted, #8a97b8);
  margin-left: 4px;
}

.plan-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--ll-text, #e8ecf8);
}

.perk-title {
  font-size: 13px;
  color: var(--ll-text-muted, #8a97b8);
  margin-bottom: 8px;
}

.perk-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--ll-text, #e8ecf8);
  padding: 5px 0;
}

.perk-item .el-icon {
  color: #ffb84f;
}

/* 支付 */
.pay-title {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 10px;
  color: var(--ll-text, #e8ecf8);
}

.channel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.channel-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 14px;
  border-radius: 12px;
  border: 1px solid rgba(120, 150, 255, 0.22);
  background: rgba(255, 255, 255, 0.04);
  cursor: pointer;
  font-family: inherit;
  color: var(--ll-text, #e8ecf8);
  text-align: left;
  transition: border-color 0.2s, background 0.2s;
}

.channel-card:hover:not(.disabled) {
  background: rgba(79, 124, 255, 0.1);
}

.channel-card.selected {
  border-color: var(--ll-cyan, #22d3ee);
  background: rgba(34, 211, 238, 0.08);
  box-shadow: inset 0 0 0 1px rgba(34, 211, 238, 0.4);
}

.channel-card.disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.channel-icon {
  font-size: 24px;
}

.channel-name {
  font-size: 14px;
  font-weight: 700;
}

.channel-note {
  font-size: 11px;
  color: var(--ll-text-muted, #8a97b8);
  line-height: 1.5;
}

.pay-btn {
  width: 100%;
  background: linear-gradient(90deg, #ffb84f, #ff9a3c);
  border: none;
  font-weight: 800;
  font-size: 15px;
}

.security-note {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  margin-top: 14px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--ll-text-muted, #8a97b8);
}

.security-note .el-icon {
  margin-top: 2px;
  flex-shrink: 0;
  color: #6ee7a0;
}

/* 订单 */
.orders {
  margin-top: 20px;
}

.orders-title {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 10px;
  color: var(--ll-text, #e8ecf8);
}

.order-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border: 1px solid rgba(120, 150, 255, 0.14);
  border-radius: 10px;
  margin-bottom: 6px;
  background: rgba(255, 255, 255, 0.03);
  font-size: 13px;
}

.order-no {
  flex: 1;
  color: var(--ll-text-muted, #8a97b8);
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
}

.order-amount {
  font-weight: 700;
  color: var(--ll-text, #e8ecf8);
}

/* 扫码弹窗 */
.qr-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.qr-img {
  width: 260px;
  height: 260px;
  border-radius: 10px;
  background: #fff;
  padding: 8px;
}

.qr-tip {
  font-size: 13px;
  color: var(--ll-text-muted, #8a97b8);
  text-align: center;
  line-height: 1.6;
}

.qr-waiting {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--ll-cyan, #22d3ee);
}

.spin {
  font-size: 16px;
}
</style>
