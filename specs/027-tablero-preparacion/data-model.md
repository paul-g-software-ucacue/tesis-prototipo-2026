# Data Model: Ver el tablero de preparación del periodo

Nada se guarda: el tablero se calcula en cada consulta.

## Preparación del periodo (`PreparacionDelPeriodo`, dominio de Cargas)

| Campo | Tipo | Regla |
|---|---|---|
| `periodo` | texto, `^[0-9]{4}-(I\|II)$` | El periodo del tablero. |
| `abierto` | sí o no | Tiene su versión de normativa (F02-H01). |
| `cerrado` | sí o no | Su estado es Cerrado. |
| `cargas` | 10 `CargaDelPeriodo`, en su orden | Ver abajo. |
| `faltan` | lista de `LoQueFalta` | Vacía si está listo. |
| `listo` | sí o no | Verdadero si `faltan` está vacía. |

## Carga del periodo (`CargaDelPeriodo`)

| Campo | Regla |
|---|---|
| `numero`, `codigo`, `nombre` | Del diccionario. |
| `obligatoriedad` | Del diccionario: `OBLIGATORIA`, `OBLIGATORIA_SALVO_SIN_PROYECTOS` o `RECOMENDADA`. |
| `estado` | Uno de cuatro, en este orden de prioridad: (1) `CARGADA` si tiene datos en el periodo (§ abajo); (2) `SIN_PROYECTOS` si es la de proyectos y el periodo está confirmado «sin proyectos»; (3) `CON_ERRORES` si su último intento de confirmar fue rechazado; (4) si no, `PENDIENTE`. |
| `motivo` | Solo en `CON_ERRORES`: el código del catálogo del rechazo. |

**«Tiene datos en el periodo»:**

| Nº | Carga | Cuenta si… |
|---|---|---|
| 5 | Oferta del periodo | hay al menos una asignatura-paralelo `OFERTADA` de ese periodo (Académico, `TraductorOferta.tieneDatosEn`) |
| 9 | Proyectos y prácticas | hay al menos un proyecto asignable en el periodo (Docentes, `ProyectosDelPeriodo.hayAsignables`, F03-H05) |
| 10 | Matriculados por carrera | hay al menos un registro de ese periodo (Académico, `TraductorMatriculas.tieneDatosEn`) |
| las demás | — | como en F01-H05: `tieneDatos()` de su dueño, en todo el sistema |

## Lo que falta (`LoQueFalta`)

| Tipo | Cuándo | Lleva |
|---|---|---|
| `CARGA` | Una carga `OBLIGATORIA` o `OBLIGATORIA_SALVO_SIN_PROYECTOS` que no está `CARGADA` ni `SIN_PROYECTOS` | su número, código, nombre y estado |
| `PERIODO_SIN_ABRIR` | El periodo no tiene su versión de normativa | — |
| `PERIODO_CERRADO` | El periodo está cerrado | — |
| `SIN_PERIODOS` | No hay ningún periodo en los ciclos académicos | — |

Las cargas van en el orden de la ERS, después el periodo.

## Cambios en lo que ya existe

| Dónde | Cambio |
|---|---|
| `TraductorDeCarga<T>` (`:plataforma:lenguaje-cargas`) | `default boolean tieneDatosEn(String periodo) { return tieneDatos(); }` |
| `AplicadorDeCargas`, `AplicadorPorTraductores` | `boolean tieneDatosEn(String carga, String periodo)` |
| `RepositorioOferta`, `RepositorioMatriculas` (Académico) | `hayOfertadasEn(periodo)`, `hayMatriculasEn(periodo)`, con su `select exists` |
| `PeriodoDeReferencia` (Académico, puerto de entrada) | Suma `boolean abierto, boolean cerrado` |
| `ConsultarReferenciasServicio` (Académico) | Recibe `NormativaDelPeriodo` para `abierto` |
| `ReferenciasDelSistema` (Cargas) | Suma `periodos()`: código, nombre, inicio, abierto y cerrado de cada periodo |
