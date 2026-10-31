---

description: "Tareas de F06-H01: calcular el puntaje de afinidad con su desglose"
---

# Tasks: Calcular el puntaje de afinidad con su desglose

**Input**: documentos de diseño en `specs/028-puntaje-afinidad/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/generacion.md](contracts/generacion.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**:
- las 4 aclaraciones del autor del 2026-10-30 (en la spec);
- ADR-023, aprobada por el autor el 2026-10-30 y por escribir en T001;
- ADR-006, ADR-009, ADR-010, ADR-018, D-49, D-68, D-70, D-72 y D-74 en [`docs/decisiones.md`](../../docs/decisiones.md);
- RN-19, RN-24 y RF-17 de la ERS;
- el glosario §7: sin términos nuevos; «Puntaje de afinidad» ampliado con aprobación del autor.

Plan aprobado por el autor el 2026-10-30.

**Organization**:
- **Fundacional:** ADR-023, los contratos, los errores comunes y el cliente entre procesos.
- **US1, calcular:** la calculadora, las tres consultas internas y la operación de Generación.
- **US2, ver:** la sección de la ficha.
- **Cierre.**

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 o US2.
- **Rutas:** relativas a la raíz. Abreviaturas:
  - `EVE` = `backend/plataforma/eventos/src`;
  - `NUC` = `backend/plataforma/errores-nucleo/src`;
  - `GEN` = `backend/servicios/generacion/src`;
  - `ACA` = `backend/servicios/academico/src`;
  - `NOR` = `backend/servicios/normativa/src`;
  - `DOC` = `backend/servicios/docentes/src`;
  - `SPA` = `frontend/src`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** `CalculadoraAfinidad` es dominio puro, sin Spring ni HTTP. `ArquitecturaTest` lo comprueba.
- **Límites (principio V):** Generación no importa tipos de Académico, Normativa ni Docentes; recibe sus propios valores. `ModulosTest` lo comprueba.
- **Rutas internas (ADR-023):** solo el rol `PROCESO`; un token de usuario recibe 403 y el API Gateway no las enruta.
- **Reglas (principio VI):** ningún peso en el código de producción. Las pruebas de la calculadora usan pesos que no son los de por defecto.
- **Nada se guarda:** el puntaje se calcula en cada consulta (FR-017). Sin migraciones.
- **Datos (principio IX):** la ruta de formación no devuelve contacto ni contrato, y ninguna ficha va a los logs. Solo datos de prueba ficticios.
- **Mensajes:** en «tú» (D-74), del catálogo con sus 8 columnas (D-72).
- **Validación (principio XIV):** `^[0-9A-Z]{1,13}$` para la identificación y `^[A-Za-z0-9._-]{1,20}$` para el periodo, en el contrato, el servidor y la pantalla.
- **Pruebas:**
  - Mockito solo en la aplicación;
  - fuera de ahí, dobles escritos a mano;
  - el cliente entre procesos, contra `ReceptorSimulado`.

---

## Phase 1: Setup

- [ ] T001 Escribir ADR-023 («Consultas entre procesos por rutas internas con token de proceso») en `docs/decisiones.md`, con su fila en el índice:
  - extiende ADR-018 de eventos a consultas;
  - precisa ADR-010: la ficha se pide por una ruta interna de la API de Docentes;
  - fija `COMUN_PROCESO_CON_ERROR` (502) y `COMUN_PROCESO_SIN_RESPUESTA` (504);
  - registra lo descartado (research §1).

---

## Phase 2: Foundational

**Purpose**: los contratos y el mecanismo entre procesos que usa toda la historia.

- [ ] T002 Contratos (principio IV), según [contracts/generacion.md](contracts/generacion.md):
  - `contratos/generacion/openapi.yaml`: `consultarAfinidadDelDocente` y sus esquemas;
  - `contratos/academico/openapi.yaml`, `contratos/normativa/openapi.yaml` y `contratos/docentes/openapi.yaml`: las tres rutas internas, con la etiqueta `Interno`, sin `x-mdd-permiso`;
  - `contratos/comun/errores.yaml`: las respuestas 502 y 504;
  - `contratos/comun/errores.md`: los 5 códigos, con sus 8 columnas.
- [ ] T003 [P] Errores comunes: `COMUN_PROCESO_CON_ERROR` y `COMUN_PROCESO_SIN_RESPUESTA` en `NUC/main/java/ec/mdd/plataforma/errores/nucleo/CodigosComunes.java`, y su traducción en `:plataforma:errores`, con su prueba.
- [ ] T004 [P] `EVE/test/java/ec/mdd/plataforma/eventos/ConsultasEntreProcesosTest.java`, contra `ReceptorSimulado`:
  - 200 con el cuerpo;
  - 404 como resultado vacío;
  - 401: otro token y un reintento;
  - 500 y sin conexión: `ProcesoConError`;
  - plazo agotado: `ProcesoSinRespuesta`;
  - Usuarios no emite el token: `ProcesoConError`.
- [ ] T005 Implementar `ConsultasEntreProcesos`, `ProcesoConError` y `ProcesoSinRespuesta` en `EVE/main/java/ec/mdd/plataforma/eventos/`, con `Clientes.http(plazo)` y `ProveedorDeTokenDeProceso`, y su bean en `EventosAutoConfiguracion.java`. Hace pasar T004.

**Checkpoint**: Generación puede consultar otro proceso con plazo y traducir sus fallos.

---

## Phase 3: User Story 1 - Calcular el puntaje de un docente para una asignatura (Priority: P1)

**Goal**: el puntaje y su desglose, por la API, con los pesos de la versión del periodo.

**Independent Test**: `CalculadoraAfinidadTest` y `AfinidadContratoTest`, y la operación contra el proceso `datos` real.

### El dominio

- [ ] T006 [US1] `GEN/test/java/ec/mdd/generacion/dominio/CalculadoraAfinidadTest.java`, sin dobles y con pesos que no son los de por defecto (spec US1, escenarios 1 a 7):
  - cuarto nivel con el campo específico igual, y el desglose que lo nombra;
  - tercer y cuarto nivel con solo el campo amplio igual: suman menos;
  - dos títulos que coinciden: suman los dos;
  - experiencia y curso con el campo específico igual;
  - experiencia y curso con solo el campo amplio igual: 0;
  - más cursos que el tope: la suma no pasa de `topeCursos`, `topeAplicado` es verdadero y el último curso que entra lleva lo que cabe;
  - un curso que no cabe en el tope no aparece en el desglose, y ningún aporte es 0;
  - nada coincide: total 0 y desglose vacío;
  - ficha sin tercer nivel.
- [ ] T007 [US1] Implementar en `GEN/main/java/ec/mdd/generacion/dominio/`: `CalculadoraAfinidad`, `PuntajeAfinidad`, `AporteDeAfinidad`, `TipoDeAporte`, `CoincidenciaDeCampo`, `PesosAfinidad`, `FormacionDelDocente` y `AsignaturaOfertada`, según [data-model.md](data-model.md). El campo amplio de cuarto nivel son los dos primeros dígitos de su campo específico. Hace pasar T006.

### Las consultas internas (en `datos`)

- [ ] T008 [P] [US1] Académico: `InternoAcademicoContratoTest` en `ACA/test/java/ec/mdd/academico/adaptadores/entrada/` y la prueba del servicio:
  - cada asignatura con un paralelo `OFERTADA`, una sola vez, con sus dos campos;
  - un periodo sin oferta: lista vacía;
  - un periodo que no existe: 404;
  - un token de usuario: 403.
- [ ] T009 [P] [US1] Implementar en Académico el puerto `ConsultarAsignaturasOfertadas` en `ACA/main/java/ec/mdd/academico/puertos/entrada/`, su servicio de aplicación, y `InternoAcademicoControlador` en `adaptadores/entrada`. Hace pasar T008.
- [ ] T010 [P] [US1] Normativa: `InternoNormativaContratoTest` en `NOR/test/java/ec/mdd/normativa/adaptadores/entrada/` y la prueba del servicio:
  - un periodo con versión: su número y sus 7 pesos;
  - un periodo sin versión: 404;
  - un token de usuario: 403.
- [ ] T011 [P] [US1] Implementar en Normativa el puerto `ConsultarPesosDelPeriodo` en `NOR/main/java/ec/mdd/normativa/puertos/entrada/`, su servicio de aplicación, e `InternoNormativaControlador` en `adaptadores/entrada`. Hace pasar T010.
- [ ] T012 [P] [US1] Docentes: `InternoDocentesContratoTest` en `DOC/test/java/ec/mdd/docentes/adaptadores/entrada/` y la prueba del servicio:
  - la formación con sus campos;
  - una respuesta que no incluye correo, teléfono ni contrato;
  - un docente que no existe: 404;
  - un token de usuario: 403.
- [ ] T013 [P] [US1] Implementar en Docentes el puerto `ConsultarFormacion` en `DOC/main/java/ec/mdd/docentes/puertos/entrada/`, su servicio de aplicación, e `InternoDocentesControlador` en `adaptadores/entrada`. Hace pasar T012.

### La operación de Generación

- [ ] T014 [US1] `GEN/test/java/ec/mdd/generacion/aplicacion/ConsultarAfinidadServicioTest.java`, con Mockito:
  - ordena del mayor puntaje al menor y, a igual puntaje, por nombre (FR-008);
  - consulta en el orden de research §2 y corta en el primer fallo;
  - periodo inexistente, sin versión o docente inexistente: su excepción, sin puntajes;
  - sin oferta: lista vacía, con la versión.
- [ ] T015 [US1] Implementar:
  - el puerto `ConsultarAfinidad` en `GEN/main/java/ec/mdd/generacion/puertos/entrada/`;
  - el puerto `DatosParaAfinidad` en `GEN/main/java/ec/mdd/generacion/puertos/salida/`;
  - `ConsultarAfinidadServicio` en `GEN/main/java/ec/mdd/generacion/aplicacion/`.

  Hace pasar T014.
- [ ] T016 [US1] `GEN/test/java/ec/mdd/generacion/adaptadores/salida/DatosDesdeProcesoDatosTest.java`, contra `ReceptorSimulado`: traduce las 3 respuestas a los valores del dominio, y cada 404 al resultado que le corresponde.
- [ ] T017 [US1] Implementar `DatosDesdeProcesoDatos` en `GEN/main/java/ec/mdd/generacion/adaptadores/salida/` y la propiedad `mdd.procesos.datos` en `backend/apps/distributivo/src/main/resources/application.yaml`. Hace pasar T016.
- [ ] T018 [US1] `GEN/test/java/ec/mdd/generacion/adaptadores/entrada/AfinidadContratoTest.java` y `VerificadorDePermisos`:
  - 200 válido contra el contrato;
  - 400 con el periodo o la identificación mal escritos, con caso válido e inválido;
  - 404 y 409 con sus códigos;
  - 502 y 504;
  - 403 sin permiso, y 200 con cada uno de los tres permisos.
- [ ] T019 [US1] Implementar `AfinidadControlador` y `ErroresGeneracion` en `GEN/main/java/ec/mdd/generacion/adaptadores/entrada/`, con `spring-boot` y las dependencias que pida el catálogo en `backend/servicios/generacion/build.gradle`. Hace pasar T018 y `VerificadorDeCatalogo`.
- [ ] T020 [US1] Comprobar `ArquitecturaTest` y `ModulosTest` de los 4 servicios y de la app `distributivo`. Ajustar sin relajar ninguna regla.

**Checkpoint**: los tres primeros escenarios del issue pasan por la API.

---

## Phase 4: User Story 2 - Ver el puntaje en la ficha del docente (Priority: P1)

**Goal**: el cuarto escenario del issue en la pantalla «Docentes».

**Independent Test**: abrir un docente, elegir un periodo y desplegar una asignatura.

- [ ] T021 [US2] Generar los tipos de la SPA, registrar el cliente de Generación si falta, y agregar `consultarAfinidad` en `SPA/pantallas/docentes/consultas.ts`.
- [ ] T022 [US2] Pruebas en `SPA/pantallas/docentes/Docentes.test.tsx`, con datos en `datosDePrueba.ts`:
  - la lista del mayor al menor;
  - el desglose desplegado, con el tipo, el nombre y el aporte;
  - el aviso del tope aplicado;
  - un periodo sin abrir: el mensaje del catálogo, sin puntajes;
  - un periodo sin oferta: «Este periodo todavía no tiene oferta»;
  - un puntaje 0 se muestra;
  - el 502 y el 504: `AvisoDeError`, sin detalles técnicos.
- [ ] T023 [US2] Implementar la sección «Afinidad por asignatura» en `SPA/pantallas/docentes/AfinidadDelDocente.tsx` y mostrarla en la ficha de `DialogosDeDocente.tsx`, con el selector de periodo de `ReferenciasDeDocentes`. Hace pasar T022.
- [ ] T024 [US2] `frontend/e2e/docentes.spec.ts`: el recorrido con `simularApi` y axe sobre la sección, con el diálogo ya en opacidad 1.

**Checkpoint**: los cuatro escenarios del issue pasan.

---

## Phase 5: Polish & Cross-Cutting Concerns

- [ ] T025 `deploy/probar_afinidad.sh`, según [quickstart.md](quickstart.md), con los dos procesos reales, y su paso en `.github/workflows/perfil-consolidado.yml`:
  - un docente y una versión con pesos distintos en otro periodo (SC-002);
  - los errores y permisos de quickstart §4;
  - una ruta `/interno/…` con token de usuario, y por el API Gateway;
  - la lista en menos de 3 s (SC-003);
  - con `datos` detenido, el mensaje en menos de 10 s (SC-004), y `datos` de vuelta y sano al terminar;
  - una ficha que no aparece en los logs de ningún contenedor (SC-005);
  - la base limpia al terminar.
- [ ] T026 Crear `specs/028-puntaje-afinidad/evidencia.md`, y verificar:
  - todas las pruebas del backend y la SPA, con su cobertura por módulo;
  - la meta de cobertura del núcleo en el dominio de Generación;
  - Spotless, SpotBugs, ESLint y TypeScript;
  - los scripts de `deploy/`, shellcheck y Gitleaks;
  - `deploy/equipo/probar_equipo.sh`;
  - los criterios de terminado de la constitución (§6).

  Completar la evidencia. Commit, push, PR en borrador con `Refs #46` y marcarlo listo cuando sus comprobaciones pasen.
