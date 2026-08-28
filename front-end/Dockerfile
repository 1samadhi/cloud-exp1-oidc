# --- Etapa 1: build ---
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
# Vite incrusta las VITE_* en el bundle en tiempo de build, no de ejecucion:
# hay que pasarlas como build args para que lleguen al artefacto.
ARG VITE_ENTRA_CLIENT_ID=""
ARG VITE_ENTRA_TENANT_ID=""
ARG VITE_REDIRECT_URI=""
ARG VITE_ENTRA_SCOPE_API=""
ARG VITE_API_PRODUCTOS=""
ARG VITE_API_PEDIDOS=""
ARG VITE_API_AUTH=""
RUN npm run build

# --- Etapa 2: servidor estatico ---
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
