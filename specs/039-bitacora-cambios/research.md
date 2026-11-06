# Research: Registrar cada cambio en la bitácora

Plan de F17-H01 (issue #69). Cada sección cierra una pregunta del diseño con la decisión, su porqué y lo que se descartó.

## 0. Lo que ya existe

| Pieza | Dónde | Qué hace hoy |
|---|---|---|
| Registro propio de cada servicio | `RegistroDeCambios` (puerto de salida) y `RegistroDeCambiosJdbc` en Generación, Académico, Docentes, Normativa y Roles | Guarda `momento`, `usuario`, `accion` y `detalle` (jsonb) en `<servicio>.registro_cambio`, en la transacción del cambio. 50 acciones en total. |
| Carga de la configuración del flujo | `validacionocs.configuracion_flujo` | Una fila por carga, con quién y cuándo (F12-H01, FR-018: «hasta que llegue la bitácora»). |
| Cargas de archivo | Los traductores de `aplicacion/cargas` de cada servicio dueño | Aplican cada fila llamando a los mismos servicios de mantenimiento, que registran el cambio con el usuario que confirmó. |
| Eventos entre procesos | `:plataforma:eventos` (ADR-018) | Publicación en la transacción, registro en `eventos_<proceso>`, entrega HTTP con token de proceso, reenvío cada 30 s. Lo usan `datos` (Académico → Roles) y `distributivo` (Generación → Usuarios, evento de prueba). |
| Receptor de Usuarios | `RegistrarEventoRecibidoServicio`, `/interno/usuarios/eventos` | Idempotente por `idEvento` (`usuarios.evento_recibido`). Solo admite `generacion.EventoDePrueba`, sin efecto (D-47). |
| Proceso `identidad` | `apps/identidad` | No incluye `:plataforma:eventos`. El esquema `eventos_identidad` ya existe (`deploy/postgres/01-esquemas.sh`). |

Lectores de los registros locales: solo `RepositorioConsolidacionJdbc` (Generación), que lee `detalle ->> 'periodo'` para saber si el consolidado está desactualizado. Por eso el formato del `detalle` local no cambia.

## 1. De dónde sale cada entrada

- **Decision:** del adaptador de `RegistroDeCambios` de cada servicio. Además de insertar la fila local, publica un `EventoEntreProcesos` de tipo `<servicio>.EntradaBitacora` hacia `usuarios`, en la misma transacción.
- **Rationale:**
  - Todos los cambios de los cinco servicios ya pasan por ese puerto, también los de las cargas: con 5 adaptadores se cubren las 50 acciones (D-90).
  - El registro de publicaciones guarda el evento en la misma transacción que el cambio: si el cambio se revierte, el evento también (FR-005), y si `identidad` está caída, queda pendiente (FR-006).
  - La transacción no espera la entrega: el oyente corre después de confirmarla (FR-008, SC-003).
- **Alternatives considered:**
  - Publicar en cada caso de uso: 50 puntos que olvidar, y repite lo que el puerto ya hace.
  - Que Usuarios lea las tablas locales: rompe el principio V.
  - Copiar las tablas locales con una tarea periódica: no es «en la transacción» y suma un mecanismo nuevo (principio X).

## 2. El contenido de cada entrada

- **Decision:** el contenido del evento es `{usuario, accion, momento, periodo, objeto, anterior, nuevo}` (esquema en [contracts/eventos.md](contracts/eventos.md)).
  - `objeto` es el `detalle` que ya guarda cada servicio («sobre qué»), sin cambiar su forma.
  - `anterior` y `nuevo` son mapas explícitos con los valores que cambiaron. Cada registro de cambio (`CambioDeDistributivo`, `CambioRegistrado`, `CambioDeNormativa`, `CambioDeRoles`) los suma como campos nuevos.
  - Cada acción declara su clase: **crea** (solo `nuevo`), **cambia** (los dos) o **quita** (solo `anterior`). Las que pueden crear o cambiar (por ejemplo, `PROPUESTA_GUARDADA`) lo deciden al registrar. El constructor del registro de cambio rechaza un cambio que no cumple su clase.
  - `periodo` va aparte cuando el cambio pertenece a un periodo, para que F17-H02 pueda filtrar sin leer `objeto`.
- **Rationale:** FR-003 pide el valor anterior y el nuevo de forma explícita, y hoy cada servicio los escribe distinto (`horasAnteriores`, `docenteAnterior`, `{campo, anterior, nuevo}`). Normalizar `detalle` rompería el lector de Generación y las pruebas de integración de los cinco servicios, sin beneficio para la bitácora.
- **Alternatives considered:**
  - Copiar `detalle` tal cual: F17-H02 tendría que conocer 50 formatos para mostrar el anterior y el nuevo.
  - Reescribir `detalle` con la forma nueva: rompe el lector de Generación y las filas antiguas tendrían otra forma.

Inventario de clases (lo completa la tarea de cada servicio):

| Servicio | Crea | Cambia | Quita |
|---|---|---|---|
| Generación | `DISTRIBUTIVO_CREADO` | `PROPUESTA_GUARDADA`*, `ACTIVIDAD_GUARDADA`*, `INCONSISTENCIA_JUSTIFICADA`*, `DISTRIBUTIVO_CONSOLIDADO`*, `INFORME_ADJUNTADO`* | `PROPUESTA_QUITADA`, `ACTIVIDAD_QUITADA` |
| Académico | `*_REGISTRADA` y `PERIODO_REGISTRADO` | `*_CORREGIDA`, `*_ESTADO_CAMBIADO`, `PERIODO_ABIERTO`, `UNIDAD_INACTIVADA`, `UNIDAD_REACTIVADA`, `ASIGNATURA_INACTIVADA`, `ASIGNATURA_REACTIVADA`, `ASIGNATURA_PARALELO_RETIRADA`, `ASIGNATURA_PARALELO_REOFERTADA` | — (nada se borra) |
| Docentes | `DOCENTE_REGISTRADO`, `CARGO_REGISTRADO`, `PROYECTO_REGISTRADO` | `DOCENTE_CORREGIDO`, `DOCENTE_ESTADO_CAMBIADO`, `CARGO_CORREGIDO`, `PROYECTO_CORREGIDO` | — |
| Normativa | `VERSION_CREADA` | `VERSION_EDITADA`, `VERSION_ASOCIADA`* | — |
| Roles | `CREAR_ROL`, `INICIALIZAR` | `EDITAR_ROL`, `ASIGNAR_ROL`, `QUITAR_ROL`, `INDICAR_CARRERA`*, `DESIGNAR_RESPONSABLE`* | `BORRAR_ROL`, `QUITAR_CARRERA` |
| Validación OCS | — | `CONFIGURACION_FLUJO_CARGADA`* | — |

\* Crea la primera vez y cambia después: decide al registrar.

**De dónde sale `periodo`** (lo aplica el adaptador de cada servicio; si la acción no está en la tabla, el cambio no pertenece a un periodo y `periodo` falta):

| Servicio | Acciones | `periodo` |
|---|---|---|
| Generación | Todas | `detalle.periodo` |
| Académico | `PERIODO_*` | `detalle.codigo` (el periodo es el elemento) |
| Académico | `ASIGNATURA_PARALELO_*`, `MATRICULA_*` | `detalle.periodo` |
| Normativa | `VERSION_ASOCIADA` | El periodo al que se asocia la versión |
| Roles | `DESIGNAR_RESPONSABLE` | `detalle.periodo` |
| Validación OCS | `CONFIGURACION_FLUJO_CARGADA` | El periodo de la configuración |
| Docentes | Ninguna | — (docentes, cargos y proyectos no pertenecen a un periodo) |

Si al implementar una acción la clave no existe con ese nombre, la tarea del servicio la ajusta y actualiza esta tabla.

**Datos personales (D-91, FR-014):** en las acciones de Docentes, los campos `primerNombre`, `segundoNombre`, `primerApellido`, `segundoApellido`, `correoInstitucional`, `telefono` y `condicionEspecial` llegan a la bitácora con su nombre en `objeto.camposPersonales` y sin valores en `anterior` ni `nuevo`. La identificación se conserva: dice a quién se refiere el cambio. Lo aplica el adaptador de Docentes al armar la entrada; el registro local no cambia. Que la condición especial cambió sigue en la bitácora, porque afecta las horas que admite la política.

**Contenido como mapa:** los emisores arman el contenido como un mapa y no con un DTO generado del contrato de Usuarios, igual que hoy Académico con `PeriodoActualizado`. El esquema `EntradaBitacora` se verifica en el receptor, con `ValidadorDeContrato` y con el dominio.

Justificaciones: el `objeto` y los valores nunca llevan el texto de una justificación (ya es una regla de `CambioDeDistributivo`). `anterior` y `nuevo` dicen si estaba justificada, no qué decía.

## 3. Dónde vive la bitácora y por qué no se puede borrar

- **Decision:** la tabla `usuarios.bitacora` (migración `V5` de Usuarios), con `id_evento` como clave primaria. Se protege en tres capas:
  1. No hay ninguna operación de la API que borre o modifique (FR-009). Una prueba de contrato lo verifica sobre todo `contratos/`.
  2. Un disparador `BEFORE UPDATE OR DELETE` por fila y otro `BEFORE TRUNCATE` por sentencia rechazan el cambio con un error, para cualquier rol (FR-010).
  3. `REVOKE UPDATE, DELETE, TRUNCATE ON usuarios.bitacora FROM identidad`: el rol del proceso solo puede insertar y leer.
- **Rationale:** el disparador vale aunque el código o un operador con la cuenta del servicio lo intente (SC-005). El `REVOKE` es una segunda barrera.
- **Riesgo residual:** el dueño de la tabla puede quitar el disparador con DDL, y un superusuario puede todo. En el prototipo la base no se expone y el equipo de demostración tiene acceso restringido (principio IX). Se documenta y no se agrega un rol más.
- **Alternatives considered:**
  - Un rol dueño distinto del proceso: Flyway corre con el rol del proceso, y cambiar el dueño exige otro rol de inicio por proceso (ADR-013).
  - Encadenar cada entrada con el hash de la anterior: detecta una manipulación, pero ni la ERS ni el issue lo piden (principio X).

## 4. Roles, en el mismo proceso que la bitácora

- **Decision:** `identidad` incluye `:plataforma:eventos`, y Roles publica como los demás. La entrega hacia `usuarios` dentro de `identidad` es **local**: el oyente llama al receptor de Usuarios por su interfaz de aplicación, sin HTTP ni token. Lo fija **ADR-024**.
  - `:plataforma:eventos` define `ReceptorLocal` (`destino()` y `recibir(EventoEntreProcesos)`).
  - `ClienteDeEventos` entrega a un `ReceptorLocal` si hay uno para el destino; si no, por HTTP, como hoy. Un fallo del receptor local es una `EntregaFallida`, y el reenvío lo repite como a cualquier otro.
  - Usuarios publica su receptor como `ReceptorLocal` de `usuarios`.
- **Rationale:**
  - El principio V dice que, dentro de un proceso, los servicios se llaman por la interfaz de aplicación y no por HTTP.
  - Roles y Usuarios mantienen transacciones separadas: la de Roles guarda el cambio y el evento, y la de Usuarios, la entrada (principio V, «cada transacción queda dentro de un servicio»).
  - Si Usuarios falla, la publicación queda en `eventos_identidad` y se reenvía: no se pierde (FR-006).
- **Alternatives considered:**
  - Roles escribe en `usuarios.bitacora`: rompe el principio V.
  - Roles llama a Usuarios en su propia transacción: una transacción entre dos servicios.
  - HTTP hacia el propio proceso: funciona sin código nuevo, pero contradice el principio V y pide un token de proceso a sí mismo.

## 5. El receptor de Usuarios

- **Decision:** `RegistrarEventoRecibidoServicio` deja de usar los manejadores de prueba y admite `<servicio>.EntradaBitacora` de `generacion`, `academico`, `docentes`, `normativa`, `roles` y `validacionocs`. En una sola transacción:
  1. registra el `idEvento` en `evento_recibido` (como hoy);
  2. si es nuevo, construye la `EntradaBitacora` (dominio) y la inserta.
- **Validación de la entrada** (dominio, sin HTTP):
  - el prefijo del tipo coincide con el `origen`;
  - `usuario`, `accion` y `periodo` cumplen sus patrones (contracts/eventos.md);
  - la clase se respeta: sin `anterior` y `nuevo` a la vez vacíos;
  - ninguna clave de `objeto`, `anterior` o `nuevo`, a cualquier profundidad, se parece a un secreto: `clave`, `contrasena`, `password`, `token`, `secreto`, `jwt` (FR-011).
- **Errores:**
  - un tipo u origen no admitido sigue siendo `COMUN_EVENTO_NO_RECONOCIDO` (400);
  - un contenido que no cumple la validación es **`USUARIOS_ENTRADA_BITACORA_INVALIDA`** (400), código nuevo.

  En los dos casos no se registra nada y la publicación queda pendiente en el emisor: no se pierde y se ve en sus registros (edge case «tipo que no conoce»).
- **Rationale:** el receptor es la última barrera contra datos sensibles, aunque cada servicio ya los evite (SC-004).

## 6. Las rutas de entrega

| Proceso | Servicios que publican | Destino `usuarios` | Cambio |
|---|---|---|---|
| `distributivo` | Generación, Validación OCS | Ya configurado | Validación OCS suma un puerto de salida que solo publica. |
| `datos` | Académico, Docentes, Normativa | **Nuevo:** `mdd.eventos.destinos.usuarios` | Docentes y Normativa suman `:plataforma:eventos`. `datos` ya tiene su clave de proceso. |
| `identidad` | Roles | Local (ADR-024) | `identidad` suma `:plataforma:eventos`, con `mdd.eventos.proceso: identidad` y sin clave. |

## 7. El evento de prueba de B00-H05

- **Decision:** se retira, como permite D-47:
  - de Generación: el endpoint, el servicio, el puerto, `mdd.generacion.eventos-prueba.habilitado` y la tabla `evento_prueba` (migración `V7`, `DROP TABLE`);
  - de Usuarios: `manejadoresDePrueba` y el tipo `generacion.EventoDePrueba`;
  - del contrato de Generación, la operación; del de Usuarios, la descripción de prueba.
  - `deploy/probar_eventos.sh` se reemplaza por `deploy/probar_bitacora.sh`, que recorre los mismos escenarios (entrega, caída y vuelta de `identidad`, repetición) con cambios reales, y el paso del workflow `perfil-consolidado` cambia de script.
- **Rationale:** el camino queda probado con entradas reales. Una ruta de prueba menos en producción (D-48) y un script menos que mantener.
- **Alternatives considered:** dejarlo: dos demostraciones del mismo camino.

## 8. Rendimiento y fallos

- Publicar suma una inserción en `eventos_<proceso>` dentro de la transacción del cambio: milisegundos, muy por debajo de los 0,5 s de SC-003. Con `identidad` caída no cambia nada para el usuario.
- SC-002: el reenvío corre cada 30 s. Con `identidad` de vuelta, dos ciclos bastan: menos de 2 minutos.
- Las publicaciones completadas crecen sin purga (ADR-018). Con la bitácora crecen más rápido, pero en el prototipo no es un problema. Se anota como riesgo de operación, sin historia nueva.

## 9. Lo que no cambia

- La matriz de permisos D-16: escribir la bitácora es solo de procesos (rol `PROCESO` en `/interno/**`). Leerla es F17-H02.
- La SPA: no hay pantalla nueva.
- Los registros locales: se conservan con su forma.
- El glosario (`sdd/modelo_dominio.md`, fuera del repositorio): ya define «Bitácora» con `EntradaBitacora` (§7.6) y el lenguaje publicado `EntradaBitacora` (§4). El esquema y los tipos de evento usan ese nombre, y el campo «sobre qué» se llama `objeto`, como en §4.
