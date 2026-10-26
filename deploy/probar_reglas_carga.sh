#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Las reglas de las filas y el informe de punta a punta sobre el perfil levantado (F01-H03;
# quickstart.md de specs/024-validar-reglas-carga).
# Uso, desde cualquier directorio: deploy/probar_reglas_carga.sh
# Registra una carrera con un prefijo al azar y arma en una carpeta temporal sus propios CSV, con
# docentes ficticios cuyas cédulas genera con su dígito verificador (RNF-SEG-06): asignaturas que
# citan esa carrera o una que no existe, docentes válidos, docentes con una cédula sin el cero
# inicial, una fecha imposible y una dedicación XX, uno con una celda =1+1 y uno de casi 5 MB. Los
# 10 archivos de prueba fijos se comprueban en ValidadorDeFilasTest (SC-001). Comprueba los errores
# en su fila y su campo, la sugerencia de la cédula, el CSV neutralizado, el reintento desde cero,
# quién valida (D-71) y el tiempo del archivo grande (SC-004); y que la validación no guarda nada.
# Al terminar, aunque un paso falle, borra en la base solo lo que creó y sus registros. Termina con
# 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime el token ni la contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly DATOS=backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba
readonly CUERPO_403='{"titulo":"Acceso no autorizado","descripcion":"No tienes permiso para esta acción. Si crees que deberías tenerlo, pide al administrador que revise tus roles.","codigo":"SEGURIDAD_ACCESO_NO_AUTORIZADO"}'
# La meta del plan es 10 s para un archivo de 5 MB; la prueba falla desde 30 s (SC-004).
readonly META_MS=10000
readonly MAXIMO_MS=30000

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
        echo "       última respuesta: $estado ${cuerpo:0:160}" | tr -d '\r'
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

