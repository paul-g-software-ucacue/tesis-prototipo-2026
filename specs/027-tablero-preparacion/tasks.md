---

description: "Tareas de F02-H02: ver el tablero de preparación del periodo"
---

# Tasks: Ver el tablero de preparación del periodo

**Input**: documentos de diseño en `specs/027-tablero-preparacion/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/cargas.md](contracts/cargas.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**:
- las 4 aclaraciones y los 2 supuestos del autor del 2026-10-30 (en la spec);
- ADR-022 (se amplía sin cambiar la decisión), D-69, D-70, D-71, D-72, D-74 y MD-13 en [`docs/decisiones.md`](../../docs/decisiones.md);
- RF-56 de la ERS;
- el glosario §7, sin cambios («Tablero de preparación», «Estado de la carga», «Carga obligatoria», «Periodo académico», «Proyecto asignable»).

Plan aprobado por el autor el 2026-10-30.

**Organization**:
- **Fundacional:** el contrato, `tieneDatosEn` y el dominio del tablero.
- **US1, ver qué falta, y US2, saber cuándo se puede empezar:** los dueños, Académico, Cargas y la pantalla.
- **Cierre.**

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 o US2.
- **Rutas:** relativas a la raíz. Abreviaturas:
  - `PLC` = `backend/plataforma/lenguaje-cargas/src`;
  - `CAR` = `backend/servicios/cargas/src`;
  - `ACA` = `backend/servicios/academico/src`;
  - `SPA` = `frontend/src`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** `PreparacionDelPeriodo` es dominio puro, sin Spring. `ArquitecturaTest` lo comprueba, también la regla de la fuente de datos de F01-H05.
- **Límites (principio V):** Cargas usa solo `ec.mdd.plataforma.cargas` y los `puertos.entrada` de los demás. `ModulosTest` lo comprueba.
- **Nada se guarda:** el tablero se calcula en cada consulta.
- **Datos (principio IX):** el tablero solo dice si hay datos; solo datos de prueba ficticios.
- **Mensajes:** en «tú» (D-74), del catálogo (D-72).
- **Pruebas:**
  - Mockito solo en la aplicación;
  - los repositorios con Testcontainers (postgres:18.6-alpine);
  - el contrato con swagger-request-validator;
  - los dobles que ya existen suman los métodos nuevos;
  - 60 % de líneas por módulo.
- **Evidencia y commits:** como en las historias anteriores. La evidencia va en `specs/027-tablero-preparacion/evidencia.md` y los commits llevan `Refs #33`.

---

## Phase 1: Setup

