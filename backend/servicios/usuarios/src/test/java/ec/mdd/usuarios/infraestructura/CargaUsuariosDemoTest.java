package ec.mdd.usuarios.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.mdd.usuarios.dominio.Usuario;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Los usuarios de la demo (D-59) con dobles escritos a mano de los puertos (sin Mockito). */
@ExtendWith(OutputCaptureExtension.class)
class CargaUsuariosDemoTest {

    private static final String CLAVE_COMUN = "clave-comun-de-prueba";

    private final RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
    private final VerificadorFalso verificador = new VerificadorFalso();
    private final AutorizacionEnMemoria autorizacion = new AutorizacionEnMemoria();

    @TempDir Path carpeta;

    @Test
    void dadoElArchivoVersionado_cuandoSeLee_entoncesTieneLosOnceUsuariosConLosDiezRoles()
            throws IOException {
        try (InputStream archivo =
                CargaUsuariosDemo.class.getResourceAsStream("/" + CargaUsuariosDemo.ARCHIVO)) {
            List<CargaUsuariosDemo.UsuarioDemo> usuarios = CargaUsuariosDemo.leer(archivo);

            assertThat(usuarios).hasSize(11);
            assertThat(usuarios)
                    .extracting(CargaUsuariosDemo.UsuarioDemo::usuario)
                    .contains("vicerrectorado.demo", "docente.demo", "docente.coordinador.demo")
                    .allMatch(nombre -> nombre.endsWith(".demo"))
                    .doesNotHaveDuplicates();
            assertThat(usuarios.stream().flatMap(u -> u.roles().stream()).distinct()).hasSize(10);
        }
    }

    @Test
    void dadoLaClaveComun_cuandoArranca_entoncesCreaTodosActivosYPideSusRolesIniciales(
            CapturedOutput salida) {
        carga(new PropiedadesUsuarios.Demo(null, CLAVE_COMUN)).run(null);

        assertThat(repositorio.usuarios).hasSize(2);
        Usuario mixto = repositorio.buscarPorNombre("docente.coordinador.demo").orElseThrow();
        assertThat(mixto.nombreParaMostrar()).isEqualTo("Docente y coordinador (demo)");
        assertThat(autorizacion.roles)
                .containsEntry(
                        "docente.coordinador.demo", List.of("COORDINADOR_CARRERA", "DOCENTE"));
        assertThat(mixto.claveHash()).isEqualTo("hash:" + CLAVE_COMUN);
        assertThat(mixto.puedeIniciarSesion()).isTrue();
        assertThat(salida.getAll()).doesNotContain(CLAVE_COMUN);
    }

    @Test
    void dadoElArchivoDeClaves_cuandoArranca_entoncesCadaUnoTieneLaSuyaYSinLineaNoSeCrea(
            CapturedOutput salida) throws IOException {
        Path claves = carpeta.resolve("claves-demo");
        Files.writeString(claves, "# comentario\n\ndocente.demo=clave=con-igual\n");

        carga(new PropiedadesUsuarios.Demo(claves.toString(), CLAVE_COMUN)).run(null);

        assertThat(repositorio.usuarios).hasSize(1);
        assertThat(repositorio.usuarios.get(0).claveHash()).isEqualTo("hash:clave=con-igual");
        assertThat(salida.getAll())
                .contains("docente.coordinador.demo")
                .doesNotContain("clave=con-igual")
                .doesNotContain(CLAVE_COMUN);
    }

    @Test
    void dadoOtraClaveYOtrosDatos_cuandoVuelveAArrancar_entoncesActualizaSinDuplicar() {
        repositorio.guardar(new Usuario("docente.demo", "Nombre viejo", "hash:vieja", false));

        carga(new PropiedadesUsuarios.Demo(null, CLAVE_COMUN)).run(null);

        assertThat(repositorio.usuarios).hasSize(2);
        Usuario docente = repositorio.buscarPorNombre("docente.demo").orElseThrow();
        assertThat(docente.claveHash()).isEqualTo("hash:" + CLAVE_COMUN);
        assertThat(docente.nombreParaMostrar()).isEqualTo("Docente (demo)");
        assertThat(autorizacion.roles).containsEntry("docente.demo", List.of("DOCENTE"));
        assertThat(docente.puedeIniciarSesion()).isTrue();
    }

