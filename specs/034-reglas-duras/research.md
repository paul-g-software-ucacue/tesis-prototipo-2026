# Research: Impedir una asignación que viola una regla dura

## 1. La regla (FR-001, FR-007, FR-011 a FR-013)

- **Decisión:** `MotorReglas.evaluar(antes, despues, horasContratadas, conProyecto, reglas)`, una función pura. Una asignación viola una regla solo si el docente queda sobre el límite **y** sube frente a lo que tenía.
- **Por qué:** una sola condición cubre todos los casos: llegar al límite, reemplazar, reducir a un excedido, quitar y volver a guardar lo mismo.
- **Orden:** primero RN-02, después RN-13 (spec, Assumptions).
- **Devuelve:** `Optional<Inconsistencia>`, con la regla y las horas que le quedan. F07-H03 la extenderá con las blandas.
- **Alternativas:** dos comprobaciones por caso en cada servicio: duplica la regla.

## 2. El antes y el después

- **Decisión:** `VersionDistributivo.conPropuesta(...)` y `conActividad(...)` devuelven las asignaciones con el cambio aplicado. `CargaHoraria.calcular` da las dos cargas.
- **Por qué:** se reutiliza la única forma de contar (F07-H01, FR-004). Reemplazar queda resuelto, porque la nueva sustituye a la anterior.

## 3. Las cifras (principio VI)

- **Decisión:** `GET /interno/normativa/periodos/{periodo}/reglas-de-asignacion`, con `horasMaximasDocente` (RN-02) y `horasClaseConProyecto` (la fila `DOCENTE_INVESTIGACION_VINCULACION`, `horas_clase_maximo`).
- **Sin una cifra:** Normativa falla y Generación responde con el error entre procesos: no se guarda (FR-025).
- **Alternativas:** escribir 16 y 40 en Generación: rompe el principio VI.

## 4. Qué proyectos activan el tope

- **Decisión:** `INVESTIGACION` y `VINCULACION`, como constante del motor.
- **Por qué:** `Regla` dice que las cifras y la severidad son datos, y la lógica de la comprobación está en el código. Es el texto de RN-13 y una decisión del autor.

## 5. Las dos fuentes del docente

- **Al guardar:** `proyectosYCargos` de F06-H04, de un docente.
- **En la sugerencia:** `GET /interno/docentes/con-proyecto?periodo=`, con los docentes y los tipos de sus proyectos asignables. Sin esto serían decenas de llamadas por sugerencia.

## 6. Dos a la vez (D-83)

- **Decisión:** dentro de la transacción, `bloquearVigente(periodo)` hace `SELECT … FOR UPDATE` sobre la versión y vuelve a leer sus asignaciones. El motor corre ahí, y después se guarda. Lo demás se consulta antes, sin transacción abierta.
- **Por qué:** el segundo espera milisegundos, ve lo del primero y recibe el error de horas (FR-016), no un conflicto técnico.
- **Frente a MD-01:** MD-01 pide un agregado con bloqueo optimista. Con un bloqueo optimista el segundo recibe un conflicto, y habría que reintentar para dar el error de horas: más código y una columna nueva. El agregado se mantiene, y cambia cómo se protege. Por eso es una decisión (D-83).
- **Docentes distintos:** esperan su turno unos milisegundos, y los dos se guardan (FR-017). Con 7 carreras y guardados de milisegundos no se nota.
- **Quitar:** no bloquea: solo baja horas.
- **Alternativas:** bloqueo por docente con un candado de sesión: más preciso, pero con un concepto nuevo para un caso que no lo necesita. Una columna de versión con reintentos: lo de MD-01, descartado arriba.

## 7. El límite

- **Decisión:** `ReglasDelPeriodo.limite(contratadas)`, el menor entre las horas contratadas y el tope. Lo usan el motor, la sugerencia y la carga horaria.
- **Por qué:** hoy las horas contratadas llegan hasta 40, así que coinciden. Con una normativa de menos horas, panel, sugerencia y guardado tienen que decir lo mismo (SC-003).

## 8. El mensaje (FR-018, FR-019)

- **Códigos:** `GENERACION_HORAS_EXCEDIDAS` y `GENERACION_TOPE_DE_CLASE`, los dos 409.
- **Las horas que quedan:** en `detalles`, con el campo `horas`, porque el texto del catálogo es fijo. `EntradaCatalogo` ya admite detalles.
- **Contrato común:** `detalles` deja de ser «solo en errores de entrada».

## 9. La sugerencia

- **Decisión:** `Candidato` agrega `motivo`: vacío si se puede proponer, `HORAS` o `TOPE_DE_CLASE`. Sale del mismo motor, con la carga del candidato antes y después (SC-003).
- **El orden:** usa `motivo` en lugar de `tieneHorasPara`.
- **`horasSuficientes`:** sigue en la sugerencia y en las propuestas, porque un dato anterior puede estar excedido. Sale de `ActividadGuardada`, porque guardar ya garantiza que alcanzan.

## 10. La pantalla

- **Sugerencia:** sin botón «Proponer», con el motivo.
- **Actividades:** se quita el aviso de horas. El error se ve en el formulario, que sigue abierto (FR-022), como cualquier fallo de `useEnvio`.

## 11. Las pruebas

- **Motor:** sin dobles, tabla de casos.
- **Concurrencia:** dos hilos contra PostgreSQL, en `integrationTest`, y 20 pares de intentos en el script (SC-002).
- **Ya existentes:** las que guardan sin horas cambian: ahora esperan 409. `deploy/probar_propuestas.sh` y `deploy/probar_actividades.sh` también.
