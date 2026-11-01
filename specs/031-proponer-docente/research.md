# Research: Proponer un docente por asignatura-paralelo

## 1. El agregado

- **Decisión:** `VersionDistributivo` es el agregado (MD-01), con sus parciales y propuestas. `Distributivo` es la cabecera, una por periodo.
- **Persistencia:** con `JdbcClient`, como `RepositorioEventosDePruebaJdbc`. Cada propuesta es una fila: no se reescribe el agregado entero en cada cambio.
- **Una propuesta por paralelo:** llave primaria `(version, asignatura, paralelo)`, con `ON CONFLICT DO UPDATE`.
- **Sin bloqueo optimista todavía:** MD-01 lo pide para que RN-02 no se pase con dos coordinadores a la vez, y eso es F07-H02. Hoy no hay regla que proteger.

## 2. Saber si el periodo está listo

- **Decisión:** una ruta interna en Cargas, `consultarPeriodoListo`: `GET /interno/cargas/periodos/{periodo}/preparacion`, que responde `{ abierto, cerrado, listo }`.
- **Por qué:** es la misma regla del tablero (F02-H02): `ConsultarPreparacion` ya la calcula.
- **Alternativas:** creer al botón de la pantalla: el servidor no puede fiarse de la SPA.

## 3. La oferta completa

- **Decisión:** una ruta interna en Académico, `consultarOfertaDelPeriodo`: `GET /interno/academico/periodos/{codigo}/oferta`, que devuelve todos los `ParaleloDeLaOferta`, también retirados.
- **Sirve para:** las carreras al crear, el avance del parcial y las horas.
- **Una consulta por lectura,** sin copiar nada a Generación.

## 4. Las horas (FR-018, FR-019)

- **Decisión:** `HorasDelPeriodo.calcular(propuestas, oferta, paralelo)`, una función pura del dominio. Devuelve un mapa de identificación a `HorasDelDocente`, y suma por docente con las `horasSemanales` efectivas. El puerto `HorasAsignadas` desaparece: quien necesita las horas lee las propuestas y la oferta, y la llama.
  - un paralelo retirado no cuenta;
  - el paralelo que se consulta no cuenta (spec, US5 escenario 3);
  - las de una carrera distinta de la del paralelo son «otras carreras».
- **FR-009 de F06-H02** prometía cambiar solo el adaptador; desaparece también el puerto, y la regla de `Sugerencia` no cambia.
- **Sin distributivo:** responde vacío; todos con 0 h.

## 5. La justificación

- **Decisión:** `Justificacion.para(puntaje, minimo, troncoComun, texto)`, una función pura:
  - alcanza el mínimo: `AFINIDAD`, sin texto;
  - bajo el mínimo en tronco común: `DISPONIBILIDAD`, sin texto;
  - bajo el mínimo sin tronco común: `EXCEPCION`, con texto de 10 a 500 caracteres, o `JustificacionObligatoria`.
- **El texto se ignora** en los dos primeros casos (FR-014).
- **El servidor recalcula el puntaje** al guardar: la pantalla solo manda el docente y el texto.

## 6. El nombre del docente

- **Decisión:** la propuesta guarda una copia del nombre.
- **Por qué:** la lista de docentes activos no trae a quien pasó a licencia, y la oferta debe seguir mostrando quién está propuesto.
- **Consecuencia:** una corrección del nombre no se refleja hasta volver a proponer. Es aceptable en el prototipo.
- **Alternativas:** una ruta interna de nombres, una llamada más en cada lectura de la oferta.

## 7. Quién puede qué

| Operación | Permiso |
|---|---|
| `crearDistributivo` | `DISTRIBUTIVO:ESCRITURA` |
| `consultarDistributivo` | Los del tablero: `DISTRIBUTIVO:LECTURA`, `CARGAS:LECTURA` |
| `proponerDocente`, `quitarPropuesta` | `OFERTA:ESCRITURA`, con `AlcanceDeCarrera` |
| `consultarParciales` | Los de la oferta, con `AlcanceDeCarrera` |

Con `OFERTA:ESCRITURA` solo sobre lo propio, un coordinador sin carrera recibe el aviso de F06-H05.

Al proponer, el alcance se mira sobre `OFERTA:ESCRITURA`, no sobre los permisos de lectura: `AlcanceDeCarrera.propia(autoridades, "OFERTA:ESCRITURA")`. Con el permiso solo sobre lo propio, se limita a su carrera, aunque no tenga lectura. Si no, un rol propio con ese permiso y sin lectura pasaría como «ve todas».

## 8. Los errores

8 códigos nuevos, en `contracts/generacion.md`. Cada uno tiene un solo estado: consultar sin distributivo es 404, y proponer sin distributivo, 409. El periodo cerrado y el docente no activo son 409. Una asignatura-paralelo que no existe reusa `GENERACION_PARALELO_NO_ENCONTRADO`; una retirada, `GENERACION_PARALELO_RETIRADO`.

## 9. Saber si el periodo está cerrado al proponer

- **Decisión:** `ParaleloDeLaOferta` suma `periodoCerrado`.
- **Por qué:** ya se consulta para proponer: evita otra llamada.

## 10. El modelo táctico y las decisiones

Antes del código, y con la aprobación del autor (principio III y §6.4):

- **Modelo táctico, `sdd/modelo_dominio.md`:** `Horas` pasa de «Horas semanales enteras y no negativas» a «Horas semanales no negativas, con hasta dos decimales».
- **D-77:** las horas semanales admiten hasta dos decimales. La oferta ya las admite (RN-18), y una asignación las hereda.
- **D-78:** F06-H03 va antes que F07-H02, y mientras tanto la propuesta sin horas suficientes se guarda con un aviso. La dependencia entre las dos issues era circular.

## 11. Dos arreglos de la pantalla «Oferta»

Se tocan porque esta historia cambia esa pantalla, y quedaron pendientes en F06-H02 y F06-H05:

- **«Horas a cubrir del periodo»** pasa a «Horas a cubrir», porque al coordinador le muestra las de su carrera.
- **`horas()`** muestra hasta dos decimales: hoy 2,25 h se ve 2,3 h.

## 12. La prueba punta a punta

- **Decisión:** `deploy/probar_propuestas.sh`, con un coordinador por carrera y un oráculo en Python para las horas.
- **Cubre:** crear y sus errores; proponer, reemplazar y quitar; las tres justificaciones; el parcial completo; retirar y volver a ofertar; horas en otras carreras reales; `datos` detenido.
- **`coordinacion.demo` y `docente.coordinador.demo`,** con carreras distintas, para demostrar las horas en otras carreras.
