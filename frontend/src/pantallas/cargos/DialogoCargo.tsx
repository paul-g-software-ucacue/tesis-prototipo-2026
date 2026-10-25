// El diálogo de registrar, corregir y finalizar un cargo (F03-H05, carga 8). El cargo se elige de
// su catálogo; la unidad, de las de Académico que corresponden a él (la de su gestión para un
// gestor, una comisión para un miembro de comisión), y la carrera solo para el coordinador de
// carrera. Al guardar, cada campo con un problema queda marcado con un motivo que dice qué hacer,
// antes de enviar nada (principio XIV); el servidor valida todo y su respuesta va junto a su campo.
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { Campo, ConErrores, Eleccion, Seccion } from '../comun/Campos';
import { Avisos, Marco } from '../comun/Formulario';
import { campos as cuantos, filtrar } from '../comun/validacion';
import { useEnvio } from '../comun/useEnvio';
import type { ReferenciasDeDocentes, ResumenDeDocente } from '../docentes/consultas';
import { corregirCargo, registrarCargo, type Cargo, type TipoDeCargo } from './consultas';
import { NOMBRE_DEL_CARGO, NOMBRE_DEL_ESTADO_DEL_CARGO, gestionDe } from './nombres';
import {
  DOCUMENTO,
  coordinaCarrera,
  datosDe,
  dosAniosDespues,
  erroresEn,
  nuevoDe,
  valoresDe,
  valoresVacios,
  type ValoresDelCargo,
} from './validacionDeCargo';

const CAMPOS = [
  'docente',
  'tipo',
  'fechaDesignacion',
  'unidad',
  'carrera',
  'fechaFin',
  'documento',
  'estado',
];

/** Las unidades activas que puede dirigir el cargo elegido (research §4). */
function unidadesPara(tipo: string, referencias: ReferenciasDeDocentes | undefined) {
  const activas = (referencias?.unidades ?? []).filter((unidad) => unidad.activa);
  if (!tipo) {
    return [];
  }
  const gestion = gestionDe(tipo as TipoDeCargo);
  if (gestion) {
    return activas.filter((unidad) => unidad.gestionAutorizada === gestion);
  }
  if (tipo === 'MIEMBRO_COMISION') {
    return activas.filter((unidad) => unidad.tipo === 'COMISION');
  }
  return activas;
}

/**
 * Registrar un cargo (sin «cargo») o corregirlo y finalizarlo. Al registrar, avisa con las
 * advertencias que devuelve el servidor, como la de RN-20.
 */
