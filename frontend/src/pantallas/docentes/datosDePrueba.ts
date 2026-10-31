// Datos de prueba de «Docentes» (ficticios, RNF-SEG-06): dos docentes, uno con su ficha completa, y
// las referencias de Académico con la carrera, dos asignaturas y los campos amplios que usan.
import type {
  AfinidadDelDocente,
  Docente,
  ReferenciasDeDocentes,
  ResumenDeDocente,
} from './consultas';

export const PERALTA: Docente = {
  tipoIdentificacion: 'CEDULA',
  identificacion: '0109990010',
  nombreCompleto: 'PERALTA MALDONADO JUAN ANDRES',
  primerApellido: 'PERALTA',
  segundoApellido: 'MALDONADO',
  primerNombre: 'JUAN',
  segundoNombre: 'ANDRES',
  correoInstitucional: 'juan.peralta@instituto-prueba.edu.ec',
  telefono: '0990000001',
  tipoPersonal: 'OCASIONAL',
  categoriaEscalafon: 'NO_APLICA',
  dedicacion: 'TC',
  horasContratadas: 40,
  fechaIngreso: '2019-10-01',
  inicioContrato: '2026-04-01',
  finContrato: '2026-08-31',
  condicionEspecial: 'NINGUNA',
  carreraBase: '550613A01-P-0301',
  nombreDeCarreraBase: 'DESARROLLO DE SOFTWARE',
  estado: 'ACTIVO',
  ficha: {
    tercerNivel: {
      titulo: 'INGENIERO DE SISTEMAS',
      campoAmplio: '06',
      campoEspecifico: '061',
    },
    cuartoNivel: [
      { titulo: 'MÁSTER EN TELEMÁTICA', tipo: 'MAESTRIA', campoEspecifico: '061', anio: 2015 },
    ],
    cursos: [
      { nombre: 'PROGRAMACIÓN EN JAVA', campoEspecifico: '061', horas: 40, anio: 2023 },
      { nombre: 'DOCENCIA UNIVERSITARIA', campoEspecifico: '011', horas: 60, anio: 2021 },
    ],
    experiencia: [
      { cargo: 'DESARROLLADOR', institucion: 'EMPRESA XYZ', campoEspecifico: '061', anios: 3 },
    ],
    asignaturasAfines: ['DS-P-II-POO-204'],
  },
  camposAmplios: [
    { codigo: '01', nombre: 'Educación' },
    { codigo: '06', nombre: 'Tecnologías de la información y la comunicación' },
  ],
  asignaturasAfines: [{ codigo: 'DS-P-II-POO-204', nombre: 'PROGRAMACIÓN ORIENTADA A OBJETOS' }],
};

function resumen(docente: Docente): ResumenDeDocente {
  return {
    identificacion: docente.identificacion,
    nombreCompleto: docente.nombreCompleto,
    carreraBase: docente.carreraBase,
    nombreDeCarreraBase: docente.nombreDeCarreraBase,
    tipoPersonal: docente.tipoPersonal,
    dedicacion: docente.dedicacion,
    horasContratadas: docente.horasContratadas,
    condicionEspecial: docente.condicionEspecial,
    estado: docente.estado,
  };
}

export const RESUMENES: ResumenDeDocente[] = [
  {
    identificacion: '0109990028',
    nombreCompleto: 'CABRERA VAZQUEZ ANA GABRIELA',
    carreraBase: '550613A01-P-0301',
    nombreDeCarreraBase: 'DESARROLLO DE SOFTWARE',
    tipoPersonal: 'TITULAR',
    dedicacion: 'MT',
    horasContratadas: 20,
    condicionEspecial: 'MATERNIDAD_LACTANCIA',
    estado: 'LICENCIA',
  },
  resumen(PERALTA),
];

export const REFERENCIAS: ReferenciasDeDocentes = {
  carreras: [{ codigo: '550613A01-P-0301', nombre: 'DESARROLLO DE SOFTWARE', siglas: 'DS' }],
  asignaturas: [
    { codigo: 'DS-B-I-MD-101', nombre: 'MATEMÁTICA DISCRETA', carrera: '550613A01-P-0301' },
    {
      codigo: 'DS-P-II-POO-204',
      nombre: 'PROGRAMACIÓN ORIENTADA A OBJETOS',
      carrera: '550613A01-P-0301',
    },
  ],
  camposAmplios: [
    { codigo: '01', nombre: 'Educación' },
    { codigo: '06', nombre: 'Tecnologías de la información y la comunicación' },
  ],
  unidades: [
    {
      codigo: 'G-INV',
      nombre: 'Gestión de Investigación',
      tipo: 'GESTION',
      gestionAutorizada: 'G07',
      activa: true,
    },
    { codigo: 'COM-DIST', nombre: 'Comisión del distributivo', tipo: 'COMISION', activa: true },
  ],
  periodos: [
    {
      codigo: '2026-I',
      nombre: 'Periodo académico 2026-I',
      inicio: '2026-04-06',
      fin: '2026-08-07',
    },
    {
      codigo: '2026-II',
      nombre: 'Periodo académico 2026-II',
      inicio: '2026-10-05',
      fin: '2027-02-05',
    },
  ],
};

/** El puntaje de PERALTA: una asignatura con el tope de cursos aplicado y otra sin coincidencias. */
export const AFINIDAD: AfinidadDelDocente = {
  periodo: '2026-I',
  version: 2,
  puntajes: [
    {
      asignatura: 'DS-P-II-POO-204',
      nombre: 'PROGRAMACIÓN ORIENTADA A OBJETOS',
      total: 7.5,
      topeAplicado: true,
      desglose: [
        {
          tipo: 'TERCER_NIVEL',
          nombre: 'INGENIERO DE SISTEMAS',
          coincidencia: 'CAMPO_ESPECIFICO',
          aporte: 2,
        },
        {
          tipo: 'CUARTO_NIVEL',
          nombre: 'MÁSTER EN TELEMÁTICA',
          coincidencia: 'CAMPO_AMPLIO',
          aporte: 2,
        },
        {
          tipo: 'EXPERIENCIA',
          nombre: 'DESARROLLADOR',
          coincidencia: 'CAMPO_ESPECIFICO',
          aporte: 2,
        },
        {
          tipo: 'CURSO',
          nombre: 'PROGRAMACIÓN EN JAVA',
          coincidencia: 'CAMPO_ESPECIFICO',
          aporte: 1.5,
        },
      ],
    },
    {
      asignatura: 'DS-P-I-ANA-101',
      nombre: 'ANATOMÍA',
      total: 0,
      topeAplicado: false,
      desglose: [],
    },
  ],
};
