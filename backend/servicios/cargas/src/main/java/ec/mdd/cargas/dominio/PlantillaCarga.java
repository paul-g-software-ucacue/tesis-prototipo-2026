package ec.mdd.cargas.dominio;

import java.util.List;

/**
 * La plantilla de una carga: el CSV vacío, con solo sus encabezados (RF-05, glosario «Plantilla de
 * carga»).
 *
 * @param archivo el nombre del archivo, como {@code 01_estructura_organica.csv}
 * @param encabezados los campos de la carga, en el orden del diccionario
 */
public record PlantillaCarga(String archivo, List<String> encabezados) {

    /** Copia los encabezados. */
    public PlantillaCarga {
        encabezados = List.copyOf(encabezados);
    }
}
