# Implementation Plan: Validar firmante, turno y vigencia, y completar la cadena

**Branch**: `049-completar-cadena` | **Date**: 2026-11-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/049-completar-cadena/spec.md`

## Summary

**La validación de F14-H02 suma tres comprobaciones** en el dominio de Firma, después de la confianza (research §1):
- **vigencia:** el certificado está vigente al subir;
- **orden:** el CN no es el de la persona de un paso posterior;
- **firmante:** el CN normalizado es el nombre que el PDF imprime en el paso en turno (D-23).

**El nombre impreso se guarda con cada firmante esperado** al generar el documento (V5, research §2).

**Con la última firma aceptada, la cadena se completa:**
- Firma publica `CadenaDeFirmasCompleta` en la misma transacción (research §3);
- Aprobación lo escucha después de confirmar y pasa el distributivo a EN_OCS como el sistema (research §4).

**Pruebas y datos:**
- **La CA de prueba se regenera** con los nombres impresos de los datos de prueba, más un certificado vencido (research §5).
- **El autor prueba con FirmaEC** con `MDD_FIRMA_NOMBRE_LOCAL` en su `.env` (research §6).

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1.1) · TypeScript (React 19) · Bash

**Primary Dependencies**: las que ya están. Sin dependencias nuevas.

**Storage**: PostgreSQL. Firma V5 (`firmante_esperado.nombre`). Aprobación, sin migraciones.

**Testing**:
- **Dominio:**
  - `NombreDeFirmante` (mayúsculas, tildes, Ñ, espacios);
  - `ComprobacionDelFirmante`: vigente, vencido, todavía no vigente, el paso en turno, uno posterior, uno anterior, otro nombre, sin CN, y la misma persona en dos pasos;
  - `ResultadoValidacion` con siete;
  - `ProcesoAprobacion.completarFirmas`: en firma, repetido, en otra etapa y con otra versión.
- **Aplicación (Mockito):**
  - `SubirFirmaServicio`: el paso sin nombre (409, sin validar); cada rechazo nuevo con su código; el aviso solo con la última firma y en la transacción; el nombre local;
  - `CompletarFirmasServicio`.
- **Adaptadores e integración:**
  - V5 con el rol del proceso;
  - `RepositorioDocumentosJdbc` con el nombre;
  - `FirmantesConNombreLocal`;
  - en `apps:distributivo`, las tres firmas con la CA regenerada y EN_OCS, y el aviso repetido sin duplicar.
- **Contrato y permisos:** el 422 de los tres códigos, con siete detalles, y el 409.
- **SPA:** Vitest y Playwright con siete comprobaciones.
- **Punta a punta:** `probar_documento_firmable.sh` con los rechazos nuevos y EN_OCS.

**Target Platform**: el perfil consolidado y el equipo de demostración.

**Project Type**: monorepo web: Firma y Aprobación en el proceso `distributivo`, la SPA, la CA de prueba y un script.

**Performance Goals**: SC-002, EN_OCS en menos de 30 s después de la última firma. El aviso se entrega en cuanto confirma la transacción.

**Constraints**:
- **MD-05:** Firma no conoce el flujo; la dependencia sigue de Aprobación hacia Firma.
- **ADR-017:** sin interruptores en la validación; el nombre local es un dato del `.env`.
- **Principio IX:** el nombre real del autor nunca entra al repositorio; la CA de prueba sigue siendo ficticia y su llave no se guarda.
- **Principio VII:** las firmas y los firmantes no se reescriben.

**Scale/Scope**:
- Firma: 3 comprobaciones, 1 columna, 4 códigos de error, 1 evento.
- Aprobación: 1 caso de uso y 1 oyente.
- CA de prueba regenerada.
- SPA: 3 nombres.
- Script: 3 rechazos y EN_OCS.
- D-104.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I. Capas | Las tres comprobaciones y la normalización, en el dominio; el aviso, por un puerto. | ✅ |
| II. DDD | Firma decide si la cadena está completa; Aprobación, a qué etapa pasa. | ✅ |
| III. Lenguaje ubicuo | «Firmante en turno», «vigencia», «cadena completa» (`CadenaDeFirmasCompleta`, del glosario §6). | ✅ |
| IV. API primero | Los códigos y las comprobaciones nuevas, en el contrato antes del código. | ✅ |
| V. Límites | Firma no conoce el flujo; el evento va en su API publicada, y Aprobación lo escucha (MD-05). | ✅ |
| VI. Las reglas son datos | El nombre local, por configuración; la validación no se apaga (ADR-017). | ✅ |
| VII. Inmutabilidad | El nombre se fija al generar. La transición se agrega, nunca se reescribe. | ✅ |
| VIII. Pruebas | El dominio y la aplicación, viéndolos fallar; la cadena completa con PostgreSQL real; el script. | ✅ |
| IX. Seguridad | Identidad y orden en el servidor. El nombre real del autor, solo en su `.env`. La CA de prueba, ficticia y sin llave guardada. | ✅ |
| X. Simplicidad | Una columna, un evento, un caso de uso. Sin cédula ni número de serie. | ✅ |
| XI. Diseño pragmático | `.env` en vez de un archivo montado (research §6). | ✅ |
| XII. JavaDoc | La normalización, por qué «orden» va antes que «firmante», el evento y su idempotencia. | ✅ |
| XIII. Errores | Cuatro códigos nuevos con sus 8 columnas. | ✅ |
| XIV. Validación | El nombre impreso, de hasta 200 caracteres como el CN. | ✅ |

**Post-diseño:** sin cambios. D-104 queda registrada antes de implementar, con research §1 a §6, y la huella nueva de la CA de prueba en D-103.

## Project Structure

### Documentation (this feature)

```text
specs/049-completar-cadena/
├── spec.md, plan.md, research.md, data-model.md, quickstart.md
├── contracts/ (firma.md, validacionocs.md, spa.md, demostracion.md)
├── checklists/requirements.md
└── tasks.md            (/speckit-tasks)
```

### Source Code (repository root)

```text
contratos/firma/openapi.yaml, contratos/comun/errores.md

