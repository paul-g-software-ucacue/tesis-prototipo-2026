# Data Model: Crear una versión nueva de la normativa (F04-H02)

Esquema `normativa` del proceso `datos`, sobre lo que dejó F04-H01. Los detalles y las alternativas están en [research.md](research.md).

## Dominio (modelo de dominio §5.1)

| Elemento | Tipo | Cambia en esta historia |
|---|---|---|
| `VersionNormativa` | Agregado | Gana `copiadaDe` (opcional) y `asignaciones`. Operaciones: `copiaDe(origen, numero, datos)`, que registra `VersionNormativaCreada`; `editada(cambios)`, que devuelve la versión y sus `CambioDeValor`; `asociadaA(periodo)`; `enUso()`. |
| `AsignacionNormativa` | Entidad del agregado | **Nueva.** `periodo` (código) y `asociadaEn`. |
| `DatosDeVersion` | Objeto de valor | **Nuevo.** `codigo`, `origen` y `vigenteDesde`: la cabecera que se escribe. |
| `CambioDeValor` | Objeto de valor | **Nuevo.** `campo`, `anterior` y `nuevo`, en texto, para el registro. |
| `VersionNormativaCreada` | Evento de dominio | **Nuevo.** `numero`, `codigo`, `copiadaDe` y `momento`. |
| `VersionEnUso`, `CambioNoPermitido`, `FilaInvalida` | Excepciones de dominio | **Nuevas.** `FilaInvalida` hereda de `IllegalArgumentException` y lleva perfil, tipo y motivo. |

Invariantes nuevas:
- Una versión con al menos una asignación no se edita.
- `editada` exige exactamente las mismas filas (pares perfil y tipo) y los mismos parámetros por regla.
- Los demás datos siguen las invariantes de F04-H01, que se comprueban al construir la versión editada.

## Tablas (`V3__versiones_y_periodos.sql`)

| Tabla | Cambio | Reglas |
|---|---|---|
| `version_normativa` | Gana `copiada_de` int nulo, FK a `version_normativa(numero)` | La versión 1 queda con `null` |
| `asignacion_normativa` | **Nueva:** `version_id` FK, `periodo` varchar(20) **único**, `asociada_en` timestamptz, `asociada_por` varchar(64) | Un periodo tiene una sola versión; una versión puede tener varias |
| `registro_cambio` | El `check` de `accion` gana `VERSION_EDITADA` y `VERSION_ASOCIADA` | El detalle de `VERSION_EDITADA` es la lista de cambios; el de `VERSION_ASOCIADA`, el periodo |

## Roles (D-67)

`V5__vicerrector_escribe_normativa.sql` agrega a `VICERRECTOR` el permiso `PARAMETROS:ESCRITURA`, con alcance `TODO`. Su fila de la matriz queda así:
- **Lectura:** `DISTRIBUTIVO`, `REPORTES`, `BITACORA` y `PARAMETROS`.
- **Escritura:** `DISTRIBUTIVO` y `PARAMETROS`.
- **Aprobación y firma:** `DISTRIBUTIVO`.
