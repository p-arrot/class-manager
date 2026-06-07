import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  build: {
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return undefined
          if (id.includes('monaco-editor')) {
            if (id.includes('/vs/base/')) return 'vendor-monaco-base'
            if (id.includes('/vs/platform/')) return 'vendor-monaco-platform'
            if (id.includes('/vs/editor/contrib/suggest/')) return 'vendor-monaco-suggest'
            if (id.includes('/vs/editor/contrib/hover/')) return 'vendor-monaco-hover'
            if (id.includes('/vs/editor/contrib/find/')) return 'vendor-monaco-find'
            if (id.includes('/vs/editor/contrib/')) return 'vendor-monaco-contrib'
            if (id.includes('/vs/editor/browser/widget/')) return 'vendor-monaco-widget'
            if (id.includes('/vs/editor/browser/view/')) return 'vendor-monaco-view'
            if (id.includes('/vs/editor/browser/controller/')) return 'vendor-monaco-controller'
            if (id.includes('/vs/editor/browser/services/')) return 'vendor-monaco-services'
            if (id.includes('/vs/editor/browser/config/')) return 'vendor-monaco-config'
            if (id.includes('/vs/editor/browser/')) return 'vendor-monaco-browser'
            if (id.includes('/vs/editor/common/')) return 'vendor-monaco-common'
            if (id.includes('/vs/editor/standalone/')) return 'vendor-monaco-standalone'
            if (id.includes('/vs/editor/')) return 'vendor-monaco-editor'
            if (id.includes('/vs/basic-languages/') || id.includes('/vs/language/')) return 'vendor-monaco-language'
            return 'vendor-monaco'
          }
          if (id.includes('echarts')) return 'vendor-echarts'
          if (id.includes('naive-ui') || id.includes('@css-render') || id.includes('vooks') || id.includes('vueuc')) {
            return 'vendor-naive'
          }
          if (id.includes('@vue') || id.includes('vue-router') || id.includes('pinia')) return 'vendor-vue'
          return 'vendor'
        },
      },
    },
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
