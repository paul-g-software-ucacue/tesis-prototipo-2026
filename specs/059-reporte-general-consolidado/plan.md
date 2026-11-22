# Implementation Plan: Reporte general consolidado en PDF y CSV

**Branch**: `059-reporte-general-consolidado` | **Date**: 2026-11-21 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/059-reporte-general-consolidado/spec.md`

## Summary

Es la segunda historia de Reporting, sobre lo que dejó F20-H01 (D-115):
- la publicación vigente de Aprobación;
- el contenido congelado por docente en Generación;
- el PDF/A-2b, el CSV;
- la bitácora de cada descarga.

**De dónde salen las horas** (research §1): la ruta interna de Generación que lista los docentes de una versión congelada suma la dedicación y las horas por componente de cada uno. Generación ya las guarda en `carga_congelada.contenido`, así que el cambio no necesita migración ni cálculo nuevo. El reporte general y el individual leen las mismas horas guardadas.

**El reporte** (§2 a §4):
- **`ReporteGeneral`:** un agregado de lectura de Reporting que comprueba el hash, rechaza un docente repetido, ordena las filas y suma los totales institucionales exactos.
- **El PDF/A-2b:** el mismo adaptador, en A4 horizontal, con el encabezado de la tabla repetido y «Total institucional» una sola vez al final.
- **El CSV:** el mismo formato de archivo que el individual, con una fila por docente, una columna por componente y una fila `TOTAL`.

**La API y la pantalla** (§5, §8):
- **La API:** tres operaciones nuevas con `REPORTES:LECTURA`: el reporte general en JSON y sus dos descargas.
- **La pantalla:** «Reportes» suma un grupo «Tipo de reporte», con «General» (propuesto) y «Por docente» (el de F20-H01).

**La bitácora** (§6): la acción `REPORTE_GENERAL_DESCARGADO`, por el mismo registro persistente, antes de responder. Usuarios no cambia.

**La demostración** (§10): R7 y R8 nuevos, y R1, R4, R5 y R6 ampliados.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash

**Primary Dependencies**: ninguna nueva. Reporting ya tiene PDFBox y openhtmltopdf, y veraPDF en las pruebas (D-115).

**Storage**:
- Ninguna tabla ni migración. Generación lee dos claves más de `carga_congelada.contenido` (V10).
- La bitácora pasa por el esquema `eventos_salida`, que ya existe.
- El PDF y el CSV se generan en memoria y no se guardan.

**Testing**:
- **Dominio de Reporting** (sin dobles, viéndolos fallar primero):
  - `ReporteGeneralTest`: el orden, los totales exactos, un componente en 0, los decimales, el hash distinto, el docente repetido y una versión vacía;
  - `HorasPorComponenteTest`: `mas`;
  - `EscritorCsvTest`: el general, con el BOM, `;`, CRLF, las 14 columnas, la fila `TOTAL`, «sin dato», las fórmulas y los bytes idénticos;
  - `DescargaDeReporteTest`: la identificación según el tipo.
- **Aplicación (Mockito en los puertos):**
  - `ConsultarReportesServicioTest`: el general, la reforma y una versión sin contenido;
  - `DescargarReporteServicioTest`: el general se registra antes de responder; sin registro si la generación falla, y sin archivo si el registro falla.
- **Adaptadores:**
  - `GeneradorDeReportePdfaTest`: 300 docentes con veraPDF 2B, sin `AcroForm` ni anotaciones, el encabezado repetido, el pie en cada página, el total una sola vez y el tiempo;
  - `RegistroDeDescargasEventosTest`: la acción y el objeto según el tipo;
  - `ClientesDeReportingTest`: los campos nuevos, y una respuesta sin `horas` da 502;
  - `RegistroDeDescargasIntegrationTest`: la entrada del general en `eventos_salida`;
  - `ContenidoCongeladoIntegrationTest`: las horas y la dedicación en el listado, en PostgreSQL real.
- **Contrato:**
  - `ReportingContratoTest` y `DescargasContratoTest`: las tres operaciones nuevas;
  - `InternoGeneracionContratoTest`: `DocenteCongelado` ampliado.
- **SPA:** Vitest y Playwright con axe-core ([contracts/spa.md](contracts/spa.md)).
- **Punta a punta:** R7, R8 y los pasos ampliados ([contracts/demostracion.md](contracts/demostracion.md)).

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web:
- Reporting, en el proceso `salida`;
- Generación, en `distributivo`;
- la SPA y un script.

**Performance Goals**:
- **SC-007:** cada descarga en menos de 10 s con 300 docentes. Se mide en `GeneradorDeReportePdfaTest`, y la evidencia anota el tiempo.
- **Las llamadas:** el reporte hace dos llamadas internas, sin importar cuántos docentes tenga.

**Constraints**:
- **Principio V:** Reporting no lee el esquema de Generación.
- **Principio VII:** el reporte sale del contenido guardado al congelar, y cada descarga queda registrada antes de responder.
- **RNF-SEG-04:** las fórmulas del CSV.
- **Principio X:** sin caché ni archivos guardados.

**Scale/Scope**:
- **Reporting:**
  - dominio: 1 agregado, 1 objeto de valor y 1 enumerado nuevos; cambian `HorasPorComponente`, `DocentesDeLaVersion`, `DescargaDeReporte` y `EscritorCsv`;
  - puertos: 2 métodos de entrada y 1 de salida;
  - aplicación: los 2 servicios existentes;
  - adaptadores: el generador de PDF, el cliente de Generación, el registro de descargas, y los 2 controladores con 3 operaciones nuevas.
- **Generación:** `DocenteDeLaVersion`, la consulta del repositorio y el mapeo del controlador interno.
- **Contratos:** `reporting` (3 operaciones, 2 esquemas), `generacion` (1 esquema) y `errores.md` (2 descripciones).
- **SPA:** la pantalla «Reportes» y sus pruebas.
- **Script:** 2 pasos nuevos y 4 ampliados.
- **La decisión D-117.**

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | El reporte, sus totales y el CSV son del dominio de Reporting. El PDF/A, el cliente HTTP y el registro de publicaciones quedan en adaptadores, detrás de los puertos que ya existen. | ✅ |
| II. DDD | Reporting es soporte: el agregado de lectura solo suma lo que Generación ya calculó. La carga horaria sigue siendo de Generación. | ✅ |
| III. Lenguaje ubicuo | «Reporte general», «versión publicada», «carga horaria», «componente» y «dedicación», del glosario. «Totales institucionales», como en F13-H02. | ✅ |
| IV. API primero | Las tres operaciones en `contratos/reporting/openapi.yaml` y el esquema ampliado en `contratos/generacion/openapi.yaml`, antes del código. | ✅ |
| V. Límites | Reporting lee Aprobación y Generación por las rutas internas de D-115 (ADR-023). La bitácora va por el registro persistente (ADR-018). | ✅ |
| VI. Las reglas son datos | No hay reglas nuevas: las horas por componente son las de RF-27, ya guardadas. | ✅ |
| VII. Inmutabilidad | El reporte sale del contenido guardado al congelar, que no cambia (R5). Cada descarga queda en la bitácora antes de responder. | ✅ |
| VIII. Pruebas | El dominio primero y sin dobles; veraPDF con 300 docentes; PostgreSQL real; R7 y R8 comparan cada fila con el reporte individual. | ✅ |
| IX. Seguridad | `REPORTES:LECTURA` en el backend; `Cache-Control: no-store`; fórmulas neutralizadas; la entrada de la bitácora sin nombres ni horas. Solo datos ficticios. | ✅ |
| X. Simplicidad | Sin tablas, migraciones, dependencias ni caché. Se reutilizan el adaptador de PDF/A, el escritor de CSV, el registro y la ruta interna. | ✅ |
| XI. Diseño pragmático | Sigue los moldes de F20-H01, sin una abstracción de «reporte» genérica para dos casos. | ✅ |
| XII. JavaDoc | Explica por qué los totales se suman en el dominio, por qué la fila del total no va en `tfoot` en el PDF y por qué el CSV es ancho. | ✅ |
| XIII. Errores | Sin códigos nuevos; los dos de Reporting amplían su descripción en el catálogo. | ✅ |
| XIV. Validación | El periodo se valida con el patrón del contrato. La respuesta interna se lee con su esquema, y una sin `horas` es un 502. | ✅ |

**Post-diseño:** sin violaciones ni ajustes a la spec. FR-015 («junto al individual») se cumple con el grupo «Tipo de reporte» en la misma pantalla (research §8).

La decisión D-117 se registra antes de implementar, con research §1 a §10.

## Project Structure

### Documentation (this feature)

```text
specs/059-reporte-general-consolidado/
├── spec.md, plan.md, research.md, data-model.md, quickstart.md
├── contracts/ (reporting.md, interno.md, spa.md, demostracion.md)
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/reporting/openapi.yaml        consultarReporteGeneral, descargarReporteGeneralPdf,
                                        descargarReporteGeneralCsv; ReporteGeneral, FilaDelDocente
