# Research: El consolidado no cambia durante la revisión

Fase 0 del plan de #215. La especificación no dejó marcas por aclarar; aquí se resuelve cómo se construye.

## 1. Cómo sabe Generación en qué etapa está el distributivo

**El problema.** La etapa la conoce Aprobación (Validación OCS). Validación OCS ya depende de la interfaz publicada de Generación (`generacion.puertos.entrada`, F12-H02). Si Generación llamara a una interfaz publicada de Validación OCS, habría un ciclo entre los dos módulos, y Spring Modulith lo rechaza (`ModulosTest`).

**Decisión: inversión de la dependencia.**
- Generación declara lo que necesita en un puerto propio, publicado en un paquete nuevo con su propia interfaz nombrada: `ec.mdd.generacion.puertos.requeridos.EtapaDelConsolidado` (`@NamedInterface("requeridos")`).
- El puerto tiene un solo método, `admiteCambios(periodo, distributivoCreadoEn)`, que devuelve un booleano. Generación no conoce las etapas.
- Validación OCS lo implementa con un adaptador de entrada (`adaptadores.entrada.EtapaParaGeneracion`), que delega en un caso de uso propio (`puertos.entrada.ConsultarEtapaDelConsolidado`).
- La regla de qué etapas admiten cambios es del dominio de Aprobación: `TipoDeEtapa.admiteCambiosEnElConsolidado()`, verdadera hasta EN_CONSOLIDACION inclusive (BORRADOR_CARRERA, BORRADOR_UNIDADES y EN_CONSOLIDACION).
- Validación OCS sigue dependiendo solo de Generación; Generación no depende de nadie. No hay ciclo.

**Por qué.**
- La comprobación se hace en el momento de la acción, con la etapa real (FR-006). No hay un estado copiado en Generación que se pueda desincronizar.
- Cuando una historia futura devuelva desde EN_REVISION_VRA (F13-H01, F14-H04), el bloqueo se levanta solo (Assumptions).
- El principio V se respeta: Generación no lee tablas de Validación OCS; usa un puerto, y Validación OCS lee las suyas.

**Alternativas.**
- **Que Aprobación le ordene a Generación «cerrar el consolidado» al enviar a revisión** (MD-04, como marcar un parcial). Exige un estado nuevo en Generación y una migración, y abre una carrera: si la transacción de Aprobación falla después de cerrar, Generación queda cerrada con el distributivo en EN_CONSOLIDACION y hace falta una compensación. Una devolución futura también tendría que reabrirlo.
- **Un evento de cambio de etapa que Generación escuche:** es eventual. Entre la transición y el evento, consolidar todavía pasaría.
- **Que la SPA lo impida sola:** no protege la API (principio IX).

## 2. Sin carrera entre consolidar y enviar a revisión (SC-004)

**El problema.** Enviar a revisión bloquea la fila del proceso (`FOR UPDATE`, F12-H02). Si consolidar solo leyera la etapa, podría leer EN_CONSOLIDACION, el envío confirmaría, y la consolidación se guardaría después.

**Decisión.**
- Las tres operaciones de Generación (consolidar, justificar, adjuntar el informe) consultan el puerto **dentro** de su transacción de escritura.
- El adaptador lee la etapa con `SELECT … FOR SHARE` sobre `validacionocs.proceso_aprobacion`. Las dos transacciones son del mismo `DataSource` y el mismo administrador de transacciones del proceso `distributivo`, así que la lectura entra en la transacción de Generación.
- Un `FOR SHARE` y un `FOR UPDATE` sobre la misma fila se esperan entre sí. Si la operación de Generación llega primero, el envío espera a que confirme. Si el envío llega primero, la operación lee EN_REVISION_VRA y se rechaza.
- **Ajuste en el envío a revisión (F10-H03):** el momento de la transición se toma dentro de la transacción bloqueada, no antes. Así, si el envío esperó a una consolidación, su momento es posterior a ella (SC-004).

**El informe (A2 del análisis).** Hoy Generación guarda el archivo en el almacén **antes** de su transacción, y el almacén no tiene una operación para borrar. Por eso el informe se comprueba dos veces:
1. antes de guardar el archivo, con `enRevision` del consolidado que el servicio ya lee al empezar: el caso normal se rechaza sin tocar el almacén;
2. dentro de la transacción, con el bloqueo, por si el envío a revisión llega entre las dos.

