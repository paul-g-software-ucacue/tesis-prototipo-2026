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
| `plataforma:pruebas` y `plataforma:seguridad` (T012 a T015) | confirmada | `plataforma:pruebas`: 6 pruebas (tokens, JWKS y validador de contratos), 90 de 99 líneas. `plataforma:seguridad`: `SeguridadProcesoTest` con un servidor real y un JWKS servido por la prueba, 13 pruebas. Las 10 causas de rechazo (sin token, `Basic`, `Bearer %%%`, token mal formado, alterado, vencido, de otra llave, de otro emisor, HS256 y `alg: none`) dan 401 con el mismo cuerpo, `Content-Type: application/json` y `WWW-Authenticate: Bearer`; un token válido llega con `ROLE_COORDINADOR_CARRERA` y `ROLE_DOCENTE`; `POST` a una ruta pública pasa sin token y `GET` a la misma ruta da 401; la salud pasa sin token. 57 de 58 líneas. **Hallazgos:** (1) Spring 7 necesita los nombres de los parámetros, que el plugin de Spring Boot solo activa en las aplicaciones; `mdd.java-conventions` compila con `-parameters`. (2) En Spring 7 ya no existe `StubMvcResult`; la prueba del validador usa MockMvc *standalone*. (3) Spring Security 7 agrega la autoridad `FACTOR_BEARER` a las autenticaciones con token; los roles siguen saliendo del claim `roles`. | 2026-10-13 |
| Procesos con seguridad y contexto en la raíz (T016 a T018) | confirmada | `./gradlew spotlessApply build integrationTest` en verde en 3 min 28 s, con los `EsquemasIntegrationTest` de los 4 procesos ya con la seguridad en el contexto. `docker compose up -d --wait --build`: imágenes construidas con el contexto en la raíz (`.dockerignore` raíz que solo deja `backend/` y `contratos/`) y los 6 contenedores `healthy` en 105 s sin token (FR-008, SC-006). Dentro del contenedor salida, `GET /api/dashboard/sesion` sin token → `HTTP/1.1 401`, `WWW-Authenticate: Bearer` y el cuerpo de `SEGURIDAD_NO_AUTENTICADO`. | 2026-10-13 |

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
