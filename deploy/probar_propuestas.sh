#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Crear el distributivo y proponer docentes de punta a punta sobre el perfil levantado, con los dos
# procesos reales (F06-H03; quickstart.md de specs/031-proponer-docente).
# Uso, desde cualquier directorio: deploy/probar_propuestas.sh
# Con una base sin estructura orgánica, carreras, distributivos ni los datos de prueba: carga las 10
# cargas de prueba (ficticias, RNF-SEG-06), abre 2026-I y le indica una carrera a cada uno de los dos
# coordinadores. Crea el distributivo; propone, reemplaza y quita; prueba las tres justificaciones;
# completa un parcial; retira un paralelo y lo vuelve a ofertar; compara las horas en otras carreras
# con una suma hecha aparte, sin el código de Generación; prueba otra carrera; mide; detiene el
# proceso datos y lo vuelve a levantar, y busca la justificación en los registros. Si la base ya
# tiene alguno de esos datos, el script falla sin tocar nada. Al terminar, aunque un paso falle,
# levanta datos, borra lo que creó y revisa que no quede nada. Termina con 0 si todos los pasos pasan
# y con 1 si alguno falla. Nunca imprime el token ni la contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly DATOS=backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba
readonly CARGAS=(estructura_organica carreras ciclos_academicos asignaturas_malla oferta_periodo
    docentes politica_carga_horaria cargos_autoridades proyectos_practicas matriculados_carrera)
readonly COORDINADOR_A=coordinacion.demo
readonly COORDINADOR_B=docente.coordinador.demo
readonly PERIODO=2026-I
readonly TEXTO='Dictó esta materia durante tres años seguidos'
readonly SEGUNDOS_DE_PROPONER=3
readonly SEGUNDOS_DE_CREAR=5
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
troncal = lambda una: asignaturas[una['codigo_asignatura']]['tronco_comun'] == 'S'
ambas = [c for c, unas in por_carrera.items() if any(map(troncal, unas)) and not all(map(troncal, unas))]
carrera_a = min(ambas, key=lambda c: (len(por_carrera[c]), c))
carrera_b = min((c for c in por_carrera if c != carrera_a), key=lambda c: (len(por_carrera[c]), c))
comun = next(una for una in por_carrera[carrera_a] if troncal(una))
propia = [una for una in por_carrera[carrera_a] if not troncal(una)]
with open(f'{carpeta}/a.tsv', 'w') as salida:
    salida.write('\t'.join([carrera_a, propia[0]['codigo_asignatura'], propia[0]['paralelo'],
                            comun['codigo_asignatura'], comun['paralelo'],
                            propia[1]['codigo_asignatura'], propia[1]['paralelo']]) + '\n')
with open(f'{carpeta}/b.tsv', 'w') as salida:
    for una in por_carrera[carrera_b]:
        salida.write('\t'.join([carrera_b, una['codigo_asignatura'], una['paralelo']]) + '\n')
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
IFS=$'\t' read -r carrera_a asignatura_n paralelo_n asignatura_s paralelo_s asignatura_h paralelo_h \
    <"$carpeta/a.tsv"
IFS=$'\t' read -r carrera_b asignatura_b paralelo_b <"$carpeta/b.tsv"
paralelos_b=$(wc -l <"$carpeta/b.tsv")

