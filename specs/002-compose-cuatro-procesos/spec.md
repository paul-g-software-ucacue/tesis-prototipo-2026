# Feature Specification: Despliegue con Docker Compose en 4 procesos

**Feature Branch**: `002-compose-cuatro-procesos`

**Created**: 2026-10-12

**Status**: Draft

**Input**: User description: "Especifica la historia B00-H02. Léela de su issue con gh issue list --search "B00-H02 in:title" y gh issue view <número>. Usa la historia y sus escenarios tal como están, sin inventar requisitos, y respeta .specify/memory/constitution.md y docs/decisiones.md"

**Origen**: issue #19 · B00-H02 · sub-issue de #2 (B00 · Base técnica de la ola 0) · milestone «Ola 0 · Base técnica» · etiquetas `tipo:historia`, `prioridad:must`, `modulo:transversal` · tamaño propuesto M · depende de #18 (B00-H01, cerrada).

**Requerimientos que cubre**: ADR-006 · ADR-008 · RNF-POR-01 · RNF-POR-02 · R-19. **Decisiones tomadas en esta historia** (`docs/decisiones.md`): D-34, D-35 y D-36.

**Historia (del issue, con la VM de D-36)**: Como autor, quiero levantar los 4 procesos, el API Gateway y PostgreSQL con un solo `docker compose up`, para comprobar desde el inicio que el perfil consolidado cabe en la VM de 8 GiB.

## Clarifications

### Session 2026-10-12

- Q: El inicio de sesión llega con B00-H03, que depende de esta historia. ¿Cómo se cumple la medición «después de iniciar sesión» en B00-H02? → A: B00-H02 mide con todo levantado y sano, después de un calentamiento que llama a la salud de cada proceso. La medición después de iniciar sesión pasa a B00-H03 (D-34). Los issues #19 y #20 se editaron.
- Q: (análisis, hallazgo I2) La especificación decía «4 GB» y los contratos «4 GiB». ¿Qué umbral vale? → A: El autor duplicó las limitaciones del hardware: la VM pasa a 4 vCPU y 8 GiB, R-19 exige menos de 8 GiB con alerta en 7 GiB, y los límites por contenedor se duplican (D-36).
- Q: Los 12 servicios todavía no existen. ¿Qué servicios aplican migraciones en B00-H02? → A: B00-H02 crea los 12 servicios desde la plantilla, sin lógica de negocio, cada uno con su esquema y una primera migración mínima sin tablas de negocio, y los 4 procesos los ensamblan según ADR-006 (D-35).

> **Nota sobre el lenguaje de esta especificación.** Igual que B00-H01, es una historia de la base técnica: su usuario es el autor y su objeto es el despliegue que ya fijan la constitución (sección 3) y ADR-006. Los nombres de herramientas (Docker Compose, PostgreSQL, Flyway, `docker stats`) aparecen porque los trae la historia; la especificación no elige ninguna tecnología nueva.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus tres escenarios se agrupan en tres partes que se pueden probar por separado. Todas son necesarias para cerrar la historia (`prioridad:must`); la prioridad solo indica el orden en que conviene construirlas.

### User Story 1 - Todo el perfil consolidado se levanta con un comando (Priority: P1)

Como autor, quiero que un solo comando levante los 4 procesos (identidad, datos, distributivo y salida), el API Gateway y PostgreSQL, y que todos queden sanos, para tener desde la ola 0 el mismo perfil consolidado que se desplegará (ADR-006, RNF-POR-01).

**Why this priority**: Es la base de las otras dos partes: sin los contenedores levantados no se puede medir la memoria ni aplicar migraciones. También desbloquea B00-H03, B00-H05 y B00-H08.

**Independent Test**: En un clon limpio del repositorio, con el archivo de variables de entorno de ejemplo, ejecutar el comando y comprobar que los 6 contenedores informan estado sano.

**Acceptance Scenarios**:

```gherkin
Escenario: Todo se levanta con un comando
  Dado el repositorio recién clonado y el archivo de variables de entorno de ejemplo
  Cuando se ejecuta docker compose up
  Entonces los 4 procesos, el API Gateway y PostgreSQL quedan sanos según su healthcheck
```

---

### User Story 2 - El perfil consolidado cabe en la memoria de la VM (Priority: P1)

Como autor, quiero medir el consumo de memoria de los contenedores levantados con sus límites, y registrarlo como evidencia, para comprobar en la ola 0 que el perfil consolidado cabe en la VM de 8 GiB (R-19, RNF-POR-02, D-36).

**Why this priority**: R-19 es un riesgo abierto que puede cambiar la arquitectura: si no cabe, la alternativa prevista es reemplazar el API Gateway (ADR-008). Conviene saberlo antes de construir sobre esta base.

