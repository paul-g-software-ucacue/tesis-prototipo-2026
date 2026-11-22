---
description: "Tareas de F19-H01: generar el CSV para SIGA con el layout configurable"
---

# Tasks: Generar el CSV para SIGA con el layout configurable

**Input**: documentos de diseño en `specs/060-csv-siga/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas. En cada tarea, la prueba compila y se ve fallar antes del código que la hace pasar; si no compila, primero va un esqueleto.

**Rutas**:
- `REP/` es `backend/servicios/reporting/src/`;
- `GEN/` es `backend/servicios/generacion/src/`;
- `DOC/` es `backend/servicios/docentes/src/`;
- `SPA/` es `frontend/src/`;
- `.../` es el paquete `ec/mdd/reporting/`, `ec/mdd/generacion/` o `ec/mdd/docentes/`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede hacer en paralelo (otro archivo, sin depender de una tarea pendiente).
- **[Story]**:
  - **US1:** la Secretaría genera el CSV para SIGA, con sus filas y sus totales;
  - **US2:** el CSV sigue las convenciones de SENESCYT;
  - **US3:** cambiar el layout sin tocar el código.

---

## Phase 1: Setup

- [X] T001 Escribir **D-119** en `docs/decisiones.md`, en la fila del índice y en su sección, con research §1 a §14:
  - la ruta interna `consultarContenidoCongelado`, una llamada para todos los docentes;
  - el tipo de identificación en el contenido congelado, tomado de `DocenteActivo`; sin el dato, «NA»;
  - la comparación de los totales con el `ReporteGeneral` de la misma publicación, con `REPORTING_CONTENIDO_INCONSISTENTE`;
  - el formato fijo: RFC 4180 con coma, CRLF, UTF-8 sin BOM, mayúsculas con `Locale.ROOT`, `NA`, horas con escala 2 y punto, la fecha en `America/Guayaquil`, y las fórmulas neutralizadas;
  - el orden de las filas;
  - el puerto de salida `ExportadorSiga` y su adaptador `ExportadorSigaCsv`: el dominio solo arma y comprueba las filas (`FilasParaSiga`), y el adaptador escribe el archivo con el layout y el formato; una API de SIGA sería otro adaptador del mismo puerto (principio I);
  - el archivo de mapeo YAML: la plantilla es el layout número 0; los 15 campos; las 3 columnas obligatorias; la traducción solo en `tipo_identificacion`, `dedicacion` y `componente`, como ajuste a FR-014; las reglas y los límites (256 KB, 4 niveles, 30 columnas, 50 problemas);
  - la tabla `reporting.layout_exportacion`, sin `update` ni `delete`; rige la última;
  - el permiso `PUBLICACION:APROBACION` en las cuatro operaciones;
  - las acciones `CSV_SIGA_GENERADO` y `LAYOUT_SIGA_CARGADO`, y el renombre de `RegistroDeDescargas` a `RegistroEnBitacora`;
  - la pantalla «Exportación a SIGA»;
  - los tres términos nuevos del glosario: «CSV para SIGA», «versión del layout de exportación» y «tipo de identificación» (T003);
  - los pasos R9 y R10;
  - la SPA valida solo la presencia y el tamaño del archivo de mapeo; su contenido lo valida el backend, como en las cargas y en F12-H01 (principio XIV).

  Relacionar con RF-52, RNF-COM-02, RNF-SEG-04, ADR-002, ADR-007, D-04, D-16, D-113, D-115 y D-117.
- [X] T002 Confirmar tres cosas y, si algo falta, pedírselo al autor antes de seguir:
  - que la rama es `060-csv-siga`, creada desde `main` actualizado;
  - que `.specify/feature.json` apunta a `specs/060-csv-siga`;
  - que el issue #266 tiene cargadas sus «Horas estimadas» en el proyecto (principio X y definición de listo del issue). **Hecho:** el proyecto no tiene ese campo; el autor confirmó 24 h el 2026-11-22 (D-119).
- [X] T003 Principio III: los términos nuevos entran al glosario antes del código. En `~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/sdd/modelo_dominio.md` y en su `modelo_dominio.docx`:
  - **§7**, tres filas nuevas:
    - «CSV para SIGA»: «El archivo de la versión publicada con el layout de exportación vigente, una fila por docente y actividad. Con su suma de verificación y su registro es la Exportación a SIGA.» (`FilasParaSiga`, `ExportadorSiga`; Publicación y consulta; RF-52, ADR-002);
    - «Versión del layout de exportación»: «Número correlativo de cada layout cargado; 0 es el layout por defecto. No se edita ni se borra.» (`LayoutExportacion.numero`; Publicación y consulta; RF-52, ADR-007);
    - «Tipo de identificación»: «Cédula o pasaporte del docente, guardado al congelar.» (`TipoDeIdentificacion`; Datos maestros, Distributivo, Publicación y consulta; diccionario de datos, carga 06);
  - **§8**, en la fila «"Versión" sin más», «Se dice» suma «o versión del layout de exportación».

  Estado:
  - [x] `modelo_dominio.md`, actualizado al cerrar el análisis (2026-11-22), con copia en `modelo_dominio.md.antes-F19-H01`;
  - [x] `modelo_dominio.docx`, con los mismos cambios (2026-11-22), con copia en `modelo_dominio.docx.antes-F19-H01`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Propósito:** los contratos, el tipo de identificación en el contenido congelado, la ruta interna nueva, y el layout con su lector y su almacenamiento. Ninguna historia empieza antes.

### Contratos (principio IV)

- [X] T004 [P] Ampliar `contratos/reporting/openapi.yaml` según `contracts/reporting.md`.
  - **Las cuatro operaciones**, con el tag `Siga`, `x-mdd-permiso: [PUBLICACION:APROBACION]`, 401 y 403:
    - `generarCsvSiga` (`GET /api/reporting/periodos/{periodo}/csv-siga`): 200 `text/csv` binario, con `ContentDisposition` `siga-{periodo}-v{version}.csv`; 400, 409 `SinPublicacion`, 500 `ErrorDelReporte`, 502 y 504;
    - `consultarLayoutsSiga` (`GET /api/reporting/layout-siga`);
    - `descargarPlantillaLayoutSiga` (`GET /api/reporting/layout-siga/plantilla.yaml`), con `text/yaml`;
    - `cargarLayoutSiga` (`PUT /api/reporting/layout-siga`): `multipart/form-data` con `archivo` obligatorio; 200, 400 y 413.
  - **Los esquemas nuevos**, con `additionalProperties: false`:
    - `LayoutsSiga`: `vigente` y `anteriores`;
    - `LayoutSiga`: `numero` (`minimum: 0`), `porDefecto`, `cargadoPor` y `cargadoEn` opcionales, `sha256` (`^[0-9a-f]{64}$`) y `columnas` (`minItems: 1`, `maxItems: 30`);
    - `ColumnaDelLayoutSiga`: `nombre` (`^[A-Za-z][A-Za-z0-9_]{0,63}$`), `campo` y `traduccion` opcional, como arreglo de `{codigo, valor}`;
    - `CampoDelLayoutSiga`: un `enum` con las 15 claves de research §6.
  - **Las descripciones** resumen el formato del archivo (research §4) y la bitácora: `CSV_SIGA_GENERADO` antes de responder, y `LAYOUT_SIGA_CARGADO`.
- [X] T005 [P] Ampliar `contratos/generacion/openapi.yaml` según `contracts/interno.md`:
  - la operación `consultarContenidoCongelado`, `GET /interno/generacion/periodos/{periodo}/versiones/{numero}/contenido`, tag `Interno`, con los parámetros de `consultarCargasCongeladas` y las respuestas 200, 400, 401, 403, 404 y 500;
  - el esquema `ContenidoCongeladoCompleto` (`hash` y `cargas`, un arreglo de `CargaCongeladaDelDocente`, por nombre y luego por identificación);
  - el esquema `TipoDeIdentificacion` (`enum: [CEDULA, PASAPORTE]`);
  - `DatosDelDocenteCongelado.tipoIdentificacion`, opcional: «Falta si el docente ya no estaba activo al congelar, o si el contenido se guardó antes de F19-H01».
- [X] T006 [P] En `contratos/docentes/openapi.yaml`, sumar `tipoIdentificacion` (`enum: [CEDULA, PASAPORTE]`) a `DocenteActivo`, también en `required`.
- [X] T007 [P] En `contratos/comun/errores.md`, según `contracts/reporting.md`:
  - **los tres códigos nuevos,** `REPORTING_LAYOUT_INVALIDO` (400), `REPORTING_ARCHIVO_ILEGIBLE` (400) y `REPORTING_ARCHIVO_DEMASIADO_GRANDE` (413), cada uno con su título, mensaje, causa, clasificación (Validación), acción y descripción técnica;
  - **los dos que se amplían:** `REPORTING_SIN_PUBLICACION` suma «generar el CSV para SIGA», y `REPORTING_CONTENIDO_INCONSISTENTE` suma los totales que no cuadran con el reporte general, con `FilasParaSiga.de`.

### Docentes y Generación: el tipo de identificación

- [X] T008 Primero, ampliar `InternoDocentesContratoTest` (`DOC/test/java/.../adaptadores/entrada/`) y `RepositorioDocentesJdbcIntegrationTest` (`DOC/integrationTest/`) y verlos fallar: cada activo trae `tipoIdentificacion`. Después, sumarlo a la consulta de los activos y al mapeo de `InternoDocentesControlador` (`DOC/main/java/.../adaptadores/entrada/`), a partir de `Identificacion.tipo()`.
- [X] T009 Generación es dominio central: TDD estricto. Primero, los casos de `ContenidoCongeladoTest` (`GEN/test/java/.../dominio/`): el tipo de un docente activo pasa a `CargaCongelada.tipoIdentificacionAlCongelar`, y un docente que no está activo queda sin tipo. Después:
  - crear `TipoDeIdentificacion` (`CEDULA`, `PASAPORTE`) en `GEN/main/java/.../dominio/`;
  - sumar `tipoIdentificacion` («obligatorio») a `DocenteActivo`;
  - sumar `tipoIdentificacionAlCongelar` («opcional, igual que la dedicación») a `CargaCongelada`, con la consulta `tipoIdentificacion()` como `Optional`;
  - tomarlo del docente activo en `ContenidoCongelado.de`;
  - ajustar los usos que construyen `DocenteActivo` y `CargaCongelada`.
- [X] T010 Primero, en `GEN/test/`, la prueba del cliente `DatosDesdeProcesoDatos` (`GEN/main/java/.../adaptadores/salida/`): lee `tipoIdentificacion` de `/interno/docentes/activos`, y una respuesta sin él es un 502 (`ProcesoConError`, principio XIV). Después, mapearlo.
- [X] T011 Primero, ampliar `ContenidoCongeladoIntegrationTest` (`GEN/integrationTest/`) en PostgreSQL real y verlo fallar. Los casos:
  - guardar escribe la clave `tipoIdentificacion` en `carga_congelada.contenido`, y leer la devuelve;
  - un contenido sin la clave da el tipo vacío;
  - el método nuevo `cargas(periodo, numero)` devuelve todas las `CargaCongelada` de la versión, por nombre y luego por identificación, iguales a las de `carga(periodo, numero, docente)`.

  Después, implementarlo en `RepositorioContenidoCongelado` (`GEN/main/java/.../puertos/salida/`) y en `RepositorioContenidoCongeladoJdbc`. Sin migración.
- [X] T012 Primero, ampliar `InternoGeneracionContratoTest` y verlo fallar:
  - `consultarContenidoCongelado` con varias cargas;
  - 404 si la versión no existe o no está congelada;
  - `tipoIdentificacion` presente y ausente en `DatosDelDocenteCongelado`.

  Después, en `InternoGeneracionControlador` (`GEN/main/java/.../adaptadores/entrada/`), la operación nueva, con la misma comprobación de versión congelada y el mismo cálculo tardío de `consultarCargaCongelada` (por el servicio que ya usan las otras dos rutas), y el campo en el mapeo de `DatosDelDocenteCongelado`.

### Reporting: lo que comparten las tres historias

- [X] T013 [P] Primero, `CeldaCsvTest` (`REP/test/java/.../dominio/`), con cada carácter peligroso (`=`, `+`, `-`, `@`, tabulación, CR), el vacío y uno normal. Después, extraer la neutralización de `EscritorCsv.comoFormula` a `CeldaCsv.neutralizada(valor)` en `REP/main/java/.../dominio/`, y hacer que `EscritorCsv` la use. `EscritorCsvTest` no cambia y sigue en verde: los bytes de los reportes no cambian.
- [X] T014 Primero, los casos en `ClientesDeReportingTest` (`REP/test/java/.../adaptadores/salida/`):
  - `cargas(periodo, version)` lee `ContenidoCongeladoCompleto`;
  - `tipoIdentificacion` presente y ausente;
  - una respuesta sin `hash` o sin `cargas` da 502;
  - un 404 da vacío.

  Después, en `REP/main/java/.../`:
  - `TipoDeIdentificacion` (`CEDULA`, `PASAPORTE`) en el dominio;
  - `DatosDelDocente` suma `tipoIdentificacionAlCongelar`, opcional, con `tipoIdentificacion()` como `Optional`;
  - `CargasDeLaVersion(String hash, List<ContenidoDelDocente> cargas)`;
  - `ContenidoDeLaVersion.cargas(String periodo, int version)`, que devuelve `Optional<CargasDeLaVersion>`, con su JavaDoc;
  - la implementación en `ContenidoDesdeGeneracion`, reutilizando el mapeo `contenido(Carga)`.
- [X] T015 [P] Renombrar el puerto `RegistroDeDescargas` a `RegistroEnBitacora` (`REP/main/java/.../puertos/salida/`) y el adaptador `RegistroDeDescargasEventos` a `RegistroEnBitacoraEventos`. Renombrar también sus pruebas (`RegistroEnBitacoraEventosTest` y, en `backend/apps/salida/src/integrationTest/`, `RegistroEnBitacoraIntegrationTest`) y ajustar `DescargarReporteServicio` y `ConfiguracionReporting`. Es un cambio mecánico: todas las pruebas de F20-H01 y F20-H02 siguen en verde sin otro cambio.
- [X] T016 Primero, `ColumnaDelLayoutTest` y `LayoutExportacionTest` (`REP/test/java/.../dominio/`), una prueba por cada regla de research §6. Los casos:
  - cada problema dice su columna (`columnas[3]` o `columnas[3] (HORAS)`) y su regla;
  - un archivo con varios problemas los da todos;
  - con más de 50, da 50 y «Hay N problemas más.»;
  - los límites: 1 y 30 columnas valen y 31 no; un nombre de 64 caracteres vale y uno de 65 no;
  - los nombres repetidos sin distinguir mayúsculas;
  - falta cada una de las obligatorias (`identificacion`, `componente`, `horas`);
  - un campo que no existe;
  - una traducción en un campo no traducible;
  - una traducción incompleta y una con un código de más;
  - un valor `NA`, un valor repetido y uno fuera de `^[A-Za-z0-9][A-Za-z0-9 ._-]{0,29}$`.

  Después, en `REP/main/java/.../dominio/`:
  - **`CampoDelLayout`:** los 15 campos, cada uno con `clave()`, `traducible()`, `codigos()` y `obligatorio()`. `TIPO_IDENTIFICACION` → `CEDULA`, `PASAPORTE`; `DEDICACION` → `TC`, `MT`, `TP`; `COMPONENTE` → los cinco de `Componente`; obligatorios: `IDENTIFICACION`, `COMPONENTE` y `HORAS`.
  - **`ColumnaDelLayout`:** `nombre` («`^[A-Za-z][A-Za-z0-9_]{0,63}$`»), `campo` y `traduccion`, un `Map` ordenado («Vacío o, si `campo.traducible()`, con exactamente las claves de `campo.codigos()`»).
  - **`BorradorDeLayout`, `ProblemaDelLayout(String columna, String regla)`, `ValidacionDelLayout`, `LayoutInvalido` y `ArchivoIlegible`.**
  - **`LayoutExportacion`:** `numero` («0 para el layout por defecto; desde 1, el de cada carga»), `columnas` («De 1 a 30, en el orden del archivo; nombres sin repetirse (sin distinguir mayúsculas); al menos una de `IDENTIFICACION`, una de `COMPONENTE` y una de `HORAS`»), `texto`, `sha256`, y `cargadoPor` y `cargadoEn` opcionales. Las fábricas son `validar(BorradorDeLayout)`, `porDefecto(...)` y `cargado(...)`, con `esPorDefecto()` y `encabezado()`.

  Nunca lanza al primer problema.
- [X] T017 Primero, `LectorDeLayoutYamlTest` (`REP/test/java/.../adaptadores/salida/`). Los casos:
  - un ancla, un alias o una etiqueta explícita lanzan `ArchivoIlegible`;
  - UTF-8 inválido lanza `ArchivoIlegible`;
  - el BOM se quita;
  - más de 4 niveles lanza `ArchivoIlegible`;
  - una clave desconocida en la raíz o en una columna, o un nodo del tipo equivocado, quedan como problema del borrador, no como excepción;
  - una clave repetida;
  - el mensaje nunca incluye el texto del lector, solo «Revisa la línea N.».

  Después:
  - sumar `implementation 'org.yaml:snakeyaml'` en `backend/servicios/reporting/build.gradle`, con un comentario («F19-H01, D-119: el archivo de mapeo; solo en los adaptadores»);
  - el puerto `LectorDeLayout` (`REP/main/java/.../puertos/salida/`): `BorradorDeLayout leer(byte[] archivo)`;
  - `LectorDeLayoutYaml`, con `LoaderOptions` sin alias ni claves repetidas, profundidad 4 y tope de 256 KB. Primero recorre los eventos y después arma el árbol de nodos, como `LectorYaml` de Validación OCS, sin usar sus clases (principio V);
  - en `ArquitecturaTest`, que `org.yaml` solo se use en `adaptadores`.
- [X] T018 Escribir `REP/main/resources/reporting/layout-siga-por-defecto.yaml`. Es la plantilla, comentada en español:
  - qué es cada clave;
  - la lista de los 15 campos y cuáles admiten traducción;
  - las reglas de los nombres;
  - que el orden es la posición;
  - un ejemplo de traducción comentado.

  Tiene las 13 columnas de FR-004, en ese orden: `PERIODO`, `TIPO_IDENTIFICACION`, `NUMERO_IDENTIFICACION`, `APELLIDOS_NOMBRES`, `DEDICACION`, `COMPONENTE`, `CODIGO_ACTIVIDAD`, `CODIGO_CARRERA`, `CODIGO_ASIGNATURA`, `PARALELO`, `HORAS_SEMANALES`, `VERSION` y `FECHA_PUBLICACION`, sin traducciones.

  Sumar a `LayoutExportacionTest` un caso que lee el recurso con `LectorDeLayoutYaml` y lo valida sin problemas: es el mismo camino que un archivo subido (research §6).
- [X] T019 Primero, `LayoutsGuardadosJdbcIntegrationTest`, en PostgreSQL real, junto a `ReportingEsquemaIntegrationTest`. Los casos:
  - `guardar` da 1 y después 2;
  - `vigente()` es el último;
  - `todos()` va del más reciente al más antiguo;
  - sin filas, `vigente()` está vacío;
  - `update` y `delete` fallan;
  - `porDefecto()` es el texto del recurso.

  Después:
  - **La migración** `REP/main/resources/db/migration/reporting/V2__layout_exportacion.sql`, con la tabla `layout_exportacion`:
    - `numero integer primary key check (numero >= 1)`;
    - `texto text not null check (octet_length(texto) <= 262144)`;
    - `sha256 char(64) not null check (sha256 ~ '^[0-9a-f]{64}$')`;
    - `cargado_por varchar(100) not null`;
    - `cargado_en timestamptz not null`;
    - el disparador que rechaza `update` y `delete`, como el de `carga_congelada`.

    Con un comentario que diga por qué (principio VII).
  - **El puerto** `LayoutsGuardados` (`REP/main/java/.../puertos/salida/`): `Optional<LayoutGuardado> vigente()`, `List<LayoutGuardado> todos()`, `int guardar(String texto, String sha256, String quien, Instant momento)` y `String porDefecto()`. El número es `coalesce(max(numero), 0) + 1`; si choca con otro, reintenta una vez.
  - **El adaptador** `LayoutsGuardadosJdbc`, con `JdbcClient`, y sus beans en `ConfiguracionReporting`.

**Checkpoint:** los contratos están escritos; el contenido congelado trae el tipo y la ruta nueva responde; el layout por defecto se lee y se valida; los reportes de F20 siguen en verde.

---

## Phase 3: User Story 1 - La Secretaría genera el CSV para SIGA del distributivo publicado (Priority: P1) 🎯 MVP

**Goal:** `secretaria.demo` genera el CSV de la versión publicada vigente con el layout vigente. Tiene una fila por docente y actividad, y sus totales son los del reporte general; queda registrado en la bitácora antes de entregarse.

**Independent Test:** spec US1. Con 2026-I publicado y el layout por defecto, el CSV tiene una fila por asignatura y paralelo y por actividad, y sus sumas por docente y componente son las del reporte general (R9, sin las convenciones).

- [X] T020 [US1] Primero, `FilaSigaTest` y `FilasParaSigaTest` (`REP/test/java/.../dominio/`), sin dobles, con los docentes de prueba de `DocentesDePrueba`. Los casos:
  - un docente con clase en dos paralelos y una actividad de investigación da tres filas, y la de investigación no tiene asignatura ni paralelo (escenario 2; el `NA` lo escribe el adaptador, T027);
  - el orden de research §5: por docente (con el comparador del reporte general), por componente, por código y luego por carrera, paralelo, proyecto y horas;
  - un docente con solo asignaturas o solo actividades;
  - dos asignaciones de la misma actividad dan dos filas;
  - totales iguales;
  - una suma de un docente en un componente distinta lanza `ContenidoInconsistente`;
  - un docente que falta o que sobra lanza `ContenidoInconsistente`;
  - un total institucional distinto lanza `ContenidoInconsistente`;
  - un hash de las cargas o de una carga distinto del de la publicación lanza `ContenidoInconsistente`;
  - un docente repetido lanza `ContenidoInconsistente`;
  - decimales sin redondear;
  - `ColumnaDelLayout.valor(FilaSiga)` da el valor tipado de cada campo (`Optional<String>`, `BigDecimal` o `LocalDate`), traducido si hay traducción y vacío si no aplica.

  Después, en `REP/main/java/.../dominio/`:
  - `FilaSiga`, con sus campos y reglas (data-model.md) y sus dos fábricas;
  - `FilasParaSiga.de(publicacion, CargasDeLaVersion, ReporteGeneral)`, sin layout ni bytes;
  - `ColumnaDelLayout.valor(FilaSiga)`.
- [X] T021 [US1] Principio I: la exportación a SIGA va por un puerto. Primero, `ExportadorSigaCsvTest` (`REP/test/java/.../adaptadores/salida/`), sin dobles. Los casos:
  - la estructura del archivo: el encabezado con los nombres del layout, separadas por coma, CRLF también al final, sin BOM, y comillas solo cuando el valor tiene `,`, `"`, CR o LF, con las internas duplicadas;
  - un layout con otro orden y sin una columna opcional;
  - dos archivos de las mismas filas y el mismo layout son idénticos byte a byte;
  - el nombre `siga-{periodo}-v{version}.csv`.

  Después:
  - el puerto de salida `ExportadorSiga` (`REP/main/java/.../puertos/salida/`): `ArchivoSiga exportar(FilasParaSiga filas, LayoutExportacion layout)`. Su JavaDoc dice que es el puerto «Exportación a SIGA» de la tabla del principio I, y que una API de SIGA sería otro adaptador;
  - el adaptador `ExportadorSigaCsv` (`REP/main/java/.../adaptadores/salida/`), con su bean. Las convenciones de SENESCYT llegan con US2 (T027); hasta entonces, lo que no aplica sale vacío.
