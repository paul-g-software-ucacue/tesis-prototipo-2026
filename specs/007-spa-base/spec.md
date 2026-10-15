# Feature Specification: SPA base con el cliente generado

**Feature Branch**: `007-spa-base`

**Created**: 2026-10-15

**Status**: Draft

**Input**: User description: "Especifica la historia B00-H06. Léela de su issue con gh issue list --search "B00-H06 in:title" y gh issue view <número>. Usa la historia y sus escenarios tal como están, sin inventar requisitos, y respeta .specify/memory/constitution.md y docs/decisiones.md"

**Origen**: issue #23 · B00-H06 · sub-issue de #2 (B00 · Base técnica de la ola 0) · milestone «Ola 0 · Base técnica» · etiquetas `tipo:historia`, `prioridad:must`, `modulo:frontend` · tamaño propuesto M · depende de #20 (B00-H03) y #21 (B00-H04), cerradas · bloquea a #27 (F01-H01) y #35 (F03-H01).

**Requerimientos que cubre**: ADR-008 · RNF-USA-01 · RNF-COM-01. **Decisiones que aplica** (`docs/decisiones.md`): D-29 (el escenario de ESLint pasa a esta historia y los jobs de frontend corren solo si existe `frontend/package.json`), D-31 (los 10 checks de la rama principal), D-37 (inicio de sesión mínimo de B00-H03), ADR-014 (token RS256 en la cabecera `Authorization`), ADR-015 (contratos en `contratos/`) y ADR-016 (modelo de error común).

**Historia (tal como está en el issue)**: Como autor, quiero el esqueleto de la SPA con inicio de sesión, navegación y el cliente generado desde OpenAPI, para que las pantallas de cada historia se agreguen sobre una base común.

**Decisiones tomadas en esta historia:** D-50, D-51 y D-52.

## Clarifications

### Session 2026-10-15

- Q: ¿La protección de las rutas internas (redirigir al inicio de sesión a quien no tiene sesión) entra en esta historia o queda para F05-H01? → A: Entra en esta historia. La base protege las rutas internas: sin sesión, o con un 401, se vuelve al inicio de sesión. F05-H01 comprueba su escenario «Acceso sin sesión» sobre esta base y agrega lo suyo (usuarios reales y permisos) (D-50).
- Q: ¿Dónde guarda la SPA el token? → A: Solo en memoria, como recomienda OWASP. Al recargar la página se vuelve a iniciar sesión (D-51).
- Q: ¿La SPA entra al perfil consolidado de `compose.yaml` en esta historia o con B00-H08? → A: En esta historia: un contenedor más, con Nginx que sirve la SPA y envía `/api` al API Gateway, accesible en local y medido en R-19. B00-H08 solo publica su imagen (D-52).

> **Nota sobre el lenguaje de esta especificación.** Es una historia de la base técnica: su usuario es el autor, y su objeto es la base del frontend que fija la constitución (§3: SPA en español, cliente generado desde OpenAPI, WCAG 2.1 AA). Los nombres «openapi-typescript», «axe-core», «ESLint» y «GitHub Actions» aparecen porque los traen los escenarios del issue; la especificación no elige ninguna tecnología nueva.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus cuatro escenarios son cuatro partes que se pueden probar por separado. Todas son necesarias para cerrar la historia (`prioridad:must`); la prioridad solo indica el orden en que conviene construirlas.

### User Story 1 - Un cambio de contrato rompe la compilación del frontend (Priority: P1)

Como autor, quiero que los tipos y el cliente de la SPA salgan de los contratos de `contratos/`, para que un cambio de contrato que afecta a la SPA se detecte al compilar y no en producción (principio IV, ADR-015).

**Why this priority**: Es la base sobre la que se escribe cada pantalla: sin ella, cada historia escribiría sus tipos a mano y podría desalinearse del backend.

**Independent Test**: Cambiar, en una copia del contrato, el nombre o el tipo de un campo que la SPA usa (por ejemplo, el token del inicio de sesión) y comprobar que la compilación del frontend falla, y que vuelve a pasar con el contrato original.

**Acceptance Scenarios**:

