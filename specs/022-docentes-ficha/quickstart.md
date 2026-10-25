# Quickstart: comprobar F03-H04

Cómo comprobar la historia de punta a punta. Los detalles del contrato están en [contracts/docentes.md](contracts/docentes.md) y las reglas en [research.md](research.md) §3.

## Pruebas automáticas

| Qué | Comando | Resultado esperado |
|---|---|---|
| Dominio, contrato, permisos y repositorio | `cd backend && ./gradlew :servicios:docentes:check integrationTest` | En verde; el dominio registra los 47 docentes de prueba y encuentra cada uno por su cédula |
| El puerto publicado de Académico y los límites del proceso | `cd backend && ./gradlew :servicios:academico:check :apps:datos:check` | En verde; `ModulosTest` pasa con Docentes usando solo `puertos.entrada` de Académico |
| Pantalla | `cd frontend && npx vitest run src/pantallas/docentes` | En verde |
| Navegador y accesibilidad | `cd frontend && npx playwright test e2e/docentes.spec.ts` | En verde, axe sin fallas WCAG 2.1 AA |
| Punta a punta sobre el perfil | `deploy/probar_docentes.sh` | Todos los pasos pasan, dos veces seguidas, y la base queda como estaba |

## A mano, en la SPA (equipo de demostración)

1. Con `datos.demo` (Talento Humano), abrir «Docentes» y registrar un docente ocasional a tiempo completo, con una carrera base que exista, un título de tercer nivel, una maestría, dos cursos y una experiencia. Medir cuánto tarda (SC-001 es buscar y abrir, no registrar).
2. Buscarlo por una parte del apellido escrita en minúsculas y sin tilde, y por su cédula; abrir su ficha: cada título con su campo amplio y específico, cada curso con sus horas (SC-001, SC-003).
3. Corregir la dedicación a medio tiempo con 20 horas; agregar un curso y quitar la experiencia.
4. Intentar guardar 0 horas contratadas, o una cédula con el dígito verificador mal: el motivo aparece junto al campo.
5. Marcarlo en licencia y volver a activarlo.
6. Con `consolidacion.demo`, ver los mismos docentes y su ficha sin acciones.
