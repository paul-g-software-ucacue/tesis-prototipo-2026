// Los diálogos de «Carreras y mallas» con escritura (F03-H02): registrar o corregir una carrera o
// una asignatura, registrar una malla, cambiar el estado de una carrera y confirmar que se deroga
// una malla o se inactiva una asignatura. Antes de enviar solo se dice qué falta; el servidor valida
// todo y aquí se muestra lo que responde, junto a su campo (ADR-019).
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import FormControl from '@mui/material/FormControl';
import FormControlLabel from '@mui/material/FormControlLabel';
import FormGroup from '@mui/material/FormGroup';
import FormHelperText from '@mui/material/FormHelperText';
import FormLabel from '@mui/material/FormLabel';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState, type ReactNode } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import type { UnidadOrganica } from '../estructura/consultas';
import {
  cambiarEstadoCarrera,
  corregirAsignatura,
  corregirCarrera,
  registrarAsignatura,
  registrarCarrera,
  registrarMalla,
  type Asignatura,
  type CampoAmplio,
  type Carrera,
  type EstadoDeCarrera,
  type Jornada,
  type Modalidad,
  type NivelDeFormacion,
  type UnidadDeOrganizacionCurricular,
} from './consultas';
import {
  NOMBRE_DE_LA_JORNADA,
  NOMBRE_DE_LA_MODALIDAD,
  NOMBRE_DE_LA_UNIDAD,
  NOMBRE_DEL_ESTADO_DE_CARRERA,
  NOMBRE_DEL_NIVEL,
} from './nombres';

const SELECCION = { select: { native: true }, inputLabel: { shrink: true } } as const;

/** Los tipos de unidad que pueden ser la sede de una carrera (carga 02). */
const TIPOS_DE_SEDE = new Set(['SEDE_MATRIZ', 'SEDE', 'EXTENSION', 'CAMPUS']);

function detalleDe(aviso: MensajeDeError | null, campo: string) {
  return aviso?.detalles.find((detalle) => detalle.campo === campo)?.descripcion;
}

/** Lo que falta para enviar, por su nombre en pantalla, o nada. */
function queFalta(campos: readonly [string, boolean][]) {
  const faltan = campos.filter(([, lleno]) => !lleno).map(([nombre]) => nombre);
  return faltan.length ? `Falta: ${faltan.join(', ')}.` : null;
}

/** Un número escrito en un campo, o nada si está vacío. */
function numero(texto: string): number | undefined {
  return texto.trim() ? Number(texto.replace(',', '.')) : undefined;
}

/** El aviso del servidor y los detalles que no son de un campo del diálogo. */
function Avisos({ aviso, campos }: { aviso: MensajeDeError | null; campos: readonly string[] }) {
  return (
    <>
      {aviso && <AvisoDeError mensaje={aviso} />}
      {(aviso?.detalles ?? [])
        .filter(({ campo }) => !campos.includes(campo))
        .map(({ descripcion }) => (
          <Typography key={descripcion} color="error">
            {descripcion}
          </Typography>
        ))}
    </>
  );
}

/** Envía, muestra el error del servidor si lo hay y avisa cuando terminó bien. */
function useEnvio(alGuardar: () => Promise<void>) {
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [enviando, setEnviando] = useState(false);
  const enviar = (operacion: () => Promise<unknown>) => {
    setAviso(null);
    setEnviando(true);
    void (async () => {
      try {
        await operacion();
        await alGuardar();
      } catch (fallo) {
        setAviso(traducirError(fallo));
      } finally {
        setEnviando(false);
      }
    })();
  };
  return { aviso, enviando, enviar };
}

