# Feature Specification: Modelo de errores común y catálogo de códigos

**Feature Branch**: `004-modelo-errores`

**Created**: 2026-10-13

**Status**: Draft

**Input**: User description: "Especifica la historia B00-H04. Léela de su issue con gh issue list --search "B00-H04 in:title" y gh issue view <número>. Usa la historia y sus escenarios tal como están, sin inventar requisitos, y respeta .specify/memory/constitution.md y docs/decisiones.md"

**Origen**: issue #21 · B00-H04 · sub-issue de #2 (B00 · Base técnica de la ola 0) · milestone «Ola 0 · Base técnica» · etiquetas `tipo:historia`, `prioridad:must`, `modulo:transversal` · tamaño propuesto M · depende de #18 (B00-H01, cerrada).

**Requerimientos que cubre**: ADR-009 · RNF-USA-01 · RNF-SEG-04. **Decisiones que aplica** (`docs/decisiones.md`): D-38 (los códigos de B00-H03 se absorben sin renombrarlos), ADR-014 (401 uniforme) y ADR-015 (contratos en `contratos/`). **Decisiones tomadas en esta historia:** D-40, D-41 y D-42.

**Historia (tal como está en el issue)**: Como autor, quiero el esquema Error, el catálogo de códigos y un solo @RestControllerAdvice por aplicación, para que todos los errores lleguen con el mismo formato y sin detalles técnicos.

## Clarifications

### Session 2026-10-13

- Q: ¿El API Gateway también entra en «un solo @RestControllerAdvice por aplicación» para sus propios errores? → A: Sí. El API Gateway responde sus propios errores con el modelo común: ruta inexistente (404), proceso que no responde (503) y fallo técnico (500 con `idIncidente`), con un manejador reactivo porque no tiene controladores (D-40).
- Q: Los servicios todavía no tienen errores de dominio; ¿con qué error se demuestra el escenario «Error reconocido»? → A: Con un error de dominio de prueba en el mecanismo común, registrado en un catálogo de prueba, más las credenciales inválidas de Usuarios, que pasan al mecanismo común como primer error real (D-41, D-38).
- Q: Al activar la validación de entrada, ¿el inicio de sesión con datos vacíos pasa a 400 o se mantiene 401? → A: Pasa a 400 con `detalles`, como cualquier campo obligatorio vacío; decir que un campo vino vacío no revela qué usuarios existen (D-42).

> **Nota sobre el lenguaje de esta especificación.** Es una historia de la base técnica: su usuario es el autor y su objeto es el modelo de errores que ya fija el principio XIII de la constitución. Los nombres «esquema Error», «@RestControllerAdvice» e «idIncidente» aparecen porque los trae la historia; la especificación no elige ninguna tecnología nueva.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus tres escenarios son tres partes que se pueden probar por separado. Todas son necesarias para cerrar la historia (`prioridad:must`); la prioridad solo indica el orden en que conviene construirlas.

### User Story 1 - Un error reconocido llega con su código del catálogo (Priority: P1)

Como autor, quiero que cada error de negocio o de aplicación registrado en el catálogo llegue al consumidor con su título, su descripción, su código y su estado HTTP, sin que el dominio ni los casos de uso conozcan HTTP, para que todas las historias traduzcan sus errores de la misma forma y en un solo lugar por aplicación (principio XIII, ADR-009).

**Why this priority**: Es el mecanismo que usarán todas las historias con reglas de negocio; sin él, cada controlador traduciría sus errores a su manera.

**Independent Test**: Producir un error registrado en el catálogo desde un caso de uso y comprobar que la respuesta trae el título, la descripción y el código del catálogo, con el estado HTTP del catálogo.

**Acceptance Scenarios**:

```gherkin
Escenario: Error reconocido
  Dado un error de dominio registrado en el catálogo con su código y su estado HTTP
  Cuando un caso de uso lo produce
  Entonces la respuesta trae titulo, descripcion y codigo del catálogo
  Y el estado HTTP es el del catálogo
```

---

### User Story 2 - Un error técnico inesperado no filtra detalles (Priority: P1)

Como autor, quiero que cualquier fallo técnico no reconocido responda 500 con un código genérico, una descripción segura y un identificador de incidente, y que el registro guarde la causa completa con ese mismo identificador, para no darle pistas a un atacante y poder diagnosticar el fallo (RNF-SEG-04, OWASP).

**Why this priority**: Hoy un fallo inesperado responde con el formato por defecto del framework, que puede incluir rutas, nombres de clases o mensajes técnicos. Es un riesgo de seguridad abierto en las 4 aplicaciones.

