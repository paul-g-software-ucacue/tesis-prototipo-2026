# Research: Crear una versión nueva de la normativa (F04-H02)

Decisiones técnicas del plan. Cada sección tiene la decisión, por qué y las alternativas. Parte de lo que ya existe en `main` (`fec9c69`), hecho en F04-H01:
- el agregado `VersionNormativa` y sus objetos de valor (`TablaCargaHoraria`, `FilaCargaHoraria`, `Regla`), que hoy solo se reconstruyen;
- la versión 1 por migración (`V2__politica_2024.sql`), con `registro_cambio` y la acción `VERSION_CREADA`;
- la consulta `listarVersiones` y `consultarVersion` con `PARAMETROS:LECTURA`, y la pantalla «Normativa» de solo lectura;
- `NormativaContratoTest` comprueba que `POST` y `PUT` responden 405: esta historia lo reemplaza por las operaciones nuevas.

## 1. El agregado gana comportamiento (DDD táctico, principio II)

- **Decisión:** `VersionNormativa` sigue siendo inmutable (un `record`), y sus operaciones devuelven una versión nueva junto con lo que cambió:
  - **`copiaDe(origen, numero, datos)`:** crea una versión con el siguiente número, copiando la tabla, las reglas y el tipo de IST, con su código, su origen y su vigencia propios, y `copiadaDe` = número de origen. Registra el evento `VersionNormativaCreada` (modelo de dominio §6).
  - **`editada(cambios)`:** aplica a la vez los cambios de cabecera, de filas y de parámetros. Si la versión tiene un periodo, lanza `VersionEnUso`. Si un cambio agrega o quita una fila o un parámetro, lanza `CambioNoPermitido` (FR-002). Si una fila rompe sus invariantes, lanza `FilaInvalida` con su perfil, su tipo y el motivo. Devuelve la versión editada y la lista de `CambioDeValor` (campo, anterior y nuevo).
  - **`asociadaA(periodo)`:** agrega una `AsignacionNormativa`. Desde entonces, `enUso()` es verdadero y `editada` falla.
- **`AsignacionNormativa`**, entidad del agregado (modelo de dominio §5.1): el código del periodo y cuándo se asoció.
- **Por qué:** las reglas «no cambia en uso», «no se agregan filas» y «la fila cumple sus invariantes» viven en el dominio y se prueban sin dobles. La aplicación solo orquesta.
- **Alternativa:** un objeto mutable con métodos `set…`. Se descarta porque perdería la comparación «antes y después» que necesita el registro, y porque las invariantes de F04-H01 están en los constructores.

## 2. Una sola forma de cambiar una versión (FR-010)

- **Decisión:** `editarVersion` (`PUT /api/normativa/versiones/{numero}`) recibe la versión **completa**: la cabecera, todas las filas con sus horas y todas las reglas con sus parámetros. El dominio compara con la versión guardada y aplica las diferencias.
- **Por qué:**
  - la carga 7 por archivo (F01, MD-13) también trae la tabla completa, así que la pantalla y el archivo usan el mismo caso de uso y las mismas reglas;
  - un cambio de varias filas se guarda entero o no se guarda (SC-004).
- **Validación de la forma (FR-002):** las filas y los parámetros que llegan tienen que ser exactamente los de la versión: mismos pares perfil y tipo, y mismas reglas con los mismos nombres. Uno de más o de menos responde 400 con el detalle.
- **Alternativa:** una operación por celda (`PATCH` de una fila). Es más fina para la pantalla, pero la carga 7 necesitaría otra operación, y un cambio de varias filas no sería atómico.

## 3. Numeración y concurrencia

- **Número:** el siguiente al más alto (`max + 1`), dentro de la transacción de la creación y con un candado de PostgreSQL de la transacción (`pg_advisory_xact_lock`). Dos creaciones simultáneas obtienen números distintos (caso límite), sin reintentos.
- **Edición y asociación:** las dos leen la fila de la versión con `SELECT … FOR UPDATE`. Así, una edición no puede colarse después de una asociación: la que llega segunda ve el estado de la primera. Entre dos ediciones, gana la última (supuesto de la especificación) y las dos quedan en el registro.
- **Código único:** restricción `unique` en la base y la regla en la aplicación. Un código repetido responde 409 `NORMATIVA_CODIGO_REPETIDO`.

## 4. Asociar una versión a un periodo (FR-006)

- **Decisión:** Normativa ofrece:
  - `asociarPeriodo` (`PUT /api/normativa/periodos/{periodo}`, cuerpo `{ "version": n }`);
  - `consultarPeriodo` (`GET /api/normativa/periodos/{periodo}`), que dice qué versión rige el periodo y responde 404 si no tiene.
