#!/usr/bin/env bash
# Resumen de Grype para el job 4 · sca y decisión del check (B00-H07, FR-011 y FR-012, D-54;
# specs/008-secretos-sca-sbom/contracts/sca.md).
# Uso: deploy/sca/resumen.sh <configuración de Grype> <módulo>.json...
#   <configuración de Grype>  la que generó excepciones.sh (para el vencimiento de las suprimidas);
#   <módulo>.json             la salida -o json de Grype para el SBOM de un módulo; el nombre del
#                             archivo, sin .json, es el nombre del módulo.
# Escribe en Markdown, en la salida estándar, tres tablas: las vulnerabilidades altas o críticas con
# versión corregida (bloquean), las altas o críticas sin arreglo todavía (informativas) y las
# suprimidas por una excepción vigente. Termina con 1 si hay alguna que bloquea y con 0 si no.
set -euo pipefail

if (($# < 2)); then
    echo "Uso: $0 <configuración de Grype> <módulo>.json..." >&2
    exit 2
fi
configuracion=$1
shift

excepciones=$(yq -o=json '.ignore // []' "$configuracion")

datos=$(jq -n --argjson excepciones "$excepciones" '
    def alta: .vulnerability.severity == "High" or .vulnerability.severity == "Critical";
    def fila($modulo): {
        modulo: $modulo,
        dependencia: .artifact.name,
        instalada: .artifact.version,
        corregida: (.vulnerability.fix.versions // [] | join(", ")),
        id: .vulnerability.id,
        severidad: .vulnerability.severity
    };
    def vence($id; $paquete):
        [$excepciones[] | select(.vulnerability == $id and .package.name == $paquete) | .vence]
        | first // "";
    [inputs | (input_filename | split("/") | last | sub("\\.json$"; "")) as $modulo | {
        bloquean: [.matches[] | select(alta and .vulnerability.fix.state == "fixed") | fila($modulo)],
        sin_arreglo: [.matches[] | select(alta and .vulnerability.fix.state != "fixed") | fila($modulo)],
        suprimidas: [(.ignoredMatches // [])[] | select(alta)
            | fila($modulo) + {vence: vence(.vulnerability.id; .artifact.name)}],
        otras: [.matches[] | select(alta | not)] | length
    }]
    | {
        modulos: length,
        bloquean: (map(.bloquean) | add // []),
        sin_arreglo: (map(.sin_arreglo) | add // []),
        suprimidas: (map(.suprimidas) | add // []),
        otras: (map(.otras) | add // 0)
    }' "$@")

jq -r '
    def tabla($filas; $extra):
        if ($filas | length) == 0 then "Ninguna.\n"
        else
            "| Módulo | Dependencia | Instalada | Corregida en | Vulnerabilidad | Severidad |"
            + (if $extra then " Vence |" else "" end) + "\n"
            + "|---|---|---|---|---|---|" + (if $extra then "---|" else "" end) + "\n"
            + ($filas | map("| \(.modulo) | \(.dependencia) | \(.instalada) | \(.corregida) | \(.id) | \(.severidad) |"
                + (if $extra then " \(.vence) |" else "" end)) | join("\n")) + "\n"
        end;
    "## Grype: dependencias de \(.modulos) módulos\n",
    (if (.bloquean | length) == 0
     then "Ninguna vulnerabilidad alta o crítica con versión corregida.\n"
     else "### Bloquean la fusión: actualizar a la versión corregida\n\n" + tabla(.bloquean; false) end),
    "### Altas o críticas sin arreglo todavía (no bloquean)\n\n" + tabla(.sin_arreglo; false),
    "### Suprimidas por una excepción vigente (D-54)\n\n" + tabla(.suprimidas; true),
    "Medias y bajas: \(.otras), en la tabla del log de cada módulo."
' <<<"$datos"

[[ $(jq '.bloquean | length' <<<"$datos") -eq 0 ]]
