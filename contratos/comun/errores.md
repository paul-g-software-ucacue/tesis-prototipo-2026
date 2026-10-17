# Catálogo de códigos de error

Cada código es estable y no depende del texto ni del mensaje de una excepción (principio XIII). El esquema `Error` está en [errores.yaml](errores.yaml).

| Código | Estado | Título | Descripción | Cuándo |
|---|---|---|---|---|
| `SEGURIDAD_NO_AUTENTICADO` | 401 | Sesión no válida | Inicia sesión de nuevo para continuar. | Petición a la API sin token, o con un token alterado, vencido, mal formado, firmado con otra llave, con otro algoritmo o de otro emisor. Lo responden el API Gateway y los 4 procesos, siempre con el mismo cuerpo, sin decir qué falló (ADR-014). |
| `USUARIOS_CREDENCIALES_INVALIDAS` | 401 | Credenciales inválidas | El usuario o la contraseña no son correctos. | Inicio de sesión con un usuario inexistente o inactivo, o con una contraseña incorrecta. La respuesta es la misma en todos los casos. Con datos vacíos, el inicio de sesión responde `COMUN_SOLICITUD_INVALIDA` (D-42). |
| `USUARIOS_INICIO_BLOQUEADO` | 429 | Demasiados intentos | Espera unos minutos y vuelve a intentarlo. | Inicio de sesión con un nombre de usuario que tuvo 5 intentos fallidos seguidos, exista o no; el bloqueo dura 15 minutos y la respuesta lleva `Retry-After` (D-60). |
| `COMUN_ERROR_INTERNO` | 500 | Error inesperado | Ocurrió un error inesperado. Intenta de nuevo en unos minutos; si el problema sigue, informa el identificador del incidente. | Excepción técnica no reconocida (por ejemplo, la base de datos no responde), o de dominio o de aplicación sin código en el catálogo. Siempre con `idIncidente`; nunca con trazas, clases, SQL ni el mensaje original. |
| `COMUN_SOLICITUD_INVALIDA` | 400 | Solicitud inválida | Revisa los campos marcados y vuelve a intentarlo. | Uno o más campos no cumplen el contrato. Con `detalles`, uno por campo; también sin `detalles`, para otros errores 4xx del framework sin código propio. |
| `COMUN_CUERPO_ILEGIBLE` | 400 | Solicitud ilegible | El contenido enviado no tiene un formato válido. Revisa los datos y vuelve a intentarlo. | El cuerpo no es JSON válido o no tiene la forma que espera la operación. |
| `COMUN_RECURSO_NO_ENCONTRADO` | 404 | Recurso no encontrado | La dirección solicitada no existe. Revisa la dirección y vuelve a intentarlo. | La ruta no existe. Sin token, el 401 llega antes. |
| `COMUN_METODO_NO_PERMITIDO` | 405 | Operación no permitida | Esta dirección no admite la operación solicitada. | La ruta existe, pero no con ese método HTTP. |
| `COMUN_FORMATO_NO_ACEPTADO` | 406 | Formato no disponible | La respuesta no está disponible en el formato solicitado. | El cliente pide un formato (`Accept`) que la operación no produce. |
| `COMUN_TIPO_NO_SOPORTADO` | 415 | Tipo de contenido no soportado | El contenido enviado está en un tipo que esta operación no acepta. | El cuerpo viene con un `Content-Type` que la operación no acepta. |
| `COMUN_SERVICIO_NO_DISPONIBLE` | 503 | Servicio no disponible | El servicio no está disponible en este momento. Intenta de nuevo en unos minutos. | El API Gateway no puede conectar con el proceso de la ruta (D-40). |
| `COMUN_EVENTO_NO_RECONOCIDO` | 400 | Evento no reconocido | El receptor no admite este tipo de evento. | Un receptor de eventos entre procesos (`/interno/<servicio>/eventos`) recibe un evento cuyo `tipo` no tiene manejador. No lo registra, y el emisor deja la publicación pendiente (ADR-018). |

- Los prefijos `COMUN_` y `SEGURIDAD_` son de la plataforma: los usan todos los procesos y el API Gateway. Los demás códigos llevan el prefijo de su servicio.
- Un código no se renombra ni se reutiliza. Cada fila coincide con el catálogo del código (`CodigosComunes` o `Errores<Servicio>`), y una prueba lo comprueba (ADR-016).
- Para registrar un error de un servicio, ver `backend/plantillas/README.md`.
