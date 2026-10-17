# Implementation Plan: Iniciar sesión

**Branch**: `010-iniciar-sesion` | **Date**: 2026-10-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/010-iniciar-sesion/spec.md` (issue #43, F05-H01)

## Summary

Sobre el inicio de sesión de B00-H03 y la base de la SPA de B00-H06:
- **Usuarios de la demo (D-59):** `usuarios-demo.yaml` con 11 usuarios ficticios (uno por cada rol de D-16 y `docente.coordinador.demo` con dos), sin contraseñas. `CargaUsuariosDemo` los crea o actualiza al arrancar, con la contraseña del entorno: un archivo `usuario=clave` que genera `preparar.sh` en el equipo, o la clave común ficticia de `.env.ejemplo` en local y en la CI. El nombre de usuario pasa a minúsculas y se agrega el nombre para mostrar (migración V4).
- **`autor.prueba` (D-57):** se desactiva en el equipo de demostración; `probar_humo.sh` pasa a `docente.demo`.
- **Bloqueo (D-60):** tabla `intento_fallido`, contada por nombre intentado, exista o no; con 5 fallos seguidos, 429 `USUARIOS_INICIO_BLOQUEADO` por 15 minutos.
- **Inactividad (D-60, ADR-020 propuesto):** el token sigue en 30 minutos y gana los claims `nombre` e `inicio`; `POST /api/usuarios/sesion/renovacion` lo renueva hasta 8 horas desde `inicio`; la SPA renueva solo si hubo actividad.
- **SPA:** «Hola, <nombre>» con los roles en español; el 429 se muestra con el modelo de error común.
- **RNF-SEG-02 (D-61):** se demuestra con evidencia, sin código nuevo.

Los detalles y las alternativas están en [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript 5.9 (React 19) · SQL (PostgreSQL 18, Flyway) · Bash

**Primary Dependencies**: las del proyecto, sin dependencias nuevas: Spring Security con el emisor JWT de Usuarios (Nimbus), `Argon2PasswordEncoder` (Bouncy Castle), Jackson YAML para leer `usuarios-demo.yaml` (ya en el BOM de Spring Boot), openapi-typescript y openapi-fetch.

**Storage**: esquema `usuarios`: `usuario` (con `nombre_para_mostrar` nuevo) y `intento_fallido` (nueva), por la migración `V4__usuarios_demo.sql`.

**Testing**:
- JUnit Jupiter 6, AssertJ y Mockito (dominio y aplicación);
- Testcontainers con PostgreSQL (adaptadores y carga de la demo);
- swagger-request-validator (contrato, con la respuesta 429 y la renovación);
- Vitest con temporizadores falsos (renovación) y Playwright (inicio de sesión de un usuario de la demo);
- `deploy/probar_autenticacion.sh` y `deploy/equipo/probar_equipo.sh`.

**Target Platform**: el perfil consolidado y el equipo de demostración (B00-H08).

**Project Type**: monorepo web: servicio Usuarios del proceso `identidad`, contratos, SPA y despliegue.

**Performance Goals**: el inicio de sesión de un usuario de la demo en menos de 1 minuto de punta a punta (SC-002). La carga de la demo verifica 11 hashes Argon2id al arrancar: menos de 2 segundos.

**Constraints**:
- el mismo mensaje y tiempo para usuario inexistente, inactivo o contraseña incorrecta (FR-006);
- el bloqueo no revela si el usuario existe;
- el token solo en memoria (D-51);
- ninguna contraseña en el repositorio, en una imagen ni en un registro;
- el contrato antes que el código (principio IV);
- 60 % de líneas por módulo.

**Scale/Scope**:
- 1 migración, 1 tabla nueva y 1 columna;
- 2 operaciones del contrato que cambian o se agregan, y 1 código de error;
- 11 usuarios de la demo;
- 3 pantallas o módulos de la SPA que cambian;
- 3 scripts de despliegue que cambian (`preparar.sh`, `probar_humo.sh` y `probar_equipo.sh`) y `compose.demo.yml`.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Dominio central | El bloqueo (`PoliticaDeIntentos`) y la normalización del nombre son reglas de dominio de Usuarios, sin infraestructura. | ✅ |
| II. Capas | Puertos nuevos: `RepositorioIntentos` y `RenovarSesion`; la carga de la demo vive en infraestructura, como la de `autor.prueba`. | ✅ |
| III. Lenguaje ubicuo | Usuario, Rol, Responsable de consolidación, Coordinador de carrera, `GESTOR_UNIDAD` y `SECRETARIA_GENERAL`, tal como en el glosario. | ✅ |
| IV. API primero | `openapi.yaml` de Usuarios y `seguridad.yaml` cambian antes del código; la SPA usa los tipos generados. | ✅ |
| V. Límites | Solo Usuarios (proceso `identidad`) y la SPA; los demás procesos no cambian. | ✅ |
| VI y VII | Sin reglas de negocio del distributivo ni versiones. | ✅ N/A |
| VIII. Pruebas | Pruebas primero en cada grupo; Mockito solo en la aplicación; adaptadores con Testcontainers; datos ficticios; 60 % de líneas. | ✅ |
| IX. Seguridad | Argon2id; contraseñas solo por entorno, generadas en el equipo; bloqueo sin enumeración; token en memoria; RNF-SEG-02 por diseño (D-61). | ✅ |
| X. Simplicidad | Sin tabla de sesiones ni *refresh token*; el archivo de la demo en lugar de una pantalla de administración. | ✅ |
| XI y XII | Puertos y dominio con JavaDoc en español. | ✅ |
| XIII. Errores | `USUARIOS_INICIO_BLOQUEADO` en el catálogo y en `ErroresUsuarios`, con la prueba que los compara (ADR-016). | ✅ |
| §6.4 Decisiones | D-59, D-60 y D-61 aceptadas; **ADR-020 propuesto**, pendiente de aprobación antes de implementar la renovación. | ⚠️ pendiente |

**Resultado del gate antes de la fase 0:** pasa, con ADR-020 pendiente de aprobación.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/010-iniciar-sesion/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1: usuario, intento fallido, usuarios de la demo, token y sesión
├── quickstart.md            # fase 1
├── contracts/
│   └── usuarios.md              # 429, renovación, SesionActual.nombre, catálogo y configuración
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
contratos/
├── usuarios/openapi.yaml               # cambia: 429, renovarSesion
└── comun/seguridad.yaml, errores.md    # cambian: SesionActual.nombre, USUARIOS_INICIO_BLOQUEADO
backend/servicios/usuarios/src/
├── main/java/ec/mdd/usuarios/
│   ├── dominio/                        # Usuario (nombre para mostrar, nombre normalizado), RolMdd, PoliticaDeIntentos
│   ├── puertos/entrada/                # RenovarSesion
│   ├── puertos/salida/                 # RepositorioIntentos; EmisorDeTokens (nombre, inicio, renovar)
│   ├── aplicacion/                     # IniciarSesionServicio (bloqueo), RenovarSesionServicio, InicioBloqueado
│   ├── adaptadores/entrada/            # SesionControlador (renovación, nombre), ErroresUsuarios (+código)
│   ├── adaptadores/salida/             # RepositorioIntentosJdbc, EmisorJwt (claims), UsuarioJpa (nombre)
│   └── infraestructura/                # CargaUsuariosDemo, CargaUsuarioDePrueba (desactiva), propiedades
├── main/resources/
│   ├── usuarios-demo.yaml              # nuevo (D-59)
│   └── db/migration/usuarios/V4__usuarios_demo.sql
└── test/…                              # dominio, aplicación, adaptadores (Testcontainers) y contrato
backend/apps/identidad/src/main/resources/application.yaml   # cambia: propiedades nuevas
frontend/src/
├── sesion/                             # almacen.ts (actividad), renovacion.ts (nuevo), roles.ts (nuevo)
├── pantallas/Inicio.tsx                # nombre y roles en español
└── …/*.test.tsx, e2e/sesion.spec.ts    # pruebas
.env.ejemplo                            # cambia: MDD_USUARIOS_DEMO_CLAVE
deploy/
├── probar_autenticacion.sh             # cambia: usuario de la demo, bloqueo y renovación
└── equipo/
    ├── compose.demo.yml                # cambia: autor.prueba apagado, claves-demo montado
    ├── preparar.sh                     # cambia: genera secretos/claves-demo
    ├── probar_humo.sh                  # cambia: docente.demo
    └── probar_equipo.sh                # cambia: autor.prueba rechazado, docente.demo aceptado
```

**Structure Decision**: todo el backend vive en el servicio Usuarios, con sus cinco capas (ADR-012); la SPA cambia solo en `sesion/` y en la pantalla de inicio.

## Fases de implementación (guía para /speckit-tasks)

1. **ADR-020:** aprobación del autor.
2. **Contrato:** `openapi.yaml`, `seguridad.yaml` y `errores.md`; generación en el backend y en la SPA.
3. **Dominio,** con sus pruebas primero: `RolMdd`, `Usuario` con nombre para mostrar y normalización, y `PoliticaDeIntentos`.
4. **Persistencia:** V4, `RepositorioIntentosJdbc` y `UsuarioJpa`, con Testcontainers.
5. **Aplicación:** bloqueo en `IniciarSesionServicio`, `RenovarSesionServicio` y los claims nuevos en `EmisorJwt`.
6. **Usuarios de la demo:** `usuarios-demo.yaml`, `CargaUsuariosDemo` y `CargaUsuarioDePrueba` que desactiva.
7. **API:** controlador (renovación y `nombre`), `ErroresUsuarios` y las pruebas de contrato.
8. **SPA:** roles en español, «Hola, <nombre>», actividad y renovación, y el 429.
9. **Perfil y equipo:** `.env.ejemplo`, `probar_autenticacion.sh`, `compose.demo.yml`, `preparar.sh`, `probar_humo.sh` y `probar_equipo.sh`.
10. **Cierre:** evidencia (incluida la de RNF-SEG-02, D-61), pull request, fusión con la aprobación del autor, el despliegue automático en el PC, `preparar.sh` para las claves de la demo y el PR de evidencia con `Closes #43`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| Renovación del token (ADR-020) | RNF-SEG-07 pide vencer por inactividad, y hoy un usuario activo es expulsado a los 30 minutos | Un token largo no vence por inactividad; una tabla de sesiones agrega estado y una consulta por petición. |
| Tabla `intento_fallido` contada por nombre intentado | El bloqueo no debe revelar qué usuarios existen y debe sobrevivir a un reinicio | Contar en memoria se pierde al reiniciar; contar solo usuarios existentes permite enumerarlos. |
