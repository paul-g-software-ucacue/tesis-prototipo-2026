// Los diálogos de «Docentes» con escritura (F03-H04): registrar y corregir un docente con su ficha
// académica, con las listas de títulos, cursos, experiencia y asignaturas afines que se agregan y
// se quitan, y cambiar su estado laboral. Antes de enviar solo se dice qué falta; el servidor
// valida todo y aquí se muestra lo que responde junto a su campo y, en las listas, junto a su fila.
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState, type ReactNode } from 'react';
import type { MensajeDeError } from '../../errores/traducir';
import { Avisos, Marco } from '../comun/Formulario';
import { SELECCION, ayuda, detalleDe } from '../comun/formulario';
import { useEnvio } from '../comun/useEnvio';
import {
  cambiarEstadoDocente,
  corregirDocente,
  registrarDocente,
  type Docente,
  type EstadoLaboral,
  type ReferenciasDeDocentes,
  type ResumenDeDocente,
} from './consultas';
import {
  CURSO_VACIO,
  EXPERIENCIA_VACIA,
  TITULO_VACIO,
  camposDelFormulario,
  cuerpoDe,
  faltaEn,
  tipoDeIdentificacion,
  valoresDe,
  valoresVacios,
  type ValoresDelDocente,
} from './formularioDeDocente';
import {
  NOMBRE_DE_LA_CATEGORIA,
  NOMBRE_DE_LA_CONDICION,
  NOMBRE_DE_LA_DEDICACION,
  NOMBRE_DEL_ESTADO_LABORAL,
  NOMBRE_DEL_TIPO_DE_IDENTIFICACION,
  NOMBRE_DEL_TIPO_DE_PERSONAL,
  NOMBRE_DEL_TIPO_DE_TITULO,
} from './nombres';

const ESPECIFICO = 'Tres dígitos; los dos primeros son su campo amplio.';

type Aviso = MensajeDeError | null;

/** Una sección del formulario, con su título. */
function Seccion({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <Box component="fieldset" sx={{ border: 0, p: 0, m: 0 }}>
      <Typography component="legend" variant="subtitle1" sx={{ fontWeight: 'bold', mb: 1 }}>
        {titulo}
      </Typography>
      <Stack spacing={2}>{children}</Stack>
    </Box>
  );
}

/** Un campo de texto con su error y su pista. */
function Campo({
  etiqueta,
  campo,
  valor,
  aviso,
  pista,
  tipo,
  deshabilitado,
  alCambiar,
}: {
  etiqueta: string;
  campo: string;
  valor: string;
  aviso: Aviso;
  pista?: string;
  tipo?: 'number' | 'date' | 'email';
  deshabilitado?: boolean;
  alCambiar: (valor: string) => void;
}) {
  return (
    <TextField
      label={etiqueta}
      type={tipo}
      value={valor}
      disabled={deshabilitado}
      onChange={(evento) => alCambiar(evento.target.value)}
      error={Boolean(detalleDe(aviso, campo))}
      helperText={ayuda(aviso, campo, pista)}
      slotProps={tipo === 'date' ? { inputLabel: { shrink: true } } : undefined}
    />
  );
}

