# Implementation Plan: Descargar las plantillas de carga

**Branch**: `018-plantillas-carga` | **Date**: 2026-10-23 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/018-plantillas-carga/spec.md` (issue #27, F01-H01)

## Summary

- **El diccionario de datos en Cargas:** las 10 cargas de la ERS §8.3, sus 115 columnas, las 13 convenciones y los 5 catálogos, en un solo archivo versionado (`diccionario-de-datos.yaml`) que se valida al arrancar. De él salen la plantilla y el diccionario, y F01-H02 validará con él (aclaración 3).
- **Dos CSV por carga:** la plantilla (solo los encabezados) y su diccionario (una fila por columna, más las convenciones), en UTF-8 con BOM (aclaración 2).
- **Permisos:** `[CARGAS:LECTURA, DISTRIBUTIVO:LECTURA]` con D-70; descarga el Responsable de consolidación, entre otros. Sin decisiones nuevas (aclaración 1).
- **Contrato:** 3 operaciones en `contratos/cargas/openapi.yaml`; sin códigos de error nuevos.
- **SPA:** la pantalla «Plantillas de carga», con las 10 cargas, sus descargas y el diccionario desplegable.
- **Punta a punta:** `deploy/probar_plantillas.sh`, nuevo.
- **Glosario:** propone el término «Diccionario de datos», pendiente de la aprobación del autor antes del código (research §11).

Los detalles y las alternativas están en [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · YAML · Bash

**Primary Dependencies**: sin dependencias nuevas (SnakeYAML ya está en Spring Boot y lo usa Usuarios).

**Storage**: ninguno. La definición es un recurso del servicio; el esquema `cargas` sigue vacío.

**Testing**: JUnit y AssertJ en el dominio, sin dobles, con la definición real y un oráculo copiado de los encabezados de la ERS; Mockito en la aplicación; swagger-request-validator en el contrato; `VerificadorDePermisos`; Vitest y Playwright con axe (evento `download`); `probar_plantillas.sh`.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Cargas (`datos`), el contrato, la SPA y un script.

**Performance Goals**: cada descarga, por debajo de 200 ms (la definición está en memoria); SC-001: descargar una plantilla o un diccionario en menos de 30 s desde la pantalla.

**Constraints**:
- las cargas y sus columnas son un dato, no código (principio VI);
- sin permiso, 403 en el servidor (principio IX, ADR-021);
- CSV sin fórmulas (principio IX);
- 60 % de líneas por módulo.

**Scale/Scope**: 1 archivo de definición (10 cargas, 115 columnas); 3 operaciones; 1 pantalla; 1 script.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Reglas de la definición en `DiccionarioDeDatos`; la aplicación orquesta; la lectura del YAML y la escritura del CSV son adaptadores. | ✅ |
| II. DDD según el subdominio | Ingesta es de soporte: valores validados, sin agregado ni repositorio (research §1). | ✅ |
| III. Lenguaje ubicuo | Carga, Plantilla de carga y Carga obligatoria, del glosario. «Diccionario de datos» se propone al glosario antes del código (research §11). | ⏳ pendiente de aprobación |
| IV. API primero | Las 3 operaciones en el contrato antes del código. | ✅ |
| V. Límites | Todo en Cargas. Los catálogos del diccionario son una copia para documentar; quién manda al validar es de F01-H02 (research §5). | ✅ |
| VI. Las reglas son datos | Las 10 cargas son un YAML versionado, no clases (research §2). | ✅ |
| VII. Inmutabilidad y trazabilidad | Descargar no cambia nada: no hay registro (research §7). | ✅ |
| VIII. Pruebas | Primero las del dominio, con la definición real y un oráculo independiente. | ✅ |
| IX. Seguridad | Dos permisos alternativos validados en el servidor (D-70); sin fórmulas en los CSV; ejemplos ficticios. | ✅ |
| X. Simplicidad | Sin tablas ni dependencias; la definición se lee una vez. | ✅ |
| XI y XII | Sin abstracciones nuevas; JavaDoc en español. | ✅ |
| XIII. Errores | 404 y 400 comunes; sin códigos nuevos. | ✅ |
| §6.4 Decisiones | Ninguna nueva: aplica D-70. Siguiente número libre: D-71 · ADR-022. | ✅ |

**Resultado del gate antes de la fase 0:** pasa, con una condición: la frase del glosario (research §11) se aprueba y se aplica antes del código, como en F03-H01.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/018-plantillas-carga/
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
contratos/cargas/openapi.yaml                    # 3 operaciones
backend/servicios/cargas/src/main/java/ec/mdd/cargas/
├── dominio/                                     # Carga, ColumnaDeCarga, PlantillaCarga, DiccionarioDeDatos…
├── puertos/{entrada,salida}/                    # ConsultarPlantillas, CargaNoEncontrada, FuenteDelDiccionario
├── aplicacion/                                  # ConsultarPlantillasServicio
├── adaptadores/                                 # PlantillasControlador, EscritorCsv, DiccionarioYaml
└── infraestructura/                             # ConfiguracionCargas
backend/servicios/cargas/src/main/resources/cargas/diccionario-de-datos.yaml
backend/servicios/cargas/src/test/resources/cargas/anexo-ers-encabezados.csv   # oráculo
frontend/src/api/cliente.ts, frontend/package.json (generar:api)
frontend/src/pantallas/plantillas/               # «Plantillas de carga»
frontend/src/navegacion/Marco.tsx, frontend/src/rutas.tsx, frontend/src/sesion/funcionalidades.ts
deploy/probar_plantillas.sh, .github/workflows/perfil-consolidado.yml
```

**Structure Decision**: las plantillas son de Ingesta de datos, en Cargas (glosario, contexto «Ingesta de datos»), que ya está en el proceso `datos`.

## Fases de implementación (guía para /speckit-tasks)

1. **Glosario:** el autor aprueba la frase de «Diccionario de datos» y se aplica (copia `modelo_dominio.md.antes-F01-H01`).
2. **Contrato:** las 3 operaciones y sus esquemas.
3. **Dominio,** con las pruebas primero: `DiccionarioDeDatos` y sus valores; la definición YAML con el oráculo de la ERS.
4. **Lectura y aplicación:** `DiccionarioYaml`, `ConsultarPlantillasServicio`.
5. **API:** `EscritorCsv`, el controlador, las pruebas de contrato y de permisos (incluido el 403 y el 404 con `Accept: text/csv`).
6. **SPA:** `apiCargas`, «Plantillas de carga», el menú y la ruta.
7. **Punta a punta:** `probar_plantillas.sh`, en el perfil y en `perfil-consolidado`.
8. **Cierre:** evidencia, PR, fusión con la aprobación del autor, despliegue, la comprobación con `consolidacion.demo` (SC-001 y SC-003 en LibreOffice Calc) y el PR de evidencia con `Closes #27`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| Oráculo con los encabezados de la ERS en las pruebas | El YAML se transcribe a mano: una columna mal copiada rompería F01-H02 | Confiar en la transcripción: un error no se vería hasta cargar un archivo. |
| BOM en los CSV | SC-003: sin él, Excel rompe las tildes y la «ñ» | CSV sin BOM: es más limpio para máquinas, pero quien prepara los archivos usa una hoja de cálculo. |
