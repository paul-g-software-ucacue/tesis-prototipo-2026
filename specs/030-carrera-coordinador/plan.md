# Implementation Plan: Indicar la carrera del coordinador

**Branch**: `030-carrera-coordinador` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/030-carrera-coordinador/spec.md` (issue #166, F06-H05)

## Summary

- **El dato, en Roles:** una tabla con la carrera de cada coordinador, y dos operaciones para indicarla y quitarla. `asignarRoles` la borra al quitar el rol.
- **El token la lleva:** `consultarAutorizacion` suma `carrera`, y `EmisorJwt` la pone en un reclamo nuevo. Así vale desde la próxima renovación (D-62), sin consultas entre procesos.
- **El alcance, en plataforma:** `AutoridadesDelToken` marca el alcance y la carrera; `AlcanceDeCarrera` decide, sin conocer el token. Un coordinador sin carrera recibe `COMUN_CARRERA_SIN_INDICAR`.
- **Tres filtros:** `consultarOferta` y `consultarAsignaturaParalelo` en Académico; `consultarSugerencia` y `consultarAfinidadDelDocente` en Generación.
- **D-76:** Roles no valida la carrera contra Académico al guardar (research §2).
- **SPA:** la carrera en «Roles de usuarios». El aviso sale del catálogo.
- **Pruebas:** `deploy/probar_carrera_coordinador.sh`, con dos carreras reales. `probar_afinidad.sh` y `probar_sugerencia.sh` pasan a un usuario con alcance total.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · Bash

**Primary Dependencies**: ninguna nueva.

**Storage**: migración `V8__carrera_del_coordinador.sql` en el esquema `roles`, con la tabla nueva y las dos acciones nuevas del registro de cambios.

**Testing**:
- **Dominio y plataforma:** sin dobles; `AutoridadesDelToken`, con tokens de prueba.
- **Aplicación:** Mockito.
- **Repositorio:** Testcontainers, como el resto de Roles.
- **Contrato y permisos:** `ValidadorDeContrato` y `VerificadorDePermisos`.
- **SPA:** Vitest, y Playwright con axe.
- **Punta a punta:** el script, con dos carreras.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: un módulo de plataforma, 4 servicios, los contratos, la SPA y un script.

**Performance Goals**: la oferta filtrada, en menos de 3 s (SC-005, con holgura).

**Constraints**:
- el filtro siempre en el servidor (FR-014);
- ningún dato personal nuevo en el token: la carrera es un código;
- sin secretos nuevos ni cambios en el equipo (research §2);
- 60 % de líneas por módulo (principio VIII).

**Scale/Scope**:
- 2 operaciones nuevas, 1 ampliada entre módulos y 4 con filtro;
- 1 migración;
- 1 reclamo nuevo en el token;
- 3 códigos de error;
- 3 clases nuevas de plataforma y 1 ampliada;
- 1 script nuevo y 2 ajustados;
- 1 decisión (D-76) y 1 término del glosario.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | La regla del rol vive en la aplicación de Roles; el filtro de la oferta, en el dominio de Académico. El token solo se lee en `AutoridadesDelToken`. | ✅ |
| II. DDD según el subdominio | Roles y Académico son CRUD ricos; Generación recibe la carrera como un valor propio. | ✅ |
| III. Lenguaje ubicuo | «Carrera del coordinador» entró al glosario antes del código, aprobada el 2026-10-31 (research §8). | ✅ |
| IV. API primero | Los contratos antes del código. | ✅ |
| V. Límites | Roles guarda un código, sin llave foránea a Académico, como `periodo` en la designación. Nadie importa tipos de otro servicio. | ✅ |
| VI. Las reglas son datos | Sin reglas nuevas de normativa. | ✅ |
| VII. Inmutabilidad y trazabilidad | Cada cambio queda en `roles.registro_cambio`. | ✅ |
| VIII. Pruebas | Primero `AutoridadesDelToken`, `AlcanceDeCarrera` y la regla del rol. | ✅ |
| IX. Seguridad y datos personales | Mínimo privilegio: sin carrera no se ve nada. El filtro está en el servidor. | ✅ |
| X. Simplicidad | Un reclamo en el token evita consultar en cada petición. | ✅ |
| XI y XII | Sin módulos nuevos. JavaDoc en español. | ✅ |
| XIII. Errores | 3 códigos con sus 8 columnas, en «tú». | ✅ |
| XIV. Validación de las entradas | El código de carrera, con la regla de Académico, en el contrato y el servidor. En la pantalla se elige de una lista. | ✅ |
| §6.4 Decisiones | D-76, aprobada el 2026-10-31. | ✅ |

**Resultado del gate antes de la fase 0:** pasa. D-76 y el término del glosario quedaron aprobados por el autor el 2026-10-31.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/030-carrera-coordinador/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1
├── quickstart.md            # fase 1
├── contracts/
│   └── roles.md
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
backend/plataforma/seguridad/                     # AutoridadesDelToken marca el alcance y la carrera
backend/plataforma/errores-nucleo/                # AlcanceDeCarrera, CarreraSinIndicar y su código
backend/plataforma/errores/                       # ErroresDeAlcance
backend/servicios/roles/                          # la tabla, las operaciones y la regla del rol
backend/servicios/usuarios/                       # el reclamo carrera del token
backend/servicios/academico/                      # el filtro de la oferta; OfertaDelPeriodo.deCarrera
backend/servicios/generacion/                     # el filtro de la sugerencia y la afinidad
contratos/{roles,academico}/openapi.yaml, contratos/comun/errores.md
frontend/src/pantallas/roles/RolesDeUsuarios.tsx  # la carrera del coordinador
deploy/probar_carrera_coordinador.sh, deploy/probar_afinidad.sh, deploy/probar_sugerencia.sh
.github/workflows/perfil-consolidado.yml
docs/decisiones.md                                # D-76
```

**Structure Decision**: el dato es de Roles, porque ahí están los alcances (D-64). Cada servicio aplica su filtro, como manda D-64.

## Fases de implementación (guía para /speckit-tasks)

1. **Base:**
   - D-76 en `docs/decisiones.md`;
   - los contratos y los 3 códigos;
   - `AutoridadesDelToken` y `AlcanceDeCarrera`, probados sin dobles.
2. **Roles y el token:** la migración, las operaciones, la regla del rol y el reclamo.
3. **Los filtros:** Académico y Generación.
4. **SPA:** la carrera en «Roles de usuarios»; Vitest, y Playwright con axe.
5. **Punta a punta y cierre:**
   - `probar_carrera_coordinador.sh`, y los dos scripts ajustados;
   - la evidencia;
   - el PR;
   - la fusión con tu aprobación;
   - el despliegue;
   - tu comprobación, indicando la carrera con `admin.demo`;
   - el PR de evidencia con `Closes #166`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| Reclamo `carrera` en el token | Cada servicio filtra sin consultar a Roles en cada petición, y los cambios valen como los roles (D-62). | Preguntar a Roles por petición: una llamada más entre procesos en cada lectura. |
