#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Las plantillas de carga de punta a punta sobre el perfil levantado (F01-H01; quickstart.md de
# specs/018-plantillas-carga, research §10).
# Uso, desde cualquier directorio: deploy/probar_plantillas.sh
# El Responsable de consolidación lista las 10 cargas del diccionario de datos con sus 115 columnas,
# descarga la plantilla de cada una (CSV en UTF-8 con BOM y una sola línea) y el diccionario de la
# estructura orgánica. Talento Humano (Cargas) y un coordinador (Distributivo, propio) también
# descargan; un gestor y un docente reciben 403 (D-70). Una carga que no existe da 404.
# No crea nada, así que no limpia. Termina con 0 si todos los pasos pasan y con 1 si alguno falla.
# Nunca imprime el token ni la contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly PLANTILLAS="$GATEWAY/api/cargas/plantillas"
readonly CUERPO_403='{"titulo":"Acceso no autorizado","descripcion":"No tienes permiso para esta acción. Si crees que deberías tenerlo, pide al administrador que revise tus roles.","codigo":"SEGURIDAD_ACCESO_NO_AUTORIZADO"}'
readonly BOM=$'\xef\xbb\xbf'

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

# Deja el estado en $estado, los encabezados en $encabezados y el cuerpo, tal cual, en $archivo.
pedir() {  # pedir <método> <url> [autorización] [cuerpo json]
    archivo=$(mktemp)
    local argumentos=(-s -o "$archivo" -D - -w '\n%{http_code}' -X "$1" "$2")
    [[ -n ${3:-} ]] && argumentos+=(-H "Authorization: $3")
    [[ -n ${4:-} ]] && argumentos+=(-H 'Content-Type: application/json' --data "$4")
    local salida
    salida=$(curl "${argumentos[@]}" || printf '\n000')
    estado=${salida##*$'\n'}
    encabezados=${salida%$'\n'*}
    cuerpo=$(cat "$archivo")
}

# iniciar <usuario>: deja el token en $token, o vacío si no entró.
iniciar() {
    pedir POST "$GATEWAY/api/usuarios/sesion" "" \
        "$(jq -cn --arg u "$1" --arg c "$(valor_de_entorno MDD_USUARIOS_DEMO_CLAVE)" '{usuario: $u, clave: $c}')"
    token=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
    rm -f "$archivo"
}

encabezado() {  # encabezado <nombre>: su valor, sin el retorno de carro
    grep -i "^$1:" <<<"$encabezados" | tail -1 | cut -d' ' -f2- | tr -d '\r'
}

es_403_comun() {
    [[ $estado == 403 && $cuerpo == "$CUERPO_403" ]]
}

es_codigo() {  # es_codigo <estado> <código del catálogo>
    [[ $estado == "$1" && $(jq -r '.codigo' <<<"$cuerpo" 2>/dev/null) == "$2" ]]
}

# --- 1. El diccionario de las 10 cargas ---
diccionario_completo() {
    [[ $estado == 200 ]] || return 1
    [[ $(jq '.cargas | length' <<<"$cuerpo") == 10 ]] || return 1
    [[ $(jq -c '[.cargas[].numero]' <<<"$cuerpo") == '[1,2,3,4,5,6,7,8,9,10]' ]] || return 1
    [[ $(jq '[.cargas[].columnas[]] | length' <<<"$cuerpo") == 115 ]] || return 1
    # Cada columna con tipo, obligatoriedad y un ejemplo.
    [[ $(jq '[.cargas[].columnas[] | select(.tipo == "" or .obligatorio == "" or .ejemplo == "")] | length' <<<"$cuerpo") == 0 ]] || return 1
    [[ $(jq '.convenciones | length' <<<"$cuerpo") == 13 ]]
}

# --- 2. La plantilla de cada carga ---
plantillas_completas() {
    local carga codigo nombre columnas
    while IFS='|' read -r codigo nombre columnas; do
        pedir GET "$PLANTILLAS/$codigo/plantilla" "Bearer $token"
        carga="$codigo"
        [[ $estado == 200 ]] || { echo "       $carga: $estado" >&2; rm -f "$archivo"; return 1; }
        [[ $(encabezado Content-Type) == 'text/csv;charset=UTF-8' ]] || { rm -f "$archivo"; return 1; }
        [[ $(encabezado Content-Disposition) == "attachment; filename=\"$nombre\"" ]] || { rm -f "$archivo"; return 1; }
        [[ $(head -c 3 "$archivo") == "$BOM" ]] || { rm -f "$archivo"; return 1; }
        # Una sola línea, terminada en CRLF, con las columnas de la carga.
        [[ $(tr -cd '\n' <"$archivo" | wc -c) == 1 ]] || { rm -f "$archivo"; return 1; }
        [[ $(tail -c +4 "$archivo" | tr -d '\r' | awk -F, '{print NF}') == "$columnas" ]] || { rm -f "$archivo"; return 1; }
        rm -f "$archivo"
    done < <(jq -r '.cargas[] | "\(.codigo)|\(.archivo)|\(.columnas | length)"' <<<"$diccionario")
}

# --- 3. El diccionario de la estructura orgánica ---
diccionario_de_la_estructura() {
    [[ $estado == 200 ]] || return 1
    [[ $(encabezado Content-Disposition) == 'attachment; filename="01_estructura_organica_diccionario.csv"' ]] || return 1
    [[ $(head -c 3 "$archivo") == "$BOM" ]] || return 1
    local lineas
    lineas=$(tail -c +4 "$archivo" | tr -d '\r')
    # El encabezado, las 7 columnas, una fila vacía, tema y regla, y las 13 convenciones.
    [[ $(wc -l <<<"$lineas") == 23 ]] || return 1
    [[ $(sed -n 1p <<<"$lineas") == 'numero,campo,descripcion,tipo,longitud_maxima,obligatorio,formato_o_valores,clave,validacion,ejemplo' ]] || return 1
    [[ $(sed -n 2p <<<"$lineas") == 1,codigo_unidad,* ]] || return 1
    grep -q '^5,codigo_gestion_autorizada,.*G01 Rector; G02 Vicerrector' <<<"$lineas" || return 1
    [[ -z $(sed -n 9p <<<"$lineas") && $(sed -n 10p <<<"$lineas") == 'tema,regla' ]]
}

descarga() {  # descarga <usuario>: 200 con la plantilla de docentes
    iniciar "$1"
    [[ -n $token ]] || return 1
    pedir GET "$PLANTILLAS/docentes/plantilla" "Bearer $token"
    rm -f "$archivo"
    [[ $estado == 200 ]]
}

sin_permiso() {  # sin_permiso <usuario>: 403 común al listar y al descargar
    iniciar "$1"
    [[ -n $token ]] || return 1
    pedir GET "$PLANTILLAS" "Bearer $token"
    rm -f "$archivo"
    es_403_comun || return 1
    pedir GET "$PLANTILLAS/docentes/diccionario" "Bearer $token"
    rm -f "$archivo"
    es_403_comun
}

iniciar consolidacion.demo
if [[ -z $token ]]; then
    echo "FALLA  0. consolidacion.demo no inicia sesión: ¿está levantado el perfil?"
    exit 1
fi
pedir GET "$PLANTILLAS" "Bearer $token"
rm -f "$archivo"
diccionario=$cuerpo
verificar 1 "consolidacion.demo lista las 10 cargas: 115 columnas con tipo, obligatoriedad y ejemplo, y 13 convenciones" diccionario_completo
verificar 2 "Descarga la plantilla de cada carga: text/csv, su nombre, el BOM y una sola línea con sus columnas" plantillas_completas
pedir GET "$PLANTILLAS/estructura_organica/diccionario" "Bearer $token"
verificar 3 "El diccionario de la estructura orgánica: 7 columnas, las 21 gestiones y las convenciones" diccionario_de_la_estructura
rm -f "$archivo"
verificar 4 "datos.demo (Cargas) descarga" descarga datos.demo
verificar 5 "coordinacion.demo (Distributivo, propio) descarga" descarga coordinacion.demo
verificar 6 "gestion.demo recibe 403 al listar y al descargar" sin_permiso gestion.demo
verificar 7 "docente.demo recibe 403 al listar y al descargar" sin_permiso docente.demo
iniciar consolidacion.demo
pedir GET "$PLANTILLAS/comisiones/plantilla" "Bearer $token"
rm -f "$archivo"
verificar 8 "Una carga que no existe da 404 COMUN_RECURSO_NO_ENCONTRADO" es_codigo 404 COMUN_RECURSO_NO_ENCONTRADO

if ((fallas > 0)); then
    echo "Plantillas de carga de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "Plantillas de carga de punta a punta: todos los pasos pasan."
