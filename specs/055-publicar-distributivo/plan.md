# Implementation Plan: Publicar el distributivo aprobado

**Branch**: `055-publicar-distributivo` | **Date**: 2026-11-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/055-publicar-distributivo/spec.md`

## Summary

**Publicar** (research §1, §6, §7):
- Una operación nueva de Validación OCS, `POST /api/validacionocs/periodos/{periodo}/publicacion`, con el permiso `PUBLICACION:APROBACION`.
- El agregado, `ProcesoAprobacion.publicar`, exige APROBADO y el rol de esa etapa, y registra un `PASO_DE_ETAPA` a PUBLICADO con la versión vigente.
- El caso de uso bloquea el proceso y, en una transacción, guarda la transición, notifica y deja `DISTRIBUTIVO_PUBLICADO` en la bitácora.
- Fuera de APROBADO, el 409 nuevo `VALIDACIONOCS_ETAPA_NO_ADMITE_PUBLICACION`.

**Bloquear** (§2): la versión publicada es la congelada que llegó a APROBADO, ya inmutable (D-100). En PUBLICADO el flujo no ofrece acciones a nadie, y el estado del flujo suma la constancia `publicacion`.

**Notificar** (§3, §4): el paso a PUBLICADO va a tres destinos por rol, DOCENTE, OPERADOR_DATOS y COORDINADOR_CARRERA, con el camino de D-108.
- Una sola notificación por usuario, por la clave única de Notificaciones; nunca a quien publicó.
- Título «El distributivo de {periodo} fue publicado» y enlace al inicio.

**La SPA** (§5): «Publicar el distributivo» con su confirmación en la tarjeta del flujo de «Resolución del OCS», la constancia para quien lee el flujo, y «publicada» en el aviso de «Consolidado del periodo».

**El script** (§8): `probar_documento_firmable.sh` publica después de la resolución, con la caída de `identidad` movida del envío a la publicación, y comprueba que la versión publicada no ofrece acciones.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash

**Primary Dependencies**: las que ya están, sin dependencias nuevas.

**Storage**: PostgreSQL, sin migraciones. La transición a PUBLICADO va en `validacionocs.transicion`, como las demás.

**Testing**:
- **Dominio de Validación OCS:**
  - `publicar` en APROBADO, en las demás etapas y con otro rol;
  - las acciones en APROBADO y en PUBLICADO;
  - `destinoDe` y los mensajes del paso a PUBLICADO.
- **Aplicación:** `PublicarDistributivoServicioTest`:
  - la transición, la notificación y la bitácora en la transacción;
  - un rechazo no notifica ni registra;
  - el periodo cerrado y sin distributivo.
- **Integración:** dos publicaciones a la vez dejan una transición (`RepositorioProcesoIntegrationTest`).
- **Contrato:** `PublicarContratoTest`, `FlujoContratoTest`, `ErroresValidacionocsTest` y `PermisosValidacionocsTest`.
- **SPA:** Vitest del diálogo, la constancia, el aviso y los enlaces; Playwright con axe-core.
- **Punta a punta:** `probar_documento_firmable.sh` ([contracts/demostracion.md](contracts/demostracion.md)).

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Validación OCS (proceso `distributivo`), la SPA y un script.

**Performance Goals**: SC-003, la notificación antes de 2 minutos desde la vuelta de `identidad`; SC-005, la publicación en menos de 30 s.

**Constraints**:
- **MD-11:** publicar es de Aprobación; el CSV y la consulta son de F19.
- **ADR-007:** APROBADO y PUBLICADO son del esqueleto fijo, con su operación propia.
- **D-100:** la versión congelada no cambia.
- **D-108:** el camino de las notificaciones.

**Scale/Scope**:
- Validación OCS: 1 operación, 1 caso de uso, 1 regla del agregado, 1 acción, 1 código y 1 acción de la bitácora.
- SPA: 1 diálogo, 1 constancia y 1 aviso.
- El script: 2 pasos y la caída movida.
- La decisión D-111.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | La regla de publicar, las acciones y los destinos van en el agregado. La base, Notificaciones y la bitácora salen por puertos de salida. | ✅ |
| II. DDD | Publicar es una transición de Aprobación (MD-11, glosario `publicar()`). Publicación y consulta solo leerá. | ✅ |
| III. Lenguaje ubicuo | «Publicar», «versión publicada», PUBLICADO, `DistributivoPublicado` y «notificación» (MD-09). | ✅ |
| IV. API primero | `publicarDistributivo`, `PUBLICAR` y `publicacion` en `contratos/validacionocs/openapi.yaml`, antes del código. | ✅ |
| V. Límites | Sin llamadas nuevas a otros servicios. La notificación va por el evento de D-108. Generación no cambia. | ✅ |
| VI. Las reglas son datos | Quién publica es el rol de la etapa APROBADO en la configuración del periodo. | ✅ |
| VII. Inmutabilidad | La versión publicada ya es inmutable, y la transición no se corrige. Cambiar después es una reforma (F18-H02). | ✅ |
| VIII. Pruebas | Dominio y aplicación viéndolos fallar; PostgreSQL real con dos publicaciones a la vez; el script con la caída. | ✅ |
| IX. Seguridad | El permiso de la matriz y el rol de la etapa, en el servidor. La notificación no lleva datos personales. | ✅ |
| X. Simplicidad | Sin tablas, sin estado nuevo en Generación y sin otra caída en el script. | ✅ |
| XI. Diseño pragmático | La operación sigue el molde de registrar la resolución. | ✅ |
| XII. JavaDoc | Por qué publica Aprobación, qué es la versión publicada y a quién se notifica. | ✅ |
| XIII. Errores | Un código nuevo con sus 8 columnas. | ✅ |
| XIV. Validación | Sin entradas nuevas: la operación no lleva cuerpo, y el periodo tiene su patrón. | ✅ |

**Post-diseño:** sin cambios. D-111 queda registrada antes de implementar, con research §1 a §8.

## Project Structure

### Documentation (this feature)

```text
specs/055-publicar-distributivo/
├── spec.md, plan.md, research.md, data-model.md, quickstart.md
├── contracts/ (validacionocs.md, spa.md, demostracion.md)
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/validacionocs/openapi.yaml, contratos/comun/errores.md

