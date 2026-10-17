# Feature Specification: Publicación en Docker Hub y despliegue en el equipo de demostración

**Feature Branch**: `009-publicacion-despliegue`

**Created**: 2026-10-17

**Status**: Draft

**Input**: User description: "Especifica la historia B00-H08 (issue #25) con sus escenarios, adaptados según D-55: el despliegue automático va al PC de demostración del autor, que baja las imágenes de Docker Hub por extracción; la CI no entra al equipo; Caddy mantiene el acceso restringido y un túnel de salida es opcional; ZAP y k6 de cierre-ola corren contra el sistema que levanta el runner; nada depende del PC. Incluye la revisión de las 6 imágenes con Grype que B00-H07 dejó para esta historia, y la llave JWT persistente y las credenciales de proceso del equipo de demostración."

**Origen**: issue #25 · B00-H08 · sub-issue de #2 (B00 · Base técnica de la ola 0) · milestone «Ola 0 · Base técnica» · etiquetas `tipo:historia`, `prioridad:must`, `modulo:transversal` · tamaño propuesto L · depende de #19 (B00-H02) y #24 (B00-H07), cerradas. Es la última historia de la ola 0.

**Requerimientos que cubre**: ADR-011 · D-27 · RNF-POR-01 · RNF-POR-02. **Decisiones que aplica** (`docs/decisiones.md`): **D-55** (el despliegue va al PC de demostración del autor, por extracción desde Docker Hub), D-36 (equipo de 4 núcleos y 8 GiB), R-19 (límites de memoria), ADR-013 y ADR-015 (imágenes construidas desde el código, con la raíz como contexto), D-52 (la SPA en el perfil), ADR-014 (llave de firma RS256), D-49 y ADR-018 (token de proceso), D-54 (excepciones de Grype) y D-31 (los 10 checks de `main`). La constitución está en la versión 2.1.3, que ya refleja D-55 (principio IX, *Operación*, y §3).

**Historia (del issue, con el destino de D-55)**: Como autor, quiero que cada fusión a `main` publique las 6 imágenes en el repositorio privado de Docker Hub y que el equipo de demostración las despliegue solo, para tener siempre la última versión funcionando, sin desplegar a mano.

**Decisiones tomadas en esta historia:** D-56 y D-57.

## Clarifications

### Session 2026-10-17

- Q: Como la CI ya no despliega (D-55), ¿dónde queda registrado el resultado de cada despliegue? → A: En GitHub, como *deployment* del entorno del despliegue: el equipo lo informa con un token de grano fino que solo puede escribir *deployments* de este repositorio y vive solo en el equipo (D-56).
- Q: ¿Se habilita `autor.prueba` en el equipo de demostración hasta que F05-H01 traiga usuarios reales? → A: Sí, con una contraseña generada en el equipo, distinta de la de `.env.ejemplo`, detrás del acceso restringido del proxy (D-57).

> **Sobre el título del issue.** El issue dice «despliegue en el VPS». D-55 cambió el destino al PC del autor, que hace de equipo de demostración, porque terminó el crédito con el que se iba a pagar el VPS. La historia y sus cinco escenarios se mantienen; cambia dónde corren y quién inicia el despliegue (el equipo, no la CI).

> **Nota sobre el lenguaje de esta especificación.** Es una historia de la base técnica: su usuario es el autor. Los nombres «Docker Hub», «Grype», «Caddy», «ZAP» y «k6» aparecen porque los traen el issue, la constitución y D-55; la especificación no elige herramientas nuevas.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus cinco escenarios, más el cierre de ola que pide D-55, son partes que se pueden probar por separado. Todas son necesarias para cerrar la historia (`prioridad:must`); la prioridad solo indica el orden en que conviene construirlas.

### User Story 1 - Cada fusión a main publica las imágenes revisadas (Priority: P1)

Como autor, quiero que cada fusión a `main` construya las 6 imágenes (los 4 procesos, el API Gateway y la SPA), que Grype las revise y que se publiquen en el repositorio privado de Docker Hub con la etiqueta del componente y del commit, para que el equipo de demostración siempre tenga de dónde bajar exactamente lo que pasó los checks.

