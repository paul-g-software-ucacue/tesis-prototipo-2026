# Contrato: el lenguaje publicado `EntradaBitacora`

Se agrega a `contratos/usuarios/openapi.yaml` como el esquema `EntradaBitacora`: es el lenguaje publicado de Identidad y acceso, y se define en el OpenAPI de su proveedor (`modelo_dominio.md` §4). Es el `contenido` de los eventos de tipo `<servicio>.EntradaBitacora` (research §2).

## Sobre

| Campo | Valor |
|---|---|
| `tipo` | `generacion.EntradaBitacora`, `academico.EntradaBitacora`, `docentes.EntradaBitacora`, `normativa.EntradaBitacora`, `roles.EntradaBitacora` o `validacionocs.EntradaBitacora` |
| `origen` | El servicio del prefijo del tipo. |
| destino | `usuarios` (no viaja en el cuerpo, ADR-018). |
| `idEvento` | Nuevo por cada cambio; es la clave de la entrada. |
| `momento` | El de la publicación. |

## `EntradaBitacora` (contenido)

```yaml
EntradaBitacora:
  description: >-
    Un cambio ya guardado en el servicio de origen, para la bitácora (F17-H01). Nunca lleva
    contraseñas, tokens ni el texto de una justificación.
  type: object
  additionalProperties: false
  required: [usuario, accion, momento, objeto]
  properties:
    usuario:
      type: string
      minLength: 1
      maxLength: 64
      description: Quién inició el cambio, o «sistema». Sin otro formato, como el nombre de usuario de Usuarios.
    accion:
      type: string
      pattern: '^[A-Z][A-Z_]{2,39}$'
      example: ACTIVIDAD_GUARDADA
    momento:
      type: string
      format: date-time
      description: Cuándo se guardó el cambio en su servicio (ISO 8601, UTC).
    periodo:
      type: string
      pattern: '^[A-Za-z0-9._-]{1,20}$'
      description: El periodo al que pertenece el cambio; falta si no pertenece a uno.
    objeto:
      type: object
      additionalProperties: true
      description: Sobre qué se hizo, con la forma del registro de cambios del servicio.
    anterior:
      type: object
      additionalProperties: true
      description: Los valores antes del cambio; falta al crear.
    nuevo:
      type: object
      additionalProperties: true
      description: Los valores después del cambio; falta al quitar.
```

Regla que el esquema no expresa y valida el receptor: `anterior` y `nuevo` no faltan los dos.

## Ejemplo: el escenario del issue

Una actividad de 4 horas que el coordinador cambia a 6:

```json
{
  "idEvento": "6f1c2d4e-8a9b-4c3d-9e1f-0a2b3c4d5e6f",
  "tipo": "generacion.EntradaBitacora",
  "origen": "generacion",
  "momento": "2026-11-06T15:00:01Z",
  "contenido": {
    "usuario": "coordinacion.demo",
    "accion": "ACTIVIDAD_GUARDADA",
    "momento": "2026-11-06T15:00:00Z",
    "periodo": "2026-I",
    "objeto": {
      "periodo": "2026-I",
      "carrera": "TDS",
      "docente": "0100000001",
      "actividad": "TUTORIAS",
      "horas": "6",
      "horasAnteriores": "4"
    },
    "anterior": { "horas": "4" },
    "nuevo": { "horas": "6" }
  }
}
```

`objeto` repite lo que ya guarda el registro local; `anterior` y `nuevo` dicen solo lo que cambió. Los valores de este ejemplo son ficticios.

## Datos personales de un docente (FR-014, D-91)

Una entrada de `docentes` nombra en `objeto.camposPersonales` los campos personales que cambiaron, sin sus valores:

```json
"objeto": { "identificacion": "0100000001", "camposPersonales": ["telefono", "condicionEspecial"] },
"anterior": { "dedicacion": "MEDIO_TIEMPO" },
"nuevo": { "dedicacion": "TIEMPO_COMPLETO" }
```
