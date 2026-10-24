# Research: Mantener la oferta del periodo (F03-H03)

Decisiones de diseño de la historia, con su porqué y lo que se descartó. Parte de la spec y de sus tres aclaraciones del 2026-10-24, del diccionario de datos (carga 5) y de lo que ya hay en Académico: los periodos (F02-H01), la estructura orgánica (F03-H01) y las carreras, mallas y asignaturas (F03-H02).

No quedan puntos abiertos: las tres preguntas de la spec las respondió el autor antes de redactarla.

## 1. Profundidad del modelo (principio II)

- **Decisión:** como en F03-H02, valores con sus reglas y un valor para las reglas entre entidades. `AsignaturaParalelo` es un *record* con las reglas de cada campo; `OfertaDelPeriodo` reúne el periodo, sus asignaturas-paralelo y los `DatosMaestros` de F03-H02, y decide registrar, corregir, retirar y volver a ofertar, y las horas a cubrir.
- **Por qué:** Académico es de soporte. Las reglas que miran a otra entidad (el estado del periodo, la asignatura activa, la carrera vigente, la jornada de la carrera, la sede, las horas de la asignatura) necesitan esos datos juntos, y `DatosMaestros` ya los tiene.
- **Descartado:** un agregado `Oferta` con eventos; no hay invariante que lo pida más allá de la clave única, que la base también garantiza.

## 2. Qué se guarda (FR-001)

- **Decisión:** una tabla `asignatura_paralelo` con clave `(periodo, asignatura, paralelo)` y referencias al periodo, a la asignatura y, si se indica, a la sede. Las horas indicadas se guardan **solo si se indican** (`null` = las de la asignatura); la sede, igual (`null` = la de la carrera). Estado `OFERTADA` o `RETIRADA`.
- **Por qué:** la spec dice que la que toma las horas de su asignatura las sigue si la asignatura se corrige; guardar una copia rompería eso. La consulta devuelve siempre las horas efectivas, si son indicadas y las de la asignatura.
- **Franjas horarias:** no se guardan (RF-60 es Should; F22).
- **Descartado:** copiar las horas y la sede al registrar; obligaría a recalcular al corregir una asignatura o una carrera.

## 3. Las reglas (FR-003 a FR-005)

En `AsignaturaParalelo`, sin dobles:
- `paralelo`: `^[0-9A-Z_-]{1,20}$`; `estudiantes`: entero ≥ 0 (máximo 9999, la longitud 4 del diccionario); `horas` indicadas: de 0,5 a 20, con decimales de punto.

En `OfertaDelPeriodo`, con `DatosMaestros`:
- el periodo, en **Planificación** para registrar, corregir, retirar o volver a ofertar (aclaración 2) → `OfertaCerrada` (409);
- la asignatura existe y está **activa**, y su carrera está **vigente** → 400 en `asignatura`; la malla puede estar derogada (spec, supuestos);
- la jornada es una de las de su carrera → 400 en `jornada`;
- la sede indicada es una sede matriz, sede, extensión o campus activa → 400 en `sede` (la misma regla que la sede de una carrera, F03-H02);
- las horas indicadas no pasan las de su asignatura (RN-18, aclaración 3) → 400 en `horasSemanales`;
- la misma asignatura y paralelo en el periodo, aunque esté retirada → `AsignaturaParaleloRepetida` (409); retirada, se vuelve a ofertar con su estado.
- **Volver a ofertar** vuelve a comprobar todo, como corregir; **retirar** solo el estado del periodo.
- **Horas a cubrir:** por asignatura, la suma de las horas efectivas de sus paralelos ofertados; por carrera, la de sus asignaturas; del periodo, la de todas. Las retiradas se muestran y no suman (FR-007). Una asignatura-paralelo con horas indicadas por encima de las de su asignatura (porque la asignatura se corrigió después) se marca en la consulta.

## 4. Concurrencia

- **Decisión:** cada cambio bloquea la fila del periodo (`RepositorioPeriodos.bloquear`, ya existe), las tablas de carreras (`RepositorioCarreras.bloquear`, ya existe, que también impide inactivar la asignatura a la vez) y la tabla `asignatura_paralelo`, lee, decide con el dominio y guarda, en una transacción.
- **Por qué:** con 165 filas por periodo y pocos usuarios, serializar los cambios de datos maestros cuesta poco y deja las reglas en el dominio. La clave primaria respalda la regla del repetido.

## 5. Quién mantiene y quién lee (aclaración 1, D-70)

- **Escritura:** `[CATALOGOS:ESCRITURA, DISTRIBUTIVO:ESCRITURA]`, como las carreras: el Administrador y el Vicerrector.
- **Lectura:** `[OFERTA:LECTURA, DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`. Lee el Responsable de consolidación (`OFERTA:LECTURA`, D-65) y, como `AutoridadesDelToken` quita el alcance `:PROPIO`, también el coordinador, que verá la oferta de todas las carreras, igual que hoy ve las carreras y la estructura. El filtro por lo propio llega con F03-H05.
- Sin decisiones nuevas; la matriz no cambia.

## 6. Registro (principio VII)

