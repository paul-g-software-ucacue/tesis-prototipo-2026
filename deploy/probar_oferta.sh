#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# La oferta del periodo de punta a punta sobre el perfil levantado (F03-H03; quickstart.md de
# specs/021-oferta-periodo, research §9).
# Uso, desde cualquier directorio: deploy/probar_oferta.sh
# Registra con un prefijo al azar las carreras, mallas y asignaturas de las cargas 02 y 04 de prueba
# y un periodo en Planificación, y el Vicerrector oferta las 165 asignaturas-paralelo de la carga 05:
# suman 660 horas a cubrir, con el total de cada carrera. Una asignatura de 4 horas en 3 paralelos
# suma 12, y con uno retirado 8. Una repetida, unas horas de más, una jornada que no es de la
# carrera o una asignatura inactiva se rechazan; con el periodo fuera de Planificación, la oferta no
# cambia. El Responsable de consolidación lee sin poder escribir y un docente no lee (D-70). El
# registro de Académico tiene las acciones. Al terminar, aunque un paso falle, borra en la base solo
# lo que creó y sus registros: la API no borra (principio VII).
# Termina con 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime el token ni la
# contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly DATOS=backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba
readonly CUERPO_403='{"titulo":"Acceso no autorizado","descripcion":"No tienes permiso para esta acción. Si crees que deberías tenerlo, pide al administrador que revise tus roles.","codigo":"SEGURIDAD_ACCESO_NO_AUTORIZADO"}'
# La meta del plan para consultar la oferta de un periodo con 165 asignaturas-paralelo.
readonly MAXIMO_MS=300

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

es_invalido_en() {  # es_invalido_en <campo>: 400 con un solo detalle, en ese campo
    [[ $estado == 400 ]] \
        && [[ $(jq -r '.codigo' <<<"$cuerpo" 2>/dev/null) == COMUN_SOLICITUD_INVALIDA ]] \
        && [[ $(jq -r '[.detalles[].campo] | join(",")' <<<"$cuerpo") == "$1" ]]
}

# Las filas de una carga sin el encabezado ni el retorno de carro (los CSV vienen con CRLF).
filas() {
    tail -n +2 "$DATOS/$1" | tr -d '\r'
}

# Cada código de prueba lleva este prefijo: P y tres dígitos, 4 caracteres.
prefijo="P$(printf '%03d' $((RANDOM % 1000)))"
cod() { printf '%s%s' "$prefijo" "$1"; }
periodo="$prefijo-2026-I"
oferta="$GATEWAY/api/academico/periodos/$periodo/oferta"

limpiar() {
    consultar_base "delete from academico.asignatura_paralelo where periodo = '$periodo';
delete from academico.registro_cambio where detalle ->> 'codigo' like '$prefijo%'
    or detalle ->> 'periodo' = '$periodo';
delete from academico.asignatura where codigo like '$prefijo%';
delete from academico.malla where codigo like '$prefijo%';
delete from academico.carrera where codigo like '$prefijo%';
delete from academico.unidad_organica where codigo like '$prefijo%';
delete from roles.designacion_responsable where periodo = '$periodo';
delete from roles.periodo_conocido where codigo = '$periodo';
delete from roles.registro_cambio where detalle ->> 'periodo' = '$periodo';
delete from roles.evento_recibido where id_evento in (select (serialized_event::jsonb ->> 'idEvento')::uuid
    from eventos_datos.event_publication where serialized_event::jsonb -> 'contenido' ->> 'codigo' = '$periodo');
delete from eventos_datos.event_publication where serialized_event::jsonb -> 'contenido' ->> 'codigo' = '$periodo';
delete from academico.periodo_academico where codigo = '$periodo';" >/dev/null 2>&1 \
        || echo "AVISO  No se pudo borrar lo creado con el prefijo $prefijo"
}
trap limpiar EXIT

# La sede matriz que ya tenga la base; si no hay, una temporal, que registra el Administrador.
iniciar admin.demo
token_admin=$token
pedir GET "$GATEWAY/api/academico/unidades" "Bearer $token_admin"
sede=$(jq -r '[.[] | select(.tipo == "SEDE_MATRIZ" and .activa) | .codigo][0] // empty' <<<"$cuerpo" 2>/dev/null || true)
sede_temporal=""
if [[ -z $sede ]]; then
    sede_temporal=$(cod MATRIZ)
    pedir POST "$GATEWAY/api/academico/unidades" "Bearer $token_admin" \
        "$(jq -cn --arg c "$sede_temporal" '{codigo: $c, nombre: "Sede matriz temporal", tipo: "SEDE_MATRIZ"}')"
    sede=$sede_temporal
fi

iniciar vicerrectorado.demo
token_vicerrector=$token
auth="Bearer $token_vicerrector"