# validar <carga> <archivo> [informe]: sube el archivo con el token de $token; con «informe», pide
# el CSV. Deja el estado, el cuerpo, los encabezados y los milisegundos.
validar() {
    local archivo cabeceras ruta="archivo"
    archivo=$(mktemp -p "$carpeta")
    cabeceras=$(mktemp -p "$carpeta")
    [[ ${3:-} == informe ]] && ruta="archivo/informe"
    local salida
    salida=$(curl -s -o "$archivo" -D "$cabeceras" -w '%{http_code} %{time_total}' -X POST \
        -H "Authorization: Bearer $token" \
        -F "archivo=@$2;filename=$(basename "$2")" \
        "$GATEWAY/api/cargas/plantillas/$1/$ruta" || echo '000 0')
    estado=${salida%% *}
    milisegundos=$(awk -v s="${salida##* }" 'BEGIN { printf "%d", s * 1000 }')
    cuerpo=$(cat "$archivo")
    encabezados=$(cat "$cabeceras")
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

# Cada código de prueba lleva este prefijo: P y tres dígitos, 4 caracteres.
prefijo="P$(printf '%03d' $((RANDOM % 1000)))"
carrera="${prefijo}-CR01"
sede_creada=""

limpiar() {
    consultar_base "delete from eventos_datos.event_publication where serialized_event::jsonb -> 'contenido' ->> 'codigo' like '$prefijo%';
delete from academico.registro_cambio where detalle ->> 'codigo' like '$prefijo%';
delete from academico.carrera where codigo like '$prefijo%';
delete from academico.unidad_organica where codigo like '$prefijo%';" >/dev/null 2>&1 \
        || echo "AVISO  No se pudo borrar lo creado con el prefijo $prefijo"
    rm -rf "$carpeta"
}
trap limpiar EXIT

# Cuántas filas tienen las tablas que una carga confirmada llenaría: no deben cambiar.
filas_guardadas() {
    consultar_base "select (select count(*) from academico.asignatura) || '/'
    || (select count(*) from docentes.docente) || '/' || (select count(*) from academico.carrera)"
}

# --- Los archivos, ficticios (RNF-SEG-06) ---
armar_archivos() {
    python3 -I - "$DATOS" "$carpeta" "$carrera" <<'PY'
import csv, io, sys
datos, carpeta, carrera = sys.argv[1:4]

def leer(nombre):
    with open(f'{datos}/{nombre}', encoding='utf-8-sig', newline='') as archivo:
        filas = list(csv.reader(archivo))
    return filas[0], filas[1:]

def escribir(nombre, encabezados, filas):
    salida = io.StringIO()
    csv.writer(salida, lineterminator='\r\n').writerows([encabezados, *filas])
    with open(f'{carpeta}/{nombre}', 'w', encoding='utf-8', newline='') as archivo:
        archivo.write(salida.getvalue())

def cedula(numero):
    """Una cédula ficticia de 10 dígitos que empieza con 09, con su dígito verificador."""
    base = f'09{numero:07d}'
    suma = 0
    for i, digito in enumerate(base):
        producto = int(digito) * (2 if i % 2 == 0 else 1)
        suma += producto - 9 if producto > 9 else producto
    return base + str((10 - suma % 10) % 10)

encabezados, asignaturas = leer('04_asignaturas_malla.csv')
a = encabezados.index
valida = list(asignaturas[0])
valida[a('codigo_carrera')] = carrera
valida[a('codigo_asignatura')] = carrera + '-A1'
otra = list(valida)
otra[a('codigo_carrera')] = carrera[:4] + '-NOEXISTE'
otra[a('codigo_asignatura')] = carrera + '-A2'
escribir('asignaturas.csv', encabezados, [valida])
escribir('asignaturas_con_error.csv', encabezados, [valida, otra])

encabezados, docentes = leer('06_docentes.csv')
d = encabezados.index
def docente(fila, numero):
    nueva = list(fila)
    nueva[d('numero_identificacion')] = cedula(numero)
    nueva[d('codigo_carrera_base')] = carrera
    nueva[d('asignaturas_afines_declaradas')] = ''
    return nueva
validos = [docente(docentes[i], 1000 + i) for i in range(3)]
escribir('docentes.csv', encabezados, validos)
con_errores = [list(fila) for fila in validos]
con_errores[0][d('numero_identificacion')] = validos[0][d('numero_identificacion')][1:]
con_errores[1][d('fecha_ingreso')] = '2026-02-30'
con_errores[2][d('dedicacion')] = 'XX'
escribir('docentes_con_errores.csv', encabezados, con_errores)
formula = [list(fila) for fila in validos]
formula[0][d('dedicacion')] = '=1+1'
escribir('docentes_formula.csv', encabezados, formula)
# Casi 5 MB, el máximo (en bytes de UTF-8): docentes válidos con cédulas distintas.
def tamano(fila):
    salida = io.StringIO()
    csv.writer(salida, lineterminator='\r\n').writerow(fila)
    return len(salida.getvalue().encode('utf-8'))
grandes, numero, total = [], 2000, tamano(encabezados)
while True:
    fila = docente(docentes[numero % len(docentes)], numero)
    if total + tamano(fila) > 5 * 1024 * 1024 - 1024:
        break
    total += tamano(fila)
    grandes.append(fila)
    numero += 1
escribir('docentes_grande.csv', encabezados, grandes)
print(validos[0][d('numero_identificacion')])
PY
}

# --- 0. Académico de prueba: la carrera, en la sede matriz ---
iniciar admin.demo
if [[ -z $token ]]; then
    echo "FALLA  0. admin.demo no inicia sesión: ¿está levantado el perfil?"
    exit 1
fi
auth_admin="Bearer $token"
pedir GET "$GATEWAY/api/academico/unidades" "$auth_admin"
sede=$(jq -r '[.[] | select(.tipo == "SEDE_MATRIZ" and .activa) | .codigo][0] // empty' <<<"$cuerpo" 2>/dev/null || true)
if [[ -z $sede ]]; then
    sede="${prefijo}MATRIZ"
    sede_creada=$sede
    pedir POST "$GATEWAY/api/academico/unidades" "$auth_admin" \
        "$(jq -cn --arg c "$sede" '{codigo: $c, nombre: "Sede matriz temporal", tipo: "SEDE_MATRIZ"}')"
fi
iniciar vicerrectorado.demo
pedir POST "$GATEWAY/api/academico/carreras" "Bearer $token" \
    "$(jq -cn --arg c "$carrera" --arg s "${prefijo}CR" --arg sede "$sede" \
        '{codigo: $c, nombre: "CARRERA DE PRUEBA DE LAS REGLAS", siglas: $s, sede: $sede,
          nivelFormacion: "TECNOLOGICO_SUPERIOR", modalidad: "PRESENCIAL", jornadas: ["NOCTURNA"],
          campoAmplio: "06", campoEspecifico: "061", numeroCiclos: 5}')"
carrera_registrada=$estado
cedula_sin_cero=$(armar_archivos)
cedula_sin_cero=${cedula_sin_cero:1}
antes=$(filas_guardadas)
listo() { [[ $carrera_registrada == 201 && -s $carpeta/docentes_grande.csv ]]; }
verificar 0 "La carrera de prueba $carrera${sede_creada:+ (en una sede temporal)} y los CSV ficticios" listo

iniciar datos.demo

es_valido() {  # es_valido <registros>
    [[ $estado == 200 && $(jq -r '.valido' <<<"$cuerpo") == true \
        && $(jq -r '.errores' <<<"$cuerpo") == 0 && $(jq -r '.registros' <<<"$cuerpo") == "$1" ]]
}

asignaturas_validas() {
    validar asignaturas_malla "$carpeta/asignaturas.csv"
    es_valido 1
}

