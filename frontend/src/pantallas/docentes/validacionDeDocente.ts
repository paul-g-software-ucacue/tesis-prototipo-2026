// Lo que el formulario del docente revisa antes de enviar (F03-H04): qué admite cada campo mientras
// se escribe y, al registrar o guardar, qué está mal en cada uno, con un mensaje para quien lo llena.
// Repite las reglas del servidor para avisar a tiempo y de una vez; el servidor sigue siendo quien
// decide (la carrera base y las asignaturas afines solo las comprueba él contra Académico).
import type { ValoresDelDocente } from './formularioDeDocente';

/** El primer año que admite la ficha (como el servidor). */
export const PRIMER_ANIO = 1950;

const PROVINCIAS = 24;
const EXTERIOR = 30;
const CORREO = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
const ESPECIFICO = 'el código del campo específico, de 3 dígitos (por ejemplo, 061)';

/** Por qué la cédula no es válida, o nada. Nunca repite el número (principio IX). */
function motivoDeLaCedula(numero: string) {
  if (!numero) {
    return 'Escribe el número de cédula.';
  }
  if (numero.length !== 10) {
    return `La cédula tiene 10 dígitos; escribiste ${String(numero.length)}.`;
  }
  const provincia = Number(numero.slice(0, 2));
  if ((provincia < 1 || provincia > PROVINCIAS) && provincia !== EXTERIOR) {
    return 'Los dos primeros dígitos de la cédula son la provincia: de 01 a 24, o 30.';
  }
  if (Number(numero[2]) >= 6) {
    return 'El tercer dígito de la cédula va de 0 a 5.';
  }
  const suma = [...numero.slice(0, 9)].reduce((total, caracter, posicion) => {
    const producto = Number(caracter) * (posicion % 2 === 0 ? 2 : 1);
    return total + (producto > 9 ? producto - 9 : producto);
  }, 0);
  if ((10 - (suma % 10)) % 10 !== Number(numero[9])) {
    return 'Esta cédula no es válida: el último dígito no corresponde a los demás. Revisa que esté bien escrita.';
  }
  return undefined;
}

/** Un año de la ficha: de 1950 al año en curso. */
function motivoDelAnio(anio: string, actual: number) {
  if (!anio) {
    return 'Escribe el año.';
  }
  const valor = Number(anio);
  return valor < PRIMER_ANIO || valor > actual
    ? `El año va de ${String(PRIMER_ANIO)} a ${String(actual)}.`
    : undefined;
}

/** Un campo específico de una lista: tres dígitos que empiezan por un campo amplio conocido. */
function motivoDelEspecifico(codigo: string, amplios: ReadonlySet<string>) {
  if (codigo.length !== 3) {
    return `Escribe ${ESPECIFICO}.`;
  }
  if (amplios.size && !amplios.has(codigo.slice(0, 2))) {
    return 'Los dos primeros dígitos tienen que ser un campo amplio de la lista (por ejemplo, 06).';
  }
  return undefined;
}

/**
 * Lo que está mal en cada campo, por su nombre en la API (el mismo de los errores del servidor).
 * Vacío si se puede enviar.
 */