# 0. Los datos maestros de prueba: 7 carreras, 12 mallas, 300 asignaturas y el periodo
maestros=0
while IFS=, read -r codigo nombre siglas _ nivel modalidad jornadas amplio especifico detallado ciclos _; do
    pedir POST "$GATEWAY/api/academico/carreras" "$auth" \
        "$(jq -cn --arg c "$(cod "$codigo")" --arg n "$nombre" --arg s "$(cod "$siglas")" --arg sede "$sede" \
            --arg nivel "$nivel" --arg m "$modalidad" --arg j "$jornadas" --arg a "$amplio" \
            --arg e "$especifico" --arg d "$detallado" --arg ciclos "$ciclos" \
            '{codigo: $c, nombre: $n, siglas: $s, sede: $sede, nivelFormacion: $nivel, modalidad: $m,
              jornadas: ($j | split(";") | map(gsub("^ +| +$"; ""))), campoAmplio: $a,
              campoEspecifico: $e, numeroCiclos: ($ciclos | tonumber)}
            + (if $d != "" then {campoDetallado: $d} else {} end)')"
    [[ $estado == 201 ]] && maestros=$((maestros + 1))
done < <(filas 02_carreras.csv)
while IFS=' ' read -r carrera malla; do
    pedir POST "$GATEWAY/api/academico/carreras/$(cod "$carrera")/mallas" "$auth" \
        "$(jq -cn --arg c "$(cod "$malla")" '{codigo: $c}')"
    [[ $estado == 201 ]] && maestros=$((maestros + 1))
done < <(filas 04_asignaturas_malla.csv | awk -F, '{print $1, $13}' | sort -u)
while IFS=, read -r carrera codigo nombre ciclo unidad clase practicas creditos tronco amplio especifico palabras malla _; do
    pedir POST "$GATEWAY/api/academico/asignaturas" "$auth" \
        "$(jq -cn --arg c "$(cod "$codigo")" --arg car "$(cod "$carrera")" --arg m "$(cod "$malla")" \
            --arg n "$nombre" --arg ciclo "$ciclo" --arg u "$unidad" --arg h "$clase" --arg p "$practicas" \
            --arg cr "$creditos" --arg t "$tronco" --arg a "$amplio" --arg e "$especifico" --arg k "$palabras" \
            '{codigo: $c, carrera: $car, malla: $m, nombre: $n, ciclo: ($ciclo | tonumber),
              unidadOrganizacionCurricular: $u, horasSemanalesClase: ($h | tonumber),
              troncoComun: ($t == "S"), campoAmplio: $a, campoEspecifico: $e,
              palabrasClave: ($k | split(";") | map(gsub("^ +| +$"; "")) | map(select(. != "")))}
            + (if $p != "" then {horasPracticasSemanales: ($p | tonumber)} else {} end)
            + (if $cr != "" then {creditos: ($cr | tonumber)} else {} end)')"
    [[ $estado == 201 ]] && maestros=$((maestros + 1))
done < <(filas 04_asignaturas_malla.csv)
pedir POST "$GATEWAY/api/academico/periodos" "$auth" \
    "$(jq -cn --arg c "$periodo" '{codigo: $c, nombre: "Periodo de prueba de la oferta", inicio: "2026-04-06", fin: "2026-08-07"}')"
[[ $estado == 201 ]] && maestros=$((maestros + 1))
todos_los_maestros() { [[ $maestros == 320 ]]; }
verificar 0 "Datos de prueba: $maestros de 320 (7 carreras, 12 mallas, 300 asignaturas y el periodo $periodo en Planificación, en la sede $sede)" \
    todos_los_maestros

# 1. Las 165 asignaturas-paralelo de la carga 05 (SC-002)
ofertadas=0
while IFS=, read -r _ asignatura paralelo jornada modalidad sede_fila estudiantes horas _; do
    pedir POST "$oferta" "$auth" \
        "$(jq -cn --arg a "$(cod "$asignatura")" --arg p "$paralelo" --arg j "$jornada" --arg m "$modalidad" \
            --arg s "$sede_fila" --arg e "$estudiantes" --arg h "$horas" \
            '{asignatura: $a, paralelo: $p, jornada: $j, modalidad: $m, estudiantes: ($e | tonumber)}
            + (if $s != "" then {sede: $s} else {} end)
            + (if $h != "" then {horasSemanales: ($h | tonumber)} else {} end)')"
    if [[ $estado == 201 ]]; then
        ofertadas=$((ofertadas + 1))
    else
        echo "       $(cod "$asignatura") $paralelo: $estado $cuerpo"
    fi
