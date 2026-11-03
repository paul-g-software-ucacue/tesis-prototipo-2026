// La pantalla «Flujo y firmas» (F12-H01, RF-34, RF-58): lo que rige en cada periodo, sus etapas y
// su árbol de firmas. Se carga un archivo, no se edita aquí (ADR-007). La lee y la carga quien
// tiene FLUJO_FIRMAS; solo cambia mientras el periodo está en Planificación.
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
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import { ESCRITURA_DEL_FLUJO } from '../../sesion/funcionalidades';
import { tieneAlguno } from '../../sesion/permisos';
import { nombreDelRol } from '../../sesion/roles';
import { useSesionActual } from '../../sesion/useSesionActual';
import { SELECCION } from '../comun/formulario';
import { useEnvio } from '../comun/useEnvio';
import { usePeriodos } from '../periodos/consultas';
import { NOMBRE_DEL_ESTADO } from '../periodos/estados';
import {
  ARCHIVO_DEMASIADO_GRANDE,
  CLAVE_FLUJO,
  MAXIMO_DEL_ARCHIVO,
  cargarConfiguracion,
  descargarPlantilla,
  useConfiguracionDelFlujo,
  type ConfiguracionDelFlujo,
  type PasoDeFirma,
} from './consultas';

const ACCION = 'Cargar un archivo';

const NOMBRE_DEL_DOCUMENTO: Record<PasoDeFirma['documento'], string> = {
  CONSOLIDADO: 'Consolidado',
  PARCIAL_CARRERA: 'Parcial de carrera',
  RESOLUCION_OCS: 'Resolución del OCS',
};

