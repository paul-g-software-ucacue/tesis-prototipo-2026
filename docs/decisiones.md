# Registro de decisiones del MDD

Este archivo es la **fuente única** de las decisiones que se toman durante el desarrollo, desde D-28 y ADR-012. Vive en el repositorio para que las especificaciones, los planes y la constitución no dependan de documentos externos (la matriz SCD en `lean_inception/Matriz_decisiones_SCD_MDD.xlsx` y los ADR de `lean_inception/adr/`).

## Cómo se usa

- **Decisión (D-NN):** una elección que cambia el alcance, una regla del proyecto o un documento ya aprobado (constitución, sección 7). Se registra **antes** de aplicarla.
- **ADR-NNN:** una decisión de arquitectura que afecta a más de una feature (constitución, sección 6, paso 4). Se registra **antes** de implementarla.
- Los números no se reutilizan. Una decisión que reemplaza a otra la cita en «Modifica a» y la otra cambia su estado.
- Cada entrada dice quién decidió, de dónde salió, qué cambia y qué documentos hay que poner al día.
- **Siguiente número libre:** D-79 · ADR-024.

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
| D-40 | 2026-10-13 | El API Gateway responde sus propios errores con el modelo común | Aceptada | Alcance del issue #21 |
| D-41 | 2026-10-13 | El escenario «Error reconocido» se demuestra con un error de dominio de prueba y con credenciales inválidas | Aceptada | Alcance del issue #21 |
| D-42 | 2026-10-13 | El inicio de sesión con datos vacíos responde 400 con detalles | Aceptada | B00-H03 (research §5), contrato de Usuarios |
| ADR-016 | 2026-10-13 | Traducción de errores con catálogo por servicio | Aceptada | ADR-009 (detalle de la traducción de errores) |
| D-43 | 2026-10-13 | Si FirmaEC no acepta la CA de prueba, el spike sigue con el certificado real solo en local | Aceptada | Alcance del issue #26 |
| D-44 | 2026-10-13 | La validación del spike queda como prueba automática del servicio Firma | Aceptada | Alcance del issue #26 |
| D-45 | 2026-10-13 | El spike B00-S01 dura 2 días de trabajo | Aceptada | Alcance del issue #26 |
| D-46 | 2026-10-13 | Comprobación de la sección 8: PDFBox y Bouncy Castle validan las firmas de FirmaEC | Aceptada | Constitución, sección 8 (2.1.0 → 2.1.1) |
| ADR-017 | 2026-10-13 | Firmantes y confianza por entorno: un solo firmante real en las pruebas del autor | Aceptada | D-23 (detalle de la configuración); ADR-004 (árbol de firmas, sin cambios en producción) |
| D-47 | 2026-10-14 | Los eventos persistentes se demuestran con un receptor de prueba en Usuarios | Aceptada | Alcance del issue #22 |
| D-48 | 2026-10-14 | El evento de prueba se publica con un endpoint de prueba apagado por defecto y un script de punta a punta | Aceptada | Alcance del issue #22 |
| D-49 | 2026-10-14 | Las entregas entre procesos se autentican con un token de proceso que emite Usuarios | Aceptada | ADR-014 (agrega el token de proceso), alcance del issue #22 |
| ADR-018 | 2026-10-14 | Eventos entre procesos: registro por proceso, sobre común, rutas internas y token de proceso | Aceptada | ADR-014 (agrega el token de proceso), ADR-009 (detalle de los fallos entre procesos) |
| D-50 | 2026-10-15 | La SPA base protege las rutas internas | Aceptada | Alcance de los issues #23 (B00-H06) y #43 (F05-H01) |
| D-51 | 2026-10-15 | La SPA guarda el token solo en memoria | Aceptada | Alcance del issue #23 |
| D-52 | 2026-10-15 | La SPA entra al perfil consolidado en B00-H06 | Aceptada | Alcance de los issues #23 y #25 (B00-H08) |
| D-53 | 2026-10-15 | Node 24 LTS en la CI y en la imagen; se cierra el PR #73 | Aceptada | PR #73 de Dependabot, `.github/dependabot.yml` |
| ADR-019 | 2026-10-15 | Base del frontend: cliente generado, errores, sesión en memoria y mismo origen | Aceptada | ADR-008 (detalle del frontend) |
| D-54 | 2026-10-15 | Excepciones de Grype solo temporales, con motivo y vencimiento | Aceptada | Constitución principio IX (2.1.2), alcance del issue #24 (B00-H07) |
| D-55 | 2026-10-17 | El despliegue automático va al PC de demostración del autor, por extracción desde Docker Hub | Aceptada | Constitución principio IX y §3 (2.1.3 y 2.1.4), alcance del issue #25 (B00-H08), `cierre-ola.yml` |
| D-56 | 2026-10-17 | Cada despliegue del equipo de demostración se informa a GitHub Deployments | Aceptada | Alcance del issue #25 (B00-H08) |
| D-57 | 2026-10-17 | El usuario de prueba `autor.prueba` se habilita en el equipo de demostración, con clave propia | Aceptada | Alcance de los issues #25 (B00-H08) y #43 (F05-H01), D-37 |
| D-58 | 2026-10-17 | Los archivos del despliegue viajan en una imagen más, el paquete de despliegue | Aceptada | Alcance del issue #25 (B00-H08: «las 6 imágenes») |
| D-59 | 2026-10-17 | Los usuarios de la demo salen de un archivo versionado; sus contraseñas, de cada entorno | Aceptada | Alcance del issue #43 (F05-H01), D-37, D-57 |
| D-60 | 2026-10-17 | F05-H01 incluye RNF-SEG-07: bloqueo tras 5 intentos e inactividad de 30 minutos | Aceptada | Alcance del issue #43, vigencia del token de B00-H03 (ADR-014) |
| D-61 | 2026-10-17 | «Tokens y secretos cifrados en reposo» (RNF-SEG-02) se cumple por diseño | Aceptada | — |
| D-62 | 2026-10-17 | Un cambio de permisos vale para una sesión abierta en su próxima renovación | Aceptada | ADR-014 (los permisos viajan con la sesión), ADR-020 |
| D-63 | 2026-10-17 | Los 10 roles de D-16 son fijos: se asignan, pero no se editan ni se borran | Aceptada | D-16 |
| D-64 | 2026-10-17 | F05-H02 marca qué permisos son «solo lo propio»; el filtro lo aplica cada historia con esos datos | Aceptada | D-16 |
| D-65 | 2026-10-18 | La matriz D-16 en permisos: 16 funcionalidades y los permisos de los 10 roles | Aceptada | D-16 (interpreta la prosa de la matriz) |
| ADR-021 | 2026-10-18 | Permisos efectivos en el token y cada operación declara su permiso en el contrato | Aceptada | ADR-014 (claim `permisos`), ADR-015 (`x-mdd-permiso`), ADR-016 (403 común) |
| D-66 | 2026-10-18 | El Vicerrector académico lee «Parámetros y normativa» | Aceptada | D-65 (un permiso más en la fila del Vicerrector) |
| D-67 | 2026-10-18 | El Vicerrector académico escribe «Parámetros y normativa» | Aceptada | D-65, D-66 (la escritura además de la lectura) |
| D-68 | 2026-10-20 | Las reglas del motor y la afinidad por defecto completan las versiones existentes por migración | Aceptada | — (excepción acotada al principio VII para V4 de Normativa) |
| D-69 | 2026-10-21 | Los periodos académicos se escriben con «Distributivo del periodo» y se abren solo desde Académico | Aceptada | D-65 (interpreta a qué funcionalidad pertenece el periodo), F04-H02 (quita el diálogo «Asociar a un periodo» de «Normativa») |
| D-70 | 2026-10-21 | Una operación puede declarar varios permisos alternativos; la estructura orgánica la leen quienes leen el distributivo o los catálogos | Aceptada | ADR-021 (`x-mdd-permiso` admite una lista), RF-12 (interpreta «sin permiso de administrador») |
| D-71 | 2026-10-23 | El Responsable de consolidación y el Vicerrector académico ejecutan las cargas | Aceptada | D-65 (matriz D-16) |
| D-72 | 2026-10-24 | Cada campo de entrada se valida por su tipo y su largo, y cada error del catálogo dice su clasificación, su causa, su descripción técnica y la acción para el usuario | Aceptada | Constitución 2.2.0 (principio XIV nuevo; amplía el XIII y la definición de terminado) |
| D-73 | 2026-10-25 | Los 16 cargos de gestor tienen código propio, del G06 al G21 | Aceptada | Diccionario de la carga 8 (catálogo CARGOS) |
| ADR-022 | 2026-10-26 | El lenguaje publicado de las cargas y el motor del lote, en `:plataforma:lenguaje-cargas` | Aceptada | MD-13 y ADR-009 (cómo llega el lote al dueño) |
| D-74 | 2026-10-26 | Los mensajes al usuario lo tratan de «tú» | Aceptada | Principio XIII (mensajes consistentes); motivos y pantalla de F01-H03 |
| ADR-023 | 2026-10-30 | Consultas entre procesos por rutas internas con token de proceso | Aceptada | ADR-018 (extiende las rutas internas a las consultas), ADR-010 (precisa cómo se pide la ficha), ADR-009 (fija los errores 502 y 504) |
| D-75 | 2026-10-31 | Las horas disponibles de un docente salen de sus horas contratadas | Aceptada | Glosario «Horas disponibles», RF-25 |
| D-76 | 2026-10-31 | Roles no consulta a Académico al guardar la carrera del coordinador | Aceptada | D-64, F06-H05 |
| D-77 | 2026-10-31 | Las horas semanales admiten hasta dos decimales | Aceptada | RN-18, F03-H03, modelo táctico |
| D-78 | 2026-10-31 | F06-H03 va antes que F07-H02, y mientras tanto una propuesta sin horas suficientes se guarda con un aviso | Aceptada | Issues #48 y #51 |
| ADR-020 | 2026-10-17 | Sesión por inactividad sin estado: token corto que la SPA renueva mientras hay actividad | Aceptada | ADR-014 (claims `nombre` e `inicio`, operación de renovación) |

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

## D-40. El API Gateway responde sus propios errores con el modelo común

- **Estado:** Aceptada · **Fecha:** 2026-10-13 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q1 de la especificación de B00-H04 (`specs/004-modelo-errores/spec.md`)
- **Modifica a:** alcance del issue #21 (B00-H04)

**Decisión.** El API Gateway también responde con el esquema `Error` sus propios errores: ruta inexistente (404), proceso que no responde (503) y fallo técnico (500 con `idIncidente`). Como es WebFlux y no tiene controladores, lo hace con un manejador de errores reactivo y no con un `@RestControllerAdvice`. El 401 por token no cambia (ADR-014).

**Por qué.** La historia pide que «todos los errores lleguen con el mismo formato». Sin esto, la SPA (B00-H06) vería el formato por defecto de Spring Cloud Gateway cuando falla la puerta de entrada, y ese formato incluye la ruta y detalles internos.

**Alternativas.** Dejar el API Gateway para otra historia.

**Consecuencias.** Los procesos detenidos o lentos detrás del API Gateway responden 503 con un código común; los errores entre procesos (502 y 504) se agregan cuando haya llamadas por HTTP entre procesos.

---

## D-41. El escenario «Error reconocido» se demuestra con un error de dominio de prueba y con credenciales inválidas

- **Estado:** Aceptada · **Fecha:** 2026-10-13 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q2 de la especificación de B00-H04
- **Modifica a:** alcance del issue #21 (B00-H04)

**Decisión.** Como los servicios todavía no tienen errores de dominio, el escenario se demuestra con un error de dominio de prueba que se registra en un catálogo de prueba y pasa por el mecanismo real de traducción. Además, las credenciales inválidas de Usuarios, que hoy traduce su controlador, pasan al mecanismo común como primer error real (D-38).

**Por qué.** Así no se inventan reglas de negocio, y el mecanismo queda probado de punta a punta con un error real de aplicación. Es el mismo enfoque de B00-H01, que probó las reglas de capas con *fixtures*.

