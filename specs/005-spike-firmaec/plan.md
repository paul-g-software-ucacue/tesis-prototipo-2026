# Implementation Plan: Prueba temprana de firma con FirmaEC y validación con PDFBox

**Branch**: `005-spike-firmaec` | **Date**: 2026-10-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/005-spike-firmaec/spec.md` (issue #26, B00-S01)

## Summary

*Spike* de 2 días (D-45) para resolver la comprobación pendiente de la sección 8 de la constitución. En las pruebas del servicio `firma`, sin tocar su código de producción (D-44), se preparan:
- una CA de prueba con tres certificados de firmantes ficticios (script de OpenSSL);
- un PDF/A-2b de prueba generado con OpenHTMLtoPDF y validado con veraPDF;
- un validador con PDFBox y Bouncy Castle que comprueba, por cada firma, la integridad sobre su ByteRange, la cadena hasta la CA de prueba y la vigencia, más el prefijo de bytes entre las dos revisiones.

El autor firma el PDF dos veces con FirmaEC. Si FirmaEC rechaza la CA de prueba, sigue con su certificado real solo en local (D-43). El resultado queda en `evidencia.md` y en el issue #26, con un ADR si D-23 tiene que cambiar.

Los detalles están en [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (pruebas) · Bash (script de OpenSSL) · HTML (plantilla del PDF/A)

**Primary Dependencies** (solo en la suite `test` de `servicios/firma`):
- Apache PDFBox 3.0.8;
- Bouncy Castle `bcpkix-jdk18on` 1.86;
- OpenHTMLtoPDF 1.1.87;
- veraPDF `validation-model` 1.30.2.

**Herramientas externas**: FirmaEC, que opera el autor, y OpenSSL 3.

**Storage**: archivos de prueba en `backend/servicios/firma/src/test/resources/ca-prueba/` y `fuentes/`.

**Testing**: JUnit Jupiter 6 y AssertJ. Las firmas reales de FirmaEC y los casos negativos se generan en la prueba.

**Target Platform**: la máquina del autor (FirmaEC) y la CI (validación automática).

**Project Type**: *spike* dentro del monorepo; solo pruebas y documentación.

**Performance Goals**: no aplica.

**Constraints**:
- Ningún certificado real ni dato personal en el repositorio.
- La llave privada de la CA de prueba no se guarda.
- Tiempo fijo de 2 días.

**Scale/Scope**: 1 script, 4 clases de prueba, 1 plantilla HTML, 1 fuente, 3 certificados, 1 raíz y 3 PDF.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cómo lo respeta el plan | Estado |
|---|---|---|
| I y V | El código de producción del servicio `firma` no cambia; todo vive en sus pruebas. | ✅ |
| II, III, IV, VI, VII y XIII | No hay modelo, términos, contratos, reglas, datos ni errores nuevos. | ✅ N/A |
| VIII. Pruebas | El escenario 3 es una prueba automática que la CI repite, con 3 casos negativos. Datos ficticios. | ✅ |
| IX. Seguridad | CA de prueba solo para la demo, en la carpeta que permite Gitleaks, y sin la llave privada de la CA. El certificado real, si hace falta, nunca entra al repositorio (D-43). | ✅ |
| X y XI | Un *spike* con tiempo fijo; un validador de prueba, no un adaptador de producción (D-44). | ✅ |
| XII. JavaDoc | Las clases de prueba con JavaDoc en español. | ✅ |
| §3 Restricciones técnicas | FirmaEC en modo «descargar, firmar y subir», OpenHTMLtoPDF para el PDF/A (verificado con veraPDF), y PDFBox con Bouncy Castle para validar PAdES. | ✅ |
| §6.4 ADR | No hay decisiones que afecten a varias features en el plan. Si el resultado cambia D-23, se registra un ADR al cerrar (FR-010). | ✅ |
| §8 Comprobaciones de la ola 0 | El *spike* resuelve la fila «PDFBox y Bouncy Castle validan un PDF/A firmado con FirmaEC». | ✅ |

**Resultado del gate:** pasa, antes de la fase 0 y después de la fase 1.

## Project Structure

### Documentation (this feature)

```text
specs/005-spike-firmaec/
├── spec.md
├── plan.md                  # este archivo
├── research.md              # fase 0
├── data-model.md            # fase 1: CA, certificados, PDF/A, firma y resultado
├── quickstart.md            # fase 1
├── contracts/
│   ├── procedimiento-firma.md   # los pasos manuales con FirmaEC y qué anotar
│   └── validacion.md            # qué comprueba la validación y qué informa
├── checklists/
│   └── requirements.md
├── evidencia.md             # se crea al implementar
└── tasks.md                 # /speckit-tasks
```

### Source Code (repository root)

```text
backend/
├── gradle.properties                          # cambia: + versiones de PDFBox, OpenHTMLtoPDF y veraPDF
└── servicios/firma/
    ├── build.gradle                           # cambia: dependencias solo en la suite test
    └── src/test/
        ├── java/ec/mdd/firma/spike/
        │   ├── PdfaDePruebaTest.java              # genera el PDF/A-2b y lo valida con veraPDF
        │   ├── ValidadorPadesDePrueba.java        # integridad, cadena y vigencia por firma
        │   ├── FirmasFirmaecTest.java             # escenarios 2 y 3 sobre los PDF firmados con FirmaEC
        │   └── CasosNegativosTest.java            # byte alterado, otra raíz y certificado vencido
        └── resources/
            ├── ca-prueba/
            │   ├── generar.sh                     # OpenSSL: CA y tres firmantes
            │   ├── ca-prueba.crt  firmante-{1,2,3}.p12
            │   └── pdfa-prueba.pdf  pdfa-firmado-1.pdf  pdfa-firmado-2.pdf   # los firmados, si FirmaEC acepta la CA (D-43)
            ├── fuentes/DejaVuSans.ttf  LICENSE-DejaVu.txt
            └── spike/plantilla-pdfa.html
```

**Structure Decision**: todo en las pruebas del servicio `firma`, en el paquete `ec.mdd.firma.spike`, para que F14 lo encuentre y lo reutilice (D-44).

## Fases de implementación (guía para /speckit-tasks)

1. **Preparación:** versiones, dependencias de prueba y `evidencia.md`.
2. **CA de prueba** (escenario 1): `generar.sh`, la CA y los tres `.p12`. El autor intenta firmar con FirmaEC y se registra el resultado.
3. **PDF/A de prueba:** la plantilla, la fuente y `PdfaDePruebaTest` (OpenHTMLtoPDF y veraPDF). Copia para firmar.
4. **Validador y casos negativos**, con las pruebas primero: `ValidadorPadesDePrueba` y `CasosNegativosTest`, con firmas generadas en la prueba.
5. **Firma con FirmaEC** (escenario 2), por el autor: dos firmas; prefijo de bytes y validación (escenario 3) en `FirmasFirmaecTest`. Con certificado real, en modo local (D-43).
6. **Resultado** (escenario 4): `evidencia.md`, comentario en el issue #26, y la sección 8 de la constitución confirmada o un ADR. Pull request, CI en verde y fusión con la aprobación del autor.

## Complexity Tracking

Sin violaciones de la constitución.
