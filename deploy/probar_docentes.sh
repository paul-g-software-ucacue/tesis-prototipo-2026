#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Los docentes con su ficha académica de punta a punta sobre el perfil levantado (F03-H04;
# quickstart.md de specs/022-docentes-ficha).
# Uso, desde cualquier directorio: deploy/probar_docentes.sh
# Registra con un prefijo al azar las 7 carreras de la carga 02 de prueba y una asignatura, y
# Talento Humano registra los 47 docentes de la carga 06: cada uno se encuentra por su cédula. La
# búsqueda no distingue mayúsculas ni tildes; la ficha trae los campos y las horas; una dedicación
# que no existe dice los valores permitidos; una identificación o un correo repetidos dan 409; un
# docente pasa a licencia y vuelve. El Responsable de consolidación lee sin poder escribir y un
# docente no lee (D-70). El registro de Docentes tiene las acciones. Los docentes de prueba son
# ficticios; si alguno ya existe en la base, el script falla sin tocarlo. Al terminar, aunque un paso
# falle, borra en la base solo lo que creó y sus registros: la API no borra (principio VII).
# Termina con 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime el token ni la
# contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly DATOS=backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba
readonly CUERPO_403='{"titulo":"Acceso no autorizado","descripcion":"No tienes permiso para esta acción. Si crees que deberías tenerlo, pide al administrador que revise tus roles.","codigo":"SEGURIDAD_ACCESO_NO_AUTORIZADO"}'
# La meta del plan es 300 ms para buscar entre los docentes; la prueba falla desde 500 ms (holgura).
readonly META_MS=300
readonly MAXIMO_MS=500
# Una cédula ficticia válida que no está en la carga 06, para el correo repetido.
readonly OTRA_CEDULA=0109999904

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
        fallas=$((fallas + 1))
    fi
}

