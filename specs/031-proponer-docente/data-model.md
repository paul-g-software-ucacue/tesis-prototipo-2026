# Data Model: Proponer un docente por asignatura-paralelo

Las horas son `BigDecimal`, comparadas con `compareTo` (D-77).

## Esquema `generacion` (`V3__distributivo.sql`)

### `distributivo`

| Columna | Regla |
|---|---|
| `id` | `uuid`, llave primaria. |
| `periodo` | `varchar(20)`, único: uno por periodo. Sin llave foránea (principio V). |
| `creado_en`, `creado_por` | Quién y cuándo. |

### `version_distributivo`

| Columna | Regla |
|---|---|
| `id` | `uuid`, llave primaria. |
| `distributivo_id` | Referencia a `distributivo`. |
| `numero` | Único por distributivo; esta historia solo crea el 1. |
| `estado` | `EDITABLE`, `CONGELADA` o `ANULADA`. |

### `parcial_carrera`

| Columna | Regla |
|---|---|
| `version_id` | Referencia a `version_distributivo`. |
| `carrera` | `varchar(20)`. Llave primaria con `version_id`. |
| `estado` | `EN_ELABORACION`, `ENVIADO` o `DEVUELTO`. |

### `propuesta`

| Columna | Regla |
|---|---|
| `version_id`, `asignatura`, `paralelo` | Llave primaria: una por asignatura-paralelo. |
| `carrera` | Con `version_id`, referencia a `parcial_carrera`. |
| `docente` | `varchar(13)`, la identificación. |
| `nombre_docente` | Copia, `varchar(200)`. |
| `justificacion` | `AFINIDAD`, `DISPONIBILIDAD` o `EXCEPCION`. |
| `texto` | `varchar(500)`; solo con `EXCEPCION`, por restricción. |
| `propuesta_en`, `propuesta_por` | Quién y cuándo. |

### `registro_cambio`

Como en los demás servicios. Acciones: `DISTRIBUTIVO_CREADO`, `PROPUESTA_GUARDADA`, `PROPUESTA_QUITADA`. El detalle lleva el periodo, el paralelo, los docentes y la justificación, sin el texto.

## Dominio de Generación

### `Justificacion` (objeto de valor)

`tipo` y `texto`. `Justificacion.para(puntaje, minimo, troncoComun, texto)`. Los tres casos están en research §5.

### `Propuesta` (entidad)

`asignatura`, `paralelo`, `carrera`, `docente`, `nombreDocente`, `justificacion`, `propuestaEn`, `propuestaPor`.

### `VersionDistributivo` (agregado)

`numero`, `estado`, los parciales y las propuestas. `proponer(propuesta)` exige la versión editable y el parcial en elaboración, y crea el parcial si la carrera no lo tenía. `quitar(asignatura, paralelo)`.

### `ParcialCarrera` (entidad)

`carrera`, `estado`. `avance(oferta, propuestas)`: ofertadas, con propuesta y si está completo.

### `HorasDelPeriodo` (servicio de dominio)

`calcular(propuestas, oferta, paralelo)`: un mapa de identificación a `HorasDelDocente`. Función pura; las reglas están en research §4.

### Cambia

- **`Sugerencia`** suma `bajoElMinimo`, una lista de `Candidato`, y `fueraPorElMinimo` pasa a ser su tamaño.
- **`AsignaturaParalelo`** suma `periodoCerrado`.
- **`Candidato`** sin cambios.

## `:plataforma:errores-nucleo`

- **`AlcanceDeCarrera.propia(autoridades, permiso)`:** la misma regla, con un solo permiso. Vacío: sobre todo. Con valor: solo esa carrera. Lanza `CarreraSinIndicar` si es solo «lo propio» y no tiene carrera.

## Puertos de Generación

- **Entrada:** `CrearDistributivo`, `ConsultarDistributivo`, `ProponerDocente`, `ConsultarParciales`.
- **Salida:**
  - `RepositorioDistributivos`;
  - `RegistroDeCambios`;
  - `Transaccion`;
  - `DatosParaSugerencia` suma `ofertaDelPeriodo` y `periodoListo`.
- **`HorasAsignadas`** desaparece.

## Transiciones

Periodo sin distributivo → con distributivo, versión 1 `EDITABLE`, parciales `EN_ELABORACION`.

Asignatura-paralelo sin propuesta → con propuesta → reemplazada → sin propuesta.
