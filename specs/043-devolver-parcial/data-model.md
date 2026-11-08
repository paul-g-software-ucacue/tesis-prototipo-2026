# Data Model: Devolver un parcial a su carrera con una observación

El proceso de aprobación, sus transiciones y la bitácora son los de F12-H02 (`specs/041-avanzar-etapas/data-model.md`). Esta historia los extiende con una migración `V4` en Validación OCS. Generación no cambia su esquema: `parcial_carrera.estado` ya admite `DEVUELTO`.

## Proceso de aprobación (Validación OCS, dominio)

| Operación | Exige | Resultado |
|---|---|---|
| `validarDevolucion(quien, devoluciones)` | En este orden (research §10): el rol de EN_CONSOLIDACION en la copia (`RolDeOtraEtapa`, 403); `estado == EN_CONSOLIDACION` (`EtapaNoAdmiteDevolucion`); `admiteDevolucion` de EN_CONSOLIDACION en la copia (`DevolucionDesactivada`); sin carreras repetidas (`CarreraRepetida`); cada carrera con parcial en el proceso (`ParcialNoEncontrado`) y ENVIADO (`ParcialNoEnviado`) | Nada: solo comprueba, antes de llamar a Generación (research §2) |
| `devolver(quien, devoluciones, version, hash, ahora)` | Lo de `validarDevolucion` | Los parciales devueltos pasan a `DEVUELTO`, el estado a BORRADOR_CARRERA y queda una `Transicion` `DEVOLUCION` con sus observaciones |
| `acciones(quien)` | — | Suma `DEVOLVER_PARCIALES` si `estado == EN_CONSOLIDACION`, `quien` tiene su rol y la etapa admite devolución |

**Cambios en las reglas de F12-H02** (research §5):
- `validarEnvio`: acepta PENDIENTE o DEVUELTO.
- `acciones`: `ENVIAR_PARCIAL` si el parcial del coordinador no está ENVIADO.
- `validarRevision`: rechaza si algún parcial no está ENVIADO.

**Un intento rechazado** no cambia el estado, los parciales ni las transiciones (FR-013).

## Devolución de una carrera (Validación OCS, dominio)

`DevolucionDeParcial(carrera, observacion)`: un valor que no cambia.

| Campo | Regla |
|---|---|
| `carrera` | Obligatoria; el código de la carrera |
| `observacion` | Sin los espacios de los extremos, de 10 a 500 caracteres (`ObservacionInvalida` con la carrera). Se guarda sin esos espacios |

## Etapa de la copia

`EtapaDelProceso` suma `admiteDevolucion` (booleano), tomado de `Etapa.admiteDevolucion()` al crear el proceso.

## Estados

`EstadoDeEnvio`: `PENDIENTE`, `ENVIADO` y **`DEVUELTO`**.

`TipoDeTransicion`: `ENVIO_PARCIAL`, `PASO_DE_ETAPA` y **`DEVOLUCION`**.

`Transicion` suma `observaciones`: mapa ordenado carrera → texto, solo en `DEVOLUCION`; vacío en los demás tipos.

```text
                ┌──────── devolver (F12-H05): parciales elegidos ENVIADO → DEVUELTO ────────┐
                ▼                                                                             │
BORRADOR_CARRERA ──último parcial enviado o reenviado (F12-H02)──► EN_CONSOLIDACION ──────────┘
                                                                         │
                                                                         └── enviar a revisión (F10-H03) ──► EN_REVISION_VRA
```

## Migración `V4__devolucion.sql` (Validación OCS)

| Cambio | Detalle |
|---|---|
| `parcial_del_proceso.estado` | El `check` admite `'DEVUELTO'` (cabe en `varchar(10)`) |
| `transicion.tipo` | El `check` admite `'DEVOLUCION'`. El `check` de la carrera ya vale: solo `ENVIO_PARCIAL` la lleva |
| `observacion_de_devolucion` (nueva) | `transicion_id uuid references transicion (id) on delete cascade`, `carrera varchar(20) not null`, `texto varchar(500) not null check (length(btrim(texto)) between 10 and 500)`, `primary key (transicion_id, carrera)`. Solo de inserción (principio VII) |
| `proceso_aprobacion.etapas` | Cada etapa de la copia suma `admiteDevolucion`, con el valor de `flujo-por-defecto.yaml` para su tipo (research §4) |

## Lo que publica Generación (`generacion.puertos.entrada`)

| Método | Contenido |
|---|---|
| `ParcialesDelDistributivo.marcarParcialesDevueltos(periodo, carreras)` | Nuevo. En una transacción, con la versión vigente bloqueada, marca DEVUELTO cada parcial; si una carrera no tiene parcial, `ParcialNoEncontrado` y no marca ninguno. Idempotente. No registra en el registro de cambios de Generación (D-95). Devuelve la versión |

El adaptador `DistributivoDesdeGeneracion` lo expone en el puerto `Distributivo.marcarDevueltos`.

## Transición y bitácora

| Dato | Valor |
|---|---|
| `Transicion.tipo` | `DEVOLUCION` |
| `desde` → `hacia` | EN_CONSOLIDACION → BORRADOR_CARRERA |
| `carrera` | vacía |
| `observaciones` | carrera → texto, de cada parcial devuelto |
| `version`, `hash` | La versión vigente; `hashDelDistributivo` |
| Acción en la bitácora | `DISTRIBUTIVO_DEVUELTO` (nueva en `CambioDeFlujo.Accion`) |
| Detalle | `periodo`, `version`, `hash`, `devueltos` (carrera → observación) |
| Anterior / nuevo | `{estado: EN_CONSOLIDACION}` / `{estado: BORRADOR_CARRERA}` |

## Vista del flujo

`FlujoDelDistributivo.Parcial` suma `devolucion`, opcional: quién, cuándo y la observación de la última `DEVOLUCION` que incluye la carrera. Está presente mientras el parcial está DEVUELTO.
