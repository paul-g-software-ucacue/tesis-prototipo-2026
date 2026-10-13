# Evidencia: B00-H04

Registro de las comprobaciones de los tres escenarios de la historia (issue #21), del API Gateway (D-40) y de las pruebas de punta a punta. Cada fila dice el comando, la versión y la salida resumida, o enlaza al run de CI.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44, 12 núcleos, 31 GiB de RAM | El equipo de despliegue tiene 4 núcleos y 8 GiB (D-36). |
| JDK | OpenJDK 25.0.4.1 (Red Hat) | Gradle 9.8.1 con `-Dorg.gradle.java.home=/usr/lib/jvm/java-25`. |
| Docker Engine y Compose | 29.8.2 y 5.5.1 (Fedora) | Con `sg docker -c '…'` hasta volver a iniciar sesión. |

## Contratos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Validación de los contratos (T006) | confirmada | `openapitools/openapi-generator-cli:v7.25.0 validate --recommend` sobre una copia de `contratos/`: `usuarios`, `cargas`, `generacion` y `dashboard` → `No validation issues detected.`, con las respuestas nuevas `SolicitudInvalida`, `ErrorInterno` y `ServicioNoDisponible` resueltas desde `comun/errores.yaml`, y `minLength: 1` en el inicio de sesión. `comun/` solo da los avisos esperados de «Unused model». | 2026-10-13 |

## Construcción y pruebas

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Escenario 1: error reconocido

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Escenario 2: error técnico

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Escenario 3: error de entrada

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## API Gateway

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Punta a punta

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas
