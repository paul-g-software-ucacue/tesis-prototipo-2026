#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# La SPA de punta a punta sobre el perfil levantado (B00-H06, D-52; specs/007-spa-base/contracts/
# perfil.md). Uso, desde cualquier directorio: deploy/probar_spa.sh
# Comprueba que Nginx sirve la SPA con sus cabeceras de seguridad, que las rutas de la SPA
# responden index.html y que /api llega al API Gateway en el mismo origen, con y sin token.
# Termina con 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime el token.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly SPA="${MDD_SPA:-http://localhost:8081}"

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

# Deja el estado en $estado, el cuerpo en $cuerpo y las cabeceras en $cabeceras.
pedir() {  # pedir <método> <url> [autorización] [cuerpo json]
    local archivo encabezados
    archivo=$(mktemp)
    encabezados=$(mktemp)
    local argumentos=(-s -o "$archivo" -D "$encabezados" -w '%{http_code}' -X "$1" "$2")
    [[ -n ${3:-} ]] && argumentos+=(-H "Authorization: $3")
    [[ -n ${4:-} ]] && argumentos+=(-H 'Content-Type: application/json' --data "$4")
    estado=$(curl "${argumentos[@]}" || echo 000)
    cuerpo=$(cat "$archivo")
    cabeceras=$(tr -d '\r' <"$encabezados")
    rm -f "$archivo" "$encabezados"
}

es_la_spa() {
    [[ $estado == 200 ]] && grep -q '<html lang="es">' <<<"$cuerpo" && grep -q 'id="raiz"' <<<"$cuerpo"
}

con_cabeceras_de_seguridad() {
    grep -qi "^content-security-policy: default-src 'self'; script-src 'self';" <<<"$cabeceras" \
        && grep -qi '^x-content-type-options: nosniff' <<<"$cabeceras" \
        && ! grep -qiE '^server: nginx/[0-9]' <<<"$cabeceras"
}

# 1. La SPA, con sus cabeceras de seguridad
pedir GET "$SPA/"
verificar 1 "GET / responde la SPA en español, con la CSP y sin la versión de Nginx (recibido: $estado)" \
    eval 'es_la_spa && con_cabeceras_de_seguridad'

# 2. Una ruta de la SPA también responde index.html
pedir GET "$SPA/una/ruta/de/la/spa"
verificar 2 "Una ruta de la SPA responde index.html (recibido: $estado)" es_la_spa

# 3. Inicio de sesión a través de la SPA y del API Gateway
credenciales=$(jq -cn --arg c "$(clave_de_prueba)" '{usuario: "autor.prueba", clave: $c}')
pedir POST "$SPA/api/usuarios/sesion" "" "$credenciales"
token=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
verificar 3 "POST /api/usuarios/sesion a través de la SPA: 200 con token (recibido: $estado)" \
    test "$estado" == 200 -a -n "$token"

# 4. Sesión actual con el token, a través de la SPA
tiene_los_roles() {
    [[ $estado == 200 ]] \
        && [[ $(jq -r '.usuario' <<<"$cuerpo" 2>/dev/null) == autor.prueba ]] \
        && [[ $(jq -r '.roles | length' <<<"$cuerpo" 2>/dev/null) -gt 0 ]]
}
pedir GET "$SPA/api/usuarios/sesion" "Bearer $token"
verificar 4 "GET /api/usuarios/sesion con el token: autor.prueba con sus roles (recibido: $estado)" tiene_los_roles

# 5. Sin token, el 401 uniforme del API Gateway (ADR-014)
es_401_uniforme() {
    [[ $estado == 401 ]] && [[ $(jq -r '.codigo' <<<"$cuerpo" 2>/dev/null) == SEGURIDAD_NO_AUTENTICADO ]]
}
pedir GET "$SPA/api/usuarios/sesion"
verificar 5 "GET /api/usuarios/sesion sin token: 401 SEGURIDAD_NO_AUTENTICADO (recibido: $estado)" es_401_uniforme

if [[ $fallas -eq 0 ]]; then
    echo "SPA de punta a punta: todos los pasos pasan."
    exit 0
fi
echo "SPA de punta a punta: $fallas paso(s) fallan."
exit 1