```gherkin
Escenario: Un cambio de contrato rompe la compilación
  Dado el cliente generado con openapi-typescript
  Cuando un contrato cambia un campo que la SPA usa
  Entonces la compilación del frontend falla
```

---

### User Story 2 - El usuario ve los errores en español y sin detalles técnicos (Priority: P1)

Como autor, quiero que la SPA muestre de cualquier respuesta de error el `titulo` y la `descripcion` del modelo común, en español, y nunca un mensaje técnico, para que el usuario sepa qué hacer sin ver trazas ni códigos internos (RNF-USA-01, principio XIII).

**Why this priority**: Es el mecanismo que usarán todas las pantallas para mostrar errores; el modelo común ya existe en el backend desde B00-H04.

**Independent Test**: Simular respuestas de error con el modelo común (401 de credenciales inválidas, 400 con `detalles`, 500 con `idIncidente`, 503 del API Gateway) y comprobar que la SPA muestra el `titulo` y la `descripcion`, y que no muestra el código, la traza, el estado HTTP ni el texto de una excepción.

**Acceptance Scenarios**:

```gherkin
Escenario: Error mostrado al usuario
  Dado una respuesta con el modelo de error común
  Cuando la SPA la recibe
  Entonces muestra el titulo y la descripcion en español
  Y nunca muestra un mensaje técnico
```

---

### User Story 3 - La pantalla de inicio de sesión es accesible (Priority: P1)

Como autor, quiero que la pantalla de inicio de sesión no tenga fallas de WCAG 2.1 AA según axe-core, para que la base de la que parten todas las pantallas ya cumpla la accesibilidad que pide la constitución (§3, RNF-COM-01).

**Why this priority**: Es la primera pantalla y el molde de las demás; corregir la accesibilidad después en cada pantalla cuesta más.

**Independent Test**: Analizar la pantalla de inicio de sesión con axe-core, con las reglas de WCAG 2.1 AA, en su estado inicial y con un error mostrado, y comprobar que no hay fallas.

**Acceptance Scenarios**:

```gherkin
Escenario: Accesibilidad de la base
  Dado la pantalla de inicio de sesión
  Cuando se analiza con axe-core
  Entonces no tiene fallas de WCAG 2.1 AA
```

---

### User Story 4 - ESLint bloquea la fusión de un pull request con errores (Priority: P2)

Como autor, quiero que cada pull request que incluye `frontend/` ejecute ESLint en la etapa de lint, y que la regla de la rama principal impida fusionarlo si ESLint reporta un error, para que el frontend tenga el mismo control de calidad que el backend (D-29, D-31).

**Why this priority**: Los jobs de frontend de la CI ya existen y esperan a que exista `frontend/package.json` (D-29); esta parte los activa.

**Independent Test**: Abrir un pull request que agrega un error de ESLint en `frontend/` y comprobar que el check `1 · lint-frontend` falla y que la rama principal no deja fusionarlo; con el error corregido, el check pasa.

**Acceptance Scenarios**:

```gherkin
Escenario: Lint del frontend en cada pull request
  Dado un pull request hacia la rama principal que incluye frontend/
  Cuando corre la etapa de lint
  Entonces GitHub Actions ejecuta ESLint
  Y la regla de la rama principal impide fusionarlo si ESLint reporta un error
```

---

### Edge Cases

- **Contrato que cambia un campo que la SPA no usa**: la compilación sigue pasando; solo falla lo que la SPA usa.
- **Respuesta de error sin el modelo común** (por ejemplo, el proxy devuelve HTML o el cuerpo está vacío): la SPA muestra un mensaje genérico en español, con la misma forma que un error común, sin el cuerpo ni el estado HTTP.
- **Error sin conexión** (el API Gateway no responde): la SPA muestra un mensaje en español de servicio no disponible, sin el detalle de la red.
- **Error con `detalles`**: la SPA muestra el `titulo` y la `descripcion`, y cada detalle junto a su campo, si la pantalla tiene ese campo.
- **Error técnico con `idIncidente`**: la SPA muestra el identificador para que el usuario lo informe, como dice la descripción del catálogo, sin otro dato técnico.
- **Credenciales inválidas**: la pantalla de inicio de sesión muestra el error de `USUARIOS_CREDENCIALES_INVALIDAS`, que no dice cuál de los dos datos falló (B00-H03).
- **Token vencido o inválido durante la navegación** (401): la SPA cierra la sesión y vuelve al inicio de sesión, con un mensaje en español.
- **Recarga de la página**: el token se pierde (está solo en memoria) y la SPA vuelve al inicio de sesión (D-51).
- **Ruta inexistente en la SPA**: muestra una página «no encontrada» en español, con un enlace a la pantalla de inicio.
- **Pull request que no toca `frontend/`**: los jobs de frontend no corren y no bloquean la fusión (D-29).
- **Error de formato (Prettier) o de tipos en `frontend/`**: también hace fallar la etapa de lint, igual que ESLint (los pasos ya están en `ci.yml`).

