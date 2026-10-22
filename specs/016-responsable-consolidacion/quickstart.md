# Quickstart: comprobar F05-H03

Guía para validar la historia de punta a punta. Los detalles están en [research.md](research.md) y el contrato en [contracts/roles.md](contracts/roles.md).

## Antes

- `.env` local con `MDD_CLAVE_PROCESO_DATOS` (o el valor de `.env.ejemplo`).
- El perfil local reconstruido con la rama (`datos` e `identidad`).

## Pruebas automáticas

```bash
cd backend && ./gradlew check integrationTest
cd frontend && npm run lint && npm run formato:verificar && npm run tipos \
  && npm test -- --run --coverage && npm run test:e2e && npm run verificar:ruptura
deploy/probar_periodos.sh        # con los pasos nuevos del Responsable; dos veces seguidas
deploy/equipo/probar_equipo.sh   # con la clave nueva de datos
```

## A mano, en la SPA

1. Entrar como `vicerrectorado.demo`, abrir «Periodos» y registrar un periodo: la columna «Responsable de consolidación» dice «Sin designar».
2. «Designar responsable»: la lista trae a los usuarios con el rol Docente (`docente.demo`, `docente.coordinador.demo`). Elegir `docente.demo` y confirmar.
3. Entrar como `docente.demo`: después de iniciar sesión, su sesión trae los permisos de consolidación (se ve «Periodos» en el menú, por `DISTRIBUTIVO:LECTURA`).
4. Cerrar el periodo con el Vicerrector; en la próxima sesión de `docente.demo`, el rol ya no está.

SC-001: del paso 2 a la confirmación, en menos de 1 minuto.

## En el equipo, antes de fusionar

En tu terminal (no por el chat): `sudo deploy/equipo/preparar.sh`, desde la rama. Agrega `MDD_CLAVE_PROCESO_DATOS` a `/opt/mdd/.env` sin cambiar nada más.
