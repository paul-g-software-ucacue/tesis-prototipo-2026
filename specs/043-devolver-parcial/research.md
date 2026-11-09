# Research: Devolver un parcial a su carrera con una observación

Fase 0 del plan de F12-H05. La especificación no dejó marcas por aclarar; aquí se resuelve cómo se construye.

## 1. Dónde vive la regla y quién marca el parcial

**Decisión.** La regla es de `ProcesoAprobacion` (Validación OCS), que suma `validarDevolucion(quien, devoluciones)` y `devolver(quien, devoluciones, version, hash, ahora)`. Generación marca los parciales con un método nuevo de su interfaz publicada, `ParcialesDelDistributivo.marcarParcialesDevueltos(periodo, carreras)`, en una transacción suya que bloquea la versión vigente, como `marcarParcialEnviado`.

**Por qué.** El modelo de dominio dice que Aprobación «ordena a Distributivo devolver un parcial» (MD-04) y que `devolver()` es de `ProcesoAprobacion`. Es el mismo reparto de F12-H02: la etapa y sus reglas en Aprobación, el estado editable del parcial en Generación.

**Alternativas.** Que Generación decida si se puede devolver: no conoce la etapa ni el rol.

## 2. El orden de las operaciones y qué pasa si falla una

**Decisión.** Como enviar un parcial en F12-H02, en tres pasos:
1. Periodo abierto; `validarDevolucion` sin cambiar nada: el rol, la etapa, que la etapa admita devolución, que las carreras no se repitan y que cada parcial esté enviado. Lo que no cabe se rechaza sin tocar Generación (FR-013).
2. `Distributivo.marcarDevueltos(periodo, carreras)`: Generación marca todos los parciales DEVUELTO en **una** transacción suya, o ninguno si una carrera no tiene parcial (FR-005). Es idempotente.
3. En la transacción de Validación OCS, con el proceso bloqueado (`FOR UPDATE`): `devolver(...)` vuelve a comprobar todo, pasa los parciales a DEVUELTO y el distributivo a BORRADOR_CARRERA, guarda la transición con sus observaciones y publica su entrada en la bitácora.

**Si falla el paso 3 después del 2.** Generación tiene los parciales DEVUELTO y Aprobación sigue en EN_CONSOLIDACION. Repetir la devolución la completa, porque el paso 2 es idempotente. Mientras tanto:
- el coordinador podría editar el parcial, porque Generación ya lo deja;
- por eso el envío a revisión suma una comprobación: exige que Generación tenga **todos** los parciales enviados (`DistributivoActual.enviados`). Si no, 409 `VALIDACIONOCS_PARCIALES_PENDIENTES`. Así un consolidado con un parcial a medio devolver no pasa a revisión.

**Por qué este orden y no el inverso.** Si Aprobación registrara primero y fallara Generación, el distributivo quedaría en BORRADOR_CARRERA con un parcial que el coordinador ve devuelto pero no puede editar, y no hay cómo repetir la devolución, porque ya no está en EN_CONSOLIDACION.

**Alternativas.** Una sola transacción para los dos esquemas: los dos servicios comparten proceso, pero el principio V separa sus transacciones, como en F12-H02.

## 3. Quién puede

**Decisión.** `x-mdd-permiso: CONSOLIDACION:APROBACION`, el mismo de enviar a revisión. En la matriz D-16, «aprobación» incluye devolver. El dominio exige además el rol de EN_CONSOLIDACION en la copia de la configuración. Si falta el permiso o el rol, `SEGURIDAD_ACCESO_NO_AUTORIZADO` (403), el mismo cuerpo.

## 4. «Admite devolución» en la copia de la configuración

**El problema.** La configuración tiene `admite_devolucion` por etapa (F12-H01), pero la copia que guarda el proceso (`proceso_aprobacion.etapas`, jsonb) solo lleva `tipo`, `activa`, `nombre` y `rol`. FR-003 de F12-H02 exige usar la copia, no la configuración vigente.

**Decisión.**
- `EtapaDelProceso` suma `admiteDevolucion`, y el repositorio lo guarda en el jsonb.
- La migración `V4` de Validación OCS completa los procesos que ya existen con el valor de `flujo-por-defecto.yaml` para cada etapa (EN_CONSOLIDACION, EN_REVISION_VRA, CONGELADO_EN_FIRMA y EN_OCS: `true`; las demás: `false`).
- Es exacto para el equipo de demostración: no tiene ninguna configuración cargada, así que su proceso de 2026-I usa la plantilla (consulta de solo lectura, 2026-11-08). En el perfil local, los scripts crean y borran sus procesos.

**Alternativas.**
- Leer `admite_devolucion` de la configuración vigente del periodo: rompe FR-003 de F12-H02.
- Tomar `true` cuando falte: una etapa configurada sin devolución la admitiría.

## 5. El estado del parcial en Aprobación

