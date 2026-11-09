---

description: "Tareas de F12-H05: devolver un parcial a su carrera con una observación"
---

# Tasks: Devolver un parcial a su carrera con una observación

**Input**: documentos de diseño en `specs/043-devolver-parcial/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si una prueba no compila, se agrega primero un esqueleto, y la implementación va en un comando aparte.

**Decisiones**:
- las cuatro aclaraciones del autor del 2026-11-08, en la spec;
- D-98, escrita en [`docs/decisiones.md`](../../docs/decisiones.md) el 2026-11-08;
- D-95, D-97, MD-04, ADR-003 y D-16;
- RF-34 y RF-36.

**Organization**:
- **Fundacional:** el contrato, los errores, la migración `V4`, `admiteDevolucion` en la copia de la configuración, `DevolucionDeParcial` y el método publicado de Generación.
- **US1:** devolver una o varias carreras (MVP).
- **US2:** corregir y reenviar.
- **US3:** sin observación no se devuelve.
- **US4:** solo el rol de EN_CONSOLIDACION devuelve.
- **Cierre:** la pantalla, el script, SC-005 de F10-H03, la evidencia y la fusión.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US4.
- **Rutas:** relativas a la raíz. Abreviaturas, cada una con `main` y `test` en `java/ec/mdd/<servicio>`, e `integrationTest` donde aplica:
  - `GEN` = `backend/servicios/generacion/src`;
  - `VAL` = `backend/servicios/validacionocs/src`;
  - `SPA` = `frontend/src`.

---

## Phase 1: Setup

- [X] T001 Revisar el glosario (`~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/sdd/modelo_dominio.md` §6 y §7): «Devolución», «observación», `ParcialCarrera` con DEVUELTO y `DistributivoDevuelto`. Confirmar si `DevolucionDeParcial` nombra un concepto nuevo del negocio. Si lo es, proponerlo al autor. Anotar el resultado aquí. **Resultado (2026-11-08):** «Devolución», `DistributivoDevuelto` y `Observacion` (objeto de valor, §5 y §7) ya están. El texto se modela como `Observacion`, del glosario. `DevolucionDeParcial` solo junta una carrera con su `Observacion` en la solicitud, no es un concepto nuevo; en el contrato se llama `ParcialADevolver`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T006.

- [X] T002 Agregar a `contratos/validacionocs/openapi.yaml`, según `contracts/validacionocs.md`:
  - la operación `devolverParciales` (`POST /api/validacionocs/periodos/{periodo}/devolucion`), en la etiqueta `Aprobacion`, con `x-mdd-permiso: CONSOLIDACION:APROBACION`;
  - el cuerpo `SolicitudDeDevolucion` (`additionalProperties: false`): `devoluciones`, de 1 a 50 elementos, cada uno con `carrera` (`^[0-9A-Z_-]{1,20}$`) y `observacion` (`minLength: 10`, `maxLength: 500`, `pattern: '^[\s\S]*\S[\s\S]*$'`, que cubre el texto entero: con `'\S'` el `@Pattern` generado solo aceptaría un carácter);
  - sus respuestas 200 `EstadoDelFlujo`, 400, 401, 403, 404, 409 y 500, con los códigos de `contracts/validacionocs.md`;
  - `DevolucionDelParcial` (`por`, `en` `date-time`, `observacion`, todos obligatorios) como `devolucion` opcional de `ParcialDelFlujo`;
  - `DEVOLVER_PARCIALES` en `AccionDelFlujo` y `DEVOLUCION` en `TransicionDelFlujo.tipo`.

  Regenerar `SPA/api/generado/validacionocs.d.ts` y compilar `:servicios:validacionocs`; si el controlador deja de compilar, un esqueleto que lance `UnsupportedOperationException`. La descripción de `devolverParciales` no nombra la bitácora. Después correr `./gradlew :servicios:usuarios:test --rerun` (`BitacoraSinBorradoContratoTest`).
- [X] T003 Agregar a `contratos/comun/errores.md`, con sus 8 columnas (research §10):
  - 409 `VALIDACIONOCS_ETAPA_NO_ADMITE_DEVOLUCION`, `VALIDACIONOCS_DEVOLUCION_DESACTIVADA` y `VALIDACIONOCS_PARCIAL_NO_ENVIADO`;
  - 400 `VALIDACIONOCS_CARRERA_REPETIDA` y `VALIDACIONOCS_OBSERVACION_INVALIDA`.

  Crear sus excepciones en `VAL/main/.../dominio/`. `ObservacionInvalida` y `CarreraRepetida` llevan la carrera, y `ParcialNoEnviado` también. Agregar sus entradas a `VAL/main/.../adaptadores/entrada/ErroresValidacionocs.java`; las que llevan carrera, con `EntradaCatalogo.conDetalles` y el campo `carrera`. Escribir primero su prueba en `ErroresValidacionocsTest`.
- [X] T004 Crear `VAL/main/resources/db/migration/validacionocs/V4__devolucion.sql` (data-model.md):
  - el `check` de `parcial_del_proceso.estado` admite `'DEVUELTO'`;
  - el `check` de `transicion.tipo` admite `'DEVOLUCION'`;
  - la tabla `observacion_de_devolucion (transicion_id uuid not null references transicion (id) on delete cascade, carrera varchar(20) not null, texto varchar(500) not null check (length(btrim(texto)) between 10 and 500), primary key (transicion_id, carrera))`;
  - en `proceso_aprobacion.etapas`, cada etapa suma `admiteDevolucion` con el valor de `flujo-por-defecto.yaml` para su tipo: `true` para EN_CONSOLIDACION, EN_REVISION_VRA, CONGELADO_EN_FIRMA y EN_OCS; `false` para las demás.

  Buscar en la base los nombres reales de los `check` antes de soltarlos. Sumar `admiteDevolucion` a `EtapaDelProceso` (y a `EtapaDelProceso.de(Etapa)`) y a `EtapaGuardada` de `VAL/main/.../adaptadores/salida/RepositorioProcesoJdbc.java`. Ver fallar primero `VAL/integrationTest/.../RepositorioProcesoIntegrationTest.java`:
  - un proceso guardado sin `admiteDevolucion`, como lo guarda F12-H02, queda con los valores de la plantilla después de `V4`;
  - un proceso nuevo guarda y lee `admiteDevolucion` de su configuración.
- [X] T005 [P] Crear `VAL/main/.../dominio/DevolucionDeParcial.java` (`carrera`, `observacion`). Quita los espacios de los extremos y exige de 10 a 500 caracteres (`ObservacionInvalida` con la carrera). Ver fallar primero `DevolucionDeParcialTest`: 9 y 10 caracteres, 500 y 501, solo espacios, y espacios en los extremos que no cuentan.
- [X] T006 [P] En Generación, agregar a `GEN/main/.../puertos/entrada/ParcialesDelDistributivo.java` el método `marcarParcialesDevueltos(periodo, carreras)`, que devuelve la versión (`contracts/generacion.md`). Ver fallar primero `GEN/test/.../aplicacion/ParcialesDelDistributivoServicioTest.java` (Mockito):
  - marca DEVUELTO todas las carreras, en una transacción con la versión bloqueada;
  - si una carrera no tiene parcial, `ParcialNoEncontrado` y no marca ninguna;
  - un parcial ya DEVUELTO queda igual;
  - sin distributivo, `DistributivoNoEncontrado`;
  - no registra en el registro de cambios.

  Después, en un comando aparte, implementarlo en `ParcialesDelDistributivoServicio`.

**Checkpoint**: el contrato compila, los errores pasan su prueba, la `V4` corre y Generación marca los parciales.

---

## Phase 3: User Story 1 - Devolver parciales con su observación (Priority: P1) 🎯 MVP

**Goal**: el Responsable devuelve una o varias carreras, cada una con su observación, y el distributivo vuelve a BORRADOR_CARRERA con una transición y una entrada.

**Independent Test**: en la prueba de integración, desde EN_CONSOLIDACION, devolver dos carreras deja el proceso en BORRADOR_CARRERA, las dos DEVUELTO, la transición `DEVOLUCION` con sus observaciones guardadas y una entrada `DISTRIBUTIVO_DEVUELTO`.

- [X] T007 [US1] En el dominio de Validación OCS, sumar `EstadoDeEnvio.DEVUELTO`, `TipoDeTransicion.DEVOLUCION` y `Transicion.observaciones` (mapa ordenado, solo en `DEVOLUCION`; vacío en los demás tipos, que el constructor exige). Agregar a `VAL/test/.../dominio/ProcesoAprobacionTest.java` y ver fallar:
  - `devolver` con dos carreras deja los dos parciales DEVUELTO, los demás ENVIADO, el estado en BORRADOR_CARRERA y una `DEVOLUCION` de EN_CONSOLIDACION a BORRADOR_CARRERA, con la versión, el hash, quién, cuándo y las dos observaciones;
  - devolver todas las carreras se permite (Edge Cases);
  - la `DEVOLUCION` lleva la versión vigente, y la versión no cambia: no se crea una nueva (FR-014);
  - una carrera sin parcial en el proceso (`ParcialNoEncontrado`) o con el parcial PENDIENTE (`ParcialNoEnviado`) rechaza todo, sin cambiar nada (FR-005);
  - una carrera repetida lanza `CarreraRepetida`;
  - `validarDevolucion` no cambia nada.

  Después, en un comando aparte, `validarDevolucion` y `devolver` en `ProcesoAprobacion`.
- [X] T008 [US1] Crear `VAL/main/.../puertos/entrada/DevolverParciales.java` y agregar `marcarDevueltos(periodo, carreras)` a `VAL/main/.../puertos/salida/Distributivo.java`. Agregar a `VAL/test/.../aplicacion/FlujoDelDistributivoServicioTest.java` y ver fallar:
  - el orden de research §2: periodo abierto, `validarDevolucion`, `marcarDevueltos` y, en la transacción con el proceso bloqueado, `devolver` y el guardado;
  - una sola entrada en `RegistroDeCambios`, con la acción `DISTRIBUTIVO_DEVUELTO`; en el detalle, `periodo`, `version`, `hash` y `devueltos` (carrera → observación); en `anterior` y `nuevo`, el estado (`contracts/bitacora.md`);
  - si `validarDevolucion` rechaza, no llama a Generación;
  - un periodo cerrado responde `PeriodoCerrado`;
  - un rechazo no registra nada;
  - si Generación lanza `ParcialNoEncontrado`, no se registra nada;
  - la vista: cada parcial DEVUELTO trae `devolucion` con quién, cuándo y la observación de su última `DEVOLUCION`.

  Después, en un comando aparte:
  - `devolver` en `FlujoDelDistributivoServicio` y la vista;
  - la acción `DISTRIBUTIVO_DEVUELTO` en `CambioDeFlujo`, que `cambio()` elige para una `DEVOLUCION`;
  - el método en `VAL/main/.../adaptadores/salida/DistributivoDesdeGeneracion.java`;
  - `RepositorioProcesoJdbc` guarda y lee `DEVUELTO`, `DEVOLUCION` y las filas de `observacion_de_devolucion`.
- [X] T009 [US1] Agregar a `VAL/test/.../adaptadores/entrada/FlujoContratoTest.java` y ver fallar:
  - el 200 de `devolverParciales` cumple `EstadoDelFlujo`, con BORRADOR_CARRERA y `devolucion` en los parciales devueltos;
  - el 404 `VALIDACIONOCS_PARCIAL_NO_ENCONTRADO` y el 409 `VALIDACIONOCS_PARCIAL_NO_ENVIADO`;
  - el 404 sin distributivo.

  Después, la operación en `VAL/main/.../adaptadores/entrada/FlujoControlador.java`:
  - arma las `DevolucionDeParcial`;
  - traduce `RolDeOtraEtapa` a `AccessDeniedException`;
  - mapea `devolucion` en `ParcialDelFlujo`.

  Conectar el caso de uso en `VAL/main/.../infraestructura/ConfiguracionValidacionocs.java`. Sumar `devolverParciales` a `PermisosValidacionocsTest` y actualizar `FlujoEnMemoria`.
- [X] T010 [US1] Crear `VAL/integrationTest/.../DevolucionIntegrationTest.java` con PostgreSQL real, como `EnvioARevisionIntegrationTest`: el servicio, `RepositorioProcesoJdbc` y la transacción reales, y un `Distributivo` falso. Comprueba:
  - devolver dos carreras desde EN_CONSOLIDACION deja el proceso en BORRADOR_CARRERA, las dos DEVUELTO y la `DEVOLUCION` con sus dos filas en `validacionocs.observacion_de_devolucion`;
  - un `RegistroDeCambios` falso recibe una sola entrada;
  - con dos hilos que devuelven a la vez, una sola `DEVOLUCION`; el otro recibe `EtapaNoAdmiteDevolucion` (SC-004);
  - una devolución y un envío a revisión a la vez: pasa solo uno;
  - dos devoluciones a la vez con carreras **distintas**: pasa una; las carreras de la otra quedan DEVUELTO en Generación y ENVIADO en Aprobación; volver a devolverlas pasa y deja todo coherente (research §11).
- [X] T011 [P] [US1] Agregar a `GEN/integrationTest/` una prueba con PostgreSQL real de `marcarParcialesDevueltos`:
  - dos carreras quedan DEVUELTO;
  - con una carrera inexistente, ninguna cambia.

**Checkpoint**: la devolución funciona por la API, con su entrada en la bitácora.

---

## Phase 4: User Story 2 - Corregir y reenviar el parcial devuelto (Priority: P1)

**Goal**: el coordinador edita el parcial devuelto y lo reenvía; con el último, el distributivo vuelve a EN_CONSOLIDACION.

**Independent Test**: en la prueba de integración, tras devolver dos carreras, reenviar la primera deja BORRADOR_CARRERA y reenviar la segunda pasa a EN_CONSOLIDACION con un `PASO_DE_ETAPA` (`probar_devolucion.sh`, pasos 7 y 8).

- [X] T012 [US2] Agregar a `ProcesoAprobacionTest` y ver fallar:
  - `validarEnvio` acepta un parcial DEVUELTO y sigue rechazando uno ENVIADO (`ParcialYaEnviado`);
  - `acciones` ofrece `ENVIAR_PARCIAL` al coordinador con el parcial DEVUELTO;
  - tras devolver dos, `enviarParcial` de la primera deja BORRADOR_CARRERA, y el de la segunda pasa a EN_CONSOLIDACION;
  - con BORRADOR_UNIDADES activa, el último reenvío pasa a BORRADOR_UNIDADES (Edge Cases);
  - `validarRevision` rechaza con un parcial DEVUELTO (`ParcialesPendientes`);
  - una segunda devolución de la misma carrera, después de reenviarla, se permite, y la vista muestra la observación de la última.

  Después, en un comando aparte, los cambios de research §5 en `ProcesoAprobacion`.
- [X] T013 [US2] Agregar a `FlujoDelDistributivoServicioTest` y ver fallar: `enviarARevision` rechaza con `ParcialesPendientes` si Generación tiene algún parcial que no está enviado aunque Aprobación lo tenga ENVIADO (research §2). Después, en un comando aparte, la comprobación en `enviarARevision`.
- [X] T014 [US2] Agregar a `DevolucionIntegrationTest` el reenvío: las dos carreras devueltas se reenvían y el proceso vuelve a EN_CONSOLIDACION con un `PASO_DE_ETAPA`. Agregar a la prueba de integración de Generación de T011 que una propuesta en el parcial devuelto se guarda, y que el consolidado queda desactualizado solo después de ese cambio (research §7).

**Checkpoint**: el ciclo completo, devolver y reenviar, funciona por la API.

---

## Phase 5: User Story 3 - No devolver sin observación (Priority: P1)

**Goal**: ninguna devolución sin observación válida; todo o nada.

**Independent Test**: sin observación, con una de 9 caracteres, con una solo de espacios y con una carrera repetida, 400 y ningún parcial devuelto (`probar_devolucion.sh`, paso 3).

- [X] T015 [US3] Agregar a `FlujoContratoTest` y ver fallar:
  - el 400 `COMUN_SOLICITUD_INVALIDA` con `devoluciones` vacía, una observación de 9 y de 501 caracteres, una solo de espacios y una carrera fuera del patrón, cada uno con su campo;
  - el 400 `VALIDACIONOCS_OBSERVACION_INVALIDA`, con una observación de 10 caracteres que tiene espacios en los extremos y la carrera en el detalle;
  - el 400 `VALIDACIONOCS_CARRERA_REPETIDA`;
  - una observación normal de 10 caracteres y una de 500, con saltos de línea, pasan el contrato (el patrón de A1 del análisis);
  - una devolución de tres carreras con una observación inválida no devuelve ninguna (US3-2).

**Checkpoint**: RF-36 se cumple en la API.

---

## Phase 6: User Story 4 - Solo el rol de la etapa devuelve (Priority: P1)

**Goal**: el 403 con cualquier otro rol, la acción solo para el rol de la etapa y solo si la etapa admite devolución.

**Independent Test**: los 9 roles que no son el de EN_CONSOLIDACION reciben 403 con el mismo cuerpo; `consultarFlujo` muestra `DEVOLVER_PARCIALES` solo al Responsable.

- [X] T016 [US4] Agregar a `ProcesoAprobacionTest` y ver fallar:
  - `validarDevolucion` y `devolver` exigen el rol de EN_CONSOLIDACION de la copia de la configuración (`RolDeOtraEtapa`), también con otro rol configurado para esa etapa;
  - fuera de EN_CONSOLIDACION, `EtapaNoAdmiteDevolucion`; con `admiteDevolucion` falso en la copia, `DevolucionDesactivada`;
  - `acciones` incluye `DEVOLVER_PARCIALES` junto a `ENVIAR_A_REVISION` solo con el rol, en EN_CONSOLIDACION y si admite devolución;
  - el orden de los rechazos de research §10.

  Después, en un comando aparte, `AccionDelProceso.DEVOLVER_PARCIALES` y lo que falte en `ProcesoAprobacion`.
- [X] T017 [US4] Agregar a la tabla de rol, operación y resultado de `FlujoContratoTest` la operación `devolverParciales`, en EN_CONSOLIDACION. Comprueba el 403 de cada uno de los 9 roles de la matriz D-16 que no son el Responsable de consolidación, con el mismo cuerpo, y el 200 del Responsable (SC-003). Agregar los 409 `VALIDACIONOCS_ETAPA_NO_ADMITE_DEVOLUCION`, `VALIDACIONOCS_DEVOLUCION_DESACTIVADA` y `VALIDACIONOCS_PERIODO_CERRADO`.

**Checkpoint**: las cuatro historias funcionan por la API.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T018 [P] En la SPA, según `contracts/spa.md`, escribir primero las pruebas.
  - **`SPA/pantallas/comun/EstadoDelFlujo.test.tsx`:**
    - un parcial devuelto dice «devuelto por … el …» y muestra la observación;
    - el coordinador con el parcial devuelto ve «Reenviar parcial».
  - **`SPA/pantallas/consolidado/Consolidado.test.tsx`:**
    - «Devolver parciales» solo con `DEVOLVER_PARCIALES`;
    - el diálogo: marcar una carrera muestra su observación, con el contador «N de 500» y el error con menos de 10 caracteres sin espacios;
    - «Devolver» deshabilitado sin carreras o con una observación inválida;
    - la petición lleva las carreras con sus observaciones;
    - el 409 se muestra en el diálogo;
    - al devolver, se invalidan el flujo y el consolidado.

  Después:
  - `devolverParciales` en `SPA/pantallas/comun/flujo.ts`;
  - el código de `EstadoDelFlujo.tsx` y `Consolidado.tsx`;
  - en `frontend/e2e/consolidado.spec.ts`, la devolución de dos carreras con la API simulada y axe-core con el diálogo abierto;
  - en `frontend/e2e/oferta.spec.ts`, el coordinador con su parcial devuelto, que ve la observación y «Reenviar parcial».
- [X] T019 Crear `deploy/probar_devolucion.sh` con los 9 pasos de `contracts/demostracion.md`, sobre los datos de prueba y con la base limpia al terminar, sin imprimir claves ni tokens. Agregar su paso a `.github/workflows/perfil-consolidado.yml` después de «Enviar a revisión». Pasar shellcheck. Correr `probar_avance.sh`, `probar_revision.sh` y `probar_devolucion.sh` en su orden sobre un perfil nuevo, y después los scripts que siguen en el workflow.
- [X] T020 SC-007, con tiempo acotado de 2 horas (research §12, quickstart §5). En el perfil local:
  - llevar 2026-I a EN_CONSOLIDACION;
  - devolver las carreras;
  - proponer los paralelos que faltan con la sugerencia de docentes (F08) por la API;
  - reenviar, consolidar y adjuntar un informe.

  Si el panel llega a cero abiertas, cronometrar «Enviar a revisión» desde que se abre «Consolidado del periodo» y anotarlo en `specs/042-enviar-revision/evidencia.md` (SC-005 de F10-H03). Si no llega, anotar allí cuántas duras quedan, de qué reglas y por qué. El script que se use queda en el scratchpad, no en el repositorio, salvo que el autor lo pida.
- [X] T021 Revisar el JavaDoc de `validarDevolucion`, `devolver`, `DevolucionDeParcial`, `ParcialesDelDistributivo.marcarParcialesDevueltos` y `FlujoDelDistributivoServicio.devolver` (principio XII). El de `devolver` dice que no notifica (D-98). Revisar el JavaDoc de `EstadoDeEnvio`, que hoy dice que PENDIENTE equivale a devuelto.
- [X] T022 Crear `specs/043-devolver-parcial/evidencia.md`, como `specs/042-enviar-revision/evidencia.md`:
  - las pruebas unitarias e `integrationTest`, completas;
  - la cobertura por módulo (60 %, D-32), Spotless, SpotBugs y Spring Modulith;
  - la SPA: tipos, ESLint, Prettier, Vitest y Playwright;
  - los scripts de `perfil-consolidado` en su orden sobre un perfil nuevo, shellcheck y Gitleaks;
  - SC-005 cronometrado en el perfil local (quickstart §3);
  - el resultado de T020;
  - los criterios de terminado de la constitución §6 y las desviaciones.
- [ ] T023 Fusionar solo con la aprobación del autor y con todas las comprobaciones en verde. Verificar el despliegue en el equipo de demostración: GitHub `demostracion`, los 8 contenedores sanos con la versión nueva y la migración `V4` en `flyway_schema_history`. Hacer la comprobación del autor con `quickstart.md` §3 y §4, con una carrera. Abrir el PR de evidencia con `Closes #211`. Borrar las ramas solo si el autor lo pide.

