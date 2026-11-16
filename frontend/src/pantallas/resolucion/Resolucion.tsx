// La pantalla «Resolución del OCS» (F15-H01, D-107): quien tiene el permiso de la resolución elige
// el periodo y ve el estado del flujo, con el formulario para registrarla (solo el rol de EN_OCS) o
// su constancia con la descarga. La Secretaría general no abre «Consolidado del periodo»: lee el
// flujo y los periodos, no el consolidado (D-16).
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import { EstadoDelFlujoDelPeriodo } from '../comun/EstadoDelFlujo';
import { SELECCION } from '../comun/formulario';
import { usePeriodos } from '../periodos/consultas';
import { NOMBRE_DEL_ESTADO } from '../periodos/estados';

export function Resolucion() {
  useTitulo('Resolución del OCS');
  const periodos = usePeriodos();
  const [elegido, setElegido] = useState<string | null>(null);
  // Por omisión, el más reciente en Planificación; si no hay, el más reciente.
  const porOmision =
    periodos.data?.find((periodo) => periodo.estado === 'PLANIFICACION')?.codigo ??
    periodos.data?.[0]?.codigo;
  const periodo = elegido ?? porOmision;

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Resolución del OCS
      </Typography>
      {periodos.error && <AvisoDeError mensaje={traducirError(periodos.error)} />}
      {periodos.data && (
        <TextField
          select
          label="Periodo"
          value={periodo ?? ''}
          onChange={(evento) => setElegido(evento.target.value)}
          slotProps={SELECCION}
          sx={{ my: 1, minWidth: 280, display: 'block' }}
        >
          {periodos.data.length === 0 && <option value="">No hay periodos</option>}
          {periodos.data.map((uno) => (
            <option key={uno.codigo} value={uno.codigo}>
              {`${uno.codigo} · ${uno.nombre} · ${NOMBRE_DEL_ESTADO[uno.estado]}`}
            </option>
          ))}
        </TextField>
      )}
      {periodo && <EstadoDelFlujoDelPeriodo key={periodo} periodo={periodo} />}
    </>
  );
}
