# Implementation Plan: Reporte individual del docente en PDF y CSV

**Branch**: `058-reporte-individual-docente` | **Date**: 2026-11-21 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/058-reporte-individual-docente/spec.md`

## Summary

**Qué lee Reporting** (research §1):
- De Aprobación, la publicación vigente de cada periodo: la última transición a PUBLICADO, con la versión y su hash.
- De Generación, el contenido de esa versión por docente.
- Las dos son rutas internas nuevas del proceso `distributivo`, leídas con `ConsultasEntreProcesos`.

**El contenido congelado** (§2): hoy las horas de clase salen de la oferta actual, y Académico deja corregirla después de publicar. Por eso, Generación guarda el contenido por docente de cada versión una sola vez, al congelarla:
- sus asignaturas con las horas de clase de ese momento;
- sus actividades;
- su carga horaria por componente y su dedicación.

Lo pide Aprobación con el mismo aviso que prepara el PDF/A firmable. Si falta, se calcula y se guarda al pedirlo, marcado como tardío.

**El reporte** (§3 a §5):
- **`ReporteIndividual`:** un agregado de lectura de Reporting que comprueba que el hash de la publicación es el del contenido congelado.
- **El PDF/A-2b:** con openhtmltopdf y PDFBox, sin campos ni contenido activo, con el periodo, la versión y el hash en el pie de cada página.
- **El CSV:** UTF-8 con BOM, `;`, coma decimal, una fila por asignación y seis de totales, y las fórmulas neutralizadas.

**La API y la pantalla** (§6, §9): cuatro operaciones con `REPORTES:LECTURA`: publicaciones, docentes, el reporte en JSON y las dos descargas. La pantalla «Reportes» tiene un selector de periodo, un buscador de docentes, el reporte y dos botones.

**La bitácora** (§7):
- Cada descarga publica `reporting.EntradaBitacora` (`REPORTE_INDIVIDUAL_DESCARGADO`) por el registro persistente de publicaciones de `salida`, antes de responder.
- Para eso:
  - `salida` suma `plataforma:eventos` y su clave de proceso;
  - Usuarios admite el origen `reporting`.

**La demostración** (§10): seis pasos nuevos en `probar_documento_firmable.sh`: antes de publicar, con la versión 1, durante la reforma (con una corrección de la oferta) y con la versión 2.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash

**Primary Dependencies**:
- **Reporting:** PDFBox y openhtmltopdf, con las versiones de `gradle.properties` que ya usa Firma; veraPDF y JAXB 2.3 solo en las pruebas, como en Firma.
- **`apps/salida`:** `plataforma:eventos`.
- Ninguna dependencia nueva para el repositorio.

**Storage**:
- **PostgreSQL:**
  - Generación V10: `contenido_congelado` y `carga_congelada`, solo de inserción;
  - el esquema `eventos_salida`, que ya existe, con el registro de publicaciones (lo migra `flywayEventos`).
  - Reporting sigue sin tablas.
- Ningún archivo en disco: el PDF y el CSV se generan en memoria y no se guardan.

**Testing**:
- **Dominio de Reporting** (sin dobles, viéndolos fallar primero):
  - `ReporteIndividualTest`: el orden, los totales, los componentes en 0, el hash distinto y los decimales;
  - `EscritorCsvTest`: BOM, `;`, CRLF, comillas, coma decimal, cada carácter de fórmula, tildes, y bytes idénticos para la misma entrada.
- **Dominio de Generación:** `CargaCongeladaTest`, el invariante de las sumas.
- **Aplicación (Mockito en los puertos):**
  - `ConsultarReportesServicioTest` y `DescargarReporteServicioTest`: el registro antes de responder, y sin archivo si el registro falla;
  - `GuardarContenidoCongeladoServicioTest`: idempotente, y no hace nada si la versión no es la vigente o no está congelada;
  - `ConsultarContenidoCongeladoServicioTest`: el caso tardío;
  - `ConsultarPublicacionesServicioTest`: durante la reforma.
- **Adaptadores:**
  - `GeneradorDeReportePdfaTest`: veraPDF 2B, sin `AcroForm` ni anotaciones, el pie en cada página y un docente con 40 filas;
  - `ContenidoCongeladoIntegrationTest` y `GeneracionEsquemaIntegrationTest`: V10, en PostgreSQL real;
  - `RegistroDeDescargasIntegrationTest`: el evento en `eventos_salida` en la misma transacción;
  - `ClientesDeReportingIntegrationTest`: 404, 502 y 504 con un servidor HTTP de prueba.
- **Contrato:**
  - `ReportingContratoTest`;
  - `InternoValidacionocsContratoTest` e `InternoGeneracionContratoTest`;
  - `RecibirEventoContratoTest`, con `reporting`.
- **Arquitectura:** `ArquitecturaTest` de Reporting: PDFBox y openhtmltopdf solo en los adaptadores; el dominio sin Spring.
- **SPA:** Vitest y Playwright con axe-core ([contracts/spa.md](contracts/spa.md)).
- **Punta a punta:** los pasos R1 a R6 ([contracts/demostracion.md](contracts/demostracion.md)).

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web:
- Reporting, en el proceso `salida`;
- Generación y Validación OCS, en `distributivo`;
- Usuarios, en `identidad`;
- la SPA y un script.

**Performance Goals**:
- **SC-005:** cada descarga en menos de 5 s con los datos de prueba (47 docentes, 165 paralelos). Un reporte individual tiene decenas de filas; el PDF/A de F13-H02, con todo el consolidado, ya se genera en ese orden.
- El contenido tardío (una vez por versión) calcula toda la versión, como el PDF/A de F13-H02.

**Constraints**:
- **Principio V:** Reporting no lee esquemas ajenos.
- **Principio VII:** el contenido publicado no cambia y cada descarga queda registrada.
- **RNF-SEG-04:** las fórmulas del CSV.
- **ADR-006:** el PDF del reporte se genera en `salida`, no en `distributivo`.
- **Principio X:** sin colas ni almacenamiento de archivos nuevos.

**Scale/Scope**:
- **Reporting** (el primer código del módulo):
  - el agregado, 6 objetos de valor y el escritor de CSV;
  - 2 casos de uso y 5 puertos de salida, con sus adaptadores;
  - 1 controlador con 5 operaciones;
  - 3 errores del catálogo.
- **Generación:**
  - 1 objeto de valor y 2 casos de uso;
  - 1 puerto de salida y 1 método de repositorio;
  - 1 controlador interno con 2 rutas;
  - 1 migración.
- **Validación OCS:** 1 caso de uso, 1 oyente y 1 controlador interno con una ruta.
- **Usuarios e Identidad:** el origen `reporting` y la clave de `salida`.
- **Despliegue:** la clave nueva en `compose.yaml` y en los secretos.
- **SPA:** 1 pantalla y 1 enlace.
- **Script:** 6 pasos.
- **La decisión D-115.**

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | El reporte, sus invariantes y el CSV son del dominio de Reporting. El PDF/A, los clientes HTTP, el registro de publicaciones y el reloj quedan en adaptadores, detrás de puertos. | ✅ |
| II. DDD | Reporting es soporte: un agregado de lectura, sin reglas propias del distributivo. `CargaCongelada` es de Generación, dueña de la carga horaria. | ✅ |
| III. Lenguaje ubicuo | «Reporte individual», «versión publicada», «carga horaria», «componente», «asignación» y «dedicación», del glosario. `CargaCongelada` y `contenido_congelado` siguen «versión congelada». | ✅ |
| IV. API primero | `contratos/reporting/openapi.yaml` nuevo, y las rutas internas en los contratos de Validación OCS y Generación, antes del código. | ✅ |
| V. Límites | Reporting lee Aprobación y Generación por rutas internas con el token de proceso (ADR-023). La bitácora llega por el registro persistente de publicaciones (ADR-018). Nadie lee el esquema de otro. | ✅ |
| VI. Las reglas son datos | No hay reglas nuevas: la carga horaria es la de RF-27, ya calculada. | ✅ |
| VII. Inmutabilidad | El contenido congelado es solo de inserción, y el reporte de una versión publicada no cambia aunque cambie la oferta (research §2, paso R5). Cada descarga queda en la bitácora antes de responder. | ✅ |
| VIII. Pruebas | El dominio primero y sin dobles; veraPDF en las pruebas; PostgreSQL real para el contenido congelado y el registro; los pasos R1 a R6. | ✅ |
| IX. Seguridad | Mínimo privilegio con `REPORTES:LECTURA` en el backend; `Cache-Control: no-store`; fórmulas neutralizadas; la entrada de la bitácora sin el nombre ni las horas. Solo datos ficticios. | ✅ |
| X. Simplicidad | Reporting no guarda archivos ni copias, no hay colas, y el contenido congelado reutiliza los cálculos existentes. Sin módulo compartido de PDF todavía (research §4). | ✅ |
| XI. Diseño pragmático | Sigue los moldes de `ConsultasEntreProcesos`, `PreparacionDelDocumento`, `GeneradorPdfa` y `RegistroDeCambiosJdbc`. | ✅ |
| XII. JavaDoc | Explica por qué se guarda el contenido al congelar, por qué se compara el hash, por qué `;` y coma decimal, y por qué se registra antes de responder. | ✅ |
| XIII. Errores | Tres códigos nuevos en el catálogo, con sus ocho columnas; la caída de un proceso usa el 502 o el 504 común; el 500 no tiene detalles técnicos. | ✅ |
| XIV. Validación | El periodo y la identificación se validan con los patrones del contrato en el adaptador de entrada; las respuestas internas se leen con su esquema. | ✅ |

**Post-diseño:** sin violaciones. Hay un ajuste de la spec y un alcance que la spec no preveía:
- **FR-014:** la pantalla propone el periodo de la publicación más reciente, no «el vigente» de Académico (research §6). Se corrigió FR-014 y su caso límite.
- **El contenido congelado en Generación** (research §2): es necesario para que FR-010 y SC-001 se cumplan después de publicar. Le sirve también a F20-H02 y a F19.

La decisión D-115 se registra antes de implementar, con research §1 a §10.

## Project Structure

### Documentation (this feature)

```text
specs/058-reporte-individual-docente/
├── spec.md, plan.md, research.md, data-model.md, quickstart.md
├── contracts/ (reporting.md, interno.md, spa.md, demostracion.md)
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/reporting/openapi.yaml        nuevo: 5 operaciones
contratos/validacionocs/openapi.yaml    consultarPublicacionesVigentes, PublicacionVigente
contratos/generacion/openapi.yaml       consultarCargasCongeladas, consultarCargaCongelada
contratos/usuarios/openapi.yaml         recibirEvento: el origen reporting
contratos/comun/errores.md              REPORTING_SIN_PUBLICACION, REPORTING_DOCENTE_SIN_DISTRIBUTIVO,
                                        REPORTING_CONTENIDO_INCONSISTENTE

