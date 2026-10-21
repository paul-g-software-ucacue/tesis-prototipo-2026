// Respuestas de la API de Académico para las pruebas de la pantalla «Periodos» (contratos/academico).
// Un periodo en Planificación sin versión y otro vigente con la versión 1; las versiones de
// normativa que se pueden elegir al abrir.
export const PLANIFICACION = {
  codigo: '2026-2',
  nombre: 'Periodo 2026-2',
  inicio: '2026-09-01',
  fin: '2027-02-28',
  estado: 'PLANIFICACION',
};

export const VIGENTE = {
  codigo: '2026-1',
  nombre: 'Periodo 2026-1',
  inicio: '2026-03-01',
  fin: '2026-08-15',
  estado: 'VIGENTE',
  version: 1,
};

export const PERIODOS = [PLANIFICACION, VIGENTE];

export const VERSIONES = [
  {
    numero: 1,
    codigo: 'SENESCYT-2024-03',
    origen: 'Política SENESCYT 2024',
    vigenteDesde: '2024-03-11',
    tipoIst: 'T2',
    periodos: ['2026-1'],
  },
  {
    numero: 2,
    codigo: 'SENESCYT-2025-01',
    origen: 'Reforma de la política 2025',
    vigenteDesde: '2025-01-15',
    tipoIst: 'T2',
    periodos: [] as string[],
    copiadaDe: 1,
  },
];
