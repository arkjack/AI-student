import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  build: {
    // ⚠️ 不要只依赖 emptyOutDir 来清理产物。
    // 实测在本机环境（Windows + 含中文的项目路径）中，即使把 emptyOutDir 显式设为 true，
    // dist 也不会被清空（Vite 内部会走到 emptyDir，但前置的 fs.existsSync(outDir) 判定失败），
    // 结果是旧版本的按需分包一直累积 —— 曾累积到 1300 个文件、23 份 Login-*.js，
    // 造成 ①部署包逐次变大 ②已下线代码仍可通过 /assets/<旧哈希>.js 公开访问。
    // 因此真正负责清空目录的是 package.json 里的 prebuild 脚本，这里保留声明仅为表达意图。
    outDir: 'dist',
    emptyOutDir: true
  },
  server: {
    port: 5173,
    open: false,
    host: '127.0.0.1',
    proxy: {
      // 后端 API 代理
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
