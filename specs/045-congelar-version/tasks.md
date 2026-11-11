---

description: "Tareas de F13-H01: aprobar el contenido y congelar la versión"
---

# Tasks: Aprobar el contenido y congelar la versión

**Input**: documentos de diseño en `specs/045-congelar-version/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si una prueba no compila, se agrega primero un esqueleto; la implementación va en un comando aparte.

**Decisiones**:
- las tres aclaraciones del autor del 2026-11-09, en la spec;
- D-100, que se escribe en T001;
- D-95, D-97, D-98, D-99 y MD-04.

**Organization**:
- **Fundacional:** D-100, los contratos y los dos errores.
- **US1:** aprobar y congelar (MVP).
- **US2:** la versión congelada no cambia.
- **US3:** la pantalla lo muestra.
- **Cierre:** el script, las regresiones, la evidencia y la fusión.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US3.
- **Rutas:** relativas a la raíz. Abreviaturas, cada una con `main` y `test` en `java/ec/mdd/<servicio>`, e `integrationTest` donde aplica:
  - `GEN` = `backend/servicios/generacion/src`;
  - `VAL` = `backend/servicios/validacionocs/src`;
  - `SPA` = `frontend/src`.

---

## Phase 1: Setup

- [X] T001 Revisar el glosario (`~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/sdd/modelo_dominio.md` §6 y §7): «Versión del distributivo», «Versión congelada», «Huella» o «Hash de contenido» y las etapas. Si «Aprobar el contenido» o «Versión congelada» no están, usar el término del glosario más cercano y anotar aquí el resultado.

  Escribir **D-100** en `docs/decisiones.md`, en la fila del índice y en su sección. Su contenido:
  - aprobar el contenido es del rol de EN_REVISION_VRA;
  - Generación congela por orden de Aprobación, con un paso idempotente, y después Aprobación registra el paso a CONGELADO_EN_FIRMA con el hash del distributivo;
  - todo paso hacia CONGELADO_EN_FIRMA congela, también el envío a revisión con EN_REVISION_VRA desactivada;
  - la versión congelada rechaza toda edición con `GENERACION_VERSION_CONGELADA`;
  - no notifica todavía.

  Relacionadas: RF-38, RF-40, RF-59, MD-04, D-95, D-99.

  **Resultado (2026-11-10):** «Versión congelada» y «Versión del distributivo» están en el glosario (§7), con `VersionDistributivo` en `CONGELADA`. «Hash» está, pero como el del documento firmable; el de esta historia es el «hash de contenido» de `ContenidoVersion` (§6), y se usa ese término. «Aprobar el contenido» no está; se usa el del diseño de firmas §2 («Aprobar contenido»).

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T004.

- [X] T002 Agregar a `contratos/validacionocs/openapi.yaml`, según `contracts/validacionocs.md`:
  - la operación `POST /api/validacionocs/periodos/{periodo}/aprobacion` (`aprobarContenido`, `x-mdd-permiso: DISTRIBUTIVO:APROBACION`, sin cuerpo, con sus respuestas);
  - `APROBAR_CONTENIDO` en la enumeración de acciones;
  - `congelacion` (opcional, con `version`, `hash` de 64 hexadecimales, `quien` y `momento`) en `EstadoDelFlujo`;
  - la nota de congelar en la descripción de `enviarARevision`.

  Compilar `:servicios:validacionocs`, con el controlador respondiendo 501 o sin implementar hasta T008. Regenerar `SPA/api/generado/validacionocs.d.ts`.
- [X] T003 Agregar a `contratos/generacion/openapi.yaml`, según `contracts/generacion.md`:
  - `versionCongelada` (booleano, obligatorio) en `ConsolidadoDelPeriodo` y en `PanelDeInconsistencias`;
  - `GENERACION_VERSION_CONGELADA` en la descripción del 409 de `proponerDocente`, `quitarPropuesta`, `asignarActividad`, `quitarActividad`, `consolidar`, `justificarInconsistencia` y `adjuntarInformeSustentado`.

  Regenerar `SPA/api/generado/generacion.d.ts`; en los controladores, `versionCongelada` en falso hasta T011. Después de T002 y T003, correr `./gradlew :servicios:usuarios:test --rerun` (`BitacoraSinBorradoContratoTest`).
- [X] T004 Agregar a `contratos/comun/errores.md`, con sus 8 columnas:
  - `VALIDACIONOCS_ETAPA_NO_ADMITE_APROBACION` (409, «Etapa sin aprobación», «El distributivo no está en revisión del Vicerrectorado: no se puede aprobar su contenido.»);
  - `GENERACION_VERSION_CONGELADA` (409, «Versión congelada», «La versión del distributivo ya se aprobó y está congelada para la firma: un cambio exige una versión nueva.», con `version` en detalles).

  Crear `VAL/main/.../dominio/EtapaNoAdmiteAprobacion.java` y `GEN/main/.../dominio/VersionCongelada.java` (con el número de la versión) y sus entradas en `ErroresValidacionocs` y `ErroresGeneracion`. Escribir primero sus pruebas, en `ErroresValidacionocsTest` y `ErroresGeneracionTest`.

**Checkpoint**: los contratos compilan y los errores están en el catálogo.

---

## Phase 3: User Story 1 - Aprobar y congelar (Priority: P1) 🎯 MVP

**Goal**: el Vicerrector aprueba en EN_REVISION_VRA; la versión queda CONGELADA y el distributivo pasa a CONGELADO_EN_FIRMA, con una transición y una entrada en la bitácora con la versión y el hash.

**Independent Test**: `probar_aprobacion.sh`, pasos 2 a 4 y 7.

- [X] T005 [P] [US1] En el dominio de Validación OCS, ver fallar primero `VAL/test/.../dominio/ProcesoAprobacionTest.java`:
  - **`validarAprobacion`:**
    - sin el rol de EN_REVISION_VRA en la copia, `RolDeOtraEtapa`;
    - en BORRADOR_CARRERA, EN_CONSOLIDACION o CONGELADO_EN_FIRMA, `EtapaNoAdmiteAprobacion`;
    - no cambia nada.
  - **`aprobarContenido`:** agrega una transición `PASO_DE_ETAPA` de EN_REVISION_VRA a CONGELADO_EN_FIRMA, con quién, la versión, el hash y el momento.
  - **`acciones`:** `[APROBAR_CONTENIDO]` para VICERRECTOR en EN_REVISION_VRA; vacío para RESPONSABLE_CONSOLIDACION y RECTOR ahí; vacío para VICERRECTOR en CONGELADO_EN_FIRMA.
  - **`congelaAlAvanzar`:** verdadero desde EN_REVISION_VRA; desde EN_CONSOLIDACION, verdadero solo si EN_REVISION_VRA está desactivada en la copia.
  - **`congelacion`:** vacía sin el paso; con el paso, la última transición `PASO_DE_ETAPA` hacia CONGELADO_EN_FIRMA.

  Después, en un comando aparte: `AccionDelProceso.APROBAR_CONTENIDO` y los métodos en `ProcesoAprobacion.java`, con su JavaDoc.
- [X] T006 [P] [US1] En Generación, ver fallar primero `GEN/test/.../aplicacion/ParcialesDelDistributivoServicioTest.java` para `congelarVersion`:
  - con una versión EDITABLE, la bloquea (`bloquearVigente`), la marca CONGELADA y devuelve el número y el mismo hash que `hashDelDistributivo`;
  - con una versión ya CONGELADA, devuelve lo mismo sin escribir (idempotente);
  - sin distributivo, `DistributivoNoEncontrado`;
  - no registra en el registro de cambios de Generación (D-95).

  Después: `ParcialesDelDistributivo.congelarVersion` y `puertos/entrada/VersionCongeladaEnGeneracion.java`, el servicio, y `RepositorioDistributivos.congelar(UUID version)` con su implementación en `RepositorioDistributivosJdbc` (`update … set estado = 'CONGELADA'`).
- [X] T007 [US1] En Validación OCS, ver fallar primero en `VAL/test/.../aplicacion/FlujoDelDistributivoServicioTest.java`, con dobles:
  - **`aprobarContenido`:** valida sin bloquear, después `distributivo.congelar`, y al final la transición con el proceso bloqueado y la versión y el hash que devolvió Generación. Una entrada en la bitácora, `CONTENIDO_APROBADO`, con periodo, versión y hash; antes `{estado: EN_REVISION_VRA}`, después `{estado: CONGELADO_EN_FIRMA}`.
  - **Un rechazo** (rol, etapa, periodo cerrado, sin distributivo) no llama a `congelar` ni registra nada.
  - **Reintento:** si la transacción final falla, volver a aprobar llama otra vez a `congelar` y completa.
  - **`enviarARevision` con EN_REVISION_VRA desactivada en la copia:** llama a `congelar` antes de la transacción final, y la transición lleva el hash que devolvió (FR-016). Con EN_REVISION_VRA activa, no llama a `congelar`.
  - **La vista** lleva `congelacion` cuando hay paso a CONGELADO_EN_FIRMA.

  Después, en un comando aparte:
  - **Puertos:** `puertos/entrada/AprobarContenido.java`; `Distributivo.congelar(periodo)` con el registro `VersionCongeladaEnAprobacion(version, hash)`; `CambioDeFlujo.Accion.CONTENIDO_APROBADO`.
  - **Servicio:** el método en `FlujoDelDistributivoServicio`; en `cambio(...)`, un `PASO_DE_ETAPA` desde EN_REVISION_VRA da `CONTENIDO_APROBADO`; `congelacion` en `FlujoDelDistributivo`.
  - **Adaptador:** `DistributivoDesdeGeneracion.congelar`, con `ParcialesDelDistributivo.congelarVersion`.

  **Resultado (2026-11-10):** además del envío a revisión, el envío del último parcial y el avance genérico también congelan si llegan a CONGELADO_EN_FIRMA (FR-016: «ningún distributivo entra en CONGELADO_EN_FIRMA con una versión editable»; posible si la configuración desactiva EN_CONSOLIDACION y EN_REVISION_VRA). Antes de congelar, el dominio valida sin cambiar nada (`validarAvance`, `validarRevision` con las condiciones), para que un intento que no cabe no congele. La prueba del avance genérico se escribió después del código.
- [X] T008 [US1] En `VAL/main/.../adaptadores/entrada/FlujoControlador.java`, la operación `aprobarContenido`, con `RolDeOtraEtapa` traducido a 403 como en enviar y devolver, y `congelacion` en la respuesta. Ver fallar primero:
  - en `FlujoContratoTest`: 200 con `congelacion` que cumple el esquema; 409 `VALIDACIONOCS_ETAPA_NO_ADMITE_APROBACION`; 403 sin el rol de la etapa; `APROBAR_CONTENIDO` en `acciones`;
  - en `PermisosValidacionocsTest`: la operación nueva con `DISTRIBUTIVO:APROBACION` (8 operaciones).
- [X] T009 [US1] Crear `VAL/integrationTest/.../AprobacionIntegrationTest.java` con PostgreSQL real y Generación en el mismo proceso:
  - aprobar deja la versión CONGELADA, el proceso en CONGELADO_EN_FIRMA, una transición con el mismo hash que la del envío a revisión (SC-002) y una entrada en la bitácora;
  - dos aprobaciones simultáneas: una responde y la otra recibe `EtapaNoAdmiteAprobacion`; queda una sola transición desde EN_REVISION_VRA (SC-004).

  **Resultado (2026-11-10):** como `EnvioARevisionIntegrationTest` y `DevolucionIntegrationTest`, Generación es un doble que congela y cuenta; la congelación real, su idempotencia y el bloqueo de la versión se prueban contra PostgreSQL en `GEN/integrationTest/.../CongelacionIntegrationTest.java` (T013).

**Checkpoint**: se aprueba y se congela por la API.

---

## Phase 4: User Story 2 - La versión congelada no cambia (Priority: P1)

**Goal**: con la versión congelada, las siete ediciones responden 409 `GENERACION_VERSION_CONGELADA` sin dejar rastro; consultar y descargar siguen.

**Independent Test**: `probar_aprobacion.sh`, pasos 5 y 6.

- [X] T010 [P] [US2] Ver fallar primero `GEN/test/.../dominio/VersionDistributivoTest.java`:
  - `exigirEditable` con una versión CONGELADA lanza `VersionCongelada` con su número, aunque el parcial esté enviado;
  - con una versión ANULADA, o EDITABLE con el parcial enviado, sigue lanzando `ParcialNoEditable`.

  Después, el cambio en `VersionDistributivo.exigirEditable`. Confirmar que proponer, quitar una propuesta, asignar y quitar una actividad la llaman con la versión bloqueada; agregar o ajustar una prueba de aplicación para cada una que no lo cubra.
- [X] T011 [US2] En `ConsolidarServicioTest`, `JustificarInconsistenciaServicioTest` e `InformeSustentadoServicioTest`, ver fallar primero:
  - con la versión vigente CONGELADA, `VersionCongelada`, sin guardar ni registrar;
  - con la versión congelada y `EnRevision` verdadero, gana `VersionCongelada` (research §4);
  - en el informe, también antes de `almacen.guardar`.

  En las pruebas de `ConsultarConsolidado` y del panel: `versionCongelada` verdadero con la versión CONGELADA, y falso sin distributivo.

  Después, en un comando aparte:
  - la comprobación en los tres servicios, dentro de su transacción y antes de `EnRevision`;
  - `versionCongelada` en `ConsolidadoControlador` y `PanelControlador`.

  Si hace falta, extender `EnRevision`/`EnRevisionServicio` con la consulta de la versión, sin duplicar la lectura.

  **Resultado (2026-11-10):** `EnRevision.versionCongelada(periodo)` (por defecto vacía, para los dobles que solo miran la etapa) y `RepositorioDistributivos.versionCongelada`, que lee la versión vigente con `FOR SHARE`: dentro de la transacción de la operación, una congelación que llega después espera. `versionCongelada` de las consultas sale de ahí, en los dos controladores; su prueba está en `EnRevisionServicioTest` y en las de contrato (T012). Además, quitar una propuesta y quitar una actividad no comprobaban la versión bloqueada (T010): ahora la comprueban dentro de su transacción.
- [X] T012 [US2] En las pruebas de contrato de Generación (propuesta, actividades, consolidado, informe y panel), ver fallar primero: con una versión CONGELADA en el doble del repositorio, 409 `GENERACION_VERSION_CONGELADA` en las siete operaciones, y `versionCongelada: true` en las dos consultas, que cumplen el esquema.
- [X] T013 [US2] En `AprobacionIntegrationTest`, congelar y proponer a la vez sobre PostgreSQL real: la propuesta que toma el bloqueo después de congelar responde `VersionCongelada`, y la versión congelada no tiene la propuesta.

  **Resultado (2026-11-10):** va en `GEN/integrationTest/.../CongelacionIntegrationTest.java`, junto al repositorio real de Generación: congelar real e idempotente; una propuesta que llega con la versión ya bloqueada por la congelación se rechaza; una congelación que llega con la versión bloqueada por una propuesta la espera y la incluye en su hash; y una lectura con `FOR SHARE` hace esperar a la congelación.

**Checkpoint**: la versión congelada no cambia por ninguna vía.

---

## Phase 5: User Story 3 - La pantalla lo muestra (Priority: P2)

**Goal**: el botón y el diálogo para el Vicerrector; la constancia con la huella copiable; sin ediciones y con el aviso.

**Independent Test**: Vitest y Playwright con axe-core, según `contracts/spa.md`.

- [ ] T014 [US3] Escribir primero `SPA/pantallas/comun/EstadoDelFlujo.test.tsx` y `SPA/pantallas/comun/DialogoDeAprobacion.test.tsx`:
  - **El botón:** con `APROBAR_CONTENIDO`, se ve «Aprobar el contenido»; sin la acción, no.
  - **El diálogo:**
    - dice «La versión N queda congelada: es la que se firmará. Todo cambio posterior exigirá una versión nueva.»;
    - «Cancelar» no llama a la API;
    - «Aprobar y congelar» llama a `aprobarContenido` una vez y desactiva el botón mientras espera;
    - un error muestra el mensaje del catálogo;
    - enfoca «Cancelar» al abrirse y se cierra con Escape.
  - **La constancia:** con `congelacion`, se ve «Versión N congelada por <quien> el <fecha>» y la huella abreviada (12 caracteres y «…»); «Copiar la huella» escribe los 64 caracteres en el portapapeles y anuncia «Huella copiada».

  Después: `aprobarContenido` en `SPA/pantallas/comun/flujo.ts`, `DialogoDeAprobacion.tsx` y los cambios en `EstadoDelFlujo.tsx`.
- [ ] T015 [US3] Escribir primero, en `SPA/pantallas/consolidado/Consolidado.test.tsx` y `SPA/pantallas/inconsistencias/Inconsistencias.test.tsx`:
  - con `versionCongelada`, sin ediciones, con el aviso «La versión N está congelada para la firma: ya no cambia.» y sin el de revisión;
  - la descarga del informe sigue;
  - con `enRevision` y sin congelar, sigue el aviso de #215;
  - a mitad de camino (`contracts/spa.md`): con `versionCongelada` y un flujo sin `congelacion` y con `APROBAR_CONTENIDO`, se ve el aviso de versión congelada y la tarjeta sigue ofreciendo «Aprobar el contenido», sin constancia.

  Agregar `versionCongelada: false` a los datos de prueba existentes. Después, el código de las dos pantallas.
- [ ] T016 [US3] En `frontend/e2e/consolidado.spec.ts`, con la API simulada:
  - el Vicerrector en EN_REVISION_VRA abre el diálogo, cancela y aprueba;
  - después, la constancia y el aviso;
  - axe-core sin fallas (WCAG 2.1 AA) con el diálogo abierto y con la constancia.

**Checkpoint**: la pantalla refleja la regla.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T017 Crear `deploy/probar_aprobacion.sh` con los 7 pasos de `contracts/demostracion.md`, a partir de `probar_consolidado_en_revision.sh` (las cargas, la sugerencia, la limpieza y las ayudas). No debe imprimir claves ni tokens, y debe dejar la base limpia al terminar. Con `MDD_CONSERVAR=1`, hace solo el paso 1, no limpia, se niega si `MDD_GATEWAY` no es local e imprime `docker compose -p tesis-prototipo-2026 down -v` para limpiar (`contracts/demostracion.md`), para la comprobación a mano de `quickstart.md` §3. Agregar su paso «Aprobar y congelar» a `.github/workflows/perfil-consolidado.yml`, después de «El consolidado en revisión». Pasar shellcheck.
- [ ] T018 Revisar los scripts que comprueban `acciones` o los errores de propuestas: `probar_revision.sh`, `probar_devolucion.sh`, `probar_consolidado_en_revision.sh`, `probar_flujo.sh` y los de propuesta y actividades. Ajustar solo lo que cambia por esta historia, como el Vicerrector con `APROBAR_CONTENIDO` en EN_REVISION_VRA. Correr todos los scripts del workflow, en su orden, sobre un perfil construido desde la rama, y anotar cuánto tarda el nuevo. Después, con `MDD_CONSERVAR=1`, recorrer `quickstart.md` §3 en el perfil local: la pantalla (US3, SC-006) y el tiempo desde que se abre «Consolidado del periodo» hasta la constancia (SC-003).
- [ ] T019 Correr completos `./gradlew build integrationTest`, `:apps:distributivo:test` (Spring Modulith), la cobertura por módulo (D-32), Spotless y SpotBugs, y en la SPA los tipos, ESLint, Prettier, Vitest y Playwright (SC-005: F10-H03, F12-H05 y #215 sin cambios).
- [ ] T020 Revisar el JavaDoc (principio XII):
  - `congelarVersion`, `VersionCongeladaEnGeneracion` y `VersionCongelada`;
  - `exigirEditable`;
  - `AprobarContenido`, `aprobarContenido` (por qué son dos transacciones y por qué es idempotente), `congelaAlAvanzar` y `congelacion`;
  - `Distributivo.congelar`, `DistributivoDesdeGeneracion` y `EtapaNoAdmiteAprobacion`.
- [ ] T021 Crear `specs/045-congelar-version/evidencia.md`, como `specs/044-consolidado-en-revision/evidencia.md`, con:
  - las pruebas, la cobertura, la arquitectura, la SPA, los scripts, shellcheck y Gitleaks;
  - la tabla del script nuevo;
  - la comprobación a mano de `quickstart.md` §3, con el tiempo medido (SC-003);
  - las desviaciones y los criterios de terminado de la constitución §6.
- [ ] T022 Fusionar solo con la aprobación del autor y con todas las comprobaciones en verde. Verificar el despliegue en el equipo de demostración. Hacer la comprobación del autor con `quickstart.md` §4: en la demo, 2026-I sigue en Consolidación y nada cambia. Abrir el PR de evidencia con `Closes #60`. Borrar las ramas solo si el autor lo pide.

---

## Dependencies & Execution Order

- **Setup:** T001 antes del código.
- **Fundacional:** T002, T003 y T004, en cualquier orden.
- **US1:**
  - T005 y T006 dependen de T004, y se hacen en paralelo;
  - T007 depende de T005 y T006;
  - T008 depende de T002 y T007;
  - T009 depende de T008.
- **US2:**
  - T010 depende de T004;
  - T011 depende de T003 y T004;
  - T012 depende de T010 y T011;
  - T013 depende de T006, T009 y T010.
- **US3:**
  - T014 depende de T002;
  - T015 depende de T003;
  - T016 depende de T014 y T015.

  Pueden empezar con la API simulada.
- **Cierre:** T017 y T018 dependen de US1 y US2; T019 a T022, al final.

## Parallel Example

```text
Después de T004: T005 (dominio de Validación OCS), T006 (Generación) y T010 (versión) a la vez;
T014 y T015 (la SPA) en paralelo con la API simulada.
```

## Implementation Strategy

- **MVP:** las fases 1 y 2 y US1 (T005 a T009): aprobar y congelar por la API.
- **Después:** US2 (la versión no cambia) y US3 (la pantalla).
- **Luego:** el script, las regresiones y el cierre.
