package ec.mdd.generacion.puertos.entrada;

import ec.mdd.generacion.dominio.InformeSustentado;

/** Adjuntar y descargar el informe sustentado (RF-32). */
public interface GestionarInforme {

    /**
     * Adjunta el informe a la versión consolidada, o lo reemplaza.
     *
     * @param periodo el código del periodo
     * @param nombre el nombre recibido
     * @param contenido sus bytes
     * @param quien quién lo adjunta
     * @return el informe vigente
     * @throws SinDistributivo si el periodo no tiene distributivo
     * @throws PeriodoCerrado si el periodo se cerró
     * @throws SinConsolidar si no se consolidó
     * @throws ec.mdd.generacion.dominio.InformeNoEsPdf si no es un PDF
     * @throws ec.mdd.generacion.dominio.InformeDemasiadoGrande si pasa de 10 MB
     */
    InformeSustentado adjuntar(String periodo, String nombre, byte[] contenido, String quien);

    /**
     * Descarga el informe vigente.
     *
     * @param periodo el código del periodo
     * @return sus bytes, idénticos a los adjuntados
     * @throws InformeNoEncontrado si no hay informe
     * @throws InformeIlegible si el archivo falta o no coincide con su huella
     */
    byte[] descargar(String periodo);
}
