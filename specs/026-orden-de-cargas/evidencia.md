# Evidencia: F01-H05

Registro de las comprobaciones de la historia (issue #31): el orden de las 10 cargas según sus dependencias, el estado de cada una, la confirmación «sin proyectos» por periodo y la fuente de datos intercambiable (RF-56, RF-18, RF-57, ERS §8.3). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Línea de base

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| `main` en verde | `78362d3` (#155): las 10 comprobaciones del PR en verde; antes, en #154, `./gradlew build integrationTest` con 1379 pruebas sin fallas y los 15 scripts de `deploy/probar_*.sh` terminando en 0 | PR #154 y #155, `specs/025-confirmar-carga/evidencia.md` | 2026-10-29 |
| El glosario | Reconstruido y con las 4 entradas de esta historia (research §12) | `sdd/modelo_dominio.md`, fuera del repositorio | 2026-10-29 |

## Base: contrato, catálogo, lenguaje publicado y orden

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| El contrato y los 3 códigos | `consultarEstadoDeLasCargas` (`GET /api/cargas/estado`) y `confirmarSinProyectos` (`POST /api/cargas/sin-proyectos`, periodo `^[0-9]{4}-(I\|II)$`, hasta 10); el 409 `CARGAS_CARGAS_ANTERIORES_PENDIENTES` en `validarArchivo`, `descargarInforme` y `confirmarCarga`; `EntradaDeHistorial` con `periodo` y sin `archivo` ni `hash` obligatorios. `CARGAS_CARGAS_ANTERIORES_PENDIENTES`, `CARGAS_PERIODO_NO_ENCONTRADO` y `CARGAS_PERIODO_CON_PROYECTOS` con sus 8 columnas, en «tú» | `contratos/cargas/openapi.yaml`, `contratos/comun/errores.md` | 2026-10-30 |
| `tieneDatos` en el lenguaje publicado | `AplicadorPorTraductoresTest`, 2 de 2, sin dobles: responde lo del traductor de esa carga; una carga que no atiende es `IllegalArgumentException`. `MotorDelLoteTest` sigue en 10 de 10 | `./gradlew :plataforma:lenguaje-cargas:check` | 2026-10-30 |
| `OrdenDeLasCargas`, sin dobles y con el diccionario real | 9 de 9: sin datos, a la 04 le falta la 02 y la 01, la 03 y la 07 no dependen de nadie; la 08 con la 01 y la 02 solo extraña la 06; las que faltan van en el orden de la ERS; el estado de las 10 en su orden; la 9 «sin proyectos» con sus periodos, y cargada aunque los tenga; «sin proyectos» no habilita a ninguna carga; pregunta una sola vez por carga | `OrdenDeLasCargasTest` | 2026-10-30 |

## Los dueños dicen si tienen datos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Académico, con PostgreSQL real | `AplicadorAcademicoIntegrationTest`: las 6 cargas sin datos, con datos tras confirmarlas, y solo con inactivas (unidades, carreras no vigentes, asignaturas y paralelos retirados) vuelven a no tener; periodos y matriculados cuentan en cualquier estado. Cada uno con un `select exists` | `./gradlew :servicios:academico:integrationTest` | 2026-10-30 |
| Docentes, con PostgreSQL real | `AplicadorDocentesIntegrationTest`: sin datos, con las 3 cargas de prueba, un docente en licencia cuenta, solo inactivos no; cargos solo finalizados no; un proyecto suspendido sí | `./gradlew :servicios:docentes:integrationTest` | 2026-10-30 |
| Normativa | `AplicadorNormativaIntegrationTest`: la política 2024 precargada (F04-H01) cuenta como datos de la carga 07; `TraductorPoliticaTest` con Mockito | `./gradlew :servicios:normativa:check integrationTest` | 2026-10-30 |

## El orden y el estado

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Validar y confirmar respetan el orden | `ValidarArchivoServicioTest` y `ConfirmarCargaServicioTest`: sin carreras, validar asignaturas lanza `CargasAnterioresPendientes` con `carreras` sin tocar la fuente de datos ni las referencias; confirmar cargos sin docentes no lee, no llama al dueño ni escribe el historial; si no se puede saber si una carga anterior tiene datos, no lee el archivo (error interno). `ConfirmacionContratoTest`: subir y confirmar carreras sin estructura responden 409 con el detalle `estructura_organica · 1 · Estructura orgánica`, validados contra el contrato, y sin historial | `./gradlew :servicios:cargas:test` | 2026-10-30 |
| El estado por la API | `EstadoContratoTest`: las 10 en su orden, con lo que falta; `PermisosCargasTest` con las 2 operaciones nuevas (lectura o distributivo para el estado, escritura para «sin proyectos») | `./gradlew :servicios:cargas:test` | 2026-10-30 |
| El proceso `datos` real | `ConfirmarLasDiezCargasIntegrationTest`, 8 de 8: con la base vacía, asignaturas antes que carreras se rechaza sin historial y el estado da las 10 pendientes salvo la política; una unidad registrada por su caso de uso (el camino de la pantalla), sin carga confirmada, habilita las carreras; las 10 en orden y todas cargadas; el estado de las 10 en **14 ms** (SC-003: menos de 2 s). `ModulosTest` en verde con el puerto nuevo de Docentes | `./gradlew :apps:datos:integrationTest test` | 2026-10-30 |

## «Sin proyectos»

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| El historial con V3 | `HistorialDeCargasJdbcIntegrationTest`, 15 de 15 (Testcontainers): «sin proyectos» se guarda una vez por periodo y la segunda devuelve la primera; se lee por periodo y en el historial de la carga; la base rechaza un «sin proyectos» sin periodo, con un periodo mal escrito, con archivo o como aplicada, y una entrada de archivo con periodo o sin archivo o hash. `resultado` pasó a 13 caracteres | `./gradlew :servicios:cargas:integrationTest` | 2026-10-30 |
| Los casos de uso | `EstadoYSinProyectosTest` (Mockito): una confirmación vale mientras su periodo no tenga asignables y deja de valer si los tiene; la de un periodo que ya no existe no vale; confirmar guarda en el historial de la carga de proyectos, con un periodo con asignables es `PeriodoConProyectos` y con uno que no existe `PeriodoNoEncontrado`, sin guardar; la repetida devuelve la que había. `ProyectosDesdePuertosTest` y `ConsultarAsignablesServicioTest` (Docentes, 4 de 4) | `./gradlew :servicios:cargas:test :servicios:docentes:test` | 2026-10-30 |
| Por la API, válidos e inválidos (principio XIV) | `EstadoContratoTest`: 201 la primera vez, 200 la segunda, y la carga 9 «sin proyectos (2026-II)»; con asignables 409; 2027-I 400 `CARGAS_PERIODO_NO_ENCONTRADO` con el detalle `periodo`; vacío, sin periodo, `2026-III`, `26-I`, `2026-i` y `20260-IIII` dan 400 `COMUN_SOLICITUD_INVALIDA`; 2026-I y 2026-II válidos | `./gradlew :servicios:cargas:test` | 2026-10-30 |
| Con el proceso `datos` real | Con las cargas 01 a 08 y sin proyectos, 2026-II se confirma una vez y la 9 queda «sin proyectos»; 2027-I no existe; con los proyectos de prueba, la 9 queda cargada, la confirmación deja de valer, otra da `PeriodoConProyectos` y el historial conserva la confirmación; con los proyectos suspendidos, vuelve a valer | `ConfirmarLasDiezCargasIntegrationTest` | 2026-10-30 |

## La fuente de datos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| El puerto con el término del glosario | `LectorDeArchivo` pasó a llamarse `FuenteDeDatos` (glosario «Fuente de datos»); `LectorPorFormato` es su adaptador de archivos | `puertos/salida/FuenteDeDatos.java` | 2026-10-30 |
| La regla de arquitectura | `ArquitecturaTest` de Cargas suma «ni `dominio` ni `aplicacion` dependen de `org.apache.poi..` ni de `..cargas.adaptadores..`». Con una clase de prueba en `aplicacion` que crea un `XSSFWorkbook`, la regla falla (`Architecture Violation … ViolacionDePrueba`); al quitarla, pasa | `./gradlew :servicios:cargas:test --tests '*ArquitecturaTest*'` | 2026-10-30 |
| Otra fuente, el mismo resultado | `FuenteDeDatosIntercambiableTest`: los ciclos de prueba entregados por una fuente que no lee bytes dan el mismo informe y la misma vista previa que el CSV leído por `LectorPorFormato` | `./gradlew :servicios:cargas:test` | 2026-10-30 |

## Pantalla

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Vitest | `Plantillas.test.tsx` suma 10: cada estado (cargada, pendiente, «sin proyectos (2026-II)»); «Subir archivo» deshabilitado con «Falta la carga de Carreras.» y «Faltan las cargas de Carreras y …» como descripción accesible; al confirmar docentes, cargos se habilita sin recargar; si el estado falla, el aviso y ningún estado falso; sin escritura ve el estado sin botones; «Sin proyectos…» confirma 2026-II y la fila cambia, un periodo mal escrito no se envía y dice cómo escribirlo, un 409 se muestra en el diálogo, y sugiere los periodos; el historial con la confirmación y su periodo. `validacionDelPeriodo.test.ts`: 3 válidos y 8 inválidos. SPA: 262 pruebas, 93,7 % de líneas | `npx vitest run --coverage` | 2026-10-30 |
| Playwright con axe | `e2e/plantillas.spec.ts`, 9 de 9: el botón deshabilitado con su descripción y el diálogo «Sin proyectos…» ya visible, sin fallas WCAG 2.1 AA | `npx playwright test e2e/plantillas.spec.ts` | 2026-10-30 |
| lint, tipos y formato | Sin observaciones | `npm run lint`, `npm run tipos`, `npm run formato:verificar` | 2026-10-30 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| `probar_orden_cargas.sh` de punta a punta | 9 de 9 pasos, dos veces seguidas: con la base vacía, la 04 antes que la 02 da 409 `CARGAS_CARGAS_ANTERIORES_PENDIENTES` (carreras) sin historial; el estado inicial; las 01 a 08 de prueba en orden y cargadas; `consolidacion.demo` confirma 2026-II «sin proyectos» (201, la segunda 200) y la 9 queda así; con la 09 y la 10, las 10 cargadas y la confirmación deja de valer; con proyectos, 409; 2027-I y 2026-III, 400; el historial de la 9; `coordinacion.demo` ve el estado y no confirma, `docente.demo` recibe 403 en los dos. Al terminar, la base sin nada de prueba (0/0/0/0/0/0) | `deploy/probar_orden_cargas.sh`, perfil reconstruido con el builder `mdd-limpio` | 2026-10-30 |
| Los 16 scripts de punta a punta | Todos terminan en 0. `probar_subida.sh` crea la sede temporal en la base vacía y la borra al salir | `deploy/probar_*.sh` | 2026-10-30 |
| Backend y SPA completos | Backend: 1440 pruebas sin fallas (1379 antes de la historia). Líneas: Cargas 92,7 %, Académico 82,1 %, Docentes 79,7 %, Normativa 75,3 %, `lenguaje-cargas` 95,2 %. SPA: 262 de Vitest (93,7 % de líneas) y 92 de 92 de Playwright | `./gradlew build integrationTest`, `npx vitest run --coverage`, `npx playwright test` | 2026-10-30 |
| Sin datos en los logs (principio IX) | Ningún valor de prueba (cédulas, apellidos, el código de la política, la sede temporal, `=1+1`, `NOEXISTE`) en los logs del gateway ni de `datos`: 0 coincidencias | `docker compose logs gateway datos \| grep -cE …` | 2026-10-30 |
| shellcheck y Gitleaks | shellcheck v0.11.0 sin observaciones en `probar_orden_cargas.sh` y `probar_subida.sh`; Gitleaks v8.30.1 sobre un clon de la rama: sin filtraciones | `koalaman/shellcheck-alpine:v0.11.0`, `ghcr.io/gitleaks/gitleaks:v8.30.1` | 2026-10-30 |
| El equipo de demostración con esta rama | `probar_equipo.sh` pasa todos sus pasos: despliegue, sin cambios, el token tras reiniciar identidad, el túnel, las claves, la vuelta atrás (141 s con `ESPERA_SALUD=60`) y el primer despliegue roto; 1401,8 MiB después de iniciar sesión (alerta: 7168) | `BUILDX_BUILDER=mdd-limpio deploy/equipo/probar_equipo.sh` | 2026-10-30 |
| Criterios de terminado de la constitución 2.2.0 (§6) | Los 4 escenarios del issue pasan (orden, las 10 de prueba, «sin proyectos», fuente intercambiable); contrato publicado y verificado; `ArquitecturaTest` (con la regla de RF-57) y `ModulosTest` pasan; los 3 códigos nuevos con sus 8 columnas, cubiertos por las pruebas de contrato; el periodo validado en contrato, servidor y pantalla con casos válidos e inválidos; mensajes en «tú»; JavaDoc de los puertos, el dominio y el lenguaje publicado al día; SpotBugs en verde; el glosario con los 4 términos nuevos antes del código | Esta evidencia | 2026-10-30 |

## Notas

- **Un puerto de entrada nuevo en Docentes.** El plan decía que Cargas llamaría a `ConsultarProyectos.listar(periodo, …)`, pero esa respuesta trae tipos del dominio de Docentes (`Proyecto`, `Asignabilidad`), y Cargas solo puede usar los puertos de entrada de otro servicio (principio V). Docentes publica `ConsultarAsignablesDelPeriodo`, que responde con un tipo de su paquete (`PERIODO_NO_ENCONTRADO`, `NINGUNO` o `ALGUNO`) y aplica la misma regla de F03-H05. `ModulosTest` lo comprueba.
- **Sin `additionalProperties: false`.** El contrato de diseño lo pedía para el cuerpo de «sin proyectos», pero ningún contrato del proyecto lo usa y el servidor ignora los campos desconocidos en todos. Se quitó para seguir la convención.
- **El periodo, sugerido.** Talento Humano tiene «Cargas: escritura» pero no lee los periodos de Académico. El diálogo pide el periodo en un campo validado, con los periodos como sugerencias para quien puede leerlos.
- **El perfil con imágenes viejas.** La primera corrida de `probar_orden_cargas.sh` falló en 9 pasos porque el builder `mdd-limpio` no se creó (`docker buildx create -q` falla en silencio aquí) y el perfil se levantó con las imágenes de hace 3 días. Con el builder creado sin `-q` y el perfil reconstruido, pasó. El script limpió lo suyo en las dos corridas.
- **El equipo de demostración, detenido.** Al verificar, sus 8 contenedores estaban «Exited» desde hacía 2 días, igual que el 2026-10-26; no lo detuvo esta verificación (`probar_equipo.sh` usa el proyecto `mdd-prueba`) y no se tocó.
- **go-jose en las imágenes (GHSA-mgcc-cww5-fp2q).** En la CI del PR #156, Grype encontró go-jose 4.1.5 en el binario de Caddy 2.11.7 y 4.1.4 en el de cloudflared 2026.9.3, corregido en 4.1.6. Son imágenes oficiales: tienen una excepción temporal en `.grype.yaml` hasta el 2026-11-16, aprobada por el autor, con el túnel apagado mientras dure.
