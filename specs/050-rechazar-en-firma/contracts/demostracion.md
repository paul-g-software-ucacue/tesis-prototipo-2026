# Contrato: prueba de punta a punta

## `deploy/probar_rechazo_en_firma.sh` (nuevo)

En `perfil-consolidado`, después de `probar_documento_firmable.sh`. Usa la CA de prueba y `deploy/herramientas/FirmarDePrueba.java` como ese script.

| Paso | Qué comprueba |
|---|---|
| 0 | La base sin estructura, distributivos ni procesos de aprobación; si no, falla sin tocar nada |
| 1 | El consolidado aprobado por `vicerrectorado.demo`: CONGELADO_EN_FIRMA y el documento generado |
| 2 | `docente.demo` sube F1 firmada con `firmante-1.p12`: se acepta; F2 en turno |
| 3 | `rectorado.demo` (fuera de turno) rechaza con observación: 403, sin fila |
| 4 | `vicerrectorado.demo` rechaza sin observación y con 5 caracteres: 400 `FIRMA_OBSERVACION_INVALIDA`, sin fila |
| 5 | `vicerrectorado.demo` rechaza con observación: 201 con el paso 2 y la observación |
| 6 | En menos de 30 s, el flujo en EN_REVISION_VRA, con la transición `RECHAZO_EN_FIRMA` por `sistema` y `rechazoEnFirma` con la observación; sin la acción de aprobar |
| 7 | `vicerrectorado.demo` sube F2 firmada y rechaza otra vez: 403 las dos; nadie tiene pendientes |
| 8 | `vicerrectorado.demo` aprueba el contenido: 409 `VALIDACIONOCS_REQUIERE_VERSION_NUEVA`; sigue en EN_REVISION_VRA |
| 9 | La base: 1 firma aceptada, 1 rechazo en firma, la transición; la bitácora con `RECHAZO_EN_FIRMA` de `firma` y `DISTRIBUTIVO_RECHAZADO_EN_FIRMA` de `validacionocs` |

Al terminar, aunque un paso falle, borra lo que creó (también los archivos del volumen y las filas de las dos tablas nuevas), salvo la bitácora. Termina con 0 si todos los pasos pasan. Nunca imprime una clave ni un token. `MDD_CONSERVAR=1`, solo en local, deja el periodo en CONGELADO_EN_FIRMA con F1 aceptada, para recorrer `quickstart.md` §3.
