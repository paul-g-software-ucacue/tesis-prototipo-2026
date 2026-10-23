#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# La estructura orgánica de punta a punta sobre el perfil levantado (F03-H01; quickstart.md de
# specs/017-estructura-organica, research §10).
# Uso, desde cualquier directorio: deploy/probar_estructura.sh
# El Administrador registra las 24 unidades de la carga 01 de los datos de prueba (ficticias, V-02),
# con un prefijo al azar en cada código para no chocar con otras; si la base ya tiene su sede matriz,
# cuelga la estructura de ella, porque hay una sola. Una gestión fuera de las 21 y una comisión sin
# «permanente» se rechazan, un padre que forma un ciclo también, y una segunda sede matriz no entra.
# Una unidad con dependientes activos no se inactiva; una hoja sí, y se reactiva. El Vicerrector la
# lee sin poder escribir y un docente no la ve (D-70). El registro de Académico tiene las acciones.
# Al terminar, aunque un paso falle, borra en la base solo las unidades que creó y sus registros: la
# API no borra unidades (principio VII).
# Termina con 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime el token ni la
# contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly CUERPO_403='{"titulo":"Acceso no autorizado","descripcion":"No tienes permiso para esta acción. Si crees que deberías tenerlo, pide al administrador que revise tus roles.","codigo":"SEGURIDAD_ACCESO_NO_AUTORIZADO"}'

# La carga 01 de los datos de prueba: código, nombre, tipo, padre, gestión autorizada y permanente.
readonly CARGA_01='MATRIZ|Sede matriz|SEDE_MATRIZ|||
OCS|Órgano Colegiado Superior|GOBIERNO|MATRIZ||
RECT|Rectorado|GOBIERNO|MATRIZ|G01|
VRA|Vicerrectorado Académico|GOBIERNO|RECT|G02|
CAI|Coordinación Académica Institucional|UNIDAD_ACADEMICA|VRA||
CC-CONT|Coordinación de la carrera de Contabilidad|UNIDAD_ACADEMICA|CAI||
CC-DS|Coordinación de la carrera de Desarrollo De Software|UNIDAD_ACADEMICA|CAI||
CC-ELE|Coordinación de la carrera de Electricidad|UNIDAD_ACADEMICA|CAI||
CC-MA|Coordinación de la carrera de Mecanica Automotriz|UNIDAD_ACADEMICA|CAI||
CC-CYGOC|Coordinación de la carrera de Construccion Y Gestion De Obra Civil|UNIDAD_ACADEMICA|CAI||
CC-PP|Coordinación de la carrera de Produccion Pecuaria|UNIDAD_ACADEMICA|CAI||
CC-TSCIOR|Coordinación de la carrera de Tecnologia Superior En Control De Incendios Y Operaciones De Rescate|UNIDAD_ACADEMICA|CAI||
G-VIN|Gestión de Vinculación con la Sociedad|GESTION|VRA|G06|
G-INV|Gestión de Investigación, Desarrollo Tecnológico e Innovación|GESTION|VRA|G07|
G-CAL|Coordinación de Aseguramiento de la Calidad|GESTION|RECT|G10|
G-SEC|Secretaría General Académica|GESTION|RECT|G11|
G-BIE|Gestión de Bienestar Institucional|GESTION|VRA|G13|
G-PLA|Gestión de Planificación Estratégica|GESTION|RECT|G15|
G-BIB|Unidad de Servicios de Biblioteca|GESTION|VRA|G17|
G-TIC|Unidad de TIC|GESTION|VRA|G20|
G-EST|Unidad de Estadísticas|GESTION|VRA|G21|
COM-EVAL|Comisión de Evaluación Interna|COMISION|G-CAL||true
COM-INV|Comisión de Investigación|COMISION|G-INV||true
COM-DIST|Comisión de elaboración del distributivo 2026-I|COMISION|VRA||false'

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
    estado=$(curl "${argumentos[@]}" || echo '000')
    cuerpo=$(cat "$archivo")
    rm -f "$archivo"
}

# iniciar <usuario>: deja el token en $token, o vacío si no entró.
iniciar() {
    pedir POST "$GATEWAY/api/usuarios/sesion" "" \
        "$(jq -cn --arg u "$1" --arg c "$(valor_de_entorno MDD_USUARIOS_DEMO_CLAVE)" '{usuario: $u, clave: $c}')"
    token=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
}

# psql en la base del perfil; imprime el resultado sin alineación ni encabezados.
consultar_base() {
    docker compose exec -T postgres psql -q -At -U postgres -d "$(valor_de_entorno POSTGRES_DB)" \
        -v ON_ERROR_STOP=1 -c "$1"
}

es_403_comun() {
    [[ $estado == 403 && $cuerpo == "$CUERPO_403" ]]
}

es_estado() {
    [[ $estado == "$1" ]]
}

es_codigo() {  # es_codigo <estado> <código del catálogo>
    [[ $estado == "$1" && $(jq -r '.codigo' <<<"$cuerpo" 2>/dev/null) == "$2" ]]
}

