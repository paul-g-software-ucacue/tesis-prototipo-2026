#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# El puntaje de afinidad de punta a punta sobre el perfil levantado, con los dos procesos reales
# (F06-H01; quickstart.md de specs/028-puntaje-afinidad).
# Uso, desde cualquier directorio: deploy/probar_afinidad.sh
# Con una base sin estructura orgánica, carreras ni los datos de prueba: carga las cargas 01 a 07 de
# prueba (ficticias, RNF-SEG-06) y abre 2026-I. Compara el puntaje de los 47 docentes en toda la
# oferta con una suma hecha aparte, sin el código de Generación; abre 2026-II con otra versión de
# pesos; prueba un periodo sin abrir, uno sin oferta, los errores, los permisos y las rutas internas;
# mide la lista; detiene el proceso datos y lo vuelve a levantar, y busca la ficha en los registros.
# La política entra como una versión nueva, con un código con prefijo al azar, para no tocar la
# versión 1. Si la base ya tiene alguno de esos datos, el script falla sin tocar nada. Al terminar,
# aunque un paso falle, levanta datos y borra en la base solo lo que creó y sus registros. Termina con
# 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime el token ni la contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly DATOS=backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba
readonly CARGAS=(estructura_organica carreras ciclos_academicos asignaturas_malla oferta_periodo
    politica_carga_horaria docentes)
readonly ARCHIVOS=(01_estructura_organica 02_carreras 03_ciclos_academicos 04_asignaturas_malla
    05_oferta_periodo 07_politica_carga_horaria 06_docentes)
readonly DOCENTE=0109990010
readonly TITULO_DE_LA_FICHA='MAGÍSTER EN CONTABILIDAD Y FINANZAS'
readonly SEGUNDOS_DE_LA_LISTA=3
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

# Cada código de prueba nuevo lleva este prefijo: P y tres dígitos.
prefijo="P$(printf '%03d' $((RANDOM % 1000)))"
version="${prefijo}-POL-2024"
version_b="${prefijo}-PESOS"

# --- Los archivos: los de prueba, con la política con el código nuevo ---
python3 -I - "$DATOS" "$carpeta" "$version" "${ARCHIVOS[@]}" <<'PY'
import csv, io, sys
datos, carpeta, version, *nombres = sys.argv[1:]

def leer(nombre):
    with open(f'{datos}/{nombre}', encoding='utf-8-sig', newline='') as archivo:
        filas = list(csv.reader(archivo))
    return filas[0], filas[1:]

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

def lista(nombre, archivo, campo):
    encabezados, filas = leer(archivo)
    i = encabezados.index(campo)
    valores = sorted({fila[i] for fila in filas if fila[i]})
    with open(f'{carpeta}/{nombre}.lista', 'w') as salida:
        salida.write(','.join("'" + v + "'" for v in valores))
    with open(f'{carpeta}/{nombre}.valores', 'w') as salida:
        salida.write('\n'.join(valores) + '\n')

lista('unidades', '01_estructura_organica.csv', 'codigo_unidad')
lista('carreras', '02_carreras.csv', 'codigo_carrera')
lista('periodos', '03_ciclos_academicos.csv', 'codigo_periodo')
lista('docentes', '06_docentes.csv', 'numero_identificacion')
lista('asignaturas_2026-I', '05_oferta_periodo.csv', 'codigo_asignatura')

encabezados, filas = leer('05_oferta_periodo.csv')
primera = dict(zip(encabezados, filas[0]))
with open(f'{carpeta}/primera_oferta.tsv', 'w') as salida:
    salida.write('\t'.join([primera['codigo_asignatura'], primera['paralelo'], primera['jornada'],
                            primera['modalidad']]) + '\n')
PY
unidades=$(cat "$carpeta/unidades.lista")
carreras=$(cat "$carpeta/carreras.lista")
# Con el periodo sin oferta del paso 5, para que también se borre.
periodos="$(cat "$carpeta/periodos.lista"),'$prefijo'"
docentes=$(cat "$carpeta/docentes.lista")
inicio=$(consultar_base "select now()")
IFS=$'\t' read -r asignatura_b paralelo_b jornada_b modalidad_b <"$carpeta/primera_oferta.tsv"

