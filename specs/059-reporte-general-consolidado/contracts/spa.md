# Contrato: el reporte general en la pantalla «Reportes»

F20-H02 (#269). Cambia la pantalla de F20-H01 en `frontend/src/pantallas/reportes/`. Siguen igual la ruta `/reportes`, el enlace del menú y el permiso. Los tipos salen del cliente generado del contrato de Reporting.

## Los estados (cambian)

| Estado | Qué se ve |
|---|---|
| Cargando, sin publicaciones, error | Sin cambios |
| Con publicaciones | La lista «Periodo» y la publicación, como hoy. Debajo, el grupo «Tipo de reporte» |
| «General» (propuesto) | El reporte general del periodo (abajo) |
| «Por docente» | El buscador y el reporte individual de F20-H01, sin cambios |

## «Tipo de reporte»

- **El control:** un `fieldset` con `legend` «Tipo de reporte» y dos `input type="radio"` con su `label`: «General» y «Por docente».
- **La opción propuesta:** «General».
- **Al cambiar de periodo,** se mantiene la opción elegida.
- **Al cambiar a «Por docente»,** el foco queda en el buscador.
- **El JSON del reporte general** (`consultarReporteGeneral`) se pide solo con «General» elegido; mientras carga, «Cargando el reporte general…», con `aria-live="polite"`.

## El reporte general en pantalla

- **El encabezado:**
  - el periodo y la versión;
  - el hash, en una fuente monoespaciada y con «Copiar», el mismo componente del individual;
  - la fecha de publicación;
  - «N docentes»;
  - el aviso de la reforma, si `enReforma`.
- **«Docentes del distributivo publicado»:** una tabla con `caption`.
  - **Columnas:** Identificación, Docente, Dedicación, Clase, Otras actividades de docencia, Investigación, Vinculación, Gestión y Total, cada encabezado con `th scope="col"`.
  - **Filas:** el nombre de cada docente en `th scope="row"`.
  - **Los totales:** la fila «Total institucional» va en `tfoot`, con `th scope="row"`.
- **Las horas:** con dos decimales y coma, alineadas a la derecha, con `horas()` de `consultas.ts`.
- **La dedicación:** con `NOMBRE_DE_LA_DEDICACION`, y «sin dato» si falta.

## Las descargas

- **Los botones:** «Descargar PDF» y «Descargar CSV» del reporte general, con el mismo componente `Descargas` del individual, que ahora recibe la ruta del reporte en lugar de la identificación.
- **El nombre del archivo:** el del `Content-Disposition` del servidor.
- **El CSV:** muestra el mismo modal del individual sobre la columna «Identificación» (`AVISO_DEL_CSV`).
- **Un error:** su título y su descripción del catálogo, con `role="alert"`, junto a los botones.

## Las pruebas

- **`Reportes.test.tsx` (Vitest):**
  - «General» propuesto, sin buscar a nadie;
  - la tabla con sus filas en orden y la fila de los totales;
  - «sin dato» en un docente sin dedicación;
  - las dos descargas con el nombre del servidor y el modal del CSV;
  - el error de una descarga;
  - el cambio a «Por docente», con el foco en el buscador;
  - el JSON del general no se pide con «Por docente».

  Los casos del individual suman el clic en «Por docente».
- **`reportes.spec.ts` (Playwright):**
  - el Rector ve el reporte general y baja los dos archivos;
  - los casos del individual, con el clic en «Por docente»;
  - axe-core (WCAG 2.1 AA) con «General» y con «Por docente», sin fallas (SC-009).
