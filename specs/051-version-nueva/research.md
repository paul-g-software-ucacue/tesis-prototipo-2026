# Research: Crear una versión nueva y anular las firmas tras un rechazo en firma

Cada sección: decisión, por qué y alternativas. Todo parte del código de F13-H01, F14-H03 y F14-H04.

## §1. Quién crea la versión nueva y en qué orden

**Decisión.** El oyente del rechazo de F14-H04 (`CierreDeLaFirma.alRechazarseEnFirma` → `RegistrarRechazoEnFirmaServicio`) hace tres pasos, como la aprobación de D-100:
1. **Lee el proceso** sin bloquearlo. Si no está en CONGELADO_EN_FIRMA con la versión del aviso, no hace nada (FR-013): ni Generación ni Firma se enteran.
2. **Ordena a Generación** crear la versión nueva (`ParcialesDelDistributivo.crearVersionNueva`), en la transacción de Generación, de forma idempotente (§2).
3. **Bloquea el proceso** y, en una transacción, vuelve a comprobar y registra dos transiciones: `RECHAZO_EN_FIRMA` (la de F14-H04) y `VERSION_NUEVA` (§4). En la misma transacción guarda el aviso para Firma (§5) y los cambios para la bitácora.

Si el paso 3 falla, el registro de publicaciones reenvía el aviso (ADR-018): el paso 2 devuelve la versión ya creada y el paso 3 se completa. Un aviso repetido después del paso 3 encuentra el proceso en EN_REVISION_VRA y no hace nada.

**Por qué.** Es el patrón de aprobar (D-100): Generación cumple la orden en su transacción y Aprobación registra el paso con el proceso bloqueado. Cada servicio guarda en su esquema y en su transacción (principio V).

**Alternativas.**
- Una transacción que abarque Generación y Aprobación: funcionaría en un proceso, pero ataría los dos esquemas en una transacción, y D-100 ya eligió no hacerlo.
- Que Generación escuche `RechazoEnFirma`: Generación decidiría por su cuenta, contra MD-04 (lo ordena Aprobación).

## §2. Cómo crea Generación la versión nueva

**Decisión.** `crearVersionNueva(periodo, versionAnulada, motivo)` en `ParcialesDelDistributivo`, con la versión vigente bloqueada (`bloquearVigente`):
- **Vigente = la del aviso y CONGELADA:** la pasa a ANULADA; inserta la siguiente, EDITABLE, con el motivo `RECHAZO_EN_FIRMA`, `sistema` y el momento; copia, con `INSERT … SELECT` y ids nuevos, los parciales con su estado, las propuestas, las actividades, las justificaciones, las consolidaciones y los informes.
- **Vigente = la siguiente, creada por ese motivo, y la del aviso ANULADA:** devuelve la que ya existe (idempotente).
- **Otro caso:** vacío, y Aprobación lo registra sin reintentar.

Devuelve el número y el hash de la versión nueva (el mismo de `hashDelDistributivo`). Como D-95, Generación no anota el cambio en su registro: lo registra Aprobación.

**La consolidación copiada sigue al día.** «Desactualizado» compara la última consolidación con el último cambio de propuestas o actividades del periodo en el registro de Generación. La copia conserva el momento de la consolidación y no agrega cambios, así que el consolidado de la versión nueva no queda desactualizado.

**El informe sustentado.** Su `clave` (el archivo en el almacén) es única. La copia comparte el archivo, que nunca se reescribe ni se borra: V8 cambia el índice único a `(version_id, clave)`.

**Por qué.** La versión es el agregado de MD-01: copiarla entera en una transacción, con la versión bloqueada (D-83), la deja igual a la anulada sin un estado intermedio. En EN_REVISION_VRA el consolidado no admite cambios (D-99), así que nadie edita mientras se copia.

**Alternativas.**
- Copiar los bytes del informe a un archivo nuevo: duplica el archivo y escribe fuera de la transacción.
- Copiar solo la última consolidación y el último informe: se perdería su historia en la versión nueva sin ganar nada.
- Que la versión nueva apunte a las filas de la anulada: rompería «una versión congelada no cambia» en cuanto se edite la nueva.

