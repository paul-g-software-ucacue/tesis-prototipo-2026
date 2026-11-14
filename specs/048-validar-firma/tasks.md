---
description: "Tareas de F14-H02: validar que el PDF subido es el mismo documento con una firma íntegra"
---

# Tasks: Validar que el PDF subido es el mismo documento con una firma íntegra

**Input**: documentos de diseño en `specs/048-validar-firma/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si no compila, primero un esqueleto, y la implementación va en un commit aparte.

**Decisiones**:
- las tres aclaraciones del autor del 2026-11-14, en la spec;
- D-103, que se escribe en T001;
- D-23, D-46, ADR-017, ADR-007, D-101, D-102 y D-88.

**Organization**:
- **Fundacional:**
  - D-103 y la raíz del BCE;
  - el contrato y los errores;
  - la migración;
  - el dominio del informe y de las firmas;
  - el validador.
- **US1:** el firmante en turno sube su firma y se registra (MVP).
- **US2:** un PDF que no es el vigente o cuya firma no es válida se rechaza.
- **US3:** cada validación deja su informe y su entrada en la bitácora.
- **US4:** quien no está en turno no sube nada.
- **Cierre:** la pantalla, la herramienta y el script, las regresiones, la evidencia y la fusión.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US4.
- **Rutas:** relativas a la raíz. Abreviaturas, cada una con `main` y `test` en `java/ec/mdd/firma`, e `integrationTest` donde aplica:
  - `FIR` = `backend/servicios/firma/src`;
  - `SPA` = `frontend/src`.

---

## Phase 1: Setup

- [X] T001 Escribir **D-103** en `docs/decisiones.md`, en la fila del índice y en su sección:
  - la validación de F14-H02: las cuatro comprobaciones, su orden y «no evaluada» (research §3);
  - la cadena se verifica a la fecha del `signingTime` firmado, y la vigencia al subir es de F14-H03;
  - una sola tabla, `validacion_firma`, donde la firma es la validación aceptada (research §1);
  - validar fuera de la transacción y volver a comprobar con el bloqueo (research §4);
  - la lista de confianza en los recursos de la imagen, con la CA de prueba y la raíz del BCE, y su huella (research §5);
  - la configuración por defecto es la de demostración: producción reemplaza `mdd.firma.raices` sin la CA de prueba (principio IX, S-10);
  - la herramienta de firma de los scripts (research §6);
  - **la enmienda de parche 2.2.1 de la constitución:** en el principio IX, las raíces públicas de confianza viven en `backend/servicios/firma/src/main/resources/firma/raices/`, con su excepción en Gitleaks solo para `.crt`; el certificado real del autor y cualquier llave siguen prohibidos.

  Relacionadas: RF-39, D-23, D-46, ADR-017, ADR-007, D-101, D-102.

  Luego, la enmienda en `.specify/memory/constitution.md`: la versión 2.2.1 en la cabecera, con su línea de historia, y el texto del principio IX.

  Revisar el glosario (`~/Downloads/MDD_artefactos_desarrollo/requerimientos_mdd/sdd/modelo_dominio.md` §7) por «Firma», «Informe de validación», «Comprobación», «Firma nueva» y «Lista de confianza» (principio III). Los que falten van a «Documentos que hay que poner al día» de D-103, con su definición, como en D-102. Anotar aquí el resultado.

  **Resultado (2026-11-14):** el glosario (§7.5) ya tiene «Firma» (`Firma`), «Validación básica» (`ResultadoValidacion`, «el informe de validación»), «Lista de confianza» (`ListaConfianza`) y «Archivo firmado rechazado» (`ArchivoFirmadoRechazado`). Para no tener sinónimos (principio III), el diseño cambió `InformeDeValidacion` por `ResultadoValidacion`, `ListaDeConfianza` por `ListaConfianza` y `FirmaRechazada` por `ArchivoFirmadoRechazado`. Faltan «Comprobación» y «Firma nueva», y `Firma` como validación aceptada difiere de la entidad de `PasoCadena`: quedan en «Documentos que hay que poner al día» de D-103. La constitución pasó a 2.2.1.
- [ ] T002 Las raíces de confianza, después de T001:
  - en `.gitleaks.toml`, una excepción para la regla `certificado-o-llave` solo en `^backend/servicios/firma/src/main/resources/firma/raices/[^/]+\.crt$`, con su comentario;
  - la misma excepción en el hook `certificado-o-llave` de `.pre-commit-config.yaml` (se cambian juntos);
  - comprobar que un `.p12` o un `.pem` en esa carpeta sigue rechazado.

  **Lo entrega el autor:** el certificado raíz del BCE que usó en el spike, del sitio oficial. Copiarlo a `FIR/main/resources/firma/raices/bce-raiz.crt` en PEM, y `ca-prueba.crt` (de `FIR/test/resources/ca-prueba/`) a la misma carpeta. Anotar en D-103 el sujeto, la vigencia y la huella SHA-256 de los dos. Comprobar que ninguno trae llave privada. Gitleaks sobre el commit, sin hallazgos.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T008.

- [X] T003 [P] En `contratos/firma/openapi.yaml`, según `contracts/firma.md`:
  - `subirFirma` (`POST /api/firma/documentos/{documento}/firmas`, `multipart/form-data` con la parte `archivo`), con `x-mdd-permiso: DISTRIBUTIVO:FIRMA`;
  - los esquemas `ResultadoValidacion`, `ResultadoDeComprobacion` («`motivo` maxLength 200») y `CertificadoDeLaFirma` («`serie` pattern `^[0-9a-f]{1,64}$`»);
  - las respuestas 201, 400, 401, 403, 413, 422, 500, 502 y 504.

  Actualizar el comentario de cabecera. Compilar `mdd.contrato` para ver la interfaz generada.
- [ ] T004 [P] Los seis códigos en `contratos/comun/errores.md`, con sus 8 columnas (`contracts/firma.md`): `FIRMA_NO_ES_LA_VERSION_VIGENTE`, `FIRMA_SIN_UNA_FIRMA_NUEVA`, `FIRMA_NO_INTEGRA`, `FIRMA_SIN_CONFIANZA` (422), `FIRMA_ARCHIVO_VACIO` (400) y `FIRMA_ARCHIVO_DEMASIADO_GRANDE` (413). Ver fallar primero `ErroresFirmaTest` con las excepciones nuevas, y luego registrarlas en `FIR/main/.../adaptadores/entrada/ErroresFirma.java`.
- [ ] T005 [P] La migración `FIR/main/resources/db/migration/firma/V4__validacion_firma.sql`, según `data-model.md`:
  - «`orden` check `>= 1`», «`sha256_subido` check `~ '^[0-9a-f]{64}$'`», «`tamano` check `> 0`»;
  - «`comprobaciones` check `jsonb_typeof = 'array'`»;
  - la clave foránea a `generacion_documento`;
  - el índice único parcial `where aceptada`;
  - `revoke update, delete, truncate`.

  Antes, ver fallar en `DocumentoFirmableEsquemaIntegrationTest` (`FIR/integrationTest`), con el rol del proceso:
  - inserta y lee;
  - no actualiza ni borra;
  - la clave foránea no da «permission denied»;
  - una segunda aceptada del mismo paso viola el índice, y una rechazada no.
- [ ] T006 [P] El dominio del informe. Ver fallar primero:
  - `ResultadoValidacionTest`:
    - exige las cuatro comprobaciones, una de cada una y en orden;
    - `aceptada()` solo con las cuatro aprobadas;
    - `primerRechazo()`;
    - rechaza una aprobada después de una rechazada de la que depende;
  - `ComprobacionDeDocumentoTest`: igual (rechazada), más corto, otro prefijo y más largo con el mismo prefijo (aprobada).

  Luego, en `FIR/main/.../dominio/`: `Comprobacion`, `Resultado`, `ResultadoDeComprobacion`, `CertificadoDeLaFirma`, `ResultadoValidacion` y `ComprobacionDeDocumento`. Las excepciones: `ArchivoFirmadoRechazado` (con el informe), `ArchivoVacio` y `ArchivoDemasiadoGrande`.
- [ ] T007 [P] Las firmas en `FirmasDelDocumento`. Ver fallar primero en `FirmasDelDocumentoTest`, con 0, 1, 2 y 3 firmas:
  - el paso en turno es el primero sin firma;
  - el archivo vigente es el de la firma de mayor orden;
  - con todo firmado, nadie está en turno.

  Luego `FirmaRegistrada` y el campo `firmas`. Ajustar quienes construyen `FirmasDelDocumento` (con una lista vacía, por ahora).
- [ ] T008 El validador y la lista de confianza (research §3, §5).
  - **Primero, Bouncy Castle a `implementation`** en `backend/servicios/firma/build.gradle`, con su comentario.
  - **`ValidadorPadesTest`**, en `FIR/test/.../adaptadores/salida/`, con PDF del generador de F13-H02 firmados por `FirmadorDePrueba` y la CA de prueba. Ver fallar:
    - una firma en el campo del paso, y una en un campo nuevo: las tres aprobadas, con el certificado;
    - sin firma nueva y con dos: `UNA_FIRMA_NUEVA` rechazada, el resto no evaluadas;
    - alterado después de firmar: `INTEGRIDAD` rechazada;
    - de la CA ajena: `CONFIANZA` rechazada;
    - un PDF dañado después del prefijo: `UNA_FIRMA_NUEVA` rechazada, con un motivo fijo y sin el mensaje de la excepción;
    - las tres firmas sucesivas, todas aprobadas, y `ValidadorPadesDePrueba` del spike las da válidas (SC-005).
  - **`ListaConfianzaTest`:**
    - carga los PEM;
    - sin raíces, o con un archivo ilegible, falla: no hay forma de arrancar sin validar (FR-009);
    - `bce-raiz.crt` tiene la huella de D-103;
    - cada archivo de `firma/raices/` es solo un certificado, sin llave privada;
    - la propiedad `mdd.firma.raices` reemplaza la lista: con solo la CA de prueba, una firma de una raíz del BCE no es de confianza (C2).
  - **Arquitectura:** en la prueba de ArchUnit de Firma, `org.bouncycastle` y `org.apache.pdfbox` solo en los adaptadores; nunca en el dominio, los puertos ni la aplicación.

  `FirmadorDePrueba` sale del paquete `spike` a uno compartido de pruebas, o se le agrega lo que falte: firmar en un campo existente, `adbe.pkcs7.detached` y alterar. Luego, el puerto `ValidadorDeFirmas` (`HallazgosDeLaFirma`), `ValidadorPades` y `ListaConfianza` en `FIR/main/.../adaptadores/salida/`.

**Checkpoint**: el dominio decide y el validador dice qué encontró, sin persistencia ni HTTP.

---

## Phase 3: User Story 1 - El firmante en turno sube su firma y se registra (Priority: P1) 🎯 MVP

**Goal**: el firmante en turno sube el PDF firmado, se registra la firma y el turno pasa al siguiente.

**Independent Test**: con el paso en F1, `docente.demo` sube el vigente firmado con F1; recibe 201 con las cuatro aprobadas; el Vicerrector ve el documento y descarga el PDF con la primera firma.

- [ ] T009 [US1] `RepositorioDocumentosJdbc`. Ver fallar primero en `RepositorioDocumentosIntegrationTest`:
  - `firmas(id)` trae las firmas registradas, por orden;
  - `registrarValidacion` inserta la aceptada y la rechazada;
  - si las firmas cambiaron desde la validación, devuelve que se convirtió en rechazo y lo guarda así;
  - dos registros a la vez del mismo paso: uno se acepta.

  Luego, `registrarValidacion` con `FOR UPDATE` sobre `generacion_documento` (research §4) y la lectura de las firmas en `firmas` y en `generados`.
- [ ] T010 [US1] `SubirFirmaServicio`, en `FIR/main/.../aplicacion/`, con el puerto de entrada `SubirFirma`. Ver fallar primero en `SubirFirmaServicioTest` (Mockito):
  - en turno y aceptada:
    - guarda el archivo por su SHA-256;
    - registra la validación aceptada;
    - publica `FIRMA_ACEPTADA` con el detalle de research §9;
    - devuelve el informe;
  - vacío (`ArchivoVacio`) y más de 10 MB (`ArchivoDemasiadoGrande`), sin validar ni registrar;
  - el vigente falta o no coincide: `ArchivoNoDisponible`, sin registrar;
  - la consulta de la identificación, fuera de la transacción.

  La regla del turno no se copia: el cálculo de «¿está este solicitante en turno en este documento?», con la identificación pedida una sola vez, sale de `FirmasPendientesServicio` a un componente de aplicación compartido por los dos servicios. `FirmasPendientesServicio` pasa a usar las firmas registradas, en lugar de `SIN_FIRMAS`, y sus pruebas siguen pasando.
- [ ] T011 [US1] El controlador. Ver fallar primero en `SubirFirmaContratoTest`:
  - 201 con el informe según el esquema;
  - 400 sin la parte `archivo`, con un uuid mal formado y con un archivo vacío;
  - 413.

  `PermisosFirmaTest` con las cinco operaciones. Luego, `subirFirma` en `FirmasPendientesControlador`, la configuración `mdd.firma.raices` y los beans en `ConfiguracionFirma`. En `apps/distributivo`, la configuración por defecto con las dos raíces, y en las pruebas, solo la CA de prueba.
- [ ] T012 [US1] En `apps/distributivo`, `DocumentoFirmableIntegrationTest`: con PostgreSQL real y el documento generado, F1, F2 y F3 se suben firmados por `FirmadorDePrueba` y se aceptan. Después de cada una:
  - el archivo vigente es el nuevo;
  - el siguiente está en turno;
  - el archivo anterior sigue en el almacén.

**Checkpoint**: la cadena de tres firmas se completa por la API.

---

## Phase 4: User Story 2 - Un PDF que no es el vigente o cuya firma no es válida se rechaza (Priority: P1)

**Goal**: los cinco rechazos con su 422 y nada cambia.

**Independent Test**: los PDF de los escenarios del issue y sin firma nueva se rechazan con su código; el turno y el archivo vigente siguen iguales.

- [ ] T013 [US2] Ver fallar primero en `SubirFirmaServicioTest`:
  - cada comprobación rechazada lanza `ArchivoFirmadoRechazado` con el informe, y su código es el de `primerRechazo()`;
  - no guarda el archivo;
  - registra la validación rechazada;
  - las firmas cambiaron entre validar y registrar: `ArchivoFirmadoRechazado` por «mismo documento».

  En `SubirFirmaContratoTest`: el 422 de cada código, con las cuatro comprobaciones en `detalles` (`campo` y `descripcion`). Luego, la traducción en `ErroresFirma` o en el controlador.

**Checkpoint**: nadie cuela un documento distinto.

---

## Phase 5: User Story 3 - Cada validación deja su informe y su entrada en la bitácora (Priority: P1)

**Goal**: el informe y la bitácora de cada subida del firmante en turno.

**Independent Test**: después de una aceptada y una rechazada, las dos tienen su fila en `validacion_firma` y su entrada en la bitácora.

- [ ] T014 [US3] Ver fallar primero:
  - en `SubirFirmaServicioTest`: `FIRMA_RECHAZADA` con el detalle de research §9, en la misma transacción que la validación;
  - en `PublicadorDeCambiosTest`: las dos acciones nuevas;
  - en el informe rechazado, el certificado si había una sola firma nueva, y sin él si no.

  Luego, las acciones en `RegistroDeCambios.CambioDeFirma` y su publicación.

**Checkpoint**: cada subida es trazable.

---

## Phase 6: User Story 4 - Quien no está en turno no sube nada (Priority: P1)

**Goal**: el 403 común para todos los demás, sin informe.

**Independent Test**: el Vicerrector, el Rector, otro usuario con el rol de Responsable y uno sin permiso suben con F1 en turno, y reciben 403 sin fila.

- [ ] T015 [US4] Ver fallar primero en `SubirFirmaServicioTest`: fuera de turno, otro con el rol, un documento inexistente, EN_CURSO o FALLIDO lanzan `SinTurnoDeFirma`. No validan, no guardan, no registran y no publican.

  En `SubirFirmaContratoTest`: el 403 tiene el mismo cuerpo que sin permiso y no menciona el documento.

**Checkpoint**: el turno se respeta en la subida.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T016 La pantalla, según `contracts/spa.md`:
  - `subirFirma` en `SPA/pantallas/firmas/consultas.ts`;
  - «Subir el PDF firmado» en `FirmasPendientes.tsx`;
  - el informe, como componente propio si crece.

  Ver fallar primero `FirmasPendientes.test.tsx`:
  - el botón por fila con su `aria-label`;
  - sube el archivo elegido;
  - con 201, el aviso «Firma registrada» con el informe, y la lista se vuelve a pedir;
  - con 422, el aviso con el título y las cuatro comprobaciones desde `detalles`;
  - otro error, con `AvisoDeError`;
  - los dos botones de la fila desactivados mientras sube.
- [ ] T017 [P] Playwright en `frontend/e2e/firmas-pendientes.spec.ts`, con la API simulada: la subida aceptada y la rechazada, con axe-core (WCAG 2.1 AA) sin fallas en las dos (SC-006).
- [ ] T018 [P] La herramienta `deploy/herramientas/FirmarDePrueba.java`, según `contracts/demostracion.md` y research §6:
  - copia las bibliotecas de `/app/BOOT-INF/lib` del contenedor `distributivo` y falla con un mensaje claro si no están;
  - la contraseña ficticia, por `MDD_CLAVE_P12`, sin imprimirla.

  Que Spotless la formatee, si su configuración lo permite. Si no, anotar por qué.
- [ ] T019 `deploy/probar_documento_firmable.sh`, pasos 9 a 12 (`contracts/demostracion.md`):
  - los cinco rechazos;
  - las dos subidas fuera de turno;
  - las tres firmas con el siguiente en turno;
  - las cuentas en la base y en la bitácora.

  Actualizar la cabecera. La limpieza borra `validacion_firma` primero, también en `probar_aprobacion.sh`, y la cuenta en el paso 0 y al final. Los archivos temporales, en la carpeta temporal del script. shellcheck sin hallazgos.
- [ ] T020 Correr todos los scripts del workflow, en su orden, sobre un perfil construido desde la rama. Recorrer `quickstart.md` §3 a mano con FirmaEC (autor):
  - medir SC-001;
  - anotar cómo firma FirmaEC (research §7);
  - abrir el PDF final con las tres firmas (SC-005).
- [ ] T021 Correr completos:
  - `./gradlew build integrationTest`;
  - `:apps:distributivo:test` (Spring Modulith);
  - la cobertura por módulo (D-32), Spotless y SpotBugs;
  - en la SPA, los tipos, ESLint, Prettier, Vitest y Playwright (SC-007).
- [ ] T022 Revisar el JavaDoc (principio XII):
  - el orden de las comprobaciones y «no evaluada»;
  - la fecha de la cadena;
  - el relleno de ceros y la llave pública (D-46);
  - el bloqueo y la nueva comprobación;
  - la lista de confianza sin interruptores (ADR-017);
  - por qué la firma es la validación aceptada.
- [ ] T023 Crear `specs/048-validar-firma/evidencia.md`, como la de 047, con:
  - las pruebas, la cobertura, la arquitectura, la SPA, los scripts, shellcheck y Gitleaks;
  - la tabla del script;
  - el recorrido con FirmaEC;
  - las desviaciones y los criterios de terminado.
- [ ] T024 Fusionar solo con la aprobación del autor y con todas las comprobaciones en verde. Verificar el despliegue en el equipo de demostración (`quickstart.md` §4, solo lectura) y abrir el PR de evidencia con `Closes #63`. Borrar las ramas solo si el autor lo pide.

