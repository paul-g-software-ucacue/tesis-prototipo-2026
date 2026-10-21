// La pantalla «Periodos» (F02-H01, FR-010): la lista con su estado y su versión de normativa y, con
// la escritura de «Distributivo del periodo» (D-69), registrar, corregir, abrir y avanzar el estado.
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { PERIODOS, PLANIFICACION, VERSIONES, VIGENTE } from './datosDePrueba';

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

const ESCRITURA = [
  'DISTRIBUTIVO:LECTURA',
  'DISTRIBUTIVO:ESCRITURA',
  'PARAMETROS:LECTURA',
  'PARAMETROS:ESCRITURA',
];

async function filaDe(codigo: string): Promise<HTMLElement> {
  const tabla = await screen.findByRole('table', { name: 'Periodos' });
  const fila = within(tabla)
    .getAllByRole('row')
    .find((candidata) => within(candidata).queryByText(codigo));
  if (!fila) {
    throw new Error(`No está el periodo ${codigo}`);
  }
  return fila;
}

describe('Periodos', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder('GET', '/api/academico/periodos', json(200, PERIODOS));
    simulada.responder('GET', '/api/normativa/versiones', json(200, VERSIONES));
    simulada.responder('GET', '/api/roles/responsables', json(200, []));
    simulada.responder(
      'GET',
      '/api/roles/docentes',
      json(200, ['docente.coordinador.demo', 'docente.demo']),
    );
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion();
  });

  it('muestra cada periodo con sus fechas, su estado y su versión, en español', async () => {
    sesionCon(simulada, 'rectorado.demo', ['DISTRIBUTIVO:LECTURA']);
    montarSpa('/periodos');

    expect(await screen.findByRole('heading', { level: 1, name: 'Periodos' })).toBeInTheDocument();
    const planificacion = await filaDe('2026-2');
    expect(planificacion).toHaveTextContent('Periodo 2026-2');
    expect(planificacion).toHaveTextContent('01/09/2026');
    expect(planificacion).toHaveTextContent('28/02/2027');
    expect(planificacion).toHaveTextContent('Planificación');
    expect(planificacion).toHaveTextContent('Sin versión');
    const vigente = await filaDe('2026-1');
    expect(vigente).toHaveTextContent('Vigente');
    expect(vigente).toHaveTextContent('Versión 1');
    expect(document.title).toBe('Periodos · MDD');
    expect(screen.getByRole('link', { name: 'Periodos' })).toHaveAttribute('aria-current', 'page');
  });

  it('con solo lectura, la tabla no tiene acciones', async () => {
    sesionCon(simulada, 'rectorado.demo', ['DISTRIBUTIVO:LECTURA']);
    montarSpa('/periodos');

    await filaDe('2026-2');
    expect(screen.queryByRole('button', { name: 'Registrar periodo' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^Abrir/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^Pasar/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^Corregir/ })).not.toBeInTheDocument();
  });

  it('sin la lectura de «Distributivo del periodo», no está en el menú', async () => {
    sesionCon(simulada, 'docente.demo', ['MI_DISTRIBUTIVO:LECTURA']);
    montarSpa('/');

    expect(await screen.findByRole('link', { name: 'Inicio' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Periodos' })).not.toBeInTheDocument();
  });

  it('registra un periodo con su código, su nombre y sus fechas', async () => {
    sesionCon(simulada, 'vicerrectorado.demo', ESCRITURA);
    simulada.responder(
      'POST',
      '/api/academico/periodos',
      json(201, { ...PLANIFICACION, codigo: '2027-1', nombre: 'Periodo 2027-1' }),
    );
    const usuario = userEvent.setup();
    montarSpa('/periodos');

    await usuario.click(await screen.findByRole('button', { name: 'Registrar periodo' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar periodo' });
    await usuario.type(within(dialogo).getByLabelText('Código'), '2027-1');
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Periodo 2027-1');
    await usuario.type(within(dialogo).getByLabelText('Inicio'), '2027-03-01');
    await usuario.type(within(dialogo).getByLabelText('Fin'), '2027-08-15');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(await cuerpoDe(simulada, 'POST', '/api/academico/periodos')).toEqual({
      codigo: '2027-1',
      nombre: 'Periodo 2027-1',
      inicio: '2027-03-01',
      fin: '2027-08-15',
    });
  });

  it('un 400 marca el campo con su motivo, y un 409 se muestra con su título', async () => {
    sesionCon(simulada, 'vicerrectorado.demo', ESCRITURA);
    let respuestas = [
      json(400, {
        titulo: 'Solicitud inválida',
        descripcion: 'Revisa los campos marcados y vuelve a intentarlo.',
        codigo: 'COMUN_SOLICITUD_INVALIDA',
        detalles: [
          {
            campo: 'fin',
            descripcion:
              'La fecha de fin (2027-01-01) debe ser posterior a la de inicio (2027-03-01)',
          },
        ],
      }),
      json(409, {
        titulo: 'Código repetido',
        descripcion: 'Ya existe un periodo con ese código. Elige otro.',
        codigo: 'ACADEMICO_CODIGO_REPETIDO',
      }),
    ];
    simulada.responder('POST', '/api/academico/periodos', () => {
      const [primera, ...resto] = respuestas;
      respuestas = resto;
      return primera ?? json(500);
    });
    const usuario = userEvent.setup();
    montarSpa('/periodos');

    await usuario.click(await screen.findByRole('button', { name: 'Registrar periodo' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar periodo' });
    await usuario.type(within(dialogo).getByLabelText('Código'), '2026-2');
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Periodo');
    await usuario.type(within(dialogo).getByLabelText('Inicio'), '2027-03-01');
    await usuario.type(within(dialogo).getByLabelText('Fin'), '2027-01-01');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));

    expect(
      await within(dialogo).findByText(
        'La fecha de fin (2027-01-01) debe ser posterior a la de inicio (2027-03-01)',
      ),
    ).toBeInTheDocument();
    expect(within(dialogo).getByLabelText('Fin')).toHaveAttribute('aria-invalid', 'true');

    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));
    expect(await within(dialogo).findByRole('alert')).toHaveTextContent('Código repetido');
  });

  it('corrige un periodo sin cambiar su código, y no lo ofrece en uno cerrado', async () => {
    sesionCon(simulada, 'vicerrectorado.demo', ESCRITURA);
    simulada.responder(
      'GET',
      '/api/academico/periodos',
      json(200, [PLANIFICACION, { ...VIGENTE, estado: 'CERRADO' }]),
    );
    simulada.responder(
      'PUT',
      '/api/academico/periodos/2026-2',
      json(200, { ...PLANIFICACION, nombre: 'Periodo académico 2026-2' }),
    );
    const usuario = userEvent.setup();
    montarSpa('/periodos');

    expect(within(await filaDe('2026-1')).queryByRole('button', { name: /^Corregir/ })).toBeNull();
    await usuario.click(
      within(await filaDe('2026-2')).getByRole('button', { name: 'Corregir 2026-2' }),
    );
    const dialogo = await screen.findByRole('dialog', { name: 'Corregir el periodo 2026-2' });
    expect(within(dialogo).queryByLabelText('Código')).not.toBeInTheDocument();
    const nombre = within(dialogo).getByLabelText('Nombre');
    await usuario.clear(nombre);
    await usuario.type(nombre, 'Periodo académico 2026-2');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(await cuerpoDe(simulada, 'PUT', '/api/academico/periodos/2026-2')).toEqual({
      nombre: 'Periodo académico 2026-2',
      inicio: '2026-09-01',
      fin: '2027-02-28',
    });
  });

  it('abre un periodo eligiendo su versión de normativa de la lista', async () => {
    sesionCon(simulada, 'vicerrectorado.demo', ESCRITURA);
    simulada.responder(
      'PUT',
      '/api/academico/periodos/2026-2/normativa',
      json(200, { ...PLANIFICACION, version: 2 }),
    );
    const usuario = userEvent.setup();
    montarSpa('/periodos');

    expect(within(await filaDe('2026-1')).queryByRole('button', { name: /^Abrir/ })).toBeNull();
    await usuario.click(
      within(await filaDe('2026-2')).getByRole('button', { name: 'Abrir 2026-2' }),
    );
    const dialogo = await screen.findByRole('dialog', { name: 'Abrir el periodo 2026-2' });
    expect(dialogo).toHaveTextContent('la versión ya no se puede cambiar');
    const lista = within(dialogo).getByLabelText('Versión de normativa');
    expect(
      within(lista)
        .getAllByRole('option')
        .map((opcion) => opcion.textContent),
    ).toEqual([
      'Versión 1 · Política SENESCYT 2024 (rige 2026-1)',
      'Versión 2 · Reforma de la política 2025',
    ]);
    await usuario.selectOptions(lista, '2');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Abrir' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(await cuerpoDe(simulada, 'PUT', '/api/academico/periodos/2026-2/normativa')).toEqual({
      version: 2,
    });
  });

  it('sin versión no deja pasar a En aprobación, y dice por qué', async () => {
    sesionCon(simulada, 'vicerrectorado.demo', ESCRITURA);
    montarSpa('/periodos');

    const fila = await filaDe('2026-2');
    expect(
      within(fila).getByRole('button', { name: 'Pasar 2026-2 a En aprobación' }),
    ).toBeDisabled();
    expect(fila).toHaveTextContent('Ábrelo antes con su versión de normativa.');
  });

  it('pasa al estado siguiente, y pide confirmación antes de cerrar', async () => {
    sesionCon(simulada, 'vicerrectorado.demo', ESCRITURA);
    simulada.responder(
      'PUT',
      '/api/academico/periodos/2026-1/estado',
      json(200, { ...VIGENTE, estado: 'CERRADO' }),
    );
    const usuario = userEvent.setup();
    montarSpa('/periodos');

    await usuario.click(
      within(await filaDe('2026-1')).getByRole('button', { name: 'Pasar 2026-1 a Cerrado' }),
    );
    const dialogo = await screen.findByRole('dialog', { name: 'Cerrar el periodo 2026-1' });
    expect(dialogo).toHaveTextContent('ya no admite cambios');
    expect(simulada.peticiones.some((peticion) => peticion.method === 'PUT')).toBe(false);
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cerrar el periodo' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(await cuerpoDe(simulada, 'PUT', '/api/academico/periodos/2026-1/estado')).toEqual({
      estado: 'CERRADO',
    });
  });

  it('pasa a En aprobación sin confirmación, y un 409 se muestra con su título', async () => {
    sesionCon(simulada, 'vicerrectorado.demo', ESCRITURA);
    simulada.responder(
      'GET',
      '/api/academico/periodos',
      json(200, [{ ...PLANIFICACION, version: 2 }]),
    );
    simulada.responder(
      'PUT',
      '/api/academico/periodos/2026-2/estado',
      json(409, {
        titulo: 'Periodo cerrado',
        descripcion: 'El periodo está cerrado y ya no admite cambios.',
        codigo: 'ACADEMICO_PERIODO_CERRADO',
      }),
    );
    const usuario = userEvent.setup();
    montarSpa('/periodos');

    await usuario.click(
      within(await filaDe('2026-2')).getByRole('button', { name: 'Pasar 2026-2 a En aprobación' }),
    );

    expect(await screen.findByRole('alert')).toHaveTextContent('Periodo cerrado');
    expect(await cuerpoDe(simulada, 'PUT', '/api/academico/periodos/2026-2/estado')).toEqual({
      estado: 'EN_APROBACION',
    });
  });

  describe('Responsable de consolidación (F05-H03)', () => {
    it('muestra el Responsable de cada periodo, o «Sin designar»', async () => {
      sesionCon(simulada, 'rectorado.demo', ['DISTRIBUTIVO:LECTURA']);
      simulada.responder(
        'GET',
        '/api/roles/responsables',
        json(200, [
          { periodo: '2026-1', usuario: 'docente.demo', designadoEn: '2026-03-02T15:00:00Z' },
        ]),
      );
      montarSpa('/periodos');

      expect(await filaDe('2026-1')).toHaveTextContent('docente.demo');
      expect(await filaDe('2026-2')).toHaveTextContent('Sin designar');
      expect(screen.queryByRole('button', { name: /^Designar responsable/ })).toBeNull();
    });

    it('designa a un docente de la lista, y no lo ofrece en un periodo cerrado', async () => {
      sesionCon(simulada, 'vicerrectorado.demo', ESCRITURA);
      simulada.responder(
        'GET',
        '/api/academico/periodos',
        json(200, [PLANIFICACION, { ...VIGENTE, estado: 'CERRADO' }]),
      );
      simulada.responder(
        'PUT',
        '/api/roles/responsables/2026-2',
        json(200, {
          periodo: '2026-2',
          usuario: 'docente.demo',
          designadoEn: '2026-10-21T15:00:00Z',
        }),
      );
      const usuario = userEvent.setup();
      montarSpa('/periodos');

      expect(
        within(await filaDe('2026-1')).queryByRole('button', { name: /^Designar responsable/ }),
      ).toBeNull();
      await usuario.click(
        within(await filaDe('2026-2')).getByRole('button', {
          name: 'Designar responsable 2026-2',
        }),
      );
      const dialogo = await screen.findByRole('dialog', {
        name: 'Designar al Responsable de consolidación de 2026-2',
      });
      const lista = within(dialogo).getByLabelText('Docente');
      expect(
        within(lista)
          .getAllByRole('option')
          .map((opcion) => opcion.textContent),
      ).toEqual(['docente.coordinador.demo', 'docente.demo']);
      await usuario.selectOptions(lista, 'docente.demo');
      await usuario.click(within(dialogo).getByRole('button', { name: 'Designar' }));

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
      expect(await cuerpoDe(simulada, 'PUT', '/api/roles/responsables/2026-2')).toEqual({
        usuario: 'docente.demo',
      });
    });

    it('muestra el motivo de un 400, el título de un 409 y un aviso si el periodo aún no llegó', async () => {
      sesionCon(simulada, 'vicerrectorado.demo', ESCRITURA);
      let respuestas = [
        json(400, {
          titulo: 'Solicitud inválida',
          descripcion: 'Revisa los campos marcados y vuelve a intentarlo.',
          codigo: 'COMUN_SOLICITUD_INVALIDA',
          detalles: [
            {
              campo: 'usuario',
              descripcion: 'Solo se puede designar a un usuario con el rol Docente',
            },
          ],
        }),
        json(409, {
          titulo: 'Periodo cerrado',
          descripcion: 'El periodo está cerrado y ya no admite cambios.',
          codigo: 'ROLES_PERIODO_CERRADO',
        }),
        json(404, {
          titulo: 'Recurso no encontrado',
          descripcion:
            'La dirección solicitada no existe. Revisa la dirección y vuelve a intentarlo.',
          codigo: 'COMUN_RECURSO_NO_ENCONTRADO',
        }),
      ];
      simulada.responder('PUT', '/api/roles/responsables/2026-2', () => {
        const [primera, ...resto] = respuestas;
        respuestas = resto;
        return primera ?? json(500);
      });
      const usuario = userEvent.setup();
      montarSpa('/periodos');

      await usuario.click(
        within(await filaDe('2026-2')).getByRole('button', {
          name: 'Designar responsable 2026-2',
        }),
      );
      const dialogo = await screen.findByRole('dialog', {
        name: 'Designar al Responsable de consolidación de 2026-2',
      });
      const designar = within(dialogo).getByRole('button', { name: 'Designar' });
      await usuario.click(designar);
      expect(
        await within(dialogo).findByText('Solo se puede designar a un usuario con el rol Docente'),
      ).toBeInTheDocument();
      await usuario.click(designar);
      expect(await within(dialogo).findByRole('alert')).toHaveTextContent('Periodo cerrado');
      await usuario.click(designar);
      expect(
        await within(dialogo).findByText(
          'El periodo todavía no está disponible para designar. Inténtalo en unos segundos.',
        ),
      ).toBeInTheDocument();
    });
  });
});
