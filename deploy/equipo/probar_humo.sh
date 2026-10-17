#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Prueba de humo del equipo de demostración a través del proxy (B00-H08, escenarios 3 y 5;
# specs/009-publicacion-despliegue/contracts/equipo.md). La corre desplegar.sh después de que todos
# los contenedores quedan sanos; también se puede correr a mano:
#   RAIZ=/opt/mdd deploy/equipo/probar_humo.sh
# Lee DIRECCION, PUERTO_HTTPS, DEMO_USUARIO, DEMO_CLAVE y MDD_USUARIO_PRUEBA_CLAVE de $RAIZ/.env.
# Las credenciales y el token van a curl por la entrada estándar: no aparecen en la lista de
# procesos ni en la salida. Termina con 0 si los 4 pasos pasan y con 1 si alguno falla.
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
clave_prueba=$(valor MDD_USUARIO_PRUEBA_CLAVE)
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

es_autor_prueba() {
    [[ $estado == 200 ]] && [[ $(jq -r '.usuario // empty' <<<"$cuerpo" 2>/dev/null) == autor.prueba ]]
}

# 1. Sin credenciales, el proxy las pide y no muestra la SPA (escenario 5).
pedir GET / </dev/null
verificar 1 "Sin credenciales, el proxy pide usuario y contraseña" pide_credenciales

# 2. Con las credenciales de la demo, la SPA.
pedir GET / <<<"user = \"$usuario_demo:$clave_demo\""
verificar 2 "Con las credenciales de la demo, la SPA en español" es_la_spa

# 3. Inicio de sesión de autor.prueba por /api (sin basic_auth: lo protege el token, ADR-014).
cuerpo_sesion=$(jq -cn --arg clave "$clave_prueba" '{usuario: "autor.prueba", clave: $clave}')
pedir POST /api/usuarios/sesion <<EOF
header = "Content-Type: application/json"
data = "$(sed 's/\\/\\\\/g; s/"/\\"/g' <<<"$cuerpo_sesion")"
EOF
token=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
verificar 3 "autor.prueba inicia sesión a través del proxy" test "$estado" = 200 -a -n "$token"

# 4. La sesión actual con ese token.
pedir GET /api/usuarios/sesion <<<"header = \"Authorization: Bearer $token\""
verificar 4 "La sesión actual responde con autor.prueba" es_autor_prueba

if ((fallas > 0)); then
    echo "Fallan $fallas pasos de la prueba de humo." >&2
    exit 1
fi
