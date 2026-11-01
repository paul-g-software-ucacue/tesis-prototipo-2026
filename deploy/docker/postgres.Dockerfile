# PostgreSQL del equipo de demostración (D-80). Es la imagen oficial con los paquetes de Alpine al día:
# trae vulnerabilidades ya corregidas, y solo se arreglan al reconstruirla. El volumen de datos no
# cambia, porque la versión de PostgreSQL es la misma.
# Ejemplo: docker build -f deploy/docker/postgres.Dockerfile -t mdd-postgres .

FROM postgres:18.6-alpine@sha256:77f585114c32fbca283dc835b0596f4e52b51b4c6662d7810b2f4084f60a1873
RUN apk upgrade --no-cache
