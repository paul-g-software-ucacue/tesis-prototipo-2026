# Evidencia: B00-H01

Registro de las comprobaciones de la sección 8 de la constitución (FR-018, SC-005) y de los pull requests de demostración (SC-001). Cada fila dice el comando, la versión y la salida resumida, o enlaza al run de CI.

**Entorno local:** OpenJDK 25.0.4.1 (Red Hat) y Gradle 9.8.1. Gradle se ejecuta con `-Dorg.gradle.java.home=/usr/lib/jvm/java-25`, para que el daemon corra en Java 25 como en CI (Temurin 25).

## Comprobaciones de la sección 8 de la constitución

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| `palantir-java25` | **fallida parcial** | palantir-java-format 2.102.0 con Spotless 8.10.4, daemon y toolchain en JDK 25. **Formatea bien:** cuerpos de constructor flexibles (sentencias antes de `super()`), variables sin nombre `_` en `for` y `catch`, *pattern matching* con `instanceof` y `switch` como expresión. **Falla:** `import module java.base;` (JEP 511) da `FormatterException: Expected ; after import`. Son problemas conocidos y abiertos de palantir: [#1506](https://github.com/palantir/palantir-java-format/issues/1506) (importación de módulos) y [#1701](https://github.com/palantir/palantir-java-format/issues/1701) (archivos fuente compactos, JEP 512). google-java-format soporta `import module` desde la 1.30.0. Decisión pendiente del autor (ver notas). | 2026-10-11 |
| `spotbugs-java25` | confirmada | La configuración `spotbugs` resuelve `com.github.spotbugs:spotbugs:4.10.4` con `org.ow2.asm:asm:9.10.1`. `spotbugsMain` analiza las clases de `plataforma:arquitectura` (`javap`: *major version* 69, Java 25) y termina sin errores ni `Unsupported class file major version`. | 2026-10-11 |
| `modulith21-boot41` | confirmada | `:plataforma:arquitectura:dependencies --configuration testRuntimeClasspath` resuelve una sola versión de cada pieza: Spring Boot 4.1.1, Spring Modulith 2.1.1, ArchUnit 1.4.2, Spring Framework 7.0.9 y JUnit Jupiter 6.0.3. `archunit-junit5` pide JUnit 5.13.4 y el BOM lo sube a 6.0.3; `ArquitecturaTest` corre bien con esa versión. `ModulosFixturesTest` (2 pruebas) pasa: `verify()` acepta el uso de `puertos.entrada` y lanza `Violations` con el uso de una clase interna. | 2026-10-11 |
| `pinact` | confirmada | pinact 5.0.0 (binario de su release, checksum verificado). `pinact run` fijó las 39 líneas `uses:` de `ci.yml` (27) y `cierre-ola.yml` (12) con su SHA de 40 caracteres y la etiqueta como comentario. `pinact run --check --verify-comment` sale con 0, y no queda ninguna línea `uses:` sin SHA. actionlint 1.7.12 con shellcheck no da errores. | 2026-10-11 |

## Otras comprobaciones locales

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Reglas con *fixtures* (T026, T028, T031) | confirmada | `:plataforma:arquitectura:build`: 10 pruebas pasan (8 de capas y 2 de módulos). Cobertura de líneas 100 % (33 de 33). Antes de T030, las pruebas no compilaban porque faltaba `ReglasCapas` (T029). | 2026-10-11 |
| Esqueletos (T038) | confirmada | Copias temporales `servicios/prueba` y `apps/procesoprueba`: `build` pasa (`ArquitecturaTest` 2, `ModulosTest` 1, cobertura, Spotless y SpotBugs). Una clase de `dominio` con `@Component` hace fallar `ArquitecturaTest > dominioSinDetalles` (escenario 1). Hallazgo: dos subproyectos con el mismo nombre (`apps:prueba` y `servicios:prueba`) crean una dependencia circular porque comparten grupo y nombre; el README de las plantillas pide nombres únicos. Los nombres reales de ADR-012 no se repiten. | 2026-10-11 |
| Servicio sin clases (T040) | confirmada | `servicios/prueba` recién copiado: el informe de JaCoCo no tiene clases con líneas y `jacocoTestCoverageVerification` pasa. No hizo falta ajustar la convención. | 2026-10-11 |
| Umbral exacto (T041) | confirmada | Con 3 de 5 líneas cubiertas (60,0 %) la verificación pasa; con 3 de 6 (50 %) falla con `Rule violated for bundle prueba: lines covered ratio is 0.50, but expected minimum is 0.60`. La clase estaba en un paquete `dominio` y no hay ninguna regla `PACKAGE` del 80 % (D-32). | 2026-10-11 |
| Confianza media no bloquea (T043) | confirmada | Una clase temporal con `new Random()` en un campo da solo `PREDICTABLE_RANDOM` (FindSecBugs). Con `reportLevel` en `MEDIUM` (cambio temporal en local), `spotbugsMain` falla; con `HIGH`, pasa. La convención quedó como estaba (`git diff` vacío). Dato útil: `DMI_RANDOM_USED_ONLY_ONCE` (`new Random().nextInt()`) es de confianza alta y sí bloquea. | 2026-10-11 |
| SQL concatenado (T044) | confirmada | Una clase temporal que ejecuta `Statement.execute("SELECT … '" + parametro + "'")` hace fallar `spotbugsMain` con `SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE`, de confianza alta. `SQL_INJECTION_JDBC` (FindSecBugs) no aparece con `HIGH` porque es de confianza media; ver `pipeline_cicd.md` §11.3 para revisarlo al cerrar la ola 1. | 2026-10-11 |
| Clase de arranque excluida (T042) | confirmada | `apps/procesoprueba`, cuya única clase es `PruebaAplicacion`: el informe de JaCoCo no la incluye y `jacocoTestCoverageVerification` pasa. Hallazgo: `mdd.aplicacion` necesita `spring-boot-starter` para compilar `@SpringBootApplication`; se agregó. | 2026-10-11 |

## CI del pull request de B00-H01

| Run | Estado del PR | Resultado | Enlace |
|---|---|---|---|
| 1 (commit `843c635`) | borrador | `0 · deteccion` (`frontend=false`: `1 · lint-frontend` quedó omitido), `1 · lint-backend`, `2 · test-backend` y `3 · secretos` pasan. `1 · lint-frontend`, `2 · test-frontend`, `2 · test-integracion`, `4 · sbom-backend`, `4 · sbom-frontend` y `4 · sca` quedan *skipped*, como corresponde a un borrador (caso límite «Pull request en borrador»). | [run 38195137742](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38195137742) |
| 2 y 3 (commit `1006a2e`) | push y `gh pr ready` casi simultáneos | **Carrera:** el run de `ready_for_review` quedó cancelado por la concurrencia del workflow, y el que sobrevivió lo disparó el push mientras el PR era borrador. Por eso omitió `test-integracion`, `sbom-backend` y `sca`, aunque el PR ya estaba listo (ver notas). | [run 38195686451](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38195686451) (cancelado), [run 38195687927](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38195687927) |
| 4 (commit `1006a2e`) | listo para revisión | Los 10 checks según el contrato: `0 · deteccion`, `1 · lint-backend`, `2 · test-backend`, `2 · test-integracion`, `3 · secretos`, `4 · sbom-backend` y `4 · sca` pasan. `4 · sca` corre aunque se omita `sbom-frontend` (caso límite «repositorio sin frontend»). Solo `1 · lint-frontend`, `2 · test-frontend` y `4 · sbom-frontend` quedan *skipped*. Secretos y SCA pasan, así que no hace falta la excepción transitoria de D-31. | [run 38195997188](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38195997188) |

## Pull requests de demostración

| PR | Violación | Check en rojo | Mensaje | «Merging is blocked» |
|---|---|---|---|---|

## Notas

- Excepciones (research §8 y §14): ninguna. Los 10 checks están en la regla.
- **Ruleset pendiente (T022):** el `PUT` del ruleset 24901935 responde 404 porque `gh` está autenticado como Paul-SSD, colaborador con permiso de escritura (`admin: false`). En un repositorio de cuenta personal solo el dueño, `paul-g-software-ucacue`, puede cambiar los rulesets. El JSON está listo en `contracts/ruleset-main.json`.
- **Carrera entre push y `ready_for_review`:** si se marca el PR como listo mientras corre un push, la concurrencia del workflow (`cancel-in-progress`) puede cancelar el run de `ready_for_review`. Entonces el último run de ese commit es el del borrador, y `2 · test-integracion`, `4 · sbom-backend` y `4 · sca` quedan *skipped*, que GitHub cuenta como aprobados. **Procedimiento:** marcar el PR como listo después de que termine el push y, antes de fusionar, comprobar que esos tres checks están en éxito y no *skipped* (T046). Si quedaron *skipped*, pasar el PR a borrador y de nuevo a listo (`gh pr ready --undo` y `gh pr ready`). Así se resolvió en el PR #74 (run 4).
- **Decisión pendiente (constitución §8, FR-018):** palantir-java-format no soporta dos construcciones de Java 25, la importación de módulos (JEP 511) y los archivos fuente compactos (JEP 512). Mientras el autor no decida, se sigue con palantir: Spotless falla si aparece alguna de las dos, así que la CI ya impide que entren al código.
