# Implementation Plan: Ver mis firmas pendientes y descargar el PDF vigente

**Branch**: `047-firmas-pendientes` | **Date**: 2026-11-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/047-firmas-pendientes/spec.md`

## Summary

**Al generar el documento firmable**, Firma guarda también el firmante esperado de cada paso, la misma persona cuyo nombre imprime (research §1):
- el usuario designado para «Elaborado por»;
- la identificación del docente del cargo vigente para «Revisado por» y «Aprobado por».

**La cuenta de usuario guarda ahora una identificación opcional y única** (Usuarios, V7). Los usuarios de la demo del Rectorado y del Vicerrectorado reciben la de su cargo en los datos de prueba (research §2).

**Dos operaciones nuevas de Firma, con `DISTRIBUTIVO:FIRMA`:**
- **`GET /api/firma/pendientes`:** los documentos generados cuyo paso en turno es del usuario. El paso en turno es el primero sin firma; hoy, el primero (research §3).
- **`GET /api/firma/documentos/{id}/archivo`:** el archivo vigente, hoy el original. Solo para el firmante en turno; a cualquier otro, el 403 común. Antes de entregar, se recalcula el SHA-256.

Para un paso por identificación, Firma pide la del usuario a Usuarios por una ruta interna.

**En la SPA**, la pantalla nueva «Pendientes de firma». El script de punta a punta suma dos pasos.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash

**Primary Dependencies**: las que ya están. Sin dependencias nuevas.

**Storage**:
- PostgreSQL:
  - esquema `firma`, migración V3 (`firmante_esperado`, solo `insert` y `select`);
  - esquema `usuarios`, migración V7 (`usuario.identificacion`, única cuando existe).
- Archivos: el volumen de D-88. Solo se lee `documentos/<sha256>.pdf`.

**Testing**:
- **Dominio:**
  - `FirmanteEsperado.reconoce`: por usuario, por identificación, y que el rol no basta;
  - `DocumentoFirmable.pasoEnTurno` (con órdenes firmados vacíos y con algunos) y `archivoVigente`;
  - `Usuario` con identificación.
- **Aplicación (Mockito):**
  - `DocumentoFirmableServicio`:
    - al generar, registra los firmantes con el documento; un firmante sin persona falla como sin nombre;
    - `pendientes`: pide la identificación solo si hace falta; no lista los no generados ni los que no son del usuario;
    - `archivo`: en turno lo entrega; fuera de turno, inexistente o no generado lanza `SinTurnoDeFirma`; con SHA-256 distinto o archivo faltante lanza `ArchivoNoDisponible`;
  - `CargaUsuariosDemo` con la identificación: la pone, la corrige, es idempotente y rechaza una repetida.
- **Adaptadores:**
  - `FirmantesDesdeProcesos` devuelve la persona;
  - `IdentificacionDesdeUsuarios` con el servidor de pruebas entre procesos (200 y 404);
  - `AlmacenDeDocumentosEnDisco.leer`.
- **Integración (PostgreSQL real):**
  - la migración V3 de Firma: el `check` de exactamente una persona y que el rol del proceso no actualiza ni borra;
  - la V7 de Usuarios: la unicidad;
  - `RepositorioDocumentosJdbc` registra y lee los firmantes con el documento.
- **Contrato:** Firma (las dos operaciones y la descarga binaria con sus cabeceras) y Usuarios (la ruta interna).
- **Permisos:** `PermisosFirmaTest` con las cuatro operaciones.
- **Arquitectura:** ArchUnit y `ModulosTest` sin cambios de dependencias entre módulos. Firma pide a Usuarios por ruta interna, no por interfaz: están en procesos distintos.
- **SPA:** Vitest de la pantalla y de la descarga. Playwright con axe-core, con pendientes y sin ellos.
- **Punta a punta:** `probar_documento_firmable.sh`, pasos 7 y 8.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Firma (proceso `distributivo`), Usuarios (proceso `identidad`), la SPA y un script.

**Performance Goals**: SC-001, descargar en menos de 1 minuto desde el inicio de sesión. La lista hace a lo sumo una consulta entre procesos; la descarga lee un archivo de unos cientos de KB.

**Constraints**:
- **Principio V:** Firma no lee las tablas de Usuarios; usa la ruta interna con token de proceso.
- **Principio IX:** la identificación no viaja en el token ni en las respuestas públicas, y el 403 no dice por qué.
- **Principio VII:** `firmante_esperado` es inmutable para el rol del proceso, y el archivo se lee, nunca se reescribe.
- **D-83:** la consulta a Usuarios va fuera de transacciones.

**Scale/Scope**:
- Firma:
  - 2 operaciones HTTP;
  - 1 tabla;
  - 2 puertos de entrada y 2 de salida nuevos;
  - 1 código de error.
- Usuarios:
  - 1 columna;
  - 1 ruta interna;
  - 1 código de error;
  - la carga de la demo.
- SPA: 1 pantalla y 1 entrada de menú.
- Script: 2 pasos.
- 1 decisión (D-102).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | El turno y el reconocimiento del firmante, en el dominio de Firma; la lectura del archivo y la identificación, detrás de puertos. | ✅ |
| II. DDD | Firma es dueña del documento, su cadena y sus firmantes. Usuarios es dueño de la cuenta y su identificación. | ✅ |
| III. Lenguaje ubicuo | «Firmante en turno», «paso en turno», «archivo vigente», «Pendientes de firma». | ✅ |
| IV. API primero | Las dos operaciones de Firma, la ruta interna de Usuarios y los dos códigos se escriben antes del código. | ✅ |
| V. Límites | Firma pide la identificación por ruta interna con token de proceso. No lee tablas ajenas. | ✅ |
| VI. Las reglas son datos | El turno sigue el orden del árbol de firmas de la configuración (RF-58). | ✅ |
| VII. Inmutabilidad y trazabilidad | Los firmantes se fijan con el documento y no cambian. El archivo se verifica por su SHA-256 antes de entregarlo. | ✅ |
| VIII. Pruebas | Primero el dominio y la aplicación, viéndolos fallar; integración para las migraciones; script de punta a punta. | ✅ |
| IX. Seguridad | El permiso y el turno se comprueban en el servidor. 403 común sin revelar si el documento existe. La identificación queda fuera del token y de las respuestas públicas. | ✅ |
| X. Simplicidad | Sin relleno de documentos viejos ni administración de usuarios. La regla del turno, sin la tabla de firmas todavía. | ✅ |
| XI. Diseño pragmático | La identificación en la cuenta, hasta que F03-H04 traiga la entidad docente. | ✅ |
| XII. JavaDoc | Los puertos nuevos, la regla del turno, `reconoce` y por qué el firmante se fija al generar. | ✅ |
| XIII. Errores | `FIRMA_ARCHIVO_NO_DISPONIBLE` y `USUARIOS_SIN_IDENTIFICACION`, con sus 8 columnas. | ✅ |
| XIV. Validación | El uuid del documento, en la ruta; la identificación, con el patrón de Docentes. | ✅ |

**Post-diseño:** sin cambios. D-102 queda registrada antes de implementar, con research §1 y §2.

## Project Structure

### Documentation (this feature)

```text
specs/047-firmas-pendientes/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── firma.md
│   ├── identidad.md
│   ├── spa.md
│   └── demostracion.md
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/firma/openapi.yaml               listarFirmasPendientes, descargarArchivoVigente
contratos/usuarios/openapi.yaml            /interno/usuarios/{usuario}/identificacion
contratos/comun/errores.md                 2 códigos

