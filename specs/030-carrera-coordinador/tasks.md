---

description: "Tareas de F06-H05: indicar la carrera del coordinador"
---

# Tasks: Indicar la carrera del coordinador

**Input**: documentos de diseño en `specs/030-carrera-coordinador/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/roles.md](contracts/roles.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**:
- las 3 aclaraciones del autor del 2026-10-31 (en la spec);
- D-76, aprobada por el autor el 2026-10-31 y por escribir en T001;
- D-16, D-62, D-64, D-65, D-70, D-72, D-74, ADR-018 y ADR-023 en [`docs/decisiones.md`](../../docs/decisiones.md);
- RF-02 y RF-03 de la ERS;
- el glosario §7: «Carrera del coordinador» agregado con aprobación del autor, ya aplicado.

Plan aprobado por el autor el 2026-10-31.

**Organization**:
- **Fundacional:** D-76, los contratos, los errores, `AutoridadesDelToken` y `AlcanceDeCarrera`.
- **US1, indicar la carrera:** Roles, el token y la pantalla.
- **US2, trabajar solo con su carrera; US3, sin carrera; US4, alcance total:** los filtros de Académico y Generación.
- **Cierre.**

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1, US2, US3 o US4.
- **Rutas:** relativas a la raíz. Abreviaturas:
  - `SEG` = `backend/plataforma/seguridad/src`;
  - `ROL` = `backend/servicios/roles/src`;
  - `USU` = `backend/servicios/usuarios/src`;
  - `ACA` = `backend/servicios/academico/src`;
  - `GEN` = `backend/servicios/generacion/src`;
  - `SPA` = `frontend/src`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** el token solo se lee en `AutoridadesDelToken`, y `AlcanceDeCarrera` solo se usa en los adaptadores de entrada. La aplicación y el dominio reciben `Optional<String> carrera`: vacío significa todas.
- **Filtro en el servidor (FR-014):** la pantalla solo muestra lo que recibe.
- **Falla cerrada:** sin carrera no se ve nada. Un código sin carrera real no coincide con nada.
- **Límites (principio V):** sin llave foránea a Académico; Roles guarda un código.
- **Datos (principio IX):** la carrera es un código; ningún dato personal nuevo en el token.
- **Mensajes:** en «tú» (D-74); los errores, del catálogo con sus 8 columnas (D-72).
- **Validación (principio XIV):** `^[0-9A-Z-]{1,20}$` para la carrera, en el contrato y el servidor; en la pantalla se elige de una lista.
- **Pruebas:**
  - Mockito solo en la aplicación;
  - fuera de ahí, dobles escritos a mano;
  - el reclamo del token y los filtros, con tokens con reclamos reales, que pasan por `AutoridadesDelToken`.

---

## Phase 1: Setup

- [X] T001 Escribir D-76 en `docs/decisiones.md`, con su fila en el índice, y pasar «Siguiente número libre» a D-77 · ADR-024: Roles no consulta a Académico al guardar la carrera (research §2).

---

## Phase 2: Foundational

**Purpose**: los contratos, los errores, `AutoridadesDelToken` y `AlcanceDeCarrera`. Bloquea las historias.

- [X] T002 Contratos, según [contracts/roles.md](contracts/roles.md):
  - `contratos/roles/openapi.yaml`: `indicarCarrera`, `quitarCarrera`, `CarreraDelCoordinador`, y `carrera` en `RolesDeUsuario` y `Autorizacion`;
  - `contratos/academico/openapi.yaml`: el 403 en `consultarOferta` y `consultarAsignaturaParalelo`, y el parámetro `carrera` en `consultarAsignaturasOfertadas`;
  - `contratos/generacion/openapi.yaml`: el 403 en `consultarSugerencia` y `consultarAfinidadDelDocente`;
  - `contratos/comun/errores.md`: los 3 códigos, con sus 8 columnas.

  Actualizar el comentario de cabecera de cada contrato. Regenerar las interfaces con Gradle y la SPA con `generar:api`.
- [X] T003 Agregar `COMUN_CARRERA_SIN_INDICAR` a `CodigosComunes` en `backend/plataforma/errores-nucleo/src/main/java/ec/mdd/plataforma/errores/nucleo/CodigosComunes.java`, y ajustar `RespuestaErrorTest`.
- [X] T004 Pruebas, sin dobles:
  - `SEG/test/java/ec/mdd/plataforma/seguridad/AutoridadesDelTokenTest.java`, con tokens de prueba: las autoridades `ALCANCE_PROPIO_…` y `CARRERA_…`; sin `:PROPIO` ni carrera, no aparecen; el token de proceso sigue igual;
  - `backend/plataforma/errores-nucleo/src/test/java/ec/mdd/plataforma/errores/nucleo/AlcanceDeCarreraTest.java`, sobre nombres de autoridades:
    - un permiso de la oferta sin alcance propio: ve todas;
    - los tres con alcance propio y carrera: solo esa;
    - con alcance propio y sin carrera: `CarreraSinIndicar`;
    - uno con alcance propio y otro sin él: ve todas (RF-03);
    - ningún permiso de la oferta, como Talento Humano: ve todas;
    - `DOCENTES:LECTURA` sin alcance propio no abre todo a quien tiene la oferta con alcance propio;
    - sin autoridades.
- [X] T005 Implementar:
  - `AutoridadesDelToken` en `SEG/main/java/ec/mdd/plataforma/seguridad/AutoridadesDelToken.java`;
  - `AlcanceDeCarrera` y `CarreraSinIndicar` en `backend/plataforma/errores-nucleo/src/main/java/ec/mdd/plataforma/errores/nucleo/`;
  - el catálogo `ErroresDeAlcance`, registrado en `ErroresAutoConfiguracion` de `:plataforma:errores`.

  Hace pasar T004.

**Checkpoint**: `./gradlew :plataforma:seguridad:check :plataforma:errores-nucleo:check :plataforma:errores:check` en verde.

---

## Phase 3: User Story 1 - Indicar la carrera (Priority: P1)

**Goal**: el Administrador indica la carrera, y el token la lleva.

**Independent Test**: indicar una carrera y verla en «Roles de usuarios»; iniciar sesión y leer el reclamo.

- [X] T006 [US1] Crear `ROL/main/resources/db/migration/roles/V8__carrera_del_coordinador.sql`: la tabla `carrera_de_coordinador`, según [data-model.md](data-model.md), y `INDICAR_CARRERA` y `QUITAR_CARRERA` en la restricción de `registro_cambio`.
- [X] T007 [US1] Pruebas de `ROL/test/java/ec/mdd/roles/aplicacion/AsignarCarreraServicioTest.java`, con Mockito:
  - indicar la carrera a un coordinador la guarda y la registra;
  - indicar otra la reemplaza;
  - a quien no tiene el rol: `CarreraSinRol`, sin guardar;
  - un usuario con mayúsculas se normaliza;
  - quitarla la borra y la registra;
  - quitarla a quien no tiene: `UsuarioSinCarrera`.
- [X] T008 [US1] Implementar el puerto `ROL/main/java/ec/mdd/roles/puertos/entrada/AsignarCarrera.java`, sus excepciones, `ROL/main/java/ec/mdd/roles/aplicacion/AsignarCarreraServicio.java`, el puerto `RepositorioCarrerasDeCoordinadores` y `ROL/main/java/ec/mdd/roles/adaptadores/salida/RepositorioCarrerasDeCoordinadoresJdbc.java`, con su prueba de repositorio. Hace pasar T007.
- [X] T009 [US1] Sumar a la prueba de `AsignarRolesServicio`: al quitar `COORDINADOR_CARRERA`, la carrera se borra y se registra; al quitar otro rol, no; si borrar la carrera falla, los roles no cambian. `AsignarRolesServicio` recibe `Transaccion`, como `GestionarRolesServicio`: ajustar su bean y `AsignarRolesServicioTest`. Implementarlo en `ROL/main/java/ec/mdd/roles/aplicacion/AsignarRolesServicio.java`.
- [X] T010 [US1] Sumar a la prueba de `ConsultarAutorizacionServicio`: la autorización trae la carrera; sin carrera, viene vacía. Implementarlo en `Autorizacion` y `ConsultarAutorizacionServicio`.
- [X] T011 [US1] Pruebas de contrato y permisos en `ROL/test/java/ec/mdd/roles/adaptadores/entrada/`:
  - `indicarCarrera`: 200, 400 por código inválido, válidos e inválidos, 409 y 403;
  - `quitarCarrera`: 200, 404 y 403;
  - `listarRolesDeUsuarios` trae la carrera;
  - `consultarAutorizacion` trae la carrera;
  - `VerificadorDePermisos` y `VerificadorDeCatalogo` con lo nuevo.
- [X] T012 [US1] Implementar las dos operaciones en `ROL/main/java/ec/mdd/roles/adaptadores/entrada/RolesControlador.java`, la carrera en sus respuestas, en `AutorizacionControlador`, y los 2 códigos en `ErroresRoles`, con sus beans. Hace pasar T011.
- [X] T013 [US1] Sumar a las pruebas de `EmisorJwt` y `AutorizacionDesdeRoles`: el reclamo `carrera` va si existe, y no va si no existe; también al renovar. Implementarlo en `AutorizacionDeUsuario`, `AutorizacionDesdeRoles` y `USU/main/java/ec/mdd/usuarios/adaptadores/salida/EmisorJwt.java`.
- [X] T014 [US1] Pruebas en `SPA/pantallas/roles/Roles.test.tsx`:
  - la carrera se ve junto a los roles;
  - con escritura, «Carrera» solo en los coordinadores;
  - la lista trae solo las carreras vigentes;
  - indicarla y quitarla;
  - un 409 muestra el aviso del catálogo;
  - sin escritura, no hay acción;
  - dice que vale desde la próxima sesión.
- [X] T015 [US1] Implementar la carrera en `SPA/pantallas/roles/RolesDeUsuarios.tsx`, con la lista de `useCarreras`, en un diálogo propio. Hace pasar T014.
- [X] T016 [US1] `frontend/e2e/roles.spec.ts`: el recorrido con `simularApi` y axe sobre el diálogo, ya en opacidad 1.

**Checkpoint**: la carrera se indica, se ve y viaja en el token.

---

## Phase 4: User Story 2 - Trabajar solo con su carrera (Priority: P1)

**Goal**: la oferta, la sugerencia y la afinidad, solo de su carrera.

**Independent Test**: con dos carreras en la oferta y un coordinador de una, abrir la oferta y pedir una sugerencia de cada una.

- [X] T017 [P] [US2] Pruebas de `OfertaDelPeriodo.deCarrera` en `ACA/test/java/ec/mdd/academico/dominio/OfertaDelPeriodoTest.java`: solo sus paralelos, sus horas a cubrir, también las del periodo, y vacía con una carrera sin oferta. Implementarlo en `ACA/main/java/ec/mdd/academico/dominio/OfertaDelPeriodo.java`.
- [X] T018 [P] [US2] Pruebas de contrato en `ACA/test/java/ec/mdd/academico/adaptadores/entrada/OfertaContratoTest.java`: con un coordinador, con un token con reclamos reales, solo su carrera; `consultarAsignaturaParalelo` de otra, 403. Implementarlo en `OfertaControlador`, con `AlcanceDeCarrera`.
- [X] T019 [US2] Pruebas de `consultarAsignaturasOfertadas` con `carrera` en `InternoAcademicoContratoTest` y `ConsultarAsignaturasOfertadasServicioTest`. Implementarlo en su puerto, su servicio y `InternoAcademicoControlador`.
- [X] T020 [US2] Sumar a `DatosDesdeProcesoDatosTest`: la consulta lleva `carrera` si la hay, y no la lleva si no. Implementarlo en `DatosParaAfinidad` y `DatosDesdeProcesoDatos`.
- [X] T021 [US2] Sumar a `ConsultarSugerenciaServicioTest` y `ConsultarAfinidadServicioTest`: un paralelo de otra carrera lanza `OtraCarrera`, sin pedir pesos ni docentes; con su carrera, responde con candidatos de otra carrera base; la afinidad pide solo su carrera. Implementarlo en los dos servicios y sus puertos.
- [X] T022 [US2] Sumar a `SugerenciaContratoTest` y `AfinidadContratoTest`, con tokens con reclamos reales con `:PROPIO`: la carrera propia responde 200 y otra, 403 sin datos. Implementarlo en `SugerenciaControlador` y `AfinidadControlador`, con `AlcanceDeCarrera`.

---

## Phase 5: User Story 3 - Sin carrera no se ve nada (Priority: P1)

**Goal**: el aviso y ningún dato.

**Independent Test**: con un coordinador sin carrera, abrir la oferta y la ficha de un docente.

- [X] T023 [US3] Sumar a `OfertaContratoTest`, `SugerenciaContratoTest` y `AfinidadContratoTest`: sin carrera, 403 `COMUN_CARRERA_SIN_INDICAR`, sin datos.
- [X] T024 [US3] Pruebas en `SPA/pantallas/oferta/Oferta.test.tsx` y `SPA/pantallas/docentes/Docentes.test.tsx`: el aviso del catálogo, sin tabla ni puntajes. También el texto «No hay oferta para mostrar en este periodo.» en `Oferta.tsx` y `AfinidadDelDocente.tsx`, en lugar de los dos actuales, con sus pruebas ajustadas (US2, escenario 5).

---

## Phase 6: User Story 4 - Quien alcanza todo sigue viendo todo (Priority: P1)

**Goal**: nada cambia para quien tiene alcance total.

**Independent Test**: con `vicerrectorado.demo`, abrir la oferta y pedir sugerencias de cualquier carrera.

- [X] T025 [US4] Sumar a `OfertaContratoTest`, `SugerenciaContratoTest` y `AfinidadContratoTest`: con alcance total, todas las carreras; con un rol `:PROPIO` y otro de alcance total, también; con `DOCENTES:LECTURA` y sin permisos de oferta, la afinidad como siempre.

**Checkpoint**: los cuatro escenarios del issue pasan.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T026 Ajustar `deploy/probar_afinidad.sh` y `deploy/probar_sugerencia.sh`: leen con `vicerrectorado.demo`, con alcance total.
- [X] T027 `deploy/probar_carrera_coordinador.sh`, según [quickstart.md](quickstart.md), con los procesos reales y dos carreras, y su paso en `.github/workflows/perfil-consolidado.yml`:
  - sin carrera: 403 `COMUN_CARRERA_SIN_INDICAR` en la oferta, la sugerencia y la afinidad;
  - indicar la carrera con `admin.demo`, y su 409 y su 403;
  - en la próxima sesión, solo su carrera, frente a un conteo hecho aparte con los CSV (SC-001);
  - la sugerencia de otra carrera, 403;
  - cambiar la carrera y renovar;
  - `vicerrectorado.demo` ve las 7 carreras;
  - quitar el rol borra la carrera;
  - la oferta en menos de 3 s (SC-005);
  - la base limpia al terminar, también los roles y la carrera de `coordinacion.demo`.
- [ ] T028 Crear `specs/030-carrera-coordinador/evidencia.md`, y verificar:
  - todas las pruebas del backend y la SPA, con su cobertura por módulo, regenerando JaCoCo;
  - Spotless, SpotBugs, ESLint y TypeScript;
  - los scripts de `deploy/`, shellcheck y Gitleaks;
  - `deploy/equipo/probar_equipo.sh`;
  - los criterios de terminado de la constitución (§6).

  Commit, push, PR en borrador con `Refs #166` y marcarlo listo cuando sus comprobaciones pasen.
