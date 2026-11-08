# Quickstart: Enviar a revisión con cero inconsistencias abiertas

Guía para comprobar la historia. El detalle está en [contracts/validacionocs.md](contracts/validacionocs.md), [contracts/generacion.md](contracts/generacion.md), [contracts/bitacora.md](contracts/bitacora.md), [contracts/spa.md](contracts/spa.md) y [contracts/demostracion.md](contracts/demostracion.md).

## Requisitos

- El perfil consolidado levantado (`docker compose up -d --wait`), con la contraseña de la demo de `.env.ejemplo`.
- Los datos de prueba, con el distributivo de 2026-I en EN_CONSOLIDACION: todas las carreras enviaron su parcial (F12-H02).

## 1. Pruebas automáticas

```bash
cd backend
./gradlew :servicios:validacionocs:test :servicios:generacion:test
./gradlew :servicios:validacionocs:integrationTest
cd ../frontend && npm test -- Consolidado EstadoDelFlujo && npx playwright test consolidado
```

El envío que pasa a EN_REVISION_VRA y los dos envíos simultáneos están en la prueba de integración de Validación OCS (research §10).

## 2. El script de la historia

```bash
deploy/probar_revision.sh
```

Esperado: los 7 pasos en OK.

## 3. En la pantalla (comprobación del autor)

1. Como `consolidacion.demo`, en **Consolidado del periodo**:
   - la etapa dice «Consolidación»;
   - el botón **Enviar a revisión** está deshabilitado;
   - debajo se lista lo que falta: consolidar, adjuntar el informe y las inconsistencias abiertas.
2. **Consolidar**. La lista ya no pide consolidar.
3. **Adjuntar el informe** con un PDF. La lista ya no pide el informe y solo quedan las inconsistencias abiertas, con su número y el enlace al panel.
4. Como `vicerrectorado.demo`: en **Consolidado del periodo** ve la etapa, pero no el botón.
5. Opcional: con la API, como `consolidacion.demo`, `POST /api/validacionocs/periodos/2026-I/revision`. Responde 409 `VALIDACIONOCS_INCONSISTENCIAS_ABIERTAS`, y el número del detalle coincide con el panel.
6. En **Bitácora**: no hay ninguna entrada `CONSOLIDADO_ENVIADO_A_REVISION`, porque un rechazo no deja entrada.

El envío que pasa no se puede ver con los datos de prueba: tienen inconsistencias duras que no se corrigen en EN_CONSOLIDACION mientras devolver un parcial no exista (#211).

## 4. En el equipo de demostración

Después del despliegue, repetir el apartado 3 y anotar el resultado en `evidencia.md`. Para llegar a EN_CONSOLIDACION en la demo, las 6 carreras que faltan envían su parcial (quickstart §3 de F12-H02).
