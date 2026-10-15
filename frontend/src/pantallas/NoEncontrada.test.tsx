import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { montarSpa } from '../pruebas/montarSpa';

describe('NoEncontrada', () => {
  it('dice que la página no existe y ofrece volver a Inicio', () => {
    montarSpa('/una/ruta/que/no/existe');

    expect(
      screen.getByRole('heading', { level: 1, name: 'Página no encontrada' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Volver a Inicio' })).toHaveAttribute('href', '/');
    expect(document.title).toBe('Página no encontrada · MDD');
  });
});
