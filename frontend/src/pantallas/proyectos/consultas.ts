// Los tipos, las consultas y las operaciones de la pantalla «Proyectos» (contratos/docentes, tag
// Proyectos, F03-H05, RF-18).
import { useQuery } from '@tanstack/react-query';
import { apiDocentes, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/docentes';

export type Proyecto = components['schemas']['Proyecto'];
export type ProyectoNuevo = components['schemas']['ProyectoNuevo'];
export type DatosDeProyecto = components['schemas']['DatosDeProyecto'];
export type TipoDeProyecto = components['schemas']['TipoDeProyecto'];
export type EstadoDeProyecto = components['schemas']['EstadoDeProyecto'];
export type RolDeParticipante = components['schemas']['RolDeParticipante'];
export type MotivoNoAsignable = components['schemas']['MotivoNoAsignable'];
export type PeriodoDeReferencia = components['schemas']['PeriodoDeReferencia'];

/** La clave de los proyectos, para recargarlos después de un cambio. */
export const CLAVE_PROYECTOS = ['proyectos'] as const;

/** Los proyectos por código y, con un periodo, si cada uno es asignable en él. */
export function useProyectos(periodo: string) {
  return useQuery({
    queryKey: [...CLAVE_PROYECTOS, periodo],
    queryFn: () =>
      llamar(
        apiDocentes.GET('/api/docentes/proyectos', {
          params: { query: { ...(periodo && { periodo }) } },
        }),
      ),
  });
}

/** Registra un proyecto aprobado por el OCS. */
export function registrarProyecto(cuerpo: ProyectoNuevo) {
  return llamar(apiDocentes.POST('/api/docentes/proyectos', { body: cuerpo }));
}

/** Corrige un proyecto o cambia su estado; su código no cambia. */
export function corregirProyecto(codigo: string, cuerpo: DatosDeProyecto) {
  return llamar(
    apiDocentes.PUT('/api/docentes/proyectos/{codigo}', {
      params: { path: { codigo } },
      body: cuerpo,
    }),
  );
}
