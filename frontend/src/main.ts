import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import { setupGuards } from './router/guards'
import App from './App.vue'

const app = createApp(App)

app.use(createPinia())
app.use(router)

setupGuards(router)

app.mount('#app')
