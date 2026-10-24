// Los diálogos de «Oferta del periodo» con escritura (F03-H03): ofertar una asignatura en un
// paralelo y corregir una asignatura-paralelo. Antes de enviar solo se dice qué falta; el servidor
// valida todo (RN-18, la jornada de la carrera, la sede) y aquí se muestra lo que responde, junto a
// su campo (ADR-019).
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import type { Carrera, Jornada, Modalidad } from '../carreras/consultas';
import { useAsignaturas } from '../carreras/consultas';
import { NOMBRE_DE_LA_JORNADA, NOMBRE_DE_LA_MODALIDAD, horas } from '../carreras/nombres';
import { Avisos, Marco } from '../comun/Formulario';
import { SELECCION, TIPOS_DE_SEDE, ayuda, detalleDe, numero, queFalta } from '../comun/formulario';
import { useEnvio } from '../comun/useEnvio';
import type { UnidadOrganica } from '../estructura/consultas';
import {
  corregirAsignaturaParalelo,
  ofertarAsignatura,
  type AsignaturaParalelo,
} from './consultas';

const CAMPOS = [
  'asignatura',
  'paralelo',
  'jornada',
  'modalidad',
  'sede',
  'estudiantes',
  'horasSemanales',
];

function pistaDeHoras(horasDeLaAsignatura: number | undefined) {
  return horasDeLaAsignatura === undefined
    ? 'Vacío: las de la asignatura; si las indicas, no pueden pasarlas.'
    : `Vacío: las de la asignatura, ${horas(horasDeLaAsignatura)} h; si las indicas, no pueden pasarlas.`;
}

/** Jornada, modalidad, sede, estudiantes y horas: lo que se registra y se corrige. */
function DatosDelParalelo({
  carrera,
  unidades,
  jornada,
  modalidad,
  sede,
  estudiantes,
  horasIndicadas,
  horasDeLaAsignatura,
  aviso,
  cambiar,
}: {
  carrera: Carrera | undefined;
  unidades: readonly UnidadOrganica[];
  jornada: string;
  modalidad: string;
  sede: string;
  estudiantes: string;
  horasIndicadas: string;
  horasDeLaAsignatura: number | undefined;
  aviso: ReturnType<typeof useEnvio>['aviso'];
  cambiar: (
    campo: 'jornada' | 'modalidad' | 'sede' | 'estudiantes' | 'horas',
    valor: string,
  ) => void;
}) {
  const sedes = unidades
    .filter((unidad) => TIPOS_DE_SEDE.has(unidad.tipo) && (unidad.activa || unidad.codigo === sede))
    .sort((a, b) => a.codigo.localeCompare(b.codigo));
  return (
    <>
      <TextField
        select
        label="Jornada"
        value={jornada}
        onChange={(evento) => cambiar('jornada', evento.target.value)}
        error={Boolean(detalleDe(aviso, 'jornada'))}
        helperText={ayuda(aviso, 'jornada', 'Una de las jornadas de la carrera.')}
        slotProps={SELECCION}
      >
        <option value="">Elige una</option>
        {(carrera?.jornadas ?? []).map((valor) => (
          <option key={valor} value={valor}>
            {NOMBRE_DE_LA_JORNADA[valor]}
          </option>
        ))}
      </TextField>
      <TextField
        select
        label="Modalidad"
        value={modalidad}
        onChange={(evento) => cambiar('modalidad', evento.target.value)}
        error={Boolean(detalleDe(aviso, 'modalidad'))}
        helperText={ayuda(aviso, 'modalidad', 'Por omisión, la de la carrera.')}
        slotProps={SELECCION}
      >
        <option value="">Elige una</option>
        {Object.entries(NOMBRE_DE_LA_MODALIDAD).map(([valor, texto]) => (
          <option key={valor} value={valor}>
            {texto}
          </option>
        ))}
      </TextField>
      <TextField
        select
        label="Sede"
        value={sede}
        onChange={(evento) => cambiar('sede', evento.target.value)}
        error={Boolean(detalleDe(aviso, 'sede'))}
        helperText={ayuda(aviso, 'sede', 'Por omisión, la de la carrera.')}
        slotProps={SELECCION}
      >
        <option value="">{`La de la carrera${carrera ? ` (${carrera.sede})` : ''}`}</option>
        {sedes.map((unidad) => (
          <option key={unidad.codigo} value={unidad.codigo}>
            {`${unidad.codigo} · ${unidad.nombre}`}
          </option>
        ))}
      </TextField>
      <TextField
        label="Estudiantes previstos"
        type="number"
        value={estudiantes}
        onChange={(evento) => cambiar('estudiantes', evento.target.value)}
        error={Boolean(detalleDe(aviso, 'estudiantes'))}
        helperText={ayuda(aviso, 'estudiantes', 'De 0 a 9999.')}
        slotProps={{ htmlInput: { min: 0, max: 9999 } }}
      />
      <TextField
        label="Horas semanales"
        type="number"
        value={horasIndicadas}
        onChange={(evento) => cambiar('horas', evento.target.value)}
        error={Boolean(detalleDe(aviso, 'horasSemanales'))}
        helperText={ayuda(aviso, 'horasSemanales', pistaDeHoras(horasDeLaAsignatura))}
        slotProps={{ htmlInput: { min: 0.5, max: 20, step: 0.5 } }}
      />
    </>
  );
}

