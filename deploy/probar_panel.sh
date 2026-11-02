#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# El panel de inconsistencias de punta a punta sobre el perfil levantado, con los procesos reales
# (F10-H01; quickstart.md de specs/036-panel-inconsistencias).
# Uso, desde cualquier directorio: deploy/probar_panel.sh
# Con una base sin estructura orgánica, carreras, distributivos ni los datos de prueba: carga las 10
# cargas de prueba (ficticias, RNF-SEG-06), abre 2026-I y crea su distributivo. Compara el panel con
# un conteo hecho aparte, corrige, justifica y prueba lo que se rechaza. Prepara en la base los casos
# que la API ya no deja crear y los deshace. Abre 2026-II con otro margen, y detiene el proceso datos.
# Si la base ya tiene alguno de esos datos, el script falla sin tocar nada. Al terminar, aunque un
# paso falle, levanta datos, borra lo que creó y revisa que no quede nada. Termina con 0 si todos los
# pasos pasan y con 1 si alguno falla. Nunca imprime el token ni la contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly DATOS=backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba
readonly CARGAS=(estructura_organica carreras ciclos_academicos asignaturas_malla oferta_periodo
    docentes politica_carga_horaria cargos_autoridades proyectos_practicas matriculados_carrera)
readonly COORDINADOR_A=coordinacion.demo
readonly COORDINADOR_B=docente.coordinador.demo
readonly CONSOLIDA=consolidacion.demo
readonly PERIODO=2026-I
readonly PERIODO_OTRO=2026-II
readonly PARALELO_NUEVO=Z
readonly TEXTO='Dictó esta materia durante tres años seguidos'
readonly SEGUNDOS_DE_JUSTIFICAR=3
readonly SEGUNDOS_DE_ABRIR=5
readonly SEGUNDOS_SIN_DATOS=10
readonly MARGEN_OTRO=45

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
version_b="${prefijo}-MARGEN"

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
inicio=$(consultar_base "select now()")
IFS=$'\t' read -r carrera_a asignatura_n paralelo_n _ _ asignatura_h paralelo_h \
    <"$carpeta/a.tsv"
IFS=$'\t' read -r carrera_b _ _ <"$carpeta/b.tsv"

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
delete from normativa.parametro_regla where version_id in (select id from normativa.version_normativa where codigo in ('$version', '$version_b'));
delete from normativa.regla where version_id in (select id from normativa.version_normativa where codigo in ('$version', '$version_b'));
delete from normativa.fila_carga_horaria where version_id in (select id from normativa.version_normativa where codigo in ('$version', '$version_b'));
delete from normativa.version_normativa where codigo in ('$version', '$version_b');
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

# El periodo de las llamadas: cambia cuando se abre el del otro margen.
periodo=$PERIODO

sugerencia() {  # sugerencia <asignatura> <paralelo>: con el token de $token
    pedir GET "$GATEWAY/api/generacion/periodos/$periodo/oferta/$1/$2/sugerencia" "Bearer $token"
}

proponer() {  # proponer <asignatura> <paralelo> <docente> [texto]: con el token de $token
    pedir PUT "$GATEWAY/api/generacion/periodos/$periodo/oferta/$1/$2/propuesta" "Bearer $token" \
        "$(jq -cn --arg d "$3" --arg t "${4:-}" 'if $t == "" then {docente: $d} else {docente: $d, texto: $t} end')"
}

# Un 200 ya no significa guardado: sin la justificación llega una advertencia (F07-H03).
guardada() {
    [[ $estado == 200 && $(jq -r '.guardada' <<<"$cuerpo") == true ]]
}

crear() {  # crear <periodo>: con el token de $token
    pedir POST "$GATEWAY/api/generacion/periodos/$1/distributivo" "Bearer $token"
}

indicar() {  # indicar <usuario> <carrera>: con el token de $token
    pedir PUT "$GATEWAY/api/roles/usuarios/$1/carrera" "Bearer $token" "{\"carrera\":\"$2\"}"
}

asignar() {  # asignar <carrera> <docente> <actividad> <horas>: con el token de $token
    pedir PUT "$GATEWAY/api/generacion/periodos/$periodo/parciales/$1/docentes/$2/actividades/$3" \
        "Bearer $token" "$(jq -cn --argjson h "$4" '{horas: $h}')"
}

