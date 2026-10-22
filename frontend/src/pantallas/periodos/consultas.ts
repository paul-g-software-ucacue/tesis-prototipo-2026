// Los tipos, las consultas y las operaciones de la pantalla «Periodos» (contratos/academico).
import { useQuery } from '@tanstack/react-query';
import { apiAcademico, apiRoles, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/academico';
import type { components as deRoles } from '../../api/generado/roles';

export type PeriodoAcademico = components['schemas']['PeriodoAcademico'];
export type PeriodoNuevo = components['schemas']['PeriodoNuevo'];
export type DatosDelPeriodo = components['schemas']['DatosDelPeriodo'];
export type EstadoPeriodo = components['schemas']['EstadoPeriodo'];
export type Responsable = deRoles['schemas']['Responsable'];

/** La clave de las consultas de Académico, para invalidarlas después de un cambio. */
export const CLAVE_PERIODOS = ['academico', 'periodos'] as const;

/** La clave de los Responsables de consolidación, de Roles (F05-H03). */
export const CLAVE_RESPONSABLES = ['roles', 'responsables'] as const;

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

/** El Responsable de consolidación de cada periodo que tiene uno (F05-H03). */
export function useResponsables() {
  return useQuery({
    queryKey: CLAVE_RESPONSABLES,
    queryFn: () => llamar(apiRoles.GET('/api/roles/responsables')),
  });
}

/** Los usuarios con el rol Docente: los que se pueden designar. */
export function useDocentes() {
  return useQuery({
    queryKey: ['roles', 'docentes'],
    queryFn: () => llamar(apiRoles.GET('/api/roles/docentes')),
  });
}

/** Designa al Responsable de consolidación del periodo, o lo reemplaza (D-13, ADR-003). */
export function designarResponsable(periodo: string, usuario: string) {
  return llamar(
    apiRoles.PUT('/api/roles/responsables/{periodo}', {
      params: { path: { periodo } },
      body: { usuario },
    }),
  );
}
