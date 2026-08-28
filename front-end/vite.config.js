import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// base './' hace que los assets se pidan con rutas relativas. Sin esto, al
// servir la app detras del stage del API Gateway (/desarrollo/) el navegador
// buscaria /assets/... en la raiz y recibiria 404.
export default defineConfig({
  plugins: [react()],
  base: './'
})
