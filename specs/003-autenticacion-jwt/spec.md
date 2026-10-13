# Feature Specification: Autenticación JWT de punta a punta

**Feature Branch**: `003-autenticacion-jwt`

**Created**: 2026-10-13

**Status**: Draft

**Input**: User description: "Especifica la historia B00-H03. Léela de su issue con gh issue list --search "B00-H03 in:title" y gh issue view <número>. Usa la historia y sus escenarios tal como están, sin inventar requisitos, y respeta .specify/memory/constitution.md y docs/decisiones.md"

**Origen**: issue #20 · B00-H03 · sub-issue de #2 (B00 · Base técnica de la ola 0) · milestone «Ola 0 · Base técnica» · etiquetas `tipo:historia`, `prioridad:must`, `modulo:gateway`, `RF-01` · tamaño propuesto M · depende de #19 (B00-H02, cerrada).

**Requerimientos que cubre**: RF-01 · ADR-008 · RNF-SEG-01 · R-19. **Decisiones que aplica** (`docs/decisiones.md`): D-34 (segunda medición de R-19, después de iniciar sesión), D-36 (equipo de 4 núcleos y 8 GiB) y ADR-013 (un rol de base por proceso). **Decisiones tomadas en esta historia:** D-37, D-38 y D-39.

**Historia (tal como está en el issue)**: Como autor, quiero que el API Gateway y cada proceso validen el JWT RS256 que emite Usuarios, para que todas las historias partan de un usuario autenticado con sus roles.

## Clarifications

### Session 2026-10-13

