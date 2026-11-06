# Contrato: `deploy/probar_bitacora.sh`

Reemplaza a `deploy/probar_eventos.sh` (B00-H05) y ocupa su paso en el workflow `perfil-consolidado`. Corre sobre el perfil consolidado levantado, con los usuarios de la demo, y lee la base solo con consultas de solo lectura. Termina con 0 si todos los pasos pasan y con 1 si alguno falla. Nunca imprime una clave ni un token.

| Paso | Qué hace | Qué comprueba | Requisito |
|---|---|---|---|
| 1 | Prepara un periodo con distributivo y un docente con una actividad de 4 horas (datos de prueba) | — | — |
| 2 | Cambia la actividad a 6 horas | Una entrada `generacion` · `ACTIVIDAD_GUARDADA` con el usuario, el periodo, `anterior.horas = 4` y `nuevo.horas = 6` | US1-1, FR-003 |
| 3 | Agrega una actividad y quita otra | Una entrada sin `anterior` y otra sin `nuevo` | US1-2, US1-3 |
| 4 | Intenta una asignación que viola una regla dura | Ninguna entrada nueva | US1-4, FR-004 |
| 5 | Hace un cambio en cada servicio: corrige un periodo (Académico), un docente (Docentes), una versión de normativa (Normativa), designa al Responsable (Roles) y carga la configuración del flujo (Validación OCS) | Una entrada por cada uno, con su servicio | FR-002, SC-001 |
| 6 | Confirma una carga de archivo pequeña | Una entrada por cada fila aplicada, con el usuario que confirmó | FR-002 |
| 7 | Detiene `identidad`, hace 10 cambios y lo vuelve a levantar | Los 10 cambios se guardaron sin error para el usuario; después de 2 ciclos de reenvío, las 10 entradas, una vez cada una | US3, SC-002, FR-008 |
| 8 | Reenvía a mano un evento ya registrado (con el token de proceso) | 204 y la misma cantidad de entradas | US3-2, FR-007 |
| 9 | Intenta `UPDATE`, `DELETE` y `TRUNCATE` sobre la bitácora con el rol `identidad` | Los tres fallan | US2-2, FR-010, SC-005 |
| 10 | Busca en todas las entradas claves o valores de contraseñas y tokens, incluida la clave del usuario de prueba | Ninguno | US4, SC-004 |
| 11 | Mide un cambio con `identidad` arriba y abajo | La diferencia es menor de 0,5 s | SC-003 |

El paso 1 usa los datos de prueba de `cargas/datos_prueba/` (D-06), como los demás scripts de `deploy/`.
