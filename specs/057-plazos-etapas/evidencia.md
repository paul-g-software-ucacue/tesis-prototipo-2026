# Evidencia: Seguir los plazos de cada etapa

F12-H03 (#58). Cada sección dice qué se verificó, cómo y con qué resultado. Las comprobaciones son del 2026-11-20, sobre la rama `057-plazos-etapas`, antes del pull request.

## Pruebas y comprobaciones (T029)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 3 488 pruebas, 0 fallas | `./gradlew build integrationTest --continue` |
| `integrationTest` completo | 374 pruebas, 0 fallas en los resultados finales. <br>• Validación OCS: V9, con `plazo_vencido` y `configuracion_flujo.advertencia` (`ValidacionocsEsquemaIntegrationTest` +1); `RepositorioPlazosIntegrationTest` (3, nueva); `enCurso` (`RepositorioProcesoIntegrationTest` +1); la advertencia guardada y leída (`RepositorioConfiguracionIntegrationTest` +2); `RevisarPlazosIntegrationTest` (3, nueva): tres revisiones dejan un vencimiento, un evento y una entrada; dos a la vez, uno; otra llegada, otro aviso. <br>• Normativa: V7 con RN-23 y su registro (`NormativaEsquemaIntegrationTest` +1); los conteos de reglas (16 → 17) y de parámetros (38 → 39) de las pruebas que ya existían. | El mismo |
| Fallas en la corrida completa | 1, en un módulo que esta rama no toca: la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest` (`datos`), la misma de F18-H02. Repetida sola, pasa (9 de 9) | `:apps:datos:integrationTest --tests '*ConfirmarLasDiezCargas*' --rerun-tasks` |
| Cobertura (60 % de instrucciones, D-32) | Validación OCS 85,9 %, Normativa 80,2 % y Académico 85,8 % | `jacocoTestReport` |
| Spotless, SpotBugs y arquitectura | En verde, con ArchUnit y Spring Modulith | `build`, `:apps:distributivo:test` |
| Dominio de Validación OCS | <br>• `PlazoTest` (8, nueva): la fecha límite en Ecuador, una llegada a las 20:00 de Ecuador, los tres estados, el atraso, `PlazosDeEtapas` y la suma de 43 días. <br>• `PlazoDelProcesoTest` (18, nueva): la llegada (creación, envío que no es el último, último envío, devolución, rechazo con su versión nueva y reforma), el plazo en PUBLICADO, el vencimiento (vence hoy, vencido, PUBLICADO), las acciones con la etapa vencida, los destinos de cada etapa y otro rol en EN_REVISION_VRA. <br>• `MensajesDePlazoTest` (5, nueva). <br>• `AdvertenciaDeRemisionTest` (8, nueva): −13 días, sin advertencia a 120, el límite exacto, el inicio pasado, el singular, sin normativa y una etapa desactivada. | `./gradlew :servicios:validacionocs:test` |
| Aplicación y contrato de Validación OCS | <br>• `RevisarPlazosServicioTest` (8, nueva): el orden, vence hoy, el periodo cerrado, Académico sin responder, la etapa que cambió entre medio, el vencimiento ya registrado, un periodo que falla y otro que sigue, y sin proceso. <br>• `EtapasAlCrearseTest` (+2), `FlujoDelDistributivoServicioTest` (+4), `CargarConfiguracionServicioTest` (+6), `PublicadorDeNotificacionesTest` (+2) y `DatosDesdeProcesosTest` (+4). <br>• `FlujoContratoTest` (+1) y `ConfiguracionContratoTest` (+5): el plazo, las etapas y la advertencia contra el contrato, y el 504 con Normativa caída. | El mismo |
| Normativa y Académico | `ConsultarRemisionDelPeriodoServicioTest` (4, nueva), `InternoNormativaContratoTest` (+3) e `InternoAcademicoContratoTest` (el `inicio`) | `:servicios:normativa:test :servicios:academico:test` |
| SPA: tipos, ESLint y Prettier | En verde | En `node:24-alpine` |
| SPA: Vitest | 659 pruebas, 0 fallas. `EstadoDelFlujo.test.tsx` (+7): los tres estados, los singulares, la lista con `aria-current` y sin plazo. `Flujo.test.tsx` (+3): el aviso después de cargar, sin normativa y sin advertencias | `vitest run` |
| SPA: Playwright | 138 pruebas, 0 fallas. `consolidado.spec.ts` (+2): la tarjeta a tiempo y vencida. `flujo.spec.ts` (+1): la carga con la advertencia. axe-core (WCAG 2.1 AA) sin fallas (SC-007) | `mcr.microsoft.com/playwright:v1.64.0-noble`, sobre una copia del frontend con sus dependencias de glibc |
| shellcheck | 0 hallazgos en `probar_avance.sh` y `probar_flujo.sh` | `koalaman/shellcheck:v0.10.0` |

## Los scripts (T027, T028)

Sobre un perfil construido desde la rama.

### `probar_avance.sh`: 16 de 16, del 0 al 15, en 101 s

| Paso | Resultado |
|---|---|
| 9. A tiempo (P1) | OK: plazo de 5 días, 5 restantes, `A_TIEMPO` y las 7 etapas activas, con EN_CONSOLIDACION como actual |
| 10. Seis días atrás (P2, P3) | OK: con `identidad` detenido, las transiciones de 2026-I en el mismo orden |
| 11. Al arrancar (P4) | OK: un vencimiento de EN_CONSOLIDACION con su fecha límite, la llegada más 5 días |
| 12. El aviso (P5, SC-004) | OK: uno para `vicerrectorado.demo` y uno para `consolidacion.demo` al volver `identidad`, con el enlace `/consolidado` |
| 13. La bitácora (P6) | OK: `PLAZO_VENCIDO` por `sistema`, 1 día de atraso |
| 14. Sin bloquear (P7, SC-003) | OK: `VENCIDO`, −1 día, y las acciones de quien consolida, las mismas que a tiempo |
| 15. Otro arranque (P8, SC-002) | OK: ni otro vencimiento ni otro aviso. P8 se queda, porque el script completo tarda 101 s |

### `probar_flujo.sh`: 24 de 24, del 0 al 19

| Paso | Resultado |
|---|---|
| 17. A 60 días (R1) | OK: 200 con `RN23_MARGEN_INSUFICIENTE`, el último plazo hoy + 43, la fecha de RN-23 inicio − 30 y −13 días; la consulta la devuelve igual |
| 18. A 120 días (R2) | OK: 200 sin advertencias |
| 19. Sin normativa (R3) | OK: la carga vigente del periodo principal dice `RN23_SIN_NORMATIVA`, sin las cifras |

## Desviaciones del plan

- **Académico sin responder en la revisión** (research §3, D-114): el plan decía dejar el periodo para la próxima revisión. El paso 11 mostró que sin `identidad` no hay token de proceso para preguntar a Académico, y el aviso habría esperado al día siguiente (escenario 8, SC-004). Ahora la revisión sigue como con el periodo abierto, y el aviso se entrega cuando `identidad` vuelve.
- **La revisión al arrancar, en un hilo propio:** en el hilo principal, la espera del token retrasaba el *healthcheck* de `distributivo` unos 40 s. Ahora no retrasa el arranque.
- **`NormativaDelPeriodo`**, un puerto aparte: los días de RN-23 no van en `DatosDelPeriodo`, para no cambiar sus dobles en seis pruebas. Lo implementa el mismo `DatosDesdeProcesos`.
- **`ConfiguracionVigente` no cambia:** la advertencia viaja en su `CargaGuardada`. La de por defecto nunca la tiene.
- **`EtapaDelDistributivo`**, no `EtapaDelFlujo`, en la vista del flujo: ese nombre ya es de la configuración (análisis, I3).
- **Las pruebas que no compilaban sin el código:** en T015, T020, T021, T023 y T024, la prueba se escribió antes que el código, pero no se pudo ver fallar porque usaba una firma nueva. Las demás se vieron fallar contra un esqueleto.
- **`probar_flujo.sh`:** el paso 12 reinicia `identidad`, y los tokens del principio dejan de valer; los pasos 17 y 18 vuelven a iniciar sesión.
- **Un caso conocido** (D-114, análisis A1): una cuenta con COORDINADOR_CARRERA y el rol de EN_REVISION_VRA recibiría dos mensajes del mismo vencimiento en BORRADOR_CARRERA. Ninguna cuenta de la demo tiene los dos roles.

## Recorrido a mano (T031, SC-005, SC-007)

El autor, el 2026-11-20, sobre el perfil local con `MDD_CONSERVAR=1 deploy/probar_documento_firmable.sh` (2026-I en EN_REVISION_VRA), con capturas:

| Paso | Resultado |
|---|---|
| A tiempo | OK: «desde el 20/11/2026. Fecha límite: 25/11/2026. Faltan 5 días.», las 7 etapas y la actual en negrita, leído a simple vista (SC-005) |
| Vencido, tras correr 6 días atrás y reiniciar `distributivo` | OK: «⚠ Plazo vencido», borde rojo, «Vencido hace 1 día»; «Aprobar el contenido» sigue disponible |
| La notificación | OK: una sola para `vicerrectorado.demo`, del sistema, con 14/11, 19/11 y 1 día de atraso; ninguna para `secretaria.demo` |
| La bitácora | OK: `PLAZO_VENCIDO` por `sistema`, fechaLimite 2026-11-19, diasDeAtraso 1 |
| Aprobar con la etapa vencida | OK: pasa a «En firma» con un plazo nuevo, desde hoy |
| RN-23 a 60 días (`PER60`, versión 1) | OK: el texto exacto, con 02/01/2027, 20/12/2026 y 13 días de margen |
| RN-23 en el límite (Propuesta de carrera a 2 días) | OK: la versión 2, sin advertencia |
| Un archivo inválido | OK: rechazado con sus problemas, la versión 2 vigente, sin advertencia nueva |

**Hallazgo corregido:** en «En firma» la tarjeta decía «En En firma desde…». Ahora el nombre va entre comillas, como en la notificación: «En «En firma» desde…» (`EstadoDelFlujo.tsx`, su prueba, `consolidado.spec.ts` y `contracts/spa.md`).

**Segunda vuelta, después de la corrección:**

| Paso | Resultado |
|---|---|
| El texto corregido | OK: «En «En firma» desde el 20/11/2026. Fecha límite: 25/11/2026. Faltan 5 días.» |
| RN-23 sin versión de normativa (`2026-3`, con `docente.demo` designado) | OK: la carga queda vigente (versión 2) y la advertencia dice que no se pudo comprobar RN-23, con la aprobación estimada el 02/01/2027 y sin la fecha de RN-23 ni el margen |
| El teclado | OK: la tarjeta y «Flujo y firmas» se recorren con Tab |
| El lector de pantalla | No se probó con Orca. Lo cubren axe-core sin fallas y los roles de las pruebas (`group` y `status` con su nombre, `aria-current="step"`) |

## Equipo de demostración (T033)

| Comprobación | Resultado |
|---|---|
| Fusión | PR #253 (`d31bf16`), con las 12 comprobaciones en verde en la segunda corrida. En la primera falló `probar_normativa.sh`, que contaba 16 reglas en la versión 1; con RN-23 son 17 (`b9731b1`) |
| Despliegue | `cd` exitoso; `mdd-actualizar` tomó `d31bf16` y dice «Sano en 57 s», con las cinco comprobaciones del proxy en OK; los 8 contenedores sanos con las imágenes `*-d31bf16ae93d` |
| Migraciones | Validación OCS en V9 (`plazo_vencido` y `configuracion_flujo.advertencia`) y Normativa en V7 (RN-23), las dos con éxito |
| El vencimiento real | La revisión al arrancar registró el de 2026-I: EN_CONSOLIDACION desde el 08/11/2026, plazo de 5 días, fecha límite 13/11/2026, detectado el 20/11/2026 a las 14:32 |
| Los avisos | Uno por usuario, «Venció el plazo de «Consolidación»», a las 14:32: `consolidacion.demo` (el rol de la etapa), `docente.demo` (el Responsable designado del periodo, D-108) y `vicerrectorado.demo`; ninguna publicación pendiente en `eventos_distributivo` |
| Memoria | Después del arranque, `identidad` usa 431 de sus 768 MiB (56 %), `distributivo` 235 de 512 y `datos` 235 de 512 (D-109) |

Es la evidencia en vivo de la historia, sin simular nada: 2026-I llevaba semanas en consolidación.

## Criterios de terminado

| Criterio | Estado |
|---|---|
| SC-001: cada etapa que vence avisa; ninguna que vence hoy | Cumplido: `PlazoDelProcesoTest`, `RevisarPlazosServicioTest` y el paso 11 |
| SC-002: un aviso por vencimiento | Cumplido: `RevisarPlazosIntegrationTest` y el paso 15 |
| SC-003: con la etapa vencida, el flujo sigue igual | Cumplido: `PlazoDelProcesoTest` y el paso 14 |
| SC-004: con `identidad` detenido, un aviso antes de 2 minutos desde que vuelve | Cumplido: el paso 12 |
| SC-005: la tarjeta, legible en menos de 10 s | Cumplido en el recorrido del autor |
| SC-006: la advertencia cuando falta margen, nunca cuando sobra | Cumplido: `AdvertenciaDeRemisionTest` y los pasos 17 y 18 |
| SC-007: sin fallas de WCAG 2.1 AA | Cumplido en axe-core y con el teclado en el recorrido; sin probar con un lector de pantalla |
