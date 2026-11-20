# Data Model: Seguir los plazos de cada etapa

F12-H03 (#58). Nombres del glosario (`sdd/modelo_dominio.md` §7): `Plazo`, `Etapa`, `PlazoDeEtapaVencido`, `ConfiguracionFlujo` y `ProcesoAprobacion`.

## Dominio de Validación OCS (Java puro)

### `Plazo` (objeto de valor, nuevo)

La fecha límite de la etapa actual (research §2).

| Campo | Tipo | Regla |
|---|---|---|
| `etapa` | `TipoDeEtapa` | La etapa actual; nunca PUBLICADO |
| `llegada` | `Instant` | Cuándo llegó el distributivo a la etapa |
| `dias` | `int` | ≥ 1; los días corridos de la etapa en la configuración de entonces |
| `fechaLimite` | `LocalDate` | La fecha de `llegada` en `America/Guayaquil` más `dias` |

- `Plazo.de(etapa, llegada, dias)` calcula la fecha límite y rechaza `dias < 1`.
- `en(LocalDate hoy)` devuelve `EstadoDelPlazo` (`A_TIEMPO`, `VENCE_HOY` o `VENCIDO`) y los días que faltan, `fechaLimite − hoy`, que son negativos si venció.
- `diasDeAtraso(hoy)` es `hoy − fechaLimite` si venció, y 0 si no.

### `PlazosDeEtapas` (objeto de valor, nuevo)

Los días de cada etapa activa, salvo PUBLICADO, según la configuración que regía al crear el distributivo. Lo arma `EtapasAlCrearse.plazos` (research §1).
- `de(TipoDeEtapa)` devuelve un `OptionalInt`: vacío para PUBLICADO y para una etapa inactiva.
- `sumaHasta(APROBADO)` es la suma de las etapas activas sin PUBLICADO, para RN-23.

### `ProcesoAprobacion` (agregado, existente)

Suma operaciones de consulta, sin estado nuevo y sin transiciones nuevas:
- **`llegadaALaEtapa()`:** el `momento` de la última transición con `desde ≠ hacia` y `hacia == estado()`; si no hay, `distributivoCreadoEn()`.
- **`plazoActual(PlazosDeEtapas)`:** un `Optional<Plazo>`, vacío en PUBLICADO.
- **`destinosDelVencimiento()`:** los destinos del aviso (research §4):
  - en BORRADOR_CARRERA, un `DestinoDeNotificacion(rol de la etapa, carrera)` por cada parcial `PENDIENTE` o `DEVUELTO`; en las demás etapas, `DestinoDeNotificacion(rol de la etapa, sin carrera)`;
  - más el rol de EN_REVISION_VRA de la copia, sin carrera;
  - sin repetidos.

Lo que no cambia: el plazo no bloquea ninguna operación (FR-003). Las validaciones de `avanzar`, `enviarARevision`, `devolver`, `aprobarContenido` y demás no lo consultan.

### `VencimientoDePlazo` (objeto de valor, nuevo; el `PlazoDeEtapaVencido` del glosario)

| Campo | Tipo | Regla |
|---|---|---|
| `periodo` | `String` | El periodo del proceso |
| `plazo` | `Plazo` | La etapa, la llegada, los días y la fecha límite |
| `detectadoEn` | `Instant` | Cuándo lo detectó la revisión |
| `diasDeAtraso` | `int` | ≥ 1 |

`ProcesoAprobacion.vencimiento(PlazosDeEtapas, Instant ahora)` devuelve un `Optional<VencimientoDePlazo>`: presente solo si el plazo actual está `VENCIDO` en la fecha de `ahora` en Ecuador.

### `MensajesDePlazo` (nuevo, como `MensajesDeTransicion`)

`de(proceso, vencimiento)` devuelve un `MensajeDeTransicion` por destino, con el título, el detalle y el enlace de research §4. Los textos no llevan datos personales: el periodo, la etapa, las fechas y los días.

### `AdvertenciaDeRemision` (objeto de valor, nuevo)

La advertencia de RN-23 al cargar una configuración (research §7).

| Campo | Tipo | Regla |
|---|---|---|
| `codigo` | `CodigoDeAdvertencia` | `RN23_MARGEN_INSUFICIENTE` o `RN23_SIN_NORMATIVA` |
| `ultimoPlazo` | `LocalDate` | La fecha de la carga en Ecuador más `PlazosDeEtapas.sumaHasta(APROBADO)` |
| `inicioDelPeriodo` | `LocalDate` | El de Académico |
| `diasAntesDelInicio` | `OptionalInt` | Los de la normativa del periodo; vacío con `RN23_SIN_NORMATIVA` |
| `fechaDeRemision` | `Optional<LocalDate>` | `inicioDelPeriodo − diasAntesDelInicio`; vacío con `RN23_SIN_NORMATIVA` |
| `diasDeMargen` | `OptionalInt` | `fechaDeRemision − ultimoPlazo`, negativo con `RN23_MARGEN_INSUFICIENTE` |

`AdvertenciaDeRemision.de(hoy, configuracion, inicio, dias)` devuelve un `Optional`:
- vacío si `ultimoPlazo ≤ fechaDeRemision`;
- `RN23_MARGEN_INSUFICIENTE` si `ultimoPlazo > fechaDeRemision`;
- `RN23_SIN_NORMATIVA` si `dias` está vacío.

Con un inicio que ya pasó o que queda a menos de 30 días, el último plazo siempre queda después, así que también advierte (escenario 6 de US3).

### `EstadoDelPeriodo` (existente)

Suma `Optional<LocalDate> inicio`, que viene de Académico. Es opcional para tolerar una versión anterior del proceso `datos` durante un despliegue (D-55). Sin inicio, la carga no calcula la advertencia y lo registra con un `WARN`.

## Puertos

| Puerto | Cambio |
|---|---|
| `RevisarPlazos` (entrada, nuevo) | `revisar()`: revisa todos los procesos en curso y devuelve cuántos vencimientos nuevos registró |
| `FlujoDelDistributivo` (entrada, existente) | Suma `Optional<PlazoDeLaEtapa> plazo` y `List<EtapaDelDistributivo> etapas` (el nombre `EtapaDelFlujo` ya es de la configuración) |
| `ConfiguracionVigente` (entrada, existente) | No cambia: la advertencia viaja en su `CargaGuardada` (`advertencia`), así que la de por defecto nunca la tiene |
| `RepositorioProceso` (salida, existente) | Suma `List<String> enCurso()`: los periodos con un proceso que no está en PUBLICADO |
| `RepositorioPlazos` (salida, nuevo) | `registrar(VencimientoDePlazo)` devuelve `boolean`: `true` si lo insertó, `false` si ya estaba |
| `RepositorioConfiguracion` (salida, existente) | `guardar` recibe la advertencia (la versión sin ella queda como método por defecto); `CargaGuardada` la devuelve |
| `DatosDelPeriodo` (salida, existente) | `estado` trae el inicio |
| `NormativaDelPeriodo` (salida, nuevo) | `OptionalInt diasDeRemision(periodo)`, vacío sin versión. Va aparte de `DatosDelPeriodo` para no cambiar sus dobles en seis pruebas; lo implementa el mismo `DatosDesdeProcesos` |
| `AvisoDePlazoVencido` (salida, nuevo) | `avisar(ProcesoAprobacion, VencimientoDePlazo)`, en la transacción en curso |
| `CambioDeFlujo.Accion` (salida, existente) | Suma `PLAZO_VENCIDO` |

## Esquema `validacionocs` (migración V9)

```text
plazo_vencido                      solo de inserción (principio VII)
  id             uuid         pk
  periodo        varchar(20)  not null
  etapa          varchar(30)  not null  check (etapa in las 8 etapas con plazo, sin PUBLICADO)
  llegada        timestamptz  not null
  plazo_dias     integer      not null  check (plazo_dias >= 1)
  fecha_limite   date         not null
  detectado_en   timestamptz  not null  check (detectado_en >= llegada)
  unique (periodo, etapa, llegada)       un aviso por llegada (aclaración del 2026-11-20)

configuracion_flujo                existente
  + advertencia  jsonb        null      la advertencia de RN-23 con la que se cargó; nula si no hubo
```

- Sin referencia a `proceso_aprobacion`: un distributivo recreado (research §5 de F12-H02) no borra el registro de sus vencimientos.
- Solo de inserción por diseño, como `transicion` y `reforma`: ningún adaptador la actualiza ni la borra. El esquema no usa disparadores ni permisos por tabla (`deploy/postgres/01-esquemas.sh`).

## Normativa (migración V7)

- La regla `RN-23` en cada versión, sin severidad (solo da parámetros), con el parámetro `dias_antes_del_inicio = 30` (POL 5.2).
- Un `VERSION_EDITADA` de `sistema` por versión completada (D-68), como en V6.
- Una versión nueva copia los parámetros de la anterior (`RepositorioVersionesJdbc`), así que RN-23 se hereda.
- `ConsultarRemisionDelPeriodoServicio` (nuevo) lee el parámetro de la versión que rige el periodo.

## Académico

`EstadoDelPeriodo` del contrato interno suma `inicio` (date, obligatorio en la respuesta). Lo lee `InternoAcademicoControlador` del `PeriodoAcademico`, que ya lo tiene.

## Estados

El plazo no agrega estados al distributivo. El estado del plazo se calcula siempre con el reloj, y no se guarda:

```text
A_TIEMPO ──(hoy = fechaLimite)──► VENCE_HOY ──(hoy > fechaLimite)──► VENCIDO
    ▲                                                                   │
    └───────── llegada a otra etapa, o de nuevo a la misma ◄────────────┘
                (el plazo empieza otra vez; un aviso por llegada)
```
