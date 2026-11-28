# Contrato: la pantalla «Exportación a SIGA» (cambia)

F19-H02 (#267). Se amplía `frontend/src/pantallas/siga/Siga.tsx` (research §7). Los tipos salen del cliente regenerado (`frontend/src/api/generado/reporting.d.ts`). La ruta, el menú, los estados generales y la sección «Layout de exportación» no cambian.

## Sección «CSV para SIGA» (US1 y US2)

- **El aviso** (FR-019a): `AVISO_DEL_CSV_SIGA`, en un `Alert` de severidad `info`, entre el texto del layout y el botón. Su texto es el de research §7. Solo se ve si hay publicaciones.
- **El botón «Generar CSV para SIGA»** sigue igual, y al terminar bien:
  - «Se generó siga-2026-I-v1.csv.», en la región `aria-live="polite"` que ya existe;
  - **la suma de verificación:** el texto «Suma de verificación (SHA-256):» y el valor de `X-Suma-Verificacion` en un `<code>` que se puede seleccionar;
  - **el botón «Copiar la suma de verificación»:** usa `navigator.clipboard.writeText`. Al copiar, «Se copió la suma de verificación.» en la región `aria-live`. Si el navegador no deja copiar, «No se pudo copiar; selecciónela y cópiela a mano.», sin error;
  - **la línea de FR-011:** «Para comprobarla: en Windows, `certutil -hashfile siga-2026-I-v1.csv SHA256`; en Linux, `sha256sum siga-2026-I-v1.csv`.», con el nombre del archivo generado;
  - se invalida la consulta de las exportaciones del periodo.
- **Si falla:** el `titulo` y la `descripcion` del catálogo en el `Alert` con `role="alert"` que ya existe. El 409 de `REPORTING_VERSION_NO_EXPORTABLE` muestra «Versión no exportable», la descripción del catálogo y la lista de sus `detalles` (uno por condición que falla). Ni la suma ni la línea aparecen.
- **Al cambiar de periodo** se borran el mensaje, la suma y el error del anterior.

## Sección «Exportaciones del periodo» (US2, FR-018)

- **Dónde:** una `section` con su `h2`, entre «CSV para SIGA» y «Layout de exportación». Solo se ve si hay publicaciones, para el periodo elegido.
- **La consulta:** `consultarExportacionesSiga` del periodo, con TanStack Query y la clave `['exportaciones-siga', periodo]`.
- **Con datos:** una tabla con `caption` «Exportaciones del CSV para SIGA de {periodo}» y `th` con `scope="col"`:

  | Columna | Valor |
  |---|---|
  | Fecha y hora | `exportadaEn` en `America/Guayaquil`, `aaaa-mm-dd hh:mm` |
  | Usuario | `exportadaPor` |
  | Versión | `version` |
  | Layout | «Por defecto» si es 0; si no, «Versión N» |
  | Archivo | `nombre` |
  | Suma de verificación | `sumaVerificacion` en `<code>`, completa, con corte de línea permitido |

- **Sin datos:** «Todavía no se exportó el CSV para SIGA de este periodo.».
- **Cargando y error:** como los demás estados de la pantalla.

## Accesibilidad (FR-021, SC-009)

- La sección nueva tiene su `h2`; la tabla, su `caption` y sus `th`.
- El botón de copiar tiene texto propio.
- Los mensajes de copia van en la región `aria-live`.
- Las sumas en `<code>` no reciben foco.

## Las pruebas

**Vitest (`Siga.test.tsx`):**
- el aviso con publicaciones, y sin aviso cuando no hay;
- después de generar: el mensaje, la suma del encabezado, la línea con el nombre del archivo y la invalidación de la lista;
- copiar, cuando se puede y cuando el navegador no deja;
- el 409 `REPORTING_VERSION_NO_EXPORTABLE`, sin suma;
- la lista con dos exportaciones (el layout por defecto y uno cargado), la lista vacía, y el cambio de periodo.

**Playwright (`frontend/e2e/siga.spec.ts`):**
- `secretaria.demo` genera el CSV, ve la suma igual al SHA-256 del archivo descargado (con `crypto` de Node) y la exportación nueva en la lista;
- axe-core sin fallas con la suma y la lista, y con la lista vacía.
