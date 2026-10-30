#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# El orden de las 10 cargas, su estado y la confirmación «sin proyectos» de punta a punta sobre el
# perfil levantado (F01-H05; quickstart.md de specs/026-orden-de-cargas).
# Uso, desde cualquier directorio: deploy/probar_orden_cargas.sh
# Con una base sin estructura orgánica, carreras ni proyectos: la carga 04 antes que la 02 se rechaza
# con CARGAS_CARGAS_ANTERIORES_PENDIENTES, sin entrada en el historial; el estado dice qué falta; las
# cargas 01 a 08 de prueba (ficticias, RNF-SEG-06) entran en orden; el periodo 2026-II se confirma
# «sin proyectos» una sola vez y la carga 9 queda así; con los proyectos de prueba, la 9 queda cargada
# y la confirmación deja de valer; un periodo que no existe o mal escrito se rechaza; y quién puede
# ver el estado y confirmar (D-70, D-71). La política (carga 07) entra como una versión nueva, con un
# código con prefijo al azar, para no tocar la versión 1. Si la base ya tiene alguno de esos datos, el
# script falla sin tocar nada. Al terminar, aunque un paso falle, borra en la base solo lo que creó y
# sus registros. Termina con 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime el
# token ni la contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly DATOS=backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba
readonly CUERPO_403='{"titulo":"Acceso no autorizado","descripcion":"No tienes permiso para esta acción. Si crees que deberías tenerlo, pide al administrador que revise tus roles.","codigo":"SEGURIDAD_ACCESO_NO_AUTORIZADO"}'
readonly CARGAS=(estructura_organica carreras ciclos_academicos asignaturas_malla oferta_periodo
    docentes politica_carga_horaria cargos_autoridades proyectos_practicas matriculados_carrera)

carpeta=$(mktemp -d)

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
        echo "       última respuesta: $estado ${cuerpo:0:200}" | tr -d '\r'
        fallas=$((fallas + 1))
    fi
}

# Deja el estado en $estado y el cuerpo en $cuerpo.
pedir() {  # pedir <método> <url> [autorización] [cuerpo json]
    local archivo
    archivo=$(mktemp -p "$carpeta")
    local argumentos=(-s -o "$archivo" -w '%{http_code}' -X "$1" "$2")
    [[ -n ${3:-} ]] && argumentos+=(-H "Authorization: $3")
    [[ -n ${4:-} ]] && argumentos+=(-H 'Content-Type: application/json' --data "$4")
    estado=$(curl "${argumentos[@]}" || echo '000')
    cuerpo=$(cat "$archivo")
}

# subir <carga> <archivo>: valida el archivo y deja la respuesta.
subir() {
    local archivo
    archivo=$(mktemp -p "$carpeta")
    estado=$(curl -s -o "$archivo" -w '%{http_code}' -X POST -H "Authorization: Bearer $token" \
        -F "archivo=@$2;filename=$(basename "$2")" \
        "$GATEWAY/api/cargas/plantillas/$1/archivo" || echo '000')
    cuerpo=$(cat "$archivo")
}

