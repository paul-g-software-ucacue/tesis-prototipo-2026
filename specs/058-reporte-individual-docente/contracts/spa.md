# Contrato: la pantalla «Reportes»

F20-H01 (#268). Una pantalla nueva en `frontend/src/pantallas/reportes/`, ruta `/reportes`. Los tipos salen del cliente generado del contrato de Reporting (`frontend/src/api/generado`).

## El acceso

- **El menú:** muestra «Reportes» solo con `REPORTES:LECTURA`.
- **La ruta:** `RutaConPermiso funcionalidad="REPORTES" tipo="LECTURA"`. Sin el permiso, la pantalla de acceso no autorizado que ya existe.
- **`enlaces.test.ts`:** `rectorado.demo`, `vicerrectorado.demo`, `secretaria.demo`, `ocs.demo` y `admin.demo` ven «Reportes»; `coordinacion.demo`, `docente.demo`, `datos.demo`, `gestion.demo` y `consolidacion.demo` no la ven.

## Los estados

| Estado | Qué se ve |
|---|---|
| Cargando | Un indicador con texto («Cargando los periodos publicados…»), anunciado con `aria-live="polite"` |
| Sin publicaciones | «Todavía no hay un distributivo publicado. Los reportes salen de la versión publicada.» Nada más |
| Con publicaciones | La lista «Periodo» con los periodos publicados; propone el primero (el de la publicación más reciente). Debajo, la publicación: «Versión N, publicada el … por …», y «Hay una reforma en curso; el reporte sale de esta versión» si `enReforma` |
| Docente sin elegir | El buscador «Docente», un combobox accesible que filtra por nombre o identificación mientras se escribe, con «N docentes» anunciado |
| Reporte | El encabezado, las tres tablas y los dos botones (abajo) |
| Error | El título y la descripción del catálogo, con `role="alert"`, como en las demás pantallas (`pantallas/comun`) |

## El reporte en pantalla

- **El encabezado:** el periodo, la versión, su hash (en una fuente monoespaciada, con un botón «Copiar»), la fecha de publicación, el nombre del docente, su identificación y su dedicación («sin dato» si falta).
- **«Asignaturas»:** una tabla con `caption` y las columnas Carrera, Código, Asignatura, Paralelo y Horas. Si está vacía: «No tiene asignaturas en esta versión».
- **«Actividades»:** una tabla con `caption` y las columnas Componente, Actividad, Unidad o proyecto y Horas. Si está vacía: «No tiene actividades en esta versión».
- **«Horas por componente»:** una tabla con los cinco componentes y una fila «Total» con `th scope="row"`.
- **Las horas:** con dos decimales y coma, alineadas a la derecha.

## Las descargas

- «Descargar PDF» y «Descargar CSV» piden `reporte.pdf` y `reporte.csv` con el token, y guardan el archivo con el nombre de `Content-Disposition`.
- Mientras descarga, el botón dice «Descargando…» y queda deshabilitado.
- Un error se muestra junto a los botones, con el código del catálogo.
- Al terminar de descargar el CSV, un modal «CSV descargado» (`DialogoInformativo`, el componente común para los mensajes que el usuario tiene que ver), con el foco en «Entendido» y que se cierra también con Escape; al cerrarse, el foco vuelve a «Descargar CSV». Dice: «Al abrir el CSV en una hoja de cálculo, importa la columna «Identificación» como texto para que conserve el cero inicial de la cédula.» (decisión del autor del 2026-11-21, opción A). No aparece con el PDF ni antes de descargar. Primero fue un texto fijo bajo los botones, que pasaba desapercibido; después un aviso en la página; el autor pidió un modal.

## Accesibilidad (FR-016)

- Cada control tiene su etiqueta visible.
- El orden de tabulación sigue la lectura.
- El foco va al encabezado del reporte al cargarlo.
- Ninguna información depende solo del color.
- Las tablas tienen `caption` y `th` con `scope`.

## Pruebas

**Vitest** (`Reportes.test.tsx`):
- sin publicaciones;
- con dos periodos (propone el más reciente);
- la reforma en curso;
- el filtro del buscador;
- un docente sin asignaturas;
- los totales;
- las dos descargas (el nombre del archivo);
- el error 409 y el 404.

**Playwright** (`frontend/e2e/reportes.spec.ts`), con axe-core:
- `rectorado.demo` abre «Reportes», elige un docente y descarga los dos archivos;
- `coordinacion.demo` no ve el enlace y recibe la pantalla de acceso no autorizado en `/reportes`;
- sin fallas WCAG 2.1 AA (SC-007).
