# Implementation Plan: Consultar la bitácora

**Branch**: `040-consultar-bitacora` | **Date**: 2026-11-07 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/040-consultar-bitacora/spec.md`

## Summary

Usuarios suma una consulta de solo lectura sobre la bitácora de F17-H01, `GET /api/usuarios/bitacora`, protegida con `BITACORA:LECTURA` (Vicerrector académico y Administrador del sistema en la matriz D-16), y la lista de acciones que la llena. Se filtra por fechas en hora de Ecuador, usuario, tipo de cambio (servicio y acción), identificador del objeto y periodo, con páginas estables gracias a un `corte` (research §2). La SPA suma la pantalla «Bitácora», con los filtros validados con las reglas del contrato.

Por la nota de F17-H01 y D-94, seis acciones de Generación, Académico y Roles dejan de guardar y de registrar cuando nada cambió. Las demás ya lo hacían (research §6).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19, Vite) · Bash · SQL (PostgreSQL)

**Primary Dependencies**: las que ya hay. Backend: el generador de OpenAPI, `JdbcClient` y Spring Security de cada servicio. SPA: MUI 9, React Query, `openapi-fetch`, `react-hook-form` y `zod`. Ninguna librería nueva.

**Storage**: `usuarios.bitacora` (V5), solo lectura. Sin migraciones; una `V6` con el índice `(usuario, momento)` solo si la medición de SC-003 lo pide (research §5).

**Testing**:
- **Dominio de Usuarios:** `FiltroDeBitacora` y `PaginaPedida` sin dobles: cada regla, sus límites y el intervalo en hora de Ecuador.
- **Aplicación:** `ConsultarBitacoraServicio` con Mockito para el repositorio. Las seis acciones de D-94: sin cambios no guardan ni registran; con un cambio, sí; el texto solo, con `textoCambiado`.
- **Adaptadores:** `RepositorioBitacoraJdbc` con PostgreSQL real (Testcontainers): cada filtro, la combinación, el orden, el corte, el identificador anidado y numérico, y 10 000 entradas en menos de 2 s.
- **Contrato:** `BitacoraContratoTest`: parámetros válidos e inválidos en sus límites, 400 con detalles, 401, 403 para los 8 roles sin permiso y 200 para los 2 con permiso. `BitacoraSinBorradoContratoTest` sigue en verde.
- **Arquitectura:** ArchUnit y Spring Modulith de Usuarios, sin cambios.
- **SPA:** Vitest para la pantalla y la validación; Playwright para US1-1 y US2; la prueba de accesibilidad suma la pantalla.
- **Punta a punta:** `deploy/probar_consulta_bitacora.sh`.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: un servicio con una consulta nueva (Usuarios), tres servicios con una regla en seis casos de uso, la SPA con una pantalla y un script.

**Performance Goals**: la primera página de una consulta filtrada en menos de 2 s con 10 000 entradas (SC-003).

**Constraints**:
- La consulta solo lee: ningún método nuevo escribe en la bitácora (FR-006).
- Ningún valor de un filtro se concatena en el SQL: todos van como parámetros, también el identificador de `jsonb_path_exists` (OWASP A03).
- La consulta no reconstruye datos personales de un docente (D-91).
- La zona horaria se fija en `America/Guayaquil`, en Usuarios y en la SPA; no depende del contenedor ni del navegador.
- Las seis operaciones de D-94 responden con éxito, como hoy, con el elemento tal como quedó guardado (FR-013).

**Scale/Scope**:
- 2 operaciones nuevas y 4 esquemas en el contrato de Usuarios;
- 2 objetos de valor y 1 caso de uso en Usuarios;
- 6 casos de uso con la regla de D-94, en 3 servicios;
- 1 pantalla, 1 opción del menú y 1 ruta en la SPA;
- 1 script nuevo en el workflow;
- 0 códigos de error nuevos, 0 tablas nuevas;
- 1 decisión (D-94), ya registrada.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | `FiltroDeBitacora` y `PaginaPedida` son dominio puro; la consulta SQL vive en el adaptador de salida; el controlador solo traduce. | ✅ |
| II. DDD según el subdominio | Usuarios es genérico: un caso de uso de lectura detrás de su puerto, sin agregado. La regla de D-94 vive en los casos de uso de cada servicio, que ya tienen el elemento. | ✅ |
| III. Lenguaje ubicuo | Bitácora y `EntradaBitacora` del glosario §7; «filtro», «página» y «corte» son términos técnicos de la consulta, no del negocio. Sin términos nuevos en el glosario. | ✅ |
| IV. API primero | Las dos operaciones y sus esquemas en `contratos/usuarios/openapi.yaml` antes del código; la SPA usa el cliente generado. | ✅ |
| V. Límites | La bitácora solo la lee Usuarios. Ningún servicio lee la tabla de otro. | ✅ |
| VI. Las reglas son datos | No aplica. | — |
| VII. Inmutabilidad y trazabilidad | La consulta no modifica nada. D-94 hace que cada entrada sea un cambio real, sin perder ninguno (se compara el elemento entero). | ✅ |
| VIII. Pruebas | Primero el filtro y la regla de cada acción, viéndolos fallar; contrato, integración con PostgreSQL, SPA y script. 60 % por módulo. | ✅ |
| IX. Seguridad | Permiso en el backend (`@PreAuthorize`, ADR-021); el 403 no dice qué falta; SQL parametrizado; sin datos personales. | ✅ |
| X. Simplicidad y alcance | RF-44 es Must. Sin infraestructura ni librerías nuevas; sin índices hasta que la medición los pida. La regla nueva tiene su decisión (D-94). | ✅ |
| XI. Diseño pragmático | Sin interfaces nuevas salvo el puerto de entrada del caso de uso; dos métodos más en el puerto existente. | ✅ |
| XII. JavaDoc | En el puerto, el caso de uso, el filtro y la página. | ✅ |
| XIII. Errores | Sin códigos nuevos: `COMUN_SOLICITUD_INVALIDA` con detalles, `SEGURIDAD_ACCESO_NO_AUTORIZADO` y `401` del catálogo, cubiertos por la prueba de contrato. | ✅ |
| XIV. Validación de entradas | Cada filtro con su regla definida (usuario y periodo de F17-H01, identificador revisado en los contratos, research §4), en el contrato, el dominio y la SPA, con casos en los límites. | ✅ |

**Post-diseño:** sin cambios. D-94 se registró antes de implementar y reemplaza el supuesto «Mismo valor» de F17-H01, con la nota en su especificación.

## Project Structure

### Documentation (this feature)

```text
specs/040-consultar-bitacora/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── usuarios.md
│   ├── spa.md
│   ├── sin-cambios.md
│   └── demostracion.md
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/usuarios/openapi.yaml       consultarBitacora, listarAccionesDeBitacora y sus esquemas

