# Research: Consultar la bitácora

Cada sección responde una pregunta del plan con la decisión, el porqué y las alternativas.

## 1. Dónde vive la consulta y quién la protege

**Decisión.** Una operación nueva de Usuarios, `GET /api/usuarios/bitacora`, en el proceso `identidad`, con `x-mdd-permiso: BITACORA:LECTURA` y `@PreAuthorize("hasAuthority('PERMISO_BITACORA:LECTURA')")`, como `listarUsuarios`. La SPA la llama por el API Gateway, que ya enruta `/api/usuarios/**`. Un usuario sin el permiso recibe `SEGURIDAD_ACCESO_NO_AUTORIZADO` (403) y una petición sin sesión `401`, de `contratos/comun/seguridad.yaml`.

**Por qué.** La bitácora es de Usuarios (D-47, F17-H01) y el permiso ya existe en la matriz D-16: lectura de `BITACORA` para `ADMINISTRADOR` y `VICERRECTOR`, alcance `TODO` (`roles/V2__matriz_d16.sql`). ADR-021 valida el permiso en el backend con el mismo mecanismo de las demás operaciones, y el 403 no dice qué permiso falta.

**Alternativas.** Una consulta en Roles o en otro proceso: tendría que leer la tabla de Usuarios (principio V). Un permiso propio para la consulta: la matriz ya lo tiene.

## 2. Páginas estables

**Decisión.** Paginación por número de página (`pagina` desde 1, `tamano` de 1 a 100, por defecto 50), orden `momento` descendente y `id_evento` como desempate. La primera respuesta devuelve `corte`, el instante de la consulta, y las páginas siguientes lo envían: solo cuentan las entradas con `recibida_en <= corte`. La respuesta trae `total`, el número de entradas que cumplen el filtro hasta el corte.

**Por qué.** Una entrada puede llegar tarde, con un `momento` antiguo, si `identidad` estuvo detenido (F17-H01, US3). Sin el corte, esa entrada empujaría las páginas ya vistas y una entrada se vería dos veces o ninguna (caso borde de la especificación). El corte es un solo parámetro y usa una columna que ya existe. Con 100 como máximo, ninguna respuesta trae la bitácora entera (FR-004).

**Riesgo residual.** `recibida_en` es la hora de inicio de la transacción que la insertó. Una entrada cuya transacción empezó antes del corte y se confirmó después aparece en la siguiente página sin haber estado en la primera. Es una ventana de milisegundos, y la entrada no se pierde: se ve al volver a consultar.

**Alternativas.** Paginación por cursor (`momento`, `id_evento`): estable, pero no da el número de página ni el total que pide la pantalla. Sin corte: más simple, pero no cumple el caso borde. Devolver todo y paginar en la SPA: la bitácora crece con cada carga (600 entradas por `datos.demo`).

## 3. Fechas y hora de Ecuador

**Decisión.** `desde` y `hasta` son fechas (`format: date`) sin hora, inclusivas, en la hora de Ecuador continental (`America/Guayaquil`, UTC−5, sin horario de verano). Usuarios las convierte en el intervalo `[desde 00:00 −05:00, hasta + 1 día 00:00 −05:00)` sobre `momento`. La SPA muestra `momento` con `Intl.DateTimeFormat('es-EC', { timeZone: 'America/Guayaquil' })`, explícito, para que el navegador de un auditor con otra zona horaria muestre lo mismo.

**Por qué.** La institución está en Ecuador continental y el filtro «del 7 de noviembre» debe incluir el día local completo (caso borde). La zona se fija en una constante de Usuarios y en la SPA, sin depender de la zona del servidor ni del navegador.

**Alternativas.** Instantes con hora en el filtro: más preciso, pero la historia pide fechas y obliga al auditor a pensar en UTC. La zona del servidor: el contenedor corre en UTC.

## 4. El filtro por objeto

**Decisión.** Dos filtros, que se combinan con los demás (Q2 de la especificación):

1. **Tipo de cambio:** `servicio` (enumeración con los seis servicios) y `accion` (patrón `^[A-Z][A-Z_]{2,39}$`), que solo se admite junto con `servicio`. La SPA llena la lista de acciones con `GET /api/usuarios/bitacora/acciones`, que devuelve los pares distintos `(servicio, accion)` que hay en la bitácora, con el mismo permiso.
2. **Identificador:** `identificador`, `^[0-9A-Za-z._/-]{1,64}$`. Una entrada lo cumple si algún valor de su `objeto`, a cualquier profundidad, es igual al buscado: un texto igual, o un número cuya representación es igual (el `numero` de una versión). Se resuelve con `jsonb_path_exists` y el valor como parámetro (`$id`), nunca concatenado.

