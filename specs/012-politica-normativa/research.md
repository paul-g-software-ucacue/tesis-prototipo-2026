# Research: La política 2024 como primera versión de normativa (F04-H01)

Decisiones técnicas del plan. Cada sección tiene la decisión, por qué y las alternativas. Parte de lo que ya existe en `main` (`bfb0e82`):
- el esqueleto del servicio Normativa (`backend/servicios/normativa`, esquema `normativa` con solo V1), ensamblado en el proceso `datos`;
- la ruta `/api/normativa/**` del API Gateway hacia `datos`;
- los permisos de F05-H02: la funcionalidad `PARAMETROS`, que hoy solo lee y escribe el Administrador (D-65), `x-mdd-permiso`, `@PreAuthorize` y `VerificadorDePermisos` (ADR-021);
- en la SPA, `RutaConPermiso`, el menú según los permisos y el patrón de la pantalla «Roles».

## 1. Normativa es un subdominio central: DDD táctico

- **Decisión:** el dominio de Normativa sigue el modelo táctico de `sdd/modelo_dominio.md` §5.1, en lo que toca a esta historia:
  - **`VersionNormativa`**, el agregado: número, código, origen, fecha de vigencia, tipo de IST, tabla de carga horaria y reglas. Se construye con sus invariantes y no tiene operaciones que lo cambien: editar o crear versiones es F04-H02.
  - **`TablaCargaHoraria`** y **`FilaCargaHoraria`**, objetos de valor: una fila por perfil y tipo de IST (o `TODOS`).
  - **`Regla`**, objeto de valor: identificador RN-xx, severidad opcional y sus parámetros con nombre.
  - **`TipoIst`** (T1 a T5) y **`Severidad`** (dura o blanda): enumeraciones.
  - **`VersionNormativaCreada`**, el evento de dominio (modelo de dominio §6), no tiene clase todavía: ningún código de producción crea una versión en esta historia. Llega con F04-H02, junto con la creación (principio X).
- **Invariantes de una fila** (se comprueban al construirla, también al leerla de la base):
  - las horas son enteras y no negativas; un valor que la política no da queda «sin valor», no en cero;
  - el mínimo de clase ≤ las horas de clase ≤ el máximo de clase;
  - clase + otras de docencia + gestión + máximo de investigación y vinculación ≤ la dedicación total.
- **Invariantes de la tabla:** un perfil y un tipo de IST no se repiten, y un perfil aplica a `TODOS` o a tipos concretos, no a los dos (`aplicaA`). Así, `filaPara(perfil, tipoIst)` da siempre una sola fila: la del tipo, o la de `TODOS`.
- Las 22 filas de la carga 7 cumplen todas las invariantes. Lo comprueba la prueba de §3, que reconstruye la versión 1 desde la base.
- **Por qué:** el principio II exige DDD táctico en los subdominios centrales, y las invariantes atrapan un error de digitación en la política antes de que llegue a un distributivo.
- **Alternativa:** un CRUD de filas como en Roles (subdominio genérico). Se descarta porque Normativa es central y sus cifras gobiernan el distributivo.

## 2. Cómo nace la versión 1: por una migración

- **Decisión:** `V2__politica_2024.sql`, en el esquema `normativa`, crea las tablas e inserta la versión 1: su cabecera, las 22 filas de la carga 7, las reglas con sus parámetros (§4) y el registro de su creación (§5).
- **Por qué:**
  - existe desde la instalación, en la misma transacción que el esquema (FR-001);
  - Flyway la aplica una sola vez: reiniciar o desplegar no la repite ni la cambia (FR-006, SC-003);
  - es la misma técnica que la matriz D-16 en Roles (`V2__matriz_d16.sql`), que el autor ya revisó;
  - la versión anterior del equipo sigue arrancando: Flyway ignora como futuras las migraciones que no conoce (comprobado en F05-H02).
- **Las invariantes no se saltan:** la prueba de §3 lee la versión 1 con el repositorio, que la reconstruye con el agregado. Una cifra mal escrita en la migración hace fallar la construcción.
- **Alternativa:** crearla al arrancar, desde un archivo de recursos y con el caso de uso. Es el camino que usará la carga 7 por archivo (F01, MD-13), pero exige ordenar el arranque, cuidar que no se duplique en dos arranques simultáneos y publicar un evento sin consumidores. Para una versión fija, la migración es más simple (principio X).

