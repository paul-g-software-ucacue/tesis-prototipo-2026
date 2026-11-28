#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar y esperar; shellcheck no lo ve.
# shellcheck disable=SC2329
# El documento firmable del consolidado, de punta a punta sobre el perfil levantado, con los procesos
# reales (F13-H02, D-101; specs/046-pdfa-consolidado/contracts/demostracion.md). Uso, desde cualquier
# directorio: deploy/probar_documento_firmable.sh
# Con una base sin estructura orgánica, carreras, distributivos ni los datos de prueba: carga las 10
# cargas de prueba (ficticias, RNF-SEG-06), arma el consolidado listo como probar_aprobacion.sh, lo
# envía a revisión, designa a docente.demo Responsable del periodo y vicerrectorado.demo lo
# aprueba. Sin que nadie lo pida, Firma genera el PDF/A con los tres campos de firma vacíos, lo
# guarda en el volumen por su SHA-256 y lo registra; veraPDF (verapdf/cli:v1.30.2, perfil 2b) lo da
# conforme. Pedirlo otra vez devuelve el mismo, sin otro
# archivo ni otra entrada en la bitácora. Desde F14-H01 (D-102;
# specs/047-firmas-pendientes/contracts/demostracion.md): docente.demo, el designado, ve su firma
# pendiente y descarga el PDF vigente con su SHA-256; quien no está en turno (consolidacion.demo,
# con el rol pero sin la designación, y vicerrectorado.demo y rectorado.demo) no la ve y recibe
# 403, y coordinacion.demo, sin el permiso, también. Desde F14-H02 (D-103;
# specs/048-validar-firma/contracts/demostracion.md): docente.demo sube cinco PDF que se rechazan con
# su 422 (otro documento, sin firma nueva, dos firmas nuevas, alterado y de una CA ajena) sin que
# cambien el turno ni el archivo vigente; quien no está en turno recibe 403 al subir; y las tres
# firmas (F1, F2 y F3) se aceptan en orden, cada una sobre el vigente que deja la anterior, con su
# resultado en la base y en la bitácora. Firma con deploy/herramientas/FirmarDePrueba.java en
# eclipse-temurin:25-jdk, con las bibliotecas del contenedor distributivo y los certificados
# ficticios de la CA de prueba. Desde F14-H03 (D-104; specs/049-completar-cadena/contracts/
# demostracion.md): los certificados ficticios tienen el CN del nombre impreso de cada paso; se
# rechazan además la firma de la persona de F2 en el turno de F1 (fuera de orden), un certificado
# vencido al subir y, en el turno de F2, la de la persona de F1 (firmante equivocado); con la
# tercera firma, el distributivo pasa a EN_OCS por el sistema en menos de 30 s. Desde F15-H01 (D-107;
# specs/052-resolucion-ocs/contracts/demostracion.md): en EN_OCS, el avance genérico responde 409
# porque falta la resolución, y quien no es la Secretaría recibe 403 al registrarla; los registros
# inválidos (número vacío, fecha futura o anterior a EN_OCS, un archivo que no es PDF y uno de más
# de 10 MB) se rechazan sin dejar fila; secretaria.demo registra la resolución, el distributivo
# queda en APROBADO y el PDF se descarga idéntico. Desde F16-H01 (D-108;
# specs/053-notificaciones-internas/contracts/demostracion.md): una transición se hace con
# identidad detenido, sin error para el usuario, y su notificación llega cuando vuelve; al final,
# las bandejas de todos los usuarios de la demo tienen exactamente las notificaciones de cada
# transición, vicerrectorado.demo lee y marca la suya, y una entrega repetida no duplica nada. Desde
# F16-H02 (D-110; specs/054-siguiente-firmante/contracts/demostracion.md): la cadena de firmas con
# el estado de cada paso después de generar y de cada firma; F1 se sube con identidad detenido, y la
# notificación de firma pendiente de F2 llega cuando vuelve; F2 y F3 reciben la suya, y nadie más.
# Desde F18-H01 (D-111; specs/055-publicar-distributivo/contracts/demostracion.md): en EN_OCS,
# publicar responde 409; con APROBADO, vicerrectorado.demo y coordinacion.demo reciben 403 al
# publicar y secretaria.demo publica con identidad detenido (la transición de F16-H01, que antes
# era el envío a revisión), con su constancia y su bitácora, y una segunda publicación da 409;
# publicado, nadie tiene acciones y cada operación de cambio responde con su error; la publicación
# llega una vez a datos.demo, coordinacion.demo, docente.demo y docente.coordinador.demo. Desde
# F18-H02 (D-113; specs/056-reforma-version-publicada/contracts/demostracion.md): en APROBADO, la
# reforma responde 409; publicado, solo vicerrectorado.demo puede iniciarla; secretaria.demo y
# rectorado.demo reciben 403 y un motivo corto, 400; vicerrectorado.demo la inicia con identidad
# detenido, el distributivo vuelve a EN_CONSOLIDACION con la versión 2 y la 1 sigue CONGELADA y
# vigente; la notificación llega una vez a quien consolida, los docentes, Talento Humano y las
# coordinaciones; la versión 2 recorre el ciclo completo hasta publicarse, y la 1 conserva sus
# firmas, su documento y su resolución. Desde F20-H01 (D-115;
# specs/058-reporte-individual-docente/contracts/demostracion.md), los pasos R1 a R6: en EN_OCS,
# Reporting no tiene publicación (409); con la versión 1 publicada, rectorado.demo y
# secretaria.demo ven el reporte de un docente y lo descargan en PDF/A (veraPDF, sin campos) y en
# CSV (los mismos datos, idéntico entre descargas), cada descarga queda en la bitácora y ver el
# reporte no; coordinacion.demo y docente.demo reciben 403; durante la reforma, el reporte sigue
# saliendo de la versión 1 y no cambia al corregir las horas de un paralelo (que después se
# restauran); con la versión 2 publicada, sale de ella. Desde F20-H02 (D-117;
# specs/059-reporte-general-consolidado/contracts/demostracion.md), R7 y R8, y R1, R4, R5 y R6
# ampliados: el reporte general tiene a los docentes del listado, cada fila igual a su reporte
# individual y los totales iguales a la suma de las filas; se descarga en PDF/A y en CSV con los
# mismos datos, cada descarga queda como REPORTE_GENERAL_DESCARGADO y no cambia durante la
# reforma. Desde F19-H02 (D-120; specs/061-exportacion-siga/contracts/demostracion.md), R11 y la suma
# de verificación y el registro de cada exportación en R1, R4, R5 y R9. Desde F19-H01 (D-119;
# specs/060-csv-siga/contracts/demostracion.md), R9 y R10, y R1, R4
# y R5 ampliados: secretaria.demo genera el CSV para SIGA de la versión 1, con una fila por
# asignatura y por actividad, los totales del reporte general y las convenciones de SENESCYT,
# idéntico entre dos pedidos y con CSV_SIGA_GENERADO en la bitácora; carga un layout nuevo (otros
# nombres, otro orden, sin la fecha y con el componente traducido) y el CSV sale con sus columnas y
# los mismos totales; uno inválido se rechaza con sus problemas sin cambiar el vigente; el Rector,
# la coordinación y el Administrador reciben 403; y durante la reforma el CSV no cambia. Las
# versiones del layout de exportación y sus entradas no se borran (principio VII). Si la base ya tiene
# alguno de esos datos, falla sin tocar nada. Al terminar, aunque un paso falle, borra lo que creó (también los archivos del volumen),
# salvo las entradas de la bitácora, que no se pueden borrar. Termina con 0 si todos los pasos pasan
# y con 1 si alguno falla. Nunca imprime una clave ni un token.
#
# MDD_CONSERVAR=1 (solo en el perfil local): deja el distributivo en EN_REVISION_VRA, con
# docente.demo designado Responsable, y no borra nada, para recorrer a mano
# specs/049-completar-cadena/quickstart.md §3. MDD_CONSERVAR=EN_OCS lo deja en EN_OCS, con las tres
# firmas, para recorrer specs/052-resolucion-ocs/quickstart.md §3. MDD_CONSERVAR=F2 lo deja con F1
# aceptada y F2 en turno, para recorrer specs/054-siguiente-firmante/quickstart.md §3.
# MDD_CONSERVAR=APROBADO lo deja con la resolución registrada, sin publicar, para recorrer
# specs/055-publicar-distributivo/quickstart.md §3. MDD_CONSERVAR=PUBLICADO lo deja con la versión
# 1 publicada, sin reformar y sin revisar las notificaciones (pasos 18 a 21), para recorrer
# specs/056-reforma-version-publicada/quickstart.md §3.
# Al terminar dice cómo limpiar.
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
readonly VERAPDF=verapdf/cli:v1.30.2
# La firma de prueba (F14-H02): la imagen con la que se compilan los procesos, los certificados
# ficticios de la CA de prueba y su contraseña ficticia (ca-prueba/generar.sh).
readonly TEMURIN=eclipse-temurin:25-jdk
readonly CA_PRUEBA=backend/servicios/firma/src/test/resources/ca-prueba
readonly CLAVE_P12=cambiar-en-local-firma
readonly CONSERVAR="${MDD_CONSERVAR:-}"
readonly SECRETARIA=secretaria.demo
readonly RESOLUCION="$GATEWAY/api/validacionocs/periodos/$PERIODO/resolucion"
readonly NUMERO='RPC-SO-12-No.123-2026'
readonly PUBLICACION="$GATEWAY/api/validacionocs/periodos/$PERIODO/publicacion"
readonly PUBLICADA="El distributivo de $PERIODO fue publicado"
readonly REFORMA_URL="$GATEWAY/api/validacionocs/periodos/$PERIODO/reforma"
readonly MOTIVO='Cambia la docente de una carrera por una licencia (prueba).'
readonly REFORMADA="Se inició una reforma del distributivo de $PERIODO"
# Dos ciclos del reenvío (30 s) y un margen (SC-002 de F16-H01).
readonly ESPERA_REENVIO=120

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
-- El proceso no puede cambiar los documentos, sus firmantes ni sus validaciones (V2 a V4 de firma), así que tampoco
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
        + (select count(*) from firma.validacion_firma where periodo in ($periodos))
        + (select count(*) from validacionocs.resolucion_ocs where periodo in ($periodos))
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

