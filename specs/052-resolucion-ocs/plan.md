# Implementation Plan: Registrar la resolución del OCS

**Branch**: `052-resolucion-ocs` | **Date**: 2026-11-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/052-resolucion-ocs/spec.md`

## Summary

**Registrar es aprobar** (research §1): `POST …/resolucion` (multipart: número, fecha, PDF), con `RESOLUCION_OCS:ESCRITURA`.
- El servicio comprueba el PDF como el informe sustentado, sin abrir sus firmas (§3), y lo guarda por su hash en `resoluciones/` del volumen (§4).
- Con el proceso bloqueado, `ProcesoAprobacion.registrarResolucion()` exige el rol y la etapa EN_OCS y la fecha entre la entrada en EN_OCS y hoy (§2). Registra el paso a APROBADO con la resolución (Validación OCS V7, §5) y su entrada en la bitácora.

**El avance genérico desde EN_OCS** responde 409 `VALIDACIONOCS_REQUIERE_RESOLUCION` (§6).

**El estado del flujo** trae la resolución y la acción `REGISTRAR_RESOLUCION`; el PDF se descarga con el permiso del flujo (§7).

**SPA y script:** el formulario y la constancia en el estado del flujo (§9); `probar_documento_firmable.sh` suma tres pasos (§10).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash

**Primary Dependencies**: las que ya están. Sin dependencias nuevas.

**Storage**: PostgreSQL, Validación OCS V7 (`resolucion_ocs`). El volumen de archivos del proceso, `resoluciones/<sha256>.pdf`.

**Testing**:
- **Dominio:**
  - `ResolucionOcs` (número) y `ArchivoDeResolucion` (PDF y tamaño);
  - `ProcesoAprobacion.registrarResolucion`: rol, etapa, fecha en los bordes y fuera, y con la revisión desactivada;
  - `avanzar` desde EN_OCS, y `acciones`;
  - `Transicion` con resolución solo en EN_OCS → APROBADO.
- **Aplicación (Mockito):** `RegistrarResolucionServicio` (el archivo antes de la transacción; la transición, la resolución y la bitácora en ella; los rechazos sin guardar) y `DescargarResolucionServicio`.
- **Integración con PostgreSQL:** V7; el repositorio con la transición y su resolución; dos registros a la vez; el almacén en disco (idempotente, la huella al leer).
- **Contrato y permisos:** el 200, los 400, el 413, el 403, los 409 y el 404; la descarga; `resolucion` y la acción en el flujo.
- **SPA:** Vitest del formulario, la confirmación, los errores y la constancia con la descarga; Playwright con axe-core.
- **Punta a punta:** `probar_documento_firmable.sh`, pasos 13 a 15.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Validación OCS en el proceso `distributivo`, la SPA y un script.

**Performance Goals**: SC-003, el registro desde la pantalla en menos de 2 minutos.

**Constraints**:
- **D-19 y ADR-007:** no se abren ni se validan las firmas del PDF.
- **Principio V:** Validación OCS tiene su propia comprobación del PDF y su almacén; no usa clases de Generación ni de Firma.
- **Principio VII:** la resolución no cambia: una por versión aprobada.

**Scale/Scope**:
- Validación OCS: 1 tabla, 2 operaciones, 1 acción, 8 códigos, 1 almacén.
- SPA: 1 formulario, 1 constancia, 1 descarga.
- 3 pasos más en un script.
- D-107.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Las reglas del número, la fecha, la etapa y el rol, en el dominio; el archivo, por un puerto; la base y el disco, en adaptadores. | ✅ |
| II. DDD | `ProcesoAprobacion` decide si se aprueba; `ResolucionOcs` es su entidad (glosario). | ✅ |
| III. Lenguaje ubicuo | `ResolucionOcs`, `registrarResolucion()`, «Resolución del OCS», APROBADO y SECRETARIA_GENERAL, del glosario. | ✅ |
| IV. API primero | Las dos operaciones, los campos del flujo y los códigos, en el contrato antes del código. | ✅ |
| V. Límites | Todo en Validación OCS, con su tabla y su carpeta del volumen. | ✅ |
| VI. Las reglas son datos | El rol de EN_OCS sale de la copia de la configuración. | ✅ |
| VII. Inmutabilidad | La resolución y su archivo no cambian; el archivo se nombra por su hash. | ✅ |
| VIII. Pruebas | Dominio y aplicación, viéndolos fallar; la concurrencia con PostgreSQL real; el script. | ✅ |
| IX. Seguridad | Permiso y rol en el servidor; el archivo comprobado por su contenido y su tamaño; el nombre sin ruta; la huella al leer. | ✅ |
| X. Simplicidad | Sin evento hasta que alguien lo escuche (§8); reutiliza el patrón del informe y del rechazo. | ✅ |
| XI. Diseño pragmático | Pasos en el script de la cadena completa, en lugar de un script nuevo (§10). | ✅ |
| XII. JavaDoc | Registrar es aprobar, la fecha en la zona de la institución, por qué no se validan las firmas y el archivo por su hash. | ✅ |
| XIII. Errores | Ocho códigos nuevos con sus 8 columnas. | ✅ |
| XIV. Validación | Número, fecha y PDF definidos en la spec, en el dominio y en el contrato. | ✅ |

**Post-diseño:** sin cambios. D-107 queda registrada antes de implementar, con research §1 a §10.

## Project Structure

### Documentation (this feature)

```text
specs/052-resolucion-ocs/
├── spec.md, plan.md, research.md, data-model.md, quickstart.md
├── contracts/ (validacionocs.md, spa.md, demostracion.md)
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/validacionocs/openapi.yaml, contratos/comun/errores.md

backend/servicios/validacionocs/
├── dominio/            ResolucionOcs, ArchivoDeResolucion, sus excepciones, Transicion.resolucion,
│                       ProcesoAprobacion.registrarResolucion, AccionDelProceso.REGISTRAR_RESOLUCION
├── puertos/entrada/    RegistrarResolucion, DescargarResolucion, FlujoDelDistributivo.Resolucion
├── puertos/salida/     AlmacenDeResoluciones; CambioDeFlujo.Accion.RESOLUCION_REGISTRADA
├── aplicacion/         RegistrarResolucionServicio, DescargarResolucionServicio
├── adaptadores/        ResolucionControlador (o FlujoControlador), ErroresValidacionocs,
│                       AlmacenDeResolucionesEnDisco, RepositorioProcesoJdbc
└── resources/db/migration/validacionocs/V7__resolucion_ocs.sql

frontend/src/pantallas/comun/EstadoDelFlujo.tsx, flujo.ts; e2e/consolidado.spec.ts
deploy/probar_documento_firmable.sh
docs/decisiones.md      D-107
```

**Structure Decision**: todo en Validación OCS; la resolución va con su transición, como el rechazo en firma.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| El archivo se guarda y la transacción falla. | El nombre por hash: un reintento lo reutiliza y no queda referencia a un archivo sin resolución. |
| Dos registros a la vez. | El bloqueo del proceso: el segundo encuentra APROBADO y recibe el 409 de la etapa. Prueba con dos hilos. |
| La fecha cambia según la zona. | Se comparan días en `America/Guayaquil`; pruebas en los bordes del día. |
| El script de la cadena se alarga. | Tres pasos, sin preparación nueva; el workflow ya lo corre. |

## Complexity Tracking

Sin violaciones que justificar.
