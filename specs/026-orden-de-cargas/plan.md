# Implementation Plan: Configurar las 10 cargas en su orden

**Branch**: `026-orden-de-cargas` | **Date**: 2026-10-27 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/026-orden-de-cargas/spec.md` (issue #31, F01-H05)

## Summary

- **Cada dueño dice si tiene datos** de sus cargas (research §1 y §2):
  - `TraductorDeCarga.tieneDatos()` y `AplicadorDeCargas.tieneDatos(carga)` en el lenguaje publicado de ADR-022;
  - los 10 traductores lo implementan con una consulta de existencia de sus registros vigentes.
- **Cargas impone el orden:**
  - `OrdenDeLasCargas`, del dominio, calcula con el `dependeDe` del diccionario qué cargas anteriores faltan;
  - `validarArchivo` y `confirmarCarga` responden 409 `CARGAS_CARGAS_ANTERIORES_PENDIENTES` antes de leer el archivo.
- **El estado:** la operación nueva `consultarEstadoDeLasCargas` da, por carga, «Pendiente», «Cargada» o «Sin proyectos» y lo que falta. Se calcula en cada consulta; no se guarda.
- **«Sin proyectos»:**
  - la operación nueva `confirmarSinProyectos` guarda la confirmación en el historial (V3, con el periodo);
  - le pregunta a Docentes si el periodo tiene proyectos asignables (F03-H05);
  - si vale se calcula al leer.
- **RF-57:** `LectorDeArchivo` pasa a llamarse `FuenteDeDatos`, el término del glosario. Una regla de ArchUnit y una prueba con una fuente que no es un archivo demuestran que las reglas no dependen del formato.
- **SPA:** la columna «Estado», el botón deshabilitado con lo que falta y el diálogo «Sin proyectos…».
- **Pruebas:**
  - el orden y el estado en `ConfirmarLasDiezCargasIntegrationTest`;
  - `deploy/probar_orden_cargas.sh`.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · Bash

**Primary Dependencies**: ninguna nueva.

**Storage**: PostgreSQL 18.
- Cargas `V3__sin_proyectos.sql`: el periodo y el resultado `SIN_PROYECTOS` en el historial.
- Sin migraciones en los dueños: las consultas de existencia usan tablas que ya existen.

**Testing**:
- **Dominio** (`OrdenDeLasCargas`): sin dobles, con el diccionario real.
- **Aplicación:** Mockito.
- **`tieneDatos()` de los 10 traductores:** Testcontainers, en las pruebas de integración de cada dueño, con activos e inactivos.
- **Historial V3:** Testcontainers.
- **Contrato, catálogo y permisos:** `ValidadorDeContrato`, `VerificadorDeCatalogo` y `VerificadorDePermisos`.
- **Arquitectura:** `ArquitecturaTest` con la regla nueva de la fuente de datos.
- **Proceso `datos`:** `ModulosTest` y `ConfirmarLasDiezCargasIntegrationTest`.
- **SPA:** Vitest, y Playwright con axe.
- **Punta a punta:** el script.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web:
- el módulo de plataforma;
- 4 servicios del proceso `datos`;
- el contrato;
- la SPA;
- un script.

**Performance Goals**: el estado de las 10 cargas en menos de 2 s (SC-003, con holgura). La validación y la confirmación no cambian su tiempo: suman 10 consultas de existencia como máximo.

**Constraints**:
- solo `puertos.entrada` entre servicios, más el módulo de plataforma (principio V);
- el orden se comprueba antes de leer el archivo;
- la confirmación «sin proyectos» nunca se borra (principio VII);
- el estado no muestra datos personales (FR-018);
- 60 % de líneas por módulo.

**Scale/Scope**:
- 2 operaciones nuevas y 2 que suman un 409;
- 1 método nuevo en el lenguaje publicado y 10 implementaciones;
- 1 migración;
- 3 códigos de error;
- 1 puerto renombrado y 1 nuevo;
- 1 pantalla ampliada;
- 1 script.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | `OrdenDeLasCargas` es dominio puro: recibe el diccionario y un predicado. Los casos de uso van en aplicación; la consulta a Docentes va en un adaptador de salida. | ✅ |
| II. DDD según el subdominio | Ingesta es de soporte: valores y casos de uso, sin agregados nuevos. | ✅ |
| III. Lenguaje ubicuo | Usa «Carga obligatoria», «sin proyectos», «Historial de cargas» y «Puerto de fuente de datos» del glosario; el puerto se renombra para que coincida. | ✅ |
| IV. API primero | El contrato y los 3 códigos, antes del código. | ✅ |
| V. Límites | Cargas pregunta a los dueños por el aplicador (ADR-022) y a Docentes por su puerto de entrada `ConsultarProyectos`; no lee tablas ajenas. `ModulosTest` lo comprueba. | ✅ |
| VI. Las reglas son datos | Las dependencias salen solo del `dependeDe` del diccionario. «Asignable» sigue siendo la regla de Docentes (F03-H05), sin copia. | ✅ |
| VII. Inmutabilidad y trazabilidad | Cada «sin proyectos» queda en el historial con quién y cuándo, y no se borra cuando deja de valer. | ✅ |
| VIII. Pruebas | Primero `OrdenDeLasCargas` y `tieneDatos()`; luego los 4 escenarios del issue, cada uno con su prueba. | ✅ |
| IX. Seguridad y datos personales | Confirmar «sin proyectos» exige `CARGAS:ESCRITURA`; el estado, la lectura de las plantillas. El estado solo dice si hay datos. | ✅ |
| X. Simplicidad | Sin tabla de estado ni eventos: se calcula. Sin tabla de confirmaciones: el historial. | ✅ |
| XI y XII | Sin módulos nuevos. JavaDoc en español. | ✅ |
| XIII. Errores | 3 códigos con sus 8 columnas, en «tú»; los detalles del orden nombran cada carga que falta. | ✅ |
| XIV. Validación de las entradas | El periodo, con el patrón y el largo del diccionario de la carga 3, en el contrato, la SPA y el servidor, con pruebas válidas e inválidas. | ✅ |
| §6.4 Decisiones | No hay decisiones nuevas: las 4 aclaraciones están en la spec, y ampliar el aplicador cabe en ADR-022. | ✅ |

**Resultado del gate antes de la fase 0:** pasa.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/026-orden-de-cargas/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1
├── quickstart.md            # fase 1
├── contracts/
│   └── cargas.md
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
backend/plataforma/lenguaje-cargas/                  # tieneDatos en TraductorDeCarga y AplicadorDeCargas
backend/servicios/cargas/
├── src/main/resources/db/migration/cargas/V3__sin_proyectos.sql
└── src/main/java/ec/mdd/cargas/
    ├── dominio/{OrdenDeLasCargas, EstadoDeCarga, …}
    ├── aplicacion/{ConsultarEstadoDeLasCargasServicio, ConfirmarSinProyectosServicio, …}
    ├── puertos/salida/{FuenteDeDatos, ProyectosDelPeriodo}
    └── adaptadores/{entrada/EstadoControlador, salida/ProyectosDesdePuertos, …}
backend/servicios/academico/…/aplicacion/cargas/      # tieneDatos de 6 traductores + consultas de existencia
backend/servicios/docentes/…/aplicacion/cargas/       # tieneDatos de 3 traductores
backend/servicios/normativa/…/aplicacion/cargas/      # tieneDatos de la política
backend/apps/datos/src/integrationTest/…/ConfirmarLasDiezCargasIntegrationTest.java
contratos/cargas/openapi.yaml, contratos/comun/errores.md
frontend/src/pantallas/plantillas/                    # estado, botón deshabilitado, «Sin proyectos…»
deploy/probar_orden_cargas.sh, .github/workflows/perfil-consolidado.yml
```

