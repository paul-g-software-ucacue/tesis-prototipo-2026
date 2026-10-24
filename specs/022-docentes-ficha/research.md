# Research: Mantener los docentes con su ficha académica (F03-H04)

Decisiones de diseño de la historia, con su porqué y lo que se descartó. Parte de la spec y de sus tres aclaraciones del 2026-10-24, del diccionario de datos (carga 6) y de lo que ya hay: el esqueleto del servicio de Docentes (B00-H02), Académico con las carreras y asignaturas (F03-H02) y la receta de `backend/plantillas/README.md` para el primer contrato de un servicio.

No quedan puntos abiertos de la spec: las tres preguntas las respondió el autor antes de redactarla. Queda una propuesta para el glosario (§11), que el autor aprueba antes del código.

## 1. Profundidad del modelo (principio II)

- **Decisión:** Docentes es de soporte. `Docente` es un *record* con las reglas de cada campo y las que cruzan campos (horas y dedicación, categoría y tipo de personal, fin de contrato del ocasional). Su ficha es otro *record*, `FichaAcademica`, con `TituloTercerNivel`, `TituloCuartoNivel`, `Curso`, `ExperienciaLaboral` y las asignaturas afines declaradas. Sin agregado ni eventos.
- **Por qué:** como en Académico (F03-H02, F03-H03), las reglas son de forma y de coherencia entre campos del mismo docente; lo único que mira afuera (la carrera base, las asignaturas afines y el catálogo de campos amplios) se comprueba con un valor de referencias (§4).
- **Descartado:** un agregado con eventos `DocenteRegistrado`/`DocenteCorregido`. Nadie los escucha todavía; Generación leerá los docentes por la API publicada en la Ola 2.

## 2. Qué se guarda (FR-001, FR-002)

- **Decisión:** una tabla `docente` en el esquema `docentes`, con clave `identificacion` y una columna por dato escalar de la carga 6, incluido el título de tercer nivel con su campo. Las cuatro listas de la ficha (títulos de cuarto nivel, cursos, experiencia, asignaturas afines) van en columnas `jsonb` de la misma fila.
- **Por qué:** RF-17 dice que la ficha vive «dentro del propio registro» del docente, y siempre se lee y se guarda entera con él. Las listas no se buscan ni se ordenan por dentro; el dominio las valida antes de guardarlas. Una fila por docente deja la corrección en una sola sentencia.
- **Descartado:** cuatro tablas hijas. Agregan joins, claves y borrados de filas hijas al corregir (con «nada se borra» difícil de explicar), sin una consulta que las necesite.
- **Correo único:** índice único sobre `lower(correo_institucional)`, además de la regla en la aplicación (§5).
- **Estado laboral:** columna con `ACTIVO`, `LICENCIA` o `INACTIVO`; un docente nuevo queda `ACTIVO`.

## 3. Las reglas (FR-004, FR-006)

En `Docente` y su ficha, sin dobles, con el año actual como parámetro:

- **Identificación:** `CEDULA` con 10 dígitos y dígito verificador módulo 10 (provincia 01–24 o 30, tercer dígito menor que 6); `PASAPORTE` con 1 a 13 letras mayúsculas o dígitos. No cambia al corregir.
- **Nombres:** primer apellido y primer nombre obligatorios, segundos opcionales; hasta 60; se guardan en mayúsculas (`strip` y `toUpperCase(Locale.ROOT)`), con letras, tildes, ñ, espacios, apóstrofo y guion.
- **Correo:** hasta 100, una forma simple de correo (`^[^@\s]+@[^@\s]+\.[^@\s]+$`), en minúsculas. **Teléfono:** opcional, 7 a 10 dígitos.
- **Tipo de personal, categoría, dedicación, condición especial, estado:** enumeraciones con los valores del diccionario (§9). Titular: categoría distinta de `NO_APLICA`; los demás: `NO_APLICA`.
- **Horas contratadas (RN-01):** `TC` = 40, `MT` = 20, `TP` de 1 a 19. Omitidas: 40 o 20; en `TP`, obligatorias.
- **Fechas:** ingreso, inicio de contrato y fin; fin obligatorio en `OCASIONAL` y, si se indica, posterior al inicio.
- **Campo de conocimiento:** amplio de 2 dígitos, específico de 3 que empieza por el amplio (como `CampoConocimiento` de Académico, copiado en Docentes: los servicios no comparten dominio). En los títulos de cuarto nivel, cursos y experiencia solo se pide el específico; el amplio son sus dos primeros dígitos.
- **Listas:** hasta 20 elementos cada una; título de cuarto nivel con título (hasta 150), tipo `MAESTRIA`, `ESPECIALIZACION` o `DOCTORADO` y año de 1950 al actual; curso con nombre (hasta 150), horas enteras de 1 a 2000 y año de 1950 al actual; experiencia con cargo e institución (hasta 150) y años decimales mayores que 0 y hasta 60; asignaturas afines sin repetir.
- **Mensajes:** cada `DatoInvalido` lleva su campo y, en las listas, su posición (`cursos[2].horas`), y el motivo en español. Un valor fuera de un catálogo lo rechaza el contrato (enum) antes del dominio; el 400 común dice los valores permitidos (escenario «Edición inválida»; la `DescripcionDeValidacion` de la SPA ya los muestra desde F03-H02).
- **No se aplican aquí** (spec, supuestos): que el ocasional sea solo TC o MT (POL 5.1), que el contrato cubra el periodo y las horas que cambia la condición especial. Son de la normativa y del periodo.

