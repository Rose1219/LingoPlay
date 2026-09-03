// 界面多语言文案（与 Web 端 key 结构保持一致的子集）
// 9 语种：简体/繁體/EN/JA/KO/ES/FR/VI/TH
const zhCN = {
  tab: { home: '游戏大厅', courses: '关卡地图', progress: '我的战绩', community: '学习社区', profile: '我的' },
  common: { back: '返回', save: '保存修改', confirm: '确定', cancel: '取消', copy: '复制', copied: '已复制', logout: '退出登录' },
  translate: {
    title: '在线翻译', subtitle: '多语种互译 · 自动检测源语言',
    auto: '自动检测', placeholder: '输入要翻译的内容…', translateBtn: '翻译', translating: '翻译中…',
    detected: '检测到', resultPlaceholder: '翻译结果将显示在这里', swap: '互换', copyResult: '复制译文',
    play: '朗读译文', failed: '翻译失败，请稍后再试', autoSwitchedTo: '已切换目标语言为'
  },
  vip: {
    title: 'VIP 会员', subtitle: '解锁方言等专属语种课程，持续上新不断更',
    never: '未开通会员', active: '会员生效中', expireAt: '到期时间',
    planName: 'VIP 月卡', perks: '专属权益',
    perk1: '方言课程：广东话 / 四川话 / 北京话 / 上海话',
    perk2: '新语种课程优先体验', perk3: '专属 VIP 身份标识',
    payTitle: '选择支付方式', payNow: '立即开通 · ¥5/月', unavailable: '暂未开通',
    mockNote: '测试环境模拟支付，不产生真实扣款',
    paySuccess: '支付成功，VIP 已开通！', redirectTip: '请在电脑或手机浏览器打开链接完成支付',
    orders: '我的订单', statusPending: '待支付', statusPaid: '已支付', statusFailed: '已关闭', statusRefunded: '已退款',
    noOrders: '暂无订单'
  },
  courses: { all: '全部', vipOnly: 'VIP 专属', locked: 'VIP 专属语种，开通会员后可学习', goVip: '去开通' },
  profile: {
    title: '个人中心', subtitle: '管理资料与学习偏好', nickname: '昵称', nicknamePh: '设置你的昵称',
    prefLang: '学习语种（影响每日单词与推荐）', uiLang: '界面语言', achievements: '🏆 成就殿堂'
  }
}

const zhTW = {
  tab: { home: '遊戲大廳', courses: '關卡地圖', progress: '我的戰績', community: '學習社區', profile: '我的' },
  common: { back: '返回', save: '保存修改', confirm: '確定', cancel: '取消', copy: '複製', copied: '已複製', logout: '登出' },
  translate: {
    title: '線上翻譯', subtitle: '多語種互譯 · 自動偵測來源語言',
    auto: '自動偵測', placeholder: '輸入要翻譯的內容…', translateBtn: '翻譯', translating: '翻譯中…',
    detected: '偵測到', resultPlaceholder: '翻譯結果將顯示在這裡', swap: '互換', copyResult: '複製譯文',
    play: '朗讀譯文', failed: '翻譯失敗，請稍後再試', autoSwitchedTo: '已切換目標語言為'
  },
  vip: {
    title: 'VIP 會員', subtitle: '解鎖方言等專屬語種課程，持續上新不斷更',
    never: '未開通會員', active: '會員生效中', expireAt: '到期時間',
    planName: 'VIP 月卡', perks: '專屬權益',
    perk1: '方言課程：廣東話 / 四川話 / 北京話 / 上海話',
    perk2: '新語種課程優先體驗', perk3: '專屬 VIP 身分標識',
    payTitle: '選擇支付方式', payNow: '立即開通 · ¥5/月', unavailable: '暫未開通',
    mockNote: '測試環境模擬支付，不產生真實扣款',
    paySuccess: '支付成功，VIP 已開通！', redirectTip: '請在電腦或手機瀏覽器打開連結完成支付',
    orders: '我的訂單', statusPending: '待支付', statusPaid: '已支付', statusFailed: '已關閉', statusRefunded: '已退款',
    noOrders: '暫無訂單'
  },
  courses: { all: '全部', vipOnly: 'VIP 專屬', locked: 'VIP 專屬語種，開通會員後可學習', goVip: '去開通' },
  profile: {
    title: '個人中心', subtitle: '管理資料與學習偏好', nickname: '暱稱', nicknamePh: '設置你的暱稱',
    prefLang: '學習語種（影響每日單詞與推薦）', uiLang: '介面語言', achievements: '🏆 成就殿堂'
  }
}

