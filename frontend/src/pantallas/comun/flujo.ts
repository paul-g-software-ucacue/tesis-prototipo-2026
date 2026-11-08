// El flujo del distributivo (contratos/validacionocs, etiqueta Aprobacion, F12-H02): la etapa
// actual, el envío de cada parcial y lo que puede hacer el usuario.
import { useQuery } from '@tanstack/react-query';
import { apiValidacionocs, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/validacionocs';

export type EstadoDelFlujo = components['schemas']['EstadoDelFlujo'];
export type ParcialDelFlujo = components['schemas']['ParcialDelFlujo'];

/** La clave del flujo, para recargarlo después de enviar o avanzar. */
export const CLAVE_FLUJO = ['validacionocs', 'flujo'] as const;

/** El flujo del periodo; sin periodo, no consulta. */
export function useFlujo(periodo: string | undefined) {
  return useQuery({
    queryKey: [...CLAVE_FLUJO, periodo],
    queryFn: () =>
      llamar(
        apiValidacionocs.GET('/api/validacionocs/periodos/{periodo}/flujo', {
          params: { path: { periodo: periodo ?? '' } },
        }),
      ),
    enabled: Boolean(periodo),
  });
}

/** Envía el parcial de una carrera; con el último, el distributivo pasa de etapa. */
export function enviarParcial(periodo: string, carrera: string) {
  return llamar(
    apiValidacionocs.POST('/api/validacionocs/periodos/{periodo}/parciales/{carrera}/envio', {
      params: { path: { periodo, carrera } },
    }),
  );
}

/** El avance genérico de una etapa sin operación propia (BORRADOR_UNIDADES). */
export function avanzarEtapa(periodo: string) {
  return llamar(
    apiValidacionocs.POST('/api/validacionocs/periodos/{periodo}/avance', {
      params: { path: { periodo } },
    }),
  );
}
