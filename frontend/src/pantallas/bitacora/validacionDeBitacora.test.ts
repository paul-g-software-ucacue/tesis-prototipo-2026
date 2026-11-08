import { describe, expect, it } from 'vitest';
import {
  FILTROS_VACIOS,
  aParametros,
  deParametros,
  erroresDelFiltro,
  limpiarIdentificador,
  limpiarPeriodo,
  limpiarUsuario,
  type FiltrosDeBitacora,
} from './validacionDeBitacora';

const con = (cambios: Partial<FiltrosDeBitacora>): FiltrosDeBitacora => ({
  ...FILTROS_VACIOS,
  ...cambios,
});

describe('validación de la bitácora (principio XIV, contracts/spa.md)', () => {
  describe('mientras se escribe', () => {
    it('el identificador solo admite letras sin tilde, dígitos, punto, guion, guion bajo y barra', () => {
      expect(limpiarIdentificador('DOC/0102 tildé-ñ_1.x')).toBe('DOC/0102tild-_1.x');
      expect(limpiarIdentificador('140-b')).toBe('140-b');
    });

    it('el identificador se corta en 64 caracteres, también al pegar', () => {
      expect(limpiarIdentificador('a'.repeat(70))).toHaveLength(64);
    });

    it('el periodo solo admite letras, dígitos, punto, guion y guion bajo, hasta 20', () => {
      expect(limpiarPeriodo('2026 I/x')).toBe('2026Ix');
      expect(limpiarPeriodo('p'.repeat(25))).toHaveLength(20);
    });

    it('el usuario se corta en 64 caracteres', () => {
      expect(limpiarUsuario('u'.repeat(70))).toHaveLength(64);
      expect(limpiarUsuario("x' or 1")).toBe("x' or 1");
    });
  });

  describe('antes de enviar', () => {
    it('sin filtros no hay nada que corregir', () => {
      expect(erroresDelFiltro(FILTROS_VACIOS)).toEqual({});
    });

    it('hasta no puede ser anterior a desde', () => {
      expect(erroresDelFiltro(con({ desde: '2026-11-07', hasta: '2026-11-06' }))).toEqual({
        hasta: 'La fecha hasta no puede ser anterior a la fecha desde.',
      });
    });

    it('el mismo día en desde y hasta es válido', () => {
      expect(erroresDelFiltro(con({ desde: '2026-11-07', hasta: '2026-11-07' }))).toEqual({});
    });

    it('la acción necesita su servicio', () => {
      expect(erroresDelFiltro(con({ accion: 'ACTIVIDAD_GUARDADA' }))).toEqual({
        accion: 'Elige primero el servicio de la acción.',
      });
      expect(
        erroresDelFiltro(con({ servicio: 'generacion', accion: 'ACTIVIDAD_GUARDADA' })),
      ).toEqual({});
    });

    it('un identificador de 64 es válido y uno de 65 no', () => {
      expect(erroresDelFiltro(con({ identificador: 'a'.repeat(64) }))).toEqual({});
      expect(erroresDelFiltro(con({ identificador: 'a'.repeat(65) }))).toHaveProperty(
        'identificador',
      );
    });

    it('un identificador con espacio o tilde no es válido', () => {
      expect(erroresDelFiltro(con({ identificador: 'con espacio' }))).toHaveProperty(
        'identificador',
      );
      expect(erroresDelFiltro(con({ identificador: 'tildé' }))).toHaveProperty('identificador');
    });

    it('un periodo de 20 es válido y uno de 21 no', () => {
      expect(erroresDelFiltro(con({ periodo: 'p'.repeat(20) }))).toEqual({});
      expect(erroresDelFiltro(con({ periodo: 'p'.repeat(21) }))).toHaveProperty('periodo');
    });

    it('un usuario de 64 es válido y uno de 65 no', () => {
      expect(erroresDelFiltro(con({ usuario: 'u'.repeat(64) }))).toEqual({});
      expect(erroresDelFiltro(con({ usuario: 'u'.repeat(65) }))).toHaveProperty('usuario');
    });
  });

  describe('en la dirección', () => {
    it('solo lleva los filtros con valor, la página y el corte', () => {
      const parametros = aParametros(
        con({ usuario: ' coordinacion.demo ', desde: '2026-11-07' }),
        2,
        25,
        '2026-11-07T20:00:00Z',
      );

      expect(parametros.toString()).toBe(
        'desde=2026-11-07&usuario=coordinacion.demo&pagina=2&tamano=25&corte=2026-11-07T20%3A00%3A00Z',
      );
    });

    it('la primera página de 50 sin corte no lleva página, tamaño ni corte', () => {
      expect(aParametros(con({ periodo: '2026-I' }), 1, 50).toString()).toBe('periodo=2026-I');
    });

    it('se leen los filtros, la página y el corte, con sus valores por defecto', () => {
      const leida = deParametros(new URLSearchParams('usuario=sistema&pagina=3&corte=x'));

      expect(leida.filtros).toEqual(con({ usuario: 'sistema' }));
      expect(leida.pagina).toBe(3);
      expect(leida.tamano).toBe(50);
      expect(leida.corte).toBe('x');
    });

    it('una página o un tamaño que no son números quedan en su valor por defecto', () => {
      const leida = deParametros(new URLSearchParams('pagina=abc&tamano=7'));

      expect(leida.pagina).toBe(1);
      expect(leida.tamano).toBe(50);
    });
  });
});
