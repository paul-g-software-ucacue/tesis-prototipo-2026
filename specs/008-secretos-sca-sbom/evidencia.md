# Evidencia: B00-H07

Registro de las comprobaciones de los cuatro escenarios de la historia (issue #24), del alcance del historial (FR-002) y de las excepciones de Grype (D-54). Cada fila dice el comando, la versión y la salida resumida, o enlaza al run de GitHub Actions. Todo con valores ficticios.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | Las herramientas en contenedor leen copias del *scratchpad* pasadas con `docker cp`; nunca se monta el repositorio. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |
| Gitleaks | 8.30.1, `sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f` | La misma imagen que fija la CI. |
| Grype | 0.120.1, `sha256:e4a44ef45d285b829ce6efe2642980329661bd2d18eab5fc539138d4adaebbbe` | La versión que fija `grype-version` en la CI. |
| yq | 4.54.1 | Binario sacado de `mikefarah/yq:4` (`sha256:4b3d9475…`), la misma versión del runner `ubuntu-24.04`. |
| pre-commit | 4.7.0 | En un entorno virtual del *scratchpad*; descarga Go y compila Gitleaks desde el tag. |
| Java | OpenJDK 25.0.4.1 | Para `./gradlew cyclonedxDirectBom` en local. |

## Escenario 1: secreto en un commit

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Casos con la configuración de B00-H01 (T006) | confirmada: no se detectaban | Repositorio de prueba en el *scratchpad*, Gitleaks 8.30.1 con `--log-opts="--text HEAD"`: `spring.datasource.password: clave123` y `mdd.usuarios.clave: Prueba2026` (`.yaml`), `password=hunter2` (`.properties`), `DB_PASSWORD=supersecreta` (`.env`) y un `password=hunter2` agregado en un commit y borrado en el siguiente → `no leaks found`. | 2026-10-15 |
| Regla `clave-en-configuracion` (T007, T008) | confirmada | Mismos casos: 5 hallazgos de `clave-en-configuracion`, con el valor como `REDACTED`. El agregado y borrado señala el commit que lo agregó (`ad0dda7`). No se reportan `${DB_CLAVE}`, `${MDD_X:}`, `"${{ secrets.X }}"`, `cambiar-en-local-postgres` ni `docker run --rm -v "$PWD:/repo"` en un `.yml`. | 2026-10-15 |
| Rama completa sin hallazgos (T008, T013; SC-003) | confirmada | Clon de `008-secretos-sca-sbom` con los cambios de las fases 2 a 4, `--log-opts="--text HEAD"`: `18 commits scanned`, `no leaks found`. Antes de la lista global (T011), la misma revisión daba 6 hallazgos de `pkcs12-file` en `ca-prueba/`. | 2026-10-15 |
| Hook de pre-commit (T005, T009) | confirmada | pre-commit 4.7.0 instala Gitleaks desde `83d9cd6` (tag v8.30.1, compilado: `gitleaks version` dice «version is set by build process»). `git commit` de `application-demo.yaml` con `mdd.usuarios.clave: Prueba2026` → `Detect hardcoded secrets … Failed`, regla `clave-en-configuracion`, código 1; el valor no aparece en la salida. | 2026-10-15 |
| Pull request de demostración (T024) | confirmada | [PR #101](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/pull/101), commit con `mdd.usuarios.clave` ficticio en `application-demo.yaml` (`--no-verify`): `3 · secretos` falla ([run 38845163075](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38845163075)) con `clave-en-configuracion`, `Finding: mdd.usuarios.clave: REDACTED`, `21 commits scanned`, `leaks found: 1`. Fusión `BLOCKED`. | 2026-10-16 |

## Escenario 2: certificado fuera de la CA de prueba

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Casos con la configuración de B00-H01 (T010) | confirmada: el `.p12` binario no se detectaba | `pruebas/certificado-autor.p12` (keytool, PKCS12, `CN=Ficticio`), `pruebas/llave-demo.pem` (openssl genpkey) y `ca-prueba/firmante-9.p12`. Con `--log-opts=HEAD`: solo la `.pem` (`certificado-o-llave` y `private-key`); el `.p12` binario no aparece. Con `--text HEAD`: aparece, pero `pkcs12-file` también reporta el de `ca-prueba/`. | 2026-10-15 |
| Lista global y regla (T011, T013) | confirmada | Con `[[allowlists]]` global para `certificado-o-llave` y `pkcs12-file`: `certificado-autor.p12` → `certificado-o-llave` y `pkcs12-file`; `llave-demo.pem` → `certificado-o-llave` y `private-key`; `firmante-9.p12` de la CA, sin hallazgos. 4 hallazgos, código 1. | 2026-10-15 |
| Hook `certificado-o-llave` (T012, T013) | confirmada | `certificado-autor.p12` → Gitleaks `Passed` (no ve el binario) y `certificado o llave fuera de la CA de prueba … Failed`; `llave-demo.pem` → los dos `Failed`; `ca-prueba/firmante-9.p12` → `Passed`, el hook local no aplica. | 2026-10-15 |
| Pull request de demostración (T025) | confirmada | [PR #102](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/pull/102), `pruebas/certificado-autor.p12` (binario, `CN=Certificado ficticio de demostracion`) y `pruebas/llave-demo.pem`: `3 · secretos` falla ([run 38845165525](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38845165525)) con `certificado-o-llave` y `pkcs12-file` (el `.p12`) y `certificado-o-llave` y `private-key` (la `.pem`); `leaks found: 4`, ninguno de `ca-prueba/`. Fusión `BLOCKED`. | 2026-10-16 |

## Escenario 3: SBOM por módulo

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| SBOM de B00-H01 (research §7) | confirmada: sobraban 5 | Artefactos del run [38727862750](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38727862750) (PR #99): 23 SBOM de módulos, 3 vacíos (`apps`, `servicios`, `plataforma`), el «directo» de la raíz (solo los subproyectos) y el agregado (226 componentes que repiten los de los módulos). | 2026-10-15 |
| Un SBOM por módulo en local (T014, T015) | confirmada | `./gradlew cyclonedxDirectBom` y `deploy/sca/verificar_sbom.sh`: `23 módulos esperados, 23 SBOM encontrados`, de 6 (`arquitectura`) a 118 componentes (`identidad`). `identidad` incluye `tomcat-embed-core` y `jackson-databind` (transitivas) y ningún `junit`. Sin SBOM en la raíz ni en los contenedores. | 2026-10-15 |
| Casos de falla de `verificar_sbom.sh` (T015) | confirmada | Sin el SBOM de `servicios/roles` y con uno vacío en `apps/`: `FALTA  servicios/roles…`, `SOBRA  apps…`, `Fallan 2 comprobaciones de SBOM.`, código 1. | 2026-10-15 |
| En el pull request (T023, SC-004) | confirmada | [PR #100](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/pull/100) listo, [run 38843985237](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38843985237): artefacto `sbom-backend` con 23 SBOM y `sbom-frontend` con 1; los 24 nombres de módulo distintos (`academico` … `validacionocs`, `mdd-frontend`). `4 · sbom-backend` en 2 min 51 s, con «Un SBOM por módulo» en verde. | 2026-10-16 |

## Escenario 4: dependencia vulnerable y excepciones (D-54)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Prueba de las excepciones (T017, T018) | confirmada | Antes de `excepciones.sh`, `deploy/sca/probar_excepciones.sh` daba 8 `FALLA`. Después, 8 `OK`: sin excepciones, vigente, vence hoy (vigente), vencida (se quita con `::warning::`), mixta, sin motivo, sin `vence` y 31 días (`::error::` y código 1, sin escribir la salida). Con el `.grype.yaml` del repositorio: `0 de 0 excepciones vigentes`. | 2026-10-15 |
| Forma del JSON de Grype (T020) | confirmada | Grype 0.120.1 con `--show-suppressed -o table -o json=…` sobre un SBOM con `lodash@4.17.20`, `ip@2.0.1` y una excepción para `GHSA-r5fr-rjxr-66jc`: la suprimida sale en `ignoredMatches` (con `appliedIgnoreRules`, sin `vence`), no en `matches`; `ip` sale con `fix.state: not-fixed`. | 2026-10-15 |
| Resumen (T020) | confirmada | `deploy/sca/resumen.sh` sobre ese JSON: «Bloquean» con `demo · lodash · 4.17.20 · 4.17.21 · GHSA-35jh-r3h4-6jhm · High`; «Sin arreglo» con `ip 2.0.1 · GHSA-2p57-rm9w-gvfp`; «Suprimidas» con `GHSA-r5fr-rjxr-66jc`, vence 2026-11-09; `Medias y bajas: 3`; código 1. Sin hallazgos: «Ninguna…» en las tres tablas y código 0. | 2026-10-15 |
| Todos los SBOM en local (T021, SC-006) | confirmada | Binario `grype` 0.120.1 sacado de su imagen, sobre los 23 SBOM del backend y el del frontend (98 componentes, `npm run sbom` con Node 24.21.0): `Grype: dependencias de 24 módulos`, «Ninguna vulnerabilidad alta o crítica con versión corregida», sin altas sin arreglo, código 0, en 2 minutos. | 2026-10-15 |
| Herramientas (T022) | confirmada | actionlint 1.7.12 sobre `ci.yml` y ShellCheck 0.11.0 sobre `deploy/sca/*.sh`, sin hallazgos (en contenedor, leyendo por la entrada estándar). | 2026-10-15 |
| En el pull request (T023, SC-006) | confirmada | [Run 38843985237](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38843985237): `probar_excepciones.sh` 8 `OK`, `0 de 0 excepciones vigentes`, `Grype: dependencias de 24 módulos`, sin hallazgos que bloquean. `4 · sca` en 2 min 5 s (SC-009). | 2026-10-16 |
| Demostración, paso 1 (T026, SC-005, SC-007) | confirmada | [PR #103](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/pull/103) con `lodash@4.17.20` en `dependencies`, [run 38845169984](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38845169984): `4 · sca` falla; el resumen «Bloquean la fusión» dice `mdd-frontend · lodash · 4.17.20 · 4.17.21 · GHSA-35jh-r3h4-6jhm · High` y `… · 4.18.0 · GHSA-r5fr-rjxr-66jc · High`; medias y bajas: 3. Fusión `BLOCKED`. | 2026-10-16 |
| Demostración, paso 2: excepciones vigentes (T026) | confirmada | Commit `3e79c7d`, dos excepciones `desde: 2026-10-16`, `vence: 2026-11-14`, [run 38846295000](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38846295000): `2 de 2 excepciones vigentes`, `4 · sca` en verde, las dos en «Suprimidas por una excepción vigente» con su vencimiento. PR `CLEAN`. | 2026-10-16 |
| Demostración, paso 3: excepción vencida (T026, SC-008) | confirmada | Commit `38f4937`, `GHSA-35jh-r3h4-6jhm` con `vence: 2026-10-10`, [run 38847182132](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38847182132): `##[warning]La excepción de GHSA-35jh-r3h4-6jhm (lodash) venció el 2026-10-10; Grype vuelve a revisarla (D-54).`, `1 de 2 excepciones vigentes`; `4 · sca` falla con esa vulnerabilidad en «Bloquean» y la otra en «Suprimidas». Fusión `BLOCKED`. | 2026-10-16 |
| Demostración, paso 4: excepción de 31 días (T026) | confirmada | Commit `bce9cee`, `desde: 2026-10-16`, `vence: 2026-11-16`, [run 38847643930](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38847643930): `##[error]Excepción de Grype inválida (GHSA-35jh-r3h4-6jhm): vence 31 días después de desde; el máximo es 30 (D-54).`; `4 · sca` falla antes de Grype. Fusión `BLOCKED`. Los PR #101, #102 y #103 se cerraron sin fusionar y sus ramas se borraron. | 2026-10-16 |

## Alcance del historial (FR-002)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Con las demostraciones abiertas (T024) | confirmada | Con #101, #102 y #103 abiertos (ramas subidas a las 03:22 UTC), se volvió a correr `3 · secretos` del #100 (job 116797350949, 03:35 UTC): `20 commits scanned`, `no leaks found`. Las ramas de las demostraciones no cuentan para el #100. | 2026-10-16 |

## Notas

- **`pipefail` en `4 · sca`:** el primer run del PR listo ([38843985237](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38843985237)) mostró `shell: /usr/bin/bash -e {0}`. Sin `pipefail`, el estado de `resumen.sh | tee` era el de `tee`, y una vulnerabilidad que bloquea no habría hecho fallar el check. Se corrigió con `shell: bash` (commit `9bcc898`), como en `perfil-consolidado.yml`, antes de las demostraciones; el paso 1 del #103 confirma que ahora falla.
- **Paso 2 de T026 y la fecha:** las demostraciones corrieron el 2026-10-16 en UTC (la noche del 15 en Ecuador); `excepciones.sh` usa la fecha UTC del runner.

- **`.gitleaksignore` (T004):** se borró la entrada de `cd9dd0e` (B00-S01): `git merge-base --is-ancestor cd9dd0e main` confirma que ese commit no llega desde `main`, y con `HEAD` ya no se revisa.
- **Fusión (T027):** el PR #100 se fusionó con *squash* como `486a32d` en `main` el 2026-10-16, con la aprobación del autor, después de los 10 checks y `perfil-consolidado` en verde sobre `340da91` ([ci, run 38848022963](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38848022963)). La rama `008-secretos-sca-sbom` se borró. Los commits de las demostraciones (PR #101 a #103) nunca estuvieron en esa rama y no llegan a `main`.
- **PR #84 (T027):** Dependabot no lo había cerrado después de la fusión; se cerró a mano con un comentario que cita el #100, porque `main` ya usa `download-grype` v7.4.2, y se borró su rama.