- [X] T022 [US1] Primero, `GenerarCsvSigaServicioTest` (`REP/test/java/.../aplicacion/`), con Mockito en los puertos. Los casos:
  - lee la publicación vigente y el layout vigente una sola vez, y pide el listado y el contenido de la misma versión;
  - sin publicación del periodo lanza `SinPublicacion` y no registra nada;
  - sin contenido lanza `ContenidoInconsistente`;
  - pasa las filas y el layout a `ExportadorSiga`, simulado con Mockito;
  - el registro `CsvGenerado` va dentro de `Transaccion` y antes de devolver el archivo; si falla, no hay archivo; si la exportación falla, no hay registro;
  - durante la reforma usa la versión publicada vigente (D-113);
  - sin layout cargado, usa el por defecto (número 0);
  - 300 docentes generan en menos de 10 s (SC-008), con `ExportadorSigaCsv` real. Salen de los 47 de `06_docentes.csv` de los datos de prueba, repetidos con una identificación derivada en `010999xxxx`, como en F20-H02.

  Después:
  - el objeto de valor `CsvGenerado(usuario, momento, periodo, version, layout)` en el dominio;
  - el puerto de entrada `GenerarCsvSiga`, con `ArchivoSiga generar(String periodo, String quien)` y su JavaDoc;
  - `GenerarCsvSigaServicio` en `REP/main/java/.../aplicacion/`: arma el `ReporteGeneral` con `ReporteGeneral.de` a partir de la misma publicación (research §3), las `FilasParaSiga` y el archivo con `ExportadorSiga`;
  - sus beans en `ConfiguracionReporting`.
