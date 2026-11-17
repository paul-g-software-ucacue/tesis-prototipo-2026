# Contrato: la demostración en `deploy/probar_documento_firmable.sh`

F16-H01 (#67). El script ya recorre la cadena completa, desde la propuesta de carrera hasta APROBADO. Esta historia le suma la revisión de las bandejas y tres pasos (SC-001 a SC-003).

## Durante los pasos 0 a 15

Después de cada transición, el script espera hasta 20 s a que la notificación llegue. Después comprueba, con `GET /api/notificaciones/bandeja` de cada usuario de la demo:

| Transición | Reciben | No reciben |
|---|---|---|
| Envío de cada parcial | `docente.demo` (el designado) | El coordinador que lo envió |
| Paso a EN_CONSOLIDACION | `docente.demo` | `vicerrectorado.demo` |
| Envío a revisión | `vicerrectorado.demo` | `consolidacion.demo` (el autor) y los coordinadores |
| Aprobar el contenido (CONGELADO_EN_FIRMA) | `docente.demo` | El Vicerrector, que es el autor |
| Paso a EN_OCS por el sistema | `secretaria.demo` | Los firmantes |
| Registrar la resolución (APROBADO) | Nadie, si `secretaria.demo` es la única con el rol de la etapa: es la autora | `secretaria.demo` |

- La comprobación cubre las bandejas de **todos** los usuarios de la demo, no solo los de la tabla: cada transición deja exactamente las notificaciones esperadas, sin faltar ni sobrar ninguna (SC-001).
- Las notificaciones de cada usuario se cuentan desde el inicio del script: la base de la demo puede tener anteriores.
- Si la cadena del script incluye una devolución o un rechazo en firma, se revisa también la observación en el detalle.

## Pasos nuevos

| Paso | Qué hace | Pasa si |
|---|---|---|
| 16. Bandeja y lectura (US2) | `vicerrectorado.demo` lee `sin-leer`, marca la del envío a revisión y la marca otra vez; intenta marcar una de `secretaria.demo` | `sin-leer` baja en 1; la segunda marca da 200 sin cambiar `leidaEn`; la ajena da 404 `NOTIFICACIONES_NOTIFICACION_NO_ENCONTRADA` |
| 17. Caída de `identidad` (SC-002) | Antes del paso 15, con los tokens ya pedidos: `docker compose stop identidad`, registrar la resolución, `start identidad` | El registro da 200 sin error, y antes de 2 minutos la notificación de APROBADO está una sola vez |
| 18. Entrega repetida (SC-003) | Vuelve a `FAILED` una publicación completada de `eventos_distributivo` (como `probar_bitacora.sh`, paso 8) y espera el reenvío | La publicación se completa otra vez, y cada bandeja sigue con una sola notificación de ese evento, con su `leidaEn` |

## Limpieza

- Las notificaciones no se pueden borrar: es lo que se prueba.
- El script cuenta solo las suyas, por `momento` desde su inicio.
- `MDD_CONSERVAR` las deja para el recorrido a mano.

## Workflow

`perfil-consolidado` ya corre este script. Solo se actualiza su comentario.
