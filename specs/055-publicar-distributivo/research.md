# Research: Publicar el distributivo aprobado

F18-H01 (#71). Cada sección resuelve una pregunta del plan con su decisión, el porqué y lo que se descartó.

## §1. Quién publica y dónde vive la regla

- **Decision:** Validación OCS (Aprobación) publica, con una operación nueva, `POST /api/validacionocs/periodos/{periodo}/publicacion`, y la regla va en el agregado: `ProcesoAprobacion.publicar(quien, ahora)`, con la versión y el hash de la congelación, como `registrarResolucion`.
  - Exige el distributivo en APROBADO y el rol de la etapa APROBADO de la copia de la configuración.
  - Registra un `PASO_DE_ETAPA` de APROBADO a PUBLICADO con la versión y el hash vigentes.
  - El caso de uso, `PublicarDistributivoServicio`, bloquea el proceso, guarda la transición, notifica (D-108) y registra el cambio en la bitácora en la misma transacción, como `RegistrarResolucionServicio`.
- **Rationale:**
  - MD-11 y el glosario: publicar es una transición de Aprobación (`publicar()`); Publicación y consulta solo lee la versión publicada.
  - Con el proceso bloqueado, dos publicaciones a la vez dejan una sola transición (FR-004), como en F15-H01.
  - El permiso de la operación es `PUBLICACION:APROBACION`, que la matriz D-16 ya da a la Secretaría general académica; el rol de la etapa se comprueba en el agregado, como en las demás operaciones del flujo. Hacen falta los dos, como al registrar la resolución (D-107): un rol de la etapa sin el permiso recibe el 403 común.
- **Alternatives considered:**
  - **El avance genérico desde APROBADO:** daría el mismo paso, pero sin una acción y un error propios, y sin el permiso de publicar. ADR-007 deja a APROBADO y PUBLICADO en el esqueleto fijo, con su operación propia.
  - **Un servicio de Publicación que publique:** contradice MD-11 y crea un ciclo con Aprobación.

## §2. Qué significa «la versión queda bloqueada»

- **Decision:** la versión publicada es la versión congelada que llegó a APROBADO; Generación no cambia. El bloqueo es:
  - **del contenido:** ya inmutable desde D-100, con sus errores (`GENERACION_VERSION_CONGELADA` y los de cada operación);
  - **del flujo:** en PUBLICADO, `ProcesoAprobacion.acciones()` no ofrece ninguna, y cada operación del flujo responde con el error de su etapa.
  
  La versión publicada y quién la publicó quedan en la transición a PUBLICADO, que Aprobación expone en el estado del flujo (`publicacion`).
- **Rationale:**
  - RF-40 pide que no haya acciones de edición sobre la versión firmada: ya no las hay desde la congelación, y esta historia cierra también el flujo.
  - La reforma (F18-H02) y la consulta (F19) leerán la versión publicada de Aprobación, que es quien la decide (MD-11).
- **Alternatives considered:**
  - **Un estado PUBLICADA en Generación:** obligaría a Aprobación a avisar a Generación en la transacción de la publicación, para un dato que la versión congelada ya cumple. Se reconsidera en F18-H02 si la reforma lo necesita.

## §3. A quién se notifica

- **Decision:** `ProcesoAprobacion.destinoDe(transicion)` devuelve, para el paso a PUBLICADO, tres destinos por rol: DOCENTE, OPERADOR_DATOS y COORDINADOR_CARRERA, sin carrera (aclaración del 2026-11-18).
  - Es una excepción a la regla de D-108 («el rol de la etapa de llegada»), como lo son el envío de un parcial y la devolución.
  - Un usuario con dos de esos roles recibe una sola notificación: Notificaciones guarda una por evento, destinatario y carrera (F16-H01), y los tres destinos van sin carrera.
  - Quien publicó no la recibe, como en todas las transiciones.
- **Rationale:**
  - El glosario y el diseño de firmas nombran a los docentes, a Talento Humano y a las coordinaciones; RF-41 incluye «publicado».
  - Por rol, Notificaciones los resuelve sin pedirle nada nuevo a Roles ni a Usuarios.
  - El rol de la etapa PUBLICADO, por defecto la Secretaría, es quien publica: con la regla de D-108 nadie recibiría nada.
- **Alternatives considered:**
  - **Los docentes del distributivo publicado, por persona (D-110):** descartado por el autor; además, Aprobación tendría que pedirle a Generación la lista de docentes de la versión.
  - **Sumar el rol de la etapa PUBLICADO:** la otra Secretaría no necesita enterarse; el issue nombra solo a los tres.

## §4. El mensaje y el enlace

- **Decision:**
  - **Título:** «El distributivo de {periodo} fue publicado».
  - **Detalle:** «Versión {v}. Es la versión oficial del periodo.»
  - **Enlace:** por rol. COORDINADOR_CARRERA va a `/oferta`; DOCENTE y OPERADOR_DATOS, al inicio, `/`.
- **Rationale:**
  - La bandeja ya muestra quién y cuándo (el autor y el momento de la transición).
  - «Oferta y propuesta de docentes» muestra la tarjeta del flujo, con la constancia de la publicación, y las coordinaciones pueden abrirla. Los docentes y Talento Humano no pueden abrir otra pantalla del distributivo: la del docente llega con F19. `enlaces.test.ts` comprueba cada enlace contra los permisos de su rol.
  - El enlace de PUBLICADO deja de ser el provisorio de D-108 (`/resolucion-ocs`), que solo la Secretaría podía abrir.
- **Alternatives considered:**
  - **El inicio para los tres roles** (la decisión del plan): en el recorrido del autor (2026-11-19), el clic no llevaba a ninguna parte visible, porque el usuario ya está en el inicio. Se cambió a un enlace por rol.

## §5. La pantalla

- **Decision:**
  - **La acción:** la acción nueva `PUBLICAR` del estado del flujo. «Resolución del OCS» la muestra en la tarjeta del estado del flujo, con un diálogo de confirmación que dice que la versión quedará bloqueada y a quiénes se notificará (aclaración del 2026-11-18).
  - **La constancia:** `EstadoDelFlujo.publicacion` (versión, quién y cuándo). La ve todo el que lee el estado del flujo, en «Resolución del OCS» y en «Consolidado del periodo».
  - **El aviso:** «Consolidado del periodo» y el panel de inconsistencias dicen «La versión {v} está publicada: ya no cambia.» cuando el flujo está en PUBLICADO, en vez de «está congelada».
- **Rationale:** la Secretaría registra la resolución en esa misma pantalla; la tarjeta del flujo ya muestra las acciones de cada etapa (F15-H01).
- **Alternatives considered:**
  - **Un botón en «Consolidado del periodo»:** la Secretaría no abre esa pantalla (D-107).

## §6. Los errores

- **Decision:** un código nuevo, `VALIDACIONOCS_ETAPA_NO_ADMITE_PUBLICACION` (409, Negocio): «El distributivo solo se publica en APROBADO». Lo lanza el agregado con el distributivo en otra etapa, también en PUBLICADO y en la segunda de dos publicaciones simultáneas.
  - Sin el rol de la etapa: el 403 común (`SEGURIDAD_ACCESO_NO_AUTORIZADO`), como en las demás operaciones.
  - Sin distributivo o con el periodo cerrado: los códigos que ya existen.
- **Rationale:** el patrón de `VALIDACIONOCS_ETAPA_NO_ADMITE_RESOLUCION` (F15-H01).

## §7. La bitácora

- **Decision:** una acción nueva de Validación OCS, `DISTRIBUTIVO_PUBLICADO`: cambia, con la etapa antes y después, y el periodo, la versión y el hash en el detalle.
- **Rationale:** cada transición deja su entrada (D-95). Usuarios toma las acciones de lo que hay en la bitácora, así que no hay una lista que actualizar.

## §8. Las pruebas de punta a punta

- **Decision:** `probar_documento_firmable.sh` sigue, después de la resolución:
  - **Publicar:** los 403 y el 409 antes de APROBADO, la publicación de `secretaria.demo`, su constancia en el flujo, la bitácora, y el 409 de una segunda publicación.
  - **La caída de `identidad`:** se mueve del envío a revisión a la publicación. Las dos son transiciones de Validación OCS por el mismo camino (D-108), así que sigue cubriendo SC-002 de F16-H01, y cubre SC-003 de esta historia sin sumar otra espera.
  - **Sin acciones:** con el distributivo publicado, el flujo no ofrece acciones a ningún rol, y cada operación de cambio probada responde con su error.
  - **Las bandejas:** suman la publicación para `datos.demo`, `coordinacion.demo`, `docente.demo` y `docente.coordinador.demo` (una sola, aunque tenga dos roles).
- **Rationale:** el script ya llega a APROBADO; publicar es su siguiente paso natural, y la caída en la publicación prueba los dos criterios con un minuto en lugar de dos.
- **Alternatives considered:**
  - **Una tercera caída:** alarga el script cerca de un minuto sin probar un camino distinto.
