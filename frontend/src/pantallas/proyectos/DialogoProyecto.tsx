// El diálogo de registrar y corregir un proyecto aprobado (F03-H05, RF-18, carga 9), con sus
// carreras y sus participantes como listas en las que se agregan y se quitan filas. El código pasa
// a mayúsculas y pierde lo que no es letra, dígito o guion; las horas admiten hasta dos decimales.
// Al guardar, cada campo con un problema queda marcado con un motivo que dice qué hacer, antes de
// enviar nada (principio XIV); el servidor valida todo y su respuesta va junto a su campo o su fila.
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { Campo, ConErrores, Eleccion, ListaEditable, Seccion } from '../comun/Campos';
import { Avisos, Marco } from '../comun/Formulario';
import { SELECCION, ayuda, detalleDe } from '../comun/formulario';
import { campos as cuantos } from '../comun/validacion';
import { useEnvio } from '../comun/useEnvio';
import type { ReferenciasDeDocentes, ResumenDeDocente } from '../docentes/consultas';
import { corregirProyecto, registrarProyecto, type Proyecto } from './consultas';
import {
  NOMBRE_DEL_ESTADO_DEL_PROYECTO,
  NOMBRE_DEL_ROL,
  NOMBRE_DEL_TIPO_DE_PROYECTO,
} from './nombres';
import {
  CODIGO,
  HORAS,
  NOMBRE,
  PARTICIPANTE_VACIO,
  RESOLUCION,
  datosDe,
  erroresEn,
  nuevoDe,
  valoresDe,
  valoresVacios,
  type FilaDeParticipante,
  type ValoresDelProyecto,
} from './validacionDeProyecto';

const HORAS_PISTA = 'Hasta dos decimales, como 2,5.';

/** Los campos del formulario, con la posición en las listas, para los avisos del servidor. */
function camposDe(valores: ValoresDelProyecto) {
  return [
    'codigo',
    'nombre',
    'tipo',
    'carreras',
    ...valores.carreras.map((_, posicion) => `carreras[${String(posicion)}]`),
    'resolucionOcs',
    'fechaResolucion',
    'estado',
    'vigenciaInicio',
    'vigenciaFin',
    'director',
    'participantes',
    ...valores.participantes.flatMap((_, posicion) =>
      ['docente', 'rol', 'horas'].map((campo) => `participantes[${String(posicion)}].${campo}`),
    ),
    'horasAprobadas',
  ];
}