## 4. La carrera base, las asignaturas afines y los campos amplios (FR-005, aclaración 3)

- **Decisión:** Académico publica un puerto nuevo en su API dentro del proceso, `ConsultarReferencias` en `ec.mdd.academico.puertos.entrada`, con `referencias()`: las carreras (código, nombre, siglas), las asignaturas (código, nombre, carrera) y los 11 campos amplios (código, nombre), en *records* del mismo paquete. Docentes lo usa por un puerto de salida propio, `ReferenciasAcademicas`, con su adaptador `AcademicoDesdePuertos`, como Académico usa Normativa (F02-H01). `ModulosTest` del proceso `datos` comprueba que Docentes solo toca `puertos.entrada` de Académico.
- **Por qué:** los dos servicios están en el mismo proceso (`datos`) y cada uno tiene su esquema; no hay clave foránea entre esquemas (principio V). `ConsultarCarreras` ya existe, pero devuelve tipos del dominio de Académico (`Asignatura`, `CampoAmplio`), que Docentes no puede usar. Un puerto con *records* propios publica solo lo que hace falta.
- **Al registrar o corregir:** la carrera base tiene que existir (en cualquier estado), cada asignatura afín también, y el campo amplio del título de tercer nivel tiene que ser uno de los 11. Si no, 400 en su campo.
- **Al consultar:** la ficha muestra el nombre del campo amplio de cada título y el nombre de cada asignatura afín; un código que ya no exista se muestra sin nombre (no pasa: Académico no borra).
- **Descartado:** copiar los campos amplios en una tabla de Docentes (dos lugares para el mismo catálogo, principio VI); que la SPA pida las carreras a Académico (Talento Humano no tiene permiso para leer Académico).

## 5. Concurrencia y unicidad

- **Decisión:** registrar y corregir bloquean la tabla `docente` (`LOCK TABLE … IN SHARE ROW EXCLUSIVE MODE`), comprueban la identificación y el correo, deciden con el dominio y guardan, en una transacción. La clave primaria y el índice único respaldan las dos reglas.
- **Por qué:** es el patrón de Académico; los cambios de datos maestros son pocos y serializarlos cuesta poco.
- **Errores:** identificación repetida → `DOCENTES_DOCENTE_REPETIDO` (409); correo de otro docente → `DOCENTES_CORREO_REPETIDO` (409); docente inexistente → 404.

## 6. Búsqueda (FR-007)

- **Decisión:** la aplicación lee los docentes y filtra en memoria: el texto se compara sin mayúsculas ni tildes (`Normalizer` NFD sin marcas, como los nombres de rol) contra los nombres y apellidos juntos, o contra el inicio de la identificación; además, por carrera base y estado. Orden por apellidos y nombres.
- **Por qué:** un IST tiene decenas o pocos cientos de docentes (47 en los datos de prueba). Filtrar en memoria evita la extensión `unaccent` (que pide un superusuario en la migración) o una columna normalizada que mantener.
- **Respuesta de la búsqueda:** un resumen por docente (identificación, nombre completo, carrera base con su nombre, tipo de personal, dedicación, condición especial, estado), sin la ficha.
- **Meta:** por debajo de 300 ms con 500 docentes.

## 7. Quién mantiene y quién lee (aclaración 1, D-70)

- **Escritura:** `[DOCENTES:ESCRITURA, CATALOGOS:ESCRITURA, DISTRIBUTIVO:ESCRITURA]`: Talento Humano, el Administrador y el Vicerrector.
- **Lectura:** `[DOCENTES:LECTURA, DISTRIBUTIVO:LECTURA, CATALOGOS:LECTURA]`: además, el Responsable de consolidación y los coordinadores de carrera (estos, de todo: D-65 les da `DOCENTES:LECTURA` con alcance `TODO`).
- **Referencias:** `consultarReferenciasDeDocentes` pide la lectura; la llamada a Académico va dentro de la operación de Docentes y su autorización es la de Docentes (como D-69 con Normativa).
- **Sin decisiones nuevas.** La matriz no cambia.

## 8. Registro de cambios (FR-010, principio VII)

