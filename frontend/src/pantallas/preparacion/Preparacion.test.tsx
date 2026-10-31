// La pantalla «Preparación del periodo» (F02-H02, RF-56): el tablero de un periodo, lo que falta y
// «Crear distributivo», con la API simulada. Los dos escenarios del issue y los demás estados.
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { SIN_PERIODOS, tablero } from './datosDePrueba';

const DOCENTES = 'Docentes contratados (con ficha académica)';
const VICERRECTOR = ['DISTRIBUTIVO:LECTURA', 'DISTRIBUTIVO:ESCRITURA', 'CARGAS:LECTURA'];

function sesionCon(simulada: ApiSimulada, permisos: string[]) {
  simulada.responder(
    'GET',
    '/api/usuarios/sesion',
    json(200, {
      usuario: 'vicerrectorado.demo',
      nombre: 'Vicerrector (demo)',
      roles: [],
      permisos,
    }),
  );
}

async function filaDe(nombre: string): Promise<HTMLElement> {
  const tabla = await screen.findByRole('table', { name: /^Cargas del periodo/ });
  const fila = within(tabla)
    .getAllByRole('row')
    .find((candidata) => within(candidata).queryByText(nombre));
  if (!fila) {
    throw new Error(`No está la carga ${nombre}`);
  }
  return fila;
}

/** Las líneas de lo que falta, dentro de su sección (el menú también tiene elementos de lista). */
async function faltan(): Promise<HTMLElement[]> {
  const seccion = await screen.findByRole('region', {
    name: 'Qué falta para crear el distributivo',
  });
  return within(seccion).getAllByRole('listitem');
}