**Alternativas.** Crear el primer error de dominio real en un servicio (sería inventar un requisito).

**Consecuencias.** La primera historia con reglas de negocio registra sus errores de dominio en el catálogo con el mecanismo ya probado.

---

## D-42. El inicio de sesión con datos vacíos responde 400 con detalles

- **Estado:** Aceptada · **Fecha:** 2026-10-13 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q3 de la especificación de B00-H04
- **Modifica a:** B00-H03 (`specs/003-autenticacion-jwt/research.md` §5: datos vacíos → 401) y el contrato de Usuarios (agrega la respuesta 400)

**Decisión.** Al activar la validación de entrada, un inicio de sesión con usuario o contraseña vacíos responde 400 con `detalles`, como cualquier otro campo obligatorio vacío. Un usuario inexistente, inactivo o una contraseña incorrecta siguen respondiendo el mismo 401 `USUARIOS_CREDENCIALES_INVALIDAS`.

**Por qué.** Es coherente con el escenario «Error de entrada», y decir que un campo vino vacío no revela qué usuarios existen ni qué dato falló entre usuario y contraseña.

**Alternativas.** Mantener el 401 como excepción a la validación de entrada.

**Consecuencias.** El research de B00-H03 queda modificado en ese punto; las pruebas de contrato de Usuarios cubren el 400.

---

## ADR-016. Traducción de errores con catálogo por servicio

- **Estado:** Aceptada · **Fecha:** 2026-10-13 · **Decidido por:** autor de la tesis
- **Origen:** plan de B00-H04 (`specs/004-modelo-errores/research.md`)
- **Modifica a:** ADR-009, que fija el modelo de errores sin el detalle de cómo se traduce
- **Relacionadas:** D-38, D-40 a D-42, ADR-014, ADR-015; principios I, IV, V, IX y XIII

**Contexto.** El principio XIII fija el esquema `Error`, la clasificación, los estados y lo que nunca se expone. Falta decidir cómo se traduce cada error en las 4 aplicaciones y en el API Gateway sin que el dominio conozca HTTP, y cómo se mantiene el catálogo, para todas las historias con errores de negocio.

**Decisión.**
1. **Un solo `@RestControllerAdvice` por proceso**, en `plataforma:errores`, que extiende `ResponseEntityExceptionHandler` (errores del framework con el modelo común), más una página `/error` propia. No captura las excepciones de seguridad.
2. **Catálogo por servicio:** cada servicio registra sus excepciones de dominio y de aplicación en un `CatalogoDeErrores` de sus adaptadores de entrada (excepción, código, estado, título y descripción). El dominio y la aplicación siguen siendo Java sin HTTP. Un código o una excepción repetidos impiden arrancar.
3. **`contratos/comun/errores.md` es el catálogo normativo.** `VerificadorDeCatalogo` comprueba en las pruebas que cada entrada del código coincide con su fila.
4. **Códigos comunes con el prefijo `COMUN_`** para los errores técnicos, de entrada y del framework.
5. **Error técnico:** 500 `COMUN_ERROR_INTERNO` con un `idIncidente` aleatorio (UUID v4). El registro guarda la traza saneada (sin JWT, `Bearer`, `clave=` ni `password=`) con el mismo `idIncidente`. Una excepción de dominio o aplicación sin registrar se trata como técnica y se avisa en el registro.
6. **Validación de entrada:** la generación de los contratos valida los cuerpos (`useBeanValidation`), y los errores responden 400 `COMUN_SOLICITUD_INVALIDA` con un `detalle` por campo, con descripciones propias en español.
7. **API Gateway:** un `ErrorWebExceptionHandler` propio (404, 405, 503 sin conexión con el proceso, 500 con `idIncidente`). Lo común vive en `plataforma:errores-nucleo`, sin servlet ni WebFlux, que también usan los servicios para registrar su catálogo.

**Por qué.** Cumple la traducción centralizada del principio XIII con un solo punto por aplicación, deja a cada servicio dueño de sus códigos (principio V), y el catálogo documentado no se desalinea del código. Las respuestas no filtran detalles (RNF-SEG-04) y el registro permite diagnosticar sin guardar secretos.

**Alternativas.** Una superclase o anotación de la plataforma en las excepciones del dominio; un advice por servicio; un catálogo YAML leído al arrancar; registrar las excepciones sin sanear; duplicar el saneo en el API Gateway.

**Consecuencias.** Cada historia con errores de negocio agrega su fila en `errores.md`, su entrada en `Errores<Servicio>` y la respuesta en su contrato. F05-H02 agrega el 403 en la seguridad; los errores entre procesos (502 y 504) llegan con la primera llamada HTTP entre servicios.

---

## D-43. Si FirmaEC no acepta la CA de prueba, el spike sigue con el certificado real solo en local

- **Estado:** Aceptada · **Fecha:** 2026-10-13 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q1 de la especificación de B00-S01 (`specs/005-spike-firmaec/spec.md`)
- **Modifica a:** alcance del issue #26 (B00-S01)

**Decisión.** Si FirmaEC no firma con los certificados de la CA de prueba, se registra el hallazgo y los escenarios «Firma incremental» y «Validación» se hacen con el certificado real del autor, solo en su equipo. Al repositorio no entran el certificado real ni los PDF firmados con él; solo el resultado y los datos públicos de la comprobación (bytes comparados, resultado de cada validación).

**Por qué.** La pregunta del *spike* es si la validación de D-23 funciona con lo que FirmaEC escribe de verdad. Sin firmas reales de FirmaEC no hay respuesta. El principio IX prohíbe el certificado real en el repositorio, no usarlo en el equipo del autor.

**Alternativas.** Cerrar el *spike* con el hallazgo; firmar con las librerías en lugar de FirmaEC (no prueba lo que FirmaEC escribe).

**Consecuencias.** Si se usa este camino, la demo necesita otra salida para los certificados (por ejemplo, otra forma de confiar en la CA de prueba), que se registra como ADR al cerrar el *spike*.

---

## D-44. La validación del spike queda como prueba automática del servicio Firma

- **Estado:** Aceptada · **Fecha:** 2026-10-13 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q2 de la especificación de B00-S01
- **Modifica a:** alcance del issue #26 (B00-S01)

**Decisión.** La validación con PDFBox y Bouncy Castle es una prueba automática del servicio `firma` (servicio 10), en su suite `test`: no necesita Docker y usa la carpeta que ya permite Gitleaks. La CA de prueba, sus certificados y los PDF firmados con ellos viven en `backend/servicios/firma/src/test/resources/ca-prueba/`. El código de producción del servicio no cambia.

**Por qué.** F14 (ola 3) parte de una prueba que ya funciona con firmas reales de FirmaEC, y la CI la sigue corriendo, sin adelantar el diseño del servicio (contratos, puertos y errores).

**Alternativas.** Código desechable fuera del build; un adaptador de producción adelantado.

**Consecuencias.** Las dependencias de PDFBox y Bouncy Castle entran solo en las pruebas del servicio `firma`.

---

## D-45. El spike B00-S01 dura 2 días de trabajo

- **Estado:** Aceptada · **Fecha:** 2026-10-13 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q3 de la especificación de B00-S01
- **Modifica a:** alcance del issue #26 (B00-S01)

**Decisión.** El tiempo fijo del *spike* es de 2 días de trabajo. Al cumplirse, se cierra con lo que se sabe y lo pendiente queda en el resultado.

**Por qué.** Alcanza para la CA, el PDF/A, las dos firmas manuales y la validación, y evita que el *spike* se convierta en desarrollo.

**Alternativas.** 1 día (muy justo si hay que instalar FirmaEC); una semana.

**Consecuencias.** Ninguna sobre otras historias.

---

## D-46. Comprobación de la sección 8: PDFBox y Bouncy Castle validan las firmas de FirmaEC

