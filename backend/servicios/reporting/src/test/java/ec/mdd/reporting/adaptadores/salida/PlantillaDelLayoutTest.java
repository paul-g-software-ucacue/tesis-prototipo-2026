package ec.mdd.reporting.adaptadores.salida;

import static org.assertj.core.api.Assertions.assertThat;

import ec.mdd.reporting.dominio.LayoutExportacion;
import ec.mdd.reporting.dominio.ValidacionDelLayout;
import ec.mdd.reporting.puertos.salida.LectorDeLayout.LecturaDelLayout;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

/**
 * La plantilla es el layout por defecto: se lee y se valida por el mismo camino que un archivo
 * subido (research §6, D-119), y tiene las 13 columnas de FR-004 en su orden.
 */
class PlantillaDelLayoutTest {

    @Test
    void laPlantilla_seLeeYSeValidaSinProblemas_conLasColumnasDeFr004() throws IOException {
        byte[] plantilla;
        try (InputStream entrada =
                getClass().getResourceAsStream("/reporting/layout-siga-por-defecto.yaml")) {
            plantilla = entrada.readAllBytes();
        }

        LecturaDelLayout lectura = new LectorDeLayoutYaml().leer(plantilla);
        ValidacionDelLayout validacion = LayoutExportacion.validar(lectura.borrador());

        assertThat(validacion.problemas()).isEmpty();
        assertThat(
                        LayoutExportacion.porDefecto(lectura.texto(), validacion.columnas())
                                .encabezado())
                .containsExactly(
                        "PERIODO",
                        "TIPO_IDENTIFICACION",
                        "NUMERO_IDENTIFICACION",
                        "APELLIDOS_NOMBRES",
                        "DEDICACION",
                        "COMPONENTE",
                        "CODIGO_ACTIVIDAD",
                        "CODIGO_CARRERA",
                        "CODIGO_ASIGNATURA",
                        "PARALELO",
                        "HORAS_SEMANALES",
                        "VERSION",
                        "FECHA_PUBLICACION");
        assertThat(validacion.columnas()).allSatisfy(una -> assertThat(una.traduccion()).isEmpty());
    }
}
