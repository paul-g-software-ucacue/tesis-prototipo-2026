# Implementation Plan: Repositorio, plantilla de servicio en cinco capas y CI con Gradle

**Branch**: `001-repo-plantilla-ci` | **Date**: 2026-10-11 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-repo-plantilla-ci/spec.md` (issue #18, B00-H01)

## Summary

El multiproyecto de Gradle se completa para que pueda construirse: `settings.gradle` y Wrapper 9.8.1, con Java 25 y Spring Boot 4.1.1. La plantilla de servicio tiene tres partes:

1. un subproyecto `plataforma:arquitectura` con las reglas ArchUnit de las cinco capas, probadas con *fixtures*;
2. los plugins de convención `mdd.servicio`, `mdd.servicio-central` y `mdd.aplicacion`, que aplican sin configuración por servicio las reglas, la cobertura, Spotless y SpotBugs;
3. dos esqueletos para copiar, uno de servicio y otro de proceso. El de proceso trae `ApplicationModules.verify()` de Spring Modulith 2.1.1.

En el `ci.yml` que ya existe, los jobs de frontend pasan a depender de que exista `frontend/package.json`, el job `sca` deja de omitirse cuando no hay frontend y las acciones se fijan por SHA con pinact. Al final, el ruleset `main` exige los checks con la política estricta y la fusión *squash*. Las tres comprobaciones de la sección 8 de la constitución quedan registradas con evidencia: palantir y SpotBugs con Java 25, Modulith 2.1 con Boot 4.1, y pinact. Los detalles y su evidencia previa están en [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (toolchain de Gradle; Temurin en CI, OpenJDK 25.0.4.1 en local) · Groovy DSL para los scripts de Gradle · YAML para GitHub Actions

**Primary Dependencies**:
- Plataforma: Spring Boot 4.1.1 (BOM) y Spring Modulith 2.1.1 (BOM propio, `spring-modulith-api` y `spring-modulith-starter-test`).
- Pruebas: ArchUnit 1.4.2 (`archunit` y `archunit-junit5`).
- Plugins de Gradle: Gradle 9.8.1 con Wrapper, Spring Boot Gradle Plugin 4.1.1 (para `mdd.aplicacion`), Spotless 8.10.4 con palantir-java-format 2.102.0, SpotBugs Gradle Plugin 6.5.12 con SpotBugs 4.10.4 y FindSecBugs 1.14.0, JaCoCo 0.8.15 y CycloneDX 3.5.1 (ya existe).

**Storage**: N/A. Esta historia no tiene persistencia.

**Testing**:
- JUnit Jupiter 6.0.3, AssertJ y Mockito 5.23.0, con las versiones del BOM de Boot 4.1.1 (ver research §6).
- ArchUnit y Spring Modulith para las pruebas de arquitectura.
- JaCoCo para la cobertura.
- La suite `integrationTest`, con Testcontainers 2.0.5, ya está en la convención; en esta historia queda en NO-SOURCE.

**Target Platform**: GitHub Actions (`ubuntu-latest`) para la CI · estaciones Linux para el desarrollo local

**Project Type**: monorepo web: multiproyecto Gradle en `backend/` y la SPA futura en `frontend/` (B00-H06). En esta historia solo cambian la base de construcción, la CI y la regla de la rama.

**Performance Goals**: un pull request listo para revisión termina las etapas 1 y 2 dentro de los `timeout-minutes` del `ci.yml`: lint 15 min, pruebas 20 min e integración 25 min.

**Constraints**:
- Repositorio privado (D-24).
- Acciones fijadas por SHA y permisos `contents: read` (principio IX).
- 3 000 minutos de Actions al mes con GitHub Pro, frente a los 975 a 1 450 estimados.
- Ninguna dependencia ni abstracción sin un uso concreto (principio XI).
- No se crean servicios ni procesos reales; llegan con sus historias.

**Scale/Scope**:
- 1 subproyecto nuevo, 2 plugins de convención nuevos y 1 que cambia, y 2 esqueletos.
- 1 workflow que cambia, 1 ruleset que se actualiza y 10 checks obligatorios.
- La plantilla sirve para los 12 servicios y los 4 procesos de ADR-006.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Cinco capas, puertos y adaptadores | Las reglas de `plataforma:arquitectura` imponen el sentido de las cinco capas y prohíben al dominio Spring, JPA, HTTP y PDF. El esqueleto trae las cinco capas con su `package-info.java`. | ✅ |
| II. DDD según el subdominio | `mdd.servicio-central` sigue exigiendo el 80 % solo en 5, 6, 10 y 12. La plantilla no impone agregados. | ✅ |
| III. Lenguaje ubicuo | No se agregan términos de dominio. Los paquetes de capa usan los nombres de ADR-009 en español sin tildes. Las clases técnicas siguen ADR-008. | ✅ |
| IV. API primero | No hay API en esta historia y no cambia `contratos/`. `puertos.entrada` es la API publicada dentro del proceso, como lo pide el principio. | ✅ N/A |
| V. Límites de servicio | `ApplicationModules.verify()` por proceso, ArchUnit por servicio, un subproyecto Gradle por servicio y una aplicación por proceso. | ✅ |
| VI. Reglas como datos | Esta historia no tiene reglas de negocio. | ✅ N/A |
| VII. Inmutabilidad y trazabilidad | Esta historia no tiene datos. La evidencia de las comprobaciones queda en `evidencia.md`. | ✅ N/A |
| VIII. Pruebas primero | Las reglas de arquitectura se escriben primero como pruebas con *fixtures* que deben fallar. JaCoCo aplica el 60 % y el 80 %. Los escenarios 3 a 5 se demuestran con pull requests. | ✅ |
| IX. Seguridad y SSDLC | SpotBugs con FindSecBugs en confianza alta. Acciones por SHA, `contents: read` y ningún secreto nuevo. Gitleaks sigue en pre-commit y en CI. | ✅ |
| X. Simplicidad y alcance | Solo lo que pide la historia. Ni servicios de ejemplo en producción, ni generadores, ni pasos de CI extra. | ✅ |
| XI. Diseño pragmático | Una interfaz o abstracción solo donde marca un límite (los plugins evitan copiar reglas, FR-002). Las alternativas descartadas están en research §9, §10 y §17. | ✅ |
| XII. JavaDoc | Las reglas de `plataforma:arquitectura` y los `package-info.java` del esqueleto llevan JavaDoc en español sobre el contrato de cada capa. | ✅ |
| XIII. Errores | Esta historia no tiene errores HTTP; el modelo es de B00-H04. | ✅ N/A |
| §3 Restricciones técnicas | Java 25, Boot 4.1.1, Gradle Groovy DSL, Modulith 2.1, Spotless, SpotBugs, FindSecBugs, JaCoCo 60/80 y JUnit Jupiter 6 (constitución 1.0.1, D-28). | ✅ |
| §8 Comprobaciones de la ola 0 | palantir y SpotBugs con Java 25, Modulith 2.1 con Boot 4.1 y pinact: tareas explícitas con evidencia en `evidencia.md`. | ✅ |

**Resultado del gate antes de la fase 0:** pasa. La única desviación (JUnit 5 frente a JUnit Jupiter 6) se resolvió con la enmienda 1.0.1 de la constitución (D-28).

**Revisión después de la fase 1:** pasa. El diseño de [data-model.md](data-model.md) y de [contracts/](contracts/) no agrega capas, servicios ni dependencias fuera de las listadas. Los contratos son de construcción y de CI, no de API, así que el principio IV sigue sin aplicar.

## Project Structure

### Documentation (this feature)

```text
specs/001-repo-plantilla-ci/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1: elementos de la plantilla, reglas y checks
├── quickstart.md            # fase 1: cómo validar los escenarios
├── contracts/
│   ├── plantilla-servicio.md    # estructura y reglas que garantiza un servicio creado desde la plantilla
│   ├── convenciones-gradle.md   # qué aplica cada plugin de convención
│   └── ci-y-regla-main.md       # jobs, checks obligatorios y ruleset de main
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar: comprobaciones de §8 y PR de demostración
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
.gitignore                                   # cambia: + backend/.gradle/, **/build/
.github/
└── workflows/
    ├── ci.yml                               # cambia: job 0 · deteccion, jobs de frontend condicionados, sca sin omitirse, SHA
    └── cierre-ola.yml                       # cambia: solo SHA (pinact)
