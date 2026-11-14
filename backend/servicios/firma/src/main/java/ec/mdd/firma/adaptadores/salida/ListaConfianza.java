package ec.mdd.firma.adaptadores.salida;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;

/**
 * Las raíces de certificación en las que confía el entorno (D-23, D-103): certificados PEM que se
 * leen al arrancar, según {@code mdd.firma.raices}.
 *
 * <p>La lista es un dato del entorno, no un interruptor (ADR-017): sin raíces, o con un archivo
 * ilegible, el proceso no arranca, porque no hay forma de aceptar una firma. Por defecto es la de
 * demostración, la CA de prueba y la raíz pública del BCE; una instalación de producción la
 * reemplaza por las raíces de las entidades acreditadas, sin la CA de prueba (principio IX, S-10).
 * Un archivo con una llave privada se rechaza: en la lista solo hay certificados públicos.
 */
public final class ListaConfianza {

    private final List<X509Certificate> raices;

    private ListaConfianza(List<X509Certificate> raices) {
        this.raices = List.copyOf(raices);
    }

    /**
     * Lee las raíces de los archivos PEM.
     *
     * @param archivos los PEM, cada uno con uno o más certificados
     * @return la lista
     * @throws IllegalStateException si no hay raíces, si un archivo no se puede leer, no es un
     *     certificado o trae una llave privada
     */
    public static ListaConfianza desde(List<Resource> archivos) {
        List<X509Certificate> raices = new ArrayList<>();
        for (Resource archivo : archivos) {
            raices.addAll(leer(archivo));
        }
        if (raices.isEmpty()) {
            throw new IllegalStateException(
                    "La lista de confianza de Firma está vacía (mdd.firma.raices)");
        }
        return new ListaConfianza(raices);
    }

    private static List<X509Certificate> leer(Resource archivo) {
        byte[] contenido;
        try (InputStream entrada = archivo.getInputStream()) {
            contenido = entrada.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se puede leer la raíz de confianza " + archivo.getDescription(), e);
        }
        String texto = new String(contenido, StandardCharsets.US_ASCII);
        if (texto.contains("PRIVATE KEY")) {
            throw new IllegalStateException(
                    "La raíz de confianza " + archivo.getDescription() + " trae una llave privada");
        }
        try {
            List<X509Certificate> certificados =
                    CertificateFactory.getInstance("X.509")
                            .generateCertificates(new ByteArrayInputStream(contenido))
                            .stream()
                            .map(X509Certificate.class::cast)
                            .toList();
            if (certificados.isEmpty()) {
                throw new IllegalStateException(
                        "La raíz de confianza "
                                + archivo.getDescription()
                                + " no tiene certificados");
            }
            return certificados;
        } catch (CertificateException e) {
            throw new IllegalStateException(
                    "La raíz de confianza " + archivo.getDescription() + " no es un certificado",
                    e);
        }
    }

    /** Las raíces, en el orden en que se leyeron. */
    public List<X509Certificate> raices() {
        return raices;
    }

    /** Las raíces como anclas de PKIX. */
    Set<TrustAnchor> anclas() {
        return raices.stream()
                .map(raiz -> new TrustAnchor(raiz, null))
                .collect(Collectors.toUnmodifiableSet());
    }
}
