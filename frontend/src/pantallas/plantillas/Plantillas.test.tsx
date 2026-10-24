// La pantalla «Plantillas de carga» (F01-H01, FR-008): las 10 cargas en su orden, la descarga de la
// plantilla y del diccionario de cada una, y el diccionario desplegable. La ven quienes tienen
// «Cargas: lectura» o «Distributivo del periodo: lectura» (D-70).
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { FalloDeApi } from '../../errores/traducir';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { subirArchivo } from './consultas';
import { DICCIONARIO } from './datosDePrueba';

// En jsdom, FormData y File son de jsdom y el Request de Node no los acepta: la subida se simula
// aquí, y que el pedido real lleve la parte «archivo» y el token lo comprueba Playwright.
vi.mock('./consultas', async (original) => ({
  ...(await original<typeof import('./consultas')>()),
  subirArchivo: vi.fn(),
}));

function sesionCon(simulada: ApiSimulada, usuario: string, permisos: string[]) {
  simulada.responder(
    'GET',
    '/api/usuarios/sesion',
    json(200, { usuario, nombre: `${usuario} (demo)`, roles: [], permisos }),
  );
}

function csv(nombre: string, contenido: string): Response {
  return new Response(contenido, {
    status: 200,
    headers: {
      'Content-Type': 'text/csv;charset=UTF-8',
      'Content-Disposition': `attachment; filename="${nombre}"`,
    },
  });
}

async function filas(): Promise<HTMLElement[]> {
  const tabla = await screen.findByRole('table', { name: 'Plantillas de carga' });
  return within(tabla)
    .getAllByRole('row')
    .filter((fila) => fila.dataset['carga'] !== undefined);
}

async function filaDe(nombre: string): Promise<HTMLElement> {
  const fila = (await filas()).find((candidata) => within(candidata).queryByText(nombre));
  if (!fila) {
    throw new Error(`No está la carga ${nombre}`);
  }
  return fila;
}

