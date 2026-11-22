# Caddy del equipo de demostración (D-80). Es el 2.11.7 oficial con el binario compilado de nuevo y los
# paquetes de Alpine al día: la imagen oficial trae vulnerabilidades ya corregidas, y solo se arreglan
# al reconstruirla. El contexto es la raíz del repositorio, como el de las demás imágenes.
# Ejemplo: docker build -f deploy/docker/caddy.Dockerfile -t mdd-caddy .

FROM golang:1.27.4-alpine@sha256:70bd7ddea8b6cbe5d6943cde279d6b67feb10cc924fb9100d1d8757c1575b280 AS compilacion
ENV GOTOOLCHAIN=local CGO_ENABLED=0
WORKDIR /src
COPY deploy/docker/caddy/ ./
RUN go build -trimpath -ldflags "-w -s" -o /caddy .

FROM caddy:2.11.9-alpine@sha256:003a97808e781aa9f7e75cc2f43d34d461099031faf5e20faefb9f61126836f0
RUN apk upgrade --no-cache
COPY --from=compilacion /caddy /usr/bin/caddy
