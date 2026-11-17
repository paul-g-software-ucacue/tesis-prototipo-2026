// El periodo de las pantallas con selector (F16-H01, D-108): el de la dirección, que pone una
// notificación; si no, el de por omisión.
import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter, useLocation } from 'react-router';
import { describe, expect, it } from 'vitest';
import type { PeriodoAcademico } from './consultas';
import { PERIODOS } from './datosDePrueba';
import { usePeriodoElegido } from './periodoElegido';

function montar(direccion: string, conPeriodos = true) {
  // Los datos de prueba son los de la API simulada, sin el tipo del contrato.
  const periodos = conPeriodos ? (PERIODOS as PeriodoAcademico[]) : undefined;
  const envoltura = ({ children }: { children: ReactNode }) => (
    <MemoryRouter initialEntries={[direccion]}>{children}</MemoryRouter>
  );
  return renderHook(() => ({ elegido: usePeriodoElegido(periodos), ubicacion: useLocation() }), {
    wrapper: envoltura,
  });
}

describe('El periodo elegido (F16-H01)', () => {
  it('sin periodo en la dirección, el más reciente en Planificación', () => {
    const { result } = montar('/consolidado');

    expect(result.current.elegido[0]).toBe('2026-2');
  });

  it('con un periodo en la dirección, ese', () => {
    const { result } = montar('/consolidado?periodo=2026-1');

    expect(result.current.elegido[0]).toBe('2026-1');
  });

  it('con un periodo que no existe en la dirección, el de por omisión', () => {
    const { result } = montar('/consolidado?periodo=NADA');

    expect(result.current.elegido[0]).toBe('2026-2');
  });

  it('elegir otro lo cambia y lo deja en la dirección', () => {
    const { result } = montar('/consolidado?periodo=2026-1');

    act(() => result.current.elegido[1]('2026-2'));

    expect(result.current.elegido[0]).toBe('2026-2');
    expect(result.current.ubicacion.search).toBe('?periodo=2026-2');
  });

  it('mientras no llegan los periodos, ninguno', () => {
    const { result } = montar('/consolidado?periodo=2026-1', false);

    expect(result.current.elegido[0]).toBeUndefined();
  });
});
