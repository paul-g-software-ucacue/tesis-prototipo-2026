import { screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json } from './pruebas/apiSimulada';
import { montarSpa } from './pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from './sesion/almacen';

describe('App', () => {
  afterEach(() => {
    cerrarSesion();
  });

  it('sin sesión, Inicio lleva al inicio de sesión con el destino (D-50)', async () => {
    instalarApiSimulada();
    const { enrutador } = montarSpa('/');

    await waitFor(() => expect(enrutador.state.location.pathname).toBe('/iniciar-sesion'));
    expect(enrutador.state.location.search).toBe('?destino=%2F');
  });

  it('un 401 de la sesión actual cierra la sesión y lo dice en el inicio de sesión', async () => {
    instalarApiSimulada().responder(
      'GET',
      '/api/usuarios/sesion',
      json(401, {
        titulo: 'Sesión no válida',
        descripcion: 'Inicia sesión de nuevo para continuar.',
        codigo: 'SEGURIDAD_NO_AUTENTICADO',
      }),
    );
    iniciarSesion('vencido', new Date(Date.now() + 60_000));
    const { enrutador } = montarSpa('/');

    await waitFor(() => expect(enrutador.state.location.pathname).toBe('/iniciar-sesion'));
    expect(await screen.findByRole('alert')).toHaveTextContent('Tu sesión terminó.');
  });
});
