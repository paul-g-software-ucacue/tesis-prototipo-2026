# Quickstart: comprobar F01-H03

Cómo comprobar la historia de punta a punta. El contrato está en [contracts/cargas.md](contracts/cargas.md) y las reglas, en [research.md](research.md) §3 a §9.

## Pruebas automáticas

| Qué | Comando | Resultado esperado |
|---|---|---|
| Reglas, informe, contrato y permisos | `cd backend && ./gradlew :servicios:cargas:check` | En verde. Cada regla de la tabla de la especificación tiene sus casos válidos, inválidos y en el límite. Los 10 archivos de prueba no tienen errores con sus referencias presentes. |
| El puerto de Docentes y los límites del proceso | `cd backend && ./gradlew :servicios:docentes:check :apps:datos:check` | En verde. `ModulosTest` pasa con Cargas usando solo `puertos.entrada` de Académico y de Docentes. |
| Pantalla | `cd frontend && npx vitest run src/pantallas/plantillas` | En verde. |
| Navegador y accesibilidad | `cd frontend && npx playwright test e2e/plantillas.spec.ts` | En verde; axe sin fallas WCAG 2.1 AA, también con el informe a la vista. |
| Punta a punta | `deploy/probar_reglas_carga.sh` | Todos los pasos pasan dos veces seguidas, y la base queda como estaba. |

## A mano, en la SPA (equipo de demostración)

1. Con `datos.demo`, en «Plantillas de carga», subir el archivo de docentes de prueba con:
   - una cédula de 9 dígitos;
   - una fecha `2026-02-30`;
   - una dedicación `XX`;
   - una carrera base que no existe.

   Deben salir los 4 errores con su fila y su campo, y para la cédula, la sugerencia con el cero.
2. Descargar el informe y abrirlo en una hoja de cálculo. Tiene las columnas fila, campo, valor, motivo y tipo, y un valor `=1+1` aparece como texto, sin calcularse.
3. Corregir el archivo y volver a subirlo: sin errores.
