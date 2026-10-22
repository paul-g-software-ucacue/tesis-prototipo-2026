---

description: "Tareas de F03-H01: mantener la estructura orgánica"
---

# Tasks: Mantener la estructura orgánica

**Input**: documentos de diseño en `specs/017-estructura-organica/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/academico.md](contracts/academico.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**: D-07, D-08, D-16, D-65, D-70, ADR-016 y ADR-021 en [`docs/decisiones.md`](../../docs/decisiones.md); RF-12, RN-10, C-12 y V-02 de la ERS; el glosario §7.3 («Sede», «Unidad», «Gestión autorizada», «Comisión»).

**Organization**:
- **Fundacional:** los permisos alternativos (D-70), el contrato, el dominio, la migración V3 y el repositorio.
- **US1:** mantener las unidades con su jerarquía.
- **US2:** solo las 21 gestiones autorizadas.
- **US3:** la comisión informativa.
- **US4:** la pantalla «Estructura orgánica».

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1 a US4.
- Rutas relativas a la raíz. Abreviaturas: `ACA` = `backend/servicios/academico/src`, `JACA` = `ACA/main/java/ec/mdd/academico`, `PACA` = `ACA/test/java/ec/mdd/academico`, `IACA` = `ACA/integrationTest/java/ec/mdd/academico` y `PRU` = `backend/plataforma/pruebas/src`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** dominio sin Spring; aplicación sin infraestructura. `ArquitecturaTest` y `ModulosTest` lo comprueban.
- **Soporte (principio II):** `UnidadOrganica` y `EstructuraOrganica` son valores con sus reglas; sin agregado elaborado ni eventos.
- **Principio VI:** las 21 gestiones solo en `V3` y en las pruebas; nunca en el código de producción.
- **Principio VII:** nada se borra; cada cambio, en `academico.registro_cambio`.
- **Pruebas:** Mockito solo en la aplicación; adaptadores con Testcontainers; contrato con swagger-request-validator; 60 % de líneas por módulo.
- **Idioma, formato y commits:** como en las historias anteriores; la evidencia va en `specs/017-estructura-organica/evidencia.md` y los commits llevan `Refs #35`.

---

## Phase 1: Setup

- [X] T001 Crear `specs/017-estructura-organica/evidencia.md` con estas secciones: entorno; base (permisos alternativos, contrato, dominio, migración y repositorio); «Mantener las unidades» (US1); «Gestión fuera del catálogo» (US2); «Comisión informativa» (US3); pantalla (US4); cierre; equipo de demostración; notas.

---

## Phase 2: Foundational (bloquea a todas las historias)

### Permisos alternativos (D-70, research §4)

- [X] T002 Prueba primero, en `PRU/test/java/ec/mdd/plataforma/pruebas/VerificadorDePermisosTest.java` (ampliar): un contrato de prueba con una operación de `x-mdd-permiso: [A:LECTURA, B:LECTURA]`; `operaciones()` la trae con sus dos permisos; `verificar` falla si sin ninguno no responde 403, o si con uno de los dos responde 403; pasa con un controlador de `hasAnyAuthority`. Las pruebas de un solo permiso siguen en verde. Cambiar `VerificadorDePermisos` (y `OperacionProtegida`, que guarda la lista y la muestra como `[A:LECTURA, B:LECTURA]`). T002 pasa; los `Permisos*Test` de todos los servicios siguen en verde.

### Contrato (principio IV, research §5 y §7)

- [X] T003 Cambiar `contratos/academico/openapi.yaml` según contracts/academico.md: los 6 esquemas y las 6 operaciones, las de lectura con `x-mdd-permiso: [DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]` y las de escritura con `CATALOGOS:ESCRITURA`; `example` en el parámetro `codigo` de unidad; la respuesta `Conflicto` con los 3 códigos nuevos; el comentario de cabecera cita F03-H01, RF-12 y D-70. Agregar los 3 códigos a `contratos/comun/errores.md`. Regenerar (`openApiGenerate`, `generar:api`); `verificar:ruptura` en verde.

### Dominio (research §1 y §3, data-model.md)