- La tabla `asignacion_normativa` (`periodo` único) garantiza que un periodo tenga una sola versión. Asociarle otra responde 409 `NORMATIVA_PERIODO_CON_VERSION`; asociar otra vez la misma versión no cambia nada y responde 200.
- **Puerto de entrada publicado:** `AsociarVersionAPeriodo` queda en `ec.mdd.normativa.puertos.entrada`. Académico, que vive en el mismo proceso `datos`, lo usará en F02-H01 por la interfaz publicada (principio V), como Usuarios usa Roles.
- **El periodo es un código** (`^[A-Za-z0-9._-]{1,20}$`, por ejemplo `2025-1`). Normativa no comprueba que exista: es dato de Académico, y un servicio no lee las tablas de otro.
- Las excepciones que lanzan los puertos publicados (`CodigoRepetido`, `PeriodoConVersion`, `VersionNoEncontrada`) están en `puertos.entrada`, junto al puerto que las declara, para que Académico (F02-H01) y Cargas (F01) las usen sin romper `ModulosTest`.
- **Alternativa:** dejar solo la regla y probar el 409 con filas insertadas en la prueba. El escenario «Versión en uso» no se vería de punta a punta hasta F02-H01.

## 5. Errores (ADR-016)

| Código | Estado | Cuándo |
|---|---|---|
| `NORMATIVA_VERSION_EN_USO` | 409 | Editar una versión que rige un periodo. El mensaje pide crear una versión nueva a partir de ella. |
| `NORMATIVA_CODIGO_REPETIDO` | 409 | Crear o renombrar una versión con el código de otra. |
| `NORMATIVA_PERIODO_CON_VERSION` | 409 | Asociar a un periodo que ya tiene otra versión. |

- Una fila que rompe sus invariantes, o un cambio que agrega o quita filas o parámetros, responde 400 `COMUN_SOLICITUD_INVALIDA` con un detalle por campo (`SolicitudInvalida`, F05-H02).
  - **Campo:** `tablaCargaHoraria[DOCENTE_MT/TODOS]` o `reglas[RN-02].horas_maximas_docente`.
  - **Descripción:** en español, sin códigos internos. Por ejemplo: «Las horas de clase (13) no están entre el mínimo y el máximo (8 a 12)».
- `FilaInvalida` hereda de `IllegalArgumentException`, para que las pruebas de F04-H01 sigan iguales. Lleva aparte el perfil, el tipo y el motivo.
- `NormativaControlador` captura `FilaInvalida` y `CambioNoPermitido` y lanza `SolicitudInvalida`, con un `DetalleRespuesta` por campo, como `RolesControlador`. El campo es `tablaCargaHoraria[<perfil>/<aplicaA>]` o `reglas[<RN-xx>].<parametro>`.
- `ErroresNormativa` registra los tres códigos, y una prueba los compara con `errores.md`.

## 6. Registro (FR-009, principio VII)

- `registro_cambio` gana las acciones `VERSION_EDITADA` y `VERSION_ASOCIADA`:
  - en **`VERSION_CREADA`**, el detalle lleva `copiadaDe`;
  - en **`VERSION_EDITADA`**, el detalle lleva cada cambio con su campo, su valor anterior y su valor nuevo;
  - en **`VERSION_ASOCIADA`**, el detalle lleva el periodo.
- **Usuario:** el de la sesión (`sub` del token). La versión 1 sigue con `sistema`.
- **El evento `VersionNormativaCreada`** se guarda como `VERSION_CREADA`. Todavía no se publica a otros procesos, porque no tiene consumidor (principio X; F04-H01, research §5).

## 7. Quién escribe la normativa: D-67

- **Decisión:** el Vicerrector académico gana `PARAMETROS:ESCRITURA`, con la migración `V5__vicerrector_escribe_normativa.sql` de Roles. `MatrizD16IntegrationTest` actualiza su fila.
- **Por qué:**
  - la historia la pide él;
  - D-16 le da los ajustes a la propuesta y aprobar y congelar;
  - es quien abre el periodo y elige su versión (F02-H01).
- **Alternativa:** solo el Administrador (D-65 tal cual). El Vicerrector pediría cada cambio de la norma a sistemas.

## 8. Contrato (`contratos/normativa/openapi.yaml`)

| Operación | Ruta | Permiso | Respuestas |
|---|---|---|---|
| `crearVersion` | `POST /api/normativa/versiones` | `PARAMETROS:ESCRITURA` | 201 · 400 · 404 (la de origen no existe) · 409 `NORMATIVA_CODIGO_REPETIDO` |
| `editarVersion` | `PUT /api/normativa/versiones/{numero}` | `PARAMETROS:ESCRITURA` | 200 · 400 · 404 · 409 `NORMATIVA_VERSION_EN_USO` o `NORMATIVA_CODIGO_REPETIDO` |
| `asociarPeriodo` | `PUT /api/normativa/periodos/{periodo}` | `PARAMETROS:ESCRITURA` | 200 · 400 · 404 (la versión no existe) · 409 `NORMATIVA_PERIODO_CON_VERSION` |
| `consultarPeriodo` | `GET /api/normativa/periodos/{periodo}` | `PARAMETROS:LECTURA` | 200 · 404 |

