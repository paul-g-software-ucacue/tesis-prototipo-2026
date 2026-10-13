#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Errores de punta a punta sobre el perfil levantado (B00-H04; specs/004-modelo-errores/quickstart.md).
# Uso, desde cualquier directorio: deploy/probar_errores.sh
# Comprueba, a través del API Gateway, que los errores llegan con el esquema Error: 400 con
# detalles, 404, 405 y 503 con un proceso detenido. Detiene y vuelve a levantar el proceso salida.
# Termina con 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime el token.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"

clave_de_prueba() {
    local archivo linea valor=""
    for archivo in .env.ejemplo .env; do
        [[ -f $archivo ]] || continue
        linea=$(grep -E '^MDD_USUARIO_PRUEBA_CLAVE=' "$archivo" | tail -1 || true)
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

# Deja el estado en $estado y el cuerpo en $cuerpo.
pedir() {  # pedir <método> <url> [autorización] [cuerpo json]
    local archivo
    archivo=$(mktemp)
    local argumentos=(-s -o "$archivo" -w '%{http_code}' -X "$1" "$2")
    [[ -n ${3:-} ]] && argumentos+=(-H "Authorization: $3")
    [[ -n ${4:-} ]] && argumentos+=(-H 'Content-Type: application/json' --data "$4")
    estado=$(curl "${argumentos[@]}" || echo 000)
    cuerpo=$(cat "$archivo")
    rm -f "$archivo"
}

# El cuerpo es un Error con ese código y sin los campos del formato por defecto del framework.
es_error() {  # es_error <estado> <código>
    [[ $estado == "$1" ]] \
        && [[ $(jq -r '.codigo' <<<"$cuerpo" 2>/dev/null) == "$2" ]] \
        && [[ $(jq -r 'has("timestamp") or has("path") or has("trace") or has("requestId")' <<<"$cuerpo" 2>/dev/null) == false ]]
}

tiene_dos_detalles() {
    es_error 400 COMUN_SOLICITUD_INVALIDA \
        && [[ $(jq -c '[.detalles[] | .campo] | sort' <<<"$cuerpo") == '["clave","usuario"]' ]]
}

credenciales=$(jq -cn --arg c "$(clave_de_prueba)" '{usuario: "autor.prueba", clave: $c}')
pedir POST "$GATEWAY/api/usuarios/sesion" "" "$credenciales"
token=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
if [[ -z $token ]]; then
    echo "FALLA  0. No se pudo iniciar sesión con el usuario de prueba (recibido: $estado)"
    exit 1
fi

# 1. Error de entrada: datos vacíos en el inicio de sesión (D-42)
pedir POST "$GATEWAY/api/usuarios/sesion" "" '{"usuario":""}'
verificar 1 "Datos vacíos: 400 COMUN_SOLICITUD_INVALIDA con detalles de usuario y clave (recibido: $estado)" tiene_dos_detalles

# 2. Ruta inexistente con token
pedir GET "$GATEWAY/api/cargas/no-existe" "Bearer $token"
verificar 2 "Ruta inexistente: 404 COMUN_RECURSO_NO_ENCONTRADO (recibido: $estado)" es_error 404 COMUN_RECURSO_NO_ENCONTRADO

# 3. Método no permitido
pedir DELETE "$GATEWAY/api/cargas/sesion" "Bearer $token"
verificar 3 "Método no permitido: 405 COMUN_METODO_NO_PERMITIDO (recibido: $estado)" es_error 405 COMUN_METODO_NO_PERMITIDO

# 4. Proceso detenido detrás del API Gateway (D-40)
docker compose stop salida >/dev/null 2>&1
pedir GET "$GATEWAY/api/dashboard/sesion" "Bearer $token"
verificar 4 "Proceso detenido: 503 COMUN_SERVICIO_NO_DISPONIBLE (recibido: $estado)" es_error 503 COMUN_SERVICIO_NO_DISPONIBLE
docker compose start salida >/dev/null 2>&1
docker compose up -d --wait salida >/dev/null 2>&1 || {
    echo "FALLA  4. El proceso salida no volvió a quedar sano"
    fallas=$((fallas + 1))
}

if [[ $fallas -eq 0 ]]; then
    echo "Errores de punta a punta: todos los pasos pasan."
    exit 0
fi
echo "Errores de punta a punta: $fallas paso(s) fallan."
exit 1