/** El estado de los campos comunes y su cambio. */
function useDatosDelParalelo(inicial: {
  jornada?: string;
  modalidad?: string;
  sede?: string;
  estudiantes?: string;
  horas?: string;
}) {
  const [datos, setDatos] = useState({
    jornada: inicial.jornada ?? '',
    modalidad: inicial.modalidad ?? '',
    sede: inicial.sede ?? '',
    estudiantes: inicial.estudiantes ?? '',
    horas: inicial.horas ?? '',
  });
  const cambiar = (campo: keyof typeof datos, valor: string) =>
    setDatos((actuales) => ({ ...actuales, [campo]: valor }));
  const cuerpo = () => {
    const horasIndicadas = numero(datos.horas);
    return {
      jornada: datos.jornada as Jornada,
      modalidad: datos.modalidad as Modalidad,
      estudiantes: Number(datos.estudiantes),
      ...(datos.sede && { sede: datos.sede }),
      ...(horasIndicadas !== undefined && { horasSemanales: horasIndicadas }),
    };
  };
  return { datos, setDatos, cambiar, cuerpo };
}

export function DialogoOfertar({
  periodo,
  carreras,
  unidades,
  alCerrar,
  alGuardar,
}: {
  periodo: string;
  carreras: readonly Carrera[];
  unidades: readonly UnidadOrganica[];
  alCerrar: () => void;
  alGuardar: () => Promise<void>;
}) {
  const [codigoCarrera, setCodigoCarrera] = useState('');
  const [asignatura, setAsignatura] = useState('');
  const [paralelo, setParalelo] = useState('');
  const [falta, setFalta] = useState<string | null>(null);
  const { datos, setDatos, cambiar, cuerpo } = useDatosDelParalelo({});
  const { aviso, enviando, enviar } = useEnvio(alGuardar);
  const vigentes = carreras.filter((carrera) => carrera.estado === 'VIGENTE');
  const carrera = vigentes.find((una) => una.codigo === codigoCarrera);
  const asignaturas = useAsignaturas(carrera?.codigo);
  const activas = (asignaturas.data ?? []).filter((una) => una.activa);
  const elegida = activas.find((una) => una.codigo === asignatura);

  const elegirCarrera = (codigo: string) => {
    setCodigoCarrera(codigo);
    setAsignatura('');
    const nueva = vigentes.find((una) => una.codigo === codigo);
    setDatos((actuales) => ({
      ...actuales,
      jornada: nueva?.jornadas.length === 1 ? (nueva.jornadas[0] ?? '') : '',
      modalidad: nueva?.modalidad ?? '',
    }));
  };

  const guardar = () => {
    const pendiente = queFalta([
      ['carrera', Boolean(carrera)],
      ['asignatura', Boolean(elegida)],
      ['paralelo', Boolean(paralelo.trim())],
      ['jornada', Boolean(datos.jornada)],
      ['modalidad', Boolean(datos.modalidad)],
      ['estudiantes previstos', Boolean(datos.estudiantes.trim())],
    ]);
    setFalta(pendiente);
    if (!pendiente) {
      enviar(() =>
        ofertarAsignatura(periodo, { asignatura, paralelo: paralelo.trim(), ...cuerpo() }),
      );
    }
  };

  return (
    <Marco
      id="titulo-ofertar"
      titulo="Ofertar asignatura"
      accion="Ofertar"
      enviando={enviando}
      alCerrar={alCerrar}
      alEnviar={guardar}
    >
      <Typography>{`En el periodo ${periodo}. La asignatura-paralelo queda ofertada; se retira en lugar de borrarse.`}</Typography>
      <Avisos aviso={aviso} campos={CAMPOS} />
      <TextField
        select
        label="Carrera"
        value={codigoCarrera}
        onChange={(evento) => elegirCarrera(evento.target.value)}
        helperText="Solo las vigentes."
        slotProps={SELECCION}
      >
        <option value="">Elige una</option>
        {vigentes.map((una) => (
          <option key={una.codigo} value={una.codigo}>
            {`${una.nombre} · ${una.codigo}`}
          </option>
        ))}
      </TextField>
      {carrera && (
        <TextField
          select
          label="Asignatura"
          value={asignatura}
          onChange={(evento) => setAsignatura(evento.target.value)}
          error={Boolean(detalleDe(aviso, 'asignatura'))}
          helperText={ayuda(aviso, 'asignatura', 'Solo las activas.')}
          slotProps={SELECCION}
        >
          <option value="">Elige una</option>
          {activas.map((una) => (
            <option key={una.codigo} value={una.codigo}>
              {`Ciclo ${String(una.ciclo)} · ${una.codigo} · ${una.nombre}`}
            </option>
          ))}
        </TextField>
      )}
      <TextField
        label="Paralelo"
        value={paralelo}
        onChange={(evento) => setParalelo(evento.target.value.toUpperCase())}
        error={Boolean(detalleDe(aviso, 'paralelo'))}
        helperText={ayuda(aviso, 'paralelo', 'Por ejemplo, A_NOCTURNA.')}
        slotProps={{ htmlInput: { maxLength: 20 } }}
      />
      <DatosDelParalelo
        carrera={carrera}
        unidades={unidades}
        {...datos}
        horasIndicadas={datos.horas}
        horasDeLaAsignatura={elegida?.horasSemanalesClase}
        aviso={aviso}
        cambiar={cambiar}
      />
      {falta && <Typography color="error">{falta}</Typography>}
    </Marco>
  );
}

