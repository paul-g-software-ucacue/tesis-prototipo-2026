# Quickstart: validar B00-H01

Esta guía sirve para comprobar que la historia cumple sus escenarios. Las garantías que se verifican están en [contracts/](contracts/) y las decisiones en [research.md](research.md). Los resultados se registran en `evidencia.md`.

## Requisitos previos

- JDK 25 en el `PATH` (`java -version` → 25). Gradle no hace falta: se usa `./gradlew`.
- `gh` autenticado con acceso de administración a `paul-g-software-ucacue/tesis-prototipo-2026`.
- `pinact`, el binario de su release en GitHub.
- Solo para `integrationTest` en local, porque la máquina usa Podman y no Docker:

  ```bash
  systemctl --user enable --now podman.socket
  export DOCKER_HOST="unix://$XDG_RUNTIME_DIR/podman/podman.sock"
  export TESTCONTAINERS_RYUK_DISABLED=true
  ```

## 1. La base construye (local)

```bash
cd backend
./gradlew --version          # Gradle 9.8.1, JVM 25
./gradlew build              # check: pruebas, jacocoTestCoverageVerification, spotlessCheck y spotbugsMain
./gradlew spotlessCheck spotbugsMain
./gradlew integrationTest    # NO-SOURCE en esta historia
```

**Resultado esperado:** todo en verde. `plataforma:arquitectura` tiene una cobertura de líneas de al menos 60 %, según el informe en `plataforma/arquitectura/build/reports/jacoco/`.

## 2. Comprobaciones de la sección 8 de la constitución

| Comprobación | Cómo | Resultado esperado |
|---|---|---|
| palantir con Java 25 | Crear un archivo temporal en `plataforma/arquitectura/src/test/java/` con `import module java.base;`, un constructor con sentencias antes de `super(...)` y una variable sin nombre `_`. Correr `./gradlew spotlessApply spotlessCheck` y después borrar el archivo. | Spotless formatea y verifica sin excepción |
| SpotBugs con Java 25 | `./gradlew :plataforma:arquitectura:spotbugsMain --info` | Analiza las clases (versión 69) sin `Unsupported class file major version` |
| Modulith 2.1 con Boot 4.1 | `./gradlew :plataforma:arquitectura:dependencies --configuration testRuntimeClasspath` y la prueba de `verify()` con *fixtures* | Spring Boot resuelto en 4.1.1 y Modulith en 2.1.1, sin conflictos; la prueba pasa |
| Acciones por SHA | `pinact run --check` en la raíz | Ningún hallazgo; todas las líneas `uses:` tienen un SHA de 40 caracteres |

Registra cada resultado en `evidencia.md`, con la versión y la salida resumida.

## 3. Los esqueletos funcionan (local, sin commit)

1. Copia `backend/plantillas/servicio/` a `backend/servicios/prueba/` según [contracts/plantilla-servicio.md](contracts/plantilla-servicio.md) e inclúyelo en `settings.gradle`.
2. `./gradlew :servicios:prueba:build` → en verde, aunque el servicio no tenga clases.
3. Agrega en `servicios/prueba/.../dominio/` una clase que importe `org.springframework.stereotype.Component` → `./gradlew :servicios:prueba:test` falla en `ArquitecturaTest` (escenario 1).
4. Revierte con `git checkout -- backend/settings.gradle` y borra `backend/servicios/prueba`.

## 4. CI en el pull request de B00-H01

Abre el pull request de `001-repo-plantilla-ci` hacia `main` y comprueba:

| Check | Esperado |
|---|---|
| `0 · deteccion` | Éxito, con la salida `frontend=false` |
| `1 · lint-backend`, `2 · test-backend` y `2 · test-integracion` | Éxito |
| `1 · lint-frontend`, `2 · test-frontend` y `4 · sbom-frontend` | Omitidos (*skipped*) |
| `3 · secretos`, `4 · sbom-backend` y `4 · sca` | Éxito; `sca` **no** se omite (caso límite «repositorio sin frontend») |

## 5. Activar la regla de la rama principal

Aplica el JSON de [contracts/ci-y-regla-main.md](contracts/ci-y-regla-main.md):

```bash
gh api -X PUT repos/paul-g-software-ucacue/tesis-prototipo-2026/rulesets/24901935 --input ruleset-main.json
gh api repos/paul-g-software-ucacue/tesis-prototipo-2026/rulesets/24901935 \
  -q '.rules[] | select(.type=="required_status_checks") | .parameters.required_status_checks[].context'
```

**Resultado esperado:** aparecen los 10 checks. La página del pull request los muestra como *Required* y solo ofrece *Squash and merge*.

## 6. Demostración de los escenarios (SC-001)

Desde `main`, crea la rama `demo/b00-h01` con dos servicios de prueba del mismo proceso y un proceso de prueba creados desde los esqueletos. Ábrela como pull request en borrador para confirmar que está en verde y **no la fusiones**. Después abre un pull request hacia `main` por cada violación, cada uno desde una rama que sale de `demo/b00-h01`:

| PR | Violación | Check que debe fallar | Escenario |
|---|---|---|---|
| 1 | Una clase de `dominio` importa una clase de Spring | `2 · test-backend` (`ArquitecturaTest`) | 1 |
| 2 | Un servicio usa una clase de `aplicacion` del otro servicio | `2 · test-backend` (`ModulosTest`) | 2 |
| 3 | Una prueba unitaria en rojo | `2 · test-backend` | 3 |
| 4 | Un módulo con menos de 60 % de líneas cubiertas | `2 · test-backend` (`jacocoTestCoverageVerification`) | 4 |
| 5 | Un adaptador arma una consulta SQL concatenando un parámetro | `1 · lint-backend` (SpotBugs, confianza alta) | 5 |

En cada pull request:

1. Confirma que el check indicado está en rojo y que GitHub muestra «Merging is blocked».
2. Anota en `evidencia.md` el enlace del pull request, el run y el mensaje de la herramienta (por ejemplo, el patrón de FindSecBugs).
3. Cierra el pull request sin fusionar y borra su rama.

Al final, cierra y borra `demo/b00-h01`.

## 7. Cierre

- El pull request de B00-H01, con todos sus checks en verde o como *skipped*, se fusiona con *squash* (SC-002).
- `evidencia.md` tiene las 4 comprobaciones y los 5 pull requests de demostración (SC-001, SC-004 y SC-005).
- Si alguna comprobación falló, hay una decisión D-28 o posterior y la sección 8 de la constitución está enmendada (FR-018).
