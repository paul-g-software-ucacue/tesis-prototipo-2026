# Implementation Plan: Etapas de secretos y SCA con SBOM por módulo

**Branch**: `008-secretos-sca-sbom` | **Date**: 2026-10-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/008-secretos-sca-sbom/spec.md` (issue #24, B00-H07)

## Summary

Los cuatro jobs de B00-H01 ya existen y son obligatorios, pero al ensayarlos aparecieron tres huecos que hacían fallar los escenarios del issue:
- **Escenario 1:** las reglas por defecto de Gitleaks no detectan una contraseña de prueba de poca entropía en un YAML o un `.env` (`clave123`, `Prueba2026`). Se agrega la regla `clave-en-configuracion`, anclada a la clave de la línea, con las referencias `${…}` y los valores `cambiar-en-local-` permitidos. Sobre el historial de `main`: 0 falsos positivos.
- **Escenario 2:** en el modo `git`, Gitleaks salta los archivos binarios, así que un `.p12` real no se rechazaba. La CI pasa a `--log-opts="--text HEAD"`, y la lista permitida de la CA de prueba se vuelve global para `certificado-o-llave` y `pkcs12-file`. En local, un hook `certificado-o-llave` de pre-commit rechaza los certificados por su nombre, porque `git diff --staged` tampoco muestra los binarios.
- **FR-002:** `gitleaks git` revisaba todas las ramas; con `HEAD`, solo `main` y los commits del pull request.

Lo demás completa la historia:
- **SBOM:** solo los 23 módulos Java (12 servicios, 4 procesos, el API Gateway y 6 de `plataforma`) y el del frontend; sin el agregado ni los vacíos, con una comprobación contra los módulos reales de Gradle (sin un número fijo).
- **Grype:** fijado en v0.120.1 con `download-grype` v7.4.2 (lo que propone el PR #84). El fallo se decide desde el JSON (alta o crítica con arreglo), para que el resumen también muestre las altas sin arreglo. Resumen del job con módulo, dependencia, instalada, corregida, vulnerabilidad y severidad.
- **Excepciones con vencimiento (D-54):** en `.grype.yaml`, validadas por `deploy/sca/excepciones.sh`, con su prueba.
- **Cadena de suministro:** la imagen de Gitleaks por *digest* y el hook por SHA.
- **Limpieza:** se borra la entrada obsoleta de `.gitleaksignore`.
- **Demostraciones:** tres pull requests que se cierran sin fusionar (escenarios 1, 2 y 4, y la excepción vencida).

Los detalles, lo comprobado y las alternativas están en [research.md](research.md).

## Technical Context

**Language/Version**: YAML (GitHub Actions, pre-commit, Grype) · TOML (Gitleaks) · Bash · Groovy DSL de Gradle (solo `backend/build.gradle`)

**Primary Dependencies**: Gitleaks 8.30.1 (imagen por *digest* y hook por SHA), Grype 0.120.1 con `anchore/scan-action/download-grype` v7.4.2, plugin CycloneDX de Gradle 3.5.1 y `@cyclonedx/cyclonedx-npm` (sin cambios), pre-commit 4.x, yq 4 (del runner).

**Storage**: N/A. Las excepciones son archivos versionados; los SBOM e informes, artefactos y resúmenes del run.

**Testing**:
- `deploy/sca/probar_excepciones.sh` (casos de D-54) en el job `4 · sca`;
- el hook y la etapa de secretos ensayados sobre un repositorio de prueba y sobre `main` (quickstart §1 y §2);
- tres pull requests de demostración (research §12);
- `actionlint` y `shellcheck` sobre lo que cambia.

**Target Platform**: GitHub Actions (`ubuntu-latest`) y el equipo del autor (Fedora con SELinux, Docker sin montajes del repositorio).

**Project Type**: pipeline de CI del monorepo: `.github/workflows/ci.yml`, configuración en la raíz y scripts en `deploy/sca/`.

**Performance Goals**: `3 · secretos` en menos de 5 minutos y `4 · sca` en menos de 10 (SC-009, los límites que ya tienen). `--text` solo agrega los 6 `.p12` de la CA al diff.

**Constraints**:
- los nombres de los 10 checks y sus condiciones no cambian (D-31, D-29);
- acciones e imágenes fijadas por SHA o *digest* (principio IX);
- nunca mostrar el valor de un secreto (FR-006);
- solo valores y certificados ficticios en las demostraciones (principios VIII y IX).

**Scale/Scope**:
- 2 reglas de Gitleaks (una nueva y una que pierde su lista propia) y 1 lista permitida global;
- 2 hooks de pre-commit;
- 3 jobs que cambian (`secretos`, `sbom-backend` y `sca`);
- 2 scripts nuevos y 1 archivo de excepciones;
- 24 SBOM revisados;
- 3 pull requests de demostración.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I a VII | Sin dominio, servicios, contratos ni datos de negocio. | ✅ N/A |
| VIII. Pruebas | `probar_excepciones.sh` antes de usar `excepciones.sh`; cada escenario se comprueba con un pull request que debe fallar y con `main`, que debe pasar. Datos ficticios. | ✅ |
| IX. Seguridad (SSDLC) | Es su objeto (versión 2.1.2, que admite las excepciones temporales de D-54): Gitleaks en cada commit (pre-commit) y en cada PR, también con binarios; certificados fuera de la CA de prueba rechazados; un SBOM CycloneDX por módulo y por aplicación; Grype sin altas ni críticas con versión corregida; herramientas fijadas por *digest* o SHA. | ✅ |
| X. Simplicidad | Sin herramientas nuevas: se configuran las que fija el principio IX. Se quitan el SBOM agregado, los vacíos y una entrada obsoleta de `.gitleaksignore`. | ✅ |
| XI y XII | Los scripts llevan su comentario de propósito y de uso en español, como los de `deploy/`. | ✅ |
| XIII. Errores | N/A: no hay respuestas de la API. | ✅ N/A |
| §6.4 Decisiones | D-54 (excepciones de Grype con vencimiento), aprobada por el autor en la aclaración del 2026-10-15; enmienda de parche de la constitución a la 2.1.2 (principio IX), aprobada el mismo día. Lo demás es comportamiento propio de esta historia (D-31 se la asignó) y queda en research. | ✅ |
| §8 Definición de terminado | «Gitleaks y Grype en verde en su pull request» pasa a significar también binarios, contraseñas de poca entropía y los 24 SBOM. | ✅ |

**Resultado del gate antes de la fase 0:** pasa. El análisis del 2026-10-15 encontró que D-54 contradecía el texto del principio IX; se resolvió con la enmienda 2.1.2.

**Revisión después de la fase 1:** pasa. [data-model.md](data-model.md) y [contracts/](contracts/) solo agregan lo listado.

## Project Structure

### Documentation (this feature)

```text
specs/008-secretos-sca-sbom/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0: lo comprobado con Gitleaks 8.30.1 y Grype 0.120.1
├── data-model.md            # fase 1: hallazgos, excepciones y SBOM
├── quickstart.md            # fase 1
├── contracts/
│   ├── secretos.md              # .gitleaks.toml, job 3 · secretos y pre-commit
│   └── sca.md                   # SBOM, job 4 · sca, .grype.yaml y deploy/sca/
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
.gitleaks.toml                   # cambia: lista permitida global y regla clave-en-configuracion
.gitleaksignore                  # cambia: sin la entrada obsoleta de B00-S01
.pre-commit-config.yaml          # cambia: gitleaks por SHA y hook local certificado-o-llave
.grype.yaml                      # nuevo: excepciones de Grype (D-54), vacío
deploy/sca/
├── excepciones.sh               # nuevo: valida y filtra .grype.yaml
├── probar_excepciones.sh        # nuevo: casos de D-54
└── casos/                       # nuevo: archivos de ejemplo de la prueba
backend/build.gradle             # cambia: SBOM solo en subproyectos con java; sin el agregado
.github/workflows/ci.yml         # cambia: secretos (digest, --text HEAD), sbom-backend (comprobación),
                                 #         sca (checkout, v7.4.2, grype-version, excepciones y resumen)
