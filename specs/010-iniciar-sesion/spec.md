# Feature Specification: Iniciar sesión

**Feature Branch**: `010-iniciar-sesion`

**Created**: 2026-10-17

**Status**: Draft

**Input**: User description: "Especifica la historia F05-H01 · Iniciar sesión (issue #43) con sus escenarios, a partir del ERS (RF-01, RNF-SEG-02, RNF-SEG-07, RF-04 y la matriz de roles D-16) y de lo que ya dejaron B00-H03 y B00-H06."

**Origen**: issue #43 · F05-H01 · sub-issue de #7 (F05 · Designar al Responsable de consolidación y aplicar los roles) · milestone «Ola 1 · Datos listos» · etiquetas `tipo:historia`, `prioridad:must`, `modulo:01-usuarios`, `RF-01` · tamaño propuesto M · depende de #20 (B00-H03) y #23 (B00-H06), cerradas. Es la primera historia de la ola 1.

**Requerimientos que cubre**: RF-01 · RNF-SEG-02 · RNF-SEG-07 (prioridad *Should*: bloqueo e inactividad, incluido por D-60) (ERS, `requerimientos_mdd/ERS_MDD_Requerimientos.md`). **Relacionados**: RF-04 (prioridad *Won't*: recuperación de contraseña) y la matriz de roles de D-16. **Decisiones tomadas en esta historia:** D-59, D-60 y D-61. **Decisiones que aplica** (`docs/decisiones.md`): D-37 (inicio de sesión mínimo de B00-H03: F05-H01 agrega los usuarios reales de la demo y los mensajes), D-50 (la base de la SPA protege las rutas internas; F05-H01 comprueba «Acceso sin sesión» sobre ella), D-51 (token solo en memoria), D-57 (F05-H01 deshabilita `autor.prueba` en el equipo de demostración cuando existan los usuarios reales), ADR-014 (JWT RS256 con los roles) y ADR-019 (base del frontend).

**Historia (tal como está en el issue)**: Como usuario del MDD, quiero iniciar sesión con mis credenciales, para trabajar solo con lo que mis roles permiten.

**Punto de partida.** B00-H03 dejó el inicio de sesión de Usuarios: contraseñas con Argon2id, una sola respuesta para usuario inexistente, inactivo o contraseña incorrecta (con un hash de relleno para no revelar por el tiempo qué usuarios existen) y el token con los roles. B00-H06 dejó la pantalla de inicio de sesión, el marco con «Cerrar sesión», la protección de las rutas internas con vuelta a la ruta pedida y el token solo en memoria. Hoy solo existe el usuario de prueba `autor.prueba`. Esta historia lleva el inicio de sesión a los **usuarios de la demo**, uno por cada rol de D-16, completa los mensajes y comprueba los tres escenarios sobre esa base.

> **Alcance respecto de F05-H02.** Qué puede hacer cada rol (permisos de lectura, escritura, aprobación y firma) es de F05-H02 (#44, RF-02 y RF-03). Esta historia solo garantiza que cada usuario entra con **sus** roles en la sesión.

## Clarifications

### Session 2026-10-17

- Q: ¿De dónde salen los usuarios de la demo, si no hay pantalla de gestión de usuarios? → A: De un archivo versionado con sus nombres y roles, sin contraseñas; Usuarios los crea al arrancar si no existen, y la contraseña de cada uno se fija en cada entorno (D-59).
- Q: ¿Entra RNF-SEG-07 (bloqueo e inactividad)? → A: Sí: bloqueo temporal tras 5 intentos fallidos seguidos con el mismo nombre (exista o no) y sesión que vence tras 30 minutos sin actividad, renovada mientras se trabaja, con un máximo de 8 horas (D-60).
- Q: ¿Cómo se trata «tokens y secretos cifrados en reposo» de RNF-SEG-02? → A: Se cumple por diseño y se demuestra con evidencia: hash Argon2id, tokens sin estado solo en memoria y secretos del equipo con permisos de su dueño, sin cifrado adicional (D-61).

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus tres escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Un usuario de la demo entra con sus credenciales (Priority: P1)

Como usuario del MDD (por ejemplo, la coordinadora de una carrera o el vicerrector), quiero entrar con mi usuario y mi contraseña y llegar a la pantalla de inicio con mi nombre y mis roles, para trabajar con lo que me corresponde.

**Why this priority**: Sin usuarios reales, el MDD solo se puede demostrar con un usuario de prueba que tiene roles fijos; el resto de la ola 1 necesita entrar con el rol que corresponde a cada pantalla.

**Independent Test**: Con los usuarios de la demo creados, iniciar sesión con dos de ellos de roles distintos (por ejemplo, Coordinador de carrera y Vicerrector académico) y comprobar que cada uno llega a la pantalla de inicio con su nombre y solo sus roles.

**Acceptance Scenarios**:

```gherkin
Escenario: Credenciales válidas
  Dado un usuario activo
  Cuando ingresa su usuario y su contraseña correctos
  Entonces accede a la pantalla de inicio
```

---

### User Story 2 - Un error de credenciales no revela cuál dato falló (Priority: P1)

Como usuario del MDD, quiero que, si me equivoco al iniciar sesión, el mensaje no diga si falló el usuario o la contraseña, para que nadie pueda averiguar qué usuarios existen (RF-01).

**Why this priority**: Es el criterio de seguridad que el ERS pone en RF-01.

**Independent Test**: Probar con una contraseña incorrecta de un usuario existente, con un usuario que no existe y con un usuario desactivado: los tres muestran el mismo mensaje en español, sin decir cuál dato falló.

**Acceptance Scenarios**:

```gherkin
Escenario: Credenciales inválidas
  Dado un usuario activo
  Cuando ingresa una contraseña incorrecta
  Entonces ve un error que no dice cuál de los dos datos falló
```

---

### User Story 3 - Sin sesión, toda ruta interna lleva al inicio de sesión (Priority: P1)

Como responsable del MDD, quiero que quien abra una dirección interna sin haber iniciado sesión vaya al inicio de sesión y, después de entrar, a la dirección que pidió (RF-01, D-50).

**Why this priority**: Es el tercer criterio de RF-01; la protección ya existe desde B00-H06 y esta historia la comprueba con los usuarios de la demo.

**Independent Test**: Sin sesión, abrir una ruta interna: se muestra el inicio de sesión; después de entrar con un usuario de la demo, se llega a la ruta pedida.

**Acceptance Scenarios**:

```gherkin
Escenario: Acceso sin sesión
  Dado un visitante sin sesión
  Cuando abre una URL interna
  Entonces se le redirige al inicio de sesión
```

---

### Edge Cases

- **Usuario desactivado**: no entra y ve el mismo mensaje que con credenciales inválidas.
- **Usuario con varios roles** (un docente que también es coordinador, ERS §3): entra una vez y su sesión lleva todos sus roles.
- **Datos vacíos**: el mismo comportamiento de hoy (400 con los detalles de cada campo, D-42), con los mensajes en español junto a cada campo.
- **Mayúsculas en el usuario**: [se asume] el nombre de usuario no distingue mayúsculas y minúsculas; la contraseña sí.
- **Sesión vencida mientras trabaja**: vuelve al inicio de sesión con un mensaje en español y, al entrar, a la pantalla en la que estaba (ya lo hace la base de B00-H06).
- **Recarga de la página**: vuelve a pedir el inicio de sesión (D-51, token solo en memoria).
- **Varios intentos fallidos seguidos**: al quinto intento fallido seguido con el mismo nombre de usuario, ese nombre queda bloqueado 15 minutos, exista o no, para no revelar qué usuarios existen; el mensaje dice en español que se espere y no dice si el usuario existe. Un inicio de sesión correcto antes del quinto intento reinicia la cuenta (D-60).
- **Contraseña correcta durante el bloqueo**: tampoco entra hasta que termine el bloqueo.
- **Inactividad**: tras 30 minutos sin actividad, la sesión vence y la SPA vuelve al inicio de sesión con un mensaje; mientras el usuario trabaja, la sesión se renueva, hasta 8 horas desde que entró (D-60).
- **`autor.prueba` en el equipo de demostración**: deja de poder entrar cuando existen los usuarios de la demo (D-57); en local y en la CI sigue igual.
- **Contraseña olvidada**: fuera del prototipo (RF-04, prioridad *Won't*); el mensaje de error no ofrece recuperarla.

## Requirements *(mandatory)*

### Functional Requirements

**Usuarios de la demo**

- **FR-001**: DEBE existir un usuario de la demo por cada uno de los 10 roles de la matriz de D-16 (Administrador del sistema, Talento Humano / Operador de datos, Coordinador de carrera, Gestor de investigación o de vinculación, Responsable de consolidación, Vicerrector académico, Rector, Miembro del OCS, Secretaría general académica y Docente), y al menos uno con dos roles a la vez (Docente y Coordinador de carrera, ERS §3), con nombres ficticios (principio IX).
- **FR-002**: Los usuarios de la demo DEBEN definirse en un archivo versionado (nombre de usuario, nombre para mostrar y roles, sin contraseñas) y crearse al arrancar Usuarios si no existen; la contraseña de cada uno DEBE fijarse en cada entorno (D-59).
- **FR-003**: Ninguna contraseña DEBE estar en el repositorio ni en una imagen: se generan o se fijan en cada entorno (local, CI y equipo de demostración), y en la base solo se guarda su hash Argon2id (RNF-SEG-02).
- **FR-004**: En el equipo de demostración, `autor.prueba` DEBE quedar deshabilitado cuando existan los usuarios de la demo (D-57).

**Inicio de sesión (escenarios 1 y 2)**

- **FR-005**: Un usuario activo con su usuario y su contraseña correctos DEBE llegar a la pantalla de inicio, que muestra su nombre para mostrar y sus roles, en español.
- **FR-006**: Usuario inexistente, usuario desactivado y contraseña incorrecta DEBEN responder con el mismo mensaje en español, en el mismo tiempo aproximado, sin indicar cuál dato falló (RF-01; ya lo hace Usuarios desde B00-H03).
- **FR-007**: La sesión DEBE llevar todos los roles del usuario, y solo los suyos.
- **FR-008**: RNF-SEG-02 DEBE quedar demostrado en la evidencia: contraseñas solo como hash Argon2id, ningún token guardado (JWT sin estado y en memoria, D-51) y secretos del equipo solo con permisos de su dueño, fuera del repositorio, de GitHub y de las imágenes (D-61).

**Acceso sin sesión (escenario 3)**

- **FR-009**: Toda ruta interna de la SPA abierta sin sesión DEBE llevar al inicio de sesión y, después de entrar, a la ruta pedida (D-50). Toda operación de la API sin un token válido DEBE responder 401 con el modelo de error común (ADR-014, ADR-016).

**Bloqueo e inactividad (RNF-SEG-07, D-60)**

- **FR-010**: Después de 5 intentos fallidos seguidos con el mismo nombre de usuario, exista o no, el inicio de sesión con ese nombre DEBE quedar bloqueado 15 minutos, con un mensaje en español que no revele si el usuario existe; un inicio de sesión correcto antes reinicia la cuenta. Los tres valores (intentos, bloqueo y nombre normalizado en minúsculas) son parámetros.
- **FR-011**: La sesión DEBE vencer tras 30 minutos sin actividad y renovarse mientras el usuario trabaja, sin guardar el token fuera de la memoria (D-51), con un máximo de 8 horas desde el inicio de sesión; al vencer, la SPA vuelve al inicio de sesión con un mensaje en español.

### Key Entities

- **Usuario**: persona que entra al MDD (glosario, `sdd/modelo_dominio.md`). Tiene un nombre de usuario único, un nombre para mostrar, el hash de su contraseña, si está activo y uno o más roles.
- **Rol**: uno de los 10 de la matriz de D-16; qué permite es de F05-H02.
- **Sesión**: lo que obtiene el usuario al entrar: su identidad y sus roles, con un vencimiento por inactividad y un máximo (D-60).
- **Intentos fallidos**: cuántos intentos fallidos seguidos lleva un nombre de usuario y hasta cuándo está bloqueado (D-60).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Los 10 roles de D-16 tienen un usuario de la demo que inicia sesión y ve, en la pantalla de inicio, exactamente sus roles.
- **SC-002**: Un usuario de la demo entra y llega a la pantalla de inicio en menos de 1 minuto, sin ayuda.
- **SC-003**: El 100 % de los intentos con contraseña incorrecta, usuario inexistente o usuario desactivado muestran el mismo mensaje, sin revelar cuál dato falló.
- **SC-004**: El 100 % de las rutas internas abiertas sin sesión llevan al inicio de sesión, y después de entrar se llega a la ruta pedida.
- **SC-005**: Ninguna contraseña aparece en el repositorio, en una imagen ni en un registro; en la base solo hay hashes Argon2id.
- **SC-006**: En el equipo de demostración, `autor.prueba` no puede iniciar sesión.
- **SC-007**: El sexto intento con el mismo nombre, después de 5 fallidos, se rechaza con el mensaje de bloqueo el 100 % de las veces, exista o no el usuario; pasado el bloqueo, las credenciales correctas vuelven a funcionar.
- **SC-008**: Una sesión sin actividad durante 30 minutos vence; una sesión con actividad sigue abierta más de 30 minutos, hasta las 8 horas.

## Assumptions

- Los usuarios de la demo son ficticios y representan los roles del ERS; no son personas reales de la institución (principio IX, LOPDP).
- El nombre de usuario no distingue mayúsculas y minúsculas; la contraseña sí.
- La pantalla de inicio de sesión, el marco y la protección de rutas son los de B00-H06; esta historia ajusta solo lo que haga falta (por ejemplo, el nombre para mostrar en el inicio).
- La recuperación de contraseña (RF-04) queda fuera del prototipo, como dice el ERS (prioridad *Won't*).
- Qué puede hacer cada rol, y los usuarios con roles por periodo, son de F05-H02 y F05-H03.
- Solo se usan datos ficticios (principios VIII y IX).