limpiar() {
    docker compose up -d --wait --no-recreate --no-build datos >/dev/null 2>&1 \
        || echo "AVISO  No se pudo volver a levantar el proceso datos"
    consultar_base "delete from generacion.propuesta where version_id in (select v.id from generacion.version_distributivo v
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
        + (select count(*) from generacion.propuesta) + (select count(*) from generacion.registro_cambio)" 2>/dev/null || echo '?')
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

quitar() {  # quitar <asignatura> <paralelo>: con el token de $token
    pedir DELETE "$GATEWAY/api/generacion/periodos/$PERIODO/oferta/$1/$2/propuesta" "Bearer $token"
}

parciales() {  # con el token de $token
    pedir GET "$GATEWAY/api/generacion/periodos/$PERIODO/parciales" "Bearer $token"
}

crear() {  # crear <periodo>: con el token de $token
    pedir POST "$GATEWAY/api/generacion/periodos/$1/distributivo" "Bearer $token"
}

estado_del_paralelo() {  # estado_del_paralelo <asignatura> <paralelo> <estado>
    pedir PUT "$GATEWAY/api/academico/periodos/$PERIODO/oferta/$1/$2/estado" "Bearer $token" \
        "{\"estado\":\"$3\"}"
}

indicar() {  # indicar <usuario> <carrera>: con el token de $token
    pedir PUT "$GATEWAY/api/roles/usuarios/$1/carrera" "Bearer $token" "{\"carrera\":\"$2\"}"
}

propuestas_del_paralelo() {  # propuestas_del_paralelo <asignatura> <paralelo>
    consultar_base "select count(*) from generacion.propuesta where asignatura = '$1' and paralelo = '$2'"
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
    [[ $estado == 200 ]]
}

no_se_puede_crear_todavia() {
    iniciar vicerrectorado.demo
    crear 2026-II
    es_codigo 409 GENERACION_PERIODO_NO_LISTO || return 1
    crear NO-EXISTE
    es_codigo 404 GENERACION_PERIODO_NO_ENCONTRADO || return 1
    pedir GET "$GATEWAY/api/generacion/periodos/$PERIODO/distributivo" "Bearer $token"
    es_codigo 404 GENERACION_DISTRIBUTIVO_NO_ENCONTRADO || return 1
    iniciar "$COORDINADOR_A"
    crear "$PERIODO"
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || return 1
    parciales
    [[ $(jq -c '.creado, (.parciales | length)' <<<"$cuerpo" | tr '\n' ' ') == 'false 0 ' ]] || return 1
    proponer "$asignatura_n" "$paralelo_n" 0109990010
    es_codigo 409 GENERACION_SIN_DISTRIBUTIVO
}

creado() {
    iniciar vicerrectorado.demo
    crear "$PERIODO"
    echo "       crear tardó $segundos s"
    [[ $estado == 201 ]] && menos_de "$segundos" "$SEGUNDOS_DE_CREAR" || return 1
    jq -e --argjson n "$carreras_con_oferta" '.version == 1 and (.carreras | length) == $n' \
        <<<"$cuerpo" >/dev/null || return 1
    pedir GET "$GATEWAY/api/generacion/periodos/$PERIODO/distributivo" "Bearer $token"
    [[ $estado == 200 ]] || return 1
    crear "$PERIODO"
    es_codigo 409 GENERACION_DISTRIBUTIVO_REPETIDO || return 1
    [[ $(consultar_base "select count(*) from generacion.distributivo where periodo = '$PERIODO'") == 1 \
        && $(consultar_base "select count(*) from generacion.parcial_carrera where estado = 'EN_ELABORACION'") == "$carreras_con_oferta" \
        && $(consultar_base "select count(*) from generacion.registro_cambio where accion = 'DISTRIBUTIVO_CREADO' and usuario = 'vicerrectorado.demo'") == 1 ]]
}

una_sola_propuesta() {
    local primero segundo
    iniciar "$COORDINADOR_A"
    sugerencia "$asignatura_n" "$paralelo_n"
    [[ $(jq -r '.distributivoCreado' <<<"$cuerpo") == true ]] || return 1
    primero=$(jq -r '.candidatos[0].identificacion' <<<"$cuerpo")
    segundo=$(jq -r '.candidatos[1].identificacion' <<<"$cuerpo")
    proponer "$asignatura_n" "$paralelo_n" "$primero"
    echo "       proponer tardó $segundos s"
    [[ $estado == 200 && $(jq -r '.justificacion' <<<"$cuerpo") == AFINIDAD ]] \
        && menos_de "$segundos" "$SEGUNDOS_DE_PROPONER" || return 1
    proponer "$asignatura_n" "$paralelo_n" "$segundo"
    [[ $estado == 200 ]] || return 1
    [[ $(propuestas_del_paralelo "$asignatura_n" "$paralelo_n") == 1 ]] || return 1
    sugerencia "$asignatura_n" "$paralelo_n"
    [[ $(jq -r '.propuesta.docente' <<<"$cuerpo") == "$segundo" ]] || return 1
    [[ $(consultar_base "select count(*) from generacion.registro_cambio where accion = 'PROPUESTA_GUARDADA' and detalle ->> 'docenteAnterior' = '$primero' and detalle ->> 'docente' = '$segundo'") == 1 ]] \
        || return 1
    quitar "$asignatura_n" "$paralelo_n"
    [[ $estado == 204 && $(propuestas_del_paralelo "$asignatura_n" "$paralelo_n") == 0 ]] || return 1
    quitar "$asignatura_n" "$paralelo_n"
    es_codigo 404 GENERACION_PROPUESTA_NO_ENCONTRADA
}

tres_justificaciones() {
    local bajo alto
    iniciar "$COORDINADOR_A"
    sugerencia "$asignatura_n" "$paralelo_n"
    bajo=$(jq -r '.bajoElMinimo[0].identificacion // empty' <<<"$cuerpo")
    [[ -n $bajo ]] || return 1
    proponer "$asignatura_n" "$paralelo_n" "$bajo"
    [[ $estado == 400 && $(jq -r '.detalles[0].campo' <<<"$cuerpo") == texto \
        && $(propuestas_del_paralelo "$asignatura_n" "$paralelo_n") == 0 ]] || return 1
    proponer "$asignatura_n" "$paralelo_n" "$bajo" 'corto'
    [[ $estado == 400 ]] || return 1
    proponer "$asignatura_n" "$paralelo_n" "$bajo" "$TEXTO"
    [[ $estado == 200 && $(jq -r '.justificacion' <<<"$cuerpo") == EXCEPCION \
        && $(jq -r '.texto' <<<"$cuerpo") == "$TEXTO" ]] || return 1

    # En tronco común, el mínimo no aplica: disponibilidad o afinidad, nunca el texto.
    sugerencia "$asignatura_s" "$paralelo_s"
    [[ $(jq -r '.bajoElMinimo | length' <<<"$cuerpo") == 0 ]] || return 1
    bajo=$(jq -r '.minimoAfinidad as $m | [.candidatos[] | select(.puntaje.total < $m)][0].identificacion // empty' <<<"$cuerpo")
    alto=$(jq -r '.minimoAfinidad as $m | [.candidatos[] | select(.puntaje.total >= $m)][0].identificacion // empty' <<<"$cuerpo")
    [[ -n $bajo && -n $alto ]] || { echo "       sin los dos casos en tronco común" >&2; return 1; }
    proponer "$asignatura_s" "$paralelo_s" "$bajo" "$TEXTO"
    [[ $estado == 200 && $(jq -r '.justificacion' <<<"$cuerpo") == DISPONIBILIDAD \
        && $(jq -r '.texto // "sin texto"' <<<"$cuerpo") == 'sin texto' ]] || return 1
    proponer "$asignatura_s" "$paralelo_s" "$alto"
    [[ $estado == 200 && $(jq -r '.justificacion' <<<"$cuerpo") == AFINIDAD ]] || return 1
    [[ $(consultar_base "select count(*) from generacion.registro_cambio where detalle::text like '%tres años%'") == 0 ]]
}

parcial_completo() {
    local asignatura paralelo docente
    iniciar "$COORDINADOR_B"
    while IFS=$'\t' read -r _ asignatura paralelo; do
        sugerencia "$asignatura" "$paralelo"
        docente=$(jq -r '.candidatos[0].identificacion // empty' <<<"$cuerpo")
        if [[ -n $docente ]]; then
            proponer "$asignatura" "$paralelo" "$docente"
        else
            proponer "$asignatura" "$paralelo" "$(jq -r '.bajoElMinimo[0].identificacion' <<<"$cuerpo")" "$TEXTO"
        fi
        [[ $estado == 200 ]] || return 1
    done <"$carpeta/b.tsv"
    parciales
    jq -e --arg c "$carrera_b" --argjson n "$paralelos_b" '
        (.parciales | length) == 1 and .parciales[0].carrera == $c and .parciales[0].ofertadas == $n
        and .parciales[0].conPropuesta == $n and .parciales[0].completo' <<<"$cuerpo" >/dev/null
}

retirar_y_volver() {
    iniciar vicerrectorado.demo
    estado_del_paralelo "$asignatura_b" "$paralelo_b" RETIRADA
    [[ $estado == 200 ]] || return 1
    iniciar "$COORDINADOR_B"
    parciales
    jq -e --arg a "$asignatura_b" --argjson n "$((paralelos_b - 1))" '
        .parciales[0].ofertadas == $n and .parciales[0].conPropuesta == $n
        and any(.parciales[0].propuestas[]; .asignatura == $a and .retirada)' <<<"$cuerpo" >/dev/null || return 1
    proponer "$asignatura_b" "$paralelo_b" 0109990010
    es_codigo 409 GENERACION_PARALELO_RETIRADO || return 1
    iniciar vicerrectorado.demo
    estado_del_paralelo "$asignatura_b" "$paralelo_b" OFERTADA
    [[ $estado == 200 ]] || return 1
    iniciar "$COORDINADOR_B"
    parciales
    jq -e --argjson n "$paralelos_b" '.parciales[0].conPropuesta == $n and .parciales[0].completo
        and all(.parciales[0].propuestas[]; .retirada | not)' <<<"$cuerpo" >/dev/null
}

# Las horas de un docente, sumadas aquí con la oferta de Académico y las propuestas: <docente> <carrera
# que se resta> [<asignatura> <paralelo> que no cuentan]. Sin carrera, todas.
suma_aparte() {
    jq -n --argjson oferta "$oferta_entera" --argjson parciales "$parciales_enteros" \
        --arg d "$1" --arg sin "${2:-}" --arg a "${3:-}" --arg p "${4:-}" '
        [$parciales.parciales[] | select(.carrera != $sin) | .propuestas[]
            | select(.docente == $d and (.asignatura == $a and .paralelo == $p | not))
            | . as $una | $oferta.asignaturasParalelo[]
            | select(.asignatura == $una.asignatura and .paralelo == $una.paralelo) | .horasSemanales]
        | add // 0'
}

# Compara las horas de un candidato con la suma aparte: <cuerpo de la sugerencia> <docente> <carrera>
# <asignatura> <paralelo>.
horas_iguales() {
    local otras asignadas fila
    fila=$(jq -c --arg d "$2" '[.candidatos[], .bajoElMinimo[] | select(.identificacion == $d)][0]' <<<"$1")
    otras=$(suma_aparte "$2" "$3")
    asignadas=$(suma_aparte "$2" '' "$4" "$5")
    echo "       $2: $(jq -r '.horasEnOtrasCarreras' <<<"$fila") h en otras carreras; aparte, $otras"
    jq -e --argjson otras "$otras" --argjson asignadas "$asignadas" '
        .horasEnOtrasCarreras == $otras
        and .horasDisponibles == ((.horasContratadas - $asignadas) | if . < 0 then 0 else . end)' \
        <<<"$fila" >/dev/null
}

horas_en_otras_carreras() {
    local docente desde_a desde_b
    iniciar "$COORDINADOR_B"
    parciales
    docente=$(jq -r '.parciales[0].propuestas[0].docente' <<<"$cuerpo")
    [[ -n $docente ]] || return 1
    # El mismo docente, también en la carrera A.
    iniciar "$COORDINADOR_A"
    sugerencia "$asignatura_h" "$paralelo_h"
    if jq -e --arg d "$docente" 'any(.candidatos[]; .identificacion == $d)' <<<"$cuerpo" >/dev/null; then
        proponer "$asignatura_h" "$paralelo_h" "$docente"
    else
        proponer "$asignatura_h" "$paralelo_h" "$docente" "$TEXTO"
    fi
    [[ $estado == 200 ]] || return 1
    sugerencia "$asignatura_n" "$paralelo_n"
    desde_a=$cuerpo
    iniciar "$COORDINADOR_B"
    sugerencia "$asignatura_b" "$paralelo_b"
    desde_b=$cuerpo
    iniciar vicerrectorado.demo
    pedir GET "$GATEWAY/api/academico/periodos/$PERIODO/oferta" "Bearer $token"
    oferta_entera=$cuerpo
    parciales
    parciales_enteros=$cuerpo
    [[ $(jq -r '.parciales | length' <<<"$cuerpo") == "$carreras_con_oferta" ]] || return 1
    horas_iguales "$desde_a" "$docente" "$carrera_a" "$asignatura_n" "$paralelo_n" || return 1
    horas_iguales "$desde_b" "$docente" "$carrera_b" "$asignatura_b" "$paralelo_b" || return 1
    [[ $(suma_aparte "$docente" "$carrera_a") != 0 && $(suma_aparte "$docente" "$carrera_b") != 0 ]]
}

otra_carrera() {
    local antes
    antes=$(consultar_base "select count(*) from generacion.propuesta")
    iniciar "$COORDINADOR_A"
    proponer "$asignatura_b" "$paralelo_b" 0109990010
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || return 1
    [[ $cuerpo != *"$asignatura_b"* ]] || return 1
    quitar "$asignatura_b" "$paralelo_b"
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || return 1
    parciales
    [[ $(jq -r '[.parciales[].carrera] | join(",")' <<<"$cuerpo") == "$carrera_a" ]] || return 1
    iniciar vicerrectorado.demo
    proponer "$asignatura_n" "$paralelo_n" 0109990010
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || return 1
    [[ $(consultar_base "select count(*) from generacion.propuesta") == "$antes" ]]
}

# Con datos detenido: el error del catálogo en menos de $SEGUNDOS_SIN_DATOS, sin guardar nada.
sin_el_proceso_datos() {
    local antes bien docente
    iniciar "$COORDINADOR_A"
    sugerencia "$asignatura_s" "$paralelo_s"
    docente=$(jq -r '.candidatos[0].identificacion' <<<"$cuerpo")
    antes=$(consultar_base "select count(*) from generacion.registro_cambio")
    docker compose stop datos >/dev/null 2>&1
    proponer "$asignatura_s" "$paralelo_s" "$docente"
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
    proponer "$asignatura_s" "$paralelo_s" "$docente"
    [[ $bien == 1 && $estado == 200 ]]
}

sin_la_justificacion_en_los_registros() {
    local veces
    veces=$(docker compose logs --no-color --since "$inicio_en_utc" datos distributivo identidad gateway 2>&1 \
        | grep -c -F -e 'tres años seguidos' || true)
    [[ $veces == 0 ]]
}

inicio_en_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)
oferta_entera=''
parciales_enteros=''

