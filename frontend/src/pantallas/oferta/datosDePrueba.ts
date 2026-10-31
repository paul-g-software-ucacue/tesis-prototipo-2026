// Datos de prueba de «Oferta del periodo»: la oferta del periodo en Planificación, con POO de 4 horas
// en 3 paralelos (12 horas a cubrir) y Matemática discreta retirada; y la del periodo vigente. Y la
// sugerencia de POO: uno con horas de sobra, uno con decimales y uno sin horas suficientes.
import type { AsignaturaParalelo, Candidato, OfertaDelPeriodo, Sugerencia } from './consultas';

const CAMPO = { codigo: '06', nombre: 'Tecnologías de la información y la comunicación' };

function paralelo(
  periodo: string,
  asignatura: string,
  nombre: string,
  ciclo: number,
  paralelo: string,
  horas: number,
  estado: 'OFERTADA' | 'RETIRADA' = 'OFERTADA',
): AsignaturaParalelo {
  return {
    periodo,
    asignatura,
    nombre,
    carrera: '550613A01-P-0301',
    malla: 'DS-2019',
    ciclo,
    paralelo,
    jornada: 'NOCTURNA',
    modalidad: 'PRESENCIAL',
    sede: 'MATRIZ',
    sedeIndicada: false,
    estudiantes: 28,
    horasSemanales: horas,
    horasIndicadas: false,
    horasDeLaAsignatura: horas,
    porEncimaDeLaAsignatura: false,
    troncoComun: false,
    campoAmplio: CAMPO,
    campoEspecifico: '061',
    palabrasClave: ['programación', 'objetos'],
    estado,
  };
}

function oferta(periodo: string, estado: 'PLANIFICACION' | 'VIGENTE'): OfertaDelPeriodo {
  return {
    periodo,
    estadoDelPeriodo: estado,
    modificable: estado === 'PLANIFICACION',
    horasACubrir: 12,
    carreras: [
      {
        carrera: '550613A01-P-0301',
        nombre: 'DESARROLLO DE SOFTWARE',
        horasACubrir: 12,
        asignaturas: [
          {
            asignatura: 'DS-B-I-MD-101',
            nombre: 'MATEMÁTICA DISCRETA',
            ciclo: 1,
            paralelos: 0,
            horasACubrir: 0,
          },
          {
            asignatura: 'DS-P-II-POO-204',
            nombre: 'PROGRAMACIÓN ORIENTADA A OBJETOS',
            ciclo: 2,
            paralelos: 3,
            horasACubrir: 12,
          },
        ],
      },
    ],
    asignaturasParalelo: [
      paralelo(periodo, 'DS-B-I-MD-101', 'MATEMÁTICA DISCRETA', 1, 'A_NOCTURNA', 3, 'RETIRADA'),
      ...['A_NOCTURNA', 'B_NOCTURNA', 'C_NOCTURNA'].map((codigo) =>
        paralelo(periodo, 'DS-P-II-POO-204', 'PROGRAMACIÓN ORIENTADA A OBJETOS', 2, codigo, 4),
      ),
    ],
  };
}

export const OFERTA_EN_PLANIFICACION = oferta('2026-2', 'PLANIFICACION');
export const OFERTA_VIGENTE = oferta('2026-1', 'VIGENTE');

const APORTE = {
  tipo: 'CUARTO_NIVEL',
  nombre: 'MÁSTER EN TELEMÁTICA',
  coincidencia: 'CAMPO_ESPECIFICO',
  aporte: 3,
} as const;

function candidato(
  identificacion: string,
  nombre: string,
  dedicacion: Candidato['dedicacion'],
  horasContratadas: number,
  horasDisponibles: number,
  horasEnOtrasCarreras: number,
  total: number,
): Candidato {
  return {
    identificacion,
    nombre,
    dedicacion,
    horasContratadas,
    horasDisponibles,
    horasEnOtrasCarreras,
    horasSuficientes: horasDisponibles >= 4,
    puntaje: {
      asignatura: 'DS-P-II-POO-204',
      nombre: 'PROGRAMACIÓN ORIENTADA A OBJETOS',
      total,
      topeAplicado: false,
      desglose: [{ ...APORTE, aporte: total }],
    },
  };
}

export const SUGERENCIA: Sugerencia = {
  periodo: '2026-2',
  asignatura: 'DS-P-II-POO-204',
  nombre: 'PROGRAMACIÓN ORIENTADA A OBJETOS',
  paralelo: 'A_NOCTURNA',
  horasDelParalelo: 4,
  troncoComun: false,
  version: 2,
  minimoAfinidad: 2,
  fueraPorElMinimo: 3,
  candidatos: [
    candidato('0109990010', 'PERALTA MALDONADO JUAN ANDRES', 'TC', 40, 40, 0, 7),
    candidato('0109990020', 'SALINAS VEGA ANA MARIA', 'MT', 20, 14.25, 5.75, 5),
    candidato('0109990030', 'TORRES LEON LUIS', 'TP', 10, 2, 8, 9),
  ],
};