const en = {
  tab: { home: 'Arcade', courses: 'Levels', progress: 'Stats', community: 'Community', profile: 'Me' },
  common: { back: 'Back', save: 'Save', confirm: 'OK', cancel: 'Cancel', copy: 'Copy', copied: 'Copied', logout: 'Sign out' },
  translate: {
    title: 'Translate', subtitle: '100+ languages · auto-detect',
    auto: 'Detect language', placeholder: 'Type something to translate…', translateBtn: 'Translate', translating: 'Translating…',
    detected: 'Detected', resultPlaceholder: 'Translation appears here', swap: 'Swap', copyResult: 'Copy',
    play: 'Read aloud', failed: 'Translation failed, please try again', autoSwitchedTo: 'Target language switched to '
  },
  vip: {
    title: 'VIP Membership', subtitle: 'Unlock dialect courses and more languages',
    never: 'No membership yet', active: 'Membership active', expireAt: 'Expires on',
    planName: 'VIP Monthly', perks: 'Member perks',
    perk1: 'Dialect courses: Cantonese / Sichuanese / Beijing / Shanghainese',
    perk2: 'Early access to new languages', perk3: 'Exclusive VIP badge',
    payTitle: 'Choose a payment method', payNow: 'Subscribe · $0.99/mo', unavailable: 'Not available yet',
    mockNote: 'Simulated payment for testing, no real charge',
    paySuccess: 'Payment received — VIP activated!', redirectTip: 'Open the link in a browser to finish payment',
    orders: 'My orders', statusPending: 'Pending', statusPaid: 'Paid', statusFailed: 'Closed', statusRefunded: 'Refunded',
    noOrders: 'No orders yet'
  },
  courses: { all: 'All', vipOnly: 'VIP only', locked: 'VIP-only language. Subscribe to unlock.', goVip: 'Unlock' },
  profile: {
    title: 'Account', subtitle: 'Manage profile & preferences', nickname: 'Nickname', nicknamePh: 'Set your nickname',
    prefLang: 'Learning languages', uiLang: 'App language', achievements: '🏆 Achievements'
  }
}

const ja = {
  tab: { home: 'ゲームホール', courses: 'ステージ', progress: '記録', community: 'コミュニティ', profile: 'マイ' },
  common: { back: '戻る', save: '保存', confirm: 'OK', cancel: 'キャンセル', copy: 'コピー', copied: 'コピーしました', logout: 'ログアウト' },
  translate: {
    title: '翻訳', subtitle: '多言語対応 · 自動検出',
    auto: '自動検出', placeholder: '翻訳したい内容を入力…', translateBtn: '翻訳', translating: '翻訳中…',
    detected: '検出', resultPlaceholder: '翻訳結果がここに表示されます', swap: '入替', copyResult: 'コピー',
    play: '読み上げ', failed: '翻訳に失敗しました', autoSwitchedTo: '翻訳先の言語を切り替えました：'
  },
  vip: {
    title: 'VIP会員', subtitle: '方言コースなどの限定語学を解放',
    never: '未加入', active: '会員有効中', expireAt: '有効期限',
    planName: 'VIP月額', perks: '会員特典',
    perk1: '方言コース：広東語 / 四川語 / 北京語 / 上海語',
    perk2: '新言語をいち早く体験', perk3: '限定VIPバッジ',
    payTitle: '支払い方法を選択', payNow: '今すぐ加入 · ¥99/月', unavailable: '未対応',
    mockNote: 'テスト用の模擬決済です',
    paySuccess: '支払い完了、VIPが有効になりました！', redirectTip: 'ブラウザでリンクを開いて支払いを完了してください',
    orders: '注文履歴', statusPending: '未払い', statusPaid: '支払済', statusFailed: 'クローズ', statusRefunded: '返金済',
    noOrders: '注文はありません'
  },
  courses: { all: 'すべて', vipOnly: 'VIP限定', locked: 'VIP限定の言語です。会員で解放。', goVip: '解放する' },
  profile: {
    title: 'マイページ', subtitle: 'プロフィールと設定', nickname: 'ニックネーム', nicknamePh: 'ニックネームを設定',
    prefLang: '学習言語', uiLang: '表示言語', achievements: '🏆 実績'
  }
}

