// Una huella SHA-256 abreviada con un botón para copiarla entera (F13-H01, F13-H02). «Huella
// copiada» se anuncia en una región viva.
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useState } from 'react';

// La huella abreviada: los 12 primeros caracteres; la completa se copia (contracts/spa.md).
const LARGO_DE_LA_HUELLA = 12;

/**
 * @param etiqueta lo que va antes de la huella: «Huella» o «Huella del PDF»
 * @param hash el SHA-256 en hexadecimal, 64 caracteres
 */
export function HuellaCopiable({ etiqueta, hash }: { etiqueta: string; hash: string }) {
  const [copiada, setCopiada] = useState(false);
  const copiar = async () => {
    await navigator.clipboard.writeText(hash);
    setCopiada(true);
  };
  return (
    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
      <Typography variant="body2" title={hash}>
        {`${etiqueta}: ${hash.slice(0, LARGO_DE_LA_HUELLA)}…`}
      </Typography>
      <Button size="small" onClick={() => void copiar()}>
        Copiar la huella
      </Button>
      <Typography variant="body2" role="status" color="text.secondary">
        {copiada ? 'Huella copiada' : ''}
      </Typography>
    </Stack>
  );
}
