# Quickstart: Devolver un parcial a su carrera con una observación

Guía para comprobar la historia. El detalle está en [contracts/validacionocs.md](contracts/validacionocs.md), [contracts/generacion.md](contracts/generacion.md), [contracts/bitacora.md](contracts/bitacora.md), [contracts/spa.md](contracts/spa.md) y [contracts/demostracion.md](contracts/demostracion.md).

## Requisitos

- El perfil consolidado levantado (`docker compose up -d --wait`), con la contraseña de la demo de `.env.ejemplo`.
- El distributivo de 2026-I en EN_CONSOLIDACION: todas las carreras enviaron su parcial (F12-H02).

## 1. Pruebas automáticas

```bash
cd backend
./gradlew :servicios:validacionocs:test :servicios:generacion:test
./gradlew :servicios:validacionocs:integrationTest :servicios:generacion:integrationTest
./gradlew :servicios:usuarios:test --rerun     # BitacoraSinBorradoContratoTest, tras cambiar el contrato
cd ../frontend && npm test -- Consolidado EstadoDelFlujo && npx playwright test consolidado oferta
```

## 2. El script de la historia

```bash
deploy/probar_devolucion.sh
```

Esperado: los 9 pasos en OK.

## 3. En la pantalla (comprobación del autor)

1. Como `consolidacion.demo`, en **Consolidado del periodo**: la etapa dice «Consolidación» y está el botón **Devolver parciales**.
2. **Devolver parciales**:
   - marcar una carrera: aparece su campo de observación;
   - con 5 caracteres, el campo marca el error y **Devolver** sigue deshabilitado;
   - escribir una observación de verdad y **Devolver**.
3. El flujo dice «Propuesta de carrera». La carrera devuelta dice «devuelto por consolidacion.demo el …» con la observación; las demás siguen enviadas.
4. Como `admin.demo`, indicar a `coordinacion.demo` la carrera devuelta (pestaña **Usuarios** de «Roles»). Entrar como `coordinacion.demo`:
   - en **Oferta del periodo** ve la observación y el botón **Reenviar parcial**;
   - la oferta ya no está en solo lectura.
5. Cambiar una propuesta y **Reenviar parcial**. El distributivo vuelve a «Consolidación».
6. Como `consolidacion.demo`, en **Consolidado del periodo**: si hubo un cambio, la lista de lo que falta pide «Consolida otra vez».
7. Como `vicerrectorado.demo`, en **Bitácora**: una `DISTRIBUTIVO_DEVUELTO` con la carrera y su observación («Ver valores»), y después el `PARCIAL_ENVIADO` y la `ETAPA_AVANZADA` del reenvío.
8. Como `vicerrectorado.demo`, en **Consolidado del periodo**: no ve **Devolver parciales**.

**Cronometrar SC-005** (menos de 1 minuto): desde abrir «Consolidado del periodo» hasta ver el flujo en «Propuesta de carrera», con una carrera.

## 4. En el equipo de demostración

Después del despliegue, la migración `V4` completa `admiteDevolucion` en el proceso de 2026-I, que está en EN_CONSOLIDACION. Repetir el apartado 3 con una carrera y anotar el resultado en `evidencia.md`.

## 5. SC-005 de F10-H03 (SC-007)

Tarea con tiempo acotado (research §12). En el perfil local, intentar dejar 2026-I sin inconsistencias duras:
- devolver las carreras;
- proponer los paralelos que faltan con la sugerencia de docentes;
- reenviar, consolidar y adjuntar el informe.

Si el panel llega a cero abiertas, cronometrar «Enviar a revisión» desde que se abre «Consolidado del periodo» y anotarlo en `specs/042-enviar-revision/evidencia.md`. Si no llega, anotar cuántas duras quedan y por qué.
