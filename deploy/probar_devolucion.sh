#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar y esperar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Devolver parciales de punta a punta sobre el perfil levantado, con los procesos reales (F12-H05;
# specs/043-devolver-parcial/contracts/demostracion.md). Uso, desde cualquier directorio:
# deploy/probar_devolucion.sh
# Con una base sin estructura orgánica, carreras, distributivos ni los datos de prueba: carga las 10
# cargas de prueba (ficticias, RNF-SEG-06), abre 2026-I, crea el distributivo, propone un docente
# afín y envía los parciales de todas las carreras: EN_CONSOLIDACION. Los roles que no son el
# Responsable de consolidación no devuelven; el Responsable no devuelve sin una observación válida
# ni con una carrera que no existe, y devuelve dos carreras con su observación: el distributivo
# vuelve a BORRADOR_CARRERA. La carrera devuelta corrige y la otra no; las dos reenvían y el
# distributivo vuelve a EN_CONSOLIDACION. Una sola entrada DISTRIBUTIVO_DEVUELTO y ningún rastro de
# los rechazos. Si la base ya tiene alguno de esos datos, falla sin tocar nada. Al terminar, aunque
# un paso falle, borra lo que creó, salvo las entradas de la bitácora, que no se pueden borrar.
# Termina con 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime una clave ni un token.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly DATOS=backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba
readonly CARGAS=(estructura_organica carreras ciclos_academicos asignaturas_malla oferta_periodo
    docentes politica_carga_horaria cargos_autoridades proyectos_practicas matriculados_carrera)
readonly COORDINADOR=coordinacion.demo
readonly PERIODO=2026-I
readonly RESPONSABLE=consolidacion.demo
readonly FLUJO="$GATEWAY/api/validacionocs/periodos/$PERIODO/flujo"
readonly DEVOLUCION="$GATEWAY/api/validacionocs/periodos/$PERIODO/devolucion"
readonly OBSERVACION='Faltan docentes en los paralelos de primer nivel.'
# Los usuarios de la demo de los roles que no devuelven (D-16).
readonly OTROS_ROLES=(vicerrectorado.demo datos.demo gestion.demo coordinacion.demo rectorado.demo
    ocs.demo secretaria.demo docente.demo admin.demo)

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

