# Evidencia: F07-H02

Registro de las comprobaciones de la historia (issue #51): las reglas duras al guardar una asignación, con el total del docente en todas las carreras (RF-28, RN-02, RN-13, RN-25, MD-01). Los datos son ficticios.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | Fedora | Con `sg docker -c '…'`. |

## Las reglas

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Horas excedidas (SC-001) | 409 `GENERACION_HORAS_EXCEDIDAS`, «Horas laborales excedidas», con «Le quedan 1 h» | `MotorReglasTest`, `probar_reglas_duras.sh`, paso 2 | 2026-11-01 |
| En dos carreras | Cuentan las dos, y justo al límite se guarda | `probar_reglas_duras.sh`, paso 2 | 2026-11-01 |
| Tope de clase | 409 `GENERACION_TOPE_DE_CLASE` con investigación o vinculación; otras de docencia se guardan; con prácticas no aplica | `MotorReglasTest`, `ProponerDocenteServicioTest`, `probar_reglas_duras.sh`, paso 3 | 2026-11-01 |
| Las cifras (principio VI) | Salen de la versión del periodo, y otra cifra se aplica | `ConsultarReglasDeAsignacionServicioTest`, `ProponerDocenteServicioTest` | 2026-11-01 |
| Docente excedido | Reduce, repite y quita; no aumenta ni agrega | `MotorReglasTest`, `probar_reglas_duras.sh`, paso 7 | 2026-11-01 |
| Sin proyecto (RN-25) | 409 `GENERACION_PROYECTO_NO_VALIDO` | `AsignarActividadServicioTest`, `probar_reglas_duras.sh`, paso 8 | 2026-11-01 |
| Rechazo sin rastro | No guarda, no crea el parcial y no deja registro | `ProponerDocenteServicioTest`, `AsignarActividadServicioTest` | 2026-11-01 |

## A la vez

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Sin bloqueo | Los dos guardados leen la versión vacía: la prueba falla con 2 filas | `BloqueoDeLaVersionIntegrationTest`, con el esqueleto | 2026-11-01 |
| Con bloqueo | El segundo ve lo del primero | `BloqueoDeLaVersionIntegrationTest` | 2026-11-01 |
| 20 pares (SC-002) | 20 guardados y 20 rechazados con el error de horas, nunca excedido | `probar_reglas_duras.sh`, paso 4 | 2026-11-01 |
| Docentes distintos | Los dos se guardan | `BloqueoDeLaVersionIntegrationTest`, `probar_reglas_duras.sh`, paso 5 | 2026-11-01 |

## Lo que se ve

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Sugerencia y guardado (SC-003) | El motivo coincide con el guardado, y quienes se pueden proponer van primero | `ConsultarSugerenciaServicioTest`, `SugerenciaTest`, `probar_reglas_duras.sh`, paso 6 | 2026-11-01 |
| Ya propuesto | No se cuenta dos veces, y un excedido no sale con motivo | `ConsultarSugerenciaServicioTest` | 2026-11-01 |
| Pantalla | Sin «Proponer» y con el motivo; el error sale en el formulario, que sigue abierto; sin el aviso de D-78 | `Oferta.test.tsx`, `Actividades.test.tsx`, `e2e/reglas.spec.ts` | 2026-11-01 |

## Rendimiento y fallos

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Guardar (SC-005) | 0,12 s (meta: menos de 3 s) | `probar_reglas_duras.sh`, paso 9 | 2026-11-01 |
| Sin el proceso `datos` (SC-006) | 504 en 5,0 s (meta: menos de 10 s), sin guardar, y vuelve | `probar_reglas_duras.sh`, paso 10 | 2026-11-01 |
| Cobertura del motor (SC-007) | `MotorReglas` 100 % de líneas y de ramas (meta: 80 %) | JaCoCo, regenerado | 2026-11-01 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Backend `./gradlew check` | 1952 pruebas unitarias, 0 fallas, 4 omitidas | Local | 2026-11-01 |
| `integrationTest` | 204 de 204, en todos los módulos | Local | 2026-11-01 |
| Líneas por módulo | Generación 91,3 %, Normativa 76,4 %, Docentes 80,6 % | JaCoCo, regenerado | 2026-11-01 |
| SPA | Vitest 392 de 392 con cobertura: 94,5 % de sentencias y 94,59 % de líneas; ESLint, Prettier y TypeScript sin observaciones | Local | 2026-11-01 |
| Playwright | 107 de 107, con axe | Local | 2026-11-01 |
| Scripts | Los 24 de `deploy/probar_*.sh` terminan en 0, sin avisos de limpieza | Local | 2026-11-01 |
| `probar_equipo.sh` | 13 controles OK | Local | 2026-11-01 |
| Registros del CI | 1038 líneas revisadas: ningún token, contraseña ni clave | Local | 2026-11-01 |
| CI | 12 de 12 en verde, con `perfil-consolidado` a la primera | PR #182 | 2026-11-01 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Despliegue | `demostracion` exitoso, «Sano en 52 s», 8 contenedores sanos; los datos del autor siguen; 401 sin credenciales | GitHub y consulta de solo lectura, `09685614ef43` | 2026-11-01 |
| Horas excedidas | Con 14 de 20 h, 7 h más: «Horas laborales excedidas» y «Le quedan 6 h», con el formulario abierto y lo escrito | Comprobación del autor | 2026-11-01 |
| Justo al límite | 6 h se guardan: 20 de 20 h y 0 disponibles | Comprobación del autor | 2026-11-01 |
| Sin espacio | 0,5 h: rechazada con «Le quedan 0 h» | Comprobación del autor | 2026-11-01 |
| Reducir y repetir | 140 c) de 5 h a 0,5 h, y 140 b) otra vez con 6 h: se guardan, con las horas anteriores en el registro | Registro de cambios | 2026-11-01 |
| Sugerencia | Sin «Proponer» y con «No se le puede proponer: no le alcanzan las horas.» | Comprobación del autor | 2026-11-01 |