# --- 9. Cinco PDF rechazados con su 422; el turno y el archivo vigente no cambian ---
rechazos() {
    local campo=firma-1-RESPONSABLE_CONSOLIDACION vigente="$carpeta/vigente.pdf"
    vigente_de "$DESIGNADO" || return 1
    cp "$carpeta/descargado.pdf" "$vigente"
    subir "$CA_PRUEBA/pdfa-prueba.pdf"
    rechazada FIRMA_NO_ES_LA_VERSION_VIGENTE || { echo "       otro documento: $estado" >&2; return 1; }
    subir "$vigente"
    rechazada FIRMA_SIN_UNA_FIRMA_NUEVA || { echo "       sin firma nueva: $estado" >&2; return 1; }
    firmar "$vigente" "$carpeta/una.pdf" 1 "$campo" || return 1
    firmar "$carpeta/una.pdf" "$carpeta/dos.pdf" 1 - || return 1
    iniciar "$DESIGNADO"
    subir "$carpeta/dos.pdf"
    rechazada FIRMA_SIN_UNA_FIRMA_NUEVA || { echo "       dos firmas nuevas: $estado" >&2; return 1; }
    firmar "$vigente" "$carpeta/alterado.pdf" 1 "$campo" --alterar || return 1
    subir "$carpeta/alterado.pdf"
    rechazada FIRMA_NO_INTEGRA || { echo "       alterado: $estado" >&2; return 1; }
    firmar "$vigente" "$carpeta/ajena.pdf" 1 "$campo" --ca-ajena || return 1
    subir "$carpeta/ajena.pdf"
    rechazada FIRMA_SIN_CONFIANZA || { echo "       CA ajena: $estado" >&2; return 1; }
    # F14-H03: la persona de F2 en el turno de F1, y un certificado vencido al subir que era
    # vigente al firmar.
    firmar "$vigente" "$carpeta/de-f2.pdf" 2 - || return 1
    subir "$carpeta/de-f2.pdf"
    rechazada FIRMA_FUERA_DE_ORDEN || { echo "       fuera de orden: $estado" >&2; return 1; }
    firmar "$vigente" "$carpeta/vencido.pdf" vencido "$campo" --momento=2025-01-15T12:00:00Z \
        || return 1
    subir "$carpeta/vencido.pdf"
    rechazada FIRMA_CERTIFICADO_NO_VIGENTE || { echo "       vencido: $estado" >&2; return 1; }
    turno_de "$DESIGNADO" 1 "Elaborado por" || return 1
    vigente_de "$DESIGNADO" || return 1
    [[ $(sha256sum "$carpeta/descargado.pdf" | cut -d' ' -f1) == "$sha" ]] || return 1
    [[ $(consultar_base "select count(*) from firma.validacion_firma where periodo = '$PERIODO'
        and not aceptada") == 7 ]]
}

# --- 10. Quien no está en turno sube un PDF bien firmado y recibe 403, sin dejar fila ---
fuera_de_turno() {
    local usuario
    [[ -f $carpeta/una.pdf ]] || return 1
    for usuario in "$VICERRECTOR" "$RESPONSABLE"; do
        iniciar "$usuario"
        subir "$carpeta/una.pdf"
        es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || { echo "       $usuario sube ($estado)" >&2; return 1; }
    done
    [[ $(consultar_base "select count(*) from firma.validacion_firma where periodo = '$PERIODO'") == 7 ]]
}

# aceptada: un 201 con las siete comprobaciones aprobadas y el certificado.
aceptada() {
    [[ $estado == 201 && $(jq -r '.aceptada' <<<"$cuerpo") == true ]] \
        && [[ $(jq -c '[.comprobaciones[].resultado] | unique' <<<"$cuerpo") == '["APROBADA"]' ]] \
        && [[ -n $(jq -r '.certificado.nombre // empty' <<<"$cuerpo") ]]
}

# F16-H02 (D-110): la cadena de firmas, como la ve vicerrectorado.demo en «Consolidado del periodo».
# cadena_es <estados separados por comas>: los tres pasos con su estado, cada uno con su nombre
# impreso y con la fecha de la firma solo si está firmado.
cadena_es() {
    iniciar "$VICERRECTOR"
    pedir GET "$DOCUMENTO" "Bearer $token"
    [[ $estado == 200 ]] || return 1
    [[ $(jq -r '[.cadena[].estado] | join(",")' <<<"$cuerpo") == "$1" ]] \
        || { echo "       cadena: $(jq -c '[.cadena[].estado]' <<<"$cuerpo")" >&2; return 1; }
    [[ $(jq '[.cadena[] | select((.firmante // "") == "")] | length' <<<"$cuerpo") == 0 ]] || return 1
    [[ $(jq '[.cadena[] | select((.estado == "FIRMADO") != (.firmadoEn != null))] | length' <<<"$cuerpo") == 0 ]]
}

# firmado_en <orden> <subidoEn>: la fecha del paso en la cadena es la de la firma aceptada. Se
# compara en microsegundos, la precisión de la base: la respuesta de la subida trae nanosegundos.
firmado_en() {
    local en_cadena
    en_cadena=$(jq -r --argjson o "$1" '.cadena[] | select(.orden == $o) | .firmadoEn' <<<"$cuerpo")
    [[ $(date -d "$en_cadena" +%s%6N) == $(date -d "$2" +%s%6N) ]] \
        || { echo "       F$1: firmadoEn $en_cadena, subidoEn $2" >&2; return 1; }
}

# --- 11. Las tres firmas en orden, cada una sobre el vigente que deja la anterior ---
firmados=()
tres_firmas() {
    local i usuarios=("$DESIGNADO" "$VICERRECTOR" rectorado.demo)
    local campos=(firma-1-RESPONSABLE_CONSOLIDACION firma-2-VICERRECTOR firma-3-RECTOR)
    local rotulos=("Elaborado por" "Revisado por" "Aprobado por")
    for i in 0 1 2; do
        turno_de "${usuarios[$i]}" $((i + 1)) "${rotulos[$i]}" \
            || { echo "       no es el turno de ${usuarios[$i]}" >&2; return 1; }
        vigente_de "${usuarios[$i]}" || return 1
        if ((i == 1)); then
            # F14-H03: la Vicerrectora sube el vigente firmado por la persona de F1.
            firmar "$carpeta/descargado.pdf" "$carpeta/equivocado.pdf" 1 - || return 1
            iniciar "${usuarios[$i]}"
            subir "$carpeta/equivocado.pdf"
            rechazada FIRMA_FIRMANTE_EQUIVOCADO \
                || { echo "       firmante equivocado: $estado" >&2; return 1; }
        fi
        firmar "$carpeta/descargado.pdf" "$carpeta/firmado-$i.pdf" $((i + 1)) "${campos[$i]}" || return 1
        iniciar "${usuarios[$i]}"
        # F16-H02 (SC-002): F1 se sube con identidad detenido, con el token ya pedido; el designado
        # se reconoce por su usuario, así que la subida no necesita a identidad. F2 y F3 sí.
        if ((i == 0)) && [[ -z $CONSERVAR ]]; then
            caida_firma=$(consultar_base "select now()")
            docker compose stop identidad >/dev/null 2>&1
        fi
        subir "$carpeta/firmado-$i.pdf"
        local estado_de_la_subida=$estado cuerpo_de_la_subida=$cuerpo
        if [[ -n $caida_firma && -z $vuelta_firma ]]; then
            vuelta_firma=$(consultar_base "select now()")
            docker compose up -d --wait --no-recreate --no-build identidad >/dev/null 2>&1 || return 1
        fi
        estado=$estado_de_la_subida
        cuerpo=$cuerpo_de_la_subida
        aceptada || { echo "       ${usuarios[$i]}: $estado" >&2; return 1; }
        [[ $(jq -r '.sha256Subido' <<<"$cuerpo") == $(sha256sum "$carpeta/firmado-$i.pdf" | cut -d' ' -f1) ]] \
            || return 1
        firmados+=("$(jq -r '.sha256Subido' <<<"$cuerpo")")
        local subido_en
        subido_en=$(jq -r '.subidoEn' <<<"$cuerpo")
        sin_pendientes "${usuarios[$i]}" || { echo "       ${usuarios[$i]} sigue en turno" >&2; return 1; }
        local esperada=("FIRMADO,EN_TURNO,PENDIENTE" "FIRMADO,FIRMADO,EN_TURNO" "FIRMADO,FIRMADO,FIRMADO")
        cadena_es "${esperada[$i]}" || return 1
        firmado_en $((i + 1)) "$subido_en" || return 1
        if [[ $CONSERVAR == F2 ]]; then
            return 0
        fi
    done
    for i in "${usuarios[@]}" "$RESPONSABLE"; do
        sin_pendientes "$i" || return 1
    done
}

en_ocs() {
    [[ $(consultar_base "select estado from validacionocs.proceso_aprobacion where periodo = '$PERIODO'") \
        == EN_OCS ]]
}

# --- 12. La base, los archivos, la bitácora y EN_OCS ---
registro_de_firmas() {
    local hash
    ((${#firmados[@]} == 3)) || return 1
    [[ $(consultar_base "select string_agg(orden::text, ',' order by orden) from firma.validacion_firma
        where periodo = '$PERIODO' and aceptada") == 1,2,3 ]] || return 1
    [[ $(consultar_base "select count(*) from firma.validacion_firma where periodo = '$PERIODO'
        and not aceptada") == 8 ]] || return 1
    for hash in "$sha" "${firmados[@]}"; do
        docker compose exec -T distributivo test -f "/var/mdd/archivos/documentos/$hash.pdf" \
            || { echo "       falta documentos/$hash.pdf" >&2; return 1; }
    done
    # F1 se subió con identidad detenido (F16-H02): su entrada llega con el reenvío.
    esperar "$ESPERA_REENVIO" hay_entradas 3 "servicio = 'firma' and accion = 'FIRMA_ACEPTADA'" || return 1
    esperar "$ESPERA_REENVIO" hay_entradas 8 "servicio = 'firma' and accion = 'FIRMA_RECHAZADA'" || return 1
    [[ $(entradas "servicio = 'firma' and accion like 'FIRMA_%' and periodo = '$PERIODO'
        and objeto -> 'comprobaciones' ? 'FIRMANTE'") == 11 ]] || return 1
    # F14-H03: la cadena completa pasa a EN_OCS por el sistema, en menos de 30 s (SC-002).
    esperar 30 en_ocs || { echo "       el proceso no llegó a EN_OCS" >&2; return 1; }
    [[ $(consultar_base "select count(*) from validacionocs.transicion where periodo = '$PERIODO'
        and desde = 'CONGELADO_EN_FIRMA' and hacia = 'EN_OCS' and quien = 'sistema'") == 1 ]] || return 1
    esperar 15 hay_entradas 1 "servicio = 'validacionocs' and accion = 'CADENA_DE_FIRMAS_COMPLETA'
        and periodo = '$PERIODO'"
}

# --- F15-H01: la resolución del OCS (D-107) ---

resoluciones_en_la_base() {
    consultar_base "select count(*) from validacionocs.resolucion_ocs where periodo = '$PERIODO'"
}

# registrar <número> <fecha> <archivo>: la resolución, con el token de $token.
registrar() {
    enviar POST "$RESOLUCION" -F "numero=$1" -F "fecha=$2" -F "archivo=@$3;type=application/pdf"
}

hoy() {
    TZ=America/Guayaquil date +%F
}

# --- 13. En EN_OCS: el avance genérico, 409 porque falta la resolución; otros roles, 403 al registrar ---
# F18-H01: publicar todavía no, 409.
sin_resolucion() {
    local usuario
    printf '%%PDF-1.7\nResolución del OCS de prueba (ficticia)\n%%%%EOF\n' >"$carpeta/resolucion.pdf"
    iniciar "$SECRETARIA"
    pedir POST "$GATEWAY/api/validacionocs/periodos/$PERIODO/avance" "Bearer $token"
    es_codigo 409 VALIDACIONOCS_REQUIERE_RESOLUCION || { echo "       avance: $estado" >&2; return 1; }
    pedir POST "$PUBLICACION" "Bearer $token"
    es_codigo 409 VALIDACIONOCS_ETAPA_NO_ADMITE_PUBLICACION || { echo "       publicar: $estado" >&2; return 1; }
    for usuario in "$VICERRECTOR" rectorado.demo "$RESPONSABLE"; do
        iniciar "$usuario"
        registrar "$NUMERO" "$(hoy)" "$carpeta/resolucion.pdf"
        es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO \
            || { echo "       $usuario registra ($estado)" >&2; return 1; }
    done
    en_ocs && [[ $(resoluciones_en_la_base) == 0 ]]
}

# --- 14. Los registros inválidos: su 400 o su 413, sin dejar fila ---
resoluciones_invalidas() {
    local llegada
    llegada=$(consultar_base "select to_char(momento at time zone 'America/Guayaquil', 'YYYY-MM-DD')
        from validacionocs.transicion where periodo = '$PERIODO' and hacia = 'EN_OCS'")
    printf 'no es un PDF\n' >"$carpeta/texto.pdf"
    { printf '%%PDF-1.7\n'; head -c $((10 * 1024 * 1024)) /dev/zero; printf '\n%%%%EOF\n'; } \
        >"$carpeta/grande.pdf"
    iniciar "$SECRETARIA"
    registrar '   ' "$(hoy)" "$carpeta/resolucion.pdf"
    es_codigo 400 VALIDACIONOCS_NUMERO_DE_RESOLUCION_INVALIDO || { echo "       número: $estado" >&2; return 1; }
    registrar "$NUMERO" "$(TZ=America/Guayaquil date -d tomorrow +%F)" "$carpeta/resolucion.pdf"
    es_codigo 400 VALIDACIONOCS_FECHA_DE_RESOLUCION_FUERA_DE_RANGO \
        || { echo "       futura: $estado" >&2; return 1; }
    registrar "$NUMERO" "$(date -d "$llegada - 1 day" +%F)" "$carpeta/resolucion.pdf"
    es_codigo 400 VALIDACIONOCS_FECHA_DE_RESOLUCION_FUERA_DE_RANGO \
        || { echo "       anterior: $estado" >&2; return 1; }
    registrar "$NUMERO" "$(hoy)" "$carpeta/texto.pdf"
    es_codigo 400 VALIDACIONOCS_RESOLUCION_NO_ES_PDF || { echo "       texto: $estado" >&2; return 1; }
    registrar "$NUMERO" "$(hoy)" "$carpeta/grande.pdf"
    es_codigo 413 VALIDACIONOCS_RESOLUCION_DEMASIADO_GRANDE || { echo "       grande: $estado" >&2; return 1; }
    en_ocs && [[ $(resoluciones_en_la_base) == 0 ]]
}

# --- 15. secretaria.demo registra: APROBADO, el PDF por su huella y descargado idéntico, y la bitácora ---
resolucion_registrada() {
    local huella
    huella=$(sha256sum "$carpeta/resolucion.pdf" | cut -d' ' -f1)
    iniciar "$SECRETARIA"
    registrar "  $NUMERO  " "$(hoy)" "$carpeta/resolucion.pdf"
    [[ $estado == 200 && $(jq -r '.etapa' <<<"$cuerpo") == APROBADO ]] \
        || { echo "       registrar: $estado" >&2; return 1; }
    [[ $(jq -c '[.resolucion.numero, .resolucion.fecha, .resolucion.registradaPor, .resolucion.sha256]' \
        <<<"$cuerpo") == "$(jq -cn --arg n "$NUMERO" --arg f "$(hoy)" --arg s "$SECRETARIA" --arg h "$huella" \
        '[$n, $f, $s, $h]')" ]] || { echo "       la resolución en el flujo" >&2; return 1; }
    [[ $(consultar_base "select count(*) from validacionocs.transicion t join validacionocs.resolucion_ocs r
        on r.transicion_id = t.id where t.periodo = '$PERIODO' and t.desde = 'EN_OCS' and t.hacia = 'APROBADO'
        and t.quien = '$SECRETARIA' and r.numero = '$NUMERO' and r.sha256 = '$huella'") == 1 ]] || return 1
    docker compose exec -T distributivo test -f "/var/mdd/archivos/resoluciones/$huella.pdf" \
        || { echo "       falta resoluciones/$huella.pdf" >&2; return 1; }
    iniciar "$VICERRECTOR"
    descargar "$RESOLUCION/archivo"
    [[ $estado == 200 && $(sha256sum "$carpeta/descargado.pdf" | cut -d' ' -f1) == "$huella" ]] \
        || { echo "       descarga del Vicerrector: $estado" >&2; return 1; }
    iniciar "$COORDINADOR"
    pedir GET "$RESOLUCION/archivo" "Bearer $token"
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || { echo "       coordinación descarga ($estado)" >&2; return 1; }
    esperar 15 hay_entradas 1 "servicio = 'validacionocs' and accion = 'RESOLUCION_REGISTRADA'
        and periodo = '$PERIODO' and usuario = '$SECRETARIA'" || return 1
    # F18-H02: aprobado todavía no se reforma.
    iniciar "$VICERRECTOR"
    reformar "$MOTIVO"
    es_codigo 409 VALIDACIONOCS_ETAPA_NO_ADMITE_REFORMA || { echo "       reforma en APROBADO: $estado" >&2; return 1; }
}

# --- F18-H01: publicar (D-111) ---

publicaciones() {
    consultar_base "select count(*) from validacionocs.transicion where periodo = '$PERIODO'
        and hacia = 'PUBLICADO'"
}

transiciones() {
    consultar_base "select count(*) from validacionocs.transicion where periodo = '$PERIODO'"
}

# --- 16. Publicar: 403 a otros roles; secretaria.demo publica con identidad detenido; la segunda, 409 ---
publicado() {
    local usuario estado_de_la_publicacion cuerpo_de_la_publicacion
    for usuario in "$VICERRECTOR" "$COORDINADOR"; do
        iniciar "$usuario"
        pedir POST "$PUBLICACION" "Bearer $token"
        es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || { echo "       $usuario publica ($estado)" >&2; return 1; }
    done
    [[ $(publicaciones) == 0 ]] || return 1
    # SC-003 (y SC-002 de F16-H01): con el token ya pedido, identidad detenido; la notificación
    # llega cuando vuelve (paso 19).
    iniciar "$SECRETARIA"
    caida=$(consultar_base "select now()")
    docker compose stop identidad >/dev/null 2>&1
    pedir POST "$PUBLICACION" "Bearer $token"
    estado_de_la_publicacion=$estado
    cuerpo_de_la_publicacion=$cuerpo
    # La vuelta es cuando se pide levantarlo, como en F16-H01.
    vuelta=$(consultar_base "select now()")
    docker compose up -d --wait --no-recreate --no-build identidad >/dev/null 2>&1 || return 1
    estado=$estado_de_la_publicacion
    cuerpo=$cuerpo_de_la_publicacion
    [[ $estado == 200 && $(jq -r '.etapa' <<<"$cuerpo") == PUBLICADO ]] \
        || { echo "       publicar: $estado" >&2; return 1; }
    [[ $(jq -c '[.publicacion.version, .publicacion.publicadaPor, .acciones]' <<<"$cuerpo") \
        == "[1,\"$SECRETARIA\",[]]" ]] || { echo "       la constancia en el flujo" >&2; return 1; }
    [[ $(publicaciones) == 1 ]] || return 1
    # Publicado con identidad detenido: la entrada de la bitácora llega con el reenvío.
    esperar "$ESPERA_REENVIO" hay_entradas 1 "servicio = 'validacionocs' and accion = 'DISTRIBUTIVO_PUBLICADO'
        and periodo = '$PERIODO' and usuario = '$SECRETARIA'" || { echo "       sin la bitácora" >&2; return 1; }
    pedir POST "$PUBLICACION" "Bearer $token"
    es_codigo 409 VALIDACIONOCS_ETAPA_NO_ADMITE_PUBLICACION || { echo "       la segunda: $estado" >&2; return 1; }
    [[ $(publicaciones) == 1 ]]
}

# --- 17. Publicado: nadie edita (solo el Vicerrector puede reformar), y cada cambio da su error ---
sin_acciones() {
    local usuario antes carrera asignatura paralelo docente
    antes=$(transiciones)
    for usuario in admin.demo datos.demo "$COORDINADOR" gestion.demo "$RESPONSABLE" "$VICERRECTOR" \
        rectorado.demo ocs.demo "$SECRETARIA" "$DESIGNADO" docente.coordinador.demo; do
        iniciar "$usuario"
        pedir GET "$GATEWAY/api/validacionocs/periodos/$PERIODO/flujo" "Bearer $token"
        if [[ $estado == 200 ]]; then
            # F18-H02, D-113: solo el Vicerrector, para iniciar una reforma, que no edita la publicada.
            if [[ $usuario == "$VICERRECTOR" ]]; then
                [[ $(jq -c '.acciones' <<<"$cuerpo") == '["INICIAR_REFORMA"]' ]] \
                    || { echo "       $usuario: $(jq -c '.acciones' <<<"$cuerpo")" >&2; return 1; }
            else
                [[ $(jq -c '.acciones' <<<"$cuerpo") == '[]' ]] || { echo "       $usuario tiene acciones" >&2; return 1; }
            fi
        else
            es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || { echo "       $usuario: $estado" >&2; return 1; }
        fi
    done
    IFS=$'\t' read -r carrera asignatura paralelo <"$carpeta/paralelos.tsv" || true
    como_coordinador_de "$carrera" || return 1
    pedir GET "$GATEWAY/api/generacion/periodos/$PERIODO/oferta/$asignatura/$paralelo/sugerencia" "Bearer $token"
    docente=$(jq -r '.candidatos[0].identificacion // empty' <<<"$cuerpo")
    [[ -n $docente ]] || { echo "       sin un docente para proponer" >&2; return 1; }
    proponer "$asignatura" "$paralelo" "$docente" "$TEXTO"
    es_codigo 409 GENERACION_VERSION_CONGELADA || { echo "       proponer: $estado" >&2; return 1; }
    iniciar "$RESPONSABLE"
    consolidar
    es_codigo 409 GENERACION_VERSION_CONGELADA || { echo "       consolidar: $estado" >&2; return 1; }
    pedir POST "$GATEWAY/api/validacionocs/periodos/$PERIODO/revision" "Bearer $token"
    es_codigo 409 VALIDACIONOCS_ETAPA_NO_ADMITE_REVISION || { echo "       revisión: $estado" >&2; return 1; }
    iniciar "$VICERRECTOR"
    pedir POST "$APROBACION" "Bearer $token"
    es_codigo 409 VALIDACIONOCS_ETAPA_NO_ADMITE_APROBACION || { echo "       aprobar: $estado" >&2; return 1; }
    iniciar "$SECRETARIA"
    registrar "$NUMERO" "$(hoy)" "$carpeta/resolucion.pdf"
    es_codigo 409 VALIDACIONOCS_ETAPA_NO_ADMITE_RESOLUCION || { echo "       resolución: $estado" >&2; return 1; }
    [[ $(transiciones) == "$antes" && $(resoluciones_en_la_base) == 1 ]]
}

# --- F18-H02: la reforma (D-113) ---

reformar() {  # reformar <motivo>, con el token de $token
    pedir POST "$REFORMA_URL" "Bearer $token" "$(jq -cn --arg m "$1" '{motivo: $m}')"
}

reformas() {
    consultar_base "select count(*) from validacionocs.transicion where periodo = '$PERIODO'
        and tipo = 'REFORMA'"
}

version_es() {  # version_es <número> <estado> [motivo]: en Generación
    [[ $(consultar_base "select v.estado || '|' || coalesce(v.motivo, '') from generacion.version_distributivo v
        join generacion.distributivo d on d.id = v.distributivo_id
        where d.periodo = '$PERIODO' and v.numero = $1") == "$2|${3:-}" ]] \
        || { echo "       versión $1: no es $2 ${3:-}" >&2; return 1; }
}

caida_reforma=''
vuelta_reforma=''

# --- 22. La reforma: 403 a otros roles, 400 sin motivo; vicerrectorado.demo la inicia con identidad detenido ---
reforma_iniciada() {
    local usuario estado_de_la_reforma cuerpo_de_la_reforma
    for usuario in "$SECRETARIA" rectorado.demo; do
        iniciar "$usuario"
        reformar "$MOTIVO"
        es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || { echo "       $usuario reforma ($estado)" >&2; return 1; }
    done
    iniciar "$VICERRECTOR"
    for corto in '' '   corta   '; do
        reformar "$corto"
        es_codigo 400 VALIDACIONOCS_MOTIVO_DE_REFORMA_INVALIDO || { echo "       motivo corto: $estado" >&2; return 1; }
    done
    [[ $(reformas) == 0 ]] || return 1
    # SC-004: con el token ya pedido, identidad detenido; la notificación llega cuando vuelve.
    caida_reforma=$(consultar_base "select now()")
    docker compose stop identidad >/dev/null 2>&1
    reformar "$MOTIVO"
    estado_de_la_reforma=$estado
    cuerpo_de_la_reforma=$cuerpo
    vuelta_reforma=$(consultar_base "select now()")
    docker compose up -d --wait --no-recreate --no-build identidad >/dev/null 2>&1 || return 1
    estado=$estado_de_la_reforma
    cuerpo=$cuerpo_de_la_reforma
    [[ $estado == 200 && $(jq -c '[.etapa, .version, .reforma.versionPublicada, .reforma.version, .publicacion.version]' \
        <<<"$cuerpo") == '["EN_CONSOLIDACION",2,1,2,1]' ]] || { echo "       reformar: $estado" >&2; return 1; }
    [[ $(reformas) == 1 ]] || return 1
    version_es 1 CONGELADA || return 1
    version_es 2 EDITABLE REFORMA || return 1
    esperar "$ESPERA_REENVIO" hay_entradas 1 "servicio = 'validacionocs' and accion = 'REFORMA_INICIADA'
        and periodo = '$PERIODO' and usuario = '$VICERRECTOR'" || { echo "       sin la bitácora" >&2; return 1; }
    reformar "$MOTIVO"
    es_codigo 409 VALIDACIONOCS_ETAPA_NO_ADMITE_REFORMA || { echo "       la segunda: $estado" >&2; return 1; }
    [[ $(reformas) == 1 ]]
}

# --- 23. La publicada sigue vigente y la reforma en curso, para quien lee el flujo ---
publicada_vigente() {
    local usuario
    for usuario in "$SECRETARIA" "$RESPONSABLE" "$COORDINADOR"; do
        iniciar "$usuario"
        pedir GET "$GATEWAY/api/validacionocs/periodos/$PERIODO/flujo" "Bearer $token"
        [[ $estado == 200 && $(jq -c '[.publicacion.version, .reforma.versionPublicada, .reforma.version,
            .reforma.motivo, .reforma.iniciadaPor]' <<<"$cuerpo") \
            == "$(jq -cn --arg m "$MOTIVO" --arg v "$VICERRECTOR" '[1, 1, 2, $m, $v]')" ]] \
            || { echo "       $usuario: $estado" >&2; return 1; }
    done
}

notificaciones_de_la_reforma() {  # notificaciones_de_la_reforma <usuario>
    consultar_base "select count(*) from notificaciones.notificacion where destinatario = '$1'
        and periodo = '$PERIODO' and titulo = '$REFORMADA' and recibida_en >= '$caida_reforma'"
}

llego_la_reforma() {
    [[ $(notificaciones_de_la_reforma "$DESIGNADO") == 1 ]]
}

# --- 24. La notificación: una a cada uno, la de docente.demo antes de 120 s de la vuelta; ninguna al Vicerrector ---
reforma_notificada() {
    [[ -n $caida_reforma && -n $vuelta_reforma ]] || { echo "       no se reformó con identidad detenido" >&2; return 1; }
    esperar "$ESPERA_REENVIO" llego_la_reforma || { echo "       no llegó a $DESIGNADO" >&2; return 1; }
    local usuario llegada
    for usuario in "$RESPONSABLE" "$DESIGNADO" datos.demo "$COORDINADOR" docente.coordinador.demo; do
        [[ $(notificaciones_de_la_reforma "$usuario") == 1 ]] \
            || { echo "       $usuario: $(notificaciones_de_la_reforma "$usuario")" >&2; return 1; }
    done
    [[ $(notificaciones_de_la_reforma "$VICERRECTOR") == 0 ]] || { echo "       el Vicerrector la recibió" >&2; return 1; }
    llegada=$(consultar_base "select extract(epoch from recibida_en - '$vuelta_reforma'::timestamptz)::int
        from notificaciones.notificacion where destinatario = '$DESIGNADO' and periodo = '$PERIODO'
        and titulo = '$REFORMADA' and recibida_en >= '$caida_reforma'")
    echo "       llegó $llegada s después de levantar identidad"
    [[ $llegada =~ ^-?[0-9]+$ ]] && ((llegada >= 0 && llegada <= ESPERA_REENVIO))
}

documento_de_la_version() {  # documento_de_la_version <número>, con el token de $token
    pedir GET "$DOCUMENTO" "Bearer $token"
    [[ $estado == 200 && $(jq -r '.estado' <<<"$cuerpo") == GENERADO && $(jq -r '.version' <<<"$cuerpo") == "$1" ]]
}

# --- 25. El ciclo de la versión 2: devolver, reenviar, consolidar, revisión y aprobación ---
ciclo_de_la_version_2() {
    local carrera=${orden[0]} una
    iniciar "$RESPONSABLE"
    pedir POST "$GATEWAY/api/validacionocs/periodos/$PERIODO/devolucion" "Bearer $token" \
        "$(jq -cn --arg c "$carrera" --arg o "$MOTIVO" '{devoluciones: [{carrera: $c, observacion: $o}]}')"
    [[ $estado == 200 && $(jq -r '.etapa' <<<"$cuerpo") == BORRADOR_CARRERA ]] \
        || { echo "       devolver: $estado" >&2; return 1; }
    como_coordinador_de "$carrera" || return 1
    enviar_parcial "$carrera"
    [[ $estado == 200 && $(jq -r '.etapa' <<<"$cuerpo") == EN_CONSOLIDACION ]] \
        || { echo "       reenviar: $estado" >&2; return 1; }
    iniciar "$RESPONSABLE"
    consolidar
    [[ $estado == 200 ]] || { echo "       consolidar: $estado" >&2; return 1; }
    printf '%%PDF-1.7\n%% el informe de la reforma\n%%%%EOF\n' >"$carpeta/informe-2.pdf"
    adjuntar "$carpeta/informe-2.pdf"
    [[ $estado == 200 ]] || { echo "       adjuntar: $estado" >&2; return 1; }
    pedir GET "$PANEL" "Bearer $token"
    while read -r una; do
        justificar "$(jq -r '.[0]' <<<"$una")" "$(jq -r '.[1]' <<<"$una")" "$TEXTO"
        [[ $estado == 200 ]] || { echo "       justificar: $estado" >&2; return 1; }
    done < <(jq -c '.inconsistencias[] | select(.estado == "ABIERTA" and .severidad == "BLANDA") | [.tipo, .objeto.clave]' <<<"$cuerpo")
    pedir POST "$GATEWAY/api/validacionocs/periodos/$PERIODO/revision" "Bearer $token"
    [[ $estado == 200 && $(jq -r '.etapa' <<<"$cuerpo") == EN_REVISION_VRA ]] \
        || { echo "       revisión: $estado ${cuerpo:0:160}" >&2; return 1; }
    iniciar "$VICERRECTOR"
    pedir POST "$APROBACION" "Bearer $token"
    [[ $estado == 200 && $(jq -c '[.etapa, .congelacion.version]' <<<"$cuerpo") == '["CONGELADO_EN_FIRMA",2]' ]] \
        || { echo "       aprobar: $estado" >&2; return 1; }
    iniciar "$RESPONSABLE"
    esperar 30 documento_de_la_version 2 || { echo "       sin el documento de la versión 2" >&2; return 1; }
    cadena_es "EN_TURNO,PENDIENTE,PENDIENTE"
}

# --- 26. Las tres firmas de la versión 2, su resolución y su publicación ---
firmas_y_publicacion_de_la_version_2() {
    local i usuarios=("$DESIGNADO" "$VICERRECTOR" rectorado.demo)
    local campos=(firma-1-RESPONSABLE_CONSOLIDACION firma-2-VICERRECTOR firma-3-RECTOR)
    local rotulos=("Elaborado por" "Revisado por" "Aprobado por")
    for i in 0 1 2; do
        turno_de "${usuarios[$i]}" $((i + 1)) "${rotulos[$i]}" \
            || { echo "       no es el turno de ${usuarios[$i]}" >&2; return 1; }
        vigente_de "${usuarios[$i]}" || return 1
        firmar "$carpeta/descargado.pdf" "$carpeta/v2-firmado-$i.pdf" $((i + 1)) "${campos[$i]}" || return 1
        iniciar "${usuarios[$i]}"
        subir "$carpeta/v2-firmado-$i.pdf"
        aceptada || { echo "       ${usuarios[$i]}: $estado" >&2; return 1; }
    done
    esperar 30 en_ocs || { echo "       la versión 2 no llegó a EN_OCS" >&2; return 1; }
    iniciar "$SECRETARIA"
    registrar 'RPC-SO-12-No.124-2026' "$(hoy)" "$carpeta/resolucion.pdf"
    [[ $estado == 200 && $(jq -r '.etapa' <<<"$cuerpo") == APROBADO ]] \
        || { echo "       resolución: $estado" >&2; return 1; }
    pedir POST "$PUBLICACION" "Bearer $token"
    [[ $estado == 200 && $(jq -c '[.etapa, .publicacion.version, .reforma]' <<<"$cuerpo") \
        == '["PUBLICADO",2,null]' ]] || { echo "       publicar: $estado" >&2; return 1; }
    [[ $(publicaciones) == 2 ]]
}

# --- 27. La versión 1, histórica: CONGELADA, con sus tres firmas, su documento y su resolución ---
version_1_historica() {
    local v
    version_es 1 CONGELADA || return 1
    version_es 2 CONGELADA REFORMA || return 1
    for v in 1 2; do
        [[ $(consultar_base "select string_agg(orden::text, ',' order by orden) from firma.validacion_firma
            where periodo = '$PERIODO' and version = $v and aceptada") == 1,2,3 ]] \
            || { echo "       las firmas de la versión $v" >&2; return 1; }
        [[ $(consultar_base "select count(*) from firma.documento_firmable where periodo = '$PERIODO'
            and version = $v") == 1 ]] || { echo "       el documento de la versión $v" >&2; return 1; }
        [[ $(consultar_base "select count(*) from validacionocs.resolucion_ocs where periodo = '$PERIODO'
            and version = $v") == 1 ]] || { echo "       la resolución de la versión $v" >&2; return 1; }
    done
    [[ $(consultar_base "select count(*) from firma.anulacion_de_firmas where periodo = '$PERIODO'") == 0 ]] \
        || { echo "       hay firmas anuladas" >&2; return 1; }
}

# --- F16-H01: las notificaciones de cada transición (D-108) ---
readonly REVISION='El distributivo llegó a «Revisión del Vicerrectorado»'

# Lo que hay en las bandejas desde que empezó el script: «destinatario|título», ordenado.
bandejas() {
    consultar_base "select destinatario || '|' || titulo from notificaciones.notificacion
        where periodo = '$PERIODO' and recibida_en >= '$inicio'" | LC_ALL=C sort
}

# Lo que debe haber: cada transición, a los usuarios del rol de su etapa de llegada, sin el autor
# (la publicación, a sus tres roles).
# El Responsable de consolidación es quien tiene el rol ($RESPONSABLE) y, desde la designación,
# $DESIGNADO; el paso a APROBADO no llega a nadie, porque $SECRETARIA es la única con su rol.
esperadas() {
    local una_carrera
    {
        # Cada carrera por su nombre en Académico.
        for una_carrera in "${orden[@]}"; do
            echo "$RESPONSABLE|La carrera $(consultar_base "select nombre from academico.carrera
                where codigo = '$una_carrera'") envió su parcial"
        done
        echo "$RESPONSABLE|El distributivo llegó a «Consolidación»"
        echo "$VICERRECTOR|$REVISION"
        echo "$RESPONSABLE|El distributivo llegó a «En firma»"
        echo "$DESIGNADO|El distributivo llegó a «En firma»"
        # F16-H02: cada firma aceptada notifica al siguiente firmante; F1 no, tras F3 nadie.
        echo "$VICERRECTOR|Tienes una firma pendiente: «Revisado por»"
        echo "rectorado.demo|Tienes una firma pendiente: «Aprobado por»"
        echo "$SECRETARIA|El distributivo llegó a «Resolución del OCS»"
        # F18-H01: la publicación, a cada usuario activo con DOCENTE, OPERADOR_DATOS o
        # COORDINADOR_CARRERA, una sola vez aunque tenga dos, y no al autor. Además de los de la
        # demo (paso 18), otros scripts dejan usuarios de prueba con esos roles.
        consultar_base "select distinct u.nombre_usuario || '|$PUBLICADA' from usuarios.usuario u
            join roles.asignacion_rol a on a.nombre_usuario = u.nombre_usuario
            join roles.rol r on r.id = a.rol_id
            where u.activo and r.identificador in ('DOCENTE', 'OPERADOR_DATOS', 'COORDINADOR_CARRERA')
            and u.nombre_usuario <> '$SECRETARIA'"
    } | LC_ALL=C sort
}

bandejas_completas() {
    [[ $(bandejas) == "$(esperadas)" ]]
}

# --- 18. Las bandejas de todos: exactamente las de cada transición; la lectura de vicerrectorado.demo ---
# La publicación se hizo con identidad detenido: su notificación puede tardar hasta $ESPERA_REENVIO s.
notificaciones_y_lectura() {
    esperar "$ESPERA_REENVIO" bandejas_completas || {
        diff <(esperadas) <(bandejas) | sed 's/^/       /' >&2
        return 1
    }
    # F18-H01: la publicación llegó a los cuatro de la demo con esos roles, una a cada uno.
    local usuario
    for usuario in datos.demo "$COORDINADOR" "$DESIGNADO" docente.coordinador.demo; do
        [[ $(bandejas | grep -cFx "$usuario|$PUBLICADA") == 1 ]] \
            || { echo "       a $usuario no le llegó la publicación" >&2; return 1; }
    done
    [[ $(consultar_base "select autor_nombre from notificaciones.notificacion where periodo = '$PERIODO'
        and recibida_en >= '$inicio' and titulo = 'El distributivo llegó a «Resolución del OCS»'") == 'el sistema' ]] \
        || { echo "       EN_OCS no dice «el sistema»" >&2; return 1; }
    local antes id ajena primera
    iniciar "$VICERRECTOR"
    pedir GET "$GATEWAY/api/notificaciones/bandeja/sin-leer" "Bearer $token"
    antes=$(jq -r '.sinLeer' <<<"$cuerpo")
    [[ $estado == 200 && $antes -ge 1 ]] || return 1
    pedir GET "$GATEWAY/api/notificaciones/bandeja" "Bearer $token"
    id=$(jq -r --arg t "$REVISION" '[.notificaciones[] | select(.titulo == $t and .periodo == "'"$PERIODO"'")] | first | .id' \
        <<<"$cuerpo")
    [[ $id =~ ^[0-9a-f-]{36}$ ]] || { echo "       no está en la bandeja" >&2; return 1; }
    pedir POST "$GATEWAY/api/notificaciones/bandeja/$id/lectura" "Bearer $token"
    [[ $estado == 200 && $(jq -r '.leida' <<<"$cuerpo") == true ]] || return 1
    primera=$(jq -r '.leidaEn' <<<"$cuerpo")
    pedir POST "$GATEWAY/api/notificaciones/bandeja/$id/lectura" "Bearer $token"
    [[ $estado == 200 && $(jq -r '.leidaEn' <<<"$cuerpo") == "$primera" ]] || return 1
    pedir GET "$GATEWAY/api/notificaciones/bandeja/sin-leer" "Bearer $token"
    [[ $(jq -r '.sinLeer' <<<"$cuerpo") == $((antes - 1)) ]] || return 1
    ajena=$(consultar_base "select id from notificaciones.notificacion where destinatario = '$SECRETARIA'
        and periodo = '$PERIODO' and recibida_en >= '$inicio' limit 1")
    pedir POST "$GATEWAY/api/notificaciones/bandeja/$ajena/lectura" "Bearer $token"
    es_codigo 404 NOTIFICACIONES_NOTIFICACION_NO_ENCONTRADA
}

# --- 19. La publicación con identidad detenido: 200 sin error y la notificación al volver, una vez ---
llego_al_volver() {
    [[ -n $caida && -n $vuelta ]] || { echo "       no se publicó con identidad detenido" >&2; return 1; }
    local llegada filas
    filas=$(consultar_base "select count(*) from notificaciones.notificacion where destinatario = '$DESIGNADO'
        and periodo = '$PERIODO' and titulo = '$PUBLICADA' and recibida_en >= '$caida'")
    [[ $filas == 1 ]] || { echo "       $filas notificaciones de la publicación" >&2; return 1; }
    llegada=$(consultar_base "select extract(epoch from recibida_en - '$vuelta'::timestamptz)::int
        from notificaciones.notificacion where destinatario = '$DESIGNADO' and periodo = '$PERIODO'
        and titulo = '$PUBLICADA' and recibida_en >= '$caida'")
    echo "       llegó $llegada s después de levantar identidad"
    [[ $llegada =~ ^-?[0-9]+$ ]] && ((llegada >= 0 && llegada <= ESPERA_REENVIO))
}

# --- 21. F1 con identidad detenido: la notificación de F2 llegó al volver, una vez (F16-H02) ---
FIRMA_PENDIENTE='Tienes una firma pendiente: «Revisado por»'
llego_la_de_f2() {
    [[ -n $caida_firma && -n $vuelta_firma ]] || { echo "       F1 no se subió con identidad detenido" >&2; return 1; }
    local filas llegada detalle autor
    filas=$(consultar_base "select count(*) from notificaciones.notificacion where destinatario = '$VICERRECTOR'
        and periodo = '$PERIODO' and titulo = '$FIRMA_PENDIENTE' and recibida_en >= '$caida_firma'")
    [[ $filas == 1 ]] || { echo "       $filas notificaciones de F2" >&2; return 1; }
    llegada=$(consultar_base "select extract(epoch from recibida_en - '$vuelta_firma'::timestamptz)::int
        from notificaciones.notificacion where destinatario = '$VICERRECTOR' and periodo = '$PERIODO'
        and titulo = '$FIRMA_PENDIENTE' and recibida_en >= '$caida_firma'")
    echo "       llegó $llegada s después de levantar identidad"
    [[ $llegada =~ ^-?[0-9]+$ ]] && ((llegada >= 0 && llegada <= ESPERA_REENVIO)) || return 1
    detalle=$(consultar_base "select detalle || '|' || autor from notificaciones.notificacion
        where destinatario = '$VICERRECTOR' and periodo = '$PERIODO' and titulo = '$FIRMA_PENDIENTE'
        and recibida_en >= '$caida_firma'")
    autor=${detalle##*|}
    detalle=${detalle%|*}
    [[ $autor == "$DESIGNADO" ]] || { echo "       autor: $autor" >&2; return 1; }
    [[ $detalle =~ ^Periodo\ $PERIODO,\ versión\ 1\.\ «Elaborado\ por»\ la\ firmó\ .+\.$ ]] \
        || { echo "       detalle: $detalle" >&2; return 1; }
}

# --- 20. Una entrega repetida: la publicación vuelve a FAILED y el reenvío no duplica nada ---
completada() {  # completada <idEvento>
    [[ $(consultar_base "select count(*) from eventos_distributivo.event_publication
        where serialized_event like '%$1%' and status = 'COMPLETED'") == 1 ]]
}

entrega_repetida() {
    local id_evento antes
    id_evento=$(consultar_base "select serialized_event::jsonb ->> 'idEvento' from eventos_distributivo.event_publication
        where publication_date >= '$inicio' and completion_date is not null
        and serialized_event::jsonb -> 'contenido' -> 'mensajes' -> 0 ->> 'titulo' = '$REVISION'
        and serialized_event::jsonb -> 'contenido' ->> 'periodo' = '$PERIODO' limit 1")
    [[ $id_evento =~ ^[0-9a-f-]{36}$ ]] || { echo "       sin la publicación del envío" >&2; return 1; }
    antes=$(consultar_base "select count(*) from notificaciones.notificacion")
    consultar_base "update eventos_distributivo.event_publication set status = 'FAILED', completion_date = null
        where serialized_event like '%$id_evento%'" >/dev/null
    esperar "$ESPERA_REENVIO" completada "$id_evento" || return 1
    [[ $(consultar_base "select count(*) from notificaciones.notificacion") == "$antes"
        && $(consultar_base "select count(*) from notificaciones.notificacion where id_evento = '$id_evento'
            and leida_en is not null") == 1 ]]
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
    # F18-H01: la caída de identidad de F16-H01 pasó a la publicación (paso 16).
    pedir POST "$GATEWAY/api/validacionocs/periodos/$PERIODO/revision" "Bearer $token"
    [[ $estado == 200 && $(jq -r '.etapa' <<<"$cuerpo") == EN_REVISION_VRA ]] || return 1
    hash_del_envio=$(jq -r '[.transiciones[] | select(.tipo == "PASO_DE_ETAPA" and .desde == "EN_CONSOLIDACION")] | last | .hash' <<<"$cuerpo")
    [[ $hash_del_envio =~ ^[0-9a-f]{64}$ ]]
}

# Lo del envío, de la aprobación, del documento y de la publicación, para los pasos siguientes.
caida=''
vuelta=''
caida_firma=''
vuelta_firma=''
hash_del_envio=''
huella_enviada=''
hash_congelado=''
aprobado=''
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
    aprobado=$(consultar_base "select now()")
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
    [[ $sha =~ ^[0-9a-f]{64}$ ]] || return 1
    # F16-H02: la cadena recién generada; coordinacion.demo, que no lee el documento, 403.
    cadena_es "EN_TURNO,PENDIENTE,PENDIENTE" || return 1
    iniciar "$COORDINADOR"
    pedir GET "$DOCUMENTO" "Bearer $token"
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO
}

# --- 3. El archivo del volumen se llama por su SHA-256, y es ese ---
archivo_por_su_hash() {
    [[ $(consultar_base "select archivo from firma.documento_firmable where periodo = '$PERIODO'") \
        == "documentos/$sha.pdf" ]] || return 1
    docker compose cp "distributivo:/var/mdd/archivos/documentos/$sha.pdf" "$carpeta/documento.pdf" \
        >/dev/null 2>&1 || return 1
    [[ $(sha256sum "$carpeta/documento.pdf" | cut -d' ' -f1) == "$sha" ]]
}

# --- 4. veraPDF lo da conforme con PDF/A-2b, con los tres campos de firma vacíos ---
conforme() {
    local campos pdf="$carpeta/verapdf"
    # Solo el PDF, legible para el usuario de la imagen; el resto de $carpeta tiene tokens.
    mkdir -p "$pdf" && cp "$carpeta/documento.pdf" "$pdf/" && chmod 755 "$pdf" && chmod 644 "$pdf/documento.pdf"
    docker run --rm -v "$pdf":/datos:ro,z "$VERAPDF" --flavour 2b \
        -x interactiveFormField,signature /datos/documento.pdf >"$carpeta/verapdf.xml" 2>&1 || true
    if ! grep -q 'isCompliant="true"' "$carpeta/verapdf.xml"; then
        echo "       veraPDF: no conforme" >&2
        { grep -o '<rule [^>]*status="failed"[^>]*>' "$carpeta/verapdf.xml" || head -5 "$carpeta/verapdf.xml"; } \
            | head -5 | sed 's/^/       /' >&2
        return 1
    fi
    campos=$(grep -o '<fullyQualifiedName>[^<]*' "$carpeta/verapdf.xml" | sed 's/.*>//' | paste -sd ' ')
    echo "       campos de firma: $campos"
    [[ $campos =~ ^firma-1-[A-Z_]+\ firma-2-[A-Z_]+\ firma-3-[A-Z_]+$ ]] || return 1
    ! grep -q '<signature[ >]' "$carpeta/verapdf.xml"
}

archivos_del_volumen() {
    docker compose exec -T distributivo sh -c 'ls /var/mdd/archivos/documentos | wc -l'
}

# --- 5. Pedirlo otra vez: el mismo, sin otro archivo ni fila; el Coordinador, 403 ---
idempotente() {
    local archivos
    archivos=$(archivos_del_volumen)
    iniciar "$RESPONSABLE"
    pedir POST "$DOCUMENTO" "Bearer $token"
    [[ $estado == 200 && $(jq -r '.estado' <<<"$cuerpo") == GENERADO ]] || return 1
    [[ $(jq -r '.documento.sha256' <<<"$cuerpo") == "$sha" ]] || return 1
    [[ $(jq -r '.documento.generadoPor' <<<"$cuerpo") == sistema ]] || return 1
    [[ $(consultar_base "select count(*) from firma.documento_firmable where periodo = '$PERIODO'") == 1 ]] || return 1
    [[ $(archivos_del_volumen) == "$archivos" ]] || return 1
    como_coordinador_de "${orden[0]}" || return 1
    pedir POST "$DOCUMENTO" "Bearer $token"
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO
}

# --- 6. La bitácora: exactamente una DOCUMENTO_FIRMABLE_GENERADO con la versión, el hash y el SHA ---
bitacora() {
    [[ -n $aprobado ]] || return 1
    local condicion="accion = 'DOCUMENTO_FIRMABLE_GENERADO' and recibida_en >= '$aprobado'"
    esperar 15 hay_entradas 1 "$condicion" || return 1
    [[ $(entradas "$condicion and servicio = 'firma' and usuario = 'sistema'
        and objeto ->> 'version' is not null and objeto ->> 'hashContenido' = '$hash_congelado'
        and objeto ->> 'sha256' = '$sha'") == 1 ]] || return 1
    sleep 3
    hay_entradas 1 "$condicion"
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

id_del_documento() {
    # F18-H02: con la reforma hay un documento por versión; el vigente es el de la más alta.
    consultar_base "select id from firma.documento_firmable where periodo = '$PERIODO'
        order by version desc limit 1"
}

# --- 7. El designado ve su firma pendiente y descarga el PDF vigente, con su SHA-256 ---
pendiente_del_designado() {
    local id
    id=$(id_del_documento)
    [[ -n $id ]] || return 1
    iniciar "$DESIGNADO"
    pedir GET "$PENDIENTES" "Bearer $token"
    [[ $estado == 200 ]] || return 1
    [[ $(jq -c --arg p "$PERIODO" '[.[] | select(.periodo == $p) | [.documento, .version, .orden, .rotulo]]' \
        <<<"$cuerpo") == "[[\"$id\",1,1,\"Elaborado por\"]]" ]] || return 1
    descargar "$GATEWAY/api/firma/documentos/$id/archivo"
    [[ $estado == 200 ]] || return 1
    [[ $(cabecera Content-Type) == application/pdf* ]] || return 1
    [[ $(cabecera Content-Disposition) == *"filename=\"distributivo-$PERIODO-v1.pdf\""* ]] || return 1
    [[ $(cabecera ETag) == "\"$sha\"" ]] || return 1
    [[ $(sha256sum "$carpeta/descargado.pdf" | cut -d' ' -f1) == "$sha" ]]
}

# --- 8. Quien no está en turno no la ve y recibe 403; sin el permiso, 403 en las dos ---
nadie_mas() {
    local id usuario
    id=$(id_del_documento)
    [[ -n $id ]] || return 1
    for usuario in "$RESPONSABLE" "$VICERRECTOR" rectorado.demo; do
        iniciar "$usuario"
        pedir GET "$PENDIENTES" "Bearer $token"
        [[ $estado == 200 && $(jq -c --arg p "$PERIODO" '[.[] | select(.periodo == $p)]' <<<"$cuerpo") == '[]' ]] \
            || { echo "       $usuario ve la firma pendiente" >&2; return 1; }
        pedir GET "$GATEWAY/api/firma/documentos/$id/archivo" "Bearer $token"
        es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO \
            || { echo "       $usuario descarga ($estado)" >&2; return 1; }
    done
    iniciar "$COORDINADOR"
    pedir GET "$PENDIENTES" "Bearer $token"
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || return 1
    pedir GET "$GATEWAY/api/firma/documentos/$id/archivo" "Bearer $token"
    es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO
}

if [[ -n $CONSERVAR && $CONSERVAR != EN_OCS && $CONSERVAR != F2 && $CONSERVAR != APROBADO \
    && $CONSERVAR != PUBLICADO ]]; then
    verificar 1 "El consolidado listo, enviado a revisión y con $DESIGNADO designado: EN_REVISION_VRA" en_revision_y_designado
    if ((fallas > 0)); then
        echo "No quedó en revisión. Para limpiar: docker compose -p tesis-prototipo-2026 down -v"
        exit 1
    fi
    echo "Quedó en EN_REVISION_VRA, sin limpiar. Recorre specs/049-completar-cadena/quickstart.md §3."
    echo "Para limpiar al terminar: docker compose -p tesis-prototipo-2026 down -v"
    exit 0
fi

# --- R. Los reportes de la versión publicada (F20-H01, D-115; specs/058-reporte-individual-docente/contracts/demostracion.md;
# F20-H02, D-117: el reporte general, specs/059-reporte-general-consolidado/contracts/demostracion.md) ---
readonly REPORTES="$GATEWAY/api/reporting"
readonly RECTOR=rectorado.demo
readonly DESCARGADO="servicio = 'reporting' and accion = 'REPORTE_INDIVIDUAL_DESCARGADO'"
readonly DESCARGADO_GENERAL="servicio = 'reporting' and accion = 'REPORTE_GENERAL_DESCARGADO'"
# F19-H01 (D-119): el CSV para SIGA y el layout de exportación.
readonly CSV_SIGA="$GATEWAY/api/reporting/periodos/$PERIODO/csv-siga"
readonly LAYOUT_SIGA="$GATEWAY/api/reporting/layout-siga"
readonly PLANTILLA_SIGA=backend/servicios/reporting/src/main/resources/reporting/layout-siga-por-defecto.yaml
readonly GENERADO_SIGA="servicio = 'reporting' and accion = 'CSV_SIGA_GENERADO'"
readonly CARGADO_SIGA="servicio = 'reporting' and accion = 'LAYOUT_SIGA_CARGADO'"
# F19-H02 (D-120): las exportaciones a SIGA y el rechazo de una versión no exportable.
readonly EXPORTACIONES_SIGA="$GATEWAY/api/reporting/periodos/$PERIODO/exportaciones-siga"
readonly RECHAZADA_SIGA="servicio = 'reporting' and accion = 'EXPORTACION_SIGA_RECHAZADA'"
readonly ADMINISTRADOR=admin.demo

hash_publicado() {
    consultar_base "select hash from validacionocs.transicion where periodo = '$PERIODO'
        and hacia = 'PUBLICADO' order by momento desc limit 1"
}

ruta_del_reporte() {  # ruta_del_reporte <identificación>
    printf '%s/periodos/%s/docentes/%s/reporte' "$REPORTES" "$PERIODO" "$1"
}

ruta_del_general() {  # ruta_del_general [periodo]
    printf '%s/periodos/%s/reporte-general' "$REPORTES" "${1:-$PERIODO}"
}

# bajar <ruta del reporte> <formato> <archivo>: la descarga, con el token de $token y sus cabeceras.
bajar() {
    estado=$(curl -s -D "$carpeta/cabeceras" -o "$3" -w '%{http_code}' \
        -H "Authorization: Bearer $token" "$1.$2" || echo '000')
}

# El primer docente de la versión publicada con asignaturas, y uno sin actividades si lo hay.
elegir_docentes() {
    local una
    pedir GET "$REPORTES/periodos/$PERIODO/docentes" "Bearer $token"
    [[ $estado == 200 ]] || return 1
    docente_con_clase=''
    sin_actividades=''
    for una in $(jq -r '.docentes[].identificacion' <<<"$cuerpo"); do
        pedir GET "$(ruta_del_reporte "$una")" "Bearer $token"
        [[ $estado == 200 ]] || return 1
        if [[ -z $docente_con_clase && $(jq '.asignaturas | length' <<<"$cuerpo") -gt 0 ]]; then
            docente_con_clase=$una
            printf '%s' "$cuerpo" >"$carpeta/reporte.json"
        fi
        [[ -z $sin_actividades && $(jq '.actividades | length' <<<"$cuerpo") == 0 ]] && sin_actividades=$una
        [[ -n $docente_con_clase && -n $sin_actividades ]] && break
    done
    [[ -n $docente_con_clase ]]
}

# El CSV tiene las mismas filas y los mismos totales que el JSON: lo compara Python, sin red.
csv_igual_al_json() {  # csv_igual_al_json <individual|general> <csv> <json>
    python3 -I - "$1" "$2" "$3" <<'PY'
import csv, json, sys
from decimal import Decimal
tipo = sys.argv[1]
crudo = open(sys.argv[2], 'rb').read()
assert crudo.startswith(b'\xef\xbb\xbf'), 'sin BOM'
assert b'\r\n' in crudo, 'sin CRLF'
filas = list(csv.reader(crudo.decode('utf-8-sig').splitlines(), delimiter=';'))
reporte = json.load(open(sys.argv[3]))
encabezado, filas = filas[0], filas[1:]
horas = lambda texto: Decimal(texto.replace(',', '.'))
componentes = ['clase', 'otrasDocencia', 'investigacion', 'vinculacion', 'gestion', 'total']
assert all(f[1] == str(reporte['publicacion']['version']) and f[2] == reporte['publicacion']['hash']
           for f in filas), 'versión o hash'
if tipo == 'general':
    # F20-H02: una fila DOCENTE por docente, en el orden del JSON, y una fila TOTAL al final.
    assert encabezado[4] == 'Tipo' and encabezado[-1] == 'Total', encabezado
    docentes = [f for f in filas if f[4] == 'DOCENTE']
    assert [(f[5], [horas(h) for h in f[8:14]]) for f in docentes] == \
        [(d['identificacion'], [Decimal(str(d['horas'][c])) for c in componentes])
         for d in reporte['docentes']], 'docentes'
    assert [f[4] for f in filas] == ['DOCENTE'] * len(docentes) + ['TOTAL'], 'la fila TOTAL al final'
    assert [horas(h) for h in filas[-1][8:14]] == \
        [Decimal(str(reporte['totales'][c])) for c in componentes], 'totales'
    sys.exit(0)
assert encabezado[0] == 'Periodo' and encabezado[-1] == 'Horas', encabezado
asignaturas = [f for f in filas if f[7] == 'ASIGNATURA']
actividades = [f for f in filas if f[7] == 'ACTIVIDAD']
totales = {f[12]: horas(f[13]) for f in filas if f[7] == 'TOTAL'}
assert [(f[9], f[11], horas(f[13])) for f in asignaturas] == \
    [(a['codigo'], a['paralelo'], Decimal(str(a['horas']))) for a in reporte['asignaturas']], 'asignaturas'
assert [(f[9], horas(f[13])) for f in actividades] == \
    [(a['codigo'], Decimal(str(a['horas']))) for a in reporte['actividades']], 'actividades'
esperados = {'Clase': 'clase', 'Otras actividades de docencia': 'otrasDocencia',
             'Investigación': 'investigacion', 'Vinculación': 'vinculacion', 'Gestión': 'gestion',
             'Total': 'total'}
assert totales == {k: Decimal(str(reporte['horas'][v])) for k, v in esperados.items()}, 'totales'
PY
}

# El PDF es PDF/A-2b para veraPDF y no tiene campos de formulario.
pdf_sin_campos() {  # pdf_sin_campos <pdf>
    local pdf="$carpeta/verapdf-reporte"
    mkdir -p "$pdf" && cp "$1" "$pdf/reporte.pdf" && chmod 755 "$pdf" && chmod 644 "$pdf/reporte.pdf"
    docker run --rm -v "$pdf":/datos:ro,z "$VERAPDF" --flavour 2b -x interactiveFormField \
        /datos/reporte.pdf >"$carpeta/verapdf-reporte.xml" 2>&1 || true
    grep -q 'isCompliant="true"' "$carpeta/verapdf-reporte.xml" || { echo "       veraPDF: no conforme" >&2; return 1; }
    ! grep -q '<fullyQualifiedName>' "$carpeta/verapdf-reporte.xml"
}

# --- R1. Sin publicación: ninguna publicación del periodo y el reporte responde 409 ---
reportes_sin_publicacion() {
    iniciar "$RECTOR"
    pedir GET "$REPORTES/publicaciones" "Bearer $token"
    [[ $estado == 200 && $(jq --arg p "$PERIODO" '[.[] | select(.periodo == $p)] | length' <<<"$cuerpo") == 0 ]] \
        || return 1
    pedir GET "$REPORTES/periodos/$PERIODO/docentes" "Bearer $token"
    es_codigo 409 REPORTING_SIN_PUBLICACION || return 1
    pedir GET "$(ruta_del_reporte 0109990010)" "Bearer $token"
    es_codigo 409 REPORTING_SIN_PUBLICACION || return 1
    # F20-H02: tampoco hay reporte general.
    pedir GET "$(ruta_del_general)" "Bearer $token"
    es_codigo 409 REPORTING_SIN_PUBLICACION || return 1
    # F19-H01: tampoco hay CSV para SIGA.
    iniciar "$SECRETARIA"
    pedir GET "$CSV_SIGA" "Bearer $token"
    es_codigo 409 REPORTING_SIN_PUBLICACION || return 1
    # F19-H02: el 409 de «sin publicación» no es un rechazo por firmas ni una exportación.
    [[ $(exportaciones_de_la_corrida) == 0 ]] || return 1
    exportaciones_siga || return 1
    hay_entradas 0 "$DESCARGADO" && hay_entradas 0 "$DESCARGADO_GENERAL" && hay_entradas 0 "$GENERADO_SIGA" \
        && hay_entradas 0 "$RECHAZADA_SIGA"
}

# --- R2. El reporte de la versión 1 publicada, con el contenido guardado al congelar ---
reporte_de_la_version_1() {
    iniciar "$RECTOR"
    pedir GET "$REPORTES/publicaciones" "Bearer $token"
    [[ $estado == 200 && $(jq -c --arg p "$PERIODO" '[.[] | select(.periodo == $p) | .version, .hash, .enReforma]' <<<"$cuerpo") \
        == "$(jq -cn --arg h "$(hash_publicado)" '[1, $h, false]')" ]] || return 1
    elegir_docentes || return 1
    echo "       con clase: $docente_con_clase; sin actividades: ${sin_actividades:-ninguno}"
    [[ $(jq -c '[.publicacion.version, .publicacion.hash]' "$carpeta/reporte.json") \
        == "$(jq -cn --arg h "$(hash_publicado)" '[1, $h]')" ]] || return 1
    # La suma de las filas es el total, y el total es la carga horaria de la versión vigente.
    # En centésimas, para comparar 8 con 8.00 sin errores de redondeo.
    [[ $(jq 'def c: . * 100 | round;
        (([.asignaturas[].horas] | add) | c) == (.horas.clase | c)
        and ((.horas.clase + .horas.otrasDocencia + .horas.investigacion + .horas.vinculacion
            + .horas.gestion) | c) == (.horas.total | c)' "$carpeta/reporte.json") == true ]] || return 1
    iniciar "$VICERRECTOR"
    pedir GET "$GATEWAY/api/generacion/periodos/$PERIODO/docentes/$docente_con_clase/carga-horaria" "Bearer $token"
    [[ $estado == 200 && $(jq -n --argjson carga "$cuerpo" --slurpfile reporte "$carpeta/reporte.json" '
        def c: . * 100 | round;
        [$carga.totales | .clase, .otrasDocencia, .investigacion, .vinculacion, .gestion | c]
            == [$reporte[0].horas | .clase, .otrasDocencia, .investigacion, .vinculacion, .gestion | c]') \
        == true ]] || { echo "       carga: $cuerpo" >&2; return 1; }
    # Guardado al congelar (D-115), no tarde.
    [[ $(consultar_base "select tardio from generacion.contenido_congelado where periodo = '$PERIODO'
        and numero = 1") == f ]]
}

# --- R3. Los dos formatos: los mismos datos, el PDF sin campos y cada descarga en la bitácora ---
reporte_en_dos_formatos() {
    local usuario vuelta
    for usuario in "$RECTOR" "$SECRETARIA"; do
        iniciar "$usuario"
        for vuelta in 1 2; do
            bajar "$(ruta_del_reporte "$docente_con_clase")" pdf "$carpeta/reporte-$usuario-$vuelta.pdf"
            [[ $estado == 200 && $(cabecera Content-Type) == application/pdf \
                && $(cabecera Content-Disposition) == "attachment; filename=\"reporte-individual-$PERIODO-$docente_con_clase.pdf\"" \
                && $(cabecera Cache-Control) == no-store ]] || { echo "       PDF de $usuario: $estado" >&2; return 1; }
            bajar "$(ruta_del_reporte "$docente_con_clase")" csv "$carpeta/reporte-$usuario-$vuelta.csv"
            [[ $estado == 200 ]] || { echo "       CSV de $usuario: $estado" >&2; return 1; }
        done
    done
    pdf_sin_campos "$carpeta/reporte-$RECTOR-1.pdf" || return 1
    csv_igual_al_json individual "$carpeta/reporte-$RECTOR-1.csv" "$carpeta/reporte.json" || return 1
    # FR-010: los CSV de la misma versión son idénticos, aunque los pidan otras personas.
    cmp -s "$carpeta/reporte-$RECTOR-1.csv" "$carpeta/reporte-$SECRETARIA-2.csv" || return 1
    cp "$carpeta/reporte-$RECTOR-1.csv" "$carpeta/reporte-version-1.csv"
    # Ver el reporte (R2) no dejó entradas; las ocho descargas, sí, con su formato.
    esperar 60 hay_entradas 8 "$DESCARGADO" || { echo "       entradas: $(entradas "$DESCARGADO")" >&2; return 1; }
    hay_entradas 4 "$DESCARGADO and nuevo ->> 'formato' = 'CSV'
        and objeto ->> 'identificacion' = '$docente_con_clase' and (objeto ->> 'version')::int = 1"
}

# Los totales del general son la suma de sus filas, y el total general la suma de los cinco (SC-003).
# En centésimas, para comparar 8 con 8.00 sin errores de redondeo.
totales_son_la_suma() {  # totales_son_la_suma <json del general>
    [[ $(jq 'def c: . * 100 | round;
        . as $r
        | (["clase", "otrasDocencia", "investigacion", "vinculacion", "gestion", "total"]
            | map(. as $k | [$r.docentes[].horas[$k] | c] | add // 0)) as $filas
        | ([.totales | .clase, .otrasDocencia, .investigacion, .vinculacion, .gestion, .total | c]
            == $filas)
        and ((.totales | .clase + .otrasDocencia + .investigacion + .vinculacion + .gestion) | c)
            == (.totales.total | c)' "$1") == true ]] || { echo "       los totales no son la suma" >&2; return 1; }
}

# --- R7. El reporte general de la versión 1: todos los docentes, cada fila igual a su individual ---
reporte_general_de_la_version_1() {
    local una
    iniciar "$RECTOR"
    pedir GET "$(ruta_del_general)" "Bearer $token"
    [[ $estado == 200 ]] || { echo "       general: $estado" >&2; return 1; }
    printf '%s' "$cuerpo" >"$carpeta/general.json"
    [[ $(jq -c '[.publicacion.version, .publicacion.hash]' "$carpeta/general.json") \
        == "$(jq -cn --arg h "$(hash_publicado)" '[1, $h]')" ]] || return 1
    # SC-001: los mismos docentes del listado, sin repetidos. El orden del general es el alfabético
    # del español (D-117), que puede no ser el de la base, así que se comparan como conjuntos.
    pedir GET "$REPORTES/periodos/$PERIODO/docentes" "Bearer $token"
    [[ $estado == 200 ]] || return 1
    [[ $(jq -c '[.docentes[].identificacion] | sort' <<<"$cuerpo") \
        == "$(jq -c '[.docentes[].identificacion] | sort' "$carpeta/general.json")" ]] \
        || { echo "       los docentes no son los del listado" >&2; return 1; }
    [[ $(jq '[.docentes[].identificacion] | length == (unique | length)' "$carpeta/general.json") == true ]] \
        || return 1
    totales_son_la_suma "$carpeta/general.json" || return 1
    # SC-002: la fila de cada docente es igual a su reporte individual.
    for una in $(jq -r '.docentes[].identificacion' "$carpeta/general.json"); do
        pedir GET "$(ruta_del_reporte "$una")" "Bearer $token"
        [[ $estado == 200 && $(jq -n --argjson individual "$cuerpo" --slurpfile general "$carpeta/general.json" \
            --arg id "$una" 'def c: . * 100 | round;
            [$individual.horas | .clase, .otrasDocencia, .investigacion, .vinculacion, .gestion, .total | c]
                == [$general[0].docentes[] | select(.identificacion == $id) | .horas
                    | .clase, .otrasDocencia, .investigacion, .vinculacion, .gestion, .total | c]') \
            == true ]] || { echo "       $una: su fila no es su reporte individual" >&2; return 1; }
    done
    echo "       $(jq '.docentes | length' "$carpeta/general.json") docentes; total institucional $(jq '.totales.total' "$carpeta/general.json") h"
    # Ver el reporte no deja entrada.
    hay_entradas 0 "$DESCARGADO_GENERAL"
}

# --- R8. Los dos formatos del general: los mismos datos, el PDF sin campos y cada descarga en la bitácora ---
reporte_general_en_dos_formatos() {
    local usuario
    for usuario in "$RECTOR" "$SECRETARIA"; do
        iniciar "$usuario"
        bajar "$(ruta_del_general)" pdf "$carpeta/general-$usuario.pdf"
        [[ $estado == 200 && $(cabecera Content-Type) == application/pdf \
            && $(cabecera Content-Disposition) == "attachment; filename=\"reporte-general-$PERIODO.pdf\"" \
            && $(cabecera Cache-Control) == no-store ]] || { echo "       PDF del general de $usuario: $estado" >&2; return 1; }
        bajar "$(ruta_del_general)" csv "$carpeta/general-$usuario.csv"
        [[ $estado == 200 && $(cabecera Content-Disposition) == "attachment; filename=\"reporte-general-$PERIODO.csv\"" ]] \
            || { echo "       CSV del general de $usuario: $estado" >&2; return 1; }
    done
    pdf_sin_campos "$carpeta/general-$RECTOR.pdf" || return 1
    csv_igual_al_json general "$carpeta/general-$RECTOR.csv" "$carpeta/general.json" || return 1
    # FR-011: el CSV de la misma versión es idéntico, aunque lo pida otra persona.
    cmp -s "$carpeta/general-$RECTOR.csv" "$carpeta/general-$SECRETARIA.csv" || return 1
    cp "$carpeta/general-$RECTOR.csv" "$carpeta/general-version-1.csv"
    esperar 60 hay_entradas 4 "$DESCARGADO_GENERAL" \
        || { echo "       entradas: $(entradas "$DESCARGADO_GENERAL")" >&2; return 1; }
    hay_entradas 2 "$DESCARGADO_GENERAL and nuevo ->> 'formato' = 'CSV'
        and objeto ->> 'identificacion' is null and (objeto ->> 'version')::int = 1"
}

# --- F19-H01: el CSV para SIGA y el layout de exportación (D-119; specs/060-csv-siga/contracts/demostracion.md) ---

# generar_siga <archivo>: el CSV para SIGA de $PERIODO, con el token de $token y sus cabeceras.
generar_siga() {
    estado=$(curl -s -D "$carpeta/cabeceras" -o "$1" -w '%{http_code}' \
        -H "Authorization: Bearer $token" "$CSV_SIGA" || echo '000')
}

# cargar_layout <archivo>: lo sube como archivo de mapeo, con el token de $token.
cargar_layout() {
    enviar PUT "$LAYOUT_SIGA" -F "archivo=@$1;type=text/yaml"
}

# El layout vigente y cuántas versiones hay guardadas.
layout_vigente() {
    pedir GET "$LAYOUT_SIGA" "Bearer $token"
    [[ $estado == 200 ]] || return 1
    printf '%s' "$cuerpo" >"$carpeta/layouts.json"
}

versiones_del_layout() {
    consultar_base "select count(*) from reporting.layout_exportacion"
}

# F19-H02 (D-120): las exportaciones a SIGA del periodo, por la API, en $carpeta/exportaciones.json.
exportaciones_siga() {
    pedir GET "$EXPORTACIONES_SIGA" "Bearer $token"
    [[ $estado == 200 && $(jq -r '.periodo' <<<"$cuerpo") == "$PERIODO" ]] || return 1
    printf '%s' "$cuerpo" >"$carpeta/exportaciones.json"
}

# Las de esta corrida, en la base: la tabla no admite borrar (principio VII), así que se cuentan
# desde que empezó, como la bitácora.
exportaciones_de_la_corrida() {
    consultar_base "select count(*) from reporting.exportacion_siga
        where periodo = '$PERIODO' and exportada_en >= '$inicio'"
}

# csv_siga_igual_al_general <csv> <general.json> <individuales> <layout.json> <convenciones 0|1>:
# Python, con el módulo csv (RFC 4180): sin BOM, CRLF, el encabezado del layout, una fila por cada
# asignatura y actividad de los reportes individuales, y las sumas por docente y componente, y por
# componente, iguales al general. Con 1, además las convenciones de SENESCYT del layout por defecto.
csv_siga_igual_al_general() {
    python3 -I - "$@" <<'PY'
import csv, io, json, os, re, sys
from collections import defaultdict
from decimal import Decimal
ruta, ruta_general, individuales, ruta_layout, convenciones = sys.argv[1:6]
crudo = open(ruta, 'rb').read()
assert not crudo.startswith(b'\xef\xbb\xbf'), 'con BOM'
assert crudo.endswith(b'\r\n') and b'\n' not in crudo.replace(b'\r\n', b''), 'sin CRLF'
filas = list(csv.reader(io.StringIO(crudo.decode('utf-8'), newline='')))
layout = json.load(open(ruta_layout))
columnas = layout['columnas']
encabezado, filas = filas[0], filas[1:]
assert encabezado == [c['nombre'] for c in columnas], encabezado
def columna(campo):
    return next(i for i, c in enumerate(columnas) if c['campo'] == campo)
inversa = {}
for c in columnas:
    if c['campo'] == 'componente':
        inversa = {t['valor'].upper(): t['codigo'] for t in c.get('traduccion', [])}
de_json = {'CLASE': 'clase', 'OTRAS_DOCENCIA': 'otrasDocencia', 'INVESTIGACION': 'investigacion',
           'VINCULACION': 'vinculacion', 'GESTION': 'gestion'}
ident, comp, horas = columna('identificacion'), columna('componente'), columna('horas')
sumas = defaultdict(Decimal)
for f in filas:
    assert len(f) == len(columnas), f
    componente = inversa.get(f[comp], f[comp])
    sumas[(f[ident], de_json[componente])] += Decimal(f[horas])
general = json.load(open(ruta_general))
esperadas = 0
for d in general['docentes']:
    individual = json.load(open(os.path.join(individuales, d['identificacion'] + '.json')))
    esperadas += len(individual['asignaturas']) + len(individual['actividades'])
    for k in de_json.values():
        assert sumas[(d['identificacion'], k)] == Decimal(str(d['horas'][k])), (d['identificacion'], k)
assert len(filas) == esperadas, ('filas', len(filas), esperadas)
for k in de_json.values():
    total = sum((v for (i, c), v in sumas.items() if c == k), Decimal(0))
    assert total == Decimal(str(general['totales'][k])), ('total', k)
if convenciones == '1':
    por_nombre = {c['nombre']: i for i, c in enumerate(columnas)}
    for f in filas:
        tipo = f[por_nombre['TIPO_IDENTIFICACION']]
        numero = f[por_nombre['NUMERO_IDENTIFICACION']]
        assert tipo in ('CEDULA', 'PASAPORTE', 'NA'), tipo
        if tipo == 'CEDULA':
            assert re.fullmatch(r'[0-9]{10}', numero), numero
        assert all(celda == celda.upper() for celda in f), f
        if f[por_nombre['COMPONENTE']] != 'CLASE':
            assert f[por_nombre['CODIGO_ASIGNATURA']] == 'NA' and f[por_nombre['PARALELO']] == 'NA', f
        assert re.fullmatch(r'[0-9]+\.[0-9]{2}', f[por_nombre['HORAS_SEMANALES']]), f
        assert re.fullmatch(r'[0-9]{4}-[0-9]{2}-[0-9]{2}', f[por_nombre['FECHA_PUBLICACION']]), f
PY
}

# Los reportes individuales de los docentes del general, para contar las filas que espera el CSV.
bajar_individuales() {
    local una
    mkdir -p "$carpeta/individuales"
    for una in $(jq -r '.docentes[].identificacion' "$carpeta/general.json"); do
        pedir GET "$(ruta_del_reporte "$una")" "Bearer $token"
        [[ $estado == 200 ]] || return 1
        printf '%s' "$cuerpo" >"$carpeta/individuales/$una.json"
    done
}

# --- R9. El CSV para SIGA de la versión 1, con la plantilla como layout vigente (US1 y US2) ---
csv_siga_por_defecto() {
    local antes numero
    iniciar "$SECRETARIA"
    # Al empezar, la plantilla como una versión más del layout de exportación: R9 no depende de lo
    # que dejó una corrida anterior, y la tabla no admite borrar (principio VII).
    antes=$(versiones_del_layout)
    cargar_layout "$PLANTILLA_SIGA"
    [[ $estado == 200 ]] || { echo "       cargar la plantilla: $estado" >&2; return 1; }
    numero=$(jq '.numero' <<<"$cuerpo")
    [[ $numero == $((antes + 1)) ]] || return 1
    layout_vigente || return 1
    [[ $(jq '.vigente.numero' "$carpeta/layouts.json") == "$numero" \
        && $(jq '.vigente.columnas | length' "$carpeta/layouts.json") == 13 ]] || return 1
    jq '.vigente' "$carpeta/layouts.json" >"$carpeta/layout-vigente.json"
    bajar_individuales || return 1
    local antes_exportadas suma_1 suma_2
    antes_exportadas=$(exportaciones_de_la_corrida)
    generar_siga "$carpeta/siga-1.csv"
    [[ $estado == 200 && $(cabecera Content-Disposition) == "attachment; filename=\"siga-$PERIODO-v1.csv\"" \
        && $(cabecera Cache-Control) == no-store ]] || { echo "       CSV para SIGA: $estado" >&2; return 1; }
    suma_1=$(cabecera X-Suma-Verificacion)
    generar_siga "$carpeta/siga-2.csv"
    [[ $estado == 200 ]] || return 1
    suma_2=$(cabecera X-Suma-Verificacion)
    # F19-H02 (SC-002, SC-005): la suma es el sha256sum del archivo, y la misma en los dos.
    [[ $suma_1 =~ ^[0-9a-f]{64}$ && $suma_1 == "$(sha256sum "$carpeta/siga-1.csv" | cut -d' ' -f1)" \
        && $suma_2 == "$suma_1" ]] || { echo "       la suma de verificación: $suma_1 / $suma_2" >&2; return 1; }
    # SC-001, SC-002 y SC-003: las filas, los totales del general y las convenciones de SENESCYT.
    csv_siga_igual_al_general "$carpeta/siga-1.csv" "$carpeta/general.json" "$carpeta/individuales" \
        "$carpeta/layout-vigente.json" 1 || return 1
    # SC-006: dos archivos de la misma versión y el mismo layout son idénticos.
    cmp -s "$carpeta/siga-1.csv" "$carpeta/siga-2.csv" || return 1
    cp "$carpeta/siga-1.csv" "$carpeta/siga-version-1.csv"
    echo "       $(($(wc -l <"$carpeta/siga-1.csv") - 1)) filas con la versión $numero del layout de exportación"
    esperar 60 hay_entradas 2 "$GENERADO_SIGA" \
        || { echo "       entradas: $(entradas "$GENERADO_SIGA")" >&2; return 1; }
    hay_entradas 2 "$GENERADO_SIGA and periodo = '$PERIODO' and (objeto ->> 'version')::int = 1
        and (nuevo ->> 'layout')::int = $numero and objeto ->> 'identificacion' is null
        and nuevo ->> 'sha256' = '$suma_1' and nuevo ->> 'exportacion' is not null" || return 1
    # F19-H02 (SC-003): dos exportaciones registradas, las más recientes de la lista, con la suma.
    [[ $(exportaciones_de_la_corrida) == $((antes_exportadas + 2)) ]] || return 1
    exportaciones_siga || return 1
    local filas
    filas=$(($(wc -l <"$carpeta/siga-1.csv") - 1))
    [[ $(jq -c --arg s "$suma_1" --argjson l "$numero" --argjson f "$filas" --arg n "siga-$PERIODO-v1.csv" \
        '[.exportaciones[:2][] | select(.version == 1 and .layout == $l and .filas == $f and .nombre == $n
            and .sumaVerificacion == $s and .exportadaPor == "secretaria.demo")] | length' \
        "$carpeta/exportaciones.json") == 2 ]] || { echo "       las exportaciones: $(cat "$carpeta/exportaciones.json")" >&2; return 1; }
    printf '%s' "$suma_1" >"$carpeta/suma-version-1"
    hay_entradas 0 "$RECHAZADA_SIGA"
}

# --- R11. Las exportaciones a SIGA no se modifican ni se borran (F19-H02, SC-006) ---
exportacion_siga_inmutable() {
    local antes salida
    antes=$(consultar_base "select count(*) from reporting.exportacion_siga")
    [[ $antes -gt 0 ]] || return 1
    # El disparador frena también al superusuario con el que corre consultar_base.
    if salida=$(consultar_base "update reporting.exportacion_siga set filas = 0" 2>&1); then
        echo "       el update pasó" >&2; return 1
    fi
    grep -q 'no se puede modificar ni borrar' <<<"$salida" || { echo "       $salida" >&2; return 1; }
    if salida=$(consultar_base "delete from reporting.exportacion_siga" 2>&1); then
        echo "       el delete pasó" >&2; return 1
    fi
    grep -q 'no se puede modificar ni borrar' <<<"$salida" || { echo "       $salida" >&2; return 1; }
    [[ $(consultar_base "select count(*) from reporting.exportacion_siga") == "$antes" ]]
}

# --- R10. Otro layout cambia las columnas sin tocar el código; uno inválido se rechaza (US3) ---
layout_siga_nuevo() {
    local antes numero
    iniciar "$SECRETARIA"
    estado=$(curl -s -D "$carpeta/cabeceras" -o "$carpeta/plantilla.yaml" -w '%{http_code}' \
        -H "Authorization: Bearer $token" "$LAYOUT_SIGA/plantilla.yaml" || echo '000')
    [[ $estado == 200 ]] && cmp -s "$carpeta/plantilla.yaml" "$PLANTILLA_SIGA" \
        || { echo "       la plantilla no es el recurso: $estado" >&2; return 1; }
    # Dos nombres cambiados, la asignatura y el paralelo invertidos, sin la fecha y el componente
    # traducido a 01…05.
    python3 -I - "$carpeta/plantilla.yaml" "$carpeta/layout-nuevo.yaml" <<'PY'
import sys
texto = open(sys.argv[1], encoding='utf-8').read()
cuerpo = texto[texto.index('columnas:'):]
cuerpo = cuerpo.replace('NUMERO_IDENTIFICACION', 'CEDULA_DOCENTE').replace('APELLIDOS_NOMBRES', 'DOCENTE')
asignatura = '  - nombre: CODIGO_ASIGNATURA\n    campo: asignatura\n'
paralelo = '  - nombre: PARALELO\n    campo: paralelo\n'
cuerpo = cuerpo.replace(asignatura + paralelo, paralelo + asignatura)
cuerpo = cuerpo.replace('  - nombre: FECHA_PUBLICACION\n    campo: fecha_publicacion\n', '')
cuerpo = cuerpo.replace('    campo: componente\n', '    campo: componente\n    traduccion:\n'
    '      CLASE: "01"\n      OTRAS_DOCENCIA: "02"\n      INVESTIGACION: "03"\n'
    '      VINCULACION: "04"\n      GESTION: "05"\n')
open(sys.argv[2], 'w', encoding='utf-8').write(cuerpo)
PY
    antes=$(versiones_del_layout)
    cargar_layout "$carpeta/layout-nuevo.yaml"
    [[ $estado == 200 ]] || { echo "       el layout nuevo: $estado ${cuerpo:0:300}" >&2; return 1; }
    numero=$(jq '.numero' <<<"$cuerpo")
    [[ $numero == $((antes + 1)) ]] || return 1
    esperar 60 hay_entradas 1 "$CARGADO_SIGA and (objeto ->> 'layout')::int = $numero" || return 1
    # Uno inválido: un campo que no existe y dos nombres repetidos; el vigente no cambia (SC-005).
    printf 'columnas:\n  - {nombre: CEDULA, campo: identificacion}\n  - {nombre: COMPONENTE, campo: componente}\n  - {nombre: HORAS, campo: horas_semanales}\n  - {nombre: horas, campo: horas}\n' \
        >"$carpeta/layout-invalido.yaml"
    cargar_layout "$carpeta/layout-invalido.yaml"
    es_codigo 400 REPORTING_LAYOUT_INVALIDO || return 1
    [[ $(jq '.detalles | length' <<<"$cuerpo") == 2 ]] || { echo "       detalles: ${cuerpo:0:300}" >&2; return 1; }
    [[ $(versiones_del_layout) == $((antes + 1)) ]] || return 1
    layout_vigente || return 1
    [[ $(jq '.vigente.numero' "$carpeta/layouts.json") == "$numero" ]] || return 1
    jq '.vigente' "$carpeta/layouts.json" >"$carpeta/layout-nuevo.json"
    # SC-004: el CSV sale con las columnas nuevas, en su orden, sin la fecha, con 01…05, y los mismos
    # totales.
    generar_siga "$carpeta/siga-nuevo.csv"
    [[ $estado == 200 ]] || return 1
    [[ $(head -1 "$carpeta/siga-nuevo.csv" | tr -d '\r') == \
        PERIODO,TIPO_IDENTIFICACION,CEDULA_DOCENTE,DOCENTE,DEDICACION,COMPONENTE,CODIGO_ACTIVIDAD,CODIGO_CARRERA,PARALELO,CODIGO_ASIGNATURA,HORAS_SEMANALES,VERSION ]] \
        || { echo "       encabezado: $(head -1 "$carpeta/siga-nuevo.csv")" >&2; return 1; }
    [[ $(tail -n +2 "$carpeta/siga-nuevo.csv" | cut -d, -f6 | sort -u | tr -d '\r' | grep -cv '^0[1-5]$') == 0 ]] \
        || { echo "       el componente no está traducido" >&2; return 1; }
    csv_siga_igual_al_general "$carpeta/siga-nuevo.csv" "$carpeta/general.json" "$carpeta/individuales" \
        "$carpeta/layout-nuevo.json" 0 || return 1
    # Al terminar, la plantilla otra vez: R5 compara con el CSV de R9.
    cargar_layout "$PLANTILLA_SIGA"
    [[ $estado == 200 ]]
}

# --- R4. Sin permiso, 403; un docente que no está, 404; una identificación inválida, 400 ---
reporte_sin_acceso() {
    local usuario
    for usuario in "$COORDINADOR" "$DESIGNADO"; do
        iniciar "$usuario"
        pedir GET "$(ruta_del_reporte "$docente_con_clase")" "Bearer $token"
        es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || { echo "       $usuario: $estado" >&2; return 1; }
        # F20-H02 (SC-008): el general y su PDF, tampoco.
        pedir GET "$(ruta_del_general)" "Bearer $token"
        es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || { echo "       $usuario, general: $estado" >&2; return 1; }
        bajar "$(ruta_del_general)" pdf "$carpeta/general-sin-permiso.pdf"
        [[ $estado == 403 ]] || { echo "       $usuario, PDF del general: $estado" >&2; return 1; }
    done
    iniciar "$RECTOR"
    pedir GET "$(ruta_del_reporte 9999999999)" "Bearer $token"
    es_codigo 404 REPORTING_DOCENTE_SIN_DISTRIBUTIVO || return 1
    pedir GET "$(ruta_del_reporte no-valida)" "Bearer $token"
    es_codigo 400 COMUN_SOLICITUD_INVALIDA || return 1
    pedir GET "$(ruta_del_general periodo-con-mas-de-veinte)" "Bearer $token"
    es_codigo 400 COMUN_SOLICITUD_INVALIDA || return 1
    # F19-H01 (SC-009): solo la Secretaría genera el CSV para SIGA y carga el layout.
    local versiones generados
    versiones=$(versiones_del_layout)
    generados=$(entradas "$GENERADO_SIGA")
    for usuario in "$RECTOR" "$COORDINADOR" "$ADMINISTRADOR"; do
        iniciar "$usuario"
        pedir GET "$CSV_SIGA" "Bearer $token"
        es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || { echo "       $usuario, CSV para SIGA: $estado" >&2; return 1; }
        cargar_layout "$PLANTILLA_SIGA"
        [[ $estado == 403 ]] || { echo "       $usuario, cargar el layout: $estado" >&2; return 1; }
        # F19-H02 (SC-008): tampoco ven las exportaciones.
        pedir GET "$EXPORTACIONES_SIGA" "Bearer $token"
        es_codigo 403 SEGURIDAD_ACCESO_NO_AUTORIZADO || { echo "       $usuario, exportaciones: $estado" >&2; return 1; }
    done
    [[ $(versiones_del_layout) == "$versiones" && $(entradas "$GENERADO_SIGA") == "$generados" ]]
}

# --- R5. Durante la reforma, la versión 1 no cambia aunque se corrijan las horas de la oferta ---
reporte_durante_la_reforma() {
    local asignatura paralelo oferta nuevas
    iniciar "$RECTOR"
    pedir GET "$REPORTES/publicaciones" "Bearer $token"
    [[ $(jq -c --arg p "$PERIODO" '[.[] | select(.periodo == $p) | .version, .enReforma]' <<<"$cuerpo") == '[1,true]' ]] \
        || return 1
    asignatura=$(jq -r '.asignaturas[0].codigo' "$carpeta/reporte.json")
    paralelo=$(jq -r '.asignaturas[0].paralelo' "$carpeta/reporte.json")
    oferta="$GATEWAY/api/academico/periodos/$PERIODO/oferta/$asignatura/$paralelo"
    iniciar "$VICERRECTOR"
    pedir GET "$oferta" "Bearer $token"
    [[ $estado == 200 ]] || return 1
    printf '%s' "$cuerpo" >"$carpeta/paralelo.json"
    nuevas=$(jq '[.horasSemanales - 1, 0.5] | max' "$carpeta/paralelo.json")
    pedir PUT "$oferta" "Bearer $token" "$(jq -c --argjson h "$nuevas" \
        '{jornada, modalidad, estudiantes, horasSemanales: $h} + (if .sedeIndicada then {sede} else {} end)' \
        "$carpeta/paralelo.json")"
    [[ $estado == 200 ]] || { echo "       corregir la oferta: $estado" >&2; return 1; }
    iniciar "$RECTOR"
    bajar "$(ruta_del_reporte "$docente_con_clase")" csv "$carpeta/reporte-en-reforma.csv"
    local igual=0
    cmp -s "$carpeta/reporte-version-1.csv" "$carpeta/reporte-en-reforma.csv" || igual=1
    # F20-H02: el CSV del general tampoco cambia.
    bajar "$(ruta_del_general)" csv "$carpeta/general-en-reforma.csv"
    cmp -s "$carpeta/general-version-1.csv" "$carpeta/general-en-reforma.csv" \
        || { echo "       el CSV del general cambió durante la reforma" >&2; igual=1; }
    # F19-H01: el CSV para SIGA tampoco cambia; sale de la versión 1.
    iniciar "$SECRETARIA"
    local exportadas_antes
    exportadas_antes=$(exportaciones_de_la_corrida)
    generar_siga "$carpeta/siga-en-reforma.csv"
    [[ $estado == 200 ]] && cmp -s "$carpeta/siga-version-1.csv" "$carpeta/siga-en-reforma.csv" \
        || { echo "       el CSV para SIGA cambió durante la reforma: $estado" >&2; igual=1; }
    # F19-H02: la misma suma que en R9 y una exportación más de la versión 1.
    [[ $(cabecera X-Suma-Verificacion) == "$(cat "$carpeta/suma-version-1")" \
        && $(exportaciones_de_la_corrida) == $((exportadas_antes + 1)) ]] \
        || { echo "       la exportación durante la reforma no es la de la versión 1" >&2; igual=1; }
    # Las horas vuelven a las de antes: los pasos 25 a 27 recorren la versión 2 con la oferta del
    # inicio de la prueba.
    iniciar "$VICERRECTOR"
    pedir PUT "$oferta" "Bearer $token" "$(jq -c \
        '{jornada, modalidad, estudiantes} + (if .horasIndicadas then {horasSemanales} else {} end)
            + (if .sedeIndicada then {sede} else {} end)' "$carpeta/paralelo.json")"
    [[ $estado == 200 ]] || { echo "       restaurar la oferta: $estado" >&2; return 1; }
    ((igual == 0))
}

# --- R6. Con la versión 2 publicada, el reporte sale de ella ---
reporte_de_la_version_2() {
    iniciar "$RECTOR"
    pedir GET "$(ruta_del_reporte "$docente_con_clase")" "Bearer $token"
    [[ $estado == 200 && $(jq -c '[.publicacion.version, .publicacion.hash, .publicacion.enReforma]' <<<"$cuerpo") \
        == "$(jq -cn --arg h "$(hash_publicado)" '[2, $h, false]')" ]] || return 1
    # F20-H02: el general también sale de la versión 2, y sus totales son la suma de sus filas.
    pedir GET "$(ruta_del_general)" "Bearer $token"
    [[ $estado == 200 && $(jq -c '[.publicacion.version, .publicacion.hash]' <<<"$cuerpo") \
        == "$(jq -cn --arg h "$(hash_publicado)" '[2, $h]')" ]] || return 1
    printf '%s' "$cuerpo" >"$carpeta/general-2.json"
    totales_son_la_suma "$carpeta/general-2.json" || return 1
    [[ $(consultar_base "select tardio from generacion.contenido_congelado where periodo = '$PERIODO'
        and numero = 2") == f ]]
}

verificar 1 "El consolidado listo, enviado a revisión y aprobado por vicerrectorado.demo: CONGELADO_EN_FIRMA" aprobado_y_congelado
verificar 2 "El documento GENERADO en menos de 30 s sin que nadie lo pida, por sistema y con el hash congelado" generado_solo
verificar 3 "El archivo del volumen es documentos/<sha256>.pdf y su SHA-256 es el registrado" archivo_por_su_hash
verificar 4 "veraPDF ($VERAPDF, PDF/A-2b) lo da conforme, con los tres campos de firma vacíos" conforme
verificar 5 "Un POST del Responsable devuelve el mismo, sin otro archivo ni fila; coordinacion.demo, 403" idempotente
verificar 6 "La bitácora: una DOCUMENTO_FIRMABLE_GENERADO con la versión, el hash y el SHA-256" bitacora
verificar 7 "$DESIGNADO ve su firma pendiente («Elaborado por») y descarga el PDF con el SHA-256 registrado" pendiente_del_designado
verificar 8 "$RESPONSABLE, $VICERRECTOR y rectorado.demo no la ven y reciben 403; $COORDINADOR, sin permiso, 403" nadie_mas
verificar 9 "Siete PDF rechazados con su 422 y las siete comprobaciones; el turno y el vigente no cambian" rechazos
verificar 10 "$VICERRECTOR y $RESPONSABLE, fuera de turno, reciben 403 al subir, sin dejar fila" fuera_de_turno
if [[ $CONSERVAR == F2 ]]; then
    verificar 11 "F2 de la persona de F1 se rechaza; F1 se acepta y F2 queda en turno, con la cadena" tres_firmas
    if ((fallas > 0)); then
        echo "No quedó F2 en turno. Para limpiar: docker compose -p tesis-prototipo-2026 down -v"
        exit 1
    fi
    echo "Quedó F1 aceptada y F2 en turno, sin limpiar. Recorre specs/054-siguiente-firmante/quickstart.md §3."
    echo "Para limpiar al terminar: docker compose -p tesis-prototipo-2026 down -v"
    exit 0
fi
verificar 11 "F2 de la persona de F1 se rechaza; F1, F2 y F3 se aceptan en orden con la cadena; al final nadie está en turno" tres_firmas
verificar 12 "3 aceptadas y 8 rechazadas, los 4 archivos y la bitácora; el proceso en EN_OCS por sistema" registro_de_firmas
verificar R1 "En EN_OCS, sin publicación: Reporting no la lista y el individual, el general y el CSV para SIGA responden 409 REPORTING_SIN_PUBLICACION, sin bitácora" reportes_sin_publicacion

if [[ $CONSERVAR == EN_OCS ]]; then
    if ((fallas > 0)); then
        echo "No quedó en EN_OCS. Para limpiar: docker compose -p tesis-prototipo-2026 down -v"
        exit 1
    fi
    echo "Quedó en EN_OCS con las tres firmas, sin limpiar. Recorre specs/052-resolucion-ocs/quickstart.md §3."
    echo "Para limpiar al terminar: docker compose -p tesis-prototipo-2026 down -v"
    exit 0
fi

verificar 13 "En EN_OCS, el avance responde 409 VALIDACIONOCS_REQUIERE_RESOLUCION; otros roles, 403 al registrar" sin_resolucion
verificar 14 "Número vacío, fecha futura y anterior, un archivo que no es PDF y uno de más de 10 MB: su error, sin fila" resoluciones_invalidas
verificar 15 "$SECRETARIA registra la resolución: APROBADO, el PDF por su huella, la descarga idéntica y la bitácora" resolucion_registrada

if [[ $CONSERVAR == APROBADO ]]; then
    if ((fallas > 0)); then
        echo "No quedó en APROBADO. Para limpiar: docker compose -p tesis-prototipo-2026 down -v"
        exit 1
    fi
    echo "Quedó en APROBADO con la resolución, sin publicar ni limpiar. Recorre specs/055-publicar-distributivo/quickstart.md §3."
    echo "Para limpiar al terminar: docker compose -p tesis-prototipo-2026 down -v"
    exit 0
fi

verificar 16 "En APROBADO, otros roles reciben 403; $SECRETARIA publica con identidad detenido, con la constancia y la bitácora; la segunda, 409" publicado
verificar 17 "Publicado: nadie edita (solo $VICERRECTOR puede reformar); proponer, consolidar, enviar, aprobar y registrar responden con su error, sin cambiar nada" sin_acciones
verificar R2 "El reporte de la versión 1: su hash, filas que suman su total, la carga horaria de la versión y el contenido guardado al congelar" reporte_de_la_version_1
verificar R3 "PDF/A sin campos y CSV con los mismos datos; dos CSV idénticos; 8 descargas en la bitácora y ver el reporte no deja entrada" reporte_en_dos_formatos
verificar R7 "El reporte general de la versión 1: los docentes del listado sin repetidos, cada fila igual a su individual y los totales, la suma de las filas" reporte_general_de_la_version_1
verificar R8 "El general en PDF/A sin campos y en CSV con los mismos datos; dos CSV idénticos; 4 descargas REPORTE_GENERAL_DESCARGADO sin docente" reporte_general_en_dos_formatos
verificar R9 "El CSV para SIGA de la versión 1: una fila por asignatura y actividad, los totales del general, las convenciones de SENESCYT, dos archivos idénticos con la misma suma de verificación, 2 exportaciones registradas y 2 entradas CSV_SIGA_GENERADO con la suma" csv_siga_por_defecto
verificar R10 "Un layout nuevo cambia las columnas sin tocar el código; uno inválido se rechaza con 2 problemas y no cambia el vigente" layout_siga_nuevo
verificar R11 "Las exportaciones a SIGA no se modifican ni se borran" exportacion_siga_inmutable
verificar R4 "$COORDINADOR y $DESIGNADO, 403 en el individual y el general; un docente que no está, 404; una identificación o un periodo inválidos, 400; $RECTOR, $COORDINADOR y $ADMINISTRADOR, 403 en el CSV para SIGA y el layout" reporte_sin_acceso
if [[ $CONSERVAR == PUBLICADO ]]; then
    if ((fallas > 0)); then
        echo "No quedó publicado. Para limpiar: docker compose -p tesis-prototipo-2026 down -v"
        exit 1
    fi
    echo "Quedó en PUBLICADO con la versión 1, sin reformar ni limpiar. Recorre specs/056-reforma-version-publicada/quickstart.md §3."
    echo "Para limpiar al terminar: docker compose -p tesis-prototipo-2026 down -v"
    exit 0
fi

verificar 18 "Las bandejas de todos tienen exactamente las de cada transición; $VICERRECTOR marca la suya y la ajena da 404" notificaciones_y_lectura
verificar 19 "La publicación con identidad detenido respondió 200, y su notificación llegó una vez a $DESIGNADO antes de $ESPERA_REENVIO s" llego_al_volver
verificar 20 "Una entrega repetida se completa otra vez sin duplicar la notificación, que sigue leída" entrega_repetida
verificar 21 "F1 subida con identidad detenido: aceptada, y la notificación de F2 llegó una vez antes de $ESPERA_REENVIO s" llego_la_de_f2


verificar 22 "En PUBLICADO, otros roles reciben 403 y un motivo corto 400; $VICERRECTOR reforma con identidad detenido: la versión 2 y la 1 CONGELADA; la segunda, 409" reforma_iniciada
verificar 23 "Durante la reforma, la versión 1 sigue publicada y vigente, y el flujo muestra la reforma en curso" publicada_vigente
verificar R5 "Durante la reforma, el individual, el general y el CSV para SIGA salen de la versión 1 y no cambian al corregir las horas de un paralelo en la oferta" reporte_durante_la_reforma
verificar 24 "La reforma llegó una vez a quien consolida, los docentes, Talento Humano y las coordinaciones; no al Vicerrector" reforma_notificada
verificar 25 "La versión 2 vuelve por el ciclo: devolver, reenviar, consolidar, revisión y aprobación, con su documento desde F1" ciclo_de_la_version_2
verificar 26 "F1, F2 y F3 sobre la versión 2, su resolución y su publicación: la versión 2 es la publicada, sin reforma en curso" firmas_y_publicacion_de_la_version_2
verificar R6 "Con la versión 2 publicada, el individual y el general salen de ella, con su hash y su contenido guardado al congelar" reporte_de_la_version_2
verificar 27 "La versión 1, histórica: CONGELADA, con sus tres firmas, su documento y su resolución, sin anulación" version_1_historica

if ((fallas > 0)); then
    echo "El documento firmable de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "El documento firmable de punta a punta: todos los pasos pasan."
