# Data Model: Consultar la bitácora

Esta historia no agrega tablas ni columnas. Lee `usuarios.bitacora` (V5, F17-H01) y agrega dos objetos de valor en Usuarios para la consulta. En Generación, Académico y Roles, seis acciones dejan de registrar cuando nada cambió (D-94).

## Filtro de la bitácora (Usuarios, dominio)

Los criterios de una consulta. No se guarda. Se construye con `FiltroDeBitacora.de(...)`, que valida las reglas y falla con `FiltroDeBitacoraInvalido` (traducido a `COMUN_SOLICITUD_INVALIDA`, con un detalle por campo).

| Campo | Tipo | Regla | Requisito |
|---|---|---|---|
| `desde` | fecha, opcional | Una fecha válida. | FR-002 |
| `hasta` | fecha, opcional | Una fecha válida, no anterior a `desde` si las dos están. | FR-002, FR-010 |
| `usuario` | texto, opcional | De 1 a 64 caracteres, sin otro formato (como `usuarios.nombre_usuario`). Se compara exacto. | FR-002, FR-010 |
| `servicio` | enumerado, opcional | `generacion`, `academico`, `docentes`, `normativa`, `roles` o `validacionocs`. | FR-002, FR-010 |
| `accion` | texto, opcional | `^[A-Z][A-Z_]{2,39}$`. Solo con `servicio`. | FR-002, FR-010 |
| `identificador` | texto, opcional | `^[0-9A-Za-z._/-]{1,64}$` (research §4). Se compara exacto con los valores del objeto. | FR-002, FR-010 |
| `periodo` | texto, opcional | `^[A-Za-z0-9._-]{1,20}$`, como el periodo de la entrada. | FR-002, FR-010 |

**Intervalo.** `desde` y `hasta` se convierten en `[desde 00:00, hasta + 1 día 00:00)` en `America/Guayaquil` (research §3). Sin `desde`, no hay límite inferior; sin `hasta`, no hay límite superior.

**Combinación.** Todos los campos presentes se cumplen a la vez (FR-003). Un filtro vacío devuelve toda la bitácora, por páginas.

## Página pedida (Usuarios, dominio)

| Campo | Tipo | Regla |
|---|---|---|
| `pagina` | entero | Desde 1. Por defecto, 1. |
| `tamano` | entero | De 1 a 100. Por defecto, 50. |
| `corte` | instante, opcional | El `corte` de la primera respuesta. Sin él, el instante de la consulta. No puede ser posterior al instante de la consulta. |

Una página más allá de la última devuelve una lista vacía, con el total, sin error.

## Página de entradas (respuesta)

| Campo | Tipo | Nota |
|---|---|---|
| `entradas` | lista de `EntradaConsultada` | Orden `momento` descendente, `id_evento` descendente como desempate. |
| `total` | entero | Entradas que cumplen el filtro con `recibida_en <= corte`. |
| `pagina`, `tamano` | entero | Los de la petición, ya con sus valores por defecto. |
| `corte` | instante | Para pedir las páginas siguientes (research §2). |

## Entrada consultada

La `EntradaBitacora` de F17-H01, como se guardó, más su identificador y el servicio. No se agrega ni se reconstruye nada: los datos personales de un docente siguen sin valores (D-91).

| Campo | Origen |
|---|---|
| `id` | `id_evento` |
| `servicio` | `servicio` |
| `usuario`, `accion`, `momento`, `periodo` | Las columnas del mismo nombre |
| `objeto`, `anterior`, `nuevo` | Las columnas `jsonb`; `anterior` falta al crear y `nuevo` al quitar |

`recibida_en` no se muestra: es un dato técnico del reenvío.

## Acción conocida

Un par distinto `(servicio, accion)` de la bitácora, ordenado por servicio y acción. Llena la lista de acciones de la pantalla (research §4).

## Puerto de salida `RepositorioBitacora`

Suma dos consultas de solo lectura; `registrar` no cambia (FR-006, FR-009 de F17-H01).

| Método | Qué hace |
|---|---|
| `consultar(FiltroDeBitacora, PaginaPedida, Instant corte)` | Las entradas de la página y el total, con un `SELECT` parametrizado: ningún valor del filtro se concatena en el SQL. |
| `acciones()` | Los pares `(servicio, accion)` distintos. |

## Guardados sin cambios (D-94)

Ningún dato nuevo. En cada caso de uso, el elemento antes y después se compara con estos datos del negocio; si es igual, no se guarda ni se registra, y se devuelve el que había.

| Servicio | Caso de uso | Acción | Igual si |
|---|---|---|---|
| Generación | `ProponerDocenteServicio.proponer` | `PROPUESTA_GUARDADA` | Mismo docente, mismo tipo y mismo texto de justificación |
| Generación | `AsignarActividadServicio.asignar` | `ACTIVIDAD_GUARDADA` | Mismas horas (la carrera, el docente, la actividad y el proyecto son la clave) |
| Generación | `JustificarInconsistenciaServicio.justificar` | `INCONSISTENCIA_JUSTIFICADA` | Ya justificada con el mismo texto |
| Académico | `MantenerMatriculasServicio.corregir` | `MATRICULA_CORREGIDA` | Los mismos valores de la matrícula |
| Roles | `GestionarRolesServicio.editar` | `EDITAR_ROL` | Mismo nombre, misma descripción y mismos permisos |
| Roles | `AsignarCarreraServicio.indicar` | `INDICAR_CARRERA` | La misma carrera |

Quién y cuándo (`asignadaPor`, `asignadaEn`, `justificadaPor`, `justificadaEn`) no cuentan. Si cambia solo el texto de una justificación, la entrada lleva `objeto.textoCambiado = true`; con el tipo o el docente también cambiados, `anterior` y `nuevo` ya lo muestran y `textoCambiado` va igual si el texto cambió.
