// Los nombres en español de los valores de la carga 6 (F03-H04): los catálogos viven en el contrato
// y en el diccionario de datos; aquí solo está cómo se leen en pantalla.
import type {
  CategoriaDeEscalafon,
  CondicionEspecial,
  Dedicacion,
  EstadoLaboral,
  TipoDeIdentificacion,
  TipoDeTituloDeCuartoNivel,
  TipoPersonal,
} from './consultas';

export const NOMBRE_DEL_TIPO_DE_IDENTIFICACION: Record<TipoDeIdentificacion, string> = {
  CEDULA: 'Cédula',
  PASAPORTE: 'Pasaporte',
};

export const NOMBRE_DEL_TIPO_DE_PERSONAL: Record<TipoPersonal, string> = {
  TITULAR: 'Titular',
  OCASIONAL: 'Ocasional',
  INVITADO: 'Invitado',
  HONORARIO: 'Honorario',
};

export const NOMBRE_DE_LA_CATEGORIA: Record<CategoriaDeEscalafon, string> = {
  AUXILIAR_1: 'Auxiliar 1',
  AUXILIAR_2: 'Auxiliar 2',
  AGREGADO_1: 'Agregado 1',
  AGREGADO_2: 'Agregado 2',
  AGREGADO_3: 'Agregado 3',
  PRINCIPAL_1: 'Principal 1',
  PRINCIPAL_2: 'Principal 2',
  PRINCIPAL_3: 'Principal 3',
  NO_APLICA: 'No aplica',
};

export const NOMBRE_DE_LA_DEDICACION: Record<Dedicacion, string> = {
  TC: 'Tiempo completo',
  MT: 'Medio tiempo',
  TP: 'Tiempo parcial',
};

export const NOMBRE_DE_LA_CONDICION: Record<CondicionEspecial, string> = {
  NINGUNA: 'Ninguna',
  CUIDADOR_SUSTITUTO: 'Cuidador sustituto',
  MATERNIDAD_LACTANCIA: 'Maternidad o lactancia',
  APOYO_SIES: 'Apoyo a la SIES',
};

export const NOMBRE_DEL_ESTADO_LABORAL: Record<EstadoLaboral, string> = {
  ACTIVO: 'Activo',
  LICENCIA: 'En licencia',
  INACTIVO: 'Inactivo',
};

export const NOMBRE_DEL_TIPO_DE_TITULO: Record<TipoDeTituloDeCuartoNivel, string> = {
  MAESTRIA: 'Maestría',
  ESPECIALIZACION: 'Especialización',
  DOCTORADO: 'Doctorado',
};

/** «1 docente», «47 docentes». */
export function docentes(cantidad: number) {
  return cantidad === 1 ? '1 docente' : `${String(cantidad)} docentes`;
}
