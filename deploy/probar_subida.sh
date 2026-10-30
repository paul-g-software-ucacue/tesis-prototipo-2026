#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# La subida de un archivo de carga de punta a punta sobre el perfil levantado (F01-H02;
# quickstart.md de specs/019-subir-archivo-carga, research §10).
# Uso, desde cualquier directorio: deploy/probar_subida.sh
# Arma en una carpeta temporal archivos de carreras ficticios: el CSV, el mismo con punto y coma y en
# XLSX, uno de unos 2 MB, uno sin codigo_carrera, uno con una columna de más, uno en Windows-1252,
# uno sin registros, un PDF y dos grandes (6 y 11 MB). Talento Humano sube los válidos y ve sus
# registros, también el de 2 MB a través del frontend (nginx); cada inválido da su código. El
# Responsable de consolidación y el Vicerrector suben (D-71); un coordinador y un docente reciben 403.
# El archivo no se guarda. Como las carreras dependen de la estructura orgánica (el orden de las
# cargas, F01-H05), si no hay una unidad orgánica activa crea una sede matriz temporal, con un
# prefijo P y tres dígitos, y la borra al salir junto con la carpeta temporal.
# Termina con 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime el token ni la
# contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly FRONTEND="${MDD_FRONTEND:-http://localhost:8081}"
readonly CUERPO_403='{"titulo":"Acceso no autorizado","descripcion":"No tienes permiso para esta acción. Si crees que deberías tenerlo, pide al administrador que revise tus roles.","codigo":"SEGURIDAD_ACCESO_NO_AUTORIZADO"}'
readonly ENCABEZADOS='codigo_carrera,nombre,siglas,codigo_unidad_sede,nivel_formacion,modalidad,jornadas,campo_amplio,campo_especifico,campo_detallado,numero_ciclos,estado'

carpeta=$(mktemp -d)
# La sede matriz temporal, si hace falta: P y tres dígitos.
prefijo="P$(printf '%03d' $((RANDOM % 1000)))"
sede_creada=""

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

# psql en la base del perfil; imprime el resultado sin alineación ni encabezados.
consultar_base() {
    docker compose exec -T postgres psql -q -At -U postgres -d "$(valor_de_entorno POSTGRES_DB)" \
        -v ON_ERROR_STOP=1 -c "$1"
}

limpiar() {
    if [[ -n $sede_creada ]]; then
        consultar_base "delete from eventos_datos.event_publication where serialized_event::jsonb -> 'contenido' ->> 'codigo' like '$prefijo%';
delete from academico.registro_cambio where detalle ->> 'codigo' like '$prefijo%';
delete from academico.unidad_organica where codigo like '$prefijo%';" >/dev/null 2>&1 \
            || echo "AVISO  No se pudo borrar la sede temporal $sede_creada"
    fi
    rm -rf "$carpeta"
}
trap limpiar EXIT

fallas=0
verificar() {
    local numero=$1 descripcion=$2
    shift 2
    if "$@"; then
        echo "OK     $numero. $descripcion"
    else
        echo "FALLA  $numero. $descripcion"
        fallas=$((fallas + 1))
    fi
}

# Deja el estado en $estado y el cuerpo en $cuerpo.
pedir() {  # pedir <método> <url> [autorización] [cuerpo json]
    local archivo
    archivo=$(mktemp -p "$carpeta")
    local argumentos=(-s -o "$archivo" -w '%{http_code}' -X "$1" "$2")
    [[ -n ${3:-} ]] && argumentos+=(-H "Authorization: $3")
    [[ -n ${4:-} ]] && argumentos+=(-H 'Content-Type: application/json' --data "$4")
    estado=$(curl "${argumentos[@]}" || echo '000')
    cuerpo=$(cat "$archivo")
}

# subir <base> <carga> <archivo> [nombre]: sube el archivo con el token de $token.
subir() {
    local archivo
    archivo=$(mktemp -p "$carpeta")
    estado=$(curl -s -o "$archivo" -w '%{http_code}' -X POST \
        -H "Authorization: Bearer $token" \
        -F "archivo=@$3;filename=${4:-$(basename "$3")}" \
        "$1/api/cargas/plantillas/$2/archivo" || echo '000')
    cuerpo=$(cat "$archivo")
}

