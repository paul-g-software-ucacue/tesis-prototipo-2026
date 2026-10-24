// Las funcionalidades del MDD y los tipos de permiso en español (D-65), para mostrarlos al usuario.
// El token y la API usan los códigos; la pantalla «Roles» toma los nombres de la API, y «Tus
// funciones» de aquí, en el mismo orden que contratos/roles.
import type { components } from '../api/generado/roles';

export type TipoDePermiso = components['schemas']['TipoDePermiso'];

/** Los tipos de permiso en su orden, con su nombre en minúsculas (RF-02). */
export const TIPOS_DE_PERMISO: readonly { tipo: TipoDePermiso; nombre: string }[] = [
  { tipo: 'LECTURA', nombre: 'lectura' },
  { tipo: 'ESCRITURA', nombre: 'escritura' },
  { tipo: 'APROBACION', nombre: 'aprobación' },
  { tipo: 'FIRMA', nombre: 'firma' },
];

/** Las 16 funcionalidades de V2__matriz_d16.sql, en su orden. */
export const FUNCIONALIDADES: readonly { codigo: string; nombre: string }[] = [
  { codigo: 'USUARIOS_ROLES', nombre: 'Usuarios y roles' },
  { codigo: 'PARAMETROS', nombre: 'Parámetros y normativa' },
  { codigo: 'CATALOGOS', nombre: 'Catálogos y datos maestros' },
  { codigo: 'FLUJO_FIRMAS', nombre: 'Flujo y árbol de firmas' },
  { codigo: 'BITACORA', nombre: 'Bitácora' },
  { codigo: 'CARGAS', nombre: 'Cargas de datos' },
  { codigo: 'DOCENTES', nombre: 'Docentes' },
  { codigo: 'OFERTA', nombre: 'Oferta y propuesta de docentes' },
  { codigo: 'PROYECTOS', nombre: 'Proyectos y horas de participantes' },
  { codigo: 'CONSOLIDACION', nombre: 'Consolidación e informe sustentado' },
  { codigo: 'DISTRIBUTIVO', nombre: 'Distributivo del periodo' },
  { codigo: 'REPORTES', nombre: 'Reportes' },
  { codigo: 'VOTACION_OCS', nombre: 'Votación del OCS' },
  { codigo: 'RESOLUCION_OCS', nombre: 'Resolución del OCS' },
  { codigo: 'PUBLICACION', nombre: 'Publicación del distributivo' },
  { codigo: 'MI_DISTRIBUTIVO', nombre: 'Mi distributivo' },
];

/** El nombre en minúsculas de un tipo de permiso. */
export function nombreDelTipo(tipo: TipoDePermiso): string {
  return TIPOS_DE_PERMISO.find((candidato) => candidato.tipo === tipo)?.nombre ?? tipo;
}

/**
 * Quién lee la estructura orgánica y las carreras y mallas: quien lee el distributivo o los
 * catálogos (D-70, F03-H02). Lo comparten las rutas y las entradas del menú.
 */
export const LECTURA_DE_LA_ESTRUCTURA: readonly { funcionalidad: string; tipo: TipoDePermiso }[] = [
  { funcionalidad: 'DISTRIBUTIVO', tipo: 'LECTURA' },
  { funcionalidad: 'CATALOGOS', tipo: 'LECTURA' },
];

/**
 * Quién lee la oferta del periodo: quien lee la oferta, el distributivo o los catálogos (D-70,
 * F03-H03). La mantienen los mismos que las carreras.
 */
export const LECTURA_DE_LA_OFERTA: readonly { funcionalidad: string; tipo: TipoDePermiso }[] = [
  { funcionalidad: 'OFERTA', tipo: 'LECTURA' },
  { funcionalidad: 'DISTRIBUTIVO', tipo: 'LECTURA' },
  { funcionalidad: 'CATALOGOS', tipo: 'LECTURA' },
];

/**
 * Quién mantiene las carreras, sus mallas y sus asignaturas: el Administrador o el Vicerrector
 * (D-70, F03-H02).
 */
export const ESCRITURA_DE_LAS_CARRERAS: readonly { funcionalidad: string; tipo: TipoDePermiso }[] =
  [
    { funcionalidad: 'CATALOGOS', tipo: 'ESCRITURA' },
    { funcionalidad: 'DISTRIBUTIVO', tipo: 'ESCRITURA' },
  ];

/**
 * Quién descarga las plantillas de carga: quien lee las cargas o el distributivo (D-70, F01-H01).
 * Lo comparten la ruta y la entrada del menú.
 */
export const DESCARGA_DE_PLANTILLAS: readonly { funcionalidad: string; tipo: TipoDePermiso }[] = [
  { funcionalidad: 'CARGAS', tipo: 'LECTURA' },
  { funcionalidad: 'DISTRIBUTIVO', tipo: 'LECTURA' },
];
