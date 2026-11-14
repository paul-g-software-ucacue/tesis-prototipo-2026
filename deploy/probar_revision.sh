#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar y esperar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Enviar a revisión de punta a punta sobre el perfil levantado, con los procesos reales (F10-H03;
# specs/042-enviar-revision/contracts/demostracion.md). Uso, desde cualquier directorio:
# deploy/probar_revision.sh
# Con una base sin estructura orgánica, carreras, distributivos ni los datos de prueba: carga las 10
# cargas de prueba (ficticias, RNF-SEG-06), abre 2026-I, crea el distributivo, propone un docente
# afín (deja blandas abiertas) y envía los parciales de todas las carreras: EN_CONSOLIDACION. Los
# roles que no son el Responsable de consolidación no envían a revisión; el Responsable recibe un
# 409 por cada falta (sin consolidar, sin informe, inconsistencias abiertas con el número del panel),
# y el número baja al justificar una blanda. Ningún rechazo deja transición ni entrada en la
# bitácora. El envío que pasa no se alcanza con los datos de prueba, que tienen inconsistencias
# duras (research §10): lo prueba EnvioARevisionIntegrationTest. Si la base ya tiene alguno de esos
# datos, falla sin tocar nada. Al terminar, aunque un paso falle, borra lo que creó, salvo las
# entradas de la bitácora, que no se pueden borrar. Termina con 0 si todos los pasos pasan y con 1 si
# alguno falla. Nunca imprime una clave ni un token.
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
readonly REVISION="$GATEWAY/api/validacionocs/periodos/$PERIODO/revision"
readonly TEXTO='Aceptada por la coordinación académica para este periodo'
# Los usuarios de la demo de los roles que no envían a revisión (D-16).
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
    docker compose exec -T distributivo sh -c 'rm -rf /var/mdd/archivos/*' >/dev/null 2>&1 \
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

revisar() {  # con el token de $token
    pedir POST "$REVISION" "Bearer $token"
}

panel() {  # con el token de $token
    pedir GET "$GATEWAY/api/generacion/periodos/$PERIODO/inconsistencias" "Bearer $token"
}

# Las transiciones guardadas desde EN_CONSOLIDACION: ninguna mientras todo se rechace.
pasos_desde_consolidacion() {
    consultar_base "select count(*) from validacionocs.transicion where periodo = '$PERIODO'
        and desde = 'EN_CONSOLIDACION'"
}

# hacer_pdf <destino>: un PDF mínimo.
hacer_pdf() {
    printf '%%PDF-1.7\n%%%%EOF\n' >"$1"
}

abiertas_del_panel=''

# --- 1. Las cargas, 2026-I abierto, una propuesta y los parciales enviados: EN_CONSOLIDACION ---
preparado() {
    local afin una numero
    cargar || return 1
    iniciar vicerrectorado.demo
    numero=$(consultar_base "select numero from normativa.version_normativa where codigo = '$version'")
    pedir PUT "$GATEWAY/api/academico/periodos/$PERIODO/normativa" "Bearer $token" "{\"version\":${numero:-0}}"
    [[ $estado == 200 ]] || return 1
    pedir POST "$GATEWAY/api/generacion/periodos/$PERIODO/distributivo" "Bearer $token"
    [[ $estado == 201 ]] || return 1
    # Un docente afín con clase y sin sus otras actividades: deja blandas abiertas (como probar_panel.sh).
    como_coordinador_de "$carrera" || return 1
    pedir GET "$GATEWAY/api/generacion/periodos/$PERIODO/oferta/$asignatura/$paralelo/sugerencia" "Bearer $token"
    afin=$(jq -r '[.candidatos[] | select(.motivoParaNoProponer == null)][0].identificacion // empty' <<<"$cuerpo")
    [[ -n $afin ]] || { echo "       sin docente afín para $asignatura/$paralelo" >&2; return 1; }
    pedir PUT "$GATEWAY/api/generacion/periodos/$PERIODO/oferta/$asignatura/$paralelo/propuesta" "Bearer $token" \
        "$(jq -cn --arg d "$afin" '{docente: $d}')"
    [[ $estado == 200 && $(jq -r '.guardada' <<<"$cuerpo") == true ]] || return 1
    for una in "${orden[@]}"; do
        como_coordinador_de "$una" || return 1
        enviar_parcial "$una"
        [[ $estado == 200 ]] || { echo "       $una: $estado" >&2; return 1; }
    done
    iniciar "$RESPONSABLE"
    pedir GET "$FLUJO" "Bearer $token"
    [[ $estado == 200 && $(jq -r '.etapa' <<<"$cuerpo") == EN_CONSOLIDACION ]] || return 1
    # Desde F12-H05 también ve devolver parciales.
    [[ $(jq -c '.acciones' <<<"$cuerpo") == '["ENVIAR_A_REVISION","DEVOLVER_PARCIALES"]' ]] || return 1
    iniciar vicerrectorado.demo
    pedir GET "$FLUJO" "Bearer $token"
    [[ $(jq -c '.acciones' <<<"$cuerpo") == '[]' ]]
}

