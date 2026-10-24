// La pantalla «Carreras y mallas» (F03-H02, FR-011): las carreras con sus mallas, y las asignaturas
// de la malla elegida por ciclo, con sus horas y los datos para la afinidad. Con «Catálogos:
// escritura» o «Distributivo: escritura» se registran, corrigen y cambian de estado (D-70).
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { UNIDADES } from '../estructura/datosDePrueba';
import { ASIGNATURAS, CAMPOS, CARRERAS } from './datosDePrueba';

const VICERRECTOR = ['DISTRIBUTIVO:LECTURA', 'DISTRIBUTIVO:ESCRITURA'];

function sesionCon(simulada: ApiSimulada, usuario: string, permisos: string[]) {
  simulada.responder(
    'GET',
    '/api/usuarios/sesion',
    json(200, { usuario, nombre: `${usuario} (demo)`, roles: [], permisos }),
  );
}

async function cuerpoDe(simulada: ApiSimulada, metodo: string, ruta: string): Promise<unknown> {
  const peticion = simulada.peticiones.find(
    (candidata) => candidata.method === metodo && new URL(candidata.url).pathname === ruta,
  );
  if (!peticion) {
    throw new Error(`No hubo ${metodo} ${ruta}`);
  }
  return (await peticion.json()) as unknown;
}

async function filaDeCarrera(codigo: string): Promise<HTMLElement> {
  const tabla = await screen.findByRole('table', { name: 'Carreras' });
  const fila = within(tabla)
    .getAllByRole('row')
    .find((candidata) => within(candidata).queryByText(codigo));
  if (!fila) {
    throw new Error(`No está la carrera ${codigo}`);
  }
  return fila;
}

