// Una lista con textos largos que se puede buscar. Una lista nativa del navegador no parte las líneas:
// mide lo que su texto más largo y se sale de la pantalla. Esta mide lo que el campo y las parte, y al
// elegir, el texto completo queda debajo: en el campo se ve cortado.
import Autocomplete from '@mui/material/Autocomplete';
import Box from '@mui/material/Box';
import ListSubheader from '@mui/material/ListSubheader';
import TextField from '@mui/material/TextField';
import type { ReactNode } from 'react';

export function SelectorLargo<Opcion>({
  etiqueta,
  opciones,
  elegida,
  claveDe,
  textoDe,
  grupoDe,
  alElegir,
  disabled,
  error,
  ayuda,
}: {
  etiqueta: string;
  opciones: readonly Opcion[];
  elegida: Opcion | null;
  claveDe: (opcion: Opcion) => string;
  textoDe: (opcion: Opcion) => string;
  /** Si llega, las opciones deben venir ya ordenadas por grupo. */
  grupoDe?: (opcion: Opcion) => string;
  alElegir: (opcion: Opcion | null) => void;
  disabled?: boolean;
  error?: boolean;
  ayuda?: ReactNode;
}) {
  return (
    <Autocomplete
      options={opciones}
      value={elegida}
      onChange={(_, nueva) => alElegir(nueva)}
      getOptionKey={claveDe}
      getOptionLabel={textoDe}
      isOptionEqualToValue={(una, otra) => claveDe(una) === claveDe(otra)}
      {...(grupoDe && { groupBy: grupoDe })}
      disabled={disabled ?? false}
      noOptionsText="Ninguna coincide con lo que escribiste."
      openText="Abrir la lista"
      closeText="Cerrar la lista"
      clearText="Borrar"
      // El marcado por defecto de los grupos no es válido para un lector de pantalla (WCAG 4.1.2).
      renderGroup={({ key, group, children }) => (
        <li key={key} role="presentation">
          <ListSubheader component="div" id={`${etiqueta}-grupo-${key}`}>
            {group}
          </ListSubheader>
          <Box
            component="ul"
            role="group"
            aria-labelledby={`${etiqueta}-grupo-${key}`}
            sx={{ m: 0, p: 0 }}
          >
            {children}
          </Box>
        </li>
      )}
      renderInput={(parametros) => (
        <TextField
          {...parametros}
          label={etiqueta}
          placeholder="Escribe para buscar o elige"
          error={error ?? false}
          helperText={ayuda}
          slotProps={{
            ...parametros.slotProps,
            inputLabel: { ...parametros.slotProps.inputLabel, shrink: true },
          }}
        />
      )}
    />
  );
}
