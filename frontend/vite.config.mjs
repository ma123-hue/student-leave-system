import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

const demoMode = process.env.VITE_DEMO_MODE === 'true';

export default defineConfig({
  base: demoMode ? './' : '/',
  plugins: [vue()],
  build: {
    outDir: 'dist',
    emptyOutDir: true
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    allowedHosts: ['localhost', '127.0.0.1', '10.0.2.2'],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});
