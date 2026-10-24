---

description: "Tareas de F01-H02: subir un archivo y validar su estructura"
---

# Tasks: Subir un archivo y validar su estructura

**Input**: documentos de diseño en `specs/019-subir-archivo-carga/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/cargas.md](contracts/cargas.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**: D-16, D-63, D-65, D-70, **D-71**, ADR-016 y ADR-021 en [`docs/decisiones.md`](../../docs/decisiones.md); RF-06, RF-07, RF-55 y RF-57 de la ERS; el glosario §7 («Carga», «Plantilla de carga», «Diccionario de datos»).

**Organization**:
- **Fundacional:** la plataforma de errores (detalles y 413), D-71 en Roles, el contrato y el dominio.
- **US1:** subir un archivo válido, en CSV o XLSX.
- **US2:** rechazar un archivo que no se puede leer.
- **US3:** subir desde la pantalla.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1 a US3.
- Rutas relativas a la raíz. Abreviaturas: `CAR` = `backend/servicios/cargas/src`, `JCAR` = `CAR/main/java/ec/mdd/cargas`, `PCAR` = `CAR/test/java/ec/mdd/cargas`, `ROL` = `backend/servicios/roles/src` y `ERR` = `backend/plataforma/errores` (con `ERRN` = `backend/plataforma/errores-nucleo`).

## Convenciones para todas las tareas

- **Capas (ADR-012):** dominio sin Spring ni POI; aplicación sin infraestructura; POI solo en `JCAR/adaptadores/salida`. `ArquitecturaTest` lo comprueba.
- **Principio VI:** las columnas esperadas salen del diccionario de datos; nunca se repiten en el código.
- **Principio IX:** el archivo no se guarda; ni sus filas ni sus valores van a los registros del proceso ni a los detalles de error; el nombre del archivo se sanea (sin ruta, hasta 255).
- **Datos de prueba:** los 10 CSV de `MDD_artefactos_desarrollo/requerimientos_mdd/cargas/datos_prueba/`, sintéticos (RNF-SEG-06), copiados a `CAR/test/resources/cargas/datos-de-prueba/`.
- **Pruebas:** Mockito solo en la aplicación; contrato con swagger-request-validator; permisos con `VerificadorDePermisos`; 60 % de líneas por módulo.
- **Idioma, formato y commits:** como en las historias anteriores; la evidencia va en `specs/019-subir-archivo-carga/evidencia.md` y los commits llevan `Refs #28`.

---

## Phase 1: Setup

- [X] T001 Crear `specs/019-subir-archivo-carga/evidencia.md` con estas secciones: entorno; base (plataforma, D-71, contrato y dominio); «Archivo válido» (US1); «Archivo que no se puede leer» (US2); pantalla (US3); cierre; equipo de demostración; notas. Agregar `versionApachePoi=5.5.1` a `backend/gradle.properties`, con el comentario de research §5 (la 6.0.0 tiene 4 días), y `implementation "org.apache.poi:poi-ooxml:${versionApachePoi}"` a `backend/servicios/cargas/build.gradle`; `./gradlew :servicios:cargas:dependencies` muestra el árbol, que se anota en la evidencia.

---

## Phase 2: Foundational (bloquea a todas las historias)

### Plataforma de errores (research §3 y §6)

- [X] T002 Prueba primero, en `ERR/src/test/java/…/ManejadorDeErroresTest.java` (ampliar) y en las pruebas de `ERRN`: una entrada `EntradaCatalogo.conDetalles(excepcion, definicion, error -> detalles)` responde su estado y su código con los detalles que calcula la función; una entrada sin detalles sigue igual; un `MaxUploadSizeExceededException` responde 413 `COMUN_CONTENIDO_DEMASIADO_GRANDE` (hoy, `COMUN_SOLICITUD_INVALIDA`). Agregar `conDetalles` a `EntradaCatalogo`, el uso en `ManejadorDeErrores`, `CodigosComunes.CONTENIDO_DEMASIADO_GRANDE` y su caso 413 en `segunEstado`, y la fila en `contratos/comun/errores.md`. T002 pasa; las pruebas de errores de todos los servicios siguen en verde.

### D-71 en Roles (data-model.md)

- [X] T003 Prueba primero, en `ROL/integrationTest/java/ec/mdd/roles/MatrizD16IntegrationTest.java` (ampliar): `RESPONSABLE_CONSOLIDACION` y `VICERRECTOR` tienen `CARGAS:LECTURA` y `CARGAS:ESCRITURA` con alcance `TODO`; los demás roles, sin cambios. Tiene que fallar sin la migración. Crear `ROL/main/resources/db/migration/roles/V7__cargas_para_quien_inicia.sql` como `V5__vicerrector_escribe_normativa.sql`, con un comentario que cite D-71 y D-63. Ajustar, si fijan los permisos de esos dos roles, `AutorizacionServiciosTest` (Roles) y `AutorizacionEnMemoria` (Usuarios). T003 pasa.

