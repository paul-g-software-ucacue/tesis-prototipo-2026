package ec.mdd.cargas.adaptadores.salida;

/**
 * Las firmas con que empiezan los archivos que no son texto, para que un archivo renombrado no pase
 * por otro formato (research §3).
 */
final class Firmas {

    private static final byte[] PDF = {'%', 'P', 'D', 'F'};
    private static final byte[] ZIP = {'P', 'K', 3, 4};
    private static final byte[] OLE2 = {
        (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };

    private Firmas() {}

    /** Si es un ZIP, como un XLSX. */
    static boolean esZip(byte[] contenido) {
        return empiezaCon(contenido, ZIP);
    }

    /** Si es un documento OLE2, como un XLS antiguo o un XLSX cifrado. */
    static boolean esOle2(byte[] contenido) {
        return empiezaCon(contenido, OLE2);
    }

    /** Si no es texto: un PDF, un ZIP, un OLE2 o algo con bytes nulos. */
    static boolean esBinario(byte[] contenido) {
        if (empiezaCon(contenido, PDF) || esZip(contenido) || esOle2(contenido)) {
            return true;
        }
        for (byte b : contenido) {
            if (b == 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean empiezaCon(byte[] contenido, byte[] firma) {
        if (contenido.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if (contenido[i] != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
