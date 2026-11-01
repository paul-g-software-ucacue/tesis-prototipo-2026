# Research: Ver los totales del docente al instante

## 1. Una sola forma de contar (FR-005)

- **Decisión:** `HorasDelPeriodo` expone a su paquete cómo una propuesta se convierte en horas, y `CargaHoraria` usa eso mismo. Una prueba compara las dos cuentas.
- **Por qué:** si la sugerencia y el panel contaran distinto, el coordinador vería dos cifras del mismo docente.
- **Alternativas:** una segunda cuenta en `CargaHoraria`: es más corta y se puede desviar, por ejemplo con los paralelos retirados.

## 2. La proporción

- **Decisión:** `Proporcion.de(clase, otras)`, vacía si las dos son 0. El porcentaje de clase se redondea al entero más cercano, con los medios hacia arriba, y el de otras es 100 menos ese.
- **Por qué:** es dominio puro y siempre suma 100.

## 3. La meta (principio VI)

- **Decisión:** `GET /interno/normativa/periodos/{periodo}/proporcion`, con los parámetros `porcentaje_clase` y `porcentaje_otras_docencia` de RN-05 de la versión que rige el periodo. Sin versión, 404.
- **Por qué:** los parámetros ya están sembrados desde V2 y una versión nueva los copia.
- **Alternativas:** agregarlos a `pesos-de-afinidad`: mezcla dos reglas en una ruta.

## 4. Quién lee (D-82)

- **Decisión:** `consultarCargaHoraria` usa los permisos de `consultarActividades`, sin filtro de carrera.
- **Por qué:** el coordinador necesita saber si le caben horas a cualquier docente activo, y la sugerencia ya le muestra las horas de otras carreras (D-01). Las sumas no dicen en qué asignaturas ni actividades están.
- **Excepción a D-64:** esta consulta no filtra por la carrera del token. Por eso queda como decisión.

## 5. Guardar no cambia su respuesta

- **Decisión:** después de guardar, la pantalla vuelve a pedir la carga. `proponerDocente` y `asignarActividad` responden lo mismo que hoy.
- **Por qué:** si los totales fallan, la asignación ya se guardó y se puede decir así (FR-026). Tampoco se agrega una llamada a Normativa dentro de cada guardado.
- **Alternativas:** devolver los totales al guardar: ahorra una petición, pero un fallo de los totales parecería un fallo de guardar.

## 6. La lista de actividades

- **Decisión:** `ActividadesDelDocente.horasPorComponente` pasa a ser `totales`, con la clase y todas las carreras, sin horas contratadas ni proporción.
- **Por qué:** un docente inactivo sigue en la lista, y la consulta de horas contratadas solo trae a los activos. La columna solo necesita las sumas.
- **Consecuencia:** la lista ahora necesita la oferta. Si `datos` no responde, la pantalla muestra el mensaje de error, y ya no ve la lista (SC-006).
- **Alternativas:** una petición por docente: N llamadas por pantalla.

## 7. Docente

- **Decisión:** se busca en `docentesActivos()`, como en `ConsultarActividadesPosiblesServicio`. Si no está, `GENERACION_DOCENTE_NO_ACTIVO`.
- **Sin distributivo:** `GENERACION_SIN_DISTRIBUTIVO`. La pantalla no lo pide si sabe que no hay distributivo.
- **Periodo que no existe:** no tiene distributivo, así que responde `GENERACION_SIN_DISTRIBUTIVO`, como `consultarActividades`, que tampoco distingue los dos casos.

## 8. El exceso

- **Decisión:** `exceso = max(asignadas - contratadas, 0)`, junto a `disponibles`, que sigue con su mínimo de 0.
- **Por qué:** D-78 permite pasarse hasta F07-H02. Si no se muestra, un docente con 33 h de 20 parece solo «sin horas».

## 9. El panel

- **Decisión:** `comun/CargaHoraria.tsx`, con su consulta bajo la clave `['generacion', 'carga-horaria', periodo, docente]`. Guardar y quitar invalidan esa clave.
- **Dónde:** bajo el campo Docente del formulario; sobre la lista de actividades, con el último docente guardado; en el detalle desplegado de un candidato, solo al abrirlo; y en la propuesta del paralelo.
- **Por qué solo al abrirlo:** una sugerencia trae decenas de candidatos.

## 10. Las pruebas

- **Dominio:** sin dobles, `Proporcion`, `CargaHoraria` y la igualdad con `HorasDelPeriodo`.
- **Punta a punta:** `deploy/probar_totales.sh`, con un docente en dos carreras y una suma aparte, con la que debe coincidir el panel (SC-001 y SC-002).