# --- 2. Los usuarios de los demás roles no envían a revisión: 403, todos con el mismo cuerpo ---
otros_roles() {
    local usuario primero=''
    for usuario in "${OTROS_ROLES[@]}"; do
        iniciar "$usuario"
        [[ -n $token ]] || { echo "       $usuario no inicia sesión" >&2; return 1; }
        revisar
        es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || { echo "       $usuario: $estado" >&2; return 1; }
        [[ $cuerpo != *RESPONSABLE_CONSOLIDACION* ]] || return 1
        primero=${primero:-$(jq -c 'del(.idIncidente)' <<<"$cuerpo")}
        [[ $(jq -c 'del(.idIncidente)' <<<"$cuerpo") == "$primero" ]] || return 1
    done
}

# --- 3. Sin consolidar: 409 VALIDACIONOCS_SIN_CONSOLIDAR ---
sin_consolidar() {
    iniciar "$RESPONSABLE"
    revisar
    es_codigo 409 VALIDACIONOCS_SIN_CONSOLIDAR
}

# --- 4. Consolida; sin informe: 409 VALIDACIONOCS_SIN_INFORME ---
sin_informe() {
    pedir POST "$GATEWAY/api/generacion/periodos/$PERIODO/consolidado" "Bearer $token"
    [[ $estado == 200 ]] || return 1
    revisar
    es_codigo 409 VALIDACIONOCS_SIN_INFORME
}

# --- 5. Con el informe: 409 con el número del panel, duras y blandas ---
con_abiertas() {
    local duras blandas total detalle
    hacer_pdf "$carpeta/informe.pdf"
    estado=$(curl -s -o "$carpeta/adjunto" -w '%{http_code}' -X PUT -H "Authorization: Bearer $token" \
        -F "archivo=@$carpeta/informe.pdf;type=application/pdf;filename=informe.pdf" \
        "$GATEWAY/api/generacion/periodos/$PERIODO/consolidado/informe" || echo '000')
    cuerpo=$(cat "$carpeta/adjunto")
    [[ $estado == 200 ]] || return 1
    panel
    [[ $estado == 200 ]] || return 1
    duras=$(jq -r '.abiertas.duras' <<<"$cuerpo")
    blandas=$(jq -r '.abiertas.blandas' <<<"$cuerpo")
    total=$((duras + blandas))
    abiertas_del_panel=$total
    echo "       el panel: $duras duras y $blandas blandas abiertas"
    ((blandas > 0)) || { echo "       sin blandas abiertas para el paso 6" >&2; return 1; }
    revisar
    es_codigo 409 VALIDACIONOCS_INCONSISTENCIAS_ABIERTAS || return 1
    detalle=$(jq -r '.detalles[0].descripcion' <<<"$cuerpo")
    echo "       el detalle: $detalle"
    [[ $detalle == "Quedan $total: $duras dura"*" y $blandas blanda"*" sin justificar." ]]
}

# --- 6. Al justificar una blanda, el número baja en uno ---
justificar_una() {
    local tipo objeto
    panel
    tipo=$(jq -r '[.inconsistencias[] | select(.severidad == "BLANDA" and .estado == "ABIERTA")][0].tipo' <<<"$cuerpo")
    objeto=$(jq -r '[.inconsistencias[] | select(.severidad == "BLANDA" and .estado == "ABIERTA")][0].objeto.clave' <<<"$cuerpo")
    pedir PUT "$GATEWAY/api/generacion/periodos/$PERIODO/inconsistencias/justificacion" "Bearer $token" \
        "$(jq -cn --arg t "$tipo" --arg o "$objeto" --arg x "$TEXTO" '{tipo: $t, objeto: $o, texto: $x}')"
    [[ $estado == 200 ]] || { echo "       justificar $tipo $objeto: $estado" >&2; return 1; }
    revisar
    es_codigo 409 VALIDACIONOCS_INCONSISTENCIAS_ABIERTAS || return 1
    [[ $(jq -r '.detalles[0].descripcion' <<<"$cuerpo") == "Quedan $((abiertas_del_panel - 1)):"* ]]
}

# --- 7. Ningún rechazo dejó transición ni entrada; sigue en EN_CONSOLIDACION ---
sin_rastro() {
    sleep 3
    [[ $(pasos_desde_consolidacion) == 0 ]] || return 1
    [[ $(entradas "servicio = 'validacionocs' and accion = 'CONSOLIDADO_ENVIADO_A_REVISION'") == 0 ]] || return 1
    pedir GET "$FLUJO" "Bearer $token"
    [[ $(jq -r '.etapa' <<<"$cuerpo") == EN_CONSOLIDACION ]]
}

verificar 1 "Las 10 cargas, $PERIODO abierto, una propuesta y los ${#orden[@]} parciales enviados: EN_CONSOLIDACION; solo $RESPONSABLE ve la acción" preparado
verificar 2 "Los ${#OTROS_ROLES[@]} usuarios de los demás roles no envían a revisión: 403 con el mismo cuerpo" otros_roles
verificar 3 "Sin consolidar: 409 VALIDACIONOCS_SIN_CONSOLIDAR" sin_consolidar
verificar 4 "Consolidado, sin informe: 409 VALIDACIONOCS_SIN_INFORME" sin_informe
verificar 5 "Con el informe: 409 VALIDACIONOCS_INCONSISTENCIAS_ABIERTAS con el número del panel" con_abiertas
verificar 6 "Al justificar una blanda, el número baja en uno" justificar_una
verificar 7 "Ningún rechazo dejó transición ni entrada; sigue en EN_CONSOLIDACION" sin_rastro

if ((fallas > 0)); then
    echo "Enviar a revisión de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "Enviar a revisión de punta a punta: todos los pasos pasan."
