# Implementation Plan: Iniciar una reforma de la versión publicada

**Branch**: `056-reforma-version-publicada` | **Date**: 2026-11-20 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/056-reforma-version-publicada/spec.md`

## Summary

**Iniciar la reforma** (research §1, §6):
- Una operación nueva de Validación OCS, `POST /api/validacionocs/periodos/{periodo}/reforma`, con el motivo, y el permiso `DISTRIBUTIVO:APROBACION`.
- El agregado, `ProcesoAprobacion.iniciarReforma`, exige PUBLICADO y el rol de la etapa EN_REVISION_VRA (por defecto, el Vicerrector), y registra una transición nueva, `REFORMA`, a EN_CONSOLIDACION con la versión nueva y el motivo.
- Dos códigos nuevos: `VALIDACIONOCS_ETAPA_NO_ADMITE_REFORMA` (409) y `VALIDACIONOCS_MOTIVO_DE_REFORMA_INVALIDO` (400).

**La versión nueva** (§2, §4): como D-106, el caso de uso valida, ordena a Generación la versión nueva y, con el proceso bloqueado, registra la transición, notifica y anota `REFORMA_INICIADA` en la bitácora.
- Generación suma el motivo `REFORMA`: la versión nueva copia todo y la publicada **sigue CONGELADA**, no se anula.
- Desde EN_CONSOLIDACION, el ciclo de siempre: devolver, consolidar, revisión, aprobación, la cadena desde F1, la resolución y la publicación.

**La publicada vigente** (§3): la última transición a PUBLICADO. Durante la reforma, la anterior; el estado del flujo suma `reforma` mientras esté en curso.

**Notificar** (§5): la `REFORMA` va al rol de la etapa de llegada y a DOCENTE, OPERADOR_DATOS y COORDINADOR_CARRERA, con un enlace por rol.

**La SPA** (§9): «Iniciar una reforma», con el motivo, en la tarjeta del flujo de «Consolidado del periodo», y la reforma en curso para quien lee el flujo.

**El script** (§10): `probar_documento_firmable.sh` reforma la versión 1 publicada con `identidad` detenido, lleva la versión 2 por el ciclo completo hasta publicarla, y comprueba que la 1 conserva sus firmas, su documento y su resolución.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash

**Primary Dependencies**: las que ya están, sin dependencias nuevas.

**Storage**: PostgreSQL.
- Validación OCS V8: el tipo `REFORMA` y la tabla `reforma`, con el motivo.
- Generación V9: el motivo `REFORMA` en `version_distributivo`.

**Testing**:
- **Dominio de Validación OCS:** `validarReforma` e `iniciarReforma` en PUBLICADO, en las demás etapas, con otro rol y con EN_CONSOLIDACION desactivada; las acciones en PUBLICADO; `reformaEnCurso`; `destinoDe` y los mensajes de la `REFORMA`; la publicación vigente durante la reforma.
- **Aplicación:** `IniciarReformaServicioTest`: el orden (validar, Generación, la transacción), un reintento sin versión doble, el rechazo sin pedir la versión, el motivo inválido, el periodo cerrado.
- **Generación:** la reforma deja la anterior CONGELADA, la idempotencia y la copia en PostgreSQL.
- **Integración:** dos reformas a la vez dejan una transición (`RepositorioProcesoIntegrationTest`); la tabla `reforma` (`ValidacionocsEsquemaIntegrationTest`).
- **Contrato:** `ReformaContratoTest`, `FlujoContratoTest`, `ErroresValidacionocsTest` y `PermisosValidacionocsTest`.
- **SPA:** Vitest del diálogo, la reforma en curso y los enlaces; Playwright con axe-core.
- **Punta a punta:** `probar_documento_firmable.sh` ([contracts/demostracion.md](contracts/demostracion.md)).

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Validación OCS y Generación (proceso `distributivo`), la SPA y un script.

**Performance Goals**: SC-004, la notificación antes de 2 minutos desde la vuelta de `identidad`; SC-005, la reforma en menos de 1 minuto.

**Constraints**:
- **RF-40 y el diseño de firmas:** la reforma crea una versión nueva con la cadena completa; la publicada queda histórica.
- **MD-04 y D-106:** Aprobación ordena a Generación; cada uno guarda en su esquema y su transacción.
- **D-107:** la resolución es única por versión.
- **D-108 y D-111:** el camino de las notificaciones y la publicación vigente.

**Scale/Scope**:
- Validación OCS: 1 operación, 1 caso de uso, 1 tipo de transición, 1 acción, 2 códigos, 1 acción de la bitácora, 1 migración.
- Generación: 1 motivo, el caso de uso que ya existe y 1 migración.
- SPA: 1 diálogo y 1 constancia.
- El script: 6 pasos nuevos, 2 que cambian y 1 modo de `MDD_CONSERVAR`.
- La decisión D-113.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | La regla de la reforma, las acciones y los destinos van en el agregado. Generación, la base, Notificaciones y la bitácora salen por puertos de salida. | ✅ |
| II. DDD | `iniciarReforma()` es de `ProcesoAprobacion`, y la versión nueva es de `VersionDistributivo` (glosario, MD-01, MD-04). | ✅ |
| III. Lenguaje ubicuo | «Reforma», «versión publicada», «histórica», `REFORMA` e `iniciarReforma`. | ✅ |
| IV. API primero | `iniciarReforma`, `SolicitudDeReforma`, `INICIAR_REFORMA`, `REFORMA` y `reforma` en `contratos/validacionocs/openapi.yaml`, antes del código. | ✅ |
| V. Límites | Aprobación usa la interfaz publicada de Generación que ya usa (D-106), con un motivo más. Sin llamadas nuevas entre procesos. | ✅ |
| VI. Las reglas son datos | Quién inicia es el rol de EN_REVISION_VRA de la configuración del periodo; la etapa de llegada sale de las etapas activas. | ✅ |
| VII. Inmutabilidad | La versión publicada no cambia ni se anula; la reforma crea otra. Las transiciones no se corrigen. | ✅ |
| VIII. Pruebas | Dominio, aplicación y Generación viéndolos fallar; PostgreSQL real con dos reformas a la vez y la copia; el script con la caída y el ciclo completo. | ✅ |
| IX. Seguridad | El permiso de la matriz y el rol de la etapa, en el servidor. El motivo no lleva datos personales por diseño; se valida su largo. | ✅ |
| X. Simplicidad | Reutiliza la copia de D-106 y el ciclo existente; sin un estado HISTORICA: la publicada vigente se deduce de las transiciones. | ✅ |
| XI. Diseño pragmático | La operación sigue el molde de la aprobación del contenido y del rechazo en firma. | ✅ |
| XII. JavaDoc | Por qué la publicada no se anula, cuál es la vigente, a quién se notifica y el orden con Generación. | ✅ |
| XIII. Errores | Dos códigos nuevos con sus 8 columnas. | ✅ |
| XIV. Validación | El motivo, de 10 a 500 caracteres en el servidor; el cuerpo, `additionalProperties: false`. | ✅ |

**Post-diseño:** sin cambios. D-113 queda registrada antes de implementar, con research §1 a §10.

## Project Structure

### Documentation (this feature)

```text
specs/056-reforma-version-publicada/
├── spec.md, plan.md, research.md, data-model.md, quickstart.md
├── contracts/ (validacionocs.md, generacion.md, spa.md, demostracion.md)
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/validacionocs/openapi.yaml, contratos/comun/errores.md

