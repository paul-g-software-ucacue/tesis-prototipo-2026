// La pantalla «Bitácora» (F17-H02, RF-44): consultar quién cambió qué, cuándo, y el valor anterior
// y el nuevo, filtrando por fecha, usuario, tipo de cambio, identificador y periodo, para responder
// a una auditoría. Solo la ven quienes leen la bitácora (D-16: Vicerrector académico y
// Administrador); el servidor valida el permiso en cada consulta. Solo lee: ninguna entrada se
// cambia ni se borra (FR-006). Los filtros, la página y el corte viven en la dirección.
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Collapse from '@mui/material/Collapse';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TablePagination from '@mui/material/TablePagination';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { Fragment, useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import { Campo, ConErrores, Eleccion } from '../comun/Campos';
import {
  useAccionesDeBitacora,
  useBitacora,
  type EntradaConsultada,
  type ParametrosDeLaConsulta,
} from './consultas';
import {
  NOMBRE_DEL_SERVICIO,
  entradas,
  formatearMomento,
  lineas,
  resumen,
  type ServicioDeBitacora,
} from './nombres';
import {
  FILTROS_VACIOS,
  TAMANOS,
  aParametros,
  deParametros,
  erroresDelFiltro,
  limpiarIdentificador,
  limpiarPeriodo,
  limpiarUsuario,
  type FiltrosDeBitacora,
} from './validacionDeBitacora';

const SERVICIOS = Object.entries(NOMBRE_DEL_SERVICIO) as [ServicioDeBitacora, string][];

/** Lo que la dirección pide, como parámetros de la API: solo los filtros con valor. */
function consultaDe(leida: ReturnType<typeof deParametros>): ParametrosDeLaConsulta {
  const { filtros, pagina, tamano, corte } = leida;
  return {
    ...(filtros.desde && { desde: filtros.desde }),
    ...(filtros.hasta && { hasta: filtros.hasta }),
    ...(filtros.usuario && { usuario: filtros.usuario }),
    ...(filtros.servicio && { servicio: filtros.servicio as ServicioDeBitacora }),
    ...(filtros.accion && { accion: filtros.accion }),
    ...(filtros.identificador && { identificador: filtros.identificador }),
    ...(filtros.periodo && { periodo: filtros.periodo }),
    ...(pagina !== 1 && { pagina }),
    ...(tamano !== 50 && { tamano }),
    ...(corte && { corte }),
  };
}

export function Bitacora() {
  useTitulo('Bitácora');
  const [direccion, setDireccion] = useSearchParams();
  const leida = deParametros(direccion);
  const [filtros, setFiltros] = useState<FiltrosDeBitacora>(leida.filtros);
  const [errores, setErrores] = useState<Record<string, string>>({});
  const consulta = useBitacora(consultaDe(leida));
  const acciones = useAccionesDeBitacora();
  const aviso = consulta.error ? traducirError(consulta.error) : null;

  const cambiar = (campo: keyof FiltrosDeBitacora) => (valor: string) => {
    setFiltros((antes) => ({
      ...antes,
      [campo]: valor,
      // La acción es de su servicio: al cambiarlo, se elige de nuevo.
      ...(campo === 'servicio' && { accion: '' }),
    }));
  };

  const consultar = (evento: FormEvent) => {
    evento.preventDefault();
    const encontrados = erroresDelFiltro(filtros);
    setErrores(encontrados);
    if (Object.keys(encontrados).length === 0) {
      // Una consulta nueva vuelve a la primera página, con un corte nuevo.
      setDireccion(aParametros(filtros, 1, leida.tamano));
    }
  };

  const limpiar = () => {
    setFiltros(FILTROS_VACIOS);
    setErrores({});
    setDireccion(new URLSearchParams());
  };

  const irA = (pagina: number, tamano = leida.tamano) => {
    // Las páginas siguientes llevan el corte de la primera; un tamaño nuevo empieza de cero.
    const corte = tamano === leida.tamano ? (leida.corte ?? consulta.data?.corte) : undefined;
    setDireccion(aParametros(leida.filtros, pagina, tamano, corte));
  };

  const accionesDelServicio = (acciones.data ?? [])
    .filter((una) => una.servicio === filtros.servicio)
    .map((una) => [una.accion, una.accion] as const);

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Bitácora
      </Typography>
      <Typography component="p" sx={{ mb: 2 }}>
        Quién cambió qué y cuándo, con el valor anterior y el nuevo. Las fechas son de la hora de
        Ecuador continental.
      </Typography>
      {aviso && <AvisoDeError mensaje={aviso} />}
      {acciones.error && <AvisoDeError mensaje={traducirError(acciones.error)} />}
      <Box component="form" noValidate onSubmit={consultar} aria-label="Filtros de la bitácora">
        <ConErrores errores={errores}>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ my: 2 }} useFlexGap>
            <Campo
              etiqueta="Desde"
              campo="desde"
              tipo="date"
              valor={filtros.desde}
              aviso={aviso}
              alCambiar={cambiar('desde')}
            />
            <Campo
              etiqueta="Hasta"
              campo="hasta"
              tipo="date"
              valor={filtros.hasta}
              aviso={aviso}
              alCambiar={cambiar('hasta')}
            />
            <Campo
              etiqueta="Quién hizo el cambio"
              campo="usuario"
              valor={filtros.usuario}
              aviso={aviso}
              pista="El usuario del sistema, por ejemplo coordinacion.demo, o sistema."
              alCambiar={(valor) => cambiar('usuario')(limpiarUsuario(valor))}
            />
          </Stack>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ mb: 2 }} useFlexGap>
            <Eleccion
              etiqueta="Servicio"
              campo="servicio"
              valor={filtros.servicio}
              opciones={SERVICIOS}
              aviso={aviso}
              vacia="Todos"
              alCambiar={cambiar('servicio')}
            />
            <Eleccion
              etiqueta="Acción"
              campo="accion"
              valor={filtros.accion}
              opciones={accionesDelServicio}
              aviso={aviso}
              vacia="Todas"
              deshabilitado={!filtros.servicio}
              alCambiar={cambiar('accion')}
            />
            <Campo
              etiqueta="Identificador"
              campo="identificador"
              valor={filtros.identificador}
              aviso={aviso}
              pista="La cédula del docente o el código de la asignatura, actividad, cargo o carrera."
              alCambiar={(valor) => cambiar('identificador')(limpiarIdentificador(valor))}
            />
            <Campo
              etiqueta="Periodo"
              campo="periodo"
              valor={filtros.periodo}
              aviso={aviso}
              alCambiar={(valor) => cambiar('periodo')(limpiarPeriodo(valor))}
            />
          </Stack>
        </ConErrores>
        <Stack direction="row" spacing={2} sx={{ mb: 2 }}>
          <Button type="submit" variant="contained">
            Consultar
          </Button>
          <Button type="button" onClick={limpiar}>
            Limpiar
          </Button>
        </Stack>
      </Box>
      {consulta.data && (
        <>
          <Typography component="p" sx={{ mb: 1 }}>
            {entradas(consulta.data.total)}
          </Typography>
          {consulta.data.entradas.length === 0 ? (
            <Typography component="p">No hay entradas con estos filtros.</Typography>
          ) : (
            <TablaDeEntradas lista={consulta.data.entradas} />
          )}
          <TablePagination
            component="div"
            count={consulta.data.total}
            page={leida.pagina - 1}
            rowsPerPage={leida.tamano}
            rowsPerPageOptions={[...TAMANOS]}
            onPageChange={(_, pagina) => irA(pagina + 1)}
            onRowsPerPageChange={(evento) => irA(1, Number(evento.target.value))}
            labelRowsPerPage="Entradas por página"
            labelDisplayedRows={({ from, to, count }) =>
              `${String(from)}–${String(to)} de ${String(count)}`
            }
            getItemAriaLabel={(tipo) =>
              ({
                first: 'Primera página',
                last: 'Última página',
                next: 'Página siguiente',
                previous: 'Página anterior',
              })[tipo]
            }
          />
        </>
      )}
    </>
  );
}

