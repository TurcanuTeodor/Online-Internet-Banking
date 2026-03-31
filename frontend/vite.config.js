import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react-swc'
import http from 'node:http'
import path from 'node:path'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://localhost:8443'

  return {
    plugins: [react()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: {
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
          secure: false,
          agent: new http.Agent({ keepAlive: false }),
          configure: (proxy) => {
            proxy.on('error', (err, _req, res) => {
              if (!res.headersSent) {
                res.writeHead(502, { 'Content-Type': 'application/json' });
              }
              res.end(JSON.stringify({ message: 'Proxy error: ' + err.message }));
            });
          },
        },
      },
    },
  }
})