### Contrato (principio IV, research §7)

- [X] T004 Cambiar `contratos/cargas/openapi.yaml` según contracts/cargas.md: el tag `Subida`, los esquemas `FormatoDeArchivo` y `LecturaDeArchivo` (`archivo` hasta 255, `registros` ≥ 1), la operación `validarArchivo` (`POST /api/cargas/plantillas/{codigo}/archivo`, `multipart/form-data` con la parte `archivo` en `format: binary`, `x-mdd-permiso: CARGAS:ESCRITURA`) con sus respuestas 200, 400, 404 y 413; el comentario de cabecera cita F01-H02, RF-06, RF-07, RF-57 y D-71. Agregar los 7 códigos `CARGAS_*` a `contratos/comun/errores.md` con los textos de contracts/cargas.md. Regenerar (`openApiGenerate`, `generar:api`); `verificar:ruptura` en verde.

### Dominio (research §2, data-model.md)

- [X] T005 Prueba primero, en `PCAR/dominio/TablaDeDatosTest.java` y `PCAR/dominio/EncabezadosTest.java`, sin dobles: `registros()` no cuenta las filas vacías del final y sí una vacía en medio; `Carga.compararEncabezados` con los de la plantilla da una lista vacía, también con espacios en los extremos; sin `codigo_carrera` da `FALTA` de `codigo_carrera`; con una columna de más, `SOBRA`; con una repetida, `REPETIDA`; con el mismo conjunto en otro orden, `FUERA_DE_LUGAR` de la primera fuera de lugar; `Carga.leer` lanza `EncabezadosDistintos` con las diferencias, `ArchivoSinRegistros` sin registros, y devuelve el número de registros. Crear en `JCAR/dominio/` `TablaDeDatos`, `MotivoDeDiferencia`, `DiferenciaDeEncabezado`, `EncabezadosDistintos` y `ArchivoSinRegistros`, y los métodos en `Carga`, con JavaDoc que cite RF-07 y RF-57. T005 pasa.

**Checkpoint**: la plataforma, D-71, el contrato y el dominio están listos.

---

## Phase 3: User Story 1 - Subir un archivo válido (Priority: P1) 🎯 MVP

**Goal**: leer el CSV o el XLSX de una carga y responder cuántos registros tiene (FR-001, FR-002, FR-004, FR-006, FR-007, FR-010, FR-011; SC-001, SC-002, SC-004).

**Independent Test**: subir los archivos de prueba de las 10 cargas, en CSV y en XLSX; cada uno responde su número de registros.

