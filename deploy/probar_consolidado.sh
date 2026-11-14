#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Consolidar y el informe sustentado de punta a punta sobre el perfil levantado, con los procesos
# reales (F10-H02; quickstart.md de specs/037-consolidar-parciales).
# Uso, desde cualquier directorio: deploy/probar_consolidado.sh
# Con una base sin estructura orgánica, carreras, distributivos ni los datos de prueba: carga las 10
# cargas de prueba (ficticias, RNF-SEG-06), abre 2026-I y crea su distributivo. Compara el consolidado
# con una suma aparte, consolida, prueba el aviso de desactualizado y adjunta un PDF de 10 MB, por el
# API Gateway y por nginx. Rechaza lo que no es un PDF, daña un archivo en el volumen, recrea el
# proceso y detiene datos. Si la base ya tiene alguno de esos datos, el script falla sin tocar nada.
# Al terminar, aunque un paso falle, levanta datos, borra lo que creó en la base y en el volumen, y
# revisa que no quede nada. Termina con 0 si todos los pasos pasan y con 1 si alguno falla. Nunca
# imprime el token, la contraseña ni el contenido de un archivo.
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
readonly SPA="${MDD_SPA:-http://localhost:8081}"
readonly TEXTO='Dictó esta materia durante tres años seguidos'
readonly SEGUNDOS_DE_ABRIR=5
readonly SEGUNDOS_DE_ADJUNTAR=10
readonly SEGUNDOS_SIN_DATOS=10
readonly DIEZ_MB=10485760

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
inicio=$(consultar_base "select now()")
IFS=$'\t' read -r carrera_a _ _ _ _ asignatura_h paralelo_h \
    <"$carpeta/a.tsv"
IFS=$'\t' read -r carrera_b _ _ <"$carpeta/b.tsv"

limpiar() {
    docker compose up -d --wait --no-recreate --no-build datos >/dev/null 2>&1 \
        || echo "AVISO  No se pudo volver a levantar el proceso datos"
    docker compose exec -T distributivo sh -c 'rm -rf /var/mdd/archivos/*' \
        || echo "AVISO  No se pudo vaciar el almacén de archivos"
    consultar_base "delete from generacion.actividad_no_lectiva where version_id in (select v.id from generacion.version_distributivo v
    join generacion.distributivo d on d.id = v.distributivo_id where d.periodo in ($periodos));
delete from generacion.propuesta where version_id in (select v.id from generacion.version_distributivo v
    join generacion.distributivo d on d.id = v.distributivo_id where d.periodo in ($periodos));
delete from generacion.parcial_carrera where version_id in (select v.id from generacion.version_distributivo v
    join generacion.distributivo d on d.id = v.distributivo_id where d.periodo in ($periodos));
delete from generacion.version_distributivo where distributivo_id in (select id from generacion.distributivo where periodo in ($periodos));
delete from generacion.distributivo where periodo in ($periodos);
delete from validacionocs.proceso_aprobacion where periodo in ($periodos);
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
    pedir PUT "$GATEWAY/api/generacion/periodos/$PERIODO/parciales/$1/docentes/$2/actividades/$3" \
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

consolidado() {  # con el token de $token
    pedir GET "$GATEWAY/api/generacion/periodos/$PERIODO/consolidado" "Bearer $token"
}

consolidar() {  # con el token de $token
    pedir POST "$GATEWAY/api/generacion/periodos/$PERIODO/consolidado" "Bearer $token"
}

