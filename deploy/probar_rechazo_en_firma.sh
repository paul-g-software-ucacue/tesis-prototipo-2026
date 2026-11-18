#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar y esperar; shellcheck no lo ve.
# shellcheck disable=SC2329
# El rechazo en firma, de punta a punta sobre el perfil levantado, con los procesos reales (F14-H04,
# D-105; specs/050-rechazar-en-firma/contracts/demostracion.md), con la versión nueva y la anulación
# de las firmas (F12-H04, D-106; specs/051-version-nueva/contracts/demostracion.md). Uso, desde
# cualquier directorio:
# deploy/probar_rechazo_en_firma.sh
# Con una base sin estructura orgánica, carreras, distributivos ni los datos de prueba: prepara el
# periodo como probar_documento_firmable.sh (las 10 cargas de prueba, ficticias, RNF-SEG-06; el
# consolidado listo, enviado a revisión y aprobado por vicerrectorado.demo, con docente.demo
# designado), espera el documento generado y docente.demo firma F1 con el certificado ficticio de la
# CA de prueba (deploy/herramientas/FirmarDePrueba.java en eclipse-temurin:25-jdk, con las
# bibliotecas del contenedor distributivo). Con F2 en turno: rectorado.demo, fuera de turno, recibe
# 403 al rechazar; vicerrectorado.demo rechaza sin observación, con una corta y sin el campo (400),
# y después con una observación (201). En menos de 30 s el distributivo vuelve a EN_REVISION_VRA por
# el sistema con la versión 2: la 1 ANULADA, la 2 EDITABLE con lo mismo, las firmas de la 1
# anuladas y la acción de aprobar otra vez; el documento queda cerrado (subir y rechazar otra vez,
# 403; nadie tiene pendientes); el Vicerrector aprueba la versión 2, su documento se genera y F1 de
# la 2 queda en turno; y la base y la bitácora tienen F1, el rechazo, la versión nueva y la
# anulación. Si la base ya tiene alguno de esos
# datos, falla sin tocar nada. Al terminar, aunque un paso falle, borra lo que creó (también los
# archivos del volumen), salvo las entradas de la bitácora, que no se pueden borrar. Termina con 0
# si todos los pasos pasan y con 1 si alguno falla. Nunca imprime una clave ni un token.
#
# MDD_CONSERVAR=1 (solo en el perfil local): deja el distributivo en CONGELADO_EN_FIRMA con F1
# aceptada y F2 en turno, y no borra nada, para recorrer a mano specs/051-version-nueva/quickstart.md
# §3. Al terminar dice cómo limpiar.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly DATOS=backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba
readonly CARGAS=(estructura_organica carreras ciclos_academicos asignaturas_malla oferta_periodo
    docentes politica_carga_horaria cargos_autoridades proyectos_practicas matriculados_carrera)