/** Una lista desplegable con su error y su pista. */
function Eleccion({
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
  return (
    <TextField
      select
      label={etiqueta}
      value={valor}
      disabled={deshabilitado}
      onChange={(evento) => alCambiar(evento.target.value)}
      error={Boolean(detalleDe(aviso, campo))}
      helperText={ayuda(aviso, campo, pista)}
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

/** Una lista de la ficha: una fila por elemento, con «Quitar», y «Agregar» al final. */
function ListaDeLaFicha<F>({
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

/** Registrar un docente (sin «docente») o corregirlo. */
export function DialogoDocente({
  docente,
  referencias,
  alCerrar,
  alGuardar,
}: {
  docente?: Docente;
  referencias: ReferenciasDeDocentes | undefined;
  alCerrar: () => void;
  alGuardar: () => Promise<void>;
}) {
  const nuevo = !docente;
  const [valores, setValores] = useState<ValoresDelDocente>(() =>
    docente ? valoresDe(docente) : valoresVacios(),
  );
  const [falta, setFalta] = useState<string | null>(null);
  const { aviso, enviando, enviar } = useEnvio(alGuardar);

  const cambiar = (campo: keyof ValoresDelDocente, valor: string) =>
    setValores((actuales) => {
      const siguientes = { ...actuales, [campo]: valor };
      // Solo un titular tiene categoría de escalafón: los demás tipos llevan «No aplica».
      if (campo === 'tipoPersonal') {
        siguientes.categoriaEscalafon =
          valor === 'TITULAR'
            ? actuales.categoriaEscalafon === 'NO_APLICA'
              ? ''
              : actuales.categoriaEscalafon
            : valor
              ? 'NO_APLICA'
              : '';
      }
      return siguientes;
    });

  function cambiarFila<L extends 'cuartoNivel' | 'cursos' | 'experiencia'>(
    lista: L,
    posicion: number,
    campo: keyof ValoresDelDocente[L][number],
    valor: string,
  ) {
    setValores((actuales) => ({
      ...actuales,
      [lista]: actuales[lista].map((fila, otra) =>
        otra === posicion ? { ...fila, [campo]: valor } : fila,
      ),
    }));
  }

  function agregarA<L extends 'cuartoNivel' | 'cursos' | 'experiencia'>(
    lista: L,
    vacia: ValoresDelDocente[L][number],
  ) {
    setValores((actuales) => ({ ...actuales, [lista]: [...actuales[lista], { ...vacia }] }));
  }

  function quitarDe(
    lista: 'cuartoNivel' | 'cursos' | 'experiencia' | 'asignaturasAfines',
    posicion: number,
  ) {
    setValores((actuales) => ({
      ...actuales,
      [lista]: (actuales[lista] as unknown[]).filter((_, otra) => otra !== posicion),
    }));
  }

  const guardar = () => {
    const queFalta = faltaEn(valores, nuevo);
    setFalta(queFalta);
    if (queFalta) {
      return;
    }
    const cuerpo = cuerpoDe(valores);
    enviar(() =>
      docente
        ? corregirDocente(docente.identificacion, cuerpo)
        : registrarDocente({
            tipoIdentificacion: tipoDeIdentificacion(valores),
            identificacion: valores.identificacion.trim().toUpperCase(),
            ...cuerpo,
          }),
    );
  };

  const campos = camposDelFormulario(valores);
  const carreras = (referencias?.carreras ?? []).map(
    (carrera) => [carrera.codigo, `${carrera.siglas} · ${carrera.nombre}`] as const,
  );
  const camposAmplios = (referencias?.camposAmplios ?? []).map(
    (campo) => [campo.codigo, `${campo.codigo} · ${campo.nombre}`] as const,
  );
  const titular = valores.tipoPersonal === 'TITULAR';
  const categorias = Object.entries(NOMBRE_DE_LA_CATEGORIA).filter(([codigo]) =>
    titular ? codigo !== 'NO_APLICA' : codigo === 'NO_APLICA',
  );

  return (
    <Marco
      id="titulo-dialogo-docente"
      titulo={nuevo ? 'Registrar docente' : `Corregir ${docente.nombreCompleto}`}
      accion={nuevo ? 'Registrar' : 'Guardar'}
      enviando={enviando}
      alCerrar={alCerrar}
      alEnviar={guardar}
      ancho="md"
    >
      <Seccion titulo="Identificación y datos personales">
        <Eleccion
          etiqueta="Tipo de identificación"
          campo="tipoIdentificacion"
          valor={valores.tipoIdentificacion}
          opciones={Object.entries(NOMBRE_DEL_TIPO_DE_IDENTIFICACION)}
          aviso={aviso}
          deshabilitado={!nuevo}
          alCambiar={(valor) => cambiar('tipoIdentificacion', valor)}
        />
        <Campo
          etiqueta="Identificación"
          campo="identificacion"
          valor={valores.identificacion}
          aviso={aviso}
          deshabilitado={!nuevo}
          pista={
            nuevo
              ? 'Cédula de 10 dígitos, con el 0 inicial, o pasaporte. No se puede cambiar después.'
              : 'No se puede cambiar.'
          }
          alCambiar={(valor) => cambiar('identificacion', valor)}
        />
        <Campo
          etiqueta="Primer apellido"
          campo="primerApellido"
          valor={valores.primerApellido}
          aviso={aviso}
          pista="Se guarda en mayúsculas."
          alCambiar={(valor) => cambiar('primerApellido', valor)}
        />
        <Campo
          etiqueta="Segundo apellido (opcional)"
          campo="segundoApellido"
          valor={valores.segundoApellido}
          aviso={aviso}
          alCambiar={(valor) => cambiar('segundoApellido', valor)}
        />
        <Campo
          etiqueta="Primer nombre"
          campo="primerNombre"
          valor={valores.primerNombre}
          aviso={aviso}
          alCambiar={(valor) => cambiar('primerNombre', valor)}
        />
        <Campo
          etiqueta="Segundo nombre (opcional)"
          campo="segundoNombre"
          valor={valores.segundoNombre}
          aviso={aviso}
          alCambiar={(valor) => cambiar('segundoNombre', valor)}
        />
        <Campo
          etiqueta="Correo institucional"
          campo="correoInstitucional"
          tipo="email"
          valor={valores.correoInstitucional}
          aviso={aviso}
          pista="Único entre los docentes."
          alCambiar={(valor) => cambiar('correoInstitucional', valor)}
        />
        <Campo
          etiqueta="Teléfono (opcional)"
          campo="telefono"
          valor={valores.telefono}
          aviso={aviso}
          pista="De 7 a 10 dígitos."
          alCambiar={(valor) => cambiar('telefono', valor)}
        />
      </Seccion>
      <Seccion titulo="Contrato">
        <Eleccion
          etiqueta="Tipo de personal"
          campo="tipoPersonal"
          valor={valores.tipoPersonal}
          opciones={Object.entries(NOMBRE_DEL_TIPO_DE_PERSONAL)}
          aviso={aviso}
          alCambiar={(valor) => cambiar('tipoPersonal', valor)}
        />
        <Eleccion
          etiqueta="Categoría de escalafón"
          campo="categoriaEscalafon"
          valor={valores.categoriaEscalafon}
          opciones={categorias}
          aviso={aviso}
          deshabilitado={!titular}
          pista={
            titular
              ? 'Auxiliar, agregado o principal, con su nivel.'
              : 'Solo un titular tiene categoría: los demás, «No aplica».'
          }
          alCambiar={(valor) => cambiar('categoriaEscalafon', valor)}
        />
        <Eleccion
          etiqueta="Dedicación"
          campo="dedicacion"
          valor={valores.dedicacion}
          opciones={Object.entries(NOMBRE_DE_LA_DEDICACION)}
          aviso={aviso}
          vacia="Elige una"
          alCambiar={(valor) => cambiar('dedicacion', valor)}
        />
        <Campo
          etiqueta="Horas contratadas"
          campo="horasContratadas"
          tipo="number"
          valor={valores.horasContratadas}
          aviso={aviso}
          pista="De 1 a 40. Tiempo completo suele ser 40; medio tiempo, 20."
          alCambiar={(valor) => cambiar('horasContratadas', valor)}
        />
        <Campo
          etiqueta="Fecha de ingreso"
          campo="fechaIngreso"
          tipo="date"
          valor={valores.fechaIngreso}
          aviso={aviso}
          alCambiar={(valor) => cambiar('fechaIngreso', valor)}
        />
        <Campo
          etiqueta="Inicio del contrato"
          campo="inicioContrato"
          tipo="date"
          valor={valores.inicioContrato}
          aviso={aviso}
          alCambiar={(valor) => cambiar('inicioContrato', valor)}
        />
        <Campo
          etiqueta={
            valores.tipoPersonal === 'OCASIONAL'
              ? 'Fin del contrato'
              : 'Fin del contrato (opcional)'
          }
          campo="finContrato"
          tipo="date"
          valor={valores.finContrato}
          aviso={aviso}
          pista="Posterior al inicio; obligatorio para un ocasional."
          alCambiar={(valor) => cambiar('finContrato', valor)}
        />
        <Eleccion
          etiqueta="Condición especial"
          campo="condicionEspecial"
          valor={valores.condicionEspecial}
          opciones={Object.entries(NOMBRE_DE_LA_CONDICION)}
          aviso={aviso}
          vacia="Elige una"
          alCambiar={(valor) => cambiar('condicionEspecial', valor)}
        />
        <Eleccion
          etiqueta="Carrera base"
          campo="carreraBase"
          valor={valores.carreraBase}
          opciones={carreras}
          aviso={aviso}
          vacia="Elige una"
          pista="La carrera a la que pertenece. No limita dónde dicta clase."
          alCambiar={(valor) => cambiar('carreraBase', valor)}
        />
      </Seccion>
      <Seccion titulo="Título de tercer nivel">
        <Campo
          etiqueta="Título"
          campo="ficha.tercerNivel.titulo"
          valor={valores.titulo}
          aviso={aviso}
          pista="Como consta en la SENESCYT."
          alCambiar={(valor) => cambiar('titulo', valor)}
        />
        <Campo
          etiqueta="Registro SENESCYT (opcional)"
          campo="ficha.tercerNivel.registroSenescyt"
          valor={valores.registroSenescyt}
          aviso={aviso}
          alCambiar={(valor) => cambiar('registroSenescyt', valor)}
        />
        <Eleccion
          etiqueta="Campo amplio"
          campo="ficha.tercerNivel.campoAmplio"
          valor={valores.campoAmplio}
          opciones={camposAmplios}
          aviso={aviso}
          alCambiar={(valor) => cambiar('campoAmplio', valor)}
        />
        <Campo
          etiqueta="Campo específico"
          campo="ficha.tercerNivel.campoEspecifico"
          valor={valores.campoEspecifico}
          aviso={aviso}
          pista={ESPECIFICO}
          alCambiar={(valor) => cambiar('campoEspecifico', valor)}
        />
      </Seccion>
      <ListaDeLaFicha
        titulo="Títulos de cuarto nivel"
        nombre="título"
        filas={valores.cuartoNivel}
        vacia="Sin títulos de cuarto nivel."
        agregar={() => agregarA('cuartoNivel', TITULO_VACIO)}
        quitar={(posicion) => quitarDe('cuartoNivel', posicion)}
        fila={(fila, posicion) => {
          const base = `ficha.cuartoNivel[${String(posicion)}]`;
          return (
            <>
              <Campo
                etiqueta="Título"
                campo={`${base}.titulo`}
                valor={fila.titulo}
                aviso={aviso}
                alCambiar={(valor) => cambiarFila('cuartoNivel', posicion, 'titulo', valor)}
              />
              <Eleccion
                etiqueta="Tipo"
                campo={`${base}.tipo`}
                valor={fila.tipo}
                opciones={Object.entries(NOMBRE_DEL_TIPO_DE_TITULO)}
                aviso={aviso}
                alCambiar={(valor) => cambiarFila('cuartoNivel', posicion, 'tipo', valor)}
              />
              <Campo
                etiqueta="Campo específico"
                campo={`${base}.campoEspecifico`}
                valor={fila.campoEspecifico}
                aviso={aviso}
                alCambiar={(valor) =>
                  cambiarFila('cuartoNivel', posicion, 'campoEspecifico', valor)
                }
              />
              <Campo
                etiqueta="Año"
                campo={`${base}.anio`}
                tipo="number"
                valor={fila.anio}
                aviso={aviso}
                alCambiar={(valor) => cambiarFila('cuartoNivel', posicion, 'anio', valor)}
              />
            </>
          );
        }}
      />
      <ListaDeLaFicha
        titulo="Cursos y capacitaciones"
        nombre="curso"
        filas={valores.cursos}
        vacia="Sin cursos."
        agregar={() => agregarA('cursos', CURSO_VACIO)}
        quitar={(posicion) => quitarDe('cursos', posicion)}
        fila={(fila, posicion) => {
          const base = `ficha.cursos[${String(posicion)}]`;
          return (
            <>
              <Campo
                etiqueta="Nombre"
                campo={`${base}.nombre`}
                valor={fila.nombre}
                aviso={aviso}
                alCambiar={(valor) => cambiarFila('cursos', posicion, 'nombre', valor)}
              />
              <Campo
                etiqueta="Campo específico"
                campo={`${base}.campoEspecifico`}
                valor={fila.campoEspecifico}
                aviso={aviso}
                alCambiar={(valor) => cambiarFila('cursos', posicion, 'campoEspecifico', valor)}
              />
              <Campo
                etiqueta="Horas"
                campo={`${base}.horas`}
                tipo="number"
                valor={fila.horas}
                aviso={aviso}
                alCambiar={(valor) => cambiarFila('cursos', posicion, 'horas', valor)}
              />
              <Campo
                etiqueta="Año"
                campo={`${base}.anio`}
                tipo="number"
                valor={fila.anio}
                aviso={aviso}
                alCambiar={(valor) => cambiarFila('cursos', posicion, 'anio', valor)}
              />
            </>
          );
        }}
      />
      <ListaDeLaFicha
        titulo="Experiencia laboral"
        nombre="experiencia"
        filas={valores.experiencia}
        vacia="Sin experiencia laboral."
        agregar={() => agregarA('experiencia', EXPERIENCIA_VACIA)}
        quitar={(posicion) => quitarDe('experiencia', posicion)}
        fila={(fila, posicion) => {
          const base = `ficha.experiencia[${String(posicion)}]`;
          return (
            <>
              <Campo
                etiqueta="Cargo"
                campo={`${base}.cargo`}
                valor={fila.cargo}
                aviso={aviso}
                alCambiar={(valor) => cambiarFila('experiencia', posicion, 'cargo', valor)}
              />
              <Campo
                etiqueta="Institución"
                campo={`${base}.institucion`}
                valor={fila.institucion}
                aviso={aviso}
                alCambiar={(valor) => cambiarFila('experiencia', posicion, 'institucion', valor)}
              />
              <Campo
                etiqueta="Campo específico"
                campo={`${base}.campoEspecifico`}
                valor={fila.campoEspecifico}
                aviso={aviso}
                alCambiar={(valor) =>
                  cambiarFila('experiencia', posicion, 'campoEspecifico', valor)
                }
              />
              <Campo
                etiqueta="Años"
                campo={`${base}.anios`}
                tipo="number"
                valor={fila.anios}
                aviso={aviso}
                alCambiar={(valor) => cambiarFila('experiencia', posicion, 'anios', valor)}
              />
            </>
          );
        }}
      />
      <ListaDeLaFicha
        titulo="Asignaturas afines declaradas"
        nombre="asignatura afín"
        filas={valores.asignaturasAfines}
        vacia="Sin asignaturas afines."
        agregar={() =>
          setValores((actuales) => ({
            ...actuales,
            asignaturasAfines: [...actuales.asignaturasAfines, ''],
          }))
        }
        quitar={(posicion) => quitarDe('asignaturasAfines', posicion)}
        fila={(codigo, posicion) => {
          const campo = `ficha.asignaturasAfines[${String(posicion)}]`;
          return (
            <TextField
              select
              label="Asignatura"
              value={codigo}
              onChange={(evento) =>
                setValores((actuales) => ({
                  ...actuales,
                  asignaturasAfines: actuales.asignaturasAfines.map((otra, cual) =>
                    cual === posicion ? evento.target.value : otra,
                  ),
                }))
              }
              error={Boolean(detalleDe(aviso, campo))}
              helperText={ayuda(aviso, campo)}
              slotProps={SELECCION}
              sx={{ minWidth: 280 }}
            >
              <option value="">Elige una</option>
              {(referencias?.carreras ?? []).map((carrera) => (
                <optgroup key={carrera.codigo} label={`${carrera.siglas} · ${carrera.nombre}`}>
                  {(referencias?.asignaturas ?? [])
                    .filter((asignatura) => asignatura.carrera === carrera.codigo)
                    .map((asignatura) => (
                      <option key={asignatura.codigo} value={asignatura.codigo}>
                        {`${asignatura.codigo} · ${asignatura.nombre}`}
                      </option>
                    ))}
                </optgroup>
              ))}
            </TextField>
          );
        }}
      />
      {falta && <Typography color="error">{falta}</Typography>}
      <Avisos aviso={aviso} campos={campos} />
    </Marco>
  );
}

/** Pasar a un docente a activo, en licencia o inactivo; ninguno se borra. */
export function DialogoEstado({
  docente,
  alCerrar,
  alGuardar,
}: {
  docente: ResumenDeDocente;
  alCerrar: () => void;
  alGuardar: () => Promise<void>;
}) {
  const [estado, setEstado] = useState<EstadoLaboral>(docente.estado);
  const { aviso, enviando, enviar } = useEnvio(alGuardar);
  return (
    <Marco
      id="titulo-dialogo-estado"
      titulo={`Estado laboral de ${docente.nombreCompleto}`}
      accion="Guardar"
      enviando={enviando}
      alCerrar={alCerrar}
      alEnviar={() => enviar(() => cambiarEstadoDocente(docente.identificacion, estado))}
    >
      <Typography>
        Solo un docente activo recibe asignaciones. Ninguno se borra: se puede volver a activar.
      </Typography>
      <Eleccion
        etiqueta="Estado laboral"
        campo="estado"
        valor={estado}
        opciones={Object.entries(NOMBRE_DEL_ESTADO_LABORAL)}
        aviso={aviso}
        alCambiar={(valor) => setEstado(valor as EstadoLaboral)}
      />
      <Avisos aviso={aviso} campos={['estado']} />
    </Marco>
  );
}
