import { screen, within } from '@testing-library/react';
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

  it('«Tus funciones» muestra las de Docente y Coordinador de carrera, sin repetir', async () => {
    simulada.responder(
      'GET',
      '/api/usuarios/sesion',
      json(200, {
        usuario: 'docente.coordinador.demo',
        nombre: 'Docente y coordinador (demo)',
        roles: ['COORDINADOR_CARRERA', 'DOCENTE'],
        permisos: [
          'DISTRIBUTIVO:LECTURA:PROPIO',
          'DOCENTES:LECTURA',
          'MI_DISTRIBUTIVO:ESCRITURA:PROPIO',
          'MI_DISTRIBUTIVO:LECTURA:PROPIO',
          'OFERTA:APROBACION:PROPIO',
          'OFERTA:ESCRITURA:PROPIO',
          'OFERTA:LECTURA:PROPIO',
        ],
      }),
    );
    montarSpa('/');

    const funciones = await screen.findByRole('list', { name: 'Tus funciones' });
    const elementos = within(funciones).getAllByRole('listitem');
    expect(elementos.map((elemento) => elemento.textContent)).toEqual([
      'DocentesLectura',
      'Oferta y propuesta de docentesLectura, escritura y aprobación · solo lo propio',
      'Distributivo del periodoLectura · solo lo propio',
      'Mi distributivoLectura y escritura · solo lo propio',
    ]);
    expect(screen.getByText('Coordinador de carrera')).toBeInTheDocument();
    expect(screen.getByText('Docente')).toBeInTheDocument();
  });

  it('sin permisos, dice que todavía no tiene funciones', async () => {
    simulada.responder(
      'GET',
      '/api/usuarios/sesion',
      json(200, { usuario: 'sin.roles', roles: [], permisos: [] }),
    );
    montarSpa('/');

    expect(await screen.findByText('Todavía no tienes funciones asignadas.')).toBeInTheDocument();
    expect(screen.queryByRole('list', { name: 'Tus funciones' })).not.toBeInTheDocument();
  });

  it('si dos roles dan alcances distintos en una funcionalidad, dice el de cada tipo', async () => {
    simulada.responder(
      'GET',
      '/api/usuarios/sesion',
      json(200, {
        usuario: 'mixto.demo',
        roles: ['COORDINADOR_CARRERA', 'RESPONSABLE_CONSOLIDACION'],
        permisos: ['OFERTA:ESCRITURA:PROPIO', 'OFERTA:LECTURA'],
      }),
    );
    montarSpa('/');

    expect(await screen.findByText('Lectura y escritura (solo lo propio)')).toBeInTheDocument();
  });

  it('un rol creado por el administrador se muestra con su nombre legible', async () => {
    simulada.responder(
      'GET',
      '/api/usuarios/sesion',
      json(200, {
        usuario: 'docente.demo',
        roles: ['AUDITOR_DE_ROLES', 'DOCENTE'],
        permisos: ['USUARIOS_ROLES:LECTURA'],
      }),
    );
    montarSpa('/');

    expect(await screen.findByText('Auditor de roles')).toBeInTheDocument();
    expect(screen.getByText('Usuarios y roles')).toBeInTheDocument();
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