backend/servicios/reporting/
├── build.gradle        pdfbox, openhtmltopdf, plataforma:eventos, jdbc, oauth2; veraPDF en pruebas
├── dominio/            ReporteIndividual, Publicacion, DatosDelDocente, FilaDeAsignatura,
│                       FilaDeActividad, HorasPorComponente, Formato, EscritorCsv,
│                       SinPublicacion, DocenteSinDistributivo, ContenidoInconsistente
├── puertos/entrada/    ConsultarReportes, DescargarReporte, ArchivoDelReporte
├── puertos/salida/     PublicacionesDelDistributivo, ContenidoDeLaVersion, GeneradorDePdf,
│                       RegistroDeDescargas, Transaccion
├── aplicacion/         ConsultarReportesServicio, DescargarReporteServicio
├── adaptadores/entrada/ ReportesControlador, ErroresReporting
├── adaptadores/salida/ PublicacionesDesdeAprobacion, ContenidoDesdeGeneracion,
│                       GeneradorDeReportePdfa, RegistroDeDescargasEventos, TransaccionSpring
├── infraestructura/    ConfiguracionReporting
└── resources/fuentes/  DejaVuSans.ttf, LICENSE-DejaVu.txt

backend/servicios/generacion/
├── dominio/            CargaCongelada, AsignaturaCongelada, ActividadCongelada
├── puertos/entrada/    GuardarContenidoCongelado, ConsultarContenidoCongelado
├── puertos/salida/     RepositorioContenidoCongelado, RepositorioDistributivos (version)
├── aplicacion/         ContenidoCongeladoServicio (docentes de una versión dada),
│                       GuardarContenidoCongeladoServicio, ConsultarContenidoCongeladoServicio
├── adaptadores/entrada/ InternoGeneracionControlador
├── adaptadores/salida/ RepositorioContenidoCongeladoJdbc, RepositorioDistributivosJdbc (version)
└── resources/db/migration/generacion/V10__contenido_congelado.sql

