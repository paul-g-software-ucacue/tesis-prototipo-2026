// La pantalla «Pendientes de firma» (F14-H01, RF-38, D-102): los documentos que le toca firmar al
// usuario, en turno según el servidor, y la descarga del PDF vigente para firmarlo con FirmaEC.
// Desde F14-H02 (RF-39, D-103), la subida del PDF firmado con el resultado de cada comprobación.
// Desde F14-H04 (RF-36, D-105), el rechazo en firma con una observación.
import Alert from '@mui/material/Alert';
import AlertTitle from '@mui/material/AlertTitle';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { useQueryClient } from '@tanstack/react-query';
import { useId, useRef, useState, type ChangeEvent } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { FalloDeApi, traducirError, type MensajeDeError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import {
  CLAVE_PENDIENTES,
  descargarArchivoVigente,
  subirFirma,
  useFirmasPendientes,
  type FirmaPendiente,
  type RechazoEnFirma,
  type ResultadoValidacion,
} from './consultas';
import { DialogoDeRechazo } from './DialogoDeRechazo';

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

// Los nombres de las comprobaciones de la validación básica, en su orden (D-103, D-104).
const COMPROBACIONES: Record<string, string> = {
  MISMO_DOCUMENTO: 'Es la versión vigente',
  UNA_FIRMA_NUEVA: 'Tiene una sola firma nueva',
  INTEGRIDAD: 'La firma es íntegra',
  CONFIANZA: 'El certificado es de confianza',
  // F14-H03 (D-104).
  VIGENCIA: 'El certificado está vigente',
  ORDEN: 'La firma está en su orden',
  FIRMANTE: 'Firma la persona del paso',
};

const RESULTADOS: Record<string, string> = {
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
  NO_EVALUADA: 'No evaluada',
};

type Aviso =
  | { tipo: 'aceptada'; resultado: ResultadoValidacion }
  | { tipo: 'rechazada'; mensaje: MensajeDeError }
  | { tipo: 'rechazo'; rechazo: RechazoEnFirma };

function fecha(instante: string) {
  return new Date(instante).toLocaleString('es-EC', {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
}

function nombreDeLaFila(pendiente: FirmaPendiente) {
  return `${pendiente.periodo}, versión ${String(pendiente.version)}`;
}

/** Las cuatro comprobaciones, cada una con su nombre y su resultado. */
function Comprobaciones({ filas }: { filas: { campo: string; resultado: string }[] }) {
  return (
    <Box component="ul" sx={{ my: 1, pl: 3 }}>
      {filas.map((fila) => (
        <li key={fila.campo}>
          <Typography component="span" variant="body2" sx={{ fontWeight: 'bold' }}>
            {COMPROBACIONES[fila.campo] ?? fila.campo}
          </Typography>
          {': '}
          <Typography component="span" variant="body2">
            {fila.resultado}
          </Typography>
        </li>
      ))}
    </Box>
  );
}

function FirmaRegistrada({
  resultado,
  alCerrar,
}: {
  resultado: ResultadoValidacion;
  alCerrar: () => void;
}) {
  const titulo = useId();
  return (
    <Alert
      severity="success"
      role="status"
      aria-labelledby={titulo}
      onClose={alCerrar}
      sx={{ my: 2 }}
    >
      <AlertTitle id={titulo}>Firma registrada</AlertTitle>
      El PDF firmado pasó la validación y es ahora el archivo vigente del documento.
      <Comprobaciones
        filas={resultado.comprobaciones.map((una) => ({
          campo: una.comprobacion,
          resultado: RESULTADOS[una.resultado] + (una.motivo ? `. ${una.motivo}` : ''),
        }))}
      />
      {resultado.certificado && (
        <Typography variant="body2">
          {`Certificado de ${resultado.certificado.nombre}, emitido por ${resultado.certificado.emisor}, vigente hasta el ${fecha(resultado.certificado.validoHasta)}`}
        </Typography>
      )}
    </Alert>
  );
}

function RechazoRegistrado({
  rechazo,
  alCerrar,
}: {
  rechazo: RechazoEnFirma;
  alCerrar: () => void;
}) {
  const titulo = useId();
  return (
    <Alert severity="info" role="status" aria-labelledby={titulo} onClose={alCerrar} sx={{ my: 2 }}>
      <AlertTitle id={titulo}>Rechazo registrado</AlertTitle>
      <Typography variant="body2">
        {`${rechazo.rotulo}, ${rechazo.periodo}, versión ${String(rechazo.version)}: el documento vuelve a la revisión del Vicerrectorado con tu observación.`}
      </Typography>
      <Typography variant="body2" sx={{ mt: 1, fontStyle: 'italic' }}>
        {rechazo.observacion}
      </Typography>
    </Alert>
  );
}

function FilaPendiente({
  pendiente,
  ocupada,
  alDescargar,
  alSubir,
  alRechazar,
}: {
  pendiente: FirmaPendiente;
  ocupada: boolean;
  alDescargar: () => void;
  alSubir: (archivo: File) => void;
  alRechazar: () => void;
}) {
  const selector = useRef<HTMLInputElement>(null);
  const elegir = (evento: ChangeEvent<HTMLInputElement>) => {
    const archivo = evento.target.files?.[0];
    evento.target.value = '';
    if (archivo) {
      alSubir(archivo);
    }
  };
  return (
    <TableRow>
      <TableCell>{pendiente.periodo}</TableCell>
      <TableCell>{pendiente.version}</TableCell>
      <TableCell>{pendiente.rotulo}</TableCell>
      <TableCell>{fecha(pendiente.generadoEn)}</TableCell>
      <TableCell>
        <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
          <Button
            variant="contained"
            disabled={ocupada}
            aria-label={`Descargar el PDF de ${nombreDeLaFila(pendiente)}`}
            onClick={alDescargar}
          >
            Descargar el PDF
          </Button>
          <Button
            variant="outlined"
            disabled={ocupada}
            aria-label={`Subir el PDF firmado de ${nombreDeLaFila(pendiente)}`}
            onClick={() => selector.current?.click()}
          >
            Subir el PDF firmado
          </Button>
          <Button
            variant="outlined"
            color="error"
            disabled={ocupada}
            aria-label={`Rechazar en firma ${nombreDeLaFila(pendiente)}`}
            onClick={alRechazar}
          >
            Rechazar
          </Button>
          <input
            ref={selector}
            type="file"
            hidden
            accept="application/pdf,.pdf"
            aria-label={`Archivo firmado de ${nombreDeLaFila(pendiente)}`}
            onChange={elegir}
          />
        </Stack>
      </TableCell>
    </TableRow>
  );
}

export function FirmasPendientes() {
  useTitulo('Pendientes de firma');
  const pendientes = useFirmasPendientes();
  const consultas = useQueryClient();
  const [ocupado, setOcupado] = useState<string | null>(null);
  const [fallo, setFallo] = useState<MensajeDeError | null>(null);
  const [aviso, setAviso] = useState<Aviso | null>(null);
  const [aRechazar, setARechazar] = useState<FirmaPendiente | null>(null);

  const rechazado = async (rechazo: RechazoEnFirma) => {
    setARechazar(null);
    setFallo(null);
    setAviso({ tipo: 'rechazo', rechazo });
    await consultas.invalidateQueries({ queryKey: CLAVE_PENDIENTES });
  };

  // Un 403 de quien ya no está en turno, u otro fallo: se cierra y se vuelve a pedir la lista.
  const rechazoFallido = async (error: unknown) => {
    setARechazar(null);
    setAviso(null);
    setFallo(traducirError(error));
    await consultas.invalidateQueries({ queryKey: CLAVE_PENDIENTES });
  };

  const descargar = async (pendiente: FirmaPendiente) => {
    setFallo(null);
    setOcupado(pendiente.documento);
    try {
      await descargarArchivoVigente(pendiente);
    } catch (error) {
      setFallo(traducirError(error));
    } finally {
      setOcupado(null);
    }
  };

  const subir = async (pendiente: FirmaPendiente, archivo: File) => {
    setFallo(null);
    setAviso(null);
    setOcupado(pendiente.documento);
    try {
      const resultado = await subirFirma(pendiente.documento, archivo);
      setAviso({ tipo: 'aceptada', resultado });
      await consultas.invalidateQueries({ queryKey: CLAVE_PENDIENTES });
    } catch (error) {
      if (error instanceof FalloDeApi && error.estado === 422) {
        setAviso({ tipo: 'rechazada', mensaje: traducirError(error) });
      } else {
        setFallo(traducirError(error));
      }
    } finally {
      setOcupado(null);
    }
  };

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Pendientes de firma
      </Typography>
      {pendientes.error && <AvisoDeError mensaje={traducirError(pendientes.error)} />}
      {fallo && <AvisoDeError mensaje={fallo} />}
      {aviso?.tipo === 'aceptada' && (
        <FirmaRegistrada resultado={aviso.resultado} alCerrar={() => setAviso(null)} />
      )}
      {aviso?.tipo === 'rechazo' && (
        <RechazoRegistrado rechazo={aviso.rechazo} alCerrar={() => setAviso(null)} />
      )}
      {aviso?.tipo === 'rechazada' && (
        <AvisoDeError mensaje={aviso.mensaje}>
          <Comprobaciones
            filas={aviso.mensaje.detalles.map((detalle) => ({
              campo: detalle.campo,
              resultado: detalle.descripcion,
            }))}
          />
        </AvisoDeError>
      )}
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
                    Acciones
                  </Box>
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {pendientes.data.map((pendiente) => (
                <FilaPendiente
                  key={pendiente.documento}
                  pendiente={pendiente}
                  ocupada={ocupado === pendiente.documento}
                  alDescargar={() => void descargar(pendiente)}
                  alSubir={(archivo) => void subir(pendiente, archivo)}
                  alRechazar={() => {
                    setFallo(null);
                    setAviso(null);
                    setARechazar(pendiente);
                  }}
                />
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
      {aRechazar && (
        <DialogoDeRechazo
          pendiente={aRechazar}
          alCerrar={() => setARechazar(null)}
          alRechazar={rechazado}
          alFallar={rechazoFallido}
        />
      )}
    </>
  );
}
