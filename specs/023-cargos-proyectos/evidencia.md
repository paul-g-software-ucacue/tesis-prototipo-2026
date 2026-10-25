# Evidencia: F03-H05

Registro de las comprobaciones de la historia (issue #39): registrar los cargos y los proyectos aprobados en el servicio de Docentes, con las referencias de Académico y los permisos de D-70 (RF-18, RN-12, RN-20, RN-25; D-72, D-73). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Base: contrato, catálogo, referencias de Académico, dominio, migración y repositorios

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Datos de prueba de las cargas 01, 03, 08 y 09 en Docentes | `01_estructura_organica.csv`, `03_ciclos_academicos.csv`, `08_cargos_autoridades.csv` (21 cargos) y `09_proyectos_practicas.csv` (11 proyectos), ficticios (RNF-SEG-06), copiados de `backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba/` a `backend/servicios/docentes/src/test/resources/docentes/datos-de-prueba/`, junto a las 02, 04 y 06 | SHA-256 `314995c68138…`, `5217bb27074d…`, `4784c87fcbac…`, `fd955f518167…` | 2026-10-25 |
| Los contratos: Docentes con 7 operaciones más (tags `Cargos` y `Proyectos`), Académico con unidades y periodos en `consultarReferencias`, y los 4 códigos `DOCENTES_*` con sus 8 columnas | Los YAML cargan; los modelos de Java y los tipos de la SPA se generan; `verificar:ruptura` pasa (el cambio de Académico solo agrega propiedades). Las lecturas son `[DOCENTES, CATALOGOS, DISTRIBUTIVO]:LECTURA`, sin `PROYECTOS:LECTURA` (D-64, aclaración 8) | `contratos/docentes/openapi.yaml`, `contratos/academico/openapi.yaml`, `contratos/comun/errores.md`, `npm run verificar:ruptura` | 2026-10-25 |
| El diccionario lista los 16 cargos de gestor (D-73) | El catálogo `CARGOS` tiene 22 valores, sin la fila del rango; la plantilla y el diccionario de la carga 8 salen de él. `DiccionarioYamlTest` comprueba los 22; Cargas, 96 pruebas sin fallas | `./gradlew :servicios:cargas:test` | 2026-10-25 |
| Académico publica unidades y periodos en sus referencias | `ConsultarReferenciasServicioTest` (todas las unidades, activas e inactivas, con su tipo y su gestión; todos los periodos con sus fechas, por código) y `CarrerasContratoTest` (la respuesta valida contra el contrato). Académico, 218 pruebas sin fallas | `./gradlew :servicios:academico:test` | 2026-10-25 |
| Las reglas del cargo, sin dobles | `CargoTest` (17): coordinador con y sin carrera, unidad que falta, no existe o está inactiva, gestor con la unidad de otra gestión, miembro de comisión fuera de una comisión, el fin de funciones en sus límites (igual, un día antes, un día después, exactamente 2 años y 2 años y un día), el documento de 60 y 61, el docente en licencia, inactivo, de medio tiempo o de tiempo parcial (`DocenteNoHabilitado`, sin la cédula en el mensaje), la corrección con sus cambios. **Los 21 cargos de la carga 08 son válidos**; los 7 coordinadores, de carreras distintas. Encontró que los 3 miembros de comisión de prueba tienen su fin de funciones el 2026-08-31: hoy aparecen con `FECHA_FIN_VENCIDA`, como se espera | `./gradlew :servicios:docentes:test` | 2026-10-25 |
| Las reglas del proyecto, sin dobles | `ProyectoTest`: el código con minúsculas, espacio o `_`, de 30 y de 31; el nombre de 250 y 251; carreras vacías, repetidas, de 20 y de 21, y una que no existe con su posición; la resolución de 60 y 61; la vigencia; el director y un participante no registrados (sin la cédula en el mensaje); participantes repetidos o sin rol; las horas `0.01`, `2.55`, `2.50`, `99.99`, `999.9` y `24` pasan, y `0`, `-1`, `2.555`, `100.25`, `1000` y `99999` no; la suma exacta pasa y una décima más no. **Los 11 proyectos de la carga 09 son válidos** | `./gradlew :servicios:docentes:test` | 2026-10-25 |
| Referencias y valores permitidos | `ReferenciasTest` (las 24 unidades y los periodos de las cargas 01 y 03; un periodo que no existe da `DatoInvalido` en `periodo`), `ValoresPermitidosTest` (los 22 cargos, los 2 estados del cargo, los 4 tipos y los 4 estados del proyecto y los 4 roles son los del diccionario) y `AcademicoDesdePuertosTest` (traduce las unidades y los periodos). El dominio y el adaptador: 89 pruebas sin fallas | `./gradlew :servicios:docentes:test --tests '*dominio*'` | 2026-10-25 |
| Las pruebas del dominio fallan si la regla se rompe | Desactivar la comprobación de la suma de horas hace fallar `ProyectoTest` (mutación revertida) | `./gradlew :servicios:docentes:test --tests '*ProyectoTest*'` | 2026-10-25 |
| V3: `cargo`, `proyecto`, el índice de RN-12 y las 4 acciones del registro | Pasa; falló sin la migración (4 de 4). Cada caso inválido con su propia clave: tipo, estado, rol fuera de catálogo; fin de funciones igual, anterior y a 2 años y un día (exactamente 2 años pasa); documento vacío; unidad en minúsculas; rector sin unidad o con carrera; coordinador sin carrera; docente que no está en `docente`; **un segundo coordinador vigente de la misma carrera** (uno vigente y uno finalizado pasan); código de proyecto en minúsculas, horas 0 y 1000 (999.99 pasa), carreras o participantes vacíos, director inexistente, código repetido; las 7 acciones del registro y ninguna otra | `CargosYProyectosEsquemaIntegrationTest`, con Testcontainers (postgres:18.6-alpine) | 2026-10-25 |
| Los repositorios de cargos y proyectos | 7 de 7 pasan: cada cargo y cada proyecto se guarda y se lee igual, campo por campo (las carreras y los participantes en `jsonb`, las horas con sus decimales, como 14.25); `guardar` cambia los datos sin tocar la clave; `hayCoordinadorVigente` excluye al cargo que se corrige y a los finalizados; los dos candados hacen esperar a otra transacción. Docentes: 20 pruebas de integración sin fallas | `RepositorioCargosYProyectosJdbcIntegrationTest` | 2026-10-25 |
| Los límites del proceso `datos` | `ModulosTest` pasa: Docentes usa solo `ec.mdd.academico.puertos.entrada` (`ConsultarReferencias` con sus unidades y periodos) | `./gradlew :apps:datos:test --tests '*ModulosTest*'` | 2026-10-25 |

## Cargos (US1)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Registrar y corregir un cargo, con Mockito | `MantenerCargosServicioTest` (10): las referencias antes de la transacción; después bloquea, lee el docente, la clave y el coordinador vigente, guarda y registra `CARGO_REGISTRADO` (sin `unidad` si no la tiene); el cargo repetido, el segundo coordinador vigente y el docente en licencia o de medio tiempo, sin guardar; el docente que no existe da `DatoInvalido` en `docente`; **finalizar se guarda aunque el docente ya no esté habilitado** (si no, su advertencia no se podría atender); volver a vigente con otro coordinador da `CoordinadorVigente`; sin cambios no registra; un cargo que no existe da `CargoNoEncontrado` | `./gradlew :servicios:docentes:test` | 2026-10-25 |
| El contrato de los cargos | `CargosContratoTest`: 201 vigente con los nombres del docente y de la carrera y sin advertencias; 400 en `carrera` (falta o no existe), `unidad` (la de otra gestión), `fechaFin` (más de 2 años) y `documento` (61), sin registro; un cargo fuera del catálogo da 400 con los valores permitidos; los tres 409 (`DOCENTES_CARGO_REPETIDO`, `DOCENTES_COORDINADOR_VIGENTE`, `DOCENTES_DOCENTE_NO_HABILITADO`, sin la cédula en el cuerpo); `listarCargos` filtra por estado y por docente con el nombre de la unidad. Cada respuesta valida contra el contrato | `./gradlew :servicios:docentes:test --tests '*CargosContratoTest*'` | 2026-10-25 |
| El catálogo de Docentes | `ErroresDocentesTest`: las 6 entradas coinciden con `contratos/comun/errores.md`, con sus 8 columnas (D-72), y son 409 | `./gradlew :servicios:docentes:test --tests '*ErroresDocentes*'` | 2026-10-25 |

## Requisitos del cargo (US2)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| La advertencia de RN-20 y las otras dos | `CargoTest`: **cada coordinador y cada gestor G06 a G21 sin cuarto nivel trae `CUARTO_NIVEL`** con «Le falta un título de cuarto nivel registrado (RN-20).»; el rector, el vicerrector y el miembro de comisión, ninguna; con maestría, ninguna. `FECHA_FIN_VENCIDA` con el fin de ayer y no con el de hoy ni en un cargo finalizado; `DOCENTE_NO_HABILITADO` si el docente pasa a licencia o a medio tiempo. `CargoTest`, 41 pruebas sin fallas | `./gradlew :servicios:docentes:test --tests '*CargoTest*'` | 2026-10-25 |
| La advertencia en la API (escenario «Requisitos del cargo») | `CargosContratoTest`: un coordinador sin cuarto nivel se registra con 201 y su respuesta trae la advertencia; `listarCargos` la sigue mostrando y deja de mostrarla cuando la ficha gana una maestría; un coordinador cuyo docente pasa a licencia muestra `DOCENTE_NO_HABILITADO` y ese docente no recibe otro cargo (409). 7 pruebas sin fallas | `./gradlew :servicios:docentes:test --tests '*CargosContratoTest*'` | 2026-10-25 |

## Proyectos (US3)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Registrar y corregir un proyecto, con Mockito | `ProyectosServiciosTest`: las carreras antes de la transacción; después bloquea, verifica el director y los participantes con Docentes, guarda y registra `PROYECTO_REGISTRADO`; el código repetido da `ProyectoRepetido` sin guardar; un participante que no es docente da `DatoInvalido` en `participantes[1].docente`; suspenderlo registra `PROYECTO_CORREGIDO`; sin cambios no registra; uno que no existe da `ProyectoNoEncontrado` | `./gradlew :servicios:docentes:test --tests '*ProyectosServiciosTest*'` | 2026-10-25 |
| El contrato de los proyectos | `ProyectosContratoTest`: 201 con los nombres del director, de los participantes y de las carreras, sin `asignable` si no se pidió periodo; 400 en `codigo` (minúsculas), `carreras[1]` (repetida), `participantes[1].horas` (2.555), `participantes[1].docente` y `director` (no registrados), `vigenciaFin` y `horasAprobadas` (0); **participantes que suman 24.5 contra 24 aprobadas dan 400 en `participantes` con «suman 24.5 y pasan las 24 aprobadas»**; 409 `DOCENTES_PROYECTO_REPETIDO`. Cada respuesta valida contra el contrato | `./gradlew :servicios:docentes:test --tests '*ProyectosContratoTest*'` | 2026-10-25 |

## Asignables (US4)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| La regla de asignable, sin dobles (escenario «Proyecto no asignable») | `ProyectoTest`: aprobado y en ejecución, asignables; **finalizado y suspendido, no asignables por `ESTADO`**; **una vigencia que termina el día antes del periodo o empieza el día después de su fin, no asignable por `VIGENCIA`**; la que termina el mismo día que empieza el periodo, o empieza el mismo día que termina, sí cruza; suspendido y fuera de vigencia, los dos motivos. `ProyectoTest`, 34 pruebas sin fallas | `./gradlew :servicios:docentes:test --tests '*ProyectoTest*'` | 2026-10-25 |
| Los 11 proyectos de prueba en 2026-I y 2026-II | `ProyectosServiciosTest`: en 2026-I los 11 son asignables; en 2026-II los 7 de prácticas no lo son por su vigencia, y `VIN-2026-02` sí; un periodo que no existe da `DatoInvalido` en `periodo`; sin periodo no hay asignabilidad | `./gradlew :servicios:docentes:test --tests '*ProyectosServiciosTest*'` | 2026-10-25 |
| Asignables por la API | `ProyectosContratoTest`: `listarProyectos?periodo=2026-I` marca asignables a los dos vigentes y al suspendido con `["ESTADO"]`; con 2026-II, el de prácticas trae `["VIGENCIA"]`; un periodo que no existe da 400 en `periodo`; `consultarProyecto` trae `asignable`; uno que no existe, 404 | `./gradlew :servicios:docentes:test --tests '*ProyectosContratoTest*'` | 2026-10-25 |
| Los permisos de las 13 operaciones | `PermisosDocentesTest` (`VerificadorDePermisos`): las 6 de F03-H04 y las 7 nuevas exigen sus permisos alternativos, y con cada uno por separado pasan; **un token con solo `PROYECTOS:LECTURA` recibe 403 en las 13** (D-64, aclaración 8). Cambiar `DISTRIBUTIVO:LECTURA` por otra autoridad en las lecturas de los cargos la hace fallar (mutación revertida) | `./gradlew :servicios:docentes:test --tests '*PermisosDocentesTest*'` | 2026-10-25 |

## Corregir y finalizar (US5)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Finalizar y corregir sin borrar | `CargosContratoTest`: finalizar un coordinador con su fecha de fin responde 200 y su carrera recibe otro coordinador vigente; volver a vigente el primero da 409 `DOCENTES_COORDINADOR_VIGENTE`; el registro tiene `CARGO_REGISTRADO`, `CARGO_CORREGIDO` y `CARGO_REGISTRADO`; un cargo que no existe da 404. `ProyectosContratoTest`: suspender un proyecto responde 200 y registra `PROYECTO_CORREGIDO`; uno que no existe, 404. Ninguna operación borra. Docentes: 182 pruebas unitarias sin fallas | `./gradlew :servicios:docentes:test` | 2026-10-25 |

## Pantallas (US6)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas

- El número de gestores por tipo de IST (RN-10), el aviso del coordinador académico institucional (RN-09), las horas de gestión por cargo y el máximo de 16 h de docencia con proyecto son del motor (F06).
