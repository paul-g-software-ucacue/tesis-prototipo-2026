// Proyectos de prueba para la pantalla «Proyectos» (F03-H05). Los datos son ficticios (RNF-SEG-06).
import type { Proyecto } from './consultas';

export const INVESTIGACION: Proyecto = {
  codigo: 'INV-2025-01',
  nombre: 'Prototipo de monitoreo IoT',
  tipo: 'INVESTIGACION',
  carreras: ['550613A01-P-0301'],
  nombresDeCarreras: [
    { codigo: '550613A01-P-0301', nombre: 'DESARROLLO DE SOFTWARE', siglas: 'DS' },
  ],
  resolucionOcs: 'RES-OCS-PRUEBA-2025-040',
  fechaResolucion: '2025-03-20',
  estado: 'EN_EJECUCION',
  vigenciaInicio: '2025-04-01',
  vigenciaFin: '2026-12-31',
  director: '0109990010',
  nombreDelDirector: 'PERALTA MALDONADO JUAN ANDRES',
  participantes: [
    {
      docente: '0109990010',
      nombre: 'PERALTA MALDONADO JUAN ANDRES',
      rol: 'DIRECTOR',
      horas: 14,
    },
    {
      docente: '0109990028',
      nombre: 'CABRERA VAZQUEZ ANA GABRIELA',
      rol: 'INVESTIGADOR',
      horas: 10,
    },
  ],
  horasAprobadas: 24,
  asignable: true,
  motivos: [],
};

export const PRACTICAS_FUERA_DE_VIGENCIA: Proyecto = {
  ...INVESTIGACION,
  codigo: 'PPP-2026-DS',
  nombre: 'Prácticas de Desarrollo de Software',
  tipo: 'PRACTICAS_PREPROFESIONALES',
  estado: 'SUSPENDIDO',
  vigenciaInicio: '2026-04-06',
  vigenciaFin: '2026-08-14',
  participantes: [
    {
      docente: '0109990010',
      nombre: 'PERALTA MALDONADO JUAN ANDRES',
      rol: 'TUTOR_PRACTICAS',
      horas: 2.5,
    },
  ],
  horasAprobadas: 2.5,
  asignable: false,
  motivos: ['ESTADO', 'VIGENCIA'],
};
