# Quickstart: comprobar las propuestas

Requisitos: el perfil consolidado levantado (`docker compose up -d --wait`), con las cargas de prueba confirmadas, 2026-I abierto y un coordinador con su carrera.

## 1. Crear (US1)

- Con `vicerrectorado.demo`, en «Preparación del periodo», pulsa «Crear distributivo»: el tablero dice que ya está creado.
- Otra vez, por la API: 409 `GENERACION_DISTRIBUTIVO_REPETIDO`.
- Un periodo sin preparar: 409 `GENERACION_PERIODO_NO_LISTO`.

## 2. Proponer (US2)

- Con `coordinacion.demo`, abre «Sugerencia» y propone a un candidato: la fila de la oferta lo muestra.
- Propón a otro: queda uno solo.
- Quítalo: la fila vuelve a estar pendiente.

## 3. Justificar (US3)

- `JustificacionTest`: los tres casos, sin dobles.
- Sin tronco común, abre «Docentes bajo el mínimo» y propón a uno sin texto: se pide; con texto: queda como excepción.
- En tronco común, con un docente bajo el mínimo: queda como disponibilidad, sin texto.

## 4. El parcial (US4)

- Propón en todos los paralelos de una carrera: el parcial dice que está completo.
- Retira uno: queda marcado y no cuenta. Vuelve a ofertarlo: cuenta.

## 5. Las horas (US5)

- Con dos coordinadores de carreras distintas, propón al mismo docente en las dos: cada sugerencia muestra las horas de la otra.

## 6. Punta a punta

- `deploy/probar_propuestas.sh` repite estos pasos y deja la base limpia.
