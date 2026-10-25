#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Los cargos y los proyectos aprobados de punta a punta sobre el perfil levantado (F03-H05;
# quickstart.md de specs/023-cargos-proyectos).
# Uso, desde cualquier directorio: deploy/probar_cargos_proyectos.sh
# Registra con un prefijo al azar las unidades de la carga 01 que dirigen los cargos, las 7
# carreras de la carga 02 y los periodos 2026-I y 2026-II de la carga 03, y Talento Humano registra
# los 47 docentes de la carga 06, los 21 cargos de la carga 08 y los 11 proyectos de la carga 09
# (SC-002). Comprueba la advertencia de RN-20, los cuatro 409, la suma de horas que pasa las
# aprobadas, los proyectos asignables en cada periodo (SC-003), uno suspendido, un coordinador
# finalizado que deja su carrera libre, quién lee (D-64, D-70) y el registro de Docentes. Los datos
# de prueba son ficticios; si un docente de prueba ya existe en la base, el script falla sin tocarlo.
# Al terminar, aunque un paso falle, borra en la base solo lo que creó y sus registros, también la
# copia de los periodos en Roles: la API no borra (principio VII). Termina con 0 si todos los pasos
# pasan y con 1 si alguno falla. Nunca imprime el token ni la contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly DATOS=backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba
readonly CUERPO_403='{"titulo":"Acceso no autorizado","descripcion":"No tienes permiso para esta acción. Si crees que deberías tenerlo, pide al administrador que revise tus roles.","codigo":"SEGURIDAD_ACCESO_NO_AUTORIZADO"}'
# La meta del plan es 300 ms para listar los proyectos de un periodo; la prueba falla desde 500 ms.
readonly META_MS=300
readonly MAXIMO_MS=500
# Un docente de prueba a tiempo completo sin cargo, y uno de medio tiempo sin cuarto nivel.
readonly SIN_CARGO=0109990010
readonly MEDIO_TIEMPO=0109990424
# Las unidades de la carga 01 que dirigen los cargos: código|nombre|tipo|padre|gestión.
readonly UNIDADES='RECT|Rectorado|GOBIERNO|MATRIZ|G01
VRA|Vicerrectorado Académico|GOBIERNO|RECT|G02
CAI|Coordinación Académica Institucional|UNIDAD_ACADEMICA|VRA|
G-VIN|Gestión de Vinculación con la Sociedad|GESTION|VRA|G06
G-INV|Gestión de Investigación|GESTION|VRA|G07
G-CAL|Aseguramiento de la Calidad|GESTION|RECT|G10
G-SEC|Secretaría General Académica|GESTION|RECT|G11
G-BIE|Bienestar Institucional|GESTION|VRA|G13
G-PLA|Planificación Estratégica|GESTION|RECT|G15
G-BIB|Servicios de Biblioteca|GESTION|VRA|G17
G-TIC|Unidad de TIC|GESTION|VRA|G20
G-EST|Unidad de Estadísticas|GESTION|VRA|G21
COM-DIST|Comisión del distributivo|COMISION|VRA|'

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
cargos="$GATEWAY/api/docentes/cargos"
proyectos="$GATEWAY/api/docentes/proyectos"
primero=$(cod -2026-I)
segundo=$(cod -2026-II)

# Las cédulas que este script registró: solo esas se borran al terminar.
creados=()

