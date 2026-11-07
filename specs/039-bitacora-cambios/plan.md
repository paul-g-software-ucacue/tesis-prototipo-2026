# Implementation Plan: Registrar cada cambio en la bitácora

**Branch**: `039-bitacora-cambios` | **Date**: 2026-11-05 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/039-bitacora-cambios/spec.md`

## Summary

La bitácora es una tabla de Usuarios en la que solo se puede insertar, protegida con disparadores y sin permisos de modificación. Se alimenta desde el puerto `RegistroDeCambios` que ya tienen Generación, Académico, Docentes, Normativa y Roles: su adaptador, además de la fila local, publica un evento `<servicio>.EntradaBitacora` en la misma transacción, por el camino de B00-H05. Así las 50 acciones de esos servicios, también las que aplica una carga, llegan a la bitácora sin tocarlas una por una (D-90). Validación OCS publica la carga de la configuración del flujo.

Cada registro de cambio suma `anterior` y `nuevo` explícitos, según la clase de su acción (crea, cambia o quita). Roles, en el mismo proceso que la bitácora, entrega sin HTTP con un receptor local (ADR-024). El evento de prueba de B00-H05 se retira, y `probar_bitacora.sh` reemplaza a `probar_eventos.sh`.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · Bash · SQL (PostgreSQL)

**Primary Dependencies**: las que ya hay. `:plataforma:eventos` (Spring Modulith `events-api`, `events-jdbc` y `events-jackson`) entra en `identidad`, `servicios:docentes` y `servicios:normativa`. Ninguna librería nueva.

**Storage**:
- Usuarios `V5__bitacora.sql`: la tabla `bitacora`, sus disparadores y el `REVOKE`.
- Generación `V7__sin_evento_de_prueba.sql`: retira `evento_prueba`.
- `eventos_identidad`: ya existe; lo migra la plataforma al arrancar `identidad`.

**Testing**:
- **Dominio de Usuarios:** `EntradaBitacora` sin dobles: invariantes, datos sensibles a cualquier profundidad, límites.
- **Registros de cambio:** la clase de cada acción, en los cinco servicios, con casos válidos e inválidos.
- **Aplicación:** Mockito para los puertos. Un cambio rechazado no registra ni publica.
- **Adaptadores:** cada `RegistroDeCambiosJdbc` publica en la misma transacción. `integrationTest` con PostgreSQL real: el evento queda en `eventos_<proceso>` solo si el cambio se confirma, y la tabla `bitacora` rechaza `UPDATE`, `DELETE` y `TRUNCATE`.
- **Plataforma:** la entrega local, su fallo y su reintento.
- **Contrato:** el receptor con los seis tipos, el código nuevo y que ninguna operación de `contratos/` borra la bitácora.
- **Arquitectura:** ArchUnit y Spring Modulith; Roles no depende de Usuarios.
- **Punta a punta:** `deploy/probar_bitacora.sh`, con los procesos reales.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web. Esta historia es solo de backend: 6 servicios, la plataforma de eventos, 2 aplicaciones y un script. La SPA no cambia.

**Performance Goals**: el cambio del usuario no se demora más de 0,5 s por la bitácora (SC-003); con `identidad` de vuelta, las entradas pendientes llegan en menos de 2 minutos (SC-002).

**Constraints**:
- Ninguna transacción cruza dos servicios: el cambio y su evento van juntos; la entrada, en la transacción de Usuarios.
- El formato local de `detalle` no cambia: lo lee `RepositorioConsolidacionJdbc`.
- Los registros locales siguen igual.
- La bitácora no guarda contraseñas, tokens ni el texto de una justificación, ni los valores de los datos personales de un docente (D-91).

**Scale/Scope**:
- 50 acciones en 5 servicios, más 1 en Validación OCS;
- 6 tipos de evento sobre un esquema de contenido;
- 1 tabla nueva y 1 retirada;
- 1 código de error nuevo;
- 1 operación pública retirada (`eventos-prueba`);
- 1 script que reemplaza a otro;
- 1 decisión (D-90) y 1 ADR (ADR-024).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | `EntradaBitacora` es dominio puro; la publicación vive en los adaptadores de salida; el disparador, en la migración. | ✅ |
| II. DDD según el subdominio | Usuarios es genérico: un CRUD de solo inserción detrás de su puerto, sin agregado elaborado. Los servicios centrales solo cambian su adaptador y su registro de cambio. | ✅ |
| III. Lenguaje ubicuo | `EntradaBitacora` y sus campos (quién, qué, cuándo, objeto, anterior y nuevo) salen del glosario §7.6 y del lenguaje publicado §4; no hay términos nuevos. | ✅ |
| IV. API primero | `EntradaBitacora` en `contratos/usuarios/openapi.yaml`, el lenguaje publicado de §4 del modelo de dominio, el receptor y el código nuevo en sus contratos, antes del código. | ✅ |
| V. Límites y fallos entre procesos | Ningún servicio lee ni escribe tablas de otro. Evento en la transacción, registro persistente, reenvío y receptor idempotente. Dentro de `identidad`, la interfaz de aplicación (ADR-024). | ✅ |
| VI. Las reglas son datos | No aplica: no hay reglas de la política. | — |
| VII. Inmutabilidad y trazabilidad | Es el objetivo: solo inserción, disparadores y `REVOKE`. | ✅ |
| VIII. Pruebas | Primero el dominio y la clase de cada acción, viéndolos fallar; 60 % por módulo; script de punta a punta. | ✅ |
| IX. Seguridad y datos personales | Sin secretos (validado en el emisor y en el receptor) y escritura solo con token de proceso o local. Los nombres, el correo, el teléfono y la condición especial de un docente no llegan con sus valores (FR-014, D-91, RNF-SEG-06). | ✅ |
| X. Simplicidad | Sin colas ni tablas de réplica: el mecanismo existente y un punto de publicación por servicio. Se retira el evento de prueba. | ✅ |
| XI. Diseño pragmático | Una sola interfaz nueva (`ReceptorLocal`), que marca un límite real. Sin capas nuevas. | ✅ |
| XII. JavaDoc | En los puertos, en `ReceptorLocal` y en `EntradaBitacora`. | ✅ |
| XIII. Catálogo de errores | `USUARIOS_ENTRADA_BITACORA_INVALIDA` con sus 8 columnas; `COMUN_EVENTO_NO_RECONOCIDO` actualiza su causa. | ✅ |
| XIV. Validación de entradas | El contenido del evento tiene patrones y largos en el contrato y en el dominio. El patrón de `usuario` se confirma contra la definición de Usuarios (data-model). No hay formularios nuevos. | ✅ con tarea |

**Post-diseño:** sin cambios. Se registró ADR-024 antes de implementar, porque modifica ADR-018.

## Project Structure

### Documentation (this feature)

```text
specs/039-bitacora-cambios/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── eventos.md
│   ├── usuarios.md
│   ├── generacion.md
│   ├── plataforma-eventos.md
│   └── demostracion.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/usuarios/openapi.yaml       EntradaBitacora (lenguaje publicado)
contratos/comun/errores.md            1 código nuevo; causa de COMUN_EVENTO_NO_RECONOCIDO
contratos/usuarios/openapi.yaml       descripción del receptor y el 400 nuevo
contratos/generacion/openapi.yaml     sin eventos-prueba

