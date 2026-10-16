# Feature Specification: Etapas de secretos y SCA con SBOM por módulo

**Feature Branch**: `008-secretos-sca-sbom`

**Created**: 2026-10-15

**Status**: Draft

**Input**: User description: "B00-H07 · Etapas de secretos y SCA con SBOM por módulo (issue #24). La historia formaliza lo que ya existe desde B00-H01, completa lo que falte y demuestra los cuatro escenarios con evidencia, como en las historias anteriores."

**Origen**: issue #24 · B00-H07 · sub-issue de #2 (B00 · Base técnica de la ola 0) · milestone «Ola 0 · Base técnica» · etiquetas `tipo:historia`, `prioridad:must`, `modulo:transversal` · tamaño propuesto M · depende de #18 (B00-H01), cerrada · bloquea a #25 (B00-H08).

**Requerimientos que cubre**: ADR-011 · D-27 · RNF-SEG-06. **Decisiones que aplica** (`docs/decisiones.md`): D-31 (los 10 checks de la rama principal, entre ellos `3 · secretos`, `4 · sbom-backend`, `4 · sbom-frontend` y `4 · sca`), D-29 (los jobs de frontend corren solo si existe `frontend/package.json`) y el principio IX de la constitución (SSDLC: Gitleaks en cada commit y en cada pull request; un SBOM CycloneDX por módulo y por aplicación; Grype sin vulnerabilidades altas o críticas con versión corregida; el certificado real del autor nunca entra al repositorio).

**Historia (tal como está en el issue)**: Como autor, quiero que cada pull request busque secretos y vulnerabilidades conocidas en las dependencias, con un SBOM CycloneDX por módulo, para no subir nunca una credencial ni el certificado real y no entregar dependencias vulnerables.

**Punto de partida.** B00-H01 dejó los cuatro jobs en `ci.yml`, `.gitleaks.toml` con la regla `certificado-o-llave` y la CA de prueba permitida, y el hook de pre-commit con Gitleaks. Los cuatro checks ya son obligatorios en la regla de `main` y la excepción transitoria de D-31 no se usó (evidencia de B00-H01). Esta historia define su comportamiento, cierra lo que falte y demuestra los cuatro escenarios.

**Decisiones tomadas en esta historia:** D-54.

## Clarifications

### Session 2026-10-15

- Q: Si Grype encuentra una vulnerabilidad alta o crítica con versión corregida que todavía no se puede aplicar, ¿qué se permite? → A: Una excepción temporal, versionada en el repositorio, con el identificador, el paquete, el motivo y una fecha de vencimiento de 30 días como máximo. Al vencer, el check vuelve a fallar (D-54).

> **Nota sobre el lenguaje de esta especificación.** Es una historia de la base técnica: su usuario es el autor y su objeto es el pipeline que fija la constitución. Los nombres «Gitleaks», «CycloneDX», «Grype» y «pre-commit» aparecen porque los traen los escenarios del issue y la constitución; la especificación no elige ninguna herramienta nueva.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus cuatro escenarios son cuatro partes que se pueden probar por separado. Todas son necesarias para cerrar la historia (`prioridad:must`); la prioridad solo indica el orden en que conviene construirlas.

### User Story 1 - Un secreto en un commit no llega al repositorio (Priority: P1)

Como autor, quiero que Gitleaks revise cada commit antes de crearlo y cada pull request, para que una contraseña escrita en un archivo de configuración no llegue a `main` (RNF-SEG-06).

**Why this priority**: Una credencial que llega a GitHub queda expuesta aunque se borre después; es el control que más daño evita.

**Independent Test**: Agregar una contraseña de prueba escrita en un archivo de configuración y comprobar que el hook de pre-commit impide el commit; saltando el hook, abrir un pull request de demostración y comprobar que el check `3 · secretos` falla y que la regla de `main` impide fusionarlo.

**Acceptance Scenarios**:

```gherkin
Escenario: Secreto en un commit
  Dado un commit que agrega una contraseña de prueba escrita en un archivo de configuración
  Cuando corre el hook de pre-commit o la etapa de secretos
  Entonces Gitleaks lo detecta y el commit o el check falla
```

---

### User Story 2 - El certificado real no entra al repositorio (Priority: P1)

Como autor, quiero que la etapa de secretos rechace cualquier certificado o llave fuera de la carpeta de la CA de prueba, y que acepte los de la CA de prueba, para que el certificado real con el que pruebo FirmaEC nunca entre al repositorio (principio IX, S-10, ADR-017).

