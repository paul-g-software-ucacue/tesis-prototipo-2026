# Contrato: la demostración en `deploy/probar_documento_firmable.sh`

F18-H02 (#72). El script llega hoy hasta PUBLICADO (paso 16), comprueba que nada se edita (17) y revisa las notificaciones (18 a 21). Esta historia le suma la reforma y su ciclo completo, después del paso 21.

## Pasos nuevos

| Paso | Qué hace | Pasa si |
|---|---|---|
| 15 (cambia) | En APROBADO, `vicerrectorado.demo` intenta iniciar una reforma | 409 `VALIDACIONOCS_ETAPA_NO_ADMITE_REFORMA` |
| 17 (cambia) | Con PUBLICADO, el estado del flujo de cada usuario | Ninguna acción, salvo `INICIAR_REFORMA` para `vicerrectorado.demo` |
| 22. Iniciar la reforma (US1) | En PUBLICADO: `secretaria.demo` y `rectorado.demo` lo intentan; `vicerrectorado.demo`, sin motivo y con uno corto; después, con el motivo y con `identidad` detenido y el token ya pedido; otra vez | Los 403 y los 400 sin cambiar nada; 200 en EN_CONSOLIDACION, con la versión 2 y `reforma`; en Generación, la versión 2 EDITABLE con motivo `REFORMA` y la 1 CONGELADA; `REFORMA_INICIADA` en la bitácora; la segunda, 409 |
| 23. La publicada sigue vigente (US2) | El estado del flujo de quien lo lee | `publicacion` sigue siendo la versión 1, y `reforma` dice la 1, la 2, el motivo, quién y cuándo |
| 24. La notificación (US3) | Las bandejas | «Se inició una reforma del distributivo de 2026-I», una sola vez, para `consolidacion.demo`, `docente.demo`, `datos.demo`, `coordinacion.demo`, `docente.coordinador.demo` y cada cuenta activa de esos roles; no para `vicerrectorado.demo`; la de `docente.demo`, antes de 120 s desde la vuelta de `identidad` |
| 25. El ciclo de la versión 2 (SC-002) | Devolver un parcial, reenviarlo, consolidar, adjuntar el informe, enviar a revisión y aprobar | CONGELADO_EN_FIRMA con la versión 2 y su documento, con la cadena desde F1 |
| 26. Las firmas, la resolución y la publicación | F1, F2 y F3 sobre la versión 2, la resolución y la publicación | PUBLICADO con la versión 2; `publicacion` dice la 2, y ya no hay `reforma` |
| 27. La versión 1, histórica | La base | La versión 1 sigue CONGELADA, con sus tres firmas aceptadas, su documento y su resolución, sin anulación |

## `MDD_CONSERVAR`

- `MDD_CONSERVAR=PUBLICADO` (nuevo) se detiene con la versión 1 publicada, para que el autor inicie la reforma a mano (`quickstart.md` §3).
- Los que ya existen no cambian.

## Workflow

`perfil-consolidado` ya corre el script. Solo se actualiza su comentario.
