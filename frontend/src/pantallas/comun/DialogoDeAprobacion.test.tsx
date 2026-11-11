// Aprobar el contenido (F13-H01, contracts/spa.md): el diálogo dice qué se congela, «Cancelar» no
// llama a la API, «Aprobar y congelar» la llama una vez y un error se muestra en el diálogo.
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { DialogoDeAprobacion } from './DialogoDeAprobacion';

const APROBACION = '/api/validacionocs/periodos/2026-I/aprobacion';

function montar() {
  const alCerrar = vi.fn();
  const alAprobar = vi.fn(() => Promise.resolve());
  render(
    <DialogoDeAprobacion periodo="2026-I" version={3} alCerrar={alCerrar} alAprobar={alAprobar} />,
  );
  return { alCerrar, alAprobar };
}

function posts(simulada: ApiSimulada) {
  return simulada.peticiones.filter((una) => una.method === 'POST');
}

describe('DialogoDeAprobacion', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
  });

  it('dice que la versión queda congelada y que un cambio exige una versión nueva', () => {
    montar();

    const dialogo = screen.getByRole('dialog', { name: 'Aprobar el contenido' });
    expect(dialogo).toHaveTextContent(
      'La versión 3 queda congelada: es la que se firmará. Todo cambio posterior exigirá una versión nueva.',
    );
    expect(within(dialogo).getByRole('button', { name: 'Aprobar y congelar' })).toBeVisible();
    expect(within(dialogo).getByRole('button', { name: 'Cancelar' })).toHaveFocus();
  });

  it('«Cancelar» no llama a la API', async () => {
    const { alCerrar, alAprobar } = montar();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(alCerrar).toHaveBeenCalled();
    expect(alAprobar).not.toHaveBeenCalled();
    expect(posts(simulada)).toHaveLength(0);
  });

  it('se cierra con Escape sin llamar a la API', async () => {
    const { alCerrar } = montar();
    const usuario = userEvent.setup();

    await usuario.keyboard('{Escape}');

    expect(alCerrar).toHaveBeenCalled();
    expect(posts(simulada)).toHaveLength(0);
  });

  it('«Aprobar y congelar» llama una vez y desactiva el botón mientras espera', async () => {
    let terminar: (respuesta: Response) => void = () => undefined;
    simulada.responder(
      'POST',
      APROBACION,
      () =>
        new Promise<Response>((listo) => {
          terminar = listo;
        }),
    );
    const { alAprobar } = montar();
    const usuario = userEvent.setup();

    const aprobar = screen.getByRole('button', { name: 'Aprobar y congelar' });
    await usuario.click(aprobar);
    await waitFor(() => expect(aprobar).toBeDisabled());
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled();
    terminar(json(200, {}));

    await waitFor(() => expect(alAprobar).toHaveBeenCalledTimes(1));
    expect(posts(simulada)).toHaveLength(1);
  });

  it('un error se muestra en el diálogo con el mensaje del catálogo', async () => {
    simulada.responder(
      'POST',
      APROBACION,
      json(409, {
        titulo: 'Etapa sin aprobación',
        descripcion:
          'El distributivo no está en revisión del Vicerrectorado: no se puede aprobar su contenido.',
        codigo: 'VALIDACIONOCS_ETAPA_NO_ADMITE_APROBACION',
      }),
    );
    const { alAprobar } = montar();
    const usuario = userEvent.setup();

    await usuario.click(screen.getByRole('button', { name: 'Aprobar y congelar' }));

    const dialogo = screen.getByRole('dialog', { name: 'Aprobar el contenido' });
    expect(await within(dialogo).findByRole('alert')).toHaveTextContent('Etapa sin aprobación');
    expect(alAprobar).not.toHaveBeenCalled();
    expect(within(dialogo).getByRole('button', { name: 'Aprobar y congelar' })).toBeEnabled();
  });
});
