---
description: "Tareas de F14-H03: validar firmante, turno y vigencia, y completar la cadena"
---

# Tasks: Validar firmante, turno y vigencia, y completar la cadena

**Input**: documentos de diseño en `specs/049-completar-cadena/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: se incluyen. El principio VIII exige escribir primero las pruebas: en cada grupo, la prueba compila y se ve fallar antes del código que la hace pasar. Si no compila, primero un esqueleto, y la implementación va en un commit aparte.

**Decisiones**:
- las tres aclaraciones del autor del 2026-11-14, en la spec;
- D-104, que se escribe en T001;
- D-23, D-102, D-103, ADR-017, ADR-007 y MD-05.

**Organization**:
- **Fundacional:** D-104, la CA de prueba, el contrato, los errores, la migración y el dominio.
- **US1:** solo firma la persona del paso, con un certificado vigente (MVP).
- **US2:** la cadena completa pasa el distributivo al OCS.
- **US3:** el autor firma la cadena con su certificado real.
- **Cierre:** la pantalla, el script, las regresiones, la evidencia y la fusión.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede hacerse en cualquier orden respecto de las otras [P] de su fase.
- **[Story]**: US1 a US3.
- **Rutas:** relativas a la raíz. Abreviaturas, con `main`, `test` e `integrationTest` en `java/ec/mdd/<servicio>`:
  - `FIR` = `backend/servicios/firma/src`;
  - `OCS` = `backend/servicios/validacionocs/src`;
  - `SPA` = `frontend/src`.

---

## Phase 1: Setup

- [X] T001 Escribir **D-104** en `docs/decisiones.md`, en la fila del índice y en su sección:
  - las tres comprobaciones, su orden y la normalización del nombre (research §1);
  - el nombre impreso guardado con el firmante esperado (research §2);
  - el aviso `CadenaDeFirmasCompleta` y el paso a EN_OCS por el sistema (research §3 y §4);
  - la CA de prueba regenerada con los nombres de los datos de prueba (research §5);
  - `MDD_FIRMA_NOMBRE_LOCAL` como el archivo local de ADR-017 (research §6).

  Relacionadas: RF-38, RF-39, RF-58, D-23, D-102, D-103, ADR-017, MD-05.

  Revisar en el glosario «Firmante en turno», «Cadena de firmas completa» (`CadenaDeFirmasCompleta`, §6) y «Vigencia», y anotar aquí el resultado.

  **Resultado (2026-11-14):** el glosario tiene «Firmante en turno» (§7.5, «usuario asignado al primer paso pendiente») y el evento `CadenaDeFirmasCompleta` (§6: «Se acepta la última firma» → Aprobación pasa a EN_OCS), el mismo nombre que se usa. No hay entrada para la vigencia del certificado. Las dos quedan en «Documentos que hay que poner al día» de D-104.
- [X] T002 Regenerar la CA de prueba (research §5):
  - en `FIR/test/resources/ca-prueba/generar.sh`:
    - los CN «Docente (demo)», «CABRERA VAZQUEZ ANA GABRIELA» y «PERALTA MALDONADO JUAN ANDRES»;
    - `firmante-vencido.p12`, con el CN de F1, `-not_before` y `-not_after` en el pasado;
    - la llave de la CA sigue sin guardarse;
  - correrlo, y copiar `ca-prueba.crt` a `FIR/main/resources/firma/raices/`;
  - anotar en D-103 la huella nueva y actualizarla en `ListaConfianzaTest`;
  - actualizar los CN esperados en `FIR/test/.../spike/FirmasFirmaecTest.java` («Firmante Uno Prueba (F1)» y «Firmante Dos Prueba (F2)»); `CasosNegativosTest` usa una CA en memoria y no cambia;
  - correr todas las pruebas de Firma después de regenerar;
  - comprobar que ningún archivo trae una llave fuera de los `.p12` de la CA de prueba, y Gitleaks sin hallazgos.

  **Resultado (2026-11-14):**
  - Hallazgo: la confianza se comprueba a la fecha de la firma (D-103), así que un certificado vencido firmado hoy fallaría en la confianza, no en la vigencia. El caso real es firmar con el certificado vigente y subir después de que venció. Por eso la CA es válida desde 2024 (`-not_before`) y el vencido, de enero de 2025; la herramienta de firma suma `--momento=<instante>` para firmar con esa fecha.
  - Huella nueva de `ca-prueba.crt`: `06d49759…bf17`, en D-103 y `ListaConfianzaTest`. Las 162 pruebas de Firma y las 21 de integración pasan con la CA nueva.
---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: ninguna historia empieza antes de T007.

- [X] T003 [P] En `contratos/firma/openapi.yaml`, según `contracts/firma.md`:
  - `VIGENCIA`, `ORDEN` y `FIRMANTE` en el enum;
  - `minItems` y `maxItems` 7;
  - el 409 de `subirFirma`;
  - la descripción, sin nombrar la bitácora: `BitacoraSinBorradoContratoTest` falla si una operación que escribe la nombra (pasó en F14-H02).
- [X] T004 [P] Los cuatro códigos en `contratos/comun/errores.md`, con sus 8 columnas. Ver fallar primero `ErroresFirmaTest` con las subclases nuevas y `PasoSinNombre`; luego, `ErroresFirma`.
- [X] T005 [P] La migración `FIR/main/resources/db/migration/firma/V5__nombre_del_firmante.sql` («`nombre` varchar(200)», nula). Antes, ver fallar en `DocumentoFirmableEsquemaIntegrationTest`, con el rol del proceso: inserta un firmante con nombre y sin él, y sigue sin poder actualizarlo.
- [X] T006 [P] El dominio de Firma. Ver fallar primero:
  - `NombreDeFirmanteTest`: mayúsculas, tildes, diéresis, la Ñ, espacios dobles, al inicio y al final, y paréntesis;
  - `ComprobacionDelFirmanteTest`:
    - vigente, vencido y todavía no vigente;
    - el CN del paso en turno; uno de un paso posterior (fuera de orden); uno de un paso anterior (firmante equivocado); otro nombre;
    - un sujeto sin CN: el certificado trae el nombre completo del sujeto (F14-H02), que nunca es un nombre impreso, así que da firmante equivocado;
    - la misma persona en dos pasos;
    - después de una rechazada, las siguientes no se evalúan;
  - `ResultadoValidacionTest`, con siete comprobaciones;
  - `FirmanteEsperadoTest`, con el nombre.

  Luego, `Comprobacion` (+3), `NombreDeFirmante`, `ComprobacionDelFirmante`, `FirmanteEsperado.nombre`, `ArchivoFirmadoRechazado` (+3) y `PasoSinNombre`. Ajustar las pruebas de F14-H02 que construyen resultados de cuatro comprobaciones.
- [X] T007 [P] El dominio de Aprobación. Ver fallar primero en `ProcesoAprobacionTest` `completarFirmas`:
  - en CONGELADO_EN_FIRMA con esa versión: una transición a EN_OCS por `sistema`, con el hash de la versión congelada;
  - ya en EN_OCS con esa versión: vacío;
  - en otra etapa o con otra versión: vacío.

  Luego `ProcesoAprobacion.completarFirmas`.

**Checkpoint**: el dominio decide, sin persistencia ni eventos.

---

## Phase 3: User Story 1 - Solo firma la persona del paso, con un certificado vigente (Priority: P1) 🎯 MVP

**Goal**: las tres comprobaciones nuevas en la subida, con el nombre impreso guardado.

**Independent Test**: con F3 en turno, las firmas de otra persona, fuera de orden o vencidas se rechazan con su 422; la del Rector vigente se acepta.

- [X] T008 [US1] El nombre impreso al generar. Ver fallar primero:
  - en `DocumentoFirmableServicioTest`, que cada firmante esperado registrado lleva el nombre que imprime el PDF;
  - en `RepositorioDocumentosIntegrationTest`, que se guarda y se lee;
  - en `RepositorioDocumentosIntegrationTest`, que las validaciones de cuatro comprobaciones se leen con las tres nuevas «no evaluadas» (research §8).

  Luego, en `DocumentoFirmableServicio.preparar` y `RepositorioDocumentosJdbc`.
- [ ] T009 [US1] `SubirFirmaServicio`. Ver fallar primero en `SubirFirmaServicioTest`:
  - el paso en turno sin nombre: `PasoSinNombre`, sin validar ni registrar;
  - cada rechazo nuevo, con su subclase, el resultado registrado y `FIRMA_RECHAZADA`;
  - la vigencia contra el momento de la subida del reloj.

  Luego, `ComprobacionDelFirmante` en la subida.
- [ ] T010 [US1] El contrato. En `SubirFirmaContratoTest`, ver fallar el 422 de los tres códigos con siete detalles y el 409. Ajustar el doble de `FirmasPendientesContratoTest`. Luego, el controlador si hace falta.
- [ ] T011 [US1] `ValidadorPadesTest` y `DocumentoFirmableIntegrationTest` (`apps:distributivo`) con la CA de prueba regenerada: las firmas de los `.p12` nuevos se aceptan en su paso.

**Checkpoint**: nadie firma por otro ni fuera de su turno.

---

## Phase 4: User Story 2 - La cadena completa pasa el distributivo al OCS (Priority: P1)

**Goal**: con la tercera firma, EN_OCS, una sola vez.

**Independent Test**: con F1 y F2 aceptadas, se acepta F3 y el distributivo queda en EN_OCS por `sistema`.

- [ ] T012 [US2] El aviso de Firma. Ver fallar primero en `SubirFirmaServicioTest`:
  - con la última firma aceptada, `AvisoDeCadenaCompleta.avisar` con el periodo y la versión, dentro de la transacción;
  - con F1 o F2, o un rechazo, nada.

  Luego, `CadenaDeFirmasCompleta` (en `puertos.entrada`), el puerto `AvisoDeCadenaCompleta`, su adaptador con el publicador de Spring y el bean.
- [ ] T013 [US2] Aprobación. Ver fallar primero en `CompletarFirmasServicioTest` (Mockito):
  - guarda la transición y registra el cambio en la bitácora;
  - repetido o en otra etapa, no guarda nada ni lanza.

  Luego `CompletarFirmas`, `CompletarFirmasServicio`, `CierreDeLaFirma` (`@ApplicationModuleListener`) y la configuración.
- [ ] T014 [US2] En `apps:distributivo`, `DocumentoFirmableIntegrationTest`:
  - después de las tres firmas, el proceso de aprobación del periodo está en EN_OCS en menos de 30 s, con la transición por `sistema`;
  - publicar el aviso otra vez no agrega otra.

  `ModulosTest` sin ciclos.

**Checkpoint**: la cadena completa avanza sola.

---

## Phase 5: User Story 3 - El autor firma la cadena con su certificado real (Priority: P2)

**Goal**: `MDD_FIRMA_NOMBRE_LOCAL`, solo local.

**Independent Test**: con la variable, el PDF imprime ese nombre en los tres pasos; sin ella, los de siempre.

- [ ] T015 [US3] Ver fallar primero `FirmantesConNombreLocalTest`:
  - con nombre, lo devuelve para cada rol y conserva el usuario o la identificación;
  - vacío, delega sin cambios.

  Luego, el decorador, la propiedad `mdd.firma.nombre-local` en `apps/distributivo` (`${MDD_FIRMA_NOMBRE_LOCAL:}`), una línea comentada en `.env.ejemplo` que dice que va en el `.env` local, y una prueba de que `.env.ejemplo` no le da valor.

**Checkpoint**: el autor puede firmar con su certificado sin tocar el repositorio.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T016 La pantalla: los tres nombres en `SPA/pantallas/firmas/FirmasPendientes.tsx`, con Vitest y Playwright de siete comprobaciones, y axe-core sin fallas.
- [ ] T017 `deploy/probar_documento_firmable.sh` según `contracts/demostracion.md`:
  - en el paso 9, fuera de orden y vencido;
  - en el paso 11, firmante equivocado;
  - en el paso 12, las cuentas nuevas y EN_OCS en menos de 30 s.

  La herramienta de firma ya lee cualquier `.p12`. La limpieza: comprobar que borra la transición a EN_OCS con el proceso. shellcheck sin hallazgos.
- [ ] T018 Correr todos los scripts del workflow sobre un perfil construido desde la rama. Recorrer `quickstart.md` §3 a mano con FirmaEC (autor): SC-002, SC-003 y SC-004.
- [ ] T019 Correr completos:
  - `./gradlew build integrationTest jacocoTestReport jacocoTestCoverageVerification`;
  - en la SPA, los tipos, ESLint, Prettier, Vitest y Playwright (SC-005).
- [ ] T020 Revisar el JavaDoc (principio XII): la normalización, por qué «orden» va antes que «firmante», el evento, su idempotencia y el nombre local.
- [ ] T021 Crear `specs/049-completar-cadena/evidencia.md`, como la de 048.
- [ ] T022 Fusionar solo con la aprobación del autor y todas las comprobaciones en verde. Verificar la demo (`quickstart.md` §4, solo lectura) y abrir el PR de evidencia con `Closes #64`. Borrar las ramas solo si el autor lo pide.

---

## Dependencies & Execution Order

- **Setup:** T001; T002 después de T001.
- **Fundacional:** T003 a T007 en paralelo, después de T001.
- **US1:**
  - T008 después de T005 y T006;
  - T009 después de T006 y T008;
  - T010 después de T003, T004 y T009;
  - T011 después de T002 y T009.
- **US2:**
  - T012 después de T009;
  - T013 después de T007;
  - T014 después de T011, T012 y T013.
- **US3:** T015 después de T008.
- **Cierre:** T016 en cuanto exista T003; T017 después de T002, T014 y T015; T018 a T022, al final.

### Parallel Opportunities

```text
T003, T004, T005, T006 y T007.
T013 (Aprobación) en paralelo con T008 a T012 (Firma).
T016 (SPA) en cuanto exista T003.
```

## Implementation Strategy

- **MVP:** Setup, Fundacional y US1 (T001 a T011): nadie firma por otro ni fuera de su turno.
- **Después:**
  - US2: EN_OCS;
  - US3: el nombre local para la prueba del autor.
- **Luego:** la pantalla, el script, las regresiones y el cierre.
