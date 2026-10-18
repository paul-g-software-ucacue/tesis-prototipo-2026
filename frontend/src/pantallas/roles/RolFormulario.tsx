// El detalle de un rol, con su matriz funcionalidad × tipo × alcance (F05-H02). Un rol nuevo o uno
// propio, con escritura, se edita con un selector por cada tipo que admite cada funcionalidad; los
// 10 de D-16 y todo lo demás se ven en solo lectura (D-63).
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import NativeSelect from '@mui/material/NativeSelect';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { apiRoles, llamar } from '../../api/cliente';
import type { components } from '../../api/generado/roles';
import { AvisoDeError } from '../../errores/AvisoDeError';
import { traducirError, type MensajeDeError } from '../../errores/traducir';
import { TIPOS_DE_PERMISO, type TipoDePermiso } from '../../sesion/funcionalidades';
import type { Funcionalidad, Rol } from './consultas';

type Alcance = components['schemas']['Alcance'];
type Permiso = components['schemas']['Permiso'];

const ALCANCES: Record<Alcance, string> = { TODO: 'Todo', PROPIO: 'Solo lo propio' };

interface Propiedades {
  /** El rol abierto; sin él, uno nuevo. */
  rol?: Rol;
  funcionalidades: Funcionalidad[];
  escritura: boolean;
  alCerrar: () => void;
}

const clave = (funcionalidad: string, tipo: TipoDePermiso) => `${funcionalidad}:${tipo}`;

