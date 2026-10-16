---

description: "Tareas de B00-H07: Etapas de secretos y SCA con SBOM por módulo"
---

# Tasks: Etapas de secretos y SCA con SBOM por módulo

**Input**: documentos de diseño en `specs/008-secretos-sca-sbom/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: `probar_excepciones.sh` va antes que `excepciones.sh`. Las reglas de Gitleaks se prueban primero con casos que deben fallar y después sobre `main`, que debe pasar. Cada escenario termina con un pull request de demostración.

**Decisiones**: D-29, D-31 y D-54 en [`docs/decisiones.md`](../../docs/decisiones.md), y el principio IX de la constitución.

**Organization**: el cambio del job `3 · secretos` (*digest* y `--log-opts="--text HEAD"`) es fundacional, porque lo usan US1 y US2. Las historias de usuario son las de la especificación:
- **US1:** secreto en un commit;
- **US2:** certificado fuera de la CA de prueba;
- **US3:** SBOM por módulo;
- **US4:** dependencia vulnerable, con las excepciones de D-54.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1, US2, US3 o US4.
- Las rutas son relativas a la raíz del repositorio.

## Convenciones para todas las tareas

- **Herramientas en contenedor** (Gitleaks, Grype, yq, shellcheck y actionlint): sobre una copia en el *scratchpad* de la sesión, pasada con `docker cp` o montando esa copia. Nunca montar el repositorio. Docker, con `sg docker -c '…'`.
- **Versiones y fijación:** las de research §1, exactas. Las acciones se fijan por SHA con la versión en un comentario; las imágenes, por *digest*.
- **Valores ficticios:** las contraseñas y certificados de las pruebas y de las demostraciones son inventados (principios VIII y IX). Nunca usar el certificado real del autor.
- **Scripts de Bash:** `set -euo pipefail`, comentario de propósito y de uso en español, con el patrón `verificar` de `deploy/probar_errores.sh`. Deben pasar `shellcheck` sin avisos.
- **Nombres de los checks:** no cambian, ni sus condiciones (D-29, D-31).
- **Evidencia:** en `specs/008-secretos-sca-sbom/evidencia.md`. Commits con `Refs #24`.

---

## Phase 1: Setup

- [X] T001 Crear `specs/008-secretos-sca-sbom/evidencia.md` con estas secciones:
  - entorno (versiones de Gitleaks, Grype, pre-commit, yq y Docker);
  - escenario 1 (secreto en un commit);
  - escenario 2 (certificado fuera de la CA de prueba);
  - escenario 3 (SBOM por módulo);
  - escenario 4 (dependencia vulnerable) y excepciones (D-54);
  - alcance del historial (FR-002);
  - notas.
- [X] T002 [P] Preparar las herramientas locales en el *scratchpad*: un clon de la rama con `git clone --no-local`; las imágenes `ghcr.io/gitleaks/gitleaks:v8.30.1`, `anchore/grype:v0.120.1` y `mikefarah/yq:4`; y pre-commit 4.x en un entorno virtual (o el del sistema, si el autor lo instala). Anotar las versiones en el «entorno» de la evidencia.

---

## Phase 2: Foundational (bloquea a US1 y US2)

- [X] T003 En `.github/workflows/ci.yml`, job `secretos` (contracts/secretos.md):
  - la imagen pasa a `ghcr.io/gitleaks/gitleaks:v8.30.1@sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f` (FR-014);
  - el comando agrega `--log-opts="--text HEAD"`, con un comentario que explica las dos partes: `--text` para que Gitleaks vea los binarios (research §3), y `HEAD` para revisar solo `main` y los commits del pull request (FR-002, research §2).

  Nada más cambia: ni el nombre, ni el `fetch-depth: 0`, ni el límite de 5 minutos.
- [X] T004 [P] En `.gitleaksignore`, borrar la entrada de `cd9dd0e…:FirmadorDePrueba.java:generic-api-key:57` y su comentario. Antes, comprobar con `git merge-base --is-ancestor cd9dd0e main` que ese commit no llega desde `main` (research §11). Conservar el comentario del formato (`commit:archivo:regla:línea`) y agregar: «cada entrada lleva encima un comentario que dice por qué no es un secreto» (FR-007).
- [X] T005 [P] En `.pre-commit-config.yaml`, fijar el hook de Gitleaks por SHA: `rev: 83d9cd684c87d95d656c1458ef04895a7f1cbd8e  # frozen: v8.30.1` (FR-005, FR-014). Comprobar en el *scratchpad* que `pre-commit install-hooks` lo instala y que `gitleaks version` responde.

