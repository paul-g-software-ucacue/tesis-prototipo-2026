#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Los periodos académicos de punta a punta sobre el perfil levantado (F02-H01; quickstart.md de
# specs/015-abrir-periodo, research §11).
# Uso, desde cualquier directorio: deploy/probar_periodos.sh
# El Vicerrector académico registra un periodo de prueba, que queda en Planificación sin versión y
# no puede avanzar; crea una versión de normativa a partir de la 1, abre el periodo con ella, y desde
# entonces esa versión no se edita y el periodo no acepta otra. El estado no salta, avanza hasta
# Cerrado y, cerrado, el periodo no cambia. El Rector lo ve sin poder escribir y un docente no lo ve
# (D-69). El registro de Académico tiene las cuatro acciones. Al terminar, aunque un paso falle,
# borra en la base solo el periodo de prueba y la versión que creó, con sus registros: la API no
# borra periodos ni versiones (principio VII).
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

# Deja el estado en $estado, el cuerpo en $cuerpo y los segundos que tardó en $tiempo.
pedir() {  # pedir <método> <url> [autorización] [cuerpo json]
    local archivo salida
    archivo=$(mktemp)
    local argumentos=(-s -o "$archivo" -w '%{http_code} %{time_total}' -X "$1" "$2")
    [[ -n ${3:-} ]] && argumentos+=(-H "Authorization: $3")
    [[ -n ${4:-} ]] && argumentos+=(-H 'Content-Type: application/json' --data "$4")
    salida=$(curl "${argumentos[@]}" || echo '000 0')
    read -r estado tiempo <<<"$salida"
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

# El periodo de la respuesta, con estado y versión: es_periodo <estado http> <estado> <versión o vacío>
es_periodo() {
    [[ $estado == "$1" ]] \
        && [[ $(jq -r '.codigo' <<<"$cuerpo" 2>/dev/null) == "$periodo" ]] \
        && [[ $(jq -r '.estado' <<<"$cuerpo") == "$2" ]] \
        && [[ $(jq -r '.version // ""' <<<"$cuerpo") == "$3" ]]
}

# El cuerpo de editarVersion a partir de una versión, sin cambios: basta para saber si se puede
# editar (F04-H02).
edicion_de() {
    jq -c '{codigo, origen, vigenteDesde,
        reglas: [.reglas[] | {identificador, parametros}],
        tablaCargaHoraria: [.tablaCargaHoraria[] | del(.severidad, .referencia, .observacion)]}' <<<"$1"
}

# La limpieza: solo el periodo de prueba y la versión que este script creó, con sus filas, reglas,
# parámetros y registros. Corre al salir, aunque falle un paso.
periodo="prueba-$RANDOM$RANDOM"
version_creada=""
limpiar() {
    local sql
    sql="delete from academico.registro_cambio where detalle ->> 'codigo' = '$periodo';
delete from academico.periodo_academico where codigo = '$periodo';
delete from normativa.asignacion_normativa where periodo = '$periodo';
delete from normativa.registro_cambio where accion = 'VERSION_ASOCIADA'
    and detalle ->> 'periodo' = '$periodo';"
    if [[ $version_creada =~ ^[0-9]+$ ]]; then
        sql+="
delete from normativa.parametro_regla where version_id =
    (select id from normativa.version_normativa where numero = $version_creada);
delete from normativa.regla where version_id =
    (select id from normativa.version_normativa where numero = $version_creada);
delete from normativa.fila_carga_horaria where version_id =
    (select id from normativa.version_normativa where numero = $version_creada);
delete from normativa.registro_cambio where (detalle ->> 'numero')::int = $version_creada;
delete from normativa.version_normativa where numero = $version_creada;"
    fi
    consultar_base "$sql" >/dev/null 2>&1 \
        || echo "AVISO  No se pudo limpiar el periodo $periodo ni la versión ${version_creada:-}"
}
trap limpiar EXIT

iniciar vicerrectorado.demo
token_vicerrector=$token

# 1. Registrar: el periodo de prueba queda en Planificación, sin versión (FR-001)
pedir POST "$GATEWAY/api/academico/periodos" "Bearer $token_vicerrector" \
    "$(jq -cn --arg c "$periodo" '{codigo: $c, nombre: "Periodo de prueba", inicio: "2026-09-01", fin: "2027-02-28"}')"
