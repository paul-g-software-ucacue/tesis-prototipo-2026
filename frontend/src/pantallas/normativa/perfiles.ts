// Los nombres en español de los perfiles de la tabla de carga horaria (carga 7, glosario §7.3). La
// API usa los códigos; uno que la SPA no conoce, de una versión nueva, se muestra legible.
const NOMBRES: Record<string, string> = {
  DOCENTE_TC: 'Docente a tiempo completo',
  DOCENTE_MT: 'Docente a medio tiempo',
  COORDINADOR_CARRERA_SEDE: 'Coordinador de carrera o de sede',
  GESTOR: 'Docente gestor',
  COORDINADOR_ACADEMICO_INSTITUCIONAL: 'Coordinador académico institucional',
  RECTOR_VICERRECTOR: 'Rector o vicerrector',
  SECRETARIA_GENERAL_ACADEMICA: 'Secretaría general académica',
  DOCENTE_PROYECTO_NACIONAL_SIES: 'Docente de un proyecto nacional de la SIES',
  DOCENTE_INVESTIGACION_VINCULACION: 'Docente de investigación o vinculación',
  CUIDADOR_SUSTITUTO: 'Cuidador sustituto',
};

/** El nombre del perfil para el usuario. */
export function nombreDelPerfil(codigo: string): string {
  return NOMBRES[codigo] ?? legible(codigo);
}

/** «ALGO_ASI» → «Algo asi»: para un código que la SPA no conoce. */
export function legible(codigo: string): string {
  const texto = codigo.toLowerCase().replaceAll('_', ' ');
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}
