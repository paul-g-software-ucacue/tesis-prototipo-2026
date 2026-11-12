# Implementation Plan: Generar el PDF/A del consolidado con su hash

**Branch**: `046-pdfa-consolidado` | **Date**: 2026-11-11 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/046-pdfa-consolidado/spec.md`

## Summary

Al pasar a CONGELADO_EN_FIRMA, Validación OCS publica su evento interno `DistributivoEnFirma` en la misma transacción del paso, con la cadena de firmas de la copia de la configuración. El registro de Spring Modulith lo guarda (ADR-018). Después de confirmar, en otro hilo, un oyente de Validación OCS le entrega la cadena a Firma por su API publicada, `PrepararDocumentoFirmable` (MD-05: Firma no conoce el flujo; research §4). Firma entonces:
1. pide el contenido de la versión congelada a Generación por su API publicada;
2. pide los nombres de los firmantes a Datos y a Identidad, entre procesos;
3. arma el XHTML y lo renderiza a PDF/A-2b con OpenHTMLtoPDF y PDFBox, el camino del spike B00-S01;
4. agrega con PDFBox los tres campos de firma vacíos con nombre;
5. guarda el archivo en el volumen, nombrado por su SHA-256 y sin sobrescribir;
6. registra el documento, la generación GENERADA y una entrada en la bitácora.

**Si falla**, la etapa no cambia. Firma registra el motivo y el Responsable de consolidación reintenta con «Generar el PDF». Generar es idempotente: un documento por versión.

**veraPDF** verifica la conformidad en las pruebas y en el script, no en el proceso (research §3).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash

**Primary Dependencies**:
- Pasan al runtime del servicio `firma`: OpenHTMLtoPDF 1.1.87 y PDFBox 3.0.8, las del spike, ya en `gradle.properties`.
- Solo en pruebas: veraPDF 1.30.2 (con JAXB 2.3, D-46) y Bouncy Castle.
- La fuente DejaVu Sans, con su licencia, pasa a los recursos de producción.

**Storage**:
- PostgreSQL: esquema `firma`, migración V2 (`generacion_documento` y `documento_firmable`).
- Usuarios: una migración para que la bitácora admita `firma`.
- Archivos: el volumen `archivos` de D-88, carpeta `documentos/`.

**Testing**:
- **Dominio:**
  - `GeneracionDeDocumento` (estados e idempotencia);
  - `ContenidoDelDocumento` (rótulos, cargos y nombres de los campos);
  - `ProcesoAprobacion` (el paso hacia CONGELADO_EN_FIRMA dice que entra en firma).
- **Aplicación (Mockito):**
  - `DocumentoFirmableServicio`: el orden de las llamadas; una generación por versión; el fallo con motivo y sin entrada; el reintento; el rol y la etapa;
  - `FlujoDelDistributivoServicio` publica `DistributivoEnFirma`, con la cadena de la copia, en los cuatro caminos y no en los rechazos; el oyente llama a `preparar`;
  - `ContenidoCongeladoServicio`;
  - el servicio de Roles del responsable.
- **Generador (sin Docker):** el PDF de un contenido pequeño y de uno de 165 paralelos pasa veraPDF 2b. Tiene el pie en cada página, el texto del contenido (extraído con PDFBox) y los tres campos de firma vacíos con su nombre y su rectángulo dentro de la última página.
- **Integración (PostgreSQL real):**
  - la migración V2;
  - dos generaciones a la vez dejan un documento y una entrada;
  - el almacén con `CREATE_NEW` no sobrescribe;
  - el evento llega a Firma después de confirmar (en el contexto de `apps:distributivo`, con dobles de los procesos remotos).
- **Contrato:** Firma (las dos operaciones), Roles (la ruta interna) y Usuarios (el servicio `firma`).
- **Arquitectura:** ArchUnit y `ModulosTest`. Validación OCS depende de Firma (`puertos.entrada`) y Firma de Generación, por interfaces nombradas; Firma no depende de Validación OCS (MD-05), sin ciclos.
- **Permisos:** `PermisosFirmaTest` con las dos operaciones.
- **SPA:** Vitest y Playwright con axe-core.
- **Punta a punta:** `deploy/probar_documento_firmable.sh`, con veraPDF en Docker.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: tres servicios de `distributivo` (Firma, Validación OCS y Generación), dos de `identidad` (Roles y Usuarios), la SPA y un script.

**Performance Goals**:
- SC-003: menos de 30 s desde CONGELADO_EN_FIRMA con 165 paralelos. Se mide en la prueba del generador y en el script.
- Memoria: el PDF se arma en memoria (unos cientos de KB), sin cambiar los límites de ADR-006.

**Constraints**:
- Principio V: Firma no lee tablas ajenas.
- D-83: el contenido y los firmantes se piden fuera de las transacciones.
- La aprobación no espera al PDF.
- Un archivo nunca se sobrescribe ni se borra.
- El rol del proceso solo inserta y lee `documento_firmable` (principio VII).

**Scale/Scope**:
- Firma:
  - 1 servicio de negocio nuevo, con dominio, aplicación, adaptadores y configuración;
  - 2 operaciones HTTP, 1 API publicada y 1 contrato nuevo;
  - 2 tablas;
  - 2 códigos de error.
- Validación OCS: el evento interno, su oyente y la cadena en `EtapasAlCrearse`.
- Generación: 1 API publicada.
- Roles: 1 ruta interna y 1 código de error. Usuarios: 1 API publicada y 1 migración.
- SPA: 1 componente.
- 1 script.
- 1 decisión (D-101).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Las reglas de la generación y del documento, en el dominio de Firma. El PDF y el almacén, detrás de puertos. La coordinación, en la aplicación. | ✅ |
| II. DDD | Firma es dueña del documento firmable (modelo de dominio §5). Generación da el contenido; Aprobación entrega la cadena y el rol de la etapa (MD-05); Roles da el Responsable. | ✅ |
| III. Lenguaje ubicuo | «Documento firmable», «versión congelada», «hash de contenido», «huella», «Elaborado/Revisado/Aprobado por». | ✅ |
| IV. API primero | `contratos/firma/openapi.yaml`, la ruta interna de Roles y el servicio `firma` en la bitácora se escriben antes del código. | ✅ |
| V. Límites | APIs publicadas en el proceso y rutas internas con token de proceso entre procesos; el evento por el registro de ADR-018. Aprobación le entrega la cadena a Firma (MD-05). | ✅ |
| VI. Las reglas son datos | Los firmantes, su orden y sus rótulos salen del árbol de firmas de la copia de la configuración (RF-58). | ✅ |
| VII. Inmutabilidad y trazabilidad | El archivo se nombra por su hash y no se sobrescribe. El registro guarda el SHA-256 original y el hash de contenido. Una entrada en la bitácora. | ✅ |
| VIII. Pruebas | Primero el dominio y la aplicación, viéndolos fallar; el generador contra veraPDF; integración para la concurrencia y el evento; script de punta a punta. | ✅ |
| IX. Seguridad | El permiso y el rol de la etapa en el servidor. El PDF no lleva datos de contacto. La ruta interna no expone más que el nombre del Responsable. | ✅ |
| X. Simplicidad | El camino probado del spike. Sin motor de plantillas, sin veraPDF en el proceso, sin un módulo de archivos en la plataforma. | ✅ |
| XI. Diseño pragmático | El bloque de firmas en la última página con posiciones fijas, para conocer los rectángulos de los campos. | ✅ |
| XII. JavaDoc | El evento, las APIs publicadas, los puertos, el generador, el almacén y las reglas del dominio. | ✅ |
| XIII. Errores | 2 códigos de Firma y 1 de Roles con sus 8 columnas. | ✅ |
| XIV. Validación | Sin cuerpo; el periodo con el patrón de siempre. | ✅ |

**Post-diseño:** sin cambios. D-101 queda registrada antes de implementar, con las decisiones de research §3, §4 y §7.

## Project Structure

### Documentation (this feature)

```text
specs/046-pdfa-consolidado/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── firma.md
│   ├── validacionocs.md
│   ├── generacion.md
│   ├── identidad.md
│   ├── spa.md
│   └── demostracion.md
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/firma/openapi.yaml               nuevo: consultar y generar el documento firmable
contratos/roles/openapi.yaml               /interno/roles/periodos/{periodo}/responsable
contratos/usuarios/openapi.yaml            servicio firma en la bitácora
contratos/comun/errores.md                 3 códigos

