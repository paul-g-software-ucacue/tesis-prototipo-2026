# Implementation Plan: Avanzar el distributivo según el rol de la etapa

**Branch**: `041-avanzar-etapas` | **Date**: 2026-11-07 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/041-avanzar-etapas/spec.md`

## Summary

Validación OCS (contexto Aprobación) suma el proceso de aprobación del distributivo: su etapa actual, el estado de envío de cada parcial, una copia de la configuración que regía al crearse el distributivo y el historial de transiciones. El coordinador envía el parcial de su carrera; Validación OCS le ordena a Generación marcarlo y calcular el hash de su contenido (por su interfaz publicada, en el mismo proceso), registra la transición y, si era el último, pasa a la siguiente etapa activa. Solo el rol de la etapa actual mueve el distributivo; el avance genérico vale solo para BORRADOR_UNIDADES. Cada transición deja una entrada en la bitácora, y Generación no duplica el cambio del parcial (D-95). La SPA muestra el estado y la acción de cada usuario en Oferta, Consolidado y Proyectos.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash · SQL (PostgreSQL)

**Primary Dependencies**: las que ya hay. Validación OCS suma `implementation project(':servicios:generacion')` para usar su interfaz publicada, como Usuarios con Roles. Ninguna librería nueva; el SHA-256 es del JDK.

**Storage**: Validación OCS `V3__proceso_aprobacion.sql` (`proceso_aprobacion`, `parcial_del_proceso`, `transicion`). Generación: sin migraciones; `parcial_carrera.estado` ya admite `ENVIADO`.

**Testing**:
- **Dominio** (sin dobles): `ProcesoAprobacion` (envío, último envío, siguiente etapa activa con etapas desactivadas, rol, carrera, ya enviado, avance genérico y etapas con operación propia); `ContenidoDelParcial` en Generación (hash fijo, sensible a un cambio, independiente del orden).
- **Aplicación** (Mockito): `EnviarParcialServicio` (orden de las dos transacciones, reintento idempotente, nada registrado si se rechaza), `AvanzarEtapaServicio`, la creación del proceso con la configuración vigente al crear el distributivo.
- **Adaptadores** (PostgreSQL real): `RepositorioProcesoJdbc` con el bloqueo (dos envíos simultáneos, un solo paso); `marcarParcialEnviado` en Generación.
- **Contrato:** `FlujoContratoTest` y `PermisosValidacionocsTest`.
- **Arquitectura:** Spring Modulith verifica que Validación OCS solo usa `generacion.puertos.entrada` y que Generación no depende de Validación OCS.
- **SPA:** Vitest de `EstadoDelFlujo` y de las tres pantallas; Playwright `flujo-envio.spec.ts` con axe-core.
- **Punta a punta:** `deploy/probar_avance.sh`.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: dos servicios del proceso `distributivo`, la SPA y un script.

**Performance Goals**: el envío responde con el estado nuevo, incluido el paso de etapa, en la misma respuesta (SC-003); un coordinador envía en menos de 30 s (SC-005).

**Constraints**:
- Una transacción por servicio (principio V): Generación marca primero, Validación OCS registra después; el reintento es idempotente (research §2).
- La configuración del proceso no cambia después de crearse (FR-003).
- Una entrada de bitácora por transición (D-95).
- El 403 no dice si faltó el permiso, el rol o la carrera.

**Scale/Scope**:
- 3 operaciones nuevas en el contrato de Validación OCS; 1 interfaz publicada nueva en Generación;
- 1 agregado y 3 tablas en Validación OCS;
- 5 códigos de error nuevos;
- 2 acciones nuevas en la bitácora;
- 1 componente y 3 pantallas en la SPA;
- 1 script nuevo;
- 1 decisión (D-95), ya registrada.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | `ProcesoAprobacion` y `ContenidoDelParcial` son dominio puro; el bloqueo y las tablas, adaptadores. | ✅ |
| II. DDD | Aprobación es central: un agregado con sus invariantes (rol, etapa, último envío), guiado por pruebas. | ✅ |
| III. Lenguaje ubicuo | `ProcesoAprobacion`, `Transicion`, `Etapa`, «parcial de carrera», «estado del distributivo» y «esqueleto fijo» del glosario §5 y §7. | ✅ |
| IV. API primero | Las tres operaciones en `contratos/validacionocs/openapi.yaml` antes del código; la interfaz de Generación documentada en `contracts/generacion.md`. | ✅ |
| V. Límites | Validación OCS no lee tablas de Generación: usa su interfaz publicada (MD-04). Una transacción por servicio, con reintento idempotente. Spring Modulith lo verifica. | ✅ |
| VI. Las reglas son datos | Las etapas, su orden activo y sus roles salen de la configuración del periodo, no del código. | ✅ |
| VII. Inmutabilidad y trazabilidad | Transiciones solo de inserción, con versión y hash; cada una en la bitácora (D-95). | ✅ |
| VIII. Pruebas | Primero el dominio, viéndolo fallar; 60 % por módulo; script de punta a punta. | ✅ |
| IX. Seguridad | Permiso en el contrato y rol de la etapa en el dominio, en el backend; 403 sin detalle. | ✅ |
| X. Simplicidad | Sin eventos nuevos entre servicios: la configuración vigente se reconstruye por fecha (research §5). Sin generar PDF. | ✅ |
| XI. Diseño pragmático | Una interfaz publicada nueva, que marca el límite real entre Aprobación y Distributivo. | ✅ |
| XII. JavaDoc | Puertos, agregado, interfaz publicada y servicios. | ✅ |
| XIII. Errores | 5 códigos nuevos con sus 8 columnas, cubiertos por la prueba de contrato. | ✅ |
| XIV. Validación | `periodo` y `carrera` con los patrones de los contratos existentes; las operaciones no tienen cuerpo. | ✅ |

**Post-diseño:** sin cambios. D-95 registrada antes de implementar.

## Project Structure

### Documentation (this feature)

```text
specs/041-avanzar-etapas/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── validacionocs.md
│   ├── generacion.md
│   ├── bitacora.md
│   ├── spa.md
│   └── demostracion.md
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/validacionocs/openapi.yaml     consultarFlujo, enviarParcial, avanzarEtapa
contratos/comun/errores.md               5 códigos nuevos

