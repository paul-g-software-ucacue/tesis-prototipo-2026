// El formulario de un cargo (F03-H05, carga 8): sus valores como texto, el cuerpo que se envía y lo
// que revisa antes de enviar, con un motivo que dice qué hacer (principio XIV). Repite las reglas
// del servidor para avisar a tiempo; el servidor decide si el docente está activo a tiempo completo
// y si la carrera ya tiene un coordinador vigente.
import type { Cargo, CargoNuevo, DatosDeCargo, EstadoDeCargo, TipoDeCargo } from './consultas';

/** El largo máximo del documento de designación. */
export const DOCUMENTO = 60;

/** Los valores del formulario, como los escribe quien lo llena. */
export interface ValoresDelCargo {
  docente: string;
  tipo: string;
  fechaDesignacion: string;
  unidad: string;
  carrera: string;
  fechaFin: string;
  documento: string;
  estado: EstadoDeCargo;
}

/** Un formulario vacío, para registrar: un cargo nuevo queda vigente. */
export function valoresVacios(): ValoresDelCargo {
  return {
    docente: '',
    tipo: '',
    fechaDesignacion: '',
    unidad: '',
    carrera: '',
    fechaFin: '',
    documento: '',
    estado: 'VIGENTE',
  };
}

/** Los valores de un cargo que ya existe, para corregirlo o finalizarlo. */
export function valoresDe(cargo: Cargo): ValoresDelCargo {
  return {
    docente: cargo.docente,
    tipo: cargo.tipo,
    fechaDesignacion: cargo.fechaDesignacion,
    unidad: cargo.unidad ?? '',
    carrera: cargo.carrera ?? '',
    fechaFin: cargo.fechaFin ?? '',
    documento: cargo.documento,
    estado: cargo.estado,
  };
}

/** Si el cargo es el de un coordinador de carrera: lleva carrera y no unidad. */
export function coordinaCarrera(tipo: string) {
  return tipo === 'COORDINADOR_CARRERA';
}

/** Los datos que se envían al corregir; al registrar, con la clave. */
export function datosDe(valores: ValoresDelCargo): DatosDeCargo {
  const carrera = coordinaCarrera(valores.tipo);
  return {
    ...(!carrera && valores.unidad && { unidad: valores.unidad }),
    ...(carrera && valores.carrera && { carrera: valores.carrera }),
    ...(valores.fechaFin && { fechaFin: valores.fechaFin }),
    documento: valores.documento.trim(),
    estado: valores.estado,
  };
}

/** El cuerpo de un cargo nuevo. */
export function nuevoDe(valores: ValoresDelCargo): CargoNuevo {
  return {
    docente: valores.docente,
    tipo: valores.tipo as TipoDeCargo,
    fechaDesignacion: valores.fechaDesignacion,
    ...datosDe(valores),
  };
}

/** La fecha dos años después, como aaaa-mm-dd: el fin de funciones va hasta ella (POL 5.4). */
export function dosAniosDespues(fecha: string) {
  const [anio = '', resto = ''] = [fecha.slice(0, 4), fecha.slice(4)];
  return `${String(Number(anio) + 2)}${resto}`;
}

/** Lo que está mal en cada campo, por su nombre en la API; vacío si se puede enviar. */
export function erroresEn(valores: ValoresDelCargo): Record<string, string> {
  const errores: Record<string, string> = {};
  const anotar = (campo: string, motivo: string | undefined) => {
    if (motivo) {
      errores[campo] = motivo;
    }
  };
  anotar('docente', valores.docente ? undefined : 'Elige el docente.');
  anotar('tipo', valores.tipo ? undefined : 'Elige el cargo.');
  anotar(
    'fechaDesignacion',
    valores.fechaDesignacion ? undefined : 'Elige la fecha de designación.',
  );
  if (coordinaCarrera(valores.tipo)) {
    anotar('carrera', valores.carrera ? undefined : 'Elige la carrera que coordina.');
  } else if (valores.tipo) {
    anotar('unidad', valores.unidad ? undefined : 'Elige la unidad que dirige.');
  }
  if (valores.fechaFin && valores.fechaDesignacion) {
    const limite = dosAniosDespues(valores.fechaDesignacion);
    anotar(
      'fechaFin',
      valores.fechaFin <= valores.fechaDesignacion
        ? 'El fin de funciones tiene que ser posterior a la designación.'
        : valores.fechaFin > limite
          ? `Una designación dura hasta 2 años: el fin de funciones va hasta ${limite}.`
          : undefined,
    );
  }
  anotar(
    'documento',
    valores.documento.trim() ? undefined : 'Escribe la resolución o la acción de personal.',
  );
  return errores;
}
