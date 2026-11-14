package ec.mdd.firma.dominio;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Un documento firmable con su cadena y sus firmantes esperados (F14-H01, D-102): de aquí salen el
 * paso en turno, quién lo firma y el archivo vigente.
 *
 * <p>El paso en turno es el primero de la cadena, en su orden, que todavía no tiene firma: la
 * cadena se firma en orden (RF-38). Las firmas llegan con F14-H02; hasta entonces el conjunto de
 * firmados está vacío y el turno es el primer paso. Un documento sin firmantes esperados (generado
 * antes de F14-H01) no tiene turno: no se adivina quién firma.
 *
 * @param documento el documento registrado
 * @param cadena los pasos que entregó Aprobación (MD-05)
 * @param firmantes el firmante esperado de cada paso, fijado al generar
 */
public record FirmasDelDocumento(
        DocumentoFirmable documento,
        List<PasoDeFirma> cadena,
        List<FirmanteEsperado> firmantes,
        List<FirmaRegistrada> firmas) {

    /** Exige los datos y copia las listas. */
    public FirmasDelDocumento {
        Objects.requireNonNull(documento, "documento");
        cadena = List.copyOf(cadena);
        firmantes = List.copyOf(firmantes);
        firmas = List.copyOf(firmas);
    }

    /** Esqueleto (F14-H02, T007). */
    public Optional<PasoEnTurno> pasoEnTurno() {
        throw new UnsupportedOperationException();
    }

    /** Esqueleto (F14-H02, T007). */
    public boolean enTurno(String usuario, Optional<String> identificacion) {
        throw new UnsupportedOperationException();
    }

    /**
     * El primer paso sin firma y su firmante esperado.
     *
     * @param firmados los órdenes de los pasos ya firmados
     * @return vacío si todos firmaron o si el paso no tiene firmante esperado
     */
    public Optional<PasoEnTurno> pasoEnTurno(Set<Integer> firmados) {
        return cadena.stream()
                .sorted(Comparator.comparingInt(PasoDeFirma::orden))
                .filter(paso -> !firmados.contains(paso.orden()))
                .findFirst()
                .flatMap(
                        paso ->
                                firmantes.stream()
                                        .filter(firmante -> firmante.orden() == paso.orden())
                                        .findFirst()
                                        .map(firmante -> new PasoEnTurno(paso, firmante)));
    }

    /**
     * Si quien pide es el firmante esperado del paso en turno.
     *
     * @param usuario su usuario
     * @param identificacion su identificación, si la tiene
     * @param firmados los órdenes ya firmados
     * @return verdadero solo para esa persona
     */
    public boolean enTurno(String usuario, Optional<String> identificacion, Set<Integer> firmados) {
        return pasoEnTurno(firmados)
                .filter(turno -> turno.firmante().reconoce(usuario, identificacion))
                .isPresent();
    }

    /**
     * El archivo que se firma ahora: el resultante de la última firma o, sin firmas, el original.
     * Hasta F14-H02 no hay firmas, así que es el original.
     */
    public ArchivoDelDocumento archivoVigente() {
        return new ArchivoDelDocumento(documento.archivo(), documento.sha256Original());
    }

    /**
     * El paso en turno y quién debe firmarlo.
     *
     * @param paso el paso de la cadena
     * @param firmante su firmante esperado
     */
    public record PasoEnTurno(PasoDeFirma paso, FirmanteEsperado firmante) {

        /** El rótulo del paso, como en el PDF: la etiqueta seguida de «por». */
        public String rotulo() {
            return paso.etiqueta() + " por";
        }
    }
}
