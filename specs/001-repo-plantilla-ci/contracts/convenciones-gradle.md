# Contrato: plugins de convención de Gradle

Decisiones: ADR-012 (plugins y paquete base), D-32 (cobertura única del 60 %) y D-33 (google-java-format), en `docs/decisiones.md`.

Viven en `backend/buildSrc/src/main/groovy/`. Un subproyecto solo aplica **un** plugin de convención; los demás se encadenan solos. Ningún subproyecto repite la configuración que da su plugin (FR-002).

```text
mdd.java-conventions          (todos)
├── mdd.servicio              (los 12 servicios)
├── mdd.aplicacion            (los 4 procesos)
└── plataforma:arquitectura   (lo aplica directamente, con java-library)
```

## `mdd.java-conventions` (existe; cambia)

| Aspecto | Contrato | Cambio en B00-H01 |
|---|---|---|
| Java | Toolchain 25 | — |
| BOM | `spring-boot-dependencies` 4.1.1 en `implementation` y en las suites de prueba | **+** `spring-modulith-bom` 2.1.1 |
| Suites | `test` (JUnit Jupiter, sin Docker) e `integrationTest` (Testcontainers, después de `test`) | — |
| JaCoCo | 0.8.15. `check` depende de `jacocoTestCoverageVerification`. Regla `LINE ≥ 0.60` del módulo; es el único umbral (D-32). | El comentario que mencionaba el 80 % de `mdd.servicio-central` pasa a decir que el 60 % es el único umbral |
| Spotless | `src/*/java/**/*.java`, google-java-format en estilo AOSP, `removeUnusedImports`, espacios finales y salto de línea final | **Formateador fijado (D-33):** `googleJavaFormat('1.36.1').aosp()`, que reemplaza a palantir-java-format |
| SpotBugs | `effort MAX`, `reportLevel HIGH`, `ignoreFailures false`, FindSecBugs 1.14.0, solo `spotbugsMain`, exclusión de `build/generated` | **Versión fijada:** `toolVersion = '4.10.4'` |

## `mdd.servicio` (nuevo)

Aplica `java-library` y `mdd.java-conventions`.

| Configuración | Dependencia | Para qué |
|---|---|---|
| `implementation` | `org.springframework.modulith:spring-modulith-api` | `@NamedInterface` en `puertos.entrada` |
| `testImplementation` | `project(':plataforma:arquitectura')` | Las reglas de las cinco capas |
| `testImplementation` | `com.tngtech.archunit:archunit-junit5:${versionArchUnit}` | `@AnalyzeClasses` y `@ArchTest` en `ArquitecturaTest` |

No agrega Spring Web, JPA ni Flyway.

## `mdd.servicio-central` (se elimina)

Solo agregaba la regla `PACKAGE` del 80 % en `*.dominio`. Con D-32 no aporta nada y se borra `backend/buildSrc/src/main/groovy/mdd.servicio-central.gradle` (principio XI).

## `mdd.aplicacion` (nuevo)

Aplica `mdd.java-conventions` y `org.springframework.boot`.

| Aspecto | Contrato |
|---|---|
| Plugin de Spring Boot | 4.1.1. La versión se lee de `backend/gradle.properties` (`versionSpringBoot`) en `buildSrc/build.gradle`, que es la única fuente de la versión. |
| `implementation` | `org.springframework.boot:spring-boot-starter`, lo mínimo para `@SpringBootApplication` (agregado al implementar, T042) |
| `testImplementation` | `org.springframework.modulith:spring-modulith-starter-test` para `ApplicationModules` |
| JaCoCo | Excluye las clases `**/*Aplicacion.class` del informe y de la verificación |
| `bootJar` | `apps/<proceso>/build/libs/<proceso>.jar`, que es lo que espera `cd.yml` (B00-H08) |

## Versiones en `backend/gradle.properties`

| Propiedad | Valor | Usada por |
|---|---|---|
| `versionSpringBoot` | `4.1.1` (existe) | `mdd.java-conventions` y `buildSrc/build.gradle` |
| `versionSpringModulith` | `2.1.1` (nueva) | `mdd.java-conventions` |
| `versionArchUnit` | `1.4.2` (nueva; la misma que usa Modulith 2.1.1) | `mdd.servicio` y `plataforma:arquitectura` |

Las versiones de google-java-format, SpotBugs, FindSecBugs y JaCoCo quedan escritas en los scripts de convención, como ya están hoy las de FindSecBugs y JaCoCo.
