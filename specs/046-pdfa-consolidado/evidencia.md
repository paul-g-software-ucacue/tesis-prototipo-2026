# Evidencia: Generar el PDF/A del consolidado con su hash

F13-H02 (#61). Cada sección dice qué se verificó, cómo y con qué resultado. Las comprobaciones son del 2026-11-12 al 2026-11-14, sobre la rama `046-pdfa-consolidado`, antes del pull request.

## Pruebas y comprobaciones (T025)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 3 002 pruebas. Firma 65, Validación OCS 349, Generación 811 | `./gradlew build integrationTest jacocoTestCoverageVerification --rerun-tasks` |
| `integrationTest` completo | 290 pruebas. Firma 8, Validación OCS 24, Generación 53, `apps:distributivo` 6 (con `DocumentoFirmableIntegrationTest`, 2) | El mismo comando |
| Fallas en la corrida completa | 2, en módulos que esta rama no toca, y las dos pasan corridas solas: `ClienteDeEventosLocalTest` de `plataforma:eventos` (3 de 3) y la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest` de `apps:datos` (9/9), con todos los módulos a la vez | Cada una con `--rerun` |
| Cobertura por módulo (60 % de líneas, D-32) | Pasa en todos: Firma 86,5 %, Validación OCS 83,8 %, Generación 90,7 %, Roles 77,2 %, Usuarios 84,3 %, Académico 84,7 % | `jacocoTestReport` (el XML no lo regenera `build`) |
| Spotless y SpotBugs | En verde | `build` |
| Arquitectura | ArchUnit y Spring Modulith (`apps:*:test`) en verde: Validación OCS llega a Firma solo por `PrepararDocumentoFirmable`; Firma lee Generación por `ContenidoCongelado` y no depende de Validación OCS (MD-05); Roles pide el nombre a Usuarios por `NombreDeUsuario` | `build` |
| `BitacoraSinBorradoContratoTest` | En verde, con `firma` en la enumeración de servicios | `./gradlew :servicios:usuarios:test --rerun` |
| PDF/A-2b en las pruebas (FR-002, SC-001) | `GeneradorPdfaTest` (10) con veraPDF: un contenido pequeño y uno de 165 paralelos, conformes con los tres campos de firma; el de 165 se genera y verifica en 1,6 s | `./gradlew :servicios:firma:test` |
| SPA: tipos, ESLint y Prettier | En verde | En `node:24-alpine` |
| SPA: Vitest | 562 pruebas en 41 archivos, 0 fallas. Con el build de Gradle corriendo a la vez, 3 fallaron por tiempo; solas pasan | `vitest run` |
| SPA: Playwright | 123 pruebas, 0 fallas, con el documento en la aprobación y el reintento del Responsable, y axe-core (WCAG 2.1 AA) sin fallas en los tres estados | `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Scripts de `perfil-consolidado` (T024, SC-007) | Los 35 del workflow pasan, en su orden, sobre un perfil construido desde la rama y con la base vacía; `probar_documento_firmable.sh` 6/6 en 44 s | Uno tras otro, como el workflow |
| shellcheck | 0 hallazgos en `probar_documento_firmable.sh` y en los 5 scripts cambiados | `koalaman/shellcheck:v0.11.0` |
| Gitleaks | Sin fugas en los 19 commits de la rama | v8.30.1, `main..HEAD` |

### `probar_documento_firmable.sh` (contracts/demostracion.md)

Corrido solo el 2026-11-14 sobre un perfil recién levantado: 6/6 en 91 s, con el arranque de veraPDF. En el orden del workflow, el 2026-11-12, tardó 44 s.

| Paso | Resultado |
|---|---|
| 0. Base sin datos | OK: sin estructura, distributivos, procesos de aprobación, documentos ni designación |
| 1. Aprobado y congelado | OK: 0 paralelos sin propuesta, 0 duras y 92 blandas justificadas; EN_REVISION_VRA; designado `docente.demo`; `vicerrectorado.demo` aprueba y queda CONGELADO_EN_FIRMA |
| 2. Generado solo (FR-001, SC-003) | OK: GENERADO **unos 2 s después de aprobar** (1 s en la corrida del 2026-11-12), sin que nadie lo pida, por `sistema`, con el hash de contenido de la congelación y sin acciones |
| 3. El archivo por su hash (FR-009, SC-004) | OK: el registro dice `documentos/<sha256>.pdf`; el archivo copiado del volumen tiene ese SHA-256 |
| 4. Conforme (FR-002, FR-007, SC-001) | OK: veraPDF (`verapdf/cli:v1.30.2`, perfil 2b) da `isCompliant="true"`; los campos son `firma-1-RESPONSABLE_CONSOLIDACION`, `firma-2-VICERRECTOR` y `firma-3-RECTOR`, sin firma |
| 5. Idempotente (FR-005, FR-012) | OK: el POST de `consolidacion.demo` devuelve el mismo SHA-256, con una sola fila y los mismos archivos; `coordinacion.demo` recibe 403 |
| 6. La bitácora (FR-010) | OK: una sola `DOCUMENTO_FIRMABLE_GENERADO` del servicio `firma`, usuario `sistema`, con la versión, el hash de contenido y el SHA-256 |

`MDD_CONSERVAR=1` deja 2026-I en EN_REVISION_VRA, con `docente.demo` designado, sin limpiar. Se negó con `MDD_GATEWAY=https://127.0.0.1` («solo se usa con el perfil local») y funcionó contra `http://localhost:8080`.

## Concurrencia (SC-005)

- **Dos generaciones a la vez** (`RepositorioDocumentosIntegrationTest`, PostgreSQL real): la segunda espera el bloqueo de la generación (`FOR UPDATE`), la ve GENERADA y no registra. Queda un documento.
- **La aprobación no espera al PDF** (`DocumentoFirmableIntegrationTest`, el proceso distributivo con PostgreSQL real): con firmantes que tardan 1 s, la transacción del paso termina antes, y después de confirmar queda el documento GENERADO, su archivo y su entrada pendiente para la bitácora. Si el paso se revierte, no se genera nada.
- **Pedirlo otra vez** (paso 5 del script): el mismo documento, sin otro archivo, fila ni entrada.

## Comprobación a mano en el perfil local (T024, SC-006)

Hecha por el autor el 2026-11-14, con `quickstart.md` §3:

| Comprobación | Resultado |
|---|---|
| Camino normal (FR-001, FR-014) | Al aprobar como `vicerrectorado.demo`, la tarjeta pasó a «Documento firmable generado el 14/11/26, 03:43», en el mismo minuto que «Versión 1 congelada…», con «Huella del PDF: 8b4e6d5c59d6…» y «Copiar la huella» |
| El PDF (FR-003, FR-004, FR-006, FR-007) | 11 páginas; su SHA-256 es el de la tarjeta. El autor revisó el resumen por carrera, los totales, el detalle por docente, el pie con la versión y el hash de contenido en cada página, y el bloque «Elaborado por», «Revisado por» y «Aprobado por» con cargo, nombre y campo de firma, y los dio por buenos |
| El fallo (FR-008, FR-011, FR-014) | Sin designación, al aprobar la tarjeta dijo «El PDF no se pudo generar» con «Falta el nombre del Responsable de consolidación.»; la versión 1 quedó congelada igual |
| El reintento (FR-012, FR-015, SC-006) | El Vicerrector designó otra vez a `docente.demo`; `consolidacion.demo` pulsó «Generar el PDF» y lo tuvo en **40 s** desde que abrió la pantalla. En la base, un solo documento, generado por `consolidacion.demo`; en la bitácora, `CONTENIDO_APROBADO` (03:50:42) y una sola `DOCUMENTO_FIRMABLE_GENERADO` (03:53:47) de `consolidacion.demo` |

El PDF todavía no se descarga por la pantalla (F14-H01): se sacó del volumen con `docker compose cp`.

## Desviaciones del plan

| Plan | Lo hecho | Por qué |
|---|---|---|
| Firma escucha y consulta a Validación OCS | Validación OCS guarda `DistributivoEnFirma` en la transacción del paso y, después de confirmar, su oyente le entrega la cadena a Firma por `PrepararDocumentoFirmable` | MD-05: Aprobación entrega la cadena y Firma no conoce el flujo, para que F14 no cree un ciclo. Se corrigió antes de implementar (T001; research §4, data-model, contratos y tareas) |
| Generación arma el contenido con sus datos | Ruta interna `/interno/academico/carreras` y puerto `DatosParaElDocumento` en Generación | Generación no conoce los nombres de las carreras: la oferta trae solo el código (T008) |
| `firma` en el `check` de `bitacora.servicio` | También en `EntradaBitacora`, `FiltroDeBitacora` y `RegistrarEventoRecibidoServicio`, con sus pruebas | Usuarios tenía la lista de servicios en tres lugares del código (T006) |
| `ContenidoDeLaVersion` con las horas de Generación | Su propio registro `Horas` | `ModulosTest` encontró que exponía `CargaHoraria`, del dominio de Generación (T013) |
| El bloque de firmas en posiciones fijas | `body { margin: 0 }` y `box-sizing: border-box` | Las imágenes de la prueba mostraron los recuadros desplazados 2,8 mm por el margen por defecto y el borde (T014) |
| Los scripts que llegan a CONGELADO_EN_FIRMA, como estaban | `probar_documento_firmable.sh` designa a `docente.demo` (solo se designa a un DOCENTE) | El nombre del Responsable sale de la designación del periodo (research §5); sin ella, FALLIDA, como se diseñó (T023). En la demostración, el Vicerrector designa al Responsable antes de aprobar |
| La limpieza borra `firma.generacion_documento` | Con `session_replication_role = replica`, después de los documentos | La verificación de la clave foránea toma `FOR KEY SHARE` como dueño de `documento_firmable`, al que V2 le quitó UPDATE. El proceso nunca borra generaciones (T023) |
| veraPDF sobre la carpeta temporal | Solo una subcarpeta con el PDF, de solo lectura | La imagen corre con el usuario 100 y la carpeta temporal es 700, con respuestas que llevan tokens (T023) |
| `quickstart.md` §3: designar a `consolidacion.demo` | Designar a `docente.demo`, y `MDD_CONSERVAR=1` también designa | El recorrido lo encontró: designar a quien no es DOCENTE da 400 |
| Ver fallar cada prueba antes del código | Así en el dominio, la aplicación, los adaptadores, los contratos y la SPA. Se escribieron junto con el código o después: la ruta de Académico y su prueba, `NombreParaRoles`, `nombres.ts` de la bitácora, la prueba del avance genérico hacia la firma y `DocumentoFirmableIntegrationTest` | Se cuenta tal como pasó |

**Limitación conocida.** Las dos fallas de la corrida completa (`ClienteDeEventosLocalTest` y la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest`) son de módulos que esta rama no toca y pasan solas: dependen de la carga del equipo.

## Criterios de terminado (constitución §6)

| Criterio | Dónde |
|---|---|
| Escenarios de aceptación | `probar_documento_firmable.sh`; `FlujoDelDistributivoServicioTest`, `EtapasAlCrearseTest`, `PreparacionDelDocumentoTest`; `GeneracionDeDocumentoTest`, `ContenidoDelDocumentoTest`, `DocumentoFirmableServicioTest`, `GeneradorPdfaTest`, `AlmacenDeDocumentosEnDiscoTest`, `FirmantesDesdeProcesosTest`, `ContenidoDesdeGeneracionTest`, `PublicadorDeCambiosTest`, `DocumentoContratoTest`, `PermisosFirmaTest`, `RepositorioDocumentosIntegrationTest`, `DocumentoFirmableEsquemaIntegrationTest`; `ContenidoCongeladoServicioTest`, `DatosDesdeProcesoDatosTest`; `ConsultarResponsableServicioTest`, `AutorizacionContratoTest`; `NombreParaRolesTest`; `DocumentoFirmableIntegrationTest`; `DocumentoFirmable.test.tsx`, `EstadoDelFlujo.test.tsx` y `e2e/consolidado.spec.ts` |
| Contrato publicado y verificado | `consultarDocumentoFirmable` y `generarDocumentoFirmable` en `contratos/firma/openapi.yaml`; `/interno/roles/periodos/{periodo}/responsable` en `contratos/roles/openapi.yaml`; `/interno/academico/carreras` en `contratos/academico/openapi.yaml`; `firma` en `contratos/usuarios/openapi.yaml`; con sus pruebas de contrato |
| Prueba de arquitectura | ArchUnit y Spring Modulith en verde |
| Errores en el catálogo (FR-016) | `FIRMA_ETAPA_NO_ADMITE_GENERACION`, `FIRMA_GENERACION_FALLIDA` (con el motivo en los detalles) y `ROLES_SIN_RESPONSABLE` en `contratos/comun/errores.md`, `ErroresFirma` y `ErroresRoles`, verificados por sus pruebas |
| JavaDoc | El evento interno y su oyente, `PrepararDocumentoFirmable`, `ContenidoCongelado`, `NombreDeUsuario`, los puertos y el dominio de Firma, el generador, el almacén (`CREATE_NEW`) y el oyente que no relanza (T026) |
| Seguridad | Los permisos (`CONSOLIDACION:LECTURA` o `DISTRIBUTIVO:ESCRITURA` para consultar, `CONSOLIDACION:APROBACION` para generar) y el rol de la etapa en el servidor; la SPA solo refleja. El rol del proceso solo inserta y lee `documento_firmable`; el archivo nunca se sobrescribe. SQL parametrizado; los textos del PDF escapados; SpotBugs y Gitleaks en verde |
| Decisión registrada | D-101, antes de implementar |

## Equipo de demostración (T028)

| Comprobación | Resultado |
|---|---|
| Imágenes | La primera corrida del cd del #221 falló en «5 · imágenes»: Grype encontró tres altas en `libxslt` 1.1.43-r3 de la base del frontend, ya corregidas en r4 y ajenas a esta rama. Se corrigieron aparte, en el #222 (`e45afaf`, con las 12 comprobaciones en verde), y se trajo `main` a la rama |
| `perfil-consolidado` | En la corrida después de traer `main`, el contenedor `identidad` se cayó sin dejar error en su registro, en el primer script (autenticación), y todo lo demás respondió 503. Relanzado, pasó los 47 pasos en 23 min. En la demo, `identidad` usa 478 de sus 512 MiB (allí, `restart: unless-stopped` lo vuelve a levantar si se cae; `compose.yaml`, el del perfil local y la CI, no tiene política de reinicio): queda en el #224 |
| Fusión | PR #221 (`bd2b503`), con las 12 comprobaciones en verde |
| Despliegue | `cd` exitoso; `mdd-actualizar` tomó `bd2b503` y dice «Sano en 51 s», con los 5 pasos de la demo en OK; los 8 contenedores sanos con las imágenes `*-bd2b503d0345`, sin reinicios |
| Nada cambió en la demo (`quickstart.md` §4, por lectura de la base) | 2026-I sigue en EN_CONSOLIDACION, sin transiciones hacia CONGELADO_EN_FIRMA, así que la tarjeta no muestra la sección del documento. Las tablas de Firma existen (migraciones 1 y 2), vacías: 0 generaciones y 0 documentos, y no hay carpeta `documentos/` en el volumen |

En la demo no se llega a la firma: 2026-I está en consolidación. El documento firmable lo cubren el script en la CI y la comprobación del autor en el perfil local.
