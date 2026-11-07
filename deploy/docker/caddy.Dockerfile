# Caddy del equipo de demostración (D-80). Es el 2.11.7 oficial con el binario compilado de nuevo y los
# paquetes de Alpine al día: la imagen oficial trae vulnerabilidades ya corregidas, y solo se arreglan
# al reconstruirla. El contexto es la raíz del repositorio, como el de las demás imágenes.
# Ejemplo: docker build -f deploy/docker/caddy.Dockerfile -t mdd-caddy .

FROM golang:1.27.2-alpine@sha256:f92b6ef800e499660581efdabdf25d9d817a9d124eaf900924f0504e7e27e12d AS compilacion
ENV GOTOOLCHAIN=local CGO_ENABLED=0
WORKDIR /src
COPY deploy/docker/caddy/ ./
RUN go build -trimpath -ldflags "-w -s" -o /caddy .

FROM caddy:2.11.7-alpine@sha256:d8542f48d34a9cf4e4c11a478865229840e87e4c96ea3f439101f31a5d35f75f
RUN apk upgrade --no-cache
COPY --from=compilacion /caddy /usr/bin/caddy
