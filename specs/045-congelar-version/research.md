# Research: Aprobar el contenido y congelar la versión

## §1. Quién congela: Generación, por orden de Aprobación

**Decisión.** La versión es de Generación y la etapa es de Aprobación (MD-04). Aprobar son dos transacciones, como enviar un parcial (F12-H02) o devolver (F12-H05):

1. **Sin cambiar nada:** con el proceso leído, `ProcesoAprobacion.validarAprobacion(quien)` exige el rol de EN_REVISION_VRA en la copia de la configuración y el distributivo en EN_REVISION_VRA. Un intento que no cabe no toca Generación.
2. **Generación** congela la versión vigente en su transacción, con la versión bloqueada (`bloquearVigente`, `FOR UPDATE`), y devuelve el número y el hash del distributivo. Es idempotente: una versión ya congelada devuelve el mismo hash sin cambiar nada.
3. **Aprobación**, con el proceso bloqueado (`FOR UPDATE`), vuelve a comprobar, registra la transición `PASO_DE_ETAPA` de EN_REVISION_VRA a la siguiente activa (CONGELADO_EN_FIRMA) con la versión y el hash, y la bitácora recibe una entrada (D-95).

Si la tercera falla, la versión queda congelada y el distributivo sigue en EN_REVISION_VRA. Volver a aprobar la completa, porque el paso 2 es idempotente (FR-002). Generación no registra el cambio de estado de la versión en su registro de cambios: lo registra Aprobación (D-95).

**Alternativas.**
- *Congelar en Aprobación con una copia del contenido:* duplica datos de Generación (principio V).
- *Una sola transacción para los dos esquemas:* hoy están en el mismo proceso, pero el diseño los separa por servicio. Las historias anteriores ya usan dos transacciones con un paso idempotente.

## §2. El hash de contenido

**Decisión.** Es `hashDelDistributivo`: el SHA-256 de los hashes de todos los parciales, el mismo que llevan las transiciones de envío, envío a revisión y devolución (F12-H02 research §4). Como nada cambia desde EN_REVISION_VRA (D-99) y los parciales están enviados, es igual al hash del envío a revisión (SC-002).

**Alternativa.** *Un hash que también incluya la consolidación, las justificaciones y el informe.* El informe ya tiene su huella en la entrada `CONSOLIDADO_ENVIADO_A_REVISION`. El documento que se firma es el PDF/A de F13-H02, que tendrá su propio hash. Cambiar el hash ahora rompería la comparación con las transiciones anteriores.

## §3. Si EN_REVISION_VRA está desactivada

`EN_REVISION_VRA` no es del esqueleto fijo; `CONGELADO_EN_FIRMA` sí. Con la revisión desactivada, el envío a revisión pasa de EN_CONSOLIDACION directo a CONGELADO_EN_FIRMA.

**Decisión.** La regla es de la etapa de llegada: **todo paso hacia CONGELADO_EN_FIRMA congela antes la versión**. Cuando la siguiente activa es CONGELADO_EN_FIRMA, `enviarARevision` pide a Generación que congele, con el mismo paso idempotente de §1. Lo mismo hace `aprobarContenido`. El dominio expone `ProcesoAprobacion.congelaAlAvanzar()`: la siguiente activa es CONGELADO_EN_FIRMA.

**Alternativa.** *Dejarlo fuera:* habría distributivos en firma con la versión editable, contra RF-59.

## §4. La regla de la versión congelada en Generación

Hoy `VersionDistributivo.exigirEditable(carrera)` rechaza con `ParcialNoEditable` (`GENERACION_PARCIAL_NO_EDITABLE`) una versión que no está EDITABLE o un parcial enviado. Proponer docentes y asignar actividades ya lo usan.

