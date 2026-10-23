# Quickstart: comprobar F03-H01

Guía para validar la historia de punta a punta. Los detalles están en [research.md](research.md) y el contrato en [contracts/academico.md](contracts/academico.md).

## Pruebas automáticas

```bash
cd backend && ./gradlew check integrationTest
cd frontend && npm run lint && npm run formato:verificar && npm run tipos \
  && npm test -- --run --coverage && npm run test:e2e && npm run verificar:ruptura
deploy/probar_estructura.sh      # nuevo; dos veces seguidas
```

## A mano, en la SPA

1. Entrar como `admin.demo` y abrir «Estructura orgánica»: vacía, con «Registrar unidad».
2. Registrar la sede matriz `MATRIZ`, luego `VRA` (Gobierno, debajo de `MATRIZ`) y `G-INV` (Gestión, debajo de `VRA`, gestión autorizada G07).
3. Registrar `COM-EVAL` (Comisión, permanente): aparece como «Informativa, sin horas».
4. Intentar inactivar `VRA`: «Unidad con dependientes». Inactivar `G-INV` y después `VRA` sí.
5. Entrar como `vicerrectorado.demo`: ve la estructura sin acciones. `docente.demo` no la ve en el menú.

SC-001: el paso 2 para `G-INV`, en menos de 1 minuto.
