package ec.mdd.usuarios.infraestructura;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import ec.mdd.usuarios.puertos.salida.VerificadorDeClaves;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * Crea el usuario de prueba {@code autor.prueba} al arrancar, solo si está habilitado (research
 * §8).
 *
 * <p>Así el usuario y su contraseña nunca están en una migración: no existe en el VPS salvo que se
 * habilite, y el hash no está en el repositorio. Si ya existe, no hace nada. La contraseña nunca se
 * registra.
 */
public final class CargaUsuarioDePrueba implements ApplicationRunner {

    /** Nombre del usuario de prueba. */
    public static final String NOMBRE = "autor.prueba";

    /** Roles del usuario de prueba: los del escenario «Varios roles» de F05-H02. */
    public static final Set<String> ROLES = Set.of("DOCENTE", "COORDINADOR_CARRERA");

    private static final Logger REGISTRO = LoggerFactory.getLogger(CargaUsuarioDePrueba.class);

    private final RepositorioUsuarios repositorio;
    private final VerificadorDeClaves verificador;
    private final PropiedadesUsuarios.UsuarioPrueba configuracion;

    /** Recibe los puertos y la configuración del usuario de prueba. */
    public CargaUsuarioDePrueba(
            RepositorioUsuarios repositorio,
            VerificadorDeClaves verificador,
            PropiedadesUsuarios.UsuarioPrueba configuracion) {
        this.repositorio = repositorio;
        this.verificador = verificador;
        this.configuracion = configuracion;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        if (!configuracion.habilitado()) {
            return;
        }
        String clave = configuracion.clave();
        if (clave == null || clave.isBlank()) {
            REGISTRO.warn("Usuario de prueba habilitado sin contraseña: no se crea.");
            return;
        }
        if (repositorio.buscarPorNombre(NOMBRE).isPresent()) {
            return;
        }
        repositorio.guardar(new Usuario(NOMBRE, verificador.calcularHash(clave), true, ROLES));
        REGISTRO.info("Usuario de prueba {} creado.", NOMBRE);
    }
}
