# Caddy del equipo de demostración (D-80). Es el 2.11.7 oficial con el binario compilado de nuevo y los
# paquetes de Alpine al día: la imagen oficial trae vulnerabilidades ya corregidas, y solo se arreglan
# al reconstruirla. El contexto es la raíz del repositorio, como el de las demás imágenes.
# Ejemplo: docker build -f deploy/docker/caddy.Dockerfile -t mdd-caddy .

FROM golang:1.26.9-alpine@sha256:3082400e369fa24d5fc60bca20edab3f6d604e0c5a690ec66b295eff4dd87ade AS compilacion
ENV GOTOOLCHAIN=local CGO_ENABLED=0
WORKDIR /src
COPY deploy/docker/caddy/ ./
RUN go build -trimpath -ldflags "-w -s" -o /caddy .

FROM caddy:2.11.8-alpine@sha256:f6228111e5fafa1a5238258d0858e103f10599b8d1659a146e7726681a002bb9
RUN apk upgrade --no-cache
COPY --from=compilacion /caddy /usr/bin/caddy
