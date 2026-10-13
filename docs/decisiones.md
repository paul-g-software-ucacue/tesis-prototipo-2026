# Registro de decisiones del MDD

Este archivo es la **fuente única** de las decisiones que se toman durante el desarrollo, desde D-28 y ADR-012. Vive en el repositorio para que las especificaciones, los planes y la constitución no dependan de documentos externos (la matriz SCD en `lean_inception/Matriz_decisiones_SCD_MDD.xlsx` y los ADR de `lean_inception/adr/`).

## Cómo se usa

- **Decisión (D-NN):** una elección que cambia el alcance, una regla del proyecto o un documento ya aprobado (constitución, sección 7). Se registra **antes** de aplicarla.
- **ADR-NNN:** una decisión de arquitectura que afecta a más de una feature (constitución, sección 6, paso 4). Se registra **antes** de implementarla.
- Los números no se reutilizan. Una decisión que reemplaza a otra la cita en «Modifica a» y la otra cambia su estado.
- Cada entrada dice quién decidió, de dónde salió, qué cambia y qué documentos hay que poner al día.
- **Siguiente número libre:** D-40 · ADR-016.

## Índice

| ID | Fecha | Tema | Estado | Modifica a |
|---|---|---|---|---|
| D-28 | 2026-10-11 | JUnit Jupiter 6 en lugar de JUnit 5 | Aceptada | Constitución §3 (1.0.1), ADR-008, ADR-011 |
| D-29 | 2026-10-11 | Jobs de frontend condicionados; ESLint pasa a B00-H06 | Aceptada | Issue #18 (escenario de CI), issue #23 |
| D-30 | 2026-10-11 | Repositorio en la cuenta `paul-g-software-ucacue` con GitHub Pro | Aceptada | — (D-24 se mantiene) |
| D-31 | 2026-10-11 | Regla de la rama principal: 10 checks, estricta, solo *squash*, sin excepciones | Aceptada | — (aplica `pipeline_cicd.md` §8) |
| D-32 | 2026-10-11 | Cobertura única del 60 % por módulo, también en el dominio central | Aceptada | Constitución principio VIII y §3 (2.0.0), RNF-MAN-02, D-27, ADR-011 punto 3, issue #18 (escenario 4) |
| ADR-012 | 2026-10-11 | Plantilla de servicio: paquete base, API publicada y plugins de convención | Aceptada, modificada por D-35 (los 12 servicios se crean en B00-H02) | ADR-011 punto 2 |
| D-33 | 2026-10-11 | google-java-format (estilo AOSP) en lugar de palantir-java-format | Aceptada | Constitución §8 (2.0.1), ADR-011 punto 4 (Spotless) |
| D-34 | 2026-10-12 | Medición de R-19 en dos pasos: en B00-H02 tras un calentamiento y en B00-H03 tras iniciar sesión | Aceptada | Issue #19 (escenario de memoria), issue #20 |
| D-35 | 2026-10-12 | B00-H02 crea los 12 servicios vacíos, cada uno con su esquema y una migración mínima | Aceptada | Issue #19 (alcance), ADR-012 (cuándo se crean los servicios) |
| ADR-013 | 2026-10-12 | Persistencia por servicio y construcción de imágenes desde el código | Aceptada, modificada por ADR-015 (contexto de construcción) | `deploy/docker/java.Dockerfile` (B00-H08) |
| D-36 | 2026-10-12 | El despliegue pasa a un servidor o PC de 4 núcleos y 8 GiB de RAM | Aceptada | Constitución principio V, §3 y §8 (2.1.0), RNF-POR-02, R-19, ADR-006, ADR-008, issues #19 y #20 |
| D-37 | 2026-10-13 | B00-H03 incluye un inicio de sesión mínimo para usuarios de prueba | Aceptada | Alcance de los issues #20 y #43 (F05-H01) |
| D-38 | 2026-10-13 | El 401 de B00-H03 usa el modelo de error del principio XIII antes de B00-H04 | Aceptada | Alcance de los issues #20 y #21 (B00-H04) |
| D-39 | 2026-10-13 | «Sesión actual» en un servicio de cada proceso para la prueba de punta a punta | Aceptada | Alcance del issue #20 |
| ADR-014 | 2026-10-13 | Autenticación con JWT RS256 emitido por Usuarios | Aceptada | ADR-008 (detalle de la autenticación) |
| ADR-015 | 2026-10-13 | Contratos OpenAPI en `contratos/` y generación por servicio | Aceptada | ADR-013 punto 3 (contexto de construcción de las imágenes) |

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

