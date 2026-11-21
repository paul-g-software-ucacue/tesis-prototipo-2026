// Los reportes de la versión publicada vigente (contratos/reporting, F20-H01, D-115): los periodos
// publicados, sus docentes, el reporte individual en pantalla y sus descargas en PDF y CSV. El
// servidor decide cuál versión es la publicada; la pantalla solo la muestra.
import { useQuery } from '@tanstack/react-query';
import { apiReporting, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/reporting';
import { FalloDeApi } from '../../errores/traducir';
import { guardar, nombreDelServidor } from '../plantillas/consultas';

export type Publicacion = components['schemas']['Publicacion'];
export type DocentesDelPeriodo = components['schemas']['DocentesDelPeriodo'];
export type ReporteIndividual = components['schemas']['ReporteIndividual'];
export type Formato = 'pdf' | 'csv';

/** Los periodos con versión publicada vigente, de la publicación más reciente a la más antigua. */
export function usePublicaciones() {
  return useQuery({
    queryKey: ['reporting', 'publicaciones'],
    queryFn: () => llamar(apiReporting.GET('/api/reporting/publicaciones')),
  });
}

/** La publicación vigente de un periodo y sus docentes. */
export function useDocentesDelPeriodo(periodo: string | null) {
  return useQuery({
    queryKey: ['reporting', 'docentes', periodo],
    enabled: periodo !== null,
    queryFn: () =>
      llamar(
        apiReporting.GET('/api/reporting/periodos/{periodo}/docentes', {
          params: { path: { periodo: periodo ?? '' } },
        }),
      ),
  });
}

/** El reporte individual de un docente, para la pantalla: no deja entrada en la bitácora. */
export function useReporteIndividual(periodo: string | null, identificacion: string | null) {
  return useQuery({
    queryKey: ['reporting', 'reporte', periodo, identificacion],
    enabled: periodo !== null && identificacion !== null,
    queryFn: () =>
      llamar(
        apiReporting.GET('/api/reporting/periodos/{periodo}/docentes/{identificacion}/reporte', {
          params: { path: { periodo: periodo ?? '', identificacion: identificacion ?? '' } },
        }),
      ),
  });
}

/**
 * Pide el reporte en PDF o en CSV con el token, que está solo en memoria (D-51), y lo guarda con
 * el nombre que manda el servidor en Content-Disposition. Cada descarga queda en la bitácora.
 */
export async function descargarReporte(
  periodo: string,
  identificacion: string,
  formato: Formato,
): Promise<void> {
  const opciones = {
    params: { path: { periodo, identificacion } },
    parseAs: 'blob' as const,
  };
  const { data, error, response } =
    formato === 'pdf'
      ? await apiReporting.GET(
          '/api/reporting/periodos/{periodo}/docentes/{identificacion}/reporte.pdf',
          opciones,
        )
      : await apiReporting.GET(
          '/api/reporting/periodos/{periodo}/docentes/{identificacion}/reporte.csv',
          opciones,
        );
  if (!response.ok || !data) {
    throw new FalloDeApi(response.status, error);
  }
  guardar(
    data,
    nombreDelServidor(response) ?? `reporte-individual-${periodo}-${identificacion}.${formato}`,
  );
}

const HORAS = new Intl.NumberFormat('es-EC', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/** Las horas con dos decimales y coma, como el PDF y el CSV. */
export function horas(valor: number) {
  return HORAS.format(valor);
}

/** El nombre de cada componente, en el orden del glosario. */
export const COMPONENTES = [
  ['clase', 'Clase'],
  ['otrasDocencia', 'Otras actividades de docencia'],
  ['investigacion', 'Investigación'],
  ['vinculacion', 'Vinculación'],
  ['gestion', 'Gestión'],
] as const;

/** El nombre del componente de una actividad no lectiva. */
export const NOMBRE_DEL_COMPONENTE: Record<string, string> = {
  OTRAS_DOCENCIA: 'Otras actividades de docencia',
  INVESTIGACION: 'Investigación',
  VINCULACION: 'Vinculación',
  GESTION: 'Gestión',
};

/** El nombre de la dedicación (RN-01). */
export const NOMBRE_DE_LA_DEDICACION: Record<string, string> = {
  TC: 'Tiempo completo',
  MT: 'Medio tiempo',
  TP: 'Tiempo parcial',
};