**Checkpoint**: la etapa de secretos revisa binarios y solo el historial del pull request.

---

## Phase 3: User Story 1 - Un secreto en un commit no llega al repositorio (Priority: P1) 🎯 MVP

**Goal**: una contraseña de prueba escrita en un archivo de configuración hace fallar el hook y el check `3 · secretos` (escenario 1; FR-001 a FR-003, FR-005 y FR-006; SC-001 y SC-003).

**Independent Test**: en local, el hook rechaza el commit; en un pull request de demostración, `3 · secretos` falla sin mostrar el valor; sobre `main`, 0 hallazgos.

- [X] T006 [US1] Prueba primero, en un repositorio de prueba del *scratchpad*: preparar estos casos y comprobar que, con el `.gitleaks.toml` actual, ninguno de los cuatro primeros se detecta:
  - un `.yaml` con `spring.datasource.password: clave123` y `mdd.usuarios.clave: Prueba2026`;
  - un `.properties` con `password=hunter2`;
  - un `.env` con `DB_PASSWORD=supersecreta`;
  - referencias que deben pasar: `${DB_CLAVE}`, `${MDD_X:}`, `${{ secrets.X }}` y `cambiar-en-local-postgres`;
  - la línea `docker run --rm -v "$PWD:/repo"` en un `.yml`, que también debe pasar;
  - un secreto agregado y borrado: un commit agrega `password=hunter2` a un `.properties` y el siguiente lo borra.

  Anotar en la evidencia que los cuatro primeros no se detectaban (research §5).
- [X] T007 [US1] Agregar a `.gitleaks.toml` la regla `clave-en-configuracion` de contracts/secretos.md y research §5:
  - `path = '''(?i)(\.(ya?ml|properties|env|conf|cfg|ini|toml)|(^|/)\.env[^/]*)$'''`;
  - `regex = '''(?im)^[ \t]*(?:export[ \t]+)?["']?[a-z0-9_.-]*(?:passw(?:or)?d|pwd|secret|clave|contrase(?:n|ñ)a)[a-z0-9_.-]*["']?[ \t]*[:=][ \t]*["']?([^\s"'#]{4,})'''`;
  - `secretGroup = 1`;
  - `keywords = ["pass", "pwd", "secret", "clave", "contrase"]`;
  - una lista permitida propia con `regexes = ['''^\$\{''', '''^cambiar-en-local-''']`.

  Agregar un comentario en español con lo que detecta y lo que permite. Actualizar el comentario de cabecera del archivo.
- [X] T008 [US1] Repetir los casos de T006 con la regla nueva y `--log-opts="--text HEAD"`: los cuatro primeros se detectan con `clave-en-configuracion`, el valor sale como `REDACTED` y las referencias y la línea de `$PWD` no se reportan. El secreto agregado y borrado sigue haciendo fallar la etapa, y el hallazgo señala el commit que lo agregó. Después, correr la etapa como en la CI (quickstart §2, con `--log-opts="--text HEAD"`) sobre el clon de la rama: `no leaks found` (SC-003). Si aparece un falso positivo, ajustar la regla, no `.gitleaksignore`. Anotar los resultados en el escenario 1 de la evidencia.
- [X] T009 [US1] Comprobar el hook en el repositorio de prueba (quickstart §1): con `pre-commit install`, el commit del `application-demo.yaml` con `mdd.usuarios.clave: Prueba2026` falla, y la salida no muestra el valor. Anotarlo en la evidencia.

**Checkpoint**: una contraseña escrita en la configuración ya no pasa ni en local ni en la CI. El pull request de demostración va en la fase final (T024).

---

## Phase 4: User Story 2 - El certificado real no entra al repositorio (Priority: P1)

**Goal**: un certificado o llave fuera de la carpeta de la CA de prueba se rechaza, también si es binario, y los de la CA de prueba se aceptan (escenario 2; FR-004 y FR-005; SC-002).

**Independent Test**: un `.p12` binario ficticio fuera de la CA de prueba hace fallar el hook y `3 · secretos` con `certificado-o-llave`; los 6 `.p12` de `ca-prueba/` no aparecen.

