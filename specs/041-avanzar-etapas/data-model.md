# Data Model: Avanzar el distributivo según el rol de la etapa

## Proceso de aprobación (Validación OCS, dominio)

`ProcesoAprobacion`: uno por distributivo (por periodo). Agregado del modelo de dominio §5.

| Campo | Tipo | Regla |
|---|---|---|
| `periodo` | texto | `^[A-Za-z0-9._-]{1,20}$`. Un distributivo por periodo. |
| `distributivoCreadoEn` | instante | El `creadoEn` del distributivo que acompaña. Si no coincide con el del distributivo actual del periodo, el proceso es de un distributivo anterior (por ejemplo, uno borrado por un script) y se recrea. |
| `etapas` | lista de `EtapaDelProceso` | Copia de la configuración que regía al crearse el distributivo (research §5): las 9, en orden, con `tipo`, `activa` y `rol`. No cambia. |
| `estado` | `TipoDeEtapa` | Una etapa activa de `etapas`. Al crearse, BORRADOR_CARRERA. |
| `parciales` | mapa carrera → `EstadoDeEnvio` | `PENDIENTE` o `ENVIADO`, uno por parcial de la versión vigente (research §6). `PENDIENTE` es el término de Aprobación: equivale a «en elaboración» o «devuelto» en Generación (`EstadoDelParcial`). |
| `transiciones` | lista de `Transicion` | En orden. Solo crece. |

**Operaciones** (dominio puro, sin infraestructura):

| Operación | Exige | Resultado |
|---|---|---|
| `enviarParcial(carrera, quien, rolesDeQuien, carreraDeQuien, version, hash, ahora)` | `estado == BORRADOR_CARRERA`; `rolesDeQuien` contiene el rol de BORRADOR_CARRERA; `carreraDeQuien`, si tiene alcance propio, es `carrera`; el parcial está `PENDIENTE` | Una `Transicion` de envío; si era el último, otra de paso a `siguienteActiva(BORRADOR_CARRERA)` con el hash del distributivo |
| `avanzar(quien, rolesDeQuien, version, hash, ahora)` | `estado` es una etapa opcional sin operación propia (hoy, BORRADOR_UNIDADES); `rolesDeQuien` contiene su rol | Una `Transicion` de paso a la siguiente etapa activa |
| `siguienteActiva(etapa)` | — | La primera etapa posterior con `activa = true`. Siempre existe hasta PUBLICADO, que es del esqueleto. |
| `puedeAvanzar(rolesDeQuien)` | — | Si ve la acción (FR-014) |

**Errores del dominio** (cada uno con su código, research §10): `EtapaNoAdmiteEnvio`, `ParcialYaEnviado`, `EtapaConOperacionPropia`, `RolDeOtraEtapa` y `ParcialDeOtraCarrera` (los dos últimos se traducen a 403 sin decir qué falta).

**Etapas con operación propia** (el avance genérico las rechaza): EN_CONSOLIDACION (F10-H03), EN_REVISION_VRA (F13-H01), CONGELADO_EN_FIRMA (F14-H03), EN_OCS (F15-H01), REMITIDO_ZONAL (desactivada; sin historia), APROBADO (F18-H01) y PUBLICADO (final).

## Transición

`Transicion` (modelo de dominio §5): un valor que no cambia.

| Campo | Tipo | Regla |
|---|---|---|
| `tipo` | `ENVIO_PARCIAL` o `PASO_DE_ETAPA` | |
| `desde` | `TipoDeEtapa` | |
| `hacia` | `TipoDeEtapa` | Igual a `desde` en un envío (el distributivo sigue en BORRADOR_CARRERA). |
| `carrera` | texto, opcional | Solo en un envío. |
| `version` | entero | El número de la versión vigente. |
| `hash` | texto | `^[0-9a-f]{64}$` (research §4). |
| `quien` | texto | De 1 a 64 caracteres. |
| `momento` | instante | |

Va a la bitácora con la acción `PARCIAL_ENVIADO` o `ETAPA_AVANZADA` (research §3, D-95).

## Tablas (migración `V3__proceso_aprobacion.sql` de Validación OCS)

| Tabla | Columnas | Restricciones |
|---|---|---|
| `proceso_aprobacion` | `periodo varchar(20) pk`, `estado varchar(30)`, `etapas jsonb`, `distributivo_creado_en timestamptz`, `creado_en timestamptz` | `estado` en los 9 tipos |
| `parcial_del_proceso` | `periodo`, `carrera varchar(20)` (como `generacion.parcial_carrera`), `estado varchar(10)` | pk `(periodo, carrera)`; `estado in ('PENDIENTE','ENVIADO')` |
| `transicion` | `id uuid pk`, `periodo`, `tipo`, `desde`, `hacia`, `carrera`, `version int`, `hash char(64)`, `quien varchar(64)`, `momento timestamptz` | Solo de inserción; índice `(periodo, momento)` |

La fila de `proceso_aprobacion` se bloquea (`FOR UPDATE`) en cada envío y en cada avance (research §8).

## Generación

**Interfaz publicada nueva** (`puertos.entrada`, `@NamedInterface("api")`): `ParcialesDelDistributivo`.

| Método | Devuelve |
|---|---|
| `estadoDelDistributivo(periodo)` | `EstadoDelDistributivo(periodo, version, creadoEn, parciales: lista de (carrera, estado))`, o `DistributivoNoEncontrado` |
| `marcarParcialEnviado(periodo, carrera)` | `ParcialMarcado(version, hash)`. Bloquea la versión vigente; si el parcial ya estaba enviado, devuelve el mismo hash sin cambiar nada. No registra en su `RegistroDeCambios` (D-95). |
| `hashDelDistributivo(periodo)` | El hash del distributivo con sus parciales (research §4) |

**Dominio:** `ContenidoDelParcial` arma el JSON canónico de un parcial y su SHA-256; `VersionDistributivo.conParcialEnviado(carrera)` devuelve la versión con ese parcial `ENVIADO`. `EstadoDelParcial.ENVIADO` ya existe y `exigirEditable` ya lo rechaza (`GENERACION_PARCIAL_NO_EDITABLE`).

## Lo que no cambia

- `validacionocs.configuracion_flujo`: se lee, no se escribe.
- `generacion.parcial_carrera`: solo cambia su `estado` a `ENVIADO`, por orden de Aprobación.
- La bitácora (`usuarios.bitacora`): recibe dos acciones nuevas de `validacionocs`.
