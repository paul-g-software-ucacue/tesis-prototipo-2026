# Quickstart: Notificar al siguiente firmante y ver la cadena de firmas

F16-H02 (#68). Cómo comprobar la historia de punta a punta. Los detalles están en [contracts/](contracts/) y [data-model.md](data-model.md).

## 1. Pruebas automáticas

```bash
cd backend
./gradlew :servicios:firma:test :servicios:notificaciones:test :servicios:usuarios:test
./gradlew :servicios:firma:integrationTest :servicios:notificaciones:integrationTest \
    :apps:distributivo:integrationTest :apps:identidad:integrationTest
cd ../frontend && npm run generar:api && npm run tipos && npm run lint && npm run formato:verificar && npm test
# Playwright, en mcr.microsoft.com/playwright:v1.64.0-noble
```

Esperado:
- todo en verde;
- `ArquitecturaTest` y `ModulosTest` también: Notificaciones depende solo de los puertos de entrada de Roles y Usuarios, y Firma no conoce a Notificaciones ni a Aprobación.

## 2. Los scripts (SC-001 a SC-003)

Con el perfil local construido desde la rama (`docker compose -p tesis-prototipo-2026 up -d --build --wait`):

```bash
deploy/probar_documento_firmable.sh
deploy/probar_rechazo_en_firma.sh
```

Esperado ([contracts/demostracion.md](contracts/demostracion.md)):
- todos los pasos en OK, también el 19 nuevo;
- la cadena correcta después de generar y de cada firma;
- las dos notificaciones de firma pendiente, a `vicerrectorado.demo` y a `rectorado.demo`, y a nadie más;
- con `identidad` detenido durante F1, la notificación de F2 una sola vez antes de 2 minutos desde que vuelve.

## 3. Recorrido a mano (SC-004, SC-005, SC-006)

1. `MDD_CONSERVAR=F2 deploy/probar_documento_firmable.sh`: deja F1 aceptada y F2 en turno.
2. Inicia sesión como `vicerrectorado.demo`, en http://localhost:8081.
3. La campana muestra «Tienes una firma pendiente: «Revisado por»», con `docente.demo` como autor y el detalle de F1.
4. Ábrela: llegas a «Pendientes de firma», con el documento de 2026-I, en menos de 10 s (SC-004).
5. Abre «Consolidado del periodo», en 2026-I. En la tarjeta del documento firmable, la cadena dice (SC-005):
   - F1 · Elaborado por · el nombre de F1 · Firmado el …;
   - F2 · Revisado por · tu nombre · En turno;
   - F3 · Aprobado por · el nombre del Rector · Pendiente.
6. Recorre la cadena con el teclado y con un lector de pantalla: cada estado se dice con texto (SC-006).
7. Como `consolidacion.demo`, ves la misma cadena. Como `coordinacion.demo`, no ves la tarjeta.
8. Como `docente.demo`, no tienes una notificación de firma pendiente: solo «El distributivo llegó a «En firma»».
9. Baja el perfil con `docker compose -p tesis-prototipo-2026 down -v`.

## 4. Equipo de demostración (después de fusionar)

Comprobaciones de solo lectura:
- `mdd-actualizar` toma el commit y dice «Sano».
- La memoria de `identidad` con `docker stats` (D-109).

En la demo, 2026-I sigue en EN_CONSOLIDACION. La primera notificación de firma pendiente llegará con la primera firma real.