## 3. La prueba contra la política (SC-001)

- **Decisión:** `PoliticaDe2024IntegrationTest` (Testcontainers) migra el esquema y compara la versión 1, fila por fila y valor por valor, con una copia de `07_politica_carga_horaria.csv` en los recursos de prueba (`politica-2024.csv`). También comprueba el tipo de IST, los parámetros de §4 y que la versión se reconstruye con el agregado.
- **Por qué:** el escenario dice «las 22 filas de la política 2024 de la carga 7». La comparación contra el archivo de origen lo prueba sin reescribir la tabla a mano en la prueba, y la copia en el repositorio la deja fija aunque cambie el archivo de los artefactos.
- Los datos son la tabla pública de la Política SENESCYT de marzo de 2024: no hay datos personales.

## 4. Los parámetros de RN-01 a RN-15 que las filas no expresan (FR-004)

- **Decisión:** cada uno es un parámetro con nombre de una `Regla`, con la severidad del modelo de dominio §5.2 (MD-08):

| Regla | Severidad | Parámetros |
|---|---|---|
| RN-01 | — (parámetro) | `dedicacion_tiempo_completo` 40, `dedicacion_medio_tiempo` 20, `dedicacion_tiempo_parcial_maxima` 19 |
| RN-02 | Dura | `horas_maximas_docente` 40 |
| RN-05 | Blanda | `porcentaje_clase` 60, `porcentaje_otras_docencia` 40 |
| RN-06 | — (parámetro) | `t1_desde` 1, `t1_hasta` 300, `t2_desde` 301, `t2_hasta` 600, `t3_desde` 601, `t3_hasta` 1200, `t4_desde` 1201, `t4_hasta` 1800, `t5_desde` 1801, `t5_hasta` 3500 |
| RN-09 | Blanda | `estudiantes_minimos` 500, `campus_minimos` 3 (en T1 a T3, el cargo exige más de 500 estudiantes o 3 o más campus) |
| RN-10 | Blanda | `t1_minimo` 5, `t1_maximo` 7, `t2_minimo` 8, `t2_maximo` 11, `t3_minimo` 12, `t3_maximo` 15, `t4_minimo` 15, `t4_maximo` 18, `t5_minimo` 21, `t5_maximo` 25 |
| RN-13 | Dura | `porcentaje_nomina` 10 (las 16 h de docencia están en su fila) |

- RN-01 y RN-06 no tienen severidad: el modelo de dominio §5.2 los deja fuera del motor de reglas porque son parámetros.
- Las severidades marcadas como inferidas (I) en §5.2, la de RN-05 y la de RN-10, se confirman al especificar la validación del distributivo (MD-08), y se cambian con una versión nueva (F04-H02 y F04-H03).
- Los valores son decimales (`numeric`): los porcentajes y las horas caben sin perder precisión, y no hace falta un tipo por parámetro.
- **Alternativa:** una columna por parámetro en la cabecera de la versión. Cada regla nueva exigiría cambiar el esquema y el agregado; con parámetros con nombre, una versión nueva solo agrega filas.

## 5. Registro de la creación (FR-010)

- **Decisión:**
  - la tabla `normativa.registro_cambio` guarda el hecho que el modelo de dominio llama `VersionNormativaCreada`, con el momento, el usuario (`sistema` para la versión 1), la acción `VERSION_CREADA` y el detalle en `jsonb` (número, código y origen);
  - la migración inserta la fila de la versión 1, y F04-H02 la escribirá desde el caso de uso.
- **No se publica entre procesos todavía:** ningún servicio consume el evento hoy. Distributivo lo necesitará al validar, y la bitácora al leer los registros; el envío entre procesos (ADR-018) llega con su primer consumidor.
- **Por qué:** es el mismo registro que Roles (F05-H02, FR-011), y el principio X pide no armar una entrega sin consumidor.

## 6. Quién lee la normativa: D-66

- **Decisión:** el Vicerrector académico gana `PARAMETROS:LECTURA`, con alcance todo. La escritura sigue solo en el Administrador.
  - Es un cambio a la matriz D-16 de D-65, así que exige una decisión (D-66, propuesta) y una migración de Roles, `V4__vicerrector_lee_normativa.sql` (D-63).
  - `MatrizD16IntegrationTest` actualiza la fila esperada del Vicerrector.