describe('Plantillas de carga', () => {
  let simulada: ApiSimulada;
  let descargas: { nombre: string; enlace: string }[];

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder('GET', '/api/cargas/plantillas', json(200, DICCIONARIO));
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
    descargas = [];
    vi.stubGlobal(
      'URL',
      Object.assign(URL, {
        createObjectURL: vi.fn(() => 'blob:prueba'),
        revokeObjectURL: vi.fn(),
      }),
    );
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      descargas.push({ nombre: this.download, enlace: this.href });
    });
  });

  afterEach(() => {
    cerrarSesion();
    vi.restoreAllMocks();
    vi.mocked(subirArchivo).mockReset();
  });

  it('muestra las 10 cargas en su orden, con sus dependencias, su obligatoriedad y sus columnas', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['DISTRIBUTIVO:LECTURA']);
    montarSpa('/plantillas');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Plantillas de carga' }),
    ).toBeInTheDocument();
    const numeros = (await filas()).map(
      (fila) => within(fila).getAllByRole('cell')[0]?.textContent,
    );
    expect(numeros).toEqual(['1', '2', '3', '4', '5', '6', '7', '8', '9', '10']);
    const estructura = await filaDe('Estructura orgánica');
    expect(estructura).toHaveTextContent('—');
    expect(estructura).toHaveTextContent('Sí');
    expect(within(estructura).getAllByRole('cell')[4]).toHaveTextContent('2');
    expect(await filaDe('Oferta del periodo')).toHaveTextContent(
      'Ciclos académicos aprobados; Asignaturas y malla',
    );
    expect(await filaDe('Proyectos y prácticas aprobados')).toHaveTextContent(
      'Sí, o sin proyectos',
    );
    expect(await filaDe('Matriculados por carrera')).toHaveTextContent('Recomendada');
  });

  it('despliega el diccionario de una carga, con los valores de su catálogo', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['DISTRIBUTIVO:LECTURA']);
    const usuario = userEvent.setup();
    montarSpa('/plantillas');

    const boton = await screen.findByRole('button', {
      name: 'Ver el diccionario de Estructura orgánica',
    });
    expect(boton).toHaveAttribute('aria-expanded', 'false');
    expect(
      screen.queryByRole('table', { name: 'Diccionario de Estructura orgánica' }),
    ).not.toBeInTheDocument();
    await usuario.click(boton);

    expect(boton).toHaveAttribute('aria-expanded', 'true');
    const diccionario = screen.getByRole('table', { name: 'Diccionario de Estructura orgánica' });
    const columnas = within(diccionario).getAllByRole('row').slice(1);
    expect(columnas).toHaveLength(2);
    expect(columnas[0]).toHaveTextContent('codigo_unidad');
    expect(columnas[0]).toHaveTextContent('Código único de la unidad');
    expect(columnas[0]).toHaveTextContent('Texto');
    expect(columnas[0]).toHaveTextContent('20');
    expect(columnas[0]).toHaveTextContent('PK');
    expect(columnas[0]).toHaveTextContent('Único en el archivo');
    expect(columnas[0]).toHaveTextContent('MATRIZ');
    expect(columnas[1]).toHaveTextContent('Sí si tipo_unidad = GESTION');
    expect(columnas[1]).toHaveTextContent('G01 Rector; G02 Vicerrector');
    expect(columnas[1]).toHaveTextContent('G07');

    await usuario.click(boton);
    expect(
      screen.queryByRole('table', { name: 'Diccionario de Estructura orgánica' }),
    ).not.toBeInTheDocument();
  });

  it('muestra las convenciones comunes, plegadas', async () => {
    sesionCon(simulada, 'datos.demo', ['CARGAS:LECTURA', 'CARGAS:ESCRITURA']);
    const usuario = userEvent.setup();
    montarSpa('/plantillas');

    const boton = await screen.findByRole('button', { name: 'Convenciones de todas las cargas' });
    expect(screen.queryByText('aaaa-mm-dd (ISO 8601).')).not.toBeInTheDocument();
    await usuario.click(boton);

    expect(boton).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByText('aaaa-mm-dd (ISO 8601).')).toBeInTheDocument();
    expect(screen.getByText('S o N.')).toBeInTheDocument();
  });

  it('descarga la plantilla y el diccionario con el token y el nombre del servidor', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['DISTRIBUTIVO:LECTURA']);
    simulada.responder(
      'GET',
      '/api/cargas/plantillas/docentes/plantilla',
      csv('06_docentes.csv', '\uFEFFtipo_identificacion,numero_identificacion\r\n'),
    );
    simulada.responder(
      'GET',
      '/api/cargas/plantillas/estructura_organica/diccionario',
      csv('01_estructura_organica_diccionario.csv', '\uFEFFnumero,campo\r\n'),
    );
    const usuario = userEvent.setup();
    montarSpa('/plantillas');

    await usuario.click(
      await screen.findByRole('button', {
        name: 'Descargar la plantilla de Docentes contratados (con ficha académica)',
      }),
    );
    await usuario.click(
      screen.getByRole('button', { name: 'Descargar el diccionario de Estructura orgánica' }),
    );

    expect(descargas).toEqual([
      { nombre: '06_docentes.csv', enlace: 'blob:prueba' },
      { nombre: '01_estructura_organica_diccionario.csv', enlace: 'blob:prueba' },
    ]);
    const plantilla = simulada.peticiones.find((peticion) =>
      peticion.url.endsWith('/api/cargas/plantillas/docentes/plantilla'),
    );
    expect(plantilla?.headers.get('Authorization')).toBe('Bearer cabecera.cuerpo.firma');
  });

  it('si la descarga falla, muestra el título del error', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['DISTRIBUTIVO:LECTURA']);
    simulada.responder(
      'GET',
      '/api/cargas/plantillas/carreras/plantilla',
      json(503, {
        titulo: 'Servicio no disponible',
        descripcion: 'El servicio no está disponible en este momento.',
        codigo: 'COMUN_SERVICIO_NO_DISPONIBLE',
      }),
    );
    const usuario = userEvent.setup();
    montarSpa('/plantillas');

    await usuario.click(
      await screen.findByRole('button', { name: 'Descargar la plantilla de Carreras' }),
    );

    expect(await screen.findByRole('alert')).toHaveTextContent('Servicio no disponible');
    expect(descargas).toEqual([]);
  });

  it('con «Cargas: escritura» sube un archivo a su carga y muestra los registros leídos', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['DISTRIBUTIVO:LECTURA', 'CARGAS:ESCRITURA']);
    vi.mocked(subirArchivo).mockResolvedValue({
      carga: 'docentes',
      archivo: '06_docentes.csv',
      formato: 'CSV',
      registros: 47,
    });
    const usuario = userEvent.setup();
    montarSpa('/plantillas');

    const boton = await screen.findByRole('button', {
      name: 'Subir un archivo de Docentes contratados (con ficha académica)',
    });
    expect(boton).toBeInTheDocument();
    const archivo = new File(['tipo_identificacion\n'], '06_docentes.csv', { type: 'text/csv' });
    await usuario.upload(
      screen.getByLabelText('Archivo de Docentes contratados (con ficha académica)'),
      archivo,
    );

    expect(await screen.findByRole('status')).toHaveTextContent(
      '06_docentes.csv: 47 registros leídos.',
    );
    expect(subirArchivo).toHaveBeenCalledWith(
      expect.objectContaining({ codigo: 'docentes' }),
      archivo,
    );
  });

  it('si el archivo se rechaza, muestra el título, la descripción y cada detalle', async () => {
    sesionCon(simulada, 'datos.demo', ['CARGAS:LECTURA', 'CARGAS:ESCRITURA']);
    vi.mocked(subirArchivo).mockRejectedValue(
      new FalloDeApi(400, {
        titulo: 'Encabezados distintos de la plantilla',
        descripcion:
          'Las columnas del archivo no son las de la plantilla de la carga, en su orden.',
        codigo: 'CARGAS_ENCABEZADOS_DISTINTOS',
        detalles: [{ campo: 'codigo_carrera', descripcion: 'Falta en el archivo.' }],
      }),
    );
    const usuario = userEvent.setup();
    montarSpa('/plantillas');

    await usuario.upload(
      await screen.findByLabelText('Archivo de Carreras'),
      new File(['nombre\n'], 'carreras.csv', { type: 'text/csv' }),
    );

    const aviso = await screen.findByRole('alert');
    expect(aviso).toHaveTextContent('Encabezados distintos de la plantilla');
    expect(aviso).toHaveTextContent('codigo_carrera: Falta en el archivo.');
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('rechaza en la pantalla un archivo de más de 5 MB, sin enviarlo', async () => {
    sesionCon(simulada, 'datos.demo', ['CARGAS:LECTURA', 'CARGAS:ESCRITURA']);
    const usuario = userEvent.setup();
    montarSpa('/plantillas');

    const grande = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'grande.csv', {
      type: 'text/csv',
    });
    await usuario.upload(await screen.findByLabelText('Archivo de Carreras'), grande);

    expect(await screen.findByRole('alert')).toHaveTextContent('Archivo demasiado grande');
    expect(subirArchivo).not.toHaveBeenCalled();
  });

  it('sin «Cargas: escritura» no ofrece subir archivos', async () => {
    sesionCon(simulada, 'coordinacion.demo', ['DISTRIBUTIVO:LECTURA:PROPIO']);
    montarSpa('/plantillas');

    await filaDe('Carreras');
    expect(screen.queryByRole('button', { name: /^Subir un archivo/ })).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/^Archivo de/)).not.toBeInTheDocument();
  });

  it.each([['CARGAS:LECTURA'], ['DISTRIBUTIVO:LECTURA'], ['DISTRIBUTIVO:LECTURA:PROPIO']])(
    'con %s está en el menú',
    async (permiso) => {
      sesionCon(simulada, 'lector.demo', [permiso]);
      montarSpa('/');

      expect(await screen.findByRole('link', { name: 'Plantillas de carga' })).toBeInTheDocument();
    },
  );

  it('sin ninguno de los dos permisos no está en el menú y la ruta no la muestra', async () => {
    sesionCon(simulada, 'gestion.demo', ['PROYECTOS:LECTURA:PROPIO']);
    montarSpa('/plantillas');

    expect(await screen.findByRole('alert')).toHaveTextContent('Acceso no autorizado');
    expect(screen.queryByRole('link', { name: 'Plantillas de carga' })).not.toBeInTheDocument();
    expect(
      simulada.peticiones.some((peticion) => peticion.url.includes('/api/cargas/plantillas')),
    ).toBe(false);
  });
});
