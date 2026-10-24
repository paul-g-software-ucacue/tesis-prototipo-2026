# Quickstart: comprobar F03-H02

Guía para validar la historia de punta a punta. Los detalles están en [research.md](research.md), el modelo en [data-model.md](data-model.md) y el contrato en [contracts/academico.md](contracts/academico.md).

## Pruebas automáticas

```bash
cd backend && ./gradlew check integrationTest
cd frontend && npm run lint && npm run formato:verificar && npm run tipos \
  && npm test -- --run --coverage && npm run test:e2e && npm run verificar:ruptura
deploy/probar_carreras.sh      # nuevo; dos veces seguidas
```

## A mano, en la SPA

1. Entrar como `vicerrectorado.demo` y abrir «Carreras y mallas».
2. Registrar la carrera `550613A01-P-0301` (DESARROLLO DE SOFTWARE, DS), en la sede matriz, tecnológico superior, presencial, nocturna, campos 06 y 061, 5 ciclos.
3. Registrar la malla `DS-2019` y, en ella, la asignatura `DS-B-I-MD-101` (MATEMÁTICA DISCRETA, ciclo 1, básica, 3 horas, tronco común, 05 y 054, «matemática; lógica; conjuntos»). SC-001: en menos de 2 minutos.
4. Intentar registrar otra sin horas: se marca el campo de las horas.
5. Ver la malla: la asignatura en el ciclo 1, con 3 horas en el ciclo y en la malla.
6. Entrar como `consolidacion.demo`: ve lo mismo, sin acciones. `docente.demo` no ve «Carreras y mallas».
