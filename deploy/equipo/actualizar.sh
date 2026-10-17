#!/usr/bin/env bash
# Consulta el registro y despliega la última versión publicada de main en el equipo de demostración
# (B00-H08, escenarios 3 y 4, D-55, D-56, D-58; specs/009-publicacion-despliegue/contracts/equipo.md).
# Lo corre mdd-actualizar.service cada 5 minutos, como el usuario mdd:
#   RAIZ=/opt/mdd /opt/mdd/bin/actualizar.sh [--forzar]
# 1. Hace HEAD de <REPOSITORIO>:despliegue-main, que no gasta el cupo de Docker Hub (research §6).
# 2. Si el digest no cambió (y no hay --forzar), termina. Un 404 es «sin versión publicada todavía».
# 3. Baja el paquete por su digest, lo extrae en versiones/<commit12>/ y corre su desplegar.sh.
# 4. Informa el despliegue a GitHub Deployments (in_progress, después success o failure), si hay token.
# Si el registro o GitHub no responden, lo registra y sigue: nunca toca lo que está corriendo por eso.
# Códigos: 0 sano, sin cambios o error pasajero del registro; 1 el despliegue falló. Nunca imprime un
# secreto: las credenciales van a curl y a docker por la entrada estándar.
set -euo pipefail

readonly RAIZ="${RAIZ:-/opt/mdd}"
export RAIZ   # desplegar.sh y la prueba de humo la leen
export DOCKER_CONFIG="${DOCKER_CONFIG:-$RAIZ/docker}"
readonly ACEPTA='application/vnd.oci.image.index.v1+json, application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.docker.distribution.manifest.v2+json'
readonly VERSIONES_GUARDADAS=3

forzar=false
[[ ${1:-} == --forzar ]] && forzar=true

