// Los nombres en español de los valores permitidos de las cargas 02 y 04 (diccionario de datos).
import type {
  EstadoDeCarrera,
  EstadoDeMalla,
  Jornada,
  Modalidad,
  NivelDeFormacion,
  UnidadDeOrganizacionCurricular,
} from './consultas';

export const NOMBRE_DEL_NIVEL: Record<NivelDeFormacion, string> = {
  TECNICO_SUPERIOR: 'Técnico superior',
  TECNOLOGICO_SUPERIOR: 'Tecnológico superior',
  TECNOLOGICO_SUPERIOR_UNIVERSITARIO: 'Tecnológico superior universitario',
};

export const NOMBRE_DE_LA_MODALIDAD: Record<Modalidad, string> = {
  PRESENCIAL: 'Presencial',
  SEMIPRESENCIAL: 'Semipresencial',
  EN_LINEA: 'En línea',
  A_DISTANCIA: 'A distancia',
  DUAL: 'Dual',
  HIBRIDA: 'Híbrida',
};

/** En el orden de la enumeración, que es el que guarda el servidor. */
export const NOMBRE_DE_LA_JORNADA: Record<Jornada, string> = {
  MATUTINA: 'Matutina',
  VESPERTINA: 'Vespertina',
  NOCTURNA: 'Nocturna',
  INTENSIVA: 'Intensiva',
};

export const NOMBRE_DEL_ESTADO_DE_CARRERA: Record<EstadoDeCarrera, string> = {
  VIGENTE: 'Vigente',
  NO_VIGENTE: 'No vigente',
  EN_CIERRE: 'En cierre',
};

export const NOMBRE_DEL_ESTADO_DE_MALLA: Record<EstadoDeMalla, string> = {
  VIGENTE: 'Vigente',
  DEROGADA: 'Derogada',
};

export const NOMBRE_DE_LA_UNIDAD: Record<UnidadDeOrganizacionCurricular, string> = {
  BASICA: 'Básica',
  PROFESIONAL: 'Profesional',
  INTEGRACION_CURRICULAR: 'Integración curricular',
};

/** Las jornadas en una frase: «Matutina, nocturna». */
export function jornadasEnTexto(jornadas: readonly Jornada[]): string {
  const texto = jornadas.map((jornada) => NOMBRE_DE_LA_JORNADA[jornada].toLowerCase()).join(', ');
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

/** Las horas con coma decimal: 4,5. */
export function horas(valor: number): string {
  return valor.toLocaleString('es-EC', { maximumFractionDigits: 1 });
}
