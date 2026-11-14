// La pantalla «Pendientes de firma» (F14-H01, RF-38, D-102): los documentos que le toca firmar al
// usuario, en turno según el servidor, y la descarga del PDF vigente para firmarlo con FirmaEC.
// Subir la firma es de F14-H02.
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import { descargarArchivoVigente, useFirmasPendientes, type FirmaPendiente } from './consultas';

// Solo para los lectores de pantalla, como en «Roles».
const visuallyHidden = {
  border: 0,
  clip: 'rect(0 0 0 0)',
  height: 1,
  margin: -1,
  overflow: 'hidden',
  padding: 0,
  position: 'absolute',
  whiteSpace: 'nowrap',
  width: 1,
} as const;

function fecha(instante: string) {
  return new Date(instante).toLocaleString('es-EC', {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
}

function nombreDeLaFila(pendiente: FirmaPendiente) {
  return `${pendiente.periodo}, versión ${String(pendiente.version)}`;
}

export function FirmasPendientes() {
  useTitulo('Pendientes de firma');
  const pendientes = useFirmasPendientes();
  const [descargando, setDescargando] = useState<string | null>(null);
  const [fallo, setFallo] = useState<MensajeDeError | null>(null);

  const descargar = async (pendiente: FirmaPendiente) => {
    setFallo(null);
    setDescargando(pendiente.documento);
    try {
      await descargarArchivoVigente(pendiente);
    } catch (error) {
      setFallo(traducirError(error));
    } finally {
      setDescargando(null);
    }
  };

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Pendientes de firma
      </Typography>
      {pendientes.error && <AvisoDeError mensaje={traducirError(pendientes.error)} />}
      {fallo && <AvisoDeError mensaje={fallo} />}
      {pendientes.data?.length === 0 && (
        <Alert severity="info" role="status" sx={{ my: 2 }}>
          No tienes firmas pendientes.
        </Alert>
      )}
      {pendientes.data && pendientes.data.length > 0 && (
        <TableContainer sx={{ my: 2 }}>
          <Table aria-label="Firmas pendientes" size="small">
            <TableHead>
              <TableRow>
                <TableCell>Periodo</TableCell>
                <TableCell>Versión</TableCell>
                <TableCell>Paso</TableCell>
                <TableCell>Generado el</TableCell>
                <TableCell>
                  <Box component="span" sx={visuallyHidden}>
                    Acción
                  </Box>
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {pendientes.data.map((pendiente) => (
                <TableRow key={pendiente.documento}>
                  <TableCell>{pendiente.periodo}</TableCell>
                  <TableCell>{pendiente.version}</TableCell>
                  <TableCell>{pendiente.rotulo}</TableCell>
                  <TableCell>{fecha(pendiente.generadoEn)}</TableCell>
                  <TableCell>
                    <Button
                      variant="contained"
                      disabled={descargando === pendiente.documento}
                      aria-label={`Descargar el PDF de ${nombreDeLaFila(pendiente)}`}
                      onClick={() => void descargar(pendiente)}
                    >
                      Descargar el PDF
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </>
  );
}
