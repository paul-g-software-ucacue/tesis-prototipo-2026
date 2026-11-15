// Las firmas pendientes del usuario, la descarga del archivo vigente (contratos/firma, F14-H01,
// D-102), la subida del PDF firmado (F14-H02, D-103) y el rechazo en firma (F14-H04, D-105). El
// servidor decide quién está en turno, si la firma se acepta y si el rechazo cabe; la pantalla solo
// lo refleja.
import { useQuery } from '@tanstack/react-query';
import { apiFirma, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/firma';
import { FalloDeApi } from '../../errores/traducir';
import { guardar, nombreDelServidor } from '../plantillas/consultas';

export type FirmaPendiente = components['schemas']['FirmaPendiente'];
export type ResultadoValidacion = components['schemas']['ResultadoValidacion'];
export type RechazoEnFirma = components['schemas']['RechazoEnFirma'];

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

/**
 * Sube el PDF firmado del paso en turno. El servidor responde 201 con el resultado si lo acepta, o
 * 422 con las cuatro comprobaciones en los detalles si lo rechaza.
 */
export function subirFirma(documento: string, archivo: File): Promise<ResultadoValidacion> {
  return llamar(
    apiFirma.POST('/api/firma/documentos/{documento}/firmas', {
      params: { path: { documento } },
      // El contrato tipa la parte binaria como texto; va el archivo tal cual, en un FormData.
      body: { archivo: archivo as unknown as string },
      bodySerializer: (cuerpo: { archivo: string }) => {
        const formulario = new FormData();
        formulario.append('archivo', cuerpo.archivo as unknown as File);
        return formulario;
      },
    }),
  );
}

/**
 * Rechaza en firma el documento con la observación, sin los espacios de los extremos. El servidor
 * responde 201 con el rechazo, 400 si la observación no cabe o 403 si quien rechaza ya no está en
 * turno.
 */
export function rechazarEnFirma(documento: string, observacion: string): Promise<RechazoEnFirma> {
  return llamar(
    apiFirma.POST('/api/firma/documentos/{documento}/rechazos', {
      params: { path: { documento } },
      body: { observacion: observacion.trim() },
    }),
  );
}
