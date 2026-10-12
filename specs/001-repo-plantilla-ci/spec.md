# Feature Specification: Repositorio, plantilla de servicio en cinco capas y CI con Gradle

**Feature Branch**: `001-repo-plantilla-ci`

**Created**: 2026-10-11

**Status**: Draft

**Input**: User description: "Especifica la historia B00-H01. Léela de su issue con gh issue list --search "B00-H01 in:title" y gh issue view <número>. Usa la historia y sus escenarios tal como están, sin inventar requisitos, y respeta .specify/memory/constitution.md"

**Origen**: issue #18 · B00-H01 · sub-issue de #2 (B00 · Base técnica de la ola 0) · milestone «Ola 0 · Base técnica» · etiquetas `tipo:historia`, `prioridad:must`, `modulo:transversal` · tamaño propuesto L.

**Requerimientos que cubre**: ADR-008 · ADR-009 · ADR-011 · D-24 · D-27 · RNF-MAN-01 · RNF-MAN-02.

**Historia (tal como está en el issue)**: Como autor, quiero un monorepo privado con Gradle (Groovy DSL, Java 25, Spring Boot 4.1.1), la plantilla de servicio en cinco capas y las etapas de lint y pruebas en cada pull request, para que cada historia se integre con las pruebas en verde y sin cruzar límites.

> **Nota sobre el lenguaje de esta especificación.** Es una historia transversal de la base técnica: su usuario es el autor y su objeto son las herramientas que la constitución ya fija (sección 3, ADR-008, ADR-011). Por eso los nombres de herramientas aparecen tal como los trae la historia; la especificación no elige ninguna tecnología que la historia o la constitución no hayan fijado antes.

## Clarifications

### Session 2026-10-11

