---

description: "Tareas de F01-H04: vista previa y confirmación de la carga"
---

# Tasks: Vista previa y confirmación de la carga

**Input**: documentos de diseño en `specs/025-confirmar-carga/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/cargas.md](contracts/cargas.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**: ADR-022 y D-74 (aprobados por el autor el 2026-10-26; se registran en T002), D-70, D-71, D-72, MD-13 y ADR-009 en [`docs/decisiones.md`](../../docs/decisiones.md); RF-10 y RF-55 de la ERS; el glosario §7 («Vista previa», «Confirmar una carga», «Historial de cargas», «Matriculados»), sin cambios.

**Organization**:
- **Fundacional:** la decisión, el contrato, el motor del lote, el historial y las operaciones de Cargas.
- **US1, la vista previa, y US2, confirmar:** por incrementos de dueño: Académico A (01 y 02), Académico B (03, 04, 05 y 10), Docentes (06, 08 y 09) y Normativa (07).
- **US3:** cancelar.
- **US4:** confirmar dos veces.
- **US5:** el historial.
- **Pantalla y cierre.**

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US5.
- **Rutas:** relativas a la raíz. Abreviaturas:
  - `PLC` = `backend/plataforma/lenguaje-cargas/src`;
  - `CAR` = `backend/servicios/cargas/src`;
  - `ACA` = `backend/servicios/academico/src`;
  - `DOC` = `backend/servicios/docentes/src`;
  - `NOR` = `backend/servicios/normativa/src`.

## Convenciones para todas las tareas

- **Capas (ADR-012):**
  - el motor del lote no depende de Spring;
  - los traductores van en `aplicacion/cargas/` de cada dueño, porque llaman a sus casos de uso;
  - `ArquitecturaTest` de cada servicio lo comprueba.
- **Límites (principio V):**
  - Cargas usa solo `ec.mdd.plataforma.cargas` y los `puertos.entrada` de los demás;
  - el paquete lleva `@NamedInterface("cargas")`, como `plataforma.eventos`;
  - `ModulosTest` lo comprueba.
- **Todo o nada (MD-13):** el aplicador de cada dueño envuelve el lote en su `Transaccion`. Los casos de uso se unen a ella, y una excepción deshace todo.
- **Datos (principio IX):**
  - el historial no guarda el contenido del archivo;
  - la vista previa no va a los logs;
  - solo datos de prueba ficticios.
- **Mensajes:** en «tú», como el catálogo (research §10). Cada código nuevo lleva sus 8 columnas (D-72).
- **Pruebas:**
  - Mockito solo en la aplicación;
  - los repositorios con Testcontainers (postgres:18.6-alpine);
  - el contrato con swagger-request-validator;
  - 60 % de líneas por módulo.
- **Evidencia y commits:** como en las historias anteriores. La evidencia va en `specs/025-confirmar-carga/evidencia.md` y los commits llevan `Refs #30`.

---

## Phase 1: Setup

- [X] T001 Crear `specs/025-confirmar-carga/evidencia.md` con estas secciones:
  - entorno;
  - base;
  - Académico A;
  - Académico B;
  - Docentes;
  - Normativa;
  - las 10 en orden;
  - pantalla;
  - cierre;
  - equipo de demostración;
  - notas.

---

## Phase 2: Foundational (bloquea a todas las historias)

- [X] T002 Registrar en `docs/decisiones.md` ADR-022, «El lenguaje publicado de las cargas en `:plataforma:lenguaje-cargas`», con su contexto, su decisión, sus alternativas (research §1) y sus consecuencias, y D-74 en el índice. Los dos llevan la aprobación del autor del 2026-10-26.
- [X] T003 En `contratos/cargas/openapi.yaml`:
  - `VistaPrevia` y `RegistroDeLaVista` dentro de `LecturaDeArchivo`;
  - `confirmarCarga`, con `multipart` de `archivo` y `huella` (`^[0-9a-f]{64}$`), el 200 `ResultadoDeConfirmacion` y el 409 con los 3 códigos;
  - `listarHistorial`, con `[CARGAS:LECTURA, DISTRIBUTIVO:LECTURA]`;
  - las descripciones, con los ejemplos de `contracts/cargas.md`.

  En `contratos/comun/errores.md`, agregar los 3 códigos de research §9 con sus 8 columnas. Regenerar y dejar `verificar:ruptura` en verde.