export function DialogoCorregir({
  asignaturaParalelo,
  carrera,
  unidades,
  alCerrar,
  alGuardar,
}: {
  asignaturaParalelo: AsignaturaParalelo;
  carrera: Carrera | undefined;
  unidades: readonly UnidadOrganica[];
  alCerrar: () => void;
  alGuardar: () => Promise<void>;
}) {
  const [falta, setFalta] = useState<string | null>(null);
  const { datos, cambiar, cuerpo } = useDatosDelParalelo({
    jornada: asignaturaParalelo.jornada,
    modalidad: asignaturaParalelo.modalidad,
    sede: asignaturaParalelo.sedeIndicada ? asignaturaParalelo.sede : '',
    estudiantes: String(asignaturaParalelo.estudiantes),
    horas: asignaturaParalelo.horasIndicadas ? String(asignaturaParalelo.horasSemanales) : '',
  });
  const { aviso, enviando, enviar } = useEnvio(alGuardar);
  const { asignatura, paralelo } = asignaturaParalelo;

  const guardar = () => {
    const pendiente = queFalta([
      ['jornada', Boolean(datos.jornada)],
      ['modalidad', Boolean(datos.modalidad)],
      ['estudiantes previstos', Boolean(datos.estudiantes.trim())],
    ]);
    setFalta(pendiente);
    if (!pendiente) {
      enviar(() => corregirAsignaturaParalelo(asignaturaParalelo, cuerpo()));
    }
  };

  return (
    <Marco
      id="titulo-corregir-paralelo"
      titulo={`Corregir ${asignatura} en ${paralelo}`}
      accion="Guardar"
      enviando={enviando}
      alCerrar={alCerrar}
      alEnviar={guardar}
    >
      <Typography>{`${asignaturaParalelo.nombre}. El periodo, la asignatura y el paralelo no cambian.`}</Typography>
      <Avisos aviso={aviso} campos={CAMPOS} />
      <DatosDelParalelo
        carrera={carrera}
        unidades={unidades}
        {...datos}
        horasIndicadas={datos.horas}
        horasDeLaAsignatura={asignaturaParalelo.horasDeLaAsignatura}
        aviso={aviso}
        cambiar={cambiar}
      />
      {falta && <Typography color="error">{falta}</Typography>}
    </Marco>
  );
}