backend/servicios/firma/
├── dominio/                               FirmanteEsperado, DocumentoFirmable (pasoEnTurno, archivoVigente),
│                                          Firmante (con la persona), SinTurnoDeFirma, ArchivoNoDisponible
├── puertos/entrada/                       ConsultarFirmasPendientes, DescargarArchivoVigente, FirmaPendiente
├── puertos/salida/                        IdentificacionDeUsuarios; AlmacenDeDocumentos.leer;
│                                          FirmantesDelPeriodo con la persona
├── aplicacion/                            DocumentoFirmableServicio
├── adaptadores/entrada/                   FirmasPendientesControlador, ErroresFirma
├── adaptadores/salida/                    IdentificacionDesdeUsuarios, FirmantesDesdeProcesos,
│                                          RepositorioDocumentosJdbc, AlmacenDeDocumentosEnDisco
└── src/main/resources/db/migration/firma/ V3__firmante_esperado.sql

backend/servicios/usuarios/                Usuario.identificacion, CargaUsuariosDemo, usuarios-demo.yaml,
                                           IdentificacionControlador (ruta interna), V7__identificacion.sql

frontend/src/navegacion/Marco.tsx, rutas.tsx
frontend/src/pantallas/firmas/             FirmasPendientes.tsx, consultas.ts
frontend/e2e/firmas-pendientes.spec.ts

deploy/probar_documento_firmable.sh        pasos 7 y 8
docs/decisiones.md                         D-102
```

**Structure Decision**: Firma suma la consulta de pendientes y la descarga a su servicio de documentos. Usuarios agrega un dato a la cuenta y una ruta interna.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| La identificación del YAML no coincide con los cargos de los datos de prueba, y nadie queda en turno en F2 y F3. | Tomarlas de `08_cargos_autoridades.csv`. Una prueba compara el YAML con ese CSV. |
| Un perfil local con un documento de F13-H02 no muestra pendientes. | Está documentado (research §1); `down -v` y otra vez el script. |
| La descarga de un binario rompe el contrato generado o el manejador de errores, que esperan JSON. | Prueba de contrato de la descarga con sus cabeceras. Un error antes de escribir el cuerpo sale como JSON del catálogo. |
| `CargaUsuariosDemo` choca con la unicidad si dos usuarios del YAML tienen la misma identificación. | Validarlo al leer el YAML y fallar al arrancar, con prueba. |
| La limpieza de los scripts no borra `firmante_esperado` y el siguiente encuentra filas viejas, o no puede borrar `documento_firmable` por la clave foránea. | T017: borrarla primero en `probar_documento_firmable.sh` y `probar_aprobacion.sh`, y contarla en el paso 0 y al final. |
| El rol del proceso de Firma no puede leer `firmante_esperado` si la V3 olvida los permisos. | Prueba de integración del esquema con un rol que no es superusuario, como en V2. |

## Complexity Tracking

Sin violaciones que justificar.