- [ ] T027 Fusionar solo con la aprobación del autor. Verificar el despliegue en el equipo de demostración con evidencia (GitHub `demostracion` y los 8 contenedores sanos con la versión nueva). Después: la comprobación del autor y el PR de evidencia con `Closes #46`.

---

## Dependencies & Execution Order

- **T001:** antes de todo.
- **Fundacional:**
  - T002 antes de todo el código;
  - T003 y T004 son [P] después de T002;
  - T005 depende de T003 y T004.
- **Fase 3:**
  - T006 y T007, después de T002;
  - T008 a T013 son [P] después de T002, cada implementación después de su prueba;
  - T014 y T015 dependen de T007;
  - T016 y T017 dependen de T005 y T015;
  - T018 y T019 dependen de T015;
  - T020, al final de la fase.
- **Fase 4:** depende de T019, por el contrato real.
- **Fase 5:** al final.

## Parallel Example

```text
Después de T002: T003 y T004 a la vez.
Después de T002: T008-T009 (Académico), T010-T011 (Normativa) y T012-T013 (Docentes) a la vez.
```

## Implementation Strategy

- **MVP:** las fases 1 a 3 dan los tres primeros escenarios por la API.
- **Después:** la pantalla (fase 4) y el cierre (fase 5). Cada fase deja el sistema en verde.
