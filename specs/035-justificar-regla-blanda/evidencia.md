# Evidencia: F07-H03

Registro de las comprobaciones de la historia (issue #52): justificar una asignación con una regla blanda, RN-19 en el motor y con la severidad de la normativa.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | Fedora | Con `sg docker -c '…'`. |

## La regla blanda

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Sin justificación (SC-001) | 200 con `guardada: false` y la advertencia de RN-19; no guarda, no crea el parcial y no deja registro. Un texto solo de espacios, igual | `ProponerDocenteServicioTest`, `PropuestaContratoTest`, `probar_justificacion.sh`, paso 2 | 2026-11-01 |
| Texto de largo inválido | 400 en el campo `texto`, con 1 a 9 o más de 500 caracteres, y no guarda | `ProponerDocenteServicioTest`, `probar_justificacion.sh`, paso 3 | 2026-11-01 |
| Justificada | Se guarda como excepción, con `inconsistencia: { regla: RN-19, estado: JUSTIFICADA }` | `PropuestaTest`, `SugerenciaContratoTest`, `probar_justificacion.sh`, paso 4 | 2026-11-01 |
| Volver a proponer | Pide otra vez el texto, y lo guardado no cambia | `probar_justificacion.sh`, paso 4 | 2026-11-01 |
| Exentos | Tronco común y quien alcanza el mínimo, sin advertencia ni inconsistencia | `MotorReglasTest`, `probar_justificacion.sh`, paso 5 | 2026-11-01 |
| Orden | Bajo el mínimo y sin horas: gana el error de horas, con o sin texto | `ProponerDocenteServicioTest`, `probar_justificacion.sh`, paso 6 | 2026-11-01 |

## La severidad la manda la normativa

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| La lectura | Normativa entrega la severidad de RN-19 de la versión, y falla si no la tiene | `ConsultarReglasDeAsignacionServicioTest`, `InternoNormativaContratoTest` | 2026-11-01 |
| RN-19 dura (SC-002) | 409 `GENERACION_AFINIDAD_INSUFICIENTE`, «Afinidad insuficiente», con o sin texto, y no guarda | `ProponerDocenteServicioTest`, `probar_justificacion.sh`, paso 8 | 2026-11-01 |
| La sugerencia (SC-003) | Con RN-19 dura, el motivo `AFINIDAD`, sin «Proponer»; con blanda, sin motivo; si además faltan horas, el de horas | `SugerenciaTest`, `SugerenciaContratoTest`, `Oferta.test.tsx`, `probar_justificacion.sh`, paso 8 | 2026-11-01 |
| Tronco común con RN-19 dura | Sigue exento, con «disponibilidad» | `MotorReglasTest`, `ProponerDocenteServicioTest`, `probar_justificacion.sh`, paso 8 | 2026-11-01 |

La versión con RN-19 dura se crea por la API y abre 2026-II, porque un periodo abierto no cambia de versión.

## Lo que se ve

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| El servidor pide el texto | Se abre el campo con el aviso, sin estilo de error, y al guardar se cierra | `Oferta.test.tsx`, `e2e/oferta.spec.ts` | 2026-11-01 |
| Vuelve un error | Lo escrito sigue | `Oferta.test.tsx` | 2026-11-01 |
| La propuesta guardada | «Bajo el mínimo, justificada», y nada en las demás | `Oferta.test.tsx`, `e2e/oferta.spec.ts` | 2026-11-01 |
| Accesibilidad | axe sin fallas con el aviso abierto | `e2e/oferta.spec.ts` | 2026-11-01 |

## Rendimiento y seguridad

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Guardar (SC-005) | 0,19 s (meta: menos de 3 s) | `probar_justificacion.sh`, paso 4 | 2026-11-01 |
| Cobertura (SC-006) | `MotorReglas`, `Propuesta` y `Sugerencia`, 100 % de líneas y de ramas (meta: 80 %) | JaCoCo, regenerado | 2026-11-01 |
| Secretos en los registros | 0 en 1 110 líneas | Registros del perfil, después de los scripts | 2026-11-01 |

## La verificación de cierre

| Comprobación | Resultado | Fecha |
|---|---|---|
| Backend, `check jacocoTestReport` | 1977 pruebas, 0 fallas, 4 omitidas. Spotless y SpotBugs incluidos | 2026-11-01 |
| `integrationTest` completo | 204 pruebas, 0 fallas | 2026-11-01 |
| Cobertura por módulo | Generación 91,6 %, Normativa 76,5 %, Docentes 80,6 % | 2026-11-01 |
| SPA | Vitest 398, Playwright 108; 94,5 % de sentencias; ESLint, `npm run tipos` y Prettier | 2026-11-01 |
| Scripts de `deploy/` | 25 de 25, sin avisos, con la base limpia | 2026-11-01 |
| `deploy/equipo/probar_equipo.sh` | Despliega, vuelve atrás e informa a GitHub | 2026-11-01 |
| shellcheck | 0 hallazgos, con una copia nueva en el scratchpad | 2026-11-01 |
| Gitleaks | Sin filtraciones en 81 commits, con un clon nuevo en el scratchpad y `.gitleaks.toml`, en modo `git` como CI | 2026-11-01 |

## Desviaciones

- **T019.** La prueba de la advertencia se agregó a `e2e/oferta.spec.ts`, no a un archivo nuevo, porque ahí está el simulador.
- **Pruebas primero.** Se vio fallar cada prueba nueva antes de implementar, con el controlador adaptado lo justo para compilar. La prueba de «lo escrito sigue si vuelve un error» pasó desde el principio: el comportamiento ya existía, así que no se vio en rojo.
- **Limpieza del script.** La primera corrida de `probar_justificacion.sh` pasó los 8 pasos, pero no borró las actividades que crea y dejó sucia la base. Se corrigió y la base se reinició. Las corridas de esta tabla son posteriores.
- **Gitleaks.** Una primera corrida en modo `git` sobre una copia sin historial no comprobaba lo que parecía, y se descartó. En modo `dir` aparecen 13 hallazgos, todos de la autoridad certificadora de prueba de `backend/servicios/firma`, anteriores a esta historia. El resultado que cuenta es el de la tabla, en modo `git` con la configuración del repositorio.

## La comprobación del autor

Pendiente, después del despliegue (SC-004).
