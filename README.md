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

### 3. 发音功能（可选增强）
小程序语音合成基于官方「微信同声传译」插件：
- 小程序后台「设置 → 第三方设置 → 插件管理」→ 搜索「微信同声传译」→ 添加
- 未添加时发音按钮会给出提示，其他功能不受影响

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
- 关卡地图（3 语种 × CEFR 分级）、星级记录
- 我的战绩（热力图 + 模块掌握度）
- 学习社区（发帖/评论/点赞）
- 成就系统（自动解锁）+ 个人中心
