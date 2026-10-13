# SPA de React servida por Nginx sin privilegios.
FROM node:26-alpine AS construccion
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run generar:api && npm run build

FROM nginxinc/nginx-unprivileged:stable-alpine
COPY --from=construccion /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