backend/
├── settings.gradle                          # nuevo: rootProject 'mdd', include plataforma:arquitectura
├── gradlew, gradlew.bat                     # nuevo: Wrapper 9.8.1
├── gradle/wrapper/                          # nuevo: gradle-wrapper.jar y .properties con distributionSha256Sum
├── build.gradle                             # existe (CycloneDX); group 'ec.mdd'
├── gradle.properties                        # cambia: + versionSpringModulith=2.1.1, versionArchUnit=1.4.2
├── buildSrc/
│   ├── build.gradle                         # cambia: + spring-boot-gradle-plugin (versión leída de ../gradle.properties)
│   └── src/main/groovy/
│       ├── mdd.java-conventions.gradle      # cambia: BOM de Modulith, toolVersion SpotBugs 4.10.4, palantir 2.102.0
│       ├── mdd.servicio.gradle              # nuevo: java-library, spring-modulith-api, reglas de arquitectura en test
│       ├── mdd.servicio-central.gradle      # cambia: aplica mdd.servicio en lugar de mdd.java-conventions
│       └── mdd.aplicacion.gradle            # nuevo: Spring Boot, Modulith en test, clase de arranque fuera de JaCoCo
├── config/spotbugs/excluir.xml              # existe
├── plataforma/
│   └── arquitectura/                        # nuevo
│       ├── build.gradle                     # java-library + mdd.java-conventions; api archunit
│       └── src/
│           ├── main/java/ec/mdd/arquitectura/   # reglas de las cinco capas y del dominio sin detalles
│           └── test/java/ec/mdd/arquitectura/   # pruebas de las reglas y de verify(), con fixtures/
└── plantillas/                              # nuevo: esqueletos para copiar (no están en settings.gradle)
    ├── servicio/                            # build.gradle, package-info de las 5 capas, ArquitecturaTest
    └── aplicacion/                          # build.gradle, clase de arranque en ec.mdd, ModulosTest
