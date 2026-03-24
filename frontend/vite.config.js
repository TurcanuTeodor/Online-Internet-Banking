import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'

const proxyTarget = process.env.VITE_PROXY_TARGET || 'https://localhost:8443'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api':{
        target: proxyTarget,
        changeOrigin: true,
        secure: false
      }
    }
  }
})
