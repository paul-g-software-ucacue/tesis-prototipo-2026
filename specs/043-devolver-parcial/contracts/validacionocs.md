# Contrato: devolver parciales (`contratos/validacionocs/openapi.yaml`)

Una operación nueva en la etiqueta `Aprobacion`, y cambios compatibles en `EstadoDelFlujo`. Se escriben en el contrato antes del código (principio IV).

## `POST /api/validacionocs/periodos/{periodo}/devolucion` · `devolverParciales`

`x-mdd-permiso: CONSOLIDACION:APROBACION` (research §3). `periodo`: `^[A-Za-z0-9._-]{1,20}$`.

**Cuerpo** (`SolicitudDeDevolucion`, `additionalProperties: false`):

```yaml
devoluciones:            # de 1 a 50
  - carrera: 550411C02-P-0301      # ^[0-9A-Z_-]{1,20}$
    observacion: "Faltan docentes en 12 paralelos de primer nivel."   # 10 a 500, pattern '^[\s\S]*\S[\s\S]*$'
```

| Estado | Cuándo |
|---|---|
| `200` · `EstadoDelFlujo` | Devuelto: el distributivo ya está en BORRADOR_CARRERA y cada parcial devuelto trae su `devolucion` |
| `400` `COMUN_SOLICITUD_INVALIDA` | `periodo`, `carrera` u `observacion` no cumplen el contrato, o `devoluciones` está vacía o pasa de 50 |
| `400` `VALIDACIONOCS_OBSERVACION_INVALIDA` | Sin los espacios de los extremos, una observación tiene menos de 10 caracteres; `detalles` nombra la carrera |
| `400` `VALIDACIONOCS_CARRERA_REPETIDA` | La misma carrera dos veces |
| `403` `SEGURIDAD_ACCESO_NO_AUTORIZADO` | Sin el permiso o sin el rol de EN_CONSOLIDACION en la copia de la configuración; el mismo cuerpo en los dos casos |
| `404` `VALIDACIONOCS_SIN_DISTRIBUTIVO` | |
| `404` `VALIDACIONOCS_PARCIAL_NO_ENCONTRADO` | Una carrera sin parcial en el distributivo; no se devuelve ninguna |
| `409` `VALIDACIONOCS_ETAPA_NO_ADMITE_DEVOLUCION` | El distributivo no está en EN_CONSOLIDACION (también la segunda de dos devoluciones simultáneas) |
| `409` `VALIDACIONOCS_DEVOLUCION_DESACTIVADA` | EN_CONSOLIDACION no admite devolución en la copia de la configuración |
| `409` `VALIDACIONOCS_PARCIAL_NO_ENVIADO` | Una de las carreras no tiene su parcial enviado |
| `409` `VALIDACIONOCS_PERIODO_CERRADO` | |
| `401`, `500` | Como las demás |

El orden de las comprobaciones está en research §10. La descripción de la operación no nombra la bitácora (`BitacoraSinBorradoContratoTest`).

## Cambios en `EstadoDelFlujo`

- `ParcialDelFlujo` suma `devolucion` (opcional, `DevolucionDelParcial`, `additionalProperties: false`), presente mientras el parcial está devuelto:
  - `por` (texto), `en` (`date-time`) y `observacion` (texto), todos obligatorios.
- `AccionDelFlujo` suma `DEVOLVER_PARCIALES`: el usuario tiene el rol de EN_CONSOLIDACION, el distributivo está en esa etapa y admite devolución.
- `TransicionDelFlujo.tipo` suma `DEVOLUCION`.

## Pruebas de contrato

- **`FlujoContratoTest`:**
  - el 200 cumple `EstadoDelFlujo`, con la etapa BORRADOR_CARRERA y `devolucion` en los parciales devueltos;
  - cada 400 y cada 409 con su código y su estado;
  - el 403 de los 9 roles que no son el de la etapa, con el mismo cuerpo;
  - una observación de 9 y de 501 caracteres, solo espacios, y `devoluciones` vacía: 400 con su campo.
- **`PermisosValidacionocsTest`** suma `devolverParciales` con `CONSOLIDACION:APROBACION`.
