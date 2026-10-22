import { describe, expect, it } from 'vitest';
import { funcionesDelUsuario, tieneAlguno, tienePermiso } from './permisos';

const COORDINADOR_Y_DOCENTE = [
  'DISTRIBUTIVO:LECTURA:PROPIO',
  'DOCENTES:LECTURA',
  'MI_DISTRIBUTIVO:ESCRITURA:PROPIO',
  'MI_DISTRIBUTIVO:LECTURA:PROPIO',
  'OFERTA:APROBACION:PROPIO',
  'OFERTA:ESCRITURA:PROPIO',
  'OFERTA:LECTURA:PROPIO',
];

describe('permisos de la sesión', () => {
  it('tienePermiso reconoce el permiso con alcance todo o solo lo propio', () => {
    expect(tienePermiso(['USUARIOS_ROLES:LECTURA'], 'USUARIOS_ROLES', 'LECTURA')).toBe(true);
    expect(tienePermiso(['OFERTA:LECTURA:PROPIO'], 'OFERTA', 'LECTURA')).toBe(true);
    expect(tienePermiso(['USUARIOS_ROLES:LECTURA'], 'USUARIOS_ROLES', 'ESCRITURA')).toBe(false);
    expect(tienePermiso(['USUARIOS_ROLES_EXTRA:LECTURA'], 'USUARIOS_ROLES', 'LECTURA')).toBe(false);
    expect(tienePermiso(undefined, 'USUARIOS_ROLES', 'LECTURA')).toBe(false);
  });

  it('tieneAlguno acepta cualquiera de los permisos alternativos (D-70)', () => {
    const lectura = [
      { funcionalidad: 'DISTRIBUTIVO', tipo: 'LECTURA' },
      { funcionalidad: 'CATALOGOS', tipo: 'LECTURA' },
    ] as const;
    expect(tieneAlguno(['DISTRIBUTIVO:LECTURA:PROPIO'], lectura)).toBe(true);
    expect(tieneAlguno(['CATALOGOS:LECTURA'], lectura)).toBe(true);
    expect(tieneAlguno(['CATALOGOS:ESCRITURA', 'PARAMETROS:LECTURA'], lectura)).toBe(false);
    expect(tieneAlguno(undefined, lectura)).toBe(false);
    expect(tieneAlguno(['CATALOGOS:LECTURA'], [])).toBe(false);
  });

  it('las funciones del usuario agrupan sus permisos por funcionalidad, en orden y sin repetir', () => {
    expect(funcionesDelUsuario(COORDINADOR_Y_DOCENTE)).toEqual([
      {
        codigo: 'DOCENTES',
        nombre: 'Docentes',
        tipos: [{ tipo: 'LECTURA', nombre: 'lectura', propio: false }],
      },
      {
        codigo: 'OFERTA',
        nombre: 'Oferta y propuesta de docentes',
        tipos: [
          { tipo: 'LECTURA', nombre: 'lectura', propio: true },
          { tipo: 'ESCRITURA', nombre: 'escritura', propio: true },
          { tipo: 'APROBACION', nombre: 'aprobación', propio: true },
        ],
      },
      {
        codigo: 'DISTRIBUTIVO',
        nombre: 'Distributivo del periodo',
        tipos: [{ tipo: 'LECTURA', nombre: 'lectura', propio: true }],
      },
      {
        codigo: 'MI_DISTRIBUTIVO',
        nombre: 'Mi distributivo',
        tipos: [
          { tipo: 'LECTURA', nombre: 'lectura', propio: true },
          { tipo: 'ESCRITURA', nombre: 'escritura', propio: true },
        ],
      },
    ]);
  });

  it('un permiso de una funcionalidad que la SPA no conoce se muestra con su código', () => {
    expect(funcionesDelUsuario(['NUEVA_FUNCION:LECTURA'])).toEqual([
      {
        codigo: 'NUEVA_FUNCION',
        nombre: 'NUEVA_FUNCION',
        tipos: [{ tipo: 'LECTURA', nombre: 'lectura', propio: false }],
      },
    ]);
  });

  it('sin permisos, no hay funciones', () => {
    expect(funcionesDelUsuario(undefined)).toEqual([]);
  });
});
