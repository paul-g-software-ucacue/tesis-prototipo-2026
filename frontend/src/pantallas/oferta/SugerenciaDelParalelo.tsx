// Los docentes sugeridos para una asignatura-paralelo (F06-H02, RF-25): los candidatos con su puntaje,
// su desglose y sus horas, y cuántos quedaron fuera por el mínimo de afinidad. Lo arma Generación,
// que ya los manda en orden (ADR-010); esta pantalla solo los muestra y no propone a nadie.
import Accordion from '@mui/material/Accordion';
import AccordionDetails from '@mui/material/AccordionDetails';
import AccordionSummary from '@mui/material/AccordionSummary';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError } from '../../errores/traducir';
import { DesgloseDeAfinidad } from '../comun/DesgloseDeAfinidad';
import { numero, puntos } from '../comun/numeros';
import { NOMBRE_DE_LA_DEDICACION } from '../docentes/nombres';
import {
  useSugerencia,
  type AsignaturaParalelo,
  type Candidato as DatosDelCandidato,
  type Sugerencia,
} from './consultas';

function textoDeLasHoras(candidato: DatosDelCandidato) {
  return [
    NOMBRE_DE_LA_DEDICACION[candidato.dedicacion],
    `${String(candidato.horasContratadas)} h contratadas`,
    `${numero(candidato.horasDisponibles)} h disponibles`,
    `${numero(candidato.horasEnOtrasCarreras)} h en otras carreras y unidades`,
  ].join(' · ');
}

function textoDeFuera(cantidad: number) {
  return cantidad === 1
    ? '1 docente quedó fuera por no alcanzar el mínimo.'
    : `${String(cantidad)} docentes quedaron fuera por no alcanzar el mínimo.`;
}

/** Un candidato con su puntaje y sus horas; se despliega para ver de dónde sale el puntaje. */
function Candidato({ candidato }: { candidato: DatosDelCandidato }) {
  return (
    <Accordion disableGutters variant="outlined" slotProps={{ heading: { component: 'h3' } }}>
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
  );
}

function Candidatos({ sugerencia }: { sugerencia: Sugerencia }) {
  const { candidatos, fueraPorElMinimo } = sugerencia;
  return (
    <Stack spacing={1}>
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
      {fueraPorElMinimo > 0 && (
        <Typography color="text.secondary">{textoDeFuera(fueraPorElMinimo)}</Typography>
      )}
      <div>
        {candidatos.map((candidato) => (
          <Candidato key={candidato.identificacion} candidato={candidato} />
        ))}
      </div>
    </Stack>
  );
}

/** El diálogo con la sugerencia; la consulta sale al abrirlo. */
export function SugerenciaDelParalelo({
  periodo,
  asignaturaParalelo,
  alCerrar,
}: {
  periodo: string;
  asignaturaParalelo: AsignaturaParalelo;
  alCerrar: () => void;
}) {
  const { asignatura, nombre, paralelo } = asignaturaParalelo;
  const sugerencia = useSugerencia(periodo, asignatura, paralelo);
  return (
    <Dialog open onClose={alCerrar} aria-labelledby="titulo-sugerencia" fullWidth maxWidth="md">
      <DialogTitle id="titulo-sugerencia">
        {`Docentes sugeridos para ${nombre} · ${paralelo}`}
      </DialogTitle>
      {/* La lista solo se lee y puede ser larga: el contenido recibe el foco para desplazarse con el
          teclado (WCAG 2.1.1, regla scrollable-region-focusable de axe). */}
      <DialogContent tabIndex={0} aria-labelledby="titulo-sugerencia">
        {sugerencia.isFetching && <Typography role="status">Calculando la sugerencia…</Typography>}
        {sugerencia.error && <AvisoDeError mensaje={traducirError(sugerencia.error)} />}
        {sugerencia.data && !sugerencia.isFetching && <Candidatos sugerencia={sugerencia.data} />}
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cerrar</Button>
      </DialogActions>
    </Dialog>
  );
}
