#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar y esperar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Aprobar el contenido y congelar la versión, de punta a punta sobre el perfil levantado, con los
# procesos reales (F13-H01, D-100; specs/045-congelar-version/contracts/demostracion.md). Uso, desde
# cualquier directorio: deploy/probar_aprobacion.sh
# Con una base sin estructura orgánica, carreras, distributivos ni los datos de prueba: carga las 10
# cargas de prueba (ficticias, RNF-SEG-06), arma el consolidado listo como
# probar_consolidado_en_revision.sh y lo envía a revisión. Solo vicerrectorado.demo puede aprobar;
# aprueba, y el distributivo pasa a CONGELADO_EN_FIRMA con la versión congelada y el hash del envío.
# Una segunda aprobación y las siete ediciones responden 409 sin cambiar nada ni dejar entrada en la
# bitácora; consultar y descargar siguen. Si la base ya tiene alguno de esos datos, falla sin tocar
# nada. Al terminar, aunque un paso falle, borra lo que creó, salvo las entradas de la bitácora, que
# no se pueden borrar. Termina con 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime
# una clave ni un token.
#
# MDD_CONSERVAR=1 (solo en el perfil local): hace solo el paso 1 y no borra nada, para recorrer a
# mano specs/045-congelar-version/quickstart.md §3 con el distributivo en EN_REVISION_VRA. Al
# terminar dice cómo limpiar.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly DATOS=backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba
readonly CARGAS=(estructura_organica carreras ciclos_academicos asignaturas_malla oferta_periodo
    docentes politica_carga_horaria cargos_autoridades proyectos_practicas matriculados_carrera)
readonly COORDINADOR=coordinacion.demo
readonly PERIODO=2026-I
readonly RESPONSABLE=consolidacion.demo
readonly CONSOLIDADO="$GATEWAY/api/generacion/periodos/$PERIODO/consolidado"
readonly PANEL="$GATEWAY/api/generacion/periodos/$PERIODO/inconsistencias"
readonly TEXTO='Aceptada por la coordinación académica para este periodo'
readonly VICERRECTOR=vicerrectorado.demo
readonly FLUJO="$GATEWAY/api/validacionocs/periodos/$PERIODO/flujo"
readonly APROBACION="$GATEWAY/api/validacionocs/periodos/$PERIODO/aprobacion"
readonly CONSERVAR="${MDD_CONSERVAR:-}"