function Marco({
  id,
  titulo,
  accion,
  enviando,
  alCerrar,
  alEnviar,
  children,
}: {
  id: string;
  titulo: string;
  accion: string;
  enviando: boolean;
  alCerrar: () => void;
  alEnviar: () => void;
  children: ReactNode;
}) {
  return (
    <Dialog open onClose={alCerrar} aria-labelledby={id} fullWidth>
      <DialogTitle id={id}>{titulo}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {children}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cancelar</Button>
        <Button variant="contained" disabled={enviando} onClick={alEnviar}>
          {accion}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

function SeleccionDeCampoAmplio({
  valor,
  campos,
  aviso,
  alCambiar,
}: {
  valor: string;
  campos: readonly CampoAmplio[];
  aviso: MensajeDeError | null;
  alCambiar: (valor: string) => void;
}) {
  return (
    <TextField
      select
      label="Campo amplio"
      value={valor}
      onChange={(evento) => alCambiar(evento.target.value)}
      error={Boolean(detalleDe(aviso, 'campoAmplio'))}
      helperText={detalleDe(aviso, 'campoAmplio') ?? 'De la CINE.'}
      slotProps={SELECCION}
    >
      <option value="">Elige uno</option>
      {campos.map((campo) => (
        <option key={campo.codigo} value={campo.codigo}>
          {`${campo.codigo} · ${campo.nombre}`}
        </option>
      ))}
    </TextField>
  );
}

const CAMPOS_DE_CARRERA = [
  'codigo',
  'nombre',
  'siglas',
  'sede',
  'nivelFormacion',
  'modalidad',
  'jornadas',
  'campoAmplio',
  'campoEspecifico',
  'campoDetallado',
  'numeroCiclos',
];

export function DialogoDatosDeCarrera({
  carrera,
  unidades,
  campos,
  alCerrar,
  alGuardar,
}: {
  /** Sin carrera, registra una nueva; con carrera, la corrige sin cambiar su código. */
  carrera?: Carrera;
  unidades: readonly UnidadOrganica[];
  campos: readonly CampoAmplio[];
  alCerrar: () => void;
  alGuardar: () => Promise<void>;
}) {
  const [codigo, setCodigo] = useState('');
  const [nombre, setNombre] = useState(carrera?.nombre ?? '');
  const [siglas, setSiglas] = useState(carrera?.siglas ?? '');
  const [sede, setSede] = useState(carrera?.sede ?? '');
  const [nivel, setNivel] = useState<NivelDeFormacion | ''>(carrera?.nivelFormacion ?? '');
  const [modalidad, setModalidad] = useState<Modalidad | ''>(carrera?.modalidad ?? '');
  const [jornadas, setJornadas] = useState<Jornada[]>(carrera?.jornadas ?? []);
  const [amplio, setAmplio] = useState(carrera?.campoAmplio ?? '');
  const [especifico, setEspecifico] = useState(carrera?.campoEspecifico ?? '');
  const [detallado, setDetallado] = useState(carrera?.campoDetallado ?? '');
  const [ciclos, setCiclos] = useState(carrera ? String(carrera.numeroCiclos) : '');
  const [falta, setFalta] = useState<string | null>(null);
  const { aviso, enviando, enviar } = useEnvio(alGuardar);
  // Las sedes activas, y la actual aunque ya no lo esté, para no perderla al corregir.
  const sedes = unidades
    .filter(
      (unidad) =>
        TIPOS_DE_SEDE.has(unidad.tipo) && (unidad.activa || unidad.codigo === carrera?.sede),
    )
    .sort((a, b) => a.codigo.localeCompare(b.codigo));

  const alternar = (jornada: Jornada) =>
    setJornadas((actuales) =>
      actuales.includes(jornada)
        ? actuales.filter((una) => una !== jornada)
        : (Object.keys(NOMBRE_DE_LA_JORNADA) as Jornada[]).filter(
            (una) => una === jornada || actuales.includes(una),
          ),
    );

  const guardar = () => {
    const pendiente = queFalta([
      ['código SENESCYT', Boolean(carrera) || Boolean(codigo.trim())],
      ['nombre', Boolean(nombre.trim())],
      ['siglas', Boolean(siglas.trim())],
      ['sede', Boolean(sede)],
      ['nivel de formación', Boolean(nivel)],
      ['modalidad', Boolean(modalidad)],
      ['al menos una jornada', jornadas.length > 0],
      ['campo amplio', Boolean(amplio)],
      ['campo específico', Boolean(especifico.trim())],
      ['número de ciclos', Boolean(ciclos.trim())],
    ]);
    setFalta(pendiente);
    if (pendiente || !nivel || !modalidad) {
      return;
    }
    const datos = {
      nombre: nombre.trim(),
      siglas: siglas.trim(),
      sede,
      nivelFormacion: nivel,
      modalidad,
      jornadas,
      campoAmplio: amplio,
      campoEspecifico: especifico.trim(),
      ...(detallado.trim() && { campoDetallado: detallado.trim() }),
      numeroCiclos: Number(ciclos),
    };
    enviar(() =>
      carrera
        ? corregirCarrera(carrera.codigo, datos)
        : registrarCarrera({ codigo: codigo.trim(), ...datos }),
    );
  };

  return (
    <Marco
      id="titulo-datos-de-carrera"
      titulo={carrera ? `Corregir la carrera ${carrera.codigo}` : 'Registrar carrera'}
      accion={carrera ? 'Guardar' : 'Registrar'}
      enviando={enviando}
      alCerrar={alCerrar}
      alEnviar={guardar}
    >
      <Typography>
        {carrera
          ? 'El código no cambia; el estado se cambia aparte.'
          : 'La carrera queda vigente. Su código no cambia después.'}
      </Typography>
      <Avisos aviso={aviso} campos={CAMPOS_DE_CARRERA} />
      {!carrera && (
        <TextField
          label="Código SENESCYT"
          value={codigo}
          onChange={(evento) => setCodigo(evento.target.value.toUpperCase())}
          error={Boolean(detalleDe(aviso, 'codigo'))}
          helperText={detalleDe(aviso, 'codigo') ?? 'Por ejemplo, 550613A01-P-0301.'}
          slotProps={{ htmlInput: { maxLength: 20 } }}
        />
      )}
      <TextField
        label="Nombre"
        value={nombre}
        onChange={(evento) => setNombre(evento.target.value.toUpperCase())}
        error={Boolean(detalleDe(aviso, 'nombre'))}
        helperText={detalleDe(aviso, 'nombre')}
        slotProps={{ htmlInput: { maxLength: 150 } }}
      />
      <TextField
        label="Siglas"
        value={siglas}
        onChange={(evento) => setSiglas(evento.target.value.toUpperCase())}
        error={Boolean(detalleDe(aviso, 'siglas'))}
        helperText={detalleDe(aviso, 'siglas') ?? 'Únicas entre las carreras.'}
        slotProps={{ htmlInput: { maxLength: 10 } }}
      />
      <TextField
        select
        label="Sede"
        value={sede}
        onChange={(evento) => setSede(evento.target.value)}
        error={Boolean(detalleDe(aviso, 'sede'))}
        helperText={
          detalleDe(aviso, 'sede') ?? 'La sede matriz, una sede, una extensión o un campus.'
        }
        slotProps={SELECCION}
      >
        <option value="">Elige una</option>
        {sedes.map((unidad) => (
          <option key={unidad.codigo} value={unidad.codigo}>
            {`${unidad.codigo} · ${unidad.nombre}`}
          </option>
        ))}
      </TextField>
      <TextField
        select
        label="Nivel de formación"
        value={nivel}
        onChange={(evento) => setNivel(evento.target.value as NivelDeFormacion | '')}
        error={Boolean(detalleDe(aviso, 'nivelFormacion'))}
        helperText={detalleDe(aviso, 'nivelFormacion')}
        slotProps={SELECCION}
      >
        <option value="">Elige uno</option>
        {Object.entries(NOMBRE_DEL_NIVEL).map(([valor, texto]) => (
          <option key={valor} value={valor}>
            {texto}
          </option>
        ))}
      </TextField>
      <TextField
        select
        label="Modalidad"
        value={modalidad}
        onChange={(evento) => setModalidad(evento.target.value as Modalidad | '')}
        error={Boolean(detalleDe(aviso, 'modalidad'))}
        helperText={detalleDe(aviso, 'modalidad')}
        slotProps={SELECCION}
      >
        <option value="">Elige una</option>
        {Object.entries(NOMBRE_DE_LA_MODALIDAD).map(([valor, texto]) => (
          <option key={valor} value={valor}>
            {texto}
          </option>
        ))}
      </TextField>
      <FormControl component="fieldset" error={Boolean(detalleDe(aviso, 'jornadas'))}>
        <FormLabel component="legend">Jornadas</FormLabel>
        <FormGroup row>
          {(Object.entries(NOMBRE_DE_LA_JORNADA) as [Jornada, string][]).map(([valor, texto]) => (
            <FormControlLabel
              key={valor}
              label={texto}
              control={
                <Checkbox checked={jornadas.includes(valor)} onChange={() => alternar(valor)} />
              }
            />
          ))}
        </FormGroup>
        {detalleDe(aviso, 'jornadas') && (
          <FormHelperText>{detalleDe(aviso, 'jornadas')}</FormHelperText>
        )}
      </FormControl>
      <SeleccionDeCampoAmplio valor={amplio} campos={campos} aviso={aviso} alCambiar={setAmplio} />
      <TextField
        label="Campo específico"
        value={especifico}
        onChange={(evento) => setEspecifico(evento.target.value)}
        error={Boolean(detalleDe(aviso, 'campoEspecifico'))}
        helperText={
          detalleDe(aviso, 'campoEspecifico') ?? 'Tres dígitos que empiezan por el campo amplio.'
        }
        slotProps={{ htmlInput: { maxLength: 3, inputMode: 'numeric' } }}
      />
      <TextField
        label="Campo detallado"
        value={detallado}
        onChange={(evento) => setDetallado(evento.target.value)}
        error={Boolean(detalleDe(aviso, 'campoDetallado'))}
        helperText={detalleDe(aviso, 'campoDetallado') ?? 'Opcional; cuatro dígitos.'}
        slotProps={{ htmlInput: { maxLength: 4, inputMode: 'numeric' } }}
      />
      <TextField
        label="Número de ciclos"
        type="number"
        value={ciclos}
        onChange={(evento) => setCiclos(evento.target.value)}
        error={Boolean(detalleDe(aviso, 'numeroCiclos'))}
        helperText={detalleDe(aviso, 'numeroCiclos') ?? 'De 1 a 10.'}
        slotProps={{ htmlInput: { min: 1, max: 10 } }}
      />
      {falta && <Typography color="error">{falta}</Typography>}
    </Marco>
  );
}

export function DialogoMalla({
  carrera,
  alCerrar,
  alGuardar,
}: {
  carrera: Carrera;
  alCerrar: () => void;
  alGuardar: () => Promise<void>;
}) {
  const [codigo, setCodigo] = useState('');
  const [falta, setFalta] = useState<string | null>(null);
  const { aviso, enviando, enviar } = useEnvio(alGuardar);

  const guardar = () => {
    const pendiente = queFalta([['código de versión', Boolean(codigo.trim())]]);
    setFalta(pendiente);
    if (!pendiente) {
      enviar(() => registrarMalla(carrera.codigo, codigo.trim()));
    }
  };

  return (
    <Marco
      id="titulo-malla"
      titulo="Registrar malla"
      accion="Registrar"
      enviando={enviando}
      alCerrar={alCerrar}
      alEnviar={guardar}
    >
      <Typography>{`La malla queda vigente en ${carrera.nombre}. Su código de versión no cambia.`}</Typography>
      <Avisos aviso={aviso} campos={['codigo']} />
      <TextField
        label="Código de versión"
        value={codigo}
        onChange={(evento) => setCodigo(evento.target.value.toUpperCase())}
        error={Boolean(detalleDe(aviso, 'codigo'))}
        helperText={detalleDe(aviso, 'codigo') ?? 'Por ejemplo, DS-2019.'}
        slotProps={{ htmlInput: { maxLength: 20 } }}
      />
      {falta && <Typography color="error">{falta}</Typography>}
    </Marco>
  );
}

export function DialogoEstadoDeCarrera({
  carrera,
  alCerrar,
  alGuardar,
}: {
  carrera: Carrera;
  alCerrar: () => void;
  alGuardar: () => Promise<void>;
}) {
  const [estado, setEstado] = useState<EstadoDeCarrera>(carrera.estado);
  const { aviso, enviando, enviar } = useEnvio(alGuardar);

  return (
    <Marco
      id="titulo-estado-de-carrera"
      titulo="Cambiar el estado de la carrera"
      accion="Guardar"
      enviando={enviando}
      alCerrar={alCerrar}
      alEnviar={() => enviar(() => cambiarEstadoCarrera(carrera.codigo, estado))}
    >
      <Typography>
        {`${carrera.nombre}. Solo una carrera vigente admite asignaturas nuevas y oferta; sus mallas y asignaturas se conservan.`}
      </Typography>
      <Avisos aviso={aviso} campos={[]} />
      <TextField
        select
        label="Estado"
        value={estado}
        onChange={(evento) => setEstado(evento.target.value as EstadoDeCarrera)}
        slotProps={SELECCION}
      >
        {Object.entries(NOMBRE_DEL_ESTADO_DE_CARRERA).map(([valor, texto]) => (
          <option key={valor} value={valor}>
            {texto}
          </option>
        ))}
      </TextField>
    </Marco>
  );
}

const CAMPOS_DE_ASIGNATURA = [
  'codigo',
  'nombre',
  'ciclo',
  'unidadOrganizacionCurricular',
  'horasSemanalesClase',
  'horasPracticasSemanales',
  'creditos',
  'troncoComun',
  'campoAmplio',
  'campoEspecifico',
  'palabrasClave',
];

export function DialogoDatosDeAsignatura({
  asignatura,
  carrera,
  malla,
  campos,
  alCerrar,
  alGuardar,
}: {
  /** Sin asignatura, registra una nueva en la malla; con asignatura, la corrige. */
  asignatura?: Asignatura;
  carrera: Carrera;
  malla: string;
  campos: readonly CampoAmplio[];
  alCerrar: () => void;
  alGuardar: () => Promise<void>;
}) {
  const [codigo, setCodigo] = useState('');
  const [nombre, setNombre] = useState(asignatura?.nombre ?? '');
  const [ciclo, setCiclo] = useState(asignatura ? String(asignatura.ciclo) : '');
  const [unidad, setUnidad] = useState<UnidadDeOrganizacionCurricular | ''>(
    asignatura?.unidadOrganizacionCurricular ?? '',
  );
  const [clase, setClase] = useState(asignatura ? String(asignatura.horasSemanalesClase) : '');
  const [practicas, setPracticas] = useState(
    asignatura?.horasPracticasSemanales === undefined
      ? ''
      : String(asignatura.horasPracticasSemanales),
  );
  const [creditos, setCreditos] = useState(
    asignatura?.creditos === undefined ? '' : String(asignatura.creditos),
  );
  const [troncoComun, setTroncoComun] = useState(asignatura?.troncoComun ?? false);
  const [amplio, setAmplio] = useState(asignatura?.campoAmplio.codigo ?? '');
  const [especifico, setEspecifico] = useState(asignatura?.campoEspecifico ?? '');
  const [palabras, setPalabras] = useState(asignatura?.palabrasClave.join('; ') ?? '');
  const [falta, setFalta] = useState<string | null>(null);
  const { aviso, enviando, enviar } = useEnvio(alGuardar);

  const guardar = () => {
    const palabrasClave = palabras
      .split(';')
      .map((palabra) => palabra.trim())
      .filter(Boolean);
    const pendiente = queFalta([
      ['código', Boolean(asignatura) || Boolean(codigo.trim())],
      ['nombre', Boolean(nombre.trim())],
      ['ciclo', Boolean(ciclo.trim())],
      ['unidad de organización curricular', Boolean(unidad)],
      ['horas semanales de clase', Boolean(clase.trim())],
      ['campo amplio', Boolean(amplio)],
      ['campo específico', Boolean(especifico.trim())],
      ['palabras clave', palabrasClave.length > 0],
    ]);
    setFalta(pendiente);
    if (pendiente || !unidad) {
      return;
    }
    const horasPracticas = numero(practicas);
    const enCreditos = numero(creditos);
    const datos = {
      nombre: nombre.trim(),
      ciclo: Number(ciclo),
      unidadOrganizacionCurricular: unidad,
      horasSemanalesClase: numero(clase) ?? 0,
      ...(horasPracticas !== undefined && { horasPracticasSemanales: horasPracticas }),
      ...(enCreditos !== undefined && { creditos: enCreditos }),
      troncoComun,
      campoAmplio: amplio,
      campoEspecifico: especifico.trim(),
      palabrasClave,
    };
    enviar(() =>
      asignatura
        ? corregirAsignatura(asignatura.codigo, datos)
        : registrarAsignatura({
            codigo: codigo.trim(),
            carrera: carrera.codigo,
            malla,
            ...datos,
          }),
    );
  };

  return (
    <Marco
      id="titulo-datos-de-asignatura"
      titulo={asignatura ? `Corregir la asignatura ${asignatura.codigo}` : 'Registrar asignatura'}
      accion={asignatura ? 'Guardar' : 'Registrar'}
      enviando={enviando}
      alCerrar={alCerrar}
      alEnviar={guardar}
    >
      <Typography>
        {`${carrera.nombre} · malla ${malla}. ${
          asignatura
            ? 'El código, la carrera y la malla no cambian.'
            : 'La asignatura queda activa. Su código no cambia después.'
        }`}
      </Typography>
      <Avisos aviso={aviso} campos={CAMPOS_DE_ASIGNATURA} />
      {!asignatura && (
        <TextField
          label="Código"
          value={codigo}
          onChange={(evento) => setCodigo(evento.target.value.toUpperCase())}
          error={Boolean(detalleDe(aviso, 'codigo'))}
          helperText={detalleDe(aviso, 'codigo') ?? 'Por ejemplo, DS-P-II-POO-204.'}
          slotProps={{ htmlInput: { maxLength: 30 } }}
        />
      )}
      <TextField
        label="Nombre"
        value={nombre}
        onChange={(evento) => setNombre(evento.target.value)}
        error={Boolean(detalleDe(aviso, 'nombre'))}
        helperText={detalleDe(aviso, 'nombre')}
        slotProps={{ htmlInput: { maxLength: 150 } }}
      />
      <TextField
        label="Ciclo"
        type="number"
        value={ciclo}
        onChange={(evento) => setCiclo(evento.target.value)}
        error={Boolean(detalleDe(aviso, 'ciclo'))}
        helperText={
          detalleDe(aviso, 'ciclo') ?? `De 1 a ${String(carrera.numeroCiclos)}, los de la carrera.`
        }
        slotProps={{ htmlInput: { min: 1, max: carrera.numeroCiclos } }}
      />
      <TextField
        select
        label="Unidad de organización curricular"
        value={unidad}
        onChange={(evento) => setUnidad(evento.target.value as UnidadDeOrganizacionCurricular | '')}
        error={Boolean(detalleDe(aviso, 'unidadOrganizacionCurricular'))}
        helperText={detalleDe(aviso, 'unidadOrganizacionCurricular')}
        slotProps={SELECCION}
      >
        <option value="">Elige una</option>
        {Object.entries(NOMBRE_DE_LA_UNIDAD).map(([valor, texto]) => (
          <option key={valor} value={valor}>
            {texto}
          </option>
        ))}
      </TextField>
      <TextField
        label="Horas semanales de clase"
        type="number"
        value={clase}
        onChange={(evento) => setClase(evento.target.value)}
        error={Boolean(detalleDe(aviso, 'horasSemanalesClase'))}
        helperText={detalleDe(aviso, 'horasSemanalesClase') ?? 'De 0,5 a 20.'}
        slotProps={{ htmlInput: { min: 0.5, max: 20, step: 0.5 } }}
      />
      <TextField
        label="Horas prácticas semanales"
        type="number"
        value={practicas}
        onChange={(evento) => setPracticas(evento.target.value)}
        error={Boolean(detalleDe(aviso, 'horasPracticasSemanales'))}
        helperText={detalleDe(aviso, 'horasPracticasSemanales') ?? 'Opcional; de 0 a 20.'}
        slotProps={{ htmlInput: { min: 0, max: 20, step: 0.5 } }}
      />
      <TextField
        label="Créditos"
        type="number"
        value={creditos}
        onChange={(evento) => setCreditos(evento.target.value)}
        error={Boolean(detalleDe(aviso, 'creditos'))}
        helperText={detalleDe(aviso, 'creditos') ?? 'Opcional.'}
        slotProps={{ htmlInput: { min: 0, step: 0.5 } }}
      />
      <FormControlLabel
        label="Tronco común"
        control={
          <Checkbox
            checked={troncoComun}
            onChange={(evento) => setTroncoComun(evento.target.checked)}
          />
        }
      />
      <SeleccionDeCampoAmplio valor={amplio} campos={campos} aviso={aviso} alCambiar={setAmplio} />
      <TextField
        label="Campo específico"
        value={especifico}
        onChange={(evento) => setEspecifico(evento.target.value)}
        error={Boolean(detalleDe(aviso, 'campoEspecifico'))}
        helperText={
          detalleDe(aviso, 'campoEspecifico') ?? 'Tres dígitos que empiezan por el campo amplio.'
        }
        slotProps={{ htmlInput: { maxLength: 3, inputMode: 'numeric' } }}
      />
      <TextField
        label="Palabras clave"
        value={palabras}
        onChange={(evento) => setPalabras(evento.target.value)}
        error={Boolean(detalleDe(aviso, 'palabrasClave'))}
        helperText={
          detalleDe(aviso, 'palabrasClave') ?? 'Al menos dos, separadas por punto y coma.'
        }
        slotProps={{ htmlInput: { maxLength: 300 } }}
      />
      {falta && <Typography color="error">{falta}</Typography>}
    </Marco>
  );
}

/** Confirma un cambio de estado que no borra nada: derogar una malla o inactivar una asignatura. */
export function DialogoConfirmar({
  titulo,
  texto,
  accion,
  alCerrar,
  alConfirmar,
}: {
  titulo: string;
  texto: string;
  accion: string;
  alCerrar: () => void;
  alConfirmar: () => void;
}) {
  const peligro = accion === 'Derogar' || accion === 'Inactivar';
  return (
    <Dialog open onClose={alCerrar} aria-labelledby="titulo-confirmar" fullWidth>
      <DialogTitle id="titulo-confirmar">{titulo}</DialogTitle>
      <DialogContent>
        <Typography>{texto}</Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cancelar</Button>
        <Button variant="contained" color={peligro ? 'error' : 'primary'} onClick={alConfirmar}>
          {accion}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
