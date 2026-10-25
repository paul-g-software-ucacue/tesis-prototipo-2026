# Contrato: las reglas de las filas y el informe (F01-H03)

`contratos/cargas/openapi.yaml` cambia antes del código (principio IV). El porqué está en [research.md](../research.md) §1, §2 y §9.

## `validarArchivo` (amplía la de F01-H02)

`POST /api/cargas/plantillas/{codigo}/archivo`, `x-mdd-permiso: CARGAS:ESCRITURA`.

- **200** `LecturaDeArchivo`, también cuando las filas tienen errores (principio XIII). Mantiene `carga`, `archivo`, `formato` y `registros`, y agrega:

  | Campo | Tipo |
  |---|---|
  | `valido` | `boolean`: sin errores |
  | `errores` | `integer`, el total |
  | `advertencias` | `integer`, el total |
  | `problemas` | `ProblemaDeFila[]`, los primeros 100 en orden de fila |

- **400**, **404** y **413**: como en F01-H02, para los errores de estructura.

## `descargarInforme` (nueva)

`POST /api/cargas/plantillas/{codigo}/archivo/informe`, `x-mdd-permiso: CARGAS:ESCRITURA`, con el mismo `multipart/form-data`.

- **200** `text/csv; charset=UTF-8`, con `Content-Disposition: attachment; filename="informe-<codigo>.csv"`:
  - columnas `fila,campo,valor,motivo,tipo`, con todos los problemas;
  - cada celda que empieza con `=`, `+`, `-`, `@`, tabulación o retorno de carro, neutralizada con un apóstrofo delante (RNF-SEG-04).
- **400**, **404** y **413**: como `validarArchivo`.

## Esquemas

| Esquema | Campos |
|---|---|
| `TipoDeProblema` | `ERROR`, `ADVERTENCIA` |
| `ProblemaDeFila` | `fila` (entero ≥ 2), `campo` (el encabezado), `valor` (el texto recibido, puede ir vacío), `motivo` (en español), `tipo` y `sugerencia` (opcional) |

## Errores

No hay códigos nuevos: un archivo con errores en sus filas no es un error HTTP (principio XIII). Los de estructura siguen con sus códigos `CARGAS_*`.

## Docentes

Sin operación nueva en el contrato: `ConsultarIdentificaciones` es un puerto publicado dentro del proceso `datos`, como `ConsultarReferencias` de Académico.
