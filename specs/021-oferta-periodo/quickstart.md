# Quickstart: comprobar F03-H03

Cómo comprobar la historia de punta a punta. Los detalles del contrato están en [contracts/academico.md](contracts/academico.md) y las reglas en [research.md](research.md) §3.

## Pruebas automáticas

| Qué | Comando | Resultado esperado |
|---|---|---|
| Dominio, contrato, permisos y repositorio | `cd backend && ./gradlew :servicios:academico:check integrationTest` | En verde; el dominio oferta las 165 asignaturas-paralelo de prueba y suma 660 horas |
| Pantalla | `cd frontend && npx vitest run src/pantallas/oferta` | En verde |
| Navegador y accesibilidad | `cd frontend && npx playwright test e2e/oferta.spec.ts` | En verde, axe sin fallas WCAG 2.1 AA |
| Punta a punta sobre el perfil | `deploy/probar_oferta.sh` | Todos los pasos pasan, dos veces seguidas, y la base queda como estaba |

## A mano, en la SPA

1. Con `vicerrectorado.demo`, registrar un periodo en Planificación (pantalla «Periodos»), si no hay uno.
2. En «Oferta del periodo», elegirlo y ofertar una asignatura de 4 horas en los paralelos `A_NOCTURNA`, `B_NOCTURNA` y `C_NOCTURNA`: la asignatura suma 12 horas a cubrir, y su carrera y el periodo también las suman.
3. Retirar uno: suma 8, y el retirado se sigue viendo. Volver a ofertarlo: 12.
4. Indicar más horas que las de la asignatura, o una jornada que no es de la carrera: el motivo aparece junto al campo.
5. Con `consolidacion.demo`, ver la misma oferta sin acciones.
