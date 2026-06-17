import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhTw from 'element-plus/es/locale/lang/zh-tw'
import 'normalize.css'
import 'element-plus/dist/index.css'
import '@/styles/vue3-admin.scss'
import App from '@/App.vue'
import router from '@/router'
import '@/permission'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhTw })
app.mount('#app')
