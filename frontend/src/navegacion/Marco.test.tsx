import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json } from '../pruebas/apiSimulada';
import { montarSpa } from '../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion, obtenerToken } from '../sesion/almacen';

describe('Marco', () => {
  beforeEach(() => {
    instalarApiSimulada().responder(
      'GET',
      '/api/usuarios/sesion',
      json(200, { usuario: 'autor.prueba', roles: ['DOCENTE'] }),
    );
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion();
  });

  it('tiene el encabezado con el usuario, el menú con Inicio y la región principal', async () => {
    montarSpa('/');

    expect(screen.getByRole('banner')).toHaveTextContent('MDD · Distributivo docente');
    expect(
      await screen.findByText('autor.prueba', { selector: '[data-usuario]' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Menú principal' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Inicio' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('main')).toBeInTheDocument();
  });

  it('Cerrar sesión borra el token y vuelve al inicio de sesión', async () => {
    const { enrutador } = montarSpa('/');

    await userEvent.setup().click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    await waitFor(() => expect(enrutador.state.location.pathname).toBe('/iniciar-sesion'));
    expect(obtenerToken()).toBeNull();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