backend/servicios/firma/
├── build.gradle                           OpenHTMLtoPDF y PDFBox en implementation
├── dominio/                               DocumentoFirmable, GeneracionDeDocumento,
│                                          ContenidoDelDocumento, errores
├── puertos/entrada|salida/                los de data-model.md
├── aplicacion/                            DocumentoFirmableServicio (consultar, generar, al entrar en firma)
├── adaptadores/entrada/                   DocumentoControlador, ErroresFirma, OyenteDeFirma (@ApplicationModuleListener)
├── adaptadores/salida/                    GeneradorPdfa, AlmacenDeDocumentosEnDisco, RepositorioDocumentosJdbc,
│                                          ContenidoDesdeGeneracion,
│                                          FirmantesDesdeProcesos, PublicadorDeCambios
├── infraestructura/                       ConfiguracionFirma
└── src/main/resources/                    db/migration/firma/V2, fuentes/DejaVuSans.ttf

backend/servicios/validacionocs/           DistributivoEnFirma (interno), AvisoDeFirma, PreparacionDelDocumento;
                                           la cadena en EtapasAlCrearse
backend/servicios/generacion/              ContenidoCongelado y su servicio
backend/servicios/roles/                   ruta interna del responsable
backend/servicios/usuarios/                NombreDeUsuario; migración de la bitácora
backend/apps/distributivo/                 la configuración de Firma y el volumen

frontend/src/pantallas/comun/              DocumentoFirmable.tsx, documento.ts, EstadoDelFlujo.tsx
frontend/e2e/consolidado.spec.ts

deploy/probar_documento_firmable.sh y su paso en perfil-consolidado
docs/decisiones.md                         D-101
```

**Structure Decision**: Firma deja de ser un esqueleto y pasa a ser un servicio con sus cinco capas. Las pruebas del spike (`ec.mdd.firma.spike`) quedan como están (D-44).

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Los campos de firma rompen la conformidad PDF/A-2b (widgets sin apariencia). | Apariencia normal vacía e indicador *Print*; veraPDF en la prueba del generador desde el primer campo. |
| El contenido no entra en el bloque fijo de la última página o una tabla corta mal entre páginas. | El bloque de firmas va solo en su página; las tablas con `page-break-inside: avoid` por fila; prueba con 165 paralelos. |
| Las limpiezas de los scripts existentes hacen `rm -f /var/mdd/archivos/*` y fallan con la carpeta `documentos/` («AVISO»). | Cambiarlas a `rm -rf /var/mdd/archivos/*` en los scripts que la usan. |
| Un fallo determinista reintentado cada 30 s por el reenvío de publicaciones. | Firma registra el fallo y no relanza (research §4). |
| El tiempo de generación con 165 paralelos en la VM de la demo. | Medirlo en la prueba y en la CI; la aprobación no lo espera. |
| Roles depende de Usuarios por primera vez. | Por interfaz publicada, en el mismo proceso; `ModulosTest` de `identidad` lo verifica. |

## Complexity Tracking

Sin violaciones que justificar.
