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
| Error de dominio de prueba (D-41) | confirmada | `ManejadorDeErroresTest` (servidor real): `HorasExcedidas`, registrada como `PRUEBA_HORAS_EXCEDIDAS` con 409, responde 409 con exactamente `{"titulo","descripcion","codigo"}` del catálogo y `Content-Type: application/json`; su subclase `HorasExcedidasEnFeriado` usa la misma entrada; `ReglaSinCatalogo` responde 500 y el registro dice «sin código en el catálogo». | 2026-10-13 |
| Credenciales inválidas por el mecanismo común (T016, T017) | confirmada | `ErroresUsuarios` registra `CredencialesInvalidas` (401 `USUARIOS_CREDENCIALES_INVALIDAS`) y coincide con `errores.md` (`VerificadorDeCatalogo`). `SesionControlador` ya no tiene `@ExceptionHandler`. La prueba de contrato sigue dando 401 válido contra el contrato. **Hallazgo:** `@WebMvcTest` no escanea los `@Component` comunes, así que la prueba de contrato importa `ErroresUsuarios`; en los procesos lo encuentra el escaneo. | 2026-10-13 |

## Escenario 2: error técnico

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Error técnico en el mecanismo | confirmada | `ManejadorDeErroresTest`: un `IllegalStateException` con un JWT y `clave=secreta` en el mensaje responde 500 `COMUN_ERROR_INTERNO` con `idIncidente`, sin la clase, `ec.mdd`, el JWT ni la clave. El registro tiene `Incidente <id>`, la clase, la traza y `[oculto]` en lugar de los secretos. Dos fallos seguidos tienen identificadores distintos. | 2026-10-13 |
| Contrato de Usuarios (T018) | confirmada | Un fallo técnico en el inicio de sesión responde 500 con `idIncidente`, sin el JWT ni la clase, y la respuesta pasa la validación contra `contratos/usuarios/openapi.yaml`. | 2026-10-13 |
| Base de datos detenida en identidad (T019, T020) | confirmada | `ErrorTecnicoIntegrationTest`: con PostgreSQL detenido, `POST /api/usuarios/sesion` responde 500 `COMUN_ERROR_INTERNO` con `idIncidente`, sin «sql», «Exception», «org.», «jdbc» ni la contraseña. El registro: `Incidente d6a57f95-…: POST /api/usuarios/sesion falló con org.springframework.dao.DataAccessResourceFailureException` y la traza («Unable to acquire JDBC Connection»). No hizo falta corregir el mecanismo (T020). | 2026-10-13 |

## Escenario 3: error de entrada

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Validación en el mecanismo | confirmada | `ManejadorDeErroresTest`: un cuerpo con 3 campos inválidos responde 400 `COMUN_SOLICITUD_INVALIDA` con 3 `detalles` (`es obligatorio`, `debe tener entre 3 y 10 caracteres`, `no tiene el formato esperado`). Un JSON mal formado responde 400 `COMUN_CUERPO_ILEGIBLE` sin el mensaje del analizador. | 2026-10-13 |
| Inicio de sesión con datos vacíos (T021, T022, D-42) | confirmada | `{"usuario":""}` responde 400 con 2 `detalles` (`usuario` y `clave`, «es obligatorio»); `{"usuario":"autor.prueba","clave":""}`, con 1. Las respuestas pasan la validación del contrato. El contrato de B00-H03 tiene la nota de D-42. | 2026-10-13 |

## API Gateway

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Manejador reactivo (T023, T024, D-40) | confirmada | `ManejadorDeErroresGatewayTest` (servidor real): con token, una ruta sin destino responde 404 con exactamente el cuerpo de `COMUN_RECURSO_NO_ENCONTRADO`; con el proceso salida en un puerto cerrado, 503 `COMUN_SERVICIO_NO_DISPONIBLE` sin la dirección ni el puerto y sin `timestamp`, `path`, `requestId`, `error` ni `status`; sin token, el mismo 401 de ADR-014, cuyo cuerpo coincide con `CodigosComunes.SEGURIDAD_NO_AUTENTICADO`. Prueba unitaria: 500 con `idIncidente` y registro saneado; 405 con su código. 19 pruebas del API Gateway, 75/77 líneas. | 2026-10-13 |

## Punta a punta

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Build completo (T027) | confirmada | `./gradlew spotlessApply build integrationTest`: 173 pruebas unitarias, de arquitectura y de contrato, y 20 de integración, en verde. **Hallazgo:** Spring Modulith ve `ec.mdd.plataforma` como un módulo en cada proceso, y `ModulosTest` de identidad rechazó que `ErroresUsuarios` usara `errores.nucleo`; el paquete se publica como interfaz nombrada (`@NamedInterface("errores")`), igual que los `puertos.entrada` de los servicios. | 2026-10-13 |
| Perfil levantado (T025 a T027) | confirmada | `docker compose up -d --wait --build`: 6 contenedores sanos en 100 s. `deploy/probar_autenticacion.sh`: los 6 pasos en `OK` (SC-005). `deploy/probar_errores.sh`: datos vacíos → 400 con detalles de `usuario` y `clave`; ruta inexistente → 404; `DELETE` a la sesión → 405; proceso salida detenido → 503; ninguno con `timestamp`, `path`, `trace` ni `requestId`. shellcheck y actionlint sin hallazgos. | 2026-10-13 |
| Base de datos detenida en el perfil (escenario 2) | confirmada | Con `postgres` detenido, el inicio de sesión responde a los 30 s (espera de Hikari) `{"titulo":"Error inesperado",…,"codigo":"COMUN_ERROR_INTERNO","idIncidente":"28b1ba01-…"}`. En `docker compose logs identidad`: `ERROR … RegistroDeIncidentes : Incidente 28b1ba01-…: POST /api/usuarios/sesion falló con org.springframework.dao.DataAccessResourceFailureException` y la traza («Unable to acquire JDBC Connection»). La contraseña de prueba no aparece en el registro. Al levantar `postgres`, todo vuelve a quedar sano y la autenticación de punta a punta sigue pasando. | 2026-10-13 |

## Notas

- **Ruta pública con un token inválido:** si una petición a una ruta pública, como el inicio de sesión, trae un token inválido, la seguridad responde el 401 uniforme antes de llegar al controlador. Es el comportamiento normal del servidor de recursos de Spring Security, y coincide con ADR-014: un token inválido nunca se acepta.
