// Respuestas de la API de Generación para las pruebas de «Consolidado del periodo». Datos ficticios
// (repositorio público).
import type { ConsolidadoDelPeriodo, InformeSustentado } from './consultas';

const HORAS = { clase: 40, otrasDocencia: 12, investigacion: 4, vinculacion: 2, gestion: 6 };

export const INFORME: InformeSustentado = {
  nombre: 'informe-2026.pdf',
  tamano: 2_097_152,
  huella: 'a'.repeat(64),
  por: 'consolidacion.demo',
  en: '2026-11-02T15:10:00Z',
};

export const SIN_CONSOLIDAR: ConsolidadoDelPeriodo = {
  periodo: '2026-2',
  creado: true,
  modificable: true,
  enRevision: false,
  versionCongelada: false,
  version: 1,
  desactualizado: false,
  carreras: [
    {
      carrera: '550411C02-P-0301',
      estado: 'EN_ELABORACION',
      paralelos: 12,
      propuestos: 9,
      completo: false,
      horas: HORAS,
    },
  ],
  totales: HORAS,
  abiertas: { duras: 3, blandas: 1 },
};

export const CONSOLIDADO: ConsolidadoDelPeriodo = {
  ...SIN_CONSOLIDAR,
  consolidacion: { por: 'consolidacion.demo', en: '2026-11-02T15:00:00Z' },
};

export const CON_INFORME: ConsolidadoDelPeriodo = { ...CONSOLIDADO, informe: INFORME };

export const SIN_DISTRIBUTIVO: ConsolidadoDelPeriodo = {
  periodo: '2026-2',
  creado: false,
  modificable: true,
  enRevision: false,
  versionCongelada: false,
  desactualizado: false,
  carreras: [],
  totales: { clase: 0, otrasDocencia: 0, investigacion: 0, vinculacion: 0, gestion: 0 },
  abiertas: { duras: 0, blandas: 0 },
};
