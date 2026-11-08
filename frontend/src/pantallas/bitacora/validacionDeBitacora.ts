// Lo que la pantalla «Bitácora» revisa en los filtros (F17-H02, FR-010, principio XIV): qué admite
// cada campo mientras se escribe y, antes de consultar, qué está mal en cada uno. Son las reglas de
// contratos/usuarios (consultarBitacora); el servidor sigue siendo quien decide. También pasa los
// filtros, la página y el corte a la dirección y de vuelta, para recargar o compartir la consulta.

/** Los filtros tal como se escriben; uno vacío no filtra. */
export interface FiltrosDeBitacora {
  desde: string;
  hasta: string;
  usuario: string;
  servicio: string;
  accion: string;
  identificador: string;
  periodo: string;
}

export const FILTROS_VACIOS: FiltrosDeBitacora = {
  desde: '',
  hasta: '',
  usuario: '',
  servicio: '',
  accion: '',
  identificador: '',
  periodo: '',
};

/** Las entradas por página que se pueden elegir; 50 por defecto (contracts/spa.md). */
export const TAMANOS = [25, 50, 100] as const;
export const TAMANO_POR_DEFECTO = 50;

const USUARIO = 64;
const IDENTIFICADOR = /^[0-9A-Za-z._/-]{1,64}$/;
const PERIODO = /^[A-Za-z0-9._-]{1,20}$/;
const CAMPOS = [
  'desde',
  'hasta',
  'usuario',
  'servicio',
  'accion',
  'identificador',
  'periodo',
] as const;

/** Letras sin tilde, dígitos, punto, guion, guion bajo y barra, hasta 64. */
export function limpiarIdentificador(texto: string) {
  return texto.replace(/[^0-9A-Za-z._/-]/g, '').slice(0, 64);
}

/** Letras sin tilde, dígitos, punto, guion y guion bajo, hasta 20. */
export function limpiarPeriodo(texto: string) {
  return texto.replace(/[^0-9A-Za-z._-]/g, '').slice(0, 20);
}

/** Hasta 64 caracteres, sin otro formato (como el nombre de usuario). */
export function limpiarUsuario(texto: string) {
  return texto.slice(0, USUARIO);
}

/** Lo que está mal en cada filtro, por su nombre en la API; vacío si se puede consultar. */
export function erroresDelFiltro(filtros: FiltrosDeBitacora): Record<string, string> {
  const errores: Record<string, string> = {};
  if (filtros.desde && filtros.hasta && filtros.hasta < filtros.desde) {
    errores.hasta = 'La fecha hasta no puede ser anterior a la fecha desde.';
  }
  if (filtros.usuario.trim().length > USUARIO) {
    errores.usuario = 'El usuario tiene hasta 64 caracteres.';
  }
  if (filtros.accion && !filtros.servicio) {
    errores.accion = 'Elige primero el servicio de la acción.';
  }
  if (filtros.identificador && !IDENTIFICADOR.test(filtros.identificador)) {
    errores.identificador =
      'Usa de 1 a 64 letras sin tilde, dígitos, punto, guion, guion bajo o barra.';
  }
  if (filtros.periodo && !PERIODO.test(filtros.periodo)) {
    errores.periodo = 'Usa de 1 a 20 letras sin tilde, dígitos, punto, guion o guion bajo.';
  }
  return errores;
}

/** Los filtros con valor, y la página, el tamaño y el corte solo si no son los de por defecto. */
export function aParametros(
  filtros: FiltrosDeBitacora,
  pagina: number,
  tamano: number,
  corte?: string,
) {
  const parametros = new URLSearchParams();
  for (const campo of CAMPOS) {
    const valor = filtros[campo].trim();
    if (valor) {
      parametros.set(campo, valor);
    }
  }
  if (pagina !== 1) {
    parametros.set('pagina', String(pagina));
  }
  if (tamano !== TAMANO_POR_DEFECTO) {
    parametros.set('tamano', String(tamano));
  }
  if (corte) {
    parametros.set('corte', corte);
  }
  return parametros;
}

/** Lo que dice la dirección; una página o un tamaño que no se pueden usar quedan por defecto. */
export function deParametros(parametros: URLSearchParams) {
  const filtros = { ...FILTROS_VACIOS };
  for (const campo of CAMPOS) {
    filtros[campo] = parametros.get(campo) ?? '';
  }
  const pagina = Number(parametros.get('pagina'));
  const tamano = Number(parametros.get('tamano'));
  const corte = parametros.get('corte') ?? undefined;
  return {
    filtros,
    pagina: Number.isInteger(pagina) && pagina >= 1 ? pagina : 1,
    tamano: (TAMANOS as readonly number[]).includes(tamano) ? tamano : TAMANO_POR_DEFECTO,
    ...(corte ? { corte } : {}),
  };
}
