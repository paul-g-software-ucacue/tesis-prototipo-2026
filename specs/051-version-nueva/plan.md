# Implementation Plan: Crear una versión nueva y anular las firmas tras un rechazo en firma

**Branch**: `051-version-nueva` | **Date**: 2026-11-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/051-version-nueva/spec.md`

## Summary

**Al recibir el rechazo en firma** (F14-H04), Aprobación hace tres pasos, como la aprobación de D-100 (research §1):
1. comprueba que el proceso sigue en firma con esa versión;
2. ordena a Generación crear la versión nueva;
3. con el proceso bloqueado, registra `RECHAZO_EN_FIRMA` y `VERSION_NUEVA` y guarda el aviso para Firma.

**Generación** anula la versión congelada y crea la siguiente, EDITABLE, con el motivo `RECHAZO_EN_FIRMA` (research §2, §3):
- copia en una transacción, con la versión bloqueada, los parciales, las propuestas, las actividades, las justificaciones, las consolidaciones y los informes;
- el informe comparte su archivo, que nunca se reescribe;
- es idempotente.

**Firma** recibe la orden por un oyente de Aprobación después de confirmar (research §5) y registra la anulación del documento en `anulacion_de_firmas` (V7). Las firmas se conservan. Firma publica `FirmasAnuladas` (research §6).

**Con `VERSION_NUEVA` al final del flujo, el bloqueo de D-105 deja de aplicar** sin cambiar su regla (research §4). El estado del flujo muestra el rechazo con la versión anulada y la nueva hasta que se apruebe otra vez (research §8).

**El script de F14-H04 cambia:** el paso 8 ya no espera el 409, sino que aprueba la versión 2 y ve F1 en turno (research §9).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash

**Primary Dependencies**: las que ya están. Sin dependencias nuevas.

**Storage**: PostgreSQL.
- Generación V8: el motivo, quién y cuándo de la versión, y el índice del informe.
- Firma V7: `anulacion_de_firmas`.
- Validación OCS V6: el tipo `VERSION_NUEVA`.

**Testing**:
- **Dominio:**
  - `ProcesoAprobacion.registrarVersionNueva`: después del rechazo, repetido, sin rechazo, con otra versión. `rechazoVigente`: hasta la congelación siguiente. `acciones` y `validarAprobacion` con la versión nueva;
  - `FirmasDelDocumento` sin turno con anulación;
  - `VersionDistributivo` y `MotivoDeVersion`.
- **Aplicación (Mockito):**
  - `ParcialesDelDistributivoServicio.crearVersionNueva`: los tres casos;
  - `RegistrarRechazoEnFirmaServicio`: fuera de firma no llama a Generación; las dos transiciones y el aviso; un reintento con la versión ya creada;
  - `AnularFirmasServicio`: anula, repetido, sin documento.
- **Integración con PostgreSQL:**
  - Generación: la copia completa tabla por tabla, la anulada intacta, el consolidado no desactualizado, el informe compartido, idempotente;
  - Firma V7 con el rol del proceso; `registrarAnulacion` y la subida a la vez;
  - Validación OCS V6;
  - `apps:distributivo`: rechazo en F2 → v2 y firmas anuladas; aprobar la v2 → documento v2 con F1 en turno.
- **Contrato:** `anulacion` en el estado del documento; `versionAnulada`, `versionNueva` y `VERSION_NUEVA` en el flujo.
- **SPA:** Vitest del aviso con y sin versión nueva; quitar la rama de `AvisoDeVersionCongelada`; Playwright sin cambios de flujo.
- **Punta a punta:** `probar_rechazo_en_firma.sh`, pasos 6, 8 y 9 cambiados.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Generación, Firma y Aprobación en el proceso `distributivo`, la SPA y un script.

**Performance Goals**: SC-001, la versión nueva y las firmas anuladas en menos de 30 s después del rechazo. El oyente corre en cuanto confirma la transacción.

**Constraints**:
- **MD-04:** Aprobación ordena la versión nueva. **MD-05:** Aprobación ordena la anulación; Firma no conoce el flujo.
- **Principio VII:** la versión anulada no cambia de contenido; las firmas no se tocan. La anulación es una fila nueva.
- **D-99:** en EN_REVISION_VRA el consolidado no cambia; la copia no compite con ediciones.

**Scale/Scope**:
- Generación: 1 migración, 1 operación de la interfaz publicada, 1 enum.
- Aprobación: 1 migración, 1 tipo de transición, 1 oyente, 1 acción de bitácora.
- Firma: 1 tabla, 1 caso de uso, 1 evento, 1 campo en el contrato.
- SPA: 1 texto y una rama menos.
- 1 script cambiado.
- D-106.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Las reglas de la versión y del flujo, en el dominio; la copia, en el adaptador; los avisos, por puertos. | ✅ |
| II. DDD | Generación decide si la versión se puede anular; Aprobación, cuándo pedirlo; Firma, cómo se anula un documento. | ✅ |
| III. Lenguaje ubicuo | `crearVersionNueva()`, `anularFirmas()`, `FirmasAnuladas`, ANULADA (glosario). El motivo «rechazo en firma» entra al glosario antes de implementar (T001). | ✅ |
| IV. API primero | Los campos nuevos del flujo y del documento, en los contratos antes del código. Crear y anular no son de la API. | ✅ |
| V. Límites | Cada servicio en su esquema y su transacción; Aprobación llama a las interfaces publicadas de Generación y Firma, y solo usa sus tipos de `puertos/entrada` (`MotivoDeVersion`, `MotivoDeAnulacion`). | ✅ |
| VI. Las reglas son datos | La etapa de llegada sigue saliendo de la copia de la configuración. | ✅ |
| VII. Inmutabilidad | La versión anulada solo cambia de estado; la anulación es una fila nueva, solo de inserción. | ✅ |
| VIII. Pruebas | Dominio y aplicación, viéndolos fallar; la copia y la anulación con PostgreSQL real; el script. | ✅ |
| IX. Seguridad | Ninguna operación nueva expuesta; las órdenes son internas. | ✅ |
| X. Simplicidad | Reutiliza el patrón de D-100 y D-101 y la regla de D-105; quita la rama que deja de alcanzarse. | ✅ |
| XI. Diseño pragmático | `INSERT … SELECT` en una transacción en lugar de leer y reescribir el agregado. | ✅ |
| XII. JavaDoc | El orden de los tres pasos, la idempotencia, la copia y la anulación. | ✅ |
| XIII. Errores | Ninguno nuevo (research §7). | ✅ |
| XIV. Validación | El motivo y los números, en el dominio y con `check` en las tablas. | ✅ |

**Post-diseño:** sin cambios. D-106 queda registrada antes de implementar, con research §1 a §9.

## Project Structure

### Documentation (this feature)

```text
specs/051-version-nueva/
├── spec.md, plan.md, research.md, data-model.md, quickstart.md
├── contracts/ (generacion.md, validacionocs.md, firma.md, spa.md, demostracion.md)
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/firma/openapi.yaml, contratos/validacionocs/openapi.yaml

backend/servicios/generacion/
├── puertos/entrada/    ParcialesDelDistributivo.crearVersionNueva, VersionNuevaEnGeneracion,
│                       MotivoDeVersion
├── puertos/salida/     RepositorioDistributivos.crearVersionNueva
├── aplicacion/         ParcialesDelDistributivoServicio
├── adaptadores/salida/ RepositorioDistributivosJdbc
└── resources/db/migration/generacion/V8__version_nueva.sql

backend/servicios/firma/
├── dominio/            AnulacionDeFirmas, FirmasDelDocumento.anulacion
├── puertos/entrada/    AnularFirmas, MotivoDeAnulacion, FirmasAnuladas, EstadoDelDocumento.anulacion
├── puertos/salida/     AvisoDeFirmasAnuladas; RepositorioDocumentos.registrarAnulacion
├── aplicacion/         AnularFirmasServicio
├── adaptadores/        AvisoDeFirmasAnuladasSpring, RepositorioDocumentosJdbc, DocumentoControlador
└── resources/db/migration/firma/V7__anulacion_de_firmas.sql

backend/servicios/validacionocs/
├── dominio/            TipoDeTransicion.VERSION_NUEVA, ProcesoAprobacion.registrarVersionNueva,
│                       rechazoVigente
├── puertos/salida/     Distributivo.crearVersionNueva, AvisoDeFirma.FirmasPorAnular,
│                       CambioDeFlujo.Accion.DISTRIBUTIVO_VERSION_NUEVA
├── aplicacion/         RegistrarRechazoEnFirmaServicio, FlujoDelDistributivoServicio (vista y bitácora)
├── adaptadores/        DistributivoDesdeGeneracion, AnulacionDeLasFirmas, FlujoControlador
└── resources/db/migration/validacionocs/V6__version_nueva.sql

frontend/src/pantallas/comun/EstadoDelFlujo.tsx, AvisoDeVersionCongelada.tsx, flujo.ts
deploy/probar_rechazo_en_firma.sh
docs/decisiones.md      D-106
```

**Structure Decision**: Aprobación coordina con las interfaces publicadas de Generación (llamada) y de Firma (oyente después de confirmar), como en D-100 y D-101. La dependencia sigue de Aprobación hacia los dos.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Generación crea la versión y la transacción de Aprobación falla. | El reenvío del aviso repite el paso 2, que devuelve la versión ya creada, y completa el 3 (research §1). Prueba con el fallo simulado. |
| La copia olvida una tabla de la versión. | Prueba de integración que compara tabla por tabla la anulada y la nueva; el script compara los totales. Una tabla nueva de la versión exige su copia (nota en el JavaDoc). |
| El consolidado de la versión nueva sale desactualizado. | La consolidación se copia con su momento y no se registran cambios; prueba de integración. |
| La clave única del informe impide copiarlo. | V8 la cambia a `(version_id, clave)`; el archivo no se reescribe ni se borra. |
| El script de F14-H04 esperaba el 409. | Se cambia el paso 8; las pruebas de F14-H04 que usan el 409 siguen valiendo en el dominio. |
| Una devolución desde EN_OCS más adelante necesitará lo mismo. | La anulación de Firma y la versión nueva no dependen del rechazo: reciben el motivo. |

## Complexity Tracking

| Lo que parece de más | Por qué se hace | Lo más simple que se descartó |
|---|---|---|
| `FirmasAnuladas` se publica y nadie lo escucha todavía (principio X) | Lo exigen el glosario (§6) y la decisión del autor; lo escuchará F16 para notificar. Cuesta un record y una línea en la transacción | No publicarlo: F16 tendría que volver a Firma a agregarlo |
