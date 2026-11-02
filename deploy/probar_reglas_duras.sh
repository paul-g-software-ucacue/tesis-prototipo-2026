#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Las reglas duras de punta a punta sobre el perfil levantado, con los dos procesos reales (F07-H02;
# quickstart.md de specs/034-reglas-duras).
# Uso, desde cualquier directorio: deploy/probar_reglas_duras.sh
# Con una base sin estructura orgánica, carreras, distributivos ni los datos de prueba: carga las 10
# cargas de prueba (ficticias, RNF-SEG-06), abre 2026-I, le indica una carrera a cada uno de los dos
# coordinadores y crea el distributivo. Prueba las horas en una y dos carreras, el tope de clase con
# proyecto, 20 pares de intentos a la vez, la sugerencia frente al guardado y un docente excedido, que
# crea directo en la base porque por la API ya no se puede; mide; detiene el proceso datos y lo vuelve
# a levantar. Si la base ya tiene alguno de esos datos, el script falla sin tocar nada. Al terminar,
# aunque un paso falle, levanta datos, borra lo que creó y revisa que no quede nada. Termina con 0 si
# todos los pasos pasan y con 1 si alguno falla. Nunca imprime el token ni la contraseña.
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
IFS=$'\t' read -r carrera_a asignatura_a paralelo_a carrera_b asignatura_b paralelo_b investigador \
    _ _ coordinador libre _ <"$carpeta/datos.tsv"

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

reiniciar() {  # reiniciar <docente>: borra sus asignaciones, directo en la base
    consultar_base "delete from generacion.actividad_no_lectiva where docente = '$1';
        delete from generacion.propuesta where docente = '$1'" >/dev/null
}

