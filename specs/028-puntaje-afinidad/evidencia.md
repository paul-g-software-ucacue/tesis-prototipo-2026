# Evidencia: F06-H01

Registro de las comprobaciones de la historia (issue #46): el puntaje de afinidad de un docente en la oferta de un periodo, con su desglose (RN-19, RN-24, RF-17, ADR-010), y la primera consulta por HTTP entre procesos (ADR-023). Los datos son ficticios.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## El cálculo

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| `CalculadoraAfinidad`, sin dobles y con pesos que no son los de por defecto | 18 de 18: los escenarios del issue, el tope que corta un curso, pesos con decimales, un peso en 0 y el orden | `CalculadoraAfinidadTest` | 2026-10-30 |
| 47 docentes en las 130 asignaturas de la oferta de 2026-I | 6110 puntajes iguales a una suma hecha aparte, con Python y los CSV, sin el código de Generación (SC-001) | `probar_afinidad.sh`, paso 2 | 2026-10-30 |
| Otra versión con otros pesos | El mismo docente tiene otro puntaje en 2026-II, igual a la suma aparte (SC-002) | `probar_afinidad.sh`, paso 5 | 2026-10-30 |

## Entre procesos (ADR-023)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| `ConsultasEntreProcesos` | 14 de 14: 200, 404, 401 con otro token, 5xx, sin conexión, plazo agotado, sin token de Usuarios y mensajes sin la ruta | `ConsultasEntreProcesosTest` | 2026-10-30 |
| Las tres rutas internas | Solo con el rol `PROCESO`; un token de usuario recibe 403, también por el API Gateway, que no las enruta | `Interno*ContratoTest`, `probar_afinidad.sh`, paso 9 | 2026-10-30 |
| Lo mínimo (principio IX) | La formación no trae correo, teléfono, contrato, institución, registro ni asignaturas afines; la oferta no trae palabras clave ni tronco común | `InternoDocentesContratoTest`, `InternoAcademicoContratoTest` | 2026-10-30 |
| Con `datos` detenido | 504 `COMUN_PROCESO_SIN_RESPUESTA` en 5,0 s, sin puntajes (meta: menos de 10 s); al volver, responde 200 | `probar_afinidad.sh`, paso 11 | 2026-10-30 |
| La ficha en los registros | Ningún título de la ficha en los logs de `datos`, `distributivo` e `identidad` (SC-005) | `probar_afinidad.sh`, paso 12 | 2026-10-30 |

## Operación y pantalla

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| `consultarAfinidadDelDocente` | Contrato, 400, 404, 409, 502 y 504; permisos con `VerificadorDePermisos` y un 403 explícito | `AfinidadContratoTest`, `PermisosGeneracionTest` | 2026-10-30 |
| Rendimiento | La lista de 130 asignaturas en 0,05 s (meta: menos de 3 s, SC-003) | `probar_afinidad.sh`, paso 10 | 2026-10-30 |
| «Afinidad por asignatura» | 8 pruebas de Vitest y un recorrido de Playwright con axe en dos estados | `Docentes.test.tsx`, `e2e/docentes.spec.ts` | 2026-10-30 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Backend `./gradlew check` | 1561 pruebas, 0 fallas, 4 omitidas | Local | 2026-10-31 |
| Líneas por módulo | Generación 97,5 %, `eventos` 95,6 %, Académico 82,0 %, Docentes 80,0 %, Normativa 75,9 % | JaCoCo | 2026-10-31 |
| SPA | Vitest 282 de 282 sin cobertura; ESLint y TypeScript sin observaciones | Local; ver Notas | 2026-10-31 |
| Playwright | 95 de 95 | Local | 2026-10-31 |
| Scripts | Los 17 de `deploy/probar_*.sh` que usan el perfil terminan en 0; el de afinidad deja la base limpia | Local | 2026-10-31 |
| `probar_equipo.sh` | Código 0 | Local | 2026-10-31 |
| shellcheck y Gitleaks | Sin observaciones | Local | 2026-10-31 |
| CI del PR #161 | 12 de 12 en verde. Vitest 282 de 282 con cobertura: 93,84 % de sentencias y 93,96 % de líneas. `probar_afinidad.sh` en `perfil-consolidado`: 6110 puntajes, la lista en 0,02 s y el 504 en 5,01 s | GitHub Actions | 2026-10-31 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| El despliegue | `e935075` (#161): «success · Sano en 64 s». Los 8 contenedores sanos, con las 6 imágenes propias en `…-e93507532bf5` | GitHub Deployments, `docker ps` (solo lectura) | 2026-10-31 |
| La comprobación del autor con la ficha | «Afinidad por asignatura» aparece al final de la ficha y pide elegir un periodo. `001 · 2026-2`, sin oferta: «Este periodo todavía no tiene oferta.» `003 · 2027-01`, sin abrir: el aviso «Periodo sin abrir» con el mensaje del catálogo. Las dos respuestas pasaron por la consulta real de `distributivo` a `datos`, donde están el periodo, su versión y su oferta | Capturas del autor (no se copian al repositorio) | 2026-10-31 |
| Lo que no se vio en la demo | El autor no abrió `003`, único periodo con oferta, así que en su demo no hay puntajes ni desglose. Eso quedó comprobado con el script, con los dos procesos reales, y con Playwright | `probar_afinidad.sh`, `e2e/docentes.spec.ts` | 2026-10-31 |

## Notas

- **Vitest con cobertura en esta máquina:** falla por el tiempo de espera de 15 s en pruebas pesadas de formularios, con resultados que cambian entre corridas (entre 1 y 6). `main` sin esta historia falla igual (3 de 274, en las mismas pruebas), así que no es una regresión. La cobertura de la SPA se toma de la CI.
- **Orden de las pruebas:** en el cliente entre procesos, la calculadora, Generación y la pantalla, se vio fallar la prueba antes de escribir el código. En las tres consultas internas, prueba y código se escribieron en el mismo paso, así que no se vio ese fallo.
- **Ajuste respecto del plan:** el tercer nivel es obligatorio en la ficha, así que no existe el caso «ficha sin tercer nivel» de T007. Por el API Gateway, `/interno` responde 403, no 404.
