# Evidencia: B00-H02

Registro de las comprobaciones de los tres escenarios de la historia (issue #19), de la medición de R-19 (D-34, D-36) y de los casos límite. Cada fila dice el comando, la versión y la salida resumida, o enlaza al run de CI.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44, 12 núcleos, 31 GiB de RAM | El equipo de despliegue tiene 4 núcleos y 8 GiB (D-36); cada contenedor tiene su límite de memoria. |
| JDK | OpenJDK 25.0.4.1 (Red Hat) | Gradle 9.8.1 con `-Dorg.gradle.java.home=/usr/lib/jvm/java-25`. |
| Docker Engine | 29.8.2 (`moby-engine` de Fedora) | Instalado por el autor el 2026-10-12. El usuario está en el grupo `docker`; hasta volver a iniciar sesión, los comandos se ejecutan con `sg docker -c '…'`. |
| Docker Compose | 5.5.1 (`docker-compose` de Fedora) | Se pide 2.24 o posterior (`env_file` con `required: false`). |
| Buildx | 0.37.2 (`docker-buildx` de Fedora) | BuildKit para `RUN --mount=type=cache`. |

## Construcción y pruebas

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Escenario 1: todo se levanta con un comando

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Escenario 2: cabe en la memoria del equipo (R-19)

Formato de cada medición: [contracts/medicion-r19.md](contracts/medicion-r19.md).

## Escenario 3: un esquema por servicio

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas
