# Feature Specification: Descargar las plantillas de carga

**Feature Branch**: `018-plantillas-carga`

**Created**: 2026-10-23

**Status**: Draft

**Input**: User description: "Especifica la historia F01-H01 · Descargar las plantillas de carga (issue #27), a partir de RF-05, la ERS §8.3, el diccionario de datos de las cargas y el esqueleto del servicio Cargas."

**Origen**: issue #27 · F01-H01 · sub-issue de F01 (Cargar los 10 archivos con validación y vista previa) · milestone «Ola 1 · Datos listos» · Ola del secuenciador MVP · etiquetas `tipo:historia`, `prioridad:must`, `modulo:03-cargas`, `RF-05` · tamaño propuesto S · depende de #23 (B00-H06), cerrada.

**Requerimientos que cubre**: RF-05 (plantillas CSV descargables, con diccionario de datos, para cada una de las 10 cargas; criterio: «Cada plantilla se descarga con encabezados, tipos, obligatoriedad y ejemplos») y la ERS §8.3 (orden, contenido y obligatoriedad de las 10 cargas). RF-55 (el botón de carga por entidad) usa estas plantillas, pero es de F01-H02 en adelante. **Decisiones que aplica** (`docs/decisiones.md`): D-16 y D-65 (matriz de permisos), D-70 (permisos alternativos), ADR-021 (permisos), ADR-016 (catálogo de errores). **Principios de la constitución**: II (Ingesta de datos es de soporte), VI (las reglas son datos) y IX (mínimo privilegio).

**Historia (tal como está en el issue)**: Como Responsable de consolidación, quiero descargar la plantilla de cada una de las 10 cargas con su diccionario de datos, para preparar los archivos sin errores de formato.

**Punto de partida.** El servicio Cargas existe solo como esqueleto de la Ola 0: su esquema y la consulta de la sesión. Las 10 plantillas vacías, sus ejemplos y el diccionario de datos están fuera del sistema, en los anexos de la ERS (`cargas/plantillas/`, `cargas/ejemplos/` y `Diccionario_datos_cargas_MDD.xlsx`). Quien prepara un archivo tiene que pedirlos por fuera. Esta historia los pone en el MDD: una pantalla con las 10 cargas en su orden, desde la que se descarga la plantilla y el diccionario de cada una.

**Cómo queda tras las aclaraciones**: descargan las plantillas quienes tienen «Cargas: lectura» o «Distributivo del periodo: lectura», entre ellos el Responsable de consolidación; cada carga tiene su plantilla y su diccionario en CSV, y el diccionario también se ve en la pantalla; los dos salen de una sola definición de las 10 cargas.

## Clarifications

### Session 2026-10-23

- Q: ¿Quién descarga las plantillas, si la historia la pide el Responsable de consolidación pero en la matriz D-65 solo el Administrador y Talento Humano tienen «Cargas»? → A: Quien tenga «Cargas: lectura» o «Distributivo del periodo: lectura», con los permisos alternativos de D-70. Así descargan el Administrador, Talento Humano, el Responsable de consolidación, el Vicerrector, el Rector, los coordinadores de carrera, el OCS y Secretaría general; no los gestores ni los docentes. La matriz no cambia.
- Q: ¿En qué forma va el diccionario? → A: Un CSV aparte por carga, junto a su plantilla, con una fila por columna; además, la pantalla muestra el diccionario de la carga al desplegarla.
- Q: ¿Las plantillas y el diccionario se sirven tal cual o se generan de una definición? → A: De una sola definición de las 10 cargas y sus columnas, guardada como dato en el repositorio (principio VI); la plantilla y el diccionario se generan de ella, y las validaciones de F01-H02 usarán la misma.

## User Scenarios & Testing *(mandatory)*

La historia es una sola. Sus escenarios son partes que se pueden probar por separado; todas son necesarias (`prioridad:must`).

### User Story 1 - Descargar la plantilla de una carga (Priority: P1)

Como quien prepara los archivos de carga, quiero descargar la plantilla vacía de cualquiera de las 10 cargas, para llenarla con los encabezados exactos que el MDD espera (RF-05).

**Why this priority**: Es el escenario del issue y la base de F01: las cargas de F01-H02 en adelante leen archivos con estos encabezados.

**Independent Test**: Descargar la plantilla de cada una de las 10 cargas y comprobar que trae solo la fila de encabezados, con los nombres y el orden del diccionario.

**Acceptance Scenarios**:

```gherkin
Escenario: Plantilla de una carga
  Dado las 10 cargas de la sección 8.3 de la ERS
  Cuando descargo la plantilla de cualquiera de ellas
  Entonces el archivo trae los encabezados de la carga
  Y el diccionario indica el tipo, la obligatoriedad y un ejemplo de cada columna
```

