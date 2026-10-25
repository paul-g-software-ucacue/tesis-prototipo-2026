// La pantalla «Docentes» (F03-H04, RF-16, RF-17): buscar docentes por nombre o identificación,
// filtrarlos por carrera base y estado laboral, y ver la ficha académica de cada uno. La ven quienes
// leen los docentes, el distributivo o los catálogos; con «Docentes», «Catálogos y datos maestros» o
// «Distributivo del periodo» de escritura se registran, se corrigen y cambian de estado (D-70).
// Ninguno se borra; el servidor valida cada operación.
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useQueryClient } from '@tanstack/react-query';
import { useDeferredValue, useState, type ReactNode } from 'react';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError } from '../../errores/traducir';
import { useTitulo } from '../../navegacion/useTitulo';
import { ESCRITURA_DE_LOS_DOCENTES } from '../../sesion/funcionalidades';
import { tieneAlguno } from '../../sesion/permisos';
import { useSesionActual } from '../../sesion/useSesionActual';
import { SELECCION } from '../comun/formulario';
import {
  CLAVE_DOCENTES,
  useDocente,
  useDocentes,
  useReferenciasDeDocentes,
  type Docente,
  type EstadoLaboral,
  type ReferenciasDeDocentes,
  type ResumenDeDocente,
} from './consultas';
import { DialogoDocente, DialogoEstado } from './DialogosDeDocente';
import {
  NOMBRE_DE_LA_CATEGORIA,
  NOMBRE_DE_LA_CONDICION,
  NOMBRE_DE_LA_DEDICACION,
  NOMBRE_DEL_ESTADO_LABORAL,
  NOMBRE_DEL_TIPO_DE_IDENTIFICACION,
  NOMBRE_DEL_TIPO_DE_PERSONAL,
  NOMBRE_DEL_TIPO_DE_TITULO,
  docentes,
} from './nombres';

/** Lo que está abierto: un diálogo, para registrar o sobre un docente. */
type Abierto =
  { que: 'registrar' } | { que: 'ficha' | 'corregir' | 'estado'; docente: ResumenDeDocente } | null;

