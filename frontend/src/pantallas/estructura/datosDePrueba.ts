// Respuestas de la API de Académico para las pruebas de «Estructura orgánica» (contratos/academico):
// una estructura chica con la sede matriz, el Rectorado (G01), una gestión, una comisión permanente
// y una coordinación inactiva, por código como las devuelve la API, y parte del catálogo.
export const UNIDADES = [
  {
    codigo: 'CC-DS',
    tipo: 'UNIDAD_ACADEMICA',
    nombre: 'Coordinación de la carrera de Desarrollo de Software',
    padre: 'RECT',
    activa: false,
  },
  {
    codigo: 'COM-EVAL',
    tipo: 'COMISION',
    nombre: 'Comisión de Evaluación Interna',
    padre: 'G-CAL',
    comisionPermanente: true,
    activa: true,
  },
  {
    codigo: 'G-CAL',
    tipo: 'GESTION',
    nombre: 'Coordinación de Aseguramiento de la Calidad',
    padre: 'RECT',
    gestionAutorizada: 'G10',
    activa: true,
  },
  { codigo: 'MATRIZ', tipo: 'SEDE_MATRIZ', nombre: 'Sede matriz', activa: true },
  {
    codigo: 'RECT',
    tipo: 'GOBIERNO',
    nombre: 'Rectorado',
    padre: 'MATRIZ',
    gestionAutorizada: 'G01',
    activa: true,
  },
];

export const GESTIONES = [
  { codigo: 'G01', nombre: 'Rector' },
  { codigo: 'G07', nombre: 'Gestor de Investigación, Desarrollo Tecnológico e Innovación' },
  { codigo: 'G10', nombre: 'Coordinador de Aseguramiento de la Calidad' },
];
