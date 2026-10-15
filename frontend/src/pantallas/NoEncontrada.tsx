// Ruta inexistente de la SPA: una página en español con un enlace a Inicio.
import Container from '@mui/material/Container';
import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import { Link as EnlaceDeRuta } from 'react-router';
import { useTitulo } from '../navegacion/useTitulo';

export function NoEncontrada() {
  useTitulo('Página no encontrada');
  return (
    <Container component="main" maxWidth="sm" sx={{ py: 8 }}>
      <Typography component="h1" variant="h4" gutterBottom>
        Página no encontrada
      </Typography>
      <Typography gutterBottom>La dirección que abriste no existe en el MDD.</Typography>
      <Link component={EnlaceDeRuta} to="/">
        Volver a Inicio
      </Link>
    </Container>
  );
}
