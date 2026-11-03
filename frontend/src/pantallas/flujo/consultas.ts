// Los tipos, las consultas y las operaciones de «Flujo y firmas» (contratos/validacionocs,
// F12-H01).
import { useQuery } from '@tanstack/react-query';
import { apiValidacionocs, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/validacionocs';
import { FalloDeApi } from '../../errores/traducir';
import { guardar, nombreDelServidor } from '../plantillas/consultas';

export type ConfiguracionDelFlujo = components['schemas']['ConfiguracionDelFlujo'];
export type EtapaDelFlujo = components['schemas']['EtapaDelFlujo'];
export type PasoDeFirma = components['schemas']['PasoDeFirma'];

/** El tamaño máximo del archivo, el mismo del servidor (FR-010). */
export const MAXIMO_DEL_ARCHIVO = 1024 * 1024;

/** El mismo texto de VALIDACIONOCS_ARCHIVO_DEMASIADO_GRANDE, para no subir en vano. */
export const ARCHIVO_DEMASIADO_GRANDE = {
  titulo: 'Archivo demasiado grande',
  descripcion: 'El archivo pasa de 1 MB. Parte de la plantilla y vuelve a cargarlo.',
  detalles: [],
};

/** La clave de la configuración de todos los periodos, para recargarla después de una carga. */
export const CLAVE_FLUJO = ['validacionocs', 'configuracion-flujo'] as const;

/** Sin periodo, no consulta. */
export function useConfiguracionDelFlujo(periodo: string | undefined) {
  return useQuery({
    queryKey: [...CLAVE_FLUJO, periodo],
    queryFn: () =>
      llamar(
        apiValidacionocs.GET('/api/validacionocs/periodos/{periodo}/configuracion-flujo', {
          params: { path: { periodo: periodo ?? '' } },
        }),
      ),
    enabled: Boolean(periodo),
  });
}

export function cargarConfiguracion(periodo: string, archivo: File) {
  return llamar(
    apiValidacionocs.PUT('/api/validacionocs/periodos/{periodo}/configuracion-flujo', {
      params: { path: { periodo } },
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

export async function descargarPlantilla() {
  const { data, error, response } = await apiValidacionocs.GET(
    '/api/validacionocs/plantilla-flujo',
    { parseAs: 'blob' },
  );
  if (!response.ok || !data) {
    throw new FalloDeApi(response.status, error);
  }
  guardar(data, nombreDelServidor(response) ?? 'flujo-por-defecto.yaml');
}
