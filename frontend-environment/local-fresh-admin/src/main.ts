import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'normalize.css'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import '@/styles/vue3-admin.scss'
import App from '@/App.vue'
import router from '@/router'
import '@/permission'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.mount('#app')
