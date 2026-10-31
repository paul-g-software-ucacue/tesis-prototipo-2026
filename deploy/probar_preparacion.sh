#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# El tablero de preparación del periodo de punta a punta sobre el perfil levantado (F02-H02;
# quickstart.md de specs/027-tablero-preparacion).
# Uso, desde cualquier directorio: deploy/probar_preparacion.sh
# Con una base sin estructura orgánica, carreras ni proyectos: con las cargas 01 a 05 y 07 de prueba
# (ficticias, RNF-SEG-06), un archivo de docentes con errores y 2026-I «sin proyectos», el tablero de
# 2026-I no está listo, docentes está «con errores» y faltan docentes y cargos; con las 10, solo le
# falta abrirse; abierto con la política de prueba, está listo; 2026-II no tiene su oferta; un periodo
# que no existe o mal escrito se rechaza; un periodo registrado en su pantalla, con otro código que
# la carga 3, tiene su tablero y su «sin proyectos»; y quién lo ve (D-70). La política (carga 07) entra como una
# versión nueva, con un código con prefijo al azar, para no tocar la versión 1. Si la base ya tiene
# alguno de esos datos, el script falla sin tocar nada. Al terminar, aunque un paso falle, borra en la
# base solo lo que creó y sus registros, también la apertura de 2026-I. Termina con 0 si todos los
# pasos pasan y con 1 si alguno falla. Nunca imprime el token ni la contraseña.
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

# La 06 con una cédula de 9 dígitos en la primera fila: un error de sus filas (F01-H03).
encabezados, filas = leer('06_docentes.csv')
i = encabezados.index('numero_identificacion')
con_errores = [list(fila) for fila in filas]
con_errores[0][i] = con_errores[0][i][1:]
escribir('docentes_con_errores.csv', encabezados, con_errores)
PY
unidades=$(cat "$carpeta/unidades.lista")
carreras=$(cat "$carpeta/carreras.lista")
# Con el periodo que el paso 6 registra en la pantalla, para que también se borre.
periodos="$(cat "$carpeta/periodos.lista"),'$prefijo'"
docentes=$(cat "$carpeta/docentes.lista")
proyectos=$(cat "$carpeta/proyectos.lista")
inicio=$(consultar_base "select now()")

