---
description: "Tareas de F19-H02: validar antes de exportar y registrar la exportación"
---

# Tasks: Validar antes de exportar y registrar la exportación

**Input**: documentos de diseño en `specs/061-exportacion-siga/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada tarea, la prueba compila y se ve fallar antes del código que la hace pasar. Si no compila, primero va un esqueleto. Firma y Generación son dominio central: TDD estricto.

**Rutas**:
- `REP/` es `backend/servicios/reporting/src/`;
- `FIR/` es `backend/servicios/firma/src/`;
- `GEN/` es `backend/servicios/generacion/src/`;
- `SPA/` es `frontend/src/`;
- `.../` es el paquete `ec/mdd/reporting/`, `ec/mdd/firma/` o `ec/mdd/generacion/`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede hacer en paralelo (otro archivo, sin depender de una tarea pendiente).
- **[Story]**:
  - **US1:** solo se exporta una versión firmada y sin inconsistencias;
  - **US2:** cada exportación queda registrada con su suma de verificación.

---

## Phase 1: Setup

- [X] T001 Escribir **D-120** en `docs/decisiones.md`, en la fila del índice (después de D-119) y en su sección, con research §1 a §12:
  - la ruta interna de Firma `consultarFirmasDeLaVersion`: Firma informa la cadena del documento de una versión, y la regla de «firmada» es de Reporting;
  - las inconsistencias abiertas que guarda Generación con el contenido congelado (V11), solo si la versión es la vigente del panel. Sin el dato, la versión no se exporta (`INCONSISTENCIAS_SIN_REGISTRO`), como ajuste a FR-003;
  - `CondicionesDeExportacion` con todos los motivos, y el 409 `REPORTING_VERSION_NO_EXPORTABLE`;
  - la `SumaDeVerificacion` (SHA-256 en hexadecimal en minúsculas, con `MessageDigest`);
  - el agregado `ExportacionSiga` en `reporting.exportacion_siga` (V3), sin `update` ni `delete`, guardado junto con su entrada antes de entregar;
  - `CSV_SIGA_GENERADO` ampliada (se conserva el nombre) y `EXPORTACION_SIGA_RECHAZADA`, que no bloquea el 409 si no se guarda;
  - el encabezado `X-Suma-Verificacion` en lugar de `Repr-Digest`, y GET se mantiene;
  - la operación `consultarExportacionesSiga`;
  - la pantalla: el aviso, la suma, copiar, la línea de `certutil`/`sha256sum` y la lista;
  - R11.

  **Alternativas:** las de cada sección de research. **Consecuencias:** una versión congelada antes de D-120 no se exporta; `datos`, `distributivo` y `salida` salen de la misma versión. **Estimación:** las 13 h restantes de F19 (D-119). Relacionar con RF-44, RF-53, RNF-INT-01, MD-07, MD-11, D-16, D-23, D-105, D-113, D-115, D-119 y ADR-018 y ADR-023.
- [X] T002 Confirmar tres cosas y, si algo falta, pedírselo al autor antes de seguir:
  - que la rama es `061-exportacion-siga` y tiene el commit `1c78fca` (los pendientes de F19-H01) sobre `main`;
  - que `.specify/feature.json` apunta a `specs/061-exportacion-siga`;
  - que el issue #267 tiene sus 13 h estimadas (D-119). El proyecto no tiene ese campo: confirmarlo con el autor.

  **Hecho** (2026-11-28): la rama y `feature.json` están bien; las 13 h son las que dejó D-119 para F19-H02, y D-120 las repite. Falta que el autor las confirme.
- [X] T003 Principio III: el término nuevo entra al glosario antes del código. En `~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/sdd/modelo_dominio.md` y en su `modelo_dominio.docx` (copiar antes los originales como `*.antes-F19-H02`), en §7, después de «Exportación a SIGA», agregar una fila (**hecho** el 2026-11-28 en el `.md` y en el `.docx`, con copias `*.antes-F19-H02`): «Suma de verificación»: «SHA-256 de los bytes del CSV para SIGA entregado, en hexadecimal. Se registra con la exportación y cualquiera la puede recalcular sobre el archivo.» (`SumaDeVerificacion`; Publicación y consulta; RF-53).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Propósito:** los contratos y el catálogo. Ninguna historia empieza antes (principio IV).

- [X] T004 [P] Ampliar `contratos/reporting/openapi.yaml` según `contracts/reporting.md`:
  - **en `generarCsvSiga`:**
    - el encabezado `X-Suma-Verificacion` (`components/headers/SumaVerificacion`, string, `pattern: '^[0-9a-f]{64}$'`) en el 200;
    - el 409 como `components/responses/NoExportable`, con `REPORTING_SIN_PUBLICACION` y `REPORTING_VERSION_NO_EXPORTABLE`;
    - las dos frases nuevas de la descripción;
  - **la operación `consultarExportacionesSiga`**: `GET /api/reporting/periodos/{periodo}/exportaciones-siga`, tag `Siga`, `x-mdd-permiso: [PUBLICACION:APROBACION]`. Respuestas 200 con `Cache-Control`, 400, 401, 403 y 500;
  - **`ExportacionesSiga`** (`periodo`, `exportaciones`);
  - **`ExportacionSiga`**, todos obligatorios y con `additionalProperties: false`: `id` (`format: uuid`), `version` (≥ 1), `layout` (≥ 0), `nombre` (`^siga-[A-Za-z0-9._-]{1,20}-v[0-9]+\.csv$`), `filas` (≥ 0), `sumaVerificacion` (`^[0-9a-f]{64}$`), `exportadaPor` y `exportadaEn` (`date-time`).
- [X] T005 [P] Ampliar `contratos/firma/openapi.yaml` según `contracts/interno.md`:
  - **la operación `consultarFirmasDeLaVersion`**: `GET /interno/firma/periodos/{periodo}/versiones/{numero}/firmas`, tag `Interno`, `periodo` con `^[A-Za-z0-9._-]{1,20}$` y `numero` con `minimum: 1`. Respuestas 200, 400, 401, 403, 404 (`COMUN_RECURSO_NO_ENCONTRADO`) y 500;
  - **`FirmasDeLaVersion`** (`periodo`, `version`, `hashContenido` con `^[0-9a-f]{64}$`, `pasos`, `anulada`, `rechazada`);
  - **`PasoDeLaVersion`** (`orden` ≥ 1, `estado` con `enum: [FIRMADO, PENDIENTE, RECHAZADO]`), los dos con `additionalProperties: false` y sin nombres ni rótulos.
- [X] T006 [P] En `contratos/generacion/openapi.yaml`, sumar a `ContenidoCongeladoCompleto` el campo opcional `inconsistenciasAbiertas`, con el esquema nuevo `InconsistenciasAlCongelar` (`duras` y `blandas`, integer, `minimum: 0`, los dos obligatorios, `additionalProperties: false`). Sumar también la frase de `contracts/interno.md` a la descripción de `consultarContenidoCongelado`.
- [X] T007 [P] En `contratos/comun/errores.md`, según research §8:
  - **el código nuevo** `REPORTING_VERSION_NO_EXPORTABLE` (409, Negocio), con su título, mensaje, acción, causa y descripción técnica;
  - **las causas ampliadas** de `COMUN_PROCESO_CON_ERROR` y `COMUN_PROCESO_SIN_RESPUESTA`: Firma, al validar el CSV para SIGA.
- [X] T008 Regenerar las interfaces de los servicios y los tipos de la SPA (`npm run tipos` en `frontend/`, que actualiza `SPA/api/generado/reporting.d.ts`) y confirmar que todo compila. Los controladores que no implementan aún las operaciones nuevas fallan solo en lo nuevo.

---

## Phase 3: User Story 1 - Solo se exporta una versión firmada y sin inconsistencias (Priority: P1) 🎯 MVP

**Goal**: antes de generar, Reporting valida que la versión publicada vigente tenga todas sus firmas sobre su documento y ninguna inconsistencia abierta registrada; si no, 409 con todos los motivos y la entrada `EXPORTACION_SIGA_RECHAZADA`.

**Independent Test**: los casos de `CondicionesDeExportacionTest` y `GenerarCsvSigaServicioTest`, el 409 de `SigaContratoTest`, y R1 y R9 del script (el 409 sin entrada y el camino feliz que pasa la validación).

### Firma: la cadena de una versión

- [X] T009 [US1] TDD. Primero, en `FirmasDelDocumentoTest` (`FIR/test/java/.../dominio/`), fijar lo que la ruta interna va a usar de `cadenaConEstado()`: una cadena completa; una con el paso en turno; una rechazada; una anulada. Después, en `DocumentoFirmableServicioTest` (`FIR/test/java/.../aplicacion/`), el caso de uso nuevo: las firmas del documento CONSOLIDADO de la versión pedida, aunque la vigente del periodo sea otra (D-113), y vacío si la versión no tiene documento. Verlos fallar y después:
  - crear el puerto de entrada `ConsultarFirmasDeLaVersion` (`FIR/main/java/.../puertos/entrada/`), con `Optional<FirmasDelDocumento> firmas(String periodo, int version)` y su JavaDoc: «Firma solo informa; la regla es de quien la usa»;
  - implementarlo en `DocumentoFirmableServicio` con `RepositorioDocumentos.documento(periodo, version)` y `firmas(id)`.
- [X] T010 [US1] Primero, `InternoFirmaContratoTest` (`FIR/test/java/.../adaptadores/entrada/`, con `ValidadorDeContrato`): el 200 con pasos `FIRMADO`, `PENDIENTE` (de `EN_TURNO` y de `PENDIENTE`) y `RECHAZADO`; `anulada` y `rechazada`; el 404 sin documento; el 403 con un token de usuario y el 401 sin token. Después, el controlador `InternoFirmaControlador` (`FIR/main/java/.../adaptadores/entrada/`), con la interfaz generada, y el bean en la configuración de Firma.

### Generación: las inconsistencias al congelar

- [X] T011 [US1] TDD. Primero, en `GuardarContenidoCongeladoServicioTest` (`GEN/test/java/.../aplicacion/`), con `ConsultarPanel` simulado:
  - si `panel.numero()` es la versión que se guarda, el contenido lleva `abiertas(DURA)` y `abiertas(BLANDA)`;
  - si es otra, o el panel no está creado, queda sin inconsistencias;
  - el guardado tardío sigue la misma regla.

  Verlo fallar y después:
  - crear `InconsistenciasAlCongelar(int duras, int blandas)` en `GEN/main/java/.../dominio/`, con «≥ 0» en los dos;
  - sumar `Optional<InconsistenciasAlCongelar>` a `ContenidoCongelado`;
  - en `GuardarContenidoCongeladoServicio`, recibir `ConsultarPanel` y pedir el panel fuera de la transacción;
  - ajustar `ConfiguracionGeneracion`.
- [X] T012 [US1] Primero, en `ContenidoCongeladoIntegrationTest` (`GEN/integrationTest/`, PostgreSQL real): guardar con y sin inconsistencias y leerlas; el `check` rechaza una columna llena y la otra vacía; el disparador sigue rechazando el `update`. Después:
  - `GEN/main/resources/db/migration/generacion/V11__inconsistencias_al_congelar.sql`: `alter table contenido_congelado add column duras_abiertas integer check (duras_abiertas >= 0), add column blandas_abiertas integer check (blandas_abiertas >= 0)` y `check ((duras_abiertas is null) = (blandas_abiertas is null))`, con su comentario;
  - el `insert` y la lectura en `RepositorioContenidoCongeladoJdbc`.
- [X] T013 [US1] Primero, en `InternoGeneracionContratoTest`, `consultarContenidoCongelado` con `inconsistenciasAbiertas` y sin él. Después, el mapeo en `InternoGeneracionControlador` (`GEN/main/java/.../adaptadores/entrada/`).

### Reporting: la validación

- [X] T014 [P] [US1] Primero, `CondicionesDeExportacionTest` (`REP/test/java/.../dominio/`), sin dobles:
  - firmas completas y 0 inconsistencias: sin motivos;
  - sin firmas: `SIN_DOCUMENTO_FIRMADO`;
  - `hashContenido` distinto del `hash` de la publicación: `DOCUMENTO_DE_OTRA_VERSION`;
  - anulada o rechazada: `FIRMAS_ANULADAS_O_RECHAZADAS`;
  - un paso sin firmar, o sin pasos: `FIRMAS_INCOMPLETAS`, con 2 de 3;
  - una cadena de un solo paso firmado: sin motivos;
  - sin el registro: `INCONSISTENCIAS_SIN_REGISTRO`;
  - 1 dura y 0 blandas, y 0 duras y 2 blandas: `INCONSISTENCIAS_ABIERTAS`;
  - faltan firmas y hay inconsistencias: los dos motivos, en orden;
  - `exigir()` lanza `ExportacionRechazada` solo con motivos.

  Verlo fallar y después crear en `REP/main/java/.../dominio/`, según `data-model.md`:
  - `FirmasDeLaVersion`: `hashContenido` con `^[0-9a-f]{64}$`; `PasoFirmado(int orden, boolean firmado)`, con «orden desde 1 y sin repetir»; `anulada`, `rechazada`, `firmados()`, `total()` y `completas(hashPublicado)`;
  - `InconsistenciasAbiertas` («≥ 0» en las dos) con `ninguna()`;
  - `MotivoDeRechazo`;
  - `CondicionesDeExportacion`;
  - `ExportacionRechazada`, con su JavaDoc: por qué falla cerrada sin el dato.
- [X] T015 [US1] Primero, los casos en `ClientesDeReportingTest` (`REP/test/java/.../adaptadores/salida/`):
  - `firmas(periodo, version)` lee `FirmasDeLaVersion`;
  - el 404 da vacío;
  - un 500 da `ProcesoConError` (502) y un tiempo agotado, `ProcesoSinRespuesta` (504);
  - una respuesta sin `hashContenido` da 502;
  - `cargas(periodo, version)` trae `inconsistenciasAbiertas` presente y ausente.

  Después:
  - el puerto `FirmasDeLasVersiones` (`REP/main/java/.../puertos/salida/`), con su JavaDoc;
  - el adaptador `FirmasDesdeFirma` (`REP/main/java/.../adaptadores/salida/`), con `ConsultasEntreProcesos` y `mdd.procesos.distributivo`;
  - `CargasDeLaVersion` suma `Optional<InconsistenciasAbiertas>`, y `ContenidoDesdeGeneracion` lo mapea;
  - el bean en `ConfiguracionReporting`.
- [X] T016 [US1] Primero, en `RegistroEnBitacoraEventosTest` y en `RegistroEnBitacoraIntegrationTest` (`backend/apps/salida/src/integrationTest/`), la entrada `EXPORTACION_SIGA_RECHAZADA`: objeto `{periodo, version}` y `nuevo` `{motivos, firmados, pasos, duras, blandas}`, sin nombres, identificaciones, horas ni hashes. Después:
  - `RechazoDeExportacion` en el dominio;
  - `registrar(RechazoDeExportacion)` en `RegistroEnBitacora` y en `RegistroEnBitacoraEventos`.
- [X] T017 [US1] Primero, los casos en `GenerarCsvSigaServicioTest` (`REP/test/java/.../aplicacion/`):
  - con firmas incompletas o inconsistencias abiertas: lanza `ExportacionRechazada`, no llama a `ExportadorSiga`, y registra el rechazo;
  - si el registro del rechazo falla, sigue lanzando `ExportacionRechazada`, y el fallo va al log;
  - `SinPublicacion` no pide las firmas ni registra nada;
  - Firma sin respuesta: el error de proceso, sin registro;
  - durante la reforma, pide las firmas de la versión publicada vigente;
  - una lectura de las firmas por pedido.

  Después, en `GenerarCsvSigaServicio`, el orden de research §3: la publicación, el layout, las firmas y el contenido, `CondicionesDeExportacion.exigir()`, y el registro del rechazo en su propia transacción antes de relanzar. `ConfiguracionReporting` le pasa `FirmasDeLasVersiones`.
- [X] T018 [US1] Primero, en `SigaContratoTest` y `ErroresReportingTest` (`REP/test/java/.../adaptadores/entrada/`):
  - el 409 `REPORTING_VERSION_NO_EXPORTABLE`, con la descripción del catálogo y un detalle por condición (`firmas`: «Tiene 2 de 3 firmas.»; `inconsistencias`: «Tiene 1 dura y 2 blandas sin justificar.» o «No se registraron sus inconsistencias al congelarla.»), sin hashes;
  - el 409 `REPORTING_SIN_PUBLICACION` sigue igual.

  Después, la traducción en `ErroresReporting` y el código en la prueba del catálogo.
- [X] T019 [P] [US1] En `SPA/pantallas/siga/Siga.tsx`, primero `Siga.test.tsx` (el aviso con publicaciones y sin él, y el 409 `REPORTING_VERSION_NO_EXPORTABLE` con su título y su mensaje, sin suma) y después:
  - la constante `AVISO_DEL_CSV_SIGA`, con el texto de research §7, en un `Alert` `info` entre el texto del layout y el botón (FR-019a);
  - el 409 nuevo, por el `Alert` de errores que ya existe.

**Checkpoint**: una versión publicada sin todas sus firmas o con inconsistencias abiertas no se exporta. La de la demo, sí.

---

## Phase 4: User Story 2 - Cada exportación queda registrada con su suma de verificación (Priority: P1)

**Goal**: cada CSV entregado es una `ExportacionSiga` con su suma de verificación, guardada junto con su entrada `CSV_SIGA_GENERADO` antes de entregar. La Secretaría ve la suma, la puede comprobar y consulta la lista del periodo.

**Independent Test**: R9 del script (el encabezado igual a `sha256sum`, 2 exportaciones con la misma suma y 2 entradas con la suma), R11 (inmutable) y `quickstart.md` §1.

- [X] T020 [P] [US2] Primero, `SumaDeVerificacionTest` y `ExportacionSigaTest` (`REP/test/java/.../dominio/`), sin dobles:
  - el SHA-256 de `abc` (`ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad`) y el del vacío;
  - un valor con mayúsculas o de 63 caracteres se rechaza;
  - `ExportacionSiga.de(...)` toma la suma de `archivo.contenido()`, el nombre de `ArchivoSiga.nombreDe`, la versión y el hash de la publicación, el número del layout y las filas;
  - las reglas de `data-model.md`: `usuario` «no vacío, hasta 64», `periodo` `^[A-Za-z0-9._-]{1,20}$`, `version` «≥ 1», `layout` «≥ 0» y `filas` «≥ 0».

  Después, `SumaDeVerificacion`, `ExportacionSiga` y `CsvExportado(ArchivoSiga, ExportacionSiga)` en `REP/main/java/.../dominio/`.
- [X] T021 [US2] Primero, `ExportacionesSigaJdbcIntegrationTest` (`REP/integrationTest/`, PostgreSQL real):
  - guardar y leer;
  - `delPeriodo` de la más reciente a la más antigua, y solo del periodo;
  - un `update` y un `delete` fallan con `insufficient_privilege`;
  - dos `guardar` concurrentes del mismo periodo quedan los dos (caso límite «Dos exportaciones casi al mismo tiempo»).

  Después:
  - `REP/main/resources/db/migration/reporting/V3__exportacion_siga.sql`, con las columnas y los `check` de `data-model.md`, el índice `exportacion_siga_del_periodo (periodo, exportada_en desc)` y el disparador `exportacion_siga_sin_cambios` (`before update or delete`), como `layout_exportacion_sin_cambios`;
  - el puerto `ExportacionesSiga` (`guardar`, `delPeriodo`), con su JavaDoc;
  - el adaptador `ExportacionesSigaJdbc`;
  - el bean.
- [X] T022 [US2] Primero, en `RegistroEnBitacoraEventosTest` y `RegistroEnBitacoraIntegrationTest`, la entrada `CSV_SIGA_GENERADO` desde la exportación: objeto `{periodo, version}` y `nuevo` `{layout, exportacion, filas, sha256}`, sin datos personales. Después:
  - `registrar(ExportacionSiga)` reemplaza a `registrar(CsvGenerado)` en `RegistroEnBitacora` y en `RegistroEnBitacoraEventos`;
  - borrar `CsvGenerado`;
  - el JavaDoc dice por qué se conserva el nombre de la acción (research §5).
- [X] T023 [US2] Primero, en `GenerarCsvSigaServicioTest`:
  - devuelve `CsvExportado`, con la suma de los bytes que entrega;
  - `exportaciones.guardar` y `registro.registrar(ExportacionSiga)` van en la misma `Transaccion`, antes de devolver;
  - si cualquiera de los dos falla, no se devuelve el archivo;
  - dos pedidos iguales dan dos exportaciones con la misma suma;
  - los 300 docentes en menos de 10 s (SC-007).

  Después, en `GenerarCsvSiga` (devuelve `CsvExportado`) y en `GenerarCsvSigaServicio`.
- [X] T024 [US2] Primero, `ConsultarExportacionesSigaServicioTest` (`REP/test/java/.../aplicacion/`): delega en `ExportacionesSiga.delPeriodo` y respeta el orden. Después, el puerto de entrada `ConsultarExportacionesSiga` y `ConsultarExportacionesSigaServicio`.
- [X] T025 [US2] Primero, en `SigaContratoTest`:
  - el 200 del CSV con `X-Suma-Verificacion`, que es el SHA-256 del cuerpo;
  - `consultarExportacionesSiga` con datos (`no-store`), vacía, 400 con un periodo inválido, y 403 sin `PUBLICACION:APROBACION`.

  Después, en `SigaControlador` (`REP/main/java/.../adaptadores/entrada/`), el encabezado y la operación nueva, con su mapeo a `ExportacionSiga` del contrato.
- [X] T026 [P] [US2] En `SPA/pantallas/siga/`, primero los casos de `Siga.test.tsx` y después `Siga.tsx` y `consultas.ts`, según `contracts/spa.md`:
  - **después de generar:** «Suma de verificación (SHA-256):» con el valor de `X-Suma-Verificacion` en `<code>`;
  - **el botón «Copiar la suma de verificación»**, con `navigator.clipboard.writeText`. Si funciona, «Se copió la suma de verificación.»; si no, «No se pudo copiar; selecciónela y cópiela a mano.»;
  - **la línea** «Para comprobarla: en Windows, `certutil -hashfile {archivo} SHA256`; en Linux, `sha256sum {archivo}`.»;
  - **la sección «Exportaciones del periodo»:** su `h2`, la tabla con su `caption` y las seis columnas («Layout» como «Por defecto» o «Versión N», la fecha en `America/Guayaquil`), y el texto «Todavía no se exportó el CSV para SIGA de este periodo.» cuando no hay;
  - **la invalidación** de `['exportaciones-siga', periodo]` al generar, y la limpieza del mensaje y de la suma al cambiar de periodo.
- [X] T027 [US2] En `frontend/e2e/siga.spec.ts`:
  - `secretaria.demo` genera el CSV y la suma de la pantalla es el SHA-256 del archivo descargado (`crypto` de Node);
  - la exportación aparece en la lista;
  - axe-core sin fallas con la suma y la lista, y con la lista vacía (SC-009).
- [X] T028 [US2] En `deploy/probar_documento_firmable.sh`, según `contracts/demostracion.md`:
  - la función `exportaciones_siga`;
  - **R1:** sin `EXPORTACION_SIGA_RECHAZADA` y la lista vacía;
  - **R4:** 403 en las exportaciones para `rectorado.demo`, `coordinacion.demo` y `admin.demo`;
  - **R9:** `X-Suma-Verificacion` igual a `sha256sum` e igual en los dos archivos; 2 exportaciones con esa suma; 2 entradas `CSV_SIGA_GENERADO` con `sha256` y `exportacion`;
  - **R5:** la misma suma y una exportación más;
  - **R11** (`exportacion_siga_inmutable`, después de R10): `update` y `delete` con `consultar_base` fallan con `insufficient_privilege`, y el número de filas no cambia;
  - las líneas `verificar` de R9 y R11, y el comentario de cabecera.

**Checkpoint**: la Secretaría exporta, ve la suma, la compara con `sha256sum` y consulta la lista. La bitácora tiene una entrada por exportación, con la suma.

---

## Phase 5: Polish & Cross-Cutting Concerns

- [X] T029 Correr el build completo (`./gradlew build integrationTest jacocoTestReport --continue`), la SPA (`npm run tipos`, `lint`, `formato:verificar`, `test`) y Playwright. Comprobar:
  - la cobertura de Reporting, Firma y Generación (mínimo 60 %, D-32);
  - Spotless, SpotBugs, ArchUnit y Spring Modulith en verde;
  - que Reporting no depende de clases de Firma, Generación ni Aprobación (FR-023);
  - Gitleaks sobre `main..HEAD`;
  - `BitacoraSinBorradoContratoTest` de Usuarios con `--rerun-tasks`: ninguna descripción de escritura nombra la bitácora (lección del PR #289);
  - el script completo contra el perfil local, con la base recreada (R1 a R11).
- [X] T030 [P] Revisar el JavaDoc (principio XII) en `CondicionesDeExportacion`, `FirmasDeLaVersion`, `ExportacionSiga`, `SumaDeVerificacion`, `GenerarCsvSiga`, `FirmasDeLasVersiones`, `ExportacionesSiga`, `RegistroEnBitacora`, `ConsultarFirmasDeLaVersion` y `GuardarContenidoCongeladoServicio`:
  - por qué las inconsistencias se fijan al congelar y solo si la versión es la vigente;
  - por qué se falla cerrado sin el dato;
  - por qué el rechazo se registra sin bloquear el 409;
  - por qué se conserva `CSV_SIGA_GENERADO`;
  - por qué Firma no decide qué es «firmada».
- [X] T031 [P] Medir SC-007 y anotarlo en la evidencia:
  - el tiempo de T023 con 300 docentes;
  - de punta a punta en el perfil local, por el API Gateway (`curl -w '%{time_total}'`, cinco pedidos);
  - el tiempo de la Secretaría desde que abre la pantalla hasta tener la suma (menos de 1 minuto).
- [X] T032 El recorrido de `quickstart.md` §1 a §3, con el teclado, en el perfil real:
  - comparar la suma de la pantalla con `sha256sum`;
  - copiarla;
  - ver la lista y la bitácora.

  El lector de pantalla de «Exportación a SIGA» (pendiente desde F19-H01) queda para el autor, y se anota.
- [X] T033 Crear `specs/061-exportacion-siga/evidencia.md`, como la de 060: las pruebas, el script, las mediciones, el recorrido, las desviaciones y los criterios de terminado. Anotar que el caso límite «La conexión se corta después de registrar» se cumple por diseño (el registro se guarda antes de responder, T023) y no tiene una prueba propia. Anotar como pendientes del autor el lector de pantalla (de F19-H01 y de esta historia) y la sección del equipo de demostración, después de fusionar.
- [ ] T034 Fusionar solo con la aprobación del autor y todas las comprobaciones en verde.
  - Antes, verificar la demo en solo lectura: «Sano» y la V3 de Reporting y la V11 de Generación aplicadas.
  - El PR lleva `Closes #267` y, si el autor lo confirma, cierra también la feature F19 (#258).

