// @ts-ignore
import { createApp } from 'vue'
// @ts-ignore
import App from './App.vue'
import router from './router'
import store from './store'
// @ts-ignore
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

const app = createApp(App)
app.use(router)
app.use(store)
app.use(ElementPlus)
app.mount('#app')
