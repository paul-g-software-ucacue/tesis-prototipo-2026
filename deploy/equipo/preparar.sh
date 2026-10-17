#!/usr/bin/env bash
# Prepara un equipo de demostración, una sola vez (B00-H08, D-55, D-56, D-57; specs/
# 009-publicacion-despliegue/research.md §11 y quickstart.md §2). Uso:
#   sudo deploy/equipo/preparar.sh                                  equipo real, en /opt/mdd
#   deploy/equipo/preparar.sh --no-interactivo --raiz <carpeta>     pruebas (probar_equipo.sh)
# Crea el usuario de sistema mdd (en el grupo docker) y la carpeta del equipo; genera en el equipo,
# sin mostrarlos, las claves de la base, la del proceso distributivo (D-49), la llave de firma RSA
# (ADR-014) y una contraseña por cada usuario de la demo (secretos/claves-demo, D-59); pide la
# contraseña del acceso de la demo y guarda su hash de Caddy; pide el usuario y el token de solo
# lectura de Docker Hub y el token de GitHub para los deployments (D-56); instala actualizar.sh y el
# temporizador de systemd. Nunca pisa un archivo que ya existe: para regenerar algo, se borra a mano
# y se vuelve a correr. La excepción es claves-demo: si el repositorio tiene usuarios de la demo
# nuevos, les agrega su contraseña sin tocar las demás.
# Variables de entrada, para no preguntar (y obligatorias con --no-interactivo): DEMO_CLAVE,
# DOCKERHUB_USUARIO, DOCKERHUB_TOKEN, GITHUB_TOKEN. Opcionales: REPOSITORIO (docker.io/<usuario>/mdd),
# GITHUB_REPOSITORIO, AUTENTICACION (auth.docker.io), GITHUB_API, MDD_PROYECTO (mdd), PUERTO_HTTPS
# (443), DIRECCION (mdd.localhost) y DEMO_USUARIO (demo).
set -euo pipefail
cd "$(dirname "$0")/../.."

readonly CADDY=caddy:2.11.7-alpine@sha256:d8542f48d34a9cf4e4c11a478865229840e87e4c96ea3f439101f31a5d35f75f
readonly UID_JAVA=10001   # usuario de las imágenes de Java (java.Dockerfile)
readonly USUARIOS_DEMO=backend/servicios/usuarios/src/main/resources/usuarios-demo.yaml

interactivo=true
raiz=/opt/mdd
while (($#)); do
    case $1 in
        --no-interactivo) interactivo=false ;;
        --raiz) raiz=${2:?--raiz necesita una carpeta}; shift ;;
        *) echo "Opción desconocida: $1" >&2; exit 2 ;;
    esac
    shift
done

if [[ $interactivo == true ]]; then
    [[ $(id -u) == 0 ]] || { echo "Correr con sudo: crea el usuario mdd e instala systemd." >&2; exit 1; }
    [[ $raiz == /opt/mdd ]] || { echo "El equipo real vive en /opt/mdd (las unidades de systemd lo suponen)." >&2; exit 1; }
fi
for herramienta in docker openssl jq curl; do
    command -v "$herramienta" >/dev/null || { echo "Falta $herramienta." >&2; exit 1; }
done

# Pide un valor sin eco si no llegó por variable; en modo no interactivo, es obligatorio.
pedir() {  # pedir <variable> <pregunta>
    local nombre=$1 pregunta=$2 valor_leido
    if [[ -n ${!nombre:-} ]]; then return 0; fi
    if [[ $interactivo == false ]]; then
        [[ ${!nombre+definida} ]] && return 0   # definida y vacía: válido en las pruebas
        echo "Falta $nombre." >&2
        exit 1
    fi
    read -rsp "$pregunta: " valor_leido
    echo
    printf -v "$nombre" '%s' "$valor_leido"
}

aleatorio() { openssl rand -hex 24; }

mkdir -p "$raiz"/{secretos,versiones,estado,bin,docker}
chmod 0750 "$raiz"
chmod 0700 "$raiz/secretos" "$raiz/docker"
umask 077