# agregar_clase <docente> <horas>: le da clases directo en la base, sin pasar por las reglas. Las saca
# de los paralelos libres de la carrera A, hasta llegar a esas horas. Deja el siguiente en $siguiente.
siguiente=''
agregar_clase() {
    local docente=$1 faltan=$2 oferta fila asignatura paralelo horas version
    version=$(consultar_base "select v.id from generacion.version_distributivo v
        join generacion.distributivo d on d.id = v.distributivo_id where d.periodo = '$PERIODO'")
    iniciar vicerrectorado.demo
    pedir GET "$GATEWAY/api/academico/periodos/$PERIODO/oferta" "Bearer $token"
    oferta=$(jq -r --arg c "$carrera_a" '.asignaturasParalelo[]
        | select(.carrera == $c and .estado != "RETIRADA")
        | "\(.asignatura)\t\(.paralelo)\t\(.horasSemanales)"' <<<"$cuerpo")
    siguiente=''
    # Por el descriptor 3: consultar_base lee la entrada estándar y se comería la lista.
    while IFS=$'\t' read -r asignatura paralelo horas <&3; do
        [[ $(consultar_base "select count(*) from generacion.propuesta
            where asignatura = '$asignatura' and paralelo = '$paralelo'") == 0 ]] || continue
        if awk -v f="$faltan" 'BEGIN { exit !(f > 0) }'; then
            consultar_base "insert into generacion.propuesta (version_id, asignatura, paralelo, carrera,
                docente, nombre_docente, justificacion, propuesta_en, propuesta_por)
                values ('$version', '$asignatura', '$paralelo', '$carrera_a', '$docente', 'PRUEBA',
                'AFINIDAD', now(), 'prueba')" >/dev/null
            faltan=$(awk -v f="$faltan" -v h="$horas" 'BEGIN { print f - h }')
        else
            siguiente="$asignatura $paralelo"
            return 0
        fi
    done 3<<<"$oferta"
    return 1
}

# Propone con el texto si hace falta: la justificación no levanta una regla dura.
proponer_con_texto() {  # proponer_con_texto <asignatura> <paralelo> <docente>
    proponer "$1" "$2" "$3"
    if [[ $estado == 400 ]]; then
        proponer "$1" "$2" "$3" 'Justificación de la prueba punta a punta'
    fi
}

contratadas=0

las_horas() {
    local faltan
    reiniciar "$libre"
    iniciar "$COORDINADOR_A"
    carga "$libre"
    contratadas=$(jq -r '.horasContratadas' <<<"$cuerpo")
    echo "       el docente tiene $contratadas h contratadas"
    faltan=$((contratadas - 1))
    asignar "$carrera_a" "$libre" 143-k "$faltan"
    [[ $estado == 200 ]] || return 1
    asignar "$carrera_a" "$libre" 140-b 2
    es_codigo 409 GENERACION_HORAS_EXCEDIDAS || return 1
    [[ $(jq -r '.titulo' <<<"$cuerpo") == 'Horas laborales excedidas' ]] || return 1
    [[ $(jq -r '.detalles[0].campo' <<<"$cuerpo") == horas ]] || return 1
    grep -q 'Le quedan 1 h' <<<"$cuerpo" || return 1
    [[ $(consultar_base "select count(*) from generacion.actividad_no_lectiva where docente = '$libre'") == 1 ]] || return 1
    asignar "$carrera_a" "$libre" 140-b 0.5
    [[ $estado == 200 ]] || return 1
    iniciar "$COORDINADOR_B"
    asignar "$carrera_b" "$libre" 140-g 1
    es_codigo 409 GENERACION_HORAS_EXCEDIDAS || return 1
    asignar "$carrera_b" "$libre" 140-g 0.5
    [[ $estado == 200 ]] || return 1
    carga "$libre"
    jq -e '.exceso == 0 and .disponibles == 0' <<<"$cuerpo" >/dev/null
}

el_tope_de_clase() {
    local asignatura paralelo con_practicas
    reiniciar "$investigador"
    agregar_clase "$investigador" 14 || return 1
    read -r asignatura paralelo <<<"$siguiente"
    iniciar "$COORDINADOR_A"
    carga "$investigador"
    echo "       con proyecto: $(jq -r '.totales.clase' <<<"$cuerpo") h de clase antes de proponer"
    if awk -v c="$(jq -r '.totales.clase' <<<"$cuerpo")" 'BEGIN { exit !(c > 16) }'; then return 1; fi
    # Con 3 h o más en el siguiente paralelo, pasa de 16.
    proponer_con_texto "$asignatura" "$paralelo" "$investigador"
    es_codigo 409 GENERACION_TOPE_DE_CLASE || return 1
    grep -q 'de clase' <<<"$cuerpo" || return 1
    asignar "$carrera_a" "$investigador" 140-b 2
    [[ $estado == 200 ]] || return 1
    con_practicas=$(consultar_base "select p.participantes -> 0 ->> 'docente' from docentes.proyecto p
        where p.tipo = 'PRACTICAS_PREPROFESIONALES' and p.estado in ('APROBADO', 'EN_EJECUCION')
        and not exists (select 1 from docentes.proyecto o where o.tipo in ('INVESTIGACION', 'VINCULACION')
            and o.participantes::text like '%' || (p.participantes -> 0 ->> 'docente') || '%') limit 1")
    [[ -n $con_practicas ]] || { echo "       no hay un docente solo con prácticas" >&2; return 1; }
    # Libera los paralelos que usó el paso anterior.
    reiniciar "$investigador"
    agregar_clase "$con_practicas" 14 || return 1
    read -r asignatura paralelo <<<"$siguiente"
    iniciar "$COORDINADOR_A"
    proponer_con_texto "$asignatura" "$paralelo" "$con_practicas"
    echo "       con prácticas respondió $estado"
    [[ $estado == 200 ]]
}

# dos_a_la_vez <docente> <horas A> <horas B>: envía las dos a la vez; deja los códigos en $codigos.
codigos=''
dos_a_la_vez() {
    local docente=$1 uno dos
    uno=$(mktemp -p "$carpeta")
    dos=$(mktemp -p "$carpeta")
    curl -s -o /dev/null -w '%{http_code}' -X PUT -H "Authorization: Bearer $token_a" \
        -H 'Content-Type: application/json' --data "{\"horas\":$2}" \
        "$GATEWAY/api/generacion/periodos/$PERIODO/parciales/$carrera_a/docentes/$docente/actividades/143-l" >"$uno" &
    curl -s -o "$dos.cuerpo" -w '%{http_code}' -X PUT -H "Authorization: Bearer $token_b" \
        -H 'Content-Type: application/json' --data "{\"horas\":$3}" \
        "$GATEWAY/api/generacion/periodos/$PERIODO/parciales/$carrera_b/docentes/$docente/actividades/143-m" >"$dos" &
    wait
    codigos="$(cat "$uno") $(cat "$dos")"
}

a_la_vez() {
    local i base rechazados=0 guardados=0 token_a token_b
    iniciar "$COORDINADOR_A"
    token_a=$token
    iniciar "$COORDINADOR_B"
    token_b=$token
    base=$((contratadas - 3))
    for i in $(seq 20); do
        reiniciar "$libre"
        token=$token_a
        asignar "$carrera_a" "$libre" 143-k "$base"
        [[ $estado == 200 ]] || return 1
        dos_a_la_vez "$libre" 2 2
        case $codigos in
            '200 409' | '409 200') guardados=$((guardados + 1)); rechazados=$((rechazados + 1)) ;;
            *) echo "       intento $i: $codigos" >&2; return 1 ;;
        esac
        carga "$libre"
        jq -e '.exceso == 0' <<<"$cuerpo" >/dev/null || return 1
    done
    echo "       20 pares: $guardados guardados y $rechazados rechazados, nunca excedido"
    reiniciar "$libre"
    reiniciar "$coordinador"
    token=$token_a
    asignar "$carrera_a" "$libre" 143-k 1
    [[ $estado == 200 ]]
}

docentes_distintos() {
    local token_a token_b uno dos
    reiniciar "$libre"
    iniciar "$COORDINADOR_A"
    token_a=$token
    iniciar "$COORDINADOR_B"
    token_b=$token
    uno=$(mktemp -p "$carpeta")
    dos=$(mktemp -p "$carpeta")
    curl -s -o /dev/null -w '%{http_code}' -X PUT -H "Authorization: Bearer $token_a" \
        -H 'Content-Type: application/json' --data '{"horas":1}' \
        "$GATEWAY/api/generacion/periodos/$PERIODO/parciales/$carrera_a/docentes/$libre/actividades/143-l" >"$uno" &
    curl -s -o /dev/null -w '%{http_code}' -X PUT -H "Authorization: Bearer $token_b" \
        -H 'Content-Type: application/json' --data '{"horas":1}' \
        "$GATEWAY/api/generacion/periodos/$PERIODO/parciales/$carrera_b/docentes/$coordinador/actividades/143-m" >"$dos" &
    wait
    [[ $(cat "$uno") == 200 && $(cat "$dos") == 200 ]]
}

sugerencia_y_guardado() {
    local fila motivo docente sin_motivo
    reiniciar "$libre"
    iniciar "$COORDINADOR_A"
    asignar "$carrera_a" "$libre" 143-k "$contratadas"
    [[ $estado == 200 ]] || return 1
    sugerencia "$asignatura_a" "$paralelo_a"
    fila=$(jq -c --arg d "$libre" '[.candidatos[], .bajoElMinimo[] | select(.identificacion == $d)][0]' <<<"$cuerpo")
    motivo=$(jq -r '.motivoParaNoProponer // ""' <<<"$fila")
    echo "       sin horas: motivo $motivo"
    [[ $motivo == HORAS ]] || return 1
    proponer_con_texto "$asignatura_a" "$paralelo_a" "$libre"
    es_codigo 409 GENERACION_HORAS_EXCEDIDAS || return 1
    sugerencia "$asignatura_a" "$paralelo_a"
    docente=$(jq -r --arg l "$libre" --arg i "$investigador" \
        '[.candidatos[], .bajoElMinimo[] | select(has("motivoParaNoProponer") | not)
            | select(.identificacion != $l and .identificacion != $i)][0].identificacion' <<<"$cuerpo")
    sin_motivo=$docente
    proponer_con_texto "$asignatura_a" "$paralelo_a" "$sin_motivo"
    [[ $estado == 200 ]] || return 1
    # El orden: nadie con motivo antes de alguien sin motivo.
    iniciar "$COORDINADOR_B"
    sugerencia "$asignatura_b" "$paralelo_b"
    jq -e '[.candidatos[] | has("motivoParaNoProponer")] | . == sort' <<<"$cuerpo" >/dev/null
}

un_docente_excedido() {
    local version
    reiniciar "$libre"
    version=$(consultar_base "select v.id from generacion.version_distributivo v
        join generacion.distributivo d on d.id = v.distributivo_id where d.periodo = '$PERIODO'")
    consultar_base "insert into generacion.actividad_no_lectiva (id, version_id, carrera, docente, nombre_docente,
        actividad, nombre_actividad, componente, horas, asignada_en, asignada_por)
        select gen_random_uuid(), '$version', '$carrera_a', '$libre', 'PRUEBA', a.codigo, 'Una actividad',
        'GESTION', a.horas, now(), 'prueba' from (values ('143-k', 30.00), ('143-l', 30.00)) as a (codigo, horas)" >/dev/null
    iniciar "$COORDINADOR_A"
    asignar "$carrera_a" "$libre" 143-k 25
    [[ $estado == 200 ]] || return 1
    asignar "$carrera_a" "$libre" 143-k 26
    es_codigo 409 GENERACION_HORAS_EXCEDIDAS || return 1
    asignar "$carrera_a" "$libre" 140-b 1
    es_codigo 409 GENERACION_HORAS_EXCEDIDAS || return 1
    quitar "$carrera_a" "$libre" 143-l
    [[ $estado == 204 ]]
}

sin_proyecto() {
    iniciar "$COORDINADOR_A"
    asignar "$carrera_a" "$coordinador" 141-a 2
    es_codigo 409 GENERACION_PROYECTO_NO_VALIDO
}

rapido() {
    reiniciar "$libre"
    iniciar "$COORDINADOR_A"
    asignar "$carrera_a" "$libre" 143-k 1
    echo "       guardar: $segundos s"
    [[ $estado == 200 ]] && menos_de "$segundos" "$SEGUNDOS_DE_GUARDAR"
}

sin_el_proceso_datos() {
    local antes bien
    iniciar "$COORDINADOR_A"
    antes=$(consultar_base "select count(*) from generacion.registro_cambio")
    docker compose stop datos >/dev/null 2>&1
    asignar "$carrera_a" "$libre" 143-l 1
    echo "       sin datos respondió $estado en $segundos s"
    bien=0
    if [[ $estado == 502 || $estado == 504 ]] && [[ $(jq -r '.codigo' <<<"$cuerpo") == COMUN_PROCESO_* ]] \
        && menos_de "$segundos" "$SEGUNDOS_SIN_DATOS" \
        && [[ $(consultar_base "select count(*) from generacion.registro_cambio") == "$antes" ]]; then
        bien=1
    fi
    docker compose up -d --wait --no-recreate --no-build datos >/dev/null 2>&1 || return 1
    iniciar "$COORDINADOR_A"
    asignar "$carrera_a" "$libre" 143-l 1
    [[ $bien == 1 && $estado == 200 ]]
}

verificar 1 "Las 10 cargas, $PERIODO abierto, una carrera para cada coordinador y el distributivo creado" preparado
verificar 2 "Las horas: 409 con las que quedan, justo al límite se guarda, y cuentan las dos carreras" las_horas
verificar 3 "El tope de clase con proyecto: 409; otras de docencia se guardan; con prácticas no aplica" el_tope_de_clase
verificar 4 "20 pares de intentos a la vez: se guarda uno de cada par, sin error técnico y nunca excedido" a_la_vez
verificar 5 "Dos docentes distintos a la vez se guardan" docentes_distintos
verificar 6 "La sugerencia dice lo mismo que el guardado, y pone primero a quienes se pueden proponer" sugerencia_y_guardado
verificar 7 "Un docente excedido: reduce y quita, pero no aumenta ni agrega" un_docente_excedido
verificar 8 "Horas de investigación sin proyecto: 409" sin_proyecto
verificar 9 "Guardar en menos de $SEGUNDOS_DE_GUARDAR s" rapido
verificar 10 "Con datos detenido: 502 o 504 en menos de $SEGUNDOS_SIN_DATOS s, sin guardar, y vuelve" sin_el_proceso_datos

if ((fallas > 0)); then
    echo "Las reglas duras de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "Las reglas duras de punta a punta: todos los pasos pasan."
