# Research: Avanzar el distributivo según el rol de la etapa

Cada sección responde una pregunta del plan con la decisión, el porqué y las alternativas.

## 1. Dónde vive el proceso de aprobación

**Decisión.** En el servicio 06, Validación OCS (contexto Aprobación), como dice el issue y el modelo de dominio (`ProcesoAprobacion`, MD-04). Corre en el proceso `distributivo`, junto con Generación. Tablas nuevas en el esquema `validacionocs`: `proceso_aprobacion`, `parcial_del_proceso` y `transicion`.

**Por qué.** MD-04: el estado del distributivo es de Aprobación; Distributivo (Generación) solo conoce el estado de la versión y de cada parcial, que Aprobación le ordena cambiar. Validación OCS ya guarda la configuración del flujo (F12-H01).

**Alternativas.** El estado en Generación: crearía el ciclo que MD-04 evita, porque Generación tendría que conocer las etapas, los roles y la configuración.

## 2. Cómo se llaman Aprobación y Generación

**Decisión.** Validación OCS llama a Generación por su interfaz de aplicación (`ec.mdd.generacion.puertos.entrada`, `@NamedInterface("api")`), como Usuarios llama a Roles en `identidad`. Generación publica una interfaz nueva, `ParcialesDelDistributivo`, con:
- `estadoDelDistributivo(periodo)`: la versión vigente, su número, cuándo se creó el distributivo y el estado de cada parcial;
- `marcarParcialEnviado(periodo, carrera)`: bloquea la versión, marca el parcial como enviado y devuelve la versión y el hash de su contenido (§4). Es idempotente: si ya estaba enviado, devuelve lo mismo sin cambiar nada.

Generación no depende de Validación OCS: el sentido es uno solo (principio V, MD-04).

**Transacciones (principio V: una por servicio).** Enviar un parcial son dos transacciones, en este orden:
1. Generación marca el parcial y calcula el hash, en la suya.
2. Validación OCS registra la transición y, si era el último, el paso de etapa, y publica las entradas de la bitácora, en la suya.

Si la segunda falla, el parcial queda enviado sin su transición. El usuario recibe el error y vuelve a enviar: como la primera es idempotente, la segunda registra la transición con el mismo hash. La pantalla lo muestra como «enviado, falta confirmar», hasta que se registra la transición (el estado de Aprobación manda).

**Alternativas.** Una transacción entre los dos servicios: prohibida por el principio V. Un evento en el proceso para que Generación marque el parcial después: el parcial quedaría editable unos instantes después del envío, y el hash podría no ser el del contenido que quedó.

## 3. Una sola entrada por transición (D-95)

**Decisión.** La bitácora recibe la transición de Aprobación, con su acción (`PARCIAL_ENVIADO` o `ETAPA_AVANZADA`), y Generación no registra aparte el cambio de estado del parcial (D-95). Validación OCS ya publica en la bitácora (`CambioDeFlujo`, F17-H01); suma las dos acciones.

**Contenido de la entrada:**
- `objeto`: `periodo`, `carrera` (en un envío), `version` y `hash`;
- `anterior`: `{ "estado": "BORRADOR_CARRERA", "parcial": "EN_ELABORACION" }` en un envío, o `{ "estado": "<etapa>" }` en un paso;
- `nuevo`: `{ "estado": "BORRADOR_CARRERA", "parcial": "ENVIADO" }`, o `{ "estado": "<siguiente>" }`.

Así quién (`usuario`), cuándo (`momento`), desde qué estado (`anterior`), hacia cuál (`nuevo`), la versión y el hash (`objeto`) quedan como pide el issue.

## 4. El hash del contenido aprobado (Q1 de la especificación)

**Decisión.** SHA-256, en hexadecimal, de una forma fija del contenido, que calcula Generación (dueña de los datos):
- **parcial:** un JSON canónico (claves ordenadas, sin espacios, UTF-8) con el periodo, la carrera, la versión, sus propuestas ordenadas por asignatura y paralelo (docente, tipo y texto de la justificación) y sus actividades ordenadas por docente, actividad y proyecto (horas con dos decimales);
- **distributivo** (al pasar de etapa): SHA-256 del JSON canónico con el periodo, la versión y la lista ordenada por carrera de `{carrera, hash}` de cada parcial.

Una prueba de dominio fija el hash de un parcial de prueba, y otra comprueba que cambiar un solo dato lo cambia (SC-006).

**Por qué.** Identifica exactamente qué se aprobó sin generar un PDF (F13-H02). Quién y cuándo (`propuestaPor`, `asignadaEn`) no entran: no son contenido (como en D-94).

**Alternativas.** El hash del PDF: llega con F13-H02. Un hash de la fila en la base: depende del almacenamiento.

## 5. La configuración con la que empieza el distributivo (FR-003)

**Decisión.** El proceso guarda una copia de las etapas (tipo, activa y rol) al crearse. Se crea la primera vez que se lo necesita (consulta o envío), con la configuración que regía **cuando se creó el distributivo**: la última carga de `configuracion_flujo` con `cargada_en` anterior a la creación del distributivo (que Generación informa), o la configuración por defecto si no había.

**Por qué.** `configuracion_flujo` es solo de inserción (F12-H01), así que la configuración vigente en un instante se puede reconstruir sin un evento de Generación a Validación OCS. También cubre los distributivos creados antes de esta historia (los del equipo de demostración).