- [X] T023 [US1] Primero, en `RegistroEnBitacoraEventosTest` y en `RegistroEnBitacoraIntegrationTest`, la entrada `CSV_SIGA_GENERADO`, con el objeto `{periodo, version}` y `nuevo` `{layout}`. No lleva nombres, identificaciones ni horas. Después, `registrar(CsvGenerado)` en `RegistroEnBitacora` y en `RegistroEnBitacoraEventos`.
- [X] T024 [US1] Primero, `SigaContratoTest` (`REP/test/java/.../adaptadores/entrada/`) para `generarCsvSiga`, con swagger-request-validator. Los casos:
  - 200 con `text/csv; charset=UTF-8`, `Content-Disposition` y `Cache-Control: no-store`;
  - 400 con un periodo inválido;
  - 409 `REPORTING_SIN_PUBLICACION`;
  - 500 `REPORTING_CONTENIDO_INCONSISTENTE`, sin trazas ni hashes en el cuerpo;
  - 502 y 504;
  - 403 con `REPORTES:LECTURA` y sin `PUBLICACION:APROBACION`.

  Después, `SigaControlador` en `REP/main/java/.../adaptadores/entrada/`, que implementa la interfaz generada, con `@PreAuthorize("hasAuthority('PERMISO_PUBLICACION:APROBACION')")` y quién lo pide como `ReportesControlador.quien()`.
