// La edición en pantalla de una versión de normativa sin periodo (F04-H02, ERS 8.3): la cabecera,
// las horas de cada fila de la tabla de carga horaria, los valores de cada parámetro y, desde
// F04-H03, la severidad de cada regla del motor (MD-08). «Guardar»
// envía la versión completa, como la carga 7 por archivo (FR-010): las reglas las aplica el
// servidor, y cada motivo que responde se muestra junto a su fila o su parámetro, sin códigos.
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
import { useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import {
  editarVersion,
  type FilaCargaHoraria,
  type FilaEditada,
  type VersionNormativa,
} from './consultas';
import { campoDeFila, campoDeParametro, SEVERIDADES, tipoIst } from './formato';
import { nombreDelPerfil } from './perfiles';
import { nombreDeLaRegla, nombreDelParametro } from './reglas';

/** Las horas que se editan de cada fila, con su nombre en la pantalla. */
const HORAS = [
  { campo: 'dedicacionTotal', nombre: 'Dedicación', obligatoria: true },
  { campo: 'horasClase', nombre: 'Clase', obligatoria: true },
  { campo: 'horasClaseMinimo', nombre: 'Clase mínima', obligatoria: true },
  { campo: 'horasClaseMaximo', nombre: 'Clase máxima', obligatoria: true },
  { campo: 'horasOtrasDocencia', nombre: 'Otras de docencia', obligatoria: false },
  { campo: 'horasGestion', nombre: 'Gestión', obligatoria: false },
  {
    campo: 'horasInvestigacionVinculacionMaximo',
    nombre: 'Investigación y vinculación (máx.)',
    obligatoria: false,
  },
] as const;

type CampoDeHoras = (typeof HORAS)[number]['campo'];

// Los valores como texto, para que un campo se pueda vaciar mientras se escribe.
type Textos = Record<string, string>;

const texto = (numero: number | undefined) => (numero === undefined ? '' : String(numero));

function textosIniciales(version: VersionNormativa): Textos {
  const textos: Textos = {};
  for (const fila of version.tablaCargaHoraria) {
    for (const { campo } of HORAS) {
      textos[`${campoDeFila(fila)}.${campo}`] = texto(fila[campo]);
    }
  }
  for (const regla of version.reglas) {
    for (const parametro of regla.parametros) {
      textos[campoDeParametro(regla.identificador, parametro.nombre)] = String(parametro.valor);
    }
  }
  return textos;
}

interface Propiedades {
  version: VersionNormativa;
  alGuardar: () => Promise<void>;
  alCancelar: () => void;
}

export function EdicionDeVersion({ version, alGuardar, alCancelar }: Propiedades) {
  const [codigo, setCodigo] = useState(version.codigo);
  const [origen, setOrigen] = useState(version.origen);
  const [vigenteDesde, setVigenteDesde] = useState(version.vigenteDesde);
  const [textos, setTextos] = useState(() => textosIniciales(version));
  // Solo las reglas del motor tienen severidad; las que solo dan parámetros no la reciben.
  const [severidades, setSeveridades] = useState<Record<string, string>>(() =>
    Object.fromEntries(
      version.reglas.flatMap((regla) =>
        regla.severidad ? [[regla.identificador, regla.severidad]] : [],
      ),
    ),
  );
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [falta, setFalta] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const cambiar = (clave: string, valor: string) => setTextos({ ...textos, [clave]: valor });
  const detalle = (campo: string) =>
    aviso?.detalles.find((uno) => uno.campo === campo)?.descripcion;

  // Los campos del formulario; los demás detalles de un 400 se muestran debajo del aviso.
  const campos = new Set([
    'codigo',
    'origen',
    'vigenteDesde',
    ...version.tablaCargaHoraria.map(campoDeFila),
    ...Object.keys(textos).filter((clave) => clave.startsWith('reglas[')),
    ...Object.keys(severidades).map((regla) => campoDeParametro(regla, 'severidad')),
  ]);

  const filaEditada = (fila: FilaCargaHoraria): FilaEditada | null => {
    const editada: Record<string, unknown> = { aplicaA: fila.aplicaA, perfil: fila.perfil };
    for (const { campo, obligatoria } of HORAS) {
      const valor = textos[`${campoDeFila(fila)}.${campo}`]?.trim() ?? '';
      if (valor === '') {
        if (obligatoria) {
          return null;
        }
        continue;
      }
      editada[campo] = Number(valor);
    }
    return editada as unknown as FilaEditada;
  };

  const guardar = async () => {
    const filas = version.tablaCargaHoraria.map(filaEditada);
    const parametrosVacios = Object.entries(textos).some(
      ([clave, valor]) => clave.startsWith('reglas[') && valor.trim() === '',
    );
    if (
      filas.some((fila) => fila === null) ||
      parametrosVacios ||
      !codigo.trim() ||
      !origen.trim()
    ) {
      setFalta(
        'Completa el código, el origen, la dedicación y las horas de clase de cada fila y todos los parámetros.',
      );
      return;
    }
    setFalta(null);
    setAviso(null);
    setEnviando(true);
    try {
      await editarVersion(version.numero, {
        codigo: codigo.trim(),
        origen: origen.trim(),
        vigenteDesde,
        tablaCargaHoraria: filas as FilaEditada[],
        reglas: version.reglas.map((regla) => ({
          identificador: regla.identificador,
          ...(severidades[regla.identificador] && {
            severidad: severidades[regla.identificador] as 'DURA' | 'BLANDA',
          }),
          parametros: regla.parametros.map((parametro) => ({
            nombre: parametro.nombre,
            valor: Number(textos[campoDeParametro(regla.identificador, parametro.nombre)]),
          })),
        })),
      });
      await alGuardar();
    } catch (fallo) {
      setAviso(traducirError(fallo));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Stack spacing={2} sx={{ mt: 2 }}>
      {aviso && <AvisoDeError mensaje={aviso} />}
      {aviso?.detalles
        .filter(({ campo }) => !campos.has(campo))
        .map(({ descripcion }) => (
          <Typography key={descripcion} color="error">
            {descripcion}
          </Typography>
        ))}
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <TextField
          label="Código"
          value={codigo}
          onChange={(evento) => setCodigo(evento.target.value)}
          error={Boolean(detalle('codigo'))}
          helperText={detalle('codigo')}
          slotProps={{ htmlInput: { maxLength: 40 } }}
        />
        <TextField
          label="Origen"
          value={origen}
          onChange={(evento) => setOrigen(evento.target.value)}
          error={Boolean(detalle('origen'))}
          helperText={detalle('origen')}
          slotProps={{ htmlInput: { maxLength: 120 } }}
          sx={{ flex: 1 }}
        />
        <TextField
          label="Vigente desde"
          type="date"
          value={vigenteDesde}
          onChange={(evento) => setVigenteDesde(evento.target.value)}
          error={Boolean(detalle('vigenteDesde'))}
          helperText={detalle('vigenteDesde')}
          slotProps={{ inputLabel: { shrink: true } }}
        />
      </Stack>

      <Typography component="h3" variant="h6">
        Reglas y parámetros
      </Typography>
      <Table aria-label="Reglas y parámetros" size="small">
        <TableHead>
          <TableRow>
            <TableCell>Regla</TableCell>
            <TableCell>Qué controla</TableCell>
            <TableCell>Severidad</TableCell>
            <TableCell>Parámetros</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {version.reglas.map((regla) => (
            <TableRow key={regla.identificador}>
              <TableCell>{regla.identificador}</TableCell>
              <TableCell>{nombreDeLaRegla(regla.identificador)}</TableCell>
              <TableCell>
                <SelectorDeSeveridad
                  regla={regla.identificador}
                  severidad={severidades[regla.identificador]}
                  motivo={detalle(campoDeParametro(regla.identificador, 'severidad'))}
                  alCambiar={(nueva) =>
                    setSeveridades({ ...severidades, [regla.identificador]: nueva })
                  }
                />
              </TableCell>
              <TableCell>
                <Stack spacing={1}>
                  {regla.parametros.map((parametro) => {
                    const clave = campoDeParametro(regla.identificador, parametro.nombre);
                    const nombre = nombreDelParametro(parametro.nombre);
                    return (
                      <TextField
                        key={parametro.nombre}
                        label={nombre}
                        type="number"
                        size="small"
                        value={textos[clave] ?? ''}
                        onChange={(evento) => cambiar(clave, evento.target.value)}
                        error={Boolean(detalle(clave))}
                        helperText={detalle(clave)}
                        slotProps={{
                          htmlInput: {
                            'aria-label': `${regla.identificador} · ${nombre}`,
                            min: 0,
                            step: 'any',
                          },
                        }}
                      />
                    );
                  })}
                </Stack>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <Typography component="h3" variant="h6">
        Tabla de carga horaria
      </Typography>
      <Typography variant="body2" color="text.secondary">
        Horas semanales por perfil y tipo de IST. Deja vacío un valor que la política no da.
      </Typography>
      <TableContainer>
        <Table aria-label="Tabla de carga horaria" size="small">
          <TableHead>
            <TableRow>
              <TableCell>Perfil</TableCell>
              <TableCell>Tipo de IST</TableCell>
              {HORAS.map(({ campo, nombre }) => (
                <TableCell key={campo}>{nombre}</TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {version.tablaCargaHoraria.map((fila) => (
              <FilaEditable
                key={campoDeFila(fila)}
                fila={fila}
                textos={textos}
                motivo={detalle(campoDeFila(fila))}
                alCambiar={cambiar}
              />
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      {falta && <Typography color="error">{falta}</Typography>}
      <Stack direction="row" spacing={1}>
        <Button variant="contained" disabled={enviando} onClick={() => void guardar()}>
          Guardar
        </Button>
        <Button onClick={alCancelar}>Cancelar</Button>
      </Stack>
    </Stack>
  );
}

// Un selector nativo, para que el nombre accesible sea el de la regla (WCAG 4.1.2). Las reglas que
// solo dan parámetros no tienen severidad y no lo muestran.
function SelectorDeSeveridad({
  regla,
  severidad,
  motivo,
  alCambiar,
}: {
  regla: string;
  severidad: string | undefined;
  motivo: string | undefined;
  alCambiar: (severidad: string) => void;
}) {
  if (!severidad) {
    return null;
  }
  return (
    <TextField
      select
      label="Severidad"
      size="small"
      value={severidad}
      onChange={(evento) => alCambiar(evento.target.value)}
      error={Boolean(motivo)}
      helperText={motivo}
      slotProps={{
        select: { native: true },
        htmlInput: { 'aria-label': `${regla} · Severidad` },
      }}
    >
      {Object.entries(SEVERIDADES).map(([valor, nombre]) => (
        <option key={valor} value={valor}>
          {nombre}
        </option>
      ))}
    </TextField>
  );
}

function FilaEditable({
  fila,
  textos,
  motivo,
  alCambiar,
}: {
  fila: FilaCargaHoraria;
  textos: Textos;
  motivo: string | undefined;
  alCambiar: (clave: string, valor: string) => void;
}) {
  const perfil = nombreDelPerfil(fila.perfil);
  const idMotivo = `motivo-${fila.perfil}-${fila.aplicaA}`;
  return (
    <TableRow>
      <TableCell>
        {perfil}
        {motivo && (
          <Typography id={idMotivo} variant="body2" color="error">
            {motivo}
          </Typography>
        )}
      </TableCell>
      <TableCell>{tipoIst(fila.aplicaA)}</TableCell>
      {HORAS.map(({ campo, nombre }) => {
        const clave = `${campoDeFila(fila)}.${campo satisfies CampoDeHoras}`;
        return (
          <TableCell key={campo}>
            <TextField
              type="number"
              size="small"
              value={textos[clave] ?? ''}
              onChange={(evento) => alCambiar(clave, evento.target.value)}
              error={Boolean(motivo)}
              sx={{ width: '6rem' }}
              slotProps={{
                htmlInput: {
                  'aria-label': `${nombre} · ${perfil} · ${tipoIst(fila.aplicaA)}`,
                  'aria-describedby': motivo ? idMotivo : undefined,
                  min: 0,
                  step: 1,
                },
              }}
            />
          </TableCell>
        );
      })}
    </TableRow>
  );
}
