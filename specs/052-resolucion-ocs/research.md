# Research: Registrar la resolución del OCS

Cada sección: decisión, por qué y alternativas. Todo parte del código de F12-H02 a F12-H05, F13-H01, F14-H03 y F12-H04.

## §1. La operación y su transición

**Decisión.** `POST /api/validacionocs/periodos/{periodo}/resolucion` (multipart: `numero`, `fecha`, `archivo`), con `x-mdd-permiso: RESOLUCION_OCS:ESCRITURA` (D-16). `RegistrarResolucionServicio`:
1. comprueba el PDF (§3) y lo guarda en el almacén (§4), fuera de la transacción;
2. bloquea el proceso; `ProcesoAprobacion.registrarResolucion(quien, resolucion, ahora)` exige el rol de EN_OCS de la copia de la configuración, la etapa EN_OCS y la fecha en rango (§2), y agrega un `PASO_DE_ETAPA` de EN_OCS a APROBADO por quien registra, con la versión y el hash de la congelación y la resolución;
3. guarda la transición con su resolución y la entrada `RESOLUCION_REGISTRADA` para la bitácora, en la misma transacción.

Responde 200 con el estado del flujo, como aprobar el contenido.

**Por qué.** Es la operación `registrarResolucion()` del glosario sobre `ProcesoAprobacion`, y el autor decidió que registrar es aprobar. El bloqueo del proceso resuelve dos registros a la vez: el segundo encuentra el proceso en APROBADO (FR-010).

**Alternativas.** Registrar y aprobar en dos pasos: el autor lo descartó. Un endpoint genérico de avance desde EN_OCS con un adjunto: mezcla un multipart con el avance sin cuerpo.

## §2. La fecha

**Decisión.** `fecha` es una fecha sin hora. El agregado la compara con el día de la transición a EN_OCS y con el día de `ahora`, los dos en `America/Guayaquil` (la zona de la institución). Fuera de rango: `ResolucionFueraDeRango` → `VALIDACIONOCS_FECHA_DE_RESOLUCION_FUERA_DE_RANGO` (400), con el rango en la descripción.

**Por qué.** La resolución aprueba la versión ya firmada (decisión del autor). Comparar días, no instantes, evita que una resolución de hoy se rechace por la hora.

## §3. El PDF

**Decisión.** Como el informe sustentado de Generación (F10-H02): empieza con `%PDF-`, termina con `%%EOF` en el último KB y pesa hasta 10 MB. Se calcula su SHA-256. No se abre ni se validan sus firmas (D-19). Validación OCS tiene su propia clase (`ArchivoDeResolucion`), no la de Generación (principio V).
- No es un PDF: `VALIDACIONOCS_RESOLUCION_NO_ES_PDF` (400).
- Pasa de 10 MB: `VALIDACIONOCS_RESOLUCION_DEMASIADO_GRANDE` (413). El límite del proceso, 11 MB, corta antes lo que es mucho más grande, con el 413 común.

## §4. Dónde se guarda el archivo

**Decisión.** En el volumen de archivos del proceso (`mdd.archivos.raiz`, D-88), en `resoluciones/<sha256>.pdf`, como Firma guarda sus documentos por su hash. Se escribe a un temporal y se mueve; si ya existe, no se reescribe. Validación OCS suma el puerto `AlmacenDeResoluciones` y su adaptador en disco.

**Por qué.** El nombre por hash hace la escritura idempotente: un reintento no duplica, y un archivo cuyo registro falló queda sin referencia y no se ofrece en ninguna parte (edge case).

**Alternativas.** Guardarlo en Firma como documento `RESOLUCION_OCS` (lo descartó el autor). Guardarlo en la base: el resto de los archivos van al volumen.

## §5. Los datos

