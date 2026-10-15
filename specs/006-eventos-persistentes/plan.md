# Implementation Plan: Eventos persistentes entre procesos

**Branch**: `006-eventos-persistentes` | **Date**: 2026-10-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/006-eventos-persistentes/spec.md` (issue #22, B00-H05)

## Summary

Un módulo nuevo, `plataforma:eventos`, da a los procesos que publican el mecanismo del principio V:
- **publicación:** el servicio publica un `EventoEntreProcesos` (sobre con `idEvento`, `tipo`, `origen`, `momento` y `contenido`) en su transacción. El registro JDBC de Spring Modulith 2.1.1 lo guarda en el esquema `eventos_<proceso>`;
- **entrega:** después de confirmar la transacción, un oyente asíncrono lo envía por HTTP, con plazo de espera, al receptor del otro proceso. Con 2xx, la publicación queda `COMPLETED`; con cualquier fallo, `FAILED`;
- **reenvío:** una tarea programada reenvía las `FAILED` cada 30 s. El monitor de publicaciones atascadas de Spring Modulith cubre al emisor que se cae a mitad de una entrega;
- **autenticación:** el emisor se autentica con un token de proceso RS256 que emite Usuarios (D-49), con el rol `PROCESO`, que solo sirve en `/interno/**`.

El receptor de prueba (D-47) es `POST /interno/usuarios/eventos` en Usuarios. Es idempotente por `idEvento`, con `INSERT … ON CONFLICT DO NOTHING` sobre `usuarios.evento_recibido`.

La demostración (D-48) son dos piezas:
- `POST /api/generacion/eventos-prueba`, apagado por defecto, que inserta una fila y publica en una transacción;
- `deploy/probar_eventos.sh`, que recorre los tres escenarios deteniendo y volviendo a arrancar identidad.

Los detalles y las alternativas están en [research.md](research.md). La decisión que afecta a varias features es **ADR-018**, aprobada por el autor el 2026-10-14 (`docs/decisiones.md`).

## Technical Context

**Language/Version**: Java 25 · YAML (OpenAPI 3.0.3) · SQL (PostgreSQL 18) · Bash

**Primary Dependencies**:
- Spring Modulith 2.1.1: `spring-modulith-events-api`, `-events-jdbc` y `-events-jackson`, sin el *starter* (research §1);
- `RestClient` de Spring 7 sobre `java.net.http.HttpClient`;
- `@Scheduled` de Spring;
- Flyway (ya está);
- `spring-security-oauth2-jose` (ya está en Usuarios).

No se agregan dependencias fuera de los BOM.

**Storage**: PostgreSQL:
- esquemas nuevos `eventos_identidad`, `eventos_datos`, `eventos_distributivo` y `eventos_salida`, cada uno con el rol de su proceso como dueño;
- tabla `event_publication` en `eventos_distributivo`;
- tablas nuevas `usuarios.evento_recibido` y `generacion.evento_prueba`.

**Testing**:
- JUnit Jupiter 6 y AssertJ;
- el servidor HTTP del JDK como receptor simulado;
- `OutputCaptureExtension` para el registro;
- Testcontainers con PostgreSQL para el registro, el reenvío, la reversión y la idempotencia (también con entregas simultáneas);
- MockMvc con `ValidadorDeContrato` para los contratos;
- ArchUnit y Spring Modulith (`ModulosTest`);
- `deploy/probar_eventos.sh` en `perfil-consolidado`.

**Target Platform**: el perfil consolidado (`compose.yaml`) en local y en la CI; el VPS en B00-H08.

**Project Type**: monorepo web: multiproyecto Gradle en `backend/`, contratos en `contratos/` y despliegue en `deploy/`.

**Performance Goals**:
- entrega normal en menos de 10 s (SC-001);
- reenvío en menos de 2 ciclos de 30 s después de que vuelve el receptor (SC-003).

**Constraints**:
- sin colas ni broker (principio X);
- ningún servicio lee tablas de otro (principio V);
- sin contenido, claves ni tokens en el registro (FR-006);
- memoria por debajo de la alerta de 7 GiB de R-19 (SC-007).

**Scale/Scope**:
- 1 módulo de plataforma nuevo;
- 3 operaciones nuevas (2 internas en Usuarios y 1 de prueba en Generación);
- 1 esquema de contrato común;
- 1 código común;
- 3 migraciones;
- 1 script de punta a punta.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Cinco capas | Generación publica por su puerto `PublicadorDeEventos`; el adaptador usa `ApplicationEventPublisher`. El HTTP vive en `plataforma:eventos` (oyente y cliente) y en el controlador del receptor. El dominio no cambia. | ✅ |
| II. DDD según el subdominio | No hay modelo de negocio. El evento de prueba es técnico (D-47). | ✅ N/A |
| III. Lenguaje ubicuo | Los nombres siguen el principio V: publicación, oyente, tarea programada y receptor. No hay términos de negocio nuevos. | ✅ |
| IV. API primero | `comun/eventos.yaml`, las 2 operaciones internas de Usuarios y la de prueba de Generación se escriben antes del código. Los controladores implementan las interfaces generadas. | ✅ |
| V. Límites y fallos entre procesos | Es la historia que implementa la regla: evento en la transacción, registro persistente, oyente del mismo proceso con plazo, tarea programada de reenvío y receptor idempotente por `idEvento`. El registro vive en un esquema del proceso y cada receptor en el suyo. | ✅ |
| VI y VII | Sin reglas ni datos de negocio. | ✅ N/A |
| VIII. Pruebas | Las pruebas van primero, para cada escenario y caso límite (research §10), con datos ficticios. Sin Mockito fuera de la aplicación. 60 % por módulo. | ✅ |
| IX. Seguridad | Token de proceso con mínimo privilegio y vida corta, y separación de tokens en `/interno/**`. Las credenciales están en el entorno, con valores ficticios en `.env.ejemplo`. Sin secretos ni tokens en el registro. El endpoint de prueba está apagado por defecto. | ✅ |
| X. Simplicidad | Sin broker, sin servidor de autorización y sin *starter* de Spring Modulith. Un solo receptor genérico por servicio y un solo oyente común. | ✅ |
| XI. Diseño pragmático | `plataforma:eventos` existe porque varios procesos van a publicar (F16, F17). El SQL idempotente queda en cada receptor, porque va en la transacción de su servicio (research §5). | ✅ |
| XII. JavaDoc | Todo el código nuevo, en español. | ✅ |
| XIII. Errores | Un código común nuevo (`COMUN_EVENTO_NO_RECONOCIDO`) en `errores.md` y en `CodigosComunes`. Los fallos de entrega no llegan al usuario: dejan la publicación pendiente. | ✅ |
| §3 Restricciones técnicas | Es el «registro persistente de publicaciones de eventos de Spring Modulith» de la tabla del backend. | ✅ |
| §6.4 ADR | ADR-018, aprobado por el autor el 2026-10-14. | ✅ |

**Resultado del gate antes de la fase 0:** pasa. ADR-018 quedó aprobado el 2026-10-14.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado. El SC-006 de la especificación se ajustó a lo que ya hace el API Gateway (`denyAll` fuera de `/api/**`: 401 o 403, nunca llega al proceso).

## Project Structure

### Documentation (this feature)

```text
specs/006-eventos-persistentes/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1: sobre, publicación, evento recibido, evento de prueba y token de proceso
├── quickstart.md            # fase 1
├── contracts/
│   ├── entrega.md               # rutas internas, receptor, token de proceso y emisor
│   └── demostracion.md          # endpoint de prueba y deploy/probar_eventos.sh
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
contratos/
├── comun/
│   ├── eventos.yaml             # nuevo: esquema EventoEntreProcesos
│   └── errores.md               # cambia: + COMUN_EVENTO_NO_RECONOCIDO
├── usuarios/openapi.yaml        # cambia: + /interno/usuarios/token-proceso y /interno/usuarios/eventos
└── generacion/openapi.yaml      # cambia: + /api/generacion/eventos-prueba
deploy/
├── postgres/01-esquemas.sh      # cambia: + eventos_<proceso> para los 4 procesos
└── probar_eventos.sh            # nuevo
.env.ejemplo                     # cambia: + MDD_CLAVE_PROCESO_DISTRIBUTIVO y MDD_EVENTOS_PRUEBA_HABILITADO
.github/workflows/perfil-consolidado.yml   # cambia: + paso «Eventos de punta a punta»
backend/
├── settings.gradle                          # cambia: + plataforma:eventos
├── plataforma/
│   ├── eventos/             # nuevo: EventoEntreProcesos, OyenteEntreProcesos, ClienteDeEventos,
│   │                        #        ProveedorDeTokenDeProceso, ReenvioDePublicaciones, flywayEventos,
│   │                        #        EventosAutoConfiguracion y db/migration/eventos/V1__registro_de_publicaciones.sql
│   ├── seguridad/           # cambia: /interno/** solo con PROCESO; PROCESO no fuera de /interno/**
│   └── errores-nucleo/      # cambia: + COMUN_EVENTO_NO_RECONOCIDO en CodigosComunes
├── plantillas/README.md     # cambia: cómo publicar un evento y cómo escribir un receptor idempotente
├── apps/
│   ├── distributivo/        # cambia: + plataforma:eventos y su configuración (application.yaml)
│   └── identidad/           # cambia: ruta pública del token de proceso y clave del proceso
└── servicios/
    ├── usuarios/            # cambia: TokenProcesoControlador, EventosControlador, EmitirTokenDeProceso,
    │                        #         RegistrarEventoRecibido, RepositorioEventosRecibidosJdbc y V3__evento_recibido.sql
    └── generacion/          # cambia: EventosPruebaControlador (condicional), PublicarEventoDePrueba,
                             #         PublicadorDeEventos (puerto y adaptador), V2__evento_prueba.sql
```

**Structure Decision**: se mantienen las rutas de ADR-012 y ADR-015. El módulo nuevo va en `plataforma/`, como `seguridad` y `errores`. Las rutas entre procesos usan el prefijo `/interno/` (research §7).

## Fases de implementación (guía para /speckit-tasks)

1. **ADR-018:** aprobado el 2026-10-14 (`docs/decisiones.md`).
2. **Contratos primero:**
   - `comun/eventos.yaml`;
   - las operaciones de Usuarios y Generación;
   - `COMUN_EVENTO_NO_RECONOCIDO` en `errores.md`;
   - validación con el CLI de openapi-generator.
3. **Base de datos:** `01-esquemas.sh` con los 4 esquemas `eventos_<proceso>`, y las migraciones de Usuarios (V3) y Generación (V2).
4. **`plataforma:seguridad`**, con sus pruebas primero: separación de tokens y `/interno/**`.
5. **Usuarios**, con sus pruebas primero:
   - token de proceso (caso de uso, configuración y controlador);
   - receptor (caso de uso, repositorio JDBC idempotente y controlador);
   - pruebas de contrato e integración con entregas repetidas y simultáneas.
6. **`plataforma:eventos`**, con sus pruebas primero:
   - sobre, cliente, proveedor del token, oyente, reenvío, Flyway y autoconfiguración;
   - integración con PostgreSQL y un receptor simulado (normal, caído y su vuelta, reversión y atascadas).
7. **Generación y procesos:**
   - endpoint de prueba condicional y caso de uso transaccional;
   - `plataforma:eventos` en distributivo, la configuración de identidad y distributivo, y `.env.ejemplo`;
   - `ModulosTest` y la integración del proceso.
8. **De punta a punta:**
   - `deploy/probar_eventos.sh` (ShellCheck) y el paso en `perfil-consolidado`;
   - `probar_autenticacion.sh` y `probar_errores.sh` siguen pasando.
9. **Documentación:** `plantillas/README.md` (publicar y recibir eventos).
10. **Cierre:**
    - `evidencia.md` y el pull request con la CI en verde;
    - la fusión con la aprobación del autor;
    - el PR de evidencia con `perfil-consolidado` sobre `main` y la medición de R-19, porque la historia cambia el despliegue.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| `plataforma:eventos` (un módulo más) | El oyente, el cliente, el token y el reenvío son los mismos para cada proceso que publique (distributivo ahora; datos, salida e identidad con F16 y F17) | Ponerlo en Generación obligaría a copiarlo en cada servicio que publique. |
| 4 esquemas `eventos_<proceso>` cuando hoy solo publica distributivo | `01-esquemas.sh` solo corre al crear el volumen; crearlos de a uno obligaría a borrar el volumen en cada historia | Un solo esquema compartido rompería ADR-013 (un rol por proceso, dueño de sus esquemas). |
