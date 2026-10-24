---

description: "Tareas de F01-H01: descargar las plantillas de carga"
---

# Tasks: Descargar las plantillas de carga

**Input**: documentos de diseño en `specs/018-plantillas-carga/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/cargas.md](contracts/cargas.md) y [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba va antes que el código que la hace pasar y tiene que fallar primero.

**Decisiones**: D-16, D-65, D-70, ADR-016 y ADR-021 en [`docs/decisiones.md`](../../docs/decisiones.md); RF-05 y la ERS §8.3; el glosario §7 («Carga», «Plantilla de carga», «Diccionario de datos», «Carga obligatoria»).

**Organization**:
- **Fundacional:** el contrato, el dominio con la definición YAML y su oráculo, la lectura y el servicio.
- **US1:** descargar la plantilla de una carga.
- **US2:** el diccionario de una carga.
- **US3:** la pantalla «Plantillas de carga».

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase (archivos distintos y sin dependencias pendientes).
- **[Story]**: US1 a US3.
- Rutas relativas a la raíz. Abreviaturas: `CAR` = `backend/servicios/cargas/src`, `JCAR` = `CAR/main/java/ec/mdd/cargas` y `PCAR` = `CAR/test/java/ec/mdd/cargas`.

## Convenciones para todas las tareas

- **Capas (ADR-012):** dominio sin Spring ni SnakeYAML; aplicación sin infraestructura. `ArquitecturaTest` lo comprueba.
- **Soporte (principio II):** `DiccionarioDeDatos` y sus valores son records validados; sin agregado, repositorio ni eventos.
- **Principio VI:** las cargas, sus columnas, las convenciones y los catálogos solo en `diccionario-de-datos.yaml` (y en el oráculo de las pruebas); nunca en el código de producción.
- **Principio IX:** sin fórmulas en los CSV; ejemplos ficticios; los permisos, en el servidor.
- **Pruebas:** Mockito solo en la aplicación; contrato con swagger-request-validator; permisos con `VerificadorDePermisos`; 60 % de líneas por módulo.
- **Idioma, formato y commits:** como en las historias anteriores; la evidencia va en `specs/018-plantillas-carga/evidencia.md` y los commits llevan `Refs #27`.

---

## Phase 1: Setup

- [X] T001 Crear `specs/018-plantillas-carga/evidencia.md` con estas secciones: entorno; base (contrato, dominio y definición, lectura y servicio); «Plantilla de una carga» (US1); «Diccionario» (US2); pantalla (US3); cierre; equipo de demostración; notas.

---

## Phase 2: Foundational (bloquea a todas las historias)

### Contrato (principio IV, research §8)

- [X] T002 Cambiar `contratos/cargas/openapi.yaml` según contracts/cargas.md: el tag `Plantillas`, los 6 esquemas, el parámetro `CodigoDeCarga` (`^[a-z_]{1,40}$`, `example: estructura_organica`) y las 3 operaciones con `x-mdd-permiso: [CARGAS:LECTURA, DISTRIBUTIVO:LECTURA]`; las dos descargas con `200` en `text/csv` (`type: string`) y el encabezado `Content-Disposition`, y `404` con el `Error` común (`COMUN_RECURSO_NO_ENCONTRADO`); el comentario de cabecera cita F01-H01, RF-05 y D-70. Sin códigos nuevos en `errores.md`. Regenerar (`openApiGenerate`); agregar `contratos/cargas/openapi.yaml` a `generar:api` en `frontend/package.json` y generar `frontend/src/api/generado/cargas.d.ts`; `verificar:ruptura` en verde.

### Dominio y definición (research §1 a §5, data-model.md)

