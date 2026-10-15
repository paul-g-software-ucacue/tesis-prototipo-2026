# SPA de React servida por Nginx sin privilegios (B00-H06, D-52, ADR-019). El contexto es la raíz
# del repositorio, como el de Java (ADR-015), porque generar:api lee ../contratos/ desde frontend/.
# Ejemplo: docker build -f deploy/docker/frontend.Dockerfile -t mdd-frontend .

# Node 24 LTS, la misma versión de la CI (D-53).
FROM node:24-alpine AS construccion
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-fund --no-audit
COPY frontend/ ./
COPY contratos/ /app/contratos/
# prebuild genera los tipos desde los contratos; tsc los revisa antes de construir.
RUN npm run build

FROM nginxinc/nginx-unprivileged:stable-alpine
# El API Gateway del perfil consolidado; se cambia con la variable (contracts/perfil.md).
ENV MDD_URI_GATEWAY=http://gateway:8080
# El DNS del contenedor queda en NGINX_LOCAL_RESOLVERS, para resolver el API Gateway en cada petición.
ENV NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1
COPY --from=construccion /app/frontend/dist /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/templates/default.conf.template
EXPOSE 8080