---

## Dependencies & Execution Order

- **Setup:** T001, T002 y T003 antes de todo (T003 antes de cualquier código: principio III).
- **Fundacional:**
  - T004, T005, T006 y T007 en paralelo;
  - T008 después de los cuatro.
- **US1:**
  - T009 después de T008;
  - T010 después de T009;
  - T011 después de T008;
  - T012 después de T011;
  - T013 después de T012;
  - T014 después de T003 (dominio puro, en paralelo con Firma y Generación);
  - T015 después de T008 y T014;
  - T016 después de T014;
  - T017 después de T015 y T016;
  - T018 después de T017;
  - T019 después de T008, en paralelo con el backend.
- **US2:**
  - T020 después de T003 (dominio puro);
  - T021 después de T020;
  - T022 después de T020;
  - T023 después de T017, T021 y T022;
  - T024 después de T021;
  - T025 después de T023 y T024;
  - T026 después de T008 y T019 (el mismo archivo);
  - T027 después de T025 y T026;
  - T028 después de T010, T013 y T025.
- **Cierre:**
  - T029 después de las dos historias;
  - T030 y T031 en paralelo con T029;
  - T032 después de T029;
  - T033 después de T029, T031 y T032;
  - T034 al final.

**Entre las historias:** US2 depende de US1 en un solo punto, `GenerarCsvSigaServicio` (T023 va después de T017), porque los dos cambian el mismo caso de uso. El dominio de US2 (T020 a T022) se puede hacer en paralelo con US1.

