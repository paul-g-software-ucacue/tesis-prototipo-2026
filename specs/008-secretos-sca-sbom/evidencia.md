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

## Escenario 2: certificado fuera de la CA de prueba

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Casos con la configuración de B00-H01 (T010) | confirmada: el `.p12` binario no se detectaba | `pruebas/certificado-autor.p12` (keytool, PKCS12, `CN=Ficticio`), `pruebas/llave-demo.pem` (openssl genpkey) y `ca-prueba/firmante-9.p12`. Con `--log-opts=HEAD`: solo la `.pem` (`certificado-o-llave` y `private-key`); el `.p12` binario no aparece. Con `--text HEAD`: aparece, pero `pkcs12-file` también reporta el de `ca-prueba/`. | 2026-10-15 |
| Lista global y regla (T011, T013) | confirmada | Con `[[allowlists]]` global para `certificado-o-llave` y `pkcs12-file`: `certificado-autor.p12` → `certificado-o-llave` y `pkcs12-file`; `llave-demo.pem` → `certificado-o-llave` y `private-key`; `firmante-9.p12` de la CA, sin hallazgos. 4 hallazgos, código 1. | 2026-10-15 |
| Hook `certificado-o-llave` (T012, T013) | confirmada | `certificado-autor.p12` → Gitleaks `Passed` (no ve el binario) y `certificado o llave fuera de la CA de prueba … Failed`; `llave-demo.pem` → los dos `Failed`; `ca-prueba/firmante-9.p12` → `Passed`, el hook local no aplica. | 2026-10-15 |

## Escenario 3: SBOM por módulo

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| SBOM de B00-H01 (research §7) | confirmada: sobraban 5 | Artefactos del run [38727862750](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38727862750) (PR #99): 23 SBOM de módulos, 3 vacíos (`apps`, `servicios`, `plataforma`), el «directo» de la raíz (solo los subproyectos) y el agregado (226 componentes que repiten los de los módulos). | 2026-10-15 |
| Un SBOM por módulo en local (T014, T015) | confirmada | `./gradlew cyclonedxDirectBom` y `deploy/sca/verificar_sbom.sh`: `23 módulos esperados, 23 SBOM encontrados`, de 6 (`arquitectura`) a 118 componentes (`identidad`). `identidad` incluye `tomcat-embed-core` y `jackson-databind` (transitivas) y ningún `junit`. Sin SBOM en la raíz ni en los contenedores. | 2026-10-15 |
| Casos de falla de `verificar_sbom.sh` (T015) | confirmada | Sin el SBOM de `servicios/roles` y con uno vacío en `apps/`: `FALTA  servicios/roles…`, `SOBRA  apps…`, `Fallan 2 comprobaciones de SBOM.`, código 1. | 2026-10-15 |

## Escenario 4: dependencia vulnerable y excepciones (D-54)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Alcance del historial (FR-002)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|

## Notas

- **`.gitleaksignore` (T004):** se borró la entrada de `cd9dd0e` (B00-S01): `git merge-base --is-ancestor cd9dd0e main` confirma que ese commit no llega desde `main`, y con `HEAD` ya no se revisa.
