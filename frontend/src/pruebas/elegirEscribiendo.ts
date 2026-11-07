// Elige en una EleccionBuscable como lo hace el usuario: escribe en el campo y pulsa la opción que
// contiene lo escrito (por ejemplo, la cédula de un docente).
import { screen, within } from '@testing-library/react';
import type { UserEvent } from '@testing-library/user-event';

export async function elegirEscribiendo(usuario: UserEvent, campo: HTMLElement, texto: string) {
  await usuario.clear(campo);
  await usuario.type(campo, texto);
  const lista = await screen.findByRole('listbox');
  await usuario.click(within(lista).getByRole('option', { name: new RegExp(texto) }));
}
