# Evidencia: F04-H01

Registro de las comprobaciones de la historia (issue #40): la Política SENESCYT 2024 como versión 1 de la normativa (RF-19, RN-01 y RN-03 a RN-15). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son la tabla pública de la política; las contraseñas de local y de la CI son ficticias.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest 5.0.4 y Playwright 1.64.0. |
| PostgreSQL | `postgres:18.6-alpine` | Testcontainers y el perfil consolidado. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |
| Backend | `./gradlew check integrationTest` en verde | Normativa: 28 pruebas unitarias y de contrato y 9 de integración, con 68,8 % de líneas en las unitarias. Roles: 69 y 11. |
| SPA | Vitest y Playwright en verde | 90 pruebas de Vitest con 97,86 % de líneas; 44 de Playwright en Chromium y Firefox. |

## Base: dominio, versión 1 y permiso

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Contrato primero (T002) | confirmada | `contratos/normativa/openapi.yaml` con `listarVersiones` y `consultarVersion`, las dos con `x-mdd-permiso: PARAMETROS:LECTURA`. `openApiGenerate` genera `VersionesApi` y `generar:api` los tipos de la SPA (`normativa.d.ts`). | 2026-10-18 |
| Dominio de Normativa (T003, T004) | confirmada | Sin dobles: `FilaCargaHorariaTest` (7), `TablaCargaHorariaTest` (5) y `VersionNormativaTest` (4). Horas no negativas; «sin valor» distinto de cero; mínimo ≤ clase ≤ máximo; la suma no supera la dedicación (el cuidador sustituto, 30 de 40, es válido); un perfil y tipo no se repiten, y un perfil no aplica a la vez a todos y a un tipo; `filaPara` da la fila del tipo o la de todos; reglas con y sin severidad. | 2026-10-18 |
| La versión 1 es la política (T005, T006, SC-001) | confirmada | `V2__politica_2024.sql`, generada desde la carga 7. `PoliticaDe2024IntegrationTest` (4, Testcontainers): una sola versión, la 1, `SENESCYT-2024-03`, «Política SENESCYT 2024», vigente desde 2024-03-11, T2; sus **22 filas coinciden valor por valor** con `politica-2024.csv`, la copia de la carga 7; 7 reglas con 29 parámetros y sus severidades; el registro `VERSION_CREADA` por `sistema`. Se comprobó que la prueba falla al cambiar un valor (las horas de gestión del gestor T2 de 10 a 9). | 2026-10-18 |
| Invariantes al reconstruir (T006) | confirmada | `RepositorioVersionesJdbcIntegrationTest` (3): el repositorio reconstruye la versión 1 con el agregado, así que las 22 filas cumplen sus invariantes; las filas vuelven en el orden de la política; una versión que no existe da vacío. | 2026-10-18 |
| Vuelta atrás segura (D-55) | confirmada | `NormativaEsquemaIntegrationTest`: la versión anterior, que solo conoce V1, arranca sobre el esquema con V2. | 2026-10-18 |
| Permiso del Vicerrector (T007, D-66) | confirmada | `MatrizD16IntegrationTest` con `PARAMETROS:LECTURA` en la fila de `VICERRECTOR`: falló antes de la migración y pasó con `V4__vicerrector_lee_normativa.sql`. | 2026-10-18 |

## Escenario «Versión inicial»

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Consulta (T008) | confirmada | `ConsultarNormativaServicioTest` (3, Mockito): listar, consultar la 1, `VersionNoEncontrada` para una que no existe. | 2026-10-18 |
| API y permisos (T009, SC-005) | confirmada | `NormativaContratoTest` (4): las dos operaciones contra el contrato; una regla sin severidad y las horas que la política no da se omiten; 404 `COMUN_RECURSO_NO_ENCONTRADO`; `POST` y `PUT` responden 405 `COMUN_METODO_NO_PERMITIDO` (US2, escenario 3). `PermisosNormativaTest`: `VerificadorDePermisos` sobre el contrato; sin la anotación de `consultarVersion` falla en esa operación (se comprobó quitándola). `ModulosTest` de `datos` en verde. | 2026-10-18 |
| Pantalla «Normativa» (T010, FR-008) | confirmada | `Normativa.test.tsx` (6): la versión 1 con su origen, su código, su vigencia (11/03/2024) y Tipo 2; las reglas con su nombre, su severidad («Dura», «Blanda», «—») y sus parámetros con su unidad; la tabla con los perfiles en español, «Todos», «Tipo 2» y «—» para lo que la política no da, sin códigos; sin el permiso, «Acceso no autorizado», sin pedir `/api/normativa` y sin la entrada en el menú; un error de la API sin su código. | 2026-10-18 |
| Playwright y accesibilidad (T011) | confirmada | `e2e/normativa.spec.ts` (3): `vicerrectorado.demo` abre «Normativa» y ve 22 filas y Tipo 2; `docente.demo` en `/normativa` ve «Acceso no autorizado» sin texto técnico; axe-core sin fallas WCAG 2.1 AA, después de poner cada versión de la lista en su `<li>`. | 2026-10-18 |
| De punta a punta (T012) | confirmada | Perfil local sobre la base de F05-H02, que migró `normativa` a V2 y `roles` a V4. `deploy/probar_normativa.sh`, pasos 1 a 3: `vicerrectorado.demo` lista una sola versión, la 1, con `SENESCYT-2024-03` y T2; tiene 22 filas y 7 reglas, y `DOCENTE_TC` con 22 h de clase entre 18 y 24 y 18 de otras de docencia; `admin.demo` ve la misma versión; `docente.demo` recibe el 403 común; una versión que no existe, 404. | 2026-10-18 |

## Solo datos (US2)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Sin duplicar al reiniciar (T013, FR-006, SC-003) | confirmada | `probar_normativa.sh`, paso 4: después de reiniciar `datos` tres veces, sigue habiendo una sola versión y es idéntica, byte a byte, a la de antes. | 2026-10-18 |
| Ninguna cifra en el código (T014, FR-005, SC-004) | confirmada | Se buscaron en `backend/servicios/normativa/src/main/java` y en `frontend/src/pantallas/normativa` las cifras de la política (22, 18, 24, 40, 20, 19, 60, 300 a 3500, 500…). En el Java no hay ninguna; en la SPA solo aparecen «F04-H01» en un comentario y `fontWeight: 500`. Las cifras solo están en `V2__politica_2024.sql` y en las pruebas. | 2026-10-18 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Otros scripts (T015) | confirmada | `probar_autenticacion.sh`, `probar_errores.sh`, `probar_eventos.sh` y `probar_spa.sh`: todos los pasos pasan. | 2026-10-18 |
| SPA (T015) | confirmada | `lint`, `formato:verificar`, `tipos` y `verificar:ruptura` en verde. | 2026-10-18 |
| Intermitencia (T015) | anotada | Con el backend corriendo en paralelo, dos pruebas de `Roles.test.tsx` vencieron su tiempo; sin carga, las 90 pruebas pasaron dos veces seguidas. | 2026-10-18 |
| Formato (T015) | corregido | La primera corrida de `./gradlew check` falló en `spotlessJavaCheck` de Roles por el comentario nuevo de `MatrizD16IntegrationTest`; con `spotlessApply`, en verde. | 2026-10-18 |
| Gitleaks y shellcheck (T015) | confirmada | Gitleaks v8.30.1 sobre un clon en el scratchpad, montado con `:ro,Z` (nunca el repositorio): sin fugas en los commits de la rama. `shellcheck` sin observaciones en `probar_normativa.sh`. | 2026-10-18 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Equipo simulado (T015) | confirmada | `deploy/equipo/probar_equipo.sh` con la versión de esta rama: pasos 0 a 11 en `OK`; memoria 1 348 MiB; la versión rota vuelve a la anterior. | 2026-10-18 |

## Notas

- La creación de una versión y su evento `VersionNormativaCreada` llegan con F04-H02; hoy la creación de la versión 1 queda en `normativa.registro_cambio`.
- Las severidades de RN-05 y RN-10 son las inferidas del modelo de dominio §5.2; se confirman al especificar la validación del distributivo (MD-08).
