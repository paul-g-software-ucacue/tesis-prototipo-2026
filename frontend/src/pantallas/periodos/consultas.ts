// Los tipos, las consultas y las operaciones de la pantalla «Periodos» (contratos/academico).
import { useQuery } from '@tanstack/react-query';
import { apiAcademico, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/academico';

export type PeriodoAcademico = components['schemas']['PeriodoAcademico'];
export type PeriodoNuevo = components['schemas']['PeriodoNuevo'];
export type DatosDelPeriodo = components['schemas']['DatosDelPeriodo'];
export type EstadoPeriodo = components['schemas']['EstadoPeriodo'];

/** La clave de las consultas de Académico, para invalidarlas después de un cambio. */
export const CLAVE_PERIODOS = ['academico', 'periodos'] as const;

/** Los periodos, del de inicio más reciente al más antiguo. */
export function usePeriodos() {
  return useQuery({
    queryKey: CLAVE_PERIODOS,
    queryFn: () => llamar(apiAcademico.GET('/api/academico/periodos')),
  });
}

/** Registra un periodo, en Planificación y sin versión. */
export function registrarPeriodo(cuerpo: PeriodoNuevo) {
  return llamar(apiAcademico.POST('/api/academico/periodos', { body: cuerpo }));
}

/** Corrige el nombre y las fechas; el código no cambia. */
export function corregirPeriodo(codigo: string, cuerpo: DatosDelPeriodo) {
  return llamar(
    apiAcademico.PUT('/api/academico/periodos/{codigo}', {
      params: { path: { codigo } },
      body: cuerpo,
    }),
  );
}

/** Abre el periodo con su versión de normativa; desde entonces la versión no cambia (D-69). */
export function abrirPeriodo(codigo: string, version: number) {
  return llamar(
    apiAcademico.PUT('/api/academico/periodos/{codigo}/normativa', {
      params: { path: { codigo } },
      body: { version },
    }),
  );
}

/** Pasa el periodo al estado de destino, que tiene que ser el siguiente. */
export function cambiarEstadoPeriodo(codigo: string, estado: EstadoPeriodo) {
  return llamar(
    apiAcademico.PUT('/api/academico/periodos/{codigo}/estado', {
      params: { path: { codigo } },
      body: { estado },
    }),
  );
}
