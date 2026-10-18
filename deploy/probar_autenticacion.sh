#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Autenticación de punta a punta sobre el perfil levantado (B00-H03, escenarios 1 a 3;
# specs/003-autenticacion-jwt/contracts/medicion-r19-sesion.md). Desde F05-H01, también los
# usuarios de la demo (D-59), el bloqueo por intentos (D-60) y la renovación (ADR-020). Desde
# F05-H02, los roles con permisos: un rol nuevo, solo lectura con 403 y varios roles (ADR-021).
# Uso, desde cualquier directorio: deploy/probar_autenticacion.sh
# Termina con 0 si los 12 pasos pasan y con 1 si alguno falla. Nunca imprime el token ni la
# contraseña. Deja los roles como los encontró: borra el rol de prueba y se lo quita a docente.demo.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly USUARIO="autor.prueba"
readonly USUARIOS_DEMO=backend/servicios/usuarios/src/main/resources/usuarios-demo.yaml
readonly CUERPO_401='{"titulo":"Sesión no válida","descripcion":"Inicia sesión de nuevo para continuar.","codigo":"SEGURIDAD_NO_AUTENTICADO"}'
readonly CUERPO_403='{"titulo":"Acceso no autorizado","descripcion":"No tienes permiso para esta acción. Si crees que deberías tenerlo, pide al administrador que revise tus roles.","codigo":"SEGURIDAD_ACCESO_NO_AUTORIZADO"}'
# El rol que crean los pasos 10 a 12 y su identificador, derivado del nombre (research §7).
readonly ROL_DE_PRUEBA="Auditor de roles"
readonly ID_DE_PRUEBA=AUDITOR_DE_ROLES

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

# iniciar <usuario> <clave>: deja el token en $token, o vacío si no entró.
iniciar() {
    pedir POST "$GATEWAY/api/usuarios/sesion" "" "$(jq -cn --arg u "$1" --arg c "$2" '{usuario: $u, clave: $c}')"
    token=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
}

# Deja docente.demo solo con DOCENTE y borra el rol de prueba, si quedó de una corrida anterior.
limpiar_rol_de_prueba() {
    pedir PUT "$GATEWAY/api/roles/usuarios/docente.demo" "Bearer $1" '{"roles":["DOCENTE"]}'
    pedir DELETE "$GATEWAY/api/roles/$ID_DE_PRUEBA" "Bearer $1"
}

es_403_comun() {
    [[ $estado == 403 && $cuerpo == "$CUERPO_403" ]]
}

es_rol_de_prueba_creado() {
    [[ $estado == 201 && $(jq -c '{identificador, nombre, predefinido, usuariosAsignados}' <<<"$cuerpo" 2>/dev/null) \
        == "{\"identificador\":\"$ID_DE_PRUEBA\",\"nombre\":\"$ROL_DE_PRUEBA\",\"predefinido\":false,\"usuariosAsignados\":0}" ]]
}

# es_sesion_con <roles json> <permisos json>: la sesión actual tiene exactamente esos roles y permisos.
es_sesion_con() {
    [[ $estado == 200 && $(jq -c '{roles, permisos}' <<<"$cuerpo" 2>/dev/null) \
        == "$(jq -cn --argjson r "$1" --argjson p "$2" '{roles: $r, permisos: $p}')" ]]
}

tiene_permiso_de_lectura_de_roles() {
    [[ $estado == 200 ]] && jq -e '.permisos | index("USUARIOS_ROLES:LECTURA")' <<<"$cuerpo" >/dev/null 2>&1
}

no_tiene_permiso_de_lectura_de_roles() {
    [[ $estado == 200 ]] && ! jq -e '.permisos | index("USUARIOS_ROLES:LECTURA")' <<<"$cuerpo" >/dev/null 2>&1
}

