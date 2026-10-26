# Data Model: Vista previa y confirmación de la carga

El porqué de cada pieza está en [research.md](research.md).

## `:plataforma:cargas`: el lenguaje publicado (§1)

| Tipo | Qué es | Reglas |
|---|---|---|
| `FilaDelLote` | Una fila del archivo: su número (el encabezado es la 1) y sus valores por campo, sin espacios. | Solo filas sin errores; la vacía no llega. |
| `LoteDeCarga` | La carga (su código), el hash del archivo y sus filas. | El hash es SHA-256 en hexadecimal (64). |
| `TipoDeRegistro` | `NUEVO`, `CAMBIA`, `BAJA`, `IGUAL`. | — |
| `CambioDeCampo` | El campo, su valor de ahora y el del archivo. | Solo campos comparables (§5). |
| `RegistroDeLaVista` | La clave (sus valores unidos con « · »), su tipo, su fila (vacía en una baja) y sus cambios. | — |
| `VistaPreviaDelLote` | Los totales de cada tipo, los registros y la huella. | La huella es SHA-256 de la forma canónica (§3). |
| `ResultadoDelLote` | Cuántos registró y cuántos corrigió, y las bajas sin aplicar. | — |
| `RechazoDelLote` (excepción) | La fila, el campo (opcional), el motivo y el código del catálogo del dueño. | El motivo no repite valores. |
| `VistaPreviaDesactualizada` (excepción) | La huella cambió. | — |
| `TraductorDeCarga<T>` (interfaz) | Lo que escribe cada dueño por carga: la clave, la entidad, las existentes, los campos comparables, registrar y corregir. | Una por carga. |
| `MotorDelLote` | Compara, calcula la huella y aplica dentro de la transacción del dueño. | Bloquea antes de recalcular. |
| `AplicadorDeCargas` (interfaz) | Las cargas que atiende, `previsualizar` y `aplicar`. | Cada una de las 10 cargas tiene un solo aplicador. |

## Cargas

| Tipo o tabla | Qué es |
|---|---|
| `VistaPrevia` (dominio) | La del lote más `filasConErrores`; sin grupos si hay errores. |
| `EntradaDeHistorial` (dominio) | La carga, el usuario, el momento, el archivo, el hash, el resultado (`APLICADA`, `RECHAZADA`, `REPETIDA`), los totales y el código del motivo. |
| `cargas.historial_carga` (V2) | `id` bigint generado, `carga` text, `usuario` text, `momento` timestamptz, `archivo` text (≤ 255), `hash` char(64), `resultado` text con `check`, `nuevos`, `cambiados` y `bajas_sin_aplicar` int ≥ 0, `motivo` text nulo. Índices por (`carga`, `momento desc`) y por (`carga`, `hash`) con `resultado = 'APLICADA'`. |
| `ConfirmarCarga` (puerto de entrada) | Confirma con el archivo y la huella. |
| `ConsultarHistorial` (puerto de entrada) | El historial de una carga. |
| `HistorialDeCargas` (puerto de salida) | Guardar y leer el historial. |

## Académico

| Tipo o tabla | Qué es |
|---|---|
| `Matricula` (dominio) | Periodo, carrera, total (≥ 0, hasta 99 999), fecha de corte y fuente (≤ 60, opcional). |
| `academico.matricula` (V6) | La clave (`periodo`, `carrera`), las referencias a `periodo_academico` y `carrera`, y las acciones `MATRICULA_REGISTRADA` y `MATRICULA_CORREGIDA` en el `check` del registro. |
| `AplicadorAcademico` | Las cargas 01 a 05 y 10, con un traductor por carga. |

## Docentes y Normativa

| Servicio | Tipo | Qué es |
|---|---|---|
| Docentes | `AplicadorDocentes` | Las cargas 06, 08 y 09, con sus traductores (de `DocentesDePrueba` y `CargasDePrueba`). |
| Normativa | `AplicadorNormativa` | La carga 07: la versión que no existe se crea copiando la última y se edita; la que existe se edita si no está en uso (§7). |
