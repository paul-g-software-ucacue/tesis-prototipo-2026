// Los tipos, las consultas y las operaciones de la pantalla «Consolidado del periodo»
// (contratos/generacion, F10-H02).
import { useQuery } from '@tanstack/react-query';
import { apiGeneracion, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/generacion';
import { FalloDeApi } from '../../errores/traducir';
import { guardar, nombreDelServidor } from '../plantillas/consultas';

export type ConsolidadoDelPeriodo = components['schemas']['ConsolidadoDelPeriodo'];
export type CarreraConsolidada = components['schemas']['CarreraConsolidada'];
export type InformeSustentado = components['schemas']['InformeSustentado'];

/** El tamaño máximo del informe, el mismo del servidor (D-88). */
export const MAXIMO_DEL_INFORME = 10 * 1024 * 1024;

/** El mismo texto de GENERACION_INFORME_DEMASIADO_GRANDE, para no subir en vano. */
export const INFORME_DEMASIADO_GRANDE = {
  titulo: 'Informe demasiado grande',
  descripcion: 'El informe pasa de 10 MB. Reduce su tamaño y vuelve a adjuntarlo.',
  detalles: [],
};

/** La clave del consolidado de todos los periodos, para recargarlo después de un cambio. */
export const CLAVE_CONSOLIDADO = ['generacion', 'consolidado'] as const;

/** Se calcula al abrirlo: no guarda en caché; sin periodo, no consulta. */
export function useConsolidado(periodo: string | undefined) {
  return useQuery({
    queryKey: [...CLAVE_CONSOLIDADO, periodo],
    queryFn: () =>
      llamar(
        apiGeneracion.GET('/api/generacion/periodos/{periodo}/consolidado', {
          params: { path: { periodo: periodo ?? '' } },
        }),
      ),
    enabled: Boolean(periodo),
    gcTime: 0,
  });
}

export function consolidar(periodo: string) {
  return llamar(
    apiGeneracion.POST('/api/generacion/periodos/{periodo}/consolidado', {
      params: { path: { periodo } },
    }),
  );
}

export function adjuntarInforme(periodo: string, archivo: File) {
  return llamar(
    apiGeneracion.PUT('/api/generacion/periodos/{periodo}/consolidado/informe', {
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

/** Pide el PDF con el token, que está solo en memoria (D-51), y lo guarda. */
export async function descargarInforme(periodo: string): Promise<void> {
  const { data, error, response } = await apiGeneracion.GET(
    '/api/generacion/periodos/{periodo}/consolidado/informe',
    { params: { path: { periodo } }, parseAs: 'blob' },
  );
  if (!response.ok || !data) {
    throw new FalloDeApi(response.status, error);
  }
  guardar(data, nombreDelServidor(response) ?? `informe-sustentado-${periodo}.pdf`);
}
