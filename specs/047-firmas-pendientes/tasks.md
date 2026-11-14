---
description: "Tareas de F14-H01: ver mis firmas pendientes y descargar el PDF vigente"
---

# Tasks: Ver mis firmas pendientes y descargar el PDF vigente

**Input**: documentos de diseño en `specs/047-firmas-pendientes/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si no compila, primero un esqueleto, y la implementación va en un comando aparte.

**Decisiones**:
- las cuatro aclaraciones del autor del 2026-11-14, en la spec;
- D-102, que se escribe en T001;
- D-101, D-100, D-88, MD-05, ADR-003, ADR-007 y ADR-023.

**Organization**:
- **Fundacional:** D-102, los contratos, los errores y las migraciones.
- **US3:** las autoridades se reconocen por su identificación (va primero: US1 y US2 la necesitan para F2 y F3).
- **US1:** el firmante en turno ve su documento y lo descarga (MVP).
- **US2:** quien no está en turno no lo ve ni lo descarga.
- **Cierre:** la pantalla con Playwright, el script, las regresiones, la evidencia y la fusión.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US3.
- **Rutas:** relativas a la raíz. Abreviaturas, cada una con `main` y `test` en `java/ec/mdd/<servicio>`, e `integrationTest` donde aplica:
  - `FIR` = `backend/servicios/firma/src`;
  - `USU` = `backend/servicios/usuarios/src`;
  - `SPA` = `frontend/src`.

---

## Phase 1: Setup

- [ ] T001 Revisar el glosario (`~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/sdd/modelo_dominio.md` §7: «Firmante», «Paso de firma», «Documento firmable») y anotar aquí el resultado. Escribir **D-102** en `docs/decisiones.md`, en la fila del índice y en su sección:
  - el firmante esperado de cada paso se fija al generar el documento y es la persona impresa: el usuario designado o la identificación del docente del cargo vigente (research §1);
  - la cuenta de usuario guarda una identificación opcional y única; Firma la pide por ruta interna, nunca en el token (research §2);
  - el turno es el primer paso sin firma; el archivo vigente, el último de la cadena (research §3);
  - fuera del turno, el 403 común, también para un documento inexistente (research §4).

  Relacionadas: RF-38, RF-58, ADR-003, ADR-007, ADR-023, D-101.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T005.

- [ ] T002 [P] En `contratos/firma/openapi.yaml`, según `contracts/firma.md`:
  - `listarFirmasPendientes` (`GET /api/firma/pendientes`) con `FirmaPendiente`;
  - `descargarArchivoVigente` (`GET /api/firma/documentos/{documento}/archivo`), con `application/pdf`, `Content-Disposition` y `ETag`;
  - las dos con `x-mdd-permiso: DISTRIBUTIVO:FIRMA`.

  En `contratos/usuarios/openapi.yaml`, `consultarIdentificacion` (`GET /interno/usuarios/{usuario}/identificacion`), según `contracts/identidad.md`. Regenerar `firma.d.ts` en la SPA. Compilar `:servicios:firma` y `:servicios:usuarios`. Después, `./gradlew :servicios:usuarios:test --rerun`.
- [ ] T003 [P] Agregar a `contratos/comun/errores.md`, con sus 8 columnas:
  - `FIRMA_ARCHIVO_NO_DISPONIBLE` (500);
  - `USUARIOS_SIN_IDENTIFICACION` (404).

  Ver fallar primero `ErroresFirmaTest` y la prueba de `ErroresUsuarios`. Después, las excepciones `ArchivoNoDisponible` y `SinIdentificacion`, y su traducción. `SinTurnoDeFirma` la traduce el controlador al 403 común, como `SinRolDeFirma`.
- [ ] T004 [P] Migraciones, con pruebas de integración primero:
  - `FIR/main/resources/db/migration/firma/V3__firmante_esperado.sql`, según `data-model.md`:
    - el `check` de exactamente una persona;
    - la clave foránea a `generacion_documento`, no a `documento_firmable`: verificarla bloquea la fila apuntada con `FOR KEY SHARE`, y el dueño de `documento_firmable` no tiene UPDATE (data-model.md);
    - solo `insert` y `select` para el rol del proceso.
    
    Ampliar `DocumentoFirmableEsquemaIntegrationTest`, insertando con el rol del proceso: una generación, su documento y sus firmantes en la misma transacción.
  - `USU/main/resources/db/migration/usuarios/V7__identificacion.sql`: la columna con el patrón `^[0-9A-Z]{1,13}$` y el índice único parcial. La prueba de integración del esquema de Usuarios: dos cuentas con la misma identificación fallan; varias sin identificación, no.
- [ ] T005 [P] En el dominio de Firma, ver fallar primero:
  - `FirmanteEsperadoTest`:
    - exige exactamente uno de usuario o identificación;
    - `reconoce` por usuario y por identificación;
    - el rol no basta;
    - sin la identificación del solicitante, no reconoce un paso por identificación;
  - en `DocumentoFirmableTest` (nuevo):
    - `pasoEnTurno` con órdenes firmados vacíos da el orden 1;
    - con {1} da el 2;
    - con todos, vacío;
    - `archivoVigente` sin firmas es el original.

  Después, `FirmanteEsperado`, `SinTurnoDeFirma` y los métodos de `DocumentoFirmable`.

**Checkpoint**: contratos, errores, tablas y reglas del turno listos.

---

## Phase 3: User Story 3 - Las autoridades se reconocen por su identificación (Priority: P1)

**Goal**: la cuenta guarda la identificación y Firma la puede pedir; los usuarios de la demo la reciben.

**Independent Test**: `CargaUsuariosDemoTest`, la prueba de contrato de la ruta interna y `IdentificacionDesdeUsuariosTest`.

- [ ] T006 [US3] En Usuarios, ver fallar primero:
  - `UsuarioTest` (o el del dominio de la cuenta): la identificación es opcional y cumple el patrón;
  - `CargaUsuariosDemoTest`:
    - pone la identificación del YAML;
    - la corrige si cambió;
    - no escribe si nada cambió;
    - falla al arrancar con dos usuarios con la misma identificación;
  - una prueba que compara las identificaciones de `rectorado.demo` y `vicerrectorado.demo` en `usuarios-demo.yaml` con los RECTOR y VICERRECTOR vigentes de `backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba/08_cargos_autoridades.csv`.

  Después:
  - el dominio, el repositorio y `CargaUsuariosDemo`;
  - `identificacion: '0109990010'` y `'0109990028'` en `USU/main/resources/usuarios-demo.yaml`.
- [ ] T007 [US3] La ruta interna. Ver fallar primero su prueba de contrato:
  - 200 con la identificación;
  - 404 `USUARIOS_SIN_IDENTIFICACION`, igual sin cuenta que sin identificación;
  - solo con token de proceso.

  Comprobar que `UsuarioResumen`, la sesión y las claims del token no la llevan (FR-013). Después:
  - `IdentificacionControlador` en `USU/main/.../adaptadores/entrada/`;
  - su caso de uso, con el puerto de entrada.
- [ ] T008 [P] [US3] En Firma, el puerto `IdentificacionDeUsuarios.de(usuario)` → `Optional<String>`. Ver fallar primero `IdentificacionDesdeUsuariosTest`, con el servidor de pruebas de los adaptadores entre procesos:
  - 200 da la identificación;
  - 404 da vacío;
  - otro error se propaga.

  Después, el adaptador `FIR/main/.../adaptadores/salida/IdentificacionDesdeUsuarios.java` y su registro en `ConfiguracionFirma`.
- [ ] T009 [US3] Los firmantes con su persona. Ver fallar primero:
  - `FirmantesDesdeProcesosTest`:
    - el Responsable trae su usuario;
    - el Rector y el Vicerrector, la identificación del cargo;
  - en `DocumentoFirmableServicioTest`, al generar se registran los firmantes esperados con el documento, uno por paso; un rol sin persona falla como sin nombre (FR-008 de F13-H02).

  Después:
  - `FirmanteDelPeriodo(nombre, usuario, identificacion)` en `FirmantesDelPeriodo`;
  - el adaptador;
  - `Firmante` con la persona;
  - `RepositorioDocumentosJdbc` que inserta y lee `firmante_esperado` en la transacción del documento, con su prueba de integración.

**Checkpoint**: cada documento nuevo sabe quién firma cada paso, y Firma sabe la identificación de quien pide.

---

## Phase 4: User Story 1 - El firmante en turno ve su documento y lo descarga (Priority: P1) 🎯 MVP

**Goal**: «Pendientes de firma» lista los documentos del usuario en turno y le entrega el archivo vigente verificado.

**Independent Test**: `DocumentoFirmableServicioTest`, `FirmasPendientesContratoTest`, Vitest de la pantalla y `probar_documento_firmable.sh`, paso 7.

- [ ] T010 [US1] `AlmacenDeDocumentos.leer(nombre)`. Ver fallar primero en `AlmacenDeDocumentosEnDiscoTest`:
  - lee lo guardado;
  - sin el archivo, una excepción;
  - no sale de la carpeta `documentos/` con un nombre con `..` o `/`.

  Después, el método.
- [ ] T011 [US1] En la aplicación de Firma, ver fallar primero en `DocumentoFirmableServicioTest` (Mockito):
  - **`pendientes(quien)`:**
    - lista los documentos generados cuyo paso en turno reconoce al usuario, con el periodo, la versión, el orden, el rótulo («Elaborado por») y la fecha;
    - pide la identificación a Usuarios solo si algún paso en turno es por identificación, y una sola vez;
    - sin documentos, `[]`;
  - **`archivo(documento, quien)`:**
    - en turno, devuelve el nombre `distributivo-<periodo>-v<version>.pdf`, el SHA-256 y los bytes;
    - recalcula el SHA-256 y, si no coincide o falta el archivo, lanza `ArchivoNoDisponible`.

  Después: `ConsultarFirmasPendientes`, `DescargarArchivoVigente`, `FirmaPendiente`, `ArchivoVigente` y el servicio. Leer el archivo y pedir la identificación, fuera de transacciones.
- [ ] T012 [US1] `FirmasPendientesControlador` y su contrato. Ver fallar primero:
  - `FirmasPendientesContratoTest`:
    - la lista según el esquema;
    - la descarga 200 con `application/pdf`, `Content-Disposition` y `ETag`;
    - 400 con un uuid mal formado;
    - 500 `FIRMA_ARCHIVO_NO_DISPONIBLE` en JSON;
  - `PermisosFirmaTest`: las cuatro operaciones con su permiso.
- [ ] T013 [P] [US1] La pantalla. Escribir primero `SPA/pantallas/firmas/FirmasPendientes.test.tsx`:
  - con pendientes, la tabla «Firmas pendientes» con Periodo, Versión, Paso y Generado el;
  - sin pendientes, «No tienes firmas pendientes.»;
  - «Descargar el PDF» llama una vez, se desactiva mientras descarga y guarda con el nombre del servidor;
  - un 403 o un 500 se muestran con `AvisoDeError`.

  Después:
  - `SPA/pantallas/firmas/consultas.ts` y `FirmasPendientes.tsx`;
  - la ruta `/firmas-pendientes` en `SPA/rutas.tsx`, con `RutaConPermiso`;
  - la entrada «Pendientes de firma» en `SPA/navegacion/Marco.tsx`, con `DISTRIBUTIVO:FIRMA`, después de «Flujo y firmas»;
  - en sus pruebas, que la ven el Responsable, el Vicerrector y el Rector, y no el coordinador.

**Checkpoint**: el designado ve su fila y descarga el PDF.

---

## Phase 5: User Story 2 - Quien no está en turno no lo ve ni lo descarga (Priority: P1)

**Goal**: ni la lista ni la descarga dejan pasar a nadie fuera de su turno.

**Independent Test**: `DocumentoFirmableServicioTest`, `FirmasPendientesContratoTest` y `probar_documento_firmable.sh`, paso 8.

- [ ] T014 [US2] Ver fallar primero en `DocumentoFirmableServicioTest`:
  - con el paso en turno en F1, no ven nada ni descargan, y `archivo` lanza `SinTurnoDeFirma`:
    - el Vicerrector y el Rector, aunque tengan su identificación;
    - otro usuario con el rol RESPONSABLE_CONSOLIDACION;
    - un usuario sin identificación;
  - un documento inexistente, EN_CURSO o FALLIDA: no aparece y `archivo` lanza `SinTurnoDeFirma`;
  - un documento de F13-H02 sin firmantes esperados no aparece para nadie;
  - con el turno simulado en F2, el usuario con la identificación del Vicerrector lo ve y lo descarga, y uno con el rol VICERRECTOR y otra identificación, no.

  Después, lo que falte en el servicio.
- [ ] T015 [US2] En `FirmasPendientesContratoTest`, `SinTurnoDeFirma` da el 403 común (`SEGURIDAD_ACCESO_NO_AUTORIZADO`), con el mismo cuerpo que sin permiso y sin decir si el documento existe.

**Checkpoint**: el turno se respeta en el servidor.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T016 En `frontend/e2e/firmas-pendientes.spec.ts` (nuevo), con la API simulada:
  - el designado ve su fila y descarga (el evento `download` con el nombre esperado);
  - sin pendientes, el mensaje;
  - un 403 en la descarga muestra el aviso;
  - axe-core sin fallas con pendientes y sin ellos.
- [ ] T017 Ampliar `deploy/probar_documento_firmable.sh` con los pasos 7 y 8 de `contracts/demostracion.md`:
  - el SHA-256 del archivo descargado comparado con el registrado y con el `ETag`;
  - la lista y el 403 para `consolidacion.demo`, `vicerrectorado.demo`, `rectorado.demo` y `coordinacion.demo`;
  - sin imprimir claves, tokens ni la identificación;
  - shellcheck.

  Actualizar el encabezado del script.

  La limpieza de `probar_documento_firmable.sh` y de `probar_aprobacion.sh` borra `firma.firmante_esperado` de los periodos de prueba antes que `documento_firmable` y `generacion_documento`, con el superusuario. Si el dueño no puede verificar las claves foráneas, se borra con `session_replication_role = replica`, como `generacion_documento`. La revisión de «base sin datos» del paso 0 y el conteo del final la cuentan también.
- [ ] T018 Correr todos los scripts del workflow, en su orden, sobre un perfil construido desde la rama; `probar_autenticacion.sh` debe seguir igual. Recorrer `quickstart.md` §3 a mano (autor) y medir SC-001.
- [ ] T019 Correr completos:
  - `./gradlew build integrationTest`;
  - `:apps:distributivo:test` y `:apps:identidad:test` (Spring Modulith);
  - la cobertura por módulo (D-32), Spotless y SpotBugs;
  - en la SPA, los tipos, ESLint, Prettier, Vitest y Playwright (SC-004, SC-005).
- [ ] T020 Revisar el JavaDoc (principio XII):
  - `FirmanteEsperado.reconoce` y `DocumentoFirmable.pasoEnTurno` (por qué el primer paso sin firma);
  - por qué el firmante se fija al generar;
  - `IdentificacionDeUsuarios` (por qué no viaja en el token);
  - `DescargarArchivoVigente` (por qué recalcula el SHA-256 y el 403 común);
  - la identificación de la cuenta.
- [ ] T021 Crear `specs/047-firmas-pendientes/evidencia.md`, como la de 046, con:
  - las pruebas, la cobertura, la arquitectura, la SPA, los scripts, shellcheck y Gitleaks;
  - la tabla del script;
  - la comprobación a mano;
  - las desviaciones y los criterios de terminado.
- [ ] T022 Fusionar solo con la aprobación del autor y con todas las comprobaciones en verde. Verificar el despliegue en el equipo de demostración (`quickstart.md` §4, solo lectura) y abrir el PR de evidencia con `Closes #62`. Borrar las ramas solo si el autor lo pide.

---

## Dependencies & Execution Order

- **Setup (T001)** antes que todo.
- **Fundacional (T002 a T005)** en paralelo entre sí.
- **US3:**
  - T006 antes de T007;
  - T008 en paralelo con T006 y T007;
  - T009 después de T004 y T005.
- **US1:**
  - T010 en paralelo con US3;
  - T011 después de T008, T009 y T010;
  - T012 después de T011;
  - T013 puede empezar con la API simulada en cuanto exista T002.
- **US2:** T014 después de T011; T015 después de T012.
- **Cierre:** T016 después de T013; T017 y T018 después de US1 a US3; T019 a T022, al final.

### Parallel Opportunities

```text
T002, T003, T004 y T005.
T006 y T007 (Usuarios) en paralelo con T008 y T010 (Firma).
T013 (SPA) con la API simulada en cuanto exista T002.
```

## Implementation Strategy

- **MVP:** Setup, Fundacional, US3 y US1 (T001 a T013): el designado ve su fila y descarga.
- **Después:** US2, que ya queda casi cubierta por la regla del dominio; aquí se prueban los rechazos.
- **Luego:** Playwright, el script, las regresiones y el cierre.
