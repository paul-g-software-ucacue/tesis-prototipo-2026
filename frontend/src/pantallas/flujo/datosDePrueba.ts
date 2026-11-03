// Respuestas de la API de Validación OCS para las pruebas de «Flujo y firmas» (F12-H01): la
// configuración por defecto, una cargada y una de un periodo en curso.
import type { ConfiguracionDelFlujo } from './consultas';

const ETAPAS: ConfiguracionDelFlujo['etapas'] = [
  {
    tipo: 'BORRADOR_CARRERA',
    esqueleto: true,
    activa: true,
    nombre: 'Propuesta de carrera',
    rol: 'COORDINADOR_CARRERA',
    plazoDias: 15,
    admiteDevolucion: false,
  },
  {
    tipo: 'BORRADOR_UNIDADES',
    esqueleto: false,
    activa: false,
    nombre: 'Planificación de investigación y vinculación',
    rol: 'GESTOR_UNIDAD',
    plazoDias: 10,
    admiteDevolucion: false,
  },
  {
    tipo: 'CONGELADO_EN_FIRMA',
    esqueleto: true,
    activa: true,
    nombre: 'En firma',
    rol: 'RESPONSABLE_CONSOLIDACION',
    plazoDias: 5,
    admiteDevolucion: true,
  },
  {
    tipo: 'PUBLICADO',
    esqueleto: true,
    activa: true,
    nombre: 'Publicado',
    rol: 'SECRETARIA_GENERAL',
    admiteDevolucion: false,
  },
];

const FIRMAS: ConfiguracionDelFlujo['firmas'] = [
  {
    orden: 1,
    etiqueta: 'Elaborado',
    rol: 'RESPONSABLE_CONSOLIDACION',
    etapa: 'CONGELADO_EN_FIRMA',
    documento: 'CONSOLIDADO',
  },
  {
    orden: 2,
    etiqueta: 'Aprobado',
    rol: 'RECTOR',
    etapa: 'CONGELADO_EN_FIRMA',
    documento: 'CONSOLIDADO',
  },
];

export const POR_DEFECTO: ConfiguracionDelFlujo = {
  periodo: '2026-2',
  porDefecto: true,
  modificable: true,
  etapas: ETAPAS,
  firmas: FIRMAS,
};

export const CARGADA: ConfiguracionDelFlujo = {
  ...POR_DEFECTO,
  porDefecto: false,
  version: 2,
  cargada: { por: 'admin.demo', en: '2026-11-02T15:00:00Z' },
};

export const EN_CURSO: ConfiguracionDelFlujo = { ...CARGADA, modificable: false };
