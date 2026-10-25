# Implementation Plan: Validar las reglas de los datos e informe por fila

**Branch**: `024-validar-reglas-carga` | **Date**: 2026-10-25 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/024-validar-reglas-carga/spec.md` (issue #29, F01-H03)

## Summary

- **Las reglas, del diccionario:** `ReglasDeColumna` interpreta una vez cada columna de las 10 cargas: su tipo, su largo, su catálogo o enumeración, su patrón, su rango, las partes de sus listas y su obligatoriedad (las 5 condiciones). Lo que el diccionario no define no se valida, y una prueba lista los formatos que no se interpretan (principio XIV).
- **La cédula:** 10 dígitos con su dígito verificador; si le falta el cero inicial, se sugiere la cédula con el cero.
- **La clave:** ninguna clave se repite dentro del archivo.
- **Las referencias:** existen en el sistema. Llegan por `ConsultarReferencias` de Académico y por un puerto nuevo de Docentes, `ConsultarIdentificaciones`, que solo da las identificaciones (principio V).
- **El informe:**
  - `validarArchivo` responde 200 con el informe aunque el archivo tenga errores (principio XIII): si es válido, los totales y los primeros 100 problemas;
  - la operación nueva `descargarInforme` responde el CSV con todos, con las celdas neutralizadas (RNF-SEG-04).
- **Sin estado:** nada se guarda; volver a subir es validar desde cero.
- **SPA:** «Plantillas de carga» muestra el resultado, los totales y la tabla, y descarga el informe.
- **Punta a punta:** `deploy/probar_reglas_carga.sh`.
- **Sin decisiones nuevas ni cambios en el glosario.**

Los detalles y las alternativas están en [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · Bash

**Primary Dependencies**: ninguna nueva. Cargas suma a su `build.gradle` `project(':servicios:academico')` y `project(':servicios:docentes')`, con su comentario.

**Storage**: ninguno nuevo; la validación es en memoria.

**Testing**:
- **Dominio:** JUnit y AssertJ, sin dobles, con las 10 cargas de prueba y el diccionario real. Cada regla tiene casos válidos, inválidos y en el límite.
- **Aplicación:** Mockito.
- **Adaptadores:** el de referencias con dobles escritos a mano de los puertos de Académico y Docentes.
- **Contrato y permisos:** swagger-request-validator en el contrato (también la respuesta CSV) y `VerificadorDePermisos` en los permisos.
- **Límites:** `ModulosTest`.
- **SPA:** Vitest, y Playwright con axe.
- **Punta a punta:** `probar_reglas_carga.sh`.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Cargas y un puerto de Docentes (`datos`), el contrato, la SPA y un script.

**Performance Goals**: un archivo de 5 MB se valida en menos de 10 s; la prueba falla desde 30 s (SC-004).

**Constraints**:
- las reglas, en el diccionario y en un solo intérprete (principio VI, FR-012);
- solo los puertos publicados de Académico y Docentes (principio V);
- nada se guarda ni se registra en los logs, y los motivos no repiten valores (principio IX);
- el CSV, con las celdas neutralizadas (RNF-SEG-04);
- 60 % de líneas por módulo.

**Scale/Scope**: 1 operación nueva y 1 ampliada en Cargas; 1 puerto publicado nuevo en Docentes; 1 pantalla ampliada; 1 script.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Las reglas, en el dominio de Cargas (`ReglasDeColumna`, `ValidadorDeFilas`); la aplicación orquesta; el CSV y las referencias, en los adaptadores. | ✅ |
| II. DDD según el subdominio | Ingesta es de soporte: valores con sus reglas, sin agregado ni eventos. | ✅ |
| III. Lenguaje ubicuo | Carga, Diccionario de datos e Informe de validación de la carga, del glosario, sin cambios. | ✅ |
| IV. API primero | El contrato ampliado y la operación nueva, antes del código. | ✅ |
| V. Límites | Cargas usa `ConsultarReferencias` y `ConsultarIdentificaciones` (`puertos.entrada`); `ModulosTest` lo comprueba. | ✅ |
| VI. Las reglas son datos | Todas salen del diccionario; las 5 condiciones y los patrones del formato se traducen una vez y se prueban contra él. | ✅ |
| VII. Inmutabilidad y trazabilidad | No guarda ni cambia datos: la confirmación y el historial son de F01-H04. | ✅ |
| VIII. Pruebas | Primero las del dominio, sin dobles, con los 10 archivos de prueba. | ✅ |
| IX. Seguridad y datos personales | El informe solo vuelve a quien subió el archivo, no se guarda ni se registra; los motivos no repiten valores; permiso «Cargas: escritura» (D-71). | ✅ |
| X. Simplicidad | Sin tablas, sin estado y con el mismo lector de F01-H02. | ✅ |
| XI y XII | Un puerto nuevo en Docentes, justificado; JavaDoc en español. | ✅ |
| XIII. Errores | Un archivo con errores en sus filas responde 200 con su informe, como dice la constitución; la estructura sigue con sus códigos. | ✅ |
| XIV. Validación de las entradas | Es el objeto de la historia: tipo, caracteres y largo de cada columna, con casos válidos e inválidos. | ✅ |
| §6.4 Decisiones | Ninguna nueva. Siguiente número libre: D-74 · ADR-022. | ✅ |

**Resultado del gate antes de la fase 0:** pasa.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/024-validar-reglas-carga/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1
├── quickstart.md            # fase 1
├── contracts/
│   └── cargas.md            # operaciones y esquemas
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
contratos/cargas/openapi.yaml                                   # validarArchivo ampliada y descargarInforme
backend/servicios/cargas/build.gradle                           # academico y docentes
backend/servicios/cargas/src/main/java/ec/mdd/cargas/
├── dominio/                 # ReglasDeColumna, CondicionDeObligatoriedad, ClaveDeCarga, ReferenciaDeColumna,
│                            # ReferenciasDelSistema, ProblemaDeFila, InformeValidacionCarga, ValidadorDeFilas
├── puertos/{entrada,salida}/# ArchivoValidado con el informe; ReferenciasDelSistema
├── aplicacion/              # ValidarArchivoServicio ampliado
└── adaptadores/             # el controlador con descargarInforme, EscritorDeInforme, ReferenciasDesdePuertos
backend/servicios/docentes/src/main/java/ec/mdd/docentes/
├── puertos/entrada/         # ConsultarIdentificaciones
└── aplicacion/              # ConsultarIdentificacionesServicio
frontend/src/pantallas/plantillas/                              # el resultado y el informe
frontend/e2e/plantillas.spec.ts
deploy/probar_reglas_carga.sh, .github/workflows/perfil-consolidado.yml
```

