# Catálogo de códigos de error

Cada código es estable y no depende del texto ni del mensaje de una excepción (principio XIII). El esquema `Error` está en [errores.yaml](errores.yaml).

| Código | Estado | Título | Descripción | Cuándo |
|---|---|---|---|---|
| `SEGURIDAD_NO_AUTENTICADO` | 401 | Sesión no válida | Inicia sesión de nuevo para continuar. | Petición a la API sin token, o con un token alterado, vencido, mal formado, firmado con otra llave, con otro algoritmo o de otro emisor. Lo responden el API Gateway y los 4 procesos, siempre con el mismo cuerpo, sin decir qué falló (ADR-014). |
| `USUARIOS_CREDENCIALES_INVALIDAS` | 401 | Credenciales inválidas | El usuario o la contraseña no son correctos. | Inicio de sesión con un usuario inexistente o inactivo, una contraseña incorrecta o datos vacíos. La respuesta es la misma en todos los casos. |

- El prefijo `SEGURIDAD_` es común a todo el sistema: la autenticación no es de un solo servicio. Los demás códigos llevan el prefijo de su servicio.
- B00-H04 completa este catálogo, agrega el código genérico de error técnico y no renombra estos códigos (D-38).
