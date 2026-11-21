# Specification Quality Checklist: Reporte individual del docente en PDF y CSV

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-11-21
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

- Iteración 1: sin marcadores. Las dos decisiones con más impacto quedaron como supuestos y conviene confirmarlas en `/speckit-clarify`: (1) el reporte sale solo de la versión publicada vigente, no de la congelada en EN_OCS, aunque la feature F20 habla de «presentar ante el OCS»; (2) «PDF no editable» se entiende como PDF/A-2b sin campos ni contenido activo, sin cifrado.
- Los dos escenarios del issue están literales en US1 (escenario 1) y US2 (escenario 1).
- Los códigos 403, 404 y 409, PDF/A-2b y veraPDF siguen la convención de las specs anteriores (055 y 046): son el catálogo de errores (principio XIII) y el criterio verificable de «no editable», no una elección de tecnología nueva.
- Supuesto a revisar en el plan: registrar cada descarga en la bitácora (FR-012) por el principio VII y porque el reporte saca datos personales del sistema; si el autor lo considera de más (principio X), se quita.
- Para el plan: cómo lee Reporting la versión publicada, la carga horaria y los datos del docente desde el proceso de salida (solo llamadas de lectura, principio V); dónde se reutiliza la generación de PDF/A de Firma sin acoplar los módulos; la codificación del CSV para que las tildes se lean bien en una hoja de cálculo.
