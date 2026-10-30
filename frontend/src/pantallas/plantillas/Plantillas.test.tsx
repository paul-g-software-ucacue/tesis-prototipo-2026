// La pantalla «Plantillas de carga» (F01-H01, FR-008): las 10 cargas en su orden, la descarga de la
// plantilla y del diccionario de cada una, y el diccionario desplegable. La ven quienes tienen
// «Cargas: lectura» o «Distributivo del periodo: lectura» (D-70).
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { FalloDeApi } from '../../errores/traducir';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import {
  confirmarCarga,
  descargarInforme,
  subirArchivo,
  type LecturaDeArchivo,
  type EstadoDeCarga,
  type ProblemaDeFila,
  type VistaPrevia,
} from './consultas';
import { DICCIONARIO, estadoCargado } from './datosDePrueba';

// En jsdom, FormData y File son de jsdom y el Request de Node no los acepta: la subida se simula
// aquí, y que el pedido real lleve la parte «archivo» y el token lo comprueba Playwright.
vi.mock('./consultas', async (original) => ({
  ...(await original<typeof import('./consultas')>()),
  subirArchivo: vi.fn(),
  descargarInforme: vi.fn(),
  confirmarCarga: vi.fn(),
}));

const HUELLA = 'c'.repeat(64);

/** Una vista previa sin errores: 1 nuevo, 1 que cambia, 1 baja y 44 iguales. */
const VISTA: VistaPrevia = {
  filasConErrores: 0,
  nuevos: 1,
  cambian: 1,
  bajas: 1,
  iguales: 44,
  huella: HUELLA,
  registros: [
    { clave: '0109990999', tipo: 'NUEVO', fila: 48, cambios: [] },
    {
      clave: '0109990010',
      tipo: 'CAMBIA',
      fila: 2,
      cambios: [{ campo: 'dedicacion', ahora: 'MT', archivo: 'TC' }],
    },
    { clave: '0109990028', tipo: 'BAJA', cambios: [] },
  ],
};

const DOCENTES = 'Docentes contratados (con ficha académica)';

function lectura(problemas: ProblemaDeFila[], total = problemas.length): LecturaDeArchivo {
  const errores = problemas.filter((problema) => problema.tipo === 'ERROR').length;
  const filasConErrores = new Set(
    problemas.filter((problema) => problema.tipo === 'ERROR').map((problema) => problema.fila),
  ).size;
  return {
    vistaPrevia: errores === 0 ? VISTA : { filasConErrores, registros: [] },
    carga: 'docentes',
    archivo: '06_docentes.csv',
    formato: 'CSV',
    registros: 47,
    valido: errores === 0,
    errores: total === problemas.length ? errores : total,
    advertencias: problemas.length - errores,
    problemas,
  };
}

const CEDULA_SIN_CERO: ProblemaDeFila = {
  fila: 2,
  campo: 'numero_identificacion',
  valor: '109990010',
  motivo: 'Cédula inválida o sin cero inicial: le falta el 0 inicial.',
  tipo: 'ERROR',
  sugerencia: '0109990010',
};

const ESPACIOS: ProblemaDeFila = {
  fila: 4,
  campo: 'dedicacion',
  valor: 'TC',
  motivo: 'Tiene espacios en los extremos: se quitan.',
  tipo: 'ADVERTENCIA',
};

/** El estado de las 10 cargas, todas cargadas salvo los cambios que se digan, por código. */
function estadoCon(cambios: Record<string, Partial<EstadoDeCarga>>): { cargas: EstadoDeCarga[] } {
  return {
    cargas: estadoCargado().cargas.map((carga) => ({ ...carga, ...cambios[carga.codigo] })),
  };
}

const FALTA_DOCENTES = { numero: 6, codigo: 'docentes', nombre: DOCENTES };

