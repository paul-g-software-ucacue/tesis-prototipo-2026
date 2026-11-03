# Evidencia: F12-H01

Registro de las comprobaciones de la historia (issue #56): cargar la configuración del flujo y del árbol de firmas.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | Fedora | Con `sg docker -c '…'`. |

## La configuración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Sin archivo (FR-001) | Rige la configuración por defecto: 9 etapas y F1, F2 y F3, y la pantalla lo dice | `ConsultarConfiguracionServicioTest`, `Flujo.test.tsx`, `probar_flujo.sh`, paso 1 | 2026-11-02 |
| Plantilla (FR-007, FR-008) | Se descarga comentada; es el mismo archivo por defecto, y se carga sin cambios | `LectorYamlTest`, `probar_flujo.sh`, pasos 2 y 4 | 2026-11-02 |
| Esqueleto (FR-004, FR-005) | Las 5 etapas fijas no se quitan ni se desactivan; las 4 opcionales sí se desactivan | `ConfiguracionFlujoTest`, `probar_flujo.sh`, paso 6 | 2026-11-02 |
| Plazo (FR-006) | Entero de 1 a 9999; 0, negativo, decimal y texto se rechazan | `ConfiguracionFlujoTest` | 2026-11-02 |
| Versiones (FR-017, FR-018) | Otra carga crea una versión; las anteriores se conservan con su texto, quién y cuándo; dos a la vez dejan dos filas | `RepositorioConfiguracionIntegrationTest`, `probar_flujo.sh`, pasos 5 y 15 | 2026-11-02 |

## Los rechazos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Lectura segura (FR-009) | Anclas, alias, tipos Java y etiquetas se rechazan sin construir objetos, sin detalles del lector | `LectorYamlTest`, `probar_flujo.sh`, paso 8 | 2026-11-02 |
| Firmas (FR-012) | Una firma en una etapa desactivada se rechaza, diciendo cuál | `ConfiguracionFlujoTest`, `probar_flujo.sh`, paso 6 | 2026-11-02 |
| Firmantes (FR-013, FR-014) | Un rol sin usuario y un periodo sin Responsable se rechazan, con Roles de verdad | `CargarConfiguracionServicioTest`, `probar_flujo.sh`, pasos 3 y 6 | 2026-11-02 |
| Todos los problemas (FR-015, SC-002) | Un archivo con 7 problemas los dice todos; los del archivo y los de firmantes salen juntos | `ConfiguracionFlujoTest`, `probar_flujo.sh`, paso 7 | 2026-11-02 |
| Nada cambia (FR-016) | Después de todos los rechazos, la respuesta es idéntica y no hay filas nuevas | `probar_flujo.sh`, paso 9 | 2026-11-02 |
| Tamaño (FR-010) | 1 byte más de 1 MB: 413; la pantalla ni lo sube | `CargarConfiguracionServicioTest`, `Flujo.test.tsx`, `probar_flujo.sh`, paso 8 | 2026-11-02 |
| Planificación (FR-019) | Fuera de ella se ve y no se carga: 409 | `probar_flujo.sh`, paso 14 | 2026-11-02 |

## Permisos, rendimiento y fallos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Permisos (FR-021, FR-022) | Sin FLUJO_FIRMAS, 403 en las tres operaciones; sin token, 401 | `PermisosValidacionocsTest`, `probar_flujo.sh`, paso 11 | 2026-11-02 |
| Cargar (SC-003) | 0,05 s (meta: menos de 3 s) | `probar_flujo.sh`, paso 4 | 2026-11-02 |
| Sin `datos` ni `identidad` (FR-024) | 504 del catálogo en 5,0 s (meta: menos de 10 s), sin guardar, y vuelve | `probar_flujo.sh`, pasos 12 y 13 | 2026-11-02 |
| Accesibilidad | Sin fallas de axe al ver, en un rechazo y después de cargar | `frontend/e2e/flujo.spec.ts` | 2026-11-02 |
| Registros | El contenido de los archivos no aparece; 0 posibles secretos en 1579 líneas | `probar_flujo.sh`, paso 16, y registros del perfil | 2026-11-02 |

## La verificación de cierre

| Comprobación | Resultado | Fecha |
|---|---|---|
| Backend, `check jacocoTestReport` | 2303 pruebas, 0 fallas, 4 omitidas. Spotless y SpotBugs incluidos | 2026-11-02 |
| `integrationTest` completo | 229 pruebas, 0 fallas | 2026-11-02 |
| Cobertura por módulo | Validación OCS 93,6 %, Generación 89,9 %, Académico 82,5 %, Docentes 80,7 %, Normativa 77,5 %, Roles 73,0 % | 2026-11-02 |
| Cobertura de las reglas (SC-006) | `ConfiguracionFlujo` y su revisor, 100 % de líneas; 100 % y 97 % de ramas | 2026-11-02 |
| Cobertura en cero | `RepositorioConfiguracionJdbc` y `ConfiguracionValidacionocs` marcan 0 % en JaCoCo porque corren en `integrationTest` y en el script, no en las pruebas unitarias | 2026-11-02 |
| SPA | Vitest 462, Playwright 112; 94,7 % de sentencias; ESLint, `npm run tipos` y Prettier | 2026-11-02 |
| Scripts de `deploy/` | 28 de 28, sin avisos | 2026-11-02 |
| `deploy/equipo/probar_equipo.sh` | Despliega, vuelve atrás e informa a GitHub | 2026-11-02 |
| shellcheck | 0 hallazgos en `probar_flujo.sh`, con una copia nueva en el scratchpad | 2026-11-02 |
| Gitleaks | Sin hallazgos en modo `git`, sobre un clon con `.gitleaks.toml`: 88 commits | 2026-11-02 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Despliegue | `demostracion` exitoso, «Sano en 61 s», 8 contenedores sanos; 401 sin credenciales | GitHub y `docker ps`, `e8cf831d0cd2` | 2026-11-02 |
| Comprobaciones del PR #191 | 12 de 12, con `probar_flujo.sh` en `perfil-consolidado` | GitHub | 2026-11-02 |
| Qué rige | El periodo 003 sin archivo: el aviso de la configuración por defecto, 9 etapas y 3 firmas | Comprobación del autor | 2026-11-02 |
| Sin Responsable | La plantilla sin cambios se rechaza: «Falta designar al Responsable de consolidación del periodo.», y sigue la de por defecto | Comprobación del autor | 2026-11-02 |
| Cargar | Designado `docente.demo`, un plazo cambiado de 15 a 20 días queda vigente: «Versión 1 · cargada por admin.demo» | Comprobación del autor | 2026-11-02 |
| Un archivo malo | Con otro nombre en lugar de EN_OCS se rechaza, diciendo los 2 problemas, y sigue la versión 1 con sus 20 días | Comprobación del autor y consulta de solo lectura | 2026-11-02 |
| Una fila | El periodo tiene 1 carga: los 2 rechazos no guardaron nada | Consulta de solo lectura | 2026-11-02 |
| Periodo en curso | El 002, vigente, se ve con su aviso, y no sale cargar | Comprobación del autor | 2026-11-02 |
| Rector | «Flujo y firmas» no está en su menú | Comprobación del autor | 2026-11-02 |
| Cargar con un plazo cambiado (SC-004) | 1 min 59 s como máximo (meta: menos de 3 minutos) | Horas guardadas en la base | 2026-11-02 |
| Todo el flujo | 5 minutos | Medido por el autor | 2026-11-02 |

SC-004 no se midió con cronómetro por separado, como pedía T027. El tiempo sale de las horas guardadas, entre designar al Responsable (20:00:35) y la carga (20:02:34). Es un máximo: incluye cambiar de sesión, además de descargar la plantilla, cambiar el plazo y cargarla. Los 5 minutos del autor no son solo ese paso, porque ese intervalo fue de 2 minutos, y no se suman al criterio.

Lo que no se vio en la demo, y queda cubierto por las pruebas:

- **Archivos peligrosos.** Anclas, alias y tipos: `LectorYamlTest`, `probar_flujo.sh`, paso 8.
- **Más de 1 MB.** `Flujo.test.tsx`, `probar_flujo.sh`, paso 8.
- **Un rol firmante sin usuario.** La demo tiene usuarios en todos los roles firmantes: `probar_flujo.sh`, paso 6.
- **Un proceso detenido.** `probar_flujo.sh`, pasos 12 y 13.
- **Salir de Planificación.** El paso 14 de `probar_flujo.sh` cambia el estado del periodo en la base; el avance real lo cubre `probar_periodos.sh`.