## §3. Los datos de la versión

**Decisión.** Generación V8: `version_distributivo` suma `motivo` (`RECHAZO_EN_FIRMA`; nulo solo en la versión 1), `creada_por` y `creada_en` (nulos en las versiones que ya existían), con el `check (numero = 1) = (motivo is null)`. `MotivoDeVersion`, con un solo valor, va en `puertos/entrada` de Generación, su interfaz publicada: Aprobación lo usa al ordenar, y no puede depender del dominio de otro servicio (principio V). El repositorio guarda su nombre. Los otros motivos del glosario (ajuste, devolución, reforma) los agregan sus historias.

**El glosario.** Sus motivos son «ajuste, devolución o reforma»; el autor eligió el nombre «rechazo en firma» y lo suma al glosario antes de implementar (principio III).

**Por qué.** Es la tabla `distributivo_version` del diseño de firmas (motivo, creada_por, creada_en).

## §4. La transición `VERSION_NUEVA`

**Decisión.** Aprobación V6 suma el tipo `VERSION_NUEVA`: de EN_REVISION_VRA (la etapa a la que volvió) a la misma, con la versión nueva, su hash, `sistema` y el motivo en el detalle. Va justo después de `RECHAZO_EN_FIRMA`. En la bitácora, `DISTRIBUTIVO_VERSION_NUEVA`, con la versión anulada, la nueva y el motivo.

Con ella:
- `rechazoPendiente()` (la última transición es un rechazo) queda vacío, así que aprobar vuelve a funcionar y `acciones()` vuelve a ofrecer APROBAR_CONTENIDO, sin cambiar la regla de D-105. El JavaDoc de D-105 ya lo anticipaba: «La versión nueva agregará otra transición y lo dejará atrás».
- `rechazoEnFirma` del estado del flujo pasa a ser el rechazo **de la versión vigente**: el último `RECHAZO_EN_FIRMA` sin una congelación después. Se ve mientras el Vicerrector trabaja en la versión nueva y desaparece al aprobarla.

**Por qué.** Registrar la versión nueva como transición deja en el flujo y en la bitácora quién y cuándo la creó (RF-34) y levanta el bloqueo con la regla que ya existe.

**Alternativas.**
- Quitar el bloqueo de D-105: un rechazo registrado sin versión nueva, por un fallo a medias, dejaría aprobar la versión anulada.
- Guardar la versión nueva solo en Generación: la bitácora no tendría la entrada y el flujo seguiría viendo el rechazo como pendiente.

## §5. Cómo ordena Aprobación la anulación a Firma

**Decisión.** Como la preparación del documento (D-101): `AvisoDeFirma` suma `avisar(FirmasPorAnular)` (periodo, versión, motivo, momento), guardado en la transacción del paso 3 de §1. El oyente nuevo `AnulacionDeLasFirmas` (adaptador de salida, `@ApplicationModuleListener`) llama a la API publicada de Firma, `AnularFirmas.anular(...)`, después de confirmar. El motivo es `MotivoDeAnulacion` (`RECHAZO_EN_FIRMA`), un enum de `puertos/entrada` de Firma, el mismo valor del contrato.

**Por qué.** Firma no conoce el flujo (MD-05) y la dependencia sigue de Aprobación hacia Firma. El aviso se guarda con la transición: si la transición no confirma, no se anula nada; si Firma falla, el registro lo reintenta (FR-012).

**Alternativas.**
- Llamar a Firma dentro de la transacción de Aprobación: ataría los dos esquemas, como en §1.
- Que Firma anule sola al registrar el rechazo: el autor eligió que lo ordene Aprobación; sirve igual para la devolución futura desde EN_OCS.

## §6. Cómo anula Firma las firmas

