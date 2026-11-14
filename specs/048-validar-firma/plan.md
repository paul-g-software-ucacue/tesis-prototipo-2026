# Implementation Plan: Validar que el PDF subido es el mismo documento con una firma íntegra

**Branch**: `048-validar-firma` | **Date**: 2026-11-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/048-validar-firma/spec.md`

## Summary

**Una operación nueva de Firma:** `POST /api/firma/documentos/{id}/firmas`, con `DISTRIBUTIVO:FIRMA`.
- Solo el firmante en turno, la regla de F14-H01. A los demás, el 403 común.
- Recibe el PDF firmado y lo valida en cuatro comprobaciones (D-23):
  1. el mismo documento, por el prefijo de bytes, en el dominio;
  2. una sola firma nueva;
  3. la integridad sobre su ByteRange;
  4. la confianza contra la lista de raíces.

  Las tres últimas van en un adaptador sobre PDFBox y Bouncy Castle, que parte del validador del spike (research §3).

**Cada subida del firmante en turno deja una fila en la tabla nueva `validacion_firma`** (V4, solo `insert` y `select`) y su entrada en la bitácora (research §1, §9).
- **Aceptada:** la fila es la firma del paso. Su archivo, guardado por su SHA-256, pasa a ser el vigente, y el turno avanza (research §2). La validación va fuera de la transacción; el registro, con el bloqueo de la generación y un índice único (research §4).
- **Rechazada:** un 422 del catálogo, con las cuatro comprobaciones en los detalles (research §8).

**La lista de confianza** son PEM en los recursos de Firma: la CA de prueba y la raíz pública del BCE. Las pruebas cargan solo la CA de prueba (research §5).

**En la SPA**, «Subir el PDF firmado» en cada fila de «Pendientes de firma», con el informe.

**El script de punta a punta** suma cuatro pasos y firma con una herramienta Java de un solo archivo (research §6).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash

**Primary Dependencies**:
- `bcpkix-jdk18on` pasa de las pruebas al código de Firma (la versión de `gradle.properties`, la del spike);
- PDFBox ya está;
- sin otras nuevas.

**Storage**:
- PostgreSQL, esquema `firma`, migración V4 (`validacion_firma`, solo `insert` y `select`, con el índice único parcial).
- Archivos: el volumen de D-88. Se escribe `documentos/<sha256>.pdf` de cada firma aceptada, sin sobrescribir.

**Testing**:
- **Dominio:**
  - `ResultadoValidacion`: las cuatro en orden, «no evaluada» después de un rechazo, `aceptada` y `primerRechazo`;
  - `ComprobacionDeDocumento.mismoDocumento`: igual, más corto, otro prefijo y más largo;
  - `FirmasDelDocumento`: el turno y el archivo vigente con 0, 1, 2 y 3 firmas.
- **Aplicación (Mockito), `SubirFirmaServicio`:**
  - en turno y aceptada: guarda el archivo, registra y publica `FIRMA_ACEPTADA`;
  - rechazada: no guarda el archivo, registra el rechazo y publica `FIRMA_RECHAZADA`;
  - fuera de turno, inexistente o no generado: `SinTurnoDeFirma`, sin registro;
  - vacío y demasiado grande, antes de validar;
  - el vigente que falta: `ArchivoNoDisponible`;
  - las firmas cambiaron entre validar y registrar: rechazo por «mismo documento».
- **Adaptador `ValidadorPades`**, con PDF del generador de F13-H02 firmados por `FirmadorDePrueba` y la CA de prueba:
  - una firma en el campo del paso, y una en un campo nuevo;
  - sin firma nueva, dos firmas nuevas, alterado y la CA ajena;
  - el relleno de ceros, y un PDF dañado después del prefijo;
  - las tres sucesivas, verificadas también con `ValidadorPadesDePrueba` del spike (SC-005).
- **La lista de confianza:** carga los PEM; sin raíces o con uno ilegible, falla al arrancar; la huella de la raíz del BCE.
- **Integración (PostgreSQL real):**
  - la V4 con el rol del proceso: inserta y lee, no actualiza ni borra, y la clave foránea funciona;
  - el índice único;
  - `RepositorioDocumentosJdbc` registra y lee las firmas;
  - dos registros a la vez para el mismo paso: uno solo se acepta.
- **Contrato:** `SubirFirmaContratoTest` (201, 400, 413, 422 con los detalles, 403); `PermisosFirmaTest` con las cinco operaciones; `ErroresFirmaTest`.
- **Arquitectura:** ArchUnit y Spring Modulith sin cambios de dependencias entre módulos. Bouncy Castle solo en los adaptadores.
- **SPA:** Vitest de la subida y del informe; Playwright con axe-core, con aceptación y con rechazo.
- **Punta a punta:** `probar_documento_firmable.sh`, pasos 9 a 12.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Firma (proceso `distributivo`), la SPA, una herramienta y un script.

**Performance Goals**:
- SC-001: subir y ver el informe en menos de 1 minuto.
- La validación de un PDF de cientos de KB tarda milisegundos (el spike validó tres firmas en una prueba unitaria).

**Constraints**:
- **ADR-017:** la validación no se apaga en ningún entorno; lo que cambia es la lista de confianza.
- **Principio IX y Gitleaks:** los `.crt` solo se admiten en la CA de prueba. Las raíces públicas necesitan su excepción y el parche 2.2.1 de la constitución (research §5).
- **Principio VII:** `validacion_firma` es inmutable; los archivos nunca se sobrescriben.
- **Principio IX:**
  - ningún certificado ni PDF del autor en el repositorio;
  - las contraseñas de prueba son ficticias;
  - los textos del informe nunca llevan el mensaje de una excepción.
- **D-83:** la consulta a Usuarios va fuera de transacciones.
- **D-46:** el relleno de ceros, la llave pública para la integridad y la cadena dentro del CMS.

**Scale/Scope**:
- Firma:
  - 1 operación;
  - 1 tabla;
  - 1 puerto de entrada y 1 de salida nuevos;
  - 6 códigos de error;
  - 2 acciones de bitácora;
  - la lista de confianza.
- SPA: 1 botón con su informe.
- Herramienta: 1 programa de prueba.
- Script: 4 pasos.
- 1 decisión (D-103).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Qué se acepta y en qué orden, en el dominio (el informe y el mismo documento). PDFBox y Bouncy Castle, solo en un adaptador detrás de `ValidadorDeFirmas`. | ✅ |
| II. DDD | Firma es dueña del documento, sus firmas y sus validaciones. | ✅ |
| III. Lenguaje ubicuo | «Informe de validación», «comprobación», «firma nueva», «lista de confianza», «archivo vigente». | ✅ |
| IV. API primero | La operación, sus esquemas y los seis códigos se escriben antes del código. | ✅ |
| V. Límites | Firma pide la identificación por ruta interna (F14-H01). No lee tablas ajenas. La bitácora, por eventos. | ✅ |
| VI. Las reglas son datos | La lista de confianza es configuración por entorno, sin interruptores (ADR-017). | ✅ |
| VII. Inmutabilidad y trazabilidad | `validacion_firma` solo se inserta; cada archivo se guarda por su hash, sin sobrescribir; cada subida, aceptada o rechazada, queda en el informe y en la bitácora. | ✅ |
| VIII. Pruebas | Primero el dominio y la aplicación, viéndolos fallar; el adaptador con PDF firmados de verdad por la CA de prueba; integración con PostgreSQL; script de punta a punta. | ✅ |
| IX. Seguridad | El turno y las comprobaciones, en el servidor. 403 común. Ningún dato del certificado real en el repositorio. Textos fijos en el informe. Límite de tamaño. Las raíces públicas en su carpeta, con la excepción de Gitleaks solo para `.crt` y la enmienda de parche 2.2.1 (D-103). La CA de prueba solo en la configuración de demo. | ✅ con enmienda |
| X. Simplicidad | Una tabla para firmas y validaciones. Sin EU DSS, sin sello de tiempo ni revocación (ADR-007). | ✅ |
| XI. Diseño pragmático | La herramienta del script reutiliza las bibliotecas del proceso, sin dependencias nuevas. | ✅ |
| XII. JavaDoc | El orden de las comprobaciones y por qué; la fecha de la cadena; el relleno de ceros; el bloqueo; la lista de confianza. | ✅ |
| XIII. Errores | Seis códigos nuevos con sus 8 columnas. | ✅ |
| XIV. Validación | El uuid en la ruta, el archivo vacío y el tamaño, antes de validar. | ✅ |

**Post-diseño:** el análisis encontró que Gitleaks y el principio IX rechazan los `.crt` fuera de la CA de prueba. Se resuelve con la enmienda de parche 2.2.1, registrada en D-103 antes de implementar, junto con research §1, §3, §5 y §6.

## Project Structure

### Documentation (this feature)

```text
specs/048-validar-firma/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── firma.md
│   ├── spa.md
│   └── demostracion.md
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/firma/openapi.yaml               subirFirma, ResultadoValidacion
contratos/comun/errores.md                 6 códigos

