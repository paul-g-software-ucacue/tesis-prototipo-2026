#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Los totales del docente de punta a punta sobre el perfil levantado, con los dos procesos reales
# (F07-H01; quickstart.md de specs/033-totales-del-docente).
# Uso, desde cualquier directorio: deploy/probar_totales.sh
# Con una base sin estructura orgánica, carreras, distributivos ni los datos de prueba: carga las 10
# cargas de prueba (ficticias, RNF-SEG-06), abre 2026-I, le indica una carrera a cada uno de los dos
# coordinadores y crea el distributivo. Compara los totales de un docente en dos carreras con una suma
# hecha aparte, sin el código de Generación; los compara con la sugerencia; reemplaza y quita; prueba
# el exceso, la proporción, los permisos, los errores y que no lleven detalle de otras carreras; mide;
# detiene el proceso datos y lo vuelve a levantar; y cierra el periodo en la base. Si la base ya tiene
# alguno de esos datos, el script falla sin tocar nada. Al terminar, aunque un paso falle, levanta
# datos, borra lo que creó y revisa que no quede nada. Termina con 0 si todos los pasos pasan y con 1
# si alguno falla. Nunca imprime el token ni la contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly DATOS=backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba
readonly CARGAS=(estructura_organica carreras ciclos_academicos asignaturas_malla oferta_periodo
    docentes politica_carga_horaria cargos_autoridades proyectos_practicas matriculados_carrera)
readonly COORDINADOR_A=coordinacion.demo
readonly COORDINADOR_B=docente.coordinador.demo
readonly PERIODO=2026-I
readonly SEGUNDOS_DE_GUARDAR=3
readonly SEGUNDOS_SIN_DATOS=10

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

