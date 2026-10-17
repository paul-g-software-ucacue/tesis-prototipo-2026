#!/usr/bin/env bash
# Publica una versión del MDD en un registro (B00-H08, D-58; specs/009-publicacion-despliegue/
# contracts/publicacion.md). Uso, con la sesión ya iniciada en el registro:
#   deploy/publicacion/publicar.sh <repositorio> <commit>
#   (por ejemplo: docker.io/p980/mdd, o localhost:5000/mdd en probar_equipo.sh)
# Sube, en este orden, las 6 imágenes como <componente>-<commit12>, el paquete como
# despliegue-<commit12> y, por último, mueve despliegue-main: el equipo de demostración solo mira esa
# etiqueta, así que nunca ve una versión a medias. Si un push falla, despliegue-main no se mueve y el
# script termina con 1. Imprime una tabla Markdown con las etiquetas y sus digest.
set -euo pipefail

readonly COMPONENTES=(identidad datos distributivo salida gateway frontend despliegue)

repositorio=${1:?Uso: publicar.sh <repositorio> <commit>}
commit=${2:?Uso: publicar.sh <repositorio> <commit>}
corto=${commit:0:12}

subir() {  # subir <imagen local> <etiqueta remota>
    docker tag "$1" "$repositorio:$2"
    docker push --quiet "$repositorio:$2" >/dev/null
    # Docker guarda los digest sin el prefijo docker.io/: se busca el de este repositorio.
    digest=$(docker inspect --format '{{range .RepoDigests}}{{println .}}{{end}}' "$repositorio:$2" \
        | grep -m1 "^${repositorio#docker.io/}@" || true)
    # Las comillas invertidas son de Markdown, no una sustitución de comandos.
    # shellcheck disable=SC2016
    printf '| `%s` | `%s` |\n' "$2" "${digest#*@}" >>"$tabla"
}

tabla=$(mktemp)
trap 'rm -f "$tabla"' EXIT
printf '| Etiqueta | Digest |\n|---|---|\n' >"$tabla"

for componente in "${COMPONENTES[@]}"; do
    subir "mdd/$componente:$corto" "$componente-$corto"
done
# Al final, cuando las 7 etiquetas del commit ya están: el punto de publicación (research §4).
subir "mdd/despliegue:$corto" despliegue-main

echo "### Publicación de \`$corto\` en \`$repositorio\`"
echo
cat "$tabla"
