---
description: "Tareas de F18-H01: publicar el distributivo aprobado"
---

# Tasks: Publicar el distributivo aprobado

**Input**: documentos de diseño en `specs/055-publicar-distributivo/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si no compila, primero va un esqueleto.

**Decisiones**:
- las dos aclaraciones del autor del 2026-11-18, en la spec;
- D-111, escrita con el plan;
- MD-11, ADR-007, D-100, D-107 y D-108.

**Organization**:
- **Fundacional:** el contrato, el código del catálogo y la regla de publicar en el agregado.
- **US1:** la Secretaría publica (MVP).
- **US2:** la notificación de la publicación.
- **US3:** la versión publicada sin acciones de edición.
- **Cierre:** el script, las regresiones, la evidencia y la fusión.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US3.
- **Rutas:** son relativas a la raíz. `OCS` es `backend/servicios/validacionocs/src`, con `main`, `test` e `integrationTest` en `java/ec/mdd/validacionocs`; `SPA` es `frontend/src`.

---

## Phase 1: Setup

- [X] T001 Escribir **D-111** en `docs/decisiones.md`, en la fila del índice y en su sección (research §1 a §8). Revisar en el glosario «Publicar», `publicar()` y `DistributivoPublicado`.

  **Resultado (2026-11-18):** quedó escrita con el plan.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T004.

- [X] T002 [P] El contrato en `contratos/validacionocs/openapi.yaml`, según `contracts/validacionocs.md`:
  - `publicarDistributivo`: `POST /api/validacionocs/periodos/{periodo}/publicacion`, sin cuerpo, `x-mdd-permiso: PUBLICACION:APROBACION`, con 200 (`EstadoDelFlujo`), 400, 401, 403, 404 y 409;
  - `AccionDelFlujo` suma `PUBLICAR`;
  - `EstadoDelFlujo.publicacion`, opcional, `PublicacionDelDistributivo` con «`version` ≥ 1», «`publicadaPor`» y «`publicadaEn` date-time», `additionalProperties: false`.
  
  Regenerar los tipos de la SPA. Esqueleto: `OCS/main/.../adaptadores/entrada/PublicacionControlador.java`, que lanza hasta T006.

  **Resultado (2026-11-18):** La operación, `PUBLICAR` y `PublicacionDelDistributivo` en el contrato, con 502 y 504 porque el caso de uso consulta el estado del periodo en Académico, como la resolución. **Desviación:** la operación cae en la misma interfaz generada (`AprobacionApi`, etiqueta `Aprobacion`) que implementa `FlujoControlador`, así que va en ese controlador y no en un `PublicacionControlador` aparte. Esqueleto hasta T006.
- [X] T003 [P] El código `VALIDACIONOCS_ETAPA_NO_ADMITE_PUBLICACION` (409, Negocio, «Etapa sin publicación», «El distributivo solo se publica cuando está aprobado.»):
  - en `contratos/comun/errores.md`, con sus 8 columnas;
  - en `OCS/main/.../adaptadores/entrada/ErroresValidacionocs.java`, con `OCS/main/.../dominio/EtapaNoAdmitePublicacion.java`.
  
  Antes, ver fallar `ErroresValidacionocsTest`.

  **Resultado (2026-11-18):** `EtapaNoAdmitePublicacion` y su entrada en `ErroresValidacionocs` y en `errores.md`. `ErroresValidacionocsTest` se vio fallar antes y pasa.
- [X] T004 [P] El agregado en `OCS/main/.../dominio/ProcesoAprobacion.java` y `AccionDelProceso.java`, según `data-model.md`. Antes, ver fallar en `ProcesoAprobacionTest`:
  - `publicar(quien, version, hash, ahora)` en APROBADO, con el rol de esa etapa: pasa a PUBLICADO y devuelve el `PASO_DE_ETAPA` de APROBADO a PUBLICADO con quién, cuándo, la versión y el hash;
  - en cada otra etapa, también PUBLICADO, `EtapaNoAdmitePublicacion` sin cambiar nada;
  - sin el rol de la etapa APROBADO (por ejemplo el Vicerrector), `RolDeOtraEtapa`;
  - con otro rol en la etapa APROBADO de la configuración, lo usa;
  - `publicacion()`: vacía antes, la transición después;
  - `acciones`: `[PUBLICAR]` en APROBADO para su rol; ninguna en PUBLICADO para nadie.


  **Resultado (2026-11-18):** `publicar(quien, ahora)`, `publicacion()` y la acción `PUBLICAR`. **Cambio respecto del plan:** la versión y el hash salen de la congelación, como en `registrarResolucion`, no de un parámetro; así el caso de uso no le pide nada a Generación. Se vieron fallar las 13 pruebas nuevas (incluidas las 8 etapas del rechazo) y pasan. La prueba de la resolución esperaba APROBADO sin acciones; ahora la Secretaría tiene `PUBLICAR`, que es lo buscado.
**Checkpoint**: el contrato, el código y la regla, en verde.

---

## Phase 3: User Story 1 - La Secretaría publica el distributivo aprobado (Priority: P1) 🎯 MVP

**Goal**: la Secretaría pasa el distributivo de APROBADO a PUBLICADO y queda la constancia.

**Independent Test**: con el distributivo en APROBADO, `secretaria.demo` publica; queda PUBLICADO con la versión, quién y cuándo, y la bitácora.

- [X] T005 [US1] El caso de uso: `OCS/main/.../puertos/entrada/PublicarDistributivo.java` y `aplicacion/PublicarDistributivoServicio.java`, con su bean en `ConfiguracionValidacionocs`, como `RegistrarResolucionServicio`:
  - el periodo cerrado y sin distributivo, con los errores que ya existen;
  - en una transacción, con el proceso bloqueado: `publicar`, `procesos.guardar(…, nuevas)`, `notificacion.notificar(…)` y la bitácora;
  - `CambioDeFlujo.Accion.DISTRIBUTIVO_PUBLICADO` en `FlujoDelDistributivoServicio.cambio`, con `{estado: APROBADO}` → `{estado: PUBLICADO}` y periodo, versión y hash.
  
  Antes, ver fallar `PublicarDistributivoServicioTest`: la transición, la notificación y la bitácora en la transacción; un 409 o un 403 no notifica ni registra; una excepción al notificar sale de la transacción, como en `RegistrarResolucionServicioTest`. `ArquitecturaTest` (`quienGuardaTransicionesNotifica`) debe seguir en verde.

  **Resultado (2026-11-18):** `PublicarDistributivo` y `PublicarDistributivoServicio`, con su bean; `DISTRIBUTIVO_PUBLICADO` en `CambioDeFlujo` y en `FlujoDelDistributivoServicio.cambio` (el paso desde APROBADO). Se vieron fallar las 6 pruebas de `PublicarDistributivoServicioTest` contra el esqueleto y pasan. `ArquitecturaTest` sigue en verde.
- [X] T006 [US1] `PublicacionControlador` y la constancia en el estado del flujo:
  - `FlujoDelDistributivo.publicacion` de `ProcesoAprobacion.publicacion()`, mapeada en el controlador del flujo para todo el que lo lee;
  - la operación devuelve el `EstadoDelFlujo`.
  
  Antes, ver fallar `PublicarContratoTest` (nueva: el 200 con la constancia y sin acciones; el 409 en EN_OCS y en PUBLICADO; los 403 sin permiso y sin el rol; el 400) y `FlujoContratoTest` (+2: `PUBLICAR` en APROBADO para la Secretaría; `publicacion` en PUBLICADO). `PermisosValidacionocsTest` suma la operación con `PUBLICACION:APROBACION`.

  **Resultado (2026-11-18):** `FlujoDelDistributivo.publicacion` (con un constructor de 10 argumentos que la deja vacía, para no tocar a quienes ya lo usan), llenada por `FlujoDelDistributivoServicio` y mapeada por `FlujoControlador`, que implementa la operación. Se vieron fallar las 2 de `PublicarContratoTest` que necesitan el controlador y la de `FlujoContratoTest` con la constancia. **Pasaban contra el esqueleto:** la acción `PUBLICAR` en `FlujoContratoTest` (la regla ya estaba en T004), el 403 sin permiso y el 400 de `PublicarContratoTest` (el permiso y el patrón se comprueban antes del controlador) y `PermisosValidacionocsTest`. Validación OCS pasa completo.
- [X] T007 [US1] `OCS/integrationTest/.../RepositorioProcesoIntegrationTest.java`: dos publicaciones a la vez dejan una sola transición a PUBLICADO; la otra, `EtapaNoAdmitePublicacion`.

  **Resultado (2026-11-18):** `RepositorioProcesoIntegrationTest` (+1): dos publicaciones a la vez dejan una transición a PUBLICADO y la otra, `EtapaNoAdmitePublicacion`. Pasa en la primera corrida: **no se vio fallar**, porque el bloqueo del proceso ya existía (F15-H01).
- [X] T008 [US1] La SPA, según `contracts/spa.md`:
  - `SPA/pantallas/comun/DialogoDePublicacion.tsx` y su uso en `EstadoDelFlujo.tsx`: «Publicar el distributivo» solo con la acción `PUBLICAR`, la confirmación con su texto, el `POST` sin cuerpo, el error en el diálogo, y recargar el flujo al publicar;
  - la constancia «Versión {v} publicada por {publicadaPor} el {dd/mm/aa hh:mm}».
  
  Antes, ver fallar en `EstadoDelFlujo.test.tsx`: el botón solo con la acción, el diálogo y su texto, el `POST`, el error, la constancia y que en PUBLICADO no hay acción.
  **Resultado (2026-11-18):** `flujo.publicarDistributivo`, `DialogoDePublicacion` (foco en «Cancelar», el error queda en el diálogo) y, en `EstadoDelFlujo`, el botón con la acción `PUBLICAR` y la constancia de la publicación. Se vieron fallar 3 de las 4 pruebas nuevas de `EstadoDelFlujo.test.tsx` (el diálogo y el `POST` sin cuerpo, el 409 en el diálogo y la constancia); **«sin la acción no hay botón» pasaba** contra el componente anterior, que nunca lo mostraba. Pasan las 34; `tipos` sin errores.
- [X] T009 [US1] Playwright, en `frontend/e2e/resolucion.spec.ts`: la Secretaría publica con el teclado, confirma y ve la constancia; axe-core (WCAG 2.1 AA) sin fallas con el diálogo abierto y en PUBLICADO (SC-006).


  **Resultado (2026-11-18):** `resolucion.spec.ts` (+1, Chromium): publica con el teclado, el foco empieza en «Cancelar», axe sin fallas con el diálogo abierto y en PUBLICADO, el `POST` sin cuerpo y la constancia. Pasa en la primera corrida: **no se vio fallar**, porque la SPA ya estaba hecha (T008).
**Checkpoint**: se publica en la API y en la SPA.

---

## Phase 4: User Story 2 - Los docentes, Talento Humano y las coordinaciones reciben la notificación (Priority: P1)

**Goal**: la publicación llega a DOCENTE, OPERADOR_DATOS y COORDINADOR_CARRERA, una vez por usuario, nunca a quien publicó.

**Independent Test**: tras publicar, `docente.demo`, `datos.demo`, `coordinacion.demo` y `docente.coordinador.demo` tienen una notificación de la publicación; `secretaria.demo` no.

- [X] T010 [P] [US2] Los destinos y el mensaje en `OCS/main/.../dominio/ProcesoAprobacion.java` (`destinoDe`) y `MensajesDeTransicion.java`, según `data-model.md`:
  - el paso a PUBLICADO va a «DOCENTE, OPERADOR_DATOS y COORDINADOR_CARRERA, sin carrera», y no incluye SECRETARIA_GENERAL ni el rol de la etapa PUBLICADO (el caso borde de las dos secretarías);
  - título «El distributivo de {periodo} fue publicado», detalle «Versión {v}. Es la versión oficial del periodo.»;
  - el enlace de PUBLICADO pasa de `/resolucion-ocs` a `/`.
  
  Antes, ver fallar en `MensajesDeTransicionTest` y `ProcesoAprobacionTest`.

  **Resultado (2026-11-18):** `ProcesoAprobacion.destinoDe` da, para el paso a PUBLICADO, DOCENTE, OPERADOR_DATOS y COORDINADOR_CARRERA sin carrera (un `if` antes del `switch`: Java no admite guardas `when` sobre constantes de enum); `MensajesDeTransicion` da el título, el detalle y el enlace `/`. Se vieron fallar la prueba nueva de `MensajesDeTransicionTest` (con el rol de PUBLICADO cambiado a RECTOR, para comprobar que no lo usa) y la de `ProcesoAprobacionTest`. Validación OCS pasa completo.
- [X] T011 [P] [US2] `SPA/navegacion/enlaces.test.ts`: PUBLICADO lleva a `/`, y lo pueden abrir DOCENTE, OPERADOR_DATOS y COORDINADOR_CARRERA (sus permisos de la matriz D-16).

  **Resultado (2026-11-18):** `enlaces.test.ts`: PUBLICADO lleva a `/`, y DOCENTE y OPERADOR_DATOS (sus permisos de V2__matriz_d16.sql) suman a la tabla de permisos; +3 casos. **No se vio fallar:** el inicio no exige permisos, así que los tres pueden abrirlo; la prueba fija la tabla igual a `MensajesDeTransicion.ENLACES`.

  **Cambio después del recorrido del autor (2026-11-19):** con el enlace al inicio, el clic en la notificación no llevaba a ninguna parte visible. El autor eligió un enlace por rol: COORDINADOR_CARRERA va a `/oferta`, cuya tarjeta del flujo muestra la constancia; DOCENTE y OPERADOR_DATOS siguen en `/` hasta F19. Se vio fallar la prueba cambiada de `MensajesDeTransicionTest` y pasa; `enlaces.test.ts` comprueba cada enlace contra los permisos de su rol (pasa a la primera, porque la coordinación ya abría `/oferta`). D-111, spec, research §4, data-model y contracts/spa.md, actualizados.
- [X] T012 [US2] `backend/apps/identidad/src/integrationTest/java/ec/mdd/NotificacionesDesdeAprobacionIntegrationTest.java` (+1): la solicitud del paso a PUBLICADO deja una notificación a un usuario con DOCENTE y COORDINADOR_CARRERA a la vez, a uno de OPERADOR_DATOS, y ninguna al autor ni a un usuario desactivado.


  **Resultado (2026-11-18):** `NotificacionesDesdeAprobacionIntegrationTest` (+1): con los tres destinos de la publicación, quien tiene DOCENTE y COORDINADOR_CARRERA recibe una; OPERADOR_DATOS, una; ni el autor, ni la otra Secretaría, ni un docente desactivado. Pasa en la primera corrida: **no se vio fallar**, porque Notificaciones no cambia (la clave única de F16-H01 ya lo resolvía).
**Checkpoint**: la publicación notifica a quien corresponde.

---

## Phase 5: User Story 3 - La versión publicada no se puede editar (Priority: P1)

**Goal**: con el distributivo publicado, nadie ve una acción de edición, y las operaciones de cambio responden con su error.

**Independent Test**: con PUBLICADO, el estado del flujo no tiene acciones para ningún rol; «Consolidado del periodo» dice «publicada».

- [X] T013 [US3] El aviso en `SPA/pantallas/consolidado/Consolidado.tsx` y `SPA/pantallas/inconsistencias/Inconsistencias.tsx`: con el flujo en PUBLICADO, «La versión {v} está publicada: ya no cambia.» en lugar de «está congelada». Antes, ver fallar en `Consolidado.test.tsx` y `Inconsistencias.test.tsx`.

  **Resultado (2026-11-18):** las dos pantallas consultan el estado del flujo solo con la versión congelada (publicar exige haberla congelado; en «Consolidado del periodo» la consulta es la misma de la tarjeta) y, en PUBLICADO, dicen «está publicada». Se vio fallar la prueba nueva de `Consolidado.test.tsx` y la de `Inconsistencias.test.tsx`; pasan las 641 de la SPA, con `tipos` y `lint` limpios.
- [X] T014 [US3] En `FlujoDelDistributivoServicioTest` y `RegistrarResolucionServicioTest`: con el distributivo en PUBLICADO, el avance, enviar a revisión, devolver, aprobar el contenido y registrar la resolución responden con el error de su etapa, sin guardar ni notificar. Las operaciones de Generación (proponer un docente, una actividad, consolidar y justificar) ya responden `GENERACION_VERSION_CONGELADA` desde D-100, con sus pruebas; aquí las cubre el paso 17 del script (T015).


  **Resultado (2026-11-18):** `FlujoDelDistributivoServicioTest` (+1), con los procesos en memoria llevados hasta PUBLICADO: el avance (`EtapaConOperacionPropia`), enviar a revisión, devolver y aprobar el contenido responden con su error, sin registrar, notificar, congelar ni pedir condiciones; y nadie (Secretaría, Responsable, Vicerrector, coordinador) tiene acciones. **Desvío:** registrar la resolución en PUBLICADO va en `ProcesoAprobacionTest` (+1), con el agregado real, porque `RegistrarResolucionServicioTest` lo simula y no probaría la regla. Pasan en la primera corrida: **no se vieron fallar**, porque cada operación ya exigía su etapa.
**Checkpoint**: las tres historias funcionan.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T015 `deploy/probar_documento_firmable.sh`, según `contracts/demostracion.md`:
  - en EN_OCS (paso 13), el 409 de publicar;
  - el paso 16, publicar: los 403 de `vicerrectorado.demo` y `coordinacion.demo`, la publicación de `secretaria.demo` con `identidad` detenido y el token ya pedido, la constancia, la bitácora y el 409 de la segunda;
  - el paso 17, sin acciones: el estado del flujo de cada usuario de la demo y cada operación de cambio con su error;
  - los pasos de las notificaciones, del 18 al 21: `esperadas()` con la publicación para `datos.demo`, `coordinacion.demo`, `docente.demo` y `docente.coordinador.demo`; la caída del paso 19, movida del envío a la publicación;
  - `MDD_CONSERVAR=APROBADO`.
  
  Actualizar la cabecera del script y el comentario de `.github/workflows/perfil-consolidado.yml`. shellcheck sin hallazgos.

  **Resultado (2026-11-19):** el 409 de publicar en el paso 13; los pasos 16 (publicar, con la caída de `identidad` movida desde el envío a revisión) y 17 (sin acciones: el flujo de los 11 usuarios de la demo, 200 sin acciones o 403, y proponer, consolidar, enviar a revisión, aprobar y registrar con su error, sin transiciones nuevas); las notificaciones pasan a los pasos 18 a 21, con la publicación en `esperadas()`; `MDD_CONSERVAR=APROBADO`. **Desvío:** el paso 18 espera las bandejas hasta `ESPERA_REENVIO` (120 s) y no 30 s, porque la notificación de la publicación puede llegar hasta 120 s después de la vuelta de `identidad`. Cabecera y comentario del workflow actualizados; shellcheck sin hallazgos. **En la CI del PR (2026-11-20):** fallaron los pasos 12 y 16, que esperaban 15 s entradas de la bitácora enviadas justo después de detener `identidad` (la de la publicación y la de F1, de F16-H02); ahora esperan hasta `ESPERA_REENVIO`.
- [X] T016 Correr los scripts de `perfil-consolidado`, en su orden, sobre un perfil construido desde la rama (SC-001 a SC-004). Después, el recorrido a mano de `quickstart.md` §3, del autor (SC-005).

  **Resultado parcial (2026-11-19):** los 36 scripts, en su orden, sobre un perfil construido desde la rama. En la primera corrida, 35 pasaron y `probar_documento_firmable.sh` falló en los pasos 18 y 20: `esperadas()` nombraba a cuatro destinatarios fijos, y la publicación también le llegó a `autor.prueba`, un usuario activo con DOCENTE y COORDINADOR_CARRERA que dejan otros scripts; el 20 falló solo porque el 18 cortó antes de la lectura. Se corrigió el script (los destinatarios de la publicación salen de la base, y se exige aparte que a los cuatro de la demo les llegue una vez), y `probar_documento_firmable.sh` y `probar_rechazo_en_firma.sh`, otra vez en ese orden sobre el mismo perfil, pasan todos sus pasos; la notificación de la publicación llegó 19 s después de la vuelta de `identidad`. `MDD_CONSERVAR=APROBADO` deja el perfil en APROBADO. Recorrido del autor (quickstart §3, 2026-11-19): secretaria.demo publicó desde «Resolución del OCS» y la tarjeta mostró la constancia sin la acción; coordinacion.demo, desde la campana, llegó a «Oferta del periodo» con la constancia; «Consolidado del periodo» dijo «está publicada» sin acciones. **Hallazgo del recorrido:** con el enlace al inicio, el clic en la notificación no llevaba a ninguna parte visible; se cambió a un enlace por rol (ver T011) y se repitió el recorrido con el servicio reconstruido.
- [X] T017 Correr completos, y medir la cobertura de Validación OCS (60 %, D-32):
  - `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --continue`;
  - en la SPA: tipos, ESLint, Prettier, Vitest y Playwright;
  - Gitleaks sobre `main..HEAD`, después del commit.

  **Resultado parcial (2026-11-19):** el build completo (`build integrationTest jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --continue`) terminó con dos fallas en módulos que esta rama no toca: `ClienteDeEventosLocalTest` (`plataforma:eventos`) y la prueba de tiempos de `ConfirmarLasDiezCargasIntegrationTest` (`apps:datos`, «se revisa en 30 s y se confirma en 60 s»). Corrió a la vez que Vitest, que también dio 9 fallas. Repetidos solos, pasan: los dos módulos con `--rerun-tasks`, y Vitest 641/641. Validación OCS: 86 % de instrucciones (mínimo 60 %, D-32), con su verificación en verde. SPA: tipos, ESLint y Prettier sin hallazgos; Playwright 134/134 (Chromium y Firefox). Gitleaks (v8.30.1) sobre `main..HEAD`, después del commit: 7 commits, sin filtraciones.
- [X] T018 [P] Revisar el JavaDoc: por qué publica Aprobación, qué es la versión publicada, a quién se notifica y por qué no es el rol de la etapa.

  **Resultado (2026-11-19):** `ProcesoAprobacion.publicar` y `PublicarDistributivoServicio` dicen por qué publica Aprobación (MD-11) y que la versión publicada es la congelada (D-100); `PublicarDistributivo`, quién publica; `EtapaNoAdmitePublicacion`, cuándo se lanza. Se sumó a `destinoDe` la excepción de la publicación: a quién va y por qué no al rol de la etapa PUBLICADO.
- [X] T019 Crear `specs/055-publicar-distributivo/evidencia.md`, como la de 054.

  **Resultado (2026-11-19):** `evidencia.md`, con las pruebas, el script, el recorrido del autor y su hallazgo, las desviaciones y los criterios de terminado; la sección del equipo de demostración queda para después de fusionar.
- [X] T020 Fusionar solo con la aprobación del autor y todas las comprobaciones en verde.
  - Verificar la demo en solo lectura (`quickstart.md` §4): «Sano» y la memoria de `identidad`.
  - Abrir el PR de evidencia con `Closes #71`.
  - La épica F18 (#17) sigue abierta por F18-H02.

  **Resultado (2026-11-20):** PR #248 fusionado (`9e05c83`) con las 12 comprobaciones en verde, después del PR #249 (PostgreSQL 18.7 y nginx mainline, D-112). La demo tomó `9e05c83`, «Sano en 55 s», con `identidad` en 492 de 768 MiB; 2026-I sigue en EN_CONSOLIDACION. PR de evidencia con `Closes #71`.

---

## Dependencies & Execution Order

- **Setup:** T001, hecha.
- **Fundacional:** T002 a T004, en paralelo.
- **US1:**
  - T005 después de T003 y T004;
  - T006 después de T002 y T005;
  - T007 después de T005;
  - T008 después de T002;
  - T009 después de T008.
- **US2:**
  - T010 después de T004;
  - T011 sin dependencias;
  - T012 después de T010.
- **US3:**
  - T013 después de T002;
  - T014 después de T004.
- **Cierre:**
  - T015 después de las tres historias;
  - T016 después de T015;
  - T017 y T018 al final;
  - T019 después de T016 y T017;
  - T020 al final.

### Parallel Opportunities

```text
T002 a T004.
T008 (SPA) en paralelo con T005 a T007 (backend).
T010 y T011 en paralelo.
T013 (SPA) en paralelo con T014 (backend).
T018 en paralelo con T016 y T017.
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. La fundación: T002 a T004.
2. US1: T005 a T009. Con eso, la Secretaría ya publica y se ve la constancia.
3. Validar con el contrato y la prueba de dos publicaciones a la vez.

### Incremental Delivery

1. US1, publicar.
2. US2, la notificación a los tres roles.
3. US3, la versión publicada sin acciones.
4. El cierre: el script, el recorrido del autor, la evidencia y la fusión.

Una sola rama y un solo PR, como en las historias anteriores. El PR principal lleva `Refs #71`, y el de evidencia, `Closes #71`.
