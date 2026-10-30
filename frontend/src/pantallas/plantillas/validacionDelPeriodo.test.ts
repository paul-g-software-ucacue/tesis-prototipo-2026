// El periodo de «sin proyectos» con valores válidos e inválidos (principio XIV, F01-H05).
import { describe, expect, it } from 'vitest';
import { motivoDelPeriodo } from './validacionDelPeriodo';

describe('El periodo de «sin proyectos»', () => {
  it.each(['2026-I', '2026-II', ' 2027-I '])('acepta %s', (periodo) => {
    expect(motivoDelPeriodo(periodo)).toBeNull();
  });

  it('pide escribirlo si está vacío', () => {
    expect(motivoDelPeriodo('   ')).toBe('Escribe el periodo.');
  });

  it.each(['2026-III', '26-I', '2026-i', '2026_I', '2026-I1', '20260-II', 'I-2026'])(
    'rechaza %s con un ejemplo de cómo escribirlo',
    (periodo) => {
      expect(motivoDelPeriodo(periodo)).toBe('Escribe el periodo como 2026-I o 2026-II.');
    },
  );
});