export function DialogoCargo({
  cargo,
  docentes,
  referencias,
  alCerrar,
  alGuardar,
}: {
  cargo?: Cargo;
  docentes: readonly ResumenDeDocente[];
  referencias: ReferenciasDeDocentes | undefined;
  alCerrar: () => void;
  alGuardar: (guardado: Cargo) => Promise<void>;
}) {
  const nuevo = !cargo;
  const [valores, setValores] = useState<ValoresDelCargo>(() =>
    cargo ? valoresDe(cargo) : valoresVacios(),
  );
  const [revisado, setRevisado] = useState(false);
  // La pantalla necesita lo que respondió el servidor (sus advertencias): se lo pasa la operación.
  const { aviso, enviando, enviar } = useEnvio(() => Promise.resolve());
  const errores = revisado ? erroresEn(valores) : {};
  const cantidad = Object.keys(errores).length;

  const cambiar = (campo: keyof ValoresDelCargo, valor: string) =>
    setValores((actuales) => {
      const siguientes = { ...actuales, [campo]: valor };
      // Otro cargo pide otra unidad: la elegida puede no corresponderle.
      if (campo === 'tipo') {
        siguientes.unidad = '';
      }
      return siguientes;
    });

  const guardar = () => {
    setRevisado(true);
    if (Object.keys(erroresEn(valores)).length) {
      return;
    }
    enviar(async () => {
      const respuesta = cargo
        ? await corregirCargo(cargo, datosDe(valores))
        : await registrarCargo(nuevoDe(valores));
      await alGuardar(respuesta);
    });
  };

  const carrera = coordinaCarrera(valores.tipo);
  const unidades = unidadesPara(valores.tipo, referencias).map(
    (unidad) => [unidad.codigo, `${unidad.codigo} · ${unidad.nombre}`] as const,
  );
  const carreras = (referencias?.carreras ?? []).map(
    (una) => [una.codigo, `${una.siglas} · ${una.nombre}`] as const,
  );

  return (
    <ConErrores errores={errores}>
      <Marco
        id="titulo-dialogo-cargo"
        titulo={
          nuevo
            ? 'Registrar cargo'
            : `Corregir el cargo de ${cargo.nombreDelDocente ?? cargo.docente}`
        }
        accion={nuevo ? 'Registrar' : 'Guardar'}
        enviando={enviando}
        alCerrar={alCerrar}
        alEnviar={guardar}
        ancho="md"
      >
        <Seccion titulo="Designación">
          <Eleccion
            etiqueta="Docente"
            campo="docente"
            valor={valores.docente}
            opciones={docentes.map(
              (uno) =>
                [uno.identificacion, `${uno.nombreCompleto} · ${uno.identificacion}`] as const,
            )}
            aviso={aviso}
            deshabilitado={!nuevo}
            pista={
              nuevo
                ? 'Activo y a tiempo completo (POL 5.4). No se puede cambiar después.'
                : 'No se puede cambiar.'
            }
            alCambiar={(valor) => cambiar('docente', valor)}
          />
          <Eleccion
            etiqueta="Cargo"
            campo="tipo"
            valor={valores.tipo}
            opciones={Object.entries(NOMBRE_DEL_CARGO)}
            aviso={aviso}
            deshabilitado={!nuevo}
            alCambiar={(valor) => cambiar('tipo', valor)}
          />
          <Campo
            etiqueta="Fecha de designación"
            campo="fechaDesignacion"
            tipo="date"
            valor={valores.fechaDesignacion}
            aviso={aviso}
            deshabilitado={!nuevo}
            alCambiar={(valor) => cambiar('fechaDesignacion', valor)}
          />
          {carrera ? (
            <Eleccion
              etiqueta="Carrera que coordina"
              campo="carrera"
              valor={valores.carrera}
              opciones={carreras}
              aviso={aviso}
              vacia="Elige una"
              pista="Un solo coordinador vigente por carrera (RN-12)."
              alCambiar={(valor) => cambiar('carrera', valor)}
            />
          ) : (
            <Eleccion
              etiqueta="Unidad que dirige"
              campo="unidad"
              valor={valores.unidad}
              opciones={unidades}
              aviso={aviso}
              vacia={valores.tipo ? 'Elige una' : 'Elige primero el cargo'}
              pista={
                gestionDe(valores.tipo as TipoDeCargo)
                  ? `La unidad de la gestión ${gestionDe(valores.tipo as TipoDeCargo) ?? ''}.`
                  : valores.tipo === 'MIEMBRO_COMISION'
                    ? 'Una comisión de la estructura orgánica.'
                    : undefined
              }
              deshabilitado={!valores.tipo}
              alCambiar={(valor) => cambiar('unidad', valor)}
            />
          )}
          <Campo
            etiqueta="Documento de designación"
            campo="documento"
            valor={valores.documento}
            largo={DOCUMENTO}
            aviso={aviso}
            pista={`La resolución o la acción de personal, hasta ${String(DOCUMENTO)} caracteres.`}
            alCambiar={(valor) => cambiar('documento', filtrar(undefined, valor, DOCUMENTO))}
          />
        </Seccion>
        <Seccion titulo="Funciones">
          <Campo
            etiqueta="Fin de funciones (opcional)"
            campo="fechaFin"
            tipo="date"
            valor={valores.fechaFin}
            aviso={aviso}
            pista={
              valores.fechaDesignacion
                ? `Hasta ${dosAniosDespues(valores.fechaDesignacion)}: una designación dura hasta 2 años (POL 5.4).`
                : 'Hasta 2 años después de la designación (POL 5.4).'
            }
            alCambiar={(valor) => cambiar('fechaFin', valor)}
          />
          <Eleccion
            etiqueta="Estado"
            campo="estado"
            valor={valores.estado}
            opciones={Object.entries(NOMBRE_DEL_ESTADO_DEL_CARGO)}
            aviso={aviso}
            pista="Un cargo no se borra: se finaliza."
            alCambiar={(valor) => cambiar('estado', valor)}
          />
        </Seccion>
        {cantidad > 0 && (
          <Typography color="error" role="alert">
            {`Revisa ${cuantos(cantidad)} marcados en rojo.`}
          </Typography>
        )}
        <Avisos aviso={aviso} campos={CAMPOS} />
      </Marco>
    </ConErrores>
  );
}
