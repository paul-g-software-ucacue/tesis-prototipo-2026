# Implementation Plan: Validar antes de exportar y registrar la exportación

**Branch**: `061-exportacion-siga` | **Date**: 2026-11-22 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/061-exportacion-siga/spec.md`

## Summary

Es la segunda y última historia de F19. Convierte el CSV para SIGA de F19-H01 (D-119) en la Exportación a SIGA del glosario: validado, con su suma de verificación y registrado.

**La validación** (research §1 a §3):
- **Las firmas:** Firma publica una ruta interna con la cadena del documento de una versión.
- **Las inconsistencias:** Generación guarda las abiertas de la versión cuando guarda su contenido congelado (D-115) y las devuelve en la ruta del contenido, que Reporting ya llama.
- **La regla** es de Reporting: `CondicionesDeExportacion`, en su dominio, junta todos los motivos y rechaza con `REPORTING_VERSION_NO_EXPORTABLE` (409). El rechazo deja la entrada `EXPORTACION_SIGA_RECHAZADA` (aclaración del 2026-11-22).

**La suma y el registro** (§4 y §5):
- **`SumaDeVerificacion`:** el SHA-256 de los bytes entregados, en hexadecimal.
- **`ExportacionSiga`:** un agregado nuevo, en `reporting.exportacion_siga` (V3), sin `update` ni `delete`.
- **Una transacción:** el registro y la entrada `CSV_SIGA_GENERADO`, ampliada con la suma, se guardan juntos antes de entregar.

**La API y la pantalla** (§6 y §7):
- **El CSV** suma el encabezado `X-Suma-Verificacion` y el nuevo 409.
- **Una operación nueva** lista las exportaciones del periodo.
- **«Exportación a SIGA»** suma el aviso de la hoja de cálculo, la suma con su botón de copiar y la línea de `certutil`/`sha256sum`, y la tabla «Exportaciones del periodo».

**La demostración** (§10): R1, R4, R5 y R9 ampliados, y R11 nuevo.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash y Python 3 en el script

**Primary Dependencies**: ninguna nueva. El SHA-256 sale de `java.security.MessageDigest` del JDK, y la copia, de `navigator.clipboard`.

**Storage**:
- **Reporting:** `V3__exportacion_siga.sql`, una tabla con disparador contra `update` y `delete`.
- **Generación:** `V11__inconsistencias_al_congelar.sql`, dos columnas opcionales en `contenido_congelado`, que se llenan en el `insert`.
- **Firma:** sin cambios en la base.
- **La bitácora:** por `eventos_salida`, que ya existe.
- **El CSV** no se guarda.

**Testing**:
- **Dominio de Reporting** (sin dobles, viéndolas fallar primero): `CondicionesDeExportacionTest`, `SumaDeVerificacionTest` y `ExportacionSigaTest`.
- **Aplicación (Mockito en los puertos):**
  - `GenerarCsvSigaServicioTest`: el rechazo y su entrada, la entrada que no se guarda, el registro y la entrada en la misma transacción antes de entregar, la suma de los bytes entregados, la reforma y los 300 docentes;
  - `ConsultarExportacionesSigaServicioTest`.
- **Adaptadores:**
  - `ExportacionesSigaJdbcIntegrationTest` (PostgreSQL real), `ClientesDeReportingTest` (Firma y las inconsistencias) y `RegistroEnBitacoraEventosTest`;
  - `RegistroEnBitacoraIntegrationTest` en `apps/salida`.
- **Firma** (dominio central, TDD): `FirmasDelDocumentoTest`, `DocumentoFirmableServicioTest` e `InternoFirmaContratoTest`.
- **Generación** (dominio central, TDD): `GuardarContenidoCongeladoServicioTest`, `ContenidoCongeladoIntegrationTest` e `InternoGeneracionContratoTest`.
- **Contrato:** `SigaContratoTest`, `ErroresReportingTest` y la prueba del catálogo ([contracts/reporting.md](contracts/reporting.md), [contracts/interno.md](contracts/interno.md)).
- **Arquitectura:** `ArquitecturaTest` y `ApplicationModules.verify()`.
- **SPA:** Vitest y Playwright con axe-core ([contracts/spa.md](contracts/spa.md)).
- **Punta a punta:** R1, R4, R5, R9 y R11 ([contracts/demostracion.md](contracts/demostracion.md)).

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web:
- Reporting, en `salida`;
- Firma y Generación, en `distributivo`;
- la SPA y el script.

**Performance Goals**:
- **SC-007:** menos de 10 s con 300 docentes, igual que en F19-H01. La validación suma una llamada interna (las firmas); las inconsistencias vienen en la del contenido. El SHA-256 de un archivo de cientos de KB tarda milisegundos.

**Constraints**:
- **Principio V:** Reporting no lee el esquema de Firma ni de Generación, ni usa sus clases. Firma informa hechos, sin decidir la regla de la exportación.
- **Principio VII:** el registro no se edita ni se borra; el registro y su entrada se guardan juntos.
- **Principio XIII:** el mensaje del rechazo no lleva hashes ni nombres.
- **Principio X:** sin guardar archivos ni agregar dependencias.

**Scale/Scope**:
- **Reporting:**
  - dominio: 1 agregado, 6 objetos de valor, 1 enumerado y 1 error nuevos; se quita `CsvGenerado`;
  - puertos: 1 de entrada nuevo y 1 que cambia; 2 de salida nuevos y 2 que cambian;
  - aplicación: 1 servicio nuevo y 1 que cambia;
  - adaptadores: el cliente de Firma, el repositorio JDBC, el controlador y el registro;
  - 1 migración.
- **Firma:** 1 puerto de entrada, 1 método de servicio y 1 controlador interno.
- **Generación:** `ContenidoCongelado`, el servicio de guardado, el repositorio y la ruta; 1 migración.
- **Contratos:**
  - `reporting`: 1 operación nueva, 1 que cambia, 2 esquemas, 1 encabezado y 1 respuesta;
  - `firma`: 1 operación interna y 2 esquemas;
  - `generacion`: 1 campo y 1 esquema;
  - `errores.md`: 1 código nuevo y 2 que amplían su causa.
- **SPA:** 1 sección nueva y 1 que cambia, con sus pruebas.
- **Script:** 4 pasos ampliados y 1 nuevo.
- **La decisión D-120.**

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | La validación, la suma y el registro son del dominio de Reporting, que sigue sin framework (`MessageDigest` es del JDK). Firma, Generación, JDBC, HTTP y los eventos quedan detrás de puertos. El puerto `ExportadorSiga` no cambia. | ✅ |
| II. DDD | Reporting es soporte: un agregado de registro y un objeto de valor con la regla de RF-53. Firma y Generación, centrales, solo exponen hechos que ya tienen, con TDD. | ✅ |
| III. Lenguaje ubicuo | «Exportación a SIGA» (`ExportacionSiga`), «Inconsistencia», «Paso de la cadena» y «Hash» son del glosario. «Suma de verificación» entra antes del código (T003). Se conserva `CSV_SIGA_GENERADO` para no tener dos nombres de la misma acción (research §5). | ✅ |
| IV. API primero | La ruta de Firma, el campo de Generación, la operación nueva, el encabezado y el 409 de Reporting van en sus OpenAPI 3.0.3 antes del código, y el código nuevo va en el catálogo. | ✅ |
| V. Límites | Reporting lee Firma y Generación por rutas internas con el token de proceso (ADR-023). La regla de «exportable» es de Reporting; Firma no la conoce. La bitácora va por el registro persistente (ADR-018). | ✅ |
| VI. Las reglas son datos | No agrega cifras ni reglas de la política. El layout sigue en su archivo. | ✅ |
| VII. Inmutabilidad | La exportación sale de la versión congelada y publicada. Las inconsistencias se fijan al congelar. El registro no admite `update` ni `delete`. El registro y la entrada se guardan juntos, antes de entregar. | ✅ |
| VIII. Pruebas | El dominio de Reporting, sin dobles; Firma y Generación, con TDD; PostgreSQL real; el contrato con sus errores; R9 compara la suma con `sha256sum`. | ✅ |
| IX. Seguridad | `PUBLICACION:APROBACION` en el backend. La ruta de Firma solo admite `PROCESO` y no lleva nombres ni firmantes. La bitácora va sin datos personales, y el mensaje del 409, sin hashes. | ✅ |
| X. Simplicidad | Sin archivos guardados ni dependencias. Dos números en lugar de la lista de inconsistencias. Las inconsistencias viajan en una llamada que ya existe. | ✅ |
| XI. Diseño pragmático | No se generaliza el motor de reglas para versiones que no son la vigente (research §2) ni se crea una interfaz de validador. El cambio de firma de `GenerarCsvSiga` es el mínimo para devolver el registro. | ✅ |
| XII. JavaDoc | Explica por qué las inconsistencias se fijan al congelar, por qué falla cerrada sin el dato, por qué el rechazo se registra sin transacción obligatoria y por qué se conserva `CSV_SIGA_GENERADO`. | ✅ |
| XIII. Errores | Un código nuevo con todos sus campos. El mensaje se arma con los motivos, sin `detalles`, que son solo de entrada. Firma usa el 404 común. | ✅ |
| XIV. Validación | La única entrada nueva es `periodo` en la operación nueva, con el patrón que ya tienen las demás. La ruta de Firma valida `periodo` y `numero`. La suma se valida con su patrón al leerla y al guardarla. | ✅ |

**Post-diseño:** sin violaciones. Hay un ajuste a la spec, que se registra en D-120 y no contradice ningún requisito. FR-003 dice «según las inconsistencias registradas de esa versión», y hoy no se registran (research §2). Esta historia las registra al congelar. Una versión sin el registro no pasa la validación (motivo `INCONSISTENCIAS_SIN_REGISTRO`), porque es la lectura de FR-001 que falla cerrada: solo se exporta lo que se pudo comprobar.

La decisión D-120 se registra antes de implementar, con research §1 a §12.

## Project Structure

### Documentation (this feature)

```text
specs/061-exportacion-siga/
├── spec.md, plan.md, research.md, data-model.md, quickstart.md
├── contracts/ (reporting.md, interno.md, spa.md, demostracion.md)
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/reporting/openapi.yaml        generarCsvSiga (X-Suma-Verificacion, 409 NoExportable);
                                        consultarExportacionesSiga; ExportacionesSiga, ExportacionSiga
contratos/firma/openapi.yaml            consultarFirmasDeLaVersion; FirmasDeLaVersion, PasoDeLaVersion
contratos/generacion/openapi.yaml       ContenidoCongeladoCompleto.inconsistenciasAbiertas;
                                        InconsistenciasAlCongelar
contratos/comun/errores.md              REPORTING_VERSION_NO_EXPORTABLE; 502 y 504 ampliados

backend/servicios/reporting/
├── dominio/            FirmasDeLaVersion, InconsistenciasAbiertas, MotivoDeRechazo,
│                       CondicionesDeExportacion, ExportacionRechazada, SumaDeVerificacion,
│                       ExportacionSiga, CsvExportado, RechazoDeExportacion (nuevos);
│                       CargasDeLaVersion (inconsistencias); CsvGenerado (se quita)
├── puertos/entrada/    ConsultarExportacionesSiga (nuevo); GenerarCsvSiga (devuelve CsvExportado)
├── puertos/salida/     FirmasDeLasVersiones, ExportacionesSiga (nuevos);
│                       ContenidoDeLaVersion, RegistroEnBitacora (cambian)
├── aplicacion/         ConsultarExportacionesSigaServicio (nuevo); GenerarCsvSigaServicio
├── adaptadores/entrada/ SigaControlador (encabezado, operación nueva), ErroresReporting
├── adaptadores/salida/ FirmasDesdeFirma, ExportacionesSigaJdbc (nuevos);
│                       ContenidoDesdeGeneracion, RegistroEnBitacoraEventos
├── infraestructura/    ConfiguracionReporting
└── resources/          db/migration/reporting/V3__exportacion_siga.sql

