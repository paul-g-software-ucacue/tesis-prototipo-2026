# Contrato: prueba de punta a punta

## `deploy/probar_documento_firmable.sh` (nuevo)

Parte de `probar_aprobacion.sh`: exige una base sin datos, carga los de prueba, borra al final lo que creó (también los archivos de `documentos/`) y nunca imprime una clave ni un token.

| Paso | Qué comprueba |
|---|---|
| 1 | El consolidado listo, enviado a revisión y aprobado por `vicerrectorado.demo`: CONGELADO_EN_FIRMA |
| 2 | El documento GENERADO en menos de 30 s, sin que nadie lo pida (SC-003); `generadoPor` es `sistema` y `hashContenido` el de la congelación (SC-002) |
| 3 | El archivo del volumen se llama `documentos/<sha256>.pdf` y su SHA-256 es el registrado (SC-004) |
| 4 | veraPDF (`verapdf/cli:v1.30.2`, perfil 2b) lo da conforme, con los tres campos de firma vacíos `firma-1-…`, `firma-2-…`, `firma-3-…` (SC-001) |
| 5 | Un `POST` del Responsable responde el mismo documento, sin otro archivo ni otra entrada; `coordinacion.demo` recibe 403 |
| 6 | La bitácora tiene exactamente una `DOCUMENTO_FIRMABLE_GENERADO` con la versión, el hash y el SHA-256 |

**CI:** paso «Documento firmable» en `.github/workflows/perfil-consolidado.yml`, después de «Aprobar y congelar».

**`MDD_CONSERVAR=1`:** como en `probar_aprobacion.sh`: hace el paso 1 hasta EN_REVISION_VRA y no limpia, solo contra el perfil local.
