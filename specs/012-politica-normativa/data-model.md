# Data Model: La política 2024 como primera versión de normativa (F04-H01)

Esquema `normativa` del proceso `datos`. Los detalles y las alternativas están en [research.md](research.md).

## Dominio (modelo de dominio §5.1)

| Elemento | Tipo | Contenido e invariantes |
|---|---|---|
| `VersionNormativa` | Agregado | `numero` (entero ≥ 1), `codigo`, `origen`, `vigenteDesde` (fecha), `tipoIst`, `tabla` y `reglas`. En esta historia solo se reconstruye desde la base, con sus invariantes: crear una versión, y su evento `VersionNormativaCreada`, llega con F04-H02. |
| `TablaCargaHoraria` | Objeto de valor | Las filas. Un par perfil y tipo de IST no se repite; un perfil aplica a `TODOS` o a tipos concretos, no a los dos. `filaPara(perfil, tipoIst)` da la fila del tipo o, si no hay, la de `TODOS`. |
| `FilaCargaHoraria` | Objeto de valor | `aplicaA` (`TODOS` o T1 a T5), `perfil`, `dedicacionTotal`, `horasClase`, `horasClaseMinimo`, `horasClaseMaximo`, `horasOtrasDocencia`, `horasGestion` y `horasInvestigacionVinculacionMaximo`, las tres últimas opcionales; además `severidad`, `referencia` y `observacion`, esta última opcional. Las horas son enteras y no negativas; mínimo ≤ clase ≤ máximo; la suma de clase, otras de docencia, gestión y el máximo de investigación y vinculación no supera la dedicación total. |
| `Regla` | Objeto de valor | `identificador` (`RN-01` a `RN-26`), `severidad` (opcional: RN-01 y RN-06 son solo parámetros) y `parametros` (nombre → valor decimal, al menos uno). |
| `TipoIst` | Enumeración | T1 a T5. |
| `Severidad` | Enumeración | `DURA` (impide guardar o avanzar) y `BLANDA` (exige justificación). |

## Tablas

| Tabla | Campos | Reglas |
|---|---|---|
| `version_normativa` | `id` uuid PK, `numero` int único, `codigo` varchar(40) único, `origen` varchar(120), `vigente_desde` date, `tipo_ist` varchar(2), `creada_en` timestamptz | `tipo_ist` ∈ `T1`…`T5`; `numero` ≥ 1 |
| `fila_carga_horaria` | `version_id` FK, `orden` int, `aplica_a` varchar(5), `perfil` varchar(60), `dedicacion_total` int, `horas_clase` int, `horas_clase_minimo` int, `horas_clase_maximo` int, `horas_otras_docencia` int nulo, `horas_gestion` int nulo, `horas_inv_vinc_maximo` int nulo, `severidad` varchar(6), `referencia` varchar(120), `observacion` varchar(300) nulo; PK (`version_id`, `aplica_a`, `perfil`) | `aplica_a` ∈ `TODOS`, `T1`…`T5`; `severidad` ∈ `DURA`, `BLANDA`; horas ≥ 0; `orden` único por versión: la posición en la política |
| `regla` | `version_id` FK, `identificador` varchar(6), `severidad` varchar(6) nulo; PK (`version_id`, `identificador`) | `identificador` con la forma `RN-NN` |
| `parametro_regla` | `version_id`, `regla` varchar(6), `nombre` varchar(60), `valor` numeric(10,2); PK (`version_id`, `regla`, `nombre`); FK (`version_id`, `regla`) → `regla` | |
| `registro_cambio` | `id` uuid PK, `momento` timestamptz, `usuario` varchar(64), `accion` varchar(20), `detalle` jsonb | `accion` ∈ `VERSION_CREADA` (F04-H02 agrega las suyas) |

Todas las crea `V2__politica_2024.sql`, que además inserta la versión 1.

## Versión 1

| Campo | Valor |
|---|---|
| `numero` | 1 |
| `codigo` | `SENESCYT-2024-03` |
| `origen` | Política SENESCYT 2024 |
| `vigente_desde` | 2024-03-11 |
| `tipo_ist` | `T2` |
| Filas | Las 22 de `07_politica_carga_horaria.csv`: 7 para `TODOS` y 3 por cada tipo de IST (coordinador de carrera o sede, gestor, y rector o vicerrector) |
| Reglas | Las 7 de research §4: RN-01, RN-02, RN-05, RN-06, RN-09, RN-10 y RN-13, con 29 parámetros |
| Registro | `VERSION_CREADA` por `sistema`, con el número, el código y el origen |

Los perfiles de la carga 7 son `DOCENTE_TC`, `DOCENTE_MT`, `COORDINADOR_CARRERA_SEDE`, `GESTOR`, `COORDINADOR_ACADEMICO_INSTITUCIONAL`, `RECTOR_VICERRECTOR`, `SECRETARIA_GENERAL_ACADEMICA`, `DOCENTE_PROYECTO_NACIONAL_SIES`, `DOCENTE_INVESTIGACION_VINCULACION` y `CUIDADOR_SUSTITUTO`.

## Roles (D-66)

`V4__vicerrector_lee_normativa.sql`, en el esquema `roles`, agrega a `VICERRECTOR` el permiso `PARAMETROS:LECTURA` con alcance `TODO`. Su fila de la matriz pasa a ser: lectura de `DISTRIBUTIVO`, `REPORTES`, `BITACORA` y `PARAMETROS`; escritura, aprobación y firma de `DISTRIBUTIVO`.