specs/001-repo-plantilla-ci/contracts/ci-y-regla-main.md   # cambia: remite a los contratos de B00-H07
```

**Structure Decision**: la configuración de las herramientas queda en la raíz, donde Gitleaks, pre-commit y Grype la buscan por defecto. Los scripts de la CI viven en `deploy/sca/`, junto a los demás scripts del proyecto.

## Fases de implementación (guía para /speckit-tasks)

1. **D-54:** registrada el 2026-10-15 (`docs/decisiones.md`).
2. **Secretos:** `.gitleaks.toml` (lista global y regla nueva), `.gitleaksignore`, el job `secretos` (*digest* y `--text HEAD`) y `.pre-commit-config.yaml`. Comprobación local sobre `main` (0 hallazgos) y sobre los casos del quickstart §1.
3. **SBOM:** `backend/build.gradle` y la comprobación de los 23 en `sbom-backend`.
4. **Excepciones,** con la prueba primero: `probar_excepciones.sh` y sus casos, después `excepciones.sh` y `.grype.yaml`.
5. **Grype:** el job `sca` con v7.4.2, `grype-version`, las excepciones, las dos salidas y el resumen.
6. **Pull request de la historia:** los 10 checks en verde y los 24 SBOM en los artefactos (escenario 3).
7. **Demostraciones:** los tres pull requests de research §12; se cierran sin fusionar y se borran sus ramas.
8. **Cierre:** `evidencia.md`, la fusión con la aprobación del autor, el cierre automático del PR #84 y el PR de evidencia con `Closes #24`.

## Complexity Tracking

| Pieza | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| Hook local `certificado-o-llave`, que repite la expresión de `.gitleaks.toml` | El hook de Gitleaks no ve los binarios preparados para el commit, y el escenario 2 nombra al hook | Confiar solo en la CI dejaría que el certificado real llegara a GitHub antes de que el check falle. |
| `deploy/sca/excepciones.sh` y su prueba | Grype no tiene vencimiento, y D-54 lo exige | Revisar las fechas a mano no garantiza que una excepción vencida vuelva a fallar. |
