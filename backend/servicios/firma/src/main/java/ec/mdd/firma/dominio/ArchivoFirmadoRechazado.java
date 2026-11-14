package ec.mdd.firma.dominio;

import java.io.Serial;

/** Esqueleto (F14-H02, T006). */
public abstract sealed class ArchivoFirmadoRechazado extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    private final transient ResultadoValidacion resultado;

    ArchivoFirmadoRechazado(ResultadoValidacion resultado) {
        this.resultado = resultado;
    }

    public static ArchivoFirmadoRechazado de(ResultadoValidacion resultado) {
        throw new UnsupportedOperationException();
    }

    public ResultadoValidacion resultado() {
        return resultado;
    }

    public static final class NoEsLaVersionVigente extends ArchivoFirmadoRechazado {
        @Serial private static final long serialVersionUID = 1L;

        NoEsLaVersionVigente(ResultadoValidacion resultado) {
            super(resultado);
        }
    }

    public static final class SinUnaFirmaNueva extends ArchivoFirmadoRechazado {
        @Serial private static final long serialVersionUID = 1L;

        SinUnaFirmaNueva(ResultadoValidacion resultado) {
            super(resultado);
        }
    }

    public static final class NoIntegra extends ArchivoFirmadoRechazado {
        @Serial private static final long serialVersionUID = 1L;

        NoIntegra(ResultadoValidacion resultado) {
            super(resultado);
        }
    }

    public static final class SinConfianza extends ArchivoFirmadoRechazado {
        @Serial private static final long serialVersionUID = 1L;

        SinConfianza(ResultadoValidacion resultado) {
            super(resultado);
        }
    }
}
