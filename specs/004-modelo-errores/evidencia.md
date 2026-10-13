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
| Núcleo, verificador y mecanismo (T007 a T011) | confirmada | `plataforma:errores-nucleo`: 13 pruebas, 78/78 líneas. `VerificadorDeCatalogo`: 4 pruebas; `CodigosComunes` coincide con `contratos/comun/errores.md`. `plataforma:errores`: 14 pruebas con un servidor real, la seguridad de los procesos y un token válido, 83/113 líneas (73 %). **Hallazgos:** (1) los procesos escanean `ec.mdd` y encuentran el advice por escaneo; la autoconfiguración lo registra solo si falta (`@ConditionalOnMissingBean`), y no exige servidor web, porque en las pruebas sin servidor el escaneo igual lo encuentra y necesita el registro. (2) FindSecBugs marcó `SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING` en la página `/error`, que atiende el método de la petición original como la `BasicErrorController` de Spring Boot; la API es *stateless* con token *bearer* y sin CSRF, así que se excluye solo esa clase y ese hallazgo en `config/spotbugs/excluir.xml`, con el motivo. (3) El 403 de la seguridad pasa por `/error` (`sendError`); se responde sin cuerpo, porque su formato es de F05-H02 (D1 del análisis). | 2026-10-13 |
| Seguridad, generación y procesos (T012 a T015) | confirmada | `plataforma:seguridad`: 15 pruebas; una petición directa a `/error` sin token da 401, y el cuerpo del 401 coincide con `CodigosComunes.SEGURIDAD_NO_AUTENTICADO` y con `errores.md`. La generación valida los cuerpos (`@Valid`; `CredencialesSesion` con `@NotNull @Size(min = 1)`). `./gradlew spotlessApply build integrationTest`: 163 pruebas unitarias, de arquitectura y de contrato, y 19 de integración, en verde. | 2026-10-13 |

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