readonly COORDINADOR=coordinacion.demo
readonly PERIODO=2026-I
readonly RESPONSABLE=consolidacion.demo
# El designado del periodo, cuyo nombre firma primero; en los datos de demostración, un DOCENTE.
readonly DESIGNADO=docente.demo
readonly CONSOLIDADO="$GATEWAY/api/generacion/periodos/$PERIODO/consolidado"
readonly PANEL="$GATEWAY/api/generacion/periodos/$PERIODO/inconsistencias"
readonly TEXTO='Aceptada por la coordinación académica para este periodo'
readonly VICERRECTOR=vicerrectorado.demo
readonly APROBACION="$GATEWAY/api/validacionocs/periodos/$PERIODO/aprobacion"
readonly DOCUMENTO="$GATEWAY/api/firma/periodos/$PERIODO/documento"
readonly PENDIENTES="$GATEWAY/api/firma/pendientes"
readonly FLUJO="$GATEWAY/api/validacionocs/periodos/$PERIODO/flujo"
readonly RECTOR=rectorado.demo
readonly OBSERVACION='Falta la carga de un docente de Software en el segundo ciclo.'
# La firma de prueba (F14-H02): la imagen con la que se compilan los procesos, los certificados
# ficticios de la CA de prueba y su contraseña ficticia (ca-prueba/generar.sh).
readonly TEMURIN=eclipse-temurin:25-jdk
readonly CA_PRUEBA=backend/servicios/firma/src/test/resources/ca-prueba
readonly CLAVE_P12=cambiar-en-local-firma
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
delete from firma.validacion_firma where periodo in ($periodos);
delete from firma.firmante_esperado where periodo in ($periodos);
delete from firma.documento_firmable where periodo in ($periodos);
delete from firma.rechazo_en_firma where periodo in ($periodos);
delete from firma.anulacion_de_firmas where periodo in ($periodos);
-- El proceso no puede cambiar los documentos, sus firmantes, sus validaciones, el rechazo ni la anulación (V2 a V7 de firma), así que tampoco
-- pasa la verificación de la clave foránea al borrar su generación; sin ellos ya, se borra sin los
-- disparadores.
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
delete from roles.designacion_responsable where periodo in ($periodos);
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
        + (select count(*) from firma.firmante_esperado where periodo in ($periodos))
        + (select count(*) from firma.validacion_firma where periodo in ($periodos))
        + (select count(*) from firma.rechazo_en_firma where periodo in ($periodos))
        + (select count(*) from firma.anulacion_de_firmas where periodo in ($periodos))
        + (select count(*) from validacionocs.rechazo_en_firma r join validacionocs.transicion t
            on t.id = r.transicion_id where t.periodo in ($periodos))
        + (select count(*) from roles.designacion_responsable where periodo in ($periodos))
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
    + (select count(*) from firma.generacion_documento where periodo in ($periodos))
    + (select count(*) from firma.firmante_esperado where periodo in ($periodos))
    + (select count(*) from firma.validacion_firma where periodo in ($periodos))
    + (select count(*) from firma.rechazo_en_firma where periodo in ($periodos))
    + (select count(*) from firma.anulacion_de_firmas where periodo in ($periodos))
    + (select count(*) from roles.designacion_responsable where periodo in ($periodos))")
if [[ $ya_estan != 0 ]]; then
    echo "FALLA  0. La base ya tiene $ya_estan unidades, carreras, datos de prueba, carreras de coordinadores o distributivos: no se tocan."
    rm -rf "$carpeta"
    exit 1
fi
# --- La firma de prueba (F14-H02): FirmarDePrueba.java con las bibliotecas del proceso ---
# Una carpeta propia, sin tokens: la herramienta, los certificados ficticios y las bibliotecas de
# /app/BOOT-INF/lib del contenedor distributivo (las mismas versiones de PDFBox y Bouncy Castle).
firmador="$carpeta/firmador"
preparar_firmador() {
    [[ -d $firmador/lib ]] && return 0
    mkdir -p "$firmador"
    cp deploy/herramientas/FirmarDePrueba.java "$CA_PRUEBA"/firmante-[123].p12 \
        "$CA_PRUEBA"/firmante-vencido.p12 "$firmador/"
    if ! docker compose cp distributivo:/app/BOOT-INF/lib "$firmador/lib" >/dev/null 2>&1 \
        || ! compgen -G "$firmador/lib/bcpkix-*.jar" >/dev/null; then
        echo "       no se pudieron copiar PDFBox y Bouncy Castle del contenedor distributivo" >&2
        return 1
    fi
    chmod -R a+rX "$firmador"
}

# firmar <entrada> <salida> <firmante 1|2|3|vencido> <campo|-> [--ca-ajena] [--alterar]
# [--momento=<instante>]: en $firmador.
firmar() {
    local entrada=$1 salida=$2 numero=$3 campo=$4
    shift 4
    preparar_firmador || return 1
    cp "$entrada" "$firmador/entrada.pdf"
    MDD_CLAVE_P12=$CLAVE_P12 docker run --rm -u "$(id -u):$(id -g)" -e MDD_CLAVE_P12 \
        -v "$firmador":/t:z -w /t "$TEMURIN" java -cp 'lib/*' FirmarDePrueba.java firmar \
        entrada.pdf salida.pdf "firmante-$numero.p12" "$campo" "$@" >"$carpeta/firmador.log" 2>&1 \
        || { sed 's/^/       /' "$carpeta/firmador.log" | tail -3 >&2; return 1; }
    mv "$firmador/salida.pdf" "$salida"
}