**Independent Test**: Con todo levantado y después del calentamiento, medir con `docker stats` y comprobar que la suma queda por debajo de 8 GiB; la medición queda registrada.

**Acceptance Scenarios**:

```gherkin
Escenario: Cabe en la memoria de la VM
  Dado los contenedores levantados con su límite de memoria
  Cuando se mide con docker stats después de un calentamiento que llama a la salud de cada proceso
  Entonces el consumo total queda por debajo de 8 GiB
  Y la medición se registra como evidencia de R-19
```

> **Cambios respecto del issue #19:**
> - D-34 (2026-10-12): la medición «después de iniciar sesión» pasó a B00-H03 (#20), que crea el inicio de sesión. El issue #19 está actualizado.
> - D-36 (2026-10-12): la VM pasa de 4 GB a 8 GiB y el umbral del escenario, de «4 GB» a «8 GiB». El issue #19 se edita con la aprobación del autor.

---

### User Story 3 - Un esquema por servicio (Priority: P2)

Como autor, quiero que cada servicio aplique sus migraciones solo en su propio esquema de PostgreSQL, para que ningún servicio comparta tablas con otro (principio V, ERS 8.5, ADR-006).

**Why this priority**: Fija la regla de persistencia que usarán todas las historias con datos. Depende de que PostgreSQL y los procesos ya se levanten (historia 1).

**Independent Test**: Con PostgreSQL levantado y las migraciones aplicadas, comprobar que cada esquema contiene solo las tablas de su servicio.

**Acceptance Scenarios**:

```gherkin
Escenario: Un esquema por servicio
  Dado PostgreSQL levantado
  Cuando cada servicio aplica sus migraciones de Flyway
  Entonces cada servicio crea sus tablas solo en su propio esquema
```

---

### Edge Cases

- **PostgreSQL tarda en estar listo**: los procesos no se declaran sanos mientras no puedan conectarse a la base; el comando no termina con contenedores en estado no sano por una carrera de arranque.
- **Falta el archivo de variables de entorno**: el comando falla con un mensaje que dice qué archivo falta, en lugar de levantar contenedores con valores vacíos.
- **Segunda ejecución sobre datos existentes**: al volver a levantar todo, las migraciones ya aplicadas no se repiten y todo vuelve a quedar sano.
- **Un contenedor supera su límite de memoria**: queda visible como no sano o reiniciado; no consume memoria de los demás sin control.
- **Consumo entre 7 GiB y 8 GiB**: cumple el escenario, pero es el disparador de R-19 (más de 7 GiB en uso, D-36); se registra como alerta en la evidencia.
- **Consumo de 8 GiB o más**: el escenario falla; se registra una decisión nueva en `docs/decisiones.md` (la alternativa prevista en R-19 y ADR-008 es Nginx en lugar del API Gateway de Spring) y se enmienda la constitución si hace falta.
- **Una migración apunta al esquema de otro servicio**: la comprobación de esquemas lo detecta y falla.
- **Medición antes de que exista el inicio de sesión**: B00-H02 mide después de un calentamiento; la medición con una sesión real es de B00-H03 (D-34).
- **Servicio sin tablas de negocio**: su primera migración no crea tablas de negocio, pero el registro de migraciones del servicio queda en su propio esquema; la comprobación de esquemas lo verifica igual.

## Requirements *(mandatory)*

### Functional Requirements

**Arranque con un comando**

- **FR-001**: Desde un clon limpio del repositorio y con el archivo de variables de entorno de ejemplo, un solo `docker compose up` DEBE levantar todo el perfil consolidado, sin pasos previos de compilación ni de configuración manual.
- **FR-002**: El perfil consolidado DEBE estar formado exactamente por los 4 procesos de ADR-006 (identidad, datos, distributivo y salida), el API Gateway y PostgreSQL.
- **FR-003**: Cada uno de los 6 contenedores DEBE tener un *healthcheck*, y solo DEBE informar estado sano cuando está listo para atender; un proceso, además, cuando puede conectarse a PostgreSQL.
- **FR-004**: Los procesos DEBEN esperar a que PostgreSQL esté sano antes de arrancar.
- **FR-005**: El archivo de variables de entorno de ejemplo DEBE estar versionado, funcionar en local sin editarse y contener solo valores ficticios, sin contraseñas ni secretos reales (principio IX); el archivo de variables real NO DEBE entrar al repositorio.

**Memoria (R-19)**

- **FR-006**: Cada contenedor DEBE tener un límite de memoria explícito, y cada JVM su propio límite (constitución, sección 3; ADR-008).
- **FR-007**: El consumo total de los contenedores, medido con `docker stats`, DEBE quedar por debajo de 8 GiB (escenario «Cabe en la memoria de la VM»; D-36).
- **FR-008**: La medición DEBE hacerse después de un calentamiento que llame a la salud de cada proceso (D-34) y registrarse como evidencia de R-19, con el consumo de cada contenedor, el total, los límites configurados, la fecha y el commit medido.

**Esquemas**

- **FR-009**: Cada servicio DEBE tener su propio esquema en PostgreSQL y aplicar sus migraciones de Flyway solo en ese esquema (constitución, sección 3; principio V).
- **FR-010**: DEBEN existir los 12 servicios de la ERS 8.5, creados desde la plantilla de servicio y sin lógica de negocio, cada uno con una primera migración mínima que no crea tablas de negocio, y cada proceso DEBE ensamblar sus servicios según ADR-006 (D-35).
- **FR-011**: DEBE existir una comprobación automática de que, después de las migraciones, cada esquema contiene solo las tablas de su servicio y ningún servicio crea tablas fuera de su esquema.

**Compatibilidad**

- **FR-012**: El despliegue DEBE funcionar con Docker Compose sobre Ubuntu Server, compatible con aaPanel (RNF-POR-01).

### Key Entities

- **Proceso**: aplicación que ensambla varios servicios (ADR-006): identidad (1, 2 y 7), datos (3, 4, 11 y 12), distributivo (5, 6 y 10) y salida (8 y 9).
- **API Gateway**: punto de entrada único hacia los 4 procesos.
- **Contenedor**: unidad que levanta el comando; tiene un *healthcheck* y un límite de memoria.
- **Servicio**: uno de los 12 servicios de la ERS 8.5. En esta historia se crea vacío, con su esquema y su primera migración (D-35).
- **Esquema**: espacio propio de un servicio en PostgreSQL; ningún otro servicio crea ni lee sus tablas.
- **Medición de R-19**: registro del consumo de memoria por contenedor y total, con su contexto (fecha, commit, límites).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Desde un clon limpio, los 6 contenedores quedan sanos sin intervención manual, y lo hacen dentro de los 180 segundos que el despliegue del VPS espera a que un proceso quede sano (`deploy/desplegar.sh`), una vez construidas las imágenes.
- **SC-002**: El consumo total medido queda por debajo de 8 GiB, y la evidencia registra si queda por debajo o por encima de 7 GiB (disparador de R-19).
- **SC-003**: Después de las migraciones, el 100 % de las tablas de cada servicio está en su propio esquema y no hay tablas de servicios en ningún otro esquema.
- **SC-004**: Una segunda ejecución del comando sobre los mismos datos deja los 6 contenedores sanos sin volver a aplicar migraciones.
- **SC-005**: El archivo de variables de entorno de ejemplo no contiene secretos reales: la etapa de secretos de la CI pasa.

## Assumptions

- Los 12 servicios se crean desde `backend/plantillas/servicio` con los nombres de ADR-012 (`servicios/usuarios`, `servicios/roles`, …, `servicios/normativa`); su lógica, sus contratos y sus tablas de negocio llegan con sus historias (D-35).
- El perfil consolidado es el de ADR-006; los procesos se crean desde `backend/plantillas/aplicacion` con los nombres de ADR-012 (`apps/identidad`, `apps/datos`, `apps/distributivo` y `apps/salida`), que también espera `cd.yml`.
- El API Gateway es Spring Cloud Gateway (constitución, sección 3; ADR-008). En esta historia basta con que arranque y quede sano; el enrutamiento autenticado y la validación del JWT son de B00-H03.
- El *healthcheck* de los procesos se basa en la salud que expone Spring Boot Actuator (ADR-008, punto 11), que también usará `deploy/desplegar.sh` en B00-H08.
- Solo el API Gateway publica un puerto en la máquina anfitriona; los procesos y PostgreSQL solo son accesibles dentro de la red de Compose (mínimo privilegio, principio IX).
- La SPA (B00-H06), el proxy HTTPS del VPS (`deploy/Caddyfile`), la publicación en Docker Hub y el despliegue en el VPS (B00-H08) quedan fuera de esta historia.
- El almacenamiento de archivos separado de la base (RNF-POR-01) se agrega cuando una historia guarde archivos; en esta historia ningún servicio los guarda.
- En la máquina del autor hay Docker Engine con Docker Compose v2 (paquetes de Fedora), además de Podman. El comando se valida allí, y la medición de referencia de R-19 se hace en GitHub Actions con Docker.
- La VM y el VPS de demostración tienen 4 vCPU y 8 GiB (D-36). La memoria se mide en GiB (1 GiB = 1 024 MiB), la unidad en que la informa `docker stats`.
- Las pruebas y el archivo de ejemplo usan solo datos ficticios (principios VIII y IX).
