// El formulario de un proyecto aprobado (F03-H05, carga 9): sus valores como texto, el cuerpo que se
// envía y lo que revisa antes de enviar, con un motivo que dice qué hacer (principio XIV,
// aclaraciones 5 a 7). Repite las reglas del servidor para avisar a tiempo; el servidor decide si
// las carreras existen y si el director y los participantes son docentes registrados.
import type {
  DatosDeProyecto,
  EstadoDeProyecto,
  Proyecto,
  ProyectoNuevo,
  RolDeParticipante,
  TipoDeProyecto,
} from './consultas';

export const CODIGO = 30;
export const NOMBRE = 250;
export const RESOLUCION = 60;
export const HORAS = 5;

/** Un participante en el formulario. */
export interface FilaDeParticipante {
  docente: string;
  rol: string;
  horas: string;
}

/** Los valores del formulario, como los escribe quien lo llena. */
export interface ValoresDelProyecto {
  codigo: string;
  nombre: string;
  tipo: string;
  carreras: string[];
  resolucionOcs: string;
  fechaResolucion: string;
  estado: string;
  vigenciaInicio: string;
  vigenciaFin: string;
  director: string;
  participantes: FilaDeParticipante[];
  horasAprobadas: string;
}

export const PARTICIPANTE_VACIO: FilaDeParticipante = { docente: '', rol: '', horas: '' };

/** Un formulario vacío, para registrar: un proyecto nuevo suele llegar aprobado. */
export function valoresVacios(): ValoresDelProyecto {
  return {
    codigo: '',
    nombre: '',
    tipo: '',
    carreras: [''],
    resolucionOcs: '',
    fechaResolucion: '',
    estado: 'APROBADO',
    vigenciaInicio: '',
    vigenciaFin: '',
    director: '',
    participantes: [{ ...PARTICIPANTE_VACIO }],
    horasAprobadas: '',
  };
}

const conComa = (valor: number) => String(valor).replace('.', ',');

/** Los valores de un proyecto que ya existe, para corregirlo. */
export function valoresDe(proyecto: Proyecto): ValoresDelProyecto {
  return {
    codigo: proyecto.codigo,
    nombre: proyecto.nombre,
    tipo: proyecto.tipo,
    carreras: [...proyecto.carreras],
    resolucionOcs: proyecto.resolucionOcs,
    fechaResolucion: proyecto.fechaResolucion,
    estado: proyecto.estado,
    vigenciaInicio: proyecto.vigenciaInicio,
    vigenciaFin: proyecto.vigenciaFin,
    director: proyecto.director,
    participantes: proyecto.participantes.map((uno) => ({
      docente: uno.docente,
      rol: uno.rol,
      horas: conComa(uno.horas),
    })),
    horasAprobadas: conComa(proyecto.horasAprobadas),
  };
}

/** Unas horas escritas con coma o punto, como número; nada si están vacías. */
export function horasDe(texto: string): number | undefined {
  return texto.trim() ? Number(texto.trim().replace(',', '.')) : undefined;
}

/** Las horas sin ceros de más, como se cuentan sus caracteres: «2,50» es «2,5». */
function sinCerosDeMas(texto: string) {
  const limpio = texto.trim().replace('.', ',');
  return limpio.includes(',') ? limpio.replace(/0+$/, '').replace(/,$/, '') : limpio;
}

/** Por qué unas horas no cumplen la regla de la carga 9 (aclaración 7), o nada. */
export function motivoDeLasHoras(texto: string): string | undefined {
  if (!texto.trim()) {
    return 'Escribe las horas semanales.';
  }
  const valor = horasDe(texto);
  if (valor === undefined || Number.isNaN(valor) || valor <= 0) {
    return 'Las horas tienen que ser mayores que 0.';
  }
  const limpio = sinCerosDeMas(texto);
  if ((limpio.split(',')[1] ?? '').length > 2) {
    return 'Las horas admiten hasta dos decimales, como 2,5 o 2,75.';
  }
  if (limpio.length > HORAS || valor >= 1000) {
    return 'Las horas tienen hasta 5 caracteres, como 999,9 o 99,75.';
  }
  return undefined;
}

