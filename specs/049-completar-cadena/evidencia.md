# Evidencia: Validar firmante, turno y vigencia, y completar la cadena

F14-H03 (#64). Cada sección dice qué se verificó, cómo y con qué resultado. Las comprobaciones son del 2026-11-14 y el 2026-11-15, sobre la rama `049-completar-cadena`, antes del pull request.

## Pruebas y comprobaciones (T017 a T020)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 3 146 pruebas | `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --continue` |
| `integrationTest` completo | 308 pruebas, en 72 archivos; corrida otra vez sola el 2026-11-15, después del recorrido del autor: 308, 0 fallas. Firma: la V5 con el rol del proceso y el nombre impreso guardado al generar; `apps:distributivo` con `DocumentoFirmableIntegrationTest`, que firma F1, F2 y F3 con los nombres de los dobles y deja el distributivo en EN_OCS | `./gradlew integrationTest --rerun-tasks --continue` |
| Fallas en la corrida completa | 2, las mismas de F13-H02, F14-H01 y F14-H02, en módulos que esta rama no toca, y las dos pasan corridas solas: `ClienteDeEventosLocalTest` (5/5) y la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest` (9/9) | Cada una con `--rerun` |
| Cobertura por módulo (60 % de líneas, D-32) | Pasa en todos: Firma 82,6 %, Validación OCS 83,8 %; la más baja, Roles, 77,2 % | `jacocoTestReport` |
| Spotless, SpotBugs y arquitectura | En verde: ArchUnit y Spring Modulith. Firma sigue sin conocer el flujo: el evento va en su API publicada y Aprobación lo escucha (MD-05) | `build`, `ArquitecturaTest`, `ModulosTest` |
| Las tres comprobaciones nuevas | `ComprobacionDelFirmanteTest`: vigente, vencido, todavía no vigente, el paso en turno, uno posterior, uno anterior, otro nombre, sin CN y la misma persona en dos pasos. `NombreDeFirmanteTest`: mayúsculas, tildes, Ñ y espacios. `ResultadoValidacionTest`, con siete | `./gradlew :servicios:firma:test` |
| La subida | `SubirFirmaServicioTest` (21): el paso sin nombre (409, sin validar), cada rechazo nuevo con su código, el aviso solo con la última firma y dentro de la transacción | El mismo |
| La cadena completa | `ProcesoAprobacionTest` (+4): en firma, repetido, en otra etapa y con otra versión. `CompletarFirmasServicioTest` (3) | `./gradlew :servicios:validacionocs:test` |
| El nombre local | `FirmantesConNombreLocalTest`: con nombre, lo devuelve en cada rol y conserva el usuario o la identificación; vacío, delega sin cambios. Una prueba comprueba que `.env.ejemplo` no le da valor | `./gradlew :servicios:firma:test` |
| Contrato y catálogo | El 422 de los tres códigos nuevos con siete comprobaciones, y el 409 del paso sin nombre (`SubirFirmaContratoTest`, `ErroresFirmaTest`) | Las mismas |
| SPA: tipos, ESLint y Prettier | En verde | En `node:24-alpine` |
| SPA: Vitest | 574 pruebas, 0 fallas; `FirmasPendientes.test.tsx` con las siete comprobaciones | `vitest run` |
| SPA: Playwright | 128 pruebas, 0 fallas; `e2e/firmas-pendientes.spec.ts` con siete comprobaciones y axe-core (WCAG 2.1 AA) sin fallas | `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Scripts de `perfil-consolidado` (T018, SC-005) | Los 35 del workflow pasan, en su orden, sobre un perfil construido desde la rama; `probar_documento_firmable.sh` en 74 s y `probar_aprobacion.sh` en 46 s | Uno tras otro, como el workflow |
| shellcheck | 0 hallazgos en `probar_documento_firmable.sh` | `koalaman/shellcheck:v0.11.0` |
| El nombre del autor (SC-004) | No aparece en ningún archivo de `HEAD` ni en el contenido de los commits de la rama | `git grep` y `git log -p --format= main..HEAD`, contando solo coincidencias |

### `probar_documento_firmable.sh` (contracts/demostracion.md)

13/13 en la primera corrida, el 2026-11-14, sobre un perfil construido desde la rama. El 2026-11-15, dentro del workflow, otra vez 13/13.

| Paso | Resultado |
|---|---|
| 0 a 8 | OK, como en F14-H02 |
| 9. Los rechazos (SC-001) | OK: siete PDF rechazados con su 422 y las siete comprobaciones. A los cinco de F14-H02 se suman la firma de la persona de F2 en el turno de F1 (`FIRMA_FUERA_DE_ORDEN`) y un certificado vencido al subir (`FIRMA_CERTIFICADO_NO_VIGENTE`). El turno y el vigente no cambian |
| 10. Fuera de turno | OK: `vicerrectorado.demo` y `consolidacion.demo` reciben 403 al subir, sin dejar fila |
| 11. Las tres firmas (SC-001) | OK: en el turno de F2, la firma de la persona de F1 se rechaza (`FIRMA_FIRMANTE_EQUIVOCADO`); F1, F2 y F3 se aceptan en orden; al final nadie está en turno |
| 12. La cadena completa (SC-002) | OK: 3 aceptadas y 8 rechazadas, los 4 archivos y la bitácora; el proceso en EN_OCS, con la transición por `sistema` y la entrada `CADENA_DE_FIRMAS_COMPLETA` |

El certificado vencido (válido del 2025-01-01 al 2025-02-01) firma con `FirmarDePrueba.java --momento=2025-01-15T12:00:00Z`. Así su firma pasa la confianza, que se evalúa en la fecha de la firma, y la vigencia la rechaza al subir.

## Comprobación a mano con FirmaEC (T018, SC-002, SC-003, SC-004)

Hecha por el autor el 2026-11-15, con `quickstart.md` §3, sobre un perfil construido desde la rama, con FirmaEC y su certificado real de la ECIBCE. Su nombre estuvo solo en su `.env` local, que Git ignora. Ni el certificado, ni los PDF firmados, ni las capturas entraron al repositorio, y aquí no se anota ningún dato del certificado.

| Comprobación | Resultado |
|---|---|
| El nombre local | Con `MDD_FIRMA_NOMBRE_LOCAL` en su `.env`, la variable llegó al contenedor `distributivo`. Se comprobó solo su largo y que no tuviera dígitos, `=` ni `+`, sin leer el valor. El PDF generado al aprobar imprimió el nombre del autor en «Elaborado por», «Revisado por» y «Aprobado por» |
| F1, F2 y F3 | `docente.demo`, `vicerrectorado.demo` y `rectorado.demo` descargaron, firmaron con FirmaEC y subieron. Las tres se aceptaron con las siete comprobaciones aprobadas, también vigencia, orden y firmante. La base: 3 aceptadas y 0 rechazadas |
| EN_OCS (SC-002) | El proceso quedó en EN_OCS, con la transición CONGELADO_EN_FIRMA → EN_OCS por `sistema`. El autor la vio en «Consolidado del periodo» a los 30 s de subir F3, con el cambio de usuario y la apertura de la pantalla incluidos: la pantalla no se refresca sola |
| El PDF final (SC-003) | FirmaEC, con «Verificar documento»: 3 firmantes, las tres firmas válidas, con su sello visible en el recuadro de cada paso |
| Limpieza | El autor quitó la variable de su `.env` y el perfil se bajó con `down -v`, que borró la base local con su nombre |

**Hallazgos del recorrido:**
1. **La transición guarda la hora de la firma.** El aviso lleva la hora en que se subió F3, y Aprobación registra la transición con ella. Por eso la base no mide la demora entre la firma y EN_OCS (da 0 s). El oyente corre después de confirmar la subida; la medida de SC-002 es la del script, que consulta el estado con un límite de 30 s, y la del autor en la pantalla.
2. **FirmaEC agrega un sello de tiempo.** Cada firma lleva un sello acreditado de la TSU de MINTEL. El MDD no lo valida ni lo exige (ADR-007): la vigencia se evalúa al subir.

## Desviaciones del plan

| Plan | Lo hecho | Por qué |
|---|---|---|
| Un archivo local fuera del repositorio para el nombre del autor | La variable `MDD_FIRMA_NOMBRE_LOCAL` en su `.env` | El `.env` ya es el archivo local que Git ignora y compose lee; no hace falta montar otro (research §6) |
| EN_OCS «en menos de 30 s», medido en la base | Medido por el script, que consulta el estado, y por el autor en la pantalla | La transición guarda la hora de la firma (hallazgo 1) |
| El reintento del aviso, probado | No se prueba aparte | Lo cubre el registro de publicaciones de Spring Modulith (ADR-018), como el paso a firma de F13-H02; `completarFirmas` es idempotente y eso sí se prueba (análisis, A4) |
| El rechazo por «firmante equivocado» con FirmaEC | No se hizo en el recorrido a mano | Era opcional: necesita firmar con la herramienta de prueba. Lo cubren el script (paso 11) y las pruebas |
| Ver fallar cada prueba antes del código | Así en el dominio, la aplicación, el repositorio, el contrato, el nombre local y Vitest. Pasaron al escribirse: `RepositorioDocumentosIntegrationTest` con el nombre (T010), `DocumentoFirmableIntegrationTest` de la cadena completa (T014, escrita con el código ya hecho), `PublicadorDeCambiosTest` con la acción nueva (el publicador ya era genérico) y las pruebas nuevas de Playwright | Se cuenta tal como pasó |

**Limitación conocida.** Las dos fallas de la corrida completa (`ClienteDeEventosLocalTest` y la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest`) son de módulos que esta rama no toca y pasan solas: dependen de la carga del equipo.

## Criterios de terminado (constitución §6)

| Criterio | Dónde |
|---|---|
| Escenarios de aceptación | `probar_documento_firmable.sh` (pasos 9 a 12); `NombreDeFirmanteTest`, `ComprobacionDelFirmanteTest`, `ResultadoValidacionTest`, `FirmanteEsperadoTest`, `SubirFirmaServicioTest`, `DocumentoFirmableServicioTest`, `FirmantesConNombreLocalTest`, `SubirFirmaContratoTest`, `FirmasPendientesContratoTest`, `ErroresFirmaTest`, `RepositorioDocumentosIntegrationTest`, `DocumentoFirmableEsquemaIntegrationTest`, `ProcesoAprobacionTest`, `CompletarFirmasServicioTest`; `DocumentoFirmableIntegrationTest`; `FirmasPendientes.test.tsx` y `e2e/firmas-pendientes.spec.ts` |
| Contrato publicado y verificado | `VIGENCIA`, `ORDEN` y `FIRMANTE` en `contratos/firma/openapi.yaml`, siete comprobaciones y el 409, con su prueba de contrato |
| Prueba de arquitectura | ArchUnit y Spring Modulith en verde; Aprobación depende de Firma, nunca al revés |
| Errores en el catálogo (FR-015) | `FIRMA_CERTIFICADO_NO_VIGENTE`, `FIRMA_FUERA_DE_ORDEN`, `FIRMA_FIRMANTE_EQUIVOCADO` y `FIRMA_PASO_SIN_NOMBRE` en `contratos/comun/errores.md` y `ErroresFirma`, verificados por sus pruebas |
| JavaDoc | La normalización, por qué «orden» va antes que «firmante», el evento, su idempotencia y el nombre local (T020) |
| Seguridad (FR-016) | Las siete comprobaciones, en el servidor; la SPA solo las muestra. El CN se busca por su tipo; el número de serie no se usa (D-23). El nombre real del autor, solo en su `.env`. La CA de prueba, ficticia y sin llave guardada. `validacion_firma` y `firmante_esperado`, sin reescrituras |
| Decisión registrada | D-104, y la huella nueva de la CA de prueba en D-103, antes de implementar |
