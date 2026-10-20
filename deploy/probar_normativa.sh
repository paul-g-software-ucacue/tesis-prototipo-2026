#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# La normativa de punta a punta sobre el perfil levantado (F04-H01, F04-H02 y F04-H03; quickstart.md
# de specs/012-politica-normativa, specs/013-version-normativa-nueva y specs/014-afinidad-severidad).
# Uso, desde cualquier directorio: deploy/probar_normativa.sh
# Comprueba que la Política SENESCYT 2024 es la versión 1 con sus 22 filas, que la leen el
# Vicerrector académico y el Administrador y no un docente (D-66), y que reiniciar el proceso datos
# no la duplica ni la cambia (FR-006). Reinicia datos tres veces. Después (F04-H02, D-67), el
# Vicerrector asocia la versión 1 a un periodo de prueba, crea una versión a partir de ella y le
# cambia las horas de clase del docente de medio tiempo; la 1 no cambia, editarla da 409 y el
# periodo no acepta otra versión. Por último (F04-H03), la 1 trae los pesos y el mínimo de afinidad
# por defecto, y la versión creada pasa RN-05 a dura, rige un segundo periodo de prueba y la consulta
# del periodo lleva a ella, con RN-05 dura; la 1 la sigue teniendo blanda. Al terminar, aunque un
# paso falle, borra en la base solo los periodos de prueba y las versiones que creó: la API no borra
# versiones (principio VII).
# Termina con 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime el token ni la
# contraseña.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
readonly CUERPO_403='{"titulo":"Acceso no autorizado","descripcion":"No tienes permiso para esta acción. Si crees que deberías tenerlo, pide al administrador que revise tus roles.","codigo":"SEGURIDAD_ACCESO_NO_AUTORIZADO"}'

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
        "$(jq -cn --arg u "$1" --arg c "$(valor_de_entorno MDD_USUARIOS_DEMO_CLAVE)" '{usuario: $u, clave: $c}')"
    token=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
}

# La 1 es la primera. No se exige que sea la única: en el perfil local puede haber versiones que el
# autor creó a mano (F04-H02).
la_version_uno_es_la_primera() {
    [[ $estado == 200 && $(jq -c '.[0] | {numero, codigo, tipoIst}' <<<"$cuerpo" 2>/dev/null) \
        == '{"numero":1,"codigo":"SENESCYT-2024-03","tipoIst":"T2"}' ]]
}

# Sin sus periodos, que cambian al asociarla (F04-H02).
sin_periodos() {
    jq -cS 'del(.periodos)' <<<"$1" 2>/dev/null
}

