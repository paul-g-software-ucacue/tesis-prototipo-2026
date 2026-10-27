# Quickstart: comprobar F01-H04

Cómo comprobar la historia de punta a punta. El contrato está en [contracts/cargas.md](contracts/cargas.md) y el diseño, en [research.md](research.md).

## Pruebas automáticas

| Qué | Comando | Resultado esperado |
|---|---|---|
| El motor del lote | `cd backend && ./gradlew :plataforma:lenguaje-cargas:check` | En verde: nuevos, cambios, bajas, iguales, huella estable, huella cambiada, todo o nada. |
| Cada dueño | `./gradlew :servicios:academico:check :servicios:docentes:check :servicios:normativa:check` | En verde: cada traductor con sus archivos de prueba y un rechazo de una regla propia que no aplica nada. |
| Cargas: contrato, permisos e historial | `./gradlew :servicios:cargas:check` | En verde. |
| **Las 10 en orden** (SC-001, SC-002) | `./gradlew :apps:datos:integrationTest --tests '*ConfirmarLasDiezCargas*'` | Las 10 aplicadas; al subirlas otra vez, 0 nuevos, 0 que cambian y 0 bajas; la repetida, `REPETIDA`. |
| Pantalla | `cd frontend && npx vitest run src/pantallas/plantillas` y `npx playwright test e2e/plantillas.spec.ts` | En verde, con axe-core. |
| Punta a punta | `deploy/probar_confirmar_carga.sh` | Todos los pasos, dos veces seguidas, y la base como estaba. |

## A mano, en la SPA (equipo de demostración)

1. Con `datos.demo`, en «Plantillas de carga»:
   - sube la carga 01 de prueba con `MATRIZ` cambiado a tu código (`001`) en `codigo_unidad` y en `codigo_unidad_padre`, porque solo puede haber una sede matriz;
   - revisa la vista previa y confirma;
   - sube la 02: sus unidades ya existen.
2. Sube otra vez la 01: 0 nuevos. Confírmala: «ya estaba confirmada».
3. Abre «Historial» de la 01: las dos entradas, la aplicada y la repetida.
4. Sube la 01 con una unidad menos: la vista previa la cuenta como baja, y confirmar no la toca.
