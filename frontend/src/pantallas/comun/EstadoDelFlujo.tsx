// El estado del flujo del distributivo (F12-H02, RF-34, contracts/spa.md): la etapa actual con su
// versión, el envío de cada parcial y un botón por cada acción que la API dice que el usuario puede
// hacer. Lo usan «Oferta del periodo» (el coordinador envía su parcial), «Consolidado del periodo»
// (el Responsable y el Vicerrector ven la etapa; desde F10-H03, el Responsable envía a revisión y ve
// lo que falta; desde F12-H05, devuelve parciales, y el coordinador ve la observación) y «Proyectos» (el Gestor de unidad avanza BORRADOR_UNIDADES). El servidor valida el
// permiso, el rol de la etapa, la carrera y las condiciones.
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useQueryClient } from '@tanstack/react-query';
import { useId, useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { FalloDeApi, traducirError, type MensajeDeError } from '../../errores/traducir';
import { useCarreras } from '../carreras/consultas';
import { DialogoConfirmar } from '../carreras/DialogosDeCarrera';
import { DialogoDeDevolucion } from './DialogoDeDevolucion';
import {
  CLAVE_FLUJO,
  avanzarEtapa,
  enviarARevision,
  enviarParcial,
  useFlujo,
  type EstadoDelFlujo,
  type ParcialDelFlujo,
} from './flujo';

const MOMENTO = new Intl.DateTimeFormat('es-EC', {
  dateStyle: 'short',
  timeStyle: 'short',
  hourCycle: 'h23',
  timeZone: 'America/Guayaquil',
});

// La carrera con su nombre, como en la tabla del consolidado; sin permiso para leerlo, el código.
function estadoDe(parcial: ParcialDelFlujo, nombre: string) {
  if (parcial.devolucion) {
    const { por, en } = parcial.devolucion;
    return `${nombre}: devuelto por ${por} el ${MOMENTO.format(new Date(en))}`;
  }
  if (parcial.enviado) {
    const cuando = parcial.enviadoEn ? ` el ${MOMENTO.format(new Date(parcial.enviadoEn))}` : '';
    return `${nombre}: enviado${parcial.enviadoPor ? ` por ${parcial.enviadoPor}` : ''}${cuando}`;
  }
  return parcial.pendienteDeConfirmar
    ? `${nombre}: enviado, falta confirmar`
    : `${nombre}: pendiente`;
}

type Confirmar = 'enviar' | 'avanzar' | 'revisar' | 'devolver' | null;

/**
 * El estado del flujo del periodo. Sin distributivo no muestra nada: las pantallas ya dicen cómo
 * crearlo.
 *
 * @param carrera la carrera cuyo parcial envía el usuario; solo la usa la acción de enviar
 * @param alCambiar se llama después de enviar o avanzar, para recargar la pantalla
 * @param faltan lo que le falta al consolidado para enviarlo a revisión; con algo, el botón queda
 *   deshabilitado y lo lista
 */
export function EstadoDelFlujoDelPeriodo({
  periodo,
  carrera,
  alCambiar,
  soloEn,
  faltan = [],
}: {
  periodo: string | undefined;
  carrera?: string | undefined;
  alCambiar?: () => void;
  /** Si llega, solo se muestra en esas etapas (en Proyectos, BORRADOR_UNIDADES). */
  soloEn?: readonly EstadoDelFlujo['etapa'][];
  faltan?: readonly string[] | undefined;
}) {
  const flujo = useFlujo(periodo);
  const carreras = useCarreras();
  const consultas = useQueryClient();
  const [confirmar, setConfirmar] = useState<Confirmar>(null);
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [enviando, setEnviando] = useState(false);
  const idFaltan = useId();

  if (flujo.error) {
    // Sin distributivo, nada que mostrar; otro fallo, el aviso común.
    return flujo.error instanceof FalloDeApi && flujo.error.estado === 404 ? null : (
      <AvisoDeError mensaje={traducirError(flujo.error)} />
    );
  }
  if (!flujo.data || !periodo) {
    return null;
  }
  const { etapa, nombreDeEtapa, version, parciales, acciones } = flujo.data;
  if (soloEn && !soloEn.includes(etapa)) {
    return null;
  }
  const propio = parciales.find((uno) => uno.carrera === carrera);
  const nombreDe = (codigo: string) =>
    carreras.data?.find((una) => una.codigo === codigo)?.nombre ?? codigo;
  const puedeEnviar = acciones.includes('ENVIAR_PARCIAL') && Boolean(carrera);
  const puedeAvanzar = acciones.includes('AVANZAR');
  const puedeRevisar = acciones.includes('ENVIAR_A_REVISION');
  const puedeDevolver = acciones.includes('DEVOLVER_PARCIALES');

  const ejecutar = async (accion: () => Promise<unknown>) => {
    setConfirmar(null);
    setAviso(null);
    setEnviando(true);
    try {
      await accion();
      await consultas.invalidateQueries({ queryKey: CLAVE_FLUJO });
      alCambiar?.();
    } catch (fallo) {
      setAviso(traducirError(fallo));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Box
      component="section"
      role="region"
      aria-label="Estado del distributivo"
      sx={{ my: 2, p: 2, border: 1, borderColor: 'divider', borderRadius: 1 }}
    >
      <Typography component="h2" variant="h6">
        {`${nombreDeEtapa} · versión ${String(version)}`}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
        {`Etapa ${etapa}`}
      </Typography>
      <Box component="ul" sx={{ m: 0, pl: 3 }}>
        {parciales.map((parcial) => (
          <Typography component="li" variant="body2" key={parcial.carrera}>
            {estadoDe(parcial, nombreDe(parcial.carrera))}
            {parcial.devolucion && (
              <Box
                component="span"
                sx={{
                  display: 'block',
                  mt: 0.5,
                  pl: 1,
                  borderLeft: 3,
                  borderColor: 'warning.main',
                }}
              >
                <Box component="span" sx={{ display: 'block', fontWeight: 'bold' }}>
                  Observación
                </Box>
                <Box component="span" sx={{ display: 'block', whiteSpace: 'pre-line' }}>
                  {parcial.devolucion.observacion}
                </Box>
              </Box>
            )}
          </Typography>
        ))}
      </Box>
      {aviso && (
        <AvisoDeError mensaje={aviso}>
          {aviso.detalles.map((detalle) => (
            <Typography variant="body2" key={detalle.campo + detalle.descripcion}>
              {detalle.descripcion}
            </Typography>
          ))}
        </AvisoDeError>
      )}
      <Stack direction="row" spacing={2} sx={{ mt: 1 }}>
        {puedeEnviar && (
          <Button variant="contained" disabled={enviando} onClick={() => setConfirmar('enviar')}>
            {propio?.pendienteDeConfirmar || propio?.devolucion
              ? 'Reenviar parcial'
              : 'Enviar parcial'}
          </Button>
        )}
        {puedeAvanzar && (
          <Button variant="contained" disabled={enviando} onClick={() => setConfirmar('avanzar')}>
            Avanzar a la siguiente etapa
          </Button>
        )}
        {puedeRevisar && (
          <Button
            variant="contained"
            disabled={enviando || faltan.length > 0}
            aria-describedby={faltan.length > 0 ? idFaltan : undefined}
            onClick={() => setConfirmar('revisar')}
          >
            Enviar a revisión
          </Button>
        )}
        {puedeDevolver && (
          <Button variant="outlined" disabled={enviando} onClick={() => setConfirmar('devolver')}>
            Devolver parciales
          </Button>
        )}
      </Stack>
      {puedeRevisar && faltan.length > 0 && (
        <Box
          component="ul"
          id={idFaltan}
          aria-label="Falta para enviar a revisión"
          sx={{ mt: 1, mb: 0, pl: 3 }}
        >
          {faltan.map((falta) => (
            <Typography component="li" variant="body2" key={falta}>
              {falta}
            </Typography>
          ))}
        </Box>
      )}
      {confirmar === 'enviar' && carrera && (
        <DialogoConfirmar
          titulo={`Enviar el parcial de ${nombreDe(carrera)}`}
          texto="Después de enviarlo, no podrás cambiar las propuestas ni las actividades de tu carrera."
          accion="Enviar"
          alCerrar={() => setConfirmar(null)}
          alConfirmar={() => void ejecutar(() => enviarParcial(periodo, carrera))}
        />
      )}
      {confirmar === 'revisar' && (
        <DialogoConfirmar
          titulo="Enviar a revisión"
          texto="El distributivo pasa a revisión del Vicerrectorado. Ya no se podrá consolidar ni justificar en esta etapa."
          accion="Enviar"
          alCerrar={() => setConfirmar(null)}
          alConfirmar={() => void ejecutar(() => enviarARevision(periodo))}
        />
      )}
      {confirmar === 'devolver' && (
        <DialogoDeDevolucion
          periodo={periodo}
          carreras={parciales
            .filter((uno) => uno.enviado)
            .map((uno) => ({ codigo: uno.carrera, nombre: nombreDe(uno.carrera) }))}
          etapaActual={nombreDeEtapa}
          alCerrar={() => setConfirmar(null)}
          alDevolver={async () => {
            setConfirmar(null);
            await consultas.invalidateQueries({ queryKey: CLAVE_FLUJO });
            alCambiar?.();
          }}
        />
      )}
      {confirmar === 'avanzar' && (
        <DialogoConfirmar
          titulo={`Avanzar desde ${nombreDeEtapa}`}
          texto="El distributivo pasa a la siguiente etapa activa del flujo."
          accion="Avanzar"
          alCerrar={() => setConfirmar(null)}
          alConfirmar={() => void ejecutar(() => avanzarEtapa(periodo))}
        />
      )}
    </Box>
  );
}
