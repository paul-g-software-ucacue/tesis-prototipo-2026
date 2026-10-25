// La pantalla «Cargos» (F03-H05, FR-013): la lista con sus filtros y sus advertencias; con escritura,
// registrar con la unidad que corresponde al cargo y la validación en pantalla (principio XIV),
// corregir y finalizar; sin escritura, solo lectura. Leen los mismos que leen los docentes (D-70).
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { REFERENCIAS, RESUMENES } from '../docentes/datosDePrueba';
import { COORDINADOR, GESTOR_SIN_CUARTO_NIVEL } from './datosDePrueba';

const TALENTO_HUMANO = ['DOCENTES:LECTURA', 'DOCENTES:ESCRITURA'];
const CONSOLIDACION = ['OFERTA:LECTURA', 'DISTRIBUTIVO:LECTURA'];
const CARGOS = '/api/docentes/cargos';

function sesionCon(simulada: ApiSimulada, usuario: string, permisos: string[]) {
  simulada.responder(
    'GET',
    '/api/usuarios/sesion',
    json(200, { usuario, nombre: `${usuario} (demo)`, roles: [], permisos }),
  );
}

async function escribir(
  usuario: ReturnType<typeof userEvent.setup>,
  campo: HTMLElement,
  texto: string,
) {
  await usuario.click(campo);
  await usuario.paste(texto);
}

