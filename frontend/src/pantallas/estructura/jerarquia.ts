// La estructura en su jerarquía: la API la devuelve plana, cada unidad con el código de su padre.
import type { TipoDeUnidad, UnidadOrganica } from './consultas';

/** Los tipos de unidad en español, en el orden en que se ofrecen. */
export const NOMBRE_DEL_TIPO: Record<TipoDeUnidad, string> = {
  SEDE_MATRIZ: 'Sede matriz',
  SEDE: 'Sede',
  EXTENSION: 'Extensión',
  CAMPUS: 'Campus',
  GOBIERNO: 'Gobierno',
  UNIDAD_ACADEMICA: 'Unidad académica',
  GESTION: 'Gestión',
  COMISION: 'Comisión',
};

/** Una unidad con su nivel en el árbol: la raíz es el 0. */
export interface UnidadEnArbol {
  unidad: UnidadOrganica;
  nivel: number;
}

/**
 * Las unidades en orden de árbol: cada una seguida de las que dependen de ella, por código. Una
 * unidad cuyo padre no está en la lista queda como raíz, para no perderla.
 */
export function enArbol(unidades: readonly UnidadOrganica[]): UnidadEnArbol[] {
  const codigos = new Set(unidades.map((unidad) => unidad.codigo));
  const hijas = new Map<string | undefined, UnidadOrganica[]>();
  for (const unidad of unidades) {
    const padre = unidad.padre && codigos.has(unidad.padre) ? unidad.padre : undefined;
    hijas.set(padre, [...(hijas.get(padre) ?? []), unidad]);
  }
  const resultado: UnidadEnArbol[] = [];
  const visitar = (padre: string | undefined, nivel: number) => {
    const deEsta = [...(hijas.get(padre) ?? [])].sort((a, b) => a.codigo.localeCompare(b.codigo));
    for (const unidad of deEsta) {
      resultado.push({ unidad, nivel });
      visitar(unidad.codigo, nivel + 1);
    }
  };
  visitar(undefined, 0);
  return resultado;
}

/** La unidad y todas las que dependen de ella, directa o indirectamente. */
export function conSusDependientes(
  unidades: readonly UnidadOrganica[],
  codigo: string,
): Set<string> {
  const resultado = new Set([codigo]);
  let creció = true;
  while (creció) {
    creció = false;
    for (const unidad of unidades) {
      if (unidad.padre && resultado.has(unidad.padre) && !resultado.has(unidad.codigo)) {
        resultado.add(unidad.codigo);
        creció = true;
      }
    }
  }
  return resultado;
}
