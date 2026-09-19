import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as Icons from '@element-plus/icons-vue'
import '@fontsource/zcool-kuaile' // 站酷快乐体（卡通标题字体）

import App from './App.vue'
import router from './router'
import './styles/main.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 注册全部图标（原型阶段简化处理）
for (const [name, comp] of Object.entries(Icons)) {
  app.component(name, comp)
}

app.mount('#app')
