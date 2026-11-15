// La pantalla «Consolidado del periodo» (F10-H02, RF-32): todas las carreras con su estado, los
// totales institucionales, si sigue al día y el informe sustentado. Consolidar marca y no bloquea:
// las carreras siguen editando. La leen Consolidación y el Vicerrectorado; escribe Consolidación.
// Desde F10-H03, el Responsable la envía a revisión y ve lo que le falta (D-97).
import Alert from '@mui/material/Alert';
import AlertTitle from '@mui/material/AlertTitle';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useQueryClient } from '@tanstack/react-query';
import { useRef, useState, type ChangeEvent } from 'react';
import { Link as RouterLink } from 'react-router';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import { EstadoDelFlujoDelPeriodo } from '../comun/EstadoDelFlujo';
import { ESCRITURA_DEL_PANEL } from '../../sesion/funcionalidades';
import { tieneAlguno } from '../../sesion/permisos';
import { useSesionActual } from '../../sesion/useSesionActual';
import { useCarreras } from '../carreras/consultas';
import { horas } from '../carreras/nombres';
import { porComponente } from '../comun/cargaHoraria';
import { SELECCION } from '../comun/formulario';
import { useEnvio } from '../comun/useEnvio';
import { usePeriodos } from '../periodos/consultas';
import { NOMBRE_DEL_ESTADO } from '../periodos/estados';
import {
  adjuntarInforme,
  CLAVE_CONSOLIDADO,
  consolidar,
  descargarInforme,
  INFORME_DEMASIADO_GRANDE,
  MAXIMO_DEL_INFORME,
  useConsolidado,
  type CarreraConsolidada,
  type ConsolidadoDelPeriodo,
  type InformeSustentado,
} from './consultas';

const ESTADO_DEL_PARCIAL: Record<CarreraConsolidada['estado'], string> = {
  EN_ELABORACION: 'En elaboración',
  ENVIADO: 'Enviado',
  DEVUELTO: 'Devuelto',
};

const UN_MB = 1024 * 1024;

/**
 * Lo que le falta al consolidado para enviarlo a revisión, en el orden de D-97 (F10-H03). El
 * servidor lo vuelve a comprobar al enviar.
 */
function faltanParaRevision(consolidado: ConsolidadoDelPeriodo): string[] {
  const faltan: string[] = [];
  if (consolidado.carreras.some((carrera) => carrera.estado !== 'ENVIADO')) {
    faltan.push('Hay parciales sin enviar: su carrera tiene que enviarlos.');
  }
  if (!consolidado.consolidacion) {
    faltan.push('Consolida el periodo.');
  } else if (consolidado.desactualizado) {
    faltan.push('Consolida otra vez: algo cambió después de consolidar.');
  }
  if (!consolidado.informe) {
    faltan.push('Adjunta el informe sustentado.');
  }
  const abiertas = consolidado.abiertas.duras + consolidado.abiertas.blandas;
  if (abiertas > 0) {
    faltan.push(
      `${abiertas === 1 ? 'Queda 1 inconsistencia abierta' : `Quedan ${String(abiertas)} inconsistencias abiertas`}: corrige las duras y justifica las blandas en el panel.`,
    );
  }
  return faltan;
}

function fecha(iso: string) {
  return new Date(iso).toLocaleString('es-EC');
}

function tamano(bytes: number) {
  const mb = Math.round((bytes / UN_MB) * 10) / 10;
  return `${String(mb).replace('.', ',')} MB`;
}

/** Una frase como «3 duras y 1 blanda abiertas». */
function abiertas({ duras, blandas }: ConsolidadoDelPeriodo['abiertas']) {
  const parte = (cuantas: number, singular: string, plural: string) =>
    `${String(cuantas)} ${cuantas === 1 ? singular : plural}`;
  return `${parte(duras, 'dura', 'duras')} y ${parte(blandas, 'blanda', 'blandas')} abiertas`;
}