# Deja el estado en $estado y el cuerpo en $cuerpo.
estado=''
cuerpo=''
pedir() {  # pedir <método> <url> [autorización] [cuerpo json]
    local archivo
    archivo=$(mktemp -p "$carpeta")
    local argumentos=(-s -o "$archivo" -w '%{http_code}' -X "$1" "$2")
    [[ -n ${3:-} ]] && argumentos+=(-H "Authorization: $3")
    [[ -n ${4:-} ]] && argumentos+=(-H 'Content-Type: application/json' --data "$4")
    estado=$(curl "${argumentos[@]}" || echo '000')
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

# Las entradas que llegaron desde que empezó el script y cumplen la condición SQL.
entradas() {  # entradas <condición>
    consultar_base "select count(*) from usuarios.bitacora where recibida_en >= '$inicio' and ($1)"
}

hay_entradas() {  # hay_entradas <cantidad> <condición>
    [[ $(entradas "$2") == "$1" ]]
}

prefijo="P$(printf '%03d' $((RANDOM % 1000)))"
version="${prefijo}-POL-2024"

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
primera = sorted(por_carrera[carrera], key=lambda u: (u['codigo_asignatura'], u['paralelo']))[0]
with open(f'{carpeta}/datos.tsv', 'w') as salida:
    salida.write('\t'.join([carrera, libre['numero_identificacion'], primera['codigo_asignatura'],
                             primera['paralelo']]) + '\n')
# La primera asignatura-paralelo de la tercera carrera, que no se devuelve (paso 7).
ordenadas = sorted(por_carrera, key=lambda c: (len(por_carrera[c]), c))
otra = sorted(por_carrera[ordenadas[2]], key=lambda u: (u['codigo_asignatura'], u['paralelo']))[0]
with open(f'{carpeta}/otra.tsv', 'w') as salida:
    salida.write('\t'.join([ordenadas[2], otra['codigo_asignatura'], otra['paralelo']]) + '\n')
# Las carreras con oferta, de la de menos asignaturas a la de más: la primera envía en el paso 2.
with open(f'{carpeta}/orden.lista', 'w') as salida:
    salida.write(' '.join(sorted(por_carrera, key=lambda c: (len(por_carrera[c]), c))) + '\n')
PY
unidades=$(cat "$carpeta/unidades.lista")
carreras=$(cat "$carpeta/carreras.lista")
periodos=$(cat "$carpeta/periodos.lista")
docentes=$(cat "$carpeta/docentes.lista")
proyectos=$(cat "$carpeta/proyectos.lista")
inicio=$(consultar_base "select now()")
# libre no se usa aquí: el bloque de datos es el mismo de probar_avance.sh.
IFS=$'\t' read -r carrera _libre asignatura paralelo <"$carpeta/datos.tsv"
read -r -a orden <"$carpeta/orden.lista"
IFS=$'\t' read -r carrera_otra asignatura_otra paralelo_otra <"$carpeta/otra.tsv"
devuelta=${orden[0]}
segunda=${orden[1]}

limpiar() {
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
delete from normativa.parametro_regla where version_id in (select id from normativa.version_normativa where codigo = '$version');
delete from normativa.regla where version_id in (select id from normativa.version_normativa where codigo = '$version');
delete from normativa.fila_carga_horaria where version_id in (select id from normativa.version_normativa where codigo = '$version');
delete from normativa.version_normativa where codigo = '$version';
delete from normativa.registro_cambio where momento >= '$inicio';
delete from cargas.historial_carga where momento >= '$inicio';
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
        + (select count(*) from validacionocs.proceso_aprobacion where periodo in ($periodos))
        + (select count(*) from docentes.docente where identificacion in ($docentes))
        + (select count(*) from academico.periodo_academico where codigo in ($periodos))" 2>/dev/null || echo '?')
    [[ $quedan == 0 ]] || echo "AVISO  Quedaron $quedan filas creadas por el script"
    docker compose exec -T distributivo sh -c 'rm -f /var/mdd/archivos/*' >/dev/null 2>&1 \
        || echo "AVISO  No se pudo vaciar el almacén de archivos"
    rm -rf "$carpeta"
}

# --- 0. La base no tiene estructura, carreras, distributivos ni los datos de prueba ---
ya_estan=$(consultar_base "select (select count(*) from academico.unidad_organica where activa or codigo in ($unidades))
    + (select count(*) from academico.carrera where estado = 'VIGENTE' or codigo in ($carreras))
    + (select count(*) from academico.periodo_academico where codigo in ($periodos))
    + (select count(*) from docentes.docente where identificacion in ($docentes))
    + (select count(*) from docentes.proyecto where codigo in ($proyectos))
    + (select count(*) from roles.carrera_de_coordinador)
    + (select count(*) from generacion.distributivo)
    + (select count(*) from validacionocs.proceso_aprobacion where periodo in ($periodos))")
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
echo "OK     0. La base no tiene estructura, distributivos ni procesos de aprobación"

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

# como_coordinador_de <carrera>: le asigna esa carrera a $COORDINADOR y deja su token en $token.
como_coordinador_de() {
    iniciar admin.demo
    pedir PUT "$GATEWAY/api/roles/usuarios/$COORDINADOR/carrera" "Bearer $token" "{\"carrera\":\"$1\"}"
    [[ $estado == 200 ]] || return 1
    iniciar "$COORDINADOR"
    [[ -n $token ]]
}

enviar_parcial() {  # enviar_parcial <carrera>, con el token de $token
    pedir POST "$GATEWAY/api/validacionocs/periodos/$PERIODO/parciales/$1/envio" "Bearer $token"
}

# devolver <cuerpo json>, con el token de $token.
devolver() {
    pedir POST "$DEVOLUCION" "Bearer $token" "$1"
}

# una <carrera> <observación>: el cuerpo de una devolución de una carrera.
una() {
    jq -cn --arg c "$1" --arg o "$2" '{devoluciones: [{carrera: $c, observacion: $o}]}'
}

devoluciones_guardadas() {
    consultar_base "select count(*) from validacionocs.transicion where periodo = '$PERIODO'
        and tipo = 'DEVOLUCION'"
}

estados_de_parciales() {  # carrera=estado, ordenados
    consultar_base "select string_agg(carrera || '=' || estado, ' ' order by carrera)
        from validacionocs.parcial_del_proceso where periodo = '$PERIODO'"
}

# --- 1. Las cargas, 2026-I abierto, una propuesta y los parciales enviados: EN_CONSOLIDACION ---
preparado() {
    local afin una_carrera numero
    cargar || return 1
    iniciar vicerrectorado.demo
    numero=$(consultar_base "select numero from normativa.version_normativa where codigo = '$version'")
    pedir PUT "$GATEWAY/api/academico/periodos/$PERIODO/normativa" "Bearer $token" "{\"version\":${numero:-0}}"
    [[ $estado == 200 ]] || return 1
    pedir POST "$GATEWAY/api/generacion/periodos/$PERIODO/distributivo" "Bearer $token"
    [[ $estado == 201 ]] || return 1
    como_coordinador_de "$carrera" || return 1
    pedir GET "$GATEWAY/api/generacion/periodos/$PERIODO/oferta/$asignatura/$paralelo/sugerencia" "Bearer $token"
    afin=$(jq -r '[.candidatos[] | select(.motivoParaNoProponer == null)][0].identificacion // empty' <<<"$cuerpo")
    [[ -n $afin ]] || { echo "       sin docente afín para $asignatura/$paralelo" >&2; return 1; }
    pedir PUT "$GATEWAY/api/generacion/periodos/$PERIODO/oferta/$asignatura/$paralelo/propuesta" "Bearer $token" \
        "$(jq -cn --arg d "$afin" '{docente: $d}')"
    [[ $estado == 200 && $(jq -r '.guardada' <<<"$cuerpo") == true ]] || return 1
    docente_afin=$afin
    for una_carrera in "${orden[@]}"; do
        como_coordinador_de "$una_carrera" || return 1
        enviar_parcial "$una_carrera"
        [[ $estado == 200 ]] || { echo "       $una_carrera: $estado" >&2; return 1; }
    done
    iniciar "$RESPONSABLE"
    pedir GET "$FLUJO" "Bearer $token"
    [[ $estado == 200 && $(jq -r '.etapa' <<<"$cuerpo") == EN_CONSOLIDACION ]] || return 1
    [[ $(jq -c '.acciones' <<<"$cuerpo") == '["ENVIAR_A_REVISION","DEVOLVER_PARCIALES"]' ]] || return 1
    iniciar vicerrectorado.demo
    pedir GET "$FLUJO" "Bearer $token"
    [[ $(jq -c '.acciones' <<<"$cuerpo") == '[]' ]]
}

docente_afin=''

# --- 2. Los usuarios de los demás roles no devuelven: 403, todos con el mismo cuerpo ---
otros_roles() {
    local usuario primero=''
    for usuario in "${OTROS_ROLES[@]}"; do
        iniciar "$usuario"
        [[ -n $token ]] || { echo "       $usuario no inicia sesión" >&2; return 1; }
        devolver "$(una "$devuelta" "$OBSERVACION")"
        es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || { echo "       $usuario: $estado" >&2; return 1; }
        [[ $cuerpo != *RESPONSABLE_CONSOLIDACION* ]] || return 1
        primero=${primero:-$(jq -c 'del(.idIncidente)' <<<"$cuerpo")}
        [[ $(jq -c 'del(.idIncidente)' <<<"$cuerpo") == "$primero" ]] || return 1
    done
}

# --- 3. Sin una observación válida o con una carrera repetida: 400 y ninguna devuelta ---
sin_observacion() {
    iniciar "$RESPONSABLE"
    devolver "$(jq -cn --arg c "$devuelta" '{devoluciones: [{carrera: $c}]}')"
    es_codigo 400 COMUN_SOLICITUD_INVALIDA || { echo "       sin observación: $estado" >&2; return 1; }
    devolver "$(una "$devuelta" 'Muy corta')"
    es_codigo 400 COMUN_SOLICITUD_INVALIDA || { echo "       9 caracteres: $estado" >&2; return 1; }
    devolver "$(una "$devuelta" '            ')"
    es_codigo 400 COMUN_SOLICITUD_INVALIDA || { echo "       solo espacios: $estado" >&2; return 1; }
    devolver "$(una "$devuelta" '   corta       ')"
    es_codigo 400 VALIDACIONOCS_OBSERVACION_INVALIDA || { echo "       corta sin espacios: $estado" >&2; return 1; }
    devolver "$(jq -cn --arg c "$devuelta" --arg o "$OBSERVACION" \
        '{devoluciones: [{carrera: $c, observacion: $o}, {carrera: $c, observacion: $o}]}')"
    es_codigo 400 VALIDACIONOCS_CARRERA_REPETIDA || { echo "       repetida: $estado" >&2; return 1; }
    devolver "$(jq -cn --arg c "$devuelta" --arg s "$segunda" --arg o "$OBSERVACION" \
        '{devoluciones: [{carrera: $c, observacion: $o}, {carrera: $s, observacion: "   corta     "}]}')"
    es_codigo 400 VALIDACIONOCS_OBSERVACION_INVALIDA || { echo "       una de dos: $estado" >&2; return 1; }
    [[ $(devoluciones_guardadas) == 0 && $(estados_de_parciales) != *DEVUELTO* ]]
}

# --- 4. Una carrera que no existe junto con una válida: 404 y ninguna devuelta ---
carrera_inexistente() {
    devolver "$(jq -cn --arg c "$devuelta" --arg o "$OBSERVACION" \
        '{devoluciones: [{carrera: $c, observacion: $o}, {carrera: "NO-EXISTE", observacion: $o}]}')"
    es_codigo 404 VALIDACIONOCS_PARCIAL_NO_ENCONTRADO || return 1
    [[ $(devoluciones_guardadas) == 0 && $(estados_de_parciales) != *DEVUELTO* ]]
}

# --- 5. Devuelve dos carreras con su observación: BORRADOR_CARRERA y una entrada ---
devueltas() {
    devolver "$(jq -cn --arg c "$devuelta" --arg s "$segunda" --arg o "$OBSERVACION" \
        '{devoluciones: [{carrera: $c, observacion: $o}, {carrera: $s, observacion: ("Revisa " + $s + ": un docente sin clase.")}]}')"
    [[ $estado == 200 && $(jq -r '.etapa' <<<"$cuerpo") == BORRADOR_CARRERA ]] || return 1
    [[ $(jq -r --arg c "$devuelta" '.parciales[] | select(.carrera == $c) | .devolucion.observacion' <<<"$cuerpo") == "$OBSERVACION" ]] || return 1
    [[ $(jq -r --arg c "$devuelta" '.parciales[] | select(.carrera == $c) | .devolucion.por' <<<"$cuerpo") == "$RESPONSABLE" ]] || return 1
    [[ $(jq -r '[.parciales[] | select(.enviado)] | length' <<<"$cuerpo") == $((${#orden[@]} - 2)) ]] || return 1
    esperar 15 hay_entradas 1 "servicio = 'validacionocs' and accion = 'DISTRIBUTIVO_DEVUELTO'" || return 1
    [[ $(entradas "servicio = 'validacionocs' and accion = 'DISTRIBUTIVO_DEVUELTO'
        and objeto -> 'devueltos' ->> '$devuelta' = '$OBSERVACION'
        and objeto -> 'devueltos' ? '$segunda'
        and anterior ->> 'estado' = 'EN_CONSOLIDACION' and nuevo ->> 'estado' = 'BORRADOR_CARRERA'") == 1 ]]
}

# --- 6. Devolver otra vez: 409, ya no está en EN_CONSOLIDACION ---
otra_vez() {
    devolver "$(una "${orden[2]}" "$OBSERVACION")"
    es_codigo 409 VALIDACIONOCS_ETAPA_NO_ADMITE_DEVOLUCION || return 1
    [[ $(devoluciones_guardadas) == 1 ]]
}

# --- 7. La carrera devuelta corrige; la que no se devolvió, no ---
corregir() {
    como_coordinador_de "$devuelta" || return 1
    pedir DELETE "$GATEWAY/api/generacion/periodos/$PERIODO/oferta/$asignatura/$paralelo/propuesta" "Bearer $token"
    [[ $estado == 200 || $estado == 204 ]] || { echo "       quitar en la devuelta: $estado" >&2; return 1; }
    como_coordinador_de "$carrera_otra" || return 1
    pedir PUT "$GATEWAY/api/generacion/periodos/$PERIODO/oferta/$asignatura_otra/$paralelo_otra/propuesta" "Bearer $token" \
        "$(jq -cn --arg d "$docente_afin" '{docente: $d}')"
    es_codigo 409 GENERACION_PARCIAL_NO_EDITABLE || { echo "       proponer en la no devuelta: $estado" >&2; return 1; }
}

# --- 8. Reenvían las dos: BORRADOR_CARRERA tras la primera, EN_CONSOLIDACION tras la segunda ---
reenviar() {
    como_coordinador_de "$devuelta" || return 1
    enviar_parcial "$devuelta"
    [[ $estado == 200 && $(jq -r '.etapa' <<<"$cuerpo") == BORRADOR_CARRERA ]] || return 1
    como_coordinador_de "$segunda" || return 1
    enviar_parcial "$segunda"
    [[ $estado == 200 && $(jq -r '.etapa' <<<"$cuerpo") == EN_CONSOLIDACION ]] || return 1
    [[ $(estados_de_parciales) != *DEVUELTO* ]] || return 1
    esperar 15 hay_entradas 1 "servicio = 'validacionocs' and accion = 'ETAPA_AVANZADA'
        and nuevo ->> 'estado' = 'EN_CONSOLIDACION' and momento >= (select max(momento)
        from usuarios.bitacora where accion = 'DISTRIBUTIVO_DEVUELTO')"
}

# --- 9. Una sola devolución en la bitácora y en las transiciones; ningún rechazo dejó rastro ---
sin_rastro() {
    sleep 3
    [[ $(devoluciones_guardadas) == 1 ]] || return 1
    [[ $(entradas "servicio = 'validacionocs' and accion = 'DISTRIBUTIVO_DEVUELTO'") == 1 ]]
}

verificar 1 "Las 10 cargas, $PERIODO abierto, una propuesta y los ${#orden[@]} parciales enviados: EN_CONSOLIDACION; solo $RESPONSABLE ve devolver" preparado
verificar 2 "Los ${#OTROS_ROLES[@]} usuarios de los demás roles no devuelven: 403 con el mismo cuerpo" otros_roles
verificar 3 "Sin observación, corta, solo espacios, carrera repetida o una de dos inválida: 400 y ninguna devuelta" sin_observacion
verificar 4 "Una carrera que no existe con una válida: 404 y ninguna devuelta" carrera_inexistente
verificar 5 "Devuelve $devuelta y $segunda con su observación: BORRADOR_CARRERA y una entrada DISTRIBUTIVO_DEVUELTO" devueltas
verificar 6 "Devolver otra vez: 409 VALIDACIONOCS_ETAPA_NO_ADMITE_DEVOLUCION" otra_vez
verificar 7 "La carrera devuelta quita una propuesta; la no devuelta recibe 409 GENERACION_PARCIAL_NO_EDITABLE" corregir
verificar 8 "Reenvían las dos: EN_CONSOLIDACION con la segunda" reenviar
verificar 9 "Una sola devolución guardada y en la bitácora; ningún rechazo dejó rastro" sin_rastro

if ((fallas > 0)); then
    echo "Devolver parciales de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "Devolver parciales de punta a punta: todos los pasos pasan."
