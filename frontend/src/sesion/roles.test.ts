import { describe, expect, it } from 'vitest';
import { nombreDelRol } from './roles';

describe('nombres de los roles', () => {
  it('traduce los 10 roles de D-16 a su nombre en español', () => {
    expect(nombreDelRol('COORDINADOR_CARRERA')).toBe('Coordinador de carrera');
    expect(nombreDelRol('VICERRECTOR')).toBe('Vicerrector académico');
    expect(nombreDelRol('SECRETARIA_GENERAL')).toBe('Secretaría general académica');
    expect(
      [
        'ADMINISTRADOR',
        'OPERADOR_DATOS',
        'COORDINADOR_CARRERA',
        'GESTOR_UNIDAD',
        'RESPONSABLE_CONSOLIDACION',
        'VICERRECTOR',
        'RECTOR',
        'MIEMBRO_OCS',
        'SECRETARIA_GENERAL',
        'DOCENTE',
      ].map(nombreDelRol),
    ).not.toContain(expect.stringMatching(/^[A-Z_]+$/));
  });

  it('un identificador desconocido se muestra tal cual', () => {
    expect(nombreDelRol('ROL_NUEVO')).toBe('ROL_NUEVO');
  });
});