- [X] T025 [P] [US1] Regenerar `SPA/api/generado/reporting.d.ts` (`npm run generar:api`). Primero, en `SPA/pantallas/siga/Siga.test.tsx`, los casos de la sección «CSV para SIGA» de `contracts/spa.md`:
  - cargando, sin publicaciones y error;
  - el periodo propuesto;
  - el texto de la versión y el de la reforma;
  - el texto del layout, por defecto y cargado;
  - el botón deshabilitado con «Generando…»;
  - la descarga con el nombre de `Content-Disposition`;
  - el mensaje en `aria-live`;
  - el 409 en `role="alert"`.

  Después:
  - `SPA/pantallas/siga/consultas.ts` y `Siga.tsx`, con esa sección;
  - la ruta `exportacion-siga` en `SPA/rutas.tsx`;
  - el enlace «Exportación a SIGA» después de «Reportes» en `SPA/navegacion/Marco.tsx`, con `{ funcionalidad: 'PUBLICACION', tipo: 'APROBACION' }`;
  - los casos del menú en `enlaces.test.ts` y `Marco.test.tsx`: la ve `SECRETARIA_GENERAL`; no la ven Rector, Vicerrector, Administrador ni coordinación.
- [X] T026 [US1] En `deploy/probar_documento_firmable.sh` (`contracts/demostracion.md`):
  - **El encabezado:** sumar al comentario de la cabecera F19-H01 (D-119, `specs/060-csv-siga/contracts/demostracion.md`).
  - **R1 ampliado:** el CSV para SIGA en EN_OCS da 409 y no hay `CSV_SIGA_GENERADO`.
  - **R9 nuevo**, después de R8, con la función `csv_siga_igual_al_general` (Python, módulo `csv` y `Decimal`):
    - `Content-Disposition` `siga-2026-I-v1.csv`, `no-store`, sin BOM y CRLF;
    - el encabezado, el del layout vigente que da `consultarLayoutsSiga`;
    - una fila por cada asignatura y actividad de los reportes individuales (SC-001);
    - las sumas por docente y componente, y por componente, iguales al general (SC-002);
    - dos archivos idénticos (SC-006);
    - 2 entradas `CSV_SIGA_GENERADO` con `{periodo: 2026-I, version: 1}` y el número de la versión del layout de exportación vigente que da `consultarLayoutsSiga`.
  - **R4 ampliado:** `rectorado.demo`, `coordinacion.demo` y `admin.demo` reciben 403 en el CSV.
  - **R5 ampliado:** durante la reforma, el CSV es idéntico al de R9.
  - **La línea `verificar`** de R9.

