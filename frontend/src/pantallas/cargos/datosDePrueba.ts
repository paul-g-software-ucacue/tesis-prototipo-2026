// Cargos de prueba para la pantalla «Cargos» (F03-H05). Los datos son ficticios (RNF-SEG-06).
import type { Cargo } from './consultas';

export const COORDINADOR: Cargo = {
  docente: '0109990010',
  nombreDelDocente: 'PERALTA MALDONADO JUAN ANDRES',
  tipo: 'COORDINADOR_CARRERA',
  fechaDesignacion: '2025-04-01',
  carrera: '550613A01-P-0301',
  nombreDeCarrera: 'DESARROLLO DE SOFTWARE',
  fechaFin: '2027-03-31',
  documento: 'RES-PRUEBA-2025-004',
  estado: 'VIGENTE',
  advertencias: [],
};

export const GESTOR_SIN_CUARTO_NIVEL: Cargo = {
  docente: '0109990028',
  nombreDelDocente: 'CABRERA VAZQUEZ ANA GABRIELA',
  tipo: 'G07_GESTOR_INVESTIGACION',
  fechaDesignacion: '2025-04-01',
  unidad: 'G-INV',
  nombreDeUnidad: 'Gestión de Investigación',
  documento: 'RES-PRUEBA-2025-012',
  estado: 'VIGENTE',
  advertencias: [
    {
      codigo: 'CUARTO_NIVEL',
      descripcion: 'Le falta un título de cuarto nivel registrado (RN-20).',
    },
  ],
};
