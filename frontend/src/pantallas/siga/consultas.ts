// El CSV para SIGA y el layout de exportación (contratos/reporting, F19-H01, RF-52, D-119): el
// CSV de la versión publicada vigente y el archivo de mapeo que decide sus columnas. Solo la
// Secretaría general académica (PUBLICACION:APROBACION).
import { useQuery } from '@tanstack/react-query';
import { apiReporting, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/reporting';
import { FalloDeApi } from '../../errores/traducir';
import { guardar, nombreDelServidor } from '../plantillas/consultas';

export type LayoutSiga = components['schemas']['LayoutSiga'];
export type LayoutsSiga = components['schemas']['LayoutsSiga'];
export type ColumnaDelLayoutSiga = components['schemas']['ColumnaDelLayoutSiga'];
export type ExportacionSiga = components['schemas']['ExportacionSiga'];

/** El CSV para SIGA generado: su nombre y su suma de verificación, si el servidor la mandó. */
export interface CsvGenerado {
  nombre: string;
  suma: string | null;
}

/** La clave de las exportaciones de un periodo, para recargarlas después de exportar. */
export function claveDeExportaciones(periodo: string) {
  return ['exportaciones-siga', periodo] as const;
}

/** Las exportaciones del periodo, de la más reciente a la más antigua (F19-H02, FR-018). */
export function useExportacionesSiga(periodo: string | null) {
  return useQuery({
    queryKey: claveDeExportaciones(periodo ?? ''),
    enabled: periodo !== null,
    queryFn: () =>
      llamar(
        apiReporting.GET('/api/reporting/periodos/{periodo}/exportaciones-siga', {
          params: { path: { periodo: periodo ?? '' } },
        }),
      ),
  });
}

/** El tamaño máximo del archivo de mapeo, el mismo del servidor (D-119). */
export const MAXIMO_DEL_ARCHIVO = 256 * 1024;

/** El mismo texto de REPORTING_ARCHIVO_DEMASIADO_GRANDE, para no subir en vano. */
export const ARCHIVO_DEMASIADO_GRANDE = {
  titulo: 'Archivo demasiado grande',
  descripcion: 'El archivo de mapeo pasa de 256 KB. Parte de la plantilla y vuelve a cargarlo.',
  detalles: [],
};

/** La clave del layout, para recargarlo después de una carga. */
export const CLAVE_LAYOUT = ['reporting', 'layout-siga'] as const;

/** El layout vigente y las versiones anteriores. */
export function useLayoutsSiga() {
  return useQuery({
    queryKey: CLAVE_LAYOUT,
    queryFn: () => llamar(apiReporting.GET('/api/reporting/layout-siga')),
  });
}

/**
 * Genera el CSV para SIGA con el token, que está solo en memoria (D-51), y lo guarda con el nombre
 * que manda el servidor. Cada CSV queda registrado, con su suma de verificación, antes de
 * entregarse (F19-H02, D-120).
 *
 * @returns el nombre del archivo guardado y su suma de verificación
 */
export async function generarCsvSiga(periodo: string): Promise<CsvGenerado> {
  const { data, error, response } = await apiReporting.GET(
    '/api/reporting/periodos/{periodo}/csv-siga',
    { params: { path: { periodo } }, parseAs: 'blob' },
  );
  if (!response.ok || !data) {
    throw new FalloDeApi(response.status, error);
  }
  const nombre = nombreDelServidor(response) ?? `siga-${periodo}.csv`;
  guardar(data, nombre);
  return { nombre, suma: response.headers.get('X-Suma-Verificacion') };
}

export async function descargarPlantilla() {
  const { data, error, response } = await apiReporting.GET(
    '/api/reporting/layout-siga/plantilla.yaml',
    { parseAs: 'blob' },
  );
  if (!response.ok || !data) {
    throw new FalloDeApi(response.status, error);
  }
  guardar(data, nombreDelServidor(response) ?? 'layout-siga-plantilla.yaml');
}

export function cargarLayout(archivo: File) {
  return llamar(
    apiReporting.PUT('/api/reporting/layout-siga', {
      // El contrato tipa la parte binaria como texto; va el archivo tal cual, en un FormData.
      body: { archivo: archivo as unknown as string },
      bodySerializer: (cuerpo: { archivo: string }) => {
        const formulario = new FormData();
        formulario.append('archivo', cuerpo.archivo as unknown as File);
        return formulario;
      },
    }),
  );
}
