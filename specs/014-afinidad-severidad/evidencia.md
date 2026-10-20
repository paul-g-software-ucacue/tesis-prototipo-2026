# Evidencia: F04-H03

Registro de las comprobaciones de la historia (issue #42): los pesos y el mínimo de afinidad en cada versión de normativa, las reglas del motor que faltaban y la severidad editable de cada regla (RN-19, RN-24, MD-08). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son la tabla pública de la política; las contraseñas de local y de la CI son ficticias.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| PostgreSQL | `postgres:18.6-alpine` | Testcontainers y el perfil consolidado. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Base: contrato, reglas, migración y repositorio

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Contrato primero (T002) | confirmada | `contratos/normativa/openapi.yaml`: `Regla` y `ReglaEditada` con `identificador` `^R[NF]-[0-9]{2}$` y `parametros` sin `minItems`; `ReglaEditada` gana `severidad` opcional (un `$ref` simple, sin `allOf`, para que el generador dé una clase simple, como en F04-H02). `openApiGenerate` y `generar:api` regeneran; `tipos` y `verificar:ruptura` en verde. | 2026-10-20 |
| Reglas (T003, T004) | confirmada | `ReglaTest` (5), sin dobles: RF-24 y RN-17 con severidad y sin parámetros; RN-24 con parámetros y sin severidad; sin parámetros ni severidad, rechazada; `RX-01`, `RN-1`, `rn-02` y `RF-024`, rechazados. Fallaron antes del código (no existía `delMotor()`). La prueba de F04-H01 que rechazaba RN-05 sin parámetros pasó a rechazarla sin parámetros ni severidad: es la regla que esta historia cambia. | 2026-10-20 |
| Orden de las reglas | agregada | El agregado ordena sus reglas por identificador, así que RF-24 salía primera. `VersionNormativa` las ordena ahora con las `RF-xx` al final; la prueba de dominio falló antes del cambio. La consulta del repositorio quedó con su orden de antes: T006 lo ponía en el SQL, pero el agregado lo reordenaba. | 2026-10-20 |
| Migración V4 (T005, D-68) | confirmada | `V4__afinidad_y_reglas_del_motor.sql`. Se escribió antes que su prueba; para que la prueba fallara primero, se corrió T006 sin V4: fallaron 6 pruebas. Con V4 pasaron las de la migración y fallaron las 2 del repositorio, hasta el `LEFT JOIN`. | 2026-10-20 |
| Versión 1 después de V4 (T006, SC-002) | confirmada | `PoliticaDe2024IntegrationTest` (5): 16 reglas con la severidad de research §2 y 37 parámetros, con los 8 nuevos (RN-24 3, 2, 2, 1, 2, 1 y 3; RN-19 2); las 22 filas, idénticas a la carga 7; el `VERSION_CREADA` de F04-H01 sigue; un `VERSION_EDITADA` de `sistema` con 16 cambios (8 severidades y 8 parámetros, con anterior `null`). | 2026-10-20 |
| Repositorio (T006) | confirmada | `RepositorioVersionesJdbcIntegrationTest` (5): 16 reglas; RN-17 dura sin parámetros; RF-24 al final; una copia de la 1 con sus reglas sin parámetros se guarda y se reconstruye igual. | 2026-10-20 |
| Versión creada a mano (T006, D-68) | confirmada | `NormativaEsquemaIntegrationTest`: una versión 2 insertada después de V3 (Flyway con `target` 3) queda, después de V4, con las mismas 16 reglas que la 1 y su propio `VERSION_EDITADA`. | 2026-10-20 |
| Vuelta atrás (T006, D-55) | confirmada | `NormativaEsquemaIntegrationTest`: el Flyway de la versión anterior, con V1 a V3, arranca sobre el esquema con V4; la consulta de reglas de esa versión (con `JOIN`, copiada de `51d3c19`) solo trae reglas `RN-xx` con parámetros: ve RN-19 y RN-24, y no RN-17 ni RF-24. | 2026-10-20 |

## Escenario «Valores por defecto» (US1)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Dominio (T007, T008) | confirmada | Sin dobles: `PesosAfinidadTest` (4): arma los 7 pesos de RN-24; el campo amplio igual al específico es válido; cuarto nivel amplio 4 con específico 3 lanza `CambioNoPermitido` en `reglas[RN-24].cuarto_nivel_campo_amplio` con «El peso del campo amplio (4) no puede superar al del campo específico (3)», y lo mismo con el tercer nivel. `VersionNormativaTest` (5 nuevas): `pesosAfinidad()` y `minimoAfinidad()`, vacíos sin RN-24 ni RN-19; editar con el campo amplio mayor se rechaza y la versión no cambia; cambiar el mínimo de 2 a 3 da su `CambioDeValor`; un peso negativo, «El valor no puede ser negativo». No compilaban antes del código. La invariante está en el constructor del agregado, así que también vale al reconstruir una versión. | 2026-10-20 |
| API (T009) | confirmada | `NormativaContratoTest` (2 nuevas, con la versión de prueba ampliada con RN-19, RN-24 y RF-24): `consultarVersion` trae los 7 pesos, el mínimo con RN-19 blanda y RF-24 dura con `parametros` vacío, contra el contrato; editar con el campo amplio mayor da 400 `COMUN_SOLICITUD_INVALIDA` con el campo y el motivo, y la versión no cambia. El controlador no cambió. Con la invariante del agregado desactivada, fallan la de contrato y la de dominio; se volvió a poner. | 2026-10-20 |
| `DatosDePrueba` de la aplicación | sin cambios | T007 pedía sumarle RN-19 y RN-24. No hizo falta: la aplicación no cambia, y sus pruebas no tocan la afinidad. | 2026-10-20 |

## Escenario «Severidad de una regla» (US2)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Dominio (T011, T012) | confirmada | `VersionNormativaTest` (6 nuevas), sin dobles: RN-19 de blanda a dura cambia y da el `CambioDeValor` `reglas[RN-19].severidad` de `BLANDA` a `DURA`, sin tocar sus parámetros ni la versión original; sin severidad pedida o con la misma, no hay cambios; una severidad en RN-24 lanza `CambioNoPermitido` en `reglas[RN-24].severidad` con «Esta regla solo da parámetros: no tiene severidad»; una severidad de una regla que no está, `CambioNoPermitido` en `reglas[RN-17]`; con un periodo, `VersionEnUso`. No compilaban antes del código. `CambiosDeVersion` gana las severidades y conserva su constructor de tres argumentos, así que `EditarVersionServicio` y sus pruebas no cambiaron. | 2026-10-20 |
| API (T013) | confirmada | `NormativaContratoTest` (4 nuevas): RN-19 a dura da 200 con la severidad nueva y la 1 la conserva blanda; sin severidades, ninguna cambia; una severidad en RN-24, 400 con su campo y su motivo; con la versión en uso, 409 `NORMATIVA_VERSION_EN_USO`. Las dos primeras de 200 y 400 fallaron antes de pasar la severidad desde el controlador; la de 409 ya pasaba, porque toda edición de una versión en uso se rechaza. `PermisosNormativaTest` sigue en verde. | 2026-10-20 |
| Persistencia y registro (T014, FR-007) | confirmada | `VersionesNuevasJdbcIntegrationTest` con `EditarVersionServicio` y `RegistroDeCambiosJdbc` reales: la copia de la 1 con RN-05 dura se guarda y se reconstruye dura; la 1 sigue blanda; la copia trae RN-19, RN-24 y RF-24 iguales a los de la 1; el registro tiene `reglas[RN-05].severidad` de `BLANDA` a `DURA`. | 2026-10-20 |

## Ajustar en pantalla (US3)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas
