# Research: Asignar actividades no lectivas

## 1. Dónde vive el catálogo (D-79)

- **Decisión:** en Normativa, una tabla `normativa.actividad_catalogo` con una fila por versión y actividad. La migración `V5` lo siembra en cada versión existente, como V4 hizo con las reglas (D-68). Una versión nueva copia las filas de la versión anterior.
- **Por qué:** el modelo de dominio lo pone dentro de `ReglasDelPeriodo`, RF-21 dice que llega sembrado con la versión, y el principio VI pide que las reglas sean datos.
- **Lo que exige cada actividad también es dato:** `tipos_de_proyecto` y `cargos`, dos listas. Vacías, no exige nada. Así Generación no tiene escrito en su código qué literal pide qué.
- **Alternativas:** una tabla sin versión: es más simple, pero F07 y RF-21 completo tendrían que migrarla. Una lista en el código de Generación: rompe el principio VI.

## 2. El contenido

- **Fuente:** el texto de los arts. 140 a 143 que trae la política de marzo de 2024, copiado literal.
- **47 actividades:** 140 b a o (14), 141 a a i (9), 142 a a i (9) y 143 a a p sin i (15).
- **Código:** artículo y literal, por ejemplo `140-b`. Es estable y se lee en la norma.
- **Lo que exige:**

| Actividades | Exigen |
|---|---|
| 140-f | Proyecto de prácticas preprofesionales o de servicio comunitario |
| 141-a a 141-i | Proyecto de investigación |
| 142-a a 142-i | Proyecto de vinculación, de prácticas preprofesionales o de servicio comunitario |
| 143-a | Cargo de rector o vicerrector |
| 143-b | Cargo de coordinador académico institucional |
| 143-c | Cargo de coordinador de sede |
| 143-d | Cargo de coordinador de carrera |
| 143-e | Cargo de gestor de investigación |
| 143-f | Cargo de gestor de vinculación |
| 143-g | Cargo de gestor de idiomas |
| 143-h | Cargo de gestor de formación integral |
| Las demás | Nada |

## 3. Las consultas entre procesos (ADR-023)

- **Normativa:** `GET /interno/normativa/periodos/{periodo}/actividades`: el catálogo de la versión que rige el periodo. Sin versión, 404.
- **Docentes:** `GET /interno/docentes/{identificacion}/proyectos-y-cargos?periodo=`: los proyectos asignables del periodo en los que participa, con código, nombre y tipo, y sus cargos vigentes. Sin datos de contacto (principio IX).
- **Por qué una sola ruta en Docentes:** el formulario y el guardado necesitan las dos cosas juntas, y así son una llamada, no dos.
- **El estado del periodo:** se reutiliza `periodoListo`, de F06-H03, para saber si se cerró.

## 4. Lo que ve el formulario

- **Decisión:** una operación `consultarActividadesPosibles(periodo, docente)` que responde cada actividad del catálogo con su componente, sus proyectos posibles y si está disponible, con su motivo.
- **Por qué:** las reglas quedan en un solo lugar, el dominio de Generación. La pantalla no compara tipos ni cargos.
- **Alternativas:** que la SPA cruce el catálogo, los proyectos y los cargos: duplica FR-014 y FR-016, y además un coordinador no tiene permiso para leer los cargos.

## 5. La regla

- **Decisión:** `ActividadDelCatalogo.exigir(proyecto, proyectosYCargos)`, una función pura del dominio, usada por el formulario y por el guardado:
  - sin exigencias, un proyecto enviado se rechaza;
  - con tipos de proyecto, exige uno de los proyectos del docente, del tipo que corresponde;
  - con cargos, exige uno vigente.
- **Por qué:** es dominio puro, se prueba sin dobles (principio VIII).

## 6. La carrera de la actividad

- **Decisión:** va en la ruta, `/parciales/{carrera}/…`. El servidor la compara con la del token mirando el permiso de escribir, como en F06-H03. Otra carrera responde 403.
- **Por qué:** quien tiene `OFERTA:ESCRITURA` sobre todo no tiene carrera en el token, así que debe poder decirla. El contrato de la sesión no trae la carrera.
- **La pantalla** la toma de la lista: `consultarActividades` devuelve las carreras que le corresponden, y siempre incluye la del coordinador, aunque aún no tenga parcial.

## 7. Las horas (FR-021, FR-022)

- **Decisión:** `HorasDelPeriodo.calcular(propuestas, actividades, oferta, paralelo)`. Una actividad suma a `asignadas`, y a `enOtrasCarreras` si es de otra carrera. Ninguna actividad se excluye por el paralelo que se mira.
- **Una actividad no se cruza con la oferta:** sus horas están en ella.
- **Al reemplazar:** el aviso se calcula sin las horas de la actividad que se reemplaza. Si no, cambiar 4 h por 6 h contaría 10 h.
- **El aviso de D-78:** `horasSuficientes` sale de las mismas horas, así que ya cuenta las actividades, sin cambiar su cálculo.

## 8. Guardar

- **Persistencia:** `generacion.actividad_no_lectiva`, con un identificador propio y un índice único por versión, carrera, docente, actividad y proyecto, con `coalesce` para el proyecto vacío. `ON CONFLICT DO UPDATE` reemplaza las horas.
- **Copias:** guarda el nombre de la actividad, su componente y el del proyecto: se muestran sin llamar a otros servicios, igual que `nombre_docente`.
- **Orden:** primero consulta y después guarda, sin abrir una transacción durante una llamada a otro proceso.
- **Registro:** `ACTIVIDAD_GUARDADA` y `ACTIVIDAD_QUITADA`, con las horas anteriores.
- **Sin bloqueo optimista:** igual que en F06-H03; llega con F07-H02.

## 9. La pantalla

- **Decisión:** una entrada nueva «Actividades no lectivas», con `LECTURA_DE_LA_OFERTA`. Una tabla por carrera, con una fila por docente, sus actividades y la suma por componente.
- **Elegir al docente:** con `/api/docentes`, que el coordinador ya puede leer, solo los activos.
- **El aviso de horas:** la respuesta de guardar trae `horasSuficientes`.
- **La actividad y el proyecto:** no usan una lista nativa, porque no parte los textos largos y se sale de la pantalla (PR #172). Se eligen en una lista que se puede buscar.

## 10. El texto «Solo lectura»

- **Decisión:** con `ESCRITURA_DE_LA_PROPUESTA` y sin escribir la oferta: «No cambias la oferta, pero propones sus docentes.» Con ninguno de los dos, sigue el texto actual.

## 11. Las pruebas

- **Dominio:** sin dobles, `ActividadDelCatalogo` y `HorasDelPeriodo`.
- **Catálogo:** una prueba de Normativa contra PostgreSQL compara las 47 filas con la lista hecha aparte (SC-001).
- **Punta a punta:** `deploy/probar_actividades.sh`, que parte de los datos de prueba y compara las horas con una suma aparte.
