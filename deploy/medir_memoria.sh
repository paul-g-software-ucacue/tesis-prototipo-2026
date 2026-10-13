#!/usr/bin/env bash
# Medición de memoria de R-19 (D-34, D-36; specs/002-compose-cuatro-procesos/contracts/medicion-r19.md).
# Uso, desde cualquier directorio: deploy/medir_memoria.sh [--con-sesion] > medicion.md
#
# Levanta el perfil si hace falta, hace el calentamiento (20 llamadas a la salud de cada componente
# Java y 30 s de espera), toma una lectura de docker stats de los contenedores del proyecto e
# imprime la tabla de evidencia en Markdown.
# Con --con-sesion (B00-H03, D-34), antes del calentamiento inicia sesión con el usuario de prueba
# a través del API Gateway y suma 20 llamadas a la sesión actual de cada proceso, con el token
# (specs/003-autenticacion-jwt/contracts/medicion-r19-sesion.md). Nunca imprime el token.
# Sale con 0 si el total es menor que 8 GiB, 1 si llega a 8 GiB o más y 2 si algo no está sano.
set -euo pipefail
cd "$(dirname "$0")/.."

readonly UMBRAL_MIB=8192   # 8 GiB: el equipo de despliegue (D-36)
readonly ALERTA_MIB=7168   # 7 GiB: disparador de R-19
readonly LLAMADAS=20
readonly ESPERA=30
readonly ORDEN=(postgres identidad datos distributivo salida gateway)
readonly JAVA=(identidad datos distributivo salida gateway)
# El mismo comando del healthcheck de compose.yaml.
readonly SALUD="exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /actuator/health/readiness HTTP/1.0\r\nHost: localhost\r\n\r\n' >&3 && grep -q '\"status\":\"UP\"' <&3"
readonly GATEWAY="${MDD_GATEWAY:-http://localhost:8080}"
# Un servicio con «sesión actual» por proceso (D-39).
readonly SESIONES=(usuarios cargas generacion dashboard)

con_sesion=false
case "${1:-}" in
    --con-sesion) con_sesion=true ;;
    "") ;;
    *) echo "Uso: deploy/medir_memoria.sh [--con-sesion]" >&2; exit 2 ;;
esac

no_sano() {
    echo "No está sano: $1" >&2
    docker compose ps >&2
    exit 2
}

# 2944 → «2 944», como en el contrato.
miles() {
    sed -E ':a; s/([0-9])([0-9]{3})($|[^0-9])/\1 \2\3/; ta' <<<"$1"
}

# Convierte «123.4MiB», «1.2GiB», «800KiB» o «512B» a MiB.
a_mib() {
    awk -v v="$1" 'BEGIN {
        n = v; sub(/[A-Za-z]+$/, "", n); u = v; sub(/^[0-9.]+/, "", u)
        f = (u == "GiB") ? 1024 : (u == "MiB") ? 1 : (u == "KiB" || u == "kB") ? 1 / 1024 : 1 / 1048576
        printf "%.1f", n * f
    }'
}

if ! docker compose up -d --wait >&2; then
    no_sano "docker compose up --wait falló"
fi

