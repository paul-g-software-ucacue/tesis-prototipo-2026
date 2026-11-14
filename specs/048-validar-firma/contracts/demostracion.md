# Contrato: prueba de punta a punta

## `deploy/herramientas/FirmarDePrueba.java` (nuevo)

Firma de prueba para los scripts (research §6). Corre en `eclipse-temurin:25-jdk`, con PDFBox y Bouncy Castle copiados del contenedor `distributivo`, y nunca toca la demo.

```text
FirmarDePrueba firmar <entrada.pdf> <salida.pdf> <firmante.p12> <campo> [--ca-ajena] [--alterar]
```

- Firma de forma incremental en el campo vacío `<campo>`, con `adbe.pkcs7.detached`, SHA-256 con RSA y la cadena en el CMS, como FirmaEC (D-46).
- `--ca-ajena`: un certificado de una CA creada en memoria, fuera de la lista de confianza.
- `--alterar`: después de firmar, cambia un byte dentro de lo firmado, fuera de `/Contents`.
- Lee la contraseña ficticia de la variable `MDD_CLAVE_P12`. Nunca la imprime.

## `deploy/probar_documento_firmable.sh` (ampliado)

Después del paso 8, con `docente.demo` en turno:

| Paso | Qué comprueba |
|---|---|
| 9 | Los rechazos (SC-002, SC-004). `docente.demo` sube, y recibe un 422 con su código y las cuatro comprobaciones en los detalles: <ul><li>otro PDF: `FIRMA_NO_ES_LA_VERSION_VIGENTE`;</li><li>el vigente sin firmar: `FIRMA_SIN_UNA_FIRMA_NUEVA`;</li><li>firmado dos veces: `FIRMA_SIN_UNA_FIRMA_NUEVA`;</li><li>firmado y alterado: `FIRMA_NO_INTEGRA`;</li><li>firmado con la CA ajena: `FIRMA_SIN_CONFIANZA`.</li></ul> Después, el turno y el archivo vigente siguen iguales |
| 10 | Quien no está en turno: `vicerrectorado.demo` y `consolidacion.demo` suben un PDF bien firmado y reciben 403, sin fila en `validacion_firma` |
| 11 | Las tres firmas (SC-005). Firma y sube cada uno con su certificado ficticio, y recibe 201 con las cuatro comprobaciones aprobadas: <ul><li>`docente.demo` con F1;</li><li>`vicerrectorado.demo` con F2, después de descargar el vigente nuevo;</li><li>`rectorado.demo` con F3.</li></ul> Después de cada una, el siguiente ve su fila y el anterior no. Al final, nadie tiene pendientes |
| 12 | La base y la bitácora (SC-003): <ul><li>`validacion_firma` tiene 3 aceptadas y 5 rechazadas;</li><li>los archivos de las tres firmas existen con su SHA-256;</li><li>la bitácora tiene 3 `FIRMA_ACEPTADA` y 5 `FIRMA_RECHAZADA` del servicio `firma`</li></ul> |

La limpieza borra `validacion_firma` antes de `firmante_esperado`, y la cuenta al final da 0. `probar_aprobacion.sh` hace lo mismo.

`MDD_CONSERVAR=1` no cambia: deja 2026-I en EN_REVISION_VRA con `docente.demo` designado, para el recorrido con FirmaEC.

**CI:** sin paso nuevo; el paso «Documento firmable» de `perfil-consolidado` ya corre el script.