**Regla del identificador (principio XIV).** Se revisaron los patrones de los contratos de los seis servicios: los identificadores usan letras sin tilde, dígitos, `.`, `_`, `-` y, en el objeto de una inconsistencia de Generación, `/` (`^[0-9A-Za-z._/-]{1,60}$`). El más largo es la huella SHA-256 de un archivo (64). Los nombres de usuario son de 1 a 64 sin otro formato, pero se buscan con el filtro de usuario, no con este.

**Por qué.** El tipo de cambio responde «qué cambios de este tipo hubo»; el identificador, «qué le pasó a este docente o a esta asignatura». Buscar entre los valores del objeto evita que Usuarios conozca la forma del objeto de cada servicio: la bitácora la recibe tal como la arma el servicio (F17-H01). La lista de acciones sale de la bitácora y no de una copia de los enumerados de cada servicio, así que una acción nueva de F12 a F18 aparece sola.

**Falsos positivos aceptados.** Un identificador muy corto puede coincidir con otro valor del objeto (por ejemplo, `4` con las horas de una actividad). El auditor lo acota con el tipo de cambio o el periodo; el filtro nunca deja fuera una entrada que tiene el identificador.

**Alternativas.** Una lista cerrada de claves de identificador en Usuarios (`identificacion`, `codigo`, `asignatura`…): más precisa, pero acopla Usuarios a la forma del objeto de cada servicio. Las acciones como enumerado en el contrato: hay más de 60 y cada historia nueva tendría que tocar el contrato de Usuarios. Búsqueda parcial: fuera de alcance (la especificación pide exacto).

## 5. Rendimiento

**Decisión.** Sin índices nuevos. Las consultas usan `bitacora_momento` y `bitacora_periodo_momento` (V5); el filtro por usuario, por servicio y acción, y por identificador se aplica sobre esas filas.

**Por qué.** SC-003 pide la primera página en menos de 2 s con 10 000 entradas. Con esa cantidad, un recorrido completo de la tabla con `jsonb_path_exists` cuesta milisegundos en PostgreSQL. La prueba de integración lo mide con 10 000 entradas; si no cumple, se agrega un índice `(usuario, momento)` en una migración `V6`, sin cambiar el contrato.

**Alternativas.** Un índice GIN sobre `objeto`: no sirve para `$.**` con igualdad y suma escritura en cada entrada.

## 6. Guardados sin cambios (D-94)

**Decisión.** La regla se aplica en el caso de uso de cada acción, comparando el elemento de dominio antes y después, como ya lo hacen Académico, Docentes y Normativa. Si es igual, el caso de uso no guarda ni registra y responde con el elemento que había.

Inventario de las acciones que pueden cambiar algo (clases `CAMBIA` y `CREA_O_CAMBIA` de F17-H01):

| Servicio | Acción | ¿Ya lo cumple? | Qué se compara |
|---|---|---|---|
| Generación | `PROPUESTA_GUARDADA` | No | Docente, tipo y texto de la justificación |
| Generación | `ACTIVIDAD_GUARDADA` | No: es el caso de la nota (4 a 4) | Las horas (el proyecto es parte de la clave) |
| Generación | `INCONSISTENCIA_JUSTIFICADA` | No | El texto de la justificación |
| Generación | `DISTRIBUTIVO_CONSOLIDADO`, `INFORME_ADJUNTADO` | Son actos: registran siempre | — |
| Académico | `PERIODO_CORREGIDO`, `UNIDAD_CORREGIDA`, `CARRERA_CORREGIDA`, `ASIGNATURA_CORREGIDA`, `ASIGNATURA_PARALELO_CORREGIDA` | Sí (`correccion.cambios().isEmpty()`) | — |
| Académico | `PERIODO_ESTADO_CAMBIADO`, `UNIDAD_*`, `CARRERA_ESTADO_CAMBIADO`, `MALLA_ESTADO_CAMBIADO`, `ASIGNATURA_INACTIVADA/REACTIVADA`, `ASIGNATURA_PARALELO_RETIRADA/REOFERTADA` | Sí (mismo estado) | — |
| Académico | `PERIODO_ABIERTO` | Sí (el periodo con versión no vuelve a abrirse) | — |
| Académico | `MATRICULA_CORREGIDA` | No: la carga 10 corrige aunque la fila sea igual | Los valores de la matrícula |
| Docentes | `DOCENTE_CORREGIDO`, `DOCENTE_ESTADO_CAMBIADO`, `CARGO_CORREGIDO`, `PROYECTO_CORREGIDO` | Sí | — |
| Normativa | `VERSION_EDITADA` | Sí (`edicion.cambios().isEmpty()`) | — |
| Normativa | `VERSION_ASOCIADA` | Sí (la misma versión devuelve la suya) | — |
| Roles | `EDITAR_ROL` | No | Nombre, descripción y permisos |
| Roles | `INDICAR_CARRERA` | No | La carrera |
| Roles | `ASIGNAR_ROL`, `QUITAR_ROL` | Sí (solo los roles cambiados) | — |
| Roles | `DESIGNAR_RESPONSABLE` | Sí (`anterior.get() == nueva`) | — |
| Validación OCS | `CONFIGURACION_FLUJO_CARGADA` | Es un acto (clase crea) | — |

