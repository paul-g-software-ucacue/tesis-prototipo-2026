---

description: "Tareas de B00-H01: repositorio, plantilla de servicio en cinco capas y CI con Gradle"
---

# Tasks: Repositorio, plantilla de servicio en cinco capas y CI con Gradle

**Input**: documentos de diseño en `specs/001-repo-plantilla-ci/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII de la constitución exige escribir primero las pruebas, y los escenarios de la especificación son pruebas. Las reglas de arquitectura (US2) se escriben primero como pruebas con *fixtures* que deben fallar. Los escenarios de CI, de cobertura y de SpotBugs (US1, US3 y US4) se demuestran con pull requests, según research §17.

**Decisiones**: D-28 a D-32 y ADR-012, en [`docs/decisiones.md`](../../docs/decisiones.md). La cobertura es un solo umbral, 60 % por módulo, también en el dominio central (D-32, constitución 2.0.0).

**Organization**: las tareas se agrupan por historia de usuario de la especificación: US1 CI y bloqueo de la fusión, US2 plantilla de cinco capas, US3 cobertura y US4 análisis estático.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede correr en paralelo (archivos distintos y sin dependencias pendientes).
- **[Story]**: la historia a la que pertenece la tarea (US1, US2, US3 o US4).
- Las rutas son relativas a la raíz del repositorio. Los comandos `./gradlew` se ejecutan en `backend/`.

## Convenciones para todas las tareas

- Paquete base `ec.mdd`; los servicios usan `ec.mdd.<servicio>` (research §13).
- El JavaDoc va en español; no usar `@author`, y usar `@return` o `@throws` solo cuando aporten (principio XII).
- No agregar dependencias que no estén en [contracts/convenciones-gradle.md](contracts/convenciones-gradle.md) o en esta lista (principio XI).
- Cada comprobación se anota en `specs/001-repo-plantilla-ci/evidencia.md` con el comando, la versión y la salida resumida, o con el enlace al run de CI.

---

## Phase 1: Setup (base de construcción)

**Purpose**: que `./gradlew` funcione en `backend/` con Java 25, Gradle 9.8.1 y las versiones fijadas.

- [X] T001 Agregar a `.gitignore` las entradas `backend/.gradle/`, `**/build/` y `backend/buildSrc/.gradle/`, sin quitar la entrada existente `.claude/settings.local.json`.
- [X] T002 Crear `backend/settings.gradle` con `rootProject.name = 'mdd'` y un comentario que diga que los servicios (`servicios:<servicio>`) y los procesos (`apps:<proceso>`) se incluyen con sus historias. Todavía no lleva `include`.
- [X] T003 Generar el Gradle Wrapper 9.8.1 en `backend/`:
  1. Descargar `https://services.gradle.org/distributions/gradle-9.8.1-bin.zip` y su `.sha256` al directorio temporal de la sesión, y verificar el checksum con `sha256sum -c`.
  2. Desde `backend/`, ejecutar `<dist>/bin/gradle wrapper --gradle-version 9.8.1 --distribution-type bin --gradle-distribution-sha256-sum <sha256>`.
  3. Hacer commit de `backend/gradlew`, `backend/gradlew.bat`, `backend/gradle/wrapper/gradle-wrapper.jar` y `backend/gradle/wrapper/gradle-wrapper.properties`. Este último lleva `distributionSha256Sum`.
- [X] T004 [P] Agregar `allprojects { group = 'ec.mdd' }` en `backend/build.gradle` y conservar la configuración de CycloneDX que ya tiene.
- [X] T005 [P] Agregar `versionSpringModulith=2.1.1` y `versionArchUnit=1.4.2` a `backend/gradle.properties`, sin tocar `versionSpringBoot=4.1.1` ni las opciones de caché.
- [X] T006 [P] En `backend/buildSrc/build.gradle`, leer `versionSpringBoot` desde `../gradle.properties` (`Properties` y `file('../gradle.properties')`) y agregar `implementation "org.springframework.boot:spring-boot-gradle-plugin:${versionSpringBoot}"`. Debe quedar una sola fuente para la versión de Boot.
- [X] T007 Modificar `backend/buildSrc/src/main/groovy/mdd.java-conventions.gradle`:
  - leer `versionSpringModulith` con `providers.gradleProperty`;
  - agregar `implementation platform("org.springframework.modulith:spring-modulith-bom:${versionSpringModulith}")` en `dependencies` y dentro de las suites `test` e `integrationTest`;
  - en `spotless.java`, cambiar `palantirJavaFormat()` por `palantirJavaFormat('2.102.0')`;
  - en el bloque `spotbugs`, agregar `toolVersion = '4.10.4'`;
  - cambiar el comentario `// Piso de cobertura de cada módulo (DS-1). mdd.servicio-central agrega el 80 % del dominio.` por uno que diga que el 60 % de líneas por módulo es el único umbral, también en el dominio central (D-32).

  Las tres versiones tienen un comentario que remite a research §1 a §3.
- [X] T008 Ejecutar `./gradlew --version` (Gradle 9.8.1, launcher JVM 25) y `./gradlew build` en `backend/`. Ambos tienen que terminar sin errores con el proyecto raíz vacío.

**Checkpoint**: el multiproyecto construye en local (FR-001).

