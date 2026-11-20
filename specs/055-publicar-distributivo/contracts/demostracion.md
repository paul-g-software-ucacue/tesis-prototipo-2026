# Contrato: la demostración en `deploy/probar_documento_firmable.sh`

F18-H01 (#71). El script llega hoy hasta APROBADO (paso 15) y revisa las notificaciones (pasos 16 a 19). Esta historia le suma la publicación, con la caída de `identidad`, y la versión sin acciones.

## Pasos nuevos, después del 15

| Paso | Qué hace | Pasa si |
|---|---|---|
| 16. Publicar (US1) | Antes de APROBADO, `secretaria.demo` intenta publicar; con APROBADO, `vicerrectorado.demo` y `coordinacion.demo` intentan publicar; `secretaria.demo` publica con `identidad` detenido y el token ya pedido; publica otra vez | El 409 `VALIDACIONOCS_ETAPA_NO_ADMITE_PUBLICACION` antes de APROBADO; los 403 sin dejar transición; 200 con PUBLICADO y `publicacion` (versión 1, `secretaria.demo`); `DISTRIBUTIVO_PUBLICADO` en la bitácora; la segunda, 409 |
| 17. Sin acciones (US3) | Con PUBLICADO, el estado del flujo para cada usuario de la demo; proponer un docente, consolidar, enviar a revisión, aprobar y registrar la resolución | Ninguna acción para nadie; cada operación responde con su error, sin cambiar nada |

El 409 antes de APROBADO se prueba en EN_OCS, durante el paso 13.

## Los pasos de las notificaciones

Se corren después de publicar y pasan a ser del 18 al 21:

| Paso | Cambio |
|---|---|
| 18. Las bandejas | `esperadas()` suma la publicación, «El distributivo de 2026-I fue publicado», para `datos.demo`, `coordinacion.demo`, `docente.demo` y `docente.coordinador.demo`, una sola cada uno; `secretaria.demo` no la recibe |
| 19. La caída de `identidad` | Ahora en la publicación, no en el envío a revisión: la publicación responde 200 sin error, y su notificación llega a `docente.demo` una sola vez antes de `ESPERA_REENVIO` (120 s) desde la vuelta (research §8) |
| 20. La entrega repetida | Sin cambios |
| 21. F1 con `identidad` detenido | Sin cambios (F16-H02) |

## `MDD_CONSERVAR`

- `MDD_CONSERVAR=EN_OCS` sigue deteniéndose en EN_OCS.
- `MDD_CONSERVAR=APROBADO` (nuevo) se detiene con la resolución registrada, para que el autor publique a mano (`quickstart.md` §3).

## Workflow

`perfil-consolidado` ya corre el script. Solo se actualiza su comentario.