const ko = {
  tab: { home: '게임홀', courses: '레벨맵', progress: '기록', community: '커뮤니티', profile: '내 정보' },
  common: { back: '뒤로', save: '저장', confirm: '확인', cancel: '취소', copy: '복사', copied: '복사됨', logout: '로그아웃' },
  translate: {
    title: '번역', subtitle: '다국어 번역 · 자동 감지',
    auto: '자동 감지', placeholder: '번역할 내용을 입력하세요…', translateBtn: '번역', translating: '번역 중…',
    detected: '감지됨', resultPlaceholder: '번역 결과가 여기에 표시됩니다', swap: '교체', copyResult: '복사',
    play: '읽기', failed: '번역에 실패했습니다', autoSwitchedTo: '대상 언어가 다음으로 변경되었습니다: '
  },
  vip: {
    title: 'VIP 멤버십', subtitle: '방언 코스 등 전용 언어를 잠금 해제',
    never: '미가입', active: '멤버십 이용 중', expireAt: '만료일',
    planName: 'VIP 월간', perks: '멤버 혜택',
    perk1: '방언 코스: 광둥어 / 쓰촨어 / 베이징어 / 상하이어',
    perk2: '신규 언어 우선 체험', perk3: '전용 VIP 배지',
    payTitle: '결제 수단 선택', payNow: '지금 가입 · ₩1,300/월', unavailable: '아직 미지원',
    mockNote: '테스트용 모의 결제입니다',
    paySuccess: '결제 완료, VIP가 활성화되었습니다!', redirectTip: '브라우저에서 링크를 열어 결제를 완료하세요',
    orders: '주문 내역', statusPending: '결제 대기', statusPaid: '결제 완료', statusFailed: '종료됨', statusRefunded: '환불됨',
    noOrders: '주문이 없습니다'
  },
  courses: { all: '전체', vipOnly: 'VIP 전용', locked: 'VIP 전용 언어입니다. 멤버십으로 잠금 해제.', goVip: '잠금 해제' },
  profile: {
    title: '내 정보', subtitle: '프로필과 설정', nickname: '닉네임', nicknamePh: '닉네임을 설정하세요',
    prefLang: '학습 언어', uiLang: '표시 언어', achievements: '🏆 업적'
  }
}

