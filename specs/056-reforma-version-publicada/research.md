# Research: Iniciar una reforma de la versión publicada

F18-H02 (#72). Cada sección resuelve una pregunta del plan con su decisión, el porqué y lo que se descartó.

## §1. Quién inicia la reforma y dónde vive la regla

- **Decision:** Validación OCS (Aprobación) inicia la reforma, con una operación nueva, `POST /api/validacionocs/periodos/{periodo}/reforma`, con el motivo en el cuerpo. La regla va en el agregado: `ProcesoAprobacion.iniciarReforma(quien, motivo, nueva, hash, ahora)`.
  - Exige el distributivo en PUBLICADO (si no, `EtapaNoAdmiteReforma`, 409).
  - Exige el rol de la etapa EN_REVISION_VRA de la copia de la configuración, que por defecto es el Vicerrector (si no, `RolDeOtraEtapa`, el 403 común), además del permiso `DISTRIBUTIVO:APROBACION` de la matriz D-16 en la operación.
  - Registra una transición nueva, `REFORMA`, de PUBLICADO a EN_CONSOLIDACION (o a la siguiente etapa activa desde ella), con la versión nueva, su hash, quién, cuándo, el motivo y la versión publicada.
- **Rationale:**
  - El glosario y el modelo de dominio: `iniciarReforma()` es una operación de `ProcesoAprobacion`, y una versión publicada solo cambia con una reforma.
  - El issue dice «el Vicerrector académico». El rol que aprueba el contenido es el de EN_REVISION_VRA en la configuración del periodo (principio VI): así no se fija un nombre de rol en el código. Aunque EN_REVISION_VRA esté desactivada, la copia de la configuración conserva su rol.
  - `DISTRIBUTIVO:APROBACION` es el permiso de aprobar el contenido, que el Vicerrector ya tiene. El Rector también lo tiene, pero no tiene el rol de la etapa: hacen falta los dos, como en las demás operaciones del flujo (D-107, D-111).
- **Alternatives considered:**
  - **El rol de la etapa PUBLICADO** (la Secretaría): el issue pide al Vicerrector, que es quien responde por el contenido.
  - **El rol VICERRECTOR fijo:** contradice el principio VI.
  - **Un permiso nuevo en la matriz:** D-63 deja fijos los roles y la matriz; aprobar el contenido ya expresa la autoridad sobre él.

## §2. La versión nueva y la publicada anterior

- **Decision:** Generación crea la versión nueva por orden de Aprobación, como en D-106, con un motivo nuevo, `REFORMA`, y **sin anular la anterior**:
  - `MotivoDeVersion` suma `REFORMA`, y su `check` en `generacion.version_distributivo` también (migración V9 de Generación).
  - `crearVersionNueva(periodo, version, motivo, momento)`: con `RECHAZO_EN_FIRMA`, como hoy, la anterior pasa a ANULADA; con `REFORMA`, la anterior sigue CONGELADA. En los dos casos, la nueva es EDITABLE y copia todo (los parciales con su estado, las propuestas, las actividades, las justificaciones, la consolidación y el informe), con el motivo, quién y cuándo.
  - **Idempotente:** si la vigente ya es la siguiente, con el mismo motivo, y la anterior sigue en el estado que corresponde (ANULADA o CONGELADA), devuelve la existente sin crear otra.
  - Quien crea la versión es quien inició la reforma, no `sistema`: la reforma la pide una persona.
- **El orden**, como la aprobación del contenido (D-100) y el rechazo (D-106):
  1. el caso de uso comprueba sin cambiar nada (el periodo, el distributivo, la etapa y el rol, con el agregado);
  2. ordena a Generación la versión nueva, en la transacción de Generación;
  3. con el proceso bloqueado, el agregado registra la `REFORMA`, se guarda, se notifica y se anota en la bitácora.
  Un reintento después de un fallo entre 2 y 3 repite 2, que devuelve la misma versión, y completa 3.
- **Rationale:**
  - El diagrama de estados de la versión: una CONGELADA «se publica y queda inmutable (histórica si luego hay reforma)». No se anula: sus firmas y su resolución siguen valiendo para ella.
  - La versión vigente de Generación es la de número más alto, así que la de la reforma pasa a ser la de trabajo sin otra regla.
  - Con el proceso en EN_CONSOLIDACION, Generación vuelve a admitir cambios en el consolidado (`admiteCambiosEnElConsolidado`), y los parciales copiados siguen ENVIADO: las carreras solo editan si se les devuelve el parcial.
- **Alternatives considered:**
  - **Anular la publicada, como el rechazo:** el diseño la deja histórica, no anulada; la anulación es para una versión que no llegó a valer.
  - **Una operación aparte de Generación para la reforma:** duplica la copia de seis tablas; el motivo basta para distinguir.
  - **Crear la versión en la misma transacción que la transición:** ataría los dos esquemas; D-100 y D-106 ya eligieron no hacerlo.

## §3. La versión publicada vigente durante la reforma

- **Decision:** la versión publicada vigente del periodo es la de la **última transición a PUBLICADO** (`ProcesoAprobacion.publicacion()`, de F18-H01). Durante la reforma sigue siendo la anterior; al publicarse la de la reforma, pasa a ser la nueva y la anterior queda histórica, sin que nada cambie en la base.
  - El estado del flujo conserva `publicacion` (la anterior) durante la reforma, y suma `reforma`: la versión publicada, la versión nueva, el motivo, quién y cuándo, mientras la reforma esté en curso (la última `REFORMA` no tiene una publicación después).
  - La congelación y la resolución del estado del flujo se muestran solo si son de la versión de trabajo, como ya pasa con la congelación (F12-H04). Las de la versión anterior siguen en la base y en la bitácora.
- **Rationale:** aclaración del 2026-11-20: el periodo nunca se queda sin un distributivo oficial. F19 leerá la versión publicada de Aprobación con la misma regla.
- **Alternatives considered:**
  - **Marcar la versión anterior como HISTORICA en Generación:** un estado nuevo, para un dato que ya se deduce de las transiciones.

## §4. La etapa de regreso y cómo se hacen los cambios

- **Decision:** la `REFORMA` va a EN_CONSOLIDACION o, si está desactivada en la copia de la configuración, a la siguiente etapa activa desde ella (`siguienteActiva(BORRADOR_UNIDADES)`, la regla que ya existe). Desde ahí no hay nada nuevo:
  - quien consolida devuelve los parciales que cambian (F12-H05), las carreras los corrigen y los reenvían, se consolida, se adjunta el informe y se envía a revisión;
  - el Vicerrector aprueba el contenido, que congela la versión nueva y genera su documento con la cadena desde F1 (D-100, D-101);
  - la cadena completa, la resolución del OCS (única por versión, D-107) y la publicación.
- **Rationale:** aclaración del 2026-11-20 y el diagrama del diseño (`PUBLICADO --> EN_CONSOLIDACION`).
- **Alternatives considered:**
  - **Elegir las carreras al iniciar:** descartado por el autor.
  - **Volver a EN_REVISION_VRA:** sin el ajuste del Vicerrector (RF-35), nadie podría cambiar el contenido.

## §5. A quién se notifica

- **Decision:** `ProcesoAprobacion.destinoDe(transicion)` devuelve, para la `REFORMA`, el rol de la etapa a la que llega (por defecto, el Responsable de consolidación, que incluye al designado del periodo, D-108) y los tres destinos de la publicación: DOCENTE, OPERADOR_DATOS y COORDINADOR_CARRERA, sin carrera.
  - Una sola notificación por usuario: la clave única de Notificaciones por evento, destinatario y carrera (F16-H01).
  - Nunca a quien la inició.
- **El mensaje:**
  - **Título:** «Se inició una reforma del distributivo de {periodo}».
  - **Detalle:** «Versión {nueva}, reforma de la versión {publicada}. Motivo: «{motivo}». La versión {publicada} sigue vigente hasta que se publique la reforma.»
  - **Enlace, por rol** (como D-111): el rol de la etapa de llegada, a la de su etapa (`/consolidado`); COORDINADOR_CARRERA, a `/oferta`; DOCENTE y OPERADOR_DATOS, al inicio.
- **Rationale:** aclaración del 2026-11-20: los que recibieron la publicación tienen que saber que va a cambiar, y quien consolida empieza a trabajar.
- **Alternatives considered:**
  - **Solo el rol de la etapa (D-108):** descartado por el autor.

## §6. El motivo y los errores

- **Decision:**
  - **El motivo:** obligatorio, de 10 a 500 caracteres sin los espacios de los extremos, con el tipo `Observacion` que ya usan la devolución y el rechazo.
  - **Códigos nuevos:**
    - `VALIDACIONOCS_ETAPA_NO_ADMITE_REFORMA` (409, Negocio): fuera de PUBLICADO, también en la segunda de dos simultáneas.
    - `VALIDACIONOCS_MOTIVO_DE_REFORMA_INVALIDO` (400, Validación): el motivo falta o no tiene de 10 a 500 caracteres.
  - Sin el rol de la etapa o sin el permiso, el 403 común; con el periodo cerrado o sin distributivo, los códigos que ya existen. Un cuerpo mal formado, `COMUN_SOLICITUD_INVALIDA`.
  - Si Generación no tiene la versión publicada como vigente ni la siguiente de una reforma, es un estado imposible: error técnico, sin cambiar nada.
- **Rationale:** `VALIDACIONOCS_OBSERVACION_INVALIDA` habla de «cada carrera devuelta» y lleva el detalle `carrera`: su texto no sirve para la reforma (principio XIII).

## §7. Los datos y la bitácora

- **Decision:**
  - **Validación OCS, V8:** `transicion.tipo` suma `REFORMA`; una tabla nueva, `validacionocs.reforma`, con la transición, el periodo, la versión publicada y el motivo, como `rechazo_en_firma` (V5) y `resolucion_ocs` (V7).
  - **Generación, V9:** el `check` de `version_distributivo.motivo` suma `REFORMA`.
  - **La bitácora:** una acción nueva de Validación OCS, `REFORMA_INICIADA`: cambia, con la etapa antes y después, y el periodo, la versión publicada, la versión nueva, su hash y el motivo en el detalle.
- **Rationale:** cada transición deja su entrada (D-95), y el motivo es de la transición, no de la versión de Generación, que solo guarda el motivo del catálogo.

## §8. Las acciones en PUBLICADO

- **Decision:** `ProcesoAprobacion.acciones(quien)` en PUBLICADO devuelve `[INICIAR_REFORMA]` a quien tiene el rol de EN_REVISION_VRA, y ninguna a los demás. Esto cambia lo que F18-H01 probó: «en PUBLICADO, ninguna acción para nadie» pasa a «ninguna acción de edición; el Vicerrector puede iniciar una reforma».
- **Rationale:** RF-40 pide que no haya acciones de **edición** sobre la versión firmada; iniciar una reforma no edita la publicada, crea otra.

## §9. La pantalla

- **Decision:** «Consolidado del periodo», en la tarjeta del estado del flujo:
  - **La acción:** «Iniciar una reforma», con un diálogo que pide el motivo (de 10 a 500 caracteres, con su contador) y dice que se creará una versión nueva, que el distributivo vuelve a consolidación, que la versión publicada sigue vigente hasta que se publique la reforma y a quiénes se notificará. Foco en «Cancelar» al abrirse; un error queda en el diálogo.
  - **La reforma en curso:** «Reforma de la versión {publicada} en curso: versión {nueva}, iniciada por {quien} el {fecha}», con el motivo, para todo el que lee el estado del flujo.
  - **La publicación anterior:** su constancia sigue, con «vigente hasta que se publique la reforma».
- **Rationale:** el Vicerrector ya aprueba el contenido en esa tarjeta, y quien consolida trabaja en esa pantalla.

## §10. Las pruebas de punta a punta

- **Decision:** `probar_documento_firmable.sh` sigue después del paso 21, con la versión 1 publicada:
  - **Iniciar la reforma:** el 409 en otra etapa (se prueba en APROBADO, en el paso 15); en PUBLICADO, el 403 de `secretaria.demo` y `rectorado.demo`, el 400 sin motivo, la reforma de `vicerrectorado.demo` con `identidad` detenido, la versión 2 marcada como reforma, la versión 1 todavía CONGELADA y vigente, la bitácora y el 409 de una segunda.
  - **La notificación:** a `consolidacion.demo`, `docente.demo` (una sola, aunque es el designado y DOCENTE), `datos.demo`, `coordinacion.demo` y `docente.coordinador.demo`, una vez cada uno, antes de 120 s desde la vuelta de `identidad`; no al Vicerrector.
  - **El ciclo de la versión 2:** devolver un parcial, reenviarlo, consolidar, adjuntar el informe, enviar a revisión, aprobar, la cadena desde F1, las tres firmas, la resolución y la publicación; al final, la versión 2 es la publicada y la 1 conserva sus tres firmas, su documento y su resolución.
  - `MDD_CONSERVAR=PUBLICADO` se detiene con la versión 1 publicada, para que el autor inicie la reforma a mano.
- **Rationale:** el script ya llega a PUBLICADO y tiene los ayudantes de las firmas y la resolución; el ciclo completo es SC-002.
- **Alternatives considered:**
  - **Un script aparte:** repetiría desde cero las cargas, el consolidado y la primera cadena de firmas.
  - **Sin la caída de `identidad`:** SC-004 la pide; cuesta lo que tarde el reenvío, y `esperar` termina en cuanto llega.