backend/servicios/validacionocs/
├── dominio/            ProcesoAprobacion (validarReforma, iniciarReforma, reformaEnCurso,
│                       acciones, destinoDe), Reforma, Transicion, TipoDeTransicion,
│                       AccionDelProceso, EtapaNoAdmiteReforma, MotivoDeReformaInvalido,
│                       MensajesDeTransicion
├── puertos/entrada/    IniciarReforma, FlujoDelDistributivo (reforma)
├── puertos/salida/     Distributivo (crearVersionNueva con el motivo), CambioDeFlujo.Accion
├── aplicacion/         IniciarReformaServicio, FlujoDelDistributivoServicio (cambio, vista)
├── adaptadores/entrada/ FlujoControlador, ErroresValidacionocs
├── adaptadores/salida/ DistributivoDesdeGeneracion, el repositorio del proceso (la tabla reforma)
├── infraestructura/    ConfiguracionValidacionocs (el bean)
└── resources/db/migration/validacionocs/V8__reforma.sql

backend/servicios/generacion/
├── puertos/entrada/    MotivoDeVersion (REFORMA), la firma de crearVersionNueva
├── aplicacion/         ParcialesDelDistributivoServicio (crearVersionNueva)
├── adaptadores/salida/ RepositorioDistributivosJdbc (sin anular con REFORMA)
└── resources/db/migration/generacion/V9__reforma.sql

frontend/src/pantallas/comun/ EstadoDelFlujo.tsx, DialogoDeReforma.tsx, flujo.ts (+ pruebas)
frontend/src/navegacion/enlaces.test.ts, frontend/e2e/consolidado.spec.ts
deploy/probar_documento_firmable.sh, .github/workflows/perfil-consolidado.yml (comentario)
docs/decisiones.md       D-113
```

**Structure Decision**: la reforma es de Aprobación, que es dueña del flujo; la versión nueva es de Generación, por orden de Aprobación. Firma y Notificaciones no cambian.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Un fallo entre la versión nueva de Generación y la transición deja la versión 2 creada con el proceso en PUBLICADO. | Generación no admite cambios en PUBLICADO; un reintento de la reforma devuelve la misma versión y completa la transición (idempotencia, como D-106). |
| La versión 2 se trata como la publicada antes de tiempo, o la 1 se anula. | La publicada vigente es la última transición a PUBLICADO; Generación no anula con `REFORMA`. Lo prueban el agregado, Generación en PostgreSQL y el paso 27 del script. |
| La resolución o la congelación de la versión 1 se muestran como si fueran de la 2. | El estado del flujo solo las muestra si son de la versión de trabajo. |
| F18-H01 probó «en PUBLICADO, ninguna acción»; ahora el Vicerrector tiene una. | Se actualizan sus pruebas y el paso 17 del script, con la razón en D-113: iniciar la reforma no edita la versión publicada. |
| El script se alarga con el ciclo completo de la versión 2 y otra caída de `identidad`. | Reutiliza los ayudantes de las firmas y la resolución; `esperar` termina en cuanto llega la notificación. El workflow tiene 45 minutos y hoy usa unos 25. |
| Los ayudantes de las firmas del script están atados a la versión 1. | Se generalizan por versión al implementar el paso 26, con el script de la versión 2 de F12-H04 como referencia. |

## Complexity Tracking

Sin violaciones que justificar.
