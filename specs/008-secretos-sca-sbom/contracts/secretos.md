# Contrato: etapa de secretos y hook de pre-commit

## `.gitleaks.toml`

| Elemento | Contenido |
|---|---|
| `[extend] useDefault = true` | Se conservan las reglas por defecto de Gitleaks 8.30.1 |
| `[[allowlists]]` global | `targetRules = ["certificado-o-llave", "pkcs12-file"]`, rutas `^backend/.+/src/test/resources/ca-prueba/` y `^pruebas/ca-prueba/` (research §4) |
| Regla `certificado-o-llave` | Ruta `(?i)\.(p12\|pfx\|pem\|key\|jks\|keystore\|crt\|cer)$`, sin patrón de contenido. Sin lista permitida propia (la cubre la global) |
| Regla `clave-en-configuracion` | Rutas, patrón y lista permitida de research §5. `secretGroup = 1`. Sin umbral de entropía |

Un comentario en la regla `certificado-o-llave` dice que su expresión se repite en `.pre-commit-config.yaml` y que se cambian juntas.

## Job `3 · secretos` (`.github/workflows/ci.yml`)

| Clave | Valor |
|---|---|
| Corre | En cada pull request hacia `main`, también en borrador (sin cambios) |
| Checkout | `fetch-depth: 0` (sin cambios) |
| Imagen | `ghcr.io/gitleaks/gitleaks:v8.30.1@sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f` |
| Comando | `git /repo --config /repo/.gitleaks.toml --redact --no-banner --verbose --log-opts="--text HEAD"` |
| Falla si | Hay al menos un hallazgo que no está en `.gitleaksignore` |
| Informe | El log del job: regla, archivo, línea, commit y huella; nunca el valor |
| Límite | 5 minutos (sin cambios) |

El nombre del check no cambia (D-31).

## `.pre-commit-config.yaml`

| Hook | Definición | Qué detecta |
|---|---|---|
| `gitleaks` | `repo: https://github.com/gitleaks/gitleaks`, `rev: 83d9cd684c87d95d656c1458ef04895a7f1cbd8e # frozen: v8.30.1` | Contraseñas y secretos en los cambios de texto preparados, con el mismo `.gitleaks.toml` |
| `certificado-o-llave` | `repo: local`, `language: fail`, `files` con la expresión de la regla, `exclude` con las carpetas de la CA de prueba | Archivos de certificados y llaves por su nombre, también binarios (research §6) |

**Instalación:** una vez por clon, `pre-commit install` (quickstart §1). La primera ejecución compila Gitleaks y puede tardar unos minutos.