describe('Cargos', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder('GET', CARGOS, json(200, [COORDINADOR, GESTOR_SIN_CUARTO_NIVEL]));
    simulada.responder('GET', '/api/docentes/docentes', json(200, RESUMENES));
    simulada.responder('GET', '/api/docentes/referencias', json(200, REFERENCIAS));
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion();
  });

  it('lista los cargos en español con su unidad o carrera y sus advertencias, y filtra por estado y docente', async () => {
    sesionCon(simulada, 'consolidacion.demo', CONSOLIDACION);
    const usuario = userEvent.setup();
    montarSpa('/cargos');

    const tabla = await screen.findByRole('table', { name: 'Cargos' });
    expect(within(tabla).getByText('Coordinador de carrera')).toBeInTheDocument();
    expect(within(tabla).getByText('DESARROLLO DE SOFTWARE')).toBeInTheDocument();
    expect(
      within(tabla).getByText('Gestor de Investigación, Desarrollo Tecnológico e Innovación (G07)'),
    ).toBeInTheDocument();
    expect(
      within(tabla).getByText('Le falta un título de cuarto nivel registrado (RN-20).'),
    ).toBeInTheDocument();
    expect(screen.getByText('2 cargos')).toBeInTheDocument();

    await usuario.selectOptions(screen.getByLabelText('Estado'), 'VIGENTE');
    await usuario.selectOptions(screen.getByLabelText('Docente'), '0109990028');
    await waitFor(() =>
      expect(
        simulada.peticiones.some(
          (peticion) => new URL(peticion.url).search === '?estado=VIGENTE&docente=0109990028',
        ),
      ).toBe(true),
    );
  });

  it('sin escritura no ofrece acciones y dice por qué', async () => {
    sesionCon(simulada, 'consolidacion.demo', CONSOLIDACION);
    montarSpa('/cargos');

    expect(
      await screen.findByText('Solo lectura: tu rol no registra ni corrige cargos.'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Registrar cargo' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^Corregir el cargo/ })).not.toBeInTheDocument();
  });

  it('al registrar marca de una vez lo que falta, ofrece la unidad del cargo y avisa la advertencia del servidor', async () => {
    sesionCon(simulada, 'datos.demo', TALENTO_HUMANO);
    simulada.responder('POST', CARGOS, json(201, GESTOR_SIN_CUARTO_NIVEL));
    const usuario = userEvent.setup();
    montarSpa('/cargos');

    await usuario.click(await screen.findByRole('button', { name: 'Registrar cargo' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar cargo' });
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));
    expect(within(dialogo).getByRole('alert')).toHaveTextContent(
      'Revisa 4 campos marcados en rojo.',
    );
    expect(within(dialogo).getByText('Elige el docente.')).toBeInTheDocument();
    expect(simulada.peticiones.some((peticion) => peticion.method === 'POST')).toBe(false);

    await usuario.selectOptions(within(dialogo).getByLabelText('Docente'), '0109990028');
    await usuario.selectOptions(
      within(dialogo).getByLabelText('Cargo'),
      'G07_GESTOR_INVESTIGACION',
    );
    const unidad = within(dialogo).getByLabelText('Unidad que dirige');
    expect(
      within(unidad)
        .getAllByRole('option')
        .map((opcion) => opcion.textContent),
    ).toEqual(['Elige una', 'G-INV · Gestión de Investigación']);
    await usuario.selectOptions(unidad, 'G-INV');
    await escribir(usuario, within(dialogo).getByLabelText('Fecha de designación'), '2025-04-01');
    await escribir(
      usuario,
      within(dialogo).getByLabelText('Fin de funciones (opcional)'),
      '2027-04-02',
    );
    await escribir(
      usuario,
      within(dialogo).getByLabelText('Documento de designación'),
      'RES-PRUEBA-2025-012',
    );
    expect(
      within(dialogo).getByText(
        'Una designación dura hasta 2 años: el fin de funciones va hasta 2027-04-01.',
      ),
    ).toBeInTheDocument();
    await usuario.clear(within(dialogo).getByLabelText('Fin de funciones (opcional)'));
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));

    await waitFor(() =>
      expect(screen.queryByRole('dialog', { name: 'Registrar cargo' })).not.toBeInTheDocument(),
    );
    const enviado = simulada.peticiones.find((peticion) => peticion.method === 'POST');
    expect(await enviado?.json()).toEqual({
      docente: '0109990028',
      tipo: 'G07_GESTOR_INVESTIGACION',
      fechaDesignacion: '2025-04-01',
      unidad: 'G-INV',
      documento: 'RES-PRUEBA-2025-012',
      estado: 'VIGENTE',
    });
    expect(
      screen.getByText(/El cargo de CABRERA VAZQUEZ ANA GABRIELA se guardó\. Le falta un título/),
    ).toBeInTheDocument();
  });

  it('el coordinador de carrera elige su carrera y no su unidad', async () => {
    sesionCon(simulada, 'datos.demo', TALENTO_HUMANO);
    const usuario = userEvent.setup();
    montarSpa('/cargos');

    await usuario.click(await screen.findByRole('button', { name: 'Registrar cargo' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar cargo' });
    await usuario.selectOptions(within(dialogo).getByLabelText('Cargo'), 'COORDINADOR_CARRERA');

    expect(within(dialogo).getByLabelText('Carrera que coordina')).toBeInTheDocument();
    expect(within(dialogo).queryByLabelText('Unidad que dirige')).not.toBeInTheDocument();
  });

  it('finaliza un cargo con su fecha de fin, y un 409 muestra su título', async () => {
    sesionCon(simulada, 'datos.demo', TALENTO_HUMANO);
    const ruta = `${CARGOS}/0109990010/COORDINADOR_CARRERA/2025-04-01`;
    simulada.responder(
      'PUT',
      ruta,
      json(409, {
        titulo: 'Coordinador vigente',
        descripcion:
          'La carrera ya tiene un coordinador vigente. Finaliza su cargo antes de registrar otro.',
        codigo: 'DOCENTES_COORDINADOR_VIGENTE',
      }),
    );
    const usuario = userEvent.setup();
    montarSpa('/cargos');

    await usuario.click(
      await screen.findByRole('button', {
        name: 'Corregir el cargo de PERALTA MALDONADO JUAN ANDRES: Coordinador de carrera',
      }),
    );
    const dialogo = await screen.findByRole('dialog', {
      name: 'Corregir el cargo de PERALTA MALDONADO JUAN ANDRES',
    });
    expect(within(dialogo).getByLabelText('Docente')).toBeDisabled();
    await usuario.selectOptions(within(dialogo).getByLabelText('Estado'), 'FINALIZADO');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    expect(await within(dialogo).findByText('Coordinador vigente')).toBeInTheDocument();
    const enviado = simulada.peticiones.find((peticion) => peticion.method === 'PUT');
    expect(await enviado?.json()).toEqual({
      carrera: '550613A01-P-0301',
      fechaFin: '2027-03-31',
      documento: 'RES-PRUEBA-2025-004',
      estado: 'FINALIZADO',
    });
  });

  it('el menú lleva a «Cargos» y «Proyectos» con la lectura de los docentes', async () => {
    sesionCon(simulada, 'consolidacion.demo', CONSOLIDACION);
    montarSpa('/');

    expect(await screen.findByRole('link', { name: 'Cargos' })).toHaveAttribute('href', '/cargos');
    expect(screen.getByRole('link', { name: 'Proyectos' })).toHaveAttribute('href', '/proyectos');
  });

  it('con solo «Proyectos: lectura» no muestra «Cargos» ni «Proyectos» (D-64)', async () => {
    sesionCon(simulada, 'gestor.demo', ['PROYECTOS:LECTURA']);
    montarSpa('/');

    expect(await screen.findByRole('button', { name: /cerrar sesión/i })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Cargos' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Proyectos' })).not.toBeInTheDocument();
  });
});