const es = {
  tab: { home: 'Arcade', courses: 'Niveles', progress: 'Stats', community: 'Comunidad', profile: 'Yo' },
  common: { back: 'Volver', save: 'Guardar', confirm: 'Aceptar', cancel: 'Cancelar', copy: 'Copiar', copied: 'Copiado', logout: 'Salir' },
  translate: {
    title: 'Traducir', subtitle: 'Más de 100 idiomas · detección automática',
    auto: 'Detectar idioma', placeholder: 'Escribe algo para traducir…', translateBtn: 'Traducir', translating: 'Traduciendo…',
    detected: 'Detectado', resultPlaceholder: 'La traducción aparecerá aquí', swap: 'Cambiar', copyResult: 'Copiar',
    play: 'Leer', failed: 'Error al traducir', autoSwitchedTo: 'Idioma de destino cambiado a '
  },
  vip: {
    title: 'Membresía VIP', subtitle: 'Desbloquea cursos de dialectos y más idiomas',
    never: 'Sin membresía', active: 'Membresía activa', expireAt: 'Vence el',
    planName: 'VIP mensual', perks: 'Ventajas',
    perk1: 'Cursos de dialectos: cantonés / sichuanés / pekinés / shanghainés',
    perk2: 'Acceso anticipado a nuevos idiomas', perk3: 'Insignia VIP exclusiva',
    payTitle: 'Elige el método de pago', payNow: 'Suscribirse · 0,99 $/mes', unavailable: 'Aún no disponible',
    mockNote: 'Pago simulado para pruebas',
    paySuccess: '¡Pago recibido, VIP activado!', redirectTip: 'Abre el enlace en un navegador para terminar el pago',
    orders: 'Mis pedidos', statusPending: 'Pendiente', statusPaid: 'Pagado', statusFailed: 'Cerrado', statusRefunded: 'Reembolsado',
    noOrders: 'Aún no hay pedidos'
  },
  courses: { all: 'Todos', vipOnly: 'Solo VIP', locked: 'Idioma solo VIP. Suscríbete para desbloquear.', goVip: 'Desbloquear' },
  profile: {
    title: 'Cuenta', subtitle: 'Perfil y preferencias', nickname: 'Apodo', nicknamePh: 'Configura tu apodo',
    prefLang: 'Idiomas de estudio', uiLang: 'Idioma', achievements: '🏆 Logros'
  }
}

const fr = {
  tab: { home: 'Arcade', courses: 'Niveaux', progress: 'Stats', community: 'Communauté', profile: 'Moi' },
  common: { back: 'Retour', save: 'Enregistrer', confirm: 'OK', cancel: 'Annuler', copy: 'Copier', copied: 'Copié', logout: 'Déconnexion' },
  translate: {
    title: 'Traduire', subtitle: 'Plus de 100 langues · détection auto',
    auto: 'Détection auto', placeholder: 'Saisissez le texte à traduire…', translateBtn: 'Traduire', translating: 'Traduction…',
    detected: 'Détecté', resultPlaceholder: 'La traduction s’affichera ici', swap: 'Inverser', copyResult: 'Copier',
    play: 'Lire', failed: 'Échec de la traduction', autoSwitchedTo: 'Langue cible changée en '
  },
  vip: {
    title: 'Abonnement VIP', subtitle: 'Débloquez les cours de dialectes et plus de langues',
    never: 'Pas encore abonné', active: 'Abonnement actif', expireAt: 'Expire le',
    planName: 'VIP mensuel', perks: 'Avantages',
    perk1: 'Cours de dialectes : cantonais / sichuanais / pékinois / shanghaïen',
    perk2: 'Accès anticipé aux nouvelles langues', perk3: 'Badge VIP exclusif',
    payTitle: 'Choisissez un moyen de paiement', payNow: 'S’abonner · 0,99 €/mois', unavailable: 'Bientôt disponible',
    mockNote: 'Paiement simulé pour les tests',
    paySuccess: 'Paiement reçu, VIP activé !', redirectTip: 'Ouvrez le lien dans un navigateur pour terminer le paiement',
    orders: 'Mes commandes', statusPending: 'En attente', statusPaid: 'Payé', statusFailed: 'Fermée', statusRefunded: 'Remboursée',
    noOrders: 'Aucune commande'
  },
  courses: { all: 'Tous', vipOnly: 'Réservé VIP', locked: 'Langue réservée aux VIP. Abonnez-vous pour débloquer.', goVip: 'Débloquer' },
  profile: {
    title: 'Compte', subtitle: 'Profil et préférences', nickname: 'Pseudo', nicknamePh: 'Définissez votre pseudo',
    prefLang: 'Langues étudiées', uiLang: 'Langue', achievements: '🏆 Trophées'
  }
}