- **Estado:** Aceptada · **Fecha:** 2026-10-13 · **Decidido por:** autor de la tesis
- **Origen:** resultado del spike B00-S01 (`specs/005-spike-firmaec/evidencia.md`, issue #26)
- **Modifica a:** constitución, sección 8 (enmienda de parche 2.1.0 → 2.1.1)
- **Relacionadas:** D-23, D-43 a D-45, ADR-007, ADR-017

**Decisión.** La comprobación «PDFBox y Bouncy Castle validan un PDF/A firmado con FirmaEC» queda **confirmada**. D-23 se mantiene sin cambios.

**Por qué.** Con FirmaEC 5.3.0 y el certificado real del autor (BCE), por el camino de D-43, se firmó tres veces de forma incremental el PDF/A-2b de prueba. Un validador de prueba hecho con PDFBox 3.0.8 y Bouncy Castle 1.86 comprobó:
- el prefijo de bytes;
- que hay una firma más en cada revisión y que la revisión anterior queda cerrada;
- la integridad sobre cada ByteRange;
- la cadena hasta la raíz del BCE, tomada de un archivo;
- la vigencia.

Con otra raíz, la cadena se rechaza, y los casos negativos (byte alterado, otra raíz, certificado vencido) se detectan.

**Hallazgos que pasan a F14:**
1. FirmaEC solo firma con certificados de entidades acreditadas que conoce: rechaza la CA de prueba con «Entidad Certificadora no reconocida».
2. FirmaEC usa `adbe.pkcs7.detached`, con un `/Contents` de 16 384 bytes y la cadena completa dentro del CMS.
3. Bouncy Castle 1.86 rechaza el relleno de ceros que queda después del CMS en `/Contents`: hay que leer un solo objeto ASN.1.
4. La integridad se verifica con la llave pública. Si se verifica con el certificado, Bouncy Castle también exige que esté vigente y mezcla las dos comprobaciones.
5. veraPDF necesita JAXB 2.3, y el BOM de Spring Boot 4.1 trae la 4.x.

**Alternativas.** Registrar la comprobación como fallida y cambiar D-23, que no hizo falta.

**Consecuencias.**
- La sección 8 de la constitución marca la fila como confirmada.
- El validador y las pruebas de `ec.mdd.firma.spike` quedan como punto de partida de F14 (D-44).
- Cómo firman la demo y las pruebas lo decide ADR-017.

---

## ADR-017. Firmantes y confianza por entorno: un solo firmante real en las pruebas del autor

- **Estado:** Aceptada · **Fecha:** 2026-10-13 · **Decidido por:** autor de la tesis
- **Origen:** resultado del spike B00-S01 (escenario 1, issue #26)
- **Modifica a:** D-23 y ADR-004 solo en el detalle de su configuración; las reglas no cambian
- **Relacionadas:** D-19, D-23, D-43, D-46, ADR-004, ADR-007; principios VI y IX; constitución, sección 3

**Contexto.**
- FirmaEC rechaza la CA de prueba, así que los firmantes ficticios de la demo no pueden firmar con FirmaEC.
- El autor tiene un solo certificado real, y el flujo pide tres roles de firma (F1, F2 y F3).
- Una prueba automática de punta a punta no puede manejar FirmaEC, que es una aplicación de escritorio.

Hay que poder probar el flujo completo de creación del distributivo sin debilitar la validación de D-23.

**Decisión.**
1. **La validación de D-23 no cambia en ningún entorno:** prefijo de bytes, una sola firma nueva, orden, rol, integridad, vigencia, cadena contra raíces en un archivo e identidad por el CN. No se agrega ningún interruptor para apagarla.
2. **Una misma persona puede ocupar F1, F2 y F3.** Ni D-19, ni D-23, ni la constitución exigen personas distintas. Exigen los roles, el orden y que el CN del certificado coincida con el usuario asignado al rol. En la instalación de pruebas del autor, el árbol de firmas asigna los tres roles al autor, y él firma tres veces con su certificado real con FirmaEC. El spike comprobó que FirmaEC firma tres veces seguidas con el mismo certificado y que la validación acepta las tres firmas.
3. **Esa asignación no se versiona.** El CN real es un dato personal (principio IX), así que los usuarios reales y su asignación a los roles se cargan desde un archivo local fuera del repositorio, como los secretos. En el repositorio solo hay usuarios y certificados ficticios.
4. **La confianza depende del entorno**, a través del archivo de raíces de D-23:
   - en la instalación de pruebas del autor y en la demo con FirmaEC, las raíces de las entidades acreditadas que se usen (por ejemplo, la del BCE);
   - en la CI y en las pruebas automáticas de punta a punta, la CA de prueba (`ca-prueba.crt`). En ese caso firma el código de prueba (`FirmadorDePrueba`) con los certificados ficticios F1, F2 y F3, cuyos CN coinciden con los usuarios de prueba;
   - en producción, las raíces de las entidades acreditadas, y el árbol con las personas reales de cada rol.

**Por qué.** Permite probar a mano el flujo completo con un solo certificado real y deja las pruebas automáticas sin depender de FirmaEC, sin ninguna excepción en la validación: lo que cambia por entorno son datos (principio VI). No guarda datos personales en el repositorio (principio IX).

**Alternativas.**
- Un interruptor que apague la validación del firmante en demo y en pruebas: descartado, porque las pruebas dejarían de cubrir la identidad y el interruptor podría quedar encendido en producción.
- Conseguir tres certificados reales de personas distintas: depende de terceros, y las pruebas automáticas seguirían sin poder manejar FirmaEC.
- Darle a la CA de prueba el nombre de una entidad acreditada: descartado, porque sería suplantar a una organización real.

**Consecuencias.**
- F14 lee el archivo de raíces y el árbol de firmas de la configuración de cada entorno, y carga desde un archivo local no versionado los usuarios reales de la instalación del autor.
- Las pruebas de punta a punta firman con `FirmadorDePrueba` y la CA de prueba.
- La guía de instalación de la demo explica cómo asignar los tres roles al certificado del autor.

---

## D-47. Los eventos persistentes se demuestran con un receptor de prueba en Usuarios

- **Estado:** Aceptada · **Fecha:** 2026-10-14 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q1 de la especificación de B00-H05
- **Modifica a:** alcance del issue #22 (B00-H05)

**Decisión.** Los escenarios de B00-H05 se demuestran con un evento de prueba y un receptor de prueba en Usuarios, el servicio dueño de la bitácora, declarado en su contrato (principio IV) y con sus eventos recibidos en el esquema de Usuarios. La publicación, la entrega y el reenvío quedan como mecanismo común, y la recepción idempotente como una forma común documentada (ADR-018, punto 4).

**Por qué.** La bitácora (F17-H01) y las notificaciones (F16-H01) son de la ola 3 y tienen sus propias reglas. Así no se inventan requisitos, y el mecanismo queda probado de punta a punta. Es el mismo enfoque de D-41.

**Alternativas.** Un receptor mínimo real de la bitácora (adelanta F17-H01 y su tabla que no se puede borrar); receptores de prueba en Usuarios y en Notificaciones (demuestra lo mismo dos veces).

**Consecuencias.** F17-H01 y F16-H01 agregan sus receptores reales sobre el mecanismo común y pueden retirar el receptor de prueba.

---

## D-48. El evento de prueba se publica con un endpoint de prueba apagado por defecto y un script de punta a punta

- **Estado:** Aceptada · **Fecha:** 2026-10-14 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q2 de la especificación de B00-H05
- **Modifica a:** alcance del issue #22 (B00-H05)

**Decisión.** Un servicio del proceso distributivo ofrece un endpoint de prueba, declarado en su contrato, que publica el evento de prueba en su transacción. Está apagado por defecto y solo se enciende en el entorno de prueba; apagado, responde como una ruta inexistente. Además de las pruebas automáticas, un script en `deploy/` recorre los tres escenarios con los procesos levantados, deteniendo y volviendo a arrancar el proceso de identidad.

**Por qué.** El escenario «Receptor caído» habla de detener el proceso de identidad, y eso se demuestra mejor con los procesos reales, como en `probar_autenticacion.sh` y `probar_errores.sh`. Apagado por defecto, no queda una ruta de prueba abierta en producción. Es el mismo enfoque de D-39.

**Alternativas.** Solo pruebas automáticas que simulan el proceso de identidad (sin demostración con los procesos reales); un endpoint siempre activo protegido por un rol (ruta de prueba abierta en producción).

**Consecuencias.** El plan elige el servicio de distributivo y la forma de encender el endpoint. B00-H08 no lo enciende en el VPS.

---

## D-49. Las entregas entre procesos se autentican con un token de proceso que emite Usuarios

- **Estado:** Aceptada · **Fecha:** 2026-10-14 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q3 de la especificación de B00-H05
- **Modifica a:** ADR-014, que solo emite tokens de usuarios; alcance del issue #22 (B00-H05)

**Decisión.** Las entregas y los reenvíos de eventos ocurren fuera de la petición de un usuario, así que el proceso que entrega se autentica con un token de proceso que emite Usuarios. Cada proceso tiene su credencial, guardada fuera del repositorio. El token tiene un rol que solo permite entregar eventos al receptor y una vida corta. El receptor valida el token como cualquier otro (ADR-014) y su ruta no pasa por el API Gateway.

**Por qué.** Reutiliza la firma RS256 y la validación de ADR-014 sin un segundo mecanismo de autenticación, y respeta el mínimo privilegio (principio IX): un contenedor cualquiera de la red no puede escribir en la bitácora, y un token de proceso no sirve para otras rutas.

**Alternativas.** Una ruta interna sin token que el API Gateway no expone (confía solo en la red de Docker); un secreto compartido en una cabecera (segundo mecanismo de autenticación).

**Consecuencias.** El plan detalla cómo se emite el token de proceso y cómo se guardan las credenciales, con un ADR si cambia ADR-014 más allá de este punto. B00-H08 configura las credenciales en el VPS.

---

## ADR-018. Eventos entre procesos: registro por proceso, sobre común, rutas internas y token de proceso

- **Estado:** Aceptada · **Fecha:** 2026-10-14 · **Decidido por:** autor de la tesis
- **Origen:** plan de B00-H05 (`specs/006-eventos-persistentes/research.md` §1 a §7)
- **Modifica a:**
  - ADR-014, que solo emite tokens de usuarios: agrega el token de proceso;
  - ADR-009, que fija los fallos entre procesos sin el detalle de cómo se entregan los eventos.
- **Relacionadas:** D-47, D-48, D-49, ADR-006, ADR-013, ADR-016; principios V, IX, X y XIII; constitución §3

**Contexto.**
- La constitución pide que lo que no puede perderse (la bitácora y las notificaciones) se publique como evento en la transacción del servicio.
- Ese evento lo guarda el registro de Spring Modulith, lo entrega un oyente del mismo proceso, una tarea programada lo reenvía si falla, y el receptor lo registra una sola vez por su identificador.
- F16, F17 y cualquier evento futuro van a usar el mismo camino, así que hay que fijar dónde vive el registro, qué viaja, por dónde entra y cómo se autentica una entrega que no tiene un usuario detrás.

**Decisión.**
1. **Registro por proceso.**
   - Usa los módulos `spring-modulith-events-api`, `-events-jdbc` y `-events-jackson`, sin el *starter*.
   - Vive en el esquema `eventos_<proceso>`, con el rol del proceso como dueño. `deploy/postgres/01-esquemas.sh` crea los cuatro.
   - Lo migra el Flyway de `plataforma:eventos`.
   - Las publicaciones completadas se conservan (`completion-mode: update`).
2. **Sobre común.**
   - Un servicio publica un `EventoEntreProcesos` (`idEvento`, `tipo`, `origen`, `momento` y `contenido`) por su puerto de salida.
   - El esquema público está en `contratos/comun/eventos.yaml`.
   - `idEvento` lo genera quien publica, no cambia en los reenvíos y es la clave de idempotencia.
   - El contenido nunca lleva contraseñas ni tokens.
3. **Entrega y reenvío.**
   - Un oyente común (`@ApplicationModuleListener`) entrega por HTTP con plazo de espera. Con 2xx, la publicación queda completada; con cualquier otra cosa, `FAILED`.
   - Una tarea programada reenvía las `FAILED` cada 30 s (configurable), sin máximo de intentos.
   - El monitor de publicaciones atascadas de Spring Modulith pasa a `FAILED` las que quedaron a medias por una caída del emisor.
4. **Recepción idempotente.**
   - Cada servicio receptor tiene `evento_recibido` en su esquema, con `id_evento` como clave primaria.
   - Registra con `INSERT … ON CONFLICT DO NOTHING` en la misma transacción que el efecto del evento.
   - Responde 204 también a un evento repetido. Un tipo sin manejador responde 400 `COMUN_EVENTO_NO_RECONOCIDO`.
5. **Rutas internas.**
   - Las operaciones entre procesos usan `/interno/<servicio>/…`.
   - El API Gateway no las enruta, y su seguridad ya niega todo lo que no es `/api/**`.
6. **Token de proceso.**
   - Usuarios emite, en `POST /interno/usuarios/token-proceso`, un JWT RS256 con la misma llave de ADR-014: `sub` = `proceso:<proceso>`, `roles` = `[PROCESO]` y 5 minutos de vigencia.
   - Lo emite contra la clave de cada proceso, que viene del entorno (`MDD_CLAVE_PROCESO_<PROCESO>`) y se compara en tiempo constante.
   - `plataforma:seguridad` solo admite `PROCESO` en `/interno/**` y lo rechaza (403) en cualquier otra ruta. Un token de usuario en `/interno/**` también responde 403.
   - El emisor guarda el token en memoria. Ante un 401, pide otro y reintenta una sola vez.

**Por qué.**
- Es el mecanismo de la constitución sin colas ni broker (principio X).
- Cada proceso guarda su registro en un esquema propio, como pide ADR-013.
- El receptor es idempotente sin bloqueos.
- El token de proceso reutiliza la firma y la validación de ADR-014 con el mínimo privilegio (principio IX): un contenedor cualquiera de la red no puede escribir en la bitácora, y un token de proceso no sirve para otras rutas.

**Alternativas.**
- El *starter* de Spring Modulith: suma `spring-modulith-core` y ArchUnit en producción.
- El registro en el esquema de un servicio: rompe el principio V.
- Una tabla de salida propia: repite Spring Modulith.
- Reintentos dentro del oyente: no sobreviven a un reinicio.
- Una ruta interna sin token: confía solo en la red.
- Un secreto compartido en una cabecera: es un segundo mecanismo de autenticación.
- OAuth2 *client credentials* con un servidor de autorización: es un servidor nuevo para un solo caso.
- Usuarios técnicos en la tabla de usuarios: mezcla cuentas técnicas con personas.

**Consecuencias.**
- Los volúmenes locales de PostgreSQL se recrean una vez (`docker compose down -v`).
- Cada proceso que publique agrega `plataforma:eventos` y su clave de proceso.
- Cada receptor agrega su tabla `evento_recibido` y su ruta `/interno/<servicio>/eventos`.
- F17-H01 y F16-H01 agregan sus tipos de evento y sus receptores sobre este mecanismo. Pueden retirar el receptor y el evento de prueba (D-47).
- B00-H08 configura las claves de proceso en el VPS y no enciende el endpoint de prueba.
- Las publicaciones completadas crecen sin purga; si hace falta, una historia futura la agrega.

---

## D-50. La SPA base protege las rutas internas

- **Estado:** Aceptada · **Fecha:** 2026-10-15 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q1 de la especificación de B00-H06
- **Modifica a:** alcance de los issues #23 (B00-H06) y #43 (F05-H01)

**Decisión.** La base de la SPA protege las rutas internas: quien abre una sin sesión, o recibe un 401 durante la navegación, vuelve al inicio de sesión, y después de iniciarla, a la ruta que pidió. F05-H01 comprueba su escenario «Acceso sin sesión» sobre esta protección y agrega lo suyo (usuarios reales y permisos por rol).

**Por qué.** Las pantallas de F01-H01, F03-H01 y las siguientes nacen protegidas, sin que cada historia repita la protección. Es parte de la navegación común que pide la historia.

**Alternativas.** Dejar la protección para F05-H01: las pantallas de las historias anteriores quedarían abiertas hasta entonces.

**Consecuencias.** F05-H01 no construye la redirección; la verifica y agrega los permisos.

---

## D-51. La SPA guarda el token solo en memoria

- **Estado:** Aceptada · **Fecha:** 2026-10-15 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q2 de la especificación de B00-H06
- **Modifica a:** alcance del issue #23 (B00-H06); detalla ADR-014 en el frontend

**Decisión.** La SPA guarda el token del usuario solo en memoria. No lo escribe en `localStorage`, `sessionStorage`, cookies ni en la URL. Al recargar la página, el usuario vuelve a iniciar sesión.

**Por qué.** Es lo que recomienda OWASP para un token *bearer*: no queda en el navegador para un ataque XSS persistente ni para otra pestaña. El token dura 30 minutos (ADR-014), así que volver a iniciar sesión tras una recarga cuesta poco.

**Alternativas.** `sessionStorage` (sobrevive a una recarga, pero un XSS puede leerlo); una cookie `HttpOnly` (cambia el backend y ADR-014, y pide protección CSRF).

**Consecuencias.** Una recarga o una pestaña nueva piden iniciar sesión. Si hace falta renovar el token sin volver a iniciar sesión, lo decide una historia futura.

---

## D-52. La SPA entra al perfil consolidado en B00-H06

- **Estado:** Aceptada · **Fecha:** 2026-10-15 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q3 de la especificación de B00-H06
- **Modifica a:** alcance de los issues #23 (B00-H06) y #25 (B00-H08, que solo publica la imagen)

**Decisión.** La SPA entra a `compose.yaml` como un contenedor más, con Nginx sirviendo la SPA y enviando `/api` al API Gateway en el mismo origen. Queda accesible en local con el mismo comando que levanta el perfil, y su memoria entra en la medición de R-19.

**Por qué.** Así la SPA se prueba de punta a punta con el perfil real desde la ola 0, y B00-H08 solo publica y despliega su imagen, igual que las de los procesos.

**Alternativas.** Construir solo la imagen en la CI y usar el servidor de desarrollo en local, con la SPA en el perfil desde B00-H08.

**Consecuencias.** El perfil pasa a 7 contenedores. El API Gateway sigue siendo el único destino de `/api`; en el VPS, Caddy hace la misma división (`deploy/Caddyfile`).

---

## D-53. Node 24 LTS en la CI y en la imagen; se cierra el PR #73

- **Estado:** Aceptada · **Fecha:** 2026-10-15 · **Decidido por:** autor de la tesis
- **Origen:** plan de B00-H06 (`specs/007-spa-base/research.md` §12)
- **Modifica a:** el PR #73 de Dependabot (Node 24 → 26 en `deploy/docker/frontend.Dockerfile`) y `.github/dependabot.yml`

**Decisión.** El frontend usa Node 24, la LTS activa, en la CI (`NODE_VERSION`), en `frontend.Dockerfile` y en `frontend/.nvmrc`. El PR #73 se cierra con un comentario. En `.github/dependabot.yml`, el ecosistema `docker` ignora los cambios de versión mayor de `node`; el paso a la próxima LTS se hace a mano, en la CI y en la imagen a la vez.

**Por qué.** Node 26 todavía es «Current», no LTS. La CI y la imagen deben usar la misma versión, y fusionar el #73 dejaría Node 26 en la imagen y Node 24 en la CI. Vite 8, Vitest 5 y React Router 8 funcionan con Node 24.

**Alternativas.** Fusionar el #73 y subir la CI a Node 26 (sale de LTS); dejar el #73 abierto (Dependabot lo seguiría reabriendo).

**Consecuencias.** Las versiones menores y de parche de Node 24 siguen llegando por Dependabot.

---

## ADR-019. Base del frontend: cliente generado, errores, sesión en memoria y mismo origen

- **Estado:** Aceptada · **Fecha:** 2026-10-15 · **Decidido por:** autor de la tesis
- **Origen:** plan de B00-H06 (`specs/007-spa-base/research.md` §2 a §11)
- **Modifica a:** ADR-008, que fija el stack del frontend sin el detalle de su base
- **Relacionadas:** D-29, D-31, D-50, D-51, D-52, ADR-014, ADR-015, ADR-016; principios IV, VIII, IX y XIII; constitución §3

**Contexto.** Todas las historias con pantallas parten de la misma SPA. Hay que fijar cómo llama a la API, cómo muestra los errores, dónde guarda la sesión, por dónde llega al backend y cómo se prueba la accesibilidad, para que cada pantalla no lo resuelva a su manera.

**Decisión.**
1. **Estructura:** `frontend/` con `src/api` (cliente), `src/sesion`, `src/errores`, `src/navegacion` y `src/pantallas`. Cada historia agrega sus pantallas y sus entradas del menú.
2. **Cliente generado:**
   - los tipos salen de `contratos/<servicio>/openapi.yaml` con openapi-typescript, antes de cada compilación, y no se versionan;
   - el cliente es openapi-fetch con esos tipos, sin código generado;
   - `npm run verificar:ruptura` demuestra en cada pull request que un cambio de contrato rompe la compilación.
3. **Errores:**
   - un solo traductor muestra `titulo` y `descripcion`, los `detalles` junto a su campo y el `idIncidente`, y nunca el `codigo`, el estado HTTP ni texto técnico;
   - sin el esquema `Error`, o sin conexión, usa los textos de `COMUN_ERROR_INTERNO` y `COMUN_SERVICIO_NO_DISPONIBLE` del catálogo.
4. **Sesión:**
   - el token vive solo en memoria y una regla de ESLint prohíbe `localStorage`, `sessionStorage` y `document.cookie`;
   - las rutas internas están protegidas, con vuelta al destino solo si es una ruta interna;
   - un 401 o el vencimiento cierran la sesión.
5. **Mismo origen:** la SPA llama a `/api` en su mismo origen. Nginx (en el perfil) o Caddy (en el VPS) envían `/api` al API Gateway. Nginx agrega una CSP sin `unsafe-inline` en los scripts.
6. **Pruebas:**
   - Vitest con Testing Library (60 % de líneas);
   - Playwright con axe-core en Chromium, con las reglas WCAG 2.1 AA, para las pantallas principales;
   - ESLint con jsx-a11y.

**Por qué.**
- Repite en el frontend lo que ya hace el backend: contrato primero (ADR-015) y errores traducidos en un solo lugar (ADR-016).
- Sigue lo que recomienda OWASP para el token (D-51).
- Con el mismo origen no hace falta CORS.
- axe-core en un navegador real cubre el contraste, que jsdom no calcula.

**Alternativas.**
- openapi-generator `typescript-fetch`, que genera código de runtime.
- Traducir los errores en cada pantalla.
- El token en `sessionStorage` o en una cookie (D-51).
- CORS entre la SPA y el API Gateway.
- axe-core solo en jsdom.

**Consecuencias.**
- Cada historia con pantallas agrega su contrato a `generar:api`, sus rutas protegidas y su prueba de accesibilidad con Playwright.
- F05-H01 construye sobre la sesión y las rutas protegidas (D-50).
- B00-H08 publica la imagen de Nginx y Caddy mantiene la división de `/api`.

---

## D-54. Excepciones de Grype solo temporales, con motivo y vencimiento

- **Estado:** Aceptada · **Fecha:** 2026-10-15 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q1 de la especificación de B00-H07
- **Modifica a:** principio IX de la constitución (2.1.1 → 2.1.2: la entrega admite la excepción temporal); alcance del issue #24 (B00-H07)

**Decisión.** Si Grype encuentra una vulnerabilidad alta o crítica con versión corregida que todavía no se puede aplicar (por ejemplo, el arreglo exige una versión mayor que rompe el proyecto), se acepta una excepción temporal. Cada excepción es una entrada versionada en la configuración de Grype del repositorio, con el identificador de la vulnerabilidad, el paquete, el motivo y la fecha en que vence, como máximo 30 días después de agregarla. La entrada se agrega en un pull request que la explica. Al vencer, el check `4 · sca` vuelve a fallar por esa vulnerabilidad.

**Por qué.** Sin excepciones, una sola dependencia sin arreglo aplicable bloquearía todas las historias. Sin vencimiento, una excepción quedaría olvidada.

**Alternativas.** Excepción sin vencimiento, revisada al cerrar cada ola; ninguna excepción, con el check fallando hasta corregir la dependencia.

**Consecuencias.** El plan de B00-H07 define dónde viven las excepciones y cómo se comprueba el vencimiento. Renovar una excepción vencida es un pull request nuevo con su motivo.

---

## D-55. El despliegue automático va al PC de demostración del autor, por extracción desde Docker Hub

- **Estado:** Aceptada · **Fecha:** 2026-10-17 · **Decidido por:** autor de la tesis
- **Origen:** preparación de B00-H08 (issue #25): el crédito de DigitalOcean del GitHub Student Developer Pack, con el que se iba a pagar el VPS, terminó el 31/07/2026
- **Modifica a:** principio IX de la constitución, *Operación*, y la fila «Repositorio y CI/CD» de §3 (2.1.2 → 2.1.3); la fila «Infraestructura» de §3, para aclarar que el equipo de demostración puede usar otra distribución (2.1.3 → 2.1.4, análisis de B00-H08); alcance del issue #25 (B00-H08); `cierre-ola.yml`, que suponía una URL pública

**Decisión.**
1. **Destino.** El despliegue automático va al PC del autor, que hace de equipo de demostración. La constitución ya decía que «la demostración corre en un equipo del autor del mismo tamaño» (§3). El PC tiene más recursos que los 4 núcleos y 8 GiB de D-36, pero los límites de memoria por contenedor (R-19) se mantienen, y el presupuesto de la demostración sigue siendo el de D-36.
2. **Despliegue por extracción.** La CI no entra al equipo. Publica las imágenes en Docker Hub (etapa 5, sin cambios) y un servicio del propio equipo consulta Docker Hub cada pocos minutos; si hay una versión nueva de `main`, corre `deploy/desplegar.sh`, con *healthchecks* y vuelta atrás. El equipo baja las imágenes con un token de solo lectura.
3. **Acceso restringido.** Caddy sigue pidiendo usuario y contraseña antes de mostrar el MDD. El acceso desde fuera de la red del autor es opcional: si hace falta, un túnel de salida (por ejemplo, Cloudflare Tunnel) lo publica por HTTPS sin abrir puertos.
4. **Cierre de ola.** ZAP y k6 dejan de apuntar a una URL pública: corren contra el sistema que levanta el propio runner, como `perfil-consolidado`. El plan de B00-H08 detalla cómo.
5. **Portabilidad.** Nada del despliegue depende del PC: el mismo `compose.demo.yml`, `desplegar.sh` y el servicio de extracción funcionan igual en un VPS o en la VM del IST, si más adelante hay uno.

**Por qué.** Sin el crédito, un VPS de 4 vCPU y 8 GiB cuesta unos 6 a 7 € al mes (Hetzner CX33), y las opciones gratuitas no alcanzan: Oracle Always Free redujo su cupo ARM a 2 OCPU y 12 GB y exige imágenes multiarquitectura, y el crédito de Azure for Students se agota en pocos meses con una VM de ese tamaño. El destino real del sistema es la VM del IST, a la que el pipeline no despliega (§3); el VPS era una comodidad para la demostración. La extracción evita dar a GitHub acceso de red o SSH a un equipo personal.

**Alternativas.** Un VPS de pago (Hetzner CX33): no cambia ninguna decisión, pero tiene un costo mensual. Oracle Always Free: cupo menor que D-36 y arquitectura ARM. Despliegue por SSH desde la CI al PC: obliga a exponer el equipo o a usar un runner propio.

**Consecuencias.**
- La demostración está disponible mientras el PC del autor está encendido.
- El entorno de GitHub del despliegue solo guarda las credenciales de publicación en Docker Hub; no hay llave SSH ni dirección del equipo en GitHub.
- B00-H08 ajusta sus escenarios: «Despliegue sano» y «Vuelta atrás» se cumplen en el PC; «Acceso restringido», con Caddy y, si se usa, con el túnel.
- `cierre-ola.yml` deja de depender de `vars.DOMINIO`.
- El PC del autor usa Fedora con SELinux; el despliegue no debe depender de la distribución, porque la VM del IST usa Ubuntu Server (§3).

---

## D-56. Cada despliegue del equipo de demostración se informa a GitHub Deployments

- **Estado:** Aceptada · **Fecha:** 2026-10-17 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q1 de la especificación de B00-H08 (`specs/009-publicacion-despliegue/spec.md`)
- **Modifica a:** alcance del issue #25 (B00-H08); completa D-55 (la CI ya no ve el resultado del despliegue)

**Decisión.** El servicio de despliegue del equipo de demostración informa cada intento a GitHub como un *deployment* del entorno del despliegue, con el commit, el estado (en curso, sano o fallido) y un resumen (vuelta atrás, error de Docker Hub, etc.). Usa un token de GitHub de grano fino, guardado solo en el equipo, con permiso únicamente sobre los *deployments* de este repositorio. Si GitHub no responde, el despliegue sigue igual y el informe queda en el registro local del equipo.

**Por qué.** Con D-55, el resultado del despliegue nace en el equipo. En la pestaña *Deployments* del repositorio queda, junto a cada commit, la evidencia de que se desplegó solo o de que volvió atrás, que es lo que piden los escenarios «Despliegue sano» y «Vuelta atrás».

**Alternativas.** Solo el registro del equipo (hay que entrar al equipo para saber si falló); el registro del equipo y una notificación de escritorio (sin evidencia en GitHub).

**Consecuencias.** El equipo guarda dos credenciales: Docker Hub de solo lectura y este token. GitHub sigue sin credenciales ni direcciones del equipo (D-55): la conexión siempre sale del equipo.

---

## D-57. El usuario de prueba `autor.prueba` se habilita en el equipo de demostración, con clave propia

- **Estado:** Aceptada · **Fecha:** 2026-10-17 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q2 de la especificación de B00-H08 (`specs/009-publicacion-despliegue/spec.md`)
- **Modifica a:** alcance del issue #25 (B00-H08); amplía el uso del usuario de prueba de D-37, que B00-H03 limitó a local y a la CI (`specs/003-autenticacion-jwt/research.md` §8)

**Decisión.** En el equipo de demostración, `autor.prueba` se habilita hasta que F05-H01 traiga los usuarios reales de la demo. Su contraseña se genera en el equipo, nunca es la de `.env.ejemplo` y vive solo ahí, como las demás credenciales del equipo. El usuario sigue detrás del acceso restringido del proxy.

**Por qué.** Sin un usuario, la demostración solo muestra la pantalla de inicio de sesión y no se puede comprobar de punta a punta, ni recorrer la SPA ni escanear la API con un token en el cierre de ola.

**Alternativas.** No habilitarlo hasta F05-H01: la demostración queda en la pantalla de inicio de sesión.

**Consecuencias.** F05-H01 lo deshabilita en el equipo de demostración cuando existan los usuarios reales.

---

## D-58. Los archivos del despliegue viajan en una imagen más, el paquete de despliegue

- **Estado:** Aceptada · **Fecha:** 2026-10-17 · **Decidido por:** autor de la tesis
- **Origen:** plan de B00-H08 (`specs/009-publicacion-despliegue/research.md` §5)
- **Modifica a:** alcance del issue #25 (B00-H08), que habla de publicar «las 6 imágenes»

**Decisión.** Además de las 6 imágenes de los procesos, el API Gateway y la SPA, cada fusión a `main` publica en el mismo repositorio privado una imagen `FROM scratch` con los archivos del despliegue de ese commit (`compose.demo.yml`, `Caddyfile`, `01-esquemas.sh`, `desplegar.sh`, `probar_humo.sh` y `VERSION`), con la etiqueta `despliegue-<commit>`. La etiqueta móvil `despliegue-main` se mueve al final, cuando las 7 imágenes del commit ya están publicadas, y es lo único que consulta el equipo de demostración.

**Por qué.** Un cambio en el despliegue viaja con las imágenes del mismo commit, y la vuelta atrás también vuelve a los archivos anteriores. El equipo usa la misma credencial de solo lectura de Docker Hub y no necesita acceso al repositorio privado de GitHub (D-55). La imagen no tiene paquetes del sistema, así que no agrega nada que revisar con Grype.

**Alternativas.** Clonar el repositorio en el equipo (necesita un token con lectura de código); instalar los archivos a mano una vez (un cambio de `compose.demo.yml` obligaría a desplegar a mano y la vuelta atrás no volvería a la configuración anterior); un artefacto OCI con ORAS (otra herramienta en el equipo).

**Consecuencias.** El repositorio tiene 7 etiquetas por commit. «Las 6 imágenes» del issue siguen siendo las que se construyen desde el código y Grype revisa.

---

## D-59. Los usuarios de la demo salen de un archivo versionado; sus contraseñas, de cada entorno

- **Estado:** Aceptada · **Fecha:** 2026-10-17 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q1 de la especificación de F05-H01 (`specs/010-iniciar-sesion/spec.md`)
- **Modifica a:** alcance del issue #43 (F05-H01); completa D-37 y D-57

**Decisión.** Los usuarios de la demo (uno por cada rol de la matriz de D-16 y uno con Docente y Coordinador de carrera) se definen en un archivo versionado del repositorio, con su nombre de usuario, su nombre para mostrar y sus roles, **sin contraseñas**. Usuarios los crea al arrancar si no existen. La contraseña de cada uno se fija en cada entorno: en local y en la CI, con valores ficticios documentados; en el equipo de demostración, generadas por `preparar.sh` y guardadas solo ahí. En la base solo queda el hash Argon2id.

**Por qué.** No hay pantalla de gestión de usuarios en esta historia, y un archivo versionado hace la demo repetible en cualquier entorno sin poner contraseñas en el repositorio.

**Alternativas.** Un comando de administración en el equipo (manual y sin registro en el repositorio); una pantalla de administración de usuarios (adelanta trabajo y lleva la historia a tamaño L).

**Consecuencias.** Cambiar los usuarios de la demo es un cambio en el repositorio, revisado en un pull request. La gestión de usuarios desde la SPA, si llega, la hará otra historia.

---

## D-60. F05-H01 incluye RNF-SEG-07: bloqueo tras 5 intentos e inactividad de 30 minutos

- **Estado:** Aceptada · **Fecha:** 2026-10-17 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q2 de la especificación de F05-H01
- **Modifica a:** alcance del issue #43 (F05-H01); la vigencia fija de 30 minutos del token de B00-H03 (ADR-014)

**Decisión.** Después de 5 intentos fallidos seguidos con el mismo nombre de usuario, el inicio de sesión con ese nombre queda bloqueado por un tiempo (15 minutos por defecto); los intentos se cuentan por nombre intentado, exista o no, para no revelar qué usuarios existen. La sesión vence tras 30 minutos sin actividad y se renueva mientras el usuario trabaja, con un máximo de 8 horas desde el inicio de sesión. Los tres valores son parámetros.

**Por qué.** RNF-SEG-07 (prioridad *Should*) está en el servicio de Usuarios y encaja con el inicio de sesión. Hoy el token dura 30 minutos fijos, así que un usuario activo sería expulsado en medio de su trabajo.

**Alternativas.** Solo el bloqueo por intentos; dejar RNF-SEG-07 para otra historia.

**Consecuencias.** El plan de F05-H01 decide cómo se renueva la sesión sin guardar el token fuera de la memoria (D-51) y dónde se cuentan los intentos.

---

## D-61. «Tokens y secretos cifrados en reposo» (RNF-SEG-02) se cumple por diseño

- **Estado:** Aceptada · **Fecha:** 2026-10-17 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q3 de la especificación de F05-H01
- **Modifica a:** — (interpreta RNF-SEG-02 para el prototipo)

**Decisión.** RNF-SEG-02 se da por cumplido así, con la evidencia de F05-H01: las contraseñas se guardan solo como hash Argon2id; el MDD no guarda tokens (son JWT sin estado y la SPA los tiene solo en memoria, D-51); los secretos del equipo de demostración viven solo en `/opt/mdd`, con permisos de su dueño (0600 y 0400), fuera del repositorio, de GitHub y de las imágenes (D-55, B00-H08). No se agrega cifrado de disco a esos archivos.

**Por qué.** No hay tokens en reposo que cifrar, y para un prototipo en un equipo personal los permisos del sistema de archivos protegen los secretos sin complicar el despliegue.

**Alternativas.** Cifrar `.env` y `configuracion.env` del equipo (por ejemplo, con `systemd-creds`): más seguridad, más complejidad en el despliegue.

**Consecuencias.** Si el MDD llega a guardar tokens o secretos en la base, o se despliega en la VM del IST, se revisa esta decisión.

---

## ADR-020. Sesión por inactividad sin estado: token corto que la SPA renueva mientras hay actividad

- **Estado:** Aceptada · **Fecha:** 2026-10-17 · **Decidido por:** autor de la tesis
- **Origen:** plan de F05-H01 (`specs/010-iniciar-sesion/research.md` §6), para cumplir D-60
- **Modifica a:** ADR-014 (agrega los claims `nombre` e `inicio` y la operación de renovación)

**Decisión.**
1. El token de sesión dura 30 minutos, como hoy, y lleva dos claims nuevos: `nombre` (nombre para mostrar) e `inicio` (instante del inicio de sesión).
2. Usuarios expone `POST /api/usuarios/sesion/renovacion`: con un token de sesión vigente, emite uno nuevo con el mismo `inicio` y otros 30 minutos, salvo que hayan pasado 8 horas desde `inicio`.
3. La SPA registra la actividad del usuario (navegación, teclado, ratón y peticiones a la API) y renueva el token una vez cuando hubo actividad desde la última renovación y le quedan menos de 20 minutos. Sin actividad, el token vence a los 30 minutos y la SPA cierra la sesión.
4. El token sigue solo en memoria (D-51) y los procesos siguen validando sin estado con el JWKS. `SesionActual` gana `nombre` opcional.

**Por qué.** Cumple «expira por inactividad» (RNF-SEG-07) sin guardar estado de sesión en el servidor ni el token fuera de la memoria, y sin que los procesos consulten a Usuarios en cada petición.

**Alternativas.** *Refresh token* en una cookie `HttpOnly` (rompe D-51 y obliga a protegerse de CSRF); tabla de sesiones con la última actividad (estado en el servidor y una consulta por petición); token largo sin inactividad (no cumple RNF-SEG-07).

**Consecuencias.**
- Un token robado podría renovarse hasta 8 horas: no hay revocación en el prototipo; se anota como limitación.
- Las historias con pantallas no hacen nada: la renovación vive en la base de la SPA.
- Los procesos ignoran los claims nuevos; solo el «sesión actual» de Usuarios devuelve `nombre`.

---

## D-62. Un cambio de permisos vale para una sesión abierta en su próxima renovación

- **Estado:** Aceptada · **Fecha:** 2026-10-17 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q1 de la especificación de F05-H02
- **Modifica a:** ADR-014 (los permisos efectivos viajan con la sesión), en la línea de ADR-020

**Decisión.** Si el administrador cambia los roles de un usuario, o los permisos de un rol, mientras ese usuario tiene la sesión abierta, el cambio vale desde la próxima renovación de su sesión: como máximo 30 minutos después (ADR-020), o al volver a entrar. Los procesos siguen validando sin estado, sin consultar al servicio Roles en cada petición.

**Por qué.** Mantiene el diseño sin estado de ADR-014 y ADR-020 y no acopla los 4 procesos a Roles. Para un prototipo, una demora de hasta 30 minutos en quitar un permiso es aceptable.

**Alternativas.** De inmediato, con una consulta a Roles en cada petición (acopla los procesos y suma latencia); recién al volver a entrar (hasta 8 horas de demora).

**Consecuencias.** La renovación recalcula los permisos y los roles vigentes. Un usuario desactivado ya no renueva (F05-H01). El plan define cómo llegan los permisos a los procesos.

---

## D-63. Los 10 roles de D-16 son fijos: se asignan, pero no se editan ni se borran

- **Estado:** Aceptada · **Fecha:** 2026-10-17 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q2 de la especificación de F05-H02
- **Modifica a:** —

**Decisión.** Los 10 roles de la matriz D-16 existen desde el principio, con sus permisos. El administrador los ve y los asigna, pero no puede cambiar sus permisos ni borrarlos. Para otra combinación de permisos, crea un rol nuevo, que sí puede editar y, si no está asignado, borrar.

**Por qué.** La matriz del ERS y el sistema nunca se separan sin una decisión registrada.

**Alternativas.** Roles de D-16 editables, pero no borrables; o editables y borrables como cualquier otro.

**Consecuencias.** Cambiar la matriz D-16 exige una decisión nueva y un cambio en el sistema, no un ajuste en la pantalla.

---

## D-64. F05-H02 marca qué permisos son «solo lo propio»; el filtro lo aplica cada historia con esos datos

- **Estado:** Aceptada · **Fecha:** 2026-10-17 · **Decidido por:** autor de la tesis
- **Origen:** aclaración Q3 de la especificación de F05-H02
- **Modifica a:** —

**Decisión.** Cada permiso tiene un alcance: **todo** o **solo lo propio** (su carrera, unidad o persona, D-16). F05-H02 registra y muestra ese alcance, pero no filtra datos, porque todavía no existen carreras, unidades ni docentes. Cada historia que trae esos datos (F03 y siguientes) aplica el filtro de «lo propio» a sus operaciones.

**Por qué.** El alcance queda en el modelo desde el principio, sin inventar datos que todavía no existen.

**Alternativas.** Ignorar el alcance hasta que lleguen los datos: más simple hoy, pero obliga a cambiar el modelo después.

**Consecuencias.** Las historias de F03 en adelante deben probar que un permiso «solo lo propio» no deja ver ni cambiar lo de otra carrera, unidad o persona.

---

## D-65. La matriz D-16 en permisos: 16 funcionalidades y los permisos de los 10 roles

- **Estado:** Aceptada · **Fecha:** 2026-10-18 · **Decidido por:** autor de la tesis
- **Origen:** plan de F05-H02 (`specs/011-roles-permisos/research.md` §6 y `data-model.md`, «Matriz D-16»)
- **Modifica a:** — (interpreta D-16)

**Decisión.** La matriz D-16 del ERS, escrita en prosa, se traduce a 16 funcionalidades (Usuarios y roles, Parámetros y normativa, Catálogos y datos maestros, Flujo y árbol de firmas, Bitácora, Cargas, Docentes, Oferta y propuesta, Proyectos, Consolidación, Distributivo, Reportes, Votación del OCS, Resolución del OCS, Publicación y Mi distributivo). Cada una admite algunos de los tipos lectura, escritura, aprobación y firma. Los permisos de los 10 roles son los de la tabla «Matriz D-16» de `data-model.md`, con la celda del ERS que origina cada uno.

**Por qué.** Sin una traducción explícita, cada historia interpretaría la matriz a su manera, y SC-002 no se podría comprobar.

**Alternativas.** Funcionalidades más finas, una por pantalla: más permisos que mantener y una matriz que el autor no podría revisar de un vistazo. Más gruesas, una por módulo: no distinguen, por ejemplo, la oferta de la consolidación.

**Consecuencias.** Cambiar un permiso de un rol de D-16 exige revisar esta decisión y escribir una migración nueva (D-63). Las historias siguientes agregan funcionalidades con su propia migración.

---

## ADR-021. Permisos efectivos en el token y cada operación declara su permiso en el contrato

- **Estado:** Aceptada · **Fecha:** 2026-10-18 · **Decidido por:** autor de la tesis
- **Origen:** plan de F05-H02 (`specs/011-roles-permisos/research.md` §1 a §5), para cumplir RNF-SEG-03 y D-62
- **Modifica a:** ADR-014 (claim `permisos`; los roles salen de Roles), ADR-015 (extensión `x-mdd-permiso`), ADR-016 (403 con cuerpo)

**Decisión.**
1. Roles es dueño de los roles, sus permisos y las asignaciones de roles. Usuarios deja de guardar roles, aunque `usuario_rol` se conserva para poder volver a una versión anterior.
2. Al iniciar sesión y al renovar, Usuarios pide a Roles los roles y permisos efectivos, por su interfaz publicada dentro del proceso, y los pone en el token: `roles` y `permisos` (`FUNCIONALIDAD:TIPO[:PROPIO]`).
3. Cada proceso convierte los permisos en autoridades `PERMISO_FUNCIONALIDAD:TIPO`. Cada operación protegida declara `x-mdd-permiso` en su contrato y `@PreAuthorize` en su controlador. `VerificadorDePermisos` (`plataforma:pruebas`) comprueba, operación por operación, que sin ese permiso se recibe 403.
4. El 403 es `SEGURIDAD_ACCESO_NO_AUTORIZADO`, con el modelo de errores común, en los 4 procesos y en el API Gateway.

**Por qué.** La validación sigue sin estado (ADR-014, ADR-020), el permiso queda en el contrato, que es la fuente de verdad, y una prueba impide que el contrato y el código se separen.

**Alternativas.** Consultar a Roles en cada petición (acopla los procesos); reglas por ruta en la cadena de filtros (se separan del contrato); un servidor de autorización externo (desproporcionado para el prototipo).

**Consecuencias.**
- Un cambio de permisos tarda hasta 30 minutos en valer (D-62).
- El token crece unos 500 bytes en el peor caso.
- Cada historia nueva declara el permiso de sus operaciones y su prueba de autorización viene sola.

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

---

## D-66. El Vicerrector académico lee «Parámetros y normativa»

- **Estado:** Aceptada · **Fecha:** 2026-10-18 · **Decidido por:** autor de la tesis (propuesta en el plan de F04-H01)
- **Origen:** aclaración de la especificación de F04-H01 (`specs/012-politica-normativa/spec.md`, Clarifications) y `research.md` §6
- **Modifica a:** D-65 (fila del Vicerrector académico en la matriz D-16)

**Decisión.** El rol Vicerrector académico (`VICERRECTOR`) gana el permiso `PARAMETROS:LECTURA` con alcance todo. La escritura de «Parámetros y normativa» sigue solo en el Administrador del sistema. Se aplica con la migración `V4__vicerrector_lee_normativa.sql` de Roles, y `MatrizD16IntegrationTest` actualiza su fila esperada.

**Por qué.** La historia F04-H01 la pide el Vicerrector académico, y es él quien abre el periodo y elige su versión de normativa (F02-H01). D-16 le da «todo el distributivo», y las reglas que lo gobiernan son parte de leerlo. Sin este permiso, no vería la normativa que elige.

**Alternativas.** Dejar la normativa solo para el Administrador (el Vicerrector elegiría a ciegas la versión de su periodo); abrirla a todos los usuarios con sesión (contradice el mínimo privilegio de D-16).

**Consecuencias.** La fila del Vicerrector en la matriz queda con lectura de `DISTRIBUTIVO`, `REPORTES`, `BITACORA` y `PARAMETROS`. El permiso llega al token de una sesión abierta en su próxima renovación (D-62). Documentos a poner al día: `specs/012-politica-normativa/data-model.md` (ya lo dice) y la prueba de la matriz.

---

## D-67. El Vicerrector académico escribe «Parámetros y normativa»

- **Estado:** Aceptada · **Fecha:** 2026-10-18 · **Decidido por:** autor de la tesis (propuesta en el plan de F04-H02)
- **Origen:** aclaración de la especificación de F04-H02 (`specs/013-version-normativa-nueva/spec.md`, Clarifications) y `research.md` §7
- **Modifica a:** D-65 (fila del Vicerrector académico en la matriz D-16) y amplía D-66

**Decisión.** El rol Vicerrector académico (`VICERRECTOR`) gana el permiso `PARAMETROS:ESCRITURA`, con alcance todo: crea versiones de normativa, las edita mientras no rigen un periodo y las asocia a un periodo. Se aplica con la migración `V5__vicerrector_escribe_normativa.sql` de Roles, y `MatrizD16IntegrationTest` actualiza su fila esperada. El Administrador del sistema conserva la lectura y la escritura.

**Por qué.** La historia F04-H02 la pide el Vicerrector académico. D-16 le da los ajustes a la propuesta y aprobar y congelar el distributivo, y él abre el periodo y elige su versión (F02-H01). Sin este permiso, cada cambio de la norma dependería de sistemas.

**Alternativas.** Solo el Administrador (D-65 tal cual): el Vicerrector pediría cada cambio de la política. Una funcionalidad aparte para «versionar la normativa»: más permisos para un solo rol más.

**Consecuencias.** La fila del Vicerrector en la matriz queda con lectura de `DISTRIBUTIVO`, `REPORTES`, `BITACORA` y `PARAMETROS`, y escritura de `DISTRIBUTIVO` y `PARAMETROS`. Como `PARAMETROS` agrupa parámetros y normativa, también podrá escribir los demás parámetros cuando las historias que los traigan los protejan con esa funcionalidad. El permiso llega al token en la próxima renovación (D-62).

## D-68. Las reglas del motor y la afinidad por defecto completan las versiones existentes por migración

- **Estado:** Aceptada · **Fecha:** 2026-10-20 · **Decidido por:** autor de la tesis (propuesta en el plan de F04-H03)
- **Origen:** aclaración de la especificación de F04-H03 (`specs/014-afinidad-severidad/spec.md`, Clarifications) y `research.md` §5
- **Modifica a:** — (excepción acotada al principio VII de la constitución para una migración)

**Decisión.** La migración `V4__afinidad_y_reglas_del_motor.sql` de Normativa agrega a cada versión de normativa existente las reglas del motor que le faltan (RN-12, RN-16, RN-17, RN-18, RN-19, RN-22, RN-25 y RF-24, con la severidad por defecto de la tabla 5.2 del modelo de dominio) y la regla RN-24 con los pesos de afinidad por defecto, más el mínimo de afinidad en RN-19. Lo hace aunque la versión rija un periodo. Deja un `VERSION_EDITADA` de `sistema` por versión, con cada valor agregado. No cambia ningún valor que ya existía.

**Por qué.** Las reglas y los pesos agregados no existían en ninguna versión: completarlas no cambia cómo se validó un periodo. Sin ellos, la versión 1 y las creadas a mano quedarían sin pesos ni mínimo, y una versión en uso no podría completarse nunca (F06 y F07 los necesitan). Hoy ninguna versión rige un periodo en ningún entorno.

**Alternativas.** Agregarlos solo a las versiones nuevas: las existentes quedarían incompletas. Agregarlos solo a las versiones sin periodo: una versión en uso sin RN-19 rompería F07.

**Consecuencias.** Por la API, una versión en uso sigue sin cambiar (409). Cualquier otra migración que quiera completar versiones en uso necesita su propia decisión.

## D-69. Los periodos académicos se escriben con «Distributivo del periodo» y se abren solo desde Académico

- **Estado:** Aceptada · **Fecha:** 2026-10-21 · **Decidido por:** autor de la tesis (propuesta en el plan de F02-H01)
- **Origen:** aclaración de la especificación de F02-H01 (`specs/015-abrir-periodo/spec.md`, Clarifications) y `research.md` §7
- **Modifica a:** D-65 (interpreta a qué funcionalidad de la matriz pertenece el periodo académico) y F04-H02 (la pantalla «Normativa» pierde el diálogo «Asociar a un periodo»)

**Decisión.** Los periodos académicos del servicio Académico se leen con `DISTRIBUTIVO:LECTURA` y se escriben (registrar, corregir, abrir y cambiar el estado) con `DISTRIBUTIVO:ESCRITURA`, la funcionalidad «Distributivo del periodo» de D-65. La matriz no cambia: hoy escribe solo el Vicerrector académico. Abrir el periodo, es decir, asociarle su versión de normativa, se hace desde Académico, que se lo pide a Normativa por su interfaz publicada dentro del proceso `datos`. La pantalla «Normativa» deja de ofrecer «Asociar a un periodo». La operación HTTP `asociarPeriodo` de Normativa se queda, para las pruebas de punta a punta y la integración.

**Por qué.** La historia y la pregunta 4 del modelo de dominio dicen que el Vicerrectorado abre el periodo y cambia su estado, y el autor eligió no cambiar la matriz. «Catálogos y datos maestros» es del Administrador del sistema: dárselo al Vicerrector le daría también las carreras, las mallas y los demás datos maestros. Con el diálogo de «Normativa», un periodo podría quedar con versión sin existir en Académico, o saltarse sus reglas (cerrado, orden de los estados).

**Alternativas.** El Vicerrector gana `CATALOGOS:ESCRITURA`: más permisos de los que pide la historia. Una funcionalidad nueva «Periodos» en la matriz: cambia D-65 para un solo rol. Dejar el diálogo de «Normativa»: dos caminos para lo mismo.

**Consecuencias.** El Administrador del sistema no escribe periodos. La operación HTTP de Normativa no comprueba que el periodo exista en Académico, porque Normativa no puede depender de Académico sin un ciclo; solo la pueden llamar quienes tienen `PARAMETROS:ESCRITURA` (el Administrador y el Vicerrector). La llamada interna de Académico a Normativa no pasa por el `@PreAuthorize` del controlador de Normativa: la autorización es la de la operación de Académico.

## D-70. Una operación puede declarar varios permisos alternativos; la estructura orgánica la leen quienes leen el distributivo o los catálogos

- **Estado:** Aceptada · **Fecha:** 2026-10-21 · **Decidido por:** autor de la tesis (propuesta en el plan de F03-H01)
- **Origen:** aclaraciones de la especificación de F03-H01 (`specs/017-estructura-organica/spec.md`, Clarifications) y `research.md` §2 y §4
- **Modifica a:** ADR-021 (`x-mdd-permiso` admite una lista de permisos alternativos) e interpreta RF-12

**Decisión.** Una operación del contrato puede declarar en `x-mdd-permiso` una lista de permisos alternativos: basta con tener uno. El `@PreAuthorize` usa `hasAnyAuthority`, y `VerificadorDePermisos` comprueba que sin ninguno responde 403 y con cada uno por separado no. Las consultas de la estructura orgánica de Académico declaran `[DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`; mantenerla sigue exigiendo un solo permiso, `CATALOGOS:ESCRITURA`, que hoy tiene solo el Administrador del sistema. Además, el criterio de RF-12 («no se puede crear una gestión fuera del catálogo autorizado sin permiso de administrador») se interpreta como que nadie la crea: el catálogo de las 21 gestiones de POL 6 es un dato precargado y solo se amplía con una migración (V-02, D-07, RN-10).

**Por qué.** La estructura la usan quienes leen el distributivo (el Vicerrector académico, el Responsable de consolidación, el Rector y los demás de D-65) y la mantiene el Administrador, como dice la ERS §8.3. Sin permisos alternativos habría que dar «Catálogos y datos maestros: lectura» a siete roles, lo que también les daría los catálogos que lleguen después. RN-10 solo admite las 21 gestiones, así que dejar que el Administrador agregue otras la contradiría.

**Alternativas.** Dar `CATALOGOS:LECTURA` a los roles que leen el distributivo (cambia D-65 para siete roles). Leer solo con `DISTRIBUTIVO:LECTURA` (mezcla la funcionalidad de los catálogos con la del distributivo). Que el Administrador amplíe el catálogo de gestiones (literal a RF-12, contrario a RN-10).

**Consecuencias.** Cada operación sigue declarando sus permisos en el contrato, y la prueba de permisos de cada servicio los cubre todos. La SPA acepta varios permisos en una entrada del menú y en una ruta.

## D-71. El Responsable de consolidación y el Vicerrector académico ejecutan las cargas

- **Estado:** Aceptada · **Fecha:** 2026-10-23 · **Decidido por:** autor de la tesis (aclaración 1 de F01-H02; propuesta en su plan)
- **Origen:** `specs/019-subir-archivo-carga/spec.md` (Clarifications) y `research.md` §1
- **Modifica a:** D-65 (matriz D-16)

**Decisión.** El Responsable de consolidación y el Vicerrector académico reciben «Cargas de datos: lectura» y «Cargas de datos: escritura», con alcance «todo». Talento Humano las conserva. Una migración nueva de Roles (`V7__cargas_para_quien_inicia.sql`) las agrega (D-63).

**Por qué.** La ERS §8.3 dice que «el rol que inicia el proceso de construcción del distributivo (por defecto, el Vicerrectorado o el Responsable de consolidación) ejecuta las cargas» y delega en Talento Humano; la historia F01-H02 la pide el Responsable de consolidación. En D-65 solo Talento Humano tenía «Cargas: escritura», porque la matriz en prosa de D-16 lo nombraba a él («Cargas que se le deleguen»).

**Alternativas.** Permisos alternativos de D-70 (`[CARGAS:ESCRITURA, CONSOLIDACION:ESCRITURA, DISTRIBUTIVO:ESCRITURA]`): no cambia la matriz, pero autoriza la carga de datos con permisos de otras funcionalidades. Solo Talento Humano: la historia no se cumpliría.

**Consecuencias.** Los dos roles suben archivos de carga y ven «Plantillas de carga» también por «Cargas: lectura». Al desplegar, los usuarios con esos roles reciben los permisos en su próximo inicio de sesión. La prueba de la matriz (`MatrizD16IntegrationTest`) se pone al día; la tabla «Matriz D-16» de `specs/011-roles-permisos/data-model.md` queda como la interpretación original de D-65, como con D-67.

## D-72. Cada campo de entrada se valida por su tipo y su largo, y cada error del catálogo dice su clasificación, su causa, su descripción técnica y la acción para el usuario

- **Estado:** Aceptada · **Fecha:** 2026-10-24 · **Decidido por:** autor de la tesis
- **Origen:** primera comprobación del autor de F03-H04 en el equipo de demostración. El formulario de docentes aceptaba letras en la cédula y el teléfono, y el servidor respondía «No tiene el formato esperado» sin decir qué corregir. Se corrigió en el PR #145.
- **Modifica a:** la constitución, que pasa a la versión 2.2.0. Agrega el principio XIV, «Validación de las entradas», amplía el catálogo de códigos del principio XIII y suma criterios a la definición de terminado de la sección 6.

**Decisión.**
1. **Validación de las entradas.** Todo campo de entrada se valida según su tipo:
   - numérico: solo los valores y el formato definidos para el campo;
   - alfabético: solo letras, según las reglas del campo;
   - alfanumérico o mixto: letras, dígitos y solo los caracteres especiales autorizados expresamente.
   
   También se validan su largo mínimo y máximo, o el exacto. La regla de cada campo sale de una definición funcional: la ERS, el diccionario de datos, la especificación o el contrato. No se inventan formatos ni largos. El backend valida siempre. El frontend aplica las mismas reglas mientras se escribe y antes de enviar, para avisar a tiempo, pero no reemplaza al backend.
2. **Catálogo centralizado de errores.** El catálogo es `contratos/comun/errores.md` junto con su contraparte en el código, y toda implementación nueva lo usa o lo amplía. Cada código nuevo registra:
   - el código único;
   - la clasificación: validación, negocio, autenticación, autorización o sistema;
   - el estado HTTP;
   - el título y el mensaje para el usuario final;
   - la acción recomendada, cuando la hay;
   - la causa o condición que lo activa;
   - una descripción técnica para el diagnóstico, que nunca llega al usuario.
   
   El usuario nunca ve trazas, excepciones ni detalles internos. Los mensajes salen del catálogo y no se duplican. Los motivos por campo (`detalles`) también dicen qué hacer.
3. **Criterios de terminado.** Antes de cerrar una implementación se verifica:
   - que las validaciones de tipo y largo estén implementadas y probadas, con casos válidos e inválidos;
   - que cada error se mapee a su código del catálogo;
   - que los mensajes sean claros, consistentes y accionables;
   - que las pruebas se hayan ejecutado y sus resultados estén en la evidencia de la historia.

**Por qué.** Una validación que solo está en el servidor, con un mensaje genérico, obliga al usuario final a adivinar qué corregir (RNF-USA-01). Un catálogo con la causa y la descripción técnica separadas del mensaje permite diagnosticar un error sin mostrarle detalles internos al usuario (RNF-SEG-04, principio XIII).

**Alternativas.**
- Dejarlo como guía del plan de cada historia: no obliga y depende de que alguien lo recuerde.
- Validar solo en el frontend: un cliente de la API se lo saltaría.
- Completar ahora el catálogo existente con los campos nuevos: se hace aparte, porque la prueba que compara el catálogo con el código tiene que crecer con él.

**Consecuencias.**
- Las especificaciones nuevas fijan el tipo, los caracteres admitidos y el largo de cada campo de entrada. Los planes y las tareas los llevan al contrato (`pattern`, `minLength`, `maxLength`, `minimum`, `maximum`), al dominio y a la SPA.
- La evidencia de cada historia anota las validaciones y los errores probados.
- Los códigos que ya están en el catálogo se completan con la clasificación, la causa y la descripción técnica en un cambio aparte, que también pone al día la prueba que compara el catálogo con el código.

## D-73. Los 16 cargos de gestor tienen código propio, del G06 al G21

- **Estado:** Aceptada · **Fecha:** 2026-10-25 · **Decidido por:** autor de la tesis (propuesta en el plan de F03-H05)
- **Origen:** `specs/023-cargos-proyectos/research.md` §3
- **Modifica a:** el catálogo `CARGOS` del diccionario de datos de la carga 8, que daba los cargos de gestor solo como un rango.

**Decisión.** Cada gestión autorizada G06 a G21 de POL 6 tiene su cargo de gestor. Su código es el de la gestión con el núcleo de su nombre:

| Gestión | Código |
|---|---|
| G06 | `G06_GESTOR_VINCULACION` |
| G07 | `G07_GESTOR_INVESTIGACION` |
| G08 | `G08_GESTOR_IDIOMAS` |
| G09 | `G09_GESTOR_FORMACION_INTEGRAL` |
| G10 | `G10_COORDINADOR_CALIDAD` |
| G11 | `G11_SECRETARIO_GENERAL` |
| G12 | `G12_PROCURADOR` |
| G13 | `G13_GESTOR_BIENESTAR` |
| G14 | `G14_GESTOR_ADMINISTRATIVO_FINANCIERO` |
| G15 | `G15_GESTOR_PLANIFICACION` |
| G16 | `G16_GESTOR_TALENTO_HUMANO` |
| G17 | `G17_GESTOR_BIBLIOTECA` |
| G18 | `G18_GESTOR_COMUNICACION` |
| G19 | `G19_GESTOR_RELACIONES_INTERNACIONALES` |
| G20 | `G20_GESTOR_TIC` |
| G21 | `G21_GESTOR_ESTADISTICAS` |

El catálogo `CARGOS` de `diccionario-de-datos.yaml` cambia la fila del rango por estos 16 valores, así que la plantilla y el diccionario de la carga 8 los listan. La unidad de un cargo de gestor es la de su gestión autorizada en la estructura orgánica.

**Por qué.** El diccionario decía «G06_GESTOR_VINCULACION … G21_GESTOR_ESTADISTICAS, prefijo con el código de gestión autorizada». Los datos de prueba usan 9 de los 16 códigos, y el principio XIV no deja inventar los otros 7. Los 7 que faltaban se forman como los 9 conocidos y caben en los 40 caracteres de la columna.

**Alternativas.**
- Aceptar cualquier `Gxx_…` con el prefijo bien puesto: no sería un catálogo, y el mismo cargo podría escribirse de dos maneras.
- Formar el código con el nombre completo de la gestión: pasa de 40 caracteres y no coincide con los datos de prueba.

**Consecuencias.**
- La enumeración `TipoDeCargo` de Docentes tiene 22 valores, que `ValoresPermitidosTest` compara con el diccionario.
- La carga 8 por archivo (F01-H04) valida contra los mismos 22 valores.

## ADR-022. El lenguaje publicado de las cargas y el motor del lote, en `:plataforma:lenguaje-cargas`

- **Estado:** Aceptada · **Fecha:** 2026-10-26 · **Decidido por:** autor de la tesis (aprobado en el plan de F01-H04)
- **Origen:** `specs/025-confirmar-carga/research.md` §1
- **Modifica a:** MD-13 y ADR-009, que dicen que Cargas envía el lote completo al servicio dueño sin decir con qué tipos

**Decisión.**
1. Un módulo nuevo, `:plataforma:lenguaje-cargas` (paquete `ec.mdd.plataforma.cargas`, con `@NamedInterface("cargas")`) —no `:plataforma:cargas`, porque Gradle confunde dos proyectos con el mismo nombre que `:servicios:cargas`—, es el lenguaje común entre Cargas y los dueños de las cargas:
   - el lote (`LoteDeCarga` y `FilaDelLote`);
   - la vista previa (`VistaPreviaDelLote`, con su huella), el resultado y los rechazos;
   - el traductor que cada dueño escribe por carga (`TraductorDeCarga`);
   - el motor que compara, calcula la huella y aplica todo o nada (`MotorDelLote`);
   - el aplicador que ve Cargas (`AplicadorDeCargas`).
2. Cada dueño publica un `AplicadorDeCargas` con las cargas que atiende: Académico la 01 a la 05 y la 10, Docentes la 06, la 08 y la 09, y Normativa la 07. Cargas recibe la lista y, al arrancar, comprueba que cada una de las 10 tiene exactamente uno.
3. El motor aplica el lote dentro de la transacción del dueño y con sus casos de uso de siempre, así que se aplican sus reglas y su registro de cambios.

**Por qué.** La comparación, la huella y el «todo o nada» son iguales para las 10 cargas: escritos una vez, se prueban una vez. Cargas no conoce los tipos de los dueños (principio V). Es el mismo camino que `:plataforma:eventos`.

**Alternativas.**
- **Un puerto propio en cada dueño:** repite los mismos registros tres veces, y Cargas tendría tres adaptadores casi iguales.
- **El puerto en Cargas:** los dueños dependerían de Cargas, y `ModulosTest` solo permite los `puertos.entrada` de otro servicio.

**Consecuencias.**
- Académico, Docentes, Normativa y Cargas dependen de `:plataforma:lenguaje-cargas`.
- Una carga nueva, o un dueño nuevo, es un traductor más.
- El motor se prueba sin dobles en su módulo.

## D-74. Los mensajes al usuario lo tratan de «tú»

- **Estado:** Aceptada · **Fecha:** 2026-10-26 · **Decidido por:** autor de la tesis (aprobado en el plan de F01-H04)
- **Origen:** `specs/025-confirmar-carga/research.md` §10
- **Modifica a:** los motivos de las reglas de las filas y la pantalla «Plantillas de carga» de F01-H03, que trataban de «usted»

**Decisión.** Los mensajes que ve el usuario (el catálogo de errores, los motivos del informe por fila y los textos de la SPA) lo tratan de «tú», como ya lo hacía el catálogo: «Guárdalo como CSV UTF-8», «Corrígelo y vuelve a subirlo».

**Por qué.** El principio XIII pide mensajes consistentes, y en la misma pantalla convivían «Descarga la plantilla» y «Corríjalo y vuelva a subirlo».

**Alternativas.** Pasar todo a «usted»: cambia el catálogo entero y casi toda la SPA, sin ganar claridad.

**Consecuencias.** F01-H04 pasa a «tú» los motivos de Cargas y los textos de «Plantillas de carga»; una búsqueda comprueba que no queda ningún «usted» en ellos.

## ADR-023. Consultas entre procesos por rutas internas con token de proceso

- **Estado:** Aceptada · **Fecha:** 2026-10-30 · **Decidido por:** autor de la tesis
- **Origen:** plan de F06-H01 (`specs/028-puntaje-afinidad/research.md` §1 a §4)
- **Modifica a:**
  - ADR-018, que fija las rutas internas y el token de proceso solo para los eventos: los extiende a las consultas;
  - ADR-010, que dice que la ficha se pide «por la API de Docentes»: se pide por una ruta interna de esa API, no por la pública;
  - ADR-009, que dejó los errores 502 y 504 para la primera llamada entre procesos: los fija.
- **Relacionadas:** D-49, D-65, ADR-006, ADR-016; principios V, IX, X y XIII

**Contexto.**
- F06-H01 es la primera historia que necesita leer datos de otro proceso: Generación corre en `distributivo`, y Académico, Normativa y Docentes, en `datos`.
- Reenviar el token del usuario no sirve: leer Normativa exige `PARAMETROS:LECTURA`, y el Coordinador de carrera no lo tiene. Con su token, el cálculo fallaría justo para el actor de la historia.
- El resto de la Ola 2 va a necesitar el mismo camino.

**Decisión.**
1. **Rutas internas.** El dueño del dato publica cada consulta en `/interno/<servicio>/…`. El API Gateway no las enruta, y `plataforma:seguridad` solo admite ahí el rol `PROCESO`.
2. **Contrato.** Cada ruta se declara en el `openapi.yaml` de su dueño, sin `x-mdd-permiso`: la seguridad es por ruta y rol, no por permiso de usuario.
3. **Lo mínimo.** Cada ruta devuelve solo lo que su consumidor necesita, con un modelo propio y no el del dominio del dueño (principio IX). El consumidor recibe esos datos en sus propios tipos (principio V).
4. **El permiso es de la operación pública.** Quien decide si el usuario puede ver el resultado es la operación pública que inicia la consulta, no el dueño del dato.
5. **Cliente.** `ConsultasEntreProcesos`, en `:plataforma:eventos`, hace un GET con el token de proceso y el plazo de `mdd.eventos.plazo`. Ante un 401, pide otro token y reintenta una vez. Un 404 es un resultado vacío, y cada adaptador dice qué significa.
6. **Errores.**
   - Si el otro proceso falla, responde 5xx, no se puede conectar con él o Usuarios no emite el token: 502 `COMUN_PROCESO_CON_ERROR`.
   - Si no responde dentro del plazo: 504 `COMUN_PROCESO_SIN_RESPUESTA`.
   - Nunca se responde un resultado parcial. `ErroresEntreProcesos`, en el mismo módulo, los registra en cada proceso que lo incluye.

**Alternativas descartadas.**
- **Reenviar el token del usuario:** ata cada consulta a los permisos de cada dueño, y obligaría a cambiar la matriz D-65 por un detalle técnico.
- **Copiar los datos por eventos:** suma tablas de réplica y datos viejos.
- **Mover el cálculo a `datos`:** contradice ADR-010.
- **Un módulo de plataforma nuevo:** el token y el cliente HTTP ya están en `:plataforma:eventos` (principio X).

**Consecuencias.**
- Una ruta interna expone datos a cualquier proceso con token de proceso, así que debe devolver lo mínimo.
- Un proceso `datos` caído impide estas consultas en `distributivo`. El usuario ve el error del catálogo dentro del plazo.
- `plataforma:eventos` ya no es solo de eventos: su `package-info` lo dice.

**Documentos que hay que poner al día.** `contratos/comun/errores.md`, con los dos códigos comunes.

## D-75. Las horas disponibles de un docente salen de sus horas contratadas

- **Estado:** Aceptada · **Fecha:** 2026-10-31 · **Decidido por:** autor de la tesis (aclaración de F06-H02, aprobada en su plan)
- **Origen:** `specs/029-docentes-sugeridos/research.md` §9
- **Modifica a:** el glosario `sdd/modelo_dominio.md` §7, «Horas disponibles», que decía «Dedicación del docente menos sus horas ya asignadas»

**Decisión.** Las horas disponibles de un docente son sus horas contratadas menos sus horas ya asignadas en todas las carreras y unidades, y nunca menos de 0.

**Por qué.** El tiempo parcial llega «hasta 19 h»: la dedicación no da un número fijo. Las horas contratadas son el número exacto de cada docente, y el que se tiene en el registro.

**Alternativas.** Restar de la dedicación, como decía el glosario: un docente a tiempo parcial tomaría 19 h aunque tuviera menos contratadas, y uno a tiempo completo con 20 h contratadas tomaría 40.

**Consecuencias.** RF-25 no cambia: la sugerencia muestra la dedicación y las horas disponibles, y la dedicación no ordena por sí sola. Que las horas contratadas cuadren con la dedicación lo sigue revisando la validación del distributivo (F07).

**Documentos que hay que poner al día.** El glosario, ya actualizado, junto con el término nuevo «Candidato».

## D-76. Roles no consulta a Académico al guardar la carrera del coordinador

- **Estado:** Aceptada · **Fecha:** 2026-10-31 · **Decidido por:** autor de la tesis (aprobada en el plan de F06-H05)
- **Origen:** `specs/030-carrera-coordinador/research.md` §2
- **Modifica a:** —

**Decisión.** Roles valida el formato del código de la carrera del coordinador, pero no comprueba que exista en Académico. La pantalla «Roles de usuarios» solo ofrece las carreras vigentes.

**Por qué.** Roles corre en el proceso `identidad`, que no tiene `:plataforma:eventos`, ni clave de proceso, ni la URL de `datos`. El filtro falla cerrado: un código que no es de ninguna carrera no coincide con nada, y el coordinador no ve datos. Solo se llega ahí llamando a la API a mano, con permiso de Administrador.

**Alternativas.**
- **Una consulta de `identidad` a `datos`:** suma un secreto `MDD_CLAVE_PROCESO_IDENTIDAD` y obliga a correr `preparar.sh` en el equipo, para validar un dato que la pantalla ya ofrece bien.
- **Que Académico avise a Roles de sus carreras por eventos, como con los periodos:** las carreras que ya existen no tienen evento, así que haría falta una carga inicial.

**Consecuencias.** Roles guarda un código sin comprobar. Si llega una gestión real de usuarios, esta validación se puede revisar.

**Documentos que hay que poner al día.** Ninguno.

## D-77. Las horas semanales admiten hasta dos decimales

- **Estado:** Aceptada · **Fecha:** 2026-10-31 · **Decidido por:** autor de la tesis (aprobada en el plan de F06-H03)
- **Origen:** `specs/031-proponer-docente/research.md` §10
- **Modifica a:** `Horas` en el modelo táctico de `sdd/modelo_dominio.md`, que decía «Horas semanales enteras y no negativas»

**Decisión.** Las horas semanales son no negativas y admiten hasta dos decimales.

**Por qué.** La oferta ya las admite (F03-H03): un paralelo puede tener 2,5 h. Una propuesta las hereda, así que el modelo tenía que decir lo mismo.

**Alternativas.** Redondear las horas de la oferta: cambiaría un dato que el autor ya aprobó, y los totales dejarían de cuadrar.

**Consecuencias.** Las horas se comparan con `BigDecimal`, también las disponibles. Las horas contratadas siguen enteras.

**Documentos que hay que poner al día.** El modelo táctico, ya actualizado.

## D-78. F06-H03 va antes que F07-H02, y mientras tanto una propuesta sin horas suficientes se guarda con un aviso

- **Estado:** Aceptada · **Fecha:** 2026-10-31 · **Decidido por:** autor de la tesis
- **Origen:** especificación de F06-H03
- **Modifica a:** las dependencias de los issues #48 y #51, que eran circulares

**Decisión.** Primero se propone (F06-H03), y después se bloquea (F07-H02). Mientras tanto, proponer a un docente sin horas suficientes se guarda con un aviso.

**Por qué.** F07-H02 valida asignaciones, y sin F06-H03 no hay ninguna que validar.

**Alternativas.** Bloquear ya en F06-H03: adelanta parte de F07-H02, y habría que rehacerla con la normativa y el control de concurrencia.

**Consecuencias.** Entre las dos historias se puede pasar de las horas contratadas, solo con un aviso. F07-H02 lo convierte en bloqueo, con el caso de dos coordinadores a la vez.

**Documentos que hay que poner al día.** Ninguno.