### Parallel Opportunities

```text
T004, T005, T006 y T007 (contratos).
Después de T008: la cadena de Firma (T009 → T010), la de Generación (T011 → T013), el dominio de Reporting (T014) y la SPA (T019), en paralelo.
T020 → T021 y T022 (dominio y base de US2) en paralelo con T015 → T018.
T026 (SPA, US2) en paralelo con T023 → T025.
T030 y T031 en paralelo con T029.
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Setup y fundación, T001 a T008: la decisión, el glosario y los contratos.
2. US1, T009 a T019: la validación de punta a punta. El CSV de F19-H01 ya no sale de una versión sin firmas o con inconsistencias.
3. Validar con `CondicionesDeExportacionTest`, `GenerarCsvSigaServicioTest`, `SigaContratoTest`, y R1 y R9.

### Incremental Delivery

1. La fundación.
2. US1: la validación y su rechazo en la bitácora.
3. US2: la suma, el registro, la lista y la pantalla, con R9 y R11. Con esto, el CSV es la Exportación a SIGA del glosario.
4. El cierre: el build, las mediciones, el recorrido, la evidencia y la fusión.

Una sola rama (`061-exportacion-siga`) y un solo PR. El primer commit de la historia lleva la spec, el plan, las tareas y D-120, como en F19-H01.
