# Plantillas de servicio y de proceso

Esqueletos que se copian cuando una historia crea un servicio o un proceso del MDD (ADR-012). No forman parte del build: no están en `settings.gradle`.

Las garantías de cada esqueleto (reglas de las cinco capas, API publicada, cobertura del 60 %, Spotless y SpotBugs) están en el contrato [`specs/001-repo-plantilla-ci/contracts/plantilla-servicio.md`](../../specs/001-repo-plantilla-ci/contracts/plantilla-servicio.md).

**Nombres únicos:** ningún servicio ni proceso puede repetir el nombre de otro subproyecto. Gradle los identifica por grupo (`ec.mdd`) y nombre, y dos con el mismo nombre crean una dependencia circular.

## Crear un servicio

1. Copiar `plantillas/servicio/` a `servicios/<servicio>/`. El nombre va en español, sin número ni guiones (`normativa`, `validacionocs`).
2. En la copia, cambiar el paquete `ec.mdd.servicio` por `ec.mdd.<servicio>`, en las carpetas y en las líneas `package`.
3. Agregar `include 'servicios:<servicio>'` en `backend/settings.gradle`.
4. Agregar `implementation project(':servicios:<servicio>')` en el `build.gradle` de su proceso.

## Crear un proceso

1. Copiar `plantillas/aplicacion/` a `apps/<proceso>/` (`identidad`, `datos`, `distributivo` o `salida`).
2. Renombrar `ProcesoAplicacion` a `<Proceso>Aplicacion`, también en `ModulosTest`. La clase se queda en el paquete `ec.mdd`.
3. Agregar `include 'apps:<proceso>'` en `backend/settings.gradle`.