- `registro_cambio` gana 4 acciones: `ASIGNATURA_PARALELO_REGISTRADA`, `ASIGNATURA_PARALELO_CORREGIDA`, `ASIGNATURA_PARALELO_RETIRADA` y `ASIGNATURA_PARALELO_REOFERTADA` (caben en los 30 caracteres de `accion`). El detalle lleva `periodo`, `asignatura` y `paralelo`, y en la corrección cada cambio con su valor anterior y el nuevo (`Detalles.de`). Corregir sin cambios no registra.

## 7. El contrato (principio IV)

- 5 operaciones con el tag `Oferta`, bajo el periodo: consultar la oferta, ofertar, consultar, corregir y cambiar el estado de una asignatura-paralelo ([contracts/academico.md](contracts/academico.md)).
- La consulta de la oferta trae las asignaturas-paralelo y los totales ya sumados, y dice si la oferta se puede cambiar (`modificable`), para que la pantalla no repita la regla del estado del periodo.
- 2 códigos nuevos en `contratos/comun/errores.md`: `ACADEMICO_ASIGNATURA_PARALELO_REPETIDA` y `ACADEMICO_OFERTA_CERRADA` (409). El periodo o la asignatura-paralelo que no existe: 404 común.
- **Descartado:** rutas propias `/api/academico/oferta?periodo=`; la oferta es del periodo y la ruta lo dice.

## 8. La pantalla «Oferta del periodo»

- Ruta `/oferta`, en el menú después de «Carreras y mallas», con la lista de lectura de §5.
- Un selector de periodo (por omisión, el más reciente en Planificación; si no hay, el más reciente). Un aviso dice por qué está en solo lectura: sin permiso de escritura, o el periodo ya no está en Planificación.
- El selector usa `listarPeriodos`, que exige `DISTRIBUTIVO:LECTURA`. Hoy todos los roles que leen la oferta lo tienen: el Administrador, el Vicerrector, el Responsable de consolidación y el coordinador. Si la lista responde 403, la pantalla lo dice y no se rompe.
- Por carrera, con su total de horas a cubrir: una tabla con una fila por asignatura (ciclo, código, nombre, sus paralelos con jornada, modalidad, estudiantes, horas y estado, y sus horas a cubrir). Arriba, el total del periodo.
- «Ofertar asignatura»: carrera vigente → asignatura activa → paralelo, jornada (las de la carrera), modalidad (la de la carrera por omisión), sede (por omisión «la de la carrera»), estudiantes y horas (vacío = las de la asignatura, que se muestran en la pista). Corregir, retirar y volver a ofertar, con confirmación al retirar. Los errores con su motivo junto a la pista, como dejó el PR #137.

## 9. Punta a punta

- **Decisión:** `deploy/probar_oferta.sh`, con el estilo de `probar_carreras.sh`. Registra con un prefijo `P` + 3 dígitos las 7 carreras, sus 12 mallas y las 300 asignaturas de prueba, y un periodo `<prefijo>2026-I` en Planificación; luego:
  1. el Vicerrector oferta las 165 asignaturas-paralelo de la carga 5; la oferta suma 660 horas y el total de cada carrera coincide con el de la carga (SC-002);
  2. una asignatura de 4 horas en 2 paralelos más un tercero suma 12; con uno retirado, 8, y vuelve a 12 al volver a ofertarlo (SC-003, escenario del issue);
  3. repetida, 409; horas por encima de las de la asignatura, jornada que no es de la carrera, asignatura inactiva: 400 con su campo;
  4. la consulta de una asignatura-paralelo trae los datos para la afinidad;
  5. con el periodo fuera de Planificación, cambiar la oferta da 409 `ACADEMICO_OFERTA_CERRADA`. El script pone el estado **en la base** (`EN_APROBACION`), porque salir de Planificación por la API exige abrir el periodo con una versión de Normativa, que ya prueba `probar_periodos.sh`;
  6. `consolidacion.demo` lee y recibe 403 al escribir; `docente.demo`, 403 al leer;
  7. el registro tiene las acciones.

  La limpieza borra en la base solo lo que creó: la oferta, las asignaturas, mallas y carreras, el periodo y sus registros, y lo que el registro del periodo dejó en Roles y en los eventos, como `probar_periodos.sh`.
- **Las pruebas del dominio** ofertan también las 165 asignaturas-paralelo de la carga 5, leídas de una copia en los recursos de prueba de Académico, sobre las cargas 02 y 04 que ya están allí.

## 10. Glosario (principio III)

Los términos de la historia ya están: Oferta, Asignatura-paralelo, Paralelo, Jornada, Modalidad y Periodo académico. Faltaban dos cosas que usa la spec; el autor las aprobó y quedaron aplicadas el 2026-10-24, antes del código (copia `modelo_dominio.md.antes-F03-H03`):

- **Asignatura-paralelo** (ampliar la definición): «… Es lo que recibe una propuesta. Se **retira** de la oferta en lugar de borrarse; retirada, no suma horas a cubrir ni recibe propuestas, y se puede volver a ofertar mientras el periodo está en Planificación.»
- **Horas a cubrir** (término nuevo, contexto Datos maestros, RF-15): «Horas semanales de clase de la oferta que hay que asignar a docentes: la suma de las horas de cada asignatura-paralelo ofertada, por asignatura, por carrera o del periodo.»
