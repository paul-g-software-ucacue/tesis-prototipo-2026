# Evidencia: B00-H01

Registro de las comprobaciones de la sección 8 de la constitución (FR-018, SC-005) y de los pull requests de demostración (SC-001). Cada fila dice el comando, la versión y la salida resumida, o enlaza al run de CI.

**Entorno local:** OpenJDK 25.0.4.1 (Red Hat) y Gradle 9.8.1. Gradle se ejecuta con `-Dorg.gradle.java.home=/usr/lib/jvm/java-25`, para que el daemon corra en Java 25 como en CI (Temurin 25).

## Comprobaciones de la sección 8 de la constitución

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| `palantir-java25` | **fallida parcial** | palantir-java-format 2.102.0 con Spotless 8.10.4, daemon y toolchain en JDK 25. **Formatea bien:** cuerpos de constructor flexibles (sentencias antes de `super()`), variables sin nombre `_` en `for` y `catch`, *pattern matching* con `instanceof` y `switch` como expresión. **Falla:** `import module java.base;` (JEP 511) da `FormatterException: Expected ; after import`. Son problemas conocidos y abiertos de palantir: [#1506](https://github.com/palantir/palantir-java-format/issues/1506) (importación de módulos) y [#1701](https://github.com/palantir/palantir-java-format/issues/1701) (archivos fuente compactos, JEP 512). google-java-format soporta `import module` desde la 1.30.0. Decisión pendiente del autor (ver notas). | 2026-10-11 |
| `spotbugs-java25` | pendiente | | |
| `modulith21-boot41` | confirmada | `:plataforma:arquitectura:dependencies --configuration testRuntimeClasspath` resuelve una sola versión de cada pieza: Spring Boot 4.1.1, Spring Modulith 2.1.1, ArchUnit 1.4.2, Spring Framework 7.0.9 y JUnit Jupiter 6.0.3. `archunit-junit5` pide JUnit 5.13.4 y el BOM lo sube a 6.0.3; `ArquitecturaTest` corre bien con esa versión. `ModulosFixturesTest` (2 pruebas) pasa: `verify()` acepta el uso de `puertos.entrada` y lanza `Violations` con el uso de una clase interna. | 2026-10-11 |
| `pinact` | confirmada | pinact 5.0.0 (binario de su release, checksum verificado). `pinact run` fijó las 39 líneas `uses:` de `ci.yml` (27) y `cierre-ola.yml` (12) con su SHA de 40 caracteres y la etiqueta como comentario. `pinact run --check --verify-comment` sale con 0, y no queda ninguna línea `uses:` sin SHA. actionlint 1.7.12 con shellcheck no da errores. | 2026-10-11 |

## Otras comprobaciones locales

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Reglas con *fixtures* (T026, T028, T031) | confirmada | `:plataforma:arquitectura:build`: 10 pruebas pasan (8 de capas y 2 de módulos). Cobertura de líneas 100 % (33 de 33). Antes de T030, las pruebas no compilaban porque faltaba `ReglasCapas` (T029). | 2026-10-11 |
| Esqueletos (T038) | confirmada | Copias temporales `servicios/prueba` y `apps/procesoprueba`: `build` pasa (`ArquitecturaTest` 2, `ModulosTest` 1, cobertura, Spotless y SpotBugs). Una clase de `dominio` con `@Component` hace fallar `ArquitecturaTest > dominioSinDetalles` (escenario 1). Hallazgo: dos subproyectos con el mismo nombre (`apps:prueba` y `servicios:prueba`) crean una dependencia circular porque comparten grupo y nombre; el README de las plantillas pide nombres únicos. Los nombres reales de ADR-012 no se repiten. | 2026-10-11 |
| Servicio sin clases (T040) | confirmada | `servicios/prueba` recién copiado: el informe de JaCoCo no tiene clases con líneas y `jacocoTestCoverageVerification` pasa. No hizo falta ajustar la convención. | 2026-10-11 |
| Umbral exacto (T041) | confirmada | Con 3 de 5 líneas cubiertas (60,0 %) la verificación pasa; con 3 de 6 (50 %) falla con `Rule violated for bundle prueba: lines covered ratio is 0.50, but expected minimum is 0.60`. La clase estaba en un paquete `dominio` y no hay ninguna regla `PACKAGE` del 80 % (D-32). | 2026-10-11 |
| Clase de arranque excluida (T042) | confirmada | `apps/procesoprueba`, cuya única clase es `PruebaAplicacion`: el informe de JaCoCo no la incluye y `jacocoTestCoverageVerification` pasa. Hallazgo: `mdd.aplicacion` necesita `spring-boot-starter` para compilar `@SpringBootApplication`; se agregó. | 2026-10-11 |

## CI del pull request de B00-H01

| Run | Estado del PR | Resultado | Enlace |
|---|---|---|---|

## Pull requests de demostración

| PR | Violación | Check en rojo | Mensaje | «Merging is blocked» |
|---|---|---|---|---|

## Notas

- Excepciones (research §8 y §14): ninguna por ahora.
- **Decisión pendiente (constitución §8, FR-018):** palantir-java-format no soporta dos construcciones de Java 25, la importación de módulos (JEP 511) y los archivos fuente compactos (JEP 512). Mientras el autor no decida, se sigue con palantir: Spotless falla si aparece alguna de las dos, así que la CI ya impide que entren al código.
