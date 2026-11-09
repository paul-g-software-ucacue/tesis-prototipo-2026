// Respuestas de la API de Generación para las pruebas de «Panel de inconsistencias». Datos
// ficticios (repositorio público).
import type { InconsistenciaDelPanel, PanelDeInconsistencias } from './consultas';

const SIN_EVALUAR = [
  'RN-03',
  'RN-04',
  'RN-07',
  'RN-08',
  'RN-09',
  'RN-11',
  'RN-14',
  'RN-15',
  'RN-16',
];

export const SIN_PROPUESTA: InconsistenciaDelPanel = {
  tipo: 'PARALELO_SIN_PROPUESTA',
  regla: 'RF-24',
  severidad: 'DURA',
  estado: 'ABIERTA',
  objeto: { clase: 'PARALELO', clave: 'POO-204/A', nombre: 'Programación · A' },
  detalle: 'Falta el docente de 4 h de clase.',
  indicacion: 'El coordinador de la carrera debe proponer un docente.',
  pantalla: 'OFERTA',
};

export const SIN_COORDINADOR: InconsistenciaDelPanel = {
  tipo: 'COORDINADORES_DE_LA_CARRERA',
  regla: 'RN-12',
  severidad: 'DURA',
  estado: 'ABIERTA',
  objeto: { clase: 'CARRERA', clave: '550411C02-P-0301', nombre: '550411C02-P-0301' },
  detalle: 'La carrera no tiene coordinador.',
  indicacion: 'El Vicerrectorado debe dejar un solo coordinador vigente en esta carrera.',
  pantalla: 'CARGOS',
};

export const PROPORCION: InconsistenciaDelPanel = {
  tipo: 'PROPORCION',
  regla: 'RN-05',
  severidad: 'BLANDA',
  estado: 'ABIERTA',
  objeto: { clase: 'DOCENTE', clave: '0109990010', nombre: 'PEREZ LUIS' },
  detalle: 'Tiene 72 % de clase y 28 % de otras actividades.',
  indicacion:
    'El coordinador de la carrera debe ajustar sus horas. Consolidación puede justificarla aquí.',
  pantalla: 'ACTIVIDADES',
};

export const GESTORES: InconsistenciaDelPanel = {
  tipo: 'GESTORES_FUERA_DE_RANGO',
  regla: 'RN-10',
  severidad: 'BLANDA',
  estado: 'JUSTIFICADA',
  objeto: { clase: 'PERIODO', clave: '2026-2', nombre: '2026-2' },
  detalle: 'Hay 7 docentes con cargo de gestión; la normativa admite de 8 a 11.',
  indicacion:
    'El Vicerrectorado debe revisar los gestores designados. Consolidación puede justificarla aquí.',
  pantalla: 'CARGOS',
  justificacion: {
    texto: 'El instituto es pequeño este año',
    por: 'consolidacion.demo',
    en: '2026-11-02T15:00:00Z',
  },
};

export const EXCEPCION: InconsistenciaDelPanel = {
  tipo: 'EXCEPCION_DE_AFINIDAD',
  regla: 'RN-19',
  severidad: 'BLANDA',
  estado: 'JUSTIFICADA',
  objeto: { clase: 'PARALELO', clave: 'BDD-101/B', nombre: 'Bases de datos · B' },
  detalle: 'Propuesta bajo el mínimo de afinidad: LOPEZ ANA.',
  indicacion: 'Ya está justificada en su propuesta.',
  pantalla: 'OFERTA',
  justificacion: {
    texto: 'Dictó esta materia durante tres años',
    por: 'coordinacion.demo',
    en: '2026-11-01T15:00:00Z',
  },
};

export const PANEL: PanelDeInconsistencias = {
  periodo: '2026-2',
  creado: true,
  modificable: true,
  enRevision: false,
  version: 1,
  abiertas: { duras: 2, blandas: 1 },
  inconsistencias: [SIN_PROPUESTA, SIN_COORDINADOR, PROPORCION, GESTORES, EXCEPCION],
  sinEvaluar: SIN_EVALUAR,
};

export const SIN_ABIERTAS: PanelDeInconsistencias = {
  ...PANEL,
  abiertas: { duras: 0, blandas: 0 },
  inconsistencias: [],
};

export const SIN_DISTRIBUTIVO: PanelDeInconsistencias = {
  periodo: '2026-2',
  creado: false,
  modificable: true,
  enRevision: false,
  abiertas: { duras: 0, blandas: 0 },
  inconsistencias: [],
  sinEvaluar: SIN_EVALUAR,
};
