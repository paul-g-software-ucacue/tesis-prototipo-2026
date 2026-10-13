# Evidencia: B00-H03

Registro de las comprobaciones de los cuatro escenarios de la historia (issue #20), de la segunda medición de R-19 (D-34, D-36) y de las búsquedas de secretos. Cada fila dice el comando, la versión y la salida resumida, o enlaza al run de CI.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44, 12 núcleos, 31 GiB de RAM | El equipo de despliegue tiene 4 núcleos y 8 GiB (D-36). |
| JDK | OpenJDK 25.0.4.1 (Red Hat) | Gradle 9.8.1 con `-Dorg.gradle.java.home=/usr/lib/jvm/java-25`. |
| Docker Engine y Compose | 29.8.2 y 5.5.1 (Fedora) | Con `sg docker -c '…'` hasta volver a iniciar sesión. |

## Contratos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Validación de los contratos (T010) | confirmada | `openapitools/openapi-generator-cli:v7.25.0 validate --recommend` sobre una copia de `contratos/`: `usuarios`, `cargas`, `generacion` y `dashboard` → `No validation issues detected.`, con las referencias a `../comun/*.yaml` resueltas. `comun/errores.yaml` y `comun/seguridad.yaml` solo dan avisos de «Unused model», porque son archivos de componentes que usan los contratos de los servicios. | 2026-10-13 |

## Construcción y pruebas

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Escenario 1: petición sin token

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Escenario 2: token válido

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Escenario 3: token alterado o vencido

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Escenario 4: R-19 después de iniciar sesión

Formato: [contracts/medicion-r19-sesion.md](contracts/medicion-r19-sesion.md).

## Búsquedas de secretos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas
