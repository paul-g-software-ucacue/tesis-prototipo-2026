---

description: "Tareas de F12-H02: avanzar el distributivo según el rol de la etapa"
---

# Tasks: Avanzar el distributivo según el rol de la etapa

**Input**: documentos de diseño en `specs/041-avanzar-etapas/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si una prueba no compila, se agrega primero un esqueleto, y la implementación va en un comando aparte.

**Decisiones**: las aclaraciones Q1 y Q2 del autor del 2026-11-07 (en la spec); D-95, escrita en [`docs/decisiones.md`](../../docs/decisiones.md) el 2026-11-07; MD-04, ADR-007, D-16, D-64, D-89, D-90 y D-94; RF-34 y RF-44.

**Organization**:
- **Fundacional:** el contrato, los errores, la migración, el agregado `ProcesoAprobacion` y la interfaz publicada de Generación con el hash.
- **US1:** enviar el parcial (MVP).
- **US2:** el paso automático a la siguiente etapa activa.
- **US3:** solo el rol de la etapa la mueve, y el avance genérico de BORRADOR_UNIDADES.
- **Cierre:** la pantalla, el script, la evidencia y la fusión.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US3.
- **Rutas:** relativas a la raíz. Abreviaturas, cada una con `main` y `test` en `java/ec/mdd/<servicio>` e `integrationTest` donde aplica:
  - `GEN` = `backend/servicios/generacion/src`;
  - `VAL` = `backend/servicios/validacionocs/src`;
  - `SPA` = `frontend/src`.

---

## Phase 1: Setup

- [X] T001 Revisar el glosario (`~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/sdd/modelo_dominio.md` §5 y §7): `ProcesoAprobacion`, `Transicion`, `Etapa`, «Parcial de carrera», «Estado del distributivo» y «Esqueleto fijo» ya están. Confirmar que «Avance genérico» y «Hash del contenido» no son términos del negocio nuevos. Si alguno lo es, proponerlo al autor para el glosario y anotarlo en `evidencia.md`. Anotar el resultado aquí. **Resultado (2026-11-07):** describen operaciones, no conceptos del negocio; no se agregan al glosario.
- [X] T002 Agregar `implementation project(':servicios:generacion')` en `backend/servicios/validacionocs/build.gradle`, con un comentario (usa su interfaz publicada, MD-04), y comprobar con las pruebas de Spring Modulith de `backend/apps/distributivo` que Validación OCS solo usa `ec.mdd.generacion.puertos.entrada` y que Generación no depende de Validación OCS.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T005.

- [X] T003 Agregar a `contratos/validacionocs/openapi.yaml`, según `contracts/validacionocs.md`, la etiqueta `Flujo` y las operaciones `consultarFlujo` (`GET /api/validacionocs/periodos/{periodo}/flujo`, `x-mdd-permiso: DISTRIBUTIVO:LECTURA`), `enviarParcial` (`POST …/parciales/{carrera}/envio`, `x-mdd-permiso: OFERTA:APROBACION`) y `avanzarEtapa` (`POST …/avance`, sin `x-mdd-permiso`), sin cuerpo de petición, con `periodo` `^[A-Za-z0-9._-]{1,20}$` y `carrera` `^[0-9A-Z_-]{1,20}$`, en la etiqueta nueva `Aprobacion` (la etiqueta `Flujo` ya agrupa la configuración de F12-H01 y su interfaz la implementa `ConfiguracionControlador`), y los esquemas `EstadoDelFlujo`, `ParcialDelFlujo`, `TransicionDelFlujo` (`hash` `^[0-9a-f]{64}$`) y `AccionDelFlujo` (`ENVIAR_PARCIAL`, `AVANZAR`). Regenerar `SPA/api/generado/validacionocs.d.ts` y compilar `:servicios:validacionocs`.
- [X] T004 Agregar a `contratos/comun/errores.md`, con sus 8 columnas, `VALIDACIONOCS_SIN_DISTRIBUTIVO` (404), `VALIDACIONOCS_ETAPA_NO_ADMITE_ENVIO` (409), `VALIDACIONOCS_PARCIAL_YA_ENVIADO` (409), `VALIDACIONOCS_ETAPA_CON_OPERACION_PROPIA` (409) y `VALIDACIONOCS_PERIODO_CERRADO` (409) (research §10), y sus entradas en `VAL/main/.../adaptadores/entrada/ErroresValidacionocs.java`, con la prueba de `ErroresValidacionocsTest`.
- [X] T005 Crear `VAL/test/.../dominio/ProcesoAprobacionTest.java`, con esqueleto, y verlo fallar, una prueba por regla de `data-model.md`:
  - un proceso nuevo está en BORRADOR_CARRERA con todos los parciales `PENDIENTE`;
  - `siguienteActiva` salta las etapas con `activa = false` (BORRADOR_UNIDADES desactivada → EN_CONSOLIDACION; activa → BORRADOR_UNIDADES);
  - `enviarParcial` exige `estado == BORRADOR_CARRERA` (`EtapaNoAdmiteEnvio`), el rol de BORRADOR_CARRERA de la copia de la configuración (`RolDeOtraEtapa`), la carrera propia si hay alcance propio (`ParcialDeOtraCarrera`) y el parcial `PENDIENTE` (`ParcialYaEnviado`);
  - el envío deja una `Transicion` `ENVIO_PARCIAL` con `desde = hacia = BORRADOR_CARRERA`, la carrera, la versión, el hash `^[0-9a-f]{64}$`, quién y cuándo; el último envío deja además una `PASO_DE_ETAPA`;
  - un intento rechazado no deja transición.
  Después crear en `VAL/main/.../dominio/` `ProcesoAprobacion`, `Transicion`, `EtapaDelProceso`, `EstadoDeEnvio` y los cinco errores, en un comando aparte.
- [X] T006 [P] Crear `GEN/test/.../dominio/ContenidoDelParcialTest.java`, y verlo fallar: el hash de un parcial de prueba es un valor fijo escrito en la prueba; cambiar el docente, el texto de la justificación o las horas lo cambia; el orden de las propuestas y de las actividades no lo cambia; quién y cuándo no lo cambian; el hash del distributivo depende de los `{carrera, hash}` ordenados (research §4). Después crear `GEN/main/.../dominio/ContenidoDelParcial.java` (JSON canónico: claves ordenadas, sin espacios, UTF-8; horas con dos decimales; SHA-256 en hexadecimal) en un comando aparte.
- [X] T007 Crear `VAL/main/resources/db/migration/validacionocs/V3__proceso_aprobacion.sql` según `data-model.md`: `proceso_aprobacion` (`periodo varchar(20)` pk, `estado` con `check` de los 9 tipos, `etapas jsonb`, `creado_en`), `parcial_del_proceso` (pk `(periodo, carrera)`, `carrera varchar(20)`, `estado in ('PENDIENTE','ENVIADO')`) y `transicion` (solo de inserción, `hash char(64)`, índice `(periodo, momento)`), con su prueba en `VAL/integrationTest/.../ValidacionocsEsquemaIntegrationTest.java`.

**Checkpoint**: el contrato compila, y el dominio y el hash pasan sus pruebas.

---

## Phase 3: User Story 1 - Enviar el parcial de mi carrera (Priority: P1) 🎯 MVP

**Goal**: el coordinador envía el parcial de su carrera; queda en solo lectura y la transición va a la bitácora.

**Independent Test**: con `coordinacion.demo`, enviar su parcial y ver la entrada `PARCIAL_ENVIADO` con quién, cuándo, desde, hacia, versión y hash; después, una propuesta en su carrera responde `GENERACION_PARCIAL_NO_EDITABLE` (quickstart §3, pasos 1 a 3).

- [X] T008 [US1] En Generación, crear la interfaz publicada `GEN/main/.../puertos/entrada/ParcialesDelDistributivo.java` con `EstadoDelDistributivo`, `ParcialMarcado` y `ParcialNoEncontrado` (`contracts/generacion.md`), y su prueba de aplicación `GEN/test/.../aplicacion/ParcialesDelDistributivoServicioTest.java` (Mockito), viéndola fallar: `marcarParcialEnviado` bloquea la versión vigente, deja el parcial `ENVIADO` y devuelve la versión y el hash de `ContenidoDelParcial`; si ya estaba enviado, devuelve el mismo hash sin guardar; **nunca** llama a `RegistroDeCambios` (D-95). Después `GEN/main/.../aplicacion/ParcialesDelDistributivoServicio.java`, `VersionDistributivo.conParcialEnviado` y el método del repositorio en `GEN/main/.../adaptadores/salida/RepositorioDistributivosJdbc.java`, con su prueba de integración (el estado queda `ENVIADO`, y proponer, quitar una propuesta, asignar una actividad y quitar una actividad sobre ese parcial responden `GENERACION_PARCIAL_NO_EDITABLE`, FR-005), en un comando aparte. **Resultado:** las cuatro operaciones de FR-005 ya tenían su prueba con el parcial enviado (`dadoUnParcialEnviado_…` en `ProponerDocenteServicioTest` y `AsignarActividadServicioTest`). `EstadoDelDistributivo` expone `enviado` como booleano y no el `EstadoDelParcial` del dominio, para que la API publicada no comparta clases internas (Spring Modulith).
- [X] T009 [US1] Crear en `VAL/main/.../puertos/salida/` `RepositorioProceso` (con `bloquear(periodo)`) y `Distributivo` (lo que Validación OCS necesita de Generación), y `VAL/integrationTest/.../RepositorioProcesoIntegrationTest.java` con PostgreSQL real, viéndolo fallar: guarda y lee el proceso con su copia de etapas, sus parciales y sus transiciones en orden; `bloquear` toma la fila con `FOR UPDATE`; un proceso con otro `distributivo_creado_en` que el del distributivo actual se reemplaza por uno nuevo (U1 del análisis, research §5). Después `VAL/main/.../adaptadores/salida/RepositorioProcesoJdbc.java` y `DistributivoDesdeGeneracion.java` (adaptador sobre `ParcialesDelDistributivo`), en un comando aparte.
- [X] T010 [US1] Crear `VAL/test/.../aplicacion/EnviarParcialServicioTest.java` (Mockito), viéndolo fallar:
  - crea el proceso la primera vez con la configuración que regía al crearse el distributivo: la última carga con `cargada_en` anterior a `creadoEn`, o la por defecto (research §5);
  - el orden de las dos transacciones: primero `marcarParcialEnviado`, después registrar la transición y publicar `PARCIAL_ENVIADO` (`contracts/bitacora.md`) en la transacción de Validación OCS;
  - si registrar falla, un segundo envío lo completa con el mismo hash (research §2), y mientras tanto `consultarFlujo` muestra el parcial con `pendienteDeConfirmar = true`;
  - un periodo cerrado responde `PeriodoCerrado` sin llamar a Generación;
  - un rechazo del dominio no publica nada.
  Después `VAL/main/.../aplicacion/EnviarParcialServicio.java`, `ConsultarFlujoServicio.java` (con las `acciones` del usuario, FR-014) y la acción `PARCIAL_ENVIADO` en `CambioDeFlujo`, en un comando aparte. **Resultado:** un solo `FlujoDelDistributivoServicio` implementa `ConsultarFlujo`, `EnviarParcial` y `AvanzarEtapa`, con `EtapasAlCrearse` (principio XI: comparten la carga y la creación del proceso). El dominio suma `validarEnvio`, que el caso de uso llama antes de tocar Generación. Se agregó `VALIDACIONOCS_PARCIAL_NO_ENCONTRADO` (404) para una carrera sin parcial, que antes habría sido un 500.
- [X] T011 [US1] Crear `VAL/test/.../adaptadores/entrada/FlujoContratoTest.java`, viéndolo fallar, según `contracts/validacionocs.md` §«Pruebas de contrato» para `consultarFlujo` y `enviarParcial`. Incluye que `consultarFlujo` responde 200 con `DISTRIBUTIVO:LECTURA`, con `OFERTA:LECTURA` y con `PROYECTOS:LECTURA`, cada uno por separado, y 403 sin ninguno (I1 del análisis). Después `VAL/main/.../adaptadores/entrada/FlujoControlador.java`: los roles del usuario de `ROLE_<rol>`, la carrera de `AlcanceDeCarrera` (`COMUN_CARRERA_SIN_INDICAR` si falta), y `RolDeOtraEtapa` y `ParcialDeOtraCarrera` a `AccessDeniedException` (403 común). Sumar las dos operaciones a `PermisosValidacionocsTest`. Registrar los casos de uso en `VAL/main/.../infraestructura/ConfiguracionValidacionocs.java`.

**Checkpoint**: el coordinador envía su parcial por la API, con su entrada en la bitácora.

---

## Phase 4: User Story 2 - Pasar a la siguiente etapa cuando todas enviaron (Priority: P1)

**Goal**: el último envío lleva el distributivo a la siguiente etapa activa, una sola vez.

**Independent Test**: con dos carreras, el primer envío deja BORRADOR_CARRERA; el último deja EN_CONSOLIDACION con una `ETAPA_AVANZADA` (quickstart §3, paso 5).

- [X] T012 [US2] Agregar a `EnviarParcialServicioTest` y ver fallar: el último envío pide `hashDelDistributivo` y publica también `ETAPA_AVANZADA` con quién envió el último parcial; con BORRADOR_UNIDADES activa, el paso va a ella. Después completar `EnviarParcialServicio` y la acción `ETAPA_AVANZADA` en `CambioDeFlujo`, en un comando aparte.
- [X] T013 [US2] Agregar a `RepositorioProcesoIntegrationTest` dos envíos simultáneos de los dos últimos parciales (dos hilos, PostgreSQL real): un solo `PASO_DE_ETAPA` y el estado final en la siguiente etapa (SC-004, research §8). **Resultado:** en `RepositorioProcesoIntegrationTest`, con dos hilos: un solo `PASO_DE_ETAPA`.

**Checkpoint**: US1 y US2 cumplen los dos primeros escenarios del issue.

---

## Phase 5: User Story 3 - Solo el rol de la etapa la mueve (Priority: P1)

**Goal**: el 403 con otro rol en cualquier operación que mueve el distributivo, y el avance genérico solo en BORRADOR_UNIDADES.

**Independent Test**: con BORRADOR_UNIDADES activa, el Gestor de unidad la avanza y cualquier otro rol recibe 403; en EN_CONSOLIDACION, el avance genérico responde 409.

- [X] T014 [US3] Agregar a `ProcesoAprobacionTest` y ver fallar: `avanzar` exige el rol de la etapa actual (`RolDeOtraEtapa`); en BORRADOR_CARRERA, EN_CONSOLIDACION, EN_REVISION_VRA, CONGELADO_EN_FIRMA, EN_OCS, REMITIDO_ZONAL, APROBADO y PUBLICADO lanza `EtapaConOperacionPropia`; en BORRADOR_UNIDADES deja un `PASO_DE_ETAPA` a la siguiente activa; `puedeAvanzar` y las acciones del usuario. Después completar `ProcesoAprobacion`, en un comando aparte.
- [X] T015 [US3] Crear `VAL/test/.../aplicacion/AvanzarEtapaServicioTest.java`, viéndolo fallar, y después `VAL/main/.../aplicacion/AvanzarEtapaServicio.java` (con `hashDelDistributivo` y `ETAPA_AVANZADA`) y la operación `avanzarEtapa` en `FlujoControlador`, con sus casos en `FlujoContratoTest`.
- [X] T016 [US3] Agregar a `FlujoContratoTest` una tabla de rol, operación y resultado: el 403 de `enviarParcial` (etapa BORRADOR_CARRERA) y de `avanzarEtapa` (etapa BORRADOR_UNIDADES activa) para cada uno de los 9 roles de la matriz D-16 que no son el de la etapa, y el 200 del rol de la etapa, con los permisos leídos de `backend/servicios/roles/src/main/resources/db/migration/roles/V2__matriz_d16.sql` (como `BitacoraContratoTest`), el mismo cuerpo en todos y sin el rol que falta (SC-002). **Resultado:** en `FlujoContratoTest`, con los permisos de cada rol leídos de la migración de Roles (archivo del repositorio: Validación OCS no depende de Roles). `PermisosValidacionocsTest` verifica solo el `@PreAuthorize`, con el caso de uso fijo.

**Checkpoint**: las tres historias funcionan por la API.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T017 [P] Crear `SPA/pantallas/comun/EstadoDelFlujo.tsx` y su prueba `EstadoDelFlujo.test.tsx` (primero la prueba), según `contracts/spa.md`: la etapa y la versión; cada parcial «Pendiente» o «Enviado por … el …»; un botón por cada acción; «Enviado, falta confirmar» con «Reenviar»; la confirmación antes de enviar; los errores con `AvisoDeError`. Usarlo en `SPA/pantallas/oferta/Oferta.tsx` (después de enviar, la oferta de su carrera en solo lectura), `SPA/pantallas/consolidado/Consolidado.tsx` y `SPA/pantallas/proyectos/Proyectos.tsx`, con sus pruebas. Crear `frontend/e2e/flujo-envio.spec.ts` con axe-core.
- [ ] T018 Crear `deploy/probar_avance.sh` con los 8 pasos de `contracts/demostracion.md`, sobre los datos de prueba y con la base limpia al terminar (también `validacionocs.proceso_aprobacion`, `parcial_del_proceso` y `transicion`), sin imprimir claves ni tokens. Agregar su paso a `.github/workflows/perfil-consolidado.yml` después de «Consulta de la bitácora». Pasar shellcheck. Hacer que **todos** los scripts existentes que crean y borran distributivos (`probar_bitacora.sh`, `probar_consulta_bitacora.sh` y los demás que borran `generacion.distributivo`) borren también `validacionocs.transicion`, `parcial_del_proceso` y `proceso_aprobacion` de sus periodos, y correrlos en su orden sobre un perfil nuevo (U1 del análisis).
- [ ] T019 Revisar el JavaDoc de `ProcesoAprobacion`, `Transicion`, `ParcialesDelDistributivo`, `ContenidoDelParcial` y los servicios (principio XII); el de `marcarParcialEnviado` dice que no registra en Generación (D-95).
- [ ] T020 Crear `specs/041-avanzar-etapas/evidencia.md`, como `specs/040-consultar-bitacora/evidencia.md`: pruebas unitarias e `integrationTest` completos, cobertura por módulo (60 %, D-32), Spotless, SpotBugs, la SPA (tipos, ESLint, Prettier, Vitest y Playwright), los scripts de `perfil-consolidado` en su orden sobre un perfil nuevo, shellcheck, Gitleaks y los criterios de terminado de la constitución §6.
- [ ] T021 Fusionar solo con la aprobación del autor y con todas las comprobaciones en verde. Verificar el despliegue en el equipo de demostración (GitHub `demostracion` y los 8 contenedores sanos con la versión nueva, y la migración `V3` en `validacionocs`). Comprobación del autor con `quickstart.md` §4, con el cronómetro de SC-005 (enviar el parcial en menos de 30 s). PR de evidencia con `Closes #57`, y borrar las ramas.