es_estado() {
    [[ $estado == "$1" ]]
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
# Si una corrida anterior se cortó entre los pasos 10 y 12, docente.demo podría tener el rol de
# prueba: se limpia antes de comprobar los roles iniciales.
iniciar admin.demo "$clave_demo"
token_admin=$token
limpiar_rol_de_prueba "$token_admin"
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

# 10. Rol nuevo (F05-H02, escenario 1): admin.demo crea un rol con solo «Usuarios y roles: lectura»
# y se lo asigna a docente.demo; su nueva sesión, y la renovación de la anterior (D-62), lo traen.
rol_nuevo=$(jq -cn --arg n "$ROL_DE_PRUEBA" '{nombre: $n, descripcion: "Ve los roles sin cambiarlos.",
    permisos: [{funcionalidad: "USUARIOS_ROLES", tipo: "LECTURA", alcance: "TODO"}]}')
pedir POST "$GATEWAY/api/roles" "Bearer $token_admin" "$rol_nuevo"
verificar 10 "admin.demo crea «$ROL_DE_PRUEBA»: 201 con el identificador $ID_DE_PRUEBA (recibido: $estado)" es_rol_de_prueba_creado
pedir PUT "$GATEWAY/api/roles/usuarios/docente.demo" "Bearer $token_admin" "{\"roles\":[\"DOCENTE\",\"$ID_DE_PRUEBA\"]}"
verificar 10 "admin.demo se lo asigna a docente.demo (recibido: $estado)" es_estado 200
pedir POST "$GATEWAY/api/usuarios/sesion/renovacion" "Bearer $token_demo"
renovado=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
pedir GET "$GATEWAY/api/usuarios/sesion" "Bearer $renovado"
verificar 10 "La sesión anterior de docente.demo, al renovarse, ya trae USUARIOS_ROLES:LECTURA (D-62)" tiene_permiso_de_lectura_de_roles
iniciar docente.demo "$clave_demo"
token_lectura=$token
pedir GET "$GATEWAY/api/usuarios/sesion" "Bearer $token_lectura"
verificar 10 "Nueva sesión de docente.demo: sus dos roles y los permisos de los dos (recibido: $estado)" \
    es_sesion_con "[\"$ID_DE_PRUEBA\",\"DOCENTE\"]" \
    '["MI_DISTRIBUTIVO:ESCRITURA:PROPIO","MI_DISTRIBUTIVO:LECTURA:PROPIO","USUARIOS_ROLES:LECTURA"]'

# 11. Solo lectura (escenario 2, RNF-SEG-03): docente.demo lista los roles, pero crear uno da el 403
# común, aunque el cuerpo sea inválido, y el rol no se crea.
pedir GET "$GATEWAY/api/roles" "Bearer $token_lectura"
verificar 11 "Con solo lectura, GET /api/roles: 200 (recibido: $estado)" es_estado 200
prohibido=$(jq -cn '{nombre: "Rol prohibido", descripcion: "No debe crearse.",
    permisos: [{funcionalidad: "CARGAS", tipo: "LECTURA", alcance: "TODO"}]}')
pedir POST "$GATEWAY/api/roles" "Bearer $token_lectura" "$prohibido"
verificar 11 "Con solo lectura, POST /api/roles: 403 SEGURIDAD_ACCESO_NO_AUTORIZADO con el cuerpo común (recibido: $estado)" es_403_comun
pedir POST "$GATEWAY/api/roles" "Bearer $token_lectura" '{"nombre":""}'
verificar 11 "Con solo lectura y un cuerpo inválido: también 403, no 400 (recibido: $estado)" es_403_comun
pedir GET "$GATEWAY/api/roles/ROL_PROHIBIDO" "Bearer $token_admin"
verificar 11 "El rol prohibido no se creó: 404 (recibido: $estado)" es_estado 404

# 12. Varios roles (escenario 3, RF-03): docente.coordinador.demo tiene la unión de Docente y de
# Coordinador de carrera, sin repetir. Después se limpia: docente.demo vuelve a solo DOCENTE, su nueva
# sesión ya no lee los roles (403) y el rol de prueba se borra (escenario 3.3).
iniciar docente.coordinador.demo "$clave_demo"
pedir GET "$GATEWAY/api/usuarios/sesion" "Bearer $token"
verificar 12 "docente.coordinador.demo: los permisos de sus dos roles, sin repetir (recibido: $estado)" \
    es_sesion_con '["COORDINADOR_CARRERA","DOCENTE"]' \
    '["DISTRIBUTIVO:LECTURA:PROPIO","DOCENTES:LECTURA","MI_DISTRIBUTIVO:ESCRITURA:PROPIO","MI_DISTRIBUTIVO:LECTURA:PROPIO","OFERTA:APROBACION:PROPIO","OFERTA:ESCRITURA:PROPIO","OFERTA:LECTURA:PROPIO"]'
pedir PUT "$GATEWAY/api/roles/usuarios/docente.demo" "Bearer $token_admin" '{"roles":["DOCENTE"]}'
verificar 12 "admin.demo le quita el rol de prueba a docente.demo (recibido: $estado)" es_estado 200
iniciar docente.demo "$clave_demo"
pedir GET "$GATEWAY/api/usuarios/sesion" "Bearer $token"
verificar 12 "La nueva sesión de docente.demo ya no trae USUARIOS_ROLES:LECTURA" no_tiene_permiso_de_lectura_de_roles
pedir GET "$GATEWAY/api/roles" "Bearer $token"
verificar 12 "docente.demo sin el rol, GET /api/roles: 403 común (recibido: $estado)" es_403_comun
pedir DELETE "$GATEWAY/api/roles/$ID_DE_PRUEBA" "Bearer $token_admin"
verificar 12 "admin.demo borra «$ROL_DE_PRUEBA»: 204 (recibido: $estado)" es_estado 204

if [[ $fallas -eq 0 ]]; then
    echo "Autenticación de punta a punta: todos los pasos pasan."
    exit 0
fi
echo "Autenticación de punta a punta: $fallas paso(s) fallan."
exit 1