function fecha(instante: string) {
  return new Date(instante).toLocaleString('es-EC', {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
}

function dias(plazo: number | undefined) {
  if (plazo === undefined) {
    return 'Sin plazo';
  }
  return plazo === 1 ? '1 día' : `${String(plazo)} días`;
}

function Etapas({ configuracion }: { configuracion: ConfiguracionDelFlujo }) {
  return (
    <TableContainer sx={{ mb: 3 }}>
      <Table aria-label="Etapas del flujo" size="small">
        <TableHead>
          <TableRow>
            <TableCell>Etapa</TableCell>
            <TableCell>Estado</TableCell>
            <TableCell>Tipo</TableCell>
            <TableCell>Rol responsable</TableCell>
            <TableCell>Plazo</TableCell>
            <TableCell>Admite devolución</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {configuracion.etapas.map((etapa) => (
            <TableRow key={etapa.tipo}>
              <TableCell>{etapa.nombre}</TableCell>
              <TableCell>{etapa.activa ? 'Activa' : 'Desactivada'}</TableCell>
              <TableCell>{etapa.esqueleto ? 'Esqueleto fijo' : 'Opcional'}</TableCell>
              <TableCell>{nombreDelRol(etapa.rol)}</TableCell>
              <TableCell>{dias(etapa.plazoDias)}</TableCell>
              <TableCell>{etapa.admiteDevolucion ? 'Sí' : 'No'}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

function Firmas({ configuracion }: { configuracion: ConfiguracionDelFlujo }) {
  const etapa = (tipo: PasoDeFirma['etapa']) =>
    configuracion.etapas.find((una) => una.tipo === tipo)?.nombre ?? tipo;
  return (
    <TableContainer sx={{ mb: 3 }}>
      <Table aria-label="Árbol de firmas" size="small">
        <TableHead>
          <TableRow>
            <TableCell>Orden</TableCell>
            <TableCell>Firma</TableCell>
            <TableCell>Rol</TableCell>
            <TableCell>Etapa</TableCell>
            <TableCell>Documento</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {configuracion.firmas.map((firma) => (
            <TableRow key={firma.orden}>
              <TableCell>{firma.orden}</TableCell>
              <TableCell>{firma.etiqueta}</TableCell>
              <TableCell>{nombreDelRol(firma.rol)}</TableCell>
              <TableCell>{etapa(firma.etapa)}</TableCell>
              <TableCell>{NOMBRE_DEL_DOCUMENTO[firma.documento]}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

/** Qué rige: la configuración por defecto, o el archivo con su versión, quién y cuándo. */
function QueRige({ configuracion }: { configuracion: ConfiguracionDelFlujo }) {
  if (configuracion.porDefecto) {
    return (
      <Alert severity="info" role="status" aria-label="Configuración por defecto" sx={{ my: 2 }}>
        <AlertTitle>Configuración por defecto</AlertTitle>
        Este periodo no tiene un archivo cargado: rige la configuración por defecto.
      </Alert>
    );
  }
  const { version, cargada } = configuracion;
  return (
    <Typography sx={{ my: 1 }}>
      {`Versión ${String(version)} · cargada por ${cargada?.por ?? ''} · ${
        cargada ? fecha(cargada.en) : ''
      }`}
    </Typography>
  );
}

/** Lo de un periodo. Va aparte, con su clave: un rechazo de un periodo no sigue en el siguiente. */
function DelPeriodo({ periodo }: { periodo: string }) {
  const sesion = useSesionActual();
  const consultas = useQueryClient();
  const [local, setLocal] = useState<MensajeDeError | null>(null);
  const selector = useRef<HTMLInputElement>(null);
  const consulta = useConfiguracionDelFlujo(periodo);
  const recargar = () => consultas.invalidateQueries({ queryKey: CLAVE_FLUJO });
  const { aviso, enviando, enviar } = useEnvio(recargar);

  const datos = consulta.data;
  const puedeCargar =
    tieneAlguno(sesion.data?.permisos, ESCRITURA_DEL_FLUJO) && Boolean(datos?.modificable);

  const elegir = (evento: ChangeEvent<HTMLInputElement>) => {
    const archivo = evento.target.files?.[0];
    // Sin esto, elegir otra vez el mismo archivo corregido no avisaría nada.
    evento.target.value = '';
    if (!archivo) {
      return;
    }
    if (archivo.size > MAXIMO_DEL_ARCHIVO) {
      setLocal(ARCHIVO_DEMASIADO_GRANDE);
      return;
    }
    setLocal(null);
    enviar(() => cargarConfiguracion(periodo, archivo));
  };

  const descargar = () => {
    setLocal(null);
    void descargarPlantilla().catch((fallo: unknown) => setLocal(traducirError(fallo)));
  };

  const mensaje = local ?? aviso;
  return (
    <>
      {consulta.error && <AvisoDeError mensaje={traducirError(consulta.error)} />}
      {mensaje && (
        <AvisoDeError mensaje={mensaje}>
          {mensaje.detalles.length > 0 && (
            <ul style={{ margin: '8px 0 0', paddingLeft: 20 }}>
              {mensaje.detalles.map((detalle, indice) => (
                <li key={`${String(indice)}-${detalle.descripcion}`}>{detalle.descripcion}</li>
              ))}
            </ul>
          )}
        </AvisoDeError>
      )}
      {datos && <QueRige configuracion={datos} />}
      {datos && !datos.modificable && (
        <Alert severity="info" role="status" sx={{ my: 2 }}>
          La configuración solo cambia mientras el periodo está en Planificación.
        </Alert>
      )}
      <Stack direction="row" spacing={1} sx={{ my: 1, flexWrap: 'wrap' }}>
        <Button onClick={descargar}>Descargar la plantilla</Button>
        {puedeCargar && (
          <>
            <Button
              variant="contained"
              disabled={enviando}
              onClick={() => selector.current?.click()}
            >
              {ACCION}
            </Button>
            <input
              ref={selector}
              type="file"
              hidden
              accept=".yaml,.yml,text/yaml"
              aria-label={ACCION}
              onChange={elegir}
            />
          </>
        )}
      </Stack>
      {datos && (
        <>
          <Typography component="h2" variant="h5" sx={{ mt: 2 }} gutterBottom>
            Etapas
          </Typography>
          <Etapas configuracion={datos} />
          <Typography component="h2" variant="h5" sx={{ mt: 2 }} gutterBottom>
            Árbol de firmas
          </Typography>
          <Firmas configuracion={datos} />
        </>
      )}
    </>
  );
}

export function Flujo() {
  useTitulo('Flujo y firmas');
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
        Flujo y firmas
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
      {periodo && <DelPeriodo key={periodo} periodo={periodo} />}
    </>
  );
}
