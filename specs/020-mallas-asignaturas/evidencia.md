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
| `deploy/probar_carreras.sh` sobre el perfil reconstruido, dos veces seguidas | Todos los pasos pasan las dos veces, en unos 14 s cada una: 7 de 7 carreras, 12 de 12 mallas y 300 de 300 asignaturas; las 12 mallas con 25 asignaturas y 100 horas; los dos escenarios; los 4 códigos 409; quién lee y escribe; el registro con sus acciones. Sin sede matriz en la base, usó una temporal; la limpieza deja 0 carreras y los 11 campos amplios | Salida del script. La primera corrida falló en un solo paso, del script: Académico responde `100.00` y jq comparaba el literal; ahora compara el número | 2026-10-24 |
| Los 10 scripts de punta a punta sobre el perfil | 10 de 10 terminan con 0 | `probar_autenticacion`, `errores`, `eventos`, `spa`, `normativa`, `periodos`, `estructura`, `plantillas`, `subida` y `carreras` | 2026-10-24 |
| Backend completo: `./gradlew check integrationTest` | 898 pruebas, 0 fallas; BUILD SUCCESSFUL en 2 min 40 s |  | 2026-10-24 |
| Playwright completo | 73 de 73 pasan en 2,5 min | `npx playwright test` | 2026-10-24 |
| `shellcheck` de `probar_carreras.sh` | Sin observaciones | `koalaman/shellcheck:stable` | 2026-10-24 |
| `deploy/equipo/probar_equipo.sh` con un constructor limpio (`BUILDX_BUILDER=mdd-limpio`) | 13 de 13 pasos OK en 4 min 57 s; memoria del equipo después de iniciar sesión: 1378,6 MiB | Salida del script | 2026-10-24 |
| Gitleaks v8.30.1 sobre un clon en el scratchpad (montado `:ro,Z`) | Sin fugas | `gitleaks git --redact` | 2026-10-24 |
| Runs del PR #136, como borrador y listo | 12 de 12 en verde, incluido `perfil-consolidado` con `probar_carreras.sh` | PR #136 | 2026-10-24 |
| Fusión con *squash*, con la aprobación del autor | `f9cf77e` | PR #136 | 2026-10-24 |
| Corrección de los mensajes que encontró el autor (ver Notas) | 11 de 11 runs en verde; fusionado como `d3762af` | PR #137 | 2026-10-24 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Despliegue de `f9cf77e` | «Sano en 44 s»; `academico` en V4 con los 11 campos amplios y 0 carreras | Estado del deployment `demostracion` en GitHub; consulta de solo lectura a `mdd-postgres-1` | 2026-10-24 |
| El autor, con `vicerrectorado.demo`, registra una carrera en su sede matriz `001`, una malla y una asignatura | Registradas: carrera `001-` (08:29:25), malla `MAT-2026` (08:30:38), asignatura `MAT` (08:36:18), cada una con su acción en el registro | Capturas del autor; `academico.registro_cambio` | 2026-10-24 |
| SC-001, primera medición: registrar una asignatura en menos de 2 minutos | **No se cumplió**: unos 5 min 40 s entre la malla y la asignatura, con tres rechazos. «debe tener al menos 2 caracteres» en una lista de un solo término, y el motivo «no tiene el formato esperado» tapaba la pista del campo específico | Capturas del autor; `academico.registro_cambio` | 2026-10-24 |
| El autor, con `consolidacion.demo`, ve la carrera y su malla en solo lectura | Sin «Registrar carrera» ni la columna «Acciones» | Captura del autor | 2026-10-24 |
| Despliegue de `d3762af` (PR #137) | «Sano en 51 s» | Estado del deployment `demostracion` en GitHub | 2026-10-24 |
| SC-001, segunda medición, con los mensajes corregidos | **Se cumple**: la malla `SOC-2026` a las 09:20:00 y la asignatura `SOC` a las 09:21:19, como mucho 1 min 19 s, sin rechazos. El autor no dio la hora de inicio; la cota es el registro de la malla. Antes, «Falta: modalidad, número de ciclos» y la pista «Tres dígitos que empiezan por 07» guiaron una carrera sin enviarla | Capturas del autor; `academico.registro_cambio` | 2026-10-24 |

## Notas

- La comprobación del autor encontró dos mensajes que no decían qué corregir, y eso hizo fallar SC-001 la primera vez. El PR #137 los corrigió: la plataforma cuenta el tamaño de una lista en elementos, y una lista vacía es «es obligatorio»; la pantalla muestra el motivo antes de la pista. La segunda medición cumple SC-001.
- Los datos del equipo de demostración (carreras `001-` e `IST-023`, mallas `MAT-2026` y `SOC-2026`, asignaturas `MAT` y `SOC`) son del autor y se quedan; las consultas a la base fueron de solo lectura.
