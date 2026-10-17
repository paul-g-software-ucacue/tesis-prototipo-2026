import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import {
  CREDENCIALES_INVALIDAS,
  TOKEN_CORRECTO,
  instalarApiSimulada,
  json,
  type ApiSimulada,
} from '../pruebas/apiSimulada';
import { montarSpa } from '../pruebas/montarSpa';
import { cerrarSesion, obtenerToken } from '../sesion/almacen';

describe('IniciarSesion', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder(
      'GET',
      '/api/usuarios/sesion',
      json(200, { usuario: 'autor.prueba', roles: ['DOCENTE'] }),
    );
  });

  afterEach(() => {
    cerrarSesion();
  });

  async function ingresar(usuario: string, clave: string) {
    const persona = userEvent.setup();
    if (usuario) await persona.type(screen.getByLabelText(/usuario/i), usuario);
    if (clave) await persona.type(screen.getByLabelText(/contraseña/i), clave);
    await persona.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
  }

  it('tiene un título, sus campos con etiqueta y el título del documento', () => {
    montarSpa('/iniciar-sesion');

    expect(screen.getByRole('heading', { level: 1, name: 'Iniciar sesión' })).toBeInTheDocument();
    expect(screen.getByLabelText(/usuario/i)).toHaveAttribute('autocomplete', 'username');
    expect(screen.getByLabelText(/contraseña/i)).toHaveAttribute('type', 'password');
    expect(document.title).toBe('Iniciar sesión · MDD');
  });

  it('con los campos vacíos, dice qué falta sin llamar a la API', async () => {
    montarSpa('/iniciar-sesion');

    await ingresar('', '');

    expect(await screen.findByText('Ingresa tu usuario.')).toBeInTheDocument();
    expect(screen.getByText('Ingresa tu contraseña.')).toBeInTheDocument();
    expect(simulada.peticiones).toHaveLength(0);
  });

  it('con credenciales correctas, guarda el token y vuelve al destino pedido', async () => {
    simulada.responder('POST', '/api/usuarios/sesion', json(200, TOKEN_CORRECTO));
    const { enrutador } = montarSpa('/iniciar-sesion?destino=%2F');

    await ingresar('autor.prueba', 'clave-correcta');

    await waitFor(() => expect(enrutador.state.location.pathname).toBe('/'));
    expect(obtenerToken()).toBe(TOKEN_CORRECTO.token);
    expect(await simulada.peticiones[0]?.json()).toEqual({
      usuario: 'autor.prueba',
      clave: 'clave-correcta',
    });
  });

  it('un destino externo se ignora y se va a Inicio', async () => {
    simulada.responder('POST', '/api/usuarios/sesion', json(200, TOKEN_CORRECTO));
    const { enrutador } = montarSpa('/iniciar-sesion?destino=%2F%2Fotro.example');

    await ingresar('autor.prueba', 'clave-correcta');

    await waitFor(() => expect(enrutador.state.location.pathname).toBe('/'));
  });

  it('con credenciales inválidas, muestra el error del modelo común sin el código', async () => {
    simulada.responder('POST', '/api/usuarios/sesion', json(401, CREDENCIALES_INVALIDAS));
    montarSpa('/iniciar-sesion');

    await ingresar('autor.prueba', 'incorrecta');

    const aviso = await screen.findByRole('alert');
    expect(aviso).toHaveTextContent('Credenciales inválidas');
    expect(aviso).toHaveTextContent('El usuario o la contraseña no son correctos.');
    expect(document.body).not.toHaveTextContent('USUARIOS_');
    expect(document.body).not.toHaveTextContent('401');
    expect(obtenerToken()).toBeNull();
  });

  it('con un 429 por demasiados intentos, muestra el error del servidor sin el código', async () => {
    simulada.responder(
      'POST',
      '/api/usuarios/sesion',
      json(429, {
        titulo: 'Demasiados intentos',
        descripcion: 'Espera unos minutos y vuelve a intentarlo.',
        codigo: 'USUARIOS_INICIO_BLOQUEADO',
      }),
    );
    montarSpa('/iniciar-sesion');

    await ingresar('autor.prueba', 'incorrecta');

    const aviso = await screen.findByRole('alert');
    expect(aviso).toHaveTextContent('Demasiados intentos');
    expect(aviso).toHaveTextContent('Espera unos minutos y vuelve a intentarlo.');
    expect(document.body).not.toHaveTextContent('USUARIOS_');
    expect(document.body).not.toHaveTextContent('429');
    expect(obtenerToken()).toBeNull();
  });

  it('con un 400 con detalles, muestra cada detalle bajo su campo', async () => {
    simulada.responder(
      'POST',
      '/api/usuarios/sesion',
      json(400, {
        titulo: 'Solicitud inválida',
        descripcion: 'Revisa los campos marcados y vuelve a intentarlo.',
        codigo: 'COMUN_SOLICITUD_INVALIDA',
        detalles: [{ campo: 'clave', descripcion: 'es obligatorio' }],
      }),
    );
    montarSpa('/iniciar-sesion');

    await ingresar('autor.prueba', 'x');

    expect(await screen.findByRole('alert')).toHaveTextContent('Solicitud inválida');
    expect(screen.getByLabelText(/contraseña/i)).toHaveAccessibleDescription('es obligatorio');
  });

  it('si la sesión terminó, lo dice al llegar', () => {
    montarSpa('/iniciar-sesion?motivo=sesion-terminada');

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Tu sesión terminó. Inicia sesión de nuevo.',
    );
  });
});
