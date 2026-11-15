# Contrato: prueba de punta a punta

## `deploy/probar_documento_firmable.sh` (ampliado)

Con la CA de prueba regenerada, los certificados ficticios tienen el CN del paso (research §5).

| Paso | Cambio |
|---|---|
| 9 | Suma dos rechazos con el vigente de F1, como `docente.demo`: <ul><li>firmado con `firmante-2.p12`, la persona de F2: `FIRMA_FUERA_DE_ORDEN`;</li><li>firmado con `firmante-vencido.p12`: `FIRMA_CERTIFICADO_NO_VIGENTE`.</li></ul> Con los cinco de F14-H02, siete rechazos; el turno y el vigente no cambian |
| 11 | Antes de F2, `vicerrectorado.demo` sube el vigente firmado con `firmante-1.p12`: `FIRMA_FIRMANTE_EQUIVOCADO`. Después, las tres firmas como antes |
| 12 | Las cuentas: 3 aceptadas y 8 rechazadas. Además, el flujo de 2026-I está en EN_OCS, con la transición por `sistema`, en menos de 30 s (SC-002) |

La limpieza ya borra `validacion_firma`; borra también las transiciones a EN_OCS con el proceso de aprobación del periodo.
