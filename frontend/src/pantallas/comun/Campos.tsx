// Los campos comunes de los formularios de los datos maestros (F03-H04, F03-H05): cada uno con su
// error y su pista, con lo que el formulario encontró mal antes de enviar (principio XIV) o, si no
// hay, con lo que respondió el servidor. Las listas editables agregan y quitan filas.
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { createContext, useContext, type ReactNode } from 'react';
import type { MensajeDeError } from '../../errores/traducir';
import { SELECCION, ayuda, detalleDe } from './formulario';
import { filtrar, type Filtro } from './validacion';

type Aviso = MensajeDeError | null;

/**
 * Lo que el formulario encontró mal en cada campo, por su nombre en la API. Va por contexto para no
 * pasarlo a cada campo junto con el aviso del servidor; su motivo ya dice qué hacer, sin la pista.
 */
const Errores = createContext<Record<string, string>>({});

/** Pasa a los campos de adentro lo que el formulario encontró mal en cada uno. */
export function ConErrores({
  errores,
  children,
}: {
  errores: Record<string, string>;
  children: ReactNode;
}) {
  return <Errores.Provider value={errores}>{children}</Errores.Provider>;
}

/** El error y el texto bajo un campo: primero lo del formulario y, si no hay, lo del servidor. */
function useMarca(aviso: Aviso, campo: string, pista?: string) {
  const propio = useContext(Errores)[campo];
  return {
    error: Boolean(propio ?? detalleDe(aviso, campo)),
    helperText: propio ?? ayuda(aviso, campo, pista),
  };
}

/** Una sección del formulario, con su título. */
export function Seccion({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <Box component="fieldset" sx={{ border: 0, p: 0, m: 0 }}>
      <Typography component="legend" variant="subtitle1" sx={{ fontWeight: 'bold', mb: 1 }}>
        {titulo}
      </Typography>
      <Stack spacing={2}>{children}</Stack>
    </Box>
  );
}

/**
 * Un campo de texto con su error y su pista. El filtro quita lo que no admite (letras en un número,
 * números en un nombre) y el largo corta lo que sobra, también al pegar.
 */
export function Campo({
  etiqueta,
  campo,
  valor,
  aviso,
  pista,
  tipo,
  filtro,
  largo,
  deshabilitado,
  alCambiar,
}: {
  etiqueta: string;
  campo: string;
  valor: string;
  aviso: Aviso;
  pista?: string;
  tipo?: 'date' | 'email';
  filtro?: Filtro;
  largo?: number;
  deshabilitado?: boolean;
  alCambiar: (valor: string) => void;
}) {
  const numerico = filtro === 'digitos' || filtro === 'decimal';
  const marca = useMarca(aviso, campo, pista);
  return (
    <TextField
      label={etiqueta}
      type={tipo}
      value={valor}
      disabled={deshabilitado}
      onChange={(evento) => alCambiar(filtrar(filtro, evento.target.value, largo))}
      {...marca}
      slotProps={{
        ...(tipo === 'date' && { inputLabel: { shrink: true } }),
        // Sin «maxLength»: el navegador cortaría lo pegado antes de quitarle guiones o espacios.
        htmlInput: numerico ? { inputMode: filtro === 'digitos' ? 'numeric' : 'decimal' } : {},
      }}
    />
  );
}

/** Una lista desplegable con su error y su pista. */
export function Eleccion({
  etiqueta,
  campo,
  valor,
  opciones,
  aviso,
  pista,
  vacia = 'Elige uno',
  deshabilitado,
  alCambiar,
}: {
  etiqueta: string;
  campo: string;
  valor: string;
  opciones: readonly (readonly [string, string])[];
  aviso: Aviso;
  pista?: string;
  vacia?: string;
  deshabilitado?: boolean;
  alCambiar: (valor: string) => void;
}) {
  const marca = useMarca(aviso, campo, pista);
  return (
    <TextField
      select
      label={etiqueta}
      value={valor}
      disabled={deshabilitado}
      onChange={(evento) => alCambiar(evento.target.value)}
      {...marca}
      slotProps={SELECCION}
    >
      <option value="">{vacia}</option>
      {opciones.map(([codigo, texto]) => (
        <option key={codigo} value={codigo}>
          {texto}
        </option>
      ))}
    </TextField>
  );
}

/** Una lista editable: una fila por elemento, con «Quitar», y «Agregar» al final. */
export function ListaEditable<F>({
  titulo,
  nombre,
  filas,
  vacia,
  agregar,
  quitar,
  fila,
}: {
  titulo: string;
  nombre: string;
  filas: readonly F[];
  vacia: string;
  agregar: () => void;
  quitar: (posicion: number) => void;
  fila: (elemento: F, posicion: number) => ReactNode;
}) {
  return (
    <Seccion titulo={titulo}>
      {filas.length === 0 && <Typography color="text.secondary">{vacia}</Typography>}
      {filas.map((elemento, posicion) => (
        <Stack
          // Las filas no tienen clave propia: su posición es su identidad mientras se edita.
          key={`${nombre}-${String(posicion)}`}
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1}
          sx={{ alignItems: { sm: 'flex-start' } }}
        >
          {fila(elemento, posicion)}
          <Button
            aria-label={`Quitar ${nombre} ${String(posicion + 1)}`}
            onClick={() => quitar(posicion)}
          >
            Quitar
          </Button>
        </Stack>
      ))}
      <Box>
        <Button variant="outlined" size="small" onClick={agregar}>
          {`Agregar ${nombre}`}
        </Button>
      </Box>
    </Seccion>
  );
}