## D-34. Medición de R-19 en dos pasos

- **Estado:** Aceptada · **Fecha:** 2026-10-12 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q1 de la especificación de B00-H02 (`specs/002-compose-cuatro-procesos/spec.md`)
- **Modifica a:** issue #19 (escenario «Cabe en la memoria de la VM») e issue #20, B00-H03 (recibe el escenario de la medición después de iniciar sesión)

**Decisión.** B00-H02 mide la memoria con `docker stats` con todo levantado y sano, después de un calentamiento que llama a la salud de cada proceso. La medición «después de iniciar sesión» pasa a B00-H03, que es la historia que crea el inicio de sesión.

**Por qué.** El escenario original de B00-H02 pedía medir después de iniciar sesión, pero el inicio de sesión llega con B00-H03, que depende de B00-H02. Así R-19 se conoce desde la ola 0 y se vuelve a medir con una sesión real.

**Alternativas.** Medir solo en reposo, sin repetir; dejar la medición pendiente hasta B00-H03 (B00-H02 no podría cerrarse).

**Consecuencias.** Los issues #19 y #20 se editaron el 2026-10-12. La evidencia de R-19 tiene dos mediciones.

---

## D-35. B00-H02 crea los 12 servicios vacíos

- **Estado:** Aceptada · **Fecha:** 2026-10-12 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q2 de la especificación de B00-H02
- **Modifica a:** ADR-012 (decía que los servicios llegan con sus historias) y el alcance del issue #19

**Decisión.** B00-H02 crea los 12 servicios desde `backend/plantillas/servicio`, sin lógica de negocio. Cada uno tiene su esquema en PostgreSQL y una primera migración de Flyway mínima, sin tablas de negocio. Los 4 procesos los ensamblan según ADR-006. Las tablas y la lógica de cada servicio llegan con sus historias.

**Por qué.** El escenario «Un esquema por servicio» necesita servicios que apliquen migraciones, y la medición de R-19 es más realista si cada proceso carga sus módulos. Sin servicios, los procesos arrancarían vacíos.

**Alternativas.** Demostrar la regla con un servicio de prueba y dejar los procesos vacíos; pasar el escenario de esquemas a la primera historia con datos (F04-H01).

**Consecuencias.** La primera historia de cada servicio parte de un servicio ya creado y ensamblado en su proceso, en lugar de copiar la plantilla.

---

## ADR-013. Persistencia por servicio y construcción de imágenes desde el código

- **Estado:** Aceptada · **Fecha:** 2026-10-12 · **Decidido por:** autor de la tesis
- **Origen:** plan de B00-H02 (`specs/002-compose-cuatro-procesos/research.md` §1, §3 y §4)
- **Modifica a:** `deploy/docker/java.Dockerfile` y el `cd.yml` pendiente de B00-H08, que suponían un JAR compilado antes de construir la imagen
- **Relacionadas:** ADR-006, ADR-012, D-35; principios V, IX y XI; constitución §3

**Contexto.** B00-H02 levanta el perfil consolidado con un solo `docker compose up` desde un clon limpio, y crea los 12 servicios con su esquema (D-35). Esto obliga a fijar cómo migra cada servicio, con qué permisos se conecta cada proceso y cómo se construyen las imágenes, y esas tres cosas afectan a todas las features que tengan datos y al despliegue.

**Decisión.**
1. **Un Flyway por servicio.** Cada servicio tiene en su capa de infraestructura una configuración `<Servicio>FlywayConfig`, con un bean `Flyway` limitado a su esquema (`schemas=<servicio>`) y a su carpeta `db/migration/<servicio>`, que migra al arrancar el proceso. El nombre de la clase es único en el proyecto.
2. **Un rol de base por proceso.** Hay una base `mdd`, un rol por proceso (identidad, datos, distributivo y salida) y 12 esquemas, cada uno a nombre del rol de su proceso. Cada proceso usa un solo `DataSource` con su rol. Un proceso no puede tocar los esquemas de otro; dentro de un proceso, el aislamiento entre servicios lo verifica la prueba de integración de cada servicio, `<Servicio>EsquemaIntegrationTest`.
3. **Imágenes construidas desde el código.** `java.Dockerfile` compila el módulo en una etapa con JDK 25 y una caché de Gradle compartida, y la imagen final usa el JRE 25. B00-H08 construye con la misma imagen o reutiliza su etapa de extracción.