backend/servicios/generacion/
├── dominio/                             ContenidoDelParcial; VersionDistributivo.conParcialEnviado
├── puertos/entrada/                     ParcialesDelDistributivo, EstadoDelDistributivo, ParcialMarcado
├── aplicacion/                          ParcialesDelDistributivoServicio
└── adaptadores/salida/                  RepositorioDistributivosJdbc: marcar el parcial

backend/servicios/validacionocs/
├── dominio/                             ProcesoAprobacion, Transicion, EtapaDelProceso y sus errores
├── puertos/entrada|salida/              EnviarParcial, AvanzarEtapa, ConsultarFlujo; RepositorioProceso, Distributivo
├── aplicacion/                          EnviarParcialServicio, AvanzarEtapaServicio, ConsultarFlujoServicio
├── adaptadores/entrada/                 FlujoControlador; ErroresValidacionocs
├── adaptadores/salida/                  RepositorioProcesoJdbc, DistributivoDesdeGeneracion; CambioDeFlujo con 2 acciones
└── resources/db/migration/validacionocs/V3__proceso_aprobacion.sql

frontend/src/pantallas/comun/EstadoDelFlujo.tsx   y su uso en Oferta, Consolidado y Proyectos
frontend/e2e/flujo-envio.spec.ts

deploy/probar_avance.sh                  y su paso en perfil-consolidado
```

**Structure Decision**: no hay módulos nuevos.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Historia L con backend de dos servicios, SPA y script. | Las tareas van por historia de usuario: US1 (enviar) es el MVP; US2 (paso automático) y US3 (rol y avance genérico) se apoyan en el mismo agregado. La SPA puede ir al final. |
| Dos transacciones: un parcial enviado sin su transición si la segunda falla. | Reintento idempotente (research §2) y su prueba de aplicación. |
| La configuración por defecto cambia entre versiones del sistema. | Riesgo residual documentado (research §5). |
| La demo tiene un solo coordinador para 7 carreras. | El script y el quickstart le cambian la carrera con `admin.demo` (research §11). |

## Complexity Tracking

Sin violaciones que justificar.