**Decisión.** Validación OCS V7, `resolucion_ocs`: `transicion_id` (clave primaria, foránea a `transicion`, `on delete cascade` como `rechazo_en_firma`), `periodo` y `version` (únicos juntos: una resolución por versión aprobada), `numero` (`varchar(60)`, recortado, de 1 a 60), `fecha`, `sha256`, `nombre` y `tamano` del archivo. Solo de inserción por el código, como las demás tablas de Validación OCS (D-105). `Transicion` suma `Optional<ResolucionOcs>`, presente solo en el paso de EN_OCS a APROBADO, y siempre en él.

**Por qué.** La resolución es una entidad de `ProcesoAprobacion` (glosario) y va con su transición, como el rechazo en firma. La unicidad por versión deja lugar a la reforma (F18-H02), que aprobará otra versión con otra resolución.

## §6. «Pasar a APROBADO sin la resolución»

**Decisión.** El único otro camino hacia APROBADO sería el avance genérico (`POST …/avance`), que hoy responde `VALIDACIONOCS_ETAPA_CON_OPERACION_PROPIA` en EN_OCS. Desde EN_OCS responde ahora `VALIDACIONOCS_REQUIERE_RESOLUCION` (409, «Para aprobar el distributivo registra la resolución del OCS»): es el escenario «Sin resolución» del issue. Registrar fuera de EN_OCS: `VALIDACIONOCS_ETAPA_NO_ADMITE_RESOLUCION` (409).

## §7. La consulta y la descarga

**Decisión.**
- `EstadoDelFlujo` suma `resolucion` (`ResolucionDelOcs`: número, fecha, registrada por, registrada en, `sha256`, nombre y tamaño), presente desde que se registra.
- `AccionDelProceso` suma `REGISTRAR_RESOLUCION`, para el rol de EN_OCS con el distributivo en EN_OCS.
- `GET /api/validacionocs/periodos/{periodo}/resolucion/archivo` devuelve el PDF (`application/pdf`, `Content-Disposition` con el nombre guardado), con el mismo permiso que el estado del flujo.
  - Sin resolución: `VALIDACIONOCS_RESOLUCION_NO_ENCONTRADA` (404).
  - El archivo falta o no coincide con su huella: `VALIDACIONOCS_RESOLUCION_ILEGIBLE` (500), como el informe.

## §8. El evento `ResolucionRegistrada`

**Decisión.** No se publica un evento propio todavía: la bitácora recibe su cambio `RESOLUCION_REGISTRADA` y nadie más escucha la aprobación. Lo agregará la historia que lo necesite (publicar, F18-H01, o las notificaciones, F16), con el nombre del glosario.

**Por qué.** Principio X: un evento sin consumidor es una abstracción especulativa. `FirmasAnuladas` sí se publicó porque lo pidió el autor.

## §9. La SPA

**Decisión.** En el estado del flujo (`EstadoDelFlujo.tsx`, que usa «Consolidado del periodo»):
- **Con `REGISTRAR_RESOLUCION`:** un formulario «Registrar la resolución del OCS» con «Número», «Fecha» y «PDF de la resolución», y una confirmación que dice que el distributivo quedará aprobado.
- **Errores:** los 400 y el 413 se muestran en el formulario.
- **Después:** con `resolucion`, una constancia «Resolución del OCS n.º …» con la fecha, quién, cuándo, la huella copiable y «Descargar la resolución».

## §10. La prueba de punta a punta

**Decisión.** `probar_documento_firmable.sh` ya termina en EN_OCS (paso 12): suma los pasos de la resolución.
- **13.** El avance genérico da 409 `VALIDACIONOCS_REQUIERE_RESOLUCION`, y otros roles reciben 403 al registrar.
- **14.** Los registros inválidos (número vacío, fecha futura y anterior a EN_OCS, un archivo que no es PDF y uno de más de 10 MB) se rechazan sin dejar fila.
- **15.** `secretaria.demo` registra, el distributivo queda en APROBADO, el PDF se descarga idéntico y la bitácora tiene `RESOLUCION_REGISTRADA`.

La limpieza borra también `resoluciones/` del volumen (ya vacía el almacén entero).

**Por qué.** Un script nuevo repetiría la preparación y las tres firmas, unos 70 s más en el workflow.
