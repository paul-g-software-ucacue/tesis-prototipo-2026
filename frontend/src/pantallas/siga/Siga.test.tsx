// «Exportación a SIGA» (F19-H01, D-119, contracts/spa.md): el CSV para SIGA de la versión publicada,
// con qué layout sale, y el layout de exportación: la plantilla, la carga, sus problemas y las
// versiones anteriores.
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { AVISO_DEL_CSV_SIGA } from './Siga';

const HASH = 'a'.repeat(64);
const PUBLICACION = {
  periodo: '2026-I',
  version: 1,
  hash: HASH,
  publicadaEn: '2026-11-18T15:00:00Z',
  publicadaPor: 'secretaria.demo',
  enReforma: false,
};
const COLUMNAS = [
  { nombre: 'NUMERO_IDENTIFICACION', campo: 'identificacion' },
  {
    nombre: 'COMPONENTE',
    campo: 'componente',
    traduccion: [
      { codigo: 'CLASE', valor: '01' },
      { codigo: 'GESTION', valor: '05' },
    ],
  },
  { nombre: 'HORAS_SEMANALES', campo: 'horas' },
];
const POR_DEFECTO = { numero: 0, porDefecto: true, sha256: HASH, columnas: COLUMNAS.slice(0, 1) };
function cargado(numero: number) {
  return {
    numero,
    porDefecto: false,
    sha256: HASH,
    cargadoPor: 'secretaria.demo',
    cargadoEn: '2026-11-22T15:00:00Z',
    columnas: COLUMNAS,
  };
}
const LAYOUT = '/api/reporting/layout-siga';
const CSV = '/api/reporting/periodos/2026-I/csv-siga';
const EXPORTACIONES = '/api/reporting/periodos/2026-I/exportaciones-siga';
const SUMA = 'b'.repeat(64);
function exportada(id: string, layout: number, exportadaEn: string, suma = SUMA) {
  return {
    id,
    version: 1,
    layout,
    nombre: 'siga-2026-I-v1.csv',
    filas: 165,
    sumaVerificacion: suma,
    exportadaPor: 'secretaria.demo',
    exportadaEn,
  };
}