---

## Phase 2: Foundational (bloquea a todas las historias)

**Purpose**: un subproyecto real sobre el que corran la CI, las reglas, la cobertura y SpotBugs, y el archivo de evidencia.

**⚠️ CRITICAL**: ninguna historia puede empezar sin esta fase.

- [X] T009 Crear `backend/plataforma/arquitectura/build.gradle` con `plugins { id 'java-library'; id 'mdd.java-conventions' }` y estas dependencias:
  - `api "com.tngtech.archunit:archunit:${versionArchUnit}"`;
  - en la suite `test`: `com.tngtech.archunit:archunit-junit5:${versionArchUnit}`, `org.springframework.modulith:spring-modulith-core`, `org.springframework.modulith:spring-modulith-api` y `jakarta.persistence:jakarta.persistence-api`. Las tres últimas tienen la versión del BOM.

  Agregar `include 'plataforma:arquitectura'` en `backend/settings.gradle`.
- [X] T010 Crear `backend/plataforma/arquitectura/src/main/java/ec/mdd/arquitectura/package-info.java`, con un JavaDoc en español que diga que el paquete contiene las reglas de arquitectura compartidas por todos los servicios (principios I y V).
- [X] T011 [P] Crear `specs/001-repo-plantilla-ci/evidencia.md` con dos tablas:
  - las comprobaciones de la sección 8 de la constitución, con las columnas Comprobación, Resultado (`confirmada` o `fallida`), Evidencia y Fecha, y las filas `palantir-java25`, `spotbugs-java25`, `modulith21-boot41` y `pinact`;
  - los pull requests de demostración, con las columnas PR, Violación, Check en rojo, Mensaje y «Merging is blocked».

  Agregar una sección de notas para las excepciones (research §8 y §14).
- [X] T012 Ejecutar `./gradlew build` en `backend/`. `:plataforma:arquitectura` tiene que compilar y quedar en NO-SOURCE en las tareas sin clases.

**Checkpoint**: fundación lista. US1, US2, US3 y US4 pueden empezar.

---

## Phase 3: User Story 1 - Integración continua que bloquea la fusión (Priority: P1) 🎯 MVP

**Goal**: cada pull request hacia `main` corre las etapas de lint y de pruebas, y no se puede fusionar con un check en rojo (escenario «Integración continua en cada pull request»; FR-007 a FR-011).

**Independent Test**: abrir un pull request hacia `main` y comprobar los 10 checks de [contracts/ci-y-regla-main.md](contracts/ci-y-regla-main.md). Los de frontend tienen que quedar omitidos y `4 · sca` tiene que correr. El ruleset tiene que exigir los 10 checks, la rama al día y solo *squash*.

### Implementation for User Story 1

- [X] T013 [US1] En `.github/workflows/ci.yml`, agregar al principio de `jobs:` el job `deteccion`, con `name: "0 · deteccion"`, `runs-on: ubuntu-latest` y `timeout-minutes: 5`:
  - un paso `actions/checkout` con `sparse-checkout: frontend/package.json` y `sparse-checkout-cone-mode: false`;
  - un paso `id: frontend` que escriba `frontend=true` en `$GITHUB_OUTPUT` si `test -f frontend/package.json` tiene éxito, y `frontend=false` si no;
  - `outputs: { frontend: ${{ steps.frontend.outputs.frontend }} }`.

  Arriba del job va un comentario que explica por qué no se usa `hashFiles()` (research §7).
