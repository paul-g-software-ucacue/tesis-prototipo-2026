package ec.mdd.usuarios.adaptadores.salida;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso de Spring Data a {@code usuarios.usuario}. */
public interface UsuarioJpaRepositorio extends JpaRepository<UsuarioJpa, UUID> {

    /** Busca por el nombre de usuario exacto. */
    Optional<UsuarioJpa> findByNombreUsuario(String nombreUsuario);

    /** Busca por la identificación, que es única (índice {@code usuario_identificacion}). */
    Optional<UsuarioJpa> findByIdentificacion(String identificacion);
}
