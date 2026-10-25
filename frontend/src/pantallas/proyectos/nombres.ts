// Los nombres en español de los valores de la carga 9 (F03-H05): los códigos viven en el contrato y
// en el diccionario de datos; aquí solo está cómo se leen en pantalla.
import type {
  EstadoDeProyecto,
  MotivoNoAsignable,
  RolDeParticipante,
  TipoDeProyecto,
} from './consultas';

export const NOMBRE_DEL_TIPO_DE_PROYECTO: Record<TipoDeProyecto, string> = {
  INVESTIGACION: 'Investigación',
  VINCULACION: 'Vinculación',
  PRACTICAS_PREPROFESIONALES: 'Prácticas preprofesionales',
  SERVICIO_COMUNITARIO: 'Servicio comunitario',
};

export const NOMBRE_DEL_ESTADO_DEL_PROYECTO: Record<EstadoDeProyecto, string> = {
  APROBADO: 'Aprobado',
  EN_EJECUCION: 'En ejecución',
  FINALIZADO: 'Finalizado',
  SUSPENDIDO: 'Suspendido',
};

export const NOMBRE_DEL_ROL: Record<RolDeParticipante, string> = {
  DIRECTOR: 'Director',
  INVESTIGADOR: 'Investigador',
  DOCENTE_APOYO: 'Docente de apoyo',
  TUTOR_PRACTICAS: 'Tutor de prácticas',
};

export const POR_QUE_NO_ES_ASIGNABLE: Record<MotivoNoAsignable, string> = {
  ESTADO: 'por su estado',
  VIGENCIA: 'por su vigencia',
};

/** «1 proyecto», «11 proyectos». */
export function proyectos(cantidad: number) {
  return cantidad === 1 ? '1 proyecto' : `${String(cantidad)} proyectos`;
}

/** Las horas como se leen en pantalla, con coma decimal: «2,5». */
export function horas(valor: number) {
  return String(valor).replace('.', ',');
}
