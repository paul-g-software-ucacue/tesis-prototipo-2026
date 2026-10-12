# Registro de decisiones del MDD

Este archivo es la **fuente única** de las decisiones que se toman durante el desarrollo, desde D-28 y ADR-012. Vive en el repositorio para que las especificaciones, los planes y la constitución no dependan de documentos externos (la matriz SCD en `lean_inception/Matriz_decisiones_SCD_MDD.xlsx` y los ADR de `lean_inception/adr/`).

## Cómo se usa

- **Decisión (D-NN):** una elección que cambia el alcance, una regla del proyecto o un documento ya aprobado (constitución, sección 7). Se registra **antes** de aplicarla.
- **ADR-NNN:** una decisión de arquitectura que afecta a más de una feature (constitución, sección 6, paso 4). Se registra **antes** de implementarla.
- Los números no se reutilizan. Una decisión que reemplaza a otra la cita en «Modifica a» y la otra cambia su estado.
- Cada entrada dice quién decidió, de dónde salió, qué cambia y qué documentos hay que poner al día.
- **Siguiente número libre:** D-34 · ADR-013.

## Índice

| ID | Fecha | Tema | Estado | Modifica a |
|---|---|---|---|---|
| D-28 | 2026-10-11 | JUnit Jupiter 6 en lugar de JUnit 5 | Aceptada | Constitución §3 (1.0.1), ADR-008, ADR-011 |
| D-29 | 2026-10-11 | Jobs de frontend condicionados; ESLint pasa a B00-H06 | Aceptada | Issue #18 (escenario de CI), issue #23 |
| D-30 | 2026-10-11 | Repositorio en la cuenta `paul-g-software-ucacue` con GitHub Pro | Aceptada | — (D-24 se mantiene) |
| D-31 | 2026-10-11 | Regla de la rama principal: 10 checks, estricta, solo *squash*, sin excepciones | Aceptada | — (aplica `pipeline_cicd.md` §8) |
| D-32 | 2026-10-11 | Cobertura única del 60 % por módulo, también en el dominio central | Aceptada | Constitución principio VIII y §3 (2.0.0), RNF-MAN-02, D-27, ADR-011 punto 3, issue #18 (escenario 4) |
| ADR-012 | 2026-10-11 | Plantilla de servicio: paquete base, API publicada y plugins de convención | Aceptada | ADR-011 punto 2 |
| D-33 | 2026-10-11 | google-java-format (estilo AOSP) en lugar de palantir-java-format | Aceptada | Constitución §8 (2.0.1), ADR-011 punto 4 (Spotless) |

---

## D-28. JUnit Jupiter 6 en lugar de JUnit 5

- **Estado:** Aceptada · **Fecha:** 2026-10-11 · **Decidido por:** autor de la tesis
- **Origen:** plan de B00-H01 (`specs/001-repo-plantilla-ci/research.md` §6)
- **Modifica a:** constitución §3 (versión 1.0.1), ADR-008 punto 10 y ADR-011 punto 3, que decían «JUnit 5»

**Decisión.** Las pruebas usan JUnit Jupiter 6, la versión que fija el BOM de Spring Boot (6.0.3 con Spring Boot 4.1.1). No se fija una versión propia.

**Por qué.** El BOM de `spring-boot-dependencies:4.1.1` gestiona `junit-jupiter.version = 6.0.3`, y la constitución y `pipeline_cicd.md` piden usar las versiones de pruebas del BOM. Forzar JUnit 5 contra el BOM podría romper `spring-boot-starter-test` y Spring Modulith 2.1.1 sin ningún beneficio.

**Consecuencias.** Enmienda de parche de la constitución (1.0 → 1.0.1). ADR-008 y ADR-011, fuera del repositorio, quedan desactualizados en ese punto; este registro prevalece.

---

## D-29. Jobs de frontend condicionados; ESLint pasa a B00-H06

- **Estado:** Aceptada · **Fecha:** 2026-10-11 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q1 de la especificación de B00-H01 (`specs/001-repo-plantilla-ci/spec.md`, Clarifications)
- **Modifica a:** issue #18 (el escenario de CI ya no nombra ESLint) e issue #23, B00-H06 (recibe el escenario «Lint del frontend en cada pull request»)

**Decisión.** Los jobs de frontend de `ci.yml` (`lint-frontend`, `test-frontend` y `sbom-frontend`) corren solo si existe `frontend/package.json`. ESLint deja de ser criterio de B00-H01 y pasa a B00-H06, que crea la SPA.

