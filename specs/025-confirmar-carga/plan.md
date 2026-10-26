# Implementation Plan: Vista previa y confirmación de la carga

**Branch**: `025-confirmar-carga` | **Date**: 2026-10-26 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/025-confirmar-carga/spec.md` (issue #30, F01-H04)

## Summary

- **Un lenguaje publicado de las cargas**, en el módulo nuevo `:plataforma:lenguaje-cargas` (ADR-022 propuesto, research §1):
  - el lote;
  - el traductor que escribe cada dueño por carga;
  - el motor que compara, calcula la huella y aplica todo o nada dentro de la transacción del dueño;
  - el aplicador que ve Cargas.
- **Los tres dueños** implementan sus traductores:
  - Académico: las cargas 01 a 05 y 10, con los matriculados nuevos en su esquema;
  - Docentes: la 06, la 08 y la 09;
  - Normativa: la 07.
  Los traductores salen del código de prueba que ya lee estos archivos, y registrar y corregir usan los casos de uso de siempre, con sus reglas y su registro de cambios.
- **Cargas:**
  - `validarArchivo` trae la vista previa;
  - `confirmarCarga` valida de nuevo, comprueba el hash en el historial y pide al dueño que aplique la misma vista previa (la huella);
  - `listarHistorial` lee el historial, que va en su esquema.
  Hay 3 códigos nuevos en el catálogo.
- **SPA:** la vista previa, «Confirmar», «Cancelar», el resultado y el historial. Los mensajes de F01-H03 pasan a «tú».
- **Pruebas:** las 10 cargas en orden, en una prueba de integración del proceso `datos` y en `deploy/probar_confirmar_carga.sh`.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · Bash

**Primary Dependencies**: ninguna externa nueva. Hay un módulo interno nuevo, `:plataforma:lenguaje-cargas`, del que dependen Cargas, Académico, Docentes y Normativa.

**Storage**: PostgreSQL 18.
- Cargas `V2__historial_de_cargas.sql`.
- Académico `V6__matriculados.sql`.
- Los registros de cambios de los dueños suman acciones y el `hash` en su detalle.

**Testing**:
- **Dominio y motor:** sin dobles.
- **Aplicación:** Mockito.
- **Traductores:** con los 10 archivos de prueba.
- **Repositorios y migraciones:** Testcontainers.
- **Contrato:** swagger-request-validator.
- **Permisos:** `VerificadorDePermisos`.
- **Catálogo:** `VerificadorDeCatalogo`.
- **Proceso `datos`:** `ModulosTest` y `ConfirmarLasDiezCargasIntegrationTest`.
- **SPA:** Vitest, y Playwright con axe.
- **Punta a punta:** el script.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web:
- un módulo de plataforma;
- 4 servicios del proceso `datos`;
- el contrato;
- la SPA;
- un script.

**Performance Goals**:
- La vista previa de 5 MB en menos de 30 s.
- Confirmarla en menos de 60 s, con la meta del plan en 20 s (SC-004, research §12).

**Constraints**:
- todo o nada en la transacción del dueño (MD-13);
- sin estado entre la vista previa y la confirmación;
- no se guarda el contenido del archivo (principio IX);
- solo `puertos.entrada` entre servicios, más el módulo de plataforma (principio V);
- 60 % de líneas por módulo.

**Scale/Scope**:
- 2 operaciones nuevas y 1 ampliada;
- 10 traductores;
- 1 entidad nueva (matriculados);
- 2 migraciones;
- 3 códigos de error;
- 1 pantalla ampliada;
- 1 script.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | El motor y los registros del lote no dependen de Spring. Los traductores van en la capa de aplicación de cada dueño, porque llaman a sus casos de uso. El historial vive en los adaptadores de Cargas. | ✅ |
| II. DDD según el subdominio | Ingesta y Datos maestros son de soporte: valores y casos de uso, sin agregados nuevos salvo `Matricula`. | ✅ |
| III. Lenguaje ubicuo | Usa Vista previa, Confirmar una carga, Historial de cargas y Matriculados, del glosario, sin cambios. | ✅ |
| IV. API primero | El contrato ampliado y las dos operaciones, antes del código. | ✅ |
| V. Límites | Cargas no conoce los tipos de los dueños: solo el lenguaje de `:plataforma:lenguaje-cargas`. Cada dueño aplica en su transacción (MD-13). `ModulosTest` lo comprueba. | ✅ con ADR-022 |
| VI. Las reglas son datos | Las reglas del diccionario siguen en F01-H03. Cada traductor es la única traducción de las columnas de su carga, probada con sus archivos de prueba. | ✅ |
| VII. Inmutabilidad y trazabilidad | Las bajas no se aplican; una versión de normativa en uso no cambia; cada cambio queda en el registro del dueño, con el hash; cada intento queda en el historial. | ✅ |
| VIII. Pruebas | Primero las del motor y de cada traductor; la de las 10 en orden demuestra SC-001 y SC-002. | ✅ |
| IX. Seguridad y datos personales | Confirmar exige «Cargas: escritura» (D-71). El historial no guarda el contenido. La vista previa con datos de docentes solo vuelve a quien subió el archivo, y no va a los logs. | ✅ |
| X. Simplicidad | Reusa los casos de uso y el código de prueba que ya lee los archivos; no guarda nada entre los pasos. | ✅ |
| XI y XII | Un módulo de plataforma justificado (Complexity Tracking) y JavaDoc en español. | ✅ |
| XIII. Errores | 3 códigos nuevos con sus 8 columnas; los rechazos de los dueños llevan la fila; los mensajes quedan en «tú», como el catálogo. | ✅ |
| XIV. Validación de las entradas | Las de F01-H03, otra vez al confirmar. La `huella` del pedido se valida: 64 caracteres hexadecimales. | ✅ |
| §6.4 Decisiones | ADR-022 y D-74, que apruebas tú antes de implementar. | ⏳ aprobación |

**Resultado del gate antes de la fase 0:** pasa, con ADR-022 pendiente de tu aprobación.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/025-confirmar-carga/
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
backend/plataforma/lenguaje-cargas/                         # nuevo: el lenguaje publicado y el motor del lote
backend/servicios/cargas/
├── src/main/resources/db/migration/cargas/V2__historial_de_cargas.sql
└── src/main/java/ec/mdd/cargas/                   # vista previa, ConfirmarCarga, ConsultarHistorial, historial JDBC
backend/servicios/academico/
├── src/main/resources/db/migration/academico/V6__matriculados.sql
└── src/main/java/ec/mdd/academico/{dominio/Matricula, aplicacion/cargas/…}   # 6 traductores y el aplicador
backend/servicios/docentes/src/main/java/ec/mdd/docentes/aplicacion/cargas/   # 3 traductores y el aplicador
backend/servicios/normativa/src/main/java/ec/mdd/normativa/aplicacion/cargas/ # el traductor de la 07
backend/apps/datos/src/integrationTest/…/ConfirmarLasDiezCargasIntegrationTest.java
contratos/cargas/openapi.yaml, contratos/comun/errores.md
frontend/src/pantallas/plantillas/                 # vista previa, confirmar, historial
deploy/probar_confirmar_carga.sh, .github/workflows/perfil-consolidado.yml
docs/decisiones.md                                 # ADR-022 y D-74
```