function TablaDeEntradas({ lista }: { lista: EntradaConsultada[] }) {
  const [abierta, setAbierta] = useState<string | null>(null);
  return (
    <TableContainer>
      <Table aria-label="Entradas de la bitácora" size="small">
        <TableHead>
          <TableRow>
            <TableCell>Momento</TableCell>
            <TableCell>Usuario</TableCell>
            <TableCell>Servicio</TableCell>
            <TableCell>Acción</TableCell>
            <TableCell>Periodo</TableCell>
            <TableCell>Objeto</TableCell>
            <TableCell>
              <Box component="span" sx={{ position: 'absolute', left: -10_000 }}>
                Valores
              </Box>
            </TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {lista.map((entrada) => {
            const expandida = abierta === entrada.id;
            const detalle = `valores-${entrada.id}`;
            return (
              <Fragment key={entrada.id}>
                <TableRow>
                  <TableCell data-momento>{formatearMomento(entrada.momento)}</TableCell>
                  <TableCell>{entrada.usuario}</TableCell>
                  <TableCell>{NOMBRE_DEL_SERVICIO[entrada.servicio]}</TableCell>
                  <TableCell>{entrada.accion}</TableCell>
                  <TableCell>{entrada.periodo ?? '—'}</TableCell>
                  <TableCell>{resumen(entrada.objeto)}</TableCell>
                  <TableCell>
                    <Button
                      size="small"
                      aria-expanded={expandida}
                      aria-controls={detalle}
                      onClick={() => setAbierta(expandida ? null : entrada.id)}
                    >
                      {expandida ? 'Ocultar valores' : 'Ver valores'}
                    </Button>
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell colSpan={7} sx={{ py: 0, borderBottom: expandida ? undefined : 0 }}>
                    <Collapse in={expandida} unmountOnExit>
                      <Box
                        id={detalle}
                        role="region"
                        aria-label="Valores de la entrada"
                        sx={{ py: 2 }}
                      >
                        <Stack direction={{ xs: 'column', md: 'row' }} spacing={4}>
                          <Valores titulo="Objeto" valores={entrada.objeto} />
                          <Valores
                            titulo="Valor anterior"
                            valores={entrada.anterior}
                            sinValores="Sin valor anterior (se creó)"
                          />
                          <Valores
                            titulo="Valor nuevo"
                            valores={entrada.nuevo}
                            sinValores="Sin valor nuevo (se quitó)"
                          />
                        </Stack>
                      </Box>
                    </Collapse>
                  </TableCell>
                </TableRow>
              </Fragment>
            );
          })}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

function Valores({
  titulo,
  valores,
  sinValores,
}: {
  titulo: string;
  valores: Record<string, unknown> | undefined;
  sinValores?: string;
}) {
  const id = `titulo-${titulo.replace(/\s/g, '-')}`;
  return (
    <Box role="group" aria-labelledby={id} sx={{ minWidth: 200 }}>
      <Typography id={id} component="h2" variant="subtitle2" sx={{ fontWeight: 'bold' }}>
        {titulo}
      </Typography>
      {valores ? (
        lineas(valores).map((linea, posicion) => (
          <Typography
            // Las líneas no cambian de orden: la posición las identifica.
            key={posicion}
            variant="body2"
            sx={{ pl: linea.nivel * 2 }}
          >
            {linea.texto}
          </Typography>
        ))
      ) : (
        <Typography variant="body2">{sinValores}</Typography>
      )}
    </Box>
  );
}