contratos/generacion/openapi.yaml       DocenteCongelado: dedicacion y horas
contratos/comun/errores.md              REPORTING_SIN_PUBLICACION y REPORTING_CONTENIDO_INCONSISTENTE

backend/servicios/reporting/
├── dominio/            ReporteGeneral, FilaDelDocente, TipoDeReporte (nuevos);
│                       HorasPorComponente (mas), DocentesDeLaVersion (dedicación y horas),
│                       DescargaDeReporte (tipo), EscritorCsv (escribir(ReporteGeneral))
├── puertos/entrada/    ConsultarReportes (reporteGeneral), DescargarReporte (descargarGeneral)
├── puertos/salida/     GeneradorDePdf (generar(ReporteGeneral))
├── aplicacion/         ConsultarReportesServicio, DescargarReporteServicio
├── adaptadores/entrada/ ReportesControlador, DescargasControlador, RespuestasDeReporting
└── adaptadores/salida/ GeneradorDeReportePdfa, ContenidoDesdeGeneracion,
                        RegistroDeDescargasEventos

backend/servicios/generacion/
├── dominio/            DocenteDeLaVersion (dedicación y horas)
├── adaptadores/salida/ RepositorioContenidoCongeladoJdbc (docentes)
└── adaptadores/entrada/ InternoGeneracionControlador (DocenteCongelado)

