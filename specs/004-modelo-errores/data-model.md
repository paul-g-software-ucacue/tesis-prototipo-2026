# Modelo de datos (fase 1): B00-H04

No hay datos persistidos. El modelo describe las piezas del modelo de errores (Key Entities de la especificación) y sus reglas.

## Error (`RespuestaError`)

| Campo | Tipo | Regla |
|---|---|---|
| `titulo` | texto | Obligatorio. El del catálogo. |
| `descripcion` | texto | Obligatorio. Dice qué puede hacer el usuario, en español (RNF-USA-01). |
| `codigo` | texto | Obligatorio. Un código del catálogo, en mayúsculas con guion bajo. |
| `detalles` | lista de `DetalleError` | Solo en `COMUN_SOLICITUD_INVALIDA`, con al menos un elemento. En los demás errores no se envía. |
| `idIncidente` | texto (UUID v4) | Solo en `COMUN_ERROR_INTERNO`. En los demás errores no se envía. |

**Reglas:**
- Nunca hay otros campos.
- Nunca hay trazas, clases, paquetes, métodos, SQL, mensajes originales, credenciales, tokens ni direcciones internas (principio XIII).
- Los campos sin valor no se envían: no hay `null` en el JSON.

## Detalle de error (`DetalleError`)

| Campo | Regla |
|---|---|
| `campo` | Nombre del campo como en el JSON del contrato (por ejemplo, `usuario`). |
| `descripcion` | «es obligatorio», «debe tener entre {min} y {max} caracteres», «no tiene el formato esperado» o «no es válido» (research §7). |

## Definición de un código (`DefinicionDeCodigo`)

| Campo | Regla |
|---|---|
| `codigo` | En mayúsculas con guion bajo. Prefijo del servicio (por ejemplo, `USUARIOS_`); `COMUN_` y `SEGURIDAD_` son solo de la plataforma. |
| `estado` | Un 4xx o 5xx. 405, 406 y 415 solo en los códigos comunes del framework. |
| `titulo` y `descripcion` | Iguales a su fila en `contratos/comun/errores.md`. |

`CodigosComunes` es una lista de definiciones, sin excepción asociada. `VerificadorDeCatalogo` recibe definiciones.

## Entrada del catálogo (`EntradaCatalogo`)

| Campo | Regla |
|---|---|
| `excepcion` | Clase de la excepción de dominio o de aplicación. Única en el proceso. |
| `definicion` | Una `DefinicionDeCodigo`. El código es único en el proceso. |

**Validaciones:**
- Al arrancar, un código o una excepción repetidos impiden que el proceso arranque.
- En las pruebas, cada entrada coincide con `errores.md`.

## Catálogo de códigos (`contratos/comun/errores.md`)

Tabla `Código | Estado | Título | Descripción | Cuándo`. Después de esta historia tiene:
- `SEGURIDAD_NO_AUTENTICADO` y `USUARIOS_CREDENCIALES_INVALIDAS`, sin cambios (D-38);
- los 8 códigos `COMUN_` de research §4.

## Incidente

| Atributo | Regla |
|---|---|
| `idIncidente` | UUID aleatorio (versión 4), distinto en cada error técnico. |
| En la respuesta | 500, `COMUN_ERROR_INTERNO`, descripción segura con el `idIncidente`. |
| En el registro | Nivel ERROR: `idIncidente`, método, ruta, clase de la excepción y traza completa con sus causas. Saneada: JWT, `Bearer …`, `clave=`, `password=` y `contraseña=` aparecen como `[oculto]`. Si la excepción es de dominio o de aplicación sin registrar, dice `sin código en el catálogo: <clase>`. |

## Correspondencia entre el estado y el código común

Para los errores del framework y del API Gateway que no tienen un código propio:

| Estado | Código |
|---|---|
| 400 (cuerpo ilegible) | `COMUN_CUERPO_ILEGIBLE` |
| 400 (validación) | `COMUN_SOLICITUD_INVALIDA` (con `detalles`) |
| 404 | `COMUN_RECURSO_NO_ENCONTRADO` |
| 405 | `COMUN_METODO_NO_PERMITIDO` |
| 406 | `COMUN_FORMATO_NO_ACEPTADO` |
| 415 | `COMUN_TIPO_NO_SOPORTADO` |
| Otro 4xx | `COMUN_SOLICITUD_INVALIDA` (sin `detalles`) |
| 503 (API Gateway sin conexión con el proceso) | `COMUN_SERVICIO_NO_DISPONIBLE` |
| 5xx | `COMUN_ERROR_INTERNO` (con `idIncidente`) |