**Por qué.** La SPA llega con B00-H06. Sin `frontend/`, los jobs de frontend fallarían en cada pull request, y crear un esqueleto de frontend en B00-H01 adelantaría trabajo de otra historia.

**Alternativas.** Crear un esqueleto mínimo de `frontend/` en B00-H01; quitar los jobs de frontend hasta B00-H06.

**Consecuencias.** Un job `0 · deteccion` decide si existe el frontend, y el job `sca` deja de depender de que corra `sbom-frontend` (research §7). Los issues #18 y #23 se editaron el 2026-10-11.

---

## D-30. Repositorio en la cuenta `paul-g-software-ucacue` con GitHub Pro

- **Estado:** Aceptada · **Fecha:** 2026-10-11 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q2 de la especificación de B00-H01
- **Modifica a:** — (D-24 se mantiene: repositorio privado en GitHub con GitHub Projects)

**Decisión.** El repositorio se transfirió a `paul-g-software-ucacue/tesis-prototipo-2026`, una cuenta con GitHub Pro. Sigue siendo privado.

**Por qué.** En la cuenta anterior, GitHub no permitía reglas ni protección de rama en repositorios privados («Upgrade to GitHub Pro or make this repository public»), y la historia exige que un pull request con un check en rojo no se pueda fusionar.

**Alternativas.** Hacer público el repositorio (lo impide el contrato de confidencialidad, D-24); mantenerlo privado sin regla y fusionar por disciplina.

**Consecuencias.** Con GitHub Pro hay 3 000 minutos de Actions al mes para repositorios privados, en lugar de los 2 000 que suponía `pipeline_cicd.md` §9.

---

## D-31. Regla de la rama principal: 10 checks, estricta, solo *squash*, sin excepciones

- **Estado:** Aceptada · **Fecha:** 2026-10-11 · **Decidido por:** autor de la tesis
- **Origen:** plan de B00-H01 (research §8; `contracts/ci-y-regla-main.md`), que aplica `pipeline_cicd.md` §8
- **Modifica a:** — (completa la regla que ya pedían D-24 y ADR-011)

**Decisión.** El ruleset `main` (id 24901935) exige:
- los checks `0 · deteccion`, `1 · lint-backend`, `1 · lint-frontend`, `2 · test-backend`, `2 · test-integracion`, `2 · test-frontend`, `3 · secretos`, `4 · sbom-backend`, `4 · sbom-frontend` y `4 · sca`, publicados por GitHub Actions;
- el pull request al día con `main` (política estricta);
- solo fusión *squash*;
- sin actores que puedan saltarse la regla, tampoco el dueño del repositorio.

Se conservan las reglas que ya tenía: prohibido borrar `main` o reescribir su historial, pull request obligatorio y aprobación adicional para cambios sin autor atribuido.

**Por qué.** Así, el commit de `main` es el mismo código que pasó las etapas 1 a 4, que es lo que supone `cd.yml`, y ningún pull request en rojo entra a `main`.

**Consecuencias.** Excepción transitoria: si `3 · secretos`, `4 · sbom-backend` o `4 · sca` fallan en el pull request de B00-H01 por algo que corresponde a B00-H07, se quitan de la lista, se anota en la evidencia de B00-H01 y B00-H07 los vuelve a agregar.

---

## D-32. Cobertura única del 60 % por módulo, también en el dominio central

- **Estado:** Aceptada · **Fecha:** 2026-10-11 · **Decidido por:** autor de la tesis
- **Origen:** revisión del plan de B00-H01 (`/speckit-analyze`)
- **Modifica a:**
  - constitución, principio VIII y §3 (versión 2.0.0);
  - RNF-MAN-02 de la ERS v2.1 («≥ 80 % de cobertura del motor de reglas» pasa a «≥ 60 % de líneas por módulo»);
  - D-27 y ADR-011 punto 3 («JaCoCo 60 % por módulo y 80 % en el dominio central» pasa a «60 % por módulo»);
  - `pipeline_cicd.md` (etapa 2);
  - issue #18, escenario «Cobertura mínima».

**Decisión.** JaCoCo exige al menos 60 % de líneas cubiertas en cada módulo, sin un umbral mayor para los paquetes `dominio` de los servicios 5, 6, 10 y 12. Se elimina el plugin de convención `mdd.servicio-central`, que solo agregaba ese umbral.