es_invalido_en() {  # es_invalido_en <campo>: 400 con un solo detalle, en ese campo
    [[ $estado == 400 ]] \
        && [[ $(jq -r '.codigo' <<<"$cuerpo" 2>/dev/null) == COMUN_SOLICITUD_INVALIDA ]] \
        && [[ $(jq -r '[.detalles[].campo] | join(",")' <<<"$cuerpo") == "$1" ]]
}

# Las unidades de prueba llevan este prefijo: P y cuatro dígitos, hasta 15 caracteres en total.
prefijo="P$(printf '%04d' $((RANDOM % 10000)))-"
cod() { printf '%s%s' "$prefijo" "$1"; }

limpiar() {
    consultar_base "delete from academico.registro_cambio where detalle ->> 'codigo' like '$prefijo%';
delete from academico.unidad_organica where codigo like '$prefijo%';" >/dev/null 2>&1 \
        || echo "AVISO  No se pudieron borrar las unidades $prefijo*"
}
trap limpiar EXIT

iniciar admin.demo
token_admin=$token

# La sede matriz que ya tenga la base, si la tiene: hay una sola (research §3).
pedir GET "$GATEWAY/api/academico/unidades" "Bearer $token_admin"
matriz_previa=$(jq -r '[.[] | select(.tipo == "SEDE_MATRIZ") | .codigo][0] // empty' <<<"$cuerpo" 2>/dev/null || true)
padre_de() {  # el código real del padre de una unidad de la carga
    if [[ $1 == MATRIZ && -n $matriz_previa ]]; then
        printf '%s' "$matriz_previa"
    else
        cod "$1"
    fi
}

