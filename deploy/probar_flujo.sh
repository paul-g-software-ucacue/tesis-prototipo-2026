#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# La configuración del flujo y del árbol de firmas de punta a punta sobre el perfil levantado
# (F12-H01; quickstart.md de specs/038-configuracion-flujo).
# Uso, desde cualquier directorio: deploy/probar_flujo.sh
# Registra un periodo de prueba: sin archivo rige la configuración por defecto; la plantilla se
# descarga; sin Responsable se rechaza y, designado, se carga sin cambios. Un archivo con varios
# problemas los dice todos, y ninguno de los rechazos cambia lo vigente. Un archivo peligroso no se
# interpreta. Fuera de Planificación no se carga. Cada operación exige su permiso. Con datos o con
# identidad detenidos responde el error del catálogo, sin guardar: distributivo llama a los dos.
# Al terminar, aunque un paso falle, borra todo lo que creó y devuelve el rol al usuario del paso 6.
# Termina con 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime el token ni la
# contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly ADMIN=admin.demo
readonly VICERRECTOR=vicerrectorado.demo
readonly RECTOR=rectorado.demo
readonly RESPONSABLE=docente.demo
# Con holgura: el criterio del plan es 3 s y el de un proceso caído, 10 s (SC-003, FR-024).
readonly SEGUNDOS_DE_CARGAR=3
readonly SEGUNDOS_SIN_PROCESO=10

trabajo=$(mktemp -d)
readonly trabajo

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
        echo "       última respuesta: $estado ${cuerpo:0:400}"
        fallas=$((fallas + 1))
    fi
}

estado=""
cuerpo=""
tiempo=0

# Deja el estado en $estado, el cuerpo en $cuerpo y los segundos que tardó en $tiempo.
pedir() {  # pedir <método> <url> [autorización] [cuerpo json]
    local archivo salida
    archivo=$(mktemp)
    local argumentos=(-s -o "$archivo" -w '%{http_code} %{time_total}' -X "$1" "$2")
    [[ -n ${3:-} ]] && argumentos+=(-H "Authorization: $3")
    [[ -n ${4:-} ]] && argumentos+=(-H 'Content-Type: application/json' --data "$4")
    salida=$(curl "${argumentos[@]}" || echo '000 0')
    read -r estado tiempo <<<"$salida"
    cuerpo=$(cat "$archivo")
    rm -f "$archivo"
}

# iniciar <usuario>: deja el token en $token, o vacío si no entró.
iniciar() {
    pedir POST "$GATEWAY/api/usuarios/sesion" "" \
        "$(jq -cn --arg u "$1" --arg c "$(valor_de_entorno MDD_USUARIOS_DEMO_CLAVE)" \
            '{usuario: $u, clave: $c}')"
    token=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
}

# psql en la base del perfil; imprime el resultado sin alineación ni encabezados.
consultar_base() {
    docker compose exec -T postgres psql -q -At -U postgres -d "$(valor_de_entorno POSTGRES_DB)" \
        -v ON_ERROR_STOP=1 -c "$1"
}

menos_de() {  # menos_de <segundos> <límite>
    awk -v a="$1" -v b="$2" 'BEGIN { exit !(a < b) }'
}

es_codigo() {  # es_codigo <estado> <código del catálogo>
    [[ $estado == "$1" && $(jq -r '.codigo' <<<"$cuerpo" 2>/dev/null) == "$2" ]]
}

dice() {  # dice <texto>: uno de los detalles del error
    jq -e --arg t "$1" '[.detalles[]?.descripcion] | index($t)' <<<"$cuerpo" >/dev/null 2>&1
}

