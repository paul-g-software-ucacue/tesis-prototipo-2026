// Los tipos, las consultas y las operaciones de la pantalla «Normativa» (contratos/normativa).
import { useQuery } from '@tanstack/react-query';
import { apiNormativa, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/normativa';

export type VersionResumen = components['schemas']['VersionResumen'];
export type VersionNormativa = components['schemas']['VersionNormativa'];
export type FilaCargaHoraria = components['schemas']['FilaCargaHoraria'];
export type VersionNueva = components['schemas']['VersionNueva'];
export type VersionEditada = components['schemas']['VersionEditada'];
export type FilaEditada = components['schemas']['FilaEditada'];

/** La clave de todas las consultas de Normativa, para invalidarlas después de un cambio. */
export const CLAVE_NORMATIVA = ['normativa'] as const;

/** Las versiones de normativa, por número. */
export function useVersiones() {
  return useQuery({
    queryKey: ['normativa', 'versiones'],
    queryFn: () => llamar(apiNormativa.GET('/api/normativa/versiones')),
  });
}

/** Una versión de normativa completa; no pide nada hasta saber cuál. */
export function useVersion(numero: number | undefined) {
  return useQuery({
    queryKey: ['normativa', 'versiones', numero],
    enabled: numero !== undefined,
    queryFn: () =>
      llamar(
        apiNormativa.GET('/api/normativa/versiones/{numero}', {
          params: { path: { numero: numero ?? 1 } },
        }),
      ),
  });
}

/** Crea una versión copiando otra (F04-H02). */
export function crearVersion(cuerpo: VersionNueva) {
  return llamar(apiNormativa.POST('/api/normativa/versiones', { body: cuerpo }));
}

/** Edita una versión sin periodo con la versión completa: la única forma de cambiarla (FR-010). */
export function editarVersion(numero: number, cuerpo: VersionEditada) {
  return llamar(
    apiNormativa.PUT('/api/normativa/versiones/{numero}', {
      params: { path: { numero } },
      body: cuerpo,
    }),
  );
}

/** Asocia una versión a un periodo; desde entonces no cambia. */
export function asociarPeriodo(periodo: string, version: number) {
  return llamar(
    apiNormativa.PUT('/api/normativa/periodos/{periodo}', {
      params: { path: { periodo } },
      body: { version },
    }),
  );
}