backend/servicios/validacionocs/
├── dominio/            ProcesoAprobacion (publicar, publicacion, acciones, destinoDe),
│                       AccionDelProceso, EtapaNoAdmitePublicacion, MensajesDeTransicion
├── puertos/entrada/    PublicarDistributivo, FlujoDelDistributivo (publicacion)
├── puertos/salida/     CambioDeFlujo.Accion (DISTRIBUTIVO_PUBLICADO)
├── aplicacion/         PublicarDistributivoServicio, FlujoDelDistributivoServicio (cambio)
├── adaptadores/entrada/ PublicacionControlador, FlujoControlador (publicacion), ErroresValidacionocs
└── infraestructura/    ConfiguracionValidacionocs (el bean)

frontend/src/pantallas/comun/ EstadoDelFlujo.tsx, DialogoDePublicacion.tsx (+ pruebas)
frontend/src/pantallas/consolidado/Consolidado.tsx, pantallas/inconsistencias (el aviso)
frontend/src/navegacion/enlaces.test.ts, frontend/e2e/resolucion.spec.ts
deploy/probar_documento_firmable.sh, .github/workflows/perfil-consolidado.yml (comentario)
docs/decisiones.md       D-111
```

**Structure Decision**: todo lo nuevo es de Aprobación, que es dueña del flujo. Notificaciones y Generación no cambian.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Una pantalla sigue ofreciendo una acción de edición con la versión publicada. | La versión ya está congelada desde D-100, y las acciones del flujo salen vacías en PUBLICADO. El paso 17 del script prueba cada operación de cambio y el estado del flujo de cada usuario. |
| Mover la caída de `identidad` deja sin probar el envío a revisión con `identidad` caído. | Envío y publicación usan el mismo camino de Validación OCS (D-108). El cambio queda en D-111 y en la evidencia. |
| Una segunda Secretaría no se entera de la publicación. | El estado del flujo muestra la constancia a todos. El issue solo pide a los tres roles. |
| El enlace al inicio no muestra el distributivo publicado. | Es provisorio hasta F19, que trae la consulta. Queda en D-111. |
| Quien tiene dos roles recibe dos notificaciones. | La clave única de Notificaciones por evento, destinatario y carrera, con los tres destinos sin carrera. Lo prueba el script con `docente.coordinador.demo`. |

## Complexity Tracking

Sin violaciones que justificar.
