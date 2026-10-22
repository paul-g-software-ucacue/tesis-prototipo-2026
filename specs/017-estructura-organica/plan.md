# Implementation Plan: Mantener la estructura orgánica

**Branch**: `017-estructura-organica` | **Date**: 2026-10-21 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/017-estructura-organica/spec.md` (issue #35, F03-H01)

## Summary

- **La estructura orgánica en Académico:** unidades con código, nombre, tipo, padre y estado. El dominio guarda las reglas de cada tipo, de la jerarquía (padre activo, sin ciclos, una sede matriz) y de la inactivación (sin dependientes activos). Ninguna unidad se borra.
- **Las 21 gestiones autorizadas** de POL 6 llegan precargadas como dato, y nadie registra una gestión fuera de ellas (aclaración 2).
- **Las comisiones** quedan como dato informativo, con su «permanente» y sin gestión ni horas.
- **Permisos:** escribe el Administrador con `CATALOGOS:ESCRITURA`; leen quienes tienen `DISTRIBUTIVO:LECTURA` o `CATALOGOS:LECTURA`. Para eso, una operación puede declarar varios permisos alternativos: es **D-70 (propuesta)**.
- **Contrato:** 6 operaciones y 3 códigos de error `ACADEMICO_*`.
- **SPA:** la pantalla «Estructura orgánica», en árbol.
- **Punta a punta:** `deploy/probar_estructura.sh`, nuevo, con las 24 unidades de la carga 01 de prueba.

Los detalles y las alternativas están en [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · SQL (PostgreSQL 18, Flyway) · Bash

**Primary Dependencies**: sin dependencias nuevas.

**Storage**: esquema `academico`, `V3__estructura_organica.sql`: `gestion_autorizada` (con los 21), `unidad_organica` y las 4 acciones nuevas del registro ([data-model.md](data-model.md)).

**Testing**: JUnit y AssertJ en el dominio, sin dobles; Mockito en la aplicación; Testcontainers en los adaptadores; swagger-request-validator en el contrato; Vitest y Playwright con axe; `probar_estructura.sh`.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Académico (`datos`), `plataforma:pruebas` (el verificador de permisos), el contrato, la SPA y un script.

**Performance Goals**: listar y guardar por debajo de 200 ms con decenas de unidades (la estructura se carga entera para validar); SC-001: registrar una gestión en menos de 1 minuto.

**Constraints**:
- el catálogo de gestiones es un dato, no código (principio VI);
- toda acción deja registro; nada se borra (principio VII);
- escribe solo `CATALOGOS:ESCRITURA` (principio IX, aclaración 1);
- 60 % de líneas por módulo.

**Scale/Scope**: 2 tablas y 21 filas precargadas; 6 operaciones y 3 códigos; 1 pantalla; 1 script.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Reglas en `UnidadOrganica` y `EstructuraOrganica`; la aplicación orquesta; los adaptadores traducen. | ✅ |
| II. DDD según el subdominio | Académico es de soporte: valores con sus reglas, sin agregado elaborado ni eventos (research §1). | ✅ |
| III. Lenguaje ubicuo | Sede, Unidad, Gestión autorizada y Comisión, del glosario; la frase de «Unidad» se propone antes del código (research §9). | ⏳ aprobación del autor |
| IV. API primero | Las 6 operaciones y los 3 códigos en el contrato antes del código. | ✅ |
| V. Límites | Todo en Académico; las historias que lo usen (F03-H02, F03-H04, F01) lo consultarán por su API. | ✅ |
| VI. Las reglas son datos | Las 21 gestiones son filas, no un enum. | ✅ |
| VII. Inmutabilidad y trazabilidad | Nada se borra; cada cambio en `registro_cambio`. | ✅ |
| VIII. Pruebas | Primero las pruebas del dominio, sin dobles, con la carga 01 de prueba como caso grande. | ✅ |
| IX. Seguridad | Escribe solo `CATALOGOS:ESCRITURA`; la lectura, con dos permisos alternativos validados en el servidor (D-70). | ⏳ aprobación del autor |
| X. Simplicidad | La estructura se carga entera para validar; sin consultas recursivas. | ✅ |
| XI y XII | Sin abstracciones nuevas; JavaDoc en español. | ✅ |
| XIII. Errores | 3 códigos en el catálogo; los datos inválidos, 400 con su campo. | ✅ |
| §6.4 Decisiones | D-70 (permisos alternativos en `x-mdd-permiso`), propuesta en research §4. | ⏳ aprobación del autor |

**Resultado del gate antes de la fase 0:** pasa, con D-70 y la frase del glosario pendientes de la aprobación del autor antes de las tareas.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/017-estructura-organica/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1
├── quickstart.md            # fase 1
├── contracts/
│   └── academico.md         # operaciones, esquemas y errores
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
contratos/academico/openapi.yaml               # 6 operaciones
contratos/comun/errores.md                     # 3 códigos
backend/plataforma/pruebas/…/VerificadorDePermisos.java   # x-mdd-permiso como lista (D-70)
backend/servicios/academico/src/main/java/ec/mdd/academico/
├── dominio/                                   # TipoDeUnidad, GestionAutorizada, UnidadOrganica, EstructuraOrganica
├── puertos/{entrada,salida}/                  # ConsultarEstructura, MantenerEstructura, RepositorioEstructura
├── aplicacion/                                # servicios
└── adaptadores/                               # EstructuraControlador, RepositorioEstructuraJdbc
backend/servicios/academico/src/main/resources/db/migration/academico/V3__estructura_organica.sql
frontend/src/pantallas/estructura/             # «Estructura orgánica»
frontend/src/navegacion/Marco.tsx, frontend/src/sesion/RutaConPermiso.tsx   # varios permisos alternativos
deploy/probar_estructura.sh, .github/workflows/perfil-consolidado.yml
docs/decisiones.md                             # D-70
```

**Structure Decision**: la estructura es de Datos maestros, en Académico, junto a los periodos.

## Fases de implementación (guía para /speckit-tasks)

1. **D-70 y el glosario,** con la aprobación del autor.
2. **Permisos alternativos:** `VerificadorDePermisos` con listas, con su prueba primero.
3. **Contrato y catálogo:** las 6 operaciones y los 3 códigos.
4. **Dominio,** con las pruebas primero: `UnidadOrganica` y `EstructuraOrganica`, con la carga 01 de prueba.
5. **Persistencia:** V3 con las 21 gestiones, y el repositorio, con Testcontainers.
6. **Aplicación y API:** los servicios, el controlador, las pruebas de contrato y de permisos.
7. **SPA:** «Estructura orgánica» y los permisos alternativos en el menú y la ruta.
8. **Punta a punta:** `probar_estructura.sh`, en el perfil y en `perfil-consolidado`.
9. **Cierre:** evidencia, PR, fusión con la aprobación del autor, despliegue y comprobación con `admin.demo` y `vicerrectorado.demo`, y el PR de evidencia con `Closes #35`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| `x-mdd-permiso` como lista (D-70) | La aclaración 1 pide que la lean quienes leen el distributivo y el Administrador, sin cambiar la matriz | Dar `CATALOGOS:LECTURA` a siete roles: cambia D-65 y les da también los catálogos futuros. |
| `EstructuraOrganica` carga todas las unidades | El ciclo y la sede matriz única miran toda la estructura | Consultas recursivas en la base: la regla quedaría fuera del dominio y sería más difícil de probar. |