- Q: ¿Qué parte del inicio de sesión entra en B00-H03 y qué queda para F05-H01 (#43)? → A: Usuarios expone un inicio de sesión mínimo: recibe usuario y contraseña de un usuario de prueba ficticio, con la contraseña guardada con Argon2id, y devuelve el token firmado. La pantalla, la gestión de usuarios y los mensajes para el usuario final quedan en F05-H01 (D-37).
- Q: El catálogo de códigos y el `@RestControllerAdvice` son de B00-H04 (#21); ¿cómo se cumple aquí el «modelo de error común»? → A: B00-H03 devuelve el 401 con el formato del principio XIII y registra los primeros códigos del catálogo. B00-H04 arma después el catálogo completo y el advice, y absorbe esos códigos (D-38).
- Q: Los procesos no tienen endpoints de negocio; ¿qué servicio de cada proceso responde en la prueba de punta a punta? → A: Un servicio de cada proceso expone «sesión actual», que devuelve el usuario y los roles del token, con su contrato OpenAPI mínimo; en identidad es `usuarios` (D-39).

> **Nota sobre el lenguaje de esta especificación.** Es una historia de la base técnica: su usuario es el autor y su objeto es la autenticación que ya fijan la constitución (sección 3, principios IX y XIII) y ADR-008. Los nombres JWT, RS256 y `docker stats` aparecen porque los trae la historia; la especificación no elige ninguna tecnología nueva.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus cuatro escenarios se agrupan en tres partes que se pueden probar por separado. Todas son necesarias para cerrar la historia (`prioridad:must`); la prioridad solo indica el orden en que conviene construirlas.

### User Story 1 - Sin un token válido no se entra (Priority: P1)

Como autor, quiero que el API Gateway rechace toda petición a la API que no traiga un token válido, y que cada proceso también lo rechace aunque la petición le llegue por otro camino, para que ninguna historia tenga que defenderse sola de un usuario no autenticado (RF-01, RNF-SEG-01).

**Why this priority**: Es la regla de seguridad que heredan todas las historias. Sin ella, cualquier endpoint que se agregue después quedaría abierto.

**Independent Test**: Con el perfil consolidado levantado, llamar a la API sin token, con un token de firma alterada y con un token vencido, y comprobar que las tres respuestas son 401 con el modelo de error común y la misma descripción.

**Acceptance Scenarios**:

```gherkin
Escenario: Petición sin token
  Dado una petición a /api sin token
  Cuando llega al API Gateway
  Entonces responde 401 con el modelo de error común

Escenario: Token alterado o vencido
  Dado un token con la firma alterada o vencido
  Cuando se usa en una petición
  Entonces la respuesta es 401
  Y la descripción no dice qué falló en el token
```

---

### User Story 2 - Un usuario autenticado llega a cualquier proceso con sus roles (Priority: P1)

Como autor, quiero que un usuario de prueba inicie sesión en Usuarios, reciba un token firmado y, con ese token, llegue a un servicio de cualquiera de los 4 procesos, que valida la firma y sabe quién es y qué roles tiene, para que las historias siguientes solo tengan que leer el usuario y sus roles.

**Why this priority**: Es el camino feliz que usarán todas las historias con datos. Junto con la parte 1, completa la autenticación de punta a punta.

**Independent Test**: Con el perfil consolidado levantado, iniciar sesión con el usuario de prueba, llamar con su token a un servicio de cada proceso a través del API Gateway, y comprobar que cada uno responde con el usuario y los roles del token.

**Acceptance Scenarios**:

```gherkin
Escenario: Token válido
  Dado un usuario de prueba que inició sesión
  Cuando llama a un servicio de cualquier proceso con su token
  Entonces el proceso valida la firma RS256
  Y obtiene del token el usuario y sus roles
```

---

### User Story 3 - Sigue cabiendo en la memoria después de iniciar sesión (Priority: P2)

Como autor, quiero repetir la medición de memoria de B00-H02 después de que un usuario de prueba inicia sesión, para saber si la autenticación cambia el consumo del perfil consolidado (R-19, D-34).

**Why this priority**: Es la segunda medición que D-34 dejó para esta historia. Depende de que el inicio de sesión ya funcione (parte 2).

**Independent Test**: Con todo levantado, iniciar sesión con el usuario de prueba, medir con `docker stats` y comprobar que el total queda por debajo de 8 GiB; la medición queda registrada junto a la de B00-H02.

**Acceptance Scenarios**:

```gherkin
Escenario: Cabe en la memoria del equipo después de iniciar sesión
  Dado los contenedores levantados con su límite de memoria
  Cuando se mide con docker stats después de que un usuario de prueba inicia sesión
  Entonces el consumo total queda por debajo de 8 GiB
  Y la medición se registra como evidencia de R-19
```

> **Cambio respecto del issue #20** (D-36, 2026-10-12): el umbral pasa de «4 GB» a «8 GiB», porque el equipo de despliegue pasó a tener 4 núcleos y 8 GiB de RAM. El issue #20 está actualizado.

---

### Edge Cases

- **Token con la firma alterada, vencido, mal formado o firmado con otra llave**: todos responden 401 con la misma descripción y el mismo código; la respuesta no dice cuál de las comprobaciones falló (escenario «Token alterado o vencido», principio XIII).
- **Token que pide un algoritmo distinto de RS256, o ninguno**: se rechaza con 401, igual que un token alterado.
- **Petición que llega directo a un proceso, sin pasar por el API Gateway**: el proceso valida el token por su cuenta y la rechaza igual; no confía en que el API Gateway ya lo validó.
- **Salud de los contenedores**: la comprobación de salud de cada contenedor sigue funcionando sin token, porque la usan el *healthcheck* y el despliegue (B00-H02, `deploy/desplegar.sh`).
- **Credenciales incorrectas al iniciar sesión**: la respuesta no dice cuál de los datos falló; el mensaje para el usuario final y la pantalla son de F05-H01.
- **Usuario sin roles**: el token se emite igual y el proceso obtiene una lista de roles vacía; qué puede hacer cada rol es de F05-H02 (matriz D-16).
- **Token vencido durante una sesión larga**: la siguiente petición responde 401; renovar la sesión no es parte de esta historia.
- **Registros**: ni el token ni la contraseña aparecen en los registros, en las respuestas de error ni en la evidencia (principio XIII, RNF-SEG-06).
- **Consumo entre 7 GiB y 8 GiB, o de 8 GiB o más**: igual que en B00-H02: alerta de R-19, o falla del escenario y decisión nueva (D-36).

## Requirements *(mandatory)*

### Functional Requirements

**Emisión del token**

- **FR-001**: Usuarios DEBE emitir, a un usuario de prueba que inicia sesión con credenciales correctas, un token JWT firmado con RS256 que contiene el nombre de usuario como identificador, sus roles y una fecha de vencimiento (RF-01, ADR-008). El inicio de sesión de esta historia es mínimo: solo para usuarios de prueba ficticios, sin pantalla ni gestión de usuarios, que son de F05-H01 (D-37).
- **FR-002**: Solo Usuarios DEBE poder firmar tokens. El API Gateway y los procesos DEBEN validar con la llave pública, sin tener acceso a la llave privada. La llave privada NO DEBE entrar al repositorio (principio IX).

**Validación**

- **FR-003**: El API Gateway DEBE rechazar con 401 toda petición a la API (`/api`) que no traiga un token válido, antes de reenviarla a un proceso. La única excepción es el inicio de sesión, que por definición se hace sin token (D-37).
- **FR-004**: Cada uno de los 4 procesos DEBE validar por su cuenta la firma RS256 y el vencimiento del token de cada petición a su API, aunque la petición haya pasado por el API Gateway, y DEBE obtener del token el identificador del usuario y sus roles.
- **FR-005**: Un token sin firma válida, vencido, mal formado o con un algoritmo distinto de RS256 DEBE rechazarse con 401. Todas las causas DEBEN responder con el mismo código y la misma descripción, que no dice qué falló.
- **FR-006**: Toda respuesta 401 DEBE usar el modelo de error común del principio XIII (`titulo`, `descripcion` y `codigo`), con un código estable del catálogo y una descripción que dice qué puede hacer el usuario (iniciar sesión de nuevo). Los códigos que crea esta historia son los primeros del catálogo; B00-H04 arma el catálogo completo y los absorbe (D-38).
- **FR-007**: Un servicio de cada uno de los 4 procesos DEBE ofrecer la consulta «sesión actual», que solo responde a peticiones autenticadas y devuelve el usuario y los roles obtenidos del token, con su contrato publicado antes de implementarla (principio IV). En identidad, el servicio es Usuarios (D-39).
- **FR-008**: La comprobación de salud de cada contenedor DEBE seguir accesible sin token, solo para el *healthcheck* (B00-H02).

**Seguridad de los datos**

- **FR-009**: Ni el token, ni la contraseña, ni la causa por la que se rechazó un token DEBEN aparecer en las respuestas; el token y la contraseña tampoco en los registros (principio XIII, RNF-SEG-06).
- **FR-010**: La contraseña del usuario de prueba DEBE guardarse con un algoritmo de derivación de contraseñas, nunca en claro (principio IX, RNF-SEG-02), y el usuario de prueba y sus roles DEBEN ser ficticios.

**Memoria (R-19)**

- **FR-011**: El consumo total de los contenedores, medido con `docker stats` después de que un usuario de prueba inicia sesión, DEBE quedar por debajo de 8 GiB (D-36), y la medición DEBE registrarse como evidencia de R-19 con el mismo formato que la de B00-H02 (D-34).

### Key Entities

- **Usuario de prueba**: usuario ficticio con credenciales y uno o más roles, que existe para probar la autenticación de punta a punta. No es un usuario real ni tiene datos personales.
- **Token (JWT)**: credencial firmada por Usuarios con RS256. Contiene el identificador del usuario, sus roles y su vencimiento. Quien lo recibe comprueba la firma con la llave pública.
- **Par de llaves**: la llave privada, que solo tiene Usuarios para firmar, y la llave pública, con la que el API Gateway y los procesos validan.
- **Sesión actual**: lo que un proceso sabe del usuario que hace la petición, obtenido del token: su identificador y sus roles.
- **Error 401**: respuesta con el modelo de error común (principio XIII) y un código estable para «sin sesión o token inválido».
- **Medición de R-19 después de iniciar sesión**: segunda medición de memoria del perfil consolidado, con el mismo formato que la de B00-H02.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las peticiones a la API sin token, con un token alterado, vencido, mal formado o firmado con otra llave recibe 401 con el modelo de error común, tanto en el API Gateway como al llamar directo a un proceso.
- **SC-002**: Las respuestas a todas las causas de rechazo de un token son idénticas en estado, código y descripción: ninguna permite saber qué comprobación falló.
- **SC-003**: Con el token del usuario de prueba, un servicio de cada uno de los 4 procesos responde con el usuario y los roles del token, a través del API Gateway.
- **SC-004**: Después de iniciar sesión, el consumo total medido queda por debajo de 8 GiB, y la evidencia registra si queda por debajo o por encima de 7 GiB y la diferencia con la medición de B00-H02.
- **SC-005**: La llave privada, el token y la contraseña del usuario de prueba no aparecen en el repositorio, en las respuestas ni en los registros: la etapa de secretos de la CI pasa y una búsqueda en los registros del perfil no los encuentra.
- **SC-006**: Los 6 contenedores siguen quedando sanos con un solo `docker compose up`, sin token, como en B00-H02.

## Assumptions

- La validación del token la hacen el API Gateway y cada proceso con Spring Security y JWT RS256, como fija la constitución (sección 3) y ADR-008; la contraseña se guarda con Argon2id, también de la sección 3.
- Los roles del token son los que tiene el usuario de prueba. Qué permite cada rol (matriz D-16), crear roles y asignarlos son de F05-H02 (#44); esta historia solo los transporta en el token.
- La pantalla de inicio de sesión, la redirección al inicio de sesión y los mensajes para el usuario final son de F05-H01 (#43) y de la SPA (B00-H06).
- El token tiene una vigencia limitada y configurable; renovarlo y cerrar la sesión quedan fuera de esta historia.
- El API Gateway reenvía el token al proceso y el proceso lo vuelve a validar (FR-004): un proceso no confía en que el API Gateway ya lo validó.
- La segunda medición de R-19 usa el script y el formato de B00-H02 (`deploy/medir_memoria.sh`, `contracts/medicion-r19.md`), con un paso de inicio de sesión antes de medir.
- La llave privada de local y de las pruebas es ficticia y no se versiona; la del VPS se configura en B00-H08.
- Las pruebas y el usuario de prueba usan solo datos ficticios (principios VIII y IX).
