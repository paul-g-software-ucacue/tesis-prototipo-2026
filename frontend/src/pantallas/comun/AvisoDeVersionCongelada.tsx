// El aviso de la versión congelada (F13-H01, D-100), en «Consolidado del periodo» y en el panel de
// inconsistencias. Desde F14-H04 (D-105), con un rechazo en firma el documento ya no está en firma:
// la versión sigue congelada hasta la versión nueva (F12-H04). El rechazo viene del flujo; si no se
// puede leer, queda el aviso de la firma.
import Alert from '@mui/material/Alert';
import { useFlujo } from './flujo';

export function AvisoDeVersionCongelada({
  periodo,
  version,
}: {
  periodo: string;
  version: number | undefined;
}) {
  const flujo = useFlujo(periodo);
  const numero = String(version ?? '');
  return (
    <Alert severity="info" role="status" sx={{ my: 2 }}>
      {flujo.data?.rechazoEnFirma
        ? `La versión ${numero} sigue congelada: para cambiarla hace falta una versión nueva.`
        : `La versión ${numero} está congelada para la firma: ya no cambia.`}
    </Alert>
  );
}
