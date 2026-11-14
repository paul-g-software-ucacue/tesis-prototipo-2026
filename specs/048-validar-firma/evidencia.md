# Evidencia: Validar que el PDF subido es el mismo documento con una firma íntegra

F14-H02 (#63). Cada sección dice qué se verificó, cómo y con qué resultado. Las comprobaciones son del 2026-11-14, sobre la rama `048-validar-firma`, antes del pull request.

## Pruebas y comprobaciones (T019 a T021)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 3 110 pruebas; Firma 161 (162 con la del CN, agregada después del recorrido del autor) | `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --continue` |
| `integrationTest` completo | 306 pruebas. Firma 21 (la V4 con el rol del proceso, el índice único y dos subidas a la vez); `apps:distributivo` con `DocumentoFirmableIntegrationTest` (3), que sube F1, F2 y F3 sobre el PDF/A real | El mismo comando |
| Fallas en la corrida completa | 2, las mismas de F13-H02 y F14-H01, en módulos que esta rama no toca, y las dos pasan corridas solas: `ClienteDeEventosLocalTest` (5/5) y la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest` (9/9) | Cada una con `--rerun` |
| Fallas propias de la primera corrida | 2, corregidas: `BitacoraSinBorradoContratoTest` (la descripción de `subirFirma` nombraba la bitácora) y SpotBugs en los test fixtures (la contraseña ficticia de la CA de prueba) | La segunda corrida, en verde salvo las 2 de arriba |
| Cobertura por módulo (60 % de líneas, D-32) | Pasa en todos: Firma 82,3 %, Usuarios 84,8 %; la más baja, Roles, 77,2 % | `jacocoTestReport` |
| Spotless y SpotBugs | En verde | `build` |
| Arquitectura | ArchUnit y Spring Modulith en verde. Regla nueva: PDFBox y Bouncy Castle solo en los adaptadores de Firma, nunca en el dominio, los puertos ni la aplicación | `ArquitecturaTest`, `ModulosTest` |
| La validación con firmas como las de FirmaEC | `ValidadorPadesTest` (10) sobre el PDF/A real de F13-H02: en el campo del paso y en uno nuevo, sin firma nueva, dos firmas nuevas, alterado, CA ajena, PDF dañado, las tres sucesivas (también válidas para el validador del spike), un `.p12` real de la CA de prueba y el CN en un componente de varios valores | `./gradlew :servicios:firma:test` |
| La lista de confianza | `ListaConfianzaTest` (8): las dos raíces y sus huellas de D-103, sin raíces o ilegible no arranca, una llave privada se rechaza, la carpeta solo tiene certificados, y la propiedad reemplaza la lista | El mismo |
| SPA: tipos, ESLint y Prettier | En verde | En `node:24-alpine` |
| SPA: Vitest | 574 pruebas en 42 archivos, 0 fallas; `FirmasPendientes.test.tsx`, 12 | `vitest run` |
| SPA: Playwright (SC-006) | 128 pruebas, 0 fallas; `e2e/firmas-pendientes.spec.ts`, 5, con axe-core (WCAG 2.1 AA) sin fallas en la subida aceptada y en la rechazada | `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Scripts de `perfil-consolidado` (T020, SC-007) | Los 35 del workflow pasan, en su orden, sobre un perfil construido desde la rama; `probar_documento_firmable.sh` 13/13 en 98 s | Uno tras otro, como el workflow |
| shellcheck | 0 hallazgos en `probar_documento_firmable.sh` y `probar_aprobacion.sh` | `koalaman/shellcheck:v0.11.0` |
| Gitleaks | Sin fugas en los 33 commits de la rama, con la excepción nueva solo para los `.crt` de `firma/raices/`. Comprobado aparte: un `.pem` en esa carpeta y un `.crt` fuera de ella siguen rechazados, por Gitleaks y por el hook de pre-commit | v8.30.1, `main..HEAD` |

### `probar_documento_firmable.sh` (contracts/demostracion.md)

Corrido solo el 2026-11-14 sobre un perfil construido desde la rama: 13/13 en la primera corrida.

| Paso | Resultado |
|---|---|
| 0 a 8 | OK, como en F14-H01 |
| 9. Los rechazos (SC-002, SC-004) | OK: `docente.demo` sube otro documento (`FIRMA_NO_ES_LA_VERSION_VIGENTE`), el vigente sin firmar y uno con dos firmas nuevas (`FIRMA_SIN_UNA_FIRMA_NUEVA`), uno alterado después de firmar (`FIRMA_NO_INTEGRA`) y uno de una CA ajena (`FIRMA_SIN_CONFIANZA`). Cada 422 lleva las cuatro comprobaciones en orden. Después, el turno y el SHA-256 del vigente siguen iguales |
| 10. Fuera de turno (FR-002) | OK: `vicerrectorado.demo` y `consolidacion.demo` suben un PDF bien firmado y reciben 403, sin dejar fila |
| 11. Las tres firmas (SC-005) | OK: F1, F2 y F3 se aceptan en orden con los certificados ficticios de la CA de prueba, cada una sobre el vigente que deja la anterior; después de cada una, el siguiente está en turno y el anterior no; al final nadie |
| 12. La base y la bitácora (SC-003) | OK: `validacion_firma` con 3 aceptadas y 5 rechazadas, los 4 archivos en el volumen, y 3 `FIRMA_ACEPTADA` y 5 `FIRMA_RECHAZADA` del servicio `firma` |

Firma con `deploy/herramientas/FirmarDePrueba.java`, en `eclipse-temurin:25-jdk` con las bibliotecas del contenedor `distributivo`. La limpieza deja `validacion_firma`, `firmante_esperado` y `documento_firmable` en 0.

## Concurrencia (FR-014)

- **Dos subidas aceptadas a la vez para el mismo paso** (`RepositorioDocumentosIntegrationTest`, PostgreSQL real): la segunda espera el bloqueo de la generación (`FOR UPDATE`), ve las firmas cambiadas y no registra. Queda una sola firma.
- **El índice único parcial** de V4 rechaza una segunda aceptada del mismo paso aunque se salte el bloqueo (`DocumentoFirmableEsquemaIntegrationTest`).
- **La subida que llega tarde** se registra como rechazada por «mismo documento» (`SubirFirmaServicioTest`).

## Comprobación a mano con FirmaEC (T020, SC-001, SC-005)

Hecha por el autor el 2026-11-14, con `quickstart.md` §3, sobre un perfil construido desde la rama, con FirmaEC y su certificado real de la ECIBCE. Ni el certificado ni los PDF firmados con él entraron al repositorio; aquí no se anota ningún dato del certificado.

| Comprobación | Resultado |
|---|---|
| F1 (SC-001) | `docente.demo` descargó, firmó con FirmaEC, subió y vio «Firma registrada» con las cuatro comprobaciones aprobadas en **40 s** desde que abrió «Pendientes de firma» |
| La confianza | Llegó a la raíz del BCE de la lista con la intermedia «AC BANCO CENTRAL DEL ECUADOR» que FirmaEC incluye en el CMS |
| Los rechazos | Como `vicerrectorado.demo`: el PDF original, «No es la versión vigente»; el PDF con F1 sin firmar otra vez, «Firma nueva no encontrada». Cada aviso con las cuatro comprobaciones, y la fila siguió |
| F2 y F3 | `vicerrectorado.demo` y `rectorado.demo` firmaron con FirmaEC y se aceptaron; al final, «No tienes firmas pendientes.» para los tres |
| El PDF final (SC-005) | Con las tres firmas visibles en los recuadros de «Elaborado por», «Revisado por» y «Aprobado por» |
| La base y la bitácora | 3 aceptadas y 4 rechazadas: dos de `vicerrectorado.demo` por no ser la versión vigente, una de `vicerrectorado.demo` y otra de `rectorado.demo` por no tener firma nueva (la de `rectorado.demo`, 3 s antes de su firma aceptada). La bitácora, con las mismas 3 `FIRMA_ACEPTADA` y 4 `FIRMA_RECHAZADA` |

**Hallazgos del recorrido** (research §7):
1. **FirmaEC no llena los campos vacíos del PDF/A.** Agrega los suyos (`Signature1` a `Signature3`) con `adbe.pkcs7.detached` y la cadena completa, y pone el sello donde el firmante lo ubica. Los campos `firma-<orden>-<rol>` quedan vacíos. La validación, que cuenta firmas y no campos, las aceptó. Para F14-H03: el paso no se reconoce por el nombre del campo.
2. **Defecto, corregido.** El aviso mostró un número como «Certificado de …». En los certificados de la ECIBCE, el CN y el `SERIALNUMBER` van en el mismo componente del sujeto, y el código del spike tomaba el primer valor. Ahora se busca el CN por su tipo (`ValidadorPades` y el validador del spike), con una prueba que lo reproduce y se vio fallar. Importa para F14-H03, que compara el CN con el nombre del firmante (D-23).

## Desviaciones del plan

| Plan | Lo hecho | Por qué |
|---|---|---|
| `InformeDeValidacion`, `ListaDeConfianza`, `FirmaRechazada` | `ResultadoValidacion`, `ListaConfianza`, `ArchivoFirmadoRechazado` | El glosario (§7.5) ya tenía esos términos; el principio III no admite sinónimos (T001) |
| «Mismo documento»: empieza con el vigente y es más largo | Basta con que empiece con el vigente | Un archivo igual al vigente lo rechaza «una sola firma nueva», que dice mejor qué falta; así lo esperaba el script (T006) |
| Las raíces en `firma/raices/` | Con una excepción nueva de Gitleaks y del hook, solo para `.crt`, y la constitución 2.2.1 | Gitleaks y el principio IX rechazaban cualquier `.crt` fuera de la CA de prueba (análisis, C1) |
| La raíz del BCE «del sitio oficial» | Sacada de la cadena del `.p12` del autor, con `openssl pkcs12 -cacerts -nokeys -legacy`; solo la raíz, autofirmada, `CA:TRUE` y sin llave | El autor no tenía el archivo del spike. Su huella está en D-103 y en una prueba |
| `FirmaRechazada` con un código | Una clase sellada con una subclase por comprobación | El catálogo relaciona una clase con un código (T004) |
| `FirmadorDePrueba` del spike, ampliado | Un test fixture nuevo de Firma, `FirmasDePrueba`, que firma como FirmaEC | Lo usan también las pruebas de `apps:distributivo` (T008, T012) |
| La regla del turno, compartida | `FirmanteEnTurno`, en la aplicación | Para no copiarla entre la lista, la descarga y la subida (análisis, U2) |
| El turno y el tamaño, en cualquier orden | Primero el turno | A quien no está en turno, un archivo vacío también le da 403, sin decir nada del documento |
| SpotBugs en todo el código | No revisa los test fixtures, como no revisaba las demás pruebas | La contraseña ficticia de la CA de prueba (T021) |
| Spotless sobre la herramienta | No la cubre: su objetivo es `src/*/java` de cada módulo | Escrita con el mismo estilo (T018) |
| Ver fallar cada prueba antes del código | Así en el dominio, la aplicación, el validador, la lista, el repositorio, los contratos, Vitest y el CN. Pasaron al escribirse: `PublicadorDeCambiosTest` con las acciones nuevas (el publicador ya era genérico), `DocumentoFirmableIntegrationTest` de las tres firmas y las dos pruebas nuevas de Playwright, escritas con el código ya hecho | Se cuenta tal como pasó |

**Limitación conocida.** Las dos fallas de la corrida completa (`ClienteDeEventosLocalTest` y la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest`) son de módulos que esta rama no toca y pasan solas: dependen de la carga del equipo.

## Criterios de terminado (constitución §6)

| Criterio | Dónde |
|---|---|
| Escenarios de aceptación | `probar_documento_firmable.sh` (pasos 9 a 12); `ResultadoValidacionTest`, `ComprobacionDeDocumentoTest`, `ArchivoSubidoTest`, `FirmasDelDocumentoTest`, `ValidadorPadesTest`, `ListaConfianzaTest`, `SubirFirmaServicioTest`, `FirmasPendientesServicioTest`, `SubirFirmaContratoTest`, `PermisosFirmaTest`, `ErroresFirmaTest`, `PublicadorDeCambiosTest`, `RepositorioDocumentosIntegrationTest`, `DocumentoFirmableEsquemaIntegrationTest`; `DocumentoFirmableIntegrationTest`; `FirmasPendientes.test.tsx` y `e2e/firmas-pendientes.spec.ts` |
| Contrato publicado y verificado | `subirFirma`, `ResultadoValidacion`, `ResultadoDeComprobacion` y `CertificadoDeLaFirma` en `contratos/firma/openapi.yaml`, con su prueba de contrato |
| Prueba de arquitectura | ArchUnit (con la regla de las bibliotecas de firma) y Spring Modulith en verde |
| Errores en el catálogo (FR-023) | `FIRMA_NO_ES_LA_VERSION_VIGENTE`, `FIRMA_SIN_UNA_FIRMA_NUEVA`, `FIRMA_NO_INTEGRA`, `FIRMA_SIN_CONFIANZA`, `FIRMA_ARCHIVO_VACIO` y `FIRMA_ARCHIVO_DEMASIADO_GRANDE` en `contratos/comun/errores.md` y `ErroresFirma`, verificados por sus pruebas |
| JavaDoc | El orden de las comprobaciones y «no evaluada», la fecha de la cadena, el relleno de ceros y la llave pública (D-46), el bloqueo y la nueva comprobación, la lista de confianza sin interruptores, la firma como validación aceptada y el CN por su tipo (T022) |
| Seguridad (FR-024) | El permiso, el turno y las cuatro comprobaciones, en el servidor; la SPA solo refleja. 403 común fuera de turno, antes de mirar el archivo. Textos fijos en el resultado, nunca mensajes de las bibliotecas. Límite de 10 MB. `validacion_firma` solo se inserta; los archivos nunca se sobrescriben. Ningún dato del certificado real en el repositorio. SQL parametrizado; SpotBugs y Gitleaks en verde |
| Decisión registrada | D-103 y la constitución 2.2.1, antes de implementar |

## Equipo de demostración (T024)

**Pendiente**, después de la fusión: `quickstart.md` §4, solo por lectura.
