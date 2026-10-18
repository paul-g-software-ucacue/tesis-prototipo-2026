// Los tipos y las consultas de la pantalla «Normativa» (contratos/normativa).
import { useQuery } from '@tanstack/react-query';
import { apiNormativa, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/normativa';

export type VersionResumen = components['schemas']['VersionResumen'];
export type VersionNormativa = components['schemas']['VersionNormativa'];
export type FilaCargaHoraria = components['schemas']['FilaCargaHoraria'];

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
