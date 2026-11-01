# Evidencia: F06-H04

Registro de las comprobaciones de la historia (issue #49): el catálogo de actividades, asignar y quitar actividades no lectivas, y las horas completas de cada docente (RF-21, RF-26, RN-22, RN-25, C-12, C-13). Los datos son ficticios.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | Fedora | Con `sg docker -c '…'`. |

## El catálogo

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Las 47 actividades (SC-001) | 14, 9, 9 y 15 por componente, comparadas con una lista escrita aparte; sin 140 a), 143 i) ni «Las demás…» | `CatalogoDeActividadesIntegrationTest` | 2026-11-01 |
| El texto de la norma | Los 47 textos coinciden con el PDF de la política de marzo de 2024, con los espacios normalizados | Comparación local | 2026-11-01 |
| Lo que exige cada una | Proyectos y cargos, comparados con la lista escrita aparte | `CatalogoDeActividadesIntegrationTest` | 2026-11-01 |
| Una versión nueva | Copia el catálogo de la anterior, y queda registrado | `CatalogoDeActividadesIntegrationTest` | 2026-11-01 |

## Asignar

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Una sola actividad (SC-003) | Registrar otra vez reemplaza las horas, y el registro guarda las anteriores | `RepositorioDistributivosIntegrationTest`, `probar_actividades.sh`, paso 3 | 2026-11-01 |
| El proyecto (SC-002) | Sin él, ajeno, de otro tipo o de más: 409, sin guardar | `ActividadDelCatalogoTest`, `probar_actividades.sh`, paso 4 | 2026-11-01 |
| El cargo (SC-002) | Sin él, 409; con él se guarda; 143 k) no lo pide | `ActividadDelCatalogoTest`, `probar_actividades.sh`, paso 5 | 2026-11-01 |
| Las horas | 0, 41 y 3 decimales se rechazan; 40 se acepta | `ActividadNoLectivaTest`, `ActividadesContratoTest`, `probar_actividades.sh`, paso 6 | 2026-11-01 |
| El aviso al reemplazar | No cuenta las horas anteriores de la misma actividad | `AsignarActividadServicioTest`, `HorasDelPeriodoTest` | 2026-11-01 |
| Otra carrera | 403 sin datos; el Vicerrector ve las 7 carreras y no asigna | `ActividadesContratoTest`, `probar_actividades.sh`, paso 7 | 2026-11-01 |
| Periodo cerrado | No se asigna ni se quita, tampoco una propuesta: corrige F06-H03 | `probar_actividades.sh`, paso 10 | 2026-11-01 |

## Las horas

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Propuestas y actividades juntas (SC-004) | Desde cada coordinador, las horas en otras carreras y las disponibles coinciden con una suma aparte: 8,25 h y 3 h | `HorasDelPeriodoTest`, `probar_actividades.sh`, paso 8 | 2026-11-01 |
| El aviso de horas | La sugerencia, proponer y los parciales cuentan las actividades | `ConsultarSugerenciaServicioTest`, `ProponerDocenteServicioTest`, `ConsultarParcialesServicioTest` | 2026-11-01 |

