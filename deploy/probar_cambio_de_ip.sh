#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Un proceso que cambia de IP, de punta a punta sobre el perfil levantado (#274, D-116).
# Uso, desde cualquier directorio: deploy/probar_cambio_de_ip.sh
# Reproduce lo que pasaba en la CI: identidad se detiene, distributivo se queda con su IP, e
# identidad vuelve con otra. Con el DNS guardado 600 s, el API Gateway mandaba el inicio de sesión a
# distributivo y respondía 401. Comprueba que el inicio de sesión sigue llegando a identidad.
# Al terminar, aunque un paso falle, devuelve distributivo a una IP propia y lo reinicia.
# Termina con 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime una clave ni un token.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"

valor_de_entorno() {
    local archivo linea valor=""
    for archivo in .env.ejemplo .env; do
        [[ -f $archivo ]] || continue
        linea=$(grep -E "^$1=" "$archivo" | tail -1 || true)
        [[ -n $linea ]] && valor=${linea#*=}
    done
    printf '%s' "$valor"
}

fallas=0
verificar() {
    local numero=$1 descripcion=$2
    shift 2
    if "$@"; then
        echo "OK     $numero. $descripcion"
    else
        echo "FALLA  $numero. $descripcion"
        fallas=$((fallas + 1))
    fi
}

contenedor() { docker compose ps -q "$1"; }
ip_de() { docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "$(contenedor "$1")"; }
red() { docker inspect -f '{{range $k, $v := .NetworkSettings.Networks}}{{$k}}{{end}}' "$(contenedor gateway)"; }

inicio_de_sesion() {  # el estado del inicio de sesión de consolidacion.demo por el API Gateway
    curl -s -o /dev/null -w '%{http_code}' -X POST "$GATEWAY/api/usuarios/sesion" \
        -H 'Content-Type: application/json' \
        --data "$(jq -cn --arg c "$(valor_de_entorno MDD_USUARIOS_DEMO_CLAVE)" \
            '{usuario: "consolidacion.demo", clave: $c}')" || echo 000
}

vieja=''
red_del_perfil=''
restaurar() {
    [[ -n $red_del_perfil ]] || return 0
    docker compose start identidad >/dev/null 2>&1 || true
    docker network disconnect "$red_del_perfil" "$(contenedor distributivo)" >/dev/null 2>&1 || true
    docker network connect --alias distributivo "$red_del_perfil" "$(contenedor distributivo)" >/dev/null 2>&1 || true
    # Al sacarlo de la red, sus conexiones abiertas (PostgreSQL, datos, identidad) quedan muertas sin
    # que nadie le avise: se reinicia para que los scripts siguientes lo encuentren como nuevo.
    docker compose restart distributivo >/dev/null 2>&1 || true
    docker compose up -d --wait --no-recreate identidad distributivo >/dev/null 2>&1 || true
}
trap restaurar EXIT

antes() { [[ $(inicio_de_sesion) == 200 ]]; }

# distributivo toma la IP de identidad detenida; identidad vuelve con otra.
cambio_de_ip() {
    red_del_perfil=$(red)
    vieja=$(ip_de identidad)
    docker compose stop identidad >/dev/null 2>&1 || return 1
    docker network disconnect "$red_del_perfil" "$(contenedor distributivo)" || return 1
    docker network connect --ip "$vieja" --alias distributivo "$red_del_perfil" "$(contenedor distributivo)" || return 1
    docker compose start identidad >/dev/null 2>&1 || return 1
    docker compose up -d --wait --no-recreate identidad >/dev/null 2>&1 || return 1
    [[ $(ip_de distributivo) == "$vieja" && $(ip_de identidad) != "$vieja" ]]
}

# Tres veces seguidas, para que una conexión reutilizada tampoco vaya a la IP vieja.
despues() {
    for _ in 1 2 3; do
        [[ $(inicio_de_sesion) == 200 ]] || return 1
        sleep 1
    done
}

verificar 1 "consolidacion.demo inicia sesión por el API Gateway" antes
verificar 2 "identidad se detiene, distributivo se queda con su IP e identidad vuelve con otra" cambio_de_ip
verificar 3 "El inicio de sesión sigue llegando a identidad: 200 tres veces, no 401 de distributivo" despues

if ((fallas > 0)); then
    echo "Un proceso que cambia de IP: $fallas paso(s) fallan."
    exit 1
fi
echo "Un proceso que cambia de IP: todos los pasos pasan."
