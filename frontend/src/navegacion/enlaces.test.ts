// Los enlaces de las notificaciones (F16-H01, contracts/eventos.md): cada etapa de llegada lleva a
// una ruta de la SPA que el rol de esa etapa, en la configuración por defecto, puede abrir. Si un
// rol no puede abrir su enlace, se corrige la tabla del enlace (MensajesDeTransicion), no la
// matriz D-16.
import { isValidElement, type ReactElement } from 'react';
import type { RouteObject } from 'react-router';
import { describe, expect, it } from 'vitest';
import { rutasDeLaSpa } from '../rutas';
import { tieneAlguno, type PermisoRequerido } from '../sesion/permisos';

// La tabla de contracts/eventos.md, igual que MensajesDeTransicion.ENLACES en Validación OCS.
const ENLACES: Record<string, string> = {
  BORRADOR_CARRERA: '/oferta',
  BORRADOR_UNIDADES: '/',
  EN_CONSOLIDACION: '/consolidado',
  EN_REVISION_VRA: '/consolidado',
  CONGELADO_EN_FIRMA: '/firmas-pendientes',
  EN_OCS: '/resolucion-ocs',
  REMITIDO_ZONAL: '/consolidado',
  APROBADO: '/resolucion-ocs',
  PUBLICADO: '/resolucion-ocs',
};

// El rol de cada etapa en flujo-por-defecto.yaml.
const ROL_DE_LA_ETAPA: Record<string, string> = {
  BORRADOR_CARRERA: 'COORDINADOR_CARRERA',
  BORRADOR_UNIDADES: 'GESTOR_UNIDAD',
  EN_CONSOLIDACION: 'RESPONSABLE_CONSOLIDACION',
  EN_REVISION_VRA: 'VICERRECTOR',
  CONGELADO_EN_FIRMA: 'RESPONSABLE_CONSOLIDACION',
  EN_OCS: 'SECRETARIA_GENERAL',
  REMITIDO_ZONAL: 'VICERRECTOR',
  APROBADO: 'SECRETARIA_GENERAL',
  PUBLICADO: 'SECRETARIA_GENERAL',
};

// Los permisos de esos roles en la matriz D-16 (roles V2, V4, V5, V7 y V9), sin el alcance. El
// Rector firma F3 (F16-H02).
const PERMISOS: Record<string, readonly string[]> = {
  COORDINADOR_CARRERA: [
    'OFERTA:LECTURA',
    'DISTRIBUTIVO:LECTURA',
    'DOCENTES:LECTURA',
    'OFERTA:ESCRITURA',
    'OFERTA:APROBACION',
  ],
  GESTOR_UNIDAD: ['PROYECTOS:LECTURA', 'PROYECTOS:ESCRITURA', 'PROYECTOS:APROBACION'],
  RESPONSABLE_CONSOLIDACION: [
    'DISTRIBUTIVO:LECTURA',
    'OFERTA:LECTURA',
    'PROYECTOS:LECTURA',
    'CONSOLIDACION:LECTURA',
    'CONSOLIDACION:ESCRITURA',
    'CONSOLIDACION:APROBACION',
    'DISTRIBUTIVO:FIRMA',
    'CARGAS:LECTURA',
    'CARGAS:ESCRITURA',
  ],
  VICERRECTOR: [
    'DISTRIBUTIVO:LECTURA',
    'REPORTES:LECTURA',
    'BITACORA:LECTURA',
    'DISTRIBUTIVO:ESCRITURA',
    'DISTRIBUTIVO:APROBACION',
    'DISTRIBUTIVO:FIRMA',
    'PARAMETROS:LECTURA',
    'PARAMETROS:ESCRITURA',
    'CARGAS:LECTURA',
    'CARGAS:ESCRITURA',
    'RESOLUCION_OCS:LECTURA',
  ],
  RECTOR: [
    'DISTRIBUTIVO:LECTURA',
    'REPORTES:LECTURA',
    'DISTRIBUTIVO:APROBACION',
    'DISTRIBUTIVO:FIRMA',
    'RESOLUCION_OCS:LECTURA',
  ],
  SECRETARIA_GENERAL: [
    'DISTRIBUTIVO:LECTURA',
    'REPORTES:LECTURA',
    'RESOLUCION_OCS:LECTURA',
    'RESOLUCION_OCS:ESCRITURA',
    'PUBLICACION:APROBACION',
  ],
};

interface Propiedades {
  cualquieraDe?: readonly PermisoRequerido[];
  funcionalidad?: string;
  tipo?: PermisoRequerido['tipo'];
}

// La ruta del marco con ese camino y lo que exige su RutaConPermiso; null si no exige nada.
function requeridosDe(enlace: string): readonly PermisoRequerido[] | null | undefined {
  const marco = rutasDeLaSpa.find((ruta) => ruta.children);
  const hijas: RouteObject[] = marco?.children ?? [];
  const ruta =
    enlace === '/'
      ? hijas.find((una) => una.index)
      : hijas.find((una) => una.path === enlace.slice(1));
  if (!ruta) {
    return undefined;
  }
  const elemento = ruta.element;
  if (!isValidElement(elemento)) {
    return null;
  }
  const { cualquieraDe, funcionalidad, tipo } = (elemento as ReactElement<Propiedades>).props;
  if (cualquieraDe) {
    return cualquieraDe;
  }
  if (funcionalidad && tipo) {
    return [{ funcionalidad, tipo }];
  }
  return null;
}

describe('Los enlaces de las notificaciones (F16-H01)', () => {
  it.each(Object.entries(ENLACES))('%s lleva a %s, una ruta de la SPA', (_etapa, enlace) => {
    expect(requeridosDe(enlace)).not.toBeUndefined();
  });

  it.each(Object.entries(ENLACES))('el rol por defecto de %s puede abrir %s', (etapa, enlace) => {
    const rol = ROL_DE_LA_ETAPA[etapa] ?? '';
    const requeridos = requeridosDe(enlace);

    expect(requeridos === null || tieneAlguno(PERMISOS[rol], requeridos ?? [])).toBe(true);
  });

  // F16-H02, D-110: la notificación de firma pendiente lleva a «Pendientes de firma».
  it.each(['VICERRECTOR', 'RECTOR'])(
    '%s, firmante de F2 o F3, puede abrir /firmas-pendientes, el enlace de su firma pendiente',
    (rol) => {
      expect(tieneAlguno(PERMISOS[rol], requeridosDe('/firmas-pendientes') ?? [])).toBe(true);
    },
  );

  it('el coordinador de una carrera devuelta puede abrir /oferta, el enlace de la devolución', () => {
    expect(tieneAlguno(PERMISOS.COORDINADOR_CARRERA, requeridosDe('/oferta') ?? [])).toBe(true);
  });
});
