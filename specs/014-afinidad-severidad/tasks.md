---

description: "Tareas de F04-H03: pesos y mínimo de afinidad y la severidad de las reglas"
---

# Tasks: Ajustar pesos y mínimo de afinidad y la severidad de las reglas

**Input**: documentos de diseño en `specs/014-afinidad-severidad/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/normativa.md](contracts/normativa.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**: D-55, D-62, D-65, D-67, D-68, ADR-016 y ADR-021 en [`docs/decisiones.md`](../../docs/decisiones.md); MD-02, MD-08 y el glosario §7.3 («Regla», «Pesos de afinidad», «Mínimo de afinidad», «Severidad») del modelo de dominio.

**Organization**:
- **Fundacional:** el contrato, `Regla` con `RF-xx` y sin parámetros, la migración V4 (D-68) y el repositorio que lee reglas sin parámetros.
- **US1:** ver y ajustar los pesos y el mínimo de afinidad.
- **US2:** cambiar la severidad de una regla.
- **US3:** ajustar en la pantalla «Normativa».

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1, US2 o US3.
- Rutas relativas a la raíz. Abreviaturas: `NOR` = `backend/servicios/normativa/src`, `JNOR` = `NOR/main/java/ec/mdd/normativa`, `PNOR` = `NOR/test/java/ec/mdd/normativa` e `INOR` = `NOR/integrationTest/java/ec/mdd/normativa`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** dominio sin Spring; aplicación sin infraestructura; adaptadores e `infraestructura/` con Spring. `ArquitecturaTest` y `ModulosTest` (proceso `datos`) lo comprueban.
- **DDD táctico (principio II):** `VersionNormativa` sigue inmutable. Las invariantes de `Regla`, de `PesosAfinidad` y de la severidad viven en el dominio (research §2 a §4).
- **Principio VI:** ninguna cifra de la política ni severidad por defecto en el código de producción; solo en `V4` y en las pruebas.
- **Principio VII:** una versión con un periodo no cambia por la API; la migración completa versiones con su registro (D-68).
- **Pruebas:** Mockito solo en la aplicación; adaptadores con Testcontainers y PostgreSQL; contrato con swagger-request-validator; 60 % de líneas por módulo (D-32).
- **Idioma:** identificadores del dominio, mensajes y JavaDoc en español (principio XII).
- **Formato:** `./gradlew spotlessApply` y `npm run formato` antes de cada commit.
- **Evidencia y commits:** la evidencia va en `specs/014-afinidad-severidad/evidencia.md`; los commits llevan `Refs #42`.

---

## Phase 1: Setup

- [X] T001 Crear `specs/014-afinidad-severidad/evidencia.md` con estas secciones: entorno; base (contrato, reglas, migración y repositorio); escenario «Valores por defecto» (US1); escenario «Severidad de una regla» (US2); «Ajustar en pantalla» (US3); cierre; equipo de demostración; notas.

---

## Phase 2: Foundational (bloquea a todas las historias)

### Contrato (principio IV)

- [X] T002 Cambiar `contratos/normativa/openapi.yaml` según contracts/normativa.md:
  - `Regla.identificador` y `ReglaEditada.identificador` con `pattern: '^R[NF]-[0-9]{2}$'`;
  - `Regla.parametros` y `ReglaEditada.parametros` siguen en `required`, sin `minItems`;
  - `ReglaEditada` gana `severidad` (`$ref: '#/components/schemas/Severidad'`), opcional, con la descripción «Ausente: no cambia. Solo en las reglas del motor; en RN-01, RN-06 y RN-24 responde 400»;
  - la descripción de `editarVersion` dice que también cambia la severidad de las reglas del motor y que la carga 7 por archivo la omite;
  - el comentario de cabecera cita F04-H03 y MD-08.

  Regenerar las interfaces (`openApiGenerate`) y los tipos de la SPA (`npm run generar:api`). `npm run verificar:ruptura` sigue en verde.

### Reglas (research §2)

