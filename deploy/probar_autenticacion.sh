#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Autenticación de punta a punta sobre el perfil levantado (B00-H03, escenarios 1 a 3;
# specs/003-autenticacion-jwt/contracts/medicion-r19-sesion.md). Desde F05-H01, también los
# usuarios de la demo (D-59), el bloqueo por intentos (D-60) y la renovación (ADR-020).
# Uso, desde cualquier directorio: deploy/probar_autenticacion.sh
# Termina con 0 si los 9 pasos pasan y con 1 si alguno falla. Nunca imprime el token ni la
# contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly USUARIO="autor.prueba"
readonly USUARIOS_DEMO=backend/servicios/usuarios/src/main/resources/usuarios-demo.yaml
readonly CUERPO_401='{"titulo":"Sesión no válida","descripcion":"Inicia sesión de nuevo para continuar.","codigo":"SEGURIDAD_NO_AUTENTICADO"}'

# El valor de una variable: el de .env si existe, si no el de .env.ejemplo.
valor_de_entorno() {
    local archivo valor=""
    for archivo in .env.ejemplo .env; do
        [[ -f $archivo ]] || continue
        local linea
        linea=$(grep -E "^$1=" "$archivo" | tail -1 || true)
        [[ -n $linea ]] && valor=${linea#*=}
    done
    printf '%s' "$valor"
}

clave_de_prueba() {
    valor_de_entorno MDD_USUARIO_PRUEBA_CLAVE
}

# Los usuarios de la demo, una línea por usuario: «usuario<TAB>nombre<TAB>roles ordenados en JSON».
# El archivo tiene una forma fija (usuario, nombre y roles en ese orden), así que basta con awk.
usuarios_demo() {
    awk '
        /^  - usuario: / { usuario = $3 }
        /^    nombre: / { sub(/^    nombre: /, ""); nombre = $0 }
        /^    roles: / {
            roles = $0
            sub(/^    roles: \[/, "", roles); sub(/\]$/, "", roles); gsub(/ /, "", roles)
            n = split(roles, lista, ",")
            for (i = 1; i <= n; i++) for (j = i + 1; j <= n; j++)
                if (lista[j] < lista[i]) { t = lista[i]; lista[i] = lista[j]; lista[j] = t }
            json = "["
            for (i = 1; i <= n; i++) json = json (i > 1 ? "," : "") "\"" lista[i] "\""
            printf "%s\t%s\t%s]\n", usuario, nombre, json
        }
    ' "$USUARIOS_DEMO"
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

es_sesion_demo() {
    local sesion esperada
    sesion=$(jq -c '{usuario, nombre, roles}' <<<"$cuerpo" 2>/dev/null || true)
    esperada=$(jq -cn --arg u "$1" --arg n "$2" --argjson r "$3" '{usuario: $u, nombre: $n, roles: $r}')
    [[ $estado == 200 && $sesion == "$esperada" ]]
}

# intentos_hasta_bloqueo <usuario>: 5 intentos fallidos y el sexto; deja los cuerpos de 401 y 429.
intentos_hasta_bloqueo() {
    local fallidas
    fallidas=$(jq -cn --arg u "$1" '{usuario: $u, clave: "no-es-la-clave"}')
    estados_401=""
    for _ in 1 2 3 4 5; do
        pedir POST "$GATEWAY/api/usuarios/sesion" "" "$fallidas"
        estados_401+=$estado
        cuerpo_401=$cuerpo
    done
    pedir POST "$GATEWAY/api/usuarios/sesion" "" "$fallidas"
}

esta_bloqueado() {
    [[ $estados_401 == 401401401401401 && $estado == 429 \
        && $(jq -r '.codigo' <<<"$cuerpo" 2>/dev/null) == USUARIOS_INICIO_BLOQUEADO ]] \
        && grep -qE '^Retry-After: [0-9]+$' <<<"$encabezados"
}

# El bloqueo de un usuario que existe responde igual que el de uno que no: no revela cuál existe.
bloqueo_igual_al_inventado() {
    esta_bloqueado && [[ $cuerpo == "$cuerpo_429_inventado" && $cuerpo_401 == "$cuerpo_401_inventado" ]]
}

hay_token_renovado() {
    [[ $estado == 200 && -n $renovado && $(jq -r '.tipo' <<<"$cuerpo" 2>/dev/null) == Bearer ]]
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

# 7. Los usuarios de la demo entran con la clave común y solo con sus roles (D-59, SC-001)
clave_demo=$(valor_de_entorno MDD_USUARIOS_DEMO_CLAVE)
token_demo=""
while IFS=$'\t' read -r usuario_demo nombre_demo roles_demo; do
    credenciales=$(jq -cn --arg u "$usuario_demo" --arg c "$clave_demo" '{usuario: $u, clave: $c}')
    pedir POST "$GATEWAY/api/usuarios/sesion" "" "$credenciales"
    token=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
    [[ $usuario_demo == docente.demo ]] && token_demo=$token
    pedir GET "$GATEWAY/api/usuarios/sesion" "Bearer $token"
    verificar 7 "Usuario de la demo $usuario_demo: su nombre y exactamente $roles_demo (recibido: $estado)" \
        es_sesion_demo "$usuario_demo" "$nombre_demo" "$roles_demo"
done < <(usuarios_demo)

# 8. Bloqueo tras 5 intentos fallidos, con un nombre inventado y con uno que existe (D-60). ocs.demo
# no lo usa ningún otro paso; al final se borran sus intentos para poder repetir la prueba.
intentos_hasta_bloqueo "bloqueo-$RANDOM$RANDOM"
cuerpo_429_inventado=$cuerpo
cuerpo_401_inventado=$cuerpo_401
verificar 8 "Nombre inventado: 5 veces 401 y el sexto 429 USUARIOS_INICIO_BLOQUEADO con Retry-After (recibido: $estado)" esta_bloqueado
intentos_hasta_bloqueo ocs.demo
verificar 8 "ocs.demo: el mismo 401 y el mismo 429 que el nombre inventado (recibido: $estado)" bloqueo_igual_al_inventado
docker compose exec -T postgres psql -q -U postgres -d "$(valor_de_entorno POSTGRES_DB)" \
    -c "delete from usuarios.intento_fallido where nombre = 'ocs.demo'" >/dev/null || true

# 9. Renovación del token (ADR-020): con un token vigente, otro que sirve; alterado, el 401 uniforme
pedir POST "$GATEWAY/api/usuarios/sesion/renovacion" "Bearer $token_demo"
renovado=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
verificar 9 "Renovación con el token de docente.demo: 200 con un token nuevo (recibido: $estado)" hay_token_renovado
pedir GET "$GATEWAY/api/usuarios/sesion" "Bearer $renovado"
verificar 9 "El token renovado sirve: la sesión de docente.demo (recibido: $estado)" \
    es_sesion_demo docente.demo "Docente (demo)" '["DOCENTE"]'
firma=${token_demo##*.}
reemplazo=A
[[ ${firma:10:1} == A ]] && reemplazo=B
pedir POST "$GATEWAY/api/usuarios/sesion/renovacion" "Bearer ${token_demo%.*}.${firma:0:10}${reemplazo}${firma:11}"
verificar 9 "Renovación con un token alterado: el mismo 401 que sin token (recibido: $estado)" es_401_uniforme

if [[ $fallas -eq 0 ]]; then
    echo "Autenticación de punta a punta: todos los pasos pasan."
    exit 0
fi
echo "Autenticación de punta a punta: $fallas paso(s) fallan."
exit 1