- [X] T006 [P] [US1] Copiar los 10 CSV de los datos de prueba a `CAR/test/resources/cargas/datos-de-prueba/`, con su origen en la evidencia. Prueba primero, en `PCAR/adaptadores/salida/LectorCsvTest.java`: cada uno de los 10 se lee con sus encabezados y su número de filas (24, 7, 2, 300, 165, 47, 22, 21, 11 y 7); el mismo con `;`, con CRLF o LF, y con el BOM, da lo mismo; un valor entre comillas con coma, comillas duplicadas y salto de línea queda en una celda; una línea vacía y una de solo `;;;` entre registros se leen como filas vacías del ancho del encabezado (se cuentan); al final, no se cuentan. Crear en `JCAR/puertos/salida/` `LectorDeArchivo.java`, `Formato.java` y `ArchivoLeido.java` (record con el formato y la `TablaDeDatos`), y `JCAR/adaptadores/salida/LectorCsv.java`. T006 pasa.
- [X] T007 [P] [US1] Prueba primero, en `PCAR/adaptadores/salida/LectorXlsxTest.java`: cada CSV de prueba convertido a un XLSX de una hoja con POI dentro de la prueba se lee igual que el CSV; una celda con formato de texto «0101010101» conserva el 0; una celda de fecha sale como la muestra la hoja; las filas vacías del final no cuentan; una fila con sus últimas celdas vacías se lee con el ancho del encabezado. Crear `JCAR/adaptadores/salida/LectorXlsx.java` (POI, `DataFormatter`). T007 pasa.
- [X] T008 [US1] Prueba primero, en `PCAR/adaptadores/salida/LectorPorFormatoTest.java`: `.csv` y `.CSV` van al lector CSV; `.xlsx`, al XLSX. Crear `JCAR/adaptadores/salida/LectorPorFormato.java`, que implementa `LectorDeArchivo`. T008 pasa.
- [X] T009 [US1] Prueba primero, en `PCAR/aplicacion/ValidarArchivoServicioTest.java` (Mockito sobre `LectorDeArchivo`, diccionario real): un archivo válido responde la carga, el nombre saneado (sin ruta, hasta 255), el formato y los registros; un código que no es de una carga lanza `CargaNoEncontrada` sin leer el archivo; más de 5 MB lanza `ArchivoDemasiadoGrande` sin leerlo; las excepciones del lector y del dominio pasan tal cual. Crear `JCAR/puertos/entrada/ValidarArchivo.java`, `ArchivoSubido`, `ArchivoDemasiadoGrande` y `JCAR/aplicacion/ValidarArchivoServicio.java`; registrar los beans en `ConfiguracionCargas`. T009 pasa.
- [ ] T010 [US1] Prueba primero, en `PCAR/adaptadores/entrada/SubidaContratoTest.java` (con los lectores y el diccionario reales): el CSV de docentes da 200 con `registros: 47`, `formato: CSV`, `carga: docentes` y `archivo: 06_docentes.csv`, y la respuesta valida contra el contrato; el mismo en XLSX, 200 con 47; sin la parte `archivo`, 400 `COMUN_SOLICITUD_INVALIDA`; una carga que no existe, 404. Crear `JCAR/adaptadores/entrada/SubidaControlador.java` (`hasAuthority('PERMISO_CARGAS:ESCRITURA')`) y `JCAR/adaptadores/entrada/ErroresCargas.java` con su prueba `ErroresCargasTest` contra `errores.md`. Anotar en la evidencia el tiempo de `validarArchivo` con `04_asignaturas_malla.csv` (300 filas), frente a la meta de 1 s del plan. T010 pasa.
- [ ] T011 [US1] Ampliar `PCAR/adaptadores/entrada/PermisosCargasTest.java`: `validarArchivo` exige `CARGAS:ESCRITURA` (sin él, 403; con `CARGAS:LECTURA` o `DISTRIBUTIVO:LECTURA`, también 403); las operaciones de las plantillas siguen igual. Agregar `spring.servlet.multipart.max-file-size: 10MB` y `max-request-size: 10MB` a `backend/apps/datos/src/main/resources/application.yaml`. Agregar `client_max_body_size 10m;` en `location /api/` de `frontend/nginx.conf`, con un comentario que cite F01-H02. T011 pasa.

**Checkpoint**: un archivo válido se lee por la API, en CSV y en XLSX.

---

## Phase 4: User Story 2 - Rechazar un archivo que no se puede leer (Priority: P1)

**Goal**: cada archivo que no se puede leer, rechazado con su código y su motivo (FR-002, FR-003, FR-005, FR-006, FR-007, FR-008, FR-009; SC-003).

- [ ] T012 [US2] Prueba primero, en `LectorPorFormatoTest`, `LectorCsvTest` y `LectorXlsxTest`: un `.pdf` y un archivo sin extensión dan `FormatoNoAdmitido`; un `.xlsx` que es un XLS antiguo (firma OLE2) o un texto, y un `.csv` que empieza con `%PDF`, `PK` o la firma OLE2, o tiene un byte nulo, también; un CSV con «Peñafiel» en Windows-1252 da `CodificacionNoAdmitida`; comillas sin cerrar y una fila con otro número de valores dan `ArchivoIlegible` con su fila, pero no una línea vacía (ver T006); un XLSX con un valor en la columna siguiente a la última de los encabezados da `ArchivoIlegible` con su fila; un XLSX con dos hojas da `VariasHojas`; uno cifrado o dañado, `ArchivoIlegible`. Agregar las excepciones a `JCAR/puertos/salida/` y las reglas a los lectores. T012 pasa.
- [ ] T013 [US2] Prueba primero, en `SubidaContratoTest`: cada caso de research §3 con su estado y su código, validados contra el contrato: 400 `CARGAS_FORMATO_NO_ADMITIDO`, `CARGAS_CODIFICACION_NO_ADMITIDA`, `CARGAS_VARIAS_HOJAS`, `CARGAS_ARCHIVO_ILEGIBLE` (con el detalle `archivo` y la fila), `CARGAS_ENCABEZADOS_DISTINTOS` (un archivo de carreras sin `codigo_carrera` trae el detalle `{campo: codigo_carrera, descripcion: "Falta en el archivo."}`; uno con una columna de más, «No está en la plantilla.») y `CARGAS_ARCHIVO_SIN_REGISTROS`; 413 `CARGAS_ARCHIVO_DEMASIADO_GRANDE` con 5 MB y un byte. Ningún detalle trae un valor del archivo. Con `OutputCaptureExtension`, subir un archivo de docentes con la cédula ficticia `0999999999` y una columna de más: la respuesta es 400 y ni la respuesta ni la salida del proceso contienen `0999999999` (FR-013, principio IX). Ampliar `ErroresCargas` (dos entradas con `conDetalles`) y `ErroresCargasTest`. T013 pasa.