backend/apps/salida/src/integrationTest/  RegistroDeDescargasIntegrationTest

frontend/src/pantallas/reportes/ Reportes.tsx, consultas.ts (+ Reportes.test.tsx)
frontend/src/api/generado/       reporting.d.ts (regenerado)
frontend/e2e/reportes.spec.ts
deploy/probar_documento_firmable.sh   R7, R8; R1, R4, R5 y R6 ampliados
docs/decisiones.md                    D-117
```

**Structure Decision**:
- El reporte general es de Publicación y consulta (Reporting, proceso `salida`), como el individual.
- Generación solo expone dos datos que ya guarda.
- Aprobación, Firma, Usuarios, Identidad, el despliegue y el API Gateway no cambian.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Una `salida` nueva contra un `distributivo` viejo: la respuesta interna no trae `horas`. | Los dos procesos salen de la misma versión del repositorio en todos los despliegues del proyecto. Si pasara, Reporting responde 502 sin entregar nada (`ClientesDeReportingTest`). |
| Los totales del reporte general no coinciden con los totales institucionales del PDF/A del consolidado firmado. Los dos se calculan en el mismo momento, pero por caminos distintos: por docente aquí, y por carrera en el resumen de F13-H02. | El reporte general es la suma de las cargas de los docentes, que es lo que pide RF-46. R7 compara cada fila con el reporte individual. El quickstart §1 sugiere comparar con el PDF/A a mano y anotar una diferencia en la evidencia. Si la hay, se registra como hallazgo, sin cambiar el reporte. |
| 300 docentes vuelven lento el PDF, o parten mal la tabla. | `GeneradorDeReportePdfaTest` genera 300 docentes, mide el tiempo y verifica el encabezado repetido, el pie y el total una sola vez. |
| El cambio de la pantalla rompe las pruebas del individual. | Los casos del individual suman el clic en «Por docente», y siguen comprobando lo mismo. axe-core corre con las dos opciones. |
| `DescargaDeReporte` con una identificación opcional deja pasar un individual sin docente. | Las fábricas `individual` y `general`, y el constructor rechaza la combinación inválida (`DescargaDeReporteTest`). |

## Complexity Tracking

Sin violaciones que justificar.