backend/servicios/firma/
├── puertos/entrada/    ConsultarFirmasDeLaVersion (nuevo)
├── aplicacion/         DocumentoFirmableServicio
├── adaptadores/entrada/ InternoFirmaControlador (nuevo)
└── infraestructura/    la configuración

backend/servicios/generacion/
├── dominio/            ContenidoCongelado, InconsistenciasAlCongelar (nuevo)
├── aplicacion/         GuardarContenidoCongeladoServicio (el panel)
├── adaptadores/        RepositorioContenidoCongeladoJdbc, InternoGeneracionControlador
└── resources/          db/migration/generacion/V11__inconsistencias_al_congelar.sql

backend/apps/salida/src/integrationTest/  RegistroEnBitacoraIntegrationTest

frontend/src/pantallas/siga/     Siga.tsx, consultas.ts, Siga.test.tsx
frontend/src/api/generado/       reporting.d.ts (regenerado)
frontend/e2e/siga.spec.ts
deploy/probar_documento_firmable.sh   R1, R4, R5, R9 ampliados; R11
docs/decisiones.md                    D-120
```

**Structure Decision**:
- La exportación y su registro son de Publicación y consulta (Reporting, `salida`): la capa anticorrupción hacia SIGA.
- Firma y Generación, en `distributivo`, solo exponen hechos de una versión.
- Aprobación, Usuarios, Identidad, el API Gateway y el despliegue no cambian. El Gateway ya enruta `/api/reporting/**`, y `/interno/**` ya exige `PROCESO`.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Una versión congelada antes de esta historia no tiene las inconsistencias registradas y no se puede exportar. | Es la falla cerrada declarada (research §2). En el equipo de demostración, 2026-I todavía no se congeló, y el script recrea el distributivo. El quickstart lo pide como requisito. D-120 lo anota como consecuencia. |
| El contenido se guarda tarde, cuando la vigente ya es otra (una reforma empezada), y queda sin inconsistencias. | Solo pasa si el guardado al congelar falló y nadie pidió un reporte antes de la reforma. El oyente de Aprobación reintenta (ADR-018). Si pasara, el rechazo lo dice y queda en la bitácora. |
| El Vicerrector ajustó la versión después de la revisión, y al congelar tiene inconsistencias abiertas: se publica, pero no se exporta. | Es justo el caso que RF-53 quiere detectar. El mensaje dice cuántas hay y a quién avisar. |
| Una `salida` nueva contra un `distributivo` viejo: la ruta de Firma no existe y la exportación falla. | Los procesos salen de la misma versión del repositorio. Si pasara, Reporting responde 502 sin entregar nada. |
| Cambiar lo que devuelve `GenerarCsvSiga` rompe el controlador y las pruebas de F19-H01. | Es un cambio mecánico, en el mismo PR. `SigaContratoTest` y R9 siguen comparando el archivo byte a byte. |
| El encabezado `X-Suma-Verificacion` no le llega a la SPA. | La petición es del mismo origen, por el API Gateway. La prueba de Playwright compara el valor con el SHA-256 del archivo descargado. |
| `navigator.clipboard` no está disponible (por ejemplo, fuera de HTTPS). | El botón avisa y la suma se puede seleccionar a mano. La pantalla se sirve por HTTPS en el perfil y en la demo. |

## Complexity Tracking

Sin violaciones que justificar.
