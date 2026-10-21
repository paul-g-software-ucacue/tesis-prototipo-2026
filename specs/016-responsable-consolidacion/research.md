# Research: Designar al Responsable de consolidación del periodo (F05-H03)

Decisiones técnicas del plan. Cada sección tiene la decisión, por qué y las alternativas. Parte de lo que ya existe en `main` (`861e9ae`):
- **Roles** (proceso `identidad`, F05-H02): los 10 roles fijos de D-16, `asignacion_rol` (usuario y rol, sin periodo), `registro_cambio` y `ConsultarAutorizacion`, que une los permisos de los roles del usuario para el token (ADR-021). Asignar roles exige `USUARIOS_ROLES:ESCRITURA`.
- **Académico** (proceso `datos`, F02-H01): los periodos con sus estados, escritos con `DISTRIBUTIVO:ESCRITURA` (D-69). No publica eventos.
- **Eventos entre procesos** (ADR-018, D-49): `plataforma:eventos` publica en la transacción del servicio, entrega con plazo de espera y reenvía cada 30 s; el receptor es idempotente por `idEvento` en `/interno/<servicio>/eventos`, con token de proceso. Hoy solo lo usa `distributivo`; el esquema `eventos_datos` ya existe en la base (`01-esquemas.sh`) y no tiene tablas.

## 1. Cómo se entera Roles de los periodos (aprobado por el autor el 2026-10-21)

- **Decisión:** Académico publica un evento cada vez que un periodo se registra o cambia de estado, con el mecanismo de ADR-018, hacia Roles. Roles guarda una copia de cada periodo con su estado (`periodo_conocido`) y valida todo en local.
- **Evento:** un solo tipo, `academico.PeriodoActualizado`, con `{codigo, estado}`. Se publica al registrar (Planificación) y en cada cambio de estado, en la misma transacción que el cambio.
- **Orden:** los reenvíos pueden llegar desordenados. Como los estados solo avanzan, Roles aplica el más avanzado de los dos (el de la copia y el del evento) y descarta uno más viejo: el resultado no depende del orden.
- **Por qué:** cumple la aclaración 2 tal cual (al cerrarse el periodo, el Responsable pierde los permisos) sin que el inicio de sesión dependa del proceso `datos`, y usa el mecanismo que la constitución exige para lo que cruza procesos (principio V).
- **Alternativas:** una llamada directa de Roles a Académico con el token del Vicerrector (no se entera del cierre; patrón nuevo); la designación en Académico (necesita eventos hacia Roles **y** una llamada de Académico a Roles para saber si el usuario es docente).
- **Costo:** `datos` gana `plataforma:eventos` (§6) y una clave de proceso nueva, `MDD_CLAVE_PROCESO_DATOS`.
- **Periodos de antes:** la copia de Roles empieza vacía. En el equipo de demostración solo existe el periodo `001`, cerrado: designar en él responde «no encontrado» en lugar de «cerrado». No se republican los periodos viejos (principio X); un periodo registrado desde el despliegue llega a Roles de inmediato.

## 2. La designación (FR-001, FR-002)

- **Tabla `roles.designacion_responsable`:** `periodo` (clave primaria: uno por periodo), `usuario`, `designado_en`, `designado_por`. Designar a otro reemplaza la fila.
- **Reglas, en el dominio de Roles** (`Designacion`, Java puro, sin dobles):
  - el periodo existe en la copia y no está cerrado;
  - el usuario tiene el rol `DOCENTE` (asignación de F05-H02);
  - el usuario no es quien designa;
  - designar a quien ya es el Responsable no cambia nada ni registra.
- **«Activo»:** la aclaración pide un usuario activo. Roles no puede preguntar a Usuarios, porque Usuarios ya depende de Roles y sería un ciclo. Se comprueba el rol `DOCENTE`, que solo tienen usuarios que existen; un usuario inactivo no inicia sesión, así que la designación no le da nada. La especificación se ajusta (FR-004).
- **Concurrencia:** dos designaciones del mismo periodo se serializan con `SELECT … FOR UPDATE` sobre la fila de `periodo_conocido`; gana la última y las dos quedan en el registro.

## 3. Los permisos en el token (FR-002, D-62)

- `ConsultarAutorizacionServicio` suma el rol `RESPONSABLE_CONSOLIDACION` a los roles del usuario si tiene al menos una designación en un periodo cuya copia no está cerrada. Los permisos salen de la matriz D-16, como cualquier otro rol.
- Llegan al token en la próxima renovación (D-62), igual que un cambio de roles; al cerrarse su último periodo, el rol sale en la renovación siguiente.
- **Rol asignado a mano:** si el Administrador asignó el rol de forma global en «Roles» (F05-H02), sigue valiendo: es una decisión suya, distinta de una designación de periodo. La pantalla «Roles» no lista las designaciones como asignaciones.
- **El filtro por periodo** lo aplican las historias de consolidación y de firma (F09, F10), con la consulta de §4 (D-64).

## 4. Contrato de Roles (`contratos/roles/openapi.yaml`)

