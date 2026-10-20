// La pantalla «Normativa» (RF-19): las versiones de normativa y, de la elegida, su cabecera, sus
// reglas con sus parámetros y la tabla de carga horaria, en español (F04-H01). La ven quienes tienen
// «Parámetros y normativa: lectura» (D-65, D-66). Con escritura (D-67, F04-H02) se crea una versión
// nueva a partir de la elegida, se edita en pantalla la que no rige ningún periodo y se asocia a un
// periodo; la que rige uno se ve en solo lectura. El servidor valida cada operación.
import Button from '@mui/material/Button';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemText from '@mui/material/ListItemText';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import { tienePermiso } from '../../sesion/permisos';
import { useSesionActual } from '../../sesion/useSesionActual';
import {
  CLAVE_NORMATIVA,
  useVersion,
  useVersiones,
  type FilaCargaHoraria,
  type VersionNormativa,
} from './consultas';
import { DialogoAsociar, DialogoVersionNueva } from './DialogosDeVersion';
import { EdicionDeVersion } from './EdicionDeVersion';
import { fecha, SEVERIDADES, SIN_VALOR, tipoIst } from './formato';
import { nombreDelPerfil } from './perfiles';
import { AFINIDAD, nombreDeLaRegla, nombreDelParametro } from './reglas';

/** Lo que está abierto sobre la versión elegida. */
type Abierto = 'edicion' | 'version-nueva' | 'asociar' | null;

function valor(horas: number | undefined): string {
  return horas === undefined ? SIN_VALOR : String(horas);
}

export function Normativa() {
  useTitulo('Normativa');
  const sesion = useSesionActual();
  const escritura = tienePermiso(sesion.data?.permisos, 'PARAMETROS', 'ESCRITURA');
  const consultas = useQueryClient();
  const versiones = useVersiones();
  const [elegida, setElegida] = useState<number | undefined>(undefined);
  const [abierto, setAbierto] = useState<Abierto>(null);
  // Al abrir, la de número más alto: la más reciente (F04-H02).
  const numero = elegida ?? versiones.data?.at(-1)?.numero;
  const version = useVersion(numero);
  const recargar = () => consultas.invalidateQueries({ queryKey: CLAVE_NORMATIVA });

  const error = versiones.error ?? version.error;
  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Normativa
      </Typography>
      {error && <AvisoDeError mensaje={traducirError(error)} />}
      {versiones.data && (
        <List aria-label="Versiones" dense sx={{ maxWidth: 480 }}>
          {versiones.data.map((resumen) => (
            // Cada botón en su <li>: un <ul> solo puede tener elementos de lista (WCAG 1.3.1).
            <ListItem key={resumen.numero} disablePadding>
              <ListItemButton
                selected={resumen.numero === numero}
                onClick={() => {
                  setElegida(resumen.numero);
                  setAbierto(null);
                }}
              >
                <ListItemText
                  primary={`Versión ${resumen.numero}`}
                  secondary={`${resumen.origen} · vigente desde ${fecha(resumen.vigenteDesde)}`}
                />
              </ListItemButton>
            </ListItem>
          ))}
        </List>
      )}
      {version.data && (
        <>
          <CabeceraDeVersion version={version.data} />
          {escritura && abierto !== 'edicion' && (
            <Stack direction="row" spacing={1} sx={{ my: 1, flexWrap: 'wrap' }}>
              <Button variant="outlined" onClick={() => setAbierto('version-nueva')}>
                Crear versión nueva
              </Button>
              {version.data.periodos.length === 0 && (
                <Button variant="outlined" onClick={() => setAbierto('edicion')}>
                  Editar
                </Button>
              )}
              <Button variant="outlined" onClick={() => setAbierto('asociar')}>
                Asociar a un periodo
              </Button>
            </Stack>
          )}
          {abierto === 'edicion' ? (
            <EdicionDeVersion
              key={version.data.numero}
              version={version.data}
              alCancelar={() => setAbierto(null)}
              alGuardar={async () => {
                await recargar();
                setAbierto(null);
              }}
            />
          ) : (
            <DetalleDeVersion version={version.data} />
          )}
          {abierto === 'version-nueva' && (
            <DialogoVersionNueva
              numero={version.data.numero}
              alCerrar={() => setAbierto(null)}
              alCrear={async (creada) => {
                await recargar();
                setElegida(creada.numero);
                setAbierto(null);
              }}
            />
          )}
          {abierto === 'asociar' && (
            <DialogoAsociar
              numero={version.data.numero}
              alCerrar={() => setAbierto(null)}
              alAsociar={async () => {
                await recargar();
                setAbierto(null);
              }}
            />
          )}
        </>
      )}
    </>
  );
}