export function Docentes() {
  useTitulo('Docentes');
  const sesion = useSesionActual();
  const escritura = tieneAlguno(sesion.data?.permisos, ESCRITURA_DE_LOS_DOCENTES);
  const consultas = useQueryClient();
  const [texto, setTexto] = useState('');
  const [carrera, setCarrera] = useState('');
  const [estado, setEstado] = useState<EstadoLaboral | ''>('');
  // Mientras se escribe, la lista anterior se queda en pantalla.
  const textoBuscado = useDeferredValue(texto);
  const lista = useDocentes({ texto: textoBuscado, carrera, estado });
  const referencias = useReferenciasDeDocentes();
  const [abierto, setAbierto] = useState<Abierto>(null);

  const terminar = async () => {
    await consultas.invalidateQueries({ queryKey: CLAVE_DOCENTES });
    setAbierto(null);
  };

  return (
    <>
      <Typography component="h1" variant="h4" gutterBottom>
        Docentes
      </Typography>
      {lista.error && <AvisoDeError mensaje={traducirError(lista.error)} />}
      {referencias.error && <AvisoDeError mensaje={traducirError(referencias.error)} />}
      {!escritura && sesion.data && (
        <Alert severity="info" sx={{ my: 1 }}>
          Solo lectura: tu rol no registra ni corrige docentes.
        </Alert>
      )}
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ my: 2 }}>
        <TextField
          label="Buscar por nombre o identificación"
          type="search"
          value={texto}
          onChange={(evento) => setTexto(evento.target.value)}
          sx={{ minWidth: 280 }}
        />
        <TextField
          select
          label="Carrera base"
          value={carrera}
          onChange={(evento) => setCarrera(evento.target.value)}
          slotProps={SELECCION}
          sx={{ minWidth: 220 }}
        >
          <option value="">Todas</option>
          {(referencias.data?.carreras ?? []).map((una) => (
            <option key={una.codigo} value={una.codigo}>
              {`${una.siglas} · ${una.nombre}`}
            </option>
          ))}
        </TextField>
        <TextField
          select
          label="Estado laboral"
          value={estado}
          onChange={(evento) => setEstado(evento.target.value as EstadoLaboral | '')}
          slotProps={SELECCION}
          sx={{ minWidth: 180 }}
        >
          <option value="">Todos</option>
          {Object.entries(NOMBRE_DEL_ESTADO_LABORAL).map(([codigo, nombre]) => (
            <option key={codigo} value={codigo}>
              {nombre}
            </option>
          ))}
        </TextField>
      </Stack>
      {escritura && (
        <Button variant="outlined" sx={{ mb: 2 }} onClick={() => setAbierto({ que: 'registrar' })}>
          Registrar docente
        </Button>
      )}
      {lista.data && (
        <>
          <Typography component="p" sx={{ mb: 1 }}>
            {docentes(lista.data.length)}
          </Typography>
          <TableContainer>
            <Table aria-label="Docentes" size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Docente</TableCell>
                  <TableCell>Identificación</TableCell>
                  <TableCell>Carrera base</TableCell>
                  <TableCell>Tipo de personal</TableCell>
                  <TableCell>Dedicación</TableCell>
                  <TableCell>Estado</TableCell>
                  <TableCell>Acciones</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {lista.data.map((docente) => (
                  <TableRow key={docente.identificacion}>
                    <TableCell>{docente.nombreCompleto}</TableCell>
                    <TableCell>{docente.identificacion}</TableCell>
                    <TableCell>{docente.nombreDeCarreraBase ?? docente.carreraBase}</TableCell>
                    <TableCell>{NOMBRE_DEL_TIPO_DE_PERSONAL[docente.tipoPersonal]}</TableCell>
                    <TableCell>
                      {`${NOMBRE_DE_LA_DEDICACION[docente.dedicacion]} · ${String(docente.horasContratadas)} h`}
                    </TableCell>
                    <TableCell>{NOMBRE_DEL_ESTADO_LABORAL[docente.estado]}</TableCell>
                    <TableCell>
                      <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
                        <Button
                          size="small"
                          aria-label={`Ver la ficha de ${docente.nombreCompleto}`}
                          onClick={() => setAbierto({ que: 'ficha', docente })}
                        >
                          Ficha
                        </Button>
                        {escritura && (
                          <>
                            <Button
                              size="small"
                              aria-label={`Corregir a ${docente.nombreCompleto}`}
                              onClick={() => setAbierto({ que: 'corregir', docente })}
                            >
                              Corregir
                            </Button>
                            <Button
                              size="small"
                              aria-label={`Cambiar el estado de ${docente.nombreCompleto}`}
                              onClick={() => setAbierto({ que: 'estado', docente })}
                            >
                              Estado
                            </Button>
                          </>
                        )}
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </>
      )}
      {abierto?.que === 'registrar' && (
        <DialogoDocente
          referencias={referencias.data}
          alCerrar={() => setAbierto(null)}
          alGuardar={terminar}
        />
      )}
      {abierto?.que === 'ficha' && (
        <Ficha identificacion={abierto.docente.identificacion} alCerrar={() => setAbierto(null)} />
      )}
      {abierto?.que === 'corregir' && (
        <Corregir
          identificacion={abierto.docente.identificacion}
          referencias={referencias.data}
          alCerrar={() => setAbierto(null)}
          alGuardar={terminar}
        />
      )}
      {abierto?.que === 'estado' && (
        <DialogoEstado
          docente={abierto.docente}
          alCerrar={() => setAbierto(null)}
          alGuardar={terminar}
        />
      )}
    </>
  );
}

/** Corregir: pide el docente completo y abre el formulario con sus datos. */
function Corregir({
  identificacion,
  referencias,
  alCerrar,
  alGuardar,
}: {
  identificacion: string;
  referencias: ReferenciasDeDocentes | undefined;
  alCerrar: () => void;
  alGuardar: () => Promise<void>;
}) {
  const docente = useDocente(identificacion);
  if (docente.error) {
    return <AvisoDeError mensaje={traducirError(docente.error)} />;
  }
  if (!docente.data) {
    return null;
  }
  return (
    <DialogoDocente
      docente={docente.data}
      referencias={referencias}
      alCerrar={alCerrar}
      alGuardar={alGuardar}
    />
  );
}

/** Un dato de la ficha: su nombre y su valor. */
function Dato({ nombre, children }: { nombre: string; children: ReactNode }) {
  return (
    <Typography>
      <Box component="span" sx={{ fontWeight: 'bold' }}>
        {`${nombre}: `}
      </Box>
      {children}
    </Typography>
  );
}

/** La ficha de un docente, en solo lectura. */
function Ficha({ identificacion, alCerrar }: { identificacion: string; alCerrar: () => void }) {
  const docente = useDocente(identificacion);
  const datos = docente.data;
  return (
    <Dialog open onClose={alCerrar} aria-labelledby="titulo-ficha" fullWidth maxWidth="md">
      <DialogTitle id="titulo-ficha">
        {datos ? `Ficha de ${datos.nombreCompleto}` : 'Ficha del docente'}
      </DialogTitle>
      {/* La ficha solo se lee y puede ser larga: el contenido recibe el foco para desplazarse con
          el teclado (WCAG 2.1.1, regla scrollable-region-focusable de axe). */}
      <DialogContent tabIndex={0} aria-labelledby="titulo-ficha">
        {docente.error && <AvisoDeError mensaje={traducirError(docente.error)} />}
        {datos && <ContenidoDeLaFicha docente={datos} />}
      </DialogContent>
      <DialogActions>
        <Button onClick={alCerrar}>Cerrar</Button>
      </DialogActions>
    </Dialog>
  );
}

function ContenidoDeLaFicha({ docente }: { docente: Docente }) {
  const ficha = docente.ficha;
  const campo = (codigo: string) =>
    docente.camposAmplios.find((uno) => uno.codigo === codigo)?.nombre;
  const conCampo = (especifico: string) => {
    const amplio = especifico.slice(0, 2);
    const nombre = campo(amplio);
    return `campo amplio ${amplio}${nombre ? ` (${nombre})` : ''}, específico ${especifico}`;
  };
  const tercero = ficha.tercerNivel;
  return (
    <Stack spacing={2}>
      <Box component="section" aria-labelledby="ficha-datos">
        <Typography id="ficha-datos" component="h2" variant="h6">
          Datos y contrato
        </Typography>
        <Dato nombre={NOMBRE_DEL_TIPO_DE_IDENTIFICACION[docente.tipoIdentificacion]}>
          {docente.identificacion}
        </Dato>
        <Dato nombre="Correo institucional">{docente.correoInstitucional}</Dato>
        {docente.telefono && <Dato nombre="Teléfono">{docente.telefono}</Dato>}
        <Dato nombre="Tipo de personal">{NOMBRE_DEL_TIPO_DE_PERSONAL[docente.tipoPersonal]}</Dato>
        <Dato nombre="Categoría de escalafón">
          {NOMBRE_DE_LA_CATEGORIA[docente.categoriaEscalafon]}
        </Dato>
        <Dato nombre="Dedicación">
          {`${NOMBRE_DE_LA_DEDICACION[docente.dedicacion]} · ${String(docente.horasContratadas)} h contratadas`}
        </Dato>
        <Dato nombre="Condición especial">{NOMBRE_DE_LA_CONDICION[docente.condicionEspecial]}</Dato>
        <Dato nombre="Ingreso">{docente.fechaIngreso}</Dato>
        <Dato nombre="Contrato">
          {docente.finContrato
            ? `${docente.inicioContrato} a ${docente.finContrato}`
            : `desde ${docente.inicioContrato}`}
        </Dato>
        <Dato nombre="Carrera base">{docente.nombreDeCarreraBase ?? docente.carreraBase}</Dato>
        <Dato nombre="Estado laboral">{NOMBRE_DEL_ESTADO_LABORAL[docente.estado]}</Dato>
      </Box>
      <Box component="section" aria-labelledby="ficha-titulos">
        <Typography id="ficha-titulos" component="h2" variant="h6">
          Títulos
        </Typography>
        <Typography>
          {`Tercer nivel: ${tercero.titulo} · campo amplio ${tercero.campoAmplio}${
            campo(tercero.campoAmplio) ? ` (${campo(tercero.campoAmplio) ?? ''})` : ''
          }, específico ${tercero.campoEspecifico}${
            tercero.registroSenescyt ? ` · registro ${tercero.registroSenescyt}` : ''
          }`}
        </Typography>
        {(ficha.cuartoNivel ?? []).length === 0 && (
          <Typography color="text.secondary">Sin títulos de cuarto nivel.</Typography>
        )}
        {(ficha.cuartoNivel ?? []).map((titulo) => (
          <Typography key={`${titulo.titulo}-${String(titulo.anio)}`}>
            {`${NOMBRE_DEL_TIPO_DE_TITULO[titulo.tipo]}: ${titulo.titulo} (${String(titulo.anio)}) · ${conCampo(titulo.campoEspecifico)}`}
          </Typography>
        ))}
      </Box>
      <Box component="section" aria-labelledby="ficha-cursos">
        <Typography id="ficha-cursos" component="h2" variant="h6">
          Cursos y capacitaciones
        </Typography>
        {(ficha.cursos ?? []).length === 0 && (
          <Typography color="text.secondary">Sin cursos.</Typography>
        )}
        {(ficha.cursos ?? []).map((curso) => (
          <Typography key={`${curso.nombre}-${String(curso.anio)}`}>
            {`${curso.nombre} · ${String(curso.horas)} h (${String(curso.anio)}) · ${conCampo(curso.campoEspecifico)}`}
          </Typography>
        ))}
      </Box>
      <Box component="section" aria-labelledby="ficha-experiencia">
        <Typography id="ficha-experiencia" component="h2" variant="h6">
          Experiencia laboral
        </Typography>
        {(ficha.experiencia ?? []).length === 0 && (
          <Typography color="text.secondary">Sin experiencia laboral.</Typography>
        )}
        {(ficha.experiencia ?? []).map((una) => (
          <Typography key={`${una.cargo}-${una.institucion}`}>
            {`${una.cargo} en ${una.institucion} · ${String(una.anios)} años · ${conCampo(una.campoEspecifico)}`}
          </Typography>
        ))}
      </Box>
      <Box component="section" aria-labelledby="ficha-afines">
        <Typography id="ficha-afines" component="h2" variant="h6">
          Asignaturas afines declaradas
        </Typography>
        {docente.asignaturasAfines.length === 0 && (
          <Typography color="text.secondary">Sin asignaturas afines.</Typography>
        )}
        {docente.asignaturasAfines.map((afin) => (
          <Typography key={afin.codigo}>
            {afin.nombre ? `${afin.codigo} · ${afin.nombre}` : afin.codigo}
          </Typography>
        ))}
      </Box>
    </Stack>
  );
}
