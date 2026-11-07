# Contrato: Usuarios

Cambios en `contratos/usuarios/openapi.yaml` (el esquema `EntradaBitacora` de [eventos.md](eventos.md) y el receptor) y en `contratos/comun/errores.md`. No hay operaciones públicas nuevas: escribir la bitácora es solo de procesos, y leerla es F17-H02.

## `POST /interno/usuarios/eventos` (`recibirEvento`)

Misma operación, mismo cuerpo (`EventoEntreProcesos`) y misma seguridad: solo un token de proceso (rol `PROCESO`), y el API Gateway no la enruta.

**Cambia la descripción:** deja de ser el receptor de prueba de B00-H05. Admite los seis tipos `<servicio>.EntradaBitacora` ([eventos.md](eventos.md)) y registra cada uno como una entrada de la bitácora.

| Respuesta | Cuándo |
|---|---|
| `204` | Entrada registrada, o ya registrada antes por el mismo `idEvento`. |
| `400 COMUN_SOLICITUD_INVALIDA` | El sobre no cumple el contrato, con `detalles`. |
| `400 COMUN_EVENTO_NO_RECONOCIDO` | El tipo no es uno de los seis, o su prefijo no coincide con el `origen`. |
| `400 USUARIOS_ENTRADA_BITACORA_INVALIDA` | **Nuevo.** El contenido no cumple `EntradaBitacora` o sus invariantes (data-model.md). |
| `401` / `403` | Sin token, o con un token que no es de proceso. |

En los tres `400` no se registra nada: la publicación queda pendiente en el emisor.

Dentro del proceso `identidad`, Roles entrega por el mismo receptor sin HTTP (ADR-024). El resultado es el mismo: una excepción equivale a un `400` y deja la publicación pendiente.

## Código nuevo del catálogo

| Campo | Valor |
|---|---|
| Código | `USUARIOS_ENTRADA_BITACORA_INVALIDA` |
| Estado | 400 |
| Título | Entrada de bitácora no válida |
| Descripción | La bitácora no admite este cambio tal como llegó. |
| Causa | El contenido de una `EntradaBitacora` no cumple su esquema o sus invariantes: falta `usuario`, `accion`, `momento` o `objeto`; un patrón no se cumple; faltan `anterior` y `nuevo` a la vez; pasa de 64 KB; o tiene una clave que parece un secreto. |
| Clasificación | Validación |
| Acción | — (solo lo recibe otro proceso, nunca la SPA) |
| Descripción técnica | `EntradaBitacoraInvalida`, del dominio de Usuarios; la traduce `ErroresUsuarios`. El emisor deja la publicación `FAILED` y la reintenta: hay que corregir el servicio emisor. El motivo se registra en Usuarios sin el contenido. |

## Lo que se retira

- La mención a `generacion.EventoDePrueba` en la descripción de `recibirEvento`.