---

## Dependencies & Execution Order

- **Setup:** T001 antes del código.
- **Fundacional:**
  - T002 y T003 antes del código;
  - T004 después de T002, para que `:servicios:validacionocs` compile;
  - T005 depende de T003;
  - T006 no depende de las anteriores.
- **US1:**
  - T007 depende de T003, T004 y T005;
  - T008 depende de T006 y T007;
  - T009 depende de T002 y T008;
  - T010 depende de T008;
  - T011 depende de T006.
- **US2:**
  - T012 depende de T007;
  - T013 depende de T008;
  - T014 depende de T010, T011 y T012.
- **US3:** T015 depende de T009.
- **US4:** T016 depende de T007; T017 depende de T009 y T016.
- **Cierre:**
  - T018 depende de T002 y puede empezar con la API simulada;
  - T019 depende de US1 a US4;
  - T020 depende de T019;
  - T021 a T023, al final.

## Parallel Example

```text
Después de T003: T005 (DevolucionDeParcial) y T006 (Generación) a la vez; T004 en cuanto compile el contrato.
Después de T009: US2, US3 y US4 en cualquier orden, una a la vez porque comparten ProcesoAprobacionTest y FlujoContratoTest; T018 (la SPA) en paralelo.
```

## Implementation Strategy

- **MVP:** las fases 1 y 2 y US1 (T007 a T011): la devolución con sus observaciones, su bitácora y su concurrencia.
- **Después:** US2 (reenviar), US3 (la observación) y US4 (el rol).
- **Luego:** la pantalla, el script, SC-005 de F10-H03 y el cierre.