**Decisión.** Firma V7, `anulacion_de_firmas`: una fila por documento (`periodo, version, tipo`, clave foránea a `generacion_documento`), con el motivo, cuántas firmas aceptadas tenía, `sistema` y el momento. Solo de inserción (`revoke update, delete, truncate`). `AnularFirmasServicio`:
- bloquea la generación, como la subida y el rechazo (D-103, D-105);
- sin documento generado para esa versión, no hace nada y lo registra;
- con la anulación ya registrada, no hace nada (idempotente, FR-009);
- si no, inserta la fila, anota `FIRMAS_ANULADAS` en la bitácora y publica `FirmasAnuladas` (periodo, versión, cuántas, motivo, momento) en la API publicada de Firma.

Las filas de `validacion_firma` no cambian: «anulada» se deduce de la anulación del documento (glosario: «Marcar como anuladas… Se conservan en el historial»).

**Con la anulación:**
- `FirmasDelDocumento` suma `Optional<AnulacionDeFirmas>`: con ella nadie está en turno;
- `registrarValidacion` y `registrarRechazo` comprueban también que no hay anulación, como ya lo hacen con el rechazo.

Con el rechazo de F14-H04 el documento ya está cerrado. Esto vale para la devolución futura sin rechazo.

**El estado del documento** (`consultarDocumentoFirmable`) suma `anulacion` (cuándo, motivo, cuántas firmas). La consulta toma la última versión del periodo, así que muestra la anulada hasta que se genere el documento de la versión nueva.

**`FirmasAnuladas` no tiene oyente todavía:** lo escuchará F16 (notificaciones). Está en el glosario y lo pidió el autor.

**Alternativas.**
- Una columna `anulada` en `validacion_firma`: exige `update` en una tabla solo de inserción (principio VII).
- Una fila de anulación por firma: más filas para decir lo mismo; el documento se anula entero.

## §7. Errores del catálogo

**Decisión.** Ninguno nuevo. Crear la versión y anular las firmas no son operaciones de la API (FR-017). Los casos que no corresponden se registran y no se reintentan. `VALIDACIONOCS_REQUIERE_VERSION_NUEVA` se conserva.

## §8. La SPA

**Decisión.**
- **El estado del flujo:** `RechazoEnFirmaDelFlujo` suma `versionAnulada` y `versionNueva`. El aviso «Rechazado en firma» dice «La versión 1 quedó anulada con sus firmas; el distributivo sigue con la versión 2.» en lugar de «Para aprobar otra vez hace falta una versión nueva.», que queda solo sin versión nueva.
- **«Aprobar el contenido»** vuelve por `acciones`, sin cambios en la pantalla.
- **«Consolidado del periodo»** ya muestra el número de la versión vigente. Con la versión nueva EDITABLE ya no está congelada: sale el aviso de revisión de #215 en lugar del de la versión congelada.
- **La rama de «sigue congelada»** de `AvisoDeVersionCongelada`, de F14-H04, ya no se alcanza: el rechazo y la versión nueva llegan juntos. Se quita, con sus dos pruebas, para no dejar código muerto.

**Por qué.** Lo pide FR-015 con lo que la pantalla ya tiene. El historial de versiones es de RF-42.

## §9. La prueba de punta a punta

**Decisión.** `probar_rechazo_en_firma.sh` cambia, porque su paso 8 esperaba el 409:
- **El paso 6** comprueba además la versión 1 ANULADA, la 2 EDITABLE con las mismas asignaciones, la transición `VERSION_NUEVA` y la anulación en Firma, en menos de 30 s (SC-001).
- **El paso 8** aprueba la versión 2. Comprueba:
  - CONGELADO_EN_FIRMA con la versión 2 y el documento nuevo generado;
  - `docente.demo` con F1 pendiente de la versión 2 y sin la 1 (SC-004).
- **El paso 9** suma la bitácora de la versión nueva y de la anulación.

`MDD_CONSERVAR=1` deja el estado después del rechazo, para el recorrido a mano.

**Por qué.** El script ya prepara el rechazo; uno nuevo repetiría toda la preparación.
