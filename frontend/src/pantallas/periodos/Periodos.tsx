// La pantalla «Periodos» (F02-H01, RF-13): los periodos académicos con sus fechas, su estado y la
// versión de normativa que los rige. La ven quienes tienen «Distributivo del periodo: lectura»; con
// su escritura (el Vicerrector académico, D-69) se registra un periodo, se corrige, se abre
// eligiendo su versión de normativa y se pasa al estado siguiente. Cerrar pide confirmación. Desde
// F05-H03, cada periodo muestra su Responsable de consolidación, que el Vicerrector designa aquí.
// El servidor valida cada operación.
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
import { useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import { tienePermiso } from '../../sesion/permisos';
import { useSesionActual } from '../../sesion/useSesionActual';
import { CLAVE_NORMATIVA } from '../normativa/consultas';
import { fecha } from '../normativa/formato';
import {
  CLAVE_PERIODOS,
  CLAVE_RESPONSABLES,
  cambiarEstadoPeriodo,
  usePeriodos,
  useResponsables,
  type EstadoPeriodo,
  type PeriodoAcademico,
} from './consultas';
import {
  DialogoAbrir,
  DialogoCerrar,
  DialogoDatosDelPeriodo,
  DialogoDesignar,
} from './DialogosDePeriodo';
import { NOMBRE_DEL_ESTADO, siguiente } from './estados';

/** Lo que está abierto: un diálogo, sobre un periodo o para uno nuevo. */
type Abierto =
  | { que: 'registrar' }
  | { que: 'corregir' | 'abrir' | 'cerrar' | 'designar'; periodo: PeriodoAcademico }
  | null;

export function Periodos() {
  useTitulo('Periodos');
  const sesion = useSesionActual();
  const escritura = tienePermiso(sesion.data?.permisos, 'DISTRIBUTIVO', 'ESCRITURA');
  const consultas = useQueryClient();
  const periodos = usePeriodos();
  const responsables = useResponsables();
  const responsableDe = (codigo: string) =>
    responsables.data?.find((responsable) => responsable.periodo === codigo)?.usuario;
  const [abierto, setAbierto] = useState<Abierto>(null);
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);

  // Abrir un periodo también cambia la versión en «Normativa»: se recargan las dos.
  const recargar = async () => {
    await Promise.all([
      consultas.invalidateQueries({ queryKey: CLAVE_PERIODOS }),
      consultas.invalidateQueries({ queryKey: CLAVE_NORMATIVA }),
      consultas.invalidateQueries({ queryKey: CLAVE_RESPONSABLES }),
    ]);
  };
  const terminar = async () => {
    await recargar();
    setAbierto(null);
  };

  const pasar = async (periodo: PeriodoAcademico, destino: EstadoPeriodo) => {
    setAviso(null);
    setAbierto(null);
    try {
      await cambiarEstadoPeriodo(periodo.codigo, destino);
      await recargar();
    } catch (fallo) {
      setAviso(traducirError(fallo));
    }
  };

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Periodos
      </Typography>
      {periodos.error && <AvisoDeError mensaje={traducirError(periodos.error)} />}
      {aviso && <AvisoDeError mensaje={aviso} />}
      {escritura && (
        <Button variant="outlined" sx={{ my: 1 }} onClick={() => setAbierto({ que: 'registrar' })}>
          Registrar periodo
        </Button>
      )}
      {periodos.data && (
        <TableContainer>
          <Table aria-label="Periodos" size="small">
            <TableHead>
              <TableRow>
                <TableCell>Código</TableCell>
                <TableCell>Nombre</TableCell>
                <TableCell>Inicio</TableCell>
                <TableCell>Fin</TableCell>
                <TableCell>Estado</TableCell>
                <TableCell>Versión de normativa</TableCell>
                <TableCell>Responsable de consolidación</TableCell>
                {escritura && <TableCell>Acciones</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {periodos.data.map((periodo) => (
                <TableRow key={periodo.codigo}>
                  <TableCell>{periodo.codigo}</TableCell>
                  <TableCell>{periodo.nombre}</TableCell>
                  <TableCell>{fecha(periodo.inicio)}</TableCell>
                  <TableCell>{fecha(periodo.fin)}</TableCell>
                  <TableCell>{NOMBRE_DEL_ESTADO[periodo.estado]}</TableCell>
                  <TableCell>
                    {periodo.version === undefined ? 'Sin versión' : `Versión ${periodo.version}`}
                  </TableCell>
                  <TableCell>{responsableDe(periodo.codigo) ?? 'Sin designar'}</TableCell>
                  {escritura && (
                    <TableCell>
                      <Acciones
                        periodo={periodo}
                        alAbrir={(que) => setAbierto({ que, periodo })}
                        alPasar={(destino) => void pasar(periodo, destino)}
                      />
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
      {abierto?.que === 'registrar' && (
        <DialogoDatosDelPeriodo alCerrar={() => setAbierto(null)} alGuardar={terminar} />
      )}
      {abierto?.que === 'corregir' && (
        <DialogoDatosDelPeriodo
          periodo={abierto.periodo}
          alCerrar={() => setAbierto(null)}
          alGuardar={terminar}
        />
      )}
      {abierto?.que === 'abrir' && (
        <DialogoAbrir
          periodo={abierto.periodo}
          alCerrar={() => setAbierto(null)}
          alTerminar={terminar}
        />
      )}
      {abierto?.que === 'designar' && (
        <DialogoDesignar
          periodo={abierto.periodo}
          actual={responsableDe(abierto.periodo.codigo)}
          alCerrar={() => setAbierto(null)}
          alTerminar={terminar}
        />
      )}
      {abierto?.que === 'cerrar' && (
        <DialogoCerrar
          periodo={abierto.periodo}
          alCerrar={() => setAbierto(null)}
          alConfirmar={() => void pasar(abierto.periodo, 'CERRADO')}
        />
      )}
    </>
  );
}

// Las acciones de un periodo: cada botón nombra el periodo, para que se distingan en la tabla.
function Acciones({
  periodo,
  alAbrir,
  alPasar,
}: {
  periodo: PeriodoAcademico;
  alAbrir: (que: 'corregir' | 'abrir' | 'cerrar' | 'designar') => void;
  alPasar: (destino: EstadoPeriodo) => void;
}) {
  const destino = siguiente(periodo.estado);
  const sinVersion = periodo.version === undefined;
  const bloqueado = periodo.estado === 'PLANIFICACION' && sinVersion;
  return (
    <Stack spacing={0.5} sx={{ alignItems: 'flex-start' }}>
      <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
        {periodo.estado !== 'CERRADO' && (
          <Button
            size="small"
            aria-label={`Corregir ${periodo.codigo}`}
            onClick={() => alAbrir('corregir')}
          >
            Corregir
          </Button>
        )}
        {periodo.estado !== 'CERRADO' && (
          <Button
            size="small"
            aria-label={`Designar responsable ${periodo.codigo}`}
            onClick={() => alAbrir('designar')}
          >
            Designar responsable
          </Button>
        )}
        {bloqueado && (
          <Button
            size="small"
            aria-label={`Abrir ${periodo.codigo}`}
            onClick={() => alAbrir('abrir')}
          >
            Abrir
          </Button>
        )}
        {destino && (
          <Button
            size="small"
            aria-label={`Pasar ${periodo.codigo} a ${NOMBRE_DEL_ESTADO[destino]}`}
            disabled={bloqueado}
            onClick={() => (destino === 'CERRADO' ? alAbrir('cerrar') : alPasar(destino))}
          >
            {`Pasar a ${NOMBRE_DEL_ESTADO[destino]}`}
          </Button>
        )}
      </Stack>
      {bloqueado && (
        <Typography variant="body2" color="text.secondary">
          Ábrelo antes con su versión de normativa.
        </Typography>
      )}
    </Stack>
  );
}
