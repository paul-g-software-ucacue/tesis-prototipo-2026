---
description: "Tareas de F12-H03: seguir los plazos de cada etapa"
---

# Tasks: Seguir los plazos de cada etapa

**Input**: documentos de diseño en `specs/057-plazos-etapas/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si no compila, primero va un esqueleto.

**Rutas**:
- `OCS/` es `backend/servicios/validacionocs/src/`;
- `NOR/` es `backend/servicios/normativa/src/`;
- `ACA/` es `backend/servicios/academico/src/`;
- `SPA/` es `frontend/src/`;
- `.../` es el paquete `ec/mdd/validacionocs/`, `ec/mdd/normativa/` o `ec/mdd/academico/`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede hacer en paralelo (otro archivo, sin depender de una tarea pendiente).
- **[Story]**: US1 (el aviso del plazo vencido), US2 (ver el plazo) o US3 (la advertencia de RN-23).

---

## Phase 1: Setup

- [X] T001 Escribir **D-114** en `docs/decisiones.md`, en la fila del índice y en su sección, con research §1 a §9:
  - el plazo se reconstruye con `EtapasAlCrearse` y no se guarda en la copia;
  - qué reinicia la llegada;
  - la revisión diaria con el arranque;
  - un aviso por llegada, con `plazo_vencido`;
  - a quién se avisa (el rol de la etapa y el de EN_REVISION_VRA);
  - RN-23 en Normativa y el inicio en Académico;
  - la advertencia no es un error del catálogo;
  - en el sistema no hay una fecha simulada.

  Revisar en el glosario `Plazo` y `PlazoDeEtapaVencido` (`sdd/modelo_dominio.md`). Si cambia el sentido de la pregunta 5 de §9.2 (queda confirmada), anotarlo en «Documentos que hay que poner al día».

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T004 y T005.

- [X] T002 [P] El contrato en `contratos/validacionocs/openapi.yaml`, según `contracts/validacionocs.md`:
  - `EstadoDelFlujo.plazo`, opcional, `PlazoDeLaEtapa`, con:
    - «`etapa` `TipoDeEtapa`»;
    - «`llegada` date-time»;
    - «`dias` integer ≥ 1»;
    - «`fechaLimite` date»;
    - «`diasRestantes` integer, negativo si venció»;
    - «`estado` enum `A_TIEMPO`, `VENCE_HOY`, `VENCIDO`»;
    - `additionalProperties: false`.
  - `EstadoDelFlujo.etapas`, una lista de `EtapaDelDistributivo`, con «`etapa`», «`nombre`», «`plazoDias` integer ≥ 1, ausente en PUBLICADO» y «`actual` boolean», `additionalProperties: false`.
  - `ConfiguracionDelFlujo.advertencias`, una lista de 0 o 1 `AdvertenciaDelFlujo`, con:
    - «`codigo` enum `RN23_MARGEN_INSUFICIENTE`, `RN23_SIN_NORMATIVA`»;
    - «`mensaje`»;
    - «`ultimoPlazo` date»;
    - «`inicioDelPeriodo` date»;
    - «`diasAntesDelInicio` integer ≥ 0, ausente con `RN23_SIN_NORMATIVA`»;
    - «`fechaDeRemision` date, ausente con `RN23_SIN_NORMATIVA`»;
    - «`diasDeMargen` integer, ausente con `RN23_SIN_NORMATIVA`»;
    - `additionalProperties: false`.
  - `cargarConfiguracionDelFlujo` documenta el 502 y el 504 también cuando Normativa no responde.

  Regenerar los tipos de la SPA (`npm run generar:api`).
- [X] T003 [P] Los contratos internos, según `contracts/interno.md`:
  - `contratos/academico/openapi.yaml`: `EstadoDelPeriodo` suma «`inicio` date», con `required: [periodo, estado, inicio]`;
  - `contratos/normativa/openapi.yaml`: `consultarRemisionDelPeriodo`, `GET /interno/normativa/periodos/{periodo}/remision`, `tags: [Interno]`, con 200 `RemisionDelPeriodo` («`version` integer ≥ 1», «`diasAntesDelInicio` integer ≥ 0», `additionalProperties: false`), 400, 401, 403, 404 `NoEncontrada` y 500, y la descripción de `contracts/interno.md`.
- [X] T004 Los objetos de valor del dominio, en `OCS/main/.../dominio/`, según `data-model.md`. Antes, ver fallar `OCS/test/.../dominio/PlazoTest.java`.
  - `EstadoDelPlazo` (`A_TIEMPO`, `VENCE_HOY`, `VENCIDO`).
  - `Plazo` (`etapa`, `llegada`, `dias`, `fechaLimite`):
    - `Plazo.de` rechaza «`dias` < 1» y PUBLICADO;
    - la fecha límite es la fecha de `llegada` en `ProcesoAprobacion.ZONA` (America/Guayaquil) más `dias`;
    - `en(hoy)` da el estado y los días que faltan («`fechaLimite − hoy`, negativo si venció»);
    - `diasDeAtraso(hoy)`.

    La prueba cubre una llegada a las 20:00 de Ecuador (01:00 UTC del día siguiente), los tres estados y el límite exacto.
  - `PlazosDeEtapas`: `de(TipoDeEtapa)` devuelve un `OptionalInt`, vacío en PUBLICADO y en una etapa inactiva; `sumaHasta(APROBADO)`.
- [X] T005 La llegada en el agregado. Antes, ver fallar en `OCS/test/.../dominio/ProcesoAprobacionTest.java`:
  - `llegadaALaEtapa()`: el momento de la última transición con «`desde ≠ hacia`» y «`hacia == estado()`»; sin ninguna, `distributivoCreadoEn()`.
    - Un envío de parcial que no es el último no la cambia; el último sí, por su `PASO_DE_ETAPA`.
    - La devolución, el rechazo en firma y la reforma la reinician.
    - La `VERSION_NUEVA` no la cambia.
  - `plazoActual(PlazosDeEtapas)`: vacío en PUBLICADO.
  - El plazo vencido no cambia `acciones(quien)` ni ninguna validación (FR-003): con una etapa vencida, las mismas acciones que sin vencer.

**Checkpoint**: los contratos y el dominio del plazo, en verde.

---

## Phase 3: User Story 1 - Recibir el aviso cuando vence el plazo de la etapa (Priority: P1) 🎯 MVP

**Goal**: cuando vence el plazo de la etapa actual, la revisión diaria avisa una vez al rol de la etapa y al Vicerrector, sin bloquear nada.

**Independent Test**:
1. Con el distributivo en EN_CONSOLIDACION y las transiciones corridas 6 días hacia atrás, se reinicia `distributivo`.
2. `vicerrectorado.demo` y quien consolida tienen una notificación «Venció el plazo de Consolidación».
3. Queda una fila en `plazo_vencido` y una entrada `PLAZO_VENCIDO` en la bitácora, y el envío a revisión sigue igual.

### Tests y dominio de US1

- [X] T006 [US1] `vencimiento` y destinos en el agregado. Antes, ver fallar en `OCS/test/.../dominio/ProcesoAprobacionTest.java`:
  - `OCS/main/.../dominio/VencimientoDePlazo.java` (`periodo`, `plazo`, `detectadoEn`, «`diasDeAtraso` ≥ 1»);
  - `vencimiento(PlazosDeEtapas, Instant ahora)`: presente solo si el plazo está `VENCIDO` en la fecha de `ahora` en Ecuador; vacío si vence hoy, si está a tiempo y en PUBLICADO;
  - `destinosDelVencimiento()`:
    - en BORRADOR_CARRERA, un destino `(rol de la etapa, carrera)` por cada parcial `PENDIENTE` o `DEVUELTO`, y ninguno por los `ENVIADO`;
    - en las demás etapas, `(rol de la etapa, sin carrera)`;
    - más el rol de EN_REVISION_VRA de la copia, sin carrera;
    - sin repetidos: en EN_REVISION_VRA queda uno solo;
    - con otro rol en EN_REVISION_VRA de la configuración, usa ese.
- [X] T007 [P] [US1] `OCS/main/.../dominio/MensajesDePlazo.java`. Antes, ver fallar `OCS/test/.../dominio/MensajesDePlazoTest.java`. Hay un mensaje por destino:
  - título: «Venció el plazo de <nombre de la etapa>»;
  - detalle: «El distributivo de <periodo> está en <nombre de la etapa> desde el <fecha de llegada>. Su plazo de <N> días venció el <fecha límite>; lleva <D> días de atraso.»;
  - enlace: `MensajesDeTransicion.enlaceDe(etapa)` para el rol de la etapa, y `/consolidado` para el rol de EN_REVISION_VRA.

  Las fechas van en formato de Ecuador, y el título no pasa de 200 caracteres ni el detalle de 1000 (`contratos/notificaciones`).

### Persistencia y puertos de US1

- [X] T008 [P] [US1] `OCS/main/resources/db/migration/validacionocs/V9__plazos.sql`, según `data-model.md`. Antes, ver fallar `ValidacionocsEsquemaIntegrationTest`:
  - la tabla `plazo_vencido` tiene:
    - «`id` uuid pk»;
    - «`periodo` varchar(20) not null»;
    - «`etapa` varchar(30) not null, check en las 8 etapas con plazo, sin PUBLICADO»;
    - «`llegada` timestamptz not null»;
    - «`plazo_dias` integer not null check ≥ 1»;
    - «`fecha_limite` date not null»;
    - «`detectado_en` timestamptz not null check ≥ `llegada`»;
    - «unique (`periodo`, `etapa`, `llegada`)»;
    - sin foránea a `proceso_aprobacion`.
  - `configuracion_flujo` suma «`advertencia` jsonb null» (la usa US3).

  La prueba rechaza PUBLICADO, `plazo_dias` 0 y una llegada repetida.
- [X] T009 [US1] Los puertos de salida, en `OCS/main/.../puertos/salida/`:
  - `RepositorioPlazos.registrar(VencimientoDePlazo)`, que devuelve `boolean`: `true` si insertó, `false` si ya estaba;
  - `AvisoDePlazoVencido.avisar(ProcesoAprobacion, VencimientoDePlazo)`, en la transacción en curso;
  - `RepositorioProceso.enCurso()`, que devuelve los periodos con un proceso que no está en PUBLICADO;
  - `CambioDeFlujo.Accion.PLAZO_VENCIDO`, con su JavaDoc: por `sistema`, el detalle `{periodo, etapa}` y el `nuevo` `{fechaLimite, diasDeAtraso}`.
- [X] T010 [US1] Los adaptadores, en `OCS/main/.../adaptadores/salida/`. Antes, ver fallar sus pruebas de integración en `OCS/integrationTest/`:
  - `RepositorioPlazosJdbc`, con `insert … on conflict (periodo, etapa, llegada) do nothing`: el número de filas dice si insertó;
  - `RepositorioProcesoJdbc.enCurso()`;
  - `PublicadorDeNotificaciones` implementa también `AvisoDePlazoVencido`: publica `validacionocs.NotificacionesSolicitadas` con `autor: "sistema"`, `momento` = `detectadoEn` y los mensajes de `MensajesDePlazo`. En BORRADOR_CARRERA nombra las carreras con `NombresDeCarreras`, como la devolución; si Académico no responde, usa el código.

### Aplicación y entrada de US1

- [X] T011 [US1] `EtapasAlCrearse.plazos(periodo, creadoEn)` en `OCS/main/.../aplicacion/EtapasAlCrearse.java`: los `PlazosDeEtapas` de la misma configuración que `de()` y `cadenaDelConsolidado()`. Antes, ver fallar su prueba: con una carga anterior a la creación que cambia un plazo, la usa; con una carga posterior, no.
- [X] T012 [US1] `OCS/main/.../puertos/entrada/RevisarPlazos.java` y `OCS/main/.../aplicacion/RevisarPlazosServicio.java`. Antes, ver fallar `OCS/test/.../aplicacion/RevisarPlazosServicioTest.java`, con Mockito en los puertos y un `Clock` fijo.

  `revisar()` recorre `enCurso()`. Para cada periodo:
  1. calcula el vencimiento con `EtapasAlCrearse.plazos` y el reloj; sin vencimiento, sigue con el siguiente;
  2. pregunta a `DatosDelPeriodo.estado`: si el periodo está cerrado, o si Académico no responde (`WARN`), sigue con el siguiente;
  3. con `Transaccion` y `RepositorioProceso.bloquear`, vuelve a calcular el vencimiento; si cambió la etapa o la llegada, no hace nada;
  4. `RepositorioPlazos.registrar`;
  5. solo si devolvió `true`, `AvisoDePlazoVencido.avisar` y `RegistroDeCambios.registrar` con `PLAZO_VENCIDO`.

  Un error con un periodo se registra y no detiene a los demás. Devuelve cuántos vencimientos nuevos registró.

  Pruebas:
  - el orden;
  - el periodo cerrado;
  - Académico caído;
  - la etapa que cambió entre medio;
  - un `registrar` que devuelve `false`, sin aviso ni bitácora;
  - un periodo que falla y otro que sigue;
  - vence hoy, sin aviso.
- [X] T013 [US1] `OCS/main/.../adaptadores/entrada/RevisionDiariaDePlazos.java`:
  - `@Scheduled(cron = "${mdd.validacionocs.plazos.revision:0 0 7 * * *}", zone = "America/Guayaquil")`;
  - `@EventListener(ApplicationReadyEvent.class)`;
  - las dos llaman a `RevisarPlazos.revisar()` y registran en `INFO` cuántos vencimientos hubo.

  Beans en `OCS/main/.../infraestructura/ConfiguracionValidacionocs.java` (con `Clock.systemUTC()`). En `backend/apps/distributivo/src/main/resources/application.yaml` va `mdd.validacionocs.plazos.revision: ${MDD_REVISION_DE_PLAZOS:0 0 7 * * *}`, con su comentario (`contracts/validacionocs.md`).

  `ArquitecturaTest` y `ModulosTest` siguen en verde.
- [X] T014 [US1] `OCS/integrationTest/.../RevisarPlazosIntegrationTest.java`, con PostgreSQL real y un `Clock` fijo:
  - con un proceso en EN_CONSOLIDACION vencido, tres `revisar()` dejan una fila en `plazo_vencido`, un evento `validacionocs.NotificacionesSolicitadas` y una entrada `PLAZO_VENCIDO`;
  - dos `revisar()` a la vez dejan una fila y un evento;
  - después de una devolución y otra llegada vencida, hay una segunda fila y un segundo evento (un aviso por llegada).

**Checkpoint**: el vencimiento se detecta, se avisa una vez y no bloquea nada.

---

## Phase 4: User Story 2 - Ver el plazo de cada etapa (Priority: P1)

**Goal**: la tarjeta del estado del flujo muestra la llegada, la fecha límite y si vence o venció, y el plazo de cada etapa activa.

**Independent Test**: con el distributivo en EN_REVISION_VRA, la tarjeta de «Consolidado del periodo» dice «Faltan 5 días» y lista las etapas activas con sus días. Con el plazo vencido, dice «Vencido hace N días», con texto.

- [X] T015 [US2] La vista, en `OCS/main/.../puertos/entrada/FlujoDelDistributivo.java`:
  - suma `Optional<PlazoDeLaEtapa> plazo` (`etapa`, `llegada`, `dias`, `fechaLimite`, `diasRestantes`, `estado`);
  - suma `List<EtapaDelFlujo> etapas` (`etapa`, `nombre`, `OptionalInt plazoDias`, `actual`), con las activas en orden;
  - con constructores que las dejan vacías, como `reforma`.

  `FlujoDelDistributivoServicio.vista` las calcula con `EtapasAlCrearse.plazos` y su `Clock`. Antes, ver fallar `FlujoDelDistributivoServicioTest`: los tres estados con un reloj fijo, sin `plazo` en PUBLICADO, y `etapas` con la actual marcada y PUBLICADO sin días.
- [X] T016 [US2] `OCS/main/.../adaptadores/entrada/FlujoControlador.java`: traduce `plazo` y `etapas` al contrato. Antes, ver fallar `FlujoContratoTest`: el cuerpo valida contra `PlazoDeLaEtapa` y `EtapaDelDistributivo`, en `A_TIEMPO` y en `VENCIDO` con `diasRestantes` negativo, y sin `plazo` en PUBLICADO.
- [X] T017 [P] [US2] La tarjeta, en `SPA/pantallas/comun/EstadoDelFlujo.tsx`, según `contracts/spa.md`. Antes, ver fallar `SPA/pantallas/comun/EstadoDelFlujo.test.tsx`.
  - **«Plazo de la etapa»:** «En <etapa> desde el <llegada>. Fecha límite: <fechaLimite>.», con:
    - «Faltan N días» o «Falta 1 día»;
    - «Vence hoy.»;
    - «Vencido hace N días.», con un ícono `aria-hidden` y «Plazo vencido» en `<strong>`.

    No se muestra sin `plazo`.
  - **«Etapas del flujo»:** una `<ol>` de «<nombre> · <plazoDias> días», con PUBLICADO sin días y la actual con `aria-current="step"` y el texto «actual».

  Contraste de 4.5:1 en los dos temas; el color no reemplaza el texto.
- [X] T018 [US2] Playwright en `frontend/e2e/consolidado.spec.ts`: la tarjeta con `plazo` `VENCIDO` y con `A_TIEMPO`, simulados en la respuesta, y axe-core sin fallas.

**Checkpoint**: el plazo se ve en las tres pantallas que muestran la tarjeta.

---

## Phase 5: User Story 3 - La advertencia de RN-23 al cargar la configuración (Priority: P2)

**Goal**: al cargar la configuración, si el último plazo queda a menos de los días de RN-23 antes del inicio del periodo, la carga queda vigente y advierte, y la consulta conserva esa advertencia.

**Independent Test**: con un periodo que empieza dentro de 60 días y la versión de normativa, cargar la plantilla responde 200 con `RN23_MARGEN_INSUFICIENTE` y 13 días de menos. Con 120 días, sin advertencia. Sin versión, `RN23_SIN_NORMATIVA`.

### Normativa y Académico

- [X] T019 [P] [US3] `NOR/main/resources/db/migration/normativa/V7__plazo_de_remision.sql`, como V6:
  - la regla `RN-23` sin severidad, en cada versión que no la tiene;
  - el parámetro «`dias_antes_del_inicio` = 30» (POL 5.2);
  - un `VERSION_EDITADA` de `sistema` por versión, con `{campo: 'RN-23.dias_antes_del_inicio', anterior: null, nuevo: '30'}` (D-68).

  Antes, ver fallar la prueba de esquema o de migración de Normativa: cada versión tiene RN-23 con 30, y una versión nueva lo copia (`RepositorioVersionesJdbc`).
- [X] T020 [US3] `NOR/main/.../aplicacion/ConsultarRemisionDelPeriodoServicio.java` y la ruta `remision` en `NOR/main/.../adaptadores/entrada/InternoNormativaControlador.java`. La cifra es de la versión que rige el periodo, y un periodo sin versión responde 404 `NoEncontrada`, como las demás rutas internas. Antes, ver fallar `InternoNormativaContratoTest` (con versión y sin versión) y la prueba del servicio.
- [X] T021 [P] [US3] `ACA/main/.../adaptadores/entrada/InternoAcademicoControlador.java`: `consultarEstadoDelPeriodo` responde `inicio`, el de `PeriodoAcademico`. Antes, ver fallar `InternoAcademicoContratoTest`.

### Validación OCS

- [X] T022 [US3] `OCS/main/.../dominio/AdvertenciaDeRemision.java`, con `CodigoDeAdvertencia`. Antes, ver fallar `OCS/test/.../dominio/AdvertenciaDeRemisionTest.java`.

  `de(hoy, configuracion, inicio, OptionalInt dias)`:
  - «`ultimoPlazo` = la fecha de la carga más `PlazosDeEtapas.sumaHasta(APROBADO)`»;
  - «`fechaDeRemision` = `inicio − dias`»;
  - vacío si «`ultimoPlazo ≤ fechaDeRemision`»;
  - `RN23_MARGEN_INSUFICIENTE`, con `diasDeMargen` negativo, si es posterior;
  - `RN23_SIN_NORMATIVA` si `dias` está vacío.

  El mensaje en español dice qué hacer («Acorta plazos o desactiva etapas opcionales»).

  Pruebas:
  - la plantilla (43 días) con un inicio a 60 días: −13;
  - a 120 días: vacío;
  - el límite exacto: vacío;
  - un inicio que ya pasó;
  - sin normativa;
  - una etapa opcional desactivada que acorta la suma.

  Con `PlazosDeEtapas.de(ConfiguracionFlujo)`, si T004 no lo dejó.
- [X] T023 [US3] Los datos del periodo, en `OCS/main/.../dominio/EstadoDelPeriodo.java` y `OCS/main/.../puertos/salida/DatosDelPeriodo.java`:
  - `EstadoDelPeriodo` suma `Optional<LocalDate> inicio`, sin romper sus usos;
  - `DatosDelPeriodo` suma `OptionalInt diasDeRemision(periodo)`.

  En `OCS/main/.../adaptadores/salida/DatosDesdeProcesos.java`:
  - lee `inicio` como opcional (D-55);
  - `diasDeRemision` usa `ConsultasEntreProcesos` contra `/interno/normativa/periodos/{periodo}/remision`, y un 404 es vacío.

  Antes, ver fallar su prueba, con un servidor simulado como las demás de `DatosDesdeProcesos`: con versión, sin versión, y `datos` sin respuesta (`COMUN_PROCESO_SIN_RESPUESTA`).
- [X] T024 [US3] La carga y la consulta. Antes, ver fallar `CargarConfiguracionServicioTest`, `ConsultarConfiguracionServicioTest` y `RepositorioConfiguracionIntegrationTest`.
  - **`CargarConfiguracionServicio`:**
    - después de validar el archivo, calcula `AdvertenciaDeRemision` con la fecha de `ahora` en Ecuador, el `inicio` y `diasDeRemision`;
    - sin `inicio`, no la calcula y anota un `WARN`;
    - Normativa caída responde la excepción común y no guarda nada;
    - guarda la advertencia con la carga, y `CONFIGURACION_FLUJO_CARGADA` suma `advertencia` (el código) en `nuevo`.
  - **`RepositorioConfiguracion.guardar` y `CargaGuardada`:** llevan la advertencia, en `configuracion_flujo.advertencia` (jsonb, nula sin advertencia), y `RepositorioConfiguracionJdbc` la lee y la escribe.
  - **`ConfiguracionVigente`:** suma `Optional<AdvertenciaDeRemision> advertencia`. `ConsultarConfiguracionServicio` la devuelve de la carga vigente; con la configuración por defecto, va vacía.
- [X] T025 [US3] `OCS/main/.../adaptadores/entrada/ConfiguracionControlador.java`: `advertencias` en las dos respuestas. Antes, ver fallar `ConfiguracionContratoTest`:
  - la carga con `RN23_MARGEN_INSUFICIENTE` responde 200 y la consulta la devuelve;
  - `RN23_SIN_NORMATIVA`;
  - sin advertencias con margen y con la configuración por defecto;
  - el 504 si Normativa no responde.
- [X] T026 [P] [US3] El aviso, en `SPA/pantallas/flujo/Flujo.tsx`, según `contracts/spa.md`:
  - después de cargar y en «Qué rige», con `role="status"`, el título «Advertencia de RN-23», el `mensaje`, el último plazo, la fecha de RN-23 y los días de margen si los hay;
  - el texto dice que la configuración quedó vigente.

  Antes, ver fallar `SPA/pantallas/flujo/Flujo.test.tsx`: las dos advertencias, y sin aviso cuando no hay. Playwright en `frontend/e2e/flujo.spec.ts`, con axe-core.

**Checkpoint**: la advertencia se calcula, se guarda y se muestra; la carga nunca se rechaza por ella.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T027 `deploy/probar_avance.sh`: los pasos P1 a P8 de `contracts/demostracion.md`, al final, antes de limpiar.
  - Correr hacia atrás 6 días todas las transiciones de 2026-I, con `psql` como superusuario.
  - Reiniciar `distributivo` con `identidad` detenido.
  - Una notificación por usuario, la entrada `PLAZO_VENCIDO`, `VENCIDO` en la tarjeta y las acciones sin cambios.
  - Si un paso falla, levantar `identidad` y `distributivo` antes de salir. La limpieza borra las filas de `plazo_vencido` de 2026-I.
  - Actualizar el comentario del encabezado.
  - Medir P4 y P8, y decidir si P8 se queda.
- [X] T028 [P] `deploy/probar_flujo.sh`: los pasos R1 a R3 de `contracts/demostracion.md`, con periodos de prueba registrados con fechas relativas a hoy; R1 y R2 se abren con la versión de normativa sembrada. Revisar que los pasos que ya existen no comparen el cuerpo completo (ahora traen `RN23_SIN_NORMATIVA`).
- [X] T029 Correr el build completo (`./gradlew build integrationTest`), la SPA (`npm run tipos`, `lint`, `formato:verificar`, `test`) y Playwright. Comprobar la cobertura (mínimo 60 %, D-32), Gitleaks sobre `main..HEAD` y los dos scripts contra el perfil local (`quickstart.md` §1 y §2).
- [X] T030 [P] Revisar el JavaDoc (principio XII):
  - por qué el plazo se reconstruye y no se guarda en la copia;
  - qué reinicia la llegada;
  - por qué hay un aviso por llegada y el bloqueo antes de insertar;
  - a quién se avisa;
  - por qué la advertencia no rechaza ni es un error del catálogo.
- [X] T031 El recorrido del autor, `quickstart.md` §3, con el teclado y un lector de pantalla (SC-005, SC-007).
- [X] T032 Crear `specs/057-plazos-etapas/evidencia.md`, como la de 056: las pruebas, los scripts, el recorrido, las desviaciones y los criterios de terminado. La sección del equipo de demostración queda para después de fusionar.
- [X] T033 Fusionar solo con la aprobación del autor y todas las comprobaciones en verde.
  - Verificar la demo en solo lectura (`quickstart.md` §4): «Sano», V9 de Validación OCS y V7 de Normativa aplicadas, y el aviso real del vencimiento de 2026-I, una sola vez.
  - Abrir el PR de evidencia con `Closes #58`.
  - Con #58 cerrada, cerrar la épica F12 (#11) y el milestone «Ola 3 · Aprobado y firmado» si el autor lo aprueba.

  **Resultado (2026-11-20):** PR #253 fusionado (`d31bf16`) con las 12 comprobaciones en verde, después de corregir el conteo de reglas de `probar_normativa.sh` (`b9731b1`). La demo tomó `d31bf16`, «Sano en 57 s», con V9 de Validación OCS y V7 de Normativa aplicadas; registró el vencimiento real de 2026-I en EN_CONSOLIDACION y avisó una vez a `consolidacion.demo`, `docente.demo` y `vicerrectorado.demo`. PR de evidencia con `Closes #58`.

