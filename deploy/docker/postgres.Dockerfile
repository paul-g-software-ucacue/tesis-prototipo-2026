# PostgreSQL del equipo de demostración (D-80). Es la imagen oficial con los paquetes de Alpine al día:
# trae vulnerabilidades ya corregidas, y solo se arreglan al reconstruirla. El volumen de datos no
# cambia, porque la versión de PostgreSQL es la misma.
# Ejemplo: docker build -f deploy/docker/postgres.Dockerfile -t mdd-postgres .

FROM postgres:18.6-alpine@sha256:10a5c69a83035ab5239f5620d708520e8b28f5aa1f2b20143e6024532edadfb5
RUN apk upgrade --no-cache
