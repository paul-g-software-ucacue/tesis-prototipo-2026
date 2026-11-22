// «Reportes» (F20-H01, F20-H02, contracts/spa.md): los periodos publicados, la reforma en curso,
// el tipo de reporte, el reporte general con sus totales, el buscador de docentes, el reporte
// individual con sus tres tablas y las descargas de los dos con el nombre del servidor.
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { elegirEscribiendo } from '../../pruebas/elegirEscribiendo';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';

const HASH = 'a'.repeat(64);
const PUBLICACION = {
  periodo: '2026-I',
  version: 1,
  hash: HASH,
  publicadaEn: '2026-11-18T15:00:00Z',
  publicadaPor: 'secretaria.demo',
  enReforma: false,
};
const ANTERIOR = { ...PUBLICACION, periodo: '2025-II', publicadaEn: '2025-06-01T15:00:00Z' };
const ANA = '0109990010';
const DOCENTES = {
  publicacion: PUBLICACION,
  docentes: [
    { identificacion: '0109990020', nombre: 'ANDRADE LUIS' },
    { identificacion: ANA, nombre: 'PÉREZ ANA' },
  ],
};
const REPORTE = `/api/reporting/periodos/2026-I/docentes/${ANA}/reporte`;
const GENERAL = '/api/reporting/periodos/2026-I/reporte-general';
function horasDe(clase: number, otrasDocencia: number, gestion: number) {
  return {
    clase,
    otrasDocencia,
    investigacion: 0,
    vinculacion: 0,
    gestion,
    total: clase + otrasDocencia + gestion,
  };
}
const REPORTE_GENERAL = {
  publicacion: PUBLICACION,
  docentes: [
    { identificacion: '0109990020', nombre: 'ANDRADE LUIS', horas: horasDe(0, 0, 3) },
    {
      identificacion: ANA,
      nombre: 'PÉREZ ANA',
      dedicacion: 'TC',
      horas: horasDe(8, 2.5, 0),
    },
  ],
  totales: horasDe(8, 2.5, 3),
  generadoPor: 'rectorado.demo',
  generadoEn: '2026-11-21T15:00:00Z',
};
const REPORTE_DE_ANA = {
  publicacion: PUBLICACION,
  docente: { identificacion: ANA, nombre: 'PÉREZ ANA', dedicacion: 'TC', horasContratadas: 40 },
  asignaturas: [
    { carrera: 'Software', codigo: 'BD-301', nombre: 'Bases de datos', paralelo: 'A', horas: 8 },
  ],
  actividades: [],
  horas: {
    clase: 8,
    otrasDocencia: 0,
    investigacion: 0,
    vinculacion: 0,
    gestion: 0,
    total: 8,
  },
  generadoPor: 'rectorado.demo',
  generadoEn: '2026-11-21T15:00:00Z',
};