carrera_que_no_existe() {
    validar asignaturas_malla "$carpeta/asignaturas_con_error.csv"
    [[ $estado == 200 && $(jq -r '.valido' <<<"$cuerpo") == false \
        && $(jq -c '[.problemas[] | [.fila, .campo, .motivo]]' <<<"$cuerpo") \
            == '[[3,"codigo_carrera","No existe la carrera en el sistema: regístrela o cárguela antes."]]' ]]
}

docentes_validos() {
    validar docentes "$carpeta/docentes.csv"
    es_valido 3
}

tres_errores() {
    validar docentes "$carpeta/docentes_con_errores.csv"
    [[ $estado == 200 && $(jq -r '.errores' <<<"$cuerpo") == 3 \
        && $(jq -c '[.problemas[] | [.fila, .campo]]' <<<"$cuerpo") \
            == '[[2,"numero_identificacion"],[3,"fecha_ingreso"],[4,"dedicacion"]]' \
        && $(jq -r '.problemas[0].motivo' <<<"$cuerpo") == 'Cédula inválida o sin cero inicial: le falta el 0 inicial.' \
        && $(jq -r '.problemas[0].sugerencia' <<<"$cuerpo") == "0$cedula_sin_cero" ]]
}

informe_neutralizado() {
    validar docentes "$carpeta/docentes_formula.csv" informe
    [[ $estado == 200 ]] \
        && grep -qi '^content-type: text/csv' <<<"$encabezados" \
        && grep -qi 'filename="informe-docentes.csv"' <<<"$encabezados" \
        && [[ $(head -1 <<<"$cuerpo" | tr -d '\r\357\273\277') == 'fila,campo,valor,motivo,tipo,sugerencia' ]] \
        && grep -q "^2,dedicacion,'=1+1," <<<"$cuerpo" \
        && ! grep -q ',=1+1' <<<"$cuerpo"
}

corregir_y_reintentar() {
    validar docentes "$carpeta/docentes_con_errores.csv"
    [[ $(jq -r '.valido' <<<"$cuerpo") == false ]] || return 1
    validar docentes "$carpeta/docentes.csv"
    es_valido 3 && [[ $(jq -c '.problemas' <<<"$cuerpo") == '[]' ]]
}

valida() {  # valida <usuario>
    iniciar "$1"
    [[ -n $token ]] || return 1
    validar docentes "$carpeta/docentes.csv"
    es_valido 3
}

no_valida() {  # no_valida <usuario>
    iniciar "$1"
    [[ -n $token ]] || return 1
    validar docentes "$carpeta/docentes.csv"
    [[ $estado == 403 && $cuerpo == "$CUERPO_403" ]] || return 1
    validar docentes "$carpeta/docentes.csv" informe
    [[ $estado == 403 && $cuerpo == "$CUERPO_403" ]]
}

archivo_grande() {
    iniciar datos.demo
    local registros
    registros=$(($(wc -l <"$carpeta/docentes_grande.csv") - 1))
    validar docentes "$carpeta/docentes_grande.csv"
    echo "       $(stat -c %s "$carpeta/docentes_grande.csv") bytes y $registros docentes en $milisegundos ms (meta $META_MS ms; falla desde $MAXIMO_MS ms)"
    es_valido "$registros" && ((milisegundos < MAXIMO_MS))
}

nada_guardado() {
    [[ $(filas_guardadas) == "$antes" ]]
}

verificar 1 "Una asignatura que cita la carrera registrada: válida" asignaturas_validas
verificar 2 "Otra que cita una carrera que no existe: el error en la fila 3 y en codigo_carrera" carrera_que_no_existe
verificar 3 "Tres docentes ficticios: válidos" docentes_validos
verificar 4 "Cédula sin el cero, fecha imposible y dedicación XX: 3 errores en su fila y su campo, con la cédula sugerida" tres_errores
verificar 5 "descargarInforme: text/csv, informe-docentes.csv y la celda =1+1 neutralizada" informe_neutralizado
verificar 6 "El archivo rechazado y después el corregido: se valida desde cero" corregir_y_reintentar
verificar 7 "consolidacion.demo valida (D-71)" valida consolidacion.demo
verificar 8 "docente.demo recibe 403 en las dos operaciones" no_valida docente.demo
verificar 9 "Un archivo de casi 5 MB se valida en menos de $((MAXIMO_MS / 1000)) s (SC-004)" archivo_grande
verificar 10 "La validación no guardó nada: asignaturas, docentes y carreras iguales" nada_guardado

if ((fallas > 0)); then
    echo "Reglas de las filas de punta a punta: $fallas paso(s) fallan."
    exit 1
fi
echo "Reglas de las filas de punta a punta: todos los pasos pasan."