- [X] T004 Prueba primero, en `PACA/dominio/`, sin dobles:
  - `UnidadOrganicaTest`: cada regla de su tipo (código con otro formato, nombre vacío o de más de 150, sede matriz con padre, otra sin padre, gestión sin código o con uno que no está en el catálogo, comisión con código de gestión, un órgano de gobierno con y sin código (los dos válidos), comisión sin «permanente», otro tipo con «permanente»), con su campo y su motivo en español en `DatoInvalido`; `corregida` no cambia código ni tipo;
  - `EstructuraOrganicaTest`: registrar con un padre inexistente o inactivo da `DatoInvalido` en `padre`; un ciclo al corregir el padre (directo y de varios niveles) también; una segunda sede matriz lanza `SedeMatrizExistente`; inactivar con dependientes activos lanza `UnidadConDependientes`, y sin ellos, se inactiva; reactivar con el padre inactivo da `DatoInvalido`; las **24 unidades de la carga 01 de prueba** (en `PACA/dominio/EstructuraDePrueba.java`, ficticias, con el catálogo de las 21 gestiones) se registran todas en orden.
- [X] T005 Crear en `JACA/dominio/`: `TipoDeUnidad`, `GestionAutorizada`, `UnidadOrganica`, `EstructuraOrganica`, `SedeMatrizExistente` y `UnidadConDependientes` (reusar `DatoInvalido` y `CambioDeValor`), con JavaDoc en español que cite RF-12, RN-10, C-12 y el glosario §7.3. T004 pasa.

### Persistencia (data-model.md)

- [X] T006 Prueba primero, en `IACA/AcademicoEsquemaIntegrationTest.java` (ampliar): `gestion_autorizada` trae las 21 de POL 6 (G01 Rector … G21 Gestor Unidad de Estadísticas); `unidad_organica` y sus `check` rechazan cada combinación inválida del tipo (sede matriz con padre, gestión sin código, comisión con código de gestión o sin «permanente», un código de gestión que no existe); el índice rechaza una segunda sede matriz; `registro_cambio` acepta las 4 acciones nuevas. Tiene que fallar sin la migración.
- [X] T007 Crear `ACA/main/resources/db/migration/academico/V3__estructura_organica.sql` según data-model.md, con los 21 nombres de POL 6 del diccionario de las cargas y un comentario que cite RF-12, V-02, D-07, RN-10 y D-70 (la interpretación de «sin permiso de administrador»). T006 pasa.
- [X] T008 Prueba primero, en `IACA/RepositorioEstructuraJdbcIntegrationTest.java`: registra y lee una unidad igual, campo por campo; `todas()` y `catalogo()`; `guardar` cambia nombre, padre, gestión, «permanente» y estado; un código repetido no se guarda (`registrar` devuelve falso); `bloquear` toma el candado de la estructura. Crear el puerto `JACA/puertos/salida/RepositorioEstructura.java` y `JACA/adaptadores/salida/RepositorioEstructuraJdbc.java`. T008 pasa.

**Checkpoint**: los permisos alternativos, el contrato, el dominio, V3 y el repositorio están listos.

---

## Phase 3: User Story 1 - Mantener las unidades de la estructura (Priority: P1) 🎯 MVP

**Goal**: registrar, consultar, corregir, inactivar y reactivar unidades, con su jerarquía y su registro (FR-001 a FR-003, FR-007, FR-009, FR-010; SC-002, SC-004).

**Independent Test**: registrar la sede matriz, un órgano y una coordinación; corregir; inactivar con y sin dependientes; cada acción en el registro.

