#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Autenticación de punta a punta sobre el perfil levantado (B00-H03, escenarios 1 a 3;
# specs/003-autenticacion-jwt/contracts/medicion-r19-sesion.md).
# Uso, desde cualquier directorio: deploy/probar_autenticacion.sh
# Termina con 0 si los 6 pasos pasan y con 1 si alguno falla. Nunca imprime el token ni la
# contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly USUARIO="autor.prueba"
readonly CUERPO_401='{"titulo":"Sesión no válida","descripcion":"Inicia sesión de nuevo para continuar.","codigo":"SEGURIDAD_NO_AUTENTICADO"}'

# La contraseña del usuario de prueba: la de .env si existe, si no la de .env.ejemplo.
clave_de_prueba() {
    local archivo valor=""
    for archivo in .env.ejemplo .env; do
        [[ -f $archivo ]] || continue
        local linea
        linea=$(grep -E '^MDD_USUARIO_PRUEBA_CLAVE=' "$archivo" | tail -1 || true)
        [[ -n $linea ]] && valor=${linea#*=}
    done
    printf '%s' "$valor"
}

fallas=0
# verificar <número> <descripción> <comando...>: el comando se evalúa dentro de un if, así que una
# falla se anota y el script sigue con los demás pasos.
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

# Hace una petición y deja el estado en $estado, el cuerpo en $cuerpo y los encabezados en $encabezados.
pedir() {  # pedir <método> <url> [autorización] [cuerpo json]
    local archivo_cuerpo archivo_encabezados
    archivo_cuerpo=$(mktemp)
    archivo_encabezados=$(mktemp)
    local argumentos=(-s -o "$archivo_cuerpo" -D "$archivo_encabezados" -w '%{http_code}' -X "$1" "$2")
    [[ -n ${3:-} ]] && argumentos+=(-H "Authorization: $3")
    [[ -n ${4:-} ]] && argumentos+=(-H 'Content-Type: application/json' --data "$4")
    estado=$(curl "${argumentos[@]}" || echo 000)
    cuerpo=$(cat "$archivo_cuerpo")
    encabezados=$(tr -d '\r' < "$archivo_encabezados")
    rm -f "$archivo_cuerpo" "$archivo_encabezados"
}

es_401_uniforme() {
    [[ $estado == 401 && $cuerpo == "$CUERPO_401" ]] && grep -qx 'WWW-Authenticate: Bearer' <<<"$encabezados"
}

hay_token() {
    [[ $estado == 200 && -n $token ]]
}

es_sesion_de_prueba() {
    local sesion
    sesion=$(jq -c '{usuario, roles}' <<<"$cuerpo" 2>/dev/null || true)
    [[ $estado == 200 && $sesion == '{"usuario":"autor.prueba","roles":["COORDINADOR_CARRERA","DOCENTE"]}' ]]
}

es_401_directo() {
    grep -q '^HTTP/1.1 401' <<<"$directo" && grep -qx 'WWW-Authenticate: Bearer' <<<"$directo" \
        && grep -qxF "$CUERPO_401" <<<"$directo"
}

son_credenciales_invalidas() {
    [[ $estado == 401 && $(jq -r '.codigo' <<<"$cuerpo" 2>/dev/null) == USUARIOS_CREDENCIALES_INVALIDAS ]]
}

# 1. Sin token
pedir GET "$GATEWAY/api/cargas/sesion"
verificar 1 "Sin token: 401 con SEGURIDAD_NO_AUTENTICADO y WWW-Authenticate: Bearer (recibido: $estado)" es_401_uniforme

# 2. Inicio de sesión
credenciales=$(jq -cn --arg u "$USUARIO" --arg c "$(clave_de_prueba)" '{usuario: $u, clave: $c}')
pedir POST "$GATEWAY/api/usuarios/sesion" "" "$credenciales"
token=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
verificar 2 "Inicio de sesión de $USUARIO: 200 con token (recibido: $estado)" hay_token

# 3. Sesión actual en los 4 procesos
for servicio in usuarios cargas generacion dashboard; do
    pedir GET "$GATEWAY/api/$servicio/sesion" "Bearer $token"
    verificar 3 "Sesión actual en $servicio: $USUARIO con COORDINADOR_CARRERA y DOCENTE (recibido: $estado)" es_sesion_de_prueba
done

# 4. Token alterado: un carácter de la firma cambiado
firma=${token##*.}
posicion=10
reemplazo=A
[[ ${firma:$posicion:1} == A ]] && reemplazo=B
alterado="${token%.*}.${firma:0:$posicion}${reemplazo}${firma:$((posicion + 1))}"
pedir GET "$GATEWAY/api/generacion/sesion" "Bearer $alterado"
verificar 4 "Token alterado: el mismo 401 que sin token (recibido: $estado)" es_401_uniforme

# 5. Directo al proceso salida, sin pasar por el API Gateway
directo=$(docker compose exec -T salida bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /api/dashboard/sesion HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 && cat <&3' | tr -d '\r' || true)
verificar 5 "Directo al proceso salida sin token: el mismo 401" es_401_directo

# 6. Contraseña incorrecta
incorrectas=$(jq -cn --arg u "$USUARIO" '{usuario: $u, clave: "no-es-la-clave"}')
pedir POST "$GATEWAY/api/usuarios/sesion" "" "$incorrectas"
verificar 6 "Contraseña incorrecta: 401 con USUARIOS_CREDENCIALES_INVALIDAS (recibido: $estado)" son_credenciales_invalidas

if [[ $fallas -eq 0 ]]; then
    echo "Autenticación de punta a punta: todos los pasos pasan."
    exit 0
fi
echo "Autenticación de punta a punta: $fallas paso(s) fallan."
exit 1