# La contraseña del usuario de prueba: la de .env si existe, si no la de .env.ejemplo.
clave_de_prueba() {
    local archivo linea valor=""
    for archivo in .env.ejemplo .env; do
        [[ -f $archivo ]] || continue
        linea=$(grep -E '^MDD_USUARIO_PRUEBA_CLAVE=' "$archivo" | tail -1 || true)
        [[ -n $linea ]] && valor=${linea#*=}
    done
    printf '%s' "$valor"
}

token=""
if [[ $con_sesion == true ]]; then
    credenciales=$(jq -cn --arg c "$(clave_de_prueba)" '{usuario: "autor.prueba", clave: $c}')
    token=$(curl -sf -H 'Content-Type: application/json' --data "$credenciales" \
        "$GATEWAY/api/usuarios/sesion" | jq -r '.token // empty' || true)
    [[ -n $token ]] || no_sano "inicio de sesión del usuario de prueba"
fi

for componente in "${JAVA[@]}"; do
    for _ in $(seq "$LLAMADAS"); do
        docker compose exec -T "$componente" bash -c "$SALUD" || no_sano "$componente (calentamiento)"
    done
done
if [[ $con_sesion == true ]]; then
    for servicio in "${SESIONES[@]}"; do
        for _ in $(seq "$LLAMADAS"); do
            curl -sf -o /dev/null -H "Authorization: Bearer $token" "$GATEWAY/api/$servicio/sesion" \
                || no_sano "sesión actual de $servicio (calentamiento)"
        done
    done
fi
sleep "$ESPERA"

while read -r servicio salud; do
    [[ $salud == healthy ]] || no_sano "$servicio ($salud)"
done < <(docker compose ps --format '{{.Service}} {{.Health}}')

declare -A servicio_de consumo limite
while IFS=';' read -r nombre servicio; do
    servicio_de[$nombre]=$servicio
done < <(docker compose ps --format '{{.Name}};{{.Service}}')

# Una sola lectura, solo de los contenedores de este proyecto.
mapfile -t ids < <(docker compose ps -q)
while IFS=';' read -r nombre uso; do
    servicio=${servicio_de[$nombre]}
    consumo[$servicio]=$(a_mib "${uso%% / *}")
done < <(docker stats --no-stream --format '{{.Name}};{{.MemUsage}}' "${ids[@]}")
for id in "${ids[@]}"; do
    nombre=$(docker inspect --format '{{.Name}}' "$id")
    bytes=$(docker inspect --format '{{.HostConfig.Memory}}' "$id")
    limite[${servicio_de[${nombre#/}]}]=$((bytes / 1048576))
done

version=$(docker version --format '{{.Server.Version}}')
if [[ ${GITHUB_ACTIONS:-} == true ]]; then
    entorno="ubuntu-latest (Docker $version)"
else
    entorno="local (Docker $version)"
fi

total=0
total_limites=0
filas=""
for servicio in "${ORDEN[@]}"; do
    [[ -n ${consumo[$servicio]:-} ]] || no_sano "$servicio (sin lectura de docker stats)"
    total=$(awk -v a="$total" -v b="${consumo[$servicio]}" 'BEGIN { printf "%.1f", a + b }')
    total_limites=$((total_limites + limite[$servicio]))
    filas+="| $servicio | ${consumo[$servicio]/./,} | ${limite[$servicio]} |"$'\n'
done
total_gib=$(awk -v t="$total" 'BEGIN { printf "%.2f", t / 1024 }')

if awk -v t="$total" -v u="$UMBRAL_MIB" 'BEGIN { exit !(t < u) }'; then
    resultado="cumple (< 8 GiB)"
    salida=0
else
    resultado="falla (≥ 8 GiB)"
    salida=1
fi
if awk -v t="$total" -v a="$ALERTA_MIB" 'BEGIN { exit !(t > a) }'; then
    alerta="Alerta R-19: más de 7 GiB."
else
    alerta="Sin alerta de R-19 (≤ 7 GiB)."
fi

titulo="Medición de R-19"
calentamiento="$LLAMADAS llamadas a la salud por componente y $ESPERA s de espera."
if [[ $con_sesion == true ]]; then
    titulo="Medición de R-19 después de iniciar sesión"
    calentamiento="inicio de sesión de autor.prueba, $LLAMADAS llamadas a la salud y $LLAMADAS a la sesión actual por componente, y $ESPERA s de espera."
fi

cat <<EOF
**$titulo** · $(date --iso-8601=seconds) · commit \`$(git rev-parse --short=7 HEAD)\` · entorno: $entorno
Calentamiento: $calentamiento

| Contenedor | Consumo (MiB) | Límite (MiB) |
|---|---:|---:|
${filas}| **Total** | **$(miles "${total/./,}")** (${total_gib/./,} GiB) | $(miles "$total_limites") |

Resultado: $resultado. $alerta
EOF
exit "$salida"
