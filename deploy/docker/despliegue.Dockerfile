# Paquete de despliegue de un commit (B00-H08, D-58; specs/009-publicacion-despliegue/contracts/publicacion.md).
# Lleva los archivos que necesita el equipo de demostración para desplegar las imágenes de ese mismo
# commit, y la vuelta atrás vuelve a ellos. Sin paquetes del sistema: no hay nada que revisar con Grype.
# El equipo lo extrae con «docker create» y «docker cp <id>:/paquete»; nunca se ejecuta.
# Ejemplo: docker build -f deploy/docker/despliegue.Dockerfile --build-arg REVISION=$(git rev-parse HEAD) .

FROM busybox:1.38@sha256:fd7dc98638c8e305f4dc34e979f1c0fdfdcaeb0fbf8fcff77ae834b6da3d7e6e AS version
ARG REVISION
RUN test -n "${REVISION}" && printf '%s\n' "${REVISION}" > /VERSION

FROM scratch
LABEL org.opencontainers.image.description="Paquete de despliegue del MDD (D-58)"
COPY deploy/equipo/compose.demo.yml deploy/Caddyfile deploy/postgres/01-esquemas.sh \
     deploy/desplegar.sh deploy/equipo/probar_humo.sh /paquete/
COPY --from=version /VERSION /paquete/VERSION
# docker create necesita un comando; el contenedor nunca se arranca.
CMD ["/nada"]
