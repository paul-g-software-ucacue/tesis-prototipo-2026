// La vista previa de una carga (F01-H04, RF-10, RF-55): cuántos registros son nuevos, cuántos
// cambian, cuántos se dan de baja y cuántos no cambian frente a lo que hay en el sistema, con los
// primeros 100 de cada grupo y, en los que cambian, sus campos. Las bajas se informan, pero no se
// aplican. «Confirmar» vuelve a enviar el archivo con la huella; «Cancelar» no cambia nada.
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Tab from '@mui/material/Tab';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Tabs from '@mui/material/Tabs';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import type { RegistroDeLaVista, VistaPrevia } from './consultas';

type Grupo = RegistroDeLaVista['tipo'];

const PESTANAS: { tipo: Grupo; titulo: string }[] = [
  { tipo: 'NUEVO', titulo: 'Nuevos' },
  { tipo: 'CAMBIA', titulo: 'Cambian' },
  { tipo: 'BAJA', titulo: 'Se dan de baja' },
];

/** «1 registro», «3 registros». */
function cuantos(numero: number, uno: string, varios: string): string {
  return `${numero} ${numero === 1 ? uno : varios}`;
}

interface Propiedades {
  archivo: string;
  vista: VistaPrevia;
  confirmando: boolean;
  alConfirmar: () => void;
  alCancelar: () => void;
}

export function VistaPreviaDeLaCarga({
  archivo,
  vista,
  confirmando,
  alConfirmar,
  alCancelar,
}: Propiedades) {
  const [grupo, setGrupo] = useState<Grupo>('NUEVO');
  const conErrores = vista.filasConErrores > 0 || vista.huella === undefined;
  const totales: Record<Grupo, number> = {
    NUEVO: vista.nuevos ?? 0,
    CAMBIA: vista.cambian ?? 0,
    BAJA: vista.bajas ?? 0,
  };
  const delGrupo = vista.registros.filter((registro) => registro.tipo === grupo);
  return (
    <Box component="section" aria-labelledby="titulo-de-la-vista-previa" sx={{ mb: 2 }}>
      <Typography id="titulo-de-la-vista-previa" component="h2" variant="h6">
        {`Vista previa de ${archivo}`}
      </Typography>
      {conErrores ? (
        <Typography sx={{ mb: 1 }}>
          {`${cuantos(vista.filasConErrores, 'fila tiene', 'filas tienen')} errores: corrígelas para poder confirmar.`}
        </Typography>
      ) : (
        <>
          <Typography sx={{ mb: 1 }}>
            {`Si confirmas: ${cuantos(totales.NUEVO, 'nuevo', 'nuevos')}, ${cuantos(totales.CAMBIA, 'cambia', 'cambian')}, ${cuantos(totales.BAJA, 'se da de baja', 'se dan de baja')} y ${cuantos(vista.iguales ?? 0, 'queda igual', 'quedan iguales')}.`}
          </Typography>
          {totales.BAJA > 0 && (
            <Alert severity="info" role="note" sx={{ mb: 1 }}>
              Las bajas no se aplican: hazlas a mano en su pantalla.
            </Alert>
          )}
          <Tabs
            value={grupo}
            onChange={(_evento, nuevo: Grupo) => setGrupo(nuevo)}
            aria-label={`Registros de ${archivo}`}
          >
            {PESTANAS.map((pestana) => (
              <Tab
                key={pestana.tipo}
                value={pestana.tipo}
                label={`${pestana.titulo} (${totales[pestana.tipo]})`}
                id={`pestana-${pestana.tipo}`}
                aria-controls={`panel-${pestana.tipo}`}
              />
            ))}
          </Tabs>
          <Box
            role="tabpanel"
            id={`panel-${grupo}`}
            aria-labelledby={`pestana-${grupo}`}
            sx={{ mb: 1 }}
          >
            {delGrupo.length === 0 ? (
              <Typography sx={{ py: 1 }}>Ninguno.</Typography>
            ) : (
              <TableContainer>
                <Table size="small" aria-label={PESTANAS.find((p) => p.tipo === grupo)?.titulo}>
                  <TableHead>
                    <TableRow>
                      <TableCell>Clave</TableCell>
                      <TableCell>Fila</TableCell>
                      {grupo === 'CAMBIA' && <TableCell>Cambios</TableCell>}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {delGrupo.map((registro) => (
                      <TableRow key={`${registro.tipo}-${registro.clave}`}>
                        <TableCell>{registro.clave}</TableCell>
                        <TableCell>{registro.fila ?? '—'}</TableCell>
                        {grupo === 'CAMBIA' && (
                          <TableCell>
                            <ul style={{ margin: 0, paddingLeft: '1.2em' }}>
                              {registro.cambios.map((cambio) => (
                                <li key={cambio.campo}>
                                  <code>{cambio.campo}</code>
                                  {`: «${cambio.ahora || 'vacío'}» → «${cambio.archivo || 'vacío'}»`}
                                </li>
                              ))}
                            </ul>
                          </TableCell>
                        )}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
            {totales[grupo] > delGrupo.length && (
              <Typography variant="body2">
                {`Se muestran ${delGrupo.length} de ${totales[grupo]}.`}
              </Typography>
            )}
          </Box>
        </>
      )}
      <Stack direction="row" spacing={1}>
        <Button
          variant="contained"
          disabled={conErrores || confirmando}
          onClick={alConfirmar}
          aria-describedby={conErrores ? 'por-que-no-se-confirma' : undefined}
        >
          Confirmar
        </Button>
        <Button variant="outlined" onClick={alCancelar} disabled={confirmando}>
          Cancelar
        </Button>
      </Stack>
      {conErrores && (
        <Typography id="por-que-no-se-confirma" variant="body2" sx={{ mt: 1 }}>
          Un archivo con errores no se confirma: la carga se aplica completa o nada.
        </Typography>
      )}
    </Box>
  );
}
