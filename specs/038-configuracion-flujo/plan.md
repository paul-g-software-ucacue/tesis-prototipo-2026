# Implementation Plan: Cargar la configuración del flujo y del árbol de firmas

**Branch**: `038-configuracion-flujo` | **Date**: 2026-11-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/038-configuracion-flujo/spec.md`

## Summary

Primera historia de `validacionocs`, hoy vacío. `ConfiguracionFlujo` valida etapas y firmas en el dominio, y un lector convierte el YAML sin interpretar nada peligroso. La plantilla es un recurso del servicio y también la configuración por defecto: un solo archivo. Cada carga guarda el texto original, solo por inserción. Académico da el estado del periodo y Roles los firmantes, por rutas internas nuevas. La SPA agrega la pantalla «Flujo y firmas».

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · Bash

**Primary Dependencies**: `snakeyaml`, que el repositorio ya usa en `servicios:usuarios`, sin versión propia: la fija Spring Boot.

**Storage**: `validacionocs`, `V2`: la tabla `configuracion_flujo`, solo de inserción. Roles y Académico no cambian de esquema.

**Testing**:
- **Dominio:** sin dobles, con 80 % o más en las reglas.
- **Lector:** los casos peligrosos, con archivos reales.
- **Aplicación:** Mockito.
- **Cliente entre procesos:** `ReceptorSimulado`.
- **Persistencia:** integración con la base real.
- **Contrato y permisos:** `ValidadorDeContrato` y `VerificadorDePermisos`.
- **SPA:** Vitest, con `npm run tipos`, y Playwright con axe.
- **Punta a punta:** el script, con los procesos reales.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: 3 servicios, 3 contratos, la SPA y un script.

**Performance Goals**: consultar y cargar en menos de 3 s (SC-003), con holgura.

**Constraints**:
- es la primera vez que `validacionocs` tiene contrato, controlador y catálogo de errores;
- `distributivo` llama por primera vez a `identidad`, por `/interno`;
- el archivo lo escribe una persona: los mensajes dicen qué corregir, sin detalles del lector;
- solo FLUJO_FIRMAS: la matriz D-16 no cambia.

**Scale/Scope**:
- 3 operaciones públicas y 2 rutas internas nuevas;
- 1 migración;
- 5 códigos de error nuevos;
- 1 pantalla nueva, 1 script nuevo;
- 1 decisión (D-89) y 1 término en el glosario.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Las reglas son dominio puro; YAML vive en un adaptador. | ✅ |
| II. DDD según el subdominio | `ConfiguracionFlujo` es un agregado, como dice el modelo táctico. | ✅ |
| III. Lenguaje ubicuo | «Configuración por defecto» entra al glosario antes del código. | ✅ con T001 |
| IV. API primero | El contrato nuevo antes del código. | ✅ |
| V. Límites | Académico y Roles por rutas internas: no se leen sus esquemas. | ✅ |
| VI. Las reglas son datos | Etapas, roles, plazos y firmas están en el archivo. El esqueleto es fijo por ADR-007. | ✅ |
| VII. Inmutabilidad y trazabilidad | Solo inserción, con quién y cuándo. | ✅ |
| VIII. Pruebas | Primero el dominio, con esqueleto, viéndolo fallar. | ✅ |
| IX. Seguridad y datos personales | Lector seguro, sin anclas ni tipos; hasta 1 MB. | ✅ |
| X. Simplicidad | Se guarda el texto: sin tablas de etapas ni de firmas. | ✅ |
| XI y XII | Sin módulos nuevos: `validacionocs` ya existe. JavaDoc en español. | ✅ |
| XIII. Catálogo de errores | 5 códigos nuevos con sus 8 columnas. | ✅ |
| XIV. Validación de entradas | Tamaño y estructura en el servidor; tamaño también en la interfaz. | ✅ |

## Project Structure

### Documentation (this feature)

```text
specs/038-configuracion-flujo/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── validacionocs.md
│   ├── academico.md
│   └── roles.md
└── tasks.md
```

### Source Code (repository root)

```text
contratos/validacionocs/openapi.yaml    nuevo
contratos/academico/openapi.yaml        estado interno del periodo
contratos/roles/openapi.yaml            firmantes
contratos/comun/errores.md              5 códigos

backend/servicios/validacionocs/
├── dominio/                    ConfiguracionFlujo, Etapa, PasoFirma, TipoDeEtapa
├── aplicacion/                 consultar y cargar
├── puertos/                    los casos de uso, el lector y los datos externos
├── adaptadores/                ConfiguracionControlador, LectorYaml, cliente, repositorio
└── resources/                  V2 y flujo-por-defecto.yaml

backend/servicios/academico/, backend/servicios/roles/   sus rutas internas

frontend/src/pantallas/flujo/
deploy/probar_flujo.sh          punta a punta
```

**Structure Decision**: no hay módulos nuevos.

## Complexity Tracking

Sin violaciones que justificar.
