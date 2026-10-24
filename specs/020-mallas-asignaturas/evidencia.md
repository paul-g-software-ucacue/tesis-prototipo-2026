# Evidencia: F03-H02

Registro de las comprobaciones de la historia (issue #36): mantener carreras, mallas y asignaturas en Académico, con los datos para la afinidad y los permisos de D-70 (RF-12, RF-14, RN-19, RN-24). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

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
| Las enumeraciones del dominio son las del diccionario de datos (FR-006) | 3 de 3 pasan | `ValoresPermitidosTest`, que lee `diccionario-de-datos.yaml` de Cargas como archivo | 2026-10-24 |
| Las reglas del dominio, sin dobles, con las 7 carreras, 12 mallas y 300 asignaturas de prueba | 23 de 23 pasan | `CampoConocimientoTest` (2), `CarreraTest` (5), `MallaTest` (2), `AsignaturaTest` (6), `DatosMaestrosTest` (8) | 2026-10-24 |
| V4 con los 11 campos amplios; el repositorio guarda y lee las tres tablas | 3 de 3 pasan, más 2 nuevas en el esquema | `RepositorioCarrerasJdbcIntegrationTest` y `AcademicoEsquemaIntegrationTest`, con Testcontainers (postgres:18.6-alpine) | 2026-10-24 |
| Los 4 códigos `ACADEMICO_*` 409 en el catálogo | 2 de 2 pasan | `ErroresAcademicoTest`; `contratos/comun/errores.md` | 2026-10-24 |
| Los servicios bloquean, validan, guardan y registran | 11 de 11 pasan | `MantenerCarrerasServicioTest` (9), `ConsultarCarrerasServicioTest` (2) | 2026-10-24 |
| Académico completo: `./gradlew :servicios:academico:check` | 182 pruebas, 0 fallas; 77,9 % de líneas (mínimo 60 %) | JaCoCo de Académico | 2026-10-24 |

## Carreras (US1)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Registrar, listar, consultar, corregir y cambiar el estado; repetidas 409; sede que no es sede o campo de otro amplio, 400 con su campo; lo que no existe, 404 | Pasan, y cada respuesta cumple el contrato | `CarrerasContratoTest` (swagger-request-validator) | 2026-10-24 |
| Las 25 operaciones de Académico exigen su permiso; las 13 nuevas, sus listas alternativas (D-70) | Pasa; con la escritura solo en `CATALOGOS:ESCRITURA`, falla (mutación revertida) | `PermisosAcademicoTest` con `VerificadorDePermisos` | 2026-10-24 |

## Mallas y asignaturas (US2)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Escenario 1: una asignatura sin carrera ni horas da 400 `COMUN_SOLICITUD_INVALIDA` con los dos campos | Pasa | `CarrerasContratoTest` | 2026-10-24 |
| Una malla repetida 409; derogada, no admite asignaturas; ciclo de más y una sola palabra clave, 400 con su campo; las horas de la malla suman las asignaturas | Pasan | `CarrerasContratoTest` | 2026-10-24 |

## Datos para la afinidad (US3)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Escenario 2: la asignatura trae su campo amplio con su nombre CINE, su campo específico, sus palabras clave y el tronco común | Pasa; sin el nombre del campo amplio, falla (mutación revertida) | `CarrerasContratoTest` | 2026-10-24 |

## Pantalla (US4)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| «Carreras y mallas»: la lista, la malla por ciclo con sus horas, sin acciones sin escritura, registrar y corregir con los errores en su campo, confirmar derogar e inactivar, el menú con los dos permisos de lectura | 10 de 10 pasan | `Carreras.test.tsx` (Vitest) | 2026-10-24 |
| La SPA completa | 156 de 156 pasan; 94,1 % de líneas. Normativa falló una vez por tiempo con la PC cargada y pasó al repetirla | `npx vitest run --coverage --testTimeout=30000` | 2026-10-24 |
| En un navegador: el Vicerrector registra carrera, malla y asignatura y la ve en su ciclo; axe sin fallas WCAG 2.1 AA en la tabla, la malla abierta y 4 diálogos | 6 de 6 pasan | `e2e/carreras.spec.ts` (Playwright) | 2026-10-24 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas
