# Evidencia: F07-H01

Registro de las comprobaciones de la historia (issue #50): los totales del docente por componente, en todas las carreras y unidades, y su proporción (RF-27, RN-05, D-01). Los datos son ficticios.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | Fedora | Con `sg docker -c '…'`. |

## Los totales

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| En dos carreras (SC-001) | Coinciden con una suma aparte: 3 h de clase, 8,25 h de otras y 2 h de gestión | `probar_totales.sh`, paso 2 | 2026-11-01 |
| Una sola cuenta (SC-002) | Las disponibles coinciden con las de la sugerencia | `CargaHorariaTest`, `probar_totales.sh`, paso 3 | 2026-11-01 |
| Reemplazar y quitar | Cuentan solo las horas nuevas, y quitar deja de contar | `probar_totales.sh`, paso 4 | 2026-11-01 |
| Exceso | Con 53,25 h de 40 dice 13,25 h, y las disponibles son 0 | `CargaHorariaTest`, `probar_totales.sh`, paso 5 | 2026-11-01 |
| Casos de borde | Paralelo retirado, horas actuales, 0 contratadas, parcial enviado y periodo cerrado | `CargaHorariaTest`, `ConsultarCargaHorariaServicioTest`, `probar_totales.sh`, paso 11 | 2026-11-01 |

## La proporción

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Enteros que suman 100 (SC-003) | 8 y 4 dan 67 y 33; 1 y 2 dan 33 y 67; un medio redondea hacia arriba | `ProporcionTest`, `probar_totales.sh`, paso 2 | 2026-11-01 |
| Solo con clase y otras | Investigación, vinculación y gestión no la cambian | `CargaHorariaTest` | 2026-11-01 |
| Sin horas | No hay proporción, y se dice | `ProporcionTest`, `PanelDeCarga.test.tsx`, `probar_totales.sh`, paso 6 | 2026-11-01 |
| La meta (principio VI) | Sale de RN-05 de la versión del periodo, y una meta distinta se muestra | `ConsultarProporcionDelPeriodoServicioTest`, `InternoNormativaContratoTest`, `PanelDeCarga.test.tsx` | 2026-11-01 |
| Sin marcar error | Apartarse de la meta no muestra aviso | `PanelDeCarga.test.tsx` | 2026-11-01 |

## Quién ve qué

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Sin filtro de carrera (D-82) | Un coordinador lee a un docente de otra carrera | `CargaHorariaContratoTest`, `probar_totales.sh`, paso 6 | 2026-11-01 |
| Solo sumas (SC-007) | Sin asignaturas, actividades, paralelos, carreras ni contacto | `CargaHorariaContratoTest`, `probar_totales.sh`, paso 6 | 2026-11-01 |
| Permisos | Leer la oferta, el distributivo o los catálogos: 200. Docente y Operador de datos: 403 | `CargaHorariaContratoTest`, `PermisosGeneracionTest`, `probar_totales.sh`, paso 7 | 2026-11-01 |
| Errores | Docente no activo y sin distributivo: 409 con su código | `CargaHorariaContratoTest`, `probar_totales.sh`, paso 7 | 2026-11-01 |
| La lista | Totales de las dos carreras, y solo las actividades de la propia | `ConsultarActividadesServicioTest`, `probar_totales.sh`, paso 8 | 2026-11-01 |

## Rendimiento y fallos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Guardar y consultar (SC-004) | 0,19 s (meta: menos de 3 s) | `probar_totales.sh`, paso 9 | 2026-11-01 |
| Sin el proceso `datos` (SC-006) | 504 en 5,0 s en los totales y en la lista (meta: menos de 10 s), y vuelve | `probar_totales.sh`, paso 10 | 2026-11-01 |
| Guardado y totales fallidos | Aclara que la asignación sí se guardó | `Actividades.test.tsx`, `PanelDeCarga.test.tsx` | 2026-11-01 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Backend `./gradlew check` | 1877 pruebas unitarias, 0 fallas, 4 omitidas | Local | 2026-11-01 |
| `integrationTest` | 202 de 202, en todos los módulos | Local | 2026-11-01 |
| Líneas por módulo | Generación 90,6 %, Normativa 75,9 % | JaCoCo, regenerado | 2026-11-01 |
| SPA | Vitest 388 de 388 con cobertura: 94,47 % de sentencias y 94,56 % de líneas; ESLint, Prettier y TypeScript sin observaciones | Local | 2026-11-01 |
| Playwright | 106 de 106, con axe sobre el panel, también a 390 px | Local | 2026-11-01 |
| Scripts | Los 23 de `deploy/probar_*.sh` terminan en 0, sin avisos de limpieza | Local | 2026-11-01 |
| `probar_equipo.sh` | 13 controles OK | Local | 2026-11-01 |
| CI | 12 de 12 en verde. `perfil-consolidado` falló la primera vez y pasó al repetirla sin cambios; ver las notas | PR #180 | 2026-11-01 |
| shellcheck y Gitleaks | Sin observaciones | Copias nuevas en el scratchpad | 2026-11-01 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Despliegue | `demostracion` exitoso, «Sano en 54 s», 8 contenedores sanos; los datos del autor siguen; 401 sin credenciales | GitHub y consulta de solo lectura, `85fea868edad` | 2026-11-01 |
| Totales al guardar | 9 h de clase y 22 h de otras: 31 h de 20, «Se pasó por 11 h», y 0 disponibles | Comprobación del autor | 2026-11-01 |
| Proporción | 29 % y 71 %, y con 5 h de otras, 64 % y 36 %, con la meta 60/40 y sin aviso de error | Comprobación del autor | 2026-11-01 |
| Quitar | Con 5 h de otras: 14 h de 20 y 6 disponibles, sin exceso | Comprobación del autor | 2026-11-01 |
| La lista | «Totales en todas las carreras» con la clase, igual que el panel | Comprobación del autor | 2026-11-01 |
| Sin acciones | La pantalla sin «Asignar actividad» ni «Quitar», con los mismos totales | Comprobación del autor | 2026-11-01 |

Lo que no se vio en la demo, y queda cubierto por las pruebas:

- **El panel en Oferta:** no hay capturas al desplegar un candidato ni al proponer (`Oferta.test.tsx`).
- **El panel antes de guardar:** no hay captura del formulario (`Actividades.test.tsx`, `e2e/totales.spec.ts`).
- **Horas en otra carrera:** la demo tiene un solo coordinador (`probar_totales.sh`, paso 2).
- **SC-005:** el autor no tomó el tiempo, así que el criterio queda sin medir en la demo.
- **Quién hizo la captura sin acciones:** la captura no muestra el usuario.

## Notas

- **Pruebas primero:** se vieron fallar `Proporcion`, `CargaHoraria`, el servicio de Normativa, el panel y las dos pantallas. En `ConsultarCargaHorariaServicio`, su controlador, el cliente de la meta y la lista, el código se escribió antes de ver fallar la prueba, porque las pruebas no compilaban. Para compensarlo se rompió el código a propósito en 8 puntos, y las pruebas detectaron los 8.
- **Un error que encontraron las pruebas:** la consulta nueva no exigía permiso, porque `@PreAuthorize` se pone a mano en cada controlador. La prueba de permisos lo detectó antes del primer commit.
- **Otro error:** sin elegir periodo, el panel se intentaba pintar sin docente y la prueba se quedaba colgada. Lo corrigió la revisión de tipos.
- **Cambios frente al plan:** el panel se llama `PanelDeCarga`. Al proponer, el diálogo de la sugerencia se cierra, así que el panel queda en la página de Oferta. La meta sembrada ya la comprueba `PoliticaDe2024IntegrationTest`, y `probar_totales.sh` la lee de punta a punta: no hizo falta una prueba de integración nueva (T007).
- **`SumaPorComponente`:** se quitó, porque `CargaHoraria` la reemplaza.
- **Fallo intermitente en CI:** en la primera corrida del PR #180, `probar_actividades.sh` falló en sus pasos 9 y 10: con `datos` detenido respondió 502 al instante, en vez de 504 en 5 s, y después de levantarlo siguió con 502. Pasó al repetirla, sin cambios. No se conoce la causa, porque la corrida no guarda los registros de los servicios, y localmente no se reprodujo. Puede repetirse.