**Decisión.** `EstadoDeEnvio` suma `DEVUELTO`, y la tabla `parcial_del_proceso` lo admite (`V4`). Las reglas de F12-H02 cambian así:
- `validarEnvio` acepta un parcial PENDIENTE o DEVUELTO; solo rechaza el ENVIADO (`ParcialYaEnviado`).
- `acciones` ofrece `ENVIAR_PARCIAL` al coordinador si su parcial no está enviado.
- `validarRevision` rechaza si algún parcial no está ENVIADO.
- Con el último reenvío, el distributivo pasa a la siguiente etapa activa, sin cambios en `enviarParcial` (FR-010).

**Por qué.** El flujo tiene que distinguir un parcial devuelto, con su observación, de uno que nunca se envió (FR-015).

## 6. La transición, las observaciones y la bitácora

**Decisión.**
- Un tipo nuevo, `DEVOLUCION`, de EN_CONSOLIDACION a BORRADOR_CARRERA, sin carrera (`carrera` es solo para `ENVIO_PARCIAL`). Cabe en `transicion.tipo` (`varchar(15)`); `V4` cambia su `check`.
- `Transicion` suma `observaciones`, un mapa ordenado carrera → texto, vacío en los demás tipos.
- Las observaciones se guardan en una tabla nueva, solo de inserción (principio VII): `observacion_de_devolucion (transicion_id, carrera, texto)`.
- El hash es el del distributivo completo (`hashDelDistributivo`) y la versión, la vigente: devolver no cambia el contenido ni crea versión (FR-014).
- **Una** entrada en la bitácora (D-95), acción `DISTRIBUTIVO_DEVUELTO`, el evento del modelo de dominio (§6). En el detalle van `periodo`, `version`, `hash` y `devueltos` (carrera → observación). En `anterior` y `nuevo`, el estado.

**Por qué un tipo nuevo y no un `PASO_DE_ETAPA`.** Un paso va siempre hacia adelante; la devolución va hacia atrás y lleva observaciones. Con un tipo propio, el flujo encuentra la última devolución de cada carrera sin mirar la dirección.

**Alternativas.**
- Guardar la observación en `parcial_del_proceso`: se perdería al devolver otra vez (la segunda devolución de una carrera, Edge Cases).
- Una entrada por carrera devuelta: D-95 pide una por transición.

**La observación en la bitácora.** Va completa. Es texto libre del Responsable y puede nombrar a un docente, pero no es uno de los campos de datos personales del docente que D-91 deja fuera; RF-36 pide que la vea quien debe corregir, y RF-44 que la transición quede auditada con lo que se decidió.

## 7. El consolidado

**Decisión.** Devolver no marca el consolidado desactualizado. Lo marca, como siempre, cualquier propuesta, asignación o quita posterior a consolidar (F10-H02: `ultimoCambioDeAsignaciones`). Se ajustó FR-006 de la especificación.

**Por qué.** «Desactualizado» quiere decir que el contenido cambió después de consolidar. Si el coordinador reenvía sin cambios, el consolidado que revisó el Responsable sigue siendo el mismo. Marcarlo exigiría que Generación registre la devolución, que es una transición de Aprobación (D-95).

## 8. Lo que ve cada uno

**Decisión.**
- **El contrato:**
  - `ParcialDelFlujo` suma `devolucion`, opcional: `{por, en, observacion}` de la última devolución, presente mientras el parcial está DEVUELTO;
  - `TransicionDelFlujo.tipo` suma `DEVOLUCION`;
  - `AccionDelFlujo` suma `DEVOLVER_PARCIALES`.
- **`EstadoDelFlujo` (SPA):**
  - un parcial devuelto dice «devuelto por … el …» y la observación debajo. Se ve en «Oferta del periodo» y en «Consolidado del periodo», que ya usan el componente;
  - el coordinador ve «Reenviar parcial».
- **`Consolidado.tsx`:**
  - con `DEVOLVER_PARCIALES`, un botón «Devolver parciales» abre un diálogo con las carreras enviadas;
  - en el diálogo, el Responsable marca una o varias y escribe la observación de cada una, de 10 a 500 caracteres, con el contador y el error en el campo;
  - «Devolver» queda deshabilitado mientras falte una observación válida.
- **«Oferta del periodo»** ya decide si se edita con el estado de Generación (`ENVIADO`): un parcial DEVUELTO se edita sin cambios en la pantalla.

**Por qué.** El Responsable ya trabaja en «Consolidado del periodo», con el panel a un enlace (SC-005), y el coordinador en «Oferta del periodo» (SC-006).

## 9. Validación de la entrada

**Decisión.**
- **El contrato:**
  - `devoluciones`: de 1 a 50 elementos;
  - `carrera`: el patrón de `ParcialDelFlujo.carrera`, `^[0-9A-Z_-]{1,20}$`;
  - `observacion`: de 10 a 500 caracteres, con al menos un carácter que no sea espacio (`pattern: '^[\s\S]*\S[\s\S]*$'`). El patrón cubre el texto entero porque la validación generada en Java compara la cadena completa (`@Pattern`); `'\S'` solo aceptaría un carácter.

  Lo que no cumple responde 400 `COMUN_SOLICITUD_INVALIDA` con el campo, como en los demás contratos (principio XIV).
