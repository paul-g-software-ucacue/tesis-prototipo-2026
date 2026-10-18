// Los permisos de la sesión (claim permisos, ADR-021) en la forma FUNCIONALIDAD:TIPO o
// FUNCIONALIDAD:TIPO:PROPIO. Solo deciden qué muestra la SPA: el servidor valida cada operación
// (RNF-SEG-03).
import { FUNCIONALIDADES, TIPOS_DE_PERMISO, type TipoDePermiso } from './funcionalidades';

/** Un tipo de permiso que el usuario tiene sobre una funcionalidad. */
export interface TipoDelUsuario {
  tipo: TipoDePermiso;
  nombre: string;
  /** Si solo vale para lo suyo: su carrera, unidad o persona (D-64). */
  propio: boolean;
}

/** Una funcionalidad que el usuario puede usar, con sus tipos de permiso en orden. */
export interface FuncionDelUsuario {
  codigo: string;
  nombre: string;
  tipos: TipoDelUsuario[];
}

function partes(permiso: string) {
  const [funcionalidad, tipo, alcance] = permiso.split(':');
  return { funcionalidad, tipo, propio: alcance === 'PROPIO' };
}

/** Si el usuario tiene ese permiso, con cualquier alcance. */
export function tienePermiso(
  permisos: readonly string[] | undefined,
  funcionalidad: string,
  tipo: TipoDePermiso,
): boolean {
  return (permisos ?? []).some((permiso) => {
    const leido = partes(permiso);
    return leido.funcionalidad === funcionalidad && leido.tipo === tipo;
  });
}

/**
 * Las funcionalidades que el usuario puede usar, sin repetir, en el orden del catálogo; las que la
 * SPA no conoce van al final, con su código.
 */
export function funcionesDelUsuario(permisos: readonly string[] | undefined): FuncionDelUsuario[] {
  const porCodigo = new Map<string, TipoDelUsuario[]>();
  for (const permiso of permisos ?? []) {
    const { funcionalidad, tipo, propio } = partes(permiso);
    const nombre = TIPOS_DE_PERMISO.find((candidato) => candidato.tipo === tipo);
    if (!funcionalidad || !nombre) {
      continue;
    }
    const tipos = porCodigo.get(funcionalidad) ?? [];
    if (!tipos.some((existente) => existente.tipo === nombre.tipo)) {
      tipos.push({ tipo: nombre.tipo, nombre: nombre.nombre, propio });
    }
    porCodigo.set(funcionalidad, tipos);
  }
  const orden = (tipo: TipoDePermiso) =>
    TIPOS_DE_PERMISO.findIndex((candidato) => candidato.tipo === tipo);
  const conocidas = FUNCIONALIDADES.map(({ codigo }) => codigo);
  return [...porCodigo.entries()]
    .sort(([a], [b]) => posicion(conocidas, a) - posicion(conocidas, b))
    .map(([codigo, tipos]) => ({
      codigo,
      nombre: FUNCIONALIDADES.find((candidata) => candidata.codigo === codigo)?.nombre ?? codigo,
      tipos: [...tipos].sort((a, b) => orden(a.tipo) - orden(b.tipo)),
    }));
}

function posicion(conocidas: string[], codigo: string): number {
  const indice = conocidas.indexOf(codigo);
  return indice === -1 ? conocidas.length : indice;
}