**Por qué.** El autor quiere un solo umbral para todo el código, y no exigir más pruebas en el dominio central que en el resto.

**Lo que no cambia.** El dominio central se sigue desarrollando guiado por pruebas (principio VIII), y sus reglas de negocio se prueban sin infraestructura ni dobles. Un umbral más bajo no reduce el código de pruebas que se escribe: solo acepta dejar más líneas sin ejecutar.

**Alternativas.** Mantener el 80 % en el dominio central, que es lo que recomendó el agente para conservar RNF-MAN-02 tal como lo declara la tesis.

**Consecuencias.** Enmienda mayor de la constitución (2.0.0), porque cambia un principio. Los documentos externos (ERS, matriz SCD, ADR-011 y `pipeline_cicd.md`) quedan desactualizados en este punto; este registro prevalece.

---

## ADR-012. Plantilla de servicio: paquete base, API publicada y plugins de convención

- **Estado:** Aceptada · **Fecha:** 2026-10-11 · **Decidido por:** autor de la tesis
- **Origen:** plan de B00-H01 (`specs/001-repo-plantilla-ci/research.md` §10 a §13)
- **Modifica a:** ADR-011 punto 2 (dos plugins de convención en `buildSrc`)
- **Relacionadas:** ADR-006, ADR-008, ADR-009, D-32; principios I, V y XI

**Contexto.** ADR-011 fija dos plugins de convención, `mdd.java-conventions` y `mdd.servicio-central`. Al planificar B00-H01 hicieron falta reglas de arquitectura compartidas y una forma de publicar la API de un servicio dentro de su proceso: Spring Modulith trata como internos los subpaquetes de cada módulo, y las cinco capas son subpaquetes. Con D-32, `mdd.servicio-central` deja de tener sentido.

**Decisión.**
1. **Paquete base `ec.mdd`.** Cada servicio usa `ec.mdd.<servicio>`, sin número ni guiones (`ec.mdd.normativa`, `ec.mdd.validacionocs`). La clase de arranque de cada proceso vive en `ec.mdd`. Las rutas son `backend/servicios/<servicio>` y `backend/apps/<proceso>`.
2. **API publicada en el proceso.** Es el paquete `puertos.entrada` de cada servicio, marcado con `@NamedInterface("api")`. Todo lo demás es interno, y `ApplicationModules.verify()` lo hace cumplir.
3. **Reglas compartidas.** El subproyecto `plataforma:arquitectura` contiene las reglas ArchUnit de las cinco capas. No es un servicio, no tiene contrato OpenAPI y solo se usa en las pruebas.
4. **Plugins de convención.** Son tres: `mdd.java-conventions` (todos), `mdd.servicio` (los 12 servicios) y `mdd.aplicacion` (los 4 procesos). Se elimina `mdd.servicio-central` (D-32).
5. **Esqueletos.** Hay un esqueleto de servicio y otro de proceso en `backend/plantillas/`. Se copian cuando una historia crea un servicio o un proceso; no forman parte del build.

**Por qué.** Un servicio nuevo recibe las reglas, la cobertura, Spotless y SpotBugs con solo aplicar su plugin, sin copiar configuración (FR-002, SC-006). El puerto de entrada es por definición «lo que ofrece el servicio» (principio I). `ec.mdd` no revela la institución.

**Alternativas.** Un paquete `api` como sexta capa; copiar las reglas en cada servicio; un servicio de ejemplo compilado; un generador de andamiaje; el dominio invertido de la institución como paquete.

**Consecuencias.** Los eventos de dominio que se escuchen dentro del mismo proceso necesitarán su propio `@NamedInterface` cuando una historia los pida. Un servicio de soporte que otro servicio del mismo proceso necesite llamar tendrá que declarar su `puertos.entrada`.

---

## D-33. google-java-format (estilo AOSP) en lugar de palantir-java-format

- **Estado:** Aceptada · **Fecha:** 2026-10-11 · **Decidido por:** autor de la tesis
- **Origen:** comprobación de la sección 8 de la constitución en B00-H01 (`specs/001-repo-plantilla-ci/evidencia.md`, fila `palantir-java25`)
- **Modifica a:** constitución §8 (versión 2.0.1); ADR-011 y `pipeline_cicd.md`, que nombraban palantir-java-format en Spotless

