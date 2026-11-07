// Elegir de una lista larga escribiendo (pedido del autor al probar F17-H02): la lista se acota con
// lo que se escribe, sin importar mayúsculas ni tildes, y al elegir queda el código de la opción.
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import { ConErrores, EleccionBuscable } from './Campos';

const DOCENTES = [
  ['0109990010', 'AGUIRRE TAPIA ANDREA VERONICA · 0109990010'],
  ['0109990020', 'ÁLVAREZ BORIS MAURICIO · 0109990020'],
  ['0109990036', 'DELGADO FAJARDO DIEGO ESTEBAN · 0109990036'],
] as const;

function Prueba({
  errores = {},
  inicial = '',
}: {
  errores?: Record<string, string>;
  inicial?: string;
}) {
  const [valor, setValor] = useState(inicial);
  return (
    <ConErrores errores={errores}>
      <EleccionBuscable
        etiqueta="Docente"
        campo="docente"
        valor={valor}
        opciones={DOCENTES}
        aviso={null}
        pista="Solo los docentes activos."
        alCambiar={setValor}
      />
      <output aria-label="Elegido">{valor}</output>
    </ConErrores>
  );
}

function opciones() {
  return within(screen.getByRole('listbox'))
    .getAllByRole('option')
    .map((una) => una.textContent);
}

describe('EleccionBuscable', () => {
  it('acota la lista con parte del nombre, sin importar mayúsculas', async () => {
    render(<Prueba />);

    await userEvent.type(screen.getByRole('combobox', { name: 'Docente' }), 'delgado');

    expect(opciones()).toEqual(['DELGADO FAJARDO DIEGO ESTEBAN · 0109990036']);
  });

  it('busca también por la cédula y sin tildes', async () => {
    render(<Prueba />);
    const campo = screen.getByRole('combobox', { name: 'Docente' });

    await userEvent.type(campo, '990010');
    expect(opciones()).toEqual(['AGUIRRE TAPIA ANDREA VERONICA · 0109990010']);

    await userEvent.clear(campo);
    await userEvent.type(campo, 'alvarez');
    expect(opciones()).toEqual(['ÁLVAREZ BORIS MAURICIO · 0109990020']);
  });

  it('al elegir deja el código de la opción y el nombre en el campo', async () => {
    render(<Prueba />);
    const campo = screen.getByRole('combobox', { name: 'Docente' });

    await userEvent.type(campo, 'diego');
    await userEvent.click(screen.getByRole('option', { name: /DELGADO FAJARDO/ }));

    expect(screen.getByLabelText('Elegido')).toHaveTextContent('0109990036');
    expect(campo).toHaveValue('DELGADO FAJARDO DIEGO ESTEBAN · 0109990036');
  });

  it('sin coincidencias lo dice', async () => {
    render(<Prueba />);

    await userEvent.type(screen.getByRole('combobox', { name: 'Docente' }), 'zzz');

    expect(screen.getByText('Ninguno coincide con lo que escribiste.')).toBeVisible();
  });

  it('muestra el valor que ya tiene, la pista y el error del formulario', () => {
    render(<Prueba inicial="0109990020" errores={{ docente: 'Elige un docente.' }} />);

    const campo = screen.getByRole('combobox', { name: 'Docente' });
    expect(campo).toHaveValue('ÁLVAREZ BORIS MAURICIO · 0109990020');
    expect(campo).toHaveAccessibleDescription('Elige un docente.');
    expect(campo).toHaveAttribute('aria-invalid', 'true');
  });
});
