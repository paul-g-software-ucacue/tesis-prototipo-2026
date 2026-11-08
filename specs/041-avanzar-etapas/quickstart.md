# Quickstart: Avanzar el distributivo según el rol de la etapa

Guía para comprobar la historia de punta a punta. El detalle está en [contracts/validacionocs.md](contracts/validacionocs.md), [contracts/generacion.md](contracts/generacion.md), [contracts/bitacora.md](contracts/bitacora.md), [contracts/spa.md](contracts/spa.md) y [contracts/demostracion.md](contracts/demostracion.md).

## Requisitos

- El perfil consolidado levantado (`docker compose up -d --wait`), con la contraseña de la demo de `.env.ejemplo`.
- Los datos de prueba con un distributivo creado en 2026-I (los pasos de preparación de los scripts de `deploy/`, o `deploy/equipo/poblar_demo.sh` en el equipo de demostración).

## 1. Pruebas automáticas

```bash
cd backend
./gradlew :servicios:validacionocs:test :servicios:generacion:test
./gradlew :servicios:validacionocs:integrationTest :servicios:generacion:integrationTest
cd ../frontend && npm test -- flujo && npx playwright test flujo-envio
```

## 2. El script de la historia

```bash
deploy/probar_avance.sh
```

Esperado: los 8 pasos en OK.

## 3. En la pantalla (comprobación del autor)

1. Como `coordinacion.demo`, en **Oferta del periodo**: el estado del flujo dice «Propuesta de carrera» y su parcial «Pendiente». **Enviar parcial**, y confirmar.
2. El parcial dice «Enviado por coordinacion.demo el …». La oferta y las actividades de su carrera quedan en solo lectura.
3. Como `vicerrectorado.demo`, en **Bitácora**: servicio «Validación OCS», acción `PARCIAL_ENVIADO`. Con «Ver valores», desde `BORRADOR_CARRERA · PENDIENTE` hacia `ENVIADO`, la versión y el hash.
4. Como `vicerrectorado.demo`, en **Consolidado del periodo**: la etapa sigue en «Propuesta de carrera», con una carrera enviada de las que hay.
5. Para ver el paso de etapa, enviar los parciales que faltan (en la demo hay un coordinador: cambiarle la carrera en **Roles** como `admin.demo` para cada una). Al enviar el último, el distributivo está en «Consolidación» y la bitácora tiene una `ETAPA_AVANZADA`.

## 4. En el equipo de demostración

Después del despliegue, repetir el apartado 3 y anotar el resultado en `evidencia.md`.