**Why this priority**: Es la base de todo lo demás: sin imágenes publicadas no hay qué desplegar.

**Independent Test**: Fusionar un cambio a `main` y comprobar que el repositorio privado tiene las 6 imágenes con la etiqueta de ese commit, que el informe de Grype de las 6 está en el run y que una persona sin credenciales no puede bajarlas.

**Acceptance Scenarios**:

```gherkin
Escenario: Publicación de las imágenes
  Dado una fusión a main con los checks en verde
  Cuando corre la etapa de publicación
  Entonces se construyen las 6 imágenes, Grype las revisa y se publican con la etiqueta del componente y del commit
```

---

### User Story 2 - Una imagen vulnerable no se publica ni se despliega (Priority: P1)

Como autor, quiero que una imagen con una vulnerabilidad alta o crítica que tiene versión corregida no se publique, para que el equipo de demostración nunca baje una versión vulnerable (principio IX).

**Why this priority**: Es el control de seguridad de la entrega para las imágenes, que B00-H07 dejó para esta historia.

**Independent Test**: Construir, en una rama de demostración, una imagen sobre una base con una vulnerabilidad conocida que tiene arreglo, correr la etapa de publicación y comprobar que falla, que no se publica ninguna de las 6 imágenes de ese commit y que el equipo no despliega nada.

**Acceptance Scenarios**:

```gherkin
Escenario: Imagen vulnerable
  Dado una imagen base con una vulnerabilidad crítica que tiene versión corregida
  Cuando Grype la revisa
  Entonces la imagen no se publica y el despliegue no corre
```

---

### User Story 3 - El equipo de demostración se actualiza solo y queda sano (Priority: P1)

Como autor, quiero que el equipo de demostración detecte una versión nueva publicada, la baje, reinicie los servicios y compruebe que todos quedan sanos, sin que yo intervenga y sin que la CI entre al equipo (D-55).

**Why this priority**: Es el objetivo de la historia: tener siempre la última versión funcionando.

**Independent Test**: Con el equipo preparado, fusionar un cambio a `main` y comprobar que, en el tiempo de consulta más el despliegue, el equipo corre la versión de ese commit, todos los contenedores quedan sanos y la prueba de humo responde a través del proxy.

**Acceptance Scenarios**:

```gherkin
Escenario: Despliegue sano
  Dado las imágenes publicadas
  Cuando el equipo de demostración detecta la versión nueva y corre el despliegue
  Entonces baja las imágenes, reinicia los servicios y todos quedan sanos según su healthcheck
  Y la prueba de humo responde a través del proxy
```

---

### User Story 4 - Un despliegue que no queda sano vuelve atrás solo (Priority: P1)

Como autor, quiero que, si un proceso no queda sano en 180 segundos, el equipo vuelva a la versión anterior y el despliegue quede registrado como fallido, para que la demostración nunca quede rota por una versión mala.

**Why this priority**: Sin vuelta atrás, una versión mala deja la demostración caída hasta que el autor la arregle a mano.

**Independent Test**: Publicar una versión de demostración con un proceso que nunca queda sano, comprobar que el equipo vuelve a la versión anterior, que todos quedan sanos con ella y que el despliegue fallido queda registrado donde el autor lo ve.

**Acceptance Scenarios**:

```gherkin
Escenario: Vuelta atrás
  Dado un despliegue en el que un proceso no queda sano en 180 segundos
  Cuando termina la espera
  Entonces el equipo vuelve a la etiqueta anterior
  Y el despliegue queda registrado como fallido
```

---

### User Story 5 - Solo quien tiene las credenciales ve el MDD (Priority: P1)

Como autor, quiero que el proxy pida usuario y contraseña antes de mostrar el MDD, también si el equipo se publica fuera de la red de la casa con un túnel, para respetar el acuerdo de confidencialidad de la demostración.

**Why this priority**: Es el requisito de acceso restringido del principio IX (*Operación*).

**Independent Test**: Abrir la dirección del equipo sin credenciales y comprobar que el proxy las pide y no muestra la SPA; con las credenciales, la SPA carga y el inicio de sesión funciona.