# subir <archivo>: el PDF al documento del periodo, con el token de $token.
subir() {
    enviar POST "$GATEWAY/api/firma/documentos/$(id_del_documento)/firmas" \
        -F "archivo=@$1;type=application/pdf"
}

# rechazada <código>: un 422 con el código y las siete comprobaciones en orden en los detalles.
rechazada() {
    es_codigo 422 "$1" && [[ $(jq -c '[.detalles[].campo]' <<<"$cuerpo") \
        == '["MISMO_DOCUMENTO","UNA_FIRMA_NUEVA","INTEGRIDAD","CONFIANZA","VIGENCIA","ORDEN","FIRMANTE"]' ]]
}

# vigente_de <usuario>: lo descarga a $carpeta/descargado.pdf; falla si no está en turno.
vigente_de() {
    iniciar "$1"
    descargar "$GATEWAY/api/firma/documentos/$(id_del_documento)/archivo"
    [[ $estado == 200 ]]
}

# turno_de <usuario> <orden> <rótulo>: su única firma pendiente del periodo es ese paso.
turno_de() {
    iniciar "$1"
    pedir GET "$PENDIENTES" "Bearer $token"
    [[ $estado == 200 && $(jq -c --arg p "$PERIODO" '[.[] | select(.periodo == $p) | [.orden, .rotulo]]' \
        <<<"$cuerpo") == "[[$2,\"$3\"]]" ]]
}

sin_pendientes() {  # sin_pendientes <usuario>
    iniciar "$1"
    pedir GET "$PENDIENTES" "Bearer $token"
    [[ $estado == 200 && $(jq -c --arg p "$PERIODO" '[.[] | select(.periodo == $p)]' <<<"$cuerpo") == '[]' ]]
}

# aceptada: un 201 con las siete comprobaciones aprobadas y el certificado.
aceptada() {
    [[ $estado == 201 && $(jq -r '.aceptada' <<<"$cuerpo") == true ]] \
        && [[ $(jq -c '[.comprobaciones[].resultado] | unique' <<<"$cuerpo") == '["APROBADA"]' ]] \
        && [[ -n $(jq -r '.certificado.nombre // empty' <<<"$cuerpo") ]]
}

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

# Lo del envío, de la aprobación y del documento, para los pasos siguientes.
hash_del_envio=''
huella_enviada=''
hash_congelado=''
aprobado_en=0
sha=''

listo_y_en_revision() {
    completo && en_consolidacion && enviado
}

# --- 1. Listo, enviado a revisión y aprobado por el Vicerrector: CONGELADO_EN_FIRMA ---
# Para el recorrido a mano: en revisión y con el Responsable designado, sin aprobar.
en_revision_y_designado() {
    listo_y_en_revision || return 1
    iniciar "$VICERRECTOR"
    esperar 15 designa "$DESIGNADO" || { echo "       no se pudo designar al Responsable" >&2; return 1; }
}

aprobado_y_congelado() {
    listo_y_en_revision || return 1
    iniciar "$VICERRECTOR"
    # El Responsable firma primero: su nombre sale de la designación del periodo (research §5).
    esperar 15 designa "$DESIGNADO" || { echo "       no se pudo designar al Responsable" >&2; return 1; }
    aprobado_en=$(date +%s)
    pedir POST "$APROBACION" "Bearer $token"
    [[ $estado == 200 && $(jq -r '.etapa' <<<"$cuerpo") == CONGELADO_EN_FIRMA ]] || return 1
    hash_congelado=$(jq -r '.congelacion.hash' <<<"$cuerpo")
    [[ $hash_congelado == "$hash_del_envio" ]]
}

designa() {  # designa <usuario>, con el token de $token
    pedir PUT "$GATEWAY/api/roles/responsables/$PERIODO" "Bearer $token" \
        "$(jq -cn --arg u "$1" '{usuario: $u}')"
    [[ $estado == 200 ]]
}

documento_generado() {  # con el token de $token
    pedir GET "$DOCUMENTO" "Bearer $token"
    [[ $estado == 200 && $(jq -r '.estado' <<<"$cuerpo") == GENERADO ]]
}

