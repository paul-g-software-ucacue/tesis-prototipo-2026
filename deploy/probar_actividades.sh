#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Las actividades no lectivas de punta a punta sobre el perfil levantado, con los dos procesos reales
# (F06-H04; quickstart.md de specs/032-actividades-no-lectivas).
# Uso, desde cualquier directorio: deploy/probar_actividades.sh
# Con una base sin estructura orgánica, carreras, distributivos ni los datos de prueba: carga las 10
# cargas de prueba (ficticias, RNF-SEG-06), abre 2026-I, le indica una carrera a cada uno de los dos
# coordinadores y crea el distributivo. Revisa el catálogo; asigna, reemplaza y quita; prueba el
# proyecto y el cargo que exige cada actividad; las horas; otra carrera; compara las horas con una suma
# hecha aparte desde dos coordinadores, sin el código de Generación; mide; detiene el proceso datos y lo
# vuelve a levantar; y cierra el periodo en la base para comprobar que no se puede quitar. Si la base
# ya tiene alguno de esos datos, el script falla sin tocar nada. Al terminar, aunque un paso falle,
# levanta datos, borra lo que creó y revisa que no quede nada. Termina con 0 si todos los pasos pasan y
# con 1 si alguno falla. Nunca imprime el token ni la contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly DATOS=backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba
readonly CARGAS=(estructura_organica carreras ciclos_academicos asignaturas_malla oferta_periodo
    docentes politica_carga_horaria cargos_autoridades proyectos_practicas matriculados_carrera)
readonly COORDINADOR_A=coordinacion.demo
readonly COORDINADOR_B=docente.coordinador.demo
readonly PERIODO=2026-I
readonly SEGUNDOS_DE_ASIGNAR=3
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
carreras_con_oferta=$(cat "$carpeta/carreras_con_oferta")
inicio=$(consultar_base "select now()")
IFS=$'\t' read -r carrera_a asignatura_a paralelo_a carrera_b asignatura_b paralelo_b investigador \
    proyecto proyecto_ajeno coordinador libre licencia <"$carpeta/datos.tsv"

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

quitar_propuesta() {  # quitar_propuesta <asignatura> <paralelo>: con el token de $token
    pedir DELETE "$GATEWAY/api/generacion/periodos/$PERIODO/oferta/$1/$2/propuesta" "Bearer $token"
}

actividades() {  # con el token de $token
    pedir GET "$GATEWAY/api/generacion/periodos/$PERIODO/actividades" "Bearer $token"
}

