import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // App 安装包由后端静态目录提供（backend/src/main/resources/static/downloads）
      '/downloads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})