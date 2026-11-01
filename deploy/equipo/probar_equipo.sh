#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar, y las de varios pasos con eval '…' para que se
# evalúen al verificar, no al llamar: shellcheck no lo ve.
# shellcheck disable=SC2329,SC2016,SC2034
# Prueba de punta a punta del equipo de demostración, sin Docker Hub ni GitHub (B00-H08, escenarios
# 3, 4 y 5, D-56; specs/009-publicacion-despliegue/research.md §12). Uso, desde cualquier directorio:
#   deploy/equipo/probar_equipo.sh
# Levanta un registro local y una API de GitHub simulada, prepara un equipo en una carpeta temporal
# y comprueba: sin versión publicada (0), despliegue sano (1 a 5), llave persistente (6), acceso por
# el sitio del túnel (7), vuelta atrás con una versión rota (8), un primer despliegue roto (9) y las
# contraseñas de los usuarios de la demo (11, F05-H01).
# Usa el proyecto de Compose «mdd-prueba» y el puerto 8443: nunca toca el equipo real («mdd»).
# Termina con 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime un secreto.
set -euo pipefail
cd "$(dirname "$0")/../.."

readonly PROYECTO=mdd-prueba
readonly PUERTO_PRUEBA=8443
readonly PUERTO_GITHUB=18765
readonly TOKEN_GITHUB=token-de-prueba
readonly REGISTRO=127.0.0.1:5000
readonly REPOSITORIO_PRUEBA=localhost:5000/mdd
readonly IMAGEN_REGISTRO=registry:2.8.3@sha256:a3d8aaa63ed8681a604f1dea0aa03f100d5895b6a58ace528858a7b332415373
readonly IMAGEN_CURL=curlimages/curl:8.22.0@sha256:58adaa4e8dca9c988bae2aba4ab3434a0bb2da16bbe3f92dec39ec7785166777
readonly COMPONENTES=(identidad datos distributivo salida gateway frontend caddy postgres)
readonly ESPERA_SANO=${ESPERA_SANO:-240}   # versión sana: margen para un runner lento
readonly ESPERA_ROTO=${ESPERA_ROTO:-60}    # versión rota: no hace falta esperar los 180 s reales

temporal=$(mktemp -d)
readonly TEMPORAL=$temporal
readonly GITHUB_REGISTRO="$TEMPORAL/github.jsonl"
touch "$GITHUB_REGISTRO"

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

# --- Preparación ---------------------------------------------------------------------------------

limpiar() {
    local raiz
    for raiz in "$TEMPORAL"/equipo*; do
        [[ -f $raiz/.env ]] || continue
        docker compose -p "$PROYECTO" --env-file "$raiz/.env" \
            -f "$(readlink -f "$raiz/actual" 2>/dev/null || echo "$PWD/deploy/equipo")/compose.demo.yml" \
            down -v --remove-orphans >/dev/null 2>&1 || true
    done
    docker rm -f mdd-registro-prueba >/dev/null 2>&1 || true
    [[ -n ${pid_github:-} ]] && kill "$pid_github" 2>/dev/null || true
    rm -rf "$TEMPORAL" 2>/dev/null || true
}
trap limpiar EXIT

docker rm -f mdd-registro-prueba >/dev/null 2>&1 || true
docker run -d --name mdd-registro-prueba -p "$REGISTRO:5000" "$IMAGEN_REGISTRO" >/dev/null
python3 -I deploy/equipo/github_simulado.py "$PUERTO_GITHUB" "$TOKEN_GITHUB" "$GITHUB_REGISTRO" &
pid_github=$!
for _ in $(seq 30); do curl -sf "http://$REGISTRO/v2/" >/dev/null && break; sleep 1; done

# Commit A: el de HEAD. Se construye siempre (con la caché es rápido), para que el paquete lleve los
# archivos de despliegue del árbol actual y no los de una construcción anterior del mismo commit.
commit_a=$(git rev-parse HEAD)
deploy/publicacion/construir.sh "$commit_a" >/dev/null
# Commit B: otro identificador con las mismas imágenes y un paquete cuyo identidad no queda sano.
commit_b=$(printf 'version-rota-%s' "$commit_a" | sha1sum | cut -c1-40)

preparar_equipo() {  # preparar_equipo <carpeta>
    MDD_PROYECTO=$PROYECTO PUERTO_HTTPS=$PUERTO_PRUEBA REPOSITORIO=$REPOSITORIO_PRUEBA AUTENTICACION="" \
        DOCKERHUB_USUARIO="" DOCKERHUB_TOKEN="" DEMO_CLAVE="$(openssl rand -hex 16)" \
        GITHUB_REPOSITORIO=prueba/mdd GITHUB_TOKEN=$TOKEN_GITHUB \
        GITHUB_API="http://127.0.0.1:$PUERTO_GITHUB" \
        deploy/equipo/preparar.sh --no-interactivo --raiz "$1" >/dev/null
}

