# Evidencia: F03-H03

Registro de las comprobaciones de la historia (issue #37): mantener la oferta del periodo en Académico, con las horas a cubrir y los permisos de D-70 (RF-15, RN-18). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Base: contrato, dominio, migración y repositorio

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Datos de prueba de la carga 05 en Académico | `05_oferta_periodo.csv` (165 asignaturas-paralelo de 2026-I, sintéticas, RNF-SEG-06), copiado de `backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba/` a `backend/servicios/academico/src/test/resources/academico/datos-de-prueba/`, junto a las cargas 02 y 04 | SHA-256 `e58090eff3e9…` | 2026-10-24 |

## Registrar la oferta (US1)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Paralelos y horas a cubrir (US2)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Corregir y retirar (US3)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Pantalla (US4)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas

- **Lo propio (D-64):** el coordinador de carrera lee la oferta de todas las carreras y no la mantiene; F03-H05 aplica el filtro de lo propio de `OFERTA` con los cargos. Es una limitación conocida, como en F03-H02.