**Checkpoint:** US1 funciona sola: el CSV sale con sus filas y sus totales, y queda en la bitácora. R1, R4, R5 y R9 (sin las convenciones) en verde.

---

## Phase 4: User Story 2 - El CSV sigue las convenciones de SENESCYT (Priority: P1)

**Goal:** cada celda cumple las convenciones de carga masiva: códigos, mayúsculas, `NA`, la cédula de 10 dígitos, las horas, la fecha y las fórmulas neutralizadas.

**Independent Test:** spec US2. El CSV de los datos de prueba pasa un validador de esquema con las convenciones y la RFC 4180 (SC-003); un nombre que empieza con `=` sale neutralizado (SC-007).

- [X] T027 [US2] Primero, ampliar `ExportadorSigaCsvTest` con las convenciones de research §4 y verlo fallar:
  - los catálogos como códigos (`TC`, `CLASE`, `CEDULA`), nunca la etiqueta;
  - mayúsculas con `Locale.ROOT` que conservan tildes y eñes («Peña Álvarez José» → «PEÑA ÁLVAREZ JOSÉ»), también en los códigos («140-b» → «140-B») y en las traducciones;
  - `NA` en lo que no aplica (la asignatura y el paralelo de una actividad; la actividad y el proyecto de una clase) y en un dato vacío (sin dedicación, sin tipo de identificación);
  - una cédula que empieza con 0 conserva sus 10 dígitos, y un pasaporte sale tal cual;
  - las horas con escala 2 y `toPlainString()` («8.00», «2.50», sin miles);
  - la fecha de publicación como `aaaa-mm-dd` en `America/Guayaquil`, también cerca de la medianoche UTC;
  - un nombre con coma o con comillas va entre comillas y con las internas duplicadas;
  - una celda que empieza con `=`, `+`, `-`, `@`, tabulación o CR lleva `'` al inicio (`CeldaCsv.neutralizada`);
  - un validador de esquema en la prueba (expresiones por columna del layout por defecto) que recorre el CSV de los datos de prueba con el lector de la RFC 4180 y pasa en el 100 % de las filas (SC-003).

  Después, aplicarlas en `ExportadorSigaCsv` (`REP/main/java/.../adaptadores/salida/`), sin que el layout las cambie (FR-011). El dominio no cambia.