## Requirements *(mandatory)*

### Functional Requirements

**Cliente generado (escenario 1)**

- **FR-001**: Los tipos de las operaciones y de los modelos que usa la SPA DEBEN generarse desde los contratos de `contratos/` con openapi-typescript, sin escribirlos a mano (principio IV, constitución §3).
- **FR-002**: La compilación del frontend (verificación de tipos y construcción) DEBE fallar si un contrato cambia el nombre, el tipo o la obligatoriedad de un campo que la SPA usa.
- **FR-003**: La generación DEBE correr antes de compilar en local, en la CI y al construir la imagen, para que nunca se compile con tipos viejos.

**Base de la SPA**

- **FR-004**: La SPA DEBE tener una pantalla de inicio de sesión que use la operación de inicio de sesión de Usuarios que ya existe (D-37), con los campos usuario y contraseña, a través del API Gateway.
- **FR-005**: Después de iniciar sesión, la SPA DEBE mostrar una estructura de navegación común (encabezado, menú y pantalla de inicio) donde las historias siguientes agreguen sus pantallas, con la opción de cerrar sesión. La pantalla de inicio DEBE mostrar el usuario y sus roles, tomados de la sesión actual de Usuarios.
- **FR-006**: Las rutas internas DEBEN estar protegidas: quien abre una ruta interna sin sesión, o recibe un 401 durante la navegación, DEBE volver al inicio de sesión, y después de iniciarla, a la ruta que pidió. F05-H01 comprueba su escenario «Acceso sin sesión» sobre esta protección (D-50).
- **FR-007**: Cada petición a la API DEBE llevar el token en la cabecera `Authorization` (ADR-014). El token DEBE guardarse solo en memoria: NO DEBE escribirse en `localStorage`, `sessionStorage`, cookies ni en la URL. Al recargar la página, el usuario vuelve a iniciar sesión (D-51).
- **FR-008**: Toda la interfaz DEBE estar en español: textos, etiquetas, mensajes y componentes (constitución §3).
- **FR-009**: La SPA DEBE funcionar en las dos últimas versiones de Chrome, Firefox y Edge, desde 1366 px de ancho (RNF-COM-01).

**Errores (escenario 2)**

- **FR-010**: La SPA DEBE mostrar de cualquier respuesta de error con el modelo común su `titulo` y su `descripcion`, y, si vienen, los `detalles` junto a sus campos y el `idIncidente`. NO DEBE mostrar el `codigo`, el estado HTTP, trazas, nombres de clases ni el texto de una excepción (principio XIII, RNF-USA-01).
- **FR-011**: Una respuesta de error sin el modelo común, o un fallo de red, DEBE mostrarse con un mensaje genérico en español, con la misma forma que un error común.
- **FR-012**: La traducción de las respuestas de error a mensajes DEBE estar en un solo lugar de la SPA, para que ninguna pantalla la repita.

**Accesibilidad (escenario 3)**

- **FR-013**: La pantalla de inicio de sesión DEBE pasar el análisis de axe-core con las reglas de WCAG 2.1 AA sin fallas, en su estado inicial y con un error mostrado. El análisis DEBE ser una prueba automática que corre en cada pull request.

**Lint y CI (escenario 4)**

