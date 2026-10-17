import { QueryClient } from '@tanstack/react-query';
import { act, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { instalarApiSimulada, json } from '../pruebas/apiSimulada';
import { cerrarSesion, estadoDeSesion, iniciarSesion, obtenerToken } from './almacen';
import { destinoSeguro } from './destinoSeguro';
import { RutaProtegida } from './RutaProtegida';
import { SesionProvider } from './SesionProvider';

function DondeEstoy() {
  const ubicacion = useLocation();
  return <p>{`${ubicacion.pathname}${ubicacion.search}`}</p>;
}

function montar(ruta: string, consultas = new QueryClient()) {
  return render(
    <SesionProvider clienteDeConsultas={consultas}>
      <MemoryRouter initialEntries={[ruta]}>
        <Routes>
          <Route path="/iniciar-sesion" element={<DondeEstoy />} />
          <Route
            path="/interna"
            element={
              <RutaProtegida>
                <p>contenido interno</p>
              </RutaProtegida>
            }
          />
        </Routes>
      </MemoryRouter>
    </SesionProvider>,
  );
}

describe('sesión en memoria (D-51)', () => {
  afterEach(() => {
    cerrarSesion();
    vi.useRealTimers();
  });

  it('iniciar deja la sesión activa y cerrar la borra', () => {
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
    expect(obtenerToken()).toBe('cabecera.cuerpo.firma');
    expect(estadoDeSesion()).toEqual({ activa: true });

    cerrarSesion();

    expect(obtenerToken()).toBeNull();
    expect(estadoDeSesion()).toEqual({ activa: false });
  });

  it('el token nunca llega al almacenamiento del navegador ni a las cookies', () => {
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));

    expect(window.localStorage.length).toBe(0);
    expect(window.sessionStorage.length).toBe(0);
    expect(document.cookie).toBe('');
  });

  it('al vencer el token, la sesión se cierra como terminada', () => {
    vi.useFakeTimers();
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 1_000));

    vi.advanceTimersByTime(1_001);

    expect(estadoDeSesion()).toEqual({ activa: false, motivo: 'sesion-terminada' });
  });

  it('un vencimiento lejano no cierra la sesión antes de tiempo (sin desbordar setTimeout)', () => {
    vi.useFakeTimers();
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 40 * 24 * 60 * 60 * 1000));

    vi.advanceTimersByTime(30 * 24 * 60 * 60 * 1000);
    expect(estadoDeSesion().activa).toBe(true);

    vi.advanceTimersByTime(10 * 24 * 60 * 60 * 1000 + 1);
    expect(estadoDeSesion()).toEqual({ activa: false, motivo: 'sesion-terminada' });
  });

  it('un token ya vencido no abre la sesión', () => {
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() - 1));

    expect(estadoDeSesion().activa).toBe(false);
  });

  it('al cerrarse la sesión, se borra la caché de las consultas', () => {
    const consultas = new QueryClient();
    consultas.setQueryData(['sesion'], { usuario: 'autor.prueba' });
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
    montar('/interna', consultas);

    act(() => cerrarSesion());

    expect(consultas.getQueryData(['sesion'])).toBeUndefined();
  });
});

describe('actividad del usuario en SesionProvider (ADR-020)', () => {
  afterEach(() => {
    cerrarSesion();
    vi.useRealTimers();
  });

  it.each([
    ['una tecla', () => fireEvent.keyDown(window, { key: 'a' })],
    ['un clic', () => fireEvent.pointerDown(window)],
    ['volver atrás en el navegador', () => fireEvent.popState(window)],
  ])('%s en los últimos 20 minutos renueva el token', async (_nombre, accion) => {
    vi.useFakeTimers();
    const simulada = instalarApiSimulada();
    simulada.responder(
      'POST',
      '/api/usuarios/sesion/renovacion',
      json(200, {
        token: 'renovado',
        tipo: 'Bearer',
        venceEn: new Date(Date.now() + 42 * 60_000).toISOString(),
      }),
    );
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 30 * 60_000));
    montar('/interna');
    await act(() => vi.advanceTimersByTimeAsync(12 * 60_000));

    accion();
    await act(() => vi.advanceTimersByTimeAsync(0));

    expect(obtenerToken()).toBe('renovado');
  });

  it('sin sesión, la actividad no pide nada a la API', async () => {
    const simulada = instalarApiSimulada();
    montar('/iniciar-sesion');

    fireEvent.keyDown(window, { key: 'a' });
    await act(() => Promise.resolve());

    expect(simulada.peticiones).toHaveLength(0);
  });
});

describe('RutaProtegida (D-50)', () => {
  afterEach(() => {
    cerrarSesion();
  });

  it('sin sesión, redirige al inicio de sesión con el destino pedido', () => {
    montar('/interna?pagina=2');

    expect(screen.getByText('/iniciar-sesion?destino=%2Finterna%3Fpagina%3D2')).toBeInTheDocument();
  });

  it('con sesión, muestra la ruta', () => {
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
    montar('/interna');

    expect(screen.getByText('contenido interno')).toBeInTheDocument();
  });

  it('si el usuario cerró la sesión, va al inicio de sesión sin destino', () => {
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
    cerrarSesion('cierre');
    montar('/interna');

    expect(screen.getByText('/iniciar-sesion')).toBeInTheDocument();
  });

  it('si la sesión terminó, lo dice al redirigir', () => {
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
    cerrarSesion('sesion-terminada');
    montar('/interna');

    expect(
      screen.getByText('/iniciar-sesion?destino=%2Finterna&motivo=sesion-terminada'),
    ).toBeInTheDocument();
  });
});

describe('destinoSeguro', () => {
  it.each([
    ['/', '/'],
    ['/inicio', '/inicio'],
    ['/interna?pagina=2', '/interna?pagina=2'],
    ['https://otro.example', '/'],
    ['//otro.example', '/'],
    ['/\\otro.example', '/'],
    ['javascript:alert(1)', '/'],
    [null, '/'],
  ])('%s → %s', (valor, esperado) => {
    expect(destinoSeguro(valor)).toBe(esperado);
  });
});
