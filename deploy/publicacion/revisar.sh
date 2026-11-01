#!/usr/bin/env bash
# Revisa con Grype las imágenes de una versión antes de publicarla (B00-H08, escenario 2, FR-002 y
# FR-003, D-54; specs/009-publicacion-despliegue/contracts/publicacion.md). Uso:
#   deploy/publicacion/revisar.sh <commit>
# Revisa las 8 imágenes mdd/<componente>:<commit12> que dejó construir.sh y las de terceros que usa el
# equipo de demostración (las image: con digest de deploy/equipo/compose.demo.yml: hoy, la del perfil
# tunel), con las mismas excepciones y el mismo resumen de 4 · sca (B00-H07). Escribe el
# resumen en Markdown en la salida estándar y termina con 1 si alguna imagen tiene una vulnerabilidad
# alta o crítica con versión corregida sin excepción vigente, o si Grype falla.
# GRYPE: ruta del binario (por defecto, grype en el PATH). Necesita yq 4 y jq.
set -euo pipefail
cd "$(dirname "$0")/../.."

readonly COMPONENTES=(identidad datos distributivo salida gateway frontend caddy postgres)
readonly GRYPE=${GRYPE:-grype}

commit=${1:?Uso: revisar.sh <commit>}
corto=${commit:0:12}

temporal=$(mktemp -d)
trap 'rm -rf "$temporal"' EXIT
mkdir -p "$temporal/json"

deploy/sca/excepciones.sh .grype.yaml "$temporal/grype.yaml" >&2

estado=0
revisar() {  # revisar <nombre del informe> <fuente de Grype>
    echo "::group::$1 ($2)" >&2
    "$GRYPE" "$2" -c "$temporal/grype.yaml" --show-suppressed \
        -o table -o "json=$temporal/json/$1.json" >&2 || estado=1
    echo "::endgroup::" >&2
}

for componente in "${COMPONENTES[@]}"; do
    revisar "$componente" "docker:mdd/$componente:$corto"
done

# Las de terceros, por su digest y directamente del registro: no hace falta bajarlas a Docker.
while IFS= read -r imagen; do
    nombre=${imagen%%[:@]*}
    revisar "${nombre##*/}" "registry:$imagen"
done < <(yq '.services[].image | select(. == "*@sha256:*")' deploy/equipo/compose.demo.yml)

RESUMEN_ASUNTO=imágenes deploy/sca/resumen.sh "$temporal/grype.yaml" "$temporal"/json/*.json || estado=1
exit "$estado"