---

## Dependencies & Execution Order

- **Setup:** T001 antes de todo.
- **Fundacional:** T002, T003 y T004 en paralelo; T005 después de T004.
- **US1:**
  - T006 después de T005;
  - T007 después de T006;
  - T008 sin dependencias;
  - T009 después de T006;
  - T010 después de T007, T008 y T009;
  - T011 después de T004;
  - T012 después de T009 y T011;
  - T013 después de T012;
  - T014 después de T010 y T013.
- **US2:**
  - T015 después de T005 y T011;
  - T016 después de T002 y T015;
  - T017 después de T002;
  - T018 después de T016 y T017.
- **US3:**
  - T019 sin dependencias;
  - T020 después de T003 y T019;
  - T021 después de T003;
  - T022 después de T004;
  - T023 después de T020 y T021;
  - T024 después de T008, T022 y T023;
  - T025 después de T002 y T024;
  - T026 después de T002.
- **Cierre:**
  - T027 después de US1 y US2;
  - T028 después de US3;
  - T029 después de T027 y T028;
  - T030 en paralelo con T029;
  - T031 después de T029;
  - T032 después de T029 y T031;
  - T033 al final.

US1 y US2 comparten el dominio del plazo (T004, T005 y T011), pero se prueban por separado. US3 no depende de US1 ni de US2: solo usa `PlazosDeEtapas` (T004) y la columna de V9 (T008).

### Parallel Opportunities

```text
T002, T003 y T004.
T007 y T008, una vez hecho T006.
T011 en paralelo con T006 a T010.
T017 (SPA) y T026 (SPA) en paralelo con el backend, una vez hecho T002.
T019 y T021 (Normativa y Académico) en cualquier momento después de T003.
T028 en paralelo con T027; T030 en paralelo con T029.
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. La fundación: T001 a T005.
2. US1: T006 a T014. Con eso, el plazo vencido ya avisa una vez y no bloquea.
3. Validar con `RevisarPlazosIntegrationTest` y los pasos P1 a P8 del script.

### Incremental Delivery

1. US1, el aviso.
2. US2, el plazo en la tarjeta.
3. US3, la advertencia de RN-23.
4. El cierre: los scripts, el build, el recorrido, la evidencia y la fusión.

Una sola rama (`057-plazos-etapas`) y un solo PR, como en las historias anteriores. El PR principal lleva `Refs #58`, y el de evidencia, `Closes #58`.
