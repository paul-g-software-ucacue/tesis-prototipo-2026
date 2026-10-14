# Contrato: entrega de eventos entre procesos

Es lo que garantizan el emisor (`plataforma:eventos`) y el receptor (Usuarios). Cubre los escenarios 1 a 3, FR-001 a FR-013, D-47, D-49 y ADR-018. Los esquemas OpenAPI se escriben antes del código (principio IV):
- `contratos/comun/eventos.yaml`: el sobre `EventoEntreProcesos`;
- `contratos/usuarios/openapi.yaml`: las dos operaciones internas.

## Rutas internas

| Ruta | Quién la llama | Token | A través del API Gateway |
|---|---|---|---|
| `POST /interno/usuarios/token-proceso` | El emisor, para obtener su token | Ninguno (ruta pública de identidad) | Nunca llega: 401 sin token, 403 con token |
| `POST /interno/usuarios/eventos` | El oyente y la tarea programada del emisor | De proceso (`PROCESO`) | Nunca llega: 401 sin token, 403 con token |

## `POST /interno/usuarios/token-proceso`

Cuerpo `CredencialesProceso`:

```json
{ "proceso": "distributivo", "clave": "…" }
```

| Situación | Estado | Cuerpo |
|---|---|---|
| Proceso con clave configurada y clave correcta | 200 | `TokenSesion` (`token`, `tipo: Bearer`, `venceEn`), con los claims de [data-model.md](../data-model.md#token-de-proceso) |
| Clave incorrecta, o proceso sin clave configurada | 401 | `USUARIOS_CREDENCIALES_INVALIDAS`, con la misma respuesta en los dos casos |
| `proceso` o `clave` vacíos | 400 | `COMUN_SOLICITUD_INVALIDA` con `detalles` |
| Fallo técnico | 500 | `COMUN_ERROR_INTERNO` con `idIncidente` |

La clave se compara en tiempo constante. El registro nunca la escribe, ni tampoco el token.

## `POST /interno/usuarios/eventos` (receptor de prueba, D-47)

Cuerpo `EventoEntreProcesos`:

```json
{
  "idEvento": "3f0e…",
  "tipo": "generacion.EventoDePrueba",
  "origen": "generacion",
  "momento": "2026-10-14T15:04:05Z",
  "contenido": { "nota": "evento de prueba B00-H05" }
}
```

| Situación | Estado | Efecto |
|---|---|---|
| `idEvento` nuevo, de un tipo con manejador | 204 | Una fila en `usuarios.evento_recibido` y el efecto del tipo (en esta historia, ninguno), en una sola transacción |
| `idEvento` ya registrado, también en una entrega simultánea | 204 | Nada; la fila sigue siendo una (escenario 3) |
| `tipo` sin manejador | 400 | `COMUN_EVENTO_NO_RECONOCIDO`; no se registra |
| Cuerpo que no cumple el esquema | 400 | `COMUN_SOLICITUD_INVALIDA` con `detalles` |
| Sin token, o con un token vencido o inválido | 401 | `SEGURIDAD_NO_AUTENTICADO` (ADR-014) |
| Token de usuario (sin `PROCESO`) | 403 | Sin cuerpo hasta F05-H02 |
| Fallo técnico | 500 | `COMUN_ERROR_INTERNO` con `idIncidente` |

Tipos con manejador en esta historia: `generacion.EventoDePrueba`. F17-H01 y F16-H01 agregan los suyos.

## Separación de tokens (`plataforma:seguridad`)

| Token | `/interno/**` | Otras rutas no públicas |
|---|---|---|
| De usuario | 403 | Según la ruta (hoy, autenticado) |
| De proceso | Permitido | 403 |
| Ninguno o inválido | 401 | 401 |

## Emisor (`plataforma:eventos`)

| Resultado de la entrega | Publicación | Registro del proceso |
|---|---|---|
| 2xx | `COMPLETED` | — |
| 401: descarta el token, pide otro y reintenta una vez | Según el reintento | WARN si el reintento falla |
| Otro 4xx o 5xx | `FAILED` | WARN con `idEvento`, `tipo`, `destino` y el estado |
| Plazo vencido, conexión rechazada o host desconocido | `FAILED` | WARN con `idEvento`, `tipo`, `destino` y la clase de la excepción |
| Usuarios no emite el token | `FAILED` | WARN con `idEvento`, `tipo` y la causa |
| Reenvío de la tarea programada | `RESUBMITTED` y después según el resultado | INFO con `idEvento` y `tipo` |

El registro nunca escribe el `contenido`, la clave del proceso ni el token (FR-006).

## Código nuevo del catálogo

| Código | Estado | Título | Descripción | Cuándo |
|---|---|---|---|---|
| `COMUN_EVENTO_NO_RECONOCIDO` | 400 | Evento no reconocido | El receptor no admite este tipo de evento. | Un receptor recibe un evento cuyo `tipo` no tiene manejador. El emisor deja la publicación pendiente. |