# --- 2. Generado solo, en menos de 30 s, por el sistema y con el hash de la congelación ---
generado_solo() {
    iniciar "$RESPONSABLE"
    esperar 30 documento_generado || return 1
    echo "       generado unos $(($(date +%s) - aprobado_en)) s después de aprobar"
    [[ $(jq -r '.documento.generadoPor' <<<"$cuerpo") == sistema ]] || return 1
    [[ $(jq -r '.documento.hashContenido' <<<"$cuerpo") == "$hash_congelado" ]] || return 1
    [[ $(jq -c '.acciones' <<<"$cuerpo") == '[]' ]] || return 1
    sha=$(jq -r '.documento.sha256' <<<"$cuerpo")
    [[ $sha =~ ^[0-9a-f]{64}$ ]]
}

# descargar <url>: el archivo en $carpeta/descargado.pdf y las cabeceras en $carpeta/cabeceras, con
# el token de $token; deja el estado en $estado.
descargar() {
    estado=$(curl -s -D "$carpeta/cabeceras" -o "$carpeta/descargado.pdf" -w '%{http_code}' \
        -H "Authorization: Bearer $token" "$1" || echo '000')
}

cabecera() {  # cabecera <nombre>: su valor en la última descarga, sin el fin de línea
    grep -i "^$1:" "$carpeta/cabeceras" | head -1 | cut -d' ' -f2- | tr -d '\r'
}

id_del_documento() {  # del documento de la versión 1, la que se rechaza
    consultar_base "select id from firma.documento_firmable where periodo = '$PERIODO' and version = 1"
}

# rechazar <cuerpo json>: el rechazo en firma del documento del periodo, con el token de $token.
rechazar() {
    pedir POST "$GATEWAY/api/firma/documentos/$(id_del_documento)/rechazos" "Bearer $token" "$1"
}

observacion() {  # observacion <texto>: el cuerpo de la solicitud
    jq -cn --arg o "$1" '{observacion: $o}'
}

rechazos_en_la_base() {
    consultar_base "select count(*) from firma.rechazo_en_firma where periodo = '$PERIODO'"
}

# --- 1. Aprobado por el Vicerrector y el documento generado por el sistema ---
en_firma() {
    aprobado_y_congelado && generado_solo
}

# --- 2. docente.demo firma F1 con el certificado ficticio de la CA de prueba: F2 en turno ---
# F16-H02 (D-110): la cadena de firmas que ve vicerrectorado.demo, con los estados separados por
# comas; vacía si el documento tiene las firmas anuladas.
cadena_es() {
    iniciar "$VICERRECTOR"
    pedir GET "$DOCUMENTO" "Bearer $token"
    [[ $estado == 200 && $(jq -r '[.cadena[].estado] | join(",")' <<<"$cuerpo") == "$1" ]] \
        || { echo "       cadena: $(jq -c '[.cadena[].estado]' <<<"$cuerpo" 2>/dev/null)" >&2; return 1; }
}

f1_aceptada() {
    vigente_de "$DESIGNADO" || return 1
    firmar "$carpeta/descargado.pdf" "$carpeta/f1.pdf" 1 firma-1-RESPONSABLE_CONSOLIDACION || return 1
    iniciar "$DESIGNADO"
    subir "$carpeta/f1.pdf"
    aceptada || { echo "       F1: $estado" >&2; return 1; }
    f1=$(jq -r '.sha256Subido' <<<"$cuerpo")
    turno_de "$VICERRECTOR" 2 "Revisado por" || return 1
    cadena_es "FIRMADO,EN_TURNO,PENDIENTE"
}

# --- 3. Quien no está en turno rechaza con observación y recibe 403, sin dejar fila ---
rechazo_fuera_de_turno() {
    local usuario
    for usuario in "$RECTOR" "$DESIGNADO" "$RESPONSABLE"; do
        iniciar "$usuario"
        rechazar "$(observacion "$OBSERVACION")"
        es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO \
            || { echo "       $usuario rechaza ($estado)" >&2; return 1; }
    done
    [[ $(rechazos_en_la_base) == 0 ]]
}