- **FR-014**: La SPA DEBE vivir en `frontend/` con su `package.json`, para que se activen los jobs de frontend de la CI (D-29), y DEBE ofrecer los comandos que esos jobs ya usan: generar los tipos, lint, formato, tipos, pruebas con cobertura y SBOM.
- **FR-015**: La etapa de lint DEBE ejecutar ESLint, y un error de ESLint DEBE hacer fallar el check `1 · lint-frontend`, que la regla de la rama principal ya exige (D-31).
- **FR-016**: Las pruebas del frontend DEBEN cubrir al menos el 60 % de sus líneas, como cualquier módulo (principio VIII, D-32).

**Despliegue**

- **FR-017**: La SPA DEBE construirse como imagen, servida por Nginx (constitución §3), y entrar al perfil consolidado de `compose.yaml` como un contenedor más. Nginx sirve la SPA y envía `/api` al API Gateway, en el mismo origen, y la SPA queda accesible en local con el mismo comando que levanta el perfil (D-52).
- **FR-018**: La memoria del perfil con la SPA DEBE seguir por debajo de la alerta de 7 GiB de R-19 (D-36).

### Key Entities

- **Cliente generado**: los tipos de las operaciones y de los modelos de los contratos, que la SPA usa para llamar a la API.
- **Sesión**: el token del usuario y su vencimiento, con el usuario y los roles de la sesión actual.
- **Mensaje de error**: lo que la SPA muestra de una respuesta de error: `titulo`, `descripcion`, los `detalles` por campo y el `idIncidente`, si vienen.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un cambio en un campo de un contrato que la SPA usa hace fallar la compilación del frontend el 100 % de las veces, comprobado con al menos un cambio de nombre y un cambio de tipo.
- **SC-002**: El 100 % de las respuestas de error probadas (400 con `detalles`, 401, 404, 500 con `idIncidente`, 503, una sin el modelo común y un fallo de red) se muestran con un mensaje en español, sin el código, el estado HTTP ni texto técnico.
- **SC-003**: La pantalla de inicio de sesión tiene 0 fallas de WCAG 2.1 AA en el análisis de axe-core, en su estado inicial y con un error mostrado.
- **SC-004**: Un pull request con un error de ESLint en `frontend/` no se puede fusionar; con el error corregido, el check pasa.
- **SC-005**: El autor inicia sesión con `autor.prueba`, ve su usuario y sus roles en la pantalla de inicio y cierra sesión, en menos de 1 minuto.
- **SC-006**: Las pruebas del frontend cubren al menos el 60 % de sus líneas.
- **SC-007**: Abrir una ruta interna sin sesión lleva al inicio de sesión el 100 % de las veces, y después de iniciarla se llega a la ruta pedida.
- **SC-008**: Después de iniciar sesión, el token no aparece en `localStorage`, `sessionStorage`, cookies ni en la URL.
- **SC-009**: Con el perfil levantado con un solo comando, la SPA responde en local, el inicio de sesión funciona a través de ella, y la memoria total sigue por debajo de la alerta de 7 GiB de R-19.

## Assumptions

- El inicio de sesión de esta historia es el mínimo de B00-H03 (D-37), con el usuario de prueba. Las reglas de F05-H01 (usuarios reales, permisos por rol, matriz de permisos) quedan para esa historia y sus siguientes.
- La SPA llama solo a operaciones que ya existen: el inicio de sesión y la sesión actual de Usuarios. No hay contratos nuevos.
- La SPA llama a la API en el mismo origen, bajo `/api`. En el VPS, Caddy ya envía `/api/*` al API Gateway y lo demás a `frontend:8080` (`deploy/Caddyfile`); en local, el Nginx de la SPA hace lo mismo con `/api` (D-52).
- Los jobs de frontend de `ci.yml` (`lint-frontend`, `test-frontend` y `sbom-frontend`) y `deploy/docker/frontend.Dockerfile` ya existen desde B00-H01; esta historia los activa y los ajusta solo si hace falta.
- El análisis de accesibilidad de esta historia cubre la pantalla de inicio de sesión, como dice el escenario; las demás pantallas principales se analizan en sus historias.
- La versión de Node es Node 24 LTS, la que fija la CI, también en la imagen; el pull request #73 de Dependabot (Node 24 → 26) se cerró (D-53).
- Solo se usan datos ficticios (principios VIII y IX).