verificar 1 "vicerrectorado.demo registra $periodo: 201 en Planificación, sin versión (recibido: $estado)" \
    es_periodo 201 PLANIFICACION ""

# 2. Sin versión, no sale de Planificación (FR-004)
pedir PUT "$GATEWAY/api/academico/periodos/$periodo/estado" "Bearer $token_vicerrector" \
    '{"estado":"EN_APROBACION"}'
verificar 2 "Pasar a En aprobación sin versión: 409 ACADEMICO_PERIODO_SIN_VERSION (recibido: $estado)" \
    es_codigo 409 ACADEMICO_PERIODO_SIN_VERSION

# 3. Abrir el periodo (escenario 1, SC-002): con una versión nueva, copiada de la 1; Normativa dice
# lo mismo que Académico
pedir POST "$GATEWAY/api/normativa/versiones" "Bearer $token_vicerrector" \
    "$(jq -cn --arg c "PRUEBA-${periodo#prueba-}" \
        '{copiarDe: 1, codigo: $c, origen: "Prueba de punta a punta", vigenteDesde: "2026-01-01"}')"
version_creada=$(jq -r '.numero // empty' <<<"$cuerpo" 2>/dev/null || true)
creada=$cuerpo
verificar 3 "Crear la versión ${version_creada:-?} a partir de la 1: 201 (recibido: $estado)" es_estado 201
pedir PUT "$GATEWAY/api/academico/periodos/$periodo/normativa" "Bearer $token_vicerrector" \
    "{\"version\":${version_creada:-0}}"
tiempo_abrir=$tiempo
verificar 3 "Abrir $periodo con la versión ${version_creada:-?}: 200 (recibido: $estado, ${tiempo_abrir} s)" \
    es_periodo 200 PLANIFICACION "$version_creada"
pedir GET "$GATEWAY/api/normativa/periodos/$periodo" "Bearer $token_vicerrector"
rige_la_creada() { [[ $estado == 200 && $(jq '.version' <<<"$cuerpo") == "$version_creada" ]]; }
verificar 3 "Normativa dice que la versión ${version_creada:-?} rige $periodo (recibido: $estado)" rige_la_creada
pedir GET "$GATEWAY/api/academico/periodos" "Bearer $token_vicerrector"
tiempo_listar=$tiempo
en_la_lista() {
    [[ $estado == 200 ]] \
        && [[ $(jq -r --arg p "$periodo" '.[] | select(.codigo == $p) | .version' <<<"$cuerpo") \
            == "$version_creada" ]]
}
verificar 3 "La lista de periodos lo muestra con su versión (recibido: $estado, ${tiempo_listar} s)" en_la_lista

# 4. La versión ya no se edita, y el periodo no acepta otra (FR-003)
pedir PUT "$GATEWAY/api/normativa/versiones/${version_creada:-0}" "Bearer $token_vicerrector" \
    "$(edicion_de "$creada")"
verificar 4 "Editar la versión ${version_creada:-?}: 409 NORMATIVA_VERSION_EN_USO (recibido: $estado)" \
    es_codigo 409 NORMATIVA_VERSION_EN_USO
pedir PUT "$GATEWAY/api/academico/periodos/$periodo/normativa" "Bearer $token_vicerrector" '{"version":1}'
verificar 4 "Abrir $periodo con la versión 1: 409 ACADEMICO_PERIODO_CON_VERSION (recibido: $estado)" \
    es_codigo 409 ACADEMICO_PERIODO_CON_VERSION

# 5. El estado no salta (escenario 2, SC-003)
pedir PUT "$GATEWAY/api/academico/periodos/$periodo/estado" "Bearer $token_vicerrector" '{"estado":"VIGENTE"}'
verificar 5 "Saltar de Planificación a Vigente: 409 ACADEMICO_TRANSICION_NO_PERMITIDA (recibido: $estado)" \
    es_codigo 409 ACADEMICO_TRANSICION_NO_PERMITIDA