- [X] T003 Prueba primero, en `PCAR/dominio/DiccionarioDeDatosTest.java` y `PCAR/dominio/ColumnaDeCargaTest.java`, sin dobles, con definiciones armadas en la prueba: cada regla de data-model.md da `IllegalArgumentException` con la carga y la columna en el mensaje (9 cargas o 11; un número repetido o fuera de 1 a 10; un código o un archivo repetido; `dependeDe` hacia una carga posterior o inexistente; una carga sin columnas; un campo repetido o con mayúsculas; una columna sin tipo, sin obligatoriedad o sin ejemplo; un `catalogo` que no existe; un texto que empieza con `=`, `+`, `-`, `@`, tabulador o retorno de carro); `carga(codigo)` devuelve la carga o `Optional.empty()`; `Carga.plantilla()` devuelve su `PlantillaCarga` con los encabezados en orden. Crear en `JCAR/dominio/` `Obligatoriedad`, `ValorDeCatalogo`, `Catalogo`, `Convencion`, `ColumnaDeCarga`, `Carga`, `PlantillaCarga` y `DiccionarioDeDatos`, con JavaDoc en español que cite RF-05, la ERS §8.3 y el glosario («Carga», «Plantilla de carga», «Diccionario de datos», «Carga obligatoria»). T003 pasa.
- [X] T004 Copiar a `CAR/test/resources/cargas/anexo-ers-encabezados.csv` la primera línea de cada uno de los 10 CSV de `MDD_artefactos_desarrollo/requerimientos_mdd/cargas/plantillas/`, precedida por el nombre del archivo (solo nombres de columnas, sin datos), con un comentario de origen en la evidencia. Es el oráculo de research §2.
- [X] T005 Prueba primero, en `PCAR/adaptadores/salida/DiccionarioYamlTest.java`, con el YAML real de los recursos: se lee y valida; tiene 10 cargas en el orden de la ERS §8.3 (1 Estructura orgánica … 10 Matriculados por carrera) con 7, 12, 8, 14, 9, 26, 14, 8, 12 y 5 columnas (115); los encabezados de cada carga son **iguales, en nombre y orden,** a los del oráculo de T004; la 9 es `OBLIGATORIA_SALVO_SIN_PROYECTOS`, la 10 `RECOMENDADA` y las demás `OBLIGATORIA`; `dependeDe` como la columna «Depende de» de la ERS (5 → 3, 4; 8 → 1, 2, 6; …); 13 convenciones y 5 catálogos (21 gestiones autorizadas); `codigo_gestion_autorizada` dice «Sí si tipo_unidad = GESTION» y nombra `GESTIONES_AUTORIZADAS`; las 6 columnas de research §4 tienen el ejemplo de esa tabla; un YAML con un error lanza la excepción del dominio. Crear `JCAR/puertos/salida/FuenteDelDiccionario.java`, `JCAR/adaptadores/salida/DiccionarioYaml.java` (SnakeYAML con `SafeConstructor`, como `CargaUsuariosDemo`) y `CAR/main/resources/cargas/diccionario-de-datos.yaml`, transcrito de las listas `L`, `CONV` y `CAT` de `build_diccionario.py` (ERS v2.1), con el ejemplo de la primera fila de `cargas/ejemplos/` que tenga valor y un comentario en cada ejemplo escrito a mano. Al transcribir, cada «(hoja Catálogos …)» de los formatos se cambia por «(ver los valores permitidos)», y la prueba comprueba que ningún texto del YAML dice «hoja Catálogos». T005 pasa.

### Servicio (data-model.md, puertos)

- [X] T006 Prueba primero, en `PCAR/aplicacion/ConsultarPlantillasServicioTest.java` (Mockito sobre `FuenteDelDiccionario`): lee la fuente una sola vez; `diccionario()` la devuelve; `plantilla(codigo)` devuelve la `Carga`; un código inexistente lanza `CargaNoEncontrada`. Crear `JCAR/puertos/entrada/ConsultarPlantillas.java`, `JCAR/puertos/entrada/CargaNoEncontrada.java` y `JCAR/aplicacion/ConsultarPlantillasServicio.java`; registrar los beans en `JCAR/infraestructura/ConfiguracionCargas.java`. T006 pasa.

**Checkpoint**: el contrato, la definición validada y el servicio están listos.

---

## Phase 3: User Story 1 - Descargar la plantilla de una carga (Priority: P1) 🎯 MVP

**Goal**: la plantilla CSV de cualquiera de las 10 cargas (FR-001, FR-002, FR-004, FR-006, FR-007; SC-002, SC-003, SC-004).

**Independent Test**: descargar las 10 plantillas; cada una trae solo sus encabezados, en orden; sin permiso, 403.