**Por qué.** Cumple el principio V a nivel de base sin un `DataSource` por servicio (12 *pools* de conexiones irían contra R-19) y con el mínimo privilegio del principio IX. Mantiene la configuración explícita y corta (principio XI). Permite el «un solo comando» de RNF-POR-01.

**Alternativas.** Un rol único para todo; un rol y un `DataSource` por servicio; un Flyway por proceso con varios esquemas; compilar los JAR antes de `docker compose up`.

**Consecuencias.** Las historias que agreguen tablas escriben `V2__…` y siguientes en la carpeta de su servicio. B00-H08 adapta `cd.yml` a la nueva etapa de compilación.

---

## D-36. El despliegue pasa a un servidor o PC de 4 núcleos y 8 GiB de RAM

- **Estado:** Aceptada · **Fecha:** 2026-10-12 · **Decidido por:** autor de la tesis
- **Origen:** análisis de B00-H02 (`/speckit-analyze`, hallazgo I2: la especificación decía «4 GB» y los contratos «4 GiB»). Al revisarlo, el autor decidió duplicar las limitaciones del hardware de despliegue.
- **Modifica a:** constitución, principio V, §3 (Infraestructura y Memoria) y §8 (versión 2.1.0); RNF-POR-02 y R-19 de la ERS; ADR-006 y ADR-008 (tamaño de la VM); issue #19 (historia y escenario de memoria); issue #20 (escenario de memoria de B00-H03); especificación, plan y tareas de B00-H02

**Decisión.**
1. El perfil consolidado se despliega en un **servidor o PC de 4 núcleos y 8 GiB de RAM**, en lugar de una VM de 2 vCPU y 4 GB. La demostración usa un equipo del mismo tamaño.
2. **R-19:** el perfil consolidado debe consumir menos de 8 GiB después del calentamiento (D-34). La alerta pasa de 3,5 GiB a **7 GiB**. Se mide en GiB porque es la unidad de `docker stats`.
3. **Los límites por contenedor no cambian:** PostgreSQL y cada proceso, 512 MiB; el gateway, 384 MiB. En total son 2 944 MiB. El resto de la memoria queda para el sistema operativo y para lo que llegue en las olas siguientes. Si una medición lo pide, los límites se suben sin una decisión nueva, mientras el total quede por debajo del umbral de R-19.
4. **Se mantienen los 4 procesos de ADR-006:** el principio V pide no distribuir sin una razón técnica, y con 12 JVM la medición de R-19 tendría mucho menos margen.

**Por qué.** Lo decidió el autor: el equipo de despliegue tendrá 8 GB de RAM y 4 núcleos.

**Alternativas.** Mantener 4 GB y medir en GiB (propuesta del análisis); mantener 4 GB con el umbral más estricto de 4 000 MB; duplicar también los límites por contenedor (se aplicó un momento y se revirtió al aclarar el autor que se refería al equipo).

**Consecuencias.**
- La constitución pasa de 2.0.1 a 2.1.0. Se sube la versión menor porque cambia una restricción de la sección 3: no es una aclaración, aunque no cambie la regla de ningún principio.
- La alternativa de R-19 (Nginx en lugar de Spring Cloud Gateway) queda para un consumo de 8 GiB o más.
- La ERS (RNF-POR-02, R-19), ADR-006 y ADR-008, fuera del repositorio, quedan desactualizados en este punto; este registro prevalece.
- El runner estándar de GitHub Actions de un repositorio privado tiene menos RAM que el equipo de despliegue. La medición de referencia sigue siendo válida mientras el consumo quede por debajo de la RAM del runner.
- El issue #19 se editó el 2026-10-12. El issue #20 se editó el 2026-10-13.

---

## D-37. B00-H03 incluye un inicio de sesión mínimo para usuarios de prueba

- **Estado:** Aceptada · **Fecha:** 2026-10-13 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q1 de la especificación de B00-H03 (`specs/003-autenticacion-jwt/spec.md`)
- **Modifica a:** alcance del issue #20 (B00-H03) y del issue #43 (F05-H01, Iniciar sesión)

