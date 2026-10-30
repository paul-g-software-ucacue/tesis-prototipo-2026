# Evidencia: F01-H05

Registro de las comprobaciones de la historia (issue #31): el orden de las 10 cargas según sus dependencias, el estado de cada una, la confirmación «sin proyectos» por periodo y la fuente de datos intercambiable (RF-56, RF-18, RF-57, ERS §8.3). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Línea de base

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| `main` en verde | `78362d3` (#155): las 10 comprobaciones del PR en verde; antes, en #154, `./gradlew build integrationTest` con 1379 pruebas sin fallas y los 15 scripts de `deploy/probar_*.sh` terminando en 0 | PR #154 y #155, `specs/025-confirmar-carga/evidencia.md` | 2026-10-29 |
| El glosario | Reconstruido y con las 4 entradas de esta historia (research §12) | `sdd/modelo_dominio.md`, fuera del repositorio | 2026-10-29 |

## Base: contrato, catálogo, lenguaje publicado y orden

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Los dueños dicen si tienen datos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## El orden y el estado

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## «Sin proyectos»

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## La fuente de datos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Pantalla

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas
