// El panel de los totales de un docente (F07-H01, RF-27, RN-05). Apartarse de la meta no es un
// error: solo se muestra. Si la asignación ya se guardó y los totales fallan, lo aclara (FR-026).
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import { useId } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError } from '../../errores/traducir';
import { porComponente, useCargaHoraria, type CargaHoraria } from './cargaHoraria';
import { numero } from './numeros';

function textoDeLaProporcion({ proporcion, meta }: CargaHoraria) {
  const objetivo = `${numero(meta.clase)}/${numero(meta.otrasDocencia)}`;
  return proporcion
    ? `Clase ${String(proporcion.clase)} % · Otras ${String(proporcion.otrasDocencia)} % (meta ${objetivo})`
    : `Todavía no hay proporción (meta ${objetivo}).`;
}

export function PanelDeCarga({
  periodo,
  docente,
  guardada = false,
}: {
  periodo: string;
  docente: string;
  /** Si el panel se muestra después de guardar una asignación. */
  guardada?: boolean;
}) {
  const carga = useCargaHoraria(periodo, docente);
  const idDelTitulo = useId();

  if (carga.error) {
    return (
      <>
        <AvisoDeError mensaje={traducirError(carga.error)}>
          <Button size="small" onClick={() => void carga.refetch()}>
            Volver a intentar
          </Button>
        </AvisoDeError>
        {guardada && (
          <Typography variant="body2">
            La asignación se guardó. Solo faltó cargar los totales.
          </Typography>
        )}
      </>
    );
  }
  if (!carga.data) {
    return <Typography role="status">Calculando los totales…</Typography>;
  }
  const datos = carga.data;
  return (
    <Box
      component="section"
      aria-labelledby={idDelTitulo}
      sx={{ border: 1, borderColor: 'divider', borderRadius: 1, p: 2 }}
    >
      <Typography id={idDelTitulo} component="h2" variant="subtitle1" gutterBottom>
        {`Totales de ${datos.nombreDocente}`}
      </Typography>
      <Typography variant="body2">
        {`Contratadas: ${String(datos.horasContratadas)} h · Asignadas: ${numero(datos.asignadas)} h · Disponibles: ${numero(datos.disponibles)} h`}
      </Typography>
      {datos.exceso > 0 && (
        <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
          {`Se pasó por ${numero(datos.exceso)} h.`}
        </Typography>
      )}
      {porComponente(datos.totales).map(([nombre, horas]) => (
        <Typography key={nombre} variant="body2">
          {`${nombre}: ${numero(horas)} h`}
        </Typography>
      ))}
      <Typography variant="body2" sx={{ mt: 1 }}>
        {textoDeLaProporcion(datos)}
      </Typography>
    </Box>
  );
}