- [ ] T001 Crear `specs/027-tablero-preparacion/evidencia.md` con las secciones de la historia anterior (base, dueños y Académico, tablero, pantalla, cierre, notas) y la línea de base: `main` en `336077f` (#157), con las 12 comprobaciones de #156 en verde.

---

## Phase 2: Foundational (bloquea a todas las historias)

- [ ] T002 Ampliar `contratos/cargas/openapi.yaml` según [contracts/cargas.md](contracts/cargas.md):
  - `consultarPreparacionDelPeriodo` (`GET /api/cargas/preparacion`, `[CARGAS:LECTURA, DISTRIBUTIVO:LECTURA]`), con el parámetro opcional `periodo` (`CodigoDePeriodo`);
  - los esquemas `PreparacionDelPeriodo`, `PeriodoParaElegir`, `CargaDelPeriodo`, `EstadoEnElPeriodo` y `LoQueFalta`;
  - los 400 `COMUN_SOLICITUD_INVALIDA` y `CARGAS_PERIODO_NO_ENCONTRADO`.

  Sumar el tablero a la causa de `CARGAS_PERIODO_NO_ENCONTRADO` en `contratos/comun/errores.md`. Regenerar los modelos.
- [ ] T003 [P] Ampliar el lenguaje publicado (`PLC/main/java/ec/mdd/plataforma/cargas/`): `default boolean tieneDatosEn(String periodo)` en `TraductorDeCarga`, que devuelve `tieneDatos()`, y `tieneDatosEn(String carga, String periodo)` en `AplicadorDeCargas` y `AplicadorPorTraductores`. Primero las pruebas, en `AplicadorPorTraductoresTest`:
  - por defecto, lo mismo que `tieneDatos()`;
  - un traductor que lo reemplaza responde por periodo;
  - una carga que no atiende es `IllegalArgumentException`.
- [ ] T004 [P] Crear el dominio del tablero en `CAR/main/java/ec/mdd/cargas/dominio/`: `PreparacionDelPeriodo`, `CargaDelPeriodo`, `EstadoEnElPeriodo` (`PENDIENTE`, `CARGADA`, `CON_ERRORES`, `SIN_PROYECTOS`), `LoQueFalta` y su tipo (`CARGA`, `PERIODO_SIN_ABRIR`, `PERIODO_CERRADO`, `SIN_PERIODOS`), según [data-model.md](data-model.md). Primero las pruebas, en `CAR/test/java/ec/mdd/cargas/dominio/PreparacionDelPeriodoTest.java`, sin dobles y con el diccionario real:
  - con todo y el periodo abierto, listo y sin nada que falte;
  - con docentes pendiente, falta docentes y no está listo;
  - sin matriculados, sigue listo;
  - una carga con un rechazo y sin datos, «con errores» con su motivo, y cuenta como lo que falta;
  - con datos y un rechazo, «cargada»;
  - proyectos «sin proyectos» no falta;
  - el periodo sin abrir y el cerrado faltan;
  - lo que falta va en el orden de la ERS, y después el periodo;
  - sin periodos, `SIN_PERIODOS`.

**Checkpoint**: el contrato, el lenguaje publicado y el dominio están listos.

---

## Phase 3: US1 y US2 · Los dueños y Académico (Priority: P1) 🎯 MVP

**Goal**: la oferta y los matriculados dicen si tienen datos de un periodo; Académico dice si un periodo está abierto o cerrado.

**Independent Test**: con Testcontainers, la oferta y los matriculados de 2026-I cuentan para 2026-I y no para 2026-II; `ConsultarReferencias` da 2026-I abierto, si tiene versión, y uno cerrado, como cerrado.

- [ ] T005 [P] [US1] Académico: `tieneDatosEn(periodo)` en `TraductorOferta` y `TraductorMatriculas` (`ACA/main/java/ec/mdd/academico/aplicacion/cargas/`), con `hayOfertadasEn(periodo)` en `RepositorioOferta` y `hayMatriculasEn(periodo)` en `RepositorioMatriculas`, y su `select exists … where periodo = :periodo` en los adaptadores JDBC y en los dobles en memoria. Primero las pruebas, en `ACA/integrationTest/java/ec/mdd/academico/AplicadorAcademicoIntegrationTest.java`: con las cargas 03, 05 y 10 de prueba, la oferta y los matriculados de 2026-I (los únicos del archivo) cuentan para 2026-I y no para 2026-II; una oferta retirada no cuenta; las otras cargas responden igual que `tieneDatos()`.
- [ ] T006 [P] [US2] Académico: `boolean abierto, boolean cerrado` en `ACA/main/java/ec/mdd/academico/puertos/entrada/PeriodoDeReferencia.java`, y `NormativaDelPeriodo` en `ConsultarReferenciasServicio` (y en su configuración) para `abierto`. Ajustar los lugares que construyen `PeriodoDeReferencia`: Académico, Docentes (`AcademicoDesdePuertos`) y las pruebas de los tres servicios. Primero las pruebas, en `ConsultarReferenciasServicioTest`, con Mockito: un periodo con versión está abierto, sin versión no, y uno cerrado está cerrado.

**Checkpoint**: Cargas ya puede preguntar todo lo que el tablero necesita.

---

## Phase 4: US1 y US2 · El tablero en Cargas (Priority: P1)

**Goal**: `consultarPreparacionDelPeriodo` responde el tablero de un periodo y la lista de periodos.

**Independent Test**:
- en el proceso `datos` real, con docentes pendiente, el tablero de 2026-I no está listo y nombra docentes;
- con todo y 2026-I abierto, está listo.

- [ ] T007 [US1] Ampliar `ReferenciasDelSistema` (`CAR/main/java/ec/mdd/cargas/puertos/salida/`) con `periodos()`: código, nombre, inicio, abierto y cerrado de cada periodo, del que empieza más tarde al más antiguo, con un record propio de Cargas. Implementarlo en `ReferenciasDesdePuertos` desde `PeriodoDeReferencia`. Primero la prueba, en `ReferenciasDesdePuertosTest`.
- [ ] T008 [US1] [US2] Crear `ConsultarPreparacion` (puerto de entrada), `ConsultarPreparacionServicio` (aplicación) y `PreparacionControlador` (`adaptadores/entrada`, operación `consultarPreparacionDelPeriodo`) en `CAR/main/java/ec/mdd/cargas/`. El servicio arma `PreparacionDelPeriodo` con:
  - `DuenosDeLasCargas` (`tieneDatosEn`);
  - `ProyectosDelPeriodo.hayAsignables`;
  - el historial (el último intento de cada carga y «sin proyectos» del periodo);
  - los periodos de `ReferenciasDelSistema`.

  Sin `periodo`, usa el que empieza más tarde; uno que no existe es `PeriodoNoEncontrado` (`CARGAS_PERIODO_NO_ENCONTRADO`). Primero las pruebas:
  - `ConsultarPreparacionServicioTest`, con Mockito: el periodo por defecto, uno que no existe, sin periodos, «con errores» desde el historial, y los proyectos asignables o «sin proyectos»;
  - `PreparacionContratoTest`: el 200 con la lista de periodos; los 400 con periodos válidos (2026-I, 2026-II) e inválidos (`2026-III`, `26-I`, `2026-i`, de 11 caracteres), y uno que no existe;
  - `PermisosCargasTest` con la operación nueva.
- [ ] T009 [US1] [US2] Ampliar `backend/apps/datos/src/integrationTest/java/…/ConfirmarLasDiezCargasIntegrationTest.java`:
  - con las cargas 01 a 05 y 07, sin la 06, y 2026-I «sin proyectos», el tablero de 2026-I no está listo, nombra docentes y cargos, y la 9 dice «Sin proyectos»;
  - con las 10 y 2026-I abierto, está listo;
  - 2026-II, sin oferta ni matriculados propios, tiene la oferta pendiente y le falta abrirse;
  - el tablero tarda menos de 3 s (SC-003), con la medición anotada en la evidencia.

  `ModulosTest` sigue en verde.

**Checkpoint**: los dos escenarios del issue funcionan por la API.

---

## Phase 5: US1 y US2 · La pantalla (Priority: P1)

**Goal**: «Preparación del periodo» muestra el tablero, lo que falta y «Crear distributivo».

**Independent Test**: con Vitest y la API simulada, el escenario 1 (docentes pendiente: el botón deshabilitado con lo que falta como descripción) y el 2 (todo listo: habilitado, y al pulsarlo el aviso de F06).

- [ ] T010 [US1] Crear `SPA/pantallas/preparacion/consultas.ts` (`usePreparacion(periodo)`, con sus tipos del contrato y el patrón del periodo) y `SPA/pantallas/preparacion/datosDePrueba.ts`. Sumar la ruta `/preparacion` en `SPA/rutas.tsx` y la entrada «Preparación del periodo» en `SPA/navegacion/Marco.tsx`, con los permisos de `DESCARGA_DE_PLANTILLAS` (`[DISTRIBUTIVO:LECTURA, CARGAS:LECTURA]`) en `SPA/sesion/funcionalidades.ts`.
- [ ] T011 [US1] [US2] Crear `SPA/pantallas/preparacion/Preparacion.tsx`:
  - el `Select` de los periodos (el sugerido, al abrir);
  - la tabla de las 10 cargas (número, carga, obligatoria, estado como `Chip` y el motivo si está «con errores»);
  - la lista de lo que falta, o un aviso de que el periodo está listo;
  - el botón «Crear distributivo», solo con `DISTRIBUTIVO:ESCRITURA`, habilitado si está listo, con `aria-describedby` a lo que falta, y que al pulsarlo muestra el aviso de F06 con `role="status"`;
  - los errores con `AvisoDeError`;
  - sin periodos, el aviso de cargar los ciclos académicos.

  Primero las pruebas, en `SPA/pantallas/preparacion/Preparacion.test.tsx`:
  - los dos escenarios del issue;
  - cada estado, con el motivo de «con errores»;
  - cambiar de periodo;
  - el periodo sin abrir y el cerrado;
  - sin periodos;
  - sin «Distributivo: escritura», sin botón;
  - sin permiso, fuera del menú;
  - el error del servidor.
- [ ] T012 [US1] [US2] Crear `frontend/e2e/preparacion.spec.ts`, con rutas simuladas: el escenario 1 (el botón deshabilitado con su descripción) y el 2 (el aviso), con axe sin fallas WCAG 2.1 AA. Correr `lint`, `tipos`, `formato`, Vitest y Playwright.

**Checkpoint**: la pantalla cubre los dos escenarios del issue.

---

## Phase 6: Punta a punta y cierre

- [ ] T013 Crear `deploy/probar_preparacion.sh`, siguiendo `deploy/probar_orden_cargas.sh`:
  0. comprobar que la base no tiene los datos de prueba;
  1. las cargas 01 a 05 y 07, y 2026-I «sin proyectos»: el tablero de 2026-I no está listo y nombra docentes y cargos;
  2. la 06, la 08, la 09 y la 10: 2026-I todavía no está listo porque le falta abrirse;
  3. abrir 2026-I con la versión de la política de prueba: listo;
  4. 2026-II: la oferta pendiente;
  5. una carga rechazada sin datos: «con errores» (en una base que no tenga esa carga);
  6. `2027-I` 400 `CARGAS_PERIODO_NO_ENCONTRADO` y `2026-III` 400 `COMUN_SOLICITUD_INVALIDA`;
  7. `vicerrectorado.demo` y `coordinacion.demo` ven el tablero, y `docente.demo` recibe 403;
  8. limpiar por códigos y por `momento >= inicio`, también la asociación de 2026-I con la versión.

  Agregarlo a `.github/workflows/perfil-consolidado.yml`. Pasar shellcheck v0.11.0.
- [ ] T014 Verificación completa:
  - `./gradlew build integrationTest`;
  - los 17 scripts, con `probar_preparacion.sh` dos veces seguidas en un perfil reconstruido (con el builder creado sin `-q`);
  - `probar_equipo.sh`;
  - shellcheck;
  - Gitleaks sobre un clon;
  - ningún valor de prueba en los logs;
  - la cobertura por módulo;
  - los criterios de terminado de la constitución (§6).

  Completar la evidencia. Commit, push, PR en borrador con `Refs #33` y marcarlo listo cuando sus comprobaciones pasen.
- [ ] T015 Fusionar solo con la aprobación del autor. Verificar el despliegue en el equipo de demostración con evidencia (GitHub `demostracion` y los 8 contenedores sanos con la versión nueva). Después: la comprobación del autor, el PR de evidencia con `Closes #33`, borrar las ramas y cerrar F02 (#4) solo si sus historias de la Ola 1 están cerradas (F02-H03 es de la Ola 4).

---

## Dependencies & Execution Order

- **Fundacional (T002 a T004):** antes de todo; T003 y T004 son [P] después de T002.
- **Fase 3 (T005, T006):** depende de T003 y T002; sus dos tareas son [P].
- **Fase 4:**
  - T007 depende de T006;
  - T008 depende de T004, T005 y T007;
  - T009 depende de T008.
- **Fase 5:** depende de T008, por el contrato real; T010 primero.
- **Fase 6:** al final.

## Parallel Example

```text
Después de T002: T003 y T004 a la vez.
Después de T003: T005 (oferta y matriculados) y T006 (periodos abiertos) a la vez.
```

## Implementation Strategy

- **MVP:** las fases 2 a 4 dan los dos escenarios del issue por la API.
- **Después:** la pantalla (fase 5) y el cierre (fase 6). Cada fase deja el sistema en verde.
