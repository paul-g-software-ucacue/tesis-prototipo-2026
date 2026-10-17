#!/usr/bin/env bash
# Construye las 6 imágenes del MDD y el paquete de despliegue de un commit (B00-H08, D-58;
# specs/009-publicacion-despliegue/contracts/publicacion.md). Uso, desde cualquier directorio:
#   deploy/publicacion/construir.sh <commit>
# Las imágenes salen de compose.yaml con «docker buildx bake», la misma definición del perfil
# consolidado (FR-001), y quedan en el Docker local como mdd/<componente>:<commit12>, con el commit
# completo en el label org.opencontainers.image.revision. Lo usan cd.yml y probar_equipo.sh.
set -euo pipefail
cd "$(dirname "$0")/../.."

readonly COMPONENTES=(identidad datos distributivo salida gateway frontend)

commit=${1:?Uso: construir.sh <commit>}
[[ $commit =~ ^[0-9a-f]{12,40}$ ]] || { echo "El commit debe ser hexadecimal, de 12 a 40 caracteres." >&2; exit 2; }
corto=${commit:0:12}

etiquetas=()
for componente in "${COMPONENTES[@]}"; do
    etiquetas+=(--set "$componente.tags=mdd/$componente:$corto")
done

docker buildx bake -f compose.yaml --load \
    --set "*.labels.org.opencontainers.image.revision=$commit" \
    "${etiquetas[@]}" "${COMPONENTES[@]}"

docker build -f deploy/docker/despliegue.Dockerfile --build-arg "REVISION=$commit" \
    --label "org.opencontainers.image.revision=$commit" -t "mdd/despliegue:$corto" .

for imagen in "${COMPONENTES[@]}" despliegue; do
    echo "mdd/$imagen:$corto"
done