describe('Preparación del periodo', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion();
  });

  function responder(cuerpo: unknown) {
    simulada.responder('GET', '/api/cargas/preparacion', json(200, cuerpo));
  }

  it('con docentes pendiente, «Crear distributivo» está deshabilitado y dice qué falta', async () => {
    sesionCon(simulada, VICERRECTOR);
    responder(tablero('2026-II', { docentes: { estado: 'PENDIENTE' } }));
    montarSpa('/preparacion');

    const crear = await screen.findByRole('button', { name: 'Crear distributivo' });
    expect(crear).toBeDisabled();
    expect(crear).toHaveAccessibleDescription(
      `${DOCENTES}: está pendiente. Súbela y confírmala en «Plantillas de carga».`,
    );
    expect(await filaDe(DOCENTES)).toHaveTextContent('Pendiente');
    expect(
      screen.getByRole('heading', { name: 'Qué falta para crear el distributivo' }),
    ).toBeVisible();
  });

  it('con las cargas obligatorias, «Crear distributivo» está habilitado y avisa que llega con F06', async () => {
    sesionCon(simulada, VICERRECTOR);
    responder(tablero('2026-II', { matriculados_carrera: { estado: 'PENDIENTE' } }));
    const usuario = userEvent.setup();
    montarSpa('/preparacion');

    const crear = await screen.findByRole('button', { name: 'Crear distributivo' });
    expect(crear).toBeEnabled();
    expect(screen.getByText(/está listo para crear su distributivo/)).toBeVisible();
    expect(await filaDe('Matriculados por carrera')).toHaveTextContent('Pendiente');

    await usuario.click(crear);

    expect(await screen.findByRole('status')).toHaveTextContent(
      'El periodo 2026-II está listo. La construcción del distributivo llega con F06',
    );
  });

  it('muestra cada estado, con el motivo de «con errores»', async () => {
    sesionCon(simulada, VICERRECTOR);
    responder(
      tablero('2026-II', {
        docentes: { estado: 'CON_ERRORES', motivo: 'CARGAS_CARGA_CON_ERRORES' },
        proyectos_practicas: { estado: 'SIN_PROYECTOS' },
      }),
    );
    montarSpa('/preparacion');

    expect(await filaDe('Carreras')).toHaveTextContent('Cargada');
    expect(await filaDe(DOCENTES)).toHaveTextContent(
      'Con errores(su último archivo tenía errores en sus filas)',
    );
    expect(await filaDe('Proyectos y prácticas aprobados')).toHaveTextContent('Sin proyectos');
    expect((await faltan())[0]).toHaveTextContent(`${DOCENTES}: está con errores.`);
  });

  it('si faltan los proyectos, ofrece confirmar el periodo sin proyectos', async () => {
    sesionCon(simulada, VICERRECTOR);
    responder(tablero('2026-II', { proyectos_practicas: { estado: 'PENDIENTE' } }));
    montarSpa('/preparacion');

    expect((await faltan())[0]).toHaveTextContent(
      'o confirma el periodo sin proyectos en «Plantillas de carga»',
    );
  });

  it('dice si el periodo no está abierto o está cerrado', async () => {
    sesionCon(simulada, VICERRECTOR);
    responder(tablero('2026-II', {}, { abierto: false, cerrado: true }));
    montarSpa('/preparacion');

    const lista = await faltan();
    expect(lista.map((item) => item.textContent)).toEqual([
      'El periodo no está abierto: ábrelo con su versión de normativa en «Periodos».',
      'El periodo está cerrado: no admite un distributivo nuevo.',
    ]);
  });

  it('al elegir otro periodo, pide su tablero', async () => {
    sesionCon(simulada, VICERRECTOR);
    simulada.responder('GET', '/api/cargas/preparacion', () => {
      const ultima = simulada.peticiones.at(-1);
      const periodo = ultima ? new URL(ultima.url).searchParams.get('periodo') : null;
      return json(
        200,
        periodo === '2026-I'
          ? tablero('2026-I')
          : tablero('2026-II', { oferta_periodo: { estado: 'PENDIENTE' } }, { abierto: false }),
      );
    });
    const usuario = userEvent.setup();
    montarSpa('/preparacion');
    expect(await filaDe('Oferta del periodo')).toHaveTextContent('Pendiente');

    await usuario.click(screen.getByRole('combobox', { name: 'Periodo' }));
    await usuario.click(await screen.findByRole('option', { name: /^2026-I ·/ }));

    await waitFor(async () =>
      expect(await filaDe('Oferta del periodo')).toHaveTextContent('Cargada'),
    );
    expect(screen.getByRole('button', { name: 'Crear distributivo' })).toBeEnabled();
  });

  it('elige un periodo con otro código de Académico, como 003', async () => {
    sesionCon(simulada, VICERRECTOR);
    simulada.responder('GET', '/api/cargas/preparacion', () => {
      const ultima = simulada.peticiones.at(-1);
      const periodo = ultima ? new URL(ultima.url).searchParams.get('periodo') : null;
      return json(200, {
        ...tablero(periodo ?? '2026-II'),
        periodos: [
          { codigo: '2026-II', nombre: 'Periodo académico 2026-II' },
          { codigo: '003', nombre: '2027-01' },
        ],
      });
    });
    const usuario = userEvent.setup();
    montarSpa('/preparacion');
    await filaDe('Carreras');

    await usuario.click(screen.getByRole('combobox', { name: 'Periodo' }));
    await usuario.click(await screen.findByRole('option', { name: '003 · 2027-01' }));

    expect(await screen.findByRole('table', { name: 'Cargas del periodo 003' })).toBeVisible();
    expect(
      simulada.peticiones.some((una) => new URL(una.url).searchParams.get('periodo') === '003'),
    ).toBe(true);
  });

  it('sin periodos, pide cargar antes los ciclos académicos', async () => {
    sesionCon(simulada, VICERRECTOR);
    responder(SIN_PERIODOS);
    montarSpa('/preparacion');

    expect(
      await screen.findByText(/Todavía no hay periodos: carga antes los ciclos académicos/),
    ).toBeVisible();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('sin «Distributivo: escritura» ve el tablero, pero no «Crear distributivo»', async () => {
    sesionCon(simulada, ['CARGAS:LECTURA']);
    responder(tablero('2026-II'));
    montarSpa('/preparacion');

    expect(await filaDe('Carreras')).toHaveTextContent('Cargada');
    expect(screen.queryByRole('button', { name: 'Crear distributivo' })).not.toBeInTheDocument();
  });

  it.each([['CARGAS:LECTURA'], ['DISTRIBUTIVO:LECTURA']])(
    'con %s está en el menú',
    async (permiso) => {
      sesionCon(simulada, [permiso]);
      montarSpa('/');

      expect(
        await screen.findByRole('link', { name: 'Preparación del periodo' }),
      ).toBeInTheDocument();
    },
  );

  it('sin ninguno de los dos permisos no está en el menú', async () => {
    sesionCon(simulada, ['DOCENTES:LECTURA']);
    montarSpa('/');

    await screen.findByRole('link', { name: 'Inicio' });
    expect(screen.queryByRole('link', { name: 'Preparación del periodo' })).not.toBeInTheDocument();
  });

  it('si el servidor falla, muestra el error sin un tablero falso', async () => {
    sesionCon(simulada, VICERRECTOR);
    simulada.responder(
      'GET',
      '/api/cargas/preparacion',
      json(500, {
        titulo: 'Error inesperado',
        descripcion: 'Ocurrió un error inesperado. Intenta de nuevo en unos minutos.',
        codigo: 'COMUN_ERROR_INTERNO',
        idIncidente: 'abc',
      }),
    );
    montarSpa('/preparacion');

    expect(await screen.findByRole('alert')).toHaveTextContent('Error inesperado');
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });
});
