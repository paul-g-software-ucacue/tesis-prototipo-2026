# Implementation Plan: Roles con permisos y varios roles por usuario

**Branch**: `011-roles-permisos` | **Date**: 2026-10-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/011-roles-permisos/spec.md` (issue #44, F05-H02)

## Summary

Sobre el inicio de sesión de F05-H01 y el esqueleto del servicio Roles:
- **Roles (servicio 2):** dueño del catálogo de funcionalidades, los roles, sus permisos (funcionalidad, tipo y alcance) y las asignaciones a usuarios. `V2__matriz_d16.sql` carga las 16 funcionalidades y los 10 roles de D-16, que son fijos (D-63). El administrador crea, edita y borra roles propios y asigna varios a un usuario.
- **Permisos en el token (ADR-021, propuesto):** Usuarios pide a Roles los roles y permisos efectivos al iniciar sesión y al renovar, y los pone en el claim `permisos`. Un cambio vale desde la próxima renovación (D-62). El alcance «solo lo propio» viaja en el permiso, pero todavía no filtra datos (D-64).
- **Validación en el servidor (RNF-SEG-03):**
  - cada operación protegida declara `x-mdd-permiso` en el contrato y `@PreAuthorize` en el controlador;
  - `VerificadorDePermisos` prueba, operación por operación, que sin el permiso se recibe 403.
- **403 común:** `SEGURIDAD_ACCESO_NO_AUTORIZADO` en los 4 procesos y en el API Gateway.
- **SPA:** la pantalla «Roles» (roles, matriz, asignaciones), «Tus funciones» en Inicio y «Acceso no autorizado» en las rutas sin permiso.
- **Usuarios:** deja de guardar roles (`usuario_rol` queda sin uso) y los usuarios de la demo reciben sus roles iniciales una sola vez.
- **Matriz D-16:** su interpretación en permisos es **D-65, propuesta**, pendiente de la aprobación del autor.

Los detalles y las alternativas están en [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · SQL (PostgreSQL 18, Flyway) · Bash

**Primary Dependencies**: sin dependencias nuevas.
- **Spring Security:** la *method security* (`@PreAuthorize`), ya en el classpath.
- **Roles:** `JdbcClient` (un subdominio genérico con un CRUD simple, principio II).
- **SPA:** openapi-typescript y openapi-fetch. Para la matriz, los componentes de MUI que ya usa la SPA.

**Storage**: esquema `roles`: `funcionalidad`, `funcionalidad_tipo`, `rol`, `rol_permiso`, `asignacion`, `inicializacion` y `registro_cambio` (V2 y V3). `usuarios.usuario_rol` queda sin uso.

**Testing**:
- **Dominio y aplicación:** JUnit Jupiter 6, AssertJ y Mockito, este último solo en la aplicación.
- **Adaptadores:** Testcontainers con PostgreSQL (repositorios de Roles y la matriz D-16).
- **Contrato:** swagger-request-validator y `VerificadorDePermisos` (nuevo en `plataforma:pruebas`).
- **SPA:** Vitest y Playwright.
- **Punta a punta:** `deploy/probar_autenticacion.sh`.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: servicios Roles y Usuarios (proceso `identidad`), plataforma de seguridad, API Gateway, contratos, SPA y despliegue.

**Performance Goals**:
- el inicio de sesión y la renovación suman una consulta a Roles dentro del proceso: menos de 50 ms;
- el token crece unos 500 bytes en el peor caso (Administrador).

**Constraints**:
- el permiso se valida en el servidor; la SPA solo decide qué mostrar;
- el 403 no dice qué permiso faltó;
- un servicio no lee las tablas de otro (principio V);
- el contrato antes que el código;
- los cambios en Usuarios no pueden impedir volver a la versión anterior en el equipo (`usuario_rol` no se borra);
- 60 % de líneas por módulo.

**Scale/Scope**:
- 16 funcionalidades y 10 roles predefinidos;
- 9 operaciones de Roles y 1 de Usuarios;
- 5 códigos de error;
- 1 pantalla nueva con 3 vistas en la SPA;
- 3 pasos nuevos en `probar_autenticacion.sh`.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Roles con sus cinco capas. Las reglas (permisos válidos, unión, predefinido y sin administrador) en el dominio. La consulta a Roles desde Usuarios, por un puerto de salida. | ✅ |
| II. DDD según el subdominio | Roles es genérico: CRUD simple detrás de sus puertos, sin agregados elaborados. | ✅ |
| III. Lenguaje ubicuo | Rol, Permiso, Funcionalidad y Asignación de rol en español, con los nombres de D-16 y el glosario. | ✅ |
| IV. API primero | `contratos/roles/openapi.yaml` nuevo, `x-mdd-permiso` en el contrato, y los cambios en Usuarios y en lo común antes del código. | ✅ |
| V. Límites | Usuarios llama a Roles solo por `ec.mdd.roles.puertos.entrada` (`ModulosTest`), en una sola dirección. Las asignaciones pasan a Roles; nadie lee las tablas de otro. | ✅ |
| VI y VII | Sin reglas del distributivo ni versionado de normativa. | ✅ N/A |
| VIII. Pruebas | Pruebas primero; Mockito solo en la aplicación; Testcontainers en los adaptadores; datos ficticios; una prueba de autorización por operación. | ✅ |
| IX. Seguridad | Mínimo privilegio validado en el servidor (RNF-SEG-03); el 403 sin detalles; registro de cambios de roles. | ✅ |
| X. Simplicidad | Sin un servidor de autorización externo ni consultas por petición; los permisos en el token. | ✅ |
| XI y XII | JavaDoc en español en los puertos y el dominio de Roles. | ✅ |
| XIII. Errores | 5 códigos en el catálogo (1 común y 4 de Roles), con su prueba (ADR-016). | ✅ |
| §6.4 Decisiones | D-62, D-63 y D-64 aceptadas. **D-65 y ADR-021 propuestas**, pendientes de aprobación antes de implementar. | ⚠️ pendiente |

**Resultado del gate antes de la fase 0:** pasa, con D-65 y ADR-021 pendientes de aprobación.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/011-roles-permisos/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1: tablas, funcionalidades, matriz D-16, token y sesión
├── quickstart.md            # fase 1
├── contracts/
│   └── roles.md             # operaciones de Roles, lista de usuarios, 403 común y errores
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
contratos/
├── roles/openapi.yaml                       # nuevo
├── usuarios/openapi.yaml                    # cambia: listarUsuarios, 403 común
└── comun/seguridad.yaml, errores.md         # cambian: SesionActual.permisos, AccesoNoAutorizado, 5 códigos
backend/plataforma/
├── errores-nucleo/…/CodigosComunes.java     # SEGURIDAD_ACCESO_NO_AUTORIZADO
├── seguridad/…                              # RespuestaAccesoNoAutorizado, autoridades PERMISO_, @EnableMethodSecurity
└── pruebas/…/VerificadorDePermisos.java     # nuevo
backend/servicios/roles/                     # build.gradle con mdd.contrato
├── src/main/java/ec/mdd/roles/
│   ├── dominio/                             # Funcionalidad, TipoDePermiso, Alcance, Permiso, Rol, PermisosEfectivos, reglas
│   ├── puertos/entrada/                     # GestionarRoles, AsignarRoles, ConsultarAutorizacion, AsignarRolesIniciales
│   ├── puertos/salida/                      # RepositorioRoles, RepositorioAsignaciones, RegistroDeCambios
│   ├── aplicacion/                          # servicios de los casos de uso y sus excepciones
│   ├── adaptadores/entrada/                 # RolesControlador, AutorizacionControlador, ErroresRoles
│   ├── adaptadores/salida/                  # repositorios JDBC
│   └── infraestructura/                     # ConfiguracionRoles
├── src/main/resources/db/migration/roles/V2__matriz_d16.sql, V3__asignaciones.sql
└── src/test, src/integrationTest
backend/servicios/usuarios/                  # Usuario sin roles; ProveedorDeAutorizacion; listarUsuarios; carga de la demo con roles iniciales
backend/gateway/                             # 403 común
frontend/src/
├── sesion/permisos.ts, funcionalidades.ts, RutaConPermiso.tsx
├── pantallas/roles/                         # Roles, RolFormulario, AsignacionesDeUsuarios
└── pantallas/Inicio.tsx                     # «Tus funciones»
deploy/probar_autenticacion.sh               # pasos de roles, solo lectura y varios roles
```

**Structure Decision**: la lógica de roles vive en el servicio Roles, que deja de ser un esqueleto. Usuarios solo consulta y pide los roles iniciales. La plataforma da el 403 y la verificación por contrato, para que las historias siguientes protejan sus operaciones igual.

## Fases de implementación (guía para /speckit-tasks)

1. **D-65 y ADR-021:** aprobación del autor.
2. **Contrato:** `roles/openapi.yaml`, los cambios en Usuarios y en lo común, y la generación en el backend y en la SPA.
3. **Plataforma:** el código común, `RespuestaAccesoNoAutorizado`, las autoridades `PERMISO_`, la *method security*, el 403 del API Gateway y `VerificadorDePermisos`.
4. **Dominio de Roles,** con sus pruebas primero.
5. **Persistencia de Roles:** V2 con la matriz D-16, V3 con las asignaciones, los repositorios JDBC y la prueba de la matriz contra `data-model.md`.
6. **Aplicación y API de Roles:** gestión, asignación, autorización, roles iniciales, registro de cambios, el controlador y `ErroresRoles`.
7. **Usuarios:** `ProveedorDeAutorizacion` en el inicio de sesión y la renovación, el claim `permisos`, `listarUsuarios`, la carga de la demo y de `autor.prueba` con roles iniciales, y el retiro de `RolMdd` y del mapeo de `usuario_rol`.
8. **SPA:** permisos, «Tus funciones», la pantalla Roles y `RutaConPermiso`.
9. **Punta a punta:** `probar_autenticacion.sh` y la comprobación en el perfil.
10. **Cierre:** evidencia, PR, fusión con la aprobación del autor, despliegue automático en el PC, comprobación con `admin.demo` y el PR de evidencia con `Closes #44`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| `x-mdd-permiso` y `VerificadorDePermisos` | RNF-SEG-03 pide pruebas de autorización por endpoint, y el permiso tiene que estar en el contrato, que es la fuente de verdad | Reglas por ruta sin prueba: el contrato y el código se separan sin que nadie lo note. |
| Tabla `inicializacion` | Las asignaciones del administrador tienen que sobrevivir a los reinicios (FR-009), incluso si le quita todos los roles a un usuario de la demo | Asignar si el usuario no tiene roles: volvería a dar los roles iniciales a quien el administrador dejó sin ninguno. |
