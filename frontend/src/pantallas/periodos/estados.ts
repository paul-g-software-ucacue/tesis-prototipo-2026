// Los estados del periodo en español y su orden (RF-13): solo avanzan, un paso a la vez.
import type { EstadoPeriodo } from './consultas';

export const NOMBRE_DEL_ESTADO: Record<EstadoPeriodo, string> = {
  PLANIFICACION: 'Planificación',
  EN_APROBACION: 'En aprobación',
  VIGENTE: 'Vigente',
  CERRADO: 'Cerrado',
};

const SIGUIENTE: Record<EstadoPeriodo, EstadoPeriodo | undefined> = {
  PLANIFICACION: 'EN_APROBACION',
  EN_APROBACION: 'VIGENTE',
  VIGENTE: 'CERRADO',
  CERRADO: undefined,
};

/** El estado que sigue; ninguno después de Cerrado. */
export function siguiente(estado: EstadoPeriodo): EstadoPeriodo | undefined {
  return SIGUIENTE[estado];
}
