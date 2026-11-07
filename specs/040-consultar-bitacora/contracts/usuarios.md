# Contrato: consulta de la bitácora (`contratos/usuarios/openapi.yaml`)

Dos operaciones nuevas, en la etiqueta `Bitacora`. Se escriben en el contrato antes del código (principio IV); el generador produce `BitacoraApi` y la SPA regenera `src/api/generado/usuarios.d.ts`. Ninguna operación nueva escribe, modifica ni borra la bitácora: la prueba `BitacoraSinBorradoContratoTest` de F17-H01 sigue en verde sin cambios.

## `GET /api/usuarios/bitacora` · `consultarBitacora`

`x-mdd-permiso: BITACORA:LECTURA`

### Parámetros (query, todos opcionales)

| Nombre | Esquema | Regla |
|---|---|---|
| `desde` | `string`, `format: date` | Fecha inicial, inclusiva, en hora de Ecuador continental. |
| `hasta` | `string`, `format: date` | Fecha final, inclusiva. No anterior a `desde`. |
| `usuario` | `string`, `minLength: 1`, `maxLength: 64` | Exacto. |
| `servicio` | `string`, `enum: [generacion, academico, docentes, normativa, roles, validacionocs]` | |
| `accion` | `string`, `pattern: '^[A-Z][A-Z_]{2,39}$'` | Solo con `servicio`. |
| `identificador` | `string`, `pattern: '^[0-9A-Za-z._/-]{1,64}$'` | Igual a algún valor del objeto, a cualquier profundidad. |
| `periodo` | `string`, `pattern: '^[A-Za-z0-9._-]{1,20}$'` | |
| `pagina` | `integer`, `minimum: 1`, `default: 1` | |
| `tamano` | `integer`, `minimum: 1`, `maximum: 100`, `default: 50` | |
| `corte` | `string`, `format: date-time` | El `corte` de la primera respuesta; no posterior a ahora. |

### Respuestas

| Estado | Cuerpo | Cuándo |
|---|---|---|
| `200` | `PaginaDeBitacora` | También sin entradas: `entradas: []` y `total: 0`. |
| `400` | `Error` · `COMUN_SOLICITUD_INVALIDA` con `detalles` | Un parámetro que no cumple su regla, `accion` sin `servicio`, `hasta` antes de `desde` o `corte` en el futuro. Un detalle por campo. |
| `401` | `NoAutenticado` (`comun/seguridad.yaml`) | Sin sesión. |
| `403` | `AccesoNoAutorizado` · `SEGURIDAD_ACCESO_NO_AUTORIZADO` | Sin `BITACORA:LECTURA`. El mismo cuerpo para todos los roles. |
| `500`, `503` | `ErrorInterno`, `ServicioNoDisponible` | Como las demás operaciones. |

### Esquemas nuevos

```yaml
PaginaDeBitacora:
  type: object
  additionalProperties: false
  required: [entradas, total, pagina, tamano, corte]
  properties:
    entradas: { type: array, maxItems: 100, items: { $ref: '#/components/schemas/EntradaConsultada' } }
    total: { type: integer, minimum: 0 }
    pagina: { type: integer, minimum: 1 }
    tamano: { type: integer, minimum: 1, maximum: 100 }
    corte: { type: string, format: date-time }

EntradaConsultada:
  description: Una entrada de la bitácora tal como se registró (F17-H01). Sin datos personales de un docente (D-91).
  allOf:
    - $ref: '#/components/schemas/EntradaBitacora'
    - type: object
      required: [id, servicio]
      properties:
        id: { type: string, format: uuid }
        servicio: { type: string, enum: [generacion, academico, docentes, normativa, roles, validacionocs] }
```

`EntradaBitacora` tiene `additionalProperties: false`, así que `EntradaConsultada` se escribe como objeto propio con los mismos campos y reglas más `id` y `servicio`, si el generador no admite el `allOf`; la prueba de contrato comprueba que los campos comunes tienen las mismas reglas.

## `GET /api/usuarios/bitacora/acciones` · `listarAccionesDeBitacora`

`x-mdd-permiso: BITACORA:LECTURA`

| Estado | Cuerpo |
|---|---|
| `200` | `array` de `AccionDeBitacora` (`{ servicio, accion }`, los dos obligatorios), ordenado por servicio y acción |
| `401`, `403`, `500`, `503` | Como la operación anterior |

## Pruebas de contrato (`BitacoraContratoTest`)

- Cada parámetro con un caso válido y uno inválido en sus límites (`usuario` de 0, 1, 64 y 65; `identificador` con tilde, con espacio, con `/` y de 65; `periodo`; `tamano` 0, 1, 100 y 101; `pagina` 0), cada inválido con `COMUN_SOLICITUD_INVALIDA` y su detalle.
- `accion` sin `servicio` y `hasta` antes de `desde`: 400 con el detalle en `accion` y en `hasta`.
- Un token con `BITACORA:LECTURA` (Vicerrector y Administrador): 200. Uno de cada uno de los otros 8 roles de D-16: 403 con el cuerpo común, sin el permiso que falta. Sin token: 401.
- La respuesta cumple `PaginaDeBitacora` y no tiene campos fuera del esquema.
