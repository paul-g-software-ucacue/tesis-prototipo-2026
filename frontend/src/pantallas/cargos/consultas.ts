// Los tipos, las consultas y las operaciones de la pantalla «Cargos» (contratos/docentes, tag
// Cargos, F03-H05).
import { useQuery } from '@tanstack/react-query';
import { apiDocentes, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/docentes';

export type Cargo = components['schemas']['Cargo'];
export type CargoNuevo = components['schemas']['CargoNuevo'];
export type DatosDeCargo = components['schemas']['DatosDeCargo'];
export type TipoDeCargo = components['schemas']['TipoDeCargo'];
export type EstadoDeCargo = components['schemas']['EstadoDeCargo'];
export type UnidadDeReferencia = components['schemas']['UnidadDeReferencia'];

/** La clave de los cargos, para recargarlos después de un cambio. */
export const CLAVE_CARGOS = ['cargos'] as const;

/** Los cargos por docente, cargo y fecha de designación; un criterio vacío no filtra. */
export function useCargos(estado: EstadoDeCargo | '', docente: string) {
  return useQuery({
    queryKey: [...CLAVE_CARGOS, estado, docente],
    queryFn: () =>
      llamar(
        apiDocentes.GET('/api/docentes/cargos', {
          params: { query: { ...(estado && { estado }), ...(docente && { docente }) } },
        }),
      ),
  });
}

/** Registra un cargo; la respuesta trae sus advertencias, como la de RN-20. */
export function registrarCargo(cuerpo: CargoNuevo) {
  return llamar(apiDocentes.POST('/api/docentes/cargos', { body: cuerpo }));
}

/** Corrige o finaliza un cargo; su docente, su cargo y su fecha de designación no cambian. */
export function corregirCargo(cargo: Cargo, cuerpo: DatosDeCargo) {
  return llamar(
    apiDocentes.PUT('/api/docentes/cargos/{docente}/{tipo}/{fechaDesignacion}', {
      params: {
        path: {
          docente: cargo.docente,
          tipo: cargo.tipo,
          fechaDesignacion: cargo.fechaDesignacion,
        },
      },
      body: cuerpo,
    }),
  );
}
