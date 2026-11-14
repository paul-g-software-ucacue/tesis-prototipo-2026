# Contrato: prueba de punta a punta

## `deploy/probar_documento_firmable.sh` (ampliado)

Después del paso 6, con el documento generado y `docente.demo` designado:

| Paso | Qué comprueba |
|---|---|
| 7 | `docente.demo` ve exactamente una fila: 2026-I, versión 1, orden 1, «Elaborado por». Descarga el archivo con 200, `application/pdf` y el `ETag` del SHA-256; el archivo descargado tiene el SHA-256 registrado (SC-002) |
| 8 | Quien no está en turno ve `[]` en la lista y recibe 403 en la descarga: `consolidacion.demo` (el rol sin la designación), `vicerrectorado.demo` y `rectorado.demo` (fuera de su turno). `coordinacion.demo`, sin el permiso, recibe 403 en las dos (SC-003) |

`MDD_CONSERVAR=1` no cambia: deja 2026-I en EN_REVISION_VRA con `docente.demo` designado.

**CI:** sin paso nuevo; el paso «Documento firmable» de `perfil-consolidado` ya corre el script.
