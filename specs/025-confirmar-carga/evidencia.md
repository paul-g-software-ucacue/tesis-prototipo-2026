# Evidencia: F01-H04

Registro de las comprobaciones de la historia (issue #30): la vista previa de cada carga frente a lo que hay en el sistema, la confirmación que el servicio dueño aplica completa o nada, y el historial de las cargas (RF-10, RF-55, MD-13; ADR-022, D-74). Cada fila dice el comando y la salida resumida, o enlaza al run de GitHub Actions. Los datos son ficticios; las contraseñas de local y de la CI también.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## Base: decisión, contrato, motor del lote e historial

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| ADR-022 y D-74 | Registrados en `docs/decisiones.md`, con la aprobación del autor del 2026-10-26. ADR-022: el lenguaje publicado en `:plataforma:lenguaje-cargas` (ver la nota del nombre). D-74: los mensajes en «tú» | `docs/decisiones.md` | 2026-10-26 |
| El contrato y los 3 códigos | `VistaPrevia`, `RegistroDeLaVista`, `CambioDeCampo`, `confirmarCarga` (`huella` `^[0-9a-f]{64}$`), `listarHistorial` y `EntradaDeHistorial`; `CARGAS_CARGA_CON_ERRORES`, `CARGAS_VISTA_PREVIA_DESACTUALIZADA` y `CARGAS_CARGA_RECHAZADA` con sus 8 columnas. Se generan los modelos de Java y los tipos de la SPA; `verificar:ruptura` pasa | `contratos/cargas/openapi.yaml`, `contratos/comun/errores.md` | 2026-10-26 |
| El motor del lote, sin dobles | `MotorDelLoteTest`, 9 de 9: nuevos, los que cambian con sus campos, las bajas dentro del alcance (la de otro periodo no cuenta) y los iguales; la huella estable al reordenar y distinta con otro valor; con la misma huella aplica 1 nuevo y 1 cambio, sin tocar bajas ni iguales, y bloquea una vez; con otra huella, `VistaPreviaDesactualizada` y nada aplicado; una regla del dueño en la fila 3 da `RechazoDelLote` con esa fila y deshace también la fila 2; una clave repetida, con la fila de la primera; 100 de cada tipo; el SHA-256 | `./gradlew :plataforma:lenguaje-cargas:check` | 2026-10-26 |
| El historial (V2) | `HistorialDeCargasJdbcIntegrationTest`, 7 de 7 (Testcontainers): guarda y lee cada resultado, del más reciente al más antiguo; la primera `APLICADA` de un hash; la base rechaza un hash que no es hexadecimal, un resultado desconocido, un total negativo, un rechazo sin motivo, una aplicada con motivo y una carga en mayúsculas. `CargasEsquemaIntegrationTest` ve `cargas.historial_carga` | `./gradlew :servicios:cargas:integrationTest` | 2026-10-26 |
| Confirmar e historial en Cargas (Mockito) | `ConfirmarCargaServicioTest` (6): aplicada con su lote (el hash SHA-256 de los bytes y las filas) y su entrada; con errores, `CargaConErrores` sin llamar al dueño y su entrada `RECHAZADA` con `CON_ERRORES`; repetida sin llamar al dueño, con la fecha de la primera; rechazada por el dueño y desactualizada, con su motivo. `ValidarArchivoServicioTest` (7): la vista previa es la del dueño; con errores, solo `filasConErrores`, sin llamar al dueño. `DuenosDeLasCargasTest`: una carga sin dueño, con dos o fuera del diccionario detiene el arranque | `./gradlew :servicios:cargas:test` | 2026-10-26 |
| El contrato, los permisos y el catálogo | `ConfirmacionContratoTest`, 7 de 7, con un dueño y un historial en memoria: la vista previa con sus totales y su huella; confirmar dos veces da `APLICADA` y `REPETIDA` (con la fecha de la primera), y al subirla otra vez, 0 nuevos y 7 iguales; el historial en orden; los tres 409 con su código, el rechazo con `fila 2` y el texto del catálogo del dueño; una huella que no es hexadecimal, 400; un historial de una carga que no existe, 404; cada respuesta valida contra el contrato. `PermisosCargasTest` con las 2 operaciones nuevas; `ErroresCargasTest`, 10 códigos, y un rechazo sin código del dueño no muestra el mensaje técnico. Cargas: 270 pruebas sin fallas | `./gradlew :servicios:cargas:test` | 2026-10-26 |

## Académico A: estructura y carreras

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Los traductores de la 01 y la 02 (Mockito) | `TraductoresAcademicoATest`, 5 de 5: los padres antes que los hijos; una unidad nueva inactiva se registra y cambia de estado; una igual no cambia, y una inactivada cambia solo de estado, sin corregirla; el tipo de una unidad no cambia y un `DatoInvalido` se rechaza con su fila y su campo; una carrera con otro nombre y otras jornadas cambia en esos dos campos, y `MATUTINA;NOCTURNA` con `05` ciclos es igual a la registrada; una carrera nueva en cierre se registra y cambia de estado | `./gradlew :servicios:academico:test` | 2026-10-26 |
| **La 01 y después la 02, aplicadas** (PostgreSQL) | `AplicadorAcademicoIntegrationTest`, 4 de 4: 24 unidades y 7 carreras, con el registro de cambios de `datos.demo`, y subidas otra vez, todo igual; **una segunda sede matriz rechaza la fila 26 con `SedeMatrizExistente` y no aplica nada, ni las 25 filas anteriores** (SC-003); una carrera que cambia se corrige y la que no viene queda como estaba (1 cambiada, 1 baja sin aplicar); la misma huella aplicada dos veces da `VistaPreviaDesactualizada` la segunda | `./gradlew :servicios:academico:integrationTest` | 2026-10-26 |
| Los límites del proceso | `ModulosTest` pasa: Académico usa `ec.mdd.plataforma.cargas` (su interfaz nombrada) y Cargas no conoce sus tipos | `./gradlew :apps:datos:test` | 2026-10-26 |

## Académico B: ciclos, asignaturas, oferta y matriculados

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Los matriculados (V6) | `MatriculaTest` (6, sin dobles): un total de 0, 1 y 99 999 pasa, y -1 y 100 000 no; la fuente hasta 60; el periodo y la carrera, obligatorios. `RepositorioMatriculasJdbcIntegrationTest` (2): se guardan y se leen por periodo, y la base exige el periodo y la carrera. `AcademicoEsquemaIntegrationTest` ve `academico.matricula` | `./gradlew :servicios:academico:test :servicios:academico:integrationTest` | 2026-10-26 |
| Los traductores de la 03, 04, 05 y 10 (Mockito) | `TraductoresAcademicoBTest`, 4 de 4: en los periodos, las semanas, la resolución y el estado del archivo no cuentan, y una fecha de fin sí; en las asignaturas, `3` y `3.0` son iguales, la malla que no existe se registra antes y una vez, y la carrera o la malla no cambian (`DatoInvalido` en su campo); en la oferta, las bajas solo en los periodos del archivo y las franjas no cuentan; en los matriculados, la clave es `periodo · carrera` | `./gradlew :servicios:academico:test` | 2026-10-26 |
| **Las 6 cargas de Académico en su orden** (PostgreSQL) | `AplicadorAcademicoIntegrationTest`, 5 de 5: la 01, 02, 03, 04, 05 y 10 de prueba dan 24, 7, 2, 300, 165 y 7 nuevos, con 12 mallas, y Roles recibe el aviso de los 2 periodos; **subidas otra vez, ninguna tiene nuevos, cambios ni bajas** (SC-002) | `./gradlew :servicios:academico:integrationTest` | 2026-10-26 |

## Docentes: docentes, cargos y proyectos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Los traductores de la 06, 08 y 09 (Mockito) | `TraductoresDocentesTest`, 5 de 5: **los 47 docentes, los 21 cargos y los 11 proyectos del archivo de prueba son iguales a los que construyen las pruebas de Docentes desde el mismo archivo** (la traducción es fiel, con la ficha académica y las listas); pasar un docente a licencia cambia solo el estado y se aplica con `cambiarEstado`; el tipo de identificación no cambia; un `DatoInvalido` se rechaza con su campo; un cargo nuevo se registra con `MantenerCargos` | `./gradlew :servicios:docentes:test` | 2026-10-26 |
| **La 06, 08 y 09 en su orden** (PostgreSQL) | `AplicadorDocentesIntegrationTest`, 3 de 3: 47 docentes, 21 cargos y 11 proyectos aplicados, y subidos otra vez, todos iguales; **un segundo coordinador vigente de la misma carrera rechaza la fila 23 con `CoordinadorVigente` (RN-12) sin aplicar ninguno de los 21 cargos**; un cargo para un docente de medio tiempo, `DocenteNoHabilitado`, sin cambios | `./gradlew :servicios:docentes:integrationTest` | 2026-10-26 |

## Normativa: la política

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| **La carga 07 como versión de normativa** (PostgreSQL) | `AplicadorNormativaIntegrationTest`, 6 de 6, cada una con su base: la carga 07 de prueba frente a la versión 1 da 21 iguales y 1 que cambia (la fila 22, `horas_otras_docencia` y `horas_gestion` de vacío a 0, research §7); con la versión 1 sin periodos, confirmar la edita y subida otra vez es igual; **con la versión 1 asociada a un periodo, se rechaza con `VersionEnUso` y no cambia nada** (principio VII); un código nuevo crea la versión 2 copiando la 1, con sus mismas reglas, y le pone las 22 filas; una fila de menos es una baja que Normativa rechaza al confirmar; la referencia y la observación no cuentan como cambio | `./gradlew :servicios:normativa:integrationTest` | 2026-10-26 |
| La política se aplica entera | El motor del lote suma un `aplicar(lote, vista, quien)` en el traductor: por defecto fila por fila (las 9 cargas restantes) y, en la política, versión por versión con `CrearVersion` y `EditarVersion`, la única forma de cambiar una versión (F04-H03). `MotorDelLoteTest` sigue en verde | `./gradlew :plataforma:lenguaje-cargas:test` | 2026-10-26 |

## Las 10 en orden

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| **Las 10 cargas de prueba en su orden** (SC-001) | `ConfirmarLasDiezCargasIntegrationTest`, con el proceso `datos` real y un PostgreSQL propio y vacío: la 01 a la 10, validadas sin errores y confirmadas en su orden, quedan `APLICADA`; cada una encuentra las referencias de las anteriores; quedan 24 unidades, 300 asignaturas, 165 asignaturas-paralelo, 7 matriculados, 47 docentes, 21 cargos y 11 proyectos | `./gradlew :apps:datos:integrationTest` | 2026-10-26 |
| **Subidas otra vez, ningún cambio** (SC-002) | Las 10, validadas otra vez, dan todos sus registros iguales: 0 nuevos, 0 que cambian y 0 bajas | `ConfirmarLasDiezCargasIntegrationTest` | 2026-10-26 |
| Cancelar y confirmar dos veces (US3, US4, US5) | Subir sin confirmar no cambia las unidades, las carreras, los docentes ni el historial; confirmar otra vez la 02 con el mismo hash da `REPETIDA` sin una fila más en el registro de cambios de Académico; el historial de la 02 es `REPETIDA` y `APLICADA`, y el de las 10, 11 entradas | `ConfirmarLasDiezCargasIntegrationTest` | 2026-10-26 |
| **Un archivo de casi 5 MB** (SC-004) | 5 241 643 bytes y 10 568 docentes ficticios nuevos: la vista previa en 408 ms y la confirmación en **15,5 s** (límite 30 s y 60 s; meta del plan, 20 s). La primera medición dio 55,8 s: cada docente volvía a leer todas las referencias de Académico. Ahora, dentro de la transacción de una carga, se leen una vez (`AcademicoDesdePuertos`, con su prueba: dentro de una transacción una lectura, fuera, cada vez), sin cambiar ninguna regla | `ConfirmarLasDiezCargasIntegrationTest`, `AcademicoDesdePuertosTest` | 2026-10-26 |

## Pantalla

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| La vista previa, confirmar, cancelar y el historial (Vitest) | `Plantillas.test.tsx`, 25 de 25 (7 nuevas): la vista previa con sus totales, el aviso de las bajas y las pestañas «Nuevos», «Cambian» (con `dedicacion: «MT» → «TC»`) y «Se dan de baja»; con errores, «Confirmar» deshabilitado y su porqué; «Cancelar» no llama al servidor; «Confirmar» envía el archivo con la huella y dice «Carga aplicada: 1 nuevo y 1 cambiado; 1 baja quedó sin aplicar.»; una repetida dice cuándo se confirmó; un rechazo muestra su título y `fila 23: …`; «Historial» abre los intentos, el más reciente primero, con el resultado y el motivo en español. La SPA: 241 pruebas, 93,7 % de líneas; `lint`, `tipos` y `formato:verificar` en verde | `npx vitest run --coverage --maxWorkers=4` | 2026-10-26 |
| En el navegador, con axe (Playwright) | `e2e/plantillas.spec.ts`, 7 de 7: la vista previa de un archivo de docentes; «Confirmar» envía el archivo y la `huella` con el token; el resultado; el historial en su diálogo; axe-core sin fallas WCAG 2.1 AA con la vista previa y con el historial a la vista | `npx playwright test e2e/plantillas.spec.ts` | 2026-10-26 |
| Los mensajes en «tú» (D-74) | Los motivos de Cargas («Escribe…», «Usa uno de los valores permitidos…», «complétalo», «no la repitas», «regístrala o confirma antes su carga») y la pantalla («Corrígelo y vuelve a subirlo.»). Una búsqueda de «Escriba», «complételo», «regístrela», «cárguela», «Corríjalo», «vuelva a», «Use uno» y otras formas de «usted» en Cargas, la pantalla y `probar_reglas_carga.sh` no encuentra ninguna | `grep -rnE …` | 2026-10-26 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| `probar_confirmar_carga.sh` de punta a punta | 9 de 9 pasos, dos veces seguidas: las 10 cargas de prueba validadas y confirmadas en su orden (la política como la versión nueva `P666-POL-2024`, sin tocar la versión 1); subidas otra vez, sin nuevos ni cambios; la 02 otra vez, `REPETIDA`; la 01 con una unidad menos, la baja sin aplicar y las 24 unidades siguen; una vista previa que cambió antes de confirmar, `CARGAS_VISTA_PREVIA_DESACTUALIZADA`; un segundo coordinador vigente, `CARGAS_CARGA_RECHAZADA` en su fila y sin cambios; el historial de la 02 en su orden; `consolidacion.demo` confirma y `docente.demo` recibe 403. Al terminar, la base quedó sin nada de prueba (0 unidades, 0 carreras, 0 docentes, 0 entradas de historial y 0 versiones con el prefijo) | `deploy/probar_confirmar_carga.sh`, perfil reconstruido con el builder `mdd-limpio` | 2026-10-27 |
| Los 15 scripts de punta a punta | Todos terminan en 0 con el perfil reconstruido | `deploy/probar_*.sh` | 2026-10-27 |
| Backend y SPA completos | Backend: 1379 pruebas sin fallas (`./gradlew build integrationTest`). Líneas: Cargas 93,4 %, Académico 82,9 %, Docentes 80 %, Normativa 75,5 % (62,3 % antes de `TraductorPoliticaTest`, que pedía T015 y faltaba). SPA: 241 de Vitest y 90 de 90 de Playwright | `./gradlew build integrationTest`, `npx vitest run`, `npx playwright test` | 2026-10-27 |
| Sin datos en los logs (principio IX) | Ningún valor de prueba (las cédulas de prueba, apellidos, el código de la política, `=1+1`, `NOEXISTE`) en los logs del gateway ni de `datos`: 0 coincidencias | `docker compose logs gateway datos \| grep -cE …` | 2026-10-27 |
| shellcheck y Gitleaks | shellcheck v0.11.0 sin observaciones en los scripts nuevos o cambiados; Gitleaks v8.30.1 sobre un clon de la rama: 67 commits, sin filtraciones | `koalaman/shellcheck-alpine:v0.11.0`, `ghcr.io/gitleaks/gitleaks:v8.30.1` | 2026-10-27 |
| El equipo de demostración con esta rama | `probar_equipo.sh` pasa todos sus pasos: despliegue, sin cambios, el token tras reiniciar identidad, el túnel, las claves, la vuelta atrás (127 s con `ESPERA_SALUD=60`) y el primer despliegue roto; 1339,7 MiB después de iniciar sesión (alerta: 7168). El equipo de demostración siguió con sus 8 contenedores sanos | `BUILDX_BUILDER=mdd-limpio deploy/equipo/probar_equipo.sh` | 2026-10-27 |
| Criterios de terminado de la constitución 2.2.0 (§6) | Los escenarios del issue pasan (vista previa, cancelar, confirmar, confirmación repetida); el contrato está publicado y verificado; `ArquitecturaTest` y `ModulosTest` pasan; los 3 códigos nuevos tienen sus 8 columnas y los cubren las pruebas de contrato; la validación de las filas es la de F01-H03 y la `huella` se valida como 64 hexadecimales; los mensajes dicen qué hacer, en «tú» (D-74); el JavaDoc de los puertos, del dominio y del lenguaje publicado está al día; SpotBugs en verde; el glosario no cambia | Esta evidencia | 2026-10-27 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas

- **D-74.** El plan reservaba D-74 para el índice de ADR-022. Como el autor aprobó dos decisiones en el plan, ADR-022 es el módulo `:plataforma:lenguaje-cargas` y D-74 es el trato de «tú» en los mensajes (research §10).
- **El nombre del módulo.** El plan lo llamaba `:plataforma:cargas`, pero Gradle trata como el mismo artefacto a dos proyectos con el mismo nombre, y `:servicios:cargas` ya se llama así: la compilación daba una dependencia circular. El módulo es `:plataforma:lenguaje-cargas`; su paquete sigue siendo `ec.mdd.plataforma.cargas`.
- **Vitest con la máquina cargada.** Con el navegador y el equipo de demostración en la misma PC (carga 15 en 12 núcleos), 9 pruebas de otras pantallas (proyectos, docentes, normativa, roles, carreras) llegaron a su límite de 15 s; pasan solas y con `--maxWorkers=4`. No las toca esta historia.
- **El equipo de demostración, detenido a mano.** La PC se reinició el 2026-10-26 a las 20:29 y Docker levantó los 8 contenedores del equipo; a las 20:45 alguien los detuvo uno por uno (`docker stop`), y por `restart: unless-stopped` no volvieron. El autor los levantó con `docker compose up -d --wait` sobre la versión actual. No es un defecto del despliegue.
- **curl en las imágenes (CVE-2026-92392).** En la CI del PR #154, Grype encontró curl y libcurl 8.22.0-r0, corregidos en 8.23.0-r0. La imagen del frontend ya los actualiza (`apk upgrade … curl libcurl`, sigue con el usuario 101). Las imágenes oficiales de PostgreSQL 18.6 y Caddy 2.11.7 tienen una excepción temporal en `.grype.yaml` hasta el 2026-11-16, aprobada por el autor, con el túnel apagado mientras dure.
- **La prueba del historial en la CI.** axe midió el contraste con el diálogo a mitad de su transición; ahora la prueba espera a que el diálogo sea visible (`opacity: 1`), como las demás pantallas.

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| El despliegue de `7bc44f9` (#154) | GitHub `demostracion`: «success · Sano en 48 s»; los 8 contenedores sanos con las imágenes `…-7bc44f9e877a`; el proceso `datos` aplicó `cargas` v2 (historial de cargas) y `academico` v6 (matriculados); la imagen del frontend con curl 8.23.0-r0 | GitHub Deployments, `docker ps`, `docker logs mdd-datos-1` (solo lectura) | 2026-10-27 |
| La comprobación del autor con `consolidacion.demo` | La carga 02 (carreras) validada, con su vista previa, y confirmada: «Aplicada», 7 nuevas, 0 cambiadas y 2 bajas sin aplicar (las carreras que ya había en su base siguen). El historial de Carreras muestra el intento con la fecha, el usuario, el archivo, la huella abreviada y los totales | Captura del autor (no se copia al repositorio) | 2026-10-27 |