## Rendimiento y fallos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Guardar (SC-006) | 0,18 s (meta: menos de 3 s) | `probar_actividades.sh`, paso 3 | 2026-11-01 |
| Sin el proceso `datos` (SC-007) | 504 del catálogo en 5,0 s (meta: menos de 10 s), sin guardar nada, y vuelve | `probar_actividades.sh`, paso 9 | 2026-11-01 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Backend `./gradlew check` | 1821 pruebas unitarias, 0 fallas, 4 omitidas | Local | 2026-11-01 |
| `integrationTest` | 202 de 202, en todos los módulos | Local | 2026-11-01 |
| Líneas por módulo | Generación 90,0 %, Normativa 75,5 %, Docentes 80,4 % | JaCoCo, regenerado | 2026-11-01 |
| Pruebas primero | Cada grupo se vio fallar antes de su código, con un esqueleto. T022 no pudo fallar primero, porque T021 ya había hecho el cambio: se rompieron a propósito las 3 llamadas, y las 3 se detectaron | Local | 2026-11-01 |
| SPA | Vitest 369 de 369 con cobertura: 94,39 % de sentencias y 94,49 % de líneas; ESLint, Prettier y TypeScript sin observaciones | Local, con la lista corregida | 2026-11-01 |
| Playwright | 103 de 103, con axe sobre la pantalla, el diálogo y la lista abierta | Local, con la lista corregida | 2026-11-01 |
| Scripts | Los 22 de `deploy/probar_*.sh` terminan en 0, sin avisos de limpieza | Local | 2026-11-01 |
| `probar_equipo.sh` | Código 0 | Local | 2026-11-01 |
| shellcheck | Sin observaciones | Local | 2026-11-01 |
| CI de la historia | 12 de 12 en verde, con `perfil-consolidado` y `5 · imágenes` | PR #171 | 2026-11-01 |
| CI de la corrección | 11 de 11 en verde; `5 · imágenes` no corre en un PR que no toca el despliegue | PR #172 | 2026-11-01 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Despliegue de la historia | `demostracion` exitoso, «Sano en 58 s», 8 contenedores sanos; V5 de Normativa y V4 de Generación aplicadas; 47 actividades en cada una de las 2 versiones | GitHub y consultas de solo lectura, `bd0de673c7b4` | 2026-11-01 |
| Despliegue de la corrección | `demostracion` exitoso, «Sano en 49 s», 8 contenedores sanos | GitHub, `b31e3e992d32` | 2026-11-01 |
| Lista que se lee | El texto largo se parte, y el texto completo de lo elegido queda debajo del campo | Comprobación del autor | 2026-11-01 |
| Asignar | Actividades de otras actividades de docencia y de gestión, cada una en su componente, con la suma por componente | Comprobación del autor | 2026-11-01 |
| Reemplazar (SC-003) | 140 b) pasó de 12 h a 22 h y de 22 h a 12 h, con una sola fila, y el registro guarda las horas anteriores | Comprobación del autor y registro de cambios | 2026-11-01 |
| Quitar | 5 quitadas, con quién y cuándo en el registro | Comprobación del autor y registro de cambios | 2026-11-01 |
| El aviso de horas | Con 20 h contratadas y 9 h propuestas: 15 h sin aviso; 21 h, 33 h y 42 h con aviso | Comprobación del autor | 2026-11-01 |
| Sin proyecto | 140 f) con el proyecto suspendido dice el motivo y no deja asignar | Comprobación del autor | 2026-11-01 |
| El Vicerrector | Ve las actividades sin acciones | Comprobación del autor | 2026-11-01 |
| Rapidez (SC-005) | Indicada, no medida con cronómetro: 14 guardados y quitados en 4 min 53 s, con asignaciones separadas por 15 a 79 s | Horas del registro de cambios | 2026-11-01 |

Lo que no se vio en la demo, y queda cubierto por las pruebas y por `probar_actividades.sh`:

- **Con un proyecto asignable:** el proyecto siguió suspendido, así que no se asignó ninguna actividad con proyecto (paso 4).
- **Con cargo:** no se guardó 143 d) con el cargo vigente ni se vio el rechazo de 143 e) (paso 5).
- **Horas en otra carrera:** la demo tiene un solo coordinador (paso 8; SC-004).

## Notas

- **Errores que encontraron las pruebas:** el aviso de horas se perdía al guardar, porque la pantalla leía un estado anterior; y la llave foránea del catálogo impedía borrar la versión de prueba, lo que rompía la limpieza de 10 scripts. Por eso el catálogo se borra en cascada con su versión.
- **Dos pruebas de V4** contaban todos los registros del sistema: ahora miran solo los de V4, porque V5 agrega el suyo.
- **Vitest:** la prueba larga de registrar un docente tiene 30 s, porque con cobertura agotaba los 15 s de vez en cuando.
- **Defecto que encontró el autor en la demo:** la lista nativa del navegador no parte las líneas, y las actividades largas salían cortadas y fuera de la pantalla. Lo corrigió el PR #172 con una lista que parte el texto y se puede buscar, también para el proyecto. Las pruebas no lo vieron porque usaban textos cortos: ahora una usa el más largo del catálogo, a 1280 y 390 px.
- **Una falla aislada:** en una corrida de Vitest con cobertura falló una prueba de `Carreras.test.tsx`, que la corrección no toca. No quedó registrado el motivo, y las 2 corridas siguientes pasaron completas.
- **A 390 px:** el formulario cabe, pero la página tiene desplazamiento horizontal, porque el menú lateral fijo ocupa 240 px. Es común a todas las pantallas y no se tocó aquí.
- **Periodo cerrado:** el paso 10 lo cierra directo en la base, porque solo importa lo que se lee.