```

Los servicios irán en `backend/servicios/<servicio>` y los procesos en `backend/apps/<proceso>`. Esta historia no los crea; ver research §10 y §13.

**Structure Decision**: es un monorepo con `backend/` (multiproyecto Gradle), `deploy/` y la carpeta futura `frontend/`. Se respetan las rutas que ya suponen los artefactos del repositorio: `:apps:<proceso>` en `cd.yml`, `backend/` en `ci.yml` y en Dependabot. Las reglas compartidas viven en `backend/plataforma/` y los esqueletos en `backend/plantillas/`.

## Fases de implementación (guía para /speckit-tasks)

1. **Base de construcción:**
   - `.gitignore`, `settings.gradle` y Wrapper 9.8.1 con checksum.
   - Versiones en `gradle.properties` y cambios en `mdd.java-conventions`: BOM de Modulith, SpotBugs 4.10.4 y palantir 2.102.0.
   - `./gradlew build` pasa en local.
2. **Reglas primero (pruebas en rojo):**
   - *Fixtures* y pruebas de `plataforma:arquitectura` para cada violación de capas y de dominio, y para `verify()` con dos módulos.
   - Después, las reglas, hasta que las pruebas pasen.
   - Cobertura ≥ 60 % en el subproyecto.
3. **Convenciones y esqueletos:**
   - `mdd.servicio`, `mdd.servicio-central` (reencadenado) y `mdd.aplicacion`.
   - `plantillas/servicio` y `plantillas/aplicacion`.
   - Prueba local: copiar los esqueletos a un directorio temporal, incluirlos y construir; después se revierte.
4. **Comprobaciones de §8:**
   - palantir con sintaxis de Java 25, SpotBugs sobre clases de Java 25 y Modulith 2.1.1 con Boot 4.1.1.
   - Se registran en `evidencia.md`.
5. **CI:**
   - Job `0 · deteccion` y condiciones de los jobs de frontend y de `sca` en `ci.yml`.
   - `pinact run` y después `pinact run --check`.
   - Abrir el pull request de B00-H01 y ver los 10 checks: los de frontend omitidos y el resto en verde.
6. **Regla de main:**
   - `gh api -X PUT` del ruleset 24901935 con los checks obligatorios, la política estricta y solo *squash*, según [contracts/ci-y-regla-main.md](contracts/ci-y-regla-main.md).
   - Verificar que el pull request de B00-H01 queda sujeto a la regla.
7. **Demostración (SC-001):**
   - Rama `demo/b00-h01` y los 5 pull requests con una violación cada uno.
   - Evidencia en `evidencia.md`; los pull requests se cierran sin fusionar.
8. **Cierre:**
   - Fusionar el pull request de B00-H01 en verde (SC-002).
   - Actualizar la sección 8 de la constitución solo si una comprobación falló, con una decisión D-28 o posterior.

## Complexity Tracking

No hay violaciones de la constitución. La diferencia entre «JUnit 5» y JUnit Jupiter 6.0.3 del BOM de Spring Boot 4.1.1 se resolvió el 2026-10-11 con la enmienda de parche 1.0.1 (D-28), aprobada por el autor (research §6).
