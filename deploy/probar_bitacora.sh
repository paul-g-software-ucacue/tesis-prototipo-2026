#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar y esperar; shellcheck no lo ve.
# shellcheck disable=SC2329
# La bitácora de punta a punta sobre el perfil levantado, con los procesos reales (F17-H01;
# specs/039-bitacora-cambios/contracts/demostracion.md). Uso, desde cualquier directorio:
# deploy/probar_bitacora.sh
# Con una base sin estructura orgánica, carreras, distributivos ni los datos de prueba: carga las 10
# cargas de prueba (ficticias, RNF-SEG-06), abre 2026-I y crea el distributivo. Cambia una actividad
# de 4 a 6 horas y revisa su entrada; agrega, quita y prueba una regla dura; hace un cambio en cada
# servicio; cuenta las entradas de las cargas; detiene identidad, hace 10 cambios y lo vuelve a
# levantar; repite una entrega; intenta modificar la bitácora con el rol del proceso; busca secretos
# y datos personales; y mide un cambio con identidad arriba y abajo. Si la base ya tiene alguno de
# esos datos, falla sin tocar nada. Al terminar, aunque un paso falle, levanta los procesos y borra
# lo que creó, salvo las entradas de la bitácora, que no se pueden borrar: es lo que se prueba. Solo
# cuenta las que llegaron desde que empezó. Termina con 0 si todos los pasos pasan y con 1 si alguno
# falla. Nunca imprime una clave ni un token.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly DATOS=backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba
readonly CARGAS=(estructura_organica carreras ciclos_academicos asignaturas_malla oferta_periodo
    docentes politica_carga_horaria cargos_autoridades proyectos_practicas matriculados_carrera)
readonly COORDINADOR=coordinacion.demo
readonly RESPONSABLE=docente.demo
readonly PERIODO=2026-I
# Dos ciclos del reenvío (30 s) y un margen (SC-002).
readonly ESPERA_REENVIO=75
readonly ESPERA_ENTREGA=20
readonly CAMBIOS_SIN_IDENTIDAD=10
readonly DIFERENCIA_MAXIMA=0.5

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