function Carreras({
  consolidado,
  nombreDe,
}: {
  consolidado: ConsolidadoDelPeriodo;
  nombreDe: (codigo: string) => string;
}) {
  const columnas = porComponente(consolidado.totales).map(([nombre]) => nombre);
  return (
    <TableContainer sx={{ mb: 3 }}>
      <Table aria-label="Carreras del consolidado" size="small">
        <TableHead>
          <TableRow>
            <TableCell>Carrera</TableCell>
            <TableCell>Estado</TableCell>
            <TableCell>Paralelos con propuesta</TableCell>
            {columnas.map((columna) => (
              <TableCell key={columna} align="right">
                {columna}
              </TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {consolidado.carreras.map((una) => (
            <TableRow key={una.carrera}>
              <TableCell>{nombreDe(una.carrera)}</TableCell>
              <TableCell>{ESTADO_DEL_PARCIAL[una.estado]}</TableCell>
              <TableCell>{`${String(una.propuestos)} de ${String(una.paralelos)}`}</TableCell>
              {porComponente(una.horas).map(([nombre, valor]) => (
                <TableCell key={nombre} align="right">
                  {`${horas(valor)} h`}
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

function Totales({ consolidado }: { consolidado: ConsolidadoDelPeriodo }) {
  return (
    <TableContainer sx={{ mb: 2, maxWidth: 420 }}>
      <Table aria-label="Totales institucionales" size="small">
        <TableBody>
          {porComponente(consolidado.totales).map(([nombre, valor]) => (
            <TableRow key={nombre}>
              <TableCell>{nombre}</TableCell>
              <TableCell align="right">{`${horas(valor)} h`}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

function Informe({
  periodo,
  informe,
  puedeAdjuntar,
  consolidado,
  alGuardar,
}: {
  periodo: string;
  informe: InformeSustentado | undefined;
  puedeAdjuntar: boolean;
  consolidado: boolean;
  alGuardar: () => Promise<void>;
}) {
  const { aviso, enviando, enviar } = useEnvio(alGuardar);
  const [local, setLocal] = useState<MensajeDeError | null>(null);
  const selector = useRef<HTMLInputElement>(null);
  const accion = informe ? 'Reemplazar el informe' : 'Adjuntar el informe';

  const elegir = (evento: ChangeEvent<HTMLInputElement>) => {
    const archivo = evento.target.files?.[0];
    // Sin esto, elegir otra vez el mismo archivo no avisaría nada.
    evento.target.value = '';
    if (!archivo) {
      return;
    }
    if (archivo.size > MAXIMO_DEL_INFORME) {
      setLocal(INFORME_DEMASIADO_GRANDE);
      return;
    }
    setLocal(null);
    enviar(() => adjuntarInforme(periodo, archivo));
  };

  const descargar = () => {
    setLocal(null);
    void descargarInforme(periodo).catch((fallo: unknown) => setLocal(traducirError(fallo)));
  };

  const mensaje = local ?? aviso;
  return (
    <>
      <Typography component="h2" variant="h5" sx={{ mt: 2 }} gutterBottom>
        Informe sustentado
      </Typography>
      {mensaje && <AvisoDeError mensaje={mensaje} />}
      {informe ? (
        <>
          <Typography>{informe.nombre}</Typography>
          <Typography variant="body2" color="text.secondary">
            {`${tamano(informe.tamano)} · ${informe.por} · ${fecha(informe.en)}`}
          </Typography>
        </>
      ) : (
        <Typography color="text.secondary">
          {consolidado
            ? 'Todavía no tiene informe.'
            : 'Primero consolida el periodo, y después adjunta el informe.'}
        </Typography>
      )}
      <Stack direction="row" spacing={1} sx={{ mt: 1, flexWrap: 'wrap' }}>
        {informe && <Button onClick={descargar}>Descargar el informe</Button>}
        {puedeAdjuntar && consolidado && (
          <>
            <Button disabled={enviando} onClick={() => selector.current?.click()}>
              {accion}
            </Button>
            <input
              ref={selector}
              type="file"
              hidden
              accept="application/pdf,.pdf"
              aria-label={accion}
              onChange={elegir}
            />
          </>
        )}
      </Stack>
    </>
  );
}

export function Consolidado() {
  useTitulo('Consolidado del periodo');
  const sesion = useSesionActual();
  const consultas = useQueryClient();
  const periodos = usePeriodos();
  const carreras = useCarreras();
  const [elegido, setElegido] = useState<string | null>(null);
  // Por omisión, el más reciente en Planificación; si no hay, el más reciente.
  const porOmision =
    periodos.data?.find((periodo) => periodo.estado === 'PLANIFICACION')?.codigo ??
    periodos.data?.[0]?.codigo;
  const periodo = elegido ?? porOmision;
  const consulta = useConsolidado(periodo);
  const recargar = () => consultas.invalidateQueries({ queryKey: CLAVE_CONSOLIDADO });
  const { aviso, enviando, enviar } = useEnvio(recargar);

  const datos = consulta.data;
  const escribe = tieneAlguno(sesion.data?.permisos, ESCRITURA_DEL_PANEL);
  // #215, D-99: desde la revisión del Vicerrectorado, el consolidado ya no cambia; F13-H01, D-100:
  // tampoco con la versión congelada.
  const puedeEscribir =
    escribe &&
    Boolean(datos?.creado && datos.modificable && !datos.enRevision && !datos.versionCongelada);
  const nombreDe = (codigo: string) =>
    carreras.data?.find((carrera) => carrera.codigo === codigo)?.nombre ?? codigo;

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Consolidado del periodo
      </Typography>
      {periodos.error && <AvisoDeError mensaje={traducirError(periodos.error)} />}
      {consulta.error && <AvisoDeError mensaje={traducirError(consulta.error)} />}
      {aviso && <AvisoDeError mensaje={aviso} />}
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
      {datos && !datos.creado && (
        <Alert severity="info" role="status" sx={{ my: 2 }}>
          Este periodo todavía no tiene distributivo: no hay nada que consolidar.
        </Alert>
      )}
      {datos?.creado && periodo && (
        <EstadoDelFlujoDelPeriodo
          periodo={periodo}
          faltan={faltanParaRevision(datos)}
          alCambiar={() => void recargar()}
        />
      )}
      {datos?.creado && periodo && (
        <>
          {/* F13-H01: el aviso de la versión congelada reemplaza al de la revisión (#215). */}
          {datos.versionCongelada ? (
            <Alert severity="info" role="status" sx={{ my: 2 }}>
              {`La versión ${String(datos.version ?? '')} está congelada para la firma: ya no cambia.`}
            </Alert>
          ) : (
            datos.enRevision && (
              <Alert severity="info" role="status" sx={{ my: 2 }}>
                El distributivo está en revisión del Vicerrectorado: el consolidado ya no cambia.
              </Alert>
            )
          )}
          <Typography sx={{ mt: 1 }}>
            {datos.consolidacion
              ? `Consolidado por ${datos.consolidacion.por} · ${fecha(datos.consolidacion.en)}`
              : 'Este periodo todavía no se consolida.'}
          </Typography>
          {datos.desactualizado && (
            <Alert
              severity="warning"
              role="status"
              aria-label="Consolidado desactualizado"
              sx={{ my: 2 }}
            >
              <AlertTitle>Consolidado desactualizado</AlertTitle>
              Cambió una propuesta o una actividad después de consolidar: vuelve a consolidar.
            </Alert>
          )}
          {puedeEscribir && (
            <Button
              variant="contained"
              disabled={enviando}
              sx={{ my: 1 }}
              onClick={() => enviar(() => consolidar(periodo))}
            >
              {datos.consolidacion ? 'Volver a consolidar' : 'Consolidar'}
            </Button>
          )}
          <Typography sx={{ my: 1 }}>
            {abiertas(datos.abiertas)}{' '}
            <RouterLink to="/inconsistencias">Ir al panel de inconsistencias</RouterLink>
          </Typography>
          <Typography component="h2" variant="h5" sx={{ mt: 2 }} gutterBottom>
            Carreras
          </Typography>
          <Carreras consolidado={datos} nombreDe={nombreDe} />
          <Typography component="h2" variant="h5" sx={{ mt: 2 }} gutterBottom>
            Totales institucionales
          </Typography>
          <Totales consolidado={datos} />
          <Typography variant="body2" color="text.secondary">
            Las unidades todavía no tienen parcial propio: su investigación, vinculación y gestión
            están en los parciales de las carreras.
          </Typography>
          <Informe
            periodo={periodo}
            informe={datos.informe}
            puedeAdjuntar={puedeEscribir}
            consolidado={Boolean(datos.consolidacion)}
            alGuardar={recargar}
          />
        </>
      )}
    </>
  );
}
