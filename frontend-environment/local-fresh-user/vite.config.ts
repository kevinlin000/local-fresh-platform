import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id: string) {
          if (!id.includes('node_modules')) {
            return
          }
          if (id.includes('element-plus')) {
            if (id.includes('theme-chalk')) {
              return 'element-theme'
            }
            return 'element-plus'
          }
          if (id.includes('@element-plus')) {
            return 'element-plus'
          }
          if (id.includes('@vueuse')) {
            return 'vueuse-vendor'
          }
          if (id.includes('@popperjs') || id.includes('dayjs') || id.includes('async-validator')) {
            return 'element-support'
          }
          if (id.includes('vue') || id.includes('pinia')) {
            return 'vue-vendor'
          }
          if (id.includes('axios')) {
            return 'http-vendor'
          }
          return 'vendor'
        }
      }
    }
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (requestPath) => requestPath.replace(/^\/api/, '')
      }
    }
  }
})
