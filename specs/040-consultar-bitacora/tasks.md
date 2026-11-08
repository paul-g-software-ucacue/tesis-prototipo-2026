---

description: "Tareas de F17-H02: consultar la bitácora"
---

# Tasks: Consultar la bitácora

**Input**: documentos de diseño en `specs/040-consultar-bitacora/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si una prueba no compila, se agrega primero un esqueleto, y la implementación va en un comando aparte.

**Decisiones**:
- las aclaraciones Q1 y Q2 del autor del 2026-11-07 (en la spec);
- D-94, escrita en [`docs/decisiones.md`](../../docs/decisiones.md) el 2026-11-07;
- D-16, D-65, D-70, D-72, D-90, D-91, ADR-016 y ADR-021;
- RF-44 y RNF-INT-02.

**Organization**:
- **Fundacional:** el contrato, el filtro, la página y el puerto.
- **US1:** la consulta con filtros, en la API y en la pantalla (MVP).
- **US2:** solo los autorizados la consultan.
- **US3:** un guardado sin cambios no deja entrada (D-94).
- **Cierre:** el script, la evidencia y la fusión.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US3.
- **Rutas:** relativas a la raíz. Abreviaturas, cada una con `main` y `test` en `java/ec/mdd/<servicio>` e `integrationTest` donde aplica:
  - `USU` = `backend/servicios/usuarios/src`;
  - `GEN` = `backend/servicios/generacion/src`;
  - `ACA` = `backend/servicios/academico/src`;
  - `ROL` = `backend/servicios/roles/src`;
  - `SPA` = `frontend/src`.

---

## Phase 1: Setup

- [X] T001 Revisar el glosario (`~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/sdd/modelo_dominio.md` §7): «Bitácora» y `EntradaBitacora` ya están; confirmar que los artefactos no introducen términos del negocio nuevos («filtro», «página» y «corte» son de la consulta). Anotar el resultado aquí. **Resultado (2026-11-07):** «Bitácora» y `EntradaBitacora` están en §7 y §4; los artefactos no agregan términos del negocio.
- [X] T002 Confirmar que el API Gateway enruta `GET /api/usuarios/bitacora` y `GET /api/usuarios/bitacora/acciones` a `identidad` con la regla actual de `/api/usuarios/**` (configuración del gateway en `backend/apps/`), y que `backend/apps/identidad/src/main/resources/application.yaml` no las deja públicas (solo `POST /api/usuarios/sesion` y `GET /api/usuarios/jwks` lo son). Si falta algo, agregarlo con su prueba en T010. **Resultado:** `backend/gateway/src/main/resources/application.yaml` enruta `/api/usuarios/**` a `identidad`, y sus rutas públicas son solo la sesión, el JWKS y el token de proceso. Sin cambios.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: el contrato y el dominio de la consulta.

**⚠️ CRITICAL**: ninguna historia empieza antes de T003.

- [X] T003 Agregar a `contratos/usuarios/openapi.yaml`, según `contracts/usuarios.md`, la etiqueta `Bitacora` y las operaciones `consultarBitacora` (`GET /api/usuarios/bitacora`) y `listarAccionesDeBitacora` (`GET /api/usuarios/bitacora/acciones`), las dos con `x-mdd-permiso: BITACORA:LECTURA` y las respuestas `200`, `400` (solo la consulta), `401`, `403`, `500` y `503` de `comun/`. Parámetros, todos opcionales: `desde` y `hasta` `format: date`; `usuario` `minLength: 1`, `maxLength: 64`; `servicio` `enum: [generacion, academico, docentes, normativa, roles, validacionocs]`; `accion` `pattern: '^[A-Z][A-Z_]{2,39}$'`; `identificador` `pattern: '^[0-9A-Za-z._/-]{1,64}$'`; `periodo` `pattern: '^[A-Za-z0-9._-]{1,20}$'`; `pagina` `minimum: 1`, `default: 1`; `tamano` `minimum: 1`, `maximum: 100`, `default: 50`; `corte` `format: date-time`. Esquemas `PaginaDeBitacora` (`required: [entradas, total, pagina, tamano, corte]`, `entradas` con `maxItems: 100`), `EntradaConsultada` (los campos y reglas de `EntradaBitacora` más `id` `uuid` y `servicio` con el mismo enum, como objeto propio si el generador no admite el `allOf`) y `AccionDeBitacora` (`servicio` y `accion`, obligatorios). Regenerar `SPA/api/generado/usuarios.d.ts` con `npm run generar:api` y compilar `:servicios:usuarios` para que aparezca `BitacoraApi`.
- [X] T004 [P] Crear `USU/test/.../dominio/FiltroDeBitacoraTest.java`, con esqueleto, y verlo fallar, una prueba por regla y por límite de `data-model.md`:
  - `usuario` «De 1 a 64 caracteres, sin otro formato»: 0, 1, 64 y 65;
  - `servicio` uno de los seis; `accion` `^[A-Z][A-Z_]{2,39}$` y «Solo con `servicio`»;
  - `identificador` `^[0-9A-Za-z._/-]{1,64}$`: con `/`, con tilde, con espacio, 64 y 65;
  - `periodo` `^[A-Za-z0-9._-]{1,20}$`: 20 y 21;
  - `hasta` «no anterior a `desde` si las dos están»; las dos iguales es válido;
  - el intervalo `[desde 00:00, hasta + 1 día 00:00)` en `America/Guayaquil`: el 2026-11-07 empieza en `2026-11-07T05:00:00Z` y termina antes de `2026-11-08T05:00:00Z`; sin `desde` o sin `hasta`, sin límite de ese lado.
  Cada error nombra su campo, para el detalle de `COMUN_SOLICITUD_INVALIDA`. Después crear `USU/main/.../dominio/FiltroDeBitacora.java` (`de(...)`, con la zona como constante) y `FiltroDeBitacoraInvalido.java` (con el campo y el motivo en español), en un comando aparte.
- [X] T005 [P] Crear `USU/test/.../dominio/PaginaPedidaTest.java`, con esqueleto, y verlo fallar: `pagina` «Desde 1. Por defecto, 1»; `tamano` «De 1 a 100. Por defecto, 50» (0, 1, 100 y 101); `corte` opcional, que «No puede ser posterior al instante de la consulta», y sin él, el instante de la consulta (con un `Clock` fijo). Después crear `USU/main/.../dominio/PaginaPedida.java`, en un comando aparte.
- [X] T006 Agregar a `USU/main/.../puertos/salida/RepositorioBitacora.java` los métodos `consultar(FiltroDeBitacora, PaginaPedida)`, que devuelve las entradas de la página y el total con `recibida_en <= corte`, y `acciones()`, con su JavaDoc (solo lectura; ningún valor del filtro se concatena en el SQL). Crear en `USU/main/.../puertos/entrada/` el puerto `ConsultarBitacora` y los registros `PaginaDeEntradas` (`entradas`, `total`, `pagina`, `tamano`, `corte`), `EntradaConsultada` (`id`, `servicio`, `usuario`, `accion`, `momento`, `periodo` opcional, `objeto`, `anterior` y `nuevo` opcionales) y `AccionConocida` (`servicio`, `accion`). Depende de T004 y T005.

**Checkpoint**: el contrato y el dominio compilan, y las pruebas de T004 y T005 pasan.

---

## Phase 3: User Story 1 - Consultar la bitácora con filtros (Priority: P1) 🎯 MVP

**Goal**: el Vicerrector o el Administrador ven las entradas, de las más recientes a las más antiguas, filtradas por fecha, usuario, tipo de cambio, identificador y periodo, por páginas.

**Independent Test**: con `vicerrectorado.demo`, filtrar por `coordinacion.demo` y por hoy, y ver solo sus entradas con su acción, su objeto y sus valores (quickstart §3, pasos 1 a 5).

- [X] T007 [US1] Crear `USU/integrationTest/.../adaptadores/salida/RepositorioBitacoraConsultaIntegrationTest.java` con PostgreSQL real (Testcontainers y `V5`), y verlo fallar. Inserta entradas de prueba con `RepositorioBitacoraJdbc.registrar` y comprueba:
  - cada filtro por separado y combinados (FR-002, FR-003): usuario exacto (`coordinacion.demo` no trae `coordinacion.demo2`), servicio, servicio y acción, periodo, fechas en el borde del día en hora de Ecuador (una entrada a las 23:59 −05:00 del 7 entra en «hasta 7» y una a las 00:00 −05:00 del 8 no);
  - `identificador` igual a un texto anidado en `objeto` (dentro de una lista), igual a un número (`numero` 3 de una versión con `"3"`), y que no trae un valor solo parecido (research §4);
  - el orden `momento` descendente con `id_evento` descendente como desempate, y que una entrada con `recibida_en` posterior al `corte` no cuenta ni aparece, aunque su `momento` sea antiguo (research §2);
  - el `total`, una página más allá de la última vacía, y `acciones()` con los pares distintos, ordenados;
  - un identificador con `'` o `%` no rompe ni amplía la consulta (SQL parametrizado);
  - con 10 000 entradas, la primera página filtrada por usuario y fechas, y por identificador, responde en menos de 2 s (SC-003). Si no cumple, crear `USU/main/resources/db/migration/usuarios/V6__bitacora_por_usuario.sql` con el índice `(usuario, momento)` y anotarlo en `research.md` §5. **Resultado:** las dos consultas, con su calentamiento, en 0,154 s en total; sin índice nuevo.
  Después implementar `consultar` y `acciones` en `USU/main/.../adaptadores/salida/RepositorioBitacoraJdbc.java`, con `jsonb_path_exists(objeto, '$.** ? ((@ == $id) || (@.type() == "number" && @.string() == $id))', jsonb_build_object('id', :identificador))` o equivalente (los paréntesis fijan la precedencia; la prueba confirma que un número y un texto no se comparan entre sí), todo con parámetros, en un comando aparte. `registrar` no cambia.
- [X] T008 [US1] Crear `USU/test/.../aplicacion/ConsultarBitacoraServicioTest.java` (Mockito para `RepositorioBitacora`, `Clock` fijo), con esqueleto, y verlo fallar: arma el filtro y la página, fija el `corte` con el reloj si no viene, devuelve la página del repositorio sin cambiar las entradas (sin agregar ni reconstruir datos personales, D-91), y un filtro inválido lanza `FiltroDeBitacoraInvalido` sin llamar al repositorio. Después crear `USU/main/.../aplicacion/ConsultarBitacoraServicio.java` y registrarlo en `USU/main/.../infraestructura/ConfiguracionUsuarios.java`, en un comando aparte.
- [X] T009 [US1] Crear `USU/test/.../adaptadores/entrada/BitacoraContratoTest.java`, con esqueleto, y verlo fallar, según `contracts/usuarios.md` §«Pruebas de contrato»: cada parámetro con un caso válido y uno inválido en sus límites (`usuario` 0, 1, 64 y 65; `identificador` con tilde, con espacio, con `/` y de 65; `periodo`; `tamano` 0, 1, 100 y 101; `pagina` 0), cada inválido con `COMUN_SOLICITUD_INVALIDA` y el detalle de su campo; `accion` sin `servicio` y `hasta` antes de `desde`, con el detalle en `accion` y en `hasta`; `corte` en el futuro; la respuesta cumple `PaginaDeBitacora` sin campos de más; `listarAccionesDeBitacora` devuelve `AccionDeBitacora`. Con un token con `PERMISO_BITACORA:LECTURA`.
- [X] T010 [US1] Crear `USU/main/.../adaptadores/entrada/BitacoraControlador.java`, que implementa `BitacoraApi` con `@PreAuthorize("hasAuthority('PERMISO_BITACORA:LECTURA')")` en las dos operaciones, y traduce `FiltroDeBitacoraInvalido` a `SolicitudInvalida` (`COMUN_SOLICITUD_INVALIDA`) con el detalle del campo (research §8). Agregar `FiltroDeBitacoraInvalido` (de Usuarios, traducida por `BitacoraControlador` a `SolicitudInvalida`, como `DatoInvalido` en Docentes) a la columna de implementación de `COMUN_SOLICITUD_INVALIDA` en `contratos/comun/errores.md` (principio XIII). JavaDoc con el permiso y el contrato, como `UsuariosControlador`. T009 pasa.
- [X] T011 [P] [US1] Crear `SPA/pantallas/bitacora/validacionDeBitacora.ts` y su prueba `validacionDeBitacora.test.ts` (primero la prueba, viéndola fallar): el esquema `zod` con las reglas de `contracts/spa.md` y del contrato (`usuario` hasta 64; `identificador` `^[0-9A-Za-z._/-]{1,64}$`; `periodo` `^[A-Za-z0-9._-]{1,20}$`; `hasta` no anterior a `desde`, con el motivo «La fecha hasta no puede ser anterior a la fecha desde.»; `accion` solo con `servicio`), casos válidos e inválidos en los límites, y los filtros que admite cada campo mientras se escribe (principio XIV). **Hecho sin `zod`:** con funciones propias y los componentes `Campo` y `Eleccion` de `pantallas/comun/`, como los demás formularios de datos (Docentes, Cargos); las reglas son las mismas.
- [X] T012 [P] [US1] Crear `SPA/pantallas/bitacora/nombres.ts` (los nombres de los seis servicios: Generación, Académico, Docentes, Normativa, Roles, Validación OCS; el formato del momento con `Intl.DateTimeFormat('es-EC', { dateStyle: 'short', timeStyle: 'medium', timeZone: 'America/Guayaquil' })`; y los pares campo–valor de un objeto, con listas y objetos anidados con sangría y `null` como «sin valor») y `consultas.ts` (las dos consultas de React Query con el cliente generado), con sus pruebas.
- [X] T013 [US1] Crear `SPA/pantallas/bitacora/Bitacora.test.tsx`, y verlo fallar, y después `Bitacora.tsx`, según `contracts/spa.md`: los filtros con `Campo`, `Eleccion` y la validación de T011, la lista de acciones del servicio elegido (deshabilitada sin servicio), los filtros, la página y el `corte` en la URL (`useSearchParams`); «Consultar» vuelve a la página 1 sin `corte`; el paginador (25, 50 o 100) envía el `corte` de la primera respuesta; «N entradas» con el `total`; la tabla (Momento, Usuario, Servicio, Acción, Periodo, Objeto) con cada fila expandible al objeto, el anterior y el nuevo, con «Sin valor anterior (se creó)» y «Sin valor nuevo (se quitó)»; «No hay entradas con estos filtros.» sin error (US1-5); los detalles de un 400 en sus campos; ningún botón que cambie una entrada (FR-006).
- [X] T014 [US1] Agregar la ruta `bitacora` a `SPA/rutas.tsx` y la opción «Bitácora» a `SPA/navegacion/Marco.tsx` con `permiso: { funcionalidad: 'BITACORA', tipo: 'LECTURA' }` (D-70), con su caso en `Marco.test.tsx`: aparece con el permiso.
- [X] T015 [US1] Crear `frontend/e2e/bitacora.spec.ts`: con `vicerrectorado.demo`, después de un cambio de `coordinacion.demo`, filtrar por `coordinacion.demo` y por hoy y ver solo sus entradas (US1-1); expandir una y ver su valor anterior y nuevo. Sumar la pantalla a `frontend/e2e/accesibilidad.spec.ts`. **Hecho en el mismo `bitacora.spec.ts`**, como `flujo.spec.ts`: axe-core con las entradas y una expandida, y con un filtro marcado. Los 4 casos pasan en Chromium (`mcr.microsoft.com/playwright:v1.64.0-noble`).

**Checkpoint**: la consulta funciona por la API y por la pantalla para los dos roles autorizados.

---

## Phase 4: User Story 2 - Solo los autorizados la consultan (Priority: P1)

**Goal**: cualquier otro rol recibe el 403 del catálogo, sin saber qué permiso le falta, y no ve la opción.

**Independent Test**: con `coordinacion.demo`, pedir la bitácora por la API y por la pantalla, y recibir el 403 (quickstart §3, paso 7).

- [X] T016 [US2] Agregar a `BitacoraContratoTest` (T009), en las dos operaciones: un token de cada uno de los 8 roles de D-16 sin `BITACORA:LECTURA` (`OPERADOR_DATOS`, `COORDINADOR_CARRERA`, `GESTOR_UNIDAD`, `RESPONSABLE_CONSOLIDACION`, `RECTOR`, `MIEMBRO_OCS`, `SECRETARIA_GENERAL`, `DOCENTE`) responde 403 `SEGURIDAD_ACCESO_NO_AUTORIZADO` con el mismo cuerpo, sin el nombre del permiso; los permisos reales de `VICERRECTOR` y `ADMINISTRADOR` responden 200; sin token, 401 (FR-007, FR-008, SC-002). Leer los permisos de cada rol de `ROL/main/resources/db/migration/roles/V2__matriz_d16.sql`, no escribirlos a mano. **Hecho en `BitacoraContratoTest`**, que lee la migración del classpath; `PermisosUsuariosTest` suma las dos operaciones.
- [X] T017 [US2] Agregar a `Marco.test.tsx` que la opción «Bitácora» no aparece sin el permiso, con un caso por cada uno de los 8 roles sin `BITACORA:LECTURA` y sus permisos reales (FR-009, SC-002), y a `Bitacora.test.tsx` que un 403 muestra el aviso común de `SPA/errores/`. Agregar a `frontend/e2e/bitacora.spec.ts`: con `coordinacion.demo`, la opción no aparece y `/bitacora` muestra el aviso de acceso no autorizado (US2-1, US2-3).

**Checkpoint**: US1 y US2 juntas cumplen los dos escenarios del issue.

---

## Phase 5: User Story 3 - Sin entradas que no cambian nada (Priority: P2)

**Goal**: las seis acciones de D-94 no guardan ni registran cuando nada cambió (research §6, `contracts/sin-cambios.md`).

**Independent Test**: guardar una actividad de 4 horas con 4 horas y ver que no hay entrada nueva; cambiarla a 6 y ver la de 4 a 6 (quickstart §3, paso 6).

En cada tarea: la comparación va después de todas las validaciones (una solicitud inválida responde su error como hoy), y quién y cuándo no cuentan.

- [X] T018 [P] [US3] En `GEN/test/.../aplicacion/AsignarActividadServicioTest.java`, agregar y ver fallar: asignar una actividad que ya tiene las mismas horas no llama a `guardarActividad` ni a `registro.registrar`, y devuelve la actividad que había, con su `asignadaEn` y su `asignadaPor`; con otras horas, los llama una vez (US3-1, US3-2). Después, en `GEN/main/.../aplicacion/AsignarActividadServicio.java`, comparar `anterior` con la actividad nueva por «Mismas horas (la carrera, el docente, la actividad y el proyecto son la clave)».
- [X] T019 [P] [US3] En `GEN/test/.../aplicacion/ProponerDocenteServicioTest.java`, agregar y ver fallar: proponer el mismo docente con el mismo tipo y el mismo texto de justificación no guarda ni registra; con solo el texto distinto, guarda y registra con `objeto.textoCambiado = true` y sin el texto; con otro docente o tipo, como hoy (y `textoCambiado` si el texto también cambió). Después cambiar `GEN/main/.../aplicacion/ProponerDocenteServicio.java` (`proponer`). **Resultado:** `unDocenteExcedido_cuandoSeVuelveAProponerIgual_entoncesGuarda` pasa a `…_entoncesSeAceptaSinGuardarNada`: sigue aceptándose, pero ya no guarda (D-94).
- [X] T020 [P] [US3] En `GEN/test/.../aplicacion/JustificarInconsistenciaServicioTest.java`, agregar y ver fallar: justificar una inconsistencia ya justificada con el mismo texto no guarda ni registra y devuelve la entrada del panel con la justificación que había; con otro texto, guarda y registra con `objeto.textoCambiado = true`. Después cambiar `GEN/main/.../aplicacion/JustificarInconsistenciaServicio.java`.
- [X] T021 [P] [US3] En `ACA/test/.../aplicacion/MantenerMatriculasServicioTest.java`, agregar y ver fallar: corregir una matrícula con los mismos valores no llama a `repositorio.guardar` ni a `registro.registrar`. Después cambiar `ACA/main/.../aplicacion/MantenerMatriculasServicio.java` (`corregir`). Agregar a `AplicadorAcademicoIntegrationTest` (o a la prueba de la carga 10) que confirmar dos veces una matrícula con los mismos valores deja una sola entrada publicada, y que la fila cuenta como aplicada en el informe de la carga. **Resultado:** no hizo falta. `MotorDelLote` solo llama a `corregir` con las filas `CAMBIA`; una fila igual queda `IGUAL` y no se aplica, como ya prueba `lasSeisCargasDeAcademicoEnSuOrden_quedanAplicadasYSubidasOtraVezSonIguales`, y dos confirmaciones con la misma huella las rechaza `VistaPreviaDesactualizada`. La regla del caso de uso es el resguardo de una llamada directa.
- [X] T022 [P] [US3] En `ROL/test/.../aplicacion/GestionarRolesServicioTest.java`, agregar y ver fallar: editar un rol con el mismo nombre, la misma descripción y los mismos permisos no guarda ni registra y devuelve el rol con sus usuarios; con un permiso distinto, como hoy. Después cambiar `ROL/main/.../aplicacion/GestionarRolesServicio.java` (`editar`). Las reglas de `ReglaDeAdministracion` siguen antes de la comparación.
- [X] T023 [P] [US3] En `ROL/test/.../aplicacion/AsignarCarreraServicioTest.java`, agregar y ver fallar: indicar la misma carrera que el coordinador ya tiene no llama a `carreras.guardar` ni a `registro.registrar`; sin carrera o con otra, como hoy. Después cambiar `ROL/main/.../aplicacion/AsignarCarreraServicio.java` (`indicar`).
- [X] T024 [P] [US3] Revisar que cada acción marcada «Sí» en `research.md` §6 (Académico: correcciones, cambios de estado y `PERIODO_ABIERTO`; Docentes: `DOCENTE_CORREGIDO`, `DOCENTE_ESTADO_CAMBIADO`, `CARGO_CORREGIDO`, `PROYECTO_CORREGIDO`; Normativa: `VERSION_EDITADA`, `VERSION_ASOCIADA`; Roles: `ASIGNAR_ROL`, `QUITAR_ROL`, `DESIGNAR_RESPONSABLE`) tiene en `test/` una prueba de que guardar sin cambios no llama a `registro.registrar`. Crear la que falte, viéndola pasar con el código actual (SC-006, US3-3). Anotar en `evidencia.md` la prueba de cada acción. **Resultado:** todas tenían su prueba salvo `MALLA_ESTADO_CAMBIADO` y `ASIGNATURA_CORREGIDA`/`ASIGNATURA_INACTIVADA`/`ASIGNATURA_REACTIVADA`; se agregaron en `MantenerCarrerasServicioTest` y pasan con el código actual.
- [X] T025 [US3] Correr `deploy/probar_bitacora.sh` dos veces seguidas sobre el perfil consolidado, como la CI, y confirmar que sus 11 pasos siguen en OK con la regla de D-94 (research §9). Si un paso dependía de un guardado con el mismo valor, cambiar el valor en el script, no la regla. **Resultado:** los 11 pasos, las dos veces; sin cambios en el script.

**Checkpoint**: las tres historias funcionan juntas.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T026 Crear `deploy/probar_consulta_bitacora.sh` con los 9 pasos de `contracts/demostracion.md`, sobre los datos de prueba, por el API Gateway y con consultas de solo lectura para comparar, sin imprimir claves ni tokens, y la base limpia al terminar, como los demás scripts. Agregar su paso «Consulta de la bitácora» a `.github/workflows/perfil-consolidado.yml`, después de «Bitácora de punta a punta». Pasar shellcheck. **Resultado:** los 9 pasos pasan; la primera corrida encontró que `servicio=generacion` respondía 400 (el enumerado generado se convertía por su nombre Java) y se corrigió con `ServicioDeBitacoraDesdeTexto` y su prueba (evidencia.md).
- [X] T027 Revisar el JavaDoc de `ConsultarBitacora`, `ConsultarBitacoraServicio`, `FiltroDeBitacora`, `PaginaPedida`, `RepositorioBitacora` y `BitacoraControlador` (principio XII), y los de las seis acciones de D-94, que dicen cuándo no se registra.
- [X] T028 Crear `specs/040-consultar-bitacora/evidencia.md`, y verificar, como en `specs/039-bitacora-cambios/evidencia.md`:
  - pruebas unitarias del backend y del frontend, `integrationTest` completo, cobertura por módulo (60 %, D-32), Spotless, SpotBugs, ESLint, Prettier y los tipos;
  - los scripts de `perfil-consolidado` en el orden del workflow, sobre un perfil nuevo, con `probar_consulta_bitacora.sh`;
  - la medición de SC-003 de T007;
  - SC-004: el autor cronometra, con el quickstart §3, cuánto tarda en encontrar quién cambió una actividad dada en un día dado desde que abre la bitácora (menos de 1 minuto);
  - el caso borde del rol perdido durante la sesión: lo cubren D-62 y su prueba existente de la renovación, citada por su nombre;
  - Gitleaks y shellcheck;
  - los criterios de terminado de la constitución §6 (escenarios, contrato, arquitectura, errores, validaciones, JavaDoc, seguridad y glosario).
- [X] T029 Fusionar solo con la aprobación del autor y con todas las comprobaciones en verde. Verificar el despliegue en el equipo de demostración con evidencia (GitHub `demostracion` y los 8 contenedores sanos con la versión nueva). Comprobación del autor con `quickstart.md` §4, con los datos de `deploy/equipo/poblar_demo.sh`. PR de evidencia con `Closes #70`, y borrar las ramas. **Resultado:** #199 fusionado (`4ac135b`) y desplegado «Sano en 93 s»; comprobación del autor y SC-004 (30 s) en `evidencia.md` §«Equipo de demostración».

---

## Dependencies & Execution Order

- **Setup:** T001 y T002, en cualquier orden, antes de T003.
- **Fundacional:** T003 primero. T004 y T005 no dependen entre sí. T006 depende de T004 y T005.
- **US1:** depende de la fase 2. T007 → T008 → T009 → T010 en el backend. T011 y T012 dependen solo de T003; T013 depende de T011 y T012; T014 de T013; T015 de T010 y T014.
- **US2:** T016 depende de T010. T017 depende de T014.
- **US3:** no depende de US1 ni de US2: puede empezar después de la fase 1, en su propio pull request (plan, Riesgos). T018 a T024 en cualquier orden. T025 depende de T018 a T023.
- **Cierre:** T026 depende de US1 a US3. T027 y T028 después de T026. T029 al final.

## Parallel Example

```text
Después de T003: T004 y T005 (dominio de Usuarios), y T011 y T012 (SPA), a la vez.
En cualquier momento después de la fase 1: T018, T019 y T020 (Generación), T021 (Académico), T022 y T023 (Roles), cada una en su caso de uso, y T024 (revisión de las acciones que ya cumplían).
```

## Implementation Strategy

- **MVP:** las fases 1 y 2, US1 y US2: los dos escenarios del issue (consulta autorizada y 403), en la API y en la pantalla.
- **Después:** US3 (D-94), que puede ir en un pull request aparte; la pantalla funciona sin ella, con la entrada de 4 a 4 visible.
- **Luego:** el script, la evidencia y el cierre.