# --- .env: secretos de la aplicación (Compose y la prueba de humo) ---------------------------------
if [[ ! -e $raiz/.env ]]; then
    pedir DEMO_CLAVE "Contraseña del acceso de la demo (no se muestra)"
    if [[ $interactivo == true ]]; then
        read -rsp "Repítela: " repetida
        echo
        [[ $repetida == "$DEMO_CLAVE" ]] || { echo "Las contraseñas no coinciden." >&2; exit 1; }
    fi
    [[ ${#DEMO_CLAVE} -ge 12 ]] || { echo "La contraseña de la demo necesita al menos 12 caracteres." >&2; exit 1; }
    [[ $DEMO_CLAVE != *[\'\"\\]* && $DEMO_CLAVE != *$'\n'* ]] \
        || { echo "La contraseña de la demo no puede tener comillas, barras invertidas ni saltos de línea." >&2; exit 1; }
    pedir DOCKERHUB_USUARIO "Usuario de Docker Hub"
    hash_demo=$(printf '%s\n' "$DEMO_CLAVE" | docker run --rm -i "$CADDY" caddy hash-password)
    {
        echo "# Equipo de demostración del MDD (preparar.sh, $(date -u +%F)). Solo para este equipo."
        echo "MDD_PROYECTO=${MDD_PROYECTO:-mdd}"
        echo "REPOSITORIO=${REPOSITORIO:-docker.io/$DOCKERHUB_USUARIO/mdd}"
        echo "DIRECCION=${DIRECCION:-mdd.localhost}"
        echo "PUERTO_HTTPS=${PUERTO_HTTPS:-443}"
        echo "MDD_SECRETOS=$raiz/secretos"
        echo "POSTGRES_PASSWORD=$(aleatorio)"
        echo "DB_CLAVE_IDENTIDAD=$(aleatorio)"
        echo "DB_CLAVE_DATOS=$(aleatorio)"
        echo "DB_CLAVE_DISTRIBUTIVO=$(aleatorio)"
        echo "DB_CLAVE_SALIDA=$(aleatorio)"
        echo "MDD_CLAVE_PROCESO_DISTRIBUTIVO=$(aleatorio)"
        echo "DEMO_USUARIO=${DEMO_USUARIO:-demo}"
        echo "DEMO_CLAVE='$DEMO_CLAVE'"
        echo "DEMO_CLAVE_HASH='$hash_demo'"
        echo "# Token del túnel de Cloudflare (opcional, quickstart §5). Vacío: sin túnel."
        echo "TUNEL_TOKEN="
    } >"$raiz/.env"
    echo "Creado $raiz/.env."
else
    echo "Ya existe $raiz/.env: no se toca."
fi

# --- Llave de firma persistente (ADR-014) --------------------------------------------------------
if [[ ! -e $raiz/secretos/jwt.pem ]]; then
    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$raiz/secretos/jwt.pem" 2>/dev/null
    if [[ $interactivo == true ]]; then
        chown "$UID_JAVA:$UID_JAVA" "$raiz/secretos/jwt.pem"
        chmod 0400 "$raiz/secretos/jwt.pem"
    else
        # Solo en las pruebas, sin root: el uid 10001 del contenedor tiene que poder leerla.
        chmod 0444 "$raiz/secretos/jwt.pem"
    fi
    echo "Creada la llave de firma."
fi

# --- Contraseñas de los usuarios de la demo (D-59) ------------------------------------------------
# Una línea usuario=clave por cada usuario de usuarios-demo.yaml que todavía no tenga la suya.
claves_demo="$raiz/secretos/claves-demo"
[[ -e $claves_demo ]] || : >"$claves_demo"
agregadas=0
while read -r usuario; do
    if ! awk -F= -v u="$usuario" '$1 == u { hay = 1 } END { exit !hay }' "$claves_demo"; then
        echo "$usuario=$(openssl rand -hex 16)" >>"$claves_demo"
        agregadas=$((agregadas + 1))
    fi
done < <(awk '/^  - usuario: / { print $3 }' "$USUARIOS_DEMO")
# Solo en las pruebas, sin root: el uid 10001 del contenedor tiene que poder leerlo.
[[ $interactivo == true ]] || chmod 0444 "$claves_demo"
echo "Contraseñas de la demo: $agregadas nuevas en $claves_demo (verlas con «sudo cat», sin copiarlas a otro lado)."

# --- configuracion.env: credenciales de actualizar.sh ---------------------------------------------
if [[ ! -e $raiz/configuracion.env ]]; then
    pedir DOCKERHUB_USUARIO "Usuario de Docker Hub"
    pedir DOCKERHUB_TOKEN "Token de Docker Hub de solo lectura (mdd-equipo; no se muestra)"
    pedir GITHUB_TOKEN "Token de GitHub para los deployments (no se muestra)"
    {
        echo "# Credenciales del equipo de demostración (preparar.sh). Solo para actualizar.sh."
        echo "DOCKERHUB_USUARIO=$DOCKERHUB_USUARIO"
        echo "DOCKERHUB_TOKEN=$DOCKERHUB_TOKEN"
        echo "AUTENTICACION=${AUTENTICACION-auth.docker.io}"
        echo "GITHUB_API=${GITHUB_API:-https://api.github.com}"
        echo "GITHUB_REPOSITORIO=${GITHUB_REPOSITORIO:-paul-g-software-ucacue/tesis-prototipo-2026}"
        echo "GITHUB_TOKEN=$GITHUB_TOKEN"
        echo "GITHUB_ENTORNO=demostracion"
    } >"$raiz/configuracion.env"
    echo "Creado $raiz/configuracion.env."
fi

install -m 0750 deploy/equipo/actualizar.sh "$raiz/bin/actualizar.sh"

[[ $interactivo == true ]] || exit 0

# --- Solo en el equipo real: usuario de sistema, permisos y systemd -------------------------------
if ! id mdd >/dev/null 2>&1; then
    useradd --system --home-dir "$raiz" --no-create-home --shell /usr/sbin/nologin mdd
fi
usermod -aG docker mdd
chown -R mdd:mdd "$raiz"
chown "$UID_JAVA:$UID_JAVA" "$raiz/secretos/jwt.pem"
# identidad lo lee como dueño (10001) y probar_humo.sh, que corre como mdd, como grupo.
chown "$UID_JAVA:mdd" "$claves_demo"
chmod 0440 "$claves_demo"
install -m 0644 deploy/equipo/mdd-actualizar.service deploy/equipo/mdd-actualizar.timer /etc/systemd/system/
systemctl daemon-reload
systemctl enable --now mdd-actualizar.timer
echo "Listo: mdd-actualizar.timer consulta Docker Hub cada 5 minutos («journalctl -u mdd-actualizar.service»)."
