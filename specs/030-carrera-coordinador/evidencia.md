# Evidencia: F06-H05

Registro de las comprobaciones de la historia (issue #166): la carrera del coordinador y el filtro de «solo lo propio» en la oferta, la sugerencia y la afinidad (D-16, D-64, RF-02, RF-03). Los datos son ficticios.

## Entorno

| Pieza | Versión | Notas |
|---|---|---|
| Sistema | Fedora Linux 44 (SELinux en modo *enforcing*) | |
| Java | OpenJDK 25.0.4.1 | Gradle con el *wrapper* del repositorio. |
| Node | 24 (`~/.local/node-24`) | Vitest y Playwright. |
| Docker Engine | 29.8.2 (Fedora) | Con `sg docker -c '…'`. |

## La regla de alcance

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| `AlcanceDeCarrera` | 7 de 7, sin dobles: alcance total, solo lo propio, sin carrera, el más amplio gana (RF-03), Talento Humano, y que «Docentes: lectura» no abre la oferta | `AlcanceDeCarreraTest` | 2026-10-31 |
| `AutoridadesDelToken` | 3 de 3: el alcance y la carrera se marcan; sin ellos, nada cambia; el token de proceso sigue igual | `AutoridadesDelTokenTest` | 2026-10-31 |
| Los filtros con tokens reales | Los permisos pasan por `AutoridadesDelToken` en cada prueba de la oferta, la sugerencia y la afinidad | `OfertaContratoTest`, `SugerenciaContratoTest`, `AfinidadContratoTest` | 2026-10-31 |

## Indicar la carrera

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Las reglas | Reemplaza la anterior; 409 sin el rol; 404 al quitar la que no hay; cada cambio queda registrado | `AsignarCarreraServicioTest`, `RolesContratoTest` | 2026-10-31 |
| Quitar el rol | La carrera se borra con él, en la misma transacción | `AsignarRolesServicioTest`, `probar_carrera_coordinador.sh`, paso 11 | 2026-10-31 |
| El token | Lleva la carrera si existe, y no la lleva si no | `EmisorJwtTest` | 2026-10-31 |
| El cambio con la sesión abierta | La sesión sigue con la carrera anterior hasta renovarse (D-62) | `probar_carrera_coordinador.sh`, paso 8 | 2026-10-31 |

## Punta a punta

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Sin carrera | 403 `COMUN_CARRERA_SIN_INDICAR` en la oferta, la sugerencia y la afinidad (SC-002) | `probar_carrera_coordinador.sh`, paso 2 | 2026-10-31 |
| Solo su carrera | 40 paralelos y 160 h, iguales a un conteo hecho aparte con los CSV (SC-001) | `probar_carrera_coordinador.sh`, paso 5 | 2026-10-31 |
| Otra carrera | La sugerencia responde 403 sin datos; la afinidad trae solo las asignaturas de su carrera | `probar_carrera_coordinador.sh`, pasos 6 y 7 | 2026-10-31 |
| Alcance total | Las 7 carreras con `vicerrectorado.demo`, y la afinidad con `datos.demo` (SC-003) | `probar_carrera_coordinador.sh`, paso 9 | 2026-10-31 |
| Rendimiento | La oferta filtrada en 0,03 s (meta: menos de 3 s, SC-005) | `probar_carrera_coordinador.sh`, paso 12 | 2026-10-31 |
| «Roles de usuarios» y los avisos | 12 pruebas de Vitest nuevas y un recorrido de Playwright con axe | `Roles.test.tsx`, `Oferta.test.tsx`, `Docentes.test.tsx`, `e2e/roles.spec.ts` | 2026-10-31 |

## Cierre

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| Backend `./gradlew check` | 1690 pruebas, 0 fallas, 4 omitidas | Local | 2026-10-31 |
| Líneas por módulo | Generación 98,0 %, Usuarios 88,7 %, Académico 82,4 %, Roles 72,5 %, `seguridad` 99,1 %, `errores-nucleo` 92,2 % | JaCoCo, regenerado | 2026-10-31 |
| SPA | Vitest 310 de 310 sin cobertura; ESLint, Prettier y TypeScript sin observaciones | Local; ver Notas | 2026-10-31 |
| Playwright | 97 de 97 | Local | 2026-10-31 |
| Scripts | Los 20 de `deploy/probar_*.sh` terminan en 0; el nuevo deja la base limpia y le devuelve el rol a `coordinacion.demo` | Local | 2026-10-31 |
| `probar_equipo.sh` | Código 0 | Local | 2026-10-31 |
| shellcheck y Gitleaks | Sin observaciones | Local | 2026-10-31 |
| CI del PR #167 | 12 de 12 en verde. Vitest 310 de 310 con cobertura: 94,01 % de sentencias y 94,12 % de líneas. `probar_carrera_coordinador.sh` en `perfil-consolidado` pasa todos sus pasos | GitHub Actions | 2026-10-31 |

## Equipo de demostración

| Comprobación | Resultado | Evidencia | Fecha |
|---|---|---|---|
| El despliegue | `d96237d` (#167): «success · Sano en 66 s». Los 8 contenedores sanos, con las 6 imágenes propias en `…-d96237d03a8e`, y la migración V8 de Roles aplicada | GitHub Deployments, `docker ps`, la base (solo lectura) | 2026-10-31 |
| Sin carrera | Con `coordinacion.demo`, «Falta tu carrera» en la oferta y en la afinidad de la ficha, sin datos | Capturas del autor (no se copian al repositorio) | 2026-10-31 |
| Indicar la carrera | Con `admin.demo`, «Indicar carrera» solo en los dos coordinadores; «Sin carrera» pasó a `SOCIALES` | Capturas del autor | 2026-10-31 |
| Con su carrera | Con `coordinacion.demo` en `003`: `SOCIALES`, sus 19 h, «Sugerencia» en los 3 paralelos ofertados y no en el retirado; las sugerencias de `A` y `A_NOCTURNA` responden | Capturas del autor | 2026-10-31 |
| SC-004, el tiempo | El autor no informó un tiempo medido. Entre su captura con «Sin carrera» y la captura con `SOCIALES` pasaron 28 s, dentro de la meta de 1 minuto: es una aproximación | Hora de las capturas | 2026-10-31 |
| Lo que no se vio en la demo | Una carrera sin oferta, con «No hay oferta para mostrar en este periodo.», y otra carrera filtrada: en esa prueba, solo `SOCIALES` tiene oferta. Quedó comprobado con el script y Vitest | `probar_carrera_coordinador.sh`, `Oferta.test.tsx` | 2026-10-31 |

## Notas

- **Vitest:** se corrió con el perfil apagado, porque con los contenedores levantados algunas pruebas pasan su tiempo de espera. La cobertura de la SPA se toma de la CI.
- **Orden de las pruebas:** en `AlcanceDeCarrera`, `AutoridadesDelToken` y la pantalla, se vio fallar la prueba antes de escribir el código. En Roles, Usuarios, y los filtros de Académico y Generación, prueba y código se escribieron en el mismo paso, así que no se vio ese fallo.
- **Ajustes respecto del plan:**
  - D-76 cambió FR-004: el servidor no comprueba que la carrera exista (research §2);
  - `AlcanceDeCarrera` pasó a `errores-nucleo`, porque los servicios no conocen el token;
  - `ReceptorSimulado` suma `direccionesConsultadas`, para probar la query de la afinidad.
- **Prueba de integración:** `RepositorioCarrerasDeCoordinadoresJdbc` se prueba contra PostgreSQL en `test-integracion`, en la CI.
- **Un texto por revisar:** la oferta filtrada dice «Horas a cubrir del periodo», aunque son las de la carrera del coordinador. En la demo coinciden, porque solo `SOCIALES` tiene oferta. Queda para una historia que toque esa pantalla.
- **Un cambio visible:** al desplegar, `coordinacion.demo` deja de ver la oferta, las sugerencias y los puntajes hasta que `admin.demo` le indique su carrera. Es el comportamiento decidido por el autor.
