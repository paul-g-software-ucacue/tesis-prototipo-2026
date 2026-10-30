# Quickstart: comprobar el orden de las cargas

Requisitos: el perfil consolidado levantado (`docker compose up -d --wait`) con una base sin datos de prueba, y los usuarios de la demo.

## 1. El orden (US1, US4)

- Con `consolidacion.demo`, abre «Plantillas de carga».
  - «Asignaturas y malla» dice «Pendiente».
  - Su «Subir archivo» está deshabilitado, con «Falta la carga de Carreras».
- Pide `validarArchivo` de `04_asignaturas_malla.csv` por la API:
  - responde 409 `CARGAS_CARGAS_ANTERIORES_PENDIENTES`;
  - el detalle nombra `carreras`;
  - el historial de la 04 sigue vacío.

## 2. El estado (US2)

- Valida y confirma la 01 y la 02:
  - quedan «Cargada»;
  - el botón de la 04 se habilita sin recargar la página.
- La 07 dice «Cargada» desde el inicio (política 2024 precargada).

## 3. «Sin proyectos» (US3)

- Con la 03 confirmada (periodos 2026-I y 2026-II) y sin proyectos, en la fila de la 9 elige «Sin proyectos…» y el periodo 2026-II:
  - la 9 dice «Sin proyectos (2026-II)»;
  - su historial muestra la confirmación con tu usuario.
- Confírmalo otra vez: 200, sin entrada nueva.
- Confirma la 09 de prueba, que tiene proyectos asignables en 2026-II:
  - la 9 dice «Cargada»;
  - pedir «sin proyectos» para 2026-II responde 409 `CARGAS_PERIODO_CON_PROYECTOS`.
- Con `2027-I` responde 400 `CARGAS_PERIODO_NO_ENCONTRADO`.
- Con `2026-III` responde 400 `COMUN_SOLICITUD_INVALIDA`.
- Con `docente.demo`, cualquier confirmación responde 403.

## 4. Las 10 en orden (US4, SC-001)

- `deploy/probar_confirmar_carga.sh`: las 10 entran en orden.
- `deploy/probar_orden_cargas.sh` repite los pasos 1 a 3 y deja la base limpia.

## 5. La fuente de datos (US5)

- `./gradlew :servicios:cargas:test --tests '*ArquitecturaTest*' --tests '*FuenteDeDatos*'`.