describe('Exportación a SIGA', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder(
      'GET',
      '/api/usuarios/sesion',
      json(200, {
        usuario: 'secretaria.demo',
        roles: [],
        permisos: ['REPORTES:LECTURA', 'PUBLICACION:APROBACION'],
      }),
    );
    simulada.responder('GET', '/api/reporting/publicaciones', json(200, [PUBLICACION]));
    simulada.responder('GET', LAYOUT, json(200, { vigente: POR_DEFECTO, anteriores: [] }));
    simulada.responder('GET', EXPORTACIONES, json(200, { periodo: '2026-I', exportaciones: [] }));
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion('cierre');
    vi.restoreAllMocks();
  });

  function capturarDescargas() {
    Object.assign(URL, { createObjectURL: vi.fn(() => 'blob:x'), revokeObjectURL: vi.fn() });
    const nombres: string[] = [];
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      nombres.push(this.download);
    });
    return nombres;
  }

  async function elegirArchivo(contenido: string, tamano?: number) {
    const archivo = new File([contenido], 'layout.yaml', { type: 'text/yaml' });
    if (tamano !== undefined) {
      Object.defineProperty(archivo, 'size', { value: tamano });
    }
    await userEvent.upload(await screen.findByLabelText('Cargar archivo de mapeo'), archivo);
  }

  it('propone el periodo publicado y dice que se generará con el layout por defecto', async () => {
    montarSpa('/exportacion-siga');

    expect(
      await screen.findByRole('heading', { name: 'Exportación a SIGA', level: 1 }),
    ).toBeVisible();
    expect(
      await screen.findByText('Versión 1, publicada el 2026-11-18 por secretaria.demo.'),
    ).toBeVisible();
    expect(screen.getAllByText('Se generará con el layout por defecto.')).not.toHaveLength(0);
    expect(screen.getByRole('button', { name: 'Generar CSV para SIGA' })).toBeEnabled();
    expect(screen.getByText('No hay versiones anteriores.')).toBeVisible();
  });

  it('con un layout cargado y una reforma, lo dice', async () => {
    simulada.responder('GET', LAYOUT, json(200, { vigente: cargado(2), anteriores: [] }));
    simulada.responder(
      'GET',
      '/api/reporting/publicaciones',
      json(200, [{ ...PUBLICACION, enReforma: true }]),
    );
    montarSpa('/exportacion-siga');

    expect(
      (
        await screen.findAllByText(
          'Se generará con la versión 2 del layout de exportación, cargada el 2026-11-22 por secretaria.demo.',
        )
      ).length,
    ).toBeGreaterThan(0);
    expect(
      screen.getByText('Hay una reforma en curso: el CSV sale de esta versión publicada.'),
    ).toBeVisible();
  });

  it('sin publicaciones, no ofrece generar el CSV y muestra el layout', async () => {
    simulada.responder('GET', '/api/reporting/publicaciones', json(200, []));
    montarSpa('/exportacion-siga');

    expect(
      await screen.findByText(
        'Ningún periodo tiene un distributivo publicado. El CSV para SIGA sale de la versión publicada.',
      ),
    ).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Generar CSV para SIGA' })).toBeNull();
    expect(await screen.findByRole('table', { name: 'Layout vigente' })).toBeVisible();
  });

  it('genera el CSV con el nombre del servidor y lo anuncia', async () => {
    const nombres = capturarDescargas();
    simulada.responder(
      'GET',
      CSV,
      () =>
        new Response(new Blob(['ID\r\n']), {
          status: 200,
          headers: { 'Content-Disposition': 'attachment; filename="siga-2026-I-v1.csv"' },
        }),
    );
    montarSpa('/exportacion-siga');

    await userEvent.click(await screen.findByRole('button', { name: 'Generar CSV para SIGA' }));

    await waitFor(() => expect(nombres).toEqual(['siga-2026-I-v1.csv']));
    expect(await screen.findByText('Se generó siga-2026-I-v1.csv.')).toHaveAttribute(
      'aria-live',
      'polite',
    );
  });

  // F19-H02 (D-120, FR-019a): el aviso de la hoja de cálculo y el rechazo por firmas o
  // inconsistencias.
  it('con publicaciones, avisa que el archivo va tal como sale y cómo abrirlo', async () => {
    montarSpa('/exportacion-siga');

    expect(await screen.findByText(AVISO_DEL_CSV_SIGA)).toBeVisible();
  });

  it('sin publicaciones, no muestra el aviso', async () => {
    simulada.responder('GET', '/api/reporting/publicaciones', json(200, []));
    montarSpa('/exportacion-siga');

    await screen.findByText(
      'Ningún periodo tiene un distributivo publicado. El CSV para SIGA sale de la versión publicada.',
    );
    expect(screen.queryByText(AVISO_DEL_CSV_SIGA)).toBeNull();
  });

  it('si la versión no se puede exportar, muestra el catálogo y cada condición que falla', async () => {
    simulada.responder(
      'GET',
      CSV,
      json(409, {
        titulo: 'Versión no exportable',
        descripcion:
          'La versión publicada no tiene todas sus firmas o tiene inconsistencias abiertas, así que no se exporta. Avisa al Vicerrector académico.',
        codigo: 'REPORTING_VERSION_NO_EXPORTABLE',
        detalles: [
          { campo: 'firmas', descripcion: 'Tiene 2 de 3 firmas.' },
          { campo: 'inconsistencias', descripcion: 'Tiene 1 dura y 2 blandas sin justificar.' },
        ],
      }),
    );
    montarSpa('/exportacion-siga');

    await userEvent.click(await screen.findByRole('button', { name: 'Generar CSV para SIGA' }));

    const alerta = await screen.findByRole('alert');
    expect(within(alerta).getByText('Versión no exportable')).toBeVisible();
    const lista = within(alerta).getByRole('list');
    expect(
      within(lista)
        .getAllByRole('listitem')
        .map((uno) => uno.textContent),
    ).toEqual(['Tiene 2 de 3 firmas.', 'Tiene 1 dura y 2 blandas sin justificar.']);
    expect(screen.queryByText(/Suma de verificación/)).toBeNull();
  });

  // F19-H02 (D-120): la suma de verificación, cómo comprobarla y las exportaciones del periodo.
  function conSuma() {
    simulada.responder(
      'GET',
      CSV,
      () =>
        new Response(new Blob(['ID\r\n']), {
          status: 200,
          headers: {
            'Content-Disposition': 'attachment; filename="siga-2026-I-v1.csv"',
            'X-Suma-Verificacion': SUMA,
          },
        }),
    );
  }

  it('después de generar, muestra la suma, cómo comprobarla y la exportación nueva', async () => {
    capturarDescargas();
    conSuma();
    montarSpa('/exportacion-siga');
    expect(
      await screen.findByText('Todavía no se exportó el CSV para SIGA de este periodo.'),
    ).toBeVisible();
    simulada.responder(
      'GET',
      EXPORTACIONES,
      json(200, {
        periodo: '2026-I',
        exportaciones: [
          exportada('00000000-0000-0000-0000-000000000001', 0, '2026-11-28T15:30:00Z'),
        ],
      }),
    );

    await userEvent.click(screen.getByRole('button', { name: 'Generar CSV para SIGA' }));

    expect(await screen.findByText('Suma de verificación (SHA-256):')).toBeVisible();
    expect(screen.getAllByText(SUMA)[0]).toBeVisible();
    expect(
      screen.getByText(
        'Para comprobarla: en Windows, certutil -hashfile siga-2026-I-v1.csv SHA256; en Linux, sha256sum siga-2026-I-v1.csv.',
      ),
    ).toBeVisible();
    const tabla = await screen.findByRole('table', {
      name: 'Exportaciones del CSV para SIGA de 2026-I',
    });
    expect(within(tabla).getByText('2026-11-28 10:30')).toBeVisible();
    expect(within(tabla).getByText('Por defecto')).toBeVisible();
  });

  it('copia la suma, y si el navegador no deja, dice cómo hacerlo a mano', async () => {
    capturarDescargas();
    conSuma();
    const escribir = vi
      .fn()
      .mockResolvedValueOnce(undefined)
      .mockRejectedValueOnce(new Error('no'));
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: escribir },
      configurable: true,
    });
    montarSpa('/exportacion-siga');
    await userEvent.click(await screen.findByRole('button', { name: 'Generar CSV para SIGA' }));
    const copiar = await screen.findByRole('button', { name: 'Copiar la suma de verificación' });

    await userEvent.click(copiar);
    expect(await screen.findByText('Se copió la suma de verificación.')).toBeVisible();
    expect(escribir).toHaveBeenCalledWith(SUMA);

    await userEvent.click(copiar);
    expect(
      await screen.findByText('No se pudo copiar; selecciónela y cópiela a mano.'),
    ).toBeVisible();
  });

  it('lista las exportaciones del periodo con su layout y su suma', async () => {
    simulada.responder(
      'GET',
      EXPORTACIONES,
      json(200, {
        periodo: '2026-I',
        exportaciones: [
          exportada(
            '00000000-0000-0000-0000-000000000002',
            2,
            '2026-11-28T16:00:00Z',
            'c'.repeat(64),
          ),
          exportada('00000000-0000-0000-0000-000000000001', 0, '2026-11-28T15:00:00Z'),
        ],
      }),
    );
    montarSpa('/exportacion-siga');

    const tabla = await screen.findByRole('table', {
      name: 'Exportaciones del CSV para SIGA de 2026-I',
    });
    const filas = within(tabla).getAllByRole('row').slice(1);
    expect(filas.map((fila) => within(fila).getAllByRole('cell')[3]?.textContent)).toEqual([
      'Versión 2',
      'Por defecto',
    ]);
    expect(within(filas[0] as HTMLElement).getByText('c'.repeat(64))).toBeVisible();
    expect(
      within(tabla)
        .getAllByRole('columnheader')
        .map((uno) => uno.textContent),
    ).toEqual(['Fecha y hora', 'Usuario', 'Versión', 'Layout', 'Archivo', 'Suma de verificación']);
  });

  it('si el periodo no tiene publicación, muestra el error del catálogo', async () => {
    simulada.responder(
      'GET',
      CSV,
      json(409, {
        titulo: 'Sin distributivo publicado',
        descripcion: 'El periodo todavía no tiene un distributivo publicado.',
        codigo: 'REPORTING_SIN_PUBLICACION',
      }),
    );
    montarSpa('/exportacion-siga');

    await userEvent.click(await screen.findByRole('button', { name: 'Generar CSV para SIGA' }));

    expect(
      within(await screen.findByRole('alert')).getByText('Sin distributivo publicado'),
    ).toBeVisible();
  });

  it('muestra el layout vigente con su traducción', async () => {
    simulada.responder('GET', LAYOUT, json(200, { vigente: cargado(2), anteriores: [] }));
    montarSpa('/exportacion-siga');

    const tabla = await screen.findByRole('table', { name: 'Layout vigente' });
    expect(within(tabla).getByText('CLASE → 01, GESTION → 05')).toBeVisible();
    expect(within(tabla).getAllByText('—')).toHaveLength(2);
  });

  it('descarga la plantilla', async () => {
    const nombres = capturarDescargas();
    simulada.responder(
      'GET',
      `${LAYOUT}/plantilla.yaml`,
      () =>
        new Response(new Blob(['# Layout']), {
          status: 200,
          headers: { 'Content-Disposition': 'attachment; filename="layout-siga-plantilla.yaml"' },
        }),
    );
    montarSpa('/exportacion-siga');

    await userEvent.click(await screen.findByRole('button', { name: 'Descargar plantilla' }));

    await waitFor(() => expect(nombres).toEqual(['layout-siga-plantilla.yaml']));
  });

  it('carga un archivo válido, lo anuncia y vuelve a pedir el layout', async () => {
    simulada.responder('PUT', LAYOUT, json(200, cargado(3)));
    montarSpa('/exportacion-siga');
    await screen.findByRole('table', { name: 'Layout vigente' });
    const antes = simulada.peticiones.filter(
      (una) => una.method === 'GET' && new URL(una.url).pathname === LAYOUT,
    ).length;

    await elegirArchivo('columnas: []');

    expect(
      await screen.findByText(
        'Se cargó la versión 3 del layout de exportación. El próximo CSV saldrá con sus columnas.',
      ),
    ).toBeVisible();
    await waitFor(() =>
      expect(
        simulada.peticiones.filter(
          (una) => una.method === 'GET' && new URL(una.url).pathname === LAYOUT,
        ).length,
      ).toBeGreaterThan(antes),
    );
  });

  it('si el archivo tiene problemas, los lista con su columna y lleva el foco a su título', async () => {
    simulada.responder(
      'PUT',
      LAYOUT,
      json(400, {
        titulo: 'Archivo de mapeo con problemas',
        descripcion: 'Corrige lo que se indica en cada columna y vuelve a cargar el archivo.',
        codigo: 'REPORTING_LAYOUT_INVALIDO',
        detalles: [
          { campo: 'columnas[3] (HORAS)', descripcion: 'El campo «horas_semanales» no existe.' },
          { campo: 'archivo', descripcion: 'Falta una columna con el campo «componente».' },
        ],
      }),
    );
    montarSpa('/exportacion-siga');

    await elegirArchivo('columnas: []');

    const titulo = await screen.findByRole('heading', {
      name: 'El archivo tiene 2 problemas. El layout vigente no cambió.',
    });
    await waitFor(() => expect(titulo).toHaveFocus());
    const aviso = screen.getByRole('alert');
    expect(
      within(aviso).getByText('columnas[3] (HORAS): El campo «horas_semanales» no existe.'),
    ).toBeVisible();
    expect(within(aviso).getByText('Falta una columna con el campo «componente».')).toBeVisible();
  });

  it('no envía un archivo de más de 256 KB', async () => {
    montarSpa('/exportacion-siga');

    await elegirArchivo('x', 256 * 1024 + 1);

    expect(
      within(await screen.findByRole('alert')).getByText('Archivo demasiado grande'),
    ).toBeVisible();
    expect(simulada.peticiones.filter((una) => una.method === 'PUT')).toHaveLength(0);
  });

  it('muestra las versiones anteriores y sus columnas al expandirlas', async () => {
    simulada.responder('GET', LAYOUT, json(200, { vigente: cargado(2), anteriores: [cargado(1)] }));
    montarSpa('/exportacion-siga');

    const boton = await screen.findByRole('button', { name: 'Ver las columnas de la versión 1' });
    expect(boton).toHaveAttribute('aria-expanded', 'false');
    await userEvent.click(boton);

    expect(
      screen.getByRole('button', { name: 'Ocultar las columnas de la versión 1' }),
    ).toHaveAttribute('aria-expanded', 'true');
    expect(await screen.findByRole('table', { name: 'Columnas de la versión 1' })).toBeVisible();
  });
});