**Structure Decision**: el orden es de Ingesta (servicio 3). Qué es «tener datos» lo decide cada servicio dueño (MD-13), por el lenguaje publicado (ADR-022).

## Fases de implementación (guía para /speckit-tasks)

Los incrementos se prueban por separado. Cada uno deja el sistema funcionando.

1. **Base:**
   - el contrato y los 3 códigos del catálogo;
   - `tieneDatos` en el lenguaje publicado;
   - `OrdenDeLasCargas` probado sin dobles.
2. **Los dueños:**
   - `tieneDatos()` en los 10 traductores, con sus consultas de existencia;
   - pruebas de integración con activos e inactivos.
3. **El orden y el estado:**
   - la comprobación en `validarArchivo` y `confirmarCarga`;
   - `consultarEstadoDeLasCargas`;
   - la 04 antes de la 02 y el estado de las 10 en `ConfirmarLasDiezCargasIntegrationTest` (SC-001, SC-002), con el tiempo del estado medido (SC-003).
4. **«Sin proyectos»:**
   - V3 y el historial;
   - `ProyectosDelPeriodo` y su adaptador;
   - `confirmarSinProyectos`, con los periodos válidos, inválidos, inexistentes y con proyectos, y la confirmación repetida.
5. **RF-57:**
   - el puerto renombrado a `FuenteDeDatos`;
   - la regla de ArchUnit;
   - la prueba con una fuente que no es un archivo.
6. **SPA:**
   - la columna «Estado»;
   - el botón deshabilitado con lo que falta;
   - el diálogo «Sin proyectos…» con la validación del periodo;
   - el historial con las confirmaciones «sin proyectos»;
   - la actualización sin recargar la página;
   - Vitest, y Playwright con axe.
7. **Punta a punta y cierre:**
   - `probar_orden_cargas.sh`, también en `perfil-consolidado`;
   - la evidencia;
   - el PR;
   - la fusión con tu aprobación;
   - el despliegue;
   - tu comprobación;
   - el PR de evidencia con `Closes #31`.

## Complexity Tracking

Sin desvíos de la constitución que justificar.