- [X] T010 [US2] Prueba primero, en el repositorio de prueba: un `.p12` binario ficticio en `pruebas/certificado-autor.p12` (generado con `keytool -genkeypair … -storetype PKCS12 -dname "CN=Ficticio"`), otro en `backend/servicios/firma/src/test/resources/ca-prueba/` y una llave ficticia `pruebas/llave-demo.pem` (`openssl genpkey -algorithm RSA`). Comprobar que con la configuración actual y sin `--text` el de fuera no se detecta (research §3), y que con `--text` la regla `pkcs12-file` reporta también el de `ca-prueba/` (research §4). Anotar las dos comprobaciones en la evidencia.
- [X] T011 [US2] En `.gitleaks.toml`:
  - mover la lista permitida de la CA de prueba a un `[[allowlists]]` global con `targetRules = ["certificado-o-llave", "pkcs12-file"]` y las mismas rutas (`^backend/.+/src/test/resources/ca-prueba/` y `^pruebas/ca-prueba/`);
  - borrar la lista propia de `certificado-o-llave`;
  - agregar a esa regla un comentario que diga que su expresión se repite en `.pre-commit-config.yaml` y que se cambian juntas.
- [X] T012 [US2] En `.pre-commit-config.yaml`, agregar el hook local `certificado-o-llave` de contracts/secretos.md:
  - `repo: local`, `language: fail`;
  - `entry: "Certificado o llave fuera de la CA de prueba; el certificado real no entra al repositorio:"`;
  - `files: '(?i)\.(p12|pfx|pem|key|jks|keystore|crt|cer)$'`;
  - `exclude: '^(backend/.+/src/test/resources/ca-prueba/|pruebas/ca-prueba/)'`.

  Agregar un comentario que explique que el hook de Gitleaks no ve los binarios preparados para el commit (research §6).
- [X] T013 [US2] Repetir T010 con la configuración nueva:
  - en la etapa como en la CI, el `.p12` de fuera falla con `certificado-o-llave` y `pkcs12-file`, la llave `.pem` con `certificado-o-llave` y la regla por defecto `private-key`, y el `.p12` de `ca-prueba/` no aparece;
  - en el hook, `git commit` del `.p12` de fuera y de la llave `.pem` falla, y el de `ca-prueba/` pasa;
  - sobre el clon de la rama: `no leaks found`, con los 6 `.p12` de la CA sin reportar (SC-002).

  Anotarlo en el escenario 2 de la evidencia.

**Checkpoint**: el certificado real no puede entrar por un commit ni por un pull request.

---

## Phase 5: User Story 3 - Un SBOM por módulo (Priority: P1)

**Goal**: un SBOM CycloneDX por cada módulo Java y del frontend, con las dependencias de ejecución, sin vacíos ni repetidos (escenario 3; FR-008 a FR-010; SC-004).

**Independent Test**: en el pull request, los artefactos tienen un SBOM por cada módulo de Gradle con código (hoy 23) y 1 del frontend, ninguno vacío y cada uno con el nombre de su módulo.

- [X] T014 [US3] En `backend/build.gradle`:
  - `cyclonedxDirectBom` corre solo en los subproyectos con el plugin `java`; en la raíz y en `apps`, `servicios` y `plataforma` queda desactivado (por ejemplo, `onlyIf { project.plugins.hasPlugin('java') }` o `enabled` según el plugin);
  - el bloque de `cyclonedxBom` se quita, si ya no hace falta para que el plugin funcione.

  Actualizar el comentario de cabecera: un SBOM por módulo, sin agregado (research §7).
- [X] T015 [US3] Comprobar en local, con `./gradlew cyclonedxDirectBom` en `backend/`:
  - cada directorio de `backend/` con su propio `build.gradle` (sin la raíz, `buildSrc/` ni `plantillas/`; hoy 23) tiene su `build/reports/cyclonedx-direct/bom.json`, y no hay otros (ni en `apps/`, `servicios/`, `plataforma/` ni en la raíz);
  - ninguno tiene `components` vacío;
  - `identidad` sigue incluyendo `tomcat-embed-core` y `jackson-databind` (transitivas) y no incluye `junit`.

  Si el entorno local no puede compilar, hacerlo en el run del pull request (T023). Anotar la lista de módulos con su número de componentes en el escenario 3 de la evidencia.
