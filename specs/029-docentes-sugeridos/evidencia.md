# Evidencia: F06-H02

Registro de las comprobaciones de la historia (issue #47): los docentes sugeridos para una asignatura-paralelo, ordenados por afinidad y horas disponibles (RF-25, RN-19, D-01, ADR-010). Los datos son ficticios.

**Lo que esta historia no demuestra.** El escenario «Horas en otras carreras» solo se probó con horas simuladas, porque las asignaciones todavía no existen. Con datos reales, todo docente tiene 0 h asignadas. Se demuestra de verdad al cerrar F06-H03.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## La sugerencia

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| `Sugerencia` y `Candidato`, sin dobles y con valores que no son los de por defecto | 26 de 26: el orden, los empates, las horas con decimales, el mínimo, el tronco común y las horas simuladas | `SugerenciaTest`, `CandidatoTest` | 2026-10-31 |
| Los 165 paralelos de la oferta de 2026-I | 3233 candidatos iguales a una sugerencia hecha aparte, con Python y los CSV, sin el código de Generación: candidatos, puntajes, desglose, horas, cuántos quedaron fuera y orden (SC-001). 40 paralelos de tronco común y 125 sin él; 2036 candidatos de otra carrera base | `probar_sugerencia.sh`, paso 2 | 2026-10-31 |
| El estado laboral | El docente en licencia de los datos no aparece. Uno activo pasa a licencia, desaparece y vuelve | `probar_sugerencia.sh`, pasos 2 y 3 | 2026-10-31 |
| Otra versión con otro mínimo | Con un mínimo de 7, en 2026-II, cambia quién aparece, igual a la sugerencia hecha aparte (SC-002) | `probar_sugerencia.sh`, paso 5 | 2026-10-31 |

## Entre procesos (ADR-023)

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Las rutas internas | Solo con el rol `PROCESO`; un token de usuario recibe 403, también por el API Gateway, que no las enruta | `Interno*ContratoTest`, `probar_sugerencia.sh`, paso 9 | 2026-10-31 |
| Lo mínimo (principio IX) | Los docentes activos no traen correo, teléfono, contrato, institución, registro, carrera base ni asignaturas afines; la asignatura-paralelo no trae palabras clave, estudiantes ni sede | `InternoDocentesContratoTest`, `InternoAcademicoContratoTest` | 2026-10-31 |
| Con `datos` detenido | 504 `COMUN_PROCESO_SIN_RESPUESTA` en 5,0 s, sin lista (meta: menos de 10 s); al volver, responde 200 | `probar_sugerencia.sh`, paso 11 | 2026-10-31 |
| La ficha en los registros | Ningún título ni correo en los logs de `datos`, `distributivo` e `identidad` (SC-005) | `probar_sugerencia.sh`, paso 12 | 2026-10-31 |

## Operación y pantalla

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| `consultarSugerencia` | Contrato, 400 por cada parámetro, 404, 409, 502 y 504; permisos con `VerificadorDePermisos` y un 403 explícito | `SugerenciaContratoTest`, `PermisosGeneracionTest` | 2026-10-31 |
| Rendimiento | La sugerencia, con 46 docentes activos, entre 0,02 y 0,06 s (meta: menos de 3 s, SC-003) | `probar_sugerencia.sh`, paso 10 | 2026-10-31 |
| «Sugerencia» en «Oferta» | 16 pruebas de Vitest y un recorrido de Playwright con axe | `Oferta.test.tsx`, `e2e/oferta.spec.ts` | 2026-10-31 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Backend `./gradlew check` | 1634 pruebas, 0 fallas, 4 omitidas | Local | 2026-10-31 |
| Líneas por módulo | Generación 97,9 %, Académico 82,2 %, Docentes 80,1 %, Normativa 76,1 % | JaCoCo | 2026-10-31 |
| SPA | Vitest 298 de 298 sin cobertura; ESLint, Prettier y TypeScript sin observaciones | Local; ver Notas | 2026-10-31 |
| Cobertura de la SPA | Vitest 298 de 298: 93,89 % de sentencias y 94,01 % de líneas | CI del PR #164 | 2026-10-31 |
| Playwright | 96 de 96 | Local | 2026-10-31 |
| Scripts | Los 19 de `deploy/probar_*.sh` terminan en 0; el de la sugerencia deja la base limpia | Local | 2026-10-31 |
| `probar_equipo.sh` | Código 0 | Local | 2026-10-31 |
| shellcheck y Gitleaks | Sin observaciones | Local | 2026-10-31 |
| CI del PR #164 | 12 de 12 en verde. `probar_sugerencia.sh` en `perfil-consolidado`: 165 sugerencias, 3233 candidatos, la sugerencia en 0,02 s y el 504 en 5,01 s | GitHub Actions | 2026-10-31 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| El despliegue | `0d4b784` (#164): «success · Sano en 77 s». Los 8 contenedores sanos, con las 6 imágenes propias en `…-0d4b7848d135`, y `main` de vuelta en verde | GitHub Deployments, `docker ps` (solo lectura) | 2026-10-31 |
| La comprobación del autor con `coordinacion.demo` | Confirmó lo descrito para `SOCIALES Y HUMANIDADES` en `003`, de tronco común: «Sugerencia» en los paralelos ofertados y no en el retirado; el aviso de tronco común; su docente con 0 puntos, sus horas y 0 h en otras carreras y unidades; sin marca de horas; el desglose vacío; y sin datos de contacto ni botón para proponer | Confirmación del autor | 2026-10-31 |
| SC-006, el tiempo con el coordinador | El autor no informó un tiempo medido, y con un solo candidato la prueba es pobre. Queda sin demostrar: conviene repetirla con F06-H03, cuando el coordinador proponga con varios candidatos | — | 2026-10-31 |
| Lo que no se vio en la demo | Varios candidatos ordenados, un docente fuera por el mínimo y la marca «Sin horas suficientes»: la demo tiene un solo docente y una asignatura de tronco común. Eso quedó comprobado con el script, con los dos procesos reales, y con Playwright | `probar_sugerencia.sh`, `e2e/oferta.spec.ts` | 2026-10-31 |

## Notas

- **Vitest con el perfil levantado:** con los contenedores corriendo, 3 pruebas de 298 fallaron por tiempo, en Docentes, Normativa y Proyectos. Con el perfil apagado pasaron las 298. La cobertura de la SPA se toma de la CI.
- **Orden de las pruebas:** en `Sugerencia`, `Candidato` y la pantalla, se vio fallar la prueba antes de escribir el código. En las consultas de los dueños y la consulta de Generación, prueba y código se escribieron en el mismo paso, así que no se vio ese fallo.
- **Ajustes respecto del plan:** `numero` y `puntos` quedaron en `comun/numeros.ts`, porque ESLint no deja mezclar funciones y componentes en un archivo. `Candidato` guarda `HorasDelDocente` en vez de dos campos. Las horas pasaron a decimales después del análisis.
- **Corridas del script:** el paso 5 falló tres veces por errores del propio script: comparaba `5.50` con `5.5` como texto; un mínimo de 5,5 no cambiaba la lista de esa asignatura, con puntajes 8, 6 y 0; y no miraba el resultado de la comparación independiente. Los tres quedaron corregidos.
- **curl otra vez en las imágenes.** En la CI del PR #164, y antes en `main`, Grype encontró 16 vulnerabilidades más de curl y libcurl 8.22.0-r0, corregidas en 8.23.0-r0: 5 críticas (CVE-2026-86413, CVE-2026-86414, CVE-2026-87861, CVE-2026-92388, CVE-2026-94265) y 11 altas. No vienen de esta historia. Las imágenes oficiales de PostgreSQL 18.6 y Caddy 2.11.7, comprobadas con `docker pull` el 2026-10-31, siguen en 8.22.0: tienen una excepción temporal en `.grype.yaml` hasta el 2026-11-16, aprobada por el autor, con el túnel apagado mientras dure.
- **Un defecto ajeno a esta historia:** la pantalla «Oferta» muestra las horas con un decimal, aunque un paralelo admite dos: 2,25 h se ve 2,3 h. El diálogo de la sugerencia no tiene ese problema.