describe('Reportes', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder(
      'GET',
      '/api/usuarios/sesion',
      json(200, { usuario: 'rectorado.demo', roles: [], permisos: ['REPORTES:LECTURA'] }),
    );
    simulada.responder('GET', '/api/reporting/publicaciones', json(200, [PUBLICACION, ANTERIOR]));
    simulada.responder('GET', '/api/reporting/periodos/2026-I/docentes', json(200, DOCENTES));
    simulada.responder('GET', REPORTE, json(200, REPORTE_DE_ANA));
    simulada.responder('GET', GENERAL, json(200, REPORTE_GENERAL));
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion('cierre');
    vi.restoreAllMocks();
  });

  async function porDocente() {
    await userEvent.click(await screen.findByRole('radio', { name: 'Por docente' }));
  }

  function pedidas(ruta: string) {
    return simulada.peticiones.filter((una) => new URL(una.url).pathname === ruta).length;
  }

  async function elegirAna() {
    montarSpa('/reportes');
    await porDocente();
    await elegirEscribiendo(
      userEvent.setup(),
      await screen.findByRole('combobox', { name: 'Docente' }),
      ANA,
    );
    return screen.findByRole('heading', { name: 'PÉREZ ANA', level: 2 });
  }

  it('propone el periodo de la publicación más reciente y el reporte general, sin buscar a nadie', async () => {
    montarSpa('/reportes');

    expect(
      await screen.findByText(/Versión 1, publicada el .* por secretaria\.demo\./),
    ).toBeVisible();
    expect(screen.getByRole('combobox', { name: 'Periodo' })).toHaveValue('2026-I');
    expect(screen.getByRole('group', { name: 'Tipo de reporte' })).toBeVisible();
    expect(screen.getByRole('radio', { name: 'General' })).toBeChecked();
    expect(screen.queryByRole('combobox', { name: 'Docente' })).not.toBeInTheDocument();
    expect(screen.getByText(/2 docentes/)).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Reportes', level: 1 })).toBeVisible();
  });

  it('muestra a cada docente con sus cinco componentes y la fila del total institucional', async () => {
    montarSpa('/reportes');

    const tabla = await screen.findByRole('table', { name: 'Docentes del distributivo publicado' });
    const filas = within(tabla).getAllByRole('row');
    // El encabezado, los dos docentes en el orden del servidor y el total.
    expect(filas).toHaveLength(4);
    expect(within(filas[1]!).getByRole('rowheader', { name: 'ANDRADE LUIS' })).toBeVisible();
    expect(within(filas[1]!).getByText('sin dato')).toBeVisible();
    expect(within(filas[2]!).getByRole('rowheader', { name: 'PÉREZ ANA' })).toBeVisible();
    expect(within(filas[2]!).getByText('Tiempo completo')).toBeVisible();
    expect(within(filas[2]!).getByText('10,50')).toBeVisible();
    const total = filas[3]!;
    expect(within(total).getByRole('rowheader', { name: 'Total institucional' })).toBeVisible();
    expect(within(total).getByText('13,50')).toBeVisible();
    expect(within(total).getByText('2,50')).toBeVisible();
    expect(pedidas(GENERAL)).toBe(1);
  });

  it('sin publicaciones, lo dice y no ofrece ningún reporte', async () => {
    simulada.responder('GET', '/api/reporting/publicaciones', json(200, []));
    montarSpa('/reportes');

    expect(await screen.findByText(/Todavía no hay un distributivo publicado/)).toBeVisible();
    expect(screen.queryByRole('group', { name: 'Tipo de reporte' })).not.toBeInTheDocument();
    expect(screen.queryByRole('combobox', { name: 'Docente' })).not.toBeInTheDocument();
    expect(pedidas(GENERAL)).toBe(0);
  });

  it('con una reforma en curso, dice que el reporte sale de la versión publicada', async () => {
    simulada.responder(
      'GET',
      GENERAL,
      json(200, { ...REPORTE_GENERAL, publicacion: { ...PUBLICACION, enReforma: true } }),
    );
    simulada.responder(
      'GET',
      '/api/reporting/periodos/2026-I/docentes',
      json(200, { ...DOCENTES, publicacion: { ...PUBLICACION, enReforma: true } }),
    );
    montarSpa('/reportes');

    expect(await screen.findByText(/Hay una reforma en curso/)).toBeVisible();
    await porDocente();
    expect(await screen.findByText(/Hay una reforma en curso/)).toBeVisible();
  });

  it('al pasar a «Por docente» deja el foco en el buscador y no vuelve a pedir el general', async () => {
    simulada.responder(
      'GET',
      '/api/reporting/periodos/2025-II/docentes',
      json(200, { ...DOCENTES, publicacion: ANTERIOR }),
    );
    montarSpa('/reportes');
    await screen.findByRole('table', { name: 'Docentes del distributivo publicado' });

    await porDocente();

    const buscador = await screen.findByRole('combobox', { name: 'Docente' });
    await waitFor(() => expect(buscador).toHaveFocus());
    expect(screen.getByText(/2 docentes en la versión publicada/)).toBeVisible();

    await userEvent.click(screen.getByRole('combobox', { name: 'Periodo' }));
    await userEvent.click(await screen.findByRole('option', { name: '2025-II' }));
    expect(await screen.findByRole('radio', { name: 'Por docente' })).toBeChecked();
    await screen.findByRole('combobox', { name: 'Docente' });
    expect(pedidas('/api/reporting/periodos/2025-II/reporte-general')).toBe(0);
    expect(pedidas(GENERAL)).toBe(1);
  });

  it('descarga el PDF y el CSV del general con el nombre del servidor y avisa sobre el CSV', async () => {
    Object.assign(URL, { createObjectURL: vi.fn(() => 'blob:x'), revokeObjectURL: vi.fn() });
    const nombres: string[] = [];
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      nombres.push(this.download);
    });
    for (const formato of ['pdf', 'csv']) {
      simulada.responder(
        'GET',
        `${GENERAL}.${formato}`,
        () =>
          new Response(new Blob(['x']), {
            status: 200,
            headers: {
              'Content-Disposition': `attachment; filename="reporte-general-2026-I.${formato}"`,
            },
          }),
      );
    }
    montarSpa('/reportes');
    await screen.findByRole('table', { name: 'Docentes del distributivo publicado' });

    await userEvent.click(screen.getByRole('button', { name: 'Descargar PDF' }));
    await waitFor(() => expect(nombres).toHaveLength(1));
    expect(screen.queryByRole('dialog', { name: 'CSV descargado' })).toBeNull();
    await userEvent.click(screen.getByRole('button', { name: 'Descargar CSV' }));

    await waitFor(() =>
      expect(nombres).toEqual(['reporte-general-2026-I.pdf', 'reporte-general-2026-I.csv']),
    );
    expect(
      await screen.findByRole('dialog', { name: 'CSV descargado' }),
    ).toHaveAccessibleDescription(/importa la columna «Identificación» como texto/);
  });

  it('si una descarga del general falla, lo dice junto a los botones', async () => {
    simulada.responder(
      'GET',
      `${GENERAL}.csv`,
      json(500, {
        titulo: 'Reporte no disponible',
        descripcion: 'No se pudo generar el reporte.',
        codigo: 'REPORTING_CONTENIDO_INCONSISTENTE',
      }),
    );
    montarSpa('/reportes');
    await screen.findByRole('table', { name: 'Docentes del distributivo publicado' });

    await userEvent.click(screen.getByRole('button', { name: 'Descargar CSV' }));

    expect(await screen.findByText('Reporte no disponible')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Descargar CSV' })).toBeEnabled();
  });

  it('busca al docente por su identificación y muestra su reporte con el foco en su nombre', async () => {
    const titulo = await elegirAna();

    await waitFor(() => expect(titulo).toHaveFocus());
    expect(screen.getByText(/Dedicación: Tiempo completo · 40 horas contratadas/)).toBeVisible();
    const asignaturas = screen.getByRole('table', { name: 'Asignaturas' });
    expect(within(asignaturas).getByText('Bases de datos')).toBeVisible();
    expect(within(asignaturas).getByText('8,00')).toBeVisible();
    expect(screen.getByText('No tiene actividades en esta versión.')).toBeVisible();
    const totales = screen.getByRole('table', { name: 'Horas por componente' });
    expect(within(totales).getByRole('rowheader', { name: 'Total' })).toBeVisible();
    expect(within(totales).getAllByText('0,00')).toHaveLength(4);
    expect(simulada.peticiones.some((una) => new URL(una.url).pathname === REPORTE)).toBe(true);
  });

  it('descarga el PDF y el CSV con el nombre del servidor', async () => {
    Object.assign(URL, { createObjectURL: vi.fn(() => 'blob:x'), revokeObjectURL: vi.fn() });
    const nombres: string[] = [];
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      nombres.push(this.download);
    });
    for (const formato of ['pdf', 'csv']) {
      simulada.responder(
        'GET',
        `${REPORTE}.${formato}`,
        () =>
          new Response(new Blob(['x']), {
            status: 200,
            headers: {
              'Content-Disposition': `attachment; filename="reporte-individual-2026-I-${ANA}.${formato}"`,
            },
          }),
      );
    }
    await elegirAna();

    await userEvent.click(screen.getByRole('button', { name: 'Descargar PDF' }));
    await waitFor(() => expect(nombres).toHaveLength(1));
    await userEvent.click(screen.getByRole('button', { name: 'Descargar CSV' }));

    await waitFor(() =>
      expect(nombres).toEqual([
        `reporte-individual-2026-I-${ANA}.pdf`,
        `reporte-individual-2026-I-${ANA}.csv`,
      ]),
    );
  });

  it('al descargar el CSV avisa que la identificación se importa como texto, y no con el PDF', async () => {
    Object.assign(URL, { createObjectURL: vi.fn(() => 'blob:x'), revokeObjectURL: vi.fn() });
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
    for (const formato of ['pdf', 'csv']) {
      simulada.responder(
        'GET',
        `${REPORTE}.${formato}`,
        () => new Response(new Blob(['x']), { status: 200 }),
      );
    }
    await elegirAna();
    expect(screen.queryByText(/importa la columna «Identificación» como texto/)).toBeNull();

    await userEvent.click(screen.getByRole('button', { name: 'Descargar PDF' }));
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Descargar PDF' })).toBeEnabled(),
    );
    expect(screen.queryByText(/importa la columna «Identificación» como texto/)).toBeNull();

    const boton = screen.getByRole('button', { name: 'Descargar CSV' });
    await userEvent.click(boton);
    const dialogo = await screen.findByRole('dialog', { name: 'CSV descargado' });
    expect(dialogo).toHaveAccessibleDescription(/importa la columna «Identificación» como texto/);
    const entendido = within(dialogo).getByRole('button', { name: 'Entendido' });
    await waitFor(() => expect(entendido).toHaveFocus());

    await userEvent.click(entendido);
    await waitFor(() =>
      expect(screen.queryByRole('dialog', { name: 'CSV descargado' })).toBeNull(),
    );
    await waitFor(() => expect(boton).toHaveFocus());
  });

  it('si una descarga falla, lo dice junto a los botones', async () => {
    simulada.responder(
      'GET',
      `${REPORTE}.pdf`,
      json(500, {
        titulo: 'Reporte no disponible',
        descripcion: 'No se pudo generar el reporte.',
        codigo: 'REPORTING_CONTENIDO_INCONSISTENTE',
      }),
    );
    await elegirAna();

    await userEvent.click(screen.getByRole('button', { name: 'Descargar PDF' }));

    expect(await screen.findByText('Reporte no disponible')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Descargar PDF' })).toBeEnabled();
  });

  it('si el docente ya no está en la versión, muestra el 404 del catálogo', async () => {
    simulada.responder(
      'GET',
      REPORTE,
      json(404, {
        titulo: 'Docente sin distributivo',
        descripcion: 'El docente no tiene asignaciones en la versión publicada de este periodo.',
        codigo: 'REPORTING_DOCENTE_SIN_DISTRIBUTIVO',
      }),
    );
    montarSpa('/reportes');
    await porDocente();
    await elegirEscribiendo(
      userEvent.setup(),
      await screen.findByRole('combobox', { name: 'Docente' }),
      ANA,
    );

    expect(await screen.findByText('Docente sin distributivo')).toBeVisible();
  });
});