---

## Dependencies & Execution Order

- **Setup:** T001 y T002 antes de T003.
- **Fundacional:** T003 y T004 antes del código. T005 y T006 no dependen entre sí. T007 antes de T009.
- **US1:** T008 depende de T006. T009 depende de T005, T007 y T008. T010 depende de T009. T011 depende de T003, T004 y T010.
- **US2:** T012 depende de T010. T013 depende de T009 y T012.
- **US3:** T014 depende de T005. T015 depende de T011 y T014. T016 depende de T015.
- **Cierre:** T017 depende de T011 y T015. T018 depende de US1 a US3. T019 a T021, al final.

## Parallel Example

```text
Después de T004: T005 (Aprobación) y T006 (hash en Generación) a la vez.
Después de T015: T016 (403 de los 10 roles) y T017 (la SPA).
```

## Implementation Strategy

- **MVP:** las fases 1 y 2 y US1 (T008 a T011): el coordinador envía su parcial y queda en la bitácora.
- **Después:** US2 (el paso de etapa) y US3 (el rol y el avance genérico), sobre el mismo agregado.
- **Luego:** la pantalla, el script y el cierre. Si la semana no alcanza, la pantalla de Proyectos (BORRADOR_UNIDADES, desactivada por defecto) es lo primero que puede quedar para otra historia, con una decisión.
