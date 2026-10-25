// La pantalla «Proyectos» (F03-H05, FR-013, RF-18): en el periodo elegido, cada proyecto dice si es
// asignable o por qué no; con escritura, registrar y corregir con las carreras y los participantes
// como listas y la validación en pantalla (principio XIV); sin escritura, solo lectura.
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { instalarApiSimulada, json, type ApiSimulada } from '../../pruebas/apiSimulada';
import { montarSpa } from '../../pruebas/montarSpa';
import { cerrarSesion, iniciarSesion } from '../../sesion/almacen';
import { REFERENCIAS, RESUMENES } from '../docentes/datosDePrueba';
import { INVESTIGACION, PRACTICAS_FUERA_DE_VIGENCIA } from './datosDePrueba';

const TALENTO_HUMANO = ['DOCENTES:LECTURA', 'DOCENTES:ESCRITURA'];
const CONSOLIDACION = ['OFERTA:LECTURA', 'DISTRIBUTIVO:LECTURA'];
const PROYECTOS = '/api/docentes/proyectos';

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

describe('Proyectos', () => {
  let simulada: ApiSimulada;

  beforeEach(() => {
    simulada = instalarApiSimulada();
    simulada.responder('GET', PROYECTOS, json(200, [INVESTIGACION, PRACTICAS_FUERA_DE_VIGENCIA]));
    simulada.responder('GET', '/api/docentes/docentes', json(200, RESUMENES));
    simulada.responder('GET', '/api/docentes/referencias', json(200, REFERENCIAS));
    iniciarSesion('cabecera.cuerpo.firma', new Date(Date.now() + 60_000));
  });

  afterEach(() => {
    cerrarSesion();
  });

  it('elige el periodo más reciente y dice si cada proyecto es asignable o por qué no', async () => {
    sesionCon(simulada, 'consolidacion.demo', CONSOLIDACION);
    const usuario = userEvent.setup();
    montarSpa('/proyectos');

    const tabla = await screen.findByRole('table', { name: 'Proyectos' });
    expect(screen.getByLabelText('Periodo')).toHaveValue('2026-II');
    expect(within(tabla).getByText('Asignable')).toBeInTheDocument();
    expect(
      within(tabla).getByText('No asignable por su estado y por su vigencia'),
    ).toBeInTheDocument();
    expect(within(tabla).getByText('Prácticas preprofesionales')).toBeInTheDocument();
    expect(within(tabla).getByText('2,5')).toBeInTheDocument();
    await waitFor(() =>
      expect(
        simulada.peticiones.some((peticion) => new URL(peticion.url).search === '?periodo=2026-II'),
      ).toBe(true),
    );

    await usuario.selectOptions(screen.getByLabelText('Periodo'), '2026-I');
    await waitFor(() =>
      expect(
        simulada.peticiones.some((peticion) => new URL(peticion.url).search === '?periodo=2026-I'),
      ).toBe(true),
    );
  });

  it('sin escritura no ofrece acciones y dice por qué', async () => {
    sesionCon(simulada, 'consolidacion.demo', CONSOLIDACION);
    montarSpa('/proyectos');

    expect(
      await screen.findByText('Solo lectura: tu rol no registra ni corrige proyectos.'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Registrar proyecto' })).not.toBeInTheDocument();
  });

  it('al registrar marca lo que falta y la suma de horas que pasa las aprobadas, sin enviar', async () => {
    sesionCon(simulada, 'datos.demo', TALENTO_HUMANO);
    simulada.responder('POST', PROYECTOS, json(201, INVESTIGACION));
    const usuario = userEvent.setup();
    montarSpa('/proyectos');

    await usuario.click(await screen.findByRole('button', { name: 'Registrar proyecto' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Registrar proyecto' });
    await escribir(usuario, within(dialogo).getByLabelText('Código'), 'inv 2025_01');
    expect(within(dialogo).getByLabelText('Código')).toHaveValue('INV202501');
    await usuario.clear(within(dialogo).getByLabelText('Código'));
    await escribir(usuario, within(dialogo).getByLabelText('Código'), 'inv-2025-01');
    await escribir(usuario, within(dialogo).getByLabelText('Nombre'), 'Prototipo de monitoreo IoT');
    await usuario.selectOptions(within(dialogo).getByLabelText('Tipo'), 'INVESTIGACION');
    await escribir(
      usuario,
      within(dialogo).getByLabelText('Resolución del OCS'),
      'RES-OCS-PRUEBA-2025-040',
    );
    await escribir(usuario, within(dialogo).getByLabelText('Fecha de la resolución'), '2025-03-20');
    await escribir(usuario, within(dialogo).getByLabelText('Inicio de la vigencia'), '2025-04-01');
    await escribir(usuario, within(dialogo).getByLabelText('Fin de la vigencia'), '2026-12-31');
    await usuario.selectOptions(within(dialogo).getByLabelText('Director'), '0109990010');
    await escribir(usuario, within(dialogo).getByLabelText('Horas semanales aprobadas'), '24');
    const carreras = within(dialogo).getByRole('group', { name: 'Carreras' });
    await usuario.selectOptions(within(carreras).getByLabelText('Carrera'), '550613A01-P-0301');
    const participantes = within(dialogo).getByRole('group', { name: 'Participantes' });
    await usuario.selectOptions(within(participantes).getByLabelText('Docente'), '0109990010');
    await usuario.selectOptions(within(participantes).getByLabelText('Rol'), 'DIRECTOR');
    await escribir(usuario, within(participantes).getByLabelText('Horas'), '24,5');

    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));
    expect(
      within(dialogo).getByText(
        'Las horas de los participantes suman 24,5 y pasan las 24 aprobadas.',
      ),
    ).toBeInTheDocument();
    expect(within(dialogo).getByRole('alert')).toHaveTextContent(
      'Revisa 1 campo marcados en rojo.',
    );
    expect(simulada.peticiones.some((peticion) => peticion.method === 'POST')).toBe(false);

    await usuario.clear(within(participantes).getByLabelText('Horas'));
    await escribir(usuario, within(participantes).getByLabelText('Horas'), '2,555');
    expect(
      within(participantes).getByText('Las horas admiten hasta dos decimales, como 2,5 o 2,75.'),
    ).toBeInTheDocument();
    await usuario.clear(within(participantes).getByLabelText('Horas'));
    await escribir(usuario, within(participantes).getByLabelText('Horas'), '14');
    await usuario.click(within(dialogo).getByRole('button', { name: 'Registrar' }));

    await waitFor(() =>
      expect(screen.queryByRole('dialog', { name: 'Registrar proyecto' })).not.toBeInTheDocument(),
    );
    const enviado = simulada.peticiones.find((peticion) => peticion.method === 'POST');
    expect(await enviado?.json()).toMatchObject({
      codigo: 'INV-2025-01',
      carreras: ['550613A01-P-0301'],
      participantes: [{ docente: '0109990010', rol: 'DIRECTOR', horas: 14 }],
      horasAprobadas: 24,
    });
  });

  it('un 400 del servidor marca su fila y un 409 muestra su título', async () => {
    sesionCon(simulada, 'datos.demo', TALENTO_HUMANO);
    simulada.responder(
      'PUT',
      `${PROYECTOS}/INV-2025-01`,
      json(400, {
        titulo: 'Solicitud inválida',
        descripcion: 'Revisa los campos marcados y vuelve a intentarlo.',
        codigo: 'COMUN_SOLICITUD_INVALIDA',
        detalles: [
          {
            campo: 'participantes[1].docente',
            descripcion: 'El participante tiene que ser un docente registrado',
          },
        ],
      }),
    );
    const usuario = userEvent.setup();
    montarSpa('/proyectos');

    await usuario.click(
      await screen.findByRole('button', { name: 'Corregir el proyecto INV-2025-01' }),
    );
    const dialogo = await screen.findByRole('dialog', { name: 'Corregir INV-2025-01' });
    expect(within(dialogo).getByLabelText('Código')).toBeDisabled();
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    const participantes = within(dialogo).getByRole('group', { name: 'Participantes' });
    expect(
      await within(participantes).findByText(
        'El participante tiene que ser un docente registrado.',
      ),
    ).toBeInTheDocument();
    expect(within(participantes).getAllByLabelText('Docente')[1]).toHaveAttribute(
      'aria-invalid',
      'true',
    );

    simulada.responder(
      'PUT',
      `${PROYECTOS}/INV-2025-01`,
      json(409, {
        titulo: 'Proyecto repetido',
        descripcion: 'Ya existe un proyecto con ese código. Búscalo y corrígelo.',
        codigo: 'DOCENTES_PROYECTO_REPETIDO',
      }),
    );
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));
    expect(await within(dialogo).findByText('Proyecto repetido')).toBeInTheDocument();
  });
});
