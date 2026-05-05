import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'

const app = createApp(App)
const pinia = createPinia()

app.config.errorHandler = (error, instance, info) => {
  console.error('[GlobalErrorHandler]', info, error, instance)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.mount('#app')
