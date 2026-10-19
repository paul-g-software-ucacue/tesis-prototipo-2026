#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# La normativa de punta a punta sobre el perfil levantado (F04-H01; specs/012-politica-normativa/
# quickstart.md). Uso, desde cualquier directorio: deploy/probar_normativa.sh
# Comprueba que la Política SENESCYT 2024 es la versión 1 con sus 22 filas, que la leen el
# Vicerrector académico y el Administrador y no un docente (D-66), y que reiniciar el proceso datos
# no la duplica ni la cambia (FR-006). Reinicia datos tres veces.
# Termina con 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime el token ni la
# contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly CUERPO_403='{"titulo":"Acceso no autorizado","descripcion":"No tienes permiso para esta acción. Si crees que deberías tenerlo, pide al administrador que revise tus roles.","codigo":"SEGURIDAD_ACCESO_NO_AUTORIZADO"}'

# El valor de una variable: el de .env si existe, si no el de .env.ejemplo.
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

# iniciar <usuario>: deja el token en $token, o vacío si no entró.
iniciar() {
    pedir POST "$GATEWAY/api/usuarios/sesion" "" \
        "$(jq -cn --arg u "$1" --arg c "$(valor_de_entorno MDD_USUARIOS_DEMO_CLAVE)" '{usuario: $u, clave: $c}')"
    token=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
}

hay_una_sola_version_uno() {
    [[ $estado == 200 && $(jq -c '[.[] | {numero, codigo, tipoIst}]' <<<"$cuerpo" 2>/dev/null) \
        == '[{"numero":1,"codigo":"SENESCYT-2024-03","tipoIst":"T2"}]' ]]
}

es_la_politica_de_2024() {
    [[ $estado == 200 ]] \
        && [[ $(jq '.tablaCargaHoraria | length' <<<"$cuerpo") == 22 ]] \
        && [[ $(jq '.reglas | length' <<<"$cuerpo") == 7 ]] \
        && [[ $(jq -c '.tablaCargaHoraria[] | select(.perfil == "DOCENTE_TC")
                | [.horasClase, .horasClaseMinimo, .horasClaseMaximo, .horasOtrasDocencia]' <<<"$cuerpo") \
            == '[22,18,24,18]' ]]
}

es_403_comun() {
    [[ $estado == 403 && $cuerpo == "$CUERPO_403" ]]
}

es_estado() {
    [[ $estado == "$1" ]]
}

es_igual_a_la_guardada() {
    [[ $estado == 200 && $cuerpo == "$guardada" ]]
}

# 1. El Vicerrector académico lista una sola versión, la 1 (D-66)
iniciar vicerrectorado.demo
token_vicerrector=$token
pedir GET "$GATEWAY/api/normativa/versiones" "Bearer $token_vicerrector"
verificar 1 "vicerrectorado.demo: una sola versión, la 1, SENESCYT-2024-03 con Tipo 2 (recibido: $estado)" hay_una_sola_version_uno

# 2. La versión 1 es la política 2024
pedir GET "$GATEWAY/api/normativa/versiones/1" "Bearer $token_vicerrector"
verificar 2 "La versión 1 tiene 22 filas y 7 reglas; DOCENTE_TC con 22 h de clase entre 18 y 24 y 18 de otras de docencia (recibido: $estado)" es_la_politica_de_2024
guardada=$cuerpo

# 3. El Administrador la ve igual; un docente recibe el 403; una versión que no existe, 404
iniciar admin.demo
pedir GET "$GATEWAY/api/normativa/versiones/1" "Bearer $token"
verificar 3 "admin.demo ve la misma versión 1 (recibido: $estado)" es_igual_a_la_guardada
iniciar docente.demo
pedir GET "$GATEWAY/api/normativa/versiones" "Bearer $token"
verificar 3 "docente.demo: 403 SEGURIDAD_ACCESO_NO_AUTORIZADO con el cuerpo común (recibido: $estado)" es_403_comun
pedir GET "$GATEWAY/api/normativa/versiones/99" "Bearer $token_vicerrector"
verificar 3 "Una versión que no existe: 404 (recibido: $estado)" es_estado 404

# 4. Reiniciar datos tres veces no duplica ni cambia la versión 1 (FR-006, SC-003)
for vuelta in 1 2 3; do
    docker compose restart datos >/dev/null 2>&1
    docker compose up -d --wait datos >/dev/null 2>&1
    echo "       datos reiniciado ($vuelta de 3)"
done
pedir GET "$GATEWAY/api/normativa/versiones" "Bearer $token_vicerrector"
verificar 4 "Después de 3 reinicios de datos: sigue una sola versión, la 1 (recibido: $estado)" hay_una_sola_version_uno
pedir GET "$GATEWAY/api/normativa/versiones/1" "Bearer $token_vicerrector"
verificar 4 "Después de 3 reinicios de datos: la versión 1 es idéntica (recibido: $estado)" es_igual_a_la_guardada

if [[ $fallas -eq 0 ]]; then
    echo "Normativa de punta a punta: todos los pasos pasan."
    exit 0
fi
echo "Normativa de punta a punta: $fallas paso(s) fallan."
exit 1
