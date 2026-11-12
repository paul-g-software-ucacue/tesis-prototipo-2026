import { describe, expect, it } from 'vitest';
import { NOMBRE_DEL_SERVICIO, entradas, formatearMomento, lineas, resumen } from './nombres';

describe('nombres de la bitácora', () => {
  it('nombra los siete servicios, Firma desde F13-H02', () => {
    expect(Object.values(NOMBRE_DEL_SERVICIO)).toEqual([
      'Generación',
      'Académico',
      'Docentes',
      'Normativa',
      'Roles',
      'Validación OCS',
      'Firma',
    ]);
  });

  it('muestra el momento en la hora de Ecuador continental, sin importar la del navegador', () => {
    // 04:59 UTC del 8 es 23:59 del 7 en Ecuador.
    expect(formatearMomento('2026-11-08T04:59:30Z')).toContain('7/11/26');
    expect(formatearMomento('2026-11-08T04:59:30Z')).toContain('23:59:30');
  });

  it('cuenta las entradas', () => {
    expect(entradas(0)).toBe('0 entradas');
    expect(entradas(1)).toBe('1 entrada');
    expect(entradas(1234)).toBe('1234 entradas');
  });

  it('resume el objeto en una línea con sus valores simples', () => {
    expect(resumen({ periodo: '2026-I', docente: '0102030405', cambios: [{ campo: 'x' }] })).toBe(
      'periodo: 2026-I · docente: 0102030405 · cambios: …',
    );
  });

  it('pone cada campo en su línea, con las listas y los objetos anidados con sangría', () => {
    expect(
      lineas({
        horas: '6',
        proyecto: null,
        camposPersonales: ['telefono', 'correoInstitucional'],
        cambios: [{ campo: 'dedicacion', anterior: 'MT', nuevo: 'TC' }],
        textoCambiado: true,
      }),
    ).toEqual([
      { nivel: 0, texto: 'horas: 6' },
      { nivel: 0, texto: 'proyecto: sin valor' },
      { nivel: 0, texto: 'camposPersonales: telefono, correoInstitucional' },
      { nivel: 0, texto: 'cambios:' },
      { nivel: 1, texto: 'campo: dedicacion' },
      { nivel: 1, texto: 'anterior: MT' },
      { nivel: 1, texto: 'nuevo: TC' },
      { nivel: 0, texto: 'textoCambiado: sí' },
    ]);
  });
});