backend/servicios/validacionocs/
├── puertos/entrada/    ConsultarPublicaciones, PublicacionVigente
├── aplicacion/         ConsultarPublicacionesServicio
├── adaptadores/entrada/ InternoValidacionocsControlador
└── adaptadores/salida/ ContenidoDeLaFirma (@ApplicationModuleListener)

backend/servicios/usuarios/   RegistrarEventoRecibidoServicio, EntradaBitacora, FiltroDeBitacora
backend/apps/salida/          build.gradle (plataforma:eventos), application.yaml (eventos, procesos)
backend/apps/identidad/       application.yaml (mdd.usuarios.procesos.salida.clave)
compose.yaml, deploy/          MDD_CLAVE_PROCESO_SALIDA, MDD_URI_DISTRIBUTIVO

frontend/src/pantallas/reportes/ Reportes.tsx (+ prueba)
frontend/src/rutas.tsx, frontend/src/navegacion/ (el enlace y enlaces.test.ts)
frontend/e2e/reportes.spec.ts
deploy/probar_documento_firmable.sh   R1 a R6
docs/decisiones.md                    D-115
```

**Structure Decision**:
- El reporte es de Publicación y consulta (Reporting, proceso `salida`), como dicen el glosario y ADR-006.
- Generación guarda su propio contenido congelado, porque es dueña de la carga horaria.
- Aprobación dice cuál versión está publicada y ordena guardar el contenido, como ordena el documento firmable.
- Firma, Académico, Docentes y Notificaciones no cambian.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Las versiones congeladas antes de esta historia no tienen contenido guardado, y en la demo la oferta pudo cambiar desde entonces. | El primer pedido lo calcula y lo marca `tardio`, con un `WARN` (research §2). En la demo, 2026-I se vuelve a recorrer con el script, así que el contenido sale de la congelación. |
| El oyente que guarda el contenido falla y nadie lo nota. | El registro de publicaciones lo reintenta (ADR-018), y el caso tardío cubre el hueco. `ContenidoDeLaFirma` registra el fallo con el periodo y la versión. |
| El contenido guardado no coincide con el PDF/A firmado, porque los dos se calculan en momentos distintos tras congelar. | Los dos se calculan con el mismo aviso, después de confirmar la congelación. Además, la comparación del hash detecta otra versión. Las horas de una oferta corregida en esos milisegundos es un caso que se acepta y se documenta en D-115. |
| La clave de proceso de `salida` falta en el despliegue y las descargas fallan con 502. | `desplegar.sh` exige la variable, como las otras dos (D-55), y el *healthcheck* de `salida` sigue en verde: el fallo es por pedido y se ve en R2. |
| El CSV con `;` se abre mal en una hoja de cálculo configurada en inglés. | Es la configuración regional de la institución (es-EC), y D-115 lo deja escrito. En otra configuración, la hoja de cálculo deja elegir el separador al importar. |
| veraPDF en Reporting repite el ajuste de JAXB de Firma. | Se copia el mismo bloque de `build.gradle`, con su comentario. Un módulo compartido queda para cuando haga falta (research §4). |
| Un docente con muchas filas parte las tablas mal entre páginas. | `GeneradorDeReportePdfaTest` prueba 40 filas: el encabezado se repite y el pie está en cada página. |
| Sumar `plataforma:eventos` a `salida` cambia su arranque: `flywayEventos` y el reenvío. | `SalidaAplicacionTest` arranca el contexto con PostgreSQL real, y la medición de memoria de ADR-006 (`medir_memoria.sh`) se repite al cerrar la ola. |

## Complexity Tracking

Sin violaciones que justificar.
