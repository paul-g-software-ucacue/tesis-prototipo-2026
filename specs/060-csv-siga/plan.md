# Implementation Plan: Generar el CSV para SIGA con el layout configurable

**Branch**: `060-csv-siga` | **Date**: 2026-11-22 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/060-csv-siga/spec.md`

## Summary

Es la primera historia de F19 y la tercera de Reporting. Construye sobre lo que dejaron F20-H01 y F20-H02 (D-115, D-117):
- la publicación vigente de Aprobación;
- el contenido congelado por docente en Generación;
- el `ReporteGeneral` con sus totales;
- la neutralización de fórmulas;
- el registro en la bitácora antes de responder.

**De dónde salen las filas** (research §1 y §2):
- Una ruta interna nueva de Generación devuelve el contenido congelado completo de todos los docentes de una versión, en una sola llamada.
- El contenido congelado guarda desde ahora el tipo de identificación. Lo toma de `DocenteActivo` de Docentes y, si falta, sale «NA».

**El CSV** (research §3 a §5):
- **`FilasParaSiga`:** un agregado de lectura de Reporting. Arma una fila por asignatura y paralelo y una por actividad, en un orden total, y comprueba sus sumas contra el `ReporteGeneral` de la misma publicación antes de entregar.
- **El puerto `ExportadorSiga`** (principio I): su adaptador `ExportadorSigaCsv` escribe el archivo con el layout y el formato. Una API de SIGA sería otro adaptador.
- **El formato** es fijo: RFC 4180 con coma, CRLF, UTF-8 sin BOM, mayúsculas, `NA`, horas con punto y dos decimales, y fórmulas neutralizadas.

**El layout** (research §6 a §8):
- **El archivo:** YAML, leído sin alias ni etiquetas. La plantilla comentada es a la vez el layout por defecto (número 0).
- **La validación:** reporta todos los problemas, con la columna y la regla.
- **Lo que se guarda:** cada carga válida en `reporting.layout_exportacion`, la primera tabla de Reporting, sin `update` ni `delete`. Rige la última.
- **La traducción** solo se admite en los catálogos cerrados: tipo de identificación, dedicación y componente.

**La API y la pantalla** (research §9, §11 y §12):
- **La API:** cuatro operaciones con `PUBLICACION:APROBACION`: el CSV, el layout vigente con los anteriores, la plantilla y la carga.
- **La pantalla:** una nueva, «Exportación a SIGA», solo para la Secretaría general académica.

**La bitácora** (§10): `CSV_SIGA_GENERADO` antes de entregar y `LAYOUT_SIGA_CARGADO` en la transacción de la carga. El puerto `RegistroDeDescargas` pasa a ser `RegistroEnBitacora`.

**La demostración** (§13): R9 y R10 nuevos; R1, R4 y R5 ampliados.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash y Python 3 en el script

**Primary Dependencies**: Reporting suma `org.yaml:snakeyaml` (versión del BOM de Spring Boot), igual que Validación OCS, solo en los adaptadores. Ninguna otra.

**Storage**:
- **Reporting:** `V2__layout_exportacion.sql`, una tabla con disparador contra `update` y `delete`.
- **Generación:** sin migración. La clave opcional `tipoIdentificacion` va en el JSON de `carga_congelada.contenido`.
- **La bitácora:** por `eventos_salida`, que ya existe.
- **El CSV** se genera en memoria y no se guarda (su registro como `ExportacionSiga` es de F19-H02).

**Testing**:
- **Dominio de Reporting** (sin dobles, viéndolos fallar primero):
  - `LayoutExportacionTest`: cada regla de FR-015, todos los problemas juntos, el tope de 50, los límites (30 columnas, 64 caracteres) y la plantilla por defecto válida;
  - `ColumnaDelLayoutTest`: el valor tipado de cada campo, la traducción y lo que no aplica (vacío);
  - `FilasParaSigaTest`:
    - las filas de clase y de actividad (escenario 2 de la US1) y el orden;
    - los totales iguales y desiguales, el docente que falta o sobra, y el hash distinto;
  - `ExportadorSigaCsvTest` (adaptador, sin dobles):
    - el formato de research §4: sin BOM, CRLF, comillas solo cuando hacen falta, la cédula con 0, horas `8.00`, la fecha en Guayaquil y las fórmulas;
    - un layout con otro orden y sin una columna opcional;
    - los bytes idénticos;
  - `CeldaCsvTest`;
  - `EscritorCsvTest` sigue igual después de extraer `CeldaCsv`.
- **Aplicación (Mockito en los puertos):**
  - `GenerarCsvSigaServicioTest`:
    - una sola lectura de la publicación y del layout;
    - sin publicación da `SinPublicacion` y no registra nada;
    - el registro va antes de entregar, y si falla no hay archivo;
    - durante la reforma usa la versión vigente;
    - 300 docentes en menos de 10 s (SC-008);
  - `GestionarLayoutSigaServicioTest`: el vigente por defecto y el cargado; una carga válida guarda y registra en una transacción; la inválida no hace ninguna de las dos cosas.
- **Adaptadores:**
  - `LectorDeLayoutYamlTest`: anclas, alias, etiquetas, BOM, UTF-8 inválido, claves desconocidas y profundidad;
  - `LayoutsGuardadosJdbcIntegrationTest` (PostgreSQL real): guardar, el vigente, el orden y que `update` y `delete` fallen;
  - `RegistroEnBitacoraEventosTest`: las dos acciones nuevas y que la entrada no tenga datos personales;
  - `ClientesDeReportingTest`: la ruta nueva, el tipo de identificación, y una respuesta sin `hash` da 502;
  - `RegistroEnBitacoraIntegrationTest` en `apps/salida`: las dos entradas en `eventos_salida`.
- **Generación:**
  - `ContenidoCongeladoTest`: el tipo desde el docente activo, o vacío;
  - `ContenidoCongeladoIntegrationTest`: la clave en el JSON y la ruta nueva igual a la ruta por docente.
- **Docentes:** el tipo en la consulta de los activos (`RepositorioDocentesJdbcIntegrationTest`).
- **Contrato:**
  - `SigaContratoTest`: las cuatro operaciones y sus errores ([contracts/reporting.md](contracts/reporting.md));
  - `InternoGeneracionContratoTest` e `InternoDocentesContratoTest`: lo de [contracts/interno.md](contracts/interno.md);
  - la prueba del catálogo: los tres códigos nuevos.
- **Arquitectura:** `ArquitecturaTest` (SnakeYAML solo en adaptadores) y `ApplicationModules.verify()`.
- **SPA:** Vitest y Playwright con axe-core ([contracts/spa.md](contracts/spa.md)).
- **Punta a punta:** R9 y R10, y R1, R4 y R5 ampliados ([contracts/demostracion.md](contracts/demostracion.md)).

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web:
- Reporting, en el proceso `salida`;
- Generación, en `distributivo`;
- Docentes, en `datos`;
- la SPA y el script.

**Performance Goals**:
- **SC-008:** menos de 10 s con 300 docentes. Se mide en la prueba del servicio, y la evidencia anota el tiempo real.
- **Las llamadas:** el CSV hace tres llamadas internas (la publicación, el listado y el contenido), sin importar cuántos docentes tenga.

**Constraints**:
- **Principio V:** Reporting no lee el esquema de Generación ni usa clases de Validación OCS.
- **Principio VII:** el CSV sale del contenido congelado; el layout no se edita ni se borra; cada CSV y cada carga quedan en la bitácora.
- **RNF-COM-02 y RNF-SEG-04:** el formato y las fórmulas.
- **Principio X:** sin caché ni archivos guardados.

**Scale/Scope**:
- **Reporting:**
  - dominio: 2 agregados, 6 objetos de valor y 2 enumerados nuevos, 4 errores y 1 utilidad; cambian `DatosDelDocente` y `EscritorCsv`;
  - puertos: 2 de entrada y 3 de salida nuevos; 1 de salida ampliado y 1 renombrado;
  - aplicación: 2 servicios nuevos;
  - adaptadores: 1 controlador, el exportador CSV (`ExportadorSigaCsv`), el lector YAML, el repositorio JDBC, el cliente de Generación y el registro;
  - 1 migración y 1 recurso.
- **Generación:** `DocenteActivo`, `CargaCongelada`, `ContenidoCongelado`, el repositorio, el cliente de Docentes y 1 operación interna.
- **Docentes:** la consulta y el mapeo de los activos.
- **Contratos:**
  - `reporting`: 4 operaciones y 4 esquemas;
  - `generacion`: 1 operación y 2 esquemas;
  - `docentes`: 1 campo;
  - `errores.md`: 3 códigos nuevos y 2 ampliados.
- **SPA:** 1 pantalla, 1 enlace y sus pruebas.
- **Script:** 2 pasos nuevos y 3 ampliados.
- **La decisión D-119.**

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | El layout, sus reglas, las filas y los totales son del dominio. La exportación a SIGA es el puerto `ExportadorSiga` que pide la tabla del principio: su adaptador del prototipo, `ExportadorSigaCsv`, escribe el CSV con el layout y el formato; una API de SIGA sería otro adaptador. SnakeYAML, JDBC, HTTP y los eventos quedan en adaptadores, detrás de puertos. | ✅ |
| II. DDD | Reporting es soporte: un agregado de lectura y un archivo de configuración validado, sin lógica de negocio del distributivo. Generación solo guarda un dato más del docente. | ✅ |
| III. Lenguaje ubicuo | «Layout de exportación» (`LayoutExportacion`), «versión publicada», «componente», «carga horaria» y «catálogo de actividades», del glosario. «CSV para SIGA», «versión del layout de exportación» y «tipo de identificación» entran al glosario antes del código (T003). No se usa `ExportacionSiga`, que es de F19-H02. | ✅ |
| IV. API primero | Las operaciones de Reporting, la ruta de Generación y el campo de Docentes van en sus OpenAPI 3.0.3 antes del código, y los tres códigos nuevos van en el catálogo. | ✅ |
| V. Límites | Reporting lee Aprobación y Generación por rutas internas (ADR-023), y Generación lee Docentes por la suya. El lector YAML es de Reporting, sin clases de Validación OCS. La bitácora va por el registro persistente (ADR-018). | ✅ |
| VI. Las reglas son datos | El nombre, el orden y la traducción de cada columna están en el archivo de mapeo, no en el código (ADR-002). Adaptar el CSV es cargar otro archivo (FR-018). | ✅ |
| VII. Inmutabilidad | El CSV sale del contenido congelado de la versión publicada. El layout no admite `update` ni `delete`, y cada carga es una versión. Cada CSV y cada carga quedan en la bitácora, antes de responder. | ✅ |
| VIII. Pruebas | El dominio primero y sin dobles; PostgreSQL real; contrato con los errores; R9 y R10 comparan el CSV con el reporte general. | ✅ |
| IX. Seguridad | `PUBLICACION:APROBACION` en el backend; `no-store`; fórmulas neutralizadas; un YAML sin alias, sin etiquetas y con límites de tamaño y profundidad; la bitácora sin datos personales. Solo datos ficticios. | ✅ |
| X. Simplicidad | Una tabla de texto, sin tablas de columnas. Sin caché ni archivos. Se reutilizan el `ReporteGeneral`, la publicación, el registro y el esquema `CargaCongeladaDelDocente`. La traducción solo en los catálogos cerrados evita dos dependencias. | ✅ |
| XI. Diseño pragmático | No se generaliza el lector YAML ni se crea un «exportador» abstracto: hay un solo formato. El renombre de `RegistroDeDescargas` es el mínimo para que el puerto diga lo que hace. | ✅ |
| XII. JavaDoc | Explica por qué el CSV no lleva BOM ni momento, por qué los totales se comparan con el reporte general y por qué solo se traducen los catálogos cerrados. | ✅ |
| XIII. Errores | Tres códigos nuevos con todos sus campos. Los dos de Reporting amplían la causa. Los mensajes del lector YAML nunca llegan al usuario. | ✅ |
| XIV. Validación | Cada campo del archivo tiene su tipo, sus caracteres y su largo (research §6), en el dominio, con un mensaje que dice qué hacer. La SPA valida el archivo (presencia y tamaño) antes de enviarlo. El contenido de Generación se lee con su esquema. | ✅ |

**Post-diseño:** sin violaciones. Un ajuste a la spec, que se registra en D-119 y no contradice ningún requisito: FR-014 dice «si corresponde, la traducción de los códigos de su catálogo», y en esta historia corresponde solo en los tres catálogos cerrados (research §6). Los códigos de actividad y de carrera salen con el código interno, como dice Assumptions.

La decisión D-119 se registra antes de implementar, con research §1 a §14.

## Project Structure

### Documentation (this feature)

```text
specs/060-csv-siga/
├── spec.md, plan.md, research.md, data-model.md, quickstart.md
├── contracts/ (reporting.md, interno.md, spa.md, demostracion.md)
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/reporting/openapi.yaml        generarCsvSiga, consultarLayoutsSiga,
                                        descargarPlantillaLayoutSiga, cargarLayoutSiga;
                                        LayoutsSiga, LayoutSiga, ColumnaDelLayoutSiga, CampoDelLayoutSiga
