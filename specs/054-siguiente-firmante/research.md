# Research: Notificar al siguiente firmante y ver la cadena de firmas

F16-H02 (#68). Cada sección resuelve una pregunta del plan con su decisión, el porqué y lo que se descartó.

## §1. Quién publica la notificación de firma pendiente

- **Decision:** Firma la publica, en la misma transacción en que `SubirFirmaServicio` registra la firma aceptada, por un puerto de salida nuevo, `NotificacionDeFirmaPendiente`.
  - **El adaptador** `PublicadorDeFirmaPendiente` publica el evento `firma.NotificacionesSolicitadas` hacia `notificaciones` (ADR-018), como `PublicadorDeCambios` publica la bitácora.
  - **Cuándo:** solo si la firma quedó aceptada y la cadena tiene un paso siguiente, y nunca cuando el paso siguiente es F1.
- **Rationale:**
  - Firma es quien sabe que una firma se aceptó, cuál es el paso siguiente y quién es su firmante esperado (D-102). Aprobación no lo sabe: solo se entera de la cadena completa (D-104).
  - D-108 ya preveía que F16-H02 publicara el mismo contenido desde Firma.
  - En la misma transacción, una firma revertida no notifica, y una registrada no se pierde (FR-007, FR-008).
- **Alternatives considered:**
  - **Que Aprobación notifique,** con un aviso de Firma por cada firma: Aprobación tendría que conocer el turno y los firmantes, que son de Firma (MD-05).
  - **Publicar después de confirmar,** fuera de la transacción: si el proceso cae entre medio, la notificación se pierde.

## §2. Cómo se dice «a esta persona» en la solicitud

- **Decision:** el destino de un mensaje admite, además del rol, una persona, con exactamente una de tres formas:
  - `{rol, carrera?}`, la de F16-H01, sin cambios;
  - `{usuario}`, para el firmante que se reconoce por su usuario (el designado);
  - `{identificacion}`, para el docente de un cargo (el Vicerrector y el Rector).
  
  Notificaciones resuelve la persona en su proceso: el usuario, si existe y está activo; la identificación, con la cuenta activa que la tiene.
- **Rationale:**
  - El firmante esperado es una persona fija, no un rol (D-102, FR-002).
  - Firma conoce la identificación del cargo, pero no la cuenta. Resolverla en Firma sería una consulta síncrona a Usuarios dentro de la transacción de la firma, que D-108 descartó por la misma razón.
  - Usuarios vive en el proceso `identidad`, junto a Notificaciones, y la identificación es única entre las cuentas (Usuarios V7, índice `usuario_identificacion`): hay a lo sumo una cuenta.
- **Alternatives considered:**
  - **Un destino por rol** (VICERRECTOR, RECTOR): llegaría a quien tiene el rol sin ser la persona del paso (FR-002 c).
  - **Un tipo de evento aparte** para personas: duplica el receptor y la validación por un campo.
  - **Que Firma resuelva la cuenta** por una ruta interna nueva de Usuarios: una llamada síncrona a otro proceso en la transacción de la firma, que además falla si `identidad` está caído (FR-010).

## §3. Usuarios: buscar la cuenta de una identificación

- **Decision:** un caso de uso publicado nuevo en Usuarios, `ConsultarCuentaPorIdentificacion`, que devuelve el resumen de la cuenta (`ResumenDeUsuario`: usuario, nombre y si está activa) o vacío.
- **Rationale:**
  - Notificaciones solo usa interfaces publicadas de otros servicios (principio V).
  - `ConsultarIdentificacion` hace lo contrario, de usuario a identificación, para Firma (F14-H01).
  - El resumen no expone la identificación, ni la contraseña ni su hash.
- **Alternatives considered:**
  - **Filtrar `ListarUsuarios`:** su resumen no lleva la identificación, y agregarla la expondría en la administración de roles.

## §4. La identificación en el evento

- **Decision:** la identificación viaja en el contenido del evento, y no se guarda en la notificación, que guarda el usuario destinatario.
  - Notificaciones no la registra en sus mensajes de error: un 400 dice «la identificación del destino no tiene la forma esperada», sin el valor.
- **Rationale:**
  - **Ya está en Firma:** la tabla de firmantes esperados la guarda desde D-102. El registro de publicaciones del proceso `distributivo` la guarda mientras se entrega, y sale por la red interna con el token de proceso, como el resto de los eventos.
  - **No se muestra:** la notificación y la cadena muestran el nombre impreso en el PDF, nunca la identificación (FR-016).
- **Alternatives considered:**
  - **Un identificador opaco del firmante:** Notificaciones no tendría cómo resolverlo sin otra tabla compartida.

## §5. El texto, el autor y el enlace

- **Decision:**
  - **Título:** «Tienes una firma pendiente: «{rótulo}»», por ejemplo «Tienes una firma pendiente: «Revisado por»».
  - **Detalle:** «Periodo {p}, versión {v}. «{rótulo anterior}» la firmó {nombre impreso del firmante anterior}.»
  - **Autor:** quien subió la firma aceptada.
  - **Momento:** el momento en que el MDD la aceptó (`subidoEn`).
  - **Enlace:** `/firmas-pendientes`.
- **Rationale:**
  - La bandeja ya muestra el autor y el momento de cada notificación. Con el autor y el momento de la firma aceptada, la notificación dice quién firmó antes y cuándo (FR-003), con la fecha que fijó la aclaración del 2026-11-18.
  - El nombre impreso es el que ya muestra el PDF, sin la identificación (FR-016).
  - El Vicerrector y el Rector tienen `DISTRIBUTIVO:FIRMA` (D-16), el permiso de la ruta `/firmas-pendientes`.
- **Alternatives considered:**
  - **La fecha en el texto:** habría que darle formato con zona horaria en el backend, y la bandeja ya la muestra.

## §6. La cadena en la consulta del documento firmable

- **Decision:** la respuesta de `consultarDocumentoFirmable` (`GET /api/firma/periodos/{periodo}/documento`) suma `cadena`, una lista de pasos, solo con el documento GENERADO y sin anulación. Cada paso lleva:
  - `orden` y `rotulo`;
  - `firmante`, el nombre impreso, si se guardó al generar;
  - `estado`: `FIRMADO`, `EN_TURNO`, `PENDIENTE` o `RECHAZADO`;
  - `firmadoEn`, solo con `FIRMADO`.
  
  La regla de los estados va en el dominio, en `FirmasDelDocumento.cadenaConEstado()`, junto a `pasoEnTurno()`.
- **Rationale:**
  - **Mismo permiso:** la consulta ya la leen quienes leen el documento firmable (`CONSOLIDACION:LECTURA` o `DISTRIBUTIVO:ESCRITURA`), así que la regla de permisos no cambia (FR-013).
  - **Una sola regla del turno:** la cadena y «Pendientes de firma» no pueden contradecirse (FR-012).
  - **Sin anulación:** con las firmas anuladas (F12-H04), la SPA ya trata ese documento como de otra versión. Sin cadena, las firmas anuladas no aparecen como firmadas.
- **Alternatives considered:**
  - **Una ruta nueva para la cadena:** otro permiso, otra consulta y otra prueba de contrato para lo mismo.
  - **Mostrar la cadena de un documento con las firmas anuladas:** confunde la versión anulada con la vigente.

## §7. La SPA

- **Decision:** `DocumentoFirmable.tsx` muestra, debajo de «Documento firmable generado», una lista «Cadena de firmas», con un elemento por paso:
  - «F{orden} · {rótulo} · {firmante}» y el estado en texto: «Firmado el {dd/mm/aa hh:mm}», «En turno», «Pendiente» o «Rechazado».
  - El paso en turno se resalta, pero el estado se dice con texto (FR-015).
- **Rationale:**
  - El documento firmable ya está en la tarjeta del flujo de «Consolidado del periodo», que el Vicerrector y el Responsable abren (aclaración del 2026-11-17).
  - La consulta se repite al volver a abrir la pantalla (FR-014).
- **Alternatives considered:**
  - **Una tabla:** tres filas no la justifican, y una lista se lee mejor con un lector de pantalla.

## §8. Las pruebas de punta a punta

- **Decision:** `probar_documento_firmable.sh` suma:
  - **Las bandejas:** a las esperadas del paso 16, «Tienes una firma pendiente: «Revisado por»» para `vicerrectorado.demo` y «Tienes una firma pendiente: «Aprobado por»» para `rectorado.demo`, y ninguna para nadie más.
  - **La cadena:** su estado después de generar, de cada firma aceptada y del rechazo, en el paso de las firmas y en un paso nuevo.
  - **Una caída de `identidad`:** durante la subida de F1. La notificación de F2 llega cuando vuelve, una sola vez (SC-002).
- **Rationale:**
  - **Por qué F1:** la subida de F1 no necesita a `identidad`, porque el designado se reconoce por su usuario. Las de F2 y F3 sí: piden la identificación de quien sube (D-102).
  - **El costo:** el script ya recorre las tres firmas, y la caída suma alrededor de un minuto.
- **Alternatives considered:**
  - **Solo una prueba de integración** de la publicación: no prueba la llegada después de la caída con los procesos reales.
  - **Mover la caída del envío a revisión:** dejaría sin la prueba de punta a punta la notificación de Aprobación de F16-H01.
