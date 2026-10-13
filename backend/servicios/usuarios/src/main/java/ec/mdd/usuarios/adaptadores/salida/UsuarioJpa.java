package ec.mdd.usuarios.adaptadores.salida;

import ec.mdd.usuarios.dominio.Usuario;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Fila de {@code usuarios.usuario}, con sus roles en {@code usuarios.usuario_rol}. */
@Entity
@Table(schema = "usuarios", name = "usuario")
public class UsuarioJpa {

    @Id private UUID id;

    @Column(name = "nombre_usuario", nullable = false, unique = true, length = 64)
    private String nombreUsuario;

    @Column(name = "clave_hash", nullable = false)
    private String claveHash;

    @Column(nullable = false)
    private boolean activo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            schema = "usuarios",
            name = "usuario_rol",
            joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "rol", length = 64)
    private Set<String> roles = new HashSet<>();

    /** Para JPA. */
    protected UsuarioJpa() {}

    /** Fila nueva para un usuario del dominio, con un identificador nuevo. */
    static UsuarioJpa nuevo(Usuario usuario) {
        UsuarioJpa fila = new UsuarioJpa();
        fila.id = UUID.randomUUID();
        fila.nombreUsuario = usuario.nombreUsuario();
        fila.claveHash = usuario.claveHash();
        fila.activo = usuario.activo();
        fila.roles = new HashSet<>(usuario.roles());
        return fila;
    }

    /** El usuario del dominio que representa esta fila. */
    Usuario aDominio() {
        return new Usuario(nombreUsuario, claveHash, activo, roles);
    }
}