**Independent Test**: Provocar un fallo técnico durante una petición (por ejemplo, que la base de datos no responda) y comprobar que la respuesta es 500 con el código genérico, una descripción segura y un `idIncidente`, sin detalles técnicos, y que el registro tiene la causa y la traza con ese `idIncidente`.

**Acceptance Scenarios**:

```gherkin
Escenario: Error técnico inesperado
  Dado una excepción técnica no reconocida, por ejemplo que la base de datos no responde
  Cuando ocurre durante una petición
  Entonces la respuesta es 500 con un código genérico, una descripción segura y un idIncidente
  Y la respuesta no contiene trazas, nombres de clases, SQL ni el mensaje original
  Y el registro guarda la causa y la traza con el mismo idIncidente
```

---

### User Story 3 - Un error de entrada dice qué campo falló (Priority: P2)

Como autor, quiero que una solicitud con un campo obligatorio vacío responda 400 y diga en `detalles` qué campo falló y qué le falta, para que el usuario sepa qué corregir (RNF-USA-01).

**Why this priority**: Activa la validación de entrada según los contratos, que B00-H03 dejó apagada a la espera de esta historia (D-38). Depende del mecanismo de las partes 1 y 2.

**Independent Test**: Enviar una solicitud con un campo obligatorio vacío y comprobar que la respuesta es 400 con un elemento en `detalles` que nombra el campo y lo que le falta.

**Acceptance Scenarios**:

```gherkin
Escenario: Error de entrada
  Dado una solicitud con un campo obligatorio vacío
  Cuando llega al controlador
  Entonces la respuesta es 400
  Y detalles indica el campo y qué le falta
```

---

### Edge Cases

- **Error de dominio no registrado en el catálogo**: se trata como error técnico inesperado (500 con `idIncidente`), y el registro dice que falta en el catálogo; así un error nuevo sin registrar no pasa desapercibido.
- **Errores que produce el propio framework antes del controlador** (ruta inexistente, método no permitido, tipo de contenido no soportado, cuerpo que no se puede leer): llegan con el mismo modelo común y su estado, sin el formato por defecto del framework.
- **Cuerpo JSON mal formado**: 400 con un código de entrada genérico; la descripción no repite el mensaje del analizador JSON.
- **Varios campos inválidos a la vez**: `detalles` trae un elemento por cada campo.
- **401 por token ausente o inválido** (B00-H03): sigue igual, con el mismo cuerpo byte por byte; esta historia no lo cambia (ADR-014).
- **Credenciales inválidas en el inicio de sesión**: dejan de traducirse en el controlador de Usuarios y pasan al mecanismo común, con el mismo código `USUARIOS_CREDENCIALES_INVALIDAS` y la misma respuesta (D-38). Con datos vacíos, el inicio de sesión responde 400 con `detalles` (D-42).
- **Proceso que no responde detrás del API Gateway**: el API Gateway responde 503 con el modelo común; no reenvía el error de conexión ni la dirección interna del proceso (D-40).
- **Mensaje original con datos sensibles** (por ejemplo, una contraseña o un token en una excepción): no aparece en la respuesta y se omite también del registro (principio XIII, RNF-SEG-06).
- **Dos errores técnicos simultáneos**: cada uno tiene su propio `idIncidente`, distinto e imposible de adivinar.
- **Error de otro servicio**: se traduce al código y contexto del servicio que lo recibe, sin reenviar el cuerpo remoto (principio XIII); hoy los servicios no se llaman entre procesos, así que se aplica cuando llegue la primera llamada.

## Requirements *(mandatory)*

### Functional Requirements

**Modelo y catálogo**

- **FR-001**: Toda respuesta de error de las aplicaciones DEBE usar el esquema `Error` común de `contratos/comun/errores.yaml` (`titulo`, `descripcion` y `codigo` obligatorios; `detalles` solo en errores de entrada; `idIncidente` solo en errores técnicos), sin campos extra (principio XIII).
- **FR-002**: DEBE existir un catálogo de códigos en `contratos/comun/errores.md` con, por cada código, su estado HTTP, su título, su descripción y cuándo se usa. Cada código es estable y lleva el prefijo de su servicio o el común. El catálogo DEBE incluir los códigos de B00-H03 sin cambios (D-38) y los códigos genéricos que crea esta historia: error técnico inesperado, error de entrada y los errores del framework antes del controlador.
- **FR-003**: Cada aplicación DEBE tener un solo punto de traducción de errores a respuestas HTTP. Ningún controlador ni caso de uso DEBE repetir esa traducción (principio XIII).
- **FR-012**: El API Gateway DEBE responder sus propios errores con el modelo común: ruta inexistente (404), proceso que no responde (503) y fallo técnico (500 con `idIncidente`), sin el formato por defecto del framework y sin reenviar detalles técnicos (D-40).

