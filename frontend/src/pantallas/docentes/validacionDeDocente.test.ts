// Lo que el formulario del docente admite y revisa antes de enviar (F03-H04): los filtros de cada
// campo y los motivos, que repiten las reglas del servidor con palabras para quien llena el
// formulario.
import { describe, expect, it } from 'vitest';
import { PERALTA } from './datosDePrueba';
import { valoresDe, valoresVacios, type ValoresDelDocente } from './formularioDeDocente';
import { filtrar } from '../comun/validacion';
import { erroresEn } from './validacionDeDocente';

const AMPLIOS = new Set(['01', '06']);
const HOY = new Date(2026, 9, 24);

function con(cambios: Partial<ValoresDelDocente>): ValoresDelDocente {
  return { ...valoresDe(PERALTA), ...cambios };
}

describe('filtrar', () => {
  it('deja solo dígitos en un número, y corta en su largo también al pegar', () => {
    expect(filtrar('digitos', '01a09-99 0010xx', 10)).toBe('0109990010');
    expect(filtrar('digitos', '01099900109', 10)).toBe('0109990010');
  });

  it('deja letras con tilde, eñe, espacio, apóstrofo y guion en un nombre', () => {
    expect(filtrar('letras', "Peña-O'Neil Núñez 2", 60)).toBe("Peña-O'Neil Núñez ");
  });

  it('admite una sola coma o punto en un decimal', () => {
    expect(filtrar('decimal', '2.5', 4)).toBe('2,5');
    expect(filtrar('decimal', '2,5,1a', 4)).toBe('2,51');
  });

  it('pone en mayúsculas un código y quita los espacios de un correo', () => {
    expect(filtrar('codigo', 'ab 12-c', 13)).toBe('AB12-C');
    expect(filtrar('correo', ' juan @x.ec ')).toBe('juan@x.ec');
  });
});

describe('erroresEn', () => {
  it('un docente completo y válido no tiene errores', () => {
    expect(erroresEn(valoresDe(PERALTA), false, AMPLIOS, HOY)).toEqual({});
  });

  it('un formulario vacío marca cada obligatorio con lo que hay que hacer', () => {
    const errores = erroresEn(valoresVacios(), true, AMPLIOS, HOY);
    expect(errores).toMatchObject({
      identificacion: 'Escribe el número de cédula.',
      primerApellido: 'Escribe el primer apellido.',
      correoInstitucional: 'Escribe el correo institucional.',
      tipoPersonal: 'Elige el tipo de personal.',
      horasContratadas: 'Escribe las horas contratadas por semana.',
      carreraBase: 'Elige la carrera base.',
      'ficha.tercerNivel.campoEspecifico':
        'Escribe el código del campo específico, de 3 dígitos (por ejemplo, 061).',
    });
    expect(errores).not.toHaveProperty('segundoApellido');
    expect(errores).not.toHaveProperty('telefono');
  });

  it.each([
    ['01099', 'La cédula tiene 10 dígitos; escribiste 5.'],
    ['2509990010', 'Los dos primeros dígitos de la cédula son la provincia: de 01 a 24, o 30.'],
    ['0169990010', 'El tercer dígito de la cédula va de 0 a 5.'],
    [
      '0109990011',
      'Esta cédula no es válida: el último dígito no corresponde a los demás. Revisa que esté bien escrita.',
    ],
  ])('la cédula %s se rechaza con su motivo', (cedula, motivo) => {
    expect(erroresEn(con({ identificacion: cedula }), true, AMPLIOS, HOY).identificacion).toBe(
      motivo,
    );
  });

  it('una cédula válida, una de provincia 30 y un pasaporte pasan', () => {
    expect(erroresEn(con({}), true, AMPLIOS, HOY)).toEqual({});
    expect(erroresEn(con({ identificacion: '3000000004' }), true, AMPLIOS, HOY)).toEqual({});
    expect(
      erroresEn(
        con({ tipoIdentificacion: 'PASAPORTE', identificacion: 'AB12345' }),
        true,
        AMPLIOS,
        HOY,
      ),
    ).toEqual({});
  });

  it('el teléfono, el correo, las horas y el fin del contrato dicen qué esperan', () => {
    const errores = erroresEn(
      con({
        telefono: '0991',
        correoInstitucional: 'juan.peralta',
        horasContratadas: '45',
        inicioContrato: '2026-04-01',
        finContrato: '2026-03-31',
      }),
      false,
      AMPLIOS,
      HOY,
    );
    expect(errores).toEqual({
      telefono: 'El teléfono tiene de 7 a 10 dígitos; escribiste 4.',
      correoInstitucional: 'Escribe un correo completo, como nombre.apellido@instituto.edu.ec.',
      horasContratadas: 'Las horas contratadas van de 1 a 40 por semana.',
      finContrato: 'El fin del contrato tiene que ser posterior a su inicio.',
    });
  });

  it('el campo específico de tercer nivel empieza por el campo amplio elegido', () => {
    expect(
      erroresEn(con({ campoAmplio: '06', campoEspecifico: '011' }), false, AMPLIOS, HOY),
    ).toEqual({
      'ficha.tercerNivel.campoEspecifico': 'Tiene que empezar por 06, el campo amplio elegido.',
    });
  });

  it('en las listas, cada fila marca su campo con su posición', () => {
    const errores = erroresEn(
      con({
        cuartoNivel: [
          { titulo: 'MAGÍSTER', tipo: 'MAESTRIA', campoEspecifico: '99', anio: '2027' },
        ],
        cursos: [{ nombre: 'JAVA', campoEspecifico: '991', horas: '0', anio: '1949' }],
        experiencia: [{ cargo: 'ANALISTA', institucion: 'X', campoEspecifico: '061', anios: '0' }],
        asignaturasAfines: ['DS-P-II-POO-204', 'DS-P-II-POO-204', ''],
      }),
      false,
      AMPLIOS,
      HOY,
    );
    expect(errores).toEqual({
      'ficha.cuartoNivel[0].campoEspecifico':
        'Escribe el código del campo específico, de 3 dígitos (por ejemplo, 061).',
      'ficha.cuartoNivel[0].anio': 'El año va de 1950 a 2026.',
      'ficha.cursos[0].campoEspecifico':
        'Los dos primeros dígitos tienen que ser un campo amplio de la lista (por ejemplo, 06).',
      'ficha.cursos[0].horas': 'Las horas van de 1 a 2000.',
      'ficha.cursos[0].anio': 'El año va de 1950 a 2026.',
      'ficha.experiencia[0].anios': 'Los años van de más de 0 a 60; admite decimales, como 2,5.',
      'ficha.asignaturasAfines[1]': 'Esta asignatura ya está en la lista.',
      'ficha.asignaturasAfines[2]': 'Elige la asignatura o quita la fila.',
    });
  });
});