backend/servicios/usuarios/
├── dominio/                          FiltroDeBitacora, PaginaPedida, FiltroDeBitacoraInvalido
├── puertos/entrada/                  ConsultarBitacora, PaginaDeEntradas, EntradaConsultada
├── puertos/salida/                   RepositorioBitacora: consultar y acciones
├── aplicacion/                       ConsultarBitacoraServicio
├── adaptadores/entrada/              BitacoraControlador; ErroresUsuarios traduce el filtro inválido
├── adaptadores/salida/               RepositorioBitacoraJdbc: las dos consultas
└── infraestructura/                  ConfiguracionUsuarios con el caso de uso

backend/servicios/generacion/aplicacion/   ProponerDocenteServicio, AsignarActividadServicio,
                                           JustificarInconsistenciaServicio (D-94)
backend/servicios/academico/aplicacion/    MantenerMatriculasServicio (D-94)
backend/servicios/roles/aplicacion/        GestionarRolesServicio, AsignarCarreraServicio (D-94)

frontend/src/pantallas/bitacora/      Bitacora.tsx, consultas.ts, validacionDeBitacora.ts, nombres.ts
frontend/src/navegacion/Marco.tsx     la opción «Bitácora»
frontend/src/rutas.tsx                la ruta /bitacora
frontend/e2e/bitacora.spec.ts         US1-1 y US2

deploy/probar_consulta_bitacora.sh    el script de la historia
.github/workflows/perfil-consolidado.yml   su paso, después de probar_bitacora.sh
```

**Structure Decision**: no hay módulos nuevos. Todo cae en servicios, en la SPA y en el workflow que ya existen.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| La regla de D-94 en seis casos de uso de tres servicios hace crecer una historia M. | Es la misma comparación en cada uno, con su prueba; va en una fase propia de las tareas, después de la consulta, y puede ir en su propio pull request. |
| El filtro por identificador da falsos positivos con valores cortos. | Documentado (research §4); se acota con el tipo de cambio y el periodo. Nunca deja fuera una entrada que lo tiene. |
| `probar_bitacora.sh` podría depender de un guardado con el mismo valor. | Sus cambios usan valores distintos (research §9); una tarea lo corre dos veces seguidas, como la CI. |
| La consulta con `jsonb_path_exists` sobre toda la tabla crece con la bitácora. | Se mide con 10 000 entradas; el índice `(usuario, momento)` y el filtro por fechas acotan la mayoría de las consultas. |

## Complexity Tracking

Sin violaciones que justificar.
