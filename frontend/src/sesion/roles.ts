// Los nombres en español de los 10 roles de la matriz D-16, para mostrarlos al usuario. El token y
// la API usan los identificadores (ADR-014); este es el único lugar que los traduce.
const NOMBRES: Record<string, string> = {
  ADMINISTRADOR: 'Administrador del sistema',
  OPERADOR_DATOS: 'Talento Humano (operador de datos)',
  COORDINADOR_CARRERA: 'Coordinador de carrera',
  GESTOR_UNIDAD: 'Gestor de investigación o vinculación',
  RESPONSABLE_CONSOLIDACION: 'Responsable de consolidación',
  VICERRECTOR: 'Vicerrector académico',
  RECTOR: 'Rector',
  MIEMBRO_OCS: 'Miembro del OCS',
  SECRETARIA_GENERAL: 'Secretaría general académica',
  DOCENTE: 'Docente',
};

/** El nombre del rol para el usuario; un identificador desconocido se muestra tal cual. */
export function nombreDelRol(identificador: string): string {
  return NOMBRES[identificador] ?? identificador;
}
