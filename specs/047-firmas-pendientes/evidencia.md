# Evidencia: Ver mis firmas pendientes y descargar el PDF vigente

F14-H01 (#62). Cada sección dice qué se verificó, cómo y con qué resultado. Las comprobaciones son del 2026-11-14, sobre la rama `047-firmas-pendientes`, antes del pull request.

## Pruebas y comprobaciones (T018, T019)

| Comprobación | Resultado | Cómo |
|---|---|---|
| Pruebas unitarias del backend | 3 048 pruebas. Firma 99, Usuarios 301 | `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification --rerun-tasks --continue` |
| `integrationTest` completo | 297 pruebas. Firma 13 (con `RepositorioDocumentosIntegrationTest` y `DocumentoFirmableEsquemaIntegrationTest`), Usuarios 37 (con `UsuariosEsquemaIntegrationTest` y `RepositorioUsuariosJpaIntegrationTest`), `apps:distributivo` 6 (`DocumentoFirmableIntegrationTest` comprueba los firmantes esperados `1:docente.demo`, `2:0109990028` y `3:0109990010`) | El mismo comando |
| Fallas en la corrida completa | 2, las mismas de F13-H02, en módulos que esta rama no toca, y las dos pasan corridas solas: `ClienteDeEventosLocalTest` de `plataforma:eventos` y la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest` de `apps:datos` | Cada una con `--rerun` |
| Cobertura por módulo (60 % de líneas, D-32) | Pasa en todos: Firma 85,7 %, Usuarios 84,8 %; la más baja, Roles, 77,2 % | `jacocoTestReport` |
| Spotless y SpotBugs | En verde | `build` |
| Arquitectura | ArchUnit y Spring Modulith (`apps:*:test`) en verde, sin dependencias nuevas entre módulos. Firma pide la identificación a Usuarios por la ruta interna `/interno/usuarios/{usuario}/identificacion`, con token de proceso: están en procesos distintos (principio V) | `build` |
| La identificación fuera del token (FR-013) | `EmisorJwtTest`: el token no la lleva. `UsuarioResumen` tampoco | `./gradlew :servicios:usuarios:test` |
| Los usuarios de la demo y los cargos (FR-012) | `CargaUsuariosDemoTest` compara la identificación de `vicerrectorado.demo` y `rectorado.demo` en `usuarios-demo.yaml` con `08_cargos_autoridades.csv`; una identificación repetida falla al arrancar, nombrando al usuario | El mismo |
| SPA: tipos, ESLint y Prettier | En verde | En `node:24-alpine` |
| SPA: Vitest | 569 pruebas en 42 archivos, 0 fallas; `FirmasPendientes.test.tsx`, 7 | `vitest run` |
| SPA: Playwright (SC-004) | 126 pruebas, 0 fallas; `e2e/firmas-pendientes.spec.ts`, 3, con axe-core (WCAG 2.1 AA) sin fallas con documentos y sin ellos | `mcr.microsoft.com/playwright:v1.64.0-noble` |
| Scripts de `perfil-consolidado` (T018, SC-005) | Los 35 del workflow pasan, en su orden, sobre un perfil construido desde la rama y con la base vacía; `probar_autenticacion.sh` igual, `probar_documento_firmable.sh` 8/8 en 44 s | Uno tras otro, como el workflow |
| shellcheck | 0 hallazgos en `probar_documento_firmable.sh` y `probar_aprobacion.sh` | `koalaman/shellcheck:v0.11.0` |
| Gitleaks | Sin fugas en los 14 commits de la rama | v8.30.1, `main..HEAD` |

### `probar_documento_firmable.sh` (contracts/demostracion.md)

Corrido solo el 2026-11-14 sobre un perfil recién levantado: 8/8 en 56 s. En el orden del workflow, 44 s.

| Paso | Resultado |
|---|---|
| 0. Base sin datos | OK: sin estructura, distributivos, procesos de aprobación, documentos, firmantes esperados ni designación |
| 1 a 6 | OK, como en F13-H02: aprobado y congelado, generado solo, el archivo por su hash, conforme con veraPDF, idempotente y una sola entrada en la bitácora |
| 7. El designado lo tiene pendiente (FR-002, FR-008, FR-009, SC-002) | OK: `docente.demo` ve una fila, 2026-I, versión 1, orden 1, «Elaborado por»; la descarga da `application/pdf`, `attachment; filename="distributivo-2026-I-v1.pdf"`, el `ETag` con el SHA-256 registrado, y el archivo tiene ese SHA-256 |
| 8. Nadie más (FR-007, FR-010, SC-003) | OK: `consolidacion.demo` (el rol, sin ser el designado), `vicerrectorado.demo` y `rectorado.demo` (los otros firmantes, fuera de turno) reciben `[]` y 403 en la descarga; `coordinacion.demo`, sin el permiso, 403 en las dos |

La limpieza borra `firma.firmante_esperado` antes que los documentos, y la cuenta al final da 0. `probar_aprobacion.sh` hace lo mismo.

## Concurrencia

Sin escrituras concurrentes nuevas. Los firmantes esperados se insertan en la misma transacción que el documento, que ya tenía su bloqueo de la generación (F13-H02); la lista y la descarga solo leen.

## Comprobación a mano en el perfil local (T018, SC-001)

Hecha por el autor el 2026-11-14, con `quickstart.md` §3, sobre un perfil construido desde la rama y `MDD_CONSERVAR=1 deploy/probar_documento_firmable.sh`:

| Comprobación | Resultado |
|---|---|
| Aprobar y generar (paso 1) | Como `vicerrectorado.demo`, la tarjeta pasó a «Documento firmable generado», con su huella |
| Fuera de turno (FR-007, FR-010) | `vicerrectorado.demo` y `consolidacion.demo` ven «Pendientes de firma» con «No tienes firmas pendientes.» |
| En turno (FR-002, FR-008) | `docente.demo` ve una fila: 2026-I, versión 1, «Elaborado por» y la fecha; «Descargar el PDF» baja `distributivo-2026-I-v1.pdf` |
| La huella (FR-009, SC-002) | El `sha256sum` del archivo descargado es la huella de la tarjeta |
| SC-001 | **35 s** desde «Iniciar sesión» como `docente.demo` hasta la descarga (meta: menos de 1 minuto) |

El autor lo dio todo por bueno.

## Desviaciones del plan

| Plan | Lo hecho | Por qué |
|---|---|---|
| `DocumentoFirmable` gana `firmantes`, `pasoEnTurno` y `archivoVigente` | Un valor nuevo, `FirmasDelDocumento` (el documento, su cadena y sus firmantes), con `ArchivoDelDocumento` | El registro `DocumentoFirmable` se construye en cuatro lugares; el turno no le hace falta a ninguno (T005) |
| `pendientes` y `archivo` en `DocumentoFirmableServicio` | Un servicio nuevo, `FirmasPendientesServicio` | `DocumentoFirmableServicio` genera, consulta y reintenta; no comparten dependencias más allá del repositorio y el almacén (T011) |
| El controlador arma el solicitante del token | `SolicitanteDelToken`, compartido con `DocumentoControlador` | Lo mismo en los dos controladores (T015) |
| `PermisosFirmaTest` con las cuatro operaciones, como estaba | El contrato da un `example` al identificador del documento, y el doble entrega ese documento a cualquiera | El verificador usa los `example` de los parámetros; sin turno, la descarga daba 403 y no se veía pasar el permiso. El turno lo prueban el servicio y `FirmasPendientesContratoTest` (T015) |
| La pantalla con indicador de carga | Sin indicador: la tabla aparece al responder | La lista es una sola consulta pequeña; el botón se deshabilita mientras descarga |
| Los documentos de F13-H02 sin firmantes | No aparecen para nadie, sin relleno | Principio X; en la demo no hay documentos, y en un perfil local basta `down -v` (spec, supuestos) |
| El glosario: «Firmante» y «Paso de firma» | El modelo de dominio dice «usuario asignado» del `PasoCadena`; aquí es el firmante esperado, la persona impresa | Queda en D-102, en «Documentos que hay que poner al día» (T001) |
| F14-H03 compara la identificación con el certificado | Solo una posibilidad: D-23 compara el nombre con el CN | Se decide en F14-H03 |
| Integración del repositorio con el rol del proceso | `RepositorioDocumentosIntegrationTest` corre como superusuario; la clave foránea y los permisos con el rol del proceso los cubre `DocumentoFirmableEsquemaIntegrationTest` | Como en F13-H02 |
| Ver fallar cada prueba antes del código | Así en el dominio, la aplicación, los adaptadores, los contratos y Vitest. Se escribieron después del código: la prueba de ida y vuelta de la identificación en `RepositorioUsuariosJpaIntegrationTest` y `e2e/firmas-pendientes.spec.ts` | Se cuenta tal como pasó |

**Limitación conocida.** Las dos fallas de la corrida completa (`ClienteDeEventosLocalTest` y la medición de tiempo de `ConfirmarLasDiezCargasIntegrationTest`) son de módulos que esta rama no toca y pasan solas: dependen de la carga del equipo.

## Criterios de terminado (constitución §6)

| Criterio | Dónde |
|---|---|
| Escenarios de aceptación | `probar_documento_firmable.sh` (pasos 7 y 8); `FirmanteEsperadoTest`, `FirmasDelDocumentoTest`, `FirmasPendientesServicioTest`, `DocumentoFirmableServicioTest`, `FirmantesDesdeProcesosTest`, `IdentificacionDesdeUsuariosTest`, `AlmacenDeDocumentosEnDiscoTest`, `FirmasPendientesContratoTest`, `PermisosFirmaTest`, `RepositorioDocumentosIntegrationTest`, `DocumentoFirmableEsquemaIntegrationTest`; `UsuarioTest`, `CargaUsuariosDemoTest`, `IdentificacionContratoTest`, `UsuariosEsquemaIntegrationTest`, `RepositorioUsuariosJpaIntegrationTest`; `DocumentoFirmableIntegrationTest`; `FirmasPendientes.test.tsx` y `e2e/firmas-pendientes.spec.ts` |
| Contrato publicado y verificado | `listarFirmasPendientes` y `descargarArchivoVigente` en `contratos/firma/openapi.yaml`; `consultarIdentificacion` en `contratos/usuarios/openapi.yaml`; con sus pruebas de contrato |
| Prueba de arquitectura | ArchUnit y Spring Modulith en verde |
| Errores en el catálogo (FR-014) | `FIRMA_ARCHIVO_NO_DISPONIBLE` y `USUARIOS_SIN_IDENTIFICACION` en `contratos/comun/errores.md`, `ErroresFirma` y `ErroresUsuarios`, verificados por sus pruebas |
| JavaDoc | `FirmanteEsperado` (por qué se fija al generar y `reconoce`), `FirmasDelDocumento.pasoEnTurno`, `IdentificacionDeUsuarios` (por qué no viaja en el token), `DescargarArchivoVigente` (el SHA-256 recalculado y el 403 común) y la identificación de la cuenta (T020) |
| Seguridad (FR-015) | El permiso `DISTRIBUTIVO:FIRMA` y el turno, en el servidor; la SPA solo refleja. El 403 fuera de turno es el común y no dice si el documento existe. La identificación no va en el token ni en las respuestas públicas. `firmante_esperado` solo se inserta y se lee; el archivo se lee, se verifica y nunca se reescribe; el nombre del archivo se valida contra `documentos/<sha256>.pdf`. SQL parametrizado; SpotBugs y Gitleaks en verde |
| Decisión registrada | D-102, antes de implementar |

## Equipo de demostración (T022)

**Pendiente**, después de la fusión: `quickstart.md` §4, solo por lectura.