- [X] T004 [P] Crear el módulo `:plataforma:lenguaje-cargas` (`settings.gradle`, `build.gradle` con su comentario y `package-info.java` con `@NamedInterface("cargas")`). Primero las pruebas, en `PLC/test/java/ec/mdd/plataforma/cargas/MotorDelLoteTest.java`, sin dobles y con un traductor de prueba en memoria:
  - los nuevos, los que cambian (con sus campos), las bajas (solo dentro del alcance que declara el traductor, como el periodo) y los iguales;
  - un `2.50` igual a `2.5`;
  - la huella estable al reordenar el archivo, y distinta si cambia un valor;
  - `aplicar` con la misma huella registra y corrige, y no toca bajas ni iguales;
  - con otra huella, `VistaPreviaDesactualizada` y nada aplicado;
  - una excepción del traductor en la fila N da `RechazoDelLote` con esa fila;
  - los primeros 100 de cada tipo.

  Después crear `FilaDelLote`, `LoteDeCarga`, `TipoDeRegistro`, `CambioDeCampo`, `RegistroDeLaVista`, `VistaPreviaDelLote`, `ResultadoDelLote`, `RechazoDelLote`, `VistaPreviaDesactualizada`, `TraductorDeCarga`, `MotorDelLote` y `AplicadorDeCargas`, con JavaDoc que cite MD-13 y ADR-022.
- [X] T005 [P] El historial de Cargas. Primero las pruebas: `CAR/integrationTest/…/HistorialDeCargasJdbcIntegrationTest.java` (Testcontainers) y una prueba del esquema que lo haga fallar sin la migración. Debe comprobar:
  - que guarda y lee cada resultado;
  - el orden de lo más reciente a lo más antiguo;
  - si hay una `APLICADA` con ese hash en esa carga;
  - los `check` (resultado, totales ≥ 0, hash de 64).

  Después crear `CAR/main/resources/db/migration/cargas/V2__historial_de_cargas.sql`, `EntradaDeHistorial` (dominio), `HistorialDeCargas` (puerto de salida) y `HistorialDeCargasJdbc`. Sumar `spring-boot-starter-jdbc` a `backend/servicios/cargas/build.gradle`, con su comentario, y crear `Transaccion` con `TransaccionSpring`, como en Docentes.
- [X] T006 Primero las pruebas, en `CAR/test/java/ec/mdd/cargas/aplicacion/ConfirmarCargaServicioTest.java` (Mockito, con aplicadores de prueba):
  - con errores en las filas, `CargaConErrores` sin llamar al dueño, y su entrada `RECHAZADA` con el motivo `CARGAS_CARGA_CON_ERRORES`;
  - con un hash ya `APLICADA`, `REPETIDA` sin llamar al dueño, con la fecha de la primera y el intento registrado;
  - aplicada, con sus totales y su entrada;
  - rechazada (`RechazoDelLote`), con su entrada `RECHAZADA` y su código;
  - desactualizada, con su entrada;
  - el lote lleva las filas sin errores, el hash SHA-256 de los bytes y el usuario.

  En `ValidarArchivoServicioTest` (ampliar): la vista previa sin errores; con errores, solo `filasConErrores`. En `ConfiguracionCargas`: arrancar falla si una carga no tiene aplicador o tiene dos (prueba con dos listas de aplicadores). Después crear `ConfirmarCarga`, `ConsultarHistorial`, sus servicios, `ArchivoValidado` con la vista previa y el reparto por carga.
- [X] T007 Primero las pruebas, en `SubidaContratoTest`, `HistorialContratoTest` (nuevo), `PermisosCargasTest` y `ErroresCargasTest` (ampliar), con aplicadores de prueba en `ReferenciasDePrueba`:
  - la vista previa en el 200;
  - `confirmarCarga` responde 200 aplicada, 200 repetida y los tres 409 con su código y su detalle `fila N · campo`;
  - una `huella` que no tiene 64 caracteres hexadecimales da 400;
  - `listarHistorial` responde 200 y 404;
  - las dos operaciones exigen su permiso: comprobar una vez que fallan sin él y anotarlo;
  - los 3 códigos coinciden con el catálogo.

  Ampliar `SubidaControlador` y crear `HistorialControlador`.

**Checkpoint**: Cargas confirma contra un dueño de prueba.

---

## Phase 3: US1 y US2 · Incremento A, Académico 01 y 02 (Priority: P1) 🎯 MVP

**Goal**: la vista previa y la confirmación de la estructura orgánica y de las carreras: la carga en orden empieza a funcionar.

