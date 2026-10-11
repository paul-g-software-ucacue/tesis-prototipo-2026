#!/usr/bin/env bash
# Despliegue en el VPS con vuelta atrás. Uso: desplegar.sh <etiqueta-del-commit>
# El VPS inicia sesión en Docker Hub con un token de solo lectura (docker login, una vez).
set -euo pipefail
cd "$(dirname "$0")"

nueva=$1
anterior=$(grep -s '^ETIQUETA=' .env | cut -d= -f2 || true)
espera=${ESPERA_SALUD:-180}

poner_etiqueta() {
    if grep -q '^ETIQUETA=' .env 2>/dev/null; then
        sed -i "s/^ETIQUETA=.*/ETIQUETA=$1/" .env
    else
        echo "ETIQUETA=$1" >> .env
    fi
}

todos_sanos() {
    local fin=$((SECONDS + espera))
    while (( SECONDS < fin )); do
        local malos
        malos=$(docker compose -f compose.vps.yml ps --format '{{.Name}} {{.Health}}' | awk '$2 != "healthy" && $2 != ""' | wc -l)
        [[ $malos -eq 0 ]] && return 0
        sleep 5
    done
    docker compose -f compose.vps.yml ps
    return 1
}

echo "Desplegando $nueva (anterior: ${anterior:-ninguna})"
poner_etiqueta "$nueva"
docker compose -f compose.vps.yml pull
docker compose -f compose.vps.yml up -d --remove-orphans

if todos_sanos; then
    echo "Despliegue de $nueva sano."
    docker image prune -f > /dev/null
    exit 0
fi

echo "Un proceso no quedó sano." >&2
if [[ -n $anterior ]]; then
    echo "Vuelvo a $anterior." >&2
    poner_etiqueta "$anterior"
    docker compose -f compose.vps.yml up -d --remove-orphans
    todos_sanos || echo "La versión anterior tampoco quedó sana; revisar a mano." >&2
fi
exit 1