propuestas_del_paralelo() {  # propuestas_del_paralelo <asignatura> <paralelo>
    consultar_base "select count(*) from generacion.propuesta where asignatura = '$1' and paralelo = '$2'"
}

guardados() {  # cuántos cambios de propuesta quedaron en el registro
    consultar_base "select count(*) from generacion.registro_cambio where accion = 'PROPUESTA_GUARDADA'"
}

preparado() {
    local numero
    cargar || return 1
    iniciar vicerrectorado.demo
    numero=$(consultar_base "select numero from normativa.version_normativa where codigo = '$version'")
    pedir PUT "$GATEWAY/api/academico/periodos/$PERIODO/normativa" "Bearer $token" "{\"version\":${numero:-0}}"
    [[ $estado == 200 ]] || return 1
    iniciar admin.demo
    indicar "$COORDINADOR_A" "$carrera_a"
    [[ $estado == 200 ]] || return 1
    indicar "$COORDINADOR_B" "$carrera_b"
    [[ $estado == 200 ]] || return 1
    iniciar vicerrectorado.demo
    crear "$PERIODO"
    [[ $estado == 201 ]]
}

panel() {  # panel <periodo>: con el token de $token
    pedir GET "$GATEWAY/api/generacion/periodos/$1/inconsistencias" "Bearer $token"
}

justificar() {  # justificar <tipo> <objeto> <texto>: con el token de $token
    pedir PUT "$GATEWAY/api/generacion/periodos/$periodo/inconsistencias/justificacion" "Bearer $token" \
        "$(jq -cn --arg t "$1" --arg o "$2" --arg x "$3" '{tipo: $t, objeto: $o, texto: $x}')"
}

abiertas() {  # abiertas <tipo>: cuántas hay en el último panel
    jq --arg t "$1" '[.inconsistencias[] | select(.tipo == $t and .estado == "ABIERTA")] | length' <<<"$cuerpo"
}

esta() {  # esta <tipo> <objeto> <estado>: en el último panel
    jq -e --arg t "$1" --arg o "$2" --arg e "$3" \
        'any(.inconsistencias[]; .tipo == $t and .objeto.clave == $o and .estado == $e)' <<<"$cuerpo" >/dev/null
}

no_esta() {  # no_esta <tipo> <objeto>: en el último panel
    jq -e --arg t "$1" --arg o "$2" \
        'any(.inconsistencias[]; .tipo == $t and .objeto.clave == $o) | not' <<<"$cuerpo" >/dev/null
}

como_consolidacion() {
    iniciar "$CONSOLIDA"
    panel "$periodo"
    [[ $estado == 200 ]]
}

preparado() {
    local numero
    cargar || return 1
    iniciar vicerrectorado.demo
    numero=$(consultar_base "select numero from normativa.version_normativa where codigo = '$version'")
    pedir PUT "$GATEWAY/api/academico/periodos/$PERIODO/normativa" "Bearer $token" "{\"version\":${numero:-0}}"
    [[ $estado == 200 ]] || return 1
    iniciar admin.demo
    indicar "$COORDINADOR_A" "$carrera_a"
    [[ $estado == 200 ]] || return 1
    indicar "$COORDINADOR_B" "$carrera_b"
    [[ $estado == 200 ]] || return 1
    iniciar vicerrectorado.demo
    crear "$PERIODO"
    [[ $estado == 201 ]]
}

