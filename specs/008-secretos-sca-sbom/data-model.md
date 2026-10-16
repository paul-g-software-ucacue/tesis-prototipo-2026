# Data Model: Etapas de secretos y SCA con SBOM por módulo (B00-H07)

La historia no tiene datos de negocio ni persistencia. Sus «datos» son archivos versionados del repositorio y lo que producen los jobs de la CI. Los detalles están en [research.md](research.md) y en [contracts/](contracts/).

## Hallazgo de secreto

Lo que informa Gitleaks, en el log de `3 · secretos` o en la salida del hook.

| Campo | Reglas |
|---|---|
| Regla | `clave-en-configuracion`, `certificado-o-llave`, `pkcs12-file` u otra de las reglas por defecto |
| Archivo y línea | Ruta relativa a la raíz del repositorio |
| Commit | El commit que lo agregó (en el hook: ninguno, son los cambios preparados) |
| Valor | Nunca se muestra: `--redact` lo reemplaza por `REDACTED` (FR-006) |
| Huella | `commit:archivo:regla:línea`; es lo que se copia a `.gitleaksignore` si es un falso positivo |

## Excepción de secreto (`.gitleaksignore`)

Una línea por falso positivo, con la huella exacta, y encima un comentario que dice por qué no es un secreto (FR-007). Sin la entrada, el check sigue fallando. Una entrada cuyo commit ya no llega desde `main` se borra (research §11).

## SBOM

| Campo | Origen | Reglas |
|---|---|---|
| Módulo | `metadata.component.name` (`identidad`, `usuarios`, `errores`, `mdd-frontend`…) | Uno por subproyecto de Gradle con el plugin `java` y uno del frontend: 24 en total (research §7) |
| Ruta en el artefacto | `sbom-backend/<ruta del módulo>/build/reports/cyclonedx-direct/bom.json` y `sbom-frontend/sbom-frontend.json` | Identifica el módulo (FR-010) |
| Formato | CycloneDX 1.6, JSON | — |
| Componentes | `runtimeClasspath` en el backend; `dependencies` sin las de desarrollo en el frontend | Directas y transitivas; nunca las de prueba ni las de compilación (FR-009). Ninguno vacío |

## Excepción de vulnerabilidad (`.grype.yaml`, D-54)

| Campo | Lo lee | Reglas |
|---|---|---|
| `vulnerability` | Grype | Identificador (`GHSA-…` o `CVE-…`); obligatorio |
| `package.name` | Grype | Nombre de la dependencia; obligatorio, para no suprimir la misma vulnerabilidad en otra |
| `reason` | Grype | Por qué no se puede corregir todavía; obligatorio y no vacío |
| `desde` | `excepciones.sh` | Fecha ISO en que se agregó; obligatoria |
| `vence` | `excepciones.sh` | Fecha ISO; obligatoria; como máximo 30 días después de `desde` |

**Estados** (se calculan en cada run con la fecha del día, en UTC):

```text
                ┌─ falta un campo, o vence − desde > 30 días ──► inválida: el check falla
entrada ────────┤
                ├─ hoy ≤ vence ──► vigente: Grype la suprime y el resumen la muestra con su vencimiento
                └─ hoy > vence ──► vencida: se quita antes de Grype, con un aviso; la vulnerabilidad vuelve a fallar
```

## Hallazgo de vulnerabilidad

Una fila del resumen del job `4 · sca` (FR-012).

| Campo | Origen (JSON de Grype) |
|---|---|
| Módulo | `source.target` → el SBOM revisado → su `metadata.component.name` |
| Dependencia | `artifact.name` (y `artifact.purl` en el JSON) |
| Versión instalada | `artifact.version` |
| Versión corregida | `vulnerability.fix.versions` |
| Vulnerabilidad | `vulnerability.id` |
| Severidad | `vulnerability.severity` |

Hace fallar el check si la severidad es `High` o `Critical`, el estado del arreglo es `fixed` y no la cubre una excepción vigente.