export function erroresEn(
  valores: ValoresDelDocente,
  nuevo: boolean,
  camposAmplios: ReadonlySet<string>,
  hoy = new Date(),
): Record<string, string> {
  const errores: Record<string, string> = {};
  const anotar = (campo: string, motivo: string | undefined) => {
    if (motivo) {
      errores[campo] = motivo;
    }
  };
  const actual = hoy.getFullYear();

  if (nuevo) {
    const numero = valores.identificacion.trim();
    anotar(
      'identificacion',
      valores.tipoIdentificacion === 'CEDULA'
        ? motivoDeLaCedula(numero)
        : numero
          ? undefined
          : 'Escribe el número de pasaporte.',
    );
  }
  for (const [campo, nombre, obligatorio] of [
    ['primerApellido', 'el primer apellido', true],
    ['segundoApellido', 'el segundo apellido', false],
    ['primerNombre', 'el primer nombre', true],
    ['segundoNombre', 'el segundo nombre', false],
  ] as const) {
    const texto = valores[campo].trim();
    if (!texto) {
      anotar(campo, obligatorio ? `Escribe ${nombre}.` : undefined);
    } else if (!/^\p{L}/u.test(texto)) {
      anotar(campo, 'Empieza por una letra.');
    }
  }
  const correo = valores.correoInstitucional.trim();
  anotar(
    'correoInstitucional',
    !correo
      ? 'Escribe el correo institucional.'
      : CORREO.test(correo)
        ? undefined
        : 'Escribe un correo completo, como nombre.apellido@instituto.edu.ec.',
  );
  const telefono = valores.telefono.trim();
  if (telefono && (telefono.length < 7 || telefono.length > 10)) {
    anotar(
      'telefono',
      `El teléfono tiene de 7 a 10 dígitos; escribiste ${String(telefono.length)}.`,
    );
  }

  anotar('tipoPersonal', valores.tipoPersonal ? undefined : 'Elige el tipo de personal.');
  anotar(
    'categoriaEscalafon',
    valores.categoriaEscalafon ? undefined : 'Elige la categoría de escalafón del titular.',
  );
  anotar('dedicacion', valores.dedicacion ? undefined : 'Elige la dedicación.');
  const horas = valores.horasContratadas.trim();
  anotar(
    'horasContratadas',
    !horas
      ? 'Escribe las horas contratadas por semana.'
      : Number(horas) < 1 || Number(horas) > 40
        ? 'Las horas contratadas van de 1 a 40 por semana.'
        : undefined,
  );
  anotar('fechaIngreso', valores.fechaIngreso ? undefined : 'Elige la fecha de ingreso.');
  anotar(
    'inicioContrato',
    valores.inicioContrato ? undefined : 'Elige la fecha de inicio del contrato.',
  );
  if (!valores.finContrato) {
    anotar(
      'finContrato',
      valores.tipoPersonal === 'OCASIONAL'
        ? 'Un docente ocasional necesita la fecha de fin de su contrato.'
        : undefined,
    );
  } else if (valores.inicioContrato && valores.finContrato <= valores.inicioContrato) {
    anotar('finContrato', 'El fin del contrato tiene que ser posterior a su inicio.');
  }
  anotar('carreraBase', valores.carreraBase ? undefined : 'Elige la carrera base.');

  const tercer = 'ficha.tercerNivel';
  anotar(
    `${tercer}.titulo`,
    valores.titulo.trim() ? undefined : 'Escribe el título de tercer nivel.',
  );
  anotar(`${tercer}.campoAmplio`, valores.campoAmplio ? undefined : 'Elige el campo amplio.');
  const especifico = valores.campoEspecifico.trim();
  anotar(
    `${tercer}.campoEspecifico`,
    especifico.length !== 3
      ? `Escribe ${ESPECIFICO}.`
      : valores.campoAmplio && !especifico.startsWith(valores.campoAmplio)
        ? `Tiene que empezar por ${valores.campoAmplio}, el campo amplio elegido.`
        : undefined,
  );

  valores.cuartoNivel.forEach((fila, posicion) => {
    const base = `ficha.cuartoNivel[${String(posicion)}]`;
    anotar(`${base}.titulo`, fila.titulo.trim() ? undefined : 'Escribe el título.');
    anotar(`${base}.tipo`, fila.tipo ? undefined : 'Elige el tipo.');
    anotar(
      `${base}.campoEspecifico`,
      motivoDelEspecifico(fila.campoEspecifico.trim(), camposAmplios),
    );
    anotar(`${base}.anio`, motivoDelAnio(fila.anio.trim(), actual));
  });
  valores.cursos.forEach((fila, posicion) => {
    const base = `ficha.cursos[${String(posicion)}]`;
    anotar(`${base}.nombre`, fila.nombre.trim() ? undefined : 'Escribe el nombre del curso.');
    anotar(
      `${base}.campoEspecifico`,
      motivoDelEspecifico(fila.campoEspecifico.trim(), camposAmplios),
    );
    const horasDelCurso = fila.horas.trim();
    anotar(
      `${base}.horas`,
      !horasDelCurso
        ? 'Escribe las horas.'
        : Number(horasDelCurso) < 1 || Number(horasDelCurso) > 2000
          ? 'Las horas van de 1 a 2000.'
          : undefined,
    );
    anotar(`${base}.anio`, motivoDelAnio(fila.anio.trim(), actual));
  });
  valores.experiencia.forEach((fila, posicion) => {
    const base = `ficha.experiencia[${String(posicion)}]`;
    anotar(`${base}.cargo`, fila.cargo.trim() ? undefined : 'Escribe el cargo.');
    anotar(`${base}.institucion`, fila.institucion.trim() ? undefined : 'Escribe la institución.');
    anotar(
      `${base}.campoEspecifico`,
      motivoDelEspecifico(fila.campoEspecifico.trim(), camposAmplios),
    );
    const anios = Number(fila.anios.trim().replace(',', '.'));
    anotar(
      `${base}.anios`,
      !fila.anios.trim()
        ? 'Escribe los años.'
        : !(anios > 0 && anios <= 60)
          ? 'Los años van de más de 0 a 60; admite decimales, como 2,5.'
          : undefined,
    );
  });
  valores.asignaturasAfines.forEach((codigo, posicion) => {
    const campo = `ficha.asignaturasAfines[${String(posicion)}]`;
    anotar(
      campo,
      !codigo
        ? 'Elige la asignatura o quita la fila.'
        : valores.asignaturasAfines.indexOf(codigo) < posicion
          ? 'Esta asignatura ya está en la lista.'
          : undefined,
    );
  });
  return errores;
}
