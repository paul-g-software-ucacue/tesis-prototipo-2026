// El diálogo de asignar una actividad (F06-H04, RF-26). Solo ofrece lo que responde el servidor: cada
// actividad dice si está disponible y por qué no, y qué proyectos se pueden elegir. Las horas se
// validan aquí y en el servidor (principio XIV).
import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useRef, useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError } from '../../errores/traducir';
import { Avisos, Marco } from '../comun/Formulario';
import { SELECCION, ayuda, detalleDe, queFalta } from '../comun/formulario';
import { useEnvio } from '../comun/useEnvio';
import { PanelDeCarga } from '../comun/PanelDeCarga';
import { useDocentes } from '../docentes/consultas';
import {
  NOMBRE_DEL_COMPONENTE,
  asignarActividad,
  literal,
  useActividadesPosibles,
  type ActividadPosible,
  type Componente,
} from './consultas';
import { SelectorLargo } from './SelectorLargo';

const CAMPOS = ['horas', 'proyecto'];
const COMPONENTES = Object.keys(NOMBRE_DEL_COMPONENTE) as Componente[];
const HORAS = /^\d{1,2}([.,]\d{1,2})?$/;
const TOPE_DE_HORAS = 40;
const HORAS_NO_VALIDAS = 'Escribe unas horas mayores que 0 y hasta 40, con hasta dos decimales.';
const PISTA_DE_HORAS = 'Horas semanales, con hasta dos decimales.';

type ProyectoPosible = ActividadPosible['proyectos'][number];

function textoDeLaActividad(una: ActividadPosible) {
  return `${literal(una.actividad)} ${una.nombre}`;
}

function textoDelProyecto(uno: ProyectoPosible) {
  return `${uno.codigo} · ${uno.nombre}`;
}

/** Las horas escritas, o nada si no son válidas. */
function horasEscritas(texto: string) {
  const limpio = texto.trim();
  if (!HORAS.test(limpio)) {
    return undefined;
  }
  const valor = Number(limpio.replace(',', '.'));
  return valor > 0 && valor <= TOPE_DE_HORAS ? valor : undefined;
}

