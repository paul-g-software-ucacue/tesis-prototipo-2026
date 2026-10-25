# Research: Registrar los cargos y los proyectos aprobados

Fase 0 del plan de F03-H05 (issue #39). Cada punto dice la decisión, por qué y qué se descartó.

## 1. Dónde viven los cargos y los proyectos

- **Decisión:** en el servicio de Docentes (04), junto al docente. Son dos tablas nuevas, `cargo` y `proyecto`, en `V3__cargos_y_proyectos.sql`. Comparten el `registro_cambio` de Docentes, con cuatro acciones más.
- **Por qué:** la ERS §9 pone en el servicio 4 «registro del docente, ficha académica, proyectos aprobados». El glosario pone el cargo en Datos maestros, y sus dos reglas fuertes, que el docente esté activo a tiempo completo y que tenga cuarto nivel, son datos del docente. Así se validan en una sola transacción, sin llamar a otro módulo.
- **Descartado:**
  - Los cargos en Académico, junto a la estructura orgánica. Para validarlos tendría que pedirle a Docentes el estado, la dedicación y los títulos del docente: el camino inverso al que ya existe.
  - Un servicio nuevo: el principio X no lo justifica para dos tablas.

## 2. Docentes y Académico, otra vez por `consultarReferencias`

- **Decisión:** `ReferenciasDeAcademico` suma dos listas, y Docentes las usa para validar y para mostrar sus nombres:
  - `unidades`: código, nombre, tipo, gestión autorizada y si está activa;
  - `periodos`: código, nombre, inicio y fin.

  En el contrato de Académico es un cambio compatible: la respuesta de `consultarReferencias` gana dos propiedades. `ReferenciasDeDocentes` (`consultarReferenciasDeDocentes`) suma las mismas dos listas para la SPA.
- **Por qué:** `ConsultarEstructura` y `ConsultarPeriodos` devuelven tipos del dominio de Académico (`UnidadOrganica`, `PeriodoAcademico`), que `ModulosTest` prohíbe a Docentes (principio V). `ConsultarReferencias` ya es la API publicada de Académico para Docentes (F03-H04), con tipos propios en `puertos.entrada`.
- **Descartado:**
  - Dos puertos nuevos en Académico: hay más interfaces para la misma lectura.
  - Copiar las unidades y los periodos en Docentes: el mismo dato en dos lugares.

## 3. El catálogo de cargos: los 16 de gestor (propuesta D-73)

- **Problema:** el diccionario da los cargos RECTOR, VICERRECTOR, COORDINADOR_ACADEMICO_INSTITUCIONAL, COORDINADOR_SEDE, COORDINADOR_CARRERA y MIEMBRO_COMISION, pero los de gestor solo como «G06_GESTOR_VINCULACION … G21_GESTOR_ESTADISTICAS, prefijo con el código de gestión autorizada». Los datos de prueba usan 9 de esos 16 códigos. El principio XIV no deja inventar los otros 7.
- **Propuesta para el autor (D-73):** los 16 códigos, formados como los 9 de los datos de prueba: el código de la gestión de POL 6 y el núcleo de su nombre.

  | Gestión | Código | En los datos de prueba |
  |---|---|---|
  | G06 Gestor de Vinculación con la Sociedad | `G06_GESTOR_VINCULACION` | Sí |
  | G07 Gestor de Investigación, Desarrollo Tecnológico e Innovación | `G07_GESTOR_INVESTIGACION` | Sí |
  | G08 Gestor de Centro de Idiomas | `G08_GESTOR_IDIOMAS` | — |
  | G09 Gestor de Centro de Formación Integral y Servicios Especializados | `G09_GESTOR_FORMACION_INTEGRAL` | — |
  | G10 Coordinador de Aseguramiento de la Calidad | `G10_COORDINADOR_CALIDAD` | Sí |
  | G11 Secretario General Académico | `G11_SECRETARIO_GENERAL` | Sí |
  | G12 Procurador | `G12_PROCURADOR` | — |
  | G13 Gestor de Bienestar Institucional | `G13_GESTOR_BIENESTAR` | Sí |
  | G14 Gestor Administrativo Financiero | `G14_GESTOR_ADMINISTRATIVO_FINANCIERO` | — |
  | G15 Gestor de Planificación Estratégica | `G15_GESTOR_PLANIFICACION` | Sí |
  | G16 Gestor Unidad de Talento Humano | `G16_GESTOR_TALENTO_HUMANO` | — |
  | G17 Gestor Unidad de Servicios de Biblioteca | `G17_GESTOR_BIBLIOTECA` | Sí |
  | G18 Gestor Unidad de Comunicación | `G18_GESTOR_COMUNICACION` | — |
  | G19 Gestor Unidad de Relaciones Internacionales e Institucionales | `G19_GESTOR_RELACIONES_INTERNACIONALES` | — |
  | G20 Gestor Unidad de TIC | `G20_GESTOR_TIC` | Sí |
  | G21 Gestor Unidad de Estadísticas | `G21_GESTOR_ESTADISTICAS` | Sí |

  El catálogo `CARGOS` de `diccionario-de-datos.yaml` cambia su fila de rango por los 16 valores, así que la plantilla y el diccionario de la carga 8 los listan (principio VI). `ValoresPermitidosTest` compara la enumeración `TipoDeCargo` con ese catálogo.
- **Descartado:**
  - Aceptar cualquier `Gxx_…` con el prefijo bien puesto: el código del cargo no sería un catálogo y podría repetirse con otra escritura.
  - Formar el código del cargo con el nombre de la gestión completo: pasa de los 40 caracteres y no coincide con los datos de prueba.

## 4. La unidad de cada cargo

El diccionario pide la unidad «salvo coordinador de carrera» y la marca como clave foránea a la estructura orgánica. A eso se suma lo que el propio diccionario dice de cada cargo:

| Cargo | Unidad | Carrera |
|---|---|---|
| Rector, vicerrector, coordinador académico institucional, coordinador de sede | Obligatoria: una unidad activa de la estructura | No |
| Gestor (G06 a G21) | Obligatoria: la unidad activa cuya gestión autorizada es la del cargo («prefijo con el código de gestión autorizada») | No |
| Miembro de comisión | Obligatoria: una unidad activa de tipo comisión | No |
| Coordinador de carrera | Opcional | Obligatoria: una carrera de Académico |

- No se exige tipo de unidad al rector, al vicerrector, al coordinador académico institucional ni al coordinador de sede, porque el diccionario no lo define (principio XIV). En los datos de prueba, RECT y VRA son unidades de gobierno con su gestión G01 y G02.
- Las reglas de la tabla son de `Cargo` (dominio). Reciben las unidades como `Referencias`, igual que la carrera base del docente.

## 5. Las reglas del cargo y su advertencia

- **Rechazos (409, negocio):**
  - un docente que no está activo o que no es de tiempo completo: `DOCENTES_DOCENTE_NO_HABILITADO`;
  - la misma clave (docente, cargo y fecha de designación): `DOCENTES_CARGO_REPETIDO`;
  - un segundo coordinador vigente para una carrera (RN-12): `DOCENTES_COORDINADOR_VIGENTE`.
- **Datos inválidos (400, `COMUN_SOLICITUD_INVALIDA`, con su campo):** la unidad o la carrera que falta, que no existe o que no corresponde al cargo; un fin de funciones que no es posterior a la designación o que pasa de dos años después (POL 5.4); un documento vacío o de más de 60 caracteres.
- **Advertencia (RN-20):** se calcula, no se guarda. Un coordinador o un gestor cuyo docente no tiene ningún título de cuarto nivel tiene la advertencia `CUARTO_NIVEL` con su texto. La respuesta de registrar y de corregir la trae, y también cada cargo de la lista. Si el docente agrega una maestría a su ficha, la advertencia desaparece sola.
- **RN-12 sin carreras de más:** «vigente» es el estado. El índice único parcial `cargo (carrera) where cargo = 'COORDINADOR_CARRERA' and estado = 'VIGENTE'` lo garantiza en la base; el servicio lo comprueba antes, bajo `LOCK TABLE cargo IN SHARE ROW EXCLUSIVE MODE` (como los docentes repetidos), para responder con el código y no con un error de la base.
- **Descartado:**
  - Guardar la advertencia: quedaría vieja cuando la ficha cambie.
  - Rechazar por cuarto nivel: la aclaración 2 pide advertir.

## 6. Las reglas del proyecto

- **Rechazos:** el código repetido (`DOCENTES_PROYECTO_REPETIDO`, 409).
- **Datos inválidos (400, con su campo y su elemento):**
  - el código con otros caracteres o con más de 30;
  - el nombre vacío o con más de 250;
  - sin carreras, una carrera repetida o que no existe;
  - la resolución vacía o con más de 60;
  - el fin de vigencia que no es posterior al inicio;
  - el director o un participante que no es un docente registrado;
  - un participante repetido;
  - unas horas que no son mayores que 0 o que no caben en 5 caracteres;
  - participantes que suman más que las horas aprobadas.
- **Las horas:** decimales mayores que 0, con hasta 5 caracteres: tres dígitos enteros y uno decimal (`999.9`). En la base son `numeric(4,1)`. La suma se compara en `BigDecimal`.
- **Las listas:** `carreras` y `participantes` van en `jsonb` dentro de la fila, como la ficha del docente (F03-H04): se leen y se guardan enteras. En pantalla y en la API, las carreras admiten hasta 20 y los participantes hasta 40. Con eso caben los 200 y los 1000 caracteres que el diccionario da a esas columnas en el archivo: un código de carrera ocupa unos 17 caracteres y un participante, unos 24.
- **Descartado:** una tabla de participantes. Por ahora ninguna consulta busca proyectos por participante; si F06 la necesita, se agrega entonces.

## 7. Asignable contra un periodo

- **Decisión:** `listarProyectos?periodo=2026-I` devuelve cada proyecto con `asignable` y `motivos` (`ESTADO`, `VIGENCIA` o los dos). Las fechas del periodo vienen de `ReferenciasAcademicas.periodos()`. La regla es un método puro del dominio, `Proyecto.asignableEn(inicio, fin)`: el estado es aprobado o en ejecución, y `vigenciaInicio <= fin` y `vigenciaFin >= inicio`, con las fechas inclusive. Un periodo que no existe responde 400 con el detalle `periodo`.
- **Sin periodo**, la lista no dice si cada proyecto es asignable.
- **Descartado:** guardar si un proyecto es asignable. Depende del periodo que se consulta, así que se calcula.

## 8. Permisos (D-70, aclaración 1)

- **Escriben** `[DOCENTES:ESCRITURA, CATALOGOS:ESCRITURA, DISTRIBUTIVO:ESCRITURA]`: registrar y corregir cargos y proyectos.
- **Leen** `[DOCENTES:LECTURA, CATALOGOS:LECTURA, DISTRIBUTIVO:LECTURA, PROYECTOS:LECTURA]`: listar y consultar.
- `consultarReferenciasDeDocentes` suma `PROYECTOS:LECTURA` a sus lecturas, porque la pantalla de proyectos la necesita para los nombres.
- `PermisosDocentesTest` (`VerificadorDePermisos`) cubre cada permiso de cada operación. La matriz no cambia.

## 9. Registro de cambios

`V3` amplía el `check` de `registro_cambio.accion` con cuatro acciones, y cada cambio guarda el valor anterior y el nuevo (`Cambios`, F03-H04):
- `CARGO_REGISTRADO`;
- `CARGO_CORREGIDO`, que incluye finalizarlo;
- `PROYECTO_REGISTRADO`;
- `PROYECTO_CORREGIDO`, que incluye cambiar su estado.

El detalle no lleva nombres ni correos, solo identificaciones y códigos (principio IX).

## 10. Errores del catálogo (D-72)

Cuatro códigos `DOCENTES_*` nuevos, con sus 8 columnas en `contratos/comun/errores.md` y su entrada en `ErroresDocentes`:

| Código | Estado | Clasificación | Causa |
|---|---|---|---|
| `DOCENTES_CARGO_REPETIDO` | 409 | Negocio | Registrar un cargo con el mismo docente, cargo y fecha de designación que otro. |
| `DOCENTES_COORDINADOR_VIGENTE` | 409 | Negocio | Dejar vigente un segundo coordinador de una carrera (RN-12). |
| `DOCENTES_DOCENTE_NO_HABILITADO` | 409 | Negocio | Dar un cargo a un docente que no está activo o no es de tiempo completo (POL 5.4). |
| `DOCENTES_PROYECTO_REPETIDO` | 409 | Negocio | Registrar un proyecto con el código de otro. |

Los mensajes no repiten identificaciones (principio IX). Un cargo o un proyecto que no existe responde 404 (`COMUN_RECURSO_NO_ENCONTRADO`), como un docente.

## 11. La SPA (principio XIV)

- Dos pantallas, «Cargos» y «Proyectos», en el menú después de «Docentes», con las cuatro lecturas.
- **«Cargos»:**
  - una lista con filtro por estado y por docente, con la advertencia junto al cargo;
  - el diálogo de registrar y corregir, con el docente elegido de la lista de Docentes;
  - el cargo, de su catálogo con nombres en español;
  - la unidad, de las de Académico que corresponden al cargo elegido, y la carrera, solo para el coordinador de carrera.
- **«Proyectos»:**
  - el periodo elegido arriba, con el último como valor inicial, y la lista con «Asignable» o su motivo;
  - el diálogo con las carreras y los participantes como listas editables, igual que la ficha del docente.
- **Validación:** `validacionDeCargo.ts` y `validacionDeProyecto.ts` reúsan `filtrar` de `validacionDeDocente.ts`, que se mueve a `pantallas/comun/validacion.ts`. El código del proyecto acepta mayúsculas, dígitos y guion, y lo demás pasa a mayúsculas o se quita. Las horas admiten un decimal. El documento y la resolución, hasta 60. Antes de enviar se marca cada campo con su motivo.

## 12. Glosario (propuesta para el autor)

En `modelo_dominio.md` §7.2:
- **Cargo (cambia):** «Puesto institucional que un docente activo a tiempo completo ocupa desde su designación y que da horas de gestión: rector, vicerrector, coordinadores, gestores de las gestiones autorizadas o miembro de comisión, este sin horas. Está vigente o finalizado; nunca se borra. Es un dato de la carga 8.» Cambia porque decía «en el periodo»: un cargo vale por sus fechas, no por un periodo.
- **Proyecto aprobado (cambia):** agrega «o de servicio comunitario» y «está aprobado, en ejecución, finalizado o suspendido».
- **Participante (nuevo):** «Docente que trabaja en un proyecto aprobado, con su rol (director, investigador, docente de apoyo o tutor de prácticas) y sus horas semanales.» `Participante`, Datos maestros, carga 9.
- **Proyecto asignable (nuevo):** «Proyecto aprobado o en ejecución cuya vigencia se cruza con las fechas del periodo. Solo él recibe horas de investigación, vinculación o prácticas.» `asignableEn`, Datos maestros, RF-18, RN-25.
- **Requisito del cargo (nuevo):** «Lo que un coordinador o un gestor debe tener para su designación: por ahora, un título de cuarto nivel registrado. Si falta, el cargo se registra con una advertencia.» `AdvertenciaDeCargo`, Datos maestros, RN-20.

## 13. Punta a punta y datos de prueba

- `deploy/probar_cargos_proyectos.sh`, como `probar_docentes.sh`:
  - registra con un prefijo al azar lo que necesita de las cargas 01, 02, 03 y 06 de prueba;
  - registra los 21 cargos y los 11 proyectos (SC-002);
  - comprueba la advertencia, los cuatro 409, los proyectos asignables en 2026-I y en 2026-II (SC-003) y la lectura sin escritura del Responsable de consolidación;
  - al terminar, borra en la base solo lo que creó.
- **Escala:** cientos de cargos y proyectos; las listas se filtran en memoria, como los docentes.
- **Meta:** listar los proyectos de un periodo en menos de 300 ms; la prueba falla desde 500 ms.
- **Recursos de prueba:** las cargas 01, 03, 08 y 09 se copian a `backend/servicios/docentes/src/test/resources/docentes/datos-de-prueba/`, junto a las 02, 04 y 06 que ya están.
