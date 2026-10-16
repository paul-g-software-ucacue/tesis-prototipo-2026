# Research: Etapas de secretos y SCA con SBOM por módulo (B00-H07)

Decisiones técnicas del plan. Cada sección tiene la decisión, por qué y las alternativas. Todo lo que dice «comprobado» se ensayó el 2026-10-15 con Gitleaks 8.30.1 y Grype 0.120.1 en contenedores, sobre un repositorio de prueba en el *scratchpad* y sobre un clon de `main` (`d87c3d4`, 15 commits), y con los SBOM del run [38727862750](https://github.com/paul-g-software-ucacue/tesis-prototipo-2026/actions/runs/38727862750) (PR #99).

## 1. Versiones

| Herramienta | Versión | Fijada por | Nota |
|---|---|---|---|
| Gitleaks (imagen de la CI) | v8.30.1 | *digest* `sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f` | La última; B00-H01 la dejó solo con la etiqueta (research §9 de B00-H01) |
| Gitleaks (hook de pre-commit) | v8.30.1 | SHA del tag `83d9cd684c87d95d656c1458ef04895a7f1cbd8e` con `# frozen: v8.30.1` | pre-commit lo compila desde ese commit: la misma versión que la CI (FR-005) |
| `anchore/scan-action/download-grype` | v7.4.2 | SHA `8576240a41341d866dbd7ebc72cd9fc0ef023f54` | Sube desde v7.4.0; es lo que propone el PR #84 de Dependabot, que se cierra solo al fusionar esta historia |
| Grype | v0.120.1 | entrada `grype-version` de la acción | Sin esa entrada, la acción instala la versión que trae por defecto (v0.118.0 en v7.4.2) y cambia sin aviso. Con un tag, la acción usa el `install.sh` de ese mismo tag, que verifica el SHA-256 del binario contra los checksums de la release (comprobado en `action.js` y en `install.sh` de v0.120.1) |
| pre-commit (en el equipo del autor) | 4.x | — | 4.7.0 descarga Go si no está instalado; comprobado |
| yq | la del runner (4.54.1 en `ubuntu-24.04`) | — | Para validar las excepciones de Grype (§8); ya viene en la imagen del runner |

## 2. Alcance del historial de la etapa de secretos

- **Comprobado:** `gitleaks git` recorre **todas** las referencias (`git log --all`). Con `main` en un commit y otra rama con un secreto, informó «2 commits scanned» y encontró el secreto de la otra rama. En la CI, `actions/checkout` con `fetch-depth: 0` trae todas las ramas, así que la rama de una demostración abierta hacía fallar los pull requests de las demás.
- **Decisión:** `--log-opts="--text HEAD"`. En un pull request, `HEAD` es el commit de fusión de prueba: llega a todo `main` y a todos los commits de la rama, y a nada más (FR-002). Comprobado: con `HEAD`, el secreto de la otra rama ya no aparece.
- **Alternativas:** `origin/main..HEAD` (solo los commits del PR): deja sin revisar `main`, que es lo que pide el escenario «todo el historial»; `fetch-depth: 0` con `--single-branch`: depende de cómo `actions/checkout` arma las referencias.

## 3. Archivos binarios: el `.p12` real no se detectaba

- **Comprobado:** en el modo `git`, Gitleaks salta los archivos binarios («Binary files differ»), y las reglas que solo miran la ruta, como `certificado-o-llave`, nunca se evalúan para ellos. Un `.p12` binario fuera de la CA de prueba **no** se rechazaba; uno con contenido de texto, sí. Es decir: el escenario 2 fallaba hoy para un certificado real.
- **Decisión:** `--text` en `--log-opts` (Git trata los binarios como texto en el diff). Comprobado:
  - el `.p12` binario de fuera de la CA se rechaza con `certificado-o-llave` y con la regla por defecto `pkcs12-file`;
  - sobre el historial de `main` no aparece ningún hallazgo nuevo por el contenido binario (los 6 `.p12` de la CA de prueba son los únicos binarios sensibles).
- **Alternativas:** una segunda pasada con `gitleaks dir` sobre el árbol final (no ve un `.p12` agregado y borrado dentro del PR); un script propio que liste los archivos agregados (duplica la regla).

## 4. La regla por defecto `pkcs12-file` y la CA de prueba

- **Comprobado:** con `--text`, la regla por defecto `pkcs12-file` también reporta los 6 `.p12` de `ca-prueba/`, porque la lista permitida actual es solo de `certificado-o-llave`.
- **Decisión:** la lista permitida de la CA de prueba pasa a ser global, con `targetRules = ["certificado-o-llave", "pkcs12-file"]` (Gitleaks ≥ 8.25). Las demás reglas siguen revisando esa carpeta. Comprobado: `main` queda con 0 hallazgos y el `.p12` de fuera de la CA sigue rechazado por las dos reglas.
- **Alternativas:** desactivar `pkcs12-file` (perdería una capa); una lista global sin `targetRules` (dejaría de buscar contraseñas en esa carpeta).

## 5. Contraseñas con poca entropía en archivos de configuración

- **Comprobado:** las reglas por defecto **no** detectan `spring.datasource.password: clave123`, `mdd.usuarios.clave: Prueba2026`, `password=hunter2` ni `DB_PASSWORD=supersecreta` en un `.yaml` o un `.env`: la regla genérica exige un valor que parezca aleatorio. El escenario 1 fallaba hoy con una contraseña de prueba típica.
- **Decisión:** una regla propia, `clave-en-configuracion`:
  - **Rutas:** `.yml`, `.yaml`, `.properties`, `.env*`, `.conf`, `.cfg`, `.ini` y `.toml`.
  - **Patrón:** una clave al inicio de la línea (con `export` opcional) que contiene `password`, `passwd`, `pwd`, `secret`, `clave` o `contraseña`/`contrasena`, seguida de `:` o `=` y un valor de 4 caracteres o más en la misma línea. Sin umbral de entropía.
  - **Lista permitida:** valores que empiezan con `${` (variables de entorno, también `${{ secrets.… }}` de GitHub Actions) o con `cambiar-en-local-` (los valores ficticios de `.env.ejemplo`).
- **Comprobado:** detecta los 4 casos de arriba; sobre el historial de `main`, 0 hallazgos. La primera versión, sin anclar la clave al inicio de la línea, tomaba `-v "$PWD:/repo"` de `ci.yml` como una contraseña; anclada, no.
- **Alternativas:** bajar el umbral de entropía de la regla genérica (muchos falsos positivos en código); revisar también el código fuente (`.java`, `.ts`): las pruebas usan contraseñas ficticias a propósito, y el escenario habla de archivos de configuración.

## 6. Hook de pre-commit

- **Comprobado** con pre-commit 4.7.0 en el repositorio de prueba:
  - el hook `gitleaks` (`gitleaks git --pre-commit --staged`) usa `.gitleaks.toml` de la raíz y bloquea el commit con la contraseña en el YAML;
  - **no** ve un `.p12` binario preparado para el commit: `git diff --staged` tampoco muestra los binarios y el hook no admite `--log-opts`.
- **Decisión:** un segundo hook, local y sin dependencias, `certificado-o-llave`, con `language: fail`: rechaza por nombre los archivos con las extensiones de la regla fuera de las carpetas de la CA de prueba. Comprobado: rechaza `certs/otro.p12` y deja pasar `…/ca-prueba/firmante-8.p12`. La expresión es la misma que la de `.gitleaks.toml`; los dos archivos lo dicen en un comentario para cambiarlos juntos. La CI sigue siendo la barrera que no se puede saltar.
- El hook `gitleaks` se fija por SHA (`rev: 83d9cd6… # frozen: v8.30.1`), como sugiere `pre-commit autoupdate --freeze`.
- **Alternativas:** `gitleaks-docker` (pre-commit monta el repositorio con `:Z`, que en el equipo del autor, con SELinux, reetiqueta el repositorio); `gitleaks-system` (depende de la versión que tenga instalada el sistema).

## 7. SBOM por módulo

- **Comprobado** en los artefactos del run 38727862750:
  - `cyclonedxDirectBom` produce un SBOM por subproyecto, con las dependencias de `runtimeClasspath` directas **y transitivas** (por ejemplo, `identidad` lleva `tomcat-embed-core`, `jackson-databind`, `nimbus-jose-jwt` y los servicios que ensambla) y sin librerías de prueba;
  - están los 12 servicios, las 4 aplicaciones, el API Gateway, los 6 módulos de `plataforma` y el frontend;
  - sobran 5 archivos: los SBOM vacíos de `apps`, `servicios` y `plataforma` (proyectos contenedores, sin Java), el SBOM «directo» de la raíz (solo lista los subproyectos) y el agregado `cyclonedxBom`, que repite los 226 componentes y duplica cada hallazgo de Grype.
- **Decisión:**
  - se genera solo el SBOM de los subproyectos con el plugin `java` (24 en total: 12 servicios, 5 aplicaciones, 6 de `plataforma` y 1 del frontend);
  - se deja de generar el agregado y el de la raíz;
  - el artefacto `sbom-backend` conserva la ruta del módulo, que lo identifica (FR-010), y el informe usa el nombre del componente del SBOM (`metadata.component.name`).
- **Alternativas:** mantener el agregado como apoyo (nadie lo consume y duplica el informe); revisar solo las aplicaciones (el principio IX pide un SBOM por módulo).
- La especificación decía 18 SBOM; los 6 de `plataforma` también son módulos y se suman (SC-004 se ajusta a 24).

## 8. Excepciones de Grype con vencimiento (D-54)

- **Comprobado:** Grype lee `.grype.yaml` con reglas `ignore` que tienen `vulnerability`, `package.name` y `reason`, y acepta campos extra (`vence`) sin error. Con `--show-suppressed`, la vulnerabilidad aparece como «(suppressed)» y el check pasa. Grype no tiene vencimiento propio.
- **Decisión:**
  - las excepciones viven en `.grype.yaml`, en la raíz, con `vulnerability`, `package.name`, `reason`, `desde` y `vence` (fechas ISO);
  - `deploy/sca/excepciones.sh` valida el archivo antes de Grype: si a una entrada le falta un campo o tiene más de 30 días entre `desde` y `vence`, el check falla;
  - las entradas vencidas se quitan de la configuración que recibe Grype, con un aviso, así que la vulnerabilidad vuelve a hacer fallar el check (FR-016);
  - `deploy/sca/probar_excepciones.sh` comprueba esos casos con archivos de ejemplo y corre en el mismo job, antes de usarlo.
  - Grype recibe la configuración filtrada con `-c`. **Comprobado:** con `-c`, Grype no carga el `.grype.yaml` del directorio de trabajo, así que una entrada vencida no se cuela por la autodetección.
- **Alternativas:** un archivo propio convertido a `.grype.yaml` (dos formatos para lo mismo); VEX (más pesado para una tesis de un autor).

## 9. Informe de Grype

- **Decisión:** por cada SBOM, Grype escribe la tabla en el log (en un grupo con el nombre del módulo) y un JSON, **sin** `--only-fixed` ni `--fail-on`. Con el JSON y `jq` se decide el fallo (alta o crítica con `fix.state == "fixed"`, no suprimida) y se arma el resumen del job (`$GITHUB_STEP_SUMMARY`), visible en el run sin descargar nada (FR-012), con tres tablas: las que bloquean, las altas o críticas sin arreglo todavía (informativas) y las suprimidas por una excepción vigente, con su vencimiento.
- **Por qué sin `--only-fixed`:** esa opción quita del informe las vulnerabilidades sin arreglo, y la especificación pide que las altas sin arreglo se vean aunque no hagan fallar el check (análisis del 2026-10-15, I1). Grype sigue terminando con error si no puede cargar su base (§10).
- **Comprobado:** Grype acepta varias salidas en una ejecución (`-o table -o json=<archivo>`). La tabla ya trae `NAME`, `INSTALLED`, `FIXED IN`, `VULNERABILITY` y `SEVERITY`, y con `--fail-on high` termina con 2 si hay hallazgos.
- **Alternativas:** subir SARIF a *code scanning* (necesita GitHub Advanced Security en un repositorio privado).

## 10. La base de vulnerabilidades no se puede descargar

- **Comprobado:** con una URL de actualización inalcanzable y sin caché, Grype termina con 1 («failed to load vulnerability db»). El job marca el estado como fallido con cualquier código distinto de 0 de Grype, así que el check falla (FR-013). Grype también rechaza una base vieja por defecto (`db.validate-age`), y no se desactiva.

## 11. `.gitleaksignore`

- Su única entrada (commit `cd9dd0e`, B00-S01) es de la rama `005-spike-firmaec`, que se fusionó con *squash*: ese commit no llega desde `main` (`git merge-base --is-ancestor` lo confirma). Con el alcance de §2 ya no se revisa. **Decisión:** se borra la entrada y se deja el archivo con su comentario de formato, para el próximo falso positivo (FR-007).

## 12. Demostraciones

Como en B00-H01 (#80, #81), los escenarios que tienen que fallar se demuestran con pull requests que se cierran sin fusionar, y sus ramas se borran. Solo con valores ficticios:

| Escenario | Pull request de demostración | Check que falla |
|---|---|---|
| 1 | Agrega `mdd.usuarios.clave: Prueba2026` a un `application-demo.yaml`, con `git commit --no-verify` (antes, el hook lo rechaza en local) | `3 · secretos`, regla `clave-en-configuracion` |
| 2 | Agrega un `.p12` ficticio (generado con `keytool`, con una CA inventada) en `pruebas/certificado-autor.p12` | `3 · secretos`, reglas `certificado-o-llave` y `pkcs12-file`; los 6 `.p12` de la CA de prueba no aparecen |
| 4 | Agrega `lodash@4.17.20` a las `dependencies` del frontend: 2 altas corregidas en 4.17.21 y 4.18.0, sin críticas (comprobado con Grype 0.120.1) | `4 · sca`, con el informe que dice `lodash`, 4.17.20 y la versión corregida |
| D-54 | En el mismo PR del escenario 4: una excepción vigente hace pasar el check; con `vence` en el pasado, vuelve a fallar | `4 · sca` |

El escenario 3 se comprueba en el propio pull request de la historia: los 24 SBOM del artefacto.