done < <(filas 05_oferta_periodo.csv)
todas_ofertadas() { [[ $ofertadas == 165 ]]; }
verificar 1 "vicerrectorado.demo oferta $ofertadas de 165 asignaturas-paralelo" todas_ofertadas
pedir GET "$oferta" "$auth"
total=$(jq -r '.horasACubrir + 0' <<<"$cuerpo" 2>/dev/null || true)
por_carrera=$(jq -r --arg p "$prefijo" '[.carreras[] | "\(.carrera | ltrimstr($p)):\(.horasACubrir + 0)"] | join(" ")' \
    <<<"$cuerpo" 2>/dev/null || true)
readonly ESPERADO="550411C02-P-0301:160 550613A01-P-0301:80 550713B01-P-0301:140 550715F02-P-0301:140 550732E01-D-0301:20 550811F01-P-0301:100 551021B01-D-0301:20"
suman_660() { [[ $total == 660 && $por_carrera == "$ESPERADO" ]]; }
verificar 1 "La oferta suma 660 horas a cubrir, con el total de cada carrera (recibido: ${total:-?}; ${por_carrera:-?})" \
    suman_660

# 2. Paralelos y horas a cubrir (escenario del issue, SC-003)
readonly CC="CONT-P-III-CC-301"  # 4 horas, en A_NOCTURNA y B_NOCTURNA
horas_de_cc() {
    pedir GET "$oferta" "$auth"
    jq -r --arg a "$(cod "$CC")" '.carreras[].asignaturas[] | select(.asignatura == $a) | "\(.paralelos):\(.horasACubrir + 0)"' \
        <<<"$cuerpo" 2>/dev/null || true
}
pedir POST "$oferta" "$auth" \
    "$(jq -cn --arg a "$(cod "$CC")" '{asignatura: $a, paralelo: "C_NOCTURNA", jornada: "NOCTURNA", modalidad: "PRESENCIAL", estudiantes: 12}')"
con_tres=$(horas_de_cc)
tres_dan_12() { [[ $con_tres == "3:12" ]]; }
verificar 2 "$CC, de 4 horas, en 3 paralelos suma 12 horas a cubrir (recibido: ${con_tres:-?})" tres_dan_12
pedir PUT "$oferta/$(cod "$CC")/C_NOCTURNA/estado" "$auth" '{"estado":"RETIRADA"}'
con_dos=$(horas_de_cc)
retirada_da_8() { [[ $con_dos == "2:8" ]]; }
verificar 2 "Con C_NOCTURNA retirada, suma 8 (recibido: ${con_dos:-?})" retirada_da_8
pedir PUT "$oferta/$(cod "$CC")/C_NOCTURNA/estado" "$auth" '{"estado":"OFERTADA"}'
otra_vez=$(horas_de_cc)
vuelve_a_12() { [[ $otra_vez == "3:12" ]]; }
verificar 2 "Vuelta a ofertar, suma 12 otra vez (recibido: ${otra_vez:-?})" vuelve_a_12

# 3. Las reglas: repetida, horas de más, jornada que no es de la carrera, asignatura inactiva
paralelo_de_prueba() {  # paralelo_de_prueba <asignatura> <paralelo> <filtro jq que lo cambia>
    jq -cn --arg a "$(cod "$1")" --arg p "$2" \
        '{asignatura: $a, paralelo: $p, jornada: "NOCTURNA", modalidad: "PRESENCIAL", estudiantes: 20} | '"$3"
}
pedir POST "$oferta" "$auth" "$(paralelo_de_prueba "$CC" A_NOCTURNA '.')"
verificar 3 "$CC en A_NOCTURNA otra vez: 409 ACADEMICO_ASIGNATURA_PARALELO_REPETIDA (recibido: $estado)" \
    es_codigo 409 ACADEMICO_ASIGNATURA_PARALELO_REPETIDA
pedir POST "$oferta" "$auth" "$(paralelo_de_prueba "$CC" D_NOCTURNA '.horasSemanales = 5')"
verificar 3 "$CC con 5 horas, más que sus 4: 400 en horasSemanales (recibido: $estado)" es_invalido_en horasSemanales
pedir POST "$oferta" "$auth" "$(paralelo_de_prueba "$CC" A_MATUTINA '.jornada = "MATUTINA"')"
verificar 3 "$CC en jornada matutina, que Contabilidad no tiene: 400 en jornada (recibido: $estado)" es_invalido_en jornada
pedir PUT "$GATEWAY/api/academico/asignaturas/$(cod DS-B-I-MD-101)/estado" "$auth" '{"activa":false}'
pedir POST "$oferta" "$auth" "$(paralelo_de_prueba DS-B-I-MD-101 Z_NOCTURNA '.')"
verificar 3 "DS-B-I-MD-101 inactiva en un paralelo nuevo: 400 en asignatura (recibido: $estado)" es_invalido_en asignatura