    @Test
    void dadoQueNadaCambio_cuandoVuelveAArrancar_entoncesNoRecalculaNiGuarda() {
        CargaUsuariosDemo carga = carga(new PropiedadesUsuarios.Demo(null, CLAVE_COMUN));
        carga.run(null);
        int hashes = verificador.hashesCalculados;

        carga.run(null);

        assertThat(verificador.hashesCalculados).isEqualTo(hashes);
        assertThat(repositorio.actualizaciones).isZero();
    }

    @Test
    void dadoUsuariosQueYaExistian_cuandoArranca_entoncesTambienPideSusRolesIniciales() {
        // Al desplegar F05-H02, los usuarios ya existen y Roles está vacío (research §8).
        repositorio.guardar(
                new Usuario("docente.demo", "Docente (demo)", "hash:" + CLAVE_COMUN, true));

        carga(new PropiedadesUsuarios.Demo(null, CLAVE_COMUN)).run(null);

        assertThat(autorizacion.roles).containsEntry("docente.demo", List.of("DOCENTE"));
    }

    @Test
    void dadoRolesCambiadosPorElAdministrador_cuandoVuelveAArrancar_entoncesNoSeTocan() {
        autorizacion.roles.put("docente.demo", List.of());

        carga(new PropiedadesUsuarios.Demo(null, CLAVE_COMUN)).run(null);

        assertThat(autorizacion.roles).containsEntry("docente.demo", List.of());
    }

    @Test
    void dadoSinArchivoNiClaveComun_cuandoArranca_entoncesNoCreaNadaYAvisa(CapturedOutput salida) {
        carga(new PropiedadesUsuarios.Demo(" ", null)).run(null);

        assertThat(repositorio.usuarios).isEmpty();
        assertThat(salida.getAll()).contains("Usuarios de la demo sin contraseñas");
    }

    @Test
    void dadoUnArchivoDeClavesQueNoExiste_cuandoArranca_entoncesNoCreaNadaYAvisa(
            CapturedOutput salida) {
        carga(new PropiedadesUsuarios.Demo(carpeta.resolve("no-existe").toString(), null))
                .run(null);

        assertThat(repositorio.usuarios).isEmpty();
        assertThat(salida.getAll()).contains("no se pudo leer");
    }

    @Test
    void dadoUnRolDesconocidoEnElArchivo_cuandoArranca_entoncesFallaYDiceCual() {
        String yaml = "usuarios:\n  - usuario: raro.demo\n    nombre: Raro\n    roles: [JEFE]\n";
        List<CargaUsuariosDemo.UsuarioDemo> usuarios =
                CargaUsuariosDemo.leer(
                        new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        CargaUsuariosDemo carga =
                new CargaUsuariosDemo(
                        repositorio,
                        verificador,
                        autorizacion,
                        usuarios,
                        new PropiedadesUsuarios.Demo(null, CLAVE_COMUN));

        assertThatThrownBy(() -> carga.run(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JEFE");
    }

    @Test
    void dadoLasPropiedades_cuandoSeConviertenEnTexto_entoncesNoMuestranLaClave() {
        assertThat(new PropiedadesUsuarios.Demo("/run/mdd/claves-demo", CLAVE_COMUN).toString())
                .doesNotContain(CLAVE_COMUN)
                .contains("/run/mdd/claves-demo");
    }

    private CargaUsuariosDemo carga(PropiedadesUsuarios.Demo demo) {
        return new CargaUsuariosDemo(
                repositorio,
                verificador,
                autorizacion,
                List.of(
                        new CargaUsuariosDemo.UsuarioDemo(
                                "docente.demo", "Docente (demo)", Set.of("DOCENTE")),
                        new CargaUsuariosDemo.UsuarioDemo(
                                "docente.coordinador.demo",
                                "Docente y coordinador (demo)",
                                Set.of("DOCENTE", "COORDINADOR_CARRERA"))),
                demo);
    }
}
