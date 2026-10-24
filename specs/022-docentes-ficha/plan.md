# Implementation Plan: Mantener los docentes con su ficha académica

**Branch**: `022-docentes-ficha` | **Date**: 2026-10-24 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/022-docentes-ficha/spec.md` (issue #38, F03-H04)

## Summary

- **El primer contrato de Docentes:** el servicio deja de ser un esqueleto. Gana `contratos/docentes/openapi.yaml`, su tabla `docente` con la ficha académica dentro de la misma fila (las listas en `jsonb`) y su `registro_cambio`. Nada se borra: un docente cambia de estado laboral.
- **Las reglas:** las de la carga 6. Entre ellas:
  - la cédula con su dígito verificador;
  - la identificación y el correo únicos;
  - las horas según la dedicación (RN-01);
  - la categoría según el tipo de personal;
  - el fin de contrato del ocasional;
  - los campos de conocimiento y los rangos de la ficha.

  Viven en el *record* `Docente` y su `FichaAcademica`.
- **Académico publica sus referencias:** un puerto nuevo, `ConsultarReferencias`, da las carreras, las asignaturas y los campos amplios. Docentes lo usa para validar la carrera base y las asignaturas afines, y para mostrar sus nombres. Es el mismo camino que Académico ya usa con Normativa.
- **Búsqueda:** por nombre o identificación, sin mayúsculas ni tildes, filtrada en memoria (decenas o cientos de docentes).
- **Permisos:**
  - escriben `[DOCENTES:ESCRITURA, CATALOGOS:ESCRITURA, DISTRIBUTIVO:ESCRITURA]`: Talento Humano, el Administrador y el Vicerrector;
  - leen `[DOCENTES:LECTURA, DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`: además, el Responsable de consolidación y los coordinadores (D-70, aclaración 1);
  - sin decisiones nuevas.
- **Contrato:** 6 operaciones y 2 códigos `DOCENTES_*`.
- **SPA:** la pantalla «Docentes», con la lista, la ficha y el formulario con listas editables.
- **Punta a punta:** `deploy/probar_docentes.sh`, con los 47 docentes de prueba.
- **Glosario:** propone cambiar «Tipo de personal» y agregar «Categoría de escalafón», «Carrera base», «Asignatura afín declarada» y «Estado laboral». El autor lo aprueba antes del código (research §11).

Los detalles y las alternativas están en [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · SQL (PostgreSQL 18, Flyway) · Bash

**Primary Dependencies**: sin dependencias nuevas. Docentes agrega a su `build.gradle` las que ya usa Académico:
- `mdd.contrato`;
- `spring-boot-starter-jdbc`;
- `jackson-databind`, para el `jsonb`;
- `spring-security-core`;
- `project(':servicios:academico')`.

**Storage**: esquema `docentes`, `V2__docentes.sql`: `docente` y `registro_cambio` ([data-model.md](data-model.md)).

**Testing**:
- JUnit y AssertJ en el dominio, sin dobles, con las cargas 02, 04 y 06 de prueba;
- `ValoresPermitidosTest` contra el diccionario;
- Mockito en la aplicación y Testcontainers en el repositorio;
- swagger-request-validator en el contrato y `VerificadorDePermisos` en los permisos;
- `ModulosTest` del proceso `datos`;
- Vitest, y Playwright con axe;
- `probar_docentes.sh`.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Docentes y un puerto de Académico (`datos`), el contrato, la SPA y un script.

**Performance Goals**: buscar entre 500 docentes por debajo de 300 ms. SC-001: buscar y abrir una ficha en menos de 30 s.

**Constraints**:
- los valores permitidos en un solo lugar, comparados con el diccionario (principio VI);
- los campos amplios, solo los de Académico (principio VI);
- nada se borra y cada cambio queda registrado (principio VII);
- datos ficticios y sin datos personales en los logs ni en los mensajes (principio IX);
- escritura con tres permisos alternativos (principio IX, D-70);
- sin claves foráneas entre esquemas (principio V);
- 60 % de líneas por módulo.

**Scale/Scope**: 2 tablas; 6 operaciones y 2 códigos; 1 puerto publicado en Académico; 1 pantalla; 1 script.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Reglas en `Docente`, su ficha y `Referencias`; la aplicación orquesta; los adaptadores traducen. | ✅ |
| II. DDD según el subdominio | Docentes es de soporte: valores con sus reglas, sin agregado ni eventos (research §1). | ✅ |
| III. Lenguaje ubicuo | Docente, Tipo de personal, Dedicación, Condición especial y Ficha académica, del glosario. Los cinco cambios de research §11 se aprueban y aplican antes del código. | ⏳ pendiente de aprobación |
| IV. API primero | `contratos/docentes/openapi.yaml` y los 2 códigos antes del código. | ✅ |
| V. Límites | Docentes usa Académico solo por su API publicada dentro del proceso (`puertos.entrada`), sin claves foráneas entre esquemas; `ModulosTest` lo comprueba (research §4). | ✅ |
| VI. Las reglas son datos | Enumeraciones comparadas con la carga 6 del diccionario; los campos amplios, de Académico (research §9). | ✅ |
| VII. Inmutabilidad y trazabilidad | Nada se borra (estado laboral); 3 acciones en `registro_cambio` con el valor anterior y el nuevo (research §8). | ✅ |
| VIII. Pruebas | Primero las del dominio, sin dobles, con los 47 docentes de prueba como caso grande. | ✅ |
| IX. Seguridad y datos personales | Escritura con permisos alternativos validados en el servidor (D-70). Datos de prueba ficticios. Ni los logs ni los mensajes llevan cédulas ni correos (research §8). | ✅ |
| X. Simplicidad | La ficha en la fila del docente; la búsqueda en memoria; sin columnas normalizadas ni extensiones (research §2, §6). | ✅ |
| XI y XII | Un puerto publicado nuevo en Académico, justificado (research §4); JavaDoc en español. | ✅ |
| XIII. Errores | 2 códigos en el catálogo; los datos inválidos, 400 con su campo y su elemento. | ✅ |
| §6.4 Decisiones | Ninguna nueva: aplica D-65, D-69 y D-70. Siguiente número libre: D-72 · ADR-022. | ✅ |

**Resultado del gate antes de la fase 0:** pasa, salvo el principio III. Los cambios del glosario se proponen en research §11 y se aplican con la aprobación del autor, antes del código (fase 1 de la implementación).

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/022-docentes-ficha/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1
├── quickstart.md            # fase 1
├── contracts/
│   └── docentes.md          # operaciones, esquemas y errores
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
contratos/docentes/openapi.yaml, contratos/comun/errores.md      # 6 operaciones y 2 códigos
backend/servicios/docentes/build.gradle
backend/servicios/docentes/src/main/java/ec/mdd/docentes/
├── dominio/                 # Docente, FichaAcademica y sus títulos, cursos y experiencia, Identificacion,
│                            # Referencias, BusquedaDeDocentes y las enumeraciones
├── puertos/{entrada,salida}/# ConsultarDocentes, MantenerDocentes, RepositorioDocentes, RegistroDeCambios,
│                            # ReferenciasAcademicas
├── aplicacion/              # servicios y su configuración
└── adaptadores/             # DocentesControlador, ErroresDocentes, RepositorioDocentesJdbc,
                             # RegistroDeCambiosJdbc, AcademicoDesdePuertos
backend/servicios/docentes/src/main/resources/db/migration/docentes/V2__docentes.sql
backend/servicios/docentes/src/test/resources/docentes/datos-de-prueba/   # cargas 02, 04 y 06
backend/servicios/academico/src/main/java/ec/mdd/academico/
├── puertos/entrada/         # ConsultarReferencias y sus records
└── aplicacion/              # ConsultarReferenciasServicio
frontend/src/pantallas/docentes/                                # «Docentes»
frontend/src/api/cliente.ts, frontend/package.json               # apiDocentes y su tipo generado
frontend/src/navegacion/Marco.tsx, frontend/src/rutas.tsx, frontend/src/sesion/funcionalidades.ts
frontend/e2e/docentes.spec.ts
deploy/probar_docentes.sh, .github/workflows/perfil-consolidado.yml
```

**Structure Decision**: los docentes son Datos maestros en su propio servicio, el 4 (ERS §9). Académico solo publica sus referencias.

## Fases de implementación (guía para /speckit-tasks)

1. **Glosario:** los cinco cambios de research §11, con la aprobación del autor y la copia `modelo_dominio.md.antes-F03-H04`.
2. **Contrato y catálogo:**
   - `contratos/docentes/openapi.yaml` con las 6 operaciones;
   - los 2 códigos;
   - `mdd.contrato` en Docentes;
   - el tipo generado y `apiDocentes` en la SPA.
3. **Académico:** `ConsultarReferencias`, con su prueba.
4. **Dominio,** con las pruebas primero: `Docente`, la ficha, `Identificacion`, `Referencias` y `BusquedaDeDocentes`, con `ValoresPermitidosTest` y los 47 docentes de prueba.
5. **Persistencia:** V2 y los dos repositorios, con Testcontainers.
6. **Aplicación y API:**
   - los servicios y el controlador;
   - `AcademicoDesdePuertos`;
   - las pruebas de contrato y de permisos;
   - `ModulosTest` del proceso `datos`.
7. **SPA:** «Docentes», con Vitest y Playwright.
8. **Punta a punta:** `probar_docentes.sh`, en el perfil y en `perfil-consolidado`.
9. **Cierre:**
   - evidencia y PR;
   - fusión con la aprobación del autor y despliegue;
   - la comprobación con `datos.demo` y `consolidacion.demo`;
   - el PR de evidencia con `Closes #38`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| `ConsultarReferencias` en Académico | Docentes tiene que validar la carrera base, las asignaturas afines y el campo amplio, y mostrar sus nombres, sin tocar el esquema ni el dominio de Académico. | Usar `ConsultarCarreras`: devuelve tipos del dominio de Académico, que `ModulosTest` prohíbe a Docentes. Copiar los catálogos: dos lugares para el mismo dato. |
| La ficha en `jsonb` | RF-17 la pone dentro del registro del docente, y se lee y se guarda entera. | Cuatro tablas hijas: joins y borrados de filas al corregir, sin consulta que los necesite. |
