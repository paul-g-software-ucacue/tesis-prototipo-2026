// La pantalla «Estructura orgánica» (F03-H01, FR-011): la estructura en su jerarquía, con el tipo, la
// gestión autorizada y el estado; con «Catálogos y datos maestros: escritura», registrar, corregir,
// inactivar y reactivar unidades. La leen también quienes leen el distributivo (D-70).
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { GESTIONES, UNIDADES } from './datosDePrueba';

const ADMINISTRADOR = ['CATALOGOS:LECTURA', 'CATALOGOS:ESCRITURA', 'USUARIOS_ROLES:LECTURA'];

async function cuerpoDe(simulada: ApiSimulada, metodo: string, ruta: string): Promise<unknown> {
  const peticion = simulada.peticiones.find(
    (candidata) => candidata.method === metodo && new URL(candidata.url).pathname === ruta,
  );
  if (!peticion) {
    throw new Error(`No hubo ${metodo} ${ruta}`);
  }
  return (await peticion.json()) as unknown;
}

function sesionCon(simulada: ApiSimulada, usuario: string, permisos: string[]) {
  simulada.responder(
    'GET',
    '/api/usuarios/sesion',
    json(200, { usuario, nombre: `${usuario} (demo)`, roles: [], permisos }),
  );
}

async function filas(): Promise<HTMLElement[]> {
  const tabla = await screen.findByRole('table', { name: 'Estructura orgánica' });
  return within(tabla).getAllByRole('row').slice(1);
}

async function filaDe(codigo: string): Promise<HTMLElement> {
  const fila = (await filas()).find((candidata) => within(candidata).queryByText(codigo));
  if (!fila) {
    throw new Error(`No está la unidad ${codigo}`);
  }
  return fila;
}

