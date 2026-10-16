#!/usr/bin/env bash
# Comprueba que cada módulo del backend tiene su SBOM CycloneDX y que ninguno está vacío (B00-H07,
# specs/008-secretos-sca-sbom/contracts/sca.md). Uso, desde cualquier directorio, después de
# `./gradlew cyclonedxDirectBom`: deploy/sca/verificar_sbom.sh
# Los módulos esperados son los directorios de backend/ con su propio build.gradle, sin la raíz,
# buildSrc/ ni plantillas/: un módulo nuevo entra solo. Termina con 0 si todo está bien y con 1 si
# falta un SBOM, sobra uno o alguno está vacío.
set -euo pipefail
cd "$(dirname "$0")/../../backend"

readonly REPORTE=build/reports/cyclonedx-direct/bom.json

mapfile -t esperados < <(find . -mindepth 2 -name build.gradle \
    -not -path './buildSrc/*' -not -path './plantillas/*' -not -path '*/build/*' \
    -printf '%h\n' | sed 's|^\./||' | sort)
mapfile -t encontrados < <(find . -path "*/$REPORTE" -not -path './buildSrc/*' \
    | sed "s|^\./||; s|/$REPORTE\$||; s|^$REPORTE\$|.|" | sort)

fallas=0
for modulo in "${esperados[@]}"; do
    archivo="$modulo/$REPORTE"
    if [[ ! -f $archivo ]]; then
        echo "FALTA  $modulo: no tiene $REPORTE"
        fallas=$((fallas + 1))
    elif [[ $(jq '.components | length' "$archivo") -eq 0 ]]; then
        echo "VACÍO  $modulo: el SBOM no tiene componentes"
        fallas=$((fallas + 1))
    else
        printf 'OK     %-28s %s (%s componentes)\n' "$modulo" \
            "$(jq -r '.metadata.component.name' "$archivo")" "$(jq '.components | length' "$archivo")"
    fi
done
for modulo in $(comm -13 <(printf '%s\n' "${esperados[@]}") <(printf '%s\n' "${encontrados[@]}")); do
    echo "SOBRA  $modulo: no es un módulo con código y no debería tener SBOM"
    fallas=$((fallas + 1))
done

echo "${#esperados[@]} módulos esperados, ${#encontrados[@]} SBOM encontrados."
if ((fallas > 0)); then
    echo "Fallan $fallas comprobaciones de SBOM." >&2
    exit 1
fi
