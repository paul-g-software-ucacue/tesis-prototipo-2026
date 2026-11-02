# Evidencia: F10-H01

Registro de las comprobaciones de la historia (issue #53): el panel de inconsistencias globales, con la acción que corrige cada una y la justificación de las blandas.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | Fedora | Con `sg docker -c '…'`. |

## Las comprobaciones

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Conteo aparte (SC-001) | Con los datos de prueba: 165 paralelos sin propuesta, 46 docentes sin clase, 0 carreras sin su coordinador y 9 gestores, igual al panel | `probar_panel.sh`, paso 2 | 2026-11-02 |
| RN-17 y RF-24 | Licencia, inactivos y retirados no cuentan; al proponer, desaparecen | `EvaluarVersionTest`, `probar_panel.sh`, paso 3 | 2026-11-02 |
| RN-12 | 0 y 2 coordinadores salen; 1 no; una carrera solo con retirados no cuenta | `EvaluarVersionTest`, `probar_panel.sh`, paso 7 | 2026-11-02 |
| RN-10 | 8 y 11 cumplen, 7 y 12 no; solo cuentan G06 a G21 | `EvaluarVersionTest`, `probar_panel.sh`, paso 7 | 2026-11-02 |
| RN-18 | Una por carrera, con sus paralelos; se reprodujo corrigiendo la asignatura después de ofertar | `EvaluarVersionTest`, `probar_panel.sh`, paso 7 | 2026-11-02 |
| Sobrecarga | Suma las carreras; justo en el límite no sale | `EvaluarVersionTest`, `probar_panel.sh`, paso 7 | 2026-11-02 |
| RN-05, porcentajes | Con meta 60 y margen 5: 55 y 65 cumplen, 54 y 66 no | `EvaluarVersionTest` | 2026-11-02 |
| RN-05, actividades | Dice cuál falta, y se justifica aparte de la proporción | `EvaluarVersionTest`, `probar_panel.sh`, paso 4 | 2026-11-02 |
| Otro margen | 2026-II, con margen 45, no marca a quien tiene 100 % de clase; 2026-I sí | `probar_panel.sh`, paso 9 | 2026-11-02 |
| Falta un dato en la versión | Falla sin inventarlo | `ConsultarReglasDelPanelServicioTest` | 2026-11-02 |

## La acción y la justificación

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Indicación | Cada tipo dice qué hacer, quién y en qué pantalla | `PanelContratoTest` | 2026-11-02 |
| Enlace | Lleva a la pantalla; sin permiso, solo la indicación | `Inconsistencias.test.tsx`, `e2e/inconsistencias.spec.ts` | 2026-11-02 |
| Justificar | Deja de estar abierta, sigue visible, con el texto, quién y cuándo | `JustificarInconsistenciaServicioTest`, `probar_panel.sh`, paso 4 | 2026-11-02 |
| Cambiar | Agrega otra fila y vale la última; dos a la vez dejan las dos filas | `RepositorioJustificacionesIntegrationTest`, `probar_panel.sh`, paso 4 | 2026-11-02 |
| Registro | Quién y cuándo, sin el texto | `JustificarInconsistenciaServicioTest`, `probar_panel.sh`, paso 4 | 2026-11-02 |
| Lo que se rechaza | Dura, RN-19, ya resuelta, periodo cerrado y texto inválido | `JustificarInconsistenciaServicioTest`, `PanelContratoTest`, `probar_panel.sh`, paso 5 | 2026-11-02 |
| RN-19 | La excepción sale como justificada, y no se cambia aquí | `PanelDeInconsistenciasTest`, `probar_panel.sh`, paso 6 | 2026-11-02 |
| Permisos | Consolidación y el Vicerrectorado leen; solo Consolidación justifica; un coordinador recibe 403 | `PanelContratoTest`, `probar_panel.sh`, paso 8 | 2026-11-02 |

## Rendimiento y fallos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Abrir (SC-002) | 0,30 s con los datos de prueba (meta: menos de 5 s) | `probar_panel.sh`, paso 2 | 2026-11-02 |
| Justificar (SC-005) | 0,16 s (meta: menos de 3 s) | `probar_panel.sh`, paso 4 | 2026-11-02 |
| Sin el proceso `datos` (SC-006) | 504 en 5,0 s (meta: menos de 10 s), sin presentarlo como vacío, y vuelve | `probar_panel.sh`, paso 10 | 2026-11-02 |
| Cobertura (SC-007) | `MotorReglas`, `PanelDeInconsistencias` y los dos servicios, 100 % de líneas y de ramas (meta: 80 %) | JaCoCo, regenerado | 2026-11-02 |
| Secretos en los registros | 0 en 1165 líneas | Registros del perfil, después de los scripts | 2026-11-02 |

## La verificación de cierre

| Comprobación | Resultado | Fecha |
|---|---|---|
| Backend, `check jacocoTestReport` | 2079 pruebas, 0 fallas, 4 omitidas. Spotless y SpotBugs incluidos | 2026-11-02 |
| `integrationTest` completo | 209 pruebas, 0 fallas | 2026-11-02 |
| Cobertura por módulo | Generación 92,0 %, Normativa 77,5 %, Docentes 80,7 %, Académico 82,5 % | 2026-11-02 |
| SPA | Vitest 418, Playwright 110; 94,6 % de sentencias; ESLint, `npm run tipos` y Prettier | 2026-11-02 |
| Scripts de `deploy/` | 26 de 26, sin avisos, con la base limpia. Ver las desviaciones | 2026-11-02 |
| `deploy/equipo/probar_equipo.sh` | Despliega, vuelve atrás e informa a GitHub | 2026-11-02 |
| shellcheck | 0 hallazgos, con una copia nueva en el scratchpad | 2026-11-02 |
| Gitleaks | Sin filtraciones, con un clon nuevo en el scratchpad y `.gitleaks.toml`, en modo `git` como CI | 2026-11-02 |

## Desviaciones

- **Pruebas primero.** Se vio fallar cada grupo antes de implementar, en un comando aparte. T014 y T016 se escribieron juntas, y el detalle de RN-05 entró con ellas.
- **`EvaluarVersionTest`.** Las pruebas de la versión completa van en un archivo nuevo, no en `MotorReglasTest`.
- **Constructores de `AsignaturaParalelo`.** Se conservaron los cortos, así que los 21 usos no cambiaron.
- **`probar_afinidad.sh`.** En la corrida de cierre falló su paso 11 con 401, porque el equipo se suspendió durante el script y venció el token. Repetido sin suspensión, pasa los 12 pasos en 28 s. No se tocó el script.
- **Dos pruebas de Normativa.** `V6` agrega un registro por versión, y dos pruebas que buscaban el de V4 lo contaban. Se corrigió su filtro. Se repitió `integrationTest` completo.
- **Accesibilidad.** axe encontró poco contraste en el chip «Blanda», que pasó a estilo neutro.
- **Script.** `probar_panel.sh` tuvo dos correcciones: el paso 7 no podía subir el paralelo a 30 h, porque la base lo impide, y una variable sin usar.

## La comprobación del autor

Pendiente, después del despliegue (SC-004, con cronómetro).