**Decisión.** Usuarios expone en B00-H03 un inicio de sesión mínimo: recibe usuario y contraseña de un usuario de prueba ficticio, con la contraseña guardada con Argon2id, y devuelve el token JWT firmado con RS256. La pantalla de inicio de sesión, la redirección, la gestión de usuarios y los mensajes para el usuario final siguen en F05-H01.

**Por qué.** La historia dice que el token lo «emite Usuarios» y su escenario parte de «un usuario de prueba que inició sesión»; D-34 pide medir R-19 después de un inicio de sesión real. F05-H01 depende de B00-H03 y es de la ola 1.

**Alternativas.** Generar tokens solo en las pruebas, con una llave de prueba (Usuarios no emitiría nada y la medición no tendría inicio de sesión); adelantar todo F05-H01 a la ola 0.

**Consecuencias.** F05-H01 parte de un inicio de sesión que ya emite tokens y agrega la pantalla, los usuarios reales de la demo y los mensajes.

---

## D-38. El 401 de B00-H03 usa el modelo de error del principio XIII antes de B00-H04

- **Estado:** Aceptada · **Fecha:** 2026-10-13 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q2 de la especificación de B00-H03
- **Modifica a:** alcance del issue #20 (B00-H03) y del issue #21 (B00-H04, que recibe los códigos ya creados)

**Decisión.** B00-H03 responde los 401 con el formato del principio XIII (`titulo`, `descripcion` y `codigo`) y registra los primeros códigos del catálogo, como el de «sin sesión o token inválido». B00-H04 arma después el catálogo completo y el `@RestControllerAdvice` de cada aplicación, y absorbe esos códigos sin cambiarlos.

**Por qué.** El escenario «Petición sin token» pide el modelo de error común, que ya está fijado en la constitución. El 401 lo produce la capa de seguridad antes de llegar a un controlador, así que no depende del advice. Se mantiene el orden de las historias de la ola 0.

**Alternativas.** Hacer primero B00-H04 y que B00-H03 dependa de ella; un formato provisional que B00-H04 corrija (va contra el escenario).

**Consecuencias.** Los códigos son estables desde B00-H03: B00-H04 no puede renombrarlos.

---

## D-39. «Sesión actual» en un servicio de cada proceso para la prueba de punta a punta

- **Estado:** Aceptada · **Fecha:** 2026-10-13 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q3 de la especificación de B00-H03
- **Modifica a:** alcance del issue #20 (B00-H03)

**Decisión.** Un servicio de cada uno de los 4 procesos ofrece la consulta «sesión actual», que solo responde a peticiones autenticadas y devuelve el usuario y los roles obtenidos del token. Cada una tiene su contrato OpenAPI mínimo, escrito antes de implementarla (principio IV). En identidad, el servicio es Usuarios; el plan elige el de datos, distributivo y salida.

**Por qué.** El escenario «Token válido» pide llamar a «un servicio de cualquier proceso», y los procesos todavía no tienen endpoints de negocio. Así la prueba recorre el API Gateway y los 4 procesos.

**Alternativas.** Solo Usuarios con endpoint y pruebas internas en los demás procesos; un endpoint técnico sin contrato (va contra el principio IV).

**Consecuencias.** Hay 4 contratos pequeños desde la ola 0. Las historias de cada servicio pueden reutilizar o retirar su «sesión actual».

---

## ADR-014. Autenticación con JWT RS256 emitido por Usuarios

- **Estado:** Aceptada · **Fecha:** 2026-10-13 · **Decidido por:** autor de la tesis
- **Origen:** plan de B00-H03 (`specs/003-autenticacion-jwt/research.md` §1 a §6)
- **Modifica a:** ADR-008, que fija JWT RS256 y Argon2id sin el detalle de llaves y validación
- **Relacionadas:** D-37, D-38, D-39; principios V, IX, XI y XIII; constitución §3

**Contexto.** Todas las historias con endpoints parten de un usuario autenticado. Hay que fijar qué lleva el token, quién lo firma, cómo validan el API Gateway y los procesos, y cómo se responde cuando falla, de forma que sirva igual en local, en la CI y en el VPS.