limpiar() {
    docker compose up -d --wait --no-recreate --no-build datos >/dev/null 2>&1 \
        || echo "AVISO  No se pudo volver a levantar el proceso datos"
    consultar_base "delete from docentes.cargo where docente in ($docentes);
delete from docentes.docente where identificacion in ($docentes);
delete from docentes.registro_cambio where momento >= '$inicio' and usuario in ('datos.demo', 'vicerrectorado.demo');
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
delete from roles.periodo_conocido where codigo in ($periodos);
delete from roles.registro_cambio where detalle ->> 'periodo' in ($periodos);
delete from roles.evento_recibido where id_evento in (select (serialized_event::jsonb ->> 'idEvento')::uuid
    from eventos_datos.event_publication where serialized_event::jsonb -> 'contenido' ->> 'codigo' in ($periodos));
delete from eventos_datos.event_publication where serialized_event::jsonb -> 'contenido' ->> 'codigo' in ($periodos);" >/dev/null 2>&1 \
        || echo "AVISO  No se pudo borrar lo creado por el script"
    rm -rf "$carpeta"
}

# --- 0. La base no tiene estructura, carreras ni los datos de prueba ---
ya_estan=$(consultar_base "select (select count(*) from academico.unidad_organica where activa or codigo in ($unidades))
    + (select count(*) from academico.carrera where estado = 'VIGENTE' or codigo in ($carreras))
    + (select count(*) from academico.periodo_academico where codigo in ($periodos))
    + (select count(*) from docentes.docente where identificacion in ($docentes))")
if [[ $ya_estan != 0 ]]; then
    echo "FALLA  0. La base ya tiene $ya_estan unidades, carreras o datos de prueba: no se tocan."
    rm -rf "$carpeta"
    exit 1
fi
trap limpiar EXIT

iniciar datos.demo
if [[ -z $token ]]; then
    echo "FALLA  0. datos.demo no inicia sesión: ¿está levantado el perfil?"
    exit 1
fi
echo "OK     0. La base no tiene estructura ni carreras; la política entra como la versión $version"

# Las cargas, en orden.
cargar() {
    local i archivo huella
    for i in "${!CARGAS[@]}"; do
        archivo="$carpeta/${ARCHIVOS[$i]}.csv"
        subir "${CARGAS[$i]}" "$archivo"
        huella=$(jq -r '.vistaPrevia.huella // empty' <<<"$cuerpo")
        [[ $estado == 200 && $(jq -r '.errores' <<<"$cuerpo") == 0 && -n $huella ]] \
            || { echo "       ${CARGAS[$i]}: la vista previa falló" >&2; return 1; }
        confirmar "${CARGAS[$i]}" "$archivo" "$huella"
        [[ $estado == 200 && $(jq -r '.resultado' <<<"$cuerpo") == APLICADA ]] \
            || { echo "       ${CARGAS[$i]}: la confirmación falló" >&2; return 1; }
    done
}

afinidad() {  # afinidad <docente> <periodo>: con el token de $token
    pedir GET "$GATEWAY/api/generacion/docentes/$1/afinidad?periodo=$2" "Bearer $token"
}

abrir() {  # abrir <periodo> <número de versión>: con el token de $token
    pedir PUT "$GATEWAY/api/academico/periodos/$1/normativa" "Bearer $token" "{\"version\":${2:-0}}"
    [[ $estado == 200 ]]
}

# Los pesos RN-24 de una versión, en un archivo.
guardar_pesos() {  # guardar_pesos <número> <archivo>: con el token de $token
    pedir GET "$GATEWAY/api/normativa/versiones/$1" "Bearer $token"
    [[ $estado == 200 ]] || return 1
    jq -c '.reglas[] | select(.identificador == "RN-24")
        | .parametros | map({(.nombre): .valor}) | add' <<<"$cuerpo" >"$2"
}

# La suma hecha aparte del código de Generación, con Python y los CSV de prueba: compara cada
# puntaje, que el desglose sume el total, y el orden.
comparar_con_la_suma_aparte() {  # comparar_con_la_suma_aparte <pesos> <respuestas> <asignaturas>
    python3 -I - "$carpeta" "$1" "$2" "$3" <<'PY'
import csv, glob, json, os, sys
from decimal import Decimal
carpeta, archivo_pesos, respuestas, archivo_ofertadas = sys.argv[1:]
pesos = {clave: Decimal(str(valor)) for clave, valor in json.load(open(archivo_pesos)).items()}

def filas(nombre):
    with open(f'{carpeta}/{nombre}', encoding='utf-8', newline='') as archivo:
        return list(csv.DictReader(archivo))

asignaturas = {fila['codigo_asignatura']: fila for fila in filas('04_asignaturas_malla.csv')}
ofertadas = set(open(archivo_ofertadas).read().split())
docentes = {fila['numero_identificacion']: fila for fila in filas('06_docentes.csv')}

def elementos(texto):
    return [[parte.strip() for parte in uno.split('|')] for uno in texto.split(';') if uno.strip()]

def puntaje(docente, asignatura):
    especifico, amplio = asignatura['campo_especifico'], asignatura['campo_amplio']
    total = Decimal(0)
    if docente['campo_especifico_3n'] == especifico:
        total += pesos['tercer_nivel_campo_especifico']
    elif docente['campo_amplio_3n'] == amplio:
        total += pesos['tercer_nivel_campo_amplio']
    for titulo in elementos(docente['titulos_cuarto_nivel']):
        if titulo[2] == especifico:
            total += pesos['cuarto_nivel_campo_especifico']
        elif titulo[2].startswith(amplio):
            total += pesos['cuarto_nivel_campo_amplio']
    for cargo in elementos(docente['experiencia_laboral']):
        if cargo[2] == especifico:
            total += pesos['experiencia_relacionada']
    cursos = sum(1 for curso in elementos(docente['cursos_capacitaciones']) if curso[1] == especifico)
    return total + min(cursos * pesos['curso_relacionado'], pesos['tope_cursos'])

errores = 0
puntajes = 0
for ruta in glob.glob(f'{respuestas}/*.json'):
    identificacion = os.path.basename(ruta)[:-5]
    lista = json.load(open(ruta), parse_float=Decimal, parse_int=Decimal)['puntajes']
    if {uno['asignatura'] for uno in lista} != ofertadas or len(lista) != len(ofertadas):
        print(f'       {identificacion}: las asignaturas no son las de la oferta', file=sys.stderr)
        errores += 1
    totales = [uno['total'] for uno in lista]
    if totales != sorted(totales, reverse=True):
        print(f'       {identificacion}: no va del mayor al menor', file=sys.stderr)
        errores += 1
    for uno in lista:
        puntajes += 1
        esperado = puntaje(docentes[identificacion], asignaturas[uno['asignatura']])
        if uno['total'] != esperado or sum(a['aporte'] for a in uno['desglose']) != esperado:
            print(f"       {identificacion} en {uno['asignatura']}: {uno['total']}, no {esperado}",
                  file=sys.stderr)
            errores += 1
print(puntajes)
sys.exit(1 if errores or not puntajes else 0)
PY
}

abierto_y_cargado() {
    cargar || return 1
    iniciar vicerrectorado.demo
    numero_a=$(consultar_base "select numero from normativa.version_normativa where codigo = '$version'")
    abrir 2026-I "$numero_a" || return 1
    guardar_pesos "$numero_a" "$carpeta/pesos.json"
}

# 47 docentes por toda la oferta, comparados con la suma aparte (SC-001).
todos_los_docentes() {
    local uno
    iniciar vicerrectorado.demo
    mkdir "$carpeta/respuestas"
    while read -r uno; do
        afinidad "$uno" 2026-I
        [[ $estado == 200 ]] || return 1
        printf '%s' "$cuerpo" >"$carpeta/respuestas/$uno.json"
    done <"$carpeta/docentes.valores"
    [[ $(find "$carpeta/respuestas" -name '*.json' | wc -l) == 47 ]] || return 1
    comparados=$(comparar_con_la_suma_aparte "$carpeta/pesos.json" "$carpeta/respuestas" \
        "$carpeta/asignaturas_2026-I.valores")
}

# El desglose nombra qué título aportó, con el peso de la versión.
el_desglose_lo_dice() {
    jq -e --arg t "$TITULO_DE_LA_FICHA" --slurpfile p "$carpeta/pesos.json" '[.puntajes[].desglose[]
        | select(.tipo == "CUARTO_NIVEL" and .nombre == $t and .coincidencia == "CAMPO_ESPECIFICO"
            and .aporte == $p[0].cuarto_nivel_campo_especifico)] | length > 0' \
        "$carpeta/respuestas/$DOCENTE.json" >/dev/null
}

