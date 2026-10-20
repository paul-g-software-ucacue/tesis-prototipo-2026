// Los nombres en español de las reglas y de sus parámetros (research §4 de F04-H01). Las cifras
// vienen de la versión de normativa (principio VI); aquí solo están los nombres.
import { legible } from './perfiles';

const REGLAS: Record<string, string> = {
  'RN-01': 'Dedicaciones',
  'RN-02': 'Máximo de horas por docente',
  'RN-05': 'Proporción de clase y otras actividades de docencia',
  'RN-06': 'Tipo de IST por matriculados',
  'RN-09': 'Coordinador académico institucional',
  'RN-10': 'Docentes con cargo de gestión',
  'RN-13': 'Investigación y vinculación',
  // F04-H03: las reglas del motor que faltaban (tabla 5.2 del modelo de dominio) y la afinidad.
  'RN-12': 'Un coordinador de carrera por carrera',
  'RN-16': 'Reemplazo por maternidad o lactancia',
  'RN-17': 'Ningún docente sin horas de clase',
  'RN-18': 'Horas de clase dentro de la malla',
  'RN-19': 'Mínimo de afinidad',
  'RN-22': 'Solo actividades del catálogo',
  'RN-24': 'Pesos de afinidad',
  'RN-25': 'Investigación y vinculación con proyecto aprobado',
  'RF-24': 'Oferta sin propuesta',
};

const PARAMETROS: Record<string, string> = {
  dedicacion_tiempo_completo: 'Tiempo completo (h)',
  dedicacion_medio_tiempo: 'Medio tiempo (h)',
  dedicacion_tiempo_parcial_maxima: 'Tiempo parcial, máximo (h)',
  horas_maximas_docente: 'Máximo por docente (h)',
  porcentaje_clase: 'Clase (%)',
  porcentaje_otras_docencia: 'Otras actividades de docencia (%)',
  estudiantes_minimos: 'Más de (estudiantes)',
  campus_minimos: 'Desde (campus)',
  porcentaje_nomina: 'Parte de la nómina (%)',
  cuarto_nivel_campo_especifico: 'Título de cuarto nivel, campo específico',
  cuarto_nivel_campo_amplio: 'Título de cuarto nivel, campo amplio',
  tercer_nivel_campo_especifico: 'Título de tercer nivel, campo específico',
  tercer_nivel_campo_amplio: 'Título de tercer nivel, campo amplio',
  experiencia_relacionada: 'Experiencia relacionada',
  curso_relacionado: 'Cada curso relacionado',
  tope_cursos: 'Tope de los cursos',
  minimo_afinidad: 'Mínimo de afinidad',
};

/**
 * Los pesos de RN-24 y el mínimo de RN-19 en el orden en que se leen (RN-24: primero los títulos,
 * del campo específico al amplio), no en el de la base.
 */
export const AFINIDAD = [
  ['RN-24', 'cuarto_nivel_campo_especifico'],
  ['RN-24', 'cuarto_nivel_campo_amplio'],
  ['RN-24', 'tercer_nivel_campo_especifico'],
  ['RN-24', 'tercer_nivel_campo_amplio'],
  ['RN-24', 'experiencia_relacionada'],
  ['RN-24', 'curso_relacionado'],
  ['RN-24', 'tope_cursos'],
  ['RN-19', 'minimo_afinidad'],
] as const;

// t2_desde → «Tipo 2, desde (estudiantes)»; t2_minimo → «Tipo 2, mínimo (docentes)».
const POR_TIPO: Record<string, string> = {
  desde: 'desde (estudiantes)',
  hasta: 'hasta (estudiantes)',
  minimo: 'mínimo (docentes)',
  maximo: 'máximo (docentes)',
};

/** El nombre de una regla, o su identificador si la SPA no lo conoce. */
export function nombreDeLaRegla(identificador: string): string {
  return REGLAS[identificador] ?? identificador;
}

/** El nombre de un parámetro, con su unidad. */
export function nombreDelParametro(nombre: string): string {
  const conocido = PARAMETROS[nombre];
  if (conocido) {
    return conocido;
  }
  const [, tipo, limite] = /^t([1-5])_(desde|hasta|minimo|maximo)$/.exec(nombre) ?? [];
  if (tipo && limite) {
    return `Tipo ${tipo}, ${POR_TIPO[limite]}`;
  }
  return legible(nombre);
}
