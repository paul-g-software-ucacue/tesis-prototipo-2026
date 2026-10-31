// Tableros de preparación para las pruebas de «Preparación del periodo» (F02-H02): los periodos
// 2026-I y 2026-II de la carga 3 de prueba y las 10 cargas de la ERS §8.3.
import type { CargaDelPeriodo, PreparacionDelPeriodo } from './consultas';

const CARGAS: Omit<CargaDelPeriodo, 'estado'>[] = [
  {
    numero: 1,
    codigo: 'estructura_organica',
    nombre: 'Estructura orgánica',
    obligatoriedad: 'OBLIGATORIA',
  },
  { numero: 2, codigo: 'carreras', nombre: 'Carreras', obligatoriedad: 'OBLIGATORIA' },
  {
    numero: 3,
    codigo: 'ciclos_academicos',
    nombre: 'Ciclos académicos aprobados',
    obligatoriedad: 'OBLIGATORIA',
  },
  {
    numero: 4,
    codigo: 'asignaturas_malla',
    nombre: 'Asignaturas y malla',
    obligatoriedad: 'OBLIGATORIA',
  },
  {
    numero: 5,
    codigo: 'oferta_periodo',
    nombre: 'Oferta del periodo',
    obligatoriedad: 'OBLIGATORIA',
  },
  {
    numero: 6,
    codigo: 'docentes',
    nombre: 'Docentes contratados (con ficha académica)',
    obligatoriedad: 'OBLIGATORIA',
  },
  {
    numero: 7,
    codigo: 'politica_carga_horaria',
    nombre: 'Horas según la política de contratación',
    obligatoriedad: 'OBLIGATORIA',
  },
  {
    numero: 8,
    codigo: 'cargos_autoridades',
    nombre: 'Cargos y autoridades',
    obligatoriedad: 'OBLIGATORIA',
  },
  {
    numero: 9,
    codigo: 'proyectos_practicas',
    nombre: 'Proyectos y prácticas aprobados',
    obligatoriedad: 'OBLIGATORIA_SALVO_SIN_PROYECTOS',
  },
  {
    numero: 10,
    codigo: 'matriculados_carrera',
    nombre: 'Matriculados por carrera',
    obligatoriedad: 'RECOMENDADA',
  },
];

export const PERIODOS = [
  { codigo: '2026-II', nombre: 'Periodo académico 2026-II' },
  { codigo: '2026-I', nombre: 'Periodo académico 2026-I' },
];

/**
 * El tablero de un periodo abierto con las 10 cargas, salvo los estados que se digan por código;
 * lo que falta y si está listo salen de esos estados.
 */
export function tablero(
  periodo: string,
  estados: Record<string, Pick<CargaDelPeriodo, 'estado' | 'motivo'>> = {},
  { abierto = true, cerrado = false } = {},
): PreparacionDelPeriodo {
  const cargas: CargaDelPeriodo[] = CARGAS.map((carga) => ({
    ...carga,
    estado: 'CARGADA',
    ...estados[carga.codigo],
  }));
  const faltan: PreparacionDelPeriodo['faltan'] = [
    ...cargas
      .filter(
        (carga) =>
          carga.obligatoriedad !== 'RECOMENDADA' &&
          carga.estado !== 'CARGADA' &&
          carga.estado !== 'SIN_PROYECTOS',
      )
      .map((carga) => ({
        tipo: 'CARGA' as const,
        carga: { numero: carga.numero, codigo: carga.codigo, nombre: carga.nombre },
        estado: carga.estado,
      })),
    ...(abierto ? [] : [{ tipo: 'PERIODO_SIN_ABRIR' as const }]),
    ...(cerrado ? [{ tipo: 'PERIODO_CERRADO' as const }] : []),
  ];
  return {
    periodos: PERIODOS,
    periodo,
    abierto,
    cerrado,
    cargas,
    faltan,
    listo: faltan.length === 0,
  };
}

/** El tablero sin periodos: no hay ciclos académicos cargados. */
export const SIN_PERIODOS: PreparacionDelPeriodo = {
  periodos: [],
  cargas: [],
  faltan: [{ tipo: 'SIN_PERIODOS' }],
  listo: false,
};