| Operación | Ruta | Permiso | Respuestas |
|---|---|---|---|
| `listarResponsables` | `GET /api/roles/responsables` | `DISTRIBUTIVO:LECTURA` | 200: `[{periodo, usuario, designadoEn}]` |
| `designarResponsable` | `PUT /api/roles/responsables/{periodo}` | `DISTRIBUTIVO:ESCRITURA` | 200 · 400 (usuario no docente, o quien designa) · 404 (periodo desconocido) · 409 `ROLES_PERIODO_CERRADO` |
| `listarDocentes` | `GET /api/roles/docentes` | `DISTRIBUTIVO:ESCRITURA` | 200: los nombres de usuario con el rol `DOCENTE`, ordenados |
| `recibirEvento` | `POST /interno/roles/eventos` | token de proceso | 204 (también repetido) · 400 `COMUN_EVENTO_NO_RECONOCIDO` |

- `listarDocentes` exige escritura porque solo sirve para designar, y muestra nombres de usuario (mínimo privilegio, principio IX).
- **Error nuevo:** `ROLES_PERIODO_CERRADO` (409): «El periodo está cerrado y ya no admite cambios.». Un usuario que no se puede designar es un 400 con el detalle en `usuario`.

## 5. Registro (FR-008, principio VII)

- `roles.registro_cambio` gana la acción `DESIGNAR_RESPONSABLE`, con el detalle `{periodo, anterior, nuevo}`; el usuario es el de la sesión.
- Las copias de los periodos no se registran como cambios de Roles: son datos de Académico, que ya registra los suyos.

## 6. Eventos en el proceso `datos` (ADR-018)

- `apps/datos` gana `plataforma:eventos` y la misma configuración que `distributivo`: `eventos_datos`, plazo de 5 s, reenvío cada 30 s, destino `roles` en `http://identidad:8080/interno/roles/eventos` y token de proceso con `MDD_CLAVE_PROCESO_DATOS`.
- `identidad` acepta esa clave (`mdd.usuarios.procesos.datos.clave`).
- **Académico** gana un puerto de salida `AvisosDelPeriodo` (`actualizado(periodo)`), implementado con el publicador de Spring como `PublicadorDeEventosSpring` de Generación. Los servicios de registrar y de cambiar el estado lo llaman dentro de su transacción.
- **Roles** gana su receptor: `evento_recibido` (clave `id_evento`) y la copia del periodo en la misma transacción, como el receptor de Usuarios.
- **Clave en cada entorno:**
  - local y CI: `.env.ejemplo` y el *workflow* con un valor ficticio;
  - equipo: `compose.demo.yml` la pasa a `datos` y a `identidad`; `preparar.sh` la genera en un equipo nuevo y, en uno que ya existe, **agrega la que falte sin tocar las demás** (como `claves-demo`, paso 11 de `probar_equipo.sh`). El autor corre `sudo deploy/equipo/preparar.sh` en su terminal antes de fusionar.

## 7. La pantalla «Periodos» (FR-010)

- La tabla gana la columna «Responsable de consolidación» (`listarResponsables`), con «Sin designar».
- Con `DISTRIBUTIVO:ESCRITURA`, en un periodo no cerrado: **«Designar responsable»** abre un diálogo con la lista de `listarDocentes` y confirma. Un 400 o un 409 se muestra con su motivo.
- Como la copia de Roles llega por un evento, un periodo recién registrado puede tardar unos instantes en aceptar la designación; si responde «no encontrado», el diálogo dice «El periodo todavía no está disponible para designar. Inténtalo en unos segundos.».

## 8. Glosario (principio III)

- «Responsable de consolidación» ya está en §7.3. **Designación** se usa con su sentido común («designar», ERS §4 y ADR-003) y no hace falta una entrada nueva.

## 9. Pruebas

- **Dominio de Roles, sin dobles:** `Designacion` (periodo cerrado, no docente, quien designa, el mismo otra vez) y `EstadoConocido` (el más avanzado gana).
- **Aplicación, con Mockito:** designar (registra con el anterior y el nuevo; el mismo no registra); recibir el evento (repetido, desordenado, tipo desconocido); `ConsultarAutorizacion` con designación en un periodo abierto y en uno cerrado.
- **Adaptadores, con Testcontainers:** V-n de Roles (tres tablas y la acción nueva); repositorios; el receptor idempotente.
- **Contrato:** las cuatro operaciones; `VerificadorDePermisos`; `ErroresRolesTest` con el código nuevo.
- **Académico:** registrar y cambiar el estado publican `academico.PeriodoActualizado` en su transacción (si la transacción falla, no se publica).
- **Proceso `datos`:** `EsquemasIntegrationTest` con `eventos_datos.event_publication`; `ModulosTest`.
- **SPA:** Vitest (columna, «Sin designar», designar, el 400 y el 409, sin escritura) y Playwright con axe (el diálogo).
- **Punta a punta:** `probar_periodos.sh` suma pasos: registrar un periodo, esperar a que Roles lo conozca, designar a `docente.demo`, comprobar que tras renovar la sesión su token tiene `CONSOLIDACION:ESCRITURA`, rechazar a un no docente y al propio Vicerrector, cerrar el periodo y comprobar que el rol sale del token. La limpieza borra también la designación, la copia y los eventos recibidos.
- **Equipo simulado:** `probar_equipo.sh` con la clave nueva generada por `preparar.sh`, y el paso 11 ampliado: `preparar.sh` agrega la clave que falta sin cambiar las demás.
