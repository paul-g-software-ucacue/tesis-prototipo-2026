// El historial de una carga (F01-H04, RF-10): cada intento de confirmarla, del más reciente al más
// antiguo, con el usuario, la fecha, el archivo, su huella, el resultado y lo que aplicó. Lo ven
// quienes ven las plantillas (D-70); no guarda el contenido de los archivos.
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError } from '../../errores/traducir';
import { useHistorial, type Carga, type EntradaDeHistorial } from './consultas';

const RESULTADO: Record<EntradaDeHistorial['resultado'], string> = {
  APLICADA: 'Aplicada',
  RECHAZADA: 'Rechazada',
  REPETIDA: 'Repetida',
};

const MOTIVO: Record<string, string> = {
  CARGAS_CARGA_CON_ERRORES: 'Con errores',
  CARGAS_VISTA_PREVIA_DESACTUALIZADA: 'Vista previa desactualizada',
  CARGAS_CARGA_RECHAZADA: 'Rechazada por su servicio dueño',
};

const FECHA = new Intl.DateTimeFormat('es-EC', { dateStyle: 'short', timeStyle: 'short' });

export function HistorialDeLaCarga({ carga, alCerrar }: { carga: Carga; alCerrar: () => void }) {
  const historial = useHistorial(carga);
  const titulo = `titulo-del-historial-${carga.codigo}`;
  return (
    <Dialog open onClose={alCerrar} aria-labelledby={titulo} maxWidth="lg" fullWidth>
      <DialogTitle id={titulo}>{`Historial de ${carga.nombre}`}</DialogTitle>
      <DialogContent>
        {historial.error && <AvisoDeError mensaje={traducirError(historial.error)} />}
        {historial.data && historial.data.length === 0 && (
          <Typography>Todavía no se ha confirmado ningún archivo de esta carga.</Typography>
        )}
        {historial.data && historial.data.length > 0 && (
          <TableContainer>
            <Table size="small" aria-label={`Historial de ${carga.nombre}`}>
              <TableHead>
                <TableRow>
                  <TableCell>Fecha</TableCell>
                  <TableCell>Usuario</TableCell>
                  <TableCell>Archivo</TableCell>
                  <TableCell>Huella</TableCell>
                  <TableCell>Resultado</TableCell>
                  <TableCell>Nuevos</TableCell>
                  <TableCell>Cambiados</TableCell>
                  <TableCell>Bajas sin aplicar</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {historial.data.map((entrada) => (
                  <TableRow key={`${entrada.momento}-${entrada.hash}-${entrada.resultado}`}>
                    <TableCell>{FECHA.format(new Date(entrada.momento))}</TableCell>
                    <TableCell>{entrada.usuario}</TableCell>
                    <TableCell>{entrada.archivo}</TableCell>
                    <TableCell>
                      <code title={entrada.hash}>{entrada.hash.slice(0, 12)}</code>
                    </TableCell>
                    <TableCell>
                      {RESULTADO[entrada.resultado]}
                      {entrada.motivo && ` (${MOTIVO[entrada.motivo] ?? entrada.motivo})`}
                    </TableCell>
                    <TableCell>{entrada.nuevos}</TableCell>
                    <TableCell>{entrada.cambiados}</TableCell>
                    <TableCell>{entrada.bajasSinAplicar}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cerrar</Button>
      </DialogActions>
    </Dialog>
  );
}