# Repite la condición cada segundo hasta que se cumpla o pasen los segundos indicados.
esperar() {  # esperar <segundos> <condición…>
    local limite=$(($(date +%s) + $1))
    shift
    until "$@"; do
        (($(date +%s) >= limite)) && return 1
        sleep 1
    done
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

# enviar <método> <url> <campos de curl…>: un formulario multipart, con el token de $token.
enviar() {
    local archivo metodo=$1 url=$2
    shift 2
    archivo=$(mktemp -p "$carpeta")
    estado=$(curl -s -o "$archivo" -w '%{http_code}' -X "$metodo" -H "Authorization: Bearer $token" \
        "$@" "$url" || echo '000')
    cuerpo=$(cat "$archivo")
}

# iniciar <usuario>: deja el token en $token, o vacío si no entró.
iniciar() {
    pedir POST "$GATEWAY/api/usuarios/sesion" "" \
        "$(jq -cn --arg u "$1" --arg c "$(valor_de_entorno MDD_USUARIOS_DEMO_CLAVE)" '{usuario: $u, clave: $c}')"
    token=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
}

# psql en la base del perfil, con el superusuario; sin alineación ni encabezados.
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

# Las entradas que llegaron desde que empezó el script y cumplen la condición SQL.
entradas() {  # entradas <condición>
    consultar_base "select count(*) from usuarios.bitacora where recibida_en >= '$inicio' and ($1)"
}

hay_entradas() {  # hay_entradas <cantidad> <condición>
    [[ $(entradas "$2") == "$1" ]]
}

# Cada código de prueba nuevo lleva este prefijo: P y tres dígitos.
prefijo="P$(printf '%03d' $((RANDOM % 1000)))"
version="${prefijo}-POL-2024"
otra_version="${prefijo}-BIT"
telefono_nuevo="09$(printf '%08d' $((RANDOM * RANDOM % 100000000)))"

# --- Los archivos de prueba, con la política con el código nuevo, y un docente libre ---
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
por_carrera = {}
for fila in filas:
    una = dict(zip(encabezados, fila))
    por_carrera.setdefault(asignaturas[una['codigo_asignatura']]['codigo_carrera'], []).append(una)
carrera = sorted(por_carrera, key=lambda c: (len(por_carrera[c]), c))[0]

encabezados, filas = leer('06_docentes.csv')
docentes = [dict(zip(encabezados, fila)) for fila in filas]
encabezados, filas = leer('08_cargos_autoridades.csv')
con_cargo = {dict(zip(encabezados, fila))['numero_identificacion'] for fila in filas}
encabezados, filas = leer('09_proyectos_practicas.csv')
con_proyecto = {uno.strip().split('|')[0] for fila in filas
                for uno in dict(zip(encabezados, fila))['participantes'].split(';')}
libre = next(d for d in docentes if d['estado'] == 'ACTIVO' and d['dedicacion'] == 'TC'
             and d['numero_identificacion'] not in con_proyecto | con_cargo)
with open(f'{carpeta}/datos.tsv', 'w') as salida:
    salida.write('\t'.join([carrera, libre['numero_identificacion'], libre['telefono']]) + '\n')
PY
unidades=$(cat "$carpeta/unidades.lista")
carreras=$(cat "$carpeta/carreras.lista")
periodos=$(cat "$carpeta/periodos.lista")
docentes=$(cat "$carpeta/docentes.lista")
proyectos=$(cat "$carpeta/proyectos.lista")
inicio=$(consultar_base "select now()")
IFS=$'\t' read -r carrera libre telefono_anterior <"$carpeta/datos.tsv"

limpiar() {
    docker compose up -d --wait --no-recreate --no-build identidad datos distributivo >/dev/null 2>&1 \
        || echo "AVISO  No se pudieron volver a levantar los procesos"
    consultar_base "delete from validacionocs.configuracion_flujo where periodo in ($periodos);
delete from generacion.actividad_no_lectiva where version_id in (select v.id from generacion.version_distributivo v
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
delete from docentes.registro_cambio where momento >= '$inicio';
delete from academico.matricula where periodo in ($periodos);
delete from academico.asignatura_paralelo where periodo in ($periodos);
delete from academico.asignatura where carrera in ($carreras);
delete from academico.malla where carrera in ($carreras);
delete from academico.carrera where codigo in ($carreras);
delete from academico.periodo_academico where codigo in ($periodos);
delete from academico.unidad_organica where codigo in ($unidades);
delete from academico.registro_cambio where momento >= '$inicio';
delete from normativa.asignacion_normativa where periodo in ($periodos);
delete from normativa.parametro_regla where version_id in (select id from normativa.version_normativa where codigo in ('$version', '$otra_version'));
delete from normativa.regla where version_id in (select id from normativa.version_normativa where codigo in ('$version', '$otra_version'));
delete from normativa.fila_carga_horaria where version_id in (select id from normativa.version_normativa where codigo in ('$version', '$otra_version'));
delete from normativa.version_normativa where codigo in ('$version', '$otra_version');
delete from normativa.registro_cambio where momento >= '$inicio';
delete from cargas.historial_carga where momento >= '$inicio';
delete from roles.designacion_responsable where periodo in ($periodos);
delete from roles.carrera_de_coordinador;
delete from roles.registro_cambio where momento >= '$inicio';
delete from roles.periodo_conocido where codigo in ($periodos);
delete from roles.evento_recibido where id_evento in (select (serialized_event::jsonb ->> 'idEvento')::uuid
    from eventos_datos.event_publication where serialized_event::jsonb -> 'contenido' ->> 'codigo' in ($periodos));
delete from eventos_datos.event_publication where serialized_event::jsonb -> 'contenido' ->> 'codigo' in ($periodos);" >/dev/null 2>&1 \
        || echo "AVISO  No se pudo borrar lo creado por el script"
    local quedan
    quedan=$(consultar_base "select (select count(*) from generacion.distributivo)
        + (select count(*) from generacion.registro_cambio where momento >= '$inicio')
        + (select count(*) from docentes.docente where identificacion in ($docentes))
        + (select count(*) from academico.periodo_academico where codigo in ($periodos))" 2>/dev/null || echo '?')
    [[ $quedan == 0 ]] || echo "AVISO  Quedaron $quedan filas creadas por el script"
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

# subir <carga> <archivo> y confirmar <carga> <archivo> <huella>, con el token de $token.
cargar() {
    local i archivo huella
    for i in "${!CARGAS[@]}"; do
        archivo=$(printf '%s/%02d_%s.csv' "$carpeta" $((i + 1)) "${CARGAS[$i]}")
        enviar POST "$GATEWAY/api/cargas/plantillas/${CARGAS[$i]}/archivo" \
            -F "archivo=@$archivo;filename=$(basename "$archivo")"
        huella=$(jq -r '.vistaPrevia.huella // empty' <<<"$cuerpo")
        [[ $estado == 200 && $(jq -r '.errores' <<<"$cuerpo") == 0 && -n $huella ]] \
            || { echo "       ${CARGAS[$i]}: la vista previa falló" >&2; return 1; }
        enviar POST "$GATEWAY/api/cargas/plantillas/${CARGAS[$i]}/archivo/confirmacion" \
            -F "archivo=@$archivo;filename=$(basename "$archivo")" -F "huella=$huella"
        [[ $estado == 200 && $(jq -r '.resultado' <<<"$cuerpo") == APLICADA ]] \
            || { echo "       ${CARGAS[$i]}: la confirmación falló" >&2; return 1; }
    done
}

# asignar <docente> <actividad> <horas> y quitar <docente> <actividad>, con el token de $token.
asignar() {
    pedir PUT "$GATEWAY/api/generacion/periodos/$PERIODO/parciales/$carrera/docentes/$1/actividades/$2" \
        "Bearer $token" "$(jq -cn --argjson h "$3" '{horas: $h}')"
}

quitar() {
    pedir DELETE "$GATEWAY/api/generacion/periodos/$PERIODO/parciales/$carrera/docentes/$1/actividades/$2" \
        "Bearer $token"
}

# --- 1. Las cargas, el periodo abierto, una carrera para el coordinador y el distributivo ---
preparado() {
    local numero
    cargar || return 1
    iniciar vicerrectorado.demo
    numero=$(consultar_base "select numero from normativa.version_normativa where codigo = '$version'")
    pedir PUT "$GATEWAY/api/academico/periodos/$PERIODO/normativa" "Bearer $token" "{\"version\":${numero:-0}}"
    [[ $estado == 200 ]] || return 1
    iniciar admin.demo
    pedir PUT "$GATEWAY/api/roles/usuarios/$COORDINADOR/carrera" "Bearer $token" "{\"carrera\":\"$carrera\"}"
    [[ $estado == 200 ]] || return 1
    iniciar vicerrectorado.demo
    pedir POST "$GATEWAY/api/generacion/periodos/$PERIODO/distributivo" "Bearer $token"
    [[ $estado == 201 ]]
}

# --- 2. El escenario del issue: una actividad de 4 a 6 horas ---
de_4_a_6() {
    iniciar "$COORDINADOR"
    asignar "$libre" 140-b 4
    [[ $estado == 200 ]] || return 1
    asignar "$libre" 140-b 6
    [[ $estado == 200 ]] || return 1
    esperar "$ESPERA_ENTREGA" hay_entradas 1 "servicio = 'generacion' and accion = 'ACTIVIDAD_GUARDADA'
        and usuario = '$COORDINADOR' and periodo = '$PERIODO' and objeto ->> 'docente' = '$libre'
        and anterior ->> 'horas' = '4' and nuevo ->> 'horas' = '6'"
}

# --- 3. Agregar una y quitar otra: una entrada sin anterior y otra sin nuevo ---
agregar_y_quitar() {
    asignar "$libre" 143-k 2
    [[ $estado == 200 ]] || return 1
    quitar "$libre" 140-b
    [[ $estado == 204 ]] || return 1
    esperar "$ESPERA_ENTREGA" hay_entradas 1 "accion = 'ACTIVIDAD_GUARDADA' and objeto ->> 'actividad' = '143-k'
        and objeto ->> 'docente' = '$libre' and anterior is null and nuevo ->> 'horas' = '2'" || return 1
    esperar "$ESPERA_ENTREGA" hay_entradas 1 "accion = 'ACTIVIDAD_QUITADA' and objeto ->> 'actividad' = '140-b'
        and objeto ->> 'docente' = '$libre' and nuevo is null and anterior ->> 'horas' = '6'"
}

# --- 4. Una regla dura: el cambio no se guarda y no deja entrada ---
regla_dura() {
    local antes
    antes=$(entradas "servicio = 'generacion'")
    asignar "$libre" 140-b 40
    es_codigo 409 GENERACION_HORAS_EXCEDIDAS || return 1
    sleep 3
    [[ $(entradas "servicio = 'generacion'") == "$antes" ]]
}

# --- 5. Un cambio en cada servicio ---
un_cambio_en_cada_servicio() {
    local periodo_actual docente_actual numero plantilla
    desde=$(consultar_base "select now()")
    iniciar vicerrectorado.demo
    pedir GET "$GATEWAY/api/academico/periodos/$PERIODO" "Bearer $token"
    periodo_actual=$cuerpo
    pedir PUT "$GATEWAY/api/academico/periodos/$PERIODO" "Bearer $token" \
        "$(jq -c '{nombre: (.nombre + " (bitácora)"), inicio, fin}' <<<"$periodo_actual")"
    [[ $estado == 200 ]] || { echo "       Académico: $estado" >&2; return 1; }
    numero=$(consultar_base "select numero from normativa.version_normativa where codigo = '$version'")
    pedir POST "$GATEWAY/api/normativa/versiones" "Bearer $token" \
        "$(jq -cn --argjson n "${numero:-0}" --arg c "$otra_version" \
            '{copiarDe: $n, codigo: $c, origen: "Prueba de la bitácora", vigenteDesde: "2026-01-15"}')"
    [[ $estado == 201 ]] || { echo "       Normativa: $estado" >&2; return 1; }
    pedir PUT "$GATEWAY/api/roles/responsables/$PERIODO" "Bearer $token" \
        "$(jq -cn --arg u "$RESPONSABLE" '{usuario: $u}')"
    [[ $estado == 200 ]] || { echo "       Roles: $estado" >&2; return 1; }
    iniciar datos.demo
    pedir GET "$GATEWAY/api/docentes/docentes/$libre" "Bearer $token"
    docente_actual=$(jq -c 'del(.tipoIdentificacion, .identificacion, .nombreCompleto,
        .nombreDeCarreraBase, .estado, .camposAmplios, .asignaturasAfines)' <<<"$cuerpo")
    pedir PUT "$GATEWAY/api/docentes/docentes/$libre" "Bearer $token" \
        "$(jq -c --arg t "$telefono_nuevo" '.telefono = $t' <<<"$docente_actual")"
    [[ $estado == 200 ]] || { echo "       Docentes: $estado" >&2; return 1; }
    iniciar admin.demo
    plantilla="$carpeta/flujo.yaml"
    curl -s -o "$plantilla" -H "Authorization: Bearer $token" "$GATEWAY/api/validacionocs/plantilla-flujo"
    enviar PUT "$GATEWAY/api/validacionocs/periodos/$PERIODO/configuracion-flujo" \
        -F "archivo=@$plantilla;filename=flujo.yaml;type=text/yaml"
    [[ $estado == 200 ]] || { echo "       Validación OCS: $estado" >&2; return 1; }
    esperar "$ESPERA_ENTREGA" cada_servicio_tiene_su_entrada
}

cada_servicio_tiene_su_entrada() {
    [[ $(entradas "recibida_en >= '$desde' and servicio = 'academico' and accion = 'PERIODO_CORREGIDO'
            and periodo = '$PERIODO'") == 1
        && $(entradas "recibida_en >= '$desde' and servicio = 'normativa' and accion = 'VERSION_CREADA'
            and objeto ->> 'codigo' = '$otra_version'") == 1
        && $(entradas "recibida_en >= '$desde' and servicio = 'roles' and accion = 'DESIGNAR_RESPONSABLE'
            and periodo = '$PERIODO' and nuevo ->> 'responsable' = '$RESPONSABLE'") == 1
        && $(entradas "recibida_en >= '$desde' and servicio = 'docentes' and accion = 'DOCENTE_CORREGIDO'
            and objeto ->> 'identificacion' = '$libre' and objeto -> 'camposPersonales' ? 'telefono'") == 1
        && $(entradas "recibida_en >= '$desde' and servicio = 'validacionocs'
            and accion = 'CONFIGURACION_FLUJO_CARGADA' and periodo = '$PERIODO'") == 1 ]]
}

# --- 6. Las cargas: una entrada por cada fila aplicada, con quien confirmó ---
filas_de_las_cargas() {
    consultar_base "select (select count(*) from academico.registro_cambio where momento >= '$inicio' and usuario = 'datos.demo')
        + (select count(*) from docentes.registro_cambio where momento >= '$inicio' and usuario = 'datos.demo')
        + (select count(*) from normativa.registro_cambio where momento >= '$inicio' and usuario = 'datos.demo')"
}

las_cargas_tienen_sus_entradas() {
    local filas
    filas=$(filas_de_las_cargas)
    echo "       $filas filas aplicadas por datos.demo"
    ((filas > 100)) || return 1
    esperar "$ESPERA_ENTREGA" hay_entradas "$filas" \
        "usuario = 'datos.demo' and servicio in ('academico', 'docentes', 'normativa')"
}

# --- 7 y 11. Identidad caída: los cambios se guardan sin error y sin demora, y llegan después ---
tiempos_arriba=()
tiempos_abajo=()
medir() {  # medir <arreglo> <horas>: asigna 143-k con esas horas y guarda el tiempo
    asignar "$libre" 143-k "$2"
    [[ $estado == 200 ]] || return 1
    local -n tiempos=$1
    tiempos+=("$segundos")
}

promedio() {
    printf '%s\n' "$@" | awk '{ suma += $1 } END { printf "%.3f", suma / NR }'
}

sin_identidad() {
    local horas
    iniciar "$COORDINADOR"
    for horas in 3 4 5; do
        medir tiempos_arriba "$horas" || return 1
    done
    caida=$(consultar_base "select now()")
    docker compose stop identidad >/dev/null 2>&1
    for horas in $(seq 1 "$CAMBIOS_SIN_IDENTIDAD"); do
        medir tiempos_abajo "$horas" \
            || { echo "       el cambio $horas respondió $estado (401 = JWKS renovado sin identidad)" >&2; return 1; }
    done
    pendientes=$(consultar_base "select count(*) from eventos_distributivo.event_publication
        where publication_date >= '$caida' and completion_date is null")
    echo "       $pendientes publicaciones pendientes con identidad detenido"
    ((pendientes >= CAMBIOS_SIN_IDENTIDAD)) || return 1
    docker compose up -d --wait --no-recreate --no-build identidad >/dev/null 2>&1 || return 1
    esperar "$ESPERA_REENVIO" hay_entradas "$CAMBIOS_SIN_IDENTIDAD" \
        "recibida_en >= '$caida' and accion = 'ACTIVIDAD_GUARDADA' and objeto ->> 'actividad' = '143-k'
            and objeto ->> 'docente' = '$libre'"
}

sin_demora() {
    local arriba abajo
    arriba=$(promedio "${tiempos_arriba[@]}")
    abajo=$(promedio "${tiempos_abajo[@]}")
    echo "       promedio con identidad: $arriba s; sin identidad: $abajo s"
    menos_de "$(awk -v a="$arriba" -v b="$abajo" 'BEGIN { d = b - a; print (d < 0 ? -d : d) }')" \
        "$DIFERENCIA_MAXIMA"
}

# --- 8. Una entrega repetida: la publicación vuelve a FAILED y el reenvío no duplica nada ---
entrega_repetida() {
    local id_evento antes
    id_evento=$(consultar_base "select serialized_event::jsonb ->> 'idEvento' from eventos_distributivo.event_publication
        where publication_date >= '$inicio' and completion_date is not null
        and serialized_event like '%EntradaBitacora%' order by publication_date limit 1")
    [[ $id_evento =~ ^[0-9a-f-]{36}$ ]] || return 1
    antes=$(consultar_base "select count(*) from usuarios.bitacora")
    consultar_base "update eventos_distributivo.event_publication set status = 'FAILED', completion_date = null
        where serialized_event like '%$id_evento%'" >/dev/null
    esperar "$ESPERA_REENVIO" completada "$id_evento" || return 1
    [[ $(consultar_base "select count(*) from usuarios.bitacora") == "$antes"
        && $(consultar_base "select count(*) from usuarios.bitacora where id_evento = '$id_evento'") == 1 ]]
}

completada() {  # completada <idEvento>
    [[ $(consultar_base "select count(*) from eventos_distributivo.event_publication
        where serialized_event like '%$1%' and status = 'COMPLETED'") == 1 ]]
}

# --- 9. Nadie modifica la bitácora: ni el rol del proceso ni el dueño de la base ---
falla_sql() {  # falla_sql <rol> <sentencia>
    ! consultar_base "set role $1; $2" >/dev/null 2>&1
}

no_se_modifica() {
    local una antes
    una=$(consultar_base "select id_evento from usuarios.bitacora where recibida_en >= '$inicio' limit 1")
    [[ -n $una ]] || return 1
    antes=$(consultar_base "select md5(string_agg(id_evento::text || accion || coalesce(nuevo::text, ''), ',' order by id_evento)) from usuarios.bitacora")
    falla_sql identidad "update usuarios.bitacora set usuario = 'otro' where id_evento = '$una'" \
        && falla_sql identidad "delete from usuarios.bitacora where id_evento = '$una'" \
        && falla_sql identidad "truncate usuarios.bitacora" \
        && falla_sql postgres "update usuarios.bitacora set usuario = 'otro' where id_evento = '$una'" \
        && falla_sql postgres "delete from usuarios.bitacora where id_evento = '$una'" \
        && falla_sql postgres "truncate usuarios.bitacora" \
        && [[ $(consultar_base "select md5(string_agg(id_evento::text || accion || coalesce(nuevo::text, ''), ',' order by id_evento)) from usuarios.bitacora") == "$antes" ]]
}

# --- 10. Ninguna entrada guarda secretos ni datos personales de un docente ---
sin_secretos() {
    local claves valores personales texto
    # Cada clave de cada objeto, a cualquier profundidad (FR-011); palabrasClave no es un secreto (D-92).
    claves=$(consultar_base "select count(*) from usuarios.bitacora,
        lateral jsonb_path_query(jsonb_build_array(objeto, coalesce(anterior, '{}'), coalesce(nuevo, '{}')),
            'lax \$.** ? (@.type() == \"object\").keyvalue().key') as clave
        where clave #>> '{}' ~* '(clave|contrasen|password|token|secreto|jwt)'
        and clave #>> '{}' <> 'palabrasClave'")
    texto="concat_ws(' ', objeto::text, anterior::text, nuevo::text)"
    valores=0
    for secreto in "$(valor_de_entorno MDD_USUARIOS_DEMO_CLAVE)" "$(valor_de_entorno MDD_USUARIO_PRUEBA_CLAVE)" \
        "$(valor_de_entorno MDD_CLAVE_PROCESO_DATOS)" "$(valor_de_entorno MDD_CLAVE_PROCESO_DISTRIBUTIVO)" "eyJhbGci"; do
        [[ -z $secreto ]] && continue
        valores=$((valores + $(consultar_base "select count(*) from usuarios.bitacora
            where strpos($texto, \$\$$secreto\$\$) > 0")))
    done
    personales=$(consultar_base "select count(*) from usuarios.bitacora where servicio = 'docentes'
        and (strpos($texto, '$telefono_nuevo') > 0 or strpos($texto, '$telefono_anterior') > 0)")
    echo "       claves que parecen secretos: $claves; valores secretos: $valores; teléfonos: $personales"
    [[ $claves == 0 && $valores == 0 && $personales == 0 ]]
}

verificar 1 "Las 10 cargas, $PERIODO abierto, una carrera para el coordinador y el distributivo creado" preparado
verificar 2 "Una actividad de 4 a 6 horas: una entrada con el usuario, el periodo, anterior 4 y nuevo 6" de_4_a_6
verificar 3 "Agregar una y quitar otra: una entrada sin anterior y otra sin nuevo" agregar_y_quitar
verificar 4 "Una asignación que viola una regla dura: 409 y ninguna entrada nueva" regla_dura
verificar 5 "Un cambio en Académico, Docentes, Normativa, Roles y Validación OCS: una entrada de cada uno" \
    un_cambio_en_cada_servicio
verificar 6 "Las cargas: una entrada por cada fila aplicada, con datos.demo" las_cargas_tienen_sus_entradas
verificar 7 "Identidad detenido: $CAMBIOS_SIN_IDENTIDAD cambios sin error, y sus entradas llegan una vez en menos de $ESPERA_REENVIO s" \
    sin_identidad
verificar 8 "Una entrega repetida: COMPLETED otra vez y la misma cantidad de entradas" entrega_repetida
verificar 9 "UPDATE, DELETE y TRUNCATE sobre la bitácora fallan con el rol identidad y con el dueño" no_se_modifica
verificar 10 "Ninguna entrada guarda claves, tokens ni el teléfono del docente" sin_secretos
verificar 11 "Un cambio no se demora más de $DIFERENCIA_MAXIMA s por la bitácora, con identidad arriba o abajo" sin_demora

if ((fallas > 0)); then
    echo "La bitácora de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "La bitácora de punta a punta: todos los pasos pasan."
