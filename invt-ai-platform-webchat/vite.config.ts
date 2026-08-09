import { defineConfig } from 'vite'
import { resolve } from 'path'

export default defineConfig({
  build: {
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'InvtWebChat',
      formats: ['es', 'umd'],
      fileName: (format) => `invt-ai-platform-webchat.${format}.js`,
    },
    rollupOptions: {
      output: {
        assetFileNames: 'invt-ai-platform-webchat.[ext]',
      },
    },
    cssCodeSplit: false,
    minify: 'esbuild',
  },
})
