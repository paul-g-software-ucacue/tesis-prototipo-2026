#!/usr/bin/env bash
# Las condiciones se invocan a través de verificar; shellcheck no lo ve.
# shellcheck disable=SC2329
# Prueba de deploy/sca/excepciones.sh con los casos de deploy/sca/casos/ (B00-H07, D-54;
# specs/008-secretos-sca-sbom/contracts/sca.md). Uso, desde cualquier directorio:
# deploy/sca/probar_excepciones.sh
# Corre cada caso con la fecha fija 2026-10-15 y comprueba el código de salida, las entradas que
# quedan para Grype y los avisos. Necesita yq 4. Termina con 0 si todos los pasos pasan y con 1 si
# alguno falla.
set -euo pipefail
cd "$(dirname "$0")"

readonly HOY=2026-10-15
temporal=$(mktemp -d)
trap 'rm -rf "$temporal"' EXIT

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

# Corre excepciones.sh sobre un caso; deja la salida del script en $temporal/<caso>.log, la
# configuración para Grype en $temporal/<caso>.yaml y el código de salida en $temporal/<caso>.rc.
correr() {
    local caso=$1 rc=0
    ./excepciones.sh "casos/$caso.yaml" "$temporal/$caso.yaml" "$HOY" >"$temporal/$caso.log" 2>&1 || rc=$?
    echo "$rc" >"$temporal/$caso.rc"
}

codigo() { [[ $(<"$temporal/$1.rc") == "$2" ]]; }
entradas() { [[ -f "$temporal/$1.yaml" && $(yq '.ignore | length' "$temporal/$1.yaml") == "$2" ]]; }
sin_salida() { [[ ! -e "$temporal/$1.yaml" ]]; }
dice() { grep -q -- "$2" "$temporal/$1.log"; }
queda() { yq -e ".ignore[] | select(.vulnerability == \"$2\")" "$temporal/$1.yaml" >/dev/null 2>&1; }
no_queda() { ! queda "$@"; }

for caso in vacia vigente borde vencida mixta sin-motivo sin-vence mas-de-30-dias; do
    correr "$caso"
done

verificar 1 "Sin excepciones: código 0 y ninguna entrada" \
    eval 'codigo vacia 0 && entradas vacia 0'
verificar 2 "Vigente: código 0 y la entrada queda para Grype" \
    eval 'codigo vigente 0 && entradas vigente 1 && queda vigente GHSA-35jh-r3h4-6jhm'
verificar 3 "Vence hoy: todavía vigente" \
    eval 'codigo borde 0 && entradas borde 1'
verificar 4 "Vencida: código 0, la entrada se quita y sale un aviso con su identificador" \
    eval 'codigo vencida 0 && entradas vencida 0 && dice vencida "::warning::.*GHSA-29mw-wpgm-hmr9"'
verificar 5 "Mixta: solo queda la vigente" \
    eval 'codigo mixta 0 && entradas mixta 1 && queda mixta GHSA-35jh-r3h4-6jhm && no_queda mixta GHSA-29mw-wpgm-hmr9'
verificar 6 "Sin motivo: código 1, error con el identificador y sin configuración" \
    eval 'codigo sin-motivo 1 && dice sin-motivo "::error::.*GHSA-35jh-r3h4-6jhm.*reason" && sin_salida sin-motivo'
verificar 7 "Sin vencimiento: código 1 y error que nombra vence" \
    eval 'codigo sin-vence 1 && dice sin-vence "::error::.*vence" && sin_salida sin-vence'
verificar 8 "Más de 30 días: código 1 y error que nombra el máximo de 30" \
    eval 'codigo mas-de-30-dias 1 && dice mas-de-30-dias "::error::.*30" && sin_salida mas-de-30-dias'

if ((fallas > 0)); then
    echo "Fallaron $fallas pasos." >&2
    for registro in "$temporal"/*.log; do
        echo "--- $(basename "$registro")" >&2
        cat "$registro" >&2
    done
    exit 1
fi
echo "Las excepciones de Grype se validan según D-54."