- [X] T007 [P] [US1] Prueba primero, en `PCAR/adaptadores/entrada/EscritorCsvTest.java`: empieza con el BOM UTF-8; separa con coma y termina cada línea con CRLF; un valor con coma, comillas o salto de línea va entre comillas dobles con las internas duplicadas; un valor que empieza con `=`, `+`, `-` o `@` sale con un apóstrofo delante; las tildes y la «ñ» salen en UTF-8. Crear `JCAR/adaptadores/entrada/EscritorCsv.java`. T007 pasa.
- [X] T008 [US1] Prueba primero, en `PCAR/adaptadores/entrada/PlantillasContratoTest.java` (con el YAML real): `listarCargas` 200 valida contra el contrato y trae las 10 cargas con 115 columnas; `descargarPlantilla` de cada una de las 10: 200, `text/csv;charset=UTF-8`, `Content-Disposition: attachment; filename="NN_<codigo>.csv"`, el BOM y una sola línea igual al oráculo de T004; un código inexistente da 404 `COMUN_RECURSO_NO_ENCONTRADO` y uno que no cumple el patrón, 400; **con `Accept: text/csv`**, el 404 y el 403 llegan con el `Error` común y no como 406 (research §8; si no, ajustar el contrato o el manejo de errores y anotarlo). Crear `JCAR/adaptadores/entrada/PlantillasControlador.java`, con `hasAnyAuthority('PERMISO_CARGAS:LECTURA','PERMISO_DISTRIBUTIVO:LECTURA')`. T008 pasa.
- [X] T009 [US1] Prueba primero, en `PCAR/adaptadores/entrada/PermisosCargasTest.java` (con `VerificadorDePermisos`): las 3 operaciones nuevas exigen sus dos permisos alternativos (sin ninguno, 403; con cada uno por separado, no); la sesión sigue igual. T009 pasa.

**Checkpoint**: las plantillas se descargan por la API.

---

## Phase 4: User Story 2 - Consultar el diccionario de una carga (Priority: P1)

**Goal**: el diccionario de cada carga, en CSV y en la respuesta de `listarCargas` (FR-003, FR-004; SC-002).

**Independent Test**: el diccionario de una carga tiene una fila por columna, en el orden de la plantilla, con tipo, obligatoriedad y ejemplo, y luego las convenciones.

- [X] T010 [US2] Prueba primero, en `PlantillasContratoTest`: `descargarDiccionario` de cada una de las 10: 200, `text/csv`, `filename="NN_<codigo>_diccionario.csv"`, el BOM, el encabezado `numero,campo,descripcion,tipo,longitud_maxima,obligatorio,formato_o_valores,clave,validacion,ejemplo`, una fila por columna en el orden de la plantilla, con tipo, obligatorio y ejemplo no vacíos; en la carga 1, la fila de `codigo_gestion_autorizada` lista en `formato_o_valores` las 21 gestiones («G01 Rector; G02 Vicerrector; …»); después, una fila vacía, `tema,regla` y las 13 convenciones; las descripciones con tildes y «ñ» (por ejemplo, «Gestión autorizada…») salen en UTF-8 después del BOM; `listarCargas` trae, en esa columna, sus `valores`; 404 como en T008. Ampliar `EscritorCsv` o el controlador para el diccionario. T010 pasa.

**Checkpoint**: la plantilla y su diccionario se descargan por la API.

---

## Phase 5: User Story 3 - Las cargas en pantalla (Priority: P1)

**Goal**: «Plantillas de carga», con las 10 cargas, sus dos descargas y el diccionario desplegable (FR-008; SC-001).

