#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# La vista previa y la confirmación de las 10 cargas de punta a punta sobre el perfil levantado
# (F01-H04; quickstart.md de specs/025-confirmar-carga).
# Uso, desde cualquier directorio: deploy/probar_confirmar_carga.sh
# Confirma en su orden las 10 cargas de prueba de la ERS (ficticias, RNF-SEG-06) con datos.demo: la
# política (carga 07) entra como una versión nueva, con un código con prefijo al azar, para no tocar
# la versión 1. Comprueba que, subidas otra vez, no tienen nuevos ni cambios; que confirmar dos veces
# el mismo archivo da REPETIDA; que una baja no se aplica; que una vista previa desactualizada no se
# aplica; que una regla del dueño (un segundo coordinador vigente, RN-12) rechaza la carga con su
# fila; el historial; y quién confirma (D-71). Los códigos y las cédulas de prueba son fijos: si
# alguno ya existe en la base, o ya hay una sede matriz, el script falla sin tocar nada. Al terminar,
# aunque un paso falle, borra en la base solo lo que creó y sus registros, también la copia de los
# periodos en Roles y sus eventos. Termina con 0 si todos los pasos pasan y con 1 si alguno falla.
# Nunca imprime el token ni la contraseña.
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

# --- Los archivos: los 10 de prueba, la política con el código nuevo y las variantes ---
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
lista('asignaturas', '04_asignaturas_malla.csv', 'codigo_asignatura')
lista('docentes', '06_docentes.csv', 'numero_identificacion')
lista('proyectos', '09_proyectos_practicas.csv', 'codigo_proyecto')

# La 01 con una unidad menos: la última, una hoja.
encabezados, filas = leer('01_estructura_organica.csv')
escribir('unidad_menos.csv', encabezados, filas[:-1])
# La 02 con el nombre de la primera carrera cambiado de dos formas.
encabezados, filas = leer('02_carreras.csv')
n = encabezados.index('nombre')
for sufijo in ('A', 'B'):
    otras = [list(fila) for fila in filas]
    otras[0][n] = otras[0][n] + ' ' + sufijo
    escribir(f'carreras_{sufijo}.csv', encabezados, otras)
# La 08 con un segundo coordinador vigente de la carrera del primero.
encabezados, filas = leer('08_cargos_autoridades.csv')
c = encabezados.index('codigo_cargo')
coordinadores = [fila for fila in filas if fila[c] == 'COORDINADOR_CARRERA']
segundo = list(coordinadores[0])
segundo[encabezados.index('numero_identificacion')] = coordinadores[1][encabezados.index('numero_identificacion')]
segundo[encabezados.index('fecha_designacion')] = '2026-05-01'
escribir('dos_coordinadores.csv', encabezados, [*filas, segundo])
PY
fila_del_segundo=$(($(wc -l <"$carpeta/dos_coordinadores.csv")))
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