# confirmar <carga> <archivo> <huella>: lo confirma y deja la respuesta.
confirmar() {
    local archivo
    archivo=$(mktemp -p "$carpeta")
    estado=$(curl -s -o "$archivo" -w '%{http_code}' -X POST -H "Authorization: Bearer $token" \
        -F "archivo=@$2;filename=$(basename "$2")" -F "huella=$3" \
        "$GATEWAY/api/cargas/plantillas/$1/archivo/confirmacion" || echo '000')
    cuerpo=$(cat "$archivo")
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

# Cada código de prueba nuevo lleva este prefijo: P y tres dígitos.
prefijo="P$(printf '%03d' $((RANDOM % 1000)))"
version="${prefijo}-POL-2024"

# --- Los archivos: los 10 de prueba, con la política con el código nuevo ---
python3 -I - "$DATOS" "$carpeta" "$version" <<'PY'
import csv, io, sys
datos, carpeta, version = sys.argv[1:4]

def leer(nombre):
    with open(f'{datos}/{nombre}', encoding='utf-8-sig', newline='') as archivo:
        filas = list(csv.reader(archivo))
    return filas[0], filas[1:]

def escribir(nombre, encabezados, filas):
    salida = io.StringIO()
    csv.writer(salida, lineterminator='\r\n').writerows([encabezados, *filas])
    with open(f'{carpeta}/{nombre}', 'w', encoding='utf-8', newline='') as archivo:
        archivo.write(salida.getvalue())

def lista(nombre, archivo, campo):
    encabezados, filas = leer(archivo)
    i = encabezados.index(campo)
    valores = sorted({fila[i] for fila in filas if fila[i]})
    with open(f'{carpeta}/{nombre}.lista', 'w') as salida:
        salida.write(','.join("'" + v + "'" for v in valores))

nombres = ['01_estructura_organica', '02_carreras', '03_ciclos_academicos', '04_asignaturas_malla',
           '05_oferta_periodo', '06_docentes', '07_politica_carga_horaria', '08_cargos_autoridades',
           '09_proyectos_practicas', '10_matriculados_carrera']
for nombre in nombres:
    encabezados, filas = leer(nombre + '.csv')
    if nombre.startswith('07'):
        i = encabezados.index('codigo_version_politica')
        for fila in filas:
            fila[i] = version
    escribir(nombre + '.csv', encabezados, filas)

lista('unidades', '01_estructura_organica.csv', 'codigo_unidad')
lista('carreras', '02_carreras.csv', 'codigo_carrera')
lista('periodos', '03_ciclos_academicos.csv', 'codigo_periodo')
lista('docentes', '06_docentes.csv', 'numero_identificacion')
lista('proyectos', '09_proyectos_practicas.csv', 'codigo_proyecto')
PY
unidades=$(cat "$carpeta/unidades.lista")
carreras=$(cat "$carpeta/carreras.lista")
periodos=$(cat "$carpeta/periodos.lista")
docentes=$(cat "$carpeta/docentes.lista")
proyectos=$(cat "$carpeta/proyectos.lista")
inicio=$(consultar_base "select now()")

limpiar() {
    consultar_base "delete from docentes.proyecto where codigo in ($proyectos);
delete from docentes.cargo where docente in ($docentes);
delete from docentes.docente where identificacion in ($docentes);
delete from docentes.registro_cambio where momento >= '$inicio' and usuario in ('datos.demo', 'consolidacion.demo');
delete from academico.matricula where periodo in ($periodos);
delete from academico.asignatura_paralelo where periodo in ($periodos);
delete from academico.asignatura where carrera in ($carreras);
delete from academico.malla where carrera in ($carreras);
delete from academico.carrera where codigo in ($carreras);
delete from academico.periodo_academico where codigo in ($periodos);
delete from academico.unidad_organica where codigo in ($unidades);
delete from academico.registro_cambio where momento >= '$inicio' and usuario in ('datos.demo', 'consolidacion.demo');
delete from normativa.parametro_regla where version_id in (select id from normativa.version_normativa where codigo = '$version');
delete from normativa.regla where version_id in (select id from normativa.version_normativa where codigo = '$version');
delete from normativa.fila_carga_horaria where version_id in (select id from normativa.version_normativa where codigo = '$version');
delete from normativa.version_normativa where codigo = '$version';
delete from normativa.registro_cambio where momento >= '$inicio' and usuario in ('datos.demo', 'consolidacion.demo');
delete from cargas.historial_carga where momento >= '$inicio';
delete from roles.periodo_conocido where codigo in ($periodos);
delete from roles.registro_cambio where detalle ->> 'periodo' in ($periodos);
delete from roles.evento_recibido where id_evento in (select (serialized_event::jsonb ->> 'idEvento')::uuid
    from eventos_datos.event_publication where serialized_event::jsonb -> 'contenido' ->> 'codigo' in ($periodos));
delete from eventos_datos.event_publication where serialized_event::jsonb -> 'contenido' ->> 'codigo' in ($periodos);" >/dev/null 2>&1 \
        || echo "AVISO  No se pudo borrar lo creado por el script"
    rm -rf "$carpeta"
}

# --- 0. La base no tiene estructura, carreras, proyectos ni los datos de prueba ---
ya_estan=$(consultar_base "select (select count(*) from academico.unidad_organica where activa or codigo in ($unidades))
    + (select count(*) from academico.carrera where estado = 'VIGENTE' or codigo in ($carreras))
    + (select count(*) from academico.periodo_academico where codigo in ($periodos))
    + (select count(*) from docentes.docente where identificacion in ($docentes))
    + (select count(*) from docentes.proyecto)")
if [[ $ya_estan != 0 ]]; then
    echo "FALLA  0. La base ya tiene $ya_estan unidades, carreras, proyectos o datos de prueba: no se tocan."
    rm -rf "$carpeta"
    exit 1
fi
trap limpiar EXIT

iniciar datos.demo
if [[ -z $token ]]; then
    echo "FALLA  0. datos.demo no inicia sesión: ¿está levantado el perfil?"
    exit 1
fi
echo "OK     0. La base no tiene estructura, carreras ni proyectos; la política entra como la versión $version"

archivo_de() {  # archivo_de <carga>: el CSV de prueba de esa carga
    local i
    for i in "${!CARGAS[@]}"; do
        [[ ${CARGAS[$i]} == "$1" ]] && printf '%s/%02d_%s.csv' "$carpeta" $((i + 1)) "$1"
    done
}

# validar_y_confirmar <carga>: con el token de $token.
validar_y_confirmar() {
    local huella
    subir "$1" "$(archivo_de "$1")"
    huella=$(jq -r '.vistaPrevia.huella // empty' <<<"$cuerpo")
    [[ $estado == 200 && $(jq -r '.errores' <<<"$cuerpo") == 0 && -n $huella ]] \
        || { echo "       $1: la vista previa falló" >&2; return 1; }
    confirmar "$1" "$(archivo_de "$1")" "$huella"
    [[ $estado == 200 && $(jq -r '.resultado' <<<"$cuerpo") == APLICADA ]] \
        || { echo "       $1: la confirmación falló" >&2; return 1; }
}

# estado_de <carga>: el estado de esa carga en la última consulta, con sus periodos «sin proyectos».
estado_de() {
    jq -r --arg c "$1" '.cargas[] | select(.codigo == $c) | .estado + " " + (.periodosSinProyectos | join(","))' <<<"$estados" | xargs
}

consultar_estado() {
    pedir GET "$GATEWAY/api/cargas/estado" "Bearer $token"
    estados=$cuerpo
    [[ $estado == 200 ]]
}

sin_proyectos() {  # sin_proyectos <periodo json>: con el token de $token
    pedir POST "$GATEWAY/api/cargas/sin-proyectos" "Bearer $token" "$1"
}

entradas_desde_el_inicio() {
    consultar_base "select count(*) from cargas.historial_carga where momento >= '$inicio'"
}

asignaturas_antes_que_carreras() {
    subir asignaturas_malla "$(archivo_de asignaturas_malla)"
    [[ $estado == 409 && $(jq -r '.codigo' <<<"$cuerpo") == CARGAS_CARGAS_ANTERIORES_PENDIENTES \
        && $(jq -c '[.detalles[].campo]' <<<"$cuerpo") == '["carreras"]' \
        && $(entradas_desde_el_inicio) == 0 ]]
}

estado_inicial() {
    consultar_estado || return 1
    [[ $(estado_de estructura_organica) == PENDIENTE && $(estado_de carreras) == PENDIENTE \
        && $(estado_de politica_carga_horaria) == CARGADA \
        && $(jq -c '[.cargas[] | select(.codigo == "asignaturas_malla") | .faltan[].codigo]' <<<"$estados") == '["carreras"]' ]]
}

de_la_01_a_la_08() {
    local carga
    for carga in "${CARGAS[@]:0:8}"; do
        validar_y_confirmar "$carga" || return 1
    done
    consultar_estado || return 1
    for carga in "${CARGAS[@]:0:8}"; do
        [[ $(estado_de "$carga") == CARGADA ]] || { echo "       $carga: $(estado_de "$carga")" >&2; return 1; }
    done
    [[ $(estado_de proyectos_practicas) == PENDIENTE ]]
}

sin_proyectos_una_vez() {
    iniciar consolidacion.demo
    sin_proyectos '{"periodo":"2026-II"}'
    [[ $estado == 201 && $(jq -r '.resultado + " " + .periodo + " " + .usuario' <<<"$cuerpo") == "SIN_PROYECTOS 2026-II consolidacion.demo" ]] \
        || return 1
    sin_proyectos '{"periodo":"2026-II"}'
    [[ $estado == 200 ]] || return 1
    consultar_estado || return 1
    [[ $(estado_de proyectos_practicas) == "SIN_PROYECTOS 2026-II" \
        && $(consultar_base "select count(*) from cargas.historial_carga where resultado = 'SIN_PROYECTOS' and momento >= '$inicio'") == 1 ]]
}

con_los_proyectos() {
    iniciar datos.demo
    validar_y_confirmar proyectos_practicas || return 1
    validar_y_confirmar matriculados_carrera || return 1
    consultar_estado || return 1
    [[ $(jq -r '[.cargas[] | select(.estado == "CARGADA")] | length' <<<"$estados") == 10 \
        && $(estado_de proyectos_practicas) == CARGADA ]]
}

periodo_con_proyectos() {
    iniciar consolidacion.demo
    sin_proyectos '{"periodo":"2026-II"}'
    [[ $estado == 409 && $(jq -r '.codigo' <<<"$cuerpo") == CARGAS_PERIODO_CON_PROYECTOS ]]
}

periodos_invalidos() {
    sin_proyectos '{"periodo":"2027-I"}'
    [[ $estado == 400 && $(jq -r '.codigo' <<<"$cuerpo") == CARGAS_PERIODO_NO_ENCONTRADO ]] || return 1
    sin_proyectos '{"periodo":"2026-III"}'
    [[ $estado == 400 && $(jq -r '.codigo' <<<"$cuerpo") == COMUN_SOLICITUD_INVALIDA ]]
}

historial_de_proyectos() {
    pedir GET "$GATEWAY/api/cargas/plantillas/proyectos_practicas/historial" "Bearer $token"
    [[ $estado == 200 && $(jq -c '[.[].resultado]' <<<"$cuerpo") == '["APLICADA","SIN_PROYECTOS"]' \
        && $(jq -r '.[1].periodo' <<<"$cuerpo") == 2026-II ]]
}

quien_lee_y_quien_confirma() {
    iniciar coordinacion.demo
    pedir GET "$GATEWAY/api/cargas/estado" "Bearer $token"
    [[ $estado == 200 ]] || return 1
    sin_proyectos '{"periodo":"2026-I"}'
    [[ $estado == 403 && $cuerpo == "$CUERPO_403" ]] || return 1
    iniciar docente.demo
    pedir GET "$GATEWAY/api/cargas/estado" "Bearer $token"
    [[ $estado == 403 ]] || return 1
    sin_proyectos '{"periodo":"2026-I"}'
    [[ $estado == 403 && $cuerpo == "$CUERPO_403" ]]
}

verificar 1 "Con la base vacía, la 04 antes que la 02: 409 CARGAS_CARGAS_ANTERIORES_PENDIENTES (carreras), sin historial" asignaturas_antes_que_carreras
verificar 2 "El estado: estructura y carreras pendientes, a asignaturas le faltan carreras, la política cargada" estado_inicial
verificar 3 "Las cargas 01 a 08 de prueba en orden: las 8 cargadas y la 9 pendiente" de_la_01_a_la_08
verificar 4 "consolidacion.demo confirma 2026-II «sin proyectos»: 201, la segunda 200, y la 9 queda «sin proyectos»" sin_proyectos_una_vez
verificar 5 "Con la 09 y la 10 de prueba, las 10 quedan cargadas y «sin proyectos» deja de valer" con_los_proyectos
verificar 6 "2026-II «sin proyectos» con proyectos asignables: 409 CARGAS_PERIODO_CON_PROYECTOS" periodo_con_proyectos
verificar 7 "2027-I: 400 CARGAS_PERIODO_NO_ENCONTRADO; 2026-III: 400 COMUN_SOLICITUD_INVALIDA" periodos_invalidos
verificar 8 "El historial de la 9 tiene la carga y la confirmación «sin proyectos» con su periodo" historial_de_proyectos
verificar 9 "coordinacion.demo ve el estado pero no confirma; docente.demo, ni lo uno ni lo otro (403)" quien_lee_y_quien_confirma

if ((fallas > 0)); then
    echo "El orden de las cargas de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "El orden de las cargas de punta a punta: todos los pasos pasan."