- [X] T003 Prueba primero, en `PNOR/dominio/ReglaTest.java`, sin dobles: una regla `RF-24` con severidad dura y sin parámetros se crea; `RN-17` con severidad y sin parámetros, también; una regla sin parámetros ni severidad lanza `IllegalArgumentException`; `RX-01` y `RN-1` siguen rechazándose; `RN-24` con parámetros y sin severidad se crea. Las pruebas de dominio de F04-H01 y F04-H02 siguen en verde.
- [X] T004 Cambiar `JNOR/dominio/Regla.java`: el identificador sigue `R[NF]-[0-9]{2}`; exige parámetros **o** severidad; agregar `delMotor()` (tiene severidad), con JavaDoc que cite MD-08 y la tabla 5.2 del modelo de dominio. T003 pasa.

### Migración y repositorio (research §2, §5 y §6; D-68)

- [X] T005 Crear `NOR/main/resources/db/migration/normativa/V4__afinidad_y_reglas_del_motor.sql` según data-model.md, con un comentario de cabecera que cite RN-19, RN-24, MD-08, la tabla 5.2 y D-68:
  - el `check` de `regla.identificador` pasa a `'^R[NF]-[0-9]{2}$'`;
  - para cada versión que no las tiene (`where not exists`), las 9 reglas de research §2 con su severidad: RN-12, RN-17, RN-18, RN-22, RN-25 y RF-24 `DURA`; RN-16 y RN-19 `BLANDA`; RN-24 sin severidad;
  - los parámetros de research §1: RN-24 `cuarto_nivel_campo_especifico` 3, `cuarto_nivel_campo_amplio` 2, `tercer_nivel_campo_especifico` 2, `tercer_nivel_campo_amplio` 1, `experiencia_relacionada` 2, `curso_relacionado` 1, `tope_cursos` 3; RN-19 `minimo_afinidad` 2;
  - un `VERSION_EDITADA` de `sistema` por versión completada, con `{"numero": n, "cambios": [...]}` y un cambio por cada severidad agregada (las 8 reglas del motor: `reglas[RN-17].severidad`, anterior `null`, nuevo `DURA`) y por cada parámetro agregado (los 8 de RN-24 y RN-19: `reglas[RN-24].tope_cursos`, anterior `null`, nuevo `3`): 16 cambios, en el formato de `EditarVersionServicio`. RN-24 no tiene severidad, así que solo aporta sus parámetros.
- [X] T006 Prueba primero, en `INOR/` (Testcontainers):
  - `PoliticaDe2024IntegrationTest`: la versión 1 tiene 16 reglas y 37 parámetros (29 + 8); los 29 de antes y las 22 filas, idénticos; las severidades de research §2; un `VERSION_EDITADA` de `sistema` con 16 cambios: las 8 severidades y los 8 parámetros agregados (SC-002);
  - `RepositorioVersionesJdbcIntegrationTest`: `hasSize(7)` pasa a 16; una versión con RF-24 y reglas sin parámetros se guarda y se reconstruye igual; las reglas salen ordenadas por identificador con las `RF-xx` al final;
  - `NormativaEsquemaIntegrationTest`: la versión anterior, que conoce V1 a V3, arranca sobre el esquema con V4 **y lee la versión 1** (D-55, research §6);
  - una versión creada a mano antes de V4 (insertada entre V3 y V4 con `target` de Flyway) queda con las mismas 16 reglas y su registro (D-68).

  Cambiar `JNOR/adaptadores/salida/RepositorioVersionesJdbc.java`: la consulta de reglas usa `LEFT JOIN` con `parametro_regla` y ordena `ORDER BY r.version_id, r.identificador LIKE 'RF-%', r.identificador, p.nombre`; una regla sin parámetros se reconstruye con un mapa vacío; guardar inserta la regla aunque no tenga parámetros (ya lo hace). T006 pasa.

**Checkpoint**: el contrato, `Regla`, V4 y el repositorio están listos; `./gradlew :servicios:normativa:check :servicios:normativa:integrationTest` en verde.

---

## Phase 3: User Story 1 - Ver y ajustar los pesos y el mínimo de afinidad (Priority: P1) 🎯 MVP

**Goal**: cada versión tiene los pesos de RN-24 y el mínimo de RN-19 con sus valores por defecto, y se pueden cambiar mientras no rija un periodo (FR-001, FR-002, FR-004, FR-006, FR-007; SC-002).