- [X] T008 [US1] Primero las pruebas, en `ACA/test/java/ec/mdd/academico/aplicacion/cargas/TraductoresAcademicoATest.java` (Mockito para los casos de uso y los 2 archivos de prueba):
  - las 24 unidades de la 01 y las 7 carreras de la 02 son nuevas sobre datos vacíos;
  - con los datos de prueba registrados, todo es igual;
  - con una carrera distinta en `nombre` y en `jornadas`, 1 cambia con esos dos campos;
  - `estado` `INACTIVO` en una unidad activa cambia y se aplica con `cambiarEstado`;
  - los padres de la 01 se aplican antes que los hijos.

  Crear `ACA/main/java/ec/mdd/academico/aplicacion/cargas/{TraductorEstructura,TraductorCarreras}.java` desde `DatosMaestrosDePrueba`, que pasa a reusarlos.
- [X] T009 [US2] Primero las pruebas, en `ACA/integrationTest/…/AplicadorAcademicoIntegrationTest.java` (Testcontainers):
  - confirmar la 01 y después la 02 deja 24 unidades y 7 carreras, con su registro de cambios con el usuario que confirma;
  - una segunda sede matriz en el archivo hace fallar la fila con `SedeMatrizExistente` y no aplica nada, ni las filas anteriores (SC-003);
  - dos aplicaciones a la vez con la misma huella aplican una sola vez.

  Crear `AplicadorAcademico` (cargas 01 y 02 por ahora) y su bean en `ConfiguracionAcademico`, y sumar `project(':plataforma:lenguaje-cargas')` a su `build.gradle`. `ModulosTest` pasa. T008 y T009 pasan.

**Checkpoint**: en el perfil, confirmar la 01 hace válida la 02.

---

## Phase 4: US1 y US2 · Incremento B, Académico 03, 04, 05 y 10 (Priority: P1)

- [X] T010 [P] [US1] Primero las pruebas, en `TraductoresAcademicoBTest`:
  - **03:** los 2 periodos nuevos e iguales; `semanas`, `fecha_limite_envio_senescyt`, `resolucion_aprobacion` y `estado` no cuentan como cambio (research §5); un cambio de `fecha_fin` sí.
  - **04:** las 300 asignaturas, con sus mallas aplicadas antes; un cambio de `horas_semanales_clase` de `3` a `3.0` no es cambio.
  - **05:** las 165 ofertas; las bajas solo en los periodos del archivo; `franjas_horarias` no cuenta.

  Crear `TraductorPeriodos`, `TraductorAsignaturas` y `TraductorOferta`.
- [X] T011 [P] [US2] Los matriculados. Primero las pruebas:
  - `MatriculaTest` (sin dobles): un total de -1 y uno de 100 000 no pasan; 0 y 99 999 sí; una fuente de 61 caracteres no pasa;
  - `RepositorioMatriculasJdbcIntegrationTest` y la prueba del esquema (fallan sin V6), con sus referencias a periodo y carrera;
  - `TraductorMatriculasTest`: los 7 de la carga 10, la clave periodo + carrera y las bajas solo en sus periodos.

  Crear `ACA/main/resources/db/migration/academico/V6__matriculados.sql` (que quita el `check` de `accion` de `registro_cambio` y lo vuelve a crear con `MATRICULA_REGISTRADA` y `MATRICULA_CORREGIDA`, como V3 a V5), `Matricula`, `RepositorioMatriculas`, su JDBC, el caso de uso que registra y corrige, y `TraductorMatriculas`.
- [X] T012 [US2] Ampliar `AplicadorAcademicoIntegrationTest`: confirmar de la 01 a la 05 y la 10 en orden deja todo aplicado. Ampliar `AplicadorAcademico` con las 4 cargas. T010 a T012 pasan.

---

## Phase 5: US1 y US2 · Incremento C, Docentes 06, 08 y 09 (Priority: P1)

- [ ] T013 [P] [US1] Primero las pruebas, en `DOC/test/java/ec/mdd/docentes/aplicacion/cargas/TraductoresDocentesTest.java`:
  - **06:** los 47 docentes, con su ficha académica; las columnas que la ficha no guarda no cuentan (la tarea las enumera en el JavaDoc del traductor, desde `DocentesDePrueba`); un cambio de dedicación cuenta; `estado` (ACTIVO, LICENCIA, INACTIVO) se compara y se aplica con `cambiarEstado` de `MantenerDocentes`.
  - **08:** los 21 cargos, por su clave compuesta.
  - **09:** los 11 proyectos, con sus participantes.

  Crear `TraductorDocentes`, `TraductorCargos` y `TraductorProyectos` desde `DocentesDePrueba` y `CargasDePrueba`, que pasan a reusarlos.
