#!/usr/bin/env bash
# Deja el equipo de demostración con los datos de prueba, para las comprobaciones del autor «A mano, en
# la demo» (por ejemplo, specs/039-bitacora-cambios/quickstart.md). Uso, en el equipo:
#   sudo deploy/equipo/poblar_demo.sh
# Sobre una demo sin estructura orgánica, carreras, periodos, docentes ni distributivos (recién
# preparada con preparar.sh): sube y confirma las 10 cargas de prueba (ficticias, RNF-SEG-06) con
# datos.demo; asigna su política al periodo 2026-I con vicerrectorado.demo; le da a coordinacion.demo
# la carrera con menos oferta (admin.demo); crea el distributivo (vicerrectorado.demo) y le asigna 4
# horas de la actividad 140-b a un docente sin cargo ni proyecto (coordinacion.demo). No designa al
# Responsable de consolidación: eso queda para la comprobación del autor.
# Si la base ya tiene alguno de esos datos, termina sin tocar nada. No borra lo que crea, porque es para
# la demo: si un paso falla, lo dice y termina, y lo hecho hasta ahí queda.
# Lee MDD_PROYECTO, DIRECCION, PUERTO_HTTPS y MDD_SECRETOS de $RAIZ/.env, y las contraseñas de
# secretos/claves-demo (D-59). Las credenciales y los tokens van a curl por la entrada estándar: no
# aparecen en la lista de procesos ni en la salida. Termina con 0 si todos los pasos pasan.
set -euo pipefail
cd "$(dirname "$0")/../.."

readonly RAIZ="${RAIZ:-/opt/mdd}"
readonly ENTORNO="$RAIZ/.env"
readonly DATOS=backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba
readonly CARGAS=(estructura_organica carreras ciclos_academicos asignaturas_malla oferta_periodo
    docentes politica_carga_horaria cargos_autoridades proyectos_practicas matriculados_carrera)
readonly PERIODO=2026-I
readonly ACTIVIDAD=140-b
readonly HORAS=4
# Lo que tarda un evento entre procesos en llegar, con margen (el reenvío es cada 30 s).
readonly ESPERA=60

[[ -r $ENTORNO ]] || { echo "No se puede leer $ENTORNO: correr con sudo en el equipo de demostración." >&2; exit 1; }
for herramienta in curl jq docker python3; do
    command -v "$herramienta" >/dev/null || { echo "Falta $herramienta." >&2; exit 1; }
done