1. **Given** cualquiera de las 10 cargas, **When** descargo su plantilla, **Then** recibo un archivo CSV con un nombre que dice qué carga es (por ejemplo, `01_estructura_organica.csv`), con una sola fila: los encabezados de la carga, en minúsculas y en el orden del diccionario.
2. **Given** la plantilla descargada, **When** la abro en una hoja de cálculo, **Then** las tildes y la «ñ» se ven bien y cada encabezado queda en su columna.

---

### User Story 2 - Consultar el diccionario de una carga (Priority: P1)

Como quien prepara los archivos de carga, quiero el diccionario de datos de cada carga, con el tipo, la obligatoriedad y un ejemplo de cada columna, para llenar la plantilla sin errores de formato (RF-05).

**Why this priority**: Es la segunda mitad del escenario del issue: sin el diccionario, la plantilla solo dice el nombre de cada columna.

**Independent Test**: Obtener el diccionario de una carga y comprobar que cada columna de la plantilla aparece, en el mismo orden, con su descripción, tipo, longitud máxima, obligatoriedad, formato o valores permitidos, clave, validación y un ejemplo.

**Acceptance Scenarios**:

1. **Given** cualquiera de las 10 cargas, **When** obtengo su diccionario, **Then** tiene una fila por cada columna de su plantilla, en el mismo orden, con descripción, tipo, longitud máxima, obligatoriedad (incluidas las condicionales, como «Sí si tipo_unidad = GESTION»), formato o valores permitidos, clave, validación y un ejemplo.
2. **Given** una columna cuyo formato remite a un catálogo, **When** la consulto, **Then** el diccionario dice qué valores admite.
3. **Given** el diccionario, **When** lo consulto, **Then** también trae las convenciones comunes a todas las cargas: separador, comillas, listas con «;» y «|», fechas aaaa-mm-dd, decimales con punto, S o N, cédula como texto y celdas vacías para lo opcional.
4. **Given** una carga, **When** descargo su diccionario, **Then** recibo un CSV aparte (por ejemplo, `01_estructura_organica_diccionario.csv`) con una fila por columna; y en la pantalla, al desplegar la carga, veo lo mismo (aclaración 2).

---

### User Story 3 - Las cargas en pantalla (Priority: P1)

Como quien prepara los archivos de carga, quiero una pantalla «Plantillas de carga» con las 10 cargas en su orden, para saber cuáles hay, de cuáles depende cada una y cuáles son obligatorias, y descargar desde allí sus plantillas y diccionarios.

**Why this priority**: Sin pantalla, las plantillas solo se obtendrían llamando al servidor.

**Independent Test**: Abrir «Plantillas de carga», ver las 10 cargas en el orden de la ERS §8.3 con su nombre, de qué cargas depende y si es obligatoria, y descargar la plantilla de una.

**Acceptance Scenarios**:

1. **Given** las 10 cargas, **When** abro «Plantillas de carga», **Then** las veo en el orden de la ERS §8.3 (1 Estructura orgánica a 10 Matriculados por carrera), cada una con su nombre, las cargas de las que depende, si es obligatoria (las 9 primeras; la 10 es recomendada) y su número de columnas.
2. **Given** una carga en la pantalla, **When** pulso descargar la plantilla o el diccionario, **Then** recibo ese CSV; al desplegar la carga veo su diccionario.
3. **Given** un usuario sin el permiso, **When** intenta abrir la pantalla o descargar, **Then** no la ve en el menú y el servidor responde «Acceso no autorizado» (403).

---

### Edge Cases

