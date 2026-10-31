// «Afinidad por asignatura» en la ficha del docente (F06-H01, RF-17): su puntaje en cada asignatura
// de la oferta del periodo elegido y de dónde sale. Lo calcula Generación con los pesos de la versión
// de normativa del periodo (ADR-010); esta pantalla solo lo muestra, en el orden en que llega.
import Accordion from '@mui/material/Accordion';
import AccordionDetails from '@mui/material/AccordionDetails';
import AccordionSummary from '@mui/material/AccordionSummary';
import Box from '@mui/material/Box';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError } from '../../errores/traducir';
import {
  useAfinidadDelDocente,
  useReferenciasDeDocentes,
  type AporteDeAfinidad,
  type PuntajeAfinidad,
  type TipoDeAporte,
} from './consultas';

const NOMBRE_DEL_APORTE: Record<TipoDeAporte, string> = {
  TERCER_NIVEL: 'Título de tercer nivel',
  CUARTO_NIVEL: 'Título de cuarto nivel',
  EXPERIENCIA: 'Experiencia',
  CURSO: 'Curso',
};

const NUMERO = new Intl.NumberFormat('es-EC', { maximumFractionDigits: 2 });

function puntos(valor: number) {
  return `${NUMERO.format(valor)} ${valor === 1 ? 'punto' : 'puntos'}`;
}

function textoDelAporte({ tipo, nombre, coincidencia, aporte }: AporteDeAfinidad) {
  const campo = coincidencia === 'CAMPO_ESPECIFICO' ? 'campo específico' : 'solo campo amplio';
  return `${NOMBRE_DEL_APORTE[tipo]}: ${nombre} · ${campo} · ${puntos(aporte)}`;
}

/** Una asignatura con su puntaje; se despliega para ver de dónde sale. */
function Puntaje({ puntaje }: { puntaje: PuntajeAfinidad }) {
  return (
    <Accordion disableGutters variant="outlined" slotProps={{ heading: { component: 'h3' } }}>
      <AccordionSummary>
        <Stack direction="row" spacing={2} sx={{ justifyContent: 'space-between', width: '100%' }}>
          <span>{puntaje.nombre}</span>
          <Box component="span" sx={{ fontWeight: 'bold', whiteSpace: 'nowrap' }}>
            {puntos(puntaje.total)}
          </Box>
        </Stack>
      </AccordionSummary>
      <AccordionDetails>
        {puntaje.desglose.length === 0 && (
          <Typography color="text.secondary">
            Nada de la ficha coincide con el campo de esta asignatura.
          </Typography>
        )}
        {puntaje.desglose.map((aporte) => (
          <Typography key={`${aporte.tipo}-${aporte.nombre}`}>{textoDelAporte(aporte)}</Typography>
        ))}
        {puntaje.topeAplicado && (
          <Typography color="text.secondary">
            Se aplicó el tope de cursos de la normativa: los demás cursos ya no suman.
          </Typography>
        )}
      </AccordionDetails>
    </Accordion>
  );
}

/** El selector del periodo y el puntaje del docente en su oferta. */
export function AfinidadDelDocente({ identificacion }: { identificacion: string }) {
  const [periodo, setPeriodo] = useState('');
  const periodos = useReferenciasDeDocentes().data?.periodos ?? [];
  const afinidad = useAfinidadDelDocente(identificacion, periodo);
  const datos = afinidad.data;
  return (
    <Box component="section" aria-labelledby="ficha-afinidad">
      <Typography id="ficha-afinidad" component="h2" variant="h6">
        Afinidad por asignatura
      </Typography>
      <Stack spacing={1} sx={{ mt: 1 }}>
        <TextField
          select
          label="Periodo"
          value={periodo}
          onChange={(evento) => setPeriodo(evento.target.value)}
          size="small"
          sx={{ maxWidth: 420 }}
        >
          {periodos.map((uno) => (
            <MenuItem key={uno.codigo} value={uno.codigo}>
              {`${uno.codigo} · ${uno.nombre}`}
            </MenuItem>
          ))}
        </TextField>
        {!periodo && (
          <Typography color="text.secondary">
            Elige un periodo para ver el puntaje en cada asignatura de su oferta.
          </Typography>
        )}
        {afinidad.isFetching && <Typography role="status">Calculando la afinidad…</Typography>}
        {afinidad.error && <AvisoDeError mensaje={traducirError(afinidad.error)} />}
        {datos && !afinidad.isFetching && datos.puntajes.length === 0 && (
          <Typography>Este periodo todavía no tiene oferta.</Typography>
        )}
        {datos && !afinidad.isFetching && datos.puntajes.length > 0 && (
          <div>
            <Typography color="text.secondary" sx={{ mb: 1 }}>
              {`Con los pesos de la versión ${String(datos.version)} de la normativa, del mayor puntaje al menor.`}
            </Typography>
            {datos.puntajes.map((puntaje) => (
              <Puntaje key={puntaje.asignatura} puntaje={puntaje} />
            ))}
          </div>
        )}
      </Stack>
    </Box>
  );
}