# --- 4. El firmante en turno sin observación, con una corta o sin el campo: 400, sin fila ---
observacion_invalida() {
    iniciar "$VICERRECTOR"
    rechazar "$(observacion '')"
    es_codigo 400 FIRMA_OBSERVACION_INVALIDA || { echo "       vacía: $estado" >&2; return 1; }
    rechazar "$(observacion '   corta   ')"
    es_codigo 400 FIRMA_OBSERVACION_INVALIDA || { echo "       corta: $estado" >&2; return 1; }
    rechazar '{}'
    es_codigo 400 COMUN_SOLICITUD_INVALIDA || { echo "       sin el campo: $estado" >&2; return 1; }
    [[ $(rechazos_en_la_base) == 0 ]] && turno_de "$VICERRECTOR" 2 "Revisado por"
}

# --- 5. El firmante en turno rechaza con una observación: 201 con el paso ---
rechazo() {
    iniciar "$VICERRECTOR"
    rechazado_en=$(date +%s)
    rechazar "$(observacion "  $OBSERVACION  ")"
    [[ $estado == 201 ]] || return 1
    [[ $(jq -c '[.orden, .rotulo, .rechazadoPor, .observacion]' <<<"$cuerpo") \
        == "$(jq -cn --arg v "$VICERRECTOR" --arg o "$OBSERVACION" '[2, "Revisado por", $v, $o]')" ]]
}

en_revision() {
    [[ $(consultar_base "select estado from validacionocs.proceso_aprobacion where periodo = '$PERIODO'") \
        == EN_REVISION_VRA ]]
}

firmas_anuladas() {
    [[ $(consultar_base "select count(*) from firma.anulacion_de_firmas where periodo = '$PERIODO'") == 1 ]]
}

# Las filas de una tabla de la versión, por número de versión: «v1:n,v2:n».
filas_por_version() {  # filas_por_version <tabla>
    consultar_base "select string_agg('v' || v.numero || ':' || (select count(*) from generacion.$1 t
        where t.version_id = v.id), ',' order by v.numero) from generacion.version_distributivo v
        join generacion.distributivo d on d.id = v.distributivo_id where d.periodo = '$PERIODO'"
}