describe('Carreras y mallas', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder('GET', '/api/academico/carreras', json(200, CARRERAS));
    simulada.responder('GET', '/api/academico/campos-amplios', json(200, CAMPOS));
    simulada.responder('GET', '/api/academico/unidades', json(200, UNIDADES));
    simulada.responder(
      'GET',
      '/api/academico/carreras/550613A01-P-0301/asignaturas',
      json(200, ASIGNATURAS),
    );
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion();
  });

  it('muestra cada carrera con sus datos en español y sus mallas con sus horas', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['DISTRIBUTIVO:LECTURA']);
    montarSpa('/carreras');

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Carreras y mallas' }),
    ).toBeInTheDocument();
    const ds = await filaDeCarrera('550613A01-P-0301');
    expect(ds).toHaveTextContent('DESARROLLO DE SOFTWARE');
    expect(ds).toHaveTextContent('DS');
    expect(ds).toHaveTextContent('MATRIZ');
    expect(ds).toHaveTextContent('Tecnológico superior');
    expect(ds).toHaveTextContent('Presencial');
    expect(ds).toHaveTextContent('Matutina, nocturna');
    expect(ds).toHaveTextContent('Vigente');
    expect(ds).toHaveTextContent('DS-2019 · Vigente · 11 h');
    const cont = await filaDeCarrera('550411C02-P-0301');
    expect(cont).toHaveTextContent('En cierre');
    expect(cont).toHaveTextContent('CONT-2019 · Derogada · 0 h');
  });

  it('al elegir una malla muestra sus asignaturas por ciclo con las horas y los datos para la afinidad', async () => {
    sesionCon(simulada, 'consolidacion.demo', ['DISTRIBUTIVO:LECTURA']);
    const usuario = userEvent.setup();
    montarSpa('/carreras');

    await usuario.click(await screen.findByRole('button', { name: 'Ver la malla DS-2019' }));

    const malla = await screen.findByRole('region', { name: 'Malla DS-2019' });
    expect(malla).toHaveTextContent('3 asignaturas · 11 horas semanales de clase');
    const ciclo1 = within(malla).getByRole('table', { name: 'Ciclo 1' });
    expect(within(ciclo1).getAllByRole('row').slice(1)).toHaveLength(2);
    expect(within(malla).getByRole('heading', { name: 'Ciclo 1 · 8 h' })).toBeInTheDocument();
    expect(within(malla).getByRole('heading', { name: 'Ciclo 2 · 3 h' })).toBeInTheDocument();
    const md = within(ciclo1).getByText('DS-B-I-MD-101').closest('tr') as HTMLElement;
    expect(md).toHaveTextContent('MATEMÁTICA DISCRETA');
    expect(md).toHaveTextContent('Básica');
    expect(md).toHaveTextContent('05 · Ciencias naturales, matemáticas y estadística');
    expect(md).toHaveTextContent('054');
    expect(md).toHaveTextContent('matemática; lógica');
    expect(md).toHaveTextContent('Tronco común');
    expect(within(malla).getByText('ASIGNATURA INACTIVA').closest('tr')).toHaveTextContent(
      'Inactiva',
    );
  });

  it('sin escritura no ofrece acciones', async () => {
    sesionCon(simulada, 'catalogos.demo', ['CATALOGOS:LECTURA']);
    const usuario = userEvent.setup();
    montarSpa('/carreras');

    await usuario.click(await screen.findByRole('button', { name: 'Ver la malla DS-2019' }));
    await screen.findByRole('region', { name: 'Malla DS-2019' });
    for (const nombre of [
      /^Registrar/,
      /^Corregir/,
      /^Cambiar el estado/,
      /^Derogar/,
      /^Inactivar/,
    ]) {
      expect(screen.queryByRole('button', { name: nombre })).not.toBeInTheDocument();
    }
  });

  it('el Vicerrector registra una carrera eligiendo la sede, el nivel, la modalidad, las jornadas y el campo', async () => {
    sesionCon(simulada, 'vicerrectorado.demo', VICERRECTOR);
    simulada.responder('POST', '/api/academico/carreras', json(201, CARRERAS[1]));
    const usuario = userEvent.setup();
    montarSpa('/carreras');

    await usuario.click(await screen.findByRole('button', { name: 'Registrar carrera' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar carrera' });
    await usuario.type(within(dialogo).getByLabelText('Código SENESCYT'), '550613a01-p-0301');
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'DESARROLLO DE SOFTWARE');
    await usuario.type(within(dialogo).getByLabelText('Siglas'), 'ds');
    await usuario.selectOptions(within(dialogo).getByLabelText('Sede'), 'MATRIZ');
    await usuario.selectOptions(
      within(dialogo).getByLabelText('Nivel de formación'),
      'TECNOLOGICO_SUPERIOR',
    );
    await usuario.selectOptions(within(dialogo).getByLabelText('Modalidad'), 'PRESENCIAL');
    await usuario.click(within(dialogo).getByLabelText('Nocturna'));
    await usuario.selectOptions(within(dialogo).getByLabelText('Campo amplio'), '06');
    await usuario.type(within(dialogo).getByLabelText('Campo específico'), '061');
    await usuario.clear(within(dialogo).getByLabelText('Número de ciclos'));
    await usuario.type(within(dialogo).getByLabelText('Número de ciclos'), '5');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));

    await waitFor(() =>
      expect(screen.queryByRole('dialog', { name: 'Registrar carrera' })).not.toBeInTheDocument(),
    );
    expect(await cuerpoDe(simulada, 'POST', '/api/academico/carreras')).toEqual({
      codigo: '550613A01-P-0301',
      nombre: 'DESARROLLO DE SOFTWARE',
      siglas: 'DS',
      sede: 'MATRIZ',
      nivelFormacion: 'TECNOLOGICO_SUPERIOR',
      modalidad: 'PRESENCIAL',
      jornadas: ['NOCTURNA'],
      campoAmplio: '06',
      campoEspecifico: '061',
      numeroCiclos: 5,
    });
  });

  it('un 400 marca su campo y un 409 muestra su título', async () => {
    sesionCon(simulada, 'admin.demo', ['CATALOGOS:LECTURA', 'CATALOGOS:ESCRITURA']);
    simulada.responder(
      'POST',
      '/api/academico/carreras',
      json(400, {
        titulo: 'Solicitud inválida',
        descripcion: 'Revisa los campos marcados y vuelve a intentarlo.',
        codigo: 'COMUN_SOLICITUD_INVALIDA',
        detalles: [{ campo: 'sede', descripcion: 'La sede está inactiva' }],
      }),
    );
    const usuario = userEvent.setup();
    montarSpa('/carreras');

    await usuario.click(await screen.findByRole('button', { name: 'Registrar carrera' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar carrera' });
    await usuario.type(within(dialogo).getByLabelText('Código SENESCYT'), 'C1');
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'N');
    await usuario.type(within(dialogo).getByLabelText('Siglas'), 'N');
    await usuario.selectOptions(within(dialogo).getByLabelText('Sede'), 'MATRIZ');
    await usuario.selectOptions(
      within(dialogo).getByLabelText('Nivel de formación'),
      'TECNICO_SUPERIOR',
    );
    await usuario.selectOptions(within(dialogo).getByLabelText('Modalidad'), 'DUAL');
    await usuario.click(within(dialogo).getByLabelText('Matutina'));
    await usuario.selectOptions(within(dialogo).getByLabelText('Campo amplio'), '04');
    await usuario.type(within(dialogo).getByLabelText('Campo específico'), '041');
    await usuario.type(within(dialogo).getByLabelText('Número de ciclos'), '4');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));

    expect(
      await within(dialogo).findByText(
        'La sede está inactiva. La sede matriz, una sede, una extensión o un campus.',
      ),
    ).toBeInTheDocument();
    expect(within(dialogo).getByLabelText('Sede')).toHaveAttribute('aria-invalid', 'true');
  });

  it('un error de formato muestra el motivo junto a la pista, que dice por qué dígitos empieza', async () => {
    sesionCon(simulada, 'vicerrectorado.demo', VICERRECTOR);
    simulada.responder(
      'POST',
      '/api/academico/asignaturas',
      json(400, {
        titulo: 'Solicitud inválida',
        descripcion: 'Revisa los campos marcados y vuelve a intentarlo.',
        codigo: 'COMUN_SOLICITUD_INVALIDA',
        detalles: [
          { campo: 'campoEspecifico', descripcion: 'no tiene el formato esperado' },
          { campo: 'palabrasClave', descripcion: 'debe tener al menos 2 elementos' },
        ],
      }),
    );
    const usuario = userEvent.setup();
    montarSpa('/carreras');

    await usuario.click(await screen.findByRole('button', { name: 'Ver la malla DS-2019' }));
    await usuario.click(
      await screen.findByRole('button', { name: 'Registrar asignatura en DS-2019' }),
    );
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar asignatura' });
    await usuario.type(within(dialogo).getByLabelText('Código'), 'A-1');
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'A');
    await usuario.type(within(dialogo).getByLabelText('Ciclo'), '1');
    await usuario.selectOptions(
      within(dialogo).getByLabelText('Unidad de organización curricular'),
      'BASICA',
    );
    await usuario.type(within(dialogo).getByLabelText('Horas semanales de clase'), '3');
    await usuario.selectOptions(within(dialogo).getByLabelText('Campo amplio'), '04');
    await usuario.type(within(dialogo).getByLabelText('Campo específico'), '04');
    await usuario.type(within(dialogo).getByLabelText('Palabras clave'), 'QW');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));

    expect(
      await within(dialogo).findByText(
        'No tiene el formato esperado. Tres dígitos que empiezan por 04, el campo amplio.',
      ),
    ).toBeInTheDocument();
    expect(
      within(dialogo).getByText(
        'Debe tener al menos 2 elementos. Al menos dos, separadas por punto y coma.',
      ),
    ).toBeInTheDocument();
  });

  it('registra una malla y una asignatura en ella, con las palabras clave separadas por punto y coma', async () => {
    sesionCon(simulada, 'vicerrectorado.demo', VICERRECTOR);
    simulada.responder(
      'POST',
      '/api/academico/carreras/550613A01-P-0301/mallas',
      json(201, {
        codigo: 'DS-AC-2026',
        estado: 'VIGENTE',
        asignaturas: 0,
        horasSemanalesClase: 0,
      }),
    );
    simulada.responder('POST', '/api/academico/asignaturas', json(201, ASIGNATURAS[0]));
    const usuario = userEvent.setup();
    montarSpa('/carreras');

    await usuario.click(
      await screen.findByRole('button', { name: 'Registrar una malla de DESARROLLO DE SOFTWARE' }),
    );
    const malla = await screen.findByRole('dialog', { name: 'Registrar malla' });
    await usuario.type(within(malla).getByLabelText('Código de versión'), 'ds-ac-2026');
    await usuario.click(within(malla).getByRole('button', { name: 'Registrar' }));
    expect(
      await cuerpoDe(simulada, 'POST', '/api/academico/carreras/550613A01-P-0301/mallas'),
    ).toEqual({ codigo: 'DS-AC-2026' });

    await usuario.click(await screen.findByRole('button', { name: 'Ver la malla DS-2019' }));
    await usuario.click(
      await screen.findByRole('button', { name: 'Registrar asignatura en DS-2019' }),
    );
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar asignatura' });
    await usuario.type(within(dialogo).getByLabelText('Código'), 'DS-B-I-MD-101');
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'MATEMÁTICA DISCRETA');
    await usuario.clear(within(dialogo).getByLabelText('Ciclo'));
    await usuario.type(within(dialogo).getByLabelText('Ciclo'), '1');
    await usuario.selectOptions(
      within(dialogo).getByLabelText('Unidad de organización curricular'),
      'BASICA',
    );
    await usuario.type(within(dialogo).getByLabelText('Horas semanales de clase'), '3');
    await usuario.click(within(dialogo).getByLabelText('Tronco común'));
    await usuario.selectOptions(within(dialogo).getByLabelText('Campo amplio'), '05');
    await usuario.type(within(dialogo).getByLabelText('Campo específico'), '054');
    await usuario.type(
      within(dialogo).getByLabelText('Palabras clave'),
      'matemática; lógica ;conjuntos',
    );
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));

    await waitFor(() =>
      expect(
        screen.queryByRole('dialog', { name: 'Registrar asignatura' }),
      ).not.toBeInTheDocument(),
    );
    expect(await cuerpoDe(simulada, 'POST', '/api/academico/asignaturas')).toEqual({
      codigo: 'DS-B-I-MD-101',
      carrera: '550613A01-P-0301',
      malla: 'DS-2019',
      nombre: 'MATEMÁTICA DISCRETA',
      ciclo: 1,
      unidadOrganizacionCurricular: 'BASICA',
      horasSemanalesClase: 3,
      troncoComun: true,
      campoAmplio: '05',
      campoEspecifico: '054',
      palabrasClave: ['matemática', 'lógica', 'conjuntos'],
    });
  });

  it('deroga una malla, inactiva una asignatura y cambia el estado de una carrera, con confirmación', async () => {
    sesionCon(simulada, 'vicerrectorado.demo', VICERRECTOR);
    simulada.responder(
      'PUT',
      '/api/academico/mallas/DS-2019/estado',
      json(200, { codigo: 'DS-2019', estado: 'DEROGADA', asignaturas: 3, horasSemanalesClase: 11 }),
    );
    simulada.responder(
      'PUT',
      '/api/academico/asignaturas/DS-B-I-MD-101/estado',
      json(200, { ...ASIGNATURAS[0], activa: false }),
    );
    simulada.responder(
      'PUT',
      '/api/academico/carreras/550613A01-P-0301/estado',
      json(200, { ...CARRERAS[1], estado: 'NO_VIGENTE' }),
    );
    const usuario = userEvent.setup();
    montarSpa('/carreras');

    await usuario.click(await screen.findByRole('button', { name: 'Ver la malla DS-2019' }));
    await usuario.click(await screen.findByRole('button', { name: 'Inactivar DS-B-I-MD-101' }));
    await usuario.click(
      within(
        await screen.findByRole('dialog', { name: 'Inactivar la asignatura DS-B-I-MD-101' }),
      ).getByRole('button', { name: 'Inactivar' }),
    );
    expect(
      await cuerpoDe(simulada, 'PUT', '/api/academico/asignaturas/DS-B-I-MD-101/estado'),
    ).toEqual({ activa: false });

    await usuario.click(await screen.findByRole('button', { name: 'Derogar DS-2019' }));
    await usuario.click(
      within(await screen.findByRole('dialog', { name: 'Derogar la malla DS-2019' })).getByRole(
        'button',
        { name: 'Derogar' },
      ),
    );
    expect(await cuerpoDe(simulada, 'PUT', '/api/academico/mallas/DS-2019/estado')).toEqual({
      estado: 'DEROGADA',
    });

    await usuario.click(
      await screen.findByRole('button', { name: 'Cambiar el estado de DESARROLLO DE SOFTWARE' }),
    );
    const estado = await screen.findByRole('dialog', { name: 'Cambiar el estado de la carrera' });
    await usuario.selectOptions(within(estado).getByLabelText('Estado'), 'NO_VIGENTE');
    await usuario.click(within(estado).getByRole('button', { name: 'Guardar' }));
    expect(
      await cuerpoDe(simulada, 'PUT', '/api/academico/carreras/550613A01-P-0301/estado'),
    ).toEqual({ estado: 'NO_VIGENTE' });
  });

  it.each([['DISTRIBUTIVO:LECTURA'], ['CATALOGOS:LECTURA']])(
    'con %s está en el menú',
    async (permiso) => {
      sesionCon(simulada, 'lector.demo', [permiso]);
      montarSpa('/');

      expect(await screen.findByRole('link', { name: 'Carreras y mallas' })).toBeInTheDocument();
    },
  );

  it('sin ninguno de los dos permisos no está en el menú y la ruta no la muestra', async () => {
    sesionCon(simulada, 'docente.demo', ['MI_DISTRIBUTIVO:LECTURA:PROPIO']);
    montarSpa('/carreras');

    expect(await screen.findByRole('alert')).toHaveTextContent('Acceso no autorizado');
    expect(screen.queryByRole('link', { name: 'Carreras y mallas' })).not.toBeInTheDocument();
  });
});