actualizar() {  # actualizar <carpeta> <espera>; deja el código en $codigo y la salida en $salida
    codigo=0
    salida=$(RAIZ=$1 ESPERA_SALUD=$2 deploy/equipo/actualizar.sh 2>&1) || codigo=$?
    printf '\n=== actualizar.sh (%s, código %s)\n%s\n' "$1" "$codigo" "$salida" >>"$TEMPORAL/actualizar.log"
}

# Valor de una variable del .env de un equipo, sin ejecutarlo.
valor() {  # valor <carpeta> <variable>
    local linea
    linea=$(grep -E "^$2=" "$1/.env" | tail -1 || true)
    linea=${linea#*=}
    linea=${linea#\'}
    printf '%s' "${linea%\'}"
}

compose() {  # compose <carpeta> <argumentos de docker compose…>
    local raiz=$1
    shift
    docker compose -p "$PROYECTO" --env-file "$raiz/.env" \
        -f "$(readlink -f "$raiz/actual" 2>/dev/null || echo "$PWD/deploy/equipo")/compose.demo.yml" "$@"
}

todos_sanos() {  # todos_sanos <carpeta>
    local estados
    estados=$(compose "$1" ps --format '{{.Service}} {{.Health}}')
    [[ -n $estados ]] && ! awk '$2 != "healthy" && $2 != ""' <<<"$estados" | grep -q .
}

informes() {  # informes <commit> <estado>: cuántos estados <estado> llegaron para ese commit
    jq -s --arg ref "$1" --arg estado "$2" '
        (map(select(.tipo == "deployment" and .ref == $ref) | .id)) as $ids
        | map(select(.tipo == "estado" and (.deployment as $d | $ids | index($d)) and .state == $estado))
        | length' "$GITHUB_REGISTRO"
}

descripcion_de() {  # descripcion_de <commit> <estado>
    jq -rs --arg ref "$1" --arg estado "$2" '
        (map(select(.tipo == "deployment" and .ref == $ref) | .id)) as $ids
        | map(select(.tipo == "estado" and (.deployment as $d | $ids | index($d)) and .state == $estado))
        | last | .description // ""' "$GITHUB_REGISTRO"
}

lineas_github() { wc -l <"$GITHUB_REGISTRO" | tr -d ' '; }

publicar_rota() {  # paquete B: el mismo de A, con identidad apuntando a una base que no existe
    local contexto="$TEMPORAL/contexto-b" componente
    mkdir -p "$contexto/deploy/equipo" "$contexto/deploy/postgres" "$contexto/deploy/docker"
    cp deploy/Caddyfile deploy/desplegar.sh "$contexto/deploy/"
    cp deploy/equipo/probar_humo.sh "$contexto/deploy/equipo/"
    cp deploy/postgres/01-esquemas.sh "$contexto/deploy/postgres/"
    cp deploy/docker/despliegue.Dockerfile "$contexto/deploy/docker/"
    awk '
        /^  identidad:/ { en_identidad = 1 }
        /^  datos:/ { en_identidad = 0 }
        { print }
        en_identidad && /<<: \*jvm/ {
            print "      SPRING_DATASOURCE_URL: jdbc:postgresql://base-que-no-existe:5432/mdd"
        }
    ' deploy/equipo/compose.demo.yml >"$contexto/deploy/equipo/compose.demo.yml"
    docker build -q -f "$contexto/deploy/docker/despliegue.Dockerfile" --build-arg "REVISION=$commit_b" \
        -t "mdd/despliegue:${commit_b:0:12}" "$contexto" >/dev/null
    for componente in "${COMPONENTES[@]}"; do
        docker tag "mdd/$componente:${commit_a:0:12}" "mdd/$componente:${commit_b:0:12}"
    done
    deploy/publicacion/publicar.sh "$REPOSITORIO_PRUEBA" "$commit_b" >/dev/null
}

# --- Equipo con una versión sana -----------------------------------------------------------------

equipo="$TEMPORAL/equipo"
preparar_equipo "$equipo"
[[ $(valor "$equipo" MDD_PROYECTO) == "$PROYECTO" ]] || { echo "El equipo de prueba no usa $PROYECTO." >&2; exit 1; }

# 0. Todavía no hay una versión publicada (M5): se registra y no se informa a GitHub.
actualizar "$equipo" "$ESPERA_SANO"
verificar 0 "Sin versión publicada: código 0, sin despliegue y sin informes (código $codigo)" \
    eval '[[ $codigo == 0 && $(lineas_github) == 0 ]] && grep -qi "sin versión publicada" <<<"$salida"'

# 1 a 4. Versión A: publicación, despliegue sano, humo e informes.
deploy/publicacion/publicar.sh "$REPOSITORIO_PRUEBA" "$commit_a" >/dev/null
actualizar "$equipo" "$ESPERA_SANO"
verificar 1 "Versión A: actualizar.sh termina con 0 (código $codigo)" test "$codigo" = 0
verificar 2 "Versión A: todos los contenedores sanos y «actual» apunta a A" \
    eval 'todos_sanos "$equipo" && [[ $(basename "$(readlink -f "$equipo/actual")") == ${commit_a:0:12} ]]'
verificar 3 "Versión A: se guardó el digest de despliegue-main" test -s "$equipo/estado/ultimo-digest"
verificar 4 "Versión A: GitHub recibió in_progress y success para el commit A" \
    eval '[[ $(informes "$commit_a" in_progress) == 1 && $(informes "$commit_a" success) == 1 ]]'

# 5. Sin cambios en el registro, no despliega ni informa nada.
antes=$(lineas_github)
actualizar "$equipo" "$ESPERA_SANO"
verificar 5 "Sin cambios: código 0 y ningún informe nuevo" \
    eval '[[ $codigo == 0 && $(lineas_github) == "$antes" ]]'

# 6. Llave persistente (SC-007, FR-012): el mismo token sirve después de reiniciar identidad.
ruta_sesion="https://mdd.localhost:$PUERTO_PRUEBA/api/usuarios/sesion"
opciones_curl=(-sk --max-time 15 --resolve "mdd.localhost:$PUERTO_PRUEBA:127.0.0.1")
# docente.demo, con su contraseña de secretos/claves-demo (D-59); autor.prueba ya no entra (D-57).
clave_docente=$(awk -F= '$1 == "docente.demo" { sub(/^[^=]*=/, ""); print; exit }' "$equipo/secretos/claves-demo")
credenciales=$(jq -cn --arg c "$clave_docente" '{usuario: "docente.demo", clave: $c}')
token=$(curl "${opciones_curl[@]}" -X POST -H 'Content-Type: application/json' --data @- "$ruta_sesion" \
    <<<"$credenciales" | jq -r '.token // empty' 2>/dev/null || true)
compose "$equipo" restart identidad >/dev/null 2>&1 || true
for _ in $(seq 40); do todos_sanos "$equipo" && break; sleep 5; done
estado_sesion=$(curl "${opciones_curl[@]}" -o /dev/null -w '%{http_code}' -K - "$ruta_sesion" \
    <<<"header = \"Authorization: Bearer $token\"" || echo 000)
verificar 6 "Después de reiniciar identidad, el mismo token sigue valiendo (recibido: $estado_sesion)" \
    eval '[[ -n $token && $estado_sesion == 200 ]]'

# 10. Memoria del equipo después de iniciar sesión (SC-010, R-19): por debajo de la alerta de 7 GiB.
memoria=$(docker stats --no-stream --format '{{.Name}} {{.MemUsage}}' | awk -v p="$PROYECTO-" '
    index($1, p) == 1 {
        split($2, partes, "/"); valor = partes[1]
        if (valor ~ /GiB$/) { sub(/GiB$/, "", valor); total += valor * 1024 }
        else if (valor ~ /MiB$/) { sub(/MiB$/, "", valor); total += valor }
        else if (valor ~ /KiB$/) { sub(/KiB$/, "", valor); total += valor / 1024 }
    }
    END { printf "%.1f", total }')
verificar 10 "Memoria del equipo después de iniciar sesión: $memoria MiB (alerta: 7 168)" \
    eval 'awk -v m="$memoria" "BEGIN { exit !(m > 0 && m < 7168) }"'

# 7. Sitio del túnel (FR-016): http://caddy:8081 pide credenciales y deja pasar /api.
red="${PROYECTO}_default"
sin_credenciales=$(docker run --rm --network "$red" "$IMAGEN_CURL" -s -o /dev/null -w '%{http_code}' http://caddy:8081/ || echo 000)
con_credenciales=$(printf 'user = "%s:%s"\n' "$(valor "$equipo" DEMO_USUARIO)" "$(valor "$equipo" DEMO_CLAVE)" \
    | docker run --rm -i --network "$red" "$IMAGEN_CURL" -s -o /dev/null -w '%{http_code}' -K - http://caddy:8081/ || echo 000)
# /api no pasa por basic_auth (la protege el token, ADR-014): el inicio de sesión responde sin las credenciales de la demo.
api=$(docker run --rm -i --network "$red" "$IMAGEN_CURL" -s -o /dev/null -w '%{http_code}' -X POST \
    -H 'Content-Type: application/json' --data @- http://caddy:8081/api/usuarios/sesion <<<"$credenciales" || echo 000)
verificar 7 "Sitio del túnel: 401 sin credenciales, 200 con ellas y /api sin basic_auth ($sin_credenciales/$con_credenciales/$api)" \
    eval '[[ $sin_credenciales == 401 && $con_credenciales == 200 && $api == 200 ]]'

# 11. Contraseñas de la demo (D-59): una por usuario de usuarios-demo.yaml, legibles por la prueba de
# humo; volver a correr preparar.sh agrega la que falte sin cambiar las demás. Lo mismo con las
# claves de proceso del .env que agregan las versiones nuevas (MDD_CLAVE_PROCESO_DATOS, F05-H03).
usuarios_en_archivo=$(awk '/^  - usuario: / { print $3 }' backend/servicios/usuarios/src/main/resources/usuarios-demo.yaml | sort)
antes_claves=$(grep -v '^docente.coordinador.demo=' "$equipo/secretos/claves-demo")
chmod u+w "$equipo/secretos/claves-demo"
printf '%s\n' "$antes_claves" >"$equipo/secretos/claves-demo"
antes_env=$(grep -v '^MDD_CLAVE_PROCESO_DATOS=' "$equipo/.env")
printf '%s\n' "$antes_env" >"$equipo/.env"
preparar_equipo "$equipo"
verificar 11 "claves-demo: una contraseña por usuario de la demo, preparar.sh agrega la que falta y no cambia las demás" \
    eval '[[ $(cut -d= -f1 "$equipo/secretos/claves-demo" | sort) == "$usuarios_en_archivo" ]] \
        && [[ $(grep -v "^docente.coordinador.demo=" "$equipo/secretos/claves-demo") == "$antes_claves" ]] \
        && [[ -r $equipo/secretos/claves-demo && $(stat -c %a "$equipo/secretos/claves-demo") == 444 ]]'
verificar 11 ".env: preparar.sh agrega MDD_CLAVE_PROCESO_DATOS si falta y no cambia las demás líneas" \
    eval '[[ $(grep -c "^MDD_CLAVE_PROCESO_DATOS=." "$equipo/.env") == 1 ]] \
        && [[ $(grep -v "^MDD_CLAVE_PROCESO_DATOS=" "$equipo/.env") == "$antes_env" ]]'

# --- Vuelta atrás ----------------------------------------------------------------------------------

# 8. Versión B rota: vuelve a A, todo sano otra vez, y GitHub recibe failure con la vuelta atrás.
publicar_rota
inicio_vuelta=$SECONDS
actualizar "$equipo" "$ESPERA_ROTO"
echo "       La vuelta atrás tardó $((SECONDS - inicio_vuelta)) s, con ESPERA_SALUD=$ESPERA_ROTO."
verificar 8 "Versión B rota: código 1, «actual» vuelve a A, todos sanos y failure con «Vuelta atrás» (código $codigo)" \
    eval '[[ $codigo == 1 ]] && todos_sanos "$equipo" \
        && [[ $(basename "$(readlink -f "$equipo/actual")") == ${commit_a:0:12} ]] \
        && [[ $(informes "$commit_b" failure) == 1 ]] \
        && grep -q "Vuelta atrás a ${commit_a:0:12}" <<<"$(descripcion_de "$commit_b" failure)"'

# 9. Primer despliegue de un equipo nuevo con B: sin versión anterior, failure sin vuelta atrás.
compose "$equipo" down -v --remove-orphans >/dev/null 2>&1 || true
nuevo="$TEMPORAL/equipo-nuevo"
preparar_equipo "$nuevo"
actualizar "$nuevo" "$ESPERA_ROTO"
verificar 9 "Primer despliegue roto: código 1, sin «anterior» y failure sin vuelta atrás (código $codigo)" \
    eval '[[ $codigo == 1 && ! -e $nuevo/anterior && $(informes "$commit_b" failure) == 2 ]] \
        && ! grep -q "Vuelta atrás" <<<"$(descripcion_de "$commit_b" failure)"'

if ((fallas > 0)); then
    echo "Fallaron $fallas pasos. Final de la salida de actualizar.sh:" >&2
    tail -n 60 "$TEMPORAL/actualizar.log" >&2
    exit 1
fi
echo "El equipo de demostración se despliega, vuelve atrás e informa a GitHub."
