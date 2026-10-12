# Evidencia: B00-H01

Registro de las comprobaciones de la sección 8 de la constitución (FR-018, SC-005) y de los pull requests de demostración (SC-001). Cada fila dice el comando, la versión y la salida resumida, o enlaza al run de CI.

**Entorno local:** OpenJDK 25.0.4.1 (Red Hat) y Gradle 9.8.1. Gradle se ejecuta con `-Dorg.gradle.java.home=/usr/lib/jvm/java-25`, para que el daemon corra en Java 25 como en CI (Temurin 25).

## Comprobaciones de la sección 8 de la constitución

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| `palantir-java25` | **fallida parcial** | palantir-java-format 2.102.0 con Spotless 8.10.4, daemon y toolchain en JDK 25. **Formatea bien:** cuerpos de constructor flexibles (sentencias antes de `super()`), variables sin nombre `_` en `for` y `catch`, *pattern matching* con `instanceof` y `switch` como expresión. **Falla:** `import module java.base;` (JEP 511) da `FormatterException: Expected ; after import`. Son problemas conocidos y abiertos de palantir: [#1506](https://github.com/palantir/palantir-java-format/issues/1506) (importación de módulos) y [#1701](https://github.com/palantir/palantir-java-format/issues/1701) (archivos fuente compactos, JEP 512). google-java-format soporta `import module` desde la 1.30.0. Decisión pendiente del autor (ver notas). | 2026-10-11 |
| `spotbugs-java25` | pendiente | | |
| `modulith21-boot41` | pendiente | | |
| `pinact` | confirmada | pinact 5.0.0 (binario de su release, checksum verificado). `pinact run` fijó las 39 líneas `uses:` de `ci.yml` (27) y `cierre-ola.yml` (12) con su SHA de 40 caracteres y la etiqueta como comentario. `pinact run --check --verify-comment` sale con 0, y no queda ninguna línea `uses:` sin SHA. actionlint 1.7.12 con shellcheck no da errores. | 2026-10-11 |

## Otras comprobaciones locales

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## CI del pull request de B00-H01

| Run | Estado del PR | Resultado | Enlace |
|---|---|---|---|

## Pull requests de demostración

| PR | Violación | Check en rojo | Mensaje | «Merging is blocked» |
|---|---|---|---|---|

## Notas

- Excepciones (research §8 y §14): ninguna por ahora.
- **Decisión pendiente (constitución §8, FR-018):** palantir-java-format no soporta dos construcciones de Java 25, la importación de módulos (JEP 511) y los archivos fuente compactos (JEP 512). Mientras el autor no decida, se sigue con palantir: Spotless falla si aparece alguna de las dos, así que la CI ya impide que entren al código.