# Lee una variable del .env sin ejecutarlo (el hash de Caddy tiene $ y va entre comillas simples).
valor() {
    local linea
    linea=$(grep -E "^$1=" "$ENTORNO" | tail -1 || true)
    linea=${linea#*=}
    linea=${linea#\'}
    printf '%s' "${linea%\'}"
}

proyecto=$(valor MDD_PROYECTO)
proyecto=${proyecto:-mdd}
direccion=$(valor DIRECCION)
direccion=${direccion:-mdd.localhost}
puerto=$(valor PUERTO_HTTPS)
puerto=${puerto:-443}
secretos=$(valor MDD_SECRETOS)
secretos=${secretos:-$RAIZ/secretos}
readonly BASE="https://$direccion:$puerto"

opciones=(-s --max-time 60 --resolve "$direccion:$puerto:127.0.0.1")
# La CA interna de Caddy solo se acepta para los nombres *.localhost del equipo, como probar_humo.sh.
[[ $direccion == *.localhost || $direccion == localhost ]] && opciones+=(-k)

carpeta=$(mktemp -d)
trap 'rm -rf "$carpeta"' EXIT

# psql en la base de la demo, con el superusuario; sin alineación ni encabezados.
consultar_base() {
    docker exec "$proyecto-postgres-1" psql -q -At -U postgres -d mdd -v ON_ERROR_STOP=1 -c "$1"
}

clave_de() {  # clave_de <usuario>
    awk -F= -v u="$1" '$1 == u { sub(/^[^=]*=/, ""); print; exit }' "$secretos/claves-demo"
}

# Escapa un texto para una cadena entre comillas de la configuración de curl.
entre_comillas() { sed 's/\\/\\\\/g; s/"/\\"/g' <<<"$1"; }

# Deja el estado en $estado y el cuerpo en $cuerpo. La configuración extra de curl (token, cuerpo o
# formulario) llega por la entrada estándar con -K -.
estado=''
cuerpo=''
pedir() {  # pedir <método> <ruta> [campos de curl…] < configuración de curl
    local archivo metodo=$1 ruta=$2
    shift 2
    archivo=$(mktemp -p "$carpeta")
    estado=$(curl "${opciones[@]}" -K - -o "$archivo" -w '%{http_code}' -X "$metodo" "$@" "$BASE$ruta" \
        || echo 000)
    cuerpo=$(cat "$archivo")
}

# iniciar <usuario>: deja el token en $token, o termina si no entra.
token=''
iniciar() {
    local credenciales clave
    clave=$(clave_de "$1")
    [[ -n $clave ]] || { echo "FALLA  $1 no tiene contraseña en $secretos/claves-demo." >&2; exit 1; }
    credenciales=$(jq -cn --arg usuario "$1" --arg clave "$clave" '{usuario: $usuario, clave: $clave}')
    pedir POST /api/usuarios/sesion <<FIN
header = "Content-Type: application/json"
data = "$(entre_comillas "$credenciales")"
FIN
    token=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
    [[ $estado == 200 && -n $token ]] || { echo "FALLA  $1 no inicia sesión ($estado)." >&2; exit 1; }
}

# json <método> <ruta> [cuerpo json]: con el token de $token.
json() {
    local datos=''
    [[ -n ${3:-} ]] && datos="data = \"$(entre_comillas "$3")\""
    pedir "$1" "$2" <<FIN
header = "Authorization: Bearer $token"
header = "Content-Type: application/json"
$datos
FIN
}

# Repite la petición hasta que responda el estado esperado o pasen $ESPERA segundos: algunos pasos
# dependen de un evento que otro proceso todavía no recibe.
hasta() {  # hasta <estado esperado> <método> <ruta> [cuerpo json]
    local esperado=$1 limite=$(($(date +%s) + ESPERA))
    shift
    until json "$@"; [[ $estado == "$esperado" ]]; do
        (($(date +%s) >= limite)) && return 1
        sleep 2
    done
}

paso() {  # paso <número> <descripción> <condición…>
    local numero=$1 descripcion=$2
    shift 2
    if "$@"; then
        echo "OK     $numero. $descripcion"
    else
        echo "FALLA  $numero. $descripcion"
        echo "       última respuesta: $estado ${cuerpo:0:300}" | tr -d '\r'
        echo "Lo hecho hasta el paso anterior queda en la demo." >&2
        exit 1
    fi
}

# --- Del archivo: la política, la carrera con menos oferta y un docente libre --------------------
python3 -I - "$DATOS" "$carpeta" <<'PY'
import csv, sys
datos, carpeta = sys.argv[1:]

def leer(nombre):
    with open(f'{datos}/{nombre}', encoding='utf-8-sig', newline='') as archivo:
        filas = list(csv.reader(archivo))
    return [dict(zip(filas[0], fila)) for fila in filas[1:]]

def lista(filas, campo):
    return ','.join("'" + v + "'" for v in sorted({f[campo] for f in filas if f[campo]}))

unidades, carreras = leer('01_estructura_organica.csv'), leer('02_carreras.csv')
periodos, docentes = leer('03_ciclos_academicos.csv'), leer('06_docentes.csv')
politica = leer('07_politica_carga_horaria.csv')[0]['codigo_version_politica']
asignaturas = {a['codigo_asignatura']: a for a in leer('04_asignaturas_malla.csv')}
por_carrera = {}
for una in leer('05_oferta_periodo.csv'):
    por_carrera.setdefault(asignaturas[una['codigo_asignatura']]['codigo_carrera'], []).append(una)
carrera = sorted(por_carrera, key=lambda c: (len(por_carrera[c]), c))[0]
nombre_carrera = next(c['nombre'] for c in carreras if c['codigo_carrera'] == carrera)
con_cargo = {c['numero_identificacion'] for c in leer('08_cargos_autoridades.csv')}
con_proyecto = {uno.strip().split('|')[0] for p in leer('09_proyectos_practicas.csv')
                for uno in p['participantes'].split(';')}
libre = next(d for d in docentes if d['estado'] == 'ACTIVO' and d['dedicacion'] == 'TC'
             and d['numero_identificacion'] not in con_proyecto | con_cargo)
nombre = ' '.join(filter(None, [libre['primer_nombre'], libre['primer_apellido'], libre['segundo_apellido']]))
with open(f'{carpeta}/datos.tsv', 'w', encoding='utf-8') as salida:
    salida.write('\t'.join([lista(unidades, 'codigo_unidad'), lista(carreras, 'codigo_carrera'),
                            lista(periodos, 'codigo_periodo'), lista(docentes, 'numero_identificacion'),
                            politica, carrera, nombre_carrera, libre['numero_identificacion'], nombre]) + '\n')
PY
IFS=$'\t' read -r unidades carreras periodos docentes politica carrera nombre_carrera libre nombre_libre \
    <"$carpeta/datos.tsv"

# --- 0. La demo está vacía ---------------------------------------------------------------------
ya_estan=$(consultar_base "select (select count(*) from academico.unidad_organica where activa or codigo in ($unidades))
    + (select count(*) from academico.carrera where estado = 'VIGENTE' or codigo in ($carreras))
    + (select count(*) from academico.periodo_academico where codigo in ($periodos))
    + (select count(*) from docentes.docente where identificacion in ($docentes))
    + (select count(*) from roles.carrera_de_coordinador)
    + (select count(*) from generacion.distributivo)") \
    || { echo "FALLA  0. No se pudo consultar la base en $proyecto-postgres-1: ¿está levantada la demo?" >&2; exit 1; }
if [[ $ya_estan != 0 ]]; then
    echo "FALLA  0. La demo ya tiene $ya_estan unidades, carreras, periodos, docentes, carreras de coordinadores o distributivos: no se toca nada." >&2
    exit 1
fi
echo "OK     0. La demo no tiene estructura, carreras, periodos, docentes ni distributivos"

# --- 1. Las 10 cargas, con datos.demo ------------------------------------------------------------
cargar() {
    local i archivo huella
    for i in "${!CARGAS[@]}"; do
        archivo=$(printf '%s/%02d_%s.csv' "$DATOS" $((i + 1)) "${CARGAS[$i]}")
        pedir POST "/api/cargas/plantillas/${CARGAS[$i]}/archivo" \
            -F "archivo=@$archivo;filename=$(basename "$archivo")" <<<"header = \"Authorization: Bearer $token\""
        huella=$(jq -r '.vistaPrevia.huella // empty' <<<"$cuerpo" 2>/dev/null || true)
        [[ $estado == 200 && $(jq -r '.errores' <<<"$cuerpo") == 0 && -n $huella ]] \
            || { echo "       ${CARGAS[$i]}: la vista previa falló"; return 1; }
        pedir POST "/api/cargas/plantillas/${CARGAS[$i]}/archivo/confirmacion" \
            -F "archivo=@$archivo;filename=$(basename "$archivo")" -F "huella=$huella" \
            <<<"header = \"Authorization: Bearer $token\""
        [[ $estado == 200 && $(jq -r '.resultado' <<<"$cuerpo") == APLICADA ]] \
            || { echo "       ${CARGAS[$i]}: la confirmación falló"; return 1; }
    done
}
iniciar datos.demo
paso 1 "Las 10 cargas de prueba, subidas y confirmadas por datos.demo" cargar

# --- 2. La política del periodo, con vicerrectorado.demo --------------------------------------------
asignar_politica() {
    local numero limite=$(($(date +%s) + ESPERA))
    until numero=$(consultar_base "select numero from normativa.version_normativa where codigo = '$politica'") \
        && [[ -n $numero ]]; do
        (($(date +%s) >= limite)) && return 1
        sleep 2
    done
    hasta 200 PUT "/api/academico/periodos/$PERIODO/normativa" "{\"version\":$numero}"
}
iniciar vicerrectorado.demo
paso 2 "La política $politica rige el periodo $PERIODO" asignar_politica

# --- 3. La carrera de coordinacion.demo, con admin.demo ------------------------------------------
iniciar admin.demo
paso 3 "coordinacion.demo coordina $carrera ($nombre_carrera)" \
    hasta 200 PUT /api/roles/usuarios/coordinacion.demo/carrera "{\"carrera\":\"$carrera\"}"

# --- 4. El distributivo del periodo, con vicerrectorado.demo ---------------------------------------
iniciar vicerrectorado.demo
paso 4 "El distributivo de $PERIODO" hasta 201 POST "/api/generacion/periodos/$PERIODO/distributivo"

# --- 5. Una actividad con 4 horas, con coordinacion.demo -------------------------------------------
iniciar coordinacion.demo
paso 5 "$nombre_libre tiene $HORAS horas de la actividad $ACTIVIDAD" \
    hasta 200 PUT "/api/generacion/periodos/$PERIODO/parciales/$carrera/docentes/$libre/actividades/$ACTIVIDAD" \
    "{\"horas\":$HORAS}"

cat <<FIN

Listo. En https://$direccion, con coordinacion.demo: periodo $PERIODO, carrera $carrera ($nombre_carrera),
docente $nombre_libre, actividad $ACTIVIDAD con $HORAS horas.
FIN