**Independent Test**: consultar la versión 1 y comprobar los pesos por defecto y el mínimo; cambiar un peso en una versión sin periodo y comprobar que la 1 no cambia.

- [ ] T007 [US1] Prueba primero, en `PNOR/dominio/`, sin dobles:
  - `PesosAfinidadTest`: se arma con los 7 parámetros de RN-24; para cada nivel, el campo amplio mayor que el específico lanza `CambioNoPermitido` con el campo `reglas[RN-24].cuarto_nivel_campo_amplio` (o `tercer_nivel_…`) y el motivo «El peso del campo amplio (4) no puede superar al del campo específico (3)»; iguales es válido;
  - `VersionNormativaTest`: `pesosAfinidad()` y `minimoAfinidad()` de una versión con RN-24 y RN-19 devuelven sus valores; `editada` con un peso del campo amplio mayor que el específico lanza ese `CambioNoPermitido` y la versión no cambia; cambiar `minimo_afinidad` da un `CambioDeValor` en `reglas[RN-19].minimo_afinidad`; un peso negativo sigue rechazándose como en F04-H02. Ampliar `PNOR/aplicacion/DatosDePrueba.java` con RN-19 y RN-24.
- [ ] T008 [US1] Crear `JNOR/dominio/PesosAfinidad.java` y `JNOR/dominio/MinimoAfinidad.java` (records con JavaDoc que cite RN-19, RN-24, MD-02 y el glosario §7.3), y en `JNOR/dominio/VersionNormativa.java` `pesosAfinidad()` y `minimoAfinidad()` (vacíos si la versión no tiene RN-24 o RN-19) y la comprobación de `PesosAfinidad` dentro de `editada`. T007 pasa.
- [ ] T009 [US1] Prueba primero, en `PNOR/adaptadores/entrada/NormativaContratoTest.java`: `consultarVersion` de una versión con RN-19, RN-24 y RF-24 valida contra el contrato y trae los 8 parámetros; `editarVersion` con `cuarto_nivel_campo_amplio` mayor que el específico responde 400 `COMUN_SOLICITUD_INVALIDA` con ese campo y su motivo en español. Ampliar `NormativaEnMemoria.java` si hace falta. El controlador no cambia: ya traduce `CambioNoPermitido` en un 400 con su campo (F04-H02). T009 pasa.
- [ ] T010 [US1] En `deploy/probar_normativa.sh`:
  - el paso 2 (`es_la_politica_de_2024`) espera 16 reglas en lugar de 7;
  - paso 8 nuevo, «Valores por defecto» (escenario 1): `consultarVersion` de la versión 1 trae en RN-24 cuarto nivel 3 y 2, tercer nivel 2 y 1, experiencia 2, curso 1 y tope 3, y en RN-19 el mínimo 2, con RN-19 blanda.

  Correrlo y anotarlo.

**Checkpoint**: los pesos y el mínimo están en cada versión y se cambian por la API.

---

## Phase 4: User Story 2 - Cambiar la severidad de una regla (Priority: P1)

**Goal**: la severidad de cada regla del motor se cambia en una versión sin periodo, y la consulta del periodo la devuelve (FR-003, FR-005, FR-006, FR-008; SC-003, SC-004).

**Independent Test**: en una versión nueva, cambiar RN-05 de blanda a dura, asociarla a un periodo y comprobar que la consulta del periodo y la de la versión la dan dura, mientras la de origen la conserva blanda.