# 4. Los datos para la afinidad de una asignatura-paralelo
pedir GET "$oferta/$(cod "$CC")/A_NOCTURNA" "$auth"
afinidad=$(jq -r '"\(.horasSemanales + 0)|\(.sede)|\(.campoAmplio.codigo)|\(.campoAmplio.nombre != "")|\(.palabrasClave | length >= 2)|\(.troncoComun | type)"' \
    <<<"$cuerpo" 2>/dev/null || true)
con_afinidad() { [[ $estado == 200 && $afinidad == "4|$sede|04|true|true|boolean" ]]; }
verificar 4 "$CC en A_NOCTURNA trae sus horas, la sede de su carrera, su campo amplio con nombre, sus palabras clave y el tronco común (recibido: $estado ${afinidad:-?})" \
    con_afinidad

# 5. El tiempo de la consulta (meta del plan): el mejor de tres, con la oferta ya en caché del SO
mejor=""
for _ in 1 2 3; do
    segundos=$(curl -s -o /dev/null -w '%{time_total}' -H "$(printf 'Authorization: %s' "$auth")" "$oferta" || echo 9)
    milis=$(awk -v s="$segundos" 'BEGIN { printf "%d", s * 1000 }')
    if [[ -z $mejor || $milis -lt $mejor ]]; then mejor=$milis; fi
done
rapida() { [[ $mejor -le $MAXIMO_MS ]]; }
verificar 5 "Consultar la oferta con 166 asignaturas-paralelo tarda ${mejor} ms (máximo $MAXIMO_MS ms)" rapida

# 6. Con el periodo fuera de Planificación, la oferta no cambia. El estado se pone en la base:
# salir de Planificación por la API exige abrir el periodo con una versión de Normativa, y eso ya
# lo prueba probar_periodos.sh.
consultar_base "update academico.periodo_academico set estado = 'EN_APROBACION' where codigo = '$periodo'" >/dev/null
pedir POST "$oferta" "$auth" "$(paralelo_de_prueba "$CC" E_NOCTURNA '.')"
verificar 6 "Ofertar con el periodo En aprobación: 409 ACADEMICO_OFERTA_CERRADA (recibido: $estado)" \
    es_codigo 409 ACADEMICO_OFERTA_CERRADA
pedir PUT "$oferta/$(cod "$CC")/A_NOCTURNA/estado" "$auth" '{"estado":"RETIRADA"}'
verificar 6 "Retirar con el periodo En aprobación: 409 ACADEMICO_OFERTA_CERRADA (recibido: $estado)" \
    es_codigo 409 ACADEMICO_OFERTA_CERRADA
pedir GET "$oferta" "$auth"
no_modificable() { [[ $estado == 200 && $(jq -r '.modificable' <<<"$cuerpo" 2>/dev/null) == false ]]; }
verificar 6 "La consulta dice que la oferta ya no se puede cambiar (recibido: $estado)" no_modificable

# 7. Quién la ve (D-70): consolidación lee sin escribir; un docente no lee
iniciar consolidacion.demo
pedir GET "$oferta" "Bearer $token"
la_lee() { [[ $estado == 200 && $(jq -r '.asignaturasParalelo | length' <<<"$cuerpo" 2>/dev/null) == 166 ]]; }
verificar 7 "consolidacion.demo lee la oferta: 200 con 166 asignaturas-paralelo (recibido: $estado)" la_lee
pedir POST "$oferta" "Bearer $token" "$(paralelo_de_prueba "$CC" F_NOCTURNA '.')"
verificar 7 "consolidacion.demo oferta: 403 común (recibido: $estado)" es_403_comun
iniciar docente.demo
pedir GET "$oferta" "Bearer $token"
verificar 7 "docente.demo lee la oferta: 403 común (recibido: $estado)" es_403_comun

# 8. El registro de Académico (FR-010)
acciones=$(consultar_base "select accion || ':' || usuario || ':' || count(*) from academico.registro_cambio
    where detalle ->> 'periodo' = '$periodo' group by accion, usuario order by accion" 2>/dev/null \
    | paste -sd' ' || true)
registro_completo() {
    [[ $acciones == "ASIGNATURA_PARALELO_REGISTRADA:vicerrectorado.demo:166 ASIGNATURA_PARALELO_REOFERTADA:vicerrectorado.demo:1 ASIGNATURA_PARALELO_RETIRADA:vicerrectorado.demo:1" ]]
}
verificar 8 "El registro tiene 166 asignaturas-paralelo ofertadas, 1 retirada y 1 vuelta a ofertar por vicerrectorado.demo (recibido: ${acciones:-?})" \
    registro_completo

if [[ $fallas -eq 0 ]]; then
    echo "Oferta del periodo de punta a punta: todos los pasos pasan."
    exit 0
fi
echo "Oferta del periodo de punta a punta: $fallas paso(s) fallan."
exit 1