describe('Estructura orgánica', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder('GET', '/api/academico/unidades', json(200, UNIDADES));
    simulada.responder('GET', '/api/academico/gestiones-autorizadas', json(200, GESTIONES));
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion();
  });

  it('muestra cada unidad debajo de la que depende, con su tipo, su gestión y su estado', async () => {
    sesionCon(simulada, 'rectorado.demo', ['DISTRIBUTIVO:LECTURA']);
    montarSpa('/estructura');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Estructura orgánica' }),
    ).toBeInTheDocument();
    const codigos = (await filas()).map(
      (fila) => within(fila).getAllByRole('cell')[0]?.textContent,
    );
    expect(codigos).toEqual(['MATRIZ', 'RECT', 'CC-DS', 'G-CAL', 'COM-EVAL']);
    const rectorado = await filaDe('RECT');
    expect(rectorado).toHaveTextContent('Rectorado');
    expect(rectorado).toHaveTextContent('Gobierno');
    expect(rectorado).toHaveTextContent('Depende de MATRIZ');
    expect(rectorado).toHaveTextContent('G01 · Rector');
    expect(rectorado).toHaveTextContent('Activa');
    expect(await filaDe('MATRIZ')).toHaveTextContent('Sede matriz');
    expect(await filaDe('CC-DS')).toHaveTextContent('Unidad académica');
    expect(await filaDe('CC-DS')).toHaveTextContent('Inactiva');
    const comision = await filaDe('COM-EVAL');
    expect(comision).toHaveTextContent('Comisión');
    expect(comision).toHaveTextContent('Informativa, sin horas');
    expect(comision).toHaveTextContent('Permanente');
    expect(within(await filaDe('COM-EVAL')).getAllByRole('cell')[1]).toHaveStyle({
      paddingLeft: '64px',
    });
  });

  it('sin escritura no ofrece acciones', async () => {
    sesionCon(simulada, 'catalogos.demo', ['CATALOGOS:LECTURA']);
    montarSpa('/estructura');

    await filaDe('RECT');
    expect(screen.queryByRole('button', { name: 'Registrar unidad' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^Corregir/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^Inactivar/ })).not.toBeInTheDocument();
  });

  it.each([['DISTRIBUTIVO:LECTURA'], ['CATALOGOS:LECTURA']])(
    'con %s está en el menú',
    async (permiso) => {
      sesionCon(simulada, 'lector.demo', [permiso]);
      montarSpa('/');

      expect(await screen.findByRole('link', { name: 'Estructura orgánica' })).toBeInTheDocument();
    },
  );

  it('sin ninguno de los dos permisos no está en el menú y la ruta no la muestra', async () => {
    sesionCon(simulada, 'docente.demo', ['MI_DISTRIBUTIVO:LECTURA:PROPIO']);
    montarSpa('/estructura');

    expect(await screen.findByRole('alert')).toHaveTextContent('Acceso no autorizado');
    expect(screen.queryByRole('link', { name: 'Estructura orgánica' })).not.toBeInTheDocument();
    expect(
      simulada.peticiones.some((peticion) => peticion.url.includes('/api/academico/unidades')),
    ).toBe(false);
  });

  it('registra una gestión eligiendo su padre de las activas y su gestión del catálogo', async () => {
    sesionCon(simulada, 'admin.demo', ADMINISTRADOR);
    simulada.responder(
      'POST',
      '/api/academico/unidades',
      json(201, {
        codigo: 'G-INV',
        tipo: 'GESTION',
        nombre: 'Gestión de Investigación',
        padre: 'RECT',
        gestionAutorizada: 'G07',
        activa: true,
      }),
    );
    const usuario = userEvent.setup();
    montarSpa('/estructura');

    await usuario.click(await screen.findByRole('button', { name: 'Registrar unidad' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar unidad' });
    await usuario.type(within(dialogo).getByLabelText('Código'), 'G-INV');
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Gestión de Investigación');
    await usuario.selectOptions(within(dialogo).getByLabelText('Tipo'), 'GESTION');
    const padres = within(dialogo).getByLabelText('Depende de');
    expect(
      within(padres)
        .getAllByRole('option')
        .map((opcion) => opcion.getAttribute('value')),
    ).toEqual(['', 'COM-EVAL', 'G-CAL', 'MATRIZ', 'RECT']);
    await usuario.selectOptions(padres, 'RECT');
    const gestiones = within(dialogo).getByLabelText('Gestión autorizada');
    expect(
      within(gestiones)
        .getAllByRole('option')
        .map((opcion) => opcion.textContent),
    ).toEqual([
      'Elige una',
      'G01 · Rector',
      'G07 · Gestor de Investigación, Desarrollo Tecnológico e Innovación',
      'G10 · Coordinador de Aseguramiento de la Calidad',
    ]);
    expect(within(dialogo).queryByLabelText('¿Es permanente?')).not.toBeInTheDocument();
    await usuario.selectOptions(gestiones, 'G07');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(await cuerpoDe(simulada, 'POST', '/api/academico/unidades')).toEqual({
      codigo: 'G-INV',
      nombre: 'Gestión de Investigación',
      tipo: 'GESTION',
      padre: 'RECT',
      gestionAutorizada: 'G07',
    });
  });

  it('una comisión pide si es permanente y no ofrece gestión autorizada', async () => {
    sesionCon(simulada, 'admin.demo', ADMINISTRADOR);
    simulada.responder('POST', '/api/academico/unidades', json(201, UNIDADES[1]));
    const usuario = userEvent.setup();
    montarSpa('/estructura');

    await usuario.click(await screen.findByRole('button', { name: 'Registrar unidad' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar unidad' });
    await usuario.type(within(dialogo).getByLabelText('Código'), 'COM-INV');
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Comisión de Investigación');
    await usuario.selectOptions(within(dialogo).getByLabelText('Tipo'), 'COMISION');
    expect(within(dialogo).queryByLabelText('Gestión autorizada')).not.toBeInTheDocument();
    expect(dialogo).toHaveTextContent('Una comisión es un dato informativo, sin horas.');
    await usuario.selectOptions(within(dialogo).getByLabelText('Depende de'), 'RECT');
    await usuario.selectOptions(within(dialogo).getByLabelText('¿Es permanente?'), 'NO');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(await cuerpoDe(simulada, 'POST', '/api/academico/unidades')).toEqual({
      codigo: 'COM-INV',
      nombre: 'Comisión de Investigación',
      tipo: 'COMISION',
      padre: 'RECT',
      comisionPermanente: false,
    });
  });

  it('sin los datos obligatorios no envía nada', async () => {
    sesionCon(simulada, 'admin.demo', ADMINISTRADOR);
    const usuario = userEvent.setup();
    montarSpa('/estructura');

    await usuario.click(await screen.findByRole('button', { name: 'Registrar unidad' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar unidad' });
    await usuario.type(within(dialogo).getByLabelText('Código'), 'G-X');
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Gestión');
    await usuario.selectOptions(within(dialogo).getByLabelText('Tipo'), 'GESTION');
    await usuario.selectOptions(within(dialogo).getByLabelText('Depende de'), 'RECT');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));

    expect(dialogo).toHaveTextContent('Elige la gestión autorizada que representa.');
    expect(simulada.peticiones.some((peticion) => peticion.method === 'POST')).toBe(false);
  });

  it('un 400 marca el campo con su motivo, y un 409 se muestra con su título', async () => {
    sesionCon(simulada, 'admin.demo', ADMINISTRADOR);
    let respuestas = [
      json(400, {
        titulo: 'Solicitud inválida',
        descripcion: 'Revisa los campos marcados y vuelve a intentarlo.',
        codigo: 'COMUN_SOLICITUD_INVALIDA',
        detalles: [{ campo: 'padre', descripcion: 'La unidad RECT está inactiva' }],
      }),
      json(409, {
        titulo: 'Unidad repetida',
        descripcion: 'Ya existe una unidad con ese código. Elige otro.',
        codigo: 'ACADEMICO_UNIDAD_REPETIDA',
      }),
    ];
    simulada.responder('POST', '/api/academico/unidades', () => {
      const [primera, ...resto] = respuestas;
      respuestas = resto;
      return primera ?? json(500);
    });
    const usuario = userEvent.setup();
    montarSpa('/estructura');

    await usuario.click(await screen.findByRole('button', { name: 'Registrar unidad' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar unidad' });
    await usuario.type(within(dialogo).getByLabelText('Código'), 'OCS');
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Consejo');
    await usuario.selectOptions(within(dialogo).getByLabelText('Tipo'), 'GOBIERNO');
    await usuario.selectOptions(within(dialogo).getByLabelText('Depende de'), 'RECT');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));

    expect(await within(dialogo).findByText('La unidad RECT está inactiva')).toBeInTheDocument();
    expect(within(dialogo).getByLabelText('Depende de')).toHaveAttribute('aria-invalid', 'true');

    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));
    expect(await within(dialogo).findByText('Unidad repetida')).toBeInTheDocument();
    expect(dialogo).toHaveTextContent('Ya existe una unidad con ese código. Elige otro.');
  });

  it('corrige una unidad sin cambiar su código ni su tipo', async () => {
    sesionCon(simulada, 'admin.demo', ADMINISTRADOR);
    simulada.responder('PUT', '/api/academico/unidades/G-CAL', json(200, UNIDADES[2]));
    const usuario = userEvent.setup();
    montarSpa('/estructura');

    await usuario.click(
      within(await filaDe('G-CAL')).getByRole('button', { name: 'Corregir G-CAL' }),
    );
    const dialogo = await screen.findByRole('dialog', { name: 'Corregir la unidad G-CAL' });
    expect(within(dialogo).queryByLabelText('Código')).not.toBeInTheDocument();
    expect(within(dialogo).queryByLabelText('Tipo')).not.toBeInTheDocument();
    expect(dialogo).toHaveTextContent('Gestión');
    expect(
      within(within(dialogo).getByLabelText('Depende de'))
        .getAllByRole('option')
        .map((opcion) => opcion.getAttribute('value')),
    ).not.toContain('G-CAL');
    const nombre = within(dialogo).getByLabelText('Nombre');
    await usuario.clear(nombre);
    await usuario.type(nombre, 'Gestión de la Calidad');
    await usuario.selectOptions(within(dialogo).getByLabelText('Depende de'), 'MATRIZ');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(await cuerpoDe(simulada, 'PUT', '/api/academico/unidades/G-CAL')).toEqual({
      nombre: 'Gestión de la Calidad',
      padre: 'MATRIZ',
      gestionAutorizada: 'G10',
    });
  });

  it('inactiva con confirmación, y un 409 se muestra con su título', async () => {
    sesionCon(simulada, 'admin.demo', ADMINISTRADOR);
    simulada.responder(
      'PUT',
      '/api/academico/unidades/G-CAL/estado',
      json(409, {
        titulo: 'Unidad con dependientes',
        descripcion: 'La unidad tiene unidades activas que dependen de ella. Inactívalas antes.',
        codigo: 'ACADEMICO_UNIDAD_CON_DEPENDIENTES',
      }),
    );
    const usuario = userEvent.setup();
    montarSpa('/estructura');

    await usuario.click(
      within(await filaDe('G-CAL')).getByRole('button', { name: 'Inactivar G-CAL' }),
    );
    const dialogo = await screen.findByRole('dialog', { name: 'Inactivar la unidad G-CAL' });
    expect(dialogo).toHaveTextContent('No se borra');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Inactivar' }));

    expect(await screen.findByText('Unidad con dependientes')).toBeInTheDocument();
    expect(await cuerpoDe(simulada, 'PUT', '/api/academico/unidades/G-CAL/estado')).toEqual({
      activa: false,
    });
  });

  it('reactiva una unidad inactiva con confirmación', async () => {
    sesionCon(simulada, 'admin.demo', ADMINISTRADOR);
    simulada.responder(
      'PUT',
      '/api/academico/unidades/CC-DS/estado',
      json(200, { ...UNIDADES[0], activa: true }),
    );
    const usuario = userEvent.setup();
    montarSpa('/estructura');

    const fila = await filaDe('CC-DS');
    expect(within(fila).queryByRole('button', { name: 'Inactivar CC-DS' })).toBeNull();
    await usuario.click(within(fila).getByRole('button', { name: 'Reactivar CC-DS' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Reactivar la unidad CC-DS' });
    await usuario.click(within(dialogo).getByRole('button', { name: 'Reactivar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(await cuerpoDe(simulada, 'PUT', '/api/academico/unidades/CC-DS/estado')).toEqual({
      activa: true,
    });
  });
});
