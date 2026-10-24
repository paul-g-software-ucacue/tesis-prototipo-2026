// Datos de prueba de «Oferta del periodo»: la oferta del periodo en Planificación, con POO de 4 horas
// en 3 paralelos (12 horas a cubrir) y Matemática discreta retirada; y la del periodo vigente.
import type { AsignaturaParalelo, OfertaDelPeriodo } from './consultas';

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