---

## Dependencies & Execution Order

- **Setup:** T001; T002 espera la raíz del autor, pero no bloquea hasta T008.
- **Fundacional:**
  - T003 a T007 en paralelo entre sí;
  - T008 después de T002 y T006.
- **US1:**
  - T009 después de T005 y T007;
  - T010 después de T008 y T009;
  - T011 después de T003, T004 y T010;
  - T012 después de T011.
- **US2:** T013 después de T010 y T011.
- **US3:** T014 después de T010.
- **US4:** T015 después de T010.
- **Cierre:**
  - T016 puede empezar con la API simulada en cuanto exista T003;
  - T017 después de T016;
  - T018 en paralelo con todo lo de Firma;
  - T019 después de US1 a US4 y T018;
  - T020 a T024, al final.

### Parallel Opportunities

```text
T003, T004, T005, T006 y T007.
T016 (SPA) con la API simulada en cuanto exista T003.
T018 (herramienta) en paralelo con las historias.
```

## Implementation Strategy

- **MVP:** Setup, Fundacional y US1 (T001 a T012): la cadena de tres firmas por la API.
- **Después:**
  - US2: los rechazos;
  - US3: la bitácora;
  - US4: el 403, que la regla de F14-H01 ya casi cubre.
- **Luego:** la pantalla, la herramienta, el script, las regresiones y el cierre.
