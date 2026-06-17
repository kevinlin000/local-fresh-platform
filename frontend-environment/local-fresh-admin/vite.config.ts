import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [
      vue(),
      Components({
        resolvers: [ElementPlusResolver({ importStyle: 'css' })]
      })
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    build: {
      rolldownOptions: {
        onLog(level, log, defaultHandler) {
          if (
            level === 'warn' &&
            log.code === 'INVALID_ANNOTATION' &&
            typeof log.id === 'string' &&
            log.id.includes('node_modules/@vueuse/core')
          ) {
            return
          }
          defaultHandler(level, log)
        },
        output: {
          codeSplitting: {
            groups: [
              { name: 'ep-table', test: 'node_modules/element-plus/es/components/table' },
              { name: 'ep-form', test: 'node_modules/element-plus/es/components/form' },
              { name: 'ep-date-picker', test: 'node_modules/element-plus/es/components/date-picker' },
              { name: 'ep-select', test: 'node_modules/element-plus/es/components/select' },
              { name: 'ep-dialog', test: 'node_modules/element-plus/es/components/dialog' },
              { name: 'ep-overlay', test: 'node_modules/element-plus/es/components/overlay' },
              { name: 'element-plus', test: 'node_modules/element-plus' },
              { name: 'element-icons', test: 'node_modules/@element-plus' },
              { name: 'vueuse-vendor', test: 'node_modules/@vueuse' },
              { name: 'popper-vendor', test: 'node_modules/@popperjs' },
              { name: 'dayjs-vendor', test: 'node_modules/dayjs' },
              { name: 'validation-vendor', test: 'node_modules/async-validator' },
              { name: 'echarts', test: 'node_modules/echarts' },
              { name: 'vue-vendor', test: 'node_modules/vue' },
              { name: 'http-vendor', test: 'node_modules/axios' }
            ]
          }
        }
      }
    },
    server: {
      port: 5174,
      proxy: {
        '/api': {
          target: env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
          rewrite: path => path.replace(/^\/api/, '/admin')
        }
      }
    }
  }
})
