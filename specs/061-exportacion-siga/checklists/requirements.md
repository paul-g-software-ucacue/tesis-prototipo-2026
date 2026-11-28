# Specification Quality Checklist: Validar antes de exportar y registrar la exportación

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-11-22
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

- Primera pasada (2026-11-22): FR-014 decía «reemplaza a la entrada `CSV_SIGA_GENERADO` (o la completa)», con dos lecturas posibles; quedó como una sola entrada por exportación, la de F19-H01 con la suma de verificación. El resto pasó.
- Como en las specs anteriores del repositorio, la spec nombra los contextos (Reporting, Firma, Aprobación), los códigos del catálogo de errores que ya existen (`REPORTING_SIN_PUBLICACION`, `REPORTING_CONTENIDO_INCONSISTENTE`) y el SHA-256: son lenguaje del dominio y del glosario («Hash: SHA-256 del documento»), no decisiones de implementación. Las rutas, las tablas, los nombres de las acciones de la bitácora y los códigos de error nuevos quedan para el plan.
- Sin marcadores [NEEDS CLARIFICATION]. Para `/speckit-clarify` quedan propuestos: (1) el aviso en «Exportación a SIGA», como el de «Reportes», de que LibreOffice Calc lee la cédula como número y pierde el cero inicial (hallazgo del recorrido T038 de F19-H01); (2) si basta con verificar recalculando el SHA-256 fuera del sistema o se quiere una pantalla para subir un archivo y compararlo (la spec asume lo primero).
- Después de `/speckit-analyze` (2026-11-22):
  - Principio III: se quitaron de la spec los nombres de código (`ExportacionSiga`, `EntradaBitacora`).
  - FR-002 y el escenario 4 de la US1 comparan el hash de la versión del documento, no el «hash original» (que es el del PDF). Los estados de un paso son PENDIENTE, FIRMADO o RECHAZADO, y la anulación es del documento.
  - FR-003, FR-005 y un caso límite suman la versión sin el registro de sus inconsistencias (D-120).
  - FR-010 queda solo para la API; copiar la suma está en FR-019.
  - La cita del glosario de «Paso de la cadena» (con ANULADO) se deja literal: el glosario la describe así, y la anulación del código es del documento entero.
