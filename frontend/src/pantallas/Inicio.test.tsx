import { screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../pruebas/apiSimulada';
import { montarSpa } from '../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../sesion/almacen';

describe('Inicio', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion();
  });

  it('saluda con el nombre para mostrar y muestra sus roles en español', async () => {
    simulada.responder(
      'GET',
      '/api/usuarios/sesion',
      json(200, {
        usuario: 'vicerrectorado.demo',
        nombre: 'Vicerrectorado académico (demo)',
        roles: ['VICERRECTOR'],
      }),
    );
    montarSpa('/');

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: 'Hola, Vicerrectorado académico (demo)',
      }),
    ).toBeInTheDocument();
    expect(screen.getByText('Vicerrector académico')).toBeInTheDocument();
    expect(document.body).not.toHaveTextContent('VICERRECTOR');
    expect(document.title).toBe('Inicio · MDD');
  });

  it('sin nombre para mostrar, saluda con el usuario y muestra todos sus roles', async () => {
    simulada.responder(
      'GET',
      '/api/usuarios/sesion',
      json(200, { usuario: 'autor.prueba', roles: ['COORDINADOR_CARRERA', 'DOCENTE'] }),
    );
    montarSpa('/');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Hola, autor.prueba' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Coordinador de carrera')).toBeInTheDocument();
    expect(screen.getByText('Docente')).toBeInTheDocument();
  });

  it('con un error técnico, muestra el aviso con el identificador del incidente', async () => {
    simulada.responder(
      'GET',
      '/api/usuarios/sesion',
      json(500, {
        titulo: 'Error inesperado',
        descripcion: 'Ocurrió un error inesperado.',
        codigo: 'COMUN_ERROR_INTERNO',
        idIncidente: '6f1c2b9e-1d2a-4c3b-8e4f-5a6b7c8d9e0f',
      }),
    );
    montarSpa('/');

    const aviso = await screen.findByRole('alert');
    expect(aviso).toHaveTextContent('Error inesperado');
    expect(aviso).toHaveTextContent(
      'Identificador del incidente: 6f1c2b9e-1d2a-4c3b-8e4f-5a6b7c8d9e0f',
    );
    expect(document.body).not.toHaveTextContent('COMUN_');
  });
});