/** Registrar un proyecto (sin «proyecto») o corregirlo. */
export function DialogoProyecto({
  proyecto,
  docentes,
  referencias,
  alCerrar,
  alGuardar,
}: {
  proyecto?: Proyecto;
  docentes: readonly ResumenDeDocente[];
  referencias: ReferenciasDeDocentes | undefined;
  alCerrar: () => void;
  alGuardar: () => Promise<void>;
}) {
  const nuevo = !proyecto;
  const [valores, setValores] = useState<ValoresDelProyecto>(() =>
    proyecto ? valoresDe(proyecto) : valoresVacios(),
  );
  const [revisado, setRevisado] = useState(false);
  const { aviso, enviando, enviar } = useEnvio(alGuardar);
  const errores = revisado ? erroresEn(valores, nuevo) : {};
  const cantidad = Object.keys(errores).length;

  const cambiar = (campo: keyof ValoresDelProyecto, valor: string) =>
    setValores((actuales) => ({ ...actuales, [campo]: valor }));
  const cambiarCarrera = (posicion: number, valor: string) =>
    setValores((actuales) => ({
      ...actuales,
      carreras: actuales.carreras.map((una, otra) => (otra === posicion ? valor : una)),
    }));
  const cambiarParticipante = (posicion: number, campo: keyof FilaDeParticipante, valor: string) =>
    setValores((actuales) => ({
      ...actuales,
      participantes: actuales.participantes.map((uno, otra) =>
        otra === posicion ? { ...uno, [campo]: valor } : uno,
      ),
    }));

  const guardar = () => {
    setRevisado(true);
    if (Object.keys(erroresEn(valores, nuevo)).length) {
      return;
    }
    enviar(() =>
      proyecto
        ? corregirProyecto(proyecto.codigo, datosDe(valores))
        : registrarProyecto(nuevoDe(valores)),
    );
  };

  const opcionesDeDocentes = docentes.map(
    (uno) => [uno.identificacion, `${uno.nombreCompleto} · ${uno.identificacion}`] as const,
  );
  const carreras = referencias?.carreras ?? [];
  const errorDeLista = (campo: string) => errores[campo] ?? detalleDe(aviso, campo);

  return (
    <ConErrores errores={errores}>
      <Marco
        id="titulo-dialogo-proyecto"
        titulo={nuevo ? 'Registrar proyecto' : `Corregir ${proyecto.codigo}`}
        accion={nuevo ? 'Registrar' : 'Guardar'}
        enviando={enviando}
        alCerrar={alCerrar}
        alEnviar={guardar}
        ancho="md"
      >
        <Seccion titulo="Proyecto">
          <Campo
            etiqueta="Código"
            campo="codigo"
            valor={valores.codigo}
            filtro="codigo"
            largo={CODIGO}
            aviso={aviso}
            deshabilitado={!nuevo}
            pista={
              nuevo
                ? 'Mayúsculas, dígitos y guion, como INV-2025-01. No se puede cambiar después.'
                : 'No se puede cambiar.'
            }
            alCambiar={(valor) => cambiar('codigo', valor)}
          />
          <Campo
            etiqueta="Nombre"
            campo="nombre"
            valor={valores.nombre}
            largo={NOMBRE}
            aviso={aviso}
            alCambiar={(valor) => cambiar('nombre', valor)}
          />
          <Eleccion
            etiqueta="Tipo"
            campo="tipo"
            valor={valores.tipo}
            opciones={Object.entries(NOMBRE_DEL_TIPO_DE_PROYECTO)}
            aviso={aviso}
            alCambiar={(valor) => cambiar('tipo', valor)}
          />
          <Campo
            etiqueta="Resolución del OCS"
            campo="resolucionOcs"
            valor={valores.resolucionOcs}
            largo={RESOLUCION}
            aviso={aviso}
            alCambiar={(valor) => cambiar('resolucionOcs', valor)}
          />
          <Campo
            etiqueta="Fecha de la resolución"
            campo="fechaResolucion"
            tipo="date"
            valor={valores.fechaResolucion}
            aviso={aviso}
            alCambiar={(valor) => cambiar('fechaResolucion', valor)}
          />
          <Eleccion
            etiqueta="Estado"
            campo="estado"
            valor={valores.estado}
            opciones={Object.entries(NOMBRE_DEL_ESTADO_DEL_PROYECTO)}
            aviso={aviso}
            pista="Solo un proyecto aprobado o en ejecución recibe horas. Ninguno se borra."
            alCambiar={(valor) => cambiar('estado', valor)}
          />
          <Campo
            etiqueta="Inicio de la vigencia"
            campo="vigenciaInicio"
            tipo="date"
            valor={valores.vigenciaInicio}
            aviso={aviso}
            alCambiar={(valor) => cambiar('vigenciaInicio', valor)}
          />
          <Campo
            etiqueta="Fin de la vigencia"
            campo="vigenciaFin"
            tipo="date"
            valor={valores.vigenciaFin}
            aviso={aviso}
            alCambiar={(valor) => cambiar('vigenciaFin', valor)}
          />
          <Eleccion
            etiqueta="Director"
            campo="director"
            valor={valores.director}
            opciones={opcionesDeDocentes}
            aviso={aviso}
            alCambiar={(valor) => cambiar('director', valor)}
          />
          <Campo
            etiqueta="Horas semanales aprobadas"
            campo="horasAprobadas"
            valor={valores.horasAprobadas}
            filtro="decimal"
            largo={HORAS + 1}
            aviso={aviso}
            pista={`${HORAS_PISTA} Los participantes no suman más.`}
            alCambiar={(valor) => cambiar('horasAprobadas', valor)}
          />
        </Seccion>
        <ListaEditable
          titulo="Carreras"
          nombre="carrera"
          filas={valores.carreras}
          vacia="Sin carreras."
          agregar={() =>
            setValores((actuales) => ({ ...actuales, carreras: [...actuales.carreras, ''] }))
          }
          quitar={(posicion) =>
            setValores((actuales) => ({
              ...actuales,
              carreras: actuales.carreras.filter((_, otra) => otra !== posicion),
            }))
          }
          fila={(codigo, posicion) => {
            const campo = `carreras[${String(posicion)}]`;
            return (
              <TextField
                select
                label="Carrera"
                value={codigo}
                onChange={(evento) => cambiarCarrera(posicion, evento.target.value)}
                error={Boolean(errorDeLista(campo))}
                helperText={errores[campo] ?? ayuda(aviso, campo)}
                slotProps={SELECCION}
                sx={{ minWidth: 280 }}
              >
                <option value="">Elige una</option>
                {carreras.map((una) => (
                  <option key={una.codigo} value={una.codigo}>
                    {`${una.siglas} · ${una.nombre}`}
                  </option>
                ))}
              </TextField>
            );
          }}
        />
        {errorDeLista('carreras') && (
          <Typography color="error">{errorDeLista('carreras')}</Typography>
        )}
        <ListaEditable
          titulo="Participantes"
          nombre="participante"
          filas={valores.participantes}
          vacia="Sin participantes."
          agregar={() =>
            setValores((actuales) => ({
              ...actuales,
              participantes: [...actuales.participantes, { ...PARTICIPANTE_VACIO }],
            }))
          }
          quitar={(posicion) =>
            setValores((actuales) => ({
              ...actuales,
              participantes: actuales.participantes.filter((_, otra) => otra !== posicion),
            }))
          }
          fila={(uno, posicion) => {
            const base = `participantes[${String(posicion)}]`;
            return (
              <>
                <Eleccion
                  etiqueta="Docente"
                  campo={`${base}.docente`}
                  valor={uno.docente}
                  opciones={opcionesDeDocentes}
                  aviso={aviso}
                  alCambiar={(valor) => cambiarParticipante(posicion, 'docente', valor)}
                />
                <Eleccion
                  etiqueta="Rol"
                  campo={`${base}.rol`}
                  valor={uno.rol}
                  opciones={Object.entries(NOMBRE_DEL_ROL)}
                  aviso={aviso}
                  alCambiar={(valor) => cambiarParticipante(posicion, 'rol', valor)}
                />
                <Campo
                  etiqueta="Horas"
                  campo={`${base}.horas`}
                  valor={uno.horas}
                  filtro="decimal"
                  largo={HORAS + 1}
                  aviso={aviso}
                  pista={HORAS_PISTA}
                  alCambiar={(valor) => cambiarParticipante(posicion, 'horas', valor)}
                />
              </>
            );
          }}
        />
        {errorDeLista('participantes') && (
          <Typography color="error">{errorDeLista('participantes')}</Typography>
        )}
        {cantidad > 0 && (
          <Typography color="error" role="alert">
            {`Revisa ${cuantos(cantidad)} marcados en rojo.`}
          </Typography>
        )}
        <Avisos aviso={aviso} campos={camposDe(valores)} />
      </Marco>
    </ConErrores>
  );
}