limpiar() {
    consultar_base "delete from docentes.proyecto where codigo in ($proyectos);
delete from docentes.cargo where docente in ($docentes);
delete from docentes.docente where identificacion in ($docentes);
delete from docentes.registro_cambio where momento >= '$inicio' and usuario in ('datos.demo', 'consolidacion.demo', 'vicerrectorado.demo');
delete from academico.matricula where periodo in ($periodos);
delete from academico.asignatura_paralelo where periodo in ($periodos);
delete from academico.asignatura where carrera in ($carreras);
delete from academico.malla where carrera in ($carreras);
delete from academico.carrera where codigo in ($carreras);
delete from academico.periodo_academico where codigo in ($periodos);
delete from academico.unidad_organica where codigo in ($unidades);
delete from academico.registro_cambio where momento >= '$inicio' and usuario in ('datos.demo', 'consolidacion.demo', 'vicerrectorado.demo');
delete from normativa.asignacion_normativa where periodo in ($periodos);
delete from normativa.parametro_regla where version_id in (select id from normativa.version_normativa where codigo = '$version');
delete from normativa.regla where version_id in (select id from normativa.version_normativa where codigo = '$version');
delete from normativa.fila_carga_horaria where version_id in (select id from normativa.version_normativa where codigo = '$version');
delete from normativa.version_normativa where codigo = '$version';
delete from normativa.registro_cambio where momento >= '$inicio' and usuario in ('datos.demo', 'consolidacion.demo', 'vicerrectorado.demo');
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

sin_proyectos() {  # sin_proyectos <periodo json>: con el token de $token
    pedir POST "$GATEWAY/api/cargas/sin-proyectos" "Bearer $token" "$1"
}

# tablero <periodo>: deja el tablero de ese periodo en $tablero, con el token de $token.
tablero() {
    pedir GET "$GATEWAY/api/cargas/preparacion?periodo=$1" "Bearer $token"
    tablero=$cuerpo
    [[ $estado == 200 ]]
}

estado_en() {  # estado_en <carga>: su estado en el último tablero
    jq -r --arg c "$1" '.cargas[] | select(.codigo == $c) | .estado' <<<"$tablero"
}

que_falta() {  # lo que falta en el último tablero, separado por comas
    jq -r '[.faltan[] | (.carga.codigo // .tipo)] | join(",")' <<<"$tablero"
}

sin_docentes_y_con_errores() {
    local carga
    for carga in estructura_organica carreras ciclos_academicos asignaturas_malla oferta_periodo politica_carga_horaria; do
        validar_y_confirmar "$carga" || return 1
    done
    confirmar docentes "$carpeta/docentes_con_errores.csv" "$(printf 'f%.0s' {1..64})"
    [[ $estado == 409 && $(jq -r '.codigo' <<<"$cuerpo") == CARGAS_CARGA_CON_ERRORES ]] \
        || { echo "       docentes con errores: $estado" >&2; return 1; }
    iniciar consolidacion.demo
    sin_proyectos '{"periodo":"2026-I"}'
    [[ $estado == 201 ]] || return 1
    iniciar vicerrectorado.demo
    tablero 2026-I || return 1
    [[ $(jq -r '.listo' <<<"$tablero") == false && $(estado_en docentes) == CON_ERRORES \
        && $(jq -r '.cargas[] | select(.codigo == "docentes") | .motivo' <<<"$tablero") == CARGAS_CARGA_CON_ERRORES \
        && $(estado_en proyectos_practicas) == SIN_PROYECTOS \
        && $(que_falta) == "docentes,cargos_autoridades,PERIODO_SIN_ABRIR" ]]
}

con_las_diez() {
    local carga
    iniciar datos.demo
    for carga in docentes cargos_autoridades proyectos_practicas matriculados_carrera; do
        validar_y_confirmar "$carga" || return 1
    done
    iniciar vicerrectorado.demo
    tablero 2026-I || return 1
    [[ $(jq -r '[.cargas[] | select(.estado == "CARGADA")] | length' <<<"$tablero") == 10 \
        && $(que_falta) == PERIODO_SIN_ABRIR && $(jq -r '.listo' <<<"$tablero") == false ]]
}

abierto_y_listo() {
    local numero
    numero=$(consultar_base "select numero from normativa.version_normativa where codigo = '$version'")
    pedir PUT "$GATEWAY/api/academico/periodos/2026-I/normativa" "Bearer $token" "{\"version\":${numero:-0}}"
    [[ $estado == 200 ]] || { echo "       abrir 2026-I: $estado" >&2; return 1; }
    tablero 2026-I || return 1
    [[ $(jq -r '.listo' <<<"$tablero") == true && $(que_falta) == "" \
        && $(jq -r '.abierto' <<<"$tablero") == true ]]
}

otro_periodo() {
    tablero 2026-II || return 1
    [[ $(estado_en oferta_periodo) == PENDIENTE && $(estado_en matriculados_carrera) == PENDIENTE \
        && $(que_falta) == "oferta_periodo,PERIODO_SIN_ABRIR" ]]
}

periodos_invalidos() {
    pedir GET "$GATEWAY/api/cargas/preparacion?periodo=2027-I" "Bearer $token"
    [[ $estado == 400 && $(jq -r '.codigo' <<<"$cuerpo") == CARGAS_PERIODO_NO_ENCONTRADO ]] || return 1
    pedir GET "$GATEWAY/api/cargas/preparacion?periodo=2026%20III" "Bearer $token"
    [[ $estado == 400 && $(jq -r '.codigo' <<<"$cuerpo") == COMUN_SOLICITUD_INVALIDA ]]
}

# Un periodo registrado en su pantalla, con otro código que la carga 3: como 003 en la demo.
codigo_de_la_pantalla() {
    pedir POST "$GATEWAY/api/academico/periodos" "Bearer $token" \
        '{"codigo":"'"$prefijo"'","nombre":"Periodo de la pantalla","inicio":"2028-04-03","fin":"2028-08-04"}'
    [[ $estado == 201 ]] || { echo "       registrar $prefijo: $estado" >&2; return 1; }
    tablero "$prefijo" || return 1
    [[ $(jq -r '.periodo' <<<"$tablero") == "$prefijo" ]] || return 1
    sin_proyectos '{"periodo":"'"$prefijo"'"}'
    [[ $estado == 201 ]] || { echo "       sin proyectos de $prefijo: $estado" >&2; return 1; }
    tablero "$prefijo" || return 1
    [[ $(estado_en proyectos_practicas) == SIN_PROYECTOS ]]
}

quien_lo_ve() {
    pedir GET "$GATEWAY/api/cargas/preparacion" "Bearer $token"
    [[ $estado == 200 ]] || return 1
    iniciar coordinacion.demo
    pedir GET "$GATEWAY/api/cargas/preparacion" "Bearer $token"
    [[ $estado == 200 ]] || return 1
    iniciar docente.demo
    pedir GET "$GATEWAY/api/cargas/preparacion" "Bearer $token"
    [[ $estado == 403 && $cuerpo == "$CUERPO_403" ]]
}

verificar 1 "Sin docentes, con un archivo de docentes con errores y 2026-I «sin proyectos»: no está listo, docentes «con errores» y faltan docentes, cargos y abrirlo" sin_docentes_y_con_errores
verificar 2 "Con las 10 cargas de prueba, 2026-I solo tiene pendiente abrirse" con_las_diez
verificar 3 "vicerrectorado.demo abre 2026-I con la versión $version: está listo" abierto_y_listo
verificar 4 "2026-II no tiene su oferta ni sus matriculados: le faltan la oferta y abrirse" otro_periodo
verificar 5 "2027-I: 400 CARGAS_PERIODO_NO_ENCONTRADO; «2026 III»: 400 COMUN_SOLICITUD_INVALIDA" periodos_invalidos
verificar 6 "Un periodo con otro código de Académico ($prefijo): su tablero y «sin proyectos»" codigo_de_la_pantalla
verificar 7 "coordinacion.demo lo ve y docente.demo recibe 403" quien_lo_ve

if ((fallas > 0)); then
    echo "El tablero de preparación de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "El tablero de preparación de punta a punta: todos los pasos pasan."