- [X] T014 [US1] En `.github/workflows/ci.yml`:
  - en `lint-frontend`, agregar `needs: deteccion` y `if: needs.deteccion.outputs.frontend == 'true'`;
  - en `sbom-frontend`, agregar `needs: deteccion` y cambiar su `if` a `github.event.pull_request.draft == false && needs.deteccion.outputs.frontend == 'true'`;
  - `test-frontend` no cambia, porque se omite junto con `lint-frontend`.

  Comentar que ESLint es un criterio de B00-H06 (#23).
- [X] T015 [US1] En el job `sca` de `.github/workflows/ci.yml`, agregar `if: ${{ !cancelled() && needs.sbom-backend.result == 'success' && contains(fromJSON('["success","skipped"]'), needs.sbom-frontend.result) }}` y un comentario sobre el caso límite «repositorio sin frontend» (research §7). En el mismo archivo, en el paso de pruebas de `test-backend`, cambiar el comentario `# jacocoTestCoverageVerification: 60 % por módulo y 80 % en dominio de los servicios centrales.` por `# jacocoTestCoverageVerification: 60 % de líneas por módulo, también en el dominio central (D-32).`
- [X] T016 [US1] Validar `.github/workflows/ci.yml` y `.github/workflows/cierre-ola.yml` con `actionlint`, usando el binario de su release o `podman run --rm -v "$PWD:/repo" -w /repo docker.io/rhysd/actionlint:latest`. No puede haber errores, incluidos los de shellcheck.
- [X] T017 [US1] Instalar `pinact` desde el binario de su release en GitHub y ejecutar `pinact run` en la raíz. Así quedan fijadas por SHA de 40 caracteres, con la etiqueta como comentario, todas las líneas `uses:` de `.github/workflows/ci.yml` y `.github/workflows/cierre-ola.yml`. Después ejecutar `pinact run --check` y registrar la fila `pinact` en `specs/001-repo-plantilla-ci/evidencia.md` (FR-010, SC-004).
- [X] T018 [US1] Comprobar palantir-java-format con Java 25 (FR-016):
  1. Crear un archivo temporal `backend/plataforma/arquitectura/src/test/java/ec/mdd/arquitectura/SintaxisJava25Temporal.java` con `import module java.base;`, un constructor con sentencias antes de `super(...)` y una variable sin nombre `_` en un `catch` o un `for`.
  2. Ejecutar `./gradlew :plataforma:arquitectura:spotlessApply :plataforma:arquitectura:spotlessCheck`.
  3. Borrar el archivo.
  4. Registrar la fila `palantir-java25` en `specs/001-repo-plantilla-ci/evidencia.md`.

  Si falla, no desactivar Spotless: se registra en las notas y se aplica T050.
- [ ] T019 [US1] Hacer commit y push en `001-repo-plantilla-ci`, y abrir el pull request hacia `main` **en borrador** con `gh pr create --draft`, usando `.github/pull_request_template.md`. Lleva `Refs #18` (la historia se cierra en T054), la carpeta `specs/001-repo-plantilla-ci/` y «Contrato OpenAPI cambiado: no».
- [ ] T020 [US1] Comprobar el caso límite «Pull request en borrador»: con el pull request de T019 todavía en borrador, `2 · test-integracion`, `4 · sbom-backend`, `4 · sbom-frontend` y `4 · sca` quedan *skipped*. Marcarlo como listo con `gh pr ready` y, en el run nuevo, comprobar la tabla de [contracts/ci-y-regla-main.md](contracts/ci-y-regla-main.md):
  - `0 · deteccion` en éxito con `frontend=false`;
  - `1 · lint-backend`, `2 · test-backend` y `2 · test-integracion` en éxito;
  - `1 · lint-frontend`, `2 · test-frontend` y `4 · sbom-frontend` en *skipped*;
  - `3 · secretos` y `4 · sbom-backend` en éxito, y `4 · sca` en éxito y **no** omitido.

  Registrar en `specs/001-repo-plantilla-ci/evidencia.md` los enlaces de los dos runs, el del borrador y el de listo para revisión.
- [ ] T021 [US1] Crear `specs/001-repo-plantilla-ci/contracts/ruleset-main.json` con el JSON exacto de [contracts/ci-y-regla-main.md](contracts/ci-y-regla-main.md): 10 checks con `integration_id` 15368, `strict_required_status_checks_policy: true` y `allowed_merge_methods: ["squash"]`. Si en T020 falló `3 · secretos`, `4 · sbom-backend` o `4 · sca` por algo de B00-H07, quitar ese check del JSON y anotarlo en las notas de `evidencia.md` (excepción transitoria del contrato).
- [ ] T022 [US1] Aplicar el ruleset con `gh api -X PUT repos/paul-g-software-ucacue/tesis-prototipo-2026/rulesets/24901935 --input specs/001-repo-plantilla-ci/contracts/ruleset-main.json`. Verificarlo con `gh api ... -q '.rules[] | select(.type=="required_status_checks") | .parameters.required_status_checks[].context'`, que tiene que listar los 10 checks, y con `gh api ... -q '{bypass: .bypass_actors, metodos: (.rules[]|select(.type=="pull_request")|.parameters.allowed_merge_methods)}'`, que tiene que devolver `{"bypass":[],"metodos":["squash"]}`. Comprobar también que sigue `require_extra_approval_for_unattributed_changes: true` (FR-009, D-31).
- [ ] T023 [US1] En el pull request de T019, comprobar que GitHub muestra los checks como *Required* y que solo ofrece *Squash and merge*: `gh pr view --json mergeStateStatus,statusCheckRollup` y la página del pull request. Registrarlo en `specs/001-repo-plantilla-ci/evidencia.md`.

**Checkpoint**: la compuerta existe y el pull request de B00-H01 es el primero sujeto a ella. El escenario 3 se demuestra en T048.

---

## Phase 4: User Story 2 - Plantilla de servicio en cinco capas con límites verificados (Priority: P1)

**Goal**: un servicio creado desde la plantilla tiene las cinco capas y trae consigo las pruebas de arquitectura. Un proceso verifica que sus servicios no usen las clases internas de otro (escenarios 1 y 2; FR-002 a FR-006).

**Independent Test**: `./gradlew :plataforma:arquitectura:test` en verde, con una prueba por cada violación que espera el fallo. Además, copiar el esqueleto a un servicio temporal, poner una importación de Spring en su `dominio` y ver fallar `ArquitecturaTest` ([quickstart.md](quickstart.md) §3).

### Tests for User Story 2 ⚠️

> Escribir primero estas pruebas y comprobar que **fallan**, o que no compilan porque todavía no existe `ReglasCapas`, antes de T030.

- [ ] T024 [P] [US2] Crear la *fixture* correcta en `backend/plataforma/arquitectura/src/test/java/ec/mdd/arquitectura/fixtures/capas/correcto/`. Lleva una clase mínima en cada capa (`dominio/`, `puertos/entrada/`, `puertos/salida/`, `aplicacion/`, `adaptadores/` e `infraestructura/`) y respeta el sentido de la tabla «Capa» de [data-model.md](data-model.md):
  - aplicación usa un puerto y el dominio;
  - un adaptador usa un puerto;
  - infraestructura usa un adaptador.
- [ ] T025 [P] [US2] Crear las *fixtures* de violación en `backend/plataforma/arquitectura/src/test/java/ec/mdd/arquitectura/fixtures/capas/`, cada una en su paquete:
  - `dominioconspring/dominio/`: usa `org.springframework.stereotype.Component`;
  - `dominioconjpa/dominio/`: usa `jakarta.persistence.Entity`;
  - `dominioconhttp/dominio/`: usa `java.net.http.HttpClient`;
  - `dominioconpdf/dominio/`: usa la clase sustituta `org.apache.pdfbox.fixtures.DocumentoPdfSimulado`, creada en `backend/plataforma/arquitectura/src/test/java/org/apache/pdfbox/fixtures/DocumentoPdfSimulado.java` para no agregar la dependencia de PDFBox;
  - `aplicacionconadaptador/`: una clase de `aplicacion` usa una de `adaptadores`;
  - `puertoconinfraestructura/`: una clase de `puertos` usa una de `infraestructura`.
- [ ] T026 [US2] Crear `backend/plataforma/arquitectura/src/test/java/ec/mdd/arquitectura/ReglasCapasTest.java`, que importa cada paquete de *fixtures* con `new ClassFileImporter().importPackages(...)`. Los nombres de los métodos van en estilo Dado–Cuando–Entonces. Comprueba que:
  - la *fixture* correcta pasa `ReglasCapas.SENTIDO_DE_LAS_CAPAS` y `ReglasCapas.DOMINIO_SIN_DETALLES`;
  - cada *fixture* de dominio lanza `AssertionError` con `DOMINIO_SIN_DETALLES`;
  - `aplicacionconadaptador` y `puertoconinfraestructura` lanzan `AssertionError` con `SENTIDO_DE_LAS_CAPAS`;
  - un paquete sin clases pasa las dos reglas (servicio recién creado, research §11).
- [ ] T027 [P] [US2] Crear las *fixtures* de módulos en `backend/plataforma/arquitectura/src/test/java/ec/mdd/arquitectura/fixtures/modulos/`:
  - `correcto/alfa/` con `puertos/entrada/package-info.java` anotado `@org.springframework.modulith.NamedInterface("api")` y una interfaz en ese paquete, más una clase en `alfa/aplicacion/`;
  - `correcto/beta/aplicacion/`, con una clase que solo usa la interfaz de `alfa.puertos.entrada`;
  - `usointerno/alfa/` y `usointerno/beta/`, con la misma estructura, salvo que `beta` usa la clase de `alfa.aplicacion`.
- [ ] T028 [US2] Crear `backend/plataforma/arquitectura/src/test/java/ec/mdd/arquitectura/ModulosFixturesTest.java`. Comprueba que `ApplicationModules.of(...)` sobre `...fixtures.modulos.correcto` pasa `verify()`, y que sobre `...fixtures.modulos.usointerno` lanza `org.springframework.modulith.core.Violations`. Como las *fixtures* son clases de prueba, se importan con la sobrecarga de `ApplicationModules` que acepta una `ImportOption`, con una opción que incluya las clases de prueba. Antes hay que confirmar la firma en `spring-modulith-core` 2.1.1 (research §12).
- [ ] T029 [US2] Ejecutar `./gradlew :plataforma:arquitectura:test` y confirmar que falla, o que no compila, porque falta `ReglasCapas`. Anotarlo en el mensaje del commit de las pruebas.

### Implementation for User Story 2

- [ ] T030 [US2] Crear `backend/plataforma/arquitectura/src/main/java/ec/mdd/arquitectura/ReglasCapas.java`: una clase `final` con constructor privado y dos `public static final ArchRule`, cada una con JavaDoc en español que explica su contrato y cita los principios I y V.
  - `SENTIDO_DE_LAS_CAPAS`: `layeredArchitecture().consideringOnlyDependenciesInLayers()` con las capas `Dominio` (`..dominio..`), `Puertos` (`..puertos..`), `Aplicacion` (`..aplicacion..`), `Adaptadores` (`..adaptadores..`) e `Infraestructura` (`..infraestructura..`), y estas restricciones:
    - Dominio solo lo usan Aplicación, Puertos, Adaptadores e Infraestructura;
    - Puertos solo lo usan Aplicación, Adaptadores e Infraestructura;
    - Aplicación solo la usan Adaptadores e Infraestructura;
    - Adaptadores solo los usa Infraestructura;
    - ninguna capa usa Infraestructura.

    Lleva `withOptionalLayers(true)`.
  - `DOMINIO_SIN_DETALLES`: `noClasses().that().resideInAPackage("..dominio..").should().dependOnClassesThat().resideInAnyPackage(...)` con exactamente estos paquetes:
    - Spring: `org.springframework..`
    - JPA y ORM: `jakarta.persistence..` y `org.hibernate..`
    - HTTP: `jakarta.servlet..`, `java.net.http..`, `org.apache.hc..` y `okhttp3..`
    - PDF: `org.apache.pdfbox..` y `com.openhtmltopdf..`

  Las dos reglas llevan `.allowEmptyShould(true)` y `because(...)` en español. T026 tiene que pasar.
- [ ] T031 [US2] Con T030 hecha, ejecutar `./gradlew :plataforma:arquitectura:build`. Tienen que pasar T026 y T028, `spotlessCheck` y `spotbugsMain`, y `jacocoTestCoverageVerification` con 60 % o más de líneas.
- [ ] T032 [US2] Crear `backend/buildSrc/src/main/groovy/mdd.servicio.gradle`:
  - `plugins { id 'java-library'; id 'mdd.java-conventions' }`;
  - `implementation 'org.springframework.modulith:spring-modulith-api'`;
  - `testImplementation project(':plataforma:arquitectura')`;
  - `testImplementation "com.tngtech.archunit:archunit-junit5:${versionArchUnit}"`, con `versionArchUnit` leído con `providers.gradleProperty`.

  Un comentario de cabecera dice que no agrega Spring Web, JPA ni Flyway ([contracts/convenciones-gradle.md](contracts/convenciones-gradle.md)).
- [ ] T033 [US2] Eliminar `backend/buildSrc/src/main/groovy/mdd.servicio-central.gradle` con `git rm` (D-32, ADR-012). Comprobar con `grep -rn 'servicio-central' backend/ .github/` que no quedan referencias.
- [ ] T034 [US2] Crear `backend/buildSrc/src/main/groovy/mdd.aplicacion.gradle`:
  - `plugins { id 'mdd.java-conventions'; id 'org.springframework.boot' }`;
  - `testImplementation 'org.springframework.modulith:spring-modulith-starter-test'`;
  - excluir `**/*Aplicacion.class` del `classDirectories` de `jacocoTestReport` y de `jacocoTestCoverageVerification` (research §14).
- [ ] T035 [P] [US2] Crear el esqueleto de servicio en `backend/plantillas/servicio/` según [contracts/plantilla-servicio.md](contracts/plantilla-servicio.md):
  - `build.gradle` con `plugins { id 'mdd.servicio' }`, que vale para todos los servicios, centrales o no (D-32);
  - un `src/main/java/ec/mdd/servicio/<capa>/package-info.java` en cada capa (`dominio`, `aplicacion`, `puertos/entrada`, `puertos/salida`, `adaptadores` e `infraestructura`), con el JavaDoc en español de lo que contiene la capa y lo que NO DEBE hacer, según la tabla del principio I. El de `puertos/entrada` lleva `@org.springframework.modulith.NamedInterface("api")` y dice que es la API publicada dentro del proceso;
  - `src/test/java/ec/mdd/servicio/ArquitecturaTest.java`, con `@AnalyzeClasses(packagesOf = ArquitecturaTest.class, importOptions = ImportOption.DoNotIncludeTests.class)` y dos campos `@ArchTest`: `ReglasCapas.SENTIDO_DE_LAS_CAPAS` y `ReglasCapas.DOMINIO_SIN_DETALLES`.
- [ ] T036 [P] [US2] Crear el esqueleto de proceso en `backend/plantillas/aplicacion/`:
  - `build.gradle` con `plugins { id 'mdd.aplicacion' }` y un comentario sobre cómo agregar `implementation project(':servicios:<servicio>')`;
  - `src/main/java/ec/mdd/ProcesoAplicacion.java` con `@SpringBootApplication` y `main`, más JavaDoc: la clase está en `ec.mdd` para que los módulos sean los `ec.mdd.<servicio>`;
  - `src/test/java/ec/mdd/ModulosTest.java`, que ejecuta `ApplicationModules.of(ProcesoAplicacion.class).verify()`.
- [ ] T037 [P] [US2] Crear `backend/plantillas/README.md` con los pasos «Crear un servicio» y «Crear un proceso» de [contracts/plantilla-servicio.md](contracts/plantilla-servicio.md), con un enlace a ese contrato en lugar de copiar sus garantías.
- [ ] T038 [US2] Validar los esqueletos en local, sin commit ([quickstart.md](quickstart.md) §3):
  1. Copiar `plantillas/servicio` a `backend/servicios/prueba` y `plantillas/aplicacion` a `backend/apps/prueba`, renombrando el paquete y la clase.
  2. Incluirlos en `settings.gradle` y declarar el servicio como dependencia del proceso.
  3. `./gradlew build` tiene que pasar.
  4. Poner `org.springframework.stereotype.Component` en una clase de `servicios/prueba/.../dominio/`: `ArquitecturaTest` tiene que fallar.
  5. Revertir con `git checkout -- backend/settings.gradle` y borrar `backend/servicios/` y `backend/apps/`.
- [ ] T039 [US2] Comprobar Modulith 2.1.1 con Boot 4.1.1 (FR-017): `./gradlew :plataforma:arquitectura:dependencies --configuration testRuntimeClasspath` tiene que resolver Spring Boot 4.1.1, Spring Modulith 2.1.1 y ArchUnit 1.4.2 sin conflictos, y T028 tiene que pasar. Registrar la fila `modulith21-boot41` en `specs/001-repo-plantilla-ci/evidencia.md`.

**Checkpoint**: las reglas están probadas y los esqueletos funcionan. Los escenarios 1 y 2 se demuestran en T046 y T047.

---

## Phase 5: User Story 3 - Cobertura mínima en cada pull request (Priority: P2)

**Goal**: `jacocoTestCoverageVerification` rechaza un módulo con menos de 60 % de líneas cubiertas, también en los servicios centrales (escenario «Cobertura mínima»; FR-012 y FR-013; D-32).

**Independent Test**: en servicios temporales creados desde el esqueleto, con 59 % la verificación falla y con exactamente 60 % pasa. Se demuestra en un pull request en T051.

### Implementation for User Story 3

- [ ] T040 [US3] Comprobar en local que un servicio recién creado **sin clases** pasa `./gradlew :servicios:prueba:jacocoTestCoverageVerification`, usando una copia temporal de `plantillas/servicio` en `backend/servicios/prueba` (research §14). Si falla porque hay 0 de 0 líneas, ajustar `backend/buildSrc/src/main/groovy/mdd.java-conventions.gradle` para que la verificación no falle sin clases y anotarlo en `specs/001-repo-plantilla-ci/evidencia.md`. Después borrar la copia.
- [ ] T041 [US3] Comprobar en local el umbral exacto:
  1. Crear una copia temporal de `plantillas/servicio` en `backend/servicios/pruebaumbral`.
  2. Agregar clases y pruebas de modo que el módulo quede con exactamente 60 % de líneas cubiertas: `jacocoTestCoverageVerification` tiene que pasar (caso límite «umbral exacto»).
  3. Agregar una línea sin cubrir para que quede por debajo del 60 %: la verificación tiene que fallar.
  4. Comprobar que no hay ninguna regla `PACKAGE` del 80 % en el informe de la verificación (D-32).

  Anotar los resultados en `specs/001-repo-plantilla-ci/evidencia.md` y borrar la copia.
- [ ] T042 [US3] Comprobar en local que un proceso temporal creado desde `plantillas/aplicacion`, en `backend/apps/prueba`, pasa `jacocoTestCoverageVerification` aunque su única clase propia sea `ProcesoAplicacion`, porque la clase de arranque está excluida (T034). Anotarlo en `specs/001-repo-plantilla-ci/evidencia.md` y borrar la copia.

**Checkpoint**: el umbral del 60 % está comprobado en local, incluido el valor exacto.

---

## Phase 6: User Story 4 - Análisis estático de seguridad en el lint (Priority: P2)

**Goal**: la etapa de lint detecta con confianza alta una consulta SQL armada concatenando un parámetro de entrada, y su check falla (escenario «Hallazgo de análisis estático»; FR-014).

**Independent Test**: una clase de `main` que concatena un parámetro en una consulta SQL hace fallar `spotbugsMain`. Se demuestra en un pull request en T050.

### Implementation for User Story 4

- [ ] T043 [US4] Comprobar SpotBugs con clases de Java 25 (FR-016): `./gradlew :plataforma:arquitectura:spotbugsMain --info` tiene que analizar las clases compiladas con Java 25 (versión 69) sin `Unsupported class file major version`, con SpotBugs 4.10.4 según el log. Registrar la fila `spotbugs-java25` en `specs/001-repo-plantilla-ci/evidencia.md`.

  Comprobar también el caso límite «Hallazgo de confianza media o baja»:
  1. Crear una clase temporal en `backend/plataforma/arquitectura/src/main/java/ec/mdd/arquitectura/` con un patrón que SpotBugs reporte con confianza media.
  2. Confirmarlo bajando a `MEDIUM`, solo en local, el `reportLevel` de `mdd.java-conventions.gradle`: el hallazgo tiene que aparecer.
  3. Volver a `HIGH` y comprobar que `spotbugsMain` pasa.
  4. Borrar la clase, revisar con `git diff` que la convención quedó como estaba y anotar el resultado.
- [ ] T044 [US4] Comprobar el hallazgo en local:
  1. Crear un archivo temporal `backend/plataforma/arquitectura/src/main/java/ec/mdd/arquitectura/ConsultaTemporal.java` con un método que reciba un `String` y ejecute `java.sql.Statement.execute("SELECT * FROM t WHERE c = '" + parametro + "'")`.
  2. `./gradlew :plataforma:arquitectura:spotbugsMain` tiene que fallar con `SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE` o `SQL_INJECTION_JDBC` de confianza alta.
  3. Borrar el archivo.

  Anotar el patrón en `specs/001-repo-plantilla-ci/evidencia.md`.

**Checkpoint**: SpotBugs con FindSecBugs funciona con Java 25 y detecta el caso del escenario.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: dejar el pull request de B00-H01 listo para fusionar.

- [ ] T045 Revisar el JavaDoc según el principio XII en `backend/plataforma/arquitectura/src/main/java/ec/mdd/arquitectura/ReglasCapas.java`, `backend/plataforma/arquitectura/src/main/java/ec/mdd/arquitectura/package-info.java` y los `package-info.java` y clases de `backend/plantillas/`. Tiene que estar en español, documentar el contrato y no repetir la firma; no usa `@author`. Luego ejecutar `./gradlew build` y hacer push a `001-repo-plantilla-ci`. Los 10 checks del pull request tienen que quedar en éxito o *skipped* según T020.

---

## Phase 8: Demostración de los escenarios y cierre (SC-001, SC-002)

**Purpose**: demostrar en GitHub que cada escenario bloquea la fusión y cerrar la historia. Requiere que la regla esté activa (T022). Empieza fusionando B00-H01 (T046), porque los pull requests de demostración salen de `main`.

- [ ] T046 Fusionar con *squash* el pull request de B00-H01, cuando todos sus checks estén en éxito o *skipped* y con la aprobación del autor (SC-002). Comprobar con `gh pr view --json mergeCommit` que `main` tiene un solo commit nuevo.
- [ ] T047 Desde `main`, crear la rama `demo/b00-h01` con:
  - `backend/servicios/demoalfa` y `backend/servicios/demobeta`, copiados de `plantillas/servicio`. `demoalfa` tiene una interfaz en `puertos/entrada` y una clase en `aplicacion`, cada una con su prueba;
  - `backend/apps/demo`, copiado de `plantillas/aplicacion`, que depende de los dos servicios;
  - los `include` correspondientes en `backend/settings.gradle`.

  Abrirla como pull request **en borrador**, confirmar que sus checks están en verde y **no fusionarla**.
- [ ] T048 [P] [US2] Desde la rama `demo/b00-h01-dominio`, que sale de `demo/b00-h01`, abrir un pull request hacia `main` en el que una clase de `demoalfa/.../dominio/` importa `org.springframework.stereotype.Component`. Registrar en `specs/001-repo-plantilla-ci/evidencia.md` el check `2 · test-backend` en rojo (`ArquitecturaTest`) y «Merging is blocked» (escenario 1).
- [ ] T049 [P] [US2] Desde la rama `demo/b00-h01-interno`, que sale de `demo/b00-h01`, abrir un pull request hacia `main` en el que `demobeta` usa la clase de `demoalfa.aplicacion`. Registrar en `specs/001-repo-plantilla-ci/evidencia.md` el check `2 · test-backend` en rojo (`ModulosTest`, `Violations`) y «Merging is blocked» (escenario 2).
- [ ] T050 [P] [US1] Desde la rama `demo/b00-h01-prueba`, que sale de `demo/b00-h01`, abrir un pull request hacia `main` con una prueba unitaria que falla en `demoalfa`. Registrar en `specs/001-repo-plantilla-ci/evidencia.md` el check `2 · test-backend` en rojo y «Merging is blocked» (escenario 3).
- [ ] T051 [P] [US3] Desde la rama `demo/b00-h01-cobertura`, que sale de `demo/b00-h01`, abrir un pull request hacia `main` que agrega a `demobeta` una clase sin pruebas, para que el módulo quede bajo el 60 % de líneas. Registrar en `specs/001-repo-plantilla-ci/evidencia.md` el check `2 · test-backend` en rojo (`jacocoTestCoverageVerification`) y «Merging is blocked» (escenario 4).
- [ ] T052 [P] [US4] Desde la rama `demo/b00-h01-sql`, que sale de `demo/b00-h01`, abrir un pull request hacia `main` que agrega en `demobeta/.../adaptadores/` una clase que concatena un parámetro en una consulta SQL ejecutada con `java.sql.Statement`. Registrar en `specs/001-repo-plantilla-ci/evidencia.md` el check `1 · lint-backend` en rojo, el patrón de FindSecBugs y «Merging is blocked» (escenario 5).
- [ ] T053 [P] [US1] Desde la rama `demo/b00-h01-frontend`, que sale de `demo/b00-h01`, abrir un pull request hacia `main` que solo agrega un `frontend/package.json` mínimo (`{"name":"mdd-frontend","private":true}`). Comprobar que `0 · deteccion` da `frontend=true` y que `1 · lint-frontend` y `4 · sbom-frontend` **corren**, en lugar de quedar *skipped*. Fallarán por falta de `package-lock.json`, y eso demuestra que se ejecutan. Registrarlo en `specs/001-repo-plantilla-ci/evidencia.md` (caso límite «Llega el frontend», FR-008, D-29).
- [ ] T054 Cerrar la historia:
  1. Cerrar sin fusionar los 6 pull requests de demostración y el borrador de `demo/b00-h01`, y borrar sus 7 ramas en el remoto (`gh pr close --delete-branch`).
  2. Completar `specs/001-repo-plantilla-ci/evidencia.md`.
  3. Si alguna comprobación de la sección 8 quedó `fallida`, registrar una decisión nueva en `docs/decisiones.md`, con el siguiente número libre, y enmendar la sección 8 de `.specify/memory/constitution.md` según su sección 7.
  4. Hacer commit de la evidencia en una rama `001-repo-plantilla-ci-evidencia` y fusionarla por pull request.
  5. Cerrar el issue #18 con un comentario que enlace la evidencia y mover la tarjeta a «Hecho» en el tablero.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (fase 1)**: sin dependencias. T002 va antes de T003, porque el wrapper necesita `settings.gradle`, y T008 al final.
- **Foundational (fase 2)**: depende de la fase 1 y bloquea todas las historias.
- **US1 (fase 3)**: depende de la fase 2. T013, T014 y T015 son secuenciales (mismo archivo). T017 va después de T013 a T016, y T021 a T023 después de T020.
- **US2 (fase 4)**: depende de la fase 2. Las pruebas (T024 a T029) van antes de T030. T032 a T034 van antes de T035 a T038.
- **US3 (fase 5)**: depende de T032 a T035 (US2), porque usa los esqueletos y los plugins.
- **US4 (fase 6)**: depende solo de la fase 2.
- **Polish (fase 7)**: depende de US1 a US4.
- **Demostración y cierre (fase 8)**: depende de la fase 7 y de T022 (regla activa). Orden: T046 (fusión de B00-H01) → T047 (rama base) → T048 a T053 (en cualquier orden) → T054.

### User Story Dependencies

- **US1 (P1)**: independiente. Sus demostraciones (T050 y T053) usan la rama base de T047.
- **US2 (P1)**: independiente de US1 para implementarse. Sus demostraciones (T048 y T049) necesitan la regla de US1.
- **US3 (P2)**: necesita los plugins y los esqueletos de US2. Su demostración es T051.
- **US4 (P2)**: independiente. Su demostración (T052) necesita la regla de US1 y la rama base de T047.

### Within Each User Story

- Las pruebas de US2 se escriben primero y tienen que fallar (T029) antes de T030.
- Primero los plugins, después los esqueletos y después la validación local.
- Cada comprobación queda en `evidencia.md` antes de pasar a la siguiente fase.

### Parallel Opportunities

- Fase 1: T004, T005 y T006 (archivos distintos).
- Fase 2: T011 en paralelo con T009 y T010.
- Con la fase 2 terminada, US1 (CI), US2 (reglas) y US4 (SpotBugs) pueden avanzar en paralelo, porque tocan archivos distintos. US3 espera a T035.
- US2: T024, T025 y T027 (*fixtures*) en paralelo; T035, T036 y T037 (esqueletos) en paralelo.
- Fase 8: T048 a T053 en cualquier orden cuando T047 está hecha.

---

## Parallel Example: User Story 2

```bash
# Fixtures de US2 en paralelo (archivos distintos):
Task: "Fixture correcta en backend/plataforma/arquitectura/src/test/java/ec/mdd/arquitectura/fixtures/capas/correcto/"
Task: "Fixtures de violación en backend/plataforma/arquitectura/src/test/java/ec/mdd/arquitectura/fixtures/capas/"
Task: "Fixtures de módulos en backend/plataforma/arquitectura/src/test/java/ec/mdd/arquitectura/fixtures/modulos/"

# Esqueletos de US2 en paralelo, después de T032 a T034:
Task: "Esqueleto de servicio en backend/plantillas/servicio/"
Task: "Esqueleto de proceso en backend/plantillas/aplicacion/"
Task: "Instrucciones en backend/plantillas/README.md"
```

## Parallel Example: Demostración

```bash
# Después de T047, cada pull request desde su rama de demo/b00-h01:
Task: "PR con el dominio importando Spring (escenario 1)"
Task: "PR de uso interno entre servicios (escenario 2)"
Task: "PR con una prueba en rojo (escenario 3)"
Task: "PR con cobertura bajo el 60 % (escenario 4)"
Task: "PR con SQL concatenado (escenario 5)"
Task: "PR que agrega frontend/package.json (caso límite «Llega el frontend»)"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Fase 1 y fase 2: el multiproyecto construye y existe `plataforma:arquitectura`.
2. Fase 3 (US1): la CI corre con los jobs de frontend condicionados, las acciones están fijadas por SHA, el pull request está abierto y la regla está activa.
3. **Parar y validar**: los 10 checks del pull request coinciden con el contrato y el pull request queda sujeto a la regla.

### Incremental Delivery

1. Con US1 hecha, ninguna historia posterior entra a `main` en rojo.
2. US2: las reglas de capas y de módulos se prueban con *fixtures* y los esqueletos quedan listos para F04-H01 y B00-H02.
3. US3 y US4: el umbral del 60 % y SpotBugs con Java 25 quedan comprobados en local.
4. Fases 7 y 8: B00-H01 se fusiona, los cinco escenarios se demuestran en GitHub y la historia se cierra.

### Una sola persona (D-22, límite de 1 en «Implementar»)

Las tareas [P] indican qué se puede hacer en cualquier orden, no trabajo simultáneo. El orden sugerido es: fase 1 → fase 2 → US1 hasta T020 → US2 → US3 → US4 → T021 a T023 → fase 7 → fase 8. Así, la regla se activa cuando el pull request ya tiene todo el código y sus checks están en verde.

---

## Notes

- Las decisiones de esta historia (D-28 a D-32 y ADR-012) están en `docs/decisiones.md`, que prevalece sobre los documentos externos. Esos documentos quedan desactualizados: la ERS (RNF-MAN-02 y D-27), la matriz SCD, ADR-008, ADR-011 y `pipeline_cicd.md`.
- Los nombres de los checks (`0 · deteccion`…) son parte del contrato del ruleset; no se cambian sin actualizar `contracts/ruleset-main.json`.
- Los archivos temporales de T018, T038, T040 a T042 y T044 nunca entran en un commit. Antes de cada commit hay que comprobar `git status`.
- Hacer commit después de cada tarea o grupo lógico en `001-repo-plantilla-ci`, con `Refs #18`.