Solo en ese segundo caso, que exige dos peticiones a la vez, el PDF queda guardado sin su fila, igual que hoy con cualquier fallo de la transacción. No se agrega una operación para borrar (principio X).

**Lo que queda fuera.** Las condiciones del envío se calculan antes del bloqueo (D-83, por la llamada a Datos). Una consolidación o un informe que entren entre ese cálculo y el bloqueo dejan la entrada de la bitácora con la huella anterior del informe. Es la misma ventana que F10-H03 aceptó (research §2 de 042), y ahora ya no puede pasar después del envío.

## 3. Qué ve la pantalla

**Decisión.**
- `ConsolidadoDelPeriodo` y el panel suman `enRevision` (booleano obligatorio) en el contrato de Generación: verdadero si el distributivo está en EN_REVISION_VRA o después.
- Las consultas lo calculan con el mismo puerto, sin bloquear nada: la lectura queda fuera de una transacción de escritura.
- **La SPA:**
  - con `enRevision`, «Consolidado del periodo» no muestra «Consolidar» o «Volver a consolidar» ni «Adjuntar el informe», y muestra el aviso;
  - el panel no muestra «Justificar» y muestra el mismo aviso;
  - la descarga del informe no cambia.

**Por qué en el contrato y no en la SPA con el flujo.** El panel no consulta el flujo, y el servidor ya sabe la respuesta. Así hay una sola regla.

## 4. El error

**Decisión.** Un código nuevo: `GENERACION_CONSOLIDADO_EN_REVISION` (409).
- Título: «Consolidado en revisión».
- Descripción: «El distributivo está en revisión del Vicerrectorado: el consolidado ya no cambia.»
- Excepción del dominio de Generación: `ConsolidadoEnRevision`.

El periodo cerrado se comprueba antes y conserva su error. El 403 de permiso va antes que todo.

**Orden de las comprobaciones:**
1. el permiso (403);
2. el periodo cerrado (`GENERACION_PERIODO_CERRADO`);
3. sin distributivo;
4. la etapa (`GENERACION_CONSOLIDADO_EN_REVISION`);
5. los errores propios de cada operación (por ejemplo, sin consolidar al adjuntar el informe).

## 5. Sin proceso todavía

**Decisión.** Si no hay proceso de aprobación para el periodo, o el guardado es de un distributivo anterior (otra `distributivo_creado_en`), el puerto responde que **admite cambios**. El proceso se crea en la primera consulta del flujo (F12-H02); antes de eso, el distributivo está en BORRADOR_CARRERA.

## 6. Cómo se prueba

- **Dominio** (Validación OCS): `TipoDeEtapa.admiteCambiosEnElConsolidado()` para las 9 etapas.
- **Aplicación** (Mockito):
  - en Generación, cada operación consulta el puerto dentro de su transacción, se rechaza sin guardar ni registrar si no admite cambios, y funciona como hoy si admite;
  - `ConsultarConsolidado` y el panel informan `enRevision`;
  - en Validación OCS, el caso de uso con el proceso en cada etapa, sin proceso y con el de un distributivo anterior.
- **Integración con PostgreSQL real** (Validación OCS):
  - el `FOR SHARE` del adaptador;
  - una transacción con el `FOR SHARE` tomado retrasa un envío a revisión, que registra su transición con un momento posterior.
- **Contrato** (Generación): el 409 en las tres operaciones y `enRevision` en las dos consultas.
- **Arquitectura:** `ModulosTest`, sin ciclos y con la interfaz nombrada nueva. Las pruebas de Spring de Generación que arman su configuración suman un doble del puerto.
- **SPA:** Vitest de `Consolidado` y del panel, y Playwright con axe-core.
- **Punta a punta:** un script nuevo, `deploy/probar_consolidado_en_revision.sh`. Arma el distributivo completo de 2026-I con la sugerencia de docentes, justifica las blandas, consolida, adjunta el informe y envía a revisión: es la primera vez que un script lo logra (F12-H05, T020). Después comprueba:
  - el 409 de cada operación;
  - que la consulta y la descarga siguen;
  - `enRevision`;
  - que nada dejó entrada.

  En la CI también cubre por primera vez el envío a revisión que pasa.
