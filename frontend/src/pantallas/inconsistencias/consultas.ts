// Los tipos, las consultas y las operaciones de la pantalla «Panel de inconsistencias»
// (contratos/generacion, F10-H01).
import { useQuery } from '@tanstack/react-query';
import { apiGeneracion, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/generacion';

export type PanelDeInconsistencias = components['schemas']['PanelDeInconsistencias'];
export type InconsistenciaDelPanel = components['schemas']['InconsistenciaDelPanel'];
export type JustificacionNueva = components['schemas']['JustificacionNueva'];

/** La clave del panel de todos los periodos, para recargarlo después de justificar. */
export const CLAVE_PANEL = ['generacion', 'inconsistencias'] as const;

/** El panel se evalúa al abrirlo: no guarda en caché; sin periodo, no consulta. */
export function usePanel(periodo: string | undefined) {
  return useQuery({
    queryKey: [...CLAVE_PANEL, periodo],
    queryFn: () =>
      llamar(
        apiGeneracion.GET('/api/generacion/periodos/{periodo}/inconsistencias', {
          params: { path: { periodo: periodo ?? '' } },
        }),
      ),
    enabled: Boolean(periodo),
    gcTime: 0,
  });
}

export function justificarInconsistencia(periodo: string, cuerpo: JustificacionNueva) {
  return llamar(
    apiGeneracion.PUT('/api/generacion/periodos/{periodo}/inconsistencias/justificacion', {
      params: { path: { periodo } },
      body: cuerpo,
    }),
  );
}
