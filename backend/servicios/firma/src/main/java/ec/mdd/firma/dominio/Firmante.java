package ec.mdd.firma.dominio;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Un firmante del bloque de firmas del documento (F13-H02, FR-006, FR-007; diseño de firmas §4).
 *
 * @param orden su turno, desde 1
 * @param rotulo la etiqueta de la configuración seguida de «por» («Elaborado por»)
 * @param cargo el cargo que firma
 * @param nombre el nombre de quien lo ocupa en el periodo
 * @param campo el nombre del campo de firma vacío: {@code firma-<orden>-<rol>}
 */
public record Firmante(int orden, String rotulo, String cargo, String nombre, String campo) {

    // El cargo de cada rol de la cadena por defecto y de los demás que firman (D-16, ADR-004).
    private static final Map<String, String> CARGOS =
            Map.of(
                    "RESPONSABLE_CONSOLIDACION", "Responsable de consolidación",
                    "VICERRECTOR", "Vicerrector académico",
                    "RECTOR", "Rector",
                    "SECRETARIA_GENERAL", "Secretaría general",
                    "MIEMBRO_OCS", "Miembro del OCS");

    /** Exige los datos. */
    public Firmante {
        Objects.requireNonNull(rotulo, "rotulo");
        Objects.requireNonNull(cargo, "cargo");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(campo, "campo");
    }

    /**
     * Los firmantes de la cadena, en su orden.
     *
     * @param cadena los pasos que entregó Aprobación
     * @param nombres el nombre de quien ocupa cada rol en el periodo
     * @return uno por paso
     * @throws FirmanteSinNombre si a un rol le falta el nombre
     */
    public static List<Firmante> de(List<PasoDeFirma> cadena, Map<String, String> nombres) {
        return cadena.stream()
                .sorted(Comparator.comparingInt(PasoDeFirma::orden))
                .map(paso -> de(paso, nombres.get(paso.rol())))
                .toList();
    }

    /** El cargo de un rol; si no es conocido, el rol. */
    public static String cargoDe(String rol) {
        return CARGOS.getOrDefault(rol, rol);
    }

    private static Firmante de(PasoDeFirma paso, String nombre) {
        String cargo = cargoDe(paso.rol());
        if (nombre == null || nombre.isBlank()) {
            throw new FirmanteSinNombre(cargo);
        }
        return new Firmante(
                paso.orden(),
                paso.etiqueta() + " por",
                cargo,
                nombre.strip(),
                "firma-" + paso.orden() + "-" + paso.rol());
    }
}
