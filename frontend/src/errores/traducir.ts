// El único lugar de la SPA que convierte un fallo en lo que ve el usuario (FR-010 a FR-012,
// ADR-019): el titulo, la descripcion, los detalles y el idIncidente del modelo común, nunca el
// codigo, el estado HTTP ni el texto de una excepción. Es la contraparte del advice único del
// backend (ADR-016).
import { z } from 'zod';
import type { components } from '../api/generado/usuarios';
import { MENSAJE_ERROR_INTERNO, MENSAJE_SERVICIO_NO_DISPONIBLE } from './mensajes';

type ErrorDelContrato = components['schemas']['Error'];
type DetalleDelContrato = components['schemas']['DetalleError'];

/** Lo que la SPA muestra de un error (data-model «Mensaje de error»). */
export interface MensajeDeError {
  titulo: string;
  descripcion: string;
  detalles: DetalleDelContrato[];
  idIncidente?: string;
}

/** Una respuesta de la API que no fue 2xx, con su cuerpo tal como llegó. */
export class FalloDeApi extends Error {
  constructor(
    readonly estado: number,
    readonly cuerpo: unknown,
  ) {
    super('La API respondió con un error');
    this.name = 'FalloDeApi';
  }
}

// El esquema Error de contratos/comun/errores.yaml; si el contrato cambia, el tipo lo detecta.
const esquemaError = z.object({
  titulo: z.string().min(1),
  descripcion: z.string().min(1),
  codigo: z.string().min(1),
  detalles: z.array(z.object({ campo: z.string(), descripcion: z.string() })).optional(),
  idIncidente: z.string().optional(),
}) satisfies z.ZodType<ErrorDelContrato>;

/**
 * Convierte cualquier fallo en el mensaje para el usuario.
 *
 * @param fallo un {@link FalloDeApi}, el `TypeError` de un fallo de red u otra excepción
 */
export function traducirError(fallo: unknown): MensajeDeError {
  if (fallo instanceof FalloDeApi) {
    const error = esquemaError.safeParse(fallo.cuerpo);
    if (error.success) {
      const { titulo, descripcion, detalles, idIncidente } = error.data;
      return {
        titulo,
        descripcion,
        detalles: detalles ?? [],
        ...(idIncidente ? { idIncidente } : {}),
      };
    }
    return { ...MENSAJE_ERROR_INTERNO, detalles: [] };
  }
  if (fallo instanceof TypeError) {
    return { ...MENSAJE_SERVICIO_NO_DISPONIBLE, detalles: [] };
  }
  return { ...MENSAJE_ERROR_INTERNO, detalles: [] };
}