- **Por qué:**
  - la historia la pide el Vicerrector («para no tener que digitar»);
  - es él quien abre el periodo y elige su versión (F02-H01);
  - D-16 le da «todo el distributivo», y las reglas que lo gobiernan son parte de leerlo.
- **Efecto en las sesiones:** el permiso nuevo llega al token en la próxima renovación o al volver a entrar (D-62).
- **Alternativas:** dejarlo solo para el Administrador (el Vicerrector no vería la normativa que elige para su periodo); abrirla a todos (contradice el mínimo privilegio de D-16).

## 7. Contrato y protección (ADR-015, ADR-021)

- **Decisión:** `contratos/normativa/openapi.yaml`, nuevo, con dos operaciones:
  - `listarVersiones` (`GET /api/normativa/versiones`), con número, código, origen, vigencia y tipo de IST;
  - `consultarVersion` (`GET /api/normativa/versiones/{numero}`), con la versión completa y 404 si no existe.
- Las dos llevan `x-mdd-permiso: PARAMETROS:LECTURA` y `@PreAuthorize`, y las prueba `PermisosNormativaTest` con `VerificadorDePermisos`.
- **Sin códigos de error nuevos:** el 404 es `COMUN_RECURSO_NO_ENCONTRADO`, como el de un rol inexistente en Roles.
- Normativa aplica `mdd.contrato` (las interfaces generadas) y agrega `spring-boot-starter-jdbc` y `spring-security-core`, que ya usa Roles. No necesita `jackson-databind`: la fila del registro la inserta la migración.

## 8. La pantalla «Normativa» (FR-008)

- **Decisión:** `/normativa`, de solo lectura, con `RutaConPermiso` en `PARAMETROS:LECTURA` y una entrada en el menú solo para quien la puede ver. Muestra:
  - las versiones, que hoy son una;
  - de la versión elegida, una cabecera con el número, el origen, el código, la vigencia y el tipo de IST;
  - los parámetros, agrupados por regla, con su severidad en español;
  - la tabla de carga horaria con sus 22 filas.
- **Nombres en español:** `normativa/perfiles.ts` traduce los 10 perfiles de la carga 7 (por ejemplo, `DOCENTE_TC` → «Docente a tiempo completo»), y uno desconocido se muestra legible, como los roles. «Sin valor» se muestra como «—», y `TODOS` como «Todos».
- F04-H02 agregará la edición sobre la misma pantalla.

## 9. Pruebas

- **Dominio, sin dobles:** las invariantes de la fila y de la tabla, `filaPara` con `TODOS` y con un tipo concreto, y la regla con y sin severidad.
- **Aplicación, con Mockito en los puertos:** listar y consultar versiones; una versión que no existe.
- **Adaptadores, con Testcontainers:** la comparación con la política de §3; el repositorio JDBC que reconstruye el agregado; el registro de la creación; y que la versión anterior arranque sobre el esquema nuevo, como en Roles.
- **Contrato:** las dos operaciones contra el contrato y `VerificadorDePermisos`.
- **Roles:** `MatrizD16IntegrationTest` con el permiso nuevo del Vicerrector.
- **SPA:**
  - Vitest de la pantalla: cabecera, parámetros, las 22 filas en español, «—» para un valor que la política no da, y «Acceso no autorizado» sin el permiso;
  - Playwright con axe-core.
- **Punta a punta:** `deploy/probar_normativa.sh`, nuevo, que también corre la CI en `perfil-consolidado`:
  - `vicerrectorado.demo` y `admin.demo` ven la versión 1 con 22 filas y el tipo de IST T2;
  - `docente.demo` recibe el 403 común;
  - después de reiniciar `datos`, sigue habiendo una sola versión.

## 10. Términos nuevos del glosario (principio III)

- **Decisión:** antes del código, el glosario (`sdd/modelo_dominio.md`, fuera del repositorio) ganó en §7.3 «Tabla de carga horaria» y «Perfil», y en §8 la fila «"Carga horaria" para la tabla de la política → Tabla de carga horaria» (2026-10-18).
- **Por qué:** «Carga horaria» ya es, en el glosario, el total de horas de un docente en el Distributivo (RF-27), y «Perfil» es la columna de la carga 7 que el código usa. «Alcance» ya significa «todo o solo lo propio» (D-64), así que el tipo de IST al que aplica una fila se llama `aplicaA`, no `alcanceIst`.
