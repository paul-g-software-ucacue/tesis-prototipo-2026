// Los diálogos de «Estructura orgánica» con escritura (F03-H01): registrar o corregir una unidad y
// confirmar que se inactiva o se reactiva. Los campos de su tipo se ofrecen según el tipo: la
// gestión autorizada es obligatoria en una gestión y no existe en una comisión, que en cambio dice
// si es permanente. El servidor valida todo; aquí solo se muestra lo que responde (ADR-019).
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import {
  corregirUnidad,
  registrarUnidad,
  type GestionAutorizada,
  type TipoDeUnidad,
  type UnidadOrganica,
} from './consultas';
import { NOMBRE_DEL_TIPO, conSusDependientes } from './jerarquia';

const CAMPOS = ['codigo', 'nombre', 'tipo', 'padre', 'gestionAutorizada', 'comisionPermanente'];

const SELECCION = { select: { native: true }, inputLabel: { shrink: true } } as const;

function detalleDe(aviso: MensajeDeError | null, campo: string) {
  return aviso?.detalles.find((detalle) => detalle.campo === campo)?.descripcion;
}

/** Lo que falta para enviar, o nada. */
function queFalta(
  nuevo: boolean,
  {
    codigo,
    nombre,
    tipo,
    padre,
    gestion,
    permanente,
  }: {
    codigo: string;
    nombre: string;
    tipo: string;
    padre: string;
    gestion: string;
    permanente: string;
  },
) {
  if ((nuevo && !codigo.trim()) || !nombre.trim() || !tipo) {
    return nuevo ? 'Escribe el código y el nombre, y elige el tipo.' : 'Escribe el nombre.';
  }
  if (tipo !== 'SEDE_MATRIZ' && !padre) {
    return 'Elige la unidad de la que depende.';
  }
  if (tipo === 'GESTION' && !gestion) {
    return 'Elige la gestión autorizada que representa.';
  }
  if (tipo === 'COMISION' && !permanente) {
    return 'Indica si la comisión es permanente.';
  }
  return null;
}

interface PropiedadesDeDatos {
  /** Sin unidad, registra una nueva; con unidad, la corrige sin cambiar su código ni su tipo. */
  unidad?: UnidadOrganica;
  unidades: readonly UnidadOrganica[];
  gestiones: readonly GestionAutorizada[];
  alCerrar: () => void;
  alGuardar: () => Promise<void>;
}