Lo que no se vio en la demo, y queda cubierto por las pruebas:

- **El tope de 16 h:** el único proyecto de la demo es de prácticas y está suspendido (`probar_reglas_duras.sh`, paso 3).
- **Dos coordinadores a la vez y horas en otra carrera:** la demo tiene un solo coordinador (pasos 2, 4 y 5).
- **Un docente excedido:** no hay ninguno, y por la pantalla ya no se puede crear (paso 7).
- **SC-004:** el autor no tomó el tiempo, así que queda sin medir en la demo.

## Notas

- **Pruebas primero:** cada grupo se vio fallar con un esqueleto antes de su código: Normativa (5), Docentes (1), el motor (10), la versión y el cliente (7), los dos servicios (18), la concurrencia (1), la sugerencia (5) y la SPA (4). En Normativa la primera vez no compiló y se escribió el código antes de verla fallar: se volvió al esqueleto, se vio fallar y se restauró. Lo mismo no se repitió después.
- **El fallo intermitente de `perfil-consolidado` (D-84):** apareció en el PR #180 y se reprodujo aquí. Mientras un contenedor está detenido, Docker no resuelve su nombre, y Java recuerda ese fallo 10 s; si vuelve antes, los demás responden 502 aunque ya esté sano. Medido: unos 2 s de 502 en 3 de 3 intentos antes del cambio, y ninguno en 3 de 3 después. Una primera prueba pareció descartarlo, porque usé una propiedad que Java ignora; con la propiedad correcta se confirmó.
- **Registros en el CI:** si `perfil-consolidado` falla, `docker compose logs` queda como artefacto por 7 días.
- **`ConsultarParcialesServicio`:** su `horasSuficientes` sigue usando las horas contratadas, no el límite. Hoy coinciden, porque las horas contratadas llegan hasta 40.
- **Scripts que cambiaron:** `probar_totales.sh` crea el exceso directo en la base, porque por la API ya no se puede.
