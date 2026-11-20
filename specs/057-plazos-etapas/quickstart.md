# Quickstart: Seguir los plazos de cada etapa

F12-H03 (#58). Cómo comprobar la historia de punta a punta. Los detalles están en [contracts/](contracts/) y en [data-model.md](data-model.md).

## 1. Pruebas automáticas

```bash
cd backend
./gradlew :servicios:validacionocs:test :servicios:validacionocs:integrationTest
./gradlew :servicios:normativa:test :servicios:normativa:integrationTest
./gradlew :servicios:academico:test :servicios:academico:integrationTest
./gradlew :apps:distributivo:integrationTest :apps:datos:integrationTest
cd ../frontend && npm run generar:api && npm run tipos && npm run lint && npm run formato:verificar && npm test
# Playwright, en mcr.microsoft.com/playwright:v1.64.0-noble
```

Esperado: todo en verde, también `ArquitecturaTest` y `ModulosTest`. En particular:
- `PlazoTest` y `ProcesoAprobacionTest`: la llegada, los tres estados, el reinicio por devolución, rechazo y reforma, y los destinos de cada etapa;
- `AdvertenciaDeRemisionTest`: con margen, sin margen, en el límite exacto y sin normativa;
- `RevisarPlazosServicioTest`:
  - el orden (calcular, comprobar el periodo, bloquear, registrar, avisar);
  - el periodo cerrado y Académico sin respuesta;
  - una etapa que cambió entre medio;
- `RevisarPlazosIntegrationTest`: tres revisiones con un `Clock` fijo dejan una fila y un evento.

## 2. Los scripts (SC-001 a SC-004, SC-006)

Con el perfil local construido desde la rama (`docker compose -p tesis-prototipo-2026 up -d --build --wait`):

```bash
deploy/probar_avance.sh
deploy/probar_flujo.sh
```

Esperado ([contracts/demostracion.md](contracts/demostracion.md)):
- **`probar_avance.sh`:** todos los pasos en OK, también P1 a P8.
  - El vencimiento de EN_CONSOLIDACION se detecta al arrancar `distributivo`.
  - Con `identidad` detenido, la notificación llega una sola vez a `vicerrectorado.demo` y a quien consolida, antes de 2 minutos desde que `identidad` vuelve.
  - La tarjeta dice `VENCIDO`, y las acciones no cambian.
- **`probar_flujo.sh`:** R1 advierte RN-23 con 13 días de menos, R2 no advierte y R3 dice que el periodo no tiene normativa.

## 3. Recorrido a mano (SC-005, SC-007)

1. `MDD_CONSERVAR=1 deploy/probar_documento_firmable.sh`: deja el distributivo de 2026-I en EN_REVISION_VRA.
2. Inicia sesión como `vicerrectorado.demo`, en http://localhost:8081, y abre «Consolidado del periodo» con 2026-I.
3. En menos de 10 segundos, la tarjeta dice desde cuándo está en «Revisión del Vicerrectorado», su fecha límite y «Faltan 5 días». Debajo están las etapas activas con sus días, y la actual se marca como tal (SC-005).
4. Para ver el plazo vencido, corre hacia atrás las transiciones de 2026-I y reinicia `distributivo`, como en P3 y P4 de [contracts/demostracion.md](contracts/demostracion.md). La tarjeta dice «Plazo vencido · Vencido hace 1 día», con texto y no solo color, y la campana trae «Venció el plazo de Revisión del Vicerrectorado», una sola vez aunque recargues o reinicies otra vez.
5. Como Administrador, en «Configuración del flujo», carga la plantilla en un periodo en Planificación que empiece dentro de 60 días: queda vigente y aparece «Advertencia de RN-23», con qué hacer. Vuelve a consultarla: la advertencia sigue ahí.
6. Revisa con el teclado y un lector de pantalla que el estado del plazo y la advertencia se anuncian; axe-core no marca fallas (SC-007).
7. Baja el perfil con `docker compose -p tesis-prototipo-2026 down -v`.

## 4. Equipo de demostración (después de fusionar)

Comprobaciones de solo lectura:
- `mdd-actualizar` toma el commit y dice «Sano»; las migraciones V9 de Validación OCS y V7 de Normativa, aplicadas.
- En la demo, 2026-I lleva semanas en EN_CONSOLIDACION. La revisión del arranque registra su vencimiento real: quien consolida y el Vicerrector reciben el aviso una vez, y la tarjeta lo muestra. Es la evidencia en vivo de la historia, sin simular nada.