const vi = {
  tab: { home: 'Sảnh game', courses: 'Màn chơi', progress: 'Thành tích', community: 'Cộng đồng', profile: 'Tôi' },
  common: { back: 'Quay lại', save: 'Lưu', confirm: 'Xác nhận', cancel: 'Hủy', copy: 'Sao chép', copied: 'Đã sao chép', logout: 'Đăng xuất' },
  translate: {
    title: 'Dịch', subtitle: 'Hơn 100 ngôn ngữ · tự nhận diện',
    auto: 'Tự nhận diện', placeholder: 'Nhập nội dung cần dịch…', translateBtn: 'Dịch', translating: 'Đang dịch…',
    detected: 'Nhận diện', resultPlaceholder: 'Kết quả dịch sẽ hiện ở đây', swap: 'Đổi', copyResult: 'Sao chép',
    play: 'Đọc', failed: 'Dịch thất bại', autoSwitchedTo: 'Đã chuyển ngôn ngữ đích sang '
  },
  vip: {
    title: 'Hội viên VIP', subtitle: 'Mở khóa khóa học phương ngữ và nhiều ngôn ngữ',
    never: 'Chưa đăng ký', active: 'Đang hiệu lực', expireAt: 'Hết hạn ngày',
    planName: 'VIP hàng tháng', perks: 'Đặc quyền',
    perk1: 'Khóa học phương ngữ: Quảng Đông / Tứ Xuyên / Bắc Kinh / Thượng Hải',
    perk2: 'Trải nghiệm sớm ngôn ngữ mới', perk3: 'Huy hiệu VIP riêng',
    payTitle: 'Chọn phương thức thanh toán', payNow: 'Đăng ký · 0,99 $/tháng', unavailable: 'Chưa hỗ trợ',
    mockNote: 'Thanh toán mô phỏng để kiểm thử',
    paySuccess: 'Thanh toán thành công, VIP đã kích hoạt!', redirectTip: 'Mở liên kết trong trình duyệt để hoàn tất thanh toán',
    orders: 'Đơn hàng của tôi', statusPending: 'Chờ thanh toán', statusPaid: 'Đã thanh toán', statusFailed: 'Đã đóng', statusRefunded: 'Đã hoàn tiền',
    noOrders: 'Chưa có đơn hàng'
  },
  courses: { all: 'Tất cả', vipOnly: 'Chỉ VIP', locked: 'Ngôn ngữ chỉ dành cho VIP. Đăng ký để mở khóa.', goVip: 'Mở khóa' },
  profile: {
    title: 'Tài khoản', subtitle: 'Hồ sơ và tùy chọn', nickname: 'Biệt danh', nicknamePh: 'Đặt biệt danh của bạn',
    prefLang: 'Ngôn ngữ học', uiLang: 'Ngôn ngữ', achievements: '🏆 Danh hiệu'
  }
}