sin_abrir() {
    afinidad "$DOCENTE" 2026-II
    es_codigo 409 GENERACION_PERIODO_SIN_ABRIR
}

# Otra versión con otros pesos, que abre 2026-II: el mismo docente tiene otro puntaje (SC-002).
otros_pesos() {
    local version_nueva numero_b edicion
    iniciar vicerrectorado.demo
    pedir POST "$GATEWAY/api/academico/periodos/2026-II/oferta" "Bearer $token" \
        "$(jq -cn --arg a "$asignatura_b" --arg p "$paralelo_b" --arg j "$jornada_b" --arg m "$modalidad_b" \
            '{asignatura: $a, paralelo: $p, jornada: $j, modalidad: $m, estudiantes: 12}')"
    [[ $estado == 201 ]] || { echo "       ofertar en 2026-II: $estado" >&2; return 1; }
    pedir POST "$GATEWAY/api/normativa/versiones" "Bearer $token" \
        "$(jq -cn --argjson n "${numero_a:-0}" --arg c "$version_b" \
            '{copiarDe: $n, codigo: $c, origen: "Pesos de prueba de F06-H01", vigenteDesde: "2026-01-15"}')"
    [[ $estado == 201 ]] || { echo "       crear $version_b: $estado" >&2; return 1; }
    version_nueva=$cuerpo
    numero_b=$(jq -r '.numero' <<<"$version_nueva")
    edicion=$(jq -c '{codigo, origen, vigenteDesde,
        reglas: [.reglas[] | {identificador, parametros}
            | if .identificador == "RN-24" then .parametros = ({
                cuarto_nivel_campo_especifico: 7.5, cuarto_nivel_campo_amplio: 4,
                tercer_nivel_campo_especifico: 5.25, tercer_nivel_campo_amplio: 3,
                experiencia_relacionada: 2.5, curso_relacionado: 1.5, tope_cursos: 4}
                | to_entries | map({nombre: .key, valor: .value}))
              else . end],
        tablaCargaHoraria: [.tablaCargaHoraria[] | del(.severidad, .referencia, .observacion)]}' \
        <<<"$version_nueva")
    pedir PUT "$GATEWAY/api/normativa/versiones/$numero_b" "Bearer $token" "$edicion"
    [[ $estado == 200 ]] || { echo "       editar $version_b: $estado" >&2; return 1; }
    abrir 2026-II "$numero_b" || return 1
    guardar_pesos "$numero_b" "$carpeta/pesos_b.json" || return 1
    iniciar vicerrectorado.demo
    mkdir "$carpeta/respuestas_b"
    afinidad "$DOCENTE" 2026-II
    [[ $estado == 200 && $(jq -r '.version' <<<"$cuerpo") == "$numero_b" ]] || return 1
    printf '%s' "$cuerpo" >"$carpeta/respuestas_b/$DOCENTE.json"
    printf '%s\n' "$asignatura_b" >"$carpeta/asignaturas_2026-II.valores"
    comparar_con_la_suma_aparte "$carpeta/pesos_b.json" "$carpeta/respuestas_b" \
        "$carpeta/asignaturas_2026-II.valores" >/dev/null
}