async function subirDocentes(): Promise<File> {
  const usuario = userEvent.setup();
  const archivo = new File(['tipo_identificacion\n'], '06_docentes.csv', { type: 'text/csv' });
  await usuario.upload(await screen.findByLabelText(`Archivo de ${DOCENTES}`), archivo);
  return archivo;
}

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
    simulada.responder('GET', '/api/cargas/estado', json(200, estadoCargado()));
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
    vi.mocked(descargarInforme).mockReset();
    vi.mocked(confirmarCarga).mockReset();
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
    vi.mocked(subirArchivo).mockResolvedValue(lectura([]));
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
      '06_docentes.csv es válido: 47 registros leídos.',
    );
    expect(screen.queryByRole('table', { name: /^Problemas de/ })).not.toBeInTheDocument();
    expect(subirArchivo).toHaveBeenCalledWith(
      expect.objectContaining({ codigo: 'docentes' }),
      archivo,
    );
  });

  it('si las filas tienen errores, dice que está rechazado y muestra cada problema', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['CARGAS:LECTURA', 'CARGAS:ESCRITURA']);
    vi.mocked(subirArchivo).mockResolvedValue(lectura([CEDULA_SIN_CERO, ESPACIOS]));
    montarSpa('/plantillas');

    await subirDocentes();

    const estado = await screen.findByRole('status');
    expect(estado).toHaveTextContent(
      '06_docentes.csv está rechazado: 1 error y 1 advertencia. Corrígelo y vuelve a subirlo.',
    );
    const tabla = screen.getByRole('table', { name: 'Problemas de 06_docentes.csv' });
    const [encabezado, cedula, espacios] = within(tabla).getAllByRole('row');
    expect(encabezado).toHaveTextContent('FilaCampoValorMotivoTipoSugerencia');
    expect(cedula).toHaveTextContent(
      '2numero_identificacion109990010Cédula inválida o sin cero inicial: le falta el 0 inicial.Error0109990010',
    );
    expect(espacios).toHaveTextContent(
      '4dedicacionTCTiene espacios en los extremos: se quitan.Advertencia',
    );
  });

  it('si solo tiene advertencias, es válido y las muestra', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['CARGAS:LECTURA', 'CARGAS:ESCRITURA']);
    vi.mocked(subirArchivo).mockResolvedValue(lectura([ESPACIOS]));
    montarSpa('/plantillas');

    await subirDocentes();

    expect(await screen.findByRole('status')).toHaveTextContent(
      '06_docentes.csv es válido, con 1 advertencia: 47 registros leídos.',
    );
    expect(screen.getByRole('table', { name: 'Problemas de 06_docentes.csv' })).toBeInTheDocument();
  });

  it('con más de 100 problemas muestra los primeros y dice cuántos hay', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['CARGAS:LECTURA', 'CARGAS:ESCRITURA']);
    const cien = Array.from({ length: 100 }, (_, i) => ({ ...CEDULA_SIN_CERO, fila: i + 2 }));
    vi.mocked(subirArchivo).mockResolvedValue(lectura(cien, 150));
    montarSpa('/plantillas');

    await subirDocentes();

    expect(
      await screen.findByText('Se muestran 100 de 150; el informe los trae todos.'),
    ).toBeInTheDocument();
    const tabla = screen.getByRole('table', { name: 'Problemas de 06_docentes.csv' });
    expect(within(tabla).getAllByRole('row')).toHaveLength(101);
  });

  it('descarga el informe enviando de nuevo el mismo archivo', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['CARGAS:LECTURA', 'CARGAS:ESCRITURA']);
    vi.mocked(subirArchivo).mockResolvedValue(lectura([CEDULA_SIN_CERO]));
    vi.mocked(descargarInforme).mockResolvedValue();
    const usuario = userEvent.setup();
    montarSpa('/plantillas');

    const archivo = await subirDocentes();
    await usuario.click(
      await screen.findByRole('button', { name: 'Descargar el informe de 06_docentes.csv' }),
    );

    expect(descargarInforme).toHaveBeenCalledWith(
      expect.objectContaining({ codigo: 'docentes' }),
      archivo,
    );
  });

  it('si la descarga del informe falla, muestra el error', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['CARGAS:LECTURA', 'CARGAS:ESCRITURA']);
    vi.mocked(subirArchivo).mockResolvedValue(lectura([CEDULA_SIN_CERO]));
    vi.mocked(descargarInforme).mockRejectedValue(
      new FalloDeApi(503, {
        codigo: 'COMUN_SERVICIO_NO_DISPONIBLE',
        titulo: 'Servicio no disponible',
        descripcion: 'Intente de nuevo en unos minutos.',
        detalles: [],
      }),
    );
    const usuario = userEvent.setup();
    montarSpa('/plantillas');

    await subirDocentes();
    await usuario.click(
      await screen.findByRole('button', { name: 'Descargar el informe de 06_docentes.csv' }),
    );

    expect(await screen.findByRole('alert')).toHaveTextContent('Servicio no disponible');
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

  it('muestra la vista previa con sus totales, sus pestañas y el aviso de las bajas', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['CARGAS:LECTURA', 'CARGAS:ESCRITURA']);
    vi.mocked(subirArchivo).mockResolvedValue(lectura([]));
    const usuario = userEvent.setup();
    montarSpa('/plantillas');

    await subirDocentes();

    const vista = await screen.findByRole('region', { name: 'Vista previa de 06_docentes.csv' });
    expect(
      within(vista).getByText(
        'Si confirmas: 1 nuevo, 1 cambia, 1 se da de baja y 44 quedan iguales.',
      ),
    ).toBeInTheDocument();
    expect(
      within(vista).getByText('Las bajas no se aplican: hazlas a mano en su pantalla.'),
    ).toBeInTheDocument();
    expect(within(vista).getByRole('table', { name: 'Nuevos' })).toHaveTextContent('0109990999');
    await usuario.click(within(vista).getByRole('tab', { name: 'Cambian (1)' }));
    expect(within(vista).getByRole('table', { name: 'Cambian' })).toHaveTextContent(
      'dedicacion: «MT» → «TC»',
    );
    await usuario.click(within(vista).getByRole('tab', { name: 'Se dan de baja (1)' }));
    expect(within(vista).getByRole('table', { name: 'Se dan de baja' })).toHaveTextContent(
      '0109990028',
    );
    expect(within(vista).getByRole('button', { name: 'Confirmar' })).toBeEnabled();
  });

  it('con errores no deja confirmar y dice por qué', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['CARGAS:LECTURA', 'CARGAS:ESCRITURA']);
    vi.mocked(subirArchivo).mockResolvedValue(lectura([CEDULA_SIN_CERO]));
    montarSpa('/plantillas');

    await subirDocentes();

    const vista = await screen.findByRole('region', { name: 'Vista previa de 06_docentes.csv' });
    expect(
      within(vista).getByText('1 fila tiene errores: corrígelas para poder confirmar.'),
    ).toBeInTheDocument();
    expect(within(vista).getByRole('button', { name: 'Confirmar' })).toBeDisabled();
    expect(
      within(vista).getByText(
        'Un archivo con errores no se confirma: la carga se aplica completa o nada.',
      ),
    ).toBeInTheDocument();
  });

  it('«Cancelar» quita la vista previa sin llamar al servidor', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['CARGAS:LECTURA', 'CARGAS:ESCRITURA']);
    vi.mocked(subirArchivo).mockResolvedValue(lectura([]));
    const usuario = userEvent.setup();
    montarSpa('/plantillas');

    await subirDocentes();
    await usuario.click(await screen.findByRole('button', { name: 'Cancelar' }));

    expect(screen.queryByRole('region', { name: /^Vista previa/ })).not.toBeInTheDocument();
    expect(confirmarCarga).not.toHaveBeenCalled();
  });

  it('«Confirmar» vuelve a enviar el archivo con su huella y muestra lo aplicado', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['CARGAS:LECTURA', 'CARGAS:ESCRITURA']);
    vi.mocked(subirArchivo).mockResolvedValue(lectura([]));
    vi.mocked(confirmarCarga).mockResolvedValue({
      resultado: 'APLICADA',
      nuevos: 1,
      cambiados: 1,
      bajasSinAplicar: 1,
      confirmadaEl: '2026-10-26T15:00:00Z',
    });
    const usuario = userEvent.setup();
    montarSpa('/plantillas');

    const archivo = await subirDocentes();
    await usuario.click(await screen.findByRole('button', { name: 'Confirmar' }));

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Carga aplicada: 1 nuevo y 1 cambiado; 1 baja quedó sin aplicar.',
    );
    expect(confirmarCarga).toHaveBeenCalledWith(
      expect.objectContaining({ codigo: 'docentes' }),
      archivo,
      HUELLA,
    );
    expect(screen.queryByRole('region', { name: /^Vista previa/ })).not.toBeInTheDocument();
  });

  it('un archivo ya confirmado dice cuándo y no se aplica otra vez', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['CARGAS:LECTURA', 'CARGAS:ESCRITURA']);
    vi.mocked(subirArchivo).mockResolvedValue(lectura([]));
    vi.mocked(confirmarCarga).mockResolvedValue({
      resultado: 'REPETIDA',
      nuevos: 0,
      cambiados: 0,
      bajasSinAplicar: 0,
      confirmadaEl: '2026-10-20T14:00:00Z',
    });
    const usuario = userEvent.setup();
    montarSpa('/plantillas');

    await subirDocentes();
    await usuario.click(await screen.findByRole('button', { name: 'Confirmar' }));

    expect(await screen.findByRole('status')).toHaveTextContent(
      /^Este archivo ya estaba confirmado el .*: no se aplicó otra vez\.$/,
    );
  });

  it('si el dueño rechaza la carga, muestra el título y la fila', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['CARGAS:LECTURA', 'CARGAS:ESCRITURA']);
    vi.mocked(subirArchivo).mockResolvedValue(lectura([]));
    vi.mocked(confirmarCarga).mockRejectedValue(
      new FalloDeApi(409, {
        codigo: 'CARGAS_CARGA_RECHAZADA',
        titulo: 'Carga rechazada',
        descripcion:
          'El servicio dueño rechazó la carga y no se aplicó ninguna fila. Revisa el detalle, corrige el archivo y vuelve a subirlo.',
        detalles: [
          {
            campo: 'fila 23',
            descripcion: 'Coordinador vigente: La carrera ya tiene un coordinador vigente.',
          },
        ],
      }),
    );
    const usuario = userEvent.setup();
    montarSpa('/plantillas');

    await subirDocentes();
    await usuario.click(await screen.findByRole('button', { name: 'Confirmar' }));

    const aviso = await screen.findByRole('alert');
    expect(aviso).toHaveTextContent('Carga rechazada');
    expect(aviso).toHaveTextContent(
      'fila 23: Coordinador vigente: La carrera ya tiene un coordinador vigente.',
    );
  });

  it('«Historial» abre los intentos de la carga, del más reciente al más antiguo', async () => {
    sesionCon(simulada, 'coordinacion.demo', ['CARGAS:LECTURA']);
    simulada.responder(
      'GET',
      '/api/cargas/plantillas/carreras/historial',
      json(200, [
        {
          usuario: 'datos.demo',
          momento: '2026-10-26T15:00:00Z',
          archivo: '02_carreras.csv',
          hash: 'a'.repeat(64),
          resultado: 'REPETIDA',
          nuevos: 0,
          cambiados: 0,
          bajasSinAplicar: 0,
        },
        {
          usuario: 'datos.demo',
          momento: '2026-10-26T14:00:00Z',
          archivo: '02_carreras.csv',
          hash: 'a'.repeat(64),
          resultado: 'RECHAZADA',
          nuevos: 0,
          cambiados: 0,
          bajasSinAplicar: 0,
          motivo: 'CARGAS_VISTA_PREVIA_DESACTUALIZADA',
        },
      ]),
    );
    const usuario = userEvent.setup();
    montarSpa('/plantillas');

    await usuario.click(
      await screen.findByRole('button', { name: 'Ver el historial de Carreras' }),
    );

    const dialogo = await screen.findByRole('dialog', { name: 'Historial de Carreras' });
    const [, primera, segunda] = within(dialogo).getAllByRole('row');
    expect(primera).toHaveTextContent('Repetida');
    expect(segunda).toHaveTextContent('Rechazada (Vista previa desactualizada)');
    expect(segunda).toHaveTextContent('aaaaaaaaaaaa');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cerrar' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
  it('cada carga dice si está cargada, pendiente o sin proyectos', async () => {
    sesionCon(simulada, 'coordinacion.demo', ['CARGAS:LECTURA']);
    simulada.responder(
      'GET',
      '/api/cargas/estado',
      json(
        200,
        estadoCon({
          carreras: { estado: 'PENDIENTE' },
          proyectos_practicas: { estado: 'SIN_PROYECTOS', periodosSinProyectos: ['2026-II'] },
        }),
      ),
    );
    montarSpa('/plantillas');

    expect(await filaDe('Estructura orgánica')).toHaveTextContent('Cargada');
    expect(await filaDe('Carreras')).toHaveTextContent('Pendiente');
    expect(await filaDe('Proyectos y prácticas aprobados')).toHaveTextContent(
      'Sin proyectos (2026-II)',
    );
  });

  it('con una carga anterior pendiente, «Subir archivo» está deshabilitado y dice cuál falta', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['CARGAS:LECTURA', 'CARGAS:ESCRITURA']);
    simulada.responder(
      'GET',
      '/api/cargas/estado',
      json(
        200,
        estadoCon({
          carreras: { estado: 'PENDIENTE' },
          asignaturas_malla: {
            estado: 'PENDIENTE',
            faltan: [{ numero: 2, codigo: 'carreras', nombre: 'Carreras' }],
          },
          cargos_autoridades: {
            estado: 'PENDIENTE',
            faltan: [{ numero: 2, codigo: 'carreras', nombre: 'Carreras' }, FALTA_DOCENTES],
          },
        }),
      ),
    );
    montarSpa('/plantillas');

    const asignaturas = await screen.findByRole('button', {
      name: 'Subir un archivo de Asignaturas y malla',
    });
    await waitFor(() => expect(asignaturas).toBeDisabled());
    expect(asignaturas).toHaveAccessibleDescription('Falta la carga de Carreras.');
    expect(
      screen.getByRole('button', { name: 'Subir un archivo de Cargos y autoridades' }),
    ).toHaveAccessibleDescription(`Faltan las cargas de Carreras y ${DOCENTES}.`);
    expect(screen.getByRole('button', { name: 'Subir un archivo de Carreras' })).toBeEnabled();
  });

  it('al confirmar la carga que faltaba, la siguiente se habilita sin recargar la página', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['CARGAS:LECTURA', 'CARGAS:ESCRITURA']);
    let docentesConfirmados = false;
    simulada.responder('GET', '/api/cargas/estado', () =>
      json(
        200,
        docentesConfirmados
          ? estadoCargado()
          : estadoCon({
              docentes: { estado: 'PENDIENTE' },
              cargos_autoridades: { estado: 'PENDIENTE', faltan: [FALTA_DOCENTES] },
            }),
      ),
    );
    vi.mocked(subirArchivo).mockResolvedValue(lectura([]));
    vi.mocked(confirmarCarga).mockImplementation(() => {
      docentesConfirmados = true;
      return Promise.resolve({
        resultado: 'APLICADA',
        nuevos: 47,
        cambiados: 0,
        bajasSinAplicar: 0,
        confirmadaEl: '2026-10-30T15:00:00Z',
      });
    });
    const usuario = userEvent.setup();
    montarSpa('/plantillas');
    const cargos = await screen.findByRole('button', {
      name: 'Subir un archivo de Cargos y autoridades',
    });
    await waitFor(() => expect(cargos).toBeDisabled());

    await subirDocentes();
    await usuario.click(await screen.findByRole('button', { name: 'Confirmar' }));

    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Subir un archivo de Cargos y autoridades' }),
      ).toBeEnabled(),
    );
    expect(await filaDe(DOCENTES)).toHaveTextContent('Cargada');
  });

  it('si no se puede saber el estado, lo dice y no muestra un estado falso', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['CARGAS:LECTURA', 'CARGAS:ESCRITURA']);
    simulada.responder(
      'GET',
      '/api/cargas/estado',
      json(500, {
        titulo: 'Error inesperado',
        descripcion: 'Ocurrió un error inesperado. Intenta de nuevo en unos minutos.',
        codigo: 'COMUN_ERROR_INTERNO',
        idIncidente: 'abc',
      }),
    );
    montarSpa('/plantillas');

    expect(await screen.findByRole('alert')).toHaveTextContent('Error inesperado');
    const carreras = await filaDe('Carreras');
    expect(carreras).not.toHaveTextContent('Cargada');
    expect(carreras).not.toHaveTextContent('Pendiente');
  });

  it('sin «Cargas: escritura» ve el estado, pero no puede subir ni confirmar «sin proyectos»', async () => {
    sesionCon(simulada, 'vicerrector.lector', ['DISTRIBUTIVO:LECTURA']);
    montarSpa('/plantillas');

    expect(await filaDe('Carreras')).toHaveTextContent('Cargada');
    expect(screen.queryByRole('button', { name: /^Subir un archivo/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /sin proyectos/ })).not.toBeInTheDocument();
  });

  describe('«Sin proyectos…»', () => {
    beforeEach(() => {
      sesionCon(simulada, 'consolidacion.demo', [
        'CARGAS:LECTURA',
        'CARGAS:ESCRITURA',
        'DISTRIBUTIVO:LECTURA',
      ]);
      simulada.responder(
        'GET',
        '/api/academico/periodos',
        json(200, [{ codigo: '2026-II' }, { codigo: '2026-I' }]),
      );
    });

    async function abrir() {
      const usuario = userEvent.setup();
      await usuario.click(
        await screen.findByRole('button', {
          name: 'Confirmar un periodo sin proyectos en Proyectos y prácticas aprobados',
        }),
      );
      const dialogo = await screen.findByRole('dialog', {
        name: 'Confirmar un periodo sin proyectos',
      });
      return { usuario, dialogo };
    }

    it('confirma un periodo y la carga de proyectos queda «sin proyectos»', async () => {
      let confirmado = false;
      simulada.responder('GET', '/api/cargas/estado', () =>
        json(
          200,
          estadoCon({
            proyectos_practicas: confirmado
              ? { estado: 'SIN_PROYECTOS', periodosSinProyectos: ['2026-II'] }
              : { estado: 'PENDIENTE' },
          }),
        ),
      );
      simulada.responder('POST', '/api/cargas/sin-proyectos', () => {
        confirmado = true;
        return json(201, {
          usuario: 'consolidacion.demo',
          momento: '2026-10-30T15:00:00Z',
          resultado: 'SIN_PROYECTOS',
          nuevos: 0,
          cambiados: 0,
          bajasSinAplicar: 0,
          periodo: '2026-II',
        });
      });
      montarSpa('/plantillas');
      const { usuario, dialogo } = await abrir();

      await usuario.type(within(dialogo).getByRole('combobox', { name: /Periodo/ }), '2026-II');
      await usuario.click(within(dialogo).getByRole('button', { name: 'Confirmar sin proyectos' }));

      expect(await screen.findByRole('status')).toHaveTextContent(
        'El periodo 2026-II quedó confirmado sin proyectos',
      );
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      const peticion = simulada.peticiones.find(
        (una) => una.method === 'POST' && una.url.endsWith('/api/cargas/sin-proyectos'),
      );
      expect(await peticion?.json()).toEqual({ periodo: '2026-II' });
      await waitFor(async () =>
        expect(await filaDe('Proyectos y prácticas aprobados')).toHaveTextContent(
          'Sin proyectos (2026-II)',
        ),
      );
    });

    it('un periodo mal escrito no se envía y dice cómo escribirlo', async () => {
      montarSpa('/plantillas');
      const { usuario, dialogo } = await abrir();

      await usuario.type(within(dialogo).getByRole('combobox', { name: /Periodo/ }), '2026-III');
      await usuario.click(within(dialogo).getByRole('button', { name: 'Confirmar sin proyectos' }));

      expect(
        within(dialogo).getByRole('combobox', { name: /Periodo/ }),
      ).toHaveAccessibleDescription('Escribe el periodo como 2026-I o 2026-II.');
      expect(simulada.peticiones.some((una) => una.method === 'POST')).toBe(false);
    });

    it('un periodo con proyectos aprobados muestra el error y no cierra el diálogo', async () => {
      simulada.responder(
        'POST',
        '/api/cargas/sin-proyectos',
        json(409, {
          titulo: 'El periodo tiene proyectos',
          descripcion:
            'No puedes confirmar «sin proyectos»: el periodo tiene proyectos aprobados que pueden recibir horas.',
          codigo: 'CARGAS_PERIODO_CON_PROYECTOS',
        }),
      );
      montarSpa('/plantillas');
      const { usuario, dialogo } = await abrir();

      await usuario.type(within(dialogo).getByRole('combobox', { name: /Periodo/ }), '2026-I');
      await usuario.click(within(dialogo).getByRole('button', { name: 'Confirmar sin proyectos' }));

      expect(await within(dialogo).findByRole('alert')).toHaveTextContent(
        'El periodo tiene proyectos',
      );
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('sugiere los periodos de los ciclos académicos', async () => {
      montarSpa('/plantillas');
      const { dialogo } = await abrir();

      await waitFor(() =>
        expect(
          [...dialogo.ownerDocument.querySelectorAll('#periodos-de-los-ciclos option')].map(
            (opcion) => opcion.getAttribute('value'),
          ),
        ).toEqual(['2026-II', '2026-I']),
      );
    });
  });

  it('el historial de proyectos muestra la confirmación «sin proyectos» con su periodo', async () => {
    sesionCon(simulada, 'coordinacion.demo', ['CARGAS:LECTURA']);
    simulada.responder(
      'GET',
      '/api/cargas/plantillas/proyectos_practicas/historial',
      json(200, [
        {
          usuario: 'consolidacion.demo',
          momento: '2026-10-30T15:00:00Z',
          resultado: 'SIN_PROYECTOS',
          nuevos: 0,
          cambiados: 0,
          bajasSinAplicar: 0,
          periodo: '2026-II',
        },
      ]),
    );
    const usuario = userEvent.setup();
    montarSpa('/plantillas');

    await usuario.click(
      await screen.findByRole('button', {
        name: 'Ver el historial de Proyectos y prácticas aprobados',
      }),
    );

    const dialogo = await screen.findByRole('dialog', {
      name: 'Historial de Proyectos y prácticas aprobados',
    });
    const [, fila] = within(dialogo).getAllByRole('row');
    expect(fila).toHaveTextContent('Sin proyectos (2026-II)');
    expect(fila).toHaveTextContent('consolidacion.demo');
  });
});
