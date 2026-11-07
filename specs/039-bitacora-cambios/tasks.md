---

description: "Tareas de F17-H01: registrar cada cambio en la bitácora"
---

# Tasks: Registrar cada cambio en la bitácora

**Input**: documentos de diseño en `specs/039-bitacora-cambios/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si una prueba no compila, se agrega primero un esqueleto, y la implementación va en un comando aparte.

**Decisiones**:
- la aclaración Q1 del autor del 2026-11-05 (en la spec), revisada al planificar;
- D-90 y ADR-024, escritas en [`docs/decisiones.md`](../../docs/decisiones.md) el 2026-11-05, y D-91, aprobada por el autor el 2026-11-05;
- D-47, D-48, D-49, ADR-016, ADR-018 y ADR-023;
- RF-44 y RNF-INT-02.

**Organization**:
- **Fundacional:** contratos, errores, la entrega local, el dominio y la tabla de la bitácora, y el receptor.
- **US1:** cada servicio publica sus cambios, con el valor anterior y el nuevo.
- **US2:** no se puede borrar. **US3:** no se pierde. **US4:** sin datos sensibles.
- **Cierre:** el evento de prueba, el script, la evidencia y la fusión.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US4.
- **Rutas:** relativas a la raíz. Abreviaturas, cada una con `main` y `test` en `java/ec/mdd/<servicio>` e `integrationTest` donde aplica:
  - `USU` = `backend/servicios/usuarios/src`;
  - `GEN` = `backend/servicios/generacion/src`;
  - `ACA` = `backend/servicios/academico/src`;
  - `DOC` = `backend/servicios/docentes/src`;
  - `NOR` = `backend/servicios/normativa/src`;
  - `ROL` = `backend/servicios/roles/src`;
  - `VAL` = `backend/servicios/validacionocs/src`;
  - `EVT` = `backend/plataforma/eventos/src`, con `java/ec/mdd/plataforma/eventos`.

---

## Phase 1: Setup

- [X] T001 Revisar el glosario (`~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/sdd/modelo_dominio.md`, restaurado por el autor el 2026-11-05). Ya define «Bitácora» con `EntradaBitacora` (§7.6) y el lenguaje publicado `EntradaBitacora` de Identidad, con «quién, qué, cuándo, objeto, valor anterior y valor nuevo» (§4). Los artefactos usan esos nombres; no hay términos nuevos.
- [X] T002 Confirmar la regla del nombre de usuario antes de fijar el patrón `^[A-Za-z0-9._-]{1,64}$` de `data-model.md`: revisar `USU/main/resources/db/migration/usuarios/V2__usuarios.sql`, `USU/main/resources/usuarios-demo.yaml` y las columnas `usuario varchar(64)` de los cinco `registro_cambio`. Si la definición difiere, ajustar `data-model.md` y `contracts/eventos.md` antes de T003 (principio XIV). **Resultado (2026-11-05):** 1 a 64 caracteres, sin patrón (`nombre_usuario varchar(64)` y el parámetro `Usuario` de Roles); se ajustaron `data-model.md` y `contracts/eventos.md`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: los contratos, los errores, la entrega local, la bitácora y su receptor.

**⚠️ CRITICAL**: ninguna historia empieza antes de que el receptor registre una entrada válida.

- [X] T003 Agregar el esquema `EntradaBitacora` a `contratos/usuarios/openapi.yaml` (lenguaje publicado de Identidad, `modelo_dominio.md` §4), tal como `contracts/eventos.md`: `required: [usuario, accion, momento, objeto]`, `additionalProperties: false`, `usuario` con `minLength: 1` y `maxLength: 64`, sin patrón (T002), `accion` con `'^[A-Z][A-Z_]{2,39}$'`, `periodo` con `'^[A-Za-z0-9._-]{1,20}$'`, y `objeto`, `anterior` y `nuevo` como objetos.
- [X] T004 Actualizar `contratos/usuarios/openapi.yaml` según `contracts/usuarios.md`: la descripción de `recibirEvento` con los seis tipos `<servicio>.EntradaBitacora`, sin la mención de prueba, y el `400 USUARIOS_ENTRADA_BITACORA_INVALIDA`. Agregar el código a `contratos/comun/errores.md` con sus 8 columnas, y actualizar la causa de `COMUN_EVENTO_NO_RECONOCIDO` (tipo no admitido o prefijo distinto del origen).
- [X] T005 [P] Crear `EVT/test/.../ClienteDeEventosLocalTest.java`, con esqueleto, y verlo fallar: con un `ReceptorLocal` para el destino lo llama sin HTTP ni token; su excepción es una `EntregaFallida` sin el contenido en el mensaje; sin receptor local entrega por HTTP como hoy; sin ninguno de los dos, `EntregaFallida`. Después crear `EVT/main/.../ReceptorLocal.java` (JavaDoc del contrato, `contracts/plataforma-eventos.md`) y cambiar `ClienteDeEventos` y `EventosAutoConfiguracion` para recibir los `ReceptorLocal` del contexto, en un comando aparte (ADR-024).
- [X] T006 [P] Crear `USU/test/.../dominio/EntradaBitacoraTest.java`, con esqueleto, y verlo fallar, una prueba por regla y por límite de `data-model.md`:
  - `servicio` uno de `generacion`, `academico`, `docentes`, `normativa`, `roles`, `validacionocs`;
  - `accion` con `^[A-Z][A-Z_]{2,39}$`; `usuario` de 1 a 64 caracteres (T002); `periodo` opcional con `^[A-Za-z0-9._-]{1,20}$`;
  - «`anterior` y `nuevo` no faltan los dos»;
  - ninguna clave de `objeto`, `anterior` ni `nuevo`, a cualquier profundidad y sin importar mayúsculas, contiene `clave`, `contrasena`, `password`, `token`, `secreto` ni `jwt`;
  - «el tamaño del contenido no pasa de 64 KB».

  Después crear `USU/main/.../dominio/EntradaBitacora.java` y `EntradaBitacoraInvalida.java`, con JavaDoc de sus invariantes, en un comando aparte.
- [X] T007 Crear `USU/main/resources/db/migration/usuarios/V5__bitacora.sql` según `data-model.md`: las 10 columnas, `id_evento` como clave primaria, `servicio varchar(20)` con su `check`, `accion varchar(40)` con su `check`, `check (anterior is not null or nuevo is not null)`, los índices `(periodo, momento)` y `(momento)`, los disparadores `bitacora_sin_cambios` y `bitacora_sin_truncar` con el mensaje «La bitácora no se puede modificar ni borrar.», y `REVOKE UPDATE, DELETE, TRUNCATE ON usuarios.bitacora FROM identidad`.
- [X] T008 Crear `USU/main/.../puertos/salida/RepositorioBitacora.java` (solo `registrar`) y `USU/main/.../adaptadores/salida/RepositorioBitacoraJdbc.java`, con prueba de integración primero en `USU/integrationTest/.../RepositorioBitacoraIntegrationTest.java`: inserta y lee los jsonb tal como llegaron.
- [X] T009 Cambiar `USU/test/.../aplicacion/RegistrarEventoRecibidoServicioTest.java`, y verlo fallar: admite los seis `<servicio>.EntradaBitacora`; registra `evento_recibido` y la entrada en la misma transacción; un `idEvento` repetido no inserta otra; un tipo no admitido o con prefijo distinto del `origen` lanza `EventoNoReconocido`; un contenido inválido lanza `EntradaBitacoraInvalida` y no registra nada. Después cambiar `RegistrarEventoRecibidoServicio` y `ConfiguracionUsuarios`, quitando `manejadoresDePrueba` y `generacion.EventoDePrueba` (research §5 y §7).
- [X] T010 Traducir `EntradaBitacoraInvalida` a `USUARIOS_ENTRADA_BITACORA_INVALIDA` (400) en `USU/main/.../adaptadores/entrada/ErroresUsuarios.java`, con la prueba del catálogo primero. Agregar al contrato del receptor en `USU/test/` los casos 204, 204 repetido, los dos 400, el 400 nuevo, el 401 sin token y el 403 con un token de usuario, con `ValidadorDeContrato` (FR-012).
- [X] T011 Crear `USU/main/.../adaptadores/entrada/ReceptorLocalDeUsuarios.java`, un `ReceptorLocal` de `usuarios` que llama a `RegistrarEventoRecibido`, con prueba primero. Agregar `:plataforma:eventos` a `backend/apps/identidad/build.gradle` y `mdd.eventos.proceso: identidad` y `mdd.eventos.plazo`/`reenvio` a `backend/apps/identidad/src/main/resources/application.yaml`, sin clave ni destinos HTTP. Verificar que `identidad` arranca, crea su registro en `eventos_identidad` y que pasan las pruebas de arquitectura y de Spring Modulith.

**Checkpoint**: una `EntradaBitacora` válida llega por HTTP o localmente y queda como entrada, una sola vez.

---

## Phase 3: User Story 1 - Un cambio queda registrado con su valor anterior y nuevo (Priority: P1) 🎯 MVP

**Goal**: cada cambio de los seis servicios llega a la bitácora con quién, qué, cuándo, sobre qué, el anterior y el nuevo (FR-001 a FR-005).

**Independent Test**: cambiar una actividad de 4 a 6 horas y ver la entrada con `anterior.horas = 4` y `nuevo.horas = 6`.

Cada servicio sigue el mismo patrón, en este orden:
1. la prueba del registro de cambio, con esqueleto y viéndola fallar: cada `Accion` declara su clase (crea, cambia o quita, research §2); el constructor rechaza un cambio que no la cumple;
2. el registro de cambio suma `anterior` y `nuevo` (mapas inmutables, vacíos si no aplican), y cada caso de uso los completa; las pruebas de aplicación de cada acción afirman sus valores;
3. la prueba del adaptador, viéndola fallar: `RegistroDeCambiosJdbc` inserta la fila local con el mismo `detalle` de antes y publica `<servicio>.EntradaBitacora` hacia `usuarios` con `{usuario, accion, momento, periodo, objeto: detalle, anterior, nuevo}`, con `periodo` según la tabla «De dónde sale `periodo`» de research §2; si la transacción se revierte, no queda evento en `eventos_<proceso>` (integración).

- [X] T012 [US1] **Generación** (8 acciones, primero por el escenario del issue): `GEN/main/.../puertos/salida/CambioDeDistributivo.java`, los casos de uso de `GEN/main/.../aplicacion/` y `GEN/main/.../adaptadores/salida/RegistroDeCambiosJdbc.java`. `ACTIVIDAD_GUARDADA` de 4 a 6 horas da `anterior {horas: "4"}` y `nuevo {horas: "6"}`; nueva, solo `nuevo`; `ACTIVIDAD_QUITADA` y `PROPUESTA_QUITADA`, solo `anterior`. `INCONSISTENCIA_JUSTIFICADA` nunca lleva el texto de la justificación. `RepositorioConsolidacionJdbc` sigue leyendo `detalle ->> 'periodo'` sin cambios.
- [X] T013 [US1] Agregar a `GEN/test/.../aplicacion/` la prueba de que una propuesta o una actividad rechazada por una regla dura no registra ni publica nada (FR-004, US1-4).
- [X] T014 [P] [US1] **Académico** (23 acciones): `ACA/main/.../puertos/salida/CambioRegistrado.java`, sus servicios de `aplicacion/` (también `Detalles.java`) y `RegistroDeCambiosJdbc`. Las `*_CORREGIDA` pasan sus pares `{campo, anterior, nuevo}` a los mapas; las de estado, el estado anterior y el nuevo. `academico` ya tiene `:plataforma:eventos`.
- [X] T015 [P] [US1] **Docentes** (7 acciones): `DOC/main/.../puertos/salida/CambioRegistrado.java`, sus servicios de `aplicacion/` y `RegistroDeCambiosJdbc`. Agregar `:plataforma:eventos` a `backend/servicios/docentes/build.gradle`. **Datos personales (FR-014, D-91):** con la prueba primero, el adaptador quita de `anterior` y `nuevo`, y también de los `cambios` copiados en `objeto`, los valores de `primerNombre`, `segundoNombre`, `primerApellido`, `segundoApellido`, `correoInstitucional`, `telefono` y `condicionEspecial`, y nombra esos campos en `objeto.camposPersonales`. El registro local no cambia.
- [X] T016 [P] [US1] **Normativa** (3 acciones): `NOR/main/.../puertos/salida/CambioDeNormativa.java`, sus servicios y `RegistroDeCambiosJdbc`. `VERSION_ASOCIADA` lleva la versión anterior del periodo si la había. Agregar `:plataforma:eventos` a `backend/servicios/normativa/build.gradle`. La versión 1 se registra con usuario `sistema`.
- [X] T017 [P] [US1] **Roles** (9 acciones): `ROL/main/.../puertos/salida/CambioDeRoles.java`, sus servicios y `RegistroDeCambiosJdbc`. `ASIGNAR_ROL` y `QUITAR_ROL` llevan los roles del usuario antes y después; `DESIGNAR_RESPONSABLE`, el anterior si lo había. Agregar `:plataforma:eventos` a `backend/servicios/roles/build.gradle`; la entrega es local (ADR-024).
- [X] T018 [P] [US1] **Validación OCS** (1 acción): crear `VAL/main/.../puertos/salida/CambioDeFlujo.java` y `RegistroDeCambios.java`, y `VAL/main/.../adaptadores/salida/PublicadorDeCambios.java`, que solo publica (su fila de carga ya es el registro local). `CargarConfiguracionServicio` registra `CONFIGURACION_FLUJO_CARGADA` con `sobre {periodo}`, `anterior {version}` si había una cargada y `nuevo {version}`. Un rechazo no publica.
- [X] T019 [US1] Agregar `mdd.eventos.destinos.usuarios: ${MDD_URI_IDENTIDAD:http://identidad:8080}/interno/usuarios/eventos` a `backend/apps/datos/src/main/resources/application.yaml`. Verificar que `identidad` admite la clave del proceso `datos` para el token (ya está en `mdd.usuarios.procesos.datos.clave`).
- [X] T020 [US1] Verificar que todo cambio guardado pasa por `RegistroDeCambios` (FR-005): listar en `evidencia.md` cada caso de uso que escribe datos en Generación, Académico, Docentes, Normativa y Roles (cada servicio de `aplicacion/` que llama a un `guardar`, `insertar`, `actualizar` o `borrar` de sus repositorios) con la acción que registra. Un caso que escribe sin registrar se corrige con su prueba primero, o se anota por qué no es un cambio del negocio (por ejemplo, `evento_recibido` o los intentos fallidos de inicio de sesión). Verificar también las cargas de archivo: cada traductor de `ACA/main/.../aplicacion/cargas/`, `DOC/main/.../aplicacion/cargas/` y `NOR/main/.../aplicacion/cargas/` aplica sus filas por servicios que registran el cambio con el usuario que confirmó. Si alguno escribe sin pasar por `RegistroDeCambios`, corregirlo con su prueba primero (FR-002).

**Checkpoint**: los seis servicios publican y la bitácora recibe el escenario del issue.

---

## Phase 4: User Story 2 - La bitácora no se puede borrar ni modificar (Priority: P1)

**Goal**: ninguna operación ni sentencia cambia una entrada (FR-009, FR-010).

**Independent Test**: un `DELETE` con el rol `identidad` falla.

- [X] T021 [US2] Agregar a `USU/integrationTest/.../RepositorioBitacoraIntegrationTest.java`, y verlo fallar contra una copia de `V5` sin los disparadores ni el `REVOKE`: `UPDATE`, `DELETE` y `TRUNCATE` con el rol del proceso fallan con el mensaje del disparador; también con el dueño; la entrada sigue igual (SC-005).
- [X] T022 [US2] Crear `USU/test/.../adaptadores/entrada/BitacoraSinBorradoContratoTest.java`, que recorre todos los `contratos/*/openapi.yaml` y falla si alguna operación `PUT`, `PATCH` o `DELETE` toca la bitácora, o si `recibirEvento` deja de ser la única escritura (FR-009).

**Checkpoint**: la bitácora solo crece.

---

## Phase 5: User Story 3 - No se pierde nada si el proceso de identidad se cae (Priority: P1)

**Goal**: los cambios hechos con `identidad` caída llegan cuando vuelve, una vez cada uno (FR-006 a FR-008).

**Independent Test**: detener `identidad`, hacer cambios y verlos llegar.

- [X] T023 [US3] Agregar a `EVT/integrationTest/` una prueba con un `ReceptorLocal` que falla y después acepta: la publicación queda `FAILED` y el reenvío la completa, sin duplicar.
- [X] T024 [US3] Agregar a `GEN/integrationTest/` una prueba con el receptor HTTP caído: el cambio se confirma igual, su evento queda pendiente en `eventos_distributivo` y el caso de uso no ve ningún error (FR-008). **Hecho en `apps/distributivo/src/integrationTest` (`BitacoraDesdeGeneracionIntegrationTest`):** el registro de publicaciones necesita el contexto del proceso, que la prueba de integración del servicio no levanta.

**Checkpoint**: la caída de `identidad` no pierde ni repite entradas.

---

## Phase 6: User Story 4 - Sin datos sensibles (Priority: P1)

**Goal**: ninguna entrada guarda contraseñas ni tokens (FR-011, SC-004).

**Independent Test**: revisar todas las entradas de las pruebas.

- [X] T025 [P] [US4] Agregar a las pruebas del registro de cambio de cada servicio de T012 a T018 un caso que recorre todas sus acciones y afirma que ninguna clave de `detalle`, `anterior` ni `nuevo` parece un secreto, con la misma lista de T006. En Docentes, afirmar además que ninguna entrada lleva el valor de los siete campos personales de D-91 (FR-014), tampoco al aplicar una carga 4. **Resultado:** cada registro de cambio rechaza una clave que parece un secreto, con la excepción de `palabrasClave` (D-92, decidida por el autor el 2026-11-06). La carga de docentes es la 06 (`AplicadorDocentesIntegrationTest`).
- [X] T026 [US4] Agregar a `RegistrarEventoRecibidoServicioTest` un evento con una clave `token` anidada en `nuevo`: se rechaza con `EntradaBitacoraInvalida` y no se registra.

**Checkpoint**: la historia está completa.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T027 Retirar el evento de prueba de B00-H05 (research §7): de Generación, `EventosPruebaControlador`, `PublicarEventoDePruebaServicio`, `PublicarEventoDePrueba`, la propiedad `mdd.generacion.eventos-prueba.habilitado` (también en `backend/apps/distributivo/src/main/resources/application.yaml`, `compose.yaml` y `.env.ejemplo` si aparece) y sus pruebas. Crear `GEN/main/resources/db/migration/generacion/V7__sin_evento_de_prueba.sql` con `DROP TABLE generacion.evento_prueba`. Quitar `publicarEventoDePrueba` y la etiqueta `EventosPrueba` de `contratos/generacion/openapi.yaml` (`contracts/generacion.md`).
- [X] T028 Crear `deploy/probar_bitacora.sh` con los 11 pasos de `contracts/demostracion.md`, sobre los datos de prueba, con consultas de solo lectura y la base limpia al terminar, como los demás scripts. Borrar `deploy/probar_eventos.sh` y cambiar el paso «Eventos de punta a punta» de `.github/workflows/perfil-consolidado.yml` por «Bitácora de punta a punta».
- [X] T029 Revisar el JavaDoc de `ReceptorLocal`, `EntradaBitacora`, `RepositorioBitacora` y de los registros de cambio (principio XII), y los comentarios que nombran el evento de prueba en `:plataforma:eventos` y en Usuarios.
- [X] T030 Crear `specs/039-bitacora-cambios/evidencia.md`, y verificar:
  - todas las pruebas del backend, con su cobertura por módulo, regenerando JaCoCo (60 % por módulo);
  - `integrationTest` completo;
  - Spotless y SpotBugs;
  - los scripts de `deploy/`, shellcheck con una copia nueva en el scratchpad y Gitleaks en modo `git` sobre un clon, con `.gitleaks.toml`;
  - `deploy/equipo/probar_equipo.sh`;
  - los criterios de terminado de la constitución (§6), con el glosario de T001.

  Commit, push, PR en borrador con `Refs #69`, y marcarlo listo cuando sus comprobaciones pasen, sin empujar a la vez.
- [ ] T031 Fusionar solo con la aprobación del autor y con todas las comprobaciones en verde. Verificar el despliegue en el equipo de demostración con evidencia (GitHub `demostracion` y los 8 contenedores sanos con la versión nueva, y la migración `V5` en el registro de `identidad`). Comprobación del autor con `quickstart.md`, «A mano, en la demo». PR de evidencia con `Closes #69`, y borrar las ramas.

---

## Dependencies & Execution Order

- **T001:** hecha.
- **T002:** antes de T003 y T006.
- **Fundacional:** T003 y T004 antes del código. T005 y T006 no dependen entre sí. T007 → T008 → T009 → T010. T011 depende de T005 y T009.
- **US1:** depende de la fase 2. T012 primero (es el MVP); T014 a T018 en cualquier orden después de T012, que fija el patrón. T013 depende de T012. T019 depende de T014 a T016. T020 depende de T014 a T016.
- **US2:** T021 depende de T007 y T008. T022 depende de T004.
- **US3:** T023 depende de T005. T024 depende de T012.
- **US4:** T025 depende de T012 a T018. T026 depende de T009.
- **Cierre:** T027 depende de T009 (el receptor ya no admite el evento de prueba); los dos van en el mismo PR, porque entre uno y otro `probar_eventos.sh` fallaría en la CI. T028 depende de US1 a US4 y de T027. T029 a T031, al final.

## Parallel Example

```text
Después de T004: T005 (plataforma) y T006 (dominio de Usuarios) a la vez.
Después de T012: T014 (Académico), T015 (Docentes), T016 (Normativa), T017 (Roles) y T018 (Validación OCS), cada uno en su servicio.
```

## Implementation Strategy

- **MVP:** las fases 1 y 2 y T012 a T013: el escenario del issue (4 → 6 horas) llega a la bitácora desde Generación.
- **Después:** los otros cinco servicios, uno por uno; cada uno deja el sistema en verde. Si la semana no alcanza, Académico (T014, 23 acciones) es el candidato a separar en otra historia, con una decisión nueva (plan, Riesgos).
- **Luego:** las garantías (US2, US3 y US4), el retiro del evento de prueba, el script y el cierre.