Faltan seis acciones en tres servicios. Además, un cambio solo en el texto de una justificación (propuesta o inconsistencia) deja su entrada con `objeto.textoCambiado = true`, porque `anterior` y `nuevo` no llevan el texto.

**Por qué.** Comparar en el adaptador del registro (`anterior.equals(nuevo)`) perdería cambios reales: el texto de una justificación no está en `anterior` ni en `nuevo` (F17-H01), así que cambiar solo el texto parecería «sin cambios». El caso de uso tiene el elemento entero. Además, si el caso de uso guardara igual y solo se omitiera la entrada, el dato cambiaría (quién y cuándo) sin registro, contra FR-005 de F17-H01.

**Alternativas.** Un filtro en cada `RegistroDeCambiosJdbc`: descartado por lo anterior. Rechazar en el registro de cambio un `CAMBIA` con `anterior = nuevo`: convertiría un descuido en un error 500 para el usuario.

## 7. La pantalla

**Decisión.** Una pantalla nueva `frontend/src/pantallas/bitacora/`, ruta `/bitacora`, en el menú como «Bitácora» con el permiso `{ funcionalidad: 'BITACORA', tipo: 'LECTURA' }` (D-70). Filtros con `react-hook-form` y `zod`, con las mismas reglas del contrato mientras se escribe y antes de enviar (principio XIV). La tabla muestra momento, usuario, servicio, acción, periodo y objeto; cada fila se expande para ver el objeto, el valor anterior y el nuevo como pares campo–valor, con «sin valor anterior (creación)» y «sin valor nuevo (eliminación)» cuando faltan. Los filtros viven en la URL, para volver a la misma consulta y compartirla.

**Por qué.** Es el patrón de las demás pantallas (`docentes`, `inconsistencias`): MUI, el cliente generado desde `contratos/` y React Query. El usuario se escribe, no se elige: la lista de usuarios exige `USUARIOS_ROLES:LECTURA`, que el Vicerrector no tiene, y la bitácora también tiene a `sistema`.

**Alternativas.** Mostrar el JSON crudo: no es legible para un auditor. Un selector de usuarios: pediría otro permiso al Vicerrector (cambiaría D-16).

## 8. Errores

**Decisión.** Sin códigos nuevos. Un filtro que no cumple su regla, una acción sin servicio y un rango al revés responden `COMUN_SOLICITUD_INVALIDA` con un `detalle` por campo (`hasta`: «La fecha hasta no puede ser anterior a la fecha desde.»). La regla de cruce se valida en el adaptador de entrada, con `SolicitudInvalida`, y en el dominio de la consulta.

**Por qué.** El catálogo ya tiene el código de validación con detalles por campo (D-72); un código propio no le dice nada más al usuario.

## 9. La demostración

**Decisión.** Un script nuevo `deploy/probar_consulta_bitacora.sh`, que corre en `perfil-consolidado` después de `probar_bitacora.sh`. La SPA tiene su prueba de punta a punta en `frontend/e2e/bitacora.spec.ts`. `probar_bitacora.sh` sigue igual: sus pasos usan valores distintos en cada cambio (4, 6, 3, 5…), así que la regla de D-94 no le quita entradas; el plan lo confirma en una tarea al correrlo.

**Por qué.** Cada historia deja su script en el workflow (constitución §6) y los de F17-H01 no prueban la consulta.
