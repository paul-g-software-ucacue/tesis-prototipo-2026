# Investigación (fase 0): B00-H01 · Repositorio, plantilla de servicio en cinco capas y CI con Gradle

**Fecha:** 2026-10-11 · **Especificación:** [spec.md](spec.md) · **Plan:** [plan.md](plan.md)

Fuentes leídas además de la constitución: ADR-008, ADR-009 y ADR-011, y `sdd/pipeline_cicd.md` v1.0 (copia local en `~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/`). También el `ci.yml`, la convención de Gradle que ya está en el repositorio, el ruleset `main` del repositorio en GitHub y los POM publicados en Maven Central.

Las decisiones que salieron de esta investigación están registradas en [`docs/decisiones.md`](../../docs/decisiones.md): D-28 (§6), D-29 (§7), D-30, D-31 (§8), D-32 (§18) y ADR-012 (§10, §12 y §13).

En las comprobaciones de la sección 8 de la constitución, este documento solo registra la **evidencia previa**. La confirmación con código se hace al implementar y queda en `evidencia.md` (FR-018).

---

## 1. Línea de Spring Modulith para Spring Boot 4.1 (FR-017)

- **Decisión:** Spring Modulith **2.1.1**, importado con su BOM (`spring-modulith-bom`), porque el BOM de Spring Boot 4.1.1 no lo gestiona.
- **Evidencia previa:** el POM de `spring-modulith-core:2.1.1` depende de `spring-boot-autoconfigure:4.1.1`, `spring-context:7.0.9` y `archunit:1.4.2`. El POM de `spring-modulith-starter-test:2.1.1` depende de `spring-boot-starter-test:4.1.1`. La 2.2 M1 pasa a Spring Boot 4.2 M1 ([blog de Spring, 26/08/2026](https://spring.io/blog/2026/08/26/spring-modulith-2-2-m1-2-1-1-2-0-8-and-1-4-13-released/)), y la 2.1 GA salió el 11/06/2026 ([blog de Spring](https://spring.io/blog/2026/06/11/spring-modulith-2-1-ga-2-0-7-and-1-4-12-released/)).
- **Confirmación pendiente:** compilar y correr `ApplicationModules.verify()` con Boot 4.1.1 y Modulith 2.1.1 en el mismo classpath, sin conflictos de versión.
- **Alternativas consideradas:** 2.2.x, que es un hito para Boot 4.2 y queda descartada; 2.0.x, que es la línea de Boot 4.0 y también queda descartada.

## 2. palantir-java-format con Java 25 (FR-016)

- **Decisión:** fijar la versión del formateador en la convención, `palantirJavaFormat('2.102.0')` (la última en Maven Central el 11/10/2026), en lugar de depender de la versión por defecto de Spotless 8.10.4.
- **Por qué:** se sabe de versiones antiguas de palantir que usan API del JDK que ya no existen en Java 25 ([OSGeo, «Exploring builds with Java 25»](https://discourse.osgeo.org/t/exploring-builds-with-java-25/149783)). Las notas de versión de palantir no nombran Java 25 de forma explícita ([releases](https://github.com/palantir/palantir-java-format/releases)). Fijar la versión hace que la comprobación sea repetible.
- **Confirmación pendiente:** `spotlessCheck` y `spotlessApply` sobre un archivo que use sintaxis de Java 25: importación de módulos (`import module java.base;`), cuerpos de constructor flexibles y variables sin nombre (`_`). La prueba corre con el JDK 25 local y en CI.
- **Si falla:** se registra una decisión nueva en `docs/decisiones.md`, por ejemplo `googleJavaFormat()` o Eclipse JDT en Spotless, y se enmienda la constitución (sección 8). No se desactiva Spotless en silencio.

## 3. SpotBugs con clases de Java 25 (FR-016)

- **Decisión:** fijar `toolVersion = '4.10.4'` en el bloque `spotbugs` de la convención. Es la última versión, del 19/08/2026 según el [CHANGELOG](https://github.com/spotbugs/spotbugs/blob/master/CHANGELOG.md).
- **Evidencia previa:** el POM de `spotbugs:4.10.4` depende de ASM **9.10.1**. ASM lee el *class file* 69 (Java 25) desde la 9.8. El changelog de la 4.10.0 corrige un error al iniciar SpotBugs en Java 25. FindSecBugs 1.14.0 se mantiene.
- **Confirmación pendiente:** `spotbugsMain` analiza las clases de `plataforma:arquitectura`, compiladas con Java 25, sin el error `Unsupported class file major version 69`. En el pull request de demostración (escenario 5), la consulta SQL concatenada da `SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE` o `SQL_INJECTION_JDBC` con confianza alta.
- **Alternativa considerada:** dejar la versión por defecto del plugin 6.5.12. Se descarta porque no se puede saber qué ASM trae sin resolverla, y la comprobación tiene que ser explícita.

## 4. ArchUnit y Java 25

- **Decisión:** ArchUnit **1.4.2**, la misma versión que trae Spring Modulith 2.1.1, con `archunit-junit5` 1.4.2 para `@ArchTest`. Una sola versión evita dos ArchUnit en el classpath.
- **Evidencia previa:** la 1.4.1 agregó el soporte de Java 25 y del *class file* 69 ([notas de la 1.4.1](https://newreleases.io/project/github/TNG/ArchUnit/release/v1.4.1)). Ya existe la 1.5.1, pero Modulith 2.1.1 está probado con la 1.4.2.
- **Alternativa considerada:** ArchUnit 1.5.1. Se descarta por ahora para no adelantarse a la versión que usa Modulith; Dependabot la propondrá.

## 5. Gradle

- **Decisión:** Gradle Wrapper **9.8.1** (la versión actual según `services.gradle.org`), en `backend/`, con `distributionSha256Sum`. ADR-011 exige 9.1 o posterior, porque la 9.1 es la primera que corre en Java 25.
- `gradle/actions/setup-gradle@v6` valida el checksum del wrapper en CI.
- El entorno local no tiene Gradle instalado. El wrapper se genera una vez con una distribución descargada y verificada por checksum, y después solo se usa `./gradlew`.
- **Archivo nuevo:** `backend/settings.gradle`, que hoy no existe y sin el cual `./gradlew` no puede correr.

## 6. JUnit: el BOM de Boot 4.1.1 trae JUnit Jupiter 6

- **Hallazgo:** el BOM de `spring-boot-dependencies:4.1.1` fija `junit-jupiter.version = 6.0.3` (JUnit 6), Mockito 5.23.0 y Testcontainers 2.0.5. La constitución 1.0 (sección 3) y ADR-008 y ADR-011 decían «JUnit 5».
- **Decisión:** usar la versión que fija el BOM, como dice `sdd/pipeline_cicd.md` («Mockito, JUnit 5, AssertJ y Testcontainers no llevan versión propia: las fija el BOM de Spring Boot 4.1.1»). Forzar JUnit 5 contra el BOM sería una desviación sin beneficio.
- **Acción propuesta:** enmienda de parche de la constitución, que cambia «JUnit 5» por «JUnit (Jupiter) 6, la del BOM de Spring Boot». Es una aclaración y no cambia ningún principio (ver «Complexity Tracking» en el plan).
- **Confirmada por el autor (2026-10-11):** se usa JUnit Jupiter 6.0.3. Registrada como D-28; la constitución se enmendó a la 1.0.1 el mismo día. ADR-008 y ADR-011, fuera del repositorio, todavía dicen «JUnit 5».

## 7. Jobs de frontend condicionados a `frontend/package.json` (FR-008)

Registrada como **D-29**.

- **Decisión:** un job nuevo, `0 · deteccion`, hace un checkout disperso (*sparse*) solo de `frontend/package.json` y publica la salida `frontend=true|false`. `lint-frontend` y `sbom-frontend` usan `needs: deteccion` y `if: needs.deteccion.outputs.frontend == 'true'`. `test-frontend` ya depende de `lint-frontend` y se omite con él.
- **Por qué un job y no `hashFiles()`:** en el `if` de un job, GitHub Actions solo admite los contextos `github`, `needs`, `vars` e `inputs`. `hashFiles()` no está disponible ahí porque todavía no hay espacio de trabajo.
- **Riesgo que se cubre:**
  - Si `deteccion` falla, los jobs de frontend quedan omitidos y un job omitido cuenta como aprobado. Por eso `0 · deteccion` también es un check obligatorio.
  - El job `sca` hoy necesita `[sbom-backend, sbom-frontend]`. Con `sbom-frontend` omitido, `sca` también se omitiría y el SBOM del backend quedaría sin revisar (caso límite de la especificación). Por eso `sca` pasa a correr cuando `sbom-backend` termina bien y `sbom-frontend` termina bien **o** se omite: `!cancelled() && needs.sbom-backend.result == 'success' && contains(fromJSON('["success","skipped"]'), needs.sbom-frontend.result)`.
- **ESLint:** se queda en `lint-frontend` del workflow, que es lo que usará B00-H06. Su escenario ahora está en #23.
- **Alternativas consideradas:**
  - un filtro `paths` a nivel de workflow, que actúa sobre los archivos cambiados y no sobre si el archivo existe, y además dejaría checks obligatorios esperando para siempre;
  - `dorny/paths-filter`, que agrega otra acción de terceros;
  - quitar los jobs, que es la opción C que el autor descartó.

## 8. Regla de la rama principal (FR-009)

Registrada como **D-31**. El JSON conserva las reglas que ya tenía el ruleset (incluida `require_extra_approval_for_unattributed_changes: true`) y fija `bypass_actors: []`, para que nadie pueda saltarse la regla.

- **Estado actual, consultado con la API:** el ruleset `main` (id 24901935) está activo sobre `~DEFAULT_BRANCH`. Prohíbe borrar la rama y los *force-push*, y exige pull request con `allowed_merge_methods: [merge, squash, rebase]`. **No exige checks.**
- **Decisión (confirmada por el autor el 2026-10-11):** actualizar el ruleset con `gh api -X PUT` según la sección 8 de `sdd/pipeline_cicd.md`:
  - `required_status_checks` con la política estricta (el PR tiene que estar al día con `main`) y estos checks de GitHub Actions: `0 · deteccion`, `1 · lint-backend`, `1 · lint-frontend`, `2 · test-backend`, `2 · test-integracion`, `2 · test-frontend`, `3 · secretos`, `4 · sbom-backend`, `4 · sbom-frontend` y `4 · sca`;
  - solo fusión *squash* (`allowed_merge_methods: [squash]`), que es lo que supone `cd.yml`.
- **Nombres de los checks:** un check de GitHub Actions toma el `name:` del job, por ejemplo `1 · lint-backend`. El ruleset tiene que usar exactamente ese texto.
- **Jobs omitidos:** un job omitido por su `if` informa «skipped», y GitHub lo cuenta como aprobado. Así, los jobs de frontend sin `frontend/` y los de integración y SCA en los borradores no bloquean.
- **Orden:** el ruleset se actualiza **al final** de la historia, cuando los checks nuevos ya corrieron una vez en el pull request de B00-H01. Así, ese pull request es el primero que queda bloqueado por la regla.
- **Secretos y SCA:** su comportamiento es de B00-H07. Si en el pull request de B00-H01 alguno falla por algo que corresponde a esa historia, se deja fuera de la lista obligatoria, se anota en `evidencia.md` y B00-H07 lo agrega.
- **Minutos:** con GitHub Pro, la cuenta tiene 3 000 minutos al mes para repositorios privados, en lugar de los 2 000 que suponía `pipeline_cicd.md`. La estimación de 975 a 1 450 minutos queda con más holgura.

## 9. Acciones fijadas por SHA (FR-010)

- **Decisión:** ejecutar `pinact run` sobre `.github/workflows/*.yml` (`ci.yml` y `cierre-ola.yml`). `cd.yml` todavía no está en el repositorio y es de B00-H08. Dependabot (`github-actions`) mantiene los SHA al día.
- **Evidencia previa:** las 9 etiquetas usadas existen en GitHub: `actions/checkout@v7`, `setup-java@v6`, `setup-node@v7`, `upload-artifact@v7`, `download-artifact@v8`, `gradle/actions/setup-gradle@v6`, `anchore/scan-action/download-grype@v7`, `grafana/setup-k6-action@v1` y `grafana/run-k6-action@v1`.
- **Verificación:** `pinact run --check` sin hallazgos (SC-004). La imagen de Gitleaks se usa con `docker run` y una etiqueta (`v8.30.1`), no como acción. Fijarla por *digest* es de B00-H07.
- **Alternativa considerada:** un paso de CI con `pinact run --check` en cada pull request. Se descarta por ahora: suma minutos, y Dependabot más la revisión del pull request cubren las acciones nuevas.

## 10. Plantilla de servicio: qué es y dónde vive (FR-003 a FR-006)

Registrada en **ADR-012**. En este documento, «plantilla» es el conjunto de reglas, plugins y esqueletos; «esqueleto» es solo la carpeta que se copia.

- **Decisión:**
  1. **Reglas compartidas, compiladas y probadas:** el subproyecto `plataforma:arquitectura` contiene las reglas ArchUnit de las cinco capas y de las dependencias prohibidas del dominio. Sus pruebas usan *fixtures*: una correcta, que pasa, y una por cada violación, que falla. Esto prueba los escenarios 1 y 2 de forma automática en cada pull request.
  2. **Plugins de convención:** `mdd.servicio` (nuevo) y `mdd.aplicacion` (nuevo). `mdd.servicio-central` se elimina, porque solo agregaba el umbral del 80 % (D-32). Un servicio recibe las reglas, la cobertura, Spotless y SpotBugs con solo aplicar su plugin (FR-002, FR-013, SC-006).
  3. **Esqueletos para copiar:** `backend/plantillas/servicio/` y `backend/plantillas/aplicacion/`. Cada uno trae un `build.gradle` con el plugin, un `package-info.java` por capa y una sola prueba de arquitectura (servicio) o de módulos (aplicación). No se incluyen en `settings.gradle`: se copian a `servicios/<nombre>` o a `apps/<proceso>` cuando una historia crea el servicio o el proceso.
- **Por qué no compilar la plantilla como subproyecto:** necesitaría clases de ejemplo en las cinco capas (ArchUnit falla si no analiza ninguna clase, y JaCoCo mediría código que no es del producto). Eso es código que no pide la ERS (principio X) y términos fuera del glosario (principio III). Las *fixtures* de `plataforma:arquitectura` cumplen ese papel dentro de las pruebas.
- **Por qué no crear ahora los servicios ni los procesos reales:** los servicios llegan con sus historias, la primera es F04-H01 (Normativa), y los 4 procesos con B00-H02. Esta historia entrega la plantilla y las reglas, no los módulos.
- **Alternativas consideradas:**
  - un subproyecto «servicio-ejemplo» compilado (descartado por lo que se explica arriba);
  - un generador o script de andamiaje (abstracción especulativa, principio XI);
  - copiar las reglas en cada servicio (contradice FR-002).

## 11. Reglas de capas con ArchUnit (FR-004, FR-005)

- **Decisión:** dos grupos de reglas en `plataforma:arquitectura`.
  - **Sentido de las capas** (`layeredArchitecture()`, con `withOptionalLayers(true)` para que un servicio nuevo o de soporte sin alguna capa no falle):
    - Dominio solo lo usan Aplicación, Puertos, Adaptadores e Infraestructura.
    - Puertos solo lo usan Aplicación, Adaptadores e Infraestructura.
    - Aplicación solo la usan Adaptadores e Infraestructura.
    - Adaptadores solo los usa Infraestructura.
    - Ninguna capa usa Infraestructura.
  - **Dominio sin detalles** (`noClasses().that().resideInAPackage("..dominio..")`). El dominio no depende de:
    - Spring: `org.springframework..`
    - JPA y ORM: `jakarta.persistence..` y `org.hibernate..`
    - HTTP: `jakarta.servlet..`, `java.net.http..`, `org.apache.hc..` y `okhttp3..`
    - PDF: `org.apache.pdfbox..` y `com.openhtmltopdf..`

  Las reglas usan `allowEmptyShould(true)`, para que un servicio recién creado, todavía sin clases, no falle.
- **Paquetes de cada capa:** `..dominio..`, `..aplicacion..`, `..puertos..`, `..adaptadores..` y `..infraestructura..` (ADR-009, identificadores en español sin tildes, ADR-008).
- **Pruebas de las reglas:** `ClassFileImporter` sobre los paquetes de *fixtures* de prueba. Se espera `AssertionError` en cada violación (dominio que importa Spring, JPA, HTTP o PDF; aplicación que usa un adaptador; puerto que usa infraestructura) y ningún error en la *fixture* correcta.

## 12. API publicada de un servicio y Spring Modulith (FR-006)

Registrada en **ADR-012**.

- **Decisión:** dentro de un proceso, la API publicada de un servicio es su paquete `puertos.entrada`, marcado con `@NamedInterface("api")` (de `spring-modulith-api`) en su `package-info.java`. Todo lo demás es interno.
- **Por qué:** con el diseño de Modulith, el módulo es `ec.mdd.<servicio>` y sus subpaquetes son internos por defecto. Las cinco capas son subpaquetes, así que hay que publicar uno de forma explícita. El puerto de entrada es por definición «lo que ofrece el servicio» (principio I), y los servicios de un proceso se llaman por la interfaz de aplicación que refleja el contrato (principios IV y V).
- **Servicios de soporte o genéricos sin puerto de entrada:** si otro servicio del mismo proceso necesita llamarlos, en esa historia se les agrega su `puertos.entrada`. Si nadie los llama, no lo necesitan.
- **Eventos de dominio que se escuchan en el mismo proceso:** cuando una historia los necesite (por ejemplo, Aprobación escuchando a Distributivo), se publican con un `@NamedInterface` propio. Esta historia no los anticipa (principio XI).
- **Pruebas:** `plataforma:arquitectura` incluye una *fixture* de dos módulos. Se comprueba que `ApplicationModules.of(...).verify()` pasa si uno usa el `puertos.entrada` del otro y falla con `Violations` si usa una clase de su `dominio` o de su `aplicacion`. Las *fixtures* están en el código de prueba, así que se importan con una `ImportOption` que incluye las clases de prueba.
- **Proceso:** el esqueleto de aplicación trae `ModulosTest` con `ApplicationModules.of(<Proceso>Aplicacion.class).verify()`. La clase de arranque vive en `ec.mdd`, así que los módulos son los `ec.mdd.<servicio>` que el proceso tiene en su classpath.

## 13. Paquete base y nombres de los subproyectos

- **Decisión (confirmada por el autor el 2026-10-11; ADR-012):**
  - paquete base `ec.mdd` y grupo de Gradle `ec.mdd`;
  - un servicio usa `ec.mdd.<servicio>` (`ec.mdd.normativa`, `ec.mdd.validacionocs`…);
  - las rutas son `backend/servicios/<servicio>` (proyecto `:servicios:<servicio>`), `backend/apps/<proceso>` (`:apps:identidad`, `:apps:datos`, `:apps:distributivo` y `:apps:salida`, como ya supone `cd.yml`) y `backend/plataforma/arquitectura`.
- **Por qué:** ningún documento fija el paquete. `ec.mdd` no revela el tema de la tesis más allá de la sigla del producto. El repositorio es privado y la sección 6 de `pipeline_cicd.md` solo restringe los nombres del repositorio, de la imagen y del dominio. Los nombres de los servicios siguen las etiquetas `modulo:NN-nombre`, sin número ni guiones, porque un paquete de Java no puede empezar con un dígito.
- **Alternativa considerada:** un dominio invertido de la institución, como `ec.edu.<ist>.mdd`. Se descarta porque revelaría la institución en el código.

## 14. Cobertura en procesos y en servicios nuevos

- **Clase de arranque del proceso:** `mdd.aplicacion` excluye de JaCoCo la clase `*Aplicacion` (solo `main`), porque no tiene lógica y ninguna prueba ejecuta `main`. Sin esa exclusión, un proceso sin otra clase propia quedaría siempre bajo el 60 %.
- **Servicio sin clases:** se comprueba al implementar que `jacocoTestCoverageVerification` no falla cuando un módulo todavía no tiene clases (0 de 0 líneas). Si fallara, se ajusta en la convención y se anota en `evidencia.md`.

## 15. Pruebas de contrato y de integración en la historia

- La etapa de pruebas ya corre `test`, donde estarán las pruebas de contrato con swagger-request-validator, e `integrationTest`, con Testcontainers.
- En B00-H01 todavía no hay contratos ni adaptadores: el esquema `Error` es de B00-H04 y los adaptadores llegan con sus historias. Las tareas existen y quedan en NO-SOURCE o sin pruebas, y la etapa ya está lista para ellas.
- No se agrega swagger-request-validator hasta que haya un contrato (principio XI: no agregar dependencias sin uso).

## 16. Entorno local

- JDK 25 instalado: OpenJDK 25.0.4.1 de Red Hat. El toolchain de Gradle acepta cualquier proveedor; en CI se usa Temurin.
- **Sin Docker y con Podman 5.8.7:** para correr `integrationTest` localmente con Testcontainers se activa `podman.socket` y se exportan `DOCKER_HOST` y `TESTCONTAINERS_RYUK_DISABLED=true` (ver [quickstart.md](quickstart.md)). En esta historia no hay pruebas de integración, así que no bloquea.
- Hay que instalar `pinact` (binario de su release). `gh` ya está autenticado con acceso al ruleset.

## 17. Demostración de los escenarios (SC-001)

- **Escenarios 1 y 2:** se prueban de forma automática con las *fixtures* (sección 11 y sección 12) y, además, en los pull requests de demostración.
- **Escenarios 3 a 5 (CI, cobertura y SpotBugs):** se prueban con **pull requests de demostración** contra `main`, cuando el ruleset ya exija los checks:
  1. Desde una rama base `demo/b00-h01`, se crea un servicio de prueba con la plantilla y se agrega a `settings.gradle`.
  2. Se abren 5 pull requests, cada uno con una sola violación: dominio que importa Spring; uso de una clase interna de otro servicio del mismo proceso; cobertura bajo el 60 %; consulta SQL concatenada; prueba en rojo. Un sexto pull request agrega solo `frontend/package.json` para comprobar el caso límite «Llega el frontend» (D-29).
  3. En cada uno se registran el enlace, el check en rojo y el mensaje «Merging is blocked».
  4. Se cierran sin fusionar y se borran las ramas.

  La rama `demo/b00-h01` nunca se fusiona.
- **SC-002:** el propio pull request de B00-H01, en verde, se fusiona.
- **Alternativa considerada:** pruebas funcionales de los plugins de convención con Gradle TestKit. Darían una regresión automática de los escenarios 3 a 5, pero suman varios minutos de CI por pull request y la convención ya se probó así en `pipeline_cicd.md`. Se reconsidera si la convención cambia a menudo.

## 18. Cobertura única del 60 % (FR-012, FR-013)

- **Decisión (D-32, confirmada por el autor el 2026-10-11):** JaCoCo exige al menos 60 % de líneas en cada módulo, también en los servicios centrales. Se elimina `mdd.servicio-central` y su regla `PACKAGE` del 80 %.
- **Efecto en la convención:** `mdd.java-conventions` ya tiene la regla del 60 % del módulo. Solo cambian dos comentarios: el de `mdd.java-conventions` que menciona el 80 % y el del paso de cobertura en `ci.yml`.
- **Lo que no cambia:** el dominio central se sigue desarrollando guiado por pruebas, sin infraestructura ni dobles (principio VIII).
- **Alternativa considerada:** mantener el 80 % en el dominio central, que conservaba RNF-MAN-02 tal como estaba en la ERS. El autor la descartó.