**Why this priority**: El certificado real del autor es la credencial más sensible del proyecto; los de la CA de prueba los usan las pruebas de Firma desde B00-S01.

**Independent Test**: Abrir un pull request de demostración que agrega un `.p12` ficticio fuera de la carpeta de la CA de prueba y comprobar que `3 · secretos` falla con la regla `certificado-o-llave`; comprobar en el mismo run, o en `main`, que los `.p12` de la CA de prueba no se reportan.

**Acceptance Scenarios**:

```gherkin
Escenario: Certificado fuera de la CA de prueba
  Dado un archivo .p12 agregado fuera de la carpeta de la CA de prueba
  Cuando corre la etapa de secretos
  Entonces Gitleaks lo rechaza con la regla certificado-o-llave
  Y los .p12 de la CA de prueba no se rechazan
```

---

### User Story 3 - Un SBOM por módulo (Priority: P1)

Como autor, quiero un SBOM CycloneDX por cada servicio, por cada aplicación y del frontend, para saber qué dependencias viajan en cada pieza y poder revisarlas una por una (principio IX, ADR-011).

**Why this priority**: Es la entrada de la revisión de vulnerabilidades (User Story 4) y el inventario que pide la entrega.

**Independent Test**: En un pull request listo para revisión, descargar los SBOM del run y comprobar que hay uno por cada uno de los 12 servicios, uno por cada aplicación (los 4 procesos y el API Gateway), uno por cada módulo de `plataforma` y uno del frontend, en formato CycloneDX JSON y con las dependencias de ejecución, también las transitivas.

**Acceptance Scenarios**:

```gherkin
Escenario: SBOM por módulo
  Dado el backend y el frontend
  Cuando corre la etapa de SCA
  Entonces se genera un SBOM CycloneDX por cada servicio, por cada aplicación y del frontend
```

---

### User Story 4 - Una dependencia vulnerable bloquea la fusión (Priority: P1)

Como autor, quiero que Grype revise cada SBOM y que el check de SCA falle si hay una vulnerabilidad alta o crítica que tiene versión corregida, con un informe que diga qué dependencia actualizar, para no entregar dependencias vulnerables que tienen arreglo.

**Why this priority**: Es el control de la etapa de entrega del SSDLC; sin él, los SBOM son solo un inventario.

**Independent Test**: Abrir un pull request de demostración que agrega una dependencia con una vulnerabilidad alta conocida y corregida en una versión posterior, y comprobar que `4 · sca` falla y que su informe dice el módulo, la dependencia, la versión instalada, la versión corregida y la vulnerabilidad.

**Acceptance Scenarios**:

```gherkin
Escenario: Dependencia vulnerable
  Dado una dependencia con una vulnerabilidad alta que tiene versión corregida
  Cuando Grype revisa los SBOM
  Entonces el check de SCA falla y el informe dice qué dependencia actualizar
```

---

### Edge Cases

- **Referencia a una variable de entorno** (por ejemplo, `spring.datasource.password: ${DB_CLAVE_IDENTIDAD}`): no es un secreto y no se reporta.
- **Valores ficticios documentados de `.env.ejemplo`** (los que empiezan con `cambiar-en-local-`): no se reportan; un valor de otra forma en ese archivo sí.
- **Falso positivo ya revisado**: se registra en `.gitleaksignore` con el commit, el archivo, la regla y la línea, y un comentario que explica por qué (como el de B00-S01). Sin esa entrada, el check sigue fallando.
- **Secreto agregado y borrado en un commit posterior del mismo pull request**: el check sigue fallando, porque el secreto ya está en el historial que se subió a GitHub. Se corrige reescribiendo el historial de la rama o, si no era un secreto, con una entrada en `.gitleaksignore`.
- **Secreto en otra rama** (por ejemplo, la rama de una demostración todavía abierta): no hace fallar los pull requests de las demás ramas.
- **Contraseña con poca entropía** (por ejemplo, `clave123`) escrita en un archivo de configuración: también se detecta; la detección no depende solo de que el valor parezca aleatorio.
- **Certificado con otra extensión** (`.pfx`, `.pem`, `.key`, `.jks`, `.keystore`, `.crt`, `.cer`): se rechaza igual que un `.p12` fuera de la CA de prueba.
- **Copias de la CA de prueba en carpetas de compilación** (`build/`): Git las ignora y no llegan a revisarse.
- **Vulnerabilidad alta sin versión corregida**: no hace fallar el check, pero aparece en el informe.
- **Vulnerabilidad media o baja con versión corregida**: no hace fallar el check; aparece en el informe y Dependabot propone la actualización.
- **Vulnerabilidad que no se puede corregir todavía** (por ejemplo, el arreglo exige una versión mayor que rompe el proyecto): se acepta una excepción temporal, con motivo y vencimiento de 30 días como máximo; al vencer, el check vuelve a fallar (D-54).
- **Pull request en borrador**: `4 · sbom-backend`, `4 · sbom-frontend` y `4 · sca` no corren y no bloquean; corren al marcarlo listo para revisión (B00-H01).
- **Pull request sin `frontend/`**: `4 · sbom-frontend` queda omitido y `4 · sca` revisa igual los SBOM del backend (D-29, B00-H01).
- **Hook de pre-commit no instalado**: el commit se crea, pero la etapa de secretos del pull request lo detecta. El hook es la primera barrera, no la única.
- **La base de vulnerabilidades no se puede descargar**: el check de SCA falla en lugar de pasar sin revisar.

