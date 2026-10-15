import { describe, expect, it } from 'vitest';
import { MENSAJE_ERROR_INTERNO, MENSAJE_SERVICIO_NO_DISPONIBLE } from './mensajes';
import { FalloDeApi, traducirError } from './traducir';

const TECNICO = [/\b[45]\d\d\b/, /Error:/, /Exception/, /\bat /, /COMUN_/, /USUARIOS_/];

function sinTextoTecnico(mensaje: object) {
  const texto = JSON.stringify(mensaje);
  TECNICO.forEach((patron) => expect(texto).not.toMatch(patron));
}

describe('traducirError', () => {
  it('toma el titulo y la descripcion del modelo común y nunca el código', () => {
    const mensaje = traducirError(
      new FalloDeApi(401, {
        titulo: 'Credenciales inválidas',
        descripcion: 'El usuario o la contraseña no son correctos.',
        codigo: 'USUARIOS_CREDENCIALES_INVALIDAS',
      }),
    );

    expect(mensaje).toEqual({
      titulo: 'Credenciales inválidas',
      descripcion: 'El usuario o la contraseña no son correctos.',
      detalles: [],
    });
    sinTextoTecnico(mensaje);
  });

  it('conserva los detalles por campo y el identificador del incidente', () => {
    const conDetalles = traducirError(
      new FalloDeApi(400, {
        titulo: 'Solicitud inválida',
        descripcion: 'Revisa los campos marcados y vuelve a intentarlo.',
        codigo: 'COMUN_SOLICITUD_INVALIDA',
        detalles: [{ campo: 'usuario', descripcion: 'es obligatorio' }],
      }),
    );
    const conIncidente = traducirError(
      new FalloDeApi(500, {
        titulo: 'Error inesperado',
        descripcion: 'Ocurrió un error inesperado.',
        codigo: 'COMUN_ERROR_INTERNO',
        idIncidente: '6f1c2b9e-1d2a-4c3b-8e4f-5a6b7c8d9e0f',
      }),
    );

    expect(conDetalles.detalles).toEqual([{ campo: 'usuario', descripcion: 'es obligatorio' }]);
    expect(conIncidente.idIncidente).toBe('6f1c2b9e-1d2a-4c3b-8e4f-5a6b7c8d9e0f');
    sinTextoTecnico(conDetalles);
  });

  it.each([
    ['un cuerpo HTML', '<html><body>502 Bad Gateway</body></html>'],
    ['un cuerpo vacío', undefined],
    ['un JSON con otra forma', { error: 'Internal Server Error', status: 500, trace: 'at x' }],
  ])('sin el modelo común (%s) da el mensaje de error interno', (_caso, cuerpo) => {
    const mensaje = traducirError(new FalloDeApi(502, cuerpo));

    expect(mensaje).toEqual({ ...MENSAJE_ERROR_INTERNO, detalles: [] });
    expect(mensaje.idIncidente).toBeUndefined();
    sinTextoTecnico(mensaje);
  });

  it('un fallo de red da el mensaje de servicio no disponible, sin el texto del fallo', () => {
    const mensaje = traducirError(new TypeError('Failed to fetch'));

    expect(mensaje).toEqual({ ...MENSAJE_SERVICIO_NO_DISPONIBLE, detalles: [] });
    expect(JSON.stringify(mensaje)).not.toContain('Failed to fetch');
  });

  it('cualquier otra excepción da el mensaje de error interno, sin su texto', () => {
    const mensaje = traducirError(new Error('NullPointerException at ec.mdd.X'));

    expect(mensaje).toEqual({ ...MENSAJE_ERROR_INTERNO, detalles: [] });
    sinTextoTecnico(mensaje);
  });
});