backend/plataforma/eventos/           ReceptorLocal; ClienteDeEventos entrega local o HTTP

backend/servicios/usuarios/
├── dominio/                          EntradaBitacora, EntradaBitacoraInvalida
├── aplicacion/                       RegistrarEventoRecibidoServicio con los seis tipos
├── puertos/salida/                   RepositorioBitacora
├── adaptadores/                      RepositorioBitacoraJdbc, ReceptorLocalDeUsuarios, ErroresUsuarios
└── resources/db/migration/usuarios/  V5__bitacora.sql

backend/servicios/{generacion,academico,docentes,normativa,roles}/
├── puertos/salida/                   el registro de cambio suma anterior y nuevo, con la clase de cada acción
├── aplicacion/                       cada acción completa su anterior y su nuevo
└── adaptadores/salida/               RegistroDeCambiosJdbc también publica su EntradaBitacora

backend/servicios/validacionocs/      CambioDeFlujo y su publicador; CargarConfiguracionServicio lo usa
backend/servicios/generacion/         se retira el evento de prueba; V7__sin_evento_de_prueba.sql

backend/apps/identidad/               :plataforma:eventos, mdd.eventos.proceso
backend/apps/datos/                   destino usuarios

deploy/probar_bitacora.sh             reemplaza a probar_eventos.sh
.github/workflows/perfil-consolidado.yml   cambia el script
```

**Structure Decision**: no hay módulos nuevos. Todo cae en servicios, en la plataforma y en aplicaciones que ya existen.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| El tamaño: la historia es M y toca 6 servicios. | El cambio por servicio es mecánico y se ordena por servicio en las tareas; Generación primero, porque tiene el escenario del issue. Si no cabe en la semana, Académico (23 acciones) es el candidato a partir en otra historia, con una decisión nueva. |
| Completar `anterior` en acciones que hoy no lo guardan exige leer el estado previo. | La mayoría ya lo lee para validar. Las tareas lo revisan acción por acción con la clase como prueba. |
| El dueño de la tabla puede quitar el disparador con DDL. | Riesgo residual documentado (research §3); la base no se expone. |
| Las publicaciones completadas crecen más rápido. | Sin purga en el prototipo (ADR-018); se observa con `medir_memoria.sh` y el tamaño de la base. |

## Complexity Tracking

Sin violaciones que justificar.
