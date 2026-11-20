# PostgreSQL del equipo de demostración (D-80). Es la imagen oficial con los paquetes de Alpine al día:
# trae vulnerabilidades ya corregidas, y solo se arreglan al reconstruirla. El volumen de datos no
# cambia, porque la versión de PostgreSQL es la misma.
# Ejemplo: docker build -f deploy/docker/postgres.Dockerfile -t mdd-postgres .

FROM postgres:18.7-alpine@sha256:ff64c9e3bdf8e2120107d3559de8db314e8305f7e38c5105903c6295876738ff
RUN apk upgrade --no-cache
