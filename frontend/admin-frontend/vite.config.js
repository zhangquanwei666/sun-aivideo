import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

const root = fileURLToPath(new URL('.', import.meta.url))

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, root)
  return {
    root,
    plugins: [vue()],
    resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
    build: { outDir: 'dist', emptyOutDir: true },
    server: {
      port: 25174,
      strictPort: true,
      proxy: {
        '/api': { target: env.VITE_ADMIN_PROXY_TARGET || 'http://localhost:18089', changeOrigin: true },
        '/files': { target: env.VITE_FILES_PROXY_TARGET || 'http://localhost:18088', changeOrigin: true }
      }
    }
  }
})
