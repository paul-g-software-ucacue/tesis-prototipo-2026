# Implementation Plan: Ver el tablero de preparación del periodo

**Branch**: `027-tablero-preparacion` | **Date**: 2026-10-30 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/027-tablero-preparacion/spec.md` (issue #33, F02-H02)

## Summary

- **Los dueños dicen si tienen datos de un periodo** (research §1):
  - `tieneDatosEn(periodo)` en el lenguaje publicado de ADR-022, que por defecto es `tieneDatos()`;
  - solo la oferta y los matriculados lo reemplazan;
  - los proyectos usan los asignables del periodo, que Cargas ya pregunta a Docentes.
- **Académico dice si un periodo está abierto o cerrado:** `PeriodoDeReferencia` suma esos dos datos (research §2).
- **Cargas arma el tablero:**
  - `PreparacionDelPeriodo`, del glosario, en su dominio, calcula el estado de cada carga en el periodo, también «con errores» desde el historial;
  - también calcula lo que falta y si está listo;
  - la operación nueva `consultarPreparacionDelPeriodo` lo responde con la lista de periodos para elegir.
- **SPA:** la pantalla nueva «Preparación del periodo», con el selector, la tabla, lo que falta y «Crear distributivo». Por ahora el botón solo avisa que la construcción llega con F06.
- **Pruebas:** el tablero en el proceso `datos` y `deploy/probar_preparacion.sh`.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · Bash

**Primary Dependencies**: ninguna nueva.

**Storage**: PostgreSQL 18, sin migraciones. Las consultas nuevas son de existencia por periodo, sobre tablas que ya existen.

**Testing**:
- **Dominio** (`PreparacionDelPeriodo`): sin dobles, con el diccionario real.
- **Aplicación:** Mockito.
- **`tieneDatosEn` de la oferta y los matriculados:** Testcontainers.
- **Contrato y permisos:** `ValidadorDeContrato` y `VerificadorDePermisos`.
- **Proceso `datos`:** `ModulosTest` y `ConfirmarLasDiezCargasIntegrationTest`.
- **SPA:** Vitest, y Playwright con axe.
- **Punta a punta:** el script.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web:
- el módulo de plataforma;
- Cargas y Académico;
- el contrato;
- la SPA;
- un script.

**Performance Goals**: el tablero en menos de 3 s (SC-003, con holgura).

**Constraints**:
- solo `puertos.entrada` entre servicios, más el lenguaje publicado (principio V);
- nada se guarda;
- sin datos personales;
- 60 % de líneas por módulo.

**Scale/Scope**:
- 1 operación nueva;
- 1 método por defecto nuevo en el lenguaje publicado, con 2 implementaciones;
- 2 datos nuevos en `PeriodoDeReferencia`;
- 1 pantalla nueva;
- 1 script;
- ningún código de error nuevo;
- ninguna migración.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | `PreparacionDelPeriodo` es dominio puro; el caso de uso, en aplicación; las consultas a otros servicios, en los adaptadores de salida que ya existen. | ✅ |
| II. DDD según el subdominio | Ingesta es de soporte: un valor calculado y un caso de uso, sin agregados. | ✅ |
| III. Lenguaje ubicuo | «Tablero de preparación» (`PreparacionDelPeriodo`), «Estado de la carga», «Carga obligatoria», «Periodo académico» y «Proyecto asignable», del glosario. Sin términos nuevos (research §10). | ✅ |
| IV. API primero | El contrato, antes del código. | ✅ |
| V. Límites | Cargas pregunta a los dueños por el lenguaje publicado, a Académico por `ConsultarReferencias` y a Docentes por `ConsultarAsignablesDelPeriodo`. `ModulosTest` lo comprueba. | ✅ |
| VI. Las reglas son datos | La obligatoriedad sale del diccionario; «asignable» sigue siendo la regla de Docentes. | ✅ |
| VII. Inmutabilidad y trazabilidad | Solo lee: el historial y los datos de los dueños. | ✅ |
| VIII. Pruebas | Primero el dominio del tablero y `tieneDatosEn`; los dos escenarios del issue, cada uno con su prueba. | ✅ |
| IX. Seguridad y datos personales | Leer el distributivo o las cargas para ver el tablero; el botón, solo con «Distributivo: escritura». Sin datos personales. | ✅ |
| X. Simplicidad | Sin tablas ni eventos; sin operación para «Crear distributivo» hasta F06. | ✅ |
| XI y XII | Sin módulos nuevos. JavaDoc en español. | ✅ |
| XIII. Errores | Reusa `CARGAS_PERIODO_NO_ENCONTRADO` y `COMUN_SOLICITUD_INVALIDA`. | ✅ |
| XIV. Validación de las entradas | El periodo, con el patrón y el largo de la carga 3, en el contrato, el servidor y la pantalla, con pruebas válidas e inválidas. | ✅ |
| §6.4 Decisiones | Las 4 aclaraciones y los 2 supuestos los aprobó el autor el 2026-10-30. No hay decisiones nuevas. | ✅ |

**Resultado del gate antes de la fase 0:** pasa.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/027-tablero-preparacion/
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
backend/plataforma/lenguaje-cargas/                 # tieneDatosEn en TraductorDeCarga y AplicadorDeCargas
backend/servicios/academico/
├── …/puertos/entrada/PeriodoDeReferencia.java      # abierto y cerrado
├── …/aplicacion/ConsultarReferenciasServicio.java  # con NormativaDelPeriodo
└── …/aplicacion/cargas/{TraductorOferta, TraductorMatriculas}.java
backend/servicios/cargas/src/main/java/ec/mdd/cargas/
├── dominio/{PreparacionDelPeriodo, CargaDelPeriodo, LoQueFalta, …}
├── aplicacion/ConsultarPreparacionServicio.java
├── puertos/{entrada/ConsultarPreparacion, salida/ReferenciasDelSistema}
└── adaptadores/{entrada/PreparacionControlador, salida/ReferenciasDesdePuertos}
backend/apps/datos/src/integrationTest/…/ConfirmarLasDiezCargasIntegrationTest.java
contratos/cargas/openapi.yaml, contratos/comun/errores.md
frontend/src/pantallas/preparacion/                 # la pantalla nueva
frontend/src/navegacion/                            # la ruta y el menú
deploy/probar_preparacion.sh, .github/workflows/perfil-consolidado.yml
```

