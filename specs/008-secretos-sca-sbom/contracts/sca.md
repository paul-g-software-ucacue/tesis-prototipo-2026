# Contrato: etapa de SCA (SBOM y Grype)

## SBOM del backend (`backend/build.gradle`)

- `cyclonedxDirectBom` corre solo en los subproyectos con el plugin `java` (hoy 23: 12 servicios, 5 aplicaciones —los 4 procesos y el API Gateway— y 6 módulos de `plataforma`), con `includeConfigs = ['runtimeClasspath']` y solo JSON (sin cambios).
- Los proyectos contenedores (`apps`, `servicios`, `plataforma`) y la raíz no generan SBOM.
- `cyclonedxBom` (agregado) deja de usarse.

## Job `4 · sbom-backend`

| Clave | Valor |
|---|---|
| Corre | Si el PR no es borrador (sin cambios) |
| Comando | `./gradlew cyclonedxDirectBom` |
| Comprueba | Que cada módulo esperado tenga su SBOM y que ninguno tenga `components` vacío; si no, falla y dice cuál falta o cuál está vacío. Los módulos esperados son los directorios de `backend/` con su propio `build.gradle`, sin la raíz, `buildSrc/` ni `plantillas/` (hoy 23, todos con `src/main`). Un módulo nuevo entra solo, sin tocar la CI |
| Artefacto | `sbom-backend`: `backend/**/build/reports/cyclonedx-direct/bom.json`, 14 días |

## Job `4 · sbom-frontend`

Sin cambios: `npm run sbom` (`cyclonedx-npm --omit dev`), artefacto `sbom-frontend`.

## Job `4 · sca`

| Paso | Qué hace |
|---|---|
| Checkout disperso | Solo `.grype.yaml` y `deploy/sca/` |
| Descarga | Los artefactos `sbom-*` (sin cambios) |
| Grype | `anchore/scan-action/download-grype@8576240a41341d866dbd7ebc72cd9fc0ef023f54 # v7.4.2`, con `grype-version: v0.120.1` |
| Excepciones | `deploy/sca/probar_excepciones.sh`; después, `deploy/sca/excepciones.sh .grype.yaml <configuración del run>` (ver abajo) |
| Revisión | Por cada SBOM: `grype sbom:<archivo> -c <configuración del run> --show-suppressed -o table -o json=<archivo>.grype.json`, **sin** `--only-fixed` ni `--fail-on`, en un grupo del log con el nombre del módulo. Si Grype termina con un código distinto de 0, el check falla (FR-013) |
| Resumen | En `$GITHUB_STEP_SUMMARY`, con módulo, dependencia, instalada, corregida, vulnerabilidad y severidad, tres tablas: (1) las que bloquean, altas o críticas con arreglo; (2) altas o críticas **sin arreglo todavía**, solo informativas; (3) las suprimidas por una excepción vigente, con su vencimiento. Las medias y bajas quedan en la tabla del log y en un conteo |
| Falla si | Algún hallazgo de `.matches[]` del JSON tiene `severity` `High` o `Critical` y `fix.state == "fixed"` (las suprimidas por una excepción vigente van aparte y no cuentan); una excepción es inválida; Grype termina con error, por ejemplo porque no puede cargar su base |

La condición del job y el nombre del check no cambian (B00-H01, D-31).

## `.grype.yaml`

```yaml
# Excepciones temporales de Grype (D-54). Cada una: motivo y vencimiento de 30 días como máximo.
ignore: []
#  - vulnerability: GHSA-xxxx-xxxx-xxxx
#    package: { name: <dependencia> }
#    reason: "Por qué no se puede actualizar todavía"
#    desde: 2026-10-15
#    vence: 2026-11-14
```

## `deploy/sca/excepciones.sh`

**Uso:** `excepciones.sh <entrada> <salida> [<hoy>]`. Lee `<entrada>` con `yq` y escribe en `<salida>` una configuración de Grype con las excepciones vigentes. `<hoy>` (ISO) es para las pruebas; por defecto, la fecha UTC del día.

| Caso | Salida | Código |
|---|---|---|
| Sin excepciones | `ignore: []` | 0 |
| Todas vigentes | Las mismas entradas | 0 |
| Alguna vencida | Sin la vencida; `::warning::` con su identificador y la fecha | 0 |
| Alguna inválida (falta un campo o pasa de 30 días) | Nada; `::error::` con el identificador y el motivo | 1 |

## `deploy/sca/probar_excepciones.sh`

Corre `excepciones.sh` sobre archivos de ejemplo de cada caso de la tabla, con una fecha fija, y comprueba la salida y el código. Termina con 0 o 1.
