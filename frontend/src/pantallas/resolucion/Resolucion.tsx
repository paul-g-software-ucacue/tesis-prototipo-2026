// La pantalla «Resolución del OCS» (F15-H01, D-107): quien tiene el permiso de la resolución elige
// el periodo y ve el estado del flujo, con el formulario para registrarla (solo el rol de EN_OCS) o
// su constancia con la descarga. La Secretaría general no abre «Consolidado del periodo»: lee el
// flujo y los periodos, no el consolidado (D-16).
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import { EstadoDelFlujoDelPeriodo } from '../comun/EstadoDelFlujo';
import { SELECCION } from '../comun/formulario';
import { usePeriodos } from '../periodos/consultas';
import { usePeriodoElegido } from '../periodos/periodoElegido';
import { NOMBRE_DEL_ESTADO } from '../periodos/estados';

export function Resolucion() {
  useTitulo('Resolución del OCS');
  const periodos = usePeriodos();
  // El de la notificación que trajo aquí; si no, el más reciente en Planificación.
  const [periodo, elegir] = usePeriodoElegido(periodos.data);

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
          onChange={(evento) => elegir(evento.target.value)}
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