**Alternativas.** Un evento `DistributivoCreado` dentro del proceso: obligaría a Generación a publicar para Validación OCS, y no cubre los distributivos que ya existen. Leer la configuración vigente en cada operación: rompe FR-003.

**Un distributivo que ya no existe.** El proceso guarda el `creadoEn` del distributivo. Si el del periodo es otro (un script de `deploy/` borró el distributivo y creó uno nuevo), el proceso anterior se reemplaza por uno nuevo, con sus transiciones. En la aplicación no se borra un distributivo, así que solo pasa en pruebas, y los scripts además borran las filas al terminar.

**Riesgo residual.** Si cambia el archivo `flujo-por-defecto.yaml` entre versiones del sistema, un distributivo sin carga se crearía con el nuevo. Es aceptable: es un cambio de código, no de una configuración del periodo.

## 6. Qué carreras cuentan (US2)

**Decisión.** Los parciales de la versión vigente: Generación crea uno por cada carrera con oferta vigente al crear el distributivo (`VersionDistributivo.primera`), y agrega uno si una carrera recibe su primera propuesta después. El distributivo pasa de etapa cuando **todos** los parciales de la versión están enviados.

**Por qué.** Generación ya los crea; no hace falta otra lista. Ajusta el supuesto de la especificación («una carrera con oferta y sin parcial»): no se da, porque el parcial existe desde que se crea el distributivo.

## 7. Quién puede

**Decisión.**
- **Enviar:** `x-mdd-permiso: OFERTA:APROBACION` («Envía su borrador» en la matriz D-16, solo el Coordinador de carrera, con alcance propio). Además, el dominio exige que el usuario tenga el rol de la etapa actual según la copia de la configuración (`ROLE_<rol>` del token), y su carrera (`AlcanceDeCarrera`, D-64). Si no, `AccessDeniedException` → `SEGURIDAD_ACCESO_NO_AUTORIZADO`.
- **Avance genérico:** cualquier sesión válida llega a la operación; el dominio exige el rol de la etapa actual. Sin él, 403. En una etapa con operación propia, 409 `VALIDACIONOCS_ETAPA_CON_OPERACION_PROPIA` (Q2 de la especificación).
- **Consultar el estado:** `DISTRIBUTIVO:LECTURA`, como el resto del distributivo.

**Por qué.** El permiso da acceso a la operación (ADR-021); el rol de la etapa es una regla del dominio que puede cambiar por periodo (RF-34). Los dos dan el mismo 403, sin decir qué falta.

## 8. Dos envíos a la vez (SC-004)

**Decisión.** Validación OCS bloquea la fila del proceso (`SELECT … FOR UPDATE`) al registrar un envío. El segundo de dos envíos simultáneos espera, ve los parciales que faltan y, si es el último, hace el paso de etapa. El paso ocurre una vez, porque solo una transacción encuentra «todos enviados» con la etapa todavía en BORRADOR_CARRERA.

**Alternativas.** Una restricción única sobre la transición de paso: detecta el doble paso pero devuelve un error a un usuario que no hizo nada mal.

## 9. La pantalla

**Decisión.** Un componente común `EstadoDelFlujo` (etapa actual, parciales enviados y el botón de la acción que corresponde al usuario), en:
- **Oferta del periodo:** el coordinador ve el estado de su parcial y «Enviar parcial», con una confirmación que dice que ya no podrá cambiarlo;
- **Consolidado del periodo:** el Responsable y el Vicerrector ven la etapa y qué carreras enviaron;
- **Proyectos:** el Gestor de unidad ve «Avanzar» cuando el distributivo está en BORRADOR_UNIDADES.

**Por qué.** Cada rol ve la acción en la pantalla donde ya trabaja.

## 10. Errores nuevos

| Código | Estado | Cuándo |
|---|---|---|
| `VALIDACIONOCS_SIN_DISTRIBUTIVO` | 404 | El periodo no tiene distributivo |
| `VALIDACIONOCS_ETAPA_NO_ADMITE_ENVIO` | 409 | El distributivo ya no está en BORRADOR_CARRERA |
| `VALIDACIONOCS_PARCIAL_YA_ENVIADO` | 409 | El parcial ya tiene su transición de envío |
| `VALIDACIONOCS_ETAPA_CON_OPERACION_PROPIA` | 409 | Avance genérico en una etapa con su historia |
| `VALIDACIONOCS_PARCIAL_NO_ENCONTRADO` | 404 | La carrera no tiene parcial en el distributivo (sin oferta en el periodo); agregado al implementar |
| `VALIDACIONOCS_PERIODO_CERRADO` | 409 | El periodo está cerrado |

Cada uno con sus 8 columnas en `contratos/comun/errores.md` (principio XIII). Un parcial enviado que se intenta editar ya responde `GENERACION_PARCIAL_NO_EDITABLE`.

## 11. La demostración

**Decisión.** Un script nuevo, `deploy/probar_avance.sh`, en `perfil-consolidado` después de `probar_consulta_bitacora.sh`: envía los parciales de todas las carreras con sus coordinadores (en la demo, un solo coordinador por carrera; el script usa uno y le cambia la carrera para cada una), comprueba la etapa, la bitácora, el 403 de otro rol y el envío doble simultáneo.