- Q: ¿Cómo se cumple ESLint en la etapa de lint antes de que exista la SPA de B00-H06? → A: Los jobs de frontend de la CI corren solo si existe `frontend/package.json`. ESLint sale de los criterios de esta historia y pasa a los de B00-H06 (#23).
- Q: El repositorio es privado y el plan de la cuenta original no permitía reglas de rama. ¿Cómo se cumple «el pull request no se puede fusionar»? → A: El repositorio se transfirió a la cuenta `paul-g-software-ucacue`, que tiene GitHub Pro; sigue siendo privado y admite la regla de la rama principal.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus cinco escenarios se agrupan aquí en cuatro partes que se pueden probar por separado. Todas son necesarias para cerrar la historia (`prioridad:must`); la prioridad solo indica el orden en que conviene construirlas.

### User Story 1 - Integración continua que bloquea la fusión (Priority: P1)

Como autor, quiero que cada pull request hacia la rama principal pase por las etapas de lint y de pruebas, y que la rama principal no acepte un pull request con algún check en rojo, para que ninguna historia se integre sin las pruebas en verde.

**Why this priority**: Es la compuerta de la que dependen las otras tres partes: los escenarios de capas, de cobertura y de análisis estático terminan en «el pull request no se puede fusionar» o «el check falla», y eso solo ocurre si esta compuerta existe.

**Independent Test**: Abrir un pull request hacia la rama principal con un cambio cualquiera; comprobar que se ejecutan las etapas de lint y de pruebas. Luego introducir un fallo deliberado en una prueba y comprobar que el pull request queda bloqueado para fusión.

**Acceptance Scenarios**:

```gherkin
Escenario: Integración continua en cada pull request
  Dado un pull request hacia la rama principal
  Cuando se abre o se actualiza
  Entonces GitHub Actions ejecuta las etapas de lint (Spotless y SpotBugs con FindSecBugs) y de pruebas (unitarias, de contrato, de arquitectura e integración con Testcontainers)
  Y la regla de la rama principal impide fusionarlo si algún check falla
```

> **Cambio por la aclaración del 2026-10-11**: ESLint salió de la etapa de lint de este escenario y pasó a B00-H06 (#23), con el escenario «Lint del frontend en cada pull request». Los issues #18 y #23 ya están actualizados.

---

### User Story 2 - Plantilla de servicio en cinco capas con límites verificados (Priority: P1)

Como autor, quiero crear cada servicio desde una plantilla en cinco capas (dominio, aplicación, puertos, adaptadores e infraestructura) que trae consigo las pruebas de arquitectura, para que ningún servicio rompa el sentido de las capas ni use las clases internas de otro servicio.

**Why this priority**: Los 12 servicios se crean desde esta plantilla; si la plantilla no impide cruzar límites desde el primer servicio, el principio I (arquitectura limpia) y el principio V (límites de servicio) quedan sin respaldo automático (RNF-MAN-01).

**Independent Test**: Crear un servicio desde la plantilla, agregar en su paquete dominio una dependencia hacia el framework web, el ORM o HTTP, y comprobar que la prueba de arquitectura falla. Con dos servicios del mismo proceso, hacer que uno use una clase no publicada del otro y comprobar que la verificación de módulos falla.

**Acceptance Scenarios**:

```gherkin
Escenario: La plantilla respeta las cinco capas
  Dado un servicio creado desde la plantilla
  Cuando una clase del paquete dominio importa una clase de Spring, JPA o HTTP
  Entonces la prueba de ArchUnit falla
  Y el pull request no se puede fusionar

Escenario: Un servicio no usa las clases internas de otro
  Dado dos servicios del mismo proceso
  Cuando uno usa una clase que no está en la API publicada del otro
  Entonces ApplicationModules.verify() de Spring Modulith falla
```

---

### User Story 3 - Cobertura mínima en cada pull request (Priority: P2)

Como autor, quiero que la etapa de pruebas rechace un módulo con menos de 60 % de líneas cubiertas, o un paquete dominio de un servicio central con menos de 80 %, para que el dominio central se mantenga guiado por pruebas (principio VIII, RNF-MAN-02).

**Why this priority**: Depende de que existan la compuerta (historia 1) y un servicio creado desde la plantilla (historia 2) sobre el cual medir.

**Independent Test**: En un módulo cualquiera, dejar la cobertura de líneas por debajo de 60 % y comprobar que la verificación de cobertura falla; en un servicio central, dejar su paquete dominio por debajo de 80 % (con el módulo por encima de 60 %) y comprobar que también falla.

**Acceptance Scenarios**:

```gherkin
Escenario: Cobertura mínima
  Dado un módulo con menos de 60 % de líneas cubiertas, o un paquete dominio de un servicio central con menos de 80 %
  Cuando corre la etapa de pruebas
  Entonces jacocoTestCoverageVerification falla
  Y el pull request no se puede fusionar
```

---

### User Story 4 - Análisis estático de seguridad en el lint (Priority: P2)

Como autor, quiero que la etapa de lint detecte con confianza alta el código vulnerable más común, como una consulta SQL armada concatenando un parámetro de entrada, para que no se integre código con hallazgos altos de seguridad (principio IX, fase de implementación del SSDLC).

**Why this priority**: Depende de la compuerta (historia 1); protege desde el primer servicio, pero no bloquea la construcción de la plantilla.

**Independent Test**: Agregar a un servicio una clase que arme una consulta SQL concatenando un parámetro de entrada y comprobar que la etapa de lint reporta un hallazgo de confianza alta y su check falla.

**Acceptance Scenarios**:

```gherkin
Escenario: Hallazgo de análisis estático
  Dado una clase que arma una consulta SQL concatenando un parámetro de entrada
  Cuando corre la etapa de lint
  Entonces SpotBugs con FindSecBugs reporta un hallazgo de confianza alta
  Y el check falla
```

---

### Edge Cases

- **Umbral exacto de cobertura**: un módulo con exactamente 60 % de líneas cubiertas, o un paquete dominio central con exactamente 80 %, no falla; el escenario solo rechaza lo que queda «por debajo».
- **Servicio de soporte o genérico**: el umbral de 80 % del paquete dominio solo se exige en los servicios centrales (5 Generación, 6 Validación OCS, 10 Firma y 12 Normativa); los demás solo deben cumplir el 60 % del módulo.
- **Pull request en borrador**: la etapa de integración no corre mientras el pull request es un borrador; al marcarlo como listo para revisión vuelve a ejecutarse y la regla de la rama principal exige su resultado antes de fusionar.
- **Dependencia del dominio hacia otros detalles técnicos**: además de Spring, JPA y HTTP, la constitución (principio I) prohíbe que el dominio importe librerías de PDF; la prueba de arquitectura también debe fallar en ese caso.
- **Dependencia entre servicios a través de la API publicada**: si un servicio usa solo lo que el otro publica, la verificación de módulos pasa; la regla no impide la colaboración, solo el uso de clases internas.
- **Hallazgo de confianza media o baja**: no hace fallar el check; la constitución solo exige que no haya hallazgos altos.
- **Repositorio sin frontend**: mientras no exista `frontend/package.json`, los jobs de frontend de la CI no corren y no hacen fallar el pull request. Los checks del backend siguen corriendo y siguen siendo obligatorios para fusionar: omitir los jobs de frontend no debe omitir ningún check del backend.
- **Llega el frontend**: cuando un pull request agrega `frontend/package.json` (B00-H06), los jobs de frontend empiezan a correr en ese mismo pull request sin cambiar el workflow.
- **Una comprobación de compatibilidad falla**: si palantir-java-format o SpotBugs no funcionan con Java 25, o Spring Modulith 2.1 no es la línea para Spring Boot 4.1, se registra una decisión nueva (D-28 en adelante) y se enmienda la constitución (sección 8), en lugar de desactivar la herramienta en silencio.

## Requirements *(mandatory)*

### Functional Requirements

**Repositorio y construcción**

- **FR-001**: El código DEBE vivir en el monorepo privado de GitHub ya creado (D-24, hoy en `paul-g-software-ucacue/tesis-prototipo-2026`), construido como multiproyecto de Gradle con Groovy DSL, Java 25 y Spring Boot 4.1.1 (constitución, sección 3).
- **FR-002**: Cada servicio DEBE ser un subproyecto de Gradle y cada proceso una aplicación Spring Boot que ensambla sus servicios (principio V, ADR-008, ADR-011); las reglas comunes de construcción, formato, análisis estático y cobertura DEBEN venir de la convención de Gradle compartida, no copiarse en cada servicio.

**Plantilla de servicio**

- **FR-003**: DEBE existir una plantilla de servicio organizada en las cinco capas del principio I: dominio, aplicación, puertos, adaptadores e infraestructura.
- **FR-004**: Un servicio creado desde la plantilla DEBE traer una prueba de arquitectura (ArchUnit) que falle cuando una clase del paquete dominio importa una clase de Spring, JPA o HTTP (escenario «La plantilla respeta las cinco capas»), y también cuando importa una librería de PDF (principio I).
- **FR-005**: La prueba de arquitectura DEBE verificar el sentido de las cinco capas: las dependencias apuntan hacia el dominio, nunca desde el dominio hacia los detalles (principios I y V).
- **FR-006**: Cada proceso DEBE verificar sus módulos con `ApplicationModules.verify()` de Spring Modulith, y esa verificación DEBE fallar cuando un servicio usa una clase que no está en la API publicada de otro servicio del mismo proceso (escenario «Un servicio no usa las clases internas de otro»).

**Integración continua**

- **FR-007**: Cada pull request hacia la rama principal, al abrirse o actualizarse, DEBE ejecutar en GitHub Actions la etapa de lint (Spotless y SpotBugs con FindSecBugs) y la etapa de pruebas (unitarias, de contrato, de arquitectura y de integración con Testcontainers).
- **FR-008**: Los jobs de frontend de la CI DEBEN correr solo si existe `frontend/package.json`. ESLint y el resto del lint y las pruebas del frontend son criterios de B00-H06, no de esta historia.
- **FR-009**: La regla de la rama principal DEBE impedir fusionar un pull request si algún check falla, y la rama principal solo DEBE recibir cambios por pull request (D-24, constitución sección 3). Los checks de las etapas de lint y de pruebas del backend DEBEN quedar como obligatorios en esa regla.
- **FR-010**: Las acciones de GitHub de los workflows DEBEN quedar fijadas por su SHA completo con `pinact run` (nota del issue; constitución, sección 8 y principio IX).
- **FR-011**: La CI DEBE usar los workflows, la convención de Gradle y las plantillas que ya están en el repositorio (nota del issue), sin duplicarlos.

**Cobertura**

- **FR-012**: La etapa de pruebas DEBE ejecutar `jacocoTestCoverageVerification`, que DEBE fallar si un módulo tiene menos de 60 % de líneas cubiertas o si un paquete dominio de un servicio central (5, 6, 10 y 12) tiene menos de 80 % (escenario «Cobertura mínima», principio VIII, RNF-MAN-02).
- **FR-013**: El umbral de 80 % DEBE aplicarse a un servicio por ser central, sin configuración adicional en cada servicio; los servicios de soporte y genéricos solo cumplen el 60 %.

**Análisis estático**

- **FR-014**: La etapa de lint DEBE ejecutar SpotBugs con FindSecBugs sobre el código de producción y su check DEBE fallar ante un hallazgo de confianza alta, como una consulta SQL armada concatenando un parámetro de entrada (escenario «Hallazgo de análisis estático»).
- **FR-015**: La etapa de lint DEBE fallar si el código Java no cumple el formato de Spotless.

**Comprobaciones de la ola 0 asignadas a esta historia (constitución, sección 8)**

- **FR-016**: DEBE comprobarse que palantir-java-format (vía Spotless) y SpotBugs funcionan con Java 25.
- **FR-017**: DEBE comprobarse que Spring Modulith 2.1 es la línea compatible con Spring Boot 4.1.
- **FR-018**: El resultado de cada comprobación (FR-010, FR-016, FR-017) DEBE quedar registrado; si alguna falla, DEBE registrarse una decisión nueva (D-28 en adelante) y enmendarse la constitución.

### Key Entities

- **Servicio**: uno de los 12 servicios de la ERS 8.5; capacidad de negocio con sus cinco capas, creado desde la plantilla.
- **Proceso**: aplicación que agrupa y ensambla varios servicios (identidad, datos, distributivo y salida, ADR-006); es el ámbito en que se verifican los límites entre servicios.
- **Plantilla de servicio**: punto de partida de cada servicio; fija las cinco capas y trae las pruebas de arquitectura y las reglas de cobertura.
- **API publicada de un servicio**: lo que un servicio expone a los demás de su proceso; todo lo demás es interno.
- **Pull request**: único camino para integrar un cambio en la rama principal.
- **Check**: resultado de una etapa de la CI sobre un pull request (lint, pruebas); en rojo bloquea la fusión.
- **Regla de la rama principal**: condición que impide fusionar un pull request con algún check en rojo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Los 5 escenarios de aceptación se demuestran con 5 pull requests de prueba, cada uno con una violación deliberada (dominio que importa el framework, uso de una clase interna de otro servicio, cobertura bajo el umbral, consulta SQL concatenada, prueba en rojo), y los 5 quedan bloqueados para fusión.
- **SC-002**: Un pull request sin violaciones pasa todos sus checks y se puede fusionar.
- **SC-003**: Ningún pull request se fusiona en la rama principal con un check en rojo desde que se cierra la historia (0 casos).
- **SC-004**: El 100 % de las acciones usadas en los workflows está fijado por SHA completo.
- **SC-005**: Las 3 comprobaciones de la constitución asignadas a esta historia (palantir-java-format y SpotBugs con Java 25, Spring Modulith 2.1 con Spring Boot 4.1, acciones fijadas por SHA) tienen su resultado registrado; ninguna queda pendiente.
- **SC-006**: Un servicio nuevo creado desde la plantilla queda cubierto por las reglas de capas, de límites, de cobertura y de análisis estático sin configuración adicional en el propio servicio.

## Assumptions

- El monorepo privado y el proyecto de GitHub Projects ya existen (constitución, versión 1.0, pendiente 6 resuelto); esta historia no los crea, los completa.
- El repositorio vive ahora en `paul-g-software-ucacue/tesis-prototipo-2026`, cuenta con GitHub Pro, y sigue siendo privado (D-24 no cambia). Ya tiene una regla activa sobre la rama principal que exige pull request y prohíbe borrarla o reescribir su historial; todavía no exige checks, y eso es lo que agrega esta historia (FR-009).
- El repositorio ya trae un borrador del workflow de CI (`.github/workflows/ci.yml`), la convención de Gradle (`backend/buildSrc`) y las plantillas de issue y de pull request; la «carpeta `github/plantillas/`» que cita el issue corresponde a lo que ya se copió al repositorio.
- Para demostrar los escenarios de capas y de límites basta con servicios creados desde la plantilla dentro de un proceso; los servicios reales con su lógica de negocio llegan con sus propias historias (por ejemplo, F04-H01 para Normativa).
- Las etapas de secretos y SCA del mismo workflow (Gitleaks, CycloneDX y Grype) son el alcance de B00-H07 (#24); esta historia no define su comportamiento, aunque sus checks, cuando existan, también cuentan para la regla de la rama principal.
- El despliegue con Docker Compose (B00-H02), el modelo de errores (B00-H04) y la SPA base con su lint (ESLint) y sus pruebas (B00-H06) quedan fuera de esta historia.
- El hallazgo de análisis estático solo se exige sobre el código de producción; el código de pruebas no viaja a producción.
- Las pruebas usan solo datos ficticios (principio VIII y IX).
