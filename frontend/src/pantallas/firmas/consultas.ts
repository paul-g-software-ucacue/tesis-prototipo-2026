// Las firmas pendientes del usuario y la descarga del archivo vigente (contratos/firma, F14-H01,
// D-102). El servidor decide quién está en turno; la pantalla solo lo refleja.
import { useQuery } from '@tanstack/react-query';
import { apiFirma, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/firma';
import { FalloDeApi } from '../../errores/traducir';
import { guardar, nombreDelServidor } from '../plantillas/consultas';

export type FirmaPendiente = components['schemas']['FirmaPendiente'];

export const CLAVE_PENDIENTES = ['firma', 'pendientes'] as const;

/** Los documentos que le toca firmar al usuario. */
export function useFirmasPendientes() {
  return useQuery({
    queryKey: CLAVE_PENDIENTES,
    queryFn: () => llamar(apiFirma.GET('/api/firma/pendientes')),
  });
}

/**
 * Pide el PDF vigente con el token, que está solo en memoria (D-51), y lo guarda con el nombre que
 * manda el servidor en Content-Disposition.
 */
export async function descargarArchivoVigente(pendiente: FirmaPendiente): Promise<void> {
  const { data, error, response } = await apiFirma.GET(
    '/api/firma/documentos/{documento}/archivo',
    { params: { path: { documento: pendiente.documento } }, parseAs: 'blob' },
  );
  if (!response.ok || !data) {
    throw new FalloDeApi(response.status, error);
  }
  guardar(
    data,
    nombreDelServidor(response) ??
      `distributivo-${pendiente.periodo}-v${String(pendiente.version)}.pdf`,
  );
}
