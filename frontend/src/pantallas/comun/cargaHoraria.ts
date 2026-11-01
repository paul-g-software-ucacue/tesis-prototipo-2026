// La carga horaria de un docente (F07-H01, RF-27): sus totales en todas las carreras y unidades, y
// su proporción. Se calcula en el servidor; aquí solo se pide y se nombra.
import { useQuery } from '@tanstack/react-query';
import { apiGeneracion, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/generacion';

export type CargaHoraria = components['schemas']['CargaHoraria'];
export type HorasPorComponente = components['schemas']['HorasPorComponente'];

/** La clave de las cargas: guardar y quitar la invalidan, sea cual sea el docente a la vista. */
export const CLAVE_CARGA_HORARIA = ['generacion', 'carga-horaria'] as const;

/** Las horas de cada componente, con su nombre, en el orden de RF-27. */
export function porComponente(totales: HorasPorComponente) {
  return [
    ['Clase', totales.clase],
    ['Otras actividades de docencia', totales.otrasDocencia],
    ['Investigación', totales.investigacion],
    ['Vinculación', totales.vinculacion],
    ['Gestión', totales.gestion],
  ] as const;
}

/** La carga de un docente; sin docente, no consulta. */
export function useCargaHoraria(periodo: string, docente: string) {
  return useQuery({
    queryKey: [...CLAVE_CARGA_HORARIA, periodo, docente],
    queryFn: () =>
      llamar(
        apiGeneracion.GET('/api/generacion/periodos/{periodo}/docentes/{docente}/carga-horaria', {
          params: { path: { periodo, docente } },
        }),
      ),
    enabled: Boolean(docente),
  });
}
