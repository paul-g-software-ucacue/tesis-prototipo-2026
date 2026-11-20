# Quickstart: Iniciar una reforma de la versión publicada

F18-H02 (#72). Cómo comprobar la historia de punta a punta. Los detalles están en [contracts/](contracts/) y [data-model.md](data-model.md).

## 1. Pruebas automáticas

```bash
cd backend
./gradlew :servicios:validacionocs:test :servicios:validacionocs:integrationTest
./gradlew :servicios:generacion:test :servicios:generacion:integrationTest
./gradlew :apps:distributivo:integrationTest :apps:identidad:integrationTest
cd ../frontend && npm run generar:api && npm run tipos && npm run lint && npm run formato:verificar && npm test
# Playwright, en mcr.microsoft.com/playwright:v1.64.0-noble
```

Esperado: todo en verde, también `ArquitecturaTest` y `ModulosTest`.

## 2. El script (SC-001 a SC-004)

Con el perfil local construido desde la rama (`docker compose -p tesis-prototipo-2026 up -d --build --wait`):

```bash
deploy/probar_documento_firmable.sh
```

Esperado ([contracts/demostracion.md](contracts/demostracion.md)):
- todos los pasos en OK, también del 22 al 27;
- la reforma con `identidad` detenido, y su notificación una sola vez antes de 2 minutos desde que vuelve;
- la versión 2 recorre el ciclo completo hasta publicarse, y la versión 1 conserva sus tres firmas, su documento y su resolución.

## 3. Recorrido a mano (SC-005, SC-006)

1. `MDD_CONSERVAR=PUBLICADO deploy/probar_documento_firmable.sh`: deja el distributivo publicado, con la versión 1.
2. Inicia sesión como `vicerrectorado.demo`, en http://localhost:8081, y abre «Consolidado del periodo», con 2026-I.
3. Con el teclado, llega a «Iniciar una reforma» y ábrelo. El diálogo dice qué pasará y a quiénes se notificará; un motivo de menos de 10 caracteres no se envía; Escape lo cierra sin iniciar (SC-006).
4. Ábrelo otra vez, escribe el motivo y confirma: en menos de 1 minuto, la tarjeta muestra «Consolidación · versión 2», la reforma en curso con el motivo, y la publicación de la versión 1 «vigente hasta que se publique la reforma» (SC-005).
5. Como `consolidacion.demo`: la campana tiene «Se inició una reforma del distributivo de 2026-I» y lleva a «Consolidado del periodo», donde puede devolver parciales.
6. Como `coordinacion.demo`, `docente.demo` y `datos.demo`: la misma notificación; la de la coordinación lleva a la oferta.
7. Como `vicerrectorado.demo`: no tiene su propia notificación.
8. Baja el perfil con `docker compose -p tesis-prototipo-2026 down -v`.

## 4. Equipo de demostración (después de fusionar)

Comprobaciones de solo lectura:
- `mdd-actualizar` toma el commit y dice «Sano»; las migraciones V8 de Validación OCS y V9 de Generación, aplicadas.
- La memoria de `identidad` con `docker stats` (D-109).

En la demo, 2026-I sigue en EN_CONSOLIDACION. La primera reforma llegará con el flujo real.