- [X] T016 [US3] En `.github/workflows/ci.yml`, job `sbom-backend`:
  - el comando pasa a `./gradlew cyclonedxDirectBom`;
  - un paso nuevo arma la lista de módulos esperados (los directorios de `backend/` con su propio `build.gradle`, sin la raíz, `buildSrc/` ni `plantillas/`) y comprueba con `jq` que cada uno tiene su SBOM y que ninguno tiene `components` vacío; si no, falla con un mensaje que dice cuál falta o cuál está vacío. Sin un número fijo: un módulo nuevo entra solo (contracts/sca.md);
  - el artefacto `sbom-backend` sube solo `backend/**/build/reports/cyclonedx-direct/bom.json`.

  El nombre del check y su condición no cambian.

**Checkpoint**: cada módulo tiene su inventario, y la CI falla si falta uno.

---

## Phase 6: User Story 4 - Una dependencia vulnerable bloquea la fusión (Priority: P1)

**Goal**: Grype revisa los 24 SBOM, falla con una vulnerabilidad alta o crítica con versión corregida, y el informe dice qué actualizar. Las excepciones son temporales (escenario 4; FR-011 a FR-014 y FR-016; SC-005, SC-006 y SC-008; D-54).

**Independent Test**: `deploy/sca/probar_excepciones.sh` pasa; en un pull request de demostración con `lodash@4.17.20`, `4 · sca` falla y su resumen dice `mdd-frontend`, `lodash`, 4.17.20 y la versión corregida.

- [ ] T017 [P] [US4] Prueba primero: crear los casos de `deploy/sca/casos/` (contracts/sca.md y data-model.md). Cada uno es un `.grype.yaml` de ejemplo:
  - `vacia.yaml` (`ignore: []`);
  - `vigente.yaml`;
  - `vencida.yaml` (`vence` antes de la fecha de la prueba);
  - `mixta.yaml` (una vigente y una vencida);
  - `sin-motivo.yaml` (`reason` vacío);
  - `sin-vence.yaml`;
  - `mas-de-30-dias.yaml` (`vence` − `desde` = 31 días).

  Crear `deploy/sca/probar_excepciones.sh`. Corre `deploy/sca/excepciones.sh` sobre cada caso con la fecha fija `2026-10-15` y comprueba, con el patrón `verificar`:
  - el código de salida: 0 para los cuatro primeros y 1 para los tres inválidos;
  - el número de entradas en la salida;
  - que la vencida no está en la salida y que sale un `::warning::` con su identificador;
  - que cada inválida sale con un `::error::` con su identificador y el motivo.

  Comprobar que falla mientras `excepciones.sh` no existe.
- [ ] T018 [US4] Crear `deploy/sca/excepciones.sh <entrada> <salida> [<hoy>]` (contracts/sca.md):
  - lee `<entrada>` con `yq`; `<hoy>` es ISO y, por defecto, `date -u +%F`;
  - valida cada entrada de `ignore`: `vulnerability`, `package.name`, `reason` no vacío, `desde` y `vence` presentes, con «`vence` como máximo 30 días después de `desde`». Si alguna es inválida, imprime `::error::` y termina con 1, sin escribir la salida;
  - escribe en `<salida>` las entradas vigentes (`hoy` ≤ `vence`) e imprime `::warning::` por cada vencida;
  - si `yq` no está, falla con un mensaje claro.

  Debe pasar `probar_excepciones.sh` (T017) y `shellcheck`.
- [ ] T019 [P] [US4] Crear `.grype.yaml` en la raíz con `ignore: []` y el comentario del formato de contracts/sca.md (D-54: motivo y vencimiento de 30 días como máximo).
- [ ] T020 [US4] Prueba primero del informe: en el *scratchpad*, con Grype 0.120.1, revisar con `--show-suppressed -o table -o json=<archivo>` (sin `--only-fixed` ni `--fail-on`) un SBOM sintético con `lodash@4.17.20` (research §12), una dependencia con una alta sin arreglo y una excepción vigente para una de las altas de `lodash`. Comprobar primero en ese JSON dónde quedan las suprimidas (se espera `ignoredMatches`, no `matches`). Escribir el filtro de `jq` que, desde el JSON y el `metadata.component.name` del SBOM, arma las filas `| módulo | dependencia | instalada | corregida | vulnerabilidad | severidad |` de tres tablas:
  1. las que bloquean: `High` o `Critical` con `fix.state == "fixed"` en `matches`;
  2. las altas o críticas sin arreglo todavía (informativas);
  3. las suprimidas por una excepción vigente, con su vencimiento.

  El filtro también devuelve si hay que fallar (la tabla 1 no está vacía) y el conteo de medias y bajas. Comprobar que la tabla 1 dice `demo`, `lodash`, `4.17.20`, `4.17.21` o `4.18.0` y la vulnerabilidad, y que la suprimida y la sin arreglo no hacen fallar.
