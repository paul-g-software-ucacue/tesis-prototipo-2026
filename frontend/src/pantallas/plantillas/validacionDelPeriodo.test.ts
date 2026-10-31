// El periodo de «sin proyectos» con valores válidos e inválidos (principio XIV, F01-H05).
import { describe, expect, it } from 'vitest';
import { motivoDelPeriodo } from './validacionDelPeriodo';

describe('El periodo de «sin proyectos»', () => {
  it.each(['2026-I', '2026-II', ' 2027-I ', '003', 'prueba-20.periodo_01'])(
    'acepta %s',
    (periodo) => {
      expect(motivoDelPeriodo(periodo)).toBeNull();
    },
  );

  it('pide escribirlo si está vacío', () => {
    expect(motivoDelPeriodo('   ')).toBe('Escribe el periodo.');
  });

  it.each(['2026 I', '2026/I', 'año-1', '123456789012345678901'])(
    'rechaza %s y dice cómo escribirlo',
    (periodo) => {
      expect(motivoDelPeriodo(periodo)).toMatch(
        /^Escribe el código del periodo como está en «Periodos»/,
      );
    },
  );
});
