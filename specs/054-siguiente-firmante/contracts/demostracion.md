# Contrato: la demostración en los scripts de la firma

F16-H02 (#68). `probar_documento_firmable.sh` ya recorre las tres firmas y revisa las bandejas desde F16-H01. Esta historia le suma la cadena, las notificaciones de firma pendiente y una caída de `identidad` durante F1. `probar_rechazo_en_firma.sh` suma la cadena de la versión nueva.

## `probar_documento_firmable.sh`

### La cadena, en los pasos que ya existen

Con `GET /api/firma/periodos/2026-I/documento` como `vicerrectorado.demo`:

| Paso | Momento | Cadena esperada |
|---|---|---|
| 2 | El documento recién generado | F1 en turno, F2 y F3 pendientes, con los tres nombres impresos |
| 11 | Después de cada firma aceptada | La firmada con `firmadoEn` igual a su `subidoEn`, la siguiente en turno y el resto pendiente |
| 11 | Al final | Las tres firmadas y ninguna en turno |

También pide la consulta como `coordinacion.demo`, que no lee el documento firmable: 403, como hoy.

### La caída de `identidad` durante F1 (paso nuevo 19)

Dentro del paso 11, antes de subir F1:
1. `docker compose stop identidad`.
2. `docente.demo` sube F1: 201, aceptada. La subida de F1 no necesita a `identidad`, porque el designado se reconoce por su usuario (research §8).
3. `docker compose up -d --wait --no-recreate --no-build identidad`. La vuelta se marca antes de levantarlo, como en el paso 17.
4. Sigue con F2 y F3, que sí necesitan a `identidad` para la identificación de quien sube.

Pasa si la notificación de firma pendiente de F2 llegó a `vicerrectorado.demo` una sola vez, antes de `ESPERA_REENVIO` (120 s) desde la vuelta. Con `MDD_CONSERVAR`, el paso no detiene `identidad`.

### Las bandejas (paso 16)

`esperadas()` suma, a las de F16-H01:

| Usuario | Título |
|---|---|
| `vicerrectorado.demo` | Tienes una firma pendiente: «Revisado por» |
| `rectorado.demo` | Tienes una firma pendiente: «Aprobado por» |

Y nada más:
- `docente.demo` no recibe una de firma pendiente: F1 se queda con «El distributivo llegó a «En firma»».
- Nadie recibe una después de F3.

El detalle se comprueba aparte: «Periodo 2026-I, versión 1. «Elaborado por» la firmó {nombre impreso de F1}.», y su autor es `docente.demo`.

### `MDD_CONSERVAR=F2` (nuevo, solo en el perfil local)

Se detiene después de que `docente.demo` sube F1 y se acepta, sin la caída de `identidad` y sin limpiar. Deja F2 en turno para el recorrido a mano de `quickstart.md` §3. Como los otros valores de `MDD_CONSERVAR`, al terminar dice cómo limpiar.

## `probar_rechazo_en_firma.sh`

| Paso | Cadena esperada |
|---|---|
| 2, F1 aceptada | F1 firmada, F2 en turno y F3 pendiente |
| 6, la versión 1 anulada | Con el documento de la versión 1 anulado, la cadena vacía |
| 8, la versión 2 aprobada | F1 en turno, F2 y F3 pendientes, sin las firmas de la versión 1 |

## Workflow

`perfil-consolidado` ya corre los dos scripts. Solo se actualiza el comentario del paso «Documento firmable».