- [ ] T021 [US4] En `.github/workflows/ci.yml`, job `sca` (contracts/sca.md):
  - un checkout disperso de `.grype.yaml` y `deploy/sca/`;
  - `anchore/scan-action/download-grype@8576240a41341d866dbd7ebc72cd9fc0ef023f54 # v7.4.2` con `grype-version: v0.120.1`;
  - un paso que corre `deploy/sca/probar_excepciones.sh` y después `deploy/sca/excepciones.sh .grype.yaml "$RUNNER_TEMP/grype.yaml"`;
  - el bucle por cada SBOM: `grype sbom:<archivo> -c "$RUNNER_TEMP/grype.yaml" --show-suppressed -o table -o json=<archivo>.grype.json`, sin `--only-fixed` ni `--fail-on`, en un grupo del log con el nombre del módulo (`metadata.component.name`). Cualquier código distinto de 0 de Grype marca el job como fallido (FR-013);
  - el filtro de T020 sobre cada JSON decide el fallo (alta o crítica con arreglo, no suprimida) y arma las tres tablas en `$GITHUB_STEP_SUMMARY`, con el número de SBOM revisados (hoy 24) y, si no hay hallazgos que bloquean, una línea que lo diga.

  Comentarios en español. La condición del job y el nombre del check no cambian.
- [ ] T022 [US4] Correr `actionlint` sobre `ci.yml` y `shellcheck` sobre `deploy/sca/*.sh`, sin avisos. Correr `deploy/sca/probar_excepciones.sh` con `yq` (en contenedor o binario en el *scratchpad*). Anotarlo en la evidencia.

**Checkpoint**: una dependencia vulnerable con arreglo bloquea la fusión, y el informe dice qué actualizar.

---

## Phase 7: Polish, demostraciones y cierre

- [ ] T023 Actualizar `specs/001-repo-plantilla-ci/contracts/ci-y-regla-main.md`: en las filas de `3 · secretos`, `4 · sbom-backend` y `4 · sca`, una nota que remita a `specs/008-secretos-sca-sbom/contracts/`. Comprobar con `git status` que no hay archivos temporales. Hacer commit y push, y abrir el pull request de `008-secretos-sca-sbom` hacia `main` **en borrador**, con `Refs #24`. Esperar a que terminen los runs del borrador y recién entonces marcarlo como listo. Comprobar:
  - los 10 checks en verde: `3 · secretos` con `no leaks found` (SC-003) y `4 · sca` sin hallazgos (SC-006);
  - los artefactos: un SBOM por cada módulo de Gradle con código en `sbom-backend` (hoy 23) y 1 en `sbom-frontend` (escenario 3, SC-004). Descargarlos con `gh run download` y anotar la lista en la evidencia;
  - el resumen de `4 · sca`: dice 24 SBOM revisados, sin hallazgos que bloquean, y lista las altas sin arreglo, si las hay;
  - las duraciones de `3 · secretos` (< 5 min) y de `4 · sca` (< 10 min) (SC-009);
  - con `gh api repos/{owner}/{repo}/rules/branches/main`, que los 4 checks siguen exigidos (FR-015, D-31).
- [ ] T024 [US1] Pull request de demostración del escenario 1, **con la aprobación del autor**:
  - rama `demo-h07-secreto` desde `008-secretos-sca-sbom`, con el `application-demo.yaml` del quickstart §1;
  - commit con `--no-verify`, pull request hacia `main` listo para revisión.

  Comprobar que `3 · secretos` falla con `clave-en-configuracion`, que el log muestra `REDACTED` y no el valor, y que la fusión queda bloqueada (`mergeStateStatus` `BLOCKED`). **Mientras está abierto**, volver a correr `3 · secretos` del pull request de la historia (`gh run rerun --job`) y comprobar que sigue en verde (FR-002). Cerrarlo sin fusionar, borrar la rama y anotar los runs en la evidencia.