export function RolFormulario({ rol, funcionalidades, escritura, alCerrar }: Propiedades) {
  const consultas = useQueryClient();
  const editable = escritura && !rol?.predefinido;
  const [nombre, setNombre] = useState(rol?.nombre ?? '');
  const [descripcion, setDescripcion] = useState(rol?.descripcion ?? '');
  const [matriz, setMatriz] = useState(
    () =>
      new Map<string, Alcance>(
        (rol?.permisos ?? []).map((permiso) => [
          clave(permiso.funcionalidad, permiso.tipo),
          permiso.alcance,
        ]),
      ),
  );
  const [falta, setFalta] = useState<string | null>(null);
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const [enviando, setEnviando] = useState(false);
  const titulo = rol?.nombre ?? 'Rol nuevo';

  const cambiar = (funcionalidad: string, tipo: TipoDePermiso, valor: string) => {
    const nueva = new Map(matriz);
    if (valor === 'TODO' || valor === 'PROPIO') {
      nueva.set(clave(funcionalidad, tipo), valor);
    } else {
      nueva.delete(clave(funcionalidad, tipo));
    }
    setMatriz(nueva);
  };

  const permisos = (): Permiso[] =>
    funcionalidades.flatMap(({ codigo, tipos }) =>
      TIPOS_DE_PERMISO.filter(({ tipo }) => tipos.includes(tipo)).flatMap(({ tipo }) => {
        const alcance = matriz.get(clave(codigo, tipo));
        return alcance ? [{ funcionalidad: codigo, tipo, alcance }] : [];
      }),
    );

  const ejecutar = async (accion: () => Promise<unknown>) => {
    setAviso(null);
    setEnviando(true);
    try {
      await accion();
      await consultas.invalidateQueries({ queryKey: ['roles'] });
      alCerrar();
    } catch (fallo) {
      setAviso(traducirError(fallo));
    } finally {
      setEnviando(false);
    }
  };

  const guardar = () => {
    const elegidos = permisos();
    if (!nombre.trim() || !descripcion.trim()) {
      setFalta('Escribe el nombre y la descripción del rol.');
      return;
    }
    if (elegidos.length === 0) {
      setFalta('El rol necesita al menos un permiso.');
      return;
    }
    setFalta(null);
    const cuerpo = { nombre: nombre.trim(), descripcion: descripcion.trim(), permisos: elegidos };
    void ejecutar(() =>
      rol
        ? llamar(
            apiRoles.PUT('/api/roles/{identificador}', {
              params: { path: { identificador: rol.identificador } },
              body: cuerpo,
            }),
          )
        : llamar(apiRoles.POST('/api/roles', { body: cuerpo })),
    );
  };

  const borrar = (identificador: string) =>
    void ejecutar(() =>
      llamar(
        apiRoles.DELETE('/api/roles/{identificador}', {
          params: { path: { identificador } },
        }),
      ),
    );

  return (
    <Dialog open onClose={alCerrar} maxWidth="md" fullWidth aria-labelledby="titulo-del-rol">
      <DialogTitle id="titulo-del-rol">{titulo}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {aviso && (
            <>
              <AvisoDeError mensaje={aviso} />
              {aviso.detalles.map(({ campo, descripcion: texto }) => (
                <Typography key={`${campo}-${texto}`} color="error">
                  {texto}
                </Typography>
              ))}
            </>
          )}
          {rol?.predefinido && (
            <Typography>
              Los roles de la matriz D-16 no se pueden cambiar ni borrar. Para otra combinación,
              crea un rol nuevo.
            </Typography>
          )}
          {editable ? (
            <>
              <TextField
                label="Nombre"
                value={nombre}
                onChange={(evento) => setNombre(evento.target.value)}
                slotProps={{ htmlInput: { maxLength: 80 } }}
              />
              <TextField
                label="Descripción"
                value={descripcion}
                onChange={(evento) => setDescripcion(evento.target.value)}
                multiline
                slotProps={{ htmlInput: { maxLength: 300 } }}
              />
            </>
          ) : (
            rol && <Typography>{rol.descripcion}</Typography>
          )}
          <Table size="small" aria-label={`Permisos de ${titulo}`}>
            <TableHead>
              <TableRow>
                <TableCell>Funcionalidad</TableCell>
                {TIPOS_DE_PERMISO.map(({ tipo, nombre: nombreDelTipo }) => (
                  <TableCell key={tipo} sx={{ textTransform: 'capitalize' }}>
                    {nombreDelTipo}
                  </TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {funcionalidades.map((funcionalidad) => (
                <TableRow key={funcionalidad.codigo}>
                  <TableCell component="th" scope="row">
                    {funcionalidad.nombre}
                  </TableCell>
                  {TIPOS_DE_PERMISO.map(({ tipo, nombre: nombreDelTipo }) => {
                    if (!funcionalidad.tipos.includes(tipo)) {
                      return <TableCell key={tipo} />;
                    }
                    const etiqueta = `${funcionalidad.nombre}: ${nombreDelTipo}`;
                    const alcance = matriz.get(clave(funcionalidad.codigo, tipo));
                    return editable ? (
                      <TableCell key={tipo}>
                        <NativeSelect
                          value={alcance ?? ''}
                          onChange={(evento) =>
                            cambiar(funcionalidad.codigo, tipo, evento.target.value)
                          }
                          inputProps={{ 'aria-label': etiqueta }}
                        >
                          <option value="">—</option>
                          <option value="TODO">{ALCANCES.TODO}</option>
                          <option value="PROPIO">{ALCANCES.PROPIO}</option>
                        </NativeSelect>
                      </TableCell>
                    ) : (
                      <TableCell key={tipo} aria-label={etiqueta}>
                        {alcance ? ALCANCES[alcance] : '—'}
                      </TableCell>
                    );
                  })}
                </TableRow>
              ))}
            </TableBody>
          </Table>
          {falta && <Typography color="error">{falta}</Typography>}
          {editable && rol && rol.usuariosAsignados > 0 && (
            <Typography>
              {rol.usuariosAsignados === 1
                ? 'Asignado a 1 usuario. Quítaselo antes de borrarlo.'
                : `Asignado a ${rol.usuariosAsignados} usuarios. Quítaselo antes de borrarlo.`}
            </Typography>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        {editable && rol && (
          <Button
            color="error"
            disabled={enviando || rol.usuariosAsignados > 0}
            onClick={() => borrar(rol.identificador)}
          >
            Borrar rol
          </Button>
        )}
        <Button onClick={alCerrar}>Cerrar</Button>
        {editable && (
          <Button variant="contained" disabled={enviando} onClick={guardar}>
            Guardar
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
}