**Acceptance Scenarios**:

```gherkin
Escenario: Acceso restringido
  Dado una persona sin las credenciales de la demo
  Cuando abre la dirección del equipo de demostración
  Entonces el proxy le pide usuario y contraseña y no muestra el MDD
```

---

### User Story 6 - El cierre de ola no depende de una URL pública (Priority: P2)

Como autor, quiero que ZAP y k6 del cierre de ola corran contra el sistema que levanta el propio runner, como `perfil-consolidado`, para cerrar cada ola sin depender de que el equipo de demostración esté encendido o publicado (D-55).

**Why this priority**: Hace falta para cerrar la ola 0 con su tag, pero no bloquea la publicación ni el despliegue.

**Independent Test**: Lanzar `cierre-ola` a mano en la rama de la historia y comprobar que levanta el sistema en el runner, que ZAP escanea la SPA y la API de los contratos que existen, y que la release de evidencia se arma con el SBOM por módulo de B00-H07.

**Acceptance Scenarios**:

```gherkin
Escenario: Cierre de ola sin URL pública
  Dado el sistema levantado en el runner con el perfil consolidado
  Cuando corre el cierre de ola
  Entonces ZAP escanea la SPA y la API de los contratos que existen, sin hallazgos altos
  Y la evidencia de la ola se publica como release
```

---

### Edge Cases

- **Fusión de solo documentación**: también publica las 6 imágenes con la etiqueta del commit, para que cada commit de `main` tenga su versión completa y la vuelta atrás siempre tenga a dónde volver. El equipo despliega igual; si nada cambió, los contenedores se reinician con la misma configuración.
- **Una sola de las 6 imágenes es vulnerable**: no se publica **ninguna** de ese commit. Publicar unas y otras no mezclaría versiones en el equipo.
- **Excepción vigente de D-54**: una vulnerabilidad cubierta por una excepción vigente no impide publicar, igual que en `4 · sca`. La misma configuración de excepciones vale para dependencias e imágenes.
- **Imágenes de terceros que usa el despliegue** (PostgreSQL y el proxy): se fijan por *digest* y Grype también las revisa; si tienen una alta o crítica con arreglo, el despliegue no corre hasta actualizarlas o agregar una excepción (D-54).
- **Dos fusiones seguidas**: el equipo despliega la más reciente que encuentra; no hace falta desplegar las intermedias.
- **El equipo estaba apagado**: al encenderse, la próxima consulta despliega la última versión publicada.
- **GitHub no responde al informar un despliegue**: el despliegue sigue igual; el informe queda en el registro local y no se reintenta hacia atrás.
- **Docker Hub no responde o la credencial venció**: el equipo sigue con la versión que tiene, registra el error y vuelve a intentar en la próxima consulta. No borra nada.
- **Primer despliegue** (no hay versión anterior): si no queda sano, no hay a dónde volver; se registra el fallo y los servicios quedan como estén, para que el autor los revise.
- **La versión anterior tampoco queda sana después de volver**: se registra el doble fallo y se deja todo como está, para revisión manual (ya lo hace `desplegar.sh`).
- **Reinicio de identidad en el equipo**: la llave de firma es persistente, así que las sesiones abiertas siguen valiendo; solo la pierde quien borra a propósito la llave.
- **Credenciales y llave en el equipo**: viven solo en el equipo, con permisos solo de su dueño; nunca en el repositorio, en GitHub ni en las imágenes.
- **Túnel no configurado**: la demostración funciona dentro de la red del autor; el túnel es opcional (D-55).
- **Contrato que todavía no existe**: el escaneo de la API en el cierre de ola usa solo los contratos que existen en `contratos/`; uno nuevo entra solo.
- **Ola sin escenario de carga**: si todavía no existe el script de k6 de la ola (la nómina base de D-15 necesita datos de olas posteriores), el job de k6 se omite con un aviso en lugar de fallar.
- **Distribución distinta**: el equipo del autor usa Fedora con SELinux y la VM del IST, Ubuntu Server; nada del despliegue debe depender de la distribución (D-55).

## Requirements *(mandatory)*

### Functional Requirements

**Publicación (escenarios 1 y 2)**

