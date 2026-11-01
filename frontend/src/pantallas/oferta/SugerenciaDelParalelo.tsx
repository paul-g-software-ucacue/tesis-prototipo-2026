// Los docentes sugeridos para una asignatura-paralelo (F06-H02, RF-25) y la propuesta (F06-H03,
// RF-24): los candidatos con su puntaje, su desglose y sus horas, y aparte los que no alcanzan el
// mínimo. Lo arma Generación, que ya los manda en orden (ADR-010). Quien escribe la oferta propone;
// la justificación la decide el servidor, y aquí solo se pide el texto de una excepción.
import Accordion from '@mui/material/Accordion';
import AccordionDetails from '@mui/material/AccordionDetails';
import AccordionSummary from '@mui/material/AccordionSummary';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState, type ReactNode } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError } from '../../errores/traducir';
import { DesgloseDeAfinidad } from '../comun/DesgloseDeAfinidad';
import { Avisos, Marco } from '../comun/Formulario';
import { ayuda, detalleDe } from '../comun/formulario';
import { numero, puntos } from '../comun/numeros';
import { useEnvio } from '../comun/useEnvio';
import { NOMBRE_DE_LA_DEDICACION } from '../docentes/nombres';
import {
  NOMBRE_DE_LA_JUSTIFICACION,
  proponerDocente,
  useSugerencia,
  type AsignaturaParalelo,
  type Candidato as DatosDelCandidato,
  type Sugerencia,
} from './consultas';

const LARGO_MINIMO = 10;
const LARGO_MAXIMO = 500;
const TEXTO_OBLIGATORIO = 'Escribe por qué propones a este docente, en 10 a 500 caracteres.';

function textoDeLasHoras(candidato: DatosDelCandidato) {
  return [
    NOMBRE_DE_LA_DEDICACION[candidato.dedicacion],
    `${String(candidato.horasContratadas)} h contratadas`,
    `${numero(candidato.horasDisponibles)} h disponibles`,
    `${numero(candidato.horasEnOtrasCarreras)} h en otras carreras y unidades`,
  ].join(' · ');
}

/** Un candidato con su puntaje y sus horas; se despliega para ver de dónde sale el puntaje. */
function Candidato({
  candidato,
  nivel,
  accion,
}: {
  candidato: DatosDelCandidato;
  nivel: 'h3' | 'h4';
  accion: ReactNode;
}) {
  return (
    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
      <Accordion
        disableGutters
        variant="outlined"
        slotProps={{ heading: { component: nivel } }}
        sx={{ flexGrow: 1, minWidth: 0 }}
      >
        <AccordionSummary>
          <Stack sx={{ width: '100%' }}>
            <Stack
              direction="row"
              spacing={2}
              sx={{ justifyContent: 'space-between', width: '100%' }}
            >
              <span>{candidato.nombre}</span>
              <Box component="span" sx={{ fontWeight: 'bold', whiteSpace: 'nowrap' }}>
                {puntos(candidato.puntaje.total)}
              </Box>
            </Stack>
            <Typography component="span" variant="body2" color="text.secondary">
              {textoDeLasHoras(candidato)}
            </Typography>
            {!candidato.horasSuficientes && (
              <Typography component="span" variant="body2" color="error">
                Sin horas suficientes
              </Typography>
            )}
          </Stack>
        </AccordionSummary>
        <AccordionDetails>
          <DesgloseDeAfinidad puntaje={candidato.puntaje} />
        </AccordionDetails>
      </Accordion>
      {accion}
    </Stack>
  );
}

/** Pide el texto de una excepción; el servidor lo vuelve a validar. */
function DialogoDeJustificacion({
  candidato,
  alCerrar,
  alEnviar,
  alGuardar,
}: {
  candidato: DatosDelCandidato;
  alCerrar: () => void;
  alEnviar: (texto: string) => Promise<unknown>;
  alGuardar: () => Promise<void>;
}) {
  const [texto, setTexto] = useState('');
  const [falta, setFalta] = useState(false);
  const { aviso, enviando, enviar } = useEnvio(alGuardar);
  const pista = `${String(texto.length)} de ${String(LARGO_MAXIMO)} caracteres.`;

  const guardar = () => {
    const limpio = texto.trim();
    const corto = limpio.length < LARGO_MINIMO;
    setFalta(corto);
    if (!corto) {
      enviar(() => alEnviar(limpio));
    }
  };

  return (
    <Marco
      id="titulo-justificacion"
      titulo={`Justificar a ${candidato.nombre}`}
      accion="Guardar propuesta"
      enviando={enviando}
      alCerrar={alCerrar}
      alEnviar={guardar}
    >
      <Typography>
        No alcanza el mínimo de afinidad: la propuesta queda como una excepción, con tu
        justificación.
      </Typography>
      <Avisos aviso={aviso} campos={['texto']} />
      <TextField
        label="Justificación"
        value={texto}
        onChange={(evento) => {
          setFalta(false);
          setTexto(evento.target.value);
        }}
        multiline
        minRows={3}
        required
        error={falta || Boolean(detalleDe(aviso, 'texto'))}
        helperText={falta ? `${TEXTO_OBLIGATORIO} ${pista}` : ayuda(aviso, 'texto', pista)}
        slotProps={{ htmlInput: { maxLength: LARGO_MAXIMO } }}
      />
    </Marco>
  );
}