posibles() {  # posibles <docente>: con el token de $token
    pedir GET "$GATEWAY/api/generacion/periodos/$PERIODO/docentes/$1/actividades-posibles" "Bearer $token"
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

actividades_guardadas() {  # actividades_guardadas <docente> <actividad>
    consultar_base "select count(*) from generacion.actividad_no_lectiva where docente = '$1' and actividad = '$2'"
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

el_catalogo() {
    iniciar "$COORDINADOR_A"
    posibles "$libre"
    [[ $estado == 200 ]] || return 1
    jq -e '(.actividades | length) == 47
        and ([.actividades[].actividad] | contains(["140-b", "143-k"]))
        and ([.actividades[].actividad] | index("143-i") == null and index("140-a") == null
            and index("143-q") == null)
        and all(.actividades[]; (.nombre | ascii_downcase | contains("comisi") | not))' \
        <<<"$cuerpo" >/dev/null || return 1

    actividades
    [[ $(jq -r '[.carreras[].carrera] | join(",")' <<<"$cuerpo") == "$carrera_a" ]]
}

una_sola_actividad() {
    iniciar "$COORDINADOR_A"
    asignar "$carrera_a" "$libre" 140-b 4.25
    echo "       asignar tardó $segundos s"
    [[ $estado == 200 && $(jq -r '.actividad.componente' <<<"$cuerpo") == OTRAS_DOCENCIA ]] \
        && menos_de "$segundos" "$SEGUNDOS_DE_ASIGNAR" || return 1
    asignar "$carrera_a" "$libre" 140-b 6
    [[ $estado == 200 && $(actividades_guardadas "$libre" 140-b) == 1 ]] || return 1
    actividades
    jq -e --arg d "$libre" '.carreras[0].docentes[] | select(.docente == $d)
        | .totales.otrasDocencia == 6' <<<"$cuerpo" >/dev/null || return 1
    [[ $(consultar_base "select count(*) from generacion.registro_cambio where accion = 'ACTIVIDAD_GUARDADA' and detalle ->> 'horasAnteriores' = '4.25'") == 1 ]] \
        || return 1
    quitar "$carrera_a" "$libre" 140-b
    [[ $estado == 204 && $(actividades_guardadas "$libre" 140-b) == 0 ]] || return 1
    quitar "$carrera_a" "$libre" 140-b
    es_codigo 404 GENERACION_ACTIVIDAD_NO_ENCONTRADA
}

el_proyecto() {
    iniciar "$COORDINADOR_A"
    posibles "$investigador"
    jq -e --arg p "$proyecto" '.actividades[] | select(.actividad == "141-a")
        | .disponible and .exigeProyecto and any(.proyectos[]; .codigo == $p)' \
        <<<"$cuerpo" >/dev/null || return 1
    posibles "$libre"
    [[ $(jq -r '.actividades[] | select(.actividad == "141-a") | .disponible' <<<"$cuerpo") == false ]] \
        || return 1
    asignar "$carrera_a" "$investigador" 141-a 6
    es_codigo 409 GENERACION_PROYECTO_NO_VALIDO || return 1
    asignar "$carrera_a" "$investigador" 141-a 6 "$proyecto_ajeno"
    es_codigo 409 GENERACION_PROYECTO_NO_VALIDO || return 1
    asignar "$carrera_a" "$investigador" 140-b 2 "$proyecto"
    es_codigo 409 GENERACION_PROYECTO_NO_VALIDO || return 1
    [[ $(actividades_guardadas "$investigador" 141-a) == 0 ]] || return 1
    asignar "$carrera_a" "$investigador" 141-a 6 "$proyecto"
    [[ $estado == 200 && $(jq -r '.actividad.proyecto' <<<"$cuerpo") == "$proyecto" ]] || return 1
    quitar "$carrera_a" "$investigador" 141-a "$proyecto"
    [[ $estado == 204 ]]
}

el_cargo() {
    iniciar "$COORDINADOR_A"
    asignar "$carrera_a" "$libre" 143-d 10
    es_codigo 409 GENERACION_CARGO_NO_VIGENTE || return 1
    asignar "$carrera_a" "$coordinador" 143-d 10
    [[ $estado == 200 && $(jq -r '.actividad.componente' <<<"$cuerpo") == GESTION ]] || return 1
    quitar "$carrera_a" "$coordinador" 143-d
    asignar "$carrera_a" "$libre" 143-k 2
    [[ $estado == 200 ]] || return 1
    quitar "$carrera_a" "$libre" 143-k
    [[ $estado == 204 ]]
}

las_entradas() {
    iniciar "$COORDINADOR_A"
    local horas
    for horas in 0 41 4.125; do
        asignar "$carrera_a" "$libre" 140-b "$horas"
        [[ $estado == 400 ]] || { echo "       $horas h respondió $estado" >&2; return 1; }
    done
    asignar "$carrera_a" "$libre" 143-i 2
    [[ $estado == 400 || $estado == 404 ]] || return 1
    es_codigo 404 GENERACION_ACTIVIDAD_FUERA_DEL_CATALOGO || return 1
    asignar "$carrera_a" "$licencia" 140-b 2
    es_codigo 409 GENERACION_DOCENTE_NO_ACTIVO || return 1
    [[ $(consultar_base "select count(*) from generacion.actividad_no_lectiva") == 0 ]]
}

otra_carrera() {
    iniciar "$COORDINADOR_A"
    asignar "$carrera_b" "$libre" 140-b 2
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || return 1
    quitar "$carrera_b" "$libre" 140-b
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || return 1
    iniciar vicerrectorado.demo
    asignar "$carrera_a" "$libre" 140-b 2
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || return 1
    actividades
    [[ $(jq -r '.carreras | length' <<<"$cuerpo") == "$carreras_con_oferta" ]] || return 1
    [[ $(consultar_base "select count(*) from generacion.actividad_no_lectiva") == 0 ]]
}

# Las horas del docente, sumadas aquí: la oferta de Académico más las actividades registradas.
horas_completas() {
    local horas_a fila contratadas
    iniciar vicerrectorado.demo
    pedir GET "$GATEWAY/api/academico/periodos/$PERIODO/oferta" "Bearer $token"
    horas_a=$(jq -r --arg a "$asignatura_a" --arg p "$paralelo_a" \
        '.asignaturasParalelo[] | select(.asignatura == $a and .paralelo == $p) | .horasSemanales' <<<"$cuerpo")
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
    sugerencia "$asignatura_b" "$paralelo_b"
    fila=$(jq -c --arg d "$libre" '[.candidatos[], .bajoElMinimo[] | select(.identificacion == $d)][0]' <<<"$cuerpo")
    contratadas=$(jq -r '.horasContratadas' <<<"$fila")
    echo "       desde B: $(jq -r '.horasEnOtrasCarreras' <<<"$fila") h en otras; aparte, $(jq -n "$horas_a + 5.25")"
    jq -e --argjson otras "$(jq -n "$horas_a + 5.25")" --argjson todas "$(jq -n "$horas_a + 5.25 + 3")" \
        --argjson c "$contratadas" '.horasEnOtrasCarreras == $otras
        and .horasDisponibles == (($c - $todas) | if . < 0 then 0 else . end)' <<<"$fila" >/dev/null || return 1
    iniciar "$COORDINADOR_A"
    sugerencia "$asignatura_a" "$paralelo_a"
    fila=$(jq -c --arg d "$libre" '[.candidatos[], .bajoElMinimo[] | select(.identificacion == $d)][0]' <<<"$cuerpo")
    echo "       desde A: $(jq -r '.horasEnOtrasCarreras' <<<"$fila") h en otras; aparte, 3"
    jq -e --argjson c "$contratadas" '.horasEnOtrasCarreras == 3
        and .horasDisponibles == (($c - 8.25) | if . < 0 then 0 else . end)' <<<"$fila" >/dev/null
}

# Con datos detenido: el error del catálogo en menos de $SEGUNDOS_SIN_DATOS, sin guardar nada.
sin_el_proceso_datos() {
    local antes bien
    iniciar "$COORDINADOR_A"
    antes=$(consultar_base "select count(*) from generacion.registro_cambio")
    docker compose stop datos >/dev/null 2>&1
    asignar "$carrera_a" "$libre" 143-k 2
    echo "       sin datos respondió $estado en $segundos s"
    bien=0
    if [[ $estado == 502 || $estado == 504 ]] \
        && [[ $(jq -r '.codigo' <<<"$cuerpo") == COMUN_PROCESO_* ]] \
        && menos_de "$segundos" "$SEGUNDOS_SIN_DATOS" \
        && [[ $(consultar_base "select count(*) from generacion.registro_cambio") == "$antes" ]]; then
        bien=1
    fi
    docker compose up -d --wait --no-recreate --no-build datos >/dev/null 2>&1 || return 1
    iniciar "$COORDINADOR_A"
    asignar "$carrera_a" "$libre" 143-k 2
    [[ $bien == 1 && $estado == 200 ]]
}

# El último paso: cierra el periodo directo en la base, porque aquí solo importa lo que se lee.
periodo_cerrado() {
    consultar_base "update academico.periodo_academico set estado = 'CERRADO' where codigo = '$PERIODO'" >/dev/null
    iniciar "$COORDINADOR_A"
    asignar "$carrera_a" "$libre" 143-l 2
    es_codigo 409 GENERACION_PERIODO_CERRADO || return 1
    quitar "$carrera_a" "$libre" 143-k
    es_codigo 409 GENERACION_PERIODO_CERRADO || return 1
    quitar_propuesta "$asignatura_a" "$paralelo_a"
    es_codigo 409 GENERACION_PERIODO_CERRADO || return 1
    [[ $(actividades_guardadas "$libre" 143-k) == 1 ]]
}

verificar 1 "Las 10 cargas, $PERIODO abierto, una carrera para cada coordinador y el distributivo creado" preparado
verificar 2 "El catálogo tiene las 47 actividades, sin comisiones ni clases, y cada coordinador ve su carrera" el_catalogo
verificar 3 "Asignar en menos de $SEGUNDOS_DE_ASIGNAR s, reemplazar y quitar: una sola, con las horas anteriores registradas" una_sola_actividad
verificar 4 "El proyecto: sin él, ajeno o de más, 409; con el del docente se guarda" el_proyecto
verificar 5 "El cargo: sin él, 409; con él se guarda; y 143-k no lo pide" el_cargo
verificar 6 "Horas en 0, 41 y con 3 decimales, comisiones y un docente en licencia: rechazados" las_entradas
verificar 7 "Otra carrera: 403; el Vicerrector no asigna, pero ve las $carreras_con_oferta carreras" otra_carrera
verificar 8 "Las horas con propuestas y actividades, iguales a una suma aparte desde los dos coordinadores" horas_completas
verificar 9 "Con datos detenido: 502 o 504 del catálogo en menos de $SEGUNDOS_SIN_DATOS s, sin guardar, y vuelve" sin_el_proceso_datos
verificar 10 "Con el periodo cerrado no se asigna ni se quita: ni actividades ni propuestas" periodo_cerrado

if ((fallas > 0)); then
    echo "Las actividades de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "Las actividades de punta a punta: todos los pasos pasan."
