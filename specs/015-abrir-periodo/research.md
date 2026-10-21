# Research: Abrir el periodo académico con su versión de normativa (F02-H01)

Decisiones técnicas del plan. Cada sección tiene la decisión, por qué y las alternativas. Parte de lo que ya existe en `main` (`798dda0`):
- Académico tiene su esquema vacío (`V1__esquema_academico.sql`, D-35), sus cinco capas vacías y su prueba de arquitectura. No tiene contrato.
- Normativa publica en `ec.mdd.normativa.puertos.entrada` (interfaz `api` de Spring Modulith) `AsociarVersionAPeriodo` y `ConsultarNormativa.periodo(codigo)`, con `AsignacionDelPeriodo`, `PeriodoConVersion`, `PeriodoSinVersion` y `VersionNoEncontrada` (F04-H02, research §4). Su HTTP `asociarPeriodo` y `consultarPeriodo` siguen ahí.
- La pantalla «Normativa» tiene un diálogo «Asociar a un periodo» que pide el código a mano (F04-H02, research §9).
- La puerta de enlace ya envía `/api/academico/**` al proceso `datos`.
- Usuarios llama a Roles dentro del proceso `identidad` por sus puertos de entrada, con un adaptador de salida (`AutorizacionDesdeRoles`). Es el precedente.

## 1. Profundidad del modelo (principio II)

- **Decisión:** Académico es un subdominio de **soporte**: CRUD detrás de sus puertos, sin agregado elaborado ni eventos. El dominio tiene solo lo que hace falta para que las reglas se prueben sin dobles:
  - **`EstadoPeriodo`** (enum): `PLANIFICACION`, `EN_APROBACION`, `VIGENTE`, `CERRADO`, con `siguiente()` (vacío en `CERRADO`).
  - **`PeriodoAcademico`** (record): código, nombre, inicio, fin y estado. El constructor valida el formato del código, el nombre no vacío (hasta 120 caracteres) y que el fin sea posterior al inicio. Ofrece:
    - `corregido(nombre, inicio, fin)`: lanza `PeriodoCerrado` si está cerrado;
    - `conEstado(destino, tieneVersion)`: si el destino es el estado actual, devuelve el mismo; si es el siguiente, lo cambia; si no, lanza `TransicionNoPermitida`. Salir de `PLANIFICACION` sin versión lanza `PeriodoSinVersion`. En `CERRADO`, `PeriodoCerrado`.
- **Por qué:** el orden de los estados, el cierre y «salir de Planificación exige versión» son las tres reglas de la historia; en el dominio se prueban solas. El resto es guardar y leer.
- **Alternativa:** un agregado con eventos (`PeriodoAbierto`, `EstadoCambiado`). No tienen consumidor todavía (principio X); el registro cubre la trazabilidad (§6).

## 2. La versión del periodo la guarda Normativa (FR-002)

- **Decisión:** Académico **no guarda** la versión del periodo. La pide a Normativa por `ConsultarNormativa.periodo(codigo)` cada vez que la necesita: al responder un periodo, al listar y al comprobar que puede salir de Planificación. Normativa es la única fuente (`AsignacionNormativa`, modelo de dominio §5.1).
- **Por qué:** con una copia en Académico, las dos podrían diferir si una de las dos escrituras falla, y haría falta reconciliarlas. Las dos consultas viven en el mismo proceso y la misma base: el costo es una consulta por periodo, con decenas de periodos como mucho.
- **Alternativa:** copiar el número en `periodo_academico.version`. Más rápido al listar, pero con dos fuentes de verdad.

## 3. Abrir el periodo sin una transacción entre dos servicios (principio V)

- **Decisión:** `AbrirPeriodoServicio` no abre una transacción alrededor de la llamada a Normativa:
  1. lee el periodo (404 si no existe) y rechaza un periodo cerrado (`PeriodoCerrado`);
  2. pide a Normativa la versión actual del periodo: si ya es la misma, termina sin cambios; si es otra, `PeriodoConVersion`;
  3. llama a `AsociarVersionAPeriodo.asociar(codigo, version, quien)`, que hace su propia transacción y su `VERSION_ASOCIADA`;
  4. en una transacción de Académico, guarda el registro `PERIODO_ABIERTO` con la versión.
- **Por qué es seguro sin una transacción común:**
  - un periodo solo sale de Planificación con versión, y solo se cierra desde Vigente: un periodo sin versión no puede estar cerrado ni cerrarse entre el paso 1 y el 3;
  - Normativa garantiza que un periodo tenga una sola versión (`periodo` único y `bloquearPeriodo`): dos aperturas simultáneas con versiones distintas dejan una y la otra recibe `PeriodoConVersion`;
  - si el paso 4 falla, la asociación queda hecha y Normativa ya tiene su `VERSION_ASOCIADA` con el usuario: la trazabilidad no se pierde, y abrirlo otra vez con la misma versión no cambia nada.
