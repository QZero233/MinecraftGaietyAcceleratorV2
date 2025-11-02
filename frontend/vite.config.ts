import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // proxy API requests to backend running on same host: adjust if necessary
      // '/server': 'http://localhost:8080',
      // '/chest_info': 'http://localhost:8080',
      // '/stat': 'http://localhost:8080'
    }
  }
})

