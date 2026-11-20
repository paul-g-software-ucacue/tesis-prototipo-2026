# Quickstart: Publicar el distributivo aprobado

F18-H01 (#71). Cómo comprobar la historia de punta a punta. Los detalles están en [contracts/](contracts/) y [data-model.md](data-model.md).

## 1. Pruebas automáticas

```bash
cd backend
./gradlew :servicios:validacionocs:test :servicios:validacionocs:integrationTest
./gradlew :apps:distributivo:integrationTest :apps:identidad:integrationTest
cd ../frontend && npm run generar:api && npm run tipos && npm run lint && npm run formato:verificar && npm test
# Playwright, en mcr.microsoft.com/playwright:v1.64.0-noble
```

Esperado: todo en verde, también `ArquitecturaTest` (quien guarda transiciones las notifica) y `ModulosTest`.

## 2. El script (SC-001 a SC-004)

Con el perfil local construido desde la rama (`docker compose -p tesis-prototipo-2026 up -d --build --wait`):

```bash
deploy/probar_documento_firmable.sh
```

Esperado ([contracts/demostracion.md](contracts/demostracion.md)):
- todos los pasos en OK, también el 16 y el 17 nuevos;
- la publicación con `identidad` detenido, y su notificación una sola vez antes de 2 minutos desde que vuelve;
- la notificación de la publicación para `datos.demo`, `coordinacion.demo`, `docente.demo` y `docente.coordinador.demo`, una cada uno, y para nadie más;
- con el distributivo publicado, ninguna acción para nadie.

## 3. Recorrido a mano (SC-005, SC-006)

1. `MDD_CONSERVAR=APROBADO deploy/probar_documento_firmable.sh`: deja el distributivo en APROBADO, con la resolución registrada.
2. Inicia sesión como `secretaria.demo`, en http://localhost:8081, y abre «Resolución del OCS», con 2026-I.
3. Con el teclado, llega a «Publicar el distributivo» y ábrelo. El diálogo dice que la versión quedará bloqueada y a quiénes se notificará, y se recorre con Tab; Escape lo cierra sin publicar (SC-006).
4. Ábrelo otra vez y confirma: en menos de 30 s desde que abriste la pantalla, la tarjeta muestra «Versión 1 publicada por secretaria.demo el …» y ya no ofrece publicar (SC-005).
5. Como `docente.demo`, `coordinacion.demo` y `datos.demo`: la campana tiene «El distributivo de 2026-I fue publicado».
6. Como `docente.coordinador.demo`: una sola notificación.
7. Como `consolidacion.demo`, en «Consolidado del periodo»: «La versión 1 está publicada: ya no cambia.», la constancia de la publicación y ninguna acción.
8. Baja el perfil con `docker compose -p tesis-prototipo-2026 down -v`.

## 4. Equipo de demostración (después de fusionar)

Comprobaciones de solo lectura:
- `mdd-actualizar` toma el commit y dice «Sano».
- La memoria de `identidad` con `docker stats` (D-109).

En la demo, 2026-I sigue en EN_CONSOLIDACION. La primera publicación llegará con el flujo real.