- [X] T028 [US2] En R9 de `deploy/probar_documento_firmable.sh`, comprobar las convenciones (SC-003):
  - las cédulas `^[0-9]{10}$`;
  - todo el texto en mayúsculas;
  - `NA` en la asignatura de cada fila de actividad;
  - las horas `^[0-9]+\.[0-9]{2}$`;
  - la fecha `^[0-9]{4}-[0-9]{2}-[0-9]{2}$`.

  Actualizar su línea `verificar`.

**Checkpoint:** US1 y US2 completas: el archivo se puede entregar. R9 completo en verde.

---

## Phase 5: User Story 3 - Cambiar el layout sin tocar código (Priority: P2)

**Goal:** la Secretaría descarga la plantilla, carga un archivo de mapeo validado y ve el vigente y los anteriores; el próximo CSV sale con las columnas nuevas.

**Independent Test:** spec US3. Con dos nombres cambiados, dos columnas invertidas, una opcional quitada y la traducción del componente, el CSV sale con las columnas nuevas y los mismos totales (SC-004). Un archivo inválido se rechaza con la columna y la regla, y el vigente no cambia (SC-005).

- [X] T029 [US3] Primero, `GestionarLayoutSigaServicioTest` (`REP/test/java/.../aplicacion/`), con Mockito en los puertos. Los casos:
  - `consultar()` sin cargas da el por defecto como vigente (número 0) y ningún anterior;
  - con cargas, da el último como vigente y los demás, del más reciente al más antiguo, sin el por defecto;
  - `plantilla()` es el texto del recurso;
  - `cargar` válido guarda y registra `LayoutCargado` en la misma `Transaccion`;
  - si el registro falla, no queda la fila;
  - `cargar` inválido lanza `LayoutInvalido` sin guardar ni registrar;
  - `ArchivoIlegible` se propaga sin guardar ni registrar;
  - el `sha256` es del texto sin BOM.

  Después:
  - el objeto de valor `LayoutCargado(usuario, momento, layout, columnas, sha256)`;
  - el puerto de entrada `GestionarLayoutSiga`, con `LayoutsSiga consultar()`, `String plantilla()` y `LayoutExportacion cargar(byte[] archivo, String quien)`, y su JavaDoc;
  - `GestionarLayoutSigaServicio`, que vuelve a validar cada `LayoutGuardado` al leerlo (data-model.md);
  - sus beans.
