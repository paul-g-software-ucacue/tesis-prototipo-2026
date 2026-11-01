// Los tipos, las consultas y las operaciones de la pantalla «Actividades no lectivas»
// (contratos/generacion, F06-H04). Lo que exige cada actividad lo decide el servidor: aquí solo se
// muestra lo que responde.
import { useQuery } from '@tanstack/react-query';
import { apiGeneracion, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/generacion';

export type ActividadesDelPeriodo = components['schemas']['ActividadesDelPeriodo'];
export type ActividadesDeCarrera = components['schemas']['ActividadesDeCarrera'];
export type ActividadesDelDocente = components['schemas']['ActividadesDelDocente'];
export type ActividadAsignada = components['schemas']['ActividadAsignada'];
export type ActividadPosible = components['schemas']['ActividadPosible'];
export type ActividadesPosibles = components['schemas']['ActividadesPosibles'];
export type ActividadNueva = components['schemas']['ActividadNueva'];
export type Componente = components['schemas']['Componente'];

/** Cómo se llama cada componente en pantalla, en su orden. */
export const NOMBRE_DEL_COMPONENTE: Record<Componente, string> = {
  OTRAS_DOCENCIA: 'Otras actividades de docencia',
  INVESTIGACION: 'Investigación',
  VINCULACION: 'Vinculación',
  GESTION: 'Gestión',
};

/** El artículo y el literal como se leen en la norma: 140-b es «140 b)». */
export function literal(codigo: string) {
  return `${codigo.replace('-', ' ')})`;
}

/** La clave de las actividades de todos los periodos, para recargarlas después de un cambio. */
export const CLAVE_ACTIVIDADES = ['generacion', 'actividades'] as const;

const RUTA_ACTIVIDAD =
  '/api/generacion/periodos/{periodo}/parciales/{carrera}/docentes/{docente}/actividades/{actividad}';

/** Las actividades del periodo, por carrera y docente; sin periodo, no consulta. */
export function useActividades(periodo: string | undefined) {
  return useQuery({
    queryKey: [...CLAVE_ACTIVIDADES, periodo],
    queryFn: () =>
      llamar(
        apiGeneracion.GET('/api/generacion/periodos/{periodo}/actividades', {
          params: { path: { periodo: periodo ?? '' } },
        }),
      ),
    enabled: Boolean(periodo),
  });
}

/** El catálogo frente a un docente; sin docente, no consulta. Siempre refleja lo de ese momento. */
export function useActividadesPosibles(periodo: string, docente: string) {
  return useQuery({
    queryKey: ['generacion', 'actividades-posibles', periodo, docente],
    queryFn: () =>
      llamar(
        apiGeneracion.GET(
          '/api/generacion/periodos/{periodo}/docentes/{docente}/actividades-posibles',
          { params: { path: { periodo, docente } } },
        ),
      ),
    enabled: Boolean(docente),
    gcTime: 0,
  });
}

/** Lo que identifica a una actividad en un parcial. */
export interface LlaveDeActividad {
  periodo: string;
  carrera: string;
  docente: string;
  actividad: string;
}

/** Asigna la actividad, o reemplaza sus horas. */
export function asignarActividad(llave: LlaveDeActividad, cuerpo: ActividadNueva) {
  return llamar(
    apiGeneracion.PUT(RUTA_ACTIVIDAD, { params: { path: { ...llave } }, body: cuerpo }),
  );
}

/** Quita la actividad del parcial. */
export function quitarActividad(llave: LlaveDeActividad, proyecto?: string) {
  return llamar(
    apiGeneracion.DELETE(RUTA_ACTIVIDAD, {
      params: { path: { ...llave }, query: proyecto ? { proyecto } : {} },
    }),
  );
}
