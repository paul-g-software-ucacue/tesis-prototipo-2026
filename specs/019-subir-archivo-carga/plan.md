# Implementation Plan: Subir un archivo y validar su estructura

**Branch**: `019-subir-archivo-carga` | **Date**: 2026-10-23 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/019-subir-archivo-carga/spec.md` (issue #28, F01-H02)

## Summary

- **Subir y validar:** una operación nueva de Cargas recibe el CSV o XLSX de una carga, lo lee y responde cuántos registros tiene, o por qué no se puede leer. Nada se guarda (aclaración 3).
- **Solo la estructura** (aclaración 2), en este orden:
  1. tamaño (hasta 5 MB);
  2. formato (CSV o XLSX, por la extensión y el contenido);
  3. UTF-8;
  4. una sola hoja;
  5. que el archivo se pueda leer (comillas y número de valores por fila);
  6. encabezados iguales a la plantilla, en su orden;
  7. al menos un registro.
- **La fuente de datos (RF-57):** un puerto `LectorDeArchivo` entrega una `TablaDeDatos`; el dominio compara los encabezados y cuenta los registros sin saber el formato.
- **XLSX con Apache POI 5.5.1**, la única dependencia nueva; **CSV** con un lector propio de RFC 4180.
- **Errores:** 7 códigos `CARGAS_*`, dos con detalles por columna o por fila; para eso `EntradaCatalogo` admite detalles. Además, `COMUN_CONTENIDO_DEMASIADO_GRANDE` para el 413.
- **Permisos:** subir exige `CARGAS:ESCRITURA`. **D-71 (propuesta)** da «Cargas» al Responsable de consolidación y al Vicerrector, con una migración de Roles (aclaración 1).
- **SPA:** «Subir archivo» en cada carga de «Plantillas de carga».
- **Punta a punta:** `deploy/probar_subida.sh`, nuevo.

Los detalles y las alternativas están en [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · SQL (Flyway) · Bash

**Primary Dependencies**: Apache POI 5.5.1 (`poi-ooxml`), nueva, solo en el adaptador de Cargas.

**Storage**: ninguno en Cargas. En Roles, `V7__cargas_para_quien_inicia.sql` (D-71).

**Testing**: JUnit y AssertJ en el dominio, sin dobles; los lectores con los 10 CSV de los datos de prueba de la ERS (copiados a los recursos de prueba) y sus XLSX generados en la prueba; Mockito en la aplicación; contrato con swagger-request-validator; `VerificadorDePermisos`; Testcontainers en la matriz de Roles; Vitest y Playwright con axe; `probar_subida.sh`.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Cargas y Roles (`datos` e `identidad`), la plataforma de errores, el contrato, la SPA y un script.

**Performance Goals**: SC-001: el archivo de docentes (47 filas, 22 KB) se valida en menos de 5 s desde que se elige; en el servidor, menos de 1 s para el más grande de prueba (300 filas, 43 KB).

**Constraints**:
- el archivo no se guarda, y ni filas ni valores van a los registros del proceso (principio IX, LOPDP);
- 5 MB por archivo;
- XXE y ZIP inflados bloqueados por POI;
- 60 % de líneas por módulo;
- la memoria del proceso `datos` no crece de forma apreciable: `probar_equipo.sh` la mide.

**Scale/Scope**: 1 operación; 8 códigos de error; 1 migración de Roles; 1 botón por carga; 1 script.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Las reglas de los encabezados y del conteo en el dominio (`Carga`, `TablaDeDatos`); la lectura del archivo en adaptadores; POI solo en `adaptadores.salida`. | ✅ |
| II. DDD según el subdominio | Ingesta es de soporte: valores y una regla en `Carga`, sin agregado. | ✅ |
| III. Lenguaje ubicuo | Carga, Plantilla de carga y Diccionario de datos, del glosario; sin términos nuevos (research §11). | ✅ |
| IV. API primero | La operación y los 8 códigos en el contrato antes del código. | ✅ |
| V. Límites | Todo en Cargas; la matriz, en Roles, con su migración. | ✅ |
| VI. Las reglas son datos | Las columnas esperadas salen del diccionario de datos; los límites (5 MB) son constantes documentadas, no reglas de la política. | ✅ |
| VII. Inmutabilidad y trazabilidad | Validar no cambia datos; el historial es de F01-H04. D-71 queda en el registro de decisiones y en una migración. | ✅ |
| VIII. Pruebas | Primero las del dominio, con los datos de prueba reales de la ERS. | ✅ |
| IX. Seguridad | `CARGAS:ESCRITURA` validado en el servidor; el archivo no se guarda ni se registra; límite de tamaño; POI con sus protecciones; el nombre del archivo se sanea. | ✅ |
| X. Simplicidad | Un lector CSV propio en lugar de otra dependencia; POI solo para XLSX. | ✅ |
| XI y XII | `EntradaCatalogo.conDetalles` amplía lo que ya existe para encabezados; JavaDoc en español. | ✅ |
| XIII. Errores | 7 códigos `CARGAS_*` y uno común, con detalles que nunca llevan valores del archivo. | ✅ |
| §6.4 Decisiones | **D-71** (Cargas para quien inicia el proceso), propuesta en research §1, pendiente de la aprobación del autor antes del código. | ⏳ |

**Resultado del gate antes de la fase 0:** pasa, con una condición: D-71 se aprueba y se registra en `docs/decisiones.md` antes del código.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/019-subir-archivo-carga/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1
├── quickstart.md            # fase 1
├── contracts/
│   └── cargas.md            # operación, esquemas y errores
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
docs/decisiones.md                                         # D-71
contratos/cargas/openapi.yaml, contratos/comun/errores.md   # la operación y los 8 códigos
backend/gradle.properties                                   # versionApachePoi=5.5.1
backend/plataforma/errores-nucleo/…/EntradaCatalogo.java, CodigosComunes.java
backend/plataforma/errores/…/ManejadorDeErrores.java
backend/servicios/roles/src/main/resources/db/migration/roles/V7__cargas_para_quien_inicia.sql
backend/servicios/cargas/build.gradle                       # poi-ooxml
backend/servicios/cargas/src/main/java/ec/mdd/cargas/
├── dominio/                 # TablaDeDatos, DiferenciaDeEncabezado, Carga.compararEncabezados…
├── puertos/{entrada,salida}/# ValidarArchivo, ArchivoSubido, LectorDeArchivo y sus excepciones
├── aplicacion/              # ValidarArchivoServicio
└── adaptadores/             # SubidaControlador, ErroresCargas, LectorPorFormato, LectorCsv, LectorXlsx
backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba/   # los 10 CSV de la ERS
backend/apps/datos/src/main/resources/application.yaml      # multipart 10 MB
frontend/src/pantallas/plantillas/                          # «Subir archivo»
deploy/probar_subida.sh, .github/workflows/perfil-consolidado.yml
```

