// La pantalla «Normativa» (F04-H01, RF-19): las versiones de normativa y, de la elegida, su
// cabecera, sus reglas con sus parámetros y la tabla de carga horaria, en español y de solo lectura.
// Crear y editar versiones es F04-H02. La ven quienes tienen «Parámetros y normativa: lectura»
// (D-65, D-66); el servidor lo valida en cada operación.
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
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import {
  useVersion,
  useVersiones,
  type FilaCargaHoraria,
  type VersionNormativa,
} from './consultas';
import { nombreDelPerfil } from './perfiles';
import { nombreDeLaRegla, nombreDelParametro } from './reglas';

const SIN_VALOR = '—';

const SEVERIDADES: Record<string, string> = { DURA: 'Dura', BLANDA: 'Blanda' };

/** «T2» → «Tipo 2»; «TODOS» → «Todos». */
function tipoIst(valor: string): string {
  return valor === 'TODOS' ? 'Todos' : `Tipo ${valor.slice(1)}`;
}

/** «2024-03-11» → «11/03/2024», sin pasar por la zona horaria. */
function fecha(iso: string): string {
  const [anio, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${anio}`;
}

function valor(horas: number | undefined): string {
  return horas === undefined ? SIN_VALOR : String(horas);
}

export function Normativa() {
  useTitulo('Normativa');
  const versiones = useVersiones();
  const [elegida, setElegida] = useState<number | undefined>(undefined);
  const numero = elegida ?? versiones.data?.[0]?.numero;
  const version = useVersion(numero);

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
                onClick={() => setElegida(resumen.numero)}
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
      {version.data && <DetalleDeVersion version={version.data} />}
    </>
  );
}

function DetalleDeVersion({ version }: { version: VersionNormativa }) {
  return (
    <>
      <Typography component="h2" variant="h5" sx={{ mt: 3 }} gutterBottom>
        {`Versión ${version.numero} · ${version.origen}`}
      </Typography>
      <List aria-label="Datos de la versión" dense>
        <Dato nombre="Código" valor={version.codigo} />
        <Dato nombre="Vigente desde" valor={fecha(version.vigenteDesde)} />
        <Dato nombre="Tipo de IST de la institución" valor={tipoIst(version.tipoIst)} />
      </List>

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
