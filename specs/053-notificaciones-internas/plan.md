# Implementation Plan: Recibir notificaciones internas por cada cambio de estado

**Branch**: `053-notificaciones-internas` | **Date**: 2026-11-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/053-notificaciones-internas/spec.md`

## Summary

**Quién publica** (research §1, §3):
- Las 8 operaciones de Validación OCS que guardan transiciones llaman al puerto `NotificacionDeTransiciones` en la misma transacción.
- Por cada transición nueva, el adaptador publica el evento `validacionocs.NotificacionesSolicitadas` hacia `notificaciones` (ADR-018).
- El evento lleva un mensaje por destino: el título, el detalle y el enlace a la pantalla, armados con la configuración del proceso.

**Quién recibe** (§2, §4):
- **Validación OCS decide el destino** con `ProcesoAprobacion.destinoDe(transicion)`: el rol de la etapa de llegada.
  - En el envío de un parcial, el de la siguiente etapa activa.
  - En una devolución, los coordinadores de las carreras devueltas.
- **Notificaciones**, en el proceso `identidad`:
  - resuelve el destino a usuarios con un caso de uso nuevo de Roles, `ConsultarDestinatarios`;
  - deja solo los activos (`ListarUsuarios`) y quita al autor;
  - guarda una fila por destinatario, una sola vez por evento (`evento_recibido` y una clave única).

**La bandeja** (§5):
- Tres operaciones en `contratos/notificaciones/openapi.yaml`: la bandeja, el número sin leer y marcar como leída.
- Solo piden sesión; cada usuario ve solo lo suyo.

**La SPA** (§6): la campana en la barra superior consulta el número cada 30 s y al volver el foco.

**El script** (§8): `probar_documento_firmable.sh` revisa las bandejas después de cada transición y prueba la caída de `identidad` y una entrega repetida.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash

**Primary Dependencies**: las que ya están, sin dependencias nuevas. Notificaciones suma:
- los módulos internos `:servicios:roles` y `:servicios:usuarios`, por sus puertos de entrada;
- el plugin `mdd.contrato`.

**Storage**: PostgreSQL, Notificaciones V2: `evento_recibido` y `notificacion`, con su disparador y `REVOKE`.

**Testing**:
- **Dominio de Validación OCS:** `destinoDe` para cada tipo de transición y con una configuración no estándar; el armado de cada mensaje.
- **Dominio de Notificaciones:**
  - `SolicitudDeNotificaciones`: los largos, el enlace y los secretos;
  - `Notificacion.marcarLeida`: es idempotente.
- **Aplicación (Mockito):**
  - Validación OCS: cada una de las 8 operaciones notifica con sus transiciones nuevas, y un rechazo no notifica;
  - `RecibirNotificacionesServicio`: los destinatarios, los activos, sin el autor, con un evento repetido y con un fallo de Roles;
  - la bandeja y la lectura.
- **Roles:** `ConsultarDestinatariosServicio` con el Responsable del periodo, los coordinadores por carrera y un rol cualquiera.
- **Integración con PostgreSQL:**
  - V2 con sus reglas: solo se cambia `leida_en`, una vez, y no se borra;
  - la unicidad por evento, destinatario y carrera;
  - dos entregas a la vez.
- **Contrato y permisos:**
  - el receptor: 204, el repetido, 400 y 403 con un token de usuario;
  - la bandeja: 200, el 404 de otro usuario y el 401 sin sesión;
  - `ErroresNotificacionesTest`.
- **Proceso `identidad`:** la recepción con Roles y Usuarios reales (`NotificacionesDesdeAprobacionIntegrationTest`).
- **SPA:**
  - Vitest de la campana: el número, la lista, abrir, vacía y el error;
  - Playwright con axe-core.
- **Punta a punta:** `probar_documento_firmable.sh`, pasos 16 a 18.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web:
- Validación OCS (proceso `distributivo`);
- Notificaciones y Roles (proceso `identidad`);
- la SPA y un script.

**Performance Goals**:
- SC-002: menos de 2 minutos después de la vuelta de `identidad`, con el reenvío cada 30 s.
- SC-004: menos de 1 minuto en la campana.
- Publicar suma una inserción a la transacción del usuario.

**Constraints**:
- **ADR-018 y ADR-024:** el mecanismo común, sin colas.
- **Principio V:** Notificaciones no lee tablas ajenas; usa los puertos de entrada de Roles y Usuarios, en su proceso.
- **Principio VII:** el contenido de una notificación no cambia; solo `leida_en`.
- **V-09:** sin correo.

**Scale/Scope**:
- Validación OCS: 1 puerto, 1 adaptador, 1 regla de dominio y 8 llamadas.
- Notificaciones: 2 tablas, 4 operaciones (1 interna y 3 de la bandeja) y 2 códigos.
- Roles: 1 caso de uso.
- SPA: 1 componente.
- El script: 3 pasos.
- La decisión D-108.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Reglas en el dominio: `destinoDe`, los mensajes, la validación de la solicitud y `marcarLeida`. Roles y Usuarios entran por puertos de salida; la base, por adaptadores. | ✅ |
| II. DDD | Aprobación decide a quién toca un hecho del flujo. Notificaciones es genérico (glosario): guarda y muestra mensajes. | ✅ |
| III. Lenguaje ubicuo | `Notificacion`, «bandeja», «notificación» (MD-09), y las etapas y roles del glosario. | ✅ |
| IV. API primero | `contratos/notificaciones/openapi.yaml` (nuevo) y el contenido del evento, antes del código. | ✅ |
| V. Límites | Entre procesos, el evento persistente. Dentro de `identidad`, los puertos de entrada de Roles y Usuarios. Cada transacción queda en su servicio. | ✅ |
| VI. Las reglas son datos | El rol y el nombre de cada etapa salen de la configuración del periodo. | ✅ |
| VII. Inmutabilidad | El contenido no cambia, y no se borra: lo impiden un disparador y el `REVOKE`. | ✅ |
| VIII. Pruebas | Dominio y aplicación, viéndolos fallar; PostgreSQL real; el proceso `identidad` completo; el script con la caída. | ✅ |
| IX. Seguridad | Cada usuario solo ve lo suyo, por el `sub` del token; la notificación ajena da 404. El receptor solo admite el rol `PROCESO`. No viajan secretos ni datos personales de docentes. | ✅ |
| X. Simplicidad | Sin permiso nuevo, sin *websockets* (consulta cada 30 s), y el mismo camino de eventos. | ✅ |
| XI. Diseño pragmático | Los pasos van en el script de la cadena completa, que ya recorre todas las transiciones. | ✅ |
| XII. JavaDoc | A quién toca cada transición, por qué se fijan al recibir y por qué no hay permiso. | ✅ |
| XIII. Errores | Dos códigos nuevos con sus 8 columnas. | ✅ |
| XIV. Validación | Los largos, el enlace y los secretos, en la spec, el contrato y el dominio. | ✅ |

**Post-diseño:** sin cambios. D-108 queda registrada antes de implementar, con research §1 a §8.

## Project Structure

### Documentation (this feature)

```text
specs/053-notificaciones-internas/
├── spec.md, plan.md, research.md, data-model.md, quickstart.md
├── contracts/ (notificaciones.md, eventos.md, spa.md, demostracion.md)
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/notificaciones/openapi.yaml (nuevo), contratos/comun/errores.md

