# Research: Mantener carreras, mallas y asignaturas (F03-H02)

Decisiones técnicas del plan. Cada sección tiene la decisión, por qué y las alternativas. Parte de lo que ya existe en `main` (`507e48e`):
- **Académico** (proceso `datos`): los periodos (F02-H01) y la estructura orgánica (F03-H01), con `unidad_organica`, `gestion_autorizada`, el registro de cambios (`registro_cambio`, con sus acciones en un `check`), `DatoInvalido` y `CambioDeValor` en el dominio, `Detalles.de(CambioDeValor)` en la aplicación y la pantalla «Estructura orgánica».
- **Permisos alternativos** (D-70) en el contrato, `VerificadorDePermisos` y la SPA.
- **El diccionario de datos** de las cargas 2 y 4 en Cargas (F01-H01), y los 10 CSV de los datos de prueba de la ERS en los recursos de prueba de Cargas (F01-H02).

## 1. Profundidad del modelo (principio II)

- **Decisión:** Académico es de soporte. El dominio tiene valores con sus reglas y una pocas reglas que miran a más de uno, como en F03-H01:
  - **`Carrera`** (record): código, nombre, siglas, sede, nivel de formación, modalidad, jornadas, campo de conocimiento (con el detallado opcional), número de ciclos y estado. El constructor valida sus reglas propias (§3).
  - **`Malla`** (record): código de versión, carrera y estado.
  - **`Asignatura`** (record): código, carrera, malla, nombre, ciclo, unidad de organización curricular, horas de clase, horas prácticas, créditos, tronco común, campo de conocimiento, palabras clave y si está activa.
  - **`CampoConocimiento`** (record, término del glosario): el campo amplio y el específico, con la regla de que el específico empieza por el amplio.
  - **`DatosMaestros`** (valor con las carreras, las mallas y las asignaturas que hacen falta para validar): las reglas entre entidades (§4).
- **Por qué:** las reglas que miran a otra entidad (la sede activa, la carrera vigente, la malla de la misma carrera, el ciclo frente al número de ciclos) se prueban sin dobles si el servicio le pasa al dominio lo que necesita.
- **Alternativa:** validar en el repositorio con consultas. La regla quedaría fuera del dominio.

## 2. Los valores permitidos: enumeraciones y una tabla (FR-006, principio VI)

- **Decisión:**
  - Los niveles de formación, las modalidades, las jornadas, las unidades de organización curricular y los estados son **enumeraciones del dominio** (`NivelDeFormacion`, `Modalidad`, `Jornada`, `UnidadDeOrganizacionCurricular`, `EstadoDeCarrera`, `EstadoDeMalla`), como `TipoDeUnidad` en F03-H01, con su `check` en la base. Cada lista vive en un solo lugar.
  - **Una prueba de Académico compara cada enumeración con el diccionario de datos** de las cargas 2 y 4 (`backend/servicios/cargas/src/main/resources/cargas/diccionario-de-datos.yaml`, leído como archivo, sin depender del módulo Cargas): si el diccionario cambia, la prueba falla. `EstadoDeMalla` no se compara: el diccionario no tiene la malla como entidad (aclaración 2). El estado de la asignatura del diccionario (`ACTIVA`, `INACTIVA`) se compara con lo que guarda `activa`.
  - Los **11 campos amplios CINE** son una tabla, `campo_amplio`, precargada por la migración con su código y su nombre (los de la hoja de catálogos del diccionario), porque la pantalla muestra su nombre (US3). Los campos específicos y detallados se validan por su forma (3 y 4 dígitos que empiezan por el anterior): el diccionario no trae su lista.
- **Por qué:** son catálogos fijos de la norma (RRA, CINE), no cifras de la política; cambiarlos es cambiar la ERS. La prueba contra el diccionario evita que las dos listas se desalineen.
- **Ajuste de la spec (aprobado y aplicado el 2026-10-24):** FR-006 pasa a decir «los valores permitidos DEBEN ser los del diccionario de datos, en un solo lugar, y una prueba lo comprueba». US3, escenario 1, dice «con su nombre CINE» para el campo amplio; el específico solo tiene código.
- **Alternativa:** una tabla por catálogo. Agrega tablas y lecturas para listas que no cambian sin cambiar la ERS.

## 3. Las reglas de cada entidad