- **Decisión:** una tabla `registro_cambio` en el esquema `docentes`, igual que la de Académico (id, momento, usuario, acción, detalle `jsonb`), con las acciones `DOCENTE_REGISTRADO`, `DOCENTE_CORREGIDO` y `DOCENTE_ESTADO_CAMBIADO`. El detalle de una corrección guarda cada campo cambiado con su valor anterior y el nuevo; una lista cambiada, entera antes y después.
- **Datos personales:** el registro vive en el esquema del servicio y no se expone por la API en esta historia. Los logs técnicos no llevan cédulas ni correos (RNF-SEG-06): los mensajes de las excepciones de dominio no los incluyen.

## 9. Los valores permitidos (FR-012, principio VI)

- **Decisión:** enumeraciones del dominio de Docentes (`TipoDeIdentificacion`, `TipoDePersonal`, `CategoriaDeEscalafon`, `Dedicacion`, `CondicionEspecial`, `TipoDeTituloDeCuartoNivel`, `EstadoLaboral`), los enums del contrato con los mismos valores y las restricciones `check` de la migración. `ValoresPermitidosTest` de Docentes los compara con la carga 6 de `diccionario-de-datos.yaml`, como ya hace Académico con las cargas 2 y 4.
- **Campos amplios:** de Académico (§4).
- **Nombres en la SPA:** las etiquetas en español («Tiempo completo», «Medio tiempo», «Tiempo parcial (hasta 19 h)»…) en un módulo de la pantalla, como las jornadas y modalidades de F03-H02.

## 10. La pantalla «Docentes» (FR-011)

- **Lista:** buscador (nombre o identificación), filtros por carrera base y estado, y una tabla con el resumen; contador «N docentes».
- **Ficha:** un panel o diálogo de solo lectura con los datos personales y de contrato y la ficha, con cada título con su campo amplio (código y nombre) y específico, cada curso con sus horas.
- **Formulario:** registrar y corregir en un diálogo con secciones (datos personales, contrato, carrera base, título de tercer nivel) y editores de lista para títulos de cuarto nivel, cursos, experiencia y asignaturas afines (agregar y quitar filas; las asignaturas afines con un selector de las asignaturas de Académico, agrupadas por carrera). Usa `frontend/src/pantallas/comun` (`formulario.ts`, `useEnvio.ts`, `Formulario.tsx`); el error de un elemento de una lista se muestra junto a esa fila.
- **Estado:** un diálogo para cambiar a activo, en licencia o inactivo.
- **Permisos:** la ruta y el menú piden la lectura; sin escritura, la pantalla queda en solo lectura y dice por qué.
- **Ruta:** `/docentes`; menú «Docentes» en el grupo de datos maestros.

## 11. Glosario (principio III) — propuesta para el autor

`modelo_dominio.md` §7.1 ya tiene Docente, Tipo de personal, Dedicación, Condición especial y Ficha académica. La historia usa cuatro nombres de la carga 6 que no están, y uno que cambia:

| Término | Definición propuesta | Código | Contexto | Origen |
|---|---|---|---|---|
| Tipo de personal *(cambia)* | Titular, ocasional, invitado u honorario (RCE art. 137). | `TipoPersonal` | Datos maestros | RF-16, carga 6 |
| Categoría de escalafón *(nuevo)* | Categoría y nivel de un docente titular en el escalafón (auxiliar, agregado o principal, con su nivel); los demás no la tienen. | `CategoriaEscalafon` | Datos maestros | RF-16, carga 6 |
| Carrera base *(nuevo)* | Carrera a la que pertenece un docente. No limita las carreras en las que puede dictar clase. | `carreraBase` | Datos maestros | Carga 6 |
| Asignatura afín declarada *(nuevo)* | Asignatura que la ficha académica de un docente marca como afín; suma al puntaje de afinidad cuando el coordinador la valida. | `asignaturasAfines` | Datos maestros | RF-17, carga 6 |
| Estado laboral *(nuevo)* | Activo, en licencia o inactivo. Solo un docente activo recibe asignaciones. | `EstadoLaboral` | Datos maestros | Carga 6 |

Se aplica con una copia `modelo_dominio.md.antes-F03-H04`, antes del código, si el autor lo aprueba.

## 12. Datos de prueba

- **Decisión:** copiar `06_docentes.csv` de los datos de prueba a `backend/servicios/docentes/src/test/resources/docentes/datos-de-prueba/`, con `02_carreras.csv` y `04_asignaturas_malla.csv` para las referencias, como ya hace Académico. Son ficticios: identificaciones `01099900xx` y correos de `instituto-prueba.edu.ec` (principio IX).
- **Caso grande:** el dominio registra los 47 docentes y encuentra cada uno por su cédula (SC-002).
- **Punta a punta:** `deploy/probar_docentes.sh` registra las carreras y asignaturas que hagan falta y los docentes, prueba la búsqueda, una corrección, un 400 con los valores permitidos, los dos 409, el estado y los permisos, y deja la base como estaba.