periodo="prueba-$RANDOM$RANDOM"
rol_quitado=""
limpiar() {
    local sql
    sql="delete from validacionocs.configuracion_flujo where periodo = '$periodo';
delete from roles.designacion_responsable where periodo = '$periodo';
delete from roles.periodo_conocido where codigo = '$periodo';
delete from roles.registro_cambio where detalle ->> 'periodo' = '$periodo';
delete from roles.evento_recibido where id_evento in (select (serialized_event::jsonb ->> 'idEvento')::uuid
    from eventos_datos.event_publication where serialized_event::jsonb -> 'contenido' ->> 'codigo' = '$periodo');
delete from eventos_datos.event_publication where serialized_event::jsonb -> 'contenido' ->> 'codigo' = '$periodo';
delete from academico.registro_cambio where detalle ->> 'codigo' = '$periodo';
delete from academico.periodo_academico where codigo = '$periodo';"
    [[ -n $rol_quitado ]] && sql+="
$(devolver_rol)"
    consultar_base "$sql" >/dev/null 2>&1 \
        || echo "AVISO  No se pudo limpiar el periodo $periodo"
    rm -rf "$trabajo"
}
trap limpiar EXIT

devolver_rol() {
    printf "%s" "insert into roles.asignacion_rol (nombre_usuario, rol_id, asignado_en, asignado_por)
select '$RECTOR', id, now(), 'probar_flujo' from roles.rol where identificador = 'RECTOR'
on conflict do nothing;"
}

ruta() {
    printf '%s' "$GATEWAY/api/validacionocs/periodos/$1/configuracion-flujo"
}

# subir <periodo> <archivo> [token]: deja el resultado como pedir.
subir() {
    local archivo salida
    archivo=$(mktemp)
    salida=$(curl -s -o "$archivo" -w '%{http_code} %{time_total}' -X PUT "$(ruta "$1")" \
        -H "Authorization: Bearer ${3:-$token_admin}" \
        -F "archivo=@$2;filename=flujo.yaml;type=text/yaml" || echo '000 0')
    read -r estado tiempo <<<"$salida"
    cuerpo=$(cat "$archivo")
    rm -f "$archivo"
}

consultar() {
    pedir GET "$(ruta "$periodo")" "Bearer $token_admin"
}

filas() {
    consultar_base "select count(*) from validacionocs.configuracion_flujo where periodo = '$periodo'"
}

# La plantilla con un cambio: variante <nombre> <expresión de sed>
variante() {
    sed -E "$2" "$trabajo/plantilla.yaml" >"$trabajo/$1.yaml"
    printf '%s' "$trabajo/$1.yaml"
}

iniciar "$ADMIN"
token_admin=$token
iniciar "$VICERRECTOR"
token_vicerrector=$token
iniciar "$RECTOR"
token_rector=$token

periodo_registrado() {
    pedir POST "$GATEWAY/api/academico/periodos" "Bearer $token_vicerrector" \
        "$(jq -cn --arg c "$periodo" \
            '{codigo: $c, nombre: "Periodo de prueba", inicio: "2026-09-01", fin: "2027-02-28"}')"
    [[ $estado == 201 ]]
}

rige_la_de_por_defecto() {
    consultar
    [[ $estado == 200 ]] \
        && jq -e '.porDefecto == true and .modificable == true and (.version == null)
            and (.etapas | length) == 9 and (.firmas | length) == 3
            and ([.firmas[].rol] == ["RESPONSABLE_CONSOLIDACION", "VICERRECTOR", "RECTOR"])' \
            <<<"$cuerpo" >/dev/null
}

se_descarga_la_plantilla() {
    local encabezados
    encabezados=$(curl -s -D - -o "$trabajo/plantilla.yaml" "$GATEWAY/api/validacionocs/plantilla-flujo" \
        -H "Authorization: Bearer $token_admin")
    [[ $encabezados == *" 200"* ]] \
        && grep -qi '^content-type: text/yaml' <<<"$encabezados" \
        && grep -qi 'flujo-por-defecto.yaml' <<<"$encabezados" \
        && grep -q '^# Configuración del flujo' "$trabajo/plantilla.yaml"
}

sin_responsable_se_rechaza() {
    subir "$periodo" "$trabajo/plantilla.yaml"
    es_codigo 400 VALIDACIONOCS_CONFIGURACION_INVALIDA \
        && dice 'Falta designar al Responsable de consolidación del periodo.' \
        && [[ $(filas) == 0 ]]
}

