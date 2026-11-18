// El documento firmable en la tarjeta del flujo (F13-H02, FR-014, FR-015, contracts/spa.md):
// generado con la huella del PDF, generándose, o «El PDF no se pudo generar» con el motivo y, para
// el rol de la etapa, «Generar el PDF». Sin descarga: es de F14-H01. Desde F16-H02 (D-110), la
// cadena de firmas con el estado de cada paso, dicho con texto y no solo con el resaltado.
import Alert from '@mui/material/Alert';
import AlertTitle from '@mui/material/AlertTitle';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import Typography from '@mui/material/Typography';
import { useQueryClient } from '@tanstack/react-query';
import { useId, useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import {
  CLAVE_DOCUMENTO,
  generarDocumentoFirmable,
  useDocumentoFirmable,
  type EstadoDelDocumento,
} from './documento';
import { HuellaCopiable } from './HuellaCopiable';

const MOMENTO = new Intl.DateTimeFormat('es-EC', {
  dateStyle: 'short',
  timeStyle: 'short',
  hourCycle: 'h23',
  timeZone: 'America/Guayaquil',
});

type Paso = EstadoDelDocumento['cadena'][number];

function estadoDelPaso(paso: Paso) {
  switch (paso.estado) {
    case 'FIRMADO':
      return paso.firmadoEn ? `Firmado el ${MOMENTO.format(new Date(paso.firmadoEn))}` : 'Firmado';
    case 'EN_TURNO':
      return 'En turno';
    case 'RECHAZADO':
      return 'Rechazado';
    default:
      return 'Pendiente';
  }
}

/** La cadena de firmas: cada paso, su firmante impreso y su estado (F16-H02, contracts/spa.md). */
function CadenaDeFirmas({ cadena }: { cadena: Paso[] }) {
  const idDelTitulo = useId();
  return (
    <Box sx={{ mt: 1 }}>
      <Typography id={idDelTitulo} component="h3" variant="subtitle2">
        Cadena de firmas
      </Typography>
      <List aria-labelledby={idDelTitulo} dense disablePadding>
        {cadena.map((paso) => (
          <ListItem key={paso.orden} disableGutters>
            <ListItemText
              primary={[`F${paso.orden}`, paso.rotulo, paso.firmante].filter(Boolean).join(' · ')}
              secondary={estadoDelPaso(paso)}
              slotProps={{
                primary: { sx: { fontWeight: paso.estado === 'EN_TURNO' ? 700 : 'normal' } },
              }}
            />
          </ListItem>
        ))}
      </List>
    </Box>
  );
}

/**
 * @param periodo el periodo, con el distributivo en CONGELADO_EN_FIRMA
 * @param intervalo cada cuánto vuelve a consultar mientras se genera, en milisegundos
 * @param version la versión congelada; el documento de otra versión todavía no es el suyo
 *   (F12-H04), así que se muestra como generándose
 */
export function DocumentoFirmable({
  periodo,
  intervalo,
  version,
}: {
  periodo: string;
  intervalo?: number;
  version?: number | undefined;
}) {
  const documento = useDocumentoFirmable(periodo, intervalo, version);
  const consultas = useQueryClient();
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [enviando, setEnviando] = useState(false);

  if (documento.error) {
    return <AvisoDeError mensaje={traducirError(documento.error)} />;
  }
  if (!documento.data) {
    return null;
  }
  // F12-H04: el documento de otra versión, el de la anulada, todavía no es el de esta.
  const deOtraVersion = version !== undefined && documento.data.version !== version;
  const { motivo } = documento.data;
  const estado = deOtraVersion ? 'SIN_GENERAR' : documento.data.estado;
  const acciones = deOtraVersion ? [] : documento.data.acciones;

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
          {documento.data.cadena.length > 0 && <CadenaDeFirmas cadena={documento.data.cadena} />}
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