- **Cuidado:** `TransaccionSpring` de Normativa usa la plantilla del proceso con propagación `REQUIRED`: si Académico la llamara dentro de su propia transacción, las dos se unirían. Una prueba de integración comprueba que, si falla el registro de Académico, la asociación de Normativa queda confirmada (cada servicio con su transacción).
- **Alternativas:** una transacción que abarque los dos servicios (la prohíbe el principio V); un evento `PeriodoAbierto` que Normativa escuche (asíncrono: la pantalla no sabría si la versión quedó, y no hay razón para la espera).

## 4. Llamada a Normativa por su interfaz publicada

- **Decisión:** un puerto de salida de Académico, `NormativaDelPeriodo`, con `versionDe(codigo): OptionalInt` y `asociar(codigo, version, quien)`. Lo implementa el adaptador `NormativaDesdePuertos` (`adaptadores/salida`), que usa solo `ec.mdd.normativa.puertos.entrada`:
  - `PeriodoSinVersion` → vacío;
  - `PeriodoConVersion` → `PeriodoConOtraVersion` de Académico;
  - `VersionNoEncontrada` → `VersionInexistente` de Académico.
- `build.gradle` de Académico gana `implementation project(':servicios:normativa')`, como Usuarios con Roles. `ModulosTest` del proceso `datos` falla si Académico usa algo fuera de la interfaz `api`.
- **Por qué:** el dominio y la aplicación de Académico no conocen las clases de Normativa (principio V): si Normativa se separa en su propio proceso, solo cambia el adaptador por un cliente HTTP con plazo de espera.
- **Permiso en Normativa:** la llamada dentro del proceso no pasa por el `@PreAuthorize` del controlador de Normativa. La autorización es la de la operación de Académico (`DISTRIBUTIVO:ESCRITURA`, §7). Hoy solo el Vicerrector académico la tiene, y también tiene `PARAMETROS:ESCRITURA` (D-67). FR-007 de la especificación se ajusta: se quita la frase que exigía además la escritura de «Parámetros y normativa».

## 5. Errores (ADR-016, principio XIII)

| Código | Estado | Cuándo |
|---|---|---|
| `ACADEMICO_CODIGO_REPETIDO` | 409 | Registrar un periodo con el código de otro. |
| `ACADEMICO_PERIODO_CERRADO` | 409 | Corregir, abrir o cambiar el estado de un periodo cerrado. |
| `ACADEMICO_TRANSICION_NO_PERMITIDA` | 409 | Saltar un estado o retroceder. La descripción es la del catálogo, fija; la pantalla ya muestra el estado actual y solo ofrece el siguiente. |
| `ACADEMICO_PERIODO_SIN_VERSION` | 409 | Pasar a En aprobación un periodo sin versión de normativa. Pide abrirlo antes. |
| `ACADEMICO_PERIODO_CON_VERSION` | 409 | Abrir con otra versión un periodo que ya tiene una. Traduce `PeriodoConVersion` de Normativa (no reenvía su código). |

- **Datos inválidos** (código con otro formato, nombre vacío, fin antes del inicio, versión que no existe): 400 `COMUN_SOLICITUD_INVALIDA` con un detalle por campo (`codigo`, `nombre`, `fin`, `version`), en español.
- **Periodo que no existe** (en la ruta): 404 `COMUN_RECURSO_NO_ENCONTRADO`.
- `ErroresAcademico` registra los cinco códigos y una prueba los compara con `contratos/comun/errores.md`, como `ErroresNormativa`.

## 6. Registro (FR-008, principio VII)

- **Tabla `academico.registro_cambio`**, con la misma forma que la de Normativa: `id`, `momento`, `usuario`, `accion` y `detalle` (`jsonb`).
- **Acciones:**
  - `PERIODO_REGISTRADO`: el código, el nombre y las fechas;
  - `PERIODO_CORREGIDO`: cada cambio con su campo, su valor anterior y el nuevo; sin cambios no se registra;
  - `PERIODO_ABIERTO`: la versión;
  - `PERIODO_ESTADO_CAMBIADO`: el estado anterior y el nuevo.
- **Usuario:** el de la sesión (`sub` del token).
- **Alternativa:** publicar a la bitácora (RF-44). La bitácora no existe todavía; cuando llegue, leerá de los registros de cada servicio o de sus eventos (como en Normativa).

## 7. Quién escribe los periodos: D-69 (aceptada)

