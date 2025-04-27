import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  build: {
    rollupOptions: {
      external: ['module-name'], // Replace 'module-name' with the actual module causing the issue
    },
  },
});
