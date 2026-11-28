# Research: Validar antes de exportar y registrar la exportación

F19-H02 (#267). Cada sección dice qué se decidió, por qué y qué se descartó. Parte de lo que dejó F19-H01 (D-119):
- `GenerarCsvSigaServicio`, que lee la publicación vigente, el layout de exportación vigente y el contenido congelado completo;
- las comprobaciones de `FilasParaSiga` contra el `ReporteGeneral`;
- el puerto `ExportadorSiga`;
- la entrada `CSV_SIGA_GENERADO` en la transacción del registro, antes de entregar.

Lo que se encontró en el código, y que cambia el diseño:
- **Firma** guarda la cadena por documento, y el documento por periodo, versión y tipo (`documento_firmable`, con `hash_contenido`). `FirmasDelDocumento.cadenaConEstado()` ya da el estado de cada paso, y el registro tiene el rechazo y la anulación. Pero Firma **no tiene ruta interna**: su ruta pública `consultarDocumentoFirmable` es la del documento **vigente** del periodo, que durante una reforma es el de la versión nueva.
- **Generación no guarda las inconsistencias**: el panel se calcula al abrirlo, con la versión vigente, la oferta de hoy y las reglas del periodo (V5: «El panel se calcula al abrirlo: solo esto se guarda»). Aprobación exige 0 abiertas para pasar a EN_REVISION_VRA (`ProcesoAprobacion`), pero no deja constancia del número, y no lo vuelve a mirar al congelar.

## §1. «Firmada»: una ruta interna de Firma por versión (FR-002)

**Decisión.**
- **La ruta:** Firma publica `GET /interno/firma/periodos/{periodo}/versiones/{numero}/firmas` (`consultarFirmasDeLaVersion`), con el token de proceso (ADR-023).
- **Lo que devuelve:** la cadena del documento CONSOLIDADO de esa versión (`FirmasDeLaVersion`):
  - el `hashContenido` del documento;
  - los pasos, cada uno con su orden y su estado (`FIRMADO`, `PENDIENTE` o `RECHAZADO`; el `EN_TURNO` de la vista pública se informa como `PENDIENTE`);
  - si sus firmas están anuladas (`anulada`) y si se rechazó en firma (`rechazada`).
- **El 404** (`COMUN_RECURSO_NO_ENCONTRADO`): si esa versión no tiene documento.
- **La regla es de Reporting** (`FirmasDeLaVersion.completas(hashPublicado)`, en su dominio). Las firmas están completas si se cumplen las cuatro condiciones:
  - el `hashContenido` es el `hash` de la publicación vigente;
  - hay al menos un paso;
  - todos los pasos están FIRMADOS;
  - no está anulada ni rechazada.
- **Sin documento:** si Firma responde 404, la versión no está firmada. Es un rechazo, no un error técnico.

**Por qué.**
- Firma es el dueño de la cadena (principio V): Reporting no lee su esquema.
- La ruta es por versión porque la publicada vigente puede no ser la última versión del periodo (D-113).
- Firma informa hechos, sin decidir qué significa «exportable», porque esa regla es de la exportación (RF-53).
- Para una versión publicada, «anulada» y «sin documento» no deberían pasar nunca. Si pasan, se rechaza (falla cerrada), que es lo que pide RF-53.

**Alternativas.**
- Usar `consultarDocumentoFirmable`: da el documento vigente, no el de la versión publicada, y exige un permiso de usuario.
- Que Firma responda un booleano `completa`: mezcla la regla de la exportación en Firma, y el booleano no dice qué falta para el mensaje.
- Dar «firmada» por hecho porque llegó a PUBLICADO, como en F19-H01: es lo que RF-53 pide comprobar.
- Pedírselo a Aprobación: Aprobación no conoce el estado de cada paso (MD-05).

## §2. «Sin inconsistencias»: Generación las guarda al congelar (FR-003)

**Decisión.**
- **Lo que se guarda:** cuando Generación guarda el contenido congelado de una versión (`GuardarContenidoCongeladoServicio`, el mismo momento de D-115), guarda también sus inconsistencias abiertas: duras y blandas sin justificar, del panel (MD-07).
- **Dónde:** dos columnas opcionales en `generacion.contenido_congelado`, `duras_abiertas` y `blandas_abiertas` (V11).
- **Cuándo se calculan:** con el panel de la versión vigente del periodo, y solo si esa vigente es la versión que se guarda.
  - Al congelar siempre lo es.
  - En un guardado tardío (F20-H01), solo si no empezó una reforma.
  - Si no lo es, las columnas quedan vacías: no se adivina.
- **Cómo llegan a Reporting:** la ruta `consultarContenidoCongelado` suma el campo opcional `inconsistenciasAbiertas` (`{duras, blandas}`). Reporting ya la llama para las filas, así que no hay una llamada más.
- **La regla, en Reporting:** sin inconsistencias es `duras + blandas == 0`. Si el campo falta (un contenido guardado antes de esta historia, o tardío con una reforma en curso), la condición **no se cumple**, con su propio motivo: «no se registraron las inconsistencias de la versión».

**Por qué.**
- **FR-003** pide las inconsistencias «de esa versión, sin volver a evaluarla con las reglas de hoy». Volver a correr el motor sobre una versión publicada usaría la oferta y las reglas de hoy, como el problema que D-115 resolvió para las horas.
- **El momento de congelar** es cuando la versión deja de cambiar, y el panel ya calcula exactamente lo que pide MD-07.
- **Aprobación** exige 0 abiertas para pasar a revisión, pero el Vicerrector puede ajustar después (RF-35). La única forma de comprobar «sin inconsistencias» sobre lo que se firmó es fijarlo al congelar.
- **Falla cerrada** cuando falta el dato: en el equipo de demostración, 2026-I todavía no se congeló, y el script recrea el distributivo, así que toda versión que se publique tendrá el dato.

**Alternativas.**
- **Evaluar el panel al exportar:** solo sirve para la versión vigente; durante una reforma, la vigente es otra. Además usa la oferta de hoy.
- **Una ruta interna de inconsistencias por versión:** el panel no se puede calcular para una versión que no es la vigente; habría que generalizar el motor (principio XI).
- **Guardar la lista completa de inconsistencias:** la exportación solo necesita los dos números. La lista se ve en el panel mientras la versión está viva.
- **Que Aprobación anote el número en la transición a EN_REVISION_VRA:** no cubre los ajustes del Vicerrector posteriores.
- **Dar por buena una versión sin el dato:** relaja RF-53 justo en el caso que no se puede comprobar.

## §3. La validación, en el dominio de Reporting (FR-001 a FR-008)

**Decisión.**
- **`CondicionesDeExportacion`:** un objeto de valor nuevo. Se arma con la `Publicacion`, `Optional<FirmasDeLaVersion>` y `Optional<InconsistenciasAbiertas>`, y da la lista de `MotivoDeRechazo`:
  - `FIRMAS_INCOMPLETAS`, con cuántos pasos están firmados de cuántos;
  - `SIN_DOCUMENTO_FIRMADO`;
  - `DOCUMENTO_DE_OTRA_VERSION`;
  - `FIRMAS_ANULADAS_O_RECHAZADAS`;
  - `INCONSISTENCIAS_ABIERTAS`, con las duras y las blandas;
  - `INCONSISTENCIAS_SIN_REGISTRO`.
- **Todos los motivos juntos:** no se corta en el primero (caso límite «varias condiciones a la vez»).
- **`exigir()`:** lanza `ExportacionRechazada(periodo, version, motivos)` si la lista no está vacía.

**El orden en `GenerarCsvSigaServicio`:**
1. La publicación vigente: sin ella, `SinPublicacion` (409), como en F19-H01, sin entrada.
2. El layout vigente.
3. Las firmas (Firma) y el contenido completo (Generación, que trae las inconsistencias).
4. `CondicionesDeExportacion.exigir()`. Si rechaza, se registra el rechazo (§5) y se relanza.
5. Las comprobaciones de F19-H01: el hash del contenido y los totales del reporte general (FR-006).
6. El archivo, su suma de verificación, el registro y la entrada, en una transacción (§4).
7. Se entrega.

**Por qué.** Es la regla de la exportación (RF-53), así que vive en el dominio de Reporting y se prueba sin dobles (principio VIII). Las lecturas siguen siendo una por fuente, sin importar cuántos docentes haya.

**Alternativas.** Validar en el controlador; un servicio de dominio con interfaz (principio XI: no hay un segundo validador).

## §4. La suma de verificación y el registro (FR-009 a FR-015)

**Decisión.**
- **`SumaDeVerificacion`:** objeto de valor de Reporting. `SumaDeVerificacion.de(byte[])` calcula el SHA-256 con `MessageDigest` del JDK, sobre los bytes exactos que devuelve `ExportadorSiga`, y lo guarda en hexadecimal en minúsculas, validado con `^[0-9a-f]{64}$`. El JDK no es infraestructura: el dominio sigue sin framework (principio I).
- **`ExportacionSiga`:** agregado nuevo de Reporting. Guarda:
  - el `id` (UUID), quién y cuándo;
  - el periodo, la versión y su `hash`;
  - la versión del layout de exportación;
  - el nombre del archivo, el número de filas y la `SumaDeVerificacion`.
  - Se crea con `ExportacionSiga.de(publicacion, layout, archivo, filas, quien, momento)`.
- **Dónde se guarda:** `reporting.exportacion_siga` (V3), sin `update` ni `delete` (un disparador, como `layout_exportacion`), con un índice por periodo y momento.
- **El puerto `ExportacionesSiga`:** `guardar(ExportacionSiga)` y `delPeriodo(periodo)`, de la más reciente a la más antigua. El adaptador es `ExportacionesSigaJdbc`.
- **Una transacción:** `transaccion.ejecutar(() -> { exportaciones.guardar(e); registro.registrar(e); })`. La entrada va al registro persistente de publicaciones de la misma base (ADR-018), así que el registro y la entrada se guardan los dos o ninguno (FR-013). Después se entrega.
- **`CsvGenerado` desaparece:** la entrada se arma con la `ExportacionSiga` (`registrar(ExportacionSiga)`).
- **El archivo no se guarda:** el registro alcanza para regenerarlo idéntico (Assumptions).

**Por qué.**
- **SHA-256** es el hash del glosario y el de toda la base (D-23, las cargas, el layout).
- **Hexadecimal en minúsculas** es lo que imprimen `sha256sum` y `certutil` (§7).
- **El agregado** es el término del glosario (`ExportacionSiga`).
- **La transacción** es la que ya existe para la entrada: solo se le suma el `insert`.

**Alternativas.**
- Base64, como `Repr-Digest`: no se compara a simple vista con las herramientas.
- Guardar el CSV: duplica lo congelado.
- Un número correlativo por periodo en lugar del UUID: exige bloquear para numerar, y nadie lo pide. La lista ordena por momento.

## §5. La bitácora (FR-014, FR-016)

| Acción | Cuándo | Objeto | Nuevo |
|---|---|---|---|
| `CSV_SIGA_GENERADO` (amplía) | En la transacción del registro, antes de entregar | `{periodo, version}` | `{layout, exportacion, filas, sha256}`: la versión del layout, el `id` de la `ExportacionSiga`, el número de filas y la suma de verificación |
| `EXPORTACION_SIGA_RECHAZADA` (nueva) | Cuando `CondicionesDeExportacion` rechaza, en su propia transacción, antes de responder el error | `{periodo, version}` | `{motivos, firmados, pasos, duras, blandas}`: los códigos de `MotivoDeRechazo` y, cuando se conocen, los números |

- **Una entrada por exportación:** se conserva el nombre `CSV_SIGA_GENERADO`. El script de demostración, las pruebas y los registros de la demo ya lo usan, y el glosario dice que el CSV para SIGA, con su suma de verificación y su registro, *es* la Exportación a SIGA. Renombrarla dejaría dos nombres para la misma acción en la bitácora.
- **El rechazo:** solo el de `CondicionesDeExportacion` (aclaración del 2026-11-22). `SinPublicacion`, un proceso que no responde y `ContenidoInconsistente` no dejan entrada. Si la entrada del rechazo no se puede guardar, se registra en el log y se responde el mismo 409: el rechazo no cambia por eso.
- **Sin datos personales:** ninguna de las dos lleva nombres, identificaciones ni horas (principio IX).

**Alternativas.**
- **Renombrar a `EXPORTACION_SIGA_REGISTRADA`:** deja dos acciones para lo mismo en la historia de la bitácora.
- **Registrar el rechazo con `Transaccion` y fallar si no se guarda:** cambiaría un 409 por un 500 por un problema de auditoría. El aviso de una anomalía no debe ocultar la anomalía.

## §6. La API (FR-010, FR-017, FR-018)

**`generarCsvSiga`** (`GET /api/reporting/periodos/{periodo}/csv-siga`) **cambia**:
- **Un encabezado nuevo:** `X-Suma-Verificacion`, el SHA-256 en hexadecimal (`^[0-9a-f]{64}$`).
- **El 409** suma el código `REPORTING_VERSION_NO_EXPORTABLE`, además de `REPORTING_SIN_PUBLICACION`.
- **La descripción** dice la validación, el registro y la entrada.

**`consultarExportacionesSiga`** (`GET /api/reporting/periodos/{periodo}/exportaciones-siga`) **es nueva**:
- **Permiso:** `PUBLICACION:APROBACION`.
- **Respuesta:** `ExportacionesSiga`, con `{periodo, exportaciones: [ExportacionSiga]}`, de la más reciente a la más antigua y `no-store`.
- **Un periodo sin exportaciones** da una lista vacía, no un error.

**Se mantiene GET** para el CSV: es la operación de F19-H01, y el registro es un efecto de auditoría, igual que la entrada de la bitácora de cada descarga (D-115). Un cambio a POST rompería el contrato sin una ventaja para el único cliente.

**El encabezado propio** se eligió en lugar de `Repr-Digest` (RFC 9530): ese usa base64 y nadie lo consume. La SPA lo lee porque la petición es del mismo origen (el API Gateway).

## §7. La pantalla (FR-011, FR-018 a FR-020, FR-019a)

**Decisión.** Se amplía «Exportación a SIGA» (`frontend/src/pantallas/siga/Siga.tsx`). La sección «Layout de exportación» no cambia.
- **El aviso** (FR-019a), fijo y antes del botón:
  - es una constante `AVISO_DEL_CSV_SIGA`, junto al `AVISO_DEL_CSV` de «Reportes»;
  - dice: «El archivo va a SIGA tal como sale. Si lo abre en una hoja de cálculo, importe la columna de la identificación como texto para no perder el cero inicial de la cédula, y no lo vuelva a guardar: un archivo guardado de nuevo deja de coincidir con su suma de verificación.»
- **Después de exportar:**
  - «Se generó siga-2026-I-v1.csv.», como en F19-H01;
  - debajo, «Suma de verificación (SHA-256):» y el valor en `<code>`, con un botón «Copiar la suma de verificación»;
  - la línea de FR-011: «Para comprobarla: en Windows, `certutil -hashfile siga-2026-I-v1.csv SHA256`; en Linux, `sha256sum siga-2026-I-v1.csv`.».
- **«Exportaciones del periodo»:**
  - una tabla con `caption` y las columnas «Fecha y hora», «Usuario», «Versión», «Layout», «Archivo» y «Suma de verificación»;
  - vacía: «Todavía no se exportó el CSV para SIGA de este periodo.»;
  - se invalida al exportar.
- **El rechazo:** el `titulo` y la `descripcion` del catálogo en el `Alert` que ya existe, como el 409 de F19-H01.

**Por qué.** `certutil` viene con Windows y, desde Windows 10, imprime el hexadecimal en minúsculas, sin espacios; en versiones anteriores lo separa con espacios, y la comparación sigue siendo a simple vista. `sha256sum` viene con cualquier Linux. La validación corre solo al exportar (aclaración del 2026-11-22).

## §8. El catálogo de errores (FR-005, FR-022)

`REPORTING_VERSION_NO_EXPORTABLE`:
- **Estado y clasificación:** 409, Negocio.
- **Título:** «Versión no exportable».
- **Mensaje:** el fijo del catálogo: «La versión publicada no tiene todas sus firmas o tiene inconsistencias abiertas, así que no se exporta. Avisa al Vicerrector académico.» Cada condición que falla va en un detalle, con sus números: `firmas` («Tiene 2 de 3 firmas.», «No tiene documento firmado.», «Su documento firmado es de otra versión.», «Sus firmas se anularon o se rechazó en firma.») e `inconsistencias` («Tiene 1 dura y 2 blandas sin justificar.», «No se registraron sus inconsistencias al congelarla.»). **Ajuste al implementar** (2026-11-28): `VerificadorDeCatalogo` exige que la descripción sea la del catálogo, así que no se puede armar; se sigue `VALIDACIONOCS_INCONSISTENCIAS_ABIERTAS`, que también es un 409 de negocio con detalles.
- **Acción:** «Avisar al Vicerrector académico: la versión publicada debe tener todas sus firmas y ninguna inconsistencia abierta.».
- **Causa:** `CondicionesDeExportacion` con motivos.
- **Descripción técnica:**
  - la excepción es `ExportacionRechazada`, de `GenerarCsvSigaServicio`, y la traduce `ErroresReporting`;
  - los motivos y los hashes van al log;
  - queda la entrada `EXPORTACION_SIGA_RECHAZADA`, y no se genera nada.

**Los códigos que se amplían:**
- `REPORTING_SIN_PUBLICACION` sigue igual.
- Los comunes 502 y 504 suman a Firma en su causa.
- Firma no suma códigos: su 404 interno es `COMUN_RECURSO_NO_ENCONTRADO`.

**Los detalles** siguen a Aprobación para las inconsistencias abiertas (`ErroresValidacionocs`, «N duras y M blandas»), el precedente de un 409 de negocio con detalles en el repositorio.

## §9. Permisos (FR-017)

- **Las dos operaciones públicas** exigen `PUBLICACION:APROBACION`, como las de F19-H01 (D-16, D-119).
- **La ruta de Firma** solo admite el rol `PROCESO`, por la regla `/interno/**` de `SeguridadProcesoAutoConfiguracion`.
- **La matriz** no cambia.

## §10. La demostración

Una versión publicada por el flujo normal siempre tiene sus firmas y 0 inconsistencias, así que el script prueba el camino feliz y los rechazos que sí se pueden provocar. Los rechazos por firmas e inconsistencias se prueban en el dominio, la aplicación, el adaptador y el contrato (§11).
- **R1 (amplía):** en EN_OCS, el 409 `REPORTING_SIN_PUBLICACION` no deja `EXPORTACION_SIGA_RECHAZADA` y la lista de exportaciones de 2026-I está vacía.
- **R9 (amplía):** con los dos CSV:
  - el encabezado `X-Suma-Verificacion` es el `sha256sum` de cada archivo y es el mismo en los dos (SC-002, SC-005);
  - `consultarExportacionesSiga` tiene 2 exportaciones con esa suma, la versión 1 y el layout;
  - las 2 entradas `CSV_SIGA_GENERADO` llevan la suma y el `id` de cada exportación (SC-003).
- **R4 (amplía):** `rectorado.demo`, `coordinacion.demo` y `admin.demo` reciben 403 en `consultarExportacionesSiga`.
- **R5 (amplía):** durante la reforma, el CSV tiene la misma suma que en R9 y hay una exportación más.
- **R11 (nuevo):** en la base, `update` y `delete` sobre `reporting.exportacion_siga` fallan, también como superusuario (SC-006). La ruta interna de Firma no se pide desde el script, que no tiene token de proceso: que R9 exporte ya prueba que Reporting la lee y que la cadena de la versión 1 está completa.

## §11. Las pruebas

- **Dominio de Reporting** (sin dobles, viéndolas fallar primero):
  - `CondicionesDeExportacionTest`: cada motivo, varios a la vez, las blandas justificadas que no cuentan (porque no llegan), el documento de otra versión, una cadena de un solo paso, y el contenido sin el registro.
  - `SumaDeVerificacionTest`: el vector conocido de SHA-256 de `abc`, el vacío y el formato.
  - `ExportacionSigaTest`: los datos y sus reglas.
- **Aplicación** (`GenerarCsvSigaServicioTest`, con Mockito en los puertos):
  - el rechazo no genera, no guarda la exportación y registra el rechazo;
  - si el registro del rechazo falla, sigue el 409;
  - `SinPublicacion` no registra nada;
  - el registro y la entrada van en la misma transacción, antes de entregar, y si fallan no hay archivo;
  - la suma es la de los bytes entregados;
  - la reforma valida la vigente;
  - los 300 docentes siguen en menos de 10 s (SC-007).
- **Adaptadores de Reporting:**
  - `ExportacionesSigaJdbcIntegrationTest` (PostgreSQL real): guardar, el orden, el periodo, y `update` y `delete` que fallan.
  - `ClientesDeReportingTest`: la ruta de Firma (200, 404 como vacío, 502 y 504) y las inconsistencias en el contenido.
  - `RegistroEnBitacoraEventosTest` y `RegistroEnBitacoraIntegrationTest` (`apps/salida`): las dos acciones, sin datos personales.
- **Firma** (dominio central, TDD):
  - `FirmasDelDocumentoTest`: el estado de cada paso para la ruta interna;
  - `DocumentoFirmableServicioTest`: por versión, y sin documento;
  - `InternoFirmaContratoTest`: la operación, el 404 y el 403 sin `PROCESO`.
- **Generación** (dominio central, TDD):
  - `GuardarContenidoCongeladoServicioTest`: las abiertas del panel si la versión es la vigente, y vacías si no lo es;
  - `ContenidoCongeladoIntegrationTest`: las dos columnas y el campo en la ruta;
  - `InternoGeneracionContratoTest`: el campo opcional.
- **Contrato:**
  - `SigaContratoTest`: el encabezado, el 409 con los dos códigos y la operación nueva con sus errores;
  - la prueba del catálogo: el código nuevo.
- **Arquitectura:** `ArquitecturaTest` y `ApplicationModules.verify()`. Reporting no usa clases de Firma ni de Generación.
- **SPA:** `Siga.test.tsx` (el aviso, la suma, copiar, la línea de las herramientas, la lista vacía y con datos, el 409 nuevo) y `siga.spec.ts` con axe-core.

## §12. El tamaño

S, 13 h (D-119). Es el resto de la feature F19.

| Parte | Horas |
|---|---|
| Firma, la ruta interna | 2 |
| Generación, las inconsistencias al congelar | 2 |
| Reporting, el dominio, el servicio, el registro y la API | 5 |
| La pantalla | 2 |
| El script, la evidencia y D-120 | 2 |

**Si no cabe,** se recorta primero el botón de copiar (la suma ya se puede seleccionar) y después R11, cuya regla ya cubren las pruebas de integración. La validación, la suma y el registro no se recortan.
