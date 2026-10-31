# Research: Ver los docentes sugeridos para una asignatura

## 1. Las consultas entre procesos

- **Decisión:** se reusan `ConsultasEntreProcesos`, el token de proceso y los errores 502 y 504 de ADR-023. No hace falta un ADR nuevo.
- **Por qué:** es el caso para el que ADR-023 se aprobó: Generación lee de `datos` lo que un coordinador no podría leer directo.
- **Alternativas:** ninguna razonable.

## 2. La asignatura-paralelo

- **Decisión:** una ruta interna nueva en Académico, `consultarParaleloDeLaOferta`, que trae una sola asignatura-paralelo con lo que hace falta: campos, tronco común, carrera, horas y estado.
- **Por qué:** `consultarAsignaturasOfertadas` devuelve asignaturas sin repetir, sin el paralelo ni sus horas; y trae lo mínimo a propósito (principio IX).
- **Alternativas:** ampliar `consultarAsignaturasOfertadas`: traería toda la oferta para usar una fila, y mezclaría paralelos con asignaturas.
- **El estado viaja, y Generación decide.** Una retirada responde 200 con su estado, para distinguirla de la que no existe.
- **Un periodo que no existe y un paralelo que no existe responden el mismo 404.** Para el usuario son el mismo problema: esa asignatura-paralelo no está en esa oferta. Un solo código evita una llamada más.

## 3. Los docentes activos

- **Decisión:** una ruta interna nueva en Docentes, `consultarDocentesActivos`: de cada docente activo, su identificación, su nombre, su dedicación, sus horas contratadas y su formación.
- **Por qué:** el filtro de «activo» es una regla del dueño de ese dato, y una sola llamada evita 47.
- **Alternativas:** pedir `consultarFormacion` por docente: 47 llamadas por sugerencia, y aun así haría falta la lista.
- **Principio IX:** ni correo, ni teléfono, ni contrato, ni institución, ni registro, ni asignaturas afines. La identificación viaja porque es la clave del docente; F06-H03 la necesita para proponer. La pantalla no la muestra (FR-016), pero un lector con este permiso ya puede leerla en «Docentes».

## 4. El mínimo de afinidad

- **Decisión:** `consultarPesosDelPeriodo` suma `minimoAfinidad` a su respuesta. Generación lo lee con los pesos.
- **Por qué:** sale de la misma versión, en la misma lectura; una ruta aparte sería una llamada más por el mismo dato.
- **Consecuencia:** el campo es obligatorio. Los dos procesos se despliegan juntos, con la misma versión, así que no hay una ventana con versiones cruzadas en el perfil consolidado ni en la demo.
- **Sin mínimo en una versión:** no pasa, porque cada versión lleva el suyo (D-68). Si faltara, es un error interno, como con los pesos.

## 5. Las horas asignadas

- **Decisión:** un puerto de salida, `HorasAsignadas.delPeriodo(periodo, carrera)`, que devuelve las horas de cada docente con asignaciones: las totales y las de otras carreras y unidades. Hoy lo cumple `SinAsignacionesTodavia`, que responde un mapa vacío; un docente ausente tiene 0 h.
- **Por qué:** cumple FR-009: F06-H03 solo cambia el adaptador. Y deja probar el orden con un doble en las pruebas.
- **Lo que no se puede demostrar:** el escenario «Horas en otras carreras» solo se prueba con datos simulados. La evidencia de esta historia debe decirlo, y F06-H03 debe demostrarlo con datos reales.

## 6. El orden y las horas suficientes

- **Decisión:** `Sugerencia` aplica, en este orden: primero el mínimo; después separa a quienes tienen horas suficientes; y dentro de cada grupo ordena por puntaje descendente, horas disponibles descendentes y nombre.
- **Horas suficientes:** las disponibles son iguales o mayores que las horas del paralelo. Las horas del paralelo admiten decimales: 2,5 h no son 2 ni 3.
- **Nombre:** con `Collator` en español, como en `CalculadoraAfinidad`.
- **Puntaje:** `BigDecimal`, comparado con `compareTo`: 2 y 2,00 son iguales. Esto importa en el mínimo, y una prueba lo fija.
- **Tronco común:** no aplica el mínimo y `fueraPorElMinimo` es 0.

## 7. Los errores

- **Decisión:** 2 códigos nuevos: `GENERACION_PARALELO_NO_ENCONTRADO` (404) y `GENERACION_PARALELO_RETIRADO` (409). `GENERACION_PERIODO_SIN_ABRIR` ya existe y se reusa.
- **El orden de las consultas:** paralelo, pesos, docentes. Un paralelo inexistente responde 404 aunque el periodo no esté abierto. La primera que falla corta: nunca hay una lista parcial, como en F06-H01.

## 8. La carrera del usuario

- **Decisión:** no se filtra en esta historia, por decisión del autor. El permiso es el mismo de `consultarAfinidadDelDocente` (D-70).
- **Por qué:** el servidor no sabe de qué carrera es un coordinador. La misma limitación ya está escrita: la spec de la oferta (021) y la de cargos y proyectos (F03-H05) dejan el filtro «solo lo propio» para cuando se pueda aplicar (D-64).
- **Pendiente:** F06-H03 tiene que resolverlo, porque ahí se escribe.

## 9. El glosario y D-75

Antes del código, y con la aprobación del autor (principio III), en `sdd/modelo_dominio.md` §7:

- **«Horas disponibles», cambia:** «Horas contratadas del docente menos sus horas ya asignadas en todas las carreras y unidades; nunca menos de 0.»
- **«Candidato», nuevo:** «Docente activo que aparece en una sugerencia, con su puntaje, su desglose y sus horas.» · `Candidato` · Distributivo · RF-25.

Como cambia una definición aprobada, se registra **D-75** en `docs/decisiones.md`. RF-25 no cambia: habla de «disponibilidad de horas y dedicación», y la pantalla muestra las dos.

## 10. La pantalla

- **Decisión:** un botón «Sugerencia» en cada fila ofertada de «Oferta», con un diálogo. Se muestra si el usuario tiene `LECTURA_DE_LOS_DOCENTES`, en cualquier estado del periodo, y aparte de `puedeCambiar`.
- **Por qué:** quien lee la oferta sin poder leer docentes no debe ver una acción que le respondería 403.
- **El desglose:** sale de `AfinidadDelDocente.tsx` a `pantallas/comun/DesgloseDeAfinidad.tsx`, para que la ficha y la sugerencia digan lo mismo.
- **Sin candidatos:** `fueraPorElMinimo > 0` dice que nadie alcanza el mínimo; 0 dice que no hay docentes activos.

## 11. La prueba punta a punta

- **Decisión:** `deploy/probar_sugerencia.sh`, con los dos procesos reales y un oráculo en Python sobre los CSV, sin el código de Generación, igual que `probar_afinidad.sh`.
- **Cubre:** el orden y los candidatos frente al oráculo, en una asignatura sin tronco común y en una con él; un candidato de otra carrera base; el docente en licencia de los datos y uno activo que pasa a licencia y desaparece; otra versión con otro mínimo; la retirada; el periodo sin abrir; los 400, 403 y 404; `datos` detenido; y los logs.
- **No cubre:** horas reales, porque no existen.
