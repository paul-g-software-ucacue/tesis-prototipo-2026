# Evidencia: F03-H04

Registro de las comprobaciones de la historia (issue #38): mantener los docentes con su ficha académica en el servicio de Docentes, con las referencias de Académico y los permisos de D-70 (RF-16, RF-17, RN-01). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Base: contrato, referencias de Académico, dominio, migración y repositorios

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Datos de prueba de las cargas 02, 04 y 06 en Docentes | `02_carreras.csv`, `04_asignaturas_malla.csv` y `06_docentes.csv` (47 docentes ficticios, RNF-SEG-06), copiados de `backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba/` a `backend/servicios/docentes/src/test/resources/docentes/datos-de-prueba/` | SHA-256 `a4ee44cea177…`, `eb1417c75800…`, `b06fe68e66fd…` | 2026-10-24 |

## Registrar (US1)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Consultar y buscar (US2)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Corregir (US3)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Estado laboral (US4)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Pantalla (US5)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas

- Que las horas contratadas cuadren con la dedicación (RN-01), que el ocasional sea solo TC o MT (POL 5.1), que el contrato cubra el periodo y las horas que cambia la condición especial no se revisan aquí: son de la normativa del periodo (análisis C2).
