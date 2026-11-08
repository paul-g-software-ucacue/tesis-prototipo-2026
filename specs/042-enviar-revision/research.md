# Research: Enviar a revisión con cero inconsistencias abiertas

Fase 0 del plan de F10-H03. La especificación no dejó marcas por aclarar; aquí se resuelve cómo se construye.

## 1. Dónde se evalúan las condiciones

**Decisión.** Generación las calcula y las entrega a Aprobación por su interfaz publicada, en el mismo proceso: un método nuevo `condicionesDeRevision(periodo)` en `ParcialesDelDistributivo`. Lo implementa `ParcialesDelDistributivoServicio` a partir de `ConsultarConsolidado`, que ya arma el panel, la consolidación, si quedó desactualizado y el informe. Devuelve un registro de `puertos.entrada` (`CondicionesDeRevision`) con tipos simples, sin clases del dominio de Generación (principio V).

**Por qué.**
- El panel y el consolidado son de Generación (MD-04). Si Validación OCS los calculara, habría dos evaluaciones del mismo panel, y SC-001 exige que el número del error coincida con el del panel.
- Es el mismo camino de F12-H02: Validación OCS solo usa `generacion.puertos.entrada`, y Spring Modulith lo verifica.

**Alternativas.**
- Que Validación OCS llame directamente a `ConsultarConsolidado`: devuelve `ConsolidadoDelPeriodo`, que es del dominio de Generación.
- Por HTTP: los dos servicios están en el mismo proceso, `distributivo`.

## 2. El orden de las comprobaciones y las transacciones

**Decisión.**
1. Se comprueba el periodo abierto, como en F12-H02.
2. `ProcesoAprobacion.validarRevision(quien)`, sin cambiar nada: el rol de EN_CONSOLIDACION, la etapa actual y todos los parciales enviados. Lo que no cabe se rechaza sin llamar a Generación (FR-012).
3. `Distributivo.condicionesDeRevision(periodo)`, **fuera de una transacción**: el panel consulta a Datos por HTTP, y no se abre una transacción durante una llamada a otro proceso (D-83).
4. En la transacción de Validación OCS, con el proceso bloqueado (`FOR UPDATE`): `enviarARevision(quien, condiciones, version, hash, ahora)` vuelve a comprobar el rol y la etapa, aplica las condiciones y registra la transición y su entrada en la bitácora.

**Por qué no hay una carrera entre el paso 3 y el 4.** En EN_CONSOLIDACION los parciales están enviados y Generación no acepta propuestas ni actividades en ellos (`GENERACION_PARCIAL_NO_EDITABLE`). Entre los dos pasos solo puede pasar esto:
- una justificación nueva, que baja las abiertas;
- otro informe, que sigue siendo un informe;
- otra consolidación, que deja el consolidado al día.

Ninguna convierte en inválido un envío que era válido. El único caso que sí lo haría, una carrera sin parcial que recibe su primera propuesta, crea un parcial pendiente: lo atrapan el paso 2 y el paso 4 (§4).

**Alternativas.** Evaluar las condiciones dentro de la transacción: abre una transacción durante una llamada HTTP a Datos (D-83).

## 3. Quién puede

**Decisión.** `x-mdd-permiso: CONSOLIDACION:APROBACION`. En la matriz D-16, ese permiso es solo del Responsable de consolidación, y es su equivalente a `OFERTA:APROBACION` del coordinador. Además, el dominio exige el rol de EN_CONSOLIDACION en la copia de la configuración del proceso. Si falta el permiso o el rol, `SEGURIDAD_ACCESO_NO_AUTORIZADO` (403), el mismo cuerpo en los dos casos.

**Por qué.** Es la regla de F12-H02 (research §7 de 041): el permiso da acceso a la operación (ADR-021) y el rol de la etapa es una regla del dominio por periodo (RF-34).

**Consecuencia.** Si una configuración le pone a EN_CONSOLIDACION otro rol sin `CONSOLIDACION:APROBACION`, ese rol recibe 403. Es la misma limitación de `enviarParcial`: queda documentada, y cambiarla exige cambiar la matriz.

## 4. Un parcial pendiente en EN_CONSOLIDACION

**Decisión.** Enviar a revisión exige que todos los parciales del proceso estén enviados. Si no, 409 `VALIDACIONOCS_PARCIALES_PENDIENTES`.