function Candidatos({
  sugerencia,
  puedeProponer,
  enviando,
  alProponer,
}: {
  sugerencia: Sugerencia;
  puedeProponer: boolean;
  enviando: boolean;
  alProponer: (candidato: DatosDelCandidato, conTexto: boolean) => void;
}) {
  const { candidatos, bajoElMinimo, fueraPorElMinimo, propuesta } = sugerencia;
  const propone = puedeProponer && sugerencia.distributivoCreado;
  const accion = (candidato: DatosDelCandidato, conTexto: boolean) => {
    if (propuesta?.docente === candidato.identificacion) {
      return <Typography variant="body2">Propuesto</Typography>;
    }
    return propone ? (
      <Button
        size="small"
        disabled={enviando}
        aria-label={`Proponer a ${candidato.nombre}`}
        onClick={() => alProponer(candidato, conTexto)}
      >
        Proponer
      </Button>
    ) : null;
  };
  return (
    <Stack spacing={1}>
      {puedeProponer && !sugerencia.distributivoCreado && (
        <Alert severity="info">
          El periodo todavía no tiene distributivo. Pide al Vicerrector académico que lo cree.
        </Alert>
      )}
      {propuesta && (
        <Typography>
          {`Propuesta actual: ${propuesta.nombreDocente} · ${NOMBRE_DE_LA_JUSTIFICACION[propuesta.justificacion]}`}
        </Typography>
      )}
      <Typography color="text.secondary">
        {sugerencia.troncoComun
          ? 'Es de tronco común: el mínimo de afinidad no aplica.'
          : `Con el mínimo de ${puntos(sugerencia.minimoAfinidad)} de la versión ${String(sugerencia.version)} de la normativa.`}
      </Typography>
      {candidatos.length === 0 && (
        <Typography>
          {fueraPorElMinimo > 0
            ? 'Ningún docente alcanza el mínimo en esta asignatura.'
            : 'No hay docentes activos.'}
        </Typography>
      )}
      <div>
        {candidatos.map((candidato) => (
          <Candidato
            key={candidato.identificacion}
            candidato={candidato}
            nivel="h3"
            accion={accion(candidato, false)}
          />
        ))}
      </div>
      {bajoElMinimo.length > 0 && (
        <Accordion disableGutters variant="outlined" slotProps={{ heading: { component: 'h3' } }}>
          <AccordionSummary expandIcon={<span aria-hidden="true">▾</span>}>
            {`Docentes bajo el mínimo (${String(bajoElMinimo.length)})`}
          </AccordionSummary>
          <AccordionDetails>
            <Typography sx={{ mb: 1 }} color="text.secondary">
              No alcanzan el mínimo de afinidad. Proponer a uno exige una justificación escrita.
            </Typography>
            {bajoElMinimo.map((candidato) => (
              <Candidato
                key={candidato.identificacion}
                candidato={candidato}
                nivel="h4"
                accion={accion(candidato, true)}
              />
            ))}
          </AccordionDetails>
        </Accordion>
      )}
    </Stack>
  );
}

/** El diálogo con la sugerencia; la consulta sale al abrirlo. */
export function SugerenciaDelParalelo({
  periodo,
  asignaturaParalelo,
  puedeProponer,
  alCerrar,
  alProponer,
}: {
  periodo: string;
  asignaturaParalelo: AsignaturaParalelo;
  puedeProponer: boolean;
  alCerrar: () => void;
  /** Se llama con la propuesta guardada: recarga la oferta y cierra. */
  alProponer: () => Promise<void>;
}) {
  const { asignatura, nombre, paralelo } = asignaturaParalelo;
  const sugerencia = useSugerencia(periodo, asignatura, paralelo);
  const { aviso, enviando, enviar } = useEnvio(alProponer);
  const [porJustificar, setPorJustificar] = useState<DatosDelCandidato | null>(null);

  const proponer = (docente: string, texto?: string) =>
    proponerDocente(periodo, asignatura, paralelo, { docente, texto });

  const elegir = (candidato: DatosDelCandidato, conTexto: boolean) => {
    if (conTexto) {
      setPorJustificar(candidato);
    } else {
      enviar(() => proponer(candidato.identificacion));
    }
  };

  return (
    <Dialog open onClose={alCerrar} aria-labelledby="titulo-sugerencia" fullWidth maxWidth="md">
      <DialogTitle id="titulo-sugerencia">
        {`Docentes sugeridos para ${nombre} · ${paralelo}`}
      </DialogTitle>
      {/* La lista puede ser larga: el contenido recibe el foco para desplazarse con el teclado
          (WCAG 2.1.1, regla scrollable-region-focusable de axe). */}
      <DialogContent tabIndex={0} aria-labelledby="titulo-sugerencia">
        {sugerencia.isFetching && <Typography role="status">Calculando la sugerencia…</Typography>}
        {sugerencia.error && <AvisoDeError mensaje={traducirError(sugerencia.error)} />}
        <Avisos aviso={aviso} campos={[]} />
        {sugerencia.data && !sugerencia.isFetching && (
          <Candidatos
            sugerencia={sugerencia.data}
            puedeProponer={puedeProponer}
            enviando={enviando}
            alProponer={elegir}
          />
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cerrar</Button>
      </DialogActions>
      {porJustificar && (
        <DialogoDeJustificacion
          candidato={porJustificar}
          alCerrar={() => setPorJustificar(null)}
          alEnviar={(texto) => proponer(porJustificar.identificacion, texto)}
          alGuardar={alProponer}
        />
      )}
    </Dialog>
  );
}
