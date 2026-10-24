// Lo común de los formularios de los datos maestros (F03-H02, F03-H03): qué falta antes de enviar,
// el texto bajo cada campo con el motivo del servidor antes de la pista, y las listas desplegables.
import type { MensajeDeError } from '../../errores/traducir';

export const SELECCION = { select: { native: true }, inputLabel: { shrink: true } } as const;

/** Los tipos de unidad que pueden ser la sede de una carrera (carga 02). */
export const TIPOS_DE_SEDE = new Set(['SEDE_MATRIZ', 'SEDE', 'EXTENSION', 'CAMPUS']);

export function detalleDe(aviso: MensajeDeError | null, campo: string) {
  return aviso?.detalles.find((detalle) => detalle.campo === campo)?.descripcion;
}

/**
 * El texto bajo un campo: la pista y, si el servidor lo rechazó, su motivo antes de ella, para que
 * se sepa qué corregir (F03-H02).
 */
export function ayuda(aviso: MensajeDeError | null, campo: string, pista?: string) {
  const motivo = detalleDe(aviso, campo);
  if (!motivo) {
    return pista;
  }
  const frase = motivo.charAt(0).toUpperCase() + motivo.slice(1);
  const conPunto = /[.!?]$/.test(frase) ? frase : `${frase}.`;
  return pista ? `${conPunto} ${pista}` : conPunto;
}

/** Lo que falta para enviar, por su nombre en pantalla, o nada. */
export function queFalta(campos: readonly [string, boolean][]) {
  const faltan = campos.filter(([, lleno]) => !lleno).map(([nombre]) => nombre);
  return faltan.length ? `Falta: ${faltan.join(', ')}.` : null;
}

/** Un número escrito en un campo, o nada si está vacío. */
export function numero(texto: string): number | undefined {
  return texto.trim() ? Number(texto.replace(',', '.')) : undefined;
}
