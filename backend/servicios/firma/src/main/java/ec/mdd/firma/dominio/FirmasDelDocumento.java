package ec.mdd.firma.dominio;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Un documento firmable con su cadena, sus firmantes esperados y sus firmas registradas (F14-H01,
 * F14-H02, D-102, D-103): de aquí salen el paso en turno, quién lo firma y el archivo vigente.
 *
 * <p>El paso en turno es el primero de la cadena, en su orden, que todavía no tiene firma: la
 * cadena se firma en orden (RF-38). Un documento sin firmantes esperados (generado antes de
 * F14-H01) no tiene turno: no se adivina quién firma.
 *
 * <p>El archivo vigente es el de la firma de mayor orden; sin firmas, el original. Como una firma
 * solo se acepta si el archivo subido empieza con los bytes del vigente, la de mayor orden es
 * siempre la última de la cadena.
 *
 * @param documento el documento registrado
 * @param cadena los pasos que entregó Aprobación (MD-05)
 * @param firmantes el firmante esperado de cada paso, fijado al generar
 * @param firmas las firmas aceptadas, a lo sumo una por paso de la cadena
 */
public record FirmasDelDocumento(
        DocumentoFirmable documento,
        List<PasoDeFirma> cadena,
        List<FirmanteEsperado> firmantes,
        List<FirmaRegistrada> firmas) {

    /** Exige los datos, copia las listas y exige a lo sumo una firma por paso de la cadena. */
    public FirmasDelDocumento {
        Objects.requireNonNull(documento, "documento");
        cadena = List.copyOf(cadena);
        firmantes = List.copyOf(firmantes);
        firmas = List.copyOf(firmas);
        Set<Integer> pasos =
                cadena.stream().map(PasoDeFirma::orden).collect(Collectors.toUnmodifiableSet());
        Set<Integer> firmados = new HashSet<>();
        for (FirmaRegistrada firma : firmas) {
            if (!pasos.contains(firma.orden()) || !firmados.add(firma.orden())) {
                throw new IllegalArgumentException(
                        "una firma por paso de la cadena: " + firma.orden());
            }
        }
    }

    /**
     * El primer paso sin firma y su firmante esperado.
     *
     * @return vacío si todos firmaron o si el paso no tiene firmante esperado
     */
    public Optional<PasoEnTurno> pasoEnTurno() {
        Set<Integer> firmados =
                firmas.stream().map(FirmaRegistrada::orden).collect(Collectors.toSet());
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
     * @return verdadero solo para esa persona
     */
    public boolean enTurno(String usuario, Optional<String> identificacion) {
        return pasoEnTurno()
                .filter(turno -> turno.firmante().reconoce(usuario, identificacion))
                .isPresent();
    }

    /** El archivo que se firma ahora: el de la última firma o, sin firmas, el original. */
    public ArchivoDelDocumento archivoVigente() {
        return firmas.stream()
                .max(Comparator.comparingInt(FirmaRegistrada::orden))
                .map(FirmaRegistrada::archivo)
                .orElseGet(
                        () ->
                                new ArchivoDelDocumento(
                                        documento.archivo(), documento.sha256Original()));
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
