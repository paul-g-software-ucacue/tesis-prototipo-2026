// Un diccionario de datos pequeño para las pruebas de «Plantillas de carga»: las 10 cargas de la
// ERS §8.3 con pocas columnas; la estructura orgánica, con su columna de gestiones autorizadas.
import type { DiccionarioDeDatos } from './consultas';

const NOMBRES = [
  'Estructura orgánica',
  'Carreras',
  'Ciclos académicos aprobados',
  'Asignaturas y malla',
  'Oferta del periodo',
  'Docentes contratados (con ficha académica)',
  'Horas según la política de contratación',
  'Cargos y autoridades',
  'Proyectos y prácticas aprobados',
  'Matriculados por carrera',
];

const CODIGOS = [
  'estructura_organica',
  'carreras',
  'ciclos_academicos',
  'asignaturas_malla',
  'oferta_periodo',
  'docentes',
  'politica_carga_horaria',
  'cargos_autoridades',
  'proyectos_practicas',
  'matriculados_carrera',
];

const DEPENDE_DE = [[], [1], [], [2], [3, 4], [1, 2], [], [1, 2, 6], [2, 3, 6], [2, 3]];

export const DICCIONARIO: DiccionarioDeDatos = {
  convenciones: [
    { tema: 'Fechas', regla: 'aaaa-mm-dd (ISO 8601).' },
    { tema: 'Booleanos', regla: 'S o N.' },
  ],
  cargas: NOMBRES.map((nombre, indice) => {
    const numero = indice + 1;
    const codigo = CODIGOS[indice] ?? '';
    return {
      numero,
      codigo,
      nombre,
      archivo: `${String(numero).padStart(2, '0')}_${codigo}.csv`,
      dependeDe: DEPENDE_DE[indice] ?? [],
      obligatoriedad:
        numero === 9
          ? 'OBLIGATORIA_SALVO_SIN_PROYECTOS'
          : numero === 10
            ? 'RECOMENDADA'
            : 'OBLIGATORIA',
      columnas:
        numero === 1
          ? [
              {
                campo: 'codigo_unidad',
                descripcion: 'Código único de la unidad',
                tipo: 'Texto',
                longitud: 20,
                obligatorio: 'Sí',
                formato: 'Mayúsculas, dígitos, «-» o «_»',
                clave: 'PK',
                validacion: 'Único en el archivo',
                ejemplo: 'MATRIZ',
              },
              {
                campo: 'codigo_gestion_autorizada',
                descripcion: 'Gestión autorizada por SENESCYT que representa',
                tipo: 'Catálogo',
                longitud: 3,
                obligatorio: 'Sí si tipo_unidad = GESTION',
                formato: 'G01–G21 (ver los valores permitidos)',
                validacion: 'Solo las 21 gestiones de POL 6',
                ejemplo: 'G07',
                valores: [
                  { codigo: 'G01', descripcion: 'Rector' },
                  { codigo: 'G02', descripcion: 'Vicerrector' },
                ],
              },
            ]
          : [
              {
                campo: 'codigo',
                descripcion: `Código de ${nombre}`,
                tipo: 'Texto',
                obligatorio: 'Sí',
                ejemplo: 'A1',
              },
            ],
    };
  }),
};