- **Columnas sin ejemplo en los anexos**: 6 de las 115 columnas no tienen ningún valor en `cargas/ejemplos/` (por ejemplo, `telefono` de docentes o `creditos` de asignaturas, que son opcionales). El diccionario igual trae un ejemplo para cada columna: para esas 6 se escribe uno coherente con su formato.
- **Obligatoriedad condicional**: algunas columnas son obligatorias solo según otra (por ejemplo, `codigo_gestion_autorizada` «Sí si tipo_unidad = GESTION»). El diccionario lo dice con la condición, no con un «Sí» o «No» a secas.
- **Carga 7, política de carga horaria**: su plantilla es igual que las demás, vacía. La política 2024 ya está precargada en Normativa (F04-H01), así que no se repite en la plantilla.
- **Las plantillas no tienen datos personales**: ni la plantilla ni el diccionario llevan datos de personas; los ejemplos son ficticios (RNF-SEG-06), y las cédulas de los ejemplos tienen dígito verificador válido.
- **Carga inexistente**: pedir la plantilla de una carga que no es una de las 10 responde «no encontrada» con su código de error (ADR-016).
- **Usuario sin permiso**: quien no tenga «Cargas: lectura» ni «Distributivo del periodo: lectura» (por ejemplo, un gestor o un docente) no ve «Plantillas de carga» en el menú, y el servidor responde «Acceso no autorizado» (403).
- **Plantilla y diccionario siempre de acuerdo**: los encabezados de la plantilla y las filas del diccionario son las mismas columnas en el mismo orden; nunca hay una columna en uno y no en el otro.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE ofrecer las 10 cargas de la ERS §8.3, en su orden, cada una con su número, nombre, las cargas de las que depende, si es obligatoria y su número de columnas.
- **FR-002**: El sistema DEBE entregar, para cada carga, una plantilla CSV en UTF-8 con una sola fila de encabezados, iguales en nombre y orden a los del diccionario de datos de la ERS (`cargas/plantillas/`).
- **FR-003**: El sistema DEBE entregar, para cada carga, su diccionario de datos en un CSV aparte y en la pantalla (aclaración 2): por cada columna, su descripción, tipo, longitud máxima, obligatoriedad (con su condición si la tiene), formato o valores permitidos, clave, validación y un ejemplo; más las convenciones comunes a todas las cargas.
- **FR-004**: La plantilla y el diccionario de una carga DEBEN tener las mismas columnas, en el mismo orden.
- **FR-005**: Las cargas, sus columnas y sus reglas DEBEN estar en una sola definición, guardada como dato y no como código repartido (principio VI, aclaración 3); la plantilla y el diccionario se generan de ella, para que las cargas de F01-H02 en adelante validen contra la misma definición.
- **FR-006**: Descargar las plantillas y el diccionario DEBE exigir un permiso, validado en el servidor (ADR-021). Lo DEBE poder hacer quien tenga «Cargas: lectura» o «Distributivo del periodo: lectura» (D-70, aclaración 1); sin ninguno de los dos, 403.
- **FR-007**: Pedir una carga que no existe DEBE responder «no encontrada» con su código del catálogo de errores (ADR-016).
- **FR-008**: La pantalla «Plantillas de carga» DEBE mostrar las 10 cargas como dice FR-001 y permitir descargar la plantilla y obtener el diccionario de cada una.

### Key Entities

- **Carga**: archivo de una de las 10 entidades que entra al MDD con validación y vista previa (glosario §7, `Carga`, Ingesta de datos); tiene número de orden, nombre, nombre de archivo, las cargas de las que depende y si es obligatoria. No es la carga horaria.
- **Plantilla de carga**: CSV vacío con su diccionario de datos para una carga (`PlantillaCarga`).
- **Columna de la plantilla**: una entrada del diccionario, con su nombre, descripción, tipo, longitud máxima, obligatoriedad, formato o valores permitidos, clave, validación y ejemplo.
- **Carga obligatoria**: carga sin la cual no se crea el distributivo: las 9 primeras (`obligatoria`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Quien prepara los archivos descarga la plantilla y el diccionario de cualquier carga desde la pantalla en menos de 30 segundos, sin ayuda.
- **SC-002**: Las 10 plantillas traen exactamente las 115 columnas de los anexos de la ERS, en su orden, y el 100 % de esas columnas tienen en el diccionario tipo, obligatoriedad y un ejemplo.
- **SC-003**: Una plantilla abierta en una hoja de cálculo muestra bien las tildes y la «ñ», con cada encabezado en su columna.
- **SC-004**: El 100 % de los intentos de descargar sin el permiso se rechazan, también llamando directamente al servidor.

## Assumptions

- Las 10 cargas, sus columnas y su diccionario son los de los anexos de la ERS v2.1 (`cargas/plantillas/`, `cargas/ejemplos/`, `Diccionario_datos_cargas_MDD.xlsx`); si la ERS cambia, se cambia la definición en el repositorio.
- El ejemplo de cada columna es el de la primera fila de `cargas/ejemplos/` que tiene un valor en esa columna, como en el diccionario de la ERS; para las 6 columnas que no tienen ninguno, se escribe uno.
- La plantilla es CSV (RF-05). El XLSX como formato de carga es de F01-H02 en adelante (RF-55).
- Descargar no cambia nada, así que no deja registro (principio VII: toda acción **relevante**).
- Fuera de alcance: subir, validar, ver la vista previa y aplicar las cargas (F01-H02 en adelante); el tablero de preparación (F02); delegar las cargas a Talento Humano.
