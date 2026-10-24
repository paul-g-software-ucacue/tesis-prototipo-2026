// Datos de prueba de «Carreras y mallas»: dos carreras, tres mallas y asignaturas de la de
// Desarrollo de Software, tomadas de las cargas 02 y 04 de prueba de la ERS (sintéticas).
import type { Asignatura, CampoAmplio, Carrera } from './consultas';

export const CAMPOS: CampoAmplio[] = [
  { codigo: '04', nombre: 'Administración de empresas y derecho' },
  { codigo: '05', nombre: 'Ciencias naturales, matemáticas y estadística' },
  { codigo: '06', nombre: 'Tecnologías de la información y la comunicación' },
];

export const CARRERAS: Carrera[] = [
  {
    codigo: '550411C02-P-0301',
    nombre: 'CONTABILIDAD',
    siglas: 'CONT',
    sede: 'MATRIZ',
    nivelFormacion: 'TECNOLOGICO_SUPERIOR',
    modalidad: 'PRESENCIAL',
    jornadas: ['NOCTURNA'],
    campoAmplio: '04',
    campoEspecifico: '041',
    numeroCiclos: 5,
    estado: 'EN_CIERRE',
    mallas: [{ codigo: 'CONT-2019', estado: 'DEROGADA', asignaturas: 0, horasSemanalesClase: 0 }],
  },
  {
    codigo: '550613A01-P-0301',
    nombre: 'DESARROLLO DE SOFTWARE',
    siglas: 'DS',
    sede: 'MATRIZ',
    nivelFormacion: 'TECNOLOGICO_SUPERIOR',
    modalidad: 'PRESENCIAL',
    jornadas: ['MATUTINA', 'NOCTURNA'],
    campoAmplio: '06',
    campoEspecifico: '061',
    campoDetallado: '0613',
    numeroCiclos: 5,
    estado: 'VIGENTE',
    mallas: [
      { codigo: 'DS-2019', estado: 'VIGENTE', asignaturas: 3, horasSemanalesClase: 11 },
      { codigo: 'DS-AC-2025', estado: 'VIGENTE', asignaturas: 0, horasSemanalesClase: 0 },
    ],
  },
];

function asignatura(
  codigo: string,
  nombre: string,
  ciclo: number,
  horas: number,
  troncoComun: boolean,
  activa = true,
): Asignatura {
  return {
    codigo,
    carrera: '550613A01-P-0301',
    malla: 'DS-2019',
    nombre,
    ciclo,
    unidadOrganizacionCurricular: ciclo === 1 ? 'BASICA' : 'PROFESIONAL',
    horasSemanalesClase: horas,
    troncoComun,
    campoAmplio: { codigo: '05', nombre: 'Ciencias naturales, matemáticas y estadística' },
    campoEspecifico: '054',
    palabrasClave: ['matemática', 'lógica'],
    activa,
  };
}

export const ASIGNATURAS: Asignatura[] = [
  asignatura('DS-B-I-MD-101', 'MATEMÁTICA DISCRETA', 1, 3, true),
  asignatura('DS-B-I-CTA-102', 'COMUNICACIÓN Y TÉCNICAS DE APRENDIZAJE', 1, 5, true),
  asignatura('DS-P-II-POO-204', 'PROGRAMACIÓN ORIENTADA A OBJETOS', 2, 3, false),
  asignatura('DS-P-II-VIEJA-205', 'ASIGNATURA INACTIVA', 2, 4, false, false),
];
