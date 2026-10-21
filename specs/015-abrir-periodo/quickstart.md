# Quickstart: comprobar F02-H01

Guía para validar la historia de punta a punta. Los detalles están en [research.md](research.md) y el contrato en [contracts/academico.md](contracts/academico.md).

## Antes

- El perfil local reconstruido con la rama (`academico` en V2).
- Node 24 en `~/.local/node-24/bin` para la SPA.

## Pruebas automáticas

```bash
cd backend && ./gradlew check integrationTest     # dominio, aplicación, contrato, Testcontainers, ModulosTest
cd frontend && npm run lint && npm run formato:verificar && npm run tipos \
  && npm test -- --run --coverage && npm run test:e2e && npm run verificar:ruptura
deploy/probar_periodos.sh                          # 9 pasos, en OK; dos veces seguidas
deploy/probar_normativa.sh                         # sin cambios: sigue en OK
```

Esperado: todo en verde, 60 % de líneas en Académico, y `probar_periodos.sh` deja la base como estaba (sin periodos de prueba ni versiones nuevas).

## A mano, en la SPA

1. Entrar como `vicerrectorado.demo` y abrir «Periodos»: la lista vacía y «Registrar periodo».
2. Registrar `2026-2`, «Periodo 2026-2», del 2026-09-01 al 2027-02-28: queda en Planificación, «Sin versión», y «Pasar a En aprobación» está deshabilitado con el motivo.
3. «Abrir»: elegir una versión de la lista y confirmar. El periodo muestra la versión; en «Normativa», esa versión dice «Rige: 2026-2» y ya no se puede editar.
4. Pasar a En aprobación, a Vigente y a Cerrado; Cerrado pide confirmación. Ya cerrado, no hay acciones.
5. Entrar como `rectorado.demo`: ve la lista sin acciones. `docente.demo` no ve «Periodos» en el menú.

SC-001: del paso 1 a «En aprobación» en menos de 2 minutos.

## En el equipo de demostración

Abrir un periodo **deja su versión de normativa fija para siempre**. En `https://mdd.localhost`, usar una versión y un código de periodo que el autor elija para la demo.
