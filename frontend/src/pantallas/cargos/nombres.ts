// Los nombres en español de los cargos (carga 8, D-73) y de su estado. Los códigos viven en el
// contrato y en el diccionario de datos; aquí solo está cómo se leen en pantalla.
import type { EstadoDeCargo, TipoDeCargo } from './consultas';

export const NOMBRE_DEL_CARGO: Record<TipoDeCargo, string> = {
  RECTOR: 'Rector',
  VICERRECTOR: 'Vicerrector',
  COORDINADOR_ACADEMICO_INSTITUCIONAL: 'Coordinador académico institucional',
  COORDINADOR_SEDE: 'Coordinador de sede, extensión o campus',
  COORDINADOR_CARRERA: 'Coordinador de carrera',
  G06_GESTOR_VINCULACION: 'Gestor de Vinculación con la Sociedad (G06)',
  G07_GESTOR_INVESTIGACION: 'Gestor de Investigación, Desarrollo Tecnológico e Innovación (G07)',
  G08_GESTOR_IDIOMAS: 'Gestor de Centro de Idiomas (G08)',
  G09_GESTOR_FORMACION_INTEGRAL:
    'Gestor de Centro de Formación Integral y Servicios Especializados (G09)',
  G10_COORDINADOR_CALIDAD: 'Coordinador de Aseguramiento de la Calidad (G10)',
  G11_SECRETARIO_GENERAL: 'Secretario General Académico (G11)',
  G12_PROCURADOR: 'Procurador (G12)',
  G13_GESTOR_BIENESTAR: 'Gestor de Bienestar Institucional (G13)',
  G14_GESTOR_ADMINISTRATIVO_FINANCIERO: 'Gestor Administrativo Financiero (G14)',
  G15_GESTOR_PLANIFICACION: 'Gestor de Planificación Estratégica (G15)',
  G16_GESTOR_TALENTO_HUMANO: 'Gestor de la Unidad de Talento Humano (G16)',
  G17_GESTOR_BIBLIOTECA: 'Gestor de la Unidad de Servicios de Biblioteca (G17)',
  G18_GESTOR_COMUNICACION: 'Gestor de la Unidad de Comunicación (G18)',
  G19_GESTOR_RELACIONES_INTERNACIONALES:
    'Gestor de la Unidad de Relaciones Internacionales e Institucionales (G19)',
  G20_GESTOR_TIC: 'Gestor de la Unidad de TIC (G20)',
  G21_GESTOR_ESTADISTICAS: 'Gestor de la Unidad de Estadísticas (G21)',
  MIEMBRO_COMISION: 'Miembro de comisión',
};

export const NOMBRE_DEL_ESTADO_DEL_CARGO: Record<EstadoDeCargo, string> = {
  VIGENTE: 'Vigente',
  FINALIZADO: 'Finalizado',
};

/** La gestión autorizada de un cargo de gestor, como «G07»; nada para los demás. */
export function gestionDe(tipo: TipoDeCargo): string | undefined {
  return /^G\d\d_/.test(tipo) ? tipo.slice(0, 3) : undefined;
}

/** «1 cargo», «21 cargos». */
export function cargos(cantidad: number) {
  return cantidad === 1 ? '1 cargo' : `${String(cantidad)} cargos`;
}