| Regla | Dónde |
|---|---|
| Código de carrera: hasta 20, `^[0-9A-Z-]+$` (como `550613A01-P-0301`); único; no cambia | dominio y clave primaria |
| Nombre: no vacío, hasta 150 (carrera y asignatura) | dominio y `check` |
| Siglas: hasta 10, `^[0-9A-Z]+$`; únicas | dominio e índice único |
| Jornadas: al menos una, sin repetir | dominio |
| Campo amplio: uno de `campo_amplio`; específico: 3 dígitos que empiezan por el amplio; detallado: 4 dígitos que empiezan por el específico, opcional | dominio, clave foránea y `check` |
| Número de ciclos: 1 a 10 | dominio y `check` |
| Código de versión de malla: hasta 20, `^[0-9A-Z-]+$` (como `DS-AC-2025`); único; no cambia | dominio y clave primaria |
| Código de asignatura: hasta 30, `^[0-9A-Z-]+$` (como `DS-P-II-POO-204`); único; no cambia | dominio y clave primaria |
| Ciclo: 1 a 10 | dominio y `check` |
| Horas de clase: 0,5 a 20, con dos decimales como máximo; prácticas: 0 a 20, opcionales; créditos: mayores que 0, opcionales | dominio y `check` |
| Palabras clave: al menos 2, distintas, sin vacías, sin espacios en los extremos | dominio |

- **El nombre en mayúsculas** que pide el formato del diccionario no se exige: es una costumbre de los archivos, no una validación del diccionario. Se guarda como viene, sin espacios en los extremos.
- **«Debe coincidir con los dígitos del código de carrera si aplica»** (campo amplio de la carga 02) no se valida: «si aplica» no dice cuándo. Se anota para F01-H03.

## 4. Las reglas entre entidades (`DatosMaestros`)

- La sede de una carrera es una unidad **activa** de la estructura orgánica de tipo sede matriz, sede, extensión o campus. Académico la consulta en su propio repositorio de la estructura.
- Una malla nueva exige su carrera **vigente**. Una malla se puede derogar y volver a poner vigente; una derogada no admite asignaturas nuevas.
- Una asignatura nueva exige su carrera vigente y una malla **vigente de esa misma carrera**; su ciclo no pasa el número de ciclos de la carrera.
- Bajar el número de ciclos de una carrera por debajo del ciclo de una de sus asignaturas activas se rechaza en `numeroCiclos`.
- Corregir una asignatura revalida el ciclo frente a su carrera. Reactivarla exige su malla vigente.
- Los rechazos de datos son `DatoInvalido` con su campo (400). Los códigos repetidos, 409:

  | Código | Cuándo |
  |---|---|
  | `ACADEMICO_CARRERA_REPETIDA` | otro código de carrera igual |
  | `ACADEMICO_SIGLAS_REPETIDAS` | otras siglas iguales |
  | `ACADEMICO_MALLA_REPETIDA` | otro código de versión igual |
  | `ACADEMICO_ASIGNATURA_REPETIDA` | otro código de asignatura igual |

- **Candado:** como en F03-H01, cada escritura toma el candado de las tablas de los datos maestros (`LOCK TABLE … IN SHARE ROW EXCLUSIVE MODE`) antes de leer lo que valida, para que dos escrituras a la vez no dejen siglas repetidas ni una asignatura en una malla recién derogada.

## 5. Quién mantiene y quién lee (aclaración 1, D-70)

- **Lectura:** `[DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`, como la estructura orgánica. Lee el Responsable de consolidación y, como `AutoridadesDelToken` quita el alcance `:PROPIO`, también el coordinador.
- **Escritura:** `[CATALOGOS:ESCRITURA, DISTRIBUTIVO:ESCRITURA]`: el Administrador y el Vicerrector, que ya escribe los periodos con `DISTRIBUTIVO:ESCRITURA` (D-69). Sin decisiones nuevas.
- **SPA:** la ruta y el menú con la misma lista que la estructura; los botones, con `tieneAlguno` de la escritura.

## 6. Registro (principio VII)

- `registro_cambio` gana 9 acciones: `CARRERA_REGISTRADA`, `CARRERA_CORREGIDA`, `CARRERA_ESTADO_CAMBIADO`, `MALLA_REGISTRADA`, `MALLA_ESTADO_CAMBIADO`, `ASIGNATURA_REGISTRADA`, `ASIGNATURA_CORREGIDA`, `ASIGNATURA_INACTIVADA` y `ASIGNATURA_REACTIVADA`. El detalle lleva el código y cada cambio con su valor anterior y el nuevo, con `Detalles.de(CambioDeValor)`.
- Corregir sin cambios no registra, como en F03-H01.

## 7. El contrato (principio IV)