- `VersionResumen` y `VersionNormativa` ganan `copiadaDe` (opcional) y `periodos` (los códigos de los periodos que rige; vacía si ninguno).
- Las operaciones con cuerpo validan su forma en el contrato; las invariantes, en el dominio.

## 9. La pantalla «Normativa» (FR-007)

- **Crear versión nueva:** un botón sobre la versión elegida. Abre un diálogo con código, origen y vigencia, y crea la copia.
- **Editar:** solo en una versión sin periodo y con escritura.
  - La tabla y los parámetros pasan a campos numéricos; la cabecera, a campos de texto y de fecha.
  - «Guardar» envía la versión completa.
  - Un 400 marca cada fila o parámetro con su motivo, y un 409 se muestra con su título y su descripción.
- **Asociar a un periodo:** un diálogo con el código del periodo.
- **Una versión con periodos** muestra «Rige: 2025-1» y queda en solo lectura, con el botón de crear una versión nueva a partir de ella.
- Sin `PARAMETROS:ESCRITURA` no aparece ninguna acción.

## 10. Glosario (principio III)

- «Asignación normativa» está en el modelo táctico (§5.1, `AsignacionNormativa`), pero no en el glosario §7.3. Además, «Asignación» sola son las horas de un docente (§8).
- **Decisión, aprobada por el autor el 2026-10-18 y ya aplicada:** se agregó al glosario §7.3 (`sdd/modelo_dominio.md`, fuera del repositorio) «Asignación normativa: el periodo, nombrado por su código, al que se asocia una versión de normativa; un periodo tiene una sola. No es una asignación de horas.». Se usa siempre completa.
- En las rutas y la pantalla se dice «periodo» (`/api/normativa/periodos/{periodo}`, «Rige: 2025-1»), para no usar «asignación» sola.

## 11. Pruebas

- **Dominio, sin dobles:**
  - `copiaDe` copia todo y registra el evento;
  - `editada` aplica cambios, rechaza una fila inválida, rechaza agregar o quitar filas o parámetros y no cambia una versión en uso;
  - `asociadaA` deja la versión en uso;
  - la lista de `CambioDeValor`.
- **Aplicación, con Mockito:** crear (número, código repetido, origen inexistente), editar (en uso, código repetido, registro con los cambios) y asociar (periodo con otra versión, la misma otra vez, registro).
- **Adaptadores, con Testcontainers:**
  - el repositorio guarda una versión nueva y la reconstruye igual, valor por valor;
  - la edición atómica;
  - el número con dos creaciones simultáneas;
  - el `FOR UPDATE` entre edición y asociación;
  - la asignación única por periodo;
  - el registro;
  - y que la versión anterior arranque sobre el esquema con V3 (D-55).
- **Contrato:** las cuatro operaciones nuevas, `VerificadorDePermisos` con las seis y `ErroresNormativaTest`.
- **Roles:** `MatrizD16IntegrationTest` con la fila nueva del Vicerrector (D-67).
- **SPA:**
  - Vitest: crear, editar, el 400 con el motivo en cada fila, el 409, asociar, el solo lectura de una versión con periodo y las acciones ocultas sin escritura;
  - Playwright con axe en la edición.
- **Punta a punta:** `probar_normativa.sh` suma tres pasos:
  - `vicerrectorado.demo` crea la versión 2 a partir de la 1, asociada a un periodo de prueba, y cambia las horas de clase del docente de medio tiempo; la 1 sigue idéntica;
  - editar la versión en uso da 409;
  - asociar otra versión al mismo periodo da 409.

  Al final borra lo que creó, para poder repetirse (research §12).

## 12. Limpieza de las pruebas de punta a punta

- **Decisión:** `probar_normativa.sh` usa un periodo de prueba con un código aleatorio (`prueba-<aleatorio>`) y, al terminar, borra en la base del perfil, con `psql` como en el paso 8 de `probar_autenticacion.sh`:
  - la asignación de ese periodo y su registro `VERSION_ASOCIADA`;
  - solo las versiones cuyo número anotó al crearlas (`versiones_creadas`), con sus filas, reglas y parámetros; nunca una versión que no creó;
  - sus registros.

  La limpieza va en un `trap … EXIT`, así corre aunque falle un paso. La API no ofrece borrar versiones: el principio VII no lo permite.
- **Pasos 1 y 4:** dejan de exigir «una sola versión». Comprueban que la versión 1 existe, es la primera de la lista y es idéntica a la de la migración sin el campo `periodos` (que cambia al asociarla), así el script pasa aunque haya otras versiones (por ejemplo, las que el autor crea a mano en el perfil local).
- **Por qué:** el script se repite en local y en la CI sobre la misma base, y la versión 1 tiene que quedar siempre como la dejó la migración.
