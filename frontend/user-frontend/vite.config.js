import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

const root = fileURLToPath(new URL('.', import.meta.url))

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, root)
  const backend = env.VITE_PROXY_TARGET || 'http://localhost:18088'

  return {
    root,
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    build: {
      outDir: 'dist',
      emptyOutDir: true
    },
    server: {
      // 前端开发端口；被占用时直接报错，不自动换端口
      port: 25173,
      strictPort: true,
      proxy: {
        '/api': { target: backend, changeOrigin: true },
        '/files': { target: backend, changeOrigin: true }
      }
    }
  }
})
