# Contrato: Generación

Cambio en `contratos/generacion/openapi.yaml`.

## Se retira `POST /api/generacion/eventos-prueba` (`publicarEventoDePrueba`)

Con su etiqueta `EventosPrueba`. Era el endpoint de prueba de B00-H05, apagado por defecto (D-48). La bitácora prueba el mismo camino con cambios reales (research §7, D-47).

Ninguna otra operación de Generación cambia: proponer, quitar, asignar actividades, justificar, consolidar y adjuntar el informe responden igual. Lo único nuevo es que, al guardar, también publican su entrada para la bitácora, sin efecto visible para quien llama.