# Deja el estado en $estado y el cuerpo en $cuerpo.
pedir() {  # pedir <método> <url> [autorización] [cuerpo json]
    local archivo
    archivo=$(mktemp)
    local argumentos=(-s -o "$archivo" -w '%{http_code}' -X "$1" "$2")
    [[ -n ${3:-} ]] && argumentos+=(-H "Authorization: $3")
    [[ -n ${4:-} ]] && argumentos+=(-H 'Content-Type: application/json' --data "$4")
    estado=$(curl "${argumentos[@]}" || echo '000')
    cuerpo=$(cat "$archivo")
    rm -f "$archivo"
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

es_403_comun() {
    [[ $estado == 403 && $cuerpo == "$CUERPO_403" ]]
}

es_codigo() {  # es_codigo <estado> <código del catálogo>
    [[ $estado == "$1" && $(jq -r '.codigo' <<<"$cuerpo" 2>/dev/null) == "$2" ]]
}

# Las filas de una carga sin el encabezado ni el retorno de carro (los CSV vienen con CRLF).
filas() {
    tail -n +2 "$DATOS/$1" | tr -d '\r'
}

# Cada código de prueba lleva este prefijo: P y tres dígitos, 4 caracteres.
prefijo="P$(printf '%03d' $((RANDOM % 1000)))"
cod() { printf '%s%s' "$prefijo" "$1"; }
docentes="$GATEWAY/api/docentes/docentes"
readonly MD=DS-B-I-MD-101  # la asignatura afín de prueba

# Las cédulas que este script registró: solo esas se borran al terminar.
creados=()

limpiar() {
    local lista=""
    if [[ ${#creados[@]} -gt 0 ]]; then
        lista=$(printf "'%s'," "${creados[@]}")
        lista=${lista%,}
    fi
    consultar_base "${lista:+delete from docentes.registro_cambio where detalle ->> 'identificacion' in ($lista);
delete from docentes.docente where identificacion in ($lista);}
delete from academico.registro_cambio where detalle ->> 'codigo' like '$prefijo%';
delete from academico.asignatura where codigo like '$prefijo%';
delete from academico.malla where codigo like '$prefijo%';
delete from academico.carrera where codigo like '$prefijo%';
delete from academico.unidad_organica where codigo like '$prefijo%';" >/dev/null 2>&1 \
        || echo "AVISO  No se pudo borrar lo creado con el prefijo $prefijo"
}
trap limpiar EXIT

# La sede matriz que ya tenga la base; si no hay, una temporal, que registra el Administrador.
iniciar admin.demo
token_admin=$token
pedir GET "$GATEWAY/api/academico/unidades" "Bearer $token_admin"
sede=$(jq -r '[.[] | select(.tipo == "SEDE_MATRIZ" and .activa) | .codigo][0] // empty' <<<"$cuerpo" 2>/dev/null || true)
if [[ -z $sede ]]; then
    sede=$(cod MATRIZ)
    pedir POST "$GATEWAY/api/academico/unidades" "Bearer $token_admin" \
        "$(jq -cn --arg c "$sede" '{codigo: $c, nombre: "Sede matriz temporal", tipo: "SEDE_MATRIZ"}')"
fi

# 0. Los datos maestros de prueba: las 7 carreras de la carga 02 y una asignatura con su malla, que
# registra el Vicerrector (Talento Humano no escribe en Académico). Ninguno de los 47 docentes existe.
iniciar vicerrectorado.demo
auth_vicerrector="Bearer $token"
maestros=0
while IFS=, read -r codigo nombre siglas _ nivel modalidad jornadas amplio especifico detallado ciclos _; do
    pedir POST "$GATEWAY/api/academico/carreras" "$auth_vicerrector" \
        "$(jq -cn --arg c "$(cod "$codigo")" --arg n "$nombre" --arg s "$(cod "$siglas")" --arg sede "$sede" \
            --arg nivel "$nivel" --arg m "$modalidad" --arg j "$jornadas" --arg a "$amplio" \
            --arg e "$especifico" --arg d "$detallado" --arg ciclos "$ciclos" \
            '{codigo: $c, nombre: $n, siglas: $s, sede: $sede, nivelFormacion: $nivel, modalidad: $m,
              jornadas: ($j | split(";") | map(gsub("^ +| +$"; ""))), campoAmplio: $a,
              campoEspecifico: $e, numeroCiclos: ($ciclos | tonumber)}
            + (if $d != "" then {campoDetallado: $d} else {} end)')"
    [[ $estado == 201 ]] && maestros=$((maestros + 1))
done < <(filas 02_carreras.csv)
IFS=, read -r carrera_md _ nombre_md ciclo_md unidad_md clase_md _ _ _ amplio_md especifico_md palabras_md malla_md _ \
    < <(filas 04_asignaturas_malla.csv | grep ",$MD,")
pedir POST "$GATEWAY/api/academico/carreras/$(cod "$carrera_md")/mallas" "$auth_vicerrector" \
    "$(jq -cn --arg c "$(cod "$malla_md")" '{codigo: $c}')"
[[ $estado == 201 ]] && maestros=$((maestros + 1))
pedir POST "$GATEWAY/api/academico/asignaturas" "$auth_vicerrector" \
    "$(jq -cn --arg c "$(cod "$MD")" --arg car "$(cod "$carrera_md")" --arg m "$(cod "$malla_md")" \
        --arg n "$nombre_md" --arg ciclo "$ciclo_md" --arg u "$unidad_md" --arg h "$clase_md" \
        --arg a "$amplio_md" --arg e "$especifico_md" --arg k "$palabras_md" \
        '{codigo: $c, carrera: $car, malla: $m, nombre: $n, ciclo: ($ciclo | tonumber),
          unidadOrganizacionCurricular: $u, horasSemanalesClase: ($h | tonumber), troncoComun: false,
          campoAmplio: $a, campoEspecifico: $e,
          palabrasClave: ($k | split(";") | map(gsub("^ +| +$"; "")) | map(select(. != "")))}')"
[[ $estado == 201 ]] && maestros=$((maestros + 1))
iniciar datos.demo
auth="Bearer $token"
ya_existen=0
while IFS=, read -r _ cedula _; do
    pedir GET "$docentes/$cedula" "$auth"
    [[ $estado == 404 ]] || ya_existen=$((ya_existen + 1))
done < <(filas 06_docentes.csv)
pedir GET "$docentes/$OTRA_CEDULA" "$auth"
[[ $estado == 404 ]] || ya_existen=$((ya_existen + 1))
datos_listos() { [[ $maestros == 9 && $ya_existen == 0 ]]; }
verificar 0 "Datos de prueba: $maestros de 9 (7 carreras, 1 malla y $MD, en la sede $sede); docentes de prueba que ya estaban: $ya_existen" \
    datos_listos
if [[ $ya_existen -gt 0 ]]; then
    echo "Docentes de punta a punta: la base ya tiene docentes de prueba; no se tocan."
    exit 1
fi

# 1. Los 47 docentes de la carga 06 (SC-002), cada uno con su ficha
docente_de() {  # docente_de <fila de la carga 06>: el cuerpo de registrarDocente, con la carrera con prefijo
    jq -cn --arg l "$1" --arg p "$prefijo" '
        def texto(v): if v == "" then null else v end;
        def lista(v): v | split(";") | map(gsub("^ +| +$"; "")) | map(select(. != "")) | map(split("|"));
        ($l | split(",")) as $c
        | {tipoIdentificacion: $c[0], identificacion: $c[1], primerApellido: $c[2],
           segundoApellido: texto($c[3]), primerNombre: $c[4], segundoNombre: texto($c[5]),
           correoInstitucional: $c[6], telefono: texto($c[7]), tipoPersonal: $c[8],
           categoriaEscalafon: $c[9], dedicacion: $c[10], horasContratadas: ($c[11] | tonumber),
           fechaIngreso: $c[12], inicioContrato: $c[13], finContrato: texto($c[14]),
           condicionEspecial: $c[15], carreraBase: ($p + $c[16]),
           ficha: {tercerNivel: {titulo: $c[17], registroSenescyt: texto($c[18]),
                                 campoAmplio: $c[19], campoEspecifico: $c[20]},
                   cuartoNivel: [lista($c[21])[] | {titulo: .[0], tipo: .[1], campoEspecifico: .[2], anio: (.[3] | tonumber)}],
                   cursos: [lista($c[22])[] | {nombre: .[0], campoEspecifico: .[1], horas: (.[2] | tonumber), anio: (.[3] | tonumber)}],
                   experiencia: [lista($c[23])[] | {cargo: .[0], institucion: .[1], campoEspecifico: .[2], anios: (.[3] | tonumber)}],
                   asignaturasAfines: [lista($c[24])[][0]]}}
        | del(.. | nulls)'
}
registrados=0
while IFS= read -r fila; do
    pedir POST "$docentes" "$auth" "$(docente_de "$fila")"
    if [[ $estado == 201 ]]; then
        registrados=$((registrados + 1))
        creados+=("$(cut -d, -f2 <<<"$fila")")
    else
        echo "       $(cut -d, -f2 <<<"$fila"): $estado $cuerpo"
    fi
done < <(filas 06_docentes.csv)
todos_registrados() { [[ $registrados == 47 ]]; }
verificar 1 "datos.demo registra $registrados de 47 docentes con su ficha" todos_registrados
encontrados=0
for cedula in "${creados[@]}"; do
    pedir GET "$docentes?texto=$cedula" "$auth"
    [[ $(jq -r --arg c "$cedula" '[.[] | select(.identificacion == $c)] | length' <<<"$cuerpo" 2>/dev/null) == 1 ]] \
        && encontrados=$((encontrados + 1))
done
por_cedula() { [[ $encontrados == 47 ]]; }
verificar 1 "Cada uno se encuentra por su cédula: $encontrados de 47" por_cedula

# 2. La búsqueda sin mayúsculas ni tildes, y por carrera base (escenario «Consulta»)
pedir GET "$docentes?texto=PERALT%C3%81" "$auth"
peraltas=$(jq -r '[.[].nombreCompleto] | join(" | ")' <<<"$cuerpo" 2>/dev/null || true)
encuentra_peralta() { [[ $peraltas == "PERALTA "*" | PERALTA "* ]]; }
verificar 2 "«PERALTÁ» encuentra a los dos PERALTA (recibido: ${peraltas:-?})" encuentra_peralta
pedir GET "$docentes?carrera=$(cod 550613A01-P-0301)" "$auth"
de_software=$(jq -r 'length' <<<"$cuerpo" 2>/dev/null || true)
esperados=$(filas 06_docentes.csv | cut -d, -f17 | grep -c '^550613A01-P-0301$' || true)
por_carrera() { [[ $de_software == "$esperados" && $esperados -gt 0 ]]; }
verificar 2 "Filtrar por la carrera 550613A01-P-0301 trae sus $esperados docentes (recibido: ${de_software:-?})" por_carrera

# 3. La ficha: cada título con su campo amplio y específico, y cada curso con sus horas
pedir GET "$docentes/0109990010" "$auth"
ficha=$(jq -r '"\(.ficha.tercerNivel.campoAmplio)|\(.ficha.tercerNivel.campoEspecifico)|\([.camposAmplios[].nombre] | all(. != ""))|\([.ficha.cursos[].horas] | all(. > 0))|\(.ficha.cuartoNivel | length)|\(.nombreDeCarreraBase != null)"' \
    <<<"$cuerpo" 2>/dev/null || true)
con_ficha() { [[ $estado == 200 && $ficha == 04\|041\|true\|true\|1\|true ]]; }
verificar 3 "La ficha de 0109990010 trae su tercer nivel 04/041, los nombres de sus campos, las horas de sus cursos, su maestría y su carrera (recibido: $estado ${ficha:-?})" \
    con_ficha

# 4. Corregir: la dedicación, una asignatura afín y una dedicación que no existe
pedir GET "$docentes/0109990010" "$auth"
datos_actuales=$(jq -c 'del(.tipoIdentificacion, .identificacion, .nombreCompleto, .nombreDeCarreraBase, .estado, .camposAmplios, .asignaturasAfines)' <<<"$cuerpo")
pedir PUT "$docentes/0109990010" "$auth" \
    "$(jq -c --arg md "$(cod "$MD")" '.dedicacion = "MT" | .horasContratadas = 20 | .ficha.asignaturasAfines = [$md]' <<<"$datos_actuales")"
corregido=$(jq -r '"\(.dedicacion)|\(.horasContratadas)|\(.asignaturasAfines[0].nombre != null)"' <<<"$cuerpo" 2>/dev/null || true)
se_corrige() { [[ $estado == 200 && $corregido == "MT|20|true" ]]; }
verificar 4 "Corregir a medio tiempo con 20 horas y $MD como afín: 200 (recibido: $estado ${corregido:-?})" se_corrige
pedir PUT "$docentes/0109990010" "$auth" "$(jq -c '.dedicacion = "XX"' <<<"$datos_actuales")"
permitidos=$(jq -r '"\(.codigo)|\(.detalles[0].campo)|\(.detalles[0].descripcion)"' <<<"$cuerpo" 2>/dev/null || true)
dice_permitidos() {
    [[ $estado == 400 && $permitidos == "COMUN_SOLICITUD_INVALIDA|dedicacion|no es un valor permitido: usa TC, MT o TP" ]]
}
verificar 4 "Una dedicación «XX»: 400 que dice los valores permitidos (recibido: $estado ${permitidos:-?})" dice_permitidos
pedir PUT "$docentes/0109990010" "$auth" \
    "$(jq -c '.ficha.asignaturasAfines = ["NO-EXISTE"]' <<<"$datos_actuales")"
afin=$(jq -r '.detalles[0].campo' <<<"$cuerpo" 2>/dev/null || true)
afin_inexistente() { [[ $estado == 400 && $afin == "ficha.asignaturasAfines[0]" ]]; }
verificar 4 "Una asignatura afín que no existe: 400 en ficha.asignaturasAfines[0] (recibido: $estado ${afin:-?})" afin_inexistente

# 5. La identificación y el correo repetidos
fila_peralta=$(filas 06_docentes.csv | grep -m1 '^CEDULA,0109990010,')
pedir POST "$docentes" "$auth" "$(docente_de "$fila_peralta")"
verificar 5 "Registrar 0109990010 otra vez: 409 DOCENTES_DOCENTE_REPETIDO (recibido: $estado)" \
    es_codigo 409 DOCENTES_DOCENTE_REPETIDO
pedir POST "$docentes" "$auth" \
    "$(docente_de "$fila_peralta" | jq -c --arg c "$OTRA_CEDULA" '.identificacion = $c | .correoInstitucional |= ascii_upcase')"
verificar 5 "Otro docente con el correo de 0109990010 en mayúsculas: 409 DOCENTES_CORREO_REPETIDO (recibido: $estado)" \
    es_codigo 409 DOCENTES_CORREO_REPETIDO
[[ $estado == 201 ]] && creados+=("$OTRA_CEDULA")

# 6. El estado laboral: licencia y de vuelta a activo
pedir PUT "$docentes/0109990010/estado" "$auth" '{"estado":"LICENCIA"}'
en_licencia=$(jq -r '.estado' <<<"$cuerpo" 2>/dev/null || true)
pedir PUT "$docentes/0109990010/estado" "$auth" '{"estado":"ACTIVO"}'
de_vuelta=$(jq -r '.estado' <<<"$cuerpo" 2>/dev/null || true)
ida_y_vuelta() { [[ $en_licencia == LICENCIA && $de_vuelta == ACTIVO ]]; }
verificar 6 "0109990010 pasa a licencia y vuelve a activo (recibido: ${en_licencia:-?} y ${de_vuelta:-?})" ida_y_vuelta

# 7. Quién los ve (D-70): consolidación lee sin escribir; un docente no lee
iniciar consolidacion.demo
pedir GET "$docentes/0109990010" "Bearer $token"
lee_la_ficha() { [[ $estado == 200 ]]; }
verificar 7 "consolidacion.demo lee la ficha (recibido: $estado)" lee_la_ficha
pedir PUT "$docentes/0109990010/estado" "Bearer $token" '{"estado":"INACTIVO"}'
verificar 7 "consolidacion.demo cambia el estado: 403 común (recibido: $estado)" es_403_comun
iniciar docente.demo
pedir GET "$docentes" "Bearer $token"
verificar 7 "docente.demo busca docentes: 403 común (recibido: $estado)" es_403_comun

# 8. El tiempo de la búsqueda (meta del plan): el mejor de tres
mejor=""
for _ in 1 2 3; do
    segundos=$(curl -s -o /dev/null -w '%{time_total}' -H "$(printf 'Authorization: %s' "$auth")" "$docentes?texto=peralta" || echo 9)
    milis=$(awk -v s="$segundos" 'BEGIN { printf "%d", s * 1000 }')
    if [[ -z $mejor || $milis -lt $mejor ]]; then mejor=$milis; fi
done
rapida() { [[ $mejor -le $MAXIMO_MS ]]; }
verificar 8 "Buscar entre los docentes tarda ${mejor} ms (meta $META_MS ms; falla desde $MAXIMO_MS ms)" rapida

# 9. El registro de Docentes (FR-010)
lista=$(printf "'%s'," "${creados[@]}")
acciones=$(consultar_base "select accion || ':' || usuario || ':' || count(*) from docentes.registro_cambio
    where detalle ->> 'identificacion' in (${lista%,}) group by accion, usuario order by accion" 2>/dev/null \
    | paste -sd' ' || true)
registro_completo() {
    [[ $acciones == "DOCENTE_CORREGIDO:datos.demo:1 DOCENTE_ESTADO_CAMBIADO:datos.demo:2 DOCENTE_REGISTRADO:datos.demo:47" ]]
}
verificar 9 "El registro tiene 47 docentes registrados, 1 corrección y 2 cambios de estado por datos.demo (recibido: ${acciones:-?})" \
    registro_completo

if [[ $fallas -eq 0 ]]; then
    echo "Docentes de punta a punta: todos los pasos pasan."
    exit 0
fi
echo "Docentes de punta a punta: $fallas paso(s) fallan."
exit 1