verificar 1 "Las 10 cargas, $PERIODO abierto y listo, y una carrera para cada coordinador" preparado
verificar 2 "Antes de crear: sin preparar 409, sin el periodo 404, sin permiso 403 y proponer 409" no_se_puede_crear_todavia
verificar 3 "vicerrectorado.demo crea la versión 1 con $carreras_con_oferta parciales en menos de $SEGUNDOS_DE_CREAR s, y otra vez 409" creado
verificar 4 "Proponer en menos de $SEGUNDOS_DE_PROPONER s, reemplazar y quitar: una sola propuesta, con el docente anterior registrado" una_sola_propuesta
verificar 5 "Las tres justificaciones; bajo el mínimo sin texto no se guarda; el texto no va al registro" tres_justificaciones
verificar 6 "$COORDINADOR_B completa el parcial de sus $paralelos_b paralelos" parcial_completo
verificar 7 "Retirar marca la propuesta y no cuenta; volver a ofertar la recupera" retirar_y_volver
verificar 8 "Las horas de un docente en dos carreras, iguales a una suma aparte desde cada coordinador" horas_en_otras_carreras
verificar 9 "Otra carrera: 403 sin datos, cada coordinador ve su parcial y el Vicerrector no propone" otra_carrera
verificar 10 "Con datos detenido: 502 o 504 del catálogo en menos de $SEGUNDOS_SIN_DATOS s, sin guardar, y vuelve" sin_el_proceso_datos
verificar 11 "La justificación no aparece en los registros de los procesos" sin_la_justificacion_en_los_registros

if ((fallas > 0)); then
    echo "Las propuestas de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "Las propuestas de punta a punta: todos los pasos pasan."