# --- 6. En menos de 30 s y sin que nadie haga nada: EN_REVISION_VRA por el sistema con la versión 2
# (F12-H04): la 1 ANULADA, la 2 EDITABLE con lo mismo, las firmas anuladas, y se aprueba otra vez ---
vuelve_a_revision() {
    local tabla filas
    esperar 30 en_revision || { echo "       el proceso no volvió a EN_REVISION_VRA" >&2; return 1; }
    echo "       en EN_REVISION_VRA unos $(($(date +%s) - rechazado_en)) s después de rechazar"
    esperar 30 firmas_anuladas || { echo "       Firma no anuló las firmas" >&2; return 1; }
    echo "       firmas anuladas unos $(($(date +%s) - rechazado_en)) s después de rechazar"
    [[ $(consultar_base "select string_agg(tipo || ':' || desde || ':' || hacia || ':' || version || ':' || quien,
        ',' order by momento, tipo) from validacionocs.transicion where periodo = '$PERIODO'
        and tipo in ('RECHAZO_EN_FIRMA', 'VERSION_NUEVA')") \
        == "RECHAZO_EN_FIRMA:CONGELADO_EN_FIRMA:EN_REVISION_VRA:1:sistema,VERSION_NUEVA:EN_REVISION_VRA:EN_REVISION_VRA:2:sistema" ]] \
        || { echo "       las transiciones no son las esperadas" >&2; return 1; }
    [[ $(consultar_base "select string_agg(v.numero || ':' || v.estado || ':' || coalesce(v.motivo, '-')
        || ':' || coalesce(v.creada_por, '-'), ',' order by v.numero) from generacion.version_distributivo v
        join generacion.distributivo d on d.id = v.distributivo_id where d.periodo = '$PERIODO'") \
        == "1:ANULADA:-:-,2:EDITABLE:RECHAZO_EN_FIRMA:sistema" ]] \
        || { echo "       las versiones no son las esperadas" >&2; return 1; }
    # SC-002: la versión 2 tiene lo mismo que la 1, tabla por tabla. La preparación no asigna
    # actividades no lectivas: esa tabla tiene 0 en las dos; las demás, alguna fila.
    for tabla in parcial_carrera propuesta actividad_no_lectiva justificacion_inconsistencia \
        consolidacion informe_sustentado; do
        filas=$(filas_por_version "$tabla")
        if ! [[ $filas =~ ^v1:([0-9]+),v2:([0-9]+)$ && ${BASH_REMATCH[1]} == "${BASH_REMATCH[2]}" ]] \
            || [[ $tabla != actividad_no_lectiva && ${BASH_REMATCH[1]} == 0 ]]; then
            echo "       $tabla: $filas" >&2
            return 1
        fi
    done
    [[ $(consultar_base "select version || ':' || motivo || ':' || firmas_anuladas || ':' || anulada_por
        from firma.anulacion_de_firmas where periodo = '$PERIODO'") == "1:RECHAZO_EN_FIRMA:1:sistema" ]] \
        || return 1
    iniciar "$VICERRECTOR"
    pedir GET "$FLUJO" "Bearer $token"
    [[ $estado == 200 ]] || return 1
    [[ $(jq -c '[.rechazoEnFirma.orden, .rechazoEnFirma.rol, .rechazoEnFirma.por, .rechazoEnFirma.observacion,
        .rechazoEnFirma.versionAnulada, .rechazoEnFirma.versionNueva, .version]' <<<"$cuerpo") \
        == "$(jq -cn --arg v "$VICERRECTOR" --arg o "$OBSERVACION" '[2, "VICERRECTOR", $v, $o, 1, 2, 2]')" ]] \
        || { echo "       el flujo: $(jq -c '.rechazoEnFirma' <<<"$cuerpo")" >&2; return 1; }
    [[ $(jq -c '.acciones' <<<"$cuerpo") == '["APROBAR_CONTENIDO"]' ]] || return 1
    # F16-H02: con las firmas de la versión 1 anuladas, la cadena no se muestra.
    cadena_es ""
}

# --- 7. El documento queda cerrado: subir y rechazar otra vez, 403; nadie tiene pendientes ---
cerrado() {
    local usuario
    firmar "$carpeta/f1.pdf" "$carpeta/f2.pdf" 2 firma-2-VICERRECTOR || return 1
    iniciar "$VICERRECTOR"
    subir "$carpeta/f2.pdf"
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || { echo "       sube F2 ($estado)" >&2; return 1; }
    rechazar "$(observacion "$OBSERVACION")"
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || { echo "       rechaza otra vez ($estado)" >&2; return 1; }
    for usuario in "$DESIGNADO" "$VICERRECTOR" "$RECTOR" "$RESPONSABLE"; do
        sin_pendientes "$usuario" || { echo "       $usuario tiene pendientes" >&2; return 1; }
    done
}

documento_de_la_version_2() {  # con el token de $token
    documento_generado && [[ $(jq -r '.version' <<<"$cuerpo") == 2 ]]
}

# --- 8. El Vicerrector aprueba la versión 2: CONGELADO_EN_FIRMA con la 2, su documento generado y F1
# en turno de la 2, sin la 1 (SC-004) ---
aprobar_la_version_nueva() {
    iniciar "$VICERRECTOR"
    pedir POST "$APROBACION" "Bearer $token"
    [[ $estado == 200 && $(jq -r '.etapa' <<<"$cuerpo") == CONGELADO_EN_FIRMA ]] \
        || { echo "       aprobar la versión 2: $estado" >&2; return 1; }
    [[ $(jq -r '.congelacion.version' <<<"$cuerpo") == 2 ]] || return 1
    iniciar "$RESPONSABLE"
    esperar 30 documento_de_la_version_2 || { echo "       el documento de la versión 2 no se generó" >&2; return 1; }
    turno_de "$DESIGNADO" 1 "Elaborado por" || return 1
    pedir GET "$PENDIENTES" "Bearer $token"
    [[ $(jq -c --arg p "$PERIODO" '[.[] | select(.periodo == $p) | .version]' <<<"$cuerpo") == '[2]' ]] \
        || return 1
    # F16-H02: la cadena de la versión 2 empieza sin firmas, sin las de la versión 1.
    cadena_es "EN_TURNO,PENDIENTE,PENDIENTE"
}

# --- 9. La base y la bitácora: F1 intacta, el rechazo en Firma y en Aprobación ---
registro_del_rechazo() {
    [[ -n $f1 ]] || return 1
    [[ $(consultar_base "select string_agg(orden || ':' || sha256_subido, ',') from firma.validacion_firma
        where periodo = '$PERIODO' and aceptada") == "1:$f1" ]] || return 1
    [[ $(consultar_base "select orden || ':' || rechazado_por || ':' || observacion
        from firma.rechazo_en_firma where periodo = '$PERIODO'") == "2:$VICERRECTOR:$OBSERVACION" ]] || return 1
    [[ $(consultar_base "select r.orden || ':' || r.rol || ':' || r.rechazado_por
        from validacionocs.rechazo_en_firma r join validacionocs.transicion t on t.id = r.transicion_id
        where t.periodo = '$PERIODO'") == "2:VICERRECTOR:$VICERRECTOR" ]] || return 1
    docker compose exec -T distributivo test -f "/var/mdd/archivos/documentos/$f1.pdf" || return 1
    esperar 15 hay_entradas 1 "servicio = 'firma' and accion = 'RECHAZO_EN_FIRMA'
        and periodo = '$PERIODO' and nuevo ->> 'observacion' = '$OBSERVACION'" || return 1
    esperar 15 hay_entradas 1 "servicio = 'validacionocs' and accion = 'DISTRIBUTIVO_RECHAZADO_EN_FIRMA'
        and periodo = '$PERIODO' and usuario = 'sistema'" || return 1
    # F12-H04: la versión nueva en Aprobación y la anulación en Firma, por el sistema.
    esperar 15 hay_entradas 1 "servicio = 'validacionocs' and accion = 'DISTRIBUTIVO_VERSION_NUEVA'
        and periodo = '$PERIODO' and usuario = 'sistema'" || return 1
    esperar 15 hay_entradas 1 "servicio = 'firma' and accion = 'FIRMAS_ANULADAS'
        and periodo = '$PERIODO' and usuario = 'sistema'" || return 1
    sleep 3
    hay_entradas 1 "servicio = 'firma' and accion = 'RECHAZO_EN_FIRMA' and periodo = '$PERIODO'" \
        && hay_entradas 1 "servicio = 'firma' and accion = 'FIRMAS_ANULADAS' and periodo = '$PERIODO'"
}

f1=''
rechazado_en=0

if [[ -n $CONSERVAR ]]; then
    verificar 1 "El consolidado aprobado por $VICERRECTOR y el documento generado por el sistema" en_firma
    verificar 2 "$DESIGNADO firma F1 y se acepta; F2, de $VICERRECTOR, en turno" f1_aceptada
    if ((fallas > 0)); then
        echo "No quedó en firma. Para limpiar: docker compose -p tesis-prototipo-2026 down -v"
        exit 1
    fi
    echo "Quedó en CONGELADO_EN_FIRMA con F1 aceptada, sin limpiar. Recorre specs/051-version-nueva/quickstart.md §3."
    echo "Para limpiar al terminar: docker compose -p tesis-prototipo-2026 down -v"
    exit 0
fi

verificar 1 "El consolidado aprobado por $VICERRECTOR y el documento generado por el sistema" en_firma
verificar 2 "$DESIGNADO firma F1 y se acepta; F2, de $VICERRECTOR, en turno" f1_aceptada
verificar 3 "$RECTOR, $DESIGNADO y $RESPONSABLE, fuera de turno, reciben 403 al rechazar, sin dejar fila" rechazo_fuera_de_turno
verificar 4 "$VICERRECTOR sin observación, con una corta o sin el campo: 400, sin fila" observacion_invalida
verificar 5 "$VICERRECTOR rechaza con una observación: 201 con el paso 2" rechazo
verificar 6 "En menos de 30 s, EN_REVISION_VRA con la versión 2, la 1 anulada con sus firmas y con lo mismo" vuelve_a_revision
verificar 7 "El documento cerrado: subir y rechazar otra vez, 403; nadie tiene pendientes" cerrado
verificar 8 "$VICERRECTOR aprueba la versión 2: su documento y F1 en turno, sin la versión 1" aprobar_la_version_nueva
verificar 9 "F1 intacta, el rechazo, la versión nueva y la anulación en la base y en la bitácora" registro_del_rechazo

if ((fallas > 0)); then
    echo "El rechazo en firma de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "El rechazo en firma de punta a punta: todos los pasos pasan."