**Decisión.**
1. **Token:** JWT RS256 con `iss` (`mdd-usuarios`), `sub` (nombre de usuario), `roles`, `iat` y `exp` (30 min por defecto), y `kid` en el encabezado.
2. **Llaves:** solo Usuarios tiene la llave privada (RSA de 2048 bits). Viene de un archivo PEM configurado (`mdd.usuarios.llave-privada`) o, si no hay, es efímera y se genera al arrancar. La pública se publica como JWKS en `GET /api/usuarios/jwks`.
3. **Doble validación:** el API Gateway valida toda `/api/**` y reenvía el token; cada proceso lo vuelve a validar (solo RS256, firma por `kid`, emisor y vencimiento) con el JWKS en caché.
4. **401 uniforme:** estado, `WWW-Authenticate: Bearer` sin detalles y el cuerpo del principio XIII con `SEGURIDAD_NO_AUTENTICADO`, idénticos para todas las causas. La causa solo va al registro, en DEBUG y sin el token.
5. **`plataforma/seguridad`:** la configuración servlet del servidor de recursos y el 401, compartidos por los 4 procesos; el API Gateway tiene su versión reactiva.

**Por qué.** La llave que firma no sale de un proceso ni entra al repositorio (principio IX), y el «un solo comando» sigue funcionando sin generar archivos. Un proceso no depende del API Gateway para su seguridad, y validar no cuesta una llamada de red por petición. El 401 uniforme cumple el escenario «Token alterado o vencido».

**Alternativas.** HS256 con un secreto compartido; token opaco con introspección; llave pública repartida a mano o par de llaves en volúmenes; configuración copiada en cada proceso.

**Consecuencias.** Con la llave efímera, reiniciar identidad invalida los tokens (en local basta con volver a iniciar sesión). B00-H08 configura la llave persistente del VPS. F05-H01 y F05-H02 construyen sobre el inicio de sesión y los roles del token.

---

## ADR-015. Contratos OpenAPI en `contratos/` y generación por servicio

- **Estado:** Aceptada · **Fecha:** 2026-10-13 · **Decidido por:** autor de la tesis
- **Origen:** plan de B00-H03 (`specs/003-autenticacion-jwt/research.md` §11)
- **Modifica a:** ADR-013 punto 3, que construía las imágenes con `backend/` como contexto
- **Relacionadas:** principios IV, V y XIII; ADR-008, ADR-012; constitución §3

**Contexto.** B00-H03 trae los primeros endpoints. El principio IV pide contratos OpenAPI 3.0.3 en `contratos/` antes del código, interfaces generadas y pruebas de contrato, y eso hay que organizarlo para todas las historias con API y para la SPA.

**Decisión.**
1. **Organización:** `contratos/comun/` (esquema `Error`, catálogo de códigos y piezas de seguridad) y un `contratos/<servicio>/openapi.yaml` por servicio con API, que referencia a `comun/`.
2. **Generación:** el plugin de convención `mdd.contrato` aplica openapi-generator 7.25.0 (generador `spring`, `interfaceOnly`, `useSpringBoot4`, `useJackson3`, sin validación de beans hasta B00-H04). Las interfaces y los modelos van a `ec.mdd.<servicio>.adaptadores.entrada.api`, en `build/generated`, fuera de Spotless, SpotBugs y JaCoCo.
3. **Pruebas de contrato:** el núcleo de openapi-request-validator 3.0.0 (antes swagger-request-validator), con un adaptador propio para MockMvc en `plataforma/pruebas`, un módulo solo para pruebas, porque su módulo MockMvc no funciona con Spring 7.
4. **Imágenes:** el contexto de construcción pasa a la raíz del repositorio, con un `.dockerignore` que solo deja `backend/` y `contratos/`.

**Por qué.** Cumple el principio IV con un contrato por módulo. Cada servicio genera sus propios modelos (principio V), y la SPA de B00-H06 lee los mismos archivos.

**Alternativas.** Controladores sin generación; un solo contrato para todo el sistema; `contratos/` dentro de `backend/`; el módulo MockMvc del validador (incompatible).

**Consecuencias.** Cada historia con API escribe o cambia su `openapi.yaml` antes del código. B00-H04 completa `comun/` (catálogo y validación de entrada) y B00-H08 adapta `cd.yml` al nuevo contexto de construcción.

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
| ADR-011 | Plataforma Java 25 con Spring Boot 4.1.1 y Gradle, y pipeline de DevSecOps | Aceptada, modificada por ADR-012 (punto 2), D-28, D-32 (punto 3), D-33 (formateador de Spotless) y ADR-013 (construcción de imágenes) |