## Requirements *(mandatory)*

### Functional Requirements

**Secretos (escenarios 1 y 2)**

- **FR-001**: La etapa de secretos DEBE correr en cada pull request hacia `main`, también en los borradores, y publicar el check `3 · secretos` (D-31).
- **FR-002**: La etapa DEBE revisar todo el historial que el pull request llevaría a GitHub: los commits de su rama y los de `main`, no solo el último commit. Un secreto de otra rama NO DEBE hacer fallar el pull request.
- **FR-003**: Una contraseña escrita como valor literal en un archivo de configuración (`.yml`, `.yaml`, `.properties`, `.env*`, `.conf`, `.cfg`, `.ini` y `.toml`), con una clave en inglés o en español (`password`, `secret`, `clave`, `contrasena` y similares), DEBE detectarse aunque tenga poca entropía. Las referencias a variables de entorno (`${…}`) y los valores ficticios documentados de `.env.ejemplo` NO DEBEN reportarse.
- **FR-004**: La regla `certificado-o-llave` DEBE rechazar los archivos de certificados y llaves (`.p12`, `.pfx`, `.pem`, `.key`, `.jks`, `.keystore`, `.crt`, `.cer`) fuera de la carpeta de la CA de prueba, y NO DEBE rechazar los que están en ella.
- **FR-005**: El hook de pre-commit DEBE usar la misma configuración y la misma versión de Gitleaks que la etapa de secretos, para que lo que pasa en local pase en la CI y al revés. La guía de la historia DEBE explicar cómo instalarlo.
- **FR-006**: El informe de la etapa DEBE decir el archivo, la línea, el commit y la regla de cada hallazgo, sin mostrar el valor del secreto.
- **FR-007**: Un falso positivo solo DEBE poder aceptarse con una entrada en `.gitleaksignore` que diga el commit, el archivo, la regla y la línea, con un comentario que explique por qué.

**SBOM (escenario 3)**

- **FR-008**: La etapa de SCA DEBE generar un SBOM CycloneDX en JSON por cada uno de los 12 servicios, por cada una de las 5 aplicaciones (los 4 procesos y el API Gateway), por cada módulo de `plataforma` y uno del frontend, en los pull requests listos para revisión.
- **FR-009**: Cada SBOM DEBE incluir las dependencias que viajan en la imagen (las de ejecución, directas y transitivas), y NO las que solo se usan para compilar o probar.
- **FR-010**: Los SBOM DEBEN quedar como artefacto del run, con un nombre que diga a qué módulo corresponde cada uno, para poder descargarlos y revisarlos.

**Vulnerabilidades (escenario 4)**

- **FR-011**: Grype DEBE revisar cada SBOM, y el check `4 · sca` DEBE fallar si encuentra una vulnerabilidad alta o crítica que tiene versión corregida en cualquiera de ellos (principio IX).
- **FR-012**: El informe DEBE decir, por cada hallazgo, el módulo, la dependencia, la versión instalada, la versión corregida, el identificador de la vulnerabilidad y su severidad, y DEBE verse en el run sin descargar archivos.
- **FR-013**: Si Grype no puede descargar o validar su base de vulnerabilidades, el check DEBE fallar.

**Cadena de suministro del pipeline**

