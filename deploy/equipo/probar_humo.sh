#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Prueba de humo del equipo de demostración a través del proxy (B00-H08, escenarios 3 y 5;
# specs/009-publicacion-despliegue/contracts/equipo.md). La corre desplegar.sh después de que todos
# los contenedores quedan sanos; también se puede correr a mano:
#   RAIZ=/opt/mdd deploy/equipo/probar_humo.sh
# Lee DIRECCION, PUERTO_HTTPS, DEMO_USUARIO, DEMO_CLAVE y MDD_SECRETOS de $RAIZ/.env, y la contraseña
# de docente.demo de secretos/claves-demo (D-59). Las credenciales y el token van a curl por la
# entrada estándar: no aparecen en la lista de procesos ni en la salida. Termina con 0 si los 5
# pasos pasan y con 1 si alguno falla.
set -euo pipefail

readonly RAIZ="${RAIZ:-/opt/mdd}"
readonly ENTORNO="$RAIZ/.env"

# Lee una variable del .env sin ejecutarlo (el hash de Caddy tiene $ y va entre comillas simples).
valor() {
    local linea
    linea=$(grep -E "^$1=" "$ENTORNO" | tail -1 || true)
    linea=${linea#*=}
    linea=${linea#\'}
    printf '%s' "${linea%\'}"
}

direccion=$(valor DIRECCION)
direccion=${direccion:-mdd.localhost}
puerto=$(valor PUERTO_HTTPS)
puerto=${puerto:-443}
usuario_demo=$(valor DEMO_USUARIO)
clave_demo=$(valor DEMO_CLAVE)
secretos=$(valor MDD_SECRETOS)
secretos=${secretos:-$RAIZ/secretos}
clave_docente=$(awk -F= '$1 == "docente.demo" { sub(/^[^=]*=/, ""); print; exit }' \
    "$secretos/claves-demo" 2>/dev/null || true)
readonly BASE="https://$direccion:$puerto"

opciones=(-s --max-time 15 --resolve "$direccion:$puerto:127.0.0.1")
# La CA interna de Caddy solo se acepta para los nombres *.localhost del equipo (research §9).
[[ $direccion == *.localhost || $direccion == localhost ]] && opciones+=(-k)

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

# Deja el estado en $estado, el cuerpo en $cuerpo y las cabeceras en $cabeceras. La configuración
# extra de curl (credenciales, cabeceras o cuerpo) llega por la entrada estándar con -K -.
pedir() {  # pedir <método> <ruta> < configuración de curl
    local archivo encabezados
    archivo=$(mktemp)
    encabezados=$(mktemp)
    estado=$(curl "${opciones[@]}" -K - -o "$archivo" -D "$encabezados" -w '%{http_code}' \
        -X "$1" "$BASE$2" || echo 000)
    cuerpo=$(cat "$archivo")
    cabeceras=$(tr -d '\r' <"$encabezados")
    rm -f "$archivo" "$encabezados"
}

pide_credenciales() {
    [[ $estado == 401 ]] && grep -qi '^www-authenticate: basic' <<<"$cabeceras"
}

es_la_spa() {
    [[ $estado == 200 ]] && grep -q '<html lang="es">' <<<"$cuerpo"
}

es_docente_demo() {
    [[ $estado == 200 ]] && [[ $(jq -r '.usuario // empty' <<<"$cuerpo" 2>/dev/null) == docente.demo ]]
}

# Rechazado sin token: credenciales inválidas o, tras 5 despliegues seguidos, el bloqueo (D-60).
no_entra() {
    local codigo
    codigo=$(jq -r '.codigo // empty' <<<"$cuerpo" 2>/dev/null || true)
    [[ ($estado == 401 && $codigo == USUARIOS_CREDENCIALES_INVALIDAS)
        || ($estado == 429 && $codigo == USUARIOS_INICIO_BLOQUEADO) ]]
}

# Inicia sesión por /api, con las credenciales por la entrada estándar de curl.
iniciar_sesion() {  # iniciar_sesion <usuario> <clave>
    local credenciales
    credenciales=$(jq -cn --arg usuario "$1" --arg clave "$2" '{usuario: $usuario, clave: $clave}')
    pedir POST /api/usuarios/sesion <<FIN
header = "Content-Type: application/json"
data = "$(sed 's/\\/\\\\/g; s/"/\\"/g' <<<"$credenciales")"
FIN
}

# 1. Sin credenciales, el proxy las pide y no muestra la SPA (escenario 5).
pedir GET / </dev/null
verificar 1 "Sin credenciales, el proxy pide usuario y contraseña" pide_credenciales

# 2. Con las credenciales de la demo, la SPA.
pedir GET / <<<"user = \"$usuario_demo:$clave_demo\""
verificar 2 "Con las credenciales de la demo, la SPA en español" es_la_spa

# 3. Inicio de sesión de docente.demo por /api (sin basic_auth: lo protege el token, ADR-014).
iniciar_sesion docente.demo "$clave_docente"
token=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
verificar 3 "docente.demo inicia sesión a través del proxy" test "$estado" = 200 -a -n "$token"

# 4. La sesión actual con ese token.
pedir GET /api/usuarios/sesion <<<"header = \"Authorization: Bearer $token\""
verificar 4 "La sesión actual responde con docente.demo" es_docente_demo

# 5. autor.prueba ya no entra al equipo de demostración (D-57, SC-006).
iniciar_sesion autor.prueba "$clave_docente"
verificar 5 "autor.prueba no inicia sesión" no_entra

if ((fallas > 0)); then
    echo "Fallan $fallas pasos de la prueba de humo." >&2
    exit 1
fi