sin_oferta() {
    iniciar vicerrectorado.demo
    pedir POST "$GATEWAY/api/academico/periodos" "Bearer $token" \
        '{"codigo":"'"$prefijo"'","nombre":"Periodo sin oferta","inicio":"2028-04-03","fin":"2028-08-04"}'
    [[ $estado == 201 ]] || { echo "       registrar $prefijo: $estado" >&2; return 1; }
    abrir "$prefijo" "$numero_a" || return 1
    iniciar vicerrectorado.demo
    afinidad "$DOCENTE" "$prefijo"
    [[ $estado == 200 && $(jq -r '.puntajes | length' <<<"$cuerpo") == 0 ]]
}

errores_del_catalogo() {
    afinidad "$DOCENTE" 2027-I
    es_codigo 404 GENERACION_PERIODO_NO_ENCONTRADO || return 1
    afinidad "$DOCENTE" '2026%20I'
    es_codigo 400 COMUN_SOLICITUD_INVALIDA || return 1
    afinidad 0000000000 2026-I
    es_codigo 404 GENERACION_DOCENTE_NO_ENCONTRADO || return 1
    afinidad abc 2026-I
    es_codigo 400 COMUN_SOLICITUD_INVALIDA
}

permisos() {
    iniciar docente.demo
    afinidad "$DOCENTE" 2026-I
    [[ $estado == 403 ]] || return 1
    token=''
    afinidad "$DOCENTE" 2026-I
    [[ $estado == 401 ]] || return 1
    iniciar vicerrectorado.demo
}

