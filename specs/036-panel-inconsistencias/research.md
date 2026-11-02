# Research: Ver el panel de inconsistencias globales

## 1. Dónde se evalúa (FR-001)

- **Decision:** `MotorReglas.evaluarVersion(...)`, dominio puro, llamado desde `ConsultarPanelServicio`.
- **Rationale:** el modelo táctico lo dice: el motor evalúa «la versión completa al consolidar».
- **Alternatives:** una clase aparte partiría el motor.

## 2. Calcular o guardar (FR-001)

- **Decision:** calcular al abrir. Solo se guardan las justificaciones.
- **Rationale:** el panel cambia con cualquier propuesta. Una foto se quedaría vieja, y habría que invalidarla.
- **Costo:** unas 5 llamadas entre procesos por apertura, medidas en SC-002.

## 3. La inconsistencia global

- **Decision:** `InconsistenciaGlobal(tipo, regla, severidad, objeto, datos)`. `Inconsistencia`, la de una asignación, no cambia.
- **Rationale:** tienen datos distintos, y `Inconsistencia` lleva `horasQueQuedan`. El glosario ya dice que una inconsistencia es de una asignación o global.
- **Tipo:** RN-05 tiene dos motivos, y el mismo docente puede incumplir los dos. La clave es `tipo` y `objeto`, no `regla`: `PROPORCION` y `ACTIVIDADES_OBLIGATORIAS` se justifican por separado.

## 4. La indicación (FR-017, FR-018)

- **Decision:** el dominio entrega hechos: tipo y datos. El adaptador arma el texto, y entrega una `pantalla` del contrato. La SPA la traduce a una ruta y oculta el enlace sin permiso.
- **Rationale:** es lo que se hizo con la advertencia de F07-H03. La ruta de una pantalla no es del servidor.

## 5. Las justificaciones (FR-020 a FR-023)

- **Decision:** tabla solo de inserción. La vigente es la última de cada `(version_id, tipo, objeto)`.
- **Rationale:** principio VII: cambiar una no borra la anterior. El registro de cambios guarda quién y cuándo, sin el texto, como RN-19.
- **Alternatives:** actualizar la fila perdería el texto anterior.
- **Sin bloqueo:** dos a la vez insertan dos filas, y vale la última (edge case).

## 6. La validación al justificar (FR-021, FR-024, FR-025)

- **Decision:** el servidor recalcula el panel antes de guardar y comprueba que la inconsistencia existe, está en la lista y es blanda. Fuera de la transacción: no se abre una transacción durante una llamada a otro proceso (D-83).
- **RN-19:** no se justifica aquí. Es de la propuesta (FR-025). `EXCEPCION_DE_AFINIDAD` no se acepta como tipo.

## 7. Normativa (principio VI)

- **Decision:** ruta nueva `reglas-del-panel`, con severidades, meta, margen, rango y actividades obligatorias.
- **Rationale:** `reglas-de-asignacion` se llama al guardar cada asignación. No conviene hacerla crecer con lo que solo usa el panel.
- **Migración:** `V6` agrega `margen_puntos = 5` a RN-05 y `obligatoria` a `140-b` y `140-g` en cada versión, también las que rigen un periodo, porque completa la versión y no cambia un valor (D-68). `RepositorioVersionesJdbc` copia `obligatoria`.
- **Tipo de IST:** Tipo 2 fijo en el prototipo. El rango se lee de `t2_minimo` y `t2_maximo`.

## 8. Docentes y Académico

- **Cargos:** ruta nueva `cargos-vigentes`, porque `proyectos-y-cargos` es por docente y serían 47 llamadas. Devuelve docente, tipo y carrera.
- **Gestores:** Generación cuenta los tipos que empiezan por `G`, G06 a G21 (D-73).
- **RN-18:** `ParaleloDeLaOferta` agrega `horasDeLaAsignatura`. Generación compara: el dato ya existe en Académico (`porEncimaDeLaAsignatura`), y esta comparación es la regla.

## 9. Los permisos (FR-026, FR-027)

| Operación | Permiso | Quién |
|---|---|---|
| Ver | `CONSOLIDACION:LECTURA` o `DISTRIBUTIVO:ESCRITURA` | Consolidación, Vicerrectorado y Administrador |
| Justificar | `CONSOLIDACION:ESCRITURA` | Consolidación |

- **Rationale:** el Vicerrector no tiene `CONSOLIDACION:LECTURA`. `DISTRIBUTIVO:LECTURA` lo tienen también el Rector, el OCS y la Secretaría. Sin cambiar la matriz D-16, esta combinación es la más cercana a lo que decidió el autor.
- **Administrador:** tiene `CONSOLIDACION:LECTURA` por D-16, así que lee el panel. Cambiar eso exige cambiar la matriz.

## 10. El registro

- **Decision:** acción nueva `INCONSISTENCIA_JUSTIFICADA`, con 26 caracteres. `registro_cambio.accion` es `varchar(20)`: `V5` lo amplía a 30.

## 11. Las pruebas

- El motor, tipo por tipo, con esqueleto y en rojo primero.
- `probar_panel.sh` usa los datos de prueba y compara el panel con un conteo hecho aparte desde la base (SC-001).
- La versión de prueba necesita el margen: las versiones nuevas lo heredan al copiarse.