- [X] T030 [US3] Primero, en `RegistroEnBitacoraEventosTest` y en `RegistroEnBitacoraIntegrationTest`, la entrada `LAYOUT_SIGA_CARGADO`, con el objeto `{layout}` y `nuevo` `{columnas, sha256}`. Después, `registrar(LayoutCargado)`.
- [X] T031 [US3] Primero, en `SigaContratoTest`:
  - `consultarLayoutsSiga` con y sin cargas;
  - `descargarPlantillaLayoutSiga` con `text/yaml` y `Content-Disposition` `layout-siga-plantilla.yaml`;
  - `cargarLayoutSiga`: 200; 400 `REPORTING_LAYOUT_INVALIDO` con un detalle por problema (`campo` `columnas[N]` o `archivo`); 400 `REPORTING_ARCHIVO_ILEGIBLE` sin el texto del lector; 400 `COMUN_SOLICITUD_INVALIDA` sin `archivo`; 413 `REPORTING_ARCHIVO_DEMASIADO_GRANDE` con 256 KB + 1 byte;
  - 403 en las tres con `REPORTES:LECTURA` y sin `PUBLICACION:APROBACION`;
  - la prueba del catálogo con los tres códigos nuevos.

  Después:
  - las tres operaciones en `SigaControlador`. Si el archivo pasa de 262 144 bytes, lanza `ArchivoDemasiadoGrande` (error de entrada del adaptador, data-model.md) antes de leerlo;
  - los tres códigos en `ErroresReporting`, el único que traduce (principio XIII): `LayoutInvalido` → 400 `REPORTING_LAYOUT_INVALIDO` con `detalles`; `ArchivoIlegible` → 400 `REPORTING_ARCHIVO_ILEGIBLE`, con el mensaje del lector solo en el log; `ArchivoDemasiadoGrande` → 413 `REPORTING_ARCHIVO_DEMASIADO_GRANDE`.
- [X] T032 [P] [US3] Primero, en `SPA/pantallas/siga/Siga.test.tsx`, los casos de la sección «Layout de exportación» de `contracts/spa.md`:
  - la tabla del vigente con su `caption`, y la traducción «CLASE → 01, …» o «—»;
  - «Descargar plantilla»;
  - la validación antes de enviar: sin archivo, o con uno de más de 256 KB, se marca el campo y no se envía;
  - la carga válida: el mensaje, la tabla actualizada y las consultas invalidadas;
  - la rechazada: «El archivo tiene N problemas. El layout vigente no cambió.», la lista de detalles en `role="alert"` y el foco en su título;
  - las versiones anteriores, expandidas con `aria-expanded` y vacías.

  Después, la sección en `Siga.tsx` y en `consultas.ts`.
- [X] T033 [US3] En `deploy/probar_documento_firmable.sh`:
  - **Al empezar:** cargar la plantilla por defecto como una versión más del layout de exportación, para que R9 no dependa de una corrida anterior. Con esa carga, R9 comprueba además que el encabezado son las 13 columnas de FR-004, en su orden.
  - **R10 nuevo**, con la función `cargar_layout`:
    - la plantilla es igual al recurso;
    - un layout válido (dos nombres cambiados, dos columnas invertidas, sin `fecha_publicacion` y `componente` traducido a `01`…`05`) da 200 con el número siguiente y `LAYOUT_SIGA_CARGADO`;
    - uno inválido (un campo inexistente y dos nombres repetidos) da 400 `REPORTING_LAYOUT_INVALIDO` con 2 detalles, y el vigente no cambia;
    - el CSV sale con las columnas nuevas y los mismos totales;
    - al terminar, vuelve a cargar la plantilla.
  - **R4 ampliado:** los tres usuarios reciben 403 también al cargar un layout, sin layout nuevo.
  - **La línea `verificar`** de R10.
- [X] T034 [US3] En `frontend/e2e/siga.spec.ts` (Playwright):
  - `secretaria.demo` genera el CSV y carga un layout válido y uno rechazado;
  - axe-core corre en los tres estados (con publicaciones, con el layout rechazado y con versiones anteriores), sin fallas WCAG 2.1 AA (SC-010).

**Checkpoint:** las tres historias funcionan; R1, R4, R5, R9 y R10 en verde.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T035 Correr el build completo (`./gradlew build integrationTest`), la SPA (`npm run tipos`, `lint`, `formato:verificar`, `test`) y Playwright. Comprobar:
  - la cobertura de Reporting, Generación y Docentes (mínimo 60 %, D-32);
  - Spotless, SpotBugs, ArchUnit y Spring Modulith en verde;
  - Gitleaks sobre `main..HEAD`;
  - `ApplicationModules.verify()` y ArchUnit confirman que Reporting no depende de clases de Generación ni de Aprobación (FR-022);
  - el script completo contra el perfil local, R1 a R10.