backend/servicios/firma/
├── dominio/                               Comprobacion, Resultado, ResultadoDeComprobacion,
│                                          CertificadoDeLaFirma, ResultadoValidacion, FirmaRegistrada,
│                                          ComprobacionDeDocumento, FirmasDelDocumento (firmas),
│                                          ArchivoFirmadoRechazado, ArchivoVacio, ArchivoDemasiadoGrande
├── puertos/entrada/                       SubirFirma
├── puertos/salida/                        ValidadorDeFirmas (HallazgosDeLaFirma);
│                                          RepositorioDocumentos (firmas, registrarValidacion)
├── aplicacion/                            SubirFirmaServicio; FirmasPendientesServicio con las firmas
├── adaptadores/entrada/                   FirmasPendientesControlador (subir), ErroresFirma
├── adaptadores/salida/                    ValidadorPades, ListaConfianza, RepositorioDocumentosJdbc
├── infraestructura/                       ConfiguracionFirma (mdd.firma.raices)
├── src/main/resources/firma/raices/       ca-prueba.crt, bce-raiz.crt
└── src/main/resources/db/migration/firma/ V4__validacion_firma.sql

frontend/src/pantallas/firmas/             FirmasPendientes.tsx (subir e informe), consultas.ts
frontend/e2e/firmas-pendientes.spec.ts

