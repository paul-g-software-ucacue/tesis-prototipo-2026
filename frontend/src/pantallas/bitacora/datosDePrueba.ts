// Las respuestas de la API para las pruebas de «Bitácora», con los datos ficticios de la demo (D-06).
import type { AccionDeBitacora, EntradaConsultada, PaginaDeBitacora } from './consultas';

export const CORTE = '2026-11-07T20:00:00Z';

export const DE_4_A_6: EntradaConsultada = {
  id: '6f1c2d4e-8a9b-4c3d-9e1f-0a2b3c4d5e6f',
  servicio: 'generacion',
  usuario: 'coordinacion.demo',
  accion: 'ACTIVIDAD_GUARDADA',
  momento: '2026-11-07T17:02:14Z',
  periodo: '2026-I',
  objeto: { periodo: '2026-I', docente: '0102030405', actividad: '140-b' },
  anterior: { horas: '4' },
  nuevo: { horas: '6' },
};

export const CREADA: EntradaConsultada = {
  id: '7a2d3e5f-9b0c-4d4e-8f20-1b3c4d5e6f70',
  servicio: 'docentes',
  usuario: 'datos.demo',
  accion: 'DOCENTE_REGISTRADO',
  momento: '2026-11-07T15:00:00Z',
  objeto: { identificacion: '0102030405', camposPersonales: ['telefono'] },
  nuevo: { dedicacion: 'TC' },
};

export const QUITADA: EntradaConsultada = {
  id: '8b3e4f60-0c1d-4e5f-9031-2c4d5e6f7081',
  servicio: 'generacion',
  usuario: 'coordinacion.demo',
  accion: 'ACTIVIDAD_QUITADA',
  momento: '2026-11-07T14:00:00Z',
  periodo: '2026-I',
  objeto: { actividad: '143-k' },
  anterior: { horas: '2' },
};

export function pagina(
  entradas: EntradaConsultada[],
  total = entradas.length,
  numero = 1,
  tamano = 50,
): PaginaDeBitacora {
  return { entradas, total, pagina: numero, tamano, corte: CORTE };
}

export const ACCIONES: AccionDeBitacora[] = [
  { servicio: 'docentes', accion: 'DOCENTE_REGISTRADO' },
  { servicio: 'generacion', accion: 'ACTIVIDAD_GUARDADA' },
  { servicio: 'generacion', accion: 'ACTIVIDAD_QUITADA' },
];
