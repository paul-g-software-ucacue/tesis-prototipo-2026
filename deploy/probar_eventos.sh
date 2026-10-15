#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar y esperar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Eventos de punta a punta sobre el perfil levantado (B00-H05; specs/006-eventos-persistentes/
# contracts/demostracion.md). Uso, desde cualquier directorio: deploy/probar_eventos.sh
# Publica eventos de prueba de Generación (distributivo) hacia Usuarios (identidad) y comprueba en
# la base la entrega normal, la caída y la vuelta de identidad, y la entrega repetida. Detiene y
# vuelve a levantar el proceso identidad. Necesita MDD_EVENTOS_PRUEBA_HABILITADO=true y la clave
# del proceso distributivo (.env.ejemplo). Termina con 0 si todos los pasos pasan y con 1 si
# alguno falla. Nunca imprime el token ni la clave.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
# Dos ciclos de la tarea de reenvío (30 s) y un margen (contracts/demostracion.md).
readonly ESPERA_REENVIO=75
readonly PATRON_UUID='^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'

clave_de_prueba() {
    local archivo linea valor=""
    for archivo in .env.ejemplo .env; do
        [[ -f $archivo ]] || continue
        linea=$(grep -E '^MDD_USUARIO_PRUEBA_CLAVE=' "$archivo" | tail -1 || true)
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
pedir() {  # pedir <método> <url> [autorización] [cuerpo json]
    local archivo
    archivo=$(mktemp)
    local argumentos=(-s -o "$archivo" -w '%{http_code}' -X "$1" "$2")
    [[ -n ${3:-} ]] && argumentos+=(-H "Authorization: $3")
    [[ -n ${4:-} ]] && argumentos+=(-H 'Content-Type: application/json' --data "$4")
    estado=$(curl "${argumentos[@]}" || echo 000)
    cuerpo=$(cat "$archivo")
    rm -f "$archivo"
}

# Consulta la base con el superusuario. Los identificadores se validan como UUID antes de usarlos.
sql() {
    docker compose exec -T postgres psql -U postgres -d mdd -tAc "$1" 2>/dev/null | tr -d '[:space:]'
}

es_uuid() {
    [[ $1 =~ $PATRON_UUID ]]
}

# Estado de la publicación de un evento en el registro de distributivo, buscada por su idEvento.
estado_publicacion() {  # estado_publicacion <idEvento>
    sql "select coalesce(max(status), 'SIN_PUBLICACION') from eventos_distributivo.event_publication where serialized_event like '%$1%'"
}

recibidos() {  # recibidos <idEvento>
    sql "select count(*) from usuarios.evento_recibido where id_evento = '$1'"
}

filas_de_prueba() {  # filas_de_prueba <idEvento>
    sql "select count(*) from generacion.evento_prueba where id_evento = '$1'"
}

completada_y_recibida_una_vez() {  # completada_y_recibida_una_vez <idEvento>
    [[ $(estado_publicacion "$1") == COMPLETED && $(recibidos "$1") == 1 ]]
}

pendiente_con_la_fila() {  # pendiente_con_la_fila <idEvento>
    local publicada
    publicada=$(estado_publicacion "$1")
    [[ $publicada != COMPLETED && $publicada != SIN_PUBLICACION && $(filas_de_prueba "$1") == 1 ]]
}

iniciar_sesion() {
    local credenciales
    credenciales=$(jq -cn --arg c "$(clave_de_prueba)" '{usuario: "autor.prueba", clave: $c}')
    pedir POST "$GATEWAY/api/usuarios/sesion" "" "$credenciales"
    token=$(jq -r '.token // empty' <<<"$cuerpo" 2>/dev/null || true)
    [[ -n $token ]]
}

# Publica un evento de prueba y deja su identificador en $id_evento.
publicar() {
    id_evento=""
    pedir POST "$GATEWAY/api/generacion/eventos-prueba" "Bearer $token"
    [[ $estado == 202 ]] || return 1
    id_evento=$(jq -r '.idEvento // empty' <<<"$cuerpo" 2>/dev/null || true)
    es_uuid "$id_evento"
}

# 0. Inicio de sesión con el usuario de prueba
if ! iniciar_sesion; then
    echo "FALLA  0. No se pudo iniciar sesión con el usuario de prueba (recibido: $estado)"
    exit 1
fi
echo "OK     0. Inicio de sesión con autor.prueba"

# 1. Entrega normal (escenario 1, SC-001)
if publicar; then
    primero=$id_evento
    verificar 1 "Entrega normal: 1 registro en usuarios.evento_recibido y la publicación COMPLETED en menos de 10 s" \
        esperar 10 completada_y_recibida_una_vez "$primero"
else
    primero=""
    verificar 1 "Entrega normal: el evento de prueba responde 202 con idEvento (recibido: $estado)" false
fi

# 2. Receptor caído (escenario 2, SC-002). El API Gateway y distributivo validan el token con el
# JWKS que ya tienen en caché; si responde 401, ese caché se renovó con identidad caído.
docker compose stop identidad >/dev/null 2>&1
if publicar; then
    segundo=$id_evento
    verificar 2 "Identidad detenido: 202, la fila en generacion.evento_prueba y la publicación pendiente" \
        esperar 15 pendiente_con_la_fila "$segundo"
else
    segundo=""
    verificar 2 "Identidad detenido: el evento de prueba responde 202 (recibido: $estado; 401 = JWKS renovado sin identidad)" false
fi

# 3. Identidad vuelve: la tarea programada reenvía (escenario 2, SC-003)
docker compose start identidad >/dev/null 2>&1
if docker compose up -d --wait identidad >/dev/null 2>&1; then
    if [[ -n $segundo ]]; then
        verificar 3 "Identidad de vuelta: 1 registro y la publicación COMPLETED en menos de $ESPERA_REENVIO s" \
            esperar "$ESPERA_REENVIO" completada_y_recibida_una_vez "$segundo"
    else
        verificar 3 "Identidad de vuelta: no hay evento del paso 2 para comprobar" false
    fi
else
    verificar 3 "Identidad de vuelta: el proceso identidad no volvió a quedar sano" false
fi

# 4. Entrega repetida (escenario 3, SC-004): se vuelve a marcar FAILED la publicación del paso 1,
# como si se hubiera perdido la respuesta del receptor.
if [[ -n $primero ]]; then
    sql "update eventos_distributivo.event_publication set status = 'FAILED', completion_date = null where serialized_event like '%$primero%'" >/dev/null
    verificar 4 "Entrega repetida: la publicación vuelve a COMPLETED y sigue 1 solo registro en menos de $ESPERA_REENVIO s" \
        esperar "$ESPERA_REENVIO" completada_y_recibida_una_vez "$primero"
else
    verificar 4 "Entrega repetida: no hay evento del paso 1 para repetir" false
fi

# 5. El receptor no se alcanza a través del API Gateway (FR-012, SC-006). El token del paso 0 ya
# no vale: identidad arrancó con una llave nueva (ADR-014).
antes=$(sql "select count(*) from usuarios.evento_recibido")
evento_externo=$(jq -cn --arg id "$(cat /proc/sys/kernel/random/uuid)" \
    '{idEvento: $id, tipo: "generacion.EventoDePrueba", origen: "generacion", momento: "2026-10-14T00:00:00Z", contenido: {}}')
pedir POST "$GATEWAY/interno/usuarios/eventos" "" "$evento_externo"
sin_token=$estado
if iniciar_sesion; then
    pedir POST "$GATEWAY/interno/usuarios/eventos" "Bearer $token" "$evento_externo"
    con_token=$estado
else
    con_token="sin sesión"
fi
despues=$(sql "select count(*) from usuarios.evento_recibido")
verificar 5 "Receptor a través del API Gateway: 401 sin token y 403 con el de usuario, sin registrar nada (recibido: $sin_token y $con_token)" \
    test "$sin_token" == 401 -a "$con_token" == 403 -a "$antes" == "$despues"

if [[ $fallas -eq 0 ]]; then
    echo "Eventos de punta a punta: todos los pasos pasan."
    exit 0
fi
echo "Eventos de punta a punta: $fallas paso(s) fallan."
exit 1
