---
description: "Tareas de F18-H02: iniciar una reforma de la versión publicada"
---

# Tasks: Iniciar una reforma de la versión publicada

**Input**: documentos de diseño en `specs/056-reforma-version-publicada/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si no compila, primero va un esqueleto.

**Rutas**: `OCS/` es `backend/servicios/validacionocs/src/`, `GEN/` es `backend/servicios/generacion/src/` y `SPA/` es `frontend/src/`. `.../` es el paquete `ec/mdd/validacionocs/` o `ec/mdd/generacion/`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede hacer en paralelo (otro archivo, sin depender de una tarea pendiente).
- **[Story]**: US1 (iniciar la reforma), US2 (la publicada sigue vigente) o US3 (la notificación).

---

## Phase 1: Setup

- [X] T001 Escribir **D-113** en `docs/decisiones.md`, en la fila del índice y en su sección (research §1 a §10). Revisar en el glosario «Reforma», `iniciarReforma()` y el motivo «reforma» de `VersionDistributivo`.

  **Resultado (2026-11-20):** quedó escrita con el plan.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T005 y T006.

- [X] T002 [P] El contrato en `contratos/validacionocs/openapi.yaml`, según `contracts/validacionocs.md`:
  - `iniciarReforma`: `POST /api/validacionocs/periodos/{periodo}/reforma`, con `SolicitudDeReforma` («`motivo` string, `maxLength: 500`, obligatorio», `additionalProperties: false`), `x-mdd-permiso: DISTRIBUTIVO:APROBACION`, con 200 (`EstadoDelFlujo`), 400, 401, 403, 404, 409, 500, 502 y 504 (el caso de uso consulta el estado del periodo en Académico, como la publicación);
  - `AccionDelFlujo` suma `INICIAR_REFORMA`, y el tipo de `TransicionDelFlujo` suma `REFORMA`;
  - `EstadoDelFlujo.reforma`, opcional, `ReformaDelDistributivo` con «`versionPublicada` ≥ 1», «`version` ≥ 1», «`motivo` string, `maxLength: 500`», «`iniciadaPor`» y «`iniciadaEn` date-time», `additionalProperties: false`.

  Regenerar los tipos de la SPA. Esqueleto: el método en `OCS/main/.../adaptadores/entrada/FlujoControlador.java`, que lanza hasta T009.

  **Resultado (2026-11-20):** `iniciarReforma` con `SolicitudDeReforma` («`motivo`, `maxLength: 500`»), `INICIAR_REFORMA`, `REFORMA` en `TransicionDelFlujo` y `ReformaDelDistributivo` en el contrato, con 500, 502 y 504. Tipos de la SPA regenerados. Esqueleto en `FlujoControlador` hasta T009.
- [X] T003 [P] Los dos códigos, en `contratos/comun/errores.md` con sus 8 columnas y en `OCS/main/.../adaptadores/entrada/ErroresValidacionocs.java`:
  - `VALIDACIONOCS_ETAPA_NO_ADMITE_REFORMA` (409, Negocio, «Etapa sin reforma», «Solo se reforma un distributivo publicado.»), con `OCS/main/.../dominio/EtapaNoAdmiteReforma.java`;
  - `VALIDACIONOCS_MOTIVO_DE_REFORMA_INVALIDO` (400, Validación, «Motivo no válido», «El motivo de la reforma tiene de 10 a 500 caracteres.»), con `OCS/main/.../dominio/MotivoDeReformaInvalido.java`.

  Antes, ver fallar `ErroresValidacionocsTest`.

  **Resultado (2026-11-20):** `EtapaNoAdmiteReforma` y `MotivoDeReformaInvalido`, con sus entradas en `ErroresValidacionocs` y `errores.md`. `ErroresValidacionocsTest` se vio fallar antes y pasa.
- [X] T004 [P] Las migraciones, con su prueba de esquema vista fallar antes:
  - `OCS/main/resources/db/migration/validacionocs/V8__reforma.sql`: el `check` de `transicion.tipo` suma `REFORMA`; la tabla `reforma` con «`transicion_id` clave y foránea a `transicion`, en cascada», «`periodo`», «`version_publicada` ≥ 1» y «`motivo varchar(500)`, de 10 a 500 sin los extremos». Prueba: `ValidacionocsEsquemaIntegrationTest`.
  - `GEN/main/resources/db/migration/generacion/V9__reforma.sql`: el `check` de `version_distributivo.motivo` suma `REFORMA`. Prueba: `GeneracionEsquemaIntegrationTest`.

  **Resultado (2026-11-20):** `V8__reforma.sql` (Validación OCS) y `V9__reforma.sql` (Generación). Se vieron fallar antes `ValidacionocsEsquemaIntegrationTest` (+1: la reforma con su motivo, el motivo corto y la versión 0 rechazados) y `GeneracionEsquemaIntegrationTest` (+1: el motivo `REFORMA` admitido y otro rechazado); pasan.
- [X] T005 El agregado, según `data-model.md`. Antes, ver fallar en `OCS/test/.../dominio/ProcesoAprobacionTest.java`:
  - `OCS/main/.../dominio/Reforma.java` («`versionPublicada` ≥ 1», «`motivo` `Observacion`: de 10 a 500 caracteres sin los extremos»); `Transicion.reforma`, presente solo con el tipo `REFORMA`; `TipoDeTransicion.REFORMA` y `AccionDelProceso.INICIAR_REFORMA`;
  - `validarReforma(quien)` e `iniciarReforma(quien, motivo, nueva, hash, ahora)` en PUBLICADO, con el rol de EN_REVISION_VRA: pasa a EN_CONSOLIDACION y devuelve la `REFORMA` con la versión nueva, su hash, quién, cuándo, el motivo y la versión publicada;
  - con EN_CONSOLIDACION desactivada, a la siguiente etapa activa;
  - en cada otra etapa, `EtapaNoAdmiteReforma` sin cambiar nada; sin el rol (por ejemplo la Secretaría o el Rector), `RolDeOtraEtapa`; con otro rol en EN_REVISION_VRA de la configuración, lo usa;
  - `reformaEnCurso()`: vacía antes, la reforma después, vacía otra vez tras una publicación posterior;
  - `publicacion()` sigue siendo la anterior durante la reforma, y la nueva después de publicarla;
  - `acciones`: en PUBLICADO, `[INICIAR_REFORMA]` para el rol de EN_REVISION_VRA y ninguna para los demás;
  - una reforma de una reforma: publicada la versión 2, reformarla crea la 3, y `publicacion()` sigue siendo la 2 durante la reforma.

  **Para seguir compilando:** los `switch` exhaustivos sobre `TipoDeTransicion` (`destinoDe`, `MensajesDeTransicion` y `FlujoDelDistributivoServicio.cambio`) suman un caso `REFORMA` provisional que lanza `UnsupportedOperationException`, hasta T008 (`cambio`) y T014 (`destinoDe` y los mensajes).

  **Resultado (2026-11-20):** `Reforma`, `Transicion.reforma` (con un constructor de 11 argumentos que la deja vacía), `TipoDeTransicion.REFORMA`, `AccionDelProceso.INICIAR_REFORMA`, y en `ProcesoAprobacion`: `validarReforma`, `iniciarReforma`, `reformaEnCurso` y la acción en PUBLICADO. Se vieron fallar las 16 pruebas nuevas de `ProcesoAprobacionTest` contra el esqueleto (las 8 etapas, el rol, la configuración con otro rol, EN_CONSOLIDACION desactivada, la reforma de una reforma y las acciones) y pasan. Casos `REFORMA` provisionales en `destinoDe` y `MensajesDeTransicion`; `FlujoDelDistributivoServicio.cambio` no tiene un `switch` sobre el tipo, así que no hizo falta ahí.
- [X] T006 Lo que F18-H01 probó como «en PUBLICADO, ninguna acción», en el mismo cambio que T005: actualizar `FlujoDelDistributivoServicioTest` y `FlujoContratoTest` para que el Vicerrector tenga `INICIAR_REFORMA` y nadie más (D-113).


  **Resultado (2026-11-20):** `ProcesoAprobacionTest` (la publicación), `FlujoDelDistributivoServicioTest` y `FlujoContratoTest`: en PUBLICADO, el Vicerrector tiene `INICIAR_REFORMA` y nadie más. Se vieron fallar con T005 y pasan. `PermisosValidacionocsTest` también: cuenta 12 operaciones y exige `DISTRIBUTIVO:APROBACION` en `/reforma`. Validación OCS pasa completo.
**Checkpoint**: el contrato, los códigos, las migraciones y la regla, en verde.

---

## Phase 3: User Story 1 - El Vicerrector inicia una reforma (Priority: P1) 🎯 MVP

**Goal**: el Vicerrector inicia una reforma de un distributivo publicado, con su motivo, y el distributivo vuelve a EN_CONSOLIDACION con una versión nueva marcada como reforma.

**Independent Test**: con PUBLICADO, `vicerrectorado.demo` inicia la reforma y el distributivo queda en EN_CONSOLIDACION con la versión 2; en Generación, la 2 es EDITABLE con motivo `REFORMA` y la 1 sigue CONGELADA.

- [X] T007 [US1] Generación, según `contracts/generacion.md`. Antes, ver fallar en `GEN/test/.../aplicacion/ParcialesDelDistributivoServicioTest.java` y `GEN/integrationTest/.../VersionNuevaIntegrationTest.java`:
  - `MotivoDeVersion.REFORMA`, y `quien` en `crearVersionNueva(periodo, version, motivo, quien, momento)` (`sistema` en el rechazo);
  - con `REFORMA`, la anterior sigue CONGELADA y la nueva es EDITABLE, con todo copiado, el motivo, quién y cuándo (`GEN/main/.../adaptadores/salida/RepositorioDistributivosJdbc.java`);
  - el reintento devuelve la misma: «la vigente es la siguiente, con motivo `REFORMA`, y la anterior sigue CONGELADA»; si no, vacío;
  - en PostgreSQL, dos pedidos a la vez dejan una sola versión;
  - un rechazo en firma durante la reforma: con la 1 CONGELADA y la 2 de la reforma rechazada, la 2 queda ANULADA, se crea la 3 y la 1 sigue CONGELADA.

  El rechazo en firma sigue igual: sus pruebas no cambian.

  **Resultado (2026-11-20):** `MotivoDeVersion.REFORMA`, `quien` en `crearVersionNueva` (el rechazo pasa `sistema`), `RepositorioDistributivos.motivoDe`, y el repositorio no anula la anterior con `REFORMA`. Se vieron fallar 2 de las 3 unitarias nuevas (el reintento y los casos que no corresponden; la de crear pasaba con solo pasar `quien`) y las 3 de `VersionNuevaIntegrationTest` (la copia, dos a la vez y el rechazo durante la reforma). **Error encontrado:** con dos reformas a la vez, la segunda esperaba el bloqueo de la fila de la versión 1 y, al soltarse, seguía con esa misma fila, todavía CONGELADA, e intentaba crear otra vez la versión 2 (la restricción única lo frenaba con un error). Ahora `bloquearVigente` bloquea antes la fila del distributivo y lee la vigente en otra sentencia. En el rechazo no se notaba porque la anterior ya estaba ANULADA. Generación pasa completo.
- [X] T008 [US1] El caso de uso. Antes, ver fallar en `OCS/test/.../aplicacion/IniciarReformaServicioTest.java`:
  - `OCS/main/.../puertos/entrada/IniciarReforma.java` e `OCS/main/.../aplicacion/IniciarReformaServicio.java`:
    1. el motivo (`MotivoDeReformaInvalido` si no tiene de 10 a 500 caracteres), el periodo, el distributivo y `validarReforma`, sin cambiar nada;
    2. `Distributivo.crearVersionNueva(periodo, publicada, REFORMA, quien, ahora)`; vacío es un error técnico;
    3. con el proceso bloqueado: `iniciarReforma`, guardar, notificar y registrar `REFORMA_INICIADA`.
  - Un rechazo en el paso 1 no pide la versión; un reintento con la versión ya creada no crea otra; la segunda de dos reformas recibe `EtapaNoAdmiteReforma` en el paso 3.
  - El puerto de salida `Distributivo` y `OCS/main/.../adaptadores/salida/DistributivoDesdeGeneracion.java` pasan el motivo y quién; el rechazo en firma pasa `RECHAZO_EN_FIRMA` y `sistema`.
  - `CambioDeFlujo.Accion.REFORMA_INICIADA` y `FlujoDelDistributivoServicio.cambio`: antes `{estado: PUBLICADO}`, después la llegada, y en el detalle `periodo`, `versionPublicada`, `version`, `hash` y `motivo`.
  - El repositorio del proceso guarda y lee la tabla `reforma`, con su prueba en `OCS/integrationTest/.../RepositorioProcesoIntegrationTest.java` (+2: guardar y leer; dos reformas a la vez dejan una transición).
  - El bean en `OCS/main/.../infraestructura/ConfiguracionValidacionocs.java`.

  **Resultado (2026-11-20):** `IniciarReforma`, `IniciarReformaServicio` con su bean, `CambioDeFlujo.Accion.REFORMA_INICIADA` y su caso en `FlujoDelDistributivoServicio.cambio`, y la tabla `reforma` en `RepositorioProcesoJdbc`. Se vieron fallar las 7 de `IniciarReformaServicioTest` contra el esqueleto y las 2 nuevas de `RepositorioProcesoIntegrationTest`; pasan. **Desvío:** el puerto de salida `Distributivo` suma un método aparte, `crearVersionDeReforma(periodo, publicada, quien, momento)`, en vez de pasarle el motivo a `crearVersionNueva`: Validación OCS no usa el enum de Generación en su puerto, y así el rechazo en firma no cambia. Los dobles de `Distributivo` de cuatro pruebas de integración y de `FlujoEnMemoria` suman el método. Validación OCS pasa completo, también `ArquitecturaTest`.
- [X] T009 [US1] El controlador. Antes, ver fallar en `OCS/test/.../adaptadores/entrada/ReformaContratoTest.java` (nueva), `FlujoContratoTest.java` (+2) y `PermisosValidacionocsTest.java`:
  - `FlujoControlador` implementa `iniciarReforma` con `@PreAuthorize` de `DISTRIBUTIVO:APROBACION`, y mapea `reforma`;
  - el 200, cada error con su código, el 403 sin permiso sin llamar al caso de uso, el 400 del cuerpo y del periodo mal formados;
  - `FlujoEnMemoria` y los dobles de las otras pruebas del controlador suman el caso de uso.

  **Resultado (2026-11-20):** `FlujoControlador.iniciarReforma` con `@PreAuthorize` de `DISTRIBUTIVO:APROBACION`, y el mapeo de `reforma`. `ReformaContratoTest` (4) y `FlujoContratoTest` (+2, de punta a punta con `FlujoEnMemoria`, que ahora lleva la versión por periodo). Se vieron fallar contra el esqueleto el 200, los rechazos del caso de uso, los cuerpos mal formados y los dos de punta a punta; **el 403 sin permiso pasaba**, porque se comprueba antes del controlador. Los dobles de las otras pruebas del controlador suman `IniciarReforma`. **Desvío:** `FlujoDelDistributivo.reforma` y `ReformaEnCurso` se adelantaron de T012, porque la prueba de contrato los necesita. **Cambio en la prueba:** no hay un caso con una propiedad desconocida en el cuerpo: Jackson no la rechaza en este proyecto, como en las demás operaciones. Validación OCS pasa completo.
- [X] T010 [P] [US1] La SPA, según `contracts/spa.md`. Antes, ver fallar en `SPA/pantallas/comun/EstadoDelFlujo.test.tsx`:
  - `SPA/pantallas/comun/flujo.ts` (`iniciarReforma`) y `SPA/pantallas/comun/DialogoDeReforma.tsx`: el texto, el campo «Motivo de la reforma» con su contador, «de 10 a 500 caracteres», el foco en «Cancelar», el error en el diálogo;
  - en `EstadoDelFlujo.tsx`, «Iniciar una reforma» solo con la acción `INICIAR_REFORMA`, y recargar al iniciar.

  **Resultado (2026-11-20):** `flujo.iniciarReforma`, `DialogoDeReforma` (el texto, «Motivo de la reforma» con su contador, «Iniciar la reforma» desactivado hasta que el motivo tenga de 10 a 500 caracteres, el foco en «Cancelar», el error en el diálogo) y el botón «Iniciar una reforma» en `EstadoDelFlujo`. Se vieron fallar el diálogo con el `POST` y el 409; **«sin la acción no hay botón» pasaba** contra el componente anterior. `tipos` y `lint` limpios.
- [X] T011 [US1] Playwright, en `frontend/e2e/consolidado.spec.ts`: el Vicerrector inicia la reforma con el teclado y ve la reforma en curso; axe-core (WCAG 2.1 AA) sin fallas con el diálogo abierto y después (SC-006).


  **Resultado (2026-11-20):** `consolidado.spec.ts` (+1, Chromium): el Vicerrector abre el diálogo con el teclado, el foco empieza en «Cancelar», llega al motivo con Mayús+Tab, lo escribe y confirma; ve la versión 2, la reforma en curso con el motivo y «Vigente hasta que se publique la reforma.»; el `POST` lleva el motivo; axe sin fallas con el diálogo abierto y después. Pasa en la primera corrida: **no se vio fallar**, porque la SPA ya estaba hecha (T010).
**Checkpoint**: el Vicerrector inicia la reforma y el distributivo vuelve a consolidación con la versión 2.

---

## Phase 4: User Story 2 - La versión publicada anterior queda como histórica (Priority: P1)

**Goal**: mientras dura la reforma, cualquiera ve la reforma en curso y la versión publicada anterior, que sigue vigente; al publicarse la de la reforma, la anterior queda histórica sin perder nada.

**Independent Test**: con la reforma iniciada, el estado del flujo tiene `reforma` y la `publicacion` de la versión 1; después de publicar la 2, `publicacion` es la 2 y ya no hay `reforma`.

- [X] T012 [US2] `FlujoDelDistributivo.reforma` y la vista en `OCS/main/.../aplicacion/FlujoDelDistributivoServicio.java`. Antes, ver fallar en `FlujoDelDistributivoServicioTest.java`:
  - `reforma` desde `reformaEnCurso()`, con la versión publicada, la nueva, el motivo, quién y cuándo;
  - `publicacion` sigue siendo la última publicación;
  - la resolución solo si es de la versión de trabajo, como la congelación;
  - la versión 1 publicada y reformada, y después la 2 publicada: `publicacion` es la 2, sin `reforma`.

  **Resultado (2026-11-20):** `FlujoDelDistributivoServicio` llena `reforma` desde `reformaEnCurso()` y muestra la resolución solo si es de la versión de trabajo. Se vio fallar la prueba de la reforma en curso de `FlujoDelDistributivoServicioTest` (+2); la de «publicado sin reforma» pasaba, porque solo fija lo de antes. **Cambio respecto de la tarea:** la congelación no se filtra en el servicio, porque el rechazo en firma depende de recibir la anterior; la SPA ya la muestra solo si es de la versión de trabajo. El caso «publicada la versión 2, sin `reforma`» lo prueba `ProcesoAprobacionTest` (`reformaEnCurso` vacía después de una publicación).
- [X] T013 [P] [US2] La SPA. Antes, ver fallar en `EstadoDelFlujo.test.tsx`: la constancia «Reforma de la versión {versionPublicada} en curso: versión {version}, iniciada por {iniciadaPor} el {dd/mm/aa hh:mm}», con el motivo, y «Vigente hasta que se publique la reforma.» en la constancia de la publicación.


  **Resultado (2026-11-20):** `ConstanciaDeLaReforma` y «Vigente hasta que se publique la reforma.» en la constancia de la publicación. Se vio fallar su prueba en `EstadoDelFlujo.test.tsx` y pasa; las 38 del archivo pasan.
**Checkpoint**: la reforma en curso y la publicada vigente, para todos.

---

## Phase 5: User Story 3 - La notificación de la reforma (Priority: P2)

**Goal**: quien consolida, los docentes, Talento Humano y las coordinaciones reciben la notificación de la reforma, una vez cada uno, y nunca quien la inició.

**Independent Test**: después de la reforma, cada cuenta activa de esos roles tiene una notificación «Se inició una reforma del distributivo de 2026-I»; el Vicerrector, ninguna.

- [X] T014 [P] [US3] Los destinos y el mensaje en `OCS/main/.../dominio/ProcesoAprobacion.java` (`destinoDe`) y `MensajesDeTransicion.java`, según `data-model.md`:
  - la `REFORMA` va al rol de la etapa de llegada y a DOCENTE, OPERADOR_DATOS y COORDINADOR_CARRERA, sin carrera;
  - título «Se inició una reforma del distributivo de {periodo}», y el detalle con la versión nueva, la publicada, el motivo y que la publicada sigue vigente;
  - el enlace, por rol: `/consolidado` para el de la llegada, `/oferta` para COORDINADOR_CARRERA y `/` para DOCENTE y OPERADOR_DATOS.

  Antes, ver fallar en `MensajesDeTransicionTest` y `ProcesoAprobacionTest`.

  **Resultado (2026-11-20):** `destinoDe` da, para la `REFORMA`, el rol de la etapa de llegada y DOCENTE, OPERADOR_DATOS y COORDINADOR_CARRERA, sin carrera y sin repetidos; `MensajesDeTransicion` da el título, el detalle y el enlace por rol (`/consolidado`, `/oferta` o `/`). Se vieron fallar las pruebas nuevas de `MensajesDeTransicionTest` y `ProcesoAprobacionTest`; pasan, y ya no quedan casos provisionales. Validación OCS pasa completo.
- [X] T015 [P] [US3] `SPA/navegacion/enlaces.test.ts`: los enlaces de la reforma, cada uno contra los permisos de su rol.

  **Resultado (2026-11-20):** `enlaces.test.ts` (+4): el Responsable abre `/consolidado`, la coordinación `/oferta`, y los docentes y Talento Humano el inicio, cada uno con sus permisos. **No se vio fallar:** las rutas y los permisos ya existían; la prueba fija la tabla igual a `MensajesDeTransicion`.
- [X] T016 [US3] `backend/apps/identidad/src/integrationTest/java/ec/mdd/NotificacionesDesdeAprobacionIntegrationTest.java` (+1): con los destinos de la reforma, el Responsable, quien tiene DOCENTE y COORDINADOR_CARRERA y el operador de datos reciben una cada uno, y el autor no.


  **Resultado (2026-11-20):** `NotificacionesDesdeAprobacionIntegrationTest` (+1): con los cuatro destinos de la reforma, el Responsable, quien tiene DOCENTE y COORDINADOR_CARRERA y el operador de datos reciben una cada uno, y el autor ninguna. Pasa en la primera corrida: **no se vio fallar**, porque Notificaciones no cambia.
**Checkpoint**: la reforma notifica a quien corresponde.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T017 `deploy/probar_documento_firmable.sh`, según `contracts/demostracion.md`:
  - en APROBADO (paso 15), el 409 de la reforma;
  - en el paso 17, `INICIAR_REFORMA` solo para `vicerrectorado.demo`;
  - el paso 22, iniciar la reforma: los 403 de `secretaria.demo` y `rectorado.demo`, los 400 sin motivo y con uno corto, la reforma de `vicerrectorado.demo` con `identidad` detenido y el token ya pedido, la versión 2 EDITABLE con motivo `REFORMA` y la 1 CONGELADA, la bitácora (esperando hasta `ESPERA_REENVIO`) y el 409 de la segunda;
  - el paso 23, la publicada vigente y la reforma en curso en el estado del flujo;
  - el paso 24, la notificación: una vez para cada cuenta activa de los roles, la de `docente.demo` antes de 120 s desde la vuelta, ninguna para `vicerrectorado.demo`;
  - los pasos 25 y 26, el ciclo de la versión 2 hasta publicarla, generalizando por versión los ayudantes de las firmas y la resolución;
  - el paso 27, la versión 1 histórica: CONGELADA, sus tres firmas aceptadas, su documento y su resolución, sin anulación;
  - `MDD_CONSERVAR=PUBLICADO`.

  Actualizar la cabecera del script y el comentario de `.github/workflows/perfil-consolidado.yml`. shellcheck sin hallazgos.

  **Resultado (2026-11-20):** el 409 de la reforma en APROBADO (paso 15); `INICIAR_REFORMA` solo para `vicerrectorado.demo` en el paso 17; los pasos 22 a 27, con la reforma con `identidad` detenido, la bitácora esperando hasta `ESPERA_REENVIO`, las notificaciones por usuario, el ciclo de la versión 2 (devolver la primera carrera, reenviarla, consolidar, adjuntar otro informe, justificar las blandas, revisión, aprobación, el documento desde F1, las tres firmas, la resolución `RPC-SO-12-No.124-2026` y la publicación) y la versión 1 histórica; `MDD_CONSERVAR=PUBLICADO`. `id_del_documento` toma el de la versión más alta. Las firmas de la versión 2 usan un ayudante propio, sin los rechazos ni la caída de F1. Cabecera y comentario del workflow actualizados; shellcheck sin hallazgos. Falta correrlo (T018).
- [X] T018 Correr los scripts de `perfil-consolidado`, en su orden, sobre un perfil construido desde la rama (SC-001 a SC-004). Medir cuánto agrega al workflow (límite de 45 minutos). Después, el recorrido a mano de `quickstart.md` §3, del autor (SC-005).

  **Resultado parcial (2026-11-20):** los 36 scripts, en su orden, sobre un perfil construido desde la rama: pasan todos en la primera corrida, también los pasos 22 a 27; la notificación de la reforma llegó 28 s después de la vuelta de `identidad`. `probar_documento_firmable.sh` tardó 188 s y los 36, 872 s (unos 15 minutos; el workflow tiene 45). **Error del modo nuevo:** `MDD_CONSERVAR=PUBLICADO` se detenía después del paso 21, que en ese modo falla porque F1 se sube sin la caída de `identidad`; ahora se detiene después del 17 (los pasos 18 a 21 solo revisan notificaciones) y, sobre un perfil limpio, termina con 0 en PUBLICADO. El cambio no toca la corrida normal. Recorrido del autor (quickstart §3, 2026-11-20): `vicerrectorado.demo` inició la reforma desde «Consolidado del periodo» con el diálogo (el texto, el contador, el foco) y la tarjeta pasó a «Consolidación · versión 2» con la reforma en curso, el motivo y la versión 1 vigente; `consolidacion.demo` y `docente.demo` (el designado) pueden devolver parciales y enviar a revisión; la oferta de la coordinación muestra la misma tarjeta; la campana del Vicerrector no tiene la reforma. En la base, una notificación por usuario: `consolidacion.demo` y `docente.demo` a `/consolidado`, `coordinacion.demo` a `/oferta`, `datos.demo` al inicio. **Observación:** `docente.coordinador.demo` recibe una sola, con el enlace al inicio y no a la oferta: Notificaciones guarda la primera de las dos, y la de DOCENTE llega antes; pasa lo mismo con la publicación de F18-H01.
- [X] T019 Correr completos, y medir la cobertura de Validación OCS y Generación (60 %, D-32):
  - `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --continue`, sin Vitest corriendo a la vez;
  - en la SPA: tipos, ESLint, Prettier, Vitest y Playwright;
  - Gitleaks sobre `main..HEAD`, después del commit.

  **Resultado parcial (2026-11-20):** el build completo dio 3 409 pruebas unitarias y 368 de integración, con dos fallas en módulos que esta rama no toca: la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest` (`datos`) y `EntregaIntegrationTest` (`plataforma:eventos`); repetidas solas con `--rerun-tasks`, pasan. Cobertura de instrucciones: Validación OCS 85 % y Generación 91 % (mínimo 60 %, D-32), con su verificación en verde. SPA: tipos, ESLint y Prettier sin hallazgos; Vitest 649/649; Playwright 135/135. Gitleaks (v8.30.1) sobre `main..HEAD`, después del commit: 8 commits, sin filtraciones.
