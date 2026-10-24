# Evidencia: F03-H03

Registro de las comprobaciones de la historia (issue #37): mantener la oferta del periodo en Académico, con las horas a cubrir y los permisos de D-70 (RF-15, RN-18). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Base: contrato, dominio, migración y repositorio

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Datos de prueba de la carga 05 en Académico | `05_oferta_periodo.csv` (165 asignaturas-paralelo de 2026-I, sintéticas, RNF-SEG-06), copiado de `backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba/` a `backend/servicios/academico/src/test/resources/academico/datos-de-prueba/`, junto a las cargas 02 y 04 | SHA-256 `e58090eff3e9…` | 2026-10-24 |
| El contrato: 5 operaciones (tag `Oferta`) y 2 códigos 409 | El YAML carga con 30 operaciones; los modelos se generan; `verificar:ruptura` pasa | `contratos/academico/openapi.yaml`, `contratos/comun/errores.md` | 2026-10-24 |
| Las reglas de `AsignaturaParalelo`, sin dobles | 7 de 7 pasan | `AsignaturaParaleloTest`: paralelo, estudiantes de 0 a 9999, horas de 0,5 a 20, corrección y estado | 2026-10-24 |
| Las reglas de `OfertaDelPeriodo`, sin dobles, con las 165 asignaturas-paralelo de la carga 05 | 10 de 10 pasan: suman 660 horas con el total de cada una de las 7 carreras; 4 horas en 3 paralelos son 12, con uno retirado 8 y al volver a ofertarlo 12 | `OfertaDelPeriodoTest` | 2026-10-24 |
| V5: `asignatura_paralelo` y sus `check`, y las 4 acciones nuevas del registro | Pasa; falló sin la migración | `AcademicoEsquemaIntegrationTest`, con Testcontainers (postgres:18.6-alpine) | 2026-10-24 |
| El repositorio guarda y lee igual, con y sin horas ni sede | 4 de 4 pasan | `RepositorioOfertaJdbcIntegrationTest` | 2026-10-24 |

## Registrar la oferta (US1)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Los servicios bloquean el periodo, las carreras y la oferta, validan, guardan y registran | 7 de 7 pasan | `MantenerOfertaServicioTest` (5), `ConsultarOfertaServicioTest` (2) | 2026-10-24 |
| Ofertar 201; 400 en `jornada`, `horasSemanales`, `asignatura` y `sede`; 409 repetida y oferta cerrada; 404 con un periodo que no existe | Pasan, y cada respuesta cumple el contrato | `OfertaContratoTest`, con los servicios reales sobre dobles en memoria | 2026-10-24 |
| Las 30 operaciones de Académico exigen su permiso; las 5 nuevas, sus listas alternativas (D-70) | Pasa; sin `OFERTA:LECTURA` en la lectura, falla (mutación revertida) | `PermisosAcademicoTest` con `VerificadorDePermisos` | 2026-10-24 |
| Académico completo: `./gradlew :servicios:academico:check` | 210 pruebas, 0 fallas; 77,9 % de líneas (mínimo 60 %) | JaCoCo de Académico | 2026-10-24 |

## Paralelos y horas a cubrir (US2)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Escenario del issue por la API: 4 horas en 3 paralelos dan 12, con una retirada 8; la retirada se ve | Pasa | `OfertaContratoTest` | 2026-10-24 |
| La asignatura-paralelo trae su campo amplio con su nombre, su campo específico, sus palabras clave y el tronco común; `porEncimaDeLaAsignatura` en `true` si su asignatura se corrigió después, y en `false` si no | Pasan | `OfertaContratoTest` | 2026-10-24 |

## Corregir y retirar (US3)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Corregir, retirar y volver a ofertar cumplen el contrato; una jornada que no es de la carrera, 400; con el periodo vigente, 409 `ACADEMICO_OFERTA_CERRADA` | Pasan | `OfertaContratoTest`, `MantenerOfertaServicioTest` | 2026-10-24 |

## Pantalla (US4)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| «Oferta del periodo»: el periodo en Planificación por omisión, las horas a cubrir por asignatura, carrera y periodo, solo lectura con su motivo, ofertar con las listas de la carrera, el motivo junto a la pista, corregir, retirar con confirmación y volver a ofertar, el 403 de los periodos, el menú con los tres permisos de lectura | 11 de 11 pasan | `Oferta.test.tsx` (Vitest) | 2026-10-24 |
| La SPA completa, después de mover lo común de los formularios a `pantallas/comun` | 168 de 168 pasan; 94,3 % de líneas | `npx vitest run --coverage --testTimeout=30000` | 2026-10-24 |
| En un navegador: 3 paralelos de 4 horas dan 12, con uno retirado 8; axe sin fallas WCAG 2.1 AA en la oferta y en los diálogos de ofertar, corregir y retirar | 5 de 5 pasan | `e2e/oferta.spec.ts` (Playwright) | 2026-10-24 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| `deploy/probar_oferta.sh` sobre el perfil reconstruido, dos veces seguidas | Todos los pasos pasan las dos veces, en unos 22 s: 320 de 320 datos maestros de prueba; 165 de 165 asignaturas-paralelo con 660 horas y el total de cada carrera; 3 paralelos de 4 h dan 12, con uno retirado 8 y al volver a ofertarlo 12; repetida 409; horas de más, jornada ajena y asignatura inactiva, 400 en su campo; los datos para la afinidad; consultar la oferta tarda 11 y 10 ms (máximo 300 ms); con el periodo En aprobación, 409 `ACADEMICO_OFERTA_CERRADA`; quién lee y escribe; el registro. Sin sede matriz en la base, usó una temporal; la limpieza deja 0 asignaturas-paralelo, 0 carreras y 0 periodos de prueba, también en Roles | Salida del script. El estado En aprobación se pone en la base, como dice research §9 | 2026-10-24 |
| Los 11 scripts de punta a punta sobre el perfil | 11 de 11 terminan con 0 | `probar_autenticacion`, `errores`, `eventos`, `spa`, `normativa`, `periodos`, `estructura`, `plantillas`, `subida`, `carreras` y `oferta` | 2026-10-24 |
| Backend completo: `./gradlew check integrationTest` | 937 pruebas, 0 fallas; BUILD SUCCESSFUL en 2 min 26 s |  | 2026-10-24 |
| Playwright completo | 78 de 78 pasan en 1,7 min | `npx playwright test` | 2026-10-24 |
| `shellcheck` de `probar_oferta.sh` | Sin observaciones | `koalaman/shellcheck:stable` | 2026-10-24 |
| `deploy/equipo/probar_equipo.sh` con un constructor limpio (`BUILDX_BUILDER=mdd-limpio`) | 13 de 13 pasos OK; memoria del equipo después de iniciar sesión: 1385,1 MiB | Salida del script | 2026-10-24 |
| Gitleaks v8.30.1 sobre un clon en el scratchpad (montado `:ro,Z`) | Sin fugas | `gitleaks git --redact` | 2026-10-24 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas

- **Lo propio (D-64):** el coordinador de carrera lee la oferta de todas las carreras y no la mantiene; F03-H05 aplica el filtro de lo propio de `OFERTA` con los cargos. Es una limitación conocida, como en F03-H02.
- **Límite de tiempo de Vitest:** en el primer run del PR #139, `test-frontend` falló porque una prueba de «Carreras y mallas» que llena un formulario largo tardó 5,03 s y el límite por omisión era 5 s. No había un error de lógica: en local pasa, y en el PR #136 pasó en la CI. El límite pasó a 15 s en `frontend/vitest.config.ts`, con un comentario que dice por qué; ahora la suite pasa sin el `--testTimeout` de la línea de comandos (168 de 168).