**Structure Decision**: la subida es de Ingesta de datos, en Cargas, junto a las plantillas; la matriz, en Roles.

## Fases de implementación (guía para /speckit-tasks)

1. **D-71:** el autor la aprueba; se registra en `docs/decisiones.md` y en la tabla «Matriz D-16».
2. **Plataforma:** `EntradaCatalogo.conDetalles` y `COMUN_CONTENIDO_DEMASIADO_GRANDE`, con sus pruebas primero.
3. **Roles:** `V7` con su prueba de la matriz primero.
4. **Contrato y catálogo:** la operación, sus esquemas y los 8 códigos.
5. **Dominio,** con las pruebas primero: `TablaDeDatos`, `Carga.compararEncabezados` y `Carga.leer`.
6. **Lectores:** `LectorCsv`, `LectorXlsx` y `LectorPorFormato`, con los datos de prueba de la ERS.
7. **Aplicación y API:** `ValidarArchivoServicio`, `SubidaControlador`, `ErroresCargas`, las pruebas de contrato y de permisos; el límite de multipart en `datos`.
8. **SPA:** «Subir archivo» y el resultado.
9. **Punta a punta:** `probar_subida.sh`, en el perfil y en `perfil-consolidado`.
10. **Cierre:** evidencia, PR, fusión con la aprobación del autor, despliegue, la comprobación con `consolidacion.demo` y el PR de evidencia con `Closes #28`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| Apache POI | Leer XLSX con el texto que muestra cada celda (cédulas con 0, fechas) | Un lector propio de OOXML: más código y el formato de las celdas no es trivial (research §5). |
| `EntradaCatalogo.conDetalles` | Decir qué columna falta (escenario 3 del issue) con un código propio | `COMUN_SOLICITUD_INVALIDA` con detalles: pierde el código y su texto no habla de archivos (research §6). |
| `COMUN_CONTENIDO_DEMASIADO_GRANDE` | Hoy un 413 de Spring sale con el código de 400 | Dejarlo así: el estado y el código no coincidirían. |
