<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
// Element Plus 内置语言包跟随界面语言，保证日期/分页等组件文案同步
import elZhCn from 'element-plus/es/locale/lang/zh-cn'
import elZhTw from 'element-plus/es/locale/lang/zh-tw'
import elEn from 'element-plus/es/locale/lang/en'
import elJa from 'element-plus/es/locale/lang/ja'
import elKo from 'element-plus/es/locale/lang/ko'
import elEs from 'element-plus/es/locale/lang/es'
import elFr from 'element-plus/es/locale/lang/fr'
import elVi from 'element-plus/es/locale/lang/vi'
import elTh from 'element-plus/es/locale/lang/th'

const { locale } = useI18n()

const EL_LOCALES = {
  'zh-CN': elZhCn,
  'zh-TW': elZhTw,
  en: elEn,
  ja: elJa,
  ko: elKo,
  es: elEs,
  fr: elFr,
  vi: elVi,
  th: elTh
}

const elLocale = computed(() => EL_LOCALES[locale.value] || elZhCn)
</script>

<template>
  <el-config-provider :locale="elLocale">
    <router-view />
  </el-config-provider>
</template>