backend/servicios/firma/
├── dominio/            Comprobacion (+3), NombreDeFirmante, ComprobacionDelFirmante, FirmanteEsperado.nombre,
│                       ArchivoFirmadoRechazado (+3), PasoSinNombre
├── puertos/entrada/    CadenaDeFirmasCompleta
├── puertos/salida/     AvisoDeCadenaCompleta
├── aplicacion/         SubirFirmaServicio, DocumentoFirmableServicio (el nombre al generar)
├── adaptadores/salida/ AvisoDeCadenaCompletaSpring, FirmantesConNombreLocal, RepositorioDocumentosJdbc
├── src/main/resources/db/migration/firma/V5__nombre_del_firmante.sql
└── src/test/resources/ca-prueba/  generar.sh y los certificados regenerados

backend/servicios/validacionocs/
├── dominio/            ProcesoAprobacion.completarFirmas
├── puertos/entrada/    CompletarFirmas
├── aplicacion/         CompletarFirmasServicio
└── adaptadores/entrada/CierreDeLaFirma

frontend/src/pantallas/firmas/FirmasPendientes.tsx, e2e/firmas-pendientes.spec.ts
deploy/probar_documento_firmable.sh
docs/decisiones.md      D-104; D-103 con la huella nueva
```

**Structure Decision**: Firma suma las comprobaciones y el aviso; Aprobación, el paso a EN_OCS. La dependencia entre los dos no cambia de sentido.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| El CN real de FirmaEC tiene otra forma que el nombre impreso (orden de apellidos, abreviaturas). | La normalización de D-23 y, para el autor, `MDD_FIRMA_NOMBRE_LOCAL` con su CN tal cual. Si no coincide, se anota y se decide en otra historia. |
| OpenSSL no emite un certificado ya vencido con `-days`. | `generar.sh` usa `-not_before` y `-not_after` (OpenSSL 3.4 o posterior), o la CA de prueba en memoria solo para las pruebas unitarias. Se prueba al regenerar. |
| El aviso se pierde si el proceso se cae entre confirmar y entregar. | El registro de publicaciones lo reenvía (ADR-018), y `completarFirmas` es idempotente. |
| Regenerar la CA rompe las pruebas que usan los `.p12` viejos. | Se corren todas las de Firma después de regenerar; la huella cambia en D-103 y en `ListaConfianzaTest`. |
| El `.env` del autor queda con su nombre después de probar. | El quickstart dice quitarlo. No sale del equipo: Git lo ignora y la demo no lo usa. |

## Complexity Tracking

Sin violaciones que justificar.