**Structure Decision**: el mecanismo es de Ingesta (servicio 3). Cada traducción es del servicio dueño (MD-13), y lo común es lenguaje publicado de plataforma (ADR-022).

## Fases de implementación (guía para /speckit-tasks)

Los incrementos se prueban por separado. Cada uno deja el sistema funcionando.

1. **Base:**
   - ADR-022 y D-74 en `docs/decisiones.md`, con tu aprobación;
   - el contrato y los 3 códigos;
   - `:plataforma:lenguaje-cargas`, con el motor probado sin dobles;
   - el historial de Cargas (V2) y la vista previa en `validarArchivo`;
   - `confirmarCarga` y `listarHistorial`;
   - la comprobación de que cada carga tiene un solo dueño.
2. **Incremento A, Académico 01 y 02:**
   - los traductores de estructura y carreras;
   - el aplicador de Académico;
   - el orden topológico de la 01;
   - un rechazo de una regla del dueño (dos sedes matriz) que no aplica nada.
3. **Incremento B, Académico 03, 04, 05 y 10:**
   - periodos, mallas y asignaturas, oferta y matriculados (V6 y `Matricula`).
4. **Incremento C, Docentes 06, 08 y 09:**
   - sus traductores, desde `DocentesDePrueba` y `CargasDePrueba`;
   - el rechazo de un segundo coordinador vigente (RN-12).
5. **Incremento D, Normativa 07:**
   - crear una versión copiando la última, o editarla;
   - la versión en uso se rechaza.
6. **Las 10 en orden:**
   - `ConfirmarLasDiezCargasIntegrationTest` (SC-001, SC-002);
   - la medición de 5 MB, y la optimización por lotes solo si no se cumple.
7. **SPA:**
   - la vista previa, «Confirmar» y «Cancelar», el resultado y el historial;
   - los mensajes de F01-H03 en «tú»;
   - Vitest, y Playwright con axe.
8. **Punta a punta y cierre:**
   - `probar_confirmar_carga.sh`;
   - la evidencia;
   - el PR;
   - la fusión con tu aprobación;
   - el despliegue;
   - tu comprobación;
   - el PR de evidencia con `Closes #30`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| El módulo `:plataforma:lenguaje-cargas` | La comparación, la huella y el «todo o nada» son iguales para los tres dueños, y Cargas no debe conocer sus tipos (principio V). | Un puerto en cada dueño repite los registros tres veces y le da a Cargas tres adaptadores. El puerto en Cargas haría a los dueños depender de Cargas. |
| La huella de la vista previa, además del hash | La historia pide aplicar solo lo que se revisó. El mismo archivo da otra vista previa si los datos cambiaron a mano entre los pasos. | Solo el hash: aplicaría algo distinto de lo revisado sin avisar. |
| `Matricula` en Académico | La carga 10 necesita un lugar donde quedar al confirmarse, y el glosario la pone en Datos maestros. | Dejar la carga 10 sin aplicar: las 10 no quedarían confirmadas (aclaración 1). |
