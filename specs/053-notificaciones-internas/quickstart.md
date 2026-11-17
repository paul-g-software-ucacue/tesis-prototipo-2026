# Quickstart: Recibir notificaciones internas por cada cambio de estado

F16-H01 (#67). Cómo comprobar la historia de punta a punta. Los detalles están en [contracts/](contracts/) y [data-model.md](data-model.md).

## 1. Pruebas automáticas

```bash
cd backend
./gradlew :servicios:validacionocs:test :servicios:roles:test :servicios:notificaciones:test
./gradlew :servicios:notificaciones:integrationTest :apps:identidad:integrationTest
cd ../frontend && npm run generar:api && npm run tipos && npm run lint && npm run formato:verificar && npm test
# Playwright, en mcr.microsoft.com/playwright:v1.64.0-noble: npx playwright test e2e/notificaciones.spec.ts
```

Esperado:
- en verde;
- `ArquitecturaTest` y `ModulosTest` también, con Notificaciones dependiendo solo de los puertos de entrada de Roles y Usuarios.

## 2. El script (SC-001 a SC-003)

Con el perfil local construido desde la rama (`docker compose -p tesis-prototipo-2026 up -d --build`):

```bash
deploy/probar_documento_firmable.sh
```

Esperado:
- todos los pasos en OK, también del 16 al 18 ([contracts/demostracion.md](contracts/demostracion.md));
- cada transición en la bandeja de quien le toca y en ninguna otra;
- la caída de `identidad` sin errores para el usuario y la notificación antes de 2 minutos;
- la entrega repetida sin duplicados.

## 3. Recorrido a mano (SC-004, SC-005, SC-006)

1. `MDD_CONSERVAR=1 deploy/probar_documento_firmable.sh`: deja el distributivo en EN_REVISION_VRA.
2. Inicia sesión como `vicerrectorado.demo`. La campana de la barra superior muestra el número sin leer.
3. Abre la campana. La notificación «El distributivo llegó a «Revisión del Vicerrectorado»» está en negrita, con quién y cuándo.
4. Ábrela con el teclado:
   - llegas a «Consolidado del periodo» en menos de 10 s (SC-005);
   - el número baja;
   - la notificación sigue en la lista, ya no en negrita.
5. En otra ventana, como otro rol, haz una transición que le toque al Vicerrector, o usa `MDD_CONSERVAR=EN_OCS`. Sin recargar, la campana cambia en menos de 1 minuto (SC-004).
6. Inicia sesión como `coordinacion.demo`: no ve las notificaciones del Vicerrector.
7. Baja el perfil con `docker compose -p tesis-prototipo-2026 down -v`.

## 4. Equipo de demostración (después de fusionar)

Comprobaciones de solo lectura:
- `mdd-actualizar` toma el commit y dice «Sano».
- `notificaciones` está en la V2.
- La memoria de `identidad` con `docker stats` (#224).

En la demo, 2026-I sigue en EN_CONSOLIDACION. La primera notificación llegará con la siguiente transición real.
