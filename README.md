# LingoPlay 四端交付说明

一套后端（Spring Boot + PostgreSQL，已部署于 https://lingoplay.pocketbay.app）支撑四端：

| 端 | 目录 | 状态 |
|---|---|---|
| Web 网站 | `frontend/` | ✅ 已上线 https://lingoplay.pocketbay.app |
| Android App | `frontend/android/` | ✅ 工程已生成，需 Android Studio 打包 |
| iOS App | `frontend/ios/` | ✅ 工程已生成，需 Mac + Xcode 打包 |
| 微信小程序 | `miniprogram/` | ✅ 代码完成，需微信开发者工具预览 |

---

## 一、Android App（frontend/android）

**本机限制**：本机 Java 8 + 2013 年版 Android SDK 无法编译，需在装有 Android Studio 的机器上打包。

```bash
cd frontend
npm install
npm run build        # 构建前端到 dist/
npx cap sync android  # 同步产物到原生工程
```

然后用 Android Studio 打开 `frontend/android/`，等待 Gradle 同步完成后：
- 运行 ▶：真机/模拟器调试
- Build → Generate Signed Bundle / APK：签名打包发布

要求：Android Studio（含 SDK 34+）、JDK 17。
App 内 API 自动指向线上后端，无需配置。

## 二、iOS App（frontend/ios）

**必须 macOS + Xcode**（Windows 无法编译 iOS）。

```bash
cd frontend
npm run build
npx cap sync ios
```

用 Xcode 打开 `frontend/ios/App/App.xcworkspace`：
- 配置签名团队（Signing & Capabilities）
- 选择模拟器/真机运行，或 Product → Archive 打包

## 三、微信小程序（miniprogram/）

### 1. 导入预览
1. 下载安装「微信开发者工具」https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html
2. 导入项目 → 选择 `miniprogram/` 目录 → AppID 选「测试号」（或填你注册的 AppID）
3. 详情 → 本地设置 → 勾选「不校验合法域名」（开发阶段）
4. 编译即可预览；登录页默认「微信一键登录」，若后端未配置 AppID 会提示，可点「使用账号密码登录」（如 demo/demo123）

### 2. 微信授权登录上线
1. 到 https://mp.weixin.qq.com 注册小程序，获得 AppID 与 AppSecret
2. 后端环境变量配置（本地 application.yml 的 `wx.appid/wx.secret` 或环境变量 `WX_APPID`/`WX_SECRET`）
3. 后台「开发管理 → 服务器域名」把 `https://lingoplay.pocketbay.app` 加入 **request 合法域名**
4. 域名需 HTTPS（已满足）

### 3. 发音功能
小程序语音合成三级降级：
1. 官方「微信同声传译」插件（`app.json` 已声明，仅支持中文/英文）
2. 后端 TTS 代理 `/api/tts`（多语种 + 方言近似发音，见第八节）
3. 均不可用时明确提示失败原因

插件使用需在小程序后台「设置 → 第三方设置 → 插件管理」确认已添加「微信同声传译」；未添加不影响其他功能（自动走后端代理）。

## 四、本地开发

```bash
# 后端（Java 8 + Maven，本地 MySQL）
cd backend
mvn spring-boot:run

# 前端 Web（Vite 代理 /api → localhost:8080）
cd frontend
npm run dev
```

数据库：本地 MySQL（root/123456，库名 lingolearn），云端由 PocketBay 托管 PostgreSQL（DATABASE_URL 自动注入）。

## 五、功能清单（四端一致）

- 微信一键登录（小程序）/ 账号注册登录（Web/App）
- 每日单词（翻卡释义 + 发音 + 换一个）
- 单词闯关：翻牌配对游戏（连击/失误/三星结算）
- 语法探险 / 口语星球 / 听力侦探 / 对话剧场
- 关卡地图（9 语种 × CEFR 分级）、星级记录
- 我的战绩（热力图 + 模块掌握度）
- 学习社区（发帖/评论/点赞）
- 成就系统（自动解锁）+ 个人中心

## 六、v1.0.6 新增功能

### 1. 在线翻译（多语种互译）
- 小程序「翻译」Tab / Web 端「在线翻译」页，四端入口一致
- 支持 70+ 语种互译，源语言**自动检测**
- 译文一键复制 + 朗读发音

### 2. 关卡语种扩容（3 → 9 语种）
- 新增：法语 🇫🇷、西班牙语 🇪🇸、阿拉伯语 🇸🇦、中文普通话 🇨🇳
- 中文方言（VIP 专属）：广东话 🇭🇰 / 四川话 / 北京话 / 上海话
- 方言发音：真人录音接入前由普通话近似替代并明确标注「方言发音开发中」

### 3. VIP 会员（¥5/月）
- 权益：解锁四大方言课程 + 后续新语种优先体验
- 支付渠道：微信支付（小程序 JSAPI / Web 扫码）、支付宝、PayPal / 信用卡（国际用户，$0.99/月）
- 演示支付：`PAY_MOCK_ENABLED=true` 时开放，用于联调，**生产必须关闭**
- 支付安全四道闸：
  1. 金额仅存在于服务端（客户端只传渠道）
  2. 回调强验签（微信 MD5 / 支付宝 RSA2 / PayPal 服务端 Capture 核验）
  3. 回调金额与本地订单逐一核对，不一致直接拒绝
  4. 订单状态机幂等入账，重复通知不重复加时长

### 4. 界面多语言（9 语种）
- 简体中文 / 繁體中文 / English / 日本語 / 한국어 / Español / Français / Tiếng Việt / ไทย
- Web 与 App 共享同一构建，语言偏好互通；小程序独立切换（个人中心）
- 按界面语言自动切换对应文字体系的字体栈（泰文/韩文/越南文等不再退化为中文字体）

## 七、支付渠道环境变量

| 环境变量 | 说明 |
|---|---|
| `WX_PAY_APPID` / `WX_PAY_MCH_ID` / `WX_PAY_MCH_KEY` | 微信支付 V2 商户凭证 |
| `ALIPAY_APP_ID` / `ALIPAY_MERCHANT_KEY` / `ALIPAY_PUBLIC_KEY` | 支付宝开放平台（PKCS8 私钥 + 支付宝公钥） |
| `PAYPAL_MODE`（sandbox/live）/ `PAYPAL_CLIENT_ID` / `PAYPAL_SECRET` | PayPal（信用卡由其收银台代收，我方不接触卡号） |
| `PAY_MOCK_ENABLED` | 演示支付开关，默认 false |

未配置凭证的渠道自动隐藏，前端渠道列表由服务端下发。

## 八、发音链路（v1.0.5 起）

三级降级，任何语种点发音都有明确结果：
1. **微信同声传译插件**（小程序，仅 zh_CN/en_US，已在 `app.json` 声明）
2. **后端 TTS 代理 `/api/tts`**：有道公开端点覆盖 16 种标准语言，限长 200 字 + 单 IP 限频 + 每日配额；支持接入预录方言音频目录（`TTS_ASSETS_DIR`）与付费云 TTS
3. 明确提示失败原因，不再假装成功