# El panel frente a un conteo hecho aparte, desde las tablas de cada servicio (SC-001).
igual_al_conteo() {
    local paralelos activos coordinadores gestores
    iniciar "$CONSOLIDA"
    panel "$PERIODO"
    echo "       abrir el panel tardó $segundos s"
    [[ $estado == 200 && $(jq -r '.creado' <<<"$cuerpo") == true ]] \
        && menos_de "$segundos" "$SEGUNDOS_DE_ABRIR" || return 1
    paralelos=$(consultar_base "select count(*) from academico.asignatura_paralelo where periodo = '$PERIODO' and estado = 'OFERTADA'")
    activos=$(consultar_base "select count(*) from docentes.docente where estado = 'ACTIVO'")
    coordinadores=$(consultar_base "select count(*) from (select distinct a.carrera from academico.asignatura_paralelo p
        join academico.asignatura a on a.codigo = p.asignatura where p.periodo = '$PERIODO' and p.estado = 'OFERTADA') c
        where (select count(*) from docentes.cargo g where g.tipo = 'COORDINADOR_CARRERA' and g.estado = 'VIGENTE'
            and g.carrera = c.carrera) <> 1")
    gestores=$(consultar_base "select count(*) from docentes.cargo where tipo ~ '^G[0-9]{2}_' and estado = 'VIGENTE'")
    echo "       aparte: $paralelos paralelos, $activos activos, $coordinadores carreras sin su coordinador, $gestores gestores"
    [[ $(abiertas PARALELO_SIN_PROPUESTA) == "$paralelos" && $(abiertas DOCENTE_SIN_CLASE) == "$activos" \
        && $(abiertas COORDINADORES_DE_LA_CARRERA) == "$coordinadores" ]] || return 1
    if ((gestores >= 8 && gestores <= 11)); then
        [[ $(abiertas GESTORES_FUERA_DE_RANGO) == 0 ]] || return 1
    else
        [[ $(abiertas GESTORES_FUERA_DE_RANGO) == 1 ]] || return 1
    fi
    [[ $(abiertas HORAS_SOBRE_LA_MALLA) == 0 && $(abiertas SOBRECARGA) == 0 && $(abiertas PROPORCION) == 0 ]] || return 1
    [[ $(jq -r '.abiertas.duras' <<<"$cuerpo") == $(jq '[.inconsistencias[] | select(.severidad == "DURA")] | length' <<<"$cuerpo") ]] || return 1
    jq -e '.inconsistencias[0].severidad == "DURA" and (.sinEvaluar | index("RN-03") != null)
        and all(.inconsistencias[]; (.indicacion | length) > 0 and (.detalle | length) > 0)' <<<"$cuerpo" >/dev/null
}

docente_afin=''
sin_clase_antes=0

corregir_desaparece() {
    local antes
    como_consolidacion || return 1
    antes=$(abiertas PARALELO_SIN_PROPUESTA)
    sin_clase_antes=$(abiertas DOCENTE_SIN_CLASE)
    iniciar "$COORDINADOR_A"
    sugerencia "$asignatura_h" "$paralelo_h"
    docente_afin=$(jq -r '[.candidatos[] | select(.motivoParaNoProponer == null)][0].identificacion // empty' <<<"$cuerpo")
    [[ -n $docente_afin ]] || return 1
    proponer "$asignatura_h" "$paralelo_h" "$docente_afin"
    guardada || return 1
    como_consolidacion || return 1
    [[ $(abiertas PARALELO_SIN_PROPUESTA) == $((antes - 1)) \
        && $(abiertas DOCENTE_SIN_CLASE) == $((sin_clase_antes - 1)) ]] || return 1
    no_esta PARALELO_SIN_PROPUESTA "$asignatura_h/$paralelo_h" && no_esta DOCENTE_SIN_CLASE "$docente_afin"
}

blandas_del_docente() {
    # Solo con clase: 100 % de clase, sin la planificación ni la evaluación.
    esta PROPORCION "$docente_afin" ABIERTA && esta ACTIVIDADES_OBLIGATORIAS "$docente_afin" ABIERTA
}

