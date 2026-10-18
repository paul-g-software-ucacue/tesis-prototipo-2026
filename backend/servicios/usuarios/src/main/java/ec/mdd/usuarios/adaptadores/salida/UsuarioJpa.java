package ec.mdd.usuarios.adaptadores.salida;

import ec.mdd.usuarios.dominio.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Fila de {@code usuarios.usuario}. Desde F05-H02 los roles son de Roles: {@code
 * usuarios.usuario_rol} queda sin uso y no se borra, para poder volver a la versión anterior
 * (research §1).
 */
@Entity
@Table(schema = "usuarios", name = "usuario")
public class UsuarioJpa {

    @Id private UUID id;

    @Column(name = "nombre_usuario", nullable = false, unique = true, length = 64)
    private String nombreUsuario;

    @Column(name = "nombre_para_mostrar", nullable = false, length = 120)
    private String nombreParaMostrar;

    @Column(name = "clave_hash", nullable = false)
    private String claveHash;

    @Column(nullable = false)
    private boolean activo;

    /** Para JPA. */
    protected UsuarioJpa() {}

    /** Fila nueva para un usuario del dominio, con un identificador nuevo. */
    static UsuarioJpa nuevo(Usuario usuario) {
        UsuarioJpa fila = new UsuarioJpa();
        fila.id = UUID.randomUUID();
        fila.nombreUsuario = usuario.nombreUsuario();
        fila.copiar(usuario);
        return fila;
    }

    /** Copia todo menos el identificador y el nombre de usuario. */
    void copiar(Usuario usuario) {
        nombreParaMostrar = usuario.nombreParaMostrar();
        claveHash = usuario.claveHash();
        activo = usuario.activo();
    }

    /** El usuario del dominio que representa esta fila. */
    Usuario aDominio() {
        return new Usuario(nombreUsuario, nombreParaMostrar, claveHash, activo);
    }
}
