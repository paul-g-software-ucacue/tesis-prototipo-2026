// El formulario de un docente (F03-H04): sus valores como texto, cómo salen de un docente para
// corregirlo, cómo se arma el cuerpo que se envía y qué falta antes de enviar. El servidor valida
// todo lo demás (la cédula, la categoría, la carrera base, los rangos de la ficha).
import { numero, queFalta } from '../comun/formulario';
import type {
  CategoriaDeEscalafon,
  CondicionEspecial,
  DatosDeDocente,
  Dedicacion,
  Docente,
  TipoDeIdentificacion,
  TipoDeTituloDeCuartoNivel,
  TipoPersonal,
} from './consultas';

/** Un título de cuarto nivel en el formulario. */
export interface FilaDeTitulo {
  titulo: string;
  tipo: string;
  campoEspecifico: string;
  anio: string;
}

/** Un curso en el formulario. */
export interface FilaDeCurso {
  nombre: string;
  campoEspecifico: string;
  horas: string;
  anio: string;
}

/** Una experiencia laboral en el formulario. */
export interface FilaDeExperiencia {
  cargo: string;
  institucion: string;
  campoEspecifico: string;
  anios: string;
}

/** Los valores del formulario, como los escribe quien lo llena. */
export interface ValoresDelDocente {
  tipoIdentificacion: string;
  identificacion: string;
  primerApellido: string;
  segundoApellido: string;
  primerNombre: string;
  segundoNombre: string;
  correoInstitucional: string;
  telefono: string;
  tipoPersonal: string;
  categoriaEscalafon: string;
  dedicacion: string;
  horasContratadas: string;
  fechaIngreso: string;
  inicioContrato: string;
  finContrato: string;
  condicionEspecial: string;
  carreraBase: string;
  titulo: string;
  registroSenescyt: string;
  campoAmplio: string;
  campoEspecifico: string;
  cuartoNivel: FilaDeTitulo[];
  cursos: FilaDeCurso[];
  experiencia: FilaDeExperiencia[];
  asignaturasAfines: string[];
}

export const TITULO_VACIO: FilaDeTitulo = { titulo: '', tipo: '', campoEspecifico: '', anio: '' };
export const CURSO_VACIO: FilaDeCurso = { nombre: '', campoEspecifico: '', horas: '', anio: '' };
export const EXPERIENCIA_VACIA: FilaDeExperiencia = {
  cargo: '',
  institucion: '',
  campoEspecifico: '',
  anios: '',
};

/** Un formulario vacío, para registrar: cédula y condición «Ninguna» por omisión. */
export function valoresVacios(): ValoresDelDocente {
  return {
    tipoIdentificacion: 'CEDULA',
    identificacion: '',
    primerApellido: '',
    segundoApellido: '',
    primerNombre: '',
    segundoNombre: '',
    correoInstitucional: '',
    telefono: '',
    tipoPersonal: '',
    categoriaEscalafon: '',
    dedicacion: '',
    horasContratadas: '',
    fechaIngreso: '',
    inicioContrato: '',
    finContrato: '',
    condicionEspecial: 'NINGUNA',
    carreraBase: '',
    titulo: '',
    registroSenescyt: '',
    campoAmplio: '',
    campoEspecifico: '',
    cuartoNivel: [],
    cursos: [],
    experiencia: [],
    asignaturasAfines: [],
  };
}

/** Los valores de un docente que ya existe, para corregirlo. */
export function valoresDe(docente: Docente): ValoresDelDocente {
  const ficha = docente.ficha;
  return {
    tipoIdentificacion: docente.tipoIdentificacion,
    identificacion: docente.identificacion,
    primerApellido: docente.primerApellido,
    segundoApellido: docente.segundoApellido ?? '',
    primerNombre: docente.primerNombre,
    segundoNombre: docente.segundoNombre ?? '',
    correoInstitucional: docente.correoInstitucional,
    telefono: docente.telefono ?? '',
    tipoPersonal: docente.tipoPersonal,
    categoriaEscalafon: docente.categoriaEscalafon,
    dedicacion: docente.dedicacion,
    horasContratadas: String(docente.horasContratadas),
    fechaIngreso: docente.fechaIngreso,
    inicioContrato: docente.inicioContrato,
    finContrato: docente.finContrato ?? '',
    condicionEspecial: docente.condicionEspecial,
    carreraBase: docente.carreraBase,
    titulo: ficha.tercerNivel.titulo,
    registroSenescyt: ficha.tercerNivel.registroSenescyt ?? '',
    campoAmplio: ficha.tercerNivel.campoAmplio,
    campoEspecifico: ficha.tercerNivel.campoEspecifico,
    cuartoNivel: (ficha.cuartoNivel ?? []).map((titulo) => ({
      titulo: titulo.titulo,
      tipo: titulo.tipo,
      campoEspecifico: titulo.campoEspecifico,
      anio: String(titulo.anio),
    })),
    cursos: (ficha.cursos ?? []).map((curso) => ({
      nombre: curso.nombre,
      campoEspecifico: curso.campoEspecifico,
      horas: String(curso.horas),
      anio: String(curso.anio),
    })),
    experiencia: (ficha.experiencia ?? []).map((una) => ({
      cargo: una.cargo,
      institucion: una.institucion,
      campoEspecifico: una.campoEspecifico,
      anios: String(una.anios),
    })),
    asignaturasAfines: [...(ficha.asignaturasAfines ?? [])],
  };
}

function opcional(texto: string) {
  return texto.trim() ? texto.trim() : undefined;
}