export function DialogoAsignar({
  periodo,
  carrera,
  alCerrar,
  alGuardar,
}: {
  periodo: string;
  carrera: string;
  alCerrar: () => void;
  /** Se llama con lo guardado: recarga y cierra. Recibe el aviso de horas, si hace falta. */
  alGuardar: (aviso: string | null, docente: string) => Promise<void>;
}) {
  const [docente, setDocente] = useState('');
  const [codigo, setCodigo] = useState('');
  const [proyecto, setProyecto] = useState('');
  const [textoDeHoras, setTextoDeHoras] = useState('');
  const [falta, setFalta] = useState<string | null>(null);
  const [malasHoras, setMalasHoras] = useState(false);
  const docentes = useDocentes({ texto: '', carrera: '', estado: 'ACTIVO' });
  const posibles = useActividadesPosibles(periodo, docente);
  // Una referencia, no un estado: se lee justo después de guardar, antes de volver a pintar.
  const avisoDeHoras = useRef<string | null>(null);
  const envio = useEnvio(() => alGuardar(avisoDeHoras.current, docente));
  const actividades = posibles.data?.actividades ?? [];
  const ordenadas = COMPONENTES.flatMap((componente) =>
    actividades.filter((una) => una.componente === componente),
  );
  const elegida: ActividadPosible | undefined = actividades.find((una) => una.actividad === codigo);
  const proyectoElegido = elegida?.proyectos.find((uno) => uno.codigo === proyecto);
  const nombreDelDocente = docentes.data?.find(
    (uno) => uno.identificacion === docente,
  )?.nombreCompleto;

  const elegirDocente = (valor: string) => {
    setDocente(valor);
    setCodigo('');
    setProyecto('');
  };

  const guardar = () => {
    const pide = elegida?.exigeProyecto ?? false;
    const pendiente = queFalta([
      ['docente', Boolean(docente)],
      ['actividad', Boolean(elegida)],
      ['proyecto', !pide || Boolean(proyecto)],
    ]);
    const valor = horasEscritas(textoDeHoras);
    setFalta(pendiente);
    setMalasHoras(valor === undefined);
    if (pendiente || valor === undefined || !elegida) {
      return;
    }
    envio.enviar(async () => {
      const guardada = await asignarActividad(
        { periodo, carrera, docente, actividad: elegida.actividad },
        { horas: valor, ...(pide && { proyecto }) },
      );
      avisoDeHoras.current = guardada.horasSuficientes
        ? null
        : `Se guardó, pero a ${nombreDelDocente ?? docente} no le alcanzan las horas.`;
    });
  };

  return (
    <Marco
      id="titulo-asignar"
      titulo="Asignar actividad"
      accion="Asignar"
      enviando={envio.enviando || elegida?.disponible === false}
      alCerrar={alCerrar}
      alEnviar={guardar}
    >
      <Avisos aviso={envio.aviso} campos={CAMPOS} />
      {docentes.error && <AvisoDeError mensaje={traducirError(docentes.error)} />}
      {posibles.error && <AvisoDeError mensaje={traducirError(posibles.error)} />}
      {falta && <Typography color="error">{falta}</Typography>}
      <TextField
        select
        label="Docente"
        value={docente}
        onChange={(evento) => elegirDocente(evento.target.value)}
        helperText="Solo los docentes activos."
        slotProps={SELECCION}
      >
        <option value="">Elige uno</option>
        {(docentes.data ?? []).map((uno) => (
          <option key={uno.identificacion} value={uno.identificacion}>
            {uno.nombreCompleto}
          </option>
        ))}
      </TextField>
      {docente && <PanelDeCarga periodo={periodo} docente={docente} />}
      <SelectorLargo
        etiqueta="Actividad"
        opciones={ordenadas}
        elegida={elegida ?? null}
        claveDe={(una) => una.actividad}
        textoDe={textoDeLaActividad}
        grupoDe={(una) => NOMBRE_DEL_COMPONENTE[una.componente]}
        alElegir={(una) => {
          setCodigo(una?.actividad ?? '');
          setProyecto('');
        }}
        disabled={actividades.length === 0}
        error={elegida?.disponible === false}
        ayuda={
          elegida ? (
            <>
              {textoDeLaActividad(elegida)}
              {elegida.motivo && (
                <Box component="span" sx={{ display: 'block' }}>
                  {elegida.motivo}
                </Box>
              )}
            </>
          ) : docente ? undefined : (
            'Elige primero el docente.'
          )
        }
      />
      {elegida?.exigeProyecto && elegida.disponible && (
        <SelectorLargo
          etiqueta="Proyecto"
          opciones={elegida.proyectos}
          elegida={proyectoElegido ?? null}
          claveDe={(uno) => uno.codigo}
          textoDe={textoDelProyecto}
          alElegir={(uno) => setProyecto(uno?.codigo ?? '')}
          error={Boolean(detalleDe(envio.aviso, 'proyecto'))}
          ayuda={
            proyectoElegido
              ? textoDelProyecto(proyectoElegido)
              : ayuda(envio.aviso, 'proyecto', 'Los proyectos asignables del docente.')
          }
        />
      )}
      <TextField
        label="Horas semanales"
        value={textoDeHoras}
        onChange={(evento) => {
          setMalasHoras(false);
          setTextoDeHoras(evento.target.value);
        }}
        error={malasHoras || Boolean(detalleDe(envio.aviso, 'horas'))}
        helperText={malasHoras ? HORAS_NO_VALIDAS : ayuda(envio.aviso, 'horas', PISTA_DE_HORAS)}
        slotProps={{ htmlInput: { inputMode: 'decimal', maxLength: 5 } }}
      />
    </Marco>
  );
}
