// La campana de la barra superior (F16-H01, US2, contracts/spa.md): el número de notificaciones
// sin leer en todas las pantallas y la lista de la bandeja. Abrir una notificación la marca como
// leída y lleva a la pantalla donde se ve el hecho, en el periodo de la notificación, aunque
// marcarla falle: el número se corrige en la siguiente consulta.
import Alert from '@mui/material/Alert';
import Badge from '@mui/material/Badge';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import IconButton from '@mui/material/IconButton';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemText from '@mui/material/ListItemText';
import Popover from '@mui/material/Popover';
import SvgIcon from '@mui/material/SvgIcon';
import Typography from '@mui/material/Typography';
import { useQueryClient } from '@tanstack/react-query';
import { useId, useState, type MouseEvent } from 'react';
import { useNavigate } from 'react-router';
import {
  CLAVE_BANDEJA,
  marcarLeida,
  useBandeja,
  useSinLeer,
  type Notificacion,
} from './notificaciones';

const MOMENTO = new Intl.DateTimeFormat('es-EC', {
  dateStyle: 'short',
  timeStyle: 'short',
  hourCycle: 'h23',
  timeZone: 'America/Guayaquil',
});

// Texto solo para lectores de pantalla: «sin leer» no depende del color ni del peso (WCAG 1.4.1).
const SOLO_LECTOR = {
  position: 'absolute',
  width: 1,
  height: 1,
  overflow: 'hidden',
  clip: 'rect(0 0 0 0)',
  whiteSpace: 'nowrap',
} as const;

function IconoDeCampana() {
  return (
    <SvgIcon aria-hidden="true">
      <path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z" />
    </SvgIcon>
  );
}

export function Campana() {
  const [ancla, setAncla] = useState<HTMLElement | null>(null);
  const abierta = Boolean(ancla);
  const sinLeer = useSinLeer();
  const bandeja = useBandeja(abierta);
  const consultas = useQueryClient();
  const navegar = useNavigate();
  const idDeLaLista = useId();
  const idDelTitulo = useId();

  const numero = sinLeer.data?.sinLeer ?? 0;
  const nombre = numero > 0 ? `Notificaciones, ${numero} sin leer` : 'Notificaciones';

  const abrirLista = (evento: MouseEvent<HTMLElement>) => setAncla(evento.currentTarget);
  const cerrarLista = () => setAncla(null);

  const abrir = (notificacion: Notificacion) => {
    cerrarLista();
    if (!notificacion.leida) {
      void marcarLeida(notificacion.id)
        .catch(() => undefined)
        .finally(() => consultas.invalidateQueries({ queryKey: CLAVE_BANDEJA }));
    }
    // Con el periodo de la notificación: las pantallas con selector abren ese (D-108).
    void navegar({
      pathname: notificacion.enlace,
      search: `?${new URLSearchParams({ periodo: notificacion.periodo }).toString()}`,
    });
  };

  const notificaciones = bandeja.data?.notificaciones ?? [];

  return (
    <>
      <IconButton
        color="inherit"
        aria-label={nombre}
        aria-haspopup="true"
        aria-expanded={abierta}
        aria-controls={abierta ? idDeLaLista : undefined}
        onClick={abrirLista}
        sx={{ mr: 1 }}
      >
        <Badge badgeContent={numero} color="error" invisible={numero === 0} max={99}>
          <IconoDeCampana />
        </Badge>
      </IconButton>
      <Popover
        id={idDeLaLista}
        open={abierta}
        anchorEl={ancla}
        onClose={cerrarLista}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <Box sx={{ width: 380, maxWidth: '90vw', maxHeight: 480 }}>
          <Typography id={idDelTitulo} component="h2" variant="h6" sx={{ px: 2, pt: 2 }}>
            Notificaciones
          </Typography>
          {bandeja.isPending && (
            <Box sx={{ p: 2, display: 'flex', justifyContent: 'center' }}>
              <CircularProgress aria-label="Cargando las notificaciones" size={28} />
            </Box>
          )}
          {bandeja.isError && (
            <Alert
              severity="error"
              sx={{ m: 2 }}
              action={
                <Button color="inherit" size="small" onClick={() => void bandeja.refetch()}>
                  Reintentar
                </Button>
              }
            >
              No se pudieron cargar las notificaciones.
            </Alert>
          )}
          {bandeja.isSuccess && notificaciones.length === 0 && (
            <Typography sx={{ p: 2 }}>No tienes notificaciones</Typography>
          )}
          {notificaciones.length > 0 && (
            <List aria-labelledby={idDelTitulo} dense>
              {notificaciones.map((notificacion) => (
                <ListItem key={notificacion.id} disablePadding>
                  <ListItemButton onClick={() => abrir(notificacion)} alignItems="flex-start">
                    <ListItemText
                      primary={
                        <>
                          {!notificacion.leida && (
                            <Box component="span" sx={SOLO_LECTOR}>
                              Sin leer:{' '}
                            </Box>
                          )}
                          <Box
                            component="span"
                            sx={{ fontWeight: notificacion.leida ? 'normal' : 'bold' }}
                          >
                            {!notificacion.leida && (
                              <Box
                                component="span"
                                aria-hidden="true"
                                sx={{ color: 'error.main', mr: 0.5 }}
                              >
                                ●
                              </Box>
                            )}
                            {notificacion.titulo}
                          </Box>
                        </>
                      }
                      secondary={
                        <>
                          {notificacion.detalle && (
                            <Box component="span" sx={{ display: 'block' }}>
                              {notificacion.detalle}
                            </Box>
                          )}
                          <Box component="span" sx={{ display: 'block' }}>
                            {`${notificacion.autorNombre} · ${MOMENTO.format(new Date(notificacion.momento))} · ${notificacion.periodo}`}
                          </Box>
                        </>
                      }
                    />
                  </ListItemButton>
                </ListItem>
              ))}
            </List>
          )}
        </Box>
      </Popover>
    </>
  );
}