/** Los datos que se envían, al registrar y al corregir. */
export function cuerpoDe(valores: ValoresDelDocente): DatosDeDocente {
  return {
    primerApellido: valores.primerApellido,
    ...(opcional(valores.segundoApellido) && { segundoApellido: valores.segundoApellido.trim() }),
    primerNombre: valores.primerNombre,
    ...(opcional(valores.segundoNombre) && { segundoNombre: valores.segundoNombre.trim() }),
    correoInstitucional: valores.correoInstitucional.trim(),
    ...(opcional(valores.telefono) && { telefono: valores.telefono.trim() }),
    tipoPersonal: valores.tipoPersonal as TipoPersonal,
    categoriaEscalafon: valores.categoriaEscalafon as CategoriaDeEscalafon,
    dedicacion: valores.dedicacion as Dedicacion,
    horasContratadas: numero(valores.horasContratadas) ?? 0,
    fechaIngreso: valores.fechaIngreso,
    inicioContrato: valores.inicioContrato,
    ...(valores.finContrato && { finContrato: valores.finContrato }),
    condicionEspecial: valores.condicionEspecial as CondicionEspecial,
    carreraBase: valores.carreraBase,
    ficha: {
      tercerNivel: {
        titulo: valores.titulo,
        ...(opcional(valores.registroSenescyt) && {
          registroSenescyt: valores.registroSenescyt.trim(),
        }),
        campoAmplio: valores.campoAmplio,
        campoEspecifico: valores.campoEspecifico.trim(),
      },
      cuartoNivel: valores.cuartoNivel.map((fila) => ({
        titulo: fila.titulo,
        tipo: fila.tipo as TipoDeTituloDeCuartoNivel,
        campoEspecifico: fila.campoEspecifico.trim(),
        anio: numero(fila.anio) ?? 0,
      })),
      cursos: valores.cursos.map((fila) => ({
        nombre: fila.nombre,
        campoEspecifico: fila.campoEspecifico.trim(),
        horas: numero(fila.horas) ?? 0,
        anio: numero(fila.anio) ?? 0,
      })),
      experiencia: valores.experiencia.map((fila) => ({
        cargo: fila.cargo,
        institucion: fila.institucion,
        campoEspecifico: fila.campoEspecifico.trim(),
        anios: numero(fila.anios) ?? 0,
      })),
      asignaturasAfines: valores.asignaturasAfines.filter(Boolean),
    },
  };
}

/** El tipo de identificación elegido. */
export function tipoDeIdentificacion(valores: ValoresDelDocente) {
  return valores.tipoIdentificacion as TipoDeIdentificacion;
}

/** Lo que falta para enviar, por su nombre en pantalla, o nada. */
export function faltaEn(valores: ValoresDelDocente, nuevo: boolean) {
  const campos: [string, boolean][] = [];
  if (nuevo) {
    campos.push(['Identificación', Boolean(valores.identificacion.trim())]);
  }
  campos.push(
    ['Primer apellido', Boolean(valores.primerApellido.trim())],
    ['Primer nombre', Boolean(valores.primerNombre.trim())],
    ['Correo institucional', Boolean(valores.correoInstitucional.trim())],
    ['Tipo de personal', Boolean(valores.tipoPersonal)],
    ['Categoría de escalafón', Boolean(valores.categoriaEscalafon)],
    ['Dedicación', Boolean(valores.dedicacion)],
    ['Horas contratadas', Boolean(valores.horasContratadas.trim())],
    ['Fecha de ingreso', Boolean(valores.fechaIngreso)],
    ['Inicio del contrato', Boolean(valores.inicioContrato)],
  );
  if (valores.tipoPersonal === 'OCASIONAL') {
    campos.push(['Fin del contrato', Boolean(valores.finContrato)]);
  }
  campos.push(
    ['Carrera base', Boolean(valores.carreraBase)],
    ['Título de tercer nivel', Boolean(valores.titulo.trim())],
    ['Campo amplio', Boolean(valores.campoAmplio)],
    ['Campo específico', Boolean(valores.campoEspecifico.trim())],
  );
  return queFalta(campos);
}

/** Los campos que el formulario muestra junto a su control, con la posición en las listas. */
export function camposDelFormulario(valores: ValoresDelDocente): string[] {
  const deLista = (lista: string, cantidad: number, campos: string[]) =>
    Array.from({ length: cantidad }, (_, posicion) =>
      campos.map((campo) => `ficha.${lista}[${String(posicion)}].${campo}`),
    ).flat();
  return [
    'tipoIdentificacion',
    'identificacion',
    'primerApellido',
    'segundoApellido',
    'primerNombre',
    'segundoNombre',
    'correoInstitucional',
    'telefono',
    'tipoPersonal',
    'categoriaEscalafon',
    'dedicacion',
    'horasContratadas',
    'fechaIngreso',
    'inicioContrato',
    'finContrato',
    'condicionEspecial',
    'carreraBase',
    'ficha.tercerNivel.titulo',
    'ficha.tercerNivel.registroSenescyt',
    'ficha.tercerNivel.campoAmplio',
    'ficha.tercerNivel.campoEspecifico',
    ...deLista('cuartoNivel', valores.cuartoNivel.length, [
      'titulo',
      'tipo',
      'campoEspecifico',
      'anio',
    ]),
    ...deLista('cursos', valores.cursos.length, ['nombre', 'campoEspecifico', 'horas', 'anio']),
    ...deLista('experiencia', valores.experiencia.length, [
      'cargo',
      'institucion',
      'campoEspecifico',
      'anios',
    ]),
    ...valores.asignaturasAfines.map(
      (_, posicion) => `ficha.asignaturasAfines[${String(posicion)}]`,
    ),
  ];
}