- [X] T029 Fusionar solo con la aprobación del autor. Verificar el despliegue en el equipo de demostración con evidencia (GitHub `demostracion` y los 8 contenedores sanos con la versión nueva). Después: la comprobación del autor, indicando la carrera con `admin.demo`, y el PR de evidencia con `Closes #166`. El autor mide cuánto tarda en indicar la carrera (SC-004); si no lo mide, la evidencia lo dice.

---

## Dependencies & Execution Order

- **T001:** antes de todo.
- **Fundacional:** T002 antes de todo el código; T003 antes de T005; T004 antes de T005.
- **US1:** T006 antes de T008; T007 a T012 en orden; T013 depende de T010; T014 a T016 dependen de T012.
- **US2:** depende de T005 y T013. T017 y T018 son [P]; T019 antes de T020; T020 antes de T021; T021 antes de T022.
- **US3 y US4:** dependen de US2.
- **Cierre:** al final.

## Parallel Example

```text
Después de T005: T017 (dominio de Académico) y T018 (su controlador) a la vez.
```

## Implementation Strategy

- **MVP:** las fases 1 a 4 dan los escenarios del issue por la API.
- **Después:** las garantías de sin carrera y de alcance total (fases 5 y 6), y el cierre. Cada fase deja el sistema en verde.