**Decisión.**
- **Error propio.** Una versión CONGELADA se rechaza primero, con `VersionCongelada` (`GENERACION_VERSION_CONGELADA`, 409): «La versión N ya se aprobó; un cambio exige una versión nueva». Se comprueba antes que el estado del parcial, porque es la causa más fuerte.
- **Las tres operaciones del consolidado.** Consolidar, justificar y adjuntar el informe comprueban la versión vigente dentro de su transacción, antes que `EnRevision` (D-99). Con la versión congelada, el error es `GENERACION_VERSION_CONGELADA` y no `GENERACION_CONSOLIDADO_EN_REVISION`.
- **Una regla de la versión, no de la etapa.** Vale aunque una historia futura devuelva el distributivo sin crear una versión nueva. F12-H04 y F18-H02 crearán la versión nueva.

**Alternativa.** *Reusar `GENERACION_PARCIAL_NO_EDITABLE`:* no dice por qué ni qué hacer.

## §5. La constancia en pantalla

**Decisión.**
- **Flujo.** La respuesta del flujo (`EstadoDelFlujo` de Validación OCS) agrega `congelacion`, opcional, con versión, hash, quién y cuándo. Sale de la última transición `PASO_DE_ETAPA` hacia CONGELADO_EN_FIRMA, sea la aprobación o el envío de §3.
- **Consolidado y panel.** Las respuestas de Generación agregan `versionCongelada` (booleano, requerido) para el aviso. La SPA muestra el aviso de versión congelada en lugar del de revisión (#215).
- **Bitácora.** Una transición `PASO_DE_ETAPA` desde EN_REVISION_VRA lleva su propia acción en la bitácora, `CONTENIDO_APROBADO`, como `CONSOLIDADO_ENVIADO_A_REVISION` desde EN_CONSOLIDACION.

No hace falta migración: la transición usa el tipo `PASO_DE_ETAPA`, que ya existe, y la columna `estado` de la versión ya admite `CONGELADA` (V3 de Generación).

**Alternativa.** *Un tipo de transición nuevo, `CONGELACION`:* exige migración y no aporta nada que la etapa de llegada no diga.

## §6. Permiso y operación

- **La operación.** `POST /api/validacionocs/periodos/{periodo}/aprobacion`, `operationId: aprobarContenido`, con permiso `DISTRIBUTIVO:APROBACION`. Lo tienen VICERRECTOR y RECTOR (matriz D-16); no hace falta migración de Roles.
- **El rol de la etapa.** Lo exige el dominio con la copia de la configuración (`RolDeOtraEtapa`, que el controlador traduce a 403, como en enviar y devolver): el Rector tiene el permiso, pero no es el rol de EN_REVISION_VRA.
- **La acción.** Nueva acción del proceso, `APROBAR_CONTENIDO`, para quien tiene el rol de EN_REVISION_VRA cuando el distributivo está en ella.
- **El error de etapa.** `VALIDACIONOCS_ETAPA_NO_ADMITE_APROBACION` (409) si el distributivo no está en EN_REVISION_VRA.

## §7. Concurrencia

- **Dos aprobaciones a la vez.** Las dos pueden congelar en Generación; es idempotente, y la versión bloqueada las ordena. En Aprobación, la segunda espera el bloqueo del proceso y luego `validarAprobacion` la rechaza: el distributivo ya no está en EN_REVISION_VRA. Queda una transición y una entrada (SC-004).
- **Una edición durante la aprobación.** Proponer y asignar actividades toman `bloquearVigente`, igual que congelar: la que llega después ve la versión congelada. Consolidar, justificar y el informe ya están rechazados desde EN_REVISION_VRA (D-99).

## §8. La prueba de punta a punta

`deploy/probar_aprobacion.sh` parte de `probar_consolidado_en_revision.sh`:
- arma el consolidado listo y lo envía a revisión;
- `vicerrectorado.demo` aprueba;
- comprueba la etapa, la versión congelada, el hash igual al del envío, la bitácora (una entrada), el rechazo de una segunda aprobación, los 409 de las cinco ediciones sin rastro, y que se puede consultar y descargar.

Su paso va en `perfil-consolidado`, después de «El consolidado en revisión».
