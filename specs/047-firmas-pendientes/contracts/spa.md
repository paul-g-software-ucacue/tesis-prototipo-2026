# Contrato: SPA

## Menú (`navegacion/Marco.tsx`)

Entrada «Pendientes de firma» → `/firmas-pendientes`, con el permiso `DISTRIBUTIVO:FIRMA`, después de «Flujo y firmas». La ruta se protege con `RutaConPermiso`, como las demás.

## Pantalla `pantallas/firmas/FirmasPendientes.tsx` (nueva)

Título «Pendientes de firma». Consulta `listarFirmasPendientes` (`firmas/consultas.ts`).

| Estado | Muestra |
|---|---|
| Con pendientes | Tabla «Firmas pendientes» con las columnas Periodo, Versión, Paso y Generado el; por fila, el botón «Descargar el PDF» |
| Sin pendientes | «No tienes firmas pendientes.» |
| Cargando | El indicador de siempre, con `role="status"` |
| Error | `AvisoDeError` con el mensaje del catálogo |

«Descargar el PDF»:
- pide `descargarArchivoVigente` con `parseAs: 'blob'`;
- guarda el archivo con el nombre de `Content-Disposition`, o `distributivo-<periodo>-v<version>.pdf` si no viene;
- se desactiva mientras descarga;
- un 403 o un 500 se muestran con `AvisoDeError`.

## Accesibilidad

La tabla tiene su `aria-label`. Cada botón nombra su fila («Descargar el PDF de 2026-I, versión 1»). axe-core (WCAG 2.1 AA) sin fallas con pendientes y sin ellos.