- **Decisión (D-69, aceptada por el autor el 2026-10-21):**
  - Los periodos académicos se leen con `DISTRIBUTIVO:LECTURA` y se escriben (registrar, corregir, abrir y cambiar el estado) con `DISTRIBUTIVO:ESCRITURA`, la funcionalidad «Distributivo del periodo» de D-65. La matriz no cambia: escribe solo el Vicerrector académico.
  - Abrir el periodo se hace desde Académico. La pantalla «Normativa» pierde el diálogo «Asociar a un periodo» de F04-H02. La operación HTTP `asociarPeriodo` de Normativa se queda, para las pruebas de punta a punta y la integración, pero ya no la usa la SPA.
- **Por qué:**
  - la historia y la pregunta 4 del modelo de dominio dicen que lo hace el Vicerrectorado, y el autor eligió no cambiar la matriz (aclaración del 2026-10-21);
  - «Catálogos y datos maestros» (`CATALOGOS`) es del Administrador del sistema; dárselo al Vicerrector le daría también las carreras, las mallas y los demás datos maestros;
  - con el diálogo de Normativa, un periodo podría quedar con versión sin existir en Académico, o sin pasar por sus reglas (cerrado, orden de estados).
- **Alternativas:** el Vicerrector gana `CATALOGOS:ESCRITURA` (más permisos de los que pide la historia); una funcionalidad nueva «Periodos» en la matriz (cambia D-65 para un solo rol); dejar el diálogo de Normativa (dos caminos para lo mismo).
- **Límite conocido:** la operación HTTP de Normativa no comprueba que el periodo exista en Académico, porque Normativa no puede depender de Académico sin un ciclo. Solo la pueden llamar quienes tienen `PARAMETROS:ESCRITURA` (el Administrador y el Vicerrector).

## 8. Contrato (`contratos/academico/openapi.yaml`, nuevo)

| Operación | Ruta | Permiso | Respuestas |
|---|---|---|---|
| `listarPeriodos` | `GET /api/academico/periodos` | `DISTRIBUTIVO:LECTURA` | 200 |
| `registrarPeriodo` | `POST /api/academico/periodos` | `DISTRIBUTIVO:ESCRITURA` | 201 · 400 · 409 `ACADEMICO_CODIGO_REPETIDO` |
| `consultarPeriodo` | `GET /api/academico/periodos/{codigo}` | `DISTRIBUTIVO:LECTURA` | 200 · 404 |
| `corregirPeriodo` | `PUT /api/academico/periodos/{codigo}` | `DISTRIBUTIVO:ESCRITURA` | 200 · 400 · 404 · 409 `ACADEMICO_PERIODO_CERRADO` |
| `abrirPeriodo` | `PUT /api/academico/periodos/{codigo}/normativa` | `DISTRIBUTIVO:ESCRITURA` | 200 · 400 · 404 · 409 `ACADEMICO_PERIODO_CERRADO` o `ACADEMICO_PERIODO_CON_VERSION` |
| `cambiarEstadoPeriodo` | `PUT /api/academico/periodos/{codigo}/estado` | `DISTRIBUTIVO:ESCRITURA` | 200 · 400 · 404 · 409 `ACADEMICO_PERIODO_CERRADO`, `ACADEMICO_TRANSICION_NO_PERMITIDA` o `ACADEMICO_PERIODO_SIN_VERSION` |

- Todas responden también 401, 403, 500 y 503 comunes.
- **`cambiarEstadoPeriodo` recibe el estado de destino**, no «avanzar»: un doble clic, o dos pestañas, no lo hace avanzar dos pasos. Pedir el estado en que ya está responde 200 sin cambios.
- **El código no cambia** (`corregirPeriodo` solo recibe nombre y fechas): Normativa y las demás partes lo usan como referencia.
- **Formato del código:** el mismo patrón de Normativa (`^[A-Za-z0-9._-]{1,20}$`).
- La SPA genera `src/api/generado/academico.d.ts` con `generar:api`.

## 9. La pantalla «Periodos» (FR-010)

- **Menú:** entrada «Periodos» con `DISTRIBUTIVO:LECTURA`.
- **Lista:** una tabla con código, nombre, inicio, fin, estado y versión de normativa («Sin versión» si no tiene), en español.
- **Con `DISTRIBUTIVO:ESCRITURA`:**
  - **«Registrar periodo»:** un diálogo con código, nombre, inicio y fin;
  - **«Corregir»:** el mismo diálogo, sin el código, en un periodo no cerrado;
  - **«Abrir»:** solo en Planificación sin versión; un diálogo con la lista de versiones de normativa (`listarVersiones`, número, código y si ya rige otros periodos);
  - **«Pasar a <siguiente estado>»:** un botón con el estado siguiente; deshabilitado en Planificación sin versión, con el motivo. Pasar a Cerrado pide confirmación, porque no se puede deshacer.