# iniciar <usuario>: deja el token en $token, o vacío si no entró.
iniciar() {
    pedir POST "$GATEWAY/api/usuarios/sesion" "" \
        "$(jq -cn --arg u "$1" --arg c "$(valor_de_entorno MDD_USUARIOS_DEMO_CLAVE)" '{usuario: $u, clave: $c}')"
    token=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
}

es_lectura() {  # es_lectura <formato> <registros>
    [[ $estado == 200 && $(jq -r '.formato' <<<"$cuerpo") == "$1" && $(jq -r '.registros' <<<"$cuerpo") == "$2" ]]
}

es_codigo() {  # es_codigo <estado> <código del catálogo>
    [[ $estado == "$1" && $(jq -r '.codigo' <<<"$cuerpo" 2>/dev/null) == "$2" ]]
}

# --- Los archivos, ficticios (RNF-SEG-06) ---
armar_archivos() {
    local fila='CF001,Carrera de prueba,CP,MATRIZ,TECNOLOGICO_SUPERIOR,PRESENCIAL,"MATUTINA; VESPERTINA",06,061,,4,VIGENTE'
    printf '%s\r\n%s\r\n%s\r\n' "$ENCABEZADOS" "$fila" "${fila/CF001/CF002}" >"$carpeta/carreras.csv"
    sed 's/,/;/g; s/"MATUTINA; VESPERTINA"/MATUTINA/' "$carpeta/carreras.csv" >"$carpeta/punto_y_coma.csv"
    printf '%s\r\n' "${ENCABEZADOS#codigo_carrera,}" "${fila#CF001,}" >"$carpeta/sin_codigo.csv"
    printf '%s\r\n' "$ENCABEZADOS,extra" "$fila,x" >"$carpeta/columna_de_mas.csv"
    printf '%s\r\n' "$ENCABEZADOS" "${fila/Carrera de prueba/Carrera de Peñafiel}" \
        | iconv -f UTF-8 -t WINDOWS-1252 >"$carpeta/windows.csv"
    printf '%s\r\n' "$ENCABEZADOS" >"$carpeta/sin_registros.csv"
    printf '%%PDF-1.7\n1 0 obj\n' >"$carpeta/carreras.pdf"
    # Unos 2 MB: la misma fila muchas veces, para pasar el límite de 1 MB que nginx tiene por defecto.
    { printf '%s\r\n' "$ENCABEZADOS"; for _ in $(seq 1 22000); do printf '%s\r\n' "$fila"; done; } >"$carpeta/dos_megas.csv"
    head -c $((6 * 1024 * 1024)) /dev/zero | tr '\0' 'a' >"$carpeta/seis_megas.csv"
    head -c $((11 * 1024 * 1024)) /dev/zero | tr '\0' 'a' >"$carpeta/once_megas.csv"
    # El mismo CSV en XLSX, de una hoja, con python3 y zipfile: sin binarios en el repositorio.
    python3 -I - "$carpeta/carreras.csv" "$carpeta/carreras.xlsx" <<'PY'
import csv, sys, zipfile
from xml.sax.saxutils import escape
filas = list(csv.reader(open(sys.argv[1], encoding='utf-8', newline='')))
def celda(c, f, v):
    return f'<c r="{chr(65 + c)}{f + 1}" t="inlineStr"><is><t>{escape(v)}</t></is></c>' if v else ''
hoja = ''.join(f'<row r="{f + 1}">' + ''.join(celda(c, f, v) for c, v in enumerate(fila)) + '</row>'
               for f, fila in enumerate(filas))
partes = {
    '[Content_Types].xml': '<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>',
    '_rels/.rels': '<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>',
    'xl/workbook.xml': '<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Carreras" sheetId="1" r:id="rId1"/></sheets></workbook>',
    'xl/_rels/workbook.xml.rels': '<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>',
    'xl/worksheets/sheet1.xml': '<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>' + hoja + '</sheetData></worksheet>',
}
with zipfile.ZipFile(sys.argv[2], 'w', zipfile.ZIP_DEFLATED) as libro:
    for nombre, contenido in partes.items():
        libro.writestr(nombre, contenido)
PY
}

# --- 1 a 3. Talento Humano: válidos e inválidos ---
validos() {
    subir "$GATEWAY" carreras "$carpeta/carreras.csv"
    es_lectura CSV 2 || return 1
    subir "$GATEWAY" carreras "$carpeta/carreras.xlsx"
    es_lectura XLSX 2
}

por_el_frontend() {
    subir "$FRONTEND" carreras "$carpeta/dos_megas.csv" carreras.csv
    es_lectura CSV 22000
}