# adjuntar <archivo> [base]: con el token de $token. Deja $estado, $cuerpo y $segundos.
adjuntar() {
    local salida resultado
    salida=$(mktemp -p "$carpeta")
    resultado=$(curl -s -o "$salida" -w '%{http_code} %{time_total}' -X PUT \
        -H "Authorization: Bearer $token" \
        -F "archivo=@$1;type=application/pdf;filename=informe.pdf" \
        "${2:-$GATEWAY}/api/generacion/periodos/$PERIODO/consolidado/informe" || echo '000 0')
    estado=${resultado%% *}
    segundos=${resultado##* }
    cuerpo=$(cat "$salida")
}

# descargar <destino>: con el token de $token. Deja $estado y, si falla, el error en $cuerpo.
descargar() {
    estado=$(curl -s -o "$1" -w '%{http_code}' -H "Authorization: Bearer $token" \
        "$GATEWAY/api/generacion/periodos/$PERIODO/consolidado/informe" || echo '000')
    cuerpo=$(head -c 300 "$1" | tr -d '\000')
}

# hacer_pdf <destino> <bytes>: un PDF mínimo con ese tamaño exacto.
hacer_pdf() {
    python3 -I - "$1" "$2" <<'PY'
import sys
destino, largo = sys.argv[1], int(sys.argv[2])
inicio, fin = b'%PDF-1.7\n', b'\n%%EOF\n'
with open(destino, 'wb') as archivo:
    archivo.write(inicio + b' ' * (largo - len(inicio) - len(fin)) + fin)
PY
}

huella() {
    sha256sum "$1" | cut -d' ' -f1
}

clave_vigente() {
    consultar_base "select i.clave from generacion.informe_sustentado i
        join generacion.version_distributivo v on v.id = i.version_id
        join generacion.distributivo d on d.id = v.distributivo_id
        where d.periodo = '$PERIODO' order by i.adjuntado_en desc limit 1"
}

filas_del_informe() {
    consultar_base "select count(*) from generacion.informe_sustentado"
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

# El consolidado frente a una suma hecha aparte, desde las tablas de Académico (SC-001).
igual_a_la_suma() {
    local aparte panel_ahora
    iniciar "$CONSOLIDA"
    consolidado
    echo "       abrir el consolidado tardó $segundos s"
    [[ $estado == 200 && $(jq -r '.creado' <<<"$cuerpo") == true ]] \
        && menos_de "$segundos" "$SEGUNDOS_DE_ABRIR" || return 1
    aparte=$(consultar_base "select a.carrera || ':' || count(*) from academico.asignatura_paralelo p
        join academico.asignatura a on a.codigo = p.asignatura
        where p.periodo = '$PERIODO' and p.estado = 'OFERTADA' group by a.carrera order by a.carrera")
    [[ $(jq -r '.carreras | sort_by(.carrera)[] | "\(.carrera):\(.paralelos)"' <<<"$cuerpo") == "$aparte" ]] || return 1
    echo "       aparte: $(wc -l <<<"$aparte") carreras"
    jq -e 'all(.carreras[]; .estado == "EN_ELABORACION" and .propuestos == 0 and .completo == false)
        and .desactualizado == false and (.consolidacion == null) and (.informe == null)
        and .totales.clase == 0' <<<"$cuerpo" >/dev/null || return 1
    panel_ahora=$cuerpo
    pedir GET "$GATEWAY/api/generacion/periodos/$PERIODO/inconsistencias" "Bearer $token"
    [[ $(jq -c '.abiertas' <<<"$cuerpo") == $(jq -c '.abiertas' <<<"$panel_ahora") ]]
}

sin_consolidar_no_se_adjunta() {
    hacer_pdf "$carpeta/corto.pdf" 2048
    adjuntar "$carpeta/corto.pdf"
    es_codigo 409 GENERACION_SIN_CONSOLIDAR
}

consolidar_y_repetir() {
    consolidar
    [[ $estado == 200 && $(jq -r '.consolidacion.por' <<<"$cuerpo") == "$CONSOLIDA" ]] || return 1
    jq -e '.abiertas.duras > 0' <<<"$cuerpo" >/dev/null || return 1
    consolidar
    [[ $estado == 200 ]] || return 1
    [[ $(consultar_base "select count(*) from generacion.consolidacion") == 2 ]] || return 1
    [[ $(consultar_base "select count(*) from generacion.registro_cambio where accion = 'DISTRIBUTIVO_CONSOLIDADO'") == 2 ]]
}

docente_afin=''

el_aviso_de_desactualizado() {
    local horas
    iniciar "$COORDINADOR_A"
    sugerencia "$asignatura_h" "$paralelo_h"
    horas=$(jq -r '.horasDelParalelo' <<<"$cuerpo")
    docente_afin=$(jq -r '[.candidatos[] | select(.motivoParaNoProponer == null)][0].identificacion // empty' <<<"$cuerpo")
    [[ -n $docente_afin ]] || return 1
    proponer "$asignatura_h" "$paralelo_h" "$docente_afin"
    guardada || return 1
    iniciar "$CONSOLIDA"
    consolidado
    [[ $(jq -r '.desactualizado' <<<"$cuerpo") == true ]] || return 1
    jq -e --argjson h "$horas" '.totales.clase == $h' <<<"$cuerpo" >/dev/null || return 1
    jq -e --arg c "$carrera_a" '[.carreras[] | select(.carrera == $c)][0].propuestos == 1' <<<"$cuerpo" >/dev/null || return 1
    consolidar
    [[ $estado == 200 && $(jq -r '.desactualizado' <<<"$cuerpo") == false ]] || return 1
    # Justificar no cambia una asignación: no lo deja desactualizado.
    pedir PUT "$GATEWAY/api/generacion/periodos/$PERIODO/inconsistencias/justificacion" "Bearer $token" \
        "$(jq -cn --arg o "$docente_afin" --arg t "$TEXTO" '{tipo: "PROPORCION", objeto: $o, texto: $t}')"
    [[ $estado == 200 ]] || return 1
    consolidado
    [[ $(jq -r '.desactualizado' <<<"$cuerpo") == false ]]
}

diez_mb_por_el_gateway_y_por_nginx() {
    hacer_pdf "$carpeta/diez.pdf" "$DIEZ_MB"
    adjuntar "$carpeta/diez.pdf"
    echo "       adjuntar 10 MB tardó $segundos s"
    [[ $estado == 200 ]] && menos_de "$segundos" "$SEGUNDOS_DE_ADJUNTAR" || return 1
    [[ $(jq -r '.huella' <<<"$cuerpo") == $(huella "$carpeta/diez.pdf") && $(jq -r '.tamano' <<<"$cuerpo") == "$DIEZ_MB" ]] || return 1
    descargar "$carpeta/bajado.pdf"
    [[ $estado == 200 ]] && cmp -s "$carpeta/diez.pdf" "$carpeta/bajado.pdf" || return 1
    adjuntar "$carpeta/diez.pdf" "$SPA"
    echo "       por nginx respondió $estado en $segundos s"
    [[ $estado == 200 ]] || return 1
    consolidado
    [[ $(jq -r '.desactualizado' <<<"$cuerpo") == false && $(jq -r '.informe.nombre' <<<"$cuerpo") == informe.pdf ]]
}

lo_que_no_es_un_pdf() {
    local antes
    antes=$(filas_del_informe)
    printf 'esto no es un PDF' >"$carpeta/texto.pdf"
    printf '\211PNG\r\n\032\n' >"$carpeta/imagen.pdf"
    : >"$carpeta/vacio.pdf"
    for archivo in texto imagen vacio; do
        adjuntar "$carpeta/$archivo.pdf"
        es_codigo 400 GENERACION_INFORME_NO_ES_PDF || { echo "       $archivo: $estado" >&2; return 1; }
    done
    hacer_pdf "$carpeta/grande.pdf" $((DIEZ_MB + 1))
    adjuntar "$carpeta/grande.pdf"
    es_codigo 413 GENERACION_INFORME_DEMASIADO_GRANDE || return 1
    hacer_pdf "$carpeta/enorme.pdf" $((12 * 1024 * 1024))
    adjuntar "$carpeta/enorme.pdf"
    es_codigo 413 COMUN_CONTENIDO_DEMASIADO_GRANDE || return 1
    [[ $(filas_del_informe) == "$antes" ]]
}

reemplazar_conserva() {
    local antes
    antes=$(filas_del_informe)
    adjuntar "$carpeta/corto.pdf"
    [[ $estado == 200 && $(filas_del_informe) == $((antes + 1)) ]] || return 1
    descargar "$carpeta/bajado.pdf"
    [[ $estado == 200 ]] && cmp -s "$carpeta/corto.pdf" "$carpeta/bajado.pdf"
}

un_archivo_danado() {
    local clave
    clave=$(clave_vigente)
    docker compose exec -T distributivo sh -c "printf x >> /var/mdd/archivos/$clave"
    descargar "$carpeta/danado.pdf"
    es_codigo 500 GENERACION_INFORME_ILEGIBLE || return 1
    docker compose exec -T distributivo sh -c "rm -f /var/mdd/archivos/$clave"
    descargar "$carpeta/danado.pdf"
    es_codigo 500 GENERACION_INFORME_ILEGIBLE || return 1
    adjuntar "$carpeta/corto.pdf"
    [[ $estado == 200 ]]
}

sobrevive_a_recrear_el_proceso() {
    docker compose up -d --wait --force-recreate --no-build distributivo >/dev/null 2>&1 || return 1
    iniciar "$CONSOLIDA"
    descargar "$carpeta/bajado.pdf"
    [[ $estado == 200 ]] && cmp -s "$carpeta/corto.pdf" "$carpeta/bajado.pdf"
}

los_permisos() {
    iniciar vicerrectorado.demo
    consolidado
    [[ $estado == 200 ]] || return 1
    descargar "$carpeta/bajado.pdf"
    [[ $estado == 200 ]] || return 1
    consolidar
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || return 1
    adjuntar "$carpeta/corto.pdf"
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || return 1
    iniciar "$COORDINADOR_A"
    consolidado
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || return 1
    consolidar
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || return 1
    adjuntar "$carpeta/corto.pdf"
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || return 1
    descargar "$carpeta/bajado.pdf"
    [[ $estado == 403 ]]
}

sin_el_proceso_datos() {
    local bien
    iniciar "$CONSOLIDA"
    docker compose stop datos >/dev/null 2>&1
    consolidado
    echo "       sin datos respondió $estado en $segundos s"
    bien=0
    if [[ $estado == 502 || $estado == 504 ]] && [[ $(jq -r '.codigo' <<<"$cuerpo") == COMUN_PROCESO_* ]] \
        && menos_de "$segundos" "$SEGUNDOS_SIN_DATOS"; then
        bien=1
    fi
    docker compose up -d --wait --no-recreate --no-build datos >/dev/null 2>&1 || return 1
    iniciar "$CONSOLIDA"
    consolidado
    [[ $bien == 1 && $estado == 200 ]]
}

el_contenido_no_va_a_los_registros() {
    [[ $(docker compose logs --no-color distributivo 2>/dev/null | grep -c '%PDF-' || true) == 0 ]]
}

verificar 1 "Las 10 cargas, $PERIODO abierto y su distributivo creado" preparado
verificar 2 "El consolidado coincide con una suma aparte, en menos de $SEGUNDOS_DE_ABRIR s, y con el panel" igual_a_la_suma
verificar 3 "Sin consolidar no se adjunta" sin_consolidar_no_se_adjunta
verificar 4 "Consolidar deja quién y cuándo, admite abiertas y se repite" consolidar_y_repetir
verificar 5 "Una propuesta lo deja desactualizado; repetir lo arregla; justificar no" el_aviso_de_desactualizado
verificar 6 "Un PDF de 10 MB, por el API Gateway y por nginx, se descarga idéntico" diez_mb_por_el_gateway_y_por_nginx
verificar 7 "Texto, imagen, vacío y más de 10 MB se rechazan, sin guardar" lo_que_no_es_un_pdf
verificar 8 "Reemplazar conserva el anterior" reemplazar_conserva
verificar 9 "Un archivo cambiado o que falta da el error del catálogo, nunca un archivo dañado" un_archivo_danado
verificar 10 "El informe sobrevive a recrear el proceso" sobrevive_a_recrear_el_proceso
verificar 11 "El Vicerrectorado lee y descarga, sin escribir; un coordinador recibe 403" los_permisos
verificar 12 "Con datos detenido: el error del catálogo en menos de $SEGUNDOS_SIN_DATOS s, y vuelve" sin_el_proceso_datos
verificar 13 "El contenido del archivo no aparece en los registros" el_contenido_no_va_a_los_registros

if ((fallas > 0)); then
    echo "Consolidar de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "Consolidar de punta a punta: todos los pasos pasan."