es_la_politica_de_2024() {
    [[ $estado == 200 ]] \
        && [[ $(jq '.tablaCargaHoraria | length' <<<"$cuerpo") == 22 ]] \
        && [[ $(jq '.reglas | length' <<<"$cuerpo") == 16 ]] \
        && [[ $(jq -c '.tablaCargaHoraria[] | select(.perfil == "DOCENTE_TC")
                | [.horasClase, .horasClaseMinimo, .horasClaseMaximo, .horasOtrasDocencia]' <<<"$cuerpo") \
            == '[22,18,24,18]' ]]
}

es_403_comun() {
    [[ $estado == 403 && $cuerpo == "$CUERPO_403" ]]
}

es_estado() {
    [[ $estado == "$1" ]]
}

es_igual_a_la_guardada() {
    [[ $estado == 200 && $(sin_periodos "$cuerpo") == "$(sin_periodos "$guardada")" ]]
}

es_codigo() {  # es_codigo <estado> <código del catálogo>
    [[ $estado == "$1" && $(jq -r '.codigo' <<<"$cuerpo" 2>/dev/null) == "$2" ]]
}

# El cuerpo de editarVersion a partir de una versión: sus filas sin severidad, referencia ni
# observación, sus reglas sin severidad y, si se pide, otras horas de clase para DOCENTE_MT.
edicion_de() {  # edicion_de <versión> [horas de clase de DOCENTE_MT]
    jq -c --arg clase "${2:-}" '{codigo, origen, vigenteDesde,
        reglas: [.reglas[] | {identificador, parametros}],
        tablaCargaHoraria: [.tablaCargaHoraria[] | del(.severidad, .referencia, .observacion)
            | if $clase != "" and .perfil == "DOCENTE_MT" then .horasClase = ($clase | tonumber)
              else . end]}' <<<"$1"
}

# Horas de clase de DOCENTE_MT distintas y válidas: una menos, o una más si ya está en el mínimo.
otra_clase_de_medio_tiempo() {
    jq '.tablaCargaHoraria[] | select(.perfil == "DOCENTE_MT")
        | if .horasClase > .horasClaseMinimo then .horasClase - 1 else .horasClase + 1 end' <<<"$1"
}

clase_de_medio_tiempo() {
    jq '.tablaCargaHoraria[] | select(.perfil == "DOCENTE_MT") | .horasClase' <<<"$1"
}

# La limpieza (research §12 de F04-H02): solo el periodo de prueba y las versiones que este script
# creó, con sus filas, reglas, parámetros y registros. Corre al salir, aunque falle un paso.
periodo="prueba-$RANDOM$RANDOM"
versiones_creadas=()
limpiar() {
    local sql numero
    sql="delete from normativa.asignacion_normativa where periodo = '$periodo';
delete from normativa.registro_cambio where accion = 'VERSION_ASOCIADA'
    and detalle ->> 'periodo' = '$periodo';"
    for numero in "${versiones_creadas[@]}"; do
        [[ $numero =~ ^[0-9]+$ ]] || continue
        sql+="
delete from normativa.parametro_regla where version_id =
    (select id from normativa.version_normativa where numero = $numero);
delete from normativa.regla where version_id =
    (select id from normativa.version_normativa where numero = $numero);
delete from normativa.fila_carga_horaria where version_id =
    (select id from normativa.version_normativa where numero = $numero);
delete from normativa.registro_cambio where (detalle ->> 'numero')::int = $numero;
delete from normativa.version_normativa where numero = $numero;"
    done
    docker compose exec -T postgres psql -q -U postgres -d "$(valor_de_entorno POSTGRES_DB)" \
        -v ON_ERROR_STOP=1 -c "$sql" >/dev/null 2>&1 \
        || echo "AVISO  No se pudo limpiar el periodo $periodo ni las versiones ${versiones_creadas[*]:-}"
}
trap limpiar EXIT

# 1. El Vicerrector académico lista las versiones: la primera es la 1 (D-66)
iniciar vicerrectorado.demo
token_vicerrector=$token
pedir GET "$GATEWAY/api/normativa/versiones" "Bearer $token_vicerrector"
verificar 1 "vicerrectorado.demo: la primera versión es la 1, SENESCYT-2024-03 con Tipo 2 (recibido: $estado)" la_version_uno_es_la_primera

# 2. La versión 1 es la política 2024
pedir GET "$GATEWAY/api/normativa/versiones/1" "Bearer $token_vicerrector"
verificar 2 "La versión 1 tiene 22 filas y 7 reglas; DOCENTE_TC con 22 h de clase entre 18 y 24 y 18 de otras de docencia (recibido: $estado)" es_la_politica_de_2024
guardada=$cuerpo

# 3. El Administrador la ve igual; un docente recibe el 403; una versión que no existe, 404
iniciar admin.demo
pedir GET "$GATEWAY/api/normativa/versiones/1" "Bearer $token"
verificar 3 "admin.demo ve la misma versión 1 (recibido: $estado)" es_igual_a_la_guardada
iniciar docente.demo
pedir GET "$GATEWAY/api/normativa/versiones" "Bearer $token"
verificar 3 "docente.demo: 403 SEGURIDAD_ACCESO_NO_AUTORIZADO con el cuerpo común (recibido: $estado)" es_403_comun
pedir GET "$GATEWAY/api/normativa/versiones/99" "Bearer $token_vicerrector"
verificar 3 "Una versión que no existe: 404 (recibido: $estado)" es_estado 404

# 4. Reiniciar datos tres veces no duplica ni cambia la versión 1 (FR-006, SC-003)
for vuelta in 1 2 3; do
    docker compose restart datos >/dev/null 2>&1
    docker compose up -d --wait datos >/dev/null 2>&1
    echo "       datos reiniciado ($vuelta de 3)"
done
pedir GET "$GATEWAY/api/normativa/versiones" "Bearer $token_vicerrector"
verificar 4 "Después de 3 reinicios de datos: la primera sigue siendo la 1 (recibido: $estado)" la_version_uno_es_la_primera
pedir GET "$GATEWAY/api/normativa/versiones/1" "Bearer $token_vicerrector"
verificar 4 "Después de 3 reinicios de datos: la versión 1 es idéntica (recibido: $estado)" es_igual_a_la_guardada

# 5. Versión nueva (F04-H02, SC-002): con la 1 en un periodo de prueba, el Vicerrector crea una
# versión a partir de ella y cambia las horas de clase del docente de medio tiempo; la 1 no cambia.
iniciar vicerrectorado.demo
token_vicerrector=$token
pedir PUT "$GATEWAY/api/normativa/periodos/$periodo" "Bearer $token_vicerrector" '{"version":1}'
verificar 5 "La versión 1 queda asociada al periodo de prueba $periodo (recibido: $estado)" es_estado 200
pedir GET "$GATEWAY/api/normativa/versiones/1" "Bearer $token_vicerrector"
antes=$cuerpo
pedir POST "$GATEWAY/api/normativa/versiones" "Bearer $token_vicerrector" \
    "$(jq -cn --arg c "PRUEBA-${periodo#prueba-}" \
        '{copiarDe: 1, codigo: $c, origen: "Prueba de punta a punta", vigenteDesde: "2025-01-15"}')"
creada=$cuerpo
tiempo_crear=$tiempo
numero=$(jq -r '.numero // empty' <<<"$creada" 2>/dev/null || true)
[[ -n $numero ]] && versiones_creadas+=("$numero")
verificar 5 "Crear una versión a partir de la 1: 201, la versión ${numero:-?} copiada de la 1 (recibido: $estado, ${tiempo_crear} s)" \
    es_estado 201
clase=$(otra_clase_de_medio_tiempo "$creada")
pedir PUT "$GATEWAY/api/normativa/versiones/${numero:-0}" "Bearer $token_vicerrector" \
    "$(edicion_de "$creada" "$clase")"
tiempo_editar=$tiempo
verificar 5 "Cambiar las horas de clase de DOCENTE_MT a $clase en la versión ${numero:-?}: 200 (recibido: $estado, ${tiempo_editar} s)" \
    es_estado 200
editada=$cuerpo
cambio_guardado() { [[ $(clase_de_medio_tiempo "$editada") == "$clase" ]]; }
verificar 5 "La versión ${numero:-?} tiene el cambio" cambio_guardado
pedir GET "$GATEWAY/api/normativa/versiones/1" "Bearer $token_vicerrector"
uno_sin_cambios() {
    [[ $estado == 200 && $(sin_periodos "$cuerpo") == "$(sin_periodos "$antes")" ]] \
        && jq -e --arg p "$periodo" '.periodos | index($p)' <<<"$cuerpo" >/dev/null
}
verificar 5 "La versión 1 es idéntica a la de antes, y rige el periodo de prueba (recibido: $estado)" uno_sin_cambios

# 6. Versión en uso (SC-003): editar la 1, que rige el periodo de prueba, da 409 y no la cambia
pedir PUT "$GATEWAY/api/normativa/versiones/1" "Bearer $token_vicerrector" \
    "$(edicion_de "$antes" "$(otra_clase_de_medio_tiempo "$antes")")"
verificar 6 "Editar la versión 1, en uso: 409 NORMATIVA_VERSION_EN_USO (recibido: $estado)" \
    es_codigo 409 NORMATIVA_VERSION_EN_USO
pedir GET "$GATEWAY/api/normativa/versiones/1" "Bearer $token_vicerrector"
verificar 6 "La versión 1 sigue idéntica (recibido: $estado)" uno_sin_cambios

# 7. Un periodo tiene una sola versión; sin escritura, un docente no crea versiones
pedir PUT "$GATEWAY/api/normativa/periodos/$periodo" "Bearer $token_vicerrector" \
    "{\"version\":${numero:-0}}"
verificar 7 "Asociar la versión ${numero:-?} al mismo periodo: 409 NORMATIVA_PERIODO_CON_VERSION (recibido: $estado)" \
    es_codigo 409 NORMATIVA_PERIODO_CON_VERSION
pedir GET "$GATEWAY/api/normativa/periodos/$periodo" "Bearer $token_vicerrector"
sigue_con_la_uno() { [[ $estado == 200 && $(jq '.version' <<<"$cuerpo") == 1 ]]; }
verificar 7 "El periodo de prueba sigue con la versión 1 (recibido: $estado)" sigue_con_la_uno
iniciar docente.demo
pedir POST "$GATEWAY/api/normativa/versiones" "Bearer $token" \
    '{"copiarDe":1,"codigo":"NO-DEBE","origen":"No debe","vigenteDesde":"2025-01-15"}'
verificar 7 "docente.demo no crea versiones: 403 con el cuerpo común (recibido: $estado)" es_403_comun

# 8. Valores por defecto (F04-H03, escenario 1): los pesos de afinidad de RN-24 y el mínimo de RN-19
pedir GET "$GATEWAY/api/normativa/versiones/1" "Bearer $token_vicerrector"
pesos_por_defecto() {
    [[ $estado == 200 ]] \
        && [[ $(jq -c '[.reglas[] | select(.identificador == "RN-24") | .parametros[]
                | {(.nombre): .valor}] | add' <<<"$cuerpo") \
            == '{"cuarto_nivel_campo_especifico":3,"cuarto_nivel_campo_amplio":2,"tercer_nivel_campo_especifico":2,"tercer_nivel_campo_amplio":1,"experiencia_relacionada":2,"curso_relacionado":1,"tope_cursos":3}' ]] \
        && [[ $(jq -c '.reglas[] | select(.identificador == "RN-19") | [.severidad, .parametros]' <<<"$cuerpo") \
            == '["BLANDA",[{"nombre":"minimo_afinidad","valor":2}]]' ]]
}
verificar 8 "La versión 1 trae los pesos de RN-24 (3, 2, 2, 1, 2, 1 y tope 3) y el mínimo 2 de RN-19, blanda (recibido: $estado)" \
    pesos_por_defecto

if [[ $fallas -eq 0 ]]; then
    echo "Normativa de punta a punta: todos los pasos pasan."
    exit 0
fi
echo "Normativa de punta a punta: $fallas paso(s) fallan."
exit 1