punto_y_coma() {
    subir "$GATEWAY" carreras "$carpeta/punto_y_coma.csv" carreras.csv
    es_lectura CSV 2
}

columna_que_falta() {
    subir "$GATEWAY" carreras "$carpeta/sin_codigo.csv" carreras.csv
    es_codigo 400 CARGAS_ENCABEZADOS_DISTINTOS \
        && [[ $(jq -c '.detalles' <<<"$cuerpo") == '[{"campo":"codigo_carrera","descripcion":"Falta en el archivo."}]' ]]
}

invalidos() {
    local archivo codigo esperado
    while read -r archivo esperado codigo; do
        subir "$GATEWAY" carreras "$carpeta/$archivo"
        es_codigo "$esperado" "$codigo" || { echo "       $archivo: $estado $cuerpo" | cut -c1-200 >&2; return 1; }
    done <<'CASOS'
columna_de_mas.csv 400 CARGAS_ENCABEZADOS_DISTINTOS
windows.csv 400 CARGAS_CODIFICACION_NO_ADMITIDA
sin_registros.csv 400 CARGAS_ARCHIVO_SIN_REGISTROS
carreras.pdf 400 CARGAS_FORMATO_NO_ADMITIDO
seis_megas.csv 413 CARGAS_ARCHIVO_DEMASIADO_GRANDE
once_megas.csv 413 COMUN_CONTENIDO_DEMASIADO_GRANDE
CASOS
}

# --- 4 y 5. Quién sube (D-71) ---
sube() {  # sube <usuario>
    iniciar "$1"
    [[ -n $token ]] || return 1
    subir "$GATEWAY" carreras "$carpeta/carreras.csv"
    es_lectura CSV 2
}

no_sube() {  # no_sube <usuario>
    iniciar "$1"
    [[ -n $token ]] || return 1
    subir "$GATEWAY" carreras "$carpeta/carreras.csv"
    [[ $estado == 403 && $cuerpo == "$CUERPO_403" ]]
}

armar_archivos
# Las carreras se suben solo con alguna unidad orgánica activa (F01-H05).
iniciar admin.demo
if [[ -n $token ]]; then
    pedir GET "$GATEWAY/api/academico/unidades" "Bearer $token"
    if [[ -z $(jq -r '[.[] | select(.activa)][0].codigo // empty' <<<"$cuerpo" 2>/dev/null || true) ]]; then
        sede_creada="${prefijo}MATRIZ"
        pedir POST "$GATEWAY/api/academico/unidades" "Bearer $token" \
            "$(jq -cn --arg c "$sede_creada" '{codigo: $c, nombre: "Sede matriz temporal", tipo: "SEDE_MATRIZ"}')"
        [[ $estado == 201 ]] || echo "AVISO  No se pudo crear la sede temporal ($estado)"
    fi
fi
iniciar datos.demo
if [[ -z $token ]]; then
    echo "FALLA  0. datos.demo no inicia sesión: ¿está levantado el perfil?"
    exit 1
fi
verificar 1 "datos.demo sube el CSV y el XLSX de carreras: 2 registros en cada uno" validos
verificar 2 "Un CSV de unos 2 MB por el frontend (nginx) se lee: 22 000 registros" por_el_frontend
verificar 3 "Uno con punto y coma también: 2 registros" punto_y_coma
verificar 4 "Sin codigo_carrera: 400 CARGAS_ENCABEZADOS_DISTINTOS con su columna" columna_que_falta
verificar 5 "Columna de más, Windows-1252, sin registros, PDF, 6 MB y 11 MB: cada uno su código" invalidos
verificar 6 "consolidacion.demo sube (D-71)" sube consolidacion.demo
verificar 7 "vicerrectorado.demo sube (D-71)" sube vicerrectorado.demo
verificar 8 "coordinacion.demo (solo descarga) recibe 403" no_sube coordinacion.demo
verificar 9 "docente.demo recibe 403" no_sube docente.demo
iniciar datos.demo
subir "$GATEWAY" comisiones "$carpeta/carreras.csv"
verificar 10 "Una carga que no existe da 404 COMUN_RECURSO_NO_ENCONTRADO" es_codigo 404 COMUN_RECURSO_NO_ENCONTRADO

if ((fallas > 0)); then
    echo "Subida de archivos de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "Subida de archivos de punta a punta: todos los pasos pasan."