- **13 operaciones** en `contratos/academico/openapi.yaml`, tag `Carreras` (detalle en [contracts/academico.md](contracts/academico.md)):
  - lectura: `listarCamposAmplios`, `listarCarreras` (cada una con el resumen de sus mallas: código, estado, asignaturas activas y horas), `consultarCarrera`, `listarAsignaturasDeCarrera`, `consultarAsignatura`;
  - escritura: `registrarCarrera`, `corregirCarrera`, `cambiarEstadoCarrera`, `registrarMalla`, `cambiarEstadoMalla`, `registrarAsignatura`, `corregirAsignatura`, `cambiarEstadoAsignatura`.
- **Escenario 1 del issue:** en `AsignaturaNueva`, `carrera` y `horasSemanalesClase` son obligatorios en el contrato; sin ellos, la validación del contrato responde 400 `COMUN_SOLICITUD_INVALIDA` con un detalle por cada uno («Es obligatorio.»). Así «se indica qué falta», los dos a la vez.
- **Las horas de cada malla** (FR-012) salen de la suma de las horas de clase de sus asignaturas activas, en la consulta; no se guardan.

## 8. La pantalla «Carreras y mallas»

- **Decisión:** `frontend/src/pantallas/carreras/`, en el menú como «Carreras y mallas», debajo de «Estructura orgánica».
  - Una tabla de carreras: código, nombre, siglas, sede, modalidad, jornadas, estado y sus mallas (código, estado, horas).
  - Al elegir una malla, sus asignaturas agrupadas por ciclo, con las horas de clase de cada ciclo y de la malla; cada asignatura con su código, nombre, unidad de organización curricular, horas, tronco común, campo y palabras clave.
  - Con escritura: «Registrar carrera», «Corregir» y «Cambiar estado» en cada carrera; «Registrar malla» y «Derogar» o «Poner vigente» en cada malla; «Registrar asignatura», «Corregir» e «Inactivar» o «Reactivar» en cada asignatura. Los diálogos usan listas para la sede, el nivel, la modalidad, las jornadas, los campos amplios y la unidad de organización curricular.
- **Alternativa:** una pantalla para carreras y otra para asignaturas. La malla quedaría lejos de su carrera.

## 9. Punta a punta

- **Decisión:** `deploy/probar_carreras.sh`, con el estilo de `probar_estructura.sh`. Lee las cargas 02 y 04 de los datos de prueba de la ERS que ya están en el repositorio (`backend/servicios/cargas/src/test/resources/cargas/datos-de-prueba/`, sintéticos) y los registra con un prefijo de 4 caracteres al azar (`P` y 3 dígitos) delante de cada código, que cabe en todos los largos:
  1. el Vicerrector registra las 7 carreras, sus 12 mallas y las 300 asignaturas; cada malla suma 100 horas (SC-002);
  2. una asignatura sin carrera y sin horas da 400 con los dos campos (escenario 1); una con una palabra clave o un ciclo de más, 400 con su campo;
  3. códigos y siglas repetidos, 409;
  4. una asignatura consultada trae su campo amplio con su nombre, su campo específico, sus palabras clave y si es de tronco común (escenario 2);
  5. derogar una malla y registrar en ella da 400;
  6. `consolidacion.demo` lee y recibe 403 al escribir; `docente.demo`, 403 al leer;
  7. el registro tiene las acciones.

  Las carreras cuelgan de la sede matriz que ya tenga la base, o de una temporal si no hay. La limpieza borra en la base solo lo que creó, con sus registros.
- **Las pruebas del dominio** registran también las 7 carreras, las 12 mallas y las 300 asignaturas, leídas de una copia de las cargas 02 y 04 en los recursos de prueba de Académico.

## 10. Glosario (principio III)

- La spec y el código usan «Modalidad», «Nivel de formación» y «Jornada». «Jornada» está en el glosario, pero como «Matutina, nocturna o intensiva», y la carga 02 admite también **vespertina**. «Modalidad» y «Nivel de formación» no están. **Aprobada por el autor y aplicada el 2026-10-24** (copia `modelo_dominio.md.antes-F03-H02`):

  | Término | Significado | En el código | Contexto | Fuente |
  |---|---|---|---|---|
  | Jornada | Matutina, vespertina, nocturna o intensiva. | `Jornada` | Datos maestros | RF-15, D-09, carga 2 |
  | Modalidad | Forma de estudios de una carrera: presencial, semipresencial, en línea, a distancia, dual o híbrida (RRA art. 54). | `Modalidad` | Datos maestros | RF-12, carga 2 |
  | Nivel de formación | Técnico superior, tecnológico superior o tecnológico superior universitario. | `NivelDeFormacion` | Datos maestros | RF-12, carga 2 |

  La fila de «Jornada» reemplaza a la actual; las otras dos se agregan después de «Carrera».
