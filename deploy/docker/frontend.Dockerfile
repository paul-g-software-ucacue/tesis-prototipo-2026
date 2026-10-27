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
# Paquetes de la base con vulnerabilidades altas ya corregidas (Grype): zlib 1.3.2-r0 → 1.3.2-r1
# (B00-H08), libexpat 2.8.5-r0 → 2.9.0-r0 (CVE-2026-77214, F05-H02) y brotli-libs 1.2.0-r1 → 1.2.1-r0
# (CVE-2026-111497, crítica) y curl y libcurl 8.22.0-r0 → 8.23.0-r0 (CVE-2026-92392, F01-H04). Se
# actualizan como root y se vuelve al usuario sin privilegios de la imagen (101).
USER root
RUN apk upgrade --no-cache zlib libexpat brotli-libs curl libcurl
USER 101
# El API Gateway del perfil consolidado; se cambia con la variable (contracts/perfil.md).
ENV MDD_URI_GATEWAY=http://gateway:8080
# El DNS del contenedor queda en NGINX_LOCAL_RESOLVERS, para resolver el API Gateway en cada petición.
ENV NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1
COPY --from=construccion /app/frontend/dist /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/templates/default.conf.template
EXPOSE 8080
