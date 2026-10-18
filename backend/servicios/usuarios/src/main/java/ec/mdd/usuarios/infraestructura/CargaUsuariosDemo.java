package ec.mdd.usuarios.infraestructura;

import ec.mdd.usuarios.dominio.Usuario;
import ec.mdd.usuarios.puertos.salida.ProveedorDeAutorizacion;
import ec.mdd.usuarios.puertos.salida.RepositorioUsuarios;
import ec.mdd.usuarios.puertos.salida.VerificadorDeClaves;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Crea o actualiza los usuarios de la demo al arrancar (D-59, research §2).
 *
 * <p>Los usuarios, sus nombres y sus roles salen de {@value #ARCHIVO}, versionado y sin
 * contraseñas. La contraseña de cada uno sale del entorno: del archivo {@code usuario=clave} de
 * {@code mdd.usuarios.demo.claves} (equipo de demostración) o, si no hay archivo, de la clave común
 * {@code mdd.usuarios.demo.clave} (local y CI). Un usuario sin contraseña no se crea. Si la
 * contraseña ya no coincide con el hash guardado, se guarda uno nuevo: rotar las claves no exige
 * tocar la base. Las contraseñas nunca se registran.
 *
 * <p>Los roles del archivo son los iniciales: en cada arranque se piden a Roles para cada usuario
 * de la demo que existe, y Roles solo los da la primera vez (FR-009, research §8). Así, al
 * desplegar F05-H02, los usuarios que ya existían los reciben, y después los cambia solo el
 * administrador. Un rol que Roles no conoce detiene el arranque y dice cuál.
 */
public final class CargaUsuariosDemo implements ApplicationRunner {

    /** Archivo con los usuarios de la demo, en los recursos del servicio. */
    public static final String ARCHIVO = "usuarios-demo.yaml";

    private static final Logger REGISTRO = LoggerFactory.getLogger(CargaUsuariosDemo.class);

    private final RepositorioUsuarios repositorio;
    private final VerificadorDeClaves verificador;
    private final ProveedorDeAutorizacion autorizacion;
    private final List<UsuarioDemo> usuarios;
    private final PropiedadesUsuarios.Demo configuracion;

    /**
     * Un usuario de la demo, tal como está en {@value #ARCHIVO}.
     *
     * @param usuario nombre de usuario
     * @param nombre nombre para mostrar
     * @param roles sus roles iniciales (identificadores de Roles)
     */
    public record UsuarioDemo(String usuario, String nombre, Set<String> roles) {

        /** Copia los roles. */
        public UsuarioDemo {
            roles = Set.copyOf(roles);
        }
    }

    /** Recibe los puertos, los usuarios ya leídos y la configuración de las contraseñas. */
    public CargaUsuariosDemo(
            RepositorioUsuarios repositorio,
            VerificadorDeClaves verificador,
            ProveedorDeAutorizacion autorizacion,
            List<UsuarioDemo> usuarios,
            PropiedadesUsuarios.Demo configuracion) {
        this.repositorio = repositorio;
        this.verificador = verificador;
        this.autorizacion = autorizacion;
        this.usuarios = List.copyOf(usuarios);
        this.configuracion = configuracion;
    }

    /** Los usuarios de {@value #ARCHIVO} de los recursos del servicio. */
    public static List<UsuarioDemo> delArchivoVersionado() {
        try (InputStream archivo = CargaUsuariosDemo.class.getResourceAsStream("/" + ARCHIVO)) {
            if (archivo == null) {
                throw new IllegalStateException("Falta " + ARCHIVO + " en los recursos");
            }
            return leer(archivo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Lee la lista {@code usuarios} de un YAML con {@code usuario}, {@code nombre} y {@code roles}.
     * Los roles los valida Roles al asignarlos.
     */
    public static List<UsuarioDemo> leer(InputStream yaml) {
        Map<String, Object> raiz = new Yaml(new SafeConstructor(new LoaderOptions())).load(yaml);
        List<UsuarioDemo> leidos = new ArrayList<>();
        for (Object elemento : (List<?>) raiz.get("usuarios")) {
            Map<?, ?> fila = (Map<?, ?>) elemento;
            Set<String> roles = new LinkedHashSet<>();
            for (Object rol : (List<?>) fila.get("roles")) {
                roles.add(String.valueOf(rol));
            }
            leidos.add(
                    new UsuarioDemo(
                            String.valueOf(fila.get("usuario")),
                            String.valueOf(fila.get("nombre")),
                            roles));
        }
        return leidos;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        Optional<Map<String, String>> claves = claves();
        if (claves.isEmpty()) {
            return;
        }
        int creados = 0;
        int actualizados = 0;
        for (UsuarioDemo demo : usuarios) {
            String clave = claves.get().get(demo.usuario());
            if (clave == null || clave.isBlank()) {
                REGISTRO.warn("Usuario de la demo {} sin contraseña: no se crea.", demo.usuario());
                continue;
            }
            Optional<Usuario> existente = repositorio.buscarPorNombre(demo.usuario());
            if (existente.isEmpty()) {
                repositorio.guardar(
                        new Usuario(
                                demo.usuario(),
                                demo.nombre(),
                                verificador.calcularHash(clave),
                                true));
                creados++;
            } else if (actualizar(existente.get(), demo, clave)) {
                actualizados++;
            }
            autorizacion.asignarRolesIniciales(Usuario.normalizar(demo.usuario()), demo.roles());
        }
        REGISTRO.info("Usuarios de la demo: {} creados y {} actualizados.", creados, actualizados);
    }

    private boolean actualizar(Usuario existente, UsuarioDemo demo, String clave) {
        boolean mismaClave = verificador.coincide(clave, existente.claveHash());
        if (mismaClave
                && existente.puedeIniciarSesion()
                && existente.nombreParaMostrar().equals(demo.nombre())) {
            return false;
        }
        repositorio.actualizar(
                new Usuario(
                        demo.usuario(),
                        demo.nombre(),
                        mismaClave ? existente.claveHash() : verificador.calcularHash(clave),
                        true));
        return true;
    }

    /** Las contraseñas por usuario; vacío si no hay ninguna fuente o no se pudo leer el archivo. */
    private Optional<Map<String, String>> claves() {
        String ruta = configuracion.claves();
        if (ruta != null && !ruta.isBlank()) {
            try {
                return Optional.of(leerClaves(Path.of(ruta)));
            } catch (IOException e) {
                REGISTRO.warn(
                        "El archivo de contraseñas de la demo {} no se pudo leer: no se crean"
                                + " usuarios de la demo.",
                        ruta);
                return Optional.empty();
            }
        }
        String comun = configuracion.clave();
        if (comun != null && !comun.isBlank()) {
            Map<String, String> todas = new HashMap<>();
            usuarios.forEach(demo -> todas.put(demo.usuario(), comun));
            return Optional.of(todas);
        }
        REGISTRO.warn("Usuarios de la demo sin contraseñas configuradas: no se crean.");
        return Optional.empty();
    }

    /** Lee líneas {@code usuario=clave}; ignora las vacías y las que empiezan con {@code #}. */
    private static Map<String, String> leerClaves(Path archivo) throws IOException {
        Map<String, String> claves = new HashMap<>();
        for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
            String limpia = linea.strip();
            int igual = limpia.indexOf('=');
            if (limpia.isEmpty() || limpia.startsWith("#") || igual < 1) {
                continue;
            }
            claves.put(Usuario.normalizar(limpia.substring(0, igual)), limpia.substring(igual + 1));
        }
        return claves;
    }
}
