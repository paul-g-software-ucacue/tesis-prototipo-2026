package ec.mdd.validacionocs.puertos.salida;

import ec.mdd.validacionocs.dominio.Borrador;

/** Lee el texto de un archivo de configuración, sin interpretar nada peligroso (FR-009). */
public interface LectorDeConfiguracion {

    /**
     * @param texto el archivo
     * @return lo que dice, sin validar
     * @throws ec.mdd.validacionocs.dominio.ArchivoIlegible si no se puede leer
     */
    Borrador leer(String texto);
}
