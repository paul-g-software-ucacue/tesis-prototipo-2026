# Quickstart: comprobar las reglas duras

Requisitos: el perfil consolidado levantado, con las cargas de prueba confirmadas, 2026-I abierto, el distributivo creado y dos coordinadores con carreras distintas.

## 1. Las horas (US1)

- Con un docente al que le quedan 2 h, propónlo para un paralelo de 4 h: «Horas laborales excedidas», con las horas que le quedan.
- Con horas en otra carrera, asígnale lo que no cabe: no se guarda.
- Llega justo al límite: se guarda.

## 2. El tope de clase (US2)

- Con un docente en un proyecto de investigación y 16 h de clase, propónlo: no se guarda.
- Asígnale otra actividad de docencia: se guarda.
- Con un proyecto de prácticas: el tope no aplica.

## 3. A la vez (US3)

- `deploy/probar_reglas_duras.sh` envía 20 pares de intentos a la vez: nunca queda excedido.

## 4. La sugerencia (US4)

- Un candidato sin horas sigue en la lista, con su motivo y sin «Proponer».

## 5. Un docente excedido (US5)

- Reduce una actividad: se guarda. Auméntala: no se guarda.

## 6. Sin proyecto (US6)

- Horas de investigación sin proyecto: no se guardan.
