# Research: Rechazar en firma con una observación

## §1 Dónde vive el rechazo

**Decisión.** En Firma, como la operación `rechazarEnFirma()` del documento (glosario §5). Una tabla nueva, `firma.rechazo_en_firma` (V6), con a lo sumo un rechazo por documento: su clave es la de la generación (periodo, versión, tipo). Solo `insert` y `select` (principio VII). `FirmasDelDocumento` suma el rechazo: con él, `pasoEnTurno()` está vacío y el documento queda cerrado.

**Por qué.** El turno ya es de Firma (D-102), y el glosario pone el rechazo en Firma y el evento `RechazoEnFirma` hacia Aprobación (§3, §6). Un rechazo cerrado en la misma tabla de la generación hace que la regla «nadie en turno» salga sola de `FirmanteEnTurno`: la lista, la descarga y la subida la siguen sin cambios.

**Alternativas.** Una transición «rechazar» en Aprobación que pregunte el turno a Firma (diseño §9): Aprobación tendría que conocer al firmante esperado y Firma no sabría que el documento está cerrado. Una columna en `documento_firmable`: esa tabla no admite `update` (V2).

## §2 Concurrencia con la subida

**Decisión.** El registro del rechazo bloquea la generación (`FOR UPDATE`, como la subida en D-103) y comprueba que las firmas siguen siendo las que vio y que no hay rechazo. Si cambió algo, quien rechaza ya no está en turno: `SinTurnoDeFirma` (403). La subida aceptada, al registrar, comprueba además que no hay rechazo; si lo hay, se registra como rechazada por «mismo documento», igual que la subida que llega tarde (F14-H02). La clave primaria de V6 impide un segundo rechazo aunque se salte el bloqueo.

**Por qué.** Reutiliza el bloqueo y la regla de la subida tardía: nunca quedan la firma y el rechazo del mismo paso (FR-006), sin un estado nuevo.

**Después del rechazo**, una subida o un segundo rechazo encuentran el documento sin turno: el 403 común (`SEGURIDAD_ACCESO_NO_AUTORIZADO`, por `SinTurnoDeFirma`), del catálogo (FR-005). No hace falta un código propio: para quien llega, el documento no es suyo para firmar.

## §3 La observación

**Decisión.** De 10 a 500 caracteres después de recortar los extremos, como en D-98. Se valida después del turno: a quien no está en turno, el 403 sin mirar el cuerpo (como la subida no mira el archivo). Código nuevo `FIRMA_OBSERVACION_INVALIDA` (400, validación). El cuerpo que no cumple el contrato, `COMUN_SOLICITUD_INVALIDA`.

**Por qué.** La regla y el estado son los de `VALIDACIONOCS_OBSERVACION_INVALIDA`; el prefijo es del servicio que la detecta (D-72).

## §4 El aviso a Aprobación

**Decisión.** `RechazoEnFirma` (record en `puertos/entrada` de Firma, su API publicada) con periodo, versión, orden, rol, quién, cuándo y la observación. Un puerto de salida `AvisoDeRechazoEnFirma` y su adaptador con `ApplicationEventPublisher`, publicado en la misma transacción que el rechazo. El registro de publicaciones lo reenvía si Aprobación falla (ADR-018), como `CadenaDeFirmasCompleta` (D-104).

**Alternativa.** Ampliar `AvisoDeCadenaCompleta` con un segundo método: el nombre ya no diría lo que hace, y cambiarlo toca las pruebas de F14-H03 sin ganar nada.

## §5 La transición en Aprobación

**Decisión.**
- `CierreDeLaFirma` suma un segundo `@ApplicationModuleListener`, que llama al caso de uso `RegistrarRechazoEnFirma`.
- `ProcesoAprobacion.rechazarEnFirma(version, rechazo, ahora)`: si el distributivo está en CONGELADO_EN_FIRMA con esa versión congelada, pasa a la etapa activa anterior, que con la configuración por defecto es EN_REVISION_VRA (diseño §2, glosario). La transición es por `sistema`, de tipo nuevo `RECHAZO_EN_FIRMA`, con el hash de la congelación.
- Si el distributivo está en otra etapa o la versión no es la congelada, no hace nada y lo registra en el log: el aviso no se reintenta para siempre (FR-012). Repetido, tampoco hace nada (FR-011).
- El firmante, el paso y la observación van en una tabla nueva, `validacionocs.rechazo_en_firma` (V5), una fila por transición, solo de inserción. `Transicion` suma un `Optional<RechazoDelFirmante>`; `observaciones` sigue siendo de la devolución.
- La bitácora: `CambioDeFlujo.Accion.DISTRIBUTIVO_RECHAZADO_EN_FIRMA`, una sola entrada, con la observación (D-95).

**Por qué la etapa activa anterior.** EN_REVISION_VRA es opcional en ADR-007. Con la configuración de la demo es la de llegada; si una copia la desactiva, volver a una etapa inactiva rompería el flujo. Es la misma regla que `siguienteActiva`, al revés.

**Por qué una tabla propia.** La observación de la devolución es por carrera; la del rechazo es una, con el paso y el firmante, que no es quien registra la transición (`sistema`).

## §6 Aprobar otra vez

**Decisión.** `ProcesoAprobacion` exige que la última transición no sea un `RECHAZO_EN_FIRMA` antes de un paso que congela la versión: en `validarAprobacion` y en `validarRevision` cuando `congelaAlAvanzar()` (EN_REVISION_VRA desactivada). Si lo es, `RequiereVersionNueva` → `VALIDACIONOCS_REQUIERE_VERSION_NUEVA` (409, negocio). `acciones()` no ofrece APROBAR_CONTENIDO en ese estado; la pantalla muestra el rechazo y que hace falta una versión nueva.

**Por qué.** Congelar otra vez la misma versión pediría a Firma el mismo documento, que es idempotente y devolvería el rechazado (D-101). F12-H04 agregará la versión nueva, cuya transición deja de ser la última y desbloquea.

## §7 El estado del flujo

**Decisión.** `EstadoDelFlujo` suma `rechazoEnFirma` (orden, rol, por, en, observación), presente mientras la última transición es un rechazo en firma. `TransicionDelFlujo.tipo` suma RECHAZO_EN_FIRMA. La SPA lo muestra en `EstadoDelFlujo.tsx`, que ya usan «Consolidado del periodo» y «Oferta del periodo», como la devolución de F12-H05.

## §8 La pantalla de pendientes

**Decisión.** «Rechazar» en cada fila de `FirmasPendientes.tsx`, junto a «Subir el PDF firmado». Abre un diálogo con la observación (contador de 10 a 500), «Cancelar» y «Rechazar en firma». Con 201, aviso «Rechazo registrado» y la lista se vuelve a pedir. Con 400, el mensaje del catálogo junto al campo. Playwright con axe-core, como la subida.

## §9 La prueba de punta a punta

**Decisión.** Un script nuevo, `deploy/probar_rechazo_en_firma.sh`, en el workflow después de `probar_documento_firmable.sh`. Prepara el periodo como ese script (pasos 0 y 1), firma F1 con la herramienta de prueba, y como `vicerrectorado.demo` (F2 en turno) prueba el rechazo sin observación y corto (400), fuera de turno (403), y con observación (201). Después comprueba EN_REVISION_VRA por `sistema` en menos de 30 s, la observación en el estado del flujo, la subida y el segundo rechazo con 403, aprobar otra vez con 409, la base y la bitácora. Limpia lo que creó.

**Por qué otro script.** Después del rechazo el periodo no puede volver a firma hasta F12-H04; en el mismo script que completa la cadena no caben los dos caminos.