**Checkpoint**: la API acepta y rechaza archivos como pide la spec.

---

## Phase 5: User Story 3 - Subir desde la pantalla (Priority: P1)

**Goal**: «Subir archivo» en cada carga para quien tiene «Cargas: escritura» (FR-012; SC-001).

- [ ] T014 [US3] Prueba primero, en `frontend/src/pantallas/plantillas/Plantillas.test.tsx` (ampliar, con la API simulada): con `CARGAS:ESCRITURA`, cada carga tiene «Subir archivo» (con su nombre accesible por carga); elegir un archivo lo envía como `FormData` en la parte `archivo` a la ruta de su carga, con el token, y muestra «06_docentes.csv: 47 registros leídos»; un 400 muestra el título y sus detalles («codigo_carrera: Falta en el archivo.»); un archivo de más de 5 MB se rechaza en la SPA sin enviarlo; sin `CARGAS:ESCRITURA` (por ejemplo, solo `DISTRIBUTIVO:LECTURA`), no hay «Subir archivo». Ampliar `consultas.ts` y `Plantillas.tsx`. T014 pasa.
- [ ] T015 [US3] Prueba con Playwright, en `frontend/e2e/plantillas.spec.ts` (ampliar): `consolidacion.demo` con `CARGAS:ESCRITURA` sube un CSV a «Docentes contratados» con `setInputFiles` y ve el número de registros; un archivo de carreras sin una columna muestra el motivo; axe-core sin fallas WCAG 2.1 AA con el resultado y con un error a la vista.

**Checkpoint**: las tres historias pasan.

---

## Phase 6: Punta a punta y cierre

- [ ] T016 Crear `deploy/probar_subida.sh`, con el estilo de `probar_plantillas.sh` y los 6 pasos de research §10, incluido un CSV de unos 2 MB por el frontend (`:8081`) que da 200; los archivos se arman en una carpeta temporal (`mktemp -d`) con datos ficticios, el XLSX con `python3 -I` y `zipfile`, y se borra al salir (`trap`). Agregar su paso a `.github/workflows/perfil-consolidado.yml`. Correrlo dos veces y anotarlo; `shellcheck` sin observaciones.
- [ ] T017 Correr todo, como en las historias anteriores: backend, SPA, los 9 scripts con el perfil reconstruido, `probar_equipo.sh` (anotar la memoria frente a los 1 365,4 MiB de F01-H01), `shellcheck` y Gitleaks sobre un clon. Hacer commit y push, abrir el PR en borrador con `Refs #28` y marcarlo listo después de sus runs; si el SCA marca una dependencia de POI, fijarla en `gradle.properties` con su motivo.
- [ ] T018 Completar la evidencia y el PR. Fusionar con *squash* con la aprobación del autor. Después: esperar el despliegue («Sano en N s»), con `roles` en V7; el autor entra con `consolidacion.demo` (vuelve a iniciar sesión para recibir D-71), sube `06_docentes.csv` en menos de 5 s (SC-001) y ve «47 registros leídos», y sube un archivo de carreras sin `codigo_carrera` y ve qué columna falta; abrir el PR de evidencia con `Closes #28` y borrar las ramas.

---

## Dependencies & Execution Order

- **Setup:** T001 libre.
- **Foundational:** T002, T003 y T004 en cualquier orden; T005 después de T004.
- **US1:** T006 y T007 después de T005; T008 después de los dos; T009 después de T008; T010 después de T002 y T009; T011 después de T010.
- **US2:** T012 después de T008; T013 después de T010 y T012.
- **US3:** T014 después de T004; T015 después de T013 y T014.
- **Cierre:** T016 después de T003 y T013; T017 y T018 al final.

### Parallel Opportunities

- La SPA (T014) puede empezar después de T004, con la API simulada.
- T006 y T007 son archivos distintos.
- Como trabajas solo, [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (fase 2 y US1)

1. Los detalles en el catálogo, D-71, el contrato y el dominio.
2. Leer un archivo válido por la API. **Validar:** los 10 archivos de prueba, en CSV y en XLSX, con su número de registros.

### Incremental

1. **US2:** los rechazos.
2. **US3:** la pantalla.
3. **Cierre:** punta a punta, PR, fusión y despliegue.

---

## Notes

- Las reglas de cada fila y el informe por fila son de F01-H03; la vista previa, la confirmación y el historial, de F01-H04.
- La pantalla se sigue llamando «Plantillas de carga»; F01-H05 la convierte en la pantalla de las cargas.
- Hacer commit por tarea o por grupo lógico en `019-subir-archivo-carga`, con `Refs #28`.
