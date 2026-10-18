// Los nombres en español de los 10 roles de la matriz D-16, para mostrarlos al usuario. El token y
// la API usan los identificadores (ADR-014); este es el único lugar que los traduce. Un rol creado
// por el administrador solo viaja con su identificador, así que se muestra legible y sin tildes
// (research §9 de F05-H02).
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

/** El nombre del rol para el usuario; uno que no es de D-16, como «AUDITOR_DE_ROLES» → «Auditor de roles». */
export function nombreDelRol(identificador: string): string {
  return NOMBRES[identificador] ?? legible(identificador);
}

function legible(identificador: string): string {
  const texto = identificador.toLowerCase().replaceAll('_', ' ');
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}
