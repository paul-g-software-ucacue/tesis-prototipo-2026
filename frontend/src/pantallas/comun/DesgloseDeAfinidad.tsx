// El puntaje de afinidad y de dónde sale (RF-17, RF-25): lo muestran igual la ficha del docente y la
// sugerencia de una asignatura-paralelo. Lo calcula Generación; aquí solo se lee.
import Typography from '@mui/material/Typography';
import type { components } from '../../api/generado/generacion';
import { puntos } from './numeros';

type PuntajeAfinidad = components['schemas']['PuntajeAfinidad'];
type AporteDeAfinidad = components['schemas']['AporteDeAfinidad'];
type TipoDeAporte = components['schemas']['TipoDeAporte'];

const NOMBRE_DEL_APORTE: Record<TipoDeAporte, string> = {
  TERCER_NIVEL: 'Título de tercer nivel',
  CUARTO_NIVEL: 'Título de cuarto nivel',
  EXPERIENCIA: 'Experiencia',
  CURSO: 'Curso',
};

function textoDelAporte({ tipo, nombre, coincidencia, aporte }: AporteDeAfinidad) {
  const campo = coincidencia === 'CAMPO_ESPECIFICO' ? 'campo específico' : 'solo campo amplio';
  return `${NOMBRE_DEL_APORTE[tipo]}: ${nombre} · ${campo} · ${puntos(aporte)}`;
}

/** Cada título, experiencia o curso que aporta al puntaje, y si se aplicó el tope de cursos. */
export function DesgloseDeAfinidad({ puntaje }: { puntaje: PuntajeAfinidad }) {
  return (
    <>
      {puntaje.desglose.length === 0 && (
        <Typography color="text.secondary">
          Nada de la ficha coincide con el campo de esta asignatura.
        </Typography>
      )}
      {puntaje.desglose.map((aporte) => (
        <Typography key={`${aporte.tipo}-${aporte.nombre}`}>{textoDelAporte(aporte)}</Typography>
      ))}
      {puntaje.topeAplicado && (
        <Typography color="text.secondary">
          Se aplicó el tope de cursos de la normativa: los demás cursos ya no suman.
        </Typography>
      )}
    </>
  );
}