**Structure Decision**: la validación es de Ingesta (servicio 3, glosario §3); Académico y Docentes solo publican lo que existe.

## Fases de implementación (guía para /speckit-tasks)

1. **Contrato:** `validarArchivo` ampliada y `descargarInforme`; el tipo generado en la SPA.
2. **Dominio, con las pruebas primero:**
   - `ReglasDeColumna`, con la prueba de los formatos interpretados y no interpretados contra el diccionario real;
   - `CondicionDeObligatoriedad`, `ClaveDeCarga` y `ReferenciaDeColumna`;
   - la cédula;
   - `ValidadorDeFilas` y `InformeValidacionCarga`;
   - **los 10 archivos de prueba sin errores**, con sus referencias.
3. **Docentes:** `ConsultarIdentificaciones` y su servicio.
4. **Aplicación y adaptadores:**
   - `ReferenciasDesdePuertos` y `ValidarArchivoServicio`;
   - el controlador, `EscritorDeInforme` y la prueba de la inyección de fórmulas;
   - las pruebas de contrato y de permisos;
   - `ModulosTest`.
5. **SPA:** el resultado, la tabla y la descarga, con Vitest y con Playwright y axe.
6. **Punta a punta:** `probar_reglas_carga.sh`, con el archivo de 5 MB y su tiempo, en el perfil y en `perfil-consolidado`.
7. **Cierre:**
   - la evidencia y el PR;
   - la fusión con la aprobación del autor y el despliegue;
   - la comprobación con `datos.demo`;
   - el PR de evidencia con `Closes #29`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| `ConsultarIdentificaciones` en Docentes | Cargas comprueba que existan los docentes que citan las cargas 8 y 9, sin datos personales ni tipos del dominio de Docentes (principio V, IX). | Usar `ConsultarDocentes.buscar`: devuelve el `Docente` del dominio, que `ModulosTest` prohíbe a Cargas. |
| Un intérprete de los formatos | El diccionario escribe las enumeraciones, los patrones y los rangos como texto. Leerlos una vez y probarlos contra el diccionario mantiene las reglas en un solo lugar (principio VI). | Escribir las reglas en Java por carga: el mismo dato en dos lugares, que se desalinean al cambiar la ERS. |
| `descargarInforme` aparte de `validarArchivo` | Una respuesta en CSV con su propio permiso, su ejemplo y su prueba de contrato. | La misma operación con `Accept: text/csv`: complica la interfaz generada y la prueba. |