limpiar() {
    local lista="''"
    if [[ ${#creados[@]} -gt 0 ]]; then
        lista=$(printf "'%s'," "${creados[@]}")
        lista=${lista%,}
    fi
    consultar_base "delete from docentes.registro_cambio where detalle ->> 'identificacion' in ($lista)
    or detalle ->> 'docente' in ($lista) or detalle ->> 'codigo' like '$prefijo%';
delete from docentes.cargo where docente in ($lista);
delete from docentes.proyecto where codigo like '$prefijo%';
delete from docentes.docente where identificacion in ($lista);
delete from roles.periodo_conocido where codigo like '$prefijo%';
delete from roles.registro_cambio where detalle ->> 'periodo' like '$prefijo%';
delete from roles.evento_recibido where id_evento in (select (serialized_event::jsonb ->> 'idEvento')::uuid
    from eventos_datos.event_publication where serialized_event::jsonb -> 'contenido' ->> 'codigo' like '$prefijo%');
delete from eventos_datos.event_publication where serialized_event::jsonb -> 'contenido' ->> 'codigo' like '$prefijo%';
delete from academico.registro_cambio where detalle ->> 'codigo' like '$prefijo%';
delete from academico.periodo_academico where codigo like '$prefijo%';
delete from academico.carrera where codigo like '$prefijo%';
delete from academico.unidad_organica where codigo like '$prefijo%' and tipo <> 'SEDE_MATRIZ';
delete from academico.unidad_organica where codigo like '$prefijo%';" >/dev/null 2>&1 \
        || echo "AVISO  No se pudo borrar lo creado con el prefijo $prefijo"
}
trap limpiar EXIT

# 0. Académico de prueba: las unidades, las carreras y los periodos, con el prefijo. Las unidades
# las registra el Administrador; las carreras y los periodos, el Vicerrector.
iniciar admin.demo
auth_admin="Bearer $token"
pedir GET "$GATEWAY/api/academico/unidades" "$auth_admin"
sede=$(jq -r '[.[] | select(.tipo == "SEDE_MATRIZ" and .activa) | .codigo][0] // empty' <<<"$cuerpo" 2>/dev/null || true)
if [[ -z $sede ]]; then
    sede=$(cod MATRIZ)
    pedir POST "$GATEWAY/api/academico/unidades" "$auth_admin" \
        "$(jq -cn --arg c "$sede" '{codigo: $c, nombre: "Sede matriz temporal", tipo: "SEDE_MATRIZ"}')"
fi
maestros=0
while IFS='|' read -r codigo nombre tipo padre gestion; do
    [[ $padre == MATRIZ ]] && padre_real=$sede || padre_real=$(cod "$padre")
    pedir POST "$GATEWAY/api/academico/unidades" "$auth_admin" \
        "$(jq -cn --arg c "$(cod "$codigo")" --arg n "$nombre" --arg t "$tipo" --arg p "$padre_real" --arg g "$gestion" \
            '{codigo: $c, nombre: $n, tipo: $t, padre: $p}
            + (if $g != "" then {gestionAutorizada: $g} else {} end)
            + (if $t == "COMISION" then {comisionPermanente: false} else {} end)')"
    [[ $estado == 201 ]] && maestros=$((maestros + 1))
done <<<"$UNIDADES"
iniciar vicerrectorado.demo
auth_vicerrector="Bearer $token"
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
while IFS=, read -r codigo nombre inicio fin _; do
    pedir POST "$GATEWAY/api/academico/periodos" "$auth_vicerrector" \
        "$(jq -cn --arg c "$(cod "-$codigo")" --arg n "$nombre" --arg i "$inicio" --arg f "$fin" \
            '{codigo: $c, nombre: $n, inicio: $i, fin: $f}')"
    [[ $estado == 201 ]] && maestros=$((maestros + 1))
done < <(filas 03_ciclos_academicos.csv)
iniciar datos.demo
auth="Bearer $token"
ya_existen=0
while IFS=, read -r _ cedula _; do
    pedir GET "$docentes/$cedula" "$auth"
    [[ $estado == 404 ]] || ya_existen=$((ya_existen + 1))
done < <(filas 06_docentes.csv)
datos_listos() { [[ $maestros == 22 && $ya_existen == 0 ]]; }
verificar 0 "Académico de prueba: $maestros de 22 (13 unidades, 7 carreras y 2 periodos, en la sede $sede); docentes de prueba que ya estaban: $ya_existen" \
    datos_listos
if [[ $ya_existen -gt 0 ]]; then
    echo "Cargos y proyectos de punta a punta: la base ya tiene docentes de prueba; no se tocan."
    exit 1
fi

# Los 47 docentes de la carga 06, con su carrera base con el prefijo (como probar_docentes.sh).
docente_de() {  # docente_de <fila de la carga 06>: el cuerpo de registrarDocente
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
                   asignaturasAfines: []}}
        | del(.. | nulls)'
}
while IFS= read -r fila; do
    pedir POST "$docentes" "$auth" "$(docente_de "$fila")"
    if [[ $estado == 201 ]]; then
        creados+=("$(cut -d, -f2 <<<"$fila")")
    else
        echo "       $(cut -d, -f2 <<<"$fila"): $estado $cuerpo"
    fi
done < <(filas 06_docentes.csv)
docentes_listos() { [[ ${#creados[@]} == 47 ]]; }
verificar 0 "datos.demo registra ${#creados[@]} de 47 docentes" docentes_listos

# 1. Los 21 cargos de la carga 08 (SC-002), con la unidad y la carrera con el prefijo
cargo_de() {  # cargo_de <fila de la carga 08>
    jq -cn --arg l "$1" --arg p "$prefijo" '
        def texto(v): if v == "" then null else v end;
        ($l | split(",")) as $c
        | {docente: $c[0], tipo: $c[1], fechaDesignacion: $c[4], documento: $c[6], estado: $c[7],
           unidad: (if $c[2] == "" then null else $p + $c[2] end),
           carrera: (if $c[3] == "" then null else $p + $c[3] end), fechaFin: texto($c[5])}
        | del(.. | nulls)'
}
registrados=0
vencidas=0
otras_advertencias=0
while IFS= read -r fila; do
    pedir POST "$cargos" "$auth" "$(cargo_de "$fila")"
    if [[ $estado == 201 ]]; then
        registrados=$((registrados + 1))
        vencidas=$((vencidas + $(jq '[.advertencias[] | select(.codigo == "FECHA_FIN_VENCIDA")] | length' <<<"$cuerpo")))
        otras_advertencias=$((otras_advertencias + $(jq '[.advertencias[] | select(.codigo != "FECHA_FIN_VENCIDA")] | length' <<<"$cuerpo")))
    else
        echo "       $(cut -d, -f1,2 <<<"$fila"): $estado $cuerpo"
    fi
done < <(filas 08_cargos_autoridades.csv)
cargos_registrados() { [[ $registrados == 21 && $otras_advertencias == 0 ]]; }
verificar 1 "datos.demo registra $registrados de 21 cargos, sin advertencias de requisitos (recibidas: $otras_advertencias)" \
    cargos_registrados
# Los 3 miembros de comisión de prueba terminan funciones el 2026-08-31: desde entonces, vencidos.
vencidas_esperadas=0
[[ $(date +%F) > 2026-08-31 ]] && vencidas_esperadas=3
fin_vencido() { [[ $vencidas == "$vencidas_esperadas" ]]; }
verificar 1 "Los miembros de comisión con su fin de funciones vencido traen su advertencia: $vencidas de $vencidas_esperadas" \
    fin_vencido

# 2. La advertencia de RN-20 (escenario «Requisitos del cargo»): un docente a tiempo completo sin
# títulos de cuarto nivel se registra como coordinador académico institucional
pedir GET "$docentes/$SIN_CARGO" "$auth"
datos_sin_cargo=$(jq -c 'del(.tipoIdentificacion, .identificacion, .nombreCompleto, .nombreDeCarreraBase, .estado, .camposAmplios, .asignaturasAfines) | .ficha.cuartoNivel = []' <<<"$cuerpo")
pedir PUT "$docentes/$SIN_CARGO" "$auth" "$datos_sin_cargo"
pedir POST "$cargos" "$auth" \
    "$(jq -cn --arg d "$SIN_CARGO" --arg u "$(cod CAI)" '{docente: $d, tipo: "COORDINADOR_ACADEMICO_INSTITUCIONAL", fechaDesignacion: "2026-04-01", unidad: $u, documento: "RES-PRUEBA-2026-030"}')"
advertencia=$(jq -r '[.advertencias[].descripcion] | join(" ")' <<<"$cuerpo" 2>/dev/null || true)
advierte_requisito() {
    [[ $estado == 201 && $advertencia == "Le falta un título de cuarto nivel registrado (RN-20)." ]]
}
verificar 2 "Un coordinador sin cuarto nivel: 201 con «${advertencia:-?}» (recibido: $estado)" advierte_requisito

# 3. Los conflictos: cargo repetido, segundo coordinador, docente no habilitado y proyecto repetido
fila_coordinador=$(filas 08_cargos_autoridades.csv | grep -m1 ',COORDINADOR_CARRERA,')
pedir POST "$cargos" "$auth" "$(cargo_de "$fila_coordinador")"
verificar 3 "El mismo cargo otra vez: 409 DOCENTES_CARGO_REPETIDO (recibido: $estado)" \
    es_codigo 409 DOCENTES_CARGO_REPETIDO
carrera_coordinada=$(cut -d, -f4 <<<"$fila_coordinador")
pedir POST "$cargos" "$auth" \
    "$(cargo_de "$fila_coordinador" | jq -c --arg d "$SIN_CARGO" '.docente = $d')"
verificar 3 "Un segundo coordinador vigente para $(cod "$carrera_coordinada"): 409 DOCENTES_COORDINADOR_VIGENTE (recibido: $estado)" \
    es_codigo 409 DOCENTES_COORDINADOR_VIGENTE
pedir POST "$cargos" "$auth" \
    "$(jq -cn --arg d "$MEDIO_TIEMPO" --arg u "$(cod G-INV)" '{docente: $d, tipo: "G07_GESTOR_INVESTIGACION", fechaDesignacion: "2026-04-01", unidad: $u, documento: "RES-1"}')"
sin_cedula() { es_codigo 409 DOCENTES_DOCENTE_NO_HABILITADO && [[ $cuerpo != *"$MEDIO_TIEMPO"* ]]; }
verificar 3 "Un cargo para un docente de medio tiempo: 409 DOCENTES_DOCENTE_NO_HABILITADO, sin su cédula (recibido: $estado)" \
    sin_cedula

# 4. Los 11 proyectos de la carga 09 (SC-002), con el código y las carreras con el prefijo
proyecto_de() {  # proyecto_de <fila de la carga 09>
    jq -cn --arg l "$1" --arg p "$prefijo" '
        def lista(v): v | split(";") | map(gsub("^ +| +$"; "")) | map(select(. != ""));
        ($l | split(",")) as $c
        | {codigo: ($p + $c[0]), nombre: $c[1], tipo: $c[2],
           carreras: [lista($c[3])[] | $p + .], resolucionOcs: $c[4], fechaResolucion: $c[5],
           estado: $c[6], vigenciaInicio: $c[7], vigenciaFin: $c[8], director: $c[9],
           participantes: [lista($c[10])[] | split("|") | {docente: .[0], rol: .[1], horas: (.[2] | tonumber)}],
           horasAprobadas: ($c[11] | tonumber)}'
}
proyectos_registrados=0
while IFS= read -r fila; do
    pedir POST "$proyectos" "$auth" "$(proyecto_de "$fila")"
    if [[ $estado == 201 ]]; then
        proyectos_registrados=$((proyectos_registrados + 1))
    else
        echo "       $(cut -d, -f1 <<<"$fila"): $estado $cuerpo"
    fi
done < <(filas 09_proyectos_practicas.csv)
todos_los_proyectos() { [[ $proyectos_registrados == 11 ]]; }
verificar 4 "datos.demo registra $proyectos_registrados de 11 proyectos" todos_los_proyectos
fila_inv=$(filas 09_proyectos_practicas.csv | grep -m1 '^INV-2025-01,')
pedir POST "$proyectos" "$auth" "$(proyecto_de "$fila_inv")"
verificar 4 "El mismo proyecto otra vez: 409 DOCENTES_PROYECTO_REPETIDO (recibido: $estado)" \
    es_codigo 409 DOCENTES_PROYECTO_REPETIDO
pedir POST "$proyectos" "$auth" \
    "$(proyecto_de "$fila_inv" | jq -c --arg c "$(cod -HORAS)" '.codigo = $c | .participantes[1].horas = 10.5')"
suma=$(jq -r '"\(.detalles[0].campo)|\(.detalles[0].descripcion)"' <<<"$cuerpo" 2>/dev/null || true)
pasa_las_aprobadas() { [[ $estado == 400 && $suma == "participantes|Las horas de los participantes suman 24.5 y pasan las 24 aprobadas" ]]; }
verificar 4 "Participantes que suman 24,5 contra 24 aprobadas: 400 en participantes (recibido: $estado ${suma:-?})" \
    pasa_las_aprobadas

# 5. Asignables en cada periodo (escenario «Proyecto no asignable», SC-003)
asignables() {  # asignables <periodo>: «asignables|por vigencia|por estado» de los proyectos de prueba
    pedir GET "$proyectos?periodo=$1" "$auth"
    jq -r --arg p "$prefijo" '[.[] | select(.codigo | startswith($p))] as $mios
        | "\([$mios[] | select(.asignable)] | length)|\([$mios[] | select(.motivos | index("VIGENCIA"))] | length)|\([$mios[] | select(.motivos | index("ESTADO"))] | length)"' \
        <<<"$cuerpo" 2>/dev/null || true
}
en_el_primero=$(asignables "$primero")
en_el_segundo=$(asignables "$segundo")
segun_el_periodo() { [[ $en_el_primero == "11|0|0" && $en_el_segundo == "4|7|0" ]]; }
verificar 5 "Asignables: en $primero 11 (recibido: $en_el_primero); en $segundo 4, y las 7 prácticas por su vigencia (recibido: $en_el_segundo)" \
    segun_el_periodo
pedir GET "$proyectos/$(cod INV-2025-01)" "$auth"
suspendido=$(jq -c '.estado = "SUSPENDIDO" | {nombre, tipo, carreras, resolucionOcs, fechaResolucion, estado, vigenciaInicio, vigenciaFin, director, participantes: [.participantes[] | {docente, rol, horas}], horasAprobadas}' <<<"$cuerpo")
pedir PUT "$proyectos/$(cod INV-2025-01)" "$auth" "$suspendido"
pedir GET "$proyectos/$(cod INV-2025-01)?periodo=$primero" "$auth"
motivo=$(jq -r '"\(.asignable)|\(.motivos | join(","))"' <<<"$cuerpo" 2>/dev/null || true)
por_su_estado() { [[ $motivo == "false|ESTADO" ]]; }
verificar 5 "$(cod INV-2025-01) suspendido no es asignable en $primero por su estado (recibido: ${motivo:-?})" \
    por_su_estado
pedir GET "$proyectos?periodo=$(cod -2030-I)" "$auth"
periodo_inexistente() { [[ $estado == 400 && $(jq -r '.detalles[0].campo' <<<"$cuerpo") == periodo ]]; }
verificar 5 "Un periodo que no existe: 400 en periodo (recibido: $estado)" periodo_inexistente

# 6. Finalizar un coordinador deja su carrera libre para otro (US5)
docente_coordinador=$(cut -d, -f1 <<<"$fila_coordinador")
pedir PUT "$cargos/$docente_coordinador/COORDINADOR_CARRERA/$(cut -d, -f5 <<<"$fila_coordinador")" "$auth" \
    "$(cargo_de "$fila_coordinador" | jq -c '{carrera, fechaFin: "2026-09-30", documento, estado: "FINALIZADO"}')"
finalizado=$(jq -r '.estado' <<<"$cuerpo" 2>/dev/null || true)
pedir POST "$cargos" "$auth" \
    "$(cargo_de "$fila_coordinador" | jq -c --arg d "$SIN_CARGO" '.docente = $d | .fechaDesignacion = "2026-10-01" | del(.fechaFin)')"
otro_coordinador=$estado
deja_libre() { [[ $finalizado == FINALIZADO && $otro_coordinador == 201 ]]; }
verificar 6 "Finalizar al coordinador de $(cod "$carrera_coordinada") deja su carrera para otro (recibido: ${finalizado:-?} y $otro_coordinador)" \
    deja_libre

# 7. Quién los ve (D-64, D-70): consolidación lee sin escribir; un gestor y un docente no leen
iniciar consolidacion.demo
pedir GET "$cargos" "Bearer $token"
lee_los_cargos() { [[ $estado == 200 ]]; }
verificar 7 "consolidacion.demo lee los cargos (recibido: $estado)" lee_los_cargos
pedir POST "$proyectos" "Bearer $token" "$(proyecto_de "$fila_inv")"
verificar 7 "consolidacion.demo registra un proyecto: 403 común (recibido: $estado)" es_403_comun
iniciar gestion.demo
pedir GET "$proyectos" "Bearer $token"
verificar 7 "gestion.demo, con «Proyectos: lectura» solo de lo propio, lista los proyectos: 403 común (recibido: $estado)" \
    es_403_comun
iniciar docente.demo
pedir GET "$cargos" "Bearer $token"
verificar 7 "docente.demo lista los cargos: 403 común (recibido: $estado)" es_403_comun

# 8. El tiempo de listar los proyectos de un periodo (meta del plan): el mejor de tres
mejor=""
for _ in 1 2 3; do
    segundos=$(curl -s -o /dev/null -w '%{time_total}' -H "$(printf 'Authorization: %s' "$auth")" "$proyectos?periodo=$primero" || echo 9)
    milis=$(awk -v s="$segundos" 'BEGIN { printf "%d", s * 1000 }')
    if [[ -z $mejor || $milis -lt $mejor ]]; then mejor=$milis; fi
done
rapida() { [[ $mejor -le $MAXIMO_MS ]]; }
verificar 8 "Listar los proyectos de $primero tarda ${mejor} ms (meta $META_MS ms; falla desde $MAXIMO_MS ms)" rapida

# 9. El registro de Docentes (FR-012)
lista=$(printf "'%s'," "${creados[@]}")
acciones=$(consultar_base "select accion || ':' || count(*) from docentes.registro_cambio
    where accion like 'CARGO%' and detalle ->> 'docente' in (${lista%,})
    or accion like 'PROYECTO%' and detalle ->> 'codigo' like '$prefijo%'
    group by accion order by accion" 2>/dev/null | paste -sd' ' || true)
registro_completo() {
    [[ $acciones == "CARGO_CORREGIDO:1 CARGO_REGISTRADO:23 PROYECTO_CORREGIDO:1 PROYECTO_REGISTRADO:11" ]]
}
verificar 9 "El registro tiene 23 cargos registrados, 1 corregido, 11 proyectos registrados y 1 corregido (recibido: ${acciones:-?})" \
    registro_completo

if [[ $fallas -eq 0 ]]; then
    echo "Cargos y proyectos de punta a punta: todos los pasos pasan."
    exit 0
fi
echo "Cargos y proyectos de punta a punta: $fallas paso(s) fallan."
exit 1