- [X] T020 [P] Revisar el JavaDoc: por qué la publicada no se anula, cuál es la vigente, el orden con Generación y a quién se notifica.

  **Resultado (2026-11-20):** `ProcesoAprobacion.iniciarReforma` y `Reforma` dicen por qué la publicada no se anula y que sigue vigente; `IniciarReformaServicio`, el orden con Generación y el reintento; `destinoDe`, a quién se notifica; `MensajesDeTransicion`, el enlace por rol. Se puso al día la doc de `ParcialesDelDistributivo.crearVersionNueva` y de `RepositorioDistributivos.crearVersionNueva` (ya no siempre anula) y se explicó el bloqueo del distributivo en `bloquearVigente`.
- [X] T021 Crear `specs/056-reforma-version-publicada/evidencia.md`, como la de 055.

  **Resultado (2026-11-20):** `evidencia.md`, con las pruebas, el script, el recorrido del autor, el comportamiento conocido de `docente.coordinador.demo`, las desviaciones y los criterios de terminado; la sección del equipo de demostración queda para después de fusionar.
- [X] T022 Fusionar solo con la aprobación del autor y todas las comprobaciones en verde.
  - Verificar la demo en solo lectura (`quickstart.md` §4): «Sano», las migraciones V8 y V9 aplicadas y la memoria de `identidad`.
  - Abrir el PR de evidencia con `Closes #72`.
  - Con #72 cerrada, cerrar la épica F18 (#17) si el autor lo aprueba.

  **Resultado (2026-11-20):** PR #251 fusionado (`71dd1b7`) con las 12 comprobaciones en verde. La demo tomó `71dd1b7`, «Sano en 56 s», con V8 de Validación OCS y V9 de Generación aplicadas e `identidad` en 488 de 768 MiB; 2026-I sigue en EN_CONSOLIDACION. PR de evidencia con `Closes #72`.