const th = {
  tab: { home: 'โถงเกม', courses: 'ด่าน', progress: 'สถิติ', community: 'ชุมชน', profile: 'ฉัน' },
  common: { back: 'กลับ', save: 'บันทึก', confirm: 'ตกลง', cancel: 'ยกเลิก', copy: 'คัดลอก', copied: 'คัดลอกแล้ว', logout: 'ออกจากระบบ' },
  translate: {
    title: 'แปลภาษา', subtitle: 'รองรับกว่า 100 ภาษา · ตรวจจับอัตโนมัติ',
    auto: 'ตรวจจับอัตโนมัติ', placeholder: 'พิมพ์ข้อความที่ต้องการแปล…', translateBtn: 'แปล', translating: 'กำลังแปล…',
    detected: 'ตรวจพบ', resultPlaceholder: 'ผลการแปลจะแสดงที่นี่', swap: 'สลับ', copyResult: 'คัดลอก',
    play: 'อ่าน', failed: 'แปลไม่สำเร็จ', autoSwitchedTo: 'เปลี่ยนภาษาปลายทางเป็น '
  },
  vip: {
    title: 'สมาชิก VIP', subtitle: 'ปลดล็อกคอร์สภาษาถิ่นและภาษาใหม่',
    never: 'ยังไม่ได้สมัคร', active: 'สมาชิกใช้งานได้', expireAt: 'หมดอายุ',
    planName: 'VIP รายเดือน', perks: 'สิทธิพิเศษ',
    perk1: 'คอร์สภาษาถิ่น: กวางตุ้ง / เสฉวน / ปักกิ่ง / เซี่ยงไฮ้',
    perk2: 'ลองใช้ภาษาใหม่ก่อนใคร', perk3: 'ป้าย VIP เฉพาะบุคคล',
    payTitle: 'เลือกวิธีชำระเงิน', payNow: 'สมัคร · $0.99/เดือน', unavailable: 'ยังไม่เปิดใช้',
    mockNote: 'จำลองการชำระเงินสำหรับทดสอบ',
    paySuccess: 'ชำระเงินสำเร็จ เปิดใช้ VIP แล้ว!', redirectTip: 'เปิดลิงก์ในเบราว์เซอร์เพื่อชำระเงิน',
    orders: 'คำสั่งซื้อของฉัน', statusPending: 'รอชำระ', statusPaid: 'ชำระแล้ว', statusFailed: 'ปิดแล้ว', statusRefunded: 'คืนเงินแล้ว',
    noOrders: 'ยังไม่มีคำสั่งซื้อ'
  },
  courses: { all: 'ทั้งหมด', vipOnly: 'เฉพาะ VIP', locked: 'ภาษานี้สำหรับ VIP สมัครสมาชิกเพื่อปลดล็อก', goVip: 'ปลดล็อก' },
  profile: {
    title: 'บัญชี', subtitle: 'โปรไฟล์และการตั้งค่า', nickname: 'ชื่อเล่น', nicknamePh: 'ตั้งชื่อเล่น',
    prefLang: 'ภาษาที่เรียน', uiLang: 'ภาษาของแอป', achievements: '🏆 ความสำเร็จ'
  }
}

const LOCALES = { 'zh-CN': zhCN, 'zh-TW': zhTW, en, ja, ko, es, fr, vi, th }

const LANG_KEY = 'lingoplay-lang'

const SUPPORTED = [
  { code: 'zh-CN', name: '简体中文', flag: '🇨🇳' },
  { code: 'zh-TW', name: '繁體中文', flag: '🇹🇼' },
  { code: 'en', name: 'English', flag: '🇺🇸' },
  { code: 'ja', name: '日本語', flag: '🇯🇵' },
  { code: 'ko', name: '한국어', flag: '🇰🇷' },
  { code: 'es', name: 'Español', flag: '🇪🇸' },
  { code: 'fr', name: 'Français', flag: '🇫🇷' },
  { code: 'vi', name: 'Tiếng Việt', flag: '🇻🇳' },
  { code: 'th', name: 'ไทย', flag: '🇹🇭' }
]

function currentLang() {
  try {
    const saved = wx.getStorageSync(LANG_KEY)
    if (saved && LOCALES[saved]) return saved
  } catch (e) { /* ignore */ }
  return 'zh-CN'
}

/** 简易取词：t('vip.title')；找不到 key 时回退中文 */
function t(path) {
  const lang = LOCALES[currentLang()]
  const zh = zhCN
  const get = (obj) => path.split('.').reduce((o, k) => (o == null ? undefined : o[k]), obj)
  const v = get(lang) !== undefined ? get(lang) : get(zh)
  return v === undefined ? path : v
}

function setLang(code) {
  if (!LOCALES[code]) return
  try {
    wx.setStorageSync(LANG_KEY, code)
  } catch (e) { /* ignore */ }
}

module.exports = { t, currentLang, setLang, SUPPORTED }
