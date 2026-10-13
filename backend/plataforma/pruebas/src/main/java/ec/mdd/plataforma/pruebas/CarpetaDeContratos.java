package ec.mdd.plataforma.pruebas;

import java.nio.file.Files;
import java.nio.file.Path;

/** Ubica la carpeta {@code contratos/} desde el directorio de trabajo de una prueba. */
final class CarpetaDeContratos {

    private CarpetaDeContratos() {}

    /**
     * Sube desde el directorio de trabajo (el del módulo) hasta encontrar {@code contratos/comun}.
     */
    static Path buscar() {
        Path directorio = Path.of("").toAbsolutePath();
        while (directorio != null) {
            Path contratos = directorio.resolve("contratos");
            if (Files.isDirectory(contratos.resolve("comun"))) {
                return contratos;
            }
            directorio = directorio.getParent();
        }
        throw new IllegalStateException("No se encontró la carpeta contratos/");
    }
}
