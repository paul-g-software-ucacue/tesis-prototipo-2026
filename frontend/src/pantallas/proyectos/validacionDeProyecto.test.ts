// Lo que el formulario de un proyecto revisa antes de enviar (F03-H05, principio XIV, aclaraciones
// 5 a 7): los casos válidos, inválidos y en el límite de la carga 9.
import { describe, expect, it } from 'vitest';
import { filtrar } from '../comun/validacion';
import {
  datosDe,
  erroresEn,
  motivoDeLasHoras,
  nuevoDe,
  valoresVacios,
  type ValoresDelProyecto,
} from './validacionDeProyecto';

function con(cambios: Partial<ValoresDelProyecto>): ValoresDelProyecto {
  return {
    ...valoresVacios(),
    codigo: 'INV-2025-01',
    nombre: 'Prototipo de monitoreo IoT',
    tipo: 'INVESTIGACION',
    carreras: ['550613A01-P-0301'],
    resolucionOcs: 'RES-OCS-PRUEBA-2025-040',
    fechaResolucion: '2025-03-20',
    estado: 'EN_EJECUCION',
    vigenciaInicio: '2025-04-01',
    vigenciaFin: '2026-12-31',
    director: '0109990200',
    participantes: [
      { docente: '0109990200', rol: 'DIRECTOR', horas: '14' },
      { docente: '0109990226', rol: 'INVESTIGADOR', horas: '10' },
    ],
    horasAprobadas: '24',
    ...cambios,
  };
}

describe('el código del proyecto (aclaración 5)', () => {
  it('pasa a mayúsculas y pierde lo que no es letra, dígito o guion, hasta 30', () => {
    expect(filtrar('codigo', 'inv 2025_01', 30)).toBe('INV202501');
    expect(filtrar('codigo', 'inv-2025-01', 30)).toBe('INV-2025-01');
    expect(filtrar('codigo', 'A'.repeat(31), 30)).toHaveLength(30);
  });
});

describe('motivoDeLasHoras (aclaración 7)', () => {
  it.each(['0,01', '2,5', '2,55', '2,50', '99,75', '999,9', '24', '2.5'])('%s pasa', (horas) => {
    expect(motivoDeLasHoras(horas)).toBeUndefined();
  });

  it.each([
    ['', 'Escribe las horas semanales.'],
    ['0', 'Las horas tienen que ser mayores que 0.'],
    ['2,555', 'Las horas admiten hasta dos decimales, como 2,5 o 2,75.'],
    ['100,25', 'Las horas tienen hasta 5 caracteres, como 999,9 o 99,75.'],
    ['1000', 'Las horas tienen hasta 5 caracteres, como 999,9 o 99,75.'],
  ])('%s se rechaza con su motivo', (horas, motivo) => {
    expect(motivoDeLasHoras(horas)).toBe(motivo);
  });
});

describe('erroresEn', () => {
  it('un proyecto completo no tiene errores y envía las horas como número', () => {
    expect(erroresEn(con({}), true)).toEqual({});
    expect(datosDe(con({ horasAprobadas: '24,5' })).horasAprobadas).toBe(24.5);
    expect(nuevoDe(con({}))).toMatchObject({ codigo: 'INV-2025-01' });
  });

  it('un formulario vacío marca cada obligatorio y cada fila', () => {
    expect(erroresEn(valoresVacios(), true)).toMatchObject({
      codigo: 'Escribe el código del proyecto, como INV-2025-01.',
      nombre: 'Escribe el nombre del proyecto.',
      tipo: 'Elige el tipo de proyecto.',
      'carreras[0]': 'Elige la carrera o quita la fila.',
      resolucionOcs: 'Escribe la resolución del OCS que lo aprueba.',
      director: 'Elige el docente que dirige el proyecto.',
      horasAprobadas: 'Escribe las horas semanales.',
      'participantes[0].docente': 'Elige el docente o quita la fila.',
      'participantes[0].rol': 'Elige el rol.',
    });
  });

  it('una carrera o un participante repetidos y la vigencia al revés dicen qué hacer', () => {
    expect(
      erroresEn(
        con({
          carreras: ['550613A01-P-0301', '550613A01-P-0301'],
          vigenciaFin: '2025-04-01',
          participantes: [
            { docente: '0109990200', rol: 'DIRECTOR', horas: '14' },
            { docente: '0109990200', rol: 'INVESTIGADOR', horas: '10' },
          ],
        }),
        true,
      ),
    ).toEqual({
      'carreras[1]': 'Esta carrera ya está en la lista.',
      vigenciaFin: 'El fin de la vigencia tiene que ser posterior a su inicio.',
      'participantes[1].docente': 'Este docente ya está en la lista.',
    });
  });

  it('la suma exacta de horas pasa y una décima más se marca en los participantes', () => {
    expect(erroresEn(con({}), true)).toEqual({});
    expect(
      erroresEn(
        con({
          participantes: [
            { docente: '0109990200', rol: 'DIRECTOR', horas: '14' },
            { docente: '0109990226', rol: 'INVESTIGADOR', horas: '10,1' },
          ],
        }),
        true,
      ),
    ).toEqual({
      participantes: 'Las horas de los participantes suman 24,1 y pasan las 24 aprobadas.',
    });
  });

  it('al corregir no pide el código, que no cambia', () => {
    expect(erroresEn(con({ codigo: '' }), false)).toEqual({});
  });
});