export function DialogoDatosDeUnidad({
  unidad,
  unidades,
  gestiones,
  alCerrar,
  alGuardar,
}: PropiedadesDeDatos) {
  const [codigo, setCodigo] = useState('');
  const [nombre, setNombre] = useState(unidad?.nombre ?? '');
  const [tipo, setTipo] = useState<TipoDeUnidad | ''>(unidad?.tipo ?? '');
  const [padre, setPadre] = useState(unidad?.padre ?? '');
  const [gestion, setGestion] = useState(unidad?.gestionAutorizada ?? '');
  const [permanente, setPermanente] = useState(
    unidad?.comisionPermanente === undefined ? '' : unidad.comisionPermanente ? 'SI' : 'NO',
  );
  const [falta, setFalta] = useState<string | null>(null);
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [enviando, setEnviando] = useState(false);
  const titulo = unidad ? `Corregir la unidad ${unidad.codigo}` : 'Registrar unidad';
  // Ni la unidad ni las suyas pueden ser su padre: sería un ciclo.
  const excluidas = unidad ? conSusDependientes(unidades, unidad.codigo) : new Set<string>();
  const padres = unidades
    .filter((candidata) => candidata.activa && !excluidas.has(candidata.codigo))
    .map((candidata) => candidata.codigo)
    .sort((a, b) => a.localeCompare(b));

  const guardar = () => {
    const pendiente = queFalta(!unidad, { codigo, nombre, tipo, padre, gestion, permanente });
    setFalta(pendiente);
    if (pendiente || !tipo) {
      return;
    }
    const datos = {
      nombre: nombre.trim(),
      ...(tipo !== 'SEDE_MATRIZ' && { padre }),
      ...(tipo !== 'COMISION' && gestion && { gestionAutorizada: gestion }),
      ...(tipo === 'COMISION' && { comisionPermanente: permanente === 'SI' }),
    };
    setAviso(null);
    setEnviando(true);
    void (async () => {
      try {
        if (unidad) {
          await corregirUnidad(unidad.codigo, datos);
        } else {
          await registrarUnidad({ codigo: codigo.trim(), tipo, ...datos });
        }
        await alGuardar();
      } catch (fallo) {
        setAviso(traducirError(fallo));
      } finally {
        setEnviando(false);
      }
    })();
  };

  return (
    <Dialog open onClose={alCerrar} aria-labelledby="titulo-datos-de-unidad" fullWidth>
      <DialogTitle id="titulo-datos-de-unidad">{titulo}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {unidad ? (
            <Typography>{`Tipo: ${NOMBRE_DEL_TIPO[unidad.tipo]}. El código y el tipo no cambian.`}</Typography>
          ) : (
            <Typography>La unidad queda activa. Su código y su tipo no cambian después.</Typography>
          )}
          {aviso && <AvisoDeError mensaje={aviso} />}
          {(aviso?.detalles ?? [])
            .filter(({ campo }) => !CAMPOS.includes(campo))
            .map(({ descripcion }) => (
              <Typography key={descripcion} color="error">
                {descripcion}
              </Typography>
            ))}
          {!unidad && (
            <TextField
              label="Código"
              value={codigo}
              onChange={(evento) => setCodigo(evento.target.value.toUpperCase())}
              error={Boolean(detalleDe(aviso, 'codigo'))}
              helperText={
                detalleDe(aviso, 'codigo') ??
                'Mayúsculas, números, guion y guion bajo; por ejemplo, G-INV.'
              }
              slotProps={{ htmlInput: { maxLength: 20 } }}
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
          {!unidad && (
            <TextField
              select
              label="Tipo"
              value={tipo}
              onChange={(evento) => setTipo(evento.target.value as TipoDeUnidad | '')}
              error={Boolean(detalleDe(aviso, 'tipo'))}
              helperText={detalleDe(aviso, 'tipo')}
              slotProps={SELECCION}
            >
              <option value="">Elige uno</option>
              {Object.entries(NOMBRE_DEL_TIPO).map(([valor, texto]) => (
                <option key={valor} value={valor}>
                  {texto}
                </option>
              ))}
            </TextField>
          )}
          {tipo === 'COMISION' && (
            <Typography color="text.secondary">
              Una comisión es un dato informativo, sin horas.
            </Typography>
          )}
          {tipo && tipo !== 'SEDE_MATRIZ' && (
            <TextField
              select
              label="Depende de"
              value={padre}
              onChange={(evento) => setPadre(evento.target.value)}
              error={Boolean(detalleDe(aviso, 'padre'))}
              helperText={detalleDe(aviso, 'padre') ?? 'Solo las unidades activas.'}
              slotProps={SELECCION}
            >
              <option value="">Elige una</option>
              {padres.map((codigoDelPadre) => (
                <option key={codigoDelPadre} value={codigoDelPadre}>
                  {codigoDelPadre}
                </option>
              ))}
            </TextField>
          )}
          {tipo && tipo !== 'COMISION' && (
            <TextField
              select
              label="Gestión autorizada"
              value={gestion}
              onChange={(evento) => setGestion(evento.target.value)}
              error={Boolean(detalleDe(aviso, 'gestionAutorizada'))}
              helperText={
                detalleDe(aviso, 'gestionAutorizada') ??
                (tipo === 'GESTION'
                  ? 'Una de las 21 gestiones autorizadas de POL 6.'
                  : 'Opcional; por ejemplo, el Rectorado representa G01.')
              }
              slotProps={SELECCION}
            >
              <option value="">{tipo === 'GESTION' ? 'Elige una' : 'Ninguna'}</option>
              {gestiones.map((opcion) => (
                <option key={opcion.codigo} value={opcion.codigo}>
                  {`${opcion.codigo} · ${opcion.nombre}`}
                </option>
              ))}
            </TextField>
          )}
          {tipo === 'COMISION' && (
            <TextField
              select
              label="¿Es permanente?"
              value={permanente}
              onChange={(evento) => setPermanente(evento.target.value)}
              error={Boolean(detalleDe(aviso, 'comisionPermanente'))}
              helperText={detalleDe(aviso, 'comisionPermanente')}
              slotProps={SELECCION}
            >
              <option value="">Elige</option>
              <option value="SI">Sí</option>
              <option value="NO">No</option>
            </TextField>
          )}
          {falta && <Typography color="error">{falta}</Typography>}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cancelar</Button>
        <Button variant="contained" disabled={enviando} onClick={guardar}>
          {unidad ? 'Guardar' : 'Registrar'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export function DialogoEstado({
  unidad,
  alCerrar,
  alConfirmar,
}: {
  unidad: UnidadOrganica;
  alCerrar: () => void;
  alConfirmar: () => void;
}) {
  const accion = unidad.activa ? 'Inactivar' : 'Reactivar';
  return (
    <Dialog open onClose={alCerrar} aria-labelledby="titulo-estado-de-unidad" fullWidth>
      <DialogTitle id="titulo-estado-de-unidad">{`${accion} la unidad ${unidad.codigo}`}</DialogTitle>
      <DialogContent>
        <Typography>
          {unidad.activa
            ? 'La unidad queda en la estructura como inactiva y no se ofrece para lo nuevo. No se borra, y se puede reactivar.'
            : 'La unidad vuelve a estar activa. Su padre tiene que estar activo.'}
        </Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cancelar</Button>
        <Button
          variant="contained"
          color={unidad.activa ? 'error' : 'primary'}
          onClick={alConfirmar}
        >
          {accion}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
