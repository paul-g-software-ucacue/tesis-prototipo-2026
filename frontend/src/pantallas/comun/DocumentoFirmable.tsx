// El documento firmable en la tarjeta del flujo (F13-H02, FR-014, FR-015, contracts/spa.md):
// generado con la huella del PDF, generándose, o «El PDF no se pudo generar» con el motivo y, para
// el rol de la etapa, «Generar el PDF». Sin descarga: es de F14-H01.
import Alert from '@mui/material/Alert';
import AlertTitle from '@mui/material/AlertTitle';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { CLAVE_DOCUMENTO, generarDocumentoFirmable, useDocumentoFirmable } from './documento';
import { HuellaCopiable } from './HuellaCopiable';

const MOMENTO = new Intl.DateTimeFormat('es-EC', {
  dateStyle: 'short',
  timeStyle: 'short',
  hourCycle: 'h23',
  timeZone: 'America/Guayaquil',
});

/**
 * @param periodo el periodo, con el distributivo en CONGELADO_EN_FIRMA
 * @param intervalo cada cuánto vuelve a consultar mientras se genera, en milisegundos
 */
export function DocumentoFirmable({ periodo, intervalo }: { periodo: string; intervalo?: number }) {
  const documento = useDocumentoFirmable(periodo, intervalo);
  const consultas = useQueryClient();
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [enviando, setEnviando] = useState(false);

  if (documento.error) {
    return <AvisoDeError mensaje={traducirError(documento.error)} />;
  }
  if (!documento.data) {
    return null;
  }
  const { estado, motivo, acciones } = documento.data;

  const generar = async () => {
    setAviso(null);
    setEnviando(true);
    try {
      await generarDocumentoFirmable(periodo);
      await consultas.invalidateQueries({ queryKey: CLAVE_DOCUMENTO });
    } catch (fallo) {
      setAviso(traducirError(fallo));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Box sx={{ mt: 1, mb: 1 }}>
      {estado === 'GENERADO' && documento.data.documento && (
        <Box sx={{ pl: 1, borderLeft: 3, borderColor: 'success.main' }}>
          <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
            {`Documento firmable generado el ${MOMENTO.format(new Date(documento.data.documento.generadoEn))}`}
          </Typography>
          <HuellaCopiable etiqueta="Huella del PDF" hash={documento.data.documento.sha256} />
        </Box>
      )}
      {(estado === 'EN_CURSO' || estado === 'SIN_GENERAR') && (
        <Typography variant="body2" role="status">
          Generando el documento firmable…
        </Typography>
      )}
      {estado === 'FALLIDA' && (
        <Alert severity="error" role="alert">
          <AlertTitle>El PDF no se pudo generar</AlertTitle>
          {motivo}
        </Alert>
      )}
      {aviso && (
        <AvisoDeError mensaje={aviso}>
          {aviso.detalles.map((detalle) => (
            <Typography variant="body2" key={detalle.campo + detalle.descripcion}>
              {detalle.descripcion}
            </Typography>
          ))}
        </AvisoDeError>
      )}
      {acciones.includes('GENERAR') && (
        <Button
          variant="contained"
          sx={{ mt: 1 }}
          disabled={enviando}
          onClick={() => void generar()}
        >
          Generar el PDF
        </Button>
      )}
    </Box>
  );
}
