# Quickstart: comprobar F03-H05

Cómo comprobar la historia de punta a punta. Los detalles del contrato están en [contracts/cargos-proyectos.md](contracts/cargos-proyectos.md) y las reglas en [research.md](research.md) §4 a §7.

## Pruebas automáticas

| Qué | Comando | Resultado esperado |
|---|---|---|
| Dominio, contrato, permisos y repositorio | `cd backend && ./gradlew :servicios:docentes:check integrationTest` | En verde. El dominio registra los 21 cargos y los 11 proyectos de prueba; cada regla de las tablas de la especificación tiene sus casos válidos e inválidos. |
| Las referencias de Académico y los límites del proceso | `cd backend && ./gradlew :servicios:academico:check :apps:datos:check` | En verde. `ModulosTest` pasa con Docentes usando solo `puertos.entrada` de Académico. |
| El catálogo de errores | `cd backend && ./gradlew :plataforma:errores:test :servicios:docentes:test` | En verde. Los 4 códigos nuevos tienen sus 8 columnas. |
| Pantallas | `cd frontend && npx vitest run src/pantallas/cargos src/pantallas/proyectos` | En verde, con los filtros y los motivos de cada campo. |
| Navegador y accesibilidad | `cd frontend && npx playwright test e2e/cargos-proyectos.spec.ts` | En verde; axe sin fallas WCAG 2.1 AA, también con los campos marcados en rojo. |
| Punta a punta sobre el perfil | `deploy/probar_cargos_proyectos.sh` | Todos los pasos pasan dos veces seguidas, y la base queda como estaba. |

## A mano, en la SPA (equipo de demostración)

1. Con `datos.demo` (Talento Humano):
   1. Abrir «Cargos» y registrar como coordinador de una carrera a un docente activo a tiempo completo con maestría: sin advertencia. Medir cuánto tarda (SC-001).
   2. Registrar como gestor a un docente sin títulos de cuarto nivel: el cargo se guarda con la advertencia «Le falta un título de cuarto nivel registrado (RN-20).» (SC-004).
   3. Intentar un segundo coordinador vigente para la misma carrera: se rechaza (RN-12).
   4. Intentar dar un cargo a un docente en licencia: se rechaza con el motivo.
2. Abrir «Proyectos» y registrar uno de investigación con dos participantes. Intentar que sus horas sumen más que las aprobadas: se marca el campo antes de enviar.
3. Elegir el periodo 2026-I y comprobar que los proyectos de prácticas son asignables. En 2026-II no lo son, por su vigencia (SC-003).
4. Pasar un proyecto a suspendido: aparece como no asignable por su estado.
5. Finalizar un cargo con su fecha de fin.
6. Con `consolidacion.demo`, ver los cargos y los proyectos sin acciones.
