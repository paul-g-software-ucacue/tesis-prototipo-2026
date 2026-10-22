// Los tipos, las consultas y las operaciones de la pantalla «Estructura orgánica»
// (contratos/academico, F03-H01).
import { useQuery } from '@tanstack/react-query';
import { apiAcademico, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/academico';

export type UnidadOrganica = components['schemas']['UnidadOrganica'];
export type UnidadNueva = components['schemas']['UnidadNueva'];
export type DatosDeUnidad = components['schemas']['DatosDeUnidad'];
export type TipoDeUnidad = components['schemas']['TipoDeUnidad'];
export type GestionAutorizada = components['schemas']['GestionAutorizada'];

/** La clave de la estructura, para recargarla después de un cambio. */
export const CLAVE_ESTRUCTURA = ['academico', 'unidades'] as const;

/** Todas las unidades, activas e inactivas, por código. */
export function useUnidades() {
  return useQuery({
    queryKey: CLAVE_ESTRUCTURA,
    queryFn: () => llamar(apiAcademico.GET('/api/academico/unidades')),
  });
}

/** Las 21 gestiones autorizadas de POL 6; el catálogo no cambia. */
export function useGestionesAutorizadas() {
  return useQuery({
    queryKey: ['academico', 'gestiones-autorizadas'],
    queryFn: () => llamar(apiAcademico.GET('/api/academico/gestiones-autorizadas')),
    staleTime: Infinity,
  });
}

/** Registra una unidad activa; su código y su tipo no cambian después. */
export function registrarUnidad(cuerpo: UnidadNueva) {
  return llamar(apiAcademico.POST('/api/academico/unidades', { body: cuerpo }));
}

/** Corrige el nombre, el padre y los datos de su tipo. */
export function corregirUnidad(codigo: string, cuerpo: DatosDeUnidad) {
  return llamar(
    apiAcademico.PUT('/api/academico/unidades/{codigo}', {
      params: { path: { codigo } },
      body: cuerpo,
    }),
  );
}

/** Inactiva o reactiva la unidad; ninguna se borra. */
export function cambiarEstadoUnidad(codigo: string, activa: boolean) {
  return llamar(
    apiAcademico.PUT('/api/academico/unidades/{codigo}/estado', {
      params: { path: { codigo } },
      body: { activa },
    }),
  );
}