backend/servicios/validacionocs/
├── dominio/            ProcesoAprobacion.destinoDe, DestinoDeNotificacion, MensajesDeTransicion
├── puertos/salida/     NotificacionDeTransiciones
├── aplicacion/         las 8 llamadas (FlujoDelDistributivo, RegistrarResolucion,
│                       RegistrarRechazoEnFirma, CompletarFirmas)
└── adaptadores/salida/ PublicadorDeNotificaciones

backend/servicios/roles/
├── puertos/entrada/    ConsultarDestinatarios, Destinatario
└── aplicacion/         ConsultarDestinatariosServicio

backend/servicios/notificaciones/
├── dominio/            Notificacion, SolicitudDeNotificaciones, MensajeSolicitado,
│                       NotificacionInvalida, NotificacionNoEncontrada
├── puertos/entrada/    RecibirNotificaciones, ConsultarBandeja, MarcarLeida
├── puertos/salida/     RepositorioNotificaciones, RepositorioEventosRecibidos, Destinatarios,
│                       Transaccion
├── aplicacion/         RecibirNotificacionesServicio, BandejaServicio
├── adaptadores/        EventosControlador, BandejaControlador, ErroresNotificaciones,
│                       *Jdbc, DestinatariosDesdeIdentidad (Roles + Usuarios)
└── resources/db/migration/notificaciones/V2__bandeja.sql

backend/apps/distributivo/src/main/resources/application.yaml   destino notificaciones

frontend/src/navegacion/Campana.tsx, notificaciones.ts, Marco.tsx; package.json (generar:api)
frontend/e2e/notificaciones.spec.ts
deploy/probar_documento_firmable.sh
docs/decisiones.md      D-108
```

**Structure Decision**:
- Las reglas de a quién toca cada transición son del flujo, y van en Validación OCS.
- Resolver usuarios toca designaciones y carreras, y va en Roles.
- Guardar y mostrar es de Notificaciones.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Olvidar notificar en una de las 8 operaciones, o en una futura. | Una prueba de aplicación por operación. Una prueba de arquitectura comprueba que cada clase que llama a `procesos.guardar(…, nuevas)` también llama a `NotificacionDeTransiciones`. |
| Roles o Usuarios fallan al recibir. | El receptor responde 5xx y el evento se reenvía cada 30 s; no se pierde. |
| Dos entregas del mismo evento a la vez. | `evento_recibido` con `ON CONFLICT DO NOTHING` y la clave única de `notificacion`. Prueba con dos hilos. |
| La consulta cada 30 s carga el proceso `identidad`, que ya usa el 93 % de su memoria (#224). | La consulta es solo un `count` indexado. Se mide la memoria en el equipo de demostración después del despliegue. |
| El script de la cadena se alarga, con unos 2 minutos de espera por el reenvío. | Una sola caída, en el registro de la resolución. |
| El enlace depende de las rutas de la SPA. | Las fija `contracts/eventos.md`, y Notificaciones valida su forma. Una prueba de la SPA comprueba que cada enlace es una ruta existente. |

## Complexity Tracking

Sin violaciones que justificar.
