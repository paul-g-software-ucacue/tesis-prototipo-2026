// La pantalla «Preparación del periodo» (F02-H02, RF-56, glosario «Tablero de preparación»): el
// estado de cada carga en el periodo elegido, lo que falta para crear el distributivo y si está
// listo. La ven quienes leen el distributivo o las cargas (D-70); «Crear distributivo» solo aparece
// para quien escribe el distributivo (D-69), se habilita con el periodo listo y crea la versión 1 con
// un parcial por carrera (F06-H03). Un periodo con distributivo lo dice y deja de ofrecerlo.
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import MenuItem from '@mui/material/MenuItem';
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
import { useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import { tienePermiso } from '../../sesion/permisos';
import { useSesionActual } from '../../sesion/useSesionActual';
import { useEnvio } from '../comun/useEnvio';
import {
  CLAVE_DISTRIBUTIVO,
  crearDistributivo,
  useDistributivo,
  usePreparacion,
  type CargaDelPeriodo,
  type EstadoDeLaCarga,
  type LoQueFalta,
} from './consultas';

const ESTADO: Record<EstadoDeLaCarga, string> = {
  PENDIENTE: 'Pendiente',
  CARGADA: 'Cargada',
  CON_ERRORES: 'Con errores',
  SIN_PROYECTOS: 'Sin proyectos',
};

const MOTIVO: Record<string, string> = {
  CARGAS_CARGA_CON_ERRORES: 'su último archivo tenía errores en sus filas',
  CARGAS_VISTA_PREVIA_DESACTUALIZADA: 'su vista previa estaba desactualizada',
  CARGAS_CARGA_RECHAZADA: 'su servicio dueño la rechazó',
};

const OBLIGATORIA: Record<CargaDelPeriodo['obligatoriedad'], string> = {
  OBLIGATORIA: 'Sí',
  OBLIGATORIA_SALVO_SIN_PROYECTOS: 'Sí, o sin proyectos',
  RECOMENDADA: 'Recomendada',
};

const LO_QUE_FALTA = 'lo-que-falta-del-periodo';

function ChipDelEstado({ carga }: { carga: CargaDelPeriodo }) {
  const color =
    carga.estado === 'CARGADA'
      ? 'success'
      : carga.estado === 'CON_ERRORES'
        ? 'error'
        : carga.estado === 'SIN_PROYECTOS'
          ? 'primary'
          : 'default';
  return <Chip size="small" variant="outlined" color={color} label={ESTADO[carga.estado]} />;
}

/** Una línea de lo que falta, que dice qué hacer. */
function queFalta(falta: LoQueFalta): string {
  switch (falta.tipo) {
    case 'CARGA': {
      const nombre = falta.carga?.nombre ?? '';
      const estado = falta.estado ? ESTADO[falta.estado].toLowerCase() : 'pendiente';
      const sinProyectos =
        falta.carga?.codigo === 'proyectos_practicas'
          ? ', o confirma el periodo sin proyectos en «Plantillas de carga»'
          : '';
      return `${nombre}: está ${estado}. Súbela y confírmala en «Plantillas de carga»${sinProyectos}.`;
    }
    case 'PERIODO_SIN_ABRIR':
      return 'El periodo no está abierto: ábrelo con su versión de normativa en «Periodos».';
    case 'PERIODO_CERRADO':
      return 'El periodo está cerrado: no admite un distributivo nuevo.';
    case 'SIN_PERIODOS':
      return 'Todavía no hay periodos: carga antes los ciclos académicos.';
  }
}

/** «Crear distributivo», con lo que responde el servidor; cada periodo empieza sin avisos. */
function CrearDistributivo({ periodo, listo }: { periodo: string; listo: boolean }) {
  const consultas = useQueryClient();
  const { aviso, enviando, enviar } = useEnvio(() =>
    consultas.invalidateQueries({ queryKey: CLAVE_DISTRIBUTIVO }),
  );
  return (
    <>
      {aviso && <AvisoDeError mensaje={aviso} />}
      <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
        <Button
          variant="contained"
          disabled={!listo || enviando}
          aria-describedby={listo ? undefined : LO_QUE_FALTA}
          onClick={() => enviar(() => crearDistributivo(periodo))}
        >
          Crear distributivo
        </Button>
      </Stack>
    </>
  );
}

export function Preparacion() {
  useTitulo('Preparación del periodo');
  const [elegido, setElegido] = useState<string | null>(null);
  const preparacion = usePreparacion(elegido);
  const sesion = useSesionActual();
  const creaElDistributivo = tienePermiso(sesion.data?.permisos, 'DISTRIBUTIVO', 'ESCRITURA');
  const tablero = preparacion.data;
  const distributivo = useDistributivo(tablero?.periodo);
  const creado = distributivo.data;

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Preparación del periodo
      </Typography>
      <Typography sx={{ mb: 2 }}>
        Elige un periodo para ver qué cargas tiene y qué falta para crear su distributivo.
      </Typography>
      {preparacion.error && <AvisoDeError mensaje={traducirError(preparacion.error)} />}
      {distributivo.error && <AvisoDeError mensaje={traducirError(distributivo.error)} />}
      {tablero && tablero.periodos.length === 0 && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Todavía no hay periodos: carga antes los ciclos académicos en «Plantillas de carga».
        </Alert>
      )}
      {tablero && tablero.periodos.length > 0 && tablero.periodo && (
        <>
          <TextField
            select
            label="Periodo"
            value={tablero.periodo}
            onChange={(evento) => setElegido(evento.target.value)}
            sx={{ minWidth: 260, mb: 2 }}
          >
            {tablero.periodos.map((periodo) => (
              <MenuItem key={periodo.codigo} value={periodo.codigo}>
                {`${periodo.codigo} · ${periodo.nombre}`}
              </MenuItem>
            ))}
          </TextField>
          <TableContainer sx={{ mb: 2 }}>
            <Table aria-label={`Cargas del periodo ${tablero.periodo}`} size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Nº</TableCell>
                  <TableCell>Carga</TableCell>
                  <TableCell>Obligatoria</TableCell>
                  <TableCell>Estado</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {tablero.cargas.map((carga) => (
                  <TableRow key={carga.codigo} data-carga={carga.codigo}>
                    <TableCell>{carga.numero}</TableCell>
                    <TableCell>{carga.nombre}</TableCell>
                    <TableCell>{OBLIGATORIA[carga.obligatoriedad]}</TableCell>
                    <TableCell>
                      <ChipDelEstado carga={carga} />
                      {carga.motivo && (
                        <Typography variant="body2" component="span" sx={{ ml: 1 }}>
                          {`(${MOTIVO[carga.motivo] ?? carga.motivo})`}
                        </Typography>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          {creado && (
            <Alert severity="success" role="status" sx={{ mb: 2 }}>
              {`El periodo ${creado.periodo} ya tiene su distributivo, en la versión ${String(creado.version)}.`}
            </Alert>
          )}
          {!creado && tablero.listo && (
            <Alert severity="success" sx={{ mb: 2 }}>
              {`El periodo ${tablero.periodo} tiene todas sus cargas obligatorias: está listo para crear su distributivo.`}
            </Alert>
          )}
          {!creado && !tablero.listo && (
            <section aria-labelledby="titulo-de-lo-que-falta">
              <Typography id="titulo-de-lo-que-falta" component="h2" variant="h6">
                Qué falta para crear el distributivo
              </Typography>
              <ul id={LO_QUE_FALTA}>
                {tablero.faltan.map((falta) => (
                  <li key={`${falta.tipo}-${falta.carga?.codigo ?? ''}`}>{queFalta(falta)}</li>
                ))}
              </ul>
            </section>
          )}
          {creaElDistributivo && !distributivo.isPending && !creado && (
            <CrearDistributivo
              key={tablero.periodo}
              periodo={tablero.periodo}
              listo={tablero.listo}
            />
          )}
        </>
      )}
    </>
  );
}
