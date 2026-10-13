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
| Build completo (T039) | confirmada | `./gradlew spotlessApply build integrationTest` en verde: 130 pruebas unitarias, de arquitectura y de contrato, y 19 de integración con PostgreSQL 18.6, sin fallos. Cobertura de líneas: `gateway` 41/41, `plataforma:seguridad` 57/58, `plataforma:pruebas` 91/100, `plataforma:arquitectura` 34/34, `servicios:usuarios` 170/177 (96 %), `cargas`, `generacion` y `dashboard` 12/12, los otros 8 servicios 6/6; los procesos no tienen clases medibles. **Hallazgos:** (1) la generación ponía `"idIncidente":null` en el 401 de credenciales, y la prueba de contrato lo rechazó porque el contrato no lo declara nullable; `mdd.contrato` ahora omite los campos nulos (`@JsonInclude(NON_NULL)` y `containerDefaultToNull`). (2) En `@WebMvcTest` no está la autoconfiguración que activa la seguridad web; `SeguridadProcesoAutoConfiguracion` lleva `@EnableWebSecurity`. (3) La interfaz generada del inicio de sesión devuelve `ResponseEntity<TokenSesion>`, así que el 401 lo traduce un `@ExceptionHandler` del propio controlador hasta que B00-H04 cree el advice (D-38). (4) `Jwt.getIssuer()` exige una URL; el emisor `mdd-usuarios` se lee como texto y la validación del emisor funciona igual. | 2026-10-13 |


## Escenario 1: petición sin token

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| API Gateway (T019 a T022) | confirmada | `SeguridadGatewayTest` con un servidor real, un JWKS de prueba y un proceso falso como destino: las 10 causas de rechazo de T014 dan 401 con el mismo cuerpo, `Content-Type: application/json` y `WWW-Authenticate: Bearer`, **sin llegar al proceso**; un token válido llega al proceso con el mismo encabezado `Authorization`; `POST /api/usuarios/sesion` pasa sin token y `GET` a la misma ruta da 401; la salud pasa sin token. 14 pruebas con `GatewayAplicacionTest`, 41 de 41 líneas. | 2026-10-13 |

## Escenario 2: token válido

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Punta a punta en local (T038, T039) | confirmada | `docker compose up -d --wait --build`: 6 contenedores `healthy` en 106 s. `deploy/probar_autenticacion.sh` → los 6 pasos en `OK` y salida 0 (tabla abajo). shellcheck sin hallazgos. | 2026-10-13 |

Salida de `deploy/probar_autenticacion.sh` en local:

| Paso | Resultado |
|---|---|
| 1. Sin token | `OK` 401 con `SEGURIDAD_NO_AUTENTICADO` y `WWW-Authenticate: Bearer` |
| 2. Inicio de sesión de `autor.prueba` | `OK` 200 con token |
| 3. Sesión actual en `usuarios`, `cargas`, `generacion` y `dashboard` | `OK` en los 4: `autor.prueba` con `COORDINADOR_CARRERA` y `DOCENTE` |
| 4. Token alterado | `OK` el mismo 401 que sin token |
| 5. Directo al proceso salida, sin token | `OK` el mismo 401 |
| 6. Contraseña incorrecta | `OK` 401 con `USUARIOS_CREDENCIALES_INVALIDAS` |


## Escenario 3: token alterado o vencido

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Causas de rechazo | confirmada | En las pruebas de `plataforma:seguridad` y del API Gateway, con llaves generadas en la prueba: alterado, vencido, de otra llave, de otro emisor, HS256, `alg: none`, mal formado, sin token y `Basic` dan el mismo 401. De punta a punta: token alterado (paso 4) y llamada directa a un proceso (paso 5). Un token vencido de punta a punta no se prueba, porque habría que esperar 30 minutos. | 2026-10-13 |


## Escenario 4: R-19 después de iniciar sesión

Formato: [contracts/medicion-r19-sesion.md](contracts/medicion-r19-sesion.md).

### Medición local (T042)

**Medición de R-19 después de iniciar sesión** · 2026-10-12T21:41:32-05:00 · commit `395135e` · entorno: local (Docker 29.8.2)
Calentamiento: inicio de sesión de autor.prueba, 20 llamadas a la salud y 20 a la sesión actual por componente, y 30 s de espera.

| Contenedor | Consumo (MiB) | Límite (MiB) |
|---|---:|---:|
| postgres | 95,7 | 512 |
| identidad | 366,5 | 512 |
| datos | 182,5 | 512 |
| distributivo | 231,5 | 512 |
| salida | 195,7 | 512 |
| gateway | 186,6 | 384 |
| **Total** | **1 258,5** (1,23 GiB) | 2 944 |

Resultado: cumple (< 8 GiB). Sin alerta de R-19 (≤ 7 GiB).

- **Diferencia:**
  - con la referencia de B00-H02 sobre `main` (931,6 MiB, runner): +326,9 MiB;
  - con la última medición local de B00-H02 (1 001,6 MiB): +256,9 MiB, la comparación justa, mismo equipo.
- **Por qué sube:** identidad pasa a 366,5 MiB (antes unos 182) por Hibernate, Spring Security y Argon2id, que usa 16 MiB por cada hash. Los demás procesos y el API Gateway suben entre 4 y 50 MiB por Spring Security.
- **Límite de identidad:** está al 72 % de sus 512 MiB, por debajo del 85 % que pedía revisar los límites. No hace falta ajustarlo todavía (D-36).
- **Sin la opción:** `deploy/medir_memoria.sh` sin `--con-sesion` sigue funcionando igual: 1 261,1 MiB con el perfil ya calentado.
- **Lint:** shellcheck y actionlint 1.7.12 sin hallazgos en `medir_memoria.sh`, `probar_autenticacion.sh` y `perfil-consolidado.yml`.

### Medición de referencia en GitHub Actions (T043)

Pendiente: el workflow corre cuando el pull request quede listo para revisión.

## Búsquedas de secretos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas
