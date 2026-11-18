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
 * <p>Un documento rechazado en firma está cerrado (F14-H04, D-105): no tiene turno, así que nadie
 * lo ve pendiente, lo descarga, sube una firma ni lo rechaza otra vez. Sus firmas aceptadas y su
 * archivo vigente se conservan.
 *
 * <p>La cadena con el estado de cada paso (F16-H02, D-110) usa la misma regla del turno, así que lo
 * que ve quien sigue el proceso coincide con las firmas pendientes del firmante: un paso con firma
 * aceptada está firmado; el rechazado, rechazado; el de {@link #pasoEnTurno()}, en turno; el resto,
 * pendiente.
 *
 * @param documento el documento registrado
 * @param cadena los pasos que entregó Aprobación (MD-05)
 * @param firmantes el firmante esperado de cada paso, fijado al generar
 * @param firmas las firmas aceptadas, a lo sumo una por paso de la cadena
 * @param rechazo el rechazo en firma, si lo hubo
 * @param anulacion la anulación de sus firmas, si la hubo (F12-H04)
 */
public record FirmasDelDocumento(
        DocumentoFirmable documento,
        List<PasoDeFirma> cadena,
        List<FirmanteEsperado> firmantes,
        List<FirmaRegistrada> firmas,
        Optional<RechazoDelFirmante> rechazo,
        Optional<AnulacionDeFirmas> anulacion) {

    /** Un documento sin rechazo en firma ni anulación. */
    public FirmasDelDocumento(
            DocumentoFirmable documento,
            List<PasoDeFirma> cadena,
            List<FirmanteEsperado> firmantes,
            List<FirmaRegistrada> firmas) {
        this(documento, cadena, firmantes, firmas, Optional.empty());
    }

    /** Un documento sin anulación de sus firmas. */
    public FirmasDelDocumento(
            DocumentoFirmable documento,
            List<PasoDeFirma> cadena,
            List<FirmanteEsperado> firmantes,
            List<FirmaRegistrada> firmas,
            Optional<RechazoDelFirmante> rechazo) {
        this(documento, cadena, firmantes, firmas, rechazo, Optional.empty());
    }

    /** Exige los datos, copia las listas y exige a lo sumo una firma por paso de la cadena. */
    public FirmasDelDocumento {
        Objects.requireNonNull(documento, "documento");
        cadena = List.copyOf(cadena);
        firmantes = List.copyOf(firmantes);
        firmas = List.copyOf(firmas);
        Objects.requireNonNull(rechazo, "rechazo");
        Objects.requireNonNull(anulacion, "anulacion");
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
     * @return vacío si todos firmaron, si el documento se rechazó en firma, si sus firmas se
     *     anularon o si el paso no tiene firmante esperado
     */
    public Optional<PasoEnTurno> pasoEnTurno() {
        if (rechazo.isPresent() || anulacion.isPresent()) {
            return Optional.empty();
        }
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

    /**
     * Cada paso de la cadena con su estado, en su orden, para quien sigue el proceso (F16-H02,
     * D-110). El paso en turno es el de {@link #pasoEnTurno()}: la cadena y las firmas pendientes
     * no se contradicen.
     *
     * @return los pasos con su estado
     */
    public List<PasoConEstado> cadenaConEstado() {
        Optional<Integer> enTurno = pasoEnTurno().map(turno -> turno.paso().orden());
        Optional<Integer> rechazado = rechazo.map(RechazoDelFirmante::orden);
        return cadena.stream()
                .sorted(Comparator.comparingInt(PasoDeFirma::orden))
                .map(
                        paso -> {
                            Optional<FirmaRegistrada> firma =
                                    firmas.stream()
                                            .filter(una -> una.orden() == paso.orden())
                                            .findFirst();
                            PasoConEstado.Estado estado;
                            if (firma.isPresent()) {
                                estado = PasoConEstado.Estado.FIRMADO;
                            } else if (rechazado.filter(uno -> uno == paso.orden()).isPresent()) {
                                estado = PasoConEstado.Estado.RECHAZADO;
                            } else if (enTurno.filter(uno -> uno == paso.orden()).isPresent()) {
                                estado = PasoConEstado.Estado.EN_TURNO;
                            } else {
                                estado = PasoConEstado.Estado.PENDIENTE;
                            }
                            return new PasoConEstado(
                                    paso.orden(),
                                    rotulo(paso),
                                    firmanteDe(paso.orden()).flatMap(FirmanteEsperado::nombre),
                                    estado,
                                    firma.map(FirmaRegistrada::subidoEn));
                        })
                .toList();
    }

    /**
     * El paso que sigue a uno recién firmado, con su firmante esperado (F16-H02).
     *
     * @param orden el orden del paso firmado
     * @return el siguiente; vacío si era el último o si el siguiente no tiene firmante esperado
     */
    public Optional<PasoEnTurno> siguienteTrasFirmar(int orden) {
        return cadena.stream()
                .filter(paso -> paso.orden() > orden)
                .min(Comparator.comparingInt(PasoDeFirma::orden))
                .flatMap(paso -> firmanteDe(paso.orden()).map(uno -> new PasoEnTurno(paso, uno)));
    }

    private Optional<FirmanteEsperado> firmanteDe(int orden) {
        return firmantes.stream().filter(firmante -> firmante.orden() == orden).findFirst();
    }

    // El rótulo, como en el PDF: la etiqueta seguida de «por».
    private static String rotulo(PasoDeFirma paso) {
        return paso.etiqueta() + " por";
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
            return FirmasDelDocumento.rotulo(paso);
        }
    }
}