# --- 0. La base no tiene nada de esto ---
ya_estan=$(consultar_base "select (select count(*) from academico.unidad_organica where codigo in ($unidades) or tipo = 'SEDE_MATRIZ')
    + (select count(*) from academico.carrera where codigo in ($carreras))
    + (select count(*) from academico.periodo_academico where codigo in ($periodos))
    + (select count(*) from docentes.docente where identificacion in ($docentes))
    + (select count(*) from docentes.proyecto where codigo in ($proyectos))")
if [[ $ya_estan != 0 ]]; then
    echo "FALLA  0. La base ya tiene $ya_estan datos de prueba o una sede matriz: no se tocan."
    rm -rf "$carpeta"
    exit 1
fi
trap limpiar EXIT

iniciar datos.demo
if [[ -z $token ]]; then
    echo "FALLA  0. datos.demo no inicia sesión: ¿está levantado el perfil?"
    exit 1
fi
echo "OK     0. La base no tiene los datos de prueba; la política entra como la versión $version"

archivo_de() {  # archivo_de <carga>: el CSV de prueba de esa carga
    local i
    for i in "${!CARGAS[@]}"; do
        [[ ${CARGAS[$i]} == "$1" ]] && printf '%s/%02d_%s.csv' "$carpeta" $((i + 1)) "$1"
    done
}

las_diez() {
    local carga huella
    for carga in "${CARGAS[@]}"; do
        subir "$carga" "$(archivo_de "$carga")"
        huella=$(jq -r '.vistaPrevia.huella // empty' <<<"$cuerpo")
        [[ $estado == 200 && $(jq -r '.errores' <<<"$cuerpo") == 0 && -n $huella ]] \
            || { echo "       $carga: la vista previa falló" >&2; return 1; }
        confirmar "$carga" "$(archivo_de "$carga")" "$huella"
        [[ $estado == 200 && $(jq -r '.resultado' <<<"$cuerpo") == APLICADA ]] \
            || { echo "       $carga: la confirmación falló" >&2; return 1; }
    done
}

sin_cambios() {
    local carga
    for carga in "${CARGAS[@]}"; do
        subir "$carga" "$(archivo_de "$carga")"
        [[ $(jq -r '.vistaPrevia.nuevos' <<<"$cuerpo") == 0 && $(jq -r '.vistaPrevia.cambian' <<<"$cuerpo") == 0 ]] \
            || { echo "       $carga: $(jq -c '.vistaPrevia | {nuevos, cambian, bajas}' <<<"$cuerpo")" >&2; return 1; }
    done
}

repetida() {
    subir carreras "$(archivo_de carreras)"
    confirmar carreras "$(archivo_de carreras)" "$(jq -r '.vistaPrevia.huella' <<<"$cuerpo")"
    [[ $estado == 200 && $(jq -r '.resultado' <<<"$cuerpo") == REPETIDA ]]
}

baja_sin_aplicar() {
    subir estructura_organica "$carpeta/unidad_menos.csv"
    [[ $(jq -r '.vistaPrevia.bajas' <<<"$cuerpo") -ge 1 ]] || return 1
    confirmar estructura_organica "$carpeta/unidad_menos.csv" "$(jq -r '.vistaPrevia.huella' <<<"$cuerpo")"
    [[ $estado == 200 && $(jq -r '.bajasSinAplicar' <<<"$cuerpo") -ge 1 ]] \
        && [[ $(consultar_base "select count(*) from academico.unidad_organica where codigo in ($unidades)") == 24 ]]
}

desactualizada() {
    local huella_a
    subir carreras "$carpeta/carreras_A.csv"
    huella_a=$(jq -r '.vistaPrevia.huella' <<<"$cuerpo")
    [[ $(jq -r '.vistaPrevia.cambian' <<<"$cuerpo") == 1 ]] || return 1
    subir carreras "$carpeta/carreras_B.csv"
    confirmar carreras "$carpeta/carreras_B.csv" "$(jq -r '.vistaPrevia.huella' <<<"$cuerpo")"
    [[ $(jq -r '.resultado' <<<"$cuerpo") == APLICADA ]] || return 1
    confirmar carreras "$carpeta/carreras_A.csv" "$huella_a"
    [[ $estado == 409 && $(jq -r '.codigo' <<<"$cuerpo") == CARGAS_VISTA_PREVIA_DESACTUALIZADA ]]
}

regla_del_dueno() {
    local antes
    antes=$(consultar_base "select count(*) from docentes.cargo")
    subir cargos_autoridades "$carpeta/dos_coordinadores.csv"
    confirmar cargos_autoridades "$carpeta/dos_coordinadores.csv" "$(jq -r '.vistaPrevia.huella' <<<"$cuerpo")"
    [[ $estado == 409 && $(jq -r '.codigo' <<<"$cuerpo") == CARGAS_CARGA_RECHAZADA \
        && $(jq -r '.detalles[0].campo' <<<"$cuerpo") == "fila $fila_del_segundo" \
        && $(consultar_base "select count(*) from docentes.cargo") == "$antes" ]]
}

historial() {
    pedir GET "$GATEWAY/api/cargas/plantillas/carreras/historial" "Bearer $token"
    [[ $estado == 200 && $(jq -c '[.[].resultado]' <<<"$cuerpo") == '["RECHAZADA","APLICADA","REPETIDA","APLICADA"]' ]]
}

confirma() {  # confirma <usuario>
    iniciar "$1"
    [[ -n $token ]] || return 1
    repetida
}

no_confirma() {  # no_confirma <usuario>
    iniciar "$1"
    [[ -n $token ]] || return 1
    confirmar carreras "$(archivo_de carreras)" "$(printf '0%.0s' {1..64})"
    [[ $estado == 403 && $cuerpo == "$CUERPO_403" ]]
}

verificar 1 "Las 10 cargas de prueba, validadas y confirmadas en su orden (SC-001)" las_diez
verificar 2 "Subidas otra vez, ninguna tiene nuevos ni cambios (SC-002)" sin_cambios
verificar 3 "La 02 confirmada otra vez con el mismo archivo: REPETIDA" repetida
verificar 4 "La 01 con una unidad menos: la baja no se aplica y las 24 siguen" baja_sin_aplicar
verificar 5 "Una vista previa que cambió antes de confirmar: 409 CARGAS_VISTA_PREVIA_DESACTUALIZADA" desactualizada
verificar 6 "Un segundo coordinador vigente (RN-12): 409 CARGAS_CARGA_RECHAZADA en su fila, sin cambios" regla_del_dueno
verificar 7 "El historial de la 02, del más reciente al más antiguo" historial
verificar 8 "consolidacion.demo confirma (D-71)" confirma consolidacion.demo
verificar 9 "docente.demo recibe 403 al confirmar" no_confirma docente.demo

if ((fallas > 0)); then
    echo "Confirmación de las cargas de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "Confirmación de las cargas de punta a punta: todos los pasos pasan."