# 1. Registrar las 24 unidades de la carga 01 (SC-002); con una sede matriz previa, 23
registradas=0
esperadas=0
while IFS='|' read -r codigo nombre tipo padre gestion permanente; do
    if [[ $tipo == SEDE_MATRIZ && -n $matriz_previa ]]; then
        continue
    fi
    esperadas=$((esperadas + 1))
    unidad=$(jq -cn --arg c "$(cod "$codigo")" --arg n "$nombre" --arg t "$tipo" \
        --arg p "$([[ -n $padre ]] && padre_de "$padre")" --arg g "$gestion" --arg s "$permanente" \
        '{codigo: $c, nombre: $n, tipo: $t}
        + (if $p != "" then {padre: $p} else {} end)
        + (if $g != "" then {gestionAutorizada: $g} else {} end)
        + (if $s != "" then {comisionPermanente: ($s == "true")} else {} end)')
    pedir POST "$GATEWAY/api/academico/unidades" "Bearer $token_admin" "$unidad"
    if [[ $estado == 201 ]]; then
        registradas=$((registradas + 1))
    else
        echo "       $(cod "$codigo"): $estado $cuerpo"
    fi
done <<<"$CARGA_01"
todas_registradas() { [[ $registradas == "$esperadas" ]]; }
verificar 1 "admin.demo registra $registradas de $esperadas unidades de la carga 01${matriz_previa:+ (debajo de la sede matriz $matriz_previa)}" \
    todas_registradas
pedir GET "$GATEWAY/api/academico/unidades" "Bearer $token_admin"
jerarquia=$(jq -r --arg p "$prefijo" '[.[] | select(.codigo | startswith($p))
    | "\(.codigo | ltrimstr($p))>\(.padre // "" | ltrimstr($p))"] | sort | join(" ")' <<<"$cuerpo" 2>/dev/null || true)
esperada=$(while IFS='|' read -r codigo _ tipo padre _ _; do
    [[ $tipo == SEDE_MATRIZ && -n $matriz_previa ]] && continue
    if [[ $padre == MATRIZ && -n $matriz_previa ]]; then padre=$matriz_previa; fi
    printf '%s>%s\n' "$codigo" "$padre"
done <<<"$CARGA_01" | sort | paste -sd' ')
en_su_jerarquia() { [[ -n $jerarquia && $jerarquia == "$esperada" ]]; }
verificar 1 "La lista trae cada unidad de prueba con su padre" en_su_jerarquia

# 2. Una gestión fuera de las 21 y una comisión sin «permanente» (US2, US3, SC-003)
pedir POST "$GATEWAY/api/academico/unidades" "Bearer $token_admin" \
    "$(jq -cn --arg c "$(cod G-X)" --arg p "$(cod VRA)" \
        '{codigo: $c, nombre: "Gestión fuera del catálogo", tipo: "GESTION", padre: $p, gestionAutorizada: "G99"}')"
verificar 2 "Una gestión con G99: 400 en gestionAutorizada (recibido: $estado)" es_invalido_en gestionAutorizada
pedir POST "$GATEWAY/api/academico/unidades" "Bearer $token_admin" \
    "$(jq -cn --arg c "$(cod COM-X)" --arg p "$(cod VRA)" \
        '{codigo: $c, nombre: "Comisión sin indicación", tipo: "COMISION", padre: $p}')"
verificar 2 "Una comisión sin «permanente»: 400 en comisionPermanente (recibido: $estado)" \
    es_invalido_en comisionPermanente

# 3. Un padre que forma un ciclo (FR-003)
pedir PUT "$GATEWAY/api/academico/unidades/$(cod VRA)" "Bearer $token_admin" \
    "$(jq -cn --arg p "$(cod CC-DS)" '{nombre: "Vicerrectorado Académico", padre: $p, gestionAutorizada: "G02"}')"
verificar 3 "Poner VRA debajo de CC-DS, que depende de ella: 400 en padre (recibido: $estado)" es_invalido_en padre

# 4. Una segunda sede matriz
pedir POST "$GATEWAY/api/academico/unidades" "Bearer $token_admin" \
    "$(jq -cn --arg c "$(cod MATRIZ2)" '{codigo: $c, nombre: "Otra sede matriz", tipo: "SEDE_MATRIZ"}')"
verificar 4 "Una segunda sede matriz: 409 ACADEMICO_SEDE_MATRIZ_EXISTENTE (recibido: $estado)" \
    es_codigo 409 ACADEMICO_SEDE_MATRIZ_EXISTENTE

# 5. Inactivar con dependientes no; una hoja sí, y se reactiva
pedir PUT "$GATEWAY/api/academico/unidades/$(cod G-CAL)/estado" "Bearer $token_admin" '{"activa":false}'
verificar 5 "Inactivar G-CAL, de la que depende COM-EVAL: 409 ACADEMICO_UNIDAD_CON_DEPENDIENTES (recibido: $estado)" \
    es_codigo 409 ACADEMICO_UNIDAD_CON_DEPENDIENTES
pedir PUT "$GATEWAY/api/academico/unidades/$(cod COM-EVAL)/estado" "Bearer $token_admin" '{"activa":false}'
inactiva() { [[ $estado == 200 && $(jq -r '.activa' <<<"$cuerpo" 2>/dev/null) == false ]]; }
verificar 5 "Inactivar COM-EVAL: 200 inactiva (recibido: $estado)" inactiva
pedir PUT "$GATEWAY/api/academico/unidades/$(cod COM-EVAL)/estado" "Bearer $token_admin" '{"activa":true}'
activa() { [[ $estado == 200 && $(jq -r '.activa' <<<"$cuerpo" 2>/dev/null) == true ]]; }
verificar 5 "Reactivar COM-EVAL: 200 activa (recibido: $estado)" activa

# 6. Quién la ve (D-70): el Vicerrector la lee sin escribir; un docente no la ve
iniciar vicerrectorado.demo
token_vicerrector=$token
pedir GET "$GATEWAY/api/academico/unidades/$(cod RECT)" "Bearer $token_vicerrector"
con_g01() { [[ $estado == 200 && $(jq -r '.gestionAutorizada' <<<"$cuerpo" 2>/dev/null) == G01 ]]; }
verificar 6 "vicerrectorado.demo consulta RECT: 200 con G01 (recibido: $estado)" con_g01
pedir GET "$GATEWAY/api/academico/gestiones-autorizadas" "Bearer $token_vicerrector"
veintiuna() { [[ $estado == 200 && $(jq -r 'length' <<<"$cuerpo" 2>/dev/null) == 21 ]]; }
verificar 6 "vicerrectorado.demo lista las gestiones autorizadas: 200 con 21 (recibido: $estado)" veintiuna
pedir POST "$GATEWAY/api/academico/unidades" "Bearer $token_vicerrector" \
    "$(jq -cn --arg c "$(cod OTRA)" --arg p "$(cod VRA)" '{codigo: $c, nombre: "Otra", tipo: "GOBIERNO", padre: $p}')"
verificar 6 "vicerrectorado.demo registra una unidad: 403 común (recibido: $estado)" es_403_comun
iniciar docente.demo
pedir GET "$GATEWAY/api/academico/unidades" "Bearer $token"
verificar 6 "docente.demo lista la estructura: 403 común (recibido: $estado)" es_403_comun

# 7. El registro de Académico (FR-009, SC-004)
acciones=$(consultar_base "select accion || ':' || usuario || ':' || count(*) from academico.registro_cambio
    where detalle ->> 'codigo' like '$prefijo%' group by accion, usuario order by accion" 2>/dev/null \
    | paste -sd' ' || true)
registro_completo() {
    [[ $acciones == "UNIDAD_INACTIVADA:admin.demo:1 UNIDAD_REACTIVADA:admin.demo:1 UNIDAD_REGISTRADA:admin.demo:$esperadas" ]]
}
verificar 7 "El registro tiene $esperadas UNIDAD_REGISTRADA, 1 UNIDAD_INACTIVADA y 1 UNIDAD_REACTIVADA de admin.demo (recibido: ${acciones:-?})" \
    registro_completo

if [[ $fallas -eq 0 ]]; then
    echo "Estructura orgánica de punta a punta: todos los pasos pasan."
    exit 0
fi
echo "Estructura orgánica de punta a punta: $fallas paso(s) fallan."
exit 1
