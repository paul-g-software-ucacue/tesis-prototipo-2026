# Data Model: Calcular el puntaje de afinidad con su desglose

Nada se guarda: el puntaje se calcula en cada consulta (FR-017). Sin migraciones.

Los pesos, los aportes y el total son `BigDecimal`; nunca `double`.

## Dominio de Generación

### `PuntajeAfinidad` (objeto de valor)

| Campo | Regla |
|---|---|
| `asignatura` | Código y nombre. |
| `total` | La suma de los aportes; 0 si nada coincide. |
| `desglose` | Lista de `AporteDeAfinidad`, en este orden: tercer nivel, cuarto nivel, experiencia, cursos. |
| `topeAplicado` | Verdadero si los cursos pasaban del tope. |

### `AporteDeAfinidad` (objeto de valor)

| Campo | Regla |
|---|---|
| `tipo` | `TERCER_NIVEL`, `CUARTO_NIVEL`, `EXPERIENCIA` o `CURSO`. |
| `nombre` | El título, el cargo o el nombre del curso. |
| `coincidencia` | `CAMPO_ESPECIFICO` o `CAMPO_AMPLIO`. |
| `aporte` | Lo que suma; mayor que 0. Un curso que no cabe en el tope no aparece. |

### `CalculadoraAfinidad` (servicio de dominio)

`calcular(FormacionDelDocente, AsignaturaOfertada, PesosAfinidad) → PuntajeAfinidad`. Función pura.

| Elemento | Campo específico igual | Solo campo amplio igual | Si no |
|---|---|---|---|
| Título de cuarto nivel | `cuartoNivelCampoEspecifico` | `cuartoNivelCampoAmplio` | 0 |
| Título de tercer nivel | `tercerNivelCampoEspecifico` | `tercerNivelCampoAmplio` | 0 |
| Experiencia | `experienciaRelacionada` | 0 | 0 |
| Curso | `cursoRelacionado`, hasta `topeCursos` en total | 0 | 0 |

### Lo que recibe (capa anticorrupción)

| Tipo | Campos |
|---|---|
| `FormacionDelDocente` | Tercer nivel (título, campo amplio, campo específico; obligatorio en la ficha), cuartos niveles (título, campo específico), experiencias (cargo, campo específico), cursos (nombre, campo específico). |
| `AsignaturaOfertada` | Código, nombre, campo amplio, campo específico. |
| `PesosAfinidad` | Los 7 valores de Normativa. |

## Consultas internas (en `datos`)

| Dueño | Puerto de entrada nuevo | Devuelve |
|---|---|---|
| Académico | `ConsultarAsignaturasOfertadas` | Las asignaturas con al menos un paralelo `OFERTADA` del periodo, sin repetir. |
| Normativa | `ConsultarPesosDelPeriodo` | La versión asociada al periodo y sus pesos. |
| Docentes | `ConsultarFormacion` | La formación de la ficha, sin datos de contacto ni de contrato. |

## Plataforma (`:plataforma:eventos`)

`ConsultasEntreProcesos.obtener(URI, Class<T>) → Optional<T>`, con `ProcesoConError` y `ProcesoSinRespuesta`. Las dos quedan en `:plataforma:eventos`, y `ErroresEntreProcesos` (`CatalogoDeErrores`) las registra con `COMUN_PROCESO_CON_ERROR` y `COMUN_PROCESO_SIN_RESPUESTA`.

## Configuración del proceso `distributivo`

| Propiedad | Valor |
|---|---|
| `mdd.procesos.datos` | `${MDD_URI_DATOS:http://datos:8080}` |

Usa `mdd.eventos.plazo` y `mdd.eventos.clave`, que ya existen. No hay secretos nuevos.