contratos/generacion/openapi.yaml       consultarContenidoCongelado; ContenidoCongeladoCompleto,
                                        TipoDeIdentificacion; DatosDelDocenteCongelado.tipoIdentificacion
contratos/docentes/openapi.yaml         DocenteActivo.tipoIdentificacion
contratos/comun/errores.md              REPORTING_LAYOUT_INVALIDO, REPORTING_ARCHIVO_ILEGIBLE,
                                        REPORTING_ARCHIVO_DEMASIADO_GRANDE; 2 ampliados

backend/servicios/reporting/
├── build.gradle        snakeyaml
├── dominio/            LayoutExportacion, ColumnaDelLayout, CampoDelLayout, BorradorDeLayout,
│                       ProblemaDelLayout, LayoutInvalido, ArchivoIlegible, FilaSiga, FilasParaSiga,
│                       CeldaCsv, CargasDeLaVersion, TipoDeIdentificacion, CsvGenerado,
│                       LayoutCargado (nuevos); DatosDelDocente, EscritorCsv (cambian)
├── puertos/entrada/    GenerarCsvSiga, GestionarLayoutSiga (nuevos)
├── puertos/salida/     ExportadorSiga, LayoutsGuardados, LectorDeLayout (nuevos); ContenidoDeLaVersion (cargas);
│                       RegistroDeDescargas → RegistroEnBitacora
├── aplicacion/         GenerarCsvSigaServicio, GestionarLayoutSigaServicio (nuevos);
│                       DescargarReporteServicio (el puerto renombrado)
├── adaptadores/entrada/ SigaControlador (nuevo), ErroresReporting (3 códigos)
├── adaptadores/salida/ ExportadorSigaCsv, LectorDeLayoutYaml, LayoutsGuardadosJdbc (nuevos);
│                       ContenidoDesdeGeneracion (cargas, tipo);
│                       RegistroDeDescargasEventos → RegistroEnBitacoraEventos
├── infraestructura/    ConfiguracionReporting (los beans nuevos)
└── resources/          db/migration/reporting/V2__layout_exportacion.sql,
                        reporting/layout-siga-por-defecto.yaml

