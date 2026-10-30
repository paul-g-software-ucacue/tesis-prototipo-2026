# Research: Configurar las 10 cargas en su orden

Fase 0 del plan de F01-H05 (issue #31). Cada sección responde una pregunta del diseño con la decisión, el porqué y lo que se descartó.

## §1 · ¿Quién dice si una carga «tiene datos»?

**Decisión**: el servicio dueño, por el lenguaje publicado de las cargas (ADR-022).
- `TraductorDeCarga` suma `boolean tieneDatos()`, sin implementación por defecto: cada uno de los 10 traductores dice qué cuenta en su carga (§2).
- `AplicadorDeCargas` suma `boolean tieneDatos(String carga)`; `AplicadorPorTraductores` lo reparte al traductor de esa carga.
- Cargas pregunta por `DuenosDeLasCargas`, como ya hace para la vista previa y la confirmación.

**Por qué**: la aclaración (a) dice «su servicio dueño ya tiene datos», sean de archivo o de pantalla. El dueño es el único que sabe qué es un registro vigente (MD-13, principio V), y el aplicador ya es la única puerta de Cargas hacia los dueños.

**Descartado**:
- Leer el historial de Cargas: no ve lo registrado por pantalla (contradice la aclaración a).
- Usar `existentes(lote)`: trae todos los registros (10 568 docentes en la prueba de 5 MB) solo para saber si hay uno.
- Un puerto nuevo en cada dueño: repite lo que el aplicador ya resuelve.

Es una ampliación del lenguaje publicado, no una decisión nueva: ADR-022 no cambia y su texto ya dice que el aplicador es «lo que Cargas ve de un servicio dueño».

## §2 · ¿Qué es un registro vigente en cada carga?

**Decisión**: si la entidad tiene estado, solo cuentan los activos; si no tiene, cuenta cualquiera. La tabla está en [data-model.md](data-model.md). Cada traductor lo resuelve con una consulta de existencia (`select exists (…)`) en su repositorio, para que el estado de las 10 cargas tarde poco (§10).

**Por qué**: el caso borde de la spec («todas las carreras dadas de baja» queda «Pendiente»), y F01-H03 solo acepta referencias a registros activos.

## §3 · ¿Dónde se impone el orden?

**Decisión**: en la capa de aplicación de Cargas, antes de leer el archivo:
- `ValidarArchivoServicio` y `ConfirmarCargaServicio` llaman a `OrdenDeLasCargas.faltantes(carga)`.
- Si falta alguna, lanzan `CargasAnterioresPendientes` con las cargas que faltan.
- Ese rechazo no deja entrada en el historial: no hubo intento de aplicar.

La confirmación hace la comprobación al empezar (FR-005). Además, al confirmar, las filas se vuelven a validar con las referencias del sistema (F01-H03), así que una referencia borrada entre la vista previa y la confirmación ya se rechaza fila por fila. La ventana que queda entre la comprobación y la transacción del dueño es de milisegundos; se acepta y se documenta, sin bloqueos entre servicios.

**Descartado**: comprobar solo en la pantalla (la aclaración b pide también el servidor); bloquear las tablas de los dueños de las cargas anteriores (complejidad entre servicios para una ventana mínima).

## §4 · ¿Cómo llega el estado a la pantalla?

**Decisión**: una operación nueva, `consultarEstadoDeLasCargas` (`GET /api/cargas/estado`), con los mismos permisos que `listarCargas`. El estado se calcula en cada consulta y no se guarda.

**Por qué**:
- `listarCargas` devuelve el diccionario, que no cambia; mezclarle un estado que cambia con cada confirmación complica su uso.
- F02-H02 (#33) construye el tablero del periodo sobre esta misma operación, sumándole el periodo.
- Calcularlo evita que un estado guardado se desactualice cuando alguien registra o da de baja por pantalla.

## §5 · ¿Cómo se confirma «sin proyectos»?

**Decisión**:
- Operación `confirmarSinProyectos` (`POST /api/cargas/sin-proyectos`, cuerpo `{periodo}`), con `CARGAS:ESCRITURA`, como `confirmarCarga` (D-71).
- Un puerto de salida nuevo de Cargas, `ProyectosDelPeriodo.hayAsignables(periodo)`, con su adaptador `ProyectosDesdePuertos`. Llama a `ConsultarProyectos.listar(periodo, null, null)` de Docentes (F03-H05), que ya dice si cada proyecto es asignable en el periodo y lanza `DatoInvalido` si el periodo no existe en Académico.
- La confirmación queda en `cargas.historial_carga` (V3): resultado `SIN_PROYECTOS`, la columna nueva `periodo`, sin archivo ni hash. Un índice único parcial por `(carga, periodo)` impide duplicarla; la segunda confirmación responde 200 con la entrada que ya estaba.
- **Si vale se calcula al leer**: una confirmación vale mientras `hayAsignables(periodo)` sea falso. No hace falta escuchar las confirmaciones de proyectos, y la entrada del historial no se borra (FR-013, principio VII).

**Por qué**: el mismo historial de F01-H04 da quién y cuándo sin otra tabla. Docentes ya resolvió qué es «asignable»; repetirlo en Cargas duplicaría la regla (principio VI).

**Descartado**:
- Una tabla aparte de confirmaciones: duplica el historial.
- Invalidarla al confirmar proyectos: otro camino que se olvida cuando los proyectos entran por pantalla.

**Consecuencia aceptada**: si los proyectos asignables del periodo dejan de serlo (se suspenden o se corrige su vigencia), la confirmación vuelve a valer. Es coherente: el periodo vuelve a no tener proyectos.

## §6 · ¿Cómo se valida el periodo?

**Decisión**: con la regla de `codigo_periodo` del diccionario de la carga 3: texto, hasta 10 caracteres, formato `AAAA-I` o `AAAA-II` (patrón `^[0-9]{4}-(I|II)$`).
- En el contrato, como `pattern` y `maxLength`.
- En la SPA, antes de enviar (principio XIV).
- En el servidor, por el contrato y por `ProyectosDelPeriodo`: un periodo con buen formato que no existe da `CARGAS_PERIODO_NO_ENCONTRADO`.

La pantalla ofrece solo los periodos de los ciclos cargados, en una lista; el patrón cubre a quien llama a la API directamente.

## §7 · RF-57: la fuente de datos

**Punto de partida**: la lectura ya está detrás de un puerto de salida, `LectorDeArchivo`, que entrega una `TablaDeDatos` del dominio. `LectorCsv` y `LectorXlsx` (Apache POI) son sus adaptadores, y ninguna regla conoce el formato.

**Decisión**:
- Renombrar el puerto a `FuenteDeDatos`, el término del glosario («Puerto de fuente de datos», principio III). `LectorPorFormato` queda como el adaptador de archivos y su JavaDoc dice que Moodle o SIGA serían otro adaptador.
- Una regla de ArchUnit en `ArquitecturaTest` de Cargas: ni `dominio` ni `aplicacion` dependen de `org.apache.poi..` ni de los lectores de `adaptadores.salida`.
- Una prueba que alimenta `ValidarArchivoServicio` con una `FuenteDeDatos` de prueba que no lee bytes y compara su informe y su vista previa con los del CSV equivalente.

**Descartado**: cambiar la firma del puerto para que no reciba un nombre y bytes. Un adaptador de Moodle no existe todavía (RF-57 C) y su forma de pedir datos es desconocida; se decide cuando llegue.

## §8 · La pantalla

**Decisión**: en «Plantillas de carga», una columna «Estado» con un chip de MUI:
- «Cargada» o «Pendiente»;
- para la 9, «Sin proyectos» con los periodos.

Además:
- Si faltan cargas anteriores, «Subir archivo» queda deshabilitado y debajo dice «Falta la carga de Carreras». Ese texto va asociado al botón con `aria-describedby`.
- En la fila de la 9, quien tiene `CARGAS:ESCRITURA` ve «Sin proyectos…». Abre un diálogo con la lista de periodos y un botón de confirmar.
- Al confirmar una carga o «sin proyectos», React Query invalida la consulta del estado (FR-008).

## §9 · Pruebas y el cierre de la Ola 1

**Decisión**:
- **Dominio:** `OrdenDeLasCargas` sin dobles, con las dependencias del diccionario real.
- **Aplicación:** Mockito.
- **Traductores:** `tieneDatos()` de cada uno, en las pruebas de integración de su dueño con Testcontainers, con activos e inactivos.
- **Historial V3:** prueba de integración.
- **Contrato y catálogo:** `ValidadorDeContrato`, `VerificadorDeCatalogo` y `VerificadorDePermisos`.
- **Proceso `datos`:** `ConfirmarLasDiezCargasIntegrationTest` suma la 04 antes de la 02 (rechazada) y el estado de las 10 al final (SC-001).
- **SPA:** Vitest para el chip, el botón deshabilitado y el diálogo con periodos válidos e inválidos; Playwright con axe.
- **Punta a punta:** `deploy/probar_orden_cargas.sh`, con la base de prueba:
  - la 04 antes que la 02, rechazada;
  - el estado;
  - «sin proyectos» para un periodo sin proyectos;
  - el rechazo con proyectos asignables;
  - los permisos.

  Va también en `perfil-consolidado`. `probar_confirmar_carga.sh` ya confirma las 10 en orden y queda igual.

## §10 · Rendimiento

**Decisión**: el estado de las 10 cargas son 10 consultas de existencia, más una consulta de proyectos por cada periodo confirmado «sin proyectos». La meta es menos de 2 s en el perfil consolidado (SC-003, con holgura); se mide en la prueba del proceso `datos`.

## §11 · Errores nuevos

| Código | Estado | Cuándo |
|---|---|---|
| `CARGAS_CARGAS_ANTERIORES_PENDIENTES` | 409 | Validar o confirmar una carga con cargas anteriores sin datos. Los detalles nombran cada carga que falta. |
| `CARGAS_PERIODO_NO_ENCONTRADO` | 400 | «Sin proyectos» para un periodo que no está en los ciclos académicos. |
| `CARGAS_PERIODO_CON_PROYECTOS` | 409 | «Sin proyectos» para un periodo con al menos un proyecto asignable. |

Cada uno con sus 8 columnas (D-72) y en «tú» (D-74). El formato inválido del periodo usa `COMUN_SOLICITUD_INVALIDA`, como cualquier campo que no cumple el contrato.

## §12 · Glosario

**Decisión**: cuatro términos nuevos para §7 de `sdd/modelo_dominio.md`, aprobados por el autor el 2026-10-29 antes del código (principio III):

| Término | Definición | Identificador | Contexto | Origen |
|---|---|---|---|---|
| Orden de las cargas | Las dependencias entre las 10 cargas (ERS 8.3): una carga no se puede subir mientras alguna de las que depende esté pendiente. | `OrdenDeLasCargas` | Ingesta | RF-56, ERS 8.3 |
| Estado de la carga | Si una carga tiene datos vigentes en su servicio dueño, entrados por archivo o por pantalla: Pendiente o Cargada; la de proyectos puede estar además «Sin proyectos» en un periodo. El tablero de preparación suma «con errores». | `EstadoDeCarga` | Ingesta | RF-56 |
| Confirmación «sin proyectos» | Declaración explícita de que un periodo no tiene proyectos aprobados asignables. Hace que la carga de proyectos cuente como cargada en ese periodo; queda en el historial de cargas y vale mientras el periodo no tenga proyectos asignables. | `ConfirmarSinProyectos`, `SIN_PROYECTOS` | Ingesta | RF-18, RF-56 |
| Fuente de datos | Puerto por el que llegan los registros de una carga, sin que las reglas sepan su formato ni su origen. Su primer adaptador lee archivos CSV o XLSX; Moodle, SIGA o Talento Humano serían otros. | `FuenteDeDatos` | Ingesta | RF-57 |

**Pendiente**: el 2026-10-29 la carpeta de los artefactos (`~/Downloads/MDD_artefactos_desarrollo/`) ya no estaba en el equipo. Las entradas se pasan al glosario en cuanto el autor indique dónde está, antes de T005, que crea el primer tipo con estos nombres.
