# Research: Ver el tablero de preparación del periodo

Fase 0 del plan de F02-H02 (issue #33). Cada sección responde una pregunta del diseño con la decisión, el porqué y lo que se descartó.

## §1 · ¿Quién dice si una carga del periodo tiene datos de ese periodo?

**Decisión**: el servicio dueño, ampliando el lenguaje publicado de F01-H05 (ADR-022):
- `TraductorDeCarga` suma `default boolean tieneDatosEn(String periodo)`, que por defecto es `tieneDatos()`: la carga no depende del periodo.
- Lo reemplazan `TraductorOferta` (una asignatura-paralelo `OFERTADA` del periodo) y `TraductorMatriculas` (un registro del periodo), con un `select exists … where periodo = :periodo` en su repositorio.
- `AplicadorDeCargas` suma `tieneDatosEn(carga, periodo)`, que `AplicadorPorTraductores` reparte.
- **La carga de proyectos** no usa este método: «tiene datos en el periodo» es «tiene al menos un proyecto asignable en él», y Cargas ya lo pregunta por `ProyectosDelPeriodo` (F01-H05), con la regla de Docentes (F03-H05).

**Por qué**: solo el dueño sabe si sus datos tienen periodo (MD-13), y el método por defecto evita tocar los 7 traductores cuyas cargas no dependen del periodo (aclaración c).

**Descartado**: marcar en el diccionario qué cargas son del periodo. El diccionario describe columnas, no la naturaleza de la carga, y la decisión de cómo contar sigue siendo del dueño.

## §2 · ¿Cómo sabe Cargas si el periodo está abierto o cerrado?

**Decisión**: por la referencia de periodos que Académico ya publica a los otros servicios (`ConsultarReferencias` → `PeriodoDeReferencia`, F03-H05). `PeriodoDeReferencia` suma dos datos:
- `abierto`: tiene su versión de normativa (F02-H01);
- `cerrado`: su estado es Cerrado.

`ConsultarReferenciasServicio` recibe `NormativaDelPeriodo`, como ya lo hace `ConsultarPeriodosServicio`, para saber la versión de cada periodo. Cargas lo lee por `ReferenciasDelSistema`, que ya llama a `ConsultarReferencias`.

**Por qué**: `ConsultarPeriodos` devuelve el dominio de Académico (`PeriodoAcademico`), que Cargas no puede ver (principio V). `PeriodoDeReferencia` ya es el tipo publicado para eso.

**Descartado**: un puerto de entrada nuevo en Académico solo para el tablero, que duplicaría `PeriodoDeReferencia`.

**Consecuencia**: los pocos lugares que construyen `PeriodoDeReferencia` (Académico, Docentes y las pruebas de Cargas) pasan los dos datos nuevos. Docentes los ignora.

## §3 · ¿Cuándo está una carga «con errores»?

**Decisión** (aclaración b): sin datos en el periodo, y con la entrada más reciente de su historial, sin contar «sin proyectos», en `RECHAZADA`. El motivo sale de esa entrada, con el código del catálogo (`CARGAS_CARGA_CON_ERRORES`, `CARGAS_VISTA_PREVIA_DESACTUALIZADA` o `CARGAS_CARGA_RECHAZADA`). Se lee con `HistorialDeCargas.deLaCarga`, que ya devuelve las entradas de la más reciente a la más antigua: el puerto no cambia.

## §4 · El dominio del tablero

**Decisión**: `PreparacionDelPeriodo`, del glosario («Tablero de preparación»), en `cargas.dominio`. Lo arma una función pura con:
- el diccionario;
- si cada carga tiene datos en el periodo;
- el último rechazo de cada carga;
- si el periodo está confirmado «sin proyectos»;
- si está abierto o cerrado.

Calcula el estado de cada carga, lo que falta (`LoQueFalta`: una carga obligatoria, el periodo sin abrir o el periodo cerrado) y si está listo. La obligatoriedad sale del diccionario (principio VI): `OBLIGATORIA` y `OBLIGATORIA_SALVO_SIN_PROYECTOS` bloquean, y `RECOMENDADA` no.

**Por qué**: es la misma forma de `OrdenDeLasCargas`: dominio sin dependencias, probado con el diccionario real y sin dobles.

## §5 · La operación

**Decisión**: una sola operación, `consultarPreparacionDelPeriodo` (`GET /api/cargas/preparacion?periodo=…`), con `[CARGAS:LECTURA, DISTRIBUTIVO:LECTURA]` (D-70).
- Responde la lista de periodos para el selector (código y nombre, del que empieza más tarde al más antiguo) y el tablero del periodo pedido.
- Sin `periodo`, usa el que empieza más tarde.
- Sin periodos, responde la lista vacía y el tablero sin cargas, «no listo», con lo que falta: cargar los ciclos académicos.
- Un periodo con buen formato que no existe: 400 `CARGAS_PERIODO_NO_ENCONTRADO` (el código de F01-H05). Uno mal escrito: 400 `COMUN_SOLICITUD_INVALIDA`, por el patrón del contrato (principio XIV).

**Por qué la lista va en la misma respuesta**: quien solo lee las cargas (`CARGAS:LECTURA`, como Talento Humano) no puede leer los periodos de Académico (`DISTRIBUTIVO:LECTURA`, D-69). Cargas ya los tiene por `ReferenciasDelSistema`, sin datos personales.

**Descartado**: ampliar `consultarEstadoDeLasCargas` con un periodo. Su respuesta y su uso (la columna de «Plantillas de carga») siguen sin periodo, y mezclar las dos complica a los dos.

## §6 · «Crear distributivo»

**Decisión** (aclaración a):
- La pantalla muestra el botón solo con `DISTRIBUTIVO:ESCRITURA`.
- Lo habilita con `listo` de la respuesta.
- Deshabilitado, su `aria-describedby` apunta a la lista de lo que falta.
- Al pulsarlo, un aviso (`role="status"`) dice que el periodo está listo y que la construcción del distributivo llega con F06 (Ola 2).

No hay operación en el servidor: F06 la creará en el servicio del distributivo.

## §7 · La pantalla

**Decisión**: `frontend/src/pantallas/preparacion/`:
- `Preparacion.tsx`, con la ruta `/preparacion` y la entrada del menú «Preparación del periodo», con los permisos `[DISTRIBUTIVO:LECTURA, CARGAS:LECTURA]`, como «Plantillas de carga»;
- un `Select` de MUI con los periodos;
- la tabla de las 10 cargas (número, carga, obligatoria, estado con un `Chip`, y el motivo si está «con errores»);
- la lista de lo que falta, o un aviso de «listo»;
- el botón.

El periodo elegido se valida con el mismo patrón antes de pedirlo (principio XIV), aunque solo pueda venir del selector.

## §8 · Pruebas

- **Dominio:** `PreparacionDelPeriodoTest` sin dobles, con el diccionario real.
- **Aplicación:** `ConsultarPreparacionServicioTest` con Mockito.
- **Dueños:** `tieneDatosEn` de la oferta y los matriculados con Testcontainers (del periodo y de otro); `PeriodoDeReferencia` con `abierto` y `cerrado` en `ConsultarReferenciasServicioTest`.
- **Contrato y permisos:** `PreparacionContratoTest` y `PermisosCargasTest`.
- **Proceso `datos`:** `ConfirmarLasDiezCargasIntegrationTest` suma el tablero de 2026-I:
  - con docentes pendiente, no está listo;
  - abierto y con todo, listo;
  - 2026-II sin oferta propia, la oferta pendiente.
- **SPA:** Vitest, y Playwright con axe.
- **Punta a punta:** `deploy/probar_preparacion.sh`, con la base de prueba y el periodo abierto con la política de prueba. Va también en `perfil-consolidado`.

## §9 · Rendimiento

El tablero son 9 consultas de existencia, una de proyectos asignables, 10 lecturas del historial y la referencia de periodos. La meta es menos de 3 s (SC-003, con holgura); se mide en la prueba del proceso `datos`.

## §10 · Errores y glosario

- **Sin códigos nuevos:** el periodo que no existe usa `CARGAS_PERIODO_NO_ENCONTRADO`; su columna «causa» suma el tablero.
- **Sin términos nuevos:**
  - «Tablero de preparación» (`PreparacionDelPeriodo`), «Estado de la carga» (con «con errores») y «Periodo académico» (abierto, cerrado) ya están en el glosario §7;
  - `LoQueFalta` no es un término del negocio, sino la lista de «qué falta» de la definición del tablero.