- **El dominio:**
  - una carrera repetida: 400 `VALIDACIONOCS_CARRERA_REPETIDA`;
  - una observación que, sin los espacios de los extremos, quede con menos de 10 caracteres: 400 `VALIDACIONOCS_OBSERVACION_INVALIDA`, con la carrera en `detalles`.

**Por qué.** OpenAPI no puede exigir carreras únicas en una lista de objetos, ni contar sin espacios. La longitud sigue a la justificación de una blanda (F10-H01).

## 10. Errores nuevos

| Código | Estado | Cuándo |
|---|---|---|
| `VALIDACIONOCS_ETAPA_NO_ADMITE_DEVOLUCION` | 409 | El distributivo no está en EN_CONSOLIDACION (también la segunda de dos devoluciones a la vez) |
| `VALIDACIONOCS_DEVOLUCION_DESACTIVADA` | 409 | EN_CONSOLIDACION tiene `admite_devolucion: false` en la copia de la configuración |
| `VALIDACIONOCS_PARCIAL_NO_ENVIADO` | 409 | Una de las carreras no tiene el parcial enviado (por ejemplo, una carrera con su primera propuesta después) |
| `VALIDACIONOCS_CARRERA_REPETIDA` | 400 | La misma carrera dos veces en la devolución |
| `VALIDACIONOCS_OBSERVACION_INVALIDA` | 400 | Sin los espacios de los extremos, la observación tiene menos de 10 caracteres |

Se reutilizan:
- `VALIDACIONOCS_PARCIAL_NO_ENCONTRADO` (404), para una carrera sin parcial;
- `VALIDACIONOCS_SIN_DISTRIBUTIVO` y `VALIDACIONOCS_PERIODO_CERRADO`;
- `SEGURIDAD_ACCESO_NO_AUTORIZADO` y `COMUN_SOLICITUD_INVALIDA`.

Orden de las comprobaciones: el permiso (403) y el contrato (400); la observación, al armar cada `DevolucionDeParcial` (400); después, en el dominio, el rol (403), la etapa, la devolución activa, la carrera repetida, el parcial encontrado y el parcial enviado.

## 11. Dos operaciones a la vez (SC-004)

**Decisión.** El bloqueo de la fila del proceso de F12-H02. La segunda devolución, o un envío a revisión que llega después, encuentra el distributivo en BORRADOR_CARRERA y recibe su 409, sin transición. Generación ya habrá marcado sus parciales (idempotente), lo que no cambia nada si eran los mismos. Si eran otros, quedan DEVUELTO en Generación y ENVIADO en Aprobación:
- el coordinador podría editarlos, pero no reenviarlos: Aprobación responde `ParcialYaEnviado`;
- cuando las carreras de la primera devolución reenvían, el distributivo vuelve a EN_CONSOLIDACION, y el envío a revisión se rechaza con `ParcialesPendientes`, porque Generación no los tiene enviados (§2);
- la salida es que el Responsable vuelva a devolver esas carreras: Aprobación las tiene ENVIADO, así que la devolución pasa, y después su coordinador las reenvía.

La prueba de integración cubre las dos variantes: con las mismas carreras y con carreras distintas, incluida esa salida.

## 12. Cómo se prueba, y SC-005 de F10-H03 (SC-007)

**Decisión.**
- **Dominio:** sin dobles, viéndolo fallar primero.
- **Aplicación:** con Mockito.
- **Integración:** con PostgreSQL real, en Validación OCS. Prueba la devolución guardada con sus observaciones, el reenvío que vuelve a EN_CONSOLIDACION y dos devoluciones a la vez. En Generación, `marcarParcialesDevueltos` todo o nada.
- **Script `deploy/probar_devolucion.sh`** con datos reales, después de `probar_revision.sh` en `perfil-consolidado` (contracts/demostracion.md).
- **SC-007:** el script deja el flujo completo. Lo que falta para cronometrar SC-005 de F10-H03 es un consolidado sin inconsistencias duras: hay que proponer los paralelos que faltan (unos 165) y dar clase a cada docente, respetando afinidad, horas y choques. Al cerrar la historia, una tarea con tiempo acotado intenta armarlo en el perfil local con la sugerencia de docentes (F08). Si llega a cero duras, cronometra el envío y lo anota en `specs/042-enviar-revision/evidencia.md`. Si no, anota cuántas duras quedan y por qué.

**Alternativas.** Armar a mano el distributivo de la demo: son cientos de propuestas.

## 13. La notificación

**Decisión.** Ninguna (D-97, aclaración Q3). La devolución queda en el proceso y en la bitácora con su acción propia, y F16-H01 la notificará. Se registra con el resto de esta historia en D-98.
