# Contrato: Validación OCS (Aprobación)

## `GET /api/validacionocs/periodos/{periodo}/flujo`

`RechazoEnFirmaDelFlujo` suma dos campos y cambia cuándo está presente:

```yaml
versionAnulada:
  description: La versión que se rechazó en firma y quedó anulada.
  type: integer
  format: int32
  minimum: 1
versionNueva:
  description: La versión creada por el rechazo; falta si todavía no existe.
  type: integer
  format: int32
  minimum: 2
```

- `required` suma `versionAnulada`.
- **Presente** mientras el último rechazo en firma no tenga una congelación después: desde el rechazo hasta que se apruebe la versión nueva.
- `TransicionDelFlujo.tipo` suma `VERSION_NUEVA`.
- Con la versión nueva, `acciones` vuelve a traer `APROBAR_CONTENIDO` para el Vicerrector.

## `POST …/aprobacion` y `…/revision`

Sin cambios. El 409 `VALIDACIONOCS_REQUIERE_VERSION_NUEVA` se conserva, pero ya no se alcanza en el flujo normal: la versión nueva llega con el rechazo.

## Oyentes

- **`CierreDeLaFirma.alRechazarseEnFirma(RechazoEnFirma)`**: además de volver a la etapa activa anterior, ordena la versión nueva y guarda el aviso de anulación (research §1).
- **`AnulacionDeLasFirmas.alPedirseLaAnulacion(FirmasPorAnular)`** (nuevo): después de confirmar, llama a `AnularFirmas` de Firma. Si falla, el registro de publicaciones lo reintenta.

## Bitácora

| Acción | Detalle |
|---|---|
| `DISTRIBUTIVO_VERSION_NUEVA` | `periodo`, `versionAnulada`, `version` (la nueva), `motivo`; por `sistema` |