designado() {
    pedir PUT "$GATEWAY/api/roles/responsables/$periodo" "Bearer $token_vicerrector" \
        "$(jq -cn --arg u "$RESPONSABLE" '{usuario: $u}')"
    [[ $estado == 200 ]]
}

esperar_designacion() {
    # El aviso de Académico llega a Roles por un evento.
    for _ in $(seq 30); do
        designado && return 0
        sleep 1
    done
    return 1
}

la_plantilla_se_carga_sin_cambios() {
    subir "$periodo" "$trabajo/plantilla.yaml"
    echo "       cargar tardó $tiempo s"
    [[ $estado == 200 ]] \
        && jq -e --arg a "$ADMIN" '.porDefecto == false and .version == 1 and .cargada.por == $a' \
            <<<"$cuerpo" >/dev/null \
        && menos_de "$tiempo" "$SEGUNDOS_DE_CARGAR"
}

otra_carga_crea_otra_version() {
    subir "$periodo" "$(variante cambiado 's/plazo_dias: 15/plazo_dias: 20/')"
    [[ $estado == 200 ]] || return 1
    consultar
    jq -e '.version == 2
        and (.etapas[] | select(.tipo == "BORRADOR_CARRERA") | .plazoDias) == 20' \
        <<<"$cuerpo" >/dev/null \
        && [[ $(filas) == 2 ]]
}

lo_vigente_sigue_igual() {
    local antes=$cuerpo_vigente
    consultar
    [[ $cuerpo == "$antes" && $(filas) == 2 ]]
}

sin_en_ocs_dice_la_etapa() {
    subir "$periodo" "$(variante sin_ocs 's/^  EN_OCS:$/  OTRA_COSA:/')"
    es_codigo 400 VALIDACIONOCS_CONFIGURACION_INVALIDA \
        && dice 'Falta la etapa EN_OCS.' && dice 'La etapa OTRA_COSA no existe.'
}

firma_en_etapa_inactiva() {
    subir "$periodo" "$(variante inactiva \
        '0,/etapa: CONGELADO_EN_FIRMA/s//etapa: REMITIDO_ZONAL/')"
    es_codigo 400 VALIDACIONOCS_CONFIGURACION_INVALIDA \
        && dice 'La firma Elaborado está en REMITIDO_ZONAL, que no está activa.'
}

rol_firmante_sin_usuario() {
    consultar_base "delete from roles.asignacion_rol where nombre_usuario = '$RECTOR'
        and rol_id = (select id from roles.rol where identificador = 'RECTOR')" >/dev/null
    rol_quitado=si
    subir "$periodo" "$trabajo/plantilla.yaml"
    local bien=1
    es_codigo 400 VALIDACIONOCS_CONFIGURACION_INVALIDA \
        && dice 'El rol RECTOR no tiene usuarios.' && bien=0
    consultar_base "$(devolver_rol)" >/dev/null
    rol_quitado=""
    return "$bien"
}

cinco_problemas_a_la_vez() {
    cat >"$trabajo/cinco.yaml" <<'YAML'
otra_clave: 1
etapas:
  EN_OCS:
    activa: false
    nombre: Resolución
    rol: CONSERJE
    plazo_dias: 0
    admite_devolucion: true
firmas:
  - orden: 2
    etiqueta: Elaborado
    rol: RECTOR
    etapa: EN_OCS
    documento: CONTRATO
YAML
    subir "$periodo" "$trabajo/cinco.yaml"
    es_codigo 400 VALIDACIONOCS_CONFIGURACION_INVALIDA \
        && dice 'Línea 1: la clave otra_clave no existe.' \
        && dice 'La etapa EN_OCS no se puede desactivar.' \
        && dice 'La etapa EN_OCS tiene el rol CONSERJE, que no existe.' \
        && dice 'El plazo de la etapa EN_OCS debe ser un número entero de días, mayor que 0.' \
        && dice 'Falta la etapa BORRADOR_CARRERA.' \
        && dice 'La firma Elaborado es sobre CONTRATO, un documento que no existe.' \
        && dice 'Falta la firma de orden 1: van desde 1, sin huecos.'
}