- [ ] T014 [US2] Primero las pruebas, en `DOC/integrationTest/…/AplicadorDocentesIntegrationTest.java`:
  - confirmar la 06, la 08 y la 09 sobre las referencias de Académico de prueba las deja aplicadas;
  - un segundo coordinador vigente de la misma carrera (RN-12) rechaza la fila con `CoordinadorVigente` y no aplica nada;
  - un docente de medio tiempo con un cargo rechaza con `DocenteNoHabilitado`.

  Crear `AplicadorDocentes` y su bean, y sumar `project(':plataforma:lenguaje-cargas')`. T013 y T014 pasan.

---

## Phase 6: US1 y US2 · Incremento D, Normativa 07 (Priority: P1)

- [ ] T015 [US2] Primero las pruebas, en `NOR/test/…/TraductorPoliticaTest.java` y `NOR/integrationTest/…/AplicadorNormativaIntegrationTest.java`:
  - la carga 07 de prueba contra la versión 1 da 1 que cambia (la fila 22, con `horas_otras_docencia` y `horas_gestion` de vacío a 0; research §7);
  - con la versión 1 sin periodos, confirmar la edita;
  - con un periodo asociado, se rechaza con `VersionEnUso` y no cambia nada;
  - un código de versión nuevo crea la versión copiando la última, con sus reglas, y le pone las filas;
  - un archivo con una fila de menos se rechaza con el motivo de Normativa;
  - `tipo_regla`, `referencia_normativa` y `observacion` no cuentan como cambio.

  Crear `TraductorPolitica`, `AplicadorNormativa` y su bean, y sumar `project(':plataforma:lenguaje-cargas')`.

**Checkpoint**: cada una de las 10 cargas tiene su dueño; Cargas arranca.

---

## Phase 7: Las 10 en orden, cancelar y confirmar dos veces (US2, US3, US4)

- [ ] T016 [US2] [US4] Primero la prueba, en `backend/apps/datos/src/integrationTest/java/ec/mdd/ConfirmarLasDiezCargasIntegrationTest.java` (Testcontainers, con el contexto del proceso `datos`):
  - **SC-001:** las 10 cargas de prueba, validadas y confirmadas en su orden sobre una base vacía, quedan aplicadas.
  - **SC-002:** subidas otra vez, cada una da 0 nuevos, 0 que cambian y 0 bajas.
  - **Repetida (US4):** confirmar de nuevo la 02 con el mismo hash da `REPETIDA`, sin registros nuevos en Académico.
  - **Cancelar (US3):** subir sin confirmar no cambia nada: se cuentan las tablas antes y después.
  - **Historial (US5):** tiene 10 `APLICADA` y 1 `REPETIDA`.

  Debe pasar.
- [ ] T017 La medición de SC-004: en la misma prueba, o en un `@Tag("rendimiento")`, unos 11 600 docentes ficticios (5 MB) dan su vista previa en menos de 30 s y se confirman en menos de 60 s. La meta del plan es 20 s. Anotar los tiempos. Si no se cumple, agregar en `RepositorioDocentes` un camino por lotes que aplique las mismas reglas, y medir otra vez.

---

## Phase 8: Pantalla (US1 a US5)

- [ ] T018 [US1] Primero las pruebas, en `frontend/src/pantallas/plantillas/Plantillas.test.tsx` (ampliar):
  - la vista previa, con los 4 totales en un `role="status"`, las pestañas «Nuevos», «Cambian» y «Se dan de baja» con sus listas (en «Cambian», los campos con el valor de ahora y el del archivo) y el aviso de las bajas;
  - con errores, «Confirmar» deshabilitado con su motivo;
  - «Cancelar» limpia todo sin llamar al servidor.

  Ampliar la pantalla y sus consultas.
- [ ] T019 [US2] [US4] [US5] Primero las pruebas, en la misma prueba de la pantalla:
  - «Confirmar» vuelve a enviar el archivo con la huella;
  - el resultado «aplicada» con sus totales y «ya estaba confirmada el …»;
  - los tres 409 con su título y sus detalles `fila N · campo`;
  - «Historial» de una carga abre su lista, de la más reciente a la más antigua, con el resultado en español.

  Ampliar la pantalla y crear las consultas de `confirmarCarga` y `listarHistorial`.
- [ ] T020 Los mensajes en «tú» (research §10):
  - en Cargas, los motivos de `ReglasDeColumna`, `CondicionDeObligatoriedad`, `ClaveDeCarga`, `Cedula` y `ValidadorDeFilas`, con la referencia que no existe diciendo «regístrala o confirma antes su carga» (FR-014);
  - en la SPA, «Corrígelo y vuelve a subirlo»;
  - actualizar las pruebas de F01-H03 que los nombran;
  - comprobar con `grep` que no queda ningún «Escriba», «complételo», «regístrela», «Corríjalo» o «vuelva a» en Cargas ni en la pantalla, y anotarlo.