- [X] T036 [P] Revisar el JavaDoc (principio XII) en `FilasParaSiga`, `ExportadorSiga`, `ExportadorSigaCsv`, `LayoutExportacion`, `CampoDelLayout`, `LectorDeLayoutYaml`, `GenerarCsvSiga`, `GestionarLayoutSiga`, `RegistroEnBitacora` y `CargaCongelada`:
  - por qué el CSV no lleva BOM ni el momento de generación;
  - por qué los totales se comparan con el reporte general de la misma publicación;
  - por qué solo se traducen los catálogos cerrados;
  - por qué el layout no se edita ni se borra;
  - por qué el tipo de identificación es opcional en el contenido congelado.
- [X] T037 [P] Medir SC-008 y anotarlo en la evidencia:
  - el tiempo de T022 con 300 docentes;
  - en el perfil local, el tiempo de punta a punta del CSV (`curl -w '%{time_total}'`);
  - el tiempo de la Secretaría desde que abre la pantalla hasta tener el archivo (menos de 1 minuto).

  Si no se cumple, anotarlo como desviación con su causa.
- [X] T038 El recorrido del autor, `quickstart.md` §1 a §3, con el teclado y un lector de pantalla:
  - abrir el CSV en un editor de texto y en un lector de la RFC 4180;
  - comparar los totales con el reporte general en la pantalla;
  - abrir el CSV en LibreOffice Calc y confirmar que la celda con `=` no se evalúa (SC-007).

  **Hecho por Claude** el 2026-11-22 en el perfil real (Playwright y LibreOffice Calc sin interfaz; detalle en la evidencia), salvo el lector de pantalla, que queda para el autor.
- [X] T039 Crear `specs/060-csv-siga/evidencia.md`, como la de 059: las pruebas, el script, el recorrido, las mediciones, las desviaciones y los criterios de terminado. La sección del equipo de demostración queda para después de fusionar.
- [ ] T040 Fusionar solo con la aprobación del autor y todas las comprobaciones en verde.
  - **Verificar la demo en solo lectura:** «Sano» y, si 2026-I está publicado, un CSV para SIGA generado. Antes, regenerar la base si el contenido es anterior a la historia (Riesgos del plan).
  - El PR principal lleva `Refs #266`, y el de evidencia, `Closes #266`. F19 (#258) sigue abierta hasta F19-H02 (#267).

---

## Dependencies & Execution Order

- **Setup:** T001, T002 y T003 antes de todo (T003, el glosario, antes de cualquier código: principio III).
- **Fundacional:**
  - T004, T005, T006 y T007 en paralelo;
  - T008 después de T006;
  - T009 después de T005;
  - T010 después de T006 y T009;
  - T011 después de T009;
  - T012 después de T011;
  - T013 y T015 en cualquier momento después de T002;
  - T014 después de T005 y T012;
  - T016 después de T002 (dominio puro);
  - T017 después de T016;
  - T018 después de T017;
  - T019 después de T018.
- **US1:**
  - T020 después de T014 y T016;
  - T021 después de T016 y T020;
  - T022 después de T019 y T021;
  - T023 después de T015 y T022;
  - T024 después de T004, T007 y T023;
  - T025 después de T004 (en paralelo con el backend);
  - T026 después de T024 y T025.
- **US2:**
  - T027 después de T021 y T013;
  - T028 después de T026 y T027.
- **US3:**
  - T029 después de T019 y T023;
  - T030 después de T029;
  - T031 después de T024 y T030;
  - T032 después de T025;
  - T033 después de T026 y T031;
  - T034 después de T032 y T033.
- **Cierre:**
  - T035 después de las tres historias;
  - T036 y T037 en paralelo con T035;
  - T038 después de T035;
  - T039 después de T035, T037 y T038;
  - T040 al final.

**Entre las historias:**
- US2 refina el CSV de US1: depende de `ExportadorSigaCsv` (T021), pero se prueba por separado con su validador de esquema. El dominio no cambia en US2.
- US3 usa el layout y su almacenamiento de la fundación, y el controlador de US1. Se prueba sola con R10.

### Parallel Opportunities

```text
T004, T005, T006 y T007 (contratos).
T013 (CeldaCsv), T015 (renombre) y T016 → T019 (el layout) en paralelo con la cadena T008 → T012 de Docentes y Generación. T014 (el cliente de Generación) va al final de esa cadena, después de T012.
T025 (SPA, US1) en paralelo con T020 → T024, una vez hecho T004.
T027 (US2) en paralelo con T022 → T024, una vez hecho T021 (el adaptador).
T032 (SPA, US3) en paralelo con T029 → T031.
T036 y T037 en paralelo con T035.
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. La fundación, T001 a T019: el tipo en el contenido congelado, la ruta nueva y el layout por defecto. Los reportes de F20 siguen en verde.
2. US1, T020 a T026: la Secretaría ya genera el CSV con sus filas y sus totales.
3. Validar con `SigaContratoTest` y los pasos R1, R4, R5 y R9.

### Incremental Delivery

1. La fundación.
2. US1: el CSV, sus totales y la bitácora.
3. US2: las convenciones de SENESCYT. Con esto, el archivo ya se puede entregar a SIGA.
4. US3: el layout configurable, con R10.
5. El cierre: el build, las mediciones, el recorrido, la evidencia y la fusión.

Una sola rama (`060-csv-siga`) y un solo PR, como en las historias anteriores.
