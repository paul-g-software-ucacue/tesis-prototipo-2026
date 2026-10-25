// Lo que el formulario de un cargo revisa antes de enviar (F03-H05, principio XIV): los casos
// válidos, inválidos y en el límite de la carga 8, con un motivo que dice qué hacer.
import { describe, expect, it } from 'vitest';
import { filtrar } from '../comun/validacion';
import {
  datosDe,
  dosAniosDespues,
  erroresEn,
  nuevoDe,
  valoresVacios,
  type ValoresDelCargo,
} from './validacionDeCargo';

function con(cambios: Partial<ValoresDelCargo>): ValoresDelCargo {
  return {
    ...valoresVacios(),
    docente: '0109990010',
    tipo: 'COORDINADOR_CARRERA',
    fechaDesignacion: '2025-04-01',
    carrera: '550613A01-P-0301',
    documento: 'RES-PRUEBA-2025-004',
    ...cambios,
  };
}

describe('erroresEn', () => {
  it('un coordinador de carrera completo no tiene errores y no envía unidad', () => {
    expect(erroresEn(con({ unidad: 'CC-DS' }))).toEqual({});
    expect(datosDe(con({ unidad: 'CC-DS' }))).not.toHaveProperty('unidad');
    expect(nuevoDe(con({}))).toMatchObject({ carrera: '550613A01-P-0301', estado: 'VIGENTE' });
  });

  it('un formulario vacío marca cada obligatorio con lo que hay que hacer', () => {
    expect(erroresEn(valoresVacios())).toEqual({
      docente: 'Elige el docente.',
      tipo: 'Elige el cargo.',
      fechaDesignacion: 'Elige la fecha de designación.',
      documento: 'Escribe la resolución o la acción de personal.',
    });
  });

  it('el coordinador pide su carrera y los demás, su unidad', () => {
    expect(erroresEn(con({ carrera: '' }))).toEqual({
      carrera: 'Elige la carrera que coordina.',
    });
    expect(erroresEn(con({ tipo: 'G07_GESTOR_INVESTIGACION' }))).toEqual({
      unidad: 'Elige la unidad que dirige.',
    });
    expect(datosDe(con({ tipo: 'RECTOR', unidad: 'RECT' }))).not.toHaveProperty('carrera');
  });

  it('el fin de funciones va de un día después a exactamente dos años después', () => {
    expect(dosAniosDespues('2025-04-01')).toBe('2027-04-01');
    expect(erroresEn(con({ fechaFin: '2025-04-01' })).fechaFin).toBe(
      'El fin de funciones tiene que ser posterior a la designación.',
    );
    expect(erroresEn(con({ fechaFin: '2025-04-02' }))).toEqual({});
    expect(erroresEn(con({ fechaFin: '2027-04-01' }))).toEqual({});
    expect(erroresEn(con({ fechaFin: '2027-04-02' })).fechaFin).toBe(
      'Una designación dura hasta 2 años: el fin de funciones va hasta 2027-04-01.',
    );
  });

  it('el documento corta en 60 caracteres y no puede quedar en blanco', () => {
    expect(filtrar(undefined, 'R'.repeat(61), 60)).toHaveLength(60);
    expect(erroresEn(con({ documento: '   ' })).documento).toBe(
      'Escribe la resolución o la acción de personal.',
    );
  });
});
