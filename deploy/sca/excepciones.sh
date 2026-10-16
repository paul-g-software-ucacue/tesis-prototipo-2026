#!/usr/bin/env bash
# Valida las excepciones de Grype y deja solo las vigentes (B00-H07, D-54;
# specs/008-secretos-sca-sbom/contracts/sca.md).
# Uso: deploy/sca/excepciones.sh <entrada> <salida> [<hoy>]
#   <entrada>  el .grype.yaml del repositorio;
#   <salida>   la configuración que recibe Grype con -c, solo con las excepciones vigentes;
#   <hoy>      fecha AAAA-MM-DD, para las pruebas; por defecto, la fecha UTC del día.
# Cada excepción necesita vulnerability, package.name, reason, desde y vence, con vence como máximo
# 30 días después de desde. Si alguna es inválida, imprime ::error:: y termina con 1 sin escribir la
# salida. Una vencida (hoy > vence) se quita con un ::warning::, así que Grype vuelve a revisarla.
set -euo pipefail

if (($# < 2 || $# > 3)); then
    echo "Uso: $0 <entrada> <salida> [<hoy>]" >&2
    exit 2
fi
readonly ENTRADA=$1 SALIDA=$2 HOY=${3:-$(date -u +%F)}
readonly MAXIMO_DIAS=30

if ! command -v yq >/dev/null; then
    echo "::error::Falta yq 4, que lee las excepciones de Grype." >&2
    exit 1
fi

fecha_valida() {
    [[ $1 =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]] && [[ $(date -u -d "$1" +%F 2>/dev/null) == "$1" ]]
}
dias_entre() { echo $((($(date -u -d "$2" +%s) - $(date -u -d "$1" +%s)) / 86400)); }

if ! fecha_valida "$HOY"; then
    echo "::error::La fecha de hoy ($HOY) no tiene la forma AAAA-MM-DD." >&2
    exit 2
fi

total=$(yq '.ignore // [] | length' "$ENTRADA")
invalidas=0
vencidas=()
for ((i = 0; i < total; i++)); do
    campo() { yq ".ignore[$i].$1 // \"\"" "$ENTRADA"; }
    id=$(campo vulnerability)
    paquete=$(campo package.name)
    motivo=$(campo reason)
    desde=$(campo desde)
    vence=$(campo vence)
    nombre=${id:-"entrada $((i + 1))"}

    problema=""
    if [[ -z $id ]]; then
        problema="falta vulnerability"
    elif [[ -z $paquete ]]; then
        problema="falta package.name"
    elif [[ -z ${motivo//[[:space:]]/} ]]; then
        problema="falta reason (el motivo)"
    elif ! fecha_valida "$desde"; then
        problema="falta desde o no es una fecha AAAA-MM-DD"
    elif ! fecha_valida "$vence"; then
        problema="falta vence o no es una fecha AAAA-MM-DD"
    elif (($(dias_entre "$desde" "$vence") < 0)); then
        problema="vence ($vence) es anterior a desde ($desde)"
    elif (($(dias_entre "$desde" "$vence") > MAXIMO_DIAS)); then
        problema="vence $(dias_entre "$desde" "$vence") días después de desde; el máximo es $MAXIMO_DIAS (D-54)"
    fi

    if [[ -n $problema ]]; then
        echo "::error::Excepción de Grype inválida ($nombre): $problema."
        invalidas=$((invalidas + 1))
    elif [[ $HOY > $vence ]]; then
        echo "::warning::La excepción de $id ($paquete) venció el $vence; Grype vuelve a revisarla (D-54)."
        vencidas+=("$i")
    else
        echo "Excepción vigente: $id ($paquete), vence el $vence."
    fi
done

if ((invalidas > 0)); then
    echo "Hay $invalidas excepciones inválidas en $ENTRADA." >&2
    exit 1
fi

# Se borran de atrás hacia adelante para que los índices sigan valiendo.
cp "$ENTRADA" "$SALIDA"
yq -i '.ignore = (.ignore // [])' "$SALIDA"
for ((j = ${#vencidas[@]} - 1; j >= 0; j--)); do
    yq -i "del(.ignore[${vencidas[j]}])" "$SALIDA"
done
echo "$((total - ${#vencidas[@]})) de $total excepciones vigentes."
