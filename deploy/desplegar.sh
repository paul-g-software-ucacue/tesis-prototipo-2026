#!/usr/bin/env bash
# Despliega una versión en el equipo de demostración, con vuelta atrás (B00-H08, escenarios 3 y 4,
# D-55, D-58; specs/009-publicacion-despliegue/contracts/equipo.md). Viaja dentro del paquete de
# despliegue de cada commit y lo llama actualizar.sh:
#   RAIZ=/opt/mdd desplegar.sh <carpeta de la versión nueva>
# Mueve los enlaces «actual» y «anterior», escribe ETIQUETA en $RAIZ/.env, baja las imágenes y levanta
# la versión con Compose (con la ruta real de su carpeta, para que los montajes cambien con ella),
# espera hasta ESPERA_SALUD segundos (180) a que todo quede sano y corre su prueba de humo. Si algo
# falla, vuelve a la versión anterior: imágenes y archivos.
# La última línea de la salida estándar es el resumen para el informe a GitHub (D-56); el detalle va
# a la salida de errores. Códigos: 0 sano; 1 no quedó sano (con vuelta atrás, o sin versión anterior);
# 2 la versión anterior tampoco quedó sana.
set -euo pipefail

readonly RAIZ="${RAIZ:-/opt/mdd}"
export RAIZ   # la prueba de humo la lee
readonly ESPERA="${ESPERA_SALUD:-180}"
export DOCKER_CONFIG="${DOCKER_CONFIG:-$RAIZ/docker}"

nueva=$(readlink -f "${1:?Uso: desplegar.sh <carpeta de la versión nueva>}")
[[ -f $nueva/compose.demo.yml && -f $nueva/VERSION ]] || { echo "$nueva no es un paquete de despliegue." >&2; exit 2; }

# Lee una variable del .env sin ejecutarlo.
valor() {
    local linea
    linea=$(grep -E "^$1=" "$RAIZ/.env" | tail -1 || true)
    linea=${linea#*=}
    linea=${linea#\'}
    printf '%s' "${linea%\'}"
}

proyecto=$(valor MDD_PROYECTO)
proyecto=${proyecto:-mdd}
perfiles=()
[[ -n $(valor TUNEL_TOKEN) ]] && perfiles=(--profile tunel)

compose_de() {  # compose_de <carpeta de una versión> <argumentos…>
    local carpeta=$1
    shift
    docker compose -p "$proyecto" --env-file "$RAIZ/.env" -f "$carpeta/compose.demo.yml" "${perfiles[@]}" "$@"
}

etiqueta_de() { head -c 12 "$1/VERSION"; }

poner_etiqueta() {  # poner_etiqueta <etiqueta>
    if grep -q '^ETIQUETA=' "$RAIZ/.env"; then
        sed -i "s/^ETIQUETA=.*/ETIQUETA=$1/" "$RAIZ/.env"
    else
        echo "ETIQUETA=$1" >>"$RAIZ/.env"
    fi
}

enlazar() {  # enlazar <nombre> <destino>: cambio atómico del enlace
    ln -sfn "$2" "$RAIZ/.$1.nuevo"
    mv -T "$RAIZ/.$1.nuevo" "$RAIZ/$1"
}

# Deja en $no_sanos los servicios que no están corriendo o no están sanos.
todos_sanos() {  # todos_sanos <carpeta>
    local fin=$((SECONDS + ESPERA)) estados
    while ((SECONDS < fin)); do
        estados=$(compose_de "$1" ps -a --format '{{.Service}} {{.State}} {{.Health}}' 2>/dev/null || true)
        no_sanos=$(awk '$2 != "running" || ($3 != "" && $3 != "healthy") {print $1}' <<<"$estados" | sort -u | paste -sd, -)
        [[ -n $estados && -z $no_sanos ]] && return 0
        sleep 5
    done
    compose_de "$1" ps -a >&2 || true
    return 1
}

levantar() {  # levantar <carpeta>
    poner_etiqueta "$(etiqueta_de "$1")"
    compose_de "$1" pull --quiet >&2
    compose_de "$1" up -d --remove-orphans >&2
}

anterior=$(readlink -f "$RAIZ/actual" 2>/dev/null || true)
[[ $anterior == "$nueva" ]] && anterior=$(readlink -f "$RAIZ/anterior" 2>/dev/null || true)
[[ -n $anterior && ! -f $anterior/compose.demo.yml ]] && anterior=""

echo "Desplegando $(etiqueta_de "$nueva") (anterior: ${anterior:+$(etiqueta_de "$anterior")})" >&2
[[ -n $anterior ]] && enlazar anterior "$anterior"
enlazar actual "$nueva"
inicio=$SECONDS
problema=""
if ! levantar "$nueva"; then
    problema="Docker no pudo levantar la versión"
elif ! todos_sanos "$nueva"; then
    problema="$no_sanos no quedó sano en ${ESPERA} s"
elif ! "$nueva/probar_humo.sh" >&2; then
    problema="la prueba de humo falló"
fi

if [[ -z $problema ]]; then
    echo "Sano en $((SECONDS - inicio)) s"
    docker image prune -f >/dev/null 2>&1 || true
    exit 0
fi

echo "La versión $(etiqueta_de "$nueva") falló: $problema." >&2
if [[ -z $anterior ]]; then
    echo "Sin versión anterior: $problema"
    exit 1
fi

echo "Vuelvo a $(etiqueta_de "$anterior")." >&2
enlazar actual "$anterior"
if levantar "$anterior" && todos_sanos "$anterior"; then
    echo "Vuelta atrás a $(etiqueta_de "$anterior"): $problema"
    exit 1
fi
echo "Vuelta atrás a $(etiqueta_de "$anterior") sin éxito: tampoco quedó sana; revisar a mano ($problema)"
exit 2