valor() {  # valor <archivo> <variable>: lee una variable sin ejecutar el archivo
    local linea
    linea=$(grep -E "^$2=" "$1" 2>/dev/null | tail -1 || true)
    linea=${linea#*=}
    linea=${linea#\'}
    printf '%s' "${linea%\'}"
}

readonly CONFIGURACION="$RAIZ/configuracion.env"
repositorio=$(valor "$RAIZ/.env" REPOSITORIO)
[[ -n $repositorio ]] || { echo "Falta REPOSITORIO en $RAIZ/.env." >&2; exit 1; }
usuario_registro=$(valor "$CONFIGURACION" DOCKERHUB_USUARIO)
token_registro=$(valor "$CONFIGURACION" DOCKERHUB_TOKEN)
autenticacion=$(grep -q '^AUTENTICACION=' "$CONFIGURACION" && valor "$CONFIGURACION" AUTENTICACION || echo auth.docker.io)
github_api=$(valor "$CONFIGURACION" GITHUB_API)
github_api=${github_api:-https://api.github.com}
github_repositorio=$(valor "$CONFIGURACION" GITHUB_REPOSITORIO)
github_token=$(valor "$CONFIGURACION" GITHUB_TOKEN)
github_entorno=$(valor "$CONFIGURACION" GITHUB_ENTORNO)
github_entorno=${github_entorno:-demostracion}

# REPOSITORIO completo → registro y ruta (docker.io/p980/mdd → registry-1.docker.io y p980/mdd).
registro=${repositorio%%/*}
ruta=${repositorio#*/}
[[ $registro == docker.io ]] && registro=registry-1.docker.io
esquema=https
[[ $registro == localhost:* || $registro == 127.0.0.1:* ]] && esquema=http

# --- Registro ----------------------------------------------------------------------------------

token_lectura=""
if [[ -n $autenticacion ]]; then
    token_lectura=$(printf 'user = "%s:%s"\n' "$usuario_registro" "$token_registro" \
        | curl -fsS --max-time 20 -K - \
            "https://$autenticacion/token?service=registry.docker.io&scope=repository:$ruta:pull" \
        | jq -r '.token // empty' 2>/dev/null || true)
    if [[ -z $token_lectura ]]; then
        echo "No se pudo obtener un token de lectura de $autenticacion; reintento en la próxima consulta."
        exit 0
    fi
fi

cabeceras=$(mktemp)
trap 'rm -f "$cabeceras"' EXIT
codigo=$( { [[ -z $token_lectura ]] || printf 'header = "Authorization: Bearer %s"\n' "$token_lectura"; } \
    | curl -s --max-time 20 -K - -I -o /dev/null -D "$cabeceras" -w '%{http_code}' -H "Accept: $ACEPTA" \
        "$esquema://$registro/v2/$ruta/manifests/despliegue-main" || echo 000)
if [[ $codigo == 404 ]]; then
    echo "Sin versión publicada todavía en $repositorio:despliegue-main."
    exit 0
elif [[ $codigo != 200 ]]; then
    echo "El registro respondió $codigo; sigo con la versión actual y reintento en la próxima consulta."
    exit 0
fi
digest=$(tr -d '\r' <"$cabeceras" | awk -F': ' 'tolower($1) == "docker-content-digest" {print $2}')
mkdir -p "$RAIZ/estado" "$RAIZ/versiones"
if [[ $forzar == false && $digest == "$(cat "$RAIZ/estado/ultimo-digest" 2>/dev/null || true)" ]]; then
    echo "Sin cambios ($digest)."
    exit 0
fi

# --- Paquete -----------------------------------------------------------------------------------

if [[ -n $autenticacion ]]; then
    printf '%s' "$token_registro" | docker login -u "$usuario_registro" --password-stdin "${repositorio%%/*}" >/dev/null 2>&1 \
        || { echo "docker login falló con el token de solo lectura; reintento en la próxima consulta."; exit 0; }
fi
if ! docker pull --quiet "$repositorio@$digest" >/dev/null; then
    echo "No se pudo bajar el paquete $digest; reintento en la próxima consulta."
    exit 0
fi
extraido=$(mktemp -d "$RAIZ/versiones/.paquete.XXXXXX")
contenedor=$(docker create "$repositorio@$digest")
docker cp -q "$contenedor:/paquete/." "$extraido"
docker rm "$contenedor" >/dev/null
version=$(tr -d '[:space:]' <"$extraido/VERSION")
carpeta="$RAIZ/versiones/${version:0:12}"
if [[ $(readlink -f "$RAIZ/actual" 2>/dev/null) == "$(readlink -f "$carpeta" 2>/dev/null)" && -d $carpeta ]]; then
    rm -rf "$extraido"   # la versión que corre: se redespliega con sus archivos
else
    rm -rf "$carpeta"
    mv "$extraido" "$carpeta"
fi

# --- Informe a GitHub (D-56) -------------------------------------------------------------------

id_despliegue=""
github() {  # github <ruta> <json>: POST con el token por la entrada estándar; imprime la respuesta
    printf 'header = "Authorization: Bearer %s"\n' "$github_token" \
        | curl -fsS --max-time 20 -K - -X POST -H 'Accept: application/vnd.github+json' \
            -H 'X-GitHub-Api-Version: 2022-11-28' --data "$2" "$github_api$1"
}
informar() {  # informar <estado> <descripción>
    [[ -n $github_token && -n $github_repositorio ]] || return 0
    if [[ -z $id_despliegue ]]; then
        id_despliegue=$(github "/repos/$github_repositorio/deployments" "$(jq -cn \
            --arg ref "$version" --arg entorno "$github_entorno" \
            '{ref: $ref, environment: $entorno, auto_merge: false, required_contexts: [],
              description: "Despliegue en el equipo de demostración (D-55)"}')" \
            | jq -r '.id // empty' 2>/dev/null || true)
        [[ -n $id_despliegue ]] || { echo "GitHub no respondió al crear el deployment; sigo sin informe."; return 0; }
    fi
    github "/repos/$github_repositorio/deployments/$id_despliegue/statuses" "$(jq -cn \
        --arg estado "$1" --arg descripcion "${2:0:140}" \
        '{state: $estado, description: $descripcion, auto_inactive: true}')" >/dev/null \
        || echo "GitHub no respondió al informar $1; sigo."
}

# --- Despliegue --------------------------------------------------------------------------------

echo "Versión nueva: ${version:0:12} ($digest)."
informar in_progress "Desplegando ${version:0:12}"
resultado=0
resumen=$("$carpeta/desplegar.sh" "$carpeta" | tail -1) || resultado=$?
echo "$resumen"
echo "$digest" >"$RAIZ/estado/ultimo-digest"
if ((resultado == 0)); then
    informar success "$resumen"
else
    informar failure "$resumen"
fi

# Se guardan las últimas versiones, sin tocar las que apuntan «actual» y «anterior».
actual=$(readlink -f "$RAIZ/actual" 2>/dev/null || true)
anterior=$(readlink -f "$RAIZ/anterior" 2>/dev/null || true)
while IFS= read -r vieja; do
    [[ $vieja == "$actual" || $vieja == "$anterior" ]] || rm -rf "$vieja"
done < <(find "$RAIZ/versiones" -mindepth 1 -maxdepth 1 -type d ! -name '.*' -printf '%T@ %p\n' \
    | sort -rn | tail -n +$((VERSIONES_GUARDADAS + 1)) | cut -d' ' -f2-)

((resultado == 0)) || exit 1