rechazado_sin_interpretar() {  # rechazado_sin_interpretar <contenido>
    printf '%s' "$1" >"$trabajo/peligroso.yaml"
    subir "$periodo" "$trabajo/peligroso.yaml"
    es_codigo 400 VALIDACIONOCS_ARCHIVO_ILEGIBLE \
        && [[ $cuerpo != *snakeyaml* && $cuerpo != *org.yaml* && $cuerpo != *Exception* ]]
}

lo_peligroso_no_se_interpreta() {
    rechazado_sin_interpretar 'a: &x 1' \
        && rechazado_sin_interpretar $'a: &x hola\nb: *x\n' \
        && rechazado_sin_interpretar 'a: !!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[]]]' \
        && rechazado_sin_interpretar 'a: !raro 1' \
        && rechazado_sin_interpretar $'a: [\n' \
        && rechazado_sin_interpretar ' '
}

clave_repetida() {
    subir "$periodo" "$(variante repetida 's/^etapas:$/firmas: []\netapas:/')"
    es_codigo 400 VALIDACIONOCS_CONFIGURACION_INVALIDA \
        && jq -e '[.detalles[]?.descripcion] | any(endswith("la clave firmas está repetida."))' \
            <<<"$cuerpo" >/dev/null
}

mas_de_un_mega() {
    head -c 1048577 /dev/zero | tr '\0' ' ' >"$trabajo/grande.yaml"
    subir "$periodo" "$trabajo/grande.yaml"
    es_codigo 413 VALIDACIONOCS_ARCHIVO_DEMASIADO_GRANDE
}

periodo_que_no_existe() {
    subir NADA-DE-NADA "$trabajo/plantilla.yaml"
    es_codigo 404 VALIDACIONOCS_PERIODO_NO_ENCONTRADO || return 1
    pedir GET "$(ruta NADA-DE-NADA)" "Bearer $token_admin"
    es_codigo 404 VALIDACIONOCS_PERIODO_NO_ENCONTRADO
}

permisos() {
    local ruta_plantilla="$GATEWAY/api/validacionocs/plantilla-flujo"
    pedir GET "$(ruta "$periodo")" "Bearer $token_rector"
    [[ $estado == 403 ]] || return 1
    pedir GET "$ruta_plantilla" "Bearer $token_rector"
    [[ $estado == 403 ]] || return 1
    subir "$periodo" "$trabajo/plantilla.yaml" "$token_rector"
    [[ $estado == 403 ]] || return 1
    pedir GET "$(ruta "$periodo")"
    [[ $estado == 401 ]]
}

sin_el_proceso() {  # sin_el_proceso <servicio>
    local bien=0 antes
    antes=$(filas)
    docker compose stop "$1" >/dev/null 2>&1
    subir "$periodo" "$trabajo/plantilla.yaml"
    echo "       sin $1 respondió $estado en $tiempo s"
    if [[ $estado == 502 || $estado == 504 ]] \
        && [[ $(jq -r '.codigo' <<<"$cuerpo") == COMUN_PROCESO_* ]] \
        && menos_de "$tiempo" "$SEGUNDOS_SIN_PROCESO"; then
        bien=1
    fi
    docker compose up -d --wait --no-recreate --no-build "$1" >/dev/null 2>&1 || return 1
    [[ $bien == 1 && $(filas) == "$antes" ]]
}

vuelve_con_los_procesos() {
    iniciar "$ADMIN"
    token_admin=$token
    for _ in $(seq 30); do
        subir "$periodo" "$trabajo/plantilla.yaml"
        [[ $estado == 200 ]] && break
        sleep 1
    done
    [[ $estado == 200 ]] && jq -e '.version == 3' <<<"$cuerpo" >/dev/null
}