---

## Dependencies & Execution Order

- **Setup:** T001, hecha.
- **Fundacional:** T002 a T004 en paralelo; T005 después de T003, y T006 en el mismo cambio que T005.
- **US1:**
  - T007 después de T004;
  - T008 después de T005 y T007;
  - T009 después de T002 y T008;
  - T010 después de T002;
  - T011 después de T009 y T010.
- **US2:**
  - T012 después de T008;
  - T013 después de T010.
- **US3:**
  - T014 después de T005;
  - T015 sin dependencias;
  - T016 después de T014.
- **Cierre:**
  - T017 después de las tres historias;
  - T018 después de T017;
  - T019 y T020 al final;
  - T021 después de T018 y T019;
  - T022 al final.

### Parallel Opportunities

```text
T002 a T004.
T007 (Generación) en paralelo con T014 (los destinos) una vez hecho T005.
T010 y T013 (SPA) en paralelo con T007 a T009 (backend).
T015 en cualquier momento.
T020 en paralelo con T018 y T019.
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. La fundación: T002 a T006.
2. US1: T007 a T011. Con eso, el Vicerrector ya inicia la reforma, y la versión 2 vuelve al ciclo.
3. Validar con el contrato, Generación en PostgreSQL y la prueba de dos reformas a la vez.

### Incremental Delivery

1. US1, iniciar la reforma.
2. US2, la reforma en curso y la publicada vigente.
3. US3, la notificación.
4. El cierre: lo que cambia de F18-H01, el script con el ciclo de la versión 2, el recorrido del autor, la evidencia y la fusión.

Una sola rama y un solo PR, como en las historias anteriores. El PR principal lleva `Refs #72`, y el de evidencia, `Closes #72`.