# El API Gateway no las enruta y las rechaza; dentro de la red, un token de usuario recibe 403.
rutas_internas() {
    local respuesta
    pedir GET "$GATEWAY/interno/docentes/$DOCENTE/formacion" "Bearer $token"
    [[ $estado == 403 ]] || return 1
    # Las imágenes no traen curl ni wget (compose.yaml), y el token va por la entrada estándar.
    # shellcheck disable=SC2016
    respuesta=$(printf '%s\n' "$token" | docker compose exec -T distributivo bash -c '
        read -r t
        exec 3<>/dev/tcp/datos/8080
        printf "GET /interno/docentes/%s/formacion HTTP/1.1\r\nHost: datos\r\nAuthorization: Bearer %s\r\nConnection: close\r\n\r\n" \
            "$1" "$t" >&3
        head -1 <&3' _ "$DOCENTE" || true)
    [[ $respuesta == *' 403'* ]]
}

# La oferta de 2026-I en menos de $SEGUNDOS_DE_LA_LISTA segundos, con la sesión ya iniciada (SC-003).
la_lista_tarda_poco() {
    afinidad "$DOCENTE" 2026-I
    echo "       la lista de $(jq -r '.puntajes | length' <<<"$cuerpo") asignaturas tardó $segundos s"
    [[ $estado == 200 ]] && awk -v s="$segundos" -v m="$SEGUNDOS_DE_LA_LISTA" 'BEGIN { exit !(s < m) }'
}

# Con datos detenido: el error del catálogo, sin puntajes, en menos de $SEGUNDOS_SIN_DATOS (SC-004).
sin_el_proceso_datos() {
    local bien
    docker compose stop datos >/dev/null 2>&1
    afinidad "$DOCENTE" 2026-I
    echo "       sin datos respondió $estado en $segundos s"
    bien=0
    if [[ $estado == 502 || $estado == 504 ]] \
        && [[ $(jq -r '.codigo' <<<"$cuerpo") == COMUN_PROCESO_* ]] \
        && [[ $(jq -r '.puntajes // "sin puntajes"' <<<"$cuerpo") == 'sin puntajes' ]] \
        && awk -v s="$segundos" -v m="$SEGUNDOS_SIN_DATOS" 'BEGIN { exit !(s < m) }'; then
        bien=1
    fi
    docker compose up -d --wait --no-recreate --no-build datos >/dev/null 2>&1 || return 1
    afinidad "$DOCENTE" 2026-I
    [[ $bien == 1 && $estado == 200 ]]
}

# Ningún dato de la ficha en los registros de los procesos (SC-005).
sin_la_ficha_en_los_registros() {
    local veces
    veces=$(docker compose logs --no-color --since "$inicio_en_utc" datos distributivo identidad 2>&1 \
        | grep -c -F "$TITULO_DE_LA_FICHA" || true)
    [[ $veces == 0 ]]
}

inicio_en_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)
numero_a=''
comparados=0

verificar 1 "Las cargas de prueba y 2026-I abierto con la versión $version" abierto_y_cargado
verificar 2 "vicerrectorado.demo ve los 47 docentes en toda la oferta, iguales a una suma hecha aparte" todos_los_docentes
echo "       comparó ${comparados:-0} puntajes"
verificar 3 "El desglose nombra el título de cuarto nivel con el peso de la versión" el_desglose_lo_dice
verificar 4 "2026-II sin abrir: 409 GENERACION_PERIODO_SIN_ABRIR" sin_abrir
verificar 5 "2026-II abierto con otros pesos: otro puntaje, igual a la suma aparte" otros_pesos
verificar 6 "Un periodo abierto sin oferta: lista vacía" sin_oferta
verificar 7 "2027-I, «2026 I», un docente que no existe y «abc»: sus errores del catálogo" errores_del_catalogo
verificar 8 "docente.demo recibe 403 y sin token, 401" permisos
verificar 9 "/interno/… responde 403 por el API Gateway y con un token de usuario dentro de la red" rutas_internas
verificar 10 "La lista de la oferta tarda menos de $SEGUNDOS_DE_LA_LISTA s" la_lista_tarda_poco
verificar 11 "Con datos detenido: 502 o 504 del catálogo en menos de $SEGUNDOS_SIN_DATOS s, y vuelve" sin_el_proceso_datos
verificar 12 "La ficha no aparece en los registros de los procesos" sin_la_ficha_en_los_registros

if ((fallas > 0)); then
    echo "El puntaje de afinidad de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "El puntaje de afinidad de punta a punta: todos los pasos pasan."
