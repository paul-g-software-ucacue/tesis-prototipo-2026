# Evidencia: B00-H05

Registro de las comprobaciones de los tres escenarios de la historia (issue #22), de la seguridad entre procesos (D-49, ADR-018) y de las pruebas de punta a punta. Cada fila dice el comando, la versión y la salida resumida, o enlaza al run de CI.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44, 12 núcleos, 31 GiB de RAM | El equipo de despliegue tiene 4 núcleos y 8 GiB (D-36). |
| JDK | OpenJDK 25 (Red Hat) | Gradle 9.8.1 con `-Dorg.gradle.java.home=/usr/lib/jvm/java-25`. |
| Docker Engine y Compose | 29.8.2 y 5.5.1 (Fedora) | Con `sg docker -c '…'` hasta volver a iniciar sesión. |
| Spring Modulith | 2.1.1 | `spring-modulith-events-api`, `-events-jdbc` y `-events-jackson`, sin el *starter* (research §1). |

## Contratos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Validación de los contratos (T007) | confirmada | `openapitools/openapi-generator-cli:v7.25.0 validate --recommend` sobre una copia de `contratos/` en el scratchpad: `usuarios` (con `/interno/usuarios/token-proceso` y `/interno/usuarios/eventos`), `generacion` (con `/api/generacion/eventos-prueba`), `cargas` y `dashboard` → `No validation issues detected.` `comun/eventos.yaml`, `errores.yaml` y `seguridad.yaml` solo dan los avisos esperados de «Unused model». | 2026-10-14 |

## Construcción y pruebas

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Receptor, evento de prueba y armado (T018 a T024) | confirmada | Usuarios: receptor `POST /interno/usuarios/eventos` (204 nuevo y repetido, 400 `COMUN_EVENTO_NO_RECONOCIDO`, 400 con `detalles`, 401 y 403, todo contra el contrato) y `V3__evento_recibido.sql`. Generación: `POST /api/generacion/eventos-prueba` (202 encendido, 404 apagado, 401) y `V2__evento_prueba.sql`. Distributivo: `ModulosTest` pide exponer `ec.mdd.plataforma.eventos` como interfaz nombrada (`@NamedInterface("eventos")`), como `errores-nucleo`; `EsquemasIntegrationTest` encuentra `eventos_distributivo.event_publication`. | 2026-10-14 |
| Hallazgos de la implementación | confirmada | (1) Una prueba de `ClienteDeEventos` falló una vez con `IOException` en la primera petición: el `HttpClient` del JDK intenta el *upgrade* a h2c sobre HTTP plano. Se fija HTTP/1.1 en `Clientes.http`; 6 corridas seguidas sin fallas. (2) Cada entrega fallida dejaba además un ERROR con la traza de `SimpleAsyncUncaughtExceptionHandler`. Spring Boot 4 registra su propio `AsyncConfigurer` con `@ConditionalOnMissingBean`, así que `EventosAutoConfiguracion` corre antes que `TaskExecutionAutoConfiguration` y da uno con `FallosAsincronos` (`EntregaFallida` en DEBUG; lo demás, en ERROR con traza) y el ejecutor `applicationTaskExecutor` de Spring Boot. | 2026-10-14 |
| Build completo (T039) | confirmada | `./gradlew spotlessApply build integrationTest`: 271 pruebas unitarias, de arquitectura y de contrato, y 31 de integración, en verde. Cobertura de líneas: `plataforma:eventos` 159/166 (96 %), `plataforma:seguridad` 68/69 (99 %), `plataforma:pruebas` 205/222 (92 %), Usuarios 243/259 (94 %), Generación 48/52 (92 %). SpotBugs sin hallazgos. | 2026-10-14 |
| Herramientas | confirmada | ShellCheck (`koalaman/shellcheck:stable`) sin hallazgos en `deploy/probar_eventos.sh`; actionlint sin hallazgos en los workflows; Gitleaks v8.30.1 sobre un clon con los cambios: `no leaks found`. Todo sobre copias en el scratchpad. | 2026-10-14 |
| Dependencias de `plataforma:eventos` (T016, A4) | confirmada | `./gradlew :plataforma:eventos:dependencies --configuration runtimeClasspath`: `spring-modulith-events-api`, `-core`, `-jdbc` y `-jackson` 2.1.1, sin `spring-modulith-core` ni ArchUnit; `jcl-over-slf4j` excluido; solo `commons-logging` 1.3.6. | 2026-10-14 |
| Separación de tokens (T011, T012) | confirmada | `plataforma:seguridad`: 29 pruebas. Token `PROCESO` en `/interno/**` → 200; token de usuario en `/interno/**` → 403; token de proceso en `/api/**` → 403; sin token o con cualquiera de los 10 tokens inválidos en `/interno/**` → el 401 uniforme. | 2026-10-14 |
| Token de proceso (T013, T014) | confirmada | Usuarios: 45 pruebas. `EmitirTokenDeProcesoServicio` compara SHA-256 en tiempo constante; clave incorrecta, proceso desconocido y clave configurada vacía dan `CredencialesInvalidas`. El token lleva `sub` = `proceso:distributivo`, `roles` = `[PROCESO]` y 5 min. El contrato da 200, el mismo 401 y 400 con `detalles`; el registro no tiene la clave ni el token. | 2026-10-14 |
| Núcleo del emisor (T015 a T017, T026) | confirmada | `plataforma:eventos`: pruebas del sobre (el registro conserva `destino`; el cuerpo HTTP no lo lleva y valida contra el contrato), del token (caché, renovación con menos de 1 min, descarte, 503, Usuarios detenido, clave vacía), del cliente (204, 400/403/500/503, plazo vencido, receptor detenido, host desconocido, 401 con reintento, dos 401, destino sin URL, clave vacía), del esquema, de la autoconfiguración, del oyente y del reenvío. Cobertura del 60 % y SpotBugs sin hallazgos. | 2026-10-14 |

## Escenario 1: entrega normal

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Integración (T020) | confirmada | `EntregaIntegrationTest` (PostgreSQL 18.6 y `ReceptorSimulado`): el receptor recibe el sobre una vez con `Bearer token-…` y la publicación queda `COMPLETED` en menos de 10 s; una transacción que falla después de publicar no deja ni la fila ni la publicación, y el receptor no recibe nada (SC-005). | 2026-10-14 |
| Punta a punta (T025) | confirmada | `deploy/probar_eventos.sh`, paso 1: `OK  1. Entrega normal: 1 registro en usuarios.evento_recibido y la publicación COMPLETED en menos de 10 s`. | 2026-10-14 |

## Escenario 2: receptor caído

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Integración (T027) | confirmada | `ReenvioIntegrationTest` (ciclos de 2 s): con el receptor detenido la fila existe, la publicación queda pendiente y hay un WARN con el `idEvento`; al volver, `COMPLETED` en menos de 2 ciclos. También: 3 pendientes se completan todas; una `PUBLISHED` y una `PROCESSING` viejas pasan a `FAILED` y se completan (FR-005); un 401 al token guardado se resuelve en el mismo intento; con el token en 503 la publicación queda pendiente y se completa después (FR-011). | 2026-10-14 |
| Punta a punta (T030) | confirmada | Pasos 2 y 3: `OK  2. Identidad detenido: 202, la fila en generacion.evento_prueba y la publicación pendiente` y `OK  3. Identidad de vuelta: 1 registro y la publicación COMPLETED en menos de 75 s`. El registro de distributivo tiene `WARN … Entrega fallida del evento c9418bb3-… de tipo generacion.EventoDePrueba hacia usuarios: no se pudo conectar con el receptor: ClosedChannelException` y `INFO … Reenvío del evento c9418bb3-… de tipo generacion.EventoDePrueba`, sin ERROR, sin el contenido, la clave ni un token. | 2026-10-14 |

## Escenario 3: entrega repetida

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Integración (T031, T032) | confirmada | `RepositorioEventosRecibidosJdbcIntegrationTest`: 5 entregas seguidas dan `true` una vez y 1 fila; 2 hilos simultáneos (con `CountDownLatch`) dan exactamente un `true` y 1 fila; se conserva el `recibido_en` de la primera. Un evento repetido no vuelve a aplicar su manejador y responde 204. T033: no hubo nada que corregir. | 2026-10-14 |
| Punta a punta (T034) | confirmada | Paso 4: `OK  4. Entrega repetida: la publicación vuelve a COMPLETED y sigue 1 solo registro en menos de 75 s`. | 2026-10-14 |

## Seguridad

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| API Gateway (T035, T036) | confirmada | `SeguridadGatewayTest`: `POST /interno/usuarios/eventos` sin token → el 401 uniforme; con token de usuario o de proceso → 403; el proceso falso no recibe nada. Pasa sin cambiar el API Gateway (`denyAll` fuera de `/api/**`). Punta a punta, paso 5: `401 sin token y 403 con el de usuario, sin registrar nada`. | 2026-10-14 |

## Punta a punta

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Perfil desde un volumen nuevo (T039) | confirmada | `docker compose down -v`, `docker compose build` y `docker compose up -d --wait`. `deploy/probar_autenticacion.sh` y `deploy/probar_errores.sh`: todos los pasos en `OK`. `deploy/probar_eventos.sh`: pasos 0 a 5 en `OK`, en 67 s. | 2026-10-14 |

## Memoria (R-19)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas
