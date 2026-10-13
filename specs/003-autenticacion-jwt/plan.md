# Implementation Plan: Autenticación JWT de punta a punta

**Branch**: `003-autenticacion-jwt` | **Date**: 2026-10-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/003-autenticacion-jwt/spec.md` (issue #20, B00-H03)

## Summary

Usuarios emite, en un inicio de sesión mínimo para un usuario de prueba ficticio (D-37), un JWT RS256 con el usuario y sus roles. Publica su llave pública como JWKS. El API Gateway y cada uno de los 4 procesos validan el token por su cuenta y responden 401 con el modelo de error del principio XIII, siempre con el mismo cuerpo y sin decir qué falló (D-38). Un servicio de cada proceso ofrece «sesión actual» (D-39) para probar de punta a punta.

La historia trae también:
- los primeros contratos OpenAPI en `contratos/`, con la generación de interfaces y las pruebas de contrato (principio IV);
- el módulo `plataforma/seguridad`, que comparten los 4 procesos;
- la segunda medición de R-19, después de iniciar sesión (D-34, D-36).

Los detalles y sus alternativas están en [research.md](research.md). Las decisiones que afectan a varias features están en ADR-014 y ADR-015, aprobados por el autor el 2026-10-13.

## Technical Context

**Language/Version**: Java 25 · YAML (OpenAPI 3.0.3, Compose, Actions) · Bash (pruebas de punta a punta y medición) · SQL (migración de Usuarios)

**Primary Dependencies**:
- **Del BOM de Spring Boot 4.1.1:**
  - Spring Security 7.1.1, con `spring-boot-starter-security-oauth2-resource-server` en los procesos y en el API Gateway (versión reactiva);
  - `spring-security-oauth2-jose` (Nimbus) para firmar en Usuarios;
  - `spring-boot-starter-data-jpa` (Hibernate 7.4.5), solo en Usuarios.
- **Fuera del BOM:**
  - BouncyCastle `bcprov-jdk18on` 1.86, que pide `Argon2PasswordEncoder`;
  - plugin `org.openapi.generator` 7.25.0;
  - `com.atlassian.oai:openapi-request-validator-core` 3.0.0, solo en las pruebas.
- Spring Cloud Gateway 5.0.3, ya presente.

**Storage**: PostgreSQL 18.6. Esquema `usuarios`, con las tablas `usuario` y `usuario_rol` (migración V2). Ningún otro esquema cambia.

**Testing**:
- JUnit Jupiter 6, AssertJ, Mockito (solo en la aplicación de Usuarios) y Testcontainers;
- `spring-boot-starter-security-oauth2-resource-server-test` y `WebTestClient` en el API Gateway;
- pruebas de contrato con openapi-request-validator;
- `deploy/probar_autenticacion.sh` de punta a punta.

**Target Platform**: el perfil consolidado de B00-H02 (Docker Compose), en un equipo de 4 núcleos y 8 GiB (D-36). En local, Docker Engine 29.

**Project Type**: monorepo web: multiproyecto Gradle en `backend/`, contratos en `contratos/` y despliegue en `compose.yaml` y `deploy/`.

**Performance Goals**: validar un token no hace llamadas de red, porque el JWKS queda en caché. Los 6 contenedores siguen sanos en 180 s o menos (SC-006).

**Constraints**:
- Sin la llave privada, el token ni la contraseña en el repositorio, las respuestas ni los registros (FR-002, FR-009).
- Las causas de un 401 son indistinguibles (SC-002).
- El consumo queda por debajo de 8 GiB después de iniciar sesión, con alerta en 7 GiB.

**Scale/Scope**:
- 1 servicio con dominio (Usuarios), 4 endpoints de «sesión actual», 1 inicio de sesión y 1 JWKS;
- 4 contratos de servicio y 3 archivos comunes;
- 2 módulos de plataforma (uno solo para pruebas), 1 plugin de convención y 2 scripts.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Cinco capas | Usuarios: `Usuario` en el dominio (Java puro), `IniciarSesion` en los puertos de entrada, el servicio en la aplicación, repositorio, verificador y emisor como puertos de salida, adaptadores JPA, Argon2 y JWT, y llaves y configuración en la infraestructura. «Sesión actual» es un controlador en los adaptadores de entrada. `ArquitecturaTest` corre en cada servicio. | ✅ |
| II. DDD según el subdominio | Identidad y acceso es un subdominio genérico: un `Usuario` simple, sin agregados complejos. | ✅ |
| III. Lenguaje ubicuo | Usa «usuario», «rol» y «sesión», ya del glosario. Los roles de prueba son los de F05-H02. | ✅ |
| IV. API primero | Los contratos OpenAPI 3.0.3 se escriben y revisan antes del código, en `contratos/`. Las interfaces se generan con openapi-generator y hay pruebas de contrato (ADR-015). | ✅ |
| V. Límites de servicio | Los roles viven en el esquema de Usuarios y nadie más los lee. Cada servicio genera sus propios modelos del contrato, sin compartir clases. Los procesos piden el JWKS por HTTP según el contrato de Usuarios. | ✅ |
| VI y VII | Esta historia no tiene reglas ni datos de negocio. | ✅ N/A |
| VIII. Pruebas | Pruebas unitarias del dominio y de la aplicación (con Mockito solo en los puertos), de los adaptadores con PostgreSQL real, de contrato, de seguridad con llaves generadas en la prueba, y de punta a punta. Cobertura del 60 % por módulo. | ✅ |
| IX. Seguridad | RS256, Argon2id, llave privada fuera del repositorio, el usuario de prueba solo donde se habilita, mínimo privilegio en las rutas públicas y Gitleaks en verde. | ✅ |
| X. Simplicidad | Sin renovación, revocación ni servidor de autorización. Ni roles dinámicos ni matriz de permisos. JPA solo en Usuarios. | ✅ |
| XI. Diseño pragmático | `plataforma/seguridad` existe porque 4 procesos usan la misma configuración desde el primer día (research §6). El adaptador del validador de contratos reemplaza un módulo incompatible con Spring 7. | ✅ |
| XII. JavaDoc | Puertos, dominio, configuración de seguridad y llaves, en español. | ✅ |
| XIII. Errores | 401 con `titulo`, `descripcion` y `codigo`; catálogo inicial en `contratos/comun/errores.md` (D-38); sin trazas, tokens ni causas en las respuestas. | ✅ |
| §3 Restricciones técnicas | Spring Security con JWT RS256 y Argon2id, Spring Data JPA en los adaptadores, openapi-generator 7.16 o posterior con `useSpringBoot4` y `useJackson3`, Spring Cloud Gateway 2025.1.3 y memoria medida (R-19). | ✅ |
| §6.4 ADR | ADR-014 (autenticación) y ADR-015 (contratos), aprobados por el autor el 2026-10-13. | ✅ |

**Resultado del gate antes de la fase 0:** pasa. ADR-014 y ADR-015 quedaron aprobados el 2026-10-13.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado: una entidad, 6 endpoints, un módulo de plataforma y un plugin de convención.

## Project Structure

### Documentation (this feature)

```text
specs/003-autenticacion-jwt/
├── spec.md
├── plan.md                    # este archivo
├── research.md                # fase 0
├── data-model.md              # fase 1: usuario, token, llaves, sesión actual, 401 y medición
├── quickstart.md              # fase 1
├── contracts/
│   ├── autenticacion.md           # token, llaves, validación, rutas públicas y 401
│   ├── api.md                     # los 6 endpoints y los archivos de contratos/
│   └── medicion-r19-sesion.md     # medición después de iniciar sesión y pruebas de punta a punta
├── checklists/
│   └── requirements.md
├── evidencia.md               # se crea al implementar
└── tasks.md                   # /speckit-tasks
```

### Source Code (repository root)

```text
contratos/                                    # nuevo (ADR-015)
├── comun/errores.yaml  comun/errores.md  comun/seguridad.yaml
├── usuarios/openapi.yaml  cargas/openapi.yaml  generacion/openapi.yaml  dashboard/openapi.yaml
.dockerignore                                 # nuevo: el contexto de las imágenes pasa a la raíz
.env.ejemplo                                  # cambia: + usuario de prueba y URL del JWKS
compose.yaml                                  # cambia: contexto de construcción y variables de identidad
deploy/
├── docker/java.Dockerfile                    # cambia: copia backend/ y contratos/
├── probar_autenticacion.sh                   # nuevo
└── medir_memoria.sh                          # cambia: opción --con-sesion
.github/workflows/perfil-consolidado.yml      # cambia: pruebas de punta a punta y medición con sesión
backend/
├── gradle.properties                         # cambia: versiones de BouncyCastle, openapi-generator y del validador
├── settings.gradle                           # cambia: + plataforma:seguridad
├── buildSrc/
│   ├── build.gradle                          # cambia: + plugin org.openapi.generator
│   └── src/main/groovy/mdd.contrato.gradle   # nuevo
├── plataforma/seguridad/                     # nuevo: servidor de recursos servlet y 401 de los procesos
├── plataforma/pruebas/                       # nuevo: solo para pruebas: llaves y tokens de prueba, JWKS y validador de contratos
├── plantillas/aplicacion/                    # cambia: + plataforma:seguridad y propiedades del JWKS
├── apps/{identidad,datos,distributivo,salida}/  # cambia: + plataforma:seguridad y propiedades
├── gateway/                                  # cambia: rutas, seguridad reactiva y 401
└── servicios/
    ├── usuarios/                             # cambia: dominio, aplicación, adaptadores, V2 e infraestructura de llaves
    └── cargas/  generacion/  dashboard/      # cambia: contrato y controlador de «sesión actual»
```

**Structure Decision**: se mantienen las rutas de ADR-012. Los contratos van en `contratos/` en la raíz, como pide el principio IV, porque también los leerá la SPA (B00-H06).

## Fases de implementación (guía para /speckit-tasks)

1. **ADR-014 y ADR-015:** aprobados el 2026-10-13 (`docs/decisiones.md`).
2. **Contratos primero:** los archivos de `contratos/`, con `comun/` y los 4 de servicio, más el catálogo inicial. Se revisan antes de escribir código (principio IV).
3. **Construcción:** `mdd.contrato`, versiones nuevas, `plataforma/seguridad` (con sus pruebas primero) y el adaptador del validador de contratos.
4. **Usuarios:** pruebas primero (dominio, aplicación, adaptadores y contrato); dominio, caso de uso, V2, JPA, Argon2, llaves, emisor, JWKS, inicio de sesión, «sesión actual» y usuario de prueba.
5. **«Sesión actual»** en `cargas`, `generacion` y `dashboard`, con su prueba de contrato.
6. **API Gateway:** rutas, seguridad reactiva y 401, con sus pruebas.
7. **Procesos y despliegue:** `plataforma:seguridad` en los 4 procesos y la plantilla; contexto de construcción en la raíz; `.env.ejemplo` y `compose.yaml`. `docker compose up -d --wait` en local.
8. **De punta a punta y R-19:** `probar_autenticacion.sh`, `medir_memoria.sh --con-sesion` y `perfil-consolidado.yml`; medición local y de referencia.
9. **Cierre:** `evidencia.md`, pull request con la CI en verde y fusión con la aprobación del autor.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| `plataforma/seguridad` (módulo compartido) | 4 procesos con la misma configuración de seguridad y el mismo 401 desde esta historia | Copiarla en la plantilla haría 4 copias que se desalinean; la configuración reactiva del API Gateway no se puede compartir igual. |
| Adaptador propio para el validador de contratos | El módulo MockMvc de openapi-request-validator usa `javax.servlet` y no funciona con Spring 7 | Sin pruebas de contrato se viola el principio IV. |
