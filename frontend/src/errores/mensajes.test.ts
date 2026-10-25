import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';
import {
  MENSAJE_ACCESO_NO_AUTORIZADO,
  MENSAJE_ERROR_INTERNO,
  MENSAJE_SERVICIO_NO_DISPONIBLE,
} from './mensajes';

// Las filas de contratos/comun/errores.md: | `CODIGO` | estado | título | descripción | causa | …
function filaDelCatalogo(codigo: string) {
  const catalogo = readFileSync(resolve(__dirname, '../../../contratos/comun/errores.md'), 'utf-8');
  const fila = catalogo.split('\n').find((linea) => linea.startsWith(`| \`${codigo}\` |`));
  if (!fila) {
    throw new Error(`${codigo} no está en errores.md`);
  }
  const [, , , titulo, descripcion] = fila.split('|').map((celda) => celda.trim());
  return { titulo, descripcion };
}

describe('mensajes genéricos', () => {
  it.each([
    ['COMUN_ERROR_INTERNO', MENSAJE_ERROR_INTERNO],
    ['COMUN_SERVICIO_NO_DISPONIBLE', MENSAJE_SERVICIO_NO_DISPONIBLE],
    ['SEGURIDAD_ACCESO_NO_AUTORIZADO', MENSAJE_ACCESO_NO_AUTORIZADO],
  ])('%s coincide con contratos/comun/errores.md', (codigo, mensaje) => {
    expect(mensaje).toEqual(filaDelCatalogo(codigo));
  });
});