- [X] T011 [US3] Prueba primero, en `frontend/src/pantallas/plantillas/Plantillas.test.tsx` (Vitest con la API simulada): la tabla con las 10 cargas en orden, cada una con número, nombre, «Depende de» con los nombres de esas cargas («—» si no depende de ninguna), «Obligatoria» («Sí», «Sí, o sin proyectos» o «Recomendada») y su número de columnas; desplegar una carga muestra su diccionario (descripción, tipo, longitud, obligatorio, formato o valores, clave, validación y ejemplo) y las convenciones, plegadas arriba; «Plantilla» y «Diccionario» piden su CSV con el token y descargan un archivo con el nombre del `Content-Disposition`; un error muestra su título; el menú y la ruta con `[CARGAS:LECTURA, DISTRIBUTIVO:LECTURA]` (con uno basta; sin ninguno, «Acceso no autorizado» y sin la entrada). Crear `apiCargas` en `frontend/src/api/cliente.ts`, `DESCARGA_DE_PLANTILLAS` en `frontend/src/sesion/funcionalidades.ts`, la pantalla en `frontend/src/pantallas/plantillas/`, su ruta en `frontend/src/rutas.tsx` y su entrada en `frontend/src/navegacion/Marco.tsx`. T011 pasa.
- [X] T012 [US3] Prueba con Playwright, en `frontend/e2e/plantillas.spec.ts`: `consolidacion.demo` abre «Plantillas de carga», descarga la plantilla de docentes (evento `download`: nombre `06_docentes.csv` y 26 encabezados) y el diccionario de la estructura orgánica; `gestion.demo` no ve la entrada; axe-core sin fallas WCAG 2.1 AA en la tabla y con un diccionario desplegado.

**Checkpoint**: las tres historias pasan.

---

## Phase 6: Punta a punta y cierre

- [ ] T013 Crear `deploy/probar_plantillas.sh`, con el estilo de `probar_estructura.sh`, con los 5 pasos de research §10: (1) `consolidacion.demo` lista 10 cargas y 115 columnas, cada una con tipo, obligatoriedad y ejemplo; (2) descarga las 10 plantillas: 200, `text/csv`, su `Content-Disposition`, el BOM y una sola línea; (3) el diccionario de la carga 1 tiene 7 filas de columnas y las 13 convenciones; (4) `datos.demo` y `coordinacion.demo` descargan; `gestion.demo` y `docente.demo` reciben 403; (5) una carga inexistente da 404 `COMUN_RECURSO_NO_ENCONTRADO`. No crea nada. Agregar su paso a `.github/workflows/perfil-consolidado.yml`. Correrlo dos veces y anotarlo; `shellcheck` sin observaciones.
- [ ] T014 Correr todo, como en las historias anteriores: backend, SPA, los 8 scripts con el perfil reconstruido, `probar_equipo.sh`, `shellcheck` y Gitleaks sobre un clon. Hacer commit y push, abrir el PR en borrador con `Refs #27` y marcarlo listo después de sus runs.
- [ ] T015 Completar la evidencia y el PR. Fusionar con *squash* con la aprobación del autor. Después: esperar el despliegue («Sano en N s»); el autor entra con `consolidacion.demo`, abre «Plantillas de carga», descarga una plantilla y un diccionario en menos de 30 s cada uno (SC-001) y abre el diccionario en LibreOffice Calc con las tildes bien y cada dato en su columna, y llena una fila de la plantilla con un valor con tilde, la guarda como CSV y comprueba que se conserva (SC-003); abrir el PR de evidencia con `Closes #27` y borrar las ramas.

---

## Dependencies & Execution Order

- **Setup:** T001 libre.
- **Foundational:** T002 primero; T003 antes que T005; T004 antes que T005; T006 después de T005.
- **US1:** T007 libre después de la fase 2; T008 después de T006 y T007; T009 después de T008.
- **US2:** T010 después de T008.
- **US3:** T011 después de T002; T012 después de T010 y T011.
- **Cierre:** T013 después de T010; T014 y T015 al final.

### Parallel Opportunities

- La SPA (T011) puede empezar después de T002, con la API simulada.
- T003 y T004 no dependen entre sí, ni T007 de T006.
- Como trabajas solo, [P] indica orden libre, no trabajo simultáneo.

---

## Implementation Strategy

### MVP (fase 2 y US1)

1. El contrato, la definición validada con su oráculo y el servicio.
2. Descargar las plantillas por la API. **Validar:** las 10 plantillas son iguales a las de la ERS.

### Incremental

1. **US2:** el diccionario.
2. **US3:** la pantalla.
3. **Cierre:** punta a punta, PR, fusión y despliegue.

---

## Notes

- Subir, validar, la vista previa y aplicar las cargas son de F01-H02 en adelante; usarán `DiccionarioDeDatos`.
- El XLSX como formato de carga y el tablero de preparación quedan fuera.
- Hacer commit por tarea o por grupo lógico en `018-plantillas-carga`, con `Refs #27`.
