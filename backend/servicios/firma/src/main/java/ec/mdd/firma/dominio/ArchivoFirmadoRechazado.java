package ec.mdd.firma.dominio;

import java.io.Serial;
import java.util.Objects;

/**
 * Un PDF subido por el firmante en turno que no pasó la validación básica (RF-39, D-103): no cambia
 * nada, el resultado queda registrado y el firmante ve el motivo.
 *
 * <p>Hay una subclase por la primera comprobación rechazada, porque cada una tiene su código del
 * catálogo (422). El resultado completo va en los detalles de la respuesta.
 */
public abstract sealed class ArchivoFirmadoRechazado extends RuntimeException
        permits ArchivoFirmadoRechazado.NoEsLaVersionVigente,
                ArchivoFirmadoRechazado.SinUnaFirmaNueva,
                ArchivoFirmadoRechazado.NoIntegra,
                ArchivoFirmadoRechazado.SinConfianza,
                ArchivoFirmadoRechazado.NoVigente,
                ArchivoFirmadoRechazado.FueraDeOrden,
                ArchivoFirmadoRechazado.FirmanteEquivocado {

    @Serial private static final long serialVersionUID = 1L;

    private final transient ResultadoValidacion resultado;

    private ArchivoFirmadoRechazado(ResultadoValidacion resultado) {
        super("archivo firmado rechazado: " + resultado.primerRechazo().orElseThrow());
        this.resultado = resultado;
    }

    /**
     * El rechazo de un resultado no aceptado, con la subclase de su primera comprobación rechazada.
     *
     * @param resultado el resultado rechazado
     * @return el rechazo para lanzar
     * @throws IllegalArgumentException si el resultado se aceptó
     */
    public static ArchivoFirmadoRechazado de(ResultadoValidacion resultado) {
        Objects.requireNonNull(resultado, "resultado");
        Comprobacion primera =
                resultado
                        .primerRechazo()
                        .orElseThrow(
                                () -> new IllegalArgumentException("el resultado fue aceptado"));
        return switch (primera) {
            case MISMO_DOCUMENTO -> new NoEsLaVersionVigente(resultado);
            case UNA_FIRMA_NUEVA -> new SinUnaFirmaNueva(resultado);
            case INTEGRIDAD -> new NoIntegra(resultado);
            case CONFIANZA -> new SinConfianza(resultado);
            case VIGENCIA -> new NoVigente(resultado);
            case ORDEN -> new FueraDeOrden(resultado);
            case FIRMANTE -> new FirmanteEquivocado(resultado);
        };
    }

    /** El resultado registrado, con las cuatro comprobaciones. */
    public ResultadoValidacion resultado() {
        return resultado;
    }

    /** El archivo no empieza con el vigente ({@code FIRMA_NO_ES_LA_VERSION_VIGENTE}). */
    public static final class NoEsLaVersionVigente extends ArchivoFirmadoRechazado {
        @Serial private static final long serialVersionUID = 1L;

        private NoEsLaVersionVigente(ResultadoValidacion resultado) {
            super(resultado);
        }
    }

    /** Sin firma nueva o con más de una ({@code FIRMA_SIN_UNA_FIRMA_NUEVA}). */
    public static final class SinUnaFirmaNueva extends ArchivoFirmadoRechazado {
        @Serial private static final long serialVersionUID = 1L;

        private SinUnaFirmaNueva(ResultadoValidacion resultado) {
            super(resultado);
        }
    }

    /** La firma nueva no es íntegra ({@code FIRMA_NO_INTEGRA}). */
    public static final class NoIntegra extends ArchivoFirmadoRechazado {
        @Serial private static final long serialVersionUID = 1L;

        private NoIntegra(ResultadoValidacion resultado) {
            super(resultado);
        }
    }

    /** El certificado no llega a una raíz de confianza ({@code FIRMA_SIN_CONFIANZA}). */
    public static final class SinConfianza extends ArchivoFirmadoRechazado {
        @Serial private static final long serialVersionUID = 1L;

        private SinConfianza(ResultadoValidacion resultado) {
            super(resultado);
        }
    }

    /** El certificado no está vigente al subir ({@code FIRMA_CERTIFICADO_NO_VIGENTE}). */
    public static final class NoVigente extends ArchivoFirmadoRechazado {
        @Serial private static final long serialVersionUID = 1L;

        private NoVigente(ResultadoValidacion resultado) {
            super(resultado);
        }
    }

    /** La firma es de la persona de un paso posterior ({@code FIRMA_FUERA_DE_ORDEN}). */
    public static final class FueraDeOrden extends ArchivoFirmadoRechazado {
        @Serial private static final long serialVersionUID = 1L;

        private FueraDeOrden(ResultadoValidacion resultado) {
            super(resultado);
        }
    }

    /** El CN no es el nombre impreso del paso en turno ({@code FIRMA_FIRMANTE_EQUIVOCADO}). */
    public static final class FirmanteEquivocado extends ArchivoFirmadoRechazado {
        @Serial private static final long serialVersionUID = 1L;

        private FirmanteEquivocado(ResultadoValidacion resultado) {
            super(resultado);
        }
    }
}