# MDD_CONSERVAR deja datos en la base: solo contra el perfil local, nunca contra otro equipo.
if [[ -n $CONSERVAR && ! $GATEWAY =~ ^http://(localhost|127\.0\.0\.1)(:[0-9]+)?$ ]]; then
    echo "FALLA  0. MDD_CONSERVAR solo se usa con el perfil local (MDD_GATEWAY=http://localhost:…)."
    exit 1
fi

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
# Cada asignatura-paralelo, por carrera, para proponer en todos (paso 1).
with open(f'{carpeta}/paralelos.tsv', 'w') as salida:
    for c in sorted(por_carrera):
        for u in sorted(por_carrera[c], key=lambda u: (u['codigo_asignatura'], u['paralelo'])):
            salida.write('\t'.join([c, u['codigo_asignatura'], u['paralelo']]) + '\n')
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
delete from firma.documento_firmable where periodo in ($periodos);
-- El proceso no puede cambiar los documentos (V2 de firma), así que tampoco pasa la verificación de
-- la clave foránea al borrar su generación; sin documentos ya, se borra sin los disparadores.
set session_replication_role = replica;
delete from firma.generacion_documento where periodo in ($periodos);
set session_replication_role = origin;
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
        + (select count(*) from firma.generacion_documento where periodo in ($periodos))
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
    + (select count(*) from validacionocs.proceso_aprobacion where periodo in ($periodos))
    + (select count(*) from firma.generacion_documento where periodo in ($periodos))")
if [[ $ya_estan != 0 ]]; then
    echo "FALLA  0. La base ya tiene $ya_estan unidades, carreras, datos de prueba, carreras de coordinadores o distributivos: no se tocan."
    rm -rf "$carpeta"
    exit 1
fi
if [[ -n $CONSERVAR ]]; then
    trap 'rm -rf "$carpeta"' EXIT
else
    trap limpiar EXIT
fi

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



proponer() {  # proponer <asignatura> <paralelo> <docente> [texto], con el token de $token
    local datos
    if [[ -n ${4:-} ]]; then
        datos=$(jq -cn --arg d "$3" --arg t "$4" '{docente: $d, texto: $t}')
    else
        datos=$(jq -cn --arg d "$3" '{docente: $d}')
    fi
    pedir PUT "$GATEWAY/api/generacion/periodos/$PERIODO/oferta/$1/$2/propuesta" "Bearer $token" "$datos"
}

consolidar() {  # con el token de $token
    pedir POST "$CONSOLIDADO" "Bearer $token"
}

adjuntar() {  # adjuntar <archivo>, con el token de $token; deja la respuesta en $cuerpo
    estado=$(curl -s -o "$carpeta/adjunto" -w '%{http_code}' -X PUT -H "Authorization: Bearer $token" \
        -F "archivo=@$1;type=application/pdf;filename=informe.pdf" "$CONSOLIDADO/informe" || echo '000')
    cuerpo=$(cat "$carpeta/adjunto")
}

justificar() {  # justificar <tipo> <objeto> <texto>, con el token de $token
    pedir PUT "$PANEL/justificacion" "Bearer $token" \
        "$(jq -cn --arg t "$1" --arg o "$2" --arg x "$3" '{tipo: $t, objeto: $o, texto: $x}')"
}

# --- (paso 1) Todos los paralelos con un docente de la sugerencia: el panel sin duras ---
completo() {
    local c a p actual='' elegidos elegido d sin=0
    declare -A con_clase=()
    cargar || return 1
    iniciar vicerrectorado.demo
    local numero
    numero=$(consultar_base "select numero from normativa.version_normativa where codigo = '$version'")
    pedir PUT "$GATEWAY/api/academico/periodos/$PERIODO/normativa" "Bearer $token" "{\"version\":${numero:-0}}"
    [[ $estado == 200 ]] || return 1
    pedir POST "$GATEWAY/api/generacion/periodos/$PERIODO/distributivo" "Bearer $token"
    [[ $estado == 201 ]] || return 1
    while IFS=$'\t' read -r c a p; do
        if [[ $c != "$actual" ]]; then
            como_coordinador_de "$c" || return 1
            actual=$c
        fi
        pedir GET "$GATEWAY/api/generacion/periodos/$PERIODO/oferta/$a/$p/sugerencia" "Bearer $token"
        elegidos=$(jq -r '[.candidatos[] | select(.motivoParaNoProponer == null and .horasSuficientes)] | .[].identificacion' <<<"$cuerpo")
        elegido=''
        for d in $elegidos; do
            [[ -z ${con_clase[$d]:-} ]] && { elegido=$d; break; }
        done
        [[ -n $elegido ]] || elegido=$(head -1 <<<"$elegidos")
        [[ -n $elegido ]] || elegido=$(jq -r '[.candidatos[] | select(.motivoParaNoProponer == null)][0].identificacion // empty' <<<"$cuerpo")
        [[ -n $elegido ]] || { sin=$((sin + 1)); continue; }
        proponer "$a" "$p" "$elegido"
        if [[ $estado == 200 && $(jq -r '.guardada' <<<"$cuerpo") == false ]]; then
            proponer "$a" "$p" "$elegido" "$TEXTO"
        fi
        if [[ $estado == 200 && $(jq -r '.guardada' <<<"$cuerpo") == true ]]; then
            con_clase[$elegido]=1
        else
            sin=$((sin + 1))
        fi
    done <"$carpeta/paralelos.tsv"
    iniciar "$RESPONSABLE"
    pedir GET "$PANEL" "Bearer $token"
    echo "       $sin paralelos sin propuesta; el panel: $(jq -r '.abiertas.duras' <<<"$cuerpo") duras y $(jq -r '.abiertas.blandas' <<<"$cuerpo") blandas"
    [[ $sin == 0 && $(jq -r '.abiertas.duras' <<<"$cuerpo") == 0 && $(jq -r '.enRevision' <<<"$cuerpo") == false ]]
}

# --- (paso 1) Los parciales enviados, consolidado y con el informe ---
en_consolidacion() {
    local una_carrera
    for una_carrera in "${orden[@]}"; do
        como_coordinador_de "$una_carrera" || return 1
        enviar_parcial "$una_carrera"
        [[ $estado == 200 ]] || return 1
    done
    [[ $(jq -r '.etapa' <<<"$cuerpo") == EN_CONSOLIDACION ]] || return 1
    iniciar "$RESPONSABLE"
    consolidar
    [[ $estado == 200 && $(jq -r '.enRevision' <<<"$cuerpo") == false ]] || return 1
    printf '%%PDF-1.7\n%% el informe enviado\n%%%%EOF\n' >"$carpeta/informe.pdf"
    adjuntar "$carpeta/informe.pdf"
    [[ $estado == 200 ]] || return 1
    huella_enviada=$(jq -r '.huella' <<<"$cuerpo")
    [[ -n $huella_enviada ]]
}

# --- (paso 1) Justifica las blandas y envía a revisión: pasa ---
enviado() {
    local una
    pedir GET "$PANEL" "Bearer $token"
    while read -r una; do
        justificar "$(jq -r '.[0]' <<<"$una")" "$(jq -r '.[1]' <<<"$una")" "$TEXTO"
        [[ $estado == 200 ]] || { echo "       justificar: $estado" >&2; return 1; }
    done < <(jq -c '.inconsistencias[] | select(.estado == "ABIERTA" and .severidad == "BLANDA") | [.tipo, .objeto.clave]' <<<"$cuerpo")
    pedir POST "$GATEWAY/api/validacionocs/periodos/$PERIODO/revision" "Bearer $token"
    [[ $estado == 200 && $(jq -r '.etapa' <<<"$cuerpo") == EN_REVISION_VRA ]] || return 1
    hash_del_envio=$(jq -r '[.transiciones[] | select(.tipo == "PASO_DE_ETAPA" and .desde == "EN_CONSOLIDACION")] | last | .hash' <<<"$cuerpo")
    [[ $hash_del_envio =~ ^[0-9a-f]{64}$ ]]
}

# Lo del envío a revisión y de la aprobación, para compararlo después de los rechazos.
huella_enviada=''
hash_del_envio=''
aprobado=''
contenido_congelado=''

# La huella del contenido de la versión vigente, en la base: propuestas, actividades, consolidación,
# justificaciones e informe. Si un rechazo cambiara algo, cambiaría.
contenido() {
    consultar_base "select md5(coalesce(string_agg(fila, '|' order by fila), '')) from (
        select 'p' || p::text as fila from generacion.propuesta p
            join generacion.version_distributivo v on v.id = p.version_id
            join generacion.distributivo d on d.id = v.distributivo_id where d.periodo = '$PERIODO'
        union all select 'a' || a::text from generacion.actividad_no_lectiva a
            join generacion.version_distributivo v on v.id = a.version_id
            join generacion.distributivo d on d.id = v.distributivo_id where d.periodo = '$PERIODO'
        union all select 'v' || v.numero || v.estado from generacion.version_distributivo v
            join generacion.distributivo d on d.id = v.distributivo_id where d.periodo = '$PERIODO'
        union all select 'c' || count(*)::text from generacion.registro_cambio where momento >= '$aprobado'
    ) filas"
}

pasos_de_aprobacion() {  # las transiciones desde EN_REVISION_VRA, en la base
    consultar_base "select count(*) from validacionocs.transicion where periodo = '$PERIODO'
        and tipo = 'PASO_DE_ETAPA' and desde = 'EN_REVISION_VRA'"
}

listo_y_en_revision() {
    completo && en_consolidacion && enviado
}

# --- 2. Antes de aprobar: solo el Vicerrector tiene la acción; los demás, 403 sin cambiar nada ---
solo_el_vicerrector() {
    local usuario
    for usuario in "$COORDINADOR" "$RESPONSABLE" rectorado.demo; do
        if [[ $usuario == "$COORDINADOR" ]]; then
            como_coordinador_de "${orden[0]}" || return 1
        else
            iniciar "$usuario"
        fi
        pedir GET "$FLUJO" "Bearer $token"
        [[ $estado == 200 && $(jq -r '.acciones | index("APROBAR_CONTENIDO")' <<<"$cuerpo") == null ]] \
            || { echo "       $usuario ve la acción" >&2; return 1; }
        pedir POST "$APROBACION" "Bearer $token"
        es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || { echo "       $usuario: $estado" >&2; return 1; }
    done
    [[ $(pasos_de_aprobacion) == 0 ]] || return 1
    iniciar "$VICERRECTOR"
    pedir GET "$FLUJO" "Bearer $token"
    [[ $(jq -c '.acciones' <<<"$cuerpo") == '["APROBAR_CONTENIDO"]' && $(jq -r '.congelacion // empty' <<<"$cuerpo") == '' ]]
}

# --- 3. El Vicerrector aprueba: CONGELADO_EN_FIRMA, la versión congelada con el hash del envío ---
aprueba() {
    local antes despues
    iniciar "$VICERRECTOR"
    aprobado=$(consultar_base "select now()")
    antes=$(date +%s%N)
    pedir POST "$APROBACION" "Bearer $token"
    despues=$(date +%s%N)
    echo "       la aprobación tardó $(((despues - antes) / 1000000)) ms por la API"
    [[ $estado == 200 && $(jq -r '.etapa' <<<"$cuerpo") == CONGELADO_EN_FIRMA ]] || return 1
    [[ $(jq -r '.congelacion.quien' <<<"$cuerpo") == "$VICERRECTOR" ]] || return 1
    [[ $(jq -r '.congelacion.version' <<<"$cuerpo") == "$(jq -r '.version' <<<"$cuerpo")" ]] || return 1
    # SC-002: el hash de la congelación es el del envío a revisión.
    [[ $(jq -r '.congelacion.hash' <<<"$cuerpo") == "$hash_del_envio" ]] \
        || { echo "       el hash no es el del envío" >&2; return 1; }
    [[ $(jq -c '.acciones' <<<"$cuerpo") == '[]' ]] || return 1
    [[ $(consultar_base "select v.estado from generacion.version_distributivo v
        join generacion.distributivo d on d.id = v.distributivo_id where d.periodo = '$PERIODO'
        order by v.numero desc limit 1") == CONGELADA ]] || return 1
    pedir GET "$CONSOLIDADO" "Bearer $token"
    [[ $(jq -r '.versionCongelada' <<<"$cuerpo") == true ]] || return 1
    pedir GET "$PANEL" "Bearer $token"
    [[ $(jq -r '.versionCongelada' <<<"$cuerpo") == true ]] || return 1
    contenido_congelado=$(contenido)
}

# --- 4. Una segunda aprobación: 409, y una sola transición desde EN_REVISION_VRA ---
una_sola_vez() {
    iniciar "$VICERRECTOR"
    pedir POST "$APROBACION" "Bearer $token"
    es_codigo 409 VALIDACIONOCS_ETAPA_NO_ADMITE_APROBACION || return 1
    [[ $(pasos_de_aprobacion) == 1 ]]
}

# --- 5. Las siete ediciones: 409 GENERACION_VERSION_CONGELADA, y el contenido no cambia ---
congelada() {  # congelada <qué>: la última respuesta es el 409 de la versión congelada
    es_codigo 409 GENERACION_VERSION_CONGELADA || { echo "       $1: $estado ${cuerpo:0:120}" >&2; return 1; }
}

ediciones() {
    local carrera libre asignatura paralelo tipo objeto
    IFS=$'\t' read -r carrera libre asignatura paralelo <"$carpeta/datos.tsv"
    como_coordinador_de "$carrera" || return 1
    proponer "$asignatura" "$paralelo" "$libre" "$TEXTO"
    congelada proponer || return 1
    pedir DELETE "$GATEWAY/api/generacion/periodos/$PERIODO/oferta/$asignatura/$paralelo/propuesta" "Bearer $token"
    congelada 'quitar la propuesta' || return 1
    pedir PUT "$GATEWAY/api/generacion/periodos/$PERIODO/parciales/$carrera/docentes/$libre/actividades/140-b" \
        "Bearer $token" '{"horas":4}'
    congelada 'asignar una actividad' || return 1
    pedir DELETE "$GATEWAY/api/generacion/periodos/$PERIODO/parciales/$carrera/docentes/$libre/actividades/140-b" \
        "Bearer $token"
    congelada 'quitar una actividad' || return 1
    iniciar "$RESPONSABLE"
    consolidar
    congelada consolidar || return 1
    pedir GET "$PANEL" "Bearer $token"
    tipo=$(jq -r '[.inconsistencias[] | select(.justificacion)][0].tipo' <<<"$cuerpo")
    objeto=$(jq -r '[.inconsistencias[] | select(.justificacion)][0].objeto.clave' <<<"$cuerpo")
    justificar "$tipo" "$objeto" 'Otra justificación, escrita después de congelar.'
    congelada justificar || return 1
    printf '%%PDF-1.7\n%% otro informe\n%%%%EOF\n' >"$carpeta/otro.pdf"
    adjuntar "$carpeta/otro.pdf"
    congelada adjuntar || return 1
    [[ $(contenido) == "$contenido_congelado" ]] || { echo "       el contenido cambió" >&2; return 1; }
}

# --- 6. Se consultan el consolidado y el panel, y el informe descargado es el enviado ---
consultas() {
    iniciar "$RESPONSABLE"
    pedir GET "$CONSOLIDADO" "Bearer $token"
    [[ $estado == 200 && $(jq -r '.versionCongelada' <<<"$cuerpo") == true ]] || return 1
    pedir GET "$PANEL" "Bearer $token"
    [[ $estado == 200 ]] || return 1
    estado=$(curl -s -o "$carpeta/descargado.pdf" -w '%{http_code}' -H "Authorization: Bearer $token" \
        "$CONSOLIDADO/informe" || echo '000')
    [[ $estado == 200 && $(sha256sum "$carpeta/descargado.pdf" | cut -d' ' -f1) == "$huella_enviada" ]] || return 1
    pedir GET "$FLUJO" "Bearer $token"
    [[ $estado == 200 && $(jq -r '.congelacion.hash' <<<"$cuerpo") == "$hash_del_envio" ]]
}

# --- 7. La bitácora: una CONTENIDO_APROBADO con la versión y el hash; ningún rechazo dejó rastro ---
bitacora() {
    esperar 15 hay_entradas 1 "accion = 'CONTENIDO_APROBADO'" || return 1
    [[ $(entradas "accion = 'CONTENIDO_APROBADO' and usuario = '$VICERRECTOR'
        and objeto ->> 'hash' = '$hash_del_envio' and objeto ->> 'version' is not null
        and anterior ->> 'estado' = 'EN_REVISION_VRA' and nuevo ->> 'estado' = 'CONGELADO_EN_FIRMA'") == 1 ]] \
        || return 1
    sleep 3
    [[ $(consultar_base "select count(*) from usuarios.bitacora where recibida_en >= '$aprobado'
        and accion <> 'CONTENIDO_APROBADO' and servicio in ('generacion', 'validacionocs')") == 0 ]]
}

if [[ -n $CONSERVAR ]]; then
    verificar 1 "El consolidado listo y enviado a revisión: EN_REVISION_VRA" listo_y_en_revision
    if ((fallas > 0)); then
        echo "No quedó en revisión. Para limpiar: docker compose -p tesis-prototipo-2026 down -v"
        exit 1
    fi
    echo "Quedó en EN_REVISION_VRA, sin limpiar. Recorre specs/045-congelar-version/quickstart.md §3."
    echo "Para limpiar al terminar: docker compose -p tesis-prototipo-2026 down -v"
    exit 0
fi

verificar 1 "El consolidado listo y enviado a revisión: EN_REVISION_VRA, con el hash del envío" listo_y_en_revision
verificar 2 "Antes de aprobar, solo vicerrectorado.demo tiene APROBAR_CONTENIDO; los demás, 403 sin cambiar nada" solo_el_vicerrector
verificar 3 "vicerrectorado.demo aprueba: CONGELADO_EN_FIRMA, la versión congelada con el hash del envío" aprueba
verificar 4 "Una segunda aprobación: 409 VALIDACIONOCS_ETAPA_NO_ADMITE_APROBACION; una sola transición" una_sola_vez
verificar 5 "Las siete ediciones: 409 GENERACION_VERSION_CONGELADA, y el contenido no cambia" ediciones
verificar 6 "Se consultan el consolidado y el panel, y se descarga el informe enviado" consultas
verificar 7 "La bitácora: una CONTENIDO_APROBADO con la versión y el hash; ningún rechazo dejó rastro" bitacora

if ((fallas > 0)); then
    echo "Aprobar y congelar de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "Aprobar y congelar de punta a punta: todos los pasos pasan."
