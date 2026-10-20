# Research: Ajustar pesos y mínimo de afinidad y la severidad de las reglas (F04-H03)

Decisiones técnicas del plan. Cada sección tiene la decisión, por qué y las alternativas. Parte de lo que ya existe en `main` (`42b7266`), hecho en F04-H01 y F04-H02:
- el agregado `VersionNormativa`, inmutable, con `copiaDe`, `editada` y `asociadaA`;
- `Regla(identificador, severidad, parametros)`: el identificador sigue `RN-[0-9]{2}`, la severidad es `null` en las reglas que solo dan parámetros y los parámetros son al menos uno;
- las tablas `regla` (con `check (identificador ~ '^RN-[0-9]{2}$')` y la severidad opcional) y `parametro_regla` (`numeric(10, 2)`);
- `editarVersion` con la versión completa, que hoy cambia los valores de los parámetros pero no las severidades;
- `RepositorioVersionesJdbc` lee las reglas con un `JOIN` entre `regla` y `parametro_regla`, y al guardar reemplaza las reglas de la versión.

## 1. La afinidad son dos reglas con parámetros (principio X)

- **Decisión:** los pesos de afinidad son los parámetros de una regla **RN-24**, sin severidad, como RN-01 y RN-06. El mínimo de afinidad es el parámetro de la regla **RN-19**, que además tiene severidad (blanda) porque el motor la comprueba (tabla 5.2).

  | Regla | Parámetro | Por defecto |
  |---|---|---|
  | RN-24 | `cuarto_nivel_campo_especifico` | 3 |
  | RN-24 | `cuarto_nivel_campo_amplio` | 2 |
  | RN-24 | `tercer_nivel_campo_especifico` | 2 |
  | RN-24 | `tercer_nivel_campo_amplio` | 1 |
  | RN-24 | `experiencia_relacionada` | 2 |
  | RN-24 | `curso_relacionado` | 1 |
  | RN-24 | `tope_cursos` | 3 |
  | RN-19 | `minimo_afinidad` | 2 |

- **`PesosAfinidad` y `MinimoAfinidad`** (modelo de dominio §5.1) son objetos de valor que el agregado arma con esos parámetros (`version.pesosAfinidad()`, `version.minimoAfinidad()`). Distributivo los usará en F06 y F07; aquí sirven para la invariante de §4.
- **Por qué:**
  - se guardan, se copian, se editan, se versionan y se registran por el camino que ya existe, sin tablas ni operaciones nuevas;
  - la pantalla ya muestra y edita los parámetros de cada regla;
  - el modelo de dominio pone a RN-24 entre las reglas que «son parámetros», igual que RN-01 y RN-06.
- **Alternativa:** un bloque `afinidad` propio en la versión, con su tabla y su sección del contrato. Se descarta: duplicaría el mismo mecanismo de los parámetros y su edición.

## 2. Las reglas del motor que faltan (clarificación 1)

- **Decisión:** cada versión gana las reglas del motor de la tabla 5.2 que todavía no tiene, con su severidad por defecto:

  | Regla | Qué comprueba | Severidad | Parámetros |
  |---|---|---|---|
  | RN-12 | Un coordinador de carrera por carrera | Dura (I) | — |
  | RN-16 | Reemplazo de un docente con maternidad o lactancia | Blanda (I) | — |
  | RN-17 | Ningún docente sin horas de clase | Dura (I) | — |
  | RN-18 | Las horas de clase de una carrera no exceden su malla | Dura (I) | — |
  | RN-19 | Afinidad bajo el mínimo, salvo tronco común | Blanda | `minimo_afinidad` |
  | RN-22 | Solo actividades del catálogo | Dura | — |
  | RN-25 | Investigación o vinculación solo con un proyecto aprobado y vigente | Dura | — |
  | RF-24 | Asignatura-paralelo de la oferta sin propuesta | Dura (I) | — |

  Más RN-24, solo con sus parámetros (§1). En total, cada versión pasa de 7 a 16 reglas.
- **Una regla puede no tener parámetros.** `Regla` exige ahora que tenga **parámetros o severidad** (o las dos), no al menos un parámetro.
- **El identificador** admite `RN-xx` y `RF-xx`: la tabla 5.2 nombra «RF-24» a la comprobación de la oferta sin propuesta, que no tiene número RN. Cambian la expresión de `Regla`, el `check` de la tabla y el `pattern` del contrato.
- **Quedan fuera:** RN-26 (Should, F22); las filas de la tabla de carga horaria (RN-03, RN-04, RN-07, RN-08, RN-11, RN-14 y RN-15), que siguen duras porque su doble severidad la define F07.
- **Orden:** las reglas se leen por su identificador, con las `RF-xx` al final, para que RF-24 no quede primera en la pantalla.

## 3. Cambiar una severidad (FR-003, FR-005)

- **Decisión:** `ReglaEditada` gana `severidad`, **opcional**:
  - **ausente:** la severidad no cambia. Así la carga 7 por archivo (F01), que no trae severidades, sigue usando la misma operación (FR-010 de F04-H02);
  - **presente:** tiene que ser `DURA` o `BLANDA`. En una regla que solo da parámetros (RN-01, RN-06 y RN-24) se rechaza con `CambioNoPermitido` en `reglas[RN-24].severidad` y «Esta regla solo da parámetros: no tiene severidad».
- Como una regla del motor siempre tiene severidad y una regla de parámetros nunca la tiene, «ausente» no es ambiguo: no hace falta un valor para «quitar la severidad», porque no se puede quitar.
- **Registro:** un cambio de severidad es un `CambioDeValor` más, en `reglas[RN-05].severidad`, de `BLANDA` a `DURA`, dentro de `VERSION_EDITADA`.
- **Alternativa:** una operación aparte para las severidades. Se descarta por FR-010 de F04-H02: una sola forma de cambiar una versión.

## 4. Invariantes de los pesos (FR-004)

- Los pesos y el mínimo ya cumplen las de todos los parámetros (F04-H02): no negativos, hasta dos decimales y menores que 100 000 000.
- **Una invariante nueva, de RN-24:** «los títulos cuentan más si coincide el campo específico que si solo coincide el campo amplio». El peso del campo amplio no puede superar al del específico, para cada nivel. Si lo supera, `CambioNoPermitido` en `reglas[RN-24].cuarto_nivel_campo_amplio` con «El peso del campo amplio (4) no puede superar al del campo específico (3)».
- **Sin más invariantes.** Un tope menor que un curso o un mínimo mayor que la suma de los pesos son decisiones de la institución, no errores: el mínimo solo exige justificación (RN-19).

## 5. Las versiones que ya existen (FR-002) — D-68

- **Decisión:** `V4__afinidad_y_reglas_del_motor.sql` agrega las 9 reglas nuevas (§2) y los 8 parámetros (§1) a **cada versión que no las tiene**, y deja un `VERSION_EDITADA` por versión, de `sistema`, con cada valor agregado (anterior `null`). No cambia ningún valor que ya existía.
- **Versiones en uso:** hoy ninguna versión rige un periodo en ningún entorno (los periodos llegan con F02-H01, y `probar_normativa.sh` borra el suyo). Aun así la migración las trataría igual: no cambia un valor, completa la versión con reglas que no definía. Es una excepción acotada al principio VII que se propone como **D-68** (§10).
- **Alternativa:** agregar las reglas solo a la versión 1, y que las demás las tengan al crear una nueva. Se descarta: la versión 2 que el autor creó en el equipo quedaría sin pesos, y una versión sin RN-19 no podría usarse en F07.

## 6. Vuelta atrás (D-55)

- **La versión anterior arranca** sobre el esquema con V4, como en F04-H02 (`NormativaEsquemaIntegrationTest`, con V1 a V3).
- **Y lee las versiones:** su consulta une `regla` y `parametro_regla`, así que no ve las reglas sin parámetros, incluida RF-24, cuyo identificador no aceptaría. Ve RN-19 y RN-24, que tienen parámetros y siguen `RN-xx`.
- **Límite conocido:** si alguien edita una versión mientras corre la versión anterior, esa edición reemplaza las reglas y borra las 7 sin parámetros de esa versión. Volver a la versión nueva no las repone. Se anota en la evidencia; la vuelta atrás es una situación corta y rara, y la prueba del equipo simulado no edita versiones.

## 7. Contrato (`contratos/normativa/openapi.yaml`)

- `Regla` y `ReglaEditada`: `identificador` con `pattern: '^R[NF]-[0-9]{2}$'`; `parametros` sigue obligatorio pero puede venir vacío (sin `minItems`).
- `ReglaEditada` gana `severidad` (`$ref: Severidad`), opcional, con su descripción (§3).
- Sin operaciones ni códigos de error nuevos: los 400 usan `COMUN_SOLICITUD_INVALIDA` con su detalle, y la versión en uso, el 409 `NORMATIVA_VERSION_EN_USO` de F04-H02.
- **La consulta del periodo (clarificación 3):** `consultarPeriodo` dice qué versión rige el periodo y `consultarVersion` devuelve sus reglas con su severidad, sus pesos y su mínimo. No se agrega una operación `ReglasDelPeriodo` todavía: su consumidor, el motor de Distributivo (F07), está en otro proceso y decidirá cómo llama (con el usuario o con la credencial del proceso). Se anota en la evidencia para F07. La especificación dice «la consulta de una versión y la del periodo»: el plan propone leerla como esas dos llamadas.

## 8. La pantalla «Normativa» (FR-009)

- **Solo lectura:** la tabla «Reglas y parámetros» ya muestra la severidad y los parámetros. Gana los nombres en español de las reglas nuevas y de los parámetros de afinidad (`reglas.ts`), y «—» en «Parámetros» para las reglas que no tienen.
- **Edición:** cada regla con severidad gana un selector «Severidad» (Dura o Blanda) en la vista de edición, con su `aria-label` («RN-05 · Severidad»). Las reglas sin severidad no lo tienen. El valor se envía siempre para las reglas del motor.
- **Una sección «Afinidad»** en la vista de solo lectura, sobre la tabla de reglas, con los pesos de RN-24 y el mínimo de RN-19 en una lista corta («Título de cuarto nivel, campo específico: 3»…). Es lo que el escenario 1 consulta, y evita buscarlo entre 16 reglas. En la edición se editan en sus reglas, como cualquier parámetro.

## 9. Pruebas (principio VIII)

- **Dominio, sin dobles:** `Regla` sin parámetros y con severidad; sin parámetros ni severidad (falla); `RF-24`. `editada`: cambia una severidad y la anota; una severidad en RN-24 falla; ausente no cambia; el campo amplio mayor que el específico falla. `pesosAfinidad()` y `minimoAfinidad()` de la versión 1.
- **Persistencia (Testcontainers):** después de V4, cada versión tiene 16 reglas, con la severidad por defecto, los 8 parámetros nuevos y su `VERSION_EDITADA` de `sistema`; los valores de antes, idénticos. Una regla sin parámetros se guarda y se reconstruye. Vuelta atrás: la versión anterior arranca y lee la versión 1.
- **Contrato:** cambiar RN-05 a dura da 200 y la respuesta la tiene dura; una severidad en RN-24, 400 con su campo; RF-24 en la respuesta valida contra el contrato.
- **SPA:** Vitest para el selector y la sección «Afinidad»; Playwright con axe en la edición con el selector.
- **Punta a punta:** `probar_normativa.sh` gana un paso: la versión 1 tiene los pesos por defecto (escenario 1); en la versión de prueba, RN-05 pasa a dura, se asocia al periodo de prueba, y `consultarPeriodo` más `consultarVersion` la devuelven dura, con la 1 todavía blanda (escenario 2, SC-004).

## 10. Decisiones y glosario (§6.4, principio III)

- **D-68 (propuesta):** las reglas del motor que faltan y los pesos y el mínimo de afinidad por defecto se agregan por migración a todas las versiones existentes, con su registro, aunque una rija un periodo: completan la versión, no cambian un valor. Necesita la aprobación del autor antes del código.
- **Glosario §7.3 (propuesta):**
  - «Regla»: «Comprobación de la política con su identificador RN-xx (o RF-xx, como RF-24), sus parámetros y su severidad.» Hoy dice solo RN-xx.
  - «Pesos de afinidad»: «Puntos que suma cada título, experiencia o curso relacionado. Un título suma más si coincide el campo específico de la asignatura que si solo coincide el campo amplio.»
  - Se hace una copia anterior (`modelo_dominio.md.antes-F04-H03`), como en F04-H01 y F04-H02.