- [ ] T021 Prueba con Playwright, en `frontend/e2e/plantillas.spec.ts` (ampliar):
  - la vista previa de un archivo;
  - «Confirmar» con la huella en el pedido;
  - el resultado y el historial;
  - axe-core sin fallas WCAG 2.1 AA con la vista previa y con el historial a la vista.

**Checkpoint**: las 5 historias pasan.

---

## Phase 9: Punta a punta y cierre

- [ ] T022 Crear `deploy/probar_confirmar_carga.sh`, con el estilo de `probar_cargos_proyectos.sh`:
  - **Antes de empezar:** comprueba que ningún código ni cédula de las 10 cargas de prueba existe en la base; si alguno existe, falla sin tocar nada.
  - **Los pasos:**
    - confirma las 10 en orden, con `datos.demo`;
    - las sube otra vez y ve 0 nuevos, 0 que cambian y 0 bajas;
    - confirma la 02 otra vez y ve `REPETIDA`;
    - sube la 01 con una unidad menos y ve 1 baja que no se aplica;
    - cambia a mano una carrera entre la vista previa y la confirmación y ve `CARGAS_VISTA_PREVIA_DESACTUALIZADA`;
    - una regla del dueño: `CARGAS_CARGA_RECHAZADA` con su fila, sin cambios;
    - el historial;
    - `consolidacion.demo` confirma y `docente.demo` recibe 403.
  - **La limpieza:** borra en la base todo lo creado, con sus registros, la copia de los periodos en Roles y los eventos.

  Agregar su paso a `.github/workflows/perfil-consolidado.yml`. Correrlo dos veces seguidas y anotarlo; shellcheck v0.11.0 sin observaciones.
- [ ] T023 Correr todo, como en las historias anteriores:
  - backend con `integrationTest`, y la SPA;
  - los 15 scripts con el perfil reconstruido;
  - `probar_equipo.sh`;
  - shellcheck y Gitleaks sobre un clon.

  Buscar un valor de prueba en `docker compose logs gateway datos` y anotar que no aparece (principio IX). Comprobar los criterios de terminado de la sección 6 de la constitución 2.2.0. Hacer commit y push, abrir el PR en borrador con `Refs #30` y marcarlo listo después de sus runs.
- [ ] T024 Completar la evidencia y el PR. Fusionar con *squash* con la aprobación del autor. Después:
  1. esperar el despliegue («Sano en N s»);
  2. el autor confirma en orden, con `datos.demo`, las cargas de su equipo: la 01 con su sede matriz, la 02 y la 04;
  3. revisa una vista previa con un cambio, una repetida y el historial;
  4. abrir el PR de evidencia con `Closes #30` y borrar las ramas.

---

## Dependencies & Execution Order

- **Setup:** T001 libre.
- **Foundational:** T002 y T003 primero; T004 y T005 en cualquier orden; T006 después de T004 y T005; T007 después de T006.
- **Incremento A:** T008 después de T004; T009 después de T008 y T007.
- **Incremento B:** T010 y T011 después de T009; T012 después de los dos.
- **Incremento C:** T013 después de T004; T014 después de T013 y T007.
- **Incremento D:** T015 después de T004 y T007.
- **Las 10:** T016 después de T012, T014 y T015; T017 después de T016.
- **Pantalla:** T018 y T019 después de T003; T020 en cualquier momento después de T003; T021 después de T019.
- **Cierre:** T022 después de T016; T023 y T024 al final.

### Parallel Opportunities

- T004 y T005; T010 y T011; T013, T015 y el incremento B, una vez cerrada la base.
- La SPA (T018 a T020) puede avanzar con la API simulada desde T003.
- Como trabajas solo, [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (fase 2 e incremento A)

1. **El motor y Cargas,** con dueños de prueba.
2. **Académico 01 y 02.**
3. **Validar:** en el perfil, confirmar la 01 hace válida la 02. Es lo que hoy falla en el equipo.

### Incremental

1. **Académico B, Docentes y Normativa:** cada incremento con su prueba de integración.
2. **Las 10 en orden y la medición.**
3. **La pantalla y los mensajes en «tú».**
4. **Cierre:** punta a punta, PR, fusión y despliegue.

---

## Notes

- Las bajas no se aplican (aclaración 2); el orden obligatorio, el «sin proyectos» y el mapeo de etiquetas son de F01-H05; el tablero es de F02-H02.
- Hacer commit por incremento en `025-confirmar-carga`, con `Refs #30`.