- [ ] T025 [US2] Pull request de demostración del escenario 2, **con la aprobación del autor**: rama `demo-h07-certificado` con el `.p12` ficticio de T010 en `pruebas/certificado-autor.p12`, con `--no-verify`. Comprobar que `3 · secretos` falla con `certificado-o-llave` y `pkcs12-file`, que los 6 `.p12` de `ca-prueba/` no aparecen en el log y que la fusión queda bloqueada. Cerrarlo sin fusionar, borrar la rama y anotarlo.
- [ ] T026 [US4] Pull request de demostración del escenario 4 y de D-54, **con la aprobación del autor**: rama `demo-h07-vulnerable` con `npm install lodash@4.17.20 --save-exact` en `frontend/`. Comprobar, uno por commit:
  1. `4 · sca` falla, su resumen dice `mdd-frontend`, `lodash`, `4.17.20`, la versión corregida, el identificador y `High` (SC-005), y `gh pr view` muestra `mergeStateStatus` `BLOCKED` (SC-007);
  2. con una excepción vigente en `.grype.yaml` por cada alta de `lodash` (`desde` hoy, `vence` dentro de 30 días), `4 · sca` pasa y el resumen la lista como suprimida;
  3. con `vence` en el pasado, `4 · sca` vuelve a fallar con el aviso de la excepción vencida (SC-008);
  4. con una excepción de 31 días, `4 · sca` falla por excepción inválida.

  Cerrarlo sin fusionar, borrar la rama y anotar los runs en la evidencia.
- [ ] T027 Completar `specs/008-secretos-sca-sbom/evidencia.md` y la «Definición de terminado» del pull request. Fusionarlo con *squash* **con la aprobación del autor**. Después:
  - comprobar que Dependabot cerró el PR #84 (`download-grype` 7.4.0 → 7.4.2) porque `main` ya lo tiene; si no, cerrarlo con un comentario que cite esta historia;
  - abrir el pull request de evidencia con `Closes #24`, como en B00-H06;
  - borrar las ramas fusionadas.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (fase 1)**: T001 y T002 son independientes.
- **Foundational (fase 2)**: T003, T004 y T005 son independientes entre sí; dependen de T002 para comprobarse.
- **US1 (fase 3)**: depende de T003. T006 va antes que T007, y T008 y T009 después.
- **US2 (fase 4)**: depende de T003 y T005. T010 va antes que T011 y T012, y T013 después. T011 toca `.gitleaks.toml`, como T007: hacerlas una después de la otra.
- **US3 (fase 5)**: independiente de las fases 2 a 4. T014, después T015 y T016.
- **US4 (fase 6)**: independiente de las fases 2 a 5, salvo que T021 y T016 tocan `ci.yml`. T017 va antes que T018; T019 es libre; T020 va antes que T021; T022 cierra la fase.
- **Cierre (fase 7)**: T023 depende de todo lo anterior. T024, T025 y T026 dependen de T023 y de la aprobación del autor. T027 depende de T024 a T026.

### Parallel Opportunities

- Fase 1: T001 y T002.
- Fase 2: T004 y T005.
- US3 y US4 en cualquier orden respecto de US1 y US2.
- US4: T017 y T019.
- Las tres demostraciones (T024 a T026) pueden estar abiertas a la vez, porque cada una falla solo por su propia rama (FR-002).
- Como trabajas solo, [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (fase 2 y US1)

1. Fases 1 y 2: el job de secretos con *digest* y `--text HEAD`, `.gitleaksignore` y el hook fijado.
2. Fase 3: la regla `clave-en-configuracion`.
3. **Validar:** los casos de T006 se detectan y `main` queda en `no leaks found`.

### Incremental

1. **US2:** la lista global, el hook de certificados y la comprobación con un `.p12` binario.
2. **US3:** SBOM por módulo y su comprobación en `sbom-backend`.
3. **US4:** excepciones con su prueba, Grype fijado y el resumen.
4. **Cierre:** el pull request de la historia, las tres demostraciones, la evidencia y la fusión.

---

## Notes

- Ninguna tarea agrega herramientas nuevas: Gitleaks, CycloneDX, Grype y pre-commit ya están en el principio IX. yq viene en el runner.
- Si Grype marca una dependencia real en el pull request de la historia, se actualiza en ese mismo pull request y se anota. No se agrega una excepción salvo que la actualización rompa algo (D-54).
- Las ramas de demostración salen de `008-secretos-sca-sbom` para usar la configuración nueva. Sus valores quedan en las referencias `refs/pull/*` de GitHub aunque se borre la rama: por eso son ficticios.
- Hacer commit por tarea o por grupo lógico en `008-secretos-sca-sbom`, con `Refs #24`.
