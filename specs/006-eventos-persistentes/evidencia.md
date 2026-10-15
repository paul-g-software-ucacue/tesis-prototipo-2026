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
| Dependencias de `plataforma:eventos` (T016, A4) | confirmada | `./gradlew :plataforma:eventos:dependencies --configuration runtimeClasspath`: `spring-modulith-events-api`, `-core`, `-jdbc` y `-jackson` 2.1.1, sin `spring-modulith-core` ni ArchUnit; `jcl-over-slf4j` excluido; solo `commons-logging` 1.3.6. | 2026-10-14 |
| Separación de tokens (T011, T012) | confirmada | `plataforma:seguridad`: 29 pruebas. Token `PROCESO` en `/interno/**` → 200; token de usuario en `/interno/**` → 403; token de proceso en `/api/**` → 403; sin token o con cualquiera de los 10 tokens inválidos en `/interno/**` → el 401 uniforme. | 2026-10-14 |
| Token de proceso (T013, T014) | confirmada | Usuarios: 45 pruebas. `EmitirTokenDeProcesoServicio` compara SHA-256 en tiempo constante; clave incorrecta, proceso desconocido y clave configurada vacía dan `CredencialesInvalidas`. El token lleva `sub` = `proceso:distributivo`, `roles` = `[PROCESO]` y 5 min. El contrato da 200, el mismo 401 y 400 con `detalles`; el registro no tiene la clave ni el token. | 2026-10-14 |
| Núcleo del emisor (T015 a T017, T026) | confirmada | `plataforma:eventos`: pruebas del sobre (el registro conserva `destino`; el cuerpo HTTP no lo lleva y valida contra el contrato), del token (caché, renovación con menos de 1 min, descarte, 503, Usuarios detenido, clave vacía), del cliente (204, 400/403/500/503, plazo vencido, receptor detenido, host desconocido, 401 con reintento, dos 401, destino sin URL, clave vacía), del esquema, de la autoconfiguración, del oyente y del reenvío. Cobertura del 60 % y SpotBugs sin hallazgos. | 2026-10-14 |

## Escenario 1: entrega normal

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Escenario 2: receptor caído

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Escenario 3: entrega repetida

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Seguridad

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Punta a punta

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Memoria (R-19)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas
