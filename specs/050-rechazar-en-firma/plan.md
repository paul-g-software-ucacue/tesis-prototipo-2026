# Implementation Plan: Rechazar en firma con una observación

**Branch**: `050-rechazar-en-firma` | **Date**: 2026-11-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/050-rechazar-en-firma/spec.md`

## Summary

**Firma registra el rechazo** como la operación `rechazarEnFirma()` del documento (glosario §5, research §1):
- solo el firmante en turno, con la regla de F14-H01; a los demás, el 403 común;
- la observación de 10 a 500 caracteres, después del turno (research §3);
- una fila en `firma.rechazo_en_firma` (V6), con la generación bloqueada como en la subida (research §2). Con ella, nadie está en turno: el documento queda cerrado.

**En la misma transacción, Firma publica `RechazoEnFirma`** (research §4). Aprobación lo escucha en `CierreDeLaFirma` después de confirmar y pasa el distributivo de CONGELADO_EN_FIRMA a la etapa activa anterior, EN_REVISION_VRA por defecto, como `sistema` e idempotente (research §5).

**Aprobar otra vez se bloquea** con `VALIDACIONOCS_REQUIERE_VERSION_NUEVA` mientras la última transición sea el rechazo (research §6). El estado del flujo muestra la observación (research §7).

**SPA y script:** «Rechazar» en «Pendientes de firma» y el aviso en el estado del flujo (research §8); un script nuevo de punta a punta (research §9).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash

**Primary Dependencies**: las que ya están. Sin dependencias nuevas.

**Storage**: PostgreSQL. Firma V6 (`rechazo_en_firma`). Validación OCS V5 (`rechazo_en_firma` y el tipo de transición).

**Testing**:
- **Dominio:**
  - `RechazoEnFirma`: la observación vacía, corta, larga y con espacios en los extremos;
  - `FirmasDelDocumento`: con rechazo no hay turno; el constructor de cuatro componentes sigue igual;
  - `ProcesoAprobacion.rechazarEnFirma`: en firma, repetido, en otra etapa, con otra versión y con EN_REVISION_VRA desactivada; `validarAprobacion`, `validarRevision` y `acciones` con un rechazo pendiente;
  - `Transicion` con rechazo solo en `RECHAZO_EN_FIRMA`.
- **Aplicación (Mockito):**
  - `RechazarEnFirmaServicio`: fuera de turno sin mirar la observación; la observación inválida sin registrar; el registro, la bitácora y el aviso en la transacción; perdido contra una firma (403);
  - `SubirFirmaServicio`: aceptada después de un rechazo, registrada como tardía;
  - `RegistrarRechazoEnFirmaServicio`.
- **Adaptadores e integración:**
  - V6 y V5 con el rol del proceso (sin `update` ni `delete`);
  - `RepositorioDocumentosJdbc`: el rechazo y la subida a la vez, una sola gana;
  - el repositorio de Aprobación con la transición y su rechazo;
  - en `apps:distributivo`, F1 aceptada, el rechazo de F2 y EN_REVISION_VRA, y el aviso repetido sin duplicar.
- **Contrato y permisos:** el 201, los dos 400 y el 403 del rechazo; el 409 de la aprobación; `rechazoEnFirma` en el flujo.
- **SPA:** Vitest del diálogo y del aviso del flujo; Playwright con axe-core.
- **Punta a punta:** `probar_rechazo_en_firma.sh`, 10 pasos.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Firma y Aprobación en el proceso `distributivo`, la SPA y un script.

**Performance Goals**: SC-002, EN_REVISION_VRA en menos de 30 s después del rechazo; el aviso se entrega en cuanto confirma la transacción. SC-003, el rechazo desde la pantalla en menos de 1 minuto.

**Constraints**:
- **MD-05:** Firma no conoce el flujo; la dependencia sigue de Aprobación hacia Firma.
- **Principio VII:** el rechazo y la transición no se reescriben; las firmas aceptadas se conservan (FR-007).
- **F12-H04:** ni versión nueva ni anulación aquí.

**Scale/Scope**:
- Firma: 1 tabla, 1 caso de uso, 1 evento, 1 código.
- Aprobación: 1 tabla, 1 tipo de transición, 1 caso de uso, 1 oyente, 1 código.
- SPA: 1 diálogo y 1 aviso.
- 1 script nuevo en el workflow.
- D-105.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | La observación y el cierre, en el dominio; el aviso, por un puerto; la base, en adaptadores. | ✅ |
| II. DDD | Firma decide si el documento admite el rechazo; Aprobación, a qué etapa vuelve. | ✅ |
| III. Lenguaje ubicuo | `RechazoEnFirma`, `rechazarEnFirma()`, `Observacion` (glosario §5, §6, §7). «Rechazo en firma» no es «archivo firmado rechazado» ni «devolución» (§9). | ✅ |
| IV. API primero | El endpoint, los códigos y el campo del flujo, en el contrato antes del código. | ✅ |
| V. Límites | El evento va en la API publicada de Firma y Aprobación lo escucha (MD-05). | ✅ |
| VI. Las reglas son datos | La etapa de llegada sale de la copia de la configuración (la anterior activa). | ✅ |
| VII. Inmutabilidad | Dos tablas solo de inserción; ninguna firma se toca. | ✅ |
| VIII. Pruebas | Dominio y aplicación, viéndolos fallar; la concurrencia y la cadena con PostgreSQL real; el script. | ✅ |
| IX. Seguridad | Turno, observación y estado en el servidor; el 403 común no dice nada del documento; SQL parametrizado. | ✅ |
| X. Simplicidad | Reutiliza el bloqueo, la regla de la subida tardía y el patrón del aviso de F14-H03. | ✅ |
| XI. Diseño pragmático | Un script aparte en vez de ramificar el de la cadena completa (research §9). | ✅ |
| XII. JavaDoc | El cierre del documento, la carrera con la subida, la etapa de llegada y el bloqueo de aprobar. | ✅ |
| XIII. Errores | Dos códigos nuevos con sus 8 columnas. | ✅ |
| XIV. Validación | La observación de 10 a 500, en el dominio y en el contrato. | ✅ |

**Post-diseño:** sin cambios. D-105 queda registrada antes de implementar, con research §1 a §9.

## Project Structure

### Documentation (this feature)

```text
specs/050-rechazar-en-firma/
├── spec.md, plan.md, research.md, data-model.md, quickstart.md
├── contracts/ (firma.md, validacionocs.md, spa.md, demostracion.md)
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/firma/openapi.yaml, contratos/validacionocs/openapi.yaml, contratos/comun/errores.md

backend/servicios/firma/
├── dominio/            RechazoEnFirma, ObservacionInvalida, FirmasDelDocumento.rechazo
├── puertos/entrada/    RechazarEnFirma, RechazoEnFirma (evento), RechazoRegistrado
├── puertos/salida/     AvisoDeRechazoEnFirma; RepositorioDocumentos.registrarRechazo
├── aplicacion/         RechazarEnFirmaServicio
├── adaptadores/entrada/DocumentoControlador (rechazos), ErroresFirma
├── adaptadores/salida/ AvisoDeRechazoEnFirmaSpring, RepositorioDocumentosJdbc
└── src/main/resources/db/migration/firma/V6__rechazo_en_firma.sql

backend/servicios/validacionocs/
├── dominio/            TipoDeTransicion.RECHAZO_EN_FIRMA, RechazoDelFirmante, Transicion.rechazo,
│                       ProcesoAprobacion.rechazarEnFirma, RequiereVersionNueva
├── puertos/entrada/    RegistrarRechazoEnFirma
├── aplicacion/         RegistrarRechazoEnFirmaServicio
├── adaptadores/entrada/CierreDeLaFirma.alRechazar, FlujoControlador, ErroresValidacionocs
└── src/main/resources/db/migration/validacionocs/V5__rechazo_en_firma.sql

frontend/src/pantallas/firmas/FirmasPendientes.tsx, src/pantallas/comun/EstadoDelFlujo.tsx,
e2e/firmas-pendientes.spec.ts
deploy/probar_rechazo_en_firma.sh, .github/workflows/perfil-consolidado.yml
docs/decisiones.md      D-105
```

**Structure Decision**: Firma suma el rechazo y su aviso; Aprobación, la vuelta a revisión y el bloqueo. La dependencia entre los dos no cambia de sentido.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| El rechazo y una subida del mismo paso a la vez dejan los dos. | El bloqueo de la generación y la comprobación de firmas y rechazo al registrar los dos; la clave primaria de V6. Prueba de integración con dos hilos. |
| El aviso se pierde entre confirmar y entregar. | El registro de publicaciones lo reenvía (ADR-018); `rechazarEnFirma` es idempotente. |
| Un distributivo queda en EN_REVISION_VRA sin salida hasta F12-H04. | Es lo decidido (Clarifications). La demo no llega a firma; el script lo deja limpio. Se anota en D-105. |
| Cambiar el constructor de `FirmasDelDocumento` o de `Transicion` rompe muchas pruebas. | Se conservan los constructores de antes, sin rechazo. |
| La etapa activa anterior es BORRADOR_CARRERA si una copia desactiva las dos opcionales. | Con todos los parciales enviados, nadie tiene acciones hasta F12-H04, como en EN_REVISION_VRA; se anota como límite en D-105. La demo tiene EN_REVISION_VRA activa. |

## Complexity Tracking

Sin violaciones que justificar.
