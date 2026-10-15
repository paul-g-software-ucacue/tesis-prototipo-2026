// Inicio de sesión (FR-004, D-37): usuario y contraseña con React Hook Form y Zod, contra
// POST /api/usuarios/sesion. El token queda solo en memoria (D-51) y se vuelve a la ruta pedida si
// es interna (D-50). Los errores salen del traductor único (ADR-019).
import { zodResolver } from '@hookform/resolvers/zod';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, useSearchParams } from 'react-router';
import { z } from 'zod';
import { api, llamar } from '../api/cliente';
import { AvisoDeError } from '../errores/AvisoDeError';
import { MENSAJE_SESION_TERMINADA } from '../errores/mensajes';
import { traducirError, type MensajeDeError } from '../errores/traducir';
import { useTitulo } from '../navegacion/useTitulo';
import { iniciarSesion } from '../sesion/almacen';
import { destinoSeguro } from '../sesion/destinoSeguro';

const esquema = z.object({
  usuario: z.string().trim().min(1, 'Ingresa tu usuario.'),
  clave: z.string().min(1, 'Ingresa tu contraseña.'),
});

type Credenciales = z.infer<typeof esquema>;

const CAMPOS: readonly (keyof Credenciales)[] = ['usuario', 'clave'];

export function IniciarSesion() {
  useTitulo('Iniciar sesión');
  const navegar = useNavigate();
  const [parametros] = useSearchParams();
  const [aviso, setAviso] = useState<MensajeDeError | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<Credenciales>({
    resolver: zodResolver(esquema),
    defaultValues: { usuario: '', clave: '' },
  });

  const enviar = async (credenciales: Credenciales) => {
    setAviso(null);
    try {
      const token = await llamar(api.POST('/api/usuarios/sesion', { body: credenciales }));
      iniciarSesion(token.token, new Date(token.venceEn));
      await navegar(destinoSeguro(parametros.get('destino')), { replace: true });
    } catch (fallo) {
      const mensaje = traducirError(fallo);
      mensaje.detalles.forEach(({ campo, descripcion }) => {
        const nombre = CAMPOS.find((candidato) => candidato === campo);
        if (nombre) {
          setError(nombre, { message: descripcion });
        }
      });
      setAviso(mensaje);
    }
  };

  const sesionTerminada = parametros.get('motivo') === 'sesion-terminada';

  return (
    <Container component="main" maxWidth="xs" sx={{ py: 8 }}>
      <Paper sx={{ p: 4 }}>
        <Typography component="h1" variant="h5" gutterBottom>
          Iniciar sesión
        </Typography>
        <Stack spacing={2}>
          {aviso && <AvisoDeError mensaje={aviso} />}
          {!aviso && sesionTerminada && (
            <AvisoDeError mensaje={MENSAJE_SESION_TERMINADA} gravedad="warning" />
          )}
          <Box component="form" noValidate onSubmit={(evento) => void handleSubmit(enviar)(evento)}>
            <Stack spacing={2}>
              <TextField
                label="Usuario"
                autoComplete="username"
                error={!!errors.usuario}
                helperText={errors.usuario?.message}
                {...register('usuario')}
              />
              <TextField
                label="Contraseña"
                type="password"
                autoComplete="current-password"
                error={!!errors.clave}
                helperText={errors.clave?.message}
                {...register('clave')}
              />
              <Button type="submit" variant="contained" disabled={isSubmitting}>
                Iniciar sesión
              </Button>
            </Stack>
          </Box>
        </Stack>
      </Paper>
    </Container>
  );
}