**Errores reconocidos**

- **FR-004**: Un error de dominio o de aplicación registrado en el catálogo DEBE responder con el título, la descripción, el código y el estado HTTP del catálogo. El dominio y la aplicación NO DEBEN depender de HTTP ni del modelo de respuesta. Como los servicios todavía no tienen errores de dominio, el escenario se demuestra con un error de dominio de prueba registrado en un catálogo de prueba, y con las credenciales inválidas de Usuarios como primer error real (D-41).
- **FR-005**: La traducción de credenciales inválidas que hoy hace el controlador de Usuarios DEBE pasar al punto de traducción común, sin cambiar su código ni su respuesta (D-38).

**Errores técnicos**

- **FR-006**: Una excepción técnica no reconocida DEBE responder 500 con un código genérico, una descripción segura en español y un `idIncidente` único e impredecible.
- **FR-007**: La respuesta de un error técnico NO DEBE contener trazas, nombres de clases, paquetes o métodos, SQL, el mensaje original de la excepción, credenciales, tokens ni direcciones internas.
- **FR-008**: El registro de un error técnico DEBE guardar la causa original y la traza junto con el mismo `idIncidente`, sin contraseñas, tokens ni datos personales.

**Errores de entrada**

- **FR-009**: Una solicitud con un campo obligatorio vacío o que no cumple el contrato DEBE responder 400, con un elemento en `detalles` por cada campo inválido, que dice el campo y qué le falta. Esto incluye el inicio de sesión: con usuario o contraseña vacíos responde 400 con `detalles`, en lugar del 401 de credenciales inválidas de B00-H03 (D-42).
- **FR-010**: Los errores del framework antes del controlador (ruta inexistente, método no permitido, tipo de contenido no soportado, cuerpo ilegible) DEBEN responder con el modelo común y el estado que corresponde.

**Contratos**

- **FR-011**: Los contratos de los servicios con API DEBEN declarar las respuestas de error que pueden dar (400, 500 y las de su catálogo), y las pruebas de contrato DEBEN cubrir esas respuestas (principios IV y VIII).

### Key Entities

- **Error**: respuesta pública de error con `titulo`, `descripcion`, `codigo` y, según el tipo, `detalles` o `idIncidente`.
- **Detalle de error**: un campo de la solicitud que no pasó la validación, con su nombre y lo que le falta.
- **Código del catálogo**: identificador estable de un error, con su estado HTTP, su título y su descripción.
- **Incidente**: un error técnico inesperado identificado por su `idIncidente`, que une la respuesta al usuario con la causa en el registro.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las respuestas de error de las aplicaciones, incluidas las del framework antes del controlador, tienen la forma del esquema `Error` y pasan la validación contra el contrato.
- **SC-002**: Ninguna respuesta de error técnico contiene trazas, nombres de clases, SQL ni el mensaje original de la excepción, comprobado con al menos un fallo provocado por aplicación.
- **SC-003**: Para cada error técnico provocado, el `idIncidente` de la respuesta aparece en el registro junto con la causa y la traza; dos fallos seguidos tienen identificadores distintos.
- **SC-004**: Una solicitud con N campos inválidos responde 400 con N elementos en `detalles`.
- **SC-005**: El 401 por token de B00-H03 y la respuesta de credenciales inválidas con datos completos no cambian: las pruebas de B00-H03 y `deploy/probar_autenticacion.sh` siguen pasando.
- **SC-006**: Una ruta inexistente y un proceso detenido detrás del API Gateway responden con el modelo común (404 y 503), sin el formato por defecto del framework.

## Assumptions

- El esquema `Error` y el catálogo ya existen desde B00-H03 (`contratos/comun/errores.yaml` y `errores.md`, D-38); esta historia los completa y los aplica en todas las aplicaciones.
- «Aplicación» son los 4 procesos de ADR-006 (identidad, datos, distributivo y salida), que comparten el mecanismo de traducción, y el API Gateway, con su manejador reactivo (D-40); cada servicio registra sus propios códigos.
- La clasificación de los errores (dominio, aplicación, infraestructura y entrada) y los estados HTTP son los del principio XIII.
- Los errores entre procesos (502, 503 y 504) se traducen cuando llegue la primera llamada por HTTP entre procesos; hoy no existe.
- Las respuestas y el registro usan solo datos ficticios (principios VIII y IX).
