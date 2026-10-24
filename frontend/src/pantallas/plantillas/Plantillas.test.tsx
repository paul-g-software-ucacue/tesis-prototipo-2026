// La pantalla «Plantillas de carga» (F01-H01, FR-008): las 10 cargas en su orden, la descarga de la
// plantilla y del diccionario de cada una, y el diccionario desplegable. La ven quienes tienen
// «Cargas: lectura» o «Distributivo del periodo: lectura» (D-70).
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { DICCIONARIO } from './datosDePrueba';

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
