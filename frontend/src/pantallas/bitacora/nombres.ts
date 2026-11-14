// Los textos de la pantalla «Bitácora» (F17-H02): los nombres de los servicios, el momento en la
// hora de Ecuador continental y los valores de una entrada como pares campo–valor, nunca como JSON.
import type { components } from '../../api/generado/usuarios';

export type ServicioDeBitacora = components['schemas']['ServicioDeBitacora'];

/** Los siete servicios que registran en la bitácora (D-90; Firma desde F13-H02), en el orden del contrato. */
export const NOMBRE_DEL_SERVICIO: Record<ServicioDeBitacora, string> = {
  generacion: 'Generación',
  academico: 'Académico',
  docentes: 'Docentes',
  normativa: 'Normativa',
  roles: 'Roles',
  validacionocs: 'Validación OCS',
  firma: 'Firma',
};

// La zona se fija: un auditor con otra en su navegador ve la misma hora (research §3). Con 24
// horas, sin «p. m.», para comparar momentos sin ambigüedad.
const MOMENTO = new Intl.DateTimeFormat('es-EC', {
  dateStyle: 'short',
  timeStyle: 'medium',
  hourCycle: 'h23',
  timeZone: 'America/Guayaquil',
});

export function formatearMomento(instante: string) {
  return MOMENTO.format(new Date(instante));
}

/** «1 entrada», «3 entradas». */
export function entradas(cantidad: number) {
  return cantidad === 1 ? '1 entrada' : `${String(cantidad)} entradas`;
}

/** Una línea de los valores, con su sangría. */
export interface Linea {
  nivel: number;
  texto: string;
}

function simple(valor: unknown): string | undefined {
  if (valor === null || valor === undefined) {
    return 'sin valor';
  }
  if (typeof valor === 'boolean') {
    return valor ? 'sí' : 'no';
  }
  if (typeof valor === 'string' || typeof valor === 'number') {
    return String(valor);
  }
  if (Array.isArray(valor) && valor.every((uno) => simple(uno) !== undefined)) {
    return valor.map((uno) => simple(uno)).join(', ');
  }
  return undefined;
}

/** El objeto en una línea: sus valores simples, y «…» para lo anidado. */
export function resumen(valores: Record<string, unknown>) {
  return Object.entries(valores)
    .map(([campo, valor]) => `${campo}: ${simple(valor) ?? '…'}`)
    .join(' · ');
}

/** Cada campo en su línea; lo anidado, debajo y con un nivel más de sangría. */
export function lineas(valores: Record<string, unknown>, nivel = 0): Linea[] {
  return Object.entries(valores).flatMap(([campo, valor]) => {
    const texto = simple(valor);
    if (texto !== undefined) {
      return [{ nivel, texto: `${campo}: ${texto}` }];
    }
    const hijos = Array.isArray(valor) ? valor : [valor];
    return [
      { nivel, texto: `${campo}:` },
      ...hijos.flatMap((hijo) =>
        typeof hijo === 'object' && hijo !== null
          ? lineas(hijo as Record<string, unknown>, nivel + 1)
          : [{ nivel: nivel + 1, texto: simple(hijo) ?? '' }],
      ),
    ];
  });
}