- [X] T009 [US1] Prueba primero, en `PACA/aplicacion/MantenerEstructuraServicioTest.java` (Mockito): registrar bloquea la estructura, valida con `EstructuraOrganica`, guarda y registra `UNIDAD_REGISTRADA`; un código repetido lanza `UnidadRepetida` sin registro; corregir registra `UNIDAD_CORREGIDA` con cada cambio, y sin cambios no registra; cambiar el estado registra `UNIDAD_INACTIVADA` o `UNIDAD_REACTIVADA`, y el mismo estado no; una unidad que no existe lanza `UnidadNoEncontrada`. Y `ConsultarEstructuraServicioTest`: las unidades por código, una por código (404) y el catálogo. Crear los puertos de entrada (`ConsultarEstructura`, `MantenerEstructura`, `UnidadRepetida`, `UnidadNoEncontrada`) y los servicios. T009 pasa.
- [X] T010 [US1] Prueba primero, en `PACA/adaptadores/entrada/EstructuraContratoTest.java` (con un doble en memoria que usa las reglas reales del dominio): `registrarUnidad` 201; 409 `ACADEMICO_UNIDAD_REPETIDA` y `ACADEMICO_SEDE_MATRIZ_EXISTENTE`; 400 con el campo `padre` (inexistente o ciclo); `listarUnidades` y `consultarUnidad` validan contra el contrato; 404; `corregirUnidad` 200; `cambiarEstadoUnidad` 200 y 409 `ACADEMICO_UNIDAD_CON_DEPENDIENTES`. `ErroresAcademicoTest` con los 3 códigos. Crear `JACA/adaptadores/entrada/EstructuraControlador.java` (lecturas con `hasAnyAuthority('PERMISO_DISTRIBUTIVO:LECTURA','PERMISO_CATALOGOS:LECTURA')`, escrituras con `PERMISO_CATALOGOS:ESCRITURA`) y las entradas del catálogo. T010 pasa.
- [X] T011 [US1] Ampliar `PACA/adaptadores/entrada/PermisosAcademicoTest.java` con `EstructuraControlador`: las 12 operaciones exigen su permiso, las de lectura de la estructura con sus dos alternativos; con solo `CATALOGOS:LECTURA`, registrar da 403. Registrar los beans en `ConfiguracionAcademico`. T011 pasa.

**Checkpoint**: la estructura se mantiene por la API.

---

## Phase 4: User Story 2 - Solo las 21 gestiones autorizadas (Priority: P1)

**Goal**: una gestión solo con un código del catálogo; nadie lo amplía (FR-004, FR-005; SC-003).

- [X] T012 [US2] Prueba en `EstructuraContratoTest`: una gestión con `G07` da 201 y muestra el nombre; con `G99` o sin código, 400 con el campo `gestionAutorizada` y su motivo; también con `CATALOGOS:ESCRITURA` (el Administrador): no hay forma de agregar una gestión al catálogo (aclaración 2); `listarGestionesAutorizadas` trae las 21. Como el dominio y el controlador ya lo cubren, la prueba pasa a la primera: comprobar una vez que falla si se quita la regla a mano (la de la gestión), restaurarla y anotarlo en la evidencia.

---

## Phase 5: User Story 3 - Comisión informativa (Priority: P1)

**Goal**: la comisión, con «permanente», sin gestión ni horas (FR-006).

- [X] T013 [US3] Prueba en `EstructuraContratoTest`: una comisión con `comisionPermanente: true` da 201 y la respuesta la trae; sin él, o con un código de gestión, 400 con su campo. Anotar en la evidencia que la estructura no tiene horas: no hay campo de horas en ninguna unidad, y F07 no las toma de las comisiones (C-12). Como el dominio y el controlador ya lo cubren, la prueba pasa a la primera: comprobar una vez que falla si se quita la regla a mano (la de «permanente»), restaurarla y anotarlo en la evidencia.

---

## Phase 6: User Story 4 - La estructura en pantalla (Priority: P1)

**Goal**: «Estructura orgánica» en árbol, con las acciones para quien escribe (FR-011; SC-001).