- [ ] T011 [US2] Prueba primero, en `PNOR/dominio/VersionNormativaTest.java`, sin dobles: `editada` con RN-05 en `DURA` cambia su severidad y da un `CambioDeValor` en `reglas[RN-05].severidad` de `BLANDA` a `DURA`; sin severidad pedida, no cambia; una severidad en RN-24 (o RN-01, RN-06) lanza `CambioNoPermitido` en `reglas[RN-24].severidad` con «Esta regla solo da parámetros: no tiene severidad»; la misma severidad no da cambios; con un periodo, `VersionEnUso`. La versión original no cambia.
- [ ] T012 [US2] Cambiar `JNOR/dominio/CambiosDeVersion.java` (la severidad pedida por regla, opcional: un `Map<String, Severidad>` con solo las que llegan) y `editada` en `JNOR/dominio/VersionNormativa.java`. `PNOR/aplicacion/EditarVersionServicioTest.java` sigue en verde sin cambios (la severidad viaja en `CambiosDeVersion`). T011 pasa.
- [ ] T013 [US2] Prueba primero, en `PNOR/adaptadores/entrada/NormativaContratoTest.java`: `editarVersion` con RN-05 `DURA` responde 200 y la respuesta la tiene dura; con severidad en RN-24, 400 con el campo `reglas[RN-24].severidad`; sin `severidad` en ninguna regla, 200 y ninguna cambia; con una versión en uso, 409 `NORMATIVA_VERSION_EN_USO`. Cambiar `JNOR/adaptadores/entrada/NormativaControlador.java` para pasar la severidad de cada `ReglaEditada` a `CambiosDeVersion`. T013 pasa; `PermisosNormativaTest` sigue en verde.
- [ ] T014 [US2] Prueba en `INOR/VersionesNuevasJdbcIntegrationTest.java`: una versión copiada de la 1 con RN-05 cambiada a dura se guarda y se reconstruye dura, y la 1 la sigue teniendo blanda; el registro tiene el `VERSION_EDITADA` con `reglas[RN-05].severidad`; la copia trae RN-19, RN-24 y RF-24, con sus parámetros y su severidad, iguales a los de la 1 (FR-007).
- [ ] T015 [US2] Agregar a `deploy/probar_normativa.sh` el paso 9, «Severidad de una regla» (escenario 2, SC-004): sobre la versión que creó el paso 5, que sigue sin periodo porque el paso 7 rechaza asociarla, cambiar RN-05 a `DURA`; asociarla a un segundo periodo de prueba `prueba-<aleatorio>-b`, que la limpieza también borra con su `VERSION_ASOCIADA`; `consultarPeriodo` da esa versión y `consultarVersion` la trae con RN-05 dura; la 1 sigue con RN-05 blanda. Con la versión ya asociada, cambiar su severidad da 409. Correrlo dos veces seguidas y anotarlo.

**Checkpoint**: las severidades se cambian, se versionan y se protegen de punta a punta.

---

## Phase 5: User Story 3 - Ajustar en pantalla (Priority: P1)

**Goal**: «Normativa» muestra los pesos, el mínimo y las severidades, y permite editarlos en una versión sin periodo (FR-009; SC-001).

**Independent Test**: con una versión sin periodo, editar en pantalla un peso, el mínimo y una severidad, y comprobar que el cambio queda; con una versión en uso, verlos en solo lectura.

- [ ] T016 [US3] Prueba primero, en `frontend/src/pantallas/normativa/` (Vitest con la API simulada; ampliar `datosDePrueba.ts` con RN-19, RN-24, RF-24 y una regla sin parámetros):
  - la sección «Afinidad» de la vista de solo lectura muestra «Título de cuarto nivel, campo específico: 3», «… campo amplio: 2», tercer nivel, experiencia, curso, «Tope de los cursos: 3» y «Mínimo de afinidad: 2»;
  - la tabla «Reglas y parámetros» muestra los nombres en español de las reglas nuevas («Oferta sin propuesta» para RF-24) y «—» en «Parámetros» si no tiene;
  - en la edición, cada regla con severidad tiene un selector «RN-05 · Severidad» con «Dura» y «Blanda», y las de solo parámetros no; «Guardar» envía la severidad de cada regla del motor; un 400 con `reglas[RN-24].severidad` o `reglas[RN-24].cuarto_nivel_campo_amplio` se muestra junto a su campo;
  - con solo `PARAMETROS:LECTURA`, la sección «Afinidad» se ve y no hay selectores ni «Editar»;
  - las pruebas de F04-H01 y F04-H02 siguen en verde.

  Cambiar `reglas.ts` (nombres de RN-12, RN-16, RN-17, RN-18, RN-19, RN-22, RN-24, RN-25 y RF-24, y de los 8 parámetros), `Normativa.tsx` (la sección «Afinidad», sobre la tabla de reglas) y `EdicionDeVersion.tsx` (el selector, con un `TextField select` de MUI). T016 pasa.
