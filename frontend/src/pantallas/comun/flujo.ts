// El flujo del distributivo (contratos/validacionocs, etiqueta Aprobacion, F12-H02): la etapa
// actual, el envío de cada parcial y lo que puede hacer el usuario.
import { useQuery } from '@tanstack/react-query';
import { apiValidacionocs, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/validacionocs';
import { FalloDeApi } from '../../errores/traducir';
import { guardar, nombreDelServidor } from '../plantillas/consultas';

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

/**
 * Envía el consolidado a revisión (F10-H03, D-97). El servidor comprueba las condiciones al
 * enviar: lo que vio la pantalla puede haber cambiado.
 */
export function enviarARevision(periodo: string) {
  return llamar(
    apiValidacionocs.POST('/api/validacionocs/periodos/{periodo}/revision', {
      params: { path: { periodo } },
    }),
  );
}

export type ParcialADevolver = components['schemas']['ParcialADevolver'];

/**
 * Devuelve el parcial de una o varias carreras, cada una con su observación (F12-H05, D-98). El
 * distributivo vuelve a la propuesta de carrera; el servidor exige el rol y la observación.
 */
export function devolverParciales(periodo: string, devoluciones: ParcialADevolver[]) {
  return llamar(
    apiValidacionocs.POST('/api/validacionocs/periodos/{periodo}/devolucion', {
      params: { path: { periodo } },
      body: { devoluciones },
    }),
  );
}

export type CongelacionDeLaVersion = components['schemas']['CongelacionDeLaVersion'];
export type RechazoEnFirmaDelFlujo = components['schemas']['RechazoEnFirmaDelFlujo'];

/**
 * Aprueba el contenido y congela la versión vigente (F13-H01, D-100). Sin cuerpo: no lleva
 * comentario. El servidor exige el rol de la etapa.
 */
export function aprobarContenido(periodo: string) {
  return llamar(
    apiValidacionocs.POST('/api/validacionocs/periodos/{periodo}/aprobacion', {
      params: { path: { periodo } },
    }),
  );
}

export type ResolucionDelOcs = components['schemas']['ResolucionDelOcs'];

/**
 * Registra la resolución del OCS y aprueba el distributivo (F15-H01, D-107): número, fecha
 * (AAAA-MM-DD) y el PDF. El servidor exige el rol de la etapa y comprueba todo.
 */
export function registrarResolucion(periodo: string, numero: string, fecha: string, archivo: File) {
  return llamar(
    apiValidacionocs.POST('/api/validacionocs/periodos/{periodo}/resolucion', {
      params: { path: { periodo } },
      // El contrato tipa la parte binaria como texto; va el archivo tal cual, en un FormData.
      body: { numero, fecha, archivo: archivo as unknown as string },
      bodySerializer: (cuerpo: { numero: string; fecha: string; archivo: string }) => {
        const formulario = new FormData();
        formulario.append('numero', cuerpo.numero);
        formulario.append('fecha', cuerpo.fecha);
        // Con el nombre explícito: es el que se guarda y se muestra en la constancia.
        formulario.append('archivo', cuerpo.archivo as unknown as File, archivo.name);
        return formulario;
      },
    }),
  );
}

/** Pide el PDF de la resolución con el token, que está solo en memoria (D-51), y lo guarda. */
export async function descargarResolucion(periodo: string): Promise<void> {
  const { data, error, response } = await apiValidacionocs.GET(
    '/api/validacionocs/periodos/{periodo}/resolucion/archivo',
    { params: { path: { periodo } }, parseAs: 'blob' },
  );
  if (!response.ok || !data) {
    throw new FalloDeApi(response.status, error);
  }
  guardar(data, nombreDelServidor(response) ?? `resolucion-${periodo}.pdf`);
}
