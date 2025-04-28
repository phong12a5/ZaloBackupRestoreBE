import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tsconfigPaths from 'vite-tsconfig-paths' // Import the plugin
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    tsconfigPaths() // Add the plugin here
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // Target the API Gateway
        changeOrigin: true,
        // No rewrite needed if gateway handles /api prefix
      },
      '/auth': {
        target: 'http://localhost:8080', // Target the API Gateway
        changeOrigin: true,
        // No rewrite needed if gateway handles /auth prefix
      }
    }
  }
})
