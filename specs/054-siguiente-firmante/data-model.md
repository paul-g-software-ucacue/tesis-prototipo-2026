# Data Model: Notificar al siguiente firmante y ver la cadena de firmas

F16-H02 (#68). No hay tablas ni migraciones nuevas: lo que se notifica y lo que se muestra sale de lo que Firma ya guarda (F13-H02 a F14-H04), y la notificación es la de F16-H01.

## Firma

### `FirmasDelDocumento` (dominio, existente)

Ya tiene:
- la cadena, con el orden, la etiqueta y el rol de cada paso (`PasoDeFirma`);
- el firmante esperado de cada paso (`FirmanteEsperado`), con su nombre impreso si se guardó al generar;
- las firmas aceptadas (`FirmaRegistrada`), con el orden, quién la subió y cuándo la aceptó el MDD (`subidoEn`);
- el rechazo (`RechazoDelFirmante`), con el orden del paso rechazado;
- la anulación de sus firmas, si la hubo (F12-H04).

Suma dos consultas, sin estado nuevo:

| Consulta | Devuelve | Regla |
|---|---|---|
| `cadenaConEstado()` | Un `PasoConEstado` por paso de la cadena, en su orden | Ver la tabla de estados |
| `siguienteTrasFirmar(orden)` | El paso siguiente y su firmante esperado, si existe | Vacío si `orden` es el último paso o si el siguiente no tiene firmante esperado |

**Los estados de un paso:**

| Estado | Cuándo |
|---|---|
| `FIRMADO` | El paso tiene firma aceptada. Lleva `firmadoEn` = `subidoEn` de esa firma. |
| `RECHAZADO` | El documento tiene un rechazo en firma, y es el paso rechazado. |
| `EN_TURNO` | Es el paso de `pasoEnTurno()`: el primero sin firma, si el documento no está rechazado ni anulado y tiene firmantes esperados. |
| `PENDIENTE` | Todos los demás. |

Reglas:
- A lo sumo un paso está `EN_TURNO` (FR-012). Con la cadena completa, un rechazo o sin firmantes esperados, ninguno.
- Un paso después del rechazado queda `PENDIENTE`; los firmados antes del rechazo siguen `FIRMADO` (US2, escenario 5).
- `cadenaConEstado()` y `pasoEnTurno()` usan la misma regla del turno: la cadena y «Pendientes de firma» no se contradicen.

### `PasoConEstado` (dominio, nuevo)

| Campo | Tipo | Regla |
|---|---|---|
| `orden` | int | Desde 1 |
| `rotulo` | String | La etiqueta seguida de «por», como en el PDF y en `PasoEnTurno.rotulo()` |
| `firmante` | Optional&lt;String&gt; | El nombre impreso del firmante esperado. Vacío en un documento generado antes de D-104 |
| `estado` | enum | `FIRMADO`, `EN_TURNO`, `PENDIENTE` o `RECHAZADO` |
| `firmadoEn` | Optional&lt;Instant&gt; | Solo con `FIRMADO` |

No lleva la identificación, el usuario ni el certificado (FR-016).

### `EstadoDelDocumento` (puerto de entrada, existente)

Suma `cadena`, una lista de `PasoConEstado`:
- con `GENERADO` y sin anulación, la cadena con su estado;
- en cualquier otro caso, vacía.

### `FirmaPendienteSolicitada` (puerto de salida, nuevo)

Lo que `SubirFirmaServicio` le pasa a `NotificacionDeFirmaPendiente` en la transacción de la firma aceptada:

| Campo | Origen |
|---|---|
| `periodo`, `version` | El documento |
| `autor` | Quien subió la firma aceptada |
| `momento` | `subidoEn` de la firma aceptada |
| `rotulo` | El rótulo del paso siguiente |
| `firmante` | El firmante esperado del paso siguiente: su usuario o su identificación |
| `rotuloAnterior`, `nombreAnterior` | El rótulo y el nombre impreso del paso recién firmado |

Se pide solo si la firma se aceptó y el paso firmado no es el último. El siguiente nunca es F1, así que el firmante de F1 no recibe esta notificación (FR-006).

## Notificaciones

### `DestinoDelMensaje` (dominio, existente → cambia)

Pasa a ser uno de tres tipos:

| Tipo | Campos | Quiénes |
|---|---|---|
| `PorRol` | `rol`, `carrera?` | Los de F16-H01, sin cambios |
| `PorUsuario` | `usuario` | Esa cuenta, si existe y está activa |
| `PorIdentificacion` | `identificacion` | La cuenta activa con esa identificación, si existe |

Reglas de `SolicitudDeNotificaciones.de(Map)`:
- El destino tiene exactamente una de las claves `rol`, `usuario` o `identificacion`. `carrera` solo junto con `rol`.
- `usuario`: texto no vacío de hasta 64 caracteres, el largo de `nombre_usuario` en Usuarios. Sin patrón: Usuarios no lo define, y el `autor` tampoco lo tiene.
- `identificacion`: `^[0-9A-Z]{1,13}$`, el de Usuarios V7 y `FirmanteEsperado`.
- Un error de forma no repite el valor recibido (research §4).

### `RecibirNotificacionesServicio` (aplicación, existente → cambia)

- `ORIGENES` suma `firma`: el tipo `firma.NotificacionesSolicitadas`, con origen `firma`, se acepta.
- Los destinatarios de cada tipo se resuelven con `Destinatarios.de(destino, periodo)`. Para una persona:
  - si no existe, no está activa o no tiene cuenta, nadie (FR-002 d);
  - el autor se sigue quitando.
- La notificación guardada no cambia de forma: `destinatario` es el usuario, y `carrera` queda vacía. La clave única por evento, destinatario y carrera sigue sirviendo.

## Usuarios

### `ConsultarCuentaPorIdentificacion` (puerto de entrada, nuevo)

| Método | Devuelve |
|---|---|
| `de(String identificacion)` | `Optional<ResumenDeUsuario>`: el usuario, su nombre y si está activo |

- La identificación es única entre las cuentas (Usuarios V7, índice `usuario_identificacion`).
- No devuelve la identificación, la contraseña ni su hash.
- Solo lee.

## SPA

### `EstadoDelDocumento.cadena` (tipos generados del contrato de Firma)

La SPA la recibe tal como la define `contratos/firma/openapi.yaml` ([contracts/firma.md](contracts/firma.md)) y la muestra como se dice en [contracts/spa.md](contracts/spa.md).