**Por qué.** Una carrera sin parcial puede recibir su primera propuesta después de crear el distributivo, y Generación le crea uno (research §6 de 041). El proceso lo suma como pendiente. Enviar a revisión con un parcial que su carrera no aprobó rompería RF-34. Ese caso no se arregla en esta historia: lo corrige devolver (#211) o reenviar el parcial.

## 5. El hash, la transición y la bitácora

**Decisión.**
- La transición es un `PASO_DE_ETAPA` de EN_CONSOLIDACION a la siguiente etapa activa (por defecto, EN_REVISION_VRA). No hace falta un tipo nuevo: un paso desde EN_CONSOLIDACION solo puede venir de esta operación, porque el avance genérico rechaza esa etapa. Así no hay migración: la tabla `transicion` ya admite el paso, y su `tipo` es `varchar(15)`.
- El hash es el del distributivo completo, `hashDelDistributivo` de F12-H02 (FR-010).
- La bitácora recibe **una** entrada (D-95) con la acción `CONSOLIDADO_ENVIADO_A_REVISION`, el nombre del evento del modelo de dominio (`ConsolidadoEnviadoARevision`, §6). `cambio()` la elige para un paso desde EN_CONSOLIDACION. En el detalle van `periodo`, `version`, `hash` y `informe` (la huella del informe vigente). En `anterior` y `nuevo` va el estado.
- La huella del informe no se guarda en la transición, solo en la entrada de la bitácora. Así la tabla `transicion` no cambia.

**Por qué.** Una acción propia, y no `ETAPA_AVANZADA`, deja que la bitácora filtre los envíos a revisión y que F16-H01 sepa a quién avisar. La huella del informe dice qué sustento recibió el Vicerrector.

**Alternativas.** Un tipo `ENVIO_A_REVISION`: necesita una migración V4 que ensanche la columna y cambie su `check`, y no dice nada que el paso desde EN_CONSOLIDACION no diga ya.

## 6. Dos envíos a la vez (SC-004)

**Decisión.** El bloqueo de la fila del proceso de F12-H02. El segundo envío espera, encuentra el distributivo en EN_REVISION_VRA y recibe 409 `VALIDACIONOCS_ETAPA_NO_ADMITE_REVISION`, sin otra transición.

## 7. La notificación al Vicerrector

**Decisión.** No hay notificación en esta historia (aclaración Q1). La transición queda en el proceso y en la bitácora con su acción propia (§5). Cuando llegue F16-H01, notificará cada cambio de estado, también este. Se registra como D-97, con las condiciones de §8.

## 8. Las condiciones del envío (aclaración Q2)

**Decisión.** Se comprueban en este orden, y responde el primer error que se encuentra:

| # | Condición | Código (409) |
|---|---|---|
| 1 | El distributivo está en EN_CONSOLIDACION | `VALIDACIONOCS_ETAPA_NO_ADMITE_REVISION` |
| 2 | Todos los parciales enviados | `VALIDACIONOCS_PARCIALES_PENDIENTES` |
| 3 | Se consolidó | `VALIDACIONOCS_SIN_CONSOLIDAR` |
| 4 | El consolidado no está desactualizado | `VALIDACIONOCS_CONSOLIDADO_DESACTUALIZADO` |
| 5 | Tiene informe sustentado | `VALIDACIONOCS_SIN_INFORME` |
| 6 | Cero abiertas (MD-07): 0 duras y 0 blandas sin justificar | `VALIDACIONOCS_INCONSISTENCIAS_ABIERTAS` |

**Por qué este orden.** Va de lo que se arregla antes a lo que se arregla después. No sirve justificar blandas en un consolidado que hay que repetir.

**El número de abiertas (US2).** La descripción del catálogo es fija («Quedan inconsistencias abiertas…»). El número va en `detalles`, como `GENERACION_HORAS_EXCEDIDAS` lleva las horas que quedan: `{campo: "inconsistencias", descripcion: "Quedan 3: 2 duras y 1 blanda sin justificar."}`. Se usa `EntradaCatalogo.conDetalles`.

**SC-006.** La pantalla no necesita una consulta nueva para ver todas las faltas a la vez. Las condiciones 3 a 6 ya están en `consultarConsolidado` (`consolidacion`, `desactualizado`, `informe`, `abiertas`), y las 1 y 2 en `consultarFlujo`.

## 9. La pantalla

**Decisión.**
- `EstadoDelFlujo` suma la acción `ENVIAR_A_REVISION`, con el botón «Enviar a revisión» y una confirmación.
- Recibe una lista opcional `faltan`: si tiene algo, el botón queda deshabilitado y debajo se lista lo que falta.
- `Consolidado.tsx` arma la lista con los datos que ya consulta:
  - «Consolida el periodo»;
  - «Consolida otra vez: algo cambió»;
  - «Adjunta el informe sustentado»;
  - «Quedan N inconsistencias abiertas», con enlace al panel.
- Al enviar, se invalidan el flujo y el consolidado.

**Por qué.** El Responsable ya trabaja en «Consolidado del periodo», y ahí ya están las cuatro condiciones (SC-005, SC-006). El servidor vuelve a comprobarlas (FR-008).

## 10. Cómo se prueba el envío que pasa

**El problema.** Con los datos de prueba, el panel de 2026-I tiene más de cien inconsistencias duras: los paralelos sin propuesta (RF-24) y los docentes sin clase (RN-17). La normativa marca las dos como duras, y en EN_CONSOLIDACION no se pueden corregir mientras devolver no exista (#211). Ni el script ni la demo pueden llegar al panel vacío con datos reales sin armar un distributivo completo, que haría falta proponer con reglas de afinidad, horas y topes.

**Decisión.**
- **El envío que pasa** (US1, SC-002, SC-004):
  - pruebas de aplicación con Mockito;
  - una prueba de integración nueva en Validación OCS, con PostgreSQL real: `FlujoDelDistributivoServicio`, `RepositorioProcesoJdbc` y la bitácora reales, y un `Distributivo` falso con las condiciones cumplidas. Prueba el paso a EN_REVISION_VRA, la única transición, su entrada y dos envíos simultáneos con un solo paso.
- **Las condiciones en Generación**, con su prueba de aplicación: `condicionesDeRevision` coincide con `consultarConsolidado`.
- **El script `deploy/probar_revision.sh`**, con datos reales:
  - lleva 2026-I a EN_CONSOLIDACION, con los envíos de `probar_avance.sh`;
  - comprueba el 403 de los 9 roles que no son el de la etapa;
  - comprueba `SIN_CONSOLIDAR`, consolida, comprueba `SIN_INFORME` y adjunta un PDF mínimo;
  - comprueba que `INCONSISTENCIAS_ABIERTAS` dice el mismo número que el panel, y que baja en uno al justificar una blanda;
  - comprueba que ningún rechazo deja transición ni entrada.
- **La comprobación del autor:** en la demo, ve las condiciones que faltan y el 409 con el número del panel. El envío que pasa queda en las pruebas automáticas hasta que #211 permita corregir las duras.

**Alternativas descartadas.**
- Una normativa de prueba con todo blando, y justificar unas 220 inconsistencias por la API: un periodo nuevo con sus 10 cargas, y minutos de CI.
- Cambiar severidades o insertar justificaciones por SQL: el script fabricaría un estado que el sistema no permite.

## 11. Errores nuevos

| Código | Estado | Cuándo |
|---|---|---|
| `VALIDACIONOCS_ETAPA_NO_ADMITE_REVISION` | 409 | El distributivo no está en EN_CONSOLIDACION |
| `VALIDACIONOCS_PARCIALES_PENDIENTES` | 409 | Algún parcial del proceso sin enviar |
| `VALIDACIONOCS_SIN_CONSOLIDAR` | 409 | Nunca se consolidó la versión vigente |
| `VALIDACIONOCS_CONSOLIDADO_DESACTUALIZADO` | 409 | Algo cambió después de consolidar |
| `VALIDACIONOCS_SIN_INFORME` | 409 | La versión consolidada no tiene informe |
| `VALIDACIONOCS_INCONSISTENCIAS_ABIERTAS` | 409 | Quedan duras o blandas sin justificar; con el número en `detalles` |

Cada uno lleva sus 8 columnas en `contratos/comun/errores.md` (principio XIII). Los ya existentes se reutilizan: `VALIDACIONOCS_SIN_DISTRIBUTIVO`, `VALIDACIONOCS_PERIODO_CERRADO`, `SEGURIDAD_ACCESO_NO_AUTORIZADO` y los 502 y 504 comunes si Datos no responde al evaluar el panel.