- [ ] T017 [US3] Prueba con Playwright, en `frontend/e2e/normativa.spec.ts`, con la API simulada: `vicerrectorado.demo` cambia la severidad de RN-05 a «Dura» y un peso, guarda y ve los valores nuevos; axe-core sin fallas WCAG 2.1 AA en la vista con «Afinidad» y en la edición con los selectores (solo Chromium).
- [ ] T018 [US3] Revisión del principio VI: buscar en `JNOR/` y en `frontend/src/pantallas/normativa/` los pesos, el mínimo y las severidades por defecto, y anotar que solo aparecen en `V4`, en las pruebas y en los nombres. Anotar en la evidencia el límite de la vuelta atrás de research §6.

**Checkpoint**: las tres historias pasan de punta a punta.

---

## Phase 6: Cierre

- [ ] T019 Correr:
  - en el backend, `./gradlew check integrationTest`;
  - en el frontend, `npm run lint`, `formato:verificar`, `tipos`, `test -- --run --coverage`, `test:e2e` y `verificar:ruptura`;
  - `shellcheck` sobre `probar_normativa.sh`, y Gitleaks sobre un clon en el scratchpad, montado con `:ro,Z` (nunca el repositorio);
  - con el perfil local reconstruido con la rama: `probar_autenticacion.sh`, `probar_errores.sh`, `probar_eventos.sh`, `probar_spa.sh`, `probar_normativa.sh` dos veces seguidas y `deploy/equipo/probar_equipo.sh`.

  Anotar el tiempo de `editarVersion` en el paso 9 (meta < 200 ms con 16 reglas). Todo en verde, con el 60 % de líneas. Hacer commit y push, abrir el PR en borrador con `Refs #42` y marcarlo listo **después** de que terminen sus runs. Comprobar los checks, `perfil-consolidado` y `5 · imágenes`.
- [ ] T020 Completar `evidencia.md` y la «Definición de terminado» del PR. Fusionarlo con *squash* **con la aprobación del autor**, con la rama al día con `main`. Después:
  - esperar el despliegue automático en el PC («Sano en N s»), con `normativa` en V4;
  - en la base del equipo, cada versión (la 1 y la 2 que creó el autor) con 16 reglas y su `VERSION_EDITADA` de `sistema` (D-68);
  - el autor entra con `vicerrectorado.demo`, ve «Afinidad» en la versión 1 y cambia en la versión 2 un peso y la severidad de RN-05 en menos de 2 minutos (SC-001);
  - abrir el PR de evidencia con `Closes #42` y borrar las ramas.

---

## Dependencies & Execution Order

- **Setup (fase 1)**: T001 es libre.
- **Foundational (fase 2)**: T002 antes que todo lo demás; T003 antes que T004; T005 después de T004; T006 después de T005.
- **US1 (fase 3)**: depende de la fase 2. T007 antes que T008; T009 después de T008; T010 después de T009.
- **US2 (fase 4)**: depende de la fase 2. T011 antes que T012; T013 después de T012; T014 después de T012; T015 después de T013 y de T010 (comparten el script).
- **US3 (fase 5)**: T016 depende de T002; T017 de T016; T018 de T008 y T012.
- **Cierre (fase 6)**: T019 y T020 dependen de todo.

### Parallel Opportunities

- US1 y US2 tocan los mismos archivos del dominio (`VersionNormativa`): mejor una después de la otra.
- US3 puede empezar después de T002, porque la pantalla se prueba con la API simulada.
- Como trabajas solo, [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (fase 2 y US1)

1. El contrato, `Regla`, V4 y el repositorio.
2. Los pesos y el mínimo en el dominio y por la API.
3. **Validar:** la versión 1 trae los valores por defecto y el resto sigue idéntico.

### Incremental

1. **US2:** la severidad editable, versionada y protegida.
2. **US3:** la pantalla.
3. **Cierre:** evidencia, PR, fusión y despliegue.

---

## Notes

- El cálculo del puntaje, las sugerencias y el motor que aplica la severidad son de Distributivo (F06 y F07). F07 decide cómo pide las reglas del periodo desde su proceso (research §7).
- Las filas de la tabla de carga horaria siguen duras: su doble severidad la define F07.
- Hacer commit por tarea o por grupo lógico en `014-afinidad-severidad`, con `Refs #42`.