fuera_de_planificacion() {
    consultar_base "update academico.periodo_academico set estado = 'VIGENTE'
        where codigo = '$periodo'" >/dev/null
    subir "$periodo" "$trabajo/plantilla.yaml"
    es_codigo 409 VALIDACIONOCS_PERIODO_SIN_PLANIFICACION || return 1
    consultar
    [[ $estado == 200 ]] \
        && jq -e '.modificable == false and .version == 3' <<<"$cuerpo" >/dev/null
}

queda_quien_y_cuando() {
    [[ $(consultar_base "select count(*) from validacionocs.configuracion_flujo
        where periodo = '$periodo' and cargada_por = '$ADMIN' and cargada_en > now() - interval '1 hour'
        and contenido like '%# Configuración del flujo%'") == 3 ]]
}

el_contenido_no_va_a_los_registros() {
    [[ $(docker compose logs --no-color distributivo 2>/dev/null \
        | grep -c 'ScriptEngineManager\|otra_clave' || true) == 0 ]]
}

verificar 0 "vicerrectorado.demo registra $periodo, en Planificación" periodo_registrado
verificar 1 "Sin archivo rige la configuración por defecto: 9 etapas y F1, F2 y F3" rige_la_de_por_defecto
verificar 2 "El Administrador descarga la plantilla comentada" se_descarga_la_plantilla
verificar 3 "Sin Responsable designado se rechaza diciéndolo, y no guarda" sin_responsable_se_rechaza
verificar 4 "El aviso del periodo llega a Roles y $RESPONSABLE queda designado" esperar_designacion
verificar 4 "La plantilla se carga sin cambios en menos de $SEGUNDOS_DE_CARGAR s (SC-001, SC-003)" \
    la_plantilla_se_carga_sin_cambios
verificar 5 "Otra carga crea la versión 2 y conserva la 1" otra_carga_crea_otra_version
consultar
cuerpo_vigente=$cuerpo
verificar 6 "Sin EN_OCS se rechaza diciendo la etapa" sin_en_ocs_dice_la_etapa
verificar 6 "Una firma en una etapa desactivada se rechaza" firma_en_etapa_inactiva
verificar 6 "Un rol firmante sin usuario se rechaza diciendo cuál (distributivo llama a identidad)" \
    rol_firmante_sin_usuario
verificar 7 "Varios problemas salen juntos (SC-002)" cinco_problemas_a_la_vez
verificar 8 "Anclas, alias, tipos, YAML roto y vacío: sin interpretarlos ni detalles técnicos" \
    lo_peligroso_no_se_interpreta
verificar 8 "Una clave repetida se rechaza" clave_repetida
verificar 8 "Más de 1 MB: 413" mas_de_un_mega
verificar 9 "Ningún rechazo cambió lo vigente" lo_vigente_sigue_igual
verificar 10 "Un periodo que no existe: 404" periodo_que_no_existe
verificar 11 "Sin FLUJO_FIRMAS, 403 en las tres operaciones; sin token, 401" permisos
verificar 12 "Con datos detenido: 502 o 504 del catálogo en menos de $SEGUNDOS_SIN_PROCESO s, sin guardar" \
    sin_el_proceso datos
verificar 12 "Con identidad detenido: 502 o 504 del catálogo en menos de $SEGUNDOS_SIN_PROCESO s, sin guardar" \
    sin_el_proceso identidad
verificar 13 "Con los procesos de vuelta se carga la versión 3" vuelve_con_los_procesos
verificar 14 "Fuera de Planificación se ve y no se carga: 409" fuera_de_planificacion
verificar 15 "Cada carga queda con su texto, quién y cuándo" queda_quien_y_cuando
verificar 16 "El contenido de los archivos no va a los registros" el_contenido_no_va_a_los_registros

if ((fallas > 0)); then
    echo "Fallaron $fallas pasos."
    exit 1
fi
echo "La configuración del flujo funciona de punta a punta."
