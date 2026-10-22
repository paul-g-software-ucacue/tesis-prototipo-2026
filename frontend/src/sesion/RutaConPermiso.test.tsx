// Una ruta con permisos alternativos (D-70): basta con uno para ver la pantalla; sin ninguno,
// «Acceso no autorizado». Las rutas de un solo permiso siguen igual.
import { QueryClient } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { createMemoryRouter } from 'react-router';
import { App } from '../App';
import { instalarApiSimulada, json, type ApiSimulada } from '../pruebas/apiSimulada';
import { cerrarSesion, iniciarSesion } from './almacen';
import { RutaConPermiso } from './RutaConPermiso';

function montar() {
  const enrutador = createMemoryRouter([
    {
      path: '/alternativa',
      element: (
        <RutaConPermiso
          cualquieraDe={[
            { funcionalidad: 'DISTRIBUTIVO', tipo: 'LECTURA' },
            { funcionalidad: 'CATALOGOS', tipo: 'LECTURA' },
          ]}
        >
          <p>Contenido protegido</p>
        </RutaConPermiso>
      ),
    },
    {
      path: '/una',
      element: (
        <RutaConPermiso funcionalidad="CATALOGOS" tipo="ESCRITURA">
          <p>Contenido protegido</p>
        </RutaConPermiso>
      ),
    },
  ]);
  const consultas = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(<App enrutador={enrutador} clienteDeConsultas={consultas} />);
  return enrutador;
}

describe('RutaConPermiso', () => {
  let simulada: ApiSimulada;

  function sesionCon(permisos: string[]) {
    simulada.responder(
      'GET',
      '/api/usuarios/sesion',
      json(200, { usuario: 'autor.prueba', nombre: 'Autor', roles: [], permisos }),
    );
  }

  beforeEach(() => {
    simulada = instalarApiSimulada();
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion();
  });

  it.each([['DISTRIBUTIVO:LECTURA'], ['CATALOGOS:LECTURA'], ['DISTRIBUTIVO:LECTURA:PROPIO']])(
    'con %s muestra la pantalla de permisos alternativos',
    async (permiso) => {
      sesionCon([permiso]);
      const enrutador = montar();
      await enrutador.navigate('/alternativa');

      expect(await screen.findByText('Contenido protegido')).toBeInTheDocument();
    },
  );

  it('sin ninguno de los permisos alternativos muestra «Acceso no autorizado»', async () => {
    sesionCon(['CATALOGOS:ESCRITURA', 'PARAMETROS:LECTURA']);
    const enrutador = montar();
    await enrutador.navigate('/alternativa');

    expect(await screen.findByRole('alert')).toHaveTextContent('Acceso no autorizado');
    expect(screen.queryByText('Contenido protegido')).not.toBeInTheDocument();
  });

  it('con un solo permiso, como antes', async () => {
    sesionCon(['CATALOGOS:LECTURA']);
    const enrutador = montar();
    await enrutador.navigate('/una');

    expect(await screen.findByRole('alert')).toHaveTextContent('Acceso no autorizado');
  });
});