/** Los datos que se envían al corregir; al registrar, con el código. */
export function datosDe(valores: ValoresDelProyecto): DatosDeProyecto {
  return {
    nombre: valores.nombre.trim(),
    tipo: valores.tipo as TipoDeProyecto,
    carreras: valores.carreras.filter(Boolean),
    resolucionOcs: valores.resolucionOcs.trim(),
    fechaResolucion: valores.fechaResolucion,
    estado: valores.estado as EstadoDeProyecto,
    vigenciaInicio: valores.vigenciaInicio,
    vigenciaFin: valores.vigenciaFin,
    director: valores.director,
    participantes: valores.participantes.map((uno) => ({
      docente: uno.docente,
      rol: uno.rol as RolDeParticipante,
      horas: horasDe(uno.horas) ?? 0,
    })),
    horasAprobadas: horasDe(valores.horasAprobadas) ?? 0,
  };
}

/** El cuerpo de un proyecto nuevo. */
export function nuevoDe(valores: ValoresDelProyecto): ProyectoNuevo {
  return { codigo: valores.codigo, ...datosDe(valores) };
}

/** Lo que está mal en cada campo, por su nombre en la API; vacío si se puede enviar. */
export function erroresEn(valores: ValoresDelProyecto, nuevo: boolean): Record<string, string> {
  const errores: Record<string, string> = {};
  const anotar = (campo: string, motivo: string | undefined) => {
    if (motivo) {
      errores[campo] = motivo;
    }
  };
  if (nuevo) {
    anotar(
      'codigo',
      valores.codigo ? undefined : 'Escribe el código del proyecto, como INV-2025-01.',
    );
  }
  anotar('nombre', valores.nombre.trim() ? undefined : 'Escribe el nombre del proyecto.');
  anotar('tipo', valores.tipo ? undefined : 'Elige el tipo de proyecto.');
  if (!valores.carreras.length) {
    anotar('carreras', 'Agrega al menos una carrera.');
  }
  valores.carreras.forEach((carrera, posicion) => {
    anotar(
      `carreras[${String(posicion)}]`,
      !carrera
        ? 'Elige la carrera o quita la fila.'
        : valores.carreras.indexOf(carrera) < posicion
          ? 'Esta carrera ya está en la lista.'
          : undefined,
    );
  });
  anotar(
    'resolucionOcs',
    valores.resolucionOcs.trim() ? undefined : 'Escribe la resolución del OCS que lo aprueba.',
  );
  anotar(
    'fechaResolucion',
    valores.fechaResolucion ? undefined : 'Elige la fecha de la resolución.',
  );
  anotar('estado', valores.estado ? undefined : 'Elige el estado.');
  anotar('vigenciaInicio', valores.vigenciaInicio ? undefined : 'Elige el inicio de la vigencia.');
  anotar(
    'vigenciaFin',
    !valores.vigenciaFin
      ? 'Elige el fin de la vigencia.'
      : valores.vigenciaInicio && valores.vigenciaFin <= valores.vigenciaInicio
        ? 'El fin de la vigencia tiene que ser posterior a su inicio.'
        : undefined,
  );
  anotar('director', valores.director ? undefined : 'Elige el docente que dirige el proyecto.');
  anotar('horasAprobadas', motivoDeLasHoras(valores.horasAprobadas));
  if (!valores.participantes.length) {
    anotar('participantes', 'Agrega al menos un participante.');
  }
  const docentes = valores.participantes.map((uno) => uno.docente);
  valores.participantes.forEach((uno, posicion) => {
    const base = `participantes[${String(posicion)}]`;
    anotar(
      `${base}.docente`,
      !uno.docente
        ? 'Elige el docente o quita la fila.'
        : docentes.indexOf(uno.docente) < posicion
          ? 'Este docente ya está en la lista.'
          : undefined,
    );
    anotar(`${base}.rol`, uno.rol ? undefined : 'Elige el rol.');
    anotar(`${base}.horas`, motivoDeLasHoras(uno.horas));
  });
  const aprobadas = horasDe(valores.horasAprobadas);
  const todasValidas = valores.participantes.every((uno) => !motivoDeLasHoras(uno.horas));
  if (aprobadas !== undefined && !errores.horasAprobadas && todasValidas) {
    // En centésimas, para que 0,1 + 0,2 no dé 0,30000000000000004.
    const suma =
      valores.participantes.reduce(
        (total, uno) => total + Math.round((horasDe(uno.horas) ?? 0) * 100),
        0,
      ) / 100;
    if (suma > aprobadas) {
      anotar(
        'participantes',
        `Las horas de los participantes suman ${conComa(suma)} y pasan las ${conComa(aprobadas)} aprobadas.`,
      );
    }
  }
  return errores;
}