- **Errores:** un 400 marca cada campo con su motivo; un 409 se muestra con su título y su descripción.
- **Sin escritura,** la misma tabla sin acciones.
- **«Normativa»:** se quita el diálogo «Asociar a un periodo» y su prueba (D-69). «Rige: 2025-1» se queda.

## 10. Glosario (principio III)

- El glosario §7.3 tiene «Periodo académico» con sus estados, pero no dice qué es «abrir» el periodo ni el orden de los estados.
- **Decisión, aprobada por el autor el 2026-10-21 y ya aplicada (copia en `modelo_dominio.md.antes-F02-H01`):** en «Periodo académico» se agregó: «Se abre al elegir su versión de normativa, mientras está en Planificación. Sus estados solo avanzan, en este orden: Planificación, En aprobación, Vigente y Cerrado; un periodo cerrado no cambia.».
- En el código se usan `PeriodoAcademico`, `EstadoPeriodo` y `abrir`; «ciclo» no se usa (MD-10).

## 11. Pruebas

- **Dominio, sin dobles:** las validaciones de `PeriodoAcademico`; `conEstado` con cada par de estados (16 casos), con y sin versión; `corregido` en cada estado.
- **Aplicación, con Mockito:**
  - registrar (código repetido, registro);
  - corregir (cerrado, sin cambios sin registro, registro con los cambios);
  - abrir (no existe, cerrado, misma versión sin cambios, otra versión, versión inexistente, registro con la versión, y que no llama a Normativa dentro de su transacción);
  - cambiar el estado (sin versión, salto, retroceso, el mismo estado sin cambios, registro).
- **Adaptadores, con Testcontainers:**
  - el repositorio guarda y lee; el código único; `FOR UPDATE` al cambiar el estado;
  - `NormativaDesdePuertos` contra la Normativa real del proceso: abrir deja la versión en Normativa, y la versión ya no se puede editar (`VersionEnUso`);
  - si falla el registro de Académico después de asociar, la asociación de Normativa queda (cada servicio con su transacción);
  - la migración V2 sobre el esquema vacío.
- **Contrato:** las seis operaciones con swagger-request-validator, `VerificadorDePermisos` y `ErroresAcademicoTest`.
- **Arquitectura:** `ModulosTest` del proceso `datos` con la dependencia nueva de Académico hacia la interfaz `api` de Normativa.
- **SPA:**
  - Vitest: la lista, registrar, corregir, abrir con la lista de versiones, avanzar con la confirmación de Cerrado, el 400 por campo, los 409, y el solo lectura sin escritura; «Normativa» sin el diálogo de asociar;
  - Playwright con axe sobre «Periodos» y sus diálogos.
- **Punta a punta:** un script nuevo, `deploy/probar_periodos.sh`, en el perfil local y en `perfil-consolidado`:
  1. `vicerrectorado.demo` registra un periodo de prueba (`prueba-<aleatorio>`), en Planificación y sin versión;
  2. pasarlo a En aprobación sin versión da 409 `ACADEMICO_PERIODO_SIN_VERSION`;
  3. crea una versión de normativa a partir de la 1, abre el periodo con ella, y la consulta del periodo en Normativa dice esa versión;
  4. editar esa versión da 409 `NORMATIVA_VERSION_EN_USO`, y abrir el periodo con otra versión da 409 `ACADEMICO_PERIODO_CON_VERSION`;
  5. saltar de Planificación a Vigente da 409 `ACADEMICO_TRANSICION_NO_PERMITIDA`;
  6. lo avanza a En aprobación, Vigente y Cerrado;
  7. corregir el nombre o cambiar el estado del periodo cerrado da 409 `ACADEMICO_PERIODO_CERRADO`;
  8. `rectorado.demo` (`DISTRIBUTIVO:LECTURA` sin escritura) lo ve y recibe 403 al intentar registrar; `docente.demo` (sin `DISTRIBUTIVO`) recibe 403 al listar;
  9. el registro de Académico tiene las cuatro acciones con el usuario.

  Al terminar, aunque un paso falle, borra en la base solo lo que creó: el periodo de prueba y sus registros en Académico, su asignación y su `VERSION_ASOCIADA` en Normativa, y la versión que creó con sus filas, reglas, parámetros y registros (como research §12 de F04-H02).
- **`probar_normativa.sh` no cambia:** sigue usando la operación HTTP de Normativa (D-69).
