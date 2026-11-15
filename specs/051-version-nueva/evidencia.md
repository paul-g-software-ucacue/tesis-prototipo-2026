# Evidencia: Crear una versión nueva y anular las firmas tras un rechazo en firma

F12-H04 (#59). Cada sección dice qué se verificó, cómo y con qué resultado. Las comprobaciones son del 2026-11-15, sobre la rama `051-version-nueva`, antes del pull request.

## Pruebas y comprobaciones (T022 a T025)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 3 206 pruebas, 0 fallas | `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --continue` |
| `integrationTest` completo | 329 pruebas, 0 fallas. Generación: V8 (`GeneracionEsquemaIntegrationTest`) y la copia de la versión, tabla por tabla, con la anulada intacta (`VersionNuevaIntegrationTest`). Firma: V7 con el rol del proceso (+3) y la anulación, también a la vez que una subida (`RepositorioDocumentosIntegrationTest`, +3). Validación OCS: V6 y la transición `VERSION_NUEVA` guardada y leída. `apps:distributivo`: la orden real de anular llega a Firma después de confirmar, y la versión 2 empieza su cadena en F1 | El mismo |
| Fallas en la corrida completa | 3, ninguna de esta historia: `ClienteDeEventosLocalTest` y `ConfirmarLasDiezCargasIntegrationTest`, las intermitentes de siempre, que pasan solas y con sus módulos completos; y `spotlessJavaCheck` de Validación OCS, por el formato del JavaDoc de T025, que pasa después de `spotlessApply` | Cada una otra vez |
| Cobertura por módulo (60 % de líneas, D-32) | Pasa en todos: Generación 90,1 %, Firma 80,7 %, Validación OCS 83,2 %; la más baja, Roles, 77,2 % | `jacocoTestReport` |
| Spotless, SpotBugs y arquitectura | En verde: ArchUnit y Spring Modulith. Aprobación solo usa los tipos de `puertos/entrada` de Generación y de Firma (`MotivoDeVersion`, `MotivoDeAnulacion`); la dependencia sigue de Aprobación hacia los dos (MD-04, MD-05) | `build`, `ArquitecturaTest`, `ModulosTest` |
| El flujo | `ProcesoAprobacionTest` (+8): la versión nueva después del rechazo, repetida, sin rechazo, con la revisión desactivada, aprobarla, `rechazoVigente`, un segundo rechazo (v2 → v3) y `admiteRechazoEnFirma` | `./gradlew :servicios:validacionocs:test` |
| La orden de la versión nueva | `RegistrarRechazoEnFirmaServicioTest` (5): fuera de firma no llama a Generación; Generación en su transacción y las dos transiciones con el aviso en la de Aprobación; Generación sin versión; otro aviso ganó; sin proceso | El mismo |
| La versión en Generación | `ParcialesDelDistributivoServicioTest` (+5): crear, idempotente, segundo rechazo, otra situación y sin distributivo | `./gradlew :servicios:generacion:test` |
| La anulación en Firma | `FirmasDelDocumentoTest` (+3), `AnularFirmasServicioTest` (3) y `AnulacionDeLasFirmasTest` (1) | `./gradlew :servicios:firma:test` |
| Contratos | `anulacion` en el estado del documento (`DocumentoContratoTest`); `versionAnulada`, `versionNueva` y `VERSION_NUEVA` en el flujo (`FlujoContratoTest`, +2); `verificar:ruptura` | Las mismas y `npm run verificar:ruptura` |
| SPA: tipos, ESLint y Prettier | En verde | En `node:24-alpine` |
| SPA: Vitest | 583 pruebas, 0 fallas: el aviso con la versión anulada y la nueva, la constancia solo de la versión vigente, y la tarjeta del documento que espera el de la versión congelada. Las dos pruebas de la rama «sigue congelada» de F14-H04 se quitaron con ella | `vitest run` |
| SPA: Playwright | Los archivos de las tres pantallas tocadas, 14/14, con axe-core (WCAG 2.1 AA) sin fallas | `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Scripts de `perfil-consolidado` (T023, SC-005) | Los 36 del workflow pasan, en su orden, sobre un perfil construido desde la rama; `probar_rechazo_en_firma.sh` en 51 s, `probar_documento_firmable.sh` en 72 s y `probar_aprobacion.sh` en 42 s | Uno tras otro, como el workflow |
| shellcheck | 0 hallazgos en `probar_rechazo_en_firma.sh` | `koalaman/shellcheck:v0.11.0` |

### `probar_rechazo_en_firma.sh` (contracts/demostracion.md)

10/10 en 55 s, sobre un perfil construido desde la rama; dentro del workflow, otra vez 10/10. La primera corrida falló en el paso 6, por una condición del propio script (abajo).

| Paso | Resultado |
|---|---|
| 0 a 5 | OK, como en F14-H04 |
| 6. La versión nueva (SC-001, SC-002) | OK: EN_REVISION_VRA unos 0 s y las firmas anuladas unos 1 s después de rechazar (límite 30 s). Las transiciones `RECHAZO_EN_FIRMA` (v1) y `VERSION_NUEVA` (v2) por `sistema`; la v1 ANULADA y la v2 EDITABLE con el motivo; las seis tablas de la versión con las mismas filas en las dos; la anulación de Firma con 1 firma; el flujo con la versión anulada, la nueva y APROBAR_CONTENIDO |
| 7. El documento v1 cerrado | OK: subir y rechazar otra vez, 403; nadie tiene pendientes |
| 8. Aprobar la versión 2 (SC-004) | OK: CONGELADO_EN_FIRMA con la v2, su documento generado, y `docente.demo` con F1 de la v2, sin la v1 |
| 9. Lo guardado | OK: F1 intacta; el rechazo, la versión nueva y la anulación en la base y en la bitácora, por `sistema` |

## Recorrido a mano (T023)

Hecho por el autor el 2026-11-15, con `quickstart.md` §3, sobre un perfil construido desde la rama, después de `MDD_CONSERVAR=1 deploy/probar_rechazo_en_firma.sh`.

| Comprobación | Resultado |
|---|---|
| El rechazo | `vicerrectorado.demo` rechazó en F2 desde «Pendientes de firma» |
| La versión nueva | En «Consolidado del periodo»: «Revisión del Vicerrectorado · versión 2», el aviso «Rechazado en firma» con la observación y «La versión 1 quedó anulada con sus firmas; el distributivo sigue con la versión 2.», y «Aprobar el contenido» |
| Aprobar la versión 2 | «En firma · versión 2», con «Versión 2 congelada…», sin el aviso del rechazo |
| La cadena nueva | `docente.demo`: en «Pendientes de firma», solo la versión 2, paso «Elaborado por» |

**Hallazgos del recorrido, corregidos en la rama:**
1. **La tarjeta del documento mostraba el PDF de la versión 1 después de aprobar la 2.** La pantalla consultó antes de que Firma empezara a generar el de la versión 2: Firma respondió con el último documento que tenía, el de la 1, ya generado, y la pantalla dejó de consultar. En la base, el documento de la versión 2 se generó a las 16:27:29.

   Ahora `DocumentoFirmable` recibe la versión congelada. Mientras el documento sea de otra versión, dice «Generando el documento firmable…» y sigue consultando.
2. **Después del rechazo seguía visible la constancia «Versión 1 congelada…»,** con la versión 2 vigente. Ahora la constancia solo se muestra si su versión es la vigente.

Las dos correcciones las comprueban Vitest y Playwright; el autor no repitió el recorrido después de corregir.

El autor preguntó por la columna «Paso» de «Pendientes de firma». Muestra el recuadro del PDF en que firma quien consulta («Elaborado por»), no quién elaboró el documento. Decidió dejarla como está.

## Desviaciones del plan

| Plan | Lo hecho | Por qué |
|---|---|---|
| `AvisoDeFirma.avisar(FirmasPorAnular)` | Un puerto propio, `AvisoDeAnulacion`, funcional; `AvisoDeFirmaSpring` implementa los dos | Un segundo método le quitaba el `@FunctionalInterface` a `AvisoDeFirma` y rompía las lambdas de cuatro pruebas de integración y de `FlujoEnMemoria` |
| `FirmasPorAnular` con el motivo | Periodo, versión y momento | El único motivo que Aprobación ordena hoy es el rechazo en firma; lo pone el oyente |
| El paso 1 de research §1 | `ProcesoAprobacion.admiteRechazoEnFirma(version)`, nuevo | Leer el proceso sin bloquearlo exige preguntarle al agregado; `rechazarEnFirma` usa la misma regla |
| `MotivoDeVersion` en el dominio de Generación | En `puertos/entrada` | La usa Aprobación (principio V; análisis C2) |
| `AnulacionDeFirmas` con el enum del motivo | Con su nombre como texto | El enum es de la API publicada de Firma, y el dominio no depende de los puertos |
| La prueba en el proceso (T017) cubre la vuelta y la versión nueva | Cubre la orden de anular y la versión 2 desde F1 | La vuelta y la versión nueva necesitan un proceso de aprobación y un distributivo reales. Las cubren `RegistrarRechazoEnFirmaServicioTest`, `VersionNuevaIntegrationTest` y el script |
| Quitar la rama «sigue congelada» (T020) | Se quitó el componente entero | Sin la rama solo era el aviso de F13-H01: las dos pantallas vuelven a como estaban antes de F14-H04 |
| El paso 6 del script exige filas en cada tabla | Igualdad entre versiones; filas en todas menos las actividades | La preparación del script no asigna actividades no lectivas |
| Ver fallar cada prueba antes del código | Así en la mayoría. Pasaron al escribirse o contra el esqueleto:<br>• la transición `VERSION_NUEVA` en el repositorio (T012), porque guarda cualquier tipo;<br>• `DocumentoFirmableIntegrationTest` (T017), escrita con T013 a T016 hechos;<br>• los casos de «no hace nada» de T007, T010, T011, T014 y T018, contra esqueletos que no hacían nada;<br>• la de valores inválidos de V7 (T004), que pasaba sin la tabla | Se cuenta tal como pasó |

**Limitación conocida.** Las dos fallas intermitentes de la corrida completa son de módulos que esta rama no toca y dependen de la carga del equipo.

## Criterios de terminado (constitución §6)

| Criterio | Dónde |
|---|---|
| Escenarios de aceptación | `probar_rechazo_en_firma.sh` (pasos 6 a 9).<br>Backend: `ProcesoAprobacionTest`, `RegistrarRechazoEnFirmaServicioTest`, `ParcialesDelDistributivoServicioTest`, `VersionNuevaIntegrationTest`, `GeneracionEsquemaIntegrationTest`, `FirmasDelDocumentoTest`, `AnularFirmasServicioTest`, `RepositorioDocumentosIntegrationTest`, `DocumentoFirmableEsquemaIntegrationTest`, `AnulacionDeLasFirmasTest`, `RepositorioProcesoIntegrationTest`, `ValidacionocsEsquemaIntegrationTest`, `FlujoContratoTest`, `DocumentoContratoTest` y `DocumentoFirmableIntegrationTest`.<br>SPA: `EstadoDelFlujo.test.tsx` y `DocumentoFirmable.test.tsx` |
| Contrato publicado y verificado | `AnulacionDeFirmas` en `contratos/firma/openapi.yaml`; `versionAnulada`, `versionNueva` y `VERSION_NUEVA` en `contratos/validacionocs/openapi.yaml`; con sus pruebas de contrato y `verificar:ruptura` |
| Prueba de arquitectura | ArchUnit y Spring Modulith en verde; Aprobación depende de Generación y de Firma, nunca al revés |
| Errores en el catálogo | Ninguno nuevo (research §7): crear la versión y anular las firmas no son operaciones de la API |
| JavaDoc | Los tres pasos y su reintento, la copia y su idempotencia, la anulación como fila del documento (principio VII) y `rechazoVigente` frente a `rechazoPendiente` (T025) |
| Seguridad | Ninguna operación nueva expuesta: las órdenes son internas (FR-017). La versión anulada no cambia de contenido; `anulacion_de_firmas` sin reescrituras; las firmas y sus archivos se conservan |
| Decisión registrada | D-106, antes de implementar; el motivo «rechazo en firma» en el glosario (análisis C1) |

## Equipo de demostración (T027)

| Comprobación | Resultado |
|---|---|
| Fusión | PR #237 (`8c802b3`), con las 12 comprobaciones en verde en la primera corrida |
| Despliegue | `cd` exitoso; `mdd-actualizar` tomó `8c802b3` y dice «Sano en 56 s»; los 8 contenedores sanos con las imágenes `*-8c802b36db43` |
| Las migraciones | En la base, `generacion` en la V8 (`version nueva`), `firma` en la V7 (`anulacion de firmas`) y `validacionocs` en la V6 (`version nueva`) |
| Nada anulado | `firma.anulacion_de_firmas` vacía; ninguna versión ANULADA ni con motivo: 2026-I sigue en EN_CONSOLIDACION con su versión 1 |
| Memoria | `distributivo` usa 245 de sus 512 MiB; `identidad`, 475 (#224, como después de F14-H04) |

En la demo no se llega a la firma: 2026-I está en consolidación. El rechazo con la versión nueva lo cubren el script en la CI y el recorrido del autor en el perfil local.