- **FR-014**: Gitleaks DEBE fijarse por *digest* (la imagen de la CI) y por SHA (el hook); la acción que instala Grype, por SHA; y Grype, por versión exacta, que la acción instala con el instalador de ese mismo tag y verificando el SHA-256 contra los checksums de la release (principio IX, ADR-011). B00-H01 dejó la imagen de Gitleaks fijada solo por etiqueta.
- **FR-015**: Los cuatro checks (`3 · secretos`, `4 · sbom-backend`, `4 · sbom-frontend` y `4 · sca`) DEBEN seguir en la regla de la rama principal (D-31).
- **FR-016**: Una vulnerabilidad alta o crítica con versión corregida solo DEBE dejar de hacer fallar el check con una excepción versionada en el repositorio, que diga el identificador, el paquete, el motivo y la fecha de vencimiento, de 30 días como máximo. Al vencer, el check DEBE volver a fallar por esa vulnerabilidad (D-54).

### Key Entities

- **Hallazgo de secreto**: archivo, línea, commit y regla de Gitleaks; nunca el valor.
- **Excepción de secreto**: entrada de `.gitleaksignore` con el commit, el archivo, la regla, la línea y el porqué.
- **SBOM**: inventario CycloneDX de las dependencias de ejecución de un módulo (servicio, aplicación o frontend).
- **Excepción de vulnerabilidad**: identificador de la vulnerabilidad, paquete, motivo y fecha de vencimiento (D-54).
- **Hallazgo de vulnerabilidad**: módulo, dependencia, versión instalada, versión corregida, identificador y severidad.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Una contraseña de prueba escrita en un archivo de configuración hace fallar el hook de pre-commit y el check `3 · secretos` el 100 % de las veces, comprobado con un valor de poca entropía y una clave en español.
- **SC-002**: Un `.p12` ficticio fuera de la CA de prueba hace fallar `3 · secretos` con la regla `certificado-o-llave`, y los 6 `.p12` de la CA de prueba no se reportan.
- **SC-003**: En `main`, `3 · secretos` pasa con 0 hallazgos: las referencias `${…}` y los valores ficticios de `.env.ejemplo` no se reportan.
- **SC-004**: Un run de un pull request listo para revisión produce un SBOM por cada subproyecto de Gradle con código y uno del frontend (hoy 24: 12 de servicios, 5 de aplicaciones, 6 de los módulos de `plataforma` y 1 del frontend), cada uno identificable por su módulo, y ninguno vacío ni repetido.
- **SC-005**: Una dependencia con una vulnerabilidad alta corregida hace fallar `4 · sca`, y el informe dice el módulo, la dependencia, la versión instalada y la versión a la que hay que actualizar.
- **SC-006**: En `main`, `4 · sca` pasa sin vulnerabilidades altas o críticas con versión corregida.
- **SC-007**: Un pull request con cualquiera de los tres fallos (secreto, certificado o dependencia vulnerable) no se puede fusionar.
- **SC-008**: Una excepción vencida vuelve a hacer fallar `4 · sca`.
- **SC-009**: La etapa de secretos termina en menos de 5 minutos y la de SCA (SBOM y Grype) en menos de 10, como los límites que ya tienen los jobs.

## Assumptions

- Los cuatro jobs, `.gitleaks.toml`, `.gitleaksignore` y `.pre-commit-config.yaml` ya existen desde B00-H01; esta historia los ajusta solo donde hace falta para cumplir los requisitos.
- Los escenarios 1, 2 y 4 se demuestran con pull requests de demostración que se cierran sin fusionar y cuyas ramas se borran después, como los de B00-H01 (#80, #81). Usan solo valores y certificados ficticios, nunca el certificado real del autor.
- «Aplicación» incluye los 4 procesos (`identidad`, `datos`, `distributivo`, `salida`) y el API Gateway, que también se publica como imagen en B00-H08. Los módulos de `plataforma/` son bibliotecas: tienen su propio SBOM y sus dependencias aparecen también en los SBOM de las aplicaciones que los usan.
- El SBOM agregado del backend que generaba B00-H01 deja de generarse: repite todos los componentes y duplica cada hallazgo (research §7).
- El SBOM del frontend excluye las dependencias de desarrollo, porque la imagen de Nginx solo lleva los archivos compilados.
- La revisión de las imágenes con Grype (las 6 imágenes y su imagen base) es de B00-H08; esta historia revisa las dependencias de los módulos.
- Dependabot sigue proponiendo las actualizaciones de dependencias; esta historia no cambia su configuración.
- Solo se usan datos ficticios (principios VIII y IX).