# Deja el estado en $estado, el cuerpo en $cuerpo y los segundos en $segundos.
estado=''
cuerpo=''
segundos=0
pedir() {  # pedir <método> <url> [autorización] [cuerpo json]
    local archivo resultado
    archivo=$(mktemp -p "$carpeta")
    local argumentos=(-s -o "$archivo" -w '%{http_code} %{time_total}' -X "$1" "$2")
    [[ -n ${3:-} ]] && argumentos+=(-H "Authorization: $3")
    [[ -n ${4:-} ]] && argumentos+=(-H 'Content-Type: application/json' --data "$4")
    resultado=$(curl "${argumentos[@]}" || echo '000 0')
    estado=${resultado%% *}
    segundos=${resultado##* }
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

es_codigo() {  # es_codigo <estado> <código del catálogo>
    [[ $estado == "$1" && $(jq -r '.codigo' <<<"$cuerpo" 2>/dev/null) == "$2" ]]
}

menos_de() {  # menos_de <segundos> <límite>
    awk -v s="$1" -v m="$2" 'BEGIN { exit !(s < m) }'
}

# Cada código de prueba nuevo lleva este prefijo: P y tres dígitos.
prefijo="P$(printf '%03d' $((RANDOM % 1000)))"
version="${prefijo}-POL-2024"

# --- Los archivos: los de prueba, con la política con el código nuevo ---
# La carrera A tiene paralelos de tronco común y de los demás, y la B es la más pequeña: se completa.
python3 -I - "$DATOS" "$carpeta" "$version" <<'PY'
import csv, io, sys
datos, carpeta, version = sys.argv[1:]

def leer(nombre):
    with open(f'{datos}/{nombre}', encoding='utf-8-sig', newline='') as archivo:
        filas = list(csv.reader(archivo))
    return filas[0], filas[1:]

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
    salida = io.StringIO()
    csv.writer(salida, lineterminator='\r\n').writerows([encabezados, *filas])
    with open(f'{carpeta}/{nombre}.csv', 'w', encoding='utf-8', newline='') as archivo:
        archivo.write(salida.getvalue())

lista('unidades', '01_estructura_organica.csv', 'codigo_unidad')
lista('carreras', '02_carreras.csv', 'codigo_carrera')
lista('periodos', '03_ciclos_academicos.csv', 'codigo_periodo')
lista('docentes', '06_docentes.csv', 'numero_identificacion')
lista('proyectos', '09_proyectos_practicas.csv', 'codigo_proyecto')

encabezados, filas = leer('04_asignaturas_malla.csv')
asignaturas = {fila[encabezados.index('codigo_asignatura')]: dict(zip(encabezados, fila)) for fila in filas}
encabezados, filas = leer('05_oferta_periodo.csv')
oferta = [dict(zip(encabezados, fila)) for fila in filas]
por_carrera = {}
for una in oferta:
    por_carrera.setdefault(asignaturas[una['codigo_asignatura']]['codigo_carrera'], []).append(una)
carrera_a, carrera_b = sorted(por_carrera, key=lambda c: (len(por_carrera[c]), c))[:2]
paralelo_a = por_carrera[carrera_a][0]
paralelo_b = por_carrera[carrera_b][0]

encabezados, filas = leer('06_docentes.csv')
docentes = [dict(zip(encabezados, fila)) for fila in filas]
encabezados, filas = leer('08_cargos_autoridades.csv')
cargos = [dict(zip(encabezados, fila)) for fila in filas]
encabezados, filas = leer('09_proyectos_practicas.csv')
proyectos = [dict(zip(encabezados, fila)) for fila in filas]
participantes = {p['codigo_proyecto']: [uno.strip().split('|')[0] for uno in p['participantes'].split(';')]
                 for p in proyectos}
con_proyecto = {uno for lista in participantes.values() for uno in lista}
con_cargo = {c['numero_identificacion'] for c in cargos}
investigacion = next(p for p in proyectos if p['tipo_proyecto'] == 'INVESTIGACION'
                     and p['vigencia_fin'] >= '2026-08-07')
investigador = participantes[investigacion['codigo_proyecto']][0]
ajeno = next(p['codigo_proyecto'] for p in proyectos if p['tipo_proyecto'] == 'INVESTIGACION'
             and investigador not in participantes[p['codigo_proyecto']])
coordinador = next(c['numero_identificacion'] for c in cargos
                   if c['codigo_cargo'] == 'COORDINADOR_CARRERA' and c['estado'] == 'VIGENTE')
libres = [d for d in docentes if d['estado'] == 'ACTIVO'
          and d['numero_identificacion'] not in con_proyecto | con_cargo]
licencia = next(d['numero_identificacion'] for d in docentes if d['estado'] == 'LICENCIA')
with open(f'{carpeta}/datos.tsv', 'w') as salida:
    salida.write('\t'.join([carrera_a, paralelo_a['codigo_asignatura'], paralelo_a['paralelo'],
                            carrera_b, paralelo_b['codigo_asignatura'], paralelo_b['paralelo'],
                            investigador, investigacion['codigo_proyecto'], ajeno, coordinador,
                            libres[0]['numero_identificacion'], licencia]) + '\n')
with open(f'{carpeta}/carreras_con_oferta', 'w') as salida:
    salida.write(str(len(por_carrera)))
PY
unidades=$(cat "$carpeta/unidades.lista")
carreras=$(cat "$carpeta/carreras.lista")
periodos=$(cat "$carpeta/periodos.lista")
docentes=$(cat "$carpeta/docentes.lista")
proyectos=$(cat "$carpeta/proyectos.lista")
inicio=$(consultar_base "select now()")
IFS=$'\t' read -r carrera_a asignatura_a paralelo_a carrera_b asignatura_b paralelo_b _ \
    _ _ coordinador libre licencia <"$carpeta/datos.tsv"

limpiar() {
    docker compose up -d --wait --no-recreate --no-build datos >/dev/null 2>&1 \
        || echo "AVISO  No se pudo volver a levantar el proceso datos"
    consultar_base "delete from generacion.actividad_no_lectiva where version_id in (select v.id from generacion.version_distributivo v
    join generacion.distributivo d on d.id = v.distributivo_id where d.periodo in ($periodos));
delete from generacion.propuesta where version_id in (select v.id from generacion.version_distributivo v
    join generacion.distributivo d on d.id = v.distributivo_id where d.periodo in ($periodos));
delete from generacion.parcial_carrera where version_id in (select v.id from generacion.version_distributivo v
    join generacion.distributivo d on d.id = v.distributivo_id where d.periodo in ($periodos));
delete from generacion.version_distributivo where distributivo_id in (select id from generacion.distributivo where periodo in ($periodos));
delete from generacion.distributivo where periodo in ($periodos);
delete from generacion.registro_cambio where momento >= '$inicio';
delete from docentes.proyecto where codigo in ($proyectos);
delete from docentes.cargo where docente in ($docentes);
delete from docentes.docente where identificacion in ($docentes);
delete from docentes.registro_cambio where momento >= '$inicio' and usuario in ('datos.demo', 'vicerrectorado.demo');
delete from academico.matricula where periodo in ($periodos);
delete from academico.asignatura_paralelo where periodo in ($periodos);
delete from academico.asignatura where carrera in ($carreras);
delete from academico.malla where carrera in ($carreras);
delete from academico.carrera where codigo in ($carreras);
delete from academico.periodo_academico where codigo in ($periodos);
delete from academico.unidad_organica where codigo in ($unidades);
delete from academico.registro_cambio where momento >= '$inicio' and usuario in ('datos.demo', 'vicerrectorado.demo');
delete from normativa.asignacion_normativa where periodo in ($periodos);
delete from normativa.parametro_regla where version_id in (select id from normativa.version_normativa where codigo = '$version');
delete from normativa.regla where version_id in (select id from normativa.version_normativa where codigo = '$version');
delete from normativa.fila_carga_horaria where version_id in (select id from normativa.version_normativa where codigo = '$version');
delete from normativa.version_normativa where codigo = '$version';
delete from normativa.registro_cambio where momento >= '$inicio' and usuario in ('datos.demo', 'vicerrectorado.demo');
delete from cargas.historial_carga where momento >= '$inicio';
delete from roles.carrera_de_coordinador;
delete from roles.registro_cambio where momento >= '$inicio' and usuario = 'admin.demo';
delete from roles.periodo_conocido where codigo in ($periodos);
delete from roles.registro_cambio where detalle ->> 'periodo' in ($periodos);
delete from roles.evento_recibido where id_evento in (select (serialized_event::jsonb ->> 'idEvento')::uuid
    from eventos_datos.event_publication where serialized_event::jsonb -> 'contenido' ->> 'codigo' in ($periodos));
delete from eventos_datos.event_publication where serialized_event::jsonb -> 'contenido' ->> 'codigo' in ($periodos);" >/dev/null 2>&1 \
        || echo "AVISO  No se pudo borrar lo creado por el script"
    local quedan
    quedan=$(consultar_base "select (select count(*) from generacion.distributivo)
        + (select count(*) from generacion.propuesta) + (select count(*) from generacion.actividad_no_lectiva)
        + (select count(*) from generacion.registro_cambio)" 2>/dev/null || echo '?')
    [[ $quedan == 0 ]] || echo "AVISO  Quedaron $quedan filas en el distributivo"
    rm -rf "$carpeta"
}

# --- 0. La base no tiene estructura, carreras, distributivos ni los datos de prueba ---
ya_estan=$(consultar_base "select (select count(*) from academico.unidad_organica where activa or codigo in ($unidades))
    + (select count(*) from academico.carrera where estado = 'VIGENTE' or codigo in ($carreras))
    + (select count(*) from academico.periodo_academico where codigo in ($periodos))
    + (select count(*) from docentes.docente where identificacion in ($docentes))
    + (select count(*) from docentes.proyecto where codigo in ($proyectos))
    + (select count(*) from roles.carrera_de_coordinador)
    + (select count(*) from generacion.distributivo)")
if [[ $ya_estan != 0 ]]; then
    echo "FALLA  0. La base ya tiene $ya_estan unidades, carreras, datos de prueba, carreras de coordinadores o distributivos: no se tocan."
    rm -rf "$carpeta"
    exit 1
fi
trap limpiar EXIT

iniciar datos.demo
if [[ -z $token ]]; then
    echo "FALLA  0. datos.demo no inicia sesión: ¿está levantado el perfil?"
    exit 1
fi
echo "OK     0. La base no tiene estructura ni distributivos; la política entra como la versión $version"

# Las 10 cargas, en orden.
cargar() {
    local i archivo huella
    for i in "${!CARGAS[@]}"; do
        archivo=$(printf '%s/%02d_%s.csv' "$carpeta" $((i + 1)) "${CARGAS[$i]}")
        subir "${CARGAS[$i]}" "$archivo"
        huella=$(jq -r '.vistaPrevia.huella // empty' <<<"$cuerpo")
        [[ $estado == 200 && $(jq -r '.errores' <<<"$cuerpo") == 0 && -n $huella ]] \
            || { echo "       ${CARGAS[$i]}: la vista previa falló" >&2; return 1; }
        confirmar "${CARGAS[$i]}" "$archivo" "$huella"
        [[ $estado == 200 && $(jq -r '.resultado' <<<"$cuerpo") == APLICADA ]] \
            || { echo "       ${CARGAS[$i]}: la confirmación falló" >&2; return 1; }
    done
}

sugerencia() {  # sugerencia <asignatura> <paralelo>: con el token de $token
    pedir GET "$GATEWAY/api/generacion/periodos/$PERIODO/oferta/$1/$2/sugerencia" "Bearer $token"
}

proponer() {  # proponer <asignatura> <paralelo> <docente> [texto]: con el token de $token
    pedir PUT "$GATEWAY/api/generacion/periodos/$PERIODO/oferta/$1/$2/propuesta" "Bearer $token" \
        "$(jq -cn --arg d "$3" --arg t "${4:-}" 'if $t == "" then {docente: $d} else {docente: $d, texto: $t} end')"
}

# asignar <carrera> <docente> <actividad> <horas> [proyecto]: con el token de $token
asignar() {
    pedir PUT "$GATEWAY/api/generacion/periodos/$PERIODO/parciales/$1/docentes/$2/actividades/$3" \
        "Bearer $token" \
        "$(jq -cn --argjson h "$4" --arg p "${5:-}" 'if $p == "" then {horas: $h} else {horas: $h, proyecto: $p} end')"
}

quitar() {  # quitar <carrera> <docente> <actividad> [proyecto]: con el token de $token
    pedir DELETE "$GATEWAY/api/generacion/periodos/$PERIODO/parciales/$1/docentes/$2/actividades/$3${4:+?proyecto=$4}" \
        "Bearer $token"
}

crear() {  # crear <periodo>: con el token de $token
    pedir POST "$GATEWAY/api/generacion/periodos/$1/distributivo" "Bearer $token"
}

indicar() {  # indicar <usuario> <carrera>: con el token de $token
    pedir PUT "$GATEWAY/api/roles/usuarios/$1/carrera" "Bearer $token" "{\"carrera\":\"$2\"}"
}

preparado() {
    local numero
    cargar || return 1
    iniciar vicerrectorado.demo
    numero=$(consultar_base "select numero from normativa.version_normativa where codigo = '$version'")
    pedir PUT "$GATEWAY/api/academico/periodos/$PERIODO/normativa" "Bearer $token" "{\"version\":${numero:-0}}"
    [[ $estado == 200 ]] || return 1
    pedir GET "$GATEWAY/api/cargas/preparacion?periodo=$PERIODO" "Bearer $token"
    [[ $(jq -r '.listo' <<<"$cuerpo") == true ]] || return 1
    iniciar admin.demo
    indicar "$COORDINADOR_A" "$carrera_a"
    [[ $estado == 200 ]] || return 1
    indicar "$COORDINADOR_B" "$carrera_b"
    [[ $estado == 200 ]] || return 1
    iniciar vicerrectorado.demo
    crear "$PERIODO"
    [[ $estado == 201 ]]
}

carga() {  # carga <docente>: con el token de $token
    pedir GET "$GATEWAY/api/generacion/periodos/$PERIODO/docentes/$1/carga-horaria" "Bearer $token"
}

horas_del_paralelo() {  # horas_del_paralelo: las de asignatura_a, con el token de $token
    pedir GET "$GATEWAY/api/academico/periodos/$PERIODO/oferta" "Bearer $token"
    jq -r --arg a "$asignatura_a" --arg p "$paralelo_a" \
        '.asignaturasParalelo[] | select(.asignatura == $a and .paralelo == $p) | .horasSemanales' <<<"$cuerpo"
}

# Las mismas cuentas de Generación, escritas aparte: suma, disponibles, exceso y proporción.
coincide() {  # coincide <clase> <otras> <investigación> <vinculación> <gestión>
    jq -e --argjson c "$1" --argjson o "$2" --argjson i "$3" --argjson v "$4" --argjson g "$5" '
        ($c + $o + $i + $v + $g) as $todas
        | (.horasContratadas - $todas) as $resto
        | .totales == {clase: $c, otrasDocencia: $o, investigacion: $i, vinculacion: $v, gestion: $g}
        and .asignadas == $todas
        and .disponibles == (if $resto < 0 then 0 else $resto end)
        and .exceso == (if $resto < 0 then -$resto else 0 end)
        and .meta == {clase: 60, otrasDocencia: 40}
        and (if ($c + $o) == 0 then (has("proporcion") | not)
             else (($c * 100 / ($c + $o) + 0.5) | floor) as $p
                 | .proporcion == {clase: $p, otrasDocencia: (100 - $p)} end)' <<<"$cuerpo" >/dev/null
}

horas_a=0

en_dos_carreras() {
    iniciar vicerrectorado.demo
    horas_a=$(horas_del_paralelo)
    [[ -n $horas_a ]] || return 1
    iniciar "$COORDINADOR_A"
    sugerencia "$asignatura_a" "$paralelo_a"
    if jq -e --arg d "$libre" 'any(.candidatos[]; .identificacion == $d)' <<<"$cuerpo" >/dev/null; then
        proponer "$asignatura_a" "$paralelo_a" "$libre"
    else
        proponer "$asignatura_a" "$paralelo_a" "$libre" 'Justificación de la prueba punta a punta'
    fi
    [[ $estado == 200 ]] || return 1
    asignar "$carrera_a" "$libre" 140-b 5.25
    [[ $estado == 200 ]] || return 1
    iniciar "$COORDINADOR_B"
    asignar "$carrera_b" "$libre" 140-g 3
    [[ $estado == 200 ]] || return 1
    asignar "$carrera_b" "$libre" 143-k 2
    [[ $estado == 200 ]] || return 1
    carga "$libre"
    echo "       panel: $(jq -c '.totales' <<<"$cuerpo"); aparte: clase $horas_a, otras 8.25, gestión 2"
    [[ $estado == 200 ]] && coincide "$horas_a" 8.25 0 0 2
}

igual_que_la_sugerencia() {
    local disponibles
    iniciar "$COORDINADOR_B"
    carga "$libre"
    disponibles=$(jq -r '.disponibles' <<<"$cuerpo")
    sugerencia "$asignatura_b" "$paralelo_b"
    jq -e --arg d "$libre" --argjson h "$disponibles" \
        '[.candidatos[], .bajoElMinimo[] | select(.identificacion == $d)][0].horasDisponibles == $h' \
        <<<"$cuerpo" >/dev/null
}

reemplazar_y_quitar() {
    iniciar "$COORDINADOR_A"
    asignar "$carrera_a" "$libre" 140-b 6.5
    carga "$libre"
    coincide "$horas_a" 9.5 0 0 2 || return 1
    quitar "$carrera_a" "$libre" 140-b
    [[ $estado == 204 ]] || return 1
    carga "$libre"
    coincide "$horas_a" 3 0 0 2 || return 1
    asignar "$carrera_a" "$libre" 140-b 5.25
    carga "$libre"
    coincide "$horas_a" 8.25 0 0 2
}

el_exceso() {
    iniciar "$COORDINADOR_A"
    asignar "$carrera_a" "$libre" 143-l 40
    [[ $estado == 200 ]] || return 1
    carga "$libre"
    echo "       con 40 h más: exceso $(jq -r '.exceso' <<<"$cuerpo") h de $(jq -r '.horasContratadas' <<<"$cuerpo") contratadas"
    coincide "$horas_a" 8.25 0 0 42 || return 1
    jq -e '.exceso > 0 and .disponibles == 0' <<<"$cuerpo" >/dev/null || return 1
    quitar "$carrera_a" "$libre" 143-l
    [[ $estado == 204 ]]
}

solo_sumas() {
    iniciar "$COORDINADOR_B"
    carga "$libre"
    [[ $estado == 200 ]] || return 1
    ! grep -qE '"(asignatura|actividades?|paralelo|correo|telefono|carrera)"' <<<"$cuerpo" || return 1
    carga "$coordinador"
    [[ $estado == 200 ]] || return 1
    coincide 0 0 0 0 0
}

los_permisos_y_errores() {
    iniciar docente.demo
    carga "$libre"
    [[ $estado == 403 ]] || return 1
    iniciar datos.demo
    carga "$libre"
    [[ $estado == 403 ]] || return 1
    iniciar vicerrectorado.demo
    carga "$libre"
    [[ $estado == 200 ]] || return 1
    carga "$licencia"
    es_codigo 409 GENERACION_DOCENTE_NO_ACTIVO || return 1
    pedir GET "$GATEWAY/api/generacion/periodos/SIN-PERIODO/docentes/$libre/carga-horaria" "Bearer $token"
    es_codigo 409 GENERACION_SIN_DISTRIBUTIVO
}

la_lista() {
    iniciar "$COORDINADOR_A"
    pedir GET "$GATEWAY/api/generacion/periodos/$PERIODO/actividades" "Bearer $token"
    jq -e --arg d "$libre" --argjson c "$horas_a" '.carreras[0].docentes[] | select(.docente == $d)
        | .totales == {clase: $c, otrasDocencia: 8.25, investigacion: 0, vinculacion: 0, gestion: 2}
        and (.actividades | map(.actividad)) == ["140-b"]' <<<"$cuerpo" >/dev/null
}

rapido() {
    local total
    iniciar "$COORDINADOR_A"
    asignar "$carrera_a" "$libre" 143-m 1
    [[ $estado == 200 ]] || return 1
    total=$segundos
    carga "$libre"
    total=$(awk -v a="$total" -v b="$segundos" 'BEGIN { print a + b }')
    echo "       guardar y consultar: $total s"
    [[ $estado == 200 ]] && menos_de "$total" "$SEGUNDOS_DE_GUARDAR"
}

sin_el_proceso_datos() {
    local bien lista
    iniciar "$COORDINADOR_A"
    docker compose stop datos >/dev/null 2>&1
    carga "$libre"
    echo "       sin datos, los totales respondieron $estado en $segundos s"
    bien=0
    if [[ $estado == 502 || $estado == 504 ]] && [[ $(jq -r '.codigo' <<<"$cuerpo") == COMUN_PROCESO_* ]] \
        && menos_de "$segundos" "$SEGUNDOS_SIN_DATOS"; then
        pedir GET "$GATEWAY/api/generacion/periodos/$PERIODO/actividades" "Bearer $token"
        lista=$estado
        echo "       sin datos, la lista respondió $lista en $segundos s"
        if [[ $lista == 502 || $lista == 504 ]] && menos_de "$segundos" "$SEGUNDOS_SIN_DATOS"; then
            bien=1
        fi
    fi
    docker compose up -d --wait --no-recreate --no-build datos >/dev/null 2>&1 || return 1
    iniciar "$COORDINADOR_A"
    carga "$libre"
    [[ $bien == 1 && $estado == 200 ]]
}

# El último paso: cierra el periodo directo en la base, porque aquí solo importa lo que se lee.
periodo_cerrado() {
    consultar_base "update academico.periodo_academico set estado = 'CERRADO' where codigo = '$PERIODO'" >/dev/null
    iniciar "$COORDINADOR_A"
    carga "$libre"
    [[ $estado == 200 ]]
}

verificar 1 "Las 10 cargas, $PERIODO abierto, una carrera para cada coordinador y el distributivo creado" preparado
verificar 2 "Un docente en dos carreras: totales, disponibles y proporción iguales a una suma aparte" en_dos_carreras
verificar 3 "Las horas disponibles coinciden con las de la sugerencia" igual_que_la_sugerencia
verificar 4 "Reemplazar cuenta solo las horas nuevas, y quitar deja de contar" reemplazar_y_quitar
verificar 5 "Con más horas de las contratadas dice el exceso, y las disponibles son 0" el_exceso
verificar 6 "Solo sumas, sin detalle ni contacto; sin horas no hay proporción" solo_sumas
verificar 7 "Docente y Operador de datos: 403; el Vicerrector lee; docente no activo y sin distributivo: 409" los_permisos_y_errores
verificar 8 "La lista trae los totales de las dos carreras, y solo las actividades de la suya" la_lista
verificar 9 "Guardar y consultar los totales en menos de $SEGUNDOS_DE_GUARDAR s" rapido
verificar 10 "Con datos detenido: totales y lista con su mensaje en menos de $SEGUNDOS_SIN_DATOS s, y vuelve" sin_el_proceso_datos
verificar 11 "Con el periodo cerrado los totales se pueden consultar" periodo_cerrado

if ((fallas > 0)); then
    echo "Los totales de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "Los totales de punta a punta: todos los pasos pasan."
