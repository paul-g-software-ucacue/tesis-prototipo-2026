package ec.mdd.firma.adaptadores.salida;

import java.security.cert.X509Certificate;
import java.util.List;
import org.springframework.core.io.Resource;

/** Esqueleto (F14-H02, T008). */
public final class ListaConfianza {

    private ListaConfianza() {}

    public static ListaConfianza desde(List<Resource> archivos) {
        throw new UnsupportedOperationException();
    }

    public List<X509Certificate> raices() {
        throw new UnsupportedOperationException();
    }
}