**Structure Decision**: el tablero es de Ingesta (servicio 3, glosario). Qué es tener datos de un periodo lo dice cada dueño (MD-13, ADR-022); si el periodo está abierto, Académico.

## Fases de implementación (guía para /speckit-tasks)

1. **Base:**
   - el contrato y la causa de `CARGAS_PERIODO_NO_ENCONTRADO`;
   - `tieneDatosEn` en el lenguaje publicado;
   - `PreparacionDelPeriodo` probado sin dobles.
2. **Los dueños y Académico:**
   - `tieneDatosEn` de la oferta y los matriculados, con sus consultas;
   - `abierto` y `cerrado` en `PeriodoDeReferencia`, con `NormativaDelPeriodo` en `ConsultarReferenciasServicio`.
3. **Cargas:**
   - `ReferenciasDelSistema.periodos()`;
   - `ConsultarPreparacionServicio` y `PreparacionControlador`;
   - el contrato y los permisos;
   - el tablero en `ConfirmarLasDiezCargasIntegrationTest` (los dos escenarios y la medición).
4. **SPA:**
   - la pantalla, la ruta y el menú;
   - el selector, la tabla, lo que falta y el botón;
   - Vitest, y Playwright con axe.
5. **Punta a punta y cierre:**
   - `probar_preparacion.sh`;
   - la evidencia;
   - el PR;
   - la fusión con tu aprobación;
   - el despliegue;
   - tu comprobación;
   - el PR de evidencia con `Closes #33`.

## Complexity Tracking

Sin desvíos de la constitución que justificar.