- **FR-001**: Cada fusión a `main` DEBE construir las 6 imágenes (`identidad`, `datos`, `distributivo`, `salida`, `gateway` y `frontend`) desde el código de ese commit, con los mismos Dockerfiles del perfil consolidado (ADR-013, ADR-015).
- **FR-002**: Grype DEBE revisar las 6 imágenes antes de publicarlas, con la misma regla y las mismas excepciones de `4 · sca`: falla con una vulnerabilidad alta o crítica con versión corregida que no cubra una excepción vigente (principio IX, D-54). El informe DEBE decir la imagen, el paquete, la versión instalada y la corregida.
- **FR-003**: Si alguna de las 6 imágenes falla la revisión, NO DEBE publicarse ninguna imagen de ese commit, y el equipo no tiene nada nuevo que desplegar.
- **FR-004**: Las imágenes DEBEN publicarse en un único repositorio privado de Docker Hub, cada una con una etiqueta que diga el componente y el commit, y DEBE existir una forma de que el equipo sepa cuál es la última versión publicada de `main`.
- **FR-005**: La publicación DEBE usar solo credenciales de escritura de Docker Hub guardadas en un entorno de GitHub restringido a `main`; no DEBE haber en GitHub credenciales ni direcciones del equipo de demostración (D-55).

**Despliegue en el equipo (escenarios 3 y 4)**

- **FR-006**: El equipo de demostración DEBE consultar Docker Hub cada pocos minutos, con una credencial de solo lectura, y desplegar la última versión publicada de `main` cuando sea distinta de la que corre (D-55).
- **FR-007**: El despliegue DEBE bajar las imágenes, reiniciar los servicios y esperar hasta 180 segundos a que todos queden sanos según su *healthcheck*, y después correr una prueba de humo a través del proxy.
- **FR-008**: Si algún proceso no queda sano en 180 segundos, el equipo DEBE volver a la versión anterior y registrar el despliegue como fallido.
- **FR-009**: El equipo DEBE informar cada despliegue a GitHub como *deployment* del entorno del despliegue, con el commit, el estado (en curso, sano o fallido) y un resumen, usando un token de grano fino que solo puede escribir *deployments* de este repositorio. Si GitHub no responde, el despliegue sigue igual y el informe queda en el registro local del equipo (D-56).
- **FR-010**: El despliegue DEBE ser el mismo en cualquier equipo con Docker y Compose (el PC del autor, un VPS o la VM del IST): mismos archivos, mismo servicio de consulta y la configuración del equipo en un solo lugar fuera del repositorio.
- **FR-011**: El perfil del equipo DEBE respetar los límites de memoria de R-19 y el presupuesto de D-36, aunque el equipo tenga más recursos.

**Secretos del equipo**

- **FR-012**: La llave de firma de Usuarios DEBE ser persistente en el equipo (ADR-014), para que reiniciar identidad no invalide las sesiones.
- **FR-013**: Las claves de la base, la credencial del proceso distributivo (D-49), la contraseña de `autor.prueba` (D-57), el acceso de la demo y las credenciales de Docker Hub y de GitHub del equipo (D-56) DEBEN generarse o guardarse en el equipo y vivir solo ahí, con permisos solo de su dueño; nunca en el repositorio, en GitHub ni dentro de las imágenes.
- **FR-014**: El usuario de prueba `autor.prueba` DEBE estar habilitado en el equipo de demostración hasta F05-H01, con una contraseña generada en el equipo que NO DEBE ser la de `.env.ejemplo` (D-57).

**Acceso (escenario 5)**

- **FR-015**: El proxy DEBE pedir usuario y contraseña antes de mostrar la SPA, servir el MDD por HTTPS cuando se publica con un nombre de dominio, y dejar `/api` protegida por el token del MDD (ADR-014), como ya hace `deploy/Caddyfile`.
- **FR-016**: El equipo DEBE poder publicarse fuera de la red del autor con un túnel de salida, sin abrir puertos de entrada; es opcional y el acceso restringido de FR-015 se mantiene detrás del túnel (D-55).

**Cierre de ola (D-55)**