deploy/herramientas/FirmarDePrueba.java    firma de prueba para los scripts
deploy/probar_documento_firmable.sh        pasos 9 a 12
deploy/probar_aprobacion.sh                la limpieza de validacion_firma
docs/decisiones.md                         D-103
.specify/memory/constitution.md            2.2.1 (principio IX)
.gitleaks.toml, .pre-commit-config.yaml    la excepción de firma/raices/ (solo .crt)
```

**Structure Decision**: Firma suma la subida a su controlador y a un servicio nuevo, como F14-H01 sumó la consulta. La validación criptográfica queda en un adaptador, para que el dominio no dependa de las bibliotecas.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| FirmaEC firma el PDF/A de F13-H02 de una forma que el spike no vio (por ejemplo, sin llenar el campo, o con otra estructura de revisión). | La validación cuenta diccionarios de firma, no campos (research §3). El autor lo comprueba con FirmaEC en `quickstart.md` §3, antes de fusionar. |
| PDFBox reescribe el PDF al firmar en la herramienta y rompe el prefijo. | La herramienta firma con `saveIncremental`, como `FirmadorDePrueba`. El adaptador lo prueba con PDF del generador real. |
| Bouncy Castle en el código de producción trae vulnerabilidades. | Es la misma versión del spike, revisada por Grype en la CI (SCA). |
| La raíz del BCE equivocada o reemplazada. | Su huella SHA-256 se anota en D-103 y una prueba la comprueba. |
| La herramienta del script no encuentra las bibliotecas en el contenedor. | Las copia de `/app/BOOT-INF/lib` del contenedor `distributivo` y falla con un mensaje claro si no están. |
| La limpieza de los scripts no borra `validacion_firma` y bloquea la de `generacion_documento`. | Borrarla primero en `probar_documento_firmable.sh` y `probar_aprobacion.sh`, y contarla en el paso 0 y al final. |
| Un archivo escrito sin registro si la transacción falla. | Es el caso de F13-H02: nombrado por su hash, no es vigente ni se entrega (research §4). |

## Complexity Tracking

Sin violaciones que justificar.
