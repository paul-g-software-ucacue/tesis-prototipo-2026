# Quickstart: Etapas de secretos y SCA con SBOM por módulo (B00-H07)

Lo que garantiza cada pieza está en [contracts/secretos.md](contracts/secretos.md) y [contracts/sca.md](contracts/sca.md). Todo usa valores ficticios.

## Requisitos

- `pre-commit` 4.x (`sudo dnf install pre-commit` o `pipx install pre-commit`). La primera vez descarga Go y compila Gitleaks: unos minutos.
- Docker, para correr Gitleaks y Grype como en la CI. En el equipo del autor (SELinux), copiar los archivos con `docker cp` o a una copia en el *scratchpad*; nunca montar el repositorio con `:z`.
- `yq` 4 para `deploy/sca/` en local (en la CI ya viene en el runner).

## 1. Hook de pre-commit (escenarios 1 y 2, en local)

```bash
pre-commit install
pre-commit run --all-files            # sobre el repositorio: todo en verde
```

En una rama de prueba que después se borra:

```bash
printf 'mdd.usuarios.clave: Prueba2026\n' > backend/apps/identidad/src/main/resources/application-demo.yaml
git add backend/apps/identidad/src/main/resources/application-demo.yaml
git commit -m "demo"                  # falla: gitleaks, regla clave-en-configuracion

keytool -genkeypair -alias demo -keyalg RSA -dname "CN=Ficticio" -storetype PKCS12 \
  -keystore pruebas/certificado-autor.p12 -storepass cambiar -validity 1
git add pruebas/certificado-autor.p12
git commit -m "demo"                  # falla: certificado o llave fuera de la CA de prueba
```

Resultado esperado: los dos commits se rechazan y el mensaje no muestra la contraseña. Un `.p12` en `backend/servicios/firma/src/test/resources/ca-prueba/` sí se acepta.

## 2. Etapa de secretos como en la CI

Sobre un clon (`git clone --no-local`) en el *scratchpad*:

```bash
id=$(docker create -w /repo ghcr.io/gitleaks/gitleaks:v8.30.1 \
  git /repo --config /repo/.gitleaks.toml --redact --no-banner --verbose --log-opts="--text HEAD")
docker cp -q <clon>/. "$id:/repo"; docker start -a "$id"; docker rm "$id"
```

Resultado esperado sobre `main`: `no leaks found` (SC-003).

## 3. SBOM (escenario 3)

```bash
cd backend && ./gradlew cyclonedxDirectBom
../deploy/sca/verificar_sbom.sh                                        # un OK por módulo (hoy 23); código 0
cd ../frontend && npm ci && npm run sbom                               # el 24.º
```

En el pull request: el artefacto `sbom-backend` tiene los 23 y `sbom-frontend` el del frontend (SC-004).

## 4. Excepciones de Grype (D-54)

```bash
deploy/sca/probar_excepciones.sh      # todos los casos en OK; código 0
```

## 5. Demostraciones en pull requests (research §12)

Cada una en su rama, como pull request hacia `main`, listo para revisión. Se cierran sin fusionar y se borra la rama.

| PR | Cambio | Resultado esperado |
|---|---|---|
| Escenario 1 | El `application-demo.yaml` de §1, con `git commit --no-verify` | `3 · secretos` falla con `clave-en-configuracion`; el log no muestra el valor; la regla de `main` impide fusionar |
| Escenario 2 | El `.p12` de §1, con `--no-verify` | `3 · secretos` falla con `certificado-o-llave` y `pkcs12-file`; los `.p12` de `ca-prueba/` no aparecen |
| Escenario 4 | `npm install lodash@4.17.20 --save-exact` en `frontend/` | `4 · sca` falla y la fusión queda bloqueada; el resumen del job dice `mdd-frontend`, `lodash`, `4.17.20` y la versión corregida |
| D-54 | En el PR del escenario 4: una excepción vigente para cada alta de `lodash` en `.grype.yaml`; después, una con `vence` en el pasado | Con la vigente, `4 · sca` pasa y el resumen la lista; con la vencida, vuelve a fallar con un aviso |

Mientras una demostración está abierta, los demás pull requests no fallan por ella (FR-002).

## 6. Lo que no debe pasar

- Que un log o un resumen muestre el valor de un secreto.
- Que `3 · secretos` falle en `main` o por una rama que no es la del pull request.
- Que `4 · sca` pase sin haber revisado todos los SBOM (hoy 24), o con una base de vulnerabilidades que no se pudo cargar.
- Que una alta o crítica sin arreglo desaparezca del resumen: se muestra, aunque no haga fallar el check.
