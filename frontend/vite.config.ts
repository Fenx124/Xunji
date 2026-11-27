import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    // 同时接受 localhost 和 127.0.0.1。只绑 IPv6 的 localhost 时，用 127.0.0.1 打开会连接被拒绝。
    host: true,
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        // 必须在创建代理请求前覆盖 Origin。proxyReq 事件触发时请求头往往已经发出，removeHeader 不会生效。
        headers: {
          origin: 'http://localhost:5173',
        },
      },
    },
  },
})