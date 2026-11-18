---
description: "Tareas de F16-H02: notificar al siguiente firmante y ver la cadena de firmas"
---

# Tasks: Notificar al siguiente firmante y ver la cadena de firmas

**Input**: documentos de diseño en `specs/054-siguiente-firmante/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si no compila, primero va un esqueleto.

**Decisiones**:
- las dos aclaraciones del autor del 2026-11-17 y las dos del 2026-11-18, en la spec;
- D-110, escrita con el plan;
- D-102 a D-106 y D-108, MD-05 y ADR-018.

**Organization**:
- **Fundacional:** los dos contratos, la consulta de Usuarios, los destinos por persona de Notificaciones y la cadena con estado en el dominio de Firma.
- **US1:** el siguiente firmante recibe su notificación (MVP).
- **US2:** la cadena de firmas en «Consolidado del periodo».
- **US3:** sin duplicados y sin pérdidas.
- **Cierre:** los scripts, las regresiones, la evidencia y la fusión.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US3.
- **Rutas:** son relativas a la raíz. Cada abreviatura tiene `main`, `test` e `integrationTest` en `java/ec/mdd/<servicio>`:

  | Abreviatura | Ruta |
  |---|---|
  | `FIR` | `backend/servicios/firma/src` |
  | `NOT` | `backend/servicios/notificaciones/src` |
  | `USU` | `backend/servicios/usuarios/src` |
  | `SPA` | `frontend/src` |

---

## Phase 1: Setup

- [X] T001 Escribir **D-110** en `docs/decisiones.md`, en la fila del índice y en su sección (research §1 a §8). Revisar en el glosario la cadena de firmas, el firmante y `Notificacion`.

  **Resultado (2026-11-18):** quedó escrita con el plan.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T006.

- [X] T002 [P] El destino por persona en `contratos/notificaciones/openapi.yaml`, según `contracts/eventos.md`:
  - `DestinoDelMensaje` pasa a `oneOf` de `DestinoPorRol` («`rol` requerido, `carrera` opcional»), `DestinoPorUsuario` («`usuario` requerido») y `DestinoPorIdentificacion` («`identificacion` requerida, `^[0-9A-Z]{1,13}$`»), cada uno con `additionalProperties: false`;
  - `usuario`, de 1 a 64 caracteres, sin patrón (el largo de `nombre_usuario`; Usuarios no define un formato).
  
  Regenerar los tipos de la SPA (`npm run generar:api`). Correr la prueba de contrato del receptor con un destino por rol, como el que publica Validación OCS: sigue dando 204.

  **Resultado (2026-11-18):** `DestinoDelMensaje` es un `oneOf` de `DestinoPorRol`, `DestinoPorUsuario` (de 1 a 64, sin patrón) y `DestinoPorIdentificacion`. La descripción de `recibirEvento` nombra a Firma. El código generado compila y los tipos de la SPA quedaron regenerados. `EventosNotificacionesContratoTest`, con el destino por rol, sigue pasando.
- [X] T003 [P] La cadena en `contratos/firma/openapi.yaml`, según `contracts/firma.md`:
  - `EstadoDelDocumento.cadena`, **requerida**, `maxItems: 10`, vacía si no corresponde;
  - `PasoConEstado`: «`orden` ≥ 1», «`rotulo` ≤ 60», «`firmante` ≤ 200, opcional», «`estado` en `FIRMADO`, `EN_TURNO`, `PENDIENTE`, `RECHAZADO`», «`firmadoEn` date-time, solo con `FIRMADO`», `additionalProperties: false`.
  
  Regenerar los tipos de la SPA. Esqueleto: `DocumentoControlador` responde `cadena: []` hasta T012.

  **Resultado (2026-11-18):** `EstadoDelDocumento.cadena`, requerida, y `PasoConEstado`. El esqueleto responde `cadena: []`; `DocumentoContratoTest` comparaba el JSON exacto y se actualizó con `"cadena":[]`.
- [X] T004 [P] Usuarios: `USU/main/.../puertos/entrada/ConsultarCuentaPorIdentificacion.java` («`de(String identificacion)` devuelve `Optional<ResumenDeUsuario>`»), su servicio en `aplicacion/` sobre el repositorio de usuarios y su bean. Solo lee; no devuelve la identificación, la contraseña ni su hash. Antes, ver fallar:
  - `USU/test/.../aplicacion/` su prueba: con cuenta activa, con cuenta inactiva (se devuelve con `activo = false`) y sin cuenta;
  - `USU/integrationTest/...` contra PostgreSQL: la búsqueda usa el índice único `usuario_identificacion`.

  **Resultado (2026-11-18):** `ConsultarCuentaPorIdentificacion` con su servicio y su bean; `RepositorioUsuarios.buscarPorIdentificacion` en JPA y en el doble de memoria. Se vio fallar: 2 de 3 en la prueba del servicio (la de «sin cuenta» pasaba contra el esqueleto, que siempre da vacío) y la de PostgreSQL. `:servicios:usuarios:check` e `integrationTest` pasan.
- [X] T005 [P] Los destinos por persona en el dominio de Notificaciones, según `data-model.md`:
  - `NOT/main/.../dominio/DestinoDelMensaje.java` pasa a ser uno de `PorRol(rol, carrera)`, `PorUsuario(usuario)` y `PorIdentificacion(identificacion)`;
  - `SolicitudDeNotificaciones.de(Map)`: «exactamente una de las claves `rol`, `usuario` o `identificacion`», «`carrera` solo junto con `rol`», el patrón de la identificación y el largo del usuario de T002, y un error de forma que **no repite el valor recibido**.
  
  Ajustar a la nueva forma `RecibirNotificacionesServicio`, `DestinatariosDesdeIdentidad` y sus pruebas, sin cambiar todavía su comportamiento. Antes, ver fallar `SolicitudDeNotificacionesTest` con los tres destinos, dos claves a la vez, `carrera` sin `rol`, una identificación mal formada (sin el valor en el mensaje) y un usuario mal formado.

  **Resultado (2026-11-18):** `DestinoDelMensaje` es una interfaz sellada con `PorRol`, `PorUsuario` y `PorIdentificacion`; `PorIdentificacion.toString()` no muestra el valor. Se vieron fallar las 2 pruebas que esperan un destino por persona. **Las de destinos inválidos pasaron contra el esqueleto**, que rechazaba toda clave distinta de `rol`. El adaptador y los dobles ignoran por ahora los destinos por persona (T009).
- [X] T006 [P] La cadena con estado en el dominio de Firma, según `data-model.md`:
  - `FIR/main/.../dominio/PasoConEstado.java`: «`orden`, `rotulo`, `firmante` opcional, `estado`, `firmadoEn` solo con `FIRMADO`»;
  - `FirmasDelDocumento.cadenaConEstado()` y `siguienteTrasFirmar(int orden)`, con la regla del turno de `pasoEnTurno()`.
  
  Antes, ver fallar en `FirmasDelDocumentoTest`:
  - recién generado: F1 `EN_TURNO`, F2 y F3 `PENDIENTE`;
  - F1 firmada: `FIRMADO` con `firmadoEn` = su `subidoEn`, F2 `EN_TURNO`;
  - completo: tres `FIRMADO`, ninguno en turno;
  - rechazado en F2: F1 `FIRMADO`, F2 `RECHAZADO`, F3 `PENDIENTE`, ninguno en turno;
  - sin firmantes esperados: ninguno en turno y sin `firmante`;
  - en cada caso, el `EN_TURNO` coincide con `pasoEnTurno()`;
  - `siguienteTrasFirmar(1)` da F2, `(2)` da F3 y `(3)` da vacío.


  **Resultado (2026-11-18):** `PasoConEstado` y `FirmasDelDocumento.cadenaConEstado()` y `siguienteTrasFirmar(orden)`, en `CadenaConEstadoTest`. Se vieron fallar 5 de 8; **las otras 3 pasaron contra el esqueleto**: con anulación, sin firmantes esperados y el siguiente sin firmante, porque el esqueleto devolvía listas vacías. `PasoEnTurno.rotulo()` usa el mismo rótulo.
**Checkpoint**: los contratos, la consulta de Usuarios, los destinos y la cadena del dominio, en verde.

---

## Phase 3: User Story 1 - El siguiente firmante recibe su notificación (Priority: P1) 🎯 MVP

**Goal**: cuando se acepta F1 o F2, el firmante esperado del paso siguiente, la persona, recibe una notificación de firma pendiente que lleva a «Pendientes de firma».

**Independent Test**: el designado sube F1 y se acepta. `vicerrectorado.demo` tiene «Tienes una firma pendiente: «Revisado por»», con `docente.demo` como autor, y nadie más la tiene.

- [X] T007 [P] [US1] El puerto `FIR/main/.../puertos/salida/NotificacionDeFirmaPendiente.java`, con `FirmaPendienteSolicitada` («`periodo`, `version`, `autor`, `momento`, `rotulo`, el firmante esperado por usuario o por identificación, `rotuloAnterior`, `nombreAnterior`»), y el adaptador `adaptadores/salida/PublicadorDeFirmaPendiente.java`:
  - un `EventoEntreProcesos` `firma.NotificacionesSolicitadas`, de `firma` a `notificaciones`;
  - un mensaje con el destino `{usuario}` o `{identificacion}`, el título «Tienes una firma pendiente: «{rótulo}»», el detalle «Periodo {p}, versión {v}. «{rótulo anterior}» la firmó {nombre anterior}.» y el enlace `/firmas-pendientes`.
  
  El bean va en `ConfiguracionFirma`. Antes, ver fallar `PublicadorDeFirmaPendienteTest`, que valida el contenido con `SolicitudDeNotificaciones` de Notificaciones o con el esperado.

  **Resultado (2026-11-18):** `NotificacionDeFirmaPendiente`, `FirmaPendienteSolicitada` y `PublicadorDeFirmaPendiente`, con su bean. `PublicadorDeFirmaPendienteTest` (2) se vio fallar contra el esqueleto y pasa; compara con el contenido esperado, porque Firma no puede depender del módulo de Notificaciones.
- [X] T008 [US1] La llamada en `FIR/main/.../aplicacion/SubirFirmaServicio.java`, dentro de `transaccion.ejecutar`, después de registrar: solo si «`aRegistrar.aceptada()`» y «`siguienteTrasFirmar(orden)` existe»; el siguiente nunca es F1 (FR-006). Antes, ver fallar en `SubirFirmaServicioTest`:
  - F1 aceptada notifica a F2, con el autor y el momento de la firma;
  - F2 aceptada notifica a F3;
  - F3 aceptada no notifica, y sigue avisando la cadena completa;
  - una firma rechazada por la validación o ganada por otra subida no notifica;
  - una excepción al notificar revierte la transacción.
  
  `ArquitecturaTest` de Firma: Firma no depende de Notificaciones ni de Aprobación.

  **Resultado (2026-11-18):** `SubirFirmaServicio` recibe el puerto como último parámetro y notifica dentro de `transaccion.ejecutar`, con `siguienteTrasFirmar`. Se vieron fallar las 3 pruebas nuevas: F1 notifica a F2 dentro de la transacción, F2 a F3, y un fallo al notificar sale de la transacción. Las de F3, la validación rechazada y la ganada por otra subida verifican que no notifica. `ArquitecturaTest` suma la regla `sinAprobacionNiNotificaciones`.
- [X] T009 [US1] Notificaciones recibe a Firma:
  - `RecibirNotificacionesServicio.ORIGENES` suma `firma`;
  - `DestinatariosDesdeIdentidad.de(destino, periodo)` resuelve `PorUsuario` con `ListarUsuarios` (solo si existe y está activo) y `PorIdentificacion` con `ConsultarCuentaPorIdentificacion` (solo si está activa); sin cuenta, una lista vacía.
  
  Antes, ver fallar en `RecibirNotificacionesServicioTest` y en la prueba del adaptador:
  - el origen `firma` con el tipo `firma.NotificacionesSolicitadas`;
  - un usuario activo, uno inactivo y uno inexistente;
  - una identificación con cuenta activa, con cuenta inactiva y sin cuenta;
  - sin el autor.
  
  En `EventosNotificacionesContratoTest`, un evento de Firma con destino por identificación da 204, y con un token de usuario, 403 (FR-017).

  **Resultado (2026-11-18):** `ORIGENES` suma `firma`, y `DestinatariosDesdeIdentidad` resuelve las personas con `ListarUsuarios` y `ConsultarCuentaPorIdentificacion`, solo si la cuenta está activa. Se vieron fallar las 2 de `RecibirNotificacionesServicioTest` y las 2 de `DestinatariosDesdeIdentidadTest` (nueva). En `EventosNotificacionesContratoTest`, el evento de Firma da 204 y con un token de usuario 403; **esas dos no se vieron fallar**: se escribieron con el origen ya admitido. Notificaciones pasa `check` e `integrationTest`.
- [X] T010 [US1] `backend/apps/identidad/src/integrationTest/java/ec/mdd/NotificacionesDesdeFirmaIntegrationTest.java`, como `NotificacionesDesdeAprobacionIntegrationTest`, con Usuarios real:
  - un evento de Firma con la identificación de una cuenta deja la notificación en su bandeja, con el autor y su nombre;
  - con una identificación sin cuenta, el evento queda recibido y no hay notificación;
  - con una cuenta desactivada, tampoco.

  **Resultado (2026-11-18):** `NotificacionesDesdeFirmaIntegrationTest` (4): la cuenta activa de la identificación la recibe con el autor; sin cuenta y con la cuenta desactivada, el evento queda recibido y no hay notificación; un destino por usuario. Pasan en la primera corrida: **no se vieron fallar**, se escribieron con T009 hecha.
- [X] T011 [P] [US1] `SPA/navegacion/enlaces.test.ts`: `/firmas-pendientes`, el enlace de la notificación de firma pendiente, lo abren VICERRECTOR y RECTOR (`DISTRIBUTIVO:FIRMA`, D-16).


  **Resultado (2026-11-18):** `enlaces.test.ts` suma el Rector, con sus permisos de la V2 y la V9 de Roles, y la prueba de `/firmas-pendientes` para VICERRECTOR y RECTOR: 21 pruebas en verde.
**Checkpoint**: las notificaciones de firma pendiente llegan a la persona, en la API.

---

## Phase 4: User Story 2 - Ver la cadena de firmas y quién está en turno (Priority: P1)

**Goal**: «Consolidado del periodo» muestra cada paso de la cadena con su estado.

**Independent Test**: con F1 aceptada, el Vicerrector ve F1 firmada con la fecha, F2 en turno con su nombre y F3 pendiente.

- [X] T012 [US2] La cadena en la consulta:
  - `FIR/main/.../puertos/entrada/EstadoDelDocumento.java` suma `cadena`;
  - `DocumentoFirmableServicio.estado` la llena con `cadenaConEstado()` solo con GENERADO y sin anulación;
  - `DocumentoControlador` la mapea.
  
  Antes, ver fallar en `DocumentoFirmableServicioTest` y en la prueba de contrato del controlador los casos de `contracts/firma.md`: recién generado, F1 firmada, completo, rechazado en F2, con anulación, en generación, fallido y sin generar. Un coordinador sigue recibiendo 403.

  **Resultado (2026-11-18):** El puerto lleva su propio tipo, `EstadoDelDocumento.Paso`, en vez de la clase del dominio. `DocumentoFirmableServicio` llena la cadena solo sin anulación, y `DocumentoControlador` la mapea a `PasoConEstado`. Se vieron fallar la prueba del servicio (la cadena del documento generado) y las 2 de contrato con cadena («CADENA» y «RECHAZADO»). **Pasaban contra el esqueleto:** la del documento con las firmas anuladas, que espera la cadena vacía, y la del 403 de quien no lee el documento. La prueba de contrato compara la cadena como datos, sin depender del orden de los campos. Firma pasa `check`.
- [X] T013 [US2] `SPA/pantallas/comun/DocumentoFirmable.tsx`, según `contracts/spa.md`: la lista «Cadena de firmas» debajo de «Documento firmable generado», un elemento por paso con «F{orden} · {rótulo}», el firmante y el estado en texto («Firmado el {dd/mm/aa hh:mm}» en `America/Guayaquil`, «En turno», «Pendiente», «Rechazado»), y el paso en turno resaltado. Antes, ver fallar en `DocumentoFirmable.test.tsx`:
  - los cuatro estados;
  - el paso en turno resaltado, con su texto;
  - un paso sin firmante;
  - la cadena vacía, sin la lista;
  - el documento de otra versión, sin la lista;
  - al volver a montar la tarjeta, la consulta se repite y muestra la firma nueva (FR-014).
  
  No agregar `staleTime` a `useDocumentoFirmable`.

  **Resultado (2026-11-18):** `CadenaDeFirmas` en `DocumentoFirmable.tsx`: una lista con nombre «Cadena de firmas», el estado en el texto secundario y el paso en turno en negrita. Se vieron fallar 4 de 6 pruebas nuevas; **las de «sin cadena» y «otra versión» pasaban** porque el componente no mostraba ninguna lista. Los datos de prueba del documento suman `cadena: []`, también en `EstadoDelFlujo.test.tsx` y en `e2e/consolidado.spec.ts`. Vitest: 632.
- [X] T014 [US2] Playwright, en `frontend/e2e/`, con la API simulada: el Vicerrector abre «Consolidado del periodo» con F2 en turno, ve la cadena y la recorre con el teclado; axe-core (WCAG 2.1 AA) sin fallas (SC-006). Esperar a que termine la animación antes de axe, como en `notificaciones.spec.ts`.


  **Resultado (2026-11-18):** En `e2e/consolidado.spec.ts`, el Vicerrector ve los tres pasos con su estado, y axe-core no encuentra fallas. **Desviación:** la lista no tiene elementos interactivos, así que no hay nada que recorrer con Tab; un lector de pantalla la lee como lista con su nombre. Pasó en la primera corrida, porque se escribió con el componente hecho. Playwright: 133.
**Checkpoint**: la cadena se ve en la API y en la SPA.

---

## Phase 5: User Story 3 - Sin duplicados y sin pérdidas (Priority: P2)

**Goal**: la notificación de firma pendiente llega una sola vez, aunque `identidad` haya estado caído.

**Independent Test**: con `identidad` detenido, F1 se acepta sin error, y la notificación de F2 llega una sola vez cuando vuelve.

- [X] T015 [US3] En `backend/apps/distributivo/src/integrationTest/java/ec/mdd/`, como `NotificacionesPendientesIntegrationTest`:
  - con el destino `notificaciones` sin responder, una firma aceptada se confirma y su publicación queda pendiente (FR-008, FR-010);
  - si la transacción de la firma se revierte, no queda publicación (FR-007).
  
  La repetición la cubre `RecepcionRepetidaIntegrationTest` de F16-H01; sumarle un caso con un destino por identificación.


  **Resultado (2026-11-18):** `FirmaPendienteSinIdentidadIntegrationTest` (2): confirmada con `identidad` caído, la publicación queda `FAILED` sin error; revertida, no deja evento. `RecepcionRepetidaIntegrationTest` suma la misma solicitud de Firma dos veces. **No se vieron fallar:** la publicación y la recepción ya estaban hechas (T007, T009).
**Checkpoint**: las tres historias funcionan.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T016 `deploy/probar_documento_firmable.sh`, según `contracts/demostracion.md`:
  - la cadena después de generar (paso 2) y de cada firma (paso 11), como `vicerrectorado.demo`, y el 403 de `coordinacion.demo`;
  - el paso 19: `identidad` detenido durante la subida de F1, la vuelta marcada antes de levantarlo, y la notificación de F2 una sola vez antes de `ESPERA_REENVIO`; sin caída con `MDD_CONSERVAR`;
  - en `esperadas()`, las dos notificaciones de firma pendiente, y su detalle y autor aparte;
  - `MDD_CONSERVAR=F2`, que se detiene con F1 aceptada.
  
  Actualizar la cabecera del script y el comentario de `.github/workflows/perfil-consolidado.yml`. shellcheck sin hallazgos.

  **Resultado (2026-11-18):** `cadena_es` y `firmado_en` revisan la cadena en el paso 2 (con el 403 de `coordinacion.demo`) y después de cada firma. F1 se sube con `identidad` detenido y el token ya pedido, y el paso 19 comprueba que la notificación de F2 llegó una vez, con su detalle y su autor. `esperadas()` suma las dos notificaciones de firma pendiente. `MDD_CONSERVAR=F2` se detiene con F1 aceptada. shellcheck sin hallazgos; la cabecera y el comentario del workflow, al día.

  La primera corrida falló el paso 11 y destapó un error real, como `leidaEn` en F16-H01: la subida respondía `subidoEn` con nanosegundos, y PostgreSQL lo **redondea** a microsegundos al guardarlo, así que la cadena mostraba otra hora (`.609956638` contra `.609957`). Ahora `SubirFirmaServicio` trunca el momento a microsegundos, con una prueba que se vio fallar antes. Con eso, 20 de 20; la notificación de F2 llegó 32 s después de levantar `identidad`.
- [X] T017 `deploy/probar_rechazo_en_firma.sh`, según `contracts/demostracion.md`: la cadena en los pasos 2, 6 y 8. shellcheck sin hallazgos.

  **Resultado (2026-11-18):** La cadena en los pasos 2 (F1 firmada y F2 en turno), 6 (vacía, con las firmas de la versión 1 anuladas) y 8 (la versión 2 desde F1 en turno). Pasa completo. shellcheck sin hallazgos.
- [X] T018 Correr los scripts de `perfil-consolidado`, en su orden, sobre un perfil construido desde la rama (SC-001 a SC-003). Después, el recorrido a mano de `quickstart.md` §3, del autor (SC-004 y SC-005).

  **Resultado (2026-11-18):** **Avance:** los 36 scripts del workflow corrieron sobre un perfil construido desde la rama; 35 pasaron a la primera, y `probar_documento_firmable.sh` falló el paso 11 por la precisión de `subidoEn` (ver T016). Con la corrección, los dos scripts de la firma pasan completos. **Falta el recorrido a mano del autor** (`MDD_CONSERVAR=F2`).

  **Recorrido a mano (2026-11-18):** el autor lo hizo con `MDD_CONSERVAR=F2`, sobre un perfil limpio:
  - `vicerrectorado.demo` tuvo «Tienes una firma pendiente: «Revisado por»», con el detalle de F1 y «Docente (demo)» como autor;
  - al abrirla llegó a «Pendientes de firma», con el paso «Revisado por» de 2026-I;
  - en «Consolidado del periodo», la cadena: F1 firmada con su fecha, F2 en turno y en negrita, F3 pendiente, con los nombres impresos;
  - `consolidacion.demo` vio la misma cadena; `coordinacion.demo` no tiene la pantalla;
  - `docente.demo` solo tiene «El distributivo llegó a «En firma»».
  
  Sin hallazgos. El perfil se bajó con `down -v`.
- [X] T019 Correr completos, y medir la cobertura de Firma, Notificaciones y Usuarios (60 %, D-32):
  - `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --continue`;
  - en la SPA: tipos, ESLint, Prettier, Vitest y Playwright;
  - Gitleaks sobre `main..HEAD`, después del commit.

  **Resultado (2026-11-18):**
  - **Backend:** 3 343 pruebas unitarias y 353 de integración. La única falla de la corrida completa fue `ConfirmarLasDiezCargasIntegrationTest`, la medición de tiempo intermitente de siempre, en `datos`, que esta rama no toca: pasa sola.
  - **Cobertura (60 %):** Firma 81,8 %, Notificaciones 78,9 % y Usuarios 84,8 %.
  - **SPA:** tipos, ESLint y Prettier en verde; Vitest 632 y Playwright 133.
  - **Gitleaks:** queda para después del commit (`main..HEAD`).
- [X] T020 [P] Revisar el JavaDoc:
  - por qué publica Firma;
  - por qué el destino es la persona y se resuelve en `identidad`;
  - por qué no hay respaldo por rol;
  - por qué la identificación no se guarda ni se repite en los errores;
  - la regla del estado de cada paso.

  **Resultado (2026-11-18):** Dicen lo pedido:
  - por qué publica Firma: `NotificacionDeFirmaPendiente` y `PublicadorDeFirmaPendiente`;
  - por qué el destino es la persona y se resuelve en `identidad`: `PublicadorDeFirmaPendiente` y `DestinoDelMensaje`;
  - por qué no hay respaldo por rol: `RecibirNotificacionesServicio` y `DestinatariosDesdeIdentidad`;
  - por qué la identificación no se guarda ni se repite: `DestinoDelMensaje.PorIdentificacion` y `SolicitudDeNotificaciones`;
  - la regla del estado de cada paso: `FirmasDelDocumento`.
  
  Se corrigió el JavaDoc de `ORIGENES`, que todavía decía «F16-H02 sumará `firma`».
- [X] T021 Crear `specs/054-siguiente-firmante/evidencia.md`, como la de 053.
- [ ] T022 Fusionar solo con la aprobación del autor y todas las comprobaciones en verde.
  - Verificar la demo en solo lectura (`quickstart.md` §4): «Sano» y la memoria de `identidad` (D-109).
  - Abrir el PR de evidencia con `Closes #68`.
  - Con F16-H02, la épica F16 (#15) queda sin historias abiertas: preguntarle al autor si la cierra.

---

## Dependencies & Execution Order

- **Setup:** T001, hecha.
- **Fundacional:** de T002 a T006, en paralelo. T005 usa los patrones de T002.
- **US1:**
  - T007 después de T002;
  - T008 después de T006 y T007;
  - T009 después de T004 y T005;
  - T010 después de T009;
  - T011 sin dependencias.
- **US2:**
  - T012 después de T003 y T006;
  - T013 después de T003;
  - T014 después de T013.
- **US3:** T015 después de T008.
- **Cierre:**
  - T016 y T017 después de las tres historias;
  - T018 después de T016 y T017;
  - T019 y T020 al final;
  - T021 después de T018 y T019;
  - T022 al final.

US1 y US2 son independientes entre sí: comparten solo el dominio de Firma de T006.

### Parallel Opportunities

```text
T002 a T006.
T007 (Firma) en paralelo con T009 (Notificaciones) y T011 (SPA).
T012 (Firma) en paralelo con T013 (SPA).
T020 en paralelo con T018 y T019.
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. La fundación: T002 a T006.
2. US1: T007 a T011. Con eso, el siguiente firmante ya recibe su notificación, aunque la cadena todavía no se vea.
3. Validar con un evento de Firma real en el proceso `identidad` (T010).

### Incremental Delivery

1. US1, la notificación.
2. US2, la cadena en la SPA.
3. US3, la caída y la repetición.
4. El cierre: los scripts, el recorrido del autor, la evidencia y la fusión.

Una sola rama y un solo PR, como en las historias anteriores. El PR principal lleva `Refs #68`, y el de evidencia, `Closes #68`.
