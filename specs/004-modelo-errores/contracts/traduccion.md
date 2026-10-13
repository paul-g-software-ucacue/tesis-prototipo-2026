# Contrato: traducción de errores

Es lo que garantizan los 4 procesos y el API Gateway sobre las respuestas de error (escenarios 1 a 3; FR-001 a FR-012; D-38, D-40 a D-42; ADR-016). Las decisiones están en `research.md`.

## En los procesos (`plataforma:errores`)

| Situación | Estado | Código | `detalles` | `idIncidente` |
|---|---|---|---|---|
| Excepción registrada en un `CatalogoDeErrores` (o subclase de una registrada) | El del catálogo | El del catálogo | — | — |
| Cuerpo que no pasa la validación del contrato | 400 | `COMUN_SOLICITUD_INVALIDA` | Uno por campo | — |
| Cuerpo que no es JSON válido o no tiene la forma esperada | 400 | `COMUN_CUERPO_ILEGIBLE` | — | — |
| Ruta inexistente, con token válido | 404 | `COMUN_RECURSO_NO_ENCONTRADO` | — | — |
| Método no permitido | 405 | `COMUN_METODO_NO_PERMITIDO` | — | — |
| Formato pedido no disponible | 406 | `COMUN_FORMATO_NO_ACEPTADO` | — | — |
| Tipo de contenido no soportado | 415 | `COMUN_TIPO_NO_SOPORTADO` | — | — |
| Cualquier otra excepción, o una de dominio o aplicación sin registrar | 500 | `COMUN_ERROR_INTERNO` | — | Sí |
| Error que no pasa por el advice (página `/error`) | El original | Según la correspondencia de `data-model.md` | — | Solo en 5xx |
| Sin token o con un token inválido | 401 | `SEGURIDAD_NO_AUTENTICADO` | — | — |
| `AccessDeniedException` | La atiende la seguridad, no el advice; nunca un 500 | — | — | — |

El 401 por token lo sigue escribiendo `plataforma:seguridad` (ADR-014), sin cambios.

## En el API Gateway (D-40)

| Situación | Estado | Código |
|---|---|---|
| Ruta sin destino, con token válido | 404 | `COMUN_RECURSO_NO_ENCONTRADO` |
| Método no permitido | 405 | `COMUN_METODO_NO_PERMITIDO` |
| No puede conectar con el proceso de la ruta | 503 | `COMUN_SERVICIO_NO_DISPONIBLE` |
| Otro error propio | 500 | `COMUN_ERROR_INTERNO`, con `idIncidente` |
| Sin token o con un token inválido | 401 | `SEGURIDAD_NO_AUTENTICADO` (sin cambios) |

Las respuestas de error que vienen de un proceso pasan sin cambios: el proceso ya respondió con el modelo común.

## Registro del incidente

```text
ERROR … Incidente 6f1c…-…: POST /api/usuarios/sesion falló con org.springframework.jdbc.CannotGetJdbcConnectionException
<traza completa con sus causas, saneada: JWT, Bearer …, clave=, password= y contraseña= aparecen como [oculto]>
```

## Garantías

1. Todas las respuestas de error tienen la forma del esquema `Error`, y nunca traen otros campos ni `null` (FR-001, SC-001).
2. Un error registrado responde con su código, su estado, su título y su descripción del catálogo, sin que el dominio ni la aplicación conozcan HTTP (escenario 1, FR-004).
3. Un error técnico responde 500 con `idIncidente`, sin trazas, clases, SQL ni el mensaje original. El registro tiene la causa y la traza con el mismo `idIncidente`, saneadas (escenario 2, FR-006 a FR-008).
4. Un error de entrada responde 400 con un `detalle` por campo inválido (escenario 3, FR-009, SC-004).
5. El 401 por token, el de credenciales inválidas y las excepciones de seguridad no cambian (SC-005).
6. El API Gateway responde sus propios errores con el modelo común (FR-012, SC-006).