- [ ] T014 [US4] Prueba primero, en `frontend/src/sesion/` y `frontend/src/navegacion/` (Vitest): `RutaConPermiso` y una entrada del menú aceptan una lista de permisos alternativos (con uno basta; sin ninguno, «Acceso no autorizado» y sin la entrada). Cambiar `RutaConPermiso.tsx` y `Marco.tsx` sin romper las entradas de un solo permiso. T014 pasa.
- [ ] T015 [US4] Prueba primero, en `frontend/src/pantallas/estructura/Estructura.test.tsx` (Vitest con la API simulada): el árbol con sangría bajo cada padre, el tipo en español, el estado y la gestión autorizada si la tiene; las comisiones como «Informativa, sin horas»; con `CATALOGOS:ESCRITURA`, «Registrar unidad» (el padre de una lista de las activas; la gestión de la lista de 21, obligatoria si el tipo es gestión y oculta si es comisión; «permanente» solo si es comisión), «Corregir» sin código ni tipo, e «Inactivar» o «Reactivar» con confirmación; un 400 marca su campo y un 409 muestra su título; sin escritura, sin acciones. Crear la pantalla, su ruta y su entrada en el menú con `[DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`. T015 pasa.
- [ ] T016 [US4] Prueba con Playwright, en `frontend/e2e/estructura.spec.ts`: `admin.demo` registra una sede matriz y una gestión debajo, e inactiva la gestión; axe-core sin fallas WCAG 2.1 AA en el árbol y en los diálogos de registrar, corregir e inactivar.

**Checkpoint**: las cuatro historias pasan.

---

## Phase 7: Punta a punta y cierre

- [ ] T017 Crear `deploy/probar_estructura.sh`, con el estilo de `probar_periodos.sh`: (1) `admin.demo` registra las 24 unidades de la carga 01 de prueba (en el script, ficticias) y la lista las trae en su jerarquía (SC-002); (2) una gestión `G99` y una comisión sin «permanente» dan 400; (3) un padre que forma un ciclo da 400; (4) una segunda sede matriz da 409; (5) inactivar una unidad con dependientes da 409, e inactivar una hoja, 200; (6) `vicerrectorado.demo` la lee y recibe 403 al registrar; `docente.demo` recibe 403 al leer; (7) el registro tiene las acciones. La limpieza borra solo lo que creó (las unidades, de las hojas a la raíz, y sus registros). Agregar su paso a `perfil-consolidado.yml`. Correrlo dos veces seguidas y anotarlo; `shellcheck` sin observaciones.
- [ ] T018 Correr todo, como en las historias anteriores: backend, SPA, los 7 scripts con el perfil reconstruido, `probar_equipo.sh`, `shellcheck` y Gitleaks sobre un clon. Hacer commit y push, abrir el PR en borrador con `Refs #35` y marcarlo listo después de sus runs.
- [ ] T019 Completar la evidencia y el PR. Fusionar con *squash* con la aprobación del autor. Después: esperar el despliegue («Sano en N s»), con `academico` en V3 y las 21 gestiones; el autor entra con `admin.demo`, registra una sede matriz y una gestión debajo en menos de 1 minuto (SC-001) y la ve con `vicerrectorado.demo` en solo lectura; abrir el PR de evidencia con `Closes #35` y borrar las ramas.

---

## Dependencies & Execution Order

- **Setup:** T001 libre.
- **Foundational:** T002 primero (cambia `plataforma:pruebas`); T003 después de T002; T004 antes que T005; T006 antes que T007; T008 después de T005 y T007.
- **US1:** depende de la fase 2. T009 antes que T010; T011 después de T010.
- **US2 y US3:** T012 y T013 después de T010.
- **US4:** T014 libre; T015 después de T003 y T014; T016 después de T015.
- **Cierre:** T017 después de T011; T018 y T019 al final.

### Parallel Opportunities

- La SPA (T014 a T016) puede empezar después de T003, con la API simulada.
- Como trabajas solo, [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (fase 2 y US1)

1. Los permisos alternativos, el contrato, el dominio, V3 y el repositorio.
2. Mantener las unidades por la API. **Validar:** las 24 unidades de prueba entran en su jerarquía.

### Incremental

1. **US2 y US3:** las gestiones y las comisiones.
2. **US4:** la pantalla.
3. **Cierre:** punta a punta, PR, fusión y despliegue.

---

## Notes

- Las carreras van con F03-H02 (aclaración 3, comentado en #36).
- Las horas de gestión y quién ocupa cada gestión son de F03-H05 y F07.
- Hacer commit por tarea o por grupo lógico en `017-estructura-organica`, con `Refs #35`.
