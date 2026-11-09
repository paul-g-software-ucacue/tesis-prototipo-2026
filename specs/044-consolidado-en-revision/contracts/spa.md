# Contrato: la pantalla durante la revisión

**`Consolidado.tsx`**, con `enRevision`:
- no muestra «Consolidar», «Volver a consolidar» ni «Adjuntar el informe»;
- muestra, en un `Alert` informativo, «El distributivo está en revisión del Vicerrectorado: el consolidado ya no cambia.»;
- el informe vigente sigue con su botón para descargarlo.

**El panel** (`pantallas/inconsistencias/Inconsistencias.tsx`), con `enRevision`:
- no muestra la acción de justificar en ninguna fila;
- muestra el mismo aviso.

Sin `enRevision`, las dos pantallas quedan como hoy.

**Pruebas:**
- Vitest de `Consolidado` y del panel: con y sin `enRevision`; la descarga sigue.
- Playwright `consolidado.spec.ts`: el consolidado y el panel en revisión, con axe-core.