function CabeceraDeVersion({ version }: { version: VersionNormativa }) {
  const enUso = version.periodos.length > 0;
  return (
    <>
      <Typography component="h2" variant="h5" sx={{ mt: 3 }} gutterBottom>
        {`Versión ${version.numero} · ${version.origen}`}
      </Typography>
      <List aria-label="Datos de la versión" dense>
        <Dato nombre="Código" valor={version.codigo} />
        <Dato nombre="Vigente desde" valor={fecha(version.vigenteDesde)} />
        <Dato nombre="Tipo de IST de la institución" valor={tipoIst(version.tipoIst)} />
        {version.copiadaDe !== undefined && (
          <Dato nombre="Origen de la copia" valor={`Copiada de la versión ${version.copiadaDe}`} />
        )}
        <Dato
          nombre="Periodos"
          valor={enUso ? `Rige: ${version.periodos.join(', ')}` : 'No rige ningún periodo'}
        />
      </List>
      {enUso && (
        <Typography variant="body2" color="text.secondary">
          Esta versión rige un periodo y ya no se puede cambiar. Para cambiarla, crea una versión
          nueva a partir de ella.
        </Typography>
      )}
    </>
  );
}

// Los pesos y el mínimo de afinidad de la versión (F04-H03), si los tiene.
function Afinidad({ version }: { version: VersionNormativa }) {
  const datos = AFINIDAD.flatMap(([regla, nombre]) => {
    const parametro = version.reglas
      .find((una) => una.identificador === regla)
      ?.parametros.find((uno) => uno.nombre === nombre);
    return parametro ? [{ nombre, valor: parametro.valor }] : [];
  });
  if (datos.length === 0) {
    return null;
  }
  return (
    <>
      <Typography component="h3" variant="h6" sx={{ mt: 2 }}>
        Afinidad
      </Typography>
      <List aria-label="Afinidad" dense>
        {datos.map(({ nombre, valor: cifra }) => (
          <Dato key={nombre} nombre={nombreDelParametro(nombre)} valor={String(cifra)} />
        ))}
      </List>
    </>
  );
}

function DetalleDeVersion({ version }: { version: VersionNormativa }) {
  return (
    <>
      <Afinidad version={version} />
      <Typography component="h3" variant="h6" sx={{ mt: 2 }}>
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
              <TableCell>{regla.severidad ? SEVERIDADES[regla.severidad] : SIN_VALOR}</TableCell>
              <TableCell>
                {regla.parametros.length === 0 && SIN_VALOR}
                {regla.parametros.map((parametro) => (
                  <div key={parametro.nombre}>
                    {`${nombreDelParametro(parametro.nombre)}: ${parametro.valor}`}
                  </div>
                ))}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      <Typography component="h3" variant="h6" sx={{ mt: 3 }}>
        Tabla de carga horaria
      </Typography>
      <Typography variant="body2" color="text.secondary">
        Horas semanales por perfil y tipo de IST. «{SIN_VALOR}» es un valor que la política no da.
      </Typography>
      <TableContainer>
        <Table aria-label="Tabla de carga horaria" size="small">
          <TableHead>
            <TableRow>
              <TableCell>Perfil</TableCell>
              <TableCell>Tipo de IST</TableCell>
              <TableCell>Dedicación</TableCell>
              <TableCell>Clase</TableCell>
              <TableCell>Rango de clase</TableCell>
              <TableCell>Otras de docencia</TableCell>
              <TableCell>Gestión</TableCell>
              <TableCell>Investigación y vinculación (máx.)</TableCell>
              <TableCell>Severidad</TableCell>
              <TableCell>Referencia</TableCell>
              <TableCell>Observación</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {version.tablaCargaHoraria.map((fila) => (
              <FilaDeLaTabla key={`${fila.aplicaA}-${fila.perfil}`} fila={fila} />
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
}

function FilaDeLaTabla({ fila }: { fila: FilaCargaHoraria }) {
  return (
    <TableRow>
      <TableCell>{nombreDelPerfil(fila.perfil)}</TableCell>
      <TableCell>{tipoIst(fila.aplicaA)}</TableCell>
      <TableCell>{fila.dedicacionTotal}</TableCell>
      <TableCell>{fila.horasClase}</TableCell>
      <TableCell>{`${fila.horasClaseMinimo}–${fila.horasClaseMaximo}`}</TableCell>
      <TableCell>{valor(fila.horasOtrasDocencia)}</TableCell>
      <TableCell>{valor(fila.horasGestion)}</TableCell>
      <TableCell>{valor(fila.horasInvestigacionVinculacionMaximo)}</TableCell>
      <TableCell>{SEVERIDADES[fila.severidad]}</TableCell>
      <TableCell>{fila.referencia}</TableCell>
      <TableCell>{fila.observacion ?? SIN_VALOR}</TableCell>
    </TableRow>
  );
}

function Dato({ nombre, valor: texto }: { nombre: string; valor: string }) {
  return (
    <li>
      <Typography component="span" sx={{ fontWeight: 500 }}>
        {nombre}:
      </Typography>{' '}
      {texto}
    </li>
  );
}