justificar_una_blanda() {
    local blandas filas
    como_consolidacion || return 1
    blandas_del_docente || return 1
    blandas=$(jq -r '.abiertas.blandas' <<<"$cuerpo")
    justificar PROPORCION "$docente_afin" "$TEXTO"
    echo "       justificar tardó $segundos s"
    [[ $estado == 200 && $(jq -r '.estado' <<<"$cuerpo") == JUSTIFICADA ]] \
        && menos_de "$segundos" "$SEGUNDOS_DE_JUSTIFICAR" || return 1
    panel "$PERIODO"
    esta PROPORCION "$docente_afin" JUSTIFICADA && esta ACTIVIDADES_OBLIGATORIAS "$docente_afin" ABIERTA || return 1
    [[ $(jq -r '.abiertas.blandas' <<<"$cuerpo") == $((blandas - 1)) ]] || return 1
    justificar PROPORCION "$docente_afin" "Otro motivo, ya revisado con la coordinación"
    [[ $estado == 200 ]] || return 1
    panel "$PERIODO"
    [[ $(jq -r --arg d "$docente_afin" '.inconsistencias[] | select(.tipo == "PROPORCION" and .objeto.clave == $d)
        | .justificacion.texto' <<<"$cuerpo") == 'Otro motivo, ya revisado con la coordinación' ]] || return 1
    filas=$(consultar_base "select count(*) from generacion.justificacion_inconsistencia where objeto = '$docente_afin'")
    [[ $filas == 2 ]] || return 1
    [[ $(consultar_base "select count(*) from generacion.registro_cambio where accion = 'INCONSISTENCIA_JUSTIFICADA'
        and detalle::text like '%$docente_afin%'") == 2 ]] || return 1
    [[ $(consultar_base "select count(*) from generacion.registro_cambio where detalle::text like '%tres años%'") == 0 ]]
}

lo_que_se_rechaza() {
    iniciar "$CONSOLIDA"
    justificar PARALELO_SIN_PROPUESTA "$asignatura_n/$paralelo_n" "$TEXTO"
    es_codigo 409 GENERACION_INCONSISTENCIA_DURA || return 1
    justificar EXCEPCION_DE_AFINIDAD "$asignatura_n/$paralelo_n" "$TEXTO"
    es_codigo 409 GENERACION_INCONSISTENCIA_DURA || return 1
    justificar PROPORCION 0000000000 "$TEXTO"
    es_codigo 409 GENERACION_INCONSISTENCIA_NO_ENCONTRADA || return 1
    justificar ACTIVIDADES_OBLIGATORIAS "$docente_afin" corto
    [[ $estado == 400 && $(jq -r '.detalles[0].campo' <<<"$cuerpo") == texto ]] || return 1
    panel "$PERIODO"
    esta ACTIVIDADES_OBLIGATORIAS "$docente_afin" ABIERTA
}

excepcion_de_rn19() {
    local bajo
    iniciar "$COORDINADOR_A"
    sugerencia "$asignatura_n" "$paralelo_n"
    bajo=$(jq -r '[.bajoElMinimo[] | select(.motivoParaNoProponer == null)][0].identificacion // empty' <<<"$cuerpo")
    [[ -n $bajo ]] || return 1
    proponer "$asignatura_n" "$paralelo_n" "$bajo" "$TEXTO"
    guardada || return 1
    como_consolidacion || return 1
    esta EXCEPCION_DE_AFINIDAD "$asignatura_n/$paralelo_n" JUSTIFICADA
}

# Lo que la API ya no deja crear: se prepara en la base y se deshace (SC-003).
cada_regla_aparece_y_desaparece() {
    local coordinador gestores horas version_id
    coordinador=$(consultar_base "select docente from docentes.cargo where tipo = 'COORDINADOR_CARRERA'
        and estado = 'VIGENTE' and carrera = '$carrera_a' limit 1")
    consultar_base "update docentes.cargo set estado = 'FINALIZADO' where docente = '$coordinador' and tipo = 'COORDINADOR_CARRERA'" >/dev/null
    como_consolidacion || return 1
    esta COORDINADORES_DE_LA_CARRERA "$carrera_a" ABIERTA || return 1
    consultar_base "update docentes.cargo set estado = 'VIGENTE' where docente = '$coordinador' and tipo = 'COORDINADOR_CARRERA'" >/dev/null
    panel "$PERIODO"
    no_esta COORDINADORES_DE_LA_CARRERA "$carrera_a" || return 1

    gestores=$(consultar_base "select string_agg(quote_literal(docente), ',') from (select docente from docentes.cargo
        where tipo ~ '^G[0-9]{2}_' and estado = 'VIGENTE' order by docente limit 2) g")
    consultar_base "update docentes.cargo set estado = 'FINALIZADO' where docente in ($gestores) and tipo ~ '^G[0-9]{2}_'" >/dev/null
    panel "$PERIODO"
    esta GESTORES_FUERA_DE_RANGO "$PERIODO" ABIERTA || return 1
    justificar GESTORES_FUERA_DE_RANGO "$PERIODO" "$TEXTO"
    [[ $estado == 200 ]] || return 1
    panel "$PERIODO"
    esta GESTORES_FUERA_DE_RANGO "$PERIODO" JUSTIFICADA || return 1
    consultar_base "update docentes.cargo set estado = 'VIGENTE' where docente in ($gestores) and tipo ~ '^G[0-9]{2}_'" >/dev/null
    panel "$PERIODO"
    no_esta GESTORES_FUERA_DE_RANGO "$PERIODO" || return 1

    # Como en la vida real: el paralelo indica sus horas y la asignatura se corrige después.
    horas=$(consultar_base "select horas_semanales_clase from academico.asignatura where codigo = '$asignatura_n'")
    consultar_base "update academico.asignatura_paralelo set horas_semanales = $horas where periodo = '$PERIODO'
        and asignatura = '$asignatura_n' and paralelo = '$paralelo_n';
        update academico.asignatura set horas_semanales_clase = $horas - 0.5 where codigo = '$asignatura_n'" >/dev/null
    panel "$PERIODO"
    esta HORAS_SOBRE_LA_MALLA "$carrera_a" ABIERTA || return 1
    consultar_base "update academico.asignatura set horas_semanales_clase = $horas where codigo = '$asignatura_n';
        update academico.asignatura_paralelo set horas_semanales = null where periodo = '$PERIODO'
        and asignatura = '$asignatura_n' and paralelo = '$paralelo_n'" >/dev/null
    panel "$PERIODO"
    no_esta HORAS_SOBRE_LA_MALLA "$carrera_a" || return 1

    version_id=$(consultar_base "select v.id from generacion.version_distributivo v
        join generacion.distributivo d on d.id = v.distributivo_id where d.periodo = '$PERIODO'")
    consultar_base "insert into generacion.actividad_no_lectiva (id, version_id, carrera, docente, nombre_docente,
        actividad, nombre_actividad, componente, horas, asignada_en, asignada_por)
        select gen_random_uuid(), '$version_id', '$carrera_a', '$docente_afin', 'PRUEBA', a.codigo, 'Una actividad',
        'GESTION', 40, now(), 'prueba' from (values ('143-k'), ('143-l')) as a (codigo)" >/dev/null
    panel "$PERIODO"
    esta SOBRECARGA "$docente_afin" ABIERTA || return 1
    consultar_base "delete from generacion.actividad_no_lectiva where docente = '$docente_afin' and asignada_por = 'prueba'" >/dev/null
    panel "$PERIODO"
    no_esta SOBRECARGA "$docente_afin"
}

los_permisos() {
    local cuantas
    como_consolidacion || return 1
    cuantas=$(jq '.inconsistencias | length' <<<"$cuerpo")
    iniciar vicerrectorado.demo
    panel "$PERIODO"
    [[ $estado == 200 && $(jq '.inconsistencias | length' <<<"$cuerpo") == "$cuantas" ]] || return 1
    justificar ACTIVIDADES_OBLIGATORIAS "$docente_afin" "$TEXTO"
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || return 1
    iniciar "$COORDINADOR_A"
    panel "$PERIODO"
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO
}

ofertar_en_el_otro() {  # ofertar_en_el_otro <asignatura> <jornada> <modalidad>
    pedir POST "$GATEWAY/api/academico/periodos/$PERIODO_OTRO/oferta" "Bearer $token" \
        "$(jq -cn --arg a "$1" --arg p "$PARALELO_NUEVO" --arg j "$2" --arg m "$3" \
            '{asignatura: $a, paralelo: $p, jornada: $j, modalidad: $m, estudiantes: 12}')"
    [[ $estado == 201 ]] || { echo "       ofertar $1: $estado" >&2; return 1; }
}

# Otra versión con otro margen, que abre el otro periodo: 2026-I no cambia de versión.
otro_margen() {
    local numero_a numero_b nueva edicion jornada modalidad
    iniciar vicerrectorado.demo
    jornada=$(consultar_base "select jornada from academico.asignatura_paralelo where periodo = '$PERIODO'
        and asignatura = '$asignatura_h' and paralelo = '$paralelo_h'")
    modalidad=$(consultar_base "select modalidad from academico.asignatura_paralelo where periodo = '$PERIODO'
        and asignatura = '$asignatura_h' and paralelo = '$paralelo_h'")
    numero_a=$(consultar_base "select numero from normativa.version_normativa where codigo = '$version'")
    pedir POST "$GATEWAY/api/normativa/versiones" "Bearer $token" \
        "$(jq -cn --argjson n "${numero_a:-0}" --arg c "$version_b" \
            '{copiarDe: $n, codigo: $c, origen: "Margen de prueba de F10-H01", vigenteDesde: "2026-01-15"}')"
    [[ $estado == 201 ]] || { echo "       crear $version_b: $estado" >&2; return 1; }
    nueva=$cuerpo
    numero_b=$(jq -r '.numero' <<<"$nueva")
    edicion=$(jq -c --argjson m "$MARGEN_OTRO" '{codigo, origen, vigenteDesde,
        reglas: [.reglas[] | {identificador, parametros}
            | if .identificador == "RN-05"
              then .parametros = [.parametros[] | if .nombre == "margen_puntos" then .valor = $m else . end]
              else . end],
        tablaCargaHoraria: [.tablaCargaHoraria[] | del(.severidad, .referencia, .observacion)]}' <<<"$nueva")
    pedir PUT "$GATEWAY/api/normativa/versiones/$numero_b" "Bearer $token" "$edicion"
    [[ $estado == 200 ]] || { echo "       editar $version_b: $estado" >&2; return 1; }
    pedir PUT "$GATEWAY/api/academico/periodos/$PERIODO_OTRO/normativa" "Bearer $token" "{\"version\":$numero_b}"
    [[ $estado == 200 ]] || return 1
    ofertar_en_el_otro "$asignatura_h" "$jornada" "$modalidad" || return 1
    crear "$PERIODO_OTRO"
    [[ $estado == 201 ]] || return 1
    periodo=$PERIODO_OTRO
    iniciar "$COORDINADOR_A"
    proponer "$asignatura_h" "$PARALELO_NUEVO" "$docente_afin"
    guardada || return 1
    como_consolidacion || return 1
    # Tiene 100 % de clase: con margen 45 cumple, y con el de 2026-I no.
    no_esta PROPORCION "$docente_afin" || return 1
    periodo=$PERIODO
    panel "$PERIODO"
    esta PROPORCION "$docente_afin" JUSTIFICADA
}

sin_el_proceso_datos() {
    local bien
    iniciar "$CONSOLIDA"
    docker compose stop datos >/dev/null 2>&1
    panel "$PERIODO"
    echo "       sin datos respondió $estado en $segundos s"
    bien=0
    if [[ $estado == 502 || $estado == 504 ]] && [[ $(jq -r '.codigo' <<<"$cuerpo") == COMUN_PROCESO_* ]] \
        && menos_de "$segundos" "$SEGUNDOS_SIN_DATOS"; then
        bien=1
    fi
    docker compose up -d --wait --no-recreate --no-build datos >/dev/null 2>&1 || return 1
    como_consolidacion || return 1
    [[ $bien == 1 ]]
}

verificar 1 "Las 10 cargas, $PERIODO abierto y su distributivo creado" preparado
verificar 2 "El panel coincide con un conteo aparte, en menos de $SEGUNDOS_DE_ABRIR s, y primero las duras" igual_al_conteo
verificar 3 "Al proponer, el paralelo y el docente desaparecen del panel" corregir_desaparece
verificar 4 "Una blanda se justifica y se cambia: sigue visible, el registro no lleva el texto" justificar_una_blanda
verificar 5 "Una dura, RN-19, lo que ya no existe y el texto corto se rechazan" lo_que_se_rechaza
verificar 6 "Una excepción de RN-19 sale como justificada" excepcion_de_rn19
verificar 7 "RN-12, RN-10, RN-18 y la sobrecarga aparecen y desaparecen" cada_regla_aparece_y_desaparece
verificar 8 "Consolidación y el Vicerrectorado leen lo mismo; solo Consolidación justifica; un coordinador no entra" los_permisos
verificar 9 "$PERIODO_OTRO abierto con otro margen: se aplica el de su versión" otro_margen
verificar 10 "Con datos detenido: el error del catálogo en menos de $SEGUNDOS_SIN_DATOS s, y vuelve" sin_el_proceso_datos

if ((fallas > 0)); then
    echo "El panel de inconsistencias de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "El panel de inconsistencias de punta a punta: todos los pasos pasan."