**Decisión.** Spotless formatea el código Java con **google-java-format 1.36.1 en estilo AOSP**: sangría de 4 espacios y líneas de 100 caracteres. La versión queda fijada en `mdd.java-conventions`.

**Por qué.**
- palantir-java-format 2.102.0 no soporta dos construcciones de Java 25: la importación de módulos (JEP 511) y los archivos fuente compactos (JEP 512). Son los issues abiertos [#1506](https://github.com/palantir/palantir-java-format/issues/1506) y [#1701](https://github.com/palantir/palantir-java-format/issues/1701).
- google-java-format usa el analizador de javac y soporta toda la sintaxis de Java 25.
- Cambiar ahora, con muy poco código, cuesta mucho menos que reformatear los 12 servicios más adelante.

**Detalle técnico.** Se usa la 1.36.1 y no la 1.37.0, la última disponible, porque Spotless 8.10.4 trae por defecto la 1.36.1 y falla con la 1.37.0 (`InvocationTargetException` en todos los archivos). Usada directamente desde la línea de comandos, la 1.37.0 sí funciona con Java 25. Cuando Dependabot proponga una versión nueva de Spotless, se revisa si ya acepta la 1.37 o una posterior.

**Alternativas.** Mantener palantir y prohibir esas dos construcciones (la CI ya las rechazaba); usar google-java-format con el estilo de Google por defecto (sangría de 2 espacios).

**Consecuencias.** El código existente se reformateó una vez (6 archivos de `plataforma:arquitectura` y los esqueletos de `backend/plantillas/`). Las líneas pasan de un máximo de 120 a 100 caracteres. Los esqueletos se formatearon con el mismo Spotless que usa la CI; el JAR de la línea de comandos en modo `--aosp` agrupa los `import` de otra forma (agrega una línea en blanco entre grupos).

---

## Anexo A. Decisiones anteriores (D-01 a D-27)

Copia de la tabla de la ERS v2.1, sección 8.6, al 2026-10-11. El registro original es `lean_inception/Matriz_decisiones_SCD_MDD.xlsx`; las de arquitectura, integración y flujo tienen además un ADR (anexo B).

| ID | Tema | Decisión | Registro |
|---|---|---|---|
| D-01 | Alcance del prototipo | Ciclo completo con flujo y árbol de firmas configurables por pantalla; reportería básica. Must pasa de 48 a 43 (más RF-37 mixto) y 14 RF quedan W | Matriz, ADR-005 (reemplazada por ADR-007) |
| D-02 | Capacidad de la nocturna | Nueva RN-26 y ventanas como parámetro | Matriz |
| D-03 | Despliegue de los 12 servicios | Dos perfiles de Docker Compose y medición con k6 (reemplazada por D-18) | ADR-001 |
| D-04 | CSV para SIGA | Layout propio con convenciones SENESCYT y mapeo configurable | ADR-002 |
| D-05 | Mallas | Desarrollo de Software como carrera de referencia | Matriz |
| D-06 | Nómina | 47 docentes más escenarios ±15 % | Matriz |
| D-07 | Orgánico | 21 gestiones de POL y estructura de los datos de prueba | Matriz |
| D-08 | Comisiones | Fuera de alcance, en «No hace» | Matriz |
| D-09 | Ventanas de jornada | Las actuales, como parámetro | Matriz |
| D-10 | 30 h (C-02) | No es dedicación: es la condición de cuidador sustituto | Matriz |
| D-11 | Medio tiempo (C-03) | 12 + 8 como parámetro | Matriz |
| D-12 | Institución (C-18) | IST público SENESCYT Tipo 2 como referencia | Matriz |
| D-13 | Coordinador académico (C-22) | Rol «Responsable de consolidación» | ADR-003 |
| D-14 | Firmantes (V-07) | Árbol de 9.1 definitivo y configurable; Quipux solo remite | ADR-004 |
| D-15 | Métricas (V-08) | Medir y fijar la meta con lo medido | Matriz |
| D-16 | Permisos (V-10) | Matriz rol × permiso de la sección 3 | Matriz |
| D-17 | Servicio Dashboard | Versión mínima: estado del periodo | Matriz |
| D-18 | Ajuste al plazo de 5 meses | Solo el perfil consolidado: 12 módulos en 4 procesos | ADR-006 |
| D-19 | Recorte para una sola persona | Configuración del flujo y firmas en archivo, firma con certificado solo en F1–F3, validación sin sello de tiempo, una propuesta por asignatura, franjas en Should, un escenario de k6 | ADR-007 |
| D-20 | Stack de desarrollo | Java 21 y Spring Boot (Maven multimódulo: 12 módulos en 4 aplicaciones, Spring Modulith y ArchUnit), React con TypeScript, contratos OpenAPI 3.0.3. Modificada por D-27. | ADR-008 |
| D-21 | Trazabilidad de la implementación | GitLab: issues como historias, milestones como olas, etiquetas por RF y merge requests con GitLab CI. Reemplazada por D-24 | Matriz |
| D-22 | Metodología de desarrollo | Kanban personal con prácticas XP y SDD, cadencia semanal y olas como hitos (`sdd/metodologia_desarrollo.md`) | Matriz |
| D-23 | Validación de la firma subida (Día 3 del Lean Inception) | Validación básica: prefijo de bytes y una firma nueva, integridad, vigencia y orden; confianza contra raíces en archivo; identidad por CN | Matriz |
| D-24 | Trazabilidad de la implementación (reemplaza a D-21) | GitHub: GitHub Projects con features como issues épicos e historias como sub-issues, milestones como olas, pull requests y GitHub Actions | Matriz |
| D-25 | Estándares de ingeniería backend | 12 microservicios desplegados en los 4 procesos de ADR-006, cinco capas, modelo de errores propio (titulo, descripcion y codigo), eventos persistentes con reintento entre procesos, cada transacción dentro de un servicio y JavaDoc en español | ADR-009 |
| D-26 | Cálculo del puntaje de afinidad | En el servicio 5 (Generación), con la ficha de Docentes y los pesos de Normativa; Docentes conserva la ficha académica | ADR-010 |
| D-27 | Plataforma y pipeline de DevSecOps | Java 25, Spring Boot 4.1.1 y Gradle con Groovy DSL. Pipeline de seis etapas: lint (Spotless, SpotBugs con FindSecBugs), pruebas (Mockito, JaCoCo 60 % por módulo y 80 % en el dominio central), Gitleaks, SCA (CycloneDX y Grype), publicación en Docker Hub privado y despliegue en un VPS de demostración. ZAP y k6 al cerrar cada ola. | ADR-011 |

**Cambios posteriores a esta tabla:**
- D-20 quedó modificada por D-25 (cinco capas y estándares de backend) y por D-27 (Java 25, Spring Boot 4.1.1 y Gradle).
- D-21 quedó reemplazada por D-24.
- D-27 quedó modificada por D-28 (JUnit Jupiter 6) y por D-32 (cobertura única del 60 %).

## Anexo B. ADR anteriores (ADR-001 a ADR-011)

Estado al 2026-10-11. El texto completo está en `lean_inception/adr/`.

| ADR | Título | Estado |
|---|---|---|
| ADR-001 | Despliegue de los 12 servicios en dos perfiles de Docker Compose | Reemplazada por ADR-006 |
| ADR-002 | Layout propio del CSV para SIGA con mapeo configurable | Aceptada |
| ADR-003 | Rol «Responsable de consolidación» designado por el Vicerrectorado | Aceptada |
| ADR-004 | Árbol de firmas definitivo del prototipo y Quipux como canal de remisión | Aceptada, modificada por ADR-007 |
| ADR-005 | Flujo de estados configurable por pantalla, con esqueleto fijo | Reemplazada por ADR-007 |
| ADR-006 | Solo el perfil consolidado para el prototipo (12 módulos en 4 procesos) | Aceptada |
| ADR-007 | Recorte del prototipo para una sola persona: configuración en archivo y firma acotada | Aceptada |
| ADR-008 | Stack de desarrollo: Java con Spring Boot en el backend y React en el frontend | Aceptada, modificada por ADR-009, ADR-011 y D-28 |
| ADR-009 | Estándares de ingeniería backend: microservicios en el perfil consolidado, cinco capas, errores y fallos entre procesos | Aceptada |
| ADR-010 | El puntaje de afinidad se calcula en el servicio de Generación (contexto Distributivo) | Aceptada |
| ADR-011 | Plataforma Java 25 con Spring Boot 4.1.1 y Gradle, y pipeline de DevSecOps | Aceptada, modificada por ADR-012 (punto 2), D-28, D-32 (punto 3) y D-33 (formateador de Spotless) |