# 6. Corregir el nombre y avanzar paso a paso hasta Cerrado
pedir PUT "$GATEWAY/api/academico/periodos/$periodo" "Bearer $token_vicerrector" \
    '{"nombre":"Periodo de prueba corregido","inicio":"2026-09-01","fin":"2027-02-28"}'
corregido() { [[ $estado == 200 && $(jq -r '.nombre' <<<"$cuerpo") == "Periodo de prueba corregido" ]]; }
verificar 6 "Corregir el nombre en Planificación: 200 (recibido: $estado)" corregido
for destino in EN_APROBACION VIGENTE CERRADO; do
    pedir PUT "$GATEWAY/api/academico/periodos/$periodo/estado" "Bearer $token_vicerrector" \
        "{\"estado\":\"$destino\"}"
    verificar 6 "Pasar a $destino: 200 (recibido: $estado)" es_periodo 200 "$destino" "$version_creada"
done

# 7. Cerrado, no cambia (escenario 3, SC-004)
pedir PUT "$GATEWAY/api/academico/periodos/$periodo" "Bearer $token_vicerrector" \
    '{"nombre":"Otro nombre","inicio":"2026-09-01","fin":"2027-02-28"}'
verificar 7 "Corregir el periodo cerrado: 409 ACADEMICO_PERIODO_CERRADO (recibido: $estado)" \
    es_codigo 409 ACADEMICO_PERIODO_CERRADO
pedir PUT "$GATEWAY/api/academico/periodos/$periodo/estado" "Bearer $token_vicerrector" '{"estado":"VIGENTE"}'
verificar 7 "Volverlo a Vigente: 409 ACADEMICO_PERIODO_CERRADO (recibido: $estado)" \
    es_codigo 409 ACADEMICO_PERIODO_CERRADO
pedir GET "$GATEWAY/api/academico/periodos/$periodo" "Bearer $token_vicerrector"
sigue_igual() {
    es_periodo 200 CERRADO "$version_creada" \
        && [[ $(jq -r '.nombre' <<<"$cuerpo") == "Periodo de prueba corregido" ]]
}
verificar 7 "Sigue cerrado, con su nombre y su versión (recibido: $estado)" sigue_igual

# 8. Quién lo ve (D-69): el Rector lo lee sin escribir; un docente no lo ve
iniciar rectorado.demo
pedir GET "$GATEWAY/api/academico/periodos/$periodo" "Bearer $token"
verificar 8 "rectorado.demo lo consulta: 200 (recibido: $estado)" es_periodo 200 CERRADO "$version_creada"
pedir POST "$GATEWAY/api/academico/periodos" "Bearer $token" \
    '{"codigo":"no-debe","nombre":"No debe","inicio":"2026-09-01","fin":"2027-02-28"}'
verificar 8 "rectorado.demo no registra periodos: 403 con el cuerpo común (recibido: $estado)" es_403_comun
iniciar docente.demo
pedir GET "$GATEWAY/api/academico/periodos" "Bearer $token"
verificar 8 "docente.demo no lista periodos: 403 con el cuerpo común (recibido: $estado)" es_403_comun

# 9. El registro de Académico tiene las cuatro acciones, con el usuario (FR-008, SC-005)
acciones=$(consultar_base "select string_agg(accion || ':' || usuario, ' ' order by momento, accion)
    from academico.registro_cambio where detalle ->> 'codigo' = '$periodo'" 2>/dev/null || true)
registro_completo() {
    [[ $acciones == "PERIODO_REGISTRADO:vicerrectorado.demo PERIODO_ABIERTO:vicerrectorado.demo PERIODO_CORREGIDO:vicerrectorado.demo PERIODO_ESTADO_CAMBIADO:vicerrectorado.demo PERIODO_ESTADO_CAMBIADO:vicerrectorado.demo PERIODO_ESTADO_CAMBIADO:vicerrectorado.demo" ]]
}
verificar 9 "Registrado, abierto, corregido y 3 cambios de estado, por vicerrectorado.demo" registro_completo

if [[ $fallas -eq 0 ]]; then
    echo "Periodos de punta a punta: todos los pasos pasan."
    exit 0
fi
echo "Periodos de punta a punta: $fallas paso(s) fallan."
exit 1