backend/servicios/generacion/
├── dominio/            TipoDeIdentificacion (nuevo); DocenteActivo, CargaCongelada,
│                       ContenidoCongelado
├── puertos/salida/     RepositorioContenidoCongelado (cargas)
├── adaptadores/salida/ RepositorioContenidoCongeladoJdbc, el cliente de Docentes
└── adaptadores/entrada/ InternoGeneracionControlador (consultarContenidoCongelado)

backend/servicios/docentes/   la consulta de los activos y su mapeo interno

backend/apps/salida/src/integrationTest/  RegistroEnBitacoraIntegrationTest

frontend/src/pantallas/siga/     Siga.tsx, consultas.ts, Siga.test.tsx
frontend/src/navegacion/         Marco.tsx (enlace) y sus pruebas; el router
frontend/src/api/generado/       reporting.d.ts (regenerado)
frontend/e2e/siga.spec.ts
deploy/probar_documento_firmable.sh   R9, R10; R1, R4 y R5 ampliados
docs/decisiones.md                    D-119
```

**Structure Decision**:
- El CSV y el layout son de Publicación y consulta (Reporting, proceso `salida`), la capa anticorrupción hacia SIGA del mapa de contextos.
- Generación y Docentes solo exponen un dato que ya tienen.
- Aprobación, Firma, Usuarios, Identidad, el API Gateway y el despliegue no cambian. El Gateway ya enruta `/api/reporting/**`.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Un contenido congelado antes de esta historia no tiene el tipo de identificación, y la columna sale `NA` para todos. | Es el comportamiento declarado (research §2). La demostración recrea el distributivo y congela con el tipo, y el quickstart lo pide como requisito. Una base de demo vieja se regenera con el script. |
| Una `salida` nueva contra un `distributivo` o un `datos` viejos: la ruta `/contenido` no existe, o `DocenteActivo` no trae el tipo. | Los procesos salen de la misma versión del repositorio. Si pasara, Reporting responde 502 sin entregar nada, y Generación no congela (lee `DocenteActivo` con su esquema). |
| Renombrar `RegistroDeDescargas` rompe las descargas de los reportes. | El cambio es mecánico. Las pruebas de F20-H01 y F20-H02 (`DescargarReporteServicioTest`, `RegistroDeDescargasIntegrationTest` renombrada, R2 y R8) siguen igual y corren en el mismo PR. |
| Extraer `CeldaCsv` cambia el CSV de los reportes. | `EscritorCsvTest` no cambia y compara bytes. R5 y R8 comparan el CSV con el de antes. |
| El YAML abre una superficie de ataque (alias, etiquetas, tamaño). | Las reglas de F12-H01 §2, con 256 KB y 4 niveles; `LectorDeLayoutYamlTest` las prueba una por una; ZAP al cerrar la ola. |
| El formato de SIGA resulta distinto del layout por defecto. | Es la limitación declarada de ADR-002: se adapta con otro archivo de mapeo. La traducción de actividades y carreras queda como ampliación del mismo archivo (research §6). |
| Mayúsculas con `Locale.ROOT` cambian un código interno («140-b» → «140-B»). | Es la convención de SENESCYT (FR-009). La plantilla lo explica, y una traducción de SIGA puede reemplazarlo cuando se amplíe. |

## Complexity Tracking

Sin violaciones que justificar.
