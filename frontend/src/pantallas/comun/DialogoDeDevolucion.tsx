// Devolver parciales desde la consolidación (F12-H05, D-98, contracts/spa.md): el Responsable marca
// una o varias carreras con el parcial enviado y escribe la observación de cada una, de 10 a 500
// caracteres sin los espacios de los extremos (RF-36). El servidor vuelve a validarlo todo.
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import FormControlLabel from '@mui/material/FormControlLabel';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { devolverParciales } from './flujo';

const MINIMO = 10;
const MAXIMO = 500;

function valida(texto: string) {
  const limpio = texto.trim();
  return limpio.length >= MINIMO && limpio.length <= MAXIMO;
}

/**
 * @param carreras las carreras con el parcial enviado, las que se pueden devolver
 * @param etapaActual el nombre de la etapa actual, adonde vuelve el distributivo al reenviar
 * @param alDevolver se llama después de devolver, para recargar
 */
export function DialogoDeDevolucion({
  periodo,
  carreras,
  etapaActual,
  alCerrar,
  alDevolver,
}: {
  periodo: string;
  carreras: readonly string[];
  etapaActual: string;
  alCerrar: () => void;
  alDevolver: () => Promise<void>;
}) {
  // Las marcadas, en el orden de la lista, con su observación.
  const [observaciones, setObservaciones] = useState<Record<string, string>>({});
  const [tocadas, setTocadas] = useState<Record<string, boolean>>({});
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [enviando, setEnviando] = useState(false);

  const marcadas = carreras.filter((carrera) => carrera in observaciones);
  const lista =
    marcadas.length > 0 && marcadas.every((carrera) => valida(observaciones[carrera] ?? ''));

  const marcar = (carrera: string, marcada: boolean) => {
    setObservaciones((antes) => {
      const despues = { ...antes };
      if (marcada) {
        despues[carrera] = '';
      } else {
        delete despues[carrera];
      }
      return despues;
    });
  };

  const devolver = async () => {
    setAviso(null);
    setEnviando(true);
    try {
      await devolverParciales(
        periodo,
        marcadas.map((carrera) => ({
          carrera,
          observacion: (observaciones[carrera] ?? '').trim(),
        })),
      );
      await alDevolver();
    } catch (fallo) {
      setAviso(traducirError(fallo));
      setEnviando(false);
    }
  };

  return (
    <Dialog open onClose={alCerrar} aria-labelledby="titulo-devolver" fullWidth maxWidth="sm">
      <DialogTitle id="titulo-devolver">Devolver parciales</DialogTitle>
      <DialogContent>
        <Typography variant="body2" sx={{ mb: 2 }}>
          {`Las carreras marcadas vuelven a editar su parcial y el distributivo vuelve a la propuesta de carrera. Cuando reenvíen, vuelve a ${etapaActual}.`}
        </Typography>
        {aviso && (
          <AvisoDeError mensaje={aviso}>
            {aviso.detalles.map((detalle) => (
              <Typography variant="body2" key={detalle.campo + detalle.descripcion}>
                {detalle.descripcion}
              </Typography>
            ))}
          </AvisoDeError>
        )}
        {carreras.map((carrera) => {
          const marcada = carrera in observaciones;
          const texto = observaciones[carrera] ?? '';
          const corta = tocadas[carrera] === true && !valida(texto);
          return (
            <Box key={carrera} sx={{ mb: 1 }}>
              <FormControlLabel
                control={
                  <Checkbox
                    checked={marcada}
                    onChange={(evento) => marcar(carrera, evento.target.checked)}
                  />
                }
                label={carrera}
              />
              {marcada && (
                <TextField
                  label={`Observación para ${carrera}`}
                  value={texto}
                  onChange={(evento) => {
                    setTocadas((antes) => ({ ...antes, [carrera]: true }));
                    setObservaciones((antes) => ({ ...antes, [carrera]: evento.target.value }));
                  }}
                  multiline
                  minRows={2}
                  required
                  fullWidth
                  error={corta}
                  helperText={`${corta ? 'Escribe qué tiene que corregir, de 10 a 500 caracteres. ' : ''}${String(texto.length)} de ${String(MAXIMO)} caracteres.`}
                  slotProps={{ htmlInput: { maxLength: MAXIMO } }}
                />
              )}
            </Box>
          );
        })}
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cancelar</Button>
        <Button variant="contained" disabled={!lista || enviando} onClick={() => void devolver()}>
          Devolver
        </Button>
      </DialogActions>
    </Dialog>
  );
}