- **FR-017**: `cierre-ola` DEBE levantar el sistema en el runner, como `perfil-consolidado`, y correr ZAP contra la SPA y contra la API de cada contrato que exista en `contratos/`, sin depender de una URL pública ni de variables del equipo de demostración.
- **FR-018**: El job de k6 DEBE correr contra ese mismo sistema y omitirse con un aviso si la ola todavía no tiene su script de carga.
- **FR-019**: La release de evidencia DEBE incluir los SBOM por módulo de B00-H07, en lugar del SBOM agregado que ya no se usa.

### Key Entities

- **Imagen publicada**: una de las 6, en el repositorio privado, con la etiqueta del componente y del commit y su informe de Grype.
- **Versión publicada**: el conjunto de las 6 imágenes de un mismo commit de `main`; es lo que el equipo despliega o deja de desplegar entero.
- **Equipo de demostración**: el PC del autor (o cualquier otro con Docker y Compose), con su configuración y sus secretos fuera del repositorio.
- **Despliegue**: un intento de pasar de una versión a otra en el equipo, con su resultado (sano, vuelta atrás o error) y sus tiempos.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Después de una fusión a `main` con los checks en verde, el repositorio privado tiene las 6 imágenes de ese commit en menos de 30 minutos, cada una con su informe de Grype en el run.
- **SC-002**: Una imagen con una vulnerabilidad alta o crítica con arreglo deja 0 imágenes publicadas de ese commit y el equipo no cambia de versión.
- **SC-003**: El equipo encendido corre la versión de una fusión nueva en menos de 15 minutos después de que termina la publicación, con todos los contenedores sanos y la prueba de humo en verde, sin intervención del autor.
- **SC-004**: Una versión con un proceso que no queda sano vuelve sola a la anterior en menos de 5 minutos desde el inicio del despliegue, y el fallo aparece en la pestaña *Deployments* del repositorio con su commit.
- **SC-005**: El 100 % de los despliegues del equipo, sanos o fallidos, aparece en *Deployments* con su commit y su estado, si GitHub respondía en ese momento.
- **SC-006**: Sin credenciales, la dirección del equipo responde con la petición de usuario y contraseña el 100 % de las veces, y no muestra la SPA.
- **SC-007**: Reiniciar identidad en el equipo no cierra una sesión abierta: el mismo token sigue siendo aceptado.
- **SC-008**: En GitHub no hay ninguna credencial ni dirección del equipo de demostración; las únicas credenciales del despliegue en GitHub son las de escritura en Docker Hub. El token que guarda el equipo solo puede escribir *deployments* de este repositorio.
- **SC-009**: `cierre-ola` lanzado a mano termina con ZAP sin hallazgos altos sobre la SPA y la API de los contratos que existen, sin usar una URL pública.
- **SC-010**: La memoria total del equipo con la versión desplegada sigue por debajo de la alerta de 7 GiB de R-19.

## Assumptions

- **Equipo de demostración:** el PC del autor (Fedora 44, con SELinux y Docker), que el autor prepara siguiendo la guía de la historia. Un VPS o la VM del IST se prepararían igual (D-55).
- **Docker Hub:** un repositorio privado, con un token de escritura para la CI y uno de solo lectura para el equipo. El plan gratuito permite un repositorio privado; las 6 imágenes van al mismo con etiquetas distintas.
- **Proxy:** Caddy, como en `deploy/Caddyfile`. Sin dominio, sirve en la red del autor; con dominio (propio o del túnel), por HTTPS.
- **La CI no despliega:** la publicación termina en Docker Hub. El equipo inicia el despliegue por su cuenta (D-55); por eso «el job de despliegue falla» del issue pasa a «el despliegue queda registrado como fallido» en *Deployments* (D-56).
- **Imágenes:** se construyen con los Dockerfiles existentes (`java.Dockerfile` y `frontend.Dockerfile`), desde la raíz del repositorio.
- **Usuarios reales:** llegan con F05-H01; esta historia no crea usuarios ni permisos.
- **Tag `ola-0`:** se crea después de fusionar esta historia, como cierre de la ola; no es parte de la historia.
- Solo se usan datos ficticios (principios VIII y IX).
